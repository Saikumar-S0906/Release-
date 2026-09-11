package com.pixijoy.ai.board

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import android.Manifest
import android.os.Build
import com.pixijoy.ai.testing.BoardFlowJourneys
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardScreenHost
import com.pixijoy.ai.ui.board.BoardUiCatalog
import com.pixijoy.ai.ui.theme.PixiJoyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Shared Compose harness: host each board screen under [BoardScreenHost] and assert root tag.
 */
internal object BoardComposeHarness {
    fun assertScreen(
        rule: androidx.compose.ui.test.junit4.ComposeContentTestRule,
        screenId: String,
        hostTag: String? = null
    ) {
        rule.waitForIdle()
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithTag(TestTags.boardScreen(screenId)).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag(TestTags.boardScreen(screenId)).assertIsDisplayed()
        if (hostTag != null) {
            val nodes = rule.onAllNodesWithTag(hostTag).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                rule.onNodeWithTag(hostTag).assertIsDisplayed()
            }
        }
    }
}

/**
 * Parameterized: every [BoardUiCatalog] screen composes without crash.
 * Full matrix — prefer batched classes in [BoardCatalogBatchComposeTest] on a single AVD.
 * Nightly / FTL: keep this class. Daily: `scripts/run_board_qa_depth.ps1`.
 */
@RunWith(Parameterized::class)
class BoardCatalogComposeTest(private val screenId: String) {

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
                BoardScreenHost(
                    screenId = screenId,
                    onBack = {},
                    onNavigate = {}
                )
            }
        }
        BoardComposeHarness.assertScreen(composeRule, screenId)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun screens(): Collection<Array<String>> =
            BoardUiCatalog.all.map { arrayOf(it.id) }
    }
}

/**
 * Parameterized: walk each flow journey (happy path) by advancing screen state.
 * Simulates end-to-end navigation at the host layer without live AI backends.
 */
@RunWith(Parameterized::class)
class BoardFlowJourneyComposeTest(private val journeyId: String) {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun happyPath_allStepsCompose() {
        val journey = BoardFlowJourneys.byId(journeyId)
        var current by mutableStateOf(journey.steps.first())
        composeRule.setContent {
            PixiJoyTheme {
                BoardScreenHost(
                    screenId = current,
                    onBack = {},
                    onNavigate = { current = it }
                )
            }
        }
        journey.steps.forEach { step ->
            current = step
            composeRule.waitForIdle()
            BoardComposeHarness.assertScreen(composeRule, step, journey.hostTag)
        }
    }

    @Test
    fun edgeScreens_composeWhenDeclared() {
        val journey = BoardFlowJourneys.byId(journeyId)
        if (journey.edgeIds.isEmpty()) return
        var current by mutableStateOf(journey.edgeIds.first())
        composeRule.setContent {
            PixiJoyTheme {
                BoardScreenHost(
                    screenId = current,
                    onBack = {},
                    onNavigate = { current = it }
                )
            }
        }
        journey.edgeIds.forEach { edge ->
            current = edge
            composeRule.waitForIdle()
            BoardComposeHarness.assertScreen(composeRule, edge)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun journeys(): Collection<Array<String>> =
            BoardFlowJourneys.all.map { arrayOf(it.id) }
    }
}

/** Host-tag smoke for create / media / C-tool entry screens. */
@RunWith(Parameterized::class)
class BoardFlowHostTagComposeTest(
    private val screenId: String,
    private val hostTag: String
) {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun entry_exposesHostTag() {
        composeRule.setContent {
            PixiJoyTheme {
                BoardScreenHost(screenId = screenId, onBack = {}, onNavigate = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.boardScreen(screenId)).assertIsDisplayed()
        composeRule.onNodeWithTag(hostTag).assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): Collection<Array<String>> = listOf(
            arrayOf("b5_choose", TestTags.FLOW5_BEAUTIFUL),
            arrayOf("b6_choose", TestTags.FLOW6_FUNNY),
            arrayOf("b7_sheet", TestTags.FLOW7_TELL),
            arrayOf("b_diff_choose", TestTags.FLOW_DIFF_HOST),
            arrayOf("b13_photo", TestTags.FLOW13_HOST),
            arrayOf("b14_input", TestTags.FLOW14_HOST),
            arrayOf("b15_open", TestTags.FLOW15_HOST),
            arrayOf("b16_select", TestTags.FLOW16_HOST),
            arrayOf("b17_open", TestTags.FLOW17_HOST),
            arrayOf("b_c04", TestTags.FLOW_C04_HOST),
            arrayOf("b_c07", TestTags.FLOW_C07_HOST),
            arrayOf("b_c08", TestTags.FLOW_C08_HOST),
            arrayOf("b_c11", TestTags.FLOW_C11_HOST),
            arrayOf("b_c14", TestTags.FLOW_C14_HOST)
        )
    }
}

/**
 * Functional click-through for rematch Beautiful: style pick → adjust (when primary CTA present).
 * Soft AI only — no generation backend.
 */
@RunWith(AndroidJUnit4::class)
class BoardFlowInteractiveComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun beautiful_entryShowsHostAndNavigatesOnPrimaryIfPresent() {
        var current by mutableStateOf("b5_choose")
        composeRule.setContent {
            PixiJoyTheme {
                BoardScreenHost(
                    screenId = current,
                    onBack = {},
                    onNavigate = { current = it }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.FLOW5_BEAUTIFUL).assertIsDisplayed()
        runCatching {
            composeRule.onNodeWithText("Continue", substring = true, ignoreCase = true).performClick()
        }.recoverCatching {
            composeRule.onNodeWithText("Next", substring = true, ignoreCase = true).performClick()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.boardScreen(current)).assertIsDisplayed()
    }

    @Test
    fun createHub_renders() {
        composeRule.setContent {
            PixiJoyTheme {
                BoardScreenHost(screenId = "b_create_hub", onBack = {}, onNavigate = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.boardScreen("b_create_hub")).assertIsDisplayed()
    }
}
