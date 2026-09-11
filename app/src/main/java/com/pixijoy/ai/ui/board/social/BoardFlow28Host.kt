package com.pixijoy.ai.ui.board.social

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.pixijoy.ai.ui.board.components.BoardSoftProgress
import com.pixijoy.ai.ui.board.components.BoardSoftPulse
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.delay

/** Flow 28 session — soft offline/sync demo state. */
internal object BoardFlow28Session {
    var draftCount: Int = 3
    var lastSyncFailed: Boolean = false
}

/**
 * Flow 28 — Offline / Network Recovery.
 * Sync progress is simulated (soft); no real network upload.
 */
@Composable
fun BoardFlow28Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    when (screenId) {
        "b28_hub", "b28_banner" -> OfflineHome(
            onBanner = { onNavigate("b28_modal") },
            onTab = tab
        )
        "b28_modal" -> OfflineModal(
            onContinue = { onNavigate("b28_edit") },
            onDrafts = { onNavigate("b28_saved") },
            onBack = { onNavigate("b28_banner") }
        )
        "b28_edit" -> OfflineEdit(
            onSave = { onNavigate("b28_saved") },
            onBack = { onNavigate("b28_modal") }
        )
        "b28_saved" -> SavedOffline(
            onDrafts = { onNavigate("b18_browse") },
            onContinue = { onNavigate("b28_edit") },
            onOnline = { onNavigate("b28_online") },
            onBack = onBack
        )
        "b28_online" -> BackOnline(
            onSync = { onNavigate("b28_syncing") },
            onLater = onBack,
            onBack = { onNavigate("b28_saved") }
        )
        "b28_syncing" -> Syncing(
            onDone = {
                BoardFlow28Session.lastSyncFailed = false
                onNavigate("b28_synced")
            },
            onFail = {
                BoardFlow28Session.lastSyncFailed = true
                onNavigate("b28_failed")
            },
            onBack = { onNavigate("b28_online") }
        )
        "b28_synced" -> Synced(
            onCreations = { onNavigate("b18_browse") },
            onCommunity = { onNavigate("b27_feed") },
            onBack = onBack
        )
        "b28_failed" -> SyncFailed(
            onRetry = { onNavigate("b28_syncing") },
            onDrafts = { onNavigate("b18_browse") },
            onHelp = { onNavigate("b11_help") },
            onBack = onBack
        )
        else -> OfflineHome(
            onBanner = { onNavigate("b28_modal") },
            onTab = tab
        )
    }
}

@Composable
private fun OfflineHome(onBanner: () -> Unit, onTab: (BoardShellTab) -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardMainShell(selected = BoardShellTab.Home, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW28_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.md))
                    .background(colors.danger.copy(0.14f))
                    .clickable(onClick = onBanner)
                    .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_wifi_off), null, tint = colors.danger, modifier = Modifier.size(22.dp))
                Text(
                    stringResource(R.string.flow28_no_internet),
                    color = colors.danger,
                    style = BoardType.body,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
            Image(
                painter = painterResource(BoardFlowArt.offlineHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.board_f28_keep_creating),
                style = BoardType.title,
                color = colors.textPrimary
            )
            Text(
                stringResource(R.string.flow28_home_offline),
                style = BoardType.body,
                color = colors.textSecondary
            )
            BoardPrimaryButton(text = stringResource(R.string.flow28_got_it), onClick = onBanner)
        }
    }
}

