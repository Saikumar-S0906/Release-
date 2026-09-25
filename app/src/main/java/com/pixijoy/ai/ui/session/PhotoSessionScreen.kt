package com.pixijoy.ai.ui.session

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.zIndex
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.ProjectRenameDialog
import com.pixijoy.ai.ui.StudioShellViewModel
import com.pixijoy.ai.export.MovieExportWorker
import com.pixijoy.ai.ui.isStudioTwoPane
import com.pixijoy.ai.export.PhotoExportWorker
import com.pixijoy.core.common.AppResult
import com.pixijoy.core.data.Project
import com.pixijoy.core.media.RawPreview
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiError
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiSecondaryButton
import com.pixijoy.designsystem.PixiSuccess
import com.pixijoy.designsystem.PixiSurface
import com.pixijoy.designsystem.PixiSurface2
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.photo.BundledLooks
import com.pixijoy.photo.CubeLutParser
import com.pixijoy.photo.ImportedLuts
import com.pixijoy.photo.LutParseResult
import com.pixijoy.photo.BlendMode
import com.pixijoy.photo.ExportFormat
import com.pixijoy.photo.ExportFormatSupport
import com.pixijoy.photo.LayeredOp
import com.pixijoy.photo.LiveGrade
import com.pixijoy.photo.MaskFill
import com.pixijoy.photo.MaskStroke
import com.pixijoy.photo.PhotoOp
import com.pixijoy.photo.PhotoPreset
import com.pixijoy.photo.SelectiveHslRange
import com.pixijoy.photo.SkyReplacePresets
import com.pixijoy.photo.SkyReplaceTarget
import com.pixijoy.photo.isIdentity
import com.pixijoy.photo.Stamp
import com.pixijoy.photo.decodeLayers
import com.pixijoy.photo.histogramClip
import com.pixijoy.photo.encodeLayers
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.launch

