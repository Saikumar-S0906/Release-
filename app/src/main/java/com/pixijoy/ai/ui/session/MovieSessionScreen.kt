package com.pixijoy.ai.ui.session

import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixijoy.ai.ui.StudioShellViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil3.compose.AsyncImage
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.isStudioTwoPane
import com.pixijoy.ai.export.MovieExportWorker
import com.pixijoy.core.common.AppResult
import com.pixijoy.core.data.Project
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiSecondaryButton
import com.pixijoy.designsystem.PixiSuccess
import com.pixijoy.designsystem.PixiSurface
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.movie.AudioBed
import com.pixijoy.movie.AudioMasterFx
import com.pixijoy.movie.AudioStem
import com.pixijoy.movie.ClipMix
import com.pixijoy.movie.ClipTransition
import com.pixijoy.movie.CompositionPreviewCapability
import com.pixijoy.movie.CompositionPreviewSeekResult
import com.pixijoy.movie.CompositionPreviewState
import com.pixijoy.movie.CompositionPreview
import com.pixijoy.movie.DuckPreset
import com.pixijoy.movie.Filmstrip
import com.pixijoy.movie.FilmstripFrame
import com.pixijoy.movie.MovieExportSize
import com.pixijoy.movie.MovieHdrMode
import com.pixijoy.movie.MovieMotion
import com.pixijoy.movie.MovieSpeed
import com.pixijoy.movie.MovieTimelineDisplay
import com.pixijoy.movie.MovieTrim
import com.pixijoy.movie.MultiTrackTimeline
import com.pixijoy.movie.Timeline
import com.pixijoy.movie.TimelineClip
import com.pixijoy.movie.TimelineTitle
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MovieSessionScreen(
    project: Project,
    toolLabel: String,
    toolKey: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shell = hiltViewModel<StudioShellViewModel>()
    val repo = shell.repository
    val engine = shell.movieEngine
    val paths = remember(project.media, project.coverPath) {
        project.media.map { it.path }.ifEmpty { listOf(project.coverPath) }
    }
    val path = paths.first()
    val talking = toolKey == "talking"
    val still = remember(path) { engine.isStill(path) }
    val sourceDurations = remember(paths, engine) {
        paths.map { engine.durationMs(it).coerceAtLeast(1) }
    }
    val duration = remember(path, talking, paths, sourceDurations) {
        if (still) MovieTrim.stillDurationMs(talking)
        else sourceDurations.sum().coerceAtLeast(1000)
    }
    val initialTimeline = remember(project.id) {
        (
            MultiTrackTimeline.decode(project.graph)
                ?: MultiTrackTimeline.fromFlatTimeline(
                    Timeline.decode(project.graph, paths.size) { i ->
                        engine.durationMs(paths.getOrElse(i) { path })
                    }
                )
            ).ensureDefaultTracks()
    }
    val sessionViewModel: MovieSessionViewModel =
        hiltViewModel<HiltMovieSessionViewModel, HiltMovieSessionViewModel.Factory>(
            key = "movie-session-${project.id}",
            creationCallback = { factory ->
                factory.create(
                    initialTimeline = initialTimeline,
                    projectId = project.id
                )
            }
        )
    val toolEntry = remember(project.id, toolKey) { Any() }
    if (sessionViewModel.syncToolChrome(toolKey, toolEntry)) {
        sessionViewModel.controls.bindEndMs(
            if (still) duration else engine.durationMs(path).coerceAtLeast(1000)
        )
    }
    val sessionState by sessionViewModel.state.collectAsState()
    val multi = sessionState.multi
    val timeline = sessionState.timeline
    val previewEpoch = sessionState.previewEpoch
    FlushSessionOnStop(sessionViewModel::flushInBackground)
    PredictiveBackHandler {
        it.collect { }
        runCatching { sessionViewModel.flush() }
        onBack()
    }
    fun leaveSession() {
        scope.launch {
            runCatching { sessionViewModel.flush() }
            onBack()
        }
    }
    fun settlePreview() {
        sessionViewModel.settleGesture()
    }
    fun commitTimeline(next: Timeline, live: Boolean = false) {
        sessionViewModel.setTimeline(next, live)
    }
    fun commitMulti(next: MultiTrackTimeline, live: Boolean = false) {
        sessionViewModel.setMulti(next, live)
    }
    var movieGroup by sessionViewModel.movieGroupState
    var effectSub by sessionViewModel.effectSubState
    var selectedId by sessionViewModel.selectedClipState
    var selectedTitleId by sessionViewModel.selectedTitleState
    val sessionControls = sessionViewModel.controls
    with(sessionControls) {
    val addClip = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                repo.addUri(project.id, uri)
            }
        }
    }
    val addMusic = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                when (val result = repo.importAudioFile(project.id, uri, "bed")) {
                    is AppResult.Ok -> {
                        val bed = DuckPreset.Medium.apply(
                            AudioBed(fileName = File(result.value).name)
                        )
                        commitTimeline(timeline.copy(bed = bed))
                    }
                    is AppResult.Err -> status = result.message
                }
            }
        }
    }
    val addVoice = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                when (val result = repo.importAudioFile(project.id, uri, "voice")) {
                    is AppResult.Ok -> {
                        val voice = AudioBed(
                            fileName = File(result.value).name,
                            volume = 1f,
                            duck = 1f,
                            fadeInMs = 80,
                            fadeOutMs = 120
                        )
                        val bed = timeline.bed?.let { DuckPreset.Medium.apply(it) }
                        commitTimeline(timeline.copy(voice = voice, bed = bed))
                    }
                    is AppResult.Err -> status = result.message
                }
            }
        }
    }
    val addSfx = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                when (val result = repo.importAudioFile(project.id, uri, "sfx")) {
                    is AppResult.Ok -> {
                        val name = File(result.value).name
                        val id = "sfx${timeline.extraStems.size + 1}"
                        commitTimeline(timeline.copy(
                            extraStems = timeline.extraStems + AudioStem(
                                id = id,
                                fileName = name,
                                volume = 1f,
                                role = "sfx"
                            )
                        ))
                    }
                    is AppResult.Err -> status = result.message
                }
            }
        }
    }
    val selected = timeline.clips.firstOrNull { it.id == selectedId } ?: timeline.clips.firstOrNull()
    val projectDir = remember(path) { File(path).parent }
    val bedFile = timeline.bed?.let { File(projectDir, it.fileName) }?.takeIf { it.exists() }
    val voiceFile = timeline.voice?.let { File(projectDir, it.fileName) }?.takeIf { it.exists() }

    val compositionPreview = remember(still, engine) {
        if (still) null else CompositionPreview(context, engine)
    }
    val previewState = if (compositionPreview == null) {
        CompositionPreviewState()
    } else {
        val current by compositionPreview.state.collectAsState()
        current
    }
    val player = previewState.player
    val bedPlayer = remember { if (still) ExoPlayer.Builder(context).build() else null }
    val voicePlayer = remember { if (still) ExoPlayer.Builder(context).build() else null }
    DisposableEffect(compositionPreview) {
        onDispose { compositionPreview?.release() }
    }
    DisposableEffect(bedPlayer) {
        onDispose { bedPlayer?.release() }
    }
    DisposableEffect(voicePlayer) {
        onDispose { voicePlayer?.release() }
    }
    LaunchedEffect(bedFile?.absolutePath, timeline.bed?.muted) {
        val exo = bedPlayer ?: return@LaunchedEffect
        val file = bedFile
        val bed = timeline.bed
        if (file != null && bed != null && !bed.muted) {
            exo.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            exo.repeatMode = Player.REPEAT_MODE_ALL
            exo.prepare()
            exo.playWhenReady = playing
            exo.volume = bed.volume
        } else {
            exo.stop()
            exo.clearMediaItems()
        }
    }
    LaunchedEffect(voiceFile?.absolutePath, timeline.voice?.muted) {
        val exo = voicePlayer ?: return@LaunchedEffect
        val file = voiceFile
        val voice = timeline.voice
        if (file != null && voice != null && !voice.muted) {
            exo.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            exo.repeatMode = Player.REPEAT_MODE_OFF
            exo.prepare()
            exo.playWhenReady = playing
            exo.volume = voice.volume
        } else {
            exo.stop()
            exo.clearMediaItems()
        }
    }
    LaunchedEffect(paths.size) {
        if (!still && timeline.clips.size != paths.size) {
            val next = Timeline.fromMedia(paths.size) { engine.durationMs(paths[it]) }
                .copy(
                    bed = timeline.bed,
                    voice = timeline.voice,
                    extraStems = timeline.extraStems,
                    masterFx = timeline.masterFx
                )
            commitTimeline(next)
            selectedId = next.clips.firstOrNull()?.id
        }
    }
    LaunchedEffect(paths.joinToString(), previewEpoch, compositionPreview) {
        compositionPreview?.bind(paths, multi, playing)
    }
    LaunchedEffect(player) {
        val exo = player ?: return@LaunchedEffect
        while (true) {
            sessionViewModel.notePlaybackPosition(exo.currentPosition, timeline.totalMs())
            delay(50)
        }
    }
    val workManager = remember { WorkManager.getInstance(context) }
    val exportInfos by workManager
        .getWorkInfosForUniqueWorkFlow(MovieExportWorker.uniqueName(project.id))
        .collectAsState(initial = emptyList())
    LaunchedEffect(exportInfos) {
        val info = exportInfos.firstOrNull() ?: return@LaunchedEffect
        when (info.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> {
                val fraction = info.progress.getFloat(MovieExportWorker.KEY_PROGRESS, sessionControls.progress)
                val pct = (fraction * 100f).toInt().coerceIn(0, 100)
                sessionViewModel.onExportRunning(
                    fraction,
                    context.getString(R.string.studio_export_pct, pct)
                )
            }
            WorkInfo.State.SUCCEEDED -> {
                val gifOut = info.outputData.getBoolean(MovieExportWorker.KEY_GIF, false)
                sessionViewModel.onExportSucceeded(
                    info.outputData.getString(MovieExportWorker.KEY_URI),
                    context.getString(
                        if (gifOut) R.string.studio_export_gif_ok else R.string.studio_export_movie_ok
                    )
                )
            }
            WorkInfo.State.FAILED -> sessionViewModel.onExportFailed(
                info.outputData.getString(MovieExportWorker.KEY_ERROR)
                    ?: context.getString(R.string.studio_export_failed)
            )
            else -> Unit
        }
    }

    fun clipFps(): Float {
        val p = paths.getOrElse(selected?.mediaIndex ?: 0) { path }
        return engine.fps(p)
    }

    fun seekComposition(ms: Long): Boolean =
        when (compositionPreview?.seekMs(ms)) {
            CompositionPreviewSeekResult.Applied -> true
            CompositionPreviewSeekResult.Unsupported -> {
                status = context.getString(R.string.studio_layout_seek_unavailable)
                false
            }
            CompositionPreviewSeekResult.Failed,
            CompositionPreviewSeekResult.Unavailable,
            null -> {
                status = context.getString(R.string.studio_layout_preview_export_only)
                false
            }
        }

    fun stepFrame(delta: Int) {
        player?.playWhenReady = false
        val total = timeline.totalMs()
        val next = sessionViewModel.stepPlayhead(delta, clipFps(), total)
        if (seekComposition(next)) {
            sessionViewModel.confirmSteppedPlayhead(next, total)
        }
    }

    fun runExport(gif: Boolean) {
        player?.playWhenReady = false
        bedPlayer?.playWhenReady = false
        voicePlayer?.playWhenReady = false
        sessionViewModel.beginExport(context.getString(R.string.studio_export_pct, 0))
        scope.launch {
            sessionViewModel.flush()
            MovieExportWorker.ensureChannel(context)
            val request = OneTimeWorkRequestBuilder<MovieExportWorker>()
                .setInputData(
                    workDataOf(
                        MovieExportWorker.KEY_PROJECT to project.id,
                        MovieExportWorker.KEY_GIF to gif,
                        MovieExportWorker.KEY_TALKING to talking
                    )
                )
                .build()
            workManager.enqueueUniqueWork(
                MovieExportWorker.uniqueName(project.id),
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    val configuration = LocalConfiguration.current
    val sideTools = isStudioTwoPane(configuration.screenWidthDp, configuration.screenHeightDp)
    val haptics = LocalHapticFeedback.current
    @Composable
    fun TimelineDock() {
        MovieTimelineDock(
            playhead = playhead,
            playing = playing,
            timeline = timeline,
            selected = selected,
            paths = paths,
            sourceDurations = sourceDurations,
            thumbnailEpoch = previewEpoch,
            seekEnabled = previewState.result.capability ==
                CompositionPreviewCapability.LiveSingle,
            playEnabled = previewState.result.capability !=
                CompositionPreviewCapability.ExportOnly,
            onPlayhead = sessionViewModel::setPlayhead,
            onSeek = { ms ->
                if (seekComposition(ms)) {
                    val bedDur = bedPlayer?.duration ?: 0L
                    if (bedDur > 0) bedPlayer?.seekTo(ms % bedDur)
                    val voiceDur = voicePlayer?.duration ?: 0L
                    if (voiceDur > 0) voicePlayer?.seekTo(ms.coerceAtMost(voiceDur))
                    true
                } else {
                    false
                }
            },
            onTogglePlay = {
                val nextPlaying = sessionViewModel.togglePlaying()
                player?.playWhenReady = nextPlaying
                bedPlayer?.playWhenReady = nextPlaying
                voicePlayer?.playWhenReady = nextPlaying
            },
            onStep = { stepFrame(it) },
            onSelectClip = { selectedId = it },
            onTrimIn = { id, value ->
                commitTimeline(timeline.rippleTrimIn(id, value), live = true)
            },
            onTrimOut = { id, value, sourceEnd ->
                commitTimeline(
                    timeline.rippleTrimOut(id, value, sourceEnd),
                    live = true
                )
            },
            onTrimSettled = { settlePreview() }
        )
    }
    @Composable
    fun ToolsChrome(sideDock: Boolean, modifier: Modifier = Modifier) {
        StudioChromeBar(
            modifier = modifier,
            sideDock = sideDock,
            panelScrollKey = sessionViewModel.chromeEpoch to movieGroup to effectSub,
            abovePanel = if (!sideDock && !still) {
                {
                    TimelineDock()
                }
            } else {
                null
            },
            rail = {
                StudioGroupRail(
                    items = movieGroupItems(),
                    selectedTag = movieGroup.tag,
                    onSelect = { tag ->
                        movieGroup = MovieGroup.entries.first { it.tag == tag }
                    }
                )
            },
            panel = {
            if (movieGroup == MovieGroup.Layout) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("movie_tracks"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                multi.tracks.forEach { track ->
                    val label = if (track.muted) {
                        stringResource(R.string.studio_track_muted, track.name)
                    } else {
                        track.name
                    }
                    Text(
                        label,
                        color = PixiText,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (track.muted) PixiBg else PixiAccent)
                            .clickable {
                                commitMulti(multi.withTrackMuted(track.id, !track.muted))
                            }
                            .testTag("movie_track_${track.id}")
                            .semantics { role = Role.Button }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
            }
            StudioHint(
                stringResource(
                    when {
                        previewState.result.capability == CompositionPreviewCapability.ExportOnly ->
                            R.string.studio_layout_preview_export_only
                        multi.layout.isMulti() -> R.string.studio_layout_preview_wysiwyg
                        else -> R.string.studio_layout_preview_single
                    }
                ),
                tag = if (previewState.result.capability == CompositionPreviewCapability.ExportOnly) {
                    "movie_layout_preview_export_only"
                } else {
                    "movie_layout_preview_live"
                }
            )
            }
            val canRoll = run {
                val id = selected?.id ?: return@run false
                val i = timeline.clips.indexOfFirst { it.id == id }
                if (i < 0 || i >= timeline.clips.lastIndex) return@run false
                val left = timeline.clips[i]
                val right = timeline.clips[i + 1]
                left.mediaIndex == right.mediaIndex && left.outMs == right.inMs
            }
            if (movieGroup == MovieGroup.Edit) {
            key(sessionViewModel.chromeEpoch) {
            MovieEditOpsPanel(
                selected = selected,
                canRoll = canRoll,
                onRoll = { delta ->
                    val id = selected?.id ?: return@MovieEditOpsPanel
                    commitTimeline(timeline.rollCut(id, delta))
                },
                onTransition = { kind, ms ->
                    val id = selected?.id ?: return@MovieEditOpsPanel
                    commitTimeline(
                        timeline.update(id) {
                            it.copy(transition = kind, transitionMs = if (kind == ClipTransition.None) 0L else ms)
                        },
                        live = true
                    )
                },
                onSpeedRamp = { start, end ->
                    val id = selected?.id ?: return@MovieEditOpsPanel
                    sessionViewModel.setSpeed(start)
                    commitTimeline(
                        timeline.update(id) { it.copy(speed = start, speedEnd = end) },
                        live = true
                    )
                },
                onSettled = { settlePreview() }
            )
            }
            }
            if (movieGroup == MovieGroup.Effects) {
            StudioHScrollRow {
                StudioChip(stringResource(R.string.studio_motion), effectSub == MovieEffectSub.Motion, MovieEffectSub.Motion.tag) {
                    effectSub = MovieEffectSub.Motion
                }
                StudioChip(stringResource(R.string.studio_stabilize), effectSub == MovieEffectSub.Frame, MovieEffectSub.Frame.tag) {
                    effectSub = MovieEffectSub.Frame
                }
                StudioChip(stringResource(R.string.studio_chroma_key), effectSub == MovieEffectSub.Chroma, MovieEffectSub.Chroma.tag) {
                    effectSub = MovieEffectSub.Chroma
                }
                StudioChip(stringResource(R.string.studio_slow_mo), effectSub == MovieEffectSub.SlowMo, MovieEffectSub.SlowMo.tag) {
                    effectSub = MovieEffectSub.SlowMo
                }
                StudioChip(stringResource(R.string.studio_track), effectSub == MovieEffectSub.Track, MovieEffectSub.Track.tag) {
                    effectSub = MovieEffectSub.Track
                }
            }
            if (effectSub == MovieEffectSub.Motion) {
            MovieMotionPanel(
                selected = selected,
                onMotion = { motion ->
                    val id = selected?.id ?: return@MovieMotionPanel
                    commitTimeline(timeline.update(id) { it.copy(motion = motion) })
                }
            )
            }
            if (effectSub == MovieEffectSub.Frame) {
            MovieFramePanel(
                selected = selected,
                onFrame = { frame ->
                    val id = selected?.id ?: return@MovieFramePanel
                    commitTimeline(timeline.update(id) { it.copy(frame = frame) })
                }
            )
            }
            if (effectSub == MovieEffectSub.Chroma) {
            MovieChromaPanel(
                selected = selected,
                onChroma = { chroma ->
                    val id = selected?.id ?: return@MovieChromaPanel
                    commitTimeline(timeline.update(id) { it.copy(chroma = chroma) }, live = true)
                },
                onSettled = { settlePreview() }
            )
            }
            if (effectSub == MovieEffectSub.SlowMo) {
            MovieSlowMoPanel(
                selected = selected,
                onSlowMo = { slowMo ->
                    val id = selected?.id ?: return@MovieSlowMoPanel
                    commitTimeline(timeline.update(id) { it.copy(slowMo = slowMo) })
                }
            )
            }
            if (effectSub == MovieEffectSub.Track) {
            MovieTrackPanel(
                selected = selected,
                onTrack = { track ->
                    val id = selected?.id ?: return@MovieTrackPanel
                    commitTimeline(timeline.update(id) { it.copy(track = track) })
                }
            )
            }
            }
            if (movieGroup == MovieGroup.Text) {
            MovieTitlesPanel(
                titles = multi.titles,
                selectedId = selectedTitleId,
                onSelect = { selectedTitleId = it },
                onAddTitle = {
                    val total = multi.totalMs()
                    val next = TimelineTitle.titleDefault(total, id = "t${multi.titles.size}")
                    commitMulti(multi.copy(titles = multi.titles + next))
                    selectedTitleId = next.id
                },
                onAddCaption = {
                    val total = multi.totalMs()
                    val next = TimelineTitle.captionDefault(total, id = "c${multi.titles.size}")
                    commitMulti(multi.copy(titles = multi.titles + next))
                    selectedTitleId = next.id
                },
                onClear = {
                    commitMulti(multi.copy(titles = emptyList()))
                    selectedTitleId = null
                }
            )
            }
            if (movieGroup == MovieGroup.Color) {
            MovieGradePanel(
                grade = multi.grade,
                onGrade = { next -> commitMulti(multi.copy(grade = next), live = true) },
                onSettled = { settlePreview() }
            )
            MovieHdrPanel(
                hdrMode = multi.hdrMode,
                onHdrMode = { next -> commitMulti(multi.copy(hdrMode = next)) }
            )
            }
            if (movieGroup == MovieGroup.Audio) {
            MovieAudioFinishPanel(
                bed = timeline.bed,
                hasVoice = timeline.voice != null,
                onApplyPreset = { preset ->
                    val bed = timeline.bed ?: return@MovieAudioFinishPanel
                    commitTimeline(timeline.copy(bed = preset.apply(bed)))
                },
                onBedChange = { bed -> commitTimeline(timeline.copy(bed = bed), live = true) },
                onSettled = { settlePreview() }
            )
            }
            if (movieGroup == MovieGroup.Layout) {
            MovieLayoutPanel(
                layout = multi.layout,
                onLayout = { next -> commitMulti(multi.copy(layout = next)) }
            )
            }
            if (!still && movieGroup == MovieGroup.Edit) {
                val selectedSourceEnd = engine.durationMs(
                    paths.getOrElse(selected?.mediaIndex ?: 0) { path }
                ).coerceAtLeast(1000)
                Text(stringResource(R.string.studio_trim_start), color = PixiTextMuted)
                Slider(
                    value = ((selected?.inMs ?: startMs) / selectedSourceEnd.toFloat()).coerceIn(0f, 1f),
                    onValueChange = {
                        val id = selected?.id ?: return@Slider
                        val inn = (it * selectedSourceEnd).toLong()
                        sessionViewModel.setTrimStart(inn)
                        commitTimeline(timeline.rippleTrimIn(id, inn), live = true)
                    },
                    onValueChangeFinished = { settlePreview() },
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_trim_start")
                )
                Text(stringResource(R.string.studio_trim_end), color = PixiTextMuted)
                Slider(
                    value = ((selected?.outMs ?: endMs) / selectedSourceEnd.toFloat()).coerceIn(0f, 1f),
                    onValueChange = {
                        val id = selected?.id ?: return@Slider
                        val out = (it * selectedSourceEnd).toLong()
                        sessionViewModel.setTrimEnd(out)
                        commitTimeline(timeline.rippleTrimOut(id, out, selectedSourceEnd), live = true)
                    },
                    onValueChangeFinished = { settlePreview() },
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_trim_end")
                )
                TextButton(
                    onClick = {
                        val ph = (playhead * timeline.totalMs()).toLong()
                        val next = timeline.split(ph)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        commitTimeline(next)
                        selectedId = next.clips.firstOrNull()?.id
                    },
                    modifier = Modifier.testTag("movie_split")
                ) {
                    Text(stringResource(R.string.studio_split), color = PixiText)
                }
                TextButton(
                    onClick = {
                        val id = selected?.id ?: return@TextButton
                        commitTimeline(timeline.update(id) { it.copy(muted = !it.muted) })
                    },
                    modifier = Modifier.testTag("movie_mute")
                ) {
                    Text(
                        if (selected?.muted == true) stringResource(R.string.studio_unmute)
                        else stringResource(R.string.studio_mute),
                        color = PixiText
                    )
                }
                Text(
                    stringResource(R.string.studio_volume, (selected?.volume ?: 1f) * 100f),
                    color = PixiTextMuted
                )
                Slider(
                    value = selected?.volume ?: 1f,
                    onValueChange = { v ->
                        val id = selected?.id ?: return@Slider
                        commitTimeline(timeline.update(id) { it.copy(volume = ClipMix.volume(v)) }, live = true)
                    },
                    onValueChangeFinished = { settlePreview() },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_volume")
                )
                Text(
                    stringResource(R.string.studio_fade_in, (selected?.fadeInMs ?: 0L).toInt()),
                    color = PixiTextMuted
                )
                Slider(
                    value = (selected?.fadeInMs ?: 0L).toFloat(),
                    onValueChange = { v ->
                        val id = selected?.id ?: return@Slider
                        commitTimeline(timeline.update(id) { it.copy(fadeInMs = v.toLong().coerceAtLeast(0)) }, live = true)
                    },
                    onValueChangeFinished = { settlePreview() },
                    valueRange = 0f..2000f,
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_fade_in")
                )
                Text(
                    stringResource(R.string.studio_fade_out, (selected?.fadeOutMs ?: 0L).toInt()),
                    color = PixiTextMuted
                )
                Slider(
                    value = (selected?.fadeOutMs ?: 0L).toFloat(),
                    onValueChange = { v ->
                        val id = selected?.id ?: return@Slider
                        commitTimeline(timeline.update(id) { it.copy(fadeOutMs = v.toLong().coerceAtLeast(0)) }, live = true)
                    },
                    onValueChangeFinished = { settlePreview() },
                    valueRange = 0f..2000f,
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_fade_out")
                )
                TextButton(
                    onClick = {
                        val id = selected?.id ?: return@TextButton
                        val next = timeline.remove(id)
                        commitTimeline(next)
                        selectedId = next.clips.firstOrNull()?.id
                    },
                    modifier = Modifier.testTag("movie_delete_clip")
                ) {
                    Text(stringResource(R.string.studio_delete_clip), color = PixiText)
                }
                PixiSecondaryButton(
                    text = stringResource(R.string.studio_add_clip),
                    onClick = {
                        addClip.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("movie_add_clip")
                )
                if (paths.size > 1) {
                    Text(
                        stringResource(R.string.studio_clip_count, paths.size),
                        color = PixiTextMuted
                    )
                }
            } else if (still && movieGroup == MovieGroup.Edit) {
                Text(
                    if (talking) stringResource(R.string.studio_talking_hint) else stringResource(R.string.studio_move_hint),
                    color = PixiTextMuted
                )
            }
            if (movieGroup == MovieGroup.Audio) {
            PixiSecondaryButton(
                text = stringResource(
                    if (timeline.bed == null) R.string.studio_add_music else R.string.studio_replace_music
                ),
                onClick = { addMusic.launch("audio/*") },
                modifier = Modifier.fillMaxWidth().testTag("movie_add_music")
            )
            val bed = timeline.bed
            if (bed != null) {
                Text(
                    stringResource(R.string.studio_music_volume, bed.volume * 100f),
                    color = PixiTextMuted
                )
                Slider(
                    value = bed.volume,
                    onValueChange = { v ->
                        commitTimeline(
                            timeline.copy(bed = bed.copy(volume = ClipMix.volume(v))),
                            live = true
                        )
                    },
                    onValueChangeFinished = { settlePreview() },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_bed_volume")
                )
                Text(
                    stringResource(R.string.studio_fade_in, bed.fadeInMs.toInt()),
                    color = PixiTextMuted
                )
                Slider(
                    value = bed.fadeInMs.toFloat(),
                    onValueChange = {
                        commitTimeline(
                            timeline.copy(bed = bed.copy(fadeInMs = it.toLong().coerceAtLeast(0))),
                            live = true
                        )
                    },
                    onValueChangeFinished = { settlePreview() },
                    valueRange = 0f..2000f,
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_bed_fade_in")
                )
                Text(
                    stringResource(R.string.studio_fade_out, bed.fadeOutMs.toInt()),
                    color = PixiTextMuted
                )
                Slider(
                    value = bed.fadeOutMs.toFloat(),
                    onValueChange = {
                        commitTimeline(
                            timeline.copy(bed = bed.copy(fadeOutMs = it.toLong().coerceAtLeast(0))),
                            live = true
                        )
                    },
                    onValueChangeFinished = { settlePreview() },
                    valueRange = 0f..2000f,
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_bed_fade_out")
                )
                Text(
                    stringResource(R.string.studio_duck, bed.duck * 100f),
                    color = PixiTextMuted
                )
                Slider(
                    value = bed.duck,
                    onValueChange = { v ->
                        commitTimeline(
                            timeline.copy(bed = bed.copy(duck = ClipMix.volume(v))),
                            live = true
                        )
                    },
                    onValueChangeFinished = { settlePreview() },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_bed_duck")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { commitTimeline(timeline.copy(bed = bed.copy(muted = !bed.muted))) },
                        modifier = Modifier.testTag("movie_bed_mute")
                    ) {
                        Text(
                            if (bed.muted) stringResource(R.string.studio_unmute)
                            else stringResource(R.string.studio_mute),
                            color = PixiText
                        )
                    }
                    TextButton(
                        onClick = { commitTimeline(timeline.copy(bed = null)) },
                        modifier = Modifier.testTag("movie_bed_remove")
                    ) {
                        Text(stringResource(R.string.studio_remove_music), color = PixiText)
                    }
                }
            }
            PixiSecondaryButton(
                text = stringResource(
                    if (timeline.voice == null) R.string.studio_add_voice else R.string.studio_replace_voice
                ),
                onClick = { addVoice.launch("audio/*") },
                modifier = Modifier.fillMaxWidth().testTag("movie_add_voice")
            )
            val voice = timeline.voice
            if (voice != null) {
                Text(
                    stringResource(R.string.studio_voice_volume, voice.volume * 100f),
                    color = PixiTextMuted
                )
                Slider(
                    value = voice.volume,
                    onValueChange = { v ->
                        commitTimeline(
                            timeline.copy(voice = voice.copy(volume = ClipMix.volume(v))),
                            live = true
                        )
                    },
                    onValueChangeFinished = { settlePreview() },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_voice_volume")
                )
                Text(
                    stringResource(R.string.studio_fade_in, voice.fadeInMs.toInt()),
                    color = PixiTextMuted
                )
                Slider(
                    value = voice.fadeInMs.toFloat(),
                    onValueChange = {
                        commitTimeline(
                            timeline.copy(voice = voice.copy(fadeInMs = it.toLong().coerceAtLeast(0))),
                            live = true
                        )
                    },
                    onValueChangeFinished = { settlePreview() },
                    valueRange = 0f..2000f,
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_voice_fade_in")
                )
                Text(
                    stringResource(R.string.studio_fade_out, voice.fadeOutMs.toInt()),
                    color = PixiTextMuted
                )
                Slider(
                    value = voice.fadeOutMs.toFloat(),
                    onValueChange = {
                        commitTimeline(
                            timeline.copy(voice = voice.copy(fadeOutMs = it.toLong().coerceAtLeast(0))),
                            live = true
                        )
                    },
                    onValueChangeFinished = { settlePreview() },
                    valueRange = 0f..2000f,
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_voice_fade_out")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { commitTimeline(timeline.copy(voice = voice.copy(muted = !voice.muted))) },
                        modifier = Modifier.testTag("movie_voice_mute")
                    ) {
                        Text(
                            if (voice.muted) stringResource(R.string.studio_unmute)
                            else stringResource(R.string.studio_mute),
                            color = PixiText
                        )
                    }
                    TextButton(
                        onClick = { commitTimeline(timeline.copy(voice = null)) },
                        modifier = Modifier.testTag("movie_voice_remove")
                    ) {
                        Text(stringResource(R.string.studio_remove_voice), color = PixiText)
                    }
                }
            }
            PixiSecondaryButton(
                text = stringResource(
                    if (timeline.extraStems.isEmpty()) R.string.studio_add_sfx else R.string.studio_replace_sfx
                ),
                onClick = { addSfx.launch("audio/*") },
                modifier = Modifier.fillMaxWidth().testTag("movie_add_sfx")
            )
            val sfx = timeline.extraStems.firstOrNull()
            if (sfx != null) {
                Text(
                    stringResource(R.string.studio_sfx_volume, sfx.volume * 100f),
                    color = PixiTextMuted
                )
                Slider(
                    value = sfx.volume,
                    onValueChange = { v ->
                        commitTimeline(
                            timeline.copy(
                                extraStems = timeline.extraStems.map {
                                    if (it.id == sfx.id) it.copy(volume = ClipMix.volume(v)) else it
                                }
                            ),
                            live = true
                        )
                    },
                    onValueChangeFinished = { settlePreview() },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_sfx_volume")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            commitTimeline(timeline.copy(
                                extraStems = timeline.extraStems.map {
                                    if (it.id == sfx.id) it.copy(muted = !it.muted) else it
                                }
                            ))
                        },
                        modifier = Modifier.testTag("movie_sfx_mute")
                    ) {
                        Text(
                            if (sfx.muted) stringResource(R.string.studio_unmute)
                            else stringResource(R.string.studio_mute),
                            color = PixiText
                        )
                    }
                    TextButton(
                        onClick = {
                            commitTimeline(timeline.copy(
                                extraStems = timeline.extraStems.filterNot { it.id == sfx.id }
                            ))
                        },
                        modifier = Modifier.testTag("movie_sfx_remove")
                    ) {
                        Text(stringResource(R.string.studio_remove_sfx), color = PixiText)
                    }
                }
            }
            Text(stringResource(R.string.studio_master_eq), color = PixiTextMuted)
            val fx = timeline.masterFx
            Text(
                stringResource(R.string.studio_eq_low, fx.lowGainDb),
                color = PixiTextMuted
            )
            Slider(
                value = fx.lowGainDb,
                onValueChange = {
                    commitTimeline(timeline.copy(masterFx = fx.copy(lowGainDb = it)), live = true)
                },
                onValueChangeFinished = { settlePreview() },
                valueRange = -12f..12f,
                colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                modifier = Modifier.testTag("movie_eq_low")
            )
            Text(
                stringResource(R.string.studio_eq_mid, fx.midGainDb),
                color = PixiTextMuted
            )
            Slider(
                value = fx.midGainDb,
                onValueChange = {
                    commitTimeline(timeline.copy(masterFx = fx.copy(midGainDb = it)), live = true)
                },
                onValueChangeFinished = { settlePreview() },
                valueRange = -12f..12f,
                colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                modifier = Modifier.testTag("movie_eq_mid")
            )
            Text(
                stringResource(R.string.studio_eq_high, fx.highGainDb),
                color = PixiTextMuted
            )
            Slider(
                value = fx.highGainDb,
                onValueChange = {
                    commitTimeline(timeline.copy(masterFx = fx.copy(highGainDb = it)), live = true)
                },
                onValueChangeFinished = { settlePreview() },
                valueRange = -12f..12f,
                colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                modifier = Modifier.testTag("movie_eq_high")
            )
            Text(stringResource(R.string.studio_master_dynamics), color = PixiTextMuted)
            Text(
                stringResource(R.string.studio_comp_threshold, fx.compThresholdDb),
                color = PixiTextMuted
            )
            Slider(
                value = fx.compThresholdDb,
                onValueChange = {
                    commitTimeline(timeline.copy(masterFx = fx.copy(compThresholdDb = it)), live = true)
                },
                onValueChangeFinished = { settlePreview() },
                valueRange = -40f..0f,
                colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                modifier = Modifier.testTag("movie_comp_threshold")
            )
            Text(
                stringResource(R.string.studio_comp_ratio, fx.compRatio),
                color = PixiTextMuted
            )
            Slider(
                value = fx.compRatio,
                onValueChange = {
                    commitTimeline(
                        timeline.copy(masterFx = fx.copy(compRatio = it.coerceAtLeast(1f))),
                        live = true
                    )
                },
                onValueChangeFinished = { settlePreview() },
                valueRange = 1f..8f,
                colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                modifier = Modifier.testTag("movie_comp_ratio")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        commitTimeline(timeline.copy(
                            masterFx = fx.copy(limiterEnabled = !fx.limiterEnabled)
                        ))
                    },
                    modifier = Modifier.testTag("movie_limiter_toggle")
                ) {
                    Text(
                        if (fx.limiterEnabled) stringResource(R.string.studio_limiter_on)
                        else stringResource(R.string.studio_limiter_off),
                        color = PixiText
                    )
                }
                TextButton(
                    onClick = { commitTimeline(timeline.copy(masterFx = AudioMasterFx())) },
                    modifier = Modifier.testTag("movie_fx_reset")
                ) {
                    Text(stringResource(R.string.studio_fx_reset), color = PixiText)
                }
            }
            }
            if (movieGroup == MovieGroup.Edit) {
            Text(
                stringResource(R.string.studio_speed, MovieSpeed.normalize(selected?.speed ?: speed)),
                color = PixiTextMuted
            )
            Slider(
                value = MovieSpeed.normalize(selected?.speed ?: speed),
                onValueChange = {
                    val s = MovieSpeed.normalize(it)
                    sessionViewModel.setSpeed(s)
                    val id = selected?.id
                    if (id != null) {
                        commitTimeline(timeline.update(id) { clip -> clip.copy(speed = s, speedEnd = s) }, live = true)
                    }
                },
                onValueChangeFinished = { settlePreview() },
                valueRange = 0.5f..2f,
                colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                modifier = Modifier.testTag("movie_speed")
            )
            }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixiBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("movie_session")
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
        StudioTopChrome(Modifier.align(Alignment.TopCenter).zIndex(2f)) {
            Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = ::leaveSession, modifier = Modifier.testTag("session_back")) {
                    Text(stringResource(R.string.studio_back), color = PixiText)
                }
                Text(
                    toolLabel,
                    color = PixiText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        exportGif = false
                        showExport = true
                    },
                    enabled = !exporting,
                    modifier = Modifier.testTag("session_export")
                ) {
                    Text(stringResource(R.string.studio_export), color = PixiText)
                }
                if (still) {
                    TextButton(
                        onClick = {
                            if (!exporting) runExport(gif = true)
                        },
                        modifier = Modifier.testTag("session_export_gif")
                    ) {
                        Text(stringResource(R.string.studio_export_gif), color = PixiText)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = sessionViewModel::undo,
                    enabled = sessionState.canUndo,
                    modifier = Modifier.testTag("movie_undo")
                ) {
                    Text(stringResource(R.string.studio_undo), color = PixiTextMuted)
                }
                TextButton(
                    onClick = sessionViewModel::redo,
                    enabled = sessionState.canRedo,
                    modifier = Modifier.testTag("movie_redo")
                ) {
                    Text(stringResource(R.string.studio_redo), color = PixiTextMuted)
                }
            }
            }
        }
        Column(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .then(if (sideTools) Modifier.weight(1f).fillMaxWidth() else Modifier.fillMaxSize())
                .clipToBounds()
                .background(PixiSurface)
                .testTag("movie_preview")
        ) {
            if (still) {
                KenBurnsPreview(path = path, talking = talking, durationMs = duration.toInt())
            } else if (player != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            this.player = player
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view -> view.player = player },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    stringResource(R.string.studio_layout_preview_export_only),
                    color = PixiText,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .testTag("movie_preview_export_only")
                )
            }
        }
        if (sideTools && !still) {
            TimelineDock()
        }
        }

        if (!sideTools) {
            ToolsChrome(
                sideDock = false,
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(1f)
            )
        }
        }
        if (sideTools) {
            ToolsChrome(sideDock = true)
        }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(PixiBg)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("session_export_bar"),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (exporting) {
                if (progress <= 0.01f) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("session_export_progress"),
                        color = PixiAccent,
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("session_export_progress"),
                        color = PixiAccent,
                    )
                }
            }
            status?.let {
                Text(
                    it,
                    color = if (exporting) PixiTextMuted else PixiSuccess,
                    modifier = Modifier.testTag("session_status")
                )
            }
        }
    }

    if (showExport) {
        ExportSheet(
            title = stringResource(R.string.studio_export_sheet_title),
            formats = if (still) listOf("mp4" to "MP4", "gif" to "GIF") else listOf("mp4" to "MP4"),
            selectedKey = if (exportGif) "gif" else "mp4",
            quality = 92,
            showQuality = false,
            destination = stringResource(
                if (exportGif) R.string.studio_export_destination_pictures
                else R.string.studio_export_destination_movies
            ),
            exporting = exporting,
            progress = progress,
            onSelectFormat = { exportGif = it == "gif" },
            onQuality = {},
            onExport = {
                runExport(gif = exportGif)
            },
            onDismiss = { if (!exporting) showExport = false },
            resolutions = MovieExportSize.heights.map { it to MovieExportSize.label(it) },
            selectedHeight = MovieExportSize.normalize(timeline.outputHeight),
            onSelectHeight = { height -> commitTimeline(timeline.copy(outputHeight = height)) },
            showHdrMode = true,
            hdrModeKey = timeline.hdrMode.encode(),
            onHdrMode = { key ->
                commitTimeline(timeline.copy(hdrMode = MovieHdrMode.parse(key)))
            },
            showLeaveSafeHint = true,
            successUri = exportSuccessUri,
            onView = {
                exportSuccessUri?.let { uri ->
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW)
                                .setDataAndType(android.net.Uri.parse(uri), if (exportGif) "image/gif" else "video/*")
                                .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        )
                    }
                }
            },
            onShare = {
                exportSuccessUri?.let { uri ->
                    runCatching {
                        context.startActivity(
                            android.content.Intent.createChooser(
                                android.content.Intent(android.content.Intent.ACTION_SEND)
                                    .setType(if (exportGif) "image/gif" else "video/*")
                                    .putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.parse(uri))
                                    .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                context.getString(R.string.export_share)
                            )
                        )
                    }
                }
            },
            onDone = {
                exportSuccessUri = null
                showExport = false
            }
        )
    }
    }
}

