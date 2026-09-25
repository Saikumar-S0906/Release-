package com.pixijoy.ai.ui.session

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiError
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted

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
    Layout("studio_group_layout")
}

enum class MovieEffectSub(val tag: String) {
    Motion("movie_effect_motion"),
    Frame("movie_effect_frame"),
    Chroma("movie_effect_chroma"),
    SlowMo("movie_effect_slow"),
    Track("movie_effect_track")
}

data class StudioGroupItem(
    val tag: String,
    val label: String,
    val iconRes: Int
)

/** Bottom glass: long fade into the picture, one panel, then the icon rail.
 *  [sideDock] is the G1 tablet/fold inner layout: tools sit beside the canvas. */
@Composable
fun StudioChromeBar(
    modifier: Modifier = Modifier,
    sideDock: Boolean = false,
    panelScrollKey: Any = Unit,
    abovePanel: (@Composable ColumnScope.() -> Unit)? = null,
    rail: @Composable () -> Unit,
    panel: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .then(if (sideDock) Modifier.width(360.dp).fillMaxHeight() else Modifier.fillMaxWidth())
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
        StudioFrostedPanel(
            modifier = if (sideDock) Modifier.weight(1f) else Modifier,
            expanded = sideDock,
            scrollKey = panelScrollKey,
            content = panel
        )
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
 * Tall enough that common groups fit; when content still overflows, a bottom fade
 * and chevron tell the user to scroll. Glass stays translucent so the picture reads through.
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
    Box(
        modifier
            .fillMaxWidth()
            .then(if (expanded) Modifier.fillMaxHeight() else Modifier.heightIn(max = 220.dp))
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color.Black.copy(alpha = 0.40f))
            .testTag("session_tool_scrim")
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .padding(bottom = if (moreBelow) 18.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
        if (moreBelow) {
            val more = stringResource(R.string.studio_scroll_more)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.72f)
                        )
                    )
                    .testTag("studio_scroll_more")
                    .semantics { contentDescription = more },
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    more,
                    color = PixiText.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
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
                    .testTag("studio_hscroll_more")
                    .semantics { contentDescription = more }
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
                    tint = if (selected) PixiAccent else PixiText.copy(alpha = 0.78f),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    item.label,
                    color = if (selected) PixiText else PixiTextMuted,
                    fontSize = 12.sp,
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
                        .background(if (selected) PixiAccent else Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun StudioChip(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    val spoken = talkBackName(label, selected, stringResource(R.string.a11y_selected))
    Text(
        label,
        color = if (selected) PixiText else PixiText.copy(alpha = 0.85f),
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (selected) PixiAccent.copy(alpha = 0.92f)
                else Color.White.copy(alpha = 0.10f)
            )
            .clickable(onClick = onClick)
            .testTag(tag)
            .semantics { contentDescription = spoken }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}

@Composable
fun StudioHint(text: String, tag: String? = null) {
    Text(
        text,
        color = PixiTextMuted,
        fontSize = 12.sp,
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
    tag: String? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .semantics { contentDescription = text },
        colors = ButtonDefaults.buttonColors(containerColor = PixiAccent),
        shape = RoundedCornerShape(12.dp),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Text(
            text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

/**
 * Lightroom-style: large live value, thin slider.
 * Short-tap the value to type a number (clamped to [range] on IME Done or focus clear).
 * Long-press resets to [defaultValue]. A value that does not parse is left unchanged.
 * When [showLabel] is false the chip row already names the control.
 */
@OptIn(ExperimentalFoundationApi::class)
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
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .combinedClickable(
                        onClickLabel = editLabel,
                        onLongClickLabel = resetLabel,
                        onLongClick = {
                            onChange(defaultValue)
                            onSettled()
                        },
                        onClick = { editing = true }
                    )
                    .testTag("session_slider_value")
                    .semantics { contentDescription = spokenValue }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        if (showLabel) {
            Text(label, color = PixiTextMuted, fontSize = 12.sp)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            onValueChangeFinished = onSettled,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = PixiAccent,
                activeTrackColor = PixiAccent,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag(tag)
                .semantics { contentDescription = label }
        )
    }
}
