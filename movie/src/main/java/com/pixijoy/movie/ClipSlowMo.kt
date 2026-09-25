package com.pixijoy.movie

import com.pixijoy.ml.pack.PackId
import com.pixijoy.ml.pack.PackRuntime
import kotlin.math.abs

/**
 * Slow-mo timeline/export contract (D10).
 *
 * [rate] is playback rate relative to 1× (0.125..1). [engine] selects how in-between
 * frames are produced: [SlowMoEngine.Heuristic] is the D10 default (Media3 speed /
 * frame-hold); [SlowMoEngine.Ml] is reserved for the E4 pack and falls back to
 * heuristic until that pack is installed ([FrameInterp.mlAvailable]).
 */
data class ClipSlowMo(
    val rate: Float = 1f,
    val engine: SlowMoEngine = SlowMoEngine.Heuristic
) {
    fun normalized(): ClipSlowMo {
        val r = MovieSpeed.normalizeSlowMo(rate)
        return copy(
            rate = r,
            engine = if (r >= 0.99f) SlowMoEngine.None else engine.coerceActive()
        )
    }

    val isActive: Boolean
        get() {
            val n = normalized()
            return n.rate < 0.99f && n.engine != SlowMoEngine.None
        }

    /** Engine that will actually run after E4 availability check. */
    fun resolvedEngine(): SlowMoEngine = FrameInterp.resolve(engine)

    fun encode(): String {
        if (!isActive) return "none"
        val n = normalized()
        return "${fmt(n.rate)}_${n.engine.encode()}"
    }

    fun matchesPreset(preset: SlowMoPreset): Boolean {
        if (preset == SlowMoPreset.Off) return !isActive
        val n = normalized()
        return abs(n.rate - preset.rate) < 0.02f
    }

    fun withPreset(preset: SlowMoPreset): ClipSlowMo =
        if (preset == SlowMoPreset.Off) None
        else copy(rate = preset.rate, engine = SlowMoEngine.Heuristic).normalized()

    fun withEngine(engine: SlowMoEngine): ClipSlowMo {
        if (!isActive && engine != SlowMoEngine.None) return this
        return copy(engine = engine).normalized()
    }

    companion object {
        val None = ClipSlowMo(rate = 1f, engine = SlowMoEngine.None)

        fun decode(raw: String?): ClipSlowMo {
            if (raw.isNullOrBlank() || raw == "none") return None
            val p = raw.split('_')
            val rate = p.getOrNull(0)?.toFloatOrNull() ?: return None
            val engine = SlowMoEngine.parse(p.getOrNull(1))
            return ClipSlowMo(rate = rate, engine = engine).normalized()
                .let { if (it.rate >= 0.99f) None else it }
        }

        private fun fmt(v: Float): String =
            String.format(java.util.Locale.US, "%.3f", v).trimEnd('0').trimEnd('.')
    }
}

enum class SlowMoEngine {
    None,
    Heuristic,
    /** Reserved for E4 ML pack — resolves to Heuristic until pack is available. */
    Ml;

    fun encode(): String = name.lowercase()

    fun coerceActive(): SlowMoEngine = if (this == None) Heuristic else this

    companion object {
        fun parse(raw: String?): SlowMoEngine =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: Heuristic
    }
}

enum class SlowMoPreset(val rate: Float) {
    Off(1f),
    Half(0.5f),
    Quarter(0.25f),
    Eighth(0.125f);

    fun toSlowMo(): ClipSlowMo = ClipSlowMo.None.withPreset(this)
}

/**
 * Frame-interp availability gate. E4 pack flips [mlAvailable] without changing D10 UX.
 */
object FrameInterp {
    /** Override in tests / E4 pack install. Default: no ML in base APK. */
    @Volatile
    var mlAvailableOverride: Boolean? = null

    fun mlAvailable(): Boolean = mlAvailableOverride ?: PackRuntime.engineReady(PackId.FrameInterp)

    fun resolve(requested: SlowMoEngine): SlowMoEngine = when (requested) {
        SlowMoEngine.None -> SlowMoEngine.None
        SlowMoEngine.Heuristic -> SlowMoEngine.Heuristic
        SlowMoEngine.Ml -> if (mlAvailable()) SlowMoEngine.Ml else SlowMoEngine.Heuristic
    }

    /** Human-readable engine label for session UI / future pack badge. */
    fun engineLabelKey(engine: SlowMoEngine): String = when (resolve(engine)) {
        SlowMoEngine.None -> "none"
        SlowMoEngine.Heuristic -> "heuristic"
        SlowMoEngine.Ml -> "ml"
    }
}
