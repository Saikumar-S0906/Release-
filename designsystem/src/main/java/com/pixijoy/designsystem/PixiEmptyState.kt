package com.pixijoy.designsystem

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Resting sheen on the empty plate. Same band as a library skeleton, not a second loader. */
const val PixiEmptySheenRest = 0.38f

private val EmptyPlate = RoundedCornerShape(22.dp)
private val GhostThumb = RoundedCornerShape(14.dp)

/**
 * One empty pattern: glass plate, one line, one primary action.
 * The plate reuses the library sheen and the session chrome wash. Callers supply the glyph.
 */
@Composable
fun PixiEmptyState(
    line: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    lineModifier: Modifier = Modifier,
    showGhostThumbs: Boolean = false,
    cta: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showGhostThumbs) {
            PixiGhostThumbs(modifier = Modifier.padding(bottom = 16.dp))
        } else {
            PixiEmptyMark(icon)
        }
        Text(
            text = line,
            color = PixiTextMuted,
            style = PixiHint,
            textAlign = TextAlign.Center,
            modifier = lineModifier
                .padding(top = if (showGhostThumbs) 4.dp else 12.dp)
                .fillMaxWidth()
        )
        if (cta != null) {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                cta()
            }
        }
    }
}

/** Soft glass plate. Still sheen, so an empty surface does not read as a loading thumb. */
@Composable
fun PixiEmptyMark(
    icon: Painter,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(88.dp)
            .clip(EmptyPlate)
            .drawBehind { drawPixiEmptyWash() }
            .border(1.dp, PixiBorder.copy(alpha = 0.85f), EmptyPlate),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = PixiText.copy(alpha = 0.9f),
            modifier = Modifier.size(28.dp)
        )
    }
}

/** Ghost recent thumbs so empty Home/Library don’t read as dead black. */
@Composable
fun PixiGhostThumbs(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        repeat(3) { index ->
            Box(
                Modifier
                    .size(PixiGhostThumbSize)
                    .clip(GhostThumb)
                    .drawBehind {
                        drawRect(PixiSurface2)
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    PixiAccentSoft.copy(alpha = 0.35f - index * 0.08f),
                                    Color.Transparent,
                                    Color(0x18FF5AA5)
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            )
                        )
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.08f),
                                1f to Color.Transparent
                            )
                        )
                    }
                    .border(1.dp, PixiBorder, GhostThumb)
            )
        }
    }
}

private fun DrawScope.drawPixiEmptyWash() {
    drawRect(PixiSurface2)
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            0.18f to PixiBg.copy(alpha = 0.22f),
            0.5f to PixiBg.copy(alpha = 0.58f),
            1f to PixiBg.copy(alpha = 0.72f)
        )
    )
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.10f),
            0.42f to Color.Transparent
        )
    )
    val width = size.width
    if (width <= 0f || size.height <= 0f) return
    val band = width * PixiSkeletonSheenFraction
    val start = pixiSkeletonSheenStart(PixiEmptySheenRest, width)
    drawRect(
        brush = Brush.linearGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.22f to PixiSkeletonSheen,
                0.5f to PixiSkeletonSheenCore,
                0.78f to PixiSkeletonSheen,
                1f to Color.Transparent
            ),
            start = Offset(start, size.height),
            end = Offset(start + band, 0f)
        )
    )
}
