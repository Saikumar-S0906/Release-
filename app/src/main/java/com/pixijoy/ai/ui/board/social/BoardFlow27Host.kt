package com.pixijoy.ai.ui.board.social

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.rememberKidsSpace
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardBodyText
import com.pixijoy.ai.ui.board.components.BoardCaptionText
import com.pixijoy.ai.ui.board.components.BoardFilterPill
import com.pixijoy.ai.ui.board.components.BoardEmptyState
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardScrollCue
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardTabViewport
import com.pixijoy.ai.ui.board.components.BoardTitleText
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.components.BoardWideFrame
import com.pixijoy.ai.ui.board.components.boardGridColumns
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.launch

private val Thumbs = BoardFlowArt.feedThumbs

/** Flow 27 session — feed tab / follow / share visibility. Soft social only. */
internal object BoardFlow27Session {
    var feedTab: Int = 0
    var trendingFilter: Int = 0
    var categoryIndex: Int = 0
    var selectedCreator: Int = 0
    var followed: Boolean = false
    var publicPost: Boolean = true
    var caption: String = ""
    var selectedPost: Int = 0
}

/**
 * Flow 27 — Community / UGC.
 * Social actions are soft (local UI); no live network posts.
 */
@Composable
fun BoardFlow27Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val pipeline = LocalBoardPipeline.current
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    val kidsSpace = rememberKidsSpace()
    when (screenId) {
        "b27_kids_blocked" -> KidsCommunityBlocked(
            onSettings = { onNavigate("b11_prefs") },
            onHome = { onNavigate(BoardProductionTabs.HOME) },
            onTab = tab
        )
        "b27_hub", "b27_feed" -> {
            if (kidsSpace) {
                KidsCommunityBlocked(
                    onSettings = { onNavigate("b11_prefs") },
                    onHome = { onNavigate(BoardProductionTabs.HOME) },
                    onTab = tab
                )
            } else {
                FeedHub(
                    onTrending = { onNavigate("b27_trending") },
                    onCategories = { onNavigate("b27_categories") },
                    onPost = { i ->
                        BoardFlow27Session.selectedPost = i
                        onNavigate("b27_post")
                    },
                    onCreator = { i ->
                        BoardFlow27Session.selectedCreator = i
                        onNavigate("b27_creator")
                    },
                    onPeople = { onNavigate("b27_people") },
                    onShare = { onNavigate("b27_share") },
                    onGuidelines = { onNavigate("b27_guidelines") },
                    onNotif = { onNavigate("b25_center") },
                    onCreate = { onNavigate(BoardProductionTabs.CREATE) },
                    onTab = tab
                )
            }
        }
        "b27_trending" -> TrendingGrid(
            onPost = { i ->
                BoardFlow27Session.selectedPost = i
                onNavigate("b27_post")
            },
            onBack = { onNavigate("b27_feed") }
        )
        "b27_categories" -> CategoriesGrid(
            onPick = { i ->
                BoardFlow27Session.categoryIndex = i
                onNavigate("b27_trending")
            },
            onBack = { onNavigate("b27_feed") }
        )
        "b27_post" -> PostDetail(
            onCreator = { onNavigate("b27_creator") },
            onRemix = pipeline.openMediaInput,
            onShare = { onNavigate("b27_share") },
            onBack = { onNavigate("b27_feed") }
        )
        "b27_creator", "b27_creators" -> CreatorProfile(
            onPeople = { onNavigate("b27_people") },
            onPost = { i ->
                BoardFlow27Session.selectedPost = i
                onNavigate("b27_post")
            },
            onBack = { onNavigate("b27_feed") }
        )
        "b27_people" -> {
            if (kidsSpace) {
                KidsCommunityBlocked(
                    onSettings = { onNavigate("b11_prefs") },
                    onHome = { onNavigate(BoardProductionTabs.HOME) },
                    onTab = tab
                )
            } else {
                FindPeople(
                    onCreator = { i ->
                        BoardFlow27Session.selectedCreator = i
                        onNavigate("b27_creator")
                    },
                    onBack = { onNavigate("b27_feed") }
                )
            }
        }
        "b27_share" -> ShareToCommunity(
            onShared = { onNavigate("b27_feed") },
            onBack = { onNavigate("b27_post") }
        )
        "b27_guidelines" -> Guidelines(
            onDone = { onNavigate("b27_feed") },
            onBack = { onNavigate("b27_feed") }
        )
        else -> {
            if (kidsSpace) {
                KidsCommunityBlocked(
                    onSettings = { onNavigate("b11_prefs") },
                    onHome = { onNavigate(BoardProductionTabs.HOME) },
                    onTab = tab
                )
            } else {
                FeedHub(
                    onTrending = { onNavigate("b27_trending") },
                    onCategories = { onNavigate("b27_categories") },
                    onPost = { i ->
                        BoardFlow27Session.selectedPost = i
                        onNavigate("b27_post")
                    },
                    onCreator = { i ->
                        BoardFlow27Session.selectedCreator = i
                        onNavigate("b27_creator")
                    },
                    onPeople = { onNavigate("b27_people") },
                    onShare = { onNavigate("b27_share") },
                    onGuidelines = { onNavigate("b27_guidelines") },
                    onNotif = { onNavigate("b25_center") },
                    onCreate = { onNavigate(BoardProductionTabs.CREATE) },
                    onTab = tab
                )
            }
        }
    }
}

