package com.pixijoy.movie

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

internal class EnvelopeAudioProcessor(
    private val durationMs: Long,
    private val volume: Float,
    private val fadeInMs: Long,
    private val fadeOutMs: Long,
    private val offsetMs: Long = 0,
    private val windows: List<ClipMix.Window> = emptyList(),
    private val duck: Float = 1f,
    private val attackMs: Long = 120,
    private val releaseMs: Long = 250
) : BaseAudioProcessor() {
    private var framesRead = 0L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun onFlush() {
        framesRead = 0L
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val channels = inputAudioFormat.channelCount
        val sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(1)
        val bytesPerFrame = inputAudioFormat.bytesPerFrame.coerceAtLeast(1)
        val frames = inputBuffer.remaining() / bytesPerFrame
        val out = replaceOutputBuffer(frames * bytesPerFrame)
        repeat(frames) {
            val tMs = offsetMs + framesRead * 1000L / sampleRate
            val g = ClipMix.gain(tMs, durationMs, volume, fadeInMs, fadeOutMs) *
                ClipMix.duck(tMs, windows, duck, attackMs, releaseMs)
            repeat(channels) {
                val sample = inputBuffer.short
                val scaled = (sample * g).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                out.putShort(scaled.toShort())
            }
            framesRead++
        }
        out.flip()
    }
}
