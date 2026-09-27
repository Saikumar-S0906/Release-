package com.pixijoy.ai.ui.session

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.studioGlassMaxDp
import com.pixijoy.ai.ui.studioSideDockWidthDp
import com.pixijoy.designsystem.PixiAccentMuted
import com.pixijoy.designsystem.PixiApply
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiChip
import com.pixijoy.designsystem.PixiChipSelectedScale
import com.pixijoy.designsystem.PixiError
import com.pixijoy.designsystem.PixiHint
import com.pixijoy.designsystem.PixiLabel
import com.pixijoy.designsystem.PixiLabeledSlider
import com.pixijoy.designsystem.PixiSlider
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.designsystem.PixiValue
import com.pixijoy.designsystem.pixiChipTween
import com.pixijoy.designsystem.pixiGroupTween
import kotlinx.coroutines.launch

enum class PhotoGroup(val tag: String) {
    Crop("studio_group_crop"),
    Light("studio_group_light"),
    Color("studio_group_color"),
    Retouch("studio_group_retouch"),
    Compose("studio_group_compose"),
    Add("studio_group_add")
}

enum class MovieGroup(val tag: String) {
    Edit("studio_group_edit"),
    Audio("studio_group_audio"),
    Text("studio_group_text"),
    Color("studio_group_color"),
    Effects("studio_group_effects"),
    /** R7 / CODING_PHASES P1 — Filament CG lane (classical plate until feature_cg). */
    Cg("studio_group_cg"),
    Layout("studio_group_layout"),
    /** R9 delivery placeholders (review / handoff presets land in P6). */
    Deliver("studio_group_deliver")
}

enum class MovieEffectSub(val tag: String) {
    Motion("movie_effect_motion"),
    Frame("movie_effect_frame"),
    Chroma("movie_effect_chroma"),
    SlowMo("movie_effect_slow"),
    Track("movie_effect_track"),
    Vfx("movie_effect_vfx")
}

data class StudioGroupItem(
    val tag: String,
    val label: String,
    val iconRes: Int
)

/** Bottom glass: long fade into the picture, one panel, then the icon rail.
 *  [sideDock] is the G1 tablet / fold / phone-landscape layout: tools sit beside the canvas. */
@Composable
fun StudioChromeBar(
    modifier: Modifier = Modifier,
    sideDock: Boolean = false,
    panelScrollKey: Any = Unit,
    abovePanel: (@Composable ColumnScope.() -> Unit)? = null,
    rail: @Composable () -> Unit,
    panel: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val dockWidth = studioSideDockWidthDp(
        configuration.screenWidthDp,
        configuration.screenHeightDp
    ).dp
    Column(
        modifier
            .then(if (sideDock) Modifier.width(dockWidth).fillMaxHeight() else Modifier.fillMaxWidth())
            // Edge-to-edge + adjustResize: lift glass above the IME so tap-to-type stays visible.
            .imePadding()
            .background(
                if (sideDock) {
                    Brush.horizontalGradient(
                        0f to PixiBg.copy(alpha = 0.78f),
                        1f to PixiBg.copy(alpha = 0.94f)
                    )
                } else {
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.18f to PixiBg.copy(alpha = 0.22f),
                        0.5f to PixiBg.copy(alpha = 0.58f),
                        1f to PixiBg.copy(alpha = 0.86f)
                    )
                }
            )
            .testTag(if (sideDock) "studio_session_side_tools" else "studio_chrome_bar")
    ) {
        abovePanel?.invoke(this)
        val groupFade = pixiGroupTween()
        AnimatedContent(
            targetState = panelScrollKey,
            transitionSpec = {
                fadeIn(animationSpec = groupFade) togetherWith fadeOut(animationSpec = groupFade)
            },
            label = "studio_panel_group",
            modifier = if (sideDock) Modifier.weight(1f) else Modifier
        ) { key ->
            StudioFrostedPanel(
                modifier = if (sideDock) Modifier.fillMaxHeight() else Modifier,
                expanded = sideDock,
                scrollKey = key,
                content = panel
            )
        }
        rail()
        Spacer(Modifier.height(4.dp))
    }
}

/**
 * Project name on the top scrim. Tap opens rename. The chip hugs the title
 * so the picture still shows through the rest of the bar.
 */
@Composable
fun SessionProjectTitle(
    title: String,
    toolLabel: String,
    onRename: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.heightIn(min = 48.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            color = PixiText,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PixiBg.copy(alpha = 0.55f))
                .clickable(onClickLabel = stringResource(R.string.session_rename_project), onClick = onRename)
                .testTag("session_project_title")
                .semantics {
                    role = Role.Button
                    stateDescription = toolLabel
                }
                .padding(horizontal = 10.dp, vertical = 12.dp)
        )
    }
}

