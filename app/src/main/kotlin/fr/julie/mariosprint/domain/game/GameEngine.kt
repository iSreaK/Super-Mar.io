package fr.julie.mariosprint.domain.game

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class Phase { READY, RUNNING, PAUSED, DEAD, FINISHED }
enum class GameEvent { JUMP, COIN, STOMP, POWER, HURT, FINISH, BLOCK }
class GameInput {
    /** Axe analogique du stick, de -1 à +1 ; les boutons restent utilisables au clavier. */
    var horizontalAxis = 0f
        set(value) { field = value.coerceIn(-1f, 1f) }
    var left = false
    var right = false
    var sprint = false
    var jump = false
        set(value) { if (value && !field) jumpSerial++; field = value }
    var jumpSerial = 0; private set
    fun clear() { horizontalAxis = 0f; left = false; right = false; sprint = false; jump = false }
}
open class Body(var x: Float, var y: Float, val width: Float, var height: Float) {
    var vx = 0f
    var vy = 0f
    var grounded = false
    fun overlaps(other: Body) = x < other.x + other.width && x + width > other.x &&
        y < other.y + other.height && y + height > other.y
}
class Player : Body(Level.START_X, Level.START_Y, 12f, 16f) {
    var big = false
    var facing = 1
    var invincible = 0f
    var starSeconds = 0f
    var magnetSeconds = 0f
    var shield = false
}
class Enemy(x: Float, y: Float) : Body(x, y, 14f, 14f) {
    var alive = true
    var squashed = 0f
    init { vx = -24f }
}
class Mushroom(x: Float, y: Float) : Body(x, y, 14f, 14f) {
    var alive = true
    init { vx = 36f; vy = -70f }
}
data class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float)
data class RunResult(val timeMs: Long, val coins: Int, val eligible: Boolean, val levelId: String = Level.ID)

/**
 * Simulation déterministe à 120 Hz. Le chrono suit le temps réel reçu, même si une
 * image tarde. Les interruptions invalident le classement ; aucun temps de pause
 * ne peut donc améliorer un record en ligne.
 */
class GameEngine(val levelId: LevelId = LevelId.MEADOW) {
    val level = Level.create(levelId)
    val player = Player()
    val enemies = level.enemies.map { Enemy(it.x, it.y) }
    val mushrooms = mutableListOf<Mushroom>()
    val sparks = mutableListOf<Spark>()
    var phase = Phase.READY; private set
    var eligible = true; private set
    var coins = 0; private set
    var elapsedSeconds = 0.0; private set
    val elapsedMs get() = (elapsedSeconds * 1000).toLong()
    val progress get() = ((player.x - Level.START_X) / (Level.FLAG_X - Level.START_X)).coerceIn(0f, 1f)
    val result get() = if (phase == Phase.FINISHED) RunResult(elapsedMs, coins, eligible, levelId.value) else null
    var onEvent: (GameEvent) -> Unit = {}
    private var accumulator = 0.0
    private var jumpBuffer = 0f
    private var coyote = 0f
    private var lastJumpSerial = 0

    fun pause() {
        if (phase == Phase.RUNNING || phase == Phase.READY) { eligible = false; phase = Phase.PAUSED }
    }
    fun resume() { if (phase == Phase.PAUSED) { phase = Phase.RUNNING; accumulator = 0.0 } }

    fun advance(seconds: Double, input: GameInput) {
        if (phase == Phase.READY && (input.left || input.right || input.jump || abs(input.horizontalAxis) > .05f)) phase = Phase.RUNNING
        if (phase != Phase.RUNNING) return
        val delta = seconds.coerceAtLeast(0.0)
        if (delta > .5) { pause(); return }
        elapsedSeconds += delta
        if (elapsedMs > Level.MAX_TIME_MS) { die(); return }
        if (input.jumpSerial != lastJumpSerial) {
            jumpBuffer = .13f
            lastJumpSerial = input.jumpSerial
        }
        accumulator += delta
        while (accumulator + 1e-9 >= STEP && phase == Phase.RUNNING) {
            simulate(STEP.toFloat(), input)
            accumulator -= STEP
        }
    }

