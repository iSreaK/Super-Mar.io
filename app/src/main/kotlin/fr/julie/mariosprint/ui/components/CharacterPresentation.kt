package fr.julie.mariosprint.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text
import fr.julie.mariosprint.R
import fr.julie.mariosprint.domain.model.CharacterSkin
import fr.julie.mariosprint.domain.model.MissionId
import fr.julie.mariosprint.game.rendering.GameArt

internal val CharacterSkin.nameRes get() = when (this) {
    CharacterSkin.LUIGI -> R.string.skin_luigi
    CharacterSkin.TOAD -> R.string.skin_toad
    CharacterSkin.YOSHI -> R.string.skin_yoshi
    CharacterSkin.PEACH -> R.string.skin_peach
}
internal val MissionId.titleRes get() = when (this) {
    MissionId.FIRST_FINISH -> R.string.mission_first_title
    MissionId.THREE_WORLDS -> R.string.mission_worlds_title
    MissionId.EXPERT_FINISHES -> R.string.mission_expert_title
}
internal val MissionId.descriptionRes get() = when (this) {
    MissionId.FIRST_FINISH -> R.string.mission_first_description
    MissionId.THREE_WORLDS -> R.string.mission_worlds_description
    MissionId.EXPERT_FINISHES -> R.string.mission_expert_description
}

@Composable
internal fun CharacterPortrait(skin: CharacterSkin, modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) Text(stringResource(skin.nameRes), modifier)
    else {
        val portrait = remember(skin) { GameArt.characters.getValue(skin).first().asImageBitmap() }
        Image(portrait, stringResource(skin.nameRes), modifier, contentScale = ContentScale.Fit)
    }
}
