package com.pixijoy.ai.ui.board.media

import androidx.annotation.DrawableRes

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
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
import com.pixijoy.ai.R
import com.pixijoy.ai.di.BoardAiEntryPoint
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardMediaSession
import com.pixijoy.ai.ui.board.BoardSessionPhoto
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardCaptionText
import com.pixijoy.ai.ui.board.components.BoardFilterPill
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardGateScreen
import com.pixijoy.ai.ui.board.components.BoardHeroImage
import com.pixijoy.ai.ui.board.components.BoardIntentTile
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardSecondaryButton
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardTipCallout
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.components.BoardWideFrame
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing
import com.pixijoy.ai.util.MediaPermissionHelper
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Flow 3 — Photo / Video input. Source: flows/03_Photo_Video_Input.png */
@Composable
fun BoardFlow3Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = com.pixijoy.ai.ui.board.BoardProductionTabs.tabHandler(onNavigate)
    val pipeline = LocalBoardPipeline.current
    when (screenId) {
        "b3_open" -> OpenAddPhoto(
            onAddPhoto = { onNavigate("b3_chooser") },
            onTab = tab,
            pipeline = pipeline,
            onNavigate = onNavigate
        )
        "b3_chooser" -> Chooser(
            onCamera = { onNavigate("b3_camera") },
            onGallery = { onNavigate("b3_gallery") },
            onFiles = { onNavigate("b3_preview") },
            onStock = {
                BoardMediaSession.setSampleRes(BoardFlowArt.creationThumb)
                onNavigate("b3_preview")
            },
            onCloud = {
                BoardMediaSession.setSampleRes(BoardFlowArt.homeHero)
                onNavigate("b3_preview")
            },
            onBack = onBack
        )
        "b3_camera" -> CameraUi(
            onCapture = { onNavigate("b3_preview") },
            onDenied = { onNavigate("b3_camera_denied") },
            onGallery = { onNavigate("b3_gallery") },
            onBack = { onNavigate("b3_chooser") }
        )
        "b3_gallery" -> GalleryPick(
            onNext = { onNavigate("b3_preview") },
            onMulti = { onNavigate("b3_multi") },
            onVideo = { onNavigate("b3_video") },
            onDenied = { onNavigate("b3_photos_denied") },
            onBack = { onNavigate("b3_chooser") }
        )
        "b3_preview" -> PreviewEdit(
            onEdit = { onNavigate("b15_open") },
            onCutout = { onNavigate("b_c04") },
            onRestore = { onNavigate("b_c05") },
            onStickers = pipeline.openStickers,
            onBack = { onNavigate("b3_chooser") }
        )
        "b3_multi" -> MultiPurpose(
            onCollage = { onNavigate("b16_select") },
            onSlideshow = { onNavigate("b14_input") },
            onStory = { onNavigate("b14_input") },
            onMeme = { onNavigate("b6_choose") },
            onAddMore = { onNavigate("b3_gallery") },
            onBack = { onNavigate("b3_gallery") }
        )
        "b3_video" -> VideoPick(
            onNext = { onNavigate("b3_trim") },
            onProceed = { onNavigate("b3_proceed") },
            onBack = { onNavigate("b3_chooser") }
        )
        "b3_trim" -> VideoTrim(
            onDone = { onNavigate("b3_proceed") },
            onBack = { onNavigate("b3_video") }
        )
        "b3_proceed" -> ProceedAi(
            onDone = { onNavigate("b4_selected") },
            onCancel = { onNavigate("b3_preview") }
        )
        "b3_camera_denied" -> EdgeScreen(
            icon = R.drawable.pj_ic_lock,
            title = stringResource(R.string.flow3_camera_denied_title),
            body = stringResource(R.string.flow3_camera_denied_body),
            primary = stringResource(R.string.board_f3_try_again),
            onPrimary = { onNavigate("b3_camera") },
            secondary = stringResource(R.string.flow3_use_gallery),
            onSecondary = { onNavigate("b3_gallery") },
            tertiary = stringResource(R.string.board_f3_open_settings),
            tipTitle = stringResource(R.string.board_perm_why_title),
            tipBody = stringResource(R.string.board_perm_why_camera)
        )
        "b3_photos_denied" -> EdgeScreen(
            icon = R.drawable.pj_ic_gallery,
            title = stringResource(R.string.flow3_photos_denied_title),
            body = stringResource(R.string.flow3_photos_denied_body),
            primary = stringResource(R.string.board_f3_try_again),
            onPrimary = { onNavigate("b3_gallery") },
            secondary = stringResource(R.string.board_perm_limited),
            onSecondary = { onNavigate("b3_limited") },
            tertiary = stringResource(R.string.board_f3_open_settings),
            tipTitle = stringResource(R.string.board_perm_why_title),
            tipBody = stringResource(R.string.board_perm_why_photos)
        )
        "b3_limited" -> EdgeScreen(
            icon = R.drawable.pj_ic_gallery,
            title = stringResource(R.string.board_limited_title),
            body = stringResource(R.string.board_limited_body),
            primary = stringResource(R.string.board_f3_select_photos),
            onPrimary = { onNavigate("b3_gallery") },
            secondary = stringResource(R.string.board_f3_allow_all),
            onSecondary = { onNavigate("b3_gallery") }
        )
        "b3_no_photos" -> EdgeScreen(
            icon = R.drawable.pj_ic_gallery,
            title = stringResource(R.string.board_f3_no_photos_title),
            body = stringResource(R.string.board_f3_no_photos_body),
            primary = stringResource(R.string.board_f3_capture),
            onPrimary = { onNavigate("b3_camera") },
            secondary = stringResource(R.string.board_f3_import_files),
            onSecondary = { onNavigate("b3_chooser") }
        )
        "b3_unsupported" -> EdgeScreen(
            icon = R.drawable.pj_ic_warning,
            title = stringResource(R.string.flow3_unsupported_title),
            body = stringResource(R.string.flow3_unsupported_body),
            primary = stringResource(R.string.flow3_choose_another),
            onPrimary = { onNavigate("b3_chooser") }
        )
        "b3_too_large" -> EdgeScreen(
            icon = R.drawable.pj_ic_warning,
            title = stringResource(R.string.flow3_too_large_title),
            body = stringResource(R.string.flow3_too_large_body),
            primary = stringResource(R.string.flow3_compress),
            onPrimary = { onNavigate("b3_preview") },
            secondary = stringResource(R.string.flow3_choose_smaller),
            onSecondary = { onNavigate("b3_chooser") }
        )
        "b3_corrupt" -> EdgeScreen(
            icon = R.drawable.pj_ic_close,
            title = stringResource(R.string.board_f3_corrupt_title),
            body = stringResource(R.string.board_f3_corrupt_body),
            primary = stringResource(R.string.board_f3_try_another),
            onPrimary = { onNavigate("b3_chooser") }
        )
        else -> Chooser(
            onCamera = { onNavigate("b3_camera") },
            onGallery = { onNavigate("b3_gallery") },
            onFiles = { onNavigate("b3_preview") },
            onStock = { onNavigate("b3_preview") },
            onCloud = { onNavigate("b3_preview") },
            onBack = onBack
        )
    }
}

