package com.pixijoy.ai.ui.board.me

import androidx.annotation.DrawableRes

import android.widget.Toast
import androidx.annotation.StringRes
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
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.boardConsentRepository
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardEmptyState
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.snapshot
import com.pixijoy.ai.ui.board.social.BoardSocialStore
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.board.toastSavedOnDevice
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.launch

internal enum class NotifKind { Like, Comment, Follow, System, Promo }

internal data class NotifItem(
    val id: Int,
    val kind: NotifKind,
    @StringRes val title: Int,
    val time: String,
    val dayGroup: Int,
    var unread: Boolean = true
)

/** Flow 25 session — filter + prefs across notification screens. */
internal object BoardFlow25Session {
    var filterTab: Int = 0
    var push: Boolean = true
    var inApp: Boolean = true
    var email: Boolean = false
    var likes: Boolean = true
    var comments: Boolean = true
    var follows: Boolean = true
    var system: Boolean = true
    var selectedId: Int = 1

    val items: MutableList<NotifItem> = mutableListOf(
        NotifItem(1, NotifKind.Like, R.string.board_f25_item_aditi_like, "2m", 0),
        NotifItem(2, NotifKind.Comment, R.string.board_f25_item_rahul_comment, "18m", 0),
        NotifItem(3, NotifKind.System, R.string.board_f25_item_video_ready, "1h", 0),
        NotifItem(4, NotifKind.Follow, R.string.flow25_item_follow, "Yesterday", 1),
        NotifItem(5, NotifKind.Promo, R.string.board_f25_item_challenge, "Yesterday", 1)
    )

    fun filtered(): List<NotifItem> = when (filterTab) {
        1 -> items.filter { it.kind == NotifKind.Like }
        2 -> items.filter { it.kind == NotifKind.Comment }
        3 -> items.filter { it.kind == NotifKind.Follow }
        else -> items
    }

    fun selected(): NotifItem? = items.find { it.id == selectedId } ?: items.firstOrNull()
}

/**
 * Flow 25 — Notification Center.
 * Push lock-screen is a mock preview only.
 */
@Composable
fun BoardFlow25Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val pipeline = LocalBoardPipeline.current
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    when (screenId) {
        "b25_hub" -> HomeEntry(
            onNotif = { onNavigate("b25_center") },
            onTab = tab
        )
        "b25_center", "b25_filtered" -> NotifCenter(
            onDetail = { id ->
                BoardFlow25Session.selectedId = id
                BoardFlow25Session.items.find { it.id == id }?.unread = false
                onNavigate("b25_detail")
            },
            onManage = { onNavigate("b25_manage") },
            onSettings = { onNavigate("b25_settings") },
            onEmpty = { onNavigate("b25_empty") },
            onPush = { onNavigate("b25_push") },
            onBack = onBack
        )
        "b25_detail" -> NotifDetail(
            onView = {
                val target = when (BoardFlow25Session.selected()?.kind) {
                    NotifKind.System -> "b19_success"
                    NotifKind.Like, NotifKind.Comment, NotifKind.Follow -> "b27_post"
                    NotifKind.Promo -> "b2_discover"
                    null -> "b25_center"
                    else -> "b19_generating"
                }
                onNavigate(target)
            },
            onBack = { onNavigate("b25_center") }
        )
        "b25_manage" -> ManageMenu(
            onMarkRead = {
                BoardFlow25Session.items.forEach { it.unread = false }
                onNavigate("b25_center")
            },
            onSettings = { onNavigate("b25_settings") },
            onClear = {
                BoardFlow25Session.items.clear()
                onNavigate("b25_empty")
            },
            onBack = { onNavigate("b25_center") }
        )
        "b25_settings" -> NotifSettings(
            onDone = { onNavigate("b25_center") },
            onBack = { onNavigate("b25_center") }
        )
        "b25_push" -> PushPreview(
            onDone = { onNavigate("b25_center") },
            onBack = { onNavigate("b25_center") }
        )
        "b25_empty" -> EmptyState(
            onExplore = { onNavigate("b2_discover") },
            onBack = onBack
        )
        else -> NotifCenter(
            onDetail = { id ->
                BoardFlow25Session.selectedId = id
                onNavigate("b25_detail")
            },
            onManage = { onNavigate("b25_manage") },
            onSettings = { onNavigate("b25_settings") },
            onEmpty = { onNavigate("b25_empty") },
            onPush = { onNavigate("b25_push") },
            onBack = onBack
        )
    }
}

