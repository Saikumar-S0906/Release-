package com.pixijoy.ai.ui.board.library

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
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
import android.graphics.Bitmap
import android.widget.Toast
import com.pixijoy.ai.R
import com.pixijoy.ai.di.BoardAiEntryPoint
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardEditorDocument
import com.pixijoy.ai.ui.board.BoardMediaSession
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.BoardSafeResultImage
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardColorAdjust
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardEmptyState
import com.pixijoy.ai.ui.board.components.BoardFavoriteToggle
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardLottie
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardMotionKey
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardSecondaryButton
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardSkeletonCard
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.components.BoardWideFrame
import com.pixijoy.ai.ui.board.components.boardGridColumns
import com.pixijoy.ai.ui.board.home.ExploreSearchHit
import com.pixijoy.ai.ui.board.home.exploreMoreLike
import com.pixijoy.ai.ui.board.home.exploreSearchIndex
import com.pixijoy.ai.ui.board.home.filterExploreSearch
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.theme.PixiSpacing
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Thumbs = BoardFlowArt.libraryThumbs

/** Bake ColorMatrix + text overlay into working bitmap before gallery save. */
private suspend fun bakeEditorDocumentForSave(context: android.content.Context) {
    val src = BoardEditorDocument.workingBitmap?.takeIf { !it.isRecycled } ?: return
    val caption = BoardEditorDocument.textOverlay
    val matrix = BoardEditorDocument.bakeMatrix()
    val needsGrade =
        BoardEditorDocument.brightness != BoardColorAdjust.Neutral ||
            BoardEditorDocument.contrast != BoardColorAdjust.Neutral ||
            BoardEditorDocument.saturation != BoardColorAdjust.Neutral ||
            BoardEditorDocument.warmth != BoardColorAdjust.Neutral
    val pipe = runCatching {
        EntryPointAccessors.fromApplication(context, BoardAiEntryPoint::class.java)
            .createExtraPipeline()
    }.getOrNull()
    val baked = withContext(Dispatchers.Default) {
        var out: Bitmap = src
        if (needsGrade) {
            val graded = Bitmap.createBitmap(out.width, out.height, Bitmap.Config.ARGB_8888)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
            }
            android.graphics.Canvas(graded).drawBitmap(out, 0f, 0f, paint)
            if (out !== src && !out.isRecycled) runCatching { out.recycle() }
            out = graded
        }
        if (pipe != null && caption.isNotBlank()) {
            val withText = pipe.bakeTextOverlay(
                out,
                caption,
                colorArgb = android.graphics.Color.WHITE,
                align = 1,
                fontStyle = 0,
                scale = 1f,
                verticalBias = 0.88f
            )
            if (withText !== out && out !== src && !out.isRecycled) runCatching { out.recycle() }
            out = withText
        }
        out
    }
    if (baked !== src) {
        BoardEditorDocument.setWorkingBitmap(baked, push = false)
        BoardEditorDocument.resetAdjust(push = false)
        BoardEditorDocument.textOverlay = ""
    }
}

/** Flow 26 session — query / filter / favorites across search steps. */
internal object BoardFlow26Session {
    var query: String = ""
    var resultTab: Int = 0
    var sortIndex: Int = 0
    var themeIndex: Int = 0
    var lookIndex: Int = 0
    var contentType: Int = 0
    var selectedId: Int = 0
    var lastHits: List<ExploreSearchHit> = emptyList()
    val recent = mutableListOf("birthday", "cute pets", "travel")
    val favorites = mutableSetOf<Int>()

    fun selectedHit(): ExploreSearchHit? =
        lastHits.find { it.id == selectedId } ?: exploreSearchIndex().find { it.id == selectedId }

    fun selectedArt(): Int = selectedHit()?.card?.art ?: BoardFlowArt.homeTemplate

    fun selectedTitle(): Int = selectedHit()?.card?.titleRes ?: R.string.board_f26_tpl_wishes
}

/**
 * Flow 26 — Search / Templates.
 * Customize bakes grade + text into [BoardEditorDocument.workingBitmap] before save.
 */