@Composable
fun PhotoSessionScreen(
    project: Project,
    toolLabel: String,
    toolKey: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shell = hiltViewModel<StudioShellViewModel>()
    val repo = shell.repository
    val engine = shell.photoEngine
    val sessionViewModel: PhotoSessionViewModel =
        hiltViewModel<HiltPhotoSessionViewModel, HiltPhotoSessionViewModel.Factory>(
            key = "photo-session-${project.id}",
            creationCallback = { factory ->
                factory.create(
                    initialGraph = project.graph,
                    projectId = project.id,
                    toolKey = toolKey
                )
            }
        )
    val toolEntry = remember(project.id, toolKey) { Any() }
    sessionViewModel.syncToolChrome(toolKey, toolEntry)
    val sessionState by sessionViewModel.state.collectAsState()
    val saveStatus by sessionViewModel.saveStatus.collectAsState()
    var renaming by remember { mutableStateOf(false) }
    val layers = sessionState.layers
    val redo = sessionState.redo
    FlushSessionOnStop(sessionViewModel::flushInBackground)
    PredictiveBackHandler {
        it.collect { }
        runCatching { sessionViewModel.flush() }
        onBack()
    }
    fun leaveSession() {
        scope.launch {
            runCatching { sessionViewModel.flush() }
            onBack()
        }
    }
    val projectDir = remember(project.id) { repo.projectDir(project.id) }
    LaunchedEffect(projectDir) {
        engine.lutDirectory = projectDir
    }
    val sources = remember(project.media, project.coverPath) {
        val raw = project.media.firstOrNull {
            RawPreview.looksLikeRaw(it.path) || RawPreview.isRawMime(it.mime)
        }
        when {
            raw != null && project.coverPath.endsWith("preview.jpg") ->
                listOf(project.coverPath)
            else -> project.media.map { it.path }.ifEmpty { listOf(project.coverPath) }
        }
    }
    val isRawProject = remember(project.media) {
        project.media.any { RawPreview.looksLikeRaw(it.path) || RawPreview.isRawMime(it.mime) }
    }
    var selectedLayer by sessionViewModel.selectedLayerState
    var mode by sessionViewModel.toolModeState
    var group by sessionViewModel.toolGroupState
    val sessionControls = sessionViewModel.controls
    with(sessionControls) {
    val visionAvailable = engine.visionAvailable
    LaunchedEffect(Unit) {
        presets = repo.listPresetFiles().mapNotNull { file ->
            runCatching { PhotoPreset.decode(file.readText()) }.getOrNull()
        }
    }
    val comparing = sessionViewModel.preview.comparing

    fun reportStatusError(message: String) {
        status = message
        statusIsError = true
    }

    fun push(op: PhotoOp) {
        sessionViewModel.pushOp(op)
    }

    val addPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                when (val result = repo.addUri(project.id, uri)) {
                    is AppResult.Err -> reportStatusError(result.message)
                    is AppResult.Ok -> {
                        if (layers.none { it.op is PhotoOp.Collage }) {
                            push(PhotoOp.Collage(2))
                        }
                    }
                }
            }
        }
    }

    val pickLut = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val name = uri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')
                ?: "import.cube"
            when (val copied = repo.importLut(project.id, uri, name)) {
                is AppResult.Err -> reportStatusError(copied.message)
                is AppResult.Ok -> {
                    when (val parsed = CubeLutParser.parseFile(copied.value, maxSize = 33)) {
                        is LutParseResult.Err -> {
                            copied.value.delete()
                            reportStatusError(parsed.message)
                        }
                        is LutParseResult.Ok -> {
                            val key = ImportedLuts.keyFor(copied.value.name)
                            val label = parsed.lut.title ?: copied.value.name
                            sessionViewModel.preview.acceptImportedLut(
                                key,
                                label,
                                context.getString(R.string.studio_lut_imported, label)
                            )
                            sessionViewModel.selectMode(SessionMode.Looks)
                        }
                    }
                }
            }
        }
    }

    PhotoPreviewEffects(
        viewModel = sessionViewModel,
        projectId = project.id,
        sources = sources,
        layers = layers,
        projectDir = projectDir,
        renderLayers = engine::renderLayers
    )
    val hasLiveDelta = sessionViewModel.preview.hasLiveDelta(mode)
    val canCompare = sessionViewModel.preview.canCompare(mode, layers)
    val workManager = remember { WorkManager.getInstance(context) }

    val configuration = LocalConfiguration.current
    val sideTools = isStudioTwoPane(configuration.screenWidthDp, configuration.screenHeightDp)
    val haptics = LocalHapticFeedback.current
    @Composable
    fun ToolsChrome(sideDock: Boolean, modifier: Modifier = Modifier) {
        StudioChromeBar(
            modifier = modifier,
            sideDock = sideDock,
            panelScrollKey = sessionViewModel.chromeEpoch to group to mode,
            rail = {
                StudioGroupRail(
                    items = photoGroupItems(),
                    selectedTag = group.tag,
                    onSelect = { tag ->
                        sessionViewModel.selectGroup(
                            PhotoGroup.entries.first { it.tag == tag },
                            toolKey
                        )
                    }
                )
            },
            panel = {
            key(sessionViewModel.chromeEpoch) {
            ToolBody(
            controls = sessionControls,
            mode = mode,
            group = group,
            toolKey = toolKey,
            onMode = sessionViewModel::selectMode,
            onRotate = { push(PhotoOp.Rotate(90)) },
            onFlip = { push(PhotoOp.FlipH) },
            exposure = exposure,
            contrast = contrast,
            hue = hue,
            saturation = saturation,
            expand = expand,
            brush = brush,
            text = text,
            onExposure = sessionViewModel.preview::setExposure,
            onContrast = sessionViewModel.preview::setContrast,
            onHue = sessionViewModel.preview::setHue,
            onSaturation = sessionViewModel.preview::setSaturation,
            onExpand = { expand = it },
            onBrush = { brush = it },
            onText = { text = it },
            onApplyGrade = { sessionViewModel.applyGrade() },
            sharpen = sharpen,
            vignette = vignette,
            blurRadius = blurRadius,
            onSharpenAmount = sessionViewModel.preview::setSharpen,
            onVignetteAmount = sessionViewModel.preview::setVignette,
            onBlurAmount = sessionViewModel.preview::setBlur,
            onApplyCrop = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                push(crop); sessionViewModel.selectMode(defaultMode(toolKey))
            },
            onApplyExpand = { push(PhotoOp.Expand(expand)) },
            onApplySkyReplace = { target, color -> push(PhotoOp.SkyReplace(target, color)) },
            onApplyHeal = { if (pendingStamps.isNotEmpty()) push(PhotoOp.Heal(pendingStamps)) },
            onApplyFill = { neural ->
                if (pendingStamps.isNotEmpty()) {
                    push(PhotoOp.Fill(pendingStamps, neural = neural))
                }
                pendingStamps = emptyList()
            },
            onUndoFillStroke = {
                if (pendingStamps.isNotEmpty()) pendingStamps = pendingStamps.dropLast(1)
                lastStamp = pendingStamps.lastOrNull()
            },
            onClearFillStrokes = { pendingStamps = emptyList(); lastStamp = null },
            onApplyPerspective = { h, v ->
                if (!PhotoOp.Perspective(h, v).isIdentity()) push(PhotoOp.Perspective(h, v))
            },
            onApplyLiquify = { x, y, r, a ->
                if (!PhotoOp.Liquify(x, y, r, a).isIdentity()) push(PhotoOp.Liquify(x, y, r, a))
            },
            onApplySoften = { a -> if (!PhotoOp.Soften(a).isIdentity()) push(PhotoOp.Soften(a)) },
            onApplySuperRes = { s -> if (!PhotoOp.SuperRes(s).isIdentity()) push(PhotoOp.SuperRes(s)) },
            onApplyDenoise = { a -> if (!PhotoOp.Denoise(a).isIdentity()) push(PhotoOp.Denoise(a)) },
            onSavePreset = { name ->
                scope.launch {
                    val id = "p${System.currentTimeMillis()}"
                    val preset = PhotoPreset(
                        id = id,
                        name = name.trim().ifBlank { "Preset" },
                        layersEncoded = layers.encodeLayers()
                    )
                    when (val r = repo.writePresetFile(id, preset.encode())) {
                        is AppResult.Ok -> {
                            presets = listOf(preset) + presets.filterNot { it.id == id }
                            shell.prefs.setLastLookPreset(id)
                            status = context.getString(R.string.studio_preset_save)
                            statusIsError = false
                        }
                        is AppResult.Err -> reportStatusError(r.message)
                    }
                }
            },
            onApplyPreset = { id ->
                scope.launch {
                    when (val r = repo.readPresetFile(id)) {
                        is AppResult.Ok -> {
                            val preset = PhotoPreset.decode(r.value)
                            if (preset == null) {
                                reportStatusError(context.getString(R.string.studio_preset_unreadable))
                            } else {
                                sessionViewModel.replaceLayers(
                                    preset.layersEncoded.decodeLayers().ifEmpty { layers }
                                )
                                status = context.getString(R.string.studio_presets)
                                statusIsError = false
                            }
                        }
                        is AppResult.Err -> reportStatusError(r.message)
                    }
                }
            },
            presets = presets,
            onApplyErase = { if (pendingStamps.isNotEmpty()) push(PhotoOp.Erase(pendingStamps)) },
            onApplyText = { push(PhotoOp.TextLayer(text, placeX, placeY, 0.08f, 0xFFFFFFFF.toInt())) },
            onRestore = { push(PhotoOp.Restore) },
            onCutout = { push(PhotoOp.Subject); format = ExportFormat.PNG },
            onApplyCurves = { sessionViewModel.applyCurves(it) },
            selectiveRange = selectiveRange,
            selectiveHue = selectiveHue,
            selectiveSat = selectiveSat,
            selectiveLight = selectiveLight,
            onSelectiveRange = sessionViewModel.preview::setSelectiveRange,
            onSelectiveHue = sessionViewModel.preview::setSelectiveHue,
            onSelectiveSat = sessionViewModel.preview::setSelectiveSat,
            onSelectiveLight = sessionViewModel.preview::setSelectiveLight,
            onApplySelectiveHsl = { sessionViewModel.applySelective() },
            curveYs = curveYs,
            onCurveYs = sessionViewModel.preview::setCurveYs,
            lookKey = lookKey,
            onLookKey = sessionViewModel.preview::setLookKey,
            onApplyLook = { sessionViewModel.applyLook(it) },
            onImportLut = {
                pickLut.launch(arrayOf("*/*"))
            },
            importedLookLabel = importedLookLabel,
            maskFill = (layers.firstOrNull { it.id == selectedLayer } ?: layers.lastOrNull())?.maskFill
                ?: MaskFill.None,
            maskReveal = maskReveal,
            onMaskReveal = { maskReveal = it },
            onClearMask = {
                val id = selectedLayer ?: layers.lastOrNull()?.id
                val layer = layers.firstOrNull { it.id == id }
                if (id != null && layer != null) {
                    lastMaskSnapshot = layer.mask to layer.maskFill
                    sessionViewModel.replaceLayers(
                        layers.map {
                            if (it.id == id) it.copy(mask = emptyList(), maskFill = MaskFill.None) else it
                        }
                    )
                }
            },
            onRestoreMask = {
                val snapshot = lastMaskSnapshot ?: return@ToolBody
                val id = selectedLayer ?: layers.lastOrNull()?.id ?: return@ToolBody
                sessionViewModel.replaceLayers(
                    layers.map {
                        if (it.id == id) it.copy(mask = snapshot.first, maskFill = snapshot.second) else it
                    }
                )
            },
            canRestoreMask = lastMaskSnapshot != null,
            visionAvailable = visionAvailable,
            onSubjectMask = {
                val id = selectedLayer ?: layers.lastOrNull()?.id ?: return@ToolBody
                sessionViewModel.replaceLayers(
                    layers.map {
                        if (it.id == id) it.copy(maskFill = MaskFill.Subject) else it
                    }
                )
            },
            onBackgroundMask = {
                val id = selectedLayer ?: layers.lastOrNull()?.id ?: return@ToolBody
                sessionViewModel.replaceLayers(
                    layers.map {
                        if (it.id == id) it.copy(maskFill = MaskFill.Background) else it
                    }
                )
            },
            collageCols = collageCols,
            pickingHealSource = pickingHealSource,
            healSourceSet = healSource != null,
            onPickHealSource = { pickingHealSource = true },
            onPaintHeal = { pickingHealSource = false },
            onSticker = { glyph -> push(PhotoOp.Sticker(glyph, placeX, placeY, 0.18f)) },
            onCollage = { cols ->
                collageCols = cols
                push(PhotoOp.Collage(cols))
                status = context.getString(
                    if (cols >= 4) R.string.studio_collage_4_hint else R.string.studio_collage_2_hint
                )
                statusIsError = false
            },
            onAddPhoto = {
                addPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
            if (group == PhotoGroup.Compose && mode == SessionMode.Layers && layers.isNotEmpty()) {
                LayerStack(
                    layers = layers,
                    selectedId = selectedLayer,
                    onSelect = { selectedLayer = it },
                    onChange = { sessionViewModel.replaceLayers(it) }
                )
            }
            }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixiBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("photo_session")
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
        StudioTopChrome(Modifier.align(Alignment.TopCenter).zIndex(2f)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val backLabel = stringResource(R.string.studio_back)
                TextButton(
                    onClick = ::leaveSession,
                    modifier = Modifier
                        .testTag("session_back")
                        .semantics { contentDescription = backLabel }
                ) {
                    Text(backLabel, color = PixiText, modifier = Modifier.clearAndSetSemantics { })
                }
                SessionProjectTitle(
                    title = project.title,
                    toolLabel = toolLabel,
                    onRename = { renaming = true },
                    modifier = Modifier.weight(1f)
                )
                if (isRawProject) {
                    Text(
                        stringResource(R.string.studio_raw_preview_badge),
                        color = PixiAccent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("session_raw_badge")
                    )
                }
                val exportLabel = stringResource(R.string.studio_export)
                TextButton(
                    onClick = { showExport = true },
                    enabled = !exporting && !busy,
                    modifier = Modifier
                        .testTag("session_export")
                        .semantics { contentDescription = exportLabel }
                ) {
                    Text(exportLabel, color = PixiText, modifier = Modifier.clearAndSetSemantics { })
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val undoLabel = stringResource(R.string.studio_undo)
                val redoLabel = stringResource(R.string.studio_redo)
                TextButton(
                    onClick = sessionViewModel::undo,
                    enabled = layers.isNotEmpty(),
                    modifier = Modifier
                        .testTag("session_undo")
                        .semantics { contentDescription = undoLabel }
                ) {
                    Text(undoLabel, color = PixiTextMuted, modifier = Modifier.clearAndSetSemantics { })
                }
                TextButton(
                    onClick = sessionViewModel::redo,
                    enabled = redo.isNotEmpty(),
                    modifier = Modifier
                        .testTag("session_redo")
                        .semantics { contentDescription = redoLabel }
                ) {
                    Text(redoLabel, color = PixiTextMuted, modifier = Modifier.clearAndSetSemantics { })
                }
                val compareLabel = stringResource(
                    when {
                        !canCompare -> R.string.session_compare_disabled
                        comparing -> R.string.session_compare_before
                        else -> R.string.session_compare_after_hold
                    }
                )
                val compareState = stringResource(
                    if (comparing) R.string.session_compare_state_before
                    else R.string.session_compare_state_after
                )
                val compareAction = stringResource(
                    if (comparing) R.string.session_compare_action_after
                    else R.string.session_compare_action_before
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (comparing) PixiAccent.copy(alpha = 0.9f)
                            else PixiSurface2.copy(alpha = if (canCompare) 0.85f else 0.45f)
                        )
                        .photoComparePress(canCompare) { compareControlHeld = it }
                        .testTag("session_compare")
                        .semantics {
                            contentDescription = compareLabel
                            role = Role.Button
                            stateDescription = compareState
                            if (canCompare) {
                                onClick(label = compareAction) {
                                    compareAccessible = !compareAccessible
                                    true
                                }
                            } else {
                                disabled()
                            }
                        }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = compareLabel,
                        color = if (canCompare) PixiText else PixiTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                }
                SessionAutosaveChip(
                    status = saveStatus,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PixiSurface)
                .padding(bottom = if (!sideTools && mode == SessionMode.Crop) 156.dp else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            val snap = compareSnapshot
            val bmp = if (comparing && hasLiveDelta) {
                preview
            } else if (comparing && snap != null && !snap.isRecycled) {
                snap
            } else {
                selectiveLivePreview ?: lutLivePreview ?: curvesLivePreview ?: preview
            }
            if (bmp != null && !bmp.isRecycled) {
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .clipToBounds()
                ) {
                    val viewportSize = Size(
                        constraints.maxWidth.toFloat(),
                        constraints.maxHeight.toFloat()
                    )
                    val imageRect = fittedImageRect(
                        viewportSize.width,
                        viewportSize.height,
                        bmp.width,
                        bmp.height
                    )
                    val imageSize = bmp.width to bmp.height
                    LaunchedEffect(imageSize, viewportSize) {
                        val imageChanged = viewportImageSize != imageSize
                        viewportImageSize = imageSize
                        viewportState = if (imageChanged) {
                            viewportState.reset()
                        } else {
                            viewportState.clamped(viewportSize, imageRect)
                        }
                    }
                    val showLiveFilters = !comparing
                    val liveAdjust = showLiveFilters && mode == SessionMode.Adjust &&
                        !LiveGrade.isAdjustIdentity(exposure, contrast, hue, saturation)
                    val liveDetail = showLiveFilters && mode == SessionMode.Adjust &&
                        (sharpen > 0.01f || vignette > 0.01f || blurRadius > 0.5f)
                    val liveCurves = showLiveFilters && mode == SessionMode.Curves && !LiveGrade.curvesIdentity(curveYs)
                    val liveLooks = showLiveFilters && mode == SessionMode.Looks &&
                        !ImportedLuts.isImported(lookKey) &&
                        !BundledLooks.isIdentity(lookKey)
                    val toolInputMode = modeUsesSingleFingerToolInput(mode)
                    val viewportGestureKey = listOf(
                        viewportSize.width,
                        viewportSize.height,
                        imageRect.width,
                        imageRect.height
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("session_viewport")
                            .photoViewportTransformGestures(
                                toolInputMode = toolInputMode,
                                gestureKey = viewportGestureKey
                            ) { centroid, pan, zoom ->
                                viewportState = viewportState.transform(
                                    centroid = centroid,
                                    panChange = pan,
                                    zoomChange = zoom,
                                    viewportSize = viewportSize,
                                    fittedImageRect = imageRect
                                )
                            }
                            .photoViewportPressGestures(
                                toolInputMode = toolInputMode,
                                compareEnabled = canCompare,
                                gestureKey = viewportGestureKey,
                                onCompareHeld = { compareCanvasHeld = it },
                                onDoubleTap = { viewportState = viewportState.reset() }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = viewportState.zoom
                                    scaleY = viewportState.zoom
                                    translationX = viewportState.pan.x
                                    translationY = viewportState.pan.y
                                }
                        ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = project.title,
                        contentScale = ContentScale.Fit,
                        colorFilter = when {
                            liveAdjust -> ColorFilter.colorMatrix(
                                ColorMatrix(LiveGrade.colorMatrix(exposure, contrast, hue, saturation))
                            )
                            liveLooks -> ColorFilter.colorMatrix(
                                ColorMatrix(BundledLooks.colorMatrix(lookKey))
                            )
                            else -> null
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                when {
                                    liveCurves && Build.VERSION.SDK_INT >= 33 && curvesShader != null ->
                                        Modifier.graphicsLayer {
                                            curvesShader.setFloatUniform("y0", curveYs[0])
                                            curvesShader.setFloatUniform("y1", curveYs[1])
                                            curvesShader.setFloatUniform("y2", curveYs[2])
                                            curvesShader.setFloatUniform("y3", curveYs[3])
                                            curvesShader.setFloatUniform("y4", curveYs[4])
                                            renderEffect = android.graphics.RenderEffect
                                                .createRuntimeShaderEffect(curvesShader, "img")
                                                .asComposeRenderEffect()
                                        }
                                    liveDetail && Build.VERSION.SDK_INT >= 33 && detailShader != null ->
                                        Modifier.graphicsLayer {
                                            detailShader.setFloatUniform("size", size.width, size.height)
                                            detailShader.setFloatUniform("sharpen", sharpen)
                                            detailShader.setFloatUniform("vignette", vignette)
                                            detailShader.setFloatUniform("blur", blurRadius)
                                            renderEffect = android.graphics.RenderEffect
                                                .createRuntimeShaderEffect(detailShader, "img")
                                                .asComposeRenderEffect()
                                        }
                                    else -> Modifier
                                }
                            )
                    )
                    when (mode) {
                        SessionMode.Crop -> CropOverlay(
                            crop = crop,
                            imageRect = imageRect,
                            onChange = { crop = it },
                            modifier = Modifier.fillMaxSize(),
                            viewportZoom = viewportState.zoom
                        )
                        SessionMode.Mask -> BrushOverlay(
                            imageRect = imageRect,
                            radius = brush,
                            onStamp = { stamp ->
                                if (!stampSpacing(lastStamp, stamp)) return@BrushOverlay
                                lastStamp = stamp
                                val id = selectedLayer ?: layers.lastOrNull()?.id ?: return@BrushOverlay
                                sessionViewModel.replaceLayers(
                                    layers.map { layer ->
                                        if (layer.id != id) layer
                                        else layer.copy(
                                            mask = layer.mask + MaskStroke(
                                                stamp.x, stamp.y, stamp.r, maskReveal
                                            )
                                        )
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        SessionMode.Heal -> {

                            if (pickingHealSource) {

                                TapPlaceOverlay(

                                    imageRect = imageRect,

                                    onPlace = { x, y ->

                                        healSource = x to y

                                        pickingHealSource = false

                                    },

                                    modifier = Modifier.fillMaxSize()

                                )

                            } else {

                                BrushOverlay(

                                    imageRect = imageRect,

                                    radius = brush,

                                    onStamp = { stamp ->

                                        if (stampSpacing(lastStamp, stamp)) {

                                            val src = healSource

                                            val next = if (src != null) stamp.copy(sx = src.first, sy = src.second) else stamp

                                            lastStamp = next

                                            pendingStamps = pendingStamps + next

                                        }

                                    },

                                    modifier = Modifier.fillMaxSize()

                                )

                            }

                        }

                        SessionMode.Fill -> BrushOverlay(

                            imageRect = imageRect,

                            radius = brush,

                            onStamp = { stamp ->

                                if (stampSpacing(lastStamp, stamp)) {

                                    lastStamp = stamp

                                    pendingStamps = pendingStamps + stamp

                                }

                            },

                            modifier = Modifier.fillMaxSize()

                        )
                        SessionMode.Erase -> BrushOverlay(

                            imageRect = imageRect,

                            radius = brush,

                            onStamp = { stamp ->

                                if (stampSpacing(lastStamp, stamp)) {

                                    lastStamp = stamp

                                    pendingStamps = pendingStamps + stamp

                                }

                            },

                            modifier = Modifier.fillMaxSize()

                        )
                        SessionMode.Text, SessionMode.Stickers -> TapPlaceOverlay(
                            imageRect = imageRect,
                            onPlace = { x, y -> placeX = x; placeY = y },
                            modifier = Modifier.fillMaxSize()
                        )
                        else -> Unit
                    }
                    val source = healSource
                    if (source != null && (mode == SessionMode.Heal || mode == SessionMode.Erase)) {
                        Canvas(modifier = Modifier.fillMaxSize().testTag("heal_source_crosshair")) {
                            if (imageRect.width <= 0f || imageRect.height <= 0f) return@Canvas
                            val cx = imageRect.left + source.first * imageRect.width
                            val cy = imageRect.top + source.second * imageRect.height
                            val arm = 14.dp.toPx()
                            drawLine(Color.White, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = 2.dp.toPx())
                            drawLine(Color.White, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = 2.dp.toPx())
                            drawCircle(Color(0xFFFF5AA5), radius = 5.dp.toPx(), center = Offset(cx, cy))
                        }
                    }
                        }
                    val bins = histogram
                    if (bins != null && (mode == SessionMode.Adjust || mode == SessionMode.Curves)) {
                        HistogramStrip(
                            bins = bins,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(top = 96.dp)
                        )
                    }
                    }
                }
            } else {
                Text(stringResource(R.string.studio_loading), color = PixiTextMuted)
            }
        }
        status?.let {
            Text(
                it,
                color = when {
                    statusIsError -> PixiError
                    exporting -> PixiTextMuted
                    else -> PixiSuccess
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .testTag("session_status")
            )
        }

        if (!sideTools) {
            ToolsChrome(
                sideDock = false,
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(1f)
            )
        }
        }
        if (sideTools) {
            ToolsChrome(sideDock = true)
        }
        }
    }
    if (showExport) {
        ExportSheet(
            title = stringResource(R.string.studio_export_sheet_title),
            formats = ExportFormatSupport.chipFormats(),
            selectedKey = format.name.lowercase(),
            quality = quality,
            showQuality = format != ExportFormat.PNG,
            destination = stringResource(R.string.studio_export_destination_pictures),
            exporting = exporting,
            onSelectFormat = { key -> format = ExportFormatSupport.parseKey(key) },
            onQuality = { quality = it },
            showKeepExif = true,
            keepExif = keepExif,
            onKeepExif = { keepExif = it },
            showColorIntent = true,
            colorIntentKey = when (colorIntent) {
                com.pixijoy.photo.ColorIntent.DisplayP3 -> "display_p3"
                else -> "srgb"
            },
            onColorIntent = { key ->
                colorIntent = when (key) {
                    "display_p3" -> com.pixijoy.photo.ColorIntent.DisplayP3
                    else -> com.pixijoy.photo.ColorIntent.Srgb
                }
            },
            successUri = exportSuccessUri,
            onView = {
                exportSuccessUri?.let { uri ->
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), "image/*")
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        )
                    }
                }
            },
            onShare = {
                exportSuccessUri?.let { uri ->
                    runCatching {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND)
                                    .setType("image/*")
                                    .putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                context.getString(R.string.export_share)
                            )
                        )
                    }
                }
            },
            onDone = {
                exportSuccessUri = null
                showExport = false
            },
            onRenameProject = {
                exportSuccessUri = null
                showExport = false
                renaming = true
            },
            onExport = {
                if (exporting) return@ExportSheet
                exportSuccessUri = null
                exporting = true
                status = context.getString(R.string.studio_export_background)
                statusIsError = false
                scope.launch {
                    sessionViewModel.flush()
                    MovieExportWorker.ensureChannel(context)
                    val request = OneTimeWorkRequestBuilder<PhotoExportWorker>()
                        .setInputData(
                            workDataOf(
                                PhotoExportWorker.KEY_PROJECT to project.id,
                                PhotoExportWorker.KEY_FORMAT to format.name.lowercase(),
                                PhotoExportWorker.KEY_QUALITY to quality,
                                PhotoExportWorker.KEY_KEEP_EXIF to keepExif,
                                PhotoExportWorker.KEY_STRIP_GPS to true,
                                PhotoExportWorker.KEY_COLOR_INTENT to when (colorIntent) {
                                    com.pixijoy.photo.ColorIntent.DisplayP3 -> "display_p3"
                                    else -> "srgb"
                                }
                            )
                        )
                        .build()
                    workManager.enqueueUniqueWork(
                        PhotoExportWorker.uniqueName(project.id),
                        ExistingWorkPolicy.KEEP,
                        request
                    )
                }
            },
            onDismiss = {
                if (!exporting) {
                    exportSuccessUri = null
                    showExport = false
                }
            }
        )
    }
    if (renaming) {
        ProjectRenameDialog(
            titleKey = project.id,
            initialTitle = project.title,
            onDismiss = { renaming = false },
            onConfirm = { title ->
                renaming = false
                scope.launch { repo.rename(project.id, title) }
            }
        )
    }
    }
}

