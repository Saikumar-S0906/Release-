package com.pixijoy.ai.ui.board.ctools

import androidx.annotation.DrawableRes

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
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
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardGateScreen
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardProcessingHero
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.delay
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.pixijoy.ai.di.BoardAiEntryPoint
import com.pixijoy.ai.ui.board.BoardCToolEngine
import com.pixijoy.ai.ui.board.BoardMediaSession
import com.pixijoy.ai.ui.board.BoardSafeResultImage
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardEdgeKind
import com.pixijoy.ai.ui.board.components.BoardResultFooter
import com.pixijoy.ai.ui.board.components.BoardSoftEdgeGate
import com.pixijoy.ai.ui.board.components.decodeBoardDrawable
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Slider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import com.pixijoy.ai.ui.board.components.BoardEdgeActions
import com.pixijoy.ai.ui.board.components.BoardScreenHeader
import kotlinx.coroutines.launch

/** C08 session — soft people add/remove prefs (local demo only). */
internal object BoardFlowC08Session {
    var actionIndex: Int = 0
    var modeRemove: Boolean = true
    var maskConfirmed: Boolean = false
    var variationIndex: Int = 0
    var prompt: String = ""
    var usedEnhanceFallback: Boolean = false

    var usedSoftFallback: Boolean = false
    @Volatile
    var resultBitmap: android.graphics.Bitmap? = null
        private set

    fun setResult(bitmap: android.graphics.Bitmap?) {
        val old = resultBitmap
        resultBitmap = bitmap
        if (old != null && old !== bitmap && !old.isRecycled) {
            runCatching { old.recycle() }
        }
    }
}

/**
 * C08 — Add or Remove People.
 * Soft: simulated mark / mask / inpaint; consent UI only (no real generative people edits).
 */
@Composable
fun BoardFlowC08Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    val step = BoardCTool.stepOf(screenId)
    when (step) {
        "intro", "hub", "" -> C08ChooseAction(
            onRemove = { onNavigate("b_c08_photo") },
            onFunny = { onNavigate("b6_choose") },
            onStickers = { onNavigate("b13_photo") },
            onScene = { onNavigate("b_c07_intro") },
            onDifferent = { onNavigate("b_diff_choose") },
            onBack = onBack,
            onTab = tab
        )
        "photo" -> C08PreviewPrompt(
            onSubmit = { onNavigate("b_c08_options") },
            onBack = { onNavigate("b_c08_intro") }
        )
        "options" -> C08MarkTarget(
            onSubmit = { onNavigate("b_c08_adjust") },
            onBack = { onNavigate("b_c08_photo") }
        )
        "adjust" -> C08ConfirmMask(
            onSubmit = { onNavigate("b_c08_consent") },
            onBack = { onNavigate("b_c08_options") }
        )
        "consent" -> C08Consent(
            onConfirm = { onNavigate("b_c08_processing") },
            onDeny = { onNavigate("b_c08_consent_denied") },
            onBack = { onNavigate("b_c08_adjust") }
        )
        "consent_denied" -> BoardGateScreen(
            title = stringResource(R.string.board_ct_consent_denied_title),
            body = stringResource(R.string.board_ct_consent_denied_body),
            primary = stringResource(R.string.board_ct_review_consent),
            onPrimary = { onNavigate("b_c08_consent") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = onBack
        )
        "processing" -> C08Processing(
            onDone = { onNavigate("b_c08_result") },
            onOffline = { onNavigate("b_c08_offline") },
            onLowCredits = { onNavigate("b_c08_low_credits") },
            onBack = { onNavigate("b_c08_consent") }
        )
        "result" -> C08Result(
            onUse = { onNavigate("b_c08_share") },
            onVariations = { onNavigate("b_c08_variations") },
            onBack = { onNavigate("b_c08_consent") }
        )
        "variations" -> C08Variations(
            onPick = { onNavigate("b_c08_share") },
            onBack = { onNavigate("b_c08_result") }
        )
        "share" -> C08Share(
            onSave = { onNavigate("b8_ready") },
            onShare = { onNavigate("b8_share") },
            onEdit = { onNavigate("b_c08_options") },
            onDone = onBack,
            onBack = { onNavigate("b_c08_result") }
        )
        "offline" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_offline_title),
            body = stringResource(R.string.board_ct_offline_body),
            primary = stringResource(R.string.board_f3_try_again),
            onPrimary = { onNavigate("b_c08_processing") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate("b_c08_consent") }
        )
        "low_credits" -> BoardGateScreen(
            title = stringResource(R.string.board_f19_low_credits_title),
            body = stringResource(R.string.board_f19_low_credits_body),
            primary = stringResource(R.string.board_f19_get_credits),
            onPrimary = { onNavigate("b23_dash") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate("b_c08_consent") }
        )
        "safety" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_safety_title),
            body = stringResource(R.string.board_f4_safety_body),
            primary = stringResource(R.string.board_f3_try_another),
            onPrimary = { onNavigate("b_c08_photo") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = onBack
        )
        else -> C08ChooseAction(
            onRemove = { onNavigate("b_c08_photo") },
            onFunny = { onNavigate("b6_choose") },
            onStickers = { onNavigate("b13_photo") },
            onScene = { onNavigate("b_c07_intro") },
            onDifferent = { onNavigate("b_diff_choose") },
            onBack = onBack,
            onTab = tab
        )
    }
}

