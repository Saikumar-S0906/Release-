package com.pixijoy.ai.ui.board.social

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Filter
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardGenerateBlock
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardSecondaryButton
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardTipCallout
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing

/** Guided stop index for Flow 12 tour. */
internal object BoardFlow12Session {
    var stop: Int = 0
}

private data class JourneyStop(
    val id: String,
    @StringRes val title: Int,
    val realDest: String
)

private val JourneyStops = listOf(
    JourneyStop("b12_welcome", R.string.board_f12_stop_welcome, "b1_splash"),
    JourneyStop("b12_signin", R.string.board_f12_stop_signin, "b22_welcome"),
    JourneyStop("b12_home", R.string.board_f12_stop_home, "b2_home"),
    JourneyStop("b12_create", R.string.board_f12_stop_create, "b_create_hub"),
    JourneyStop("b12_edit", R.string.board_f12_stop_edit, "b8_preview"),
    JourneyStop("b12_share", R.string.board_f12_stop_share, "b8_ready"),
    JourneyStop("b12_community", R.string.board_f12_stop_community, "b27_feed"),
    JourneyStop("b12_pro", R.string.board_f12_stop_pro, "b10_discover")
)

/**
 * Flow 12 — End-to-End Journey.
 * Board chrome shows 5 tabs; production keeps Home · Create · Discover · My Stuff
 * (Community → b27_feed, Profile → My Stuff / b9).
 */
@Composable
fun BoardFlow12Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    val pipeline = LocalBoardPipeline.current
    when (screenId) {
        "b12_hub", "b12_intro" -> JourneyMap(
            onStart = { onNavigate("b12_welcome") },
            onOpenStop = { onNavigate(it) },
            onOpenReal = { onNavigate(it) },
            onBack = onBack
        )
        "b12_welcome" -> TourWelcome(
            onContinue = { onNavigate("b12_signin") },
            onOpenReal = { onNavigate("b1_splash") },
            onMap = { onNavigate("b12_intro") }
        )
        "b12_signin" -> TourSignIn(
            onContinue = { onNavigate("b12_home") },
            onOpenReal = { onNavigate("b22_welcome") },
            onBack = { onNavigate("b12_welcome") }
        )
        "b12_home" -> TourHome(
            onTab = tab,
            onContinue = { onNavigate("b12_create") },
            onOpenReal = { onNavigate("b2_home") },
            onBack = { onNavigate("b12_signin") }
        )
        "b12_create" -> TourCreate(
            onContinue = { onNavigate("b12_edit") },
            onOpenReal = pipeline.openMediaInput,
            onBack = { onNavigate("b12_home") }
        )
        "b12_edit" -> TourEdit(
            onContinue = { onNavigate("b12_share") },
            onOpenReal = { onNavigate("b8_preview") },
            onBack = { onNavigate("b12_create") }
        )
        "b12_share" -> TourShare(
            onContinue = { onNavigate("b12_community") },
            onShare = { onNavigate("b8_share") },
            onPublish = { onNavigate("b8_publish") },
            onOpenReal = { onNavigate("b8_ready") },
            onBack = { onNavigate("b12_edit") }
        )
        "b12_community" -> TourCommunity(
            onTab = tab,
            onContinue = { onNavigate("b12_pro") },
            onOpenReal = { onNavigate("b27_feed") },
            onBack = { onNavigate("b12_share") }
        )
        "b12_pro" -> TourPro(
            onFinish = { onNavigate("b12_intro") },
            onUpgrade = { onNavigate("b10_discover") },
            onLater = { onNavigate("b2_home") },
            onBack = { onNavigate("b12_community") }
        )
        else -> JourneyMap(
            onStart = { onNavigate("b12_welcome") },
            onOpenStop = { onNavigate(it) },
            onOpenReal = { onNavigate(it) },
            onBack = onBack
        )
    }
}

