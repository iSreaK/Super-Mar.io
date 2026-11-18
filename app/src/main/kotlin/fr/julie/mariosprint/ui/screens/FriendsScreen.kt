package fr.julie.mariosprint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julie.mariosprint.R
import fr.julie.mariosprint.domain.model.*
import fr.julie.mariosprint.ui.components.*
import fr.julie.mariosprint.ui.model.*
import fr.julie.mariosprint.ui.theme.SprintTheme

@Composable
internal fun FriendsScreen(state: FriendsState, onAction: (FriendsAction) -> Unit) {
    var code by rememberSaveable(state.viewerUid) { mutableStateOf("") }
    var selected by rememberSaveable(state.viewerUid) { mutableIntStateOf(0) }
    var removingUid by rememberSaveable(state.viewerUid) { mutableStateOf<String?>(null) }
    val removing = state.friends.firstOrNull { it.uid == removingUid }
    removing?.let { friend -> RemoveFriendDialog(friend.name, {
        onAction(FriendsAction.Execute(FriendCommand.Remove(friend.id))); removingUid = null
    }, { removingUid = null }) }
    MenuPage(Modifier.testTag("friends-screen")) {
        ScreenTopBar(stringResource(R.string.friends_title), { onAction(FriendsAction.Back) }) {
            IconButton(onClick = { onAction(FriendsAction.Refresh) }, enabled = state.viewerUid != null && !state.refreshing,
                modifier = Modifier.size(48.dp).testTag("refresh-friends")) {
                Icon(painterResource(R.drawable.ic_refresh), stringResource(R.string.refresh))
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text(stringResource(R.string.friends_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (state.viewerUid == null) item {
                InfoNotice(stringResource(R.string.friends_sign_in))
                Spacer(Modifier.height(16.dp))
                PrimaryAction(stringResource(R.string.friends_open_account), { onAction(FriendsAction.SignIn) })
            } else {
                item {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Eyebrow(stringResource(R.string.friends_my_code))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(FriendCode.format(state.friendCode).ifBlank { "—" }, Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.secondary)
                                IconButton(onClick = { onAction(FriendsAction.CopyCode) }, enabled = state.friendCode.isNotBlank(),
                                    modifier = Modifier.size(48.dp).testTag("copy-friend-code")) {
                                    Icon(painterResource(R.drawable.ic_copy), stringResource(R.string.friends_copy_code))
                                }
                            }
                            TextButton(onClick = { onAction(FriendsAction.OpenProfile(state.viewerUid)) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.friends_my_profile))
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeading(stringResource(R.string.friends_add_by_code))
                        OutlinedTextField(value = code, onValueChange = { code = it.take(32) }, singleLine = true,
                            label = { Text(stringResource(R.string.friends_code_label)) },
                            placeholder = { Text(stringResource(R.string.friends_code_example)) },
                            modifier = Modifier.fillMaxWidth().testTag("friend-code"), enabled = !state.busy,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters,
                                keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (!state.busy && FriendCode.normalize(code) != null) {
                                    onAction(FriendsAction.Execute(FriendCommand.SendByCode(code))); selected = 2
                                }
                            }))
                        SecondaryAction(stringResource(R.string.friends_send), {
                            onAction(FriendsAction.Execute(FriendCommand.SendByCode(code))); selected = 2
                        }, enabled = !state.busy && FriendCode.normalize(code) != null)
                    }
                }
                if (state.refreshing || state.busy) item {
                    val description = stringResource(R.string.friends_refreshing)
                    LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = description })
                }
                state.error?.let { error -> item {
                    InfoNotice(error.resolve(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { onAction(FriendsAction.Refresh) }, enabled = !state.refreshing,
                        modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.retry)) }
                } }
                if (state.friendCode.isNotBlank()) item { Text(stringResource(if (!state.cached) R.string.friends_live else R.string.friends_cached),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(R.string.friends_title to state.friends.size, R.string.friends_received to state.incoming.size,
                            R.string.friends_sent to state.outgoing.size).forEachIndexed { index, (label, count) ->
                            FilterChip(selected = selected == index, onClick = { selected = index },
                                label = { Text(stringResource(R.string.friends_tab_count, stringResource(label), count)) },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("friends-tab-$index"))
                        }
                    }
                }
                val rows = when (selected) { 1 -> state.incoming; 2 -> state.outgoing; else -> state.friends }
                if (rows.isEmpty() && !state.refreshing && state.friendCode.isNotBlank()) item { InfoNotice(stringResource(when (selected) {
                    1 -> R.string.friends_empty_incoming; 2 -> R.string.friends_empty_outgoing; else -> R.string.friends_empty
                })) }
                items(rows, key = { it.uid }) { friend ->
                    val profileDescription = stringResource(R.string.friends_open_profile, friend.name)
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(onClick = { onAction(FriendsAction.OpenProfile(friend.uid)) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics { contentDescription = profileDescription }) {
                                CharacterPortrait(friend.skin, Modifier.size(32.dp, 44.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(friend.name.ifBlank { stringResource(R.string.player_default_name) }, Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium)
                                Icon(painterResource(R.drawable.ic_arrow_forward), null, Modifier.size(20.dp))
                            }
                            FriendshipActions(friend.uid, friend, !state.busy) { command ->
                                if (command is FriendCommand.Remove) removingUid = friend.uid else onAction(FriendsAction.Execute(command))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable private fun FriendsGuestPreview() { SprintTheme { FriendsScreen(FriendsState(), {}) } }

@Preview(showBackground = true, widthDp = 320, heightDp = 700, fontScale = 1.5f)
@Composable private fun FriendsRequestsPreview() {
    SprintTheme { FriendsScreen(FriendsState("preview", "ABCDEF123456", listOf(
        FriendUi("request", "peer", "Un autre joueur", CharacterSkin.TOAD, FriendshipStatus.INCOMING))), {}) }
}
