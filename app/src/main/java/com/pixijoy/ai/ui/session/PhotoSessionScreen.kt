package com.pixijoy.ai.ui.session

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
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
import com.pixijoy.photo.ExportFormat
import com.pixijoy.photo.PhotoOp
import com.pixijoy.photo.Stamp
import com.pixijoy.photo.decodeGraph
import com.pixijoy.photo.encodeGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SessionMode { Crop, Adjust, Heal, Erase, Text, Expand, Stickers, Collage }

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
    var ops by remember(project.id) { mutableStateOf(project.graph.decodeGraph()) }
    var redo by remember(project.id) { mutableStateOf(listOf<PhotoOp>()) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var mode by remember { mutableStateOf(defaultMode(toolKey)) }
    var crop by remember { mutableStateOf(PhotoOp.Crop(0.08f, 0.08f, 0.92f, 0.92f)) }
    var exposure by remember { mutableStateOf(0f) }
    var contrast by remember { mutableStateOf(1f) }
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var expand by remember { mutableStateOf(0.12f) }
    var brush by remember { mutableStateOf(0.05f) }
    var pendingStamps by remember { mutableStateOf(listOf<Stamp>()) }
    var lastStamp by remember { mutableStateOf<Stamp?>(null) }
    var text by remember { mutableStateOf("PixiJoy") }
    var placeX by remember { mutableStateOf(0.5f) }
    var placeY by remember { mutableStateOf(0.82f) }
    var format by remember {
        mutableStateOf(if (toolKey == "cutout" || toolKey == "stickers") ExportFormat.PNG else ExportFormat.JPEG)
    }
    var quality by remember { mutableStateOf(92) }
    var showExport by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val addPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                repo.addUri(project.id, uri)
                if (ops.none { it is PhotoOp.Collage }) {
                    ops = ops + PhotoOp.Collage(2)
                    redo = emptyList()
                }
            }
        }
    }

    LaunchedEffect(sources, ops) {
        busy = true
        val next = withContext(Dispatchers.Default) {
            engine.render(sources, ops, maxEdge = 1600)
        }
        preview?.let { if (!it.isRecycled) it.recycle() }
        preview = next
        repo.saveGraph(project.id, ops.encodeGraph())
        busy = false
    }
    DisposableEffect(Unit) {
        onDispose { preview?.let { if (!it.isRecycled) it.recycle() } }
    }

    fun push(op: PhotoOp) {
        ops = ops + op
        redo = emptyList()
        status = null
        pendingStamps = emptyList()
        lastStamp = null
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
                    if (ops.isNotEmpty()) {
                        redo = redo + ops.last()
                        ops = ops.dropLast(1)
                    }
                },
                enabled = ops.isNotEmpty(),
                modifier = Modifier.testTag("session_undo")
            ) { Text(stringResource(R.string.studio_undo), color = PixiTextMuted) }
            TextButton(
                onClick = {
                    if (redo.isNotEmpty()) {
                        ops = ops + redo.last()
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
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = project.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    when (mode) {
                        SessionMode.Crop -> CropOverlay(crop, imageRect, { crop = it }, Modifier.fillMaxSize())
                        SessionMode.Heal, SessionMode.Erase -> BrushOverlay(
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
                }
            } else {
                Text(stringResource(R.string.studio_loading), color = PixiTextMuted)
            }
        }
        status?.let {
            Text(
                it,
                color = PixiSuccess,
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
            onApplyGrade = { push(PhotoOp.Grade(exposure, contrast)); push(PhotoOp.Hsl(hue, saturation)) },
            onApplyCrop = { push(crop); mode = defaultMode(toolKey) },
            onApplyExpand = { push(PhotoOp.Expand(expand)) },
            onApplyHeal = { if (pendingStamps.isNotEmpty()) push(PhotoOp.Heal(pendingStamps)) },
            onApplyErase = { if (pendingStamps.isNotEmpty()) push(PhotoOp.Erase(pendingStamps)) },
            onApplyText = { push(PhotoOp.TextLayer(text, placeX, placeY, 0.08f, 0xFFFFFFFF.toInt())) },
            onRestore = { push(PhotoOp.Restore) },
            onCutout = { push(PhotoOp.Cutout); format = ExportFormat.PNG },
            onSticker = { glyph -> push(PhotoOp.Sticker(glyph, placeX, placeY, 0.18f)) },
            onCollage = { cols -> push(PhotoOp.Collage(cols)) },
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
                RailAction(stringResource(item.label), item.tag) { mode = item.mode }
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
            formats = listOf(
                "jpeg" to "JPEG",
                "png" to "PNG",
                "webp" to "WebP"
            ),
            selectedKey = format.name.lowercase(),
            quality = quality,
            showQuality = format != ExportFormat.PNG,
            destination = stringResource(R.string.studio_export_destination_pictures),
            exporting = exporting,
            onSelectFormat = { key ->
                format = when (key) {
                    "png" -> ExportFormat.PNG
                    "webp" -> ExportFormat.WEBP
                    else -> ExportFormat.JPEG
                }
            },
            onQuality = { quality = it },
            onExport = {
                if (exporting) return@ExportSheet
                exporting = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        engine.export(
                            context,
                            sources,
                            ops,
                            "PixiJoy_${project.title}_${System.currentTimeMillis()}.${format.extension}",
                            format,
                            quality
                        )
                    }
                    exporting = false
                    showExport = false
                    status = when (result) {
                        is AppResult.Ok -> context.getString(R.string.studio_export_ok)
                        is AppResult.Err -> result.message
                    }
                }
            },
            onDismiss = { if (!exporting) showExport = false }
        )
    }
}

