package com.pixijoy.ai.ui.board.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Filter
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardAddPhotoButton
import com.pixijoy.ai.ui.board.BoardEditorDocument
import com.pixijoy.ai.ui.board.BoardMediaSession
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.BoardSafeResultImage
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.BoardSessionPhoto
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardArtImage
import com.pixijoy.ai.ui.board.components.BoardColorAdjust
import com.pixijoy.ai.ui.board.components.BoardCropCanvas
import com.pixijoy.ai.ui.board.components.BoardCropNorm
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardEdgeActions
import com.pixijoy.ai.ui.board.components.BoardEdgeKind
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardListDetail
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardResultFooter
import com.pixijoy.ai.ui.board.components.BoardScreenHeader
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardSoftEdgeGate
import com.pixijoy.ai.ui.board.components.BoardTipCallout
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.components.BoardWideFrame
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.components.WindowWidthSizeClass
import com.pixijoy.ai.ui.components.rememberWindowWidthSizeClass
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Flow 15 session — delegates to [BoardEditorDocument] (undo/redo + working bitmap). */
internal object BoardFlow15Session {
    var aspectIndex: Int
        get() = BoardEditorDocument.aspectIndex
        set(value) {
            BoardEditorDocument.aspectIndex = value
        }
    var filterIndex: Int
        get() = BoardEditorDocument.filterIndex
        set(value) {
            BoardEditorDocument.filterIndex = value
        }
    var filterCategory: Int = 0
    var textOverlay: String
        get() = BoardEditorDocument.textOverlay
        set(value) {
            BoardEditorDocument.textOverlay = value
        }
    var textTool: Int = 0
    var fontIndex: Int = 0
    var colorIndex: Int = 0
    var alignIndex: Int = 1
    var brightness: Float
        get() = BoardEditorDocument.brightness
        set(value) {
            BoardEditorDocument.brightness = value
        }
    var contrast: Float
        get() = BoardEditorDocument.contrast
        set(value) {
            BoardEditorDocument.contrast = value
        }
    var saturation: Float
        get() = BoardEditorDocument.saturation
        set(value) {
            BoardEditorDocument.saturation = value
        }
    var warmth: Float
        get() = BoardEditorDocument.warmth
        set(value) {
            BoardEditorDocument.warmth = value
        }
    var sharpness: Float
        get() = BoardEditorDocument.sharpness
        set(value) {
            BoardEditorDocument.sharpness = value
        }
    var vignette: Float
        get() = BoardEditorDocument.vignette
        set(value) {
            BoardEditorDocument.vignette = value
        }
    var grain: Float
        get() = BoardEditorDocument.grain
        set(value) {
            BoardEditorDocument.grain = value
        }
    var hue: Float
        get() = BoardEditorDocument.hue
        set(value) {
            BoardEditorDocument.hue = value
        }
    var lightness: Float
        get() = BoardEditorDocument.lightness
        set(value) {
            BoardEditorDocument.lightness = value
        }
    var shadows: Float
        get() = BoardEditorDocument.shadows
        set(value) {
            BoardEditorDocument.shadows = value
        }
    var midtones: Float
        get() = BoardEditorDocument.midtones
        set(value) {
            BoardEditorDocument.midtones = value
        }
    var highlights: Float
        get() = BoardEditorDocument.highlights
        set(value) {
            BoardEditorDocument.highlights = value
        }
    var showAfter: Boolean
        get() = BoardEditorDocument.showAfter
        set(value) {
            BoardEditorDocument.showAfter = value
        }

    @DrawableRes
    fun hero(): Int = if (showAfter) BoardFlowArt.baAfter else BoardFlowArt.baBefore
}

/**
 * Flow 15 — Advanced Photo Editor.
 * Entry via Create Tools; production 5-tab shell.
 */
