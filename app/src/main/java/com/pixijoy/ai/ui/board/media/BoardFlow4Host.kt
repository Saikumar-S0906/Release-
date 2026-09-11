package com.pixijoy.ai.ui.board.media

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.pixijoy.ai.ui.board.BoardRouteMap
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardEdgeKind
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardGateScreen
import com.pixijoy.ai.ui.board.components.BoardHeroImage
import com.pixijoy.ai.ui.board.components.BoardIntentTile
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardSecondaryButton
import com.pixijoy.ai.ui.board.components.BoardSoftEdgeGate
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.delay

/** Session choice for Flow 4 → rematch / media destinations. */
internal object BoardFlow4Session {
    enum class Path {
        Beautiful, Funny, Stickers, Move, Different, Tell;

        fun destination(): String = when (this) {
            Beautiful -> BoardRouteMap.createIntent("beautiful")
            Funny -> BoardRouteMap.createIntent("funny")
            Different -> BoardRouteMap.createIntent("different")
            Move -> BoardRouteMap.createIntent("move")
            Tell -> BoardRouteMap.createIntent("tell")
            Stickers -> BoardRouteMap.media("stickers")
        }

        @StringRes
        fun titleRes(): Int = when (this) {
            Beautiful -> R.string.make_it_beautiful
            Funny -> R.string.make_it_funny
            Stickers -> R.string.board_f4_create_stickers
            Move -> R.string.make_it_move
            Different -> R.string.make_it_different
            Tell -> R.string.board_f2_tell
        }

        @StringRes
        fun descRes(): Int = when (this) {
            Beautiful -> R.string.board_f4_path_beautiful_desc
            Funny -> R.string.board_f4_path_funny_desc
            Stickers -> R.string.board_f4_path_stickers_desc
            Move -> R.string.board_f4_path_move_desc
            Different -> R.string.board_f4_path_different_desc
            Tell -> R.string.board_f4_path_tell_desc
        }

        @DrawableRes
        fun imageRes(): Int = when (this) {
            Beautiful -> BoardFlowArt.beautifulHero
            Funny -> BoardFlowArt.funnyHero
            Stickers -> BoardFlowArt.stickerHero
            Move -> BoardFlowArt.motionHero
            Different -> BoardFlowArt.differentHero
            Tell -> BoardFlowArt.tellHero
        }

        fun previewImages(): List<Int> = when (this) {
            Beautiful -> BoardFlowArt.rematchBeautiful
            Funny -> BoardFlowArt.rematchFunny
            Different -> BoardFlowArt.rematchDifferent
            Tell -> BoardFlowArt.rematchTell
            Stickers -> listOf(
                BoardFlowArt.stickerHero,
                BoardFlowArt.baAfter,
                BoardFlowArt.profileAvatar,
                BoardFlowArt.funnyHero
            )
            Move -> listOf(
                BoardFlowArt.motionHero,
                BoardFlowArt.posterMove,
                BoardFlowArt.baAfter,
                BoardFlowArt.cameraViewfinder
            )
        }
    }

    var path: Path = Path.Beautiful
}

