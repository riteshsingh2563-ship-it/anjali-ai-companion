package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AnjaliRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createAndObserveConversations_authenticatedOwner_succeeds() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repository = AnjaliRepository(firestore, auth)

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createConversation(
        title = "Evening Chai Chat",
        language = "Hinglish",
        initialPreview = "Namaste Anjali!"
      )
    }
    assertTrue("Expected conversation creation to succeed", createResult.isSuccess)
    val convId = createResult.getOrThrow()
    assertTrue(convId.isNotEmpty())

    val msgResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.sendMessage(
        conversationId = convId,
        sender = "user",
        text = "Kaise ho Anjali?",
        language = "Hinglish"
      )
    }
    assertTrue("Expected sending message to succeed", msgResult.isSuccess)

    val emittedConvs = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeConversations(aliceUid).first { list -> list.any { it.id == convId } }
    }
    assertTrue(emittedConvs.any { it.id == convId })

    val emittedMessages = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeMessages(convId, aliceUid).first { list -> list.isNotEmpty() }
    }
    assertEquals("Kaise ho Anjali?", emittedMessages.first().text)
  }

  @Test
  fun memoriesAndPreferences_authenticatedOwner_succeeds() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repository = AnjaliRepository(firestore, auth)

    val memResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.addMemory("Loves filter coffee and Hindi indie music", "preference")
    }
    assertTrue(memResult.isSuccess)
    val memId = memResult.getOrThrow()

    val memories = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeMemories(aliceUid).first { list -> list.any { it.id == memId } }
    }
    assertTrue(memories.any { it.id == memId })

    val delMem = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.deleteMemory(memId)
    }
    assertTrue(delMem.isSuccess)

    val prefResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.savePreferences(
        preferredLanguage = "Hinglish",
        memoryEnabled = true,
        expressiveTone = "Warm & Friendly",
        onboardingCompleted = true
      )
    }
    assertTrue(prefResult.isSuccess)
  }

  @Test
  fun getConversationById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = AnjaliRepository(firestore, auth)
    val convId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createConversation("Private Chat", "English").getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = AnjaliRepository(firestore, auth)
    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.getConversationById(convId, ownerUserId = aliceUid)
    }
    assertTrue("Expected cross-user access to fail", result.isFailure)
    val ex = result.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull("Expected FirebaseFirestoreException", ex)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
  }

  @Test
  fun observeConversations_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    auth.signOut()
    val repository = AnjaliRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeConversations(aliceUid).first()
      }
      fail("Expected unauthenticated query to throw exception")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@test.com"
    const val BOB_EMAIL = "bob@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
