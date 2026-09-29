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
import com.pixijoy.core.media.RawPreview
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
        title: String,
        kind: MediaKind = MediaKind.PHOTO
    ): AppResult<Project> = withContext(dispatchers.io) {
        runCatching {
            val id = UUID.randomUUID().toString()
            val dir = File(context.filesDir, "projects/$id").apply { mkdirs() }
            val tmp = File(dir, "incoming")
            tmp.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            finishImport(id, dir, tmp, kind, title)
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
            val ext = extensionFor(info.mime, info.durationMs, pathHint = tmp.name)
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

    suspend fun rename(id: String, title: String): Unit = withContext(dispatchers.io) {
        val row = dao.get(id)?.project ?: return@withContext
        val cleaned = title.trim().ifBlank { row.title }
        dao.upsertProject(row.copy(title = cleaned, updatedAt = System.currentTimeMillis()))
    }

    /** Hard-delete project row and on-disk media. Call only after an Undo window expires. */
    suspend fun delete(id: String): Unit = withContext(dispatchers.io) {
        dao.deleteProject(id)
        File(context.filesDir, "projects/$id").deleteRecursively()
    }

    /** Wipe every project row and the on-disk projects tree. */
    suspend fun deleteAll(): Unit = withContext(dispatchers.io) {
        dao.deleteAllProjects()
        File(context.filesDir, "projects").deleteRecursively()
    }

    fun projectDir(id: String): File = File(context.filesDir, "projects/$id")

    fun presetsDir(): File = File(context.filesDir, "presets").also { it.mkdirs() }

    suspend fun listPresetFiles(): List<File> = withContext(dispatchers.io) {
        presetsDir().listFiles()?.filter { it.extension == "preset" }?.sortedByDescending { it.lastModified() }.orEmpty()
    }

    suspend fun writePresetFile(id: String, body: String): AppResult<File> = withContext(dispatchers.io) {
        runCatching {
            val dest = File(presetsDir(), "$id.preset")
            dest.writeText(body)
            AppResult.Ok(dest)
        }.fold(
            onSuccess = { it },
            onFailure = { AppResult.Err("Could not save preset.", it) }
        )
    }

    suspend fun readPresetFile(presetId: String): AppResult<String> = withContext(dispatchers.io) {
        val file = File(presetsDir(), "$presetId.preset")
        if (!file.exists()) return@withContext AppResult.Err("Preset missing.")
        runCatching { AppResult.Ok(file.readText()) }.fold(
            onSuccess = { it },
            onFailure = { AppResult.Err("Preset unreadable.", it) }
        )
    }

    /**
     * Copies a LUT file into `{project}/luts/`. Caller must validate parse before Apply.
     * @return destination file on success
     */
    suspend fun importLut(projectId: String, uri: Uri, suggestedName: String): AppResult<File> =
        withContext(dispatchers.io) {
            runCatching {
                val dir = File(projectDir(projectId), "luts").apply { mkdirs() }
                val name = sanitizeLutFileName(suggestedName)
                val dest = File(dir, name)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext AppResult.Err("Could not read LUT.")
                if (dest.length() < 8L) {
                    dest.delete()
                    return@withContext AppResult.Err("LUT file is empty.")
                }
                if (dest.length() > 8L * 1024L * 1024L) {
                    dest.delete()
                    return@withContext AppResult.Err("LUT file is too large.")
                }
                AppResult.Ok(dest)
            }.fold(
                onSuccess = { it },
                onFailure = { AppResult.Err("Could not import LUT.", it) }
            )
        }

    /**
     * Copies a `.gltf` or `.glb` into `{project}/props/` (same folder the CG plate reads).
     * Caller validates the mesh before keeping the file.
     */
    suspend fun importGltfProp(projectId: String, uri: Uri, suggestedName: String): AppResult<File> =
        withContext(dispatchers.io) {
            runCatching {
                dao.get(projectId) ?: return@withContext AppResult.Err("Project missing.")
                val name = sanitizePropFileName(suggestedName)
                if (name.isEmpty()) return@withContext AppResult.Err("Use a .gltf or .glb file.")
                val dir = File(projectDir(projectId), "props").apply { mkdirs() }
                val dest = uniquePropFile(dir, name)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext AppResult.Err("Could not read prop.")
                if (dest.length() < 20L) {
                    dest.delete()
                    return@withContext AppResult.Err("Prop file is empty.")
                }
                if (dest.length() > 8L * 1024L * 1024L) {
                    dest.delete()
                    return@withContext AppResult.Err("Prop file is too large.")
                }
                AppResult.Ok(dest)
            }.fold(
                onSuccess = { it },
                onFailure = { AppResult.Err("Could not import prop.", it) }
            )
        }

    private fun uniquePropFile(dir: File, name: String): File {
        var dest = File(dir, name)
        if (!dest.exists()) return dest
        val stem = name.substringBeforeLast('.')
        val ext = name.substringAfterLast('.')
        var i = 2
        while (dest.exists() && i < 100) {
            dest = File(dir, "${stem}_$i.$ext")
            i++
        }
        return dest
    }

    private fun sanitizePropFileName(raw: String): String {
        val base = raw.substringAfterLast('/').substringAfterLast('\\').substringAfterLast(':')
        val cleaned = buildString {
            for (ch in base) {
                append(
                    when {
                        ch.isLetterOrDigit() || ch == '.' || ch == '_' || ch == '-' -> ch
                        else -> '_'
                    }
                )
            }
        }.trim('.').ifBlank { "" }
        val lower = cleaned.lowercase()
        if (!lower.endsWith(".gltf") && !lower.endsWith(".glb")) return ""
        return cleaned.take(120)
    }

    private fun sanitizeLutFileName(raw: String): String {
        val base = raw.substringAfterLast('/').substringAfterLast('\\')
        val cleaned = buildString {
            for (ch in base) {
                append(
                    when {
                        ch.isLetterOrDigit() || ch == '.' || ch == '_' || ch == '-' -> ch
                        else -> '_'
                    }
                )
            }
        }.ifBlank { "import" }
        val lower = cleaned.lowercase()
        val named = when {
            lower.endsWith(".cube") || lower.endsWith(".3dl") -> cleaned
            else -> "$cleaned.cube"
        }
        return named.take(120)
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
            movie = kind == MediaKind.MOVIE,
            pathHint = tmp.name
        )
        val dest = File(dir, "source.$ext")
        if (!tmp.renameTo(dest)) {
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
        var coverPath = dest.absolutePath
        var width = info.width
        var height = info.height
        var mime = info.mime
        if (kind == MediaKind.PHOTO && (RawPreview.looksLikeRaw(dest.absolutePath) || RawPreview.isRawMime(mime))) {
            val raw = RawPreview.open(dest)
            width = raw.width.takeIf { it > 0 } ?: width
            height = raw.height.takeIf { it > 0 } ?: height
            mime = raw.mime
            if (RawPreview.ensurePreviewJpeg(dest)) {
                val preview = File(dir, "preview.jpg")
                if (preview.isFile) coverPath = preview.absolutePath
            }
        }
        val now = System.currentTimeMillis()
        val entity = ProjectEntity(
            id = id,
            title = title,
            kind = kind.name,
            createdAt = now,
            updatedAt = now,
            coverPath = coverPath,
            graph = ""
        )
        val media = MediaRefEntity(
            id = UUID.randomUUID().toString(),
            projectId = id,
            path = dest.absolutePath,
            mime = mime,
            width = width,
            height = height,
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

internal fun extensionFor(mime: String, durationMs: Long?, movie: Boolean = false, pathHint: String = ""): String {
    val m = mime.lowercase()
    val hint = pathHint.lowercase()
    return when {
        m.contains("dng") || hint.endsWith(".dng") -> "dng"
        m.contains("nef") || hint.endsWith(".nef") -> "nef"
        m.contains("cr2") || hint.endsWith(".cr2") -> "cr2"
        m.contains("arw") || hint.endsWith(".arw") -> "arw"
        RawPreview.isRawMime(m) || RawPreview.looksLikeRaw(hint) ->
            hint.substringAfterLast('.', "dng").ifBlank { "dng" }
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
