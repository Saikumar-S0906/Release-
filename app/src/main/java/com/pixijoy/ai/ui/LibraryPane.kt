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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.core.data.Project
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiError
import com.pixijoy.designsystem.PixiSurface2
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted

@Composable
fun LibraryPane(
    projects: List<Project>,
    onOpenProject: (Project) -> Unit,
    onDeleteProject: (Project) -> Unit
) {
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
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
            stringResource(R.string.studio_promise),
            color = PixiTextMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
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
            EmptyLibrary(stringResource(R.string.studio_library_empty), "library_empty")
        } else if (visible.isEmpty()) {
            EmptyLibrary(stringResource(R.string.studio_library_filter_empty), "library_filter_empty")
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
                    SwipeDeleteCard(project, onOpenProject, onDeleteProject)
                }
            }
        }
    }
}

@Composable
private fun SwipeDeleteCard(
    project: Project,
    onOpen: (Project) -> Unit,
    onDelete: (Project) -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(project)
                true
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
                    .clip(com.pixijoy.designsystem.PixiCardShape)
                    .background(PixiError)
                    .padding(12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(stringResource(R.string.studio_library_delete), color = PixiText)
            }
        }
    ) {
        ProjectCard(project, onOpen)
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Text(
        label,
        color = PixiText,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) PixiAccent else PixiSurface2)
            .clickable(onClick = onClick)
            .testTag(tag)
            .semantics { role = Role.Tab }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun EmptyLibrary(message: String, tag: String) {
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
        }
    }
}
