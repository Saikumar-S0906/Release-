package com.pixijoy.ai.ui.board.create

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.pixijoy.ai.ui.board.BoardCToolEngine
import com.pixijoy.ai.ui.board.BoardMediaSession
import com.pixijoy.ai.ui.board.BoardSafeResultImage
import com.pixijoy.ai.ui.board.BoardSessionPhoto
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardColorAdjust
import com.pixijoy.ai.ui.board.components.BoardCreditChip
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardEdgeActions
import com.pixijoy.ai.ui.board.components.BoardEdgeKind
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardProgressTrackHeight
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardSoftEdgeGate
import com.pixijoy.ai.ui.board.components.BoardSoftProgress
import com.pixijoy.ai.ui.board.components.BoardSoftPulse
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.delay

/** Different session — soft AI-art prefs (local demo only). */
internal object BoardFlowDifferentSession {
    var styleIndex: Int = 0
    var photoIndex: Int = 0
    var strength: Float = 0.8f
    var prompt: String = ""
    var aspectIndex: Int = 2
    var variationIndex: Int = 0
    var styleTab: Int = 0
    /** -1 = style look; 0..4 = era pack (80s / 90s / 2000s / film / now). */
    var eraIndex: Int = -1
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

private data class DiffStyle(
    @StringRes val title: Int,
    @StringRes val badge: Int,
    @DrawableRes val art: Int,
    val category: Int // 0 all, 1 trending, 2 art, 3 cartoon
)

/**
 * FLOW_06 — Make it Different (AI Art).
 * Soft: simulated style transfer; no real generative AI.
 * Board: flows/additional/REMATCH_EXTRA_Different.png (superseded cores archived under flows/_superseded/).
 */
@Composable
fun BoardFlowDifferentHost(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    val pipeline = LocalBoardPipeline.current
    val step = when {
        screenId == "b_different" || screenId == "b_diff_choose" || screenId.endsWith("_choose") -> "photo"
        screenId.contains("_photo") -> "photo"
        screenId.contains("_options") || screenId.contains("_styles") -> "options"
        screenId.contains("_adjust") -> "adjust"
        screenId.contains("_processing") -> "processing"
        screenId.contains("_result") -> "result"
        screenId.contains("_variations") -> "variations"
        screenId.contains("_share") -> "share"
        screenId.contains("_offline") -> "offline"
        screenId.contains("_low_credits") -> "low_credits"
        screenId.contains("_safety") -> "safety"
        else -> "photo"
    }
    when (step) {
        "photo" -> DiffSelectPhoto(
            onPick = { onNavigate("b_diff_options") },
            onCamera = { onNavigate("b3_camera") },
            onGallery = { onNavigate("b3_chooser") },
            onBack = onBack,
            onTab = tab
        )
        "options" -> DiffChooseStyle(
            onContinue = { onNavigate("b_diff_adjust") },
            onBack = { onNavigate("b_diff_choose") },
            onTab = tab
        )
        "adjust" -> DiffCustomize(
            onGenerate = { onNavigate("b_diff_processing") },
            onBack = { onNavigate("b_diff_options") }
        )
        "processing" -> DiffProcessing(
            onDone = { onNavigate("b_diff_result") },
            onOffline = { onNavigate("b_diff_offline") },
            onLowCredits = { onNavigate("b_diff_low_credits") },
            onBack = { onNavigate("b_diff_adjust") }
        )
        "result" -> DiffResult(
            onSaved = { onNavigate("b_diff_share") },
            onShare = { onNavigate("b8_share") },
            onAnotherStyle = { onNavigate("b_diff_options") },
            onVariations = { onNavigate("b_diff_variations") },
            onBack = { onNavigate("b_diff_adjust") }
        )
        "variations" -> DiffVariations(
            onPick = { onNavigate("b_diff_result") },
            onMore = { onNavigate("b_diff_processing") },
            onBack = { onNavigate("b_diff_result") }
        )
        "share" -> DiffSaved(
            onShare = { onNavigate("b8_share") },
            onVideo = { onNavigate("b14_input") },
            onMove = { onNavigate("b14_input") },
            onEdit = { onNavigate("b_diff_adjust") },
            onAnother = pipeline.openDifferent,
            onHome = onBack,
            onBack = { onNavigate("b_diff_result") }
        )
        "offline" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Offline,
            onPrimary = { onNavigate("b_diff_processing") },
            onSecondary = { onNavigate("b_diff_adjust") }
        )
        "low_credits" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.LowCredits,
            onPrimary = { onNavigate("b23_dash") },
            onSecondary = { onNavigate("b_diff_adjust") }
        )
        "safety" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Safety,
            onPrimary = { onNavigate("b_diff_choose") },
            onSecondary = onBack
        )
        else -> DiffSelectPhoto(
            onPick = { onNavigate("b_diff_options") },
            onCamera = { onNavigate("b3_camera") },
            onGallery = { onNavigate("b3_chooser") },
            onBack = onBack,
            onTab = tab
        )
    }
}