@Composable
private fun OfflineModal(onContinue: () -> Unit, onDrafts: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val ok = listOf(
        R.string.flow28_ok1,
        R.string.flow28_ok2,
        R.string.flow28_ok3,
        R.string.flow28_ok4
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW28_HOST),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            Image(
                painter = painterResource(BoardFlowArt.offlineHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(BoardTheme.radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.flow28_offline_title),
                style = BoardType.headline,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.flow28_offline_body),
                style = BoardType.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            ok.forEach { res ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(20.dp))
                    Text(stringResource(res), color = colors.textPrimary)
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            BoardPrimaryButton(text = stringResource(R.string.flow28_continue), onClick = onContinue)
            BoardOutlinedButton(text = stringResource(R.string.flow28_manage_drafts), onClick = onDrafts)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun OfflineEdit(onSave: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var tool by remember { mutableIntStateOf(0) }
    val tools = listOf(
        R.drawable.pj_ic_filter to R.string.board_f28_tool_filters,
        R.drawable.pj_ic_tune to R.string.board_f28_tool_adjust,
        R.drawable.pj_ic_text to R.string.board_f28_tool_text,
        R.drawable.pj_ic_emoji to R.string.board_f28_tool_stickers,
        R.drawable.pj_ic_crop to R.string.board_f28_tool_crop
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow28_edit_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.flow28_offline_pill),
                    modifier = Modifier
                        .clip(RoundedCornerShape(radii.pill))
                        .background(colors.danger.copy(0.12f))
                        .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.xs),
                    color = colors.danger,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                Image(
                    painter = painterResource(BoardFlowArt.offlineHero),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Text(
                    stringResource(R.string.board_f28_good_vibes),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(PixiSpacing.lg),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Text(stringResource(R.string.board_f28_edit_note), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tools.forEachIndexed { i, (icon, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.md))
                            .background(if (tool == i) colors.primary.copy(0.12f) else Color.Transparent)
                            .clickable { tool = i }
                            .padding(PixiSpacing.sm)
                    ) {
                        Icon(painter = painterResource(icon), null, tint = if (tool == i) colors.primary else colors.textSecondary)
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (tool == i) colors.primary else colors.textSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            BoardPrimaryButton(text = stringResource(R.string.flow28_save_draft), onClick = onSave)
        }
    }
}

@Composable
private fun SavedOffline(
    onDrafts: () -> Unit,
    onContinue: () -> Unit,
    onOnline: () -> Unit,
    onBack: () -> Unit
) {
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
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_document), null, tint = colors.primary, modifier = Modifier.size(48.dp))
                Icon(painter = painterResource(R.drawable.pj_ic_check_circle),
                    null,
                    tint = colors.success,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Text(
                stringResource(R.string.flow28_saved_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Text(
                stringResource(R.string.flow28_saved_body),
                textAlign = TextAlign.Center,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = PixiSpacing.md)
            )
            BoardPrimaryButton(text = stringResource(R.string.flow28_view_drafts), onClick = onDrafts)
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardOutlinedButton(text = stringResource(R.string.flow28_continue), onClick = onContinue)
            TextButton(onClick = onOnline) {
                Text(stringResource(R.string.board_f28_simulate_online), color = colors.primary)
            }
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.cd_back), color = colors.textSecondary)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun BackOnline(onSync: () -> Unit, onLater: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val pending = listOf(
        R.string.flow28_photos_n,
        R.string.flow28_videos_n,
        R.string.flow28_templates_n
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(colors.successSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_wifi), null, tint = colors.success, modifier = Modifier.size(56.dp))
            }
            Text(
                stringResource(R.string.flow28_back_online),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.flow28_found_items),
                textAlign = TextAlign.Center,
                color = colors.textSecondary
            )
            pending.forEach { res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Image(
                        painter = painterResource(BoardFlowArt.offlineHero),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(radii.md)),
                        contentScale = ContentScale.Crop
                    )
                    Text(stringResource(res), fontWeight = FontWeight.Medium, color = colors.textPrimary)
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.flow28_sync_now), onClick = onSync)
            BoardOutlinedButton(text = stringResource(R.string.flow28_sync_later), onClick = onLater)
        }
    }
}

