package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Conversation(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val language: String = "Hinglish",
  val lastMessagePreview: String = "",
  val messageCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId val id: String = "",
  val userId: String = "",
  val conversationId: String = "",
  val sender: String = "user", // "user" or "anjali"
  val text: String = "",
  val language: String = "Hinglish",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MemoryItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val fact: String = "",
  val category: String = "personal", // "personal", "preference", "goal", "interest"
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class UserPreferences(
  @DocumentId val id: String = "",
  val userId: String = "",
  val preferredLanguage: String = "Hinglish",
  val memoryEnabled: Boolean = true,
  val expressiveTone: String = "Warm & Friendly",
  val onboardingCompleted: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

/**
 * 100% Website-controllable live configuration stored in Firestore `/app_config/ai`.
 * Any change saved from the Admin Website immediately updates Anjali's behaviour, branding,
 * greeting, AI model, and API credentials in real time across all installed APKs.
 */
@IgnoreExtraProperties
data class BackendAppConfig(
  val companionName: String = "Anjali",
  val companionTagline: String = "Your Bestie",
  val onlineStatusText: String = "Online",
  val welcomeQuote: String = "Always here to listen, support and talk with you. 💕",
  val defaultGreeting: String = "Heyy! 😄\nKaise ho? Aaj ka din kaisa raha?",
  val systemPrompt: String = "",
  val personalityTraits: List<String> = listOf("caring", "playful", "supportive", "curious"),
  val conversationTone: String = "friendly",
  val responseStyle: String = "friendly",
  val responseLength: String = "medium",
  val emojiEnabled: Boolean = true,
  val aiEnabled: Boolean = true,
  val fallbackMessage: String = "Anjali is taking a quick break — please try again in a moment.",
  val apiKey: String = "",
  val apiEndpoint: String = "https://integrate.api.nvidia.com/v1/chat/completions",
  val backendUrl: String = "",
  val model: String = "nvidia/nemotron-3.5-lightning-30b-a3b",
  val provider: String = "nvidia",
  val temperature: Double = 0.75,
  val maxTokens: Int = 2048,
  val announcementBanner: String = "",
  val maintenanceMode: Boolean = false,
  val maintenanceMessage: String = "Anjali is taking a quick break. Back soon! 💕",
  val avatarUrl: String = "",
  val accentColor: String = "#ec4899",
  val chatTheme: String = "light",
  val configurationVersion: Int = 1,
) {
  companion object {
    fun fromMap(map: Map<String, Any>?): BackendAppConfig {
      if (map == null) return BackendAppConfig()

      val resolvedName = (
        (map["characterName"] as? String)?.takeIf { it.isNotBlank() }
          ?: (map["companionName"] as? String)?.takeIf { it.isNotBlank() }
          ?: (map["displayName"] as? String)?.takeIf { it.isNotBlank() }
          ?: "Anjali"
      )

      val resolvedQuote = (
        (map["shortDescription"] as? String)?.takeIf { it.isNotBlank() }
          ?: (map["welcomeQuote"] as? String)?.takeIf { it.isNotBlank() }
          ?: "Always here to listen, support and talk with you. 💕"
      )

      val resolvedGreeting = (
        (map["greetingMessage"] as? String)?.takeIf { it.isNotBlank() }
          ?: (map["defaultGreeting"] as? String)?.takeIf { it.isNotBlank() }
          ?: (map["welcomeMessage"] as? String)?.takeIf { it.isNotBlank() }
          ?: "Heyy! 😄\nKaise ho? Aaj ka din kaisa raha?"
      )

      val resolvedSystemPrompt = (
        (map["personalityInstructions"] as? String)?.takeIf { it.isNotBlank() }
          ?: (map["systemPrompt"] as? String)?.takeIf { it.isNotBlank() }
          ?: (map["additionalInstructions"] as? String)?.takeIf { it.isNotBlank() }
          ?: (map["behaviour"] as? String)?.takeIf { it.isNotBlank() }
          ?: ""
      ).trim()

      val rawTraits = (map["personalityTraits"] as? List<*>)
        ?.mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } }
        ?.takeIf { it.isNotEmpty() }
        ?: listOf("caring", "playful", "supportive", "curious")

      val bannerEnabled = (map["bannerEnabled"] as? Boolean) ?: true
      val bannerText = (map["bannerText"] as? String)?.trim().orEmpty()
      val explicitBanner = (map["announcementBanner"] as? String)?.trim().orEmpty()
      val resolvedBanner = when {
        explicitBanner.isNotEmpty() -> explicitBanner
        bannerEnabled && bannerText.isNotEmpty() -> bannerText
        else -> ""
      }

      val resolvedMaxTokens = (
        (map["maximumResponseLength"] as? Number)?.toInt()
          ?: (map["maxTokens"] as? Number)?.toInt()
          ?: (map["maxOutputTokens"] as? Number)?.toInt()
          ?: 600
      )

      val resolvedModel = (
        (map["selectedModel"] as? String)?.takeIf { it.isNotBlank() }
          ?: (map["model"] as? String)?.takeIf { it.isNotBlank() }
          ?: "nvidia/nemotron-3.5-lightning-30b-a3b"
      )

      val isMaintenance = (map["maintenanceMode"] as? Boolean) ?: false

      return BackendAppConfig(
        companionName = resolvedName,
        companionTagline = (map["companionTagline"] as? String)?.takeIf { it.isNotBlank() } ?: "Your Bestie",
        onlineStatusText = (map["onlineStatusText"] as? String)?.takeIf { it.isNotBlank() }
          ?: if (isMaintenance) "Maintenance" else "Online",
        welcomeQuote = resolvedQuote,
        defaultGreeting = resolvedGreeting,
        systemPrompt = resolvedSystemPrompt,
        personalityTraits = rawTraits,
        conversationTone = (map["conversationTone"] as? String)?.takeIf { it.isNotBlank() } ?: "friendly",
        responseStyle = (map["responseStyle"] as? String)?.takeIf { it.isNotBlank() } ?: "friendly",
        responseLength = (map["responseLength"] as? String)?.takeIf { it.isNotBlank() } ?: "medium",
        emojiEnabled = (map["emojiEnabled"] as? Boolean) ?: true,
        aiEnabled = (map["aiEnabled"] as? Boolean) ?: true,
        fallbackMessage = (map["fallbackMessage"] as? String)?.takeIf { it.isNotBlank() }
          ?: "Anjali is taking a quick break — please try again in a moment.",
        apiKey = (map["apiKey"] as? String).orEmpty().trim(),
        apiEndpoint = (map["apiEndpoint"] as? String ?: map["endpoint"] as? String)
          ?.takeIf { it.isNotBlank() }
          ?: "https://integrate.api.nvidia.com/v1/chat/completions",
        backendUrl = (map["backendUrl"] as? String)
          ?.trim()
          ?.trimEnd('/')
          .orEmpty(),
        model = resolvedModel,
        provider = (map["provider"] as? String)?.takeIf { it.isNotBlank() } ?: "nvidia",
        temperature = (map["temperature"] as? Number)?.toDouble() ?: 0.75,
        maxTokens = resolvedMaxTokens,
        announcementBanner = resolvedBanner,
        maintenanceMode = isMaintenance,
        maintenanceMessage = (map["maintenanceMessage"] as? String)?.takeIf { it.isNotBlank() }
          ?: "Anjali is taking a quick break. Back soon! 💕",
        avatarUrl = (map["avatarUrl"] as? String).orEmpty().trim(),
        accentColor = (map["accentColor"] as? String)?.takeIf { it.isNotBlank() } ?: "#ec4899",
        chatTheme = (map["chatTheme"] as? String)?.takeIf { it.isNotBlank() } ?: "light",
        configurationVersion = (map["configurationVersion"] as? Number)?.toInt() ?: 1
      )
    }
  }
}

