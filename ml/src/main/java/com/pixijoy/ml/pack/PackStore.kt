package com.pixijoy.ml.pack

import java.io.File
import java.io.IOException

/**
 * Checksumed on-device pack store. Root is typically `filesDir/ml_packs`.
 */
class PackStore(private val root: File) {
    fun modelFile(spec: PackSpec): File =
        File(File(root, spec.id.name.lowercase()), "${spec.version}/${spec.fileName}")

    fun checksumFile(spec: PackSpec): File =
        File(modelFile(spec).parentFile, "${spec.fileName}.sha256")

    fun installedSha(spec: PackSpec): String? {
        val side = checksumFile(spec)
        if (!side.isFile) return null
        return side.readText().trim().ifBlank { null }
    }

    fun isReady(spec: PackSpec): Boolean {
        val model = modelFile(spec)
        val recorded = installedSha(spec) ?: return false
        if (spec.expectedSha256 != null &&
            !recorded.equals(spec.expectedSha256, ignoreCase = true)
        ) {
            return false
        }
        return PackChecksum.matches(model, recorded)
    }

    fun readyState(spec: PackSpec): PackInstallState {
        val sha = installedSha(spec) ?: return PackInstallState.Missing
        return if (isReady(spec)) {
            PackInstallState.Ready(sha, spec.version)
        } else {
            PackInstallState.Missing
        }
    }

    fun installFrom(spec: PackSpec, source: File): PackInstallState {
        if (!source.isFile) {
            return PackInstallState.Failed("Model file is missing.")
        }
        if (source.length() < MIN_BYTES) {
            return PackInstallState.Failed("Model file is too small to be a pack.")
        }
        val dest = modelFile(spec)
        val parent = dest.parentFile ?: return PackInstallState.StorageError("Pack folder is missing.")
        return try {
            if (!parent.exists() && !parent.mkdirs()) {
                return PackInstallState.StorageError("Could not create pack storage.")
            }
            val tmp = File(parent, dest.name + ".tmp")
            source.copyTo(tmp, overwrite = true)
            val sha = PackChecksum.sha256(tmp)
            val expected = spec.expectedSha256
            if (expected != null && !sha.equals(expected, ignoreCase = true)) {
                tmp.delete()
                return PackInstallState.Failed("Model checksum did not match.")
            }
            if (dest.exists() && !dest.delete()) {
                tmp.delete()
                return PackInstallState.StorageError("Could not replace the installed model.")
            }
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
            checksumFile(spec).writeText(sha)
            PackInstallState.Ready(sha, spec.version)
        } catch (_: IOException) {
            PackInstallState.StorageError("Not enough storage to install this pack.")
        }
    }

    fun uninstall(spec: PackSpec) {
        val dir = modelFile(spec).parentFile
        dir?.deleteRecursively()
    }

    companion object {
        const val MIN_BYTES = 64L
        const val INCOMING_DIR = "ml_incoming"
        const val STORE_DIR = "ml_packs"
    }
}
