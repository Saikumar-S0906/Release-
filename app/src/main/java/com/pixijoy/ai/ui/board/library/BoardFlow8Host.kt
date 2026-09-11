package com.pixijoy.ai.ui.board.library

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Filter
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardHeroImage
import com.pixijoy.ai.ui.board.components.BoardLottie
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardMotionKey
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardSecondaryButton
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.components.rememberBoardHaptics
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Flow 8 session — caption / publish fields across screens. */
internal object BoardFlow8Session {
    var overlayText: String = "Good Vibes"
    var filterIndex: Int = 0
    var title: String = "Good Vibes with My Buddy"
    var description: String = "#PixiJoy #Pets #Happiness"
    var publicGallery: Boolean = true
    var allowRemix: Boolean = true
    var shareCaption: String =
        "Check out my creation made with PixiJoy! ✨ #PixiJoy #AIArt #GoodVibe"

    fun heroRes(): Int = when (filterIndex % 5) {
        1 -> BoardFlowArt.baAfter
        2 -> BoardFlowArt.beautifulHero
        3 -> BoardFlowArt.funnyHero
        4 -> BoardFlowArt.stickerHero
        else -> BoardFlowArt.creationThumb
    }
}

private fun saveDrawableToGallery(context: Context, @DrawableRes resId: Int): Boolean =
    BoardDrawableIo.saveToGallery(context, resId, filePrefix = "PixiJoy_Share")

private fun shareDrawable(context: Context, @DrawableRes resId: Int, caption: String) {
    BoardDrawableIo.shareAsync(
        context = context,
        resId = resId,
        text = caption,
        filePrefix = "pixijoy_share"
    )
}


/**
 * Flow 8 — Save, Share & Community.
 * Nav chrome: keep production Home · Create · Discover · My Stuff.
 * Board's extra Community / center + map to Discover feed / Create input.
 */
@Composable
fun BoardFlow8Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val pipeline = LocalBoardPipeline.current
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    when (screenId) {
        "b8_hub", "b8_ready" -> CreationReady(
            onSave = { onNavigate("b8_saved") },
            onShare = { onNavigate("b8_share") },
            onPublish = { onNavigate("b8_publish") },
            onPreview = { onNavigate("b8_preview") },
            onCreateAnother = pipeline.openMediaInput
        )
        "b8_preview" -> PreviewEdit(
            onNext = { onNavigate("b8_ready") },
            onBack = { onNavigate("b8_ready") }
        )
        "b8_saved" -> SavedConfirm(
            onViewGallery = { onNavigate("b8_manage") },
            onShare = { onNavigate("b8_share") },
            onPublish = { onNavigate("b8_publish") },
            onDone = onBack
        )
        "b8_share" -> ShareSheet(
            onPublish = { onNavigate("b8_publish") },
            onBack = { onNavigate("b8_ready") }
        )
        "b8_publish" -> PublishCommunity(
            onPublish = { onNavigate("b8_live") },
            onBack = { onNavigate("b8_ready") }
        )
        "b8_live" -> LivePost(
            onCommunity = { onNavigate("b27_feed") },
            onShareAgain = { onNavigate("b8_share") },
            onManage = { onNavigate("b8_manage") }
        )
        "b8_manage" -> ManageCreations(
            onTab = tab,
            onCreate = pipeline.openMediaInput,
            onOpen = { onNavigate("b8_ready") },
            onCommunity = { onNavigate("b27_feed") }
        )
        else -> CreationReady(
            onSave = { onNavigate("b8_saved") },
            onShare = { onNavigate("b8_share") },
            onPublish = { onNavigate("b8_publish") },
            onPreview = { onNavigate("b8_preview") },
            onCreateAnother = pipeline.openMediaInput
        )
    }
}

