package fr.julie.mariosprint.game

import fr.julie.mariosprint.domain.game.*

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class GameEngineTest {
    private fun frames(game: GameEngine, input: GameInput, count: Int) {
        repeat(count) { game.advance(GameEngine.STEP, input) }
    }
    @Test fun waitingDoesNotStartTheClock() {
        val game = GameEngine(); frames(game, GameInput(), 1200)
        assertEquals(Phase.READY, game.phase); assertEquals(0L, game.elapsedMs)
    }
    @Test fun firstMovementStartsClockAndMovingRightWorks() {
        val game = GameEngine(); frames(game, GameInput().apply { right = true }, 60)
        assertEquals(Phase.RUNNING, game.phase); assertTrue(game.player.x > Level.START_X + 20)
        assertTrue(abs(game.elapsedMs - 500) <= 1)
    }
    @Test fun sprintIsFasterThanWalking() {
        val walk = GameEngine(); val run = GameEngine()
        frames(walk, GameInput().apply { right = true }, 90)
        frames(run, GameInput().apply { right = true; sprint = true }, 90)
        assertTrue(run.player.x > walk.player.x + 20)
    }
    @Test fun fullAnalogStickHasTheSamePhysicsAsKeyboardSprint() {
        val keyboard = GameEngine(); val stick = GameEngine()
        frames(keyboard, GameInput().apply { right = true; sprint = true }, 90)
        frames(stick, GameInput().apply { horizontalAxis = 1f }, 90)
        assertEquals(keyboard.player.x, stick.player.x, .001f)
        assertEquals(keyboard.player.vx, stick.player.vx, .001f)
    }
    @Test fun halfStickMovesMoreSlowlyWithoutASeparateRunButton() {
        val slow = GameEngine(); val fast = GameEngine()
        frames(slow, GameInput().apply { horizontalAxis = .5f }, 90)
        frames(fast, GameInput().apply { horizontalAxis = 1f }, 90)
        assertTrue(slow.player.x > Level.START_X)
        assertTrue(fast.player.x > slow.player.x + 20)
    }
    @Test fun levelBoundaryCannotBeCrossed() {
        val game = GameEngine(); frames(game, GameInput().apply { left = true }, 180)
        assertTrue(game.player.x >= 0)
    }
    @Test fun holdingJumpMakesAHigherJump() {
        fun peak(held: Boolean): Float {
            val game = GameEngine(); val input = GameInput().apply { right = true }
            frames(game, input, 2); input.right = false; input.jump = true
            var minY = game.player.y
            repeat(70) { i -> if (!held && i == 3) input.jump = false
                frames(game, input, 1); minY = minOf(minY, game.player.y) }
            return minY
        }
        assertTrue(peak(true) + 20 < peak(false))
    }
    @Test fun heldJumpDoesNotAutomaticallyJumpAgain() {
        val game = GameEngine(); val input = GameInput().apply { jump = true }
        frames(game, input, 180)
        assertTrue(game.player.grounded); assertEquals(192f, game.player.y, .02f)
    }
    @Test fun jumpInputHasOnlyOneRisingEdgeUntilReleased() {
        val input = GameInput(); input.jump = true; input.jump = true
        assertEquals(1, input.jumpSerial)
        input.jump = false; input.jump = true; assertEquals(2, input.jumpSerial)
    }
    @Test fun pauseInvalidatesTheRunAndFreezesTheClock() {
        val game = GameEngine(); val input = GameInput().apply { right = true }
        frames(game, input, 30); game.pause(); val time = game.elapsedMs
        frames(game, input, 1000)
        assertEquals(time, game.elapsedMs); assertFalse(game.eligible)
        game.resume(); frames(game, input, 10); assertTrue(game.elapsedMs > time); assertFalse(game.eligible)
    }
    @Test fun pauseBeforeFirstMovementCanStillBeResumed() {
        val game = GameEngine(); game.pause(); assertEquals(Phase.PAUSED, game.phase)
        game.resume(); assertEquals(Phase.RUNNING, game.phase)
    }
    @Test fun longFrameSuspendsInsteadOfGivingASlowMotionAdvantage() {
        val game = GameEngine(); val input = GameInput().apply { right = true }
        frames(game, input, 10); game.advance(1.5, input)
        assertEquals(Phase.PAUSED, game.phase); assertFalse(game.eligible)
    }
    @Test fun normalFrameJitterDoesNotShortenTheClock() {
        val game = GameEngine(); val input = GameInput().apply { right = true }
        game.advance(.2, input); game.advance(.1, input)
        assertEquals(300L, game.elapsedMs); assertTrue(game.eligible)
    }
    @Test fun fixedStepGivesTheSamePhysicsAt30And120Fps() {
        val a = GameEngine(); val b = GameEngine(); val input = GameInput().apply { right = true }
        repeat(30) { a.advance(1.0 / 30, input) }; frames(b, input, 120)
        assertEquals(a.player.x, b.player.x, .001f); assertEquals(a.player.y, b.player.y, .001f)
    }
    @Test fun pipesBlockHorizontalMovement() {
        val game = GameEngine(); game.player.x = 28 * 16f - 25
        frames(game, GameInput().apply { right = true; sprint = true }, 100)
        assertTrue(game.player.x + game.player.width <= 28 * 16f + .01f)
    }
    @Test fun fallingInAPitEndsTheRun() {
        val game = GameEngine(); game.player.x = 39 * 16f
        frames(game, GameInput().apply { right = true }, 100)
        assertEquals(Phase.DEAD, game.phase); assertNull(game.result)
    }
    @Test fun coinsAreOnlyCollectedOnce() {
        val game = GameEngine(); val coin = game.level.coins.first()
        game.player.x = coin.x; game.player.y = coin.y
        frames(game, GameInput().apply { right = true }, 2)
        assertTrue(coin.collected); assertEquals(1, game.coins)
        game.player.x = coin.x; game.player.y = coin.y
        frames(game, GameInput(), 2); assertEquals(1, game.coins)
    }
    @Test fun headHitTurnsQuestionIntoUsedBlock() {
        val game = GameEngine(); val input = GameInput().apply { jump = true }
        game.player.x = 17 * 16f + 2
        frames(game, input, 70)
        assertEquals(Tile.USED, game.level.tile(17, 9)); assertEquals(1, game.coins)
    }
    @Test fun mushroomMakesMarioBiggerAndDamageShrinksHim() {
        val game = GameEngine(); val input = GameInput().apply { right = true }
        game.mushrooms += Mushroom(game.player.x, game.player.y)
        frames(game, input, 2); assertTrue(game.player.big); assertEquals(24f, game.player.height)
        game.player.invincible = 0f
        val enemy = game.enemies.first(); enemy.x = game.player.x; enemy.y = game.player.y
        frames(game, input, 1); assertFalse(game.player.big); assertEquals(Phase.RUNNING, game.phase)
        assertTrue(game.player.invincible > 0)
    }
    @Test fun landingOnEnemyStompsIt() {
        val game = GameEngine(); val enemy = game.enemies.first()
        enemy.x = game.player.x; enemy.y = 194f
        game.player.y = 175f; game.player.vy = 170f
        frames(game, GameInput().apply { right = true }, 4)
        assertFalse(enemy.alive); assertTrue(game.player.vy < 0)
    }
    @Test fun flagFinishesAndFreezesTheResult() {
        val game = GameEngine(); game.player.x = Level.FLAG_X - 1
        val input = GameInput().apply { right = true }
        frames(game, input, 30); assertEquals(Phase.FINISHED, game.phase)
        val result = game.result; frames(game, input, 100); assertEquals(result, game.result)
    }
    @Test fun retryStartsWithACompletelyFreshLevel() {
        val old = GameEngine(); old.level.setTile(17, 9, Tile.USED); old.pause()
        val fresh = GameEngine()
        assertEquals(Tile.QUESTION_COIN, fresh.level.tile(17, 9)); assertTrue(fresh.eligible)
        assertEquals(0, fresh.coins); assertEquals(0L, fresh.elapsedMs)
    }
    @Test fun allPitsHaveARunningJumpWithinReach() {
        // Max horizontal travel in one full jump: 2 * 350 / 900 * 132 ≈ 103 units.
        val level = Level.prairie()
        var gap = 0
        for (col in 0 until Level.COLS) {
            if (level.tile(col, 13) == Tile.AIR) gap++ else { assertTrue("Fossé de $gap cases trop large", gap <= 4); gap = 0 }
        }
        assertTrue(level.coins.size + 4 <= 100)
        // Les plateformes hautes de quatre cases restent accessibles depuis le sol.
        assertTrue(350f * 350f / (2 * 900f) > 4 * Level.SIZE)
    }
    @Test fun entireLevelCanBeCompletedUsingOnlyNormalControls() {
        LevelId.entries.forEach { completeLevel(it) }
    }
    private fun completeLevel(levelId: LevelId) {
        val game = GameEngine(levelId)
        val input = GameInput().apply { right = true; sprint = true }
        var releasedOnLanding = false
        repeat(120 * 90) {
            val p = game.player
            if (p.grounded) {
                if (input.jump) {
                    input.jump = false; releasedOnLanding = true
                } else {
                    val ahead = p.x + p.width + 12
                    val feet = p.y + p.height
                    val obstacle = game.level.solidAt(ahead, feet - 4)
                    val gap = !game.level.solidAt(ahead, feet + 2)
                    val enemy = game.enemies.any { e -> e.alive && e.x - (p.x + p.width) in -4f..26f && abs(e.y + e.height - feet) < 20 }
                    input.jump = obstacle || gap || enemy
                    releasedOnLanding = false
                }
            } else if (releasedOnLanding) input.jump = false
            game.advance(GameEngine.STEP, input)
            if (game.phase == Phase.FINISHED) {
                assertTrue(game.eligible); assertTrue(game.elapsedMs in 10_000..90_000)
                return
            }
            assertNotEquals("Mort dans $levelId à x=${p.x}, y=${p.y}, t=${game.elapsedMs}", Phase.DEAD, game.phase)
        }
        fail("Le joueur reste bloqué à x=${game.player.x}, y=${game.player.y}")
    }
}
