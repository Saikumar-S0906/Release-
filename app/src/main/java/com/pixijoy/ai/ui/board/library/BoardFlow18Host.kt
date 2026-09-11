package com.pixijoy.ai.ui.board.library

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.LocalBoardCanPop
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardCaptionText
import com.pixijoy.ai.ui.board.components.BoardEmptyState
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardListDetail
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardSkeletonCard
import com.pixijoy.ai.ui.board.components.BoardTitleText
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.components.BoardWideFrame
import com.pixijoy.ai.ui.board.components.boardGridColumns
import com.pixijoy.ai.ui.board.components.rememberBoardTwoPane
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.components.WindowWidthSizeClass
import com.pixijoy.ai.ui.components.rememberWindowWidthSizeClass
import com.pixijoy.ai.ui.theme.PixiSpacing

import kotlinx.coroutines.launch

private data class AlbumItem(@StringRes val title: Int, val count: Int)

private data class ActionTriple(@DrawableRes val icon: Int, val label: Int, val action: () -> Unit)

private data class DetailAction(
    @DrawableRes val iconRes: Int? = null,
    @DrawableRes val icon: Int? = null,
    @StringRes val label: Int,
    val action: () -> Unit
)

/** Flow 18 session — browse filters / selection persist lightly. */
internal object BoardFlow18Session {
    var statusTab: Int = 0
    var selectedId: Int = 0
    var versionIndex: Int = 0
    var albumIndex: Int = 2
    var mediaType: Int = 0
    var favoritesOnly: Boolean = false
    var sortNewest: Boolean = true
}

/**
 * Flow 18 — My Creations / Drafts / History.
 * Production tab: My Stuff → b18_browse.
 */
@Composable
fun BoardFlow18Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    val pipeline = LocalBoardPipeline.current
    when (screenId) {
        "b18_hub", "b18_browse" -> Browse(
            onTab = tab,
            onOpen = { id ->
                BoardFlow18Session.selectedId = id
                onNavigate("b18_detail")
            },
            onSearch = { onNavigate("b18_search") },
            onTrash = { onNavigate("b18_trash") },
            onProfile = { onNavigate("b9_open") },
            onSettings = { onNavigate("b11_hub") },
            onHome = onBack,
            onBack = onBack,
            onCreate = pipeline.openMediaInput
        )
        "b18_empty" -> Browse(
            onTab = tab,
            onOpen = { },
            onSearch = { onNavigate("b18_search") },
            onTrash = { onNavigate("b18_trash") },
            onProfile = { onNavigate("b9_open") },
            onSettings = { onNavigate("b11_hub") },
            onHome = onBack,
            onBack = onBack,
            onCreate = pipeline.openMediaInput,
            forceEmpty = true
        )
        "b18_detail" -> Detail(
            onEdit = { onNavigate("b15_open") },
            onRemix = {
                val item = BoardLibraryStore.selected()
                when (item?.status) {
                    1, 2 -> onNavigate("b19_canceled") // draft / processing → recovery
                    else -> pipeline.openTell()
                }
            },
            onShare = pipeline.openShare,
            onMore = { onNavigate("b18_more") },
            onBack = { onNavigate("b18_browse") }
        )
        "b18_more" -> MoreSheet(
            onEdit = { onNavigate("b15_open") },
            onVersion = { onNavigate("b18_versions") },
            onRemix = {
                val item = BoardLibraryStore.selected()
                when (item?.status) {
                    1, 2 -> onNavigate("b19_failed")
                    else -> pipeline.openTell()
                }
            },
            onAlbum = { onNavigate("b18_album") },
            onShare = pipeline.openShare,
            onDelete = { onNavigate("b18_trash") },
            onDismiss = { onNavigate("b18_detail") }
        )
        "b18_versions" -> Versions(
            onOpen = { onNavigate("b18_detail") },
            onBack = { onNavigate("b18_detail") }
        )
        "b18_album" -> MoveAlbum(
            onMoved = { onNavigate("b18_browse") },
            onClose = { onNavigate("b18_more") }
        )
        "b18_search" -> SearchFilter(
            onApply = { onNavigate("b18_browse") },
            onClose = { onNavigate("b18_browse") }
        )
        "b18_trash" -> Trash(
            onRestored = { onNavigate("b18_browse") },
            onClose = { onNavigate("b18_browse") }
        )
        else -> Browse(
            onTab = tab,
            onOpen = { id ->
                BoardFlow18Session.selectedId = id
                onNavigate("b18_detail")
            },
            onSearch = { onNavigate("b18_search") },
            onTrash = { onNavigate("b18_trash") },
            onProfile = { onNavigate("b9_open") },
            onSettings = { onNavigate("b11_hub") },
            onHome = onBack,
            onBack = onBack,
            onCreate = pipeline.openMediaInput
        )
    }
}

