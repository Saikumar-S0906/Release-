package com.pixijoy.ai.board

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardScreenHost
import com.pixijoy.ai.ui.theme.PixiJoyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Focused regression for screens that previously broke the full catalog run. */
@RunWith(AndroidJUnit4::class)
class BoardCatalogRegressionComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun b3_gallery_rendersWithoutPermissionDialog() {
        composeRule.setContent {
            PixiJoyTheme {
                BoardScreenHost(screenId = "b3_gallery", onBack = {}, onNavigate = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.boardScreen("b3_gallery")).assertIsDisplayed()
    }

    @Test
    fun b14_gen_rendersProcessing() {
        composeRule.setContent {
            PixiJoyTheme {
                BoardScreenHost(screenId = "b14_gen", onBack = {}, onNavigate = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.boardScreen("b14_gen")).assertIsDisplayed()
    }
}
