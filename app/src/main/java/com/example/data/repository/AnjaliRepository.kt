package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BackendAppConfig
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.MemoryItem
import com.example.data.model.UserPreferences
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AnjaliRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  // --- REAL-TIME OBSERVATION FLOWS ---

  fun observeConversations(userId: String = auth.currentUser?.uid.orEmpty()): Flow<List<Conversation>> {
    val path = "users/$userId/conversations"
    return db.collection("users").document(userId).collection("conversations")
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeMessages(
    conversationId: String,
    userId: String = auth.currentUser?.uid.orEmpty(),
  ): Flow<List<ChatMessage>> {
    val path = "users/$userId/conversations/$conversationId/messages"
    return db.collection("users").document(userId)
      .collection("conversations").document(conversationId)
      .collection("messages")
      .whereEqualTo("userId", userId)
      .whereEqualTo("conversationId", conversationId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.createdAt ?: Timestamp.now() }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeMemories(userId: String = auth.currentUser?.uid.orEmpty()): Flow<List<MemoryItem>> {
    val path = "users/$userId/memories"
    return db.collection("users").document(userId).collection("memories")
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(MemoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observePreferences(userId: String = auth.currentUser?.uid.orEmpty()): Flow<UserPreferences?> {
    val path = "users/$userId/settings/preferences"
    return db.collection("users").document(userId)
      .collection("settings").document("preferences")
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(UserPreferences::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
        throw error
      }
  }

  // --- CONVERSATION CRUD ---

  suspend fun createConversation(
    title: String,
    language: String,
    initialPreview: String = "Say hello to Anjali!",
  ): Result<String> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val safeTitle = title.trim().ifBlank { "New Conversation" }.take(120)
    val safePreview = initialPreview.trim().take(500)
    val colRef = db.collection("users").document(uid).collection("conversations")
    val docRef = colRef.document()
    val payload = mapOf(
      "userId" to uid,
      "title" to safeTitle,
      "language" to language,
      "lastMessagePreview" to safePreview,
      "messageCount" to 0L,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun getConversationById(conversationId: String, ownerUserId: String = requireUserId()): Result<Conversation> {
    val docRef = db.collection("users").document(ownerUserId)
      .collection("conversations").document(conversationId)
    return try {
      val snapshot = docRef.get().await()
      val conv = snapshot.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?: throw NoSuchElementException("Conversation not found")
      Result.success(conv)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun getUserConversations(): Result<List<Conversation>> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val path = "users/$uid/conversations"
    return try {
      val snapshot = db.collection("users").document(uid).collection("conversations")
        .whereEqualTo("userId", uid)
        .get()
        .await()
      val list = snapshot.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
      Result.success(list)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  suspend fun updateConversationMetadata(
    conversationId: String,
    title: String,
    language: String,
    lastMessagePreview: String,
    messageCount: Long,
  ): Result<Unit> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val docRef = db.collection("users").document(uid)
      .collection("conversations").document(conversationId)
    val updates = mapOf(
      "title" to title.trim().ifBlank { "Chat with Anjali" }.take(120),
      "language" to language,
      "lastMessagePreview" to lastMessagePreview.trim().take(500),
      "messageCount" to messageCount.coerceIn(0L, 10000L),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteConversation(conversationId: String): Result<Unit> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val convRef = db.collection("users").document(uid)
      .collection("conversations").document(conversationId)
    val messagesPath = "${convRef.path}/messages"
    return try {
      val messagesSnap = convRef.collection("messages")
        .whereEqualTo("userId", uid)
        .whereEqualTo("conversationId", conversationId)
        .get()
        .await()
      val batch = db.batch()
      for (msgDoc in messagesSnap.documents) {
        batch.delete(msgDoc.reference)
      }
      batch.delete(convRef)
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, messagesPath)
      Result.failure(e)
    }
  }

  suspend fun deleteAllConversations(): Result<Unit> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val path = "users/$uid/conversations"
    return try {
      val convsSnap = db.collection("users").document(uid).collection("conversations")
        .whereEqualTo("userId", uid)
        .get()
        .await()
      for (convDoc in convsSnap.documents) {
        deleteConversation(convDoc.id).getOrThrow()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  // --- MESSAGE CRUD ---

  suspend fun sendMessage(
    conversationId: String,
    sender: String,
    text: String,
    language: String,
  ): Result<String> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val safeText = text.trim().take(8000)
    require(safeText.isNotEmpty()) { "Message text must not be empty" }
    val msgRef = db.collection("users").document(uid)
      .collection("conversations").document(conversationId)
      .collection("messages").document()
    val payload = mapOf(
      "userId" to uid,
      "conversationId" to conversationId,
      "sender" to sender,
      "text" to safeText,
      "language" to language,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    return try {
      msgRef.set(payload).await()
      Result.success(msgRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, msgRef.path)
      Result.failure(e)
    }
  }

  // --- MEMORY CRUD ---

  suspend fun addMemory(fact: String, category: String = "personal"): Result<String> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val safeFact = fact.trim().take(300)
    if (safeFact.isEmpty()) return Result.failure(IllegalArgumentException("Memory fact cannot be empty"))
    val validCategory = if (category in setOf("personal", "preference", "goal", "interest")) {
      category
    } else {
      "personal"
    }
    val memRef = db.collection("users").document(uid).collection("memories").document()
    val payload = mapOf(
      "userId" to uid,
      "fact" to safeFact,
      "category" to validCategory,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      memRef.set(payload).await()
      Result.success(memRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, memRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteMemory(memoryId: String): Result<Unit> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val docRef = db.collection("users").document(uid).collection("memories").document(memoryId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteAllMemories(): Result<Unit> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val path = "users/$uid/memories"
    return try {
      val snap = db.collection("users").document(uid).collection("memories")
        .whereEqualTo("userId", uid)
        .get()
        .await()
      val batch = db.batch()
      for (doc in snap.documents) {
        batch.delete(doc.reference)
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  // --- PREFERENCES CRUD ---

  suspend fun savePreferences(
    preferredLanguage: String,
    memoryEnabled: Boolean,
    expressiveTone: String,
    onboardingCompleted: Boolean,
  ): Result<Unit> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val docRef = db.collection("users").document(uid)
      .collection("settings").document("preferences")
    val payload = mapOf(
      "userId" to uid,
      "preferredLanguage" to preferredLanguage,
      "memoryEnabled" to memoryEnabled,
      "expressiveTone" to expressiveTone,
      "onboardingCompleted" to onboardingCompleted,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteAllUserData(): Result<Unit> {
    val uid = try {
      requireUserId()
    } catch (e: Exception) {
      return Result.failure(e)
    }
    return try {
      deleteAllConversations().getOrThrow()
      deleteAllMemories().getOrThrow()
      val prefRef = db.collection("users").document(uid)
        .collection("settings").document("preferences")
      val prefSnap = prefRef.get().await()
      if (prefSnap.exists()) {
        prefRef.delete().await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "users/$uid")
      Result.failure(e)
    }
  }

  fun observeBackendAppConfig(): Flow<BackendAppConfig> {
    return db.collection("app_config").document("ai")
      .snapshots()
      .map { snap ->
        if (snap.exists()) {
          BackendAppConfig.fromMap(snap.data)
        } else {
          BackendAppConfig()
        }
      }
      .catch {
        emit(BackendAppConfig())
      }
  }

  suspend fun fetchBackendAiConfig(): Map<String, Any>? {
    return try {
      val snap = db.collection("app_config").document("ai").get().await()
      if (snap.exists()) snap.data else null
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      null
    }
  }

  suspend fun seedBackendAiConfig(apiKey: String, provider: String = "nvidia"): Result<Unit> {
    val docRef = db.collection("app_config").document("ai")
    return try {
      val existing = docRef.get().await()
      if (!existing.exists()) {
        val payload = mapOf(
          "companionName" to "Anjali",
          "companionTagline" to "Your Bestie",
          "onlineStatusText" to "Online",
          "welcomeQuote" to "Always here to listen, support and talk with you. 💕",
          "defaultGreeting" to "Heyy! 😄\nKaise ho? Aaj ka din kaisa raha?",
          "systemPrompt" to "You are Anjali, the user's warm, caring, playful, and supportive best friend (Bestie). Talk naturally like a real bestie in Hinglish, Hindi, or English depending on how the user talks. Keep replies expressive, relatable, and concise.",
          "apiKey" to apiKey,
          "apiEndpoint" to "https://integrate.api.nvidia.com/v1/chat/completions",
          "provider" to provider,
          "model" to "meta/llama-3.3-70b-instruct",
          "temperature" to 0.75,
          "maxTokens" to 600,
          "announcementBanner" to "",
          "maintenanceMode" to false,
          "maintenanceMessage" to "Anjali is taking a quick break. Back soon! 💕",
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload, SetOptions.merge()).await()
      } else if (apiKey.isNotBlank() && (existing.getString("apiKey").isNullOrBlank())) {
        docRef.set(
          mapOf(
            "apiKey" to apiKey,
            "updatedAt" to FieldValue.serverTimestamp()
          ),
          SetOptions.merge()
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}

