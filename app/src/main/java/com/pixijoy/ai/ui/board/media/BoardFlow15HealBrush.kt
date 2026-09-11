package com.pixijoy.ai.ui.board.media

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.di.BoardAiEntryPoint
import com.pixijoy.ai.ui.board.BoardEditorDocument
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardLottie
import com.pixijoy.ai.ui.board.components.BoardMotionKey
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.theme.PixiSpacing
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Flow 15 heal / selective brush — real inpaint + dodge/burn on [BoardEditorDocument].
 */
@Composable
internal fun BoardFlow15HealBrush(onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    var rev by remember { mutableIntStateOf(BoardEditorDocument.revision) }
    var mode by remember { mutableIntStateOf(0) } // 0 heal, 1 dodge, 2 burn
    var brush by remember { mutableFloatStateOf(0.08f) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        BoardEditorDocument.ensureLoaded(context)
        rev = BoardEditorDocument.revision
    }
    val preview = remember(rev, BoardEditorDocument.workingBitmap) {
        BoardEditorDocument.workingBitmap?.takeIf { !it.isRecycled }?.copy(Bitmap.Config.ARGB_8888, false)?.asImageBitmap()
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(com.pixijoy.ai.testing.TestTags.FLOW15_HEAL),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f15_heal_title),
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(onClick = onDone) {
                    Text(stringResource(R.string.flow15_save), color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                stringResource(R.string.board_f15_heal_tip),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.surfaceMuted)
            ) {
                if (preview != null) {
                    Image(
                        bitmap = preview,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(mode, brush, busy, rev) {
                                detectTapGestures { offset ->
                                    if (busy) return@detectTapGestures
                                    val src = BoardEditorDocument.workingBitmap ?: return@detectTapGestures
                                    if (src.isRecycled) return@detectTapGestures
                                    val nx = (offset.x / size.width).coerceIn(0f, 1f)
                                    val ny = (offset.y / size.height).coerceIn(0f, 1f)
                                    busy = true
                                    scope.launch {
                                        try {
                                            val pipe = EntryPointAccessors.fromApplication(
                                                context.applicationContext,
                                                BoardAiEntryPoint::class.java
                                            ).createExtraPipeline()
                                            val out = withContext(Dispatchers.Default) {
                                                when (mode) {
                                                    1 -> pipe.selectiveAt(src, nx, ny, brush, amount = 0.45f)
                                                    2 -> pipe.selectiveAt(src, nx, ny, brush, amount = -0.45f)
                                                    else -> pipe.healAt(src, nx, ny, brush)
                                                }
                                            }
                                            if (out !== src) {
                                                BoardEditorDocument.setWorkingBitmap(out, push = true)
                                            }
                                            rev = BoardEditorDocument.revision
                                        } finally {
                                            busy = false
                                        }
                                    }
                                }
                            },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(BoardFlowArt.editorHero),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                if (busy) {
                    BoardLottie(
                        key = BoardMotionKey.GenerationProgress,
                        modifier = Modifier.align(Alignment.Center),
                        size = 72.dp
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                listOf(
                    0 to R.string.board_f15_heal_mode,
                    1 to R.string.board_f15_dodge_mode,
                    2 to R.string.board_f15_burn_mode
                ).forEach { (i, label) ->
                    Text(
                        stringResource(label),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (mode == i) colors.primary else colors.surfaceMuted)
                            .clickable { mode = i }
                            .padding(vertical = PixiSpacing.sm),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = if (mode == i) colors.onPrimary else colors.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Text(
                stringResource(R.string.board_f15_brush_size),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = brush,
                onValueChange = { brush = it },
                valueRange = 0.03f..0.2f,
                modifier = Modifier.fillMaxWidth()
            )
            BoardPrimaryButton(text = stringResource(R.string.board_f15_apply), onClick = onDone)
            Spacer(modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}
