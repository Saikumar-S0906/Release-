package com.pixijoy.ai.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.photo.PhotoOp

internal enum class CropHandle { TL, TR, BL, BR }

internal enum class CropBound { Left, Top, Right, Bottom }

/** Normalized distance at which a crop edge sticks to the photo bound. */
internal const val CropEdgeSnap = 0.02f

internal const val CropMinSpan = 0.15f

@Composable
fun CropOverlay(
    crop: PhotoOp.Crop,
    imageRect: Rect,
    onChange: (PhotoOp.Crop) -> Unit,
    modifier: Modifier = Modifier,
    viewportZoom: Float = 1f
) {
    var handle by remember { mutableStateOf<CropHandle?>(null) }
    var heldBounds by remember { mutableStateOf(cropBoundEdges(crop)) }
    val latestCrop by rememberUpdatedState(crop)
    val haptics = LocalHapticFeedback.current
    val safeZoom = viewportZoom.coerceAtLeast(1f)
    Canvas(
        modifier = modifier
            .testTag("session_crop_overlay")
            .pointerInput(imageRect, safeZoom) {
                detectDragGestures(
                    onDragStart = { start ->
                        val current = latestCrop
                        handle = hitTestCropHandle(
                            start = start,
                            imageRect = imageRect,
                            crop = current,
                            hitRadiusPx = 26.dp.toPx() / safeZoom
                        )
                        heldBounds = cropBoundEdges(current)
                    },
                    onDragEnd = { handle = null },
                    onDragCancel = { handle = null },
                    onDrag = { change, amount ->
                        val active = handle ?: return@detectDragGestures
                        if (imageRect.width <= 0f || imageRect.height <= 0f) return@detectDragGestures
                        change.consume()
                        val next = dragCropHandle(
                            latestCrop,
                            active,
                            amount.x / imageRect.width,
                            amount.y / imageRect.height
                        )
                        val bounds = cropBoundEdges(next)
                        if ((bounds - heldBounds).isNotEmpty()) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        heldBounds = bounds
                        onChange(next)
                    }
                )
            }
    ) {
        val l = imageRect.left + crop.left * imageRect.width
        val t = imageRect.top + crop.top * imageRect.height
        val r = imageRect.left + crop.right * imageRect.width
        val b = imageRect.top + crop.bottom * imageRect.height
        drawRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = imageRect.topLeft,
            size = Size(imageRect.width, (t - imageRect.top).coerceAtLeast(0f))
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(imageRect.left, b),
            size = Size(imageRect.width, (imageRect.bottom - b).coerceAtLeast(0f))
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(imageRect.left, t),
            size = Size((l - imageRect.left).coerceAtLeast(0f), (b - t).coerceAtLeast(0f))
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(r, t),
            size = Size((imageRect.right - r).coerceAtLeast(0f), (b - t).coerceAtLeast(0f))
        )
        val stroke = 2.dp.toPx() / safeZoom
        drawRect(
            color = Color.White,
            topLeft = Offset(l, t),
            size = Size(r - l, b - t),
            style = Stroke(width = stroke)
        )
        val bounds = cropBoundEdges(crop)
        if (CropBound.Left in bounds) {
            drawLine(PixiAccent, Offset(l, t), Offset(l, b), strokeWidth = stroke)
        }
        if (CropBound.Right in bounds) {
            drawLine(PixiAccent, Offset(r, t), Offset(r, b), strokeWidth = stroke)
        }
        if (CropBound.Top in bounds) {
            drawLine(PixiAccent, Offset(l, t), Offset(r, t), strokeWidth = stroke)
        }
        if (CropBound.Bottom in bounds) {
            drawLine(PixiAccent, Offset(l, b), Offset(r, b), strokeWidth = stroke)
        }
        // The common viewport layer scales positions; compensate radii so handles remain
        // 10dp visually and retain a separate 26dp touch radius at every zoom.
        val radius = 10.dp.toPx() / safeZoom
        listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b)).forEach { center ->
            drawCircle(Color.White, radius, center)
        }
    }
}

