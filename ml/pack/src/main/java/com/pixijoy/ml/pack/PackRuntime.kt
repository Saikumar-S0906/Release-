package com.pixijoy.ml.pack

import android.content.Context
import java.io.File

/**
 * App-wide pack facade. [bind] from [android.app.Application]. Unbound, every
 * query reports missing / not-ready so JVM tests stay hermetic.
 */
object PackRuntime {
    @Volatile
    private var appContext: Context? = null

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
