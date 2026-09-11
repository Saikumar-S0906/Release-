package com.pixijoy.ai.ui.board.ctools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardBeforeAfter
import com.pixijoy.ai.ui.board.components.BoardGateScreen
import com.pixijoy.ai.ui.board.components.BoardHeroImage
import com.pixijoy.ai.ui.board.components.BoardPreviewControls
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardSecondaryButton
import com.pixijoy.ai.ui.board.components.BoardTipCallout
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.components.BoardVariationGrid
import com.pixijoy.ai.ui.board.components.BoardWideFrame
import com.pixijoy.ai.ui.board.components.boardGridColumns
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.theme.PixiSpacing
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import com.pixijoy.ai.ui.board.BoardSafeResultImage
import com.pixijoy.ai.ui.board.components.BoardEdgeActions
import com.pixijoy.ai.ui.board.components.BoardResultFooter
import com.pixijoy.ai.ui.board.components.BoardScreenHeader
import com.pixijoy.ai.ui.board.components.BoardSoftProgress
import com.pixijoy.ai.ui.board.components.BoardSoftPulse
import kotlinx.coroutines.launch

/** Wave 3 — C04–C14 board tool wizards (parallel path). */
@Composable
fun BoardCToolsHost(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tool = BoardCTool.fromScreenId(screenId)
    if (tool == null) {
        BoardGateScreen(
            title = stringResource(R.string.board_placeholder_title),
            body = screenId,
            primary = stringResource(R.string.cd_back),
            onPrimary = onBack
        )
        return
    }
    val step = BoardCTool.stepOf(screenId)
    val id = { s: String -> "${tool.prefix}_$s" }

    when (step) {
        "intro", "hub" -> Intro(tool, onContinue = {
            onNavigate(id("photo"))
        }, onBack = onBack, onEdge = { onNavigate(id(it)) })
        "photo" -> PhotoPick(
            tool = tool,
            onSample = {
                onNavigate(if (tool.needsConsent) id("consent") else id("options"))
            },
            onGallery = { onNavigate("b3_chooser") },
            onBack = { onNavigate(tool.prefix) }
        )
        "consent" -> Consent(
            tool = tool,
            onAllow = { onNavigate(id("options")) },
            onDeny = { onNavigate(id("consent_denied")) }
        )
        "consent_denied" -> BoardGateScreen(
            title = stringResource(R.string.board_ct_consent_denied_title),
            body = stringResource(R.string.board_ct_consent_denied_body),
            primary = stringResource(R.string.board_ct_review_consent),
            onPrimary = { onNavigate(id("consent")) },
            secondary = stringResource(R.string.cd_back),
            onSecondary = onBack
        )
        "options" -> Options(
            tool = tool,
            onPick = { onNavigate(id("adjust")) },
            onBack = { onNavigate(id("photo")) }
        )
        "adjust" -> Adjust(
            tool = tool,
            onGenerate = { onNavigate("b19_start") },
            onOffline = { onNavigate(id("offline")) },
            onLowCredits = { onNavigate(id("low_credits")) },
            onBack = { onNavigate(id("options")) }
        )
        "result" -> Result(
            tool = tool,
            onVariations = { onNavigate(id("variations")) },
            onShare = { onNavigate(id("share")) },
            onRefine = { onNavigate(id("refine")) },
            onDone = onBack
        )
        "variations" -> Variations(tool = tool, onPick = { onNavigate(id("result")) })
        "share" -> Share(
            tool = tool,
            onDone = onBack,
            onRemix = { onNavigate("b_c14_intro") }
        )
        "offline" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_offline_title),
            body = stringResource(R.string.board_ct_offline_body),
            primary = stringResource(R.string.board_f3_try_again),
            onPrimary = { onNavigate(id("adjust")) },
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate(id("options")) }
        )
        "low_credits" -> BoardGateScreen(
            title = stringResource(R.string.board_f19_low_credits_title),
            body = stringResource(R.string.board_f19_low_credits_body),
            primary = stringResource(R.string.board_f19_get_credits),
            onPrimary = onBack,
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate(id("adjust")) }
        )
        "safety" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_safety_title),
            body = stringResource(R.string.board_f4_safety_body),
            primary = stringResource(R.string.board_f3_try_another),
            onPrimary = { onNavigate(id("photo")) },
            secondary = stringResource(R.string.cd_back),
            onSecondary = onBack
        )
        "refine" -> Refine(
            tool = tool,
            onGenerate = { onNavigate("b19_start") },
            onBack = { onNavigate(id("result")) }
        )
        else -> Intro(tool, onContinue = { onNavigate(id("photo")) }, onBack = onBack, onEdge = { onNavigate(id(it)) })
    }
}

