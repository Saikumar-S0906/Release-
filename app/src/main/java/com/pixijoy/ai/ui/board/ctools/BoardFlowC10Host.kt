package com.pixijoy.ai.ui.board.ctools

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wallpaper
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
import com.pixijoy.ai.ui.board.BoardSessionPhoto
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardLottie
import com.pixijoy.ai.ui.board.components.BoardMotionKey
import com.pixijoy.ai.ui.board.components.BoardEdgeActions
import com.pixijoy.ai.ui.board.components.BoardEdgeKind
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardSoftEdgeGate
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import com.pixijoy.ai.ui.board.BoardSafeResultImage
import com.pixijoy.ai.ui.board.components.BoardResultFooter
import com.pixijoy.ai.ui.board.components.BoardScreenHeader
import kotlinx.coroutines.launch

/** C10 session — expand / upscale prefs + result bitmap. */
internal object BoardFlowC10Session {
    var toolIndex: Int = 13 // AI Expand default in more-tools grid
    var aspectIndex: Int = 1
    var filterIndex: Int = 1
    var upscale4x: Boolean = true
    var brightness: Float = 0.7f
    var contrast: Float = 0.55f
    var saturation: Float = 0.45f
    var warmth: Float = 0.3f
    var sharpness: Float = 0.1f
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

    /** Maps UI state → [com.pixijoy.ai.ml.CreateExtraPipeline.expandOrUpscale] index. */
    fun pipelineOptionIndex(): Int = when {
        // Upscale screen choice wins when user picked 2×/4× on the upscale step
        upscale4x -> 3
        aspectIndex == 1 -> 4 // 1:1 square pad
        toolIndex == 13 -> 1 // expand canvas
        else -> 2 // 2× upscale
    }

    var usedSoftFallback: Boolean = false
}

/**
 * C10 — AI Expand, Fill & Upscale.
 * Real expand/upscale when a photo is in [com.pixijoy.ai.ui.board.BoardMediaSession].
 */
@Composable
fun BoardFlowC10Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val step = BoardCTool.stepOf(screenId)
    when (step) {
        "intro", "hub", "" -> C10MoreTools(
            onExpand = { onNavigate("b_c10_photo") },
            onRemoveBg = { onNavigate("b_c04") },
            onBack = onBack
        )
        "photo" -> C10Preview(
            onCrop = { onNavigate("b_c10_options") },
            onAdjust = { onNavigate("b_c10_adjust") },
            onFilters = { onNavigate("b_c10_filters") },
            onMore = { onNavigate("b_c10_options") },
            onBack = { onNavigate("b_c10") }
        )
        "options" -> C10Crop(
            onDone = { onNavigate("b_c10_adjust") },
            onBack = { onNavigate("b_c10_photo") }
        )
        "adjust" -> C10Adjust(
            onDone = { onNavigate("b_c10_filters") },
            onBack = { onNavigate("b_c10_options") }
        )
        "filters" -> C10Filters(
            onDone = { onNavigate("b_c10_result") },
            onBack = { onNavigate("b_c10_adjust") }
        )
        "result", "variations" -> C10Upscale(
            onContinue = { onNavigate("b_c10_erase") },
            onBack = { onNavigate("b_c10_filters") }
        )
        "erase", "processing" -> C10GenerativeErase(
            onDone = { onNavigate("b_c10_share") },
            onOffline = { onNavigate("b_c10_offline") },
            onLowCredits = { onNavigate("b_c10_low_credits") },
            onBack = { onNavigate("b_c10_result") }
        )
        "share" -> C10SaveHd(
            onSave = { onNavigate("b8_ready") },
            onShare = { onNavigate("b8_share") },
            onEdit = { onNavigate("b15_open") },
            onExport = { onNavigate("b17_presets") },
            onRemake = { onNavigate("b_c10_photo") },
            onPublish = { onNavigate("b8_publish") },
            onDone = onBack,
            onBack = { onNavigate("b_c10_result") }
        )
        "offline" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Offline,
            onPrimary = { onNavigate("b_c10_result") },
            onSecondary = { onNavigate("b_c10_result") }
        )
        "low_credits" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.LowCredits,
            onPrimary = { onNavigate("b23_dash") },
            onSecondary = { onNavigate("b_c10_result") }
        )
        "safety" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Safety,
            onPrimary = { onNavigate("b_c10_photo") },
            onSecondary = onBack
        )
        else -> C10MoreTools(
            onExpand = { onNavigate("b_c10_photo") },
            onRemoveBg = { onNavigate("b_c04") },
            onBack = onBack
        )
    }
}

