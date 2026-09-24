package com.pixijoy.ai.ui

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.pixijoy.core.data.Project
import com.pixijoy.core.media.MediaKind

enum class StudioTab { Home, Studio, Library }

data class OpenSession(
    val projectId: String,
    val tool: String,
    val kind: MediaKind
) {
    internal fun toSaveable(): List<String> = listOf(projectId, tool, kind.name)

    companion object {
        val Saver: Saver<OpenSession?, Any> = listSaver<OpenSession?, String>(
            save = { it?.toSaveable().orEmpty() },
            restore = ::fromSaveable
        )

        fun fromProject(project: Project, tool: String): OpenSession =
            OpenSession(project.id, tool, project.kind)

        internal fun fromSaveable(values: List<String>): OpenSession? {
            if (values.size != 3 || values[0].isBlank()) return null
            val kind = runCatching { MediaKind.valueOf(values[2]) }.getOrNull() ?: return null
            return OpenSession(values[0], values[1], kind)
        }
    }
}

data class PendingPick(
    val tool: String,
    val kind: MediaKind,
    val title: String
)
