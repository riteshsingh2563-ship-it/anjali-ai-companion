package com.example.util

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.BackendAppConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class NvidiaCompletionResponse(
  val reply: String,
  val model: String,
  val promptTokens: Int?,
  val completionTokens: Int?,
  val totalTokens: Int?,
  val latencyMs: Long,
  val requestId: String,
)

object NvidiaAiClient {
  private const val TAG = "NvidiaAiClient"
  private const val DEFAULT_ENDPOINT = "https://integrate.api.nvidia.com/v1/chat/completions"
  private const val DEFAULT_BACKEND_URL = ""
  private val FALLBACK_MODELS = listOf(
    "nvidia/nemotron-3.5-lightning-30b-a3b",
    "google/gemma-4-31b-it"
  )

  @Volatile
  private var backendApiKey: String? = null

  @Volatile
  private var activeConfig: BackendAppConfig = BackendAppConfig()

  fun updateFromBackendConfig(config: BackendAppConfig) {
    activeConfig = config
    if (config.apiKey.isNotBlank() && config.apiKey != "YOUR_NVIDIA_API_KEY") {
      backendApiKey = config.apiKey.trim()
      Log.d(TAG, "Updated AI config & API key from live backend.")
    }
  }

  fun setBackendApiKey(key: String) {
    val clean = key.trim()
    if (clean.isNotBlank() && clean != "YOUR_NVIDIA_API_KEY") {
      backendApiKey = clean
      Log.d(TAG, "Backend AI key configured successfully from Firestore.")
    }
  }

  fun getBackendApiKey(): String? = backendApiKey

  fun isConfigured(customApiKey: String? = null): Boolean {
    val key = resolveApiKey(customApiKey)
    val hasDirectKey = key.isNotBlank() &&
      key != "YOUR_NVIDIA_API_KEY" &&
      key != "NVIDIA_API_KEY_DEFAULT_VALUE" &&
      !key.startsWith("YOUR_")
    val hasBackendUrl = activeConfig.backendUrl.isNotBlank()
    return hasDirectKey || hasBackendUrl
  }

  fun resolveApiKey(customApiKey: String? = null): String {
    val runtimeKey = customApiKey?.trim().orEmpty()
    if (runtimeKey.isNotEmpty() && !runtimeKey.startsWith("YOUR_")) return runtimeKey

    val configKey = activeConfig.apiKey.trim()
    if (configKey.isNotEmpty() && !configKey.startsWith("YOUR_")) return configKey

    val fromBackend = backendApiKey?.trim().orEmpty()
    if (fromBackend.isNotEmpty() && !fromBackend.startsWith("YOUR_")) return fromBackend

    val fromBuild = BuildConfig.NVIDIA_API_KEY.trim()
    if (fromBuild.isNotEmpty() && !fromBuild.startsWith("YOUR_")) return fromBuild

    return ""
  }

  private suspend fun getFirebaseIdTokenOrNull(): String? {
    return try {
      Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
    } catch (e: Exception) {
      null
    }
  }