@Composable
private fun photoGroupItems(): List<StudioGroupItem> = listOf(
    StudioGroupItem(PhotoGroup.Crop.tag, stringResource(R.string.studio_group_crop), R.drawable.pj_ic_crop),
    StudioGroupItem(PhotoGroup.Light.tag, stringResource(R.string.studio_group_light), R.drawable.pj_ic_brightness),
    StudioGroupItem(PhotoGroup.Color.tag, stringResource(R.string.studio_group_color), R.drawable.pj_ic_palette),
    StudioGroupItem(PhotoGroup.Retouch.tag, stringResource(R.string.studio_group_retouch), R.drawable.pj_ic_heal),
    StudioGroupItem(PhotoGroup.Compose.tag, stringResource(R.string.studio_group_compose), R.drawable.pj_ic_layers),
    StudioGroupItem(PhotoGroup.Add.tag, stringResource(R.string.studio_group_add), R.drawable.pj_ic_add)
)

@Composable
private fun PhotoGroupChips(
    group: PhotoGroup,
    mode: SessionMode,
    toolKey: String,
    onMode: (SessionMode) -> Unit,
    onRotate: () -> Unit,
    onFlip: () -> Unit
) {
    StudioHScrollRow {
        when (group) {
            PhotoGroup.Crop -> {
                FormatChip(stringResource(R.string.studio_crop), mode == SessionMode.Crop, "session_crop") {
                    onMode(SessionMode.Crop)
                }
                FormatChip(stringResource(R.string.studio_expand_tool), mode == SessionMode.Expand, "session_expand") {
                    onMode(SessionMode.Expand)
                }
                FormatChip(stringResource(R.string.studio_rotate), false, "session_rotate", onRotate)
                FormatChip(stringResource(R.string.studio_flip), false, "session_flip", onFlip)
            }
            PhotoGroup.Light -> Unit
            PhotoGroup.Color -> {
                FormatChip(stringResource(R.string.studio_color_tune), mode == SessionMode.Adjust, "session_color_tune") {
                    onMode(SessionMode.Adjust)
                }
                FormatChip(stringResource(R.string.studio_curves), mode == SessionMode.Curves, "session_curves") {
                    onMode(SessionMode.Curves)
                }
                FormatChip(stringResource(R.string.studio_selective_hsl), mode == SessionMode.SelectiveHsl, "session_selective_hsl") {
                    onMode(SessionMode.SelectiveHsl)
                }
                FormatChip(stringResource(R.string.studio_looks), mode == SessionMode.Looks, "session_looks") {
                    onMode(SessionMode.Looks)
                }
                FormatChip(stringResource(R.string.studio_presets), mode == SessionMode.Presets, "session_presets") {
                    onMode(SessionMode.Presets)
                }
            }
            PhotoGroup.Retouch -> {
                FormatChip(stringResource(R.string.studio_heal), mode == SessionMode.Heal, "session_heal") {
                    onMode(SessionMode.Heal)
                }
                FormatChip(stringResource(R.string.studio_fill), mode == SessionMode.Fill, "session_fill") {
                    onMode(SessionMode.Fill)
                }
                FormatChip(stringResource(R.string.studio_soften), mode == SessionMode.Soften, "session_soften") {
                    onMode(SessionMode.Soften)
                }
                FormatChip(stringResource(R.string.studio_super_res), mode == SessionMode.SuperRes, "session_super_res") {
                    onMode(SessionMode.SuperRes)
                }
                FormatChip(stringResource(R.string.studio_denoise), mode == SessionMode.Denoise, "session_denoise") {
                    onMode(SessionMode.Denoise)
                }
                FormatChip(stringResource(R.string.studio_sky), mode == SessionMode.Sky, "session_sky") {
                    onMode(SessionMode.Sky)
                }
                if (toolKey == "cutout") {
                    FormatChip(stringResource(R.string.studio_erase), mode == SessionMode.Erase, "session_erase") {
                        onMode(SessionMode.Erase)
                    }
                }
            }
            PhotoGroup.Compose -> {
                FormatChip(stringResource(R.string.studio_layers), mode == SessionMode.Layers, "session_layers") {
                    onMode(SessionMode.Layers)
                }
                FormatChip(stringResource(R.string.studio_mask), mode == SessionMode.Mask, "session_mask") {
                    onMode(SessionMode.Mask)
                }
                FormatChip(stringResource(R.string.studio_warp), mode == SessionMode.Warp, "session_warp") {
                    onMode(SessionMode.Warp)
                }
                FormatChip(stringResource(R.string.studio_collage), mode == SessionMode.Collage, "session_collage") {
                    onMode(SessionMode.Collage)
                }
            }
            PhotoGroup.Add -> {
                FormatChip(stringResource(R.string.studio_text), mode == SessionMode.Text, "session_text") {
                    onMode(SessionMode.Text)
                }
                FormatChip(stringResource(R.string.studio_stickers), mode == SessionMode.Stickers, "session_stickers") {
                    onMode(SessionMode.Stickers)
                }
            }
        }
    }
}

