package fr.julie.mariosprint.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.julie.mariosprint.R
import fr.julie.mariosprint.domain.model.CharacterSkin
import fr.julie.mariosprint.domain.model.MissionId
import fr.julie.mariosprint.ui.components.*
import fr.julie.mariosprint.ui.model.*
import fr.julie.mariosprint.ui.theme.SprintTheme

@Composable
internal fun CollectionScreen(state: CollectionState, onAction: (CollectionAction) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().testTag("collection"), contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = { onAction(CollectionAction.Back) }) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.return_home))
                }
                Text(stringResource(R.string.collection_title), fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    CharacterPortrait(state.equippedSkin, Modifier.size(74.dp, 94.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Eyebrow(stringResource(R.string.skin_equipped))
                        Text(stringResource(state.equippedSkin.nameRes), fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.skins_unlocked_count, state.unlockedSkins.size, CharacterSkin.entries.size), fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            Text(stringResource(if (state.signedIn) R.string.collection_account_info else R.string.collection_guest_info),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 19.sp)
            if (state.signedIn) TextButton(enabled = !state.busy, onClick = { onAction(CollectionAction.Synchronize) }) {
                Text(stringResource(if (state.busy) R.string.sync_in_progress else R.string.sync_account))
            }
            state.error?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            if (state.pendingSelection) Text(stringResource(R.string.skin_pending), color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
        }
        item { Text(stringResource(R.string.missions_title), fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        items(state.missions, key = { it.id }) { mission -> MissionCard(mission) }
        item {
            Text(stringResource(R.string.wardrobe_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.skins_cosmetic_info), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        items(CharacterSkin.entries, key = { it.value }) { skin ->
            SkinCard(skin, skin in state.unlockedSkins, skin == state.equippedSkin, state.busy) {
                onAction(CollectionAction.Equip(skin))
            }
        }
    }
}

@Composable
private fun MissionCard(mission: MissionUi) {
    val completed = mission.progress >= mission.target
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CharacterPortrait(mission.reward, Modifier.size(44.dp, 56.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(mission.id.titleRes), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(stringResource(R.string.mission_reward, stringResource(mission.reward.nameRes)),
                        color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                }
                Text(stringResource(R.string.mission_progress, mission.progress, mission.target), fontWeight = FontWeight.Bold)
            }
            Text(stringResource(mission.id.descriptionRes), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 18.sp)
            LinearProgressIndicator(progress = { mission.progress.toFloat() / mission.target }, modifier = Modifier.fillMaxWidth())
            if (completed) Text(stringResource(R.string.mission_completed), color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SkinCard(skin: CharacterSkin, unlocked: Boolean, equipped: Boolean, busy: Boolean, onEquip: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (equipped) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CharacterPortrait(skin, Modifier.size(54.dp, 70.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(skin.nameRes), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (!unlocked) Text(stringResource(MissionId.entries.first { it.reward == skin }.titleRes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                FilledTonalButton(onClick = onEquip, enabled = unlocked && !equipped && !busy,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("equip-${skin.value}")) {
                    Text(stringResource(when { equipped -> R.string.skin_equipped; unlocked -> R.string.skin_equip; else -> R.string.skin_locked }))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionScreenPreview() {
    SprintTheme { CollectionScreen(CollectionState(missions = MissionId.entries.map { MissionUi(it, 0, it.target, it.reward) }), {}) }
}
