package com.pixijoy.ai.ui.board.components

import android.graphics.Bitmap
import android.net.Uri
import android.widget.VideoView
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.components.rememberPixiGridColumns
import com.pixijoy.ai.ui.theme.PixiMotionMs
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.delay

private val IntentTileScrim = listOf(
    Color.Transparent,
    Color.Black.copy(alpha = 0.18f),
    Color.Black.copy(alpha = 0.75f)
)

/** Hero / collage image for board screens (size-bounded decode). */
@Composable
fun BoardHeroImage(
    @DrawableRes resId: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    aspectRatio: Float = 1f
) {
    val radii = BoardTheme.radii
    BoardArtImage(
        resId = resId,
        contentDescription = contentDescription,
        maxSidePx = 1080,
        modifier = modifier
            .fillMaxWidth()
            .then(if (aspectRatio > 0f) Modifier.aspectRatio(aspectRatio) else Modifier)
            .clip(RoundedCornerShape(radii.card))
    )
}

/** K-BA — Before/After compare slider + peek motion + button alternative (Bible §28). */
@Composable
fun BoardBeforeAfter(
    @DrawableRes beforeRes: Int,
    @DrawableRes afterRes: Int,
    modifier: Modifier = Modifier,
    beforeBitmap: Bitmap? = null,
    afterBitmap: Bitmap? = null
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val reduceMotion = rememberBoardReduceMotion()
    var split by remember { mutableFloatStateOf(0.5f) }
    val animatedSplit by animateFloatAsState(
        targetValue = split,
        animationSpec = tween(
            durationMillis = if (reduceMotion) 0 else PixiMotionMs.Component,
            easing = FastOutSlowInEasing
        ),
        label = "board_ba_split"
    )
    val compareLabel = stringResource(R.string.cd_compare)
    LaunchedEffect(beforeRes, afterRes, reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        // Soft before_after peek (manifest key) — one pass, then settle mid.
        split = 0.72f
        delay(PixiMotionMs.Slow.toLong())
        split = 0.28f
        delay(PixiMotionMs.Slow.toLong())
        split = 0.5f
    }
    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(radii.card))
                .semantics(mergeDescendants = true) {
                    contentDescription = compareLabel
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        split = (split + dragAmount / w).coerceIn(0.08f, 0.92f)
                    }
                }
        ) {
            val afterSnap = remember(afterBitmap) {
                afterBitmap?.takeIf { !it.isRecycled }?.let {
                    runCatching { it.copy(Bitmap.Config.ARGB_8888, false)?.asImageBitmap() }.getOrNull()
                }
            }
            val beforeSnap = remember(beforeBitmap) {
                beforeBitmap?.takeIf { !it.isRecycled }?.let {
                    runCatching { it.copy(Bitmap.Config.ARGB_8888, false)?.asImageBitmap() }.getOrNull()
                }
            }
            if (afterSnap != null) {
                Image(
                    bitmap = afterSnap,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .boardDecorative(),
                    contentScale = ContentScale.Crop
                )
            } else {
                BoardArtImage(
                    resId = afterRes,
                    contentDescription = null,
                    maxSidePx = 1080,
                    modifier = Modifier
                        .fillMaxSize()
                        .boardDecorative()
                )
            }
            val beforeClip = Modifier
                .fillMaxSize()
                .boardDecorative()
                .drawWithContent {
                    clipRect(
                        left = 0f,
                        top = 0f,
                        right = size.width * animatedSplit,
                        bottom = size.height
                    ) {
                        this@drawWithContent.drawContent()
                    }
                }
            if (beforeSnap != null) {
                Image(
                    bitmap = beforeSnap,
                    contentDescription = null,
                    modifier = beforeClip,
                    contentScale = ContentScale.Crop
                )
            } else {
                BoardArtImage(
                    resId = beforeRes,
                    contentDescription = null,
                    maxSidePx = 1080,
                    modifier = beforeClip
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = maxWidth * animatedSplit - 1.dp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(colors.onPrimary)
                    .boardDecorative()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = maxWidth * animatedSplit - 24.dp)
                    .size(48.dp)
                    .boardDecorative(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                        .border(2.dp, colors.onPrimary, CircleShape)
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(PixiSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.board_ba_before),
                    color = colors.onPrimary,
                    style = BoardType.caption.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    stringResource(R.string.board_ba_after),
                    color = colors.onPrimary,
                    style = BoardType.caption.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        Spacer(modifier = Modifier.height(PixiSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            BoardOutlinedButton(
                text = stringResource(R.string.board_ba_before),
                onClick = { split = 0.88f },
                modifier = Modifier.weight(1f)
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_ba_mid),
                onClick = { split = 0.5f },
                modifier = Modifier.weight(1f)
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_ba_after),
                onClick = { split = 0.12f },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** K-VAR — variation grid. */
@Composable
fun BoardVariationGrid(
    @DrawableRes images: List<Int>,
    modifier: Modifier = Modifier,
    onPick: (Int) -> Unit = {}
) {
    val columns = rememberPixiGridColumns(compact = 2, medium = 3, expanded = 4)
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        contentPadding = PaddingValues(PixiSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
    ) {
        itemsIndexed(images, key = { _, resId -> resId }) { index, resId ->
            val label = stringResource(R.string.board_a11y_variation, index + 1)
            BoardArtImage(
                resId = resId,
                contentDescription = null,
                maxSidePx = 512,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.md))
                    .border(1.dp, colors.surfaceMuted, RoundedCornerShape(radii.md))
                    .clickable(role = Role.Button, onClick = { onPick(index) })
                    .semantics { contentDescription = label }
            )
        }
    }
}

/**
 * Still / video preview. Video uses [VideoView] looping a raw resource when [videoRawRes] is set.
 */
@Composable
fun BoardMediaPreview(
    modifier: Modifier = Modifier,
    @DrawableRes posterRes: Int? = null,
    videoRawRes: Int? = null,
    showPlayAffordance: Boolean = videoRawRes != null || posterRes != null
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(radii.card))
            .background(colors.surfaceMuted)
    ) {
        when {
            videoRawRes != null -> {
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            setVideoURI(
                                Uri.parse("android.resource://${context.packageName}/$videoRawRes")
                            )
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                start()
                            }
                        }
                    },
                    update = { view ->
                        if (!view.isPlaying) {
                            view.start()
                        }
                    },
                    onRelease = { view ->
                        view.stopPlayback()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            posterRes != null -> {
                BoardArtImage(
                    resId = posterRes,
                    contentDescription = null,
                    maxSidePx = 1080,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Text(
                    stringResource(R.string.board_media_preview),
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.textSecondary
                )
            }
        }
        if (showPlayAffordance && videoRawRes == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_play),
                    contentDescription = stringResource(R.string.board_play),
                    tint = colors.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
fun BoardIntentTile(
    title: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int? = null,
    aspectRatio: Float = 1f,
    /** One-line outcome so tools explain themselves at a glance (Bible / Flow 2). */
    subtitle: String? = null
) {
    val radii = BoardTheme.radii
    val colors = BoardTheme.colors
    val a11y = if (subtitle.isNullOrBlank()) title else "$title. $subtitle"
    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(radii.card))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.22f))
                )
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = a11y
            }
    ) {
        if (imageRes != null) {
            BoardArtImage(
                resId = imageRes,
                contentDescription = null,
                maxSidePx = 640,
                modifier = Modifier
                    .fillMaxSize()
                    .boardDecorative()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(IntentTileScrim))
                    .boardDecorative()
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(PixiSpacing.md)
                .size(width = 28.dp, height = 4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(accent)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.md),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.size(36.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = BoardType.cta.copy(fontWeight = FontWeight.Bold),
                    color = if (imageRes != null) {
                        androidx.compose.ui.graphics.Color.White
                    } else {
                        colors.textPrimary
                    },
                    maxLines = 2
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = BoardType.caption,
                        color = if (imageRes != null) {
                            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.88f)
                        } else {
                            colors.textSecondary
                        },
                        maxLines = 2
                    )
                }
            }
        }
    }
}
