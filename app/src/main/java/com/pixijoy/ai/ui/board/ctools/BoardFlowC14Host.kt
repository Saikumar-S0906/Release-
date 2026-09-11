package com.pixijoy.ai.ui.board.ctools

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.pixijoy.ai.ui.board.components.BoardTipCallout
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

/** C14 session — soft remix prefs (local demo only). */
internal object BoardFlowC14Session {
    var postIndex: Int = 0
    var photoIndex: Int = 0
    var keepStyle: Boolean = true
    var keepPose: Boolean = true
    var keepFace: Boolean = true
    var boostColor: Boolean = false
    var softRelight: Boolean = true
    var visibilityIndex: Int = 0
    var variationIndex: Int = 0

    fun remixOptionIndex(): Int = variationIndex

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
 * C14 — Remix from Community.
 * Soft: simulated remix / attribution UI; no real community publish or generative remix.
 */
@Composable
fun BoardFlowC14Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    val step = BoardCTool.stepOf(screenId)
    when (step) {
        "intro", "hub", "" -> C14CommunityPost(
            onRemix = { onNavigate("b_c14_options") },
            onBack = onBack,
            onTab = tab
        )
        "options" -> C14Attribution(
            onContinue = { onNavigate("b_c14_photo") },
            onBack = { onNavigate("b_c14_intro") }
        )
        "photo" -> C14AddPhoto(
            onPick = { onNavigate("b_c14_adjust") },
            onGallery = { onNavigate("b3_chooser") },
            onBack = { onNavigate("b_c14_options") }
        )
        "adjust" -> C14KeepStyle(
            onGenerate = { onNavigate("b_c14_processing") },
            onBack = { onNavigate("b_c14_photo") }
        )
        "processing" -> C14Processing(
            onDone = { onNavigate("b_c14_result") },
            onOffline = { onNavigate("b_c14_offline") },
            onLowCredits = { onNavigate("b_c14_low_credits") },
            onBack = { onNavigate("b_c14_adjust") }
        )
        "result" -> C14Result(
            onPublish = { onNavigate("b_c14_share") },
            onVariations = { onNavigate("b_c14_variations") },
            onBack = { onNavigate("b_c14_adjust") }
        )
        "variations" -> C14Variations(
            onPick = { onNavigate("b_c14_share") },
            onBack = { onNavigate("b_c14_result") }
        )
        "share" -> C14Publish(
            onPublish = { onNavigate("b27_feed") },
            onSave = { onNavigate("b8_ready") },
            onDone = onBack,
            onBack = { onNavigate("b_c14_result") }
        )
        "offline" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_offline_title),
            body = stringResource(R.string.board_ct_offline_body),
            primary = stringResource(R.string.board_f3_try_again),
            onPrimary = { onNavigate("b_c14_processing") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate("b_c14_adjust") }
        )
        "low_credits" -> BoardGateScreen(
            title = stringResource(R.string.board_f19_low_credits_title),
            body = stringResource(R.string.board_f19_low_credits_body),
            primary = stringResource(R.string.board_f19_get_credits),
            onPrimary = { onNavigate("b23_dash") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate("b_c14_adjust") }
        )
        "safety" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_safety_title),
            body = stringResource(R.string.board_f4_safety_body),
            primary = stringResource(R.string.board_f3_try_another),
            onPrimary = { onNavigate("b_c14_intro") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = onBack
        )
        else -> C14CommunityPost(
            onRemix = { onNavigate("b_c14_options") },
            onBack = onBack,
            onTab = tab
        )
    }
}

