package com.pixijoy.ai.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.pixijoy.ai.R
import com.pixijoy.core.data.Project
import com.pixijoy.core.media.MediaKind
import com.pixijoy.photo.PhotoPreset
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiCardShape
import com.pixijoy.designsystem.PixiChip
import com.pixijoy.designsystem.PixiEmptyState
import com.pixijoy.designsystem.PixiError
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiSkeleton
import com.pixijoy.designsystem.PixiSurface
import com.pixijoy.designsystem.PixiSurface2
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.designsystem.pixiMotionMs
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun LibraryPane(
    projects: List<Project>,
    onOpenProject: (Project) -> Unit,
    onRequestDelete: (Project) -> Unit,
    onRenameProject: (Project, String) -> Unit,
    onCreate: () -> Unit,
    onExportAllPhotos: (() -> Unit)? = null,
    presets: List<PhotoPreset> = emptyList(),
    lastLookPresetId: String? = null,
    onExportPhotosWithLook: ((String) -> Unit)? = null,
    onDeleteAllProjects: (() -> Unit)? = null,
    onResetAppData: (() -> Unit)? = null
) {
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
    var renameTarget by remember { mutableStateOf<Project?>(null) }
    var showLookPicker by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val visible = projects.filterBy(filter)
    val photoCount = projects.count { it.kind == MediaKind.PHOTO }
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
        if (onExportAllPhotos != null && photoCount > 0) {
            TextButton(
                onClick = onExportAllPhotos,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .heightIn(min = 48.dp)
                    .testTag("library_export_photos")
            ) {
                Text(stringResource(R.string.studio_library_export_photos), color = PixiAccent)
            }
            if (onExportPhotosWithLook != null && presets.isNotEmpty()) {
                TextButton(
                    onClick = { showLookPicker = true },
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .heightIn(min = 48.dp)
                        .testTag("library_export_with_look")
                ) {
                    Text(stringResource(R.string.studio_library_export_with_look), color = PixiAccent)
                }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("library_legal_footer"),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(context.getString(R.string.studio_url_privacy))
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("library_privacy_link")
                ) {
                    Text(stringResource(R.string.studio_privacy_policy), color = PixiTextMuted)
                }
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(context.getString(R.string.studio_url_terms))
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("library_terms_link")
                ) {
                    Text(stringResource(R.string.studio_terms), color = PixiTextMuted)
                }
            }
            if (onDeleteAllProjects != null && projects.isNotEmpty()) {
                TextButton(
                    onClick = { confirmDeleteAll = true },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("library_delete_all")
                ) {
                    Text(stringResource(R.string.studio_library_delete_all), color = PixiTextMuted)
                }
            }
            if (onResetAppData != null) {
                TextButton(
                    onClick = { confirmReset = true },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("library_reset_app")
                ) {
                    Text(stringResource(R.string.studio_library_reset_app), color = PixiTextMuted)
                }
            }
        }
    }
    if (confirmDeleteAll && onDeleteAllProjects != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text(stringResource(R.string.studio_library_delete_all_title)) },
            text = { Text(stringResource(R.string.studio_library_delete_all_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteAll = false
                        onDeleteAllProjects()
                    },
                    modifier = Modifier.testTag("library_delete_all_confirm")
                ) {
                    Text(stringResource(R.string.studio_library_delete_all_confirm), color = PixiAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) {
                    Text(stringResource(R.string.studio_cancel), color = PixiTextMuted)
                }
            }
        )
    }
    if (confirmReset && onResetAppData != null) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.studio_library_reset_title)) },
            text = { Text(stringResource(R.string.studio_library_reset_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        onResetAppData()
                    },
                    modifier = Modifier.testTag("library_reset_confirm")
                ) {
                    Text(stringResource(R.string.studio_library_reset_confirm), color = PixiAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.studio_cancel), color = PixiTextMuted)
                }
            }
        )
    }
    if (showLookPicker && onExportPhotosWithLook != null) {
        AlertDialog(
            onDismissRequest = { showLookPicker = false },
            title = { Text(stringResource(R.string.studio_library_pick_look)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presets.forEach { preset ->
                        val selected = preset.id == lastLookPresetId
                        TextButton(
                            onClick = {
                                onExportPhotosWithLook(preset.id)
                                showLookPicker = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("library_look_${preset.id}")
                        ) {
                            Text(
                                if (selected) {
                                    stringResource(R.string.studio_library_look_recent, preset.name)
                                } else {
                                    preset.name
                                },
                                color = PixiText
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLookPicker = false }) {
                    Text(stringResource(R.string.studio_cancel), color = PixiTextMuted)
                }
            }
        )
    }
    renameTarget?.let { target ->
        ProjectRenameDialog(
            titleKey = target.id,
            initialTitle = target.title,
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
            LibraryCover(project)
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
            Text(
                text = relativeEdited(project.updatedAt),
                color = PixiText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PixiBg.copy(alpha = 0.72f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("library_edited_ago")
            )
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                val moreLabel = stringResource(R.string.studio_library_more)
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier
                        .testTag("library_project_overflow")
                        .semantics {
                            role = Role.Button
                            contentDescription = moreLabel
                        }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_more),
                        contentDescription = null,
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            maxLines = 1
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

private enum class LibraryThumbPhase { Pending, Ready, Failed }

@Composable
private fun LibraryCover(project: Project) {
    val loading = stringResource(R.string.studio_library_thumb_loading)
    val unavailable = stringResource(R.string.studio_library_thumb_unavailable)
    val fadeMs = pixiMotionMs(emphasized = false)
    SubcomposeAsyncImage(
        model = File(project.coverPath),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .testTag("library_thumb")
    ) {
        val imageState by painter.state.collectAsState()
        val phase = when (imageState) {
            is AsyncImagePainter.State.Success -> LibraryThumbPhase.Ready
            is AsyncImagePainter.State.Error -> LibraryThumbPhase.Failed
            else -> LibraryThumbPhase.Pending
        }
        Crossfade(
            targetState = phase,
            modifier = Modifier.fillMaxSize(),
            animationSpec = tween(durationMillis = fadeMs),
            label = "libraryThumb"
        ) { target ->
            when (target) {
                LibraryThumbPhase.Ready -> this@SubcomposeAsyncImage.SubcomposeAsyncImageContent(
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = project.title
                )
                LibraryThumbPhase.Failed -> Box(
                    Modifier
                        .fillMaxSize()
                        .background(PixiSurface2)
                        .semantics { contentDescription = unavailable }
                        .testTag("library_thumb_unavailable")
                )
                LibraryThumbPhase.Pending -> PixiSkeleton(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("library_thumb_skeleton"),
                    contentDescription = loading
                )
            }
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
private fun FilterChip(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    PixiChip(
        label = label,
        selected = selected,
        onClick = onClick,
        contentDescription = label,
        modifier = Modifier.testTag(tag)
    )
}

@Composable
private fun EmptyLibrary(
    message: String,
    tag: String,
    showCreate: Boolean,
    onCreate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (showCreate) Modifier.testTag("empty_library") else Modifier),
        contentAlignment = Alignment.Center
    ) {
        PixiEmptyState(
            line = message,
            icon = painterResource(R.drawable.pj_ic_folder),
            showGhostThumbs = showCreate,
            lineModifier = Modifier.testTag(tag),
            cta = if (showCreate) {
                {
                    PixiPrimaryButton(
                        text = stringResource(R.string.studio_library_create_cta),
                        onClick = onCreate,
                        modifier = Modifier.testTag("library_empty_create")
                    )
                }
            } else {
                null
            }
        )
    }
}
