package com.pixijoy.ai

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardScreenHost
import com.pixijoy.ai.ui.theme.PixiJoyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.io.FileOutputStream

/**
 * Renders each Scope-A board entry and writes a PNG under the app's external files dir:
 * `/sdcard/Android/data/com.pixijoy.ai/files/board_review/<id>.png`
 *
 * Pull with:
 * `adb pull /sdcard/Android/data/com.pixijoy.ai/files/board_review docs/board_review/phone`
 */
@RunWith(Parameterized::class)
class BoardScreenshotCaptureTest(private val screenId: String) {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureEntry() {
        composeRule.setContent {
            PixiJoyTheme {
                BoardScreenHost(screenId = screenId, onBack = {}, onNavigate = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.boardScreen(screenId)).assertExists()
        composeRule.waitForIdle()

        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(context.filesDir, "board_review")
        dir.mkdirs()
        val out = File(dir, "$screenId.png")
        FileOutputStream(out).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        check(out.exists() && out.length() > 1000) { "Screenshot not written: ${out.absolutePath}" }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun ids(): Collection<Array<String>> = listOf(
            "b1_splash", "b1_welcome", "b1_ready",
            "b2_home", "b2_discover",
            "b_c00_empty",
            "b3_chooser", "b4_result",
            "b_create_hub",
            "b5_choose", "b6_choose", "b7_sheet", "b_diff_choose",
            "b8_ready", "b9_open", "b10_discover", "b11_home",
            "b12_intro",
            "b13_photo", "b14_input", "b15_open", "b16_select", "b17_open",
            "b18_browse", "b19_start", "b26_open",
            "b20_input", "b21_hub", "b22_welcome", "b23_dash", "b24_plans", "b25_center",
            "b27_feed", "b28_banner", "b29_home", "b30_home",
            "b_c04", "b_c05", "b_c06", "b_c07", "b_c08", "b_c09", "b_c10", "b_c11", "b_c12", "b_c14",
            "b_collab_select"
        ).map { arrayOf(it) }
    }
}
