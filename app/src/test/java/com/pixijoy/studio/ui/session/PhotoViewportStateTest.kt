package com.pixijoy.studio.ui.session

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.pixijoy.photo.PhotoOp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoViewportStateTest {
    private val viewport = Size(1_000f, 800f)
    private val fitted = Rect(100f, 0f, 900f, 800f)

    @Test
    fun transform_clampsZoomAndPanToImageBounds() {
        val state = PhotoViewportState().transform(
            centroid = Offset(viewport.width / 2f, viewport.height / 2f),
            panChange = Offset(20_000f, -20_000f),
            zoomChange = 20f,
            viewportSize = viewport,
            fittedImageRect = fitted
        )

        assertEquals(PhotoViewportState.MAX_ZOOM, state.zoom, 0.001f)
        assertEquals(2_700f, state.pan.x, 0.001f)
        assertEquals(-2_800f, state.pan.y, 0.001f)
    }

    @Test
    fun baselineAndReset_removePan() {
        val clamped = PhotoViewportState(
            zoom = 1f,
            pan = Offset(400f, -300f)
        ).clamped(viewport, fitted)

        assertEquals(PhotoViewportState(), clamped)
        assertEquals(PhotoViewportState(), PhotoViewportState(4f, Offset(10f, 20f)).reset())
    }

    @Test
    fun viewportChange_safelyClampsExistingPan() {
        val resizedViewport = Size(1_200f, 900f)
        val resizedFit = Rect(150f, 0f, 1_050f, 900f)
        val clamped = PhotoViewportState(
            zoom = 2f,
            pan = Offset(900f, -900f)
        ).clamped(resizedViewport, resizedFit)

        assertEquals(300f, clamped.pan.x, 0.001f)
        assertEquals(-450f, clamped.pan.y, 0.001f)
    }

    @Test
    fun normalizedCoordinates_roundTripThroughCommonTransform() {
        val state = PhotoViewportState(
            zoom = 2.5f,
            pan = Offset(120f, -70f)
        )
        val normalized = Offset(0.23f, 0.78f)

        val viewportPoint = state.normalizedToViewport(normalized, viewport, fitted)
        val result = state.viewportToNormalized(viewportPoint, viewport, fitted)

        assertEquals(normalized.x, result.x, 0.0001f)
        assertEquals(normalized.y, result.y, 0.0001f)
    }

    @Test
    fun zoom_keepsCentroidImageCoordinateStable() {
        val centroid = Offset(720f, 310f)
        val before = PhotoViewportState().viewportToNormalized(centroid, viewport, fitted)
        val zoomed = PhotoViewportState().transform(
            centroid = centroid,
            panChange = Offset.Zero,
            zoomChange = 3f,
            viewportSize = viewport,
            fittedImageRect = fitted
        )
        val after = zoomed.viewportToNormalized(centroid, viewport, fitted)

        assertEquals(before.x, after.x, 0.0001f)
        assertEquals(before.y, after.y, 0.0001f)
    }

    @Test
    fun cropHandleHitTest_usesIndependentRadiusAndRejectsDistantTouch() {
        val imageRect = Rect(100f, 50f, 900f, 650f)
        val crop = PhotoOp.Crop(0.1f, 0.2f, 0.9f, 0.8f)
        val topLeft = Offset(180f, 170f)

        assertEquals(
            CropHandle.TL,
            hitTestCropHandle(topLeft + Offset(25f, 0f), imageRect, crop, 26f)
        )
        assertNull(hitTestCropHandle(topLeft + Offset(27f, 0f), imageRect, crop, 26f))
        assertNull(hitTestCropHandle(Offset(500f, 350f), imageRect, crop, 26f))
    }
}
