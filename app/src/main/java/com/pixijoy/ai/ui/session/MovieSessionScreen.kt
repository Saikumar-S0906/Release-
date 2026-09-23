package com.pixijoy.ai.ui.session

import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.pixijoy.ai.export.MovieExportWorker
import com.pixijoy.core.common.AppResult
import com.pixijoy.core.data.Project
import com.pixijoy.core.data.ProjectRepository
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
import com.pixijoy.movie.CompositionPreview
import com.pixijoy.movie.DuckPreset
import com.pixijoy.movie.Filmstrip
import com.pixijoy.movie.FilmstripFrame
import com.pixijoy.movie.Media3MovieEngine
import com.pixijoy.movie.MovieEngine
import com.pixijoy.movie.MovieExportSize
import com.pixijoy.movie.MovieFrame
import com.pixijoy.movie.MovieHdrMode
import com.pixijoy.movie.MovieMotion
import com.pixijoy.movie.MovieSpeed
import com.pixijoy.movie.MovieTrim
import com.pixijoy.movie.MultiTrackTimeline
import com.pixijoy.movie.Timeline
import com.pixijoy.movie.TimelineClip
import com.pixijoy.movie.TimelineTitle
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MovieSessionScreen(
    project: Project,
    toolLabel: String,
    toolKey: String,
    repo: ProjectRepository,
    engine: MovieEngine = remember { Media3MovieEngine() },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val paths = remember(project.media, project.coverPath) {
        project.media.map { it.path }.ifEmpty { listOf(project.coverPath) }
    }
    val path = paths.first()
    val talking = toolKey == "talking"
    val still = remember(path) { engine.isStill(path) }
    val duration = remember(path, talking, paths) {
        if (still) MovieTrim.stillDurationMs(talking)
        else paths.sumOf { engine.durationMs(it) }.coerceAtLeast(1000)
    }
    var multi by remember {
        mutableStateOf(
            (
                MultiTrackTimeline.decode(project.graph)
                    ?: MultiTrackTimeline.fromFlatTimeline(
                        Timeline.decode(project.graph, paths.size) { i ->
                            engine.durationMs(paths.getOrElse(i) { path })
                        }
                    )
                ).ensureDefaultTracks()
        )
    }
    var timeline by remember { mutableStateOf(multi.toFlatTimeline()) }
    fun commitTimeline(next: Timeline) {
        timeline = next
        multi = multi.withPrimaryFlat(next)
    }
    fun commitMulti(next: MultiTrackTimeline) {
        multi = next.ensureDefaultTracks()
        timeline = multi.toFlatTimeline()
    }
    var selectedId by remember { mutableStateOf(timeline.clips.firstOrNull()?.id) }
    var selectedTitleId by remember { mutableStateOf<String?>(null) }
    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(if (still) duration else engine.durationMs(path).coerceAtLeast(1000)) }
    var speed by remember { mutableFloatStateOf(1f) }
    var playhead by remember { mutableFloatStateOf(0f) }
    var playing by remember { mutableStateOf(true) }
    var exporting by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf<String?>(null) }
    var showExport by remember { mutableStateOf(false) }
    var exportSuccessUri by remember { mutableStateOf<String?>(null) }
    var exportGif by remember { mutableStateOf(false) }
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

    val compositionPreview = remember {
        if (still) null else CompositionPreview(context)
    }
    val player = compositionPreview?.player
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
            commitTimeline(
                Timeline.fromMedia(paths.size) { engine.durationMs(paths[it]) }
                    .copy(
                        bed = timeline.bed,
                        voice = timeline.voice,
                        extraStems = timeline.extraStems,
                        masterFx = timeline.masterFx
                    )
            )
            selectedId = timeline.clips.firstOrNull()?.id
        }
    }
    LaunchedEffect(paths.joinToString(), multi.encode(), compositionPreview) {
        compositionPreview?.bind(paths, multi, playing)
    }
    LaunchedEffect(player) {
        val exo = player ?: return@LaunchedEffect
        while (true) {
            val total = timeline.totalMs().coerceAtLeast(1)
            playhead = (exo.currentPosition / total.toFloat()).coerceIn(0f, 1f)
            delay(50)
        }
    }
    LaunchedEffect(multi, speed) {
        repo.saveGraph(project.id, multi.encode())
    }

    val workManager = remember { WorkManager.getInstance(context) }
    val exportInfos by workManager
        .getWorkInfosForUniqueWorkFlow(MovieExportWorker.uniqueName(project.id))
        .collectAsState(initial = emptyList())
    LaunchedEffect(exportInfos) {
        val info = exportInfos.firstOrNull() ?: return@LaunchedEffect
        when (info.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> {
                exporting = true
                progress = info.progress.getFloat(MovieExportWorker.KEY_PROGRESS, progress)
                val pct = (progress * 100f).toInt().coerceIn(0, 100)
                status = context.getString(R.string.studio_export_pct, pct)
            }
            WorkInfo.State.SUCCEEDED -> {
                if (exporting) {
                    exporting = false
                    exportSuccessUri = info.outputData.getString(MovieExportWorker.KEY_URI)
                    showExport = true
                    val gifOut = info.outputData.getBoolean(MovieExportWorker.KEY_GIF, false)
                    status = context.getString(
                        if (gifOut) R.string.studio_export_gif_ok else R.string.studio_export_movie_ok
                    )
                }
            }
            WorkInfo.State.FAILED -> {
                if (exporting) {
                    exporting = false
                    status = info.outputData.getString(MovieExportWorker.KEY_ERROR)
                        ?: context.getString(R.string.studio_export_failed)
                }
            }
            else -> Unit
        }
    }

    fun clipFps(): Float {
        val p = paths.getOrElse(selected?.mediaIndex ?: 0) { path }
        return engine.fps(p)
    }

    fun stepFrame(delta: Int) {
        playing = false
        player?.playWhenReady = false
        val total = timeline.totalMs().coerceAtLeast(1)
        val now = (playhead * total).toLong()
        val next = MovieFrame.step(now, delta, clipFps(), total)
        compositionPreview?.seekMs(next)
        playhead = (next / total.toFloat()).coerceIn(0f, 1f)
    }

    fun runExport(gif: Boolean) {
        player?.playWhenReady = false
        bedPlayer?.playWhenReady = false
        voicePlayer?.playWhenReady = false
        exporting = true
        progress = 0f
        status = context.getString(R.string.studio_export_pct, 0)
        exportSuccessUri = null
        scope.launch {
            repo.saveGraph(project.id, multi.encode())
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixiBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("movie_session")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack, modifier = Modifier.testTag("session_back")) {
                Text(stringResource(R.string.studio_back), color = PixiText)
            }
            Text(toolLabel, color = PixiText, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        val scrimMax = (LocalConfiguration.current.screenHeightDp * 0.42f).dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = scrimMax)
                .background(Color.Black.copy(alpha = 0.50f))
                .testTag("session_tool_scrim")
        ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (track.muted) PixiBg else PixiAccent)
                            .clickable {
                                commitMulti(multi.withTrackMuted(track.id, !track.muted))
                            }
                            .testTag("movie_track_${track.id}")
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
            val canRoll = run {
                val id = selected?.id ?: return@run false
                val i = timeline.clips.indexOfFirst { it.id == id }
                if (i < 0 || i >= timeline.clips.lastIndex) return@run false
                val left = timeline.clips[i]
                val right = timeline.clips[i + 1]
                left.mediaIndex == right.mediaIndex && left.outMs == right.inMs
            }
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
                        }
                    )
                },
                onSpeedRamp = { start, end ->
                    val id = selected?.id ?: return@MovieEditOpsPanel
                    speed = start
                    commitTimeline(timeline.update(id) { it.copy(speed = start, speedEnd = end) })
                }
            )
            MovieMotionPanel(
                selected = selected,
                onMotion = { motion ->
                    val id = selected?.id ?: return@MovieMotionPanel
                    commitTimeline(timeline.update(id) { it.copy(motion = motion) })
                }
            )
            MovieFramePanel(
                selected = selected,
                onFrame = { frame ->
                    val id = selected?.id ?: return@MovieFramePanel
                    commitTimeline(timeline.update(id) { it.copy(frame = frame) })
                }
            )
            MovieChromaPanel(
                selected = selected,
                onChroma = { chroma ->
                    val id = selected?.id ?: return@MovieChromaPanel
                    commitTimeline(timeline.update(id) { it.copy(chroma = chroma) })
                }
            )
            MovieSlowMoPanel(
                selected = selected,
                onSlowMo = { slowMo ->
                    val id = selected?.id ?: return@MovieSlowMoPanel
                    commitTimeline(timeline.update(id) { it.copy(slowMo = slowMo) })
                }
            )
            MovieHdrPanel(
                hdrMode = multi.hdrMode,
                onHdrMode = { next -> commitMulti(multi.copy(hdrMode = next)) }
            )
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
            MovieGradePanel(
                grade = multi.grade,
                onGrade = { next -> commitMulti(multi.copy(grade = next)) }
            )
            MovieAudioFinishPanel(
                bed = timeline.bed,
                hasVoice = timeline.voice != null,
                onApplyPreset = { preset ->
                    val bed = timeline.bed ?: return@MovieAudioFinishPanel
                    commitTimeline(timeline.copy(bed = preset.apply(bed)))
                },
                onBedChange = { bed -> commitTimeline(timeline.copy(bed = bed)) }
            )
            MovieLayoutPanel(
                layout = multi.layout,
                onLayout = { next -> commitMulti(multi.copy(layout = next)) }
            )
            if (!still) {
                Text(stringResource(R.string.studio_scrub), color = PixiTextMuted)
                Slider(
                    value = playhead.coerceIn(0f, 1f),
                    onValueChange = {
                        playhead = it
                        val total = timeline.totalMs().coerceAtLeast(1)
                        val ms = (it * total).toLong()
                        compositionPreview?.seekMs(ms)
                        val bedDur = bedPlayer?.duration ?: 0L
                        if (bedDur > 0) bedPlayer?.seekTo(ms % bedDur)
                        val voiceDur = voicePlayer?.duration ?: 0L
                        if (voiceDur > 0) voicePlayer?.seekTo(ms.coerceAtMost(voiceDur))
                    },
                    onValueChangeFinished = {
                        val total = timeline.totalMs().coerceAtLeast(1)
                        val snapped = MovieFrame.snap((playhead * total).toLong(), clipFps(), total)
                        compositionPreview?.seekMs(snapped)
                        playhead = (snapped / total.toFloat()).coerceIn(0f, 1f)
                    },
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_scrub")
                )
                val selectedSourceEnd = engine.durationMs(
                    paths.getOrElse(selected?.mediaIndex ?: 0) { path }
                ).coerceAtLeast(1000)
                Text(stringResource(R.string.studio_trim_start), color = PixiTextMuted)
                Slider(
                    value = ((selected?.inMs ?: startMs) / selectedSourceEnd.toFloat()).coerceIn(0f, 1f),
                    onValueChange = {
                        val id = selected?.id ?: return@Slider
                        val inn = (it * selectedSourceEnd).toLong()
                        startMs = inn
                        commitTimeline(timeline.rippleTrimIn(id, inn))
                    },
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_trim_start")
                )
                Text(stringResource(R.string.studio_trim_end), color = PixiTextMuted)
                Slider(
                    value = ((selected?.outMs ?: endMs) / selectedSourceEnd.toFloat()).coerceIn(0f, 1f),
                    onValueChange = {
                        val id = selected?.id ?: return@Slider
                        val out = (it * selectedSourceEnd).toLong()
                        endMs = out
                        commitTimeline(timeline.rippleTrimOut(id, out, selectedSourceEnd))
                    },
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_trim_end")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { stepFrame(-1) },
                        modifier = Modifier.testTag("movie_frame_prev")
                    ) {
                        Text(stringResource(R.string.studio_frame_prev), color = PixiText)
                    }
                    TextButton(
                        onClick = {
                            playing = !playing
                            player?.playWhenReady = playing
                            bedPlayer?.playWhenReady = playing
                            voicePlayer?.playWhenReady = playing
                        },
                        modifier = Modifier.testTag("movie_play")
                    ) {
                        Text(
                            if (playing) stringResource(R.string.studio_pause) else stringResource(R.string.studio_play),
                            color = PixiText
                        )
                    }
                    TextButton(
                        onClick = { stepFrame(1) },
                        modifier = Modifier.testTag("movie_frame_next")
                    ) {
                        Text(stringResource(R.string.studio_frame_next), color = PixiText)
                    }
                }
                TextButton(
                    onClick = {
                        val ph = (playhead * timeline.totalMs()).toLong()
                        commitTimeline(timeline.split(ph))
                        selectedId = timeline.clips.firstOrNull()?.id
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
                        commitTimeline(timeline.update(id) { it.copy(volume = ClipMix.volume(v)) })
                    },
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
                        commitTimeline(timeline.update(id) { it.copy(fadeInMs = v.toLong().coerceAtLeast(0)) })
                    },
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
                        commitTimeline(timeline.update(id) { it.copy(fadeOutMs = v.toLong().coerceAtLeast(0)) })
                    },
                    valueRange = 0f..2000f,
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_fade_out")
                )
                Text(
                    stringResource(R.string.studio_timeline_clips, timeline.clips.size),
                    color = PixiTextMuted,
                    modifier = Modifier.testTag("movie_timeline")
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("movie_timeline_strip"),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    timeline.clips.forEach { clip ->
                        val sourceEnd = engine.durationMs(paths.getOrElse(clip.mediaIndex) { path })
                        TimelineClipBlock(
                            clip = clip,
                            selected = clip.id == selected?.id,
                            sourceEndMs = sourceEnd,
                            onSelect = { selectedId = clip.id },
                            onTrimIn = { commitTimeline(timeline.rippleTrimIn(clip.id, it)) },
                            onTrimOut = { commitTimeline(timeline.rippleTrimOut(clip.id, it, sourceEnd)) }
                        )
                    }
                }
                TextButton(
                    onClick = {
                        val id = selected?.id ?: return@TextButton
                        commitTimeline(timeline.remove(id))
                        selectedId = timeline.clips.firstOrNull()?.id
                    },
                    modifier = Modifier.testTag("movie_delete_clip")
                ) {
                    Text(stringResource(R.string.studio_delete_clip), color = PixiText)
                }
                FilmstripRow(
                    path = paths.getOrElse(selected?.mediaIndex ?: 0) { path },
                    startMs = selected?.inMs ?: 0L,
                    endMs = selected?.outMs ?: Long.MAX_VALUE,
                    onScrub = { sourceMs ->
                        val clip = selected ?: return@FilmstripRow
                        val idx = timeline.clips.indexOfFirst { it.id == clip.id }
                        if (idx < 0) return@FilmstripRow
                        val local = (sourceMs - clip.inMs).coerceAtLeast(0)
                        val composition = timeline.startOf(clip.id) + local
                        compositionPreview?.seekMs(composition)
                        val total = timeline.totalMs().coerceAtLeast(1)
                        playhead = (composition / total.toFloat()).coerceIn(0f, 1f)
                        val bedDur = bedPlayer?.duration ?: 0L
                        if (bedDur > 0) bedPlayer?.seekTo(composition % bedDur)
                        val voiceDur = voicePlayer?.duration ?: 0L
                        if (voiceDur > 0) voicePlayer?.seekTo(composition.coerceAtMost(voiceDur))
                    }
                )
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
            } else {
                Text(
                    if (talking) stringResource(R.string.studio_talking_hint) else stringResource(R.string.studio_move_hint),
                    color = PixiTextMuted
                )
            }
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
                        commitTimeline(timeline.copy(bed = bed.copy(volume = ClipMix.volume(v))))
                    },
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
                    onValueChange = { commitTimeline(timeline.copy(bed = bed.copy(fadeInMs = it.toLong().coerceAtLeast(0)))) },
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
                    onValueChange = { commitTimeline(timeline.copy(bed = bed.copy(fadeOutMs = it.toLong().coerceAtLeast(0)))) },
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
                        commitTimeline(timeline.copy(bed = bed.copy(duck = ClipMix.volume(v))))
                    },
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
                        commitTimeline(timeline.copy(voice = voice.copy(volume = ClipMix.volume(v))))
                    },
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
                        commitTimeline(timeline.copy(voice = voice.copy(fadeInMs = it.toLong().coerceAtLeast(0))))
                    },
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
                        commitTimeline(timeline.copy(voice = voice.copy(fadeOutMs = it.toLong().coerceAtLeast(0))))
                    },
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
                        commitTimeline(timeline.copy(
                            extraStems = timeline.extraStems.map {
                                if (it.id == sfx.id) it.copy(volume = ClipMix.volume(v)) else it
                            }
                        ))
                    },
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
                onValueChange = { commitTimeline(timeline.copy(masterFx = fx.copy(lowGainDb = it))) },
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
                onValueChange = { commitTimeline(timeline.copy(masterFx = fx.copy(midGainDb = it))) },
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
                onValueChange = { commitTimeline(timeline.copy(masterFx = fx.copy(highGainDb = it))) },
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
                onValueChange = { commitTimeline(timeline.copy(masterFx = fx.copy(compThresholdDb = it))) },
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
                onValueChange = { commitTimeline(timeline.copy(masterFx = fx.copy(compRatio = it.coerceAtLeast(1f)))) },
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
            Text(
                stringResource(R.string.studio_speed, MovieSpeed.normalize(selected?.speed ?: speed)),
                color = PixiTextMuted
            )
            Slider(
                value = MovieSpeed.normalize(selected?.speed ?: speed),
                onValueChange = {
                    val s = MovieSpeed.normalize(it)
                    speed = s
                    val id = selected?.id
                    if (id != null) {
                        commitTimeline(timeline.update(id) { clip -> clip.copy(speed = s, speedEnd = s) })
                    }
                },
                valueRange = 0.5f..2f,
                colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                modifier = Modifier.testTag("movie_speed")
            )
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
            PixiPrimaryButton(
                text = stringResource(R.string.studio_export),
                onClick = {
                    exportGif = false
                    showExport = true
                },
                enabled = !exporting,
                modifier = Modifier.testTag("session_export")
            )
            if (still) {
                PixiSecondaryButton(
                    text = stringResource(R.string.studio_export_gif),
                    onClick = {
                        if (exporting) return@PixiSecondaryButton
                        runExport(gif = true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_export_gif")
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

@Composable
private fun TimelineClipBlock(
    clip: TimelineClip,
    selected: Boolean,
    sourceEndMs: Long,
    onSelect: () -> Unit,
    onTrimIn: (Long) -> Unit,
    onTrimOut: (Long) -> Unit
) {
    val density = LocalDensity.current
    val w = (clip.durationMs / 12f).coerceIn(64f, 220f)
    Row(
        modifier = Modifier
            .width(w.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PixiAccent else PixiSurface)
            .clickable(onClick = onSelect)
            .testTag("timeline_clip"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(48.dp)
                .background(PixiText.copy(alpha = 0.35f))
                .pointerInput(clip.id) {
                    var acc = 0L
                    detectDragGestures(
                        onDragStart = { acc = 0L },
                        onDrag = { change, drag ->
                            change.consume()
                            val dxDp = drag.x / density.density
                            acc += (dxDp * 12f * clip.speed).toLong()
                            onTrimIn(clip.inMs + acc)
                        }
                    )
                }
                .testTag("clip_handle_in")
        )
        Text(
            clip.id,
            color = PixiText,
            maxLines = 1,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
        )
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(48.dp)
                .background(PixiText.copy(alpha = 0.35f))
                .pointerInput(clip.id, sourceEndMs) {
                    var acc = 0L
                    detectDragGestures(
                        onDragStart = { acc = 0L },
                        onDrag = { change, drag ->
                            change.consume()
                            val dxDp = drag.x / density.density
                            acc += (dxDp * 12f * clip.speed).toLong()
                            onTrimOut(clip.outMs + acc)
                        }
                    )
                }
                .testTag("clip_handle_out")
        )
    }
}

@Composable
private fun FilmstripRow(
    path: String,
    startMs: Long,
    endMs: Long,
    onScrub: (Long) -> Unit
) {
    var thumbs by remember(path, startMs, endMs) { mutableStateOf<List<FilmstripFrame>>(emptyList()) }
    LaunchedEffect(path, startMs, endMs) {
        thumbs.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
        thumbs = withContext(Dispatchers.Default) { Filmstrip.frames(path, 10, startMs, endMs) }
    }
    DisposableEffect(path, startMs, endMs) {
        onDispose { thumbs.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() } }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .height(56.dp)
            .testTag("movie_filmstrip"),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        thumbs.forEach { frame ->
            if (!frame.bitmap.isRecycled) {
                Image(
                    bitmap = frame.bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .height(56.dp)
                        .width(84.dp)
                        .clickable { onScrub(frame.timeMs) }
                        .testTag("filmstrip_frame")
                )
            }
        }
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
