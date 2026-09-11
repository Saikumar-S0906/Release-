package com.pixijoy.ai.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardSecondaryButton
import com.pixijoy.ai.ui.theme.PixiRadii
import com.pixijoy.ai.ui.theme.PixiSpacing

/**
 * Adaptive screen shell: centers content on tablet, full-bleed on phone.
 * UI-only — wire-up later.
 */
@Composable
fun PixiScreenScaffold(
    modifier: Modifier = Modifier,
    testTag: String? = null,
    maxContentWidth: androidx.compose.ui.unit.Dp = 840.dp,
    contentPadding: PaddingValues = PaddingValues(PixiSpacing.xl),
    content: @Composable () -> Unit
) {
    val widthClass = rememberWindowWidthSizeClass()
    val maxW = when (widthClass) {
        WindowWidthSizeClass.Compact -> androidx.compose.ui.unit.Dp.Unspecified
        WindowWidthSizeClass.Medium -> 720.dp
        WindowWidthSizeClass.Expanded -> maxContentWidth
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (maxW != androidx.compose.ui.unit.Dp.Unspecified) Modifier.widthIn(max = maxW) else Modifier)
                .padding(contentPadding)
        ) {
            content()
        }
    }
}

@Composable
fun PixiSuggestionChip(
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier.heightIn(min = PixiSpacing.touch),
        shape = RoundedCornerShape(PixiRadii.pill),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            selectedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

/** @deprecated Prefer [BoardSecondaryButton]. */
@Composable
fun PixiSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoardSecondaryButton(text = text, onClick = onClick, modifier = modifier)
}

@Composable
fun PixiTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = PixiSpacing.touch)) {
        Text(text, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun PixiIntentTile(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 88.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .semantics { contentDescription = title },
        shape = RoundedCornerShape(PixiRadii.md),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(PixiSpacing.lg),
            horizontalAlignment = Alignment.Start
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Spacer(Modifier.height(PixiSpacing.xs))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PixiEdgeStatePanel(
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(PixiSpacing.md))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(PixiSpacing.xxl))
        BoardPrimaryButton(text = primaryLabel, onClick = onPrimary)
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(PixiSpacing.sm))
            BoardSecondaryButton(text = secondaryLabel, onClick = onSecondary)
        }
    }
}

@Composable
fun PixiChecklistRow(
    items: List<Pair<String, Boolean>>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        items.forEach { (label, done) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = PixiSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (done) "✓" else "○",
                    color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = PixiSpacing.sm)
                )
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
