package com.pixijoy.ai.ui.session

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.pixijoy.movie.Filmstrip
import com.pixijoy.movie.Media3MovieEngine
import com.pixijoy.movie.MovieEngine
import com.pixijoy.movie.MovieMotion
import com.pixijoy.movie.MovieSpeed
import com.pixijoy.movie.MovieTrim
import com.pixijoy.movie.Timeline
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val selected = timeline.clips.firstOrNull { it.id == selectedId } ?: timeline.clips.firstOrNull()

    val player = remember {
        if (still) null else ExoPlayer.Builder(context).build()
    }
    DisposableEffect(player) {
        onDispose { player?.release() }
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
    LaunchedEffect(player, startMs, endMs, paths.size) {
        val exo = player ?: return@LaunchedEffect
        while (true) {
            val pos = exo.currentPosition
            val total = exo.duration.takeIf { it > 0 } ?: duration
            playhead = (pos / total.toFloat()).coerceIn(0f, 1f)
            if (paths.size == 1 && pos >= endMs) {
                exo.seekTo(startMs)
            }
            delay(200)
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
                timeline = timeline.update(selected?.id ?: "") {
                    it.copy(speed = MovieSpeed.normalize(speed), inMs = startMs, outMs = endMs)
                }.let { if (it.clips.isEmpty()) timeline else it },
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
                        player?.seekTo((it * duration).toLong())
                    },
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_scrub")
                )
                val firstDuration = engine.durationMs(path).coerceAtLeast(1000)
                Text(stringResource(R.string.studio_trim_start), color = PixiTextMuted)
                Slider(
                    value = (startMs / firstDuration.toFloat()).coerceIn(0f, 1f),
                    onValueChange = {
                        startMs = MovieTrim.normalize((it * firstDuration).toLong(), endMs, firstDuration).first
                    },
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_trim_start")
                )
                Text(stringResource(R.string.studio_trim_end), color = PixiTextMuted)
                Slider(
                    value = (endMs / firstDuration.toFloat()).coerceIn(0f, 1f),
                    onValueChange = {
                        endMs = MovieTrim.normalize(startMs, (it * firstDuration).toLong(), firstDuration).second
                    },
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_trim_end")
                )
                TextButton(
                    onClick = {
                        playing = !playing
                        player?.playWhenReady = playing
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
                    stringResource(R.string.studio_timeline_clips, timeline.clips.size),
                    color = PixiTextMuted,
                    modifier = Modifier.testTag("movie_timeline")
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
            Text(stringResource(R.string.studio_speed, MovieSpeed.normalize(speed)), color = PixiTextMuted)
            Slider(
                value = speed,
                onValueChange = { speed = MovieSpeed.normalize(it) },
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
