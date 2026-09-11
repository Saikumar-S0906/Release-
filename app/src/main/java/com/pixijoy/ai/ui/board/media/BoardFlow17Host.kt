package com.pixijoy.ai.ui.board.media

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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.pixijoy.ai.R
import com.pixijoy.ai.di.BoardAiEntryPoint
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardEditorDocument
import com.pixijoy.ai.ui.board.BoardMediaSession
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.BoardSafeResultImage
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.rememberBoardPhotoGate
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardEdgeActions
import com.pixijoy.ai.ui.board.components.BoardEdgeKind
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardListDetail
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardSoftEdgeGate
import com.pixijoy.ai.ui.board.components.BoardSoftPulse
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.components.WindowWidthSizeClass
import com.pixijoy.ai.ui.components.rememberWindowWidthSizeClass
import com.pixijoy.ai.ui.theme.PixiSpacing
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import java.io.File
import java.io.FileOutputStream

private data class PlatformOption(
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    @DrawableRes val icon: Int,
    val accent: Color,
    val variants: List<Int>,
    val sizeLabel: String,
    val aspect: Float,
    val printW: Int = 0,
    val printH: Int = 0
)

/** Flow 17 session — platform / format / quality across export steps. */
internal object BoardFlow17Session {
    var platformIndex: Int = 0
    var variantIndex: Int = 0
    var formatIndex: Int = 0
    var quality: Float = 90f
    var keepAspect: Boolean = true
    var removeWatermark: Boolean = false
    var resolutionIndex: Int = 0
    var saveLocationIndex: Int = 0

    @Volatile
    var exportedBitmap: Bitmap? = null
        private set

    fun setExported(bitmap: Bitmap?) {
        val old = exportedBitmap
        exportedBitmap = bitmap
        if (old != null && old !== bitmap && !old.isRecycled) {
            runCatching { old.recycle() }
        }
    }

    @DrawableRes
    fun hero(): Int = BoardFlowArt.baAfter
}

/** Map platform aspect float to crop ratios for [CreateExtraPipeline.exportForPlatform]. */
private fun aspectParts(aspect: Float): Pair<Float, Float> = when {
    abs(aspect - 4f / 5f) < 0.02f -> 4f to 5f
    abs(aspect - 9f / 16f) < 0.02f -> 9f to 16f
    abs(aspect - 16f / 9f) < 0.02f -> 16f to 9f
    abs(aspect - 1f) < 0.02f -> 1f to 1f
    aspect >= 1f -> aspect to 1f
    else -> 1f to (1f / aspect.coerceAtLeast(0.01f))
}

private fun resolveExportSource(context: Context): Bitmap? {
    BoardEditorDocument.workingBitmap?.takeIf { !it.isRecycled }?.let { return it }
    return BoardMediaSession.decodeBitmap(context, maxSide = 1920)
}

private fun maxSideForResolution(index: Int): Int = when (index) {
    1 -> 1080
    2 -> 3840
    else -> 1920
}

/**
 * Flow 17 — Export & Platform Optimization.
 * Entry via Share from editor / collage / pipeline openExport.
 */
@Composable
fun BoardFlow17Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    val pipeline = LocalBoardPipeline.current
    when (screenId) {
        "b17_hub", "b17_open" -> OpenExport(
            onTab = tab,
            onShare = { onNavigate("b17_platform") },
            onEdit = { onNavigate("b15_open") },
            onSave = {
                // soft: jump to staging save
                onNavigate("b17_staging")
            },
            onBack = onBack
        )
        "b17_platform" -> ChoosePlatform(
            onPick = { onNavigate("b17_optimize") },
            onBack = { onNavigate("b17_open") }
        )
        "b17_optimize" -> AutoOptimize(
            onContinue = { onNavigate("b17_preview") },
            onBack = { onNavigate("b17_platform") }
        )
        "b17_preview" -> PlatformPreview(
            onNext = { onNavigate("b17_staging") },
            onAdvanced = { onNavigate("b17_advanced") },
            onBack = { onNavigate("b17_optimize") }
        )
        "b17_advanced" -> AdvancedOptions(
            onApply = { onNavigate("b17_preview") },
            onPro = pipeline.openPro,
            onBack = { onNavigate("b17_preview") }
        )
        "b17_staging" -> ExportShareHub(
            onSave = { onNavigate("b17_exporting") },
            onSharePlatform = { onNavigate("b17_exporting") },
            onMore = { shareDrawable(it, BoardFlow17Session.hero()) },
            onBack = { onNavigate("b17_preview") }
        )
        "b17_exporting" -> Exporting(
            onDone = { onNavigate("b17_done") },
            onCancel = { onNavigate("b17_staging") },
            onOffline = { onNavigate("b17_offline") },
            onLowCredits = { onNavigate("b17_low_credits") }
        )
        "b17_done" -> Done(
            onGallery = pipeline.openCreations,
            onShareAgain = { onNavigate("b17_platform") },
            onBack = onBack
        )
        "b17_offline" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Offline,
            onPrimary = { onNavigate("b17_exporting") },
            onSecondary = { onNavigate("b17_staging") }
        )
        "b17_low_credits" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.LowCredits,
            onPrimary = { onNavigate("b23_dash") },
            onSecondary = { onNavigate("b17_staging") }
        )
        "b17_safety" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Safety,
            onPrimary = { onNavigate("b17_open") },
            onSecondary = onBack
        )
        else -> OpenExport(
            onTab = tab,
            onShare = { onNavigate("b17_platform") },
            onEdit = { onNavigate("b15_open") },
            onSave = { onNavigate("b17_staging") },
            onBack = onBack
        )
    }
}

@Composable
private fun rememberPlatforms(): List<PlatformOption> {
    val colors = BoardTheme.colors
    return listOf(
        PlatformOption(
            R.string.board_f17_ig, R.string.board_f17_ig_sub, R.drawable.pj_ic_gallery, colors.accentPink,
            listOf(R.string.board_f17_post, R.string.board_f17_story, R.string.board_f17_reel),
            "1080 × 1350", 4f / 5f
        ),
        PlatformOption(
            R.string.board_f17_wa, R.string.board_f17_wa_sub, R.drawable.pj_ic_share, Color(0xFF25D366),
            listOf(R.string.board_f17_chat, R.string.board_f17_status),
            "1080 × 1350", 4f / 5f
        ),
        PlatformOption(
            R.string.board_f17_tiktok, R.string.board_f17_tiktok_sub, R.drawable.pj_ic_movie, colors.textPrimary,
            listOf(R.string.board_f17_video),
            "1080 × 1920", 9f / 16f
        ),
        PlatformOption(
            R.string.board_f17_snap, R.string.board_f17_snap_sub, R.drawable.pj_ic_sparkles, colors.accentYellow,
            listOf(R.string.board_f17_snap_item, R.string.board_f17_story),
            "1080 × 1920", 9f / 16f
        ),
        PlatformOption(
            R.string.board_f17_fb, R.string.board_f17_fb_sub, R.drawable.pj_ic_share, Color(0xFF1877F2),
            listOf(R.string.board_f17_post, R.string.board_f17_story),
            "1200 × 1500", 4f / 5f
        ),
        PlatformOption(
            R.string.board_f17_yt, R.string.board_f17_yt_sub, R.drawable.pj_ic_movie, Color(0xFFFF0000),
            listOf(R.string.board_f17_thumb, R.string.board_f17_video),
            "1280 × 720", 16f / 9f
        ),
        PlatformOption(
            R.string.board_f17_wallpaper, R.string.board_f17_wallpaper_sub, R.drawable.pj_ic_landscape, colors.primary,
            listOf(R.string.board_f17_home_lock),
            "1080 × 1920", 9f / 16f
        ),
        PlatformOption(
            R.string.board_f17_print_4x6, R.string.board_f17_print_4x6_sub, R.drawable.pj_ic_gallery, colors.accentPink,
            listOf(R.string.board_f17_print),
            "1200 × 1800 · 300 dpi", 4f / 6f,
            printW = 1200, printH = 1800
        ),
        PlatformOption(
            R.string.board_f17_print_a4, R.string.board_f17_print_a4_sub, R.drawable.pj_ic_crop, colors.primary,
            listOf(R.string.board_f17_print),
            "2480 × 3508 · 300 dpi", 210f / 297f,
            printW = 2480, printH = 3508
        ),
        PlatformOption(
            R.string.board_f17_custom, R.string.board_f17_custom_sub, R.drawable.pj_ic_crop, colors.accentPurple,
            listOf(R.string.board_f17_custom_size),
            "Custom", 1f
        )
    )
}

