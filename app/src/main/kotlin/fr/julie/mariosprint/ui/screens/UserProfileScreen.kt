package fr.julie.mariosprint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julie.mariosprint.R
import fr.julie.mariosprint.domain.model.*
import fr.julie.mariosprint.ui.components.*
import fr.julie.mariosprint.ui.model.*
import fr.julie.mariosprint.ui.theme.SprintTheme

@Composable
internal fun UserProfileScreen(state: UserProfileState, onAction: (UserProfileAction) -> Unit) {
    var removing by rememberSaveable(state.viewerUid, state.playerUid) { mutableStateOf(false) }
    if (removing && state.relation?.status == FriendshipStatus.FRIEND) RemoveFriendDialog(state.player?.name.orEmpty(), {
        state.relation?.let { onAction(UserProfileAction.Execute(FriendCommand.Remove(it.id))) }; removing = false
    }, { removing = false })
    MenuPage(Modifier.testTag("user-profile-screen")) {
        ScreenTopBar(stringResource(R.string.user_profile_title), { onAction(UserProfileAction.Back) }) {
            IconButton(onClick = { onAction(UserProfileAction.Refresh) }, enabled = state.viewerUid != null && !state.loading && !state.busy,
                modifier = Modifier.size(48.dp)) { Icon(painterResource(R.drawable.ic_refresh), stringResource(R.string.refresh)) }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.viewerUid == null) item {
                InfoNotice(stringResource(R.string.friends_sign_in))
                Spacer(Modifier.height(16.dp))
                PrimaryAction(stringResource(R.string.friends_open_account), { onAction(UserProfileAction.SignIn) })
            } else {
                if (state.loading || state.busy) item {
                    val description = stringResource(R.string.user_profile_loading)
                    LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = description })
                }
                state.error?.let { error -> item {
                    InfoNotice(error.resolve(), color = MaterialTheme.colorScheme.error)
                    SecondaryAction(stringResource(R.string.retry), { onAction(UserProfileAction.Refresh) }, enabled = !state.loading && !state.busy)
                } }
                state.player?.let { player -> item {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                CharacterPortrait(player.skin, Modifier.size(48.dp, 64.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(player.name.ifBlank { stringResource(R.string.player_default_name) }, style = MaterialTheme.typography.headlineSmall)
                                    Text(pluralStringResource(R.plurals.user_profile_record_count, state.records.size, state.records.size),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (player.uid == state.viewerUid) {
                                Text(stringResource(R.string.user_profile_self), color = MaterialTheme.colorScheme.secondary)
                                state.friendCode?.let { Text(FriendCode.format(it), style = MaterialTheme.typography.titleMedium) }
                            } else {
                                state.relation?.let { relation -> StatusPill(stringResource(when (relation.status) {
                                    FriendshipStatus.INCOMING -> R.string.friends_status_incoming
                                    FriendshipStatus.OUTGOING -> R.string.friends_status_outgoing
                                    FriendshipStatus.FRIEND -> R.string.friends_status_friend
                                }), MaterialTheme.colorScheme.secondary) }
                                FriendshipActions(player.uid, state.relation, !state.busy && !state.loading) { command ->
                                    if (command is FriendCommand.Remove) removing = true else onAction(UserProfileAction.Execute(command))
                                }
                            }
                        }
                    }
                } }
                if (state.player != null && state.cached) item { Text(stringResource(R.string.user_profile_cached),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (state.player != null) {
                    item { SectionHeading(stringResource(R.string.user_profile_records)) }
                    if (state.records.isEmpty()) item { InfoNotice(stringResource(R.string.user_profile_no_records)) }
                    items(state.records, key = { it.level.value }) { record ->
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(record.level.titleRes), style = MaterialTheme.typography.titleSmall)
                                RaceTime(record.timeMs, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                    item { Text(stringResource(R.string.user_profile_privacy), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 700, fontScale = 1.5f)
@Composable private fun UserProfilePreview() {
    SprintTheme { UserProfileScreen(UserProfileState(viewerUid = "me", playerUid = "peer",
        player = PublicPlayerUi("peer", "Un autre joueur", CharacterSkin.YOSHI)), {}) }
}