@Composable
fun BoardFlow15Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    val pipeline = LocalBoardPipeline.current
    when (screenId) {
        "b15_hub", "b15_open" -> OpenEditor(
            onTab = tab,
            onEdit = { onNavigate("b15_workspace") },
            onGallery = pipeline.openMediaInput,
            onOffline = { onNavigate("b15_offline") },
            onLowCredits = { onNavigate("b15_low_credits") },
            onSafety = { onNavigate("b15_safety") },
            onBack = onBack
        )
        "b15_workspace" -> Workspace(
            onCrop = { onNavigate("b15_crop") },
            onAdjust = { onNavigate("b15_adjust") },
            onFilters = { onNavigate("b15_filters") },
            onText = { onNavigate("b15_text") },
            onMore = { onNavigate("b15_more") },
            onSave = { onNavigate("b15_save") },
            onBack = { onNavigate("b15_open") }
        )
        "b15_crop" -> CropRotate(
            onDone = { onNavigate("b15_workspace") },
            onBack = { onNavigate("b15_workspace") }
        )
        "b15_adjust" -> AdjustEnhance(
            onDone = { onNavigate("b15_workspace") },
            onBack = { onNavigate("b15_workspace") }
        )
        "b15_heal" -> BoardFlow15HealBrush(
            onDone = { onNavigate("b15_workspace") },
            onBack = { onNavigate("b15_workspace") }
        )
        "b15_filters" -> FiltersEffects(
            onDone = { onNavigate("b15_workspace") },
            onBack = { onNavigate("b15_workspace") }
        )
        "b15_text" -> TextStickers(
            onDone = { onNavigate("b15_workspace") },
            onBack = { onNavigate("b15_workspace") }
        )
        "b15_more" -> MoreTools(
            onPick = { dest -> onNavigate(dest) },
            onBack = { onNavigate("b15_workspace") }
        )
        "b15_save" -> SaveShare(
            onEdit = { onNavigate("b15_workspace") },
            onExport = { onNavigate("b17_open") },
            onRemake = { onNavigate("b15_workspace") },
            onPublish = { onNavigate("b8_publish") },
            onStickers = { onNavigate("b13_photo") },
            onDone = onBack,
            onBack = { onNavigate("b15_workspace") }
        )
        "b15_offline" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Offline,
            onPrimary = { onNavigate("b15_workspace") },
            onSecondary = { onNavigate("b15_open") }
        )
        "b15_low_credits" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.LowCredits,
            onPrimary = { onNavigate("b23_dash") },
            onSecondary = { onNavigate("b15_workspace") }
        )
        "b15_safety" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Safety,
            onPrimary = { onNavigate("b15_open") },
            onSecondary = onBack
        )
        else -> OpenEditor(
            onTab = tab,
            onEdit = { onNavigate("b15_workspace") },
            onGallery = pipeline.openMediaInput,
            onOffline = { onNavigate("b15_offline") },
            onLowCredits = { onNavigate("b15_low_credits") },
            onSafety = { onNavigate("b15_safety") },
            onBack = onBack
        )
    }
}