@Composable
private fun Intro(
    tool: BoardCTool,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onEdge: (String) -> Unit
) {
    val colors = BoardTheme.colors
    val art = tool.art()
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(tool.titleRes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(stringResource(R.string.board_ct_intro_body), color = colors.textSecondary)
            BoardHeroImage(
                resId = art.hero,
                aspectRatio = 16f / 9f
            )
            BoardTipCallout(
                title = stringResource(R.string.board_tip_title),
                body = stringResource(tool.tipRes)
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_ct_get_started),
                onClick = onContinue
            )
            BoardSecondaryButton(
                text = stringResource(R.string.cd_back),
                onClick = onBack
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun PhotoPick(
    tool: BoardCTool,
    onSample: () -> Unit,
    onGallery: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val art = tool.art()
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.board_ct_photo_title, stringResource(tool.titleRes)),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            BoardHeroImage(
                resId = art.hero
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_c00_use_my_photo),
                onClick = onGallery
            )
            BoardSecondaryButton(
                text = stringResource(R.string.cd_back),
                onClick = onBack
            )
        }
    }
}

@Composable
private fun Consent(tool: BoardCTool, onAllow: () -> Unit, onDeny: () -> Unit) {
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
                stringResource(R.string.board_ct_consent_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            Text(stringResource(R.string.board_ct_consent_body), color = colors.textSecondary)
            BoardTipCallout(
                title = stringResource(R.string.board_ct_allowed_title),
                body = stringResource(R.string.board_ct_allowed_body)
            )
            BoardTipCallout(
                title = stringResource(R.string.board_ct_not_allowed_title),
                body = stringResource(R.string.board_ct_not_allowed_body)
            )
            Text(
                stringResource(R.string.board_ct_keep_kind, stringResource(tool.titleRes)),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_ct_consent_agree),
                onClick = onAllow
            )
            BoardSecondaryButton(
                text = stringResource(R.string.board_ct_consent_disagree),
                onClick = onDeny
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun Options(tool: BoardCTool, onPick: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val columns = boardGridColumns()
    val art = tool.art()
    BoardWideFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
        ) {
            Text(
                stringResource(R.string.board_ct_options_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            BoardHeroImage(resId = art.hero, aspectRatio = 16f / 9f)
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                items(tool.optionRes.size) { i ->
                    val accent = listOf(
                        colors.primary,
                        colors.accentPink,
                        colors.accentPurple,
                        colors.accentYellow
                    )[i % 4]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .clip(RoundedCornerShape(radii.card))
                            .background(accent.copy(0.18f))
                            .clickable(onClick = onPick)
                            .padding(PixiSpacing.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(tool.optionRes[i]),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = colors.textPrimary
                        )
                    }
                }
            }
            BoardSecondaryButton(
                text = stringResource(R.string.cd_back),
                onClick = onBack
            )
        }
    }
}

@Composable
private fun Adjust(
    tool: BoardCTool,
    onGenerate: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val art = tool.art()
    BoardWideFrame {
        BoardPreviewControls(
            preview = {
                BoardHeroImage(resId = art.after ?: art.result)
            },
            controls = {
                Text(
                    stringResource(R.string.board_ct_adjust_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(PixiSpacing.md))
                if (tool.isMotion) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.board_ct_script_hint)) },
                        minLines = 2,
                        shape = RoundedCornerShape(radii.md)
                    )
                    Text(stringResource(R.string.board_ct_motion_hint), color = colors.textSecondary)
                }
                Text(
                    stringResource(R.string.board_ct_strength),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                    listOf("Low", "Med", "High").forEachIndexed { i, label ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(radii.pill))
                                .background(
                                    if (i == 1) colors.primary else colors.surfaceMuted
                                )
                                .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.sm)
                        ) {
                            Text(
                                label,
                                color = if (i == 1) colors.onPrimary else colors.textSecondary
                            )
                        }
                    }
                }
                if (tool == BoardCTool.Expand) {
                    Text(
                        stringResource(R.string.board_ct_pro_hd),
                        color = colors.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (tool == BoardCTool.Remix) {
                    BoardTipCallout(
                        title = stringResource(R.string.board_ct_attribution_title),
                        body = stringResource(R.string.board_ct_attribution_body)
                    )
                }
                BoardTipCallout(
                    title = stringResource(R.string.board_tip_title),
                    body = stringResource(tool.tipRes)
                )
                BoardPrimaryButton(
                    text = stringResource(R.string.board_f19_generate),
                    onClick = onGenerate
                )
                BoardSecondaryButton(
                    text = stringResource(R.string.cd_back),
                    onClick = onBack
                )
                BoardTrustFooter()
            }
        )
    }
}

