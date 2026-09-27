package com.pixijoy.ai.ui.session

/**
 * CODING_PHASES P0.1 — jump to a movie tool group (and optional effect sub) by query.
 * Pure logic so unit tests don't need Compose.
 */
data class ToolJump(
    val groupTag: String,
    val effectSubTag: String? = null,
    val label: String
)

object MovieToolSearch {
    private val catalog: List<Pair<List<String>, ToolJump>> = listOf(
        listOf("trim", "split", "cut", "edit", "ripple", "roll", "speed", "transition") to
            ToolJump(MovieGroup.Edit.tag, label = "Edit"),
        listOf("marker", "markers", "beat", "assembly", "bin", "bins", "version", "versions", "snap") to
            ToolJump(MovieGroup.Edit.tag, label = "Assembly"),
        listOf("audio", "music", "duck", "voice", "vo", "stem", "mix", "eq") to
            ToolJump(MovieGroup.Audio.tag, label = "Audio"),
        listOf("title", "caption", "text", "3d title", "titles3d") to
            ToolJump(MovieGroup.Text.tag, label = "Text"),
        listOf("grade", "color", "lut", "look", "scope", "scopes", "waveform", "secondary", "match", "shot match") to
            ToolJump(MovieGroup.Color.tag, label = "Color"),
        listOf("motion", "ken burns", "kenburns", "pan", "zoom") to
            ToolJump(MovieGroup.Effects.tag, MovieEffectSub.Motion.tag, "Motion"),
        listOf("stabilize", "reframe", "steady", "frame") to
            ToolJump(MovieGroup.Effects.tag, MovieEffectSub.Frame.tag, "Steady"),
        listOf("chroma", "key", "green", "blue screen") to
            ToolJump(MovieGroup.Effects.tag, MovieEffectSub.Chroma.tag, "Chroma"),
        listOf("slow", "slowmo", "interp", "film") to
            ToolJump(MovieGroup.Effects.tag, MovieEffectSub.SlowMo.tag, "Slow-mo"),
        listOf("track", "roto", "follow") to
            ToolJump(MovieGroup.Effects.tag, MovieEffectSub.Track.tag, "Track"),
        listOf("vfx", "particle", "sparkle", "ember", "snow") to
            ToolJump(MovieGroup.Effects.tag, MovieEffectSub.Vfx.tag, "VFX"),
        listOf("cg", "gltf", "filament", "prop", "ibl", "3d", "set extension", "sky dome") to
            ToolJump(MovieGroup.Cg.tag, label = "CG"),
        listOf("layout", "pip", "multicam", "split screen", "grid") to
            ToolJump(MovieGroup.Layout.tag, label = "Layout"),
        listOf("export", "deliver", "handoff", "review", "edl") to
            ToolJump(MovieGroup.Deliver.tag, label = "Deliver")
    )

    fun find(query: String, limit: Int = 6): List<ToolJump> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        val hits = LinkedHashMap<String, ToolJump>()
        for ((keys, jump) in catalog) {
            if (keys.any { it.contains(q) || q.contains(it.trim()) }) {
                hits.putIfAbsent("${jump.groupTag}|${jump.effectSubTag}|${jump.label}", jump)
            }
        }
        return hits.values.take(limit)
    }
}
