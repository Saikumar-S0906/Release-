package com.pixijoy.ai.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioVideoMuxer @Inject constructor() {

    suspend fun mux(videoFile: File, audioWav: File?, outputFile: File): File = withContext(Dispatchers.IO) {
        if (audioWav == null || !audioWav.exists()) {
            videoFile.copyTo(outputFile, overwrite = true)
            return@withContext outputFile
        }
        val aacFile = File(videoFile.parentFile, "audio_${System.currentTimeMillis()}.aac")
        try {
            encodeWavToAac(audioWav, aacFile)
            combineTracks(videoFile, aacFile, outputFile)
        } catch (e: Exception) {
            // Soft-fail: ship video without muxed audio rather than crashing the export path.
            videoFile.copyTo(outputFile, overwrite = true)
        } finally {
            aacFile.delete()
        }
        outputFile
    }

    private fun encodeWavToAac(wavFile: File, aacFile: File) {
        val wav = WavReader.read(wavFile)
        require(wav.sampleRate > 0) { "Invalid WAV sample rate" }
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, wav.sampleRate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val muxer = MediaMuxer(aacFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            var trackIndex = -1
            val bufferInfo = MediaCodec.BufferInfo()
            var offset = 0
            var inputDone = false
            var outputDone = false
            val frameSize = 4_096

            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = encoder.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputIndex)
                            ?: throw IllegalStateException("AAC encoder input buffer null")
                        inputBuffer.clear()
                        val remaining = wav.pcm.size - offset
                        if (remaining > 0) {
                            val chunk = remaining.coerceAtMost(frameSize)
                            val shortBuffer = inputBuffer.asShortBuffer()
                            for (i in 0 until chunk / 2) {
                                val lo = wav.pcm[offset + i * 2].toInt() and 0xFF
                                val hi = wav.pcm[offset + i * 2 + 1].toInt() shl 8
                                shortBuffer.put((lo or hi).toShort())
                            }
                            encoder.queueInputBuffer(inputIndex, 0, chunk, computePts(offset, wav.sampleRate), 0)
                            offset += chunk
                        } else {
                            encoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                computePts(offset, wav.sampleRate),
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        }
                    }
                }
                val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        trackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outputIndex >= 0 -> {
                        val encoded = encoder.getOutputBuffer(outputIndex)
                            ?: throw IllegalStateException("AAC encoder output buffer null")
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) bufferInfo.size = 0
                        if (bufferInfo.size > 0 && muxerStarted) {
                            encoded.position(bufferInfo.offset)
                            encoded.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, encoded, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(outputIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }
            }
        } finally {
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            if (muxerStarted) runCatching { muxer.stop() }
            runCatching { muxer.release() }
        }
    }

    private fun combineTracks(videoFile: File, audioFile: File, outputFile: File) {
        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        try {
            videoExtractor.setDataSource(videoFile.absolutePath)
            audioExtractor.setDataSource(audioFile.absolutePath)
            val videoTrack = selectTrack(videoExtractor, "video/")
            val audioTrack = selectTrack(audioExtractor, "audio/")
            videoExtractor.selectTrack(videoTrack)
            audioExtractor.selectTrack(audioTrack)
            val outVideo = muxer.addTrack(videoExtractor.getTrackFormat(videoTrack))
            val outAudio = muxer.addTrack(audioExtractor.getTrackFormat(audioTrack))
            muxer.start()
            val buffer = ByteBuffer.allocate(1_048_576)
            val info = MediaCodec.BufferInfo()
            copySamples(videoExtractor, muxer, outVideo, buffer, info)
            copySamples(audioExtractor, muxer, outAudio, buffer, info)
            muxer.stop()
        } finally {
            runCatching { muxer.release() }
            runCatching { videoExtractor.release() }
            runCatching { audioExtractor.release() }
        }
    }

    private fun selectTrack(extractor: MediaExtractor, prefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith(prefix)) return i
        }
        throw IllegalStateException("No track for $prefix")
    }

    private fun copySamples(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        trackIndex: Int,
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo
    ) {
        while (true) {
            info.offset = 0
            info.size = extractor.readSampleData(buffer, 0)
            if (info.size < 0) break
            info.presentationTimeUs = extractor.sampleTime
            info.flags = extractor.sampleFlags
            muxer.writeSampleData(trackIndex, buffer, info)
            extractor.advance()
        }
    }

    private fun computePts(byteOffset: Int, sampleRate: Int): Long {
        if (sampleRate <= 0) return 0L
        return (byteOffset / 2L) * 1_000_000L / sampleRate
    }

    private data class WavData(val pcm: ByteArray, val sampleRate: Int)

    private object WavReader {
        fun read(file: File): WavData {
            RandomAccessFile(file, "r").use { raf ->
                raf.skipBytes(22)
                val sampleRate = Integer.reverseBytes(raf.readInt())
                raf.skipBytes(6)
                val dataSize = (raf.length() - 44).coerceAtLeast(0)
                val pcm = ByteArray(dataSize.toInt())
                raf.seek(44)
                raf.readFully(pcm)
                return WavData(pcm, sampleRate)
            }
        }
    }
}
