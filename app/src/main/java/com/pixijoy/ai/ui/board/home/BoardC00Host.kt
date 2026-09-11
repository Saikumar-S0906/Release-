package com.pixijoy.ai.ui.board.home

import androidx.annotation.DrawableRes
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.pixijoy.ai.ui.board.BoardAuthReturn
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.components.BoardBodyText
import com.pixijoy.ai.ui.board.components.BoardCaptionText
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardIntentTile
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardTitleText
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.delay

/** C00 session — soft first-run prefs (local demo only). */
internal object BoardC00Session {
    val tastes = mutableSetOf<Int>()
    var usedSample: Boolean = true
    var guestMode: Boolean = false
}

/**
 * C00 — Empty Home & First Creation.
 * Soft: sample photo path; no real gallery / network generation.
 */
@Composable
fun BoardC00Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val pipeline = LocalBoardPipeline.current
    val tabHandler: (BoardShellTab) -> Unit = { tab ->
        when (tab) {
            BoardShellTab.Home -> onNavigate("b_c00_empty")
            BoardShellTab.Discover -> onNavigate("b2_discover")
            BoardShellTab.Create -> onNavigate(com.pixijoy.ai.ui.board.BoardProductionTabs.CREATE)
            BoardShellTab.Community -> onNavigate("b27_feed")
            BoardShellTab.MyStuff -> onNavigate("b18_browse")
        }
    }
    when (screenId) {
        "b_c00_hub", "b_c00_empty" -> C00EmptyHome(
            onOpenStudio = { onNavigate(com.pixijoy.ai.ui.board.BoardProductionTabs.CREATE) },
            onTaste = { onNavigate("b_c00_taste") },
            onBeautiful = pipeline.openBeautiful,
            onFunny = pipeline.openFunny,
            onDifferent = pipeline.openDifferent,
            onMove = pipeline.openMove,
            onTell = pipeline.openTell,
            onPhotoPicked = { onNavigate("b3_preview") },
            onCameraDenied = { onNavigate("b_c00_perm_denied") },
            onNotif = { onNavigate("b25_center") },
            onTab = tabHandler
        )
        "b_c00_taste" -> C00Taste(
            onContinue = { onNavigate("b_c00_discover") },
            onSkip = { onNavigate("b_c00_skip_taste") },
            onTab = tabHandler
        )
        "b_c00_skip_taste" -> C00AltCard(
            iconRes = R.drawable.pj_ic_heart,
            title = stringResource(R.string.board_c00_skip_title),
            body = stringResource(R.string.board_c00_skip_body),
            primary = stringResource(R.string.board_c00_continue),
            onPrimary = { onNavigate("b_c00_discover") },
            secondary = stringResource(R.string.board_c00_back_taste),
            onSecondary = { onNavigate("b_c00_taste") },
            onTab = tabHandler
        )
        "b_c00_discover" -> C00Discover(
            onOpenPack = { pack ->
                BoardExploreSession.pack = pack
                onNavigate("b2_discover_cat")
            },
            onBack = { onNavigate("b_c00_empty") },
            onTab = tabHandler
        )
        "b_c00_funny" -> C00FunnyCategory(
            onUse = { onNavigate("b_c00_generating") },
            onBack = { onNavigate("b_c00_discover") },
            onTab = tabHandler
        )
        "b_c00_generating" -> C00Generating(
            onReady = { onNavigate("b_c00_first") },
            onTab = tabHandler
        )
        "b_c00_first" -> C00FirstCreation(
            onSave = { onNavigate("b_c00_guest_gate") },
            onChangePhoto = { onNavigate("b_c00_sample") },
            onClose = { onNavigate("b_c00_empty") },
            onTab = tabHandler
        )
        "b_c00_guest_gate" -> C00GuestGate(
            onSignUp = {
                BoardAuthReturn.remember("b_c00_first")
                onNavigate("b22_welcome")
            },
            onGuest = {
                BoardC00Session.guestMode = true
                onNavigate("b_c00_guest")
            },
            onDismiss = { onNavigate("b_c00_first") },
            onTab = tabHandler
        )
        "b_c00_sample" -> C00SamplePhoto(
            onUseSample = {
                BoardC00Session.usedSample = true
                onNavigate("b_c00_generating")
            },
            onMyPhoto = { onNavigate("b3_gallery") },
            onBack = { onNavigate("b_c00_empty") },
            onTab = tabHandler
        )
        "b_c00_perm_denied" -> C00AltCard(
            iconRes = R.drawable.pj_ic_gallery,
            title = stringResource(R.string.board_c00_perm_title),
            body = stringResource(R.string.board_c00_perm_body),
            primary = stringResource(R.string.board_perm_allow),
            onPrimary = { onNavigate("b3_gallery") },
            secondary = stringResource(R.string.board_c00_try_sample),
            onSecondary = {
                BoardC00Session.usedSample = true
                onNavigate("b_c00_sample")
            },
            onTab = tabHandler
        )
        "b_c00_guest" -> C00AltCard(
            icon = R.drawable.pj_ic_wave,
            title = stringResource(R.string.board_c00_guest_title),
            body = stringResource(R.string.board_c00_guest_body),
            primary = stringResource(R.string.board_continue_guest),
            onPrimary = { onNavigate("b_c00_empty") },
            secondary = stringResource(R.string.board_signup_title),
            onSecondary = {
                BoardAuthReturn.remember("b_c00_empty")
                onNavigate("b22_welcome")
            },
            onTab = tabHandler
        )
        else -> C00EmptyHome(
            onOpenStudio = { onNavigate(com.pixijoy.ai.ui.board.BoardProductionTabs.CREATE) },
            onTaste = { onNavigate("b_c00_taste") },
            onBeautiful = pipeline.openBeautiful,
            onFunny = pipeline.openFunny,
            onDifferent = pipeline.openDifferent,
            onMove = pipeline.openMove,
            onTell = pipeline.openTell,
            onPhotoPicked = { onNavigate("b3_preview") },
            onCameraDenied = { onNavigate("b_c00_perm_denied") },
            onNotif = { onNavigate("b25_center") },
            onTab = tabHandler
        )
    }
}