private fun isDeviceOnline(context: Context): Boolean {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return true
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/** Flow 4 — AI Understanding & suggestions. Source: flows/04_AI_Understanding.png */
@Composable
fun BoardFlow4Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    when (screenId) {
        "b4_selected" -> Selected(
            onContinue = { onNavigate("b4_analyzing") },
            onChange = { onNavigate("b3_chooser") }
        )
        "b4_analyzing" -> Analyzing(
            onDone = { onNavigate("b4_understanding") },
            onOffline = { onNavigate("b4_offline") }
        )
        "b4_result", "b4_understanding" -> UnderstandingResult(
            onLooksGood = { onNavigate("b4_suggestions") }
        )
        "b4_suggestions" -> Suggestions(
            onPick = { path ->
                BoardFlow4Session.path = path
                onNavigate("b4_preview")
            },
            onList = { onNavigate("b4_choose_path") },
            onEdge = { onNavigate(it) }
        )
        "b4_choose_path" -> ChoosePath(
            onPick = { path ->
                BoardFlow4Session.path = path
                onNavigate("b4_preview")
            }
        )
        "b4_preview" -> QuickPreview(
            onTry = { onNavigate("b4_start") },
            onBack = { onNavigate("b4_suggestions") }
        )
        "b4_start" -> StartCreating(
            onStart = { onNavigate(BoardFlow4Session.path.destination()) },
            onBack = { onNavigate("b4_preview") }
        )
        "b4_no_subject" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_no_subject_title),
            body = stringResource(R.string.board_f4_no_subject_body),
            primary = stringResource(R.string.board_f3_try_another),
            onPrimary = { onNavigate("b3_chooser") },
            secondary = stringResource(R.string.flow4_continue_anyway),
            onSecondary = { onNavigate("b4_suggestions") },
            tipTitle = stringResource(R.string.board_soft_preview_title),
            tipBody = stringResource(R.string.board_soft_preview_tip),
            heroRes = BoardFlowArt.baBefore
        )
        "b4_multi_subject" -> MultiSubject(
            onContinue = { onNavigate("b4_understanding") },
            onBack = { onNavigate("b4_analyzing") }
        )
        "b4_low_quality" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_low_q_title),
            body = stringResource(R.string.board_f4_low_q_body),
            primary = stringResource(R.string.board_f3_capture),
            onPrimary = { onNavigate("b3_camera") },
            secondary = stringResource(R.string.flow4_continue_anyway),
            onSecondary = { onNavigate("b4_suggestions") },
            tipTitle = stringResource(R.string.board_soft_preview_title),
            tipBody = stringResource(R.string.board_soft_preview_tip),
            heroRes = BoardFlowArt.baBefore
        )
        "b4_no_face" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_no_face_title),
            body = stringResource(R.string.board_f4_no_face_body),
            primary = stringResource(R.string.board_f4_continue),
            onPrimary = { onNavigate("b4_suggestions") },
            secondary = stringResource(R.string.board_f3_try_another),
            onSecondary = { onNavigate("b3_chooser") },
            tipTitle = stringResource(R.string.board_soft_preview_title),
            tipBody = stringResource(R.string.board_soft_preview_tip),
            heroRes = BoardFlowArt.profileAvatar
        )
        "b4_unsupported" -> BoardGateScreen(
            title = stringResource(R.string.flow3_unsupported_title),
            body = stringResource(R.string.flow3_unsupported_body),
            primary = stringResource(R.string.flow3_choose_another),
            onPrimary = { onNavigate("b3_chooser") },
            tipTitle = stringResource(R.string.board_soft_preview_title),
            tipBody = stringResource(R.string.board_soft_preview_tip),
            heroRes = BoardFlowArt.baBefore
        )
        "b4_offline" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Offline,
            onPrimary = { onNavigate("b4_analyzing") },
            onSecondary = { onNavigate("b15_open") },
            secondary = stringResource(R.string.flow4_basic_editor)
        )
        "b4_safety" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Safety,
            onPrimary = { onNavigate("b3_chooser") },
            onSecondary = { onNavigate("b20_input") },
            secondary = stringResource(R.string.board_f4_learn_more)
        )
        else -> Selected(
            onContinue = { onNavigate("b4_analyzing") },
            onChange = { onNavigate("b3_chooser") }
        )
    }
}