@Composable
private fun movieGroupItems(): List<StudioGroupItem> = listOf(
    StudioGroupItem(MovieGroup.Edit.tag, stringResource(R.string.studio_group_edit), R.drawable.pj_ic_edit),
    StudioGroupItem(MovieGroup.Audio.tag, stringResource(R.string.studio_group_audio), R.drawable.pj_ic_music),
    StudioGroupItem(MovieGroup.Text.tag, stringResource(R.string.studio_group_text), R.drawable.pj_ic_text),
    StudioGroupItem(MovieGroup.Color.tag, stringResource(R.string.studio_group_color), R.drawable.pj_ic_palette),
    StudioGroupItem(MovieGroup.Effects.tag, stringResource(R.string.studio_group_effects), R.drawable.pj_ic_wave),
    StudioGroupItem(MovieGroup.Layout.tag, stringResource(R.string.studio_group_layout), R.drawable.pj_ic_grid)
)

@Composable
private fun MovieTimelineDock(
    playhead: Float,
    playing: Boolean,
    timeline: Timeline,
    selected: TimelineClip?,
    paths: List<String>,
    sourceDurations: List<Long>,
    thumbnailEpoch: Int,
    seekEnabled: Boolean,
    playEnabled: Boolean,
    onPlayhead: (Float) -> Unit,
    onSeek: (Long) -> Boolean,
    onTogglePlay: () -> Unit,
    onStep: (Int) -> Unit,
    onSelectClip: (String) -> Unit,
    onTrimIn: (String, Long) -> Unit,
    onTrimOut: (String, Long, Long) -> Unit,
    onTrimSettled: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(
            stringResource(R.string.studio_timeline_clips, timeline.clips.size),
            color = PixiTextMuted,
            fontSize = 12.sp,
            modifier = Modifier.testTag("movie_timeline")
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("movie_timeline_strip"),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(
                items = timeline.clips,
                key = { _, clip -> clip.id }
            ) { index, clip ->
                val sourcePath = paths.getOrElse(clip.mediaIndex) { paths.first() }
                val sourceEndMs = sourceDurations
                    .getOrElse(clip.mediaIndex) { clip.outMs.coerceAtLeast(1) }
                TimelineClipBlock(
                    clip = clip,
                    clipIndex = index,
                    selected = clip.id == selected?.id,
                    sourcePath = sourcePath,
                    sourceEndMs = sourceEndMs,
                    thumbnailEpoch = thumbnailEpoch,
                    onSelect = { onSelectClip(clip.id) },
                    onTrimIn = { onTrimIn(clip.id, it) },
                    onTrimOut = { onTrimOut(clip.id, it, sourceEndMs) },
                    onTrimSettled = onTrimSettled
                )
            }
        }
        Slider(
            value = playhead.coerceIn(0f, 1f),
            onValueChange = {
                val total = timeline.totalMs().coerceAtLeast(1)
                if (onSeek((it * total).toLong())) onPlayhead(it)
            },
            enabled = seekEnabled,
            colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 28.dp)
                .testTag("movie_scrub")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(
                onClick = { onStep(-1) },
                enabled = seekEnabled,
                modifier = Modifier.testTag("movie_frame_prev")
            ) {
                Text(stringResource(R.string.studio_frame_prev), color = PixiText)
            }
            TextButton(
                onClick = onTogglePlay,
                enabled = playEnabled,
                modifier = Modifier.testTag("movie_play")
            ) {
                Text(
                    if (playing) stringResource(R.string.studio_pause) else stringResource(R.string.studio_play),
                    color = PixiText
                )
            }
            TextButton(
                onClick = { onStep(1) },
                enabled = seekEnabled,
                modifier = Modifier.testTag("movie_frame_next")
            ) {
                Text(stringResource(R.string.studio_frame_next), color = PixiText)
            }
        }
    }
}