@Composable
private fun OpenEditor(
    onTab: (BoardShellTab) -> Unit,
    onEdit: () -> Unit,
    onGallery: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit,
    onSafety: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var hasPhoto by remember { mutableStateOf(BoardMediaSession.hasPhoto() || BoardEditorDocument.workingBitmap != null) }
    val openRaw = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        BoardMediaSession.setPhotoUri(uri)
        BoardEditorDocument.setWorkingBitmap(null)
        BoardEditorDocument.ensureLoaded(context)
        val loaded = BoardEditorDocument.workingBitmap
        if (loaded == null || loaded.isRecycled) {
            Toast.makeText(context, context.getString(R.string.board_f15_raw_failed), Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        hasPhoto = true
        onEdit()
    }
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW15_HOST),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            BoardArtImage(
                resId = BoardFlowArt.logo,
                contentDescription = stringResource(R.string.app_name),
                maxSidePx = 256,
                modifier = Modifier.size(56.dp)
            )
            Text(
                stringResource(R.string.flow15_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                BoardSafeResultImage(
                    bitmap = BoardEditorDocument.workingBitmap,
                    fallbackRes = BoardFlowArt.editorHero,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (!hasPhoto) {
                com.pixijoy.ai.ui.board.BoardUseSampleButton(
                    sampleRes = BoardFlowArt.editorHero,
                    onReady = {
                        BoardEditorDocument.setWorkingBitmap(null)
                        BoardEditorDocument.ensureLoaded(context)
                        hasPhoto = true
                        onEdit()
                    }
                )
                BoardAddPhotoButton(
                    labelRes = R.string.board_c00_use_my_photo,
                    onPicked = {
                        BoardEditorDocument.setWorkingBitmap(null)
                        BoardEditorDocument.ensureLoaded(context)
                        hasPhoto = true
                        onEdit()
                    }
                )
            } else {
                BoardPrimaryButton(
                    text = stringResource(R.string.flow15_title),
                    onClick = {
                        BoardEditorDocument.ensureLoaded(context)
                        onEdit()
                    }
                )
            }
            BoardOutlinedButton(
                text = stringResource(R.string.board_f15_open_raw),
                onClick = {
                    openRaw.launch(
                        arrayOf(
                            "image/*",
                            "image/x-adobe-dng",
                            "image/x-canon-cr2",
                            "image/x-nikon-nef"
                        )
                    )
                }
            )
            BoardEdgeActions(
                onOffline = onOffline,
                    onLowCredits = null,
                onSafety = onSafety
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun Workspace(
    onCrop: () -> Unit,
    onAdjust: () -> Unit,
    onFilters: () -> Unit,
    onText: () -> Unit,
    onMore: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    var rev by remember { mutableIntStateOf(BoardEditorDocument.revision) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        BoardEditorDocument.ensureLoaded(context)
        rev = BoardEditorDocument.revision
    }
    val canUndo = BoardEditorDocument.canUndo
    val canRedo = BoardEditorDocument.canRedo
    BoardWideFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TestTags.FLOW15_WORKSPACE)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PixiSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                IconButton(
                    onClick = {
                        BoardEditorDocument.undo()
                        rev = BoardEditorDocument.revision
                    },
                    enabled = canUndo
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_undo),
                        contentDescription = stringResource(R.string.cd_undo),
                        tint = if (canUndo) colors.textPrimary else colors.textSecondary.copy(alpha = 0.45f)
                    )
                }
                IconButton(
                    onClick = {
                        BoardEditorDocument.redo()
                        rev = BoardEditorDocument.revision
                    },
                    enabled = canRedo
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_redo),
                        contentDescription = stringResource(R.string.cd_redo),
                        tint = if (canRedo) colors.textPrimary else colors.textSecondary.copy(alpha = 0.45f)
                    )
                }
                IconButton(onClick = {
                    BoardEditorDocument.resetAdjust(push = true)
                    rev = BoardEditorDocument.revision
                }) {
                    Icon(painter = painterResource(R.drawable.pj_ic_refresh),
                        contentDescription = stringResource(R.string.cd_reset),
                        tint = colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = onSave,
                    modifier = Modifier.testTag(TestTags.FLOW15_SAVE)
                ) {
                    Text(stringResource(R.string.flow15_save), color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                if (canUndo || canRedo) {
                    stringResource(R.string.board_f15_history_ready)
                } else {
                    stringResource(R.string.board_f15_history_hint)
                },
                modifier = Modifier.padding(horizontal = PixiSpacing.xl),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = PixiSpacing.xl)
            ) {
                // rev forces recomposition after undo/redo
                @Suppress("UNUSED_EXPRESSION")
                rev
                BoardSafeResultImage(
                    bitmap = BoardEditorDocument.workingBitmap,
                    fallbackRes = BoardFlow15Session.hero(),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(BoardTheme.radii.card)),
                    contentScale = ContentScale.Crop,
                    colorFilter = BoardColorAdjust.lookColorFilter(
                        BoardFlow15Session.filterIndex,
                        BoardFlow15Session.filterCategory
                    ) ?: BoardEditorDocument.previewColorFilter()
                )
                // Apply grade as overlay filter via AndroidView-less: bake shown separately in Adjust
                if (BoardFlow15Session.textOverlay.isNotBlank()) {
                    Text(
                        BoardFlow15Session.textOverlay,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(PixiSpacing.lg),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Cursive,
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(Color.Black, blurRadius = 6f),
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixiSpacing.md),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    Triple(R.drawable.pj_ic_crop, R.string.flow15_crop, onCrop to TestTags.FLOW15_CROP),
                    Triple(R.drawable.pj_ic_tune, R.string.flow15_adjust, onAdjust to TestTags.FLOW15_ADJUST),
                    Triple(R.drawable.pj_ic_filter, R.string.flow15_filters, onFilters to TestTags.FLOW15_FILTERS),
                    Triple(R.drawable.pj_ic_sparkles, R.string.board_f15_effects, onFilters to TestTags.FLOW15_FILTERS),
                    Triple(R.drawable.pj_ic_more_horiz, R.string.flow15_more, onMore to TestTags.FLOW15_MORE)
                ).forEach { (icon, label, actionAndTag) ->
                    val (action, tag) = actionAndTag
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .testTag(tag)
                            .clickable(onClick = action)
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
            BoardOutlinedButton(
                text = stringResource(R.string.flow15_text),
                onClick = onText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PixiSpacing.xl, vertical = PixiSpacing.sm)
                    .testTag(TestTags.FLOW15_TEXT)
            )
        }
    }
}

