package com.pixijoy.ai.ui.board.create

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
import androidx.annotation.StringRes
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
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.pixijoy.ai.di.BoardAiEntryPoint
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardAddPhotoButton
import com.pixijoy.ai.ui.board.BoardAuthReturn
import com.pixijoy.ai.ui.board.BoardFeatureFlags
import com.pixijoy.ai.ui.board.BoardMediaSession
import com.pixijoy.ai.ui.board.BoardNavPrefs
import com.pixijoy.ai.ui.board.BoardSessionPhoto
import com.pixijoy.ai.ui.board.BoardSoftGenerate
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.rememberBoardPhotoGate
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardFlowChrome
import com.pixijoy.ai.ui.board.components.BoardEdgeActions
import com.pixijoy.ai.ui.board.components.BoardEdgeKind
import com.pixijoy.ai.ui.board.components.BoardHeroImage
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPreviewControls
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardResultFooter
import com.pixijoy.ai.ui.board.components.BoardSecondaryButton
import com.pixijoy.ai.ui.board.components.BoardSoftEdgeGate
import com.pixijoy.ai.ui.board.components.BoardSoftProgress
import com.pixijoy.ai.ui.board.components.BoardSoftPulse
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.components.BoardWideFrame
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Funny meme session across Flow 6 screens. */
internal object BoardFlow6Session {
    data class Style(
        @StringRes val titleRes: Int,
        @DrawableRes val imageRes: Int,
        val category: Int // 0 all, 1 trending, 2 reactions, 3 text
    )

    val styles = listOf(
        Style(R.string.board_f6_style_classic, BoardFlowArt.memeCategoryHero, 1),
        Style(R.string.board_f6_style_funny_face, BoardFlowArt.baAfter, 2),
        Style(R.string.board_f6_style_cartoon, BoardFlowArt.differentHero, 1),
        Style(R.string.board_f6_style_animal, R.drawable.board_c00_funny_dog, 1),
        Style(R.string.board_f6_style_movie, BoardFlowArt.posterMove, 1),
        Style(R.string.board_f6_style_wfh, BoardFlowArt.memeMonday, 2),
        Style(R.string.board_f6_style_expectation, BoardFlowArt.memeGoodVibes, 2),
        Style(R.string.board_f6_style_thug, BoardFlowArt.stickerHero, 1),
        Style(R.string.board_f6_style_baby, R.drawable.board_c09_portrait, 2),
        Style(R.string.board_f6_style_faceswap, R.drawable.board_c06_faces, 1),
        Style(R.string.board_f6_style_timetravel, BoardFlowArt.trendTravel, 1),
        Style(R.string.board_f6_style_custom, BoardFlowArt.collageHero, 3)
    )

    var styleIndex: Int = 0
    var topText: String = "MONDAY ALREADY?"
    var bottomText: String = "BUT I'M JUST A GIRL WITH A DOG"
    var textColorArgb: Int = 0xFFFFFFFF.toInt()
    var stickerEmoji: String = ""
    var captionScale: Float = 1f
    var resultVariant: Int = 0
    var isCloudPowered: Boolean = false
    var lastUserMessage: String? = null
    @Volatile
    var resultBitmap: android.graphics.Bitmap? = null
        private set

    fun setResult(bitmap: android.graphics.Bitmap?) {
        val old = resultBitmap
        resultBitmap = bitmap
        if (old != null && old !== bitmap && !old.isRecycled) {
            runCatching { old.recycle() }
        }
    }

    fun style(): Style = styles.getOrElse(styleIndex) { styles.first() }

    fun resultImage(): Int = when (resultVariant % 4) {
        0 -> style().imageRes
        1 -> BoardFlowArt.funnyHero
        2 -> BoardFlowArt.rematchFunny.getOrElse(1) { BoardFlowArt.baAfter }
        else -> BoardFlowArt.memeGoodVibes
    }
}

private fun saveDrawableToGallery(context: Context, @DrawableRes resId: Int): Boolean =
    BoardDrawableIo.saveToGallery(context, resId, filePrefix = "PixiJoy_Funny")

