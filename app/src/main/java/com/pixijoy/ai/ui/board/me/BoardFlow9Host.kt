package com.pixijoy.ai.ui.board.me

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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewAgenda
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.pixijoy.ai.ui.board.rememberKidsSpace
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardEmptyState
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardSkeletonCard
import com.pixijoy.ai.ui.board.components.BoardTitleText
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.delay

/** Flow 9 session â€” profile fields persist across edit / theme / privacy. */
internal object BoardFlow9Session {
    var name: String = ""
    var bio: String = ""
    var website: String = ""
    var location: String = ""
    var avatarIndex: Int = 0
    var themeIndex: Int = 0
    var badgeIndex: Int = 0
    var profileVisibility: Int = 0
    var postVisibility: Int = 0
    var allowComments: Boolean = true
    var allowRemix: Boolean = true
    var showLocation: Boolean = false
    var notifLikes: Boolean = true
    var notifComments: Boolean = true
    var notifFollows: Boolean = true
    var notifMentions: Boolean = true
    var notifRemix: Boolean = true
    var notifCommunity: Boolean = false
    var notifProduct: Boolean = true
    var socialInstagram: Boolean = true
    var socialThreads: Boolean = true
    var socialX: Boolean = false
    var contentTab: Int = 0
    /** Where Avatar/Theme returns — profile home or edit. */
    var themeReturnId: String = "b9_open"

    @DrawableRes
    fun avatarRes(): Int = when (avatarIndex % 5) {
        1 -> BoardFlowArt.beautifulHero
        2 -> BoardFlowArt.funnyHero
        3 -> BoardFlowArt.stickerHero
        4 -> BoardFlowArt.creationThumb
        else -> BoardFlowArt.profileAvatar
    }
}

/**
 * Flow 9 â€” Profile & Personalization.
 * Board chrome shows Profile as a 5th tab; production keeps Home Â· Create Â· Discover Â· My Stuff
 * (Profile maps onto My Stuff).
 */
@Composable
fun BoardFlow9Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    when (screenId) {
        "b9_hub", "b9_open" -> ProfileHome(
            highlightUpdated = false,
            onTab = tab,
            onEdit = { onNavigate("b9_edit") },
            onSettings = { onNavigate("b11_hub") },
            onCreations = { onNavigate("b18_browse") },
            onFavorites = { onNavigate("b9_favorites") },
            onAlbums = { onNavigate("b9_albums") },
            onPrivacy = { onNavigate("b9_privacy") },
            onNotifs = { onNavigate("b9_notifs") },
            onTheme = {
                BoardFlow9Session.themeReturnId = "b9_open"
                onNavigate("b9_theme")
            }
        )
        "b9_edit" -> EditProfile(
            onSave = { onNavigate("b9_updated") },
            onTheme = {
                BoardFlow9Session.themeReturnId = "b9_edit"
                onNavigate("b9_theme")
            },
            onBack = { onNavigate("b9_open") }
        )
        "b9_theme" -> AvatarThemeBadges(
            onApply = { onNavigate("b9_updated") },
            onBack = { onNavigate(BoardFlow9Session.themeReturnId) }
        )
        "b9_albums" -> ManageAlbums(
            favoritesOnly = false,
            onOpen = { onNavigate("b18_browse") },
            onBack = { onNavigate("b9_open") }
        )
        "b9_favorites" -> ManageAlbums(
            favoritesOnly = true,
            onOpen = { onNavigate("b18_browse") },
            onBack = { onNavigate("b9_open") }
        )
        "b9_privacy" -> PrivacySettings(
            onBlocked = { onNavigate("b21_hub") },
            onBack = { onNavigate("b9_open") }
        )
        "b9_notifs" -> NotificationSettings(
            onBack = { onNavigate("b9_open") }
        )
        "b9_updated" -> ProfileHome(
            highlightUpdated = true,
            onTab = tab,
            onEdit = { onNavigate("b9_edit") },
            onSettings = { onNavigate("b11_hub") },
            onCreations = { onNavigate("b18_browse") },
            onFavorites = { onNavigate("b9_favorites") },
            onAlbums = { onNavigate("b9_albums") },
            onPrivacy = { onNavigate("b9_privacy") },
            onNotifs = { onNavigate("b9_notifs") },
            onTheme = {
                BoardFlow9Session.themeReturnId = "b9_open"
                onNavigate("b9_theme")
            }
        )
        else -> ProfileHome(
            highlightUpdated = false,
            onTab = tab,
            onEdit = { onNavigate("b9_edit") },
            onSettings = { onNavigate("b11_hub") },
            onCreations = { onNavigate("b18_browse") },
            onFavorites = { onNavigate("b9_favorites") },
            onAlbums = { onNavigate("b9_albums") },
            onPrivacy = { onNavigate("b9_privacy") },
            onNotifs = { onNavigate("b9_notifs") },
            onTheme = {
                BoardFlow9Session.themeReturnId = "b9_open"
                onNavigate("b9_theme")
            }
        )
    }
}

