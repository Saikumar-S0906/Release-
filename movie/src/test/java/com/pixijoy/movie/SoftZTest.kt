package com.pixijoy.movie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftZTest {
    @Test
    fun plane_mapsInFrontToNearAndBehindToFar() {
        assertEquals(0f, cgPlaneDepth(8), 0.0001f)
        assertEquals(0.5f, cgPlaneDepth(0), 0.0001f)
        assertEquals(1f, cgPlaneDepth(-8), 0.0001f)
        assertEquals(0f, cgPlaneDepth(99), 0.0001f)
    }

    @Test
    fun nearerScene_hidesPlate_fartherScene_keepsIt() {
        assertFalse(softZKeepsPlate(sceneDepth = 0.2f, planeDepth = 0.5f))
        assertTrue(softZKeepsPlate(sceneDepth = 0.8f, planeDepth = 0.5f))
        assertTrue(softZKeepsPlate(sceneDepth = 0.5f, planeDepth = 0.5f))
    }

    @Test
    fun occlude_clearsAlphaOnlyWhereSceneIsNearer() {
        val opaque = (0xff shl 24) or (10 shl 16) or (20 shl 8) or 30
        val clear = 10 shl 16 or (20 shl 8) or 30
        val pixels = intArrayOf(opaque, opaque, clear)
        // Plane 0.5. Pixel 0 sits on a near subject; pixel 1 is on the far background.
        occludePlateByDepth(
            pixels,
            width = 3,
            height = 1,
            depth = floatArrayOf(0.1f, 0.9f, 0.9f),
            depthW = 3,
            depthH = 1,
            planeDepth = 0.5f
        )
        assertEquals(opaque and 0x00ffffff, pixels[0])
        assertEquals(opaque, pixels[1])
        assertEquals(clear, pixels[2])
    }

    @Test
    fun depthFrames_missesUntilPublished_holdsForward_rejectsStalePast() {
        DepthFrames.resetForTests()
        assertNull(DepthFrames.at(0L))
        val map = floatArrayOf(0.25f)
        assertEquals(0, DepthFrames.generation())
        DepthFrames.publish(1_000_000L, map)
        assertEquals(1, DepthFrames.generation())
        assertTrue(DepthFrames.at(1_200_000L) === map)
        assertTrue(DepthFrames.at(1_000_000L + DepthFrames.HOLD_US) === map)
        assertNull(DepthFrames.at(1_000_000L + DepthFrames.HOLD_US + 1))
        assertTrue(DepthFrames.at(1_000_000L - DepthFrames.SLACK_US) === map)
        assertNull(DepthFrames.at(1_000_000L - DepthFrames.SLACK_US - 1))
        DepthFrames.resetForTests()
        assertNull(DepthFrames.at(1_000_000L))
    }

    @Test
    fun depthGate_keepsNewestFrameWhileBusy_thenRunsIt() {
        val idle = DepthGate()
        val (running, first) = idle.offer(0L)
        assertEquals(DepthOffer.Start, first)
        assertTrue(running.busy)
        assertEquals(0L, running.inFlightUs)

        val (sameBucketWhileBusy, skipped) = running.offer(50_000L)
        assertEquals(DepthOffer.Skip, skipped)
        assertEquals(0L, sameBucketWhileBusy.inFlightUs)

        val (queued, second) = running.offer(1_500_000L)
        assertEquals(DepthOffer.Queue, second)
        val (replaced, newer) = queued.offer(1_700_000L)
        assertEquals(DepthOffer.Queue, newer)
        assertEquals(1_700_000L, replaced.pendingUs)
        val (stillQueued, sameBucket) = replaced.offer(1_750_000L)
        assertEquals(DepthOffer.Skip, sameBucket)
        assertEquals(1_700_000L, stillQueued.pendingUs)

        val (after, next) = stillQueued.finished(published = true, timeUs = 0L)
        assertEquals(1_700_000L, next)
        assertTrue(after.busy)
        assertEquals(1_700_000L, after.inFlightUs)
        val (done, none) = after.finished(published = true, timeUs = 1_700_000L)
        assertNull(none)
        assertFalse(done.busy)
        assertNull(done.inFlightUs)
        val (publishedBucket, offer) = done.offer(1_750_000L)
        assertEquals(DepthOffer.Skip, offer)
        assertFalse(publishedBucket.busy)
    }
}