private fun shareDrawable(context: Context, @DrawableRes resId: Int) {
    BoardDrawableIo.shareAsync(
        context = context,
        resId = resId,
        textRes = R.string.board_f6_share_text,
        filePrefix = "pixijoy_funny"
    )
}

/** Bake top/bottom captions onto user/session photo (or style fallback) and save. */
private fun bakeAndSaveFunny(context: Context): Boolean {
    val app = context.applicationContext
    val source = BoardMediaSession.decodeBitmap(app)
        ?: runCatching {
            BitmapFactory.decodeResource(app.resources, BoardFlow6Session.resultImage())
        }.getOrNull()
        ?: return saveDrawableToGallery(context, BoardFlow6Session.resultImage())
    val baked = runCatching {
        EntryPointAccessors.fromApplication(app, BoardAiEntryPoint::class.java)
            .createExtraPipeline()
            .bakeMemeCaptions(
                source,
                BoardFlow6Session.topText,
                BoardFlow6Session.bottomText +
                    if (BoardFlow6Session.stickerEmoji.isNotBlank()) {
                        " ${BoardFlow6Session.stickerEmoji}"
                    } else {
                        ""
                    },
                BoardFlow6Session.textColorArgb,
                BoardFlow6Session.captionScale
            )
    }.getOrDefault(source)
    return BoardDrawableIo.saveBitmapToGallery(context, baked, filePrefix = "PixiJoy_Funny")
}


/** Flow 6 — Make it Funny. Board: REMATCH_06_Make_it_Funny.png */
@Composable
fun BoardFlow6Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val pipeline = LocalBoardPipeline.current
    when (screenId) {
        "b6_choose", "b6_funny" -> StylePick(
            onPick = {
                if (BoardFlow6Session.style().titleRes == R.string.board_f6_style_faceswap) {
                    onNavigate("b_c06_photo")
                } else {
                    onNavigate("b6_customize")
                }
            },
            onBack = onBack,
            onSwitchIntent = { onNavigate(it) }
        )
        "b6_customize" -> Customize(
            onNext = { onNavigate("b6_processing") },
            onBack = { onNavigate("b6_choose") }
        )
        "b6_processing" -> Processing(
            onDone = { onNavigate("b6_result") },
            onCancel = onBack,
            onOffline = { onNavigate("b6_offline") },
            onLowCredits = { onNavigate("b6_low_credits") },
            onSafety = { onNavigate("b6_safety") }
        )
        "b6_result" -> FunnyResult(
            onVariations = { onNavigate("b6_variations") },
            onShare = { onNavigate("b8_share") },
            onRemake = { onNavigate("b6_customize") },
            onEdit = { onNavigate("b15_open") },
            onExport = pipeline.openExport,
            onPublish = {
                BoardAuthReturn.remember("b6_result")
                onNavigate("b8_publish")
            },
            onBack = { onNavigate("b6_customize") },
            onClose = onBack
        )
        "b6_variations" -> Variations(
            onPick = { onNavigate("b6_result") },
            onBack = { onNavigate("b6_result") }
        )
        "b6_share" -> ShareReady(
            onCreateVideo = { onNavigate("b14_input") },
            onStickers = { onNavigate("b13_photo") },
            onPublish = {
                BoardAuthReturn.remember("b6_share")
                onNavigate("b8_publish")
            },
            onShareFlow = { onNavigate("b8_share") },
            onAnotherPhoto = pipeline.openMediaInput,
            onHome = onBack
        )
        "b6_offline" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Offline,
            onPrimary = { onNavigate("b6_processing") },
            onSecondary = { onNavigate("b6_customize") }
        )
        "b6_low_credits" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.LowCredits,
            onPrimary = { onNavigate("b23_dash") },
            onSecondary = { onNavigate("b6_customize") }
        )
        "b6_safety" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Safety,
            onPrimary = { onNavigate("b6_choose") },
            onSecondary = onBack
        )
        else -> StylePick(
            onPick = {
                if (BoardFlow6Session.style().titleRes == R.string.board_f6_style_faceswap) {
                    onNavigate("b_c06_photo")
                } else {
                    onNavigate("b6_customize")
                }
            },
            onBack = onBack,
            onSwitchIntent = { onNavigate(it) }
        )
    }
}

