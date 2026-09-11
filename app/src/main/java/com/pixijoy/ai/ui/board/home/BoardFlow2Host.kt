package com.pixijoy.ai.ui.board.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Tune

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardEditorDocument
import com.pixijoy.ai.ui.board.BoardMediaSession
import com.pixijoy.ai.ui.board.BoardMemory
import com.pixijoy.ai.ui.board.BoardRecentCreation
import com.pixijoy.ai.ui.board.BoardSafeResultImage
import com.pixijoy.ai.ui.board.BoardSessionPhoto
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.rememberBoardMemoryRevision
import com.pixijoy.ai.ui.board.rememberBoardPhotoGate
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardAiSparkleIcon
import com.pixijoy.ai.ui.board.components.BoardBodyText
import com.pixijoy.ai.ui.board.components.BoardCaptionText
import com.pixijoy.ai.ui.board.components.BoardCdn
import com.pixijoy.ai.ui.board.components.BoardEmptyState
import com.pixijoy.ai.ui.board.components.BoardFadeIn
import com.pixijoy.ai.ui.board.components.BoardFavoriteToggle
import com.pixijoy.ai.ui.board.components.BoardStepReveal
import com.pixijoy.ai.ui.board.components.BoardTertiaryButton
import com.pixijoy.ai.ui.board.components.BoardTipCallout
import com.pixijoy.ai.ui.board.components.BoardFilterPill
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardHeroImage
import com.pixijoy.ai.ui.board.components.BoardIntentTile
import com.pixijoy.ai.ui.board.components.BoardLazyScrollBar
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardRemoteImage
import com.pixijoy.ai.ui.board.components.BoardScrollCue
import com.pixijoy.ai.ui.board.components.BoardSecondaryButton
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardTabViewport
import com.pixijoy.ai.ui.board.components.BoardTertiaryButton
import com.pixijoy.ai.ui.board.components.BoardTitleText
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.components.rememberBoardTwoPane
import com.pixijoy.ai.ui.board.create.BoardFlow5Session
import com.pixijoy.ai.ui.board.create.BoardFlowDifferentSession
import com.pixijoy.ai.ui.board.media.BoardFlow13Session
import com.pixijoy.ai.ui.board.social.BoardFlow27Session
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.launch

/**
 * Flow 2 — Home & Discover (board-faithful).
 * Source: flows/02_Home_and_Discover.png
 */
