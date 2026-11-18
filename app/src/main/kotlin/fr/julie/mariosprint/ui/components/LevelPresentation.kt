package fr.julie.mariosprint.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import fr.julie.mariosprint.R
import fr.julie.mariosprint.domain.game.LevelId

internal val LevelId.titleRes get() = when (this) {
    LevelId.MEADOW -> R.string.level_meadow_title
    LevelId.CANYON -> R.string.level_canyon_title
    LevelId.NIGHT -> R.string.level_night_title
}
internal val LevelId.shortTitleRes get() = when (this) {
    LevelId.MEADOW -> R.string.level_meadow_short
    LevelId.CANYON -> R.string.level_canyon_short
    LevelId.NIGHT -> R.string.level_night_short
}
internal val LevelId.descriptionRes get() = when (this) {
    LevelId.MEADOW -> R.string.level_meadow_description
    LevelId.CANYON -> R.string.level_canyon_description
    LevelId.NIGHT -> R.string.level_night_description
}
internal val LevelId.difficultyRes get() = when (this) {
    LevelId.MEADOW -> R.string.level_meadow_difficulty
    LevelId.CANYON -> R.string.level_canyon_difficulty
    LevelId.NIGHT -> R.string.level_night_difficulty
}
@Composable internal fun LevelId.accentColor() = when (this) {
    LevelId.MEADOW -> MaterialTheme.colorScheme.secondary
    LevelId.CANYON -> MaterialTheme.colorScheme.primary
    LevelId.NIGHT -> MaterialTheme.colorScheme.tertiary
}