@Composable
private fun StylePick(
    onPick: () -> Unit,
    onBack: () -> Unit,
    onSwitchIntent: (String) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(BoardFlow6Session.styleIndex) }
    var hasPhoto by remember { mutableStateOf(BoardMediaSession.hasPhoto()) }
    val continueWithPhoto = rememberBoardPhotoGate {
        hasPhoto = true
        BoardFlow6Session.styleIndex = selected
        onPick()
    }
    val tabs = listOf(
        R.string.board_f4_ideas_all,
        R.string.board_f6_tab_trending,
        R.string.board_f6_tab_reactions,
        R.string.board_f6_tab_text
    )
    val filtered = BoardFlow6Session.styles.mapIndexed { i, s -> i to s }.filter { (_, s) ->
        val catOk = tab == 0 || s.category == tab
        if (!catOk) return@filter false
        val q = query.trim()
        if (q.isEmpty()) true
        else stringResource(s.titleRes).contains(q, ignoreCase = true)
    }

    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW6_FUNNY)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = null)
                }
                Text(
                    stringResource(R.string.make_it_funny),
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(painter = painterResource(R.drawable.pj_ic_search), null, tint = colors.textSecondary)
            }
            if (!hasPhoto) {
                com.pixijoy.ai.ui.board.BoardUseSampleButton(
                    sampleRes = BoardFlowArt.funnyHero,
                    onReady = { hasPhoto = true }
                )
                BoardAddPhotoButton(onPicked = { hasPhoto = true })
                Spacer(modifier = Modifier.height(PixiSpacing.sm))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(radii.card))
                ) {
                    BoardSessionPhoto(fallbackRes = BoardFlowArt.funnyHero)
                }
                Spacer(modifier = Modifier.height(PixiSpacing.sm))
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.board_f6_search)) },
                singleLine = true,
                shape = RoundedCornerShape(radii.pill)
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                tabs.forEachIndexed { i, res ->
                    val on = i == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (on) colors.accentYellow else colors.surfaceMuted)
                            .clickable { tab = i }
                            .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(res),
                            color = if (on) Color.Black else colors.textSecondary,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal
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
                filtered.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        row.forEach { (index, style) ->
                            val on = index == selected
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(radii.card))
                                    .border(
                                        width = if (on) 2.dp else 0.dp,
                                        color = colors.accentYellow,
                                        shape = RoundedCornerShape(radii.card)
                                    )
                                    .clickable {
                                        selected = index
                                        BoardFlow6Session.styleIndex = index
                                    }
                            ) {
                                Image(
                                    painter = painterResource(style.imageRes),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(radii.card)),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    stringResource(style.titleRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(4.dp),
                                    maxLines = 2
                                )
                            }
                        }
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardPrimaryButton(
                text = stringResource(R.string.board_f4_continue),
                onClick = continueWithPhoto
            )
            Text(
                stringResource(R.string.board_f5_switch_intent),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = PixiSpacing.sm)
            )
            TextButton(onClick = { onSwitchIntent("b5_choose") }) {
                Text(stringResource(R.string.make_it_beautiful), color = colors.primary)
            }
        }
    }
}

