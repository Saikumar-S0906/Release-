package com.pixijoy.ai.ui.board.ctools

import android.graphics.Bitmap
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardAddPhotoButton
import com.pixijoy.ai.ui.board.BoardCToolEngine
import com.pixijoy.ai.ui.board.BoardMediaSession
import com.pixijoy.ai.ui.board.BoardSafeResultImage
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardBeforeAfter
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardEdgeActions
import com.pixijoy.ai.ui.board.components.BoardEdgeKind
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardProcessingHero
import com.pixijoy.ai.ui.board.components.BoardResultFooter
import com.pixijoy.ai.ui.board.components.BoardSoftEdgeGate
import androidx.activity.compose.BackHandler
import com.pixijoy.ai.ui.board.components.BoardTertiaryButton
import com.pixijoy.ai.ui.board.toastBoardSaveResult
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import com.pixijoy.ai.ui.board.components.BoardScreenHeader

/** C05 session — restore prefs + last processed bitmap. */
internal object BoardFlowC05Session {
    var optionIndex: Int = 0
    var variationIndex: Int = 1
    var filterIndex: Int = 2
    var brightness: Float = 0.7f
    var contrast: Float = 0.5f
    var saturation: Float = 0.65f
    var warmth: Float = 0.4f
    var faceEnhance: Boolean = true
    var usedSoftFallback: Boolean = false
    @Volatile
    var beforeBitmap: Bitmap? = null
        private set
    @Volatile
    var resultBitmap: Bitmap? = null
        private set

    fun setBefore(bitmap: Bitmap?) {
        val old = beforeBitmap
        beforeBitmap = bitmap
        if (old != null && old !== bitmap && !old.isRecycled) {
            runCatching { old.recycle() }
        }
    }

    fun setResult(bitmap: Bitmap?) {
        val old = resultBitmap
        resultBitmap = bitmap
        if (old != null && old !== bitmap && !old.isRecycled) {
            runCatching { old.recycle() }
        }
    }
}

/**
 * C05 — Restore Old Photo.
 * Real [CreateExtraPipeline.restoreChain] when a photo is in [BoardMediaSession].
 */
@Composable
fun BoardFlowC05Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val step = BoardCTool.stepOf(screenId)
    when (step) {
        "intro", "hub", "" -> C05Landing(
            onContinue = { onNavigate("b_c05_photo") },
            onBack = onBack
        )
        "photo" -> C05PickPhoto(
            onSample = { onNavigate("b_c05_processing") },
            onGallery = { onNavigate("b_c05_processing") },
            onBack = { onNavigate("b_c05") }
        )
        "options" -> C05ChooseType(
            onGenerate = { onNavigate("b_c05_processing") },
            onBack = {
                if (BoardFlowC05Session.resultBitmap != null) onNavigate("b_c05_result")
                else onNavigate("b_c05_photo")
            }
        )
        "processing", "adjust" -> C05Processing(
            onDone = { onNavigate("b_c05_result") },
            onOffline = { onNavigate("b_c05_offline") },
            onLowCredits = { onNavigate("b_c05_low_credits") },
            onBack = { onNavigate("b_c05_photo") }
        )
        "result" -> C05Result(
            onVariations = { onNavigate("b_c05_variations") },
            onEdit = { onNavigate("b_c05_edit") },
            onTune = { onNavigate("b_c05_options") },
            onExport = { onNavigate("b17_presets") },
            onStickers = { onNavigate("b13_photo") },
            onRemake = { onNavigate("b_c05_photo") },
            onPublish = { onNavigate("b8_publish") },
            onBack = { onNavigate("b_c05_photo") }
        )
        "variations" -> C05Variations(
            onPick = { onNavigate("b_c05_edit") },
            onBack = { onNavigate("b_c05_result") }
        )
        "edit" -> C05Edit(
            onApply = { onNavigate("b_c05_share") },
            onBack = { onNavigate("b_c05_result") }
        )
        "share" -> C05Share(
            onSave = { onNavigate("b8_ready") },
            onDone = onBack,
            onBack = { onNavigate("b_c05_result") }
        )
        "offline" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Offline,
            onPrimary = { onNavigate("b_c05_options") },
            onSecondary = { onNavigate("b_c05_options") }
        )
        "low_credits" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.LowCredits,
            onPrimary = { onNavigate("b23_dash") },
            onSecondary = { onNavigate("b_c05_options") }
        )
        "safety" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Safety,
            onPrimary = { onNavigate("b_c05_photo") },
            onSecondary = onBack
        )
        else -> C05Landing(
            onContinue = { onNavigate("b_c05_photo") },
            onBack = onBack
        )
    }
}