@Composable
fun BoardFlow2Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tabHandler = com.pixijoy.ai.ui.board.BoardProductionTabs.tabHandler(onNavigate)
    val pipeline = LocalBoardPipeline.current
    val denied = { onNavigate("b3_camera_denied") }
    val useTemplate = rememberBoardPhotoGate(onCameraDenied = denied) {
        onNavigate("b2_template_create")
    }
    val useStyle = rememberBoardPhotoGate(onCameraDenied = denied) {
        onNavigate("b_diff_options")
    }
    val useSticker = rememberBoardPhotoGate(onCameraDenied = denied) {
        onNavigate("b13_cutout")
    }
    val useBeautiful = rememberBoardPhotoGate(onCameraDenied = denied) {
        onNavigate("b5_choose")
    }
    val useMove = rememberBoardPhotoGate(onCameraDenied = denied) {
        onNavigate("b14_input")
    }
    fun openExploreCard(card: ExploreCard, returnId: String) {
        BoardExploreSession.select(card, returnId)
        when (card.action) {
            ExploreAction.Template -> onNavigate("b2_template")
            ExploreAction.Style -> {
                BoardFlowDifferentSession.styleIndex = card.styleIndex
                useStyle()
            }
            ExploreAction.Sticker -> {
                BoardFlow13Session.styleIndex = card.styleIndex
                useSticker()
            }
            ExploreAction.Beautiful -> {
                BoardFlow5Session.style = beautifulStyleFor(card.titleRes)
                useBeautiful()
            }
            ExploreAction.Move -> useMove()
        }
    }
    when (screenId) {
        "b2_home" -> BoardHomeDefault(
            onSeeAllRecents = { onNavigate(com.pixijoy.ai.ui.board.BoardProductionTabs.MY_STUFF) },
            onOpenCreation = { onNavigate(it) },
            onDiscover = { onNavigate("b2_discover") },
            onTell = pipeline.openTell,
            onBeautiful = pipeline.openBeautiful,
            onFunny = pipeline.openFunny,
            onDifferent = pipeline.openDifferent,
            onMove = pipeline.openMove,
            onPhotoPicked = { onNavigate("b3_preview") },
            onCameraDenied = { onNavigate("b3_camera_denied") },
            onNotif = { onNavigate("b25_center") },
            onTab = tabHandler
        )
        "b2_home_scroll" -> BoardHomeScrolled(
            onDiscover = { onNavigate("b2_discover") },
            onSearch = { onNavigate("b2_search") },
            onTrending = { onNavigate("b2_trending") },
            onCommunity = { onNavigate("b27_feed") },
            onCartoon = pipeline.openDifferent,
            onSticker = pipeline.openStickers,
            onMove = pipeline.openMove,
            onBg = { onNavigate("b_c04") },
            onTab = tabHandler
        )
        "b2_discover" -> BoardDiscoverMain(
            onSearch = { onNavigate("b2_search") },
            onFeatured = {
                BoardExploreSession.selectFeatured()
                openExploreCard(BoardExploreSession.featuredCard(), "b2_discover")
            },
            onCategory = { pack ->
                BoardExploreSession.pack = pack
                onNavigate("b2_discover_cat")
            },
            onCard = { openExploreCard(it, "b2_discover") },
            onTab = tabHandler
        )
        "b2_discover_cat" -> BoardDiscoverCategory(
            onBack = { onNavigate("b2_discover") },
            onCard = { openExploreCard(it, "b2_discover_cat") },
            onTab = tabHandler
        )
        "b2_template" -> BoardTemplateDetail(
            onBack = { onNavigate(BoardExploreSession.returnId) },
            onUse = { useTemplate() },
            onShare = pipeline.openShare,
            onSimilar = { card -> BoardExploreSession.select(card, BoardExploreSession.returnId) },
            onSeeAll = { onNavigate("b2_discover_cat") },
            onTab = tabHandler
        )
        "b2_template_create" -> BoardTemplateCreate(
            onBack = { onNavigate("b2_template") },
            onNext = { onNavigate("b2_template_result") },
            onChangePhoto = { onNavigate("b3_chooser") },
            onTab = tabHandler
        )
        "b2_template_result" -> BoardTemplateResult(
            onBack = { onNavigate("b2_template_create") },
            onClose = onBack,
            onSave = { onNavigate("b8_ready") },
            onShare = pipeline.openShare,
            onRemix = { onNavigate("b_c14") },
            onTab = tabHandler
        )
        "b2_search" -> BoardSearchModule(
            onBack = { onNavigate("b2_discover") },
            onCard = { openExploreCard(it, "b2_search") },
            onTab = tabHandler
        )
        "b2_trending" -> {
            val cards = exploreTrendingCards()
            BoardImageModule(
                title = stringResource(R.string.board_f2_trending),
                images = cards.map { it.art },
                labels = cards.map { stringResource(it.titleRes) },
                onItem = { i -> openExploreCard(cards[i], "b2_trending") },
                onBack = onBack,
                onTab = tabHandler,
                selected = BoardShellTab.Discover
            )
        }
        "b2_collections" -> {
            val cards = exploreCollectionCards()
            BoardImageModule(
                title = stringResource(R.string.board_f2_collections),
                images = cards.map { it.art },
                labels = cards.map { stringResource(it.titleRes) },
                onItem = { i -> openExploreCard(cards[i], "b2_collections") },
                onBack = onBack,
                onTab = tabHandler,
                selected = BoardShellTab.Discover
            )
        }
        "b2_seasons" -> {
            val cards = exploreCards(ExplorePack.Festive)
            BoardImageModule(
                title = stringResource(R.string.board_f2_seasons),
                images = cards.map { it.art },
                labels = cards.map { stringResource(it.titleRes) },
                onItem = { i -> openExploreCard(cards[i], "b2_seasons") },
                onBack = onBack,
                onTab = tabHandler,
                selected = BoardShellTab.Discover
            )
        }
        "b2_creators" -> BoardImageModule(
            title = stringResource(R.string.board_f2_creators),
            images = BoardFlowArt.creatorFaces,
            labels = listOf(
                stringResource(R.string.board_demo_maya),
                stringResource(R.string.board_demo_alex),
                stringResource(R.string.board_demo_priya),
                stringResource(R.string.board_demo_jordan),
                stringResource(R.string.board_demo_sam),
                stringResource(R.string.board_demo_leo)
            ),
            onItem = { i ->
                BoardFlow27Session.selectedCreator = i
                onNavigate("b27_creator")
            },
            onBack = onBack,
            onTab = tabHandler,
            selected = BoardShellTab.Discover,
            circular = true
        )
        "b2_favorites" -> {
            val context = LocalContext.current
            val favRev = BoardExploreFavorites.revision
            LaunchedEffect(Unit) { BoardExploreFavorites.hydrate(context) }
            val cards = remember(favRev) { BoardExploreFavorites.favoriteCards() }
            if (cards.isEmpty()) {
                BoardMainShell(selected = BoardShellTab.Discover, onTab = tabHandler) {
                    Column(Modifier.fillMaxSize().padding(PixiSpacing.xl)) {
                        BoardEmptyState(
                            title = stringResource(R.string.projects_empty_title),
                            body = stringResource(R.string.board_search_empty_body),
                            ctaLabel = stringResource(R.string.flow2_chip_templates),
                            onCta = { onNavigate("b2_discover") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                BoardImageModule(
                    title = stringResource(R.string.board_f2_favorites),
                    images = cards.map { it.art },
                    labels = cards.map { stringResource(it.titleRes) },
                    onItem = { i -> openExploreCard(cards[i], "b2_favorites") },
                    onBack = onBack,
                    onTab = tabHandler,
                    selected = BoardShellTab.Discover
                )
            }
        }
        "b2_recent" -> BoardImageModule(
            title = stringResource(R.string.board_f2_recent),
            images = BoardFlowArt.homeRecents + BoardFlowArt.memeTemplates.take(2),
            labels = (BoardFlowArt.homeRecents + BoardFlowArt.memeTemplates.take(2)).indices.map {
                stringResource(R.string.board_f2_item_recent_n, it + 1)
            },
            onItem = { _ -> onNavigate("b18_browse") },
            onBack = onBack,
            onTab = tabHandler,
            selected = BoardShellTab.Home
        )
        else -> BoardHomeDefault(
            onSeeAllRecents = { onNavigate(com.pixijoy.ai.ui.board.BoardProductionTabs.MY_STUFF) },
            onOpenCreation = { onNavigate(it) },
            onDiscover = { onNavigate("b2_discover") },
            onTell = pipeline.openTell,
            onBeautiful = pipeline.openBeautiful,
            onFunny = pipeline.openFunny,
            onDifferent = pipeline.openDifferent,
            onMove = pipeline.openMove,
            onPhotoPicked = { onNavigate("b3_preview") },
            onCameraDenied = { onNavigate("b3_camera_denied") },
            onNotif = { onNavigate("b25_center") },
            onTab = tabHandler
        )
    }
}

@Composable
private fun BoardHomeDefault(
    onSeeAllRecents: () -> Unit,
    onOpenCreation: (String) -> Unit,
    onDiscover: () -> Unit,
    onTell: () -> Unit,
    onBeautiful: () -> Unit,
    onFunny: () -> Unit,
    onDifferent: () -> Unit,
    onMove: () -> Unit,
    onPhotoPicked: () -> Unit,
    onCameraDenied: () -> Unit,
    onNotif: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    rememberBoardMemoryRevision()
    val recents = BoardMemory.lastCreations()
    val coachHidden = BoardHomeCoach.dismissed
    LaunchedEffect(Unit) { BoardHomeCoach.hydrate(context) }
    BoardMainShell(selected = BoardShellTab.Home, onTab = onTab) {
        BoardAdaptiveFrame {
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TestTags.FLOW2_HOME),
                    contentPadding = PaddingValues(bottom = PixiSpacing.xxl)
                ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PixiSpacing.xl),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        Image(
                            painter = painterResource(BoardFlowArt.logo),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier.size(36.dp)
                        )
                        Column {
                            Text(
                                stringResource(R.string.board_f2_greeting),
                                style = BoardType.headline,
                                color = colors.textPrimary
                            )
                            BoardBodyText(text = stringResource(R.string.board_f2_greeting_sub))
                        }
                    }
                    IconButton(onClick = onNotif) {
                        Icon(painter = painterResource(R.drawable.pj_ic_notifications),
                            contentDescription = stringResource(R.string.flow25_title),
                            tint = colors.textPrimary
                        )
                    }
                }
            }
            item {
                BoardStepReveal(visible = !coachHidden) {
                    Column(
                        modifier = Modifier.padding(horizontal = PixiSpacing.xl),
                        verticalArrangement = Arrangement.spacedBy(PixiSpacing.xs)
                    ) {
                        BoardTipCallout(
                            title = stringResource(R.string.home_coach_title),
                            body = stringResource(R.string.home_coach_body)
                        )
                        BoardTertiaryButton(
                            text = stringResource(R.string.home_coach_got_it),
                            onClick = { scope.launch { BoardHomeCoach.dismiss(context) } }
                        )
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PixiSpacing.xl)
                        .clip(RoundedCornerShape(radii.card))
                        .testTag(TestTags.FLOW2_HERO)
                        .clickable(onClick = onDiscover)
                ) {
                    BoardHeroImage(
                        resId = BoardFlowArt.homeHero,
                        aspectRatio = BoardTabViewport.HeroAspect
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                                )
                            )
                    )
                    BoardTitleText(
                        text = stringResource(R.string.board_f2_hero),
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(PixiSpacing.xl)
                    )
                }
            }
            item {
                com.pixijoy.ai.ui.board.BoardCapturePhotoRow(
                    onPicked = { uri ->
                        BoardMediaSession.setPhotoUri(uri)
                        onPhotoPicked()
                    },
                    onCameraDenied = onCameraDenied,
                    galleryTestTag = TestTags.FLOW2_ADD_PHOTO,
                    selfieTestTag = TestTags.FLOW2_TAKE_SELFIE,
                    cameraTestTag = TestTags.FLOW2_TAKE_PHOTO,
                    modifier = Modifier.padding(horizontal = PixiSpacing.xl)
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PixiSpacing.xl, vertical = PixiSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        HomeIntentQuad(
                            onBeautiful = onBeautiful,
                            onFunny = onFunny,
                            onDifferent = onDifferent,
                            onMove = onMove
                        )
                        Text(
                            text = stringResource(R.string.board_scroll_for_more),
                            style = BoardType.caption,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            androidx.compose.ui.graphics.Color.Transparent,
                                            colors.background.copy(alpha = 0.92f)
                                        )
                                    )
                                )
                                .padding(top = PixiSpacing.md, bottom = PixiSpacing.xs)
                                .testTag("home_scroll_cue")
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(radii.md))
                            .background(colors.surfaceMuted)
                            .testTag(TestTags.FLOW2_TELL)
                            .clickable(onClick = onTell)
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.pj_ic_sparkles),
                            contentDescription = stringResource(R.string.home_talk_to_pixijoy),
                            tint = colors.primary
                        )
                        Text(
                            stringResource(R.string.board_f2_tell),
                            style = BoardType.body,
                            color = colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            item {
                SectionHeader(
                    title = stringResource(R.string.board_f2_recents),
                    action = stringResource(R.string.board_f2_see_all),
                    onAction = onSeeAllRecents
                )
                if (recents.isEmpty()) {
                    BoardEmptyState(
                        title = stringResource(R.string.board_empty_title),
                        body = stringResource(R.string.home_recent_empty),
                        ctaLabel = stringResource(R.string.board_empty_cta),
                        onCta = { onTab(BoardShellTab.Create) },
                        heroRes = null
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = PixiSpacing.xl)
                            .testTag(TestTags.HOME_LAST_CREATIONS),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        recents.forEachIndexed { index, item ->
                            HomeCreationThumb(
                                item = item,
                                index = index,
                                onClick = {
                                    runCatching {
                                        BoardMediaSession.setPhotoUri(android.net.Uri.parse(item.uri))
                                    }
                                    onOpenCreation(item.openScreenId)
                                }
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(PixiSpacing.md))
                BoardTrustFooter(modifier = Modifier.padding(PixiSpacing.md))
            }
                }
                Text(
                    text = stringResource(R.string.board_scroll_for_more),
                    style = BoardType.caption,
                    color = colors.textSecondary,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = PixiSpacing.sm)
                        .testTag("home_scroll_cue")
                )
                BoardLazyScrollBar(
                    listState = listState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp, top = 48.dp, bottom = 48.dp)
                )
            }
        }
    }
}

