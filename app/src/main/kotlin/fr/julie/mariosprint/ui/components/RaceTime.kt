package fr.julie.mariosprint.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import fr.julie.mariosprint.ui.formatting.formatRaceTime

/** Le chrono conserve tous ses chiffres sur les petites largeurs, y compris en gros caractères. */
@Composable
internal fun RaceTime(timeMs: Long?, modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge, color: Color = MaterialTheme.colorScheme.onSurface) {
    val value = formatRaceTime(timeMs)
    BoxWithConstraints(modifier) {
        // Marge de sécurité autour de l'avance fixe d'une police monospace (environ 0,6 em).
        val availableSize = maxWidth.value / (value.length * .62f * LocalDensity.current.fontScale)
        Text(value, style = style, fontFamily = FontFamily.Monospace, color = color,
            fontSize = minOf(style.fontSize.value, availableSize).sp, maxLines = 1, softWrap = false)
    }
}
