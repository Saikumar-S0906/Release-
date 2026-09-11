package com.pixijoy.ai.ui.board.me

import androidx.annotation.DrawableRes

import android.widget.Toast
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Tablet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardAuthReturn
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.toastOauthPreview
import com.pixijoy.ai.ui.theme.PixiSpacing

/** Flow 22 session — profile fields across auth steps. */
internal object BoardFlow22Session {
    var provider: String = "Google"
    var email: String = "saikumar.s@gmail.com"
    var displayName: String = "Saikumar"
    var username: String = "saikumar"
    var dob: String = "Apr 15, 1988"
    var photosPerm: Boolean = true
    var cameraPerm: Boolean = true
    var notifsPerm: Boolean = false
}

/**
 * Flow 22 — Account & Authentication.
 * OAuth is mocked (consent UI only) — matches board soft limit.
 */
private fun resumeAfterAuth(onNavigate: (String) -> Unit, fallback: () -> Unit) {
    val ret = BoardAuthReturn.consume()?.takeIf { it.isNotBlank() && !it.startsWith("b22_") }
    if (ret != null) onNavigate(ret) else fallback()
}

@Composable
fun BoardFlow22Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val pipeline = LocalBoardPipeline.current
    when (screenId) {
        "b22_hub", "b22_welcome" -> Welcome(
            onGetStarted = { onNavigate("b22_methods") },
            onSignIn = { onNavigate("b22_methods") },
            onGuest = onBack,
            onBack = onBack
        )
        "b22_methods" -> ChooseMethod(
            onPick = { provider ->
                BoardFlow22Session.provider = provider
                onNavigate("b22_oauth")
            },
            onBack = { onNavigate("b22_welcome") }
        )
        "b22_oauth" -> OauthConfirm(
            onContinue = { onNavigate("b22_profile") },
            onAnother = { onNavigate("b22_methods") },
            onBack = { onNavigate("b22_methods") }
        )
        "b22_profile" -> CompleteProfile(
            onContinue = { onNavigate("b22_perms") },
            onBack = { onNavigate("b22_oauth") }
        )
        "b22_perms" -> OptionalPermissions(
            onContinue = { onNavigate("b22_ready") },
            onLater = { onNavigate("b22_ready") },
            onBack = { onNavigate("b22_profile") }
        )
        "b22_ready" -> AccountReady(
            onCreate = {
                resumeAfterAuth(onNavigate) { pipeline.openMediaInput() }
            },
            onExplore = {
                resumeAfterAuth(onNavigate) { onNavigate("b2_discover") }
            },
            onManage = { onNavigate("b22_manage") },
            onBack = {
                resumeAfterAuth(onNavigate, onBack)
            }
        )
        "b22_manage" -> ManageAccount(
            onDevices = { onNavigate("b22_devices") },
            onDelete = { onNavigate("b11_delete") },
            onSignOut = onBack,
            onBack = { onNavigate("b22_ready") }
        )
        "b22_devices" -> MultiDevice(
            onDone = { onNavigate("b22_manage") },
            onBack = { onNavigate("b22_manage") }
        )
        else -> Welcome(
            onGetStarted = { onNavigate("b22_methods") },
            onSignIn = { onNavigate("b22_methods") },
            onGuest = onBack,
            onBack = onBack
        )
    }
}

