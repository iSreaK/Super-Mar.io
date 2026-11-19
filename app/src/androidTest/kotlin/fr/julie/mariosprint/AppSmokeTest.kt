package fr.julie.mariosprint

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import fr.julie.mariosprint.game.GameView
import fr.julie.mariosprint.domain.game.Level
import fr.julie.mariosprint.domain.game.Phase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class AppSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun keepScreenOn() {
        compose.runOnUiThread { compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        compose.waitUntil(10000) { compose.onAllNodesWithTag("dashboard").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val path = File(compose.activity.getExternalFilesDir(null), "qa-$name.png")
        path.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun findGame(view: View): GameView? {
        if (view is GameView && view.isFocusableInTouchMode) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findGame(view.getChildAt(i))?.let { return it }
        return null
    }
    @Test fun homeAndLeaderboardWorkWithoutSupabase() {
        compose.onNodeWithTag("dashboard").assertExists(); capture("home")
        compose.onNodeWithTag("open-profile").performClick()
        compose.onNodeWithTag("profile-screen").assertExists()
        compose.onNodeWithTag("open-about").performScrollTo().performClick()
        compose.onNodeWithTag("about-screen").assertExists()
        compose.onNodeWithText("iSreaK").assertExists()
        compose.onNodeWithText("KiSs").assertExists()
        compose.onNodeWithTag("home").performClick()
        compose.onNodeWithTag("home").performClick()
        compose.onNodeWithTag("open-friends").performScrollTo().performClick()
        compose.onNodeWithTag("friends-screen").assertExists()
        compose.onNodeWithText("Ouvrir mon compte").assertExists()
        compose.onNodeWithTag("home").performClick()
        compose.onNodeWithTag("open-help").performClick()
        compose.onNodeWithTag("help-screen").assertExists()
        compose.onNodeWithTag("home").performClick()
        compose.onNodeWithTag("leaderboard").performClick()
        compose.onNodeWithTag("ranking-screen").assertExists()
        compose.onNodeWithText("Prêt à te mesurer aux autres ?").assertExists(); capture("ranking")
        compose.onNodeWithTag("home").performClick()
        compose.onNodeWithTag("dashboard").assertExists()
    }
    @Test fun pauseRetryAndBackWorkBeforeFirstMovement() {
        compose.onNodeWithTag("play").performClick()
        compose.onNodeWithTag("game-screen").assertExists(); capture("game")
        compose.onNodeWithTag("pause").performClick()
        compose.onNodeWithTag("resume").assertExists(); capture("pause")
        compose.onNodeWithTag("restart").performClick()
        compose.onNodeWithTag("ready-hint").assertExists()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("quit").performClick()
        compose.onNodeWithTag("dashboard").assertExists()
    }
    @Test fun twoThumbsCanRunAndJumpTogether() {
        compose.onNodeWithTag("play").performClick()
        compose.onNodeWithTag("game-screen").assertExists()
        compose.waitUntil(5000) { var found: GameView? = null; compose.runOnUiThread { found = findGame(compose.activity.window.decorView) }; found != null }
        lateinit var game: GameView
        compose.runOnUiThread { game = checkNotNull(findGame(compose.activity.window.decorView)) }
        compose.waitUntil(3000) { game.isLaidOut && game.hasWindowFocus() }
        val downTime = SystemClock.uptimeMillis()
        fun touch(action: Int, xs: List<Float>) {
            compose.runOnUiThread {
                val d = game.resources.displayMetrics.density
                val props = Array(xs.size) { i -> MotionEvent.PointerProperties().apply { id = i; toolType = MotionEvent.TOOL_TYPE_FINGER } }
                val coords = Array(xs.size) { i -> MotionEvent.PointerCoords().apply { x = xs[i] * d; y = game.height - 84 * d; pressure = 1f; size = 1f } }
                val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, xs.size, props, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
                game.dispatchTouchEvent(event); event.recycle()
            }
        }
        val width = game.width / game.resources.displayMetrics.density
        touch(MotionEvent.ACTION_DOWN, listOf(117f))
        compose.waitUntil(3000) { game.engine.player.x > Level.START_X + 6 }
        touch(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), listOf(117f, width - 80))
        compose.waitUntil(3000) { game.engine.player.y < Level.START_Y - 15 }
        touch(MotionEvent.ACTION_MOVE, listOf(117f, width - 80))
        compose.runOnUiThread { assertTrue(game.engine.player.vx > 110); assertEquals(Phase.RUNNING, game.engine.phase) }
        touch(MotionEvent.ACTION_CANCEL, listOf(117f, width - 80))
        compose.onNodeWithTag("pause").performClick()
        compose.onNodeWithTag("quit").performClick()
    }
    @Test fun resumeAfterLongPauseContinuesAsPractice() {
        compose.onNodeWithTag("play").performClick()
        compose.waitUntil(5000) { var found: GameView? = null; compose.runOnUiThread { found = findGame(compose.activity.window.decorView) }; found != null }
        compose.onNodeWithTag("pause").performClick()
        SystemClock.sleep(650)
        compose.onNodeWithTag("resume").performClick()
        lateinit var game: GameView
        compose.runOnUiThread { game = checkNotNull(findGame(compose.activity.window.decorView)) }
        compose.waitUntil(3000) { game.engine.elapsedMs > 100 }
        compose.runOnUiThread { assertEquals(Phase.RUNNING, game.engine.phase); assertFalse(game.engine.eligible) }
        compose.onNodeWithTag("pause").performClick()
        compose.onNodeWithTag("quit").performClick()
    }

}
