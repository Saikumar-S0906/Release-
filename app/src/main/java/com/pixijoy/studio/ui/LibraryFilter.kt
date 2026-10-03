package com.pixijoy.studio.ui

import com.pixijoy.core.data.Project
import com.pixijoy.core.media.MediaKind

enum class LibraryFilter { ALL, PHOTO, MOVIE }

fun List<Project>.filterBy(filter: LibraryFilter): List<Project> = when (filter) {
    LibraryFilter.ALL -> this
    LibraryFilter.PHOTO -> filter { it.kind == MediaKind.PHOTO }
    LibraryFilter.MOVIE -> filter { it.kind == MediaKind.MOVIE }
}
