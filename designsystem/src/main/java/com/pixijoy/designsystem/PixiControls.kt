package com.pixijoy.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Quiet selection chip — soft accent fill + hairline, not a solid purple slab.
 * [dense] tightens padding for short landscape glass / side docks.
 */
@Composable
fun PixiChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = label,
    dense: Boolean = false
) {
    val hPad = if (dense) 11.dp else 14.dp
    val vPad = if (dense) 8.dp else 11.dp
    Text(
        text = label,
        color = if (selected) PixiText else PixiText.copy(alpha = 0.82f),
        style = PixiLabel,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .heightIn(min = if (dense) 40.dp else 48.dp)
            .clip(PixiPillShape)
            .background(
                if (selected) PixiAccentSoft else Color.White.copy(alpha = 0.08f)
            )
            .then(
                if (selected) {
                    Modifier.border(BorderStroke(1.dp, PixiAccentMuted.copy(alpha = 0.55f)), PixiPillShape)
                } else {
                    Modifier.border(BorderStroke(1.dp, PixiBorder), PixiPillShape)
                }
            )
            .clickable(onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Tab
                this.selected = selected
            }
            .padding(horizontal = hPad, vertical = vPad)
    )
}

@Composable
fun PixiIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = PixiText,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .clip(PixiPillShape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = ripple(bounded = true, radius = size / 2),
                onClick = onClick,
                role = Role.Button
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier
                .size(iconSize)
                .clearAndSetSemantics { }
        )
    }
}

private val PixiTrackHeight = 3.dp
private val PixiThumbSize = 18.dp
private val PixiSliderHitHeight = 44.dp

/**
 * Lightroom-grade seekbar: hairline track, small ring thumb, optional center tick
 * for bipolar ranges (exposure, etc.). Shared on phone / landscape / tablet dock.
 */
@Composable
fun PixiSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    contentDescription: String? = null,
    showCenterTick: Boolean = valueRange.start < 0f && valueRange.endInclusive > 0f
) {
    val latestOnChange by rememberUpdatedState(onValueChange)
    val latestFinished by rememberUpdatedState(onValueChangeFinished)
    val latestRange by rememberUpdatedState(valueRange)
    val density = LocalDensity.current
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val coerced = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((coerced - valueRange.start) / span).coerceIn(0f, 1f)
    val thumbPx = with(density) { PixiThumbSize.toPx() }
    val trackAlpha = if (enabled) 1f else 0.38f

    fun valueAt(x: Float): Float {
        val w = trackWidthPx.coerceAtLeast(1f)
        val f = (x / w).coerceIn(0f, 1f)
        return latestRange.start + f * (latestRange.endInclusive - latestRange.start)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PixiSliderHitHeight)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                latestOnChange(valueAt(offset.x))
                                latestFinished?.invoke()
                            }
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = { latestFinished?.invoke() },
                                onDragCancel = { latestFinished?.invoke() },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    latestOnChange(valueAt(change.position.x))
                                }
                            )
                        }
                } else {
                    Modifier
                }
            )
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = fraction,
                    range = 0f..1f
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(PixiTrackHeight)
        ) {
            val h = size.height
            val radius = CornerRadius(h / 2f, h / 2f)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.14f * trackAlpha),
                size = size,
                cornerRadius = radius
            )
            val activeW = size.width * fraction
            if (activeW > 0f) {
                drawRoundRect(
                    color = PixiAccent.copy(alpha = 0.88f * trackAlpha),
                    size = Size(activeW, h),
                    cornerRadius = radius
                )
            }
            if (showCenterTick) {
                val zeroF = ((0f - valueRange.start) / span).coerceIn(0f, 1f)
                val cx = size.width * zeroF
                drawLine(
                    color = Color.White.copy(alpha = 0.45f * trackAlpha),
                    start = Offset(cx, -4.dp.toPx()),
                    end = Offset(cx, h + 4.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
        val x = ((trackWidthPx - thumbPx) * fraction).coerceAtLeast(0f)
        Box(
            Modifier
                .offset { IntOffset(x.roundToInt(), 0) }
                .size(PixiThumbSize)
                .shadow(if (enabled) 2.dp else 0.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(PixiAccentMuted.copy(alpha = if (enabled) 1f else 0.45f))
                .border(1.5.dp, Color.White.copy(alpha = if (enabled) 0.92f else 0.4f), CircleShape)
        )
    }
}

/**
 * Compact labeled control for secondary tools (brush, trim, warp…).
 * Same seekbar craft as [PixiSlider] so tablet / fold / landscape stay consistent.
 */
@Composable
fun PixiLabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    dense: Boolean = false,
    sliderModifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            label,
            color = PixiTextMuted,
            style = PixiHint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!dense) Spacer(Modifier.height(2.dp))
        PixiSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            contentDescription = label,
            modifier = sliderModifier
        )
    }
}

/**
 * Compact bake / apply control. Prefer this over full-width purple slabs
 * when the gesture already shows live preview.
 */
@Composable
fun PixiApply(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
    contentDescription: String = text
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(if (compact) Modifier else Modifier.fillMaxWidth())
            .height(if (compact) 40.dp else 48.dp)
            .semantics { this.contentDescription = contentDescription },
        colors = ButtonDefaults.buttonColors(
            containerColor = PixiAccent.copy(alpha = 0.92f),
            disabledContainerColor = PixiAccent.copy(alpha = 0.35f)
        ),
        shape = PixiControlShape,
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Text(
            text,
            style = PixiLabel,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

/** Segmented control matching quiet chip selection (Photo / Movie, filters). */
@Composable
fun PixiSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = label
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(PixiPillShape)
            .background(if (selected) PixiAccentSoft else PixiSurface2)
            .then(
                if (selected) {
                    Modifier.border(BorderStroke(1.dp, PixiAccentMuted.copy(alpha = 0.5f)), PixiPillShape)
                } else {
                    Modifier
                }
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab
            )
            .semantics { this.contentDescription = contentDescription }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = PixiText,
            style = PixiLabel,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

@Composable
fun PixiValueText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = PixiText,
        style = PixiValue,
        modifier = modifier
    )
}

@Composable
fun PixiHintText(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = PixiText.copy(alpha = 0.78f),
            style = PixiHint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