private val diffStyles = listOf(
    DiffStyle(R.string.board_diff_style_anime, R.string.board_diff_badge_popular, BoardFlowArt.differentHero, 1),
    DiffStyle(R.string.board_diff_style_3d, R.string.board_diff_badge_trendy, R.drawable.board_c14_remix, 3),
    DiffStyle(R.string.board_diff_style_oil, R.string.board_diff_badge_classic, BoardFlowArt.styleOil, 2),
    DiffStyle(R.string.board_diff_style_watercolor, R.string.board_diff_badge_artistic, BoardFlowArt.styleWatercolor, 2),
    DiffStyle(R.string.board_diff_style_sketch, R.string.board_diff_badge_minimal, BoardFlowArt.styleSketch, 2),
    DiffStyle(R.string.board_diff_style_pixel, R.string.board_diff_badge_retro, BoardFlowArt.stylePixel, 3),
    DiffStyle(R.string.board_diff_style_fantasy, R.string.board_diff_badge_magical, BoardFlowArt.styleFantasy, 1),
    DiffStyle(R.string.board_diff_style_cyber, R.string.board_diff_badge_future, R.drawable.board_c07_space, 1),
    DiffStyle(R.string.board_diff_style_pop, R.string.board_diff_badge_colorful, R.drawable.board_c07_festival, 3),
    DiffStyle(R.string.board_diff_style_vintage, R.string.board_diff_badge_timeless, R.drawable.board_c07_cafe, 2),
    DiffStyle(R.string.board_diff_style_line, R.string.board_diff_badge_elegant, R.drawable.board_c08_people, 2),
    DiffStyle(R.string.board_diff_style_neon, R.string.board_diff_badge_trendy, R.drawable.board_c11_talk, 1)
)