@Composable
private fun rememberLibraryCreations(): List<BoardCreation> {
    val context = LocalContext.current
    val revision = BoardLibraryStore.revision
    LaunchedEffect(Unit) {
        BoardLibraryStore.hydrate(context)
    }
    return remember(revision) { BoardLibraryStore.visible() }
}

@Composable
private fun Browse(
    onTab: (BoardShellTab) -> Unit,
    onOpen: (Int) -> Unit,
    onSearch: () -> Unit,
    onTrash: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    forceEmpty: Boolean = false
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var statusTab by remember { mutableIntStateOf(BoardFlow18Session.statusTab) }
    val gridReady = true
    val all = rememberLibraryCreations()
    val filtered = if (forceEmpty) {
        emptyList()
    } else {
        all.asSequence()
            .filter {
                when (statusTab) {
                    1 -> it.status == 0
                    2 -> it.status == 1
                    3 -> it.status == 2
                    else -> true
                }
            }
            .filter {
                when (BoardFlow18Session.mediaType) {
                    1 -> !it.isVideo
                    2 -> it.isVideo
                    else -> true
                }
            }
            .filter {
                !BoardFlow18Session.favoritesOnly || BoardLibraryStore.isFavorite(it.id)
            }
            .toList()
            .let { list ->
                if (BoardFlow18Session.sortNewest) list else list.asReversed()
            }
    }
    val tabs = listOf(
        R.string.board_f18_tab_all,
        R.string.board_f18_tab_completed,
        R.string.board_f18_tab_drafts,
        R.string.board_f18_tab_processing
    )
    val columns = boardGridColumns(compact = 2, medium = 3, expanded = 4)
    BoardMainShell(selected = BoardShellTab.MyStuff, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW18_HOST)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val canPop = LocalBoardCanPop.current
                if (canPop) {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.pj_ic_back),
                            contentDescription = stringResource(R.string.cd_back),
                            tint = colors.textPrimary
                        )
                    }
                }
                BoardTitleText(
                    text = stringResource(R.string.nav_my_stuff),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCreate) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_add),
                        contentDescription = stringResource(R.string.board_empty_cta),
                        tint = colors.primary
                    )
                }
                IconButton(onClick = onSearch) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_search),
                        contentDescription = stringResource(R.string.flow18_search),
                        tint = colors.textPrimary
                    )
                }
                IconButton(onClick = onTrash) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_delete),
                        contentDescription = stringResource(R.string.flow18_trash),
                        tint = colors.textPrimary
                    )
                }
                IconButton(onClick = onProfile) {
                    Icon(painter = painterResource(R.drawable.pj_ic_person),
                        contentDescription = stringResource(R.string.nav_profile),
                        tint = colors.textPrimary
                    )
                }
                IconButton(onClick = onSettings) {
                    Icon(painter = painterResource(R.drawable.pj_ic_settings), contentDescription = stringResource(R.string.settings), tint = colors.textPrimary)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = PixiSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                tabs.forEachIndexed { i, res ->
                    val on = i == statusTab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (on) colors.primary else colors.surfaceMuted)
                            .clickable {
                                statusTab = i
                                BoardFlow18Session.statusTab = i
                            }
                            .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(res),
                            color = if (on) colors.onPrimary else colors.textSecondary,
                            style = BoardType.caption,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            when {
                !gridReady -> {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                    ) {
                        repeat(3) { BoardSkeletonCard() }
                    }
                }
                filtered.isEmpty() -> {
                    BoardEmptyState(
                        title = stringResource(R.string.projects_empty_title),
                        body = stringResource(R.string.projects_empty_desc),
                        ctaLabel = stringResource(R.string.board_empty_cta),
                        onCta = onCreate,
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    val expanded = rememberBoardTwoPane()
                    var previewIdx by remember(statusTab, forceEmpty) { mutableIntStateOf(0) }
                    LaunchedEffect(filtered.size, statusTab) {
                        if (previewIdx >= filtered.size) previewIdx = 0
                    }
                    val listPane: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                        ) {
                            items(filtered.size) { i ->
                                val item = filtered[i]
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (expanded) {
                                                previewIdx = i
                                            } else {
                                                BoardFlow18Session.selectedId = item.id
                                                onOpen(item.id)
                                            }
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(radii.card))
                                            .then(
                                                if (expanded && i == previewIdx) {
                                                    Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.card))
                                                } else {
                                                    Modifier
                                                }
                                            )
                                    ) {
                                        Image(
                                            painter = painterResource(item.image),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (BoardLibraryStore.isFavorite(item.id)) {
                                            Icon(painter = painterResource(R.drawable.pj_ic_heart),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(8.dp)
                                                    .size(18.dp)
                                            )
                                        }
                                        if (item.isVideo) {
                                            Icon(
                                                painter = painterResource(R.drawable.pj_ic_play),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(0.45f))
                                                    .padding(4.dp)
                                            )
                                        }
                                        if (item.isCollage) {
                                            Icon(painter = painterResource(R.drawable.pj_ic_grid),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(8.dp)
                                            )
                                        }
                                        Icon(painter = painterResource(R.drawable.pj_ic_more),
                                            contentDescription = stringResource(R.string.flow18_more),
                                            tint = Color.White,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(0.35f))
                                                .clickable {
                                                    BoardFlow18Session.selectedId = item.id
                                                    onOpen(item.id)
                                                }
                                                .padding(4.dp)
                                                .size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(PixiSpacing.xs))
                                    Text(
                                        BoardLibraryStore.displayTitle(context, item),
                                        style = BoardType.body,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    BoardCaptionText(text = stringResource(item.dateRes))
                                }
                            }
                        }
                    }
                    val detailPane: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
                        val item = filtered[previewIdx.coerceIn(0, filtered.lastIndex)]
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(PixiSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                        ) {
                            Image(
                                painter = painterResource(item.image),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(radii.card)),
                                contentScale = ContentScale.Crop
                            )
                            BoardTitleText(text = BoardLibraryStore.displayTitle(context, item))
                            BoardPrimaryButton(
                                text = stringResource(R.string.flow18_open_version),
                                onClick = {
                                    BoardFlow18Session.selectedId = item.id
                                    onOpen(item.id)
                                }
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (expanded) {
                            BoardWideFrame {
                                BoardListDetail(
                                    showDetail = true,
                                    list = listPane,
                                    detail = detailPane
                                )
                            }
                        } else {
                            BoardListDetail(
                                showDetail = false,
                                list = listPane,
                                detail = detailPane
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Detail(
    onEdit: () -> Unit,
    onRemix: () -> Unit,
    onShare: () -> Unit,
    onMore: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val items = rememberLibraryCreations()
    val item = items.find { it.id == BoardFlow18Session.selectedId } ?: items.firstOrNull() ?: return
    val tags = listOf(
        R.string.board_f18_tag_ai,
        R.string.board_f18_tag_portrait,
        R.string.board_f18_tag_happy
    )
    val actions = listOf(
        DetailAction(iconRes = R.drawable.pj_ic_edit, label = R.string.flow18_edit, action = onEdit),
        DetailAction(icon = R.drawable.pj_ic_sparkles, label = R.string.result_remix, action = onRemix),
        DetailAction(iconRes = R.drawable.pj_ic_share, label = R.string.share, action = onShare),
        DetailAction(icon = R.drawable.pj_ic_more, label = R.string.flow18_more, action = onMore)
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
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
                BoardTitleText(
                    text = stringResource(R.string.board_f18_detail_title),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onMore) {
                    Icon(painter = painterResource(R.drawable.pj_ic_more),
                        contentDescription = stringResource(R.string.flow18_more),
                        tint = colors.textPrimary
                    )
                }
            }
            Image(
                painter = painterResource(item.image),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            BoardTitleText(text = BoardLibraryStore.displayTitle(context, item))
            BoardCaptionText(text = stringResource(item.dateRes))
            if (BoardLibraryStore.isFavorite(item.id)) {
                BoardCaptionText(
                    text = stringResource(R.string.board_f18_favorited),
                    color = colors.primary
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                tags.forEach { res ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(colors.primary.copy(0.12f))
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.xs)
                    ) {
                        BoardCaptionText(text = stringResource(res), color = colors.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                actions.forEach { itemAction ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(onClick = itemAction.action)
                    ) {
                        if (itemAction.iconRes != null) {
                            Icon(
                                painter = painterResource(itemAction.iconRes),
                                contentDescription = null,
                                tint = colors.primary
                            )
                        } else if (itemAction.icon != null) {
                            Icon(painter = painterResource(itemAction.icon), contentDescription = null, tint = colors.primary)
                        }
                        BoardCaptionText(text = stringResource(itemAction.label))
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreSheet(
    onEdit: () -> Unit,
    onVersion: () -> Unit,
    onRemix: () -> Unit,
    onAlbum: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val revision = BoardLibraryStore.revision
    val item = remember(revision) { BoardLibraryStore.selected() }
    var showRename by remember { mutableStateOf(false) }
    var renameText by remember(item?.id) {
        mutableStateOf(item?.let { BoardLibraryStore.displayTitle(context, it) }.orEmpty())
    }
    val fav = item != null && BoardLibraryStore.isFavorite(item.id)
    val actions = listOf(
        ActionTriple(R.drawable.pj_ic_edit, R.string.flow18_edit, onEdit),
        ActionTriple(R.drawable.pj_ic_copy, R.string.flow18_new_version, onVersion),
        ActionTriple(R.drawable.pj_ic_sparkles, R.string.board_f18_remix_ai, onRemix),
        ActionTriple(R.drawable.pj_ic_copy, R.string.board_f18_save_copy, {
            val id = item?.id ?: return@ActionTriple
            scope.launch {
                BoardLibraryStore.saveCopy(context, id)
                Toast.makeText(context, context.getString(R.string.board_f18_copy_saved), Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        }),
        ActionTriple(R.drawable.pj_ic_folder, R.string.flow18_move_album, onAlbum),
        ActionTriple(
            if (fav) R.drawable.pj_ic_heart else R.drawable.pj_ic_heart,
            if (fav) R.string.board_f18_unfavorited else R.string.flow18_favorite,
            {
                val id = item?.id ?: return@ActionTriple
                scope.launch {
                    val nowFav = BoardLibraryStore.toggleFavorite(context, id)
                    Toast.makeText(
                        context,
                        context.getString(
                            if (nowFav) R.string.board_f18_favorited else R.string.board_f18_unfavorited
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                    onDismiss()
                }
            }
        ),
        ActionTriple(R.drawable.pj_ic_download, R.string.flow18_download, {
            val id = item?.id ?: return@ActionTriple
            val ok = BoardLibraryStore.download(context, id)
            Toast.makeText(
                context,
                context.getString(
                    if (ok) R.string.board_f18_downloaded else R.string.board_soft_preview_title
                ),
                Toast.LENGTH_SHORT
            ).show()
            if (ok) onDismiss()
        }),
        ActionTriple(R.drawable.pj_ic_share, R.string.share, onShare),
        ActionTriple(R.drawable.pj_ic_edit, R.string.flow18_rename, {
            showRename = true
        }),
        ActionTriple(R.drawable.pj_ic_delete, R.string.delete_creation, {
            val id = item?.id ?: return@ActionTriple
            scope.launch {
                BoardLibraryStore.moveToTrash(context, id)
                Toast.makeText(context, context.getString(R.string.board_f18_moved_trash), Toast.LENGTH_SHORT).show()
                onDelete()
            }
        })
    )
    if (showRename && item != null) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text(stringResource(R.string.flow18_rename)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            BoardLibraryStore.rename(context, item.id, renameText)
                            Toast.makeText(
                                context,
                                context.getString(R.string.board_f18_renamed),
                                Toast.LENGTH_SHORT
                            ).show()
                            showRename = false
                            onDismiss()
                        }
                    }
                ) { Text(stringResource(R.string.done)) }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
    BoardAdaptiveFrame {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.35f))
                .clickable(onClick = onDismiss)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(colors.surface)
                    .clickable(enabled = false) {}
                    .padding(PixiSpacing.xl)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.xs)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.surfaceMuted)
                )
                Text(
                    stringResource(R.string.flow18_more),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(vertical = PixiSpacing.sm)
                )
                actions.forEach { row ->
                    val danger = row.label == R.string.delete_creation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(radii.md))
                            .clickable(onClick = row.action)
                            .padding(vertical = PixiSpacing.md, horizontal = PixiSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                    ) {
                        Icon(painter = painterResource(row.icon), null, tint = if (danger) colors.danger else colors.textPrimary)
                        Text(
                            stringResource(row.label),
                            color = if (danger) colors.danger else colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Versions(onOpen: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlow18Session.versionIndex) }
    val versions = listOf(
        R.string.board_f18_ver_current to R.string.board_f18_ver_time_4,
        R.string.board_f18_ver_3 to R.string.board_f18_ver_time_3,
        R.string.board_f18_ver_2 to R.string.board_f18_ver_time_2,
        R.string.board_f18_ver_original to R.string.board_f18_ver_time_1
    )
    val items = rememberLibraryCreations()
    val hero = (items.find { it.id == BoardFlow18Session.selectedId } ?: items.firstOrNull())?.image
        ?: BoardFlowArt.baAfter
    val shortViewport = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 520
    BoardAdaptiveFrame {
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
                    stringResource(R.string.flow18_versions),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                versions.forEachIndexed { i, (title, time) ->
                    val on = i == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(radii.card))
                            .background(if (on) colors.primary.copy(0.12f) else colors.surfaceMuted)
                            .border(
                                width = if (on) 2.dp else 0.dp,
                                color = if (on) colors.primary else Color.Transparent,
                                shape = RoundedCornerShape(radii.card)
                            )
                            .clickable {
                                selected = i
                                BoardFlow18Session.versionIndex = i
                            }
                            .padding(if (shortViewport) PixiSpacing.sm else PixiSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                    ) {
                        Image(
                            painter = painterResource(hero),
                            contentDescription = null,
                            modifier = Modifier
                                .size(if (shortViewport) 40.dp else 56.dp)
                                .clip(RoundedCornerShape(radii.md)),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(title), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            Text(stringResource(time), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                        if (i == 0) {
                            Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success)
                        }
                    }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.flow18_open_version),
                onClick = onOpen
            )
        }
    }
}

@Composable
private fun MoveAlbum(onMoved: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember {
        mutableIntStateOf(
            BoardLibraryStore.albumOf(BoardFlow18Session.selectedId).takeIf { it > 0 }
                ?: BoardFlow18Session.albumIndex
        )
    }
    val albums = listOf(
        AlbumItem(R.string.board_f18_album_all, 245),
        AlbumItem(R.string.board_f18_album_fav, 32),
        AlbumItem(R.string.board_f18_album_family, 46),
        AlbumItem(R.string.board_f18_album_travel, 17),
        AlbumItem(R.string.board_f18_album_birthday, 12),
        AlbumItem(R.string.board_f18_album_memes, 21),
        AlbumItem(R.string.board_f18_album_ai, 18),
        AlbumItem(R.string.board_f18_album_stories, 9)
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.flow18_move_album),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(painter = painterResource(R.drawable.pj_ic_close), contentDescription = stringResource(R.string.cd_back))
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .border(1.dp, colors.primary, RoundedCornerShape(radii.card))
                    .clickable {
                        Toast.makeText(context, context.getString(R.string.board_f18_album_created), Toast.LENGTH_SHORT).show()
                    }
                    .padding(PixiSpacing.lg),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_add), null, tint = colors.primary)
                    Text(
                        stringResource(R.string.board_f18_create_album),
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                albums.forEachIndexed { i, album ->
                    val on = i == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(radii.card))
                            .background(if (on) colors.primary.copy(0.12f) else colors.surfaceMuted)
                            .clickable {
                                selected = i
                                BoardFlow18Session.albumIndex = i
                            }
                            .padding(PixiSpacing.lg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(R.drawable.pj_ic_folder), null, tint = colors.primary)
                        Spacer(modifier = Modifier.size(PixiSpacing.md))
                        Text(
                            stringResource(album.title) + " (${album.count})",
                            modifier = Modifier.weight(1f),
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        if (on) {
                            Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.primary)
                        }
                    }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f18_move_here),
                onClick = {
                    val id = BoardFlow18Session.selectedId
                    scope.launch {
                        BoardLibraryStore.setAlbum(context, id, selected)
                        Toast.makeText(context, context.getString(R.string.board_f18_moved), Toast.LENGTH_SHORT).show()
                        onMoved()
                    }
                }
            )
        }
    }
}

