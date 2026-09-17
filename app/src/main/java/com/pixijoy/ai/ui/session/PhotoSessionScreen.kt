package com.pixijoy.ai.ui.session

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.photo.BitmapPhotoEngine
import com.pixijoy.photo.PhotoOp
import com.pixijoy.photo.decodeGraph
import com.pixijoy.photo.encodeGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SessionTool { Crop, Adjust }

@Composable
fun PhotoSessionScreen(
    project: Project,
    toolLabel: String,
    repo: ProjectRepository,
    engine: BitmapPhotoEngine = remember { BitmapPhotoEngine() },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val source = project.source?.path ?: project.coverPath
    var ops by remember(project.id) { mutableStateOf(project.graph.decodeGraph()) }
    var redo by remember(project.id) { mutableStateOf(listOf<PhotoOp>()) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var cropMode by remember { mutableStateOf(false) }
    var crop by remember { mutableStateOf(PhotoOp.Crop(0.08f, 0.08f, 0.92f, 0.92f)) }
    var rail by remember { mutableStateOf(SessionTool.Crop) }
    var exposure by remember { mutableStateOf(0f) }
    var contrast by remember { mutableStateOf(1f) }
    var status by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }

    LaunchedEffect(source, ops) {
        val next = withContext(Dispatchers.Default) {
            engine.render(source, ops, maxEdge = 2048)
        }
        preview?.let { if (!it.isRecycled) it.recycle() }
        preview = next
        repo.saveGraph(project.id, ops.encodeGraph())
    }
    DisposableEffect(Unit) {
        onDispose { preview?.let { if (!it.isRecycled) it.recycle() } }
    }

    fun push(op: PhotoOp) {
        ops = ops + op
        redo = emptyList()
        status = null
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
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, modifier = Modifier.testTag("session_back")) {
                Text(stringResource(R.string.studio_back), color = PixiText)
            }
            Text(
                toolLabel,
                color = PixiText,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
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
                    if (cropMode) {
                        CropOverlay(
                            crop = crop,
                            imageRect = imageRect,
                            onChange = { crop = it },
                            modifier = Modifier.fillMaxSize()
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
                color = PixiSuccess,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("session_status")
            )
        }
        if (rail == SessionTool.Adjust) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(stringResource(R.string.studio_exposure), color = PixiTextMuted)
                Slider(
                    value = exposure,
                    onValueChange = { exposure = it },
                    valueRange = -1.5f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("session_exposure")
                )
                Text(stringResource(R.string.studio_contrast), color = PixiTextMuted)
                Slider(
                    value = contrast,
                    onValueChange = { contrast = it },
                    valueRange = 0.4f..1.8f,
                    colors = SliderDefaults.colors(thumbColor = PixiAccent, activeTrackColor = PixiAccent),
                    modifier = Modifier.testTag("session_contrast")
                )
                PixiPrimaryButton(
                    text = stringResource(R.string.studio_apply),
                    onClick = { push(PhotoOp.Grade(exposure, contrast)) }
                )
            }
        }
        if (cropMode) {
            PixiPrimaryButton(
                text = stringResource(R.string.studio_apply),
                onClick = {
                    push(crop)
                    cropMode = false
                },
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("session_apply_crop")
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RailAction(stringResource(R.string.studio_crop), "session_crop") {
                rail = SessionTool.Crop
                cropMode = true
            }
            RailAction(stringResource(R.string.studio_rotate), "session_rotate") {
                cropMode = false
                push(PhotoOp.Rotate(90))
            }
            RailAction(stringResource(R.string.studio_flip), "session_flip") {
                cropMode = false
                push(PhotoOp.FlipH)
            }
            RailAction(stringResource(R.string.studio_adjust), "session_adjust") {
                cropMode = false
                rail = SessionTool.Adjust
            }
        }
        PixiPrimaryButton(
            text = if (exporting) stringResource(R.string.studio_exporting) else stringResource(R.string.studio_export),
            onClick = {
                if (exporting) return@PixiPrimaryButton
                exporting = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        engine.exportJpeg(
                            context,
                            source,
                            ops,
                            "PixiJoy_${project.title}_${System.currentTimeMillis()}.jpg"
                        )
                    }
                    exporting = false
                    status = when (result) {
                        is AppResult.Ok -> context.getString(R.string.studio_export_ok)
                        is AppResult.Err -> result.message
                    }
                }
            },
            enabled = !exporting,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("session_export")
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RailAction(label: String, tag: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.testTag(tag)) {
        Text(label, color = PixiText, fontWeight = FontWeight.SemiBold)
    }
}
