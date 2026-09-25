package com.pixijoy.movie

import com.pixijoy.ml.pack.PackId
import com.pixijoy.ml.pack.PackRuntime

/**
 * F1 tracking / roto-lite contract. No optical-flow or mask follow ships in the base
 * APK. [TrackFacade.mlAvailable] flips when the track pack engine is bound.
 */
data class ClipTrack(
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val radius: Float = 0.08f,
    val engine: TrackEngine = TrackEngine.Off
) {
    fun normalized(): ClipTrack = copy(
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f),
        radius = radius.coerceIn(0.02f, 0.35f)
    )

    val isActive: Boolean get() = engine != TrackEngine.Off

    fun resolvedEngine(): TrackEngine = TrackFacade.resolve(engine)

    fun encode(): String {
        if (!isActive) return "none"
        val n = normalized()
        return "${fmt(n.x)}_${fmt(n.y)}_${fmt(n.radius)}_${n.engine.encode()}"
    }

    companion object {
        val None = ClipTrack()

        fun decode(raw: String?): ClipTrack {
            if (raw.isNullOrBlank() || raw == "none") return None
            val p = raw.split('_')
            if (p.size < 4) return None
            val x = p[0].toFloatOrNull() ?: return None
            val y = p[1].toFloatOrNull() ?: return None
            val radius = p[2].toFloatOrNull() ?: return None
            val engine = TrackEngine.parse(p[3])
            return ClipTrack(x, y, radius, engine).normalized()
        }

        private fun fmt(v: Float): String =
            String.format(java.util.Locale.US, "%.3f", v).trimEnd('0').trimEnd('.')
    }
}

enum class TrackEngine {
    Off,
    Ml;

    fun encode(): String = name.lowercase()

    companion object {
        fun parse(raw: String?): TrackEngine =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: Off
    }
}

object TrackFacade {
    @Volatile
    var mlAvailableOverride: Boolean? = null

    fun mlAvailable(): Boolean = mlAvailableOverride ?: PackRuntime.engineReady(PackId.Track)

    fun resolve(requested: TrackEngine): TrackEngine = when (requested) {
        TrackEngine.Off -> TrackEngine.Off
        TrackEngine.Ml -> if (mlAvailable()) TrackEngine.Ml else TrackEngine.Off
    }
}