@Composable
private fun OpenExport(
    onTab: (BoardShellTab) -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val mediaRevision = BoardMediaSession.revision
    val source = remember(mediaRevision, BoardEditorDocument.revision) {
        resolveExportSource(context)
    }
    val photoGate = rememberBoardPhotoGate(
        onCameraDenied = { }
    ) { }
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW17_HOST)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow17_title),
                    style = BoardType.title,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
            if (source == null) {
                com.pixijoy.ai.ui.board.components.BoardEmptyState(
                    title = stringResource(R.string.board_f17_need_photo),
                    body = stringResource(R.string.photo_gate_body),
                    ctaLabel = stringResource(R.string.choose_a_photo),
                    onCta = { photoGate() },
                    modifier = Modifier.weight(1f)
                )
                return@Column
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                BoardSafeResultImage(
                    bitmap = source,
                    fallbackRes = BoardFlowArt.editorHero,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Text(
                    stringResource(R.string.board_f17_overlay),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(PixiSpacing.lg)
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            BoardPrimaryButton(
                text = stringResource(R.string.share),
                onClick = onShare
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardOutlinedButton(
                text = stringResource(R.string.result_save),
                onClick = onSave
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardOutlinedButton(
                text = stringResource(R.string.board_f17_edit),
                onClick = onEdit
            )
        }
    }
}

@Composable
private fun ChoosePlatform(onPick: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val platforms = rememberPlatforms()
    var selected by remember { mutableIntStateOf(BoardFlow17Session.platformIndex) }
    val isExpanded = rememberWindowWidthSizeClass() == WindowWidthSizeClass.Expanded
    val platformPane: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow17_platform_title),
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                val platformThumbs = listOf(
                    BoardFlowArt.baAfter,
                    BoardFlowArt.creationThumb
                ) + BoardFlowArt.mediaThumbs
                platforms.forEachIndexed { i, p ->
                    val on = i == selected
                    val thumb = platformThumbs[i % platformThumbs.size]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(radii.card))
                            .background(if (on) p.accent.copy(0.15f) else colors.surfaceMuted)
                            .border(
                                width = if (on) 2.dp else 0.dp,
                                color = if (on) p.accent else Color.Transparent,
                                shape = RoundedCornerShape(radii.card)
                            )
                            .clickable {
                                selected = i
                                BoardFlow17Session.platformIndex = i
                                BoardFlow17Session.variantIndex = 0
                                onPick()
                            }
                            .padding(PixiSpacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                    ) {
                        Image(
                            painter = painterResource(thumb),
                            contentDescription = null,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(p.title), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            Text(stringResource(p.subtitle), style = BoardType.caption, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
    BoardAdaptiveFrame {
        BoardListDetail(
            showDetail = isExpanded,
            list = {
                if (isExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(PixiSpacing.xl)
                            .clip(RoundedCornerShape(radii.card))
                    ) {
                        com.pixijoy.ai.ui.board.BoardSessionPhoto(
                            fallbackRes = BoardFlowArt.editorHero,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    platformPane()
                }
            },
            detail = platformPane
        )
    }
}

@Composable
private fun AutoOptimize(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val platforms = rememberPlatforms()
    val platform = platforms[BoardFlow17Session.platformIndex.coerceIn(0, platforms.lastIndex)]
    var progress by remember { mutableFloatStateOf(0.1f) }
    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(220)
            progress = (progress + 0.18f).coerceAtMost(1f)
        }
    }
    val checks = listOf(
        stringResource(R.string.board_f17_opt_best_size, platform.sizeLabel),
        stringResource(R.string.board_f17_opt_quality),
        stringResource(R.string.board_f17_opt_aspect),
        stringResource(R.string.board_f17_opt_color),
        stringResource(R.string.board_f17_opt_light)
    )
    val doneCount = ((progress * checks.size).toInt()).coerceIn(0, checks.size)
    BoardAdaptiveFrame {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(BoardFlow17Session.hero()),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.35f
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PixiSpacing.xl)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                    }
                    Text(
                        stringResource(R.string.board_f17_optimize_for, stringResource(platform.title)),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surface)
                        .padding(PixiSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    checks.forEachIndexed { i, label ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (i < doneCount) {
                                Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(22.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, colors.textSecondary, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.size(PixiSpacing.sm))
                            Text(label, color = colors.textPrimary)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(radii.md))
                            .background(colors.primary.copy(0.12f))
                            .padding(PixiSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        Icon(painter = painterResource(R.drawable.pj_ic_info), null, tint = colors.primary)
                        Text(
                            stringResource(R.string.board_f17_opt_reassure),
                            color = colors.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                BoardPrimaryButton(
                    text = stringResource(R.string.board_f4_continue),
                    onClick = onContinue
                )
            }
        }
    }
}

@Composable
private fun PlatformPreview(
    onNext: () -> Unit,
    onAdvanced: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val platforms = rememberPlatforms()
    val platform = platforms[BoardFlow17Session.platformIndex.coerceIn(0, platforms.lastIndex)]
    var variant by remember { mutableIntStateOf(BoardFlow17Session.variantIndex.coerceIn(0, platform.variants.lastIndex)) }
    val variantRes = platform.variants[variant]
    val aspect = when (variantRes) {
        R.string.board_f17_story,
        R.string.board_f17_reel,
        R.string.board_f17_status,
        R.string.board_f17_home_lock -> 9f / 16f
        R.string.board_f17_thumb -> 16f / 9f
        R.string.board_f17_video -> if (platform.title == R.string.board_f17_tiktok) 9f / 16f else 16f / 9f
        else -> platform.aspect
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f17_preview),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onAdvanced) {
                    Text(stringResource(R.string.flow17_advanced), color = colors.primary)
                }
            }
            if (platform.variants.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.pill))
                        .background(colors.surfaceMuted)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    platform.variants.forEachIndexed { i, res ->
                        val on = i == variant
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(radii.pill))
                                .background(if (on) colors.primary else Color.Transparent)
                                .clickable {
                                    variant = i
                                    BoardFlow17Session.variantIndex = i
                                }
                                .padding(vertical = PixiSpacing.sm),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(res),
                                color = if (on) colors.onPrimary else colors.textSecondary,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(BoardFlow17Session.hero()),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(aspect)
                        .clip(RoundedCornerShape(radii.card))
                        .border(1.dp, colors.surfaceMuted, RoundedCornerShape(radii.card)),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                stringResource(
                    R.string.board_f17_preview_hint,
                    stringResource(platform.title),
                    stringResource(platform.variants[variant])
                ),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            BoardPrimaryButton(text = stringResource(R.string.next), onClick = onNext)
        }
    }
}

