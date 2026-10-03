package com.pixijoy.studio

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudioAppSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun openShell() {
        composeRule.waitUntil(timeoutMillis = 90_000) {
            tagExists("privacy_accept") || tagExists("nav_home")
        }
        if (tagExists("privacy_accept")) {
            composeRule.onNodeWithTag("privacy_accept").performClick()
        }
        waitForTag("nav_home")
    }

    @Test
    fun shellTabsOpen() {
        composeRule.onNodeWithTag("nav_create").performClick()
        waitForTag("create_hub_editor")

        composeRule.onNodeWithTag("nav_my_stuff").performClick()
        waitForAnyTag("library_grid", "library_empty")

        composeRule.onNodeWithTag("nav_home").performClick()
        waitForTag("home_create")
    }

    @Test
    fun photoSampleRestoresAfterRotation() {
        composeRule.onNodeWithTag("nav_create").performClick()
        waitForTag("create_hub_editor")
        composeRule.onNodeWithTag("create_hub_editor").performClick()
        waitForTag("studio_use_sample")
        composeRule.onNodeWithTag("studio_use_sample").performClick()

        waitForTag("photo_session", timeoutMillis = 60_000)

        val beforeRotation = composeRule.activity
        beforeRotation.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.waitUntil(timeoutMillis = 60_000) {
            composeRule.activity !== beforeRotation
        }
        waitForTag("photo_session", timeoutMillis = 60_000)
        waitForTag("session_back")
    }

    @Test
    fun movieSampleOpensRealTimeline() {
        composeRule.onNodeWithTag("nav_create").performClick()
        waitForTag("create_hub_segment_movie")
        composeRule.onNodeWithTag("create_hub_segment_movie").performClick()
        waitForTag("create_hub_move")
        composeRule.onNodeWithTag("create_hub_move").performClick()
        waitForTag("studio_use_sample")
        composeRule.onNodeWithTag("studio_use_sample").performClick()

        waitForTag("movie_session", timeoutMillis = 90_000)
        waitForTag("movie_timeline", timeoutMillis = 30_000)
    }

    private fun waitForTag(tag: String, timeoutMillis: Long = 30_000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) { tagExists(tag) }
        composeRule.onNodeWithTag(tag).assertExists()
    }

    private fun waitForAnyTag(vararg tags: String, timeoutMillis: Long = 30_000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            tags.any(::tagExists)
        }
    }

    private fun tagExists(tag: String): Boolean =
        composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
}
