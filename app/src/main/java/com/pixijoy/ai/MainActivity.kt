package com.pixijoy.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pixijoy.ai.ui.PixiJoyAppRoot
import com.pixijoy.ai.ui.theme.PixiJoyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** Debug/QA: open a board screen by id via `--es board_screen <id>`. */
    private var pendingBoardScreen by mutableStateOf<String?>(null)

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        pendingBoardScreen = intent?.getStringExtra(EXTRA_BOARD_SCREEN)?.takeIf { it.isNotBlank() }
        enableEdgeToEdge()
        setContent {
            PixiJoyTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { testTagsAsResourceId = true }
                ) {
                    PixiJoyAppRoot(
                        pendingBoardScreenId = pendingBoardScreen,
                        onPendingBoardScreenConsumed = { pendingBoardScreen = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingBoardScreen = intent.getStringExtra(EXTRA_BOARD_SCREEN)?.takeIf { it.isNotBlank() }
    }

    companion object {
        const val EXTRA_BOARD_SCREEN = "board_screen"
    }
}