@Composable
private fun Welcome(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
    onGuest: () -> Unit,
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
                .testTag(TestTags.FLOW22_HOST),
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
                Spacer(modifier = Modifier.weight(1f))
            }
            Image(
                painter = painterResource(BoardFlowArt.logo),
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )
            Text(
                stringResource(R.string.board_f22_moments),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Image(
                painter = painterResource(BoardFlowArt.homeHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_f22_get_started),
                onClick = onGetStarted
            )
            TextButton(onClick = onSignIn) {
                Text(stringResource(R.string.board_f22_already_account), color = colors.primary)
            }
            TextButton(onClick = onGuest) {
                Text(stringResource(R.string.board_continue_guest), color = colors.textSecondary)
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun ChooseMethod(onPick: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val methods = listOf(
        Triple(R.drawable.pj_ic_language, R.string.board_f22_google, "Google"),
        Triple(R.drawable.pj_ic_device, R.string.board_f22_apple, "Apple"),
        Triple(R.drawable.pj_ic_public, R.string.board_f22_facebook, "Facebook"),
        Triple(R.drawable.pj_ic_email, R.string.board_f22_email, "Email")
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
                    stringResource(R.string.board_f22_create_account),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Text(
                stringResource(R.string.board_oauth_preview),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            methods.forEach { (icon, label, key) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .clickable {
                            context.toastOauthPreview()
                            onPick(key)
                        }
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Icon(painter = painterResource(icon), null, tint = colors.primary)
                    Text(
                        stringResource(label),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(painter = painterResource(R.drawable.pj_ic_chevron_right), null, tint = colors.textSecondary)
                }
            }
            Text(
                stringResource(R.string.board_m22_tos),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OauthConfirm(onContinue: () -> Unit, onAnother: () -> Unit, onBack: () -> Unit) {
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
                    stringResource(R.string.board_f22_sign_in_with, BoardFlow22Session.provider),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.surfaceMuted)
                    .padding(PixiSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                Image(
                    painter = painterResource(BoardFlowArt.profileAvatar),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text(BoardFlow22Session.displayName, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(BoardFlow22Session.email, color = colors.textSecondary)
            }
            Text(
                stringResource(R.string.board_m22_oauth_body),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            BoardPrimaryButton(text = stringResource(R.string.flow22_continue), onClick = onContinue)
            TextButton(onClick = onAnother) {
                Text(stringResource(R.string.board_f22_another_account), color = colors.primary)
            }
        }
    }
}

@Composable
private fun CompleteProfile(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var name by remember { mutableStateOf(BoardFlow22Session.displayName) }
    var username by remember { mutableStateOf(BoardFlow22Session.username) }
    var dob by remember { mutableStateOf(BoardFlow22Session.dob) }
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
                    stringResource(R.string.board_f22_complete_profile),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Image(
                    painter = painterResource(BoardFlowArt.profileAvatar),
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
                        .background(colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_camera), null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.board_f22_display_name)) },
                shape = RoundedCornerShape(radii.md),
                singleLine = true
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.board_f22_username)) },
                shape = RoundedCornerShape(radii.md),
                singleLine = true
            )
            OutlinedTextField(
                value = dob,
                onValueChange = { dob = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.board_f22_dob)) },
                trailingIcon = { Icon(painter = painterResource(R.drawable.pj_ic_calendar), null) },
                shape = RoundedCornerShape(radii.md),
                singleLine = true
            )
            BoardPrimaryButton(
                text = stringResource(R.string.flow22_continue),
                onClick = {
                    BoardFlow22Session.displayName = name
                    BoardFlow22Session.username = username
                    BoardFlow22Session.dob = dob
                    onContinue()
                }
            )
        }
    }
}