@Composable
private fun CropRotate(onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    var aspect by remember { mutableIntStateOf(BoardFlow15Session.aspectIndex) }
    var rev by remember { mutableIntStateOf(BoardEditorDocument.revision) }
    var applying by remember { mutableStateOf(false) }
    var crop by remember { mutableStateOf(BoardCropNorm.Full) }
    // Free · 1:1 · 4:5 · 9:16 · 16:9
    val ratios = listOf("Free", "1:1", "4:5", "9:16", "16:9")
    fun aspectParts(index: Int): Pair<Float, Float>? = when (index.coerceIn(0, 4)) {
        1 -> 1f to 1f
        2 -> 4f to 5f
        3 -> 9f to 16f
        4 -> 16f to 9f
        else -> null
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        BoardEditorDocument.ensureLoaded(context)
        rev = BoardEditorDocument.revision
    }
    androidx.compose.runtime.LaunchedEffect(aspect, rev) {
        val src = BoardEditorDocument.workingBitmap
        val parts = aspectParts(aspect)
        crop = BoardCropNorm.fit(src?.width ?: 1, src?.height ?: 1, parts?.first, parts?.second)
    }
    fun applyCropAndDone() {
        if (applying) return
        applying = true
        scope.launch {
            try {
                BoardEditorDocument.ensureLoaded(context)
                val src = BoardEditorDocument.workingBitmap
                val win = crop.coerced()
                if (src != null && !src.isRecycled) {
                    val pipe = runCatching {
                        dagger.hilt.android.EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            com.pixijoy.ai.di.BoardAiEntryPoint::class.java
                        ).createExtraPipeline()
                    }.getOrNull()
                    val cropped = withContext(Dispatchers.Default) {
                        pipe?.cropNormalized(src, win.left, win.top, win.width, win.height) ?: src
                    }
                    BoardEditorDocument.setWorkingBitmap(cropped, push = true)
                    rev = BoardEditorDocument.revision
                }
                BoardFlow15Session.aspectIndex = aspect
                onDone()
            } finally {
                applying = false
            }
        }
    }
    BoardWideFrame {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PixiSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { applyCropAndDone() }) {
                    Text(stringResource(R.string.flow15_save), color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(horizontal = PixiSpacing.sm, vertical = PixiSpacing.xs)
            ) {
                @Suppress("UNUSED_EXPRESSION")
                rev
                val src = BoardEditorDocument.workingBitmap
                BoardCropCanvas(
                    bitmap = src,
                    fallbackRes = BoardFlowArt.editorHero,
                    crop = crop,
                    onCropChange = { crop = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(radii.card))
                )
            }
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = PixiSpacing.xl),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                ratios.forEachIndexed { i, label ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (i == aspect) colors.primary else colors.surfaceMuted)
                            .clickable {
                                aspect = i
                                BoardFlow15Session.aspectIndex = i
                            }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            label,
                            color = if (i == aspect) colors.onPrimary else colors.textSecondary
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixiSpacing.lg),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        BoardEditorDocument.rotate90(push = true)
                        rev = BoardEditorDocument.revision
                    }
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_rotate), null, tint = colors.primary)
                    Text(stringResource(R.string.board_f15_rotate), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        BoardEditorDocument.flip(horizontal = true, push = true)
                        rev = BoardEditorDocument.revision
                    }
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_flip), null, tint = colors.primary)
                    Text(stringResource(R.string.board_f15_flip_h), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        BoardEditorDocument.flip(horizontal = false, push = true)
                        rev = BoardEditorDocument.revision
                    }
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_flip), null, tint = colors.primary)
                    Text(stringResource(R.string.board_f15_flip_v), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f15_apply),
                onClick = { applyCropAndDone() },
                modifier = Modifier
                    .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                    .testTag(TestTags.FLOW15_APPLY)
            )
        }
    }
}