private data class RailItem(val label: Int, val tag: String, val mode: SessionMode)

private fun defaultMode(toolKey: String): SessionMode = when (toolKey) {
    "restore" -> SessionMode.Adjust
    "expand" -> SessionMode.Expand
    "cutout" -> SessionMode.Erase
    "stickers" -> SessionMode.Stickers
    "collage" -> SessionMode.Collage
    else -> SessionMode.Crop
}

private fun railsFor(toolKey: String): List<RailItem> = when (toolKey) {
    "restore" -> listOf(
        RailItem(R.string.studio_adjust, "session_adjust", SessionMode.Adjust),
        RailItem(R.string.studio_heal, "session_heal", SessionMode.Heal)
    )
    "expand" -> listOf(RailItem(R.string.studio_expand_tool, "session_expand", SessionMode.Expand))
    "cutout" -> listOf(RailItem(R.string.studio_erase, "session_erase", SessionMode.Erase))
    "stickers" -> listOf(
        RailItem(R.string.studio_stickers, "session_stickers", SessionMode.Stickers),
        RailItem(R.string.studio_text, "session_text", SessionMode.Text)
    )
    "collage" -> listOf(RailItem(R.string.studio_collage, "session_collage", SessionMode.Collage))
    else -> listOf(
        RailItem(R.string.studio_crop, "session_crop", SessionMode.Crop),
        RailItem(R.string.studio_adjust, "session_adjust", SessionMode.Adjust),
        RailItem(R.string.studio_heal, "session_heal", SessionMode.Heal),
        RailItem(R.string.studio_text, "session_text", SessionMode.Text)
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
    onApplyCrop: () -> Unit,
    onApplyExpand: () -> Unit,
    onApplyHeal: () -> Unit,
    onApplyErase: () -> Unit,
    onApplyText: () -> Unit,
    onRestore: () -> Unit,
    onCutout: () -> Unit,
    onSticker: (String) -> Unit,
    onCollage: (Int) -> Unit,
    onAddPhoto: () -> Unit
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
                PixiPrimaryButton(stringResource(R.string.studio_apply), onApplyGrade)
            }
            SessionMode.Expand -> {
                LabeledSlider(stringResource(R.string.studio_expand_tool), expand, 0.05f..0.35f, "session_expand_amount", onExpand)
                PixiPrimaryButton(stringResource(R.string.studio_apply), onApplyExpand, Modifier.testTag("session_apply_expand"))
            }
            SessionMode.Heal -> {
                LabeledSlider(stringResource(R.string.studio_brush), brush, 0.02f..0.12f, "session_brush_size", onBrush)
                PixiPrimaryButton(stringResource(R.string.studio_apply), onApplyHeal, Modifier.testTag("session_apply_heal"))
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
            SessionMode.Collage -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormatChip("2-up", false, "collage_2") { onCollage(2) }
                    FormatChip("4-up", false, "collage_4") { onCollage(2) }
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
private fun RailAction(label: String, tag: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.testTag(tag)) {
        Text(label, color = PixiText, fontWeight = FontWeight.SemiBold)
    }
}

