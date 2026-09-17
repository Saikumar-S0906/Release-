package com.pixijoy.ai.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiSurface
import com.pixijoy.designsystem.PixiSurface2
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import java.io.File

@Composable
fun HomePane(
    projects: List<Project>,
    onOpenStudio: () -> Unit,
    onTakePhoto: () -> Unit,
    onGallery: () -> Unit,
    onSample: () -> Unit,
    onOpenProject: (Project) -> Unit
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
            stringResource(R.string.studio_promise),
            style = MaterialTheme.typography.bodySmall,
            color = PixiTextMuted.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )
        PixiPrimaryButton(
            text = stringResource(R.string.studio_open),
            onClick = onOpenStudio,
            modifier = Modifier.testTag("home_open_studio")
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CaptureChip(stringResource(R.string.studio_take_photo), "home_take_photo", onTakePhoto, Modifier.weight(1f))
            CaptureChip(stringResource(R.string.studio_gallery), "home_gallery", onGallery, Modifier.weight(1f))
            CaptureChip(stringResource(R.string.studio_use_sample), "home_use_sample", onSample, Modifier.weight(1f))
        }
        Text(
            stringResource(R.string.studio_recents),
            color = PixiText,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
        )
        if (projects.isEmpty()) {
            Text(
                stringResource(R.string.studio_recents_empty),
                color = PixiTextMuted,
                modifier = Modifier.testTag("home_recents_empty")
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
private fun CaptureChip(label: String, tag: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        label,
        color = PixiText,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(PixiSurface2)
            .clickable(onClick = onClick)
            .testTag(tag)
            .semantics { role = Role.Button }
            .padding(horizontal = 8.dp, vertical = 12.dp),
        maxLines = 1
    )
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
