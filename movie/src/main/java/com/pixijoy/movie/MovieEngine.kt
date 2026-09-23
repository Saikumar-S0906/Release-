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
import androidx.media3.effect.Presentation
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.pixijoy.core.common.AppResult
import com.pixijoy.core.media.MediaProbe
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

interface MovieEngine {
    fun isStill(path: String): Boolean
    fun durationMs(path: String): Long
    fun fps(path: String): Float
    fun composition(paths: List<String>, timeline: Timeline): Composition
    fun composition(paths: List<String>, timeline: MultiTrackTimeline): Composition
    suspend fun export(
        context: Context,
        paths: List<String>,
        timeline: Timeline,
        talking: Boolean,
        gif: Boolean,
        onProgress: (Float) -> Unit
    ): AppResult<String>
    suspend fun export(
        context: Context,
        paths: List<String>,
        timeline: MultiTrackTimeline,
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

    override fun fps(path: String): Float = MovieFrame.fps(MediaProbe.probe(File(path)).fps)

    override fun composition(paths: List<String>, timeline: Timeline): Composition =
        composition(paths, MultiTrackTimeline.fromFlatTimeline(timeline))

    override fun composition(paths: List<String>, timeline: MultiTrackTimeline): Composition {
        val mt = timeline.ensureDefaultTracks()
        val clips = mt.composedVideoClips().ifEmpty {
            Timeline.fromMedia(paths.size) { durationMs(paths[it]) }.clips
        }
        val items = clips.map { clip -> videoItem(paths, clip) }
        val videoSeq = EditedMediaItemSequence.Builder(items).build()
        val audioTrackSeqs = mt.unmutedAudioTracksWithClips().map { track ->
            audioClipSequence(paths, track.clips)
        }
        return composeAudio(
            videoSeq = videoSeq,
            paths = paths,
            timeline = mt.toFlatTimeline().copy(clips = clips),
            durationOverrideMs = null,
            extraAudioSequences = audioTrackSeqs
        )
    }

    override suspend fun export(
        context: Context,
        paths: List<String>,
        timeline: Timeline,
        talking: Boolean,
        gif: Boolean,
        onProgress: (Float) -> Unit
    ): AppResult<String> = export(
        context,
        paths,
        MultiTrackTimeline.fromFlatTimeline(timeline),
        talking,
        gif,
        onProgress
    )

    override suspend fun export(
        context: Context,
        paths: List<String>,
        timeline: MultiTrackTimeline,
        talking: Boolean,
        gif: Boolean,
        onProgress: (Float) -> Unit
    ): AppResult<String> {
        val primary = paths.firstOrNull() ?: return AppResult.Err("No clip to export.")
        val still = isStill(primary)
        val flat = timeline.toFlatTimeline()
        val rate = flat.clips.firstOrNull()?.speed ?: 1f
        val cacheName = if (gif) "pixijoy-export-${System.currentTimeMillis()}.gif"
        else "pixijoy-export-${System.currentTimeMillis()}.mp4"
        val cache = File(context.cacheDir, cacheName)
        val encoded = when {
            gif -> encodeGif(primary, cache, talking, rate, onProgress)
            still -> {
                val stillResult = encodeStill(primary, cache, talking, rate, flat.outputHeight, onProgress)
                if (stillResult is AppResult.Err) stillResult
                else attachBed(
                    context,
                    cache,
                    paths,
                    flat,
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
        height: Int,
        onProgress: (Float) -> Unit
    ): AppResult<Unit> = withContext(Dispatchers.Default) {
        val bitmap = BitmapFactory.decodeFile(path)
            ?: return@withContext AppResult.Err("Could not read the still.")
        try {
            val duration = MovieSpeed.outputDurationMs(MovieTrim.stillDurationMs(talking), speed)
            KenBurnsEncoder().encode(bitmap, cache, duration, talking, height, onProgress)
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private suspend fun encodeClips(
        context: Context,
        paths: List<String>,
        timeline: MultiTrackTimeline,
        cache: File,
        onProgress: (Float) -> Unit
    ): AppResult<Unit> = withContext(Dispatchers.Main) {
        startTransformer(context, composition(paths, timeline), cache, onProgress)
    }

    private fun videoItem(paths: List<String>, clip: TimelineClip): EditedMediaItem {
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
            .setDurationUs(clip.durationMs.coerceAtLeast(1) * 1000)
        if (clip.muted) edited.setRemoveAudio(true)
        val effects = clipEffects(clip)
        if (effects != null) edited.setEffects(effects)
        return edited.build()
    }

    /** Audio-only sequence from timeline clips on an audio track (D1 multi-track). */
    private fun audioClipSequence(paths: List<String>, clips: List<TimelineClip>): EditedMediaItemSequence {
        val items = clips.map { clip ->
            val path = paths.getOrElse(clip.mediaIndex) { paths.first() }
            val duration = durationMs(path)
            val (start, end) = MovieTrim.normalize(clip.inMs, clip.outMs, duration)
            val media = MediaItem.Builder()
                .setUri(Uri.fromFile(File(path)))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(start)
                        .setEndPositionMs(end)
                        .build()
                )
                .build()
            val edited = EditedMediaItem.Builder(media)
                .setRemoveVideo(true)
                .setDurationUs(clip.durationMs.coerceAtLeast(1) * 1000)
            val effects = clipEffects(clip.copy(muted = false))
            if (effects != null) edited.setEffects(effects)
            edited.build()
        }
        return EditedMediaItemSequence.Builder(items).build()
    }

    private suspend fun attachBed(
        context: Context,
        videoFile: File,
        paths: List<String>,
        timeline: Timeline,
        pictureDurationMs: Long,
        onProgress: (Float) -> Unit
    ): AppResult<Unit> {
        if (resolveStem(paths, timeline.bed) == null && resolveStem(paths, timeline.voice) == null) {
            return AppResult.Ok(Unit)
        }
        return withContext(Dispatchers.Main) {
            val videoItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(videoFile))).build()
            val videoSeq = EditedMediaItemSequence.Builder(listOf(videoItem)).build()
            val mixed = File(videoFile.parentFile, "mix-${videoFile.name}")
            val result = startTransformer(
                context,
                composeAudio(videoSeq, paths, timeline, pictureDurationMs.coerceAtLeast(250)),
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

    private fun composeAudio(
        videoSeq: EditedMediaItemSequence,
        paths: List<String>,
        timeline: Timeline,
        durationOverrideMs: Long? = null,
        extraAudioSequences: List<EditedMediaItemSequence> = emptyList()
    ): Composition {
        val total = (durationOverrideMs ?: timeline.totalMs()).coerceAtLeast(250)
        val sequences = ArrayList<EditedMediaItemSequence>()
        sequences += videoSeq
        sequences += extraAudioSequences
        val voice = resolveStem(paths, timeline.voice)
        val voiceEnd = voice?.let { minOf(durationMs(it.second.absolutePath), total) } ?: 0L
        val windows = timeline.duckWindows(voiceEnd)
        resolveStem(paths, timeline.bed)?.let { (bed, file) ->
            sequences += bedSequence(file, bed, total, durationMs(file.absolutePath), windows)
        }
        voice?.let { (stem, file) ->
            sequences += voiceSequence(file, stem, total, durationMs(file.absolutePath))
        }
        for (stem in timeline.extraStems) {
            if (stem.muted || stem.fileName.isBlank()) continue
            val dir = File(paths.first()).parentFile ?: continue
            val file = File(dir, stem.fileName)
            if (!file.exists()) continue
            val asBed = AudioBed(
                fileName = stem.fileName,
                volume = stem.volume,
                fadeInMs = stem.fadeInMs,
                fadeOutMs = stem.fadeOutMs,
                muted = false,
                duck = 0.25f,
                attackMs = 120
            )
            val dur = durationMs(file.absolutePath)
            if (stem.role.equals("music", ignoreCase = true)) {
                sequences += bedSequence(file, asBed, total, dur, windows)
            } else {
                sequences += voiceSequence(file, asBed, total, dur)
            }
        }
        val height = MovieExportSize.normalize(timeline.outputHeight)
        val videoEffects = listOf<Effect>(Presentation.createForHeight(height))
        val audioProcessors = ArrayList<AudioProcessor>()
        val fx = timeline.masterFx
        if (!fx.isNeutral()) {
            audioProcessors += EqAudioProcessor(fx.lowGainDb, fx.midGainDb, fx.highGainDb)
            audioProcessors += DynamicsAudioProcessor(
                thresholdDb = fx.compThresholdDb,
                ratio = fx.compRatio,
                attackMs = fx.compAttackMs,
                releaseMs = fx.compReleaseMs,
                makeupDb = fx.compMakeupDb,
                limiterCeilingDb = fx.limiterCeilingDb,
                limiterEnabled = fx.limiterEnabled
            )
        }
        val compositionEffects = Effects(audioProcessors, videoEffects)
        return if (sequences.size == 1) {
            Composition.Builder(videoSeq).setEffects(compositionEffects).build()
        } else {
            Composition.Builder(sequences)
                .experimentalSetForceAudioTrack(true)
                .setEffects(compositionEffects)
                .build()
        }
    }

    private fun resolveStem(paths: List<String>, stem: AudioBed?): Pair<AudioBed, File>? {
        if (stem == null || stem.muted || stem.fileName.isBlank()) return null
        val dir = File(paths.first()).parentFile ?: return null
        val file = File(dir, stem.fileName)
        return if (file.exists()) stem to file else null
    }

    private fun voiceSequence(
        file: File,
        stem: AudioBed,
        totalMs: Long,
        sourceMs: Long
    ): EditedMediaItemSequence {
        val take = minOf(sourceMs.coerceAtLeast(1), totalMs)
        val media = MediaItem.Builder()
            .setUri(Uri.fromFile(file))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(0)
                    .setEndPositionMs(take)
                    .build()
            )
            .build()
        val edited = EditedMediaItem.Builder(media)
            .setRemoveVideo(true)
            .setDurationUs(take * 1000)
        val vol = ClipMix.volume(stem.volume)
        if (vol != 1f || stem.fadeInMs > 0 || stem.fadeOutMs > 0) {
            edited.setEffects(
                Effects(
                    listOf(
                        EnvelopeAudioProcessor(
                            durationMs = take,
                            volume = vol,
                            fadeInMs = stem.fadeInMs,
                            fadeOutMs = stem.fadeOutMs
                        )
                    ),
                    emptyList()
                )
            )
        }
        return EditedMediaItemSequence.Builder(listOf(edited.build())).build()
    }

    private fun bedSequence(
        file: File,
        bed: AudioBed,
        totalMs: Long,
        bedDur: Long,
        windows: List<ClipMix.Window>
    ): EditedMediaItemSequence {
        val slice = bedDur.coerceAtLeast(250)
        val items = ArrayList<EditedMediaItem>()
        var t = 0L
        var index = 0
        val vol = ClipMix.volume(bed.volume)
        val duck = ClipMix.volume(bed.duck)
        while (t < totalMs && index < 64) {
            val take = minOf(slice, totalMs - t).coerceAtLeast(1)
            val media = MediaItem.Builder()
                .setUri(Uri.fromFile(file))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(0)
                        .setEndPositionMs(take)
                        .build()
                )
                .build()
            val edited = EditedMediaItem.Builder(media)
                .setRemoveVideo(true)
                .setDurationUs(take * 1000)
            val needsMix = vol != 1f || bed.fadeInMs > 0 || bed.fadeOutMs > 0 ||
                (duck < 0.999f && windows.isNotEmpty())
            if (needsMix) {
                edited.setEffects(
                    Effects(
                        listOf(
                            EnvelopeAudioProcessor(
                                durationMs = totalMs,
                                volume = vol,
                                fadeInMs = bed.fadeInMs,
                                fadeOutMs = bed.fadeOutMs,
                                offsetMs = t,
                                windows = windows,
                                duck = duck,
                                attackMs = bed.attackMs
                            )
                        ),
                        emptyList()
                    )
                )
            }
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
    ): AppResult<Unit> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .build()
            val holder = ProgressHolder()
            val poll = launch {
                while (isActive) {
                    if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgress((holder.progress / 100f).coerceIn(0f, 1f))
                    }
                    delay(250)
                }
            }
            transformer.addListener(
                object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, result: ExportResult) {
                        poll.cancel()
                        onProgress(1f)
                        if (cont.isActive) cont.resume(AppResult.Ok(Unit))
                    }

                    override fun onError(
                        composition: Composition,
                        result: ExportResult,
                        exception: ExportException
                    ) {
                        poll.cancel()
                        if (cont.isActive) {
                            cont.resume(
                                AppResult.Err(exception.localizedMessage ?: "Movie export failed.", exception)
                            )
                        }
                    }
                }
            )
            transformer.start(composition, cache.absolutePath)
            cont.invokeOnCancellation {
                poll.cancel()
                transformer.cancel()
            }
        }
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
        val resolver = context.contentResolver
        var pendingUri: android.net.Uri? = null
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
            val uri = resolver.insert(collection, values)
                ?: return AppResult.Err("Could not create an export file.")
            pendingUri = uri
            val out = resolver.openOutputStream(uri)
            if (out == null) {
                resolver.delete(uri, null, null)
                pendingUri = null
                return AppResult.Err("Could not open export stream.")
            }
            out.use { stream ->
                file.inputStream().use { it.copyTo(stream) }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            pendingUri = null
            AppResult.Ok(uri.toString())
        } catch (t: Throwable) {
            AppResult.Err("Could not save the movie.", t)
        } finally {
            pendingUri?.let { orphan ->
                try {
                    resolver.delete(orphan, null, null)
                } catch (_: Exception) {
                }
            }
        }
    }
}
