package fr.julie.mariosprint.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julie.mariosprint.R
import fr.julie.mariosprint.domain.model.CharacterSkin
import fr.julie.mariosprint.domain.model.MissionId
import fr.julie.mariosprint.ui.components.*
import fr.julie.mariosprint.ui.model.*
import fr.julie.mariosprint.ui.theme.SprintTheme

@Composable
internal fun CollectionScreen(state: CollectionState, onAction: (CollectionAction) -> Unit) {
    MenuPage(Modifier.testTag("collection")) {
        ScreenTopBar(stringResource(R.string.wardrobe_title), { onAction(CollectionAction.Back) })
        BoxWithConstraints(Modifier.weight(1f)) {
            // Une colonne quand les libellés agrandis ont besoin de plus d'espace.
            val columns = if (maxWidth >= 360.dp && LocalDensity.current.fontScale <= 1.3f) 2 else 1
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            CharacterPortrait(state.equippedSkin, Modifier.size(56.dp, 72.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Eyebrow(stringResource(R.string.skin_equipped))
                                Text(stringResource(state.equippedSkin.nameRes), style = MaterialTheme.typography.titleLarge)
                                Text(stringResource(R.string.skins_unlocked_count, state.unlockedSkins.size, CharacterSkin.entries.size),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                state.error?.let { error -> item { InfoNotice(error.resolve(), color = MaterialTheme.colorScheme.error) } }
                if (state.pendingSelection) item { InfoNotice(stringResource(R.string.skin_pending), color = MaterialTheme.colorScheme.tertiary) }
                if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                item { SectionHeading(stringResource(R.string.choose_character), stringResource(R.string.skins_cosmetic_info)) }
                items(CharacterSkin.entries.chunked(columns), key = { it.first().value }) { skins ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        skins.forEach { skin ->
                            SkinCard(skin, skin in state.unlockedSkins, skin == state.equippedSkin, state.busy,
                                Modifier.weight(1f).fillMaxHeight()) { onAction(CollectionAction.Equip(skin)) }
                        }
                    }
                }
                item { Spacer(Modifier.height(4.dp)); SectionHeading(stringResource(R.string.missions_title), stringResource(R.string.missions_subtitle)) }
                items(state.missions, key = { it.id }) { mission -> MissionCard(mission) }
                item {
                    Text(stringResource(if (state.signedIn) R.string.collection_account_info else R.string.collection_guest_info),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.signedIn) TextButton(onClick = { onAction(CollectionAction.Synchronize) }, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(if (state.busy) R.string.sync_in_progress else R.string.sync_account))
                    }
                }
            }
        }
    }
}

@Composable
private fun SkinCard(skin: CharacterSkin, unlocked: Boolean, equipped: Boolean, busy: Boolean, modifier: Modifier, onEquip: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier, shape = MaterialTheme.shapes.large, color = colors.surface,
        border = BorderStroke(1.dp, if (equipped) colors.secondary else colors.outline.copy(alpha = .6f))) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CharacterPortrait(skin, Modifier.size(60.dp, 76.dp))
            Text(stringResource(skin.nameRes), style = MaterialTheme.typography.titleMedium)
            if (!unlocked) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_lock), null, Modifier.size(14.dp), tint = colors.onSurfaceVariant)
                    Text(stringResource(MissionId.entries.first { it.reward == skin }.titleRes),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            Spacer(Modifier.weight(1f))
            FilledTonalButton(onClick = onEquip, enabled = unlocked && !equipped && !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("equip-${skin.value}"),
                shape = MaterialTheme.shapes.small, contentPadding = PaddingValues(10.dp)) {
                if (equipped) { Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
                Text(stringResource(when { equipped -> R.string.skin_equipped; unlocked -> R.string.skin_equip; else -> R.string.skin_locked }))
            }
        }
    }
}

@Composable
private fun MissionCard(mission: MissionUi) {
    val completed = mission.progress >= mission.target
    val accent = if (completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(mission.id.titleRes), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.mission_reward, stringResource(mission.reward.nameRes)),
                        style = MaterialTheme.typography.bodySmall, color = accent)
                }
                StatusPill(stringResource(R.string.mission_progress, mission.progress, mission.target), accent)
            }
            Text(stringResource(mission.id.descriptionRes), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(progress = { (mission.progress.toFloat() / mission.target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(), color = accent, trackColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            if (completed) Text(stringResource(R.string.mission_completed), style = MaterialTheme.typography.bodySmall, color = accent)
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun CollectionScreenPreview() {
    SprintTheme { CollectionScreen(CollectionState(missions = MissionId.entries.map { MissionUi(it, 0, it.target, it.reward) }), {}) }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun CollectionScreenLargeTextPreview() { SprintTheme { CollectionScreen(CollectionState(), {}) } }
