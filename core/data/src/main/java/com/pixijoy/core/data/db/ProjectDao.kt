package com.pixijoy.core.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class ProjectWithMedia(
    @Embedded val project: ProjectEntity,
    @Relation(parentColumn = "id", entityColumn = "projectId")
    val media: List<MediaRefEntity>
)

@Dao
interface ProjectDao {
    @Transaction
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProjectWithMedia>>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun get(id: String): ProjectWithMedia?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(entity: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedia(entity: MediaRefEntity)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM media_refs WHERE projectId = :projectId")
    suspend fun maxSortOrder(projectId: String): Int

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)

    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()
}
