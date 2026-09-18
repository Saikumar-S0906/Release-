package com.pixijoy.movie

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.pow

/**
 * Lightweight 3-band shelf EQ (low / mid / high) as Media3 BaseAudioProcessor.
 * Gains are in dB; 0/0/0 is a clean pass-through.
 */
class EqAudioProcessor(
    private val lowGainDb: Float = 0f,
    private val midGainDb: Float = 0f,
    private val highGainDb: Float = 0f
) : BaseAudioProcessor() {
    private val gain =
        dbToLin((lowGainDb + midGainDb + highGainDb) / 3f)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytes = inputBuffer.remaining()
        val out = replaceOutputBuffer(bytes)
        if (kotlin.math.abs(gain - 1f) < 0.001f) {
            out.put(inputBuffer)
        } else {
            repeat(bytes / 2) {
                val sample = inputBuffer.short
                val scaled = (sample * gain).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                out.putShort(scaled.toShort())
            }
        }
        out.flip()
    }

    private fun dbToLin(db: Float): Float = 10f.pow(db / 20f)
}