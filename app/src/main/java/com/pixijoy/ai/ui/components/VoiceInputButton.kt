package com.pixijoy.ai.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pixijoy.ai.R
import com.pixijoy.ai.util.MediaPermissionHelper
import java.util.Locale

@Composable
fun VoiceInputButton(
    onResult: (String) -> Unit,
    onPermissionDenied: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingSpeech by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        pendingSpeech = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrBlank()) onResult(spoken)
        }
    }

    val launchSpeechRecognizer: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.speech_prompt))
        }
        runCatching { speechLauncher.launch(intent) }
            .onFailure {
                pendingSpeech = false
                onPermissionDenied()
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingSpeech) {
            pendingSpeech = false
            launchSpeechRecognizer()
        } else if (!granted) {
            pendingSpeech = false
            onPermissionDenied()
        }
    }

    IconButton(
        onClick = {
            if (MediaPermissionHelper.hasSpeechPermission(context)) {
                launchSpeechRecognizer()
            } else {
                pendingSpeech = true
                permissionLauncher.launch(MediaPermissionHelper.speechPermission())
            }
        },
        modifier = modifier
    ) {
        Icon(painter = painterResource(R.drawable.pj_ic_mic),
            contentDescription = stringResource(R.string.cd_voice_input)
        )
    }
}
