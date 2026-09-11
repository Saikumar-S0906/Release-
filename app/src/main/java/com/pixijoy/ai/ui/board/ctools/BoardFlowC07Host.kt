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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
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
import com.pixijoy.ai.ui.board.components.BoardBeforeAfter
import com.pixijoy.ai.ui.board.components.BoardFlowArt
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import com.pixijoy.ai.ui.board.components.BoardEdgeActions
import com.pixijoy.ai.ui.board.components.BoardScreenHeader
import kotlinx.coroutines.launch

/** C07 session — soft scene relocate prefs (local demo only). */
internal object BoardFlowC07Session {
    var sceneIndex: Int = 0
    var intensity: Float = 0.65f
    var prompt: String = ""
    var variationIndex: Int = 0

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

private fun c07SceneArt(index: Int = BoardFlowC07Session.sceneIndex): Int =
    BoardFlowArt.sceneThumbs[index.coerceIn(0, BoardFlowArt.sceneThumbs.lastIndex)]

/**
 * C07 — Change Scene / Relocate.
 * Soft: simulated scene swap; no real generative backends.
 */
@Composable
fun BoardFlowC07Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    val step = BoardCTool.stepOf(screenId)
    when (step) {
        "intro", "hub", "" -> C07ChooseIntent(
            onStart = { onNavigate("b_c07_photo") },
            onBack = onBack,
            onTab = tab
        )
        "photo" -> C07Photo(
            onSample = { onNavigate("b_c07_options") },
            onGallery = { onNavigate("b3_chooser") },
            onBack = { onNavigate("b_c07_intro") }
        )
        "options" -> C07SceneGrid(
            onContinue = { onNavigate("b_c07_adjust") },
            onBack = { onNavigate("b_c07_photo") }
        )
        "adjust" -> C07Adjust(
            onGenerate = { onNavigate("b_c07_processing") },
            onBack = { onNavigate("b_c07_options") }
        )
        "processing" -> C07Processing(
            onDone = { onNavigate("b_c07_result") },
            onOffline = { onNavigate("b_c07_offline") },
            onLowCredits = { onNavigate("b_c07_low_credits") },
            onCancel = { onNavigate("b_c07_adjust") }
        )
        "result" -> C07Result(
            onUse = { onNavigate("b_c07_share") },
            onVariations = { onNavigate("b_c07_variations") },
            onBack = { onNavigate("b_c07_adjust") }
        )
        "variations" -> C07Variations(
            onPick = { onNavigate("b_c07_share") },
            onBack = { onNavigate("b_c07_result") }
        )
        "share" -> C07Share(
            onSave = { onNavigate("b8_ready") },
            onShare = { onNavigate("b8_share") },
            onEdit = { onNavigate("b_c07_options") },
            onDone = onBack,
            onBack = { onNavigate("b_c07_result") }
        )
        "offline" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_offline_title),
            body = stringResource(R.string.board_ct_offline_body),
            primary = stringResource(R.string.board_f3_try_again),
            onPrimary = { onNavigate("b_c07_processing") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate("b_c07_adjust") }
        )
        "low_credits" -> BoardGateScreen(
            title = stringResource(R.string.board_f19_low_credits_title),
            body = stringResource(R.string.board_f19_low_credits_body),
            primary = stringResource(R.string.board_f19_get_credits),
            onPrimary = { onNavigate("b23_dash") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate("b_c07_adjust") }
        )
        "safety" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_safety_title),
            body = stringResource(R.string.board_f4_safety_body),
            primary = stringResource(R.string.board_f3_try_another),
            onPrimary = { onNavigate("b_c07_photo") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = onBack
        )
        else -> C07ChooseIntent(
            onStart = { onNavigate("b_c07_photo") },
            onBack = onBack,
            onTab = tab
        )
    }
}

