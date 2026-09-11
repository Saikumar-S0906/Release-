package com.pixijoy.ai.ui.board.me

import androidx.annotation.DrawableRes

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardNavPrefs
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.boardConsentRepository
import com.pixijoy.ai.ui.board.rememberKidsSpace
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardCaptionText
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardScreenHeader
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.snapshot
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.board.toastSavedOnDevice
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.launch

/** Flow 11 session — settings toggles / account fields. */
internal object BoardFlow11Session {
    var email: String = "saikumar@example.com"
    var phone: String = "+91 98765 43210"
    var passwordMasked: Boolean = true
    var defaultStyle: Int = 0
    var imageQuality: Int = 1
    var saveOriginal: Boolean = true
    var autoEnhance: Boolean = true
    var suggestPrompts: Boolean = true
    var haptic: Boolean = false
    var notifAppUpdates: Boolean = true
    var notifLikes: Boolean = true
    var notifFollowers: Boolean = true
    var notifMentions: Boolean = false
    var notifCommunity: Boolean = true
    var notifMarketing: Boolean = false
    var appearance: Int = 2
    var language: Int = 0
    var cacheCleared: Boolean = false
}

/**
 * Flow 11 — Settings & Help.
 * Board menu shows 5-tab chrome (My Stuff active); bible labels Home · Explore · Create · Community · My Stuff.
 */
@Composable
fun BoardFlow11Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    when (screenId) {
        "b11_hub" -> OpenMenu(
            onTab = tab,
            onProfile = { onNavigate("b9_open") },
            onCreations = { onNavigate("b18_browse") },
            onFavorites = { onNavigate("b9_favorites") },
            onSettings = { onNavigate("b11_home") },
            onHelp = { onNavigate("b11_help") },
            onCommunity = { onNavigate("b27_feed") }
        )
        "b11_home" -> SettingsHome(
            onBack = { onNavigate("b11_hub") },
            onAccount = { onNavigate("b11_account") },
            onPrefs = { onNavigate("b11_prefs") },
            onNotifs = { onNavigate("b11_notifs") },
            onPrivacy = { onNavigate("b21_hub") },
            onStorage = { onNavigate("b11_storage") },
            onLanguage = { onNavigate("b11_language") },
            onAppearance = { onNavigate("b11_appearance") },
            onHelp = { onNavigate("b11_help") }
        )
        "b11_account" -> AccountSettings(
            onBack = { onNavigate("b11_home") },
            onDelete = { onNavigate("b11_delete") },
            onTheme = {
                BoardFlow9Session.themeReturnId = "b11_account"
                onNavigate("b9_theme")
            }
        )
        "b11_prefs" -> PreferencesSettings(
            onBack = { onNavigate("b11_home") }
        )
        "b11_notifs" -> NotificationSettings(
            onBack = { onNavigate("b11_home") }
        )
        "b11_help" -> HelpSupport(
            onBack = { onNavigate("b11_home") },
            onArticle = { onNavigate("b11_article") }
        )
        "b11_article" -> HelpArticle(
            onBack = { onNavigate("b11_help") },
            onTry = { onNavigate("b3_open") }
        )
        "b11_delete" -> DeleteAccount(
            onConfirm = { onNavigate("b11_hub") },
            onCancel = { onNavigate("b11_account") }
        )
        "b11_storage" -> StorageSettings(onBack = { onNavigate("b11_home") })
        "b11_language" -> LanguageSettings(onBack = { onNavigate("b11_home") })
        "b11_appearance" -> AppearanceSettings(onBack = { onNavigate("b11_home") })
        else -> SettingsHome(
            onBack = onBack,
            onAccount = { onNavigate("b11_account") },
            onPrefs = { onNavigate("b11_prefs") },
            onNotifs = { onNavigate("b11_notifs") },
            onPrivacy = { onNavigate("b21_hub") },
            onStorage = { onNavigate("b11_storage") },
            onLanguage = { onNavigate("b11_language") },
            onAppearance = { onNavigate("b11_appearance") },
            onHelp = { onNavigate("b11_help") }
        )
    }
}