@Composable
private fun CreationReady(
    onSave: () -> Unit,
    onShare: () -> Unit,
    onPublish: () -> Unit,
    onPreview: () -> Unit,
    onCreateAnother: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    val hero = BoardFlow8Session.heroRes()
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW8_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.flow8_creation_ready),
                style = BoardType.title,
                color = colors.textPrimary
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card))
                    .clickable(onClick = onPreview)
            ) {
                Image(
                    painter = painterResource(hero),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                BoardFlowArt.libraryThumbs.forEachIndexed { i, res ->
                    Image(
                        painter = painterResource(res),
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(radii.md))
                            .border(
                                width = if (i == BoardFlow8Session.filterIndex % BoardFlowArt.libraryThumbs.size) 2.dp else 0.dp,
                                color = colors.primary,
                                shape = RoundedCornerShape(radii.md)
                            )
                            .clickable { BoardFlow8Session.filterIndex = i },
                        contentScale = ContentScale.Crop
                    )
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f5_save_gallery),
                onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            saveDrawableToGallery(context, hero)
                        }
                        Toast.makeText(
                            context,
                            context.getString(
                                if (ok) R.string.board_f8_saved_toast else R.string.board_f8_save_failed
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                        if (ok) onSave()
                    }
                }
            )
            BoardOutlinedButton(
                text = stringResource(R.string.share),
                onClick = onShare
            )
            BoardOutlinedButton(
                text = stringResource(R.string.flow8_publish),
                onClick = onPublish
            )
            TextButton(onClick = onCreateAnother) {
                Text(stringResource(R.string.result_create_another), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun PreviewEdit(onNext: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var tool by remember { mutableIntStateOf(2) }
    var filter by remember { mutableIntStateOf(BoardFlow8Session.filterIndex) }
    var caption by remember { mutableStateOf(BoardFlow8Session.overlayText) }
    val filters = listOf(
        R.string.board_f5_chip_original to BoardFlowArt.creationThumb,
        R.string.flow5_vivid to BoardFlowArt.baAfter,
        R.string.board_f8_filter_warm to BoardFlowArt.beautifulHero,
        R.string.flow5_bw to BoardFlowArt.funnyHero,
        R.string.board_f5_chip_dreamy to BoardFlowArt.stickerHero
    )
    BoardAdaptiveFrame {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), null)
                }
                Text(
                    stringResource(R.string.flow8_preview_edit),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    color = colors.textPrimary
                )
                TextButton(onClick = {
                    BoardFlow8Session.overlayText = caption
                    BoardFlow8Session.filterIndex = filter
                    onNext()
                }) {
                    Text(stringResource(R.string.board_f6_next), color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = PixiSpacing.xl)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                Image(
                    painter = painterResource(filters[filter].second),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Text(
                    "$caption ❤️",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(PixiSpacing.lg),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(Color.Black, blurRadius = 8f),
                        textAlign = TextAlign.Center
                    )
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixiSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        R.drawable.pj_ic_crop to R.string.board_f8_tool_crop,
                        R.drawable.pj_ic_filter to R.string.board_f8_tool_filter,
                        R.drawable.pj_ic_text to R.string.board_f8_tool_text,
                        R.drawable.pj_ic_sticker to R.string.board_f8_tool_sticker,
                        R.drawable.pj_ic_tune to R.string.board_f8_tool_adjust
                    ).forEachIndexed { i, (icon, label) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { tool = i }
                        ) {
                            Icon(painter = painterResource(icon),
                                null,
                                tint = if (i == tool) colors.primary else colors.textSecondary
                            )
                            Text(
                                stringResource(label),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (i == tool) colors.primary else colors.textSecondary
                            )
                        }
                    }
                }
                if (tool == 2) {
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.board_f8_overlay_text)) },
                        singleLine = true,
                        shape = RoundedCornerShape(radii.md)
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    filters.forEachIndexed { i, (label, image) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { filter = i }
                        ) {
                            Image(
                                painter = painterResource(image),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(radii.md))
                                    .border(
                                        width = if (i == filter) 2.dp else 0.dp,
                                        color = colors.primary,
                                        shape = RoundedCornerShape(radii.md)
                                    ),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                stringResource(label),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (i == filter) colors.primary else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedConfirm(
    onViewGallery: () -> Unit,
    onShare: () -> Unit,
    onPublish: () -> Unit,
    onDone: () -> Unit
) {
    val colors = BoardTheme.colors
    val haptics = rememberBoardHaptics()
    LaunchedEffect(Unit) { haptics.success() }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BoardLottie(key = BoardMotionKey.ShareSuccess, size = 88.dp)
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Text(
                stringResource(R.string.flow8_saved_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            Text(
                stringResource(R.string.flow8_saved_body),
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            BoardHeroImage(resId = BoardFlow8Session.heroRes(), aspectRatio = 1f)
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            BoardPrimaryButton(
                text = stringResource(R.string.flow8_view_gallery),
                onClick = onViewGallery
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardPrimaryButton(
                text = stringResource(R.string.flow8_share_now),
                onClick = onShare
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardOutlinedButton(
                text = stringResource(R.string.flow8_publish),
                onClick = onPublish
            )
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.board_f8_done), color = colors.primary)
            }
        }
    }
}

@Composable
private fun ShareSheet(onPublish: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var caption by remember { mutableStateOf(BoardFlow8Session.shareCaption) }
    val hero = BoardFlow8Session.heroRes()
    val targets = listOf(
        "WhatsApp", "Instagram", "Facebook",
        "Snapchat", "X", "Telegram",
        "Messages", "Email", "More"
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), null)
                }
                Text(
                    stringResource(R.string.flow8_share_to),
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            val shareTiles = (BoardFlowArt.mediaThumbs + BoardFlowArt.homeRecents).distinct()
            targets.chunked(3).forEachIndexed { rowIdx, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEachIndexed { colIdx, label ->
                        val thumb = shareTiles[(rowIdx * 3 + colIdx) % shareTiles.size]
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(radii.card))
                                .background(colors.surfaceMuted)
                                .clickable {
                                    BoardFlow8Session.shareCaption = caption
                                    shareDrawable(context, hero, caption)
                                }
                                .padding(PixiSpacing.md),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(thumb),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(PixiSpacing.xs))
                            Text(
                                label,
                                style = BoardType.caption,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(radii.md),
                trailingIcon = {
                    IconButton(onClick = {
                        val cm = context.getSystemService(ClipboardManager::class.java)
                        cm?.setPrimaryClip(ClipData.newPlainText("PixiJoy", caption))
                        Toast.makeText(
                            context,
                            context.getString(R.string.board_f8_copied),
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(painter = painterResource(R.drawable.pj_ic_copy), null, tint = colors.primary)
                    }
                }
            )
            BoardOutlinedButton(
                text = stringResource(R.string.flow8_publish),
                onClick = onPublish
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun PublishCommunity(onPublish: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var title by remember { mutableStateOf(BoardFlow8Session.title) }
    var desc by remember { mutableStateOf(BoardFlow8Session.description) }
    var publicGallery by remember { mutableStateOf(BoardFlow8Session.publicGallery) }
    var allowRemix by remember { mutableStateOf(BoardFlow8Session.allowRemix) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), null)
                }
                Column {
                    Text(
                        stringResource(R.string.flow8_publish_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        stringResource(R.string.board_f8_publish_sub),
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Box {
                BoardHeroImage(resId = BoardFlow8Session.heroRes(), aspectRatio = 16f / 9f)
                TextButton(
                    onClick = { /* cover edit stays on this screen */ },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(stringResource(R.string.flow8_cover), color = colors.primary)
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.flow8_post_title)) },
                shape = RoundedCornerShape(radii.md)
            )
            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.flow8_description)) },
                minLines = 2,
                shape = RoundedCornerShape(radii.md)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.surfaceMuted)
                    .padding(PixiSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.flow8_public_gallery), color = colors.textPrimary)
                Switch(checked = publicGallery, onCheckedChange = { publicGallery = it })
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.surfaceMuted)
                    .padding(PixiSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.flow8_allow_remix), color = colors.textPrimary)
                Switch(checked = allowRemix, onCheckedChange = { allowRemix = it })
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f8_publish_cta),
                onClick = {
                    BoardFlow8Session.title = title
                    BoardFlow8Session.description = desc
                    BoardFlow8Session.publicGallery = publicGallery
                    BoardFlow8Session.allowRemix = allowRemix
                    onPublish()
                }
            )
        }
    }
}

