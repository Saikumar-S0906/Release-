package com.pixijoy.ai.ui.flow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.theme.PixiSpacing

/**
 * Debug hub: open board catalog or responsive QA.
 * Pre-board `f*` catalog removed in Phase 2 cleanup — use Board UI Index.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowIndexScreen(
    onBack: () -> Unit = {},
    onOpenResponsiveQa: () -> Unit = {},
    onOpenBoardUi: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        modifier = Modifier.testTag(TestTags.FLOW_INDEX),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.flow_index_title), color = colors.onBackground) },
                navigationIcon = {
                    Text(
                        stringResource(R.string.cd_back),
                        modifier = Modifier
                            .clickable(onClick = onBack)
                            .padding(PixiSpacing.lg),
                        color = colors.secondary
                    )
                },
                actions = {
                    Text(
                        stringResource(R.string.responsive_qa_title),
                        modifier = Modifier
                            .clickable(onClick = onOpenResponsiveQa)
                            .padding(PixiSpacing.lg),
                        color = colors.secondary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.board_ui_open), color = colors.onBackground) },
                    supportingContent = {
                        Text(stringResource(R.string.board_ui_index_title), color = colors.onSurfaceVariant)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenBoardUi)
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.responsive_qa_title), color = colors.onBackground)
                    },
                    supportingContent = {
                        Text(stringResource(R.string.responsive_qa_live), color = colors.onSurfaceVariant)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenResponsiveQa)
                )
            }
        }
    }
}