@Composable
private fun BoardHomeScrolled(
    onDiscover: () -> Unit,
    onSearch: () -> Unit,
    onTrending: () -> Unit,
    onCommunity: () -> Unit,
    onCartoon: () -> Unit,
    onSticker: () -> Unit,
    onMove: () -> Unit,
    onBg: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val ideas = listOf(
        R.string.board_f2_idea_cartoon to onCartoon,
        R.string.board_f2_idea_sticker to onSticker,
        R.string.board_f2_idea_video to onMove,
        R.string.board_f2_idea_bg to onBg
    )
    val ideaColors = listOf(colors.accentPink, colors.accentYellow, colors.accentPurple, colors.primary)
    BoardMainShell(selected = BoardShellTab.Home, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.lg)
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSearch),
                placeholder = {
                    Text(
                        stringResource(R.string.board_f2_search_templates),
                        style = BoardType.body,
                        color = colors.textSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_search),
                        contentDescription = null
                    )
                },
                shape = RoundedCornerShape(radii.md)
            )
            SectionHeader(
                title = stringResource(R.string.board_f2_trending),
                action = stringResource(R.string.board_f2_see_all),
                onAction = onTrending,
                padded = false
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                val labels = listOf(
                    R.string.board_f2_item_ai_portraits,
                    R.string.board_f2_item_funny_memes,
                    R.string.board_f2_item_travel_vibes,
                    R.string.board_f2_item_festive,
                    R.string.board_f2_item_pets
                )
                BoardFlowArt.trendingCards.forEachIndexed { i, res ->
                    Column(
                        modifier = Modifier
                            .width(140.dp)
                            .clickable(onClick = onTrending)
                    ) {
                        Image(
                            painter = painterResource(res),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(radii.card)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(PixiSpacing.xs))
                        Text(
                            stringResource(labels.getOrElse(i) { R.string.board_f2_trending }),
                            fontWeight = FontWeight.SemiBold,
                            style = BoardType.caption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            BoardTitleText(text = stringResource(R.string.board_f2_ideas))
            ideas.forEachIndexed { i, (res, onClick) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(colors.surfaceMuted)
                        .clickable(onClick = onClick)
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ideaColors[i % ideaColors.size].copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        BoardAiSparkleIcon(
                            modifier = Modifier.size(18.dp),
                            tint = ideaColors[i % ideaColors.size]
                        )
                    }
                    Spacer(modifier = Modifier.width(PixiSpacing.md))
                    Text(
                        stringResource(res),
                        color = colors.textPrimary,
                        style = BoardType.body,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_forward),
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            SectionHeader(
                title = stringResource(R.string.board_f2_community),
                action = stringResource(R.string.board_f2_see_all),
                onAction = onCommunity,
                padded = false
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                BoardFlowArt.feedThumbs.take(4).forEach { res ->
                    Image(
                        painter = painterResource(res),
                        contentDescription = null,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(radii.md))
                            .clickable(onClick = onCommunity),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            BoardOutlinedButton(
                text = stringResource(R.string.nav_explore),
                onClick = onDiscover
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun BoardDiscoverMain(
    onSearch: () -> Unit,
    onFeatured: () -> Unit,
    onCategory: (ExplorePack) -> Unit,
    onCard: (ExploreCard) -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scroll = rememberScrollState()
    val detailScroll = rememberScrollState()
    var chip by remember { mutableIntStateOf(BoardExploreSession.chip) }
    var showAllCats by remember { mutableStateOf(false) }
    val chipPack = packForChip(chip)
    val twoPane = rememberBoardTwoPane()
    val shortViewport = LocalConfiguration.current.screenHeightDp < 520
    val catAspect = if (shortViewport) 1.45f else 1.1f
    val previewCats = BoardFlowArt.discoverCategories.take(6)
    val header: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoardTitleText(text = stringResource(R.string.nav_explore))
            IconButton(onClick = onSearch) {
                Icon(
                    painter = painterResource(R.drawable.pj_ic_search),
                    contentDescription = stringResource(R.string.board_f2_search_anything),
                    tint = colors.textPrimary
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(radii.md))
                .background(colors.surfaceMuted)
                .clickable(onClick = onSearch)
                .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            Icon(
                painter = painterResource(R.drawable.pj_ic_search),
                contentDescription = stringResource(R.string.board_f2_search_anything),
                tint = colors.textSecondary
            )
            Text(
                stringResource(R.string.board_f2_search_anything),
                style = BoardType.body,
                color = colors.textSecondary
            )
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            listOf(
                R.string.flow2_chip_all,
                R.string.flow2_chip_templates,
                R.string.flow2_chip_styles,
                R.string.flow2_chip_stickers
            ).forEachIndexed { i, res ->
                BoardFilterPill(
                    text = stringResource(res),
                    selected = chip == i,
                    onClick = {
                        chip = i
                        showAllCats = false
                        BoardExploreSession.chip = i
                        packForChip(i)?.let { BoardExploreSession.pack = it }
                    }
                )
            }
        }
    }
    val categoryBlock: @Composable () -> Unit = {
        if (showAllCats) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showAllCats = false }) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_back),
                        contentDescription = stringResource(R.string.cd_back),
                        tint = colors.textPrimary
                    )
                }
                BoardTitleText(
                    text = stringResource(R.string.board_f2_categories),
                    modifier = Modifier.weight(1f)
                )
            }
            DiscoverCategoryGrid(
                items = BoardFlowArt.discoverCategories,
                aspect = catAspect,
                onCategory = onCategory
            )
        } else {
            BoardTitleText(text = stringResource(R.string.board_f2_categories))
            DiscoverCategoryGrid(
                items = previewCats,
                aspect = catAspect,
                onCategory = onCategory
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_f2_see_all_categories),
                onClick = { showAllCats = true }
            )
        }
    }
    val catalogOrFeatured: @Composable () -> Unit = {
        BoardFadeIn(key = chip) {
            Column(verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)) {
            if (chipPack != null) {
                BoardTitleText(text = stringResource(exploreTitle(chipPack)))
                BoardBodyText(
                    text = stringResource(exploreSubtitle(chipPack)),
                    color = colors.textSecondary
                )
                ExploreCardGrid(
                    cards = exploreCards(chipPack),
                    onCard = onCard,
                    aspect = catAspect
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .clickable(onClick = onFeatured)
                ) {
                    BoardRemoteImage(
                        cdnKey = BoardCdn.DISCOVER_FEATURED,
                        fallbackRes = BoardFlowArt.discoverFeatured,
                        aspectRatio = if (shortViewport) 2.2f else BoardTabViewport.HeroAspect
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(PixiSpacing.lg)
                    ) {
                        BoardCaptionText(
                            text = stringResource(R.string.board_f2_featured),
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        BoardTitleText(
                            text = stringResource(R.string.board_f2_featured_title),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(PixiSpacing.sm))
                        BoardPrimaryButton(
                            text = stringResource(R.string.board_f2_try_now),
                            onClick = onFeatured
                        )
                    }
                }
            }
            }
        }
    }
    BoardMainShell(selected = BoardShellTab.Discover, onTab = onTab) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (twoPane) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(PixiSpacing.xl),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.lg)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxSize()
                            .verticalScroll(scroll),
                        verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                    ) {
                        header()
                        categoryBlock()
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxSize()
                            .verticalScroll(detailScroll),
                        verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                    ) {
                        catalogOrFeatured()
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(PixiSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    header()
                    if (showAllCats) {
                        categoryBlock()
                    } else {
                        catalogOrFeatured()
                        if (chipPack == null) categoryBlock()
                    }
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
private fun BoardDiscoverCategory(
    onBack: () -> Unit,
    onCard: (ExploreCard) -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val pack = BoardExploreSession.pack
    BoardMainShell(selected = BoardShellTab.Discover, onTab = onTab) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_back),
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    BoardTitleText(text = stringResource(exploreTitle(pack)))
                    BoardBodyText(
                        text = stringResource(exploreSubtitle(pack)),
                        color = colors.textSecondary
                    )
                }
            }
            BoardHeroImage(
                resId = exploreHero(pack),
                aspectRatio = 16f / 9f,
                modifier = Modifier.padding(horizontal = PixiSpacing.xl)
            )
            var sortTab by remember { mutableIntStateOf(0) }
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(PixiSpacing.xl),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                listOf(
                    R.string.board_f2_tab_popular,
                    R.string.board_f2_tab_trending,
                    R.string.board_f2_tab_latest
                ).forEachIndexed { i, res ->
                    BoardFilterPill(
                        text = stringResource(res),
                        selected = i == sortTab,
                        onClick = { sortTab = i }
                    )
                }
            }
            val sorted = remember(sortTab, pack) {
                exploreSorted(pack, exploreSortFromTab(sortTab))
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = PixiSpacing.xl),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                contentPadding = PaddingValues(bottom = PixiSpacing.xl)
            ) {
                itemsIndexed(sorted) { _, card ->
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.card))
                            .background(colors.surfaceMuted)
                            .clickable { onCard(card) }
                            .padding(PixiSpacing.sm)
                    ) {
                        Image(
                            painter = painterResource(card.art),
                            contentDescription = stringResource(card.titleRes),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(radii.md)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(PixiSpacing.xs))
                        Text(
                            stringResource(card.titleRes),
                            fontWeight = FontWeight.SemiBold,
                            style = BoardType.caption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            stringResource(exploreCta(card.action)),
                            color = colors.primary,
                            style = BoardType.caption,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardTemplateDetail(
    onBack: () -> Unit,
    onUse: () -> Unit,
    onShare: () -> Unit,
    onSimilar: (ExploreCard) -> Unit,
    onSeeAll: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var art by remember { mutableIntStateOf(BoardExploreSession.selectedArt) }
    var titleRes by remember { mutableIntStateOf(BoardExploreSession.selectedTitleRes) }
    val pack = BoardExploreSession.pack
    val similar = exploreCards(pack).filter { it.art != art }.ifEmpty { exploreCards(ExplorePack.Templates) }
    BoardMainShell(selected = BoardShellTab.Discover, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_back),
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
                Row {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    val favRev = BoardExploreFavorites.revision
                    LaunchedEffect(Unit) { BoardExploreFavorites.hydrate(context) }
                    val favored = remember(favRev, art) { BoardExploreFavorites.isFavorite(art) }
                    BoardFavoriteToggle(
                        favored = favored,
                        onToggle = { scope.launch { BoardExploreFavorites.toggle(context, art) } },
                        contentDescription = stringResource(R.string.flow9_favorites)
                    )
                    IconButton(onClick = onShare) {
                        Icon(
                            painter = painterResource(R.drawable.pj_ic_share),
                            contentDescription = stringResource(R.string.board_f2_share),
                            tint = colors.primary
                        )
                    }
                }
            }
            BoardHeroImage(resId = art, aspectRatio = 1f)
            BoardTitleText(text = stringResource(titleRes))
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                exploreTags(pack).forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.surfaceMuted)
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.xs)
                    ) {
                        BoardCaptionText(text = tag, color = colors.textSecondary)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.pj_ic_heart),
                    contentDescription = null,
                    tint = colors.accentYellow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                BoardBodyText(text = stringResource(R.string.board_f2_template_stats))
            }
            SectionHeader(
                title = stringResource(R.string.board_f2_similar),
                action = stringResource(R.string.board_f2_see_all),
                onAction = onSeeAll,
                padded = false
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                similar.take(4).forEach { card ->
                    Image(
                        painter = painterResource(card.art),
                        contentDescription = stringResource(card.titleRes),
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(radii.md))
                            .clickable {
                                onSimilar(card)
                                art = card.art
                                titleRes = card.titleRes
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f2_use_this), onClick = onUse)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun BoardTemplateCreate(
    onBack: () -> Unit,
    onNext: () -> Unit,
    onChangePhoto: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var tool by remember { mutableIntStateOf(0) }
    var caption by remember { mutableStateOf(BoardEditorDocument.textOverlay) }
    var selectedArt by remember { mutableIntStateOf(BoardExploreSession.selectedArt) }
    var brightness by remember { mutableFloatStateOf(BoardEditorDocument.brightness) }
    var contrast by remember { mutableFloatStateOf(BoardEditorDocument.contrast) }
    var saturation by remember { mutableFloatStateOf(BoardEditorDocument.saturation) }
    val templates = remember { exploreCards(ExplorePack.Templates) }
    val styleLooks = remember { exploreCards(ExplorePack.Styles) }
    val tools = listOf(
        R.string.board_f2_tool_text,
        R.string.board_f2_tool_style,
        R.string.board_f2_tool_sticker,
        R.string.board_f2_tool_filters,
        R.string.board_f2_tool_adjust
    )
    fun applyArt(art: Int, titleRes: Int) {
        selectedArt = art
        BoardExploreSession.selectedArt = art
        BoardExploreSession.selectedTitleRes = titleRes
    }
    fun goNext() {
        if (caption.isNotBlank()) BoardEditorDocument.textOverlay = caption
        if (BoardMediaSession.hasPhoto()) {
            BoardEditorDocument.ensureLoaded(context, fallbackRes = selectedArt)
        } else {
            BoardEditorDocument.loadFromDrawable(context, selectedArt)
        }
        onNext()
    }
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_back),
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
                BoardTitleText(text = stringResource(R.string.board_f2_customize_title))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                BoardSessionPhoto(
                    fallbackRes = selectedArt,
                    modifier = Modifier.fillMaxSize()
                )
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
            BoardOutlinedButton(
                text = stringResource(R.string.board_f2_change_photo),
                onClick = onChangePhoto
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tools.forEachIndexed { i, res ->
                    val on = tool == i
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.md))
                            .background(if (on) colors.primary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { tool = i }
                            .padding(PixiSpacing.sm)
                    ) {
                        if (res == R.string.board_f2_tool_adjust) {
                            Icon(painter = painterResource(R.drawable.pj_ic_tune),
                                contentDescription = stringResource(res),
                                tint = if (on) colors.primary else colors.textSecondary
                            )
                        } else {
                            BoardAiSparkleIcon(
                                tint = if (on) colors.primary else colors.textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            stringResource(res),
                            style = BoardType.caption,
                            color = if (on) colors.primary else colors.textSecondary
                        )
                    }
                }
            }
            when (tool) {
                0 -> OutlinedTextField(
                    value = caption,
                    onValueChange = {
                        caption = it
                        BoardEditorDocument.textOverlay = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.board_f2_tool_text)) },
                    singleLine = true,
                    shape = RoundedCornerShape(radii.md)
                )
                2 -> Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    listOf("✨", "❤️", "😂", "🎉", "🔥").forEach { emoji ->
                        BoardOutlinedButton(
                            text = emoji,
                            onClick = {
                                caption = (caption + " " + emoji).trim()
                                BoardEditorDocument.textOverlay = caption
                            }
                        )
                    }
                }
                1 -> Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    styleLooks.forEach { card ->
                        val on = BoardExploreSession.selectedStyleIndex == card.styleIndex
                        Image(
                            painter = painterResource(card.art),
                            contentDescription = stringResource(card.titleRes),
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(radii.md))
                                .then(
                                    if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md))
                                    else Modifier
                                )
                                .clickable {
                                    BoardExploreSession.selectedStyleIndex = card.styleIndex
                                    BoardEditorDocument.filterIndex = when (card.styleIndex) {
                                        9 -> 2
                                        4 -> 3
                                        0, 5, 8 -> 1
                                        else -> 0
                                    }
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                3 -> Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    listOf(
                        R.string.board_f15_filter_none to 0,
                        R.string.board_f15_filter_vivid to 1,
                        R.string.board_f15_filter_warm to 2,
                        R.string.board_f15_filter_bw to 3
                    ).forEach { (label, idx) ->
                        BoardOutlinedButton(
                            text = stringResource(label),
                            onClick = { BoardEditorDocument.filterIndex = idx }
                        )
                    }
                }
                4 -> Column(verticalArrangement = Arrangement.spacedBy(PixiSpacing.xs)) {
                    Text(stringResource(R.string.board_f15_brightness), style = BoardType.caption, color = colors.textSecondary)
                    Slider(
                        value = brightness,
                        onValueChange = {
                            brightness = it
                            BoardEditorDocument.brightness = it
                        },
                        valueRange = 0f..20f
                    )
                    Text(stringResource(R.string.board_f15_contrast), style = BoardType.caption, color = colors.textSecondary)
                    Slider(
                        value = contrast,
                        onValueChange = {
                            contrast = it
                            BoardEditorDocument.contrast = it
                        },
                        valueRange = 0f..20f
                    )
                    Text(stringResource(R.string.board_f15_saturation), style = BoardType.caption, color = colors.textSecondary)
                    Slider(
                        value = saturation,
                        onValueChange = {
                            saturation = it
                            BoardEditorDocument.saturation = it
                        },
                        valueRange = 0f..20f
                    )
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                templates.forEach { card ->
                    Image(
                        painter = painterResource(card.art),
                        contentDescription = stringResource(card.titleRes),
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(radii.md))
                            .then(
                                if (card.art == selectedArt) {
                                    Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md))
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { applyArt(card.art, card.titleRes) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f2_continue),
                onClick = { goNext() }
            )
        }
    }
}

