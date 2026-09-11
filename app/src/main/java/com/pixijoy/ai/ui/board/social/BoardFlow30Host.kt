package com.pixijoy.ai.ui.board.social

import androidx.annotation.DrawableRes

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.delay

/** Flow 30 session — soft lifecycle demo flags only. */
internal object BoardFlow30Session {
    var sessionSaved: Boolean = true
    var restoredFromCrash: Boolean = false
    var updateInstalled: Boolean = false
}

/**
 * Flow 30 — App Lifecycle / Recovery.
 * Background, update, crash, and restart are simulated UI demos (soft).
 */
@Composable
fun BoardFlow30Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    when (screenId) {
        "b30_hub", "b30_home" -> LifecycleHome(
            onBackground = { onNavigate("b30_bg") },
            onJump = onNavigate,
            onTab = tab
        )
        "b30_bg" -> BackgroundOverlay(
            onGotIt = { onNavigate("b30_welcome") },
            onDismiss = { onNavigate("b30_home") }
        )
        "b30_welcome" -> WelcomeBack(
            onContinue = { onNavigate("b30_update") },
            onFresh = { onNavigate("b30_home") },
            onTab = tab
        )
        "b30_update" -> UpdateAvailable(
            onUpdate = {
                BoardFlow30Session.updateInstalled = true
                onNavigate("b30_restart")
            },
            onLater = { onNavigate("b30_home") },
            onBack = { onNavigate("b30_welcome") }
        )
        "b30_restart" -> AppRestart(
            onContinue = { onNavigate("b30_crash") },
            onBack = { onNavigate("b30_update") }
        )
        "b30_crash" -> CrashRecovery(
            onRestore = {
                BoardFlow30Session.restoredFromCrash = true
                onNavigate("b30_welcome")
            },
            onDrafts = { onNavigate("b18_browse") },
            onFresh = { onNavigate("b30_device") },
            onBack = { onNavigate("b30_restart") }
        )
        "b30_device" -> DeviceRestart(
            onContinue = { onNavigate("b30_integrity") },
            onBack = { onNavigate("b30_crash") }
        )
        "b30_integrity" -> DataIntegrity(
            onDone = onBack,
            onHome = { onNavigate("b30_home") }
        )
        else -> LifecycleHome(
            onBackground = { onNavigate("b30_bg") },
            onJump = onNavigate,
            onTab = tab
        )
    }
}

@Composable
private fun LifecycleHome(
    onBackground: () -> Unit,
    onJump: (String) -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardMainShell(selected = BoardShellTab.Home, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW30_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(BoardFlowArt.logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onJump("b25_center") }) {
                    Icon(painter = painterResource(R.drawable.pj_ic_notifications),
                        contentDescription = stringResource(R.string.flow25_title)
                    )
                }
                Image(
                    painter = painterResource(BoardFlowArt.profileAvatar),
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
            }
            Box {
                Image(
                    painter = painterResource(BoardFlowArt.homeHero),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(radii.card)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    stringResource(R.string.board_f30_hero),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(PixiSpacing.md),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                QuickTile(
                    R.drawable.pj_ic_camera,
                    R.string.camera,
                    Modifier.weight(1f)
                ) { onJump("b3_camera") }
                QuickTile(
                    R.drawable.pj_ic_gallery,
                    R.string.gallery,
                    Modifier.weight(1f)
                ) { onJump("b3_gallery") }
                QuickTile(
                    R.drawable.pj_ic_grid,
                    R.string.board_f30_templates,
                    Modifier.weight(1f)
                ) { onJump("b2_search") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.board_f30_try_new),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.see_all),
                    color = colors.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                listOf(
                    R.string.board_f30_chip_ai to BoardFlowArt.beautifulHero,
                    R.string.board_f30_chip_funny to BoardFlowArt.funnyHero,
                    R.string.board_f30_chip_collage to BoardFlowArt.collageHero,
                    R.string.board_f30_chip_stickers to BoardFlowArt.stickerHero
                ).forEach { (label, art) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(art),
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.flow30_demo_hint),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                listOf(
                    R.string.flow30_demo_bg to "b30_bg",
                    R.string.flow30_demo_update to "b30_update",
                    R.string.flow30_demo_crash to "b30_crash",
                    R.string.flow30_device_title to "b30_device",
                    R.string.flow30_integrity_title to "b30_integrity"
                ).forEach { (res, id) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(colors.surfaceMuted)
                            .clickable { onJump(id) }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(res),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textPrimary
                        )
                    }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.board_f30_simulate_bg),
                onClick = onBackground
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun QuickTile(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(radii.md))
            .background(colors.surfaceMuted)
            .clickable(onClick = onClick)
            .padding(PixiSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(painter = painterResource(icon), null, tint = colors.primary, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(PixiSpacing.xs))
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun BackgroundOverlay(onGotIt: () -> Unit, onDismiss: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TestTags.FLOW30_HOST)
        ) {
            Image(
                painter = painterResource(BoardFlowArt.homeHero),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.35f
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onDismiss)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(PixiSpacing.xl)
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.surface)
                    .padding(PixiSpacing.xl)
                    .clickable(enabled = false) {},
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                Image(
                    painter = painterResource(BoardFlowArt.logo),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    stringResource(R.string.flow30_bg_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.flow30_bg_body),
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                CheckLine(stringResource(R.string.flow30_session_saved))
                CheckLine(stringResource(R.string.flow30_quick_resume))
                BoardPrimaryButton(text = stringResource(R.string.flow30_got_it), onClick = onGotIt)
            }
        }
    }
}

