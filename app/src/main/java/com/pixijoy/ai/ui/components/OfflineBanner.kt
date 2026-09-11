package com.pixijoy.ai.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.theme.PixiSpacing
import com.pixijoy.ai.ui.theme.WarningAmber

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = WarningAmber.copy(alpha = 0.16f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painter = painterResource(R.drawable.pj_ic_cloud_off),
                contentDescription = stringResource(R.string.cd_offline),
                tint = WarningAmber,
                modifier = Modifier.padding(end = PixiSpacing.sm)
            )
            Text(
                text = stringResource(R.string.offline_banner),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
