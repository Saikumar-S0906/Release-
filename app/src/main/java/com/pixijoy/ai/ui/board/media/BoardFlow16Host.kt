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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Filter
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardCollageRasterizer
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.BoardSafeResultImage
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardEdgeActions
import com.pixijoy.ai.ui.board.components.BoardEdgeKind
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardSoftEdgeGate
import com.pixijoy.ai.ui.board.components.BoardSoftPulse
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.components.BoardWideFrame
import com.pixijoy.ai.ui.board.components.boardGridColumns
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Flow 16 session — multi-photo collage picks persist across steps. */
internal object BoardFlow16Session {
    var purposeIndex: Int = 0
    var layoutIndex: Int = 7 // AI Auto
    var filterIndex: Int = 0
    var previewTool: Int = 0
    var arrangeTool: Int = 0
    var selectedSlot: Int = 0
    val selectedIndices: MutableList<Int> = mutableListOf(0, 1, 2, 3, 4)

    @Volatile
    var resultBitmap: Bitmap? = null
        private set

    fun setResult(bitmap: Bitmap?) {
        val old = resultBitmap
        resultBitmap = bitmap
        if (old != null && old !== bitmap && !old.isRecycled) {
            runCatching { old.recycle() }
        }
    }

    val library: List<Int> = listOf(
        BoardFlowArt.beautifulHero,
        BoardFlowArt.funnyHero,
        BoardFlowArt.stickerHero,
        BoardFlowArt.creationThumb,
        BoardFlowArt.homeHero,
        BoardFlowArt.trendPortrait,
        BoardFlowArt.feedThumb,
        BoardFlowArt.profileAvatar,
        BoardFlowArt.differentHero,
        BoardFlowArt.motionHero,
        BoardFlowArt.editorHero,
        BoardFlowArt.collageHero
    )

    @DrawableRes
    fun hero(): Int = BoardFlowArt.collageHero

    @DrawableRes
    fun thumb(index: Int): Int = library[index.coerceIn(0, library.lastIndex)]

    fun clampSelection() {
        while (selectedIndices.size > 9) selectedIndices.removeAt(selectedIndices.lastIndex)
        while (selectedIndices.size < 2 && library.isNotEmpty()) {
            val next = (0 until library.size).firstOrNull { it !in selectedIndices } ?: break
            selectedIndices.add(next)
        }
    }
}

private data class PurposeOption(@StringRes val title: Int, val accent: Color)

/**
 * Flow 16 — Multi-Photo / Collage.
 * WORKS: [BoardCollageRasterizer] → result bitmap; gallery save via [BoardDrawableIo.saveBitmapToGallery].
 * SoftEdge offline / low-credits / safety overlays stay.
 * Board may show 5-tab chrome; production keeps Home · Discover · Create · Community · Profile
 * (entry via Create / openCollage / Flow 3 multi).
 */
@Composable
fun BoardFlow16Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    val pipeline = LocalBoardPipeline.current
    when (screenId) {
        "b16_hub", "b16_select" -> SelectPhotos(
            onTab = tab,
            onNext = { onNavigate("b16_purpose") },
            onBack = onBack
        )
        "b16_purpose" -> ChoosePurpose(
            onPick = { onNavigate("b16_arrange") },
            onBack = { onNavigate("b16_select") }
        )
        "b16_arrange" -> ArrangePhotos(
            onNext = { onNavigate("b16_creating") },
            onCancel = { onNavigate("b16_purpose") },
            onAdd = { onNavigate("b16_select") }
        )
        "b16_creating" -> AiCreating(
            onDone = { onNavigate("b16_preview") },
            onCancel = { onNavigate("b16_arrange") },
            onOffline = { onNavigate("b16_offline") },
            onLowCredits = { onNavigate("b16_low_credits") }
        )
        "b16_preview" -> PreviewEdit(
            onNext = { onNavigate("b16_save") },
            onBack = { onNavigate("b16_arrange") }
        )
        "b16_save" -> SaveShare(
            onDone = { onNavigate("b16_end") },
            onHome = onBack,
            onShare = pipeline.openShare,
            onSlideshow = { onNavigate("b14_input") },
            onBack = { onNavigate("b16_preview") }
        )
        "b16_end" -> EndResult(
            onClose = onBack,
            onShare = pipeline.openShare
        )
        "b16_offline" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Offline,
            onPrimary = { onNavigate("b16_creating") },
            onSecondary = { onNavigate("b16_arrange") }
        )
        "b16_low_credits" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.LowCredits,
            onPrimary = { onNavigate("b23_dash") },
            onSecondary = { onNavigate("b16_arrange") }
        )
        "b16_safety" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Safety,
            onPrimary = { onNavigate("b16_select") },
            onSecondary = onBack
        )
        else -> SelectPhotos(
            onTab = tab,
            onNext = { onNavigate("b16_purpose") },
            onBack = onBack
        )
    }
}