@Composable
private fun DiffSelectPhoto(
    onPick: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onBack: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowDifferentSession.photoIndex) }
    val recent = listOf(
        BoardFlowArt.beautifulHero,
        BoardFlowArt.differentHero,
        R.drawable.board_c00_hero,
        BoardFlowArt.baAfter,
        R.drawable.board_c09_portrait,
        R.drawable.board_c08_people
    )
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_DIFF_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.make_it_different),
                        fontWeight = FontWeight.Bold,
                        style = BoardType.title,
                        color = colors.textPrimary
                    )
                    Text(
                        stringResource(R.string.board_diff_slogan),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
            com.pixijoy.ai.ui.board.BoardUseSampleButton(
                sampleRes = BoardFlowArt.differentHero,
                onReady = onPick
            )
            BoardPrimaryButton(
                text = stringResource(R.string.photo_take),
                onClick = onCamera
            )
            BoardOutlinedButton(
                text = stringResource(R.string.photo_gallery),
                onClick = onGallery
            )
            Text(
                stringResource(R.string.board_c09_recent),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            recent.chunked(3).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEachIndexed { col, art ->
                        val index = rowIndex * 3 + col
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
                                    BoardFlowDifferentSession.photoIndex = index
                                    onPick()
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            BoardCreditChip(credits = 2)
            BoardPrimaryButton(text = stringResource(R.string.board_c00_use_this_photo), onClick = onPick)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun DiffChooseStyle(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var tab by remember { mutableIntStateOf(BoardFlowDifferentSession.styleTab) }
    var selected by remember { mutableIntStateOf(BoardFlowDifferentSession.styleIndex) }
    val tabs = listOf(
        R.string.board_f4_ideas_all,
        R.string.board_f6_tab_trending,
        R.string.board_diff_tab_art,
        R.string.board_diff_tab_cartoon
    )
    val filtered = diffStyles.mapIndexed { i, s -> i to s }.filter { (_, s) ->
        tab == 0 || s.category == tab
    }
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_DIFF_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.board_diff_choose_style),
                        fontWeight = FontWeight.Bold,
                        style = BoardType.title,
                        color = colors.textPrimary
                    )
                    val era = BoardFlowDifferentSession.eraIndex
                    if (era >= 0) {
                        val eraRes = when (era) {
                            0 -> R.string.board_ai_era_80s
                            1 -> R.string.board_ai_era_90s
                            2 -> R.string.board_ai_era_2000s
                            3 -> R.string.board_ai_era_film
                            else -> R.string.board_ai_era_now
                        }
                        Text(
                            stringResource(R.string.board_ai_eras_title) + " · " + stringResource(eraRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                tabs.forEachIndexed { i, res ->
                    val on = tab == i
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (on) colors.primary else colors.surfaceMuted)
                            .clickable {
                                tab = i
                                BoardFlowDifferentSession.styleTab = i
                            }
                            .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(res),
                            color = if (on) colors.onPrimary else colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
            filtered.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEach { (index, style) ->
                        val on = selected == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selected = index
                                    BoardFlowDifferentSession.styleIndex = index
                                }
                        ) {
                            Box {
                                Image(
                                    painter = painterResource(style.art),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .then(if (on) Modifier.border(3.dp, colors.primary, CircleShape) else Modifier),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Text(
                                stringResource(style.title),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Text(
                                stringResource(style.badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            }
            BoardCreditChip(credits = 2)
            BoardPrimaryButton(
                text = stringResource(R.string.board_c00_continue),
                onClick = onContinue,
                modifier = Modifier.testTag(TestTags.TOOL_CONTINUE)
            )
        }
    }
}

@Composable
private fun DiffCustomize(onGenerate: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var strength by remember { mutableFloatStateOf(BoardFlowDifferentSession.strength) }
    var prompt by remember { mutableStateOf(BoardFlowDifferentSession.prompt) }
    var aspect by remember { mutableIntStateOf(BoardFlowDifferentSession.aspectIndex) }
    val style = diffStyles.getOrElse(BoardFlowDifferentSession.styleIndex) { diffStyles.first() }
    val aspects = listOf(
        R.string.board_diff_aspect_original,
        R.string.board_c10_aspect_11,
        R.string.board_diff_aspect_34,
        R.string.board_diff_aspect_916,
        R.string.board_c10_aspect_169
    )
    val previewRatio = when (aspect) {
        1 -> 1f
        2 -> 3f / 4f
        3 -> 9f / 16f
        4 -> 16f / 9f
        else -> 1f
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_DIFF_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_diff_customize),
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
            Image(
                painter = painterResource(style.art),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(previewRatio)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.board_diff_strength, (strength * 100).toInt()),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Slider(
                value = strength,
                onValueChange = {
                    strength = it
                    BoardFlowDifferentSession.strength = it
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = prompt,
                onValueChange = {
                    prompt = it
                    BoardFlowDifferentSession.prompt = it
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(radii.md),
                label = { Text(stringResource(R.string.board_diff_prompt_hint)) }
            )
            Text(
                stringResource(R.string.board_diff_aspect),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                aspects.forEachIndexed { i, res ->
                    val on = aspect == i
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (on) colors.primary else colors.surfaceMuted)
                            .clickable {
                                aspect = i
                                BoardFlowDifferentSession.aspectIndex = i
                            }
                            .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(res),
                            color = if (on) colors.onPrimary else colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            }
            BoardCreditChip(credits = 2)
            BoardPrimaryButton(
                text = stringResource(R.string.board_f19_generate),
                onClick = onGenerate,
                aiAccent = true,
                modifier = Modifier.testTag(TestTags.AI_GENERATE)
            )
        }
    }
}

@Composable
private fun DiffProcessing(
    onDone: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    var step by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    val checks = listOf(
        R.string.board_c09_check_understand,
        R.string.board_diff_check_style,
        R.string.board_c09_check_details,
        R.string.board_c09_check_life
    )
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val era = BoardFlowDifferentSession.eraIndex
        val styleLook = (BoardFlowDifferentSession.styleIndex % 7) + 1
        val enhanced = BoardCToolEngine.run(context) { pipe, src ->
            val looked = if (era >= 0) pipe.applyEraLook(src, era) else pipe.applyLook(src, styleLook)
            val sat = BoardColorAdjust.Neutral +
                (BoardFlowDifferentSession.strength - 0.5f) * 12f
            val graded = pipe.applyColorMatrix(
                looked,
                BoardColorAdjust.androidMatrix(saturation = sat.coerceIn(0f, 20f))
            )
            val avatar = when (BoardFlowDifferentSession.styleIndex) {
                0 -> com.pixijoy.ai.domain.model.AvatarStyle.ANIME
                4 -> com.pixijoy.ai.domain.model.AvatarStyle.SKETCH
                8 -> com.pixijoy.ai.domain.model.AvatarStyle.CARTOON
                else -> com.pixijoy.ai.domain.model.AvatarStyle.GLOW
            }
            runCatching {
                dagger.hilt.android.EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    com.pixijoy.ai.di.BoardAiEntryPoint::class.java
                ).avatarStyleProcessor().apply(graded, avatar)
            }.getOrDefault(graded)
        }
        if (enhanced != null) BoardFlowDifferentSession.setResult(enhanced)
        for (i in checks.indices) {
            delay(280)
            step = i + 1
            progress = (i + 1f) / checks.size
        }
        delay(200)
        onDone()
    }
    BoardAdaptiveFrame {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.processingSurface)
                .testTag(TestTags.FLOW_DIFF_HOST)
                .padding(PixiSpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BoardSoftPulse {
                    Icon(painter = painterResource(R.drawable.pj_ic_sparkles), null, tint = colors.primary, modifier = Modifier.size(64.dp))
                }
                Spacer(modifier = Modifier.height(PixiSpacing.md))
                Text(
                    stringResource(R.string.board_c04_ai_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title
                )
                Text(
                    stringResource(R.string.board_diff_creating),
                    color = Color.White.copy(0.85f),
                    textAlign = TextAlign.Center
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
                BoardSoftProgress(progress = (progress ), darkSurface = true)
                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                BoardEdgeActions(
                    onOffline = onOffline,
                    onLowCredits = onLowCredits,
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
private fun DiffResult(
    onSaved: () -> Unit,
    onShare: () -> Unit,
    onAnotherStyle: () -> Unit,
    onVariations: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val style = diffStyles.getOrElse(BoardFlowDifferentSession.styleIndex) { diffStyles.first() }
    val variationArts = listOf(
        style.art,
        BoardFlowArt.differentHero,
        R.drawable.board_c14_remix,
        BoardFlowArt.baAfter,
        R.drawable.board_c09_portrait,
        BoardFlowArt.funnyHero
    )
    val art = variationArts.getOrElse(BoardFlowDifferentSession.variationIndex % variationArts.size) {
        style.art
    }
    val strengthSat = BoardColorAdjust.Neutral +
        (BoardFlowDifferentSession.strength - 0.5f) * 12f
    val satClamped = strengthSat.coerceIn(0f, 20f)
    val grade = BoardColorAdjust.colorFilter(saturation = satClamped)
    val bake = BoardColorAdjust.androidMatrix(saturation = satClamped)
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_DIFF_RESULT),
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
                    stringResource(R.string.board_diff_result_title),
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            Text(
                stringResource(
                    R.string.board_diff_strength,
                    (BoardFlowDifferentSession.strength * 100).toInt()
                ),
                style = BoardType.caption,
                color = colors.textSecondary
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
            ) {
                val live = BoardFlowDifferentSession.resultBitmap
                if (live != null && !live.isRecycled) {
                    BoardSafeResultImage(
                        bitmap = live,
                        fallbackRes = art,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f),
                        colorFilter = grade
                    )
                } else {
                    BoardSessionPhoto(
                        fallbackRes = art,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f),
                        colorFilter = grade
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(PixiSpacing.md)
                        .clip(RoundedCornerShape(radii.pill))
                        .background(Color.Black.copy(0.5f))
                        .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                ) {
                    Text(stringResource(style.title), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f2_save),
                onClick = {
                    val ok = BoardDrawableIo.saveToGallery(
                        context,
                        art,
                        filePrefix = "PixiJoy_Different",
                        colorMatrix = bake
                    )
                    Toast.makeText(
                        context,
                        if (ok) context.getString(R.string.board_diff_saved)
                        else context.getString(R.string.board_soft_preview_title),
                        Toast.LENGTH_SHORT
                    ).show()
                    if (ok) onSaved()
                }
            )
            BoardOutlinedButton(text = stringResource(R.string.board_f2_share), onClick = onShare)
            BoardOutlinedButton(
                text = stringResource(R.string.board_f7_another_style),
                onClick = onAnotherStyle
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_diff_more_like),
                onClick = onVariations
            )
        }
    }
}

@Composable
private fun DiffVariations(onPick: () -> Unit, onMore: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowDifferentSession.variationIndex) }
    val arts = listOf(
        BoardFlowArt.differentHero,
        R.drawable.board_c14_remix,
        BoardFlowArt.baAfter,
        R.drawable.board_c09_portrait,
        BoardFlowArt.funnyHero,
        R.drawable.board_c12_card
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_DIFF_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_diff_more_like),
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title,
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
                                .aspectRatio(3f / 4f)
                                .clip(RoundedCornerShape(radii.md))
                                .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md)) else Modifier)
                                .clickable {
                                    selected = index
                                    BoardFlowDifferentSession.variationIndex = index
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_c09_generate_more), onClick = onMore)
            BoardOutlinedButton(text = stringResource(R.string.board_c04_use_this), onClick = onPick)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun DiffSaved(
    onShare: () -> Unit,
    onVideo: () -> Unit,
    onMove: () -> Unit,
    onEdit: () -> Unit,
    onAnother: () -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val actions = listOf(
        R.string.board_f2_share to onShare,
        R.string.board_diff_create_video to onVideo,
        R.string.make_it_move to onMove,
        R.string.board_f7_edit to onEdit,
        R.string.board_diff_create_another to onAnother
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_DIFF_HOST),
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
                Spacer(modifier = Modifier.weight(1f))
            }
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(colors.success.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(48.dp))
            }
            Text(
                stringResource(R.string.board_diff_saved),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            actions.forEach { (label, action) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(colors.surfaceMuted)
                        .clickable(onClick = action)
                        .padding(PixiSpacing.lg)
                ) {
                    Text(
                        stringResource(label),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_diff_back_home), onClick = onHome)
            BoardTrustFooter()
        }
    }
}
