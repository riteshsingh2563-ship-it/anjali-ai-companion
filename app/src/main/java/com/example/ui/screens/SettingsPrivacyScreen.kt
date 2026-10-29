package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompanionLanguage
import com.example.data.model.ExpressiveTone
import com.example.data.model.UserPreferences
import com.example.ui.components.AiDisclosureBanner
import com.example.ui.components.AnjaliAvatarIllustration
import com.example.ui.components.AnjaliExpression
import com.example.ui.theme.AnjaliOnlineGreen
import com.example.ui.theme.AnjaliPinkStart
import com.example.ui.theme.AnjaliSignOutPinkBg
import com.example.ui.theme.AnjaliSignOutPinkText
import com.example.data.model.BackendAppConfig
import com.example.ui.theme.AnjaliThemeMode
import com.example.ui.theme.RoseOutlineLight

private enum class DestructiveAction {
  DELETE_MEMORIES,
  DELETE_CONVERSATIONS,
  WIPE_ALL_DATA
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsPrivacyScreen(
  userEmail: String?,
  userDisplayName: String?,
  preferences: UserPreferences,
  conversationCount: Int,
  memoryCount: Int,
  onUpdatePreferences: (language: String, tone: String, memoryEnabled: Boolean) -> Unit,
  onDeleteAllMemories: () -> Unit,
  onDeleteAllConversations: () -> Unit,
  onWipeAllData: () -> Unit,
  onSignOut: () -> Unit,
  themeMode: AnjaliThemeMode = AnjaliThemeMode.LIGHT,
  onCycleThemeMode: () -> Unit = {},
  modifier: Modifier = Modifier,
  backendConfig: BackendAppConfig = BackendAppConfig(),
) {
  var pendingDestructiveAction by remember { mutableStateOf<DestructiveAction?>(null) }
  var expandAccountSection by rememberSaveable { mutableStateOf(false) }
  var expandChatSettingsSection by rememberSaveable { mutableStateOf(false) }
  var expandPrivacyDataSection by rememberSaveable { mutableStateOf(false) }
  var showAboutDialog by remember { mutableStateOf(false) }

  val isDark = isSystemInDarkTheme()
  val bgBrush = if (isDark) {
    Brush.verticalGradient(
      listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)
    )
  } else {
    Brush.verticalGradient(
      listOf(Color(0xFFFDF0F6), Color(0xFFFFFAFC), Color(0xFFF9F1FC))
    )
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(bgBrush),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Reference Screen 5 Header: "Settings"
    item(key = "settings_title") {
      Text(
        text = "Settings",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 26.sp
        ),
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    // Centered Anjali Avatar + Name + Quote Pill matching Reference Screen 5
    item(key = "settings_profile_header") {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        AnjaliAvatarIllustration(
          size = 94.dp,
          showAiBadge = false,
          expression = AnjaliExpression.DEFAULT
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "${backendConfig.companionName} ",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(text = "💗", fontSize = 19.sp)
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = backendConfig.companionTagline,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Soft Pink Quote Pill
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFCE9F2),
          border = BorderStroke(1.dp, AnjaliPinkStart.copy(alpha = 0.16f))
        ) {
          Text(
            text = "\"${backendConfig.welcomeQuote}\"",
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF7A4960),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
          )
        }
      }
    }

    // Group Card 1: Account, Memory, Chat Settings, Theme (Reference Screen 5)
    item(key = "settings_group_1") {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          // 1. Account Row
          SettingsRowItem(
            icon = Icons.Outlined.Person,
            title = "Account",
            subtitle = "Manage your account",
            onClick = { expandAccountSection = !expandAccountSection }
          )

          AnimatedVisibility(
            visible = expandAccountSection,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
              Text(
                text = userDisplayName?.takeIf { it.isNotBlank() } ?: "Google Signed-In User",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = userEmail ?: "Authenticated via Google Sign-In",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "$conversationCount saved chats • $memoryCount saved memories",
                style = MaterialTheme.typography.labelSmall,
                color = AnjaliPinkStart
              )
            }
          }

          SettingsDivider()

          // 2. Memory Row with pink heart circle & pink Switch
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(AnjaliPinkStart),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Memory",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Remember your preferences",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = preferences.memoryEnabled,
              onCheckedChange = { enabled ->
                onUpdatePreferences(
                  preferences.preferredLanguage,
                  preferences.expressiveTone,
                  enabled
                )
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AnjaliPinkStart
              ),
              modifier = Modifier.testTag("memory_toggle_switch")
            )
          }

          SettingsDivider()

          // 3. Chat Settings Row
          SettingsRowItem(
            icon = Icons.Outlined.ChatBubbleOutline,
            title = "Chat Settings",
            subtitle = "Language, responses, etc.",
            onClick = { expandChatSettingsSection = !expandChatSettingsSection }
          )

          AnimatedVisibility(
            visible = expandChatSettingsSection,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 18.dp, vertical = 12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "Default Language",
                style = MaterialTheme.typography.labelLarge
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                CompanionLanguage.entries.forEach { lang ->
                  FilterChip(
                    selected = preferences.preferredLanguage.equals(lang.key, ignoreCase = true),
                    onClick = {
                      onUpdatePreferences(
                        lang.key,
                        preferences.expressiveTone,
                        preferences.memoryEnabled
                      )
                    },
                    label = { Text(lang.displayLabel) }
                  )
                }
              }

              Text(
                text = "Anjali's Tone",
                style = MaterialTheme.typography.labelLarge
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                ExpressiveTone.entries.forEach { tone ->
                  FilterChip(
                    selected = preferences.expressiveTone.equals(tone.key, ignoreCase = true),
                    onClick = {
                      onUpdatePreferences(
                        preferences.preferredLanguage,
                        tone.key,
                        preferences.memoryEnabled
                      )
                    },
                    label = { Text(tone.key) }
                  )
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, RoseOutlineLight.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(AnjaliOnlineGreen)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = "Cloud Sync & Live Bestie Engine Active",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = "Your chats automatically restore across devices",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }

          SettingsDivider()

          // 4. Theme Row ("Light >")
          SettingsRowItem(
            icon = Icons.Outlined.DarkMode,
            title = "Theme",
            subtitle = null,
            trailingValue = themeMode.displayLabel,
            onClick = onCycleThemeMode,
            modifier = Modifier.testTag("theme_mode_row")
          )
        }
      }
    }

    // Group Card 2: Privacy & Data, Delete All Conversations, About (Reference Screen 5)
    item(key = "settings_group_2") {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          // 1. Privacy & Data Row
          SettingsRowItem(
            icon = Icons.Outlined.VerifiedUser,
            title = "Privacy & Data",
            subtitle = "Manage your data and privacy",
            onClick = { expandPrivacyDataSection = !expandPrivacyDataSection }
          )

          AnimatedVisibility(
            visible = expandPrivacyDataSection,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
                .padding(horizontal = 18.dp, vertical = 12.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text(
                text = "• Virtual Companion Disclosure: ${backendConfig.companionName} is your virtual bestie (AI character), not a real human.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "• Cloud Backup & Isolation: Your chat history ($conversationCount) and saved memories ($memoryCount) are privately stored under your Google account in Cloud Firestore and automatically restore if you reinstall.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              OutlinedButton(
                onClick = { pendingDestructiveAction = DestructiveAction.DELETE_MEMORIES },
                shape = RoundedCornerShape(50),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("delete_all_memories_button")
              ) {
                Text(
                  text = "Delete All Saved Memories ($memoryCount)",
                  color = MaterialTheme.colorScheme.error
                )
              }

              Button(
                onClick = { pendingDestructiveAction = DestructiveAction.WIPE_ALL_DATA },
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.error,
                  contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("wipe_all_data_button")
              ) {
                Text("Delete All Stored Cloud Data")
              }
            }
          }

          SettingsDivider()

          // 2. Delete All Conversations Row
          SettingsRowItem(
            icon = Icons.Outlined.DeleteOutline,
            title = "Delete All Conversations",
            subtitle = "Remove all your chat history",
            onClick = { pendingDestructiveAction = DestructiveAction.DELETE_CONVERSATIONS },
            modifier = Modifier.testTag("delete_all_conversations_button")
          )

          SettingsDivider()

          // 3. About Row
          SettingsRowItem(
            icon = Icons.Outlined.Info,
            title = "About",
            subtitle = "ANJALI v1.0.0",
            onClick = { showAboutDialog = true }
          )
        }
      }
    }

    // Compact AI Disclosure Card
    item(key = "settings_ai_disclosure") {
      AiDisclosureBanner(compact = true)
    }

    // Bottom Full-Width Soft Pink "Sign Out" Button matching Reference Screen 5
    item(key = "settings_sign_out") {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) MaterialTheme.colorScheme.errorContainer else AnjaliSignOutPinkBg,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .clip(RoundedCornerShape(20.dp))
          .clickable { onSignOut() }
          .testTag("sign_out_button")
      ) {
        Row(
          modifier = Modifier.fillMaxSize(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            tint = if (isDark) MaterialTheme.colorScheme.onErrorContainer else AnjaliSignOutPinkText,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Sign Out",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) MaterialTheme.colorScheme.onErrorContainer else AnjaliSignOutPinkText
          )
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }

  if (showAboutDialog) {
    AlertDialog(
      onDismissRequest = { showAboutDialog = false },
      title = { Text("About ANJALI v1.0.0") },
      text = {
        Text(
          "${backendConfig.companionName} (${backendConfig.companionTagline}) is your warm, friendly virtual bestie. " +
            "She understands Hindi, Hinglish, and English, and keeps your conversations backed up to your Google account."
        )
      },
      confirmButton = {
        TextButton(onClick = { showAboutDialog = false }) {
          Text("Close")
        }
      }
    )
  }

  pendingDestructiveAction?.let { action ->
    val (title, body) = when (action) {
      DestructiveAction.DELETE_MEMORIES ->
        "Clear All Saved Memories?" to "This will permanently delete all $memoryCount saved memories from Cloud Firestore. Anjali will no longer recall them."
      DestructiveAction.DELETE_CONVERSATIONS ->
        "Delete All Conversations?" to "This will permanently delete all $conversationCount conversations and their messages from Cloud Firestore."
      DestructiveAction.WIPE_ALL_DATA ->
        "Delete All Stored Data?" to "This will permanently wipe all of your conversations, messages, saved memories, and companion settings from Cloud Firestore."
    }

    AlertDialog(
      onDismissRequest = { pendingDestructiveAction = null },
      title = { Text(title) },
      text = { Text(body) },
      confirmButton = {
        Button(
          onClick = {
            val target = action
            pendingDestructiveAction = null
            when (target) {
              DestructiveAction.DELETE_MEMORIES -> onDeleteAllMemories()
              DestructiveAction.DELETE_CONVERSATIONS -> onDeleteAllConversations()
              DestructiveAction.WIPE_ALL_DATA -> onWipeAllData()
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
          ),
          modifier = Modifier.testTag("confirm_destructive_action_button")
        ) {
          Text("Delete Permanently")
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDestructiveAction = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun SettingsRowItem(
  icon: ImageVector,
  title: String,
  subtitle: String?,
  trailingValue: String? = null,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(19.dp)
      )
    }

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      if (!subtitle.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    if (!trailingValue.isNullOrBlank()) {
      Text(
        text = trailingValue,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(end = 6.dp)
      )
    }

    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
      modifier = Modifier.size(20.dp)
    )
  }
}

@Composable
private fun SettingsDivider() {
  HorizontalDivider(
    modifier = Modifier.padding(start = 64.dp, end = 16.dp),
    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
  )
}
