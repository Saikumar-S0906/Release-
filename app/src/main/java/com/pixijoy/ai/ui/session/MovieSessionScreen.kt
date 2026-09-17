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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import coil3.compose.AsyncImage
import com.pixijoy.ai.R
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
import com.pixijoy.movie.ClipMix
import com.pixijoy.movie.Filmstrip
import com.pixijoy.movie.FilmstripFrame
import com.pixijoy.movie.Media3MovieEngine
import com.pixijoy.movie.MovieEngine
import com.pixijoy.movie.MovieMotion
import com.pixijoy.movie.MovieSpeed
import com.pixijoy.movie.MovieTrim
import com.pixijoy.movie.Timeline
import com.pixijoy.movie.TimelineClip
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
    var timeline by remember {
        mutableStateOf(
            Timeline.decode(project.graph, paths.size) { i ->
                engine.durationMs(paths.getOrElse(i) { path })
            }
        )
    }
    var selectedId by remember { mutableStateOf(timeline.clips.firstOrNull()?.id) }
    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(if (still) duration else engine.durationMs(path).coerceAtLeast(1000)) }
    var speed by remember { mutableFloatStateOf(1f) }
    var playhead by remember { mutableFloatStateOf(0f) }
    var playing by remember { mutableStateOf(true) }
    var exporting by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf<String?>(null) }
    var showExport by remember { mutableStateOf(false) }
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
                when (val result = repo.importAudioBed(project.id, uri)) {
                    is AppResult.Ok -> {
                        timeline = timeline.copy(bed = AudioBed(fileName = File(result.value).name))
                    }
                    is AppResult.Err -> status = result.message
                }
            }
        }
    }
    val selected = timeline.clips.firstOrNull { it.id == selectedId } ?: timeline.clips.firstOrNull()
    val projectDir = remember(path) { File(path).parent }
    val bedFile = timeline.bed?.let { File(projectDir, it.fileName) }?.takeIf { it.exists() }

    val player = remember {
        if (still) null else ExoPlayer.Builder(context).build()
    }
    val bedPlayer = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(player) {
        onDispose { player?.release() }
    }
    DisposableEffect(bedPlayer) {
        onDispose { bedPlayer.release() }
    }
    LaunchedEffect(bedFile?.absolutePath, timeline.bed?.muted) {
        val file = bedFile
        val bed = timeline.bed
        if (file != null && bed != null && !bed.muted) {
            bedPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            bedPlayer.repeatMode = Player.REPEAT_MODE_ALL
            bedPlayer.prepare()
            bedPlayer.playWhenReady = playing
            bedPlayer.volume = bed.volume
        } else {
            bedPlayer.stop()
            bedPlayer.clearMediaItems()
        }
    }
    LaunchedEffect(paths.size) {
        if (!still && timeline.clips.size != paths.size) {
            timeline = Timeline.fromMedia(paths.size) { engine.durationMs(paths[it]) }
            selectedId = timeline.clips.firstOrNull()?.id
        }
    }
    LaunchedEffect(paths.joinToString(), timeline.encode(), player) {
        player?.apply {
            val items = timeline.clips.map { clip ->
                val p = paths.getOrElse(clip.mediaIndex) { path }
                MediaItem.Builder()
                    .setUri(Uri.fromFile(File(p)))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(clip.inMs)
                            .setEndPositionMs(clip.outMs)
                            .build()
                    )
                    .build()
            }
            setMediaItems(items.ifEmpty { listOf(MediaItem.fromUri(Uri.fromFile(File(path)))) })
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ALL
            setPlaybackSpeed(MovieSpeed.normalize(selected?.speed ?: speed))
        }
    }
    LaunchedEffect(speed) {
        player?.setPlaybackSpeed(MovieSpeed.normalize(speed))
    }
    LaunchedEffect(player, timeline.encode()) {
        val exo = player ?: return@LaunchedEffect
        while (true) {
            val clips = timeline.clips
            val idx = exo.currentMediaItemIndex.coerceIn(0, (clips.size - 1).coerceAtLeast(0))
            val before = clips.take(idx).sumOf { it.durationMs }
            val composition = before + exo.currentPosition
            val total = timeline.totalMs().coerceAtLeast(1)
            playhead = (composition / total.toFloat()).coerceIn(0f, 1f)
            val clip = clips.getOrNull(idx)
            if (clip != null) {
                exo.volume = if (clip.muted) 0f
                else ClipMix.gain(
                    exo.currentPosition,
                    clip.durationMs,
                    clip.volume,
                    clip.fadeInMs,
                    clip.fadeOutMs
                )
            }
            val bed = timeline.bed
            if (bed != null && !bed.muted) {
                bedPlayer.volume = ClipMix.gain(
                    composition,
                    total,
                    bed.volume,
                    bed.fadeInMs,
                    bed.fadeOutMs
                )
            }
            delay(50)
        }
    }
    LaunchedEffect(timeline, speed) {
        repo.saveGraph(project.id, timeline.encode())
    }

    fun runExport(gif: Boolean) {
        exporting = true
        progress = 0f
        player?.playWhenReady = false
        scope.launch {
            val result = engine.export(
                context = context,
                paths = paths,
                timeline = timeline,
                talking = talking,
                gif = gif,
                onProgress = { progress = it }
            )
            exporting = false
            status = when (result) {
                is AppResult.Ok -> context.getString(
                    if (gif) R.string.studio_export_gif_ok else R.string.studio_export_movie_ok
                )
                is AppResult.Err -> result.message
            }
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
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!still) {
                Text(stringResource(R.string.studio_scrub), color = PixiTextMuted)
                Slider(
                    value = playhead.coerceIn(0f, 1f),
                    onValueChange = {
                        playhead = it
                        val total = timeline.totalMs().coerceAtLeast(1)
                        val ms = (it * total).toLong()
                        timeline.windowSeek(ms)?.let { (i, local) ->
                            player?.seekTo(i, local)
                        }
                        val bedDur = bedPlayer.duration
                        if (bedDur > 0) bedPlayer.seekTo(ms % bedDur)
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
                        timeline = timeline.rippleTrimIn(id, inn)
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
                        timeline = timeline.rippleTrimOut(id, out, selectedSourceEnd)
                    },
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_trim_end")
                )
                TextButton(
                    onClick = {
                        playing = !playing
                        player?.playWhenReady = playing
                        bedPlayer.playWhenReady = playing
                    },
                    modifier = Modifier.testTag("movie_play")
                ) {
                    Text(
                        if (playing) stringResource(R.string.studio_pause) else stringResource(R.string.studio_play),
                        color = PixiText
                    )
                }
                TextButton(
                    onClick = {
                        val ph = (playhead * timeline.totalMs()).toLong()
                        timeline = timeline.split(ph)
                        selectedId = timeline.clips.firstOrNull()?.id
                    },
                    modifier = Modifier.testTag("movie_split")
                ) {
                    Text(stringResource(R.string.studio_split), color = PixiText)
                }
                TextButton(
                    onClick = {
                        val id = selected?.id ?: return@TextButton
                        timeline = timeline.update(id) { it.copy(muted = !it.muted) }
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
                        timeline = timeline.update(id) { it.copy(volume = ClipMix.volume(v)) }
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
                        timeline = timeline.update(id) { it.copy(fadeInMs = v.toLong().coerceAtLeast(0)) }
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
                        timeline = timeline.update(id) { it.copy(fadeOutMs = v.toLong().coerceAtLeast(0)) }
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
                            onTrimIn = { timeline = timeline.rippleTrimIn(clip.id, it) },
                            onTrimOut = { timeline = timeline.rippleTrimOut(clip.id, it, sourceEnd) }
                        )
                    }
                }
                TextButton(
                    onClick = {
                        val id = selected?.id ?: return@TextButton
                        timeline = timeline.remove(id)
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
                        player?.seekTo(idx, local)
                        val composition = timeline.startOf(clip.id) + local
                        val total = timeline.totalMs().coerceAtLeast(1)
                        playhead = (composition / total.toFloat()).coerceIn(0f, 1f)
                        val bedDur = bedPlayer.duration
                        if (bedDur > 0) bedPlayer.seekTo(composition % bedDur)
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
                        timeline = timeline.copy(bed = bed.copy(volume = ClipMix.volume(v)))
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
                    onValueChange = { timeline = timeline.copy(bed = bed.copy(fadeInMs = it.toLong().coerceAtLeast(0))) },
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
                    onValueChange = { timeline = timeline.copy(bed = bed.copy(fadeOutMs = it.toLong().coerceAtLeast(0))) },
                    valueRange = 0f..2000f,
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_bed_fade_out")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { timeline = timeline.copy(bed = bed.copy(muted = !bed.muted)) },
                        modifier = Modifier.testTag("movie_bed_mute")
                    ) {
                        Text(
                            if (bed.muted) stringResource(R.string.studio_unmute)
                            else stringResource(R.string.studio_mute),
                            color = PixiText
                        )
                    }
                    TextButton(
                        onClick = { timeline = timeline.copy(bed = null) },
                        modifier = Modifier.testTag("movie_bed_remove")
                    ) {
                        Text(stringResource(R.string.studio_remove_music), color = PixiText)
                    }
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
                    if (id != null) timeline = timeline.update(id) { clip -> clip.copy(speed = s) }
                },
                valueRange = 0.5f..2f,
                colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                modifier = Modifier.testTag("movie_speed")
            )
            status?.let {
                Text(it, color = PixiSuccess, modifier = Modifier.testTag("session_status"))
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
            onSelectFormat = { exportGif = it == "gif" },
            onQuality = {},
            onExport = {
                showExport = false
                runExport(gif = exportGif)
            },
            onDismiss = { if (!exporting) showExport = false }
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
