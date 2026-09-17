package com.pixijoy.ai.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pixijoy.ai.R
import com.pixijoy.core.data.Project
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import java.io.File

@Composable
fun MovieSessionScreen(
    project: Project,
    toolLabel: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixiBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .testTag("movie_session"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack, modifier = Modifier.testTag("session_back")) {
            Text(stringResource(R.string.studio_back), color = PixiText)
        }
        Text(toolLabel, color = PixiText, fontWeight = FontWeight.SemiBold)
        AsyncImage(
            model = File(project.coverPath),
            contentDescription = project.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(PixiBg)
        )
        Text(
            stringResource(R.string.studio_movie_placeholder),
            color = PixiTextMuted
        )
    }
}
