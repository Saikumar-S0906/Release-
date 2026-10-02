package com.pixijoy.ml

import android.content.Context
import android.graphics.Bitmap
import com.pixijoy.ml.pack.PackCatalog
import com.pixijoy.ml.pack.PackChecksum
import com.pixijoy.ml.pack.PackId
import com.pixijoy.ml.pack.PackInstallManager
import com.pixijoy.ml.pack.PackInstallState
import com.pixijoy.ml.pack.PackRuntime
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real interpreter for one pack capability. Feature modules construct these.
 * The base APK does not.
 */
sealed interface NeuralEngine {
    val capability: PackId

    class Inpaint(
        private val invoke: (Bitmap, BooleanArray) -> Bitmap?
    ) : NeuralEngine {
        override val capability: PackId = PackId.Inpaint
        fun run(source: Bitmap, mask: BooleanArray): Bitmap? = invoke(source, mask)
    }

    class SuperRes(
        private val invoke: (Bitmap, Int) -> Bitmap?
    ) : NeuralEngine {
        override val capability: PackId = PackId.SuperRes
        fun run(source: Bitmap, scale: Int): Bitmap? = invoke(source, scale)
    }

    class Denoise(
        private val invoke: (Bitmap, Float) -> Bitmap?
    ) : NeuralEngine {
        override val capability: PackId = PackId.Denoise
        fun run(source: Bitmap, amount: Float): Bitmap? = invoke(source, amount)
    }

    class FrameInterp(
        private val invoke: (Bitmap, Bitmap, Int) -> List<Bitmap>?
    ) : NeuralEngine {
        override val capability: PackId = PackId.FrameInterp
        fun run(before: Bitmap, after: Bitmap, steps: Int): List<Bitmap>? =
            invoke(before, after, steps)
    }

    class Track(
        private val invoke: (Bitmap, Float, Float) -> Pair<Float, Float>?
    ) : NeuralEngine {
        override val capability: PackId = PackId.Track
        fun run(frame: Bitmap, x: Float, y: Float): Pair<Float, Float>? = invoke(frame, x, y)
    }

    /**
     * Relative depth, length [com.pixijoy.ml.pack.PackModelContract.DEPTH_EDGE] squared,
     * row-major, 0 near → 1 far. Null when the interpreter misses.
     */
    class Depth(
        private val invoke: (Bitmap) -> FloatArray?
    ) : NeuralEngine {
        override val capability: PackId = PackId.Depth
        fun run(frame: Bitmap): FloatArray? = invoke(frame)
    }
}

/** Install + checksum gate. True only for a ready pack whose file hash matches. */
fun interface PackReadiness {
    fun verified(id: PackId): Boolean
}

/**
 * True when [state] is ready, the model file matches the recorded SHA-256,
 * and any pinned hash matches too. Missing, failed, and storage errors are false.
 */
fun interpreterBindAllowed(
    state: PackInstallState,
    fileMatchesRecordedSha: Boolean,
    expectedSha256: String?
): Boolean {
    val ready = state as? PackInstallState.Ready ?: return false
    if (!fileMatchesRecordedSha) return false
    if (expectedSha256 != null && !ready.sha256.equals(expectedSha256, ignoreCase = true)) {
        return false
    }
    return true
}

/**
 * Bind-time file gate: Ready, bytes match the recorded SHA-256, and any
 * catalog [expectedSha256] matches that record. HTTPS download pins are
 * checked before the pack becomes Ready; this does not treat Ready alone
 * as permission to register an interpreter.
 */
fun modelFileVerified(
    state: PackInstallState,
    file: File,
    expectedSha256: String?
): Boolean {
    val ready = state as? PackInstallState.Ready ?: return false
    if (ready.sha256.isBlank()) return false
    return interpreterBindAllowed(
        state,
        PackChecksum.matches(file, ready.sha256),
        expectedSha256
    )
}

class StorePackReadiness(
    context: Context
) : PackReadiness {
    private val appContext = context.applicationContext

    init {
        PackRuntime.bind(appContext)
    }

    override fun verified(id: PackId): Boolean {
        val manager = PackInstallManager.get(appContext)
        val state = manager.state(id)
        val ready = state as? PackInstallState.Ready ?: return false
        val fileOk = PackChecksum.matches(manager.modelFile(id), ready.sha256)
        return interpreterBindAllowed(state, fileOk, PackCatalog.spec(id).expectedSha256)
    }
}

