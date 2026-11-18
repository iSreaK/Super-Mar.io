package fr.julie.mariosprint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julie.mariosprint.R
import fr.julie.mariosprint.ui.components.*
import fr.julie.mariosprint.ui.model.*
import fr.julie.mariosprint.ui.theme.SprintTheme

@Composable
internal fun ProfileScreen(state: ProfileState, onAction: (ProfileAction) -> Unit) {
    val soundDescription = stringResource(R.string.sound_title)
    fun account(action: AccountAction) = onAction(ProfileAction.Account(action))
    MenuPage(Modifier.testTag("profile-screen")) {
        ScreenTopBar(stringResource(R.string.profile_title), { onAction(ProfileAction.Back) })
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            CharacterPortrait(state.skin, Modifier.size(48.dp, 64.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (state.player == null) stringResource(R.string.profile_guest_name)
                                    else state.player.name.ifBlank { stringResource(R.string.player_default_name) },
                                    style = MaterialTheme.typography.titleLarge)
                                StatusPill(stringResource(if (state.player == null) R.string.local_mode else R.string.connected_status),
                                    MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Text(stringResource(if (state.player == null) R.string.profile_signed_out_description else R.string.profile_signed_in_description),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.player == null) {
                            GoogleSignInButton(state) { account(AccountAction.SignIn) }
                            if (!state.configured) Text(stringResource(R.string.supabase_unconfigured_hint),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
            if (state.player != null) item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeading(stringResource(R.string.sync_section_title), stringResource(R.string.sync_explanation))
                    SecondaryAction(stringResource(if (state.syncing) R.string.sync_in_progress else R.string.sync_account),
                        { account(AccountAction.Synchronize) }, enabled = !state.syncing && !state.busy)
                    if (state.syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            item {
                SectionHeading(stringResource(R.string.settings_title))
                Spacer(Modifier.height(12.dp))
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.sound_title), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.sound_description), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = state.sound, onCheckedChange = { account(AccountAction.ToggleSound) },
                            modifier = Modifier.testTag("sound-setting").semantics { contentDescription = soundDescription })
                    }
                }
            }
            if (state.player == null) item { InfoNotice(stringResource(R.string.profile_guest_notice)) }
            if (state.player != null) item {
                TextButton(onClick = { account(AccountAction.SignOut) }, enabled = !state.busy && !state.syncing,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("sign-out")) {
                    Text(stringResource(R.string.sign_out), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ProfileScreenPreview() { SprintTheme { ProfileScreen(ProfileState(), {}) } }

@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun ProfileScreenConnectedPreview() {
    SprintTheme { ProfileScreen(ProfileState(player = PlayerUi("preview", "Julie"), configured = true), {}) }
}
