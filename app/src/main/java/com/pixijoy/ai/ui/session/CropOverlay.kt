package com.pixijoy.ai.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pixijoy.photo.PhotoOp
import kotlin.math.hypot

private enum class CropHandle { TL, TR, BL, BR }

@Composable
fun CropOverlay(
    crop: PhotoOp.Crop,
    imageRect: Rect,
    onChange: (PhotoOp.Crop) -> Unit,
    modifier: Modifier = Modifier
) {
    var handle by remember { mutableStateOf<CropHandle?>(null) }
    Canvas(
        modifier = modifier
            .testTag("session_crop_overlay")
            .pointerInput(crop, imageRect) {
                detectDragGestures(
                    onDragStart = { start ->
                        handle = nearestHandle(start, imageRect, crop)
                    },
                    onDragEnd = { handle = null },
                    onDragCancel = { handle = null },
                    onDrag = { change, amount ->
                        change.consume()
                        val active = handle ?: return@detectDragGestures
                        if (imageRect.width <= 0f || imageRect.height <= 0f) return@detectDragGestures
                        onChange(
                            crop.drag(
                                active,
                                amount.x / imageRect.width,
                                amount.y / imageRect.height
                            )
                        )
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
        drawRect(
            color = Color.White,
            topLeft = Offset(l, t),
            size = Size(r - l, b - t),
            style = Stroke(width = 2.dp.toPx())
        )
        val radius = 10.dp.toPx()
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

private fun nearestHandle(start: Offset, imageRect: Rect, crop: PhotoOp.Crop): CropHandle {
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
    return points.minBy { hypot((it.value.x - start.x).toDouble(), (it.value.y - start.y).toDouble()) }.key
}

private fun PhotoOp.Crop.drag(handle: CropHandle, dx: Float, dy: Float): PhotoOp.Crop {
    val min = 0.15f
    var l = left
    var t = top
    var r = right
    var b = bottom
    when (handle) {
        CropHandle.TL -> {
            l = (l + dx).coerceIn(0f, r - min)
            t = (t + dy).coerceIn(0f, b - min)
        }
        CropHandle.TR -> {
            r = (r + dx).coerceIn(l + min, 1f)
            t = (t + dy).coerceIn(0f, b - min)
        }
        CropHandle.BL -> {
            l = (l + dx).coerceIn(0f, r - min)
            b = (b + dy).coerceIn(t + min, 1f)
        }
        CropHandle.BR -> {
            r = (r + dx).coerceIn(l + min, 1f)
            b = (b + dy).coerceIn(t + min, 1f)
        }
    }
    return copy(left = l, top = t, right = r, bottom = b)
}
