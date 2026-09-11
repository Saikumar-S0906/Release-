package com.pixijoy.ai.ui.board.create

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Filter
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
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
import com.pixijoy.ai.R
import com.pixijoy.ai.di.BoardAiEntryPoint
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardAddPhotoButton
import com.pixijoy.ai.ui.board.BoardCToolEngine
import com.pixijoy.ai.ui.board.BoardFeatureFlags
import com.pixijoy.ai.ui.board.BoardMediaSession
import com.pixijoy.ai.ui.board.BoardSafeResultImage
import com.pixijoy.ai.ui.board.BoardSessionPhoto
import com.pixijoy.ai.ui.board.BoardSoftGenerate
import com.pixijoy.ai.ui.board.BoardAuthReturn
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.BoardNavPrefs
import com.pixijoy.ai.ui.board.rememberBoardPhotoGate
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardBeforeAfter
import com.pixijoy.ai.ui.board.components.BoardCdn
import com.pixijoy.ai.ui.board.components.BoardColorAdjust
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardEdgeActions
import com.pixijoy.ai.ui.board.components.BoardEdgeKind
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardFlowChrome
import com.pixijoy.ai.ui.board.components.BoardGateScreen
import com.pixijoy.ai.ui.board.components.BoardGenerateBlock
import com.pixijoy.ai.ui.board.components.BoardHeroImage
import com.pixijoy.ai.ui.board.components.BoardLottie
import com.pixijoy.ai.ui.board.components.BoardMotionKey
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPreviewControls
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardRemoteImage
import com.pixijoy.ai.ui.board.components.BoardResultFooter
import com.pixijoy.ai.ui.board.components.BoardSecondaryButton
import com.pixijoy.ai.ui.board.components.BoardSoftEdgeGate
import com.pixijoy.ai.ui.board.components.BoardSoftProgress
import com.pixijoy.ai.ui.board.components.BoardSoftPulse
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.components.BoardWideFrame
import com.pixijoy.ai.ui.board.components.rememberBoardHaptics
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Remembers Beautiful style + toggles across Flow 5 screens. */
internal object BoardFlow5Session {
    enum class Style(
        @StringRes val titleRes: Int,
        @StringRes val descRes: Int,
        @DrawableRes val imageRes: Int,
        val recommended: Boolean = false
    ) {
        Auto(R.string.flow5_auto, R.string.board_f5_auto_desc, BoardFlowArt.beautifulHero, recommended = true),
        Portrait(R.string.flow5_portrait, R.string.board_f5_portrait_desc, R.drawable.board_c09_portrait),
        Vivid(R.string.flow5_vivid, R.string.board_f5_vivid_desc, R.drawable.board_c07_festival),
        Aesthetic(R.string.flow5_aesthetic, R.string.board_f5_aesthetic_desc, BoardFlowArt.trendPortrait),
        Professional(R.string.flow5_professional, R.string.board_f5_pro_desc, R.drawable.board_c07_studio),
        Creative(R.string.flow5_creative, R.string.board_f5_creative_desc, R.drawable.board_c07_scene)
    }

    var style: Style = Style.Auto
    var enhanceFace: Boolean = true
    var improveLighting: Boolean = true
    var colorBalance: Boolean = true
    var removeNoise: Boolean = true
    var resultVariant: Int = 0
    var sceneDescription: String? = null
    var tipTop: String? = null
    var tipBottom: String? = null
    var isCloudPowered: Boolean = false
    var lastUserMessage: String? = null
    var brightness: Float = BoardColorAdjust.Neutral
    var contrast: Float = BoardColorAdjust.Neutral
    var saturation: Float = BoardColorAdjust.Neutral
    var warmth: Float = BoardColorAdjust.Neutral
    var lookIndex: Int = 0
    var textOverlay: String = ""
    var stickerEmoji: String = ""
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

    fun afterImage(): Int = when (resultVariant % 4) {
        0 -> style.imageRes
        1 -> BoardFlowArt.baAfter
        2 -> BoardFlowArt.rematchBeautiful.getOrElse(1) { BoardFlowArt.beautifulHero }
        else -> BoardFlowArt.rematchBeautiful.getOrElse(2) { BoardFlowArt.baAfter }
    }