@Composable
private fun OpenMenu(
    onTab: (BoardShellTab) -> Unit,
    onProfile: () -> Unit,
    onCreations: () -> Unit,
    onFavorites: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onCommunity: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardMainShell(selected = BoardShellTab.MyStuff, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW11_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Image(
                painter = painterResource(BoardFlowArt.logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(36.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.surfaceMuted)
                    .clickable(onClick = onProfile)
                    .padding(PixiSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                Image(
                    painter = painterResource(BoardFlow9Session.avatarRes()),
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        BoardFlow9Session.name.ifBlank { stringResource(R.string.nav_profile) },
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        stringResource(R.string.board_f11_view_profile),
                        color = colors.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Icon(painter = painterResource(R.drawable.pj_ic_chevron_right), null, tint = colors.textSecondary)
            }
            MenuItem(R.drawable.pj_ic_folder, R.string.flow8_my_creations, onCreations)
            MenuItem(R.drawable.pj_ic_heart, R.string.flow9_favorites, onFavorites)
            MenuItem(R.drawable.pj_ic_settings, R.string.board_f11_settings, onSettings)
            MenuItem(R.drawable.pj_ic_help, R.string.flow11_help, onHelp)
            MenuItem(R.drawable.pj_ic_share, R.string.board_f11_invite) {
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.board_f11_invite_text))
                        },
                        context.getString(R.string.board_f11_invite)
                    )
                )
            }
            MenuItem(R.drawable.pj_ic_star, R.string.board_f11_rate) {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                    )
                } catch (_: Exception) {
                    Toast.makeText(context, context.getString(R.string.board_f11_rate_toast), Toast.LENGTH_SHORT).show()
                }
            }
            MenuItem(R.drawable.pj_ic_info, R.string.board_f11_about) {
                Toast.makeText(context, context.getString(R.string.board_f11_about_body), Toast.LENGTH_LONG).show()
            }
            TextButton(onClick = onCommunity) {
                Text(stringResource(R.string.flow8_view_community), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun SettingsHome(
    onBack: () -> Unit,
    onAccount: () -> Unit,
    onPrefs: () -> Unit,
    onNotifs: () -> Unit,
    onPrivacy: () -> Unit,
    onStorage: () -> Unit,
    onLanguage: () -> Unit,
    onAppearance: () -> Unit,
    onHelp: () -> Unit
) {
    val colors = BoardTheme.colors
    val items = listOf(
        SettingsCat(R.drawable.pj_ic_person, R.string.flow11_account, R.string.board_f11_account_sub, onAccount),
        SettingsCat(R.drawable.pj_ic_tune, R.string.flow11_prefs, R.string.board_f11_prefs_sub, onPrefs),
        SettingsCat(R.drawable.pj_ic_notifications, R.string.flow11_notifications, R.string.board_f11_notifs_sub, onNotifs),
        SettingsCat(R.drawable.pj_ic_lock, R.string.board_f11_privacy, R.string.board_f11_privacy_sub, onPrivacy),
        SettingsCat(R.drawable.pj_ic_storage, R.string.board_f11_storage, R.string.board_f11_storage_sub, onStorage),
        SettingsCat(R.drawable.pj_ic_language, R.string.flow11_language, R.string.board_f11_language_sub, onLanguage),
        SettingsCat(R.drawable.pj_ic_brightness, R.string.board_f11_appearance, R.string.board_f11_appearance_sub, onAppearance),
        SettingsCat(R.drawable.pj_ic_help, R.string.flow11_help, R.string.board_f11_help_sub, onHelp)
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW11_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            BoardScreenHeader(title = stringResource(R.string.board_f11_settings), onBack = onBack)
            Image(
                painter = painterResource(BoardFlowArt.settingsHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(BoardTheme.radii.card)),
                contentScale = ContentScale.Crop
            )
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BoardTheme.radii.md))
                        .background(colors.surfaceMuted)
                        .clickable(onClick = item.onClick)
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Icon(painter = painterResource(item.icon), null, tint = colors.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(item.title), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text(stringResource(item.sub), color = colors.textSecondary, style = BoardType.caption)
                    }
                    Icon(painter = painterResource(R.drawable.pj_ic_chevron_right), null, tint = colors.textSecondary)
                }
            }
        }
    }
}

private data class SettingsCat(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    @StringRes val sub: Int,
    val onClick: () -> Unit
)

