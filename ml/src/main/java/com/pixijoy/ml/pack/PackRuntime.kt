package com.pixijoy.ml.pack

import android.content.Context
import com.pixijoy.ml.PackEngineHooks
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide pack install queries. [bind] runs when the hooks singleton is created
 * for a later feature-module [com.pixijoy.ml.PackFeatureEntry] call, not from
 * Application.onCreate. Unbound, every query reports missing / not-ready.
 */
object PackRuntime {
    @Volatile
    private var appContext: Context? = null

    /** App registers a LiteRT binder. Pack install never fakes an engine. */
    @Volatile
    var onPackReady: ((PackId, File) -> Unit)? = null

    @Volatile
    var onPackRemoved: ((PackId) -> Unit)? = null

    /** True after Play-services LiteRT initializes. Missing GMS leaves this false. */
    @Volatile
    var liteRtReady: Boolean = false

    private val engineEpoch = MutableStateFlow(0)

    fun observeEngineEpoch(): StateFlow<Int> = engineEpoch.asStateFlow()

    fun notifyEngineChanged() {
        engineEpoch.value = engineEpoch.value + 1
    }

    fun bind(context: Context) {
        appContext = context.applicationContext
    }

    fun unbind() {
        appContext = null
    }

    fun manager(): PackInstallManager? {
        val ctx = appContext ?: return null
        return PackInstallManager.get(ctx)
    }

    fun state(id: PackId): PackInstallState =
        manager()?.state(id) ?: PackInstallState.Missing

    fun isPackReady(id: PackId): Boolean = state(id).isReady

    fun engineReady(id: PackId): Boolean =
        isEngineReady(isPackReady(id), PackEngineHooks.hasEngine(id))

    /**
     * Neural chip gate. A Ready pack with no bound interpreter is not an engine.
     * Telea, bicubic, and frame-hold never pass [engineBound].
     */
    internal fun isEngineReady(packReady: Boolean, engineBound: Boolean): Boolean =
        packReady && engineBound

    fun incomingDir(): File? {
        val ctx = appContext ?: return null
        return File(ctx.filesDir, PackStore.INCOMING_DIR)
    }
}