@Composable
private fun OptionalPermissions(
    onContinue: () -> Unit,
    onLater: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var photos by remember { mutableStateOf(BoardFlow22Session.photosPerm) }
    var camera by remember { mutableStateOf(BoardFlow22Session.cameraPerm) }
    var notifs by remember { mutableStateOf(BoardFlow22Session.notifsPerm) }
    data class Perm(@DrawableRes val icon: Int, val title: Int, val body: Int, val checked: Boolean, val onChange: (Boolean) -> Unit)
    val perms = listOf(
        Perm(R.drawable.pj_ic_gallery, R.string.flow22_photos_perm, R.string.board_f22_photos_body, photos) {
            photos = it
            BoardFlow22Session.photosPerm = it
        },
        Perm(R.drawable.pj_ic_camera, R.string.board_f22_camera_perm, R.string.board_f22_camera_body, camera) {
            camera = it
            BoardFlow22Session.cameraPerm = it
        },
        Perm(R.drawable.pj_ic_notifications, R.string.flow22_notifs_perm, R.string.board_f22_notifs_body, notifs) {
            notifs = it
            BoardFlow22Session.notifsPerm = it
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
                    stringResource(R.string.board_m22_perms),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Text(stringResource(R.string.board_m22_perms_body), color = colors.textSecondary)
            perms.forEach { p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painter = painterResource(p.icon), null, tint = colors.primary)
                    Column(modifier = Modifier.weight(1f).padding(horizontal = PixiSpacing.md)) {
                        Text(stringResource(p.title), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text(stringResource(p.body), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    Switch(checked = p.checked, onCheckedChange = p.onChange)
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.flow22_continue), onClick = onContinue)
            TextButton(onClick = onLater) {
                Text(stringResource(R.string.board_maybe_later), color = colors.primary)
            }
        }
    }
}

@Composable
private fun AccountReady(
    onCreate: () -> Unit,
    onExplore: () -> Unit,
    onManage: () -> Unit,
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
                    .background(colors.successSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(64.dp))
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Text(
                stringResource(R.string.board_oauth_preview),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.board_f22_welcome_ready),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.board_f22_ready_body),
                textAlign = TextAlign.Center,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = PixiSpacing.md)
            )
            BoardPrimaryButton(text = stringResource(R.string.board_f22_start_creating), onClick = onCreate)
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardOutlinedButton(text = stringResource(R.string.board_f22_explore), onClick = onExplore)
            TextButton(onClick = onManage) {
                Text(stringResource(R.string.flow22_manage), color = colors.primary)
            }
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.cd_back), color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun ManageAccount(
    onDevices: () -> Unit,
    onDelete: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val rows = listOf(
        R.string.board_f22_field_name to BoardFlow22Session.displayName,
        R.string.board_f22_field_email to BoardFlow22Session.email,
        R.string.board_f22_field_username to BoardFlow22Session.username,
        R.string.board_f22_field_phone to stringResource(R.string.board_f22_optional),
        R.string.board_f22_field_dob to BoardFlow22Session.dob,
        R.string.board_f22_field_password to "••••••••",
        R.string.flow22_linked to BoardFlow22Session.provider
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
                    stringResource(R.string.board_f22_account_settings),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(BoardFlowArt.profileAvatar),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .align(Alignment.CenterHorizontally),
                contentScale = ContentScale.Crop
            )
            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .clickable {
                            Toast.makeText(context, context.getString(R.string.board_f22_edit_soon), Toast.LENGTH_SHORT).show()
                        }
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                        Text(value, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                    }
                    Icon(painter = painterResource(R.drawable.pj_ic_chevron_right), null, tint = colors.textSecondary)
                }
            }
            BoardOutlinedButton(text = stringResource(R.string.flow22_devices), onClick = onDevices)
            BoardOutlinedButton(text = stringResource(R.string.flow22_sign_out), onClick = onSignOut)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .clickable(onClick = onDelete)
                    .padding(PixiSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_delete), null, tint = colors.danger)
                Text(stringResource(R.string.flow11_delete), color = colors.danger, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MultiDevice(onDone: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val benefits = listOf(
        R.string.board_f22_sync_creations,
        R.string.board_f22_multi_access,
        R.string.board_f22_settings_sync,
        R.string.board_f22_secure_private,
        R.string.board_f22_always_updated
    )
    val devices = listOf(
        R.drawable.pj_ic_device to R.string.board_f22_device_phone,
        R.drawable.pj_ic_device to R.string.board_f22_device_tablet,
        R.drawable.pj_ic_device to R.string.board_f22_device_web
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
                    stringResource(R.string.board_f22_everywhere),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                devices.forEach { (icon, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(radii.card))
                                .background(colors.primary.copy(0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painter = painterResource(icon), null, tint = colors.primary, modifier = Modifier.size(32.dp))
                        }
                        Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }
            }
            benefits.forEach { res ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(20.dp))
                    Text(stringResource(res), color = colors.textPrimary)
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            BoardPrimaryButton(text = stringResource(R.string.board_f21_got_it), onClick = onDone)
            BoardTrustFooter()
        }
    }
}