@Composable
private fun SelectPhotos(
    onTab: (BoardShellTab) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var albumTab by remember { mutableIntStateOf(0) }
    val selected = remember {
        mutableStateListOf<Int>().also { it.addAll(BoardFlow16Session.selectedIndices) }
    }
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW16_HOST)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f16_select_photos),
                    style = BoardType.title,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.lg)
            ) {
                listOf(R.string.board_f16_tab_photos, R.string.board_f16_tab_albums).forEachIndexed { i, res ->
                    Column(
                        modifier = Modifier.clickable { albumTab = i },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(res),
                            fontWeight = if (i == albumTab) FontWeight.Bold else FontWeight.Medium,
                            color = if (i == albumTab) colors.primary else colors.textSecondary
                        )
                        if (i == albumTab) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .height(3.dp)
                                    .fillMaxWidth(0.6f)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colors.primary)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            LazyVerticalGrid(
                columns = GridCells.Fixed(boardGridColumns(compact = 3, medium = 4, expanded = 5)),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                itemsIndexed(BoardFlow16Session.library) { index, res ->
                    val isOn = selected.contains(index)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(radii.card))
                            .clickable {
                                if (isOn) {
                                    if (selected.size > 2) selected.remove(index)
                                } else if (selected.size < 9) {
                                    selected.add(index)
                                }
                            }
                    ) {
                        Image(
                            painter = painterResource(res),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (isOn) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painter = painterResource(R.drawable.pj_ic_check), null, tint = colors.onPrimary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            Text(
                stringResource(R.string.flow16_selected, selected.size),
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = PixiSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                selected.toList().forEach { idx ->
                    Box {
                        Image(
                            painter = painterResource(BoardFlow16Session.thumb(idx)),
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(radii.md)),
                            contentScale = ContentScale.Crop
                        )
                        Icon(painter = painterResource(R.drawable.pj_ic_close),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xE0E53935))
                                .padding(2.dp)
                                .clickable {
                                    if (selected.size > 2) selected.remove(idx)
                                }
                        )
                    }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.flow16_next_count, selected.size),
                onClick = {
                    if (selected.size in 2..9) {
                        BoardFlow16Session.selectedIndices.clear()
                        BoardFlow16Session.selectedIndices.addAll(selected.take(9))
                        BoardFlow16Session.clampSelection()
                        onNext()
                    }
                }
            )
        }
    }
}

