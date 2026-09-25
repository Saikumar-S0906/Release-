package com.pixijoy.ai.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pixijoy.ai.R
import com.pixijoy.photo.Stamp
import kotlin.math.hypot

@Composable
fun BrushOverlay(
    imageRect: Rect,
    radius: Float,
    onStamp: (Stamp) -> Unit,
    modifier: Modifier = Modifier
) {
    var finger by remember { mutableStateOf<Offset?>(null) }

    fun toStamp(offset: Offset): Stamp? {
        if (imageRect.width <= 0f || imageRect.height <= 0f) return null
        val x = ((offset.x - imageRect.left) / imageRect.width).coerceIn(0f, 1f)
        val y = ((offset.y - imageRect.top) / imageRect.height).coerceIn(0f, 1f)
        if (offset.x < imageRect.left || offset.x > imageRect.right) return null
        if (offset.y < imageRect.top || offset.y > imageRect.bottom) return null
        return Stamp(x, y, radius)
    }

    val brushLabel = stringResource(R.string.studio_brush)
    Canvas(
        modifier = modifier
            .testTag("session_brush")
            .semantics { contentDescription = brushLabel }
            .pointerInput(imageRect, radius) {
                detectDragGestures(
                    onDragStart = { start ->
                        finger = start
                        toStamp(start)?.let(onStamp)
                    },
                    onDragEnd = { finger = null },
                    onDragCancel = { finger = null },
                    onDrag = { change, _ ->
                        change.consume()
                        finger = change.position
                        toStamp(change.position)?.let(onStamp)
                    }
                )
            }
            .pointerInput(imageRect, radius) {
                detectTapGestures { toStamp(it)?.let(onStamp) }
            }
    ) {
        val center = finger
        if (center != null) {
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = radius * minOf(imageRect.width, imageRect.height),
                center = center
            )
        }
    }
}

@Composable
fun TapPlaceOverlay(
    imageRect: Rect,
    onPlace: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val placeLabel = stringResource(R.string.a11y_place)
    Canvas(
        modifier = modifier
            .testTag("session_place")
            .semantics { contentDescription = placeLabel }
            .pointerInput(imageRect) {
                detectTapGestures { offset ->
                    if (imageRect.width <= 0f) return@detectTapGestures
                    val x = ((offset.x - imageRect.left) / imageRect.width).coerceIn(0f, 1f)
                    val y = ((offset.y - imageRect.top) / imageRect.height).coerceIn(0f, 1f)
                    onPlace(x, y)
                }
            }
    ) {}
}

fun stampSpacing(prev: Stamp?, next: Stamp): Boolean {
    if (prev == null) return true
    return hypot((prev.x - next.x).toDouble(), (prev.y - next.y).toDouble()) >= next.r * 0.45
}