@Composable
private fun LivePost(
    onCommunity: () -> Unit,
    onShareAgain: () -> Unit,
    onManage: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.flow8_live_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.surfaceMuted)
                    .padding(PixiSpacing.md)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(BoardFlowArt.profileAvatar),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.size(PixiSpacing.sm))
                    Column {
                        Text(
                            stringResource(R.string.board_f8_author),
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            stringResource(R.string.board_f8_posted_ago),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(PixiSpacing.sm))
                Image(
                    painter = painterResource(BoardFlow8Session.heroRes()),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(radii.md)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(PixiSpacing.sm))
                Text(
                    BoardFlow8Session.title,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Text(
                    stringResource(R.string.board_f8_reactions_big),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            BoardPrimaryButton(
                text = stringResource(R.string.flow8_view_community),
                onClick = onCommunity
            )
            BoardOutlinedButton(
                text = stringResource(R.string.flow8_share_again),
                onClick = onShareAgain
            )
            TextButton(onClick = onManage) {
                Text(stringResource(R.string.flow8_my_creations), color = colors.primary)
            }
        }
    }
}

@Composable
private fun ManageCreations(
    onTab: (BoardShellTab) -> Unit,
    onCreate: () -> Unit,
    onOpen: () -> Unit,
    onCommunity: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var filter by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        R.string.board_f4_ideas_all,
        R.string.board_f8_tab_images,
        R.string.board_f8_tab_videos,
        R.string.board_f8_tab_favorites
    )
    val items = BoardFlowArt.libraryThumbs
    BoardMainShell(selected = BoardShellTab.MyStuff, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.flow8_my_creations),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCommunity) {
                    Icon(painter = painterResource(R.drawable.pj_ic_search), null, tint = colors.textPrimary)
                }
            }
            // Accommodate Flow 08 board's Community tab without breaking 4-tab IA.
            TextButton(onClick = onCommunity) {
                Text(stringResource(R.string.flow8_view_community), color = colors.primary)
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                tabs.forEachIndexed { i, res ->
                    val on = i == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (on) colors.primary else colors.surfaceMuted)
                            .clickable { filter = i }
                            .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(res),
                            color = if (on) colors.onPrimary else colors.textSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                val grid = listOf(null) + items
                grid.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        row.forEach { res ->
                            if (res == null) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(radii.card))
                                        .background(colors.primarySoft)
                                        .clickable(onClick = onCreate),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(painter = painterResource(R.drawable.pj_ic_add), null, tint = colors.primary)
                                        Text(
                                            stringResource(R.string.board_f8_create_new),
                                            color = colors.primary,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            } else {
                                Image(
                                    painter = painterResource(res),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(radii.card))
                                        .clickable(onClick = onOpen),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