@Composable
private fun HomeEntry(onNotif: () -> Unit, onTab: (BoardShellTab) -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardMainShell(selected = BoardShellTab.Home, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW25_HOST),
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
                Box {
                    IconButton(onClick = onNotif) {
                        Icon(painter = painterResource(R.drawable.pj_ic_notifications), contentDescription = stringResource(R.string.flow25_title))
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.danger)
                    )
                }
            }
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
                stringResource(R.string.board_f25_home_cta),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            BoardPrimaryButton(text = stringResource(R.string.flow25_title), onClick = onNotif)
        }
    }
}

@Composable
private fun NotifCenter(
    onDetail: (Int) -> Unit,
    onManage: () -> Unit,
    onSettings: () -> Unit,
    onEmpty: () -> Unit,
    onPush: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var tab by remember { mutableIntStateOf(BoardFlow25Session.filterTab) }
    val tabs = listOf(
        R.string.board_f25_tab_all,
        R.string.board_f25_tab_likes,
        R.string.board_f25_tab_comments,
        R.string.board_f25_tab_followers
    )
    val list = BoardFlow25Session.filtered()
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW25_HOST)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow25_title),
                    style = BoardType.title,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onManage) {
                    Icon(painter = painterResource(R.drawable.pj_ic_tune), contentDescription = stringResource(R.string.flow25_manage))
                }
                IconButton(onClick = onSettings) {
                    Icon(painter = painterResource(R.drawable.pj_ic_settings), contentDescription = stringResource(R.string.board_m25_settings))
                }
            }
            Image(
                painter = painterResource(BoardFlowArt.notifHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                tabs.forEachIndexed { i, label ->
                    val active = tab == i
                    val count = when (i) {
                        1 -> BoardFlow25Session.items.count { it.kind == NotifKind.Like }
                        2 -> BoardFlow25Session.items.count { it.kind == NotifKind.Comment }
                        3 -> BoardFlow25Session.items.count { it.kind == NotifKind.Follow }
                        else -> BoardFlow25Session.items.size
                    }
                    Text(
                        "${stringResource(label)} ($count)",
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (active) colors.primary else colors.surfaceMuted)
                            .clickable {
                                tab = i
                                BoardFlow25Session.filterTab = i
                            }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                        color = if (active) colors.onPrimary else colors.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            if (list.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(stringResource(R.string.flow25_empty), fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    TextButton(onClick = onEmpty) {
                        Text(stringResource(R.string.flow25_show_empty), color = colors.primary)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    val today = list.filter { it.dayGroup == 0 }
                    val yesterday = list.filter { it.dayGroup == 1 }
                    if (today.isNotEmpty()) {
                        Text(stringResource(R.string.board_f23_today), fontWeight = FontWeight.Bold, color = colors.textSecondary)
                        today.forEach { item -> NotifRow(item) { onDetail(item.id) } }
                    }
                    if (yesterday.isNotEmpty()) {
                        Text(stringResource(R.string.board_f23_yesterday), fontWeight = FontWeight.Bold, color = colors.textSecondary)
                        yesterday.forEach { item -> NotifRow(item) { onDetail(item.id) } }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                TextButton(onClick = onPush) {
                    Text(stringResource(R.string.board_f25_push_demo), color = colors.primary)
                }
                TextButton(onClick = onEmpty) {
                    Text(stringResource(R.string.flow25_show_empty), color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun NotifRow(item: NotifItem, onClick: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    @DrawableRes val icon: Int = when (item.kind) {
        NotifKind.Like -> R.drawable.pj_ic_heart
        NotifKind.Comment -> R.drawable.pj_ic_comment
        NotifKind.Follow -> R.drawable.pj_ic_person_add
        NotifKind.System -> R.drawable.pj_ic_cut
        NotifKind.Promo -> R.drawable.pj_ic_megaphone
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radii.card))
            .background(if (item.unread) colors.primary.copy(0.06f) else colors.surfaceMuted)
            .clickable(onClick = onClick)
            .padding(PixiSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(icon), null, tint = colors.primary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(item.title),
                fontWeight = if (item.unread) FontWeight.Bold else FontWeight.Medium,
                color = colors.textPrimary
            )
            Text(item.time, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        }
        Image(
            painter = painterResource(BoardFlowArt.homeHero),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(radii.md)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun NotifDetail(onView: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val item = BoardFlow25Session.selected()
    if (item == null) {
        EmptyState(onExplore = onBack, onBack = onBack)
        return
    }
    val socialId = "f25_notif_${item.id}"
    LaunchedEffect(Unit) { BoardSocialStore.hydrate(context) }
    val revision = BoardSocialStore.revision
    var followed by remember(revision) { mutableStateOf(BoardSocialStore.isFollowed(socialId)) }
    var liked by remember(revision) { mutableStateOf(BoardSocialStore.isLiked(socialId)) }
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
                    stringResource(R.string.flow25_detail_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Text(stringResource(item.title), color = colors.textPrimary)
            Text(item.time, color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            Image(
                painter = painterResource(BoardFlowArt.homeHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.surfaceMuted)
                    .padding(PixiSpacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(BoardFlowArt.profileAvatar),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = PixiSpacing.md)
                ) {
                    Text(stringResource(R.string.board_f25_user_aditi), fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text(stringResource(R.string.board_f25_handle_aditi), color = colors.textSecondary)
                }
                BoardOutlinedButton(
                    text = stringResource(
                        if (followed) R.string.board_f27_following_btn else R.string.board_f25_follow
                    ),
                    onClick = {
                        scope.launch {
                            followed = BoardSocialStore.toggleFollow(context, socialId)
                        }
                    }
                )
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f25_view_creation), onClick = onView)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            liked = BoardSocialStore.toggleLike(context, socialId)
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_heart),
                        contentDescription = null,
                        tint = colors.primary
                    )
                }
                IconButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.board_chat_saved_local),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) { Icon(painter = painterResource(R.drawable.pj_ic_comment), null, tint = colors.primary) }
                IconButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.board_demo_share),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) { Icon(painter = painterResource(R.drawable.pj_ic_share), null, tint = colors.primary) }
            }
        }
    }
}

@Composable
private fun ManageMenu(
    onMarkRead: () -> Unit,
    onSettings: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val rows = listOf(
        Triple(R.drawable.pj_ic_email, R.string.board_f25_mark_all, onMarkRead),
        Triple(R.drawable.pj_ic_settings, R.string.board_m25_settings, onSettings),
        Triple(R.drawable.pj_ic_tune, R.string.board_f25_manage_categories, onSettings)
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
                    stringResource(R.string.flow25_manage),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            rows.forEach { (icon, label, action) ->
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
                    Text(stringResource(label), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .clickable(onClick = onClear)
                    .padding(PixiSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_delete), null, tint = colors.danger)
                Text(stringResource(R.string.board_f25_clear_all), color = colors.danger, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun NotifSettings(onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var push by remember { mutableStateOf(BoardFlow25Session.push) }
    var inApp by remember { mutableStateOf(BoardFlow25Session.inApp) }
    var email by remember { mutableStateOf(BoardFlow25Session.email) }
    var likes by remember { mutableStateOf(BoardFlow25Session.likes) }
    var comments by remember { mutableStateOf(BoardFlow25Session.comments) }
    var follows by remember { mutableStateOf(BoardFlow25Session.follows) }
    var system by remember { mutableStateOf(BoardFlow25Session.system) }
    LaunchedEffect(Unit) {
        val repo = context.boardConsentRepository()
        val enabled = repo.snapshot().notifications
        push = enabled
        BoardFlow25Session.push = enabled
        val cats = repo.notifCategories()
        inApp = cats.inApp
        email = cats.email
        likes = cats.likes
        comments = cats.comments
        follows = cats.follows
        system = cats.system
        BoardFlow25Session.inApp = cats.inApp
        BoardFlow25Session.email = cats.email
        BoardFlow25Session.likes = cats.likes
        BoardFlow25Session.comments = cats.comments
        BoardFlow25Session.follows = cats.follows
        BoardFlow25Session.system = cats.system
    }
    data class Toggle(@StringRes val label: Int, val checked: Boolean, val onChange: (Boolean) -> Unit)
    val toggles = listOf(
        Toggle(R.string.flow25_push, push) {
            push = it
            BoardFlow25Session.push = it
            scope.launch {
                context.boardConsentRepository().setNotificationsEnabled(it)
                context.toastSavedOnDevice()
            }
        },
        Toggle(R.string.flow25_in_app, inApp) {
            inApp = it
            BoardFlow25Session.inApp = it
            scope.launch {
                context.boardConsentRepository().setNotifInApp(it)
                context.toastSavedOnDevice()
            }
        },
        Toggle(R.string.flow25_email, email) {
            email = it
            BoardFlow25Session.email = it
            scope.launch {
                context.boardConsentRepository().setNotifEmail(it)
                context.toastSavedOnDevice()
            }
        },
        Toggle(R.string.board_f25_cat_likes, likes) {
            likes = it
            BoardFlow25Session.likes = it
            scope.launch {
                context.boardConsentRepository().setNotifLikes(it)
                context.toastSavedOnDevice()
            }
        },
        Toggle(R.string.board_f25_cat_comments, comments) {
            comments = it
            BoardFlow25Session.comments = it
            scope.launch {
                context.boardConsentRepository().setNotifComments(it)
                context.toastSavedOnDevice()
            }
        },
        Toggle(R.string.board_f25_cat_follows, follows) {
            follows = it
            BoardFlow25Session.follows = it
            scope.launch {
                context.boardConsentRepository().setNotifFollows(it)
                context.toastSavedOnDevice()
            }
        },
        Toggle(R.string.board_f25_cat_system, system) {
            system = it
            BoardFlow25Session.system = it
            scope.launch {
                context.boardConsentRepository().setNotifSystem(it)
                context.toastSavedOnDevice()
            }
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
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_m25_settings),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Text(stringResource(R.string.flow25_banner_demo), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            toggles.forEach { t ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(t.label),
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = t.checked, onCheckedChange = t.onChange)
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_ct_done), onClick = onDone)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun PushPreview(onDone: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val pushes = listOf(
        R.string.board_f25_item_aditi_like,
        R.string.board_f25_item_rahul_comment,
        R.string.board_f25_item_video_ready,
        R.string.board_f25_item_template,
        R.string.board_f25_item_challenge
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
                    stringResource(R.string.board_f25_push_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card))
                    .background(Color(0xFF0B0B10))
                    .padding(PixiSpacing.lg)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    Text(
                        stringResource(R.string.board_f25_lock_hint),
                        color = Color.White.copy(0.7f),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    pushes.forEach { res ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(radii.md))
                                .background(Color.White.copy(0.12f))
                                .padding(PixiSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                        ) {
                            Icon(painter = painterResource(R.drawable.pj_ic_notifications), null, tint = Color.White)
                            Column {
                                Text(
                                    stringResource(R.string.app_name),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    stringResource(res),
                                    color = Color.White.copy(0.9f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f21_got_it), onClick = onDone)
        }
    }
}

@Composable
private fun EmptyState(onExplore: () -> Unit, onBack: () -> Unit) {
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW25_HOST)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.flow25_title),
                    style = BoardType.title,
                    color = BoardTheme.colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            BoardEmptyState(
                title = stringResource(R.string.flow25_empty),
                body = stringResource(R.string.board_f25_empty_body),
                ctaLabel = stringResource(R.string.board_f22_explore),
                onCta = onExplore,
                heroRes = BoardFlowArt.notifHero
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