  suspend fun generateReply(
    systemPrompt: String,
    userPrompt: String,
    customApiKey: String? = null,
    configOverride: BackendAppConfig? = null,
  ): Result<String> = withContext(Dispatchers.IO) {
    val cfg = configOverride ?: activeConfig
    val requestId = "and_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    val startMs = System.currentTimeMillis()
    val idToken = getFirebaseIdTokenOrNull()
    val userId = Firebase.auth.currentUser?.uid

    // 1. Try executing through the server-side backend (/api/ai/chat) first.
    // This keeps the NVIDIA API key server-side and automatically logs usage in PostgreSQL.
    val backendBase = cfg.backendUrl.ifBlank { DEFAULT_BACKEND_URL }.trimEnd('/')
    if (backendBase.startsWith("http") && !idToken.isNullOrBlank()) {
      try {
        val backendReply = executeViaBackendServer(
          backendBaseUrl = backendBase,
          idToken = idToken,
          systemPrompt = systemPrompt,
          userPrompt = userPrompt,
          model = cfg.model,
          temperature = cfg.temperature,
          maxTokens = cfg.maxTokens,
          userId = userId,
          requestId = requestId
        )
        if (backendReply.isNotBlank()) {
          return@withContext Result.success(backendReply)
        }
      } catch (e: Exception) {
        Log.d(TAG, "Server /api/ai/chat unavailable (${e.message}), using direct NVIDIA client")
      }
    }

    // 2. Fallback to direct NVIDIA endpoint if an API key is available
    val apiKey = resolveApiKey(customApiKey)
    if (apiKey.isBlank() || apiKey.startsWith("YOUR_")) {
      return@withContext Result.failure(
        IllegalStateException("AI API key not configured on backend.")
      )
    }

    val endpoint = cfg.apiEndpoint.ifBlank { DEFAULT_ENDPOINT }
    val rawModel = cfg.model.ifBlank { FALLBACK_MODELS.first() }
    val preferredModel = when {
      endpoint.contains("nvidia.com") && rawModel.startsWith("gemini-") -> "nvidia/nemotron-3.5-lightning-30b-a3b"
      endpoint.contains("nvidia.com") && !rawModel.contains("/") && rawModel.startsWith("nemotron") -> "nvidia/$rawModel"
      else -> rawModel
    }
    val modelCandidates = (listOf(preferredModel) + FALLBACK_MODELS).distinct()
    val temperature = cfg.temperature.coerceIn(0.0, 1.5)
    val isReasoning = preferredModel.contains("nemotron")
    val minTokens = if (isReasoning) 2048 else 200
    val maxTokens = cfg.maxTokens.coerceIn(minTokens, 4096)

    var lastException: Exception? = null
    var wasRateLimited = false

    for (modelName in modelCandidates) {
      for (attempt in 1..2) {
        try {
          val result = executeChatCompletion(
            endpoint = endpoint,
            apiKey = apiKey,
            model = modelName,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            temperature = temperature,
            maxTokens = maxTokens,
            requestId = requestId,
            startMs = startMs
          )
          if (result.reply.isNotBlank()) {
            reportUsageEvent(
              backendBaseUrl = backendBase,
              idToken = idToken,
              userId = userId,
              status = "success",
              tokensUsed = result.totalTokens,
              promptTokens = result.promptTokens,
              completionTokens = result.completionTokens,
              model = result.model,
              provider = cfg.provider.ifBlank { "nvidia" },
              latencyMs = result.latencyMs,
              requestId = result.requestId
            )
            return@withContext Result.success(result.reply)
          }
        } catch (e: Exception) {
          lastException = e
          val msg = e.message.orEmpty()
          Log.w(TAG, "AI attempt $attempt with $modelName failed: $msg")
          if (msg.contains("429")) {
            wasRateLimited = true
            if (attempt == 1) {
              delay(1200L)
            }
          } else {
            break
          }
        }
      }
    }

    reportUsageEvent(
      backendBaseUrl = backendBase,
      idToken = idToken,
      userId = userId,
      status = if (wasRateLimited) "rate_limited" else "failure",
      tokensUsed = null,
      promptTokens = null,
      completionTokens = null,
      model = preferredModel,
      provider = cfg.provider.ifBlank { "nvidia" },
      latencyMs = System.currentTimeMillis() - startMs,
      requestId = requestId,
      errorMessage = lastException?.message?.take(200)
    )

    Result.failure(
      lastException ?: IllegalStateException("AI service returned an empty response.")
    )
  }

