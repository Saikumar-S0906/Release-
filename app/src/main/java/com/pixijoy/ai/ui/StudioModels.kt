package com.pixijoy.ai.ui

import com.pixijoy.core.data.Project
import com.pixijoy.core.media.MediaKind

enum class StudioTab { Home, Studio, Library }

data class OpenSession(
    val project: Project,
    val tool: String
) {
    val kind: MediaKind get() = project.kind
}

data class PendingPick(
    val tool: String,
    val kind: MediaKind,
    val title: String
)
