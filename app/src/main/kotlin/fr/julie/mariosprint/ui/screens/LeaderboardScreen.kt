package fr.julie.mariosprint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julie.mariosprint.R
import fr.julie.mariosprint.ui.components.*
import fr.julie.mariosprint.ui.formatting.formatRaceTime
import fr.julie.mariosprint.ui.model.*
import fr.julie.mariosprint.ui.theme.SprintTheme
import fr.julie.mariosprint.domain.model.FriendshipStatus

@Composable
internal fun LeaderboardScreen(state: LeaderboardState, onAction: (LeaderboardAction) -> Unit) {
    MenuPage(Modifier.testTag("ranking-screen")) {
        ScreenTopBar(stringResource(R.string.ranking_title), { onAction(LeaderboardAction.Back) }) {
            IconButton(onClick = { onAction(LeaderboardAction.Refresh) }, enabled = state.profile.player != null && !state.loading,
                modifier = Modifier.size(48.dp).testTag("refresh-ranking")) {
                Icon(painterResource(R.drawable.ic_refresh), stringResource(R.string.refresh), Modifier.size(22.dp))
            }
        }
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Eyebrow(stringResource(R.string.level_context, stringResource(state.level.theme.titleRes), stringResource(state.level.difficultyRes)), state.level.accentColor())
                        Text(stringResource(state.level.titleRes), style = MaterialTheme.typography.titleLarge)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Text(stringResource(R.string.personal_best_label), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        RaceTime(state.stats.bestMs, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeading(stringResource(R.string.ranking_limit_label), stringResource(R.string.ranking_subtitle))
            }
            if (state.loading) item {
                val description = stringResource(R.string.ranking_loading)
                LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = description })
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.entries.isNotEmpty() && state.cached) item {
                Text(stringResource(R.string.cached_ranking), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.error?.let { error -> item { InfoNotice(error.resolve(), color = MaterialTheme.colorScheme.tertiary) } }
            state.friendError?.let { error -> item { InfoNotice(error.resolve(), color = MaterialTheme.colorScheme.error) } }
            when {
                state.profile.player == null -> item {
                    RankingEmptyState(stringResource(R.string.ranking_signed_out_title), stringResource(R.string.ranking_signed_out_description)) {
                        GoogleSignInButton(state.profile) { onAction(LeaderboardAction.Account(AccountAction.SignIn)) }
                        if (!state.profile.configured) Text(stringResource(R.string.supabase_unconfigured_hint),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary, textAlign = TextAlign.Center)
                    }
                }
                state.error != null && state.entries.isEmpty() -> item {
                    SecondaryAction(stringResource(R.string.retry), { onAction(LeaderboardAction.Refresh) }, enabled = !state.loading)
                }
                !state.loading && state.entries.isEmpty() -> item {
                    RankingEmptyState(stringResource(R.string.ranking_empty_title), stringResource(R.string.ranking_empty_description))
                }
                else -> itemsIndexed(state.entries, key = { _, entry -> entry.uid }) { index, entry ->
                    RankingRow(entry, index + 1, entry.uid == state.profile.player?.uid, state.friendships[entry.uid],
                        !state.friendActionBusy, { onAction(LeaderboardAction.OpenProfile(entry.uid)) },
                        { onAction(LeaderboardAction.AddFriend(entry.uid)) })
                }
            }
        }
    }
}

@Composable
private fun RankingRow(entry: RankingEntryUi, rank: Int, currentPlayer: Boolean, friendship: FriendshipStatus?,
    canAdd: Boolean, onProfile: () -> Unit, onAdd: () -> Unit) {
    val largeText = LocalDensity.current.fontScale > 1.3f
    Surface(onClick = onProfile, shape = MaterialTheme.shapes.medium,
        color = if (currentPlayer) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.ranking_position, rank), Modifier.widthIn(min = 28.dp), fontFamily = FontFamily.Monospace,
                    color = if (rank <= 3) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleSmall)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(entry.name.ifBlank { stringResource(R.string.player_default_name) }, style = MaterialTheme.typography.titleSmall,
                        maxLines = if (largeText) 2 else 1, overflow = TextOverflow.Ellipsis)
                    if (currentPlayer) Text(stringResource(R.string.ranking_you), color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelMedium)
                    if (largeText) RankingTime(entry.timeMs)
                }
                if (!largeText) RankingTime(entry.timeMs)
            }
            if (!currentPlayer) {
                if (friendship == null) TextButton(onClick = onAdd, enabled = canAdd,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add-friend-${entry.uid}")) {
                    Text(stringResource(R.string.friends_add))
                } else Text(stringResource(when (friendship) {
                    FriendshipStatus.INCOMING -> R.string.friends_status_incoming
                    FriendshipStatus.OUTGOING -> R.string.friends_status_outgoing
                    FriendshipStatus.FRIEND -> R.string.friends_status_friend
                }), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun RankingTime(timeMs: Long) {
    Text(formatRaceTime(timeMs), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun RankingEmptyState(title: String, description: String, action: @Composable () -> Unit = {}) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(painterResource(R.drawable.ic_flag), null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.secondary)
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
            action()
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LeaderboardScreenPreview() { SprintTheme { LeaderboardScreen(LeaderboardState(), {}) } }

@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun LeaderboardEntriesPreview() {
    SprintTheme { LeaderboardScreen(LeaderboardState(profile = ProfileState(player = PlayerUi("preview", "Julie")),
        entries = listOf(RankingEntryUi("preview", "Julie", 42_300), RankingEntryUi("second", "Un autre joueur", 45_200))), {}) }
}
