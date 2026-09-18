package com.pixijoy.ai.ui.session

import android.graphics.Bitmap
import android.graphics.RuntimeShader
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.export.MovieExportWorker
import com.pixijoy.ai.export.PhotoExportWorker
import com.pixijoy.core.common.AppResult
import com.pixijoy.core.data.Project
import com.pixijoy.core.data.ProjectRepository
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiSuccess
import com.pixijoy.designsystem.PixiSurface
import com.pixijoy.designsystem.PixiSurface2
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.photo.BitmapPhotoEngine
import com.pixijoy.photo.BlendMode
import com.pixijoy.photo.ExportFormat
import com.pixijoy.photo.ExportFormatSupport
import com.pixijoy.photo.LayeredOp
import com.pixijoy.photo.LiveGrade
import com.pixijoy.photo.MaskFill
import com.pixijoy.photo.MaskStroke
import com.pixijoy.photo.PhotoOp
import com.pixijoy.photo.Stamp
import com.pixijoy.photo.decodeLayers
import com.pixijoy.photo.histogramClip
import com.pixijoy.photo.lumaHistogram
import com.pixijoy.photo.encodeLayers
import com.pixijoy.photo.withBackgroundSeed
import com.pixijoy.photo.layerName
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SessionMode { Crop, Adjust, Heal, Fill, Erase, Text, Expand, Stickers, Collage, Curves, Layers, Mask }

