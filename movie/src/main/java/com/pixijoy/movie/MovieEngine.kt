package com.pixijoy.movie

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
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
            still -> {
                val stillResult = encodeStill(primary, cache, talking, rate, onProgress)
                if (stillResult is AppResult.Err) stillResult
                else attachBed(
                    context,
                    cache,
                    paths,
                    timeline,
                    MovieSpeed.outputDurationMs(MovieTrim.stillDurationMs(talking), rate),
                    onProgress
                )
            }
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
            val effects = clipEffects(clip)
            if (effects != null) edited.setEffects(effects)
            edited.build()
        }
        val sequence = EditedMediaItemSequence.Builder(items).build()
        val composition = composeWithBed(sequence, paths, source)
        startTransformer(context, composition, cache, onProgress)
    }

    private suspend fun attachBed(
        context: Context,
        videoFile: File,
        paths: List<String>,
        timeline: Timeline,
        pictureDurationMs: Long,
        onProgress: (Float) -> Unit
    ): AppResult<Unit> {
        val resolved = resolveBed(paths, timeline) ?: return AppResult.Ok(Unit)
        return withContext(Dispatchers.Main) {
            val videoItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(videoFile))).build()
            val videoSeq = EditedMediaItemSequence.Builder(listOf(videoItem)).build()
            val bedSeq = bedSequence(
                resolved.second,
                resolved.first,
                pictureDurationMs.coerceAtLeast(250),
                durationMs(resolved.second.absolutePath)
            )
            val mixed = File(videoFile.parentFile, "mix-${videoFile.name}")
            val result = startTransformer(
                context,
                Composition.Builder(videoSeq, bedSeq).experimentalSetForceAudioTrack(true).build(),
                mixed,
                onProgress
            )
            if (result is AppResult.Ok) {
                videoFile.delete()
                if (!mixed.renameTo(videoFile)) {
                    mixed.copyTo(videoFile, overwrite = true)
                    mixed.delete()
                }
            } else {
                mixed.delete()
            }
            result
        }
    }

    private fun composeWithBed(
        videoSeq: EditedMediaItemSequence,
        paths: List<String>,
        timeline: Timeline
    ): Composition {
        val resolved = resolveBed(paths, timeline) ?: return Composition.Builder(videoSeq).build()
        val bedSeq = bedSequence(
            resolved.second,
            resolved.first,
            timeline.totalMs().coerceAtLeast(250),
            durationMs(resolved.second.absolutePath)
        )
        return Composition.Builder(videoSeq, bedSeq).experimentalSetForceAudioTrack(true).build()
    }

    private fun resolveBed(paths: List<String>, timeline: Timeline): Pair<AudioBed, File>? {
        val bed = timeline.bed ?: return null
        if (bed.muted || bed.fileName.isBlank()) return null
        val dir = File(paths.first()).parentFile ?: return null
        val file = File(dir, bed.fileName)
        return if (file.exists()) bed to file else null
    }

    private fun bedSequence(
        file: File,
        bed: AudioBed,
        totalMs: Long,
        bedDur: Long
    ): EditedMediaItemSequence {
        val slice = bedDur.coerceAtLeast(250)
        val items = ArrayList<EditedMediaItem>()
        var t = 0L
        var index = 0
        while (t < totalMs && index < 64) {
            val take = minOf(slice, totalMs - t).coerceAtLeast(1)
            val first = index == 0
            val last = t + take >= totalMs
            val media = MediaItem.Builder()
                .setUri(Uri.fromFile(file))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(0)
                        .setEndPositionMs(take)
                        .build()
                )
                .build()
            val edited = EditedMediaItem.Builder(media).setRemoveVideo(true)
            val vol = ClipMix.volume(bed.volume)
            val fadeIn = if (first) bed.fadeInMs else 0L
            val fadeOut = if (last) bed.fadeOutMs else 0L
            val processors = ArrayList<AudioProcessor>()
            if (fadeIn > 0 || fadeOut > 0) {
                processors += EnvelopeAudioProcessor(take, vol, fadeIn, fadeOut)
            } else if (vol != 1f) {
                processors += volumeMixer(vol)
            }
            if (processors.isNotEmpty()) edited.setEffects(Effects(processors, emptyList()))
            items += edited.build()
            t += take
            index++
        }
        return EditedMediaItemSequence.Builder(items.ifEmpty {
            listOf(
                EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(file))).setRemoveVideo(true).build()
            )
        }).build()
    }

    private suspend fun startTransformer(
        context: Context,
        composition: Composition,
        cache: File,
        onProgress: (Float) -> Unit
    ): AppResult<Unit> = suspendCancellableCoroutine { cont ->
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

    private fun clipEffects(clip: TimelineClip): Effects? {
        val speed = MovieSpeed.normalize(clip.speed)
        val audio = ArrayList<AudioProcessor>()
        val video = ArrayList<Effect>()
        if (speed != 1f) {
            audio += SonicAudioProcessor().apply {
                setSpeed(speed)
                setPitch(1f)
            }
            video += SpeedChangeEffect(speed)
        }
        if (!clip.muted) {
            val vol = ClipMix.volume(clip.volume)
            if (clip.fadeInMs > 0 || clip.fadeOutMs > 0) {
                audio += EnvelopeAudioProcessor(clip.durationMs, vol, clip.fadeInMs, clip.fadeOutMs)
            } else if (vol != 1f) {
                audio += volumeMixer(vol)
            }
        }
        if (audio.isEmpty() && video.isEmpty()) return null
        return Effects(audio, video)
    }

    private fun volumeMixer(volume: Float): ChannelMixingAudioProcessor {
        val processor = ChannelMixingAudioProcessor()
        for (ch in 1..8) {
            processor.putChannelMixingMatrix(ChannelMixingMatrix.create(ch, ch).scaleBy(volume))
        }
        return processor
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