@Composable
private fun TimelineClipBlock(
    clip: TimelineClip,
    clipIndex: Int,
    selected: Boolean,
    sourcePath: String,
    sourceEndMs: Long,
    thumbnailEpoch: Int,
    onSelect: () -> Unit,
    onTrimIn: (Long) -> Unit,
    onTrimOut: (Long) -> Unit,
    onTrimSettled: () -> Unit
) {
    val density = LocalDensity.current
    val widthDp = MovieTimelineDisplay.clipWidthDp(clip.durationMs)
    val latestClip = rememberUpdatedState(clip)
    val latestTrimIn = rememberUpdatedState(onTrimIn)
    val latestTrimOut = rememberUpdatedState(onTrimOut)
    val latestSettled = rememberUpdatedState(onTrimSettled)
    Box(
        modifier = Modifier
            .width(widthDp.dp)
            .height(68.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PixiAccent else PixiBg)
            .clickable(onClick = onSelect)
            .testTag("timeline_clip")
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
    ) {
        ClipFilmstrip(
            clipId = clip.id,
            path = sourcePath,
            startMs = clip.inMs,
            endMs = clip.outMs.coerceAtMost(sourceEndMs),
            count = MovieTimelineDisplay.thumbnailCount(widthDp),
            thumbnailEpoch = thumbnailEpoch,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (selected) 2.dp else 1.dp)
                .clip(RoundedCornerShape(7.dp))
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.68f))
                .padding(horizontal = 26.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                MovieTimelineDisplay.clipLabel(clipIndex, clip.durationMs),
                color = PixiText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${MovieTimelineDisplay.formatTimecode(clip.inMs)}–" +
                    MovieTimelineDisplay.formatTimecode(clip.outMs),
                color = PixiTextMuted,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(24.dp)
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.48f))
                .pointerInput(clip.id, "trim-in") {
                    var startValue = 0L
                    var accumulatedPx = 0f
                    detectDragGestures(
                        onDragStart = {
                            startValue = latestClip.value.inMs
                            accumulatedPx = 0f
                        },
                        onDragEnd = { latestSettled.value() },
                        onDragCancel = { latestSettled.value() },
                        onDrag = { change, drag ->
                            change.consume()
                            accumulatedPx += drag.x
                            val current = latestClip.value
                            val dragWidthDp = MovieTimelineDisplay.clipWidthDp(current.durationMs)
                            val sourceDeltaMs = (
                                accumulatedPx / density.density /
                                    dragWidthDp * current.sourceDurationMs
                                ).toLong()
                            latestTrimIn.value(startValue + sourceDeltaMs)
                        }
                    )
                }
                .testTag("timeline_trim_handle_in")
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(24.dp)
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.48f))
                .pointerInput(clip.id, sourceEndMs, "trim-out") {
                    var startValue = 0L
                    var accumulatedPx = 0f
                    detectDragGestures(
                        onDragStart = {
                            startValue = latestClip.value.outMs
                            accumulatedPx = 0f
                        },
                        onDragEnd = { latestSettled.value() },
                        onDragCancel = { latestSettled.value() },
                        onDrag = { change, drag ->
                            change.consume()
                            accumulatedPx += drag.x
                            val current = latestClip.value
                            val dragWidthDp = MovieTimelineDisplay.clipWidthDp(current.durationMs)
                            val sourceDeltaMs = (
                                accumulatedPx / density.density /
                                    dragWidthDp * current.sourceDurationMs
                                ).toLong()
                            latestTrimOut.value(startValue + sourceDeltaMs)
                        }
                    )
                }
                .testTag("timeline_trim_handle_out")
        )
    }
}

