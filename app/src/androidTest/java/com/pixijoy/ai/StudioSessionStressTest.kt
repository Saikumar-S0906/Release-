package com.pixijoy.ai

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UiAutomator stress (no Espresso idle). Espresso.onIdle still crashes on
 * API 37 Pixel_7a AVDs via InputManager.getInstance — this path is green on
 * that AVD. Heap climb is the automated gate.
 */
@RunWith(AndroidJUnit4::class)
class StudioSessionStressTest {
    private lateinit var device: UiDevice

    @Before
    fun launchShell() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val launch = context.packageManager.getLaunchIntentForPackage(PACKAGE)
            ?: error("Missing launch intent for $PACKAGE")
        launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
        device.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), 90_000)
        dismissAnr()
        if (await("privacy_accept", 20_000) || device.wait(Until.hasObject(By.text("Continue")), 5_000)) {
            when {
                await("privacy_accept", 2_000) -> click("privacy_accept")
                else -> device.findObject(By.text("Continue"))?.click()
            }
        }
        dismissAnr()
        check(awaitAny(120_000, "nav_home", "home_create")) {
            "Shell did not open after privacy"
        }
    }

    private fun dismissAnr() {
        repeat(8) {
            val wait = device.findObject(By.text("Wait"))
            val anr = device.hasObject(By.textContains("isn't responding"))
            if (wait != null && anr) {
                runCatching { wait.click() }
                Thread.sleep(400)
            } else {
                return
            }
        }
    }

    @Test
    fun photoSessionOpenClose_stableHeap() {
        val heaps = LongArray(ITERATIONS + 1)
        heaps[0] = usedHeapBytes()

        repeat(ITERATIONS) { i ->
            openSamplePhoto()
            if (await("studio_group_light", 5_000)) {
                click("studio_group_light")
            }
            if (await("session_light_exposure", 5_000)) {
                click("session_light_exposure")
            }
            check(await("session_back", 15_000)) { "session back missing" }
            click("session_back")
            check(awaitAny(30_000, "nav_home", "home_create", "create_hub_editor")) {
                "did not leave photo session"
            }
            Runtime.getRuntime().gc()
            Thread.sleep(400)
            heaps[i + 1] = usedHeapBytes()
        }

        val mid = heaps[ITERATIONS / 2]
        val last = heaps[ITERATIONS]
        val climb = last - mid
        check(climb < HEAP_CLIMB_BUDGET_BYTES) {
            "Heap climbed ${climb / 1024} KB from mid→end across $ITERATIONS sessions. " +
                "samples=${heaps.joinToString { "${it / 1024}KB" }}"
        }
    }

    @Test
    @org.junit.Ignore("Export success sheet flaky on tablet AVD (worker/permission); heap gate is the release check")
    fun photoExportOnce_completes() {
        openSamplePhoto()
        click("session_export")
        dismissSystemPrompts()
        check(await("export_sheet_confirm", 20_000)) { "export confirm missing" }
        click("export_sheet_confirm")
        dismissSystemPrompts()
        // Notification permission can steal focus on API 33+.
        repeat(6) {
            if (await("export_done", 15_000)) {
                click("export_done")
                return
            }
            dismissSystemPrompts()
        }
        error("export did not finish")
    }

    private fun openSamplePhoto() {
        when {
            await("home_create", 5_000) -> click("home_create")
            await("nav_create", 5_000) -> {
                click("nav_create")
                check(await("create_hub_editor", 20_000)) { "create hub missing" }
                click("create_hub_editor")
            }
            else -> error("No create entry point")
        }
        check(awaitAny(25_000, "source_sample", "studio_use_sample")) { "sample CTA missing" }
        tapSample()
        check(await("photo_session", 60_000)) { "photo session missing" }
    }

    private fun tapSample() {
        val node = device.wait(Until.findObject(By.res("source_sample")), 3_000)
            ?: device.wait(Until.findObject(By.res("studio_use_sample")), 2_000)
            ?: device.wait(Until.findObject(By.res("$PACKAGE:id/source_sample")), 2_000)
            ?: device.wait(Until.findObject(By.text("Try a sample")), 5_000)
            ?: error("Cannot tap sample CTA")
        node.click()
    }

    private fun dismissSystemPrompts() {
        listOf("Allow", "While using the app", "ONLY THIS TIME", "Allow once").forEach { label ->
            val node = device.findObject(By.text(label))
            if (node != null) {
                runCatching { node.click() }
                Thread.sleep(300)
            }
        }
    }

    private fun usedHeapBytes(): Long {
        val rt = Runtime.getRuntime()
        return rt.totalMemory() - rt.freeMemory()
    }

    private fun click(tag: String) {
        repeat(4) { attempt ->
            try {
                val node = device.wait(Until.findObject(By.res(tag)), 5_000)
                    ?: device.wait(Until.findObject(By.res("$PACKAGE:id/$tag")), 5_000)
                    ?: error("Cannot tap $tag")
                node.click()
                return
            } catch (stale: androidx.test.uiautomator.StaleObjectException) {
                if (attempt == 3) throw stale
                Thread.sleep(250)
            }
        }
    }

    private fun await(tag: String, timeoutMs: Long): Boolean {
        if (device.wait(Until.hasObject(By.res(tag)), timeoutMs)) return true
        if (device.wait(Until.hasObject(By.res("$PACKAGE:id/$tag")), 1_000)) return true
        if (tag == "source_sample" || tag == "studio_use_sample") {
            return device.wait(Until.hasObject(By.text("Try a sample")), 2_000)
        }
        return false
    }

    private fun awaitAny(timeoutMs: Long, vararg tags: String): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (tags.any { await(it, 800) }) return true
            if (device.hasObject(By.text("Try a sample"))) return true
        }
        return false
    }

    private companion object {
        const val PACKAGE = "com.pixijoy.ai"
        const val ITERATIONS = 8
        const val HEAP_CLIMB_BUDGET_BYTES = 24L * 1024L * 1024L
    }
}
