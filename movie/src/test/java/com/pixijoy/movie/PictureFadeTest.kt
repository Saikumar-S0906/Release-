package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Test

class PictureFadeTest {
    @Test
    fun softCut_fadesPictureOutThenIn() {
        val clips = listOf(
            TimelineClip("a", 0, 0, 3000, transition = ClipTransition.Soft, transitionMs = 400),
            TimelineClip("b", 0, 0, 3000)
        )
        val windows = PictureFade.windows(clips)
        assertEquals(0L to 400L, windows[0])
        assertEquals(400L to 0L, windows[1])

        val durationUs = 3_000_000L
        val fadeUs = 400_000L
        assertEquals(0f, PictureFade.blackAmount(0L, durationUs, 0L, fadeUs), 0.001f)
        assertEquals(0.5f, PictureFade.blackAmount(2_800_000L, durationUs, 0L, fadeUs), 0.001f)
        assertEquals(1f, PictureFade.blackAmount(durationUs, durationUs, 0L, fadeUs), 0.001f)
        assertEquals(1f, PictureFade.blackAmount(0L, durationUs, fadeUs, 0L), 0.001f)
        assertEquals(0.5f, PictureFade.blackAmount(200_000L, durationUs, fadeUs, 0L), 0.001f)
        assertEquals(0f, PictureFade.blackAmount(400_000L, durationUs, fadeUs, 0L), 0.001f)
    }

    @Test
    fun cut_doesNotFadePicture() {
        val clips = listOf(
            TimelineClip("a", 0, 0, 2000),
            TimelineClip("b", 0, 0, 2000)
        )
        val windows = PictureFade.windows(clips)
        assertEquals(0L to 0L, windows[0])
        assertEquals(0L to 0L, windows[1])
        assertEquals(0f, PictureFade.blackAmount(1_000_000L, 2_000_000L, 0L, 0L), 0.001f)
    }
}
