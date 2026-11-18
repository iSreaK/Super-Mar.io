package fr.julie.mariosprint.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.julie.mariosprint.R
import fr.julie.mariosprint.ui.model.AccountAction
import fr.julie.mariosprint.ui.model.ProfileState

/** Une largeur de lecture commune ; les listes et leurs actions restent propres à chaque écran. */
@Composable
internal fun MenuPage(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier.widthIn(max = 600.dp).fillMaxSize(), content = content)
    }
}

@Composable
internal fun AppHeader(profile: ProfileState, onAccountAction: (AccountAction) -> Unit,
    onProfile: () -> Unit, onHelp: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_launcher), null, Modifier.size(30.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(if (profile.player == null) R.string.profile_guest_status else R.string.profile_connected_status),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onHelp, modifier = Modifier.size(48.dp).testTag("open-help")) {
            Icon(painterResource(R.drawable.ic_help), stringResource(R.string.help_title), Modifier.size(22.dp))
        }
        IconButton(onClick = { onAccountAction(AccountAction.ToggleSound) }, modifier = Modifier.size(48.dp).testTag("sound")) {
            Icon(painterResource(if (profile.sound) R.drawable.ic_volume else R.drawable.ic_mute),
                stringResource(if (profile.sound) R.string.mute_sound else R.string.enable_sound), Modifier.size(22.dp))
        }
        IconButton(onClick = onProfile, modifier = Modifier.size(48.dp).testTag("open-profile")) {
            Icon(painterResource(R.drawable.ic_person), stringResource(R.string.profile_title), Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
internal fun ScreenTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("home")) {
            Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.return_home), Modifier.size(22.dp))
        }
        Text(title, Modifier.weight(1f).padding(horizontal = 4.dp), style = MaterialTheme.typography.titleMedium)
        actions()
    }
}

@Composable
internal fun SectionHeading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
internal fun Eyebrow(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, color = color, style = MaterialTheme.typography.labelMedium)
}

@Composable
internal fun StatusPill(text: String, color: Color, containerColor: Color = color.copy(alpha = .12f)) {
    Surface(color = containerColor, shape = RoundedCornerShape(50)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = color,
            style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun InfoNotice(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.secondary) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(painterResource(R.drawable.ic_info), null, Modifier.size(20.dp), tint = color)
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun PrimaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun SecondaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun GoogleSignInButton(profile: ProfileState, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !profile.busy && profile.configured, shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("google"),
        contentPadding = PaddingValues(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface, contentColor = MaterialTheme.colorScheme.background)) {
        if (profile.busy) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
        }
        Text(stringResource(if (profile.busy) R.string.sign_in_progress else R.string.sign_in_google),
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}
