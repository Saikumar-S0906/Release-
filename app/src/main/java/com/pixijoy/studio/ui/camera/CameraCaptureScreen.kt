package com.pixijoy.studio.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pixijoy.studio.R
import com.pixijoy.studio.ui.StudioDirectedRow
import com.pixijoy.studio.ui.StudioPosture
import com.pixijoy.studio.ui.rememberStudioFrame
import com.pixijoy.studio.ui.sessionHingeStartDp
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiError
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "PixiCamera"

enum class CaptureMode { PHOTO, VIDEO }

@Composable
fun CameraCaptureScreen(
    initialVideo: Boolean,
    onStill: (File) -> Unit,
    onVideo: (File) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { ContextCompat.getMainExecutor(context) }
    var mode by remember { mutableStateOf(if (initialVideo) CaptureMode.VIDEO else CaptureMode.PHOTO) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var cameraGranted by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.CAMERA))
    }
    val audioGranted = context.hasPermission(Manifest.permission.RECORD_AUDIO)
    var recording by remember { mutableStateOf(false) }
    var videoQuality by remember { mutableStateOf(Quality.HD) }
    var error by remember { mutableStateOf<String?>(null) }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        cameraGranted = result[Manifest.permission.CAMERA] == true ||
            context.hasPermission(Manifest.permission.CAMERA)
    }

    LaunchedEffect(Unit) {
        if (!cameraGranted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
        provider = awaitCameraProvider(context, executor)
    }

    LaunchedEffect(provider, cameraGranted, mode, lensFacing, videoQuality) {
        val cameraProvider = provider ?: return@LaunchedEffect
        if (!cameraGranted) return@LaunchedEffect
        cameraProvider.unbindAll()
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        try {
            if (mode == CaptureMode.PHOTO) {
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = capture
                videoCapture = null
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            } else {
                val recorder = Recorder.Builder()
                    .setQualitySelector(
                        QualitySelector.from(
                            videoQuality,
                            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                        )
                    )
                    .build()
                val capture = VideoCapture.withOutput(recorder)
                videoCapture = capture
                imageCapture = null
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            }
            error = null
        } catch (t: Throwable) {
            Log.w(TAG, "bind failed", t)
            error = t.localizedMessage
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activeRecording?.stop()
            provider?.unbindAll()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixiBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("camera_screen")
    ) {
        if (!cameraGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.studio_camera_permission), color = PixiText)
                PixiPrimaryButton(
                    text = stringResource(R.string.studio_camera_grant),
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        )
                    },
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .testTag("camera_permission")
                )
                TextButton(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("camera_close")
                ) {
                    Text(stringResource(R.string.studio_cancel), color = PixiTextMuted)
                }
            }
        } else {
            val frame = rememberStudioFrame()
            val density = LocalDensity.current
            var origin by remember { mutableStateOf(Offset.Zero) }
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { origin = it.positionInWindow() }
            ) {
                val paneW = maxWidth.value.toInt().coerceAtLeast(1)
                val paneH = maxHeight.value.toInt().coerceAtLeast(1)
                val hingeStart = sessionHingeStartDp(
                    fold = frame.fold,
                    posture = frame.posture,
                    originXPx = origin.x,
                    originYPx = origin.y,
                    pxToDp = { px -> with(density) { px.toDp().value } },
                    paneWidthDp = paneW,
                    paneHeightDp = paneH
                ).dp
                val hingeGap = if (frame.posture == StudioPosture.Flat) 0.dp else frame.hinge
                val preview: @Composable BoxScope.() -> Unit = {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("camera_preview")
                    )
                    TextButton(
                        onClick = onClose,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .testTag("camera_close")
                    ) {
                        Text(stringResource(R.string.studio_back), color = PixiText, fontWeight = FontWeight.SemiBold)
                    }
                }
                val controls: @Composable (Boolean, Boolean) -> Unit = { narrow, overlay ->
                    CameraControlColumn(
                        narrow = narrow,
                        overlay = overlay,
                        error = error,
                        mode = mode,
                        recording = recording,
                        videoQuality = videoQuality,
                        onMode = { mode = it },
                        onQuality = { videoQuality = it },
                        onFlip = {
                            if (!recording) {
                                lensFacing =
                                    if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                        CameraSelector.LENS_FACING_FRONT
                                    } else {
                                        CameraSelector.LENS_FACING_BACK
                                    }
                            }
                        },
                        onShutter = shutter@{
                            if (mode == CaptureMode.PHOTO) {
                                takePhoto(
                                    context.cacheDir,
                                    imageCapture,
                                    executor,
                                    onStill
                                ) { error = it }
                            } else {
                                val current = activeRecording
                                if (current != null) {
                                    current.stop()
                                    activeRecording = null
                                    recording = false
                                } else {
                                    val capture = videoCapture ?: return@shutter
                                    val file = File(
                                        context.cacheDir,
                                        "capture-${System.currentTimeMillis()}.mp4"
                                    )
                                    val pending = capture.output.prepareRecording(
                                        context,
                                        FileOutputOptions.Builder(file).build()
                                    )
                                    val prepared = try {
                                        if (audioGranted) pending.withAudioEnabled() else pending
                                    } catch (_: SecurityException) {
                                        pending
                                    }
                                    activeRecording = prepared.start(executor) { event ->
                                        if (event is VideoRecordEvent.Finalize) {
                                            recording = false
                                            activeRecording = null
                                            if (!event.hasError()) onVideo(file)
                                            else {
                                                error = event.cause?.localizedMessage
                                                    ?: "Could not record video."
                                            }
                                        }
                                    }
                                    recording = true
                                }
                            }
                        }
                    )
                }
                when {
                    frame.posture == StudioPosture.Tabletop && hingeStart > 0.dp -> {
                        Column(Modifier.fillMaxSize()) {
                            Box(Modifier.height(hingeStart).fillMaxWidth()) { preview() }
                            Spacer(
                                Modifier
                                    .height(hingeGap)
                                    .fillMaxWidth()
                                    .testTag("camera_hinge_gap")
                            )
                            Box(
                                Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) { controls(false, false) }
                        }
                    }
                    frame.posture == StudioPosture.Book && hingeStart > 0.dp -> {
                        StudioDirectedRow(
                            physicalStart = true,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(Modifier.width(hingeStart).fillMaxHeight()) { preview() }
                            Spacer(
                                Modifier
                                    .width(hingeGap)
                                    .fillMaxHeight()
                                    .testTag("camera_hinge_gap")
                            )
                            Box(
                                Modifier.weight(1f).fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) { controls(true, false) }
                        }
                    }
                    else -> {
                        val sideControls = maxHeight < 480.dp && maxWidth > maxHeight
                        Box(Modifier.fillMaxSize()) {
                            preview()
                            Box(
                                Modifier.align(
                                    if (sideControls) Alignment.CenterEnd else Alignment.BottomCenter
                                )
                            ) { controls(sideControls, true) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraControlColumn(
    narrow: Boolean,
    overlay: Boolean,
    error: String?,
    mode: CaptureMode,
    recording: Boolean,
    videoQuality: Quality,
    onMode: (CaptureMode) -> Unit,
    onQuality: (Quality) -> Unit,
    onFlip: () -> Unit,
    onShutter: () -> Unit
) {
    Column(
        modifier = Modifier
            .then(
                if (narrow) {
                    Modifier
                        .widthIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                } else {
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                }
            )
            .then(
                if (overlay) {
                    Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(PixiBg.copy(alpha = 0.78f))
                } else {
                    Modifier
                }
            )
            .padding(if (narrow) 8.dp else 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        error?.let { Text(it, color = PixiError) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            ModeChip(
                label = stringResource(R.string.studio_camera_photo),
                selected = mode == CaptureMode.PHOTO,
                tag = "camera_mode_photo",
                enabled = !recording
            ) { onMode(CaptureMode.PHOTO) }
            ModeChip(
                label = stringResource(R.string.studio_camera_video),
                selected = mode == CaptureMode.VIDEO,
                tag = "camera_mode_video",
                enabled = !recording
            ) { onMode(CaptureMode.VIDEO) }
        }
        if (mode == CaptureMode.VIDEO && !recording) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf(
                    Quality.SD to "SD",
                    Quality.HD to "HD",
                    Quality.FHD to "FHD"
                ).forEach { (quality, label) ->
                    ModeChip(
                        label = label,
                        selected = videoQuality == quality,
                        tag = "camera_quality_$label",
                        enabled = true
                    ) { onQuality(quality) }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onFlip,
                modifier = Modifier.testTag("camera_flip")
            ) {
                Text(stringResource(R.string.studio_camera_flip), color = PixiText, maxLines = 1)
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(if (recording) PixiError else PixiAccent)
                    .clickable(onClick = onShutter)
                    .semantics { role = Role.Button }
                    .testTag("camera_shutter")
            )
            Text(
                if (recording) stringResource(R.string.studio_camera_stop) else " ",
                color = PixiText,
                maxLines = 1,
                modifier = Modifier.testTag("camera_rec_label")
            )
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    tag: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.testTag(tag)) {
        Text(
            label,
            color = if (selected) PixiAccent else PixiTextMuted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun takePhoto(
    cacheDir: File,
    imageCapture: ImageCapture?,
    executor: Executor,
    onStill: (File) -> Unit,
    onError: (String) -> Unit
) {
    val capture = imageCapture ?: return
    val file = File(cacheDir, "capture-${System.currentTimeMillis()}.jpg")
    capture.takePicture(
        ImageCapture.OutputFileOptions.Builder(file).build(),
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onStill(file)
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception.localizedMessage ?: "Could not take photo.")
            }
        }
    )
}

private suspend fun awaitCameraProvider(
    context: Context,
    executor: Executor
): ProcessCameraProvider? = suspendCancellableCoroutine { cont ->
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener(
        {
            if (!cont.isActive) return@addListener
            runCatching { future.get() }
                .onSuccess { cont.resume(it) }
                .onFailure { cont.resume(null) }
        },
        executor
    )
}
