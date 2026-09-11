package com.pixijoy.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pixijoy.ai.qa.QaReportManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QaReportManagerTest {

    @Test
    fun buildsReportFile_withDeviceInfo() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = QaReportManager(context.applicationContext)
        manager.install()
        manager.log("Test event from QaReportManagerTest")

        val file = runBlocking { manager.buildReportFile() }
        val text = file.readText()

        assertTrue(file.exists())
        assertTrue(text.contains("PIXIJOY QA REPORT"))
        assertTrue(text.contains("Test event from QaReportManagerTest"))
        assertTrue(text.contains(context.packageName))
    }
}
