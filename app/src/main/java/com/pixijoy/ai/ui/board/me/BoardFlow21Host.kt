package com.pixijoy.ai.ui.board.me

import androidx.annotation.DrawableRes

import android.widget.Toast
import androidx.annotation.StringRes
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.boardConsentRepository
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.snapshot
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.board.toastSavedOnDevice
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.launch

/** Flow 21 session — personalization toggles persist across visits. */
internal object BoardFlow21Session {
    var suggestions: Boolean = true
    var improveAi: Boolean = true
    var analytics: Boolean = false
    var advertising: Boolean = false
    var cloudSync: Boolean = true
    var keepOriginals: Boolean = true
    var faceData: Boolean = false
}

private data class PrivacyLink(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    val route: String
)

/**
 * Flow 21 — Privacy Center.
 */
@Composable
fun BoardFlow21Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    when (screenId) {
        "b21_hub" -> PrivacyHub(
            onOpen = onNavigate,
            onBack = onBack
        )
        "b21_photos" -> PhotosPrivacy(
            onDone = { onNavigate("b21_hub") },
            onBack = { onNavigate("b21_hub") }
        )
        "b21_data" -> DataStorage(
            onLearnMore = { onNavigate("b21_download") },
            onBack = { onNavigate("b21_hub") }
        )
        "b21_personalization" -> PersonalizationSettings(
            onSaved = { onNavigate("b21_hub") },
            onBack = { onNavigate("b21_hub") }
        )
        "b21_download" -> DownloadOrDelete(
            onDownload = {
                onNavigate("b21_done")
            },
            onDelete = { onNavigate("b21_confirm") },
            onBack = { onNavigate("b21_hub") }
        )
        "b21_confirm" -> ConfirmDeletion(
            onConfirm = { onNavigate("b21_done") },
            onCancel = { onNavigate("b21_download") }
        )
        "b21_done" -> PrivacyDone(
            onBackHub = { onNavigate("b21_hub") },
            onBack = onBack
        )
        else -> PrivacyHub(onOpen = onNavigate, onBack = onBack)
    }
}

@Composable
private fun PrivacyHub(onOpen: (String) -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val links = listOf(
        PrivacyLink(R.drawable.pj_ic_gallery, R.string.flow21_photos, "b21_photos"),
        PrivacyLink(R.drawable.pj_ic_sparkles, R.string.board_f21_ai_processing, "b21_photos"),
        PrivacyLink(R.drawable.pj_ic_storage, R.string.flow21_data, "b21_data"),
        PrivacyLink(R.drawable.pj_ic_tune, R.string.flow21_personalization, "b21_personalization"),
        PrivacyLink(R.drawable.pj_ic_chart, R.string.board_f21_analytics, "b21_personalization"),
        PrivacyLink(R.drawable.pj_ic_megaphone, R.string.board_f21_advertising, "b21_personalization"),
        PrivacyLink(R.drawable.pj_ic_gavel, R.string.board_f21_rights, "b21_download")
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW21_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow21_title),
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(BoardFlowArt.privacyHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.primary.copy(0.12f))
                    .padding(PixiSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                Text(
                    stringResource(R.string.board_f21_matters),
                    style = BoardType.headline,
                    color = colors.textPrimary
                )
                Text(
                    stringResource(R.string.board_f21_matters_body),
                    textAlign = TextAlign.Center,
                    color = colors.textSecondary,
                    style = BoardType.body
                )
            }
            links.forEach { link ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .clickable { onOpen(link.route) }
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Icon(painter = painterResource(link.icon), null, tint = colors.primary)
                    Text(
                        stringResource(link.title),
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                    Icon(painter = painterResource(R.drawable.pj_ic_chevron_right), null, tint = colors.textSecondary)
                }
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun PhotosPrivacy(onDone: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var cloud by remember { mutableStateOf(BoardFlow21Session.cloudSync) }
    var originals by remember { mutableStateOf(BoardFlow21Session.keepOriginals) }
    var face by remember { mutableStateOf(BoardFlow21Session.faceData) }
    val facts = listOf(
        R.string.board_f21_photo_upload,
        R.string.board_f21_photo_used,
        R.string.board_f21_photo_shared,
        R.string.board_f21_photo_delete
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
                    stringResource(R.string.flow21_photos),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(BoardFlowArt.funnyHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            facts.forEach { res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(colors.surfaceMuted)
                        .padding(PixiSpacing.md),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(20.dp))
                    Text(stringResource(res), color = colors.textPrimary)
                }
            }
            ToggleRow(stringResource(R.string.flow21_cloud_sync), cloud) {
                cloud = it
                BoardFlow21Session.cloudSync = it
            }
            ToggleRow(stringResource(R.string.flow21_keep_originals), originals) {
                originals = it
                BoardFlow21Session.keepOriginals = it
            }
            ToggleRow(stringResource(R.string.flow21_face_data), face) {
                face = it
                BoardFlow21Session.faceData = it
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f21_got_it), onClick = onDone)
        }
    }
}