@Composable
private fun Customize(onNext: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var top by remember { mutableStateOf(BoardFlow6Session.topText) }
    var bottom by remember { mutableStateOf(BoardFlow6Session.bottomText) }
    var tool by remember { mutableIntStateOf(0) }
    var colorIndex by remember { mutableIntStateOf(0) }
    var stickerEmoji by remember { mutableStateOf(BoardFlow6Session.stickerEmoji) }
    var captionScale by remember { mutableFloatStateOf(BoardFlow6Session.captionScale) }
    val emojiPack = listOf("👑", "😎", "😂", "🔥", "✨", "💯", "🐶", "🥳")
    val swatches = listOf(
        Color.White,
        Color(0xFFFFEB3B),
        Color(0xFFFF4081),
        Color(0xFF40C4FF),
        Color(0xFF69F0AE),
        Color.Black
    )
    val memeColor = swatches[colorIndex]
    BoardFlow6Session.textColorArgb = android.graphics.Color.argb(
        (memeColor.alpha * 255).toInt(),
        (memeColor.red * 255).toInt(),
        (memeColor.green * 255).toInt(),
        (memeColor.blue * 255).toInt()
    )
    val previewFont = (22f * captionScale).sp

    BoardAdaptiveFrame {
        BoardFlowChrome(
            title = stringResource(R.string.board_c6_customize_title),
            onBack = onBack,
            primaryLabel = stringResource(R.string.board_f6_next),
            onPrimary = {
                BoardFlow6Session.topText = top
                BoardFlow6Session.bottomText = bottom
                BoardFlow6Session.stickerEmoji = stickerEmoji
                BoardFlow6Session.captionScale = captionScale
                onNext()
            },
            scrollable = false
        ) {
            BoardWideFrame(modifier = Modifier.weight(1f)) {
                BoardPreviewControls(
                    preview = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(radii.card))
                        ) {
                            Image(
                                painter = painterResource(BoardFlow6Session.style().imageRes),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                top.uppercase(),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(PixiSpacing.md),
                                style = TextStyle(
                                    color = memeColor,
                                    fontSize = previewFont,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    shadow = Shadow(Color.Black, blurRadius = 6f)
                                )
                            )
                            Text(
                                bottom.uppercase(),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(PixiSpacing.md),
                                style = TextStyle(
                                    color = memeColor,
                                    fontSize = (18f * captionScale).sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    shadow = Shadow(Color.Black, blurRadius = 6f)
                                )
                            )
                            if (stickerEmoji.isNotBlank()) {
                                Text(
                                    stickerEmoji,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(PixiSpacing.md),
                                    fontSize = (28f * captionScale).sp
                                )
                            }
                        }
                    },
                    controls = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(
                                R.drawable.pj_ic_text to R.string.board_f6_tool_text,
                                R.drawable.pj_ic_font to R.string.board_f6_tool_font,
                                R.drawable.pj_ic_palette to R.string.board_f6_tool_color,
                                R.drawable.pj_ic_sticker to R.string.board_f6_tool_sticker,
                                R.drawable.pj_ic_emoji to R.string.board_f6_tool_resize
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
                        Spacer(modifier = Modifier.height(PixiSpacing.sm))
                        when (tool) {
                            0, 1 -> {
                                OutlinedTextField(
                                    value = top,
                                    onValueChange = { top = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(stringResource(R.string.board_f6_top_text)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(radii.md)
                                )
                                Spacer(modifier = Modifier.height(PixiSpacing.sm))
                                OutlinedTextField(
                                    value = bottom,
                                    onValueChange = { bottom = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(stringResource(R.string.board_f6_bottom_text)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(radii.md)
                                )
                            }
                            2 -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                                    swatches.forEachIndexed { i, c ->
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                                .border(
                                                    width = if (i == colorIndex) 3.dp else 1.dp,
                                                    color = if (i == colorIndex) colors.primary else colors.surfaceMuted,
                                                    shape = CircleShape
                                                )
                                                .clickable { colorIndex = i }
                                        )
                                    }
                                }
                            }
                            3 -> {
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                                ) {
                                    emojiPack.forEach { emoji ->
                                        Text(
                                            emoji,
                                            fontSize = 28.sp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(radii.md))
                                                .background(
                                                    if (stickerEmoji.contains(emoji)) colors.primarySoft
                                                    else colors.surfaceMuted
                                                )
                                                .clickable {
                                                    stickerEmoji = if (stickerEmoji.contains(emoji)) {
                                                        stickerEmoji.replace(emoji, "")
                                                    } else {
                                                        stickerEmoji + emoji
                                                    }
                                                }
                                                .padding(PixiSpacing.sm)
                                        )
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    stringResource(R.string.board_f6_tool_resize) +
                                        " · ${(captionScale * 100).toInt()}%",
                                    color = colors.textSecondary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Slider(
                                    value = captionScale,
                                    onValueChange = { captionScale = it },
                                    valueRange = 0.6f..1.8f
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun Processing(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit,
    onSafety: () -> Unit
) {
    val colors = BoardTheme.colors
    val steps = listOf(
        R.string.board_f6_step_photo,
        R.string.board_f6_step_style,
        R.string.board_f6_step_funny,
        R.string.board_f6_step_variations
    )
    var done by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    androidx.activity.compose.BackHandler(onBack = onCancel)
    LaunchedEffect(Unit) {
        val result = BoardSoftGenerate.run(context, BoardSoftGenerate.Intent.Funny)
        result.topText?.let { BoardFlow6Session.topText = it }
        result.bottomText?.let { BoardFlow6Session.bottomText = it }
        BoardFlow6Session.isCloudPowered = result.isCloudPowered
        BoardFlow6Session.lastUserMessage = result.userMessage
        result.bitmap?.let { BoardFlow6Session.setResult(it) }
        steps.indices.forEach { i ->
            done = i + 1
        }
        onDone()
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.splashTop)
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoardSoftPulse {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(colors.accentYellow),
                    contentAlignment = Alignment.Center
                ) {
                    Text("😂", fontSize = 40.sp)
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Text(
                stringResource(R.string.board_f6_processing_title),
                fontWeight = FontWeight.Bold,
                style = BoardType.title,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            steps.forEachIndexed { i, res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = PixiSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    when {
                        i < done -> Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success)
                        i == done -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colors.accentYellow
                        )
                        else -> Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color.White.copy(0.35f), CircleShape)
                        )
                    }
                    Text(
                        stringResource(res),
                        color = if (i <= done) Color.White else Color.White.copy(0.45f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            BoardSoftProgress(
                progress = done / steps.size.toFloat(),
                darkSurface = true
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            BoardEdgeActions(
                onOffline = onOffline,
                onLowCredits = onLowCredits,
                onSafety = onSafety,
                darkSurface = true
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_f19_cancel),
                onClick = onCancel
            )
        }
    }
}

@Composable
private fun FunnyResult(
    onVariations: () -> Unit,
    onShare: () -> Unit,
    onRemake: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onPublish: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val scope = rememberCoroutineScope()
    val image = BoardFlow6Session.resultImage()
    val captionColor = Color(BoardFlow6Session.textColorArgb)
    androidx.activity.compose.BackHandler(onBack = onBack)
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_back),
                        contentDescription = stringResource(R.string.cd_back),
                        tint = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_close),
                        contentDescription = stringResource(R.string.close),
                        tint = colors.textPrimary
                    )
                }
            }
            Icon(painter = painterResource(R.drawable.pj_ic_check_circle),
                null,
                tint = colors.success,
                modifier = Modifier.size(48.dp)
            )
            Text(
                stringResource(R.string.board_f6_result_title),
                fontWeight = FontWeight.Bold,
                style = BoardType.title,
                color = colors.textPrimary
            )
            if (BoardFlow6Session.isCloudPowered) {
                Text(
                    stringResource(R.string.powered_by_gemini),
                    style = BoardType.caption,
                    color = colors.primary
                )
            } else if (BoardFeatureFlags.showDemoCaption) {
                Text(
                    stringResource(R.string.board_soft_preview_title),
                    style = BoardType.caption,
                    color = colors.textSecondary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(BoardTheme.radii.card))
            ) {
                val baked = BoardFlow6Session.resultBitmap
                if (baked != null && !baked.isRecycled) {
                    com.pixijoy.ai.ui.board.BoardSafeResultImage(
                        bitmap = baked,
                        fallbackRes = image,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    BoardSessionPhoto(fallbackRes = image)
                    Text(
                        BoardFlow6Session.topText.uppercase(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(PixiSpacing.md),
                        style = TextStyle(
                            color = captionColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            shadow = Shadow(Color.Black, blurRadius = 6f)
                        )
                    )
                    Text(
                        BoardFlow6Session.bottomText.uppercase(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(PixiSpacing.md),
                        style = TextStyle(
                            color = captionColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            shadow = Shadow(Color.Black, blurRadius = 6f)
                        )
                    )
                }
            }
            BoardResultFooter(
                onSave = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            bakeAndSaveFunny(context)
                        }
                        Toast.makeText(
                            context,
                            context.getString(
                                if (ok) R.string.board_f6_saved else R.string.board_f6_save_failed
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                        if (ok) BoardNavPrefs.markUserCreations(context)
                    }
                },
                onShare = {
                    scope.launch {
                        withContext(Dispatchers.IO) { bakeAndSaveFunny(context) }
                        shareDrawable(context, image)
                        onShare()
                    }
                },
                onEdit = onEdit,
                onExport = onExport,
                onRemake = onRemake,
                onPublish = onPublish
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_f6_more_variations),
                onClick = onVariations
            )
        }
    }
}

