package fr.julie.mariosprint.data

import fr.julie.mariosprint.data.local.model.*
import fr.julie.mariosprint.data.mapping.*
import fr.julie.mariosprint.data.remote.ProgressRemote
import fr.julie.mariosprint.domain.game.LevelId
import fr.julie.mariosprint.domain.model.CharacterSkin
import org.junit.Assert.*
import org.junit.Test

class ProgressMappingTest {
    @Test fun guestAndAccountOwnersCannotCollide() {
        assertNotEquals(progressOwner(null), progressOwner("guest"))
        assertNotEquals(progressOwner("alice"), progressOwner("bob"))
    }
    @Test fun unknownLevelsCannotGrantMissionsOrAnUnknownSkin() {
        val rows = listOf(CompletedLevelEntity("guest", "not-a-level"))
        val progress = rows.toProgress(AppearanceEntity("guest", "peach"))
        assertTrue(progress.completedLevels.isEmpty())
        assertEquals(CharacterSkin.LUIGI, progress.equippedSkin)
    }
    @Test fun aLevelAddedOnTheServerDoesNotBreakAnOlderVersion() {
        val progress = ProgressRemote(listOf(LevelId.MEADOW.value, "future-level-v1"), "toad").toProgress()
        assertEquals(setOf(LevelId.MEADOW), progress.completedLevels)
        assertEquals(CharacterSkin.TOAD, progress.equippedSkin)
    }
    @Test fun aServerSkinThatKnownLevelsDoNotUnlockFallsBackToLuigi() {
        assertEquals(CharacterSkin.LUIGI, ProgressRemote(listOf("future-level-v1"), "peach").toProgress().equippedSkin)
        assertEquals(CharacterSkin.LUIGI, ProgressRemote(listOf(LevelId.MEADOW.value), "future-skin").toProgress().equippedSkin)
    }
    @Test fun anOversizedServerAnswerIsRefused() {
        try { ProgressRemote(List(51) { "level-$it" }, "luigi").toProgress(); fail("Expected refusal") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun anOlderCloudAcknowledgementCannotOverwriteANewerLocalChoice() {
        val sent = AppearanceEntity("account:alice", "toad", 1, true)
        val current = sent.copy(skinId = "yoshi", revision = 2)
        assertFalse(current.acceptsRemoteSelection(sent, CharacterSkin.TOAD))
    }
    @Test fun cloudCannotAcknowledgeADifferentPendingSelection() {
        val sent = AppearanceEntity("account:alice", "toad", 1, true)
        assertFalse(sent.acceptsRemoteSelection(sent, CharacterSkin.LUIGI))
        assertTrue(sent.acceptsRemoteSelection(sent, CharacterSkin.TOAD))
    }
    @Test fun cachedCompletionsRecoverTheEquippedSkin() {
        val rows = listOf(CompletedLevelEntity("account:alice", LevelId.MEADOW.value))
        val progress = rows.toProgress(AppearanceEntity("account:alice", "toad", 2, true))
        assertEquals(CharacterSkin.TOAD, progress.equippedSkin)
        assertTrue(progress.pendingSelection)
    }
}