@Composable
private fun ToolBody(
    controls: PhotoSessionControls,
    mode: SessionMode,
    group: PhotoGroup,
    toolKey: String,
    onMode: (SessionMode) -> Unit,
    onRotate: () -> Unit,
    onFlip: () -> Unit,
    exposure: Float,
    contrast: Float,
    hue: Float,
    saturation: Float,
    expand: Float,
    brush: Float,
    text: String,
    onExposure: (Float) -> Unit,
    onContrast: (Float) -> Unit,
    onHue: (Float) -> Unit,
    onSaturation: (Float) -> Unit,
    onExpand: (Float) -> Unit,
    onBrush: (Float) -> Unit,
    onText: (String) -> Unit,
    onApplyGrade: () -> Unit,
    sharpen: Float,
    vignette: Float,
    blurRadius: Float,
    onSharpenAmount: (Float) -> Unit,
    onVignetteAmount: (Float) -> Unit,
    onBlurAmount: (Float) -> Unit,
    onApplyCrop: () -> Unit,
    onApplyExpand: () -> Unit,
    onApplySkyReplace: (SkyReplaceTarget, Int) -> Unit,
    onApplyHeal: () -> Unit,
    onApplyFill: (neural: Boolean) -> Unit,
    onClearFillStrokes: () -> Unit,
    onUndoFillStroke: () -> Unit,
    onApplyPerspective: (Float, Float) -> Unit,
    onApplyLiquify: (Float, Float, Float, Float) -> Unit,
    onApplySoften: (Float) -> Unit,
    onApplySuperRes: (Int) -> Unit,
    onApplyDenoise: (Float) -> Unit,
    onSavePreset: (String) -> Unit,
    onApplyPreset: (String) -> Unit,
    presets: List<PhotoPreset>,
    onApplyErase: () -> Unit,
    onApplyText: () -> Unit,
    onRestore: () -> Unit,
    onCutout: () -> Unit,
    onSticker: (String) -> Unit,
    onCollage: (Int) -> Unit,
    onAddPhoto: () -> Unit,
    onApplyCurves: (FloatArray) -> Unit,
    selectiveRange: SelectiveHslRange,
    selectiveHue: Float,
    selectiveSat: Float,
    selectiveLight: Float,
    onSelectiveRange: (SelectiveHslRange) -> Unit,
    onSelectiveHue: (Float) -> Unit,
    onSelectiveSat: (Float) -> Unit,
    onSelectiveLight: (Float) -> Unit,
    onApplySelectiveHsl: () -> Unit,
    curveYs: FloatArray,
    onCurveYs: (FloatArray) -> Unit,
    lookKey: String,
    onLookKey: (String) -> Unit,
    onApplyLook: (String) -> Unit,
    onImportLut: () -> Unit,
    importedLookLabel: String?,
    maskFill: MaskFill,
    maskReveal: Boolean,
    onMaskReveal: (Boolean) -> Unit,
    onClearMask: () -> Unit,
    onRestoreMask: () -> Unit = {},
    canRestoreMask: Boolean = false,
    visionAvailable: Boolean = false,
    onSubjectMask: () -> Unit,
    onBackgroundMask: () -> Unit,
    collageCols: Int = 2,
    pickingHealSource: Boolean = false,
    healSourceSet: Boolean = false,
    onPickHealSource: () -> Unit = {},
    onPaintHeal: () -> Unit = {}
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PhotoGroupChips(group, mode, toolKey, onMode, onRotate, onFlip)
        if (toolKey == "restore" && group == PhotoGroup.Retouch) {
            PixiPrimaryButton(stringResource(R.string.studio_restore_now), onRestore, Modifier.testTag("session_restore"))
        }
        if ((toolKey == "cutout" || toolKey == "stickers") && (group == PhotoGroup.Retouch || group == PhotoGroup.Add)) {
            StudioHint(
                stringResource(
                    if (visionAvailable) R.string.studio_cutout_subject_available
                    else R.string.studio_cutout_corner_fallback
                ),
                "session_cutout_capability"
            )
            PixiPrimaryButton(stringResource(R.string.studio_cutout_now), onCutout, Modifier.testTag("session_cutout"))
        }
        when (mode) {
            SessionMode.Crop -> {
                if (group == PhotoGroup.Crop) {
                    StudioApplyButton(
                        stringResource(R.string.studio_apply),
                        onApplyCrop,
                        tag = "session_apply_crop"
                    )
                }
            }
            SessionMode.Adjust -> {
                if (group == PhotoGroup.Light) {
                    val lightFocus = controls.lightFocus
                    StudioHScrollRow {
                        StudioChip(stringResource(R.string.studio_exposure), lightFocus == 0, "session_light_exposure") {
                            controls.lightFocus = 0
                        }
                        StudioChip(stringResource(R.string.studio_contrast), lightFocus == 1, "session_light_contrast") {
                            controls.lightFocus = 1
                        }
                        StudioChip(stringResource(R.string.studio_sharpen), lightFocus == 2, "session_light_sharpen") {
                            controls.lightFocus = 2
                        }
                        StudioChip(stringResource(R.string.studio_vignette), lightFocus == 3, "session_light_vignette") {
                            controls.lightFocus = 3
                        }
                        StudioChip(stringResource(R.string.studio_blur), lightFocus == 4, "session_light_blur") {
                            controls.lightFocus = 4
                        }
                    }
                    when (lightFocus) {
                        0 -> StudioValueSlider(
                            stringResource(R.string.studio_exposure), exposure, -1.5f..1.5f,
                            "session_exposure", 0f, "%.2f".format(exposure), onExposure,
                            showLabel = false
                        )
                        1 -> StudioValueSlider(
                            stringResource(R.string.studio_contrast), contrast, 0.4f..1.8f,
                            "session_contrast", 1f, "%.2f".format(contrast), onContrast,
                            showLabel = false
                        )
                        2 -> StudioValueSlider(
                            stringResource(R.string.studio_sharpen), sharpen, 0f..1f,
                            "session_sharpen", 0f, "%.2f".format(sharpen), onSharpenAmount,
                            showLabel = false
                        )
                        3 -> StudioValueSlider(
                            stringResource(R.string.studio_vignette), vignette, 0f..1f,
                            "session_vignette", 0f, "%.2f".format(vignette), onVignetteAmount,
                            showLabel = false
                        )
                        else -> StudioValueSlider(
                            stringResource(R.string.studio_blur), blurRadius, 0f..8f,
                            "session_blur", 0f, "%.1f".format(blurRadius), onBlurAmount,
                            showLabel = false
                        )
                    }
                    StudioApplyButton(stringResource(R.string.studio_apply), onApplyGrade)
                } else if (group == PhotoGroup.Color) {
                    val colorFocus = controls.colorFocus
                    StudioHScrollRow {
                        StudioChip(
                            stringResource(R.string.studio_hue),
                            colorFocus == 0,
                            "session_color_hue"
                        ) {
                            controls.colorFocus = 0
                        }
                        StudioChip(
                            stringResource(R.string.studio_saturation),
                            colorFocus == 1,
                            "session_color_saturation"
                        ) {
                            controls.colorFocus = 1
                        }
                    }
                    if (colorFocus == 0) {
                        StudioValueSlider(
                            stringResource(R.string.studio_hue),
                            hue,
                            -180f..180f,
                            "session_hue",
                            0f,
                            "%.0f".format(hue),
                            onHue,
                            showLabel = false
                        )
                    } else {
                        StudioValueSlider(
                            stringResource(R.string.studio_saturation),
                            saturation,
                            0f..2f,
                            "session_saturation",
                            1f,
                            "%.2f".format(saturation),
                            onSaturation,
                            showLabel = false
                        )
                    }
                    StudioApplyButton(stringResource(R.string.studio_apply), onApplyGrade)
                }
            }
            SessionMode.Curves -> {
                val curveFocus = controls.curveFocus
                StudioHint(stringResource(R.string.studio_curves_hint), "session_curves_hint")
                StudioHScrollRow {
                    listOf(
                        0 to R.string.studio_curve_black,
                        1 to R.string.studio_curve_shadow,
                        2 to R.string.studio_curve_mid,
                        3 to R.string.studio_curve_light,
                        4 to R.string.studio_curve_white
                    ).forEach { (i, label) ->
                        StudioChip(stringResource(label), curveFocus == i, "curve_$i") { controls.curveFocus = i }
                    }
                }
                fun set(i: Int, v: Float) {
                    val next = curveYs.copyOf()
                    next[i] = v
                    onCurveYs(next)
                }
                val i = curveFocus.coerceIn(0, 4)
                LabeledSlider(
                    stringResource(
                        when (i) {
                            0 -> R.string.studio_curve_black
                            1 -> R.string.studio_curve_shadow
                            3 -> R.string.studio_curve_light
                            4 -> R.string.studio_curve_white
                            else -> R.string.studio_curve_mid
                        }
                    ),
                    curveYs[i],
                    0f..1f,
                    "curve_slider"
                ) { set(i, it) }
                StudioApplyButton(
                    stringResource(R.string.studio_apply),
                    { onApplyCurves(curveYs.copyOf()) },
                    tag = "session_apply_curves"
                )
            }

            SessionMode.SelectiveHsl -> {
                StudioHint(stringResource(R.string.studio_selective_hsl_hint), "session_selective_hsl_hint")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SelectiveHslRange.values().forEach { range ->
                        FormatChip(
                            label = stringResource(
                                when (range) {
                                    SelectiveHslRange.Reds -> R.string.studio_hsl_reds
                                    SelectiveHslRange.Oranges -> R.string.studio_hsl_oranges
                                    SelectiveHslRange.Yellows -> R.string.studio_hsl_yellows
                                    SelectiveHslRange.Greens -> R.string.studio_hsl_greens
                                    SelectiveHslRange.Cyans -> R.string.studio_hsl_cyans
                                    SelectiveHslRange.Blues -> R.string.studio_hsl_blues
                                    SelectiveHslRange.Magentas -> R.string.studio_hsl_magentas
                                }
                            ),
                            selected = selectiveRange == range,
                            tag = "hsl_range_${range.name.lowercase()}",
                            onClick = { onSelectiveRange(range) }
                        )
                    }
                }
                LabeledSlider(stringResource(R.string.studio_hue), selectiveHue, -180f..180f, "session_selective_hue", onSelectiveHue)
                LabeledSlider(stringResource(R.string.studio_saturation), selectiveSat, 0f..2f, "session_selective_sat", onSelectiveSat)
                LabeledSlider(stringResource(R.string.studio_selective_light), selectiveLight, -0.5f..0.5f, "session_selective_light", onSelectiveLight)
                PixiPrimaryButton(
                    stringResource(R.string.studio_apply),
                    onApplySelectiveHsl,
                    Modifier.testTag("session_apply_selective_hsl")
                )
            }


            SessionMode.Looks -> {
                StudioHint(stringResource(R.string.studio_looks_hint), "session_looks_hint")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        BundledLooks.ORIGINAL to R.string.studio_look_original,
                        BundledLooks.CLEAN to R.string.studio_look_clean,
                        BundledLooks.WARM to R.string.studio_look_warm,
                        BundledLooks.COOL to R.string.studio_look_cool,
                        BundledLooks.FILM to R.string.studio_look_film,
                        BundledLooks.NIGHT to R.string.studio_look_night
                    ).forEach { (key, label) ->
                        FormatChip(
                            stringResource(label),
                            lookKey == key,
                            "look_$key"
                        ) { onLookKey(key) }
                    }
                }
                if (ImportedLuts.isImported(lookKey)) {
                    Text(
                        stringResource(
                            R.string.studio_lut_selected,
                            importedLookLabel ?: ImportedLuts.fileName(lookKey)
                        ),
                        color = PixiText,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .testTag("session_lut_imported_name")
                    )
                }
                PixiSecondaryButton(
                    text = stringResource(R.string.studio_lut_import),
                    onClick = onImportLut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_lut_import")
                )
                PixiPrimaryButton(
                    stringResource(R.string.studio_apply),
                    { onApplyLook(lookKey) },
                    Modifier.testTag("session_apply_look")
                )
            }
            SessionMode.Expand -> {
                LabeledSlider(stringResource(R.string.studio_expand_tool), expand, 0.05f..0.35f, "session_expand_amount", onExpand)
                PixiPrimaryButton(stringResource(R.string.studio_apply), onApplyExpand, Modifier.testTag("session_apply_expand"))
            }
            SessionMode.Sky -> SkyToolPanel(controls, onApply = onApplySkyReplace)
            SessionMode.Heal -> {
                LabeledSlider(stringResource(R.string.studio_brush), brush, 0.02f..0.12f, "session_brush_size", onBrush)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.studio_heal_set_source),
                        color = PixiText,
                        fontWeight = if (pickingHealSource) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (pickingHealSource) PixiAccent else PixiBg)
                            .clickable(onClick = onPickHealSource)
                            .testTag("session_heal_set_source")
                            .semantics {
                                role = Role.RadioButton
                                selected = pickingHealSource
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                    Text(
                        stringResource(R.string.studio_heal_paint),
                        color = PixiText,
                        fontWeight = if (!pickingHealSource) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (!pickingHealSource) PixiAccent else PixiBg)
                            .clickable(onClick = onPaintHeal)
                            .testTag("session_heal_paint")
                            .semantics {
                                role = Role.RadioButton
                                selected = !pickingHealSource
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
                Text(
                    if (healSourceSet) stringResource(R.string.studio_heal_source_set)
                    else stringResource(R.string.studio_heal_source_ring),
                    color = PixiTextMuted,
                    modifier = Modifier.testTag("session_heal_source_status")
                )
                PixiPrimaryButton(stringResource(R.string.studio_apply), onApplyHeal, Modifier.testTag("session_apply_heal"))
            }
            SessionMode.Fill -> FillToolPanel(
                brush = brush,
                onBrush = onBrush,
                onUndoStroke = onUndoFillStroke,
                onClear = onClearFillStrokes,
                onApply = onApplyFill
            )
            SessionMode.Warp -> WarpToolPanel(
                onApplyPerspective = onApplyPerspective,
                onApplyLiquify = onApplyLiquify
            )
            SessionMode.Soften -> SoftenToolPanel(onApply = onApplySoften)
            SessionMode.SuperRes -> SuperResToolPanel(onApply = onApplySuperRes)
            SessionMode.Denoise -> DenoiseToolPanel(onApply = onApplyDenoise)
            SessionMode.Presets -> PresetToolPanel(
                presets = presets,
                onSave = onSavePreset,
                onApply = onApplyPreset
            )
            SessionMode.Erase -> {
                LabeledSlider(stringResource(R.string.studio_brush), brush, 0.02f..0.12f, "session_brush_size", onBrush)
                PixiPrimaryButton(stringResource(R.string.studio_apply), onApplyErase, Modifier.testTag("session_apply_erase"))
            }
            SessionMode.Text -> {
                OutlinedTextField(
                    value = text,
                    onValueChange = onText,
                    modifier = Modifier.fillMaxWidth().testTag("session_text_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PixiText,
                        unfocusedTextColor = PixiText
                    )
                )
                Text(stringResource(R.string.studio_tap_place), color = PixiTextMuted)
                PixiPrimaryButton(stringResource(R.string.studio_apply), onApplyText, Modifier.testTag("session_apply_text"))
            }
            SessionMode.Stickers -> {
                Text(stringResource(R.string.studio_tap_place), color = PixiTextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormatChip("❤", false, "sticker_heart") { onSticker("❤") }
                    FormatChip("★", false, "sticker_star") { onSticker("★") }
                    FormatChip("✦", false, "sticker_spark") { onSticker("✦") }
                }
            }
            SessionMode.Mask -> {
                LabeledSlider(stringResource(R.string.studio_brush), brush, 0.02f..0.12f, "session_brush_size", onBrush)
                StudioHint(
                    stringResource(
                        if (visionAvailable) R.string.studio_mask_hint
                        else R.string.studio_mask_subject_unavailable
                    ),
                    "session_mask_capability"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormatChip(
                        stringResource(R.string.studio_mask_subject),
                        maskFill == MaskFill.Subject,
                        "mask_subject"
                    ) {
                        if (visionAvailable) onSubjectMask()
                    }
                    FormatChip(
                        stringResource(R.string.studio_mask_background),
                        maskFill == MaskFill.Background,
                        "mask_background"
                    ) {
                        if (visionAvailable) onBackgroundMask()
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormatChip(stringResource(R.string.studio_mask_protect), maskReveal, "mask_protect") {
                        onMaskReveal(true)
                    }
                    FormatChip(stringResource(R.string.studio_mask_erase), !maskReveal, "mask_erase") {
                        onMaskReveal(false)
                    }
                    FormatChip(stringResource(R.string.studio_mask_hide), !maskReveal, "mask_hide") {
                        onMaskReveal(false)
                    }
                    FormatChip(stringResource(R.string.studio_mask_reveal), maskReveal, "mask_reveal") {
                        onMaskReveal(true)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixiPrimaryButton(
                        stringResource(R.string.studio_mask_clear),
                        onClearMask,
                        Modifier.weight(1f).testTag("mask_clear")
                    )
                    if (canRestoreMask) {
                        PixiSecondaryButton(
                            stringResource(R.string.studio_mask_restore),
                            onRestoreMask,
                            Modifier.weight(1f).testTag("mask_restore")
                        )
                    }
                }
            }
            SessionMode.Layers -> {
                StudioHint(stringResource(R.string.studio_layers_hint), "layers_panel_hint")
            }
            SessionMode.Collage -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormatChip(
                        stringResource(R.string.studio_collage_2),
                        collageCols == 2,
                        "collage_2"
                    ) { onCollage(2) }
                    FormatChip(
                        stringResource(R.string.studio_collage_4),
                        collageCols == 4,
                        "collage_4"
                    ) { onCollage(4) }
                }
                PixiPrimaryButton(
                    stringResource(R.string.studio_add_photo),
                    onAddPhoto,
                    Modifier.testTag("session_add_photo")
                )
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    tag: String,
    onChange: (Float) -> Unit
) {
    Text(label, color = PixiTextMuted)
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = range,
        colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag(tag)
            .semantics { contentDescription = label }
    )
}

