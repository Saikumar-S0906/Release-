package com.pixijoy.movie

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.pixijoy.core.common.AppResult
import com.pixijoy.core.media.MediaProbe
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

interface MovieEngine {
    fun isStill(path: String): Boolean
    fun durationMs(path: String): Long
    suspend fun export(
        context: Context,
        paths: List<String>,
        timeline: Timeline,
        talking: Boolean,
        gif: Boolean,
        onProgress: (Float) -> Unit
    ): AppResult<String>
}

class Media3MovieEngine : MovieEngine {
    override fun isStill(path: String): Boolean {
        val info = MediaProbe.probe(File(path))
        return info.durationMs == null || info.durationMs == 0L
    }

    override fun durationMs(path: String): Long {
        val info = MediaProbe.probe(File(path))
        return info.durationMs?.takeIf { it > 0 } ?: MovieTrim.stillDurationMs(false)
    }

    override suspend fun export(
        context: Context,
        paths: List<String>,
        timeline: Timeline,
        talking: Boolean,
        gif: Boolean,
        onProgress: (Float) -> Unit
    ): AppResult<String> {
        val primary = paths.firstOrNull() ?: return AppResult.Err("No clip to export.")
        val still = isStill(primary)
        val rate = timeline.clips.firstOrNull()?.speed ?: 1f
        val cacheName = if (gif) "pixijoy-export-${System.currentTimeMillis()}.gif"
        else "pixijoy-export-${System.currentTimeMillis()}.mp4"
        val cache = File(context.cacheDir, cacheName)
        val encoded = when {
            gif -> encodeGif(primary, cache, talking, rate, onProgress)
            still -> encodeStill(primary, cache, talking, rate, onProgress)
            else -> encodeClips(context, paths, timeline, cache, onProgress)
        }
        if (encoded is AppResult.Err) {
            cache.delete()
            return encoded
        }
        val published = if (gif) {
            publish(
                context,
                cache,
                "PixiJoy_${System.currentTimeMillis()}.gif",
                mime = "image/gif",
                pictures = true
            )
        } else {
            publish(
                context,
                cache,
                "PixiJoy_${System.currentTimeMillis()}.mp4",
                mime = "video/mp4",
                pictures = false
            )
        }
        cache.delete()
        return published
    }

    private suspend fun encodeGif(
        path: String,
        cache: File,
        talking: Boolean,
        speed: Float,
        onProgress: (Float) -> Unit
    ): AppResult<Unit> = withContext(Dispatchers.Default) {
        val bitmap = BitmapFactory.decodeFile(path)
            ?: return@withContext AppResult.Err("Could not read the still.")
        try {
            val duration = MovieSpeed.outputDurationMs(MovieTrim.stillDurationMs(talking), speed)
            GifEncoder().encode(bitmap, cache, duration, talking, onProgress)
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private suspend fun encodeStill(
        path: String,
        cache: File,
        talking: Boolean,
        speed: Float,
        onProgress: (Float) -> Unit
    ): AppResult<Unit> = withContext(Dispatchers.Default) {
        val bitmap = BitmapFactory.decodeFile(path)
            ?: return@withContext AppResult.Err("Could not read the still.")
        try {
            val duration = MovieSpeed.outputDurationMs(MovieTrim.stillDurationMs(talking), speed)
            KenBurnsEncoder().encode(bitmap, cache, duration, talking, onProgress)
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private suspend fun encodeClips(
        context: Context,
        paths: List<String>,
        timeline: Timeline,
        cache: File,
        onProgress: (Float) -> Unit
    ): AppResult<Unit> = withContext(Dispatchers.Main) {
        val source = if (timeline.clips.isEmpty()) {
            Timeline.fromMedia(paths.size) { durationMs(paths[it]) }
        } else timeline
        val items = source.clips.map { clip ->
            val path = paths.getOrElse(clip.mediaIndex) { paths.first() }
            val duration = durationMs(path)
            val (start, end) = MovieTrim.normalize(clip.inMs, clip.outMs, duration)
            val builder = MediaItem.Builder()
                .setUri(Uri.fromFile(File(path)))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(start)
                        .setEndPositionMs(end)
                        .build()
                )
            val edited = EditedMediaItem.Builder(builder.build())
            if (clip.muted) edited.setRemoveAudio(true)
            val speed = MovieSpeed.normalize(clip.speed)
            if (speed != 1f) edited.setEffects(speedEffects(speed))
            edited.build()
        }
        val sequence = EditedMediaItemSequence.Builder(items).build()
        val composition = Composition.Builder(sequence).build()
        suspendCancellableCoroutine { cont ->
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .build()
            transformer.addListener(
                object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, result: ExportResult) {
                        onProgress(1f)
                        if (cont.isActive) cont.resume(AppResult.Ok(Unit))
                    }

                    override fun onError(
                        composition: Composition,
                        result: ExportResult,
                        exception: ExportException
                    ) {
                        if (cont.isActive) {
                            cont.resume(
                                AppResult.Err(exception.localizedMessage ?: "Movie export failed.", exception)
                            )
                        }
                    }
                }
            )
            transformer.start(composition, cache.absolutePath)
            cont.invokeOnCancellation { transformer.cancel() }
        }
    }

    private fun speedEffects(speed: Float): Effects {
        val audio = SonicAudioProcessor().apply {
            setSpeed(speed)
            setPitch(1f)
        }
        return Effects(listOf(audio), listOf(SpeedChangeEffect(speed)))
    }

    private fun publish(
        context: Context,
        file: File,
        displayName: String,
        mime: String,
        pictures: Boolean
    ): AppResult<String> {
        return try {
            val collection = if (pictures) {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            val folder = if (pictures) {
                Environment.DIRECTORY_PICTURES + "/PixiJoy"
            } else {
                Environment.DIRECTORY_MOVIES + "/PixiJoy"
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(collection, values)
                ?: return AppResult.Err("Could not create an export file.")
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } ?: return AppResult.Err("Could not open export stream.")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            AppResult.Ok(uri.toString())
        } catch (t: Throwable) {
            AppResult.Err("Could not save the movie.", t)
        }
    }
}
