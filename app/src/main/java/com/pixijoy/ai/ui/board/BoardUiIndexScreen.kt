package com.pixijoy.ai.ui.board

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.theme.PixiSpacing

/**
 * Debug hub for board screen catalog (production UI is always board).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardUiIndexScreen(
    onBack: () -> Unit,
    onOpenScreen: (BoardScreenEntry) -> Unit
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.BOARD_UI_INDEX),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.board_ui_index_title)) },
                navigationIcon = {
                    Text(
                        stringResource(R.string.cd_back),
                        modifier = Modifier
                            .clickable(onClick = onBack)
                            .padding(PixiSpacing.lg),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Text(
                    stringResource(R.string.board_ui_catalog_hint),
                    modifier = Modifier.padding(horizontal = PixiSpacing.xl, vertical = PixiSpacing.sm),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            items(BoardUiCatalog.all, key = { it.id }) { entry ->
                ListItem(
                    headlineContent = { Text("F${entry.flow} · ${entry.title}") },
                    supportingContent = {
                        Text("${entry.kind} · ${entry.status.releaseLabel} · ${entry.id}")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenScreen(entry) }
                )
            }
            item { Spacer(modifier = Modifier.height(PixiSpacing.xxl)) }
        }
    }
}