@Composable
private fun C08ChooseAction(
    onRemove: () -> Unit,
    onFunny: () -> Unit,
    onStickers: () -> Unit,
    onScene: () -> Unit,
    onDifferent: () -> Unit,
    onBack: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC08Session.actionIndex) }
    val actions = listOf(
        Triple(R.string.board_c08_action_remove, R.string.board_c08_action_remove_sub, onRemove),
        Triple(R.string.board_c08_action_meme, R.string.board_c04_intent_funny_sub, onFunny),
        Triple(R.string.board_c08_action_sticker, R.string.board_c04_intent_stickers_sub, onStickers),
        Triple(R.string.board_c08_action_beach, R.string.board_ct_tip_scene, onScene),
        Triple(R.string.board_c08_action_cinematic, R.string.board_c04_intent_diff_sub, onDifferent)
    )
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C08_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.create_extra_people),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        stringResource(R.string.board_c08_slogan),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
            Text(
                stringResource(R.string.board_c08_tell_title),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Text(
                stringResource(R.string.board_c04_what_create),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            actions.forEachIndexed { index, (title, sub, action) ->
                val on = selected == index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(if (on) colors.primary.copy(0.14f) else colors.surfaceMuted)
                        .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md)) else Modifier)
                        .clickable {
                            selected = index
                            BoardFlowC08Session.actionIndex = index
                            action()
                        }
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Icon(
                        painter = painterResource(
                            if (index == 0) R.drawable.pj_ic_person_remove else R.drawable.pj_ic_sparkles
                        ),
                        contentDescription = null,
                        tint = if (on) colors.primary else colors.textSecondary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(title), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text(stringResource(sub), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_c08_action_remove), onClick = onRemove)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C08PreviewPrompt(onSubmit: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var prompt by remember {
        mutableStateOf(
            BoardFlowC08Session.prompt.ifEmpty {
                ""
            }
        )
    }
    val defaultHint = stringResource(R.string.board_c08_prompt_sample)
    LaunchedEffect(Unit) {
        if (prompt.isEmpty()) {
            prompt = defaultHint
            BoardFlowC08Session.prompt = defaultHint
        }
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C08_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c08_preview_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(R.drawable.board_c08_people),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            OutlinedTextField(
                value = prompt,
                onValueChange = {
                    prompt = it
                    BoardFlowC08Session.prompt = it
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(radii.md),
                label = { Text(stringResource(R.string.tell_pixijoy_hint)) }
            )
            BoardPrimaryButton(text = stringResource(R.string.flow7_submit), onClick = onSubmit)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C08MarkTarget(onSubmit: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var modeRemove by remember { mutableStateOf(BoardFlowC08Session.modeRemove) }
    var marked by remember { mutableStateOf(true) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C08_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c08_mark_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card))
                    .clickable { marked = true }
            ) {
                Image(
                    painter = painterResource(R.drawable.board_c08_people),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (marked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = (-24).dp, y = (-48).dp)
                            .size(88.dp)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }
            Text(
                stringResource(R.string.board_c08_mark_hint),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                ModeChip(
                    selected = modeRemove,
                    icon = R.drawable.pj_ic_person_remove,
                    label = stringResource(R.string.board_ct_opt_remove_person),
                    onClick = {
                        modeRemove = true
                        BoardFlowC08Session.modeRemove = true
                    }
                )
                ModeChip(
                    selected = !modeRemove,
                    icon = R.drawable.pj_ic_person_add,
                    label = stringResource(R.string.board_c08_add_person),
                    onClick = {
                        modeRemove = false
                        BoardFlowC08Session.modeRemove = false
                    }
                )
            }
            BoardPrimaryButton(text = stringResource(R.string.flow7_submit), onClick = onSubmit)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun ModeChip(
    selected: Boolean,
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(radii.pill))
            .background(if (selected) colors.primary else colors.surfaceMuted)
            .clickable(onClick = onClick)
            .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
    ) {
        Icon(painter = painterResource(icon), null, tint = if (selected) colors.onPrimary else colors.textSecondary, modifier = Modifier.size(18.dp))
        Text(label, color = if (selected) colors.onPrimary else colors.textSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun C08ConfirmMask(onSubmit: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C08_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c08_mask_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                Image(
                    painter = painterResource(R.drawable.board_c08_people),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = (-24).dp, y = (-48).dp)
                        .size(96.dp)
                        .border(3.dp, Color(0xFFFF4D6D), CircleShape)
                        .background(Color(0xFFFF4D6D).copy(0.18f), CircleShape)
                )
            }
            Text(
                stringResource(R.string.board_ct_tip_people),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            BoardPrimaryButton(
                text = stringResource(R.string.flow7_submit),
                onClick = {
                    BoardFlowC08Session.maskConfirmed = true
                    onSubmit()
                }
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C08Consent(onConfirm: () -> Unit, onDeny: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TestTags.FLOW_C08_HOST)
        ) {
            Image(
                painter = painterResource(R.drawable.board_c08_people),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.35f
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.45f))
                    .clickable(onClick = onBack)
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
                    stringResource(R.string.board_ct_consent_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
                Icon(painter = painterResource(R.drawable.pj_ic_person_remove), null, tint = colors.primary, modifier = Modifier.size(40.dp))
                Text(
                    stringResource(R.string.board_c08_consent_prompt),
                    textAlign = TextAlign.Center,
                    color = colors.textSecondary
                )
                Text(
                    stringResource(R.string.board_ct_consent_body),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                BoardPrimaryButton(text = stringResource(R.string.board_ct_consent_agree), onClick = onConfirm)
                TextButton(onClick = onDeny) {
                    Text(stringResource(R.string.board_ct_consent_disagree), color = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun C08Processing(
    onDone: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    var step by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    val checks = listOf(
        R.string.board_c08_check_analyze,
        R.string.board_c08_check_identify,
        R.string.board_c08_check_style,
        R.string.board_c08_check_create
    )
    LaunchedEffect(Unit) {
        BoardFlowC08Session.setResult(null)
        BoardFlowC08Session.usedSoftFallback = false
        BoardFlowC08Session.usedEnhanceFallback = false
        step = 1
        progress = 0.25f
        val removeMode = BoardFlowC08Session.modeRemove
        val result = BoardCToolEngine.run(context) { pipe, src ->
            val hasFace = runCatching { pipe.hasFace(src) }.getOrDefault(false)
            if (removeMode && !hasFace) {
                BoardFlowC08Session.usedEnhanceFallback = true
                pipe.enhanceFaceLook(src)
            } else {
                pipe.peopleEdit(src, removeMode)
            }
        }
        step = 2
        progress = 0.5f
        delay(80)
        step = 3
        progress = 0.75f
        delay(80)
        step = 4
        progress = 1f
        if (result != null) {
            BoardFlowC08Session.setResult(result)
            BoardFlowC08Session.usedSoftFallback = false
        } else {
            BoardFlowC08Session.usedSoftFallback = true
        }
        delay(200)
        onDone()
    }
    BoardAdaptiveFrame {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.processingSurface)
                .statusBarsPadding()
                .testTag(TestTags.FLOW_C08_HOST)
                .padding(PixiSpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BoardProcessingHero(progress = progress, darkSurface = true)
                Spacer(modifier = Modifier.height(PixiSpacing.md))
                Text(
                    stringResource(R.string.create_extra_people),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    stringResource(
                        if (BoardFlowC08Session.modeRemove) R.string.board_c08_removing
                        else R.string.board_c08_adding
                    ),
                    color = Color.White.copy(0.85f)
                )
                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                checks.forEachIndexed { i, res ->
                    if (i < step) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                        ) {
                            Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(20.dp))
                            Text(stringResource(res), color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(PixiSpacing.md))
                BoardEdgeActions(
                    onOffline = onOffline,
                    onLowCredits = null,
                    darkSurface = true
                )
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.cd_back), color = Color.White.copy(0.8f))
                }
            }
        }
    }
}

@Composable
private fun C08Result(
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onRemake: () -> Unit,
    onPublish: () -> Unit,
    onVariations: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C08_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.size(PixiSpacing.sm))
                Text(
                    stringResource(R.string.board_f7_result_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
            ) {
                BoardSafeResultImage(
                    bitmap = BoardFlowC08Session.resultBitmap,
                    fallbackRes = R.drawable.board_c08_people,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(0.65f))
                            )
                        )
                        .padding(PixiSpacing.lg)
                ) {
                    Text(
                        stringResource(
                            if (BoardFlowC08Session.modeRemove) R.string.board_c08_result_removed
                            else R.string.board_c08_result_added
                        ),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (BoardFlowC08Session.usedSoftFallback || BoardFlowC08Session.usedEnhanceFallback) {
                Text(
                    stringResource(
                        if (BoardFlowC08Session.usedEnhanceFallback) R.string.ai_status_on_device
                        else R.string.board_c04_soft_fallback
                    ),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            BoardResultFooter(
                onSave = {
                    val out = BoardFlowC08Session.resultBitmap
                    val ok = if (out != null && !out.isRecycled) {
                        BoardDrawableIo.saveBitmapToGallery(context, out, filePrefix = "PixiJoy_People")
                    } else {
                        BoardDrawableIo.saveToGallery(
                            context,
                            R.drawable.board_c08_people,
                            filePrefix = "PixiJoy_People"
                        )
                    }
                    Toast.makeText(
                        context,
                        if (ok) R.string.board_c04_saved else R.string.board_c04_save_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onShare = onShare,
                onEdit = onEdit,
                onExport = onExport,
                onRemake = onRemake,
                onPublish = onPublish
            )
            TextButton(onClick = onVariations) {
                Text(stringResource(R.string.board_f7_another_style), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C08Result(onUse: () -> Unit, onVariations: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val actions = listOf(
        R.drawable.pj_ic_download to R.string.board_f7_download,
        R.drawable.pj_ic_share to R.string.board_f2_share,
        R.drawable.pj_ic_edit to R.string.board_f7_edit,
        R.drawable.pj_ic_refresh to R.string.board_f7_regenerate
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C08_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.size(PixiSpacing.sm))
                Text(
                    stringResource(R.string.board_f7_result_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
            ) {
                Image(
                    painter = painterResource(R.drawable.board_c08_people),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(0.65f))
                            )
                        )
                        .padding(PixiSpacing.lg)
                ) {
                    Text(
                        stringResource(
                            if (BoardFlowC08Session.modeRemove) R.string.board_c08_result_removed
                            else R.string.board_c08_result_added
                        ),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                actions.forEach { (icon, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(onClick = onVariations)
                    ) {
                        Icon(painter = painterResource(icon), null, tint = colors.primary)
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f7_use_this), onClick = onUse)
            TextButton(onClick = onVariations) {
                Text(stringResource(R.string.board_f7_another_style), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C08Variations(onPick: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC08Session.variationIndex) }
    val arts = listOf(
        R.drawable.board_c08_people,
        R.drawable.board_c00_hero,
        R.drawable.board_c04_cutout,
        R.drawable.board_ba_after
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C08_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f7_result_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            arts.chunked(2).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEachIndexed { col, art ->
                        val index = rowIndex * 2 + col
                        val on = selected == index
                        Image(
                            painter = painterResource(art),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(radii.md))
                                .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md)) else Modifier)
                                .clickable {
                                    selected = index
                                    BoardFlowC08Session.variationIndex = index
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f7_use_this), onClick = onPick)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C08Share(
    onSave: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C08_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c08_share_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            BoardSafeResultImage(
                bitmap = BoardFlowC08Session.resultBitmap,
                fallbackRes = R.drawable.board_c08_people,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(BoardTheme.radii.card))
            )
            BoardPrimaryButton(text = stringResource(R.string.board_f12_save), onClick = onSave)
            BoardPrimaryButton(text = stringResource(R.string.board_f2_share), onClick = onShare)
            BoardOutlinedButton(text = stringResource(R.string.board_f7_edit), onClick = onEdit)
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.done), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}