@Composable
private fun AdjustEnhance(onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    var panel by remember { mutableIntStateOf(0) }
    var brightness by remember { mutableFloatStateOf(BoardFlow15Session.brightness) }
    var contrast by remember { mutableFloatStateOf(BoardFlow15Session.contrast) }
    var saturation by remember { mutableFloatStateOf(BoardFlow15Session.saturation) }
    var warmth by remember { mutableFloatStateOf(BoardFlow15Session.warmth) }
    var sharpness by remember { mutableFloatStateOf(BoardFlow15Session.sharpness) }
    var vignette by remember { mutableFloatStateOf(BoardFlow15Session.vignette) }
    var grain by remember { mutableFloatStateOf(BoardFlow15Session.grain) }
    var hue by remember { mutableFloatStateOf(BoardFlow15Session.hue) }
    var lightness by remember { mutableFloatStateOf(BoardFlow15Session.lightness) }
    var shadows by remember { mutableFloatStateOf(BoardFlow15Session.shadows) }
    var midtones by remember { mutableFloatStateOf(BoardFlow15Session.midtones) }
    var highlights by remember { mutableFloatStateOf(BoardFlow15Session.highlights) }
    var showAfter by remember { mutableStateOf(BoardFlow15Session.showAfter) }
    var applying by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        BoardEditorDocument.ensureLoaded(context)
    }
    fun persist() {
        BoardFlow15Session.brightness = brightness
        BoardFlow15Session.contrast = contrast
        BoardFlow15Session.saturation = saturation
        BoardFlow15Session.warmth = warmth
        BoardFlow15Session.sharpness = sharpness
        BoardFlow15Session.vignette = vignette
        BoardFlow15Session.grain = grain
        BoardFlow15Session.hue = hue
        BoardFlow15Session.lightness = lightness
        BoardFlow15Session.shadows = shadows
        BoardFlow15Session.midtones = midtones
        BoardFlow15Session.highlights = highlights
        BoardEditorDocument.brightness = brightness
        BoardEditorDocument.contrast = contrast
        BoardEditorDocument.saturation = saturation
        BoardEditorDocument.warmth = warmth
        BoardEditorDocument.sharpness = sharpness
        BoardEditorDocument.vignette = vignette
        BoardEditorDocument.grain = grain
        BoardEditorDocument.hue = hue
        BoardEditorDocument.lightness = lightness
        BoardEditorDocument.shadows = shadows
        BoardEditorDocument.midtones = midtones
        BoardEditorDocument.highlights = highlights
    }
    fun applyAndDone() {
        if (applying) return
        applying = true
        scope.launch {
            try {
                persist()
                val needStructure =
                    kotlin.math.abs(sharpness - BoardColorAdjust.Neutral) > 0.05f ||
                        vignette > 0.5f ||
                        grain > 0.5f
                if (needStructure) {
                    BoardEditorDocument.ensureLoaded(context)
                    val src = BoardEditorDocument.workingBitmap
                    if (src != null && !src.isRecycled) {
                        val pipe = runCatching {
                            dagger.hilt.android.EntryPointAccessors.fromApplication(
                                context.applicationContext,
                                com.pixijoy.ai.di.BoardAiEntryPoint::class.java
                            ).createExtraPipeline()
                        }.getOrNull()
                        val structured = withContext(Dispatchers.Default) {
                            pipe?.applyStructure(
                                src,
                                sharpness = sharpness,
                                vignette = (vignette / 20f).coerceIn(0f, 1f),
                                grain = (grain / 20f).coerceIn(0f, 1f)
                            ) ?: src
                        }
                        BoardEditorDocument.setWorkingBitmap(structured, push = true)
                    } else {
                        BoardEditorDocument.pushHistory()
                    }
                } else {
                    BoardEditorDocument.pushHistory()
                }
                val engine = runCatching {
                    dagger.hilt.android.EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.pixijoy.ai.di.BoardAiEntryPoint::class.java
                    ).advancedEditEngine()
                }.getOrNull()
                BoardEditorDocument.bakePreviewGrade(engine)
                brightness = BoardEditorDocument.brightness
                contrast = BoardEditorDocument.contrast
                saturation = BoardEditorDocument.saturation
                warmth = BoardEditorDocument.warmth
                hue = BoardEditorDocument.hue
                lightness = BoardEditorDocument.lightness
                shadows = BoardEditorDocument.shadows
                midtones = BoardEditorDocument.midtones
                highlights = BoardEditorDocument.highlights
                persist()
                onDone()
            } finally {
                applying = false
            }
        }
    }
    val liveFilter = if (showAfter) {
        BoardColorAdjust.colorFilter(
            brightness, contrast, saturation, warmth,
            hue, lightness, shadows, midtones, highlights
        )
    } else {
        null
    }
    val previewBmp = remember(BoardEditorDocument.revision, BoardEditorDocument.workingBitmap) {
        BoardEditorDocument.workingBitmap?.let { src ->
            if (src.isRecycled) null else src.copy(Bitmap.Config.ARGB_8888, false)?.asImageBitmap()
        }
    }
    BoardWideFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PixiSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { applyAndDone() },
                    modifier = Modifier.testTag(TestTags.FLOW15_SAVE)
                ) {
                    Text(stringResource(R.string.flow15_save), color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                if (previewBmp != null) {
                    Image(
                        bitmap = previewBmp,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = liveFilter
                    )
                } else {
                    Image(
                        painter = painterResource(if (showAfter) BoardFlowArt.baAfter else BoardFlowArt.baBefore),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = liveFilter
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(PixiSpacing.sm)
                        .clip(RoundedCornerShape(radii.pill))
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    listOf(false to R.string.board_f15_before, true to R.string.board_f15_after).forEach { (after, label) ->
                        Text(
                            stringResource(label),
                            modifier = Modifier
                                .clickable {
                                    showAfter = after
                                    BoardFlow15Session.showAfter = after
                                }
                                .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                            color = if (showAfter == after) Color.White else Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                listOf(
                    R.string.board_f15_basic,
                    R.string.hsl_adjust,
                    R.string.curves_adjust
                ).forEachIndexed { i, label ->
                    Text(
                        stringResource(label),
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (panel == i) colors.primary else colors.surfaceMuted)
                            .clickable { panel = i }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                        color = if (panel == i) colors.onPrimary else colors.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            when (panel) {
                1 -> {
                    SliderRow(R.string.board_f15_hue, hue) { hue = it }
                    SliderRow(R.string.board_f15_saturation, saturation) { saturation = it }
                    SliderRow(R.string.board_f15_lightness, lightness) { lightness = it }
                }
                2 -> {
                    SliderRow(R.string.board_f15_shadows, shadows) { shadows = it }
                    SliderRow(R.string.board_f15_midtones, midtones) { midtones = it }
                    SliderRow(R.string.board_f15_highlights, highlights) { highlights = it }
                }
                else -> {
                    SliderRow(R.string.board_f15_brightness, brightness) { brightness = it }
                    SliderRow(R.string.board_f15_contrast, contrast) { contrast = it }
                    SliderRow(R.string.board_f15_saturation, saturation) { saturation = it }
                    SliderRow(R.string.board_f15_warmth, warmth) { warmth = it }
                    SliderRow(R.string.board_f15_sharpness, sharpness) { sharpness = it }
                    SliderRow(R.string.board_f15_vignette, vignette) { vignette = it }
                    SliderRow(R.string.board_f15_grain, grain) { grain = it }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f15_apply),
                onClick = { applyAndDone() },
                modifier = Modifier.testTag(TestTags.FLOW15_APPLY)
            )
        }
    }
}

@Composable
private fun FiltersEffects(onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    var category by remember { mutableIntStateOf(BoardFlow15Session.filterCategory) }
    var filter by remember { mutableIntStateOf(BoardFlow15Session.filterIndex) }
    var rev by remember { mutableIntStateOf(BoardEditorDocument.revision) }
    var applying by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        BoardEditorDocument.ensureLoaded(context)
        rev = BoardEditorDocument.revision
    }
    val cats = listOf(
        R.string.flow15_filters,
        R.string.board_f15_effects,
        R.string.board_f15_color,
        R.string.board_f15_artistic
    )
    val filters = listOf(
        R.string.board_f15_filter_none to BoardFlowArt.editorHero,
        R.string.board_f15_filter_vivid to BoardFlowArt.baAfter,
        R.string.board_f15_filter_warm to BoardFlowArt.beautifulHero,
        R.string.board_f15_filter_cool to BoardFlowArt.creationThumb,
        R.string.board_f15_filter_bw to BoardFlowArt.differentHero,
        R.string.board_f15_filter_vintage to BoardFlowArt.funnyHero,
        R.string.board_f15_filter_cinema to BoardFlowArt.trendPortrait,
        R.string.board_f15_filter_dream to BoardFlowArt.stickerHero
    )
    val lookFilter = BoardColorAdjust.lookColorFilter(filter, category)
    val previewBmp = remember(rev, BoardEditorDocument.workingBitmap) {
        BoardEditorDocument.workingBitmap?.let { src ->
            if (src.isRecycled) null else src.copy(Bitmap.Config.ARGB_8888, false)?.asImageBitmap()
        }
    }
    fun applyLookAndDone() {
        if (applying) return
        applying = true
        scope.launch {
            try {
                BoardEditorDocument.ensureLoaded(context)
                val src = BoardEditorDocument.workingBitmap
                if (src != null && !src.isRecycled) {
                    val pipe = runCatching {
                        dagger.hilt.android.EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            com.pixijoy.ai.di.BoardAiEntryPoint::class.java
                        ).createExtraPipeline()
                    }.getOrNull()
                    val baked = withContext(Dispatchers.Default) {
                        pipe?.applyLook(src, filter, category) ?: src
                    }
                    BoardEditorDocument.pushHistory()
                    BoardEditorDocument.setWorkingBitmap(baked, push = false)
                    BoardFlow15Session.filterIndex = 0
                    BoardFlow15Session.filterCategory = 0
                    rev = BoardEditorDocument.revision
                } else {
                    BoardFlow15Session.filterIndex = filter
                }
                onDone()
            } finally {
                applying = false
            }
        }
    }
    BoardWideFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PixiSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { applyLookAndDone() }) {
                    Text(stringResource(R.string.flow15_save), color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
            if (previewBmp != null) {
                Image(
                    bitmap = previewBmp,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(radii.card)),
                    contentScale = ContentScale.Crop,
                    colorFilter = lookFilter
                )
            } else {
                Image(
                    painter = painterResource(filters[filter % filters.size].second),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(radii.card)),
                    contentScale = ContentScale.Crop,
                    colorFilter = lookFilter
                )
            }
            Text(
                stringResource(R.string.board_f15_looks_pack),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Text(
                stringResource(R.string.board_f15_looks_pack_sub),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                cats.forEachIndexed { i, res ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (i == category) colors.primary else colors.surfaceMuted)
                            .clickable {
                                category = i
                                BoardFlow15Session.filterCategory = i
                            }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(res),
                            color = if (i == category) colors.onPrimary else colors.textSecondary
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                filters.forEachIndexed { i, (label, res) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            filter = i
                            BoardFlow15Session.filterIndex = i
                        }
                    ) {
                        Image(
                            painter = painterResource(res),
                            contentDescription = stringResource(label),
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(radii.md))
                                .border(
                                    width = if (i == filter) 2.dp else 0.dp,
                                    color = colors.primary,
                                    shape = RoundedCornerShape(radii.md)
                                ),
                            contentScale = ContentScale.Crop,
                            colorFilter = BoardColorAdjust.lookColorFilter(i, category)
                        )
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (i == filter) colors.primary else colors.textSecondary
                        )
                    }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f15_apply),
                onClick = { applyLookAndDone() },
                modifier = Modifier.testTag(TestTags.FLOW15_APPLY)
            )
        }
    }
}

