package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.AnjaliRepository
import com.example.ui.screens.AnjaliProfileScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ConversationHistoryScreen
import com.example.ui.screens.SettingsPrivacyScreen
import com.example.ui.screens.WelcomeSignInScreen
import com.example.ui.theme.AnjaliPinkStart
import com.example.ui.theme.AnjaliTheme
import com.example.ui.theme.AnjaliThemeMode
import com.example.util.authStateFlow
import com.example.util.configureAppCheck
import com.example.util.signOutUser
import com.example.viewmodel.AnjaliViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // STEP 1: Mandatory App Check configuration with launch intent before any UI or Firebase calls
    configureAppCheck(this, intent)

    enableEdgeToEdge()

    // STEP 2: Render Compose UI
    setContent {
      var themeMode by rememberSaveable { mutableStateOf(AnjaliThemeMode.LIGHT) }
      AnjaliTheme(themeMode = themeMode) {
        AnjaliAppRoot(
          themeMode = themeMode,
          onCycleThemeMode = { themeMode = themeMode.next() }
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    configureAppCheck(this, intent)
  }
}

private enum class AnjaliTab(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
) {
  CHAT(
    route = "chat",
    labelRes = R.string.nav_chat,
    selectedIcon = Icons.Filled.ChatBubble,
    unselectedIcon = Icons.Outlined.ChatBubbleOutline
  ),
  HISTORY(
    route = "history",
    labelRes = R.string.nav_history,
    selectedIcon = Icons.Filled.History,
    unselectedIcon = Icons.Outlined.History
  ),
  PROFILE(
    route = "profile",
    labelRes = R.string.nav_profile,
    selectedIcon = Icons.Filled.Person,
    unselectedIcon = Icons.Outlined.PersonOutline
  ),
  SETTINGS(
    route = "settings",
    labelRes = R.string.nav_settings,
    selectedIcon = Icons.Filled.Settings,
    unselectedIcon = Icons.Outlined.Settings
  )
}

@Composable
fun AnjaliAppRoot(
  auth: FirebaseAuth = Firebase.auth,
  themeMode: AnjaliThemeMode = AnjaliThemeMode.LIGHT,
  onCycleThemeMode: () -> Unit = {},
) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  // Mandatory Google Authentication: user must sign in with Google to use the app
  if (user == null) {
    WelcomeSignInScreen(
      onAuthSuccess = { /* Auth state flow automatically transitions once signed in */ }
    )
  } else {
    AuthenticatedAnjaliShell(
      userId = user.uid,
      userEmail = user.email,
      userDisplayName = user.displayName,
      themeMode = themeMode,
      onCycleThemeMode = onCycleThemeMode,
      onSignedOut = { /* Handled automatically by authStateFlow */ }
    )
  }
}

