package com.pixijoy.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.theme.AccentPurple
import com.pixijoy.ai.ui.theme.PrimaryGradient

/** Canonical post-creation actions: Remix / Save / Share (+ optional More tools). */
@Composable
fun RemixResultBar(
    onRemix: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onMoreTools: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ResultActionChip(
                label = stringResource(R.string.result_remix),
                onClick = onRemix,
                modifier = Modifier.weight(1f)
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_sparkles), contentDescription = null, tint = AccentPurple)
            }
            ResultActionChip(
                label = stringResource(R.string.result_save),
                onClick = onSave,
                modifier = Modifier.weight(1f)
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_download), contentDescription = null, tint = colors.onSurface)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .background(PrimaryGradient, RoundedCornerShape(16.dp))
            ) {
                Surface(
                    onClick = onShare,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(R.drawable.pj_ic_share),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            stringResource(R.string.result_share),
                            modifier = Modifier.padding(start = 6.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        if (onMoreTools != null) {
            Surface(
                onClick = onMoreTools,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = colors.surfaceVariant
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.home_more_tools),
                        color = colors.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultActionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceVariant
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Text(
                label,
                modifier = Modifier.padding(start = 6.dp),
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