@Composable
private fun JourneyMap(
    onStart: () -> Unit,
    onOpenStop: (String) -> Unit,
    onOpenReal: (String) -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val styles = listOf(
        BoardFlowArt.differentHero to R.string.board_f12_style_anime,
        BoardFlowArt.funnyHero to R.string.board_f12_style_3d,
        BoardFlowArt.editorHero to R.string.board_f12_style_sketch,
        BoardFlowArt.beautifulHero to R.string.board_f12_style_watercolor,
        BoardFlowArt.trendPortrait to R.string.board_f12_style_cinema,
        BoardFlowArt.stickerHero to R.string.board_f12_style_stickers,
        BoardFlowArt.collageHero to R.string.board_f12_style_collage,
        BoardFlowArt.creationThumb to R.string.board_f12_style_fantasy
    )
    val benefits = listOf(
        R.string.board_f12_benefit1,
        R.string.board_f12_benefit2,
        R.string.board_f12_benefit3,
        R.string.board_f12_benefit4,
        R.string.board_f12_benefit5,
        R.string.board_f12_benefit6,
        R.string.board_f12_benefit7
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW12_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(BoardFlowArt.logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.size(PixiSpacing.sm))
                Column {
                    Text(
                        stringResource(R.string.flow12_title),
                        style = BoardType.title,
                        color = colors.textPrimary
                    )
                    Text(
                        stringResource(R.string.board_f12_tagline),
                        color = colors.textSecondary,
                        style = BoardType.caption
                    )
                }
            }
            Text(
                stringResource(R.string.flow12_intro_body),
                color = colors.textSecondary,
                style = BoardType.body
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_f12_start_tour),
                onClick = onStart
            )
            Text(
                stringResource(R.string.board_f12_journey_title),
                style = BoardType.title,
                color = colors.textPrimary
            )
            JourneyStops.forEachIndexed { i, stop ->
                val thumb = BoardFlowArt.journeyStops.getOrElse(i) { BoardFlowArt.homeHero }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .border(1.dp, colors.border.copy(alpha = 0.35f), RoundedCornerShape(radii.card))
                        .clickable { onOpenStop(stop.id) }
                        .padding(PixiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Box {
                        Image(
                            painter = painterResource(thumb),
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(radii.md)),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(colors.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${i + 1}",
                                color = colors.onPrimary,
                                style = BoardType.caption,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        stringResource(stop.title),
                        modifier = Modifier.weight(1f),
                        style = BoardType.body,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                    TextButton(onClick = { onOpenReal(stop.realDest) }) {
                        Text(
                            stringResource(R.string.board_f12_open_real),
                            color = colors.primary,
                            style = BoardType.caption
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.board_f12_styles_title),
                style = BoardType.title,
                color = colors.textPrimary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                styles.forEach { (res, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(80.dp)
                    ) {
                        Image(
                            painter = painterResource(res),
                            contentDescription = stringResource(label),
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(radii.md)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.board_f12_share_title),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("WA", "IG", "FB", "SC", "X", "TG", "…").forEach { label ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceMuted),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontWeight = FontWeight.Bold, color = colors.primary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text(
                stringResource(R.string.board_f12_community_actions),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    R.drawable.pj_ic_heart to R.string.board_f12_act_like,
                    R.drawable.pj_ic_comment to R.string.board_f12_act_comment,
                    R.drawable.pj_ic_person_add to R.string.board_f12_act_follow,
                    R.drawable.pj_ic_share to R.string.board_f12_act_share
                ).forEach { (icon, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painter = painterResource(icon), null, tint = colors.primary)
                        Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }
            }
            Text(
                stringResource(R.string.board_f12_benefits_title),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            benefits.forEach { res ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(18.dp))
                    Text(stringResource(res), color = colors.textSecondary)
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.cd_back), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun TourChrome(
    step: Int,
    total: Int = 8,
    onBack: (() -> Unit)?,
    onOpenReal: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = BoardTheme.colors
    BoardAdaptiveFrame {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
                Text(
                    stringResource(R.string.board_f12_step_of, step, total),
                    modifier = Modifier.weight(1f),
                    style = BoardType.title,
                    color = colors.textPrimary
                )
                TextButton(onClick = onOpenReal) {
                    Text(
                        stringResource(R.string.board_f12_open_real),
                        color = colors.primary,
                        style = BoardType.caption
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) { content() }
        }
    }
}

@Composable
private fun TourWelcome(onContinue: () -> Unit, onOpenReal: () -> Unit, onMap: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    TourChrome(step = 1, onBack = onMap, onOpenReal = onOpenReal) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Image(painter = painterResource(BoardFlowArt.logo), contentDescription = null, modifier = Modifier.size(64.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                listOf(
                    BoardFlowArt.beautifulHero,
                    BoardFlowArt.funnyHero,
                    BoardFlowArt.stickerHero,
                    BoardFlowArt.creationThumb
                ).forEach { res ->
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
            }
            Text(
                stringResource(R.string.board_f12_welcome_hero),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = colors.textPrimary
            )
            BoardPrimaryButton(text = stringResource(R.string.board_f12_get_started), onClick = onContinue)
            TextButton(onClick = onContinue) {
                Text(stringResource(R.string.board_f12_sign_in_link), color = colors.primary)
            }
        }
    }
}

@Composable
private fun TourSignIn(onContinue: () -> Unit, onOpenReal: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    TourChrome(step = 2, onBack = onBack, onOpenReal = onOpenReal) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.board_f12_welcome_back),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.board_f12_email_phone)) },
                shape = RoundedCornerShape(radii.md)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.flow11_password)) },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(radii.md)
            )
            BoardPrimaryButton(text = stringResource(R.string.board_f12_sign_in), onClick = onContinue)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("G", "A", "f").forEach { label ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceMuted)
                            .clickable(onClick = onContinue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                }
            }
            BoardOutlinedButton(
                text = stringResource(R.string.board_f12_create_account),
                onClick = onContinue
            )
        }
    }
}

