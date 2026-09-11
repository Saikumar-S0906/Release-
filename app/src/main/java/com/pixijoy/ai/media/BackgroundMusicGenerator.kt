package com.pixijoy.ai.media

import com.pixijoy.ai.domain.model.MusicMood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin

@Singleton
class BackgroundMusicGenerator @Inject constructor() {

    suspend fun generate(mood: MusicMood, durationSec: Float = 8f, outputFile: File): File =
        withContext(Dispatchers.Default) {
            writeTone(outputFile, notesFor(mood), durationSec)
            outputFile
        }

    suspend fun generateMemeTone(sound: com.pixijoy.ai.domain.model.MemeSound, durationSec: Float, outputFile: File): File =
        withContext(Dispatchers.Default) {
            val freqs = when (sound) {
                com.pixijoy.ai.domain.model.MemeSound.BRUH -> listOf(110.0, 87.0)
                com.pixijoy.ai.domain.model.MemeSound.VINE_BOOM -> listOf(55.0, 40.0)
                com.pixijoy.ai.domain.model.MemeSound.LAUGH -> listOf(440.0, 523.0, 440.0)
                com.pixijoy.ai.domain.model.MemeSound.WOW -> listOf(330.0, 660.0)
                com.pixijoy.ai.domain.model.MemeSound.SAD -> listOf(196.0, 174.0)
            }
            writeTone(outputFile, freqs, durationSec)
            outputFile
        }

    private fun writeTone(outputFile: File, notes: List<Double>, durationSec: Float) {
        val sampleRate = 44_100
        val totalSamples = (sampleRate * durationSec).toInt()
        val pcm = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            val noteIndex = ((t * 2) % notes.size).toInt()
            val freq = notes[noteIndex]
            val envelope = fadeEnvelope(i, totalSamples)
            pcm[i] = (sin(2 * PI * freq * t) * 8_000 * envelope).toInt().toShort()
        }
        writeWav(outputFile, pcm, sampleRate)
    }

    private fun notesFor(mood: MusicMood): List<Double> = when (mood) {
        MusicMood.BIRTHDAY -> listOf(523.25, 659.25, 783.99, 659.25)
        MusicMood.CHILL -> listOf(261.63, 329.63, 392.0, 329.63)
        MusicMood.EPIC -> listOf(196.0, 246.94, 293.66, 392.0)
        MusicMood.ROMANTIC -> listOf(349.23, 440.0, 523.25, 440.0)
    }

    private fun fadeEnvelope(index: Int, total: Int): Float {
        val fade = (total * 0.05f).toInt().coerceAtLeast(1)
        return when {
            index < fade -> index / fade.toFloat()
            index > total - fade -> (total - index) / fade.toFloat()
            else -> 1f
        }
    }

    private fun writeWav(file: File, pcm: ShortArray, sampleRate: Int) {
        val channels = 1
        val bits = 16
        val byteRate = sampleRate * channels * bits / 8
        FileOutputStream(file).use { out ->
            val dataSize = pcm.size * 2
            out.write("RIFF".toByteArray())
            out.write(intLE(36 + dataSize))
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            out.write(intLE(16))
            out.write(shortLE(1))
            out.write(shortLE(channels.toShort()))
            out.write(intLE(sampleRate))
            out.write(intLE(byteRate))
            out.write(shortLE((channels * bits / 8).toShort()))
            out.write(shortLE(bits.toShort()))
            out.write("data".toByteArray())
            out.write(intLE(dataSize))
            val buffer = ByteArray(2)
            for (sample in pcm) {
                buffer[0] = (sample.toInt() and 0xFF).toByte()
                buffer[1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                out.write(buffer)
            }
        }
    }

    private fun intLE(value: Int): ByteArray =
        byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )

    private fun shortLE(value: Short): ByteArray =
        byteArrayOf((value.toInt() and 0xFF).toByte(), ((value.toInt() shr 8) and 0xFF).toByte())
}
