package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AnjaliOnlineGreen
import com.example.ui.theme.AnjaliPinkEnd
import com.example.ui.theme.AnjaliPinkStart
import com.example.ui.theme.AnjaliPurpleEnd
import com.example.ui.theme.AnjaliVioletBubbleEnd
import com.example.ui.theme.AnjaliVioletBubbleStart
import com.example.ui.theme.PlayfairDisplayFontFamily
import kotlin.math.sin

enum class AnjaliExpression(
  val key: String,
  val label: String,
  val moodDescription: String,
  @DrawableRes val drawableRes: Int,
) {
  DEFAULT(
    key = "default",
    label = "Default",
    moodDescription = "Warm & Attentive",
    drawableRes = R.drawable.anjali_expr_default
  ),
  HAPPY(
    key = "happy",
    label = "Happy",
    moodDescription = "Cheerful & Playful",
    drawableRes = R.drawable.anjali_expr_happy
  ),
  CUTE(
    key = "cute",
    label = "Cute",
    moodDescription = "Sweet & Curious",
    drawableRes = R.drawable.anjali_expr_cute
  ),
  THINKING(
    key = "thinking",
    label = "Thinking",
    moodDescription = "Thoughtful & Listening",
    drawableRes = R.drawable.anjali_expr_thinking
  ),
  SAD_CONCERNED(
    key = "sad",
    label = "Sad / Concerned",
    moodDescription = "Empathetic & Caring",
    drawableRes = R.drawable.anjali_expr_sad
  );

  companion object {
    fun inferFromText(text: String, isThinking: Boolean = false): AnjaliExpression {
      if (isThinking) return THINKING
      val lower = text.lowercase()
      return when {
        lower.contains("tired") || lower.contains("exhausting") || lower.contains("sad") ||
          lower.contains("difficult") || lower.contains("mood off") || lower.contains("sorry") ||
          lower.contains("😢") || lower.contains("🥺") || lower.contains("😔") -> SAD_CONCERNED
        lower.contains("😂") || lower.contains("😄") || lower.contains("haha") ||
          lower.contains("yay") || lower.contains("awesome") || lower.contains("happy") ||
          lower.contains("💪") || lower.contains("🎉") -> HAPPY
        lower.contains("💕") || lower.contains("❤️") || lower.contains("song") ||
          lower.contains("cute") || lower.contains("sweet") || lower.contains("🎵") ||
          lower.contains("late night") -> CUTE
        lower.contains("?") || lower.contains("think") || lower.contains("plan") ||
          lower.contains("studies") || lower.contains("school") -> THINKING
        else -> DEFAULT
      }
    }
  }
}

