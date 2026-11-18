package fr.julie.mariosprint.ui.screens

import fr.julie.mariosprint.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import fr.julie.mariosprint.ui.theme.SprintTheme
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import fr.julie.mariosprint.game.GameView
import fr.julie.mariosprint.domain.game.Phase
import fr.julie.mariosprint.ui.model.*
import androidx.compose.ui.res.painterResource
import fr.julie.mariosprint.ui.components.*
import fr.julie.mariosprint.ui.components.Eyebrow
import fr.julie.mariosprint.ui.components.StatusPill
import fr.julie.mariosprint.ui.formatting.formatRaceTime

@Composable
internal fun GameScreen(state: GameState, onAction: (GameAction) -> Unit, gameContent: @Composable () -> Unit = {}) {
    Column(Modifier.fillMaxSize().testTag("game-screen")) {
        if (state.recordStatus == RecordStatus.SAVE_FAILED) Text(stringResource(R.string.local_save_failed), color = MaterialTheme.colorScheme.tertiary)
        GameHud(state, onAction)
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.Center)) else gameContent()
            if (state.phase == Phase.READY) Surface(Modifier.align(Alignment.TopCenter).padding(top = 16.dp, start = 20.dp, end = 20.dp),
                color = MaterialTheme.colorScheme.background.copy(alpha = .85f), shape = RoundedCornerShape(50)) {
                Text(stringResource(R.string.game_ready_hint), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).testTag("ready-hint"))
            }
            if (state.phase in listOf(Phase.PAUSED, Phase.DEAD, Phase.FINISHED)) GameOverlay(state, onAction)
        }
    }
}

@Composable
private fun GameHud(state: GameState, onAction: (GameAction) -> Unit) {
    val pauseDescription = stringResource(R.string.pause_description)
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Eyebrow(stringResource(R.string.level_index, state.level.ordinal + 1))
                Text(stringResource(state.level.titleRes), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            val mode = if (!state.eligible) stringResource(R.string.practice_mode) else if (state.profile.player == null) stringResource(R.string.local_mode) else stringResource(R.string.online_mode)
            StatusPill(mode, if (state.eligible) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(10.dp))
            FilledTonalIconButton(onClick = { onAction(GameAction.Pause) }, modifier = Modifier.size(48.dp).testTag("pause").semantics { contentDescription = pauseDescription }) {
                Icon(painterResource(R.drawable.ic_pause), null, Modifier.size(22.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(formatRaceTime(state.timeMs), color = MaterialTheme.colorScheme.onSurface, fontSize = 27.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.coin_counter, state.coins), color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 28.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.starSeconds > 0) BonusPill(R.drawable.ic_star, stringResource(R.string.bonus_duration, state.starSeconds))
            if (state.shield) BonusPill(R.drawable.ic_shield, stringResource(R.string.bonus_shield_active))
            if (state.magnetSeconds > 0) BonusPill(R.drawable.ic_magnet, stringResource(R.string.bonus_duration, state.magnetSeconds))
        }
        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth().height(3.dp),
            color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    }
}

@Composable
private fun BonusPill(icon: Int, label: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(painterResource(icon), stringResource(when (icon) {
                R.drawable.ic_star -> R.string.bonus_star_title
                R.drawable.ic_shield -> R.string.bonus_shield_title
                else -> R.string.bonus_magnet_title
            }), Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun GameOverlay(state: GameState, onAction: (GameAction) -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = .76f)), contentAlignment = Alignment.Center) {
        Surface(Modifier.widthIn(max = 420.dp).fillMaxWidth(.9f).padding(vertical = 16.dp),
            shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (state.phase) {
                    Phase.PAUSED -> {
                        Eyebrow(stringResource(R.string.pause_eyebrow), MaterialTheme.colorScheme.tertiary)
                        Text(stringResource(R.string.pause_title), fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.pause_explanation),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
                        OverlayButton(stringResource(R.string.resume_practice), "resume", { onAction(GameAction.Resume) })
                        OutlinedButton(onClick = { onAction(GameAction.Restart) }, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("restart")) {
                            Text(stringResource(R.string.restart_timer))
                        }
                    }
                    Phase.DEAD -> {
                        Eyebrow(stringResource(R.string.retry_eyebrow), MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.retry_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(formatRaceTime(state.timeMs), color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 26.sp)
                        Text(pluralStringResource(R.plurals.coins_collected, state.coins, state.coins), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        OverlayButton(stringResource(R.string.retry_race), "restart", { onAction(GameAction.Restart) })
                    }
                    Phase.FINISHED -> {
                        Eyebrow(if (state.personalBest) stringResource(R.string.personal_best_achieved) else stringResource(R.string.finish_achieved), MaterialTheme.colorScheme.tertiary)
                        Text(formatRaceTime(state.timeMs), color = MaterialTheme.colorScheme.onSurface, fontSize = 34.sp,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(pluralStringResource(R.plurals.finish_summary, state.coins, state.coins, stringResource(state.level.titleRes)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text(recordStatusText(state.recordStatus), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
                        OverlayButton(stringResource(R.string.race_again), "restart", { onAction(GameAction.Restart) })
                        OutlinedButton(onClick = { onAction(GameAction.Leaderboard) }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(stringResource(R.string.open_leaderboard)) }
                        if (state.profile.player != null) TextButton(onClick = { onAction(GameAction.Synchronize) }) { Text(stringResource(R.string.sync_record)) }
                    }
                    else -> Unit
                }
                TextButton(onClick = { onAction(GameAction.Quit) }, modifier = Modifier.testTag("quit")) { Text(stringResource(R.string.return_home), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun OverlayButton(label: String, testTag: String, onClick: () -> Unit) {
    Button(onClick = onClick, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(50.dp).testTag(testTag)) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun recordStatusText(status: RecordStatus): String = stringResource(when(status) {
    RecordStatus.NONE -> R.string.empty_text
    RecordStatus.PRACTICE -> R.string.record_practice
    RecordStatus.LOCAL -> R.string.record_local
    RecordStatus.SYNCING -> R.string.record_syncing
    RecordStatus.SYNCED -> R.string.record_synced
    RecordStatus.PENDING -> R.string.record_pending
    RecordStatus.SAVE_FAILED -> R.string.local_save_failed
})

@Preview(showBackground = true)
@Composable
private fun GameScreenPreview() { SprintTheme { GameScreen(GameState(phase = Phase.PAUSED, loading = false), {}) } }
