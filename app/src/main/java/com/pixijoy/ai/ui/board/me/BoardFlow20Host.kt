package com.pixijoy.ai.ui.board.me

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.delay

/** Flow 20 session — prompt / media mode across safety steps. */
internal object BoardFlow20Session {
    var prompt: String = "A cute dog wearing sunglasses at a beach"
    var inputTab: Int = 1 // 0 photo, 1 text
    var forceBlocked: Boolean = false
}

/**
 * Flow 20 — AI Safety & Content Moderation.
 */
@Composable
fun BoardFlow20Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val pipeline = LocalBoardPipeline.current
    when (screenId) {
        "b20_hub", "b20_input" -> SafetyInput(
            onCreate = {
                BoardFlow20Session.forceBlocked = false
                onNavigate("b20_check")
            },
            onDemoBlocked = {
                BoardFlow20Session.forceBlocked = true
                onNavigate("b20_check")
            },
            onBack = onBack
        )
        "b20_check" -> SafetyCheck(
            onAllowed = { onNavigate("b20_allowed") },
            onBlocked = { onNavigate("b20_blocked") },
            onBack = { onNavigate("b20_input") }
        )
        "b20_allowed" -> Allowed(
            onContinue = { onNavigate("b20_generating") },
            onBack = { onNavigate("b20_input") }
        )
        "b20_blocked" -> Blocked(
            onEdit = { onNavigate("b20_modify") },
            onReport = { onNavigate("b20_report") },
            onBack = { onNavigate("b20_input") }
        )
        "b20_modify" -> ModifyRetry(
            onRetry = {
                BoardFlow20Session.forceBlocked = false
                onNavigate("b20_check")
            },
            onBack = { onNavigate("b20_blocked") }
        )
        "b20_generating" -> SafeGenerating(
            onDone = { onNavigate("b20_result") },
            onBack = { onNavigate("b20_allowed") }
        )
        "b20_result" -> SafeResult(
            onSave = pipeline.openCreations,
            onShare = pipeline.openShare,
            onEdit = { onNavigate("b15_open") },
            onAnother = { onNavigate("b20_input") },
            onReport = { onNavigate("b20_report") },
            onBack = onBack
        )
        "b20_report" -> ReportContent(
            onSubmitted = { onNavigate("b20_input") },
            onBack = { onNavigate("b20_result") }
        )
        else -> SafetyInput(
            onCreate = { onNavigate("b20_check") },
            onDemoBlocked = {
                BoardFlow20Session.forceBlocked = true
                onNavigate("b20_check")
            },
            onBack = onBack
        )
    }
}

