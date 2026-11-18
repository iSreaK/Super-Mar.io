package fr.julie.mariosprint.game

import fr.julie.mariosprint.domain.game.*
import org.junit.Assert.*
import org.junit.Test

class BonusTest {
    private val input get() = GameInput().apply { right = true }
    private fun collect(game: GameEngine, type: BonusType) {
        val bonus = game.level.bonuses.first { it.type == type }
        game.player.x = bonus.x; game.player.y = bonus.y
        game.advance(GameEngine.STEP, input)
        assertTrue(bonus.collected)
    }
    @Test fun allCoursesAreDistinctAndHaveAllThreeBonuses() {
        val levels = LevelId.entries.map(Level::create)
        assertEquals(LevelId.entries.size, levels.map { it.tiles.contentHashCode() }.toSet().size)
        assertEquals(LevelId.entries.size, levels.map { it.id.value }.toSet().size)
        levels.forEach { level ->
            assertEquals(BonusType.entries.toSet(), level.bonuses.map { it.type }.toSet())
            level.bonuses.forEach { assertFalse(level.solidAt(it.x + 7, it.y + 7)) }
        }
        assertFalse(Level.LEGACY_ID in LevelId.rankedIds)
    }
    @Test fun starDefeatsEnemiesWithoutDamagingThePlayer() {
        val game = GameEngine(); collect(game, BonusType.STAR)
        val enemy = game.enemies.first(); enemy.x = game.player.x; enemy.y = game.player.y
        game.advance(GameEngine.STEP, input)
        assertFalse(enemy.alive); assertEquals(Phase.RUNNING, game.phase)
    }
    @Test fun shieldAbsorbsOneHitThenExpires() {
        val game = GameEngine(); collect(game, BonusType.SHIELD)
        val enemy = game.enemies.first(); enemy.x = game.player.x; enemy.y = game.player.y
        game.advance(GameEngine.STEP, input)
        assertFalse(game.player.shield); assertTrue(game.player.invincible > 0)
        assertEquals(Phase.RUNNING, game.phase)
        game.player.invincible = 0f; enemy.x = game.player.x; enemy.y = game.player.y
        game.advance(GameEngine.STEP, input); assertEquals(Phase.DEAD, game.phase)
    }
    @Test fun magnetMovesNearbyCoinsAndCollectsThemOnce() {
        val game = GameEngine(); collect(game, BonusType.MAGNET)
        val coin = game.level.coins.first(); coin.x = game.player.x + 42; coin.y = game.player.y + 2
        val x = coin.x
        repeat(60) { game.advance(GameEngine.STEP, GameInput()) }
        assertTrue(coin.x < x); assertTrue(coin.collected); assertTrue(game.coins > 0)
        val count = game.coins
        repeat(5) { game.advance(GameEngine.STEP, GameInput()) }; assertEquals(count, game.coins)
    }
    @Test fun pauseFreezesBonusDurationsAndRestartClearsThem() {
        val game = GameEngine(); collect(game, BonusType.STAR); collect(game, BonusType.MAGNET)
        game.pause(); val star = game.player.starSeconds; val magnet = game.player.magnetSeconds
        repeat(100) { game.advance(.1, input) }
        assertEquals(star, game.player.starSeconds, 0f); assertEquals(magnet, game.player.magnetSeconds, 0f)
        val fresh = GameEngine(); assertEquals(0f, fresh.player.starSeconds, 0f)
        assertFalse(fresh.player.shield); assertTrue(fresh.level.bonuses.none { it.collected })
    }
    @Test fun timedBonusExpiresAndCannotBeCollectedTwice() {
        val game = GameEngine(); collect(game, BonusType.STAR)
        repeat(120 * 9) { game.advance(GameEngine.STEP, GameInput()) }
        assertEquals(0f, game.player.starSeconds, 0f)
        collect(game, BonusType.STAR); assertEquals(0f, game.player.starSeconds, 0f)
    }
    @Test fun resultCarriesTheCourseIdentity() {
        LevelId.entries.forEach { id ->
            val game = GameEngine(id); game.player.x = Level.FLAG_X
            game.advance(.02, input); assertEquals(id.value, game.result?.levelId)
        }
    }
}