@Composable
private fun AccountSettings(
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onTheme: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var name by remember { mutableStateOf(BoardFlow9Session.name) }
    var email by remember { mutableStateOf(BoardFlow11Session.email) }
    var phone by remember { mutableStateOf(BoardFlow11Session.phone) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            BoardScreenHeader(title = stringResource(R.string.flow11_account), onBack = onBack)
            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Image(
                    painter = painterResource(BoardFlow9Session.avatarRes()),
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                        .clickable(onClick = onTheme),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_camera), null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    BoardFlow9Session.name = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.flow9_name)) },
                shape = RoundedCornerShape(radii.md)
            )
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    BoardFlow11Session.email = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.flow11_email)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(radii.md)
            )
            OutlinedTextField(
                value = "•••••••••••",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.flow11_password)) },
                trailingIcon = {
                    Icon(painter = painterResource(R.drawable.pj_ic_edit), null, tint = colors.primary)
                },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(radii.md)
            )
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    BoardFlow11Session.phone = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.board_f11_phone)) },
                trailingIcon = {
                    Icon(painter = painterResource(R.drawable.pj_ic_phone), null, tint = colors.textSecondary)
                },
                shape = RoundedCornerShape(radii.md)
            )
            Text(stringResource(R.string.board_f11_connected), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                listOf("G", "A", "f").forEach { label ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceMuted),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.flow11_delete), color = colors.danger)
            }
            BoardPrimaryButton(
                text = stringResource(R.string.flow9_save),
                onClick = onBack
            )
        }
    }
}

@Composable
private fun PreferencesSettings(onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kidsSpace = rememberKidsSpace()
    var kids by remember(kidsSpace) { mutableStateOf(kidsSpace) }
    LaunchedEffect(kidsSpace) { kids = kidsSpace }
    var style by remember { mutableIntStateOf(BoardFlow11Session.defaultStyle) }
    var quality by remember { mutableIntStateOf(BoardFlow11Session.imageQuality) }
    var saveOriginal by remember { mutableStateOf(BoardFlow11Session.saveOriginal) }
    var autoEnhance by remember { mutableStateOf(BoardFlow11Session.autoEnhance) }
    var suggest by remember { mutableStateOf(BoardFlow11Session.suggestPrompts) }
    var haptic by remember { mutableStateOf(BoardFlow11Session.haptic) }
    val styles = listOf(
        R.string.board_f11_style_anime,
        R.string.board_f11_style_natural,
        R.string.board_f11_style_cinematic
    )
    val qualities = listOf(
        R.string.board_f11_quality_std,
        R.string.board_f11_quality_high,
        R.string.board_f11_quality_4k
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            BoardScreenHeader(title = stringResource(R.string.flow11_prefs), onBack = onBack)
            PrefRow(
                label = stringResource(R.string.board_f11_default_style),
                value = stringResource(styles[style % styles.size])
            ) {
                style = (style + 1) % styles.size
                BoardFlow11Session.defaultStyle = style
            }
            PrefRow(
                label = stringResource(R.string.board_f11_image_quality),
                value = stringResource(qualities[quality % qualities.size])
            ) {
                quality = (quality + 1) % qualities.size
                BoardFlow11Session.imageQuality = quality
            }
            ToggleRow(R.string.board_f11_save_original, saveOriginal) {
                saveOriginal = it
                BoardFlow11Session.saveOriginal = it
            }
            ToggleRow(R.string.board_f11_auto_enhance, autoEnhance) {
                autoEnhance = it
                BoardFlow11Session.autoEnhance = it
            }
            ToggleRow(R.string.board_f11_suggest_prompts, suggest) {
                suggest = it
                BoardFlow11Session.suggestPrompts = it
            }
            ToggleRow(R.string.board_f11_haptic, haptic) {
                haptic = it
                BoardFlow11Session.haptic = it
            }
            ToggleRow(R.string.board_kids_space, kids) {
                kids = it
                scope.launch { BoardNavPrefs.setKidsSpace(context, it) }
            }
            BoardCaptionText(
                text = stringResource(R.string.board_kids_space_sub),
                color = colors.textSecondary
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_f11_reset_defaults),
                onClick = {
                    style = 0
                    quality = 1
                    saveOriginal = true
                    autoEnhance = true
                    suggest = true
                    haptic = false
                    BoardFlow11Session.defaultStyle = 0
                    BoardFlow11Session.imageQuality = 1
                    BoardFlow11Session.saveOriginal = true
                    BoardFlow11Session.autoEnhance = true
                    BoardFlow11Session.suggestPrompts = true
                    BoardFlow11Session.haptic = false
                }
            )
        }
    }
}