@Composable
private fun SafetyInput(
    onCreate: () -> Unit,
    onDemoBlocked: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var prompt by remember { mutableStateOf(BoardFlow20Session.prompt) }
    var tab by remember { mutableIntStateOf(BoardFlow20Session.inputTab) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW20_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f20_create_ai),
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            OutlinedTextField(
                value = prompt,
                onValueChange = {
                    prompt = it
                    BoardFlow20Session.prompt = it
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.board_f20_prompt_hint)) },
                shape = RoundedCornerShape(radii.md),
                trailingIcon = {
                    if (prompt.isNotEmpty()) {
                        IconButton(onClick = {
                            prompt = ""
                            BoardFlow20Session.prompt = ""
                        }) {
                            Icon(painter = painterResource(R.drawable.pj_ic_close), contentDescription = null)
                        }
                    }
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                listOf(
                    R.drawable.pj_ic_camera to R.string.board_f20_tab_photo,
                    R.drawable.pj_ic_edit to R.string.board_f20_tab_text
                ).forEachIndexed { i, (icon, label) ->
                    val on = i == tab
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (on) colors.primary else colors.surfaceMuted)
                            .clickable {
                                tab = i
                                BoardFlow20Session.inputTab = i
                            }
                            .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.xs)
                    ) {
                        Icon(painter = painterResource(icon), null, tint = if (on) colors.onPrimary else colors.textSecondary, modifier = Modifier.size(18.dp))
                        Text(
                            stringResource(label),
                            color = if (on) colors.onPrimary else colors.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Image(
                painter = painterResource(
                    if (BoardFlow20Session.forceBlocked) BoardFlowArt.safetyHero else BoardFlowArt.funnyHero
                ),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_f20_create),
                onClick = {
                    BoardFlow20Session.prompt = prompt
                    onCreate()
                }
            )
            TextButton(onClick = onDemoBlocked) {
                Text(stringResource(R.string.flow20_blocked_demo), color = colors.danger)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun SafetyCheck(
    onAllowed: () -> Unit,
    onBlocked: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    var progress by remember { mutableFloatStateOf(0.1f) }
    val steps = listOf(
        R.string.board_f20_chk_scan,
        R.string.board_f20_chk_safety,
        R.string.board_f20_chk_context,
        R.string.board_f20_chk_policy
    )
    LaunchedEffect(Unit) {
        while (progress < 0.95f) {
            delay(240)
            progress = (progress + 0.14f).coerceAtMost(0.95f)
        }
        delay(350)
        if (BoardFlow20Session.forceBlocked) onBlocked() else onAllowed()
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f20_checking_title),
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            Image(
                painter = painterResource(BoardFlowArt.safetyHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(BoardTheme.radii.card)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Text(
                stringResource(R.string.flow20_checking),
                style = BoardType.headline,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.board_f20_checking_body),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = PixiSpacing.sm)
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(BoardTheme.radii.pill)),
                color = colors.primary,
                trackColor = colors.surfaceMuted
            )
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                steps.forEachIndexed { i, res ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (i < doneCount) {
                            Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.primary, modifier = Modifier.size(20.dp))
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
        }
    }
}

@Composable
private fun Allowed(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
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
                    stringResource(R.string.board_f20_approved_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(colors.successSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(56.dp))
            }
            Text(
                stringResource(R.string.board_f20_allowed_body),
                textAlign = TextAlign.Center,
                color = colors.textSecondary
            )
            Image(
                painter = painterResource(BoardFlowArt.funnyHero),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.weight(1f))
            BoardPrimaryButton(
                text = stringResource(R.string.board_f20_continue_generate),
                onClick = onContinue
            )
        }
    }
}

@Composable
private fun Blocked(
    onEdit: () -> Unit,
    onReport: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val alts = listOf(
        R.drawable.pj_ic_gallery to R.string.board_f20_alt_photo,
        R.drawable.pj_ic_visibility to R.string.board_f20_alt_positive,
        R.drawable.pj_ic_block to R.string.board_f20_alt_remove
    )
    BoardAdaptiveFrame {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f20_not_allowed_title),
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
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.board_f20_not_allowed_body),
                textAlign = TextAlign.Center,
                color = colors.textSecondary,
                style = BoardType.body
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.danger.copy(0.1f))
                    .padding(PixiSpacing.lg)
            ) {
                Text(
                    stringResource(R.string.board_f20_reason_label),
                    fontWeight = FontWeight.Bold,
                    color = colors.danger
                )
                Text(
                    stringResource(R.string.board_f20_reason_body),
                    color = colors.danger
                )
            }
            Text(
                stringResource(R.string.board_f20_try_instead),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                color = colors.textPrimary
            )
            alts.forEach { (icon, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(colors.surfaceMuted)
                        .padding(PixiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Icon(painter = painterResource(icon), null, tint = colors.primary)
                    Text(stringResource(label), color = colors.textPrimary)
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f20_edit_retry), onClick = onEdit)
            TextButton(onClick = onReport) {
                Text(stringResource(R.string.flow20_report), color = colors.primary)
            }
        }
    }
}

