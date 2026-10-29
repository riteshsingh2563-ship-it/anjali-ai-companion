package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

enum class AnjaliThemeMode(val displayLabel: String) {
  LIGHT("Light"),
  DARK("Dark"),
  SYSTEM("System");

  fun next(): AnjaliThemeMode = when (this) {
    LIGHT -> DARK
    DARK -> SYSTEM
    SYSTEM -> LIGHT
  }
}

private val LightColorScheme = lightColorScheme(
  primary = RosePrimaryLight,
  onPrimary = OnRosePrimaryLight,
  primaryContainer = RosePrimaryContainerLight,
  onPrimaryContainer = OnRosePrimaryContainerLight,
  secondary = LavenderSecondaryLight,
  onSecondary = OnLavenderSecondaryLight,
  secondaryContainer = LavenderSecondaryContainerLight,
  onSecondaryContainer = OnLavenderSecondaryContainerLight,
  tertiary = MauveTertiaryLight,
  onTertiary = OnMauveTertiaryLight,
  tertiaryContainer = MauveTertiaryContainerLight,
  onTertiaryContainer = OnMauveTertiaryContainerLight,
  background = BlushBackgroundLight,
  onBackground = OnBlushBackgroundLight,
  surface = PearlSurfaceLight,
  onSurface = OnPearlSurfaceLight,
  surfaceVariant = BlushSurfaceVariantLight,
  onSurfaceVariant = OnBlushSurfaceVariantLight,
  outline = RoseOutlineLight,
)

private val DarkColorScheme = darkColorScheme(
  primary = RosePrimaryDark,
  onPrimary = OnRosePrimaryDark,
  primaryContainer = RosePrimaryContainerDark,
  onPrimaryContainer = OnRosePrimaryContainerDark,
  secondary = LavenderSecondaryDark,
  onSecondary = OnLavenderSecondaryDark,
  secondaryContainer = LavenderSecondaryContainerDark,
  onSecondaryContainer = OnLavenderSecondaryContainerDark,
  tertiary = MauveTertiaryDark,
  onTertiary = OnMauveTertiaryDark,
  tertiaryContainer = MauveTertiaryContainerDark,
  onTertiaryContainer = OnMauveTertiaryContainerDark,
  background = PlumBackgroundDark,
  onBackground = OnPlumBackgroundDark,
  surface = PlumSurfaceDark,
  onSurface = OnPlumSurfaceDark,
  surfaceVariant = PlumSurfaceVariantDark,
  onSurfaceVariant = OnPlumSurfaceVariantDark,
  outline = RoseOutlineDark,
)

val AnjaliShapes = Shapes(
  extraSmall = RoundedCornerShape(10.dp),
  small = RoundedCornerShape(16.dp),
  medium = RoundedCornerShape(22.dp),
  large = RoundedCornerShape(26.dp),
  extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun AnjaliTheme(
  themeMode: AnjaliThemeMode = AnjaliThemeMode.LIGHT,
  darkTheme: Boolean = when (themeMode) {
    AnjaliThemeMode.LIGHT -> false
    AnjaliThemeMode.DARK -> true
    AnjaliThemeMode.SYSTEM -> isSystemInDarkTheme()
  },
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = AnjaliShapes,
    content = content
  )
}