@Composable
fun AnjaliAvatarIllustration(
  modifier: Modifier = Modifier,
  size: Dp = 96.dp,
  showAiBadge: Boolean = true,
  isSpeaking: Boolean = false,
  expression: AnjaliExpression = AnjaliExpression.DEFAULT,
  showHeartDoodles: Boolean = false,
  showOnlineDot: Boolean = false,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "anjali_aura")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.97f,
    targetValue = if (isSpeaking) 1.09f else 1.03f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = if (isSpeaking) 850 else 2200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "aura_pulse"
  )

  val outerSize = if (showHeartDoodles) size * 1.24f else size

  Box(
    modifier = modifier
      .size(outerSize)
      .semantics { contentDescription = "Anjali AI digital companion avatar (${expression.label})" },
    contentAlignment = Alignment.Center
  ) {
    // Soft glowing outer aura
    Box(
      modifier = Modifier
        .size(size * 1.06f)
        .scale(pulseScale)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            colors = listOf(
              AnjaliPinkStart.copy(alpha = 0.34f),
              AnjaliPurpleEnd.copy(alpha = 0.16f),
              Color.Transparent
            )
          )
        )
    )

    // Optional decorative floating heart & music doodles around the circle (Reference Screen 2)
    if (showHeartDoodles) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = this.size.width
        val h = this.size.height

        // Left floating heart outline 1
        drawSimpleHeartOutline(
          center = Offset(w * 0.11f, h * 0.56f),
          heartSize = w * 0.10f,
          color = AnjaliPinkStart.copy(alpha = 0.78f),
          strokeWidth = 3.5f
        )
        // Left smaller heart outline 2
        drawSimpleHeartOutline(
          center = Offset(w * 0.14f, h * 0.69f),
          heartSize = w * 0.065f,
          color = AnjaliPinkStart.copy(alpha = 0.55f),
          strokeWidth = 2.8f
        )
        // Top-left sparkle dot
        drawCircle(
          color = AnjaliPinkStart.copy(alpha = 0.45f),
          radius = w * 0.015f,
          center = Offset(w * 0.17f, h * 0.38f)
        )
        // Bottom-right floating heart doodle
        drawSimpleHeartOutline(
          center = Offset(w * 0.86f, h * 0.77f),
          heartSize = w * 0.07f,
          color = AnjaliPinkStart.copy(alpha = 0.65f),
          strokeWidth = 2.8f
        )
        // Soft curved flourish lines on bottom-left
        val arcPath = Path().apply {
          moveTo(w * 0.13f, h * 0.48f)
          quadraticTo(w * 0.05f, h * 0.63f, w * 0.22f, h * 0.82f)
        }
        drawPath(
          path = arcPath,
          color = AnjaliPinkStart.copy(alpha = 0.42f),
          style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )
      }
    }

    // High-resolution portrait avatar
    Box(
      modifier = Modifier
        .size(size)
        .shadow(
          elevation = if (size >= 80.dp) 10.dp else 3.dp,
          shape = CircleShape,
          spotColor = AnjaliPinkStart.copy(alpha = 0.45f)
        )
        .clip(CircleShape)
        .border(
          width = if (size >= 72.dp) 2.5.dp else 1.5.dp,
          brush = Brush.linearGradient(
            colors = listOf(Color.White, AnjaliPinkStart.copy(alpha = 0.75f), AnjaliPurpleEnd.copy(alpha = 0.65f))
          ),
          shape = CircleShape
        )
    ) {
      Image(
        painter = painterResource(id = expression.drawableRes),
        contentDescription = "Anjali ${expression.label} expression",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    }

    if (showOnlineDot) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .offset(x = (-2).dp, y = (-2).dp)
          .size(if (size >= 56.dp) 14.dp else 11.dp)
          .clip(CircleShape)
          .background(Color.White)
          .padding(2.dp)
          .clip(CircleShape)
          .background(AnjaliOnlineGreen)
      )
    } else if (showAiBadge) {
      Surface(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .offset(
            x = if (showHeartDoodles) (-12).dp else (-2).dp,
            y = if (showHeartDoodles) (-10).dp else (-2).dp
          ),
        shape = CircleShape,
        color = AnjaliPinkStart,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(1.5.dp, Color.White)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "AI Persona Indicator",
            tint = Color.White,
            modifier = Modifier.size(11.dp)
          )
          Spacer(modifier = Modifier.width(2.dp))
          Text(
            text = "AI",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSimpleHeartOutline(
  center: Offset,
  heartSize: Float,
  color: Color,
  strokeWidth: Float,
) {
  val path = Path().apply {
    val x = center.x
    val y = center.y
    val s = heartSize * 0.5f
    moveTo(x, y + s * 0.85f)
    cubicTo(x - s * 1.3f, y + s * 0.1f, x - s * 1.1f, y - s * 0.95f, x, y - s * 0.3f)
    cubicTo(x + s * 1.1f, y - s * 0.95f, x + s * 1.3f, y + s * 0.1f, x, y + s * 0.85f)
    close()
  }
  drawPath(
    path = path,
    color = color,
    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
  )
}

/**
 * Matches the "Anjali Avatar Expressions" showcase from the bottom-left of the reference design:
 * Default, Happy, Cute, Thinking, Sad / Concerned
 */
@Composable
fun AnjaliExpressionShowcaseRow(
  selectedExpression: AnjaliExpression,
  onSelectExpression: (AnjaliExpression) -> Unit,
  modifier: Modifier = Modifier,
  cardSize: Dp = 88.dp,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      AnjaliExpression.entries.forEach { expr ->
        val isSelected = expr == selectedExpression
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onSelectExpression(expr) }
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .testTag("expression_card_${expr.key}")
        ) {
          Box(
            modifier = Modifier
              .size(cardSize)
              .shadow(
                elevation = if (isSelected) 8.dp else 3.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = AnjaliPinkStart.copy(alpha = 0.4f)
              )
              .clip(RoundedCornerShape(20.dp))
              .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                brush = if (isSelected) {
                  Brush.linearGradient(listOf(AnjaliPinkStart, AnjaliPurpleEnd))
                } else {
                  Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = 0.7f)))
                },
                shape = RoundedCornerShape(20.dp)
              )
          ) {
            Image(
              painter = painterResource(id = expr.drawableRes),
              contentDescription = "Anjali ${expr.label} expression",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
            if (isSelected) {
              Box(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(6.dp)
                  .size(18.dp)
                  .clip(CircleShape)
                  .background(AnjaliPinkStart),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Favorite,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(10.dp)
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = expr.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isSelected) AnjaliPinkStart else MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

/**
 * Full-screen Splash Hero view matching Screen 1 (leftmost screen) of the uploaded reference image:
 * Full portrait background, glowing heart icon, stylized ANJALI title, "Your AI Companion",
 * "” Always Here to Listen 💕", 3 dots, and pink loading bar.
 */
@Composable
fun AnjaliSplashScreenView(
  onContinue: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "splash_anim")
  val progressAnim by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.85f,
    animationSpec = infiniteRepeatable(
      animation = tween(1800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "splash_progress"
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF1E0E17))
      .clickable { onContinue() }
      .testTag("splash_screen_overlay")
  ) {
    Image(
      painter = painterResource(id = R.drawable.anjali_splash_portrait),
      contentDescription = "Anjali AI Companion Portrait",
      contentScale = ContentScale.Crop,
      alignment = Alignment.TopCenter,
      modifier = Modifier.fillMaxSize()
    )

    // Deep plum-to-black bottom vignette matching Screen 1 of the reference image
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colorStops = arrayOf(
              0.0f to Color.Transparent,
              0.48f to Color.Transparent,
              0.72f to Color(0xCC210D18),
              0.88f to Color(0xF2180811),
              1.0f to Color(0xFF12050C)
            )
          )
        )
    )

    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(horizontal = 28.dp, vertical = 38.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Glowing neon heart outline
      Icon(
        imageVector = Icons.Default.FavoriteBorder,
        contentDescription = null,
        tint = Color(0xFFFF9EBB),
        modifier = Modifier.size(44.dp)
      )

      Spacer(modifier = Modifier.height(4.dp))

      // Stylized "ANJALI" script/display title
      Text(
        text = "ANJALI",
        fontFamily = PlayfairDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
        fontSize = 48.sp,
        letterSpacing = 3.sp,
        color = Color.White
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Your Bestie",
        style = MaterialTheme.typography.titleMedium,
        color = Color.White.copy(alpha = 0.92f),
        fontWeight = FontWeight.Medium
      )

      Spacer(modifier = Modifier.height(4.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "” Always Here to Listen ",
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.85f)
        )
        Text(
          text = "💕",
          fontSize = 14.sp
        )
      }

      Spacer(modifier = Modifier.height(22.dp))

      // Three subtle dots
      Text(
        text = "•  •  •",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.65f)
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Pink progress bar at bottom matching Screen 1
      Box(
        modifier = Modifier
          .width(170.dp)
          .height(6.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.22f))
      ) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(progressAnim)
            .clip(CircleShape)
            .background(
              Brush.horizontalGradient(
                listOf(Color(0xFFFF7AA8), Color(0xFFF05588))
              )
            )
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Tap anywhere to continue",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.6f)
      )
    }
  }
}