@Composable
private fun KidsCommunityBlocked(
    onSettings: () -> Unit,
    onHome: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    BoardMainShell(selected = BoardShellTab.Community, onTab = onTab) {
        BoardAdaptiveFrame {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PixiSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(PixiSpacing.xl))
                Icon(painter = painterResource(R.drawable.pj_ic_shield),
                    contentDescription = null,
                    tint = BoardTheme.colors.primary,
                    modifier = Modifier.size(56.dp)
                )
                BoardTitleText(text = stringResource(R.string.board_kids_community_title))
                BoardBodyText(text = stringResource(R.string.board_kids_community_body))
                BoardPrimaryButton(
                    text = stringResource(R.string.board_f11_settings),
                    onClick = onSettings
                )
                BoardOutlinedButton(
                    text = stringResource(R.string.flow19_go_home),
                    onClick = onHome
                )
            }
        }
    }
}

@Composable
private fun FeedHub(
    onTrending: () -> Unit,
    onCategories: () -> Unit,
    onPost: (Int) -> Unit,
    onCreator: (Int) -> Unit,
    onPeople: () -> Unit,
    onShare: () -> Unit,
    onGuidelines: () -> Unit,
    onNotif: () -> Unit,
    onCreate: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var feedTab by remember { mutableIntStateOf(BoardFlow27Session.feedTab) }
    var category by remember { mutableIntStateOf(BoardFlow27Session.categoryIndex) }
    val feedTabs = listOf(
        R.string.board_f27_for_you,
        R.string.board_f27_following,
        R.string.board_f27_trending_tab,
        R.string.board_f27_new
    )
    val feedCats = listOf(
        CommunityCat.All,
        CommunityCat.Ai,
        CommunityCat.Pets,
        CommunityCat.Travel,
        CommunityCat.Quotes
    )
    val followingEmpty = feedTab == 1
    val feedPosts = remember(category, feedTab) {
        val sort = when (feedTab) {
            2 -> 1
            3 -> 2
            else -> 0
        }
        communityFilter(communityFromIndex(category), sort)
    }
    val scroll = rememberScrollState()
    BoardMainShell(selected = BoardShellTab.Community, onTab = onTab) {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW27_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(BoardFlowArt.logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(32.dp)
                )
                BoardTitleText(
                    text = stringResource(R.string.flow27_title),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = PixiSpacing.sm)
                )
                IconButton(onClick = onNotif) {
                    Icon(painter = painterResource(R.drawable.pj_ic_notifications),
                        contentDescription = stringResource(R.string.flow25_title),
                        tint = colors.textPrimary
                    )
                }
                Image(
                    painter = painterResource(BoardFlowArt.profileAvatar),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onCreator(0) },
                    contentScale = ContentScale.Crop
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                feedTabs.forEachIndexed { i, label ->
                    val active = feedTab == i
                    Text(
                        stringResource(label),
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (active) colors.primary else colors.surfaceMuted)
                            .clickable {
                                feedTab = i
                                BoardFlow27Session.feedTab = i
                            }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                        color = if (active) colors.onPrimary else colors.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        style = BoardType.caption
                    )
                }
            }
            if (followingEmpty) {
                BoardEmptyState(
                    title = stringResource(R.string.board_community_following_empty_title),
                    body = stringResource(R.string.board_community_following_empty_body),
                    ctaLabel = stringResource(R.string.flow27_find_people),
                    onCta = onPeople,
                    modifier = Modifier.fillMaxWidth(),
                    heroRes = null
                )
            } else {
                Box {
                    Image(
                        painter = painterResource(BoardFlowArt.feedThumb),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(BoardTabViewport.HeroAspect)
                            .clip(RoundedCornerShape(radii.card))
                            .clickable { onPost(0) },
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        stringResource(R.string.flow27_featured),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(PixiSpacing.lg),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    feedCats.forEach { cat ->
                        BoardFilterPill(
                            text = stringResource(communityLabel(cat)),
                            selected = cat.ordinal == category,
                            onClick = {
                                category = cat.ordinal
                                BoardFlow27Session.categoryIndex = cat.ordinal
                            }
                        )
                    }
                    BoardFilterPill(
                        text = stringResource(R.string.flow27_categories),
                        selected = false,
                        onClick = onCategories
                    )
                }
                BoardTitleText(text = stringResource(R.string.flow27_creators))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    BoardFlowArt.creatorFaces.take(5).forEachIndexed { i, face ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onCreator(i) }
                        ) {
                            Image(
                                painter = painterResource(face),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            BoardCaptionText(text = stringResource(R.string.board_f27_creator_n, i + 1))
                        }
                    }
                }
                feedPosts.take(6).forEach { post ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(radii.card))
                            .background(colors.surfaceMuted)
                            .clickable { onPost(post.id) }
                            .padding(PixiSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                        ) {
                            Image(
                                painter = painterResource(
                                    BoardFlowArt.creatorFaces[post.id % BoardFlowArt.creatorFaces.size]
                                ),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                stringResource(R.string.board_f27_creator_n, post.id + 1),
                                style = BoardType.caption,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            BoardCaptionText(text = stringResource(R.string.board_f27_stats))
                        }
                        Image(
                            painter = painterResource(post.art),
                            contentDescription = stringResource(post.titleRes),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.2f)
                                .clip(RoundedCornerShape(radii.md)),
                            contentScale = ContentScale.Crop
                        )
                        BoardBodyText(
                            text = stringResource(post.titleRes),
                            color = colors.textPrimary
                        )
                    }
                }
                BoardPrimaryButton(
                    text = stringResource(R.string.board_f27_spread_joy),
                    onClick = onShare
                )
                BoardOutlinedButton(
                    text = stringResource(R.string.flow27_find_people),
                    onClick = onPeople
                )
                BoardOutlinedButton(
                    text = stringResource(R.string.flow27_guidelines),
                    onClick = onGuidelines
                )
            }
            }
            BoardScrollCue(
                canScrollForward = scroll.canScrollForward,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun TrendingGrid(onPost: (Int) -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var filter by remember { mutableIntStateOf(BoardFlow27Session.trendingFilter) }
    val filters = listOf(
        R.string.board_f27_trending_tab,
        R.string.board_f27_most_liked,
        R.string.board_f27_recent,
        R.string.board_f27_editors
    )
    val columns = boardGridColumns(compact = 2, medium = 2, expanded = 3)
    val posts = remember(filter, BoardFlow27Session.categoryIndex) {
        communityFilter(communityFromIndex(BoardFlow27Session.categoryIndex), filter)
    }
    BoardWideFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW27_HOST)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(communityLabel(communityFromIndex(BoardFlow27Session.categoryIndex))),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(painter = painterResource(R.drawable.pj_ic_search), null, tint = colors.textSecondary)
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                filters.forEachIndexed { i, label ->
                    val active = filter == i
                    Text(
                        stringResource(label),
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (active) colors.primary else colors.surfaceMuted)
                            .clickable {
                                filter = i
                                BoardFlow27Session.trendingFilter = i
                            }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                        color = if (active) colors.onPrimary else colors.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            if (posts.isEmpty()) {
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
                    items(posts, key = { it.id }) { post ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(0.75f)
                                .clip(RoundedCornerShape(radii.card))
                                .clickable { onPost(post.id) }
                        ) {
                            Image(
                                painter = painterResource(post.art),
                                contentDescription = stringResource(post.titleRes),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(PixiSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(painter = painterResource(R.drawable.pj_ic_heart), null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text(
                                    if (post.likes >= 1000) "${post.likes / 1000}.${(post.likes % 1000) / 100}K" else "${post.likes}",
                                    color = Color.White,
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
private fun CategoriesGrid(onPick: (Int) -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val cats = CommunityCat.entries.filter { it != CommunityCat.Food }
    val columns = boardGridColumns(compact = 2, medium = 2, expanded = 3)
    BoardWideFrame {
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
                    stringResource(R.string.flow27_categories),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(painter = painterResource(R.drawable.pj_ic_search), null, tint = colors.textSecondary)
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                items(cats.size) { i ->
                    val cat = cats[i]
                    val cover = communityFilter(cat, 0).firstOrNull()?.art ?: BoardFlowArt.imageFail
                    Box(
                        modifier = Modifier
                            .aspectRatio(1.3f)
                            .clip(RoundedCornerShape(radii.card))
                            .clickable { onPick(cat.ordinal) }
                    ) {
                        Image(
                            painter = painterResource(cover),
                            contentDescription = stringResource(communityLabel(cat)),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colors.textPrimary.copy(0.35f))
                        )
                        Text(
                            stringResource(communityLabel(cat)),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(PixiSpacing.md),
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostDetail(
    onCreator: () -> Unit,
    onRemix: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val postId = "f27_post_${BoardFlow27Session.selectedPost}"
    val creatorId = "f27_creator_doggo"
    val revision = BoardSocialStore.revision
    LaunchedEffect(Unit) { BoardSocialStore.hydrate(context) }
    var liked by remember(revision) { mutableStateOf(BoardSocialStore.isLiked(postId)) }
    var bookmarked by remember(revision) { mutableStateOf(BoardSocialStore.isBookmarked(postId)) }
    var followed by remember(revision) {
        mutableStateOf(BoardSocialStore.isFollowed(creatorId) || BoardFlow27Session.followed)
    }
    var comment by remember { mutableStateOf("") }
    val post = communityCatalog().find { it.id == BoardFlow27Session.selectedPost }
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
                    stringResource(R.string.board_f27_post_details),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(painter = painterResource(R.drawable.pj_ic_more), null, tint = colors.textSecondary)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                Box {
                    Image(
                        painter = painterResource(post?.art ?: BoardFlowArt.feedThumb),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(radii.card)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        stringResource(R.string.flow27_post_media),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(PixiSpacing.md)
                            .clip(RoundedCornerShape(radii.pill))
                            .background(Color.Black.copy(0.5f))
                            .padding(horizontal = PixiSpacing.sm, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(BoardFlowArt.profileAvatar),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onCreator),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = PixiSpacing.md)
                            .clickable(onClick = onCreator)
                    ) {
                        Text(
                            stringResource(R.string.board_f27_creator_n, (post?.id ?: 0) + 1),
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            stringResource(R.string.flow27_creator_bio),
                            color = colors.textSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    BoardOutlinedButton(
                        text = stringResource(if (followed) R.string.board_f27_following_btn else R.string.flow27_follow),
                        onClick = {
                            scope.launch {
                                followed = BoardSocialStore.toggleFollow(context, creatorId)
                                BoardFlow27Session.followed = followed
                            }
                        }
                    )
                }
                Text(
                    stringResource(post?.titleRes ?: R.string.flow27_caption),
                    color = colors.textPrimary
                )
                Text(
                    stringResource(R.string.board_saved_on_device),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                liked = BoardSocialStore.toggleLike(context, postId)
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.pj_ic_heart),
                            contentDescription = null,
                            tint = if (liked) colors.danger else colors.textPrimary
                        )
                    }
                    IconButton(
                        onClick = {
                            Toast.makeText(
                                context,
                                context.getString(R.string.board_chat_saved_local),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) { Icon(painter = painterResource(R.drawable.pj_ic_chat), null) }
                    IconButton(onClick = onShare) { Icon(painter = painterResource(R.drawable.pj_ic_share), null) }
                    IconButton(
                        onClick = {
                            scope.launch {
                                bookmarked = BoardSocialStore.toggleBookmark(context, postId)
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.pj_ic_bookmark),
                            contentDescription = null,
                            tint = if (bookmarked) colors.primary else colors.textPrimary
                        )
                    }
                }
                Text(stringResource(R.string.board_f27_stats), color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.board_f27_view_comments), color = colors.primary, style = MaterialTheme.typography.labelMedium)
                listOf(
                    R.string.board_f27_comment_1,
                    R.string.board_f27_comment_2
                ).forEach { res ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = PixiSpacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(colors.primary.copy(0.2f))
                        )
                        Text(stringResource(res), color = colors.textPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                BoardOutlinedButton(
                    text = stringResource(R.string.flow27_remix),
                    onClick = onRemix
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(BoardFlowArt.profileAvatar),
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = PixiSpacing.sm),
                    placeholder = { Text(stringResource(R.string.board_f27_add_comment)) },
                    singleLine = true,
                    shape = RoundedCornerShape(radii.pill)
                )
                IconButton(
                    onClick = {
                        if (comment.isNotBlank()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.board_chat_saved_local),
                                Toast.LENGTH_SHORT
                            ).show()
                            comment = ""
                        }
                    }
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_send), null, tint = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun CreatorProfile(
    onPeople: () -> Unit,
    onPost: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val creatorId = "f27_creator_doggo"
    val revision = BoardSocialStore.revision
    LaunchedEffect(Unit) { BoardSocialStore.hydrate(context) }
    var tab by remember { mutableIntStateOf(0) }
    var followed by remember(revision) {
        mutableStateOf(BoardSocialStore.isFollowed(creatorId) || BoardFlow27Session.followed)
    }
    val tabs = listOf(
        R.string.board_f27_tab_creations,
        R.string.board_f27_tab_liked,
        R.string.board_f27_tab_collections
    )
    val columns = boardGridColumns(compact = 2, medium = 2, expanded = 3)
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
                    stringResource(R.string.board_f27_creator_profile),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onPeople) {
                    Icon(painter = painterResource(R.drawable.pj_ic_person_add), null)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                Image(
                    painter = painterResource(BoardFlowArt.profileAvatar),
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text(
                    stringResource(R.string.board_f27_creator_n, BoardFlow27Session.selectedCreator + 1),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Text(stringResource(R.string.flow27_creator_bio), color = colors.textSecondary, textAlign = TextAlign.Center)
                Text(stringResource(R.string.flow27_creator_stats), color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                BoardPrimaryButton(
                    text = stringResource(if (followed) R.string.board_f27_following_btn else R.string.flow27_follow),
                    onClick = {
                        scope.launch {
                            followed = BoardSocialStore.toggleFollow(context, creatorId)
                            BoardFlow27Session.followed = followed
                        }
                    }
                )
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
                                .clickable { tab = i }
                                .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                            color = if (active) colors.onPrimary else colors.textSecondary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                val creatorPosts = remember(tab) {
                    val all = communityCatalog()
                    when (tab) {
                        1 -> all.sortedByDescending { it.likes }
                        2 -> all.filter { it.editorPick }
                        else -> all
                    }.take(9)
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                    userScrollEnabled = false
                ) {
                    items(creatorPosts, key = { it.id }) { item ->
                        Image(
                            painter = painterResource(item.art),
                            contentDescription = stringResource(item.titleRes),
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(radii.md))
                                .clickable { onPost(item.id) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FindPeople(onCreator: (Int) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val revision = BoardSocialStore.revision
    LaunchedEffect(Unit) { BoardSocialStore.hydrate(context) }
    var query by remember { mutableStateOf("") }
    val people = listOf(
        Triple(R.string.board_f27_person_maya, R.string.board_f27_person_maya_tags, "f27_person_maya"),
        Triple(R.string.board_f27_person_alex, R.string.board_f27_person_alex_tags, "f27_person_alex"),
        Triple(R.string.board_f27_person_priya, R.string.board_f27_person_priya_tags, "f27_person_priya")
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
                    stringResource(R.string.board_f27_following_list),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.flow27_search_people)) },
                leadingIcon = { Icon(painter = painterResource(R.drawable.pj_ic_search), null) },
                shape = RoundedCornerShape(radii.md),
                singleLine = true
            )
            Text(stringResource(R.string.flow27_suggested), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            people.forEachIndexed { i, (name, tags, id) ->
                val followed = remember(revision, id) { BoardSocialStore.isFollowed(id) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .clickable { onCreator(i) }
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(BoardFlowArt.profileAvatar),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = PixiSpacing.md)
                    ) {
                        Text(stringResource(name), fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text(stringResource(tags), color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                    BoardOutlinedButton(
                        text = stringResource(
                            if (followed) R.string.board_f27_following_btn else R.string.flow27_follow
                        ),
                        onClick = {
                            scope.launch { BoardSocialStore.toggleFollow(context, id) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareToCommunity(onShared: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val defaultCaption = stringResource(R.string.flow27_caption_sample)
    var caption by remember {
        mutableStateOf(BoardFlow27Session.caption.ifBlank { defaultCaption })
    }
    var publicPost by remember { mutableStateOf(BoardFlow27Session.publicPost) }
    val post = communityCatalog().find { it.id == BoardFlow27Session.selectedPost }
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
                    stringResource(R.string.flow27_share_cta),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Text(
                stringResource(R.string.board_demo_share),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Image(
                painter = painterResource(post?.art ?: BoardFlowArt.feedThumb),
                contentDescription = post?.titleRes?.let { stringResource(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            OutlinedTextField(
                value = caption,
                onValueChange = {
                    caption = it
                    BoardFlow27Session.caption = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.flow27_caption_label)) },
                minLines = 3,
                shape = RoundedCornerShape(radii.md)
            )
            listOf(
                R.string.flow27_add_location,
                R.string.flow27_tag_people,
                R.string.flow27_add_collection
            ).forEach { res ->
                Text(
                    stringResource(res),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(colors.surfaceMuted)
                        .padding(PixiSpacing.lg),
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(stringResource(R.string.board_f27_visibility), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = publicPost, onClick = {
                    publicPost = true
                    BoardFlow27Session.publicPost = true
                })
                Text(stringResource(R.string.flow27_public), color = colors.textPrimary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !publicPost, onClick = {
                    publicPost = false
                    BoardFlow27Session.publicPost = false
                })
                Text(stringResource(R.string.flow27_followers_only), color = colors.textPrimary)
            }
            BoardPrimaryButton(
                text = stringResource(R.string.flow27_share),
                onClick = {
                    BoardFlow27Session.caption = caption
                    BoardFlow27Session.publicPost = publicPost
                    Toast.makeText(
                        context,
                        context.getString(R.string.board_demo_share),
                        Toast.LENGTH_SHORT
                    ).show()
                    onShared()
                }
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun Guidelines(onDone: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val rules = listOf(
        R.string.flow27_g1,
        R.string.flow27_g2,
        R.string.flow27_g3,
        R.string.flow27_g4,
        R.string.flow27_g5,
        R.string.flow27_g6,
        R.string.flow27_g7
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow27_guidelines),
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(BoardFlowArt.safetyHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(BoardTheme.radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.flow27_guidelines_title),
                style = BoardType.headline,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            rules.forEachIndexed { i, res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BoardTheme.radii.md))
                        .background(colors.surfaceMuted)
                        .padding(PixiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${i + 1}",
                            style = BoardType.caption,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                    Text(stringResource(res), style = BoardType.body, color = colors.textPrimary)
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.flow27_got_it), onClick = onDone)
            BoardTrustFooter()
        }
    }
}
