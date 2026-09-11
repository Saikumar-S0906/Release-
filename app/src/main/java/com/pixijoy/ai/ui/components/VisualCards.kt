package com.pixijoy.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixijoy.ai.ui.theme.BrandPink
import com.pixijoy.ai.ui.theme.PrimaryGradient

data class VisualCategory(
    val emoji: String,
    val label: String,
    val subtitle: String,
    val gradient: Brush
)

@Composable
fun VisualCategoryCard(
    category: VisualCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.width(148.dp),
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        shadowElevation = 3.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .background(category.gradient),
                contentAlignment = Alignment.Center
            ) {
                Text(category.emoji, fontSize = 44.sp)
            }
            Column(Modifier.padding(12.dp)) {
                Text(category.label, fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = 1)
                Text(category.subtitle, color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
fun AiMagicChip(
    label: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(emoji, fontSize = 18.sp)
            Text(label, fontWeight = FontWeight.SemiBold, color = colors.onSurface, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun RecommendationCard(
    title: String,
    subtitle: String,
    cta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(PrimaryGradient, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
                    Text(
                        cta,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = BrandPink,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
