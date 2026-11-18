package fr.julie.mariosprint.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julie.mariosprint.R
import fr.julie.mariosprint.domain.game.Phase
import fr.julie.mariosprint.ui.components.*
import fr.julie.mariosprint.ui.model.*
import fr.julie.mariosprint.ui.theme.SprintTheme

@Composable
internal fun GameScreen(state: GameState, onAction: (GameAction) -> Unit, gameContent: @Composable () -> Unit = {}) {
    Column(Modifier.fillMaxSize().testTag("game-screen")) {
        if (state.recordStatus == RecordStatus.SAVE_FAILED) Text(stringResource(R.string.local_save_failed),
            Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        GameHud(state, onAction)
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.Center)) else gameContent()
            if (!state.loading && state.phase == Phase.READY) Surface(Modifier.align(Alignment.TopCenter).padding(16.dp),
                color = MaterialTheme.colorScheme.background.copy(alpha = .94f), shape = MaterialTheme.shapes.medium) {
                Text(stringResource(R.string.game_ready_hint), style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp).testTag("ready-hint"))
            }
            when (state.phase) {
                Phase.PAUSED, Phase.DEAD, Phase.FINISHED -> GameOverlay(state, onAction)
                else -> Unit
            }
        }
    }
}

@Composable
private fun GameHud(state: GameState, onAction: (GameAction) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(state.level.titleRes), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(when {
                    !state.eligible -> R.string.practice_mode
                    state.profile.player == null -> R.string.local_mode
                    else -> R.string.online_mode
                }), style = MaterialTheme.typography.labelSmall,
                    color = if (state.eligible) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary)
            }
            FilledTonalIconButton(onClick = { onAction(GameAction.Pause) },
                enabled = !state.loading && (state.phase == Phase.RUNNING || state.phase == Phase.READY),
                modifier = Modifier.size(48.dp).testTag("pause")) {
                Icon(painterResource(R.drawable.ic_pause), stringResource(R.string.pause_description), Modifier.size(22.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RaceTime(state.timeMs, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.coin_counter, state.coins), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.tertiary)
        }
        // Trois emplacements permanents : un bonus ne déplace jamais la scène ni les commandes.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BonusSlot(R.drawable.ic_star, stringResource(R.string.bonus_star_title), state.starSeconds > 0,
                stringResource(R.string.bonus_duration, state.starSeconds), Modifier.weight(1f))
            BonusSlot(R.drawable.ic_shield, stringResource(R.string.bonus_shield_title), state.shield,
                stringResource(R.string.bonus_shield_count), Modifier.weight(1f))
            BonusSlot(R.drawable.ic_magnet, stringResource(R.string.bonus_magnet_title), state.magnetSeconds > 0,
                stringResource(R.string.bonus_duration, state.magnetSeconds), Modifier.weight(1f))
        }
        LinearProgressIndicator(progress = { state.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(4.dp),
            color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    }
}

@Composable
private fun BonusSlot(@DrawableRes icon: Int, name: String, active: Boolean, value: String, modifier: Modifier) {
    val description = if (active) stringResource(R.string.bonus_active_description, name, value)
        else stringResource(R.string.bonus_inactive_description, name)
    val color = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(modifier.clearAndSetSemantics { contentDescription = description }, shape = MaterialTheme.shapes.small,
        color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
        Row(Modifier.heightIn(min = 32.dp).padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            Icon(painterResource(icon), null, Modifier.size(16.dp), tint = color.copy(alpha = if (active) 1f else .55f))
            Text(if (active) value else stringResource(R.string.bonus_inactive), style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
private fun GameOverlay(state: GameState, onAction: (GameAction) -> Unit) {
    val title = stringResource(when (state.phase) {
        Phase.PAUSED -> R.string.pause_title
        Phase.DEAD -> R.string.retry_title
        else -> R.string.finish_achieved
    })
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background.copy(alpha = .82f)) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Surface(Modifier.widthIn(max = 420.dp).fillMaxWidth().semantics { paneTitle = title },
                shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(painterResource(when (state.phase) {
                        Phase.PAUSED -> R.drawable.ic_pause
                        Phase.DEAD -> R.drawable.ic_refresh
                        else -> R.drawable.ic_flag
                    }), null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    when (state.phase) {
                        Phase.PAUSED -> {
                            Text(stringResource(R.string.pause_explanation), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            PrimaryAction(stringResource(R.string.resume_practice), { onAction(GameAction.Resume) }, Modifier.testTag("resume"))
                            SecondaryAction(stringResource(R.string.restart_timer), { onAction(GameAction.Restart) }, Modifier.testTag("restart"))
                        }
                        Phase.DEAD, Phase.FINISHED -> {
                            if (state.personalBest && state.phase == Phase.FINISHED) StatusPill(stringResource(R.string.personal_best_achieved), MaterialTheme.colorScheme.tertiary)
                            RaceTime(state.timeMs, style = MaterialTheme.typography.headlineLarge)
                            Text(pluralStringResource(R.plurals.coins_collected, state.coins, state.coins), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (state.phase == Phase.FINISHED && state.recordStatus != RecordStatus.NONE) {
                                Text(recordStatusText(state.recordStatus), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                if (state.recordStatus == RecordStatus.SYNCING) LinearProgressIndicator(Modifier.fillMaxWidth())
                            }
                            PrimaryAction(stringResource(if (state.phase == Phase.FINISHED) R.string.race_again else R.string.retry_race),
                                { onAction(GameAction.Restart) }, Modifier.testTag("restart"))
                            if (state.phase == Phase.FINISHED) {
                                SecondaryAction(stringResource(R.string.open_leaderboard), { onAction(GameAction.Leaderboard) })
                                if (state.profile.player != null && state.recordStatus == RecordStatus.PENDING) {
                                    TextButton(onClick = { onAction(GameAction.Synchronize) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                        Text(stringResource(R.string.sync_record))
                                    }
                                }
                            }
                        }
                        else -> Unit
                    }
                    TextButton(onClick = { onAction(GameAction.Quit) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("quit")) {
                        Text(stringResource(R.string.return_home), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun recordStatusText(status: RecordStatus): String = stringResource(when (status) {
    RecordStatus.NONE -> R.string.empty_text
    RecordStatus.PRACTICE -> R.string.record_practice
    RecordStatus.LOCAL -> R.string.record_local
    RecordStatus.SYNCING -> R.string.record_syncing
    RecordStatus.SYNCED -> R.string.record_synced
    RecordStatus.PENDING -> R.string.record_pending
    RecordStatus.SAVE_FAILED -> R.string.local_save_failed
})

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun GameScreenPreview() { SprintTheme { GameScreen(GameState(phase = Phase.PAUSED, loading = false), {}) } }

@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun GameFinishPreview() {
    SprintTheme { GameScreen(GameState(phase = Phase.FINISHED, timeMs = 42_300, loading = false,
        recordStatus = RecordStatus.LOCAL, personalBest = true), {}) }
}
