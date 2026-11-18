package fr.julie.mariosprint.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import fr.julie.mariosprint.R
import fr.julie.mariosprint.domain.game.LevelId
import fr.julie.mariosprint.domain.game.LevelTheme
import fr.julie.mariosprint.domain.game.Difficulty

internal val LevelId.titleRes get() = when (this) {
    LevelId.MEADOW -> R.string.level_meadow_title
    LevelId.CANYON -> R.string.level_canyon_title
    LevelId.NIGHT -> R.string.level_night_title
    LevelId.MEADOW_TRAILS -> R.string.level_meadow_trails_title
    LevelId.MEADOW_SUMMITS -> R.string.level_meadow_summits_title
    LevelId.CANYON_OASIS -> R.string.level_canyon_oasis_title
    LevelId.CANYON_RIDGES -> R.string.level_canyon_ridges_title
    LevelId.NIGHT_LANTERNS -> R.string.level_night_lanterns_title
    LevelId.NIGHT_OBSERVATORY -> R.string.level_night_observatory_title
}
internal val LevelTheme.titleRes get() = when (this) {
    LevelTheme.MEADOW -> R.string.level_meadow_short
    LevelTheme.CANYON -> R.string.level_canyon_short
    LevelTheme.NIGHT -> R.string.level_night_short
}
internal val LevelId.descriptionRes get() = when (this) {
    LevelId.MEADOW -> R.string.level_meadow_description
    LevelId.CANYON -> R.string.level_canyon_description
    LevelId.NIGHT -> R.string.level_night_description
    LevelId.MEADOW_TRAILS -> R.string.level_meadow_trails_description
    LevelId.MEADOW_SUMMITS -> R.string.level_meadow_summits_description
    LevelId.CANYON_OASIS -> R.string.level_canyon_oasis_description
    LevelId.CANYON_RIDGES -> R.string.level_canyon_ridges_description
    LevelId.NIGHT_LANTERNS -> R.string.level_night_lanterns_description
    LevelId.NIGHT_OBSERVATORY -> R.string.level_night_observatory_description
}
internal val Difficulty.titleRes get() = when (this) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.NORMAL -> R.string.difficulty_normal
    Difficulty.EXPERT -> R.string.difficulty_expert
}
internal val LevelId.difficultyRes get() = difficulty.titleRes
@Composable internal fun LevelId.accentColor() = when (theme) {
    LevelTheme.MEADOW -> MaterialTheme.colorScheme.secondary
    LevelTheme.CANYON -> MaterialTheme.colorScheme.primary
    LevelTheme.NIGHT -> MaterialTheme.colorScheme.tertiary
}
