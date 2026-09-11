package com.pixijoy.ai.media

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.pixijoy.ai.domain.model.MemeSound
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sin

@Singleton
class MemeSoundEffects @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicGenerator: BackgroundMusicGenerator
) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    suspend fun exportSound(sound: MemeSound): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "meme_sound_${sound.name.lowercase()}.wav")
        val duration = when (sound) {
            MemeSound.BRUH -> 0.4f
            MemeSound.VINE_BOOM -> 0.6f
            MemeSound.LAUGH -> 0.8f
            MemeSound.WOW -> 0.5f
            MemeSound.SAD -> 1.0f
        }
        musicGenerator.generateMemeTone(sound, duration, file)
        file
    }

    fun playPreview(sound: MemeSound) {
        // Preview uses a short generated tone burst via temp file would be async;
        // SoundPool requires loaded assets — export is primary path for meme attach.
    }
}
