package com.pixijoy.ai.board

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardScreenHost
import com.pixijoy.ai.ui.board.components.BoardBodyText
import com.pixijoy.ai.ui.board.components.BoardCaptionText
import com.pixijoy.ai.ui.board.components.BoardTitleText
import com.pixijoy.ai.ui.board.theme.BoardThemeProvider
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.board.theme.loadBoardFontFamily
import com.pixijoy.ai.ui.theme.PixiJoyTheme
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Font / typography must never crash composition — platform sans for Compose.
 */
@RunWith(AndroidJUnit4::class)
class BoardFontSmokeComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadBoardFontFamily_neverThrows() {
        val family = loadBoardFontFamily()
        assertNotNull(family)
    }

    @Test
    fun boardTypeRoles_composeUnderThemeProvider() {
        composeRule.setContent {
            BoardThemeProvider {
                Column(modifier = Modifier.testTag("font_smoke")) {
                    BoardTitleText("Title role")
                    BoardBodyText("Body role")
                    BoardCaptionText("Caption role")
                    Text("CTA sample", style = BoardType.cta)
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("font_smoke").assertIsDisplayed()
        composeRule.onNodeWithText("Title role").assertIsDisplayed()
        composeRule.onNodeWithText("Body role").assertIsDisplayed()
        composeRule.onNodeWithText("Caption role").assertIsDisplayed()
    }

    @Test
    fun rematchAndShellEntries_composeWithBoardFonts() {
        val ids = listOf("b2_home", "b_create_hub", "b5_choose", "b8_ready", "b18_browse")
        var screenId by mutableStateOf(ids.first())
        composeRule.setContent {
            PixiJoyTheme {
                BoardThemeProvider {
                    BoardScreenHost(screenId = screenId, onBack = {}, onNavigate = {})
                }
            }
        }
        ids.forEach { id ->
            composeRule.runOnIdle { screenId = id }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(TestTags.boardScreen(id)).assertIsDisplayed()
        }
    }
}
