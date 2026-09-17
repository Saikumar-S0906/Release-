package com.pixijoy.core.data

import com.pixijoy.core.media.MediaKind

data class MediaRef(
    val id: String,
    val path: String,
    val mime: String,
    val width: Int,
    val height: Int,
    val durationMs: Long? = null
)

data class Project(
    val id: String,
    val title: String,
    val kind: MediaKind,
    val createdAt: Long,
    val updatedAt: Long,
    val coverPath: String,
    val graph: String,
    val media: List<MediaRef>
) {
    val source: MediaRef? get() = media.firstOrNull()
}