@Composable
private fun C10MoreTools(
    onExpand: () -> Unit,
    onRemoveBg: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val tools = listOf(
        R.drawable.pj_ic_magic to R.string.board_c10_tool_retouch,
        R.drawable.pj_ic_cut to R.string.board_c10_tool_erase,
        R.drawable.pj_ic_landscape to R.string.board_c10_tool_background,
        R.drawable.pj_ic_brush to R.string.board_c10_tool_brush,
        R.drawable.pj_ic_grid to R.string.board_c10_tool_mask,
        R.drawable.pj_ic_layers to R.string.board_c10_tool_layers,
        R.drawable.pj_ic_sparkles to R.string.board_c10_tool_ai_enhance,
        R.drawable.pj_ic_sparkles to R.string.board_c10_tool_ai_replace,
        R.drawable.pj_ic_crop to R.string.board_c10_tool_perspective,
        R.drawable.pj_ic_palette to R.string.board_c10_tool_hdr,
        R.drawable.pj_ic_blur to R.string.board_c10_tool_blur,
        R.drawable.pj_ic_cut to R.string.board_c10_tool_clone,
        R.drawable.pj_ic_palette to R.string.board_c10_tool_splash,
        R.drawable.pj_ic_expand to R.string.board_c10_tool_expand
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C10_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c10_more_tools),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Text(
                stringResource(R.string.create_extra_expand),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Text(stringResource(R.string.board_c10_subtitle), color = colors.textSecondary)
            tools.chunked(4).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEachIndexed { col, (icon, label) ->
                        val index = rowIndex * 4 + col
                        val highlight = index == 2 || index == 13 // Background / AI Expand
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(radii.md))
                                .background(if (highlight) colors.primary.copy(0.14f) else colors.surfaceMuted)
                                .then(
                                    if (highlight) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md))
                                    else Modifier
                                )
                                .clickable {
                                    BoardFlowC10Session.toolIndex = index
                                    when (index) {
                                        2 -> onRemoveBg()
                                        13 -> onExpand()
                                        else -> onExpand()
                                    }
                                }
                                .padding(PixiSpacing.sm),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(painter = painterResource(icon), null, tint = colors.primary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(label),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                    repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
            Text(
                stringResource(R.string.board_c10_slogan),
                color = colors.accentPink,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            BoardPrimaryButton(text = stringResource(R.string.board_c10_tool_expand), onClick = onExpand)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C10Preview(
    onCrop: () -> Unit,
    onAdjust: () -> Unit,
    onFilters: () -> Unit,
    onMore: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var hasPhoto by remember { mutableStateOf(BoardMediaSession.hasPhoto()) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C10_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.create_extra_expand),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                if (hasPhoto) {
                    BoardSessionPhoto(fallbackRes = R.drawable.board_c10_expand)
                } else {
                    Image(
                        painter = painterResource(R.drawable.board_c10_expand),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            if (!hasPhoto) {
                com.pixijoy.ai.ui.board.BoardUseSampleButton(
                    sampleRes = R.drawable.board_c10_expand,
                    onReady = { hasPhoto = true }
                )
                BoardAddPhotoButton(
                    labelRes = R.string.board_c00_use_my_photo,
                    onPicked = { hasPhoto = true }
                )
            }
            val tabs = listOf(
                Triple(R.drawable.pj_ic_crop, R.string.board_c10_tab_crop, onCrop),
                Triple(R.drawable.pj_ic_tune, R.string.board_c10_tab_adjust, onAdjust),
                Triple(R.drawable.pj_ic_palette, R.string.board_c10_tab_filters, onFilters),
                Triple(R.drawable.pj_ic_sparkles, R.string.board_c10_tab_effects, onFilters),
                Triple(R.drawable.pj_ic_grid, R.string.board_c10_tab_more, onMore)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabs.forEach { (icon, label, action) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(onClick = action)
                    ) {
                        Icon(painter = painterResource(icon), null, tint = colors.primary, modifier = Modifier.size(24.dp))
                        Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = colors.textPrimary)
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_c10_tab_crop), onClick = onCrop)
        }
    }
}

@Composable
private fun C10Crop(onDone: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val context = LocalContext.current
    var aspect by remember { mutableIntStateOf(BoardFlowC10Session.aspectIndex) }
    val ratios = listOf(
        R.string.board_c10_aspect_free,
        R.string.board_c10_aspect_11,
        R.string.board_c10_aspect_45,
        R.string.board_c10_aspect_169,
        R.string.board_c10_aspect_34
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C10_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c10_tab_crop),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            var crop by remember { mutableStateOf(com.pixijoy.ai.ui.board.components.BoardCropNorm.Full) }
            val cropSrc = remember(BoardMediaSession.revision) {
                BoardMediaSession.decodeBitmap(context, maxSide = 1600)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                com.pixijoy.ai.ui.board.components.BoardCropCanvas(
                    bitmap = cropSrc,
                    fallbackRes = R.drawable.board_c10_expand,
                    crop = crop,
                    onCropChange = { crop = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                ratios.forEachIndexed { i, res ->
                    val on = aspect == i
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (on) colors.primary else colors.surfaceMuted)
                            .clickable {
                                aspect = i
                                BoardFlowC10Session.aspectIndex = i
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
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                BoardOutlinedButton(
                    text = stringResource(R.string.cancel),
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                )
                BoardPrimaryButton(
                    text = stringResource(R.string.done),
                    onClick = onDone,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun C10Adjust(onDone: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var brightness by remember { mutableFloatStateOf(BoardFlowC10Session.brightness) }
    var contrast by remember { mutableFloatStateOf(BoardFlowC10Session.contrast) }
    var saturation by remember { mutableFloatStateOf(BoardFlowC10Session.saturation) }
    var warmth by remember { mutableFloatStateOf(BoardFlowC10Session.warmth) }
    var sharpness by remember { mutableFloatStateOf(BoardFlowC10Session.sharpness) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C10_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c10_tab_adjust),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(R.drawable.board_c10_expand),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            C10Slider(R.string.board_c05_brightness, brightness) {
                brightness = it
                BoardFlowC10Session.brightness = it
            }
            C10Slider(R.string.board_c05_contrast, contrast) {
                contrast = it
                BoardFlowC10Session.contrast = it
            }
            C10Slider(R.string.board_c05_saturation, saturation) {
                saturation = it
                BoardFlowC10Session.saturation = it
            }
            C10Slider(R.string.board_c05_warmth, warmth) {
                warmth = it
                BoardFlowC10Session.warmth = it
            }
            C10Slider(R.string.board_c10_sharpness, sharpness) {
                sharpness = it
                BoardFlowC10Session.sharpness = it
            }
            BoardPrimaryButton(text = stringResource(R.string.done), onClick = onDone)
        }
    }
}

@Composable
private fun C10Slider(@StringRes label: Int, value: Float, onChange: (Float) -> Unit) {
    val colors = BoardTheme.colors
    Column {
        Row {
            Text(
                stringResource(label),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary
            )
            Text("${(value * 20).toInt()}", color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
        }
        Slider(value = value, onValueChange = onChange)
    }
}

@Composable
private fun C10Filters(onDone: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC10Session.filterIndex) }
    val filters = listOf(
        R.string.board_c10_filter_none to R.drawable.board_c10_expand,
        R.string.board_c05_filter_vivid to R.drawable.board_ba_after,
        R.string.board_c10_filter_warm to R.drawable.board_c07_beach,
        R.string.board_c10_filter_cool to R.drawable.board_c07_city,
        R.string.board_c10_filter_bw to R.drawable.board_c05_before,
        R.string.board_c10_filter_vintage to R.drawable.board_c09_portrait,
        R.string.board_c10_filter_cine to R.drawable.board_c07_scene,
        R.string.board_c05_filter_dreamy to R.drawable.board_c04_beach
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C10_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c10_tab_filters),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(filters[selected].second),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            filters.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEach { (label, art) ->
                        val index = filters.indexOfFirst { it.first == label }
                        val on = selected == index
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(radii.md))
                                .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md)) else Modifier)
                                .clickable {
                                    selected = index
                                    BoardFlowC10Session.filterIndex = index
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
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
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                    repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.done), onClick = onDone)
        }
    }
}

