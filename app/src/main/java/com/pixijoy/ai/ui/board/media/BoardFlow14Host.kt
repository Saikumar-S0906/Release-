package com.pixijoy.ai.ui.board.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Filter
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.pixijoy.ai.domain.model.MusicMood
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardGateScreen
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardProcessingHero
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.theme.PixiSpacing
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import androidx.compose.ui.graphics.asImageBitmap
import com.pixijoy.ai.di.BoardAiEntryPoint
import com.pixijoy.ai.ui.board.BoardCToolEngine
import com.pixijoy.ai.ui.board.BoardMediaSession
import com.pixijoy.ai.ui.board.BoardSafeResultImage
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardEdgeKind
import com.pixijoy.ai.ui.board.components.BoardResultFooter
import com.pixijoy.ai.ui.board.components.BoardSoftEdgeGate
import com.pixijoy.ai.ui.board.components.decodeBoardDrawable
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Slider
import androidx.compose.ui.input.pointer.pointerInput
import com.pixijoy.ai.ui.board.components.BoardEdgeActions
import com.pixijoy.ai.ui.board.components.BoardScreenHeader
import com.pixijoy.ai.ui.board.components.BoardSoftProgress

/** Flow 14 session — motion / customize choices across steps. */
internal object BoardFlow14Session {
    var inputType: Int = 0
    var mediaIndex: Int = 0
    var category: Int = 0
    var motionIndex: Int = 0
    var durationIndex: Int = 1
    var speedIndex: Int = 1
    var editTool: Int = 0
    var filterIndex: Int = 0
    var caption: String = "Good Vibes Only!"
    var showStories: Boolean = false
    var startScale: Float = 1.05f
    var endScale: Float = 1.22f
    var startPanX: Float = 0.42f
    var endPanX: Float = 0.58f
    var startPanY: Float = 0.5f
    var endPanY: Float = 0.5f
    /** 0 = none, 1..4 = [com.pixijoy.ai.domain.model.MusicMood] ordinal + 1 */
    var musicMoodIndex: Int = 0
    var gifBytes: ByteArray? = null
    var reelPath: String? = null

    @Volatile
    var stillBitmap: Bitmap? = null
        private set

    fun setStill(bitmap: Bitmap?) {
        val old = stillBitmap
        stillBitmap = bitmap
        if (old != null && old !== bitmap && !old.isRecycled) {
            runCatching { old.recycle() }
        }
    }

    @DrawableRes
    fun hero(): Int = when (mediaIndex % 4) {
        1 -> BoardFlowArt.beautifulHero
        2 -> BoardFlowArt.funnyHero
        3 -> BoardFlowArt.creationThumb
        else -> BoardFlowArt.motionHero
    }
}

/**
 * FLOW_14 — Make it Move (GIF / Animation / Video).
 * Soft: simulated motion generation; local gallery save & share intents only.
 * Board: flows/additional/REMATCH_14_Move.png (superseded cores archived under flows/_superseded/).
 */
@Composable
fun BoardFlow14Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    val pipeline = LocalBoardPipeline.current
    when (screenId) {
        "b14_hub", "b14_input" -> SelectInput(
            onTab = tab,
            onPick = { type ->
                BoardFlow14Session.inputType = type
                onNavigate("b14_media")
            },
            onBack = onBack
        )
        "b14_media" -> ChooseMedia(
            onCamera = pipeline.openMediaInput,
            onContinue = { onNavigate("b14_motion") },
            onBack = { onNavigate("b14_input") }
        )
        "b14_motion" -> ChooseMotion(
            onContinue = { onNavigate("b14_customize") },
            onBack = { onNavigate("b14_media") }
        )
        "b14_customize" -> CustomizeAnimation(
            onGenerate = { onNavigate("b14_gen") },
            onBack = { onNavigate("b14_motion") }
        )
        "b14_gen" -> Generating(
            onDone = { onNavigate("b14_preview") },
            onOffline = { onNavigate("b14_offline") },
            onLowCredits = { onNavigate("b14_low_credits") },
            onBack = { onNavigate("b14_customize") }
        )
        "b14_preview" -> PreviewEdit(
            onContinue = { onNavigate("b14_save") },
            onBack = { onNavigate("b14_customize") }
        )
        "b14_save" -> SaveShare(
            onDone = onBack,
            onStories = { onNavigate("b14_stories") },
            onBack = { onNavigate("b14_preview") }
        )
        "b14_stories" -> EnjoyEverywhere(
            onDone = onBack,
            onBack = { onNavigate("b14_save") }
        )
        "b14_offline" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_offline_title),
            body = stringResource(R.string.board_ct_offline_body),
            primary = stringResource(R.string.board_f3_try_again),
            onPrimary = { onNavigate("b14_gen") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate("b14_customize") }
        )
        "b14_low_credits" -> BoardGateScreen(
            title = stringResource(R.string.board_f19_low_credits_title),
            body = stringResource(R.string.board_f19_low_credits_body),
            primary = stringResource(R.string.board_f19_get_credits),
            onPrimary = { onNavigate("b23_dash") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate("b14_customize") }
        )
        "b14_safety" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_safety_title),
            body = stringResource(R.string.board_f4_safety_body),
            primary = stringResource(R.string.board_f3_try_another),
            onPrimary = { onNavigate("b14_input") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = onBack
        )
        else -> SelectInput(
            onTab = tab,
            onPick = {
                BoardFlow14Session.inputType = it
                onNavigate("b14_media")
            },
            onBack = onBack
        )
    }
}

