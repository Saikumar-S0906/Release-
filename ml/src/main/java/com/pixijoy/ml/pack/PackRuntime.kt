package com.pixijoy.ml.pack

import android.content.Context
import com.pixijoy.ml.PackEngineHooks
import java.io.File

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
        isPackReady(id) && PackEngineHooks.hasEngine(id)

    fun incomingDir(): File? {
        val ctx = appContext ?: return null
        return File(ctx.filesDir, PackStore.INCOMING_DIR)
    }
}
