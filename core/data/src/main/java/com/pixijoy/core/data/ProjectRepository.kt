package com.pixijoy.core.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.pixijoy.core.common.AppDispatchers
import com.pixijoy.core.common.AppResult
import com.pixijoy.core.data.db.MediaRefEntity
import com.pixijoy.core.data.db.ProjectEntity
import com.pixijoy.core.data.db.ProjectWithMedia
import com.pixijoy.core.data.db.StudioDatabase
import com.pixijoy.core.media.MediaKind
import com.pixijoy.core.media.MediaProbe
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProjectRepository(
    private val context: Context,
    private val db: StudioDatabase = StudioDatabase.get(context),
    private val dispatchers: AppDispatchers = AppDispatchers()
) {
    private val dao = db.projects()

    fun observeProjects(): Flow<List<Project>> =
        dao.observeAll().map { rows -> rows.map { it.toProject() } }

    suspend fun get(id: String): Project? = withContext(dispatchers.io) {
        dao.get(id)?.toProject()
    }

    suspend fun importUri(
        uri: Uri,
        kind: MediaKind,
        title: String
    ): AppResult<Project> = withContext(dispatchers.io) {
        runCatching {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Photo picker URIs often cannot persist; copy-on-import covers that.
            }
            val id = UUID.randomUUID().toString()
            val dir = File(context.filesDir, "projects/$id").apply { mkdirs() }
            val tmp = File(dir, "incoming")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { input.copyTo(it) }
            } ?: return@withContext AppResult.Err("Could not read that file.")
            finishImport(id, dir, tmp, kind, title)
        }.fold(
            onSuccess = { it },
            onFailure = { AppResult.Err("Could not import media.", it) }
        )
    }

    suspend fun importBitmap(
        bitmap: Bitmap,
        title: String
    ): AppResult<Project> = withContext(dispatchers.io) {
        runCatching {
            val id = UUID.randomUUID().toString()
            val dir = File(context.filesDir, "projects/$id").apply { mkdirs() }
            val tmp = File(dir, "incoming")
            tmp.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            finishImport(id, dir, tmp, MediaKind.PHOTO, title)
        }.fold(
            onSuccess = { it },
            onFailure = { AppResult.Err("Could not save sample.", it) }
        )
    }

    suspend fun saveGraph(id: String, graph: String): Unit = withContext(dispatchers.io) {
        val row = dao.get(id)?.project ?: return@withContext
        dao.upsertProject(row.copy(graph = graph, updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(id: String): Unit = withContext(dispatchers.io) {
        dao.deleteProject(id)
        File(context.filesDir, "projects/$id").deleteRecursively()
    }

    private suspend fun finishImport(
        id: String,
        dir: File,
        tmp: File,
        kind: MediaKind,
        title: String
    ): AppResult<Project> {
        val info = MediaProbe.probe(tmp)
        val ext = when {
            info.mime.contains("png") -> "png"
            info.mime.contains("webp") -> "webp"
            info.mime.contains("video") || kind == MediaKind.MOVIE -> "mp4"
            else -> "jpg"
        }
        val dest = File(dir, "source.$ext")
        if (!tmp.renameTo(dest)) {
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
        val now = System.currentTimeMillis()
        val entity = ProjectEntity(
            id = id,
            title = title,
            kind = kind.name,
            createdAt = now,
            updatedAt = now,
            coverPath = dest.absolutePath,
            graph = ""
        )
        val media = MediaRefEntity(
            id = UUID.randomUUID().toString(),
            projectId = id,
            path = dest.absolutePath,
            mime = info.mime,
            width = info.width,
            height = info.height,
            durationMs = info.durationMs,
            sortOrder = 0
        )
        dao.upsertProject(entity)
        dao.upsertMedia(media)
        return AppResult.Ok(ProjectWithMedia(entity, listOf(media)).toProject())
    }
}

private fun com.pixijoy.core.data.db.ProjectWithMedia.toProject() = Project(
    id = project.id,
    title = project.title,
    kind = runCatching { MediaKind.valueOf(project.kind) }.getOrDefault(MediaKind.PHOTO),
    createdAt = project.createdAt,
    updatedAt = project.updatedAt,
    coverPath = project.coverPath,
    graph = project.graph,
    media = media.map {
        MediaRef(
            id = it.id,
            path = it.path,
            mime = it.mime,
            width = it.width,
            height = it.height,
            durationMs = it.durationMs
        )
    }
)
