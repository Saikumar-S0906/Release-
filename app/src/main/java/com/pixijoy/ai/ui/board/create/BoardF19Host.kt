package com.pixijoy.ai.ui.board.create

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Block
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
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.BoardNavPrefs
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardCreditChip
import com.pixijoy.ai.ui.board.components.BoardEdgeKind
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardHeroImage
import com.pixijoy.ai.ui.board.components.BoardLottie
import com.pixijoy.ai.ui.board.components.BoardMotionKey
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardResultFooter
import com.pixijoy.ai.ui.board.components.BoardSoftEdgeGate
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.components.rememberBoardHaptics
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Flow 19 session — intent / style for generation kit. */
internal object BoardFlow19Session {
    var intentIndex: Int = 1 // Funny
    var styleLabel: String = "Funny"
    var outputLabel: String = "Image"
    var creditCost: Int = 1
}

/**
 * Flow 19 — AI Generation States & Recovery (shared kit).
 * Happy path: start → analyzing → generating → almost → success.
 * Edges: canceled / offline / busy / timeout / unsupported / safety / low credits / failed.
 */
@Composable
fun BoardF19Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val pipeline = LocalBoardPipeline.current
    when (screenId) {
        "b19_start" -> StartGeneration(
            onGenerate = { onNavigate("b19_analyzing") },
            onDemo = { onNavigate(it) },
            onBack = onBack
        )
        "b19_analyzing" -> ProgressPhase(
            phase = ProgressPhase.Analyzing,
            testTag = TestTags.FLOW19_ANALYZING,
            onAdvance = { onNavigate("b19_generating") },
            onCancel = { onNavigate("b19_canceled") }
        )
        "b19_generating" -> ProgressPhase(
            phase = ProgressPhase.Generating,
            testTag = TestTags.FLOW19_GENERATING,
            onAdvance = { onNavigate("b19_almost") },
            onCancel = { onNavigate("b19_canceled") }
        )
        "b19_almost" -> ProgressPhase(
            phase = ProgressPhase.Almost,
            testTag = TestTags.FLOW19_ALMOST,
            onAdvance = { onNavigate("b19_success") },
            onCancel = { onNavigate("b19_canceled") }
        )
        "b19_success" -> Success(
            onEdit = { onNavigate("b15_open") },
            onShare = pipeline.openShare,
            onSave = {
                pipeline.openCreations()
            },
            onExport = pipeline.openExport,
            onRemake = { onNavigate("b19_start") },
            onPublish = { onNavigate("b8_publish") },
            onBack = onBack
        )
        "b19_canceled" -> RecoveryState(
            icon = R.drawable.pj_ic_close,
            iconTint = Color(0xFFE53935),
            title = stringResource(R.string.flow19_canceled_title),
            body = stringResource(R.string.flow19_canceled_body),
            primary = stringResource(R.string.board_f3_try_again),
            onPrimary = { onNavigate("b19_start") },
            secondary = stringResource(R.string.flow19_go_home),
            onSecondary = onBack,
            heroRes = BoardFlowArt.aiFail,
            testTag = TestTags.FLOW19_CANCELED
        )
        "b19_offline" -> Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TestTags.FLOW19_OFFLINE)
        ) {
            BoardSoftEdgeGate(
                kind = BoardEdgeKind.Offline,
                onPrimary = { onNavigate("b19_analyzing") },
                onSecondary = { onNavigate("b15_open") },
                secondary = stringResource(R.string.flow19_continue_offline)
            )
        }
        "b19_busy" -> RecoveryState(
            icon = R.drawable.pj_ic_cloud_off,
            iconTint = BoardTheme.colors.primary,
            title = stringResource(R.string.flow19_busy_title),
            body = stringResource(R.string.flow19_busy_body),
            primary = stringResource(R.string.board_f3_try_again),
            onPrimary = { onNavigate("b19_analyzing") },
            secondary = stringResource(R.string.flow19_notify_me),
            onSecondary = {
                // toast via local — handled inside with notify flag
            },
            secondaryIsNotify = true,
            tip = stringResource(R.string.board_f19_tip_notify),
            heroRes = BoardFlowArt.aiBusy,
            testTag = TestTags.FLOW19_BUSY
        )
        "b19_timeout" -> RecoveryState(
            icon = R.drawable.pj_ic_timer,
            iconTint = Color(0xFFE53935),
            title = stringResource(R.string.flow19_timeout_title),
            body = stringResource(R.string.flow19_timeout_body),
            primary = stringResource(R.string.board_f3_try_again),
            onPrimary = { onNavigate("b19_start") },
            secondary = stringResource(R.string.flow19_go_back),
            onSecondary = onBack,
            tip = stringResource(R.string.board_f19_tip_trouble),
            heroRes = BoardFlowArt.aiFail,
            testTag = TestTags.FLOW19_TIMEOUT
        )
        "b19_unsupported" -> RecoveryState(
            icon = R.drawable.pj_ic_block,
            iconTint = Color(0xFFE53935),
            title = stringResource(R.string.flow19_unsupported_title),
            body = stringResource(R.string.flow19_unsupported_body),
            primary = stringResource(R.string.flow19_try_something_else),
            onPrimary = { onNavigate("b3_chooser") },
            secondary = stringResource(R.string.flow19_see_suggestions),
            onSecondary = { onNavigate("b4_suggestions") },
            heroRes = BoardFlowArt.aiEmpty,
            testTag = TestTags.FLOW19_UNSUPPORTED
        )
        "b19_safety" -> Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TestTags.FLOW19_SAFETY)
        ) {
            BoardSoftEdgeGate(
                kind = BoardEdgeKind.Safety,
                onPrimary = { onNavigate("b19_start") },
                onSecondary = { onNavigate("b20_blocked") },
                secondary = stringResource(R.string.flow19_learn_more)
            )
        }
        "b19_low_credits" -> Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TestTags.FLOW19_LOW_CREDITS)
        ) {
            BoardSoftEdgeGate(
                kind = BoardEdgeKind.LowCredits,
                onPrimary = { onNavigate("b23_dash") },
                onSecondary = pipeline.openPro,
                secondary = stringResource(R.string.flow19_view_plans)
            )
        }
        "b19_failed" -> RecoveryState(
            icon = R.drawable.pj_ic_error,
            iconTint = Color(0xFFE53935),
            title = stringResource(R.string.flow19_failed_title),
            body = stringResource(R.string.flow19_failed_body),
            primary = stringResource(R.string.board_f3_try_again),
            onPrimary = { onNavigate("b19_start") },
            secondary = stringResource(R.string.flow19_contact_support),
            onSecondary = { onNavigate("b11_help") },
            tip = stringResource(R.string.board_f19_tip_failed),
            heroRes = BoardFlowArt.aiFail,
            testTag = TestTags.FLOW19_FAILED
        )
        else -> StartGeneration(
            onGenerate = { onNavigate("b19_analyzing") },
            onDemo = { onNavigate(it) },
            onBack = onBack
        )
    }
}

