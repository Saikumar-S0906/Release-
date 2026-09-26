package com.pixijoy.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun PixiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = text
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .semantics { this.contentDescription = contentDescription },
        colors = ButtonDefaults.buttonColors(
            containerColor = PixiAccent.copy(alpha = 0.95f),
            disabledContainerColor = PixiAccent.copy(alpha = 0.35f)
        ),
        shape = PixiPillShape
    ) {
        Text(
            text,
            style = PixiLabel,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

@Composable
fun PixiSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = PixiPillShape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PixiText),
        border = BorderStroke(1.dp, PixiBorder)
    ) {
        Text(text, style = PixiLabel, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Editorial tool tile: soft gradient glyph plate + title/subtitle type roles.
 */
@Composable
fun PixiToolTile(
    title: String,
    subtitle: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(PixiCardShape)
            .background(PixiSurface)
            .border(BorderStroke(1.dp, PixiBorder), PixiCardShape)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(14.dp)
            .height(132.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(PixiControlShape)
                .drawBehind {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                PixiAccentSoft,
                                Color(0x22FF5AA5),
                                PixiSurface2
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height)
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = PixiAccentMuted,
                modifier = Modifier.size(22.dp)
            )
        }
        Column {
            Text(
                title,
                color = PixiText,
                style = PixiLabel,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                color = PixiTextMuted,
                style = PixiHint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
