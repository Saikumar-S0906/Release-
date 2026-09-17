package com.pixijoy.ai.ui.session

import android.net.Uri
import android.view.ViewGroup
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
import com.pixijoy.designsystem.PixiSuccess
import com.pixijoy.designsystem.PixiSurface
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.movie.Media3MovieEngine
import com.pixijoy.movie.MovieEngine
import com.pixijoy.movie.MovieTrim
import java.io.File
import kotlin.math.cos
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
    val path = project.source?.path ?: project.coverPath
    val talking = toolKey == "talking"
    val still = remember(path) { engine.isStill(path) }
    val duration = remember(path, talking) {
        if (still) MovieTrim.stillDurationMs(talking) else engine.durationMs(path).coerceAtLeast(1000)
    }
    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(duration) }
    var playhead by remember { mutableFloatStateOf(0f) }
    var playing by remember { mutableStateOf(true) }
    var exporting by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf<String?>(null) }

    val player = remember {
        if (still) null else ExoPlayer.Builder(context).build()
    }
    DisposableEffect(path, player) {
        player?.apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
        }
        onDispose { player?.release() }
    }
    LaunchedEffect(player, startMs, endMs) {
        val exo = player ?: return@LaunchedEffect
        while (true) {
            val pos = exo.currentPosition
            playhead = pos / duration.toFloat()
            if (pos >= endMs) {
                exo.seekTo(startMs)
            }
            delay(200)
        }
    }
    LaunchedEffect(startMs, endMs) {
        repo.saveGraph(project.id, "M,$startMs,$endMs")
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
                Text(stringResource(R.string.studio_trim_start), color = PixiTextMuted)
                Slider(
                    value = startMs / duration.toFloat(),
                    onValueChange = {
                        startMs = MovieTrim.normalize((it * duration).toLong(), endMs, duration).first
                    },
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("movie_trim_start")
                )
                Text(stringResource(R.string.studio_trim_end), color = PixiTextMuted)
                Slider(
                    value = endMs / duration.toFloat(),
                    onValueChange = {
                        endMs = MovieTrim.normalize(startMs, (it * duration).toLong(), duration).second
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
            } else {
                Text(
                    if (talking) stringResource(R.string.studio_talking_hint) else stringResource(R.string.studio_move_hint),
                    color = PixiTextMuted
                )
            }
            status?.let {
                Text(it, color = PixiSuccess, modifier = Modifier.testTag("session_status"))
            }
            PixiPrimaryButton(
                text = when {
                    exporting && progress > 0f -> context.getString(R.string.studio_export_pct, (progress * 100).toInt())
                    exporting -> stringResource(R.string.studio_exporting)
                    else -> stringResource(R.string.studio_export_mp4)
                },
                onClick = {
                    if (exporting) return@PixiPrimaryButton
                    exporting = true
                    progress = 0f
                    player?.playWhenReady = false
                    scope.launch {
                        val result = engine.export(
                            context = context,
                            path = path,
                            trimStartMs = startMs,
                            trimEndMs = endMs,
                            talking = talking,
                            onProgress = { progress = it }
                        )
                        exporting = false
                        status = when (result) {
                            is AppResult.Ok -> context.getString(R.string.studio_export_movie_ok)
                            is AppResult.Err -> result.message
                        }
                    }
                },
                enabled = !exporting,
                modifier = Modifier.testTag("session_export")
            )
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
    val scale = if (talking) {
        1f + 0.08f * (0.5f - 0.5f * cos((t * 2f * Math.PI).toFloat()))
    } else {
        1f + 0.18f * t
    }
    val pan = if (talking) 0f else (t - 0.5f) * 80f
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