@Composable
private fun SelectInput(
    onTab: (BoardShellTab) -> Unit,
    onPick: (Int) -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val options = listOf(
        Triple(R.string.flow14_input_photo, R.string.board_f14_photo_sub, R.drawable.pj_ic_gallery),
        Triple(R.string.flow14_input_video, R.string.board_f14_video_sub, R.drawable.pj_ic_movie),
        Triple(R.string.flow14_input_multi, R.string.board_f14_multi_sub, R.drawable.pj_ic_sparkles)
    )
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW14_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            ScreenHeader(stringResource(R.string.flow14_input_title), onBack)
            Text(stringResource(R.string.board_m14_tip), color = colors.textSecondary)
            Image(
                painter = painterResource(BoardFlowArt.motionHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            options.forEachIndexed { i, (title, sub, icon) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(if (i == BoardFlow14Session.inputType) colors.primarySoft else colors.surfaceMuted)
                        .clickable { onPick(i) }
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Icon(painter = painterResource(icon), null, tint = colors.primary, modifier = Modifier.size(32.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(title), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text(stringResource(sub), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun ChooseMedia(
    onCamera: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlow14Session.mediaIndex) }
    val thumbs = listOf(
        BoardFlowArt.motionHero,
        BoardFlowArt.beautifulHero,
        BoardFlowArt.funnyHero,
        BoardFlowArt.creationThumb,
        BoardFlowArt.stickerHero,
        BoardFlowArt.collageHero
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            ScreenHeader(stringResource(R.string.board_f14_choose_media), onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                thumbs.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        row.forEachIndexed { local, res ->
                            val index = thumbs.indexOf(res)
                            Image(
                                painter = painterResource(res),
                                contentDescription = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(radii.md))
                                    .border(
                                        width = if (index == selected) 3.dp else 0.dp,
                                        color = colors.primary,
                                        shape = RoundedCornerShape(radii.md)
                                    )
                                    .clickable {
                                        selected = index
                                        BoardFlow14Session.mediaIndex = index
                                    },
                                contentScale = ContentScale.Crop
                            )
                        }
                        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                BoardOutlinedButton(
                    text = stringResource(R.string.board_f13_take_photo),
                    onClick = onCamera,
                    modifier = Modifier.weight(1f)
                )
                BoardOutlinedButton(
                    text = stringResource(R.string.gallery),
                    onClick = onContinue,
                    modifier = Modifier.weight(1f)
                )
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f13_next), onClick = onContinue)
        }
    }
}

@Composable
private fun ChooseMotion(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var category by remember { mutableIntStateOf(BoardFlow14Session.category) }
    var motion by remember { mutableIntStateOf(BoardFlow14Session.motionIndex) }
    val cats = listOf(
        R.string.board_f14_cat_popular,
        R.string.board_f14_cat_fun,
        R.string.board_f14_cat_realistic,
        R.string.board_f14_cat_creative
    )
    val styles = listOf(
        R.string.board_f14_motion_subtle to BoardFlowArt.motionHero,
        R.string.board_f14_motion_dance to BoardFlowArt.funnyHero,
        R.string.board_f14_motion_blink to BoardFlowArt.beautifulHero,
        R.string.board_f14_motion_zoom to BoardFlowArt.creationThumb,
        R.string.board_f14_motion_3d to BoardFlowArt.differentHero,
        R.string.board_f14_motion_cartoon to BoardFlowArt.stickerHero,
        R.string.board_f14_motion_cinema to BoardFlowArt.trendPortrait,
        R.string.board_f14_motion_crazy to BoardFlowArt.collageHero
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            ScreenHeader(stringResource(R.string.flow14_motion_title), onBack)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                cats.forEachIndexed { i, res ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (i == category) colors.primary else colors.surfaceMuted)
                            .clickable {
                                category = i
                                BoardFlow14Session.category = i
                            }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(res),
                            color = if (i == category) colors.onPrimary else colors.textSecondary
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                styles.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        row.forEach { (label, res) ->
                            val index = styles.indexOfFirst { it.first == label }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(radii.card))
                                    .border(
                                        width = if (index == motion) 2.dp else 0.dp,
                                        color = colors.primary,
                                        shape = RoundedCornerShape(radii.card)
                                    )
                                    .clickable {
                                        motion = index
                                        BoardFlow14Session.motionIndex = index
                                    }
                            ) {
                                Image(
                                    painter = painterResource(res),
                                    contentDescription = stringResource(label),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(topStart = radii.card, topEnd = radii.card)),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    stringResource(label),
                                    modifier = Modifier.padding(PixiSpacing.sm),
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f13_next), onClick = onContinue)
        }
    }
}