@Composable
private fun ChoosePurpose(onPick: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlow16Session.purposeIndex) }
    val purposes = listOf(
        PurposeOption(R.string.board_f16_purpose_collage, colors.accentPurple),
        PurposeOption(R.string.board_f16_purpose_meme, colors.accentYellow),
        PurposeOption(R.string.board_f16_purpose_story, colors.accentPink),
        PurposeOption(R.string.board_f16_purpose_reel, colors.accentPurple),
        PurposeOption(R.string.board_f16_purpose_slideshow, colors.primary),
        PurposeOption(R.string.board_f16_purpose_poster, colors.accentYellow),
        PurposeOption(R.string.board_f16_purpose_birthday, colors.accentPink),
        PurposeOption(R.string.board_f16_purpose_festival, colors.primary),
        PurposeOption(R.string.board_f16_purpose_family, colors.accentPurple),
        PurposeOption(R.string.board_f16_purpose_creative, colors.primary),
        PurposeOption(R.string.board_f16_purpose_merge, colors.accentPink),
        PurposeOption(R.string.board_f16_purpose_surprise, colors.accentPurple)
    )
    val columns = boardGridColumns(compact = 3, medium = 4, expanded = 4)
    BoardWideFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow16_purpose_title),
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                val purposeThumbs = BoardFlowArt.mediaThumbs + BoardFlowArt.sceneThumbs
                items(purposes.size) { i ->
                    val p = purposes[i]
                    val on = i == selected
                    val thumb = purposeThumbs[i % purposeThumbs.size]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(radii.card))
                            .background(if (on) p.accent.copy(0.28f) else colors.surfaceMuted)
                            .border(
                                width = if (on) 2.dp else 0.dp,
                                color = if (on) p.accent else Color.Transparent,
                                shape = RoundedCornerShape(radii.card)
                            )
                            .clickable {
                                selected = i
                                BoardFlow16Session.purposeIndex = i
                                onPick()
                            }
                            .padding(PixiSpacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        Image(
                            painter = painterResource(thumb),
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            stringResource(p.title),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            style = BoardType.caption,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArrangePhotos(
    onNext: () -> Unit,
    onCancel: () -> Unit,
    onAdd: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var layout by remember { mutableIntStateOf(BoardFlow16Session.layoutIndex) }
    var tool by remember { mutableIntStateOf(BoardFlow16Session.arrangeTool) }
    var slot by remember { mutableIntStateOf(BoardFlow16Session.selectedSlot) }
    val layouts = listOf(
        R.string.board_f16_layout_classic,
        R.string.board_f16_layout_modern,
        R.string.board_f16_layout_polaroid,
        R.string.board_f16_layout_grid,
        R.string.board_f16_layout_magazine,
        R.string.board_f16_layout_circular,
        R.string.board_f16_layout_free,
        R.string.board_f16_layout_ai
    )
    val tools = listOf(
        R.drawable.pj_ic_grid to R.string.board_f16_tool_layout,
        R.drawable.pj_ic_swap to R.string.board_f16_tool_reorder,
        R.drawable.pj_ic_refresh to R.string.board_f16_tool_replace,
        R.drawable.pj_ic_rotate to R.string.board_f16_tool_rotate,
        R.drawable.pj_ic_delete to R.string.board_f16_tool_delete
    )
    val picks = BoardFlow16Session.selectedIndices.ifEmpty { listOf(0, 1, 2, 3, 4) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel), color = colors.textSecondary)
                }
                Text(
                    stringResource(R.string.board_f16_arrange_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    BoardFlow16Session.layoutIndex = layout
                    BoardFlow16Session.arrangeTool = tool
                    BoardFlow16Session.selectedSlot = slot
                    BoardFlow16Session.clampSelection()
                    onNext()
                }) {
                    Text(stringResource(R.string.next), color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.surfaceMuted)
                    .padding(PixiSpacing.sm)
            ) {
                CollageCanvas(
                    indices = picks,
                    selectedSlot = slot,
                    layoutIndex = layout,
                    onSelect = { slot = it }
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                layouts.forEachIndexed { i, res ->
                    val on = i == layout
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(BoardTheme.radii.pill))
                            .background(if (on) colors.primary else colors.surfaceMuted)
                            .clickable {
                                layout = i
                                BoardFlow16Session.layoutIndex = i
                            }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(res),
                            color = if (on) colors.onPrimary else colors.textSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                picks.forEachIndexed { i, idx ->
                    Image(
                        painter = painterResource(BoardFlow16Session.thumb(idx)),
                        contentDescription = null,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(radii.md))
                            .border(
                                width = if (i == slot) 2.dp else 0.dp,
                                color = if (i == slot) colors.primary else Color.Transparent,
                                shape = RoundedCornerShape(radii.md)
                            )
                            .clickable { slot = i },
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(radii.md))
                        .background(colors.surfaceMuted)
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_add), null, tint = colors.primary)
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tools.forEachIndexed { i, (icon, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            tool = i
                            BoardFlow16Session.arrangeTool = i
                            when (i) {
                                2 -> { /* replace: cycle thumb */ }
                                3 -> { /* rotate visual only */ }
                                4 -> {
                                    if (BoardFlow16Session.selectedIndices.size > 2) {
                                        val removeAt = slot.coerceIn(0, BoardFlow16Session.selectedIndices.lastIndex)
                                        BoardFlow16Session.selectedIndices.removeAt(removeAt)
                                        slot = slot.coerceAtMost(BoardFlow16Session.selectedIndices.lastIndex)
                                    }
                                }
                            }
                        }
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
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardPrimaryButton(
                text = stringResource(R.string.board_m16_ai_create),
                onClick = {
                    BoardFlow16Session.layoutIndex = layout
                    BoardFlow16Session.clampSelection()
                    onNext()
                }
            )
        }
    }
}