@Composable
private fun C05Landing(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C05_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Image(
                    painter = painterResource(BoardFlowArt.logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(28.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.surfaceMuted)
                    .clickable(onClick = onContinue)
            ) {
                Image(
                    painter = painterResource(R.drawable.board_c05_before),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop
                )
                Text(
                    stringResource(R.string.create_extra_restore),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(PixiSpacing.lg),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Text(
                stringResource(R.string.board_c05_recent),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                listOf(
                    R.drawable.board_c05_before,
                    R.drawable.board_c05_after,
                    BoardFlowArt.profileAvatar,
                    R.drawable.board_c09_portrait,
                    BoardFlowArt.homeHero
                ).forEach { art ->
                    Image(
                        painter = painterResource(art),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onContinue),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Text(stringResource(R.string.board_ct_tip_restore), color = colors.textSecondary)
            BoardPrimaryButton(
                text = stringResource(R.string.board_c05_start),
                onClick = onContinue,
                modifier = Modifier.testTag(TestTags.TOOL_CONTINUE)
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C05PickPhoto(onSample: () -> Unit, onGallery: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val thumbs = listOf(
        R.drawable.board_c05_before,
        R.drawable.board_ba_before,
        R.drawable.board_c05_before,
        R.drawable.board_c09_portrait,
        R.drawable.board_ba_before,
        R.drawable.board_c05_before
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C05_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c05_select_photo),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            thumbs.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEachIndexed { col, art ->
                        Image(
                            painter = painterResource(art),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.85f)
                                .clip(RoundedCornerShape(radii.md))
                                .then(
                                    if (col == 0 && art == thumbs[0]) {
                                        Modifier.testTag(TestTags.TOOL_USE_SAMPLE)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable {
                                    BoardMediaSession.setSampleRes(art)
                                    onSample()
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            BoardAddPhotoButton(
                labelRes = R.string.board_c00_use_my_photo,
                onPicked = onGallery
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C05ChooseType(onGenerate: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC05Session.optionIndex) }
    val options = listOf(
        Triple(R.drawable.pj_ic_palette, R.string.board_ct_opt_colorize, R.string.board_c05_opt_colorize_sub),
        Triple(R.drawable.pj_ic_edit, R.string.board_c05_opt_repair, R.string.board_c05_opt_repair_sub),
        Triple(R.drawable.pj_ic_face, R.string.board_ct_opt_enhance_face, R.string.board_c05_opt_face_sub),
        Triple(R.drawable.pj_ic_sun, R.string.board_ct_opt_full_restore, R.string.board_c05_opt_full_sub)
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C05_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c04_adjust_settings),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(R.drawable.board_c05_before),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            options.forEachIndexed { i, (icon, title, sub) ->
                val on = selected == i
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(if (on) colors.primary.copy(0.12f) else colors.surfaceMuted)
                        .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md)) else Modifier)
                        .clickable {
                            selected = i
                            BoardFlowC05Session.optionIndex = i
                        }
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Icon(painter = painterResource(icon), null, tint = colors.primary, modifier = Modifier.size(28.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(title), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text(stringResource(sub), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_c05_generate), onClick = onGenerate)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C05Processing(
    onDone: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    var step by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    val checks = listOf(
        R.string.board_c04_check_details,
        R.string.board_c04_check_lighting,
        R.string.board_c04_check_colors,
        R.string.board_c04_check_best
    )
    val cancelled = remember { AtomicBoolean(false) }
    fun requestCancel() {
        cancelled.set(true)
        onBack()
    }
    BackHandler(onBack = { requestCancel() })
    LaunchedEffect(Unit) {
        BoardFlowC05Session.setResult(null)
        BoardFlowC05Session.setBefore(null)
        BoardFlowC05Session.usedSoftFallback = false
        step = 1
        progress = 0.25f
        val result = BoardCToolEngine.run(context) { pipe, src ->
            val before = src.copy(Bitmap.Config.ARGB_8888, false)
            if (before != null) BoardFlowC05Session.setBefore(before)
            pipe.restoreChain(src, BoardFlowC05Session.optionIndex)
        }
        if (!isActive || cancelled.get()) return@LaunchedEffect
        step = 2
        progress = 0.5f
        delay(100)
        if (!isActive || cancelled.get()) return@LaunchedEffect
        step = 3
        progress = 0.75f
        delay(100)
        if (!isActive || cancelled.get()) return@LaunchedEffect
        step = 4
        progress = 1f
        if (result != null) {
            BoardFlowC05Session.setResult(result)
            BoardFlowC05Session.usedSoftFallback = false
        } else {
            BoardFlowC05Session.usedSoftFallback = true
        }
        delay(200)
        if (!isActive || cancelled.get()) return@LaunchedEffect
        onDone()
    }
    BoardAdaptiveFrame {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.processingSurface)
                .statusBarsPadding()
                .testTag(TestTags.FLOW_C05_HOST)
                .padding(PixiSpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BoardProcessingHero(progress = progress, darkSurface = true)
                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                Text(
                    stringResource(R.string.board_c05_processing),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                checks.forEachIndexed { i, res ->
                    if (i < step) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                        ) {
                            Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(20.dp))
                            Text(stringResource(res), color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(PixiSpacing.md))
                Text(stringResource(R.string.board_c04_few_seconds), color = Color.White.copy(0.7f))
                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                BoardEdgeActions(
                    onOffline = onOffline,
                    onLowCredits = null,
                    darkSurface = true
                )
                TextButton(onClick = { requestCancel() }) {
                    Text(stringResource(R.string.cancel), color = Color.White.copy(0.8f))
                }
            }
        }
    }
}

@Composable
private fun C05Result(
    onVariations: () -> Unit,
    onEdit: () -> Unit,
    onTune: () -> Unit,
    onExport: () -> Unit,
    onStickers: () -> Unit,
    onRemake: () -> Unit,
    onPublish: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var filter by remember { mutableIntStateOf(BoardFlowC05Session.filterIndex) }
    val filters = listOf(
        R.string.board_c05_filter_original,
        R.string.board_c05_filter_vivid,
        R.string.board_c05_filter_natural,
        R.string.board_c05_filter_bright,
        R.string.board_c05_filter_dreamy
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C05_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c05_result),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            BoardBeforeAfter(
                beforeRes = R.drawable.board_c05_before,
                afterRes = R.drawable.board_c05_after,
                beforeBitmap = BoardFlowC05Session.beforeBitmap,
                afterBitmap = BoardFlowC05Session.resultBitmap,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card))
            )
            if (BoardFlowC05Session.usedSoftFallback) {
                Text(
                    stringResource(R.string.board_c04_soft_fallback),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                filters.forEachIndexed { i, res ->
                    val on = filter == i
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (on) colors.primary else colors.surfaceMuted)
                            .clickable {
                                filter = i
                                BoardFlowC05Session.filterIndex = i
                            }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(res),
                            color = if (on) colors.onPrimary else colors.textPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            BoardResultFooter(
                onSave = {
                    val out = BoardFlowC05Session.resultBitmap
                    val ok = if (out != null && !out.isRecycled) {
                        BoardDrawableIo.saveBitmapToGallery(context, out, filePrefix = "PixiJoy_Restore")
                    } else {
                        BoardDrawableIo.saveToGallery(
                            context,
                            R.drawable.board_c05_after,
                            filePrefix = "PixiJoy_Restore"
                        )
                    }
                    context.toastBoardSaveResult(ok)
                },
                onShare = {
                    BoardDrawableIo.shareBitmapAsync(
                        context = context,
                        bitmap = BoardFlowC05Session.resultBitmap,
                        fallbackRes = R.drawable.board_c05_after,
                        filePrefix = "PixiJoy_Restore"
                    )
                },
                onEdit = onEdit,
                onExport = onExport,
                onRemake = onRemake,
                onPublish = onPublish,
                onStickers = onStickers,
                onWhatsApp = {
                    BoardDrawableIo.shareToWhatsApp(
                        context = context,
                        bitmap = BoardFlowC05Session.resultBitmap,
                        fallbackRes = R.drawable.board_c05_after,
                        filePrefix = "PixiJoy_Restore"
                    )
                },
                onStatus = {
                    BoardDrawableIo.shareToWhatsApp(
                        context = context,
                        bitmap = BoardFlowC05Session.resultBitmap,
                        fallbackRes = R.drawable.board_c05_after,
                        filePrefix = "PixiJoy_Restore"
                    )
                }
            )
            BoardTertiaryButton(
                text = stringResource(R.string.board_c04_adjust_settings),
                onClick = onTune
            )
            TextButton(onClick = onVariations, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.board_c04_more_variations), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C05Variations(onPick: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC05Session.variationIndex) }
    val items = listOf(
        R.string.board_c05_var_subtle to R.drawable.board_c05_after,
        R.string.board_c05_filter_vivid to R.drawable.board_ba_after,
        R.string.board_c05_var_film to R.drawable.board_c09_portrait,
        R.string.board_c05_filter_natural to R.drawable.board_c05_after
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C05_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c04_more_variations),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            items.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEach { (label, art) ->
                        val index = items.indexOfFirst { it.first == label }
                        val on = selected == index
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(radii.md))
                                .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md)) else Modifier)
                                .clickable {
                                    selected = index
                                    BoardFlowC05Session.variationIndex = index
                                }
                        ) {
                            Image(
                                painter = painterResource(art),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(radii.md)),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                stringResource(label),
                                modifier = Modifier.padding(PixiSpacing.sm),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
            BoardOutlinedButton(
                text = stringResource(R.string.board_c04_generate_more),
                onClick = {
                    if (items.isNotEmpty()) {
                        selected = (selected + 1) % items.size
                        BoardFlowC05Session.variationIndex = selected
                    }
                    Toast.makeText(
                        context,
                        context.getString(R.string.board_soft_preview_title),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
            BoardPrimaryButton(text = stringResource(R.string.board_c04_use_this), onClick = onPick)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C05Edit(onApply: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var tool by remember { mutableIntStateOf(0) }
    var brightness by remember { mutableFloatStateOf(BoardFlowC05Session.brightness) }
    var contrast by remember { mutableFloatStateOf(BoardFlowC05Session.contrast) }
    var saturation by remember { mutableFloatStateOf(BoardFlowC05Session.saturation) }
    var warmth by remember { mutableFloatStateOf(BoardFlowC05Session.warmth) }
    var faceEnhance by remember { mutableStateOf(BoardFlowC05Session.faceEnhance) }
    val tools = listOf(
        R.string.board_c05_tool_adjust,
        R.string.board_c05_tool_filters,
        R.string.board_c05_tool_effects,
        R.string.board_c05_tool_text,
        R.string.board_c05_tool_stickers
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C05_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c05_edit_photo),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(R.drawable.board_c05_after),
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
                tools.forEachIndexed { i, res ->
                    val on = tool == i
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (on) colors.primary else colors.surfaceMuted)
                            .clickable { tool = i }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(res),
                            color = if (on) colors.onPrimary else colors.textPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            EditSlider(R.string.board_c05_brightness, brightness) {
                brightness = it
                BoardFlowC05Session.brightness = it
            }
            EditSlider(R.string.board_c05_contrast, contrast) {
                contrast = it
                BoardFlowC05Session.contrast = it
            }
            EditSlider(R.string.board_c05_saturation, saturation) {
                saturation = it
                BoardFlowC05Session.saturation = it
            }
            EditSlider(R.string.board_c05_warmth, warmth) {
                warmth = it
                BoardFlowC05Session.warmth = it
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = faceEnhance,
                    onCheckedChange = {
                        faceEnhance = it
                        BoardFlowC05Session.faceEnhance = it
                    }
                )
                Text(stringResource(R.string.board_c05_face_enhance), color = colors.textPrimary)
            }
            BoardPrimaryButton(text = stringResource(R.string.board_c04_apply), onClick = onApply)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun EditSlider(@StringRes label: Int, value: Float, onChange: (Float) -> Unit) {
    val colors = BoardTheme.colors
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(R.drawable.pj_ic_brightness), null, tint = colors.primary, modifier = Modifier.size(18.dp))
            Text(
                stringResource(label),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = PixiSpacing.sm),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary
            )
            Text("${(value * 10).toInt()}", color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
        }
        Slider(value = value, onValueChange = onChange)
    }
}

@Composable
private fun C05Share(
    onSave: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TestTags.FLOW_C05_HOST)
        ) {
            val bmp = BoardFlowC05Session.resultBitmap
            if (bmp != null && !bmp.isRecycled) {
                BoardSafeResultImage(
                    bitmap = bmp,
                    fallbackRes = R.drawable.board_c05_after,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.board_c05_after),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = radii.card, topEnd = radii.card))
                    .background(colors.surface)
                    .padding(PixiSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_sparkles), null, tint = colors.primary, modifier = Modifier.size(36.dp))
                Text(
                    stringResource(R.string.board_c05_memory_ready),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.board_c05_memory_body),
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                BoardResultFooter(
                    onSave = {
                        val out = BoardFlowC05Session.resultBitmap
                        val ok = if (out != null && !out.isRecycled) {
                            BoardDrawableIo.saveBitmapToGallery(context, out, filePrefix = "PixiJoy_Restore")
                        } else {
                            BoardDrawableIo.saveToGallery(
                                context,
                                R.drawable.board_c05_after,
                                filePrefix = "PixiJoy_Restore"
                            )
                        }
                        context.toastBoardSaveResult(ok)
                        if (ok) onSave()
                    },
                    onShare = {
                        BoardDrawableIo.shareBitmapAsync(
                            context = context,
                            bitmap = BoardFlowC05Session.resultBitmap,
                            fallbackRes = R.drawable.board_c05_after,
                            filePrefix = "PixiJoy_Restore"
                        )
                    },
                    onEdit = onBack,
                    onExport = onBack,
                    onRemake = onBack,
                    onPublish = onDone,
                    onWhatsApp = {
                        BoardDrawableIo.shareToWhatsApp(
                            context = context,
                            bitmap = BoardFlowC05Session.resultBitmap,
                            fallbackRes = R.drawable.board_c05_after,
                            filePrefix = "PixiJoy_Restore"
                        )
                    },
                    onStatus = {
                        BoardDrawableIo.shareToWhatsApp(
                            context = context,
                            bitmap = BoardFlowC05Session.resultBitmap,
                            fallbackRes = R.drawable.board_c05_after,
                            filePrefix = "PixiJoy_Restore"
                        )
                    }
                )
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.cd_back), color = colors.primary)
                }
                TextButton(onClick = onDone) {
                    Text(stringResource(R.string.board_ct_done), color = colors.textSecondary)
                }
            }
        }
    }
}
