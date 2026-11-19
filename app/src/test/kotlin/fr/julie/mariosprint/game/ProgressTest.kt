package fr.julie.mariosprint.game

import fr.julie.mariosprint.domain.game.*
import fr.julie.mariosprint.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ProgressTest {
    @Test fun newPlayerStartsWithLuigiOnly() {
        val progress = PlayerProgress()
        assertEquals(CharacterSkin.LUIGI, progress.equippedSkin)
        assertEquals(setOf(CharacterSkin.LUIGI), progress.unlockedSkins)
    }
    @Test fun firstFinishedLevelUnlocksToad() {
        assertTrue(CharacterSkin.TOAD in PlayerProgress(setOf(LevelId.MEADOW)).unlockedSkins)
    }
    @Test fun threeCoursesInTheSameWorldDoNotUnlockYoshi() {
        val progress = PlayerProgress(setOf(LevelId.MEADOW, LevelId.MEADOW_TRAILS, LevelId.MEADOW_SUMMITS))
        assertEquals(1, MissionId.THREE_WORLDS.progress(progress.completedLevels))
        assertFalse(CharacterSkin.YOSHI in progress.unlockedSkins)
    }
    @Test fun oneCourseInEachWorldUnlocksYoshiWithoutExpertRequirement() {
        val progress = PlayerProgress(setOf(LevelId.MEADOW, LevelId.CANYON_OASIS, LevelId.NIGHT_LANTERNS))
        assertTrue(CharacterSkin.YOSHI in progress.unlockedSkins)
        assertFalse(CharacterSkin.PEACH in progress.unlockedSkins)
    }
    @Test fun peachRequiresAllThreeDifferentExpertCourses() {
        val levels = setOf(LevelId.MEADOW_SUMMITS, LevelId.CANYON_RIDGES)
        assertFalse(CharacterSkin.PEACH in PlayerProgress(levels).unlockedSkins)
        assertTrue(CharacterSkin.PEACH in PlayerProgress(levels + LevelId.NIGHT).unlockedSkins)
        assertEquals(3, MissionId.EXPERT_FINISHES.progress(levels + LevelId.NIGHT))
    }
    @Test fun catalogueHasOneCourseForEveryWorldAndDifficulty() {
        assertEquals(9, LevelId.entries.size)
        assertEquals(9, LevelId.entries.map { it.value }.distinct().size)
        LevelTheme.entries.forEach { theme -> Difficulty.entries.forEach { difficulty ->
            assertEquals(1, LevelId.entries.count { it.theme == theme && it.difficulty == difficulty })
        } }
    }
    @Test fun everyRankedCourseHasTheSameVersionInSql() {
        val migrations = File("../supabase/migrations").listFiles().orEmpty().filter { it.extension == "sql" }
        val sql = migrations.joinToString("\n") { it.readText() }
        val inserts = Regex("""(?is)insert\s+into\s+public[.]game_levels\s*\([^;]*?\)\s*values\s*([^;]*);""")
            .findAll(sql).joinToString("\n") { it.groupValues[1] }
        assertTrue("No game_levels INSERT statements found", inserts.isNotBlank())
        val levels = Regex("\\('([a-z0-9-]+)',\\s*([0-9]+)").findAll(inserts)
            .associate { it.groupValues[1] to it.groupValues[2].toInt() }
        assertEquals(LevelId.entries.associate { it.value to it.version }, levels)
    }
    @Test fun coursesHaveDifferentGeometryAndBoundedCoinTotals() {
        val hashes = LevelId.entries.map { level ->
            val course = Level.create(level)
            assertTrue(course.coins.size + course.tiles.count { it == Tile.QUESTION_COIN } <= 100)
            course.tiles.contentHashCode()
        }
        assertEquals(LevelId.entries.size, hashes.distinct().size)
    }
    @Test fun publishedCoursesKeepTheirOriginalIdsAndLandmarks() {
        assertEquals("prairie-1-v2", LevelId.MEADOW.value)
        assertEquals(2, LevelId.MEADOW.version)
        assertEquals(Tile.PIPE_TOP, Level.create(LevelId.MEADOW).tile(28, 11))
        assertEquals(Tile.AIR, Level.create(LevelId.CANYON).tile(33, 13))
        assertEquals(Tile.AIR, Level.create(LevelId.NIGHT).tile(28, 13))
        assertEquals(9, Level.create(LevelId.NIGHT).enemies.size)
    }
}