@Composable
fun BoardFlow26Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val pipeline = LocalBoardPipeline.current
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    when (screenId) {
        "b26_hub", "b26_open" -> OpenSearch(
            onSearch = { onNavigate("b26_search") },
            onTab = tab
        )
        "b26_search" -> SearchScreen(
            onResults = { onNavigate("b26_results") },
            onBack = { onNavigate("b26_open") }
        )
        "b26_results" -> ResultsScreen(
            onDetail = { id ->
                BoardFlow26Session.selectedId = id
                onNavigate("b26_detail")
            },
            onFilters = { onNavigate("b26_filters") },
            onBack = { onNavigate("b26_search") }
        )
        "b26_empty" -> ResultsScreen(
            onDetail = { },
            onFilters = { onNavigate("b26_filters") },
            onBack = { onNavigate("b26_search") },
            forceEmpty = true
        )
        "b26_filters" -> FiltersScreen(
            onApply = { onNavigate("b26_results") },
            onBack = { onNavigate("b26_results") }
        )
        "b26_detail" -> DetailScreen(
            onUse = { onNavigate("b26_customize") },
            onPreview = pipeline.openMediaInput,
            onMore = { onNavigate("b26_more") },
            onBack = { onNavigate("b26_results") }
        )
        "b26_customize" -> CustomizeScreen(
            onNext = { onNavigate("b26_saved") },
            onBack = { onNavigate("b26_detail") }
        )
        "b26_saved" -> SavedScreen(
            onShare = { onNavigate("b8_share") },
            onCreations = { onNavigate("b18_browse") },
            onAnother = { onNavigate("b26_search") },
            onMore = { onNavigate("b26_more") },
            onBack = onBack
        )
        "b26_more" -> MoreLikeThis(
            onDetail = { id ->
                BoardFlow26Session.selectedId = id
                onNavigate("b26_detail")
            },
            onBack = { onNavigate("b26_detail") }
        )
        else -> OpenSearch(
            onSearch = { onNavigate("b26_search") },
            onTab = tab
        )
    }
}

@Composable
private fun OpenSearch(onSearch: () -> Unit, onTab: (BoardShellTab) -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardMainShell(selected = BoardShellTab.Home, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW26_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(BoardFlowArt.logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onSearch) {
                    Icon(painter = painterResource(R.drawable.pj_ic_search), contentDescription = stringResource(R.string.flow26_search))
                }
            }
            Image(
                painter = painterResource(BoardFlowArt.homeHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.board_f26_find_inspiration),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            BoardPrimaryButton(text = stringResource(R.string.flow26_open_search), onClick = onSearch)
        }
    }
}

@Composable
private fun SearchScreen(onResults: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var query by remember { mutableStateOf(BoardFlow26Session.query) }
    var recent by remember { mutableStateOf(BoardFlow26Session.recent.toList()) }
    val popular = listOf(
        R.string.board_f26_chip_birthday,
        R.string.board_f26_chip_wedding,
        R.string.board_f26_chip_nature,
        R.string.board_f26_chip_pets,
        R.string.board_f26_chip_travel
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW26_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        BoardFlow26Session.query = it
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.flow26_search)) },
                    leadingIcon = { Icon(painter = painterResource(R.drawable.pj_ic_search), null) },
                    singleLine = true,
                    shape = RoundedCornerShape(radii.md)
                )
            }
            Text(stringResource(R.string.flow26_recent), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            recent.forEach { q ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .clickable {
                            query = q
                            BoardFlow26Session.query = q
                            onResults()
                        }
                        .padding(PixiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_time), null, tint = colors.textSecondary)
                    Text(q, modifier = Modifier.weight(1f).padding(horizontal = PixiSpacing.md), color = colors.textPrimary)
                    IconButton(onClick = {
                        BoardFlow26Session.recent.remove(q)
                        recent = BoardFlow26Session.recent.toList()
                    }) {
                        Icon(painter = painterResource(R.drawable.pj_ic_close), null, tint = colors.textSecondary)
                    }
                }
            }
            Text(stringResource(R.string.flow26_popular), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                popular.forEach { res ->
                    val label = stringResource(res)
                    Text(
                        label,
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(colors.surfaceMuted)
                            .clickable {
                                query = label
                                BoardFlow26Session.query = label
                                if (!BoardFlow26Session.recent.contains(label)) {
                                    BoardFlow26Session.recent.add(0, label)
                                    recent = BoardFlow26Session.recent.toList()
                                }
                                onResults()
                            }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.flow26_search), onClick = {
                if (query.isNotBlank() && !BoardFlow26Session.recent.contains(query)) {
                    BoardFlow26Session.recent.add(0, query)
                    recent = BoardFlow26Session.recent.toList()
                }
                onResults()
            })
        }
    }
}

