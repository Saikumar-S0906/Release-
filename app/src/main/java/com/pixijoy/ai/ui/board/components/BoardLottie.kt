package com.pixijoy.ai.ui.board.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiMotionMs

/**
 * Keys from [motion/MOTION_MANIFEST.json].
 * Drop B7 JSON into `res/raw/board_motion_*` later — API stays stable; Compose stubs play until then.
 */
enum class BoardMotionKey(val manifestId: String, val rawName: String) {
    GenerationProgress("generation_progress", "board_motion_generation_progress"),
    SparkleSuccess("sparkle_success", "board_motion_sparkle_success"),
    StickerPop("sticker_pop", "board_motion_sticker_pop"),
    BeforeAfter("before_after", "board_motion_before_after"),
    ShareSuccess("share_success", "board_motion_share_success"),
    Favorite("favorite", "board_motion_favorite"),
    Remix("remix", "board_motion_remix"),
    UploadProcessing("upload_processing", "board_motion_upload_processing"),
    TemplatePreview("template_preview", "board_motion_template_preview");

    companion object {
        fun fromManifestId(id: String): BoardMotionKey? =
            entries.firstOrNull { it.manifestId == id }
    }
}

/**
 * Manifest-backed motion slot. Prefers `R.raw.board_motion_*` when present;
 * otherwise plays a Bible-timed Compose stub. Honors [rememberBoardReduceMotion].
 */
@Composable
fun BoardLottie(
    key: BoardMotionKey,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    play: Boolean = true,
    contentDescription: String? = null,
    decorative: Boolean = false
) {
    val reduce = rememberBoardReduceMotion()
    val context = LocalContext.current
    remember(key) {
        context.resources.getIdentifier(key.rawName, "raw", context.packageName)
    }
    val a11y = when {
        decorative -> null
        else -> contentDescription ?: stringResource(R.string.board_motion_a11y, key.manifestId)
    }
    BoardMotionStub(
        key = key,
        modifier = modifier,
        size = size,
        play = play && !reduce,
        a11y = a11y
    )
}

private fun Modifier.boardMotionA11y(a11y: String?): Modifier =
    if (a11y == null) boardDecorative() else semantics { contentDescription = a11y }

@Composable
private fun BoardMotionStub(
    key: BoardMotionKey,
    modifier: Modifier,
    size: Dp,
    play: Boolean,
    a11y: String?
) {
    val colors = BoardTheme.colors
    when (key) {
        BoardMotionKey.GenerationProgress, BoardMotionKey.UploadProcessing -> {
            PopScale(play = play, modifier = modifier.size(size).boardMotionA11y(a11y)) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(colors.primarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    BoardAiSparkleIcon(modifier = Modifier.size(size * 0.45f), tint = colors.primary)
                }
            }
        }
        BoardMotionKey.SparkleSuccess -> {
            PopScale(play = play, modifier = modifier.size(size).boardMotionA11y(a11y)) {
                BoardAiSparkleIcon(modifier = Modifier.size(size * 0.55f), tint = colors.accentPink)
            }
        }
        BoardMotionKey.StickerPop, BoardMotionKey.TemplatePreview -> {
            PopScale(play = play, modifier = modifier.size(size).boardMotionA11y(a11y)) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(RoundedCornerShape(BoardTheme.radii.md))
                        .background(colors.primarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    BoardAiSparkleIcon(tint = colors.primary)
                }
            }
        }
        BoardMotionKey.ShareSuccess -> {
            PopScale(play = play, modifier = modifier.size(size).boardMotionA11y(a11y)) {
                Icon(painter = painterResource(R.drawable.pj_ic_check_circle),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(size * 0.7f)
                )
            }
        }
        BoardMotionKey.Favorite -> {
            PopScale(play = play, modifier = modifier.size(size).boardMotionA11y(a11y)) {
                Icon(painter = painterResource(R.drawable.pj_ic_heart),
                    contentDescription = null,
                    tint = colors.accentPink,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }
        BoardMotionKey.Remix -> {
            val rot = remember { Animatable(0f) }
            LaunchedEffect(play) {
                if (play) {
                    rot.snapTo(-8f)
                    rot.animateTo(0f, tween(PixiMotionMs.Component, easing = FastOutSlowInEasing))
                } else {
                    rot.snapTo(0f)
                }
            }
            Box(
                modifier = modifier
                    .size(size)
                    .graphicsLayer { rotationZ = rot.value }
                    .boardMotionA11y(a11y),
                contentAlignment = Alignment.Center
            ) {
                BoardAiSparkleIcon(tint = colors.primary)
            }
        }
        BoardMotionKey.BeforeAfter -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(colors.surfaceMuted)
                    .boardMotionA11y(a11y),
                contentAlignment = Alignment.Center
            ) {
                BoardCaptionText(text = "B/A", color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun PopScale(
    play: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scale = remember { Animatable(if (play) 0.72f else 1f) }
    LaunchedEffect(play) {
        if (play) {
            scale.snapTo(0.72f)
            scale.animateTo(1f, tween(PixiMotionMs.Normal, easing = FastOutSlowInEasing))
        } else {
            scale.snapTo(1f)
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Shared processing chrome: motion stub + determinate track + percent.
 * TalkBack: one progress widget, not a spinner plus a second bar.
 */
@Composable
fun BoardProcessingHero(
    progress: Float,
    modifier: Modifier = Modifier,
    darkSurface: Boolean = false
) {
    val colors = BoardTheme.colors
    val clamped = progress.coerceIn(0f, 1f)
    val pct = (clamped * 100).toInt()
    val label = stringResource(R.string.board_a11y_progress)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .boardProgressSemantics(clamped, label),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoardLottie(
            key = BoardMotionKey.GenerationProgress,
            size = 72.dp,
            decorative = true
        )
        BoardSoftProgress(progress = clamped, darkSurface = darkSurface)
        Text(
            "$pct%",
            color = if (darkSurface) Color.White else colors.textPrimary,
            fontWeight = FontWeight.Bold,
            style = BoardType.title
        )
    }
}
