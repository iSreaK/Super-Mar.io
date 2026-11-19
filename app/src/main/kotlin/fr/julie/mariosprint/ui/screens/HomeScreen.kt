package fr.julie.mariosprint.ui.screens

import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import fr.julie.mariosprint.R
import fr.julie.mariosprint.domain.game.*
import fr.julie.mariosprint.domain.model.CharacterSkin
import fr.julie.mariosprint.game.GameView
import fr.julie.mariosprint.ui.components.*
import fr.julie.mariosprint.ui.formatting.formatRaceTime
import fr.julie.mariosprint.ui.model.*
import fr.julie.mariosprint.ui.theme.SprintTheme

@Composable
internal fun HomeScreen(state: HomeState, onAction: (HomeAction) -> Unit) {
    MenuPage(Modifier.testTag("dashboard")) {
        AppHeader(state.profile, { onAction(HomeAction.Account(it)) },
            onProfile = { onAction(HomeAction.Profile) }, onHelp = { onAction(HomeAction.Help) })
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.home_title), Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                    IconButton(onClick = { onAction(HomeAction.Friends) }, modifier = Modifier.size(48.dp).testTag("open-friends")) {
                        BadgedBox(badge = { if (state.friendRequests > 0) Badge { Text(state.friendRequests.toString()) } }) {
                            Icon(painterResource(R.drawable.ic_friends),
                                pluralStringResource(R.plurals.friends_badge_description, state.friendRequests, state.friendRequests),
                                tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
            state.error?.let { error -> item { InfoNotice(error.resolve(), color = MaterialTheme.colorScheme.error) } }
            item { LevelSelector(state.selectedLevel) { onAction(HomeAction.SelectLevel(it)) } }
            item { LevelPreviewCard(state.selectedLevel, state.profile.skin, state.stats) }
            item {
                Surface(onClick = { onAction(HomeAction.Collection) }, shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().testTag("open-collection")) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CharacterPortrait(state.profile.skin, Modifier.size(40.dp, 52.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.collection_title), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.collection_teaser, state.profile.unlockedSkins),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(painterResource(R.drawable.ic_arrow_forward), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            state.legacyBestMs?.let { best -> item {
                Text(stringResource(R.string.legacy_best, formatRaceTime(best)), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
        }
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryAction(stringResource(R.string.start_selected_level), { onAction(HomeAction.Play) }, Modifier.testTag("play"))
                TextButton(onClick = { onAction(HomeAction.Leaderboard) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("leaderboard")) {
                    Text(stringResource(R.string.open_leaderboard), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Icon(painterResource(R.drawable.ic_arrow_diagonal), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun LevelPreviewCard(level: LevelId, skin: CharacterSkin, stats: StatsUi) {
    val largeText = LocalDensity.current.fontScale > 1.3f
    val previewEngine = remember(level) { GameEngine(level).apply { player.x = 230f } }
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column {
            Box(Modifier.fillMaxWidth().height(132.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                if (!LocalInspectionMode.current) key(level) {
                    AndroidView(factory = { GameView(it, previewEngine, interactive = false, skin = skin).apply {
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    } }, update = { it.setSkin(skin) }, modifier = Modifier.fillMaxSize())
                }
                Box(Modifier.align(Alignment.TopStart).padding(12.dp)) {
                    StatusPill(stringResource(level.difficultyRes), MaterialTheme.colorScheme.onSurface,
                        MaterialTheme.colorScheme.background.copy(alpha = .94f))
                }
            }
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(level.titleRes), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(level.descriptionRes), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .6f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Eyebrow(stringResource(R.string.personal_best_label))
                        RaceTime(stats.bestMs, color = MaterialTheme.colorScheme.tertiary)
                    }
                    if (!largeText) Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.home_attempts_count, stats.attempts), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.home_finishes_count, stats.finishes), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (largeText) {
                    Text(stringResource(R.string.home_attempts_count, stats.attempts), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.home_finishes_count, stats.finishes), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun LevelSelector(selected: LevelId, onSelect: (LevelId) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SelectorGroup(stringResource(R.string.choose_world)) {
            LevelTheme.entries.forEach { theme ->
                val level = LevelId.entries.first { it.theme == theme && it.difficulty == selected.difficulty }
                SelectorOption(stringResource(theme.titleRes), selected.theme == theme, Modifier.weight(1f).testTag("world-${theme.name}"),
                    { onSelect(level) })
            }
        }
        SelectorGroup(stringResource(R.string.choose_difficulty)) {
            Difficulty.entries.forEach { difficulty ->
                val level = LevelId.entries.first { it.theme == selected.theme && it.difficulty == difficulty }
                SelectorOption(stringResource(difficulty.titleRes), selected == level, Modifier.weight(1f).testTag("level-${level.ordinal + 1}"),
                    { onSelect(level) })
            }
        }
    }
}

@Composable
private fun SelectorGroup(label: String, options: @Composable RowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Eyebrow(label)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = options)
    }
}

@Composable
private fun SelectorOption(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier.clip(MaterialTheme.shapes.small).selectable(selected, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.small, color = if (selected) colors.secondaryContainer else colors.surface,
        border = BorderStroke(1.dp, if (selected) colors.secondary else colors.outline)) {
        Box(Modifier.heightIn(min = 48.dp).padding(horizontal = 6.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) colors.secondary else colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomeScreenPreview() { SprintTheme { HomeScreen(HomeState(), {}) } }

@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun HomeScreenLargeTextPreview() { SprintTheme { HomeScreen(HomeState(), {}) } }