@Composable
private fun ClipFilmstrip(
    clipId: String,
    path: String,
    startMs: Long,
    endMs: Long,
    count: Int,
    thumbnailEpoch: Int,
    modifier: Modifier = Modifier
) {
    val ownedFrames = remember(path, clipId) {
        mutableStateOf<List<FilmstripFrame>>(emptyList())
    }
    LaunchedEffect(path, clipId, thumbnailEpoch) {
        var loaded: List<FilmstripFrame>? = null
        try {
            loaded = withContext(Dispatchers.IO) {
                Filmstrip.frames(path, count, startMs, endMs)
            }
            if (!isActive) return@LaunchedEffect
            val previous = ownedFrames.value
            ownedFrames.value = loaded
            loaded = null
            previous.recycleFilmstripFrames()
        } finally {
            loaded?.recycleFilmstripFrames()
        }
    }
    DisposableEffect(path, clipId) {
        onDispose {
            ownedFrames.value.recycleFilmstripFrames()
            ownedFrames.value = emptyList()
        }
    }
    Row(
        modifier = modifier
            .background(PixiSurface)
            .testTag("timeline_clip_thumbnail")
    ) {
        ownedFrames.value.forEach { frame ->
            if (!frame.bitmap.isRecycled) {
                Image(
                    bitmap = frame.bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

private fun List<FilmstripFrame>.recycleFilmstripFrames() {
    forEach { frame ->
        if (!frame.bitmap.isRecycled) frame.bitmap.recycle()
    }
}

@Composable
private fun KenBurnsPreview(path: String, talking: Boolean, durationMs: Int) {
    val transition = rememberInfiniteTransition(label = "kenburns")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs.coerceAtLeast(1000), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t"
    )
    val scale = MovieMotion.scale(t, talking)
    val pan = MovieMotion.pan(t, talking, 640f)
    AsyncImage(
        model = File(path),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = pan
            }
    )
}
