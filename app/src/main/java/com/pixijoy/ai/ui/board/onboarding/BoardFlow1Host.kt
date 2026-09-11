package com.pixijoy.ai.ui.board.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.LocalBoardCanPop
import com.pixijoy.ai.ui.board.LocalBoardOnboardingActions
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardHeroImage
import com.pixijoy.ai.ui.board.components.BoardIntentTile
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPageDots
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardSecondaryButton
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.board.toastOauthPreview
import com.pixijoy.ai.ui.theme.PixiRadii
import com.pixijoy.ai.ui.theme.PixiSpacing
import com.pixijoy.ai.util.MediaPermissionHelper
import kotlinx.coroutines.delay

/**
 * Board-faithful Flow 1 � all 7 happy-path screens + A�F edges.
 * Source: flows/01_Launch_and_Onboarding.png
 */
@Composable
fun BoardFlow1Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val actions = LocalBoardOnboardingActions.current
    when (screenId) {
        "b1_splash" -> BoardSplash(
            onFinished = {
                actions.markPriorLaunch()
                onNavigate("b1_welcome")
            }
        )
        "b1_welcome" -> BoardWelcome(
            onNext = { onNavigate("b1_features") },
            onSkip = { onNavigate("b1_skip") },
            onBack = null
        )
        "b1_features" -> BoardKeyFeatures(
            onNext = { onNavigate("b1_for_everyone") },
            onSkip = { onNavigate("b1_skip") },
            onBack = onBack
        )
        "b1_for_everyone" -> BoardForEveryone(
            onNext = { onNavigate("b1_permissions") },
            onSkip = { onNavigate("b1_skip") },
            onBack = onBack
        )
        "b1_permissions" -> BoardPermissions(
            onAllow = { onNavigate("b1_signup") },
            onNotNow = { onNavigate("b1_perm_denied") },
            onBack = onBack
        )
        "b1_perm_denied" -> BoardPermissionDenied(
            onRetry = { onNavigate("b1_permissions") },
            onLimited = { onNavigate("b1_limited") },
            onBack = onBack
        )
        "b1_limited" -> BoardLimitedAccess(
            onSelect = { onNavigate("b1_signup") },
            onAllowAll = { onNavigate("b1_permissions") },
            onBack = onBack
        )
        "b1_signup" -> BoardSignUp(
            onGuest = {
                actions.continueAsGuest { onNavigate("b1_ready") }
            },
            onAuth = { onNavigate("b22_methods") },
            onLater = { onNavigate("b1_signup_later") },
            onBack = onBack
        )
        "b1_signup_later" -> BoardSignUpLater(
            onContinue = {
                actions.continueAsGuest { onNavigate("b1_ready") }
            },
            onBack = onBack
        )
        "b1_skip" -> BoardSkip(
            onGetStarted = { onNavigate("b1_permissions") },
            onSkip = { onNavigate("b1_permissions") },
            onBack = onBack
        )
        "b1_returning" -> BoardReturning(
            onContinue = {
                actions.continueAsGuest { onNavigate("b1_ready") }
            },
            onBack = onBack
        )
        "b1_whats_new" -> BoardWhatsNew(
            onSee = {
                actions.dismissWhatsNew()
                onNavigate("b2_home")
            },
            onLater = {
                actions.dismissWhatsNew()
                onNavigate("b2_home")
            },
            onBack = onBack
        )
        "b1_ready" -> BoardReadyToCreate(
            onEnterHome = { onNavigate("b2_home") },
            onEditor = { onNavigate("b15_open") },
            onRestore = { onNavigate("b_c05") },
            onStickers = { onNavigate("b13_photo") },
            onMove = { onNavigate("b14_input") },
            onTell = { onNavigate("b7_sheet") },
            onBack = onBack
        )
        else -> BoardSplash(
            onFinished = {
                actions.markPriorLaunch()
                onNavigate("b1_welcome")
            }
        )
    }
}

