package fr.julie.mariosprint.ui.screens

import fr.julie.mariosprint.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import fr.julie.mariosprint.ui.theme.SprintTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.julie.mariosprint.ui.model.*
import fr.julie.mariosprint.ui.components.*
import fr.julie.mariosprint.ui.formatting.formatRaceTime

@Composable
internal fun LeaderboardScreen(state: LeaderboardState, onAction: (LeaderboardAction) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 8.dp).testTag("ranking-screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AppHeader(state.profile, { onAction(LeaderboardAction.Account(it)) }, showBack = true, onBack = { onAction(LeaderboardAction.Back) })
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow(stringResource(state.level.titleRes), MaterialTheme.colorScheme.secondary)
            Text(stringResource(R.string.ranking_title), color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.ranking_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Eyebrow(stringResource(R.string.personal_best_label))
                    Text(formatRaceTime(state.stats.bestMs), color = MaterialTheme.colorScheme.tertiary, fontSize = 24.sp, fontFamily = FontFamily.Monospace)
                }
                StatusPill(stringResource(R.string.ranking_limit_label), MaterialTheme.colorScheme.tertiary)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(stringResource(R.string.ranking_section_title))
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onAction(LeaderboardAction.Refresh) }, enabled = state.profile.player != null && !state.loading) { Text(stringResource(R.string.refresh)) }
        }
        if (state.entries.isNotEmpty() && state.error != null) {
            Text(state.error.resolve(), color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
        }
        if (state.entries.isNotEmpty() && state.cached) Eyebrow(stringResource(R.string.cached_ranking))
        Surface(Modifier.weight(1f).fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
            when {
                state.profile.player == null -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    RankingEmptyState(stringResource(R.string.ranking_signed_out_title), stringResource(R.string.ranking_signed_out_description))
                    Spacer(Modifier.height(20.dp)); GoogleSignInButton(state.profile) { onAction(LeaderboardAction.Account(AccountAction.SignIn)) }
                }
                state.loading && state.entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                }
                state.error != null && state.entries.isEmpty() -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.Center) {
                    Text(state.error?.resolve().orEmpty(), color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = { onAction(LeaderboardAction.Refresh) }) { Text(stringResource(R.string.retry)) }
                }
                state.entries.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    RankingEmptyState(stringResource(R.string.ranking_empty_title), stringResource(R.string.ranking_empty_description))
                }
                else -> LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
                    itemsIndexed(state.entries, key = { _, entry -> entry.uid }) { index, entry ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (entry.uid == state.profile.player?.uid) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text((index + 1).toString().padStart(2, '0'), color = if (index < 3) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace, fontSize = 13.sp, modifier = Modifier.width(32.dp))
                            Text(if (entry.uid == state.profile.player?.uid) stringResource(R.string.current_player_name, entry.name.ifBlank { stringResource(R.string.player_default_name) }) else entry.name.ifBlank { stringResource(R.string.player_default_name) }, color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                            Spacer(Modifier.width(6.dp))
                            Text(formatRaceTime(entry.timeMs), color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun RankingEmptyState(title: String, description: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("01", color = MaterialTheme.colorScheme.primary, fontSize = 58.sp, fontWeight = FontWeight.Black)
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
    }
}

@Preview(showBackground = true)
@Composable
private fun LeaderboardScreenPreview() { SprintTheme { LeaderboardScreen(LeaderboardState(), {}) } }