@Composable
private fun BoardTemplateResult(
    onBack: () -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onRemix: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_back),
                        contentDescription = null
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_close),
                        contentDescription = null
                    )
                }
            }
            Icon(
                painter = painterResource(R.drawable.pj_ic_check),
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier.size(40.dp)
            )
            BoardTitleText(
                text = stringResource(R.string.board_f2_ready),
                color = colors.success
            )
            BoardSafeResultImage(
                bitmap = BoardEditorDocument.workingBitmap,
                fallbackRes = BoardExploreSession.selectedArt,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(BoardTheme.radii.card))
            )
            BoardPrimaryButton(text = stringResource(R.string.board_f2_save), onClick = onSave)
            BoardOutlinedButton(text = stringResource(R.string.board_f2_share), onClick = onShare)
            BoardSecondaryButton(text = stringResource(R.string.board_f2_remix), onClick = onRemix)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("WA", "IG", "SC", "···").forEach { label ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceMuted)
                            .clickable(onClick = onShare),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, style = BoardType.caption, fontWeight = FontWeight.Bold)
                    }
                }
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun BoardSearchModule(
    onBack: () -> Unit,
    onCard: (ExploreCard) -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val haystack = remember { exploreSearchCards() }
    val matches = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) {
            haystack.take(8)
        } else {
            haystack.filter { card ->
                context.getString(card.titleRes).contains(q, ignoreCase = true)
            }
        }
    }
    val recents = listOf("meme", "anime", "travel", "sticker", "portrait")
    BoardMainShell(selected = BoardShellTab.Discover, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_back),
                        contentDescription = null
                    )
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.FLOW2_SEARCH),
                    placeholder = {
                        Text(stringResource(R.string.board_f2_search_templates))
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.pj_ic_search),
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(radii.md)
                )
            }
            if (query.isBlank()) {
                BoardTitleText(text = stringResource(R.string.board_f2_recent_searches))
                recents.forEach { q ->
                    Text(
                        q,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { query = q }
                            .padding(vertical = PixiSpacing.sm),
                        color = colors.textSecondary,
                        style = BoardType.secondary
                    )
                }
            }
            if (matches.isEmpty()) {
                BoardEmptyState(
                    title = stringResource(R.string.board_f2_search_empty),
                    body = stringResource(R.string.board_f2_search_templates),
                    modifier = Modifier.fillMaxWidth(),
                    heroRes = null
                )
            } else {
                BoardTitleText(
                    text = if (query.isBlank()) {
                        stringResource(R.string.board_f2_tab_popular)
                    } else {
                        stringResource(R.string.board_f2_search_anything)
                    }
                )
                ExploreCardGrid(cards = matches, onCard = onCard)
            }
        }
    }
}