@Composable
private fun OpenAddPhoto(
    onAddPhoto: () -> Unit,
    onTab: (BoardShellTab) -> Unit,
    pipeline: com.pixijoy.ai.ui.board.BoardPipelineCallbacks,
    onNavigate: (String) -> Unit
) {
    val colors = BoardTheme.colors
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW3_CHOOSER),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.board_f3_ready_prompt),
                style = BoardType.title,
                fontWeight = FontWeight.Bold
            )
            com.pixijoy.ai.ui.board.BoardCapturePhotoRow(
                onPicked = { uri ->
                    BoardMediaSession.setPhotoUri(uri)
                    onNavigate("b3_preview")
                },
                onCameraDenied = { onNavigate("b3_camera_denied") },
                galleryTestTag = null
            )
            val tiles = listOf(
                Triple(
                    stringResource(R.string.flow15_title),
                    colors.primary to BoardFlowArt.editorHero,
                    { onNavigate("b15_open") }
                ),
                Triple(
                    stringResource(R.string.create_extra_remove_bg),
                    colors.accentPink to R.drawable.board_c04_cutout,
                    { onNavigate("b_c04") }
                ),
                Triple(
                    stringResource(R.string.create_stickers),
                    colors.primary to BoardFlowArt.stickerHero,
                    pipeline.openStickers
                ),
                Triple(
                    stringResource(R.string.board_f17_export_share),
                    colors.accentPurple to BoardFlowArt.editorHero,
                    pipeline.openExport
                )
            )
            tiles.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEach { (title, style, onClick) ->
                        val (accent, image) = style
                        BoardIntentTile(
                            title = title,
                            accent = accent,
                            imageRes = image,
                            onClick = onClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f3_add_photo_cta),
                onClick = onAddPhoto
            )
            Text(stringResource(R.string.board_f2_recents), fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                BoardFlowArt.homeRecents.forEachIndexed { index, res ->
                    Image(
                        painter = painterResource(res),
                        contentDescription = stringResource(R.string.board_f2_item_recent_n, index + 1),
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(BoardTheme.radii.md))
                            .clickable {
                                BoardMediaSession.setSampleRes(res)
                                onNavigate("b3_preview")
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun Chooser(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onFiles: () -> Unit,
    onStock: () -> Unit,
    onCloud: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val filesPicker = com.pixijoy.ai.ui.components.rememberPhotoUriHandler(
        onPhotoUri = { uri ->
            BoardMediaSession.setPhotoUri(uri)
            onFiles()
        }
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
                    stringResource(R.string.flow3_chooser_title),
                    style = BoardType.title,
                    fontWeight = FontWeight.Bold
                )
            }
            PrimaryChoice(R.drawable.pj_ic_camera, stringResource(R.string.board_f3_take_photo), onCamera)
            PrimaryChoice(R.drawable.pj_ic_gallery, stringResource(R.string.board_f3_choose_gallery), onGallery)
            PrimaryChoice(
                R.drawable.pj_ic_file,
                stringResource(R.string.board_f3_import_files),
                filesPicker.openFiles
            )
            Text(
                stringResource(R.string.board_f3_more_options),
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary
            )
            listOf(
                stringResource(R.string.board_f3_stock) to onStock,
                stringResource(R.string.flow3_cloud) to onCloud
            ).forEach { (label, action) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(colors.surfaceMuted)
                        .clickable(onClick = action)
                        .padding(PixiSpacing.lg)
                ) {
                    Text(label, fontWeight = FontWeight.Medium)
                }
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun PrimaryChoice(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    val colors = BoardTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BoardTheme.radii.card))
            .background(colors.primary)
            .clickable(onClick = onClick)
            .padding(PixiSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
    ) {
        Icon(painter = painterResource(icon), null, tint = Color.White)
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun CameraUi(
    onCapture: () -> Unit,
    onDenied: () -> Unit,
    onGallery: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val photo = com.pixijoy.ai.ui.components.rememberPhotoUriHandler(
        onPhotoUri = { uri ->
            BoardMediaSession.setPhotoUri(uri)
            onCapture()
        },
        onCameraPermissionDenied = onDenied
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(BoardFlowArt.cameraViewfinder),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.board_f3_take_photo),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = BoardType.title,
                textAlign = TextAlign.Center
            )
            BoardPrimaryButton(
                text = stringResource(R.string.take_selfie),
                onClick = photo.openSelfie
            )
            BoardPrimaryButton(
                text = stringResource(R.string.take_photo),
                onClick = photo.openCamera
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_f3_choose_gallery),
                onClick = {
                    photo.openGallery()
                }
            )
            BoardSecondaryButton(text = stringResource(R.string.cd_back), onClick = onBack)
        }
    }
}