@Composable
private fun C07ChooseIntent(
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
                .testTag(TestTags.FLOW_C07_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.create_extra_scene),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        stringResource(R.string.board_ct_tip_scene),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
            Image(
                painter = painterResource(R.drawable.board_c07_scene),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.create_hub_tools_caption),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            BoardPrimaryButton(text = stringResource(R.string.board_ct_get_started), onClick = onStart)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C07Photo(onSample: () -> Unit, onGallery: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C07_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_ct_photo_title, stringResource(R.string.create_extra_scene)),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(R.drawable.board_c04_subject),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            com.pixijoy.ai.ui.board.BoardUseSampleButton(
                sampleRes = R.drawable.board_c07_scene,
                onReady = onSample
            )
            BoardOutlinedButton(text = stringResource(R.string.board_c00_use_my_photo), onClick = onGallery)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C07SceneGrid(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC07Session.sceneIndex) }
    val labels = BoardCTool.Scene.optionRes
    val thumbs = BoardFlowArt.sceneThumbs
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C07_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_ct_options_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(c07SceneArt(selected)),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            labels.chunked(2).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEachIndexed { col, label ->
                        val index = rowIndex * 2 + col
                        val on = selected == index
                        val thumb = thumbs[index % thumbs.size]
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(radii.md))
                                .background(if (on) colors.primary.copy(0.12f) else colors.surfaceMuted)
                                .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md)) else Modifier)
                                .clickable {
                                    selected = index
                                    BoardFlowC07Session.sceneIndex = index
                                }
                                .padding(PixiSpacing.sm)
                        ) {
                            Image(
                                painter = painterResource(thumb),
                                contentDescription = stringResource(label),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.15f)
                                    .clip(RoundedCornerShape(radii.sm)),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                stringResource(label),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = PixiSpacing.sm),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary
                            )
                        }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_c00_continue), onClick = onContinue)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C07Adjust(onGenerate: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var intensity by remember { mutableFloatStateOf(BoardFlowC07Session.intensity) }
    var prompt by remember { mutableStateOf(BoardFlowC07Session.prompt) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C07_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_ct_adjust_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(c07SceneArt()),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.board_ct_strength),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = intensity,
                    onValueChange = {
                        intensity = it
                        BoardFlowC07Session.intensity = it
                    },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${(intensity * 100).toInt()}%",
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = PixiSpacing.sm)
                )
            }
            OutlinedTextField(
                value = prompt,
                onValueChange = {
                    prompt = it
                    BoardFlowC07Session.prompt = it
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(radii.md),
                label = { Text(stringResource(R.string.tell_pixijoy_hint)) },
                placeholder = { Text(stringResource(R.string.board_f7_sug_beach)) }
            )
            Text(
                stringResource(R.string.board_ct_tip_scene),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            BoardPrimaryButton(text = stringResource(R.string.board_c00_continue), onClick = onGenerate)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C07Processing(
    onDone: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    var step by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    val checks = listOf(
        R.string.board_f7_step_photo,
        R.string.board_f3_step_scene,
        R.string.board_f7_step_style,
        R.string.board_f7_step_create
    )
    LaunchedEffect(Unit) {
        BoardFlowC07Session.setResult(null)
        BoardFlowC07Session.usedSoftFallback = false
        step = 1
        progress = 0.25f
        val sceneRes = c07SceneArt()
        val result = BoardCToolEngine.run(context) { pipe, src ->
            val scene = decodeBoardDrawable(context.applicationContext, sceneRes, maxSidePx = 1600)
                ?: return@run src
            pipe.relocateToScene(src, scene)
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
            BoardFlowC07Session.setResult(result)
            BoardFlowC07Session.usedSoftFallback = false
        } else {
            BoardFlowC07Session.usedSoftFallback = true
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
                .testTag(TestTags.FLOW_C07_HOST)
                .padding(PixiSpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BoardProcessingHero(progress = progress, darkSurface = true)
                Spacer(modifier = Modifier.height(PixiSpacing.md))
                Text(
                    stringResource(R.string.create_extra_scene),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(stringResource(R.string.create_hub_tools_caption), color = Color.White.copy(0.85f))
                Text(
                    stringResource(R.string.board_f7_generating_title),
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
                BoardOutlinedButton(text = stringResource(R.string.board_f19_cancel), onClick = onCancel)
            }
        }
    }
}

@Composable
private fun C07Result(
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
                .testTag(TestTags.FLOW_C07_HOST),
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
                    stringResource(R.string.board_ct_result_title, stringResource(R.string.create_extra_scene)),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            BoardSafeResultImage(
                bitmap = BoardFlowC07Session.resultBitmap,
                fallbackRes = c07SceneArt(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card))
            )
            if (BoardFlowC07Session.usedSoftFallback) {
                Text(
                    stringResource(R.string.board_c04_soft_fallback),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                stringResource(
                    BoardCTool.Scene.optionRes[
                        BoardFlowC07Session.sceneIndex.coerceIn(0, BoardCTool.Scene.optionRes.lastIndex)
                    ]
                ),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            BoardResultFooter(
                onSave = {
                    val out = BoardFlowC07Session.resultBitmap
                    val ok = if (out != null && !out.isRecycled) {
                        BoardDrawableIo.saveBitmapToGallery(context, out, filePrefix = "PixiJoy_Scene")
                    } else {
                        BoardDrawableIo.saveToGallery(context, c07SceneArt(), filePrefix = "PixiJoy_Scene")
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
private fun C07Result(onUse: () -> Unit, onVariations: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
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
                .testTag(TestTags.FLOW_C07_HOST),
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
                    stringResource(R.string.board_ct_result_title, stringResource(R.string.create_extra_scene)),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            BoardBeforeAfter(
                beforeRes = R.drawable.board_c07_scene,
                afterRes = c07SceneArt()
            )
            Text(
                stringResource(
                    BoardCTool.Scene.optionRes[
                        BoardFlowC07Session.sceneIndex.coerceIn(0, BoardCTool.Scene.optionRes.lastIndex)
                    ]
                ),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
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
private fun C07Variations(onPick: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC07Session.variationIndex) }
    val arts = BoardFlowArt.sceneThumbs
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C07_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c04_more_variations),
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
                                    BoardFlowC07Session.variationIndex = index
                                    BoardFlowC07Session.sceneIndex = index
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
private fun C07Share(
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
                .testTag(TestTags.FLOW_C07_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_ct_share_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            BoardSafeResultImage(
                bitmap = BoardFlowC07Session.resultBitmap,
                fallbackRes = c07SceneArt(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(BoardTheme.radii.card))
            )
            BoardPrimaryButton(text = stringResource(R.string.board_f12_save), onClick = onSave)
            BoardPrimaryButton(text = stringResource(R.string.board_f2_share), onClick = onShare)
            BoardOutlinedButton(text = stringResource(R.string.board_f7_edit), onClick = onEdit)
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.board_ct_done), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}