internal fun fittedImageRect(viewW: Float, viewH: Float, imgW: Int, imgH: Int): Rect {
    if (viewW <= 0f || viewH <= 0f || imgW <= 0 || imgH <= 0) {
        return Rect(0f, 0f, viewW.coerceAtLeast(1f), viewH.coerceAtLeast(1f))
    }
    val viewAspect = viewW / viewH
    val imgAspect = imgW.toFloat() / imgH.toFloat()
    return if (imgAspect > viewAspect) {
        val h = viewW / imgAspect
        val top = (viewH - h) / 2f
        Rect(0f, top, viewW, top + h)
    } else {
        val w = viewH * imgAspect
        val left = (viewW - w) / 2f
        Rect(left, 0f, left + w, viewH)
    }
}

internal fun hitTestCropHandle(
    start: Offset,
    imageRect: Rect,
    crop: PhotoOp.Crop,
    hitRadiusPx: Float
): CropHandle? {
    if (imageRect.width <= 0f || imageRect.height <= 0f || hitRadiusPx <= 0f) return null
    val points = mapOf(
        CropHandle.TL to Offset(
            imageRect.left + crop.left * imageRect.width,
            imageRect.top + crop.top * imageRect.height
        ),
        CropHandle.TR to Offset(
            imageRect.left + crop.right * imageRect.width,
            imageRect.top + crop.top * imageRect.height
        ),
        CropHandle.BL to Offset(
            imageRect.left + crop.left * imageRect.width,
            imageRect.top + crop.bottom * imageRect.height
        ),
        CropHandle.BR to Offset(
            imageRect.left + crop.right * imageRect.width,
            imageRect.top + crop.bottom * imageRect.height
        )
    )
    val radiusSquared = hitRadiusPx * hitRadiusPx
    return points
        .asSequence()
        .map { (handle, point) ->
            val delta = point - start
            handle to delta.getDistanceSquared()
        }
        .filter { (_, distanceSquared) -> distanceSquared <= radiusSquared }
        .minByOrNull { (_, distanceSquared) -> distanceSquared }
        ?.first
}

internal fun dragCropHandle(crop: PhotoOp.Crop, handle: CropHandle, dx: Float, dy: Float): PhotoOp.Crop {
    var l = crop.left
    var t = crop.top
    var r = crop.right
    var b = crop.bottom
    when (handle) {
        CropHandle.TL -> {
            l = snapCropEdge(l + dx, low = 0f, high = r - CropMinSpan)
            t = snapCropEdge(t + dy, low = 0f, high = b - CropMinSpan)
        }
        CropHandle.TR -> {
            r = snapCropEdge(r + dx, low = l + CropMinSpan, high = 1f)
            t = snapCropEdge(t + dy, low = 0f, high = b - CropMinSpan)
        }
        CropHandle.BL -> {
            l = snapCropEdge(l + dx, low = 0f, high = r - CropMinSpan)
            b = snapCropEdge(b + dy, low = t + CropMinSpan, high = 1f)
        }
        CropHandle.BR -> {
            r = snapCropEdge(r + dx, low = l + CropMinSpan, high = 1f)
            b = snapCropEdge(b + dy, low = t + CropMinSpan, high = 1f)
        }
    }
    return crop.copy(left = l, top = t, right = r, bottom = b)
}

/**
 * Pulls an edge onto the photo bound when it is within [CropEdgeSnap] and that
 * bound is still legal. The minimum-span wall is not a snap target.
 */
internal fun snapCropEdge(raw: Float, low: Float, high: Float): Float {
    val clamped = raw.coerceIn(low, high)
    if (low == 0f && clamped <= CropEdgeSnap) return 0f
    if (high == 1f && clamped >= 1f - CropEdgeSnap) return 1f
    return clamped
}

internal fun cropBoundEdges(crop: PhotoOp.Crop): Set<CropBound> = buildSet {
    if (crop.left == 0f) add(CropBound.Left)
    if (crop.top == 0f) add(CropBound.Top)
    if (crop.right == 1f) add(CropBound.Right)
    if (crop.bottom == 1f) add(CropBound.Bottom)
}