@Composable
private fun CustomizeAnimation(onGenerate: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var duration by remember { mutableIntStateOf(BoardFlow14Session.durationIndex) }
    var speed by remember { mutableIntStateOf(BoardFlow14Session.speedIndex) }
    var caption by remember { mutableStateOf(BoardFlow14Session.caption) }
    var music by remember { mutableIntStateOf(BoardFlow14Session.musicMoodIndex) }
    var startZoom by remember { mutableFloatStateOf(BoardFlow14Session.startScale) }
    var endZoom by remember { mutableFloatStateOf(BoardFlow14Session.endScale) }
    var startPan by remember { mutableFloatStateOf(BoardFlow14Session.startPanX) }
    var endPan by remember { mutableFloatStateOf(BoardFlow14Session.endPanX) }
    val durations = listOf("3s", "5s", "10s", "Custom")
    val speeds = listOf("0.5x", "1x", "1.5x", "2x")
    val moods = listOf(
        stringResource(R.string.board_f14_music_none),
        stringResource(R.string.board_f14_mood_birthday),
        stringResource(R.string.board_f14_mood_chill),
        stringResource(R.string.board_f14_mood_epic),
        stringResource(R.string.board_f14_mood_romantic)
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            ScreenHeader(stringResource(R.string.flow14_customize_title), onBack)
            Text(
                stringResource(R.string.create_hub_reels_strip),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Text(
                stringResource(R.string.create_hub_reels_layer_photo),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            BoardSafeResultImage(
                bitmap = BoardFlow14Session.stillBitmap,
                fallbackRes = BoardFlow14Session.hero(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(radii.card))
            )
            Text(
                stringResource(R.string.create_hub_reels_layer_caption),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            OutlinedTextField(
                value = caption,
                onValueChange = {
                    caption = it
                    BoardFlow14Session.caption = it
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(radii.md)
            )
            Text(
                stringResource(R.string.create_hub_reels_layer_music),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            ChipRow(moods, music) {
                music = it
                BoardFlow14Session.musicMoodIndex = it
            }
            Text(stringResource(R.string.flow14_duration), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            ChipRow(durations, duration) {
                duration = it
                BoardFlow14Session.durationIndex = it
            }
            Text(stringResource(R.string.flow14_speed), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            ChipRow(speeds, speed) {
                speed = it
                BoardFlow14Session.speedIndex = it
            }
            Text(stringResource(R.string.board_kb_start_zoom), color = colors.textPrimary)
            Slider(
                value = startZoom,
                onValueChange = {
                    startZoom = it
                    BoardFlow14Session.startScale = it
                },
                valueRange = 1.02f..1.8f
            )
            Text(stringResource(R.string.board_kb_end_zoom), color = colors.textPrimary)
            Slider(
                value = endZoom,
                onValueChange = {
                    endZoom = it
                    BoardFlow14Session.endScale = it
                },
                valueRange = 1.05f..2.2f
            )
            Text(stringResource(R.string.board_kb_start_pan), color = colors.textPrimary)
            Slider(
                value = startPan,
                onValueChange = {
                    startPan = it
                    BoardFlow14Session.startPanX = it
                },
                valueRange = 0f..1f
            )
            Text(stringResource(R.string.board_kb_end_pan), color = colors.textPrimary)
            Slider(
                value = endPan,
                onValueChange = {
                    endPan = it
                    BoardFlow14Session.endPanX = it
                },
                valueRange = 0f..1f
            )
            BoardPrimaryButton(text = stringResource(R.string.board_f14_generate), onClick = onGenerate)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun Generating(
    onDone: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    var progress by remember { mutableFloatStateOf(0f) }
    val step = when {
        progress < 0.25f -> 0
        progress < 0.5f -> 1
        progress < 0.85f -> 2
        else -> 3
    }
    val steps = listOf(
        R.string.board_f14_step_analyze,
        R.string.board_f14_step_motion,
        R.string.board_f14_step_generate,
        R.string.board_f14_step_finalize
    )
    val cancelled = remember { AtomicBoolean(false) }
    fun requestCancel() {
        cancelled.set(true)
        onBack()
    }
    BackHandler(onBack = { requestCancel() })
    LaunchedEffect(Unit) {
        val app = context.applicationContext
        BoardFlow14Session.gifBytes = null
        BoardFlow14Session.reelPath = null
        progress = 0.18f
        val encoded = withContext(Dispatchers.Default) {
            val entry = runCatching {
                EntryPointAccessors.fromApplication(app, BoardAiEntryPoint::class.java)
            }.getOrNull()
            val pipe = entry?.createExtraPipeline()
            val source = BoardMediaSession.decodeBitmap(app, maxSide = 960)
                ?: runCatching {
                    BitmapFactory.decodeResource(app.resources, BoardFlow14Session.hero())
                }.getOrNull()
            if (source == null) return@withContext null
            val lookIdx = when (BoardFlow14Session.filterIndex) {
                1 -> 1
                2 -> 6
                3 -> 7
                else -> 0
            }
            var frame = if (lookIdx > 0 && pipe != null) pipe.applyLook(source, lookIdx) else source
            if (BoardFlow14Session.caption.isNotBlank() && pipe != null) {
                frame = pipe.bakeMemeCaptions(frame, "", BoardFlow14Session.caption)
            }
            val animator = entry?.talkingPhotoAnimator()
            var frames = animator?.animate(
                bitmap = frame,
                frames = 12,
                startScale = BoardFlow14Session.startScale,
                endScale = BoardFlow14Session.endScale,
                startPanX = BoardFlow14Session.startPanX,
                startPanY = BoardFlow14Session.startPanY,
                endPanX = BoardFlow14Session.endPanX,
                endPanY = BoardFlow14Session.endPanY
            ).orEmpty()
            if (frames.isEmpty()) {
                frames = entry?.advancedEditEngine()?.buildTimelineFrames(
                    frame,
                    listOf(
                        com.pixijoy.ai.creative.EditKeyframe(0, exposure = -0.1f, contrast = 1f),
                        com.pixijoy.ai.creative.EditKeyframe(500, exposure = 0.15f, contrast = 1.1f)
                    ),
                    frameCount = 8
                ).orEmpty()
            }
            val poster = when {
                frames.isNotEmpty() ->
                    frames.first().copy(frames.first().config ?: Bitmap.Config.ARGB_8888, false)
                frame !== source && !frame.isRecycled -> frame
                else -> source
            }
            Triple(entry, frames, poster)
        }
        if (!isActive || cancelled.get()) return@LaunchedEffect
        progress = 0.48f
        if (encoded != null) {
            val (entry, frames, poster) = encoded
            BoardFlow14Session.setStill(poster)
            if (entry != null && frames.isNotEmpty()) {
                val baseSec = when (BoardFlow14Session.durationIndex) {
                    0 -> 3f
                    2 -> 10f
                    else -> 5f
                }
                val speedMul = when (BoardFlow14Session.speedIndex) {
                    0 -> 2f
                    2 -> 0.67f
                    3 -> 0.5f
                    else -> 1f
                }
                val durationSec = (baseSec * speedMul).coerceIn(2.5f, 12f)
                val frameDurationUs = ((durationSec * 1_000_000f) / frames.size)
                    .toLong()
                    .coerceIn(80_000L, 1_000_000L)
                val gifDelayMs = (frameDurationUs / 1000L).toInt().coerceIn(80, 1000)
                progress = 0.62f
                if (!isActive || cancelled.get()) return@LaunchedEffect
                withContext(Dispatchers.Default) {
                    runCatching {
                        BoardFlow14Session.gifBytes =
                            entry.gifEncoder().encode(frames, frameDelayMs = gifDelayMs)
                    }
                }
                if (!isActive || cancelled.get()) return@LaunchedEffect
                progress = 0.78f
                val mood = MusicMood.entries.getOrNull(BoardFlow14Session.musicMoodIndex - 1)
                withContext(Dispatchers.IO) {
                    runCatching {
                        val out = File(app.cacheDir, "pixijoy_reel_${System.currentTimeMillis()}.mp4")
                        entry.reelExporter().export(
                            frames = frames,
                            outputFile = out,
                            musicMood = mood,
                            frameDurationUs = frameDurationUs
                        )
                        if (out.exists() && out.length() > 0L) {
                            BoardFlow14Session.reelPath = out.absolutePath
                        }
                    }
                }
                if (!isActive || cancelled.get()) return@LaunchedEffect
                frames.forEach { bmp ->
                    if (bmp !== poster && !bmp.isRecycled) {
                        runCatching { bmp.recycle() }
                    }
                }
            }
        }
        progress = 1f
        delay(200)
        if (!isActive || cancelled.get()) return@LaunchedEffect
        onDone()
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW14_HOST),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ScreenHeader(stringResource(R.string.board_f14_creating_magic), onBack)
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            BoardProcessingHero(progress = progress)
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            steps.forEachIndexed { i, res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = PixiSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    if (i < step) {
                        Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success)
                    } else if (i == step) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceMuted)
                        )
                    }
                    Text(
                        stringResource(res),
                        color = if (i <= step) colors.textPrimary else colors.textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            TextButton(onClick = { requestCancel() }) {
                Text(stringResource(R.string.cancel), color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun PreviewEdit(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var tool by remember { mutableIntStateOf(BoardFlow14Session.editTool) }
    var filter by remember { mutableIntStateOf(BoardFlow14Session.filterIndex) }
    val tools = listOf(
        R.drawable.pj_ic_cut to R.string.board_f14_trim,
        R.drawable.pj_ic_speed to R.string.flow14_speed,
        R.drawable.pj_ic_text to R.string.board_f14_text,
        R.drawable.pj_ic_music to R.string.board_f14_music,
        R.drawable.pj_ic_filter to R.string.board_f14_filters
    )
    val filters = listOf(
        R.string.board_f13_style_original,
        R.string.board_f14_filter_vivid,
        R.string.board_f14_filter_cinema,
        R.string.board_f14_filter_cartoon
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            ScreenHeader(stringResource(R.string.board_f14_preview_edit), onBack)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                BoardSafeResultImage(
                    bitmap = BoardFlow14Session.stillBitmap,
                    fallbackRes = BoardFlow14Session.hero(),
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_play), null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tools.forEachIndexed { i, (icon, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            tool = i
                            BoardFlow14Session.editTool = i
                        }
                    ) {
                        Icon(painter = painterResource(icon), null, tint = if (i == tool) colors.primary else colors.textSecondary)
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (i == tool) colors.primary else colors.textSecondary
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                filters.forEachIndexed { i, res ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (i == filter) colors.primarySoft else colors.surfaceMuted)
                            .border(
                                width = if (i == filter) 1.dp else 0.dp,
                                color = colors.primary,
                                shape = RoundedCornerShape(radii.pill)
                            )
                            .clickable {
                                filter = i
                                BoardFlow14Session.filterIndex = i
                            }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                    ) {
                        Text(stringResource(res), color = colors.textPrimary)
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f13_next), onClick = onContinue)
        }
    }
}

@Composable
private fun SaveShare(
    onDone: () -> Unit,
    onStories: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW14_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            ScreenHeader(stringResource(R.string.board_m14_save_title), onBack = onBack)
            BoardSafeResultImage(
                bitmap = BoardFlow14Session.stillBitmap,
                fallbackRes = BoardFlow14Session.hero(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(radii.card))
            )
            Text(
                stringResource(R.string.flow14_ready_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            BoardPrimaryButton(
                text = stringResource(R.string.flow14_save_mp4),
                onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            val path = BoardFlow14Session.reelPath
                            if (!path.isNullOrBlank()) {
                                BoardDrawableIo.saveVideoToGallery(context, File(path))
                            } else {
                                val bmp = BoardFlow14Session.stillBitmap
                                if (bmp != null && !bmp.isRecycled) {
                                    BoardDrawableIo.saveBitmapToGallery(
                                        context,
                                        bmp,
                                        filePrefix = "PixiJoy_Motion"
                                    )
                                } else {
                                    saveDrawableToGallery(context, BoardFlow14Session.hero())
                                }
                            }
                        }
                        Toast.makeText(
                            context,
                            context.getString(
                                if (ok) R.string.board_f14_mp4_saved else R.string.board_f14_save_failed
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            BoardOutlinedButton(
                text = stringResource(R.string.flow14_save_gif),
                onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            val gif = BoardFlow14Session.gifBytes
                            if (gif != null && gif.isNotEmpty()) {
                                BoardDrawableIo.saveBytesToGallery(
                                    context,
                                    gif,
                                    filePrefix = "PixiJoy_Motion"
                                )
                            } else {
                                val bmp = BoardFlow14Session.stillBitmap
                                if (bmp != null && !bmp.isRecycled) {
                                    BoardDrawableIo.saveBitmapToGallery(
                                        context,
                                        bmp,
                                        filePrefix = "PixiJoy_Motion"
                                    )
                                } else {
                                    saveDrawableToGallery(context, BoardFlow14Session.hero())
                                }
                            }
                        }
                        Toast.makeText(
                            context,
                            context.getString(
                                if (ok) R.string.board_f14_gif_saved else R.string.board_f14_save_failed
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_f14_share_apps),
                onClick = { shareMotion(context) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("WA", "IG", "TT").forEach { label ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceMuted)
                            .clickable { shareMotion(context) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.flow14_share_stories),
                onClick = onStories
            )
            BoardPrimaryButton(text = stringResource(R.string.done), onClick = onDone)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun EnjoyEverywhere(onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW14_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenHeader(stringResource(R.string.board_f14_enjoy), onBack)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                Image(
                    painter = painterResource(BoardFlow14Session.hero()),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Text(
                    BoardFlow14Session.caption,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(PixiSpacing.lg),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(Color.Black, blurRadius = 8f),
                        textAlign = TextAlign.Center
                    )
                )
            }
            BoardPrimaryButton(
                text = stringResource(R.string.flow14_share_stories),
                onClick = {
                    shareDrawable(context, BoardFlow14Session.hero())
                    onDone()
                }
            )
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.done), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun ChipRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
    ) {
        labels.forEachIndexed { i, label ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(radii.pill))
                    .background(if (i == selected) colors.primary else colors.surfaceMuted)
                    .clickable { onSelect(i) }
                    .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
            ) {
                Text(
                    label,
                    color = if (i == selected) colors.onPrimary else colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
        }
        Text(
            title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary
        )
    }
}

private fun saveDrawableToGallery(context: Context, @DrawableRes resId: Int): Boolean {
    val bitmap = BitmapFactory.decodeResource(context.resources, resId) ?: return false
    val fileName = "PixiJoy_Move_${System.currentTimeMillis()}.png"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PixiJoyAI")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    return try {
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    } catch (_: Exception) {
        false
    }
}

private fun shareMotion(context: Context) {
    val gif = BoardFlow14Session.gifBytes
    val reel = BoardFlow14Session.reelPath
    try {
        val cache = File(context.cacheDir, "share")
        cache.mkdirs()
        val (file, mime) = when {
            !reel.isNullOrBlank() && File(reel).exists() -> {
                val out = File(cache, "pixijoy_reel_${System.currentTimeMillis()}.mp4")
                File(reel).copyTo(out, overwrite = true)
                out to "video/mp4"
            }
            gif != null && gif.isNotEmpty() -> {
                val out = File(cache, "pixijoy_move_${System.currentTimeMillis()}.gif")
                out.writeBytes(gif)
                out to "image/gif"
            }
            else -> {
                shareDrawable(context, BoardFlow14Session.hero())
                return
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, context.getString(R.string.board_f14_share_text))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                context.getString(R.string.share)
            )
        )
    } catch (_: Exception) {
        shareDrawable(context, BoardFlow14Session.hero())
    }
}

private fun shareDrawable(context: Context, @DrawableRes resId: Int) {
    val bitmap = BitmapFactory.decodeResource(context.resources, resId) ?: run {
        Toast.makeText(context, context.getString(R.string.board_f14_save_failed), Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val cache = File(context.cacheDir, "share")
        cache.mkdirs()
        val file = File(cache, "pixijoy_move_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, context.getString(R.string.board_f14_share_text))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                context.getString(R.string.share)
            )
        )
    } catch (_: Exception) {
        Toast.makeText(context, context.getString(R.string.board_f14_save_failed), Toast.LENGTH_SHORT).show()
    }
}