@Composable
private fun TourHome(
    onTab: (BoardShellTab) -> Unit,
    onContinue: () -> Unit,
    onOpenReal: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardMainShell(selected = BoardShellTab.Home, onTab = onTab) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PixiSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f12_step_of, 3, 8),
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                TextButton(onClick = onOpenReal) {
                    Text(stringResource(R.string.board_f12_open_real), color = colors.primary)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(PixiSpacing.xl)
                    .testTag(TestTags.FLOW12_HOST),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.pill))
                        .background(colors.surfaceMuted)
                        .padding(PixiSpacing.md)
                ) {
                    Text(stringResource(R.string.board_f12_search_hint), color = colors.textSecondary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                    listOf(
                        R.string.board_f12_cat_art,
                        R.string.board_f12_cat_anime,
                        R.string.board_f12_cat_stickers,
                        R.string.board_f12_cat_collage
                    ).forEach { res ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(radii.pill))
                                .background(colors.primarySoft)
                                .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                        ) {
                            Text(stringResource(res), color = colors.primary, style = MaterialTheme.typography.labelMedium)
                        }
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
                Text(stringResource(R.string.board_f12_promo), fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(stringResource(R.string.board_f12_try_these), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                    BoardFlowArt.homeRecents.take(4).forEach { res ->
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
                }
                BoardPrimaryButton(text = stringResource(R.string.board_f12_continue), onClick = onContinue)
            }
        }
    }
}

@Composable
private fun TourCreate(onContinue: () -> Unit, onOpenReal: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var style by remember { mutableIntStateOf(1) }
    val styles = listOf(
        R.string.board_f12_style_original,
        R.string.board_f12_style_anime,
        R.string.board_f12_style_3d,
        R.string.board_f12_style_cartoon
    )
    TourChrome(step = 4, onBack = onBack, onOpenReal = onOpenReal) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.board_f12_create_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            Image(
                painter = painterResource(BoardFlowArt.creationThumb),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                styles.forEachIndexed { i, res ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (i == style) colors.primary else colors.surfaceMuted)
                            .clickable { style = i }
                            .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(res),
                            color = if (i == style) colors.onPrimary else colors.textSecondary
                        )
                    }
                }
            }
            BoardGenerateBlock(
                credits = 1,
                onGenerate = onContinue,
                generateLabel = stringResource(R.string.board_f12_generate)
            )
        }
    }
}

@Composable
private fun TourEdit(onContinue: () -> Unit, onOpenReal: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var tool by remember { mutableIntStateOf(1) }
    var filter by remember { mutableIntStateOf(2) }
    val tools = listOf(
        R.drawable.pj_ic_crop to R.string.board_f8_tool_crop,
        R.drawable.pj_ic_filter to R.string.board_f8_tool_filter,
        R.drawable.pj_ic_text to R.string.board_f8_tool_text,
        R.drawable.pj_ic_sticker to R.string.board_f8_tool_sticker,
        R.drawable.pj_ic_tune to R.string.board_f8_tool_adjust
    )
    val filters = listOf(
        R.string.board_f15_filter_none,
        R.string.board_f15_filter_vivid,
        R.string.board_f15_filter_warm,
        R.string.board_f15_filter_bw,
        R.string.board_f15_filter_dream
    )
    TourChrome(step = 5, onBack = onBack, onOpenReal = onOpenReal) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PixiSpacing.xl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.board_f12_edit_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onContinue) {
                    Text(stringResource(R.string.board_f12_save), color = colors.primary)
                }
            }
            Image(
                painter = painterResource(BoardFlowArt.baAfter),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = PixiSpacing.xl)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixiSpacing.md),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tools.forEachIndexed { i, (icon, label) ->
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
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = PixiSpacing.xl),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                filters.forEachIndexed { i, name ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (i == filter) colors.primarySoft else colors.surfaceMuted)
                            .border(
                                width = if (i == filter) 1.dp else 0.dp,
                                color = colors.primary,
                                shape = RoundedCornerShape(radii.pill)
                            )
                            .clickable { filter = i }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(name),
                            color = colors.textPrimary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardPrimaryButton(
                text = stringResource(R.string.board_f12_apply_continue),
                onClick = onContinue,
                modifier = Modifier.padding(PixiSpacing.xl)
            )
        }
    }
}