@Composable
private fun Syncing(onDone: () -> Unit, onFail: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var tick by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (tick < 1f) {
            delay(120)
            tick = (tick + 0.06f).coerceAtMost(1f)
        }
        delay(400)
        onDone()
    }
    val progress by animateFloatAsState(tick, animationSpec = tween(280), label = "sync")
    val fileNames = remember {
        listOf(
            R.string.board_f28_file_beach to R.string.board_f28_uploading,
            R.string.board_f28_file_sunset to R.string.board_f28_uploading,
            R.string.board_f28_file_cat to R.string.board_f28_preparing,
            R.string.board_f28_file_card to R.string.board_f28_waiting
        )
    }
    val fileProgress = remember(progress) {
        floatArrayOf(
            progress.coerceAtMost(1f),
            (progress * 0.55f).coerceAtMost(0.55f),
            (progress * 0.15f).coerceAtMost(0.15f),
            0f
        )
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW28_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow28_syncing),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            BoardSoftPulse(
                active = progress < 1f,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_upload),
                    null,
                    tint = colors.primary,
                    modifier = Modifier.size(64.dp)
                )
            }
            fileNames.forEachIndexed { index, (name, status) ->
                val pct = fileProgress[index]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .padding(PixiSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(PixiSpacing.xs)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(name),
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (pct == 0f) {
                            Icon(painter = painterResource(R.drawable.pj_ic_time), null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            stringResource(status),
                            color = colors.textSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    BoardSoftProgress(progress = pct, darkSurface = false)
                }
            }
            Text(stringResource(R.string.flow28_sync_hint), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            Text(
                stringResource(R.string.flow28_sync_auto_hint),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = onFail) {
                Text(stringResource(R.string.flow28_demo_fail), color = colors.danger)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun Synced(onCreations: () -> Unit, onCommunity: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val done = listOf(
        R.string.board_f28_done_photos,
        R.string.board_f28_done_videos,
        R.string.board_f28_done_templates
    )
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
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(colors.successSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(56.dp))
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Text(
                stringResource(R.string.flow28_synced_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Text(
                stringResource(R.string.board_f28_caught_up),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = PixiSpacing.sm)
            )
            Text(
                stringResource(R.string.flow28_synced_body),
                textAlign = TextAlign.Center,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = PixiSpacing.md)
            )
            done.forEach { res ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(18.dp))
                    Text(stringResource(res), color = colors.textPrimary)
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            BoardPrimaryButton(text = stringResource(R.string.flow8_my_creations), onClick = onCreations)
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardOutlinedButton(text = stringResource(R.string.board_f28_explore_community), onClick = onCommunity)
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.cd_back), color = colors.textSecondary)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun SyncFailed(
    onRetry: () -> Unit,
    onDrafts: () -> Unit,
    onHelp: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val fails = listOf(
        R.string.board_f28_fail_beach to R.string.board_f28_err_timeout,
        R.string.board_f28_fail_card to R.string.board_f28_err_server
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
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow28_failed_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(colors.danger.copy(0.12f))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_cloud_off), null, tint = colors.danger, modifier = Modifier.size(48.dp))
            }
            Text(
                stringResource(R.string.board_f28_couldnt_sync),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.flow28_failed_body),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            fails.forEach { (file, err) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(file), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text(stringResource(err), color = colors.danger, style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = onRetry) {
                        Icon(painter = painterResource(R.drawable.pj_ic_refresh),
                            contentDescription = stringResource(R.string.cd_retry),
                            tint = colors.primary
                        )
                    }
                }
            }
            Text(stringResource(R.string.board_s28_fail_tip), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            BoardPrimaryButton(text = stringResource(R.string.flow28_retry), onClick = onRetry)
            BoardOutlinedButton(text = stringResource(R.string.flow28_manage_drafts), onClick = onDrafts)
            TextButton(onClick = onHelp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painter = painterResource(R.drawable.pj_ic_help), null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(PixiSpacing.sm))
                    Text(stringResource(R.string.board_f28_get_help), color = colors.primary)
                }
            }
            BoardTrustFooter()
        }
    }
}