@Composable
private fun SkyToolPanel(
    controls: PhotoSessionControls,
    onApply: (SkyReplaceTarget, Int) -> Unit
) {
    val skyTarget = controls.skyTarget
    val skyColor = controls.skyColor
    Text(
        stringResource(R.string.studio_sky_hint),
        color = PixiTextMuted,
        modifier = Modifier.testTag("session_sky_hint")
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FormatChip(
            stringResource(R.string.studio_sky_target_sky),
            skyTarget == SkyReplaceTarget.Sky,
            "sky_target_sky"
        ) { controls.skyTarget = SkyReplaceTarget.Sky }
        FormatChip(
            stringResource(R.string.studio_sky_target_background),
            skyTarget == SkyReplaceTarget.Background,
            "sky_target_background"
        ) { controls.skyTarget = SkyReplaceTarget.Background }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        FormatChip("Blue", skyColor == SkyReplacePresets.BlueSky, "sky_preset_blue") {
            controls.skyColor = SkyReplacePresets.BlueSky
        }
        FormatChip("Deep", skyColor == SkyReplacePresets.DeepBlue, "sky_preset_deep") {
            controls.skyColor = SkyReplacePresets.DeepBlue
        }
        FormatChip("Sunset", skyColor == SkyReplacePresets.Sunset, "sky_preset_sunset") {
            controls.skyColor = SkyReplacePresets.Sunset
        }
        FormatChip("Gray", skyColor == SkyReplacePresets.Overcast, "sky_preset_overcast") {
            controls.skyColor = SkyReplacePresets.Overcast
        }
        FormatChip("Green", skyColor == SkyReplacePresets.StudioGreen, "sky_preset_green") {
            controls.skyColor = SkyReplacePresets.StudioGreen
        }
    }
    PixiPrimaryButton(
        stringResource(R.string.studio_apply),
        { onApply(skyTarget, skyColor) },
        Modifier.testTag("session_apply_sky")
    )
}

