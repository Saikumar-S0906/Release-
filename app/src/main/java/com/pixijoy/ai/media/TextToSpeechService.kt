package com.pixijoy.ai.media

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class TextToSpeechService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null

    suspend fun synthesizeToFile(text: String, locale: Locale = Locale.US): File = withContext(Dispatchers.IO) {
        val engine = ensureTts()
        engine.language = locale
        val file = File(context.cacheDir, "tts_${System.currentTimeMillis()}.wav")
        suspendCancellableCoroutine { cont ->
            val done = AtomicBoolean(false)
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) {
                    if (done.compareAndSet(false, true)) cont.resume(file)
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (done.compareAndSet(false, true)) cont.resumeWithException(IllegalStateException("TTS failed"))
                }
                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (done.compareAndSet(false, true)) cont.resumeWithException(IllegalStateException("TTS error $errorCode"))
                }
            })
            val result = engine.synthesizeToFile(text, Bundle(), file, "pixijoy_tts")
            if (result != TextToSpeech.SUCCESS && done.compareAndSet(false, true)) {
                cont.resumeWithException(IllegalStateException("TTS synthesize failed"))
            }
        }
    }

    private suspend fun ensureTts(): TextToSpeech = withContext(Dispatchers.Main) {
        tts?.let { return@withContext it }
        suspendCancellableCoroutine { cont ->
            lateinit var engine: TextToSpeech
            engine = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts = engine
                    cont.resume(engine)
                } else {
                    cont.resumeWithException(IllegalStateException("TTS init failed"))
                }
            }
        }
    }
}
