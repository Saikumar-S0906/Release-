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
import androidx.media3.common.VideoCompositorSettings
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
import com.pixijoy.ml.PackEngineHooks
import com.pixijoy.ml.pack.PackId
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
        val prepared = mt.toFlatTimeline()
            .copy(clips = mt.composedVideoClips().ifEmpty {
                Timeline.fromMedia(paths.size) { durationMs(paths[it]) }.clips
            })
            .withTransitionFadesApplied()
        val clips = MovieSpeed.expandTimeline(prepared.clips)
        val flat = prepared.copy(clips = clips)
        val audioTrackSeqs = mt.unmutedAudioTracksWithClips().map { track ->
            audioClipSequence(paths, MovieSpeed.expandTimeline(track.clips))
        }
        val layout = flat.layout
        if (layout.isMulti()) {
            val inputPaths = VideoLayoutPlan.inputPaths(paths, layout)
            val totalMs = flat.totalMs().coerceAtLeast(250)
            val videoSeqs = ArrayList<EditedMediaItemSequence>(inputPaths.size)
            videoSeqs += EditedMediaItemSequence.Builder(clips.map { videoItem(paths, it) }).build()
            for (i in 1 until inputPaths.size) {
                videoSeqs += parallelVideoSequence(inputPaths[i], totalMs, removeAudio = true)
            }
            return composeAudio(
                videoSequences = videoSeqs,
                paths = paths,
                timeline = flat,
                durationOverrideMs = totalMs,
                extraAudioSequences = audioTrackSeqs,
                compositor = layout.compositor(),
                burnInTc = mt.delivery.burnInTc
            )
        }
        val videoSeq = EditedMediaItemSequence.Builder(clips.map { videoItem(paths, it) }).build()
        return composeAudio(
            videoSequences = listOf(videoSeq),
            paths = paths,
            timeline = flat,
            durationOverrideMs = null,
            extraAudioSequences = audioTrackSeqs,
            burnInTc = mt.delivery.burnInTc
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
                if (timeline.titles.isNotEmpty() || !timeline.grade.isNeutral() || timeline.layout.isMulti()) {
                    encodeClips(context, paths, timeline, cache, onProgress)
                } else {
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
    ): AppResult<Unit> {
        val prepared = prepareNeuralSlowMo(context, paths, timeline) { p ->
            onProgress((p * 0.35f).coerceIn(0f, 0.35f))
        }
        return try {
            withContext(Dispatchers.Main) {
                startTransformer(
                    context,
                    composition(prepared.paths, prepared.timeline),
                    cache
                ) { p -> onProgress(0.35f + p * 0.65f) }
            }
        } finally {
            prepared.temps.forEach { it.delete() }
        }
    }

    /**
     * When a clip requests [SlowMoEngine.Ml] and FILM is bound, synthesize stretched
     * mid-frames and clear slow-mo so Transformer does not also frame-hold. Failure
     * leaves the clip unchanged (heuristic Media3 rate path).
     */
    private suspend fun prepareNeuralSlowMo(
        context: Context,
        paths: List<String>,
        timeline: MultiTrackTimeline,
        onProgress: (Float) -> Unit
    ): NeuralExportPrep = withContext(Dispatchers.Default) {
        if (!FrameInterp.mlAvailable()) {
            return@withContext NeuralExportPrep(paths, timeline, emptyList())
        }
        val mt = timeline.ensureDefaultTracks()
        val neuralClips = mt.composedVideoClips().count {
            it.slowMo.resolvedEngine() == SlowMoEngine.Ml
        }
        if (neuralClips == 0) {
            return@withContext NeuralExportPrep(paths, timeline, emptyList())
        }
        val pathList = paths.toMutableList()
        val temps = ArrayList<File>()
        var done = 0
        val nextTracks = mt.tracks.map { track ->
            if (track.kind != TrackKind.Video) return@map track
            track.copy(
                clips = track.clips.map { clip ->
                    if (clip.slowMo.resolvedEngine() != SlowMoEngine.Ml) return@map clip
                    val source = paths.getOrElse(clip.mediaIndex) { paths.first() }
                    val duration = durationMs(source)
                    val (start, end) = MovieTrim.normalize(clip.inMs, clip.outMs, duration)
                    val rate = MovieSpeed.normalizeSlowMo(clip.slowMo.rate)
                    val outMs = MovieSpeed.outputDurationMs((end - start).coerceAtLeast(1), rate)
                    val silent = File(
                        context.cacheDir,
                        "neural-silent-${System.currentTimeMillis()}-${clip.id}.mp4"
                    )
                    val mixed = File(
                        context.cacheDir,
                        "neural-mix-${System.currentTimeMillis()}-${clip.id}.mp4"
                    )
                    val synth = NeuralSlowMoMux.synthesize(
                        path = source,
                        startMs = start,
                        endMs = end,
                        rate = rate,
                        output = silent,
                        height = mt.outputHeight
                    ) { p ->
                        onProgress((done + p) / neuralClips.toFloat())
                    }
                    if (synth !is AppResult.Ok) {
                        silent.delete()
                        done++
                        onProgress(done / neuralClips.toFloat())
                        return@map clip
                    }
                    temps += silent
                    val finalFile = if (clip.muted) {
                        silent
                    } else {
                        val remuxed = remuxNeuralAudio(
                            context = context,
                            silentVideo = silent,
                            sourcePath = source,
                            startMs = start,
                            endMs = end,
                            rate = rate,
                            outputDurationMs = outMs,
                            output = mixed
                        )
                        if (remuxed is AppResult.Ok) {
                            silent.delete()
                            temps.remove(silent)
                            temps += mixed
                            mixed
                        } else {
                            mixed.delete()
                            silent
                        }
                    }
                    val mediaIndex = pathList.size
                    pathList += finalFile.absolutePath
                    done++
                    onProgress(done / neuralClips.toFloat())
                    clip.copy(
                        mediaIndex = mediaIndex,
                        inMs = 0L,
                        outMs = outMs,
                        speed = 1f,
                        speedEnd = 1f,
                        slowMo = ClipSlowMo.None,
                        muted = clip.muted
                    )
                }
            )
        }
        NeuralExportPrep(pathList, mt.copy(tracks = nextTracks), temps)
    }

    private suspend fun remuxNeuralAudio(
        context: Context,
        silentVideo: File,
        sourcePath: String,
        startMs: Long,
        endMs: Long,
        rate: Float,
        outputDurationMs: Long,
        output: File
    ): AppResult<Unit> = withContext(Dispatchers.Main) {
        val videoItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(silentVideo)))
            .setDurationUs(outputDurationMs.coerceAtLeast(1) * 1000)
            .setRemoveAudio(true)
            .build()
        val audioMedia = MediaItem.Builder()
            .setUri(Uri.fromFile(File(sourcePath)))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
            .build()
        val sonic = SonicAudioProcessor().apply {
            setSpeed(rate)
            setPitch(1f)
        }
        val audioItem = EditedMediaItem.Builder(audioMedia)
            .setRemoveVideo(true)
            .setDurationUs(outputDurationMs.coerceAtLeast(1) * 1000)
            .setEffects(Effects(listOf(sonic), emptyList()))
            .build()
        val composition = Composition.Builder(
            listOf(
                EditedMediaItemSequence.Builder(listOf(videoItem)).build(),
                EditedMediaItemSequence.Builder(listOf(audioItem)).build()
            )
        )
            .experimentalSetForceAudioTrack(true)
            .build()
        startTransformer(context, composition, output) { }
    }

    private data class NeuralExportPrep(
        val paths: List<String>,
        val timeline: MultiTrackTimeline,
        val temps: List<File>
    )

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
        val effects = clipEffects(clip, path)
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
                composeAudio(
                    videoSequences = listOf(videoSeq),
                    paths = paths,
                    timeline = timeline,
                    durationOverrideMs = pictureDurationMs.coerceAtLeast(250)
                ),
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
        videoSequences: List<EditedMediaItemSequence>,
        paths: List<String>,
        timeline: Timeline,
        durationOverrideMs: Long? = null,
        extraAudioSequences: List<EditedMediaItemSequence> = emptyList(),
        compositor: VideoCompositorSettings = VideoCompositorSettings.DEFAULT,
        burnInTc: Boolean = false
    ): Composition {
        val mixed = timeline.withStemBusesApplied()
        val total = (durationOverrideMs ?: mixed.totalMs()).coerceAtLeast(250)
        val sequences = ArrayList<EditedMediaItemSequence>()
        sequences += videoSequences
        sequences += extraAudioSequences
        val voice = resolveStem(paths, mixed.voice)
        val voiceEnd = voice?.let { minOf(durationMs(it.second.absolutePath), total) } ?: 0L
        val bedMeta = mixed.bed
        val rawWindows = mixed.duckWindows(voiceEnd)
        val windows = if (bedMeta != null) {
            AudioFinish.polishWindows(rawWindows, bedMeta.attackMs, bedMeta.releaseMs, total)
        } else {
            rawWindows
        }
        resolveStem(paths, mixed.bed)?.let { (bed, file) ->
            sequences += bedSequence(file, bed, total, durationMs(file.absolutePath), windows)
        }
        voice?.let { (stem, file) ->
            sequences += voiceSequence(file, stem, total, durationMs(file.absolutePath))
        }
        for (stem in mixed.extraStems) {
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
                attackMs = 120,
                slipMs = stem.slipMs
            )
            val dur = durationMs(file.absolutePath)
            if (stem.role.equals("music", ignoreCase = true)) {
                sequences += bedSequence(file, asBed, total, dur, windows)
            } else {
                sequences += voiceSequence(file, asBed, total, dur)
            }
        }
        val height = MovieExportSize.normalize(mixed.outputHeight)
        val videoEffects = ArrayList<Effect>()
        videoEffects += Presentation.createForHeight(height)
        TitleOverlays.effect(mixed.titles, total)?.let { videoEffects += it }
        val trackAnchors = TrackAnchorResolver.forTimeline(paths, mixed)
        VfxOverlays.effect(mixed.vfx, total, trackAnchors)?.let { videoEffects += it }
        Title3dOverlays.effect(mixed.titles3d, total)?.let { videoEffects += it }
        val projectDir = paths.firstOrNull()?.let { File(it).parentFile }
        val cg = CgOverlays.effect(mixed.cg, total, trackAnchors, projectDir)
        // Depth sampling reads the frame on the CPU. That effect rejects HDR,
        // so Keep HDR stays on manual Z-order. No engine means no extra pass.
        if (cg != null &&
            mixed.hdrMode != MovieHdrMode.KeepHdr &&
            PackEngineHooks.hasEngine(PackId.Depth)
        ) {
            videoEffects += depthSampleEffect()
        }
        cg?.let { videoEffects += it }
        TimecodeOverlays.effect(burnInTc, total)?.let { videoEffects += it }
        MovieGrade.effect(mixed.grade)?.let { videoEffects += it }
        val audioProcessors = ArrayList<AudioProcessor>()
        val fx = if (mixed.loudness.isOff()) {
            mixed.masterFx
        } else {
            mixed.loudness.applyMaster(mixed.masterFx)
        }
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
        val multiVideo = videoSequences.size > 1
        return if (sequences.size == 1 && !multiVideo) {
            Composition.Builder(videoSequences.first())
                .setEffects(compositionEffects)
                .applyHdr(mixed.hdrMode)
                .build()
        } else {
            val builder = Composition.Builder(sequences)
                .experimentalSetForceAudioTrack(true)
                .setEffects(compositionEffects)
                .applyHdr(mixed.hdrMode)
            if (multiVideo) {
                builder.setVideoCompositorSettings(compositor)
            }
            builder.build()
        }
    }

    /** D11 — HDR export intent on Composition (tone-map SDR vs Keep HDR + Ultra HDR gain map). */
    private fun Composition.Builder.applyHdr(mode: MovieHdrMode): Composition.Builder {
        setHdrMode(mode.toCompositionHdrMode())
        if (mode.retainUltraHdrGainMap()) {
            experimentalSetRetainHdrFromUltraHdrImage(true)
        }
        return this
    }

    /** Parallel video input for PiP / Split / Grid (D7). Duration locked to primary timeline. */
    private fun parallelVideoSequence(
        path: String,
        totalMs: Long,
        removeAudio: Boolean
    ): EditedMediaItemSequence {
        val sourceMs = durationMs(path).coerceAtLeast(1)
        val endMs = minOf(sourceMs, totalMs.coerceAtLeast(1))
        val media = MediaItem.Builder()
            .setUri(Uri.fromFile(File(path)))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(0)
                    .setEndPositionMs(endMs)
                    .build()
            )
            .build()
        val edited = EditedMediaItem.Builder(media)
            .setDurationUs(totalMs.coerceAtLeast(1) * 1000)
        if (removeAudio) edited.setRemoveAudio(true)
        return EditedMediaItemSequence.Builder(listOf(edited.build())).build()
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
        val slip = AudioSync.clampSlip(stem.slipMs)
        val advance = (-slip).coerceAtLeast(0L)
        val delay = slip.coerceAtLeast(0L)
        val take = minOf((sourceMs - advance).coerceAtLeast(1), totalMs)
        val media = MediaItem.Builder()
            .setUri(Uri.fromFile(file))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(advance)
                    .setEndPositionMs(advance + take)
                    .build()
            )
            .build()
        val edited = EditedMediaItem.Builder(media)
            .setRemoveVideo(true)
            .setDurationUs(take * 1000)
        val vol = ClipMix.volume(stem.volume)
        val needsMix = vol != 1f || stem.fadeInMs > 0 || stem.fadeOutMs > 0 || delay > 0
        if (needsMix) {
            edited.setEffects(
                Effects(
                    listOf(
                        EnvelopeAudioProcessor(
                            durationMs = take,
                            volume = vol,
                            fadeInMs = stem.fadeInMs,
                            fadeOutMs = stem.fadeOutMs,
                            delayMs = delay
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
        val slip = AudioSync.clampSlip(bed.slipMs)
        val advance = (-slip).coerceAtLeast(0L)
        val delay = slip.coerceAtLeast(0L)
        val slice = (bedDur - advance).coerceAtLeast(250)
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
                        .setStartPositionMs(advance)
                        .setEndPositionMs(advance + take)
                        .build()
                )
                .build()
            val edited = EditedMediaItem.Builder(media)
                .setRemoveVideo(true)
                .setDurationUs(take * 1000)
            val needsMix = vol != 1f || bed.fadeInMs > 0 || bed.fadeOutMs > 0 ||
                (duck < 0.999f && windows.isNotEmpty()) || delay > 0
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
                                attackMs = bed.attackMs,
                                releaseMs = bed.releaseMs,
                                delayMs = if (index == 0) delay else 0L
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

    private fun clipEffects(clip: TimelineClip, mediaPath: String? = null): Effects? {
        val speed = MovieSpeed.playbackRate(clip)
        val audio = ArrayList<AudioProcessor>()
        val video = ArrayList<Effect>()
        if (kotlin.math.abs(speed - 1f) > 0.01f) {
            audio += SonicAudioProcessor().apply {
                setSpeed(speed)
                setPitch(1f)
            }
            video += SpeedChangeEffect(speed)
        }
        if (clip.hasMotion) {
            val durationUs = clip.durationMs.coerceAtLeast(1) * 1000L
            video += clip.motion.toMatrixTransformation(durationUs)
        }
        if (clip.hasTrack && mediaPath != null) {
            val follow = TrackPathBake.motion(mediaPath, clip.track, clip.inMs, clip.outMs)
            if (!follow.isIdentity) {
                val durationUs = clip.durationMs.coerceAtLeast(1) * 1000L
                video += follow.toMatrixTransformation(durationUs)
            }
        }
        if (clip.hasFrame) {
            video += clip.frame.toEffects()
        }
        ChromaKeyEffect.from(clip.chroma)?.let { video += it }
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
            val coefficients = FloatArray(ch * ch) { index ->
                if (index % (ch + 1) == 0) volume else 0f
            }
            processor.putChannelMixingMatrix(ChannelMixingMatrix(ch, ch, coefficients))
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