@Composable
private fun PrefRow(label: String, value: String, onClick: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radii.md))
            .background(colors.surfaceMuted)
            .clickable(onClick = onClick)
            .padding(PixiSpacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium, color = colors.textPrimary)
            Text(value, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Icon(painter = painterResource(R.drawable.pj_ic_chevron_right), null, tint = colors.textSecondary)
    }
}

@Composable
private fun NotificationSettings(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var app by remember { mutableStateOf(BoardFlow11Session.notifAppUpdates) }
    var likes by remember { mutableStateOf(BoardFlow11Session.notifLikes) }
    var followers by remember { mutableStateOf(BoardFlow11Session.notifFollowers) }
    var mentions by remember { mutableStateOf(BoardFlow11Session.notifMentions) }
    var community by remember { mutableStateOf(BoardFlow11Session.notifCommunity) }
    var marketing by remember { mutableStateOf(BoardFlow11Session.notifMarketing) }
    LaunchedEffect(Unit) {
        val enabled = context.boardConsentRepository().snapshot().notifications
        app = enabled
        BoardFlow11Session.notifAppUpdates = enabled
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            BoardScreenHeader(title = stringResource(R.string.flow11_notifications), onBack = onBack)
            Text(
                stringResource(R.string.board_saved_on_device),
                style = MaterialTheme.typography.bodySmall,
                color = BoardTheme.colors.textSecondary
            )
            ToggleRow(R.string.board_f11_n_app, app) {
                app = it
                BoardFlow11Session.notifAppUpdates = it
                scope.launch {
                    context.boardConsentRepository().setNotificationsEnabled(it)
                    context.toastSavedOnDevice()
                }
            }
            ToggleRow(R.string.board_f11_n_likes, likes) {
                likes = it
                BoardFlow11Session.notifLikes = it
                context.toastSavedOnDevice()
            }
            ToggleRow(R.string.board_f11_n_followers, followers) {
                followers = it
                BoardFlow11Session.notifFollowers = it
                context.toastSavedOnDevice()
            }
            ToggleRow(R.string.board_f11_n_mentions, mentions) {
                mentions = it
                BoardFlow11Session.notifMentions = it
                context.toastSavedOnDevice()
            }
            ToggleRow(R.string.board_f11_n_community, community) {
                community = it
                BoardFlow11Session.notifCommunity = it
                context.toastSavedOnDevice()
            }
            ToggleRow(R.string.board_f11_n_marketing, marketing) {
                marketing = it
                BoardFlow11Session.notifMarketing = it
                context.toastSavedOnDevice()
            }
            BoardPrimaryButton(text = stringResource(R.string.board_ct_done), onClick = onBack)
        }
    }
}

@Composable
private fun HelpSupport(onBack: () -> Unit, onArticle: () -> Unit) {
    val colors = BoardTheme.colors
    var query by remember { mutableStateOf("") }
    val topics = listOf(
        R.drawable.pj_ic_help to R.string.board_f11_help_faq,
        R.drawable.pj_ic_info to R.string.board_f11_help_howto,
        R.drawable.pj_ic_chat to R.string.board_f11_help_contact,
        R.drawable.pj_ic_bug to R.string.flow11_help_report,
        R.drawable.pj_ic_bulb to R.string.board_f11_help_feature,
        R.drawable.pj_ic_account to R.string.board_f11_help_guidelines
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            BoardScreenHeader(title = stringResource(R.string.flow11_help), onBack = onBack)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.board_f11_help_search)) },
                shape = RoundedCornerShape(BoardTheme.radii.md)
            )
            topics
                .filter { query.isBlank() || stringResource(it.second).contains(query, ignoreCase = true) }
                .forEach { (icon, label) ->
                    MenuItem(icon, label, onArticle)
                }
            Text(
                stringResource(R.string.flow11_still_need),
                color = colors.textSecondary,
                modifier = Modifier.padding(top = PixiSpacing.md)
            )
        }
    }
}