@Composable
fun PhotoSessionScreen(
    project: Project,
    toolLabel: String,
    toolKey: String,
    repo: ProjectRepository,
    engine: BitmapPhotoEngine = remember { BitmapPhotoEngine() },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sources = remember(project.media, project.coverPath) {
        project.media.map { it.path }.ifEmpty { listOf(project.coverPath) }
    }
    var layers by remember(project.id) { mutableStateOf(project.graph.decodeLayers()) }
    var redo by remember(project.id) { mutableStateOf(listOf<LayeredOp>()) }
    var selectedLayer by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var histogram by remember { mutableStateOf<IntArray?>(null) }
    var mode by remember { mutableStateOf(defaultMode(toolKey)) }
    var crop by remember { mutableStateOf(PhotoOp.Crop(0.08f, 0.08f, 0.92f, 0.92f)) }
    var exposure by remember { mutableStateOf(0f) }
    var contrast by remember { mutableStateOf(1f) }
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var curveYs by remember { mutableStateOf(floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)) }
    val curvesShader = remember {
        if (Build.VERSION.SDK_INT >= 33) RuntimeShader(LiveGrade.CURVES_AGSL) else null
    }
    val detailShader = remember {
        if (Build.VERSION.SDK_INT >= 33) RuntimeShader(LiveGrade.DETAIL_AGSL) else null
    }
    var sharpen by remember { mutableStateOf(0f) }
    var vignette by remember { mutableStateOf(0f) }
    var blurRadius by remember { mutableStateOf(0f) }
    var expand by remember { mutableStateOf(0.12f) }
    var brush by remember { mutableStateOf(0.05f) }
    var maskReveal by remember { mutableStateOf(false) }
    var pendingStamps by remember { mutableStateOf(listOf<Stamp>()) }
    var lastStamp by remember { mutableStateOf<Stamp?>(null) }
    var healSource by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var pickingHealSource by remember { mutableStateOf(false) }
    var collageCols by remember { mutableStateOf(2) }
    var text by remember { mutableStateOf("PixiJoy") }
    var placeX by remember { mutableStateOf(0.5f) }
    var placeY by remember { mutableStateOf(0.82f) }
    var format by remember {
        mutableStateOf(if (toolKey == "cutout" || toolKey == "stickers") ExportFormat.PNG else ExportFormat.JPEG)
    }
    var quality by remember { mutableStateOf(92) }
    var keepExif by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun push(op: PhotoOp) {
        val layer = LayeredOp(
            id = "L${System.currentTimeMillis()}",
            name = op.layerName(),
            op = op
        )
        layers = layers + layer
        selectedLayer = layer.id
        redo = emptyList()
        status = null
        pendingStamps = emptyList()
        lastStamp = null
        pickingHealSource = false
    }

    val addPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                repo.addUri(project.id, uri)
                if (layers.none { it.op is PhotoOp.Collage }) {
                    push(PhotoOp.Collage(2))
                }
            }
        }
    }


    LaunchedEffect(mode, project.id) {
        if (mode != SessionMode.Layers) return@LaunchedEffect
        if (layers.isNotEmpty()) return@LaunchedEffect
        val seeded = layers.withBackgroundSeed(id = "L-bg-${project.id}")
        layers = seeded
        selectedLayer = seeded.first().id
    }

    LaunchedEffect(sources, layers) {
        busy = true
        val next = withContext(Dispatchers.Default) {
            engine.renderLayers(sources, layers, maxEdge = 1600)
        }
        preview?.let { if (!it.isRecycled) it.recycle() }
        preview = next
        histogram = lumaHistogram(next)
        repo.saveGraph(project.id, layers.encodeLayers())
        busy = false
    }
    DisposableEffect(Unit) {
        onDispose { preview?.let { if (!it.isRecycled) it.recycle() } }
    }
    val workManager = remember { WorkManager.getInstance(context) }
    val exportInfos by workManager
        .getWorkInfosForUniqueWorkFlow(PhotoExportWorker.uniqueName(project.id))
        .collectAsState(initial = emptyList())
    LaunchedEffect(exportInfos) {
        val info = exportInfos.firstOrNull() ?: return@LaunchedEffect
        when (info.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> {
                exporting = true
                if (status == null) status = context.getString(R.string.studio_export_background)
            }
            WorkInfo.State.SUCCEEDED -> {
                if (exporting) {
                    exporting = false
                    showExport = false
                    status = context.getString(R.string.studio_export_ok)
                }
            }
            WorkInfo.State.FAILED -> {
                if (exporting) {
                    exporting = false
                    status = info.outputData.getString(PhotoExportWorker.KEY_ERROR)
                        ?: context.getString(R.string.studio_export_failed)
                }
            }
            else -> Unit
        }
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, modifier = Modifier.testTag("session_back")) {
                Text(stringResource(R.string.studio_back), color = PixiText)
            }
            Text(toolLabel, color = PixiText, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(
                onClick = {
                    if (layers.isNotEmpty()) {
                        redo = redo + layers.last()
                        layers = layers.dropLast(1)
                    }
                },
                enabled = layers.isNotEmpty(),
                modifier = Modifier.testTag("session_undo")
            ) { Text(stringResource(R.string.studio_undo), color = PixiTextMuted) }
            TextButton(
                onClick = {
                    if (redo.isNotEmpty()) {
                        layers = layers + redo.last()
                        redo = redo.dropLast(1)
                    }
                },
                enabled = redo.isNotEmpty(),
                modifier = Modifier.testTag("session_redo")
            ) { Text(stringResource(R.string.studio_redo), color = PixiTextMuted) }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(PixiSurface),
            contentAlignment = Alignment.Center
        ) {
            val bmp = preview
            if (bmp != null && !bmp.isRecycled) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val imageRect = fittedImageRect(
                        constraints.maxWidth.toFloat(),
                        constraints.maxHeight.toFloat(),
                        bmp.width,
                        bmp.height
                    )
                    val liveAdjust = mode == SessionMode.Adjust &&
                        !LiveGrade.isAdjustIdentity(exposure, contrast, hue, saturation)
                    val liveDetail = mode == SessionMode.Adjust &&
                        (sharpen > 0.01f || vignette > 0.01f || blurRadius > 0.5f)
                    val liveCurves = mode == SessionMode.Curves && !LiveGrade.curvesIdentity(curveYs)
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = project.title,
                        contentScale = ContentScale.Fit,
                        colorFilter = if (liveAdjust) {
                            ColorFilter.colorMatrix(
                                ColorMatrix(LiveGrade.colorMatrix(exposure, contrast, hue, saturation))
                            )
                        } else {
                            null
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
                        SessionMode.Crop -> CropOverlay(crop, imageRect, { crop = it }, Modifier.fillMaxSize())
                        SessionMode.Mask -> BrushOverlay(
                            imageRect = imageRect,
                            radius = brush,
                            onStamp = { stamp ->
                                if (!stampSpacing(lastStamp, stamp)) return@BrushOverlay
                                lastStamp = stamp
                                val id = selectedLayer ?: layers.lastOrNull()?.id ?: return@BrushOverlay
                                layers = layers.map { layer ->
                                    if (layer.id != id) layer
                                    else layer.copy(
                                        mask = layer.mask + MaskStroke(
                                            stamp.x, stamp.y, stamp.r, maskReveal
                                        )
                                    )
                                }
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
                    val bins = histogram
                    if (bins != null && (mode == SessionMode.Adjust || mode == SessionMode.Curves)) {
                        HistogramStrip(
                            bins = bins,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        )
                    }
                }
            } else {
                Text(stringResource(R.string.studio_loading), color = PixiTextMuted)
            }
        }
        status?.let {
            Text(
                it,
                color = if (exporting) PixiTextMuted else PixiSuccess,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp).testTag("session_status")
            )
        }
        ToolBody(
            mode = mode,
            toolKey = toolKey,
            exposure = exposure,
            contrast = contrast,
            hue = hue,
            saturation = saturation,
            expand = expand,
            brush = brush,
            text = text,
            onExposure = { exposure = it },
            onContrast = { contrast = it },
            onHue = { hue = it },
            onSaturation = { saturation = it },
            onExpand = { expand = it },
            onBrush = { brush = it },
            onText = { text = it },
            onApplyGrade = {
                push(PhotoOp.Grade(exposure, contrast))
                push(PhotoOp.Hsl(hue, saturation))
                if (sharpen > 0.01f) push(PhotoOp.Sharpen(sharpen))
                if (vignette > 0.01f) push(PhotoOp.Vignette(vignette))
                if (blurRadius > 0.5f) push(PhotoOp.Blur(blurRadius.toInt().coerceIn(1, 8)))
                sharpen = 0f
                vignette = 0f
                blurRadius = 0f
            },
            sharpen = sharpen,
            vignette = vignette,
            blurRadius = blurRadius,
            onSharpenAmount = { sharpen = it },
            onVignetteAmount = { vignette = it },
            onBlurAmount = { blurRadius = it },
            onApplyCrop = { push(crop); mode = defaultMode(toolKey) },
            onApplyExpand = { push(PhotoOp.Expand(expand)) },
            onApplyHeal = { if (pendingStamps.isNotEmpty()) push(PhotoOp.Heal(pendingStamps)) },
            onApplyFill = { if (pendingStamps.isNotEmpty()) push(PhotoOp.Fill(pendingStamps)); pendingStamps = emptyList() },
            onApplyErase = { if (pendingStamps.isNotEmpty()) push(PhotoOp.Erase(pendingStamps)) },
            onApplyText = { push(PhotoOp.TextLayer(text, placeX, placeY, 0.08f, 0xFFFFFFFF.toInt())) },
            onRestore = { push(PhotoOp.Restore) },
            onCutout = { push(PhotoOp.Subject); format = ExportFormat.PNG },
            onApplyCurves = { push(PhotoOp.Curves(it.copyOf())) },
            curveYs = curveYs,
            onCurveYs = { curveYs = it },
            onBlur = { push(PhotoOp.Blur(2)) },
            onSharpen = { push(PhotoOp.Sharpen(0.55f)) },
            onVignette = { push(PhotoOp.Vignette(0.4f)) },
            maskFill = (layers.firstOrNull { it.id == selectedLayer } ?: layers.lastOrNull())?.maskFill
                ?: MaskFill.None,
            maskReveal = maskReveal,
            onMaskReveal = { maskReveal = it },
            onClearMask = {
                val id = selectedLayer ?: layers.lastOrNull()?.id
                if (id != null) {
                    layers = layers.map {
                        if (it.id == id) it.copy(mask = emptyList(), maskFill = MaskFill.None) else it
                    }
                }
            },
            onSubjectMask = {
                val id = selectedLayer ?: layers.lastOrNull()?.id ?: return@ToolBody
                layers = layers.map {
                    if (it.id == id) it.copy(maskFill = MaskFill.Subject) else it
                }
            },
            onBackgroundMask = {
                val id = selectedLayer ?: layers.lastOrNull()?.id ?: return@ToolBody
                layers = layers.map {
                    if (it.id == id) it.copy(maskFill = MaskFill.Background) else it
                }
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
            },
            onAddPhoto = {
                addPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            railsFor(toolKey).forEach { item ->
                RailAction(stringResource(item.label), item.tag, selected = mode == item.mode) { mode = item.mode }
            }
            if (toolKey == "editor") {
                RailAction(stringResource(R.string.studio_rotate), "session_rotate") {
                    push(PhotoOp.Rotate(90))
                }
                RailAction(stringResource(R.string.studio_flip), "session_flip") {
                    push(PhotoOp.FlipH)
                }
            }
        }
        if (layers.isNotEmpty()) {
            LayerStack(
                layers = layers,
                selectedId = selectedLayer,
                onSelect = { selectedLayer = it },
                onChange = { layers = it }
            )
        }
        PixiPrimaryButton(
            text = stringResource(R.string.studio_export),
            onClick = { showExport = true },
            enabled = !exporting && !busy,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("session_export")
        )
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
            onExport = {
                if (exporting) return@ExportSheet
                exporting = true
                status = context.getString(R.string.studio_export_background)
                showExport = false
                scope.launch {
                    repo.saveGraph(project.id, layers.encodeLayers())
                    MovieExportWorker.ensureChannel(context)
                    val request = OneTimeWorkRequestBuilder<PhotoExportWorker>()
                        .setInputData(
                            workDataOf(
                                PhotoExportWorker.KEY_PROJECT to project.id,
                                PhotoExportWorker.KEY_FORMAT to format.name.lowercase(),
                                PhotoExportWorker.KEY_QUALITY to quality,
                                PhotoExportWorker.KEY_KEEP_EXIF to keepExif,
                                PhotoExportWorker.KEY_STRIP_GPS to true
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
            onDismiss = { if (!exporting) showExport = false }
        )
    }
}

private data class RailItem(val label: Int, val tag: String, val mode: SessionMode)

private fun defaultMode(toolKey: String): SessionMode = when (toolKey) {
    "layers" -> SessionMode.Layers
    "restore" -> SessionMode.Adjust
    "expand" -> SessionMode.Expand
    "cutout" -> SessionMode.Erase
    "stickers" -> SessionMode.Stickers
    "collage" -> SessionMode.Collage
    else -> SessionMode.Crop
}

private fun railsFor(toolKey: String): List<RailItem> = when (toolKey) {
    "layers" -> listOf(
        RailItem(R.string.studio_layers, "session_layers", SessionMode.Layers),
        RailItem(R.string.studio_mask, "session_mask", SessionMode.Mask)
    )
    "restore" -> listOf(
        RailItem(R.string.studio_adjust, "session_adjust", SessionMode.Adjust),
        RailItem(R.string.studio_heal, "session_heal", SessionMode.Heal),
        RailItem(R.string.studio_fill, "session_fill", SessionMode.Fill)
    )
    "expand" -> listOf(RailItem(R.string.studio_expand_tool, "session_expand", SessionMode.Expand))
    "cutout" -> listOf(
        RailItem(R.string.studio_erase, "session_erase", SessionMode.Erase)
    )
    "stickers" -> listOf(
        RailItem(R.string.studio_stickers, "session_stickers", SessionMode.Stickers),
        RailItem(R.string.studio_text, "session_text", SessionMode.Text)
    )
    "collage" -> listOf(RailItem(R.string.studio_collage, "session_collage", SessionMode.Collage))
    else -> listOf(
        RailItem(R.string.studio_crop, "session_crop", SessionMode.Crop),
        RailItem(R.string.studio_adjust, "session_adjust", SessionMode.Adjust),
        RailItem(R.string.studio_curves, "session_curves", SessionMode.Curves),
        RailItem(R.string.studio_heal, "session_heal", SessionMode.Heal),
        RailItem(R.string.studio_fill, "session_fill", SessionMode.Fill),
        RailItem(R.string.studio_text, "session_text", SessionMode.Text),
        RailItem(R.string.studio_layers, "session_layers", SessionMode.Layers),
        RailItem(R.string.studio_mask, "session_mask", SessionMode.Mask)
    )
}

@Composable
private fun ToolBody(
    mode: SessionMode,
    toolKey: String,
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
    onApplyHeal: () -> Unit,
    onApplyFill: () -> Unit,
    onApplyErase: () -> Unit,
    onApplyText: () -> Unit,
    onRestore: () -> Unit,
    onCutout: () -> Unit,
    onSticker: (String) -> Unit,
    onCollage: (Int) -> Unit,
    onAddPhoto: () -> Unit,
    onApplyCurves: (FloatArray) -> Unit,
    curveYs: FloatArray,
    onCurveYs: (FloatArray) -> Unit,
    onBlur: () -> Unit,
    onSharpen: () -> Unit,
    onVignette: () -> Unit,
    maskFill: MaskFill,
    maskReveal: Boolean,
    onMaskReveal: (Boolean) -> Unit,
    onClearMask: () -> Unit,
    onSubjectMask: () -> Unit,
    onBackgroundMask: () -> Unit,
    collageCols: Int = 2,
    pickingHealSource: Boolean = false,
    healSourceSet: Boolean = false,
    onPickHealSource: () -> Unit = {},
    onPaintHeal: () -> Unit = {}
) {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (toolKey == "restore") {
            PixiPrimaryButton(stringResource(R.string.studio_restore_now), onRestore, Modifier.testTag("session_restore"))
        }
        if (toolKey == "cutout" || toolKey == "stickers") {
            PixiPrimaryButton(stringResource(R.string.studio_cutout_now), onCutout, Modifier.testTag("session_cutout"))
        }
        when (mode) {
            SessionMode.Crop -> PixiPrimaryButton(
                stringResource(R.string.studio_apply),
                onApplyCrop,
                Modifier.testTag("session_apply_crop")
            )
            SessionMode.Adjust -> {
                LabeledSlider(stringResource(R.string.studio_exposure), exposure, -1.5f..1.5f, "session_exposure", onExposure)
                LabeledSlider(stringResource(R.string.studio_contrast), contrast, 0.4f..1.8f, "session_contrast", onContrast)
                LabeledSlider(stringResource(R.string.studio_hue), hue, -180f..180f, "session_hue", onHue)
                LabeledSlider(stringResource(R.string.studio_saturation), saturation, 0f..2f, "session_saturation", onSaturation)
                LabeledSlider(stringResource(R.string.studio_blur), blurRadius, 0f..8f, "session_blur_amount", onBlurAmount)
                LabeledSlider(stringResource(R.string.studio_sharpen), sharpen, 0f..1f, "session_sharpen_amount", onSharpenAmount)
                LabeledSlider(stringResource(R.string.studio_vignette), vignette, 0f..1f, "session_vignette_amount", onVignetteAmount)
                PixiPrimaryButton(stringResource(R.string.studio_apply), onApplyGrade)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormatChip(stringResource(R.string.studio_blur), false, "session_blur", onBlur)
                    FormatChip(stringResource(R.string.studio_sharpen), false, "session_sharpen", onSharpen)
                    FormatChip(stringResource(R.string.studio_vignette), false, "session_vignette", onVignette)
                }
            }
            SessionMode.Curves -> {
                Text(
                    stringResource(R.string.studio_curves_hint),
                    color = PixiTextMuted,
                    modifier = Modifier.testTag("session_curves_hint")
                )
                fun set(i: Int, v: Float) {
                    val next = curveYs.copyOf()
                    next[i] = v
                    onCurveYs(next)
                }
                LabeledSlider(stringResource(R.string.studio_curve_black), curveYs[0], 0f..1f, "curve_0") { set(0, it) }
                LabeledSlider(stringResource(R.string.studio_curve_shadow), curveYs[1], 0f..1f, "curve_1") { set(1, it) }
                LabeledSlider(stringResource(R.string.studio_curve_mid), curveYs[2], 0f..1f, "curve_2") { set(2, it) }
                LabeledSlider(stringResource(R.string.studio_curve_light), curveYs[3], 0f..1f, "curve_3") { set(3, it) }
                LabeledSlider(stringResource(R.string.studio_curve_white), curveYs[4], 0f..1f, "curve_4") { set(4, it) }
                PixiPrimaryButton(
                    stringResource(R.string.studio_apply),
                    { onApplyCurves(curveYs.copyOf()) },
                    Modifier.testTag("session_apply_curves")
                )
            }
            SessionMode.Expand -> {
                LabeledSlider(stringResource(R.string.studio_expand_tool), expand, 0.05f..0.35f, "session_expand_amount", onExpand)
                PixiPrimaryButton(stringResource(R.string.studio_apply), onApplyExpand, Modifier.testTag("session_apply_expand"))
            }
            SessionMode.Heal -> {
                LabeledSlider(stringResource(R.string.studio_brush), brush, 0.02f..0.12f, "session_brush_size", onBrush)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.studio_heal_set_source),
                        color = PixiText,
                        fontWeight = if (pickingHealSource) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (pickingHealSource) PixiAccent else PixiBg)
                            .clickable(onClick = onPickHealSource)
                            .testTag("session_heal_set_source")
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                    Text(
                        stringResource(R.string.studio_heal_paint),
                        color = PixiText,
                        fontWeight = if (!pickingHealSource) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (!pickingHealSource) PixiAccent else PixiBg)
                            .clickable(onClick = onPaintHeal)
                            .testTag("session_heal_paint")
                            .padding(horizontal = 14.dp, vertical = 8.dp)
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
            SessionMode.Fill -> {
                LabeledSlider(stringResource(R.string.studio_brush), brush, 0.02f..0.12f, "session_brush_size", onBrush)
                Text(stringResource(R.string.studio_fill_hint), color = PixiTextMuted)
                PixiPrimaryButton(stringResource(R.string.studio_apply), onApplyFill, Modifier.testTag("session_apply_fill"))
            }
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormatChip(
                        stringResource(R.string.studio_mask_subject),
                        maskFill == MaskFill.Subject,
                        "mask_subject",
                        onSubjectMask
                    )
                    FormatChip(
                        stringResource(R.string.studio_mask_background),
                        maskFill == MaskFill.Background,
                        "mask_background",
                        onBackgroundMask
                    )
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
                Text(stringResource(R.string.studio_mask_hint), color = PixiTextMuted)
                PixiPrimaryButton(stringResource(R.string.studio_mask_clear), onClearMask, Modifier.testTag("mask_clear"))
            }
            SessionMode.Layers -> {
                Text(stringResource(R.string.studio_layers_hint), color = PixiTextMuted)
                Text(
                    stringResource(R.string.studio_mask_hint),
                    color = PixiTextMuted,
                    modifier = Modifier.testTag("layers_panel_hint")
                )
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
        modifier = Modifier.testTag(tag)
    )
}

@Composable
private fun FormatChip(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Text(
        label,
        color = PixiText,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) PixiAccent else PixiSurface2)
            .clickable(onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 14.dp, vertical = 8.dp)
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (layer.id == selected?.id) PixiAccent else PixiSurface2)
                        .clickable { onSelect(layer.id) }
                        .testTag("layer_row_${layer.id}")
                        .padding(horizontal = 10.dp, vertical = 6.dp)
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
                TextButton(
                    onClick = {
                        onChange(layers.map {
                            if (it.id == selected.id) it.copy(visible = !it.visible) else it
                        })
                    },
                    modifier = Modifier.testTag("layer_visibility")
                ) {
                    Text(
                        if (selected.visible) stringResource(R.string.studio_hide)
                        else stringResource(R.string.studio_show),
                        color = PixiText
                    )
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
private fun RailAction(
    label: String,
    tag: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Text(
        label,
        color = PixiText,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) PixiAccent else PixiBg)
            .clickable(onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun HistogramStrip(bins: IntArray, modifier: Modifier = Modifier) {
    val clip = remember(bins) { histogramClip(bins) }
    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
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

