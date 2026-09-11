package com.pixijoy.ai.ui.board.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.theme.PixiSpacing

/**
 * Locked result spine: WhatsApp · Status · Save first, then Edit · Export · Remake · Publish.
 * Bible: 48dp targets + nav-bar clearance.
 */
@Composable
fun BoardResultFooter(
    onSave: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onRemake: () -> Unit,
    onPublish: () -> Unit,
    modifier: Modifier = Modifier,
    primarySaveShare: Boolean = true,
    onStickers: (() -> Unit)? = null,
    onWhatsApp: (() -> Unit)? = null,
    onStatus: (() -> Unit)? = null
) {
    val haptics = rememberBoardHaptics()
    val whatsApp = onWhatsApp
    val status = onStatus
    val shareFirst = whatsApp != null
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
    ) {
        if (whatsApp != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                BoardPrimaryButton(
                    text = stringResource(R.string.board_f17_wa),
                    onClick = whatsApp,
                    modifier = Modifier.weight(1f)
                )
                if (status != null) {
                    BoardOutlinedButton(
                        text = stringResource(R.string.board_f17_status),
                        onClick = status,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            BoardOutlinedButton(
                text = stringResource(R.string.board_f2_save),
                onClick = {
                    haptics.success()
                    onSave()
                }
            )
        } else if (primarySaveShare) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                BoardOutlinedButton(
                    text = stringResource(R.string.board_f2_save),
                    onClick = {
                        haptics.success()
                        onSave()
                    },
                    modifier = Modifier.weight(1f)
                )
                BoardPrimaryButton(
                    text = stringResource(R.string.share),
                    onClick = onShare,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            if (!primarySaveShare && !shareFirst) {
                BoardOutlinedButton(
                    text = stringResource(R.string.board_f2_save),
                    onClick = {
                        haptics.success()
                        onSave()
                    },
                    modifier = Modifier.weight(1f)
                )
                BoardOutlinedButton(
                    text = stringResource(R.string.share),
                    onClick = onShare,
                    modifier = Modifier.weight(1f)
                )
            } else {
                if (shareFirst) {
                    BoardOutlinedButton(
                        text = stringResource(R.string.share),
                        onClick = onShare,
                        modifier = Modifier.weight(1f)
                    )
                }
                BoardOutlinedButton(
                    text = stringResource(R.string.flow19_edit_more),
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                )
                BoardOutlinedButton(
                    text = stringResource(R.string.export),
                    onClick = onExport,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (!primarySaveShare && !shareFirst) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                BoardOutlinedButton(
                    text = stringResource(R.string.flow19_edit_more),
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                )
                BoardOutlinedButton(
                    text = stringResource(R.string.export),
                    onClick = onExport,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            if (onStickers != null) {
                BoardOutlinedButton(
                    text = stringResource(R.string.create_stickers),
                    onClick = onStickers,
                    modifier = Modifier.weight(1f)
                )
            }
            BoardOutlinedButton(
                text = stringResource(R.string.board_result_remake),
                onClick = onRemake,
                modifier = Modifier.weight(1f)
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_result_publish),
                onClick = onPublish,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
