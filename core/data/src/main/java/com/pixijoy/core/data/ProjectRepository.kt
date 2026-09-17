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

    suspend fun importFile(
        file: File,
        kind: MediaKind,
        title: String
    ): AppResult<Project> = withContext(dispatchers.io) {
        runCatching {
            val id = UUID.randomUUID().toString()
            val dir = File(context.filesDir, "projects/$id").apply { mkdirs() }
            val tmp = File(dir, "incoming")
            file.copyTo(tmp, overwrite = true)
            finishImport(id, dir, tmp, kind, title)
        }.fold(
            onSuccess = { it },
            onFailure = { AppResult.Err("Could not import capture.", it) }
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

    suspend fun addUri(projectId: String, uri: Uri): AppResult<Project> = withContext(dispatchers.io) {
        runCatching {
            val row = dao.get(projectId) ?: return@withContext AppResult.Err("Project missing.")
            val dir = File(context.filesDir, "projects/$projectId").apply { mkdirs() }
            val tmp = File(dir, "incoming-${System.currentTimeMillis()}")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { input.copyTo(it) }
            } ?: return@withContext AppResult.Err("Could not read that file.")
            val info = MediaProbe.probe(tmp)
            val ext = extensionFor(info.mime, info.durationMs)
            val dest = File(dir, "extra-${System.currentTimeMillis()}.$ext")
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
            val order = dao.maxSortOrder(projectId) + 1
            dao.upsertMedia(
                MediaRefEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    path = dest.absolutePath,
                    mime = info.mime,
                    width = info.width,
                    height = info.height,
                    durationMs = info.durationMs,
                    sortOrder = order
                )
            )
            dao.upsertProject(row.project.copy(updatedAt = System.currentTimeMillis()))
            AppResult.Ok(dao.get(projectId)!!.toProject())
        }.fold(
            onSuccess = { it },
            onFailure = { AppResult.Err("Could not add photo.", it) }
        )
    }

    suspend fun importAudioBed(projectId: String, uri: Uri): AppResult<String> =
        importAudioFile(projectId, uri, "bed")

    suspend fun importAudioFile(
        projectId: String,
        uri: Uri,
        prefix: String
    ): AppResult<String> = withContext(dispatchers.io) {
        runCatching {
            dao.get(projectId) ?: return@withContext AppResult.Err("Project missing.")
            val dir = File(context.filesDir, "projects/$projectId").apply { mkdirs() }
            val tmp = File(dir, "$prefix-incoming")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { input.copyTo(it) }
            } ?: return@withContext AppResult.Err("Could not read that audio.")
            val info = MediaProbe.probe(tmp)
            val ext = audioExtension(info.mime)
            dir.listFiles()?.filter { it.name.startsWith("$prefix.") }?.forEach { it.delete() }
            val dest = File(dir, "$prefix.$ext")
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
            AppResult.Ok(dest.absolutePath)
        }.fold(
            onSuccess = { it },
            onFailure = { AppResult.Err("Could not add audio.", it) }
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
        val ext = extensionFor(
            info.mime,
            info.durationMs,
            movie = kind == MediaKind.MOVIE
        )
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

internal fun extensionFor(mime: String, durationMs: Long?, movie: Boolean = false): String {
    val m = mime.lowercase()
    return when {
        m.contains("png") -> "png"
        m.contains("webp") -> "webp"
        m.startsWith("audio") || m.contains("mpeg") && !m.contains("video") -> audioExtension(m)
        m.contains("video") || movie -> "mp4"
        (durationMs ?: 0) > 0 && (m.contains("audio") || m.contains("mpeg") || m.contains("aac") || m.contains("mp4")) ->
            if (m.contains("video")) "mp4" else audioExtension(m)
        (durationMs ?: 0) > 0 -> "mp4"
        else -> "jpg"
    }
}

internal fun audioExtension(mime: String): String {
    val m = mime.lowercase()
    return when {
        m.contains("mpeg") || m.contains("mp3") -> "mp3"
        m.contains("wav") -> "wav"
        m.contains("ogg") || m.contains("opus") -> "ogg"
        m.contains("aac") || m.contains("m4a") || m.contains("mp4") -> "m4a"
        else -> "m4a"
    }
}
