package com.pixijoy.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Honest capability label (CODING_PHASES P0.2).
 * Live = works now · NeedsPack = download/bind first · ExportOnly = preview differs.
 */
enum class PixiCapabilityKind {
    Live,
    NeedsPack,
    ExportOnly
}

@Composable
fun PixiCapabilityChip(
    kind: PixiCapabilityKind,
    label: String,
    modifier: Modifier = Modifier,
    testTag: String = "capability_chip"
) {
    val bg = when (kind) {
        PixiCapabilityKind.Live -> PixiAccentMuted.copy(alpha = 0.35f)
        PixiCapabilityKind.NeedsPack -> PixiTextMuted.copy(alpha = 0.28f)
        PixiCapabilityKind.ExportOnly -> PixiError.copy(alpha = 0.22f)
    }
    val border = when (kind) {
        PixiCapabilityKind.Live -> PixiAccentMuted
        PixiCapabilityKind.NeedsPack -> PixiTextMuted
        PixiCapabilityKind.ExportOnly -> PixiError.copy(alpha = 0.65f)
    }
    Text(
        text = label,
        color = PixiText,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag(testTag)
            .semantics { contentDescription = label }
    )
}
