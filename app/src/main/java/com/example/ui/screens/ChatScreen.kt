package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BackendAppConfig
import com.example.data.model.ChatMessage
import com.example.data.model.CompanionLanguage
import com.example.data.model.UserPreferences
import com.example.ui.components.AiDisclosureBanner
import com.example.ui.components.AnjaliAvatarIllustration
import com.example.ui.components.AnjaliExpression
import com.example.ui.components.AnjaliExpressionShowcaseRow
import com.example.ui.components.TypingIndicatorBubble
import com.example.ui.components.VoiceNoteWaveformCard
import com.example.ui.theme.AnjaliIncomingPinkBubbleDark
import com.example.ui.theme.AnjaliIncomingPinkBubbleLight
import com.example.ui.theme.AnjaliOnlineGreen
import com.example.ui.theme.AnjaliPinkEnd
import com.example.ui.theme.AnjaliPinkStart
import com.example.ui.theme.AnjaliPurpleEnd
import com.example.ui.theme.AnjaliVioletBubbleEnd
import com.example.ui.theme.AnjaliVioletBubbleStart
import com.example.viewmodel.ChatGenerationState
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
  messages: List<ChatMessage>,
  preferences: UserPreferences,
  generationState: ChatGenerationState,
  onSendMessage: (String) -> Unit,
  onRetryMessage: (String) -> Unit,
  onDismissError: () -> Unit,
  onStartNewChat: () -> Unit,
  onLanguageChange: (String) -> Unit,
  onCompleteOnboarding: (language: String, tone: String, memoryEnabled: Boolean) -> Unit,
  onOpenProfile: () -> Unit,
  onOpenHistory: () -> Unit = {},
  backendConfig: BackendAppConfig = BackendAppConfig(),
  modifier: Modifier = Modifier,
) {
  var inputText by rememberSaveable { mutableStateOf("") }
  var showLanguageMenu by remember { mutableStateOf(false) }
  var showMoreMenu by remember { mutableStateOf(false) }
  var showQuickTray by rememberSaveable { mutableStateOf(false) }
  var showEmojiRow by rememberSaveable { mutableStateOf(false) }
  var showVoiceNoteCard by rememberSaveable { mutableStateOf(true) }
  var manualExpressionOverride by remember { mutableStateOf<AnjaliExpression?>(null) }
  var isSpeakingTts by remember { mutableStateOf(false) }

  val listState = rememberLazyListState()
  val context = LocalContext.current

  // Optional Android TextToSpeech for the voice note & audio button
  var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
  DisposableEffect(context) {
    var instance: TextToSpeech? = null
    instance = TextToSpeech(context) { status ->
      if (status == TextToSpeech.SUCCESS) {
        instance?.language = Locale.forLanguageTag("en-IN")
      }
    }
    ttsEngine = instance
    onDispose {
      instance?.stop()
      instance?.shutdown()
    }
  }

  val speakText: (String) -> Unit = { textToSpeak ->
    val engine = ttsEngine
    if (engine != null) {
      if (isSpeakingTts) {
        engine.stop()
        isSpeakingTts = false
      } else {
        isSpeakingTts = true
        engine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "anjali_utt")
      }
    } else {
      isSpeakingTts = !isSpeakingTts
    }
  }

  val isBusy = generationState is ChatGenerationState.Thinking ||
    generationState is ChatGenerationState.Streaming

  val activeHeaderExpression = remember(manualExpressionOverride, messages, generationState) {
    manualExpressionOverride ?: when {
      generationState is ChatGenerationState.Thinking -> AnjaliExpression.THINKING
      messages.isNotEmpty() -> AnjaliExpression.inferFromText(messages.last().text)
      else -> AnjaliExpression.DEFAULT
    }
  }

  // Auto-scroll to bottom when messages or streaming state changes
  LaunchedEffect(messages.size, generationState) {
    val totalItems = messages.size + (if (generationState !is ChatGenerationState.Idle) 1 else 0)
    if (totalItems > 0) {
      listState.animateScrollToItem(totalItems)
    }
  }

  val isDark = isSystemInDarkTheme()
  val chatBgBrush = if (isDark) {
    Brush.verticalGradient(
      colors = listOf(
        MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.surface
      )
    )
  } else {
    Brush.verticalGradient(
      colors = listOf(
        Color(0xFFFBE9F3),
        Color(0xFFFFF8FC),
        Color(0xFFF6ECFC)
      )
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(chatBgBrush)
  ) {
    // Reference Screen 3 Header Bar:
    // <- | Avatar | Anjali + green dot Online | Language chip | Phone/Voice | New Chat | ...
    Surface(
      color = Color.Transparent,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onOpenHistory,
          modifier = Modifier.size(40.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Conversations",
            tint = MaterialTheme.colorScheme.onBackground
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenProfile() }
            .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
          AnjaliAvatarIllustration(
            size = 44.dp,
            showAiBadge = false,
            isSpeaking = isBusy || isSpeakingTts,
            expression = activeHeaderExpression,
            showOnlineDot = true
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = backendConfig.companionName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
              ) {
                Text(
                  text = backendConfig.companionTagline,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                )
              }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(AnjaliOnlineGreen)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = if (isBusy) "Typing…" else "${backendConfig.onlineStatusText} • ${activeHeaderExpression.label}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // Language selector chip (kept with exact testTag)
        Box {
          AssistChip(
            onClick = { showLanguageMenu = true },
            label = {
              Text(
                text = CompanionLanguage.fromKey(preferences.preferredLanguage).key,
                style = MaterialTheme.typography.labelSmall
              )
            },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = "Select conversation language",
                modifier = Modifier.size(14.dp)
              )
            },
            modifier = Modifier.testTag("language_selector_chip")
          )
          DropdownMenu(
            expanded = showLanguageMenu,
            onDismissRequest = { showLanguageMenu = false }
          ) {
            CompanionLanguage.entries.forEach { lang ->
              DropdownMenuItem(
                text = {
                  Column {
                    Text(lang.displayLabel, fontWeight = FontWeight.SemiBold)
                    Text(
                      lang.nativeBadge,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                },
                onClick = {
                  showLanguageMenu = false
                  onLanguageChange(lang.key)
                }
              )
            }
          }
        }

        // Pink Phone / Voice Speak action from Reference Screen 3
        IconButton(
          onClick = {
            val latestAnjaliText = messages.lastOrNull { it.sender != "user" }?.text
              ?: "Heyy! Main Anjali hoon, aapki AI companion. Aaj ka din kaisa raha?"
            speakText(latestAnjaliText)
          },
          modifier = Modifier.size(38.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Call,
            contentDescription = "Listen to Anjali's voice",
            tint = AnjaliPinkStart,
            modifier = Modifier.size(20.dp)
          )
        }

        // New Chat button (kept with exact testTag)
        IconButton(
          onClick = onStartNewChat,
          modifier = Modifier
            .size(38.dp)
            .testTag("new_chat_button")
        ) {
          Icon(
            imageVector = Icons.Default.AddComment,
            contentDescription = stringResource(R.string.new_chat_desc),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }

        // More (...) menu button from Reference Screen 3
        Box {
          IconButton(
            onClick = { showMoreMenu = true },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.MoreHoriz,
              contentDescription = "More chat options",
              tint = MaterialTheme.colorScheme.onBackground
            )
          }
          DropdownMenu(
            expanded = showMoreMenu,
            onDismissRequest = { showMoreMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Change Avatar Expression") },
              onClick = {
                showMoreMenu = false
                showQuickTray = !showQuickTray
              }
            )
            DropdownMenuItem(
              text = { Text(if (showVoiceNoteCard) "Hide Voice Greeting" else "Show Voice Greeting") },
              onClick = {
                showMoreMenu = false
                showVoiceNoteCard = !showVoiceNoteCard
              }
            )
            DropdownMenuItem(
              text = { Text("Start New Chat") },
              onClick = {
                showMoreMenu = false
                onStartNewChat()
              }
            )
            DropdownMenuItem(
              text = { Text("${backendConfig.companionName}'s Profile & Memories") },
              onClick = {
                showMoreMenu = false
                onOpenProfile()
              }
            )
          }
        }
      }
    }

    // Message List + Onboarding / Starter Prompts
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Live Website Announcement Banner (if set from Admin Dashboard)
      if (backendConfig.announcementBanner.isNotBlank()) {
        item(key = "website_announcement_banner") {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, AnjaliPinkStart.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = AnjaliPinkStart,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = backendConfig.announcementBanner,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }
        }
      }

      // Show onboarding customization card if user hasn't completed it yet
      if (!preferences.onboardingCompleted) {
        item(key = "onboarding_card") {
          OnboardingCustomizationCard(
            currentPreferences = preferences,
            onCompleteOnboarding = onCompleteOnboarding
          )
        }
      }

      // Empty state with reference conversation starters & Voice Note player
      if (messages.isEmpty() && generationState is ChatGenerationState.Idle) {
        item(key = "empty_chat_welcome") {
          EmptyChatStarterCard(
            companionName = backendConfig.companionName,
            defaultGreeting = backendConfig.defaultGreeting,
            selectedLanguage = CompanionLanguage.fromKey(preferences.preferredLanguage),
            activeExpression = activeHeaderExpression,
            isPlayingVoice = isSpeakingTts,
            onTogglePlayVoice = {
              speakText(backendConfig.defaultGreeting)
            },
            onStarterClick = { starterPrompt ->
              onSendMessage(starterPrompt)
            }
          )
        }
      } else if (showVoiceNoteCard) {
        // Optional interactive Voice Note component
        item(key = "voice_note_bar") {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
          ) {
            AnjaliAvatarIllustration(
              size = 34.dp,
              showAiBadge = false,
              expression = activeHeaderExpression
            )
            Spacer(modifier = Modifier.width(8.dp))
            VoiceNoteWaveformCard(
              isPlaying = isSpeakingTts,
              onTogglePlay = {
                val textToRead = messages.lastOrNull { it.sender != "user" }?.text
                  ?: backendConfig.defaultGreeting
                speakText(textToRead)
              },
              durationLabel = "0:08",
              timestampLabel = "Just now",
              caption = "${backendConfig.companionName} Voice Note 💕"
            )
          }
        }
      }

      // Conversation messages
      items(
        items = messages,
        key = { msg -> msg.id.ifBlank { "${msg.sender}_${msg.text.hashCode()}_${msg.createdAt?.nanoseconds}" } }
      ) { message ->
        ChatBubbleItem(
          message = message,
          expressionOverride = manualExpressionOverride,
          onCopyText = { textToCopy ->
            copyToClipboard(context, textToCopy)
          },
          onSpeakText = { textToSpeak ->
            speakText(textToSpeak)
          }
        )
      }

      // Thinking / Typing indicator matching Reference Screen 3
      if (generationState is ChatGenerationState.Thinking) {
        item(key = "thinking_indicator") {
          Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
          ) {
            AnjaliAvatarIllustration(
              size = 34.dp,
              showAiBadge = false,
              isSpeaking = true,
              expression = AnjaliExpression.THINKING
            )
            Spacer(modifier = Modifier.width(10.dp))
            TypingIndicatorBubble()
          }
        }
      }

      // Live streaming response bubble
      if (generationState is ChatGenerationState.Streaming) {
        item(key = "streaming_bubble") {
          StreamingAnjaliBubble(
            partialText = generationState.partialReply,
            expression = AnjaliExpression.inferFromText(generationState.partialReply)
          )
        }
      }

      // Transparent Error State (NEVER shows fake AI responses)
      if (generationState is ChatGenerationState.Error) {
        item(key = "error_banner") {
          ChatErrorRetryCard(
            errorMessage = generationState.errorMessage,
            onRetry = { onRetryMessage(generationState.retryMessageText) },
            onDismiss = onDismissError
          )
        }
      }
    }

    // Expandable Quick Emoji Bar
    AnimatedVisibility(
      visible = showEmojiRow,
      enter = expandVertically() + fadeIn(),
      exit = shrinkVertically() + fadeOut()
    ) {
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          listOf("😄", "💗", "🥺", "😅", "😂", "🎵", "✨", "💪").forEach { emoji ->
            Text(
              text = emoji,
              fontSize = 22.sp,
              modifier = Modifier
                .clip(CircleShape)
                .clickable { inputText += emoji }
                .padding(6.dp)
            )
          }
        }
      }
    }

    // Expandable "+" Quick Action & Avatar Expression Tray (Matches "Chat UI Components" in Reference)
    AnimatedVisibility(
      visible = showQuickTray,
      enter = expandVertically() + fadeIn(),
      exit = shrinkVertically() + fadeOut()
    ) {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Top row: The 4 circular buttons from "Chat UI Components" (+, Gallery/Topics, Mic/Voice, Emoji)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Chat UI Actions & Avatar Expressions",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            if (manualExpressionOverride != null) {
              Text(
                text = "Auto Mood",
                style = MaterialTheme.typography.labelSmall,
                color = AnjaliPinkStart,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { manualExpressionOverride = null }
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Pink + circle
            QuickActionCircleButton(
              icon = Icons.Default.Add,
              label = "New Chat",
              isPrimaryPink = true,
              onClick = {
                showQuickTray = false
                onStartNewChat()
              }
            )
            // White Gallery/Topic starter circle
            QuickActionCircleButton(
              icon = Icons.Default.Image,
              label = "Song / Topic",
              isPrimaryPink = false,
              onClick = {
                showQuickTray = false
                onSendMessage("Haan, koi song suggest karo 🎵")
              }
            )
            // White Mic/Voice greeting circle
            QuickActionCircleButton(
              icon = Icons.Default.Mic,
              label = "Voice Note",
              isPrimaryPink = false,
              onClick = {
                showVoiceNoteCard = true
                speakText("Heyy! Kaise ho? Main theek hu, tum batao?")
              }
            )
            // White Emoji circle
            QuickActionCircleButton(
              icon = Icons.Default.SentimentSatisfiedAlt,
              label = "Emojis",
              isPrimaryPink = false,
              onClick = {
                showEmojiRow = !showEmojiRow
              }
            )
          }

          // Bottom row: The 5 Anjali Avatar Expressions (Default, Happy, Cute, Thinking, Sad / Concerned)
          AnjaliExpressionShowcaseRow(
            selectedExpression = activeHeaderExpression,
            onSelectExpression = { chosen ->
              manualExpressionOverride = chosen
            },
            cardSize = 68.dp
          )
        }
      }
    }

    // Bottom Message Input Bar matching Reference Screen 3 & "Chat UI Components":
    // [ + ]  [ Type a message...               😊 ]  [ 🎤 / > ]
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Circular white '+' button on the left
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        tonalElevation = 2.dp,
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .clickable { showQuickTray = !showQuickTray }
          .testTag("chat_plus_action_button")
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Open expressions and quick actions",
            tint = if (showQuickTray) AnjaliPinkStart else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(10.dp))

      // White Pill Input Field with Emoji button inside trailingIcon
      OutlinedTextField(
        value = inputText,
        onValueChange = { inputText = it },
        placeholder = {
          Text(
            text = stringResource(R.string.chat_input_placeholder),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        },
        trailingIcon = {
          IconButton(
            onClick = { showEmojiRow = !showEmojiRow }
          ) {
            Icon(
              imageVector = Icons.Default.SentimentSatisfiedAlt,
              contentDescription = "Toggle emojis",
              tint = if (showEmojiRow) AnjaliPinkStart else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        maxLines = 4,
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface,
          focusedBorderColor = AnjaliPinkStart.copy(alpha = 0.55f),
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.Sentences,
          imeAction = ImeAction.Send
        ),
        keyboardActions = KeyboardActions(
          onSend = {
            if (inputText.isNotBlank() && !isBusy) {
              val toSend = inputText
              inputText = ""
              onSendMessage(toSend)
            }
          }
        ),
        modifier = Modifier
          .weight(1f)
          .shadow(
            elevation = 3.dp,
            shape = RoundedCornerShape(28.dp),
            spotColor = AnjaliPinkStart.copy(alpha = 0.18f)
          )
          .testTag("chat_message_input")
      )

      Spacer(modifier = Modifier.width(10.dp))

      // Right circular Pink Gradient Button (Send > or Mic when blank)
      val canSend = inputText.isNotBlank() && !isBusy
      Box(
        modifier = Modifier
          .size(48.dp)
          .shadow(
            elevation = 6.dp,
            shape = CircleShape,
            spotColor = AnjaliPinkStart.copy(alpha = 0.45f)
          )
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              colors = listOf(AnjaliPinkStart, AnjaliPinkEnd)
            )
          )
          .clickable(enabled = !isBusy) {
            if (canSend) {
              val toSend = inputText
              inputText = ""
              onSendMessage(toSend)
            } else {
              // If input is blank, insert a quick Hinglish voice-style prompt or speak greeting
              inputText = "Haan, koi song suggest karo"
            }
          }
          .testTag("send_message_button"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (inputText.isNotBlank()) {
            Icons.AutoMirrored.Filled.Send
          } else {
            Icons.Default.Mic
          },
          contentDescription = stringResource(R.string.send_message_desc),
          tint = Color.White,
          modifier = Modifier.size(21.dp)
        )
      }
    }
  }
}

