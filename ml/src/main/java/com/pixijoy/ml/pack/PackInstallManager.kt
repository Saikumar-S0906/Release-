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
                emitReady(id, store.readyState(spec))
                return
            }
            emit(id, PackInstallState.Installing(0f))
            val playResult = play?.install(spec) { progress ->
                emit(id, PackInstallState.Installing(progress.coerceIn(0f, 1f)))
            }
            if (playResult?.isSuccess == true) {
                incomingFile(spec)?.let { file ->
                    if (PackChecksum.looksLikeTflite(file)) {
                        emitReady(id, store.installFrom(spec, file))
                        if (state(id).isReady) return
                    }
                }
            }
            val local = incomingFile(spec)
            if (local != null) {
                if (!PackChecksum.looksLikeTflite(local)) {
                    emit(id, PackInstallState.Failed("Incoming file is not a TFLite pack."))
                    return
                }
                emitReady(id, store.installFrom(spec, local))
                return
            }
            val url = spec.downloadUrl
            val pin = spec.downloadSha256
            if (!url.isNullOrBlank() && !pin.isNullOrBlank()) {
                val tmp = File.createTempFile("pixijoy-pack-${id.name}", ".tflite")
                try {
                    val downloaded = PackDownloader.download(url, tmp) { progress ->
                        emit(id, PackInstallState.Installing(progress.coerceIn(0f, 1f)))
                    }
                    if (downloaded.isFailure) {
                        emit(
                            id,
                            PackInstallState.Failed(
                                downloaded.exceptionOrNull()?.message ?: "Pack download failed."
                            )
                        )
                        return
                    }
                    if (!PackChecksum.matches(tmp, pin)) {
                        emit(id, PackInstallState.Failed("Downloaded pack checksum did not match."))
                        return
                    }
                    if (!PackChecksum.looksLikeTflite(tmp)) {
                        emit(id, PackInstallState.Failed("Downloaded file is not a TFLite pack."))
                        return
                    }
                    emitReady(id, store.installFrom(spec.copy(expectedSha256 = pin), tmp))
                    return
                } finally {
                    tmp.delete()
                }
            }
            emit(
                id,
                PackInstallState.Failed(
                    PackCatalog.installUnavailableMessage(
                        spec,
                        playResult?.exceptionOrNull()?.message
                    )
                )
            )
        }
    }

    suspend fun sideload(id: PackId, source: File) {
        mutex.withLock {
            emit(id, PackInstallState.Installing(0.5f))
            if (!source.isFile) {
                emit(id, PackInstallState.Failed("Could not read the selected file."))
                return
            }
            if (!PackChecksum.looksLikeTflite(source)) {
                emit(id, PackInstallState.Failed("Selected file is not a TFLite pack."))
                return
            }
            emitReady(id, store.installFrom(PackCatalog.spec(id), source))
        }
    }

    suspend fun fail(id: PackId, message: String) {
        mutex.withLock {
            emit(id, PackInstallState.Failed(message))
        }
    }

    suspend fun uninstall(id: PackId) {
        mutex.withLock {
            store.uninstall(PackCatalog.spec(id))
            emit(id, PackInstallState.Missing)
            runCatching { PackRuntime.onPackRemoved?.invoke(id) }
        }
    }

    private fun incomingFile(spec: PackSpec): File? =
        incomingRoots.map { File(it, spec.fileName) }.firstOrNull { it.isFile && it.length() >= PackStore.MIN_BYTES }

    private fun emit(id: PackId, state: PackInstallState) {
        flows.getValue(id).value = state
    }

    private fun emitReady(id: PackId, state: PackInstallState) {
        emit(id, state)
        if (state is PackInstallState.Ready) {
            runCatching {
                PackRuntime.onPackReady?.invoke(id, store.modelFile(PackCatalog.spec(id)))
            }
        }
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
