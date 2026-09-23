package com.pixijoy.ai.export

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.pixijoy.core.common.AppResult
import com.pixijoy.core.data.ProjectRepository
import com.pixijoy.ml.MediaPipeVision
import com.pixijoy.photo.BitmapPhotoEngine
import com.pixijoy.photo.ExportExifPolicy
import com.pixijoy.photo.ExportFormatSupport
import com.pixijoy.photo.decodeLayers
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoExportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val projectId = inputData.getString(KEY_PROJECT) ?: return Result.failure(
            workDataOf(KEY_ERROR to "Missing project.")
        )
        val format = ExportFormatSupport.parseKey(inputData.getString(KEY_FORMAT) ?: "jpeg")
        val quality = inputData.getInt(KEY_QUALITY, 92)
        val keepExif = inputData.getBoolean(KEY_KEEP_EXIF, false)
        val stripGps = inputData.getBoolean(KEY_STRIP_GPS, ExportExifPolicy.DEFAULT_STRIP_GPS)
        val colorIntent = when (inputData.getString(KEY_COLOR_INTENT)) {
            "display_p3" -> com.pixijoy.photo.ColorIntent.DisplayP3
            else -> com.pixijoy.photo.ColorIntent.Srgb
        }
        setForeground(foreground(0))
        return withContext(Dispatchers.IO) {
            val repo = ProjectRepository(applicationContext)
            val project = repo.get(projectId) ?: return@withContext Result.failure(
                workDataOf(KEY_ERROR to "Project missing.")
            )
            val engine = BitmapPhotoEngine(MediaPipeVision(applicationContext)).also {
                it.lutDirectory = File(applicationContext.filesDir, "projects/$projectId")
            }
            val sources = project.media.map { it.path }.ifEmpty { listOf(project.coverPath) }
            val layers = project.graph.decodeLayers()
            val name = "PixiJoy_${project.title}_${System.currentTimeMillis()}.${format.extension}"
            setProgressAsync(workDataOf(KEY_PROGRESS to 0.15f))
            notify(15)
            val result = engine.exportLayers(
                applicationContext,
                sources,
                layers,
                name,
                format,
                quality,
                keepExif = keepExif,
                stripGps = stripGps,
                colorIntent = colorIntent
            )
            when (result) {
                is AppResult.Ok -> {
                    setProgressAsync(workDataOf(KEY_PROGRESS to 1f))
                    Result.success(workDataOf(KEY_URI to result.value))
                }
                is AppResult.Err -> Result.failure(workDataOf(KEY_ERROR to result.message))
            }
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
        const val KEY_PROJECT = "projectId"
        const val KEY_FORMAT = "format"
        const val KEY_QUALITY = "quality"
        const val KEY_KEEP_EXIF = "keepExif"
        const val KEY_STRIP_GPS = "stripGps"
        const val KEY_COLOR_INTENT = "colorIntent"
        const val KEY_PROGRESS = "progress"
        const val KEY_URI = "uri"
        const val KEY_ERROR = "error"
        private const val NOTIF_ID = 43

        fun uniqueName(projectId: String) = "photo-export-$projectId"
    }
}