@Composable
private fun CollageCanvas(
    indices: List<Int>,
    selectedSlot: Int,
    layoutIndex: Int,
    onSelect: (Int) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val count = indices.size.coerceAtLeast(2)
    when {
        layoutIndex == 5 -> {
            // Circular emphasis: center + ring
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                indices.take(4).forEachIndexed { i, idx ->
                    val size = if (i == 0) 160.dp else 72.dp
                    Image(
                        painter = painterResource(BoardFlow16Session.thumb(idx)),
                        contentDescription = null,
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape)
                            .border(
                                width = if (i == selectedSlot) 3.dp else 0.dp,
                                color = colors.primary,
                                shape = CircleShape
                            )
                            .clickable { onSelect(i) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        layoutIndex == 2 -> {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                indices.take(3).forEachIndexed { i, idx ->
                    Image(
                        painter = painterResource(BoardFlow16Session.thumb(idx)),
                        contentDescription = null,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                width = if (i == selectedSlot) 3.dp else 2.dp,
                                color = if (i == selectedSlot) colors.primary else Color.White,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { onSelect(i) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        else -> {
            val cols = when {
                count <= 2 -> 2
                count == 3 -> 2
                else -> 2
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                indices.chunked(cols).forEachIndexed { rowIdx, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        row.forEachIndexed { colIdx, idx ->
                            val slot = rowIdx * cols + colIdx
                            Image(
                                painter = painterResource(BoardFlow16Session.thumb(idx)),
                                contentDescription = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(radii.md))
                                    .border(
                                        width = if (slot == selectedSlot) 3.dp else 0.dp,
                                        color = colors.primary,
                                        shape = RoundedCornerShape(radii.md)
                                    )
                                    .clickable { onSelect(slot) },
                                contentScale = ContentScale.Crop
                            )
                        }
                        if (row.size < cols) {
                            Spacer(modifier = Modifier.weight((cols - row.size).toFloat()))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiCreating(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    var progress by remember { mutableFloatStateOf(0.08f) }
    val steps = listOf(
        R.string.board_f16_step_analyze,
        R.string.board_f16_step_layout,
        R.string.board_f16_step_enhance,
        R.string.board_f16_step_create,
        R.string.board_f16_step_touches
    )
    LaunchedEffect(Unit) {
        BoardFlow16Session.clampSelection()
        val progressJob = launch {
            while (progress < 0.9f) {
                delay(280)
                progress = (progress + 0.12f).coerceAtMost(0.9f)
            }
        }
        val thumbs = BoardFlow16Session.selectedIndices
            .take(9)
            .map { BoardFlow16Session.thumb(it) }
        val rendered = withContext(Dispatchers.Default) {
            runCatching {
                BoardCollageRasterizer.render(
                    context = context.applicationContext,
                    thumbs = thumbs.ifEmpty {
                        listOf(BoardFlowArt.collageHero, BoardFlowArt.beautifulHero)
                    },
                    layoutIndex = BoardFlow16Session.layoutIndex
                )
            }.getOrNull()
        }
        if (rendered != null) {
            BoardFlow16Session.setResult(rendered)
        }
        progressJob.cancel()
        progress = 1f
        delay(250)
        onDone()
    }
    val doneCount = ((progress * steps.size).toInt()).coerceIn(0, steps.size)
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                stringResource(R.string.board_f16_ai_creating),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            BoardSoftPulse {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(120.dp),
                        color = colors.primary,
                        trackColor = colors.surfaceMuted,
                        strokeWidth = 8.dp
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            Column(verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                steps.forEachIndexed { i, res ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (i < doneCount) {
                            Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(20.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, colors.textSecondary, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.size(PixiSpacing.sm))
                        Text(
                            stringResource(res),
                            color = if (i < doneCount) colors.textPrimary else colors.textSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(BoardTheme.radii.pill)),
                color = colors.primary,
                trackColor = colors.surfaceMuted
            )
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            BoardEdgeActions(
                onOffline = onOffline,
                onLowCredits = null
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardOutlinedButton(text = stringResource(R.string.cancel), onClick = onCancel)
        }
    }
}

@Composable
private fun PreviewEdit(onNext: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var filter by remember { mutableIntStateOf(BoardFlow16Session.filterIndex) }
    var tool by remember { mutableIntStateOf(BoardFlow16Session.previewTool) }
    val filters = listOf(
        R.string.board_f16_filter_original,
        R.string.board_f16_filter_bright,
        R.string.board_f16_filter_warm,
        R.string.board_f16_filter_cinematic,
        R.string.board_f16_filter_bw
    )
    val tools: List<Pair<Int, Int>> = listOf(
        R.drawable.pj_ic_grid to R.string.board_f16_tool_layout,
        R.drawable.pj_ic_text to R.string.flow15_text,
        R.drawable.pj_ic_sticker to R.string.board_f15_stickers,
        R.drawable.pj_ic_filter to R.string.board_f16_tool_filter,
        R.drawable.pj_ic_landscape to R.string.board_f16_tool_background
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow16_result_preview),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    BoardFlow16Session.filterIndex = filter
                    BoardFlow16Session.previewTool = tool
                    onNext()
                }) {
                    Text(stringResource(R.string.next), color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                BoardSafeResultImage(
                    bitmap = BoardFlow16Session.resultBitmap,
                    fallbackRes = BoardFlow16Session.hero(),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Text(
                    stringResource(R.string.flow16_end_preview),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(PixiSpacing.lg)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                filters.forEachIndexed { i, res ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            filter = i
                            BoardFlow16Session.filterIndex = i
                        }
                    ) {
                        Image(
                            painter = painterResource(BoardFlow16Session.hero()),
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(radii.md))
                                .border(
                                    width = if (i == filter) 2.dp else 0.dp,
                                    color = colors.primary,
                                    shape = RoundedCornerShape(radii.md)
                                ),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            stringResource(res),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (i == filter) colors.primary else colors.textSecondary
                        )
                    }
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
                            BoardFlow16Session.previewTool = i
                        }
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
            BoardPrimaryButton(
                text = stringResource(R.string.next),
                onClick = onNext,
                modifier = Modifier.testTag(com.pixijoy.ai.testing.TestTags.TOOL_CONTINUE)
            )
        }
    }
}

@Composable
private fun SaveShare(
    onDone: () -> Unit,
    onHome: () -> Unit,
    onShare: () -> Unit,
    onSlideshow: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    val actions = listOf(
        Triple(R.drawable.pj_ic_folder, R.string.board_f16_save_gallery, 0),
        Triple(R.drawable.pj_ic_share, R.string.board_f16_share_apps, 1),
        Triple(R.drawable.pj_ic_movie, R.string.flow16_slideshow, 2),
        Triple(R.drawable.pj_ic_save, R.string.flow16_save_template, 3)
    )
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
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f16_save_share),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onHome) {
                    Icon(painter = painterResource(R.drawable.pj_ic_home), contentDescription = stringResource(R.string.nav_home))
                }
            }
            BoardSafeResultImage(
                bitmap = BoardFlow16Session.resultBitmap,
                fallbackRes = BoardFlow16Session.hero(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success)
                Text(
                    stringResource(R.string.board_f16_ready),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            }
            actions.forEach { (icon, label, action) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .clickable {
                            when (action) {
                                0 -> scope.launch {
                                    val ok = withContext(Dispatchers.IO) {
                                        val bmp = BoardFlow16Session.resultBitmap
                                        if (bmp != null && !bmp.isRecycled) {
                                            BoardDrawableIo.saveBitmapToGallery(
                                                context,
                                                bmp,
                                                filePrefix = "PixiJoy_Collage"
                                            )
                                        } else {
                                            saveDrawableToGallery(context, BoardFlow16Session.hero())
                                        }
                                    }
                                    Toast.makeText(
                                        context,
                                        context.getString(
                                            if (ok) R.string.board_f16_saved else R.string.board_f16_save_failed
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    if (ok) onDone()
                                }
                                1 -> {
                                    shareDrawable(context, BoardFlow16Session.hero())
                                    onShare()
                                }
                                2 -> onSlideshow()
                                3 -> scope.launch {
                                    val ok = withContext(Dispatchers.IO) {
                                        persistCollageTemplate(context)
                                    }
                                    Toast.makeText(
                                        context,
                                        context.getString(
                                            if (ok) R.string.board_f16_template_saved
                                            else R.string.board_f16_save_failed
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Icon(painter = painterResource(icon), null, tint = colors.primary)
                    Text(stringResource(label), fontWeight = FontWeight.Medium, color = colors.textPrimary)
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.done), onClick = onDone)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun EndResult(onClose: () -> Unit, onShare: () -> Unit) {
    val colors = BoardTheme.colors
    BoardAdaptiveFrame {
        Box(modifier = Modifier.fillMaxSize()) {
            BoardSafeResultImage(
                bitmap = BoardFlow16Session.resultBitmap,
                fallbackRes = BoardFlow16Session.hero(),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(PixiSpacing.md)
                    .clip(CircleShape)
                    .background(Color.Black.copy(0.45f))
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_close), contentDescription = stringResource(R.string.cd_back), tint = Color.White)
            }
            Text(
                stringResource(R.string.flow16_watermark),
                color = Color.White.copy(0.9f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(PixiSpacing.lg)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(PixiSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                BoardPrimaryButton(text = stringResource(R.string.share), onClick = onShare)
                BoardOutlinedButton(text = stringResource(R.string.done), onClick = onClose)
            }
        }
    }
}

private fun saveDrawableToGallery(context: Context, @DrawableRes resId: Int): Boolean =
    BoardDrawableIo.saveToGallery(context, resId, filePrefix = "PixiJoy_Collage")

private fun shareDrawable(context: Context, @DrawableRes resId: Int) {
    BoardDrawableIo.shareAsync(
        context = context,
        resId = resId,
        textRes = R.string.board_f5_share_text,
        filePrefix = "pixijoy_collage"
    )
}

private suspend fun persistCollageTemplate(context: Context): Boolean = runCatching {
    val indices = BoardFlow16Session.selectedIndices.joinToString(",")
    context.applicationContext.getSharedPreferences("pixijoy_board_templates", Context.MODE_PRIVATE)
        .edit()
        .putInt("collage_layout_index", BoardFlow16Session.layoutIndex)
        .putString("collage_selected_indices", indices)
        .putInt("collage_purpose_index", BoardFlow16Session.purposeIndex)
        .apply()
    true
}.getOrDefault(false)

/** Optional load helper for saved collage template prefs. */
internal fun loadCollageTemplate(context: Context): Boolean = runCatching {
    val prefs = context.applicationContext.getSharedPreferences("pixijoy_board_templates", Context.MODE_PRIVATE)
    if (!prefs.contains("collage_layout_index")) return false
    BoardFlow16Session.layoutIndex = prefs.getInt("collage_layout_index", BoardFlow16Session.layoutIndex)
    BoardFlow16Session.purposeIndex = prefs.getInt("collage_purpose_index", BoardFlow16Session.purposeIndex)
    val raw = prefs.getString("collage_selected_indices", null) ?: return true
    val parsed = raw.split(',').mapNotNull { it.trim().toIntOrNull() }
    if (parsed.isNotEmpty()) {
        BoardFlow16Session.selectedIndices.clear()
        BoardFlow16Session.selectedIndices.addAll(parsed)
        BoardFlow16Session.clampSelection()
    }
    true
}.getOrDefault(false)

