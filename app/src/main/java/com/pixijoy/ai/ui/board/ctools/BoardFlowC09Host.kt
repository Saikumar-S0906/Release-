package com.pixijoy.ai.ui.board.ctools

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
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.runtime.mutableStateOf
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

/** C09 session — soft portrait prefs (local demo only). */
internal object BoardFlowC09Session {
    var styleIndex: Int = 0
    var outfitIndex: Int = 0
    var outfitFilter: Int = 0
    var variationIndex: Int = 0
    var enhanceIndex: Int = 0

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
 * C09 — AI Portrait & Headshot.
 * Soft: simulated style / outfit / portrait generation (no real face enhance).
 */
@Composable
fun BoardFlowC09Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    val step = BoardCTool.stepOf(screenId)
    when (step) {
        "intro", "hub", "" -> C09Intro(
            onStart = { onNavigate("b_c09_photo") },
            onBack = onBack,
            onTab = tab
        )
        "photo" -> C09SelectPhoto(
            onPick = { onNavigate("b_c09_options") },
            onGallery = { onNavigate("b3_chooser") },
            onBack = { onNavigate("b_c09_intro") },
            onTab = tab
        )
        "options" -> C09ChooseStyle(
            onContinue = { onNavigate("b_c09_adjust") },
            onBack = { onNavigate("b_c09_photo") },
            onTab = tab
        )
        "adjust" -> C09OutfitBackground(
            onGenerate = { onNavigate("b_c09_processing") },
            onBack = { onNavigate("b_c09_options") }
        )
        "processing" -> C09Processing(
            onDone = { onNavigate("b_c09_result") },
            onOffline = { onNavigate("b_c09_offline") },
            onLowCredits = { onNavigate("b_c09_low_credits") },
            onBack = { onNavigate("b_c09_adjust") }
        )
        "result" -> C09Result(
            onUse = { onNavigate("b_c09_share") },
            onStyles = { onNavigate("b_c09_variations") },
            onBack = { onNavigate("b_c09_adjust") }
        )
        "variations" -> C09Variations(
            onPick = { onNavigate("b_c09_share") },
            onMore = { onNavigate("b_c09_processing") },
            onBack = { onNavigate("b_c09_result") }
        )
        "share" -> C09Export(
            onExport = { onNavigate("b8_ready") },
            onShare = { onNavigate("b8_share") },
            onBack = { onNavigate("b_c09_variations") },
            onDone = onBack
        )
        "offline" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_offline_title),
            body = stringResource(R.string.board_ct_offline_body),
            primary = stringResource(R.string.board_f3_try_again),
            onPrimary = { onNavigate("b_c09_processing") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate("b_c09_adjust") }
        )
        "low_credits" -> BoardGateScreen(
            title = stringResource(R.string.board_f19_low_credits_title),
            body = stringResource(R.string.board_f19_low_credits_body),
            primary = stringResource(R.string.board_f19_get_credits),
            onPrimary = { onNavigate("b23_dash") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate("b_c09_adjust") }
        )
        "safety" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_safety_title),
            body = stringResource(R.string.board_f4_safety_body),
            primary = stringResource(R.string.board_f3_try_another),
            onPrimary = { onNavigate("b_c09_photo") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = onBack
        )
        else -> C09Intro(
            onStart = { onNavigate("b_c09_photo") },
            onBack = onBack,
            onTab = tab
        )
    }
}