    fun previewColorFilter() = BoardColorAdjust.colorFilter(
        brightness = brightness,
        contrast = contrast,
        saturation = saturation,
        warmth = warmth,
        boostLighting = improveLighting,
        boostColor = colorBalance,
        softenNoise = removeNoise
    )

    fun bakeMatrix() = BoardColorAdjust.androidMatrix(
        brightness = brightness,
        contrast = contrast,
        saturation = saturation,
        warmth = warmth,
        boostLighting = improveLighting,
        boostColor = colorBalance,
        softenNoise = removeNoise
    )
}

private fun saveDrawableToGallery(context: Context, @DrawableRes resId: Int): Boolean {
    val bmp = BoardFlow5Session.resultBitmap
    if (bmp != null && !bmp.isRecycled) {
        return BoardDrawableIo.saveBitmapToGallery(context, bmp, filePrefix = "PixiJoy_Beautiful")
    }
    return BoardDrawableIo.saveToGallery(
        context,
        resId,
        filePrefix = "PixiJoy_Beautiful",
        colorMatrix = BoardFlow5Session.bakeMatrix()
    )
}

private fun shareDrawable(context: Context, @DrawableRes resId: Int) {
    BoardDrawableIo.shareBitmapAsync(
        context = context,
        bitmap = BoardFlow5Session.resultBitmap,
        fallbackRes = resId,
        text = context.getString(R.string.board_f5_share_text),
        filePrefix = "pixijoy_beautiful"
    )
}

/** Flow 5 — Make it Beautiful. Board: REMATCH_05_Make_it_Beautiful.png */
@Composable
fun BoardFlow5Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val pipeline = LocalBoardPipeline.current
    when (screenId) {
        "b5_choose", "b5_beautiful" -> EnhancementTypes(
            onContinue = { onNavigate("b5_adjust") },
            onBack = onBack,
            onSwitchIntent = { dest -> onNavigate(dest) }
        )
        "b5_adjust" -> AdjustSettings(
            onGenerate = { onNavigate("b5_processing") },
            onBack = { onNavigate("b5_choose") },
            onAdvanced = { onNavigate("b_c04_photo") },
            onRestore = { onNavigate("b_c05_photo") }
        )
        "b5_processing" -> Processing(
            onDone = { onNavigate("b5_result") },
            onCancel = onBack,
            onOffline = { onNavigate("b5_offline") },
            onLowCredits = { onNavigate("b5_low_credits") },
            onSafety = { onNavigate("b5_safety") }
        )
        "b5_result" -> ResultCompare(
            onSave = { onNavigate("b5_ready") },
            onShare = { onNavigate("b8_share") },
            onVariations = { onNavigate("b5_variations") },
            onEdit = { onNavigate("b15_open") },
            onExport = pipeline.openExport,
            onRemake = { onNavigate("b5_choose") },
            onPublish = {
                BoardAuthReturn.remember("b5_result")
                onNavigate("b8_publish")
            },
            onBack = { onNavigate("b5_adjust") },
            onClose = onBack
        )
        "b5_variations" -> MoreVariations(
            onPick = { onNavigate("b5_result") },
            onGenerateMore = { BoardFlow5Session.resultVariant++ },
            onBack = { onNavigate("b5_result") }
        )
        "b5_edit" -> EditFurther(
            onApply = { onNavigate("b5_result") },
            onBack = { onNavigate("b5_result") }
        )
        "b5_ready" -> ReadyActions(
            onCreateAnother = pipeline.openMediaInput,
            onRemix = { onNavigate("b5_choose") },
            onHome = onBack
        )
        "b5_offline" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Offline,
            onPrimary = { onNavigate("b5_processing") },
            onSecondary = { onNavigate("b5_adjust") }
        )
        "b5_low_credits" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.LowCredits,
            onPrimary = { onNavigate("b23_dash") },
            onSecondary = { onNavigate("b5_adjust") }
        )
        "b5_safety" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Safety,
            onPrimary = { onNavigate("b5_choose") },
            onSecondary = onBack
        )
        else -> EnhancementTypes(
            onContinue = { onNavigate("b5_adjust") },
            onBack = onBack,
            onSwitchIntent = { dest -> onNavigate(dest) }
        )
    }
}

