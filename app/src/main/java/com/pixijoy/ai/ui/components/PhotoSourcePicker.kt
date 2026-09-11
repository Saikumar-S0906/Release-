package com.pixijoy.ai.ui.components

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.components.BoardSecondaryButton
import java.io.File

/**
 * Camera (selfie or any subject) + gallery picker. Returns a content Uri for either path.
 */
@Composable
fun rememberPhotoUriHandler(
    onPhotoUri: (Uri) -> Unit,
    onCameraPermissionDenied: (() -> Unit)? = null
): PhotoUriHandler {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { picked ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    picked,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onPhotoUri(picked)
        }
    }

    val filesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { picked ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    picked,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onPhotoUri(picked)
        }
    }

    var pendingFront by remember { mutableStateOf(false) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        TakePictureWithFacing()
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        pendingFront = false
        if (success && uri != null) onPhotoUri(uri)
    }

    fun launchCapture(front: Boolean) {
        val uri = createCaptureUri(context) ?: return
        pendingCameraUri = uri
        pendingFront = front
        takePictureLauncher.launch(uri to front)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCapture(pendingFront)
        } else {
            onCameraPermissionDenied?.invoke()
        }
    }

    fun openCameraFacing(front: Boolean) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchCapture(front)
        } else {
            pendingFront = front
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    return PhotoUriHandler(
        openGallery = { galleryLauncher.launch(arrayOf("image/*")) },
        openCamera = { openCameraFacing(front = false) },
        openSelfie = { openCameraFacing(front = true) },
        openFiles = { filesLauncher.launch(arrayOf("image/*")) },
        openAppSettings = {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    )
}

class PhotoUriHandler(
    val openGallery: () -> Unit,
    val openCamera: () -> Unit,
    val openSelfie: () -> Unit,
    val openFiles: () -> Unit = {},
    val openAppSettings: () -> Unit = {}
)

/**
 * System camera capture. [Boolean] true requests the front lens (selfie) via OEM extras.
 */
private class TakePictureWithFacing : ActivityResultContract<Pair<Uri, Boolean>, Boolean>() {
    override fun createIntent(context: Context, input: Pair<Uri, Boolean>): Intent {
        val (uri, front) = input
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("", uri)
            if (front) {
                putExtra("android.intent.extras.CAMERA_FACING", 1)
                putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            }
            context.grantUriPermissionToCameras(uri)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK
}

private fun Context.grantUriPermissionToCameras(uri: Uri) {
    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).forEach { info ->
        grantUriPermission(
            info.activityInfo.packageName,
            uri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

private fun createCaptureUri(context: android.content.Context): Uri? {
    return runCatching {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}

@Composable
fun PhotoSourceChooser(
    handler: PhotoUriHandler,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color,
    hint: String = stringResource(R.string.take_or_pick_photo)
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(hint, color = contentColor, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoSourceTile(
                    modifier = Modifier.weight(1f).height(80.dp),
                    onClick = handler.openCamera,
                    icon = {
                        Icon(painter = painterResource(R.drawable.pj_ic_camera),
                            contentDescription = stringResource(R.string.cd_take_photo),
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    label = stringResource(R.string.photo_take),
                    containerColor = accentColor.copy(alpha = 0.15f),
                    contentColor = contentColor
                )
                PhotoSourceTile(
                    modifier = Modifier.weight(1f).height(80.dp),
                    onClick = handler.openGallery,
                    icon = {
                        Icon(painter = painterResource(R.drawable.pj_ic_gallery),
                            contentDescription = stringResource(R.string.cd_pick_from_gallery),
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    label = stringResource(R.string.photo_gallery),
                    containerColor = accentColor.copy(alpha = 0.15f),
                    contentColor = contentColor
                )
                PhotoSourceTile(
                    modifier = Modifier.weight(1f).height(80.dp),
                    onClick = handler.openFiles,
                    icon = {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.pj_ic_download),
                            contentDescription = stringResource(R.string.photo_files),
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    label = stringResource(R.string.photo_files),
                    containerColor = accentColor.copy(alpha = 0.15f),
                    contentColor = contentColor
                )
            }
        }
    }
}

/** Compact Camera + Gallery actions for tool screens that use buttons instead of empty-state cards. */
@Composable
fun PhotoSourceButtons(
    handler: PhotoUriHandler,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BoardSecondaryButton(
            text = stringResource(R.string.take_photo),
            onClick = handler.openCamera,
            modifier = Modifier.weight(1f),
            enabled = enabled
        )
        BoardSecondaryButton(
            text = stringResource(R.string.gallery),
            onClick = handler.openGallery,
            modifier = Modifier.weight(1f),
            enabled = enabled
        )
    }
}

@Composable
fun ChangePhotoButton(
    handler: PhotoUriHandler,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.Unspecified
) {
    var showChoices by remember { mutableStateOf(false) }
    TextButton(onClick = { showChoices = true }, modifier = modifier) {
        Text(
            stringResource(R.string.change_photo),
            color = if (contentColor == Color.Unspecified) Color.Unspecified else contentColor
        )
    }
    if (showChoices) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showChoices = false },
            title = { Text(stringResource(R.string.choose_a_photo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        showChoices = false
                        handler.openCamera()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.take_photo))
                    }
                    TextButton(onClick = {
                        showChoices = false
                        handler.openGallery()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.photo_gallery))
                    }
                    TextButton(onClick = {
                        showChoices = false
                        handler.openFiles()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.photo_files))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showChoices = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun PhotoSourceTile(
    modifier: Modifier,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(Modifier.height(4.dp))
            Text(label, color = contentColor, style = MaterialTheme.typography.labelLarge)
        }
    }
}