@Composable
private fun C14CommunityPost(
    onRemix: () -> Unit,
    onBack: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var liked by remember { mutableStateOf(false) }
    BoardMainShell(selected = BoardShellTab.Discover, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C14_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.create_extra_remix),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        stringResource(R.string.board_c14_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.accentPink
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("PD", fontWeight = FontWeight.Bold, color = colors.primary)
                }
                Column {
                    Text(
                        stringResource(R.string.board_f27_handle_doggo),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        stringResource(R.string.create_extra_c14_original),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
            ) {
                Image(
                    painter = painterResource(R.drawable.board_c14_remix),
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
                        stringResource(R.string.board_c14_caption),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { liked = !liked }) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_heart),
                        contentDescription = null,
                        tint = if (liked) colors.accentPink else colors.textSecondary
                    )
                }
                BoardPrimaryButton(
                    text = stringResource(R.string.board_f2_remix),
                    onClick = onRemix,
                    modifier = Modifier.weight(1f)
                )
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C14Attribution(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val rules = listOf(
        R.string.board_c14_rule_credit,
        R.string.board_c14_rule_commercial,
        R.string.board_c14_rule_share
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C14_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.create_extra_c14_attr_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(R.drawable.board_c14_remix),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.board_c14_inspired_by),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            Text(
                stringResource(R.string.create_extra_c14_credit),
                color = colors.textSecondary
            )
            BoardTipCallout(
                title = stringResource(R.string.board_ct_attribution_title),
                body = stringResource(R.string.board_ct_attribution_body)
            )
            rules.forEach { res ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(20.dp))
                    Text(stringResource(res), color = colors.textPrimary)
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.create_extra_c14_continue), onClick = onContinue)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C14AddPhoto(onPick: () -> Unit, onGallery: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC14Session.photoIndex) }
    val photos = listOf(
        R.drawable.board_c00_hero,
        R.drawable.board_c09_portrait,
        R.drawable.board_b5_photo,
        R.drawable.board_c08_people,
        R.drawable.board_c11_talk,
        R.drawable.board_ba_after
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C14_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c14_add_photo),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(photos[selected.coerceIn(photos.indices)]),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            photos.chunked(3).forEachIndexed { rowIndex, row ->
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
                                    BoardFlowC14Session.photoIndex = index
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            com.pixijoy.ai.ui.board.BoardUseSampleButton(
                sampleRes = R.drawable.board_c14_remix,
                onReady = onPick
            )
            BoardPrimaryButton(text = stringResource(R.string.board_c00_use_this_photo), onClick = onPick)
            BoardOutlinedButton(text = stringResource(R.string.board_c00_use_my_photo), onClick = onGallery)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C14KeepStyle(onGenerate: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var keepStyle by remember { mutableStateOf(BoardFlowC14Session.keepStyle) }
    var keepPose by remember { mutableStateOf(BoardFlowC14Session.keepPose) }
    var keepFace by remember { mutableStateOf(BoardFlowC14Session.keepFace) }
    var boostColor by remember { mutableStateOf(BoardFlowC14Session.boostColor) }
    var softRelight by remember { mutableStateOf(BoardFlowC14Session.softRelight) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C14_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.create_extra_c14_keep_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                Image(
                    painter = painterResource(R.drawable.board_c14_remix),
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(radii.md)),
                    contentScale = ContentScale.Crop
                )
                Image(
                    painter = painterResource(R.drawable.board_c00_hero),
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(radii.md)),
                    contentScale = ContentScale.Crop
                )
            }
            ToggleRow(
                label = stringResource(R.string.create_extra_c14_keep_style),
                checked = keepStyle,
                onChecked = {
                    keepStyle = it
                    BoardFlowC14Session.keepStyle = it
                }
            )
            ToggleRow(
                label = stringResource(R.string.board_c14_keep_pose),
                checked = keepPose,
                onChecked = {
                    keepPose = it
                    BoardFlowC14Session.keepPose = it
                }
            )
            ToggleRow(
                label = stringResource(R.string.board_ct_opt_keep_face),
                checked = keepFace,
                onChecked = {
                    keepFace = it
                    BoardFlowC14Session.keepFace = it
                }
            )
            ToggleRow(
                label = stringResource(R.string.board_ct_opt_boost_color),
                checked = boostColor,
                onChecked = {
                    boostColor = it
                    BoardFlowC14Session.boostColor = it
                }
            )
            ToggleRow(
                label = stringResource(R.string.board_ct_opt_soft_relight),
                checked = softRelight,
                onChecked = {
                    softRelight = it
                    BoardFlowC14Session.softRelight = it
                }
            )
            BoardPrimaryButton(text = stringResource(R.string.board_f2_remix), onClick = onGenerate)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radii.md))
            .background(colors.surfaceMuted)
            .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun C14Processing(
    onDone: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        BoardFlowC14Session.setResult(null)
        BoardFlowC14Session.usedSoftFallback = false
        progress = 0.25f
        val optionIndex = BoardFlowC14Session.remixOptionIndex()
        val keepFace = BoardFlowC14Session.keepFace
        val result = BoardCToolEngine.run(context) { pipe, src ->
            pipe.remixStyle(src, optionIndex, keepFace)
        }
        progress = 0.75f
        delay(100)
        if (result != null) {
            BoardFlowC14Session.setResult(result)
            BoardFlowC14Session.usedSoftFallback = false
        } else {
            BoardFlowC14Session.usedSoftFallback = true
        }
        progress = 1f
        delay(200)
        onDone()
    }
    BoardAdaptiveFrame {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.processingSurface)
                .statusBarsPadding()
                .testTag(TestTags.FLOW_C14_HOST)
                .padding(PixiSpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BoardProcessingHero(progress = progress, darkSurface = true)
                Spacer(modifier = Modifier.height(PixiSpacing.md))
                Text(
                    stringResource(R.string.create_extra_remix),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    stringResource(R.string.board_c14_remixing),
                    color = Color.White.copy(0.85f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(PixiSpacing.lg))
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
private fun C14Result(
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
                .testTag(TestTags.FLOW_C14_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.size(PixiSpacing.sm))
                Text(
                    stringResource(R.string.create_extra_c14_remix_result),
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
                    bitmap = BoardFlowC14Session.resultBitmap,
                    fallbackRes = R.drawable.board_c14_remix,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(PixiSpacing.md)
                        .clip(RoundedCornerShape(radii.pill))
                        .background(Color.Black.copy(0.55f))
                        .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                ) {
                    Text(
                        stringResource(R.string.board_c14_inspired_by),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            if (BoardFlowC14Session.usedSoftFallback) {
                Text(
                    stringResource(R.string.board_c04_soft_fallback),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            BoardResultFooter(
                onSave = {
                    val out = BoardFlowC14Session.resultBitmap
                    val ok = if (out != null && !out.isRecycled) {
                        BoardDrawableIo.saveBitmapToGallery(context, out, filePrefix = "PixiJoy_Remix")
                    } else {
                        BoardDrawableIo.saveToGallery(
                            context,
                            R.drawable.board_c14_remix,
                            filePrefix = "PixiJoy_Remix"
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
            BoardOutlinedButton(text = stringResource(R.string.create_extra_remix_again), onClick = onVariations)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C14Result(onPublish: () -> Unit, onVariations: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C14_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.size(PixiSpacing.sm))
                Text(
                    stringResource(R.string.create_extra_c14_remix_result),
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
                    painter = painterResource(R.drawable.board_c14_remix),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(PixiSpacing.md)
                        .clip(RoundedCornerShape(radii.pill))
                        .background(Color.Black.copy(0.55f))
                        .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                ) {
                    Text(
                        stringResource(R.string.board_c14_inspired_by),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.create_extra_c14_publish_title), onClick = onPublish)
            BoardOutlinedButton(text = stringResource(R.string.create_extra_remix_again), onClick = onVariations)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C14Variations(onPick: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC14Session.variationIndex) }
    val arts = listOf(
        R.drawable.board_c14_remix,
        R.drawable.board_c00_funny_dog,
        R.drawable.board_c09_portrait,
        R.drawable.board_ba_after
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C14_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.create_extra_remix_again),
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
                                    BoardFlowC14Session.variationIndex = index
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_c04_use_this), onClick = onPick)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C14Publish(
    onPublish: () -> Unit,
    onSave: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var visibility by remember { mutableIntStateOf(BoardFlowC14Session.visibilityIndex) }
    val visibilityOptions = listOf(
        R.string.board_c14_vis_remix,
        R.string.board_c14_vis_followers,
        R.string.board_c14_vis_inspired
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C14_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.create_extra_c14_publish_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            BoardSafeResultImage(
                bitmap = BoardFlowC14Session.resultBitmap,
                fallbackRes = R.drawable.board_c14_remix,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(radii.card))
            )
            Text(
                stringResource(R.string.board_c14_visibility),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            visibilityOptions.forEachIndexed { index, res ->
                val on = visibility == index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(if (on) colors.primary.copy(0.14f) else colors.surfaceMuted)
                        .clickable {
                            visibility = index
                            BoardFlowC14Session.visibilityIndex = index
                        }
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(res),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (on) {
                        Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Text(
                stringResource(R.string.create_extra_c14_attribution_note),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            BoardPrimaryButton(text = stringResource(R.string.board_f8_publish_cta), onClick = onPublish)
            BoardOutlinedButton(text = stringResource(R.string.board_f12_save), onClick = onSave)
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.done), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}
