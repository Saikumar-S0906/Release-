package com.pixijoy.ai.ui.session

import com.pixijoy.photo.PhotoOp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CropSnapTest {
    private val inset = PhotoOp.Crop(0.08f, 0.08f, 0.92f, 0.92f)

    @Test
    fun leftEdge_snapsToPhotoBoundInsideThreshold() {
        val next = dragCropHandle(inset, CropHandle.TL, dx = -0.07f, dy = 0f)
        assertEquals(0f, next.left, 0f)
        assertEquals(0.08f, next.top, 0.0001f)
        assertTrue(CropBound.Left in cropBoundEdges(next))
    }

    @Test
    fun leftEdge_staysFreeOutsideThreshold() {
        val next = dragCropHandle(inset, CropHandle.TL, dx = -0.04f, dy = 0f)
        assertEquals(0.04f, next.left, 0.0001f)
        assertFalse(CropBound.Left in cropBoundEdges(next))
    }

    @Test
    fun rightAndBottom_snapToFarBounds() {
        val next = dragCropHandle(inset, CropHandle.BR, dx = 0.07f, dy = 0.07f)
        assertEquals(1f, next.right, 0f)
        assertEquals(1f, next.bottom, 0f)
        assertEquals(setOf(CropBound.Right, CropBound.Bottom), cropBoundEdges(next))
    }

    @Test
    fun snap_doesNotCollapseBelowMinSpan() {
        val tight = PhotoOp.Crop(left = 0f, top = 0.1f, right = 0.16f, bottom = 0.9f)
        val next = dragCropHandle(tight, CropHandle.TR, dx = -0.2f, dy = 0f)
        assertEquals(CropMinSpan, next.right, 0.0001f)
        assertEquals(0f, next.left, 0f)
        assertFalse(CropBound.Right in cropBoundEdges(next))
    }

    @Test
    fun release_leavesTheBoundWhenDraggedClear() {
        val fullLeft = PhotoOp.Crop(0f, 0.1f, 0.8f, 0.9f)
        val next = dragCropHandle(fullLeft, CropHandle.TL, dx = 0.05f, dy = 0f)
        assertEquals(0.05f, next.left, 0.0001f)
        assertTrue(cropBoundEdges(next).isEmpty())
    }
}