@Composable
private fun C09Intro(
    onStart: () -> Unit,
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
                .testTag(TestTags.FLOW_C09_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.create_extra_portrait),
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
                    painter = painterResource(R.drawable.board_c09_portrait),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(0.7f))
                            )
                        )
                        .padding(PixiSpacing.lg)
                ) {
                    Column {
                        Text(
                            stringResource(R.string.board_c09_card_title),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            stringResource(R.string.board_c09_slogan),
                            color = Color.White.copy(0.9f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_c09_get_started), onClick = onStart)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C09SelectPhoto(
    onPick: () -> Unit,
    onGallery: () -> Unit,
    onBack: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val recent = listOf(
        R.drawable.board_c09_portrait,
        R.drawable.board_b5_photo,
        R.drawable.board_c00_hero,
        R.drawable.board_ba_after,
        R.drawable.board_c06_faces,
        R.drawable.board_c08_people
    )
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C09_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c09_select_photo),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(R.drawable.board_b5_photo),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card))
                    .clickable(onClick = onPick),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.board_c09_recent),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            recent.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEach { art ->
                        Image(
                            painter = painterResource(art),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(radii.md))
                                .clickable(onClick = onPick),
                            contentScale = ContentScale.Crop
                        )
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_c00_use_this_photo), onClick = onPick)
            BoardOutlinedButton(text = stringResource(R.string.board_c00_use_my_photo), onClick = onGallery)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C09ChooseStyle(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC09Session.styleIndex) }
    val styles = listOf(
        StyleItem(R.string.board_c09_style_pro, R.string.board_c09_style_pro_sub, R.drawable.board_c09_portrait),
        StyleItem(R.string.board_c09_style_soft, R.string.board_c09_style_soft_sub, R.drawable.board_ba_after),
        StyleItem(R.string.board_c09_style_linkedin, R.string.board_c09_style_linkedin_sub, R.drawable.board_b5_photo),
        StyleItem(R.string.board_c09_style_yearbook, R.string.board_c09_style_yearbook_sub, R.drawable.board_c00_hero),
        StyleItem(R.string.board_c09_style_editorial, R.string.board_c09_style_editorial_sub, R.drawable.board_c06_faces),
        StyleItem(R.string.board_c09_style_natural, R.string.board_c09_style_natural_sub, R.drawable.board_c08_people)
    )
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C09_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c09_select_style),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            styles.chunked(2).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEachIndexed { col, item ->
                        val index = rowIndex * 2 + col
                        val on = selected == index
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(radii.md))
                                .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md)) else Modifier)
                                .clickable {
                                    selected = index
                                    BoardFlowC09Session.styleIndex = index
                                }
                        ) {
                            Image(
                                painter = painterResource(item.art),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 4f)
                                    .clip(RoundedCornerShape(topStart = radii.md, topEnd = radii.md)),
                                contentScale = ContentScale.Crop
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (on) colors.primary.copy(0.12f) else colors.surfaceMuted)
                                    .padding(PixiSpacing.sm)
                            ) {
                                Text(
                                    stringResource(item.title),
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(item.sub),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_c00_continue), onClick = onContinue)
            BoardTrustFooter()
        }
    }
}

private data class StyleItem(
    @StringRes val title: Int,
    @StringRes val sub: Int,
    @DrawableRes val art: Int
)

