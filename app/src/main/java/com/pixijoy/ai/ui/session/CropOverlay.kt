package com.pixijoy.ai.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.photo.PhotoOp
import kotlin.math.roundToInt

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
    val edgeLeft = stringResource(R.string.a11y_crop_edge_left)
    val edgeRight = stringResource(R.string.a11y_crop_edge_right)
    val edgeTop = stringResource(R.string.a11y_crop_edge_top)
    val edgeBottom = stringResource(R.string.a11y_crop_edge_bottom)
    Box(modifier.testTag("session_crop_overlay")) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
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
        val stroke = 1.5.dp.toPx() / safeZoom
        drawRect(
            color = Color.White.copy(alpha = 0.92f),
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
        // Thin L-corner handles — CapCut/Lightroom grade, not solid white blobs.
        val arm = 14.dp.toPx() / safeZoom
        val handleStroke = 2.dp.toPx() / safeZoom
        listOf(
            Offset(l, t) to listOf(Offset(l + arm, t), Offset(l, t + arm)),
            Offset(r, t) to listOf(Offset(r - arm, t), Offset(r, t + arm)),
            Offset(l, b) to listOf(Offset(l + arm, b), Offset(l, b - arm)),
            Offset(r, b) to listOf(Offset(r - arm, b), Offset(r, b - arm))
        ).forEach { (corner, arms) ->
            arms.forEach { end ->
                drawLine(Color.White, corner, end, strokeWidth = handleStroke)
            }
        }
    }
        CropEdgeSemantics(CropBound.Left, edgeLeft, imageRect, crop)
        CropEdgeSemantics(CropBound.Right, edgeRight, imageRect, crop)
        CropEdgeSemantics(CropBound.Top, edgeTop, imageRect, crop)
        CropEdgeSemantics(CropBound.Bottom, edgeBottom, imageRect, crop)
    }
}

/** One name per edge: "Crop edge, left". No second frame or corner tree. */
@Composable
private fun CropEdgeSemantics(
    edge: CropBound,
    label: String,
    imageRect: Rect,
    crop: PhotoOp.Crop
) {
    val point = cropEdgeOffset(imageRect, crop, edge)
    Box(
        Modifier
            .offset {
                val half = 24.dp.roundToPx()
                IntOffset(
                    (point.x - half).roundToInt(),
                    (point.y - half).roundToInt()
                )
            }
            .size(48.dp)
            .semantics { contentDescription = label }
            .testTag(cropEdgeTag(edge))
    )
}

internal fun cropEdgeOffset(imageRect: Rect, crop: PhotoOp.Crop, edge: CropBound): Offset {
    val left = imageRect.left + crop.left * imageRect.width
    val top = imageRect.top + crop.top * imageRect.height
    val right = imageRect.left + crop.right * imageRect.width
    val bottom = imageRect.top + crop.bottom * imageRect.height
    return when (edge) {
        CropBound.Left -> Offset(left, (top + bottom) / 2f)
        CropBound.Right -> Offset(right, (top + bottom) / 2f)
        CropBound.Top -> Offset((left + right) / 2f, top)
        CropBound.Bottom -> Offset((left + right) / 2f, bottom)
    }
}

internal fun cropHandleOffset(imageRect: Rect, crop: PhotoOp.Crop, handle: CropHandle): Offset {
    val xFraction = when (handle) {
        CropHandle.TL, CropHandle.BL -> crop.left
        CropHandle.TR, CropHandle.BR -> crop.right
    }
    val yFraction = when (handle) {
        CropHandle.TL, CropHandle.TR -> crop.top
        CropHandle.BL, CropHandle.BR -> crop.bottom
    }
    return Offset(
        imageRect.left + xFraction * imageRect.width,
        imageRect.top + yFraction * imageRect.height
    )
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
    val points = CropHandle.entries.associateWith { corner ->
        cropHandleOffset(imageRect, crop, corner)
    }
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
