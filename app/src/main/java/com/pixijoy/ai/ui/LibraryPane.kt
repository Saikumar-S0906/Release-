package com.pixijoy.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.core.data.Project
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted

@Composable
fun LibraryPane(
    projects: List<Project>,
    onOpenProject: (Project) -> Unit
) {
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
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )
        if (projects.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_folder),
                        contentDescription = null,
                        tint = PixiAccent,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        stringResource(R.string.studio_library_empty),
                        color = PixiTextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .testTag("library_empty")
                    )
                }
            }
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
                items(projects, key = { it.id }) { project ->
                    ProjectCard(project, onOpenProject)
                }
            }
        }
    }
}