/** Glass status for the graph write. It does not cover the canvas or block tools. */
@Composable
fun SessionAutosaveChip(
    status: GraphSaveStatus,
    modifier: Modifier = Modifier
) {
    val failed = status == GraphSaveStatus.Failed
    val label = stringResource(
        when (status) {
            GraphSaveStatus.Saving -> R.string.session_save_status_saving
            GraphSaveStatus.Failed -> R.string.session_save_status_unsaved
            GraphSaveStatus.Saved -> R.string.session_save_status_saved
        }
    )
    Text(
        text = label,
        color = if (failed) PixiError else PixiText,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PixiBg.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("session_save_status")
            .semantics {
                contentDescription = label
                liveRegion = LiveRegionMode.Polite
            }
    )
}

/** Top glass fade so Back / Export sit on the picture. */
@Composable
fun StudioTopChrome(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to PixiBg.copy(alpha = 0.72f),
                    0.65f to PixiBg.copy(alpha = 0.35f),
                    1f to Color.Transparent
                )
            )
            .padding(bottom = 8.dp),
        content = content
    )
}

/**
 * Tall enough that dense groups (Compose / Edit) usually fit without crushing
 * controls under the rail. When content still overflows, a bottom fade and
 * tappable “More ⌄” scrolls the panel. Glass stays translucent so the picture reads through.
 */
@Composable
fun StudioFrostedPanel(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    scrollKey: Any = Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val scroll = remember(scrollKey) { ScrollState(0) }
    val moreBelow = scroll.maxValue > 0 && scroll.value < scroll.maxValue
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val pagePx = with(density) { 96.dp.roundToPx() }
    val config = LocalConfiguration.current
    val glassMax = studioGlassMaxDp(config.screenHeightDp, config.fontScale).dp
    Box(
        modifier
            .fillMaxWidth()
            .then(if (expanded) Modifier.fillMaxHeight() else Modifier.heightIn(max = glassMax))
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color.Black.copy(alpha = 0.40f))
            .testTag("session_tool_scrim")
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .padding(bottom = if (moreBelow) 36.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
        if (moreBelow) {
            val more = stringResource(R.string.studio_scroll_more)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.78f)
                        )
                    )
                    .clickable(
                        onClickLabel = more,
                        onClick = {
                            scope.launch {
                                val next = (scroll.value + pagePx).coerceAtMost(scroll.maxValue)
                                scroll.animateScrollTo(next)
                            }
                        }
                    )
                    .testTag("studio_scroll_more")
                    .semantics {
                        role = Role.Button
                        contentDescription = more
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    more,
                    color = PixiText.copy(alpha = 0.95f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .clearAndSetSemantics { }
                )
            }
        }
    }
}

/** Horizontal chip strip with edge fades when more items sit off-screen. */
@Composable
fun StudioHScrollRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(6.dp),
    content: @Composable RowScope.() -> Unit
) {
    val scroll = rememberScrollState()
    val moreEnd = scroll.maxValue > 0 && scroll.value < scroll.maxValue
    val moreStart = scroll.value > 0
    Box(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll),
            horizontalArrangement = horizontalArrangement,
            content = content
        )
        if (moreStart) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(20.dp)
                    .height(36.dp)
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Black.copy(alpha = 0.55f),
                            1f to Color.Transparent
                        )
                    )
            )
        }
        if (moreEnd) {
            val more = stringResource(R.string.studio_scroll_more)
            val scope = rememberCoroutineScope()
            val density = LocalDensity.current
            val pagePx = with(density) { 72.dp.roundToPx() }
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(28.dp)
                    .height(36.dp)
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.55f)
                        )
                    )
                    .clickable(
                        onClickLabel = more,
                        onClick = {
                            scope.launch {
                                val next = (scroll.value + pagePx).coerceAtMost(scroll.maxValue)
                                scroll.animateScrollTo(next)
                            }
                        }
                    )
                    .testTag("studio_hscroll_more")
                    .semantics {
                        role = Role.Button
                        contentDescription = more
                    }
            )
        }
    }
}