@Composable
private fun C10Upscale(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var upscale4x by remember { mutableStateOf(BoardFlowC10Session.upscale4x) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C10_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c10_upscale),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                Image(
                    painter = painterResource(R.drawable.board_c10_expand),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.55f)
                        .aspectRatio(0.85f)
                        .border(2.dp, colors.primary, RoundedCornerShape(8.dp))
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                listOf(false to R.string.board_ct_opt_upscale_2x, true to R.string.board_ct_opt_upscale_4x).forEach { (is4x, label) ->
                    val on = upscale4x == is4x
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (on) colors.primary else colors.surfaceMuted)
                            .clickable {
                                upscale4x = is4x
                                BoardFlowC10Session.upscale4x = is4x
                            }
                            .padding(vertical = PixiSpacing.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(label),
                            color = if (on) colors.onPrimary else colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Text(stringResource(R.string.board_ct_pro_hd), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            BoardPrimaryButton(text = stringResource(R.string.board_c00_continue), onClick = onContinue)
        }
    }
}

@Composable
private fun C10GenerativeErase(
    onDone: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        BoardFlowC10Session.setResult(null)
        BoardFlowC10Session.usedSoftFallback = false
        progress = 0.2f
        val result = BoardCToolEngine.run(context) { pipe, src ->
            pipe.expandOrUpscale(src, BoardFlowC10Session.pipelineOptionIndex())
        }
        progress = 0.7f
        delay(120)
        if (result != null) {
            BoardFlowC10Session.setResult(result)
            BoardFlowC10Session.usedSoftFallback = false
        } else {
            BoardFlowC10Session.usedSoftFallback = true
        }
        progress = 1f
        delay(200)
        onDone()
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C10_HOST),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                stringResource(R.string.board_c10_gen_erase),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentAlignment = Alignment.Center
            ) {
                BoardSafeResultImage(
                    bitmap = BoardFlowC10Session.resultBitmap,
                    fallbackRes = R.drawable.board_c10_expand,
                    modifier = Modifier.fillMaxSize()
                )
                if (BoardFlowC10Session.resultBitmap == null) {
                    BoardLottie(
                        key = BoardMotionKey.GenerationProgress,
                        size = 56.dp,
                        decorative = true
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Text(
                stringResource(R.string.board_c10_erasing),
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            BoardEdgeActions(
                onOffline = onOffline,
                onLowCredits = null
            )
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.cd_back), color = colors.primary)
            }
        }
    }
}

