package com.pixijoy.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pixijoy.ai.R
import com.pixijoy.core.data.Project
import com.pixijoy.core.media.MediaKind
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiCardShape
import com.pixijoy.designsystem.PixiError
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiSurface
import com.pixijoy.designsystem.PixiSurface2
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun LibraryPane(
    projects: List<Project>,
    onOpenProject: (Project) -> Unit,
    onRequestDelete: (Project) -> Unit,
    onRenameProject: (Project, String) -> Unit,
    onCreate: () -> Unit
) {
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
    var renameTarget by remember { mutableStateOf<Project?>(null) }
    val visible = projects.filterBy(filter)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixiBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            stringResource(R.string.nav_library),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = PixiText
        )
        Text(
            stringResource(R.string.studio_on_device_badge),
            color = PixiTextMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(top = 4.dp, bottom = 12.dp)
                .testTag("library_on_device_badge")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            FilterChip(stringResource(R.string.studio_filter_all), filter == LibraryFilter.ALL, "library_filter_all") {
                filter = LibraryFilter.ALL
            }
            FilterChip(stringResource(R.string.studio_photo), filter == LibraryFilter.PHOTO, "library_filter_photo") {
                filter = LibraryFilter.PHOTO
            }
            FilterChip(stringResource(R.string.studio_movie), filter == LibraryFilter.MOVIE, "library_filter_movie") {
                filter = LibraryFilter.MOVIE
            }
        }
        if (projects.isEmpty()) {
            EmptyLibrary(
                message = stringResource(R.string.studio_library_empty),
                tag = "library_empty",
                showCreate = true,
                onCreate = onCreate
            )
        } else if (visible.isEmpty()) {
            EmptyLibrary(
                message = stringResource(R.string.studio_library_filter_empty),
                tag = "library_filter_empty",
                showCreate = false,
                onCreate = onCreate
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("library_grid")
            ) {
                items(visible, key = { it.id }) { project ->
                    SwipeDeleteCard(
                        project = project,
                        onOpen = onOpenProject,
                        onRequestDelete = onRequestDelete,
                        onRename = { renameTarget = it }
                    )
                }
            }
        }
    }
    renameTarget?.let { target ->
        RenameProjectDialog(
            project = target,
            onDismiss = { renameTarget = null },
            onConfirm = { title ->
                onRenameProject(target, title)
                renameTarget = null
            }
        )
    }
}

@Composable
private fun SwipeDeleteCard(
    project: Project,
    onOpen: (Project) -> Unit,
    onRequestDelete: (Project) -> Unit,
    onRename: (Project) -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRequestDelete(project)
                // Keep the card; soft-delete hides it from the filtered list in the host.
                false
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(PixiCardShape)
                    .background(PixiError)
                    .padding(12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(stringResource(R.string.studio_library_delete), color = PixiText)
            }
        }
    ) {
        LibraryProjectCard(
            project = project,
            onOpen = onOpen,
            onRename = { onRename(project) },
            onDelete = { onRequestDelete(project) }
        )
    }
}

@Composable
private fun LibraryProjectCard(
    project: Project,
    onOpen: (Project) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .clip(PixiCardShape)
            .background(PixiSurface)
            .clickable { onOpen(project) }
            .testTag("library_project_card")
            .semantics { role = Role.Button }
    ) {
        Box {
            AsyncImage(
                model = File(project.coverPath),
                contentDescription = project.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(PixiSurface2)
            )
            Text(
                text = stringResource(
                    if (project.kind == MediaKind.MOVIE) R.string.studio_movie else R.string.studio_photo
                ),
                color = PixiText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PixiBg.copy(alpha = 0.72f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("library_kind_badge")
            )
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier
                        .testTag("library_project_overflow")
                        .semantics { role = Role.Button }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_more),
                        contentDescription = stringResource(R.string.studio_library_more),
                        tint = PixiText
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.studio_library_rename)) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                        modifier = Modifier.testTag("library_action_rename")
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.studio_library_delete)) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                        modifier = Modifier.testTag("library_action_delete")
                    )
                }
            }
        }
        Text(
            project.title,
            color = PixiText,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1
        )
        Text(
            relativeEdited(project.updatedAt),
            color = PixiTextMuted,
            fontSize = 11.sp,
            modifier = Modifier
                .padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
                .testTag("library_edited_ago")
        )
        project.source?.durationMs?.takeIf { project.kind == MediaKind.MOVIE && it > 0 }?.let { ms ->
            Text(
                formatDuration(ms),
                color = PixiTextMuted,
                fontSize = 11.sp,
                modifier = Modifier
                    .padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
                    .testTag("library_duration")
            )
        }
    }
}

@Composable
private fun relativeEdited(updatedAt: Long): String {
    val agoMs = (System.currentTimeMillis() - updatedAt).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(agoMs)
    return when {
        minutes < 1 -> stringResource(R.string.studio_library_edited_just_now)
        minutes < 60 -> stringResource(R.string.studio_library_edited_minutes, minutes.toInt())
        minutes < 60 * 24 -> stringResource(
            R.string.studio_library_edited_hours,
            TimeUnit.MINUTES.toHours(minutes).toInt()
        )
        else -> stringResource(
            R.string.studio_library_edited_days,
            TimeUnit.MINUTES.toDays(minutes).toInt().coerceAtLeast(1)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

@Composable
private fun RenameProjectDialog(
    project: Project,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var draft by rememberSaveable(project.id) { mutableStateOf(project.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.studio_library_rename), color = PixiText) },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_rename_field")
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(draft) },
                modifier = Modifier.testTag("library_rename_confirm")
            ) {
                Text(stringResource(R.string.studio_apply), color = PixiAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.studio_cancel), color = PixiTextMuted)
            }
        }
    )
}

@Composable
private fun FilterChip(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Text(
        label,
        color = PixiText,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) PixiAccent else PixiSurface2)
            .clickable(onClick = onClick)
            .testTag(tag)
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun EmptyLibrary(
    message: String,
    tag: String,
    showCreate: Boolean,
    onCreate: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.pj_ic_folder),
                contentDescription = null,
                tint = PixiAccent,
                modifier = Modifier.size(40.dp)
            )
            Text(
                message,
                color = PixiTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .testTag(tag)
            )
            if (showCreate) {
                PixiPrimaryButton(
                    text = stringResource(R.string.studio_library_create_cta),
                    onClick = onCreate,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .testTag("library_empty_create")
                )
            }
        }
    }
}