private enum class ProgressPhase { Analyzing, Generating, Almost }

@Composable
private fun StartGeneration(
    onGenerate: () -> Unit,
    onDemo: (String) -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var intent by remember { mutableIntStateOf(BoardFlow19Session.intentIndex) }
    val intents = listOf(
        R.string.make_it_beautiful to "Beautiful",
        R.string.make_it_funny to "Funny",
        R.string.make_it_move to "Move",
        R.string.board_f19_intent_tell to "Tell"
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW19_START),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow19_ready_title),
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                BoardCreditChip(credits = 5)
            }
            Image(
                painter = painterResource(BoardFlowArt.beautifulHero),
                contentDescription = stringResource(R.string.flow19_photo_preview),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.surfaceMuted)
                    .padding(PixiSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                Text(
                    stringResource(intents[intent].first),
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    stringResource(R.string.board_f19_intent_body),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    intents.forEachIndexed { i, (res, style) ->
                        val on = i == intent
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(radii.pill))
                                .background(if (on) colors.primary else colors.surface)
                                .clickable {
                                    intent = i
                                    BoardFlow19Session.intentIndex = i
                                    BoardFlow19Session.styleLabel = style
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
                Text(
                    stringResource(
                        R.string.board_f19_style_output,
                        BoardFlow19Session.styleLabel,
                        BoardFlow19Session.outputLabel
                    ),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                stringResource(R.string.flow19_credit_cost, BoardFlow19Session.creditCost),
                color = colors.textSecondary
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_f19_generate_spark),
                onClick = onGenerate
            )
            Text(
                stringResource(R.string.board_f19_demo_edges),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                listOf(
                    R.string.flow19_demo_safety to "b19_safety",
                    R.string.flow19_demo_low_credits to "b19_low_credits",
                    R.string.board_f19_demo_offline to "b19_offline",
                    R.string.board_f19_demo_busy to "b19_busy",
                    R.string.board_f19_demo_failed to "b19_failed"
                ).forEach { (label, id) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .border(1.dp, colors.primary, RoundedCornerShape(radii.pill))
                            .clickable { onDemo(id) }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                    ) {
                        Text(stringResource(label), color = colors.primary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun ProgressPhase(
    phase: ProgressPhase,
    testTag: String,
    onAdvance: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var progress by remember {
        mutableFloatStateOf(
            when (phase) {
                ProgressPhase.Analyzing -> 0.08f
                ProgressPhase.Generating -> 0.45f
                ProgressPhase.Almost -> 0.82f
            }
        )
    }
    val target = when (phase) {
        ProgressPhase.Analyzing -> 0.28f
        ProgressPhase.Generating -> 0.78f
        ProgressPhase.Almost -> 0.96f
    }
    val title = when (phase) {
        ProgressPhase.Analyzing -> R.string.flow19_analyzing_title
        ProgressPhase.Generating -> R.string.flow19_generating_title
        ProgressPhase.Almost -> R.string.flow19_almost_title
    }
    val subtitle = when (phase) {
        ProgressPhase.Analyzing -> R.string.board_f19_analyzing_body
        ProgressPhase.Generating -> R.string.board_f19_generating_body
        ProgressPhase.Almost -> R.string.board_f19_almost_body
    }
    val steps = when (phase) {
        ProgressPhase.Analyzing -> listOf(
            R.string.board_f19_chk_faces,
            R.string.board_f19_chk_context,
            R.string.board_f19_chk_objects,
            R.string.board_f19_chk_ideas
        )
        ProgressPhase.Generating -> listOf(
            R.string.board_f19_chk_model,
            R.string.board_f19_chk_creative,
            R.string.board_f19_chk_details,
            R.string.board_f19_chk_almost
        )
        ProgressPhase.Almost -> listOf(
            R.string.board_f19_chk_quality,
            R.string.board_f19_chk_fine,
            R.string.board_f19_chk_share,
            R.string.board_f19_chk_wrap
        )
    }
    @DrawableRes val icon: Int = when (phase) {
        ProgressPhase.Analyzing -> R.drawable.pj_ic_search
        else -> R.drawable.pj_ic_sparkles
    }
    LaunchedEffect(phase) {
        while (progress < target) {
            delay(220)
            progress = (progress + 0.06f).coerceAtMost(target)
        }
        delay(500)
        onAdvance()
    }
    val doneCount = ((progress / target) * steps.size).toInt().coerceIn(0, steps.size)
    val phaseHero = when (phase) {
        ProgressPhase.Analyzing -> BoardFlowArt.aiAnalyzing
        ProgressPhase.Generating -> BoardFlowArt.aiBusy
        ProgressPhase.Almost -> BoardFlowArt.aiSuccess
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(testTag),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BoardHeroImage(resId = phaseHero, aspectRatio = 16f / 9f)
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(icon), null, tint = colors.primary, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Text(
                stringResource(title),
                fontWeight = FontWeight.Bold,
                style = BoardType.title,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(subtitle),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = PixiSpacing.sm)
            )
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            if (phase == ProgressPhase.Generating) {
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
                        style = BoardType.title,
                        color = colors.primary
                    )
                }
            } else {
                Text(
                    "${(progress * 100).toInt()}%",
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(radii.pill)),
                    color = colors.primary,
                    trackColor = colors.surfaceMuted
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                steps.forEachIndexed { i, res ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (i < doneCount) {
                            Icon(painter = painterResource(R.drawable.pj_ic_check_circle),
                                null,
                                tint = colors.success,
                                modifier = Modifier.size(20.dp)
                            )
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
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            BoardOutlinedButton(text = stringResource(R.string.board_f19_cancel), onClick = onCancel)
        }
    }
}

@Composable
private fun Success(
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onExport: () -> Unit,
    onRemake: () -> Unit,
    onPublish: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    val alts = listOf(
        BoardFlowArt.baAfter,
        BoardFlowArt.funnyHero,
        BoardFlowArt.beautifulHero
    )
    val haptics = rememberBoardHaptics()
    LaunchedEffect(Unit) { haptics.success() }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW19_SUCCESS),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow19_success_title),
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                BoardLottie(key = BoardMotionKey.SparkleSuccess, size = 40.dp)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                Image(
                    painter = painterResource(BoardFlowArt.baAfter),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Text(
                    stringResource(R.string.board_f17_overlay),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(PixiSpacing.lg)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                alts.forEach { res ->
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
            BoardResultFooter(
                onSave = {
                    scope.launch {
                        BoardNavPrefs.markUserCreations(context)
                        onSave()
                    }
                },
                onShare = onShare,
                onEdit = onEdit,
                onExport = onExport,
                onRemake = onRemake,
                onPublish = onPublish
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun RecoveryState(
    @DrawableRes icon: Int,
    iconTint: Color,
    title: String,
    body: String,
    primary: String,
    onPrimary: () -> Unit,
    secondary: String,
    onSecondary: () -> Unit,
    tip: String? = null,
    secondaryIsNotify: Boolean = false,
    @DrawableRes heroRes: Int? = null,
    testTag: String
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(testTag),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Spacer(modifier = Modifier.height(PixiSpacing.xxl))
            if (heroRes != null) {
                BoardHeroImage(resId = heroRes, aspectRatio = 16f / 9f)
            } else {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(icon), null, tint = iconTint, modifier = Modifier.size(44.dp))
                }
            }
            Text(
                title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            BoardPrimaryButton(text = primary, onClick = onPrimary)
            if (secondaryIsNotify) {
                BoardOutlinedButton(
                    text = secondary,
                    onClick = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.board_f19_notify_set),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } else {
                TextButton(onClick = onSecondary) {
                    Text(secondary, color = colors.primary)
                }
            }
            if (tip != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BoardTheme.radii.md))
                        .background(colors.primary.copy(0.1f))
                        .padding(PixiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    Icon(
                        painter = painterResource(
                            if (secondaryIsNotify) R.drawable.pj_ic_notifications else R.drawable.pj_ic_info
                        ),
                        contentDescription = null,
                        tint = colors.primary
                    )
                    Text(tip, color = colors.primary, style = MaterialTheme.typography.bodySmall)
                }
            }
            BoardTrustFooter()
        }
    }
}
