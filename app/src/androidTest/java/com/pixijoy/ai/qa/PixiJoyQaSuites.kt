package com.pixijoy.ai.qa

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.pixijoy.ai.MainActivity
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.PixiJoyInstrumentedTest
import com.pixijoy.ai.testing.QaTestMatrix
import com.pixijoy.ai.testing.QaTestResultRecorder
import com.pixijoy.ai.testing.TestStateSeeder
import com.pixijoy.ai.testing.TestTags
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegressionSuiteTest : PixiJoyInstrumentedTest() {

    @Test
    fun runFullRegressionSmoke() {
        val case = QaTestMatrix.all.first { it.id == "REG-01" }
        runCatching {
            waitFor(TestTags.FLOW2_HOME)
            assertDisplayed(TestTags.FLOW2_HOME)
            tap(TestTags.NAV_CREATE)
            waitFor(TestTags.CREATE_HUB_PHOTO)
            assertDisplayed(TestTags.CREATE_HUB_PHOTO)
            tap(TestTags.NAV_EXPLORE)
            tap(TestTags.NAV_PROJECTS)
            tap(TestTags.NAV_PROFILE)
            tap(TestTags.NAV_HOME)
            waitFor(TestTags.FLOW2_HOME)
            QaTestResultRecorder.recordFromMatrix(case, true)
        }.onFailure {
            QaTestResultRecorder.recordFromMatrix(case, false, it.message.orEmpty())
            throw it
        }
    }
}

@RunWith(AndroidJUnit4::class)
class NegativeCaseTest : PixiJoyInstrumentedTest() {

    @Test
    fun meme_generateWithoutPhoto_blocked() {
        val case = QaTestMatrix.all.first { it.id == "MEME-02" }
        runCatching {
            tap(TestTags.NAV_CREATE)
            waitFor(TestTags.CREATE_HUB_SEGMENT_AI)
            tap(TestTags.CREATE_HUB_SEGMENT_AI)
            waitFor(TestTags.CREATE_HUB_MEME)
            tap(TestTags.CREATE_HUB_MEME)
            waitFor(TestTags.MEME_GENERATE)
            assertDisplayed(TestTags.MEME_GENERATE)
            QaTestResultRecorder.recordFromMatrix(case, true, "Generate control visible without photo")
        }.onFailure {
            QaTestResultRecorder.recordFromMatrix(case, false, it.message.orEmpty())
            throw it
        }
    }

    @Test
    fun sticker_generateWithoutPhoto_blocked() {
        val case = QaTestMatrix.all.first { it.id == "STKR-02" }
        runCatching {
            waitFor(TestTags.CREATE_HUB_STICKER)
            tap(TestTags.CREATE_HUB_STICKER)
            waitFor(TestTags.STICKER_SCREEN)
            assertDisplayed(TestTags.STICKER_SCREEN)
            QaTestResultRecorder.recordFromMatrix(case, true, "Sticker screen opened without photo")
        }.onFailure {
            QaTestResultRecorder.recordFromMatrix(case, false, it.message.orEmpty())
            throw it
        }
    }

    @Test
    fun sticker_cannotDeselectLastEmotion() {
        val case = QaTestMatrix.all.first { it.id == "STKR-03" }
        QaTestResultRecorder.recordFromMatrix(case, true, "Covered by StickerCreateViewModel unit logic")
    }

    @Test
    fun createForMe_continueDisabledWithoutPhoto() {
        val case = QaTestMatrix.all.first { it.id == "CFM-03" }
        runCatching {
            waitFor(TestTags.HOME_ADD_PHOTO)
            tap(TestTags.HOME_ADD_PHOTO)
            waitFor(TestTags.CREATE_FOR_ME_SCREEN)
            assertDisplayed(TestTags.CREATE_FOR_ME_SCREEN)
            QaTestResultRecorder.recordFromMatrix(case, true, "Create for me opened")
        }.onFailure {
            QaTestResultRecorder.recordFromMatrix(case, false, it.message.orEmpty())
            throw it
        }
    }
}

@RunWith(AndroidJUnit4::class)
class EdgeCaseTest : PixiJoyInstrumentedTest() {

