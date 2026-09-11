package com.pixijoy.ai.ui.flow.qa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.components.PixiAdaptiveGrid
import com.pixijoy.ai.ui.components.PixiScreenScaffold
import com.pixijoy.ai.ui.components.PixiSecondaryButton
import com.pixijoy.ai.ui.components.PixiTwoPane
import com.pixijoy.ai.ui.components.PrimaryGradientButton
import com.pixijoy.ai.ui.components.WindowWidthSizeClass
import com.pixijoy.ai.ui.components.rememberPixiResponsiveSnapshot
import com.pixijoy.ai.ui.theme.PixiRadii
import com.pixijoy.ai.ui.theme.PixiSpacing

/**
 * UI-7 visual QA hub: live breakpoint readout + adaptive primitive demos + checklist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsiveQaScreen(
    onBack: () -> Unit,
    onOpenFlowIndex: () -> Unit = {},
    onOpenHome: () -> Unit = {},
    onOpenCommunity: () -> Unit = {},
    onOpenCreations: () -> Unit = {}
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val snap = rememberPixiResponsiveSnapshot(darkTheme = dark)
    var selected by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        modifier = Modifier.testTag(TestTags.RESPONSIVE_QA),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.responsive_qa_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { padding ->
        PixiScreenScaffold(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(stringResource(R.string.responsive_qa_live), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(PixiSpacing.sm))
                SnapshotRow(stringResource(R.string.responsive_qa_width_class), snap.widthClass.name)
                SnapshotRow(stringResource(R.string.responsive_qa_size), "${snap.widthDp} × ${snap.heightDp} dp")
                SnapshotRow(stringResource(R.string.responsive_qa_font), String.format("%.2f", snap.fontScale))
                SnapshotRow(stringResource(R.string.responsive_qa_rtl), if (snap.isRtl) "RTL" else "LTR")
                SnapshotRow(stringResource(R.string.responsive_qa_theme), if (snap.darkTheme) "Dark" else "Light")
                Spacer(modifier = Modifier.height(PixiSpacing.md))
                GuidanceForClass(snap.widthClass)
                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(PixiSpacing.lg))

                Text(stringResource(R.string.responsive_qa_grid_demo), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(PixiSpacing.sm))
                PixiAdaptiveGrid(
                    items = (1..8).toList(),
                    modifier = Modifier.height(220.dp)
                ) { n ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        shape = RoundedCornerShape(PixiRadii.md),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            "Tile $n",
                            modifier = Modifier.padding(PixiSpacing.md),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                Text(stringResource(R.string.responsive_qa_twopane_demo), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(PixiSpacing.sm))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(PixiRadii.md),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    PixiTwoPane(
                        showDetail = selected != null,
                        list = {
                            (1..4).forEach { i ->
                                Surface(
                                    onClick = { selected = i },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(PixiRadii.sm),
                                    color = if (selected == i) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        stringResource(R.string.responsive_qa_list_item, i),
                                        modifier = Modifier.padding(PixiSpacing.md)
                                    )
                                }
                            }
                        },
                        detail = {
                            Text(
                                stringResource(R.string.responsive_qa_detail_item, selected ?: 0),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(PixiSpacing.sm))
                            Text(
                                stringResource(R.string.responsive_qa_detail_body),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                Text(stringResource(R.string.responsive_qa_cta_demo), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(PixiSpacing.sm))
                PrimaryGradientButton(text = stringResource(R.string.responsive_qa_primary_cta), onClick = {})
                Spacer(modifier = Modifier.height(PixiSpacing.sm))
                PixiSecondaryButton(text = stringResource(R.string.responsive_qa_secondary_cta), onClick = {})

                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                Text(stringResource(R.string.responsive_qa_checklist), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(PixiSpacing.sm))
                ChecklistItem(stringResource(R.string.responsive_qa_c1))
                ChecklistItem(stringResource(R.string.responsive_qa_c2))
                ChecklistItem(stringResource(R.string.responsive_qa_c3))
                ChecklistItem(stringResource(R.string.responsive_qa_c4))
                ChecklistItem(stringResource(R.string.responsive_qa_c5))
                ChecklistItem(stringResource(R.string.responsive_qa_c6))
                ChecklistItem(stringResource(R.string.responsive_qa_c7))

                Spacer(modifier = Modifier.height(PixiSpacing.xxl))
                PrimaryGradientButton(text = stringResource(R.string.flow_index_title), onClick = onOpenFlowIndex)
                Spacer(modifier = Modifier.height(PixiSpacing.sm))
                PixiSecondaryButton(text = stringResource(R.string.nav_home), onClick = onOpenHome)
                Spacer(modifier = Modifier.height(PixiSpacing.sm))
                PixiSecondaryButton(text = stringResource(R.string.flow27_title), onClick = onOpenCommunity)
                Spacer(modifier = Modifier.height(PixiSpacing.sm))
                PixiSecondaryButton(text = stringResource(R.string.flow18_title), onClick = onOpenCreations)
            }
        }
    }
}

@Composable
private fun SnapshotRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GuidanceForClass(widthClass: WindowWidthSizeClass) {
    val text = when (widthClass) {
        WindowWidthSizeClass.Compact -> stringResource(R.string.responsive_qa_guide_compact)
        WindowWidthSizeClass.Medium -> stringResource(R.string.responsive_qa_guide_medium)
        WindowWidthSizeClass.Expanded -> stringResource(R.string.responsive_qa_guide_expanded)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PixiRadii.md),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(text, modifier = Modifier.padding(PixiSpacing.md))
    }
}

@Composable
private fun ChecklistItem(text: String) {
    Text("☐ $text", modifier = Modifier.padding(vertical = 4.dp))
}
