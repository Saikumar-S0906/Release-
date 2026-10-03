package com.pixijoy.core.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.withTransaction
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
    ): AppResult<Project> = importNewProject(kind, title, "Could not import media.") { dir ->
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Photo picker URIs often cannot persist; copy-on-import covers that.
        }
        val tmp = File(dir, "incoming")
        val input = context.contentResolver.openInputStream(uri)
        if (input == null) {
            AppResult.Err("Could not read that file.")
        } else {
            input.use { stream -> tmp.outputStream().use { stream.copyTo(it) } }
            AppResult.Ok(tmp)
        }
    }

    suspend fun importFile(
        file: File,
        kind: MediaKind,
        title: String
    ): AppResult<Project> = importNewProject(kind, title, "Could not import capture.") { dir ->
        val tmp = File(dir, "incoming")
        file.copyTo(tmp, overwrite = true)
        AppResult.Ok(tmp)
    }

    suspend fun importBitmap(
        bitmap: Bitmap,
        title: String,
        kind: MediaKind = MediaKind.PHOTO
    ): AppResult<Project> = importNewProject(kind, title, "Could not save sample.") { dir ->
        val tmp = File(dir, "incoming")
        val wrote = tmp.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        if (!wrote) AppResult.Err("Could not save sample.") else AppResult.Ok(tmp)
    }

    suspend fun addUri(projectId: String, uri: Uri): AppResult<Project> = withContext(dispatchers.io) {
        val row = dao.get(projectId) ?: return@withContext AppResult.Err("Project missing.")
        val dir = File(context.filesDir, "projects/$projectId").apply { mkdirs() }
        val tmp = File(dir, "incoming-${System.currentTimeMillis()}")
        var dest: File? = null
        var mediaSaved = false
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext AppResult.Err("Could not read that file.")
            input.use { stream -> tmp.outputStream().use { stream.copyTo(it) } }
            if (tmp.length() <= 0L) {
                return@withContext AppResult.Err("Could not read that file.")
            }
            val info = MediaProbe.probe(tmp)
            val ext = extensionFor(info.mime, info.durationMs, pathHint = tmp.name)
            val file = File(dir, "extra-${System.currentTimeMillis()}.$ext")
            dest = file
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
            val order = dao.maxSortOrder(projectId) + 1
            db.withTransaction {
                dao.upsertMedia(
                    MediaRefEntity(
                        id = UUID.randomUUID().toString(),
                        projectId = projectId,
                        path = file.absolutePath,
                        mime = info.mime,
                        width = info.width,
                        height = info.height,
                        durationMs = info.durationMs,
                        sortOrder = order
                    )
                )
                dao.upsertProject(row.project.copy(updatedAt = System.currentTimeMillis()))
            }
            mediaSaved = true
            val saved = dao.get(projectId) ?: return@withContext AppResult.Err("Project missing.")
            AppResult.Ok(saved.toProject())
        } catch (t: Throwable) {
            AppResult.Err("Could not add photo.", t)
        } finally {
            if (tmp.exists()) tmp.delete()
            if (!mediaSaved) dest?.delete()
        }
    }

    suspend fun importAudioBed(projectId: String, uri: Uri): AppResult<String> =
        importAudioFile(projectId, uri, "bed")

    suspend fun importAudioFile(
        projectId: String,
        uri: Uri,
        prefix: String
    ): AppResult<String> = withContext(dispatchers.io) {
        dao.get(projectId) ?: return@withContext AppResult.Err("Project missing.")
        val dir = File(context.filesDir, "projects/$projectId").apply { mkdirs() }
        val tmp = File(dir, "$prefix-incoming")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext AppResult.Err("Could not read that audio.")
            input.use { stream -> tmp.outputStream().use { stream.copyTo(it) } }
            if (tmp.length() <= 0L) {
                return@withContext AppResult.Err("Could not read that audio.")
            }
            val info = MediaProbe.probe(tmp)
            val ext = audioExtension(info.mime)
            val dest = File(dir, "$prefix.$ext")
            if (!replaceFile(tmp, dest)) {
                return@withContext AppResult.Err("Could not add audio.")
            }
            dir.listFiles()
                ?.filter { it.name.startsWith("$prefix.") && it.absolutePath != dest.absolutePath }
                ?.forEach { it.delete() }
            AppResult.Ok(dest.absolutePath)
        } catch (t: Throwable) {
            AppResult.Err("Could not add audio.", t)
        } finally {
            if (tmp.exists()) tmp.delete()
        }
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
            val dir = File(projectDir(projectId), "luts").apply { mkdirs() }
            val name = sanitizeLutFileName(suggestedName)
            val dest = File(dir, name)
            val tmp = File(dir, ".$name.part")
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: return@withContext AppResult.Err("Could not read LUT.")
                input.use { stream -> tmp.outputStream().use { output -> stream.copyTo(output) } }
                if (tmp.length() < 8L) {
                    return@withContext AppResult.Err("LUT file is empty.")
                }
                if (tmp.length() > 8L * 1024L * 1024L) {
                    return@withContext AppResult.Err("LUT file is too large.")
                }
                if (!replaceFile(tmp, dest)) {
                    return@withContext AppResult.Err("Could not import LUT.")
                }
                AppResult.Ok(dest)
            } catch (t: Throwable) {
                AppResult.Err("Could not import LUT.", t)
            } finally {
                if (tmp.exists()) tmp.delete()
            }
        }

    /**
     * Copies a `.gltf` or `.glb` into `{project}/props/` (same folder the CG plate reads).
     * Caller validates the mesh before keeping the file.
     */
    suspend fun importGltfProp(projectId: String, uri: Uri, suggestedName: String): AppResult<File> =
        withContext(dispatchers.io) {
            val dir = File(projectDir(projectId), "props")
            var dest: File? = null
            var kept = false
            try {
                dao.get(projectId) ?: return@withContext AppResult.Err("Project missing.")
                val name = sanitizePropFileName(suggestedName)
                if (name.isEmpty()) return@withContext AppResult.Err("Use a .gltf or .glb file.")
                dir.mkdirs()
                val file = uniquePropFile(dir, name)
                dest = file
                val input = context.contentResolver.openInputStream(uri)
                    ?: return@withContext AppResult.Err("Could not read prop.")
                input.use { stream -> file.outputStream().use { output -> stream.copyTo(output) } }
                if (file.length() < 20L) {
                    return@withContext AppResult.Err("Prop file is empty.")
                }
                if (file.length() > 8L * 1024L * 1024L) {
                    return@withContext AppResult.Err("Prop file is too large.")
                }
                kept = true
                AppResult.Ok(file)
            } catch (t: Throwable) {
                AppResult.Err("Could not import prop.", t)
            } finally {
                if (!kept) dest?.delete()
            }
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
        db.withTransaction {
            dao.upsertProject(entity)
            dao.upsertMedia(media)
        }
        return AppResult.Ok(ProjectWithMedia(entity, listOf(media)).toProject())
    }

    /**
     * New project id is unpublished until both rows commit.
     * Any earlier failure deletes the row and the folder.
     */
    private suspend fun importNewProject(
        kind: MediaKind,
        title: String,
        fallback: String,
        writeIncoming: suspend (dir: File) -> AppResult<File>
    ): AppResult<Project> = withContext(dispatchers.io) {
        val id = UUID.randomUUID().toString()
        val dir = File(context.filesDir, "projects/$id")
        var committed = false
        try {
            if (!dir.mkdirs() && !dir.isDirectory) {
                return@withContext AppResult.Err(fallback)
            }
            when (val prepared = writeIncoming(dir)) {
                is AppResult.Err -> prepared
                is AppResult.Ok -> {
                    val tmp = prepared.value
                    if (!tmp.isFile || tmp.length() <= 0L) {
                        AppResult.Err("Could not read that file.")
                    } else {
                        val result = finishImport(id, dir, tmp, kind, title)
                        committed = result is AppResult.Ok
                        result
                    }
                }
            }
        } catch (t: Throwable) {
            AppResult.Err(fallback, t)
        } finally {
            if (!committed) {
                runCatching { dao.deleteProject(id) }
                dir.deleteRecursively()
            }
        }
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

/** Next free prop name. Never returns a path that already exists. */
internal fun uniquePropFile(dir: File, name: String): File {
    val first = File(dir, name)
    if (!first.exists()) return first
    val stem = name.substringBeforeLast('.')
    val ext = name.substringAfterLast('.', "")
    var i = 2
    while (i < 100) {
        val candidate = File(dir, "${stem}_$i.$ext")
        if (!candidate.exists()) return candidate
        i++
    }
    var stamp = System.currentTimeMillis()
    var fallback = File(dir, "${stem}_$stamp.$ext")
    while (fallback.exists()) {
        stamp++
        fallback = File(dir, "${stem}_$stamp.$ext")
    }
    return fallback
}

/**
 * Replaces [dest] with [tmp].
 * If the swap fails, [dest] keeps its previous bytes when it had any.
 */
internal fun replaceFile(tmp: File, dest: File): Boolean {
    val parent = dest.parentFile ?: return false
    val backup = File(parent, ".${dest.name}.bak")
    if (backup.exists() && !backup.delete()) return false
    val hadDest = dest.exists()
    if (hadDest && !dest.renameTo(backup)) {
        tmp.delete()
        return false
    }
    val moved = if (tmp.renameTo(dest)) {
        true
    } else {
        runCatching {
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }.isSuccess
    }
    if (moved && dest.isFile && dest.length() > 0L) {
        backup.delete()
        return true
    }
    if (dest.exists()) dest.delete()
    if (hadDest) backup.renameTo(dest)
    if (tmp.exists()) tmp.delete()
    return false
}

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
