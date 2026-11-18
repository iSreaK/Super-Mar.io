package fr.julie.mariosprint.game.input

import android.view.MotionEvent
import fr.julie.mariosprint.domain.game.GameInput
import kotlin.math.abs

/** Géométrie partagée entre le rendu et les zones tactiles : aucune zone invisible différente. */
data class ControlLayout(val width: Float, val height: Float, val density: Float) {
    val panelHeight = 154f * density
    val panelTop = height - panelHeight
    val stickX = 82f * density
    val centerY = panelTop + 70f * density
    val stickRadius = 51f * density
    val stickTravel = 35f * density
    val jumpX = width - 80f * density
    val jumpRadius = 48f * density
}

/** Deux pointeurs indépendants ; la course est intégrée à l'amplitude du stick. */
class TouchControls(private val input: GameInput) {
    var stickOffset = 0f; private set
    var jumpPressed = false; private set
    private var movementPointer = -1
    private var jumpPointer = -1

    fun handle(event: MotionEvent, layout: ControlLayout): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                if (event.getY(index) >= layout.panelTop) {
                    val id = event.getPointerId(index)
                    if (event.getX(index) < layout.width / 2 && movementPointer == -1) {
                        movementPointer = id
                    } else if (event.getX(index) >= layout.width / 2 && jumpPointer == -1) {
                        jumpPointer = id; jumpPressed = true; input.jump = true
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                val id = event.getPointerId(event.actionIndex)
                if (id == movementPointer) { movementPointer = -1; stickOffset = 0f; input.horizontalAxis = 0f }
                if (id == jumpPointer) { jumpPointer = -1; jumpPressed = false; input.jump = false }
            }
            MotionEvent.ACTION_CANCEL -> clear()
        }
        if (movementPointer != -1) {
            val index = event.findPointerIndex(movementPointer)
            if (index >= 0) {
                val normalized = ((event.getX(index) - layout.stickX) / layout.stickTravel).coerceIn(-1f, 1f)
                stickOffset = normalized
                input.horizontalAxis = if (abs(normalized) < .14f) 0f else normalized
            }
        }
        return true
    }

    fun clear() {
        movementPointer = -1; jumpPointer = -1
        stickOffset = 0f; jumpPressed = false; input.clear()
    }
}