@Composable
private fun Selected(onContinue: () -> Unit, onChange: () -> Unit) {
    val colors = BoardTheme.colors
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.board_f4_selected_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Text(stringResource(R.string.board_f4_ready_magic), color = colors.textSecondary)
            BoardHeroImage(resId = BoardFlowArt.baAfter, aspectRatio = 3f / 4f)
            Spacer(modifier = Modifier.weight(1f, fill = false))
            BoardPrimaryButton(
                text = stringResource(R.string.board_f4_continue),
                onClick = onContinue
            )
            BoardSecondaryButton(
                text = stringResource(R.string.board_f4_change_photo),
                onClick = onChange
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun Analyzing(onDone: () -> Unit, onOffline: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val steps = listOf(
        R.string.board_f4_detect_subjects,
        R.string.board_f4_detect_scene,
        R.string.board_f4_detect_objects,
        R.string.board_f4_detect_ideas,
        R.string.board_f4_detect_prep
    )
    var doneCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        if (!isDeviceOnline(context)) {
            onOffline()
            return@LaunchedEffect
        }
        steps.indices.forEach { i ->
            delay(650)
            doneCount = i + 1
        }
        delay(450)
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
            BoardHeroImage(resId = BoardFlowArt.aiAnalyzing, aspectRatio = 16f / 9f)
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(colors.primary.copy(alpha = 0.9f), colors.accentPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_sparkles),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Text(
                stringResource(R.string.board_f4_analyzing_title),
                fontWeight = FontWeight.Bold,
                style = BoardType.title,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                steps.forEachIndexed { i, res ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        when {
                            i < doneCount -> Icon(painter = painterResource(R.drawable.pj_ic_check_circle),
                                null,
                                tint = colors.success,
                                modifier = Modifier.size(20.dp)
                            )
                            i == doneCount -> CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = colors.primary
                            )
                            else -> Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            )
                        }
                        Text(
                            stringResource(res),
                            color = if (i <= doneCount) Color.White else Color.White.copy(alpha = 0.45f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            LinearProgressIndicator(
                progress = { doneCount / steps.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(BoardTheme.radii.pill)),
                color = colors.primary,
                trackColor = Color.White.copy(alpha = 0.15f)
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            Text(
                stringResource(R.string.board_f4_analyzing_hint),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun UnderstandingResult(onLooksGood: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val tags = listOf(
        R.string.board_f4_tag_person to Alignment.TopStart,
        R.string.board_f4_tag_dog to Alignment.CenterEnd,
        R.string.board_f4_tag_outdoor to Alignment.BottomStart
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW4_RESULT),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                Image(
                    painter = painterResource(BoardFlowArt.baAfter),
                    contentDescription = stringResource(R.string.board_f4_tags_preview),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                tags.forEach { (res, align) ->
                    Box(
                        modifier = Modifier
                            .align(align)
                            .padding(PixiSpacing.md)
                            .clip(RoundedCornerShape(radii.pill))
                            .background(colors.primary)
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.xs)
                    ) {
                        Text(
                            stringResource(res),
                            color = colors.onPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.board_f4_found_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            listOf(
                R.string.board_f4_found_person,
                R.string.board_f4_found_dog,
                R.string.board_f4_found_scene,
                R.string.board_f4_found_mood,
                R.string.board_f4_found_themes
            ).forEach { res ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                    )
                    Text(stringResource(res), color = colors.textSecondary)
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f4_looks_good),
                onClick = onLooksGood
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun Suggestions(
    onPick: (BoardFlow4Session.Path) -> Unit,
    onList: () -> Unit,
    onEdge: (String) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var chip by remember { mutableIntStateOf(0) }
    val chips = listOf(
        R.string.board_f4_ideas_all,
        R.string.flow4_chip_popular,
        R.string.flow4_chip_for_you,
        R.string.flow4_chip_new
    )
    val paths = listOf(
        Triple(BoardFlow4Session.Path.Beautiful, colors.accentPink, BoardFlowArt.beautifulHero),
        Triple(BoardFlow4Session.Path.Funny, colors.accentYellow, BoardFlowArt.funnyHero),
        Triple(BoardFlow4Session.Path.Stickers, colors.primary, BoardFlowArt.stickerHero),
        Triple(BoardFlow4Session.Path.Move, colors.accentPurple, BoardFlowArt.motionHero),
        Triple(BoardFlow4Session.Path.Different, colors.primary, BoardFlowArt.differentHero),
        Triple(BoardFlow4Session.Path.Tell, colors.accentPink, BoardFlowArt.tellHero)
    )
    val filtered = when (chip) {
        1 -> paths.take(4)
        2 -> paths.filter {
            it.first in setOf(
                BoardFlow4Session.Path.Beautiful,
                BoardFlow4Session.Path.Funny,
                BoardFlow4Session.Path.Tell
            )
        }
        3 -> paths.takeLast(3)
        else -> paths
    }

    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW4_SUGGESTIONS),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.board_f4_suggestions_title),
                fontWeight = FontWeight.Bold,
                style = BoardType.title,
                color = colors.textPrimary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                chips.forEachIndexed { i, res ->
                    val selected = i == chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (selected) colors.primary else colors.surfaceMuted)
                            .clickable { chip = i }
                            .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(res),
                            color = if (selected) colors.onPrimary else colors.textSecondary,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
            filtered.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    row.forEach { (path, accent, image) ->
                        BoardIntentTile(
                            title = stringResource(path.titleRes()),
                            accent = accent,
                            imageRes = image,
                            onClick = { onPick(path) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            TextButton(onClick = onList) {
                Text(stringResource(R.string.board_f4_choose_path), color = colors.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                TextButton(onClick = { onEdge("b4_no_subject") }) {
                    Text(stringResource(R.string.board_f4_no_subject_title), color = colors.primary)
                }
                TextButton(onClick = { onEdge("b4_multi_subject") }) {
                    Text(stringResource(R.string.board_f4_multi_title), color = colors.primary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                TextButton(onClick = { onEdge("b4_offline") }) {
                    Text(stringResource(R.string.board_f4_offline_title), color = colors.primary)
                }
                TextButton(onClick = { onEdge("b4_safety") }) {
                    Text(stringResource(R.string.board_f4_safety_title), color = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun ChoosePath(onPick: (BoardFlow4Session.Path) -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val items = listOf(
        BoardFlow4Session.Path.Beautiful to colors.accentPink,
        BoardFlow4Session.Path.Funny to colors.accentYellow,
        BoardFlow4Session.Path.Stickers to colors.primary,
        BoardFlow4Session.Path.Move to colors.accentPurple,
        BoardFlow4Session.Path.Different to colors.primary,
        BoardFlow4Session.Path.Tell to colors.surfaceMuted
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            Text(
                stringResource(R.string.board_f4_choose_path),
                fontWeight = FontWeight.Bold,
                style = BoardType.title,
                color = colors.textPrimary
            )
            items.forEach { (path, accent) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .clickable { onPick(path) }
                        .padding(PixiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(radii.md))
                            .background(accent.copy(alpha = 0.2f))
                    ) {
                        Image(
                            painter = painterResource(path.imageRes()),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(path.titleRes()),
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            stringResource(path.descRes()),
                            color = colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun QuickPreview(onTry: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val primaryTabs = listOf(
        BoardFlow4Session.Path.Beautiful,
        BoardFlow4Session.Path.Funny,
        BoardFlow4Session.Path.Stickers,
        BoardFlow4Session.Path.Move
    )
    // Keep Tell / Different if user entered from suggestions — don't clobber to Beautiful.
    val tabs = if (BoardFlow4Session.path in primaryTabs) {
        primaryTabs
    } else {
        listOf(BoardFlow4Session.path) + primaryTabs
    }
    var tab by remember {
        mutableStateOf(BoardFlow4Session.path)
    }
    LaunchedEffect(tab) {
        BoardFlow4Session.path = tab
    }
    val previews = tab.previewImages()

    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.board_f4_see_possible),
                fontWeight = FontWeight.Bold,
                style = BoardType.title,
                color = colors.textPrimary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                tabs.forEach { path ->
                    val selected = path == tab
                    Text(
                        stringResource(path.titleRes()),
                        color = if (selected) colors.primary else colors.textSecondary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.clickable { tab = path }
                    )
                }
            }
            BoardHeroImage(resId = BoardFlowArt.baAfter, aspectRatio = 3f / 4f)
            Text(
                stringResource(tab.descRes()),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                previews.take(4).forEach { resId ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(radii.md))
                    ) {
                        Image(
                            painter = painterResource(resId),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f4_try_this),
                onClick = onTry
            )
            BoardSecondaryButton(
                text = stringResource(R.string.cd_back),
                onClick = onBack
            )
        }
    }
}

@Composable
private fun StartCreating(onStart: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_sparkles),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Text(
                stringResource(R.string.board_f4_great_choice),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            Text(
                stringResource(R.string.board_f4_start_body),
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.surfaceMuted)
                    .padding(PixiSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(radii.md))
                ) {
                    Image(
                        painter = painterResource(BoardFlow4Session.path.imageRes()),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Column {
                    Text(
                        stringResource(R.string.board_f4_chosen_path),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    Text(
                        stringResource(BoardFlow4Session.path.titleRes()),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            BoardPrimaryButton(
                text = stringResource(R.string.board_f4_start_creating),
                onClick = onStart
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardSecondaryButton(
                text = stringResource(R.string.cd_back),
                onClick = onBack
            )
        }
    }
}

@Composable
private fun MultiSubject(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val subjects = listOf(
        Triple(R.string.board_f4_subject_person, BoardFlowArt.profileAvatar, 0),
        Triple(R.string.board_f4_subject_dog, BoardFlowArt.funnyHero, 1),
        Triple(R.string.board_f4_subject_scene, BoardFlowArt.trendTravel, 2)
    )
    var selected by remember { mutableIntStateOf(0) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.board_f4_multi_title),
                fontWeight = FontWeight.Bold,
                style = BoardType.title,
                color = colors.textPrimary
            )
            Text(stringResource(R.string.board_f4_multi_body), color = colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                subjects.forEach { (label, image, index) ->
                    val on = selected == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(radii.card))
                            .border(
                                width = if (on) 2.dp else 1.dp,
                                color = if (on) colors.primary else colors.surfaceMuted,
                                shape = RoundedCornerShape(radii.card)
                            )
                            .clickable { selected = index }
                            .padding(PixiSpacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(image),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(radii.md)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(PixiSpacing.xs))
                        Text(
                            stringResource(label),
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (on) colors.primary else colors.textSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            BoardPrimaryButton(
                text = stringResource(R.string.board_f4_focus_continue),
                onClick = onContinue
            )
            BoardSecondaryButton(
                text = stringResource(R.string.cd_back),
                onClick = onBack
            )
        }
    }
}
