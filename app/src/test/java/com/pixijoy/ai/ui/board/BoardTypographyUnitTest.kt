package com.pixijoy.ai.ui.board

import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.board.theme.BoardTypography
import org.junit.Assert.assertEquals
import org.junit.Test

/** JVM smoke for board typography tokens (Design Bible §7 scale). */
class BoardTypographyUnitTest {

    @Test
    fun boardType_bibleHierarchySizes() {
        assertEquals(32, BoardType.hero.fontSize.value.toInt())
        assertEquals(28, BoardType.largeHeadline.fontSize.value.toInt())
        assertEquals(24, BoardType.headline.fontSize.value.toInt())
        assertEquals(20, BoardType.title.fontSize.value.toInt())
        assertEquals(16, BoardType.body.fontSize.value.toInt())
        assertEquals(14, BoardType.secondary.fontSize.value.toInt())
        assertEquals(12, BoardType.caption.fontSize.value.toInt())
        assertEquals(14, BoardType.cta.fontSize.value.toInt())
    }

    @Test
    fun boardTypography_mapsLockedRoles() {
        assertEquals(BoardType.hero.fontSize, BoardTypography.displayLarge.fontSize)
        assertEquals(BoardType.title.fontSize, BoardTypography.headlineSmall.fontSize)
        assertEquals(BoardType.body.fontSize, BoardTypography.bodyLarge.fontSize)
        assertEquals(BoardType.secondary.fontSize, BoardTypography.bodyMedium.fontSize)
        assertEquals(BoardType.caption.fontSize, BoardTypography.labelMedium.fontSize)
        assertEquals(BoardType.cta.fontSize, BoardTypography.labelLarge.fontSize)
    }
}