@Composable
private fun Refine(
    tool: BoardCTool,
    onGenerate: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val art = tool.art()
    BoardWideFrame {
        BoardPreviewControls(
            preview = {
                BoardHeroImage(resId = art.after ?: art.result)
            },
            controls = {
                Text(
                    stringResource(R.string.board_ct_refine_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(PixiSpacing.sm))
                Text(stringResource(R.string.board_ct_refine_body), color = colors.textSecondary)
                Text(
                    stringResource(R.string.board_ct_strength),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = PixiSpacing.md)
                )
                Text("Edge · Soft · Feather", color = colors.textSecondary)
                BoardTipCallout(
                    title = stringResource(R.string.board_tip_title),
                    body = stringResource(tool.tipRes)
                )
                BoardPrimaryButton(
                    text = stringResource(R.string.board_f19_generate),
                    onClick = onGenerate
                )
                BoardSecondaryButton(
                    text = stringResource(R.string.cd_back),
                    onClick = onBack
                )
            }
        )
    }
}

@Composable
private fun Result(
    tool: BoardCTool,
    onVariations: () -> Unit,
    onShare: () -> Unit,
    onRefine: () -> Unit,
    onDone: () -> Unit
) {
    val colors = BoardTheme.colors
    val art = tool.art()
    val variationImages = buildList {
        add(art.hero)
        art.after?.let { add(it) }
        add(art.result)
        addAll(art.variationExtras)
    }.distinct().take(4)
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.board_ct_result_title, stringResource(tool.titleRes)),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            if (tool.showBeforeAfter && art.before != null && art.after != null) {
                BoardBeforeAfter(
                    beforeRes = art.before,
                    afterRes = art.after
                )
            } else {
                BoardHeroImage(resId = art.result)
            }
            BoardVariationGrid(
                images = variationImages,
                onPick = { onVariations() }
            )
            BoardPrimaryButton(text = stringResource(R.string.board_f2_save), onClick = onDone)
            TextButton(onClick = onVariations) {
                Text(stringResource(R.string.board_c5_variations), color = colors.primary)
            }
            TextButton(onClick = onShare) {
                Text(stringResource(R.string.board_f2_share), color = colors.primary)
            }
            if (tool == BoardCTool.RemoveBg) {
                TextButton(onClick = onRefine) {
                    Text(stringResource(R.string.board_ct_refine_title), color = colors.primary)
                }
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun Variations(tool: BoardCTool, onPick: () -> Unit) {
    val colors = BoardTheme.colors
    val art = tool.art()
    val images = buildList {
        add(art.hero)
        art.after?.let { add(it) }
        add(art.result)
        addAll(art.variationExtras)
    }.distinct().take(4)
    BoardWideFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
        ) {
            Text(
                stringResource(R.string.board_c5_variations),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            BoardVariationGrid(
                images = images,
                onPick = { onPick() }
            )
            BoardPrimaryButton(text = stringResource(R.string.board_f2_use_this), onClick = onPick)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun Share(tool: BoardCTool, onDone: () -> Unit, onRemix: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.board_ct_share_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            listOf(
                R.string.gallery,
                R.string.format_whatsapp,
                R.string.format_ig_story,
                R.string.nav_community,
                R.string.nav_stickers
            ).forEach { res ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .clickable(onClick = onDone)
                        .padding(PixiSpacing.lg)
                ) {
                    Text(
                        stringResource(res),
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                }
            }
            if (tool.isGreeting) {
                BoardTipCallout(
                    title = stringResource(R.string.board_tip_title),
                    body = stringResource(R.string.board_ct_tip_greeting_share)
                )
            }
            if (tool != BoardCTool.Remix) {
                TextButton(onClick = onRemix) {
                    Text(stringResource(R.string.board_f2_remix), color = colors.primary)
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_ct_done), onClick = onDone)
            BoardTrustFooter()
        }
    }
}
