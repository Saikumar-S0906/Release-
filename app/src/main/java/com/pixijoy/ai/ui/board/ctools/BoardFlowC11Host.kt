package com.pixijoy.ai.ui.board.ctools

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardGateScreen
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardProcessingHero
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.delay
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import com.pixijoy.ai.ui.board.components.BoardEdgeActions
import com.pixijoy.ai.ui.board.components.BoardScreenHeader
import kotlinx.coroutines.launch

/** C11 session — soft talking-photo prefs (local demo only). */
internal object BoardFlowC11Session {
    var motionIndex: Int = 0
    var requestIndex: Int = 1
    var script: String = ""
    var shareIndex: Int = 0
    var gifBytes: ByteArray? = null
    var startScale: Float = 1.05f
    var endScale: Float = 1.22f
    var startPanX: Float = 0.42f
    var endPanX: Float = 0.58f
    var startPanY: Float = 0.5f
    var endPanY: Float = 0.5f

    var usedSoftFallback: Boolean = false
    @Volatile
    var resultBitmap: android.graphics.Bitmap? = null
        private set

    fun setResult(bitmap: android.graphics.Bitmap?) {
        val old = resultBitmap
        resultBitmap = bitmap
        if (old != null && old !== bitmap && !old.isRecycled) {
            runCatching { old.recycle() }
        }
    }
}

/**
 * C11 — Talking Photo.
 * Soft: simulated motion / lip-sync UI; no real video, TTS, or network generation.
 */
@Composable
fun BoardFlowC11Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    val step = BoardCTool.stepOf(screenId)
    when (step) {
        "intro", "hub", "" -> C11Intro(
            onStart = { onNavigate("b_c11_photo") },
            onBack = onBack,
            onTab = tab
        )
        "photo" -> C11SelectPhoto(
            onGenerate = { onNavigate("b_c11_options") },
            onBack = { onNavigate("b_c11_intro") }
        )
        "options" -> C11YourRequest(
            onGenerate = { onNavigate("b_c11_adjust") },
            onBack = { onNavigate("b_c11_photo") }
        )
        "adjust" -> C11Customize(
            onContinue = { onNavigate("b_c11_processing") },
            onBack = { onNavigate("b_c11_options") }
        )
        "processing" -> C11Processing(
            onDone = { onNavigate("b_c11_result") },
            onOffline = { onNavigate("b_c11_offline") },
            onLowCredits = { onNavigate("b_c11_low_credits") },
            onBack = { onNavigate("b_c11_adjust") }
        )
        "result" -> C11PreviewEdit(
            onSave = { onNavigate("b_c11_share") },
            onEditMore = { onNavigate("b_c11_adjust") },
            onBack = { onNavigate("b_c11_adjust") }
        )
        "share" -> C11SaveShare(
            onDone = { onNavigate("b_c11_variations") },
            onBack = { onNavigate("b_c11_result") }
        )
        "variations" -> C11WhatsNext(
            onMp4 = { onNavigate("b8_ready") },
            onGif = { onNavigate("b14_preview") },
            onShare = { onNavigate("b8_share") },
            onDone = onBack,
            onBack = { onNavigate("b_c11_share") }
        )
        "offline" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_offline_title),
            body = stringResource(R.string.board_ct_offline_body),
            primary = stringResource(R.string.board_f3_try_again),
            onPrimary = { onNavigate("b_c11_processing") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate("b_c11_adjust") }
        )
        "low_credits" -> BoardGateScreen(
            title = stringResource(R.string.board_f19_low_credits_title),
            body = stringResource(R.string.board_f19_low_credits_body),
            primary = stringResource(R.string.board_f19_get_credits),
            onPrimary = { onNavigate("b23_dash") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate("b_c11_adjust") }
        )
        "safety" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_safety_title),
            body = stringResource(R.string.board_f4_safety_body),
            primary = stringResource(R.string.board_f3_try_another),
            onPrimary = { onNavigate("b_c11_photo") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = onBack
        )
        else -> C11Intro(
            onStart = { onNavigate("b_c11_photo") },
            onBack = onBack,
            onTab = tab
        )
    }
}

@Composable
private fun C11Intro(
    onStart: () -> Unit,
    onBack: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardMainShell(selected = BoardShellTab.Home, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C11_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.create_extra_talking),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        stringResource(R.string.board_c11_slogan),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .clickable(onClick = onStart)
            ) {
                Image(
                    painter = painterResource(R.drawable.board_c11_talk),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(PixiSpacing.md)
                        .clip(RoundedCornerShape(radii.pill))
                        .background(colors.primary)
                        .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                ) {
                    Text(
                        stringResource(R.string.create_extra_talking),
                        color = colors.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(0.65f))
                            )
                        )
                        .padding(PixiSpacing.lg)
                ) {
                    Text(
                        stringResource(R.string.board_c11_tagline),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_c09_get_started),
                onClick = onStart,
                modifier = Modifier.testTag(TestTags.TOOL_CONTINUE)
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C11SelectPhoto(onGenerate: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var motion by remember { mutableIntStateOf(BoardFlowC11Session.motionIndex) }
    val motions = listOf(
        MotionChip(R.string.board_c11_blink, R.drawable.board_c11_talk),
        MotionChip(R.string.board_c11_smile, R.drawable.board_c09_portrait),
        MotionChip(R.string.board_c11_sing, R.drawable.board_b14_motion),
        MotionChip(R.string.board_c11_look, R.drawable.board_poster_move)
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C11_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c09_select_photo),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(R.drawable.board_c11_talk),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                motions.forEachIndexed { index, item ->
                    val on = motion == index
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            motion = index
                            BoardFlowC11Session.motionIndex = index
                        }
                    ) {
                        Image(
                            painter = painterResource(item.art),
                            contentDescription = null,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(radii.md))
                                .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md)) else Modifier),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            stringResource(item.label),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (on) colors.primary else colors.textSecondary
                        )
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f14_generate), onClick = onGenerate)
            BoardTrustFooter()
        }
    }
}

private data class MotionChip(@StringRes val label: Int, @DrawableRes val art: Int)

@Composable
private fun C11YourRequest(onGenerate: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC11Session.requestIndex) }
    val requests = listOf(
        RequestItem(R.string.board_f14_motion_blink, R.drawable.pj_ic_visibility),
        RequestItem(R.string.board_c11_speak_line, R.drawable.pj_ic_mic),
        RequestItem(R.string.board_c11_sing, R.drawable.pj_ic_play),
        RequestItem(R.string.board_c11_look, R.drawable.pj_ic_visibility)
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C11_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c08_mark_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            requests.chunked(2).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEachIndexed { col, item ->
                        val index = rowIndex * 2 + col
                        val on = selected == index
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(radii.card))
                                .background(if (on) colors.primary.copy(0.14f) else colors.surfaceMuted)
                                .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.card)) else Modifier)
                                .clickable {
                                    selected = index
                                    BoardFlowC11Session.requestIndex = index
                                }
                                .padding(PixiSpacing.lg),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(painter = painterResource(item.icon),
                                null,
                                tint = if (on) colors.primary else colors.textSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(PixiSpacing.sm))
                            Text(
                                stringResource(item.title),
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f14_generate), onClick = onGenerate)
            BoardTrustFooter()
        }
    }
}

private data class RequestItem(@StringRes val title: Int, @DrawableRes val icon: Int)

@Composable
private fun C11Customize(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val defaultScript = stringResource(R.string.board_c11_script_sample)
    var script by remember {
        mutableStateOf(BoardFlowC11Session.script.ifEmpty { "" })
    }
    LaunchedEffect(Unit) {
        if (script.isEmpty()) {
            script = defaultScript
            BoardFlowC11Session.script = defaultScript
        }
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C11_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c11_customize),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            OutlinedTextField(
                value = script,
                onValueChange = {
                    script = it
                    BoardFlowC11Session.script = it
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = RoundedCornerShape(radii.md),
                label = { Text(stringResource(R.string.board_ct_script_hint)) },
                trailingIcon = {
                    Icon(painter = painterResource(R.drawable.pj_ic_mic), contentDescription = null, tint = colors.primary)
                }
            )
            Text(
                stringResource(R.string.board_ct_tip_talking),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            var startZoom by remember { mutableFloatStateOf(BoardFlowC11Session.startScale) }
            var endZoom by remember { mutableFloatStateOf(BoardFlowC11Session.endScale) }
            var startPan by remember { mutableFloatStateOf(BoardFlowC11Session.startPanX) }
            var endPan by remember { mutableFloatStateOf(BoardFlowC11Session.endPanX) }
            Text(stringResource(R.string.board_kb_start_zoom), color = colors.textPrimary)
            Slider(
                value = startZoom,
                onValueChange = {
                    startZoom = it
                    BoardFlowC11Session.startScale = it
                },
                valueRange = 1.02f..1.8f
            )
            Text(stringResource(R.string.board_kb_end_zoom), color = colors.textPrimary)
            Slider(
                value = endZoom,
                onValueChange = {
                    endZoom = it
                    BoardFlowC11Session.endScale = it
                },
                valueRange = 1.05f..2.2f
            )
            Text(stringResource(R.string.board_kb_start_pan), color = colors.textPrimary)
            Slider(
                value = startPan,
                onValueChange = {
                    startPan = it
                    BoardFlowC11Session.startPanX = it
                },
                valueRange = 0f..1f
            )
            Text(stringResource(R.string.board_kb_end_pan), color = colors.textPrimary)
            Slider(
                value = endPan,
                onValueChange = {
                    endPan = it
                    BoardFlowC11Session.endPanX = it
                },
                valueRange = 0f..1f
            )
            BoardPrimaryButton(text = stringResource(R.string.board_c00_continue), onClick = onContinue)
            BoardOutlinedButton(text = stringResource(R.string.cancel), onClick = onBack)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C11Processing(
    onDone: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        BoardFlowC11Session.setResult(null)
        BoardFlowC11Session.gifBytes = null
        BoardFlowC11Session.usedSoftFallback = false
        progress = 0.2f
        val app = context.applicationContext
        val ok = withContext(Dispatchers.Default) {
            runCatching {
                val entry = EntryPointAccessors.fromApplication(app, BoardAiEntryPoint::class.java)
                val src = BoardMediaSession.decodeBitmap(app)
                    ?: decodeBoardDrawable(app, R.drawable.board_c11_talk, maxSidePx = 1280)
                    ?: return@runCatching false
                progress = 0.45f
                val frames = entry.talkingPhotoAnimator().animate(
                    src,
                    12,
                    startScale = BoardFlowC11Session.startScale,
                    endScale = BoardFlowC11Session.endScale,
                    startPanX = BoardFlowC11Session.startPanX,
                    startPanY = BoardFlowC11Session.startPanY,
                    endPanX = BoardFlowC11Session.endPanX,
                    endPanY = BoardFlowC11Session.endPanY
                )
                if (frames.isEmpty()) return@runCatching false
                progress = 0.7f
                val gif = entry.gifEncoder().encode(frames, frameDelayMs = 120)
                val poster = frames.first().copy(frames.first().config ?: Bitmap.Config.ARGB_8888, false)
                BoardFlowC11Session.gifBytes = gif
                BoardFlowC11Session.setResult(poster)
                true
            }.getOrDefault(false)
        }
        progress = 1f
        BoardFlowC11Session.usedSoftFallback = !ok
        delay(200)
        onDone()
    }
    BoardAdaptiveFrame {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.processingSurface)
                .statusBarsPadding()
                .testTag(TestTags.FLOW_C11_HOST)
                .padding(PixiSpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BoardProcessingHero(progress = progress, darkSurface = true)
                Spacer(modifier = Modifier.height(PixiSpacing.md))
                Text(
                    stringResource(R.string.board_c11_generating),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                BoardEdgeActions(
                    onOffline = onOffline,
                    onLowCredits = null,
                    darkSurface = true
                )
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.cd_back), color = Color.White.copy(0.8f))
                }
            }
        }
    }
}

@Composable
private fun C11PreviewEdit(
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onRemake: () -> Unit,
    onPublish: () -> Unit,
    onEditMore: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var scrub by remember { mutableFloatStateOf(0.35f) }
    val tools = listOf(
        R.drawable.pj_ic_cut to R.string.board_f14_trim,
        R.drawable.pj_ic_speed to R.string.board_f14_adjust_speed,
        R.drawable.pj_ic_text to R.string.board_f14_text,
        R.drawable.pj_ic_music to R.string.board_f14_music,
        R.drawable.pj_ic_sparkles to R.string.board_f14_filters
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C11_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c11_creating_magic),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
            ) {
                BoardSafeResultImage(
                    bitmap = BoardFlowC11Session.resultBitmap,
                    fallbackRes = R.drawable.board_c11_talk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_play), null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            if (BoardFlowC11Session.usedSoftFallback) {
                Text(
                    stringResource(R.string.board_c04_soft_fallback),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                stringResource(R.string.board_c11_timecode),
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelMedium
            )
            Slider(value = scrub, onValueChange = { scrub = it }, modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tools.forEach { (icon, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painter = painterResource(icon), null, tint = colors.primary)
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
            BoardResultFooter(
                onSave = {
                    val gif = BoardFlowC11Session.gifBytes
                    val poster = BoardFlowC11Session.resultBitmap
                    val ok = when {
                        gif != null && gif.isNotEmpty() ->
                            BoardDrawableIo.saveBytesToGallery(
                                context,
                                gif,
                                filePrefix = "PixiJoy_Talking",
                                mimeType = "image/gif",
                                extension = "gif"
                            )
                        poster != null && !poster.isRecycled ->
                            BoardDrawableIo.saveBitmapToGallery(
                                context,
                                poster,
                                filePrefix = "PixiJoy_Talking"
                            )
                        else -> BoardDrawableIo.saveToGallery(
                            context,
                            R.drawable.board_c11_talk,
                            filePrefix = "PixiJoy_Talking"
                        )
                    }
                    Toast.makeText(
                        context,
                        if (ok) R.string.board_c04_saved else R.string.board_c04_save_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onShare = onShare,
                onEdit = onEdit,
                onExport = onExport,
                onRemake = onRemake,
                onPublish = onPublish
            )
            BoardOutlinedButton(text = stringResource(R.string.board_c11_edit_more), onClick = onEditMore)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C11PreviewEdit(onSave: () -> Unit, onEditMore: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var scrub by remember { mutableFloatStateOf(0.35f) }
    val tools = listOf(
        R.drawable.pj_ic_cut to R.string.board_f14_trim,
        R.drawable.pj_ic_speed to R.string.board_f14_adjust_speed,
        R.drawable.pj_ic_text to R.string.board_f14_text,
        R.drawable.pj_ic_music to R.string.board_f14_music,
        R.drawable.pj_ic_sparkles to R.string.board_f14_filters
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C11_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c11_creating_magic),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
            ) {
                Image(
                    painter = painterResource(R.drawable.board_c11_talk),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_play), null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            Text(
                stringResource(R.string.board_c11_timecode),
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelMedium
            )
            Slider(value = scrub, onValueChange = { scrub = it }, modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tools.forEach { (icon, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painter = painterResource(icon), null, tint = colors.primary)
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
            BoardOutlinedButton(text = stringResource(R.string.board_c11_edit_more), onClick = onEditMore)
            BoardPrimaryButton(text = stringResource(R.string.board_f12_save), onClick = onSave)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C11SaveShare(onDone: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC11Session.shareIndex) }
    val options = listOf(
        R.string.flow14_save_mp4,
        R.string.flow14_save_gif,
        R.string.board_c11_share_social,
        R.string.board_c11_make_live,
        R.string.board_c11_add_music,
        R.string.board_f14_share_apps
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C11_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c08_share_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            options.forEachIndexed { index, res ->
                val on = selected == index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(if (on) colors.primary.copy(0.14f) else colors.surfaceMuted)
                        .clickable {
                            selected = index
                            BoardFlowC11Session.shareIndex = index
                        }
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(res),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (on) {
                        Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.done), onClick = onDone)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C11WhatsNext(
    onMp4: () -> Unit,
    onGif: () -> Unit,
    onShare: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val actions = listOf(
        R.string.board_c11_save_mp4 to onMp4,
        R.string.flow14_save_gif to onGif,
        R.string.board_c11_share_wa to onShare,
        R.string.board_c11_reels to onShare
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C11_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c04_whats_next),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            actions.forEach { (label, action) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(colors.primary.copy(0.12f))
                        .clickable(onClick = action)
                        .padding(PixiSpacing.lg)
                ) {
                    Text(
                        stringResource(label),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("WA", "IG", "TT", "•••").forEach { label ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceMuted)
                            .clickable(onClick = onShare),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.done), onClick = onDone)
            BoardTrustFooter()
        }
    }
}
