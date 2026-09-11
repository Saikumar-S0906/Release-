package com.pixijoy.ai.qa

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.pixijoy.ai.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Captures crashes and QA events on-device so users can share reports without logcat/root.
 */
@Singleton
class QaReportManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val events = ConcurrentLinkedQueue<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun install() {
        if (defaultHandler != null) return
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            recordCrash(thread.name, error)
            defaultHandler?.uncaughtException(thread, error)
        }
        log("QaReportManager installed")
    }

    fun log(message: String) {
        val line = "${dateFormat.format(Date())} INFO  $message"
        events.add(line)
        trim()
    }

    fun logError(message: String, error: Throwable? = null) {
        val sw = StringWriter()
        error?.printStackTrace(PrintWriter(sw))
        val line = buildString {
            append(dateFormat.format(Date()))
            append(" ERROR ")
            append(message)
            if (error != null) {
                append('\n')
                append(sw)
            }
        }
        events.add(line)
        trim()
    }

    private fun recordCrash(threadName: String, error: Throwable) {
        val sw = StringWriter()
        error.printStackTrace(PrintWriter(sw))
        val report = buildString {
            appendLine("=== CRASH REPORT ===")
            appendLine("Time: ${dateFormat.format(Date())}")
            appendLine("Thread: $threadName")
            appendLine("App: ${BuildConfig.APPLICATION_ID} v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} API ${Build.VERSION.SDK_INT}")
            appendLine(sw)
            appendLine("=== RECENT EVENTS ===")
            events.forEach { appendLine(it) }
        }
        runCatching {
            crashDir().mkdirs()
            File(crashDir(), "crash_${System.currentTimeMillis()}.txt").writeText(report)
        }
        events.add("${dateFormat.format(Date())} FATAL Crash on $threadName: ${error.message}")
    }

    suspend fun buildReportFile(): File = withContext(Dispatchers.IO) {
        val dir = reportDir()
        dir.mkdirs()
        val file = File(dir, "pixijoy_qa_report_${System.currentTimeMillis()}.txt")
        file.writeText(buildFullReport())
        file
    }

    suspend fun createShareIntent(): Intent? = withContext(Dispatchers.IO) {
        val file = buildReportFile()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "PixiJoy QA Report")
            putExtra(Intent.EXTRA_TEXT, "PixiJoy bug report attached.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun buildFullReport(): String = buildString {
        appendLine("=== PIXIJOY QA REPORT ===")
        appendLine("Generated: ${dateFormat.format(Date())}")
        appendLine("App: ${BuildConfig.APPLICATION_ID} v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Build: ${if (BuildConfig.DEBUG) "debug" else "release"}")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
        appendLine()
        appendLine("=== CRASH FILES ===")
        crashDir().listFiles()?.sortedByDescending { it.lastModified() }?.take(5)?.forEach { crash ->
            appendLine("--- ${crash.name} ---")
            appendLine(crash.readText())
            appendLine()
        } ?: appendLine("(none)")
        appendLine("=== EVENT LOG ===")
        events.forEach { appendLine(it) }
    }

    private fun reportDir(): File = File(context.filesDir, "qa_reports")
    private fun crashDir(): File = File(context.filesDir, "qa_crashes")

    private fun trim() {
        while (events.size > MAX_EVENTS) events.poll()
    }

    companion object {
        private const val MAX_EVENTS = 400
    }
}