@Composable
private fun ModifyRetry(onRetry: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var prompt by remember {
        mutableStateOf("A peaceful sunrise in the mountains with a lake")
    }
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
                    stringResource(R.string.board_f20_edit_prompt),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(radii.md)
            )
            Image(
                painter = painterResource(BoardFlowArt.trendTravel),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_f20_try_again),
                onClick = {
                    BoardFlow20Session.prompt = prompt
                    onRetry()
                }
            )
        }
    }
}

@Composable
private fun SafeGenerating(onDone: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    var progress by remember { mutableFloatStateOf(0.12f) }
    val steps = listOf(
        R.string.board_f20_gen_prepare,
        R.string.board_f20_gen_model,
        R.string.board_f20_gen_details,
        R.string.board_f20_gen_final
    )
    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(260)
            progress = (progress + 0.14f).coerceAtMost(1f)
        }
        delay(300)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f20_generating_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            Icon(painter = painterResource(R.drawable.pj_ic_sparkles), null, tint = colors.primary, modifier = Modifier.size(56.dp))
            Text(
                stringResource(R.string.board_f20_generating_body),
                textAlign = TextAlign.Center,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = PixiSpacing.md)
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(BoardTheme.radii.pill)),
                color = colors.primary,
                trackColor = colors.surfaceMuted
            )
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                steps.forEachIndexed { i, res ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (i < doneCount) {
                            Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.primary, modifier = Modifier.size(20.dp))
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
        }
    }
}

@Composable
private fun SafeResult(
    onSave: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onAnother: () -> Unit,
    onReport: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val actions = listOf(
        Triple(R.drawable.pj_ic_folder, R.string.board_f16_save_gallery, onSave),
        Triple(R.drawable.pj_ic_share, R.string.board_f16_share_apps, onShare),
        Triple(R.drawable.pj_ic_edit, R.string.flow19_edit_more, onEdit),
        Triple(R.drawable.pj_ic_sparkles, R.string.board_f20_make_another, onAnother)
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
                    stringResource(R.string.board_f20_result_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Text(
                stringResource(R.string.board_f20_ready),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Image(
                painter = painterResource(BoardFlowArt.trendTravel),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            actions.forEach { (icon, label, action) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .clickable(onClick = action)
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Icon(painter = painterResource(icon), null, tint = colors.primary)
                    Text(stringResource(label), modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, color = colors.textPrimary)
                    Icon(painter = painterResource(R.drawable.pj_ic_chevron_right), null, tint = colors.textSecondary)
                }
            }
            TextButton(onClick = onReport) {
                Text(stringResource(R.string.board_f20_report_cta), color = colors.danger)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun ReportContent(onSubmitted: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val reasons = listOf(
        R.string.board_f20_rep_harm,
        R.string.board_f20_rep_hate,
        R.string.board_f20_rep_adult,
        R.string.board_f20_rep_misinfo,
        R.string.board_f20_rep_copyright,
        R.string.flow20_cat_other
    )
    val selected = remember { mutableStateListOf<Int>() }
    var details by remember { mutableStateOf("") }
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
                    stringResource(R.string.board_f20_report_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(BoardFlowArt.trendTravel),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(radii.md)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.board_f20_report_body),
                color = colors.textSecondary
            )
            reasons.forEachIndexed { i, res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .clickable {
                            if (selected.contains(i)) selected.remove(i) else selected.add(i)
                        }
                        .padding(vertical = PixiSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selected.contains(i),
                        onCheckedChange = {
                            if (it) selected.add(i) else selected.remove(i)
                        }
                    )
                    Text(stringResource(res), color = colors.textPrimary)
                }
            }
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.board_f20_report_details)) },
                shape = RoundedCornerShape(radii.md),
                minLines = 3
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_f20_submit_report),
                onClick = {
                    Toast.makeText(
                        context,
                        context.getString(R.string.board_f20_report_thanks),
                        Toast.LENGTH_SHORT
                    ).show()
                    onSubmitted()
                }
            )
        }
    }
}