@Composable
private fun FormatChip(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Text(
        label,
        color = PixiText,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) PixiAccent else PixiSurface2)
            .clickable(onClick = onClick)
            .testTag(tag)
            .semantics {
                contentDescription = label
                role = Role.RadioButton
                this.selected = selected
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}

@Composable
private fun LayerStack(
    layers: List<LayeredOp>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onChange: (List<LayeredOp>) -> Unit
) {
    val selected = layers.firstOrNull { it.id == selectedId } ?: layers.lastOrNull()
    Column(Modifier.padding(horizontal = 12.dp).testTag("layers_panel")) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            layers.asReversed().forEach { layer ->
                Text(
                    layer.name,
                    color = PixiText,
                    fontWeight = if (layer.id == selected?.id) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (layer.id == selected?.id) PixiAccent else PixiSurface2)
                        .clickable { onSelect(layer.id) }
                        .testTag("layer_row_${layer.id}")
                        .semantics {
                            contentDescription = layer.name
                            role = Role.RadioButton
                            this.selected = layer.id == selected?.id
                        }
                        .padding(horizontal = 10.dp, vertical = 12.dp)
                )
            }
        }
        if (selected != null) {
            LabeledSlider(
                stringResource(R.string.studio_fade),
                selected.opacity,
                0f..1f,
                "layer_fade"
            ) { v ->
                onChange(layers.map { if (it.id == selected.id) it.copy(opacity = v) else it })
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BlendMode.entries.forEach { mode ->
                    FormatChip(
                        blendUiLabel(mode),
                        selected.blend == mode,
                        "blend_${mode.name.lowercase()}"
                    ) {
                        onChange(layers.map { if (it.id == selected.id) it.copy(blend = mode) else it })
                    }
                }
            }
            Row {
                val visibilityLabel = if (selected.visible) {
                    stringResource(R.string.studio_hide)
                } else {
                    stringResource(R.string.studio_show)
                }
                TextButton(
                    onClick = {
                        onChange(layers.map {
                            if (it.id == selected.id) it.copy(visible = !it.visible) else it
                        })
                    },
                    modifier = Modifier
                        .testTag("layer_visibility")
                        .semantics { contentDescription = visibilityLabel }
                ) {
                    Text(visibilityLabel, color = PixiText, modifier = Modifier.clearAndSetSemantics { })
                }
                TextButton(onClick = {
                    onChange(layers.filterNot { it.id == selected.id })
                }) { Text(stringResource(R.string.studio_library_delete), color = PixiText) }
            }
        }
    }
}