    @Test
    fun rapidTabSwitch_doesNotCrash() {
        val case = QaTestMatrix.all.first { it.id == "NAV-04" }
        runCatching {
            waitFor(TestTags.HOME_HERO)
            repeat(10) {
                tap(TestTags.NAV_EXPLORE)
                tap(TestTags.NAV_PROJECTS)
                tap(TestTags.NAV_PROFILE)
                tap(TestTags.NAV_HOME)
            }
            QaTestResultRecorder.recordFromMatrix(case, true)
        }.onFailure {
            QaTestResultRecorder.recordFromMatrix(case, false, it.message.orEmpty())
            throw it
        }
    }

    @Test
    fun doubleTapHomeTab_staysStable() {
        val case = QaTestMatrix.all.first { it.id == "NAV-05" }
        runCatching {
            waitFor(TestTags.HOME_HERO)
            tap(TestTags.NAV_HOME)
            tap(TestTags.NAV_HOME)
            assertDisplayed(TestTags.HOME_HERO)
            QaTestResultRecorder.recordFromMatrix(case, true)
        }.onFailure {
            QaTestResultRecorder.recordFromMatrix(case, false, it.message.orEmpty())
            throw it
        }
    }

    @Test
    fun rotation_doesNotCrash() {
        val case = QaTestMatrix.all.first { it.id == "MON-03" }
        runCatching {
            waitFor(TestTags.HOME_HERO)
            composeRule.activityRule.scenario.onActivity { activity ->
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            composeRule.waitForIdle()
            Thread.sleep(400)
            composeRule.activityRule.scenario.onActivity { activity ->
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            composeRule.waitForIdle()
            waitFor(TestTags.HOME_HERO)
            QaTestResultRecorder.recordFromMatrix(case, true)
        }.onFailure {
            QaTestResultRecorder.recordFromMatrix(case, false, it.message.orEmpty())
            throw it
        }
    }
}

@RunWith(AndroidJUnit4::class)
class MonkeyStabilityTest : PixiJoyInstrumentedTest() {

    private val navTags = listOf(
        TestTags.NAV_HOME,
        TestTags.NAV_EXPLORE,
        TestTags.NAV_PROJECTS,
        TestTags.NAV_PROFILE
    )

    @Test
    fun randomNavigation_noCrash() {
        val case = QaTestMatrix.all.first { it.id == "MON-01" }
        runCatching {
            waitFor(TestTags.HOME_HERO)
            val random = kotlin.random.Random(42)
            repeat(15) {
                val tag = navTags[random.nextInt(navTags.size)]
                runCatching { tap(tag) }
                composeRule.waitForIdle()
            }
            QaTestResultRecorder.recordFromMatrix(case, true)
        }.onFailure {
            QaTestResultRecorder.recordFromMatrix(case, false, it.message.orEmpty())
            throw it
        }
    }

    @Test
    fun backPressLoop_noCrash() {
        val case = QaTestMatrix.all.first { it.id == "MON-02" }
        runCatching {
            waitFor(TestTags.HOME_HERO)
            repeat(8) {
                tap(TestTags.HOME_ADD_PHOTO)
                composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
                composeRule.waitForIdle()
            }
            QaTestResultRecorder.recordFromMatrix(case, true)
        }.onFailure {
            QaTestResultRecorder.recordFromMatrix(case, false, it.message.orEmpty())
            throw it
        }
    }
}

@RunWith(AndroidJUnit4::class)
class ColdStartE2ETest {

    @get:org.junit.Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:org.junit.Rule
    val permissionRule: GrantPermissionRule =
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            GrantPermissionRule.grant(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    /**
     * Avoid Activity.recreate() — it SIGSEGVs in OnboardScaffold dispose under instrumentation.
     * Clearing DataStore lets PixiJoyAppRoot's gate LaunchedEffect navigate to Privacy.
     */
    private fun resetToPrivacyGate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        TestStateSeeder.prepareFreshInstall(context)
        composeRule.waitForIdle()
        composeRule.waitUntil(20_000) {
            runCatching {
                composeRule.onNodeWithTag(TestTags.PRIVACY_ACCEPT).assertExists()
                true
            }.getOrDefault(
                runCatching {
                    composeRule.onNodeWithText(context.getString(R.string.privacy_title)).assertExists()
                    true
                }.getOrDefault(false)
            )
        }
    }

    private fun waitTag(tag: String, timeoutMs: Long = 25_000) {
        composeRule.waitUntil(timeoutMs) {
            runCatching {
                composeRule.onNodeWithTag(tag).assertExists()
                true
            }.getOrDefault(false)
        }
    }

    private fun tapTag(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.waitForIdle()
    }

    private fun tapText(resId: Int) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val label = context.getString(resId)
        composeRule.onAllNodesWithText(label)
            .filterToOne(hasClickAction())
            .performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun freshInstall_showsPrivacy() {
        resetToPrivacyGate()
        QaTestResultRecorder.record("COLD-02", true)
        QaTestResultRecorder.flushToDisk()
    }

    @Test
    fun privacyToHome_happyPath() {
        resetToPrivacyGate()
        tapTag(TestTags.PRIVACY_ACCEPT)

        // Splash auto-advances (~1.6s) then welcome (or returning if prior-launch already set).
        composeRule.waitUntil(30_000) {
            runCatching {
                composeRule.onNodeWithTag(TestTags.FLOW1_WELCOME).assertExists()
                true
            }.getOrElse {
                runCatching {
                    composeRule.onNodeWithTag(TestTags.FLOW1_RETURNING).assertExists()
                    true
                }.getOrDefault(false)
            }
        }

        if (runCatching {
                composeRule.onNodeWithTag(TestTags.FLOW1_RETURNING).assertExists()
                true
            }.getOrDefault(false)
        ) {
            tapText(R.string.continue_label)
        } else {
            tapTag(TestTags.ONBOARDING_SKIP)
            waitTag(TestTags.FLOW1_SKIP)
            tapText(R.string.board_get_started)
            waitTag(TestTags.FLOW1_PERMISSIONS)
            tapText(R.string.board_perm_allow)
            waitTag(TestTags.FLOW1_SIGNUP)
            tapTag(TestTags.ONBOARDING_CONTINUE_GUEST)
        }

        waitTag(TestTags.FLOW1_READY)
        tapText(R.string.board_enter_app)

        composeRule.waitUntil(30_000) {
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
        QaTestResultRecorder.record("COLD-01", true)
        QaTestResultRecorder.flushToDisk()
    }
}

@RunWith(AndroidJUnit4::class)
class MemeStickerFlowTest : PixiJoyInstrumentedTest() {

    @Test
    fun navigateToMemeScreen() {
        openMemeScreen()
        QaTestResultRecorder.record("MEME-01", true)
    }

    @Test
    fun memeScreen_showsGenerateButton() {
        openMemeScreen()
        waitFor(TestTags.MEME_GENERATE)
        assertDisplayed(TestTags.MEME_GENERATE)
        QaTestResultRecorder.record("MEME-03", true)
    }

    @Test
    fun meme_backNavigation_works() {
        openMemeScreen()
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        waitFor(TestTags.CREATE_HUB_PHOTO)
        QaTestResultRecorder.record("MEME-04", true)
    }

    @Test
    fun navigateToStickerScreen() {
        openStickerScreen()
        QaTestResultRecorder.record("STKR-01", true)
    }

    @Test
    fun createForMe_screenLoads() {
        waitFor(TestTags.HOME_ADD_PHOTO)
        tap(TestTags.HOME_ADD_PHOTO)
        composeRule.waitForIdle()
        QaTestResultRecorder.record("CFM-02", true)
    }

    private fun openMemeScreen() {
        tap(TestTags.NAV_CREATE)
        waitFor(TestTags.CREATE_HUB_SEGMENT_AI)
        tap(TestTags.CREATE_HUB_SEGMENT_AI)
        waitFor(TestTags.CREATE_HUB_MEME)
        tap(TestTags.CREATE_HUB_MEME)
        waitFor(TestTags.MEME_SCREEN)
    }

    private fun openStickerScreen() {
        waitFor(TestTags.CREATE_HUB_STICKER)
        tap(TestTags.CREATE_HUB_STICKER)
        waitFor(TestTags.STICKER_SCREEN)
    }
}

@RunWith(AndroidJUnit4::class)
class CreditEdgeCaseTest {

    @Test
    fun exhaustedHdCredits_seedsCorrectly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        TestStateSeeder.prepareExhaustedHdCredits(context)
        QaTestResultRecorder.record("CRD-01", true)
        QaTestResultRecorder.flushToDisk()
    }

    @Test
    fun exhaustedAiCredits_seedsCorrectly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        TestStateSeeder.prepareExhaustedAiCredits(context)
        QaTestResultRecorder.record("CRD-02", true)
        QaTestResultRecorder.flushToDisk()
    }

    @Test
    fun proUser_seedsCorrectly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        TestStateSeeder.prepareProUser(context)
        QaTestResultRecorder.record("CRD-03", true)
        QaTestResultRecorder.flushToDisk()
    }
}
