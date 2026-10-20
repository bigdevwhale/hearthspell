package app.hearthspell.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** The little chiptune sounds of the tower, synthesised on the fly: no sound files. */
enum class Tone { CAST, POOF, GIFT, PURR, BREW, FANFARE, CLINK, PAT }

object Chime {
    private const val RATE = 22050
    private val worker = Executors.newSingleThreadExecutor()
    private val cache = HashMap<String, ShortArray>()

    /** Casts in a row within [COMBO_MS] of each other climb a pentatonic scale. */
    private const val COMBO_MS = 90_000L
    private var combo = 0
    private var lastCast = 0L

    /** The next step of the cast chain, 0 for a first cast. Nothing is lost when the chain breaks. */
    fun nextStep(now: Long = System.currentTimeMillis()): Int {
        combo = if (now - lastCast < COMBO_MS) (combo + 1).coerceAtMost(PENTA.lastIndex) else 0
        lastCast = now
        return combo
    }

    fun play(tone: Tone, step: Int = 0) {
        worker.execute {
            runCatching {
                val pcm = synchronized(cache) { cache.getOrPut("$tone:$step") { render(tone, step) } }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(pcm.size * 1000L / RATE + 60)
                track.release()
            }
        }
    }

    /** Semitones of a major pentatonic over two octaves. */
    private val PENTA = intArrayOf(0, 2, 4, 7, 9, 12, 14, 16, 19, 21, 24)

    private fun hz(semis: Int, base: Double = 523.25): Double = base * 2.0.pow(semis / 12.0)

    private class Buf(ms: Int) {
        val data = FloatArray(RATE * ms / 1000)
    }

    private enum class Wave { SQUARE, TRIANGLE, SINE }

    /** Adds a note at [startMs] with a quick attack and an exponential decay. */
    private fun Buf.note(startMs: Int, ms: Int, freq: Double, wave: Wave, vol: Float, glideTo: Double = freq) {
        val s0 = RATE * startMs / 1000
        val n = RATE * ms / 1000
        var phase = 0.0
        for (i in 0 until n) {
            val k = s0 + i
            if (k >= data.size) break
            val t = i / n.toDouble()
            val f = freq + (glideTo - freq) * t
            phase += f / RATE
            val p = phase % 1.0
            val v = when (wave) {
                Wave.SQUARE -> if (p < 0.25) 1.0 else -1.0
                Wave.TRIANGLE -> 1.0 - 4.0 * kotlin.math.abs(p - 0.5)
                Wave.SINE -> sin(2 * PI * p)
            }
            val env = (if (i < 80) i / 80.0 else 1.0) * (1.0 - t).pow(1.6)
            data[k] += (v * env * vol).toFloat()
        }
    }

    /** Noise held for [hold] samples (bigger is lower), shaped by [shape] over time. */
    private fun Buf.noise(startMs: Int, ms: Int, vol: Float, seed: Int, hold: (Double) -> Int, shape: (Double) -> Double) {
        val r = Random(seed)
        val s0 = RATE * startMs / 1000
        val n = RATE * ms / 1000
        var v = 0f
        for (i in 0 until n) {
            val k = s0 + i
            if (k >= data.size) break
            val t = i / n.toDouble()
            if (i % hold(t).coerceAtLeast(1) == 0) v = r.nextFloat() * 2 - 1
            data[k] += (v * vol * shape(t)).toFloat()
        }
    }

    private fun render(tone: Tone, step: Int): ShortArray {
        val b = when (tone) {
            Tone.CAST -> Buf(260).apply {
                val s = PENTA[step.coerceIn(0, PENTA.lastIndex)]
                note(0, 80, hz(s), Wave.SQUARE, 0.22f)
                note(70, 190, hz(s + 7), Wave.SQUARE, 0.2f)
                note(70, 190, hz(s + 19), Wave.TRIANGLE, 0.12f)
            }
            Tone.POOF -> Buf(220).apply { noise(0, 220, 0.35f, 3, { t -> 2 + (t * 14).toInt() }) { t -> (1 - t).pow(2) } }
            Tone.GIFT -> Buf(420).apply {
                listOf(12, 16, 19, 24).forEachIndexed { i, s -> note(i * 70, 200, hz(s), Wave.TRIANGLE, 0.28f) }
                note(280, 140, hz(31), Wave.SINE, 0.12f)
            }
            Tone.PURR -> Buf(1100).apply {
                noise(0, 1100, 0.32f, 9, { 40 }) { t -> (0.55 + 0.45 * sin(2 * PI * t * 24)) * sin(PI * t) }
            }
            Tone.BREW -> Buf(700).apply {
                val r = Random(5)
                repeat(7) { i -> val f = 300.0 + r.nextInt(400); note(i * 90, 80, f, Wave.SINE, 0.25f, glideTo = f * 1.8) }
            }
            Tone.FANFARE -> Buf(900).apply {
                note(0, 120, hz(0), Wave.SQUARE, 0.2f)
                note(120, 120, hz(4), Wave.SQUARE, 0.2f)
                note(240, 120, hz(7), Wave.SQUARE, 0.2f)
                note(360, 520, hz(12), Wave.SQUARE, 0.22f)
                note(360, 520, hz(16), Wave.TRIANGLE, 0.14f)
            }
            Tone.CLINK -> Buf(500).apply {
                note(0, 300, 2093.0, Wave.TRIANGLE, 0.22f)
                note(140, 340, 2637.0, Wave.TRIANGLE, 0.2f)
            }
            Tone.PAT -> Buf(160).apply { note(0, 160, 880.0, Wave.SINE, 0.3f, glideTo = 620.0) }
        }
        return ShortArray(b.data.size) { i -> (b.data[i].coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.8f).toInt().toShort() }
    }
}

/** Short buzzes that grow a little with each cast in a chain. */
object Buzz {
    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)

    /** [strength] 0..1. */
    fun tap(context: Context, strength: Float = 0.5f, ms: Long = 30) {
        val v = vibrator(context) ?: return
        if (!v.hasVibrator()) return
        val amp = (40 + strength.coerceIn(0f, 1f) * 215).toInt()
        runCatching { v.vibrate(VibrationEffect.createOneShot(ms, if (v.hasAmplitudeControl()) amp else VibrationEffect.DEFAULT_AMPLITUDE)) }
    }

    /** A soft rumble, like purring. */
    fun purr(context: Context) {
        val v = vibrator(context) ?: return
        if (!v.hasVibrator()) return
        val timings = LongArray(16) { 40 }
        val amps = IntArray(16) { if (it % 2 == 0) 60 else 0 }
        runCatching { v.vibrate(VibrationEffect.createWaveform(timings, if (v.hasAmplitudeControl()) amps else IntArray(16) { if (it % 2 == 0) 255 else 0 }, -1)) }
    }
}

/** Plays a sound and a buzz, each if the settings allow it. */
class Feel(private val context: Context, private val sound: Boolean, private val haptics: Boolean) {
    fun cast(step: Int) {
        if (sound) Chime.play(Tone.CAST, step)
        if (haptics) Buzz.tap(context, 0.35f + step * 0.065f, 25L + step * 4)
    }

    fun tone(t: Tone, buzz: Float = 0.4f) {
        if (sound) Chime.play(t)
        if (haptics && buzz > 0f) Buzz.tap(context, buzz)
    }

    fun purr() {
        if (sound) Chime.play(Tone.PURR)
        if (haptics) Buzz.purr(context)
    }
}
