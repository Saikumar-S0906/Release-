package com.pixijoy.ai.board

import android.Manifest
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.pixijoy.ai.testing.BoardCatalogBatches
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardScreenHost
import com.pixijoy.ai.ui.theme.PixiJoyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Batched catalog Compose smoke — prefer these over full [BoardCatalogComposeTest]
 * on a single AVD. Run via `scripts/run_board_qa_depth.ps1 -Batch <id>`.
 */
abstract class BoardCatalogBatchBase(private val screenId: String) {
    private val composeRule = createComposeRule()
    private val permissions = if (Build.VERSION.SDK_INT >= 33) {
        GrantPermissionRule.grant(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.CAMERA
        )
    } else {
        GrantPermissionRule.grant(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(permissions).around(composeRule)

    @Test
    fun catalogScreen_rendersWithoutCrash() {
        composeRule.setContent {
            PixiJoyTheme {
                BoardScreenHost(screenId = screenId, onBack = {}, onNavigate = {})
            }
        }
        BoardComposeHarness.assertScreen(composeRule, screenId)
    }
}

@RunWith(Parameterized::class)
class BoardCatalogBatchCoreComposeTest(screenId: String) : BoardCatalogBatchBase(screenId) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun screens(): Collection<Array<String>> =
            BoardCatalogBatches.screenIds("core").map { arrayOf(it) }
    }
}

@RunWith(Parameterized::class)
class BoardCatalogBatchCreateComposeTest(screenId: String) : BoardCatalogBatchBase(screenId) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun screens(): Collection<Array<String>> =
            BoardCatalogBatches.screenIds("create").map { arrayOf(it) }
    }
}

@RunWith(Parameterized::class)
class BoardCatalogBatchCToolsComposeTest(screenId: String) : BoardCatalogBatchBase(screenId) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun screens(): Collection<Array<String>> =
            BoardCatalogBatches.screenIds("ctools").map { arrayOf(it) }
    }
}

@RunWith(Parameterized::class)
class BoardCatalogBatchMediaComposeTest(screenId: String) : BoardCatalogBatchBase(screenId) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun screens(): Collection<Array<String>> =
            BoardCatalogBatches.screenIds("media").map { arrayOf(it) }
    }
}

@RunWith(Parameterized::class)
class BoardCatalogBatchLibraryComposeTest(screenId: String) : BoardCatalogBatchBase(screenId) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun screens(): Collection<Array<String>> =
            BoardCatalogBatches.screenIds("library").map { arrayOf(it) }
    }
}

@RunWith(Parameterized::class)
class BoardCatalogBatchMeComposeTest(screenId: String) : BoardCatalogBatchBase(screenId) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun screens(): Collection<Array<String>> =
            BoardCatalogBatches.screenIds("me").map { arrayOf(it) }
    }
}

@RunWith(Parameterized::class)
class BoardCatalogBatchSocialComposeTest(screenId: String) : BoardCatalogBatchBase(screenId) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun screens(): Collection<Array<String>> =
            BoardCatalogBatches.screenIds("social").map { arrayOf(it) }
    }
}

/**
 * Day-in-life: Home → Create hub → Beautiful rematch → Share → My Stuff.
 * Soft AI host walk — no live generative backends.
 */
@RunWith(AndroidJUnit4::class)
class BoardDayInLifeComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dayInLife_allStepsCompose() {
        val steps = BoardCatalogBatches.dayInLifeSteps
        var current by mutableStateOf(steps.first())
        composeRule.setContent {
            PixiJoyTheme {
                BoardScreenHost(
                    screenId = current,
                    onBack = {},
                    onNavigate = { current = it }
                )
            }
        }
        steps.forEach { step ->
            current = step
            composeRule.waitForIdle()
            BoardComposeHarness.assertScreen(composeRule, step)
        }
        composeRule.onNodeWithTag(TestTags.boardScreen("b18_browse")).assertIsDisplayed()
    }
}

/**
 * Golden-key rematch / create entries — compose smoke for screenshot review packs.
 * Capture PNGs with `scripts/capture_board_phone.ps1 -GoldenOnly`.
 */
@RunWith(Parameterized::class)
class BoardGoldenKeyComposeTest(private val screenId: String) {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun goldenKey_renders() {
        composeRule.setContent {
            PixiJoyTheme {
                BoardScreenHost(screenId = screenId, onBack = {}, onNavigate = {})
            }
        }
        BoardComposeHarness.assertScreen(composeRule, screenId)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun screens(): Collection<Array<String>> =
            BoardCatalogBatches.goldenKeyScreenIds.map { arrayOf(it) }
    }
}