@Composable
private fun ProfileHome(
    highlightUpdated: Boolean,
    onTab: (BoardShellTab) -> Unit,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
    onCreations: () -> Unit,
    onFavorites: () -> Unit,
    onAlbums: () -> Unit,
    onPrivacy: () -> Unit,
    onNotifs: () -> Unit,
    onTheme: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var contentTab by remember { mutableIntStateOf(BoardFlow9Session.contentTab) }
    BoardMainShell(selected = BoardShellTab.MyStuff, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW9_HOST),
            horizontalAlignment = Alignment.CenterHorizontally,
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
                IconButton(onClick = onSettings) {
                    Icon(painter = painterResource(R.drawable.pj_ic_settings), contentDescription = stringResource(R.string.flow11_title))
                }
            }
            if (highlightUpdated) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success)
                    Text(
                        stringResource(R.string.flow9_updated),
                        color = colors.success,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Image(
                painter = painterResource(BoardFlow9Session.avatarRes()),
                contentDescription = null,
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .border(3.dp, themeAccent(BoardFlow9Session.themeIndex), CircleShape)
                    .clickable(onClick = onTheme),
                contentScale = ContentScale.Crop
            )
            Text(
                BoardFlow9Session.name.ifBlank { stringResource(R.string.nav_profile) },
                style = BoardType.title,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                BoardFlow9Session.bio,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    "248" to R.string.flow9_posts,
                    "1.2K" to R.string.flow9_followers,
                    "356" to R.string.flow9_following
                ).forEach { (count, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(count, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text(stringResource(label), color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            BoardPrimaryButton(
                text = stringResource(R.string.flow9_edit_profile),
                onClick = onEdit
            )
            if (!highlightUpdated) {
                ProfileMenuRow(R.drawable.pj_ic_folder, R.string.flow8_my_creations, onCreations)
                ProfileMenuRow(R.drawable.pj_ic_heart, R.string.flow9_favorites, onFavorites)
                ProfileMenuRow(R.drawable.pj_ic_folder, R.string.flow9_albums, onAlbums)
                ProfileMenuRow(R.drawable.pj_ic_settings, R.string.flow11_title, onSettings)
                TextButton(onClick = onPrivacy) {
                    Text(stringResource(R.string.flow9_privacy), color = colors.primary)
                }
                TextButton(onClick = onNotifs) {
                    Text(stringResource(R.string.flow9_notif_settings), color = colors.primary)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        R.drawable.pj_ic_grid,
                        R.drawable.pj_ic_grid,
                        R.drawable.pj_ic_heart,
                        R.drawable.pj_ic_people
                    ).forEachIndexed { i, icon ->
                        IconButton(onClick = {
                            contentTab = i
                            BoardFlow9Session.contentTab = i
                        }) {
                            Icon(painter = painterResource(icon),
                                contentDescription = null,
                                tint = if (i == contentTab) colors.primary else colors.textSecondary
                            )
                        }
                    }
                }
                val grid = BoardFlowArt.libraryThumbs + listOf(
                    BoardFlowArt.beautifulHero,
                    BoardFlowArt.funnyHero,
                    BoardFlowArt.creationThumb
                )
                grid.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        row.forEach { res ->
                            Image(
                                painter = painterResource(res),
                                contentDescription = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(radii.md)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun ProfileMenuRow(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radii.md))
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
private fun EditProfile(
    onSave: () -> Unit,
    onTheme: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    var name by remember { mutableStateOf(BoardFlow9Session.name) }
    var bio by remember { mutableStateOf(BoardFlow9Session.bio) }
    var website by remember { mutableStateOf(BoardFlow9Session.website) }
    var location by remember { mutableStateOf(BoardFlow9Session.location) }
    var ig by remember { mutableStateOf(BoardFlow9Session.socialInstagram) }
    var threads by remember { mutableStateOf(BoardFlow9Session.socialThreads) }
    var x by remember { mutableStateOf(BoardFlow9Session.socialX) }
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
                    stringResource(R.string.flow9_edit_profile),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
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
                    Icon(painter = painterResource(R.drawable.pj_ic_camera),
                        contentDescription = stringResource(R.string.flow9_change_photo),
                        tint = colors.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.flow9_name)) },
                shape = RoundedCornerShape(BoardTheme.radii.md)
            )
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.flow9_bio)) },
                minLines = 2,
                shape = RoundedCornerShape(BoardTheme.radii.md)
            )
            OutlinedTextField(
                value = website,
                onValueChange = { website = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.flow9_website)) },
                shape = RoundedCornerShape(BoardTheme.radii.md)
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.flow9_location)) },
                shape = RoundedCornerShape(BoardTheme.radii.md)
            )
            Text(
                stringResource(R.string.flow9_social),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                SocialChip("IG", ig) { ig = !ig }
                SocialChip("Threads", threads) { threads = !threads }
                SocialChip("X", x) { x = !x }
                TextButton(onClick = { /* placeholder add */ }) {
                    Text(stringResource(R.string.flow9_add_social), color = colors.primary)
                }
            }
            BoardOutlinedButton(
                text = stringResource(R.string.flow9_themes),
                onClick = onTheme
            )
            BoardPrimaryButton(
                text = stringResource(R.string.flow9_save),
                onClick = {
                    BoardFlow9Session.name = name.trim().ifEmpty { BoardFlow9Session.name }
                    BoardFlow9Session.bio = bio
                    BoardFlow9Session.website = website
                    BoardFlow9Session.location = location
                    BoardFlow9Session.socialInstagram = ig
                    BoardFlow9Session.socialThreads = threads
                    BoardFlow9Session.socialX = x
                    onSave()
                }
            )
        }
    }
}

