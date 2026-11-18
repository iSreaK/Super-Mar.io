package fr.julie.mariosprint

import android.view.MotionEvent
import fr.julie.mariosprint.domain.game.GameInput
import fr.julie.mariosprint.game.input.ControlLayout
import fr.julie.mariosprint.game.input.TouchControls
import org.junit.Assert.*
import org.junit.Test

class TouchControlsTest {
    private val input = GameInput()
    private val controls = TouchControls(input)
    private val layout = ControlLayout(400f, 800f, 1f)

    private fun event(action: Int, pointers: List<Pair<Int, Float>>, y: Float = layout.centerY) {
        val properties = pointers.map { (pointerId, _) -> MotionEvent.PointerProperties().apply {
            id = pointerId; toolType = MotionEvent.TOOL_TYPE_FINGER
        } }.toTypedArray()
        val coordinates = pointers.map { (_, x) -> MotionEvent.PointerCoords().apply {
            this.x = x; this.y = y; pressure = 1f; size = 1f
        } }.toTypedArray()
        val event = MotionEvent.obtain(1, 2, action, pointers.size, properties, coordinates, 0, 0, 1f, 1f, 0, 0, 0, 0)
        controls.handle(event, layout); event.recycle()
    }
    @Test fun fullStickInclinationRunsWithoutAnotherButton() {
        event(MotionEvent.ACTION_DOWN, listOf(0 to 117f))
        assertEquals(1f, input.horizontalAxis, .001f); assertFalse(input.jump)
    }
    @Test fun stickSpeedIsAnalogAndCenterHasADeadZone() {
        event(MotionEvent.ACTION_DOWN, listOf(0 to 99.5f))
        assertEquals(.5f, input.horizontalAxis, .001f)
        event(MotionEvent.ACTION_MOVE, listOf(0 to 84f)); assertEquals(0f, input.horizontalAxis, .001f)
        event(MotionEvent.ACTION_MOVE, listOf(0 to 0f)); assertEquals(-1f, input.horizontalAxis, .001f)
    }
    @Test fun movementAndJumpUseIndependentPointers() {
        event(MotionEvent.ACTION_DOWN, listOf(0 to 117f))
        event(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), listOf(0 to 117f, 1 to 320f))
        assertTrue(input.jump); assertEquals(1f, input.horizontalAxis, .001f)
        // Lever le pouce gauche ne relâche pas le saut maintenu par le pouce droit.
        event(MotionEvent.ACTION_POINTER_UP, listOf(0 to 117f, 1 to 320f))
        assertEquals(0f, input.horizontalAxis, .001f); assertTrue(input.jump)
        event(MotionEvent.ACTION_MOVE, listOf(1 to 322f)); assertTrue(input.jump)
        event(MotionEvent.ACTION_UP, listOf(1 to 322f)); assertFalse(input.jump)
    }
    @Test fun movingAFingerDoesNotGenerateRepeatedJumpPresses() {
        event(MotionEvent.ACTION_DOWN, listOf(0 to 320f))
        repeat(20) { event(MotionEvent.ACTION_MOVE, listOf(0 to (300f + it))) }
        assertEquals(1, input.jumpSerial)
    }
    @Test fun cancellationClearsAllControls() {
        event(MotionEvent.ACTION_DOWN, listOf(0 to 117f))
        event(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), listOf(0 to 117f, 1 to 320f))
        event(MotionEvent.ACTION_CANCEL, listOf(0 to 117f, 1 to 320f))
        assertFalse(input.jump); assertEquals(0f, input.horizontalAxis, .001f)
    }
    @Test fun touchingTheSceneDoesNotTriggerAControl() {
        event(MotionEvent.ACTION_DOWN, listOf(0 to 320f), y = 100f)
        assertFalse(input.jump); assertEquals(0f, input.horizontalAxis, .001f)
    }
}
