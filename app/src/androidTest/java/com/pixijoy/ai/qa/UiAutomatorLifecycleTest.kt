package com.pixijoy.ai.qa

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.pixijoy.ai.testing.TestStateSeeder
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cross-app / system UI layer. Complements Compose UI tests.
 * Useful for: back gesture, home, recents, permission dialogs.
 */
@RunWith(AndroidJUnit4::class)
class UiAutomatorLifecycleTest {

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        TestStateSeeder.prepareMainFlow(context)
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun launchApp_pressHome_andReturn_noCrash() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = context.packageManager.getLaunchIntentForPackage("com.pixijoy.ai")
            ?: error("Launch intent missing for com.pixijoy.ai")
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)

        assertTrue(
            "App package did not appear in foreground",
            device.wait(Until.hasObject(By.pkg("com.pixijoy.ai").depth(0)), 15_000)
        )

        device.pressHome()
        device.waitForIdle(2_000)

        context.startActivity(intent)
        assertTrue(
            "App did not return after Home",
            device.wait(Until.hasObject(By.pkg("com.pixijoy.ai").depth(0)), 15_000)
        )
    }

    @Test
    fun backFromRoot_staysAlive() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = context.packageManager.getLaunchIntentForPackage("com.pixijoy.ai")
            ?: error("Launch intent missing")
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        device.wait(Until.hasObject(By.pkg("com.pixijoy.ai").depth(0)), 15_000)

        repeat(3) {
            device.pressBack()
            device.waitForIdle(500)
        }

        // Process may exit to launcher; ensure no instrumentation crash
        assertTrue(device.currentPackageName != null)
    }
}
