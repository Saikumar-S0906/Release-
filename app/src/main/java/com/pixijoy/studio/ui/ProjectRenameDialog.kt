package com.pixijoy.studio.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.window.DialogProperties
import com.pixijoy.studio.R
import com.pixijoy.studio.ui.session.renameBackShouldDismiss
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
    var fieldFocused by rememberSaveable(titleKey) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    // AlertDialog is its own window. MainActivity's testTagsAsResourceId does not apply here,
    // same as ExportSheet and CreateSourceSheet. The flag is not spoken by TalkBack.
    // Back while the field is focused only clears it. hideKeyboard is a Back key, and the
    // default dismiss would drop the dialog before confirm can be tapped.
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = true),
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag("project_rename_sheet"),
        containerColor = PixiSurface2,
        title = { Text(stringResource(R.string.studio_library_rename), color = PixiText) },
        text = {
            RenameFieldBack(
                fieldFocused = fieldFocused,
                focusManager = focusManager,
                keyboard = keyboard,
                onDismiss = onDismiss
            )
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { fieldFocused = it.isFocused }
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
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("library_rename_dismiss")
            ) {
                Text(stringResource(R.string.studio_cancel), color = PixiTextMuted)
            }
        }
    )
}

@Composable
private fun RenameFieldBack(
    fieldFocused: Boolean,
    focusManager: FocusManager,
    keyboard: SoftwareKeyboardController?,
    onDismiss: () -> Unit
) {
    BackHandler {
        if (renameBackShouldDismiss(fieldFocused)) {
            onDismiss()
        } else {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }
}
