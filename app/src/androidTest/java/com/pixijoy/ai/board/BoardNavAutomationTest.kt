package com.pixijoy.ai.board

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.pixijoy.ai.MainActivity
import com.pixijoy.ai.testing.TestStateSeeder
import com.pixijoy.ai.testing.TestTags
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device automation: seed app → bottom nav → board roots (UiAutomator + Compose).
 */
@RunWith(AndroidJUnit4::class)
class BoardNavAutomationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        TestStateSeeder.prepareMainFlow(context)
        composeRule.waitForIdle()
        Thread.sleep(600)
        runCatching {
            composeRule.onNodeWithTag(TestTags.PRIVACY_ACCEPT).performClick()
            composeRule.waitForIdle()
        }
        waitFor(TestTags.FLOW2_HOME)
    }

    @Test
    fun bottomNav_cyclesAllFourTabs() {
        tap(TestTags.NAV_CREATE)
        waitFor(TestTags.boardScreen("b_create_hub"))
        composeRule.onNodeWithTag(TestTags.boardScreen("b_create_hub")).assertIsDisplayed()

        tap(TestTags.NAV_DISCOVER)
        waitFor(TestTags.boardScreen("b2_discover"))

        tap(TestTags.NAV_MY_STUFF)
        waitFor(TestTags.boardScreen("b18_browse"))

        tap(TestTags.NAV_HOME)
        waitFor(TestTags.FLOW2_HOME)
        composeRule.onNodeWithTag(TestTags.FLOW2_HOME).assertIsDisplayed()
    }

    @Test
    fun device_pressHomeAndResume_stillShowsBoardShell() {
        device.pressHome()
        device.waitForIdle(2_000)
        // Relaunch via package
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        requireNotNull(launch)
        launch.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        ctx.startActivity(launch)
        device.wait(Until.hasObject(By.pkg(ctx.packageName).depth(0)), 10_000)
        composeRule.waitForIdle()
        // After resume, either home or privacy may flash
        composeRule.waitUntil(20_000) {
            composeRule.onAllNodesWithTag(TestTags.FLOW2_HOME).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithTag(TestTags.PRIVACY_ACCEPT).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithTag(TestTags.NAV_HOME).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitFor(tag: String, timeoutMs: Long = 30_000) {
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tap(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.waitForIdle()
    }
}
