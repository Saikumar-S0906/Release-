package com.pixijoy.ai.ui.session

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.ml.pack.PackCatalog
import com.pixijoy.ml.pack.PackId
import com.pixijoy.ml.pack.PackInstallManager
import com.pixijoy.ml.pack.PackInstallState
import com.pixijoy.ml.pack.PackRuntime
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PackStatusRow(
    packId: PackId,
    statusTag: String,
    downloadTag: String
) {
    val context = LocalContext.current
    val manager = remember(context) { PackInstallManager.get(context) }
    val state by manager.observe(packId).collectAsState()
    val scope = rememberCoroutineScope()
    val engineReady = PackRuntime.engineReady(packId)
    val pickPack = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val spec = PackCatalog.spec(packId)
            val tmp = File(context.cacheDir, "ml_sideload_${packId.name}_${spec.fileName}")
            try {
                val copied = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tmp.outputStream().use { output -> input.copyTo(output) }
                        tmp
                    }
                }
                if (copied != null) {
                    manager.sideload(packId, copied)
                }
            } finally {
                tmp.delete()
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            packStatusCopy(state, engineReady),
            color = PixiTextMuted,
            modifier = Modifier.testTag(statusTag)
        )
        val installing = state as? PackInstallState.Installing
        if (installing != null) {
            LinearProgressIndicator(
                progress = { installing.progress },
                modifier = Modifier.testTag("${statusTag}_progress")
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (state) {
                is PackInstallState.Missing,
                is PackInstallState.Failed,
                is PackInstallState.StorageError -> {
                    TextButton(
                        onClick = { scope.launch { manager.install(packId) } },
                        modifier = Modifier.testTag(downloadTag)
                    ) {
                        Text(
                            stringResource(
                                if (state is PackInstallState.Missing) R.string.studio_pack_download
                                else R.string.studio_pack_retry
                            ),
                            color = PixiTextMuted
                        )
                    }
                }
                is PackInstallState.Ready -> {
                    TextButton(
                        onClick = { scope.launch { manager.uninstall(packId) } },
                        modifier = Modifier.testTag("${downloadTag}_remove")
                    ) {
                        Text(stringResource(R.string.studio_pack_remove), color = PixiTextMuted)
                    }
                }
                is PackInstallState.Installing -> Unit
            }
            if (state !is PackInstallState.Installing) {
                TextButton(
                    onClick = { pickPack.launch("*/*") },
                    modifier = Modifier.testTag("${downloadTag}_from_file")
                ) {
                    Text(stringResource(R.string.studio_pack_from_file), color = PixiTextMuted)
                }
            }
        }
    }
}

@Composable
private fun packStatusCopy(state: PackInstallState, engineReady: Boolean): String = when (state) {
    PackInstallState.Missing -> stringResource(R.string.studio_pack_missing)
    is PackInstallState.Installing -> stringResource(R.string.studio_pack_installing)
    is PackInstallState.Ready -> if (engineReady) {
        stringResource(R.string.studio_pack_engine_ready)
    } else {
        stringResource(R.string.studio_pack_ready_unbound)
    }
    is PackInstallState.Failed -> state.message
    is PackInstallState.StorageError -> stringResource(R.string.studio_pack_storage, state.message)
}
