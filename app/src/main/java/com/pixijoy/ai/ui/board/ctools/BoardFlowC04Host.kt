package com.pixijoy.ai.ui.board.ctools

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.pixijoy.ai.di.BoardAiEntryPoint
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardAddPhotoButton
import com.pixijoy.ai.ui.board.BoardMediaSession
import com.pixijoy.ai.ui.board.BoardSessionPhoto
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardEdgeActions
import com.pixijoy.ai.ui.board.components.BoardEdgeKind
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardProcessingHero
import com.pixijoy.ai.ui.board.components.BoardResultFooter
import com.pixijoy.ai.ui.board.components.BoardSoftEdgeGate
import com.pixijoy.ai.ui.board.components.BoardTertiaryButton
import com.pixijoy.ai.ui.board.components.BoardTitleText
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.rememberBoardPhotoGate
import com.pixijoy.ai.ui.board.toastBoardSaveResult
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.theme.PixiSpacing
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import com.pixijoy.ai.ui.board.BoardSafeResultImage
import com.pixijoy.ai.ui.board.components.BoardScreenHeader

/**
 * C04 session — BG option prefs + cutout + last processed result.
 * optionIndex aligned with [com.pixijoy.ai.ml.CreateExtraPipeline.removeBgOptions].
 */
internal object BoardFlowC04Session {
    var optionIndex: Int = 0
    var variationIndex: Int = 1
    var precision: Float = 0.7f
    var feather: Float = 0.4f
    var smooth: Float = 0.1f
    var erased: Boolean = false
    var usedSoftFallback: Boolean = false
    @Volatile
    var resultBitmap: Bitmap? = null
        private set
    @Volatile
    var cutoutBitmap: Bitmap? = null
        private set

    fun matteParams() = com.pixijoy.ai.ml.MatteRefineParams(precision, feather, smooth)

    fun setResult(bitmap: Bitmap?) {
        val old = resultBitmap
        resultBitmap = bitmap
        if (old != null && old !== bitmap && old !== cutoutBitmap && !old.isRecycled) {
            runCatching { old.recycle() }
        }
    }

    fun setCutout(bitmap: Bitmap?) {
        val old = cutoutBitmap
        cutoutBitmap = bitmap
        if (old != null && old !== bitmap && old !== resultBitmap && !old.isRecycled) {
            runCatching { old.recycle() }
        }
    }

    fun clearBitmaps() {
        setResult(null)
        setCutout(null)
    }
}

/**
 * C04 — Remove Background & Object.
 * Real ML Kit cutout + fill chips when a photo is in [BoardMediaSession]; soft art fallback otherwise.
 */
@Composable
fun BoardFlowC04Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val step = BoardCTool.stepOf(screenId)
    when (step) {
        "intro", "hub", "" -> C04ChooseIntent(
            onContinue = { onNavigate("b_c04_photo") },
            onBack = onBack
        )
        "photo" -> C04Photo(
            onSample = { onNavigate("b_c04_processing") },
            onGallery = { onNavigate("b_c04_processing") },
            onBack = { onNavigate("b_c04") }
        )
        "options" -> C04Options(
            onContinue = { onNavigate("b_c04_processing") },
            onBack = {
                if (BoardFlowC04Session.resultBitmap != null) onNavigate("b_c04_result")
                else onNavigate("b_c04_photo")
            }
        )
        "processing", "adjust" -> C04Processing(
            onDone = { onNavigate("b_c04_result") },
            onOffline = { onNavigate("b_c04_offline") },
            onLowCredits = { onNavigate("b_c04_low_credits") },
            onBack = { onNavigate("b_c04_photo") }
        )
        "refine" -> C04Refine(
            onNext = { onNavigate("b_c04_result") },
            onBack = { onNavigate("b_c04_result") }
        )
        "variations" -> C04Variations(
            onPick = { onNavigate("b_c04_result") },
            onBack = { onNavigate("b_c04_result") }
        )
        "result" -> C04Result(
            onVariations = { onNavigate("b_c04_variations") },
            onErase = { onNavigate("b_c04_erase") },
            onEdit = { onNavigate("b15_open") },
            onExport = { onNavigate("b17_presets") },
            onRemake = { onNavigate("b_c04_photo") },
            onPublish = { onNavigate("b8_publish") },
            onStickers = { onNavigate("b13_photo") },
            onBack = { onNavigate("b_c04_photo") }
        )
        "erase" -> C04Erase(
            onApply = {
                BoardFlowC04Session.erased = true
                onNavigate("b_c04_share")
            },
            onBack = { onNavigate("b_c04_result") }
        )
        "share" -> C04Share(
            onSave = { onNavigate("b8_ready") },
            onSticker = { onNavigate("b13_photo") },
            onEdit = { onNavigate("b15_open") },
            onExport = { onNavigate("b17_presets") },
            onRemake = { onNavigate("b_c04_photo") },
            onPublish = { onNavigate("b8_publish") },
            onDone = onBack,
            onBack = { onNavigate("b_c04_result") }
        )
        "offline" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Offline,
            onPrimary = { onNavigate("b_c04_options") },
            onSecondary = { onNavigate("b_c04_options") }
        )
        "low_credits" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.LowCredits,
            onPrimary = { onNavigate("b23_dash") },
            onSecondary = { onNavigate("b_c04_options") }
        )
        "safety" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Safety,
            onPrimary = { onNavigate("b_c04_photo") },
            onSecondary = onBack
        )
        else -> C04ChooseIntent(
            onContinue = { onNavigate("b_c04_photo") },
            onBack = onBack
        )
    }
}

