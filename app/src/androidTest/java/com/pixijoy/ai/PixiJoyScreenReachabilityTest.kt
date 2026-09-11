package com.pixijoy.ai

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pixijoy.ai.testing.TestStateSeeder
import com.pixijoy.ai.testing.TestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixiJoyScreenReachabilityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun seedAppState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        TestStateSeeder.prepareMainFlow(context)
        composeRule.waitForIdle()
        Thread.sleep(500)
        composeRule.waitForIdle()
        runCatching {
            composeRule.onNodeWithTag(TestTags.PRIVACY_ACCEPT).performClick()
            composeRule.waitForIdle()
        }
        waitFor(TestTags.FLOW2_HOME)
    }

    @Test
    fun myStuffTab_showsLibrary() {
        waitFor(TestTags.NAV_MY_STUFF)
        composeRule.onNodeWithTag(TestTags.NAV_MY_STUFF).performClick()
        waitFor(TestTags.boardScreen("b18_browse"))
        composeRule.onNodeWithTag(TestTags.boardScreen("b18_browse")).assertIsDisplayed()
    }

    @Test
    fun createTab_showsBoardChooser() {
        waitFor(TestTags.NAV_CREATE)
        composeRule.onNodeWithTag(TestTags.NAV_CREATE).performClick()
        waitFor(TestTags.boardScreen("b_create_hub"))
        composeRule.onNodeWithTag(TestTags.boardScreen("b_create_hub")).assertIsDisplayed()
    }

    @Test
    fun discoverTab_showsBoardDiscover() {
        waitFor(TestTags.NAV_DISCOVER)
        composeRule.onNodeWithTag(TestTags.NAV_DISCOVER).performClick()
        waitFor(TestTags.boardScreen("b2_discover"))
        composeRule.onNodeWithTag(TestTags.boardScreen("b2_discover")).assertIsDisplayed()
    }

    private fun waitFor(tag: String, timeoutMs: Long = 30_000) {
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
