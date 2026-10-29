package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.CompanionLanguage
import com.example.data.model.ExpressiveTone
import com.example.data.model.UserPreferences
import com.example.ui.components.AiDisclosureBanner
import com.example.ui.components.AnjaliAvatarIllustration
import com.example.ui.components.AnjaliExpression
import com.example.ui.components.AnjaliExpressionShowcaseRow
import com.example.ui.components.AnjaliSplashScreenView
import com.example.ui.components.WelcomeHeroBanner
import com.example.ui.theme.AnjaliPinkEnd
import com.example.ui.theme.AnjaliPinkStart
import com.example.util.onGoogleSignInClicked

@Composable
fun WelcomeSignInScreen(
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isSigningIn by remember { mutableStateOf(false) }
  var authError by remember { mutableStateOf<String?>(null) }
  var showSplashPreview by rememberSaveable { mutableStateOf(false) }
  var selectedExpression by rememberSaveable { mutableStateOf(AnjaliExpression.DEFAULT) }

  Box(modifier = modifier.fillMaxSize()) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                Color(0xFFFDECF4),
                Color(0xFFFFF9FC),
                Color(0xFFF9EEF8)
              )
            )
          )
          .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 14.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Top bar with Splash Art toggle & Mandatory Google Cloud badge
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(50),
              color = Color.White.copy(alpha = 0.85f),
              border = BorderStroke(1.dp, AnjaliPinkStart.copy(alpha = 0.28f)),
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { showSplashPreview = true }
                .testTag("preview_splash_button")
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

            Surface(
              shape = RoundedCornerShape(50),
              color = AnjaliPinkStart.copy(alpha = 0.12f)
            ) {
              Text(
                text = "Your Bestie 💕",
                style = MaterialTheme.typography.labelMedium,
                color = AnjaliPinkStart,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Circular Anjali Avatar with floating heart doodles
          WelcomeHeroBanner(expression = selectedExpression)

          Spacer(modifier = Modifier.height(10.dp))

          // "Hi, I'm Anjali 💗"
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Text(
              text = "Hi, I'm Anjali ",
              style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 27.sp
              ),
              color = MaterialTheme.colorScheme.onBackground
            )
            Text(
              text = "💗",
              fontSize = 23.sp
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "Your Bestie",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(22.dp))

          // The 4 Feature Rows
          Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            WelcomeFeatureRow(
              icon = Icons.Default.ChatBubbleOutline,
              iconTint = Color(0xFFEC4E81),
              circleColor = Color(0xFFFDE2EC),
              title = "Chat anytime",
              description = "Share your thoughts, feelings and anything on your mind."
            )
            WelcomeFeatureRow(
              icon = Icons.Default.Favorite,
              iconTint = Color(0xFFF05588),
              circleColor = Color(0xFFFDE0EB),
              title = "Always supportive",
              description = "I'm here to listen and talk with you without judgement."
            )
            WelcomeFeatureRow(
              icon = Icons.Default.BookmarkBorder,
              iconTint = Color(0xFFB55CE6),
              circleColor = Color(0xFFF3E2FE),
              title = "Cloud chat backup & memory",
              description = "Your chats & memories stay safe in your Google account even if you reinstall."
            )
            WelcomeFeatureRow(
              icon = Icons.Default.Language,
              iconTint = Color(0xFF8364E8),
              circleColor = Color(0xFFE7E2FD),
              title = "Chat in your language",
              description = "I understand Hindi, Hinglish and English."
            )
          }

          Spacer(modifier = Modifier.height(20.dp))

          // Expression preview strip
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
          ) {
            Text(
              text = "Anjali Avatar Expressions",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(bottom = 8.dp)
            )
            AnjaliExpressionShowcaseRow(
              selectedExpression = selectedExpression,
              onSelectExpression = { selectedExpression = it },
              cardSize = 72.dp
            )
          }

          Spacer(modifier = Modifier.height(22.dp))

          if (authError != null) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.errorContainer,
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
            ) {
              Text(
                text = authError.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp)
              )
            }
          }

          // Mandatory Google Sign-In CTA Button
          Button(
            onClick = {
              authError = null
              isSigningIn = true
              onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  isSigningIn = false
                  onAuthSuccess()
                },
                onAuthError = { msg ->
                  isSigningIn = false
                  authError = msg
                },
                scope = coroutineScope,
                onAuthCancelled = {
                  isSigningIn = false
                }
              )
            },
            enabled = !isSigningIn,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
              containerColor = AnjaliPinkStart,
              contentColor = Color.White
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(50),
                spotColor = AnjaliPinkStart.copy(alpha = 0.45f)
              )
              .testTag("google_sign_in_button")
          ) {
            if (isSigningIn) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Signing in with Google…",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
              )
            } else {
              Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Continue with Google",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.width(8.dp))
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "Sign in with Google to save and restore all your chats with Anjali across devices.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(10.dp))

          AiDisclosureBanner(compact = true)
        }
      }
    }

    // Full-screen Splash Art Preview Overlay (Reference Screen 1)
    AnimatedVisibility(
      visible = showSplashPreview,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      AnjaliSplashScreenView(
        onContinue = { showSplashPreview = false }
      )
    }
  }
}

