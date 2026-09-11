package com.pixijoy.ai.creative

import android.content.Context
import android.graphics.Bitmap
import com.pixijoy.ai.domain.model.MusicMood
import com.pixijoy.ai.media.AudioVideoMuxer
import com.pixijoy.ai.media.BackgroundMusicGenerator
import com.pixijoy.ai.media.TextToSpeechService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReelExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val slideshowVideoEncoder: SlideshowVideoEncoder,
    private val backgroundMusicGenerator: BackgroundMusicGenerator,
    private val textToSpeechService: TextToSpeechService,
    private val audioVideoMuxer: AudioVideoMuxer
) {
    suspend fun export(
        frames: List<Bitmap>,
        outputFile: File,
        narration: String? = null,
        musicMood: MusicMood? = null,
        frameDurationUs: Long = 350_000L
    ): File = withContext(Dispatchers.IO) {
        val silentVideo = File(context.cacheDir, "silent_${System.currentTimeMillis()}.mp4")
        slideshowVideoEncoder.encode(frames, silentVideo, frameDurationUs = frameDurationUs)
        val musicFile = if (musicMood != null) {
            File(context.cacheDir, "music_${System.currentTimeMillis()}.wav").also {
                backgroundMusicGenerator.generate(musicMood, durationSec = frames.size * frameDurationUs / 1_000_000f + 1f, it)
            }
        } else null
        val voiceFile = narration?.let {
            runCatching { textToSpeechService.synthesizeToFile(it, Locale.US) }.getOrNull()
        }
        val mixedAudio = mixAudioFiles(voiceFile, musicFile)
        audioVideoMuxer.mux(silentVideo, mixedAudio, outputFile)
        silentVideo.delete()
        voiceFile?.delete()
        musicFile?.delete()
        mixedAudio?.delete()
        outputFile
    }

    private fun mixAudioFiles(voice: File?, music: File?): File? {
        if (voice == null) return music
        if (music == null) return voice
        return voice
    }
}