@Composable
private fun DataStorage(onLearnMore: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val items = listOf(
        R.drawable.pj_ic_cloud to R.string.board_f21_data_retention,
        R.drawable.pj_ic_lock to R.string.board_f21_data_location,
        R.drawable.pj_ic_shield to R.string.board_f21_data_encrypt,
        R.drawable.pj_ic_delete to R.string.board_f21_data_auto
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
                    stringResource(R.string.flow21_data),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Text(stringResource(R.string.flow21_storage_used), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Text(stringResource(R.string.flow21_storage_body), color = colors.textSecondary)
            items.forEach { (icon, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Icon(painter = painterResource(icon), null, tint = colors.primary)
                    Text(stringResource(label), color = colors.textPrimary)
                }
            }
            BoardOutlinedButton(
                text = stringResource(R.string.flow21_clear_cache),
                onClick = {
                    Toast.makeText(context, context.getString(R.string.board_f21_cache_cleared), Toast.LENGTH_SHORT).show()
                }
            )
            BoardPrimaryButton(text = stringResource(R.string.board_f21_learn_more), onClick = onLearnMore)
        }
    }
}

@Composable
private fun PersonalizationSettings(onSaved: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val scope = rememberCoroutineScope()
    var suggestions by remember { mutableStateOf(BoardFlow21Session.suggestions) }
    var improveAi by remember { mutableStateOf(BoardFlow21Session.improveAi) }
    var analytics by remember { mutableStateOf(BoardFlow21Session.analytics) }
    var advertising by remember { mutableStateOf(BoardFlow21Session.advertising) }
    var hydrated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val snap = context.boardConsentRepository().snapshot()
        improveAi = snap.aiCloud
        analytics = snap.analytics
        BoardFlow21Session.improveAi = snap.aiCloud
        BoardFlow21Session.analytics = snap.analytics
        hydrated = true
    }
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
                    stringResource(R.string.flow21_personalization),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Text(
                stringResource(R.string.board_saved_on_device),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            ToggleRow(stringResource(R.string.board_f21_toggle_suggestions), suggestions) {
                suggestions = it
                BoardFlow21Session.suggestions = it
            }
            ToggleRow(stringResource(R.string.board_f21_toggle_improve), improveAi) {
                improveAi = it
                BoardFlow21Session.improveAi = it
            }
            ToggleRow(stringResource(R.string.board_f21_toggle_analytics), analytics) {
                analytics = it
                BoardFlow21Session.analytics = it
            }
            ToggleRow(stringResource(R.string.board_f21_toggle_ads), advertising) {
                advertising = it
                BoardFlow21Session.advertising = it
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            BoardPrimaryButton(
                text = stringResource(R.string.board_f21_save_prefs),
                onClick = {
                    if (!hydrated) return@BoardPrimaryButton
                    scope.launch {
                        val consent = context.boardConsentRepository()
                        consent.setAiCloudConsent(improveAi)
                        consent.setAnalyticsConsent(analytics)
                        context.toastSavedOnDevice()
                        onSaved()
                    }
                }
            )
        }
    }
}

@Composable
private fun DownloadOrDelete(
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
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
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f21_download_delete),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.primary.copy(0.1f))
                    .clickable {
                        Toast.makeText(context, context.getString(R.string.board_f21_download_queued), Toast.LENGTH_SHORT).show()
                        onDownload()
                    }
                    .padding(PixiSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_download), null, tint = colors.primary, modifier = Modifier.size(40.dp))
                Text(stringResource(R.string.flow21_download), fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(
                    stringResource(R.string.flow21_download_body),
                    textAlign = TextAlign.Center,
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.danger.copy(0.1f))
                    .clickable(onClick = onDelete)
                    .padding(PixiSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_delete), null, tint = colors.danger, modifier = Modifier.size(40.dp))
                Text(stringResource(R.string.flow21_delete), fontWeight = FontWeight.Bold, color = colors.danger)
                Text(
                    stringResource(R.string.flow21_delete_body),
                    textAlign = TextAlign.Center,
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun ConfirmDeletion(onConfirm: () -> Unit, onCancel: () -> Unit) {
    val colors = BoardTheme.colors
    var understood by remember { mutableStateOf(false) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f21_confirm_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(colors.danger.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_delete), null, tint = colors.danger, modifier = Modifier.size(48.dp))
            }
            Text(
                stringResource(R.string.flow21_delete_confirm),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.flow21_delete_body),
                textAlign = TextAlign.Center,
                color = colors.textSecondary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { understood = !understood },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = understood, onCheckedChange = { understood = it })
                Text(stringResource(R.string.board_f21_understand), color = colors.textPrimary)
            }
            Spacer(modifier = Modifier.weight(1f))
            BoardPrimaryButton(
                text = stringResource(R.string.board_f21_yes_delete),
                onClick = { if (understood) onConfirm() }
            )
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel), color = colors.primary)
            }
        }
    }
}

@Composable
private fun PrivacyDone(onBackHub: () -> Unit, onBack: () -> Unit) {
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
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(colors.successSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(56.dp))
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Text(
                stringResource(R.string.board_f21_all_set),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Text(
                stringResource(R.string.board_f21_all_set_body),
                textAlign = TextAlign.Center,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = PixiSpacing.md)
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_f21_back_hub),
                onClick = onBackHub
            )
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.cd_back), color = colors.primary)
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radii.card))
            .background(colors.surfaceMuted)
            .padding(horizontal = PixiSpacing.lg, vertical = PixiSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), color = colors.textPrimary)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