@Composable
private fun WelcomeFeatureRow(
  icon: ImageVector,
  iconTint: Color,
  circleColor: Color,
  title: String,
  description: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(circleColor),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(23.dp)
      )
    }
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingCustomizationCard(
  currentPreferences: UserPreferences,
  onCompleteOnboarding: (language: String, tone: String, memoryEnabled: Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedLanguage by remember(currentPreferences.preferredLanguage) {
    mutableStateOf(currentPreferences.preferredLanguage)
  }
  var selectedTone by remember(currentPreferences.expressiveTone) {
    mutableStateOf(currentPreferences.expressiveTone)
  }
  var memoryEnabled by remember(currentPreferences.memoryEnabled) {
    mutableStateOf(currentPreferences.memoryEnabled)
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    border = BorderStroke(1.dp, AnjaliPinkStart.copy(alpha = 0.22f))
  ) {
    Column(
      modifier = Modifier.padding(22.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.primaryContainer
        ) {
          Text(
            text = "Welcome",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
        Text(
          text = "Skip",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable {
              onCompleteOnboarding(selectedLanguage, selectedTone, memoryEnabled)
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }

      AnjaliAvatarIllustration(
        size = 112.dp,
        showAiBadge = false,
        expression = AnjaliExpression.DEFAULT,
        showHeartDoodles = true
      )

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "Hi, I'm Anjali 💗",
          style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Your Bestie",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // 4 Feature rows matching Screen 2
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        WelcomeFeatureRow(
          icon = Icons.Default.ChatBubbleOutline,
          iconTint = Color(0xFFEC4E81),
          circleColor = Color(0xFFFDE2EC),
          title = "Chat anytime",
          description = "Share your thoughts, feelings and anything on your mind."
        )
        WelcomeFeatureRow(
          icon = Icons.Default.Favorite,
          iconTint = Color(0xFFF05588),
          circleColor = Color(0xFFFDE0EB),
          title = "Always supportive",
          description = "I'm here to listen and talk with you without judgement."
        )
        WelcomeFeatureRow(
          icon = Icons.Default.BookmarkBorder,
          iconTint = Color(0xFFB55CE6),
          circleColor = Color(0xFFF3E2FE),
          title = "Remembers your preferences",
          description = "I can remember important details when you enable memory."
        )
        WelcomeFeatureRow(
          icon = Icons.Default.Language,
          iconTint = Color(0xFF8364E8),
          circleColor = Color(0xFFE7E2FD),
          title = "Chat in your language",
          description = "I understand Hindi, Hinglish and English."
        )
      }

      // Language selection chips
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Preferred Language",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          CompanionLanguage.entries.forEach { lang ->
            val selected = selectedLanguage.equals(lang.key, ignoreCase = true)
            FilterChip(
              selected = selected,
              onClick = { selectedLanguage = lang.key },
              label = { Text(lang.displayLabel) },
              leadingIcon = if (selected) {
                {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                }
              } else null,
              modifier = Modifier.testTag("onboarding_lang_${lang.key.lowercase()}")
            )
          }
        }
      }

      // Expressive tone selection
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Anjali's Conversational Vibe",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ExpressiveTone.entries.forEach { tone ->
            val isSelected = selectedTone.equals(tone.key, ignoreCase = true)
            FilterChip(
              selected = isSelected,
              onClick = { selectedTone = tone.key },
              label = { Text(tone.key) }
            )
          }
        }
      }

      // Memory toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Memory",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Remember your preferences",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = memoryEnabled,
          onCheckedChange = { memoryEnabled = it },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = AnjaliPinkStart
          ),
          modifier = Modifier.testTag("onboarding_memory_switch")
        )
      }

      Button(
        onClick = {
          onCompleteOnboarding(selectedLanguage, selectedTone, memoryEnabled)
        },
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
          containerColor = AnjaliPinkStart,
          contentColor = Color.White
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("complete_onboarding_button")
      ) {
        Text(
          text = "Start Chatting",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
      }

      Text(
        text = stringResource(R.string.ai_character_footer),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
  }
}
