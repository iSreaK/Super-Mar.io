package fr.julie.mariosprint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julie.mariosprint.R
import fr.julie.mariosprint.ui.components.*
import fr.julie.mariosprint.ui.theme.SprintTheme

@Composable
internal fun AboutScreen(version: String, onBack: () -> Unit) {
    MenuPage(Modifier.testTag("about-screen")) {
        ScreenTopBar(stringResource(R.string.about_title), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { Text(stringResource(R.string.about_description), style = MaterialTheme.typography.bodyLarge) }
            item { SectionHeading(stringResource(R.string.about_designers)) }
            item { DesignerCard(stringResource(R.string.about_julien), stringResource(R.string.about_isreak)) }
            item { DesignerCard(stringResource(R.string.about_clement), stringResource(R.string.about_kiss)) }
            item {
                SectionHeading(stringResource(R.string.about_project))
                Spacer(Modifier.height(12.dp))
                InfoNotice(stringResource(R.string.about_project_details))
            }
            item { Text(stringResource(R.string.about_version, version), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun DesignerCard(name: String, alias: String) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(name, style = MaterialTheme.typography.titleLarge)
            Text(alias, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 700, fontScale = 1.5f)
@Composable private fun AboutPreview() { SprintTheme { AboutScreen("1.6.0", {}) } }
