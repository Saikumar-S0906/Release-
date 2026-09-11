package com.pixijoy.ai.ui.board.create

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardAddPhotoButton
import com.pixijoy.ai.ui.board.BoardMediaSession
import com.pixijoy.ai.ui.board.BoardUseSampleButton
import com.pixijoy.ai.creative.CompetePack
import com.pixijoy.ai.creative.CompetePackResult
import com.pixijoy.ai.di.BoardAiEntryPoint
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardBeforeAfter
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardTitleText
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


internal object BoardCompeteSession {
    var pack: CompetePackResult? = null
}

@Composable
fun BoardCompetePackHost(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    when (screenId) {
        "b_pack_run" -> PackRunning(
            onDone = { onNavigate("b_pack_result") },
            onFail = { onNavigate("b_pack") }
        )
        "b_pack_result" -> PackResult(
            onBack = onBack,
            onRetry = { onNavigate("b_pack_run") }
        )
        else -> PackPhoto(
            onContinue = { onNavigate("b_pack_run") },
            onBack = onBack
        )
    }
}

@Composable
private fun PackPhoto(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    var hasPhoto by remember { mutableStateOf(BoardMediaSession.hasPhoto()) }
    BoardMainShell(selected = BoardShellTab.Create, onTab = {}) {
        BoardAdaptiveFrame {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(PixiSpacing.xl)
                    .testTag(TestTags.FLOW_PACK_HOST),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.pj_ic_back),
                            contentDescription = stringResource(R.string.cd_back),
                            tint = colors.textPrimary
                        )
                    }
                    BoardTitleText(text = stringResource(R.string.compete_pack_title))
                }
                Text(
                    stringResource(R.string.compete_pack_sub),
                    color = colors.textSecondary,
                    style = BoardType.body
                )
                if (!hasPhoto) {
                    BoardUseSampleButton(
                        sampleRes = BoardFlowArt.stickerHero,
                        onReady = { hasPhoto = true }
                    )
                    BoardAddPhotoButton(onPicked = { hasPhoto = true })
                }
                BoardPrimaryButton(
                    text = stringResource(R.string.board_f4_continue),
                    onClick = onContinue,
                    enabled = hasPhoto,
                    aiAccent = true,
                    modifier = Modifier.testTag(TestTags.TOOL_CONTINUE)
                )
            }
        }
    }
}

@Composable
private fun PackRunning(onDone: () -> Unit, onFail: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    LaunchedEffect(Unit) {
        val pack = withContext(Dispatchers.Default) { CompetePack.generate(context) }
        if (pack == null) {
            Toast.makeText(context, context.getString(R.string.board_empty_title), Toast.LENGTH_SHORT).show()
            onFail()
        } else {
            BoardCompeteSession.pack = pack
            onDone()
        }
    }
    BoardMainShell(selected = BoardShellTab.Create, onTab = {}) {
        BoardAdaptiveFrame {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PixiSpacing.xl)
                    .testTag(TestTags.FLOW_PACK_HOST),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BoardTitleText(text = stringResource(R.string.compete_pack_running))
                Spacer(modifier = Modifier.height(PixiSpacing.md))
                Text(stringResource(R.string.compete_pack_running_sub), color = colors.textSecondary, style = BoardType.body)
            }
        }
    }
}

@Composable
private fun PackResult(onBack: () -> Unit, onRetry: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    val pack = BoardCompeteSession.pack
    if (pack == null) {
        LaunchedEffect(Unit) { onRetry() }
        return
    }
    BoardMainShell(selected = BoardShellTab.Create, onTab = {}) {
        BoardAdaptiveFrame {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(PixiSpacing.xl)
                    .testTag(TestTags.FLOW_PACK_HOST),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                BoardTitleText(text = stringResource(R.string.compete_pack_title))
                Text(
                    stringResource(R.string.compete_pack_festival, pack.festivalName, pack.festivalDate),
                    style = BoardType.caption,
                    color = colors.textSecondary
                )
                BoardBeforeAfter(
                    beforeRes = BoardFlowArt.beautifulHero,
                    afterRes = BoardFlowArt.baAfter,
                    beforeBitmap = pack.original,
                    afterBitmap = pack.beauty
                )
                Text(stringResource(R.string.compete_pack_six), style = BoardType.title, color = colors.textPrimary)
                pack.looks.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        row.forEach { look ->
                            Column(modifier = Modifier.weight(1f)) {
                                if (!look.bitmap.isRecycled) {
                                    Image(
                                        bitmap = look.bitmap.asImageBitmap(),
                                        contentDescription = look.label,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(radii.card)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Text(look.label, style = BoardType.caption, color = colors.textSecondary)
                            }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
                if (pack.stickers.isNotEmpty()) {
                    Text(stringResource(R.string.compete_pack_stickers), style = BoardType.title, color = colors.textPrimary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        pack.stickers.forEach { sticker ->
                            if (!sticker.isRecycled) {
                                Image(
                                    bitmap = sticker.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .height(88.dp)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(radii.md)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                    BoardPrimaryButton(
                        text = stringResource(R.string.board_f2_save),
                        onClick = {
                            val first = pack.looks.firstOrNull()?.bitmap ?: pack.beauty
                            val ok = BoardDrawableIo.saveBitmapToGallery(
                                context,
                                first,
                                filePrefix = "PixiJoy_Pack"
                            )
                            Toast.makeText(
                                context,
                                context.getString(
                                    if (ok) R.string.board_f5_saved else R.string.board_f5_save_failed
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    BoardOutlinedButton(
                        text = stringResource(R.string.share),
                        onClick = {
                            val first = pack.looks.firstOrNull()?.bitmap ?: pack.beauty
                            BoardDrawableIo.shareBitmapAsync(
                                context = context,
                                bitmap = first,
                                fallbackRes = BoardFlowArt.stickerHero,
                                text = context.getString(R.string.compete_pack_title),
                                filePrefix = "pixijoy_pack"
                            )
                        }
                    )
                    BoardPrimaryButton(
                        text = stringResource(R.string.compete_pack_whatsapp),
                        onClick = {
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    EntryPointAccessors.fromApplication(
                                        context.applicationContext,
                                        BoardAiEntryPoint::class.java
                                    ).whatsAppStickerExporter().exportAndLaunchWhatsApp(pack.stickers)
                                }
                                Toast.makeText(
                                    context,
                                    context.getString(
                                        if (ok) R.string.compete_pack_whatsapp_ok else R.string.compete_pack_whatsapp_fail
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        aiAccent = true
                    )
                }
                if (pack.gifBytes != null) {
                    BoardOutlinedButton(
                        text = stringResource(R.string.compete_pack_talking),
                        onClick = {
                            val ok = com.pixijoy.ai.ui.board.components.BoardDrawableIo.saveBytesToGallery(
                                context,
                                pack.gifBytes!!,
                                filePrefix = "PixiJoy_Talk"
                            )
                            Toast.makeText(
                                context,
                                context.getString(
                                    if (ok) R.string.compete_pack_gif_ok else R.string.board_f5_save_failed
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
                BoardOutlinedButton(text = stringResource(R.string.cd_back), onClick = onBack)
            }
        }
    }
}
