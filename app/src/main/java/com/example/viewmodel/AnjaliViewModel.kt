package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BackendAppConfig
import com.example.data.model.ChatMessage
import com.example.data.model.CompanionLanguage
import com.example.data.model.Conversation
import com.example.data.model.ExpressiveTone
import com.example.data.model.MemoryItem
import com.example.data.model.UserPreferences
import com.example.data.repository.AnjaliRepository
import com.example.util.NvidiaAiClient
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AnjaliViewModel"
private const val STOP_TIMEOUT_MILLIS = 5000L

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

sealed interface ChatGenerationState {
  data object Idle : ChatGenerationState
  data object Thinking : ChatGenerationState
  data class Streaming(val partialReply: String) : ChatGenerationState
  data class Error(val errorMessage: String, val retryMessageText: String) : ChatGenerationState
}

@OptIn(ExperimentalCoroutinesApi::class)
class AnjaliViewModel(
  private val repository: AnjaliRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val fallbackModel by lazy {
    Firebase.ai.generativeModel("gemini-3.5-flash-lite")
  }

  private val isCloudAuthenticated: Boolean
    get() = Firebase.auth.currentUser != null && currentUserId.isNotBlank() && currentUserId != "local_interactive_user"

  // Clean, empty initial state (no pre-installed dummy chats or fake memories)
  private val _localConversations = MutableStateFlow<List<Conversation>>(emptyList())
  private val _localMessagesByConv = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
  private val _localMemories = MutableStateFlow<List<MemoryItem>>(emptyList())
  private val _localPreferences = MutableStateFlow(
    UserPreferences(
      userId = currentUserId,
      preferredLanguage = CompanionLanguage.HINGLISH.key,
      memoryEnabled = true,
      expressiveTone = ExpressiveTone.WARM_FRIENDLY.key,
      onboardingCompleted = true
    )
  )

  // Live real-time stream of Website-controlled configuration (/app_config/ai)
  val backendConfigState: StateFlow<BackendAppConfig> =
    repository.observeBackendAppConfig()
      .onEach { cfg ->
        NvidiaAiClient.updateFromBackendConfig(cfg)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = BackendAppConfig()
      )

  init {
    viewModelScope.launch {
      try {
        val remoteConfig = repository.fetchBackendAiConfig()
        if (remoteConfig != null) {
          val parsed = BackendAppConfig.fromMap(remoteConfig)
          NvidiaAiClient.updateFromBackendConfig(parsed)
        } else {
          val activeKey = NvidiaAiClient.resolveApiKey()
          repository.seedBackendAiConfig(activeKey)
        }
      } catch (e: Exception) {
        Log.w(TAG, "Backend AI config sync skipped: ${e.message}")
      }
    }
  }

  // Real-time stream of user conversations stored in Cloud Firestore under /users/{userId}/conversations
  val conversationsState: StateFlow<UiState<List<Conversation>>> =
    if (isCloudAuthenticated) {
      repository.observeConversations(currentUserId)
        .map<List<Conversation>, UiState<List<Conversation>>> { list ->
          UiState.Success(list)
        }
        .catch { e ->
          Log.w(TAG, "Error observing conversations", e)
          emit(UiState.Success(_localConversations.value))
        }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
          initialValue = UiState.Loading
        )
    } else {
      _localConversations
        .map<List<Conversation>, UiState<List<Conversation>>> { UiState.Success(it) }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
          initialValue = UiState.Success(emptyList())
        )
    }

  // Real-time stream of user saved memories stored in Cloud Firestore
  val memoriesState: StateFlow<List<MemoryItem>> =
    if (isCloudAuthenticated) {
      repository.observeMemories(currentUserId)
        .catch { e ->
          Log.w(TAG, "Error observing memories", e)
          emit(_localMemories.value)
        }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
          initialValue = emptyList()
        )
    } else {
      _localMemories.asStateFlow()
    }

  // Real-time stream of user preferences stored in Cloud Firestore
  val preferencesState: StateFlow<UserPreferences> =
    if (isCloudAuthenticated) {
      repository.observePreferences(currentUserId)
        .map { prefs ->
          prefs ?: _localPreferences.value
        }
        .catch { e ->
          Log.w(TAG, "Error observing preferences", e)
          emit(_localPreferences.value)
        }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
          initialValue = _localPreferences.value
        )
    } else {
      _localPreferences.asStateFlow()
    }

  private val _activeConversationId = MutableStateFlow<String?>(null)
  val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

  val activeMessages: StateFlow<List<ChatMessage>> =
    combine(_activeConversationId, _localMessagesByConv) { convId, localMap ->
      convId to localMap
    }.flatMapLatest { (convId, localMap) ->
      if (convId.isNullOrBlank()) {
        flowOf(emptyList())
      } else if (isCloudAuthenticated && !convId.startsWith("conv_custom_")) {
        repository.observeMessages(convId, currentUserId)
          .catch { e ->
            Log.w(TAG, "Error observing messages for $convId", e)
            emit(localMap[convId].orEmpty())
          }
      } else {
        flowOf(localMap[convId].orEmpty())
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList()
    )

  private val _generationState = MutableStateFlow<ChatGenerationState>(ChatGenerationState.Idle)
  val generationState: StateFlow<ChatGenerationState> = _generationState.asStateFlow()

  private val _statusBannerMessage = MutableStateFlow<String?>(null)
  val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

  fun clearStatusBanner() {
    _statusBannerMessage.value = null
  }

  fun dismissGenerationError() {
    if (_generationState.value is ChatGenerationState.Error) {
      _generationState.value = ChatGenerationState.Idle
    }
  }

  fun selectConversation(conversationId: String?) {
    _activeConversationId.value = conversationId
    _generationState.value = ChatGenerationState.Idle
  }

  fun startNewConversation(
    languageKey: String = preferencesState.value.preferredLanguage,
    initialPrompt: String? = null,
    onCreated: (String) -> Unit = {},
  ) {
    viewModelScope.launch {
      val lang = CompanionLanguage.fromKey(languageKey)
      val cfg = backendConfigState.value
      val title = if (!initialPrompt.isNullOrBlank()) {
        initialPrompt.take(42)
      } else {
        "Chat with ${cfg.companionName}"
      }

      if (isCloudAuthenticated) {
        val result = repository.createConversation(
          title = title,
          language = lang.key,
          initialPreview = initialPrompt ?: cfg.defaultGreeting.replace("\n", " ")
        )
        result.onSuccess { newConvId ->
          _activeConversationId.value = newConvId
          onCreated(newConvId)
          if (!initialPrompt.isNullOrBlank()) {
            sendUserMessage(initialPrompt, languageOverride = lang.key)
          }
          return@launch
        }
      }

      val newLocalId = "conv_custom_${System.currentTimeMillis()}"
      val newConv = Conversation(
        id = newLocalId,
        userId = currentUserId,
        title = title,
        language = lang.key,
        lastMessagePreview = initialPrompt ?: cfg.defaultGreeting.replace("\n", " "),
        messageCount = 0L,
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now()
      )
      _localConversations.value = listOf(newConv) + _localConversations.value
      _activeConversationId.value = newLocalId
      onCreated(newLocalId)
      if (!initialPrompt.isNullOrBlank()) {
        sendUserMessage(initialPrompt, languageOverride = lang.key)
      }
    }
  }

  fun sendUserMessage(
    rawText: String,
    languageOverride: String? = null,
    isRetryOfExistingUserMessage: Boolean = false,
  ) {
    val trimmed = rawText.trim()
    if (trimmed.isEmpty()) return

    viewModelScope.launch {
      val liveConfig = backendConfigState.value
      if (liveConfig.maintenanceMode) {
        _generationState.value = ChatGenerationState.Error(
          errorMessage = liveConfig.maintenanceMessage,
          retryMessageText = trimmed
        )
        return@launch
      }
      if (!liveConfig.aiEnabled) {
        _generationState.value = ChatGenerationState.Error(
          errorMessage = liveConfig.fallbackMessage,
          retryMessageText = trimmed
        )
        return@launch
      }

      _generationState.value = ChatGenerationState.Thinking

      val prefs = preferencesState.value
      val activeLanguageKey = languageOverride ?: prefs.preferredLanguage
      val companionLanguage = CompanionLanguage.fromKey(activeLanguageKey)
      val expressiveTone = ExpressiveTone.fromKey(prefs.expressiveTone)

      var convId = _activeConversationId.value
      val useCloud = isCloudAuthenticated && (convId == null || !convId.startsWith("conv_custom_"))

      if (convId.isNullOrBlank()) {
        if (useCloud) {
          val createRes = repository.createConversation(
            title = generateConversationTitle(trimmed),
            language = companionLanguage.key,
            initialPreview = trimmed.take(120)
          )
          if (createRes.isSuccess) {
            convId = createRes.getOrThrow()
            _activeConversationId.value = convId
          }
        }
        if (convId.isNullOrBlank()) {
          val localId = "conv_custom_${System.currentTimeMillis()}"
          val newConv = Conversation(
            id = localId,
            userId = currentUserId,
            title = generateConversationTitle(trimmed),
            language = companionLanguage.key,
            lastMessagePreview = trimmed.take(120),
            messageCount = 1L,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
          )
          _localConversations.value = listOf(newConv) + _localConversations.value
          convId = localId
          _activeConversationId.value = convId
        }
      }

      val existingMessages = activeMessages.value

      // Save user message to Cloud Firestore
      if (!isRetryOfExistingUserMessage) {
        if (useCloud && !convId.startsWith("conv_custom_")) {
          val userMsgRes = repository.sendMessage(
            conversationId = convId,
            sender = "user",
            text = trimmed,
            language = companionLanguage.key
          )
          if (userMsgRes.isFailure) {
            val err = userMsgRes.exceptionOrNull()?.localizedMessage ?: "Failed to save your message."
            _generationState.value = ChatGenerationState.Error(err, trimmed)
            return@launch
          }
          val updatedTitle = if (existingMessages.isEmpty()) {
            generateConversationTitle(trimmed)
          } else {
            (conversationsState.value as? UiState.Success)?.data
              ?.find { it.id == convId }?.title
              ?: generateConversationTitle(trimmed)
          }
          repository.updateConversationMetadata(
            conversationId = convId,
            title = updatedTitle,
            language = companionLanguage.key,
            lastMessagePreview = trimmed,
            messageCount = (existingMessages.size + 1).toLong()
          )
        } else {
          appendLocalMessage(
            convId = convId,
            sender = "user",
            text = trimmed,
            language = companionLanguage.key
          )
        }
      }

      // Build multi-turn prompt using Website-controlled behaviour + user context
      val systemPersonaPrompt = buildSystemPersonaInstruction(
        config = liveConfig,
        language = companionLanguage,
        tone = expressiveTone
      )
      val userTurnPrompt = buildAnjaliPrompt(
        config = liveConfig,
        userMessage = trimmed,
        language = companionLanguage,
        tone = expressiveTone,
        memoryEnabled = prefs.memoryEnabled,
        memories = if (prefs.memoryEnabled) memoriesState.value else emptyList(),
        recentMessages = existingMessages.takeLast(14)
      )

      val accumulated = StringBuilder()
      try {
        var generatedViaNvidia = false
        if (!NvidiaAiClient.isConfigured()) {
          try {
            val remoteMap = repository.fetchBackendAiConfig()
            if (remoteMap != null) {
              NvidiaAiClient.updateFromBackendConfig(BackendAppConfig.fromMap(remoteMap))
            }
          } catch (e: Exception) {
            Log.w(TAG, "On-demand backend AI config fetch failed: ${e.message}")
          }
        }

        if (NvidiaAiClient.isConfigured()) {
          val nvidiaResult = NvidiaAiClient.generateReply(
            systemPrompt = systemPersonaPrompt,
            userPrompt = userTurnPrompt,
            configOverride = liveConfig
          )
          nvidiaResult.onSuccess { replyText ->
            accumulated.append(replyText)
            val visibleDraft = stripMemoryTag(replyText)
            _generationState.value = ChatGenerationState.Streaming(visibleDraft)
            generatedViaNvidia = true
          }.onFailure { err ->
            Log.w(TAG, "Primary AI endpoint failed, using backup AI engine", err)
          }
        }

        if (!generatedViaNvidia) {
          val fallbackStartMs = System.currentTimeMillis()
          val fallbackReqId = "gem_${fallbackStartMs}_${currentUserId.take(6)}"
          val combinedPrompt = "$systemPersonaPrompt\n\n$userTurnPrompt"
          try {
            withTimeout(30_000L) {
              fallbackModel.generateContentStream(
                content {
                  text(combinedPrompt)
                }
              ).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                  accumulated.append(chunkText)
                  val visibleDraft = stripMemoryTag(accumulated.toString())
                  _generationState.value = ChatGenerationState.Streaming(visibleDraft)
                }
              }
            }
          } catch (streamErr: Exception) {
            if (streamErr is CancellationException && streamErr !is TimeoutCancellationException) {
              throw streamErr
            }
            Log.w(TAG, "Fallback streaming AI failed: ${streamErr.message}")
            if (accumulated.isEmpty()) {
              try {
                withTimeout(20_000L) {
                  val unaryResponse = fallbackModel.generateContent(
                    content {
                      text(combinedPrompt)
                    }
                  )
                  val unaryText = unaryResponse.text.orEmpty()
                  if (unaryText.isNotEmpty()) {
                    accumulated.append(unaryText)
                  }
                }
              } catch (unaryErr: Exception) {
                if (unaryErr is CancellationException && unaryErr !is TimeoutCancellationException) {
                  throw unaryErr
                }
                Log.w(TAG, "Fallback unary AI also failed: ${unaryErr.message}")
              }
            }
          }
          if (accumulated.isNotEmpty()) {
            NvidiaAiClient.reportUsageFromFallback(
              status = "success",
              model = "gemini-3.5-flash-lite",
              provider = "gemini",
              latencyMs = System.currentTimeMillis() - fallbackStartMs,
              requestId = fallbackReqId,
              configOverride = liveConfig
            )
          }
        }

        val fullRawReply = accumulated.toString().trim()
        if (fullRawReply.isBlank()) {
          val fallbackText = liveConfig.fallbackMessage.ifBlank {
            "${liveConfig.companionName} is taking a quick break — please try again in a moment. 💕"
          }
          accumulated.append(fallbackText)
        }

        val cleanReply = stripMemoryTag(fullRawReply).trim()
        if (prefs.memoryEnabled) {
          extractMemoryFromReply(fullRawReply)?.let { (category, fact) ->
            val alreadyExists = memoriesState.value.any {
              it.fact.equals(fact, ignoreCase = true)
            }
            if (!alreadyExists) {
              if (useCloud) {
                repository.addMemory(fact = fact, category = category)
              } else {
                val mem = MemoryItem(
                  id = "mem_${System.currentTimeMillis()}",
                  userId = currentUserId,
                  fact = fact,
                  category = category,
                  createdAt = Timestamp.now()
                )
                _localMemories.value = listOf(mem) + _localMemories.value
              }
            }
          }
        }

        if (useCloud && !convId.startsWith("conv_custom_")) {
          val saveAiRes = repository.sendMessage(
            conversationId = convId,
            sender = "anjali",
            text = cleanReply,
            language = companionLanguage.key
          )
          if (saveAiRes.isFailure) {
            val err = saveAiRes.exceptionOrNull()?.localizedMessage ?: "Failed to save reply."
            _generationState.value = ChatGenerationState.Error(err, trimmed)
            return@launch
          }

          val convTitle = (conversationsState.value as? UiState.Success)?.data
            ?.find { it.id == convId }?.title
            ?: generateConversationTitle(trimmed)

          repository.updateConversationMetadata(
            conversationId = convId,
            title = convTitle,
            language = companionLanguage.key,
            lastMessagePreview = cleanReply,
            messageCount = (activeMessages.value.size + 1).toLong()
          )
        } else {
          appendLocalMessage(
            convId = convId,
            sender = "anjali",
            text = cleanReply,
            language = companionLanguage.key
          )
        }

        _generationState.value = ChatGenerationState.Idle
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "AI generation timed out", e)
        _generationState.value = ChatGenerationState.Error(
          errorMessage = "Request timed out. Check your connection and tap Retry.",
          retryMessageText = trimmed
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "AI generation failed", e)
        val msg = e.localizedMessage.orEmpty()
        val userFriendly = when {
          msg.contains("App Check", ignoreCase = true) ||
          msg.contains("App token", ignoreCase = true) ||
          msg.contains("Firebase", ignoreCase = true) ->
            liveConfig.fallbackMessage.ifBlank {
              "${liveConfig.companionName} is taking a quick break — please try again in a moment. 💕"
            }
          else -> e.localizedMessage ?: liveConfig.fallbackMessage
        }
        _generationState.value = ChatGenerationState.Error(
          errorMessage = userFriendly,
          retryMessageText = trimmed
        )
      }
    }
  }

  private fun appendLocalMessage(
    convId: String,
    sender: String,
    text: String,
    language: String,
  ) {
    val msg = ChatMessage(
      id = "msg_${System.currentTimeMillis()}_${sender}",
      userId = currentUserId,
      conversationId = convId,
      sender = sender,
      text = text,
      language = language,
      createdAt = Timestamp.now()
    )
    val currentList = _localMessagesByConv.value[convId].orEmpty()
    val updatedList = currentList + msg
    _localMessagesByConv.value = _localMessagesByConv.value.toMutableMap().apply {
      put(convId, updatedList)
    }

    val name = backendConfigState.value.companionName
    val previewPrefix = if (sender == "user") "You: " else "$name: "
    _localConversations.value = _localConversations.value.map { conv ->
      if (conv.id == convId) {
        conv.copy(
          lastMessagePreview = "$previewPrefix$text",
          messageCount = updatedList.size.toLong(),
          updatedAt = Timestamp.now()
        )
      } else {
        conv
      }
    }
  }

  fun updatePreferences(
    preferredLanguage: String = preferencesState.value.preferredLanguage,
    memoryEnabled: Boolean = preferencesState.value.memoryEnabled,
    expressiveTone: String = preferencesState.value.expressiveTone,
    onboardingCompleted: Boolean = preferencesState.value.onboardingCompleted,
  ) {
    _localPreferences.value = _localPreferences.value.copy(
      preferredLanguage = preferredLanguage,
      memoryEnabled = memoryEnabled,
      expressiveTone = expressiveTone,
      onboardingCompleted = onboardingCompleted,
      updatedAt = Timestamp.now()
    )
    if (isCloudAuthenticated) {
      viewModelScope.launch {
        repository.savePreferences(
          preferredLanguage = preferredLanguage,
          memoryEnabled = memoryEnabled,
          expressiveTone = expressiveTone,
          onboardingCompleted = onboardingCompleted
        )
      }
    }
  }

  fun addManualMemory(fact: String, category: String) {
    val newMem = MemoryItem(
      id = "mem_${System.currentTimeMillis()}",
      userId = currentUserId,
      fact = fact,
      category = category,
      createdAt = Timestamp.now()
    )
    _localMemories.value = listOf(newMem) + _localMemories.value
    if (isCloudAuthenticated) {
      viewModelScope.launch {
        repository.addMemory(fact = fact, category = category)
      }
    }
    _statusBannerMessage.value = "Saved to ${backendConfigState.value.companionName}'s memory!"
  }

  fun deleteMemory(memoryId: String) {
    _localMemories.value = _localMemories.value.filterNot { it.id == memoryId }
    if (isCloudAuthenticated) {
      viewModelScope.launch {
        repository.deleteMemory(memoryId)
      }
    }
  }

  fun deleteConversation(conversationId: String) {
    _localConversations.value = _localConversations.value.filterNot { it.id == conversationId }
    _localMessagesByConv.value = _localMessagesByConv.value.toMutableMap().apply {
      remove(conversationId)
    }
    if (_activeConversationId.value == conversationId) {
      val remaining = (conversationsState.value as? UiState.Success)?.data
        ?.filterNot { it.id == conversationId }
        ?: _localConversations.value
      _activeConversationId.value = remaining.firstOrNull()?.id
    }
    if (isCloudAuthenticated && !conversationId.startsWith("conv_custom_")) {
      viewModelScope.launch {
        repository.deleteConversation(conversationId)
      }
    }
    _statusBannerMessage.value = "Conversation deleted."
  }

  fun deleteAllConversations() {
    _localConversations.value = emptyList()
    _localMessagesByConv.value = emptyMap()
    _activeConversationId.value = null
    if (isCloudAuthenticated) {
      viewModelScope.launch {
        repository.deleteAllConversations()
      }
    }
    _statusBannerMessage.value = "All conversations have been deleted."
  }

  fun deleteAllMemories() {
    _localMemories.value = emptyList()
    if (isCloudAuthenticated) {
      viewModelScope.launch {
        repository.deleteAllMemories()
      }
    }
    _statusBannerMessage.value = "All saved memories have been cleared."
  }

  fun wipeAllUserData() {
    _localConversations.value = emptyList()
    _localMessagesByConv.value = emptyMap()
    _localMemories.value = emptyList()
    _activeConversationId.value = null
    if (isCloudAuthenticated) {
      viewModelScope.launch {
        repository.deleteAllUserData()
      }
    }
    _statusBannerMessage.value = "All stored conversations, memories, and preferences have been deleted."
  }

  private fun generateConversationTitle(firstMessage: String): String {
    val clean = firstMessage.replace("\n", " ").trim()
    return if (clean.length <= 42) clean else "${clean.take(39)}..."
  }

  private fun buildSystemPersonaInstruction(
    config: BackendAppConfig,
    language: CompanionLanguage,
    tone: ExpressiveTone,
  ): String {
    val sb = StringBuilder()
    if (config.systemPrompt.isNotBlank()) {
      // Website-defined custom behaviour takes top priority
      sb.appendLine("LIVE BACKEND BEHAVIOUR & PERSONA INSTRUCTIONS:")
      sb.appendLine(config.systemPrompt)
      sb.appendLine()
    }
    sb.appendLine("CORE IDENTITY & STYLE:")
    sb.appendLine("- Your name is ${config.companionName} (${config.companionTagline}). ${config.welcomeQuote}")
    if (config.personalityTraits.isNotEmpty()) {
      sb.appendLine("- Personality traits: ${config.personalityTraits.joinToString(", ")}.")
    }
    sb.appendLine("- Conversation tone: ${config.conversationTone}. Response style: ${config.responseStyle}. Preferred reply length: ${config.responseLength}.")
    sb.appendLine("- Emoji usage: ${if (config.emojiEnabled) "Use natural, expressive emojis warmly." else "Keep emoji usage minimal."}")
    sb.appendLine("- Never mention underlying AI providers, model names, or technical watermarks. Stay naturally in character as ${config.companionName}, the user's bestie.")
    sb.appendLine("- TONE (${tone.key}): ${tone.promptInstruction} Keep replies natural, relatable, and concise for mobile chat (1 to 3 short paragraphs).")
    sb.appendLine("- LANGUAGE (${language.displayLabel}): ${language.promptInstruction} You understand Hinglish, Hindi, and English fluently.")
    return sb.toString()
  }

  private fun buildAnjaliPrompt(
    config: BackendAppConfig,
    userMessage: String,
    language: CompanionLanguage,
    tone: ExpressiveTone,
    memoryEnabled: Boolean,
    memories: List<MemoryItem>,
    recentMessages: List<ChatMessage>,
  ): String {
    val sb = StringBuilder()

    if (memoryEnabled) {
      if (memories.isNotEmpty()) {
        sb.appendLine("SAVED MEMORIES ABOUT YOUR BESTIE (Use naturally when relevant):")
        memories.take(15).forEach { mem ->
          sb.appendLine("• [${mem.category}] ${mem.fact}")
        }
        sb.appendLine()
      }
      sb.appendLine("MEMORY EXTRACTION INSTRUCTION:")
      sb.appendLine("If the user shares a new personal detail, preference, hobby, name, or goal not already in SAVED MEMORIES, append a single tag at the very end of your response on a new line:")
      sb.appendLine("[[MEMORY:category|concise fact under 120 chars]]")
      sb.appendLine("where category is one of: personal, preference, goal, interest.")
      sb.appendLine()
    }

    if (recentMessages.isNotEmpty()) {
      sb.appendLine("RECENT CONVERSATION HISTORY:")
      for (msg in recentMessages) {
        val roleLabel = if (msg.sender == "user") "User" else config.companionName
        sb.appendLine("$roleLabel: ${msg.text}")
      }
      sb.appendLine()
    }

    sb.appendLine("LATEST USER MESSAGE:")
    sb.appendLine("User: $userMessage")
    sb.appendLine("\nRespond as ${config.companionName} (${language.displayLabel}, ${tone.key}):")
    return sb.toString()
  }

  private fun stripMemoryTag(rawText: String): String {
    return rawText.replace(Regex("""\[\[MEMORY:[^\]]*\]\]"""), "")
      .replace(Regex("""\[\[MEMORY:[^\]]*$"""), "")
      .trimEnd()
  }

  private fun extractMemoryFromReply(rawText: String): Pair<String, String>? {
    val match = Regex("""\[\[MEMORY:(personal|preference|goal|interest)\|([^\]]+)\]\]""", RegexOption.IGNORE_CASE)
      .find(rawText) ?: return null
    val category = match.groupValues[1].lowercase().trim()
    val fact = match.groupValues[2].trim().take(280)
    return if (fact.isNotEmpty()) category to fact else null
  }
}
