package com.pixijoy.ai.ui.session

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiEmptyState
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.ml.pack.PackCatalog
import com.pixijoy.ml.pack.PackId
import com.pixijoy.ml.pack.PackInstallManager
import com.pixijoy.ml.pack.PackInstallState
import com.pixijoy.ml.pack.PackRuntime
import com.pixijoy.ml.pack.SuperResDenoiseStatus
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun rememberPackEngineReady(packId: PackId): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { PackInstallManager.get(context) }
    val state by manager.observe(packId).collectAsState()
    val epoch by PackRuntime.observeEngineEpoch().collectAsState()
    return remember(state, epoch, packId) { PackRuntime.engineReady(packId) }
}

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
    val engineReady = rememberPackEngineReady(packId)
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
                if (copied == null) {
                    manager.fail(packId, context.getString(R.string.studio_pack_file_unreadable))
                } else {
                    withContext(Dispatchers.IO) { manager.sideload(packId, copied) }
                }
            } catch (_: Exception) {
                manager.fail(packId, context.getString(R.string.studio_pack_file_unreadable))
            } finally {
                tmp.delete()
            }
        }
    }
    if (state is PackInstallState.Ready) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                packStatusCopy(packId, state, engineReady),
                color = PixiTextMuted,
                modifier = Modifier.testTag(statusTag)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        scope.launch { withContext(Dispatchers.IO) { manager.uninstall(packId) } }
                    },
                    modifier = Modifier.testTag("${downloadTag}_remove")
                ) {
                    Text(stringResource(R.string.studio_pack_remove), color = PixiTextMuted)
                }
                TextButton(
                    onClick = { pickPack.launch("*/*") },
                    modifier = Modifier.testTag("${downloadTag}_from_file")
                ) {
                    Text(stringResource(R.string.studio_pack_from_file), color = PixiTextMuted)
                }
            }
        }
    } else {
        EmptyPackSurface(
            state = state,
            statusTag = statusTag,
            downloadTag = downloadTag,
            onDownload = {
                scope.launch { withContext(Dispatchers.IO) { manager.install(packId) } }
            },
            onPickFile = { pickPack.launch("*/*") }
        )
    }
}

/**
 * Missing / installing / failed pack. Same install actions as the ready row.
 * Copy stays off "neural" until [PackRuntime.engineReady]. Ready alone is the file.
 */
@Composable
private fun EmptyPackSurface(
    state: PackInstallState,
    statusTag: String,
    downloadTag: String,
    onDownload: () -> Unit,
    onPickFile: () -> Unit
) {
    val installing = state as? PackInstallState.Installing
    PixiEmptyState(
        line = packEmptyLine(state),
        icon = painterResource(R.drawable.pj_ic_download),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_packs"),
        lineModifier = Modifier.testTag(statusTag),
        cta = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (installing != null) {
                    LinearProgressIndicator(
                        progress = { installing.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${statusTag}_progress")
                    )
                } else {
                    PixiPrimaryButton(
                        text = stringResource(
                            if (state is PackInstallState.Missing) R.string.studio_pack_download
                            else R.string.studio_pack_retry
                        ),
                        onClick = onDownload,
                        modifier = Modifier.testTag(downloadTag)
                    )
                    TextButton(
                        onClick = onPickFile,
                        modifier = Modifier.testTag("${downloadTag}_from_file")
                    ) {
                        Text(stringResource(R.string.studio_pack_from_file), color = PixiTextMuted)
                    }
                }
            }
        }
    )
}

@Composable
private fun packEmptyLine(state: PackInstallState): String = when (state) {
    PackInstallState.Missing -> stringResource(R.string.studio_packs_empty)
    is PackInstallState.Installing -> stringResource(R.string.studio_pack_installing)
    is PackInstallState.Failed -> state.message
    is PackInstallState.StorageError -> stringResource(R.string.studio_pack_storage, state.message)
    is PackInstallState.Ready -> stringResource(R.string.studio_packs_empty)
}

@Composable
private fun packStatusCopy(
    packId: PackId,
    state: PackInstallState,
    engineReady: Boolean
): String = when (state) {
    PackInstallState.Missing -> stringResource(
        if (PackCatalog.hasHttpsFallback(PackCatalog.spec(packId))) {
            R.string.studio_pack_missing
        } else {
            R.string.studio_pack_missing_sideload
        }
    )
    is PackInstallState.Installing -> stringResource(R.string.studio_pack_installing)
    is PackInstallState.Ready -> when (packId) {
        PackId.SuperRes, PackId.Denoise -> superResDenoiseReadyCopy(engineReady)
        else -> when {
            engineReady -> stringResource(R.string.studio_pack_engine_ready)
            !PackRuntime.liteRtReady -> stringResource(R.string.studio_pack_ready_no_litert)
            packId == PackId.FrameInterp || packId == PackId.Track ->
                stringResource(R.string.studio_pack_ready_no_engine)
            else -> stringResource(R.string.studio_pack_ready_unbound)
        }
    }
    is PackInstallState.Failed -> state.message
    is PackInstallState.StorageError -> stringResource(R.string.studio_pack_storage, state.message)
}

/**
 * Super-res and Denoise. [SuperResDenoiseStatus.ReadyFileOnly] is downloaded
 * and verified. [SuperResDenoiseStatus.EngineReady] is the bound neural engine.
 */
@Composable
private fun superResDenoiseReadyCopy(engineReady: Boolean): String =
    when (PackRuntime.superResDenoiseStatus(packReady = true, engineBound = engineReady)) {
        SuperResDenoiseStatus.EngineReady ->
            stringResource(R.string.studio_pack_engine_ready)
        SuperResDenoiseStatus.ReadyFileOnly ->
            if (PackRuntime.liteRtReady) {
                stringResource(R.string.studio_pack_sr_denoise_ready)
            } else {
                stringResource(R.string.studio_pack_sr_denoise_ready_no_litert)
            }
        SuperResDenoiseStatus.NotReady ->
            stringResource(R.string.studio_pack_sr_denoise_ready)
    }