enum class CompanionLanguage(
  val key: String,
  val displayLabel: String,
  val nativeBadge: String,
  val sampleGreeting: String,
  val promptInstruction: String,
) {
  HINGLISH(
    key = "Hinglish",
    displayLabel = "Hinglish",
    nativeBadge = "Namaste + English",
    sampleGreeting = "Heyy! 😄 Kaise ho? Aaj ka din kaisa raha?",
    promptInstruction = "Respond naturally in conversational Hinglish (a warm, natural blend of Hindi written in Roman script and English, just like friendly everyday Indian bestie conversations). Match the user's script and language balance."
  ),
  ENGLISH(
    key = "English",
    displayLabel = "English",
    nativeBadge = "English",
    sampleGreeting = "Hey there! I'm Anjali, your bestie 💕 What's on your mind today?",
    promptInstruction = "Respond naturally in warm, expressive English like a close bestie. If the user includes Hindi words, understand them seamlessly."
  ),
  HINDI(
    key = "Hindi",
    displayLabel = "Hindi (हिन्दी)",
    nativeBadge = "देवनागरी",
    sampleGreeting = "नमस्ते! मैं अंजलि हूँ, आपकी बेस्टी 💕 आज आपका दिन कैसा रहा?",
    promptInstruction = "Respond naturally in warm, friendly conversational Hindi using Devanagari script (हिन्दी), avoiding overly formal or textbook vocabulary."
  );

  companion object {
    fun fromKey(key: String): CompanionLanguage =
      entries.find { it.key.equals(key, ignoreCase = true) } ?: HINGLISH
  }
}

enum class ExpressiveTone(
  val key: String,
  val subtitle: String,
  val promptInstruction: String,
) {
  WARM_FRIENDLY(
    key = "Warm & Friendly",
    subtitle = "Caring, empathetic, and uplifting bestie conversation",
    promptInstruction = "Be warm, empathetic, attentive, and genuinely caring like a close best friend (bestie)."
  ),
  PLAYFUL_WITTY(
    key = "Playful & Witty",
    subtitle = "Lighthearted banter, cheerful energy, and clever humor",
    promptInstruction = "Be cheerful, lighthearted, playfully witty, and spirited like a fun bestie while staying kind."
  ),
  CALM_SUPPORTIVE(
    key = "Calm & Supportive",
    subtitle = "Gentle, grounding, and mindful listener for peaceful chats",
    promptInstruction = "Be calm, soothing, patient, and deeply supportive like a comforting bestie."
  );

  companion object {
    fun fromKey(key: String): ExpressiveTone =
      entries.find { it.key.equals(key, ignoreCase = true) } ?: WARM_FRIENDLY
  }
}