  private fun executeViaBackendServer(
    backendBaseUrl: String,
    idToken: String,
    systemPrompt: String,
    userPrompt: String,
    model: String,
    temperature: Double,
    maxTokens: Int,
    userId: String?,
    requestId: String,
  ): String {
    val url = URL("$backendBaseUrl/api/ai/chat")
    val connection = (url.openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      connectTimeout = 15_000
      readTimeout = 40_000
      doOutput = true
      setRequestProperty("Authorization", "Bearer $idToken")
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Accept", "application/json")
    }

    try {
      val payload = JSONObject().apply {
        put("systemPrompt", systemPrompt)
        put("userPrompt", userPrompt)
        if (model.isNotBlank()) put("model", model)
        put("temperature", temperature)
        put("maxTokens", maxTokens)
        if (!userId.isNullOrBlank()) put("userId", userId)
        put("requestId", requestId)
      }

      OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
        writer.write(payload.toString())
        writer.flush()
      }

      val responseCode = connection.responseCode
      val stream = if (responseCode in 200..299) {
        connection.inputStream
      } else {
        connection.errorStream ?: connection.inputStream
      }

      val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
      if (responseCode !in 200..299) {
        throw IllegalStateException("Backend AI HTTP $responseCode: ${body.take(180)}")
      }

      val json = JSONObject(body)
      return json.optString("reply").trim()
    } finally {
      connection.disconnect()
    }
  }

  private fun executeChatCompletion(
    endpoint: String,
    apiKey: String,
    model: String,
    systemPrompt: String,
    userPrompt: String,
    temperature: Double,
    maxTokens: Int,
    requestId: String,
    startMs: Long,
  ): NvidiaCompletionResponse {
    val url = URL(endpoint)
    val connection = (url.openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      connectTimeout = 15_000
      readTimeout = 40_000
      doOutput = true
      setRequestProperty("Authorization", "Bearer $apiKey")
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Accept", "application/json")
    }

    try {
      val messagesArray = JSONArray().apply {
        put(
          JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
          }
        )
        put(
          JSONObject().apply {
            put("role", "user")
            put("content", userPrompt)
          }
        )
      }

      val payload = JSONObject().apply {
        put("model", model)
        put("messages", messagesArray)
        put("temperature", temperature)
        put("top_p", 0.9)
        put("max_tokens", maxTokens)
        put("stream", false)
      }

      OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
        writer.write(payload.toString())
        writer.flush()
      }

      val responseCode = connection.responseCode
      val stream = if (responseCode in 200..299) {
        connection.inputStream
      } else {
        connection.errorStream ?: connection.inputStream
      }

      val responseBody = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use {
        it.readText()
      }

      if (responseCode !in 200..299) {
        throw IllegalStateException("AI HTTP $responseCode: ${responseBody.take(240)}")
      }

      val json = JSONObject(responseBody)
      val usageObj = json.optJSONObject("usage")
      val promptTokens = if (usageObj != null && usageObj.has("prompt_tokens")) {
        usageObj.optInt("prompt_tokens", -1).takeIf { it >= 0 }
      } else null
      val completionTokens = if (usageObj != null && usageObj.has("completion_tokens")) {
        usageObj.optInt("completion_tokens", -1).takeIf { it >= 0 }
      } else null
      val totalTokens = if (usageObj != null && usageObj.has("total_tokens")) {
        usageObj.optInt("total_tokens", -1).takeIf { it >= 0 }
      } else if (promptTokens != null && completionTokens != null) {
        promptTokens + completionTokens
      } else null

      val usedModel = json.optString("model").takeIf { it.isNotBlank() } ?: model
      val choices = json.optJSONArray("choices")
      if (choices != null && choices.length() > 0) {
        val firstChoice = choices.getJSONObject(0)
        val messageObj = firstChoice.optJSONObject("message")
        val rawContent = messageObj?.optString("content").orEmpty().trim()
        val content = if (rawContent == "null") "" else rawContent
        if (content.isNotEmpty()) {
          return NvidiaCompletionResponse(
            reply = content,
            model = usedModel,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = totalTokens,
            latencyMs = System.currentTimeMillis() - startMs,
            requestId = requestId
          )
        }
      }
      throw IllegalStateException("No message content in AI response.")
    } finally {
      connection.disconnect()
    }
  }

  suspend fun reportUsageFromFallback(
    status: String,
    model: String,
    provider: String = "gemini",
    latencyMs: Long,
    requestId: String,
    errorMessage: String? = null,
    configOverride: BackendAppConfig? = null,
  ) = withContext(Dispatchers.IO) {
    val cfg = configOverride ?: activeConfig
    val backendBase = cfg.backendUrl.ifBlank { DEFAULT_BACKEND_URL }.trimEnd('/')
    val idToken = getFirebaseIdTokenOrNull()
    val userId = Firebase.auth.currentUser?.uid
    reportUsageEvent(
      backendBaseUrl = backendBase,
      idToken = idToken,
      userId = userId,
      status = status,
      tokensUsed = null,
      promptTokens = null,
      completionTokens = null,
      model = model,
      provider = provider,
      latencyMs = latencyMs,
      requestId = requestId,
      errorMessage = errorMessage
    )
  }

  private fun reportUsageEvent(
    backendBaseUrl: String,
    idToken: String?,
    userId: String?,
    status: String,
    tokensUsed: Int?,
    promptTokens: Int?,
    completionTokens: Int?,
    model: String,
    provider: String,
    latencyMs: Long,
    requestId: String,
    errorMessage: String? = null,
  ) {
    if (!backendBaseUrl.startsWith("http") || idToken.isNullOrBlank()) return
    try {
      val url = URL("$backendBaseUrl/api/usage/ingest")
      val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 6_000
        readTimeout = 8_000
        doOutput = true
        setRequestProperty("Authorization", "Bearer $idToken")
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Accept", "application/json")
      }
      try {
        val payload = JSONObject().apply {
          if (!userId.isNullOrBlank()) put("userId", userId)
          put("requestType", "chat_message")
          put("status", status)
          if (tokensUsed != null) put("tokensUsed", tokensUsed)
          if (promptTokens != null) put("promptTokens", promptTokens)
          if (completionTokens != null) put("completionTokens", completionTokens)
          put("model", model)
          put("provider", provider)
          put("latencyMs", latencyMs.coerceAtLeast(0L))
          put("requestId", requestId)
          if (!errorMessage.isNullOrBlank()) put("errorMessage", errorMessage.take(250))
          put("source", "android_app")
        }
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
          writer.write(payload.toString())
          writer.flush()
        }
        val code = connection.responseCode
        Log.d(TAG, "Reported usage event ($status, tokens=$tokensUsed) -> HTTP $code")
      } finally {
        connection.disconnect()
      }
    } catch (e: Exception) {
      Log.d(TAG, "Usage telemetry report skipped: ${e.message}")
    }
  }
}
