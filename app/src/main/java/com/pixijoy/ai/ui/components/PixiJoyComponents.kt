package com.pixijoy.ai.ui.components

import androidx.annotation.DrawableRes

import androidx.compose.ui.res.painterResource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.pixijoy.ai.R
import com.pixijoy.ai.data.SavedCreation
import com.pixijoy.ai.data.SavedCreationType
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.theme.BrandOrange
import com.pixijoy.ai.ui.theme.BrandPink
import com.pixijoy.ai.util.LocaleFormatters
import com.pixijoy.ai.ui.theme.PrimaryGradient
import com.pixijoy.ai.ui.theme.SurfaceLight
import com.pixijoy.ai.ui.theme.SurfaceVariantLight
import com.pixijoy.ai.ui.theme.TealAccent
import com.pixijoy.ai.ui.theme.TextPrimaryLight
import com.pixijoy.ai.ui.theme.TextSecondaryLight

@Composable
fun PrimaryGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null
) {
    BoardPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        aiAccent = false
    )
}

@Composable
fun CreditPill(
    credits: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val locales = LocalConfiguration.current.locales
    val locale = if (locales.size() > 0) locales[0] else java.util.Locale.getDefault()
    val formatted = LocaleFormatters.formatCredits(credits, locale)
    val creditsDesc = stringResource(R.string.cd_credits)
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("💎", fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            Text(formatted, style = MaterialTheme.typography.labelLarge, color = BrandPink, fontWeight = FontWeight.Bold)
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.semantics { contentDescription = creditsDesc },
            shape = RoundedCornerShape(20.dp),
            color = SurfaceVariantLight
        ) { content() }
    } else {
        Surface(
            modifier = modifier.semantics { contentDescription = creditsDesc },
            shape = RoundedCornerShape(20.dp),
            color = SurfaceVariantLight
        ) { content() }
    }
}

@Composable
fun StepHeader(number: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(TealAccent),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimaryLight, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun QuickToolTile(
    label: String,
    @DrawableRes icon: Int,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 88.dp, max = 120.dp),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceLight,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(icon), null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextPrimaryLight, fontWeight = FontWeight.Medium, maxLines = 2)
        }
    }
}

@Composable
fun StyleChip(
    label: String,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) TealAccent.copy(0.1f) else SurfaceVariantLight,
        modifier = Modifier.border(
            width = if (selected) 2.dp else 0.dp,
            color = if (selected) TealAccent else Color.Transparent,
            shape = RoundedCornerShape(14.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) TealAccent else TextSecondaryLight)
        }
    }
}

@Composable
fun CreationThumbnail(
    creation: SavedCreation,
    modifier: Modifier = Modifier
) {
    val emoji = when (creation.creationType) {
        SavedCreationType.MEME -> "😂"
        SavedCreationType.STICKER -> "😀"
        SavedCreationType.GIF -> "🎬"
        SavedCreationType.REEL -> "🎥"
        else -> "📸"
    }
    val gradient = when (creation.creationType) {
        SavedCreationType.MEME -> Brush.linearGradient(listOf(BrandPink, BrandOrange))
        SavedCreationType.STICKER -> Brush.linearGradient(listOf(TealAccent, Color(0xFF06B6D4)))
        else -> PrimaryGradient
    }
    Box(
        modifier = modifier.background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 32.sp)
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimaryLight,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}