@Composable
fun WelcomeHeroBanner(
  modifier: Modifier = Modifier,
  expression: AnjaliExpression = AnjaliExpression.DEFAULT,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    AnjaliAvatarIllustration(
      size = 152.dp,
      showAiBadge = false,
      expression = expression,
      showHeartDoodles = true
    )
  }
}

@Composable
fun AiDisclosureBanner(
  modifier: Modifier = Modifier,
  compact: Boolean = false,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(if (compact) 14.dp else 18.dp),
    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f))
  ) {
    Row(
      modifier = Modifier.padding(
        horizontal = if (compact) 12.dp else 16.dp,
        vertical = if (compact) 7.dp else 11.dp
      ),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Outlined.Info,
        contentDescription = "Companion Information",
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(if (compact) 15.dp else 19.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = if (compact) {
          "Anjali is your virtual bestie • Cloud Backup Active 💕"
        } else {
          stringResource(R.string.ai_disclosure_banner)
        },
        style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer
      )
    }
  }
}

/**
 * Matches Screen 3 of the reference image:
 * "Anjali is typing..." above dual rounded white pills with pink & lavender pulsing dots.
 */
@Composable
fun TypingIndicatorBubble(modifier: Modifier = Modifier) {
  val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")
  val dot1Alpha by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(520),
      repeatMode = RepeatMode.Reverse
    ),
    label = "dot1"
  )
  val dot2Alpha by infiniteTransition.animateFloat(
    initialValue = 0.45f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(520, delayMillis = 160),
      repeatMode = RepeatMode.Reverse
    ),
    label = "dot2"
  )
  val dot3Alpha by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(520, delayMillis = 320),
      repeatMode = RepeatMode.Reverse
    ),
    label = "dot3"
  )

  Column(modifier = modifier) {
    Text(
      text = "Anjali is typing...",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = 4.dp, bottom = 5.dp)
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // First pill with pink dots
      Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, AnjaliPinkStart.copy(alpha = 0.18f))
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { alpha ->
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(AnjaliPinkStart.copy(alpha = alpha))
            )
          }
        }
      }

      // Second smaller pill with soft lavender dots (matching reference image)
      Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, AnjaliPurpleEnd.copy(alpha = 0.15f))
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          listOf(dot3Alpha, dot1Alpha, dot2Alpha).forEach { alpha ->
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(AnjaliPurpleEnd.copy(alpha = alpha * 0.65f))
            )
          }
        }
      }
    }
  }
}

