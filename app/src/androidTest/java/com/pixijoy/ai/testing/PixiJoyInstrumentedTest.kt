package com.pixijoy.ai.testing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.pixijoy.ai.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule

abstract class PixiJoyInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun baseSetUp() {
        QaTestResultRecorder.clear()
        seedDefaultState()
        // Avoid Activity.recreate() — it caused Compose dispose crashes (AIOOBE) under load.
        // Gate navigation LaunchedEffect reacts to DataStore seed instead.
        composeRule.waitForIdle()
        waitForMainOrPrivacy()
    }

    @After
    fun baseTearDown() {
        QaTestResultRecorder.flushToDisk()
    }

    protected open fun seedDefaultState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        TestStateSeeder.prepareMainFlow(context)
    }

    /** After seeding main flow, home should appear; tolerate brief privacy flash. */
    protected fun waitForMainOrPrivacy(timeoutMs: Long = 30_000) {
        composeRule.waitUntil(timeoutMs) {
            runCatching {
                composeRule.onNodeWithTag(TestTags.HOME_HERO).assertExists()
                true
            }.getOrElse {
                runCatching {
                    composeRule.onNodeWithTag(TestTags.FLOW2_HOME).assertExists()
                    true
                }.getOrElse {
                    runCatching {
                        composeRule.onNodeWithTag(TestTags.PRIVACY_ACCEPT).assertExists()
                        true
                    }.getOrDefault(false)
                }
            }
        }
        // If still on privacy after seed, tap accept once (defensive for race).
        runCatching {
            composeRule.onNodeWithTag(TestTags.PRIVACY_ACCEPT).assertExists()
            composeRule.onNodeWithTag(TestTags.PRIVACY_ACCEPT).performClick()
            composeRule.waitForIdle()
        }
        runCatching {
            composeRule.onNodeWithTag(TestTags.ONBOARDING_SKIP).assertExists()
            composeRule.onNodeWithTag(TestTags.ONBOARDING_SKIP).performClick()
            composeRule.waitForIdle()
        }
        // Prefer board home tags (HOME_HERO aliases FLOW2_HERO).
        composeRule.waitUntil(timeoutMs) {
            runCatching {
                composeRule.onNodeWithTag(TestTags.HOME_HERO).assertExists()
                true
            }.getOrElse {
                runCatching {
                    composeRule.onNodeWithTag(TestTags.FLOW2_HOME).assertExists()
                    true
                }.getOrDefault(false)
            }
        }
    }

    protected fun tap(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.waitForIdle()
    }

    protected fun waitFor(tag: String, timeoutMs: Long = 30_000) {
        composeRule.waitUntil(timeoutMs) {
            runCatching {
                composeRule.onNodeWithTag(tag).assertExists()
                true
            }.getOrDefault(false)
        }
    }

    protected fun assertDisplayed(tag: String) {
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }
}
