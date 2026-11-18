package fr.julie.mariosprint.game

import fr.julie.mariosprint.domain.game.*

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.Choreographer
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import fr.julie.mariosprint.R
import android.view.View
import fr.julie.mariosprint.game.input.ControlLayout
import fr.julie.mariosprint.game.input.TouchControls
import fr.julie.mariosprint.game.rendering.ControlsRenderer
import fr.julie.mariosprint.game.rendering.GameSceneRenderer
import fr.julie.mariosprint.domain.model.CharacterSkin

/** Adaptateur Android : horloge, entrées et rendu. Les règles du jeu restent dans GameEngine. */
@SuppressLint("ViewConstructor")
class GameView(
    context: Context,
    val engine: GameEngine,
    private val interactive: Boolean = true,
    private val soundEnabled: () -> Boolean = { true },
    skin: CharacterSkin = CharacterSkin.LUIGI,
    private val onUpdate: () -> Unit = {},
) : View(context), Choreographer.FrameCallback {
    private val sceneRenderer = GameSceneRenderer().apply { setSkin(skin) }
    private val controlsRenderer = ControlsRenderer(context.getString(R.string.control_jump), context.getString(R.string.control_run),
        context.getString(R.string.control_move), context.getString(R.string.control_jump_hint))
    private val input = GameInput()
    private val touchControls = TouchControls(input)
    private var layout = ControlLayout(0f, 0f, resources.displayMetrics.density)
    private var audio: GameAudio? = null
    private var frameScheduled = false
    private var lastFrame = 0L
    private var lastUpdate = 0L
    private var lastPhase = engine.phase

    init {
        isFocusableInTouchMode = interactive
        contentDescription = context.getString(if (interactive) R.string.game_controls_description else R.string.level_preview_description)
    }
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        layout = ControlLayout(width.toFloat(), height.toFloat(), resources.displayMetrics.density)
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (interactive) {
            if (audio == null) audio = GameAudio(context, soundEnabled)
            engine.onEvent = { audio?.play(it) }
            requestFocus(); lastFrame = 0L
            refreshFrameScheduling()
        }
    }
    override fun onDetachedFromWindow() {
        if (interactive) {
            stopFrames()
            touchControls.clear(); engine.onEvent = {}
            audio?.close(); audio = null
        }
        super.onDetachedFromWindow()
    }
    override fun doFrame(frameTimeNanos: Long) {
        frameScheduled = false
        if (!isAttachedToWindow || !hasWindowFocus() || windowVisibility != VISIBLE) return
        val delta = if (lastFrame == 0L) 0.0 else (frameTimeNanos - lastFrame) / 1_000_000_000.0
        lastFrame = frameTimeNanos
        val previousPhase = engine.phase
        engine.advance(delta, input)
        if (engine.phase != Phase.RUNNING && engine.phase != Phase.READY) touchControls.clear()
        if (frameTimeNanos - lastUpdate >= 100_000_000 || engine.phase != lastPhase) {
            lastUpdate = frameTimeNanos; lastPhase = engine.phase; onUpdate()
        }
        if (engine.phase == Phase.RUNNING || engine.phase != previousPhase) invalidate()
        if (engine.phase == Phase.RUNNING) refreshFrameScheduling()
    }
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (interactive && !hasWindowFocus) { stopFrames(); audio?.close(); audio = null; touchControls.clear(); engine.pause(); onUpdate() }
        else { if (interactive && isAttachedToWindow && audio == null) audio = GameAudio(context, soundEnabled); refreshFrameScheduling() }
        lastFrame = 0L
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sceneHeight = if (interactive) layout.panelTop.coerceAtLeast(1f) else height.toFloat()
        sceneRenderer.draw(canvas, width.toFloat(), sceneHeight, engine, !interactive)
        if (interactive) controlsRenderer.draw(canvas, layout, touchControls)
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!interactive) return false
        if (engine.phase != Phase.READY && engine.phase != Phase.RUNNING) return true
        val wasJumpPressed = touchControls.jumpPressed
        touchControls.handle(event, layout)
        if (!wasJumpPressed && touchControls.jumpPressed) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        parent?.requestDisallowInterceptTouchEvent(event.actionMasked != MotionEvent.ACTION_UP && event.actionMasked != MotionEvent.ACTION_CANCEL)
        if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
        invalidate(); refreshFrameScheduling(); return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    fun setSkin(skin: CharacterSkin) { if (sceneRenderer.setSkin(skin)) invalidate() }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = handleKey(keyCode, true) || super.onKeyDown(keyCode, event)
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean = handleKey(keyCode, false) || super.onKeyUp(keyCode, event)
    private fun handleKey(code: Int, pressed: Boolean): Boolean {
        when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_A -> input.left = pressed
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> input.right = pressed
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_BUTTON_A -> input.jump = pressed
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT, KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_BUTTON_B -> input.sprint = pressed
            else -> return false
        }
        refreshFrameScheduling()
        return true
    }
    fun refreshFrameScheduling(phase: Phase = engine.phase) {
        if (phase != Phase.RUNNING) lastFrame = 0L
        if (phase != Phase.RUNNING && phase != Phase.READY) {
            stopFrames(); invalidate(); return
        }
        if (interactive && isAttachedToWindow && hasWindowFocus() && windowVisibility == VISIBLE && !frameScheduled &&
            (engine.phase == Phase.RUNNING || engine.phase == Phase.READY)) {
            frameScheduled = true
            Choreographer.getInstance().postFrameCallback(this)
        }
        invalidate()
    }
    private fun stopFrames() {
        if (frameScheduled) Choreographer.getInstance().removeFrameCallback(this)
        frameScheduled = false
        lastFrame = 0L
    }
}
