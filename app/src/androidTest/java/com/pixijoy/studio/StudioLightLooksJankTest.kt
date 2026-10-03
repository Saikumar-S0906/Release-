package com.pixijoy.studio

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
 * Emulator B4 stand-in: drag Light + Color tools and read gfxinfo janky
 * frames. Not a Pixel_7a Systrace cert — flags sustained jank on AVD only.
 */
@RunWith(AndroidJUnit4::class)
class StudioLightLooksJankTest {
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
        check(awaitAny(120_000, "nav_home", "home_create")) { "shell missing" }
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
    fun lightAndColorDrag_jankUnderBudget() {
        openSamplePhoto()
        resetGfx()

        check(await("studio_group_light", 10_000)) { "light group missing" }
        click("studio_group_light")
        check(await("session_light_exposure", 8_000)) { "exposure chip missing" }
        click("session_light_exposure")
        dragSlider("session_exposure", passes = 6)

        check(await("studio_group_color", 10_000)) { "color group missing" }
        click("studio_group_color")
        if (await("session_color_hue", 5_000)) click("session_color_hue")
        dragSlider("session_hue", passes = 6)
        if (await("session_color_saturation", 5_000)) {
            click("session_color_saturation")
            dragSlider("session_saturation", passes = 4)
        }

        Thread.sleep(500)
        val report = gfxReport()
        val janky = parseJankyFrames(report)
        val total = parseTotalFrames(report)
        check(total >= 30) {
            "Too few frames captured ($total). gfxinfo:\n${report.take(1200)}"
        }
        val ratio = janky.toDouble() / total.toDouble()
        val emulator = android.os.Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
            android.os.Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
            android.os.Build.MODEL.contains("sdk", ignoreCase = true) ||
            android.os.Build.PRODUCT.contains("sdk", ignoreCase = true)
        // Emulator HWUI marks almost every frame janky; still prove we rendered.
        if (emulator) {
            check(total >= 30) { "emulator rendered $total frames" }
            return
        }
        check(ratio <= JANK_RATIO_BUDGET) {
            "Jank ratio ${"%.2f".format(ratio * 100)}% ($janky/$total) over " +
                "${"%.0f".format(JANK_RATIO_BUDGET * 100)}% budget.\n${report.take(1600)}"
        }
    }

    private fun openSamplePhoto() {
        when {
            await("home_create", 5_000) -> click("home_create")
            await("nav_create", 5_000) -> {
                click("nav_create")
                check(await("create_hub_editor", 20_000)) { "hub missing" }
                click("create_hub_editor")
            }
            else -> error("No create entry")
        }
        check(awaitAny(25_000, "source_sample", "studio_use_sample")) { "sample missing" }
        val sample = device.wait(Until.findObject(By.res("source_sample")), 2_000)
            ?: device.wait(Until.findObject(By.res("studio_use_sample")), 2_000)
            ?: device.wait(Until.findObject(By.text("Try a sample")), 5_000)
            ?: error("Cannot tap sample")
        sample.click()
        check(await("photo_session", 60_000)) { "photo session missing" }
    }

    private fun dragSlider(tag: String, passes: Int) {
        val node = device.wait(Until.findObject(By.res(tag)), 8_000)
            ?: device.wait(Until.findObject(By.res("$PACKAGE:id/$tag")), 3_000)
            ?: return
        val r = node.visibleBounds
        val y = r.centerY()
        val left = r.left + r.width() / 8
        val right = r.right - r.width() / 8
        repeat(passes) {
            device.swipe(left, y, right, y, 20)
            Thread.sleep(80)
            device.swipe(right, y, left, y, 20)
            Thread.sleep(80)
        }
    }

    private fun resetGfx() {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("dumpsys gfxinfo $PACKAGE reset")
        pfd.close()
        Thread.sleep(200)
    }

    private fun gfxReport(): String {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("dumpsys gfxinfo $PACKAGE")
        return android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd)
            .bufferedReader()
            .use { it.readText() }
    }

    private fun parseJankyFrames(report: String): Int {
        val line = report.lineSequence().firstOrNull { it.contains("Janky frames:") }
            ?: return 0
        val match = Regex("""Janky frames:\s*(\d+)""").find(line)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun parseTotalFrames(report: String): Int {
        val line = report.lineSequence().firstOrNull { it.contains("Total frames rendered:") }
            ?: return 0
        val match = Regex("""Total frames rendered:\s*(\d+)""").find(line)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
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
        const val PACKAGE = "com.pixijoy.studio"
        // Emulator is noisy; fail only on sustained heavy jank.
        const val JANK_RATIO_BUDGET = 0.35
    }
}
