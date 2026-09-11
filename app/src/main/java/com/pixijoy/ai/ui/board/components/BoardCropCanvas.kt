package com.pixijoy.ai.ui.board.components

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.theme.BoardType
import androidx.compose.material3.Text
import kotlin.math.roundToInt

/** Normalized crop window in 0..1 of the displayed bitmap. */
data class BoardCropNorm(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    fun coerced(): BoardCropNorm {
        val w = width.coerceIn(Min, 1f)
        val h = height.coerceIn(Min, 1f)
        return BoardCropNorm(
            left = left.coerceIn(0f, (1f - w).coerceAtLeast(0f)),
            top = top.coerceIn(0f, (1f - h).coerceAtLeast(0f)),
            width = w,
            height = h
        )
    }

    companion object {
        /** Small enough to pinch in; large enough to still grab with a finger. */
        const val Min = 0.10f
        val Full = BoardCropNorm(0f, 0f, 1f, 1f)

        /**
         * Largest centered window matching [aspectW]:[aspectH] inside a bitmap.
         * Null aspect = free (full frame).
         */
        fun fit(bmpW: Int, bmpH: Int, aspectW: Float?, aspectH: Float?): BoardCropNorm {
            if (aspectW == null || aspectH == null || aspectW <= 0f || aspectH <= 0f) {
                return Full
            }
            val bw = bmpW.coerceAtLeast(1).toFloat()
            val bh = bmpH.coerceAtLeast(1).toFloat()
            val bmpAspect = bw / bh
            val target = aspectW / aspectH
            return if (target >= bmpAspect) {
                val h = (bmpAspect / target).coerceIn(Min, 1f)
                BoardCropNorm(0f, (1f - h) / 2f, 1f, h).coerced()
            } else {
                val w = (target / bmpAspect).coerceIn(Min, 1f)
                BoardCropNorm((1f - w) / 2f, 0f, w, 1f).coerced()
            }
        }
    }
}

/**
 * Live crop preview. Pinch to scale, drag inside to pan, pull corners/edges to stretch.
 * Parent should give this as much space as possible (fill the remaining column).
 */
@Composable
fun BoardCropCanvas(
    bitmap: Bitmap?,
    @DrawableRes fallbackRes: Int,
    crop: BoardCropNorm,
    onCropChange: (BoardCropNorm) -> Unit,
    modifier: Modifier = Modifier
) {
    val snapshot = remember(bitmap) {
        bitmap?.takeIf { !it.isRecycled }?.copy(Bitmap.Config.ARGB_8888, false)?.asImageBitmap()
    }
    val dim = Color.Black.copy(alpha = 0.48f)
    val handle = 28.dp
    var live by remember { mutableStateOf(crop) }
    live = crop
    BoxWithConstraints(modifier = modifier) {
        val boxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val boxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val density = LocalDensity.current
        val win = live.coerced()
        val overlayW = boxW * win.width
        val overlayH = boxH * win.height
        val overlayX = boxW * win.left
        val overlayY = boxH * win.top
        if (snapshot != null) {
            Image(
                bitmap = snapshot,
                contentDescription = stringResource(R.string.board_crop_drag_hint),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        } else {
            Image(
                painter = painterResource(fallbackRes),
                contentDescription = stringResource(R.string.board_crop_drag_hint),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { overlayY.toDp() })
                .background(dim)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(with(density) { (boxH - overlayY - overlayH).coerceAtLeast(0f).toDp() })
                .background(dim)
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(0, overlayY.roundToInt()) }
                .width(with(density) { overlayX.toDp() })
                .height(with(density) { overlayH.toDp() })
                .background(dim)
        )
        Box(
            modifier = Modifier
                .offset {
                    IntOffset((overlayX + overlayW).roundToInt(), overlayY.roundToInt())
                }
                .width(with(density) { (boxW - overlayX - overlayW).coerceAtLeast(0f).toDp() })
                .height(with(density) { overlayH.toDp() })
                .background(dim)
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(overlayX.roundToInt(), overlayY.roundToInt()) }
                .width(with(density) { overlayW.toDp() })
                .height(with(density) { overlayH.toDp() })
                .border(2.dp, Color.White)
                .pointerInput(boxW, boxH) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val cur = live.coerced()
                        val cx = cur.left + cur.width / 2f
                        val cy = cur.top + cur.height / 2f
                        val nw = (cur.width * zoom).coerceIn(BoardCropNorm.Min, 1f)
                        val nh = (cur.height * zoom).coerceIn(BoardCropNorm.Min, 1f)
                        val next = BoardCropNorm(
                            left = cx - nw / 2f + pan.x / boxW,
                            top = cy - nh / 2f + pan.y / boxH,
                            width = nw,
                            height = nh
                        ).coerced()
                        live = next
                        onCropChange(next)
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                repeat(3) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(0.5.dp, Color.White.copy(alpha = 0.8f))
                            )
                        }
                    }
                }
            }
        }
        CropHandle(
            x = overlayX,
            y = overlayY,
            size = handle,
            onDrag = { dx, dy ->
                val cur = live.coerced()
                val next = BoardCropNorm(
                    cur.left + dx / boxW,
                    cur.top + dy / boxH,
                    cur.width - dx / boxW,
                    cur.height - dy / boxH
                ).coerced()
                live = next
                onCropChange(next)
            }
        )
        CropHandle(
            x = overlayX + overlayW,
            y = overlayY,
            size = handle,
            onDrag = { dx, dy ->
                val cur = live.coerced()
                val next = BoardCropNorm(
                    cur.left,
                    cur.top + dy / boxH,
                    cur.width + dx / boxW,
                    cur.height - dy / boxH
                ).coerced()
                live = next
                onCropChange(next)
            }
        )
        CropHandle(
            x = overlayX,
            y = overlayY + overlayH,
            size = handle,
            onDrag = { dx, dy ->
                val cur = live.coerced()
                val next = BoardCropNorm(
                    cur.left + dx / boxW,
                    cur.top,
                    cur.width - dx / boxW,
                    cur.height + dy / boxH
                ).coerced()
                live = next
                onCropChange(next)
            }
        )
        CropHandle(
            x = overlayX + overlayW,
            y = overlayY + overlayH,
            size = handle,
            onDrag = { dx, dy ->
                val cur = live.coerced()
                val next = BoardCropNorm(
                    cur.left,
                    cur.top,
                    cur.width + dx / boxW,
                    cur.height + dy / boxH
                ).coerced()
                live = next
                onCropChange(next)
            }
        )
        Text(
            text = stringResource(R.string.board_crop_drag_hint),
            style = BoardType.caption,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.45f))
        )
    }
}

@Composable
private fun CropHandle(
    x: Float,
    y: Float,
    size: androidx.compose.ui.unit.Dp,
    onDrag: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val px = with(density) { size.toPx() }
    val dragCb = rememberUpdatedState(onDrag)
    Box(
        modifier = Modifier
            .offset { IntOffset((x - px / 2f).roundToInt(), (y - px / 2f).roundToInt()) }
            .size(size)
            .clip(CircleShape)
            .background(Color.White)
            .border(2.dp, Color.Black.copy(alpha = 0.35f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    dragCb.value(drag.x, drag.y)
                }
            }
    )
}