    private fun simulate(dt: Float, input: GameInput) {
        player.invincible = max(0f, player.invincible - dt)
        player.starSeconds = max(0f, player.starSeconds - dt)
        player.magnetSeconds = max(0f, player.magnetSeconds - dt)
        for (bonus in level.bonuses) if (!bonus.collected && player.x < bonus.x + 14 &&
            player.x + player.width > bonus.x && player.y < bonus.y + 14 && player.y + player.height > bonus.y) {
            bonus.collected = true
            when (bonus.type) {
                BonusType.STAR -> player.starSeconds = 8f
                BonusType.SHIELD -> player.shield = true
                BonusType.MAGNET -> player.magnetSeconds = 10f
            }
            burst(bonus.x + 7, bonus.y + 7); onEvent(GameEvent.POWER)
        }
        if (player.magnetSeconds > 0) for (coin in level.coins) if (!coin.collected) {
            val dx = player.x + 2 - coin.x; val dy = player.y + 3 - coin.y
            val distanceSquared = dx * dx + dy * dy
            if (distanceSquared < 64f * 64f && distanceSquared > 1f) {
                val factor = min(1f, dt * 220f / kotlin.math.sqrt(distanceSquared))
                coin.x += dx * factor; coin.y += dy * factor
            }
        }
        coyote = if (player.grounded) .09f else max(0f, coyote - dt)
        jumpBuffer = max(0f, jumpBuffer - dt)
        val axis = input.horizontalAxis.takeIf { abs(it) > .05f }
        val direction = axis?.let { if (it > 0) 1 else -1 }
            ?: ((if (input.right) 1 else 0) - (if (input.left) 1 else 0))
        val target = axis?.times(132f) ?: (direction * if (input.sprint) 132f else 84f)
        val acceleration = if (player.grounded) 820f else 550f
        player.vx = approach(player.vx, target, acceleration * dt)
        if (direction != 0) player.facing = direction
        if (jumpBuffer > 0 && coyote > 0) {
            player.vy = -350f; player.grounded = false
            jumpBuffer = 0f; coyote = 0f; onEvent(GameEvent.JUMP)
        }
        if (!input.jump && player.vy < -145f) player.vy = -145f
        moveX(player, player.vx * dt)
        player.vy = min(player.vy + 900f * dt, 440f)
        moveY(player, player.vy * dt, true)
        if (player.y > Level.HEIGHT + 24) { die(); return }
        for (coin in level.coins) if (!coin.collected && player.x < coin.x + 8 &&
            player.x + player.width > coin.x && player.y < coin.y + 11 && player.y + player.height > coin.y) {
            coin.collected = true; collectCoin(coin.x, coin.y)
        }
        for (enemy in enemies) {
            if (enemy.squashed > 0) enemy.squashed = max(0f, enemy.squashed - dt)
            if (!enemy.alive) continue
            val velocity = enemy.vx
            if (moveX(enemy, velocity * dt)) enemy.vx = -velocity
            enemy.vy = min(enemy.vy + 900f * dt, 440f)
            moveY(enemy, enemy.vy * dt, false)
            if (enemy.y > Level.HEIGHT + 24) enemy.alive = false
            if (enemy.alive && player.overlaps(enemy)) {
                if (player.starSeconds > 0) {
                    enemy.alive = false; enemy.squashed = .3f; burst(enemy.x, enemy.y); onEvent(GameEvent.STOMP)
                } else if (player.vy > 0 && player.y + player.height - enemy.y < 9f) {
                    enemy.alive = false; enemy.squashed = .3f
                    player.y = enemy.y - player.height; player.vy = -205f
                    onEvent(GameEvent.STOMP)
                } else damage()
            }
        }
        for (m in mushrooms) if (m.alive) {
            val velocity = m.vx
            if (moveX(m, velocity * dt)) m.vx = -velocity
            m.vy = min(m.vy + 900f * dt, 440f); moveY(m, m.vy * dt, false)
            if (m.y > Level.HEIGHT + 24) m.alive = false
            if (player.overlaps(m)) {
                m.alive = false
                if (!player.big) { player.big = true; player.y -= 8; player.height = 24f }
                player.invincible = 1f; onEvent(GameEvent.POWER)
            }
        }
        val iterator = sparks.iterator()
        while (iterator.hasNext()) {
            val s = iterator.next(); s.life -= dt
            if (s.life <= 0) iterator.remove() else {
                s.x += s.vx * dt; s.y += s.vy * dt; s.vy += 250f * dt
            }
        }
        if (phase == Phase.RUNNING && player.x >= Level.FLAG_X) {
            phase = Phase.FINISHED; onEvent(GameEvent.FINISH)
        }
    }

