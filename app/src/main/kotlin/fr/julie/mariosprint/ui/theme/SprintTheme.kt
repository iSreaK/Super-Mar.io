package fr.julie.mariosprint.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object SprintColors {
    val background = Color(0xFF101928)
    val surface = Color(0xFF1A293D)
    val surfaceRaised = Color(0xFF203249)
    val primary = Color(0xFFFF7966)
    val text = Color(0xFFF7F1E7)
    val muted = Color(0xFFABC0D5)
    val gold = Color(0xFFFFD98A)
    val mint = Color(0xFFACE7C5)
    val outline = Color(0xFF354A63)
}

private val sprintTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

private val sprintShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(24.dp),
)

@Composable
internal fun SprintTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = SprintColors.primary, onPrimary = SprintColors.background,
        background = SprintColors.background, surface = SprintColors.surface,
        onSurface = SprintColors.text, onSurfaceVariant = SprintColors.muted,
        onBackground = SprintColors.text, secondary = SprintColors.mint, tertiary = SprintColors.gold,
        surfaceContainerHigh = SprintColors.surfaceRaised, surfaceContainer = SprintColors.surface,
        surfaceContainerLow = SprintColors.background, surfaceContainerHighest = SprintColors.outline,
        secondaryContainer = SprintColors.mint.copy(alpha = .15f), onSecondaryContainer = SprintColors.mint,
        primaryContainer = SprintColors.primary.copy(alpha = .16f), onPrimaryContainer = SprintColors.primary,
        tertiaryContainer = SprintColors.gold.copy(alpha = .16f), onTertiaryContainer = SprintColors.gold,
        onSecondary = SprintColors.background, onTertiary = SprintColors.background, outline = SprintColors.outline,
    ), typography = sprintTypography, shapes = sprintShapes, content = content)
}
