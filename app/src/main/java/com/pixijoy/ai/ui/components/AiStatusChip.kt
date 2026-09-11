package com.pixijoy.ai.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R

enum class AiStatusKind {
    CLOUD,
    ON_DEVICE,
    OFFLINE,
    NO_CREDITS,
    NEEDS_CONSENT,
    NOT_CONFIGURED
}

@Composable
fun AiStatusChip(
    kind: AiStatusKind,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFFFF6B9D)
) {
    val label = when (kind) {
        AiStatusKind.CLOUD -> stringResource(R.string.ai_status_cloud)
        AiStatusKind.ON_DEVICE -> stringResource(R.string.ai_status_on_device)
        AiStatusKind.OFFLINE -> stringResource(R.string.ai_status_offline)
        AiStatusKind.NO_CREDITS -> stringResource(R.string.ai_status_no_credits)
        AiStatusKind.NEEDS_CONSENT -> stringResource(R.string.ai_status_needs_consent)
        AiStatusKind.NOT_CONFIGURED -> stringResource(R.string.ai_status_not_configured)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.18f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = accent,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
        )
    }
}