@Composable
private fun GalleryPick(
    onNext: () -> Unit,
    onMulti: () -> Unit,
    onVideo: () -> Unit,
    onDenied: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val context = LocalContext.current
    var selected by remember { mutableIntStateOf(3) }
    var galleryTab by remember { mutableIntStateOf(0) }
    var hasDevicePhoto by remember { mutableStateOf(BoardMediaSession.hasPhoto()) }
    val thumbs = BoardFlowArt.libraryThumbs
    val permission = MediaPermissionHelper.galleryReadPermission()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (!granted) onDenied() }
    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            BoardMediaSession.setPhotoUri(uri)
            hasDevicePhoto = true
            onNext()
        }
    }
    val needsPermission = !MediaPermissionHelper.hasGalleryReadPermission(context)
    LaunchedEffect(Unit) {
        if (!needsPermission) {
            pickMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            launcher.launch(permission)
        }
    }
    BoardWideFrame {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixiSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), null)
                }
                Text(
                    stringResource(R.string.board_f3_select_photos_title),
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title,
                    modifier = Modifier.weight(1f)
                )
            }
            BoardPrimaryButton(
                text = stringResource(R.string.home_add_photo),
                onClick = {
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PixiSpacing.xl)
            )
            if (hasDevicePhoto) {
                Text(
                    text = stringResource(R.string.board_f3_edit_photo),
                    modifier = Modifier.padding(horizontal = PixiSpacing.xl, vertical = PixiSpacing.sm),
                    color = colors.primary,
                    style = BoardType.caption
                )
            }
            if (needsPermission) {
                TextButton(onClick = { launcher.launch(permission) }) {
                    Text(stringResource(R.string.flow3_photos_denied_title), color = colors.primary)
                }
            }
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = PixiSpacing.xl),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                listOf(
                    R.string.board_f3_tab_photos,
                    R.string.board_f3_tab_albums,
                    R.string.board_f3_tab_favs
                ).forEachIndexed { i, res ->
                    BoardFilterPill(
                        text = stringResource(res),
                        selected = galleryTab == i,
                        onClick = {
                            galleryTab = i
                            if (i == 1) onVideo()
                        }
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .padding(PixiSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.xs)
            ) {
                items(12) { i ->
                    val isSelected = i < selected
                    val thumb = thumbs[i % thumbs.size]
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                selected = if (isSelected) (selected - 1).coerceAtLeast(1) else selected + 1
                                BoardMediaSession.setSampleRes(thumb)
                                hasDevicePhoto = true
                            }
                    ) {
                        Image(
                            painter = painterResource(thumb),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (isSelected) {
                            Icon(painter = painterResource(R.drawable.pj_ic_check_circle),
                                null,
                                tint = colors.accentPurple,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixiSpacing.xl),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onMulti) {
                    Text(stringResource(R.string.board_f3_preview_count, selected), color = colors.primary)
                }
                BoardPrimaryButton(
                    text = stringResource(R.string.board_f3_next_count, selected),
                    onClick = {
                        if (!BoardMediaSession.hasPhoto()) {
                            BoardMediaSession.setSampleRes(thumbs.first())
                        }
                        if (selected > 1) onMulti() else onNext()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PreviewEdit(
    onEdit: () -> Unit,
    onCutout: () -> Unit,
    onRestore: () -> Unit,
    onStickers: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val context = LocalContext.current
    val sessionRev = BoardMediaSession.revision
    var hint by remember { mutableStateOf<com.pixijoy.ai.ui.board.BoardPhotoHint?>(null) }
    LaunchedEffect(sessionRev) {
        hint = withContext(Dispatchers.Default) {
            val bmp = BoardMediaSession.decodeBitmap(context, maxSide = 256) ?: return@withContext null
            com.pixijoy.ai.ui.board.BoardPhotoHint.from(bmp)
        }
    }
    val jobs = listOf(
        R.string.board_f3_job_edit to onEdit,
        R.string.board_f3_job_cutout to onCutout,
        R.string.board_f3_job_stickers to onStickers
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
                    Icon(painter = painterResource(R.drawable.pj_ic_back), null)
                }
                Text(
                    stringResource(R.string.board_f3_edit_photo),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(radii.card))
                    .border(2.dp, colors.primary, RoundedCornerShape(radii.card))
            ) {
                BoardSessionPhoto(
                    fallbackRes = BoardFlowArt.homeHero,
                    contentDescription = stringResource(R.string.board_f3_edit_photo)
                )
            }
            Text(
                stringResource(R.string.board_f3_after_pick),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            hint?.let { h ->
                val go = when (h) {
                    com.pixijoy.ai.ui.board.BoardPhotoHint.Restore -> onRestore
                    com.pixijoy.ai.ui.board.BoardPhotoHint.Cutout -> onCutout
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(colors.primarySoft)
                        .clickable(onClick = go)
                        .testTag(TestTags.FLOW3_PHOTO_HINT)
                        .padding(PixiSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(PixiSpacing.xs)
                ) {
                    Text(
                        stringResource(h.bodyRes),
                        style = BoardType.body,
                        color = colors.textPrimary
                    )
                    Text(
                        stringResource(h.ctaRes),
                        style = BoardType.caption,
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                jobs.forEach { (label, action) ->
                    BoardFilterPill(
                        text = stringResource(label),
                        selected = label == R.string.board_f3_job_edit,
                        onClick = action,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            BoardCaptionText(
                text = stringResource(R.string.create_hub_tools_caption),
                color = colors.textPrimary
            )
            BoardPrimaryButton(
                text = stringResource(R.string.create_hub_open_editor),
                onClick = onEdit
            )
        }
    }
}

@Composable
private fun MultiPurpose(
    onCollage: () -> Unit,
    onSlideshow: () -> Unit,
    onStory: () -> Unit,
    onMeme: () -> Unit,
    onAddMore: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val thumbs = BoardFlowArt.homeRecents.take(3)
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), null)
                }
                Text(
                    stringResource(R.string.board_f3_photos_selected, 3),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onCollage) {
                    Text(stringResource(R.string.board_f2_next), color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                thumbs.forEach { res ->
                    Box {
                        Image(
                            painter = painterResource(res),
                            contentDescription = null,
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(radii.md)),
                            contentScale = ContentScale.Crop
                        )
                        Icon(painter = painterResource(R.drawable.pj_ic_close),
                            null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                                .padding(2.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(radii.md))
                        .background(colors.surfaceMuted)
                        .clickable(onClick = onAddMore),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_add), null, tint = colors.primary)
                }
            }
            Text(stringResource(R.string.flow3_multi_title), fontWeight = FontWeight.SemiBold)
            listOf(
                R.string.flow3_purpose_collage to onCollage,
                R.string.flow3_purpose_slideshow to onSlideshow,
                R.string.flow3_purpose_story to onStory,
                R.string.flow3_purpose_meme to onMeme
            ).forEach { (res, action) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(colors.surfaceMuted)
                        .clickable(onClick = action)
                        .padding(PixiSpacing.lg)
                ) {
                    Text(stringResource(res), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun VideoPick(onNext: () -> Unit, onProceed: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val scope = rememberCoroutineScope()
    var making by remember { mutableStateOf(false) }
    var gifJob by remember { mutableStateOf<Job?>(null) }
    var picked by remember { mutableStateOf(BoardMediaSession.videoUri != null) }
    val pickVideo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            BoardMediaSession.setVideoUri(uri)
            picked = true
        }
    }
    fun makeGif() {
        val uri = BoardMediaSession.videoUri
        if (uri == null) {
            pickVideo.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
            return
        }
        if (making) return
        making = true
        gifJob = scope.launch {
            try {
                val ok = withContext(Dispatchers.Default) {
                    runCatching {
                        val entry = EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            BoardAiEntryPoint::class.java
                        )
                        val frames = entry.videoToGifExtractor().extractFrames(uri, maxFrames = 12)
                        if (frames.isEmpty()) return@runCatching false
                        val gif = entry.gifEncoder().encode(frames, frameDelayMs = 120)
                        frames.forEach { bmp -> if (!bmp.isRecycled) runCatching { bmp.recycle() } }
                        BoardDrawableIo.saveBytesToGallery(
                            context.applicationContext,
                            gif,
                            filePrefix = "PixiJoy_GIF"
                        )
                    }.getOrDefault(false)
                }
                if (!isActive) return@launch
                Toast.makeText(
                    context,
                    context.getString(if (ok) R.string.board_f3_gif_saved else R.string.board_f3_gif_failed),
                    Toast.LENGTH_SHORT
                ).show()
                if (ok) onProceed()
            } finally {
                making = false
                gifJob = null
            }
        }
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
                    Icon(painter = painterResource(R.drawable.pj_ic_back), null)
                }
                Text(
                    stringResource(R.string.board_f3_select_video),
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title
                )
            }
            Text(
                stringResource(R.string.create_hub_video_gif_desc),
                color = colors.textSecondary
            )
            if (picked) {
                Text(stringResource(R.string.board_f3_video_picked), color = colors.textPrimary)
            }
            if (making) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                TextButton(onClick = {
                    gifJob?.cancel()
                    making = false
                }) {
                    Text(stringResource(R.string.cancel), color = colors.primary)
                }
            }
            if (picked) {
                BoardPrimaryButton(
                    text = stringResource(R.string.board_f3_make_gif),
                    onClick = { makeGif() }
                )
                BoardOutlinedButton(
                    text = stringResource(R.string.board_f3_select_video),
                    onClick = {
                        pickVideo.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }
                )
            } else {
                BoardPrimaryButton(
                    text = stringResource(R.string.board_f3_select_video),
                    onClick = {
                        pickVideo.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }
                )
            }
            BoardOutlinedButton(
                text = stringResource(R.string.board_f3_trim_title),
                onClick = onNext
            )
        }
    }
}

@Composable
private fun VideoTrim(onDone: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), null)
                }
                Text(stringResource(R.string.board_f3_trim_title), fontWeight = FontWeight.Bold)
            }
            BoardHeroImage(resId = BoardFlowArt.motionHero, aspectRatio = 16f / 9f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(radii.md))
                    .background(colors.surfaceMuted)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .fillMaxSize()
                        .padding(vertical = 8.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(radii.sm))
                        .background(colors.primary.copy(alpha = 0.35f))
                        .border(2.dp, colors.primary, RoundedCornerShape(radii.sm))
                )
            }
            Text(stringResource(R.string.board_f3_trim_body), color = colors.textSecondary)
            BoardPrimaryButton(text = stringResource(R.string.board_f3_trim_done), onClick = onDone)
        }
    }
}