@Composable
private fun QuickActionCircleButton(
  icon: ImageVector,
  label: String,
  isPrimaryPink: Boolean,
  onClick: () -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable { onClick() }
      .padding(4.dp)
  ) {
    Surface(
      shape = CircleShape,
      color = if (isPrimaryPink) AnjaliPinkStart else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
      shadowElevation = if (isPrimaryPink) 4.dp else 1.dp,
      modifier = Modifier.size(44.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = if (isPrimaryPink) Color.White else MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(21.dp)
        )
      }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyChatStarterCard(
  companionName: String,
  defaultGreeting: String,
  selectedLanguage: CompanionLanguage,
  activeExpression: AnjaliExpression,
  isPlayingVoice: Boolean,
  onTogglePlayVoice: () -> Unit,
  onStarterClick: (String) -> Unit,
) {
  val starters = remember(selectedLanguage) {
    when (selectedLanguage) {
      CompanionLanguage.HINGLISH -> listOf(
        "Thoda tired hu, aaj bahut busy din tha 😢",
        "Kuch acchi baatein karte hain 😄",
        "Haan, koi sukoon wala song suggest karo 🎵",
        "Heyy! 😄 Kaise ho? Aaj ka din kaisa raha?"
      )
      CompanionLanguage.HINDI -> listOf(
        "नमस्ते! आज हम किस विषय पर बात करें?",
        "मुझे एक प्रेरणादायक छोटी कहानी सुनाओ",
        "थकान भरे दिन के बाद मन शांत कैसे करें?",
        "चलो कोई मजेदार गाना या पहेली सुझाओ!"
      )
      CompanionLanguage.ENGLISH -> listOf(
        "I had a long day today, let's relax and chat 💬",
        "Can you suggest a feel-good song for tonight? 🎵",
        "Teach me a few sweet Hindi phrases with meanings!",
        "Let's brainstorm a relaxing weekend routine ✨"
      )
    }
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    // Initial greeting bubble from Anjali (customizable from Website backend)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Start,
      verticalAlignment = Alignment.Bottom
    ) {
      AnjaliAvatarIllustration(
        size = 34.dp,
        showAiBadge = false,
        expression = activeExpression
      )
      Spacer(modifier = Modifier.width(8.dp))
      Surface(
        modifier = Modifier.widthIn(max = 300.dp),
        shape = RoundedCornerShape(
          topStart = 20.dp,
          topEnd = 20.dp,
          bottomEnd = 20.dp,
          bottomStart = 6.dp
        ),
        color = if (isSystemInDarkTheme()) AnjaliIncomingPinkBubbleDark else AnjaliIncomingPinkBubbleLight,
        shadowElevation = 2.dp
      ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
          Text(
            text = defaultGreeting,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Now",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.align(Alignment.End)
          )
        }
      }
    }

    // Voice note card
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Start,
      verticalAlignment = Alignment.Bottom
    ) {
      AnjaliAvatarIllustration(
        size = 34.dp,
        showAiBadge = false,
        expression = AnjaliExpression.CUTE
      )
      Spacer(modifier = Modifier.width(8.dp))
      VoiceNoteWaveformCard(
        isPlaying = isPlayingVoice,
        onTogglePlay = onTogglePlayVoice,
        durationLabel = "0:08",
        timestampLabel = "Now",
        caption = "$companionName Voice Note 💕"
      )
    }

    // Starter prompt chips card
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
      tonalElevation = 1.dp,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.Start
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = AnjaliPinkStart,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Quick Replies (${selectedLanguage.displayLabel})",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          starters.forEachIndexed { idx, prompt ->
            SuggestionChip(
              onClick = { onStarterClick(prompt) },
              label = {
                Text(
                  text = prompt,
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.padding(vertical = 4.dp)
                )
              },
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.testTag("starter_chip_$idx")
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ChatBubbleItem(
  message: ChatMessage,
  expressionOverride: AnjaliExpression?,
  onCopyText: (String) -> Unit,
  onSpeakText: (String) -> Unit,
) {
  val isUser = message.sender == "user"
  val formattedTime = remember(message.createdAt) {
    formatTimestamp(message.createdAt)
  }
  val isDark = isSystemInDarkTheme()

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    verticalAlignment = Alignment.Bottom
  ) {
    if (!isUser) {
      val msgExpression = expressionOverride ?: AnjaliExpression.inferFromText(message.text)
      AnjaliAvatarIllustration(
        size = 34.dp,
        showAiBadge = false,
        expression = msgExpression
      )
      Spacer(modifier = Modifier.width(8.dp))
    }

    if (isUser) {
      // Outgoing Purple-Violet Bubble matching Reference Screen 3:
      // Soft violet gradient, white text, "9:41 PM ✓✓" at bottom-right
      Box(
        modifier = Modifier
          .widthIn(max = 295.dp)
          .shadow(
            elevation = 3.dp,
            shape = RoundedCornerShape(
              topStart = 20.dp,
              topEnd = 20.dp,
              bottomStart = 20.dp,
              bottomEnd = 6.dp
            ),
            spotColor = AnjaliVioletBubbleEnd.copy(alpha = 0.35f)
          )
          .clip(
            RoundedCornerShape(
              topStart = 20.dp,
              topEnd = 20.dp,
              bottomStart = 20.dp,
              bottomEnd = 6.dp
            )
          )
          .background(
            Brush.linearGradient(
              colors = listOf(AnjaliVioletBubbleStart, AnjaliVioletBubbleEnd)
            )
          )
          .padding(horizontal = 16.dp, vertical = 11.dp)
      ) {
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = message.text,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
            color = Color.White
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
          ) {
            Text(
              text = formattedTime,
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.82f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Default.DoneAll,
              contentDescription = "Sent",
              tint = Color.White.copy(alpha = 0.88f),
              modifier = Modifier.size(13.dp)
            )
          }
        }
      }
    } else {
      // Incoming Pastel Pink Bubble matching Reference Screen 3:
      // Soft blush-pink surface, clean dark text, subtle timestamp + actions
      Surface(
        modifier = Modifier.widthIn(max = 305.dp),
        shape = RoundedCornerShape(
          topStart = 20.dp,
          topEnd = 20.dp,
          bottomEnd = 20.dp,
          bottomStart = 6.dp
        ),
        color = if (isDark) AnjaliIncomingPinkBubbleDark else AnjaliIncomingPinkBubbleLight,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, AnjaliPinkStart.copy(alpha = 0.14f))
      ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)) {
          Text(
            text = message.text,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
          ) {
            IconButton(
              onClick = { onSpeakText(message.text) },
              modifier = Modifier.size(20.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Read aloud",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(13.dp)
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
              onClick = { onCopyText(message.text) },
              modifier = Modifier.size(20.dp)
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy response",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(12.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = formattedTime,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StreamingAnjaliBubble(
  partialText: String,
  expression: AnjaliExpression = AnjaliExpression.DEFAULT,
) {
  val isDark = isSystemInDarkTheme()
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Start,
    verticalAlignment = Alignment.Bottom
  ) {
    AnjaliAvatarIllustration(
      size = 34.dp,
      showAiBadge = false,
      isSpeaking = true,
      expression = expression
    )
    Spacer(modifier = Modifier.width(8.dp))
    Surface(
      modifier = Modifier.widthIn(max = 305.dp),
      shape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomEnd = 20.dp,
        bottomStart = 6.dp
      ),
      color = if (isDark) AnjaliIncomingPinkBubbleDark else AnjaliIncomingPinkBubbleLight,
      shadowElevation = 2.dp,
      border = BorderStroke(1.dp, AnjaliPinkStart.copy(alpha = 0.35f))
    ) {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
          text = partialText,
          style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Anjali is typing…",
          style = MaterialTheme.typography.labelSmall,
          color = AnjaliPinkStart,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}

@Composable
private fun ChatErrorRetryCard(
  errorMessage: String,
  onRetry: () -> Unit,
  onDismiss: () -> Unit,
) {
  AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.errorContainer,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "AI Request Error",
            tint = MaterialTheme.colorScheme.onErrorContainer
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Could Not Generate AI Reply",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = errorMessage,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          horizontalArrangement = Arrangement.End,
          modifier = Modifier.fillMaxWidth()
        ) {
          FilledTonalButton(
            onClick = onDismiss,
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            )
          ) {
            Text("Dismiss")
          }
          Spacer(modifier = Modifier.width(8.dp))
          FilledTonalButton(
            onClick = onRetry,
            modifier = Modifier.testTag("retry_message_button")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Retry")
          }
        }
      }
    }
  }
}

private fun formatTimestamp(timestamp: Timestamp?): String {
  val date = (timestamp ?: Timestamp.now()).toDate()
  return SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
}

private fun copyToClipboard(context: Context, text: String) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
  clipboard?.setPrimaryClip(ClipData.newPlainText("Anjali AI Response", text))
}
