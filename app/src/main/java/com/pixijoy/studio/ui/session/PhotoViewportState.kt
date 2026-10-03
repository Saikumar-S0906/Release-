package com.pixijoy.studio.ui.session

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.max

/**
 * Immutable viewport model for a fitted photo.
 *
 * [pan] is expressed in viewport pixels after scaling. The fitted image stays centered on
 * axes where it is still smaller than the viewport.
 */
internal data class PhotoViewportState(
    val zoom: Float = MIN_ZOOM,
    val pan: Offset = Offset.Zero
) {
    fun reset(): PhotoViewportState = PhotoViewportState()

    fun transform(
        centroid: Offset,
        panChange: Offset,
        zoomChange: Float,
        viewportSize: Size,
        fittedImageRect: Rect
    ): PhotoViewportState {
        if (!viewportSize.isUsable() || !fittedImageRect.isUsable()) return reset()

        val oldZoom = zoom.finiteOr(MIN_ZOOM).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val requestedZoom = oldZoom * zoomChange.finiteOr(1f)
        val newZoom = requestedZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val center = viewportSize.centerOffset()
        val safeCentroid = centroid.finiteOr(center)
        val safePan = pan.finiteOr(Offset.Zero)
        val scaleChange = newZoom / oldZoom

        // Keep the image point beneath the gesture centroid stationary while scaling.
        val anchoredPan = safePan + (safeCentroid - center - safePan) * (1f - scaleChange)
        return copy(
            zoom = newZoom,
            pan = anchoredPan + panChange.finiteOr(Offset.Zero)
        ).clamped(viewportSize, fittedImageRect)
    }

    fun clamped(viewportSize: Size, fittedImageRect: Rect): PhotoViewportState {
        if (!viewportSize.isUsable() || !fittedImageRect.isUsable()) return reset()
        val safeZoom = zoom.finiteOr(MIN_ZOOM).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val maxPanX = max(0f, (fittedImageRect.width * safeZoom - viewportSize.width) / 2f)
        val maxPanY = max(0f, (fittedImageRect.height * safeZoom - viewportSize.height) / 2f)
        val safePan = pan.finiteOr(Offset.Zero)
        return PhotoViewportState(
            zoom = safeZoom,
            pan = Offset(
                if (maxPanX == 0f) 0f else safePan.x.coerceIn(-maxPanX, maxPanX),
                if (maxPanY == 0f) 0f else safePan.y.coerceIn(-maxPanY, maxPanY)
            )
        )
    }

    fun viewportToNormalized(
        point: Offset,
        viewportSize: Size,
        fittedImageRect: Rect
    ): Offset {
        if (!viewportSize.isUsable() || !fittedImageRect.isUsable()) return Offset.Zero
        val safe = clamped(viewportSize, fittedImageRect)
        val center = viewportSize.centerOffset()
        val transformedDelta = point - center - safe.pan
        val untransformed = center + Offset(
            transformedDelta.x / safe.zoom,
            transformedDelta.y / safe.zoom
        )
        return Offset(
            (untransformed.x - fittedImageRect.left) / fittedImageRect.width,
            (untransformed.y - fittedImageRect.top) / fittedImageRect.height
        )
    }

    fun normalizedToViewport(
        point: Offset,
        viewportSize: Size,
        fittedImageRect: Rect
    ): Offset {
        if (!viewportSize.isUsable() || !fittedImageRect.isUsable()) return Offset.Zero
        val safe = clamped(viewportSize, fittedImageRect)
        val center = viewportSize.centerOffset()
        val imagePoint = Offset(
            fittedImageRect.left + point.x * fittedImageRect.width,
            fittedImageRect.top + point.y * fittedImageRect.height
        )
        return center + (imagePoint - center) * safe.zoom + safe.pan
    }

    companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 8f
    }
}

private fun Size.isUsable(): Boolean =
    width.isFinite() && height.isFinite() && width > 0f && height > 0f

private fun Size.centerOffset(): Offset = Offset(width / 2f, height / 2f)

private fun Rect.isUsable(): Boolean =
    width.isFinite() && height.isFinite() && width > 0f && height > 0f

private fun Float.finiteOr(fallback: Float): Float = if (isFinite()) this else fallback

private fun Offset.finiteOr(fallback: Offset): Offset =
    if (x.isFinite() && y.isFinite()) this else fallback
