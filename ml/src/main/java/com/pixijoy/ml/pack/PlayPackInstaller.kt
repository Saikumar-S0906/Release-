package com.pixijoy.ml.pack

import android.content.Context
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Play Feature Delivery installer. When Play is missing (sideload APK, emulator
 * without store) the request fails and the caller falls back to local incoming files.
 */
internal class PlayPackInstaller(context: Context) {
    private val split: SplitInstallManager? = runCatching {
        SplitInstallManagerFactory.create(context.applicationContext)
    }.getOrNull()

    fun installedModules(): Set<String> = split?.installedModules ?: emptySet()

    suspend fun install(spec: PackSpec, onProgress: (Float) -> Unit): Result<Unit> {
        val manager = split ?: return Result.failure(IllegalStateException("Play is unavailable."))
        if (spec.playModule in manager.installedModules) return Result.success(Unit)
        return suspendCancellableCoroutine { cont ->
            val done = AtomicBoolean(false)
            fun finish(result: Result<Unit>) {
                if (done.compareAndSet(false, true) && cont.isActive) {
                    cont.resume(result)
                }
            }
            var sessionId = 0
            val listener = SplitInstallStateUpdatedListener { state ->
                if (sessionId != 0 && state.sessionId() != sessionId) return@SplitInstallStateUpdatedListener
                when (state.status()) {
                    SplitInstallSessionStatus.DOWNLOADING,
                    SplitInstallSessionStatus.INSTALLING -> {
                        val total = state.totalBytesToDownload().coerceAtLeast(1L)
                        onProgress((state.bytesDownloaded().toFloat() / total.toFloat()).coerceIn(0f, 1f))
                    }
                    SplitInstallSessionStatus.INSTALLED -> finish(Result.success(Unit))
                    SplitInstallSessionStatus.FAILED,
                    SplitInstallSessionStatus.CANCELED ->
                        finish(Result.failure(IllegalStateException("Play module ${spec.playModule} failed.")))
                    else -> Unit
                }
            }
            manager.registerListener(listener)
            cont.invokeOnCancellation {
                runCatching { manager.unregisterListener(listener) }
            }
            manager.startInstall(
                SplitInstallRequest.newBuilder().addModule(spec.playModule).build()
            )
                .addOnSuccessListener { id ->
                    sessionId = id
                    if (spec.playModule in manager.installedModules) {
                        finish(Result.success(Unit))
                    }
                }
                .addOnFailureListener { err ->
                    runCatching { manager.unregisterListener(listener) }
                    finish(Result.failure(err))
                }
        }
    }
}