@Composable
private fun WelcomeBack(
    onContinue: () -> Unit,
    onFresh: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardMainShell(selected = BoardShellTab.Home, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW30_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Text(
                stringResource(R.string.flow30_welcome_back),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Text(stringResource(R.string.flow30_pick_up), color = colors.textSecondary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.md))
                    .background(colors.surfaceMuted)
                    .clickable(onClick = onContinue)
                    .padding(PixiSpacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(BoardFlowArt.creationThumb),
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(radii.sm)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.size(PixiSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.flow30_continue_edit),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        stringResource(R.string.board_f30_last_edited),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                Icon(painter = painterResource(R.drawable.pj_ic_chevron_right), null, tint = colors.textSecondary)
            }
            Text(
                stringResource(R.string.board_f30_recent),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                BoardFlowArt.homeRecents.take(3).forEach { art ->
                    Image(
                        painter = painterResource(art),
                        contentDescription = null,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(radii.md)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.flow30_continue), onClick = onContinue)
            TextButton(onClick = onFresh, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.flow30_start_fresh), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun UpdateAvailable(
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW30_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow30_update_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(BoardFlowArt.aiSuccess),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(BoardTheme.radii.card))
                    .align(Alignment.CenterHorizontally),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.flow30_update_version),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = colors.textSecondary,
                style = BoardType.caption
            )
            FeatureRow(R.drawable.pj_ic_sparkles, R.string.flow30_feat1)
            FeatureRow(R.drawable.pj_ic_grid, R.string.flow30_feat2)
            FeatureRow(R.drawable.pj_ic_speed, R.string.flow30_feat3)
            FeatureRow(R.drawable.pj_ic_sync, R.string.flow30_feat4)
            Spacer(modifier = Modifier.weight(1f))
            BoardPrimaryButton(text = stringResource(R.string.flow30_update_now), onClick = onUpdate)
            TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.flow30_later), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun FeatureRow(@DrawableRes icon: Int, @StringRes label: Int) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radii.md))
            .background(colors.surfaceMuted)
            .padding(PixiSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
    ) {
        Icon(painter = painterResource(icon), null, tint = colors.primary, modifier = Modifier.size(24.dp))
        Text(stringResource(label), color = colors.textPrimary)
    }
}

@Composable
private fun AppRestart(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var progress by remember { mutableFloatStateOf(0f) }
    var step by remember { mutableIntStateOf(0) }
    val animated by animateFloatAsState(progress, tween(400), label = "restart")
    val steps = listOf(
        R.string.flow30_load_data,
        R.string.flow30_restore_settings,
        R.string.flow30_sync_creations,
        R.string.board_f30_almost
    )
    LaunchedEffect(Unit) {
        for (i in steps.indices) {
            step = i + 1
            progress = (i + 1f) / steps.size
            delay(450)
        }
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW30_HOST),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(BoardFlowArt.logo),
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Text(
                stringResource(R.string.flow30_restart_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.flow30_restart_body),
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            steps.forEachIndexed { index, res ->
                if (index < step) {
                    CheckLine(stringResource(res))
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(radii.pill)),
                color = colors.primary,
                trackColor = colors.surfaceMuted
            )
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.board_f30_moment), color = colors.textSecondary)
                Icon(painter = painterResource(R.drawable.pj_ic_heart),
                    null,
                    tint = colors.danger,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            if (progress >= 0.99f) {
                BoardPrimaryButton(
                    text = stringResource(R.string.flow30_continue_app),
                    onClick = onContinue
                )
            }
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.cd_back), color = colors.primary)
            }
        }
    }
}

