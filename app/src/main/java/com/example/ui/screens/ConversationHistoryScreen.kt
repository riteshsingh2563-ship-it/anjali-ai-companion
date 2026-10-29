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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompanionLanguage
import com.example.data.model.Conversation
import com.example.ui.components.AnjaliAvatarIllustration
import com.example.ui.components.AnjaliExpression
import com.example.ui.theme.AnjaliPinkStart
import com.example.ui.theme.AnjaliPurpleEnd
import com.example.viewmodel.UiState
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConversationHistoryScreen(
  conversationsState: UiState<List<Conversation>>,
  activeConversationId: String?,
  onSelectConversation: (String) -> Unit,
  onDeleteConversation: (String) -> Unit,
  onStartNewConversation: () -> Unit,
  onStartTopicConversation: (prompt: String, language: String) -> Unit = { _, _ -> onStartNewConversation() },
  modifier: Modifier = Modifier,
) {
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var showSearchBar by rememberSaveable { mutableStateOf(false) }
  var selectedLanguageFilter by rememberSaveable { mutableStateOf("All") }
  var conversationPendingDelete by remember { mutableStateOf<Conversation?>(null) }

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

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = Color.Transparent,
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onStartNewConversation,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("New Chat") },
        containerColor = AnjaliPinkStart,
        contentColor = Color.White,
        modifier = Modifier.testTag("history_new_chat_fab")
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(bgBrush)
        .padding(innerPadding)
        .padding(horizontal = 18.dp)
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      // Reference Screen 4 Header: "Conversations" + circular white Search button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Conversations",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp
          ),
          color = MaterialTheme.colorScheme.onBackground
        )

        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 4.dp,
          tonalElevation = 2.dp,
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable { showSearchBar = !showSearchBar }
            .testTag("history_search_toggle_button")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search conversations",
              tint = if (showSearchBar) AnjaliPinkStart else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(21.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Search Input (Always rendered in composition tree or expandable so testTag is accessible)
      AnimatedVisibility(
        visible = showSearchBar || searchQuery.isNotEmpty(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        Column {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search past chats with Anjali…") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
              )
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
              {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
              }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface,
              focusedBorderColor = AnjaliPinkStart
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("history_search_input")
          )

          Spacer(modifier = Modifier.height(8.dp))

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            val filters = listOf("All") + CompanionLanguage.entries.map { it.key }
            filters.forEach { filterKey ->
              FilterChip(
                selected = selectedLanguageFilter == filterKey,
                onClick = { selectedLanguageFilter = filterKey },
                label = { Text(filterKey) }
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
        }
      }

      when (conversationsState) {
        is UiState.Loading -> {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(color = AnjaliPinkStart)
          }
        }

        is UiState.Error -> {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = conversationsState.message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
              textAlign = TextAlign.Center
            )
          }
        }

        is UiState.Success -> {
          val filtered = conversationsState.data.filter { conv ->
            val matchesLang = selectedLanguageFilter == "All" ||
              conv.language.equals(selectedLanguageFilter, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
              conv.title.contains(searchQuery, ignoreCase = true) ||
              conv.lastMessagePreview.contains(searchQuery, ignoreCase = true)
            matchesLang && matchesQuery
          }

          if (filtered.isEmpty()) {
            EmptyConversationsStateView(
              hasAnySavedConversations = conversationsState.data.isNotEmpty(),
              onStartNewConversation = onStartNewConversation
            )
          } else {
            val grouped = remember(filtered) {
              filtered.groupBy { conv ->
                classifyDateBucket(conv.updatedAt ?: conv.createdAt)
              }
            }

            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(top = 6.dp, bottom = 92.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              grouped.forEach { (bucketTitle, bucketConvs) ->
                item(key = "header_$bucketTitle") {
                  Column {
                    Text(
                      text = bucketTitle,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onBackground,
                      modifier = Modifier.padding(bottom = 10.dp, start = 2.dp)
                    )

                    Surface(
                      shape = RoundedCornerShape(24.dp),
                      color = MaterialTheme.colorScheme.surface,
                      shadowElevation = 3.dp,
                      tonalElevation = 1.dp,
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Column {
                        bucketConvs.forEachIndexed { index, conv ->
                          val isSelected = conv.id == activeConversationId
                          ConversationHistoryRow(
                            conversation = conv,
                            isActive = isSelected,
                            onClick = { onSelectConversation(conv.id) },
                            onDeleteClick = { conversationPendingDelete = conv }
                          )
                          if (index < bucketConvs.lastIndex) {
                            HorizontalDivider(
                              modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  conversationPendingDelete?.let { conv ->
    AlertDialog(
      onDismissRequest = { conversationPendingDelete = null },
      title = { Text("Delete Conversation?") },
      text = {
        Text("This will permanently delete \"${conv.title}\" and all its messages from your Cloud Firestore history.")
      },
      confirmButton = {
        Button(
          onClick = {
            val id = conv.id
            conversationPendingDelete = null
            onDeleteConversation(id)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
          ),
          modifier = Modifier.testTag("confirm_delete_conversation_button")
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { conversationPendingDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun ConversationHistoryRow(
  conversation: Conversation,
  isActive: Boolean,
  onClick: () -> Unit,
  onDeleteClick: () -> Unit,
) {
  val timeLabel = remember(conversation.updatedAt, conversation.createdAt) {
    formatHistoryTime(conversation.updatedAt ?: conversation.createdAt)
  }
  val avatarExpression = remember(conversation.title, conversation.lastMessagePreview) {
    AnjaliExpression.inferFromText("${conversation.title} ${conversation.lastMessagePreview}")
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        else Color.Transparent
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 12.dp)
      .testTag("conversation_card_${conversation.id}"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    AnjaliAvatarIllustration(
      size = 48.dp,
      showAiBadge = false,
      expression = avatarExpression
    )

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = conversation.title,
          style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp),
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = timeLabel,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
      }

      Spacer(modifier = Modifier.height(3.dp))

      Text(
        text = conversation.lastMessagePreview.ifBlank { "Start chatting with Anjali…" },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    IconButton(
      onClick = onDeleteClick,
      modifier = Modifier
        .size(32.dp)
        .testTag("delete_conv_${conversation.id}")
    ) {
      Icon(
        imageVector = Icons.Default.DeleteOutline,
        contentDescription = "Delete conversation",
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.size(18.dp)
      )
    }

    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = AnjaliPurpleEnd.copy(alpha = 0.75f),
      modifier = Modifier.size(20.dp)
    )
  }
}

@Composable
private fun EmptyConversationsStateView(
  hasAnySavedConversations: Boolean,
  onStartNewConversation: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(vertical = 32.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 4.dp,
      tonalElevation = 1.dp,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        AnjaliAvatarIllustration(
          size = 76.dp,
          showAiBadge = false,
          expression = AnjaliExpression.CUTE,
          showOnlineDot = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = if (hasAnySavedConversations) {
            "No matching chats found"
          } else {
            "No conversations yet 💕"
          },
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = if (hasAnySavedConversations) {
            "Try clearing your search filter or start a fresh conversation with Anjali."
          } else {
            "All your chats with Anjali are securely saved in the cloud with your Google account — even if you reinstall the app!"
          },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
          onClick = onStartNewConversation,
          shape = RoundedCornerShape(50),
          colors = ButtonDefaults.buttonColors(
            containerColor = AnjaliPinkStart,
            contentColor = Color.White
          )
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Start Chatting with Anjali",
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

private fun classifyDateBucket(timestamp: Timestamp?): String {
  val date = (timestamp ?: Timestamp.now()).toDate()
  val nowCal = Calendar.getInstance()
  val msgCal = Calendar.getInstance().apply { time = date }

  val sameYear = nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)
  val dayDiff = nowCal.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR)

  return when {
    sameYear && dayDiff == 0 -> "Today"
    sameYear && dayDiff == 1 -> "Yesterday"
    else -> "Earlier"
  }
}

private fun formatHistoryTime(timestamp: Timestamp?): String {
  val date = (timestamp ?: Timestamp.now()).toDate()
  return SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
}
