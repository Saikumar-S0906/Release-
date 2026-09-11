package com.pixijoy.ai.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.theme.PixiSpacing
import com.pixijoy.ai.ui.theme.WarningAmber

@Composable
fun SlowNetworkBanner(modifier: Modifier = Modifier) {
    val onBg = MaterialTheme.colorScheme.onBackground
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = WarningAmber.copy(alpha = 0.16f)
    ) {
        Column(modifier = Modifier.padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.md)) {
            Text(
                text = stringResource(R.string.slow_network),
                style = MaterialTheme.typography.labelMedium,
                color = onBg,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.slow_network_desc),
                style = MaterialTheme.typography.labelSmall,
                color = onBg.copy(alpha = 0.85f),
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
fun CloudAiRequiresInternetText(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.cloud_ai_requires_internet),
        style = MaterialTheme.typography.labelSmall,
        color = WarningAmber,
        modifier = modifier
    )
}