@Composable
private fun EnhancementTypes(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onSwitchIntent: (String) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableStateOf(BoardFlow5Session.style) }
    var hasPhoto by remember { mutableStateOf(BoardMediaSession.hasPhoto()) }
    val continueWithPhoto = rememberBoardPhotoGate {
        hasPhoto = true
        BoardFlow5Session.style = selected
        onContinue()
    }
    BoardAdaptiveFrame {
        BoardFlowChrome(
            title = stringResource(R.string.make_it_beautiful),
            onBack = onBack,
            primaryLabel = stringResource(R.string.board_f4_continue),
            onPrimary = continueWithPhoto,
            modifier = Modifier.testTag(TestTags.FLOW5_BEAUTIFUL)
        ) {
            if (!hasPhoto) {
                com.pixijoy.ai.ui.board.BoardUseSampleButton(
                    sampleRes = BoardFlowArt.beautifulHero,
                    onReady = { hasPhoto = true }
                )
                BoardAddPhotoButton(onPicked = { hasPhoto = true })
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(radii.card))
                ) {
                    BoardSessionPhoto(fallbackRes = BoardFlowArt.beautifulHero)
                }
            }
            Text(stringResource(R.string.board_f5_subtitle), color = colors.textSecondary)
            BoardFlow5Session.Style.entries.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEach { style ->
                        val on = style == selected
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(radii.card))
                                .background(
                                    when {
                                        style.recommended -> colors.primary.copy(
                                            alpha = if (on) 1f else 0.88f
                                        )
                                        on -> colors.primarySoft
                                        else -> colors.surfaceMuted
                                    }
                                )
                                .border(
                                    width = if (on && !style.recommended) 2.dp else 0.dp,
                                    color = colors.primary,
                                    shape = RoundedCornerShape(radii.card)
                                )
                                .clickable {
                                    selected = style
                                    BoardFlow5Session.style = style
                                }
                                .padding(PixiSpacing.md)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.2f)
                                    .clip(RoundedCornerShape(radii.md))
                            ) {
                                Image(
                                    painter = painterResource(style.imageRes),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (style.recommended) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(radii.pill))
                                            .background(Color.White.copy(alpha = 0.9f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            stringResource(R.string.board_f5_recommended),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(PixiSpacing.sm))
                            Text(
                                stringResource(style.titleRes),
                                fontWeight = FontWeight.SemiBold,
                                color = if (style.recommended) Color.White else colors.textPrimary
                            )
                            Text(
                                stringResource(style.descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (style.recommended) {
                                    Color.White.copy(alpha = 0.85f)
                                } else {
                                    colors.textSecondary
                                }
                            )
                        }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            Text(
                stringResource(R.string.board_f5_switch_intent),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
            )
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf(
                    R.string.make_it_funny to "b6_choose",
                    R.string.board_f4_create_stickers to "b13_photo",
                    R.string.make_it_move to "b14_input",
                    R.string.make_it_different to "b_diff_choose",
                    R.string.board_f2_tell to "b7_sheet"
                ).forEach { (label, dest) ->
                    Text(
                        stringResource(label),
                        color = colors.primary,
                        modifier = Modifier
                            .padding(end = PixiSpacing.md)
                            .clickable { onSwitchIntent(dest) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustSettings(
    onGenerate: () -> Unit,
    onBack: () -> Unit,
    onAdvanced: () -> Unit,
    onRestore: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var face by remember { mutableStateOf(BoardFlow5Session.enhanceFace) }
    var lighting by remember { mutableStateOf(BoardFlow5Session.improveLighting) }
    var color by remember { mutableStateOf(BoardFlow5Session.colorBalance) }
    var noise by remember { mutableStateOf(BoardFlow5Session.removeNoise) }
    BoardWideFrame {
        BoardPreviewControls(
            preview = {
                BoardSessionPhoto(
                    fallbackRes = BoardFlowArt.beautifulHero,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(radii.card)),
                    colorFilter = BoardFlow5Session.previewColorFilter()
                )
            },
            controls = {
                Text(
                    stringResource(R.string.board_f5_adjust_title),
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title,
                    color = colors.textPrimary
                )
                Text(
                    stringResource(R.string.board_f5_live_grade_hint),
                    style = BoardType.caption,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(PixiSpacing.md))
                val toggles = listOf(
                    Triple(
                        R.string.flow5_face,
                        face,
                        { v: Boolean ->
                            face = v
                            BoardFlow5Session.enhanceFace = v
                        }
                    ),
                    Triple(
                        R.string.flow5_lighting,
                        lighting,
                        { v: Boolean ->
                            lighting = v
                            BoardFlow5Session.improveLighting = v
                        }
                    ),
                    Triple(
                        R.string.flow5_color,
                        color,
                        { v: Boolean ->
                            color = v
                            BoardFlow5Session.colorBalance = v
                        }
                    ),
                    Triple(
                        R.string.flow5_noise,
                        noise,
                        { v: Boolean ->
                            noise = v
                            BoardFlow5Session.removeNoise = v
                        }
                    )
                )
                toggles.forEach { (label, checked, onChange) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(radii.card))
                            .background(colors.surfaceMuted)
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(label), color = colors.textPrimary)
                            if (label == R.string.flow5_face && checked) {
                                Text(
                                    stringResource(R.string.board_f5_face_on_hint),
                                    style = BoardType.caption,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        Switch(checked = checked, onCheckedChange = onChange)
                    }
                    Spacer(modifier = Modifier.height(PixiSpacing.sm))
                }
                BoardGenerateBlock(credits = 1, onGenerate = onGenerate)
                BoardOutlinedButton(
                    text = stringResource(R.string.board_c5_advanced),
                    onClick = onAdvanced
                )
                BoardOutlinedButton(
                    text = stringResource(R.string.create_extra_restore),
                    onClick = onRestore
                )
                BoardSecondaryButton(text = stringResource(R.string.cd_back), onClick = onBack)
            }
        )
    }
}

@Composable
private fun Processing(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit,
    onSafety: () -> Unit
) {
    val colors = BoardTheme.colors
    val steps = listOf(
        R.string.board_f5_step_details,
        R.string.board_f5_step_lighting,
        R.string.board_f5_step_colors,
        R.string.board_f5_step_best
    )
    var doneCount by remember { mutableIntStateOf(0) }
    var pct by remember { mutableIntStateOf(8) }
    val context = LocalContext.current
    androidx.activity.compose.BackHandler(onBack = onCancel)
    LaunchedEffect(Unit) {
        BoardFlow5Session.setResult(null)
        val result = BoardSoftGenerate.run(context, BoardSoftGenerate.Intent.Beautiful)
        BoardFlow5Session.sceneDescription = result.sceneDescription
        BoardFlow5Session.tipTop = result.topText
        BoardFlow5Session.tipBottom = result.bottomText
        BoardFlow5Session.isCloudPowered = result.isCloudPowered
        BoardFlow5Session.lastUserMessage = result.userMessage
        result.bitmap?.let { BoardFlow5Session.setResult(it) }
        if (BoardFlow5Session.resultBitmap == null) {
            val enhanced = BoardCToolEngine.run(context) { pipe, src ->
                if (BoardFlow5Session.enhanceFace) pipe.enhanceFaceLook(src) else src
            }
            if (enhanced != null) BoardFlow5Session.setResult(enhanced)
        }
        steps.indices.forEach { i ->
            doneCount = i + 1
            pct = ((i + 1) * 100) / steps.size
        }
        onDone()
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.splashTop)
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoardSoftPulse {
                Box(contentAlignment = Alignment.Center) {
                    BoardLottie(
                        key = BoardMotionKey.GenerationProgress,
                        size = 120.dp
                    )
                    Text(
                        "$pct%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = BoardType.title
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            steps.forEachIndexed { i, res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = PixiSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    if (i < doneCount) {
                        Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success)
                    } else if (i == doneCount) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colors.primary
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color.White.copy(0.35f), CircleShape)
                        )
                    }
                    Text(
                        stringResource(res),
                        color = if (i <= doneCount) Color.White else Color.White.copy(0.45f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            BoardSoftProgress(progress = pct / 100f, darkSurface = true)
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            Text(
                stringResource(R.string.board_f5_processing_hint),
                color = Color.White.copy(0.7f),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            BoardEdgeActions(
                onOffline = onOffline,
                onLowCredits = onLowCredits,
                onSafety = onSafety,
                darkSurface = true
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardOutlinedButton(
                text = stringResource(R.string.board_f19_cancel),
                onClick = onCancel
            )
        }
    }
}

@Composable
private fun ResultCompare(
    onSave: () -> Unit,
    onShare: () -> Unit,
    onVariations: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onRemake: () -> Unit,
    onPublish: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    val haptics = rememberBoardHaptics()
    val styles = listOf(
        R.string.board_f5_chip_original to BoardFlowArt.baBefore,
        R.string.flow5_vivid to BoardFlowArt.baAfter,
        R.string.flow5_natural to BoardFlowArt.beautifulHero,
        R.string.board_f5_chip_bright to BoardFlowArt.rematchBeautiful.getOrElse(1) { BoardFlowArt.baAfter },
        R.string.board_f5_chip_dreamy to BoardFlowArt.rematchBeautiful.getOrElse(2) { BoardFlowArt.profileAvatar }
    )
    var chip by remember { mutableIntStateOf(1) }
    val after = if (chip == 0) BoardFlowArt.baBefore else styles[chip].second
    androidx.activity.compose.BackHandler(onBack = onBack)

    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_back),
                        contentDescription = stringResource(R.string.cd_back),
                        tint = colors.textPrimary
                    )
                }
                Text(
                    stringResource(R.string.board_f5_result_title),
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_close),
                        contentDescription = stringResource(R.string.close),
                        tint = colors.textPrimary
                    )
                }
            }
            BoardLottie(key = BoardMotionKey.SparkleSuccess, size = 48.dp)
            if (BoardFlow5Session.isCloudPowered) {
                Text(
                    stringResource(R.string.powered_by_gemini),
                    style = BoardType.caption,
                    color = colors.primary
                )
            } else if (BoardFeatureFlags.showDemoCaption) {
                Text(
                    stringResource(R.string.board_soft_preview_title),
                    style = BoardType.caption,
                    color = colors.textSecondary
                )
            }
            val faceBmp = BoardFlow5Session.resultBitmap
            val beforeBmp = remember {
                com.pixijoy.ai.ui.board.BoardMediaSession.decodeBitmap(context.applicationContext, maxSide = 1280)
            }
            if (faceBmp != null && !faceBmp.isRecycled && beforeBmp != null) {
                BoardBeforeAfter(
                    beforeRes = BoardFlowArt.baBefore,
                    afterRes = after,
                    beforeBitmap = beforeBmp,
                    afterBitmap = faceBmp
                )
            } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                if (faceBmp != null && !faceBmp.isRecycled) {
                    BoardSafeResultImage(
                        bitmap = faceBmp,
                        fallbackRes = after,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    BoardSessionPhoto(
                        fallbackRes = after,
                        colorFilter = BoardFlow5Session.previewColorFilter()
                    )
                }
            }
            }
            BoardFlow5Session.sceneDescription?.takeIf { it.isNotBlank() }?.let { scene ->
                Text(scene, style = BoardType.body, color = colors.textPrimary)
            }
            BoardFlow5Session.tipTop?.takeIf { it.isNotBlank() }?.let { tip ->
                Text(tip, style = BoardType.secondary, color = colors.textSecondary)
            }
            BoardFlow5Session.lastUserMessage?.takeIf { it.isNotBlank() }?.let { msg ->
                Text(msg, style = BoardType.caption, color = colors.textSecondary)
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                styles.forEachIndexed { i, (label, image) ->
                    val on = i == chip
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(72.dp)
                            .clickable { chip = i }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(radii.md))
                                .border(
                                    width = if (on) 2.dp else 0.dp,
                                    color = colors.primary,
                                    shape = RoundedCornerShape(radii.md)
                                )
                        ) {
                            Image(
                                painter = painterResource(image),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (on) colors.primary else colors.textSecondary
                        )
                    }
                }
            }
            BoardResultFooter(
                onSave = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            saveDrawableToGallery(context, after)
                        }
                        Toast.makeText(
                            context,
                            context.getString(
                                if (ok) R.string.board_f5_saved else R.string.board_f5_save_failed
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                        if (ok) {
                            BoardNavPrefs.markUserCreations(context)
                            haptics.success()
                            onSave()
                        }
                    }
                },
                onShare = {
                    shareDrawable(context, after)
                    onShare()
                },
                onEdit = onEdit,
                onExport = onExport,
                onRemake = onRemake,
                onPublish = onPublish
            )
            BoardSecondaryButton(
                text = stringResource(R.string.board_c5_variations),
                onClick = onVariations
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun MoreVariations(
    onPick: () -> Unit,
    onGenerateMore: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var variations by remember { mutableStateOf<List<com.pixijoy.ai.creative.ImageVariation>>(emptyList()) }
    fun load() {
        loading = true
        scope.launch {
            val src = BoardFlow5Session.resultBitmap
                ?: com.pixijoy.ai.ui.board.BoardMediaSession.decodeBitmap(context.applicationContext, maxSide = 1280)
            variations = if (src != null) {
                runCatching {
                    dagger.hilt.android.EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.pixijoy.ai.di.BoardAiEntryPoint::class.java
                    ).imageVariationGenerator().generate(src, count = 4)
                }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load() }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.board_f5_more_variations),
                fontWeight = FontWeight.Bold,
                style = BoardType.title,
                color = colors.textPrimary
            )
            if (loading) {
                Text(stringResource(R.string.board_f7_generating_title), color = colors.textSecondary)
            }
            variations.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEach { variation ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(radii.card))
                                .clickable {
                                    BoardFlow5Session.setResult(variation.bitmap)
                                    onPick()
                                }
                        ) {
                            Image(
                                bitmap = variation.bitmap.asImageBitmap(),
                                contentDescription = variation.label,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(radii.card)),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                variation.label,
                                modifier = Modifier.padding(top = PixiSpacing.xs),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            BoardPrimaryButton(
                text = stringResource(R.string.board_f5_generate_more),
                onClick = {
                    onGenerateMore()
                    load()
                }
            )
            BoardSecondaryButton(text = stringResource(R.string.cd_back), onClick = onBack)
        }
    }
}