@Composable
fun StudioGroupRail(
    items: List<StudioGroupItem>,
    selectedTag: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("studio_group_rail"),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val selectedFormat = stringResource(R.string.a11y_selected)
        items.forEach { item ->
            val selected = item.tag == selectedTag
            val spoken = talkBackName(item.label, selected, selectedFormat)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable { onSelect(item.tag) }
                    .testTag(item.tag)
                    .semantics { contentDescription = spoken }
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(item.iconRes),
                    contentDescription = null,
                    tint = if (selected) PixiAccentMuted else PixiText.copy(alpha = 0.78f),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    item.label,
                    color = if (selected) PixiText else PixiTextMuted,
                    style = PixiHint,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clearAndSetSemantics { }
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .width(18.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (selected) PixiAccentMuted else Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun StudioChip(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    val spoken = talkBackName(label, selected, stringResource(R.string.a11y_selected))
    val dense = LocalConfiguration.current.screenHeightDp < 560
    val scale by animateFloatAsState(
        targetValue = if (selected) PixiChipSelectedScale else 1f,
        animationSpec = pixiChipTween(),
        label = "studio_chip_scale"
    )
    PixiChip(
        label = label,
        selected = selected,
        onClick = onClick,
        contentDescription = spoken,
        dense = dense,
        modifier = Modifier
            .scale(scale)
            .testTag(tag)
    )
}

@Composable
fun StudioHint(text: String, tag: String? = null) {
    Text(
        text,
        color = PixiText.copy(alpha = 0.78f),
        style = PixiHint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
    )
}

/** Compact bake control — full-width purple slabs steal the picture. */
@Composable
fun StudioApplyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tag: String? = null,
    compact: Boolean = false
) {
    PixiApply(
        text = text,
        onClick = onClick,
        enabled = enabled,
        compact = compact,
        modifier = modifier.then(if (tag != null) Modifier.testTag(tag) else Modifier)
    )
}

/**
 * Lightroom-style: large live value, thin slider.
 * Short-tap the value to type a number (clamped to [range] on IME Done or focus clear).
 * Long-press resets to [defaultValue]. A value that does not parse is left unchanged.
 * When [showLabel] is false the chip row already names the control.
 * Dense layout kicks in on short landscape glass so tablet/fold/phone stay consistent.
 */
@Composable
fun StudioValueSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    tag: String,
    defaultValue: Float,
    valueText: String,
    onChange: (Float) -> Unit,
    onSettled: () -> Unit = {},
    showLabel: Boolean = true
) {
    val spokenValue = stringResource(R.string.a11y_slider_value, label, valueText)
    val resetLabel = stringResource(R.string.a11y_reset_value)
    val editLabel = stringResource(R.string.a11y_edit_slider_value)
    var editing by remember(tag) { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val dense = LocalConfiguration.current.screenHeightDp < 560
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (editing) {
            SliderValueField(
                initialText = valueText,
                allowNegative = range.start < 0f,
                onCommit = { raw ->
                    if (editing) {
                        editing = false
                        keyboard?.hide()
                        val parsed = parseSliderTypedValue(raw, range)
                        if (parsed != null) {
                            onChange(parsed)
                            onSettled()
                        }
                    }
                }
            )
        } else {
            Text(
                valueText,
                color = PixiText,
                style = if (dense) PixiValue.copy(fontSize = 18.sp) else PixiValue,
                modifier = Modifier
                    .heightIn(min = if (dense) 40.dp else 48.dp)
                    .testTag("session_slider_value")
                    .semantics {
                        contentDescription = spokenValue
                        onLongClick(label = resetLabel) {
                            onChange(defaultValue)
                            onSettled()
                            true
                        }
                    }
                    .clickable(
                        onClickLabel = editLabel,
                        onClick = { editing = true }
                    )
                    .padding(horizontal = 12.dp, vertical = if (dense) 4.dp else 8.dp)
            )
        }
        if (showLabel) {
            Text(label, color = PixiTextMuted, style = PixiLabel)
        }
        PixiSlider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onSettled,
            valueRange = range,
            contentDescription = label,
            modifier = Modifier
                .padding(horizontal = if (dense) 2.dp else 0.dp)
                .testTag(tag)
        )
    }
}

/**
 * Secondary option slider (trim, brush, warp…). Routes through [PixiLabeledSlider]
 * so every form factor shares the same seekbar craft.
 */
@Composable
fun StudioLabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    tag: String,
    onChange: (Float) -> Unit,
    onSettled: () -> Unit = {}
) {
    val dense = LocalConfiguration.current.screenHeightDp < 560
    PixiLabeledSlider(
        label = label,
        value = value,
        onValueChange = onChange,
        valueRange = range,
        onValueChangeFinished = onSettled,
        dense = dense,
        sliderModifier = Modifier.testTag(tag)
    )
}