@Composable
private fun AuthenticatedAnjaliShell(
  userId: String,
  userEmail: String?,
  userDisplayName: String?,
  themeMode: AnjaliThemeMode,
  onCycleThemeMode: () -> Unit,
  onSignedOut: () -> Unit,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  // Keyed by userId so switching accounts creates a clean ViewModel instance
  val viewModel: AnjaliViewModel = viewModel(
    key = userId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        AnjaliViewModel(
          repository = AnjaliRepository(db),
          currentUserId = userId
        )
      }
    }
  )

  val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
  val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
  val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
  val memories by viewModel.memoriesState.collectAsStateWithLifecycle()
  val preferences by viewModel.preferencesState.collectAsStateWithLifecycle()
  val generationState by viewModel.generationState.collectAsStateWithLifecycle()
  val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
  val backendConfig by viewModel.backendConfigState.collectAsStateWithLifecycle()

  var currentTab by rememberSaveable { mutableStateOf(AnjaliTab.CHAT) }
  val snackbarHostState = remember { SnackbarHostState() }

  // Auto-select most recent conversation if none is selected and conversations exist
  LaunchedEffect(conversationsState) {
    val convs = (conversationsState as? UiState.Success)?.data
    if (activeConversationId == null && !convs.isNullOrEmpty()) {
      viewModel.selectConversation(convs.first().id)
    }
  }

  LaunchedEffect(statusBannerMessage) {
    statusBannerMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusBanner()
    }
  }

  // Handle system back press when on secondary tabs to return to main Chat tab
  BackHandler(enabled = currentTab != AnjaliTab.CHAT) {
    currentTab = AnjaliTab.CHAT
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
      ) {
        AnjaliTab.entries.forEach { tab ->
          val selected = currentTab == tab
          NavigationBarItem(
            selected = selected,
            onClick = { currentTab = tab },
            icon = {
              Icon(
                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = stringResource(tab.labelRes)
              )
            },
            label = {
              Text(
                text = stringResource(tab.labelRes),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = Color.White,
              selectedTextColor = AnjaliPinkStart,
              indicatorColor = AnjaliPinkStart,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_tab_${tab.route}")
          )
        }
      }
    }
  ) { innerPadding ->
    when (currentTab) {
      AnjaliTab.CHAT -> {
        ChatScreen(
          messages = activeMessages,
          preferences = preferences,
          generationState = generationState,
          onSendMessage = { text -> viewModel.sendUserMessage(text) },
          onRetryMessage = { text ->
            viewModel.sendUserMessage(
              rawText = text,
              isRetryOfExistingUserMessage = true
            )
          },
          onDismissError = { viewModel.dismissGenerationError() },
          onStartNewChat = {
            viewModel.selectConversation(null)
          },
          onLanguageChange = { newLang ->
            viewModel.updatePreferences(preferredLanguage = newLang)
          },
          onCompleteOnboarding = { lang, tone, memEnabled ->
            viewModel.updatePreferences(
              preferredLanguage = lang,
              expressiveTone = tone,
              memoryEnabled = memEnabled,
              onboardingCompleted = true
            )
          },
          onOpenProfile = { currentTab = AnjaliTab.PROFILE },
          onOpenHistory = { currentTab = AnjaliTab.HISTORY },
          backendConfig = backendConfig,
          modifier = Modifier.padding(innerPadding)
        )
      }

      AnjaliTab.PROFILE -> {
        AnjaliProfileScreen(
          preferences = preferences,
          memories = memories,
          onUpdatePreferences = { lang, tone, memEnabled ->
            viewModel.updatePreferences(
              preferredLanguage = lang,
              expressiveTone = tone,
              memoryEnabled = memEnabled
            )
          },
          onAddMemory = { fact, category ->
            viewModel.addManualMemory(fact, category)
          },
          onDeleteMemory = { memId ->
            viewModel.deleteMemory(memId)
          },
          onStartTopicChat = { prompt, lang ->
            currentTab = AnjaliTab.CHAT
            viewModel.startNewConversation(
              languageKey = lang,
              initialPrompt = prompt
            )
          },
          backendConfig = backendConfig,
          modifier = Modifier.padding(innerPadding)
        )
      }

      AnjaliTab.HISTORY -> {
        ConversationHistoryScreen(
          conversationsState = conversationsState,
          activeConversationId = activeConversationId,
          onSelectConversation = { convId ->
            viewModel.selectConversation(convId)
            currentTab = AnjaliTab.CHAT
          },
          onDeleteConversation = { convId ->
            viewModel.deleteConversation(convId)
          },
          onStartNewConversation = {
            viewModel.selectConversation(null)
            currentTab = AnjaliTab.CHAT
          },
          onStartTopicConversation = { prompt, lang ->
            currentTab = AnjaliTab.CHAT
            viewModel.startNewConversation(
              languageKey = lang,
              initialPrompt = prompt
            )
          },
          modifier = Modifier.padding(innerPadding)
        )
      }

      AnjaliTab.SETTINGS -> {
        val convCount = (conversationsState as? UiState.Success)?.data?.size ?: 0
        SettingsPrivacyScreen(
          userEmail = userEmail,
          userDisplayName = userDisplayName,
          preferences = preferences,
          conversationCount = convCount,
          memoryCount = memories.size,
          onUpdatePreferences = { lang, tone, memEnabled ->
            viewModel.updatePreferences(
              preferredLanguage = lang,
              expressiveTone = tone,
              memoryEnabled = memEnabled
            )
          },
          onDeleteAllMemories = { viewModel.deleteAllMemories() },
          onDeleteAllConversations = { viewModel.deleteAllConversations() },
          onWipeAllData = { viewModel.wipeAllUserData() },
          onSignOut = {
            signOutUser(
              credentialManager = credentialManager,
              onSignOutComplete = onSignedOut,
              scope = coroutineScope
            )
          },
          themeMode = themeMode,
          onCycleThemeMode = onCycleThemeMode,
          backendConfig = backendConfig,
          modifier = Modifier.padding(innerPadding)
        )
      }
    }
  }
}