@Composable
private fun TextStickers(onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    var tool by remember { mutableIntStateOf(BoardFlow15Session.textTool) }
    var text by remember { mutableStateOf(BoardFlow15Session.textOverlay) }
    var fontIndex by remember { mutableIntStateOf(BoardFlow15Session.fontIndex) }
    var colorIndex by remember { mutableIntStateOf(BoardFlow15Session.colorIndex) }
    var alignIndex by remember { mutableIntStateOf(BoardFlow15Session.alignIndex) }
    var rev by remember { mutableIntStateOf(BoardEditorDocument.revision) }
    var applying by remember { mutableStateOf(false) }
    val tools = listOf(
        R.string.flow15_text,
        R.string.board_f15_stickers,
        R.string.board_f15_doodle,
        R.string.board_f15_shapes
    )
    val fontLabels = listOf("Default", "Sans", "Serif", "Mono")
    val colorArgb = listOf(
        android.graphics.Color.WHITE,
        android.graphics.Color.YELLOW,
        android.graphics.Color.rgb(255, 105, 180),
        android.graphics.Color.CYAN,
        android.graphics.Color.BLACK
    )
    val previewColors = listOf(Color.White, Color.Yellow, Color(0xFFFF69B4), Color.Cyan, Color.Black)
    LaunchedEffect(Unit) {
        BoardEditorDocument.ensureLoaded(context)
        rev = BoardEditorDocument.revision
    }
    fun bakeAndDone() {
        if (applying) return
        applying = true
        scope.launch {
            try {
                BoardFlow15Session.textOverlay = text
                BoardFlow15Session.fontIndex = fontIndex
                BoardFlow15Session.colorIndex = colorIndex
                BoardFlow15Session.alignIndex = alignIndex
                BoardEditorDocument.ensureLoaded(context)
                val src = BoardEditorDocument.workingBitmap
                if (src != null && !src.isRecycled && text.isNotBlank()) {
                    val pipe = runCatching {
                        dagger.hilt.android.EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            com.pixijoy.ai.di.BoardAiEntryPoint::class.java
                        ).createExtraPipeline()
                    }.getOrNull()
                    val baked = withContext(Dispatchers.Default) {
                        pipe?.bakeTextOverlay(
                            bitmap = src,
                            text = text,
                            colorArgb = colorArgb[colorIndex.coerceIn(0, colorArgb.lastIndex)],
                            align = alignIndex.coerceIn(0, 2),
                            fontStyle = fontIndex.coerceIn(0, 3),
                            scale = 1f,
                            verticalBias = 0.5f
                        ) ?: src
                    }
                    BoardEditorDocument.setWorkingBitmap(baked, push = true)
                    rev = BoardEditorDocument.revision
                } else {
                    BoardEditorDocument.textOverlay = text
                }
                onDone()
            } finally {
                applying = false
            }
        }
    }
    BoardWideFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PixiSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { bakeAndDone() }) {
                    Text(stringResource(R.string.flow15_save), color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                @Suppress("UNUSED_EXPRESSION")
                rev
                BoardSafeResultImage(
                    bitmap = BoardEditorDocument.workingBitmap,
                    fallbackRes = BoardFlowArt.editorHero,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text.ifBlank { " " },
                    modifier = Modifier
                        .align(
                            when (alignIndex) {
                                0 -> Alignment.CenterStart
                                2 -> Alignment.CenterEnd
                                else -> Alignment.Center
                            }
                        )
                        .padding(PixiSpacing.lg),
                    style = TextStyle(
                        color = previewColors[colorIndex.coerceIn(0, previewColors.lastIndex)],
                        fontSize = 28.sp,
                        fontFamily = when (fontIndex) {
                            1 -> FontFamily.SansSerif
                            2 -> FontFamily.Serif
                            3 -> FontFamily.Monospace
                            else -> FontFamily.Cursive
                        },
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(Color.Black, blurRadius = 8f),
                        textAlign = when (alignIndex) {
                            0 -> TextAlign.Start
                            2 -> TextAlign.End
                            else -> TextAlign.Center
                        }
                    )
                )
                Text("👑", modifier = Modifier.align(Alignment.TopEnd).padding(PixiSpacing.md), fontSize = 28.sp)
                Text("❤️", modifier = Modifier.align(Alignment.BottomStart).padding(PixiSpacing.md), fontSize = 28.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tools.forEachIndexed { i, res ->
                    Text(
                        stringResource(res),
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (i == tool) colors.primarySoft else Color.Transparent)
                            .clickable {
                                tool = i
                                BoardFlow15Session.textTool = i
                            }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                        color = if (i == tool) colors.primary else colors.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (tool == 0) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        BoardFlow15Session.textOverlay = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.flow15_text)) },
                    shape = RoundedCornerShape(radii.md)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    fontLabels.forEachIndexed { i, label ->
                        Text(
                            label,
                            modifier = Modifier
                                .clip(RoundedCornerShape(radii.pill))
                                .background(if (i == fontIndex) colors.primary else colors.surfaceMuted)
                                .clickable {
                                    fontIndex = i
                                    BoardFlow15Session.fontIndex = i
                                }
                                .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                            color = if (i == fontIndex) colors.onPrimary else colors.textSecondary
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    previewColors.forEachIndexed { i, c ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (i == colorIndex) 2.dp else 1.dp,
                                    color = if (i == colorIndex) colors.primary else colors.textSecondary,
                                    shape = CircleShape
                                )
                                .clickable {
                                    colorIndex = i
                                    BoardFlow15Session.colorIndex = i
                                }
                        )
                    }
                    listOf("L", "C", "R").forEachIndexed { i, label ->
                        Text(
                            label,
                            modifier = Modifier
                                .clip(RoundedCornerShape(radii.pill))
                                .background(if (i == alignIndex) colors.primary else colors.surfaceMuted)
                                .clickable {
                                    alignIndex = i
                                    BoardFlow15Session.alignIndex = i
                                }
                                .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                            color = if (i == alignIndex) colors.onPrimary else colors.textSecondary
                        )
                    }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f15_apply),
                onClick = { bakeAndDone() },
                modifier = Modifier.testTag(TestTags.FLOW15_APPLY)
            )
        }
    }
}

