package com.pixijoy.ai.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pixijoy.ai.R
import com.pixijoy.core.data.Project
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiCardShape
import com.pixijoy.designsystem.PixiEmptyState
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiSecondaryButton
import com.pixijoy.designsystem.PixiSurface
import com.pixijoy.designsystem.PixiSurface2
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.designsystem.PixiToolTile
import java.io.File

@Composable
fun HomePane(
    projects: List<Project>,
    continueProject: Project?,
    onOpenStudio: () -> Unit,
    onCreate: () -> Unit,
    onContinue: (Project) -> Unit,
    onOpenProject: (Project) -> Unit,
    onPickTool: (StudioTool) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixiBg)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.board_logo_pixijoy),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "PixiJoy",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = PixiText
        )
        Text(
            stringResource(R.string.studio_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = PixiTextMuted,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            stringResource(R.string.studio_on_device_badge),
            style = MaterialTheme.typography.bodySmall,
            color = PixiTextMuted.copy(alpha = 0.8f),
            modifier = Modifier
                .padding(top = 4.dp, bottom = 16.dp)
                .testTag("home_on_device_badge")
        )
        PixiPrimaryButton(
            text = stringResource(R.string.home_create),
            onClick = onCreate,
            modifier = Modifier.testTag("home_create")
        )
        if (continueProject != null) {
            Spacer(Modifier.height(8.dp))
            PixiSecondaryButton(
                text = stringResource(R.string.home_continue, continueProject.title),
                onClick = { onContinue(continueProject) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_continue")
            )
        }
        TextButton(
            onClick = onOpenStudio,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("home_open_studio")
        ) {
            Text(stringResource(R.string.studio_open_tools), color = PixiTextMuted)
        }

        Text(
            stringResource(R.string.home_start_with),
            color = PixiText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = 8.dp, bottom = 10.dp)
                .testTag("home_start_with")
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("home_start_row")
        ) {
            listOf(
                PhotoTools.first { it.key == "editor" },
                PhotoTools.first { it.key == "cutout" },
                MovieTools.first { it.key == "move" }
            ).forEach { tool ->
                PixiToolTile(
                    title = stringResource(tool.title),
                    subtitle = stringResource(tool.subtitle),
                    icon = painterResource(tool.icon),
                    onClick = { onPickTool(tool) },
                    modifier = Modifier
                        .width(168.dp)
                        .testTag("home_start_${tool.key}")
                )
            }
        }

        Text(
            stringResource(R.string.studio_recents),
            color = PixiText,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)
        )
        if (projects.isEmpty()) {
            PixiEmptyState(
                line = stringResource(R.string.studio_recents_empty),
                icon = painterResource(R.drawable.pj_ic_folder),
                showGhostThumbs = true,
                lineModifier = Modifier.testTag("home_recents_empty"),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("home_recents_empty_block")
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_recents")
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectCard(project, onOpenProject)
                }
            }
        }
    }
}

@Composable
internal fun ProjectCard(project: Project, onOpen: (Project) -> Unit) {
    Column(
        modifier = Modifier
            .clip(PixiCardShape)
            .background(PixiSurface)
            .clickable { onOpen(project) }
            .testTag("home_recent")
            .semantics { role = Role.Button }
    ) {
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
            project.title,
            color = PixiText,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            maxLines = 1
        )
    }
}
