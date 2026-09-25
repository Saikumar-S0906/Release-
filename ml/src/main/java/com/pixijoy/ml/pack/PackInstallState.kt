package com.pixijoy.ml.pack

sealed class PackInstallState {
    data object Missing : PackInstallState()
    data class Installing(val progress: Float) : PackInstallState()
    data class Ready(val sha256: String, val version: String) : PackInstallState()
    data class Failed(val message: String) : PackInstallState()
    data class StorageError(val message: String) : PackInstallState()

    val isReady: Boolean get() = this is Ready
}