private fun blendUiLabel(mode: BlendMode): String = when (mode) {
    BlendMode.Normal -> "Normal"
    BlendMode.Multiply -> "Darken"
    BlendMode.Screen -> "Lighten"
    BlendMode.Overlay -> "Contrast"
}

@Composable
private fun HistogramStrip(bins: IntArray, modifier: Modifier = Modifier) {
    val clip = remember(bins) { histogramClip(bins) }
    Column(
        modifier
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PixiBg.copy(alpha = 0.45f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .testTag("photo_histogram")
        ) {
            val peak = bins.maxOrNull()?.coerceAtLeast(1) ?: 1
            val bar = size.width / bins.size.coerceAtLeast(1)
            bins.forEachIndexed { i, n ->
                val h = size.height * n / peak
                val color = when {
                    i < 5 -> Color(0xFFE05A5A)
                    i >= 251 -> Color(0xFF6FA8FF)
                    else -> PixiAccent
                }
                drawRect(
                    color = color.copy(alpha = 0.9f),
                    topLeft = Offset(i * bar, size.height - h),
                    size = Size(bar.coerceAtLeast(1f), h)
                )
            }
        }
        Text(
            "${stringResource(R.string.studio_clip_shadows, clip.shadowPct * 100f)}   ${stringResource(R.string.studio_clip_highlights, clip.highlightPct * 100f)}",
            color = PixiTextMuted,
            modifier = Modifier.testTag("photo_clipping")
        )
    }
}