@Composable
private fun C10SaveHd(
    onSave: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onRemake: () -> Unit,
    onPublish: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C10_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.create_extra_expand),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            BoardSafeResultImage(
                bitmap = BoardFlowC10Session.resultBitmap,
                fallbackRes = R.drawable.board_c07_beach,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card))
            )
            if (BoardFlowC10Session.usedSoftFallback) {
                Text(
                    stringResource(R.string.board_c04_soft_fallback),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            BoardResultFooter(
                onSave = {
                    val out = BoardFlowC10Session.resultBitmap
                    val ok = if (out != null && !out.isRecycled) {
                        BoardDrawableIo.saveBitmapToGallery(context, out, filePrefix = "PixiJoy_Expand")
                    } else {
                        BoardDrawableIo.saveToGallery(
                            context,
                            R.drawable.board_c10_expand,
                            filePrefix = "PixiJoy_Expand"
                        )
                    }
                    Toast.makeText(
                        context,
                        if (ok) R.string.board_c04_saved else R.string.board_c04_save_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                    if (ok) onSave()
                },
                onShare = onShare,
                onEdit = onEdit,
                onExport = onExport,
                onRemake = onRemake,
                onPublish = onPublish
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.board_c10_slogan), color = colors.textSecondary)
                Icon(painter = painterResource(R.drawable.pj_ic_heart),
                    null,
                    tint = colors.accentPink,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp)
                )
            }
            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.board_ct_done), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}