@Composable
private fun BoardSplash(onFinished: () -> Unit) {
    val colors = BoardTheme.colors
    val typography = BoardTheme.typography
    LaunchedEffect(Unit) {
        delay(1600)
        onFinished()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TestTags.FLOW1_SPLASH)
            .background(colors.splashTop)
            .background(
                Brush.verticalGradient(
                    listOf(colors.splashTop, colors.splashMid, colors.primary.copy(alpha = 0.55f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(BoardFlowArt.logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(128.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Text(
                "PixiJoy",
                color = Color.White,
                style = typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            Text(
                stringResource(R.string.board_tagline),
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = PixiSpacing.xxl)
            )
            Spacer(modifier = Modifier.height(PixiSpacing.xxl))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(4.dp),
                color = colors.primary,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            Text(
                stringResource(R.string.board_splash_loading),
                color = Color.White.copy(alpha = 0.75f),
                style = typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BoardWelcome(onNext: () -> Unit, onSkip: () -> Unit, onBack: (() -> Unit)?) {
    OnboardScaffold(
        modifier = Modifier.testTag(TestTags.FLOW1_WELCOME),
        title = stringResource(R.string.board_welcome_title),
        body = stringResource(R.string.board_welcome_body),
        primary = stringResource(R.string.next),
        onPrimary = onNext,
        secondary = stringResource(R.string.skip),
        onSecondary = onSkip,
        page = 0,
        onBack = onBack,
        pulsePrimary = true
    ) {
        Image(
            painter = painterResource(BoardFlowArt.logo),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.CenterHorizontally),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(PixiSpacing.md))
        BoardHeroImage(
            resId = R.drawable.board_onboard_welcome_collage,
            aspectRatio = 1f
        )
    }
}

@Composable
private fun BoardKeyFeatures(onNext: () -> Unit, onSkip: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val features = listOf(
        Triple(R.string.board_feat_beautiful, BoardFlowArt.beautifulHero, colors.accentPink),
        Triple(R.string.board_feat_funny, BoardFlowArt.funnyHero, colors.accentYellow),
        Triple(R.string.board_feat_stickers, BoardFlowArt.stickerHero, colors.primary),
        Triple(R.string.board_feat_move, BoardFlowArt.motionHero, colors.accentPurple),
        Triple(R.string.board_feat_different, BoardFlowArt.differentHero, colors.primary),
        Triple(R.string.board_feat_more, R.drawable.board_onboard_features_grid, colors.accentPink)
    )
    OnboardScaffold(
        modifier = Modifier.testTag(TestTags.FLOW1_FEATURES),
        title = stringResource(R.string.board_features_headline),
        body = null,
        primary = stringResource(R.string.next),
        onPrimary = onNext,
        secondary = stringResource(R.string.skip),
        onSecondary = onSkip,
        page = 1,
        onBack = onBack
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
            features.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEach { (titleRes, imageRes, accent) ->
                        FeatureCard(
                            title = stringResource(titleRes),
                            imageRes = imageRes,
                            accent = accent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    @DrawableRes imageRes: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(radii.card))
            .background(accent.copy(alpha = 0.12f))
            .padding(PixiSpacing.sm)
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
                .clip(RoundedCornerShape(radii.md)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(PixiSpacing.xs))
        Text(
            title,
            style = BoardTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun BoardForEveryone(onNext: () -> Unit, onSkip: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    OnboardScaffold(
        modifier = Modifier.testTag(TestTags.FLOW1_FOR_EVERYONE),
        title = stringResource(R.string.board_everyone_title),
        body = stringResource(R.string.board_everyone_body),
        primary = stringResource(R.string.next),
        onPrimary = onNext,
        secondary = stringResource(R.string.skip),
        onSecondary = onSkip,
        page = 2,
        onBack = onBack
    ) {
        BoardHeroImage(
            resId = R.drawable.board_onboard_welcome_collage,
            aspectRatio = 1f
        )
        Spacer(modifier = Modifier.height(PixiSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EveryonePill(R.drawable.pj_ic_add, stringResource(R.string.board_everyone_create))
            EveryonePill(R.drawable.pj_ic_share, stringResource(R.string.board_everyone_share))
            EveryonePill(R.drawable.pj_ic_heart, stringResource(R.string.board_everyone_love))
        }
        Spacer(modifier = Modifier.height(PixiSpacing.md))
        Text(
            stringResource(R.string.board_everyone_media),
            style = BoardTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EveryonePill(@DrawableRes icon: Int, label: String) {
    val colors = BoardTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.surfaceMuted),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(icon), contentDescription = label, tint = colors.primary)
        }
        Spacer(modifier = Modifier.height(PixiSpacing.xs))
        Text(label, color = colors.textPrimary, style = BoardTheme.typography.labelMedium)
    }
}

@Composable
private fun BoardPermissions(onAllow: () -> Unit, onNotNow: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val context = LocalContext.current
    val permission = MediaPermissionHelper.galleryReadPermission()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onAllow() else onNotNow()
    }
    OnboardScaffold(
        modifier = Modifier.testTag(TestTags.FLOW1_PERMISSIONS),
        title = stringResource(R.string.board_perm_title),
        body = null,
        primary = stringResource(R.string.board_perm_allow),
        onPrimary = {
            if (MediaPermissionHelper.hasGalleryReadPermission(context)) {
                onAllow()
            } else {
                launcher.launch(permission)
            }
        },
        secondary = stringResource(R.string.board_perm_not_now),
        onSecondary = onNotNow,
        page = 3,
        onBack = onBack
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = PixiSpacing.md),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(BoardTheme.radii.card))
                    .background(colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_camera),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
            listOf(
                R.string.board_perm_item_take,
                R.string.board_perm_item_gallery,
                R.string.board_perm_item_create
            ).forEach { res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BoardTheme.radii.md))
                        .background(colors.surfaceMuted)
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_check_circle),
                        contentDescription = null,
                        tint = colors.success,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.size(PixiSpacing.md))
                    Text(stringResource(res), color = colors.textPrimary)
                }
            }
        }
        Spacer(modifier = Modifier.height(PixiSpacing.md))
        Text(
            stringResource(R.string.board_perm_tip),
            color = colors.textSecondary,
            style = BoardTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BoardPermissionDenied(onRetry: () -> Unit, onLimited: () -> Unit, onBack: () -> Unit) {
    OnboardScaffold(
        modifier = Modifier.testTag(TestTags.FLOW1_PERM_DENIED),
        title = stringResource(R.string.board_perm_denied_title),
        body = stringResource(R.string.board_perm_denied_body),
        primary = stringResource(R.string.try_again),
        onPrimary = onRetry,
        secondary = stringResource(R.string.board_perm_limited),
        onSecondary = onLimited,
        page = -1,
        onBack = onBack
    ) {
        EdgeIcon(R.drawable.pj_ic_no_photo)
    }
}

@Composable
private fun BoardLimitedAccess(onSelect: () -> Unit, onAllowAll: () -> Unit, onBack: () -> Unit) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) onSelect()
    }
    OnboardScaffold(
        modifier = Modifier.testTag(TestTags.FLOW1_LIMITED),
        title = stringResource(R.string.board_limited_title),
        body = stringResource(R.string.board_limited_body),
        primary = stringResource(R.string.board_limited_select),
        onPrimary = {
            picker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        secondary = stringResource(R.string.board_limited_allow_all),
        onSecondary = onAllowAll,
        page = -1,
        onBack = onBack
    ) {
        EdgeIcon(R.drawable.pj_ic_gallery)
    }
}

@Composable
private fun BoardSignUp(onGuest: () -> Unit, onAuth: () -> Unit, onLater: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    // Guest is the primary product path — no mandatory account (Flow 1 success criteria).
    OnboardScaffold(
        modifier = Modifier.testTag(TestTags.FLOW1_SIGNUP),
        title = stringResource(R.string.board_signup_title),
        body = null,
        primary = stringResource(R.string.flow1_continue_guest),
        onPrimary = onGuest,
        primaryTestTag = TestTags.ONBOARDING_CONTINUE_GUEST,
        secondary = stringResource(R.string.board_signup_later),
        onSecondary = onLater,
        page = 4,
        onBack = onBack
    ) {
        Text(
            stringResource(R.string.board_oauth_preview),
            style = BoardType.caption,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(PixiSpacing.sm))
        Image(
            painter = painterResource(BoardFlowArt.profileAvatar),
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .align(Alignment.CenterHorizontally),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(PixiSpacing.md))
        Column(verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
            listOf(
                R.string.board_signup_benefit_save,
                R.string.board_signup_benefit_devices,
                R.string.board_signup_benefit_ideas
            ).forEach { res ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painter = painterResource(R.drawable.pj_ic_check_circle),
                        null,
                        tint = colors.success,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(PixiSpacing.sm))
                    Text(stringResource(res), color = colors.textPrimary, style = BoardType.body)
                }
            }
        }
        Spacer(modifier = Modifier.height(PixiSpacing.md))
        Text(
            stringResource(R.string.board_signup_or),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(PixiSpacing.sm))
        BoardOutlinedButton(
            text = stringResource(R.string.flow1_continue_google),
            onClick = {
                context.toastOauthPreview()
                onAuth()
            }
        )
        Spacer(modifier = Modifier.height(PixiSpacing.sm))
        BoardOutlinedButton(
            text = stringResource(R.string.flow1_continue_apple),
            onClick = {
                context.toastOauthPreview()
                onAuth()
            }
        )
        Spacer(modifier = Modifier.height(PixiSpacing.sm))
        BoardOutlinedButton(
            text = stringResource(R.string.flow1_continue_email),
            onClick = {
                context.toastOauthPreview()
                onAuth()
            }
        )
        Spacer(modifier = Modifier.height(PixiSpacing.sm))
        BoardSecondaryButton(
            text = stringResource(R.string.board_signup_have_account),
            onClick = {
                context.toastOauthPreview()
                onAuth()
            }
        )
    }
}

@Composable
private fun BoardSignUpLater(onContinue: () -> Unit, onBack: () -> Unit) {
    OnboardScaffold(
        modifier = Modifier.testTag(TestTags.FLOW1_SIGNUP_LATER),
        title = stringResource(R.string.board_signup_later_title),
        body = stringResource(R.string.board_signup_later_body),
        primary = stringResource(R.string.continue_label),
        onPrimary = onContinue,
        page = -1,
        onBack = onBack
    ) {
        EdgeIcon(R.drawable.pj_ic_schedule)
    }
}

@Composable
private fun BoardSkip(onGetStarted: () -> Unit, onSkip: () -> Unit, onBack: () -> Unit) {
    OnboardScaffold(
        modifier = Modifier.testTag(TestTags.FLOW1_SKIP),
        title = stringResource(R.string.board_skip_title),
        body = stringResource(R.string.board_skip_body),
        primary = stringResource(R.string.board_get_started),
        onPrimary = onGetStarted,
        secondary = stringResource(R.string.skip),
        onSecondary = onSkip,
        page = -1,
        onBack = onBack
    )
}

@Composable
private fun BoardReturning(onContinue: () -> Unit, onBack: () -> Unit) {
    OnboardScaffold(
        modifier = Modifier.testTag(TestTags.FLOW1_RETURNING),
        title = stringResource(R.string.board_returning_title),
        body = stringResource(R.string.board_returning_body),
        primary = stringResource(R.string.continue_label),
        onPrimary = onContinue,
        page = -1,
        onBack = onBack
    ) {
        EdgeIcon(R.drawable.pj_ic_wave)
    }
}

@Composable
private fun BoardWhatsNew(onSee: () -> Unit, onLater: () -> Unit, onBack: () -> Unit) {
    OnboardScaffold(
        modifier = Modifier.testTag(TestTags.FLOW1_WHATS_NEW),
        title = stringResource(R.string.board_whats_new_title),
        body = stringResource(R.string.board_whats_new_body),
        primary = stringResource(R.string.board_see_whats_new),
        onPrimary = onSee,
        secondary = stringResource(R.string.board_maybe_later),
        onSecondary = onLater,
        page = -1,
        onBack = onBack
    ) {
        EdgeIcon(R.drawable.pj_ic_gift)
    }
}

@Composable
private fun BoardReadyToCreate(
    onEnterHome: () -> Unit,
    onEditor: () -> Unit,
    onRestore: () -> Unit,
    onStickers: () -> Unit,
    onMove: () -> Unit,
    onTell: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    OnboardScaffold(
        modifier = Modifier.testTag(TestTags.FLOW1_READY),
        title = stringResource(R.string.board_ready_title),
        body = stringResource(R.string.board_ready_body),
        primary = stringResource(R.string.board_enter_app),
        onPrimary = onEnterHome,
        page = -1,
        onBack = onBack
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Image(
                painter = painterResource(BoardFlowArt.profileAvatar),
                contentDescription = stringResource(R.string.nav_profile),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(PixiSpacing.sm))
        val tiles = listOf(
            Triple(R.string.flow15_title, colors.primary to BoardFlowArt.editorHero, onEditor),
            Triple(R.string.create_extra_restore, colors.accentPink to R.drawable.board_c05_before, onRestore),
            Triple(R.string.create_stickers, colors.primary to BoardFlowArt.stickerHero, onStickers),
            Triple(R.string.make_it_move, colors.accentPurple to BoardFlowArt.motionHero, onMove)
        )
        Column(verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
            tiles.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEach { (titleRes, style, onClick) ->
                        val (accent, imageRes) = style
                        BoardIntentTile(
                            title = stringResource(titleRes),
                            accent = accent,
                            imageRes = imageRes,
                            onClick = onClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(PixiSpacing.md))
        OutlinedTextField(
            value = "",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTell),
            placeholder = { Text(stringResource(R.string.board_ready_tell_hint)) },
            leadingIcon = {
                Icon(painter = painterResource(R.drawable.pj_ic_mic), contentDescription = null, tint = colors.primary)
            },
            trailingIcon = {
                IconButton(onClick = onTell) {
                    Icon(painter = painterResource(R.drawable.pj_ic_forward), contentDescription = null, tint = colors.primary)
                }
            },
            shape = RoundedCornerShape(radii.md)
        )
    }
}

@Composable
private fun EdgeIcon(@DrawableRes icon: Int) {
    val colors = BoardTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PixiSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(BoardTheme.radii.card))
                .background(colors.surfaceMuted),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = colors.primary, modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun OnboardScaffold(
    title: String,
    body: String?,
    primary: String,
    onPrimary: () -> Unit,
    secondary: String? = null,
    onSecondary: (() -> Unit)? = null,
    page: Int,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    pulsePrimary: Boolean = false,
    primaryTestTag: String? = null,
    content: @Composable (androidx.compose.foundation.layout.ColumnScope.() -> Unit)? = null
) {
    val colors = BoardTheme.colors
    val typography = BoardTheme.typography
    val canPop = LocalBoardCanPop.current
    BoardAdaptiveFrame {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                if (onBack != null && canPop) {
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
                    }
                }
                Text(
                    title,
                    style = BoardType.title,
                    color = colors.textPrimary
                )
                if (body != null) {
                    Spacer(modifier = Modifier.height(PixiSpacing.md))
                    Text(body, style = BoardType.body, color = colors.textSecondary)
                }
                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                content?.invoke(this)
            }
            Column {
                if (page in 0..4) {
                    BoardPageDots(total = 5, selected = page)
                    Spacer(modifier = Modifier.height(PixiSpacing.md))
                }
                val primaryMod = if (primaryTestTag != null) {
                    Modifier.testTag(primaryTestTag)
                } else {
                    Modifier
                }
                if (pulsePrimary) {
                    com.pixijoy.ai.ui.board.components.BoardSoftPulse(active = true) {
                        BoardPrimaryButton(text = primary, onClick = onPrimary, modifier = primaryMod)
                    }
                } else {
                    BoardPrimaryButton(text = primary, onClick = onPrimary, modifier = primaryMod)
                }
                if (secondary != null && onSecondary != null) {
                    Spacer(modifier = Modifier.height(PixiSpacing.sm))
                    BoardOutlinedButton(
                        text = secondary,
                        onClick = onSecondary,
                        modifier = if (secondary == stringResource(R.string.skip)) {
                            Modifier.testTag(TestTags.ONBOARDING_SKIP)
                        } else {
                            Modifier
                        }
                    )
                }
                BoardTrustFooter()
            }
        }
    }
}