@Composable
private fun C00EmptyHome(
    onOpenStudio: () -> Unit,
    onTaste: () -> Unit,
    onBeautiful: () -> Unit,
    onFunny: () -> Unit,
    onDifferent: () -> Unit,
    onMove: () -> Unit,
    onTell: () -> Unit,
    onPhotoPicked: () -> Unit,
    onCameraDenied: () -> Unit,
    onNotif: () -> Unit,
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
                .testTag(TestTags.FLOW_C00_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    BoardTitleText(text = stringResource(R.string.board_c00_greeting))
                    BoardBodyText(text = stringResource(R.string.board_c00_greeting_sub))
                }
                IconButton(onClick = onNotif) {
                    Icon(painter = painterResource(R.drawable.pj_ic_notifications),
                        contentDescription = stringResource(R.string.flow25_title)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .testTag(TestTags.C00_OPEN_STUDIO)
                    .clickable(onClick = onOpenStudio)
            ) {
                Image(
                    painter = painterResource(R.drawable.board_c00_hero),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.7f),
                    contentScale = ContentScale.Crop
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
                        .padding(PixiSpacing.lg)
                )
            }
            com.pixijoy.ai.ui.board.BoardCapturePhotoRow(
                onPicked = { uri ->
                    com.pixijoy.ai.ui.board.BoardMediaSession.setPhotoUri(uri)
                    onPhotoPicked()
                },
                onCameraDenied = onCameraDenied,
                galleryTestTag = TestTags.FLOW2_ADD_PHOTO
            )
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
                    modifier = Modifier.weight(1f)
                )
                BoardIntentTile(
                    title = stringResource(R.string.make_it_funny),
                    subtitle = stringResource(R.string.home_intent_funny_desc),
                    accent = colors.accentYellow,
                    imageRes = BoardFlowArt.homeFunnyTile,
                    onClick = onFunny,
                    modifier = Modifier.weight(1f)
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
                    modifier = Modifier.weight(1f)
                )
            }
            BoardOutlinedButton(
                text = stringResource(R.string.board_f2_tell),
                onClick = onTell
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_c00_set_taste),
                onClick = onTaste
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C00Taste(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember {
        mutableStateOf(BoardC00Session.tastes.toSet())
    }
    val options = listOf(
        R.string.board_c00_taste_pets to Color(0xFFFFE4EC),
        R.string.board_c00_taste_people to Color(0xFFE3F2FD),
        R.string.board_c00_taste_travel to Color(0xFFE8F5E9),
        R.string.board_c00_taste_festivals to Color(0xFFE8E0FF)
    )
    val tasteInk = Color(0xFF1A1528)
    BoardMainShell(selected = BoardShellTab.Home, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C00_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.board_c00_taste_title),
                style = BoardType.title,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(stringResource(R.string.board_c00_taste_body), color = colors.textSecondary)
            options.forEachIndexed { index, (res, bg) ->
                val on = selected.contains(index)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(bg)
                        .then(
                            if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md))
                            else Modifier
                        )
                        .clickable {
                            selected = if (on) selected - index else selected + index
                            BoardC00Session.tastes.clear()
                            BoardC00Session.tastes.addAll(selected)
                        }
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(res),
                        fontWeight = FontWeight.SemiBold,
                        color = tasteInk,
                        modifier = Modifier.weight(1f)
                    )
                    if (on) {
                        Icon(
                            painter = painterResource(R.drawable.pj_ic_check),
                            contentDescription = null,
                            tint = colors.primary
                        )
                    }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_c00_continue),
                onClick = onContinue
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_c00_skip),
                onClick = onSkip
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C00Discover(
    onOpenPack: (ExplorePack) -> Unit,
    onBack: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val tiles = listOf(
        Triple(BoardFlowArt.funnyHero, R.string.board_f2_item_funny_memes, ExplorePack.Memes),
        Triple(BoardFlowArt.differentHero, R.string.flow2_chip_styles, ExplorePack.Styles),
        Triple(BoardFlowArt.stickerHero, R.string.flow2_chip_stickers, ExplorePack.Stickers),
        Triple(BoardFlowArt.trendPortrait, R.string.board_f2_item_ai_portraits, ExplorePack.Portraits),
        Triple(R.drawable.board_c07_festival, R.string.board_f2_item_festive, ExplorePack.Festive),
        Triple(BoardFlowArt.collageHero, R.string.board_f2_cat_aesthetic, ExplorePack.Aesthetic)
    )
    BoardMainShell(selected = BoardShellTab.Discover, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C00_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_back),
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
                Text(
                    stringResource(R.string.board_c00_discover_title),
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            tiles.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEach { (art, title, pack) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.85f)
                                .clip(RoundedCornerShape(radii.md))
                                .clickable { onOpenPack(pack) }
                        ) {
                            Image(
                                painter = painterResource(art),
                                contentDescription = stringResource(title),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                stringResource(title),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(PixiSpacing.sm)
                                    .clip(RoundedCornerShape(radii.pill))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = PixiSpacing.sm, vertical = 4.dp),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style = BoardType.caption
                            )
                        }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C00FunnyCategory(
    onUse: () -> Unit,
    onBack: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var filter by remember { mutableIntStateOf(0) }
    val filters = listOf(
        R.string.board_c00_filter_all,
        R.string.board_c00_filter_templates,
        R.string.board_c00_filter_styles,
        R.string.board_c00_filter_stickers
    )
    BoardMainShell(selected = BoardShellTab.Discover, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C00_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_back),
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
                Text(
                    stringResource(R.string.home_intent_funny),
                    style = BoardType.title,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.pill))
                    .background(colors.surfaceMuted)
                    .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.pj_ic_search),
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    stringResource(R.string.board_c00_search_hint),
                    modifier = Modifier.padding(start = PixiSpacing.sm),
                    color = colors.textSecondary
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
                            .clickable { filter = i }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(res),
                            color = if (on) colors.onPrimary else colors.textPrimary,
                            style = BoardType.caption,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .clickable(onClick = onUse)
            ) {
                Image(
                    painter = painterResource(R.drawable.board_c00_funny_dog),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(0.5f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(PixiSpacing.lg)
                ) {
                    Text(
                        stringResource(R.string.board_f2_template_title),
                        style = BoardType.title,
                        color = Color.White
                    )
                    BoardCaptionText(
                        text = stringResource(R.string.board_f2_template_stats),
                        color = Color.White.copy(0.9f)
                    )
                }
            }
            Text(
                stringResource(R.string.board_c00_popular),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                BoardFlowArt.homeRecents.forEach { art ->
                    Image(
                        painter = painterResource(art),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onUse),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f2_use_this),
                onClick = onUse
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C00Generating(onReady: () -> Unit, onTab: (BoardShellTab) -> Unit) {
    val colors = BoardTheme.colors
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1600)
        ready = true
    }
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C00_HOST),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.board_c00_funny_dog),
                contentDescription = null,
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(BoardTheme.radii.card)),
                contentScale = ContentScale.Crop,
                alpha = 0.35f
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            if (!ready) {
                CircularProgressIndicator(color = colors.primary)
                Spacer(modifier = Modifier.height(PixiSpacing.md))
            }
            Text(
                if (ready) stringResource(R.string.board_c00_ready)
                else stringResource(R.string.board_c00_generating),
                style = BoardType.title,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            if (ready) {
                BoardPrimaryButton(
                    text = stringResource(R.string.board_c00_use_this_photo),
                    onClick = onReady
                )
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C00FirstCreation(
    onSave: () -> Unit,
    onChangePhoto: () -> Unit,
    onClose: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C00_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_back),
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_close),
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            }
            Text(
                stringResource(R.string.board_c00_first_title),
                fontWeight = FontWeight.Bold,
                style = BoardType.title,
                color = colors.textPrimary
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
            ) {
                Image(
                    painter = painterResource(R.drawable.board_c00_funny_dog),
                    contentDescription = stringResource(R.string.board_c00_first_preview),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(PixiSpacing.md)
                        .clip(RoundedCornerShape(radii.pill))
                        .background(Color.Black.copy(0.55f))
                        .clickable(onClick = onChangePhoto)
                        .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.sm)
                ) {
                    Text(
                        stringResource(R.string.board_f2_tap_photo),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                stringResource(R.string.board_c00_recent_samples),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                listOf(
                    R.drawable.board_c00_funny_dog,
                    BoardFlowArt.homeHero,
                    BoardFlowArt.beautifulHero,
                    BoardFlowArt.stickerHero
                ).forEach { art ->
                    Image(
                        painter = painterResource(art),
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(radii.sm))
                            .clickable(onClick = onChangePhoto),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f2_save),
                onClick = onSave
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C00GuestGate(
    onSignUp: () -> Unit,
    onGuest: () -> Unit,
    onDismiss: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardMainShell(selected = BoardShellTab.Home, onTab = onTab) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TestTags.FLOW_C00_HOST)
        ) {
            Image(
                painter = painterResource(R.drawable.board_c00_hero),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.35f
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.45f))
                    .clickable(onClick = onDismiss)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(PixiSpacing.xl)
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.surface)
                    .padding(PixiSpacing.xl)
                    .clickable(enabled = false) {},
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                Text(
                    stringResource(R.string.board_c00_save_account_title),
                    style = BoardType.title,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = colors.textPrimary
                )
                Text(
                    stringResource(R.string.board_c00_save_account_body),
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    style = BoardType.body
                )
                BoardPrimaryButton(
                    text = stringResource(R.string.board_signup_title),
                    onClick = onSignUp
                )
                BoardOutlinedButton(
                    text = stringResource(R.string.board_continue_guest),
                    onClick = onGuest
                )
                BoardCaptionText(
                    text = stringResource(R.string.board_f2_tell),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun C00SamplePhoto(
    onUseSample: () -> Unit,
    onMyPhoto: () -> Unit,
    onBack: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C00_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_back),
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
                Text(
                    stringResource(R.string.board_c00_sample_title),
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(R.drawable.board_c00_funny_dog),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(stringResource(R.string.board_c00_sample_body), color = colors.textSecondary)
            BoardPrimaryButton(
                text = stringResource(R.string.board_c00_use_sample),
                onClick = onUseSample
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_c00_use_my_photo),
                onClick = onMyPhoto
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C00AltCard(
    title: String,
    body: String,
    primary: String,
    onPrimary: () -> Unit,
    secondary: String,
    onSecondary: () -> Unit,
    onTab: (BoardShellTab) -> Unit,
    @DrawableRes icon: Int? = null,
    @DrawableRes iconRes: Int? = null
) {
    val colors = BoardTheme.colors
    BoardMainShell(selected = BoardShellTab.Home, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C00_HOST),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    iconRes != null -> Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    icon != null -> Icon(painter = painterResource(icon),
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            BoardTitleText(
                text = title,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardBodyText(
                text = body,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            BoardPrimaryButton(text = primary, onClick = onPrimary)
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardOutlinedButton(text = secondary, onClick = onSecondary)
            BoardTrustFooter()
        }
    }
}
