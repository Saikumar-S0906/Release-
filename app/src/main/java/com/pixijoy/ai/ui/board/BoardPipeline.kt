package com.pixijoy.ai.ui.board

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Bridges board UI to production create / media / me pipelines (P7).
 * Provided from [com.pixijoy.ai.ui.PixiJoyAppRoot] and [com.pixijoy.ai.ui.MainScaffold].
 */
data class BoardPipelineCallbacks(
    val openBeautiful: () -> Unit = {},
    val openFunny: () -> Unit = {},
    val openDifferent: () -> Unit = {},
    val openMove: () -> Unit = {},
    val openTell: () -> Unit = {},
    val openStickers: () -> Unit = {},
    val openCollage: () -> Unit = {},
    val openExport: () -> Unit = {},
    val openMediaInput: () -> Unit = {},
    val openPro: () -> Unit = {},
    val openShare: () -> Unit = {},
    val openCreations: () -> Unit = {}
)

val LocalBoardPipeline = staticCompositionLocalOf { BoardPipelineCallbacks() }

/** Keep Beautiful / Funny / editor tools inside the current tab stack instead of overlaying. */
fun BoardPipelineCallbacks.inTab(navigate: (String) -> Unit) = copy(
    openBeautiful = { navigate("b5_choose") },
    openFunny = { navigate("b6_choose") },
    openDifferent = { navigate("b_diff_choose") },
    openMove = { navigate("b14_input") },
    openTell = { navigate("b7_sheet") },
    openStickers = { navigate("b13_photo") },
    openCollage = { navigate("b16_select") },
    openExport = { navigate("b17_open") },
    openMediaInput = { navigate("b3_open") },
    openPro = { navigate("b10_discover") },
    openShare = { navigate("b8_share") },
    openCreations = { navigate("b18_browse") }
)
