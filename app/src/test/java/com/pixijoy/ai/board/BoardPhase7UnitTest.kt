package com.pixijoy.ai.board

import com.pixijoy.ai.BuildConfig
import com.pixijoy.ai.ui.board.BoardFeatureFlags
import com.pixijoy.ai.ui.board.BoardSoftGenerate
import com.pixijoy.ai.ui.board.components.BoardCdn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 7 — feature flags, CDN stub, soft generate contracts (JVM-safe). */
class BoardPhase7UnitTest {

    @Test
    fun showDemoCaption_trueWhenLiveNotConfigured() {
        assertTrue(
            "Default builds keep soft Demo captions until live generate is configured",
            BoardFeatureFlags.showDemoCaption == !BoardFeatureFlags.liveGenerateConfigured
        )
        if (!BoardFeatureFlags.liveGenerateConfigured) {
            assertTrue(BoardFeatureFlags.showDemoCaption)
        }
    }

    @Test
    fun boardCdn_urlOrNull_discoverFeaturedWhenEnabled() {
        if (BuildConfig.BOARD_CDN_ENABLED) {
            assertNotNull(BoardCdn.urlOrNull(BoardCdn.DISCOVER_FEATURED))
        } else {
            assertTrue(BoardCdn.urlOrNull(BoardCdn.DISCOVER_FEATURED) == null)
        }
    }

    @Test
    fun boardSoftGenerate_intents_coverBeautifulAndFunny() {
        assertEquals(
            setOf(BoardSoftGenerate.Intent.Beautiful, BoardSoftGenerate.Intent.Funny),
            BoardSoftGenerate.Intent.entries.toSet()
        )
    }

    @Test
    fun boardSoftGenerate_result_softFallbackContract() {
        val soft = BoardSoftGenerate.Result(
            artRes = 0,
            usedLiveAttempt = false,
            softFallback = true,
            isCloudPowered = false
        )
        assertTrue(soft.softFallback)
        assertFalse(soft.isCloudPowered)
        assertFalse(soft.usedLiveAttempt)

        val live = soft.copy(
            usedLiveAttempt = true,
            softFallback = false,
            isCloudPowered = true,
            sceneDescription = "portrait"
        )
        assertTrue(live.isCloudPowered)
        assertFalse(live.softFallback)
        assertEquals("portrait", live.sceneDescription)
    }

    @Test
    fun liveGenerateConfigured_impliesDemoCaptionOff() {
        if (BoardFeatureFlags.liveGenerateConfigured) {
            assertFalse(BoardFeatureFlags.showDemoCaption)
        }
    }
}