@Composable
private fun C09OutfitBackground(onGenerate: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var filter by remember { mutableIntStateOf(BoardFlowC09Session.outfitFilter) }
    var selected by remember { mutableIntStateOf(BoardFlowC09Session.outfitIndex) }
    val filters = listOf(
        R.string.board_c09_filter_all,
        R.string.board_c09_filter_formal,
        R.string.board_c09_filter_casual,
        R.string.board_c09_filter_studio
    )
    val outfits = listOf(
        R.drawable.board_c09_portrait,
        R.drawable.board_b5_photo,
        R.drawable.board_ba_after,
        R.drawable.board_c00_hero,
        R.drawable.board_c06_faces,
        R.drawable.board_c04_cutout
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C09_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c09_outfit_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
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
                            .clickable {
                                filter = i
                                BoardFlowC09Session.outfitFilter = i
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
            outfits.chunked(3).forEachIndexed { rowIndex, row ->
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
                                .aspectRatio(3f / 4f)
                                .clip(RoundedCornerShape(radii.md))
                                .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md)) else Modifier)
                                .clickable {
                                    selected = index
                                    BoardFlowC09Session.outfitIndex = index
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f19_generate), onClick = onGenerate)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C09Processing(
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
        R.string.board_c09_check_understand,
        R.string.board_c09_check_style,
        R.string.board_c09_check_details,
        R.string.board_c09_check_life
    )
    LaunchedEffect(Unit) {
        BoardFlowC09Session.setResult(null)
        BoardFlowC09Session.usedSoftFallback = false
        step = 1
        progress = 0.25f
        val style = BoardFlowC09Session.styleIndex
        val result = BoardCToolEngine.run(context) { pipe, src ->
            pipe.faceRecastOrSwap(src, style)
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
            BoardFlowC09Session.setResult(result)
            BoardFlowC09Session.usedSoftFallback = false
        } else {
            BoardFlowC09Session.usedSoftFallback = true
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
                .testTag(TestTags.FLOW_C09_HOST)
                .padding(PixiSpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BoardProcessingHero(progress = progress, darkSurface = true)
                Spacer(modifier = Modifier.height(PixiSpacing.md))
                Text(
                    stringResource(R.string.create_extra_portrait),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    stringResource(R.string.board_c09_creating),
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
private fun C09Result(
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onRemake: () -> Unit,
    onPublish: () -> Unit,
    onStyles: () -> Unit,
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
                .testTag(TestTags.FLOW_C09_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.create_extra_portrait),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            BoardSafeResultImage(
                bitmap = BoardFlowC09Session.resultBitmap,
                fallbackRes = R.drawable.board_c09_portrait,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(radii.card))
            )
            if (BoardFlowC09Session.usedSoftFallback) {
                Text(
                    stringResource(R.string.board_c04_soft_fallback),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            BoardResultFooter(
                onSave = {
                    val out = BoardFlowC09Session.resultBitmap
                    val ok = if (out != null && !out.isRecycled) {
                        BoardDrawableIo.saveBitmapToGallery(context, out, filePrefix = "PixiJoy_Portrait")
                    } else {
                        BoardDrawableIo.saveToGallery(
                            context,
                            R.drawable.board_c09_portrait,
                            filePrefix = "PixiJoy_Portrait"
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
            TextButton(onClick = onStyles, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.board_f7_another_style), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C09Result(onUse: () -> Unit, onStyles: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TestTags.FLOW_C09_HOST)
        ) {
            Image(
                painter = painterResource(R.drawable.board_c09_portrait),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(PixiSpacing.md)
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_back),
                    contentDescription = stringResource(R.string.cd_back),
                    tint = Color.White
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(0.75f))
                        )
                    )
                    .padding(PixiSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(radii.pill))
                        .background(Color.White)
                        .clickable(onClick = onStyles)
                        .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.sm)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        Icon(painter = painterResource(R.drawable.pj_ic_sparkles), null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        Text(
                            stringResource(R.string.board_f7_another_style),
                            color = colors.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                BoardPrimaryButton(text = stringResource(R.string.board_f7_use_this), onClick = onUse)
            }
        }
    }
}

@Composable
private fun C09Variations(onPick: () -> Unit, onMore: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC09Session.variationIndex) }
    val arts = listOf(
        R.drawable.board_c09_portrait,
        R.drawable.board_ba_after,
        R.drawable.board_b5_photo,
        R.drawable.board_c00_hero,
        R.drawable.board_c06_faces,
        R.drawable.board_c08_people
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C09_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c09_more_variations),
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
                                .aspectRatio(3f / 4f)
                                .clip(RoundedCornerShape(radii.md))
                                .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md)) else Modifier)
                                .clickable {
                                    selected = index
                                    BoardFlowC09Session.variationIndex = index
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_c09_generate_more), onClick = onMore)
            BoardOutlinedButton(text = stringResource(R.string.board_f7_use_this), onClick = onPick)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C09Export(
    onExport: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var enhance by remember { mutableIntStateOf(BoardFlowC09Session.enhanceIndex) }
    val tools = listOf(
        EnhanceItem(R.string.board_ct_opt_glow, R.drawable.pj_ic_sparkles),
        EnhanceItem(R.string.board_ct_opt_relight, R.drawable.pj_ic_sun),
        EnhanceItem(R.string.board_ct_opt_youthful, R.drawable.pj_ic_face),
        EnhanceItem(R.string.board_ct_opt_teeth, R.drawable.pj_ic_tune),
        EnhanceItem(R.string.board_ct_opt_hair, R.drawable.pj_ic_face)
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C09_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c09_export_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            BoardSafeResultImage(
                bitmap = BoardFlowC09Session.resultBitmap,
                fallbackRes = R.drawable.board_c09_portrait,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card))
            )
            tools.forEachIndexed { index, item ->
                val on = enhance == index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(if (on) colors.primary.copy(0.14f) else colors.surfaceMuted)
                        .clickable {
                            enhance = index
                            BoardFlowC09Session.enhanceIndex = index
                        }
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Icon(painter = painterResource(item.icon), null, tint = if (on) colors.primary else colors.textSecondary)
                    Text(
                        stringResource(item.title),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (on) {
                        Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_c09_export_11),
                onClick = onExport,
                modifier = Modifier.fillMaxWidth()
            )
            BoardOutlinedButton(text = stringResource(R.string.board_f2_share), onClick = onShare)
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.done), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}

private data class EnhanceItem(
    @StringRes val title: Int,
    @DrawableRes val icon: Int
)
