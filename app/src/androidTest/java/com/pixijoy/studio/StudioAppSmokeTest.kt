package com.pixijoy.studio

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        waitForTag("session_project_title")
        val sampleTitle = sessionTitle()
        val sampleLabel = composeRule.activity.getString(R.string.studio_sample_title)
        assertTrue(sampleTitle.contains(sampleLabel))

        // MainActivity handles orientation in configChanges, so the same
        // activity stays up. The sample title must still be on screen.
        val activity = composeRule.activity
        val before = activity.resources.configuration.orientation
        val target = if (before == Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        val expected = if (target == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            Configuration.ORIENTATION_LANDSCAPE
        } else {
            Configuration.ORIENTATION_PORTRAIT
        }
        activity.requestedOrientation = target
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.activity === activity &&
                activity.resources.configuration.orientation == expected
        }
        composeRule.waitForIdle()
        waitForTag("photo_session", timeoutMillis = 30_000)
        waitForTag("session_back")
        assertEquals(sampleTitle, sessionTitle())
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

    private fun sessionTitle(): String {
        val node = composeRule.onNodeWithTag("session_project_title").fetchSemanticsNode()
        val text = node.config.getOrNull(SemanticsProperties.Text).orEmpty()
        return text.joinToString(separator = "") { it.text }
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
