package com.pixijoy.movie

/**
 * R8 / coding P4 — Dialogue / Music / SFX group mute + solo.
 * Solo mutes every other bus; per-stem [AudioStem.muted] still applies underneath.
 */
enum class StemSolo { None, Dialogue, Music, Sfx }

data class StemBuses(
    val dialogueMuted: Boolean = false,
    val musicMuted: Boolean = false,
    val sfxMuted: Boolean = false,
    val solo: StemSolo = StemSolo.None
) {
    fun isDefault(): Boolean =
        !dialogueMuted && !musicMuted && !sfxMuted && solo == StemSolo.None

    fun encodeLine(): String =
        "SB,${b(dialogueMuted)},${b(musicMuted)},${b(sfxMuted)},${solo.name}"

    /** Bus mute after solo resolution (true = force silent). */
    fun forcesMute(bus: StemSolo): Boolean {
        if (bus == StemSolo.None) return false
        if (solo != StemSolo.None && solo != bus) return true
        return when (bus) {
            StemSolo.Dialogue -> dialogueMuted
            StemSolo.Music -> musicMuted
            StemSolo.Sfx -> sfxMuted
            StemSolo.None -> false
        }
    }

    companion object {
        val Default = StemBuses()

        fun decodeLine(parts: List<String>): StemBuses? {
            if (parts.getOrNull(0) != "SB" || parts.size < 5) return null
            return runCatching {
                StemBuses(
                    dialogueMuted = parts[1] == "1",
                    musicMuted = parts[2] == "1",
                    sfxMuted = parts[3] == "1",
                    solo = runCatching { StemSolo.valueOf(parts[4]) }.getOrDefault(StemSolo.None)
                )
            }.getOrNull()
        }

        private fun b(v: Boolean): String = if (v) "1" else "0"
    }
}

fun AudioStem.busKind(): StemSolo = when {
    role.equals("music", ignoreCase = true) || role.equals("bed", ignoreCase = true) -> StemSolo.Music
    role.equals("dialogue", ignoreCase = true) ||
        role.equals("vo", ignoreCase = true) ||
        role.equals("adr", ignoreCase = true) -> StemSolo.Dialogue
    else -> StemSolo.Sfx
}

/** Apply group mute/solo onto beds/stems for preview/export (does not mutate stored flags permanently). */
fun Timeline.withStemBusesApplied(): Timeline {
    val b = stemBuses
    if (b.isDefault()) return this
    return copy(
        voice = voice?.copy(muted = voice.muted || b.forcesMute(StemSolo.Dialogue)),
        bed = bed?.copy(muted = bed.muted || b.forcesMute(StemSolo.Music)),
        extraStems = extraStems.map { stem ->
            stem.copy(muted = stem.muted || b.forcesMute(stem.busKind()))
        }
    )
}
