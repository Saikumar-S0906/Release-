package com.pixijoy.ml.pack

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PackInstallManager internal constructor(
    private val store: PackStore,
    private val incomingRoots: List<File>,
    private val play: PlayPackInstaller?
) {
    private val mutex = Mutex()
    private val flows = PackId.entries.associateWith { id ->
        MutableStateFlow(store.readyState(PackCatalog.spec(id)))
    }

    fun observe(id: PackId): StateFlow<PackInstallState> = flows.getValue(id).asStateFlow()

    fun state(id: PackId): PackInstallState = flows.getValue(id).value

    fun isReady(id: PackId): Boolean = store.isReady(PackCatalog.spec(id))

    fun modelFile(id: PackId): File = store.modelFile(PackCatalog.spec(id))

    suspend fun install(id: PackId) {
        mutex.withLock {
            val spec = PackCatalog.spec(id)
            if (store.isReady(spec)) {
                emit(id, store.readyState(spec))
                return
            }
            emit(id, PackInstallState.Installing(0f))
            val playResult = play?.install(spec) { progress ->
                emit(id, PackInstallState.Installing(progress.coerceIn(0f, 1f)))
            }
            if (playResult?.isSuccess == true) {
                incomingFile(spec)?.let { file ->
                    emit(id, store.installFrom(spec, file))
                    if (state(id).isReady) return
                }
            }
            val local = incomingFile(spec)
            if (local != null) {
                emit(id, store.installFrom(spec, local))
                return
            }
            val playError = playResult?.exceptionOrNull()?.message
            emit(
                id,
                PackInstallState.Failed(
                    playError ?: "Pack is not on this phone yet. Copy ${spec.fileName} into the incoming folder."
                )
            )
        }
    }

    suspend fun sideload(id: PackId, source: File) {
        mutex.withLock {
            emit(id, PackInstallState.Installing(0.5f))
            emit(id, store.installFrom(PackCatalog.spec(id), source))
        }
    }

    suspend fun uninstall(id: PackId) {
        mutex.withLock {
            store.uninstall(PackCatalog.spec(id))
            emit(id, PackInstallState.Missing)
        }
    }

    private fun incomingFile(spec: PackSpec): File? =
        incomingRoots.map { File(it, spec.fileName) }.firstOrNull { it.isFile && it.length() >= PackStore.MIN_BYTES }

    private fun emit(id: PackId, state: PackInstallState) {
        flows.getValue(id).value = state
    }

    companion object {
        @Volatile
        private var instance: PackInstallManager? = null

        fun get(context: Context): PackInstallManager {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                val app = context.applicationContext
                val store = PackStore(File(app.filesDir, PackStore.STORE_DIR))
                val incoming = buildList {
                    add(File(app.filesDir, PackStore.INCOMING_DIR))
                    app.getExternalFilesDir(null)?.let { add(File(it, PackStore.INCOMING_DIR)) }
                }
                val created = PackInstallManager(
                    store = store,
                    incomingRoots = incoming,
                    play = PlayPackInstaller(app)
                )
                instance = created
                return created
            }
        }

        fun resetForTests() {
            instance = null
        }
    }
}
