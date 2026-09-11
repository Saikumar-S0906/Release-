package com.pixijoy.ai.board

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardScreenHost
import com.pixijoy.ai.ui.theme.PixiJoyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Phase 7 DoD — large font + RTL smoke for rematch board hosts. */
@RunWith(AndroidJUnit4::class)
class BoardPhase7DoDComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun largeFont_b2Home_displaysBoardScreen() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.3f)
            ) {
                PixiJoyTheme {
                    BoardScreenHost(screenId = "b2_home", onBack = {}, onNavigate = {})
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.boardScreen("b2_home")).assertIsDisplayed()
    }

    @Test
    fun largeFont_b5Choose_displaysBoardScreen() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.3f)
            ) {
                PixiJoyTheme {
                    BoardScreenHost(screenId = "b5_choose", onBack = {}, onNavigate = {})
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.boardScreen("b5_choose")).assertIsDisplayed()
    }

    @Test
    fun rtl_b2Home_displaysBoardScreen() {
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                PixiJoyTheme {
                    BoardScreenHost(screenId = "b2_home", onBack = {}, onNavigate = {})
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.boardScreen("b2_home")).assertIsDisplayed()
    }
}
