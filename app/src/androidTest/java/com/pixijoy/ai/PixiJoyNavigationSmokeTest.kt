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
class PixiJoyNavigationSmokeTest {

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
    fun homeTab_showsHero() {
        waitFor(TestTags.FLOW2_HERO)
        composeRule.onNodeWithTag(TestTags.FLOW2_HERO).assertIsDisplayed()
    }

    @Test
    fun allBottomTabs_areReachable() {
        waitFor(TestTags.FLOW2_HOME)

        tap(TestTags.NAV_CREATE)
        waitFor(TestTags.boardScreen("b_create_hub"))

        tap(TestTags.NAV_DISCOVER)
        composeRule.onNodeWithTag(TestTags.NAV_DISCOVER).assertIsDisplayed()
        waitFor(TestTags.boardScreen("b2_discover"))

        tap(TestTags.NAV_MY_STUFF)
        composeRule.onNodeWithTag(TestTags.NAV_MY_STUFF).assertIsDisplayed()
        waitFor(TestTags.boardScreen("b18_browse"))

        tap(TestTags.NAV_HOME)
        waitFor(TestTags.FLOW2_HOME)
        composeRule.onNodeWithTag(TestTags.FLOW2_HOME).assertIsDisplayed()
    }

    @Test
    fun createTab_opensCreateHub() {
        tap(TestTags.NAV_CREATE)
        waitFor(TestTags.boardScreen("b_create_hub"))
        composeRule.onNodeWithTag(TestTags.boardScreen("b_create_hub")).assertIsDisplayed()
    }

    private fun tap(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.waitForIdle()
    }

    private fun waitFor(tag: String, timeoutMs: Long = 30_000) {
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
