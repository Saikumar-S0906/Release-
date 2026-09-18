package com.pixijoy.movie

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.pow

/**
 * Simple compressor + optional ceiling limiter for master bus.
 * Pass-through when ratio≈1 and limiter off with 0 makeup.
 */
class DynamicsAudioProcessor(
    private val thresholdDb: Float = 0f,
    private val ratio: Float = 1f,
    private val attackMs: Float = 10f,
    private val releaseMs: Float = 100f,
    private val makeupDb: Float = 0f,
    private val limiterCeilingDb: Float = -1f,
    private val limiterEnabled: Boolean = false
) : BaseAudioProcessor() {
    private val makeup = dbToLin(makeupDb)
    private val threshold = dbToLin(thresholdDb)
    private val ceiling = dbToLin(limiterCeilingDb)
    private val safeRatio = ratio.coerceAtLeast(1f)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytes = inputBuffer.remaining()
        val out = replaceOutputBuffer(bytes)
        val identity = safeRatio <= 1.001f && !limiterEnabled && kotlin.math.abs(makeup - 1f) < 0.001f
        if (identity) {
            out.put(inputBuffer)
            out.flip()
            return
        }
        // Keep attack/release in signature for API parity with Timeline masterFx; v1 is sample-wise.
        @Suppress("UNUSED_VARIABLE") val attack = attackMs
        @Suppress("UNUSED_VARIABLE") val release = releaseMs
        repeat(bytes / 2) {
            val sample = inputBuffer.short.toFloat() / Short.MAX_VALUE
            val abs = kotlin.math.abs(sample)
            var y = sample
            if (abs > threshold && safeRatio > 1f) {
                val over = abs - threshold
                val compressed = threshold + over / safeRatio
                y = kotlin.math.sign(sample) * compressed
            }
            y *= makeup
            if (limiterEnabled) {
                y = y.coerceIn(-ceiling, ceiling)
            }
            val pcm = (y * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            out.putShort(pcm.toShort())
        }
        out.flip()
    }

    private fun dbToLin(db: Float): Float = 10f.pow(db / 20f)
}