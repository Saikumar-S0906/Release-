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
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
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
import com.pixijoy.ai.ui.board.components.BoardLottie
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardMotionKey
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
import java.util.concurrent.atomic.AtomicBoolean
import java.io.File
import java.io.FileOutputStream
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.ui.input.pointer.pointerInput
import com.pixijoy.ai.ui.board.components.BoardEdgeActions
import com.pixijoy.ai.ui.board.components.BoardScreenHeader
import com.pixijoy.ai.ui.board.components.BoardSoftProgress
import com.pixijoy.ai.ui.board.components.BoardSoftPulse

/** Flow 13 session — sticker style / text + cutout / pack bitmaps across steps. */
internal object BoardFlow13Session {
    var styleIndex: Int = 0
    var toolTab: Int = 0
    var brushSize: Float = 0.4f
    var expressionIndex: Int = 0
    var overlayText: String = "Best Friends!"
    var fontIndex: Int = 0
    var colorIndex: Int = 5
    var packName: String = "My Stickers"
    var saveToMyStickers: Boolean = true
    var packCount: Int = 4

    @Volatile
    var cutoutBitmap: Bitmap? = null
        private set

    @Volatile
    var styleSourceBitmap: Bitmap? = null
        private set

    @Volatile
    var packBitmaps: List<Bitmap> = emptyList()
        private set

    fun setCutout(bitmap: Bitmap?, rememberAsStyleSource: Boolean = false) {
        val old = cutoutBitmap
        cutoutBitmap = bitmap
        if (old != null && old !== bitmap && old !== styleSourceBitmap && !packBitmaps.contains(old) && !old.isRecycled) {
            runCatching { old.recycle() }
        }
        if (rememberAsStyleSource && bitmap != null && !bitmap.isRecycled) {
            val prev = styleSourceBitmap
            styleSourceBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            if (prev != null && prev !== cutoutBitmap && !prev.isRecycled) {
                runCatching { prev.recycle() }
            }
        }
    }

    fun setPack(bitmaps: List<Bitmap>) {
        val old = packBitmaps
        packBitmaps = bitmaps
        packCount = bitmaps.size.coerceAtLeast(1)
        old.forEach { b ->
            if (b !== cutoutBitmap && !bitmaps.contains(b) && !b.isRecycled) {
                runCatching { b.recycle() }
            }
        }
    }

    @DrawableRes
    fun hero(): Int = when (styleIndex % 6) {
        1 -> BoardFlowArt.stickerHero
        2 -> BoardFlowArt.funnyHero
        3 -> BoardFlowArt.beautifulHero
        4 -> BoardFlowArt.creationThumb
        5 -> BoardFlowArt.differentHero
        else -> BoardFlowArt.stickerHero
    }
}

private val TextColors = listOf(
    Color.Black,
    Color(0xFFE53935),
    Color(0xFFFB8C00),
    Color(0xFFFDD835),
    Color(0xFF43A047),
    Color(0xFF1E88E5),
    Color(0xFF8E24AA),
    Color(0xFFEC407A)
)

/**
 * FLOW_13 — Create Stickers / Sticker & Emoji.
 * WORKS: cutoutTransparent / generateStickerPack / healAt erase; gallery + WhatsApp pack export.
 * SoftEdge offline / low-credits / safety overlays stay.
 * Board: flows/additional/REMATCH_13_Sticker.png (superseded cores archived under flows/_superseded/).
 */
@Composable
fun BoardFlow13Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    val pipeline = LocalBoardPipeline.current
    when (screenId) {
        "b13_hub", "b13_photo" -> SelectPhoto(
            onTab = tab,
            onCamera = pipeline.openMediaInput,
            onGallery = { onNavigate("b3_chooser") },
            onSample = { onNavigate("b13_cutout") },
            onBack = onBack
        )
        "b13_cutout" -> AiCutout(
            onDone = { onNavigate("b13_pack") },
            onOffline = { onNavigate("b13_offline") },
            onLowCredits = { onNavigate("b13_low_credits") },
            onBack = { onNavigate("b13_photo") }
        )
        "b13_refine" -> EditSticker(
            onNext = { onNavigate("b13_pack") },
            onBack = { onNavigate("b13_pack") }
        )
        "b13_express" -> AddExpression(
            onNext = { onNavigate("b13_pack") },
            onBack = { onNavigate("b13_refine") }
        )
        "b13_pack" -> CreatePack(
            onAddMore = { onNavigate("b13_photo") },
            onRefine = { onNavigate("b13_refine") },
            onBack = { onNavigate("b13_photo") }
        )
        "b13_preview" -> PreviewSave(
            onSave = { onNavigate("b13_share") },
            onBack = { onNavigate("b13_pack") }
        )
        "b13_share" -> ShareUse(
            onDone = onBack,
            onBack = { onNavigate("b13_preview") }
        )
        "b13_offline" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Offline,
            onPrimary = { onNavigate("b13_cutout") },
            onSecondary = { onNavigate("b13_photo") }
        )
        "b13_low_credits" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.LowCredits,
            onPrimary = { onNavigate("b23_dash") },
            onSecondary = { onNavigate("b13_photo") }
        )
        "b13_safety" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Safety,
            onPrimary = { onNavigate("b13_photo") },
            onSecondary = onBack
        )
        else -> SelectPhoto(
            onTab = tab,
            onCamera = pipeline.openMediaInput,
            onGallery = { onNavigate("b3_chooser") },
            onSample = { onNavigate("b13_cutout") },
            onBack = onBack
        )
    }
}

@Composable
private fun SelectPhoto(
    onTab: (BoardShellTab) -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onSample: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val recent = listOf(
        BoardFlowArt.stickerHero,
        BoardFlowArt.beautifulHero,
        BoardFlowArt.funnyHero,
        BoardFlowArt.creationThumb,
        R.drawable.board_c00_hero,
        R.drawable.board_c09_portrait
    )
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW13_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.board_f13_create_sticker),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        stringResource(R.string.flow13_select_title),
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Image(
                painter = painterResource(BoardFlowArt.stickerHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            com.pixijoy.ai.ui.board.BoardUseSampleButton(
                sampleRes = BoardFlowArt.stickerHero,
                onReady = onSample
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_f13_take_photo),
                onClick = onCamera
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_f13_choose_gallery),
                onClick = onGallery
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun AiCutout(
    onDone: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var progress by remember { mutableFloatStateOf(0f) }
    val cancelled = remember { AtomicBoolean(false) }
    fun requestCancel() {
        cancelled.set(true)
        onBack()
    }
    BackHandler(onBack = { requestCancel() })
    LaunchedEffect(Unit) {
        val app = context.applicationContext
        val progressJob = launch {
            while (progress < 0.92f) {
                delay(40)
                progress = (progress + 0.02f).coerceAtMost(0.92f)
            }
        }
        val cutout = withContext(Dispatchers.Default) {
            runCatching {
                val pipe = EntryPointAccessors.fromApplication(app, BoardAiEntryPoint::class.java)
                    .createExtraPipeline()
                val source = BoardMediaSession.decodeBitmap(app)
                    ?: BitmapFactory.decodeResource(app.resources, BoardFlowArt.stickerHero)
                if (source != null && !source.isRecycled) {
                    pipe.cutoutTransparent(source)
                } else {
                    null
                }
            }.getOrNull()
        }
        if (!isActive || cancelled.get()) {
            progressJob.cancel()
            return@LaunchedEffect
        }
        if (cutout != null) {
            BoardFlow13Session.setCutout(cutout)
        } else {
            // Soft fallback: keep prior cutout or leave null (preview uses hero art).
            val soft = withContext(Dispatchers.IO) {
                runCatching {
                    BitmapFactory.decodeResource(app.resources, BoardFlowArt.stickerHero)
                }.getOrNull()
            }
            if (soft != null) BoardFlow13Session.setCutout(soft)
        }
        progressJob.cancel()
        progress = 1f
        delay(200)
        if (!isActive || cancelled.get()) return@LaunchedEffect
        onDone()
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW13_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            BoardScreenHeader(stringResource(R.string.board_f13_ai_cutout), onBack)
            Text(stringResource(R.string.flow13_cut_remove), color = colors.textSecondary)
            BoardProcessingHero(progress = progress)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                CheckerboardPreview(
                    resId = BoardFlowArt.stickerHero,
                    bitmap = BoardFlow13Session.cutoutBitmap,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                stringResource(R.string.board_f13_ai_working),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            BoardEdgeActions(
                onOffline = onOffline,
                onLowCredits = null
            )
            TextButton(onClick = { requestCancel() }) {
                Text(stringResource(R.string.cancel), color = colors.primary)
            }
        }
    }
}

@Composable
private fun EditSticker(onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    var tool by remember { mutableIntStateOf(BoardFlow13Session.toolTab) }
    var style by remember { mutableIntStateOf(BoardFlow13Session.styleIndex) }
    var brushSize by remember { mutableFloatStateOf(BoardFlow13Session.brushSize) }
    var cutoutRev by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val styles = listOf(
        R.string.board_f13_style_original,
        R.string.board_f13_style_outline,
        R.string.board_f13_style_cartoon,
        R.string.board_f13_style_sketch,
        R.string.board_f13_style_3d,
        R.string.board_f13_style_watercolor
    )
    val previewBmp = remember(cutoutRev, BoardFlow13Session.cutoutBitmap) {
        BoardFlow13Session.cutoutBitmap
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            BoardScreenHeader(stringResource(R.string.board_f13_edit_sticker), onBack)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card))
                    .border(2.dp, colors.primary, RoundedCornerShape(radii.card))
            ) {
                CheckerboardPreview(
                    resId = BoardFlow13Session.hero(),
                    bitmap = previewBmp,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (tool == 1) {
                                Modifier.pointerInput(brushSize, busy, cutoutRev) {
                                    detectTapGestures { offset ->
                                        if (busy) return@detectTapGestures
                                        val src = BoardFlow13Session.cutoutBitmap ?: return@detectTapGestures
                                        if (src.isRecycled) return@detectTapGestures
                                        val nx = (offset.x / size.width).coerceIn(0f, 1f)
                                        val ny = (offset.y / size.height).coerceIn(0f, 1f)
                                        val radiusFrac = 0.04f + (brushSize.coerceIn(0.1f, 1f) - 0.1f) / 0.9f * 0.16f
                                        busy = true
                                        scope.launch {
                                            try {
                                                val pipe = EntryPointAccessors.fromApplication(
                                                    context.applicationContext,
                                                    BoardAiEntryPoint::class.java
                                                ).createExtraPipeline()
                                                val out = withContext(Dispatchers.Default) {
                                                    pipe.healAt(src, nx, ny, radiusFrac)
                                                }
                                                if (out !== src) {
                                                    BoardFlow13Session.setCutout(out)
                                                    cutoutRev++
                                                }
                                            } finally {
                                                busy = false
                                            }
                                        }
                                    }
                                }
                            } else {
                                Modifier
                            }
                        )
                )
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = colors.primary
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                listOf(
                    R.drawable.pj_ic_tune to R.string.flow13_adjust,
                    R.drawable.pj_ic_magic to R.string.board_f13_erase_restore
                ).forEachIndexed { i, (icon, label) ->
                    val on = i == tool
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (on) colors.primary else colors.surfaceMuted)
                            .clickable {
                                tool = i
                                BoardFlow13Session.toolTab = i
                            }
                            .padding(PixiSpacing.md),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(icon), null, tint = if (on) colors.onPrimary else colors.textSecondary)
                        Spacer(modifier = Modifier.size(PixiSpacing.sm))
                        Text(
                            stringResource(label),
                            color = if (on) colors.onPrimary else colors.textSecondary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            if (tool == 1) {
                Text(
                    stringResource(R.string.erase_manual_brush),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Text(
                    stringResource(R.string.board_f15_brush) + " ${(brushSize * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                Slider(
                    value = brushSize,
                    onValueChange = {
                        brushSize = it
                        BoardFlow13Session.brushSize = it
                    },
                    valueRange = 0.1f..1f
                )
                Text(
                    stringResource(R.string.board_f15_heal_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    styles.forEachIndexed { i, res ->
                        val on = i == style
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    style = i
                                    BoardFlow13Session.styleIndex = i
                                }
                        ) {
                            Image(
                                painter = painterResource(
                                    when (i % 4) {
                                        1 -> BoardFlowArt.funnyHero
                                        2 -> BoardFlowArt.beautifulHero
                                        3 -> BoardFlowArt.differentHero
                                        else -> BoardFlowArt.stickerHero
                                    }
                                ),
                                contentDescription = stringResource(res),
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(radii.md))
                                    .border(
                                        width = if (on) 2.dp else 0.dp,
                                        color = colors.primary,
                                        shape = RoundedCornerShape(radii.md)
                                    ),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                stringResource(res),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (on) colors.primary else colors.textSecondary
                            )
                        }
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f13_next), onClick = onNext)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun AddExpression(onNext: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var tool by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf(BoardFlow13Session.overlayText) }
    var font by remember { mutableIntStateOf(BoardFlow13Session.fontIndex) }
    var colorIdx by remember { mutableIntStateOf(BoardFlow13Session.colorIndex) }
    var expression by remember { mutableIntStateOf(BoardFlow13Session.expressionIndex) }
    val fonts = listOf(
        R.string.board_f13_font_default,
        R.string.board_f13_font_cute,
        R.string.board_f13_font_bold,
        R.string.board_f13_font_hand
    )
    val expressions = listOf(
        R.string.board_f13_expr_happy,
        R.string.board_f13_expr_love,
        R.string.board_f13_expr_cool,
        R.string.board_f13_expr_surprised,
        R.string.board_f13_expr_sad,
        R.string.board_f13_expr_celebrate
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            ScreenHeader(stringResource(R.string.flow13_expression_title), onBack)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                CheckerboardPreview(
                    resId = BoardFlow13Session.hero(),
                    bitmap = BoardFlow13Session.cutoutBitmap,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text.ifBlank { " " },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(PixiSpacing.lg),
                    style = TextStyle(
                        color = TextColors[colorIdx % TextColors.size],
                        fontSize = 28.sp,
                        fontWeight = if (font == 2) FontWeight.Black else FontWeight.Bold,
                        fontFamily = if (font == 3) FontFamily.Cursive else FontFamily.Default,
                        shadow = Shadow(Color.White, blurRadius = 4f),
                        textAlign = TextAlign.Center
                    )
                )
                if (expression == 1) {
                    Text("❤️", modifier = Modifier.align(Alignment.TopEnd).padding(PixiSpacing.md), fontSize = 28.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    R.drawable.pj_ic_emoji to R.string.board_f13_tool_expr,
                    R.drawable.pj_ic_text to R.string.board_f13_tool_text,
                    R.drawable.pj_ic_sticker to R.string.board_f13_tool_stickers
                ).forEachIndexed { i, (icon, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { tool = i }
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
            when (tool) {
                0 -> Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    expressions.forEachIndexed { i, res ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(radii.pill))
                                .background(if (i == expression) colors.primary else colors.surfaceMuted)
                                .clickable {
                                    expression = i
                                    BoardFlow13Session.expressionIndex = i
                                }
                                .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                        ) {
                            Text(
                                stringResource(res),
                                color = if (i == expression) colors.onPrimary else colors.textSecondary
                            )
                        }
                    }
                }
                else -> {
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            BoardFlow13Session.overlayText = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (text.isNotEmpty()) {
                                IconButton(onClick = {
                                    text = ""
                                    BoardFlow13Session.overlayText = ""
                                }) {
                                    Icon(painter = painterResource(R.drawable.pj_ic_close), null)
                                }
                            }
                        },
                        shape = RoundedCornerShape(radii.md)
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        fonts.forEachIndexed { i, res ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(radii.pill))
                                    .background(if (i == font) colors.primarySoft else colors.surfaceMuted)
                                    .clickable {
                                        font = i
                                        BoardFlow13Session.fontIndex = i
                                    }
                                    .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                            ) {
                                Text(stringResource(res), color = colors.textPrimary)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                        TextColors.forEachIndexed { i, c ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (i == colorIdx) 2.dp else 0.dp,
                                        color = colors.primary,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        colorIdx = i
                                        BoardFlow13Session.colorIndex = i
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (i == colorIdx) Icon(painter = painterResource(R.drawable.pj_ic_check), null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f13_next), onClick = onNext)
        }
    }
}

@Composable
private fun CreatePack(
    onAddMore: () -> Unit,
    onRefine: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(BoardFlow13Session.packName) }
    var generating by remember { mutableStateOf(false) }
    var packRev by remember { mutableIntStateOf(0) }
    val fallbackItems = listOf(
        BoardFlowArt.stickerHero,
        BoardFlowArt.funnyHero,
        BoardFlowArt.beautifulHero,
        BoardFlowArt.creationThumb
    )
    suspend fun ensurePack() {
        if (BoardFlow13Session.packBitmaps.isNotEmpty()) return
        generating = true
        try {
            val app = context.applicationContext
            val source = BoardFlow13Session.cutoutBitmap?.takeIf { !it.isRecycled }
                ?: BoardMediaSession.decodeBitmap(app)
                ?: withContext(Dispatchers.IO) {
                    BitmapFactory.decodeResource(app.resources, BoardFlowArt.stickerHero)
                }
            if (source != null && !source.isRecycled) {
                val pack = withContext(Dispatchers.Default) {
                    EntryPointAccessors.fromApplication(app, BoardAiEntryPoint::class.java)
                        .createExtraPipeline()
                        .generateStickerPack(source)
                }
                if (pack.stickers.isNotEmpty()) {
                    BoardFlow13Session.setPack(pack.stickers)
                    packRev++
                }
            }
        } finally {
            generating = false
        }
    }
    LaunchedEffect(Unit) { ensurePack() }
    val pack = remember(packRev) { BoardFlow13Session.packBitmaps }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW13_PACK),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            BoardScreenHeader(stringResource(R.string.board_f13_create_pack), onBack)
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    BoardFlow13Session.packName = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.flow13_pack_title)) },
                trailingIcon = {
                    if (name.isNotEmpty()) {
                        IconButton(onClick = {
                            name = ""
                            BoardFlow13Session.packName = ""
                        }) { Icon(painter = painterResource(R.drawable.pj_ic_close), null) }
                    }
                },
                shape = RoundedCornerShape(radii.md)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                if (generating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
                val gridBitmaps = pack.take(4)
                val grid: List<Any?> = listOf(null) + if (gridBitmaps.isNotEmpty()) {
                    gridBitmaps
                } else {
                    fallbackItems
                }
                grid.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        row.forEach { cell ->
                            when (cell) {
                                null -> Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(radii.card))
                                        .background(colors.primarySoft)
                                        .clickable(onClick = onAddMore),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(painter = painterResource(R.drawable.pj_ic_add), null, tint = colors.primary)
                                        Text(
                                            stringResource(R.string.board_f13_add_more),
                                            color = colors.primary,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                is Bitmap -> BoardSafeResultImage(
                                    bitmap = cell,
                                    fallbackRes = BoardFlowArt.stickerHero,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(radii.card)),
                                    contentScale = ContentScale.Crop
                                )
                                is Int -> Image(
                                    painter = painterResource(cell),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(radii.card)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.add_to_whatsapp),
                onClick = {
                    scope.launch {
                        ensurePack()
                        val pack = BoardFlow13Session.packBitmaps
                        val ok = if (pack.isNotEmpty()) {
                            withContext(Dispatchers.IO) {
                                runCatching {
                                    EntryPointAccessors.fromApplication(
                                        context.applicationContext,
                                        BoardAiEntryPoint::class.java
                                    ).whatsAppStickerExporter().exportAndLaunchWhatsApp(pack)
                                }.getOrDefault(false)
                            }
                        } else {
                            false
                        }
                        if (!ok) shareDrawable(context, BoardFlow13Session.hero())
                    }
                }
            )
            BoardOutlinedButton(
                text = stringResource(R.string.flow13_save_pack),
                onClick = {
                    scope.launch {
                        ensurePack()
                        val ok = withContext(Dispatchers.IO) {
                            saveStickerBitmapsToGallery(context)
                        }
                        Toast.makeText(
                            context,
                            context.getString(
                                if (ok) R.string.board_f13_saved_toast else R.string.board_f13_save_failed
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_f13_edit_sticker),
                onClick = onRefine
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun PreviewSave(onSave: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    var saveMine by remember { mutableStateOf(BoardFlow13Session.saveToMyStickers) }
    val pack = BoardFlow13Session.packBitmaps
    val cutout = BoardFlow13Session.cutoutBitmap
    val fallbackItems = listOf(
        BoardFlowArt.stickerHero,
        BoardFlowArt.funnyHero,
        BoardFlowArt.beautifulHero,
        BoardFlowArt.creationThumb,
        BoardFlowArt.differentHero,
        BoardFlowArt.collageHero,
        BoardFlowArt.editorHero,
        BoardFlowArt.baAfter
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            BoardScreenHeader(stringResource(R.string.flow13_preview_title), onBack)
            BoardLottie(key = BoardMotionKey.StickerPop, size = 56.dp)
            if (pack.isNotEmpty()) {
                pack.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        row.forEach { bmp ->
                            BoardSafeResultImage(
                                bitmap = bmp,
                                fallbackRes = BoardFlowArt.stickerHero,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(radii.md)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            } else if (cutout != null && !cutout.isRecycled) {
                BoardSafeResultImage(
                    bitmap = cutout,
                    fallbackRes = BoardFlow13Session.hero(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(radii.md)),
                    contentScale = ContentScale.Fit
                )
            } else {
                fallbackItems.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        row.forEach { res ->
                            Image(
                                painter = painterResource(res),
                                contentDescription = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(radii.md)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = saveMine,
                    onCheckedChange = {
                        saveMine = it
                        BoardFlow13Session.saveToMyStickers = it
                    }
                )
                Text(stringResource(R.string.board_f13_save_my_stickers), color = colors.textPrimary)
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f13_save),
                onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            saveStickerBitmapsToGallery(context)
                        }
                        Toast.makeText(
                            context,
                            context.getString(
                                if (ok) R.string.board_f13_saved_toast else R.string.board_f13_save_failed
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                        if (ok) onSave()
                    }
                }
            )
            TextButton(onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        saveStickerBitmapsToGallery(context)
                    }
                    onSave()
                }
            }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.flow13_save_png), color = colors.primary)
            }
        }
    }
}

@Composable
private fun ShareUse(onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    val targets = listOf(
        "WhatsApp", "Telegram", "Instagram", "Messenger", "Messages", "More"
    )
    val pack = BoardFlow13Session.packBitmaps
    val preview = pack.firstOrNull() ?: BoardFlow13Session.cutoutBitmap
    fun shareTarget(label: String) {
        scope.launch {
            if (label == "WhatsApp" && pack.isNotEmpty()) {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            BoardAiEntryPoint::class.java
                        ).whatsAppStickerExporter().exportAndLaunchWhatsApp(pack)
                    }.getOrDefault(false)
                }
                if (!ok) shareDrawable(context, BoardFlow13Session.hero())
            } else {
                shareDrawable(context, BoardFlow13Session.hero())
            }
        }
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            BoardScreenHeader(stringResource(R.string.board_f13_share_use), onBack)
            BoardSafeResultImage(
                bitmap = preview,
                fallbackRes = BoardFlow13Session.hero(),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.flow13_share_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.board_f13_share_body),
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            targets.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { label ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { shareTarget(label) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceMuted),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label.take(1), fontWeight = FontWeight.Bold, color = colors.primary)
                            }
                            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        }
                    }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f13_share_now),
                onClick = {
                    scope.launch {
                        if (pack.isNotEmpty()) {
                            val ok = withContext(Dispatchers.IO) {
                                runCatching {
                                    EntryPointAccessors.fromApplication(
                                        context.applicationContext,
                                        BoardAiEntryPoint::class.java
                                    ).whatsAppStickerExporter().exportAndLaunchWhatsApp(pack)
                                }.getOrDefault(false)
                            }
                            if (!ok) shareDrawable(context, BoardFlow13Session.hero())
                        } else {
                            shareDrawable(context, BoardFlow13Session.hero())
                        }
                        onDone()
                    }
                }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(PixiSpacing.sm))
                Text(stringResource(R.string.board_f13_saved_pixijoy), color = colors.success)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun CheckerboardPreview(
    @DrawableRes resId: Int,
    modifier: Modifier = Modifier,
    bitmap: Bitmap? = null
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cell = 16.dp.toPx()
            val light = Color(0xFFE8E8EE)
            val dark = Color(0xFFD0D0D8)
            val cols = (size.width / cell).toInt() + 1
            val rows = (size.height / cell).toInt() + 1
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    drawRect(
                        color = if ((r + c) % 2 == 0) light else dark,
                        topLeft = androidx.compose.ui.geometry.Offset(c * cell, r * cell),
                        size = androidx.compose.ui.geometry.Size(cell, cell)
                    )
                }
            }
        }
        BoardSafeResultImage(
            bitmap = bitmap,
            fallbackRes = resId,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}


private fun saveDrawableToGallery(context: Context, @DrawableRes resId: Int): Boolean =
    BoardDrawableIo.saveToGallery(context, resId, filePrefix = "PixiJoy_Sticker")

private fun saveStickerBitmapsToGallery(context: Context): Boolean {
    val pack = BoardFlow13Session.packBitmaps.filter { !it.isRecycled }
    if (pack.isNotEmpty()) {
        return pack.map { BoardDrawableIo.saveBitmapToGallery(context, it, filePrefix = "PixiJoy_Sticker") }
            .any { it }
    }
    val cut = BoardFlow13Session.cutoutBitmap
    if (cut != null && !cut.isRecycled) {
        return BoardDrawableIo.saveBitmapToGallery(context, cut, filePrefix = "PixiJoy_Sticker")
    }
    return saveDrawableToGallery(context, BoardFlow13Session.hero())
}

private fun shareDrawable(context: Context, @DrawableRes resId: Int) {
    BoardDrawableIo.shareAsync(
        context = context,
        resId = resId,
        textRes = R.string.board_f13_share_text,
        filePrefix = "pixijoy_sticker"
    )
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
