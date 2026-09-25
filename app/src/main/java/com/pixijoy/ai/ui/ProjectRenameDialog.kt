package com.pixijoy.ai.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiSurface2
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ProjectRenameDialog(
    titleKey: String,
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var draft by rememberSaveable(titleKey) { mutableStateOf(initialTitle) }
    // AlertDialog is its own window. MainActivity's testTagsAsResourceId does not apply here,
    // same as ExportSheet. Without this, library_rename_* never become resource-ids.
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        containerColor = PixiSurface2,
        title = { Text(stringResource(R.string.studio_library_rename), color = PixiText) },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_rename_field")
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(draft) },
                modifier = Modifier.testTag("library_rename_confirm")
            ) {
                Text(stringResource(R.string.studio_apply), color = PixiAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.studio_cancel), color = PixiTextMuted)
            }
        }
    )
}