@Composable
private fun HelpArticle(onBack: () -> Unit, onTry: () -> Unit) {
    val colors = BoardTheme.colors
    val steps = listOf(
        R.string.board_f11_step1,
        R.string.board_f11_step2,
        R.string.board_f11_step3,
        R.string.board_f11_step4,
        R.string.board_f11_step5
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            BoardScreenHeader(title = stringResource(R.string.board_f11_help_center), onBack = onBack)
            Text(
                stringResource(R.string.board_f11_article_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            steps.forEachIndexed { i, res ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${i + 1}", color = colors.onPrimary, fontWeight = FontWeight.Bold)
                    }
                    Text(stringResource(res), color = colors.textPrimary, modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            BoardPrimaryButton(text = stringResource(R.string.board_f11_try_now), onClick = onTry)
        }
    }
}

@Composable
private fun DeleteAccount(onConfirm: () -> Unit, onCancel: () -> Unit) {
    val colors = BoardTheme.colors
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoardScreenHeader(title = stringResource(R.string.flow11_delete), onBack = onCancel)
            Icon(painter = painterResource(R.drawable.pj_ic_delete), null, tint = colors.danger, modifier = Modifier.size(72.dp))
            Text(
                stringResource(R.string.board_m11_delete_body),
                color = colors.textSecondary
            )
            BoardPrimaryButton(text = stringResource(R.string.flow21_delete_forever), onClick = onConfirm)
            BoardOutlinedButton(text = stringResource(R.string.cd_back), onClick = onCancel)
        }
    }
}

@Composable
private fun StorageSettings(onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            BoardScreenHeader(title = stringResource(R.string.board_f11_storage), onBack = onBack)
            Text(stringResource(R.string.board_f11_storage_body), color = colors.textSecondary)
            BoardOutlinedButton(
                text = stringResource(R.string.board_f11_clear_cache),
                onClick = {
                    BoardFlow11Session.cacheCleared = true
                    Toast.makeText(context, context.getString(R.string.board_f11_cache_cleared), Toast.LENGTH_SHORT).show()
                }
            )
            BoardPrimaryButton(text = stringResource(R.string.board_ct_done), onClick = onBack)
        }
    }
}

@Composable
private fun LanguageSettings(onBack: () -> Unit) {
    val colors = BoardTheme.colors
    var selected by remember { mutableIntStateOf(BoardFlow11Session.language) }
    val langs = listOf(
        R.string.board_f11_lang_en,
        R.string.board_f11_lang_hi,
        R.string.board_f11_lang_te,
        R.string.board_f11_lang_ta
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            BoardScreenHeader(title = stringResource(R.string.flow11_language), onBack = onBack)
            langs.forEachIndexed { i, res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BoardTheme.radii.md))
                        .background(if (i == selected) colors.primarySoft else colors.surfaceMuted)
                        .clickable {
                            selected = i
                            BoardFlow11Session.language = i
                        }
                        .padding(PixiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(res), modifier = Modifier.weight(1f), color = colors.textPrimary)
                    RadioButton(selected = i == selected, onClick = {
                        selected = i
                        BoardFlow11Session.language = i
                    })
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_ct_done), onClick = onBack)
        }
    }
}

@Composable
private fun AppearanceSettings(onBack: () -> Unit) {
    val colors = BoardTheme.colors
    var selected by remember { mutableIntStateOf(BoardFlow11Session.appearance) }
    val options = listOf(
        Triple(R.drawable.pj_ic_sun, R.string.board_f11_light, 0),
        Triple(R.drawable.pj_ic_moon, R.string.board_f11_dark, 1),
        Triple(R.drawable.pj_ic_brightness, R.string.board_f11_system, 2)
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            BoardScreenHeader(title = stringResource(R.string.board_f11_appearance), onBack = onBack)
            options.forEach { (icon, label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BoardTheme.radii.md))
                        .background(if (selected == value) colors.primarySoft else colors.surfaceMuted)
                        .clickable {
                            selected = value
                            BoardFlow11Session.appearance = value
                        }
                        .padding(PixiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Icon(painter = painterResource(icon), null, tint = colors.primary)
                    Text(stringResource(label), modifier = Modifier.weight(1f), color = colors.textPrimary)
                    RadioButton(selected = selected == value, onClick = {
                        selected = value
                        BoardFlow11Session.appearance = value
                    })
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_ct_done), onClick = onBack)
        }
    }
}


@Composable
private fun MenuItem(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    val colors = BoardTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BoardTheme.radii.md))
            .background(colors.surfaceMuted)
            .clickable(onClick = onClick)
            .padding(PixiSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
    ) {
        Icon(painter = painterResource(icon), null, tint = colors.primary)
        Text(
            stringResource(label),
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary
        )
        Icon(painter = painterResource(R.drawable.pj_ic_chevron_right), null, tint = colors.textSecondary)
    }
}

@Composable
private fun ToggleRow(@StringRes label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = BoardTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(label), modifier = Modifier.weight(1f), color = colors.textPrimary)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