@Composable
private fun Variations(onPick: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val captions = listOf(
        R.string.board_f6_var_mood,
        R.string.board_f6_var_deal,
        R.string.board_f6_var_dogs,
        R.string.board_f6_var_monday,
        R.string.board_f6_var_vibe,
        R.string.board_f6_var_oops
    )
    var images by remember {
        mutableStateOf(
            (BoardFlowArt.rematchFunny + BoardFlowArt.memeTemplates).distinct().take(6)
        )
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.board_f6_more_variations),
                fontWeight = FontWeight.Bold,
                style = BoardType.title,
                color = colors.textPrimary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                images.chunked(2).forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        row.forEachIndexed { col, resId ->
                            val index = rowIndex * 2 + col
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(radii.card))
                                    .clickable {
                                        BoardFlow6Session.resultVariant = index
                                        onPick()
                                    }
                            ) {
                                Image(
                                    painter = painterResource(resId),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    stringResource(captions.getOrElse(index) { R.string.board_f6_var_mood }),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(PixiSpacing.sm),
                                    style = TextStyle(
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        shadow = Shadow(Color.Black, blurRadius = 4f)
                                    )
                                )
                            }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f5_generate_more),
                onClick = {
                    images = (BoardFlowArt.rematchFunny + BoardFlowArt.memeTemplates + BoardFlowArt.mediaThumbs)
                        .distinct()
                        .shuffled()
                        .take(6)
                }
            )
            BoardSecondaryButton(text = stringResource(R.string.cd_back), onClick = onBack)
        }
    }
}

