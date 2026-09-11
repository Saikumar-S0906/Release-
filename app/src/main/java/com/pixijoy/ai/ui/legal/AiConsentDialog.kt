package com.pixijoy.ai.ui.legal

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pixijoy.ai.R

@Composable
fun AiConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Require explicit Accept or On-device — no silent decline */ },
        title = { Text(stringResource(R.string.ai_consent_title)) },
        text = { Text(stringResource(R.string.ai_consent_message)) },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(stringResource(R.string.ai_consent_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(stringResource(R.string.ai_consent_decline))
            }
        }
    )
}