/**
 * Process-wide pack facade. Hilt can construct the singleton; nothing binds an
 * interpreter until [PackFeatureEntry] calls [register] after [PackSplits.install].
 *
 * [register] keeps an interpreter only when that capability is Ready and the
 * SHA-256 matches. Otherwise the classical path stays in place (Telea, bicubic,
 * frame-hold) and is not labeled neural.
 *
 * Photo and movie engines call [tryInpaint] and the other try* methods.
 * The base application does not call [register].
 */
@Singleton
class PackEngineHooks @Inject constructor(
    private val readiness: PackReadiness
) {
    private val bound = HashMap<PackId, NeuralEngine>()

    /**
     * Bind interpreters for capabilities that are ready and checksum-valid.
     * Entries that fail the gate are dropped. Installs this instance as the
     * process facade so [tryInpaint] and the other try* methods can see it.
     *
     * Call from [PackFeatureEntry] after SplitCompat. Not from Application.onCreate.
     */
    fun register(engines: Map<PackId, NeuralEngine>) {
        engines.forEach { (capability, engine) ->
            if (engine.capability != capability || !readiness.verified(capability)) {
                bound.remove(capability)
            } else {
                bound[capability] = engine
            }
        }
        attach(this)
        PackRuntime.notifyEngineChanged()
    }

    /** Drop one capability. Used when a pack file is removed. Does not bind a replacement. */
    internal fun release(id: PackId) {
        bound.remove(id)
        attach(this)
        PackRuntime.notifyEngineChanged()
    }

    fun isBound(id: PackId): Boolean = bound[id] != null && readiness.verified(id)

    fun inpaint(source: Bitmap, mask: BooleanArray): Bitmap? =
        (boundEngine(PackId.Inpaint) as? NeuralEngine.Inpaint)?.run(source, mask)

    fun superRes(source: Bitmap, scale: Int): Bitmap? =
        (boundEngine(PackId.SuperRes) as? NeuralEngine.SuperRes)?.run(source, scale)

    fun denoise(source: Bitmap, amount: Float): Bitmap? =
        (boundEngine(PackId.Denoise) as? NeuralEngine.Denoise)?.run(source, amount)

    fun frameInterp(before: Bitmap, after: Bitmap, steps: Int): List<Bitmap>? =
        (boundEngine(PackId.FrameInterp) as? NeuralEngine.FrameInterp)?.run(before, after, steps)

    fun track(frame: Bitmap, x: Float, y: Float): Pair<Float, Float>? =
        (boundEngine(PackId.Track) as? NeuralEngine.Track)?.run(frame, x, y)

    fun depth(frame: Bitmap): FloatArray? =
        (boundEngine(PackId.Depth) as? NeuralEngine.Depth)?.run(frame)

    fun clear() {
        bound.clear()
    }

    private fun boundEngine(id: PackId): NeuralEngine? =
        if (readiness.verified(id)) bound[id] else null

    companion object {
        @Volatile
        private var active: PackEngineHooks? = null

        private fun attach(hooks: PackEngineHooks) {
            active = hooks
        }

        fun tryInpaint(source: Bitmap, mask: BooleanArray): Bitmap? =
            active?.inpaint(source, mask)

        fun trySuperRes(source: Bitmap, scale: Int): Bitmap? =
            active?.superRes(source, scale)

        fun tryDenoise(source: Bitmap, amount: Float): Bitmap? =
            active?.denoise(source, amount)

        fun tryFrameInterp(before: Bitmap, after: Bitmap, steps: Int): List<Bitmap>? =
            active?.frameInterp(before, after, steps)

        fun tryTrack(frame: Bitmap, x: Float, y: Float): Pair<Float, Float>? =
            active?.track(frame, x, y)

        /** Null unless a depth interpreter is bound. Classical Z stays in that case. */
        fun tryDepth(frame: Bitmap): FloatArray? = active?.depth(frame)

        fun hasEngine(id: PackId): Boolean = active?.isBound(id) == true

        fun resetForTests() {
            active?.clear()
            active = null
        }
    }
}
