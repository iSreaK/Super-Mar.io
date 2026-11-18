package fr.julie.mariosprint.ui.screens

import fr.julie.mariosprint.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import fr.julie.mariosprint.ui.theme.SprintTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import fr.julie.mariosprint.domain.game.GameEngine
import fr.julie.mariosprint.domain.game.LevelId
import fr.julie.mariosprint.domain.game.LevelTheme
import fr.julie.mariosprint.domain.game.Difficulty
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import fr.julie.mariosprint.game.GameView
import fr.julie.mariosprint.ui.model.*
import fr.julie.mariosprint.ui.components.*
import fr.julie.mariosprint.ui.formatting.formatRaceTime

@Composable
internal fun HomeScreen(state: HomeState, onAction: (HomeAction) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 22.dp, vertical = 8.dp).testTag("dashboard"),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AppHeader(state.profile, { onAction(HomeAction.Account(it)) })
        state.error?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.tertiary) }
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Eyebrow(stringResource(R.string.home_eyebrow), MaterialTheme.colorScheme.secondary)
            Text(stringResource(R.string.home_title), color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp,
                lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.home_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 21.sp)
        }
        LevelSelector(state.selectedLevel) { onAction(HomeAction.SelectLevel(it)) }
        LevelPreviewCard(state.selectedLevel, state.profile.skin)
        Button(onClick = { onAction(HomeAction.Play) }, modifier = Modifier.fillMaxWidth().height(56.dp).testTag("play"),
            shape = RoundedCornerShape(17.dp)) {
            Text(stringResource(R.string.start_selected_level, state.selectedLevel.ordinal + 1), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.weight(1f)); Icon(painterResource(R.drawable.ic_arrow_forward), null, Modifier.size(22.dp))
        }
        PersonalRecordCard(state.stats, state.selectedLevel)
        Surface(onClick = { onAction(HomeAction.Collection) }, shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth().testTag("open-collection")) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CharacterPortrait(state.profile.skin, Modifier.size(44.dp, 56.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.collection_title), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.collection_teaser, state.profile.unlockedSkins), fontSize = 12.sp)
                }
                Icon(painterResource(R.drawable.ic_arrow_forward), null, Modifier.size(22.dp))
            }
        }
        state.legacyBestMs?.let {
            Text(stringResource(R.string.legacy_best, formatRaceTime(it)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 16.sp)
        }
        OutlinedButton(onClick = { onAction(HomeAction.Leaderboard) }, modifier = Modifier.fillMaxWidth().height(51.dp).testTag("leaderboard"),
            shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Text(stringResource(R.string.open_leaderboard), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f)); Icon(painterResource(R.drawable.ic_arrow_diagonal), null, Modifier.size(20.dp), tint = state.selectedLevel.accentColor())
        }
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.controls_title), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(stringResource(R.string.controls_instructions), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 19.sp)
            }
        }
        BonusGuide()
        ProfileCard(state.profile, { onAction(HomeAction.Account(it)) })
        Eyebrow(stringResource(R.string.offline_level_count))
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun LevelPreviewCard(level: LevelId, skin: fr.julie.mariosprint.domain.model.CharacterSkin) {
    val previewEngine = remember(level) { GameEngine(level).apply { player.x = 230f } }
    Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(24.dp))) {
        if (!androidx.compose.ui.platform.LocalInspectionMode.current) key(level) {
            AndroidView(factory = { GameView(it, previewEngine, interactive = false, skin = skin) },
                update = { it.setSkin(skin) }, modifier = Modifier.fillMaxSize())
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = .85f)))))
        Surface(Modifier.align(Alignment.TopStart).padding(16.dp), shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.background.copy(alpha = .8f)) {
            Text(stringResource(R.string.level_index, level.ordinal + 1), Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(stringResource(level.titleRes), color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(level.descriptionRes), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun PersonalRecordCard(stats: StatsUi, level: LevelId) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Eyebrow(stringResource(R.string.personal_best_label))
                    Text(formatRaceTime(stats.bestMs), color = MaterialTheme.colorScheme.tertiary, fontSize = 25.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                StatusPill(stringResource(R.string.level_index, level.ordinal + 1), level.accentColor())
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Statistic(stringResource(R.string.attempts_label), stats.attempts)
                Statistic(stringResource(R.string.victories_label), stats.finishes)
                Statistic(stringResource(R.string.coins_label), stats.coins)
            }
        }
    }
}

@Composable
private fun Statistic(label: String, count: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(count.toString(), color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Eyebrow(label)
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() { SprintTheme { HomeScreen(HomeState(), {}) } }

@Composable
private fun LevelSelector(selected: LevelId, onSelect: (LevelId) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Eyebrow(stringResource(R.string.choose_world))
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LevelTheme.entries.forEach { theme ->
                val level = LevelId.entries.first { it.theme == theme && it.difficulty == selected.difficulty }
                FilterChip(selected = selected.theme == theme, onClick = { onSelect(level) },
                    label = { Text(stringResource(theme.titleRes), fontSize = 13.sp) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("world-${theme.name}"))
            }
        }
        Eyebrow(stringResource(R.string.choose_difficulty))
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.entries.forEach { difficulty ->
                val level = LevelId.entries.first { it.theme == selected.theme && it.difficulty == difficulty }
                FilterChip(selected = selected == level, onClick = { onSelect(level) },
                    label = { Text(stringResource(difficulty.titleRes), fontSize = 12.sp) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("level-${level.ordinal + 1}"))
            }
        }
    }
}
@Composable private fun BonusGuide() {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.bonuses_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            BonusGuideRow(R.drawable.ic_star, R.string.bonus_star_title, R.string.bonus_star_description)
            BonusGuideRow(R.drawable.ic_shield, R.string.bonus_shield_title, R.string.bonus_shield_description)
            BonusGuideRow(R.drawable.ic_magnet, R.string.bonus_magnet_title, R.string.bonus_magnet_description)
        }
    }
}
@Composable private fun BonusGuideRow(icon: Int, title: Int, description: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(color = MaterialTheme.colorScheme.tertiary.copy(alpha = .12f), shape = RoundedCornerShape(12.dp)) {
            Icon(painterResource(icon), null, Modifier.padding(10.dp).size(22.dp), tint = MaterialTheme.colorScheme.tertiary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(title), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(stringResource(description), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}