@Composable
private fun SocialChip(label: String, on: Boolean, onToggle: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(radii.pill))
            .background(if (on) colors.primary else colors.surfaceMuted)
            .clickable(onClick = onToggle)
            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
    ) {
        Text(label, color = if (on) colors.onPrimary else colors.textSecondary)
    }
}

@Composable
private fun AvatarThemeBadges(onApply: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var tab by remember { mutableIntStateOf(1) }
    var avatar by remember { mutableIntStateOf(BoardFlow9Session.avatarIndex) }
    var theme by remember { mutableIntStateOf(BoardFlow9Session.themeIndex) }
    var badge by remember { mutableIntStateOf(BoardFlow9Session.badgeIndex) }
    val tabs = listOf(
        R.string.board_f9_tab_avatar,
        R.string.board_f9_tab_themes,
        R.string.board_f9_tab_badges
    )
    val themes = listOf(
        "Default" to colors.primary,
        "Dark" to Color(0xFF212121),
        "Nature" to Color(0xFF4CAF50),
        "Sunset" to colors.accentPink,
        "Minimal" to colors.surfaceMuted,
        "Dreamy" to colors.accentPurple
    )
    val badges = listOf(
        R.string.board_f9_badge_creator,
        R.string.board_f9_badge_remixer,
        R.string.board_f9_badge_streak,
        R.string.board_f9_badge_pro
    )
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
                    stringResource(R.string.board_f9_theme_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.pill))
                    .background(colors.surfaceMuted)
                    .padding(4.dp)
            ) {
                tabs.forEachIndexed { i, res ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (i == tab) colors.surface else Color.Transparent)
                            .clickable { tab = i }
                            .padding(vertical = PixiSpacing.sm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(res),
                            color = if (i == tab) colors.primary else colors.textSecondary,
                            fontWeight = if (i == tab) FontWeight.SemiBold else FontWeight.Normal,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            when (tab) {
                0 -> {
                    Text(stringResource(R.string.board_f9_choose_avatar), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(colors.primarySoft)
                                .clickable { avatar = (avatar + 1) % 5 },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painter = painterResource(R.drawable.pj_ic_add), null, tint = colors.primary)
                        }
                        (0 until 5).forEach { i ->
                            val res = when (i) {
                                1 -> BoardFlowArt.beautifulHero
                                2 -> BoardFlowArt.funnyHero
                                3 -> BoardFlowArt.stickerHero
                                4 -> BoardFlowArt.creationThumb
                                else -> BoardFlowArt.profileAvatar
                            }
                            Box {
                                Image(
                                    painter = painterResource(res),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (i == avatar) 3.dp else 0.dp,
                                            color = colors.primary,
                                            shape = CircleShape
                                        )
                                        .clickable { avatar = i },
                                    contentScale = ContentScale.Crop
                                )
                                if (i == avatar) {
                                    Icon(painter = painterResource(R.drawable.pj_ic_check_circle),
                                        null,
                                        tint = colors.primary,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    Text(stringResource(R.string.flow9_choose_theme), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    themes.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                        ) {
                            row.forEach { (name, accent) ->
                                val index = themes.indexOfFirst { it.first == name }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(radii.card))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.35f))
                                            )
                                        )
                                        .border(
                                            width = if (index == theme) 2.dp else 0.dp,
                                            color = colors.primary,
                                            shape = RoundedCornerShape(radii.card)
                                        )
                                        .clickable { theme = index }
                                        .padding(PixiSpacing.sm),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        if (index == theme) {
                                            Icon(painter = painterResource(R.drawable.pj_ic_check), null, tint = Color.White)
                                        }
                                        Text(name, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    Text(stringResource(R.string.board_f9_earn_badges), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    badges.forEachIndexed { i, res ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(radii.md))
                                .background(if (i == badge) colors.primarySoft else colors.surfaceMuted)
                                .clickable { badge = i }
                                .padding(PixiSpacing.lg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(res),
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            if (i == badge) Icon(painter = painterResource(R.drawable.pj_ic_check), null, tint = colors.primary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            BoardPrimaryButton(
                text = stringResource(R.string.flow9_apply_theme),
                onClick = {
                    BoardFlow9Session.avatarIndex = avatar
                    BoardFlow9Session.themeIndex = theme
                    BoardFlow9Session.badgeIndex = badge
                    onApply()
                }
            )
        }
    }
}

@Composable
private fun ManageAlbums(
    favoritesOnly: Boolean,
    onOpen: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(favoritesOnly) {
        ready = false
        delay(280)
        ready = true
    }
    val albums = if (favoritesOnly) {
        listOf(Triple(R.string.flow9_album_fav, 56, BoardFlowArt.beautifulHero))
    } else {
        listOf(
            Triple(R.string.flow9_album_all, 248, BoardFlowArt.creationThumb),
            Triple(R.string.flow9_album_fav, 56, BoardFlowArt.beautifulHero),
            Triple(R.string.flow9_album_travel, 32, BoardFlowArt.collageHero),
            Triple(R.string.flow9_album_family, 46, BoardFlowArt.funnyHero),
            Triple(R.string.flow9_album_pets, 28, BoardFlowArt.stickerHero),
            Triple(R.string.flow9_album_creative, 24, BoardFlowArt.editorHero)
        )
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
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
                BoardTitleText(
                    text = stringResource(
                        if (favoritesOnly) R.string.flow9_favorites else R.string.flow9_albums
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOpen) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_add),
                        contentDescription = stringResource(R.string.board_f9_create_album),
                        tint = colors.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            when {
                !ready -> {
                    Column(verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)) {
                        repeat(2) { BoardSkeletonCard() }
                    }
                }
                albums.isEmpty() -> {
                    BoardEmptyState(
                        title = stringResource(R.string.board_empty_title),
                        body = stringResource(R.string.board_empty_body),
                        ctaLabel = stringResource(R.string.board_f9_create_album),
                        onCta = onOpen,
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        albums.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                            ) {
                                row.forEach { (title, count, res) ->
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(radii.card))
                                            .clickable(onClick = onOpen)
                                    ) {
                                        Box {
                                            Image(
                                                painter = painterResource(res),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(radii.card)),
                                                contentScale = ContentScale.Crop
                                            )
                                            if (title == R.string.flow9_album_fav) {
                                                Icon(
                                                    painter = painterResource(R.drawable.pj_ic_heart),
                                                    contentDescription = null,
                                                    tint = colors.accentPink,
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(PixiSpacing.sm)
                                                )
                                            }
                                        }
                                        Text(
                                            stringResource(title),
                                            modifier = Modifier.padding(top = PixiSpacing.sm),
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary,
                                            style = BoardType.body
                                        )
                                        Text(
                                            stringResource(R.string.board_f9_album_count, count),
                                            color = colors.textSecondary,
                                            style = BoardType.caption
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
    }
}

@Composable
private fun PrivacySettings(onBlocked: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val kids = rememberKidsSpace()
    var profileVis by remember { mutableIntStateOf(BoardFlow9Session.profileVisibility) }
    var postVis by remember { mutableIntStateOf(BoardFlow9Session.postVisibility) }
    var comments by remember { mutableStateOf(BoardFlow9Session.allowComments) }
    var remix by remember { mutableStateOf(BoardFlow9Session.allowRemix) }
    var location by remember { mutableStateOf(BoardFlow9Session.showLocation) }
    LaunchedEffect(kids) {
        if (kids) {
            profileVis = 2
            BoardFlow9Session.profileVisibility = 2
        }
    }
    val profileOptions = listOf(
        Triple(R.string.flow9_public, R.drawable.pj_ic_public, 0),
        Triple(R.string.flow9_friends, R.drawable.pj_ic_people, 1),
        Triple(R.string.flow9_only_me, R.drawable.pj_ic_lock, 2)
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
                    stringResource(R.string.flow9_privacy),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            if (kids) {
                Text(
                    stringResource(R.string.board_kids_search_blocked),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(stringResource(R.string.flow9_visibility), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            profileOptions.forEach { (label, icon, value) ->
                val enabled = !kids || value == 2
                VisibilityRow(label, icon, profileVis == value) {
                    if (!enabled) return@VisibilityRow
                    profileVis = value
                    BoardFlow9Session.profileVisibility = value
                }
            }
            Text(stringResource(R.string.board_f9_post_visibility), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            profileOptions.forEach { (label, icon, value) ->
                VisibilityRow(label, icon, postVis == value) {
                    postVis = value
                    BoardFlow9Session.postVisibility = value
                }
            }
            ToggleRow(R.string.flow9_allow_comments, comments) {
                comments = it
                BoardFlow9Session.allowComments = it
            }
            ToggleRow(R.string.flow9_allow_remix, remix) {
                remix = it
                BoardFlow9Session.allowRemix = it
            }
            ToggleRow(R.string.flow9_show_location, location) {
                location = it
                BoardFlow9Session.showLocation = it
            }
            ProfileMenuRow(R.drawable.pj_ic_lock, R.string.flow9_blocked, onBlocked)
            BoardPrimaryButton(text = stringResource(R.string.board_f9_save_privacy), onClick = onBack)
        }
    }
}

@Composable
private fun VisibilityRow(
    @StringRes label: Int,
    @DrawableRes icon: Int,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radii.md))
            .background(if (selected) colors.primarySoft else colors.surfaceMuted)
            .clickable(onClick = onSelect)
            .padding(PixiSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
    ) {
        Icon(painter = painterResource(icon), null, tint = if (selected) colors.primary else colors.textSecondary)
        Text(stringResource(label), modifier = Modifier.weight(1f), color = colors.textPrimary)
        RadioButton(selected = selected, onClick = onSelect)
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

@Composable
private fun NotificationSettings(onBack: () -> Unit) {
    val colors = BoardTheme.colors
    var likes by remember { mutableStateOf(BoardFlow9Session.notifLikes) }
    var comments by remember { mutableStateOf(BoardFlow9Session.notifComments) }
    var follows by remember { mutableStateOf(BoardFlow9Session.notifFollows) }
    var mentions by remember { mutableStateOf(BoardFlow9Session.notifMentions) }
    var remix by remember { mutableStateOf(BoardFlow9Session.notifRemix) }
    var community by remember { mutableStateOf(BoardFlow9Session.notifCommunity) }
    var product by remember { mutableStateOf(BoardFlow9Session.notifProduct) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow9_notif_settings),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            ToggleRow(R.string.flow9_n_likes, likes) {
                likes = it; BoardFlow9Session.notifLikes = it
            }
            ToggleRow(R.string.flow9_n_comments, comments) {
                comments = it; BoardFlow9Session.notifComments = it
            }
            ToggleRow(R.string.flow9_n_follows, follows) {
                follows = it; BoardFlow9Session.notifFollows = it
            }
            ToggleRow(R.string.flow9_n_mentions, mentions) {
                mentions = it; BoardFlow9Session.notifMentions = it
            }
            ToggleRow(R.string.flow9_n_remix, remix) {
                remix = it; BoardFlow9Session.notifRemix = it
            }
            ToggleRow(R.string.flow9_n_community, community) {
                community = it; BoardFlow9Session.notifCommunity = it
            }
            ToggleRow(R.string.flow9_n_product, product) {
                product = it; BoardFlow9Session.notifProduct = it
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            BoardPrimaryButton(text = stringResource(R.string.board_f9_save_notifs), onClick = onBack)
            TextButton(onClick = { onBack() }) {
                Text(stringResource(R.string.cd_back), color = colors.primary)
            }
        }
    }
}

private fun themeAccent(index: Int): Color = when (index % 6) {
    1 -> Color(0xFF212121)
    2 -> Color(0xFF4CAF50)
    3 -> Color(0xFFFF6B9D)
    4 -> Color(0xFFBDBDBD)
    5 -> Color(0xFF9C6BFF)
    else -> Color(0xFF3D5AFE)
}
