package com.pixijoy.movie

import android.content.Context
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * On-demand feature modules kept out of the base APK
 * (F3 Filament titles, H1/H10 `:feature_cg`, F4 ARCore, F5 FFmpeg).
 * H10: `./gradlew :app:reportH10SizeGate` asserts Filament/CG stay out of base.
 */
object OptFeatureModules {
    const val TITLES3D = "feature_titles3d"
    const val AR = "feature_ar"
    const val FFMPEG = "feature_ffmpeg"
    const val CG = "feature_cg"

    fun isInstalled(context: Context, module: String): Boolean =
        runCatching {
            SplitInstallManagerFactory.create(context.applicationContext).installedModules.contains(module)
        }.getOrDefault(false)

    suspend fun ensureInstalled(context: Context, module: String): Result<Unit> {
        val split = runCatching {
            SplitInstallManagerFactory.create(context.applicationContext)
        }.getOrNull() ?: return Result.failure(IllegalStateException("SplitInstall unavailable"))
        if (split.installedModules.contains(module)) return Result.success(Unit)
        return suspendCancellableCoroutine { cont ->
            var sessionId = 0
            val listener = SplitInstallStateUpdatedListener { state ->
                if (sessionId != 0 && state.sessionId() != sessionId) return@SplitInstallStateUpdatedListener
                when (state.status()) {
                    SplitInstallSessionStatus.INSTALLED -> {
                        if (cont.isActive) cont.resume(Result.success(Unit))
                    }
                    SplitInstallSessionStatus.FAILED, SplitInstallSessionStatus.CANCELED -> {
                        if (cont.isActive) {
                            cont.resume(
                                Result.failure(IllegalStateException("Split install failed: ${state.errorCode()}"))
                            )
                        }
                    }
                }
            }
            split.registerListener(listener)
            cont.invokeOnCancellation { split.unregisterListener(listener) }
            runCatching {
                split.startInstall(SplitInstallRequest.newBuilder().addModule(module).build())
                    .addOnSuccessListener { sessionId = it }
                    .addOnFailureListener {
                        split.unregisterListener(listener)
                        if (cont.isActive) cont.resume(Result.failure(it))
                    }
            }.onFailure {
                split.unregisterListener(listener)
                if (cont.isActive) cont.resume(Result.failure(it))
            }
        }
    }

    fun tryBindTitles3d() {
        runCatching {
            Class.forName("com.pixijoy.feature.titles3d.FilamentTitles3dEntry")
                .getMethod("install")
                .invoke(null)
        }
    }

    /** Binds Filament prop rendering when `:feature_cg` is installed. A miss leaves the classical raster. */
    fun tryBindCg() {
        if (CgHost.filamentAvailable()) return
        runCatching {
            Class.forName("com.pixijoy.feature.cg.FilamentCgEntry")
                .getMethod("install")
                .invoke(null)
        }
    }
}