@Composable
private fun ResultsScreen(
    onDetail: (Int) -> Unit,
    onFilters: () -> Unit,
    onBack: () -> Unit,
    forceEmpty: Boolean = false
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var tab by remember { mutableIntStateOf(BoardFlow26Session.resultTab) }
    var gridReady by remember { mutableStateOf(false) }
    LaunchedEffect(tab, forceEmpty) {
        gridReady = false
        delay(280)
        gridReady = true
    }
    val tabs = listOf(
        R.string.board_f26_tab_all,
        R.string.board_f26_tab_templates,
        R.string.board_f26_tab_stickers,
        R.string.board_f26_tab_styles
    )
    val columns = boardGridColumns(compact = 2, medium = 3, expanded = 3)
    val context = LocalContext.current
    val type = if (tab == 0) BoardFlow26Session.contentType else tab
    val hits = remember(
        tab,
        type,
        BoardFlow26Session.query,
        BoardFlow26Session.themeIndex,
        BoardFlow26Session.lookIndex,
        BoardFlow26Session.sortIndex,
        forceEmpty
    ) {
        if (forceEmpty) {
            emptyList()
        } else {
            filterExploreSearch(
                query = BoardFlow26Session.query,
                contentType = type,
                theme = BoardFlow26Session.themeIndex,
                look = BoardFlow26Session.lookIndex,
                sort = BoardFlow26Session.sortIndex,
                titleOf = { context.getString(it) }
            ).also { BoardFlow26Session.lastHits = it }
        }
    }
    val resultCount = hits.size
    BoardWideFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW26_HOST)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f26_showing, BoardFlow26Session.query),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onFilters) {
                    Icon(painter = painterResource(R.drawable.pj_ic_filter), contentDescription = stringResource(R.string.flow26_filters))
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                tabs.forEachIndexed { i, label ->
                    val active = tab == i
                    Text(
                        stringResource(label),
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (active) colors.primary else colors.surfaceMuted)
                            .clickable {
                                tab = i
                                BoardFlow26Session.resultTab = i
                                if (i <= 3) BoardFlow26Session.contentType = i
                            }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                        color = if (active) colors.onPrimary else colors.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            when {
                !gridReady -> {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                    ) {
                        repeat(2) { BoardSkeletonCard() }
                    }
                }
                resultCount == 0 -> {
                    BoardEmptyState(
                        title = stringResource(R.string.board_search_empty_title),
                        body = stringResource(R.string.board_search_empty_body),
                        ctaLabel = stringResource(R.string.board_search_empty_cta),
                        onCta = onBack,
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        items(hits, key = { it.id }) { hit ->
                            val fav = BoardFlow26Session.favorites.contains(hit.id)
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(radii.card))
                                    .background(colors.surfaceMuted)
                                    .clickable { onDetail(hit.id) }
                                    .padding(PixiSpacing.sm)
                            ) {
                                Box {
                                    Image(
                                        painter = painterResource(hit.card.art),
                                        contentDescription = stringResource(hit.card.titleRes),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(radii.md)),
                                        contentScale = ContentScale.Crop
                                    )
                                    BoardFavoriteToggle(
                                        favored = fav,
                                        onToggle = {
                                            if (fav) BoardFlow26Session.favorites.remove(hit.id)
                                            else BoardFlow26Session.favorites.add(hit.id)
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd),
                                        contentDescription = stringResource(R.string.flow9_favorites)
                                    )
                                }
                                Text(
                                    stringResource(hit.card.titleRes),
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    stringResource(R.string.board_f26_uses),
                                    color = colors.textSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FiltersScreen(onApply: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var sort by remember { mutableIntStateOf(BoardFlow26Session.sortIndex) }
    var type by remember { mutableIntStateOf(BoardFlow26Session.contentType) }
    var theme by remember { mutableIntStateOf(BoardFlow26Session.themeIndex) }
    var look by remember { mutableIntStateOf(BoardFlow26Session.lookIndex) }
    data class ChipGroup(@StringRes val title: Int, val options: List<Int>)
    val groups = listOf(
        ChipGroup(
            R.string.board_f26_content_type,
            listOf(
                R.string.board_f26_tab_all,
                R.string.board_f26_tab_templates,
                R.string.board_f26_tab_stickers,
                R.string.board_f26_tab_styles,
                R.string.board_f26_chip_frames,
                R.string.board_f26_chip_collages
            )
        ),
        ChipGroup(
            R.string.board_f26_theme,
            listOf(
                R.string.board_f26_tab_all,
                R.string.board_f26_chip_birthday,
                R.string.board_f26_chip_love,
                R.string.board_f26_chip_family,
                R.string.board_f26_chip_festival,
                R.string.board_f26_chip_travel,
                R.string.board_f26_chip_pets
            )
        ),
        ChipGroup(
            R.string.board_f26_style,
            listOf(
                R.string.board_f26_tab_all,
                R.string.board_f26_style_photo,
                R.string.board_f26_style_illustration,
                R.string.board_f26_style_cartoon,
                R.string.board_f26_style_3d,
                R.string.board_f26_style_watercolor
            )
        )
    )
    val sorts = listOf(
        R.string.board_f26_sort_relevance,
        R.string.flow26_popular,
        R.string.board_f26_sort_newest
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow26_filters),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            groups.forEachIndexed { gi, group ->
                Text(stringResource(group.title), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    group.options.forEachIndexed { oi, label ->
                        val active = when (gi) {
                            0 -> type == oi
                            1 -> theme == oi
                            else -> look == oi
                        }
                        Text(
                            stringResource(label),
                            modifier = Modifier
                                .clip(RoundedCornerShape(radii.pill))
                                .background(if (active) colors.primary else colors.surfaceMuted)
                                .clickable {
                                    when (gi) {
                                        0 -> {
                                            type = oi
                                            BoardFlow26Session.contentType = oi
                                            BoardFlow26Session.resultTab = oi.coerceAtMost(3)
                                        }
                                        1 -> {
                                            theme = oi
                                            BoardFlow26Session.themeIndex = oi
                                        }
                                        else -> {
                                            look = oi
                                            BoardFlow26Session.lookIndex = oi
                                        }
                                    }
                                }
                                .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                            color = if (active) colors.onPrimary else colors.textPrimary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            Text(stringResource(R.string.board_f26_sort_by), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                sorts.forEachIndexed { i, label ->
                    val active = sort == i
                    Text(
                        stringResource(label),
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (active) colors.primary else colors.surfaceMuted)
                            .clickable {
                                sort = i
                                BoardFlow26Session.sortIndex = i
                            }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                        color = if (active) colors.onPrimary else colors.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f26_apply_filters), onClick = onApply)
        }
    }
}

@Composable
private fun DetailScreen(
    onUse: () -> Unit,
    onPreview: () -> Unit,
    onMore: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val hit = BoardFlow26Session.selectedHit()
    val art = hit?.card?.art ?: BoardFlowArt.homeTemplate
    val titleRes = hit?.card?.titleRes ?: R.string.board_f26_tpl_wishes
    val tags = listOf(
        R.string.board_f26_chip_birthday,
        R.string.board_f26_tag_greeting,
        R.string.board_f26_tag_colorful,
        R.string.board_f26_tag_fun
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow26_template_detail),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                BoardLottie(key = BoardMotionKey.TemplatePreview, size = 40.dp)
            }
            Image(
                painter = painterResource(art),
                contentDescription = stringResource(titleRes),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(titleRes),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            Text(stringResource(R.string.flow26_detail_meta), color = colors.textSecondary)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                tags.forEach { res ->
                    Text(
                        stringResource(res),
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(colors.surfaceMuted)
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textPrimary
                    )
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.flow26_use_template), onClick = onUse)
            BoardOutlinedButton(text = stringResource(R.string.flow26_preview_photo), onClick = onPreview)
            TextButton(onClick = onMore) {
                Text(stringResource(R.string.flow26_more_like), color = colors.primary)
            }
        }
    }
}

@Composable
private fun CustomizeScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    var tool by remember { mutableIntStateOf(0) }
    var rev by remember { mutableIntStateOf(BoardEditorDocument.revision) }
    var caption by remember { mutableStateOf(BoardEditorDocument.textOverlay) }
    var brightness by remember { mutableFloatStateOf(BoardEditorDocument.brightness) }
    var contrast by remember { mutableFloatStateOf(BoardEditorDocument.contrast) }
    var saturation by remember { mutableFloatStateOf(BoardEditorDocument.saturation) }
    var warmth by remember { mutableFloatStateOf(BoardEditorDocument.warmth) }
    var baking by remember { mutableStateOf(false) }
    val tools = listOf(
        R.drawable.pj_ic_text to R.string.board_f26_tool_text,
        R.drawable.pj_ic_emoji to R.string.board_f26_tool_stickers,
        R.drawable.pj_ic_filter to R.string.board_f26_tool_filters,
        R.drawable.pj_ic_tune to R.string.board_f26_tool_adjust,
        R.drawable.pj_ic_more_horiz to R.string.board_f26_tool_more
    )
    val art = BoardFlow26Session.selectedArt()
    LaunchedEffect(art, BoardMediaSession.revision) {
        if (BoardMediaSession.hasPhoto()) {
            BoardEditorDocument.setWorkingBitmap(null)
            BoardEditorDocument.ensureLoaded(context, fallbackRes = art)
        } else {
            BoardEditorDocument.loadFromDrawable(context, art, clearHistory = true)
        }
        BoardEditorDocument.resetAdjust(push = false)
        brightness = BoardEditorDocument.brightness
        contrast = BoardEditorDocument.contrast
        saturation = BoardEditorDocument.saturation
        warmth = BoardEditorDocument.warmth
        caption = BoardEditorDocument.textOverlay
        rev = BoardEditorDocument.revision
    }
    val previewFilter = BoardColorAdjust.colorFilter(brightness, contrast, saturation, warmth)
    val previewBmp = remember(rev, BoardEditorDocument.workingBitmap) {
        BoardEditorDocument.workingBitmap?.takeIf { !it.isRecycled }
            ?.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
            ?.asImageBitmap()
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow26_customize),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    enabled = BoardEditorDocument.canUndo,
                    onClick = {
                        BoardEditorDocument.undo()
                        brightness = BoardEditorDocument.brightness
                        contrast = BoardEditorDocument.contrast
                        saturation = BoardEditorDocument.saturation
                        warmth = BoardEditorDocument.warmth
                        caption = BoardEditorDocument.textOverlay
                        rev = BoardEditorDocument.revision
                    }
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_undo),
                        contentDescription = stringResource(R.string.board_f15_history_ready)
                    )
                }
                IconButton(
                    enabled = BoardEditorDocument.canRedo,
                    onClick = {
                        BoardEditorDocument.redo()
                        brightness = BoardEditorDocument.brightness
                        contrast = BoardEditorDocument.contrast
                        saturation = BoardEditorDocument.saturation
                        warmth = BoardEditorDocument.warmth
                        caption = BoardEditorDocument.textOverlay
                        rev = BoardEditorDocument.revision
                    }
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_redo),
                        contentDescription = stringResource(R.string.board_f15_history_ready)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                if (previewBmp != null) {
                    Image(
                        bitmap = previewBmp,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = previewFilter
                    )
                } else {
                    Image(
                        painter = painterResource(art),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = previewFilter
                    )
                }
                if (caption.isNotBlank()) {
                    Text(
                        caption,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(PixiSpacing.md)
                            .clip(RoundedCornerShape(radii.pill))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tools.forEachIndexed { i, (icon, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.md))
                            .background(if (tool == i) colors.primary.copy(0.12f) else Color.Transparent)
                            .clickable {
                                tool = i
                                when (i) {
                                    2 -> {
                                        BoardEditorDocument.pushHistory()
                                        BoardEditorDocument.filterIndex =
                                            (BoardEditorDocument.filterIndex + 1) % 4
                                        when (BoardEditorDocument.filterIndex) {
                                            1 -> {
                                                saturation = 14f
                                                contrast = 12f
                                            }
                                            2 -> {
                                                warmth = 14f
                                                saturation = 11f
                                            }
                                            3 -> {
                                                saturation = 0f
                                                contrast = 12f
                                            }
                                            else -> {
                                                brightness = BoardColorAdjust.Neutral
                                                contrast = BoardColorAdjust.Neutral
                                                saturation = BoardColorAdjust.Neutral
                                                warmth = BoardColorAdjust.Neutral
                                            }
                                        }
                                        BoardEditorDocument.brightness = brightness
                                        BoardEditorDocument.contrast = contrast
                                        BoardEditorDocument.saturation = saturation
                                        BoardEditorDocument.warmth = warmth
                                        rev = BoardEditorDocument.revision
                                    }
                                    4 -> {
                                        BoardEditorDocument.rotate90(push = true)
                                        rev = BoardEditorDocument.revision
                                    }
                                }
                            }
                            .padding(PixiSpacing.sm)
                    ) {
                        Icon(painter = painterResource(icon), null, tint = if (tool == i) colors.primary else colors.textSecondary)
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (tool == i) colors.primary else colors.textSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            when (tool) {
                0 -> {
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.board_f26_tool_text)) }
                    )
                    TextButton(onClick = {
                        BoardEditorDocument.pushHistory()
                        BoardEditorDocument.textOverlay = caption
                        rev = BoardEditorDocument.revision
                    }) {
                        Text(stringResource(R.string.board_f15_apply), color = colors.primary)
                    }
                }
                1 -> {
                    val stickers = listOf("✨", "❤️", "😂", "🔥", "⭐", "🎉", "🌈", "👑")
                    Text(stringResource(R.string.board_f26_tool_stickers), color = colors.textSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        stickers.forEach { emoji ->
                            BoardOutlinedButton(
                                text = emoji,
                                onClick = {
                                    BoardEditorDocument.pushHistory()
                                    caption = (caption + " " + emoji).trim()
                                    BoardEditorDocument.textOverlay = caption
                                    rev = BoardEditorDocument.revision
                                }
                            )
                        }
                    }
                    BoardSecondaryButton(
                        text = stringResource(R.string.board_f15_clear_overlay),
                        onClick = {
                            BoardEditorDocument.pushHistory()
                            caption = ""
                            BoardEditorDocument.textOverlay = ""
                            rev = BoardEditorDocument.revision
                        }
                    )
                }
                2 -> {
                    val looks = listOf(
                        R.string.board_f15_filter_none to 0,
                        R.string.board_f15_filter_vivid to 1,
                        R.string.board_f15_filter_warm to 2,
                        R.string.board_f15_filter_bw to 3
                    )
                    looks.forEach { (label, idx) ->
                        BoardOutlinedButton(
                            text = stringResource(label),
                            onClick = {
                                BoardEditorDocument.pushHistory()
                                BoardEditorDocument.filterIndex = idx
                                when (idx) {
                                    1 -> {
                                        saturation = 14f
                                        contrast = 12f
                                        warmth = BoardColorAdjust.Neutral
                                    }
                                    2 -> {
                                        warmth = 14f
                                        saturation = 11f
                                        contrast = BoardColorAdjust.Neutral
                                    }
                                    3 -> {
                                        saturation = 0f
                                        contrast = 12f
                                        warmth = BoardColorAdjust.Neutral
                                    }
                                    else -> {
                                        brightness = BoardColorAdjust.Neutral
                                        contrast = BoardColorAdjust.Neutral
                                        saturation = BoardColorAdjust.Neutral
                                        warmth = BoardColorAdjust.Neutral
                                    }
                                }
                                BoardEditorDocument.brightness = brightness
                                BoardEditorDocument.contrast = contrast
                                BoardEditorDocument.saturation = saturation
                                BoardEditorDocument.warmth = warmth
                                rev = BoardEditorDocument.revision
                            }
                        )
                    }
                }
                3 -> {
                    Text(stringResource(R.string.board_f15_brightness), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = brightness,
                        onValueChange = {
                            brightness = it
                            BoardEditorDocument.brightness = it
                        },
                        valueRange = 0f..20f
                    )
                    Text(stringResource(R.string.board_f15_contrast), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = contrast,
                        onValueChange = {
                            contrast = it
                            BoardEditorDocument.contrast = it
                        },
                        valueRange = 0f..20f
                    )
                    Text(stringResource(R.string.board_f15_saturation), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = saturation,
                        onValueChange = {
                            saturation = it
                            BoardEditorDocument.saturation = it
                        },
                        valueRange = 0f..20f
                    )
                    Text(stringResource(R.string.board_f15_warmth), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = warmth,
                        onValueChange = {
                            warmth = it
                            BoardEditorDocument.warmth = it
                        },
                        valueRange = 0f..20f
                    )
                    TextButton(onClick = {
                        BoardEditorDocument.pushHistory()
                        rev = BoardEditorDocument.revision
                    }) {
                        Text(stringResource(R.string.board_f15_apply), color = colors.primary)
                    }
                }
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                        BoardOutlinedButton(
                            text = stringResource(R.string.board_f15_rotate),
                            onClick = {
                                BoardEditorDocument.rotate90(push = true)
                                rev = BoardEditorDocument.revision
                            },
                            modifier = Modifier.weight(1f)
                        )
                        BoardOutlinedButton(
                            text = stringResource(R.string.board_f15_flip_h),
                            onClick = {
                                BoardEditorDocument.flip(horizontal = true, push = true)
                                rev = BoardEditorDocument.revision
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    BoardOutlinedButton(
                        text = stringResource(R.string.board_f15_flip_v),
                        onClick = {
                            BoardEditorDocument.flip(horizontal = false, push = true)
                            rev = BoardEditorDocument.revision
                        }
                    )
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f4_continue),
                onClick = {
                    if (baking) return@BoardPrimaryButton
                    baking = true
                    BoardEditorDocument.textOverlay = caption
                    BoardEditorDocument.brightness = brightness
                    BoardEditorDocument.contrast = contrast
                    BoardEditorDocument.saturation = saturation
                    BoardEditorDocument.warmth = warmth
                    scope.launch {
                        try {
                            bakeEditorDocumentForSave(context.applicationContext)
                            onNext()
                        } finally {
                            baking = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SavedScreen(
    onShare: () -> Unit,
    onCreations: () -> Unit,
    onAnother: () -> Unit,
    onMore: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val art = BoardFlow26Session.selectedArt()
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f26_your_creation),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Text(
                stringResource(R.string.flow26_saved),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.md))
                    .background(colors.successSoft)
                    .padding(PixiSpacing.md),
                color = colors.success,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            BoardSafeResultImage(
                bitmap = BoardEditorDocument.workingBitmap,
                fallbackRes = art,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            BoardPrimaryButton(text = stringResource(R.string.board_f2_share), onClick = onShare)
            BoardOutlinedButton(
                text = stringResource(R.string.board_f26_download),
                onClick = {
                    val bmp = BoardEditorDocument.workingBitmap
                    val ok = if (bmp != null && !bmp.isRecycled) {
                        BoardDrawableIo.saveBitmapToGallery(
                            context,
                            bmp,
                            filePrefix = "PixiJoy_Template"
                        )
                    } else {
                        BoardDrawableIo.saveToGallery(
                            context,
                            art,
                            filePrefix = "PixiJoy_Template"
                        )
                    }
                    Toast.makeText(
                        context,
                        context.getString(
                            if (ok) R.string.board_f18_downloaded else R.string.board_soft_preview_title
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
            TextButton(onClick = onAnother) {
                Text(stringResource(R.string.board_f26_create_another), color = colors.primary)
            }
            TextButton(onClick = onCreations) {
                Text(stringResource(R.string.flow8_my_creations), color = colors.primary)
            }
            TextButton(onClick = onMore) {
                Text(stringResource(R.string.flow26_more_like), color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun MoreLikeThis(onDetail: (Int) -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val columns = boardGridColumns(compact = 2, medium = 3, expanded = 3)
    val similar = BoardFlow26Session.selectedHit()?.let { exploreMoreLike(it) }.orEmpty()
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
                    stringResource(R.string.flow26_more_like),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            if (similar.isEmpty()) {
                BoardEmptyState(
                    title = stringResource(R.string.board_search_empty_title),
                    body = stringResource(R.string.board_search_empty_body),
                    ctaLabel = stringResource(R.string.board_search_empty_cta),
                    onCta = onBack,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    items(similar, key = { it.id }) { hit ->
                        Image(
                            painter = painterResource(hit.card.art),
                            contentDescription = stringResource(hit.card.titleRes),
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(radii.card))
                                .clickable { onDetail(hit.id) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            BoardTrustFooter()
        }
    }
}
