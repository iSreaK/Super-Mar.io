package fr.julie.mariosprint.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    ), content = content)
}