    private fun moveX(body: Body, dx: Float): Boolean {
        body.x += dx
        var hit = false
        forTiles(body) { col, row ->
            if (level.tile(col, row) != Tile.AIR && overlapsTile(body, col, row)) {
                if (dx > 0) body.x = col * Level.SIZE - body.width
                else if (dx < 0) body.x = (col + 1) * Level.SIZE
                body.vx = 0f; hit = true
            }
        }
        return hit
    }
    private fun moveY(body: Body, dy: Float, hitBlocks: Boolean) {
        body.y += dy; body.grounded = false
        var hitHead = false
        forTiles(body) { col, row ->
            if (level.tile(col, row) != Tile.AIR && overlapsTile(body, col, row)) {
                if (dy > 0) { body.y = row * Level.SIZE - body.height; body.grounded = true }
                else if (dy < 0) {
                    body.y = (row + 1) * Level.SIZE
                    if (hitBlocks && !hitHead) { hitBlock(col, row); hitHead = true }
                }
                body.vy = 0f
            }
        }
    }
    private fun overlapsTile(b: Body, col: Int, row: Int): Boolean {
        val x = col * Level.SIZE; val y = row * Level.SIZE
        return b.x < x + Level.SIZE - .001f && b.x + b.width > x + .001f &&
            b.y < y + Level.SIZE - .001f && b.y + b.height > y + .001f
    }
    private inline fun forTiles(body: Body, block: (Int, Int) -> Unit) {
        val left = floor(body.x / Level.SIZE).toInt()
        val right = floor((body.x + body.width - .001f) / Level.SIZE).toInt()
        val top = floor(body.y / Level.SIZE).toInt()
        val bottom = floor((body.y + body.height - .001f) / Level.SIZE).toInt()
        for (row in top..bottom) for (col in left..right) block(col, row)
    }
    private fun hitBlock(col: Int, row: Int) {
        when (level.tile(col, row)) {
            Tile.QUESTION_COIN -> {
                level.setTile(col, row, Tile.USED); collectCoin(col * 16f + 4, row * 16f - 12)
            }
            Tile.QUESTION_MUSHROOM -> {
                level.setTile(col, row, Tile.USED)
                mushrooms += Mushroom(col * 16f + 1, row * 16f - 15); onEvent(GameEvent.BLOCK)
            }
            Tile.BRICK -> {
                if (player.big) { level.setTile(col, row, Tile.AIR); burst(col * 16f + 8, row * 16f + 8) }
                onEvent(GameEvent.BLOCK)
            }
            else -> Unit
        }
    }
    private fun collectCoin(x: Float, y: Float) { coins++; burst(x, y); onEvent(GameEvent.COIN) }
    private fun burst(x: Float, y: Float) {
        repeat(4) { i -> sparks += Spark(x, y, (i - 1.5f) * 24, -90f - (i % 2) * 28, .42f) }
    }
    private fun damage() {
        if (player.invincible > 0 || player.starSeconds > 0 || phase != Phase.RUNNING) return
        onEvent(GameEvent.HURT)
        if (player.shield) {
            player.shield = false; player.invincible = 2f
        } else if (player.big) {
            player.big = false; player.y += 8; player.height = 16f; player.invincible = 2f
        } else die(false)
    }
    private fun die(playSound: Boolean = true) {
        phase = Phase.DEAD
        if (playSound) onEvent(GameEvent.HURT)
    }
    companion object {
        const val STEP = 1.0 / 120.0
        private fun approach(value: Float, target: Float, delta: Float) = when {
            abs(target - value) <= delta -> target
            value < target -> value + delta
            else -> value - delta
        }
    }
}
