package fr.julie.mariosprint.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import fr.julie.mariosprint.ui.theme.SprintTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.julie.mariosprint.R
import fr.julie.mariosprint.ui.model.*

@Composable
internal fun AppHeader(profile: ProfileState, onAccountAction: (AccountAction) -> Unit, showBack: Boolean = false, onBack: () -> Unit = {}) {
    val soundDescription = if (profile.sound) stringResource(R.string.mute_sound) else stringResource(R.string.enable_sound)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showBack) TextButton(onClick = onBack, modifier = Modifier.testTag("home")) {
            Icon(painterResource(R.drawable.ic_arrow_back), null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.home_back), color = MaterialTheme.colorScheme.onSurface)
        } else {
            Image(painterResource(R.drawable.ic_launcher), null, Modifier.size(36.dp))
            Spacer(Modifier.width(9.dp))
            Text(stringResource(R.string.brand_mario), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
            Text(stringResource(R.string.brand_sprint), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
        }
        Spacer(Modifier.weight(1f))
        StatusPill(if (profile.player == null) stringResource(R.string.local_mode) else stringResource(R.string.connected_status), MaterialTheme.colorScheme.secondary)
        IconButton(onClick = { onAccountAction(AccountAction.ToggleSound) }, modifier = Modifier.size(48.dp).testTag("sound")
            .semantics { contentDescription = soundDescription }) {
            Icon(painterResource(if (profile.sound) R.drawable.ic_volume else R.drawable.ic_mute), null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun Eyebrow(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.3.sp)
}

@Composable
internal fun StatusPill(text: String, color: Color) {
    Surface(color = color.copy(alpha = .10f), shape = RoundedCornerShape(50)) {
        Text(text, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun ProfileCard(profile: ProfileState, onAccountAction: (AccountAction) -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (profile.player == null) stringResource(R.string.profile_signed_out_title) else stringResource(R.string.profile_greeting, profile.player?.name?.ifBlank { stringResource(R.string.player_default_name) }.orEmpty()),
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(if (profile.player == null) stringResource(R.string.profile_signed_out_description)
                else stringResource(R.string.profile_signed_in_description), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp)
            if (profile.player == null) {
                GoogleSignInButton(profile) { onAccountAction(AccountAction.SignIn) }
                if (!profile.configured) Text(stringResource(R.string.supabase_unconfigured_hint), color = MaterialTheme.colorScheme.tertiary, fontSize = 10.sp)
            } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { onAccountAction(AccountAction.Synchronize) }) { Text(stringResource(R.string.sync), color = MaterialTheme.colorScheme.secondary) }
                TextButton(onClick = { onAccountAction(AccountAction.SignOut) }, enabled = !profile.busy) { Text(stringResource(R.string.sign_out), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
internal fun GoogleSignInButton(profile: ProfileState, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !profile.busy,
        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(50.dp).testTag("google"),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface, contentColor = MaterialTheme.colorScheme.background)) {
        Text(if (profile.busy) stringResource(R.string.sign_in_progress) else stringResource(R.string.sign_in_google), fontWeight = FontWeight.Bold)
    }
}