@Composable
private fun CrashRecovery(
    onRestore: () -> Unit,
    onDrafts: () -> Unit,
    onFresh: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW30_HOST),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
                Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
            }
            Image(
                painter = painterResource(BoardFlowArt.aiFail),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.flow30_crash_title),
                style = BoardType.headline,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.flow30_crash_body),
                style = BoardType.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            CrashOption(
                R.drawable.pj_ic_history,
                R.string.flow30_restore_session,
                R.string.board_f30_recover_unsaved,
                onRestore
            )
            CrashOption(
                R.drawable.pj_ic_document,
                R.string.flow30_view_drafts,
                null,
                onDrafts
            )
            CrashOption(
                R.drawable.pj_ic_bug,
                R.string.flow30_send_report,
                null,
                {}
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardPrimaryButton(
                text = stringResource(R.string.flow30_restore_continue),
                onClick = onRestore
            )
            TextButton(onClick = onFresh, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.flow30_start_fresh), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun CrashOption(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes subtitle: Int?,
    onClick: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radii.md))
            .background(colors.surfaceMuted)
            .clickable(onClick = onClick)
            .padding(PixiSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
    ) {
        Icon(painter = painterResource(icon), null, tint = colors.primary, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(title), fontWeight = FontWeight.Medium, color = colors.textPrimary)
            if (subtitle != null) {
                Text(
                    stringResource(subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
        Icon(painter = painterResource(R.drawable.pj_ic_chevron_right), null, tint = colors.textSecondary)
    }
}

@Composable
private fun DeviceRestart(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    var step by remember { mutableIntStateOf(0) }
    val checks = listOf(
        R.string.flow30_restore_data,
        R.string.flow30_sync_cloud,
        R.string.flow30_load_drafts,
        R.string.flow30_all_set
    )
    LaunchedEffect(Unit) {
        for (i in checks.indices) {
            delay(400)
            step = i + 1
        }
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW30_HOST),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(BoardFlowArt.logo),
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Text(
                stringResource(R.string.flow30_device_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            Text(
                stringResource(R.string.board_f30_still_here),
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            checks.forEachIndexed { index, res ->
                if (index < step) CheckLine(stringResource(res))
            }
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            if (step >= checks.size) {
                BoardPrimaryButton(
                    text = stringResource(R.string.flow30_continue_app),
                    onClick = onContinue
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = PixiSpacing.md)
            ) {
                Text(stringResource(R.string.board_f30_nothing_lost), color = colors.textSecondary)
                Icon(painter = painterResource(R.drawable.pj_ic_heart),
                    null,
                    tint = colors.danger,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp)
                )
            }
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.cd_back), color = colors.primary)
            }
        }
    }
}

@Composable
private fun DataIntegrity(onDone: () -> Unit, onHome: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW30_HOST),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Image(
                painter = painterResource(BoardFlowArt.aiSuccess),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.flow30_integrity_title),
                style = BoardType.headline,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.board_f30_data_safe),
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                IntegrityTile(R.drawable.pj_ic_save, R.string.flow30_autosave, Modifier.weight(1f))
                IntegrityTile(R.drawable.pj_ic_check_circle, R.string.flow30_cloud_sync, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                IntegrityTile(R.drawable.pj_ic_sync, R.string.flow30_local_backup, Modifier.weight(1f))
                IntegrityTile(R.drawable.pj_ic_history, R.string.flow30_easy_recovery, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            BoardPrimaryButton(text = stringResource(R.string.flow30_great), onClick = onDone)
            BoardOutlinedButton(text = stringResource(R.string.flow30_home), onClick = onHome)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun IntegrityTile(@DrawableRes icon: Int, @StringRes label: Int, modifier: Modifier = Modifier) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(radii.md))
            .background(colors.surfaceMuted)
            .padding(PixiSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(painter = painterResource(icon), null, tint = colors.primary, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(PixiSpacing.sm))
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CheckLine(text: String) {
    val colors = BoardTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
    ) {
        Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(20.dp))
        Text(text, color = colors.textPrimary)
    }
}
