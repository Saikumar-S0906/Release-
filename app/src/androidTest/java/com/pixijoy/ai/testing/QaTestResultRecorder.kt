package com.pixijoy.ai.testing

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

object QaTestResultRecorder {
    private val results = CopyOnWriteArrayList<QaTestRunResult>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun record(caseId: String, passed: Boolean, message: String = "") {
        results.add(
            QaTestRunResult(
                caseId = caseId,
                passed = passed,
                message = message,
                timestamp = dateFormat.format(Date())
            )
        )
    }

    fun recordFromMatrix(case: QaTestCase, passed: Boolean, message: String = "") {
        record(case.id, passed, message)
    }

    fun flushToDisk() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(context.filesDir, "qa_test_results").apply { mkdirs() }
        val file = File(dir, "latest_run.txt")
        val report = buildString {
            appendLine("=== PIXIJOY QA TEST RUN ===")
            appendLine("Time: ${dateFormat.format(Date())}")
            appendLine("Total: ${results.size}")
            appendLine("Passed: ${results.count { it.passed }}")
            appendLine("Failed: ${results.count { !it.passed }}")
            appendLine()
            results.forEach { r ->
                appendLine("${if (r.passed) "PASS" else "FAIL"} | ${r.caseId} | ${r.message}")
            }
            appendLine()
            appendLine("=== MATRIX COVERAGE ===")
            val automated = QaTestMatrix.automated()
            val covered = results.map { it.caseId }.toSet()
            automated.forEach { c ->
                val status = if (c.id in covered) "RUN" else "NOT_RUN"
                appendLine("$status | ${c.id} | ${c.suite} | ${c.category} | ${c.title}")
            }
        }
        file.writeText(report)
    }

    fun clear() = results.clear()
}

data class QaTestRunResult(
    val caseId: String,
    val passed: Boolean,
    val message: String,
    val timestamp: String
)