@Composable
private fun TourShare(
    onContinue: () -> Unit,
    onShare: () -> Unit,
    onPublish: () -> Unit,
    onOpenReal: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    TourChrome(step = 6, onBack = onBack, onOpenReal = onOpenReal) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(56.dp))
            Text(
                stringResource(R.string.board_f12_ready_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Image(
                painter = painterResource(BoardFlowArt.creationThumb),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            BoardTipCallout(
                title = stringResource(R.string.board_soft_preview_title),
                body = stringResource(R.string.board_f12_share_tip)
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_f12_continue_tour),
                onClick = onContinue
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_f12_share_social),
                onClick = onShare
            )
            BoardOutlinedButton(
                text = stringResource(R.string.flow8_publish),
                onClick = onPublish
            )
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.result_create_another), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun TourCommunity(
    onTab: (BoardShellTab) -> Unit,
    onContinue: () -> Unit,
    onOpenReal: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var feedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        R.string.board_f12_feed_trending,
        R.string.board_f12_feed_following,
        R.string.board_f12_feed_foryou
    )
    val posts = listOf(
        Triple("Priya_art", BoardFlowArt.beautifulHero, R.string.board_f12_post1),
        Triple("Arjun_photo", BoardFlowArt.funnyHero, R.string.board_f12_post2)
    )
    // Community maps to Discover in production IA for this tour chrome.
    BoardMainShell(selected = BoardShellTab.Discover, onTab = onTab) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PixiSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f12_step_of, 7, 8),
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                TextButton(onClick = onOpenReal) {
                    Text(stringResource(R.string.board_f12_open_real), color = colors.primary)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PixiSpacing.xl),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                tabs.forEachIndexed { i, res ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (i == feedTab) colors.primary else colors.surfaceMuted)
                            .clickable { feedTab = i }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(res),
                            color = if (i == feedTab) colors.onPrimary else colors.textSecondary
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(PixiSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                posts.forEach { (author, res, caption) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(radii.card))
                            .background(colors.surfaceMuted)
                            .padding(PixiSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        Text(author, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Image(
                            painter = painterResource(res),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(radii.md)),
                            contentScale = ContentScale.Crop
                        )
                        Text(stringResource(caption), color = colors.textSecondary)
                        Text("1.2K · 86 · 42", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                    }
                }
                BoardPrimaryButton(text = stringResource(R.string.board_f12_continue), onClick = onContinue)
            }
        }
    }
}

@Composable
private fun TourPro(
    onFinish: () -> Unit,
    onUpgrade: () -> Unit,
    onLater: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var yearly by remember { mutableStateOf(true) }
    TourChrome(step = 8, onBack = onBack, onOpenReal = onUpgrade) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Icon(painter = painterResource(R.drawable.pj_ic_premium), null, tint = colors.accentYellow, modifier = Modifier.size(64.dp))
            Text(
                stringResource(R.string.flow10_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            listOf(
                R.string.board_f10_feat_styles,
                R.string.board_f10_feat_hd,
                R.string.board_f10_feat_no_ads,
                R.string.board_f10_feat_video
            ).forEach { res ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_check), null, tint = colors.success)
                    Text(stringResource(res), color = colors.textPrimary)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                PlanChip(
                    title = stringResource(R.string.board_f10_monthly),
                    price = stringResource(R.string.board_f10_pro_monthly_price),
                    selected = !yearly,
                    modifier = Modifier.weight(1f)
                ) { yearly = false }
                PlanChip(
                    title = stringResource(R.string.board_f10_yearly),
                    price = stringResource(R.string.board_f12_yearly_price),
                    selected = yearly,
                    modifier = Modifier.weight(1f)
                ) { yearly = true }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f10_upgrade_cta), onClick = onUpgrade)
            BoardSecondaryButton(text = stringResource(R.string.board_f12_maybe_later), onClick = onLater)
            TextButton(onClick = onFinish) {
                Text(stringResource(R.string.board_f12_back_map), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun PlanChip(
    title: String,
    price: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(radii.card))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.accentPurple else colors.surfaceMuted,
                shape = RoundedCornerShape(radii.card)
            )
            .background(if (selected) colors.accentPurple.copy(alpha = 0.08f) else colors.surface)
            .clickable(onClick = onClick)
            .padding(PixiSpacing.md)
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        Text(price, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
    }
}