@Composable
private fun C04ChooseIntent(
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C04_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.create_extra_remove_bg),
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
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.create_hub_remove_bg_desc),
                color = colors.textSecondary
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_c00_continue),
                onClick = onContinue,
                modifier = Modifier.testTag(TestTags.TOOL_CONTINUE)
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C04Photo(onSample: () -> Unit, onGallery: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var hasPhoto by remember { mutableStateOf(BoardMediaSession.hasPhoto()) }
    val pickThenContinue = rememberBoardPhotoGate {
        hasPhoto = true
        onGallery()
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C04_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c04_pick_photo),
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
                if (hasPhoto) {
                    BoardSessionPhoto(fallbackRes = R.drawable.board_c04_subject)
                } else {
                    Image(
                        painter = painterResource(R.drawable.board_c04_subject),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            if (!hasPhoto) {
                com.pixijoy.ai.ui.board.BoardUseSampleButton(
                    sampleRes = R.drawable.board_c04_subject,
                    onReady = onSample
                )
                BoardAddPhotoButton(
                    labelRes = R.string.board_c00_use_my_photo,
                    onPicked = {
                        hasPhoto = true
                        onGallery()
                    }
                )
            } else {
                BoardOutlinedButton(
                    text = stringResource(R.string.board_c00_continue),
                    onClick = pickThenContinue
                )
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C04Options(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC04Session.optionIndex) }
    // Indices match CreateExtraPipeline.removeBgOptions
    val options = listOf(
        Triple(R.string.board_ct_opt_auto_cutout, R.drawable.board_c04_cutout, null as Color?),
        Triple(R.string.board_ct_opt_studio_white, R.drawable.pj_bg_solid_white, Color.White),
        Triple(R.string.board_ct_opt_studio_black, R.drawable.pj_bg_solid_black, Color.Black),
        Triple(R.string.board_ct_opt_transparent, R.drawable.pj_bg_checker_hint, Color(0xFFECECEC)),
        Triple(R.string.board_ct_opt_blur_bg, R.drawable.board_c07_scene, null),
        Triple(R.string.board_ct_opt_gradient_bg, R.drawable.pj_bg_grad_accent, null),
        Triple(R.string.board_ct_opt_remove_object, R.drawable.board_c04_subject, null)
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C04_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c04_adjust_settings),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                BoardSessionPhoto(fallbackRes = R.drawable.board_c04_subject)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.md))
                    .background(colors.surfaceMuted)
                    .padding(PixiSpacing.md)
            ) {
                Column {
                    Text(
                        stringResource(R.string.board_c04_detect_label),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        stringResource(R.string.board_c04_detect_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
            Text(
                stringResource(R.string.board_c04_bg_chips_title),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                options.forEachIndexed { i, (label, art, swatch) ->
                    val on = selected == i
                    Column(
                        modifier = Modifier
                            .width(88.dp)
                            .clip(RoundedCornerShape(radii.md))
                            .background(if (on) colors.primary.copy(0.14f) else colors.surfaceMuted)
                            .then(
                                if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md))
                                else Modifier
                            )
                            .clickable {
                                selected = i
                                BoardFlowC04Session.optionIndex = i
                            }
                            .padding(PixiSpacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(radii.sm))
                                .then(
                                    if (swatch != null) Modifier.background(swatch)
                                    else Modifier
                                )
                        ) {
                            if (swatch == null) {
                                Image(
                                    painter = painterResource(art),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (i == 5) {
                                Image(
                                    painter = painterResource(art),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_c00_continue), onClick = onContinue)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C04Processing(
    onDone: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var step by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var failed by remember { mutableStateOf(false) }
    val checks = listOf(
        R.string.board_c04_check_details,
        R.string.board_c04_check_lighting,
        R.string.board_c04_check_colors,
        R.string.board_c04_check_best
    )
    val cancelled = remember { AtomicBoolean(false) }
    fun requestCancel() {
        cancelled.set(true)
        onBack()
    }
    BackHandler(onBack = { requestCancel() })
    LaunchedEffect(Unit) {
        val app = context.applicationContext
        BoardFlowC04Session.clearBitmaps()
        BoardFlowC04Session.usedSoftFallback = false
        val pipeline = runCatching {
            EntryPointAccessors.fromApplication(app, BoardAiEntryPoint::class.java)
                .createExtraPipeline()
        }.getOrNull()
        val source = BoardMediaSession.decodeBitmap(app)
        val params = BoardFlowC04Session.matteParams()

        step = 1
        progress = 0.25f
        val pair = withContext(Dispatchers.Default) {
            if (pipeline != null && source != null) {
                runCatching {
                    pipeline.removeBgWithCutout(source, BoardFlowC04Session.optionIndex, params)
                }.getOrNull()
            } else {
                null
            }
        }
        if (!isActive || cancelled.get()) return@LaunchedEffect
        step = 2
        progress = 0.5f
        delay(120)
        if (!isActive || cancelled.get()) return@LaunchedEffect
        step = 3
        progress = 0.75f
        delay(120)
        if (!isActive || cancelled.get()) return@LaunchedEffect
        step = 4
        progress = 1f

        if (pair != null) {
            val (cut, display) = pair
            if (cut != null) BoardFlowC04Session.setCutout(cut)
            BoardFlowC04Session.setResult(display)
            BoardFlowC04Session.usedSoftFallback = false
            delay(200)
            if (!isActive || cancelled.get()) return@LaunchedEffect
            onDone()
        } else {
            BoardFlowC04Session.usedSoftFallback = true
            failed = source == null
            delay(200)
            if (!isActive || cancelled.get()) return@LaunchedEffect
            onDone()
        }
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C04_HOST),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BoardProcessingHero(progress = progress)
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            BoardTitleText(text = stringResource(R.string.create_extra_remove_bg))
            Text(stringResource(R.string.create_hub_tools_caption), color = colors.textSecondary)
            Text(stringResource(R.string.board_c04_processing), color = colors.textSecondary)
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            C04PreviewImage(
                fallbackRes = R.drawable.board_c04_cutout,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(radii.card))
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
                        Text(stringResource(res), color = colors.textPrimary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Text(
                if (failed) stringResource(R.string.board_c04_soft_fallback)
                else stringResource(R.string.board_c04_few_seconds),
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            BoardEdgeActions(
                onOffline = onOffline,
                onLowCredits = null
            )
            TextButton(onClick = { requestCancel() }) {
                Text(stringResource(R.string.cancel), color = colors.primary)
            }
        }
    }
}

@Composable
private fun C04Refine(onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var precision by remember { mutableFloatStateOf(BoardFlowC04Session.precision) }
    var feather by remember { mutableFloatStateOf(BoardFlowC04Session.feather) }
    var smooth by remember { mutableFloatStateOf(BoardFlowC04Session.smooth) }
    var applying by remember { mutableStateOf(false) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C04_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, enabled = !applying) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c04_adjust_settings),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(BoardTheme.radii.card))
                    .background(Color(0xFFECECEC))
            ) {
                C04PreviewImage(
                    fallbackRes = R.drawable.board_c04_cutout,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            Text(
                stringResource(R.string.board_ct_refine_title),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Text(
                stringResource(R.string.board_c04_refine_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            RefineSlider(
                icon = R.drawable.pj_ic_focus,
                label = R.string.board_c04_refine_precision,
                value = precision,
                onChange = {
                    if (!applying) {
                        precision = it
                        BoardFlowC04Session.precision = it
                    }
                }
            )
            RefineSlider(
                icon = R.drawable.pj_ic_person,
                label = R.string.board_c04_refine_feather,
                value = feather,
                onChange = {
                    if (!applying) {
                        feather = it
                        BoardFlowC04Session.feather = it
                    }
                }
            )
            RefineSlider(
                icon = R.drawable.pj_ic_waves,
                label = R.string.board_c04_refine_smooth,
                value = smooth,
                onChange = {
                    if (!applying) {
                        smooth = it
                        BoardFlowC04Session.smooth = it
                    }
                }
            )
            BoardPrimaryButton(
                text = if (applying) {
                    stringResource(R.string.board_c04_refining)
                } else {
                    stringResource(R.string.board_f15_apply)
                },
                onClick = {
                    if (applying) return@BoardPrimaryButton
                    applying = true
                    scope.launch {
                        applyC04Matte(context)
                        applying = false
                    }
                }
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_c00_continue),
                onClick = { if (!applying) onNext() }
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C04PreviewImage(
    @DrawableRes fallbackRes: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    com.pixijoy.ai.ui.board.BoardSafeResultImage(
        bitmap = BoardFlowC04Session.resultBitmap,
        fallbackRes = fallbackRes,
        modifier = modifier,
        contentScale = contentScale
    )
}

@Composable
private fun RefineSlider(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    value: Float,
    onChange: (Float) -> Unit
) {
    val colors = BoardTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painter = painterResource(icon), null, tint = colors.primary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f).padding(horizontal = PixiSpacing.sm)) {
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
            Slider(value = value, onValueChange = onChange)
        }
        Text("${(value * 10).toInt()}", color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CheckerPreview(@DrawableRes resId: Int) {
    val radii = BoardTheme.radii
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(radii.card))
            .background(Color(0xFFECECEC))
    ) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun C04Variations(onPick: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC04Session.variationIndex) }
    val items = listOf(
        R.string.board_c04_var_original to R.drawable.board_c04_subject,
        R.string.board_ct_opt_studio_white to R.drawable.board_ba_after,
        R.string.board_c04_var_beach to R.drawable.board_c04_beach,
        R.string.board_c04_var_office to R.drawable.board_c07_city,
        R.string.board_c04_var_bokeh to R.drawable.board_c07_scene,
        R.string.board_c04_var_solid to R.drawable.board_c04_cutout
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C04_HOST),
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
            items.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEach { (label, art) ->
                        val index = items.indexOfFirst { it.first == label }
                        val on = selected == index
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(radii.md))
                                .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md)) else Modifier)
                                .clickable {
                                    selected = index
                                    BoardFlowC04Session.variationIndex = index
                                }
                        ) {
                            Image(
                                painter = painterResource(art),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(radii.md)),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                stringResource(label),
                                modifier = Modifier.padding(PixiSpacing.sm),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary
                            )
                        }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            BoardOutlinedButton(
                text = stringResource(R.string.board_c04_generate_more),
                onClick = {
                    if (items.isNotEmpty()) {
                        selected = (selected + 1) % items.size
                        BoardFlowC04Session.variationIndex = selected
                    }
                    Toast.makeText(
                        context,
                        context.getString(R.string.board_soft_preview_title),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
            BoardPrimaryButton(text = stringResource(R.string.board_c04_use_this), onClick = onPick)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C04Result(
    onVariations: () -> Unit,
    onErase: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onRemake: () -> Unit,
    onPublish: () -> Unit,
    onStickers: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var resultRev by remember { mutableIntStateOf(0) }
    var tune by remember { mutableStateOf<C04Tune?>(null) }
    val art = when (BoardFlowC04Session.variationIndex) {
        0 -> R.drawable.board_c04_subject
        1 -> R.drawable.board_ba_after
        2 -> R.drawable.board_c04_beach
        3 -> R.drawable.board_c07_city
        4 -> R.drawable.board_c07_scene
        else -> R.drawable.board_c04_cutout
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C04_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c04_heres_result),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            key(resultRev) {
                C04PreviewImage(
                    fallbackRes = art,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(radii.card))
                )
            }
            if (BoardFlowC04Session.usedSoftFallback) {
                Text(
                    stringResource(R.string.board_c04_soft_fallback),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            C04ShareActions(
                fallbackRes = art,
                onEdit = onEdit,
                onExport = onExport,
                onRemake = onRemake,
                onPublish = onPublish,
                onStickers = onStickers
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.xs)
            ) {
                BoardTertiaryButton(
                    text = stringResource(R.string.board_ct_refine_title),
                    onClick = { tune = C04Tune.Refine }
                )
                BoardTertiaryButton(
                    text = stringResource(R.string.board_c04_studio_fills),
                    onClick = { tune = C04Tune.Fills }
                )
                BoardTertiaryButton(
                    text = stringResource(R.string.board_c04_tap_erase),
                    onClick = onErase,
                    modifier = Modifier.testTag(TestTags.FLOW_C04_ERASE)
                )
            }
            TextButton(onClick = onVariations, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.board_c04_more_variations), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
    tune?.let { kind ->
        C04TuneSheet(
            kind = kind,
            onChanged = { resultRev++ },
            onDismiss = { tune = null }
        )
    }
}

@Composable
private fun C04Erase(onApply: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var tapped by remember { mutableStateOf(BoardFlowC04Session.erased) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C04_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c04_whats_next),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .clickable { tapped = true }
            ) {
                Image(
                    painter = painterResource(R.drawable.board_c04_beach),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 40.dp, y = (-20).dp)
                        .size(56.dp)
                        .border(2.dp, Color.White, CircleShape)
                        .background(Color.White.copy(0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(PixiSpacing.md)
                        .clip(RoundedCornerShape(radii.pill))
                        .background(Color.Black.copy(0.55f))
                        .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.sm)
                ) {
                    Text(
                        if (tapped) stringResource(R.string.board_c04_object_marked)
                        else stringResource(R.string.board_c04_tap_erase),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_c04_apply),
                onClick = onApply,
                modifier = Modifier.testTag(TestTags.FLOW_C04_APPLY)
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C04Share(
    onSave: () -> Unit,
    onSticker: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onRemake: () -> Unit,
    onPublish: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C04_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c04_whats_next),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            C04PreviewImage(
                fallbackRes = R.drawable.board_c04_beach,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card))
            )
            if (BoardFlowC04Session.usedSoftFallback) {
                Text(
                    stringResource(R.string.board_c04_soft_fallback),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            C04ShareActions(
                fallbackRes = R.drawable.board_c04_cutout,
                onEdit = onEdit,
                onExport = onExport,
                onRemake = onRemake,
                onPublish = onPublish,
                onStickers = onSticker,
                onSaved = onSave
            )
            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.board_ct_done), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}

private enum class C04Tune { Refine, Fills }

private suspend fun applyC04Matte(context: android.content.Context) {
    val pipeline = runCatching {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            BoardAiEntryPoint::class.java
        ).createExtraPipeline()
    }.getOrNull()
    val params = BoardFlowC04Session.matteParams()
    withContext(Dispatchers.Default) {
        val existing = BoardFlowC04Session.cutoutBitmap
        val refined = when {
            pipeline != null && existing != null && !existing.isRecycled ->
                pipeline.refineCutout(existing, params)
            pipeline != null -> {
                val src = BoardMediaSession.decodeBitmap(context.applicationContext)
                if (src != null) pipeline.cutoutTransparent(src, params) else null
            }
            else -> null
        }
        if (refined != null && pipeline != null) {
            BoardFlowC04Session.setCutout(refined)
            BoardFlowC04Session.setResult(
                pipeline.fillCutout(refined, BoardFlowC04Session.optionIndex)
            )
        }
    }
}

private suspend fun applyC04Fill(context: android.content.Context) {
    val pipeline = runCatching {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            BoardAiEntryPoint::class.java
        ).createExtraPipeline()
    }.getOrNull()
    val cut = BoardFlowC04Session.cutoutBitmap
    withContext(Dispatchers.Default) {
        if (pipeline != null && cut != null && !cut.isRecycled) {
            BoardFlowC04Session.setResult(
                pipeline.fillCutout(cut, BoardFlowC04Session.optionIndex)
            )
        }
    }
}

@Composable
private fun C04ShareActions(
    @DrawableRes fallbackRes: Int,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onRemake: () -> Unit,
    onPublish: () -> Unit,
    onStickers: (() -> Unit)? = null,
    onSaved: (() -> Unit)? = null
) {
    val context = LocalContext.current
    fun shareWa() {
        BoardDrawableIo.shareToWhatsApp(
            context = context,
            bitmap = BoardFlowC04Session.resultBitmap,
            fallbackRes = fallbackRes,
            filePrefix = "PixiJoy_BG"
        )
    }
    BoardResultFooter(
        onSave = {
            val bmp = BoardFlowC04Session.resultBitmap
            val ok = if (bmp != null && !bmp.isRecycled) {
                BoardDrawableIo.saveBitmapToGallery(context, bmp, filePrefix = "PixiJoy_BG")
            } else {
                BoardDrawableIo.saveToGallery(context, fallbackRes, filePrefix = "PixiJoy_BG")
            }
            context.toastBoardSaveResult(ok)
            if (ok) onSaved?.invoke()
        },
        onShare = {
            BoardDrawableIo.shareBitmapAsync(
                context = context,
                bitmap = BoardFlowC04Session.resultBitmap,
                fallbackRes = fallbackRes,
                filePrefix = "PixiJoy_BG"
            )
        },
        onEdit = onEdit,
        onExport = onExport,
        onRemake = onRemake,
        onPublish = onPublish,
        onStickers = onStickers,
        onWhatsApp = { shareWa() },
        onStatus = { shareWa() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun C04TuneSheet(
    kind: C04Tune,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var applying by remember { mutableStateOf(false) }
    var precision by remember { mutableFloatStateOf(BoardFlowC04Session.precision) }
    var feather by remember { mutableFloatStateOf(BoardFlowC04Session.feather) }
    var smooth by remember { mutableFloatStateOf(BoardFlowC04Session.smooth) }
    var selected by remember { mutableIntStateOf(BoardFlowC04Session.optionIndex) }
    val fills = listOf(
        Triple(R.string.board_ct_opt_auto_cutout, R.drawable.board_c04_cutout, null as Color?),
        Triple(R.string.board_ct_opt_studio_white, R.drawable.pj_bg_solid_white, Color.White),
        Triple(R.string.board_ct_opt_studio_black, R.drawable.pj_bg_solid_black, Color.Black),
        Triple(R.string.board_ct_opt_transparent, R.drawable.pj_bg_checker_hint, Color(0xFFECECEC)),
        Triple(R.string.board_ct_opt_blur_bg, R.drawable.board_c07_scene, null),
        Triple(R.string.board_ct_opt_gradient_bg, R.drawable.pj_bg_grad_accent, null),
        Triple(R.string.board_ct_opt_remove_object, R.drawable.board_c04_subject, null)
    )
    ModalBottomSheet(
        onDismissRequest = { if (!applying) onDismiss() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = radii.sheet, topEnd = radii.sheet)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = PixiSpacing.xl, vertical = PixiSpacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(
                    if (kind == C04Tune.Refine) R.string.board_ct_refine_title
                    else R.string.board_c04_studio_fills
                ),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            if (kind == C04Tune.Refine) {
                Text(
                    stringResource(R.string.board_c04_refine_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                RefineSlider(
                    icon = R.drawable.pj_ic_focus,
                    label = R.string.board_c04_refine_precision,
                    value = precision,
                    onChange = {
                        if (!applying) {
                            precision = it
                            BoardFlowC04Session.precision = it
                        }
                    }
                )
                RefineSlider(
                    icon = R.drawable.pj_ic_person,
                    label = R.string.board_c04_refine_feather,
                    value = feather,
                    onChange = {
                        if (!applying) {
                            feather = it
                            BoardFlowC04Session.feather = it
                        }
                    }
                )
                RefineSlider(
                    icon = R.drawable.pj_ic_waves,
                    label = R.string.board_c04_refine_smooth,
                    value = smooth,
                    onChange = {
                        if (!applying) {
                            smooth = it
                            BoardFlowC04Session.smooth = it
                        }
                    }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    fills.forEachIndexed { i, (label, art, swatch) ->
                        val on = selected == i
                        Column(
                            modifier = Modifier
                                .width(88.dp)
                                .clip(RoundedCornerShape(radii.md))
                                .background(if (on) colors.primary.copy(0.14f) else colors.surfaceMuted)
                                .then(
                                    if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md))
                                    else Modifier
                                )
                                .clickable {
                                    selected = i
                                    BoardFlowC04Session.optionIndex = i
                                }
                                .padding(PixiSpacing.sm),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(radii.sm))
                                    .then(if (swatch != null) Modifier.background(swatch) else Modifier)
                            ) {
                                if (swatch == null || i == 5) {
                                    Image(
                                        painter = painterResource(art),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                stringResource(label),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_c04_apply),
                onClick = {
                    if (applying) return@BoardPrimaryButton
                    applying = true
                    scope.launch {
                        if (kind == C04Tune.Refine) applyC04Matte(context)
                        else applyC04Fill(context)
                        applying = false
                        onChanged()
                        onDismiss()
                    }
                }
            )
        }
    }
}