@Composable
private fun ShareReady(
    onCreateVideo: () -> Unit,
    onStickers: () -> Unit,
    onPublish: () -> Unit,
    onShareFlow: () -> Unit,
    onAnotherPhoto: () -> Unit,
    onHome: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val scope = rememberCoroutineScope()
    val image = BoardFlow6Session.resultImage()
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.board_f6_share_title),
                fontWeight = FontWeight.Bold,
                style = BoardType.title,
                color = colors.textPrimary
            )
            BoardHeroImage(resId = image, aspectRatio = 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("WhatsApp", "Instagram", "Snapchat", "More").forEach { label ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { shareDrawable(context, image) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceMuted),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label.take(1), fontWeight = FontWeight.Bold, color = colors.primary)
                        }
                        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f5_save_gallery),
                onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            bakeAndSaveFunny(context)
                        }
                        Toast.makeText(
                            context,
                            context.getString(
                                if (ok) R.string.board_f6_saved else R.string.board_f6_save_failed
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            BoardOutlinedButton(
                text = stringResource(R.string.flow8_share_now),
                onClick = onShareFlow
            )
            BoardOutlinedButton(
                text = stringResource(R.string.flow8_publish),
                onClick = onPublish
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_f6_create_video),
                onClick = onCreateVideo
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_f6_make_stickers),
                onClick = onStickers
            )
            BoardSecondaryButton(
                text = stringResource(R.string.board_f6_another_photo),
                onClick = onAnotherPhoto
            )
            TextButton(onClick = onHome) {
                Text(stringResource(R.string.board_f6_back_home), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}
