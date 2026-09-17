package com.pixijoy.movie

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
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
        path: String,
        trimStartMs: Long,
        trimEndMs: Long,
        talking: Boolean,
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
        path: String,
        trimStartMs: Long,
        trimEndMs: Long,
        talking: Boolean,
        onProgress: (Float) -> Unit
    ): AppResult<String> {
        val still = isStill(path)
        val cache = File(context.cacheDir, "pixijoy-export-${System.currentTimeMillis()}.mp4")
        val encoded = if (still) {
            encodeStill(path, cache, talking, onProgress)
        } else {
            encodeClip(context, path, trimStartMs, trimEndMs, cache, onProgress)
        }
        if (encoded is AppResult.Err) {
            cache.delete()
            return encoded
        }
        return publish(context, cache, "PixiJoy_${System.currentTimeMillis()}.mp4").also {
            cache.delete()
        }
    }

    private suspend fun encodeStill(
        path: String,
        cache: File,
        talking: Boolean,
        onProgress: (Float) -> Unit
    ): AppResult<Unit> = withContext(Dispatchers.Default) {
        val bitmap = BitmapFactory.decodeFile(path)
            ?: return@withContext AppResult.Err("Could not read the still.")
        try {
            KenBurnsEncoder().encode(
                source = bitmap,
                output = cache,
                durationMs = MovieTrim.stillDurationMs(talking),
                talking = talking,
                onProgress = onProgress
            )
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private suspend fun encodeClip(
        context: Context,
        path: String,
        trimStartMs: Long,
        trimEndMs: Long,
        cache: File,
        onProgress: (Float) -> Unit
    ): AppResult<Unit> = withContext(Dispatchers.Main) {
        val duration = durationMs(path)
        val (start, end) = MovieTrim.normalize(trimStartMs, trimEndMs, duration)
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(File(path)))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(start)
                    .setEndPositionMs(end)
                    .build()
            )
            .build()
        val edited = EditedMediaItem.Builder(mediaItem).build()
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
                            cont.resume(AppResult.Err(exception.localizedMessage ?: "Trim export failed.", exception))
                        }
                    }
                }
            )
            transformer.start(edited, cache.absolutePath)
            cont.invokeOnCancellation { transformer.cancel() }
        }
    }

    private fun publish(context: Context, file: File, displayName: String): AppResult<String> {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/PixiJoy")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return AppResult.Err("Could not create a Movies file.")
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } ?: return AppResult.Err("Could not open export stream.")
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            AppResult.Ok(uri.toString())
        } catch (t: Throwable) {
            AppResult.Err("Could not save the movie.", t)
        }
    }
}
