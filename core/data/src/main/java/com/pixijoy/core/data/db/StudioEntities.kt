package com.pixijoy.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects", indices = [Index("updatedAt")])
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val kind: String,
    val createdAt: Long,
    val updatedAt: Long,
    val coverPath: String,
    val graph: String
)

@Entity(
    tableName = "media_refs",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class MediaRefEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val path: String,
    val mime: String,
    val width: Int,
    val height: Int,
    val durationMs: Long?,
    val sortOrder: Int
)
