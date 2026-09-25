package com.pixijoy.ai.export

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.pixijoy.ai.di.studioGraph
import com.pixijoy.core.common.AppResult
import com.pixijoy.movie.MultiTrackTimeline
import com.pixijoy.movie.Timeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MovieExportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val projectId = inputData.getString(KEY_PROJECT) ?: return Result.failure(
            workDataOf(KEY_ERROR to "Missing project.")
        )
        val gif = inputData.getBoolean(KEY_GIF, false)
        val talking = inputData.getBoolean(KEY_TALKING, false)
        setForeground(foreground(0))
        return withContext(Dispatchers.IO) {
            val graph = studioGraph(applicationContext)
            val repo = graph.repository()
            val project = repo.get(projectId) ?: return@withContext Result.failure(
                workDataOf(KEY_ERROR to "Project missing.")
            )
            val engine = graph.movieEngine()
            val paths = project.media.map { it.path }.ifEmpty { listOf(project.coverPath) }
            val multi = MultiTrackTimeline.decode(project.graph)
                ?: MultiTrackTimeline.fromFlatTimeline(
                    Timeline.decode(project.graph, paths.size) { i ->
                        engine.durationMs(paths.getOrElse(i) { paths.first() })
                    }
                )
            val result = engine.export(
                context = applicationContext,
                paths = paths,
                timeline = multi,
                talking = talking,
                gif = gif,
                onProgress = { p ->
                    val pct = (p * 100f).toInt().coerceIn(0, 100)
                    setProgressAsync(workDataOf(KEY_PROGRESS to p))
                    val nm = applicationContext.getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIF_ID, ExportNotifications.notification(applicationContext, pct))
                    // Keep FGS notification in sync without blocking the encode callback thread.
                    try {
                        setForegroundAsync(foreground(pct))
                    } catch (_: Exception) { }
                }
            )
            when (result) {
                is AppResult.Ok -> Result.success(workDataOf(KEY_URI to result.value, KEY_GIF to gif))
                is AppResult.Err -> Result.failure(workDataOf(KEY_ERROR to result.message))
            }
        }
    }

    private fun foreground(progress: Int): ForegroundInfo =
        ForegroundInfo(
            NOTIF_ID,
            ExportNotifications.notification(applicationContext, progress),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

    companion object {
        const val KEY_PROJECT = "projectId"
        const val KEY_GIF = "gif"
        const val KEY_TALKING = "talking"
        const val KEY_PROGRESS = "progress"
        const val KEY_URI = "uri"
        const val KEY_ERROR = "error"
        private const val NOTIF_ID = 42

        fun uniqueName(projectId: String) = "export-$projectId"

        fun ensureChannel(context: Context) = ExportNotifications.ensureChannel(context)
    }
}