@Composable
private fun EditFurther(onApply: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    var tool by remember { mutableIntStateOf(0) }
    var brightness by remember { mutableFloatStateOf(BoardFlow5Session.brightness) }
    var contrast by remember { mutableFloatStateOf(BoardFlow5Session.contrast) }
    var saturation by remember { mutableFloatStateOf(BoardFlow5Session.saturation) }
    var warmth by remember { mutableFloatStateOf(BoardFlow5Session.warmth) }
    var lookIndex by remember { mutableIntStateOf(BoardFlow5Session.lookIndex) }
    var textOverlay by remember { mutableStateOf(BoardFlow5Session.textOverlay) }
    var stickerEmoji by remember { mutableStateOf(BoardFlow5Session.stickerEmoji) }
    var applying by remember { mutableStateOf(false) }
    val gradeFilter = BoardColorAdjust.colorFilter(
        brightness = brightness,
        contrast = contrast,
        saturation = saturation,
        warmth = warmth,
        boostLighting = BoardFlow5Session.improveLighting,
        boostColor = BoardFlow5Session.colorBalance,
        softenNoise = BoardFlow5Session.removeNoise
    )
    val lookFilter = BoardColorAdjust.lookColorFilter(lookIndex)
    val liveFilter = lookFilter ?: gradeFilter
    val lookLabels = listOf(
        R.string.board_f15_filter_none,
        R.string.board_f15_filter_vivid,
        R.string.board_f15_filter_warm,
        R.string.board_f15_filter_cool,
        R.string.board_f15_filter_bw,
        R.string.board_f15_filter_vintage,
        R.string.board_f15_filter_cinema,
        R.string.board_f15_filter_dream
    )
    val emojiPack = listOf("😀", "😂", "😍", "😎", "🥳", "👍", "❤️", "👑", "🔥", "✨")
    val tools = listOf(
        R.drawable.pj_ic_tune to R.string.board_f5_tool_adjust,
        R.drawable.pj_ic_filter to R.string.board_f5_tool_filters,
        R.drawable.pj_ic_sparkles to R.string.board_f5_tool_effects,
        R.drawable.pj_ic_text to R.string.board_f5_tool_text,
        R.drawable.pj_ic_brightness to R.string.board_f5_tool_stickers
    )
    BoardAdaptiveFrame {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(BoardFlow5Session.afterImage()),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = liveFilter
                )
                if (textOverlay.isNotBlank()) {
                    Text(
                        textOverlay,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(PixiSpacing.lg),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(Color.Black, blurRadius = 8f),
                            textAlign = TextAlign.Center
                        )
                    )
                }
                if (stickerEmoji.isNotBlank()) {
                    Text(
                        stickerEmoji,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(PixiSpacing.md),
                        fontSize = 36.sp
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = radii.card, topEnd = radii.card))
                    .background(colors.surface)
                    .padding(PixiSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                Text(
                    stringResource(R.string.board_f5_edit_title),
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title,
                    color = colors.textPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    tools.forEachIndexed { i, (icon, label) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { tool = i }
                        ) {
                            Icon(painter = painterResource(icon),
                                contentDescription = null,
                                tint = if (i == tool) colors.primary else colors.textSecondary
                            )
                            Text(
                                stringResource(label),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (i == tool) colors.primary else colors.textSecondary
                            )
                        }
                    }
                }
                when (tool) {
                    0 -> {
                        SliderRow(R.string.board_f5_brightness, brightness) { brightness = it }
                        SliderRow(R.string.board_f5_contrast, contrast) { contrast = it }
                        SliderRow(R.string.board_f5_saturation, saturation) { saturation = it }
                        SliderRow(R.string.board_f5_warmth, warmth) { warmth = it }
                    }
                    1, 2 -> {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                        ) {
                            lookLabels.forEachIndexed { i, res ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(radii.pill))
                                        .background(if (i == lookIndex) colors.primarySoft else colors.surfaceMuted)
                                        .border(
                                            width = if (i == lookIndex) 1.dp else 0.dp,
                                            color = colors.primary,
                                            shape = RoundedCornerShape(radii.pill)
                                        )
                                        .clickable { lookIndex = i }
                                        .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                                ) {
                                    Text(stringResource(res), color = colors.textPrimary)
                                }
                            }
                        }
                    }
                    3 -> {
                        OutlinedTextField(
                            value = textOverlay,
                            onValueChange = { textOverlay = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.board_f5_tool_text)) },
                            singleLine = true,
                            shape = RoundedCornerShape(radii.md)
                        )
                    }
                    else -> {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                        ) {
                            emojiPack.forEach { emoji ->
                                Text(
                                    emoji,
                                    fontSize = 28.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(radii.md))
                                        .background(
                                            if (stickerEmoji.contains(emoji)) colors.primarySoft
                                            else colors.surfaceMuted
                                        )
                                        .clickable {
                                            stickerEmoji = if (stickerEmoji.contains(emoji)) {
                                                stickerEmoji.replace(emoji, "")
                                            } else {
                                                stickerEmoji + emoji
                                            }
                                        }
                                        .padding(PixiSpacing.sm)
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                    BoardOutlinedButton(
                        text = stringResource(R.string.board_f5_reset),
                        onClick = {
                            brightness = BoardColorAdjust.Neutral
                            contrast = BoardColorAdjust.Neutral
                            saturation = BoardColorAdjust.Neutral
                            warmth = BoardColorAdjust.Neutral
                            lookIndex = 0
                            textOverlay = ""
                            stickerEmoji = ""
                        },
                        modifier = Modifier.weight(1f)
                    )
                    BoardPrimaryButton(
                        text = stringResource(R.string.board_f5_apply),
                        onClick = {
                            if (applying) return@BoardPrimaryButton
                            applying = true
                            BoardFlow5Session.brightness = brightness
                            BoardFlow5Session.contrast = contrast
                            BoardFlow5Session.saturation = saturation
                            BoardFlow5Session.warmth = warmth
                            BoardFlow5Session.lookIndex = lookIndex
                            BoardFlow5Session.textOverlay = textOverlay
                            BoardFlow5Session.stickerEmoji = stickerEmoji
                            scope.launch {
                                try {
                                    val app = context.applicationContext
                                    var src = BoardFlow5Session.resultBitmap
                                        ?: BoardMediaSession.decodeBitmap(app)
                                        ?: BitmapFactory.decodeResource(
                                            app.resources,
                                            BoardFlow5Session.afterImage()
                                        )
                                    if (src != null && !src.isRecycled) {
                                        val pipe = runCatching {
                                            EntryPointAccessors.fromApplication(
                                                app,
                                                BoardAiEntryPoint::class.java
                                            ).createExtraPipeline()
                                        }.getOrNull()
                                        val baked = withContext(Dispatchers.Default) {
                                            var out: Bitmap = src
                                            if (lookIndex > 0 && pipe != null) {
                                                out = pipe.applyLook(out, lookIndex)
                                            }
                                            val matrix = BoardFlow5Session.bakeMatrix()
                                            val needsGrade =
                                                brightness != BoardColorAdjust.Neutral ||
                                                    contrast != BoardColorAdjust.Neutral ||
                                                    saturation != BoardColorAdjust.Neutral ||
                                                    warmth != BoardColorAdjust.Neutral ||
                                                    BoardFlow5Session.improveLighting ||
                                                    BoardFlow5Session.colorBalance ||
                                                    BoardFlow5Session.removeNoise
                                            if (needsGrade) {
                                                val graded = Bitmap.createBitmap(
                                                    out.width,
                                                    out.height,
                                                    Bitmap.Config.ARGB_8888
                                                )
                                                val paint = android.graphics.Paint(
                                                    android.graphics.Paint.ANTI_ALIAS_FLAG
                                                ).apply {
                                                    colorFilter =
                                                        android.graphics.ColorMatrixColorFilter(matrix)
                                                }
                                                android.graphics.Canvas(graded)
                                                    .drawBitmap(out, 0f, 0f, paint)
                                                if (out !== src && out !== graded && !out.isRecycled) {
                                                    runCatching { out.recycle() }
                                                }
                                                out = graded
                                            }
                                            if (pipe != null && textOverlay.isNotBlank()) {
                                                val withText = pipe.bakeTextOverlay(
                                                    out,
                                                    textOverlay,
                                                    colorArgb = android.graphics.Color.WHITE,
                                                    align = 1,
                                                    fontStyle = 0,
                                                    scale = 1.1f,
                                                    verticalBias = 0.5f
                                                )
                                                if (withText !== out && out !== src && !out.isRecycled) {
                                                    runCatching { out.recycle() }
                                                }
                                                out = withText
                                            }
                                            if (pipe != null && stickerEmoji.isNotBlank()) {
                                                val withEmoji = pipe.bakeTextOverlay(
                                                    out,
                                                    stickerEmoji,
                                                    colorArgb = android.graphics.Color.WHITE,
                                                    align = 2,
                                                    fontStyle = 0,
                                                    scale = 1.4f,
                                                    verticalBias = 0.14f
                                                )
                                                if (withEmoji !== out && out !== src && !out.isRecycled) {
                                                    runCatching { out.recycle() }
                                                }
                                                out = withEmoji
                                            }
                                            out
                                        }
                                        if (baked !== src) BoardFlow5Session.setResult(baked)
                                    }
                                    onApply()
                                } finally {
                                    applying = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                BoardSecondaryButton(text = stringResource(R.string.cd_back), onClick = onBack)
            }
        }
    }
}

@Composable
private fun SliderRow(@StringRes label: Int, value: Float, onChange: (Float) -> Unit) {
    val colors = BoardTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(label),
            modifier = Modifier.weight(0.35f),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodySmall
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..20f,
            modifier = Modifier.weight(0.65f)
        )
    }
}

