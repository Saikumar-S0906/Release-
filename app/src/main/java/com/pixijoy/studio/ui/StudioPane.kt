package com.pixijoy.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixijoy.studio.R
import com.pixijoy.core.media.MediaKind
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiSegment
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.designsystem.PixiToolTile

data class StudioTool(
    val title: Int,
    val subtitle: Int,
    val icon: Int,
    val tag: String,
    val kind: MediaKind,
    val key: String
)

val PhotoTools = listOf(
    StudioTool(R.string.tool_editor, R.string.tool_editor_sub, R.drawable.pj_ic_edit, "create_hub_editor", MediaKind.PHOTO, "editor"),
    StudioTool(R.string.tool_restore, R.string.tool_restore_sub, R.drawable.pj_ic_heal, "create_hub_restore", MediaKind.PHOTO, "restore"),
    StudioTool(R.string.tool_expand, R.string.tool_expand_sub, R.drawable.pj_ic_expand, "create_hub_expand", MediaKind.PHOTO, "expand"),
    StudioTool(R.string.tool_cutout, R.string.tool_cutout_sub, R.drawable.pj_ic_cut, "create_hub_remove_bg", MediaKind.PHOTO, "cutout"),
    StudioTool(R.string.tool_stickers, R.string.tool_stickers_sub, R.drawable.pj_ic_sticker, "create_hub_sticker", MediaKind.PHOTO, "stickers"),
    StudioTool(R.string.tool_collage, R.string.tool_collage_sub, R.drawable.pj_ic_grid, "create_hub_collage", MediaKind.PHOTO, "collage"),
    StudioTool(R.string.tool_layers, R.string.tool_layers_sub, R.drawable.pj_ic_layers, "create_hub_layers", MediaKind.PHOTO, "layers")
)

val MovieTools = listOf(
    StudioTool(R.string.tool_move, R.string.tool_move_sub, R.drawable.pj_ic_movie, "create_hub_move", MediaKind.MOVIE, "move"),
    StudioTool(R.string.tool_talking, R.string.tool_talking_sub, R.drawable.pj_ic_gif, "create_hub_talking", MediaKind.MOVIE, "talking")
)

private val RecommendedPhoto = listOf("editor", "cutout")
private val RecommendedMovie = listOf("move")

@Composable
fun StudioPane(onPickTool: (StudioTool) -> Unit, modifier: Modifier = Modifier) {
    var photo by rememberSaveable { mutableStateOf(true) }
    val tools = if (photo) PhotoTools else MovieTools
    val recommendedKeys = if (photo) RecommendedPhoto else RecommendedMovie
    val recommended = tools.filter { it.key in recommendedKeys }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val columns = studioPaneColumns(maxWidth.value.toInt())
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixiBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            stringResource(R.string.nav_studio),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = PixiText
        )
        Text(
            stringResource(R.string.studio_on_device_badge),
            color = PixiTextMuted,
            modifier = Modifier
                .padding(top = 4.dp, bottom = 12.dp)
                .testTag("studio_on_device_badge")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Segment(
                label = stringResource(R.string.studio_photo),
                selected = photo,
                tag = "create_hub_segment_tools",
                onClick = { photo = true }
            )
            Segment(
                label = stringResource(R.string.studio_movie),
                selected = !photo,
                tag = "create_hub_segment_movie",
                onClick = { photo = false }
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp, top = 12.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (recommended.isNotEmpty()) {
                item(span = { GridItemSpan(columns) }) {
                    Text(
                        stringResource(R.string.studio_recommended),
                        color = PixiText,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .testTag("studio_recommended_label")
                    )
                }
                item(span = { GridItemSpan(columns) }) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("studio_recommended_row")
                    ) {
                        recommended.forEach { tool ->
                            Box(Modifier.testTag("recommended_${tool.tag}")) {
                                PixiToolTile(
                                    title = stringResource(tool.title),
                                    subtitle = stringResource(
                                        when (tool.key) {
                                            "editor" -> R.string.tool_best_for_editor
                                            "cutout" -> R.string.tool_best_for_cutout
                                            "move" -> R.string.tool_best_for_move
                                            else -> tool.subtitle
                                        }
                                    ),
                                    icon = painterResource(tool.icon),
                                    onClick = { onPickTool(tool) },
                                    modifier = Modifier.testTag(tool.tag)
                                )
                            }
                        }
                    }
                }
            }
            items(tools.filter { it.key !in recommendedKeys }, key = { it.tag }) { tool ->
                PixiToolTile(
                    title = stringResource(tool.title),
                    subtitle = stringResource(tool.subtitle),
                    icon = painterResource(tool.icon),
                    onClick = { onPickTool(tool) },
                    modifier = Modifier.testTag(tool.tag)
                )
            }
        }
    }
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    PixiSegment(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = Modifier.testTag(tag)
    )
}