/**
 * Matches the Voice Note / Audio Waveform Player component in "Chat UI Components"
 * at the bottom-right of the reference design.
 */
@Composable
fun VoiceNoteWaveformCard(
  isPlaying: Boolean,
  onTogglePlay: () -> Unit,
  durationLabel: String = "0:08",
  timestampLabel: String = "9:42 PM",
  caption: String? = null,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
  val phase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 6.28f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "wave_phase"
  )

  val barHeights = listOf(
    0.28f, 0.45f, 0.35f, 0.72f, 0.95f, 0.60f, 0.85f, 0.50f,
    0.90f, 0.68f, 0.40f, 0.78f, 0.92f, 0.55f, 0.75f, 0.48f,
    0.82f, 0.62f, 0.38f, 0.70f, 0.52f, 0.35f, 0.25f
  )

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    shadowElevation = 4.dp,
    tonalElevation = 2.dp,
    border = BorderStroke(1.dp, AnjaliPurpleEnd.copy(alpha = 0.16f))
  ) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
      if (!caption.isNullOrBlank()) {
        Text(
          text = caption,
          style = MaterialTheme.typography.labelSmall,
          color = AnjaliPinkStart,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(bottom = 4.dp)
        )
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Purple circular Play/Pause button
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(
                listOf(AnjaliVioletBubbleStart, AnjaliVioletBubbleEnd)
              )
            )
            .clickable { onTogglePlay() }
            .testTag("voice_note_play_button"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause voice note" else "Play Anjali voice greeting",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }

        // Waveform bars canvas
        Canvas(
          modifier = Modifier
            .width(132.dp)
            .height(26.dp)
        ) {
          val count = barHeights.size
          val barWidth = 3.dp.toPx()
          val spacing = (size.width - count * barWidth) / (count - 1).coerceAtLeast(1)

          barHeights.forEachIndexed { index, baseRatio ->
            val dynamicRatio = if (isPlaying) {
              (baseRatio * (0.65f + 0.35f * sin(phase + index * 0.55f))).coerceIn(0.2f, 1f)
            } else {
              baseRatio
            }
            val barH = size.height * dynamicRatio
            val top = (size.height - barH) / 2f
            val x = index * (barWidth + spacing)
            val color = if (index % 3 == 0) AnjaliPinkStart.copy(alpha = 0.78f) else AnjaliPurpleEnd.copy(alpha = 0.75f)
            drawRoundRect(
              color = color,
              topLeft = Offset(x, top),
              size = Size(barWidth, barH),
              cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
          }
        }

        Text(
          text = durationLabel,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Medium
        )
      }

      Text(
        text = timestampLabel,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        textAlign = TextAlign.End,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 2.dp)
      )
    }
  }
}
