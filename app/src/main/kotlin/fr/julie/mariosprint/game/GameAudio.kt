package fr.julie.mariosprint.game

import fr.julie.mariosprint.domain.game.*

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import fr.julie.mariosprint.R

/** Cycle de vie des sons séparé de la vue et de la simulation. */
internal class GameAudio(context: Context, private val enabled: () -> Boolean) : AutoCloseable {
    private val pool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val sounds = mapOf(
        GameEvent.JUMP to R.raw.jump, GameEvent.COIN to R.raw.coin, GameEvent.STOMP to R.raw.stomp,
        GameEvent.HURT to R.raw.hurt, GameEvent.POWER to R.raw.power, GameEvent.FINISH to R.raw.finish,
        GameEvent.BLOCK to R.raw.stomp,
    ).mapValues { (_, resource) -> pool.load(context, resource, 1) }

    fun play(event: GameEvent) {
        if (enabled()) sounds[event]?.let { pool.play(it, .35f, .35f, 1, 0, 1f) }
    }
    override fun close() { pool.release() }
}
