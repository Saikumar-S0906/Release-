package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StillPictureTest {
    @Test
    fun kenBurns_matchesScaleAndPan() {
        val width = 1280f
        for (talking in listOf(false, true)) {
            val motion = MovieMotion.kenBurns(talking)
            for (t in listOf(0f, 0.5f, 1f)) {
                val sample = motion.sample(t)
                assertEquals(MovieMotion.scale(t, talking), sample.scale, 0.011f)
                assertEquals(MovieMotion.panNdc(t, talking), sample.tx, 0.011f)
                assertEquals(MovieMotion.pan(t, talking, width), sample.tx / 2f * width, 0.6f)
            }
        }
        assertFalse(MovieMotion.kenBurns(false).isIdentity)
        val mid = MovieMotion.kenBurns(false).sample(0.5f)
        assertEquals(MovieMotion.scale(0.5f, false), mid.scale, 0.001f)
        assertEquals(1.09f, mid.scale, 0.001f)
    }

    @Test
    fun plainStill_staysOnEncoder_gradedStill_getsTheSameMove() {
        val clip = TimelineClip("c", 0, 0, 4000)
        val plain = MultiTrackTimeline.fromFlatTimeline(Timeline(clips = listOf(clip)))
        assertFalse(StillPicture.needsComposition(plain))
        assertNull(StillPicture.forExport(plain, talking = false) { true })

        val graded = plain.copy(grade = TimelineGrade(exposure = 0.5f))
        val exported = StillPicture.forExport(graded, talking = false) { true }
        assertEquals(
            MovieMotion.kenBurns(false).encode(),
            exported!!.composedVideoClips().single().motion.encode()
        )

        val titled = plain.copy(titles = listOf(TimelineTitle.titleDefault(4000)))
        val withTitle = StillPicture.forExport(titled, talking = true) { true }
        assertEquals(
            MovieMotion.kenBurns(true).encode(),
            withTitle!!.composedVideoClips().single().motion.encode()
        )
    }

    @Test
    fun userMove_isKept_andVideoClip_isNotGivenStillMove() {
        val custom = TimelineClip(
            "c", 0, 0, 4000,
            motion = ClipMotionPreset.ZoomIn.toMotion()
        )
        val mt = MultiTrackTimeline.fromFlatTimeline(
            Timeline(clips = listOf(custom), grade = TimelineGrade(exposure = 0.4f))
        )
        val exported = StillPicture.forExport(mt, talking = false) { true }
        assertTrue(exported!!.composedVideoClips().single().motion.matchesPreset(ClipMotionPreset.ZoomIn))

        val still = TimelineClip("c", 0, 0, 4000)
        val video = MultiTrackTimeline.fromFlatTimeline(
            Timeline(clips = listOf(still), grade = TimelineGrade(contrast = 1.2f))
        )
        val untouched = StillPicture.forExport(video, talking = false) { false }
        assertTrue(untouched!!.composedVideoClips().single().motion.isIdentity)

        val grid = MultiTrackTimeline.fromFlatTimeline(Timeline(clips = listOf(still)))
            .copy(layout = VideoLayout.Grid)
        val gridExport = StillPicture.forExport(grid, talking = false) { true }
        assertTrue(gridExport!!.composedVideoClips().single().motion.isIdentity)
        assertTrue(gridExport.layout.isMulti())
    }

    @Test
    fun titleBaseline_matchesExportAnchors() {
        val title = TimelineTitle.titleDefault(4000)
        val (tx, ty) = TitleOverlays.baselineFraction(title)
        assertEquals(0.5f, tx, 0.001f)
        assertEquals(0.14f, ty, 0.001f)
        val caption = TimelineTitle.captionDefault(4000)
        val (_, cy) = TitleOverlays.baselineFraction(caption)
        assertEquals(0.86f, cy, 0.001f)
        assertEquals(72f, TitleOverlays.textSizePx(TitleRole.Title), 0.01f)
        assertEquals(54f, TitleOverlays.textSizePx(TitleRole.Caption), 0.01f)
    }
}
