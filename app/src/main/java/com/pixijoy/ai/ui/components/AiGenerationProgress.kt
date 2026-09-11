package com.pixijoy.ai.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.theme.BrandPink
import com.pixijoy.ai.ui.theme.PrimaryGradient
import com.pixijoy.ai.ui.theme.SuccessGreen
import com.pixijoy.ai.ui.theme.SurfaceVariantLight
import com.pixijoy.ai.ui.theme.TextPrimaryLight
import com.pixijoy.ai.ui.theme.TextSecondaryLight

enum class GenerationStepStatus { Pending, Active, Done }

data class GenerationStep(
    val label: String,
    val status: GenerationStepStatus
)

@Composable
fun AiGenerationProgress(
    title: String,
    steps: List<GenerationStep>,
    progress: Float,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onCancel: (() -> Unit)? = null
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceVariantLight
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size((36 * pulse).dp)
                        .clip(CircleShape)
                        .background(PrimaryGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✨", style = MaterialTheme.typography.titleMedium)
                }
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, color = TextPrimaryLight)
                    subtitle?.let {
                        Text(it, color = TextSecondaryLight, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            steps.forEach { step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (step.status) {
                        GenerationStepStatus.Done -> {
                            Box(
                                Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painter = painterResource(R.drawable.pj_ic_check), null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                            }
                        }
                        GenerationStepStatus.Active -> {
                            Box(
                                Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(BrandPink.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("●", color = BrandPink, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        GenerationStepStatus.Pending -> {
                            Box(
                                Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE5E7EB))
                            )
                        }
                    }
                    Text(
                        step.label,
                        color = when (step.status) {
                            GenerationStepStatus.Done -> TextSecondaryLight
                            GenerationStepStatus.Active -> TextPrimaryLight
                            GenerationStepStatus.Pending -> TextSecondaryLight.copy(alpha = 0.7f)
                        },
                        fontWeight = if (step.status == GenerationStepStatus.Active) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = BrandPink,
                trackColor = Color(0xFFE5E7EB)
            )

            onCancel?.let {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = it, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.generation_cancel))
                }
            }
        }
    }
}
