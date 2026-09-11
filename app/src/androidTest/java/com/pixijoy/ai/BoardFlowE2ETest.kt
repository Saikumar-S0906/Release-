package com.pixijoy.ai

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pixijoy.ai.testing.TestStateSeeder
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardScreenHost
import com.pixijoy.ai.ui.theme.PixiJoyTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Launches each flow's entry board screen and asserts it composes without crashing.
 * Prefer [com.pixijoy.ai.board.BoardCatalogComposeTest] for full catalog coverage
 * and [com.pixijoy.ai.board.BoardFlowJourneyComposeTest] for journey E2E walks.
 */
@RunWith(Parameterized::class)
class BoardFlowE2ETest(private val screenId: String) {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun entryScreen_rendersWithoutCrash() {
        composeRule.setContent {
            PixiJoyTheme {
                BoardScreenHost(
                    screenId = screenId,
                    onBack = {},
                    onNavigate = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.boardScreen(screenId)).assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun flowEntryScreens(): Collection<Array<String>> = listOf(
            "b1_splash", "b1_welcome", "b1_features", "b1_for_everyone",
            "b1_permissions", "b1_signup", "b1_ready",
            "b1_skip", "b1_perm_denied", "b1_limited", "b1_signup_later",
            "b1_returning", "b1_whats_new",
            "b2_home", "b2_home_scroll", "b2_discover", "b2_discover_cat",
            "b2_template", "b2_template_create", "b2_template_result",
            "b2_search", "b2_trending", "b2_collections", "b2_seasons",
            "b2_creators", "b2_favorites", "b2_recent",
            "b3_chooser",
            "b4_result",
            "b_create_hub",
            "b5_choose", "b6_choose", "b7_sheet", "b_diff_choose",
            "b8_ready", "b9_open", "b10_discover", "b11_home",
            "b13_photo", "b14_input", "b15_open", "b16_select", "b17_open",
            "b18_browse", "b19_start", "b26_open",
            "b20_input", "b21_hub", "b22_welcome", "b23_dash", "b24_plans", "b25_center",
            "b12_intro", "b27_feed", "b28_banner", "b29_home", "b30_home",
            "b_c00_empty",
            "b_c04", "b_c05", "b_c06", "b_c07", "b_c08", "b_c09", "b_c10", "b_c11", "b_c12", "b_c14",
            "b_collab_select"
        ).map { arrayOf(it) }
    }
}

/** Full-app smoke after board production flip. */
@RunWith(AndroidJUnit4::class)
class BoardProductionLaunchSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun seed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        TestStateSeeder.prepareMainFlow(context)
        composeRule.waitForIdle()
        Thread.sleep(800)
        composeRule.waitForIdle()
        runCatching {
            composeRule.onNodeWithTag(TestTags.PRIVACY_ACCEPT).performClick()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun seededLaunch_showsBoardHome() {
        waitForHome()
        composeRule.onNodeWithTag(TestTags.FLOW2_HOME).assertIsDisplayed()
    }

    @Test
    fun bottomTabs_switchBoardRoots() {
        waitForHome()
        tap(TestTags.NAV_CREATE)
        waitFor(TestTags.boardScreen("b_create_hub"))
        tap(TestTags.NAV_DISCOVER)
        waitFor(TestTags.boardScreen("b2_discover"))
        tap(TestTags.NAV_MY_STUFF)
        waitFor(TestTags.boardScreen("b18_browse"))
        tap(TestTags.NAV_HOME)
        waitForHome()
    }

    private fun waitForHome() {
        waitFor(TestTags.FLOW2_HOME)
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