@Composable
private fun BoardImageModule(
    title: String,
    images: List<Int>,
    labels: List<String>,
    onItem: (Int) -> Unit,
    onBack: () -> Unit,
    onTab: (BoardShellTab) -> Unit,
    selected: BoardShellTab,
    circular: Boolean = false
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    androidx.activity.compose.BackHandler(onBack = onBack)
    BoardMainShell(selected = selected, onTab = onTab) {
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
                    text = title,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_close),
                        contentDescription = stringResource(R.string.close),
                        tint = colors.textPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                items(images.size) { i ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(images[i]),
                            contentDescription = labels.getOrNull(i),
                            modifier = Modifier
                                .size(96.dp)
                                .clip(if (circular) CircleShape else RoundedCornerShape(radii.md))
                                .clickable { onItem(i) },
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(PixiSpacing.xs))
                        Text(
                            labels.getOrElse(i) { "" },
                            style = BoardType.caption,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverCategoryGrid(
    items: List<Pair<Int, Int>>,
    aspect: Float,
    onCategory: (ExplorePack) -> Unit
) {
    val radii = BoardTheme.radii
    Column(verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                row.forEach { (titleRes, imageRes) ->
                    val pack = packForCategory(titleRes)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCategory(pack) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(imageRes),
                            contentDescription = stringResource(titleRes),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(aspect)
                                .clip(RoundedCornerShape(radii.md)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(PixiSpacing.xs))
                        Text(
                            stringResource(titleRes),
                            style = BoardType.caption,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                repeat(2 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ExploreCardGrid(
    cards: List<ExploreCard>,
    onCard: (ExploreCard) -> Unit,
    aspect: Float = 1f
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Column(verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
        cards.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                row.forEach { card ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(radii.card))
                            .background(colors.surfaceMuted)
                            .clickable { onCard(card) }
                            .padding(PixiSpacing.sm)
                    ) {
                        Image(
                            painter = painterResource(card.art),
                            contentDescription = stringResource(card.titleRes),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(aspect)
                                .clip(RoundedCornerShape(radii.md)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(PixiSpacing.xs))
                        Text(
                            stringResource(card.titleRes),
                            fontWeight = FontWeight.SemiBold,
                            style = BoardType.caption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            stringResource(exploreCta(card.action)),
                            color = colors.primary,
                            style = BoardType.caption,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun beautifulStyleFor(titleRes: Int): BoardFlow5Session.Style = when (titleRes) {
    R.string.flow5_vivid -> BoardFlow5Session.Style.Vivid
    R.string.flow5_aesthetic, R.string.board_f2_cat_aesthetic -> BoardFlow5Session.Style.Aesthetic
    R.string.flow5_professional -> BoardFlow5Session.Style.Professional
    R.string.flow5_natural, R.string.flow5_auto -> BoardFlow5Session.Style.Auto
    R.string.board_diff_style_vintage, R.string.flow5_creative -> BoardFlow5Session.Style.Creative
    else -> BoardFlow5Session.Style.Portrait
}

@Composable
private fun HomeIntentQuad(
    onBeautiful: () -> Unit,
    onFunny: () -> Unit,
    onDifferent: () -> Unit,
    onMove: () -> Unit
) {
    val colors = BoardTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            BoardIntentTile(
                title = stringResource(R.string.make_it_beautiful),
                subtitle = stringResource(R.string.home_intent_beautiful_desc),
                accent = colors.accentPink,
                imageRes = BoardFlowArt.homeBeautifulTile,
                onClick = onBeautiful,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.HOME_INTENT_BEAUTIFUL)
            )
            BoardIntentTile(
                title = stringResource(R.string.make_it_funny),
                subtitle = stringResource(R.string.home_intent_funny_desc),
                accent = colors.accentYellow,
                imageRes = BoardFlowArt.homeFunnyTile,
                onClick = onFunny,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.HOME_INTENT_FUNNY)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            BoardIntentTile(
                title = stringResource(R.string.make_it_different),
                subtitle = stringResource(R.string.home_intent_different_desc),
                accent = colors.primary,
                imageRes = BoardFlowArt.homeDifferentTile,
                onClick = onDifferent,
                modifier = Modifier.weight(1f)
            )
            BoardIntentTile(
                title = stringResource(R.string.make_it_move),
                subtitle = stringResource(R.string.home_intent_move_desc),
                accent = colors.accentPurple,
                imageRes = BoardFlowArt.homeMoveTile,
                onClick = onMove,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.HOME_INTENT_MOVE)
            )
        }
    }
}

@Composable
private fun HomeCreationThumb(
    item: BoardRecentCreation,
    index: Int,
    onClick: () -> Unit
) {
    val radii = BoardTheme.radii
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context).data(item.uri).build(),
        contentDescription = stringResource(R.string.board_f2_item_recent_n, index + 1),
        modifier = Modifier
            .size(104.dp)
            .clip(RoundedCornerShape(radii.md))
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop,
        placeholder = painterResource(item.fallbackRes),
        error = painterResource(item.fallbackRes)
    )
}

@Composable
private fun SectionHeader(
    title: String,
    action: String,
    onAction: () -> Unit,
    padded: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (padded) Modifier.padding(horizontal = PixiSpacing.xl, vertical = PixiSpacing.lg) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = BoardType.title, modifier = Modifier.weight(1f))
        BoardTertiaryButton(text = action, onClick = onAction)
    }
}
