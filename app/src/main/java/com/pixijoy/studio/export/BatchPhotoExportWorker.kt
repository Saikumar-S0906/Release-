package com.pixijoy.studio.export

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.pixijoy.studio.di.studioGraph
import com.pixijoy.core.common.AppResult
import com.pixijoy.photo.BatchPresetApply
import com.pixijoy.photo.ColorIntent
import com.pixijoy.photo.ExportExifPolicy
import com.pixijoy.photo.ExportFormatSupport
import com.pixijoy.photo.PhotoPreset
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * C6 — export many photo projects sequentially on-device (WorkManager).
 */
class BatchPhotoExportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ids = inputData.getStringArray(KEY_PROJECTS)?.toList().orEmpty()
        if (ids.isEmpty()) {
            return Result.failure(workDataOf(KEY_ERROR to "No projects."))
        }
        val format = ExportFormatSupport.parseKey(inputData.getString(KEY_FORMAT) ?: "jpeg")
        val quality = inputData.getInt(KEY_QUALITY, 92)
        val keepExif = inputData.getBoolean(KEY_KEEP_EXIF, false)
        val stripGps = inputData.getBoolean(KEY_STRIP_GPS, ExportExifPolicy.DEFAULT_STRIP_GPS)
        val colorIntent = when (inputData.getString(KEY_COLOR_INTENT)) {
            "display_p3" -> ColorIntent.DisplayP3
            else -> ColorIntent.Srgb
        }
        val preset = inputData.getString(KEY_PRESET)?.let(PhotoPreset::decode)
        setForeground(foreground(0))
        return withContext(Dispatchers.IO) {
            val graph = studioGraph(applicationContext)
            val repo = graph.repository()
            val engine = graph.exportPhotoEngine()
            var ok = 0
            ids.forEachIndexed { index, projectId ->
                val project = repo.get(projectId) ?: return@forEachIndexed
                if (project.kind != com.pixijoy.core.media.MediaKind.PHOTO) return@forEachIndexed
                engine.lutDirectory = File(applicationContext.filesDir, "projects/$projectId")
                val sources = project.media.map { it.path }.ifEmpty { listOf(project.coverPath) }
                val layers = BatchPresetApply.layersForExport(project.graph, preset)
                if (preset != null) {
                    repo.saveGraph(projectId, BatchPresetApply.appliedGraph(project.graph, preset))
                }
                val display = "PixiJoy_${project.title}_${System.currentTimeMillis()}.${format.extension}"
                val progress = ((index + 1f) / ids.size).coerceIn(0f, 1f)
                setProgressAsync(workDataOf(KEY_PROGRESS to progress))
                notify((progress * 100).toInt())
                val result = engine.exportLayers(
                    applicationContext,
                    sources,
                    layers,
                    display,
                    format,
                    quality,
                    keepExif = keepExif,
                    stripGps = stripGps,
                    colorIntent = colorIntent
                )
                if (result is AppResult.Ok) ok++
            }
            if (ok == 0) Result.failure(workDataOf(KEY_ERROR to "Nothing exported."))
            else Result.success(workDataOf(KEY_COUNT to ok))
        }
    }

    private fun notify(progress: Int) {
        applicationContext.getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, ExportNotifications.notification(applicationContext, progress))
    }

    private fun foreground(progress: Int): ForegroundInfo =
        ForegroundInfo(
            NOTIF_ID,
            ExportNotifications.notification(applicationContext, progress),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

    companion object {
        const val KEY_PROJECTS = "projectIds"
        const val KEY_FORMAT = "format"
        const val KEY_QUALITY = "quality"
        const val KEY_KEEP_EXIF = "keepExif"
        const val KEY_STRIP_GPS = "stripGps"
        const val KEY_COLOR_INTENT = "colorIntent"
        const val KEY_PRESET = "preset"
        const val KEY_PROGRESS = "progress"
        const val KEY_COUNT = "count"
        const val KEY_ERROR = "error"
        private const val NOTIF_ID = 44

        fun uniqueName() = "photo-batch-export"
    }
}
