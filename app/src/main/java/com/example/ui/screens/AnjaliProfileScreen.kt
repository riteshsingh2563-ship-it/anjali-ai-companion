package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.data.model.MemoryItem
import com.example.data.model.UserPreferences
import com.example.ui.components.AiDisclosureBanner
import com.example.ui.components.AnjaliAvatarIllustration
import com.example.ui.components.AnjaliExpression
import com.example.ui.components.AnjaliExpressionShowcaseRow
import com.example.ui.components.AnjaliSplashScreenView
import com.example.ui.theme.AnjaliPinkStart
import com.example.ui.theme.AnjaliPurpleEnd

import com.example.data.model.BackendAppConfig

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnjaliProfileScreen(
  preferences: UserPreferences,
  memories: List<MemoryItem>,
  onUpdatePreferences: (language: String, tone: String, memoryEnabled: Boolean) -> Unit,
  onAddMemory: (fact: String, category: String) -> Unit,
  onDeleteMemory: (String) -> Unit,
  onStartTopicChat: (prompt: String, language: String) -> Unit,
  modifier: Modifier = Modifier,
  backendConfig: BackendAppConfig = BackendAppConfig(),
) {
  var showAddMemoryDialog by remember { mutableStateOf(false) }
  var selectedExpression by rememberSaveable { mutableStateOf(AnjaliExpression.DEFAULT) }
  var showSplashOverlay by rememberSaveable { mutableStateOf(false) }

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

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(bgBrush),
      contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Top Title + Splash Poster Button
      item(key = "profile_header_bar") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${backendConfig.companionName} Profile",
            style = MaterialTheme.typography.headlineMedium.copy(
              fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 26.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
          )

          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, AnjaliPinkStart.copy(alpha = 0.3f)),
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .clickable { showSplashOverlay = true }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = AnjaliPinkStart,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "Splash Art",
                style = MaterialTheme.typography.labelMedium,
                color = AnjaliPinkStart,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }

      // Hero Profile Card with Heart Doodles
      item(key = "profile_hero") {
        Surface(
          shape = RoundedCornerShape(28.dp),
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 3.dp,
          tonalElevation = 1.dp,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    AnjaliPinkStart.copy(alpha = 0.10f),
                    AnjaliPurpleEnd.copy(alpha = 0.05f),
                    MaterialTheme.colorScheme.surface
                  )
                )
              )
              .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            AnjaliAvatarIllustration(
              size = 132.dp,
              showAiBadge = false,
              expression = selectedExpression,
              showHeartDoodles = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Hi, I'm ${backendConfig.companionName} 💗",
              style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "${backendConfig.companionTagline} • ${selectedExpression.label} (${selectedExpression.moodDescription})",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = backendConfig.welcomeQuote,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              textAlign = TextAlign.Center
            )
          }
        }
      }

      // "Anjali Avatar Expressions" Showcase Card (Bottom-Left of Reference Image)
      item(key = "expressions_showcase") {
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 3.dp,
          tonalElevation = 1.dp,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "Anjali Avatar Expressions",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Tap any expression to preview Anjali's mood:",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AnjaliExpressionShowcaseRow(
              selectedExpression = selectedExpression,
              onSelectExpression = { selectedExpression = it },
              cardSize = 86.dp
            )
          }
        }
      }

      // The 4 Companion Feature Pillars from Reference Screen 2
      item(key = "personality_traits") {
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 3.dp,
          tonalElevation = 1.dp,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Text(
              text = "About Anjali",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            ProfileTraitRow(
              icon = Icons.Default.ChatBubbleOutline,
              circleColor = Color(0xFFFDE2EC),
              iconTint = Color(0xFFEC4E81),
              title = "Chat anytime",
              subtitle = "Share your thoughts, feelings and anything on your mind."
            )
            ProfileTraitRow(
              icon = Icons.Default.Favorite,
              circleColor = Color(0xFFFDE0EB),
              iconTint = Color(0xFFF05588),
              title = "Always supportive",
              subtitle = "I'm here to listen and talk with you without judgement."
            )
            ProfileTraitRow(
              icon = Icons.Default.BookmarkBorder,
              circleColor = Color(0xFFF3E2FE),
              iconTint = Color(0xFFB55CE6),
              title = "Remembers your preferences",
              subtitle = "I can remember important details when you enable memory."
            )
            ProfileTraitRow(
              icon = Icons.Default.Language,
              circleColor = Color(0xFFE7E2FD),
              iconTint = Color(0xFF8364E8),
              title = "Chat in your language",
              subtitle = "I understand Hindi, Hinglish and English."
            )
          }
        }
      }

      // Active Tone & Language Controls
      item(key = "persona_controls") {
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 2.dp,
          tonalElevation = 1.dp,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Text(
              text = "Active Conversation Style",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Language Mode",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant
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
              text = "Expressive Tone",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant
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
          }
        }
      }

      // Quick Topic Starters
      item(key = "topic_starters") {
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 2.dp,
          tonalElevation = 1.dp,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "Start a Topic with Anjali",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              val topics = listOf(
                "Late Night Talk 💕" to ("Haan, koi song suggest karo 🎵" to "Hinglish"),
                "School & Studies" to ("Thoda tired hu, school me bahut classes thi 😢" to "Hinglish"),
                "मन की बात (Hindi)" to ("नमस्ते अंजलि! आज कुछ सकारात्मक और सुकून भरी बातें करते हैं।" to "Hindi"),
                "Motivation 💪" to ("Give me a warm pep talk and motivation for my goals today!" to "English")
              )
              topics.forEach { (label, pair) ->
                AssistChip(
                  onClick = { onStartTopicChat(pair.first, pair.second) },
                  label = { Text(label) },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Default.AutoAwesome,
                      contentDescription = null,
                      tint = AnjaliPinkStart,
                      modifier = Modifier.size(15.dp)
                    )
                  }
                )
              }
            }
          }
        }
      }

      // Saved Memories Section
      item(key = "memories_header") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Memory,
              contentDescription = null,
              tint = AnjaliPinkStart
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Saved Memories (${memories.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (preferences.memoryEnabled) {
                  "Anjali uses these details to personalize chats"
                } else {
                  "Memory is currently paused in Settings"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          OutlinedButton(
            onClick = { showAddMemoryDialog = true },
            shape = RoundedCornerShape(50),
            modifier = Modifier.testTag("add_memory_button")
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add")
          }
        }
      }

      if (memories.isEmpty()) {
        item(key = "empty_memories") {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "No saved memories yet",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "When Memory is enabled, Anjali remembers helpful details you share, or you can add one manually above.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )
            }
          }
        }
      } else {
        items(items = memories, key = { it.id }) { memory ->
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Surface(
                  shape = RoundedCornerShape(50),
                  color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                  Text(
                    text = memory.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = memory.fact,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              IconButton(
                onClick = { onDeleteMemory(memory.id) },
                modifier = Modifier.testTag("delete_memory_${memory.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = "Delete memory",
                  tint = MaterialTheme.colorScheme.error
                )
              }
            }
          }
        }
      }

      // Mandatory AI Transparency Banner
      item(key = "profile_disclosure") {
        AiDisclosureBanner(compact = false)
      }
    }

    AnimatedVisibility(
      visible = showSplashOverlay,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      AnjaliSplashScreenView(
        onContinue = { showSplashOverlay = false }
      )
    }
  }

  if (showAddMemoryDialog) {
    AddMemoryDialog(
      onDismiss = { showAddMemoryDialog = false },
      onSave = { fact, category ->
        showAddMemoryDialog = false
        onAddMemory(fact, category)
      }
    )
  }
}

@Composable
private fun ProfileTraitRow(
  icon: ImageVector,
  circleColor: Color,
  iconTint: Color,
  title: String,
  subtitle: String,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .background(circleColor),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(21.dp)
      )
    }
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddMemoryDialog(
  onDismiss: () -> Unit,
  onSave: (fact: String, category: String) -> Unit,
) {
  var factText by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("preference") }
  val categories = listOf("personal", "preference", "goal", "interest")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add a Detail for Anjali to Remember") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = factText,
          onValueChange = { factText = it.take(280) },
          label = { Text("What should Anjali remember?") },
          placeholder = { Text("e.g., I love adrak wali chai and acoustic music") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("memory_fact_input")
        )
        Text(
          text = "Category",
          style = MaterialTheme.typography.labelLarge
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          categories.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat.replaceFirstChar { it.uppercase() }) }
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (factText.isNotBlank()) {
            onSave(factText.trim(), selectedCategory)
          }
        },
        enabled = factText.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = AnjaliPinkStart),
        modifier = Modifier.testTag("confirm_save_memory_button")
      ) {
        Text("Save Memory")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