@Composable
private fun SearchFilter(onApply: () -> Unit, onClose: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var query by remember { mutableStateOf("") }
    var mediaType by remember { mutableIntStateOf(BoardFlow18Session.mediaType) }
    var favoritesOnly by remember { mutableStateOf(BoardFlow18Session.favoritesOnly) }
    var sortNewest by remember { mutableStateOf(BoardFlow18Session.sortNewest) }
    val mediaTypes = listOf(
        R.string.board_f18_media_all,
        R.string.board_f18_media_image,
        R.string.board_f18_media_video,
        R.string.board_f18_media_gif
    )
    val filters = listOf(
        R.string.board_f18_filter_date,
        R.string.board_f18_filter_type,
        R.string.board_f18_filter_style,
        R.string.board_f18_filter_album
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
                Text(
                    stringResource(R.string.flow18_search),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(painter = painterResource(R.drawable.pj_ic_close), contentDescription = stringResource(R.string.cd_back))
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.board_f18_search_placeholder)) },
                leadingIcon = { Icon(painter = painterResource(R.drawable.pj_ic_search), contentDescription = null) },
                shape = RoundedCornerShape(radii.md),
                singleLine = true
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                mediaTypes.forEachIndexed { i, res ->
                    val on = i == mediaType
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (on) colors.primary else colors.surfaceMuted)
                            .clickable { mediaType = i }
                            .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(res),
                            color = if (on) colors.onPrimary else colors.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            filters.forEach { res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(colors.surfaceMuted)
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(res), modifier = Modifier.weight(1f), color = colors.textPrimary)
                    Text(stringResource(R.string.board_f18_any), color = colors.textSecondary)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.board_f18_fav_only), modifier = Modifier.weight(1f), color = colors.textPrimary)
                Switch(checked = favoritesOnly, onCheckedChange = { favoritesOnly = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.board_f18_sort_newest), modifier = Modifier.weight(1f), color = colors.textPrimary)
                Switch(checked = sortNewest, onCheckedChange = { sortNewest = it })
            }
            BoardPrimaryButton(
                text = stringResource(R.string.flow18_apply_filters),
                onClick = {
                    BoardFlow18Session.mediaType = mediaType
                    BoardFlow18Session.favoritesOnly = favoritesOnly
                    BoardFlow18Session.sortNewest = sortNewest
                    onApply()
                }
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun Trash(onRestored: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val revision = BoardLibraryStore.revision
    LaunchedEffect(Unit) { BoardLibraryStore.hydrate(context) }
    val items = remember(revision) { BoardLibraryStore.trashItems() }
    val selectedIds = remember { mutableStateListOf<Int>() }
    LaunchedEffect(items.map { it.id }) {
        selectedIds.clear()
        selectedIds.addAll(items.map { it.id })
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.flow18_trash_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(painter = painterResource(R.drawable.pj_ic_close), contentDescription = stringResource(R.string.cd_back))
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.danger.copy(0.12f))
                    .padding(PixiSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_delete), null, tint = colors.danger)
                Text(
                    stringResource(R.string.flow18_trash_body),
                    color = colors.danger,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            if (items.isEmpty()) {
                BoardEmptyState(
                    title = stringResource(R.string.projects_empty_title),
                    body = stringResource(R.string.flow18_trash_body),
                    ctaLabel = stringResource(R.string.close),
                    onCta = onClose,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    items.forEach { item ->
                        val checked = selectedIds.contains(item.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(radii.card))
                                .background(colors.surfaceMuted)
                                .clickable {
                                    if (checked) selectedIds.remove(item.id) else selectedIds.add(item.id)
                                }
                                .padding(PixiSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                        ) {
                            Image(
                                painter = painterResource(item.image),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(radii.md)),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    BoardLibraryStore.displayTitle(context, item),
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    stringResource(item.dateRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (it) selectedIds.add(item.id) else selectedIds.remove(item.id)
                                }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    BoardOutlinedButton(
                        text = stringResource(R.string.board_f18_restore_count, selectedIds.size),
                        onClick = {
                            val ids = selectedIds.toList()
                            scope.launch {
                                BoardLibraryStore.restore(context, ids)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.board_f18_restored),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onRestored()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        BoardPrimaryButton(
                            text = stringResource(R.string.flow18_delete_forever),
                            onClick = {
                                val ids = selectedIds.toList()
                                scope.launch {
                                    BoardLibraryStore.deleteForever(context, ids)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.board_f18_deleted_forever),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onRestored()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
