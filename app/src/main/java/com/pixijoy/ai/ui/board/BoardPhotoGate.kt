package com.pixijoy.ai.ui.board

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardTertiaryButton
import com.pixijoy.ai.ui.components.rememberPhotoUriHandler
import com.pixijoy.ai.ui.theme.PixiSpacing

/**
 * Ensures a photo is in [BoardMediaSession] before [onReady].
 * Offers camera (flip to selfie in the system camera) or gallery.
 */
@Composable
fun rememberBoardPhotoGate(
    onCameraDenied: (() -> Unit)? = null,
    onReady: () -> Unit
): () -> Unit {
    var showChooser by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(BoardMediaSession.revision) }
    val handler = rememberPhotoUriHandler(
        onPhotoUri = { uri ->
            BoardMediaSession.setPhotoUri(uri)
            revision = BoardMediaSession.revision
            showChooser = false
            onReady()
        },
        onCameraPermissionDenied = {
            showChooser = false
            onCameraDenied?.invoke()
        }
    )
    if (showChooser) {
        BoardPhotoChoiceDialog(
            onCamera = handler.openCamera,
            onGallery = handler.openGallery,
            onDismiss = { showChooser = false }
        )
    }
    return {
        revision = BoardMediaSession.revision
        if (BoardMediaSession.hasPhoto()) {
            onReady()
        } else {
            showChooser = true
        }
    }
}

/** On-tool sample so Maestro and first-run users can try a job without the gallery. */
@Composable
fun BoardUseSampleButton(
    @androidx.annotation.DrawableRes sampleRes: Int,
    onReady: () -> Unit,
    modifier: Modifier = Modifier
) {
    com.pixijoy.ai.ui.board.components.BoardOutlinedButton(
        text = stringResource(R.string.board_c00_use_sample),
        onClick = {
            BoardMediaSession.setSampleRes(sampleRes)
            onReady()
        },
        modifier = modifier.testTag(TestTags.TOOL_USE_SAMPLE)
    )
}

@Composable
fun BoardPhotoChoiceDialog(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_a_photo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                Text(stringResource(R.string.photo_gate_body))
                BoardPrimaryButton(
                    text = stringResource(R.string.take_photo),
                    onClick = {
                        onDismiss()
                        onCamera()
                    }
                )
                BoardOutlinedButton(
                    text = stringResource(R.string.photo_gallery),
                    onClick = {
                        onDismiss()
                        onGallery()
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            BoardTertiaryButton(text = stringResource(R.string.cancel), onClick = onDismiss)
        }
    )
}

/** Take photo (camera, flip for selfie) + Gallery. */
@Composable
fun BoardCapturePhotoRow(
    onPicked: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    onCameraDenied: () -> Unit = {},
    galleryTestTag: String? = TestTags.FLOW2_ADD_PHOTO,
    selfieTestTag: String? = null,
    cameraTestTag: String? = null
) {
    val handler = rememberPhotoUriHandler(
        onPhotoUri = onPicked,
        onCameraPermissionDenied = onCameraDenied
    )
    @Suppress("UNUSED_PARAMETER")
    val unusedSelfieTag = selfieTestTag
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
    ) {
        BoardOutlinedButton(
            text = stringResource(R.string.take_photo),
            onClick = handler.openCamera,
            modifier = Modifier
                .weight(1f)
                .then(if (cameraTestTag != null) Modifier.testTag(cameraTestTag) else Modifier)
        )
        BoardOutlinedButton(
            text = stringResource(R.string.gallery),
            onClick = handler.openGallery,
            modifier = Modifier
                .weight(1f)
                .then(if (galleryTestTag != null) Modifier.testTag(galleryTestTag) else Modifier)
        )
    }
}

@Composable
fun BoardAddPhotoButton(
    modifier: Modifier = Modifier,
    labelRes: Int = R.string.home_add_photo,
    onPicked: () -> Unit = {},
    onCameraDenied: () -> Unit = {}
) {
    var showChooser by remember { mutableStateOf(false) }
    val handler = rememberPhotoUriHandler(
        onPhotoUri = { uri ->
            BoardMediaSession.setPhotoUri(uri)
            showChooser = false
            onPicked()
        },
        onCameraPermissionDenied = {
            showChooser = false
            onCameraDenied()
        }
    )
    com.pixijoy.ai.ui.board.components.BoardPrimaryButton(
        text = stringResource(labelRes),
        onClick = { showChooser = true },
        modifier = modifier.fillMaxWidth()
    )
    if (showChooser) {
        BoardPhotoChoiceDialog(
            onCamera = handler.openCamera,
            onGallery = handler.openGallery,
            onDismiss = { showChooser = false }
        )
    }
}