@Composable
private fun ProceedAi(onDone: () -> Unit, onCancel: () -> Unit) {
    val colors = BoardTheme.colors
    val steps = listOf(
        R.string.board_f3_step_subject,
        R.string.board_f3_step_scene,
        R.string.board_f3_step_ideas,
        R.string.board_f3_step_ready
    )
    var doneCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        steps.indices.forEach { i ->
            delay(700)
            doneCount = i + 1
        }
        delay(400)
        onDone()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(BoardFlowArt.homeHero),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(PixiSpacing.xl)
                .clip(RoundedCornerShape(BoardTheme.radii.card))
                .background(Color.White)
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.board_f3_understand),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            steps.forEachIndexed { i, res ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    if (i < doneCount) {
                        Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success)
                    } else if (i == doneCount) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(2.dp, colors.textSecondary, CircleShape)
                        )
                    }
                    Text(stringResource(res), color = colors.textPrimary)
                }
            }
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.board_f3_cancel), color = colors.primary)
            }
        }
    }
}

@Composable
private fun EdgeScreen(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    primary: String,
    onPrimary: () -> Unit,
    secondary: String? = null,
    onSecondary: (() -> Unit)? = null,
    tertiary: String? = null,
    tipTitle: String? = null,
    tipBody: String? = null
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(BoardTheme.radii.card))
                    .background(colors.surfaceMuted),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(icon), null, tint = colors.primary, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Text(title, fontWeight = FontWeight.Bold, style = BoardType.title)
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            Text(body, color = colors.textSecondary)
            if (tipTitle != null && tipBody != null) {
                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                BoardTipCallout(title = tipTitle, body = tipBody)
            }
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            BoardPrimaryButton(text = primary, onClick = onPrimary)
            if (secondary != null && onSecondary != null) {
                Spacer(modifier = Modifier.height(PixiSpacing.sm))
                BoardSecondaryButton(text = secondary, onClick = onSecondary)
            }
            if (tertiary != null) {
                Spacer(modifier = Modifier.height(PixiSpacing.sm))
                BoardOutlinedButton(
                    text = tertiary,
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    }
                )
            }
        }
    }
}