@Composable
private fun ReadyActions(
    onCreateAnother: () -> Unit,
    onRemix: () -> Unit,
    onHome: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val scope = rememberCoroutineScope()
    val after = BoardFlow5Session.afterImage()
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(painter = painterResource(R.drawable.pj_ic_check_circle),
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Text(
                stringResource(R.string.board_f5_ready_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            BoardHeroImage(resId = after, aspectRatio = 1f)
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("WhatsApp", "Instagram", "Facebook", "More").forEach { label ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { shareDrawable(context, after) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceMuted),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label.take(1),
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                        }
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            BoardPrimaryButton(
                text = stringResource(R.string.board_f5_save_gallery),
                onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            saveDrawableToGallery(context, after)
                        }
                        Toast.makeText(
                            context,
                            context.getString(
                                if (ok) R.string.board_f5_saved else R.string.board_f5_save_failed
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardOutlinedButton(
                text = stringResource(R.string.share),
                onClick = { shareDrawable(context, after) }
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardSecondaryButton(
                text = stringResource(R.string.board_f2_remix),
                onClick = onRemix
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardOutlinedButton(
                text = stringResource(R.string.result_create_another),
                onClick = onCreateAnother
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardOutlinedButton(
                text = stringResource(R.string.cd_back),
                onClick = onHome
            )
        }
    }
}
