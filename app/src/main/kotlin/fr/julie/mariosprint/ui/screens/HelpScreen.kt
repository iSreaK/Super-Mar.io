package fr.julie.mariosprint.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julie.mariosprint.R
import fr.julie.mariosprint.ui.components.*
import fr.julie.mariosprint.ui.model.HelpAction
import fr.julie.mariosprint.ui.theme.SprintTheme

@Composable
internal fun HelpScreen(onAction: (HelpAction) -> Unit) {
    MenuPage(Modifier.testTag("help-screen")) {
        ScreenTopBar(stringResource(R.string.help_title), { onAction(HelpAction.Back) })
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item {
                SectionHeading(stringResource(R.string.controls_title), stringResource(R.string.help_controls_intro))
                Spacer(Modifier.height(12.dp))
                HelpCard {
                    HelpRow(R.drawable.ic_arrow_forward, stringResource(R.string.help_move_title), stringResource(R.string.help_move_description))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    HelpRow(R.drawable.ic_arrow_up, stringResource(R.string.help_jump_title), stringResource(R.string.help_jump_description))
                }
            }
            item {
                SectionHeading(stringResource(R.string.bonuses_title))
                Spacer(Modifier.height(12.dp))
                HelpCard {
                    HelpRow(R.drawable.ic_star, stringResource(R.string.bonus_star_title), stringResource(R.string.bonus_star_description))
                    HelpRow(R.drawable.ic_shield, stringResource(R.string.bonus_shield_title), stringResource(R.string.bonus_shield_description))
                    HelpRow(R.drawable.ic_magnet, stringResource(R.string.bonus_magnet_title), stringResource(R.string.bonus_magnet_description))
                }
            }
            item {
                SectionHeading(stringResource(R.string.help_records_title), stringResource(R.string.help_records_description))
                Spacer(Modifier.height(12.dp))
                InfoNotice(stringResource(R.string.pause_explanation), color = MaterialTheme.colorScheme.tertiary)
            }
            item { InfoNotice(stringResource(R.string.skins_cosmetic_info)) }
        }
    }
}

@Composable
private fun HelpCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(20.dp), content = content)
    }
}

@Composable
private fun HelpRow(@DrawableRes icon: Int, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(painterResource(icon), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HelpScreenPreview() { SprintTheme { HelpScreen {} } }