@Composable
private fun MoreTools(onPick: (String) -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val isExpanded = rememberWindowWidthSizeClass() == WindowWidthSizeClass.Expanded
    val tools = listOf(
        R.drawable.pj_ic_crop to R.string.flow15_crop to "b15_crop",
        R.drawable.pj_ic_tune to R.string.flow15_adjust to "b15_adjust",
        R.drawable.pj_ic_filter to R.string.flow15_filters to "b15_filters",
        R.drawable.pj_ic_text to R.string.flow15_text to "b15_text",
        R.drawable.pj_ic_magic to R.string.board_f15_retouch to "b15_heal",
        R.drawable.pj_ic_landscape to R.string.create_extra_remove_bg to "b_c04",
        R.drawable.pj_ic_magic to R.string.create_stickers to "b13_photo",
        R.drawable.pj_ic_sparkles to R.string.create_extra_restore to "b_c05",
        R.drawable.pj_ic_crop to R.string.create_extra_expand to "b_c10",
        R.drawable.pj_ic_palette to R.string.board_f17_export_share to "b17_open"
    )
    val toolsPane: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            BoardScreenHeader(stringResource(R.string.flow15_more_title), onBack)
            BoardTipCallout(
                title = stringResource(R.string.board_f15_more_tools_title),
                body = stringResource(R.string.board_f15_more_tools_tip)
            )
            tools.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEach { (pair, dest) ->
                        val (icon, label) = pair
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(radii.card))
                                .background(colors.surfaceMuted)
                                .then(
                                    if (dest == "b15_heal") Modifier.testTag(TestTags.FLOW15_HEAL)
                                    else Modifier
                                )
                                .clickable { onPick(dest) }
                                .padding(PixiSpacing.md),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(painter = painterResource(icon), null, tint = colors.primary)
                            Text(
                                stringResource(label),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
    BoardWideFrame {
        BoardListDetail(
            showDetail = isExpanded,
            list = {
                if (isExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(PixiSpacing.xl)
                            .clip(RoundedCornerShape(radii.card))
                    ) {
                        BoardSessionPhoto(
                            fallbackRes = BoardFlowArt.editorHero,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    toolsPane()
                }
            },
            detail = toolsPane
        )
    }
}

@Composable
private fun SaveShare(
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onRemake: () -> Unit,
    onPublish: () -> Unit,
    onStickers: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            BoardScreenHeader(stringResource(R.string.flow15_ready), onBack)
            Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(48.dp))
            BoardSafeResultImage(
                bitmap = BoardEditorDocument.workingBitmap,
                fallbackRes = BoardFlow15Session.hero(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card))
            )
            BoardResultFooter(
                onSave = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            val bmp = BoardEditorDocument.workingBitmap
                            if (bmp != null && !bmp.isRecycled) {
                                BoardDrawableIo.saveBitmapToGallery(
                                    context,
                                    bmp,
                                    filePrefix = "PixiJoy_Edit"
                                )
                            } else {
                                saveDrawableToGallery(context, BoardFlow15Session.hero())
                            }
                        }
                        Toast.makeText(
                            context,
                            context.getString(
                                if (ok) R.string.board_f15_saved else R.string.board_f15_save_failed
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onShare = { shareDrawable(context, BoardFlow15Session.hero()) },
                onEdit = onEdit,
                onExport = onExport,
                onRemake = onRemake,
                onPublish = onPublish,
                onStickers = onStickers
            )
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.board_ct_done), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun SliderRow(@StringRes label: Int, value: Float, onChange: (Float) -> Unit) {
    val colors = BoardTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(label),
            modifier = Modifier.weight(0.35f),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodySmall
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..20f,
            modifier = Modifier.weight(0.65f)
        )
    }
}

private fun saveDrawableToGallery(context: Context, @DrawableRes resId: Int): Boolean =
    BoardDrawableIo.saveToGallery(context, resId, filePrefix = "PixiJoy_Edit")

private fun shareDrawable(context: Context, @DrawableRes resId: Int) {
    BoardDrawableIo.shareAsync(
        context = context,
        resId = resId,
        textRes = R.string.board_f15_share_text,
        filePrefix = "pixijoy_edit"
    )
}