@Composable
private fun AdvancedOptions(
    onApply: () -> Unit,
    onPro: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var format by remember { mutableIntStateOf(BoardFlow17Session.formatIndex) }
    var quality by remember { mutableFloatStateOf(BoardFlow17Session.quality) }
    var keepAspect by remember { mutableStateOf(BoardFlow17Session.keepAspect) }
    var removeWm by remember { mutableStateOf(BoardFlow17Session.removeWatermark) }
    var resolution by remember { mutableIntStateOf(BoardFlow17Session.resolutionIndex) }
    var location by remember { mutableIntStateOf(BoardFlow17Session.saveLocationIndex) }
    val formats = listOf("JPG", "PNG", "WEBP")
    val resolutions = listOf(
        R.string.board_f17_res_recommended,
        R.string.board_f17_res_1080,
        R.string.board_f17_res_4k
    )
    val locations = listOf(
        R.string.board_f17_loc_gallery,
        R.string.board_f17_loc_creations,
        R.string.board_f17_loc_both
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow17_advanced),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    format = 0
                    quality = 90f
                    keepAspect = true
                    removeWm = false
                    resolution = 0
                    location = 0
                }) {
                    Text(stringResource(R.string.board_f17_reset), color = colors.primary)
                }
            }
            Text(stringResource(R.string.flow17_format), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                formats.forEachIndexed { i, label ->
                    val on = i == format
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (on) colors.primary else colors.surfaceMuted)
                            .clickable { format = i }
                            .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.sm)
                    ) {
                        Text(label, color = if (on) colors.onPrimary else colors.textSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Text(stringResource(R.string.flow17_resolution), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            resolutions.forEachIndexed { i, res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(if (i == resolution) colors.primary.copy(0.12f) else colors.surfaceMuted)
                        .clickable { resolution = i }
                        .padding(PixiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(res), color = colors.textPrimary, modifier = Modifier.weight(1f))
                    if (i == resolution) {
                        Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Text(
                stringResource(R.string.flow17_quality) + " · ${quality.toInt()}%",
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Slider(
                value = quality,
                onValueChange = { quality = it },
                valueRange = 40f..100f
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.board_f17_smaller), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                Text(stringResource(R.string.board_f17_higher), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.board_f17_keep_aspect), modifier = Modifier.weight(1f), color = colors.textPrimary)
                Switch(checked = keepAspect, onCheckedChange = { keepAspect = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.flow17_remove_watermark), modifier = Modifier.weight(1f), color = colors.textPrimary)
                Switch(
                    checked = removeWm,
                    onCheckedChange = {
                        if (it) {
                            onPro()
                        } else {
                            removeWm = false
                            BoardFlow17Session.removeWatermark = false
                        }
                    }
                )
            }
            Text(stringResource(R.string.board_f17_save_location), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            locations.forEachIndexed { i, res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(if (i == location) colors.primary.copy(0.12f) else colors.surfaceMuted)
                        .clickable { location = i }
                        .padding(PixiSpacing.md)
                ) {
                    Text(stringResource(res), color = colors.textPrimary)
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.apply),
                onClick = {
                    BoardFlow17Session.formatIndex = format
                    BoardFlow17Session.quality = quality
                    BoardFlow17Session.keepAspect = keepAspect
                    BoardFlow17Session.removeWatermark = removeWm
                    BoardFlow17Session.resolutionIndex = resolution
                    BoardFlow17Session.saveLocationIndex = location
                    onApply()
                }
            )
        }
    }
}

@Composable
private fun ExportShareHub(
    onSave: () -> Unit,
    onSharePlatform: () -> Unit,
    onMore: (Context) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val platforms = rememberPlatforms()
    val platform = platforms[BoardFlow17Session.platformIndex.coerceIn(0, platforms.lastIndex)]
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
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f17_export_share),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(BoardFlow17Session.hero()),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(radii.card))
                    .align(Alignment.CenterHorizontally),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.board_f17_ready_body),
                textAlign = TextAlign.Center,
                color = colors.textSecondary,
                modifier = Modifier.fillMaxWidth()
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_f16_save_gallery),
                onClick = onSave
            )
            listOf(
                Triple(R.drawable.pj_ic_share, stringResource(R.string.board_f17_share_to, stringResource(platform.title)), onSharePlatform),
                Triple(R.drawable.pj_ic_share, stringResource(R.string.board_f17_share_wa), onSharePlatform),
                Triple(R.drawable.pj_ic_share, stringResource(R.string.board_f17_share_tiktok), onSharePlatform),
                Triple(R.drawable.pj_ic_share, stringResource(R.string.board_f17_more_apps), { onMore(context) })
            ).forEach { (icon, label, action) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .clickable(onClick = action)
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Icon(painter = painterResource(icon), null, tint = colors.primary)
                    Text(label, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                }
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun Exporting(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    var progress by remember { mutableFloatStateOf(0.08f) }
    val steps = listOf(
        R.string.board_f17_exp_process,
        R.string.board_f17_exp_optimize,
        R.string.board_f17_exp_quality,
        R.string.board_f17_exp_final
    )
    val platforms = rememberPlatforms()
    LaunchedEffect(Unit) {
        val platform = platforms[BoardFlow17Session.platformIndex.coerceIn(0, platforms.lastIndex)]
        val (aspectW, aspectH) = aspectParts(platform.aspect)
        val maxSide = maxSideForResolution(BoardFlow17Session.resolutionIndex)
        val exportJob = withContext(Dispatchers.Default) {
            runCatching {
                val src = resolveExportSource(context.applicationContext) ?: return@runCatching null
                val pipe = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    BoardAiEntryPoint::class.java
                ).createExtraPipeline()
                val exported = if (platform.printW > 0 && platform.printH > 0) {
                    pipe.exportPrintPage(src, platform.printW, platform.printH)
                } else {
                    pipe.exportForPlatform(src, aspectW, aspectH, maxSide)
                }
                val copy = if (exported !== src && !exported.isRecycled) {
                    exported
                } else {
                    src.copy(Bitmap.Config.ARGB_8888, false)
                }
                copy
            }.getOrNull()
        }
        while (progress < 0.92f) {
            delay(180)
            progress = (progress + 0.14f).coerceAtMost(0.92f)
        }
        withContext(Dispatchers.IO) {
            if (exportJob != null && !exportJob.isRecycled) {
                BoardFlow17Session.setExported(exportJob)
                BoardDrawableIo.saveBitmapToGallery(
                    context = context.applicationContext,
                    bitmap = exportJob,
                    filePrefix = "PixiJoy_Export",
                    formatIndex = if (platform.printW > 0) 0 else BoardFlow17Session.formatIndex,
                    quality = BoardFlow17Session.quality.toInt().coerceIn(1, 100)
                )
            } else {
                saveDrawableToGallery(context, BoardFlow17Session.hero())
            }
        }
        progress = 1f
        delay(280)
        onDone()
    }
    val doneCount = ((progress * steps.size).toInt()).coerceIn(0, steps.size)
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                stringResource(R.string.flow17_exporting),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            BoardSoftPulse {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(120.dp),
                        color = colors.primary,
                        trackColor = colors.surfaceMuted,
                        strokeWidth = 8.dp
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Text(stringResource(R.string.board_f17_creating_file), color = colors.textSecondary)
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            Column(verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                steps.forEachIndexed { i, res ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (i < doneCount) {
                            Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(20.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, colors.textSecondary, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.size(PixiSpacing.sm))
                        Text(
                            stringResource(res),
                            color = if (i < doneCount) colors.textPrimary else colors.textSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            BoardEdgeActions(
                onOffline = onOffline,
                onLowCredits = null
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardOutlinedButton(text = stringResource(R.string.cancel), onClick = onCancel)
        }
    }
}

@Composable
private fun Done(
    onGallery: () -> Unit,
    onShareAgain: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
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
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow17_success_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            BoardSafeResultImage(
                bitmap = BoardFlow17Session.exportedBitmap,
                fallbackRes = BoardFlow17Session.hero(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Icon(painter = painterResource(R.drawable.pj_ic_check_circle),
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier.size(56.dp)
            )
            Text(
                stringResource(R.string.board_f17_success_body),
                textAlign = TextAlign.Center,
                color = colors.textSecondary
            )
            BoardPrimaryButton(text = stringResource(R.string.flow17_view_gallery), onClick = onGallery)
            BoardOutlinedButton(text = stringResource(R.string.flow17_share_again), onClick = onShareAgain)
            BoardTrustFooter()
        }
    }
}

private fun saveDrawableToGallery(context: Context, @DrawableRes resId: Int): Boolean =
    BoardDrawableIo.saveToGallery(context, resId, filePrefix = "PixiJoy_Music")

private fun shareDrawable(context: Context, @DrawableRes resId: Int) {
    BoardDrawableIo.shareAsync(
        context = context,
        resId = resId,
        textRes = R.string.board_f5_share_text,
        filePrefix = "pixijoy_music"
    )
}

