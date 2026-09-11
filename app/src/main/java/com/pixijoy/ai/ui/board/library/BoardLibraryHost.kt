package com.pixijoy.ai.ui.board.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardGateScreen
import com.pixijoy.ai.ui.board.components.BoardHeroImage
import com.pixijoy.ai.ui.board.components.BoardListDetail
import com.pixijoy.ai.ui.board.components.BoardMediaPreview
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardTipCallout
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.components.BoardTrySoftEdgeGate
import com.pixijoy.ai.ui.board.components.BoardWideFrame
import com.pixijoy.ai.ui.board.components.boardGridColumns
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing

private val LibraryThumbs = BoardFlowArt.libraryThumbs

/** Wave 5 — Save/Share, Collaboration, Creations, Search. */
@Composable
fun BoardLibraryHost(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    if (BoardTrySoftEdgeGate(screenId, onBack, onNavigate)) return
    when {
        screenId.startsWith("b8_") -> BoardFlow8Host(screenId, onBack, onNavigate)
        screenId.startsWith("b_collab") || screenId == "b0_hub" -> CollabFlow(screenId, onBack, onNavigate)
        screenId.startsWith("b18_") -> BoardFlow18Host(screenId, onBack, onNavigate)
        screenId.startsWith("b26_") -> BoardFlow26Host(screenId, onBack, onNavigate)
        else -> BoardGateScreen(
            title = stringResource(R.string.board_placeholder_title),
            body = screenId,
            primary = stringResource(R.string.cd_back),
            onPrimary = onBack
        )
    }
}

@Composable
private fun CollabFlow(screenId: String, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    when (screenId) {
        "b0_hub", "b_collab_select", "b_collab" -> BoardGateScreen(
            title = stringResource(R.string.board_collab_title),
            body = stringResource(R.string.board_collab_select_body),
            primary = stringResource(R.string.board_collab_share_creation),
            onPrimary = { onNavigate("b_collab_share") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = onBack,
            tipTitle = stringResource(R.string.board_tip_title),
            tipBody = stringResource(R.string.board_collab_tip),
            heroRes = BoardFlowArt.feedThumb
        )
        "b_collab_share" -> BoardAdaptiveFrame {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PixiSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                Text(
                    stringResource(R.string.board_collab_share_title),
                    style = BoardType.title,
                    color = colors.textPrimary
                )
                BoardHeroImage(resId = BoardFlowArt.stickerHero)
                BoardPrimaryButton(
                    text = stringResource(R.string.board_collab_add_people),
                    onClick = { onNavigate("b_collab_people") }
                )
            }
        }
        "b_collab_people" -> BoardAdaptiveFrame {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PixiSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                Text(
                    stringResource(R.string.board_collab_people_title),
                    style = BoardType.title,
                    color = colors.textPrimary
                )
                listOf("Maya", "Alex", "Priya", "Jordan").forEachIndexed { i, name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(radii.md))
                            .background(colors.surfaceMuted)
                            .clickable { onNavigate("b_collab_message") }
                            .padding(PixiSpacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                    ) {
                        Image(
                            painter = painterResource(
                                BoardFlowArt.creatorFaces[i % BoardFlowArt.creatorFaces.size]
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Text(name, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                    }
                }
            }
        }
        "b_collab_message" -> {
            var message by remember { mutableStateOf("") }
            BoardAdaptiveFrame {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(PixiSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Text(
                        stringResource(R.string.board_collab_message_title),
                        style = BoardType.title,
                        color = colors.textPrimary
                    )
                    Text(
                        stringResource(R.string.board_soft_preview_title),
                        style = BoardType.caption,
                        color = colors.textSecondary
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.board_collab_message_hint)) },
                        minLines = 3,
                        shape = RoundedCornerShape(radii.md)
                    )
                    BoardPrimaryButton(
                        text = stringResource(R.string.board_f4_continue),
                        onClick = { onNavigate("b_collab_perms") }
                    )
                }
            }
        }
        "b_collab_perms" -> {
            var perm by remember { mutableIntStateOf(0) }
            val perms = listOf(
                R.string.board_collab_perm_view,
                R.string.board_collab_perm_edit,
                R.string.board_collab_perm_remix
            )
            BoardAdaptiveFrame {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(PixiSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Text(
                        stringResource(R.string.board_collab_perms_title),
                        style = BoardType.title,
                        color = colors.textPrimary
                    )
                    Text(
                        stringResource(R.string.board_demo_share),
                        style = BoardType.caption,
                        color = colors.textSecondary
                    )
                    perms.forEachIndexed { i, res ->
                        ListRow(
                            label = stringResource(res),
                            onClick = { perm = i },
                            selected = i == perm
                        )
                    }
                    BoardPrimaryButton(
                        text = stringResource(R.string.board_collab_send),
                        onClick = { onNavigate("b_collab_ready") }
                    )
                }
            }
        }
        "b_collab_ready" -> BoardGateScreen(
            title = stringResource(R.string.board_collab_ready_title),
            body = stringResource(R.string.board_collab_ready_body),
            primary = stringResource(R.string.board_collab_open_album),
            onPrimary = { onNavigate("b_collab_album") },
            secondary = stringResource(R.string.board_collab_activity),
            onSecondary = { onNavigate("b_collab_activity") }
        )
        "b_collab_album" -> {
            val columns = boardGridColumns()
            BoardWideFrame {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(PixiSpacing.xl)
                ) {
                    Text(
                        stringResource(R.string.board_collab_album_title),
                        style = BoardType.title,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(PixiSpacing.md))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        items(LibraryThumbs.size) { i ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(radii.md))
                                    .clickable { onNavigate("b_collab_together") }
                            ) {
                                BoardHeroImage(
                                    resId = LibraryThumbs[i],
                                    aspectRatio = 1f
                                )
                            }
                        }
                    }
                }
            }
        }
        "b_collab_activity" -> BoardGateScreen(
            title = stringResource(R.string.board_collab_activity),
            body = stringResource(R.string.board_collab_activity_body),
            primary = stringResource(R.string.board_collab_open_album),
            onPrimary = { onNavigate("b_collab_album") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = onBack
        )
        "b_collab_together" -> BoardGateScreen(
            title = stringResource(R.string.board_collab_together_title),
            body = stringResource(R.string.board_collab_together_body),
            primary = stringResource(R.string.board_ct_done),
            onPrimary = onBack,
            secondary = stringResource(R.string.board_f2_share),
            onSecondary = { onNavigate("b8_share") }
        )
        else -> BoardGateScreen(
            title = stringResource(R.string.board_collab_title),
            body = stringResource(R.string.board_collab_select_body),
            primary = stringResource(R.string.board_collab_share_creation),
            onPrimary = { onNavigate("b_collab_share") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = onBack
        )
    }
}

@Composable
private fun ListRow(label: String, onClick: () -> Unit, selected: Boolean = false) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radii.md))
            .background(if (selected) colors.primary.copy(0.15f) else colors.surfaceMuted)
            .clickable(onClick = onClick)
            .padding(PixiSpacing.lg)
    ) {
        Text(
            label,
            fontWeight = FontWeight.Medium,
            color = if (selected) colors.primary else colors.textPrimary
        )
    }
}
