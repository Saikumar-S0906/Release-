package com.pixijoy.ai.ui.board.me

import androidx.annotation.DrawableRes

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.pixijoy.ai.ui.board.openPlaySubscriptions
import com.pixijoy.ai.ui.board.toastBillingPlaySoon
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

/** Flow 10 session — plan / payment choices across checkout. */
internal object BoardFlow10Session {
    var yearly: Boolean = false
    var paymentMethod: Int = 0
    var promoCode: String = ""
    var isPro: Boolean = false
    var discoverFilter: Int = 2

    fun priceLabel(): String = if (yearly) "₹999 / year" else "₹199 / month"
    fun planLabel(): String = if (yearly) "PixiJoy Pro Yearly Plan" else "PixiJoy Pro Monthly Plan"
}

private val ProGradient = Brush.horizontalGradient(
    listOf(Color(0xFF7C3AED), Color(0xFFEC4899))
)
private val WelcomeBg = Brush.verticalGradient(
    listOf(Color(0xFF1A0B2E), Color(0xFF2E1065), Color(0xFF0B0B10))
)

/**
 * Flow 10 — Upgrade to PixiJoy Pro.
 * Board shows a Pro tab; production keeps 4 tabs — Pro entry stays route-based
 * (Discover / Profile / openPro), not a 5th bottom tab.
 */
@Composable
fun BoardFlow10Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    when (screenId) {
        "b10_hub", "b10_discover" -> DiscoverPremium(
            onUpgrade = { onNavigate("b10_compare") },
            onTab = tab
        )
        "b10_compare" -> ComparePlans(
            onUpgrade = { onNavigate("b10_benefits") },
            onTrial = { onNavigate("b10_benefits") },
            onBack = { onNavigate("b10_discover") }
        )
        "b10_benefits" -> HighlightBenefits(
            onContinue = { onNavigate("b10_pay") },
            onBack = { onNavigate("b10_compare") }
        )
        "b10_pay" -> ChoosePayment(
            onContinue = { onNavigate("b10_confirm") },
            onBack = { onNavigate("b10_benefits") }
        )
        "b10_confirm" -> OrderSummary(
            onContinue = { /* toast inside OrderSummary — no fake Pro */ },
            onBack = { onNavigate("b10_pay") }
        )
        "b10_welcome" -> ProWelcome(
            onExplore = { onNavigate("b10_inapp") },
            onManage = { onNavigate("b10_manage") }
        )
        "b10_inapp" -> ProInApp(
            onTab = tab,
            onManage = { onNavigate("b10_manage") },
            onOpenStyle = { onNavigate("b5_choose") }
        )
        "b10_manage" -> ManageSubscription(
            onBack = { onNavigate("b10_inapp") },
            onHelp = { onNavigate("b11_help") },
            onCancel = {
                BoardFlow10Session.isPro = false
                onNavigate("b10_discover")
            }
        )
        else -> DiscoverPremium(
            onUpgrade = { onNavigate("b10_compare") },
            onTab = tab
        )
    }
}

@Composable
private fun DiscoverPremium(
    onUpgrade: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val features = listOf(
        R.string.board_f10_feat_styles,
        R.string.board_f10_feat_hd,
        R.string.board_f10_feat_no_ads,
        R.string.board_f10_feat_video,
        R.string.board_f10_feat_early
    )
    BoardMainShell(selected = BoardShellTab.Discover, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW10_HOST),
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(radii.pill))
                        .background(colors.accentYellow)
                        .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.xs)
                ) {
                    Text(
                        stringResource(R.string.board_f10_pro_chip),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A0B2E)
                    )
                }
            }
            Image(
                painter = painterResource(BoardFlowArt.proHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.board_f10_hero_title),
                style = BoardType.headline,
                color = colors.textPrimary
            )
            Text(
                stringResource(R.string.board_f10_hero_sub),
                style = BoardType.body,
                color = colors.textSecondary
            )
            features.forEach { res ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.textPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painter = painterResource(R.drawable.pj_ic_check),
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(stringResource(res), color = colors.textPrimary)
                }
            }
            GradientCta(
                text = stringResource(R.string.board_f10_upgrade_cta),
                onClick = onUpgrade
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_lock), null, tint = colors.success, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(R.string.board_f10_secure),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun ComparePlans(
    onUpgrade: () -> Unit,
    onTrial: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var yearly by remember { mutableStateOf(BoardFlow10Session.yearly) }
    val freeFeatures = listOf(
        R.string.board_f10_feat_styles,
        R.string.board_f10_feat_hd,
        R.string.board_f10_feat_no_ads,
        R.string.board_f10_feat_video
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
                    stringResource(R.string.board_f10_choose_plan),
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
                listOf(
                    false to R.string.board_f10_monthly,
                    true to R.string.board_f10_yearly
                ).forEach { (isYearly, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (yearly == isYearly) colors.surface else Color.Transparent)
                            .clickable {
                                yearly = isYearly
                                BoardFlow10Session.yearly = isYearly
                            }
                            .padding(vertical = PixiSpacing.sm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(label),
                            fontWeight = if (yearly == isYearly) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (yearly == isYearly) colors.primary else colors.textSecondary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            PlanCompareCard(
                title = stringResource(R.string.board_f10_free),
                price = stringResource(R.string.board_f10_free_price),
                features = freeFeatures,
                pro = false,
                cta = stringResource(R.string.board_f10_current_plan),
                ctaEnabled = false,
                onCta = {}
            )
            PlanCompareCard(
                title = stringResource(R.string.flow10_title),
                price = if (yearly) {
                    stringResource(R.string.board_f10_pro_yearly_price)
                } else {
                    stringResource(R.string.board_f10_pro_monthly_price)
                },
                features = freeFeatures + R.string.board_f10_feat_early,
                pro = true,
                cta = stringResource(R.string.board_f10_upgrade_now),
                ctaEnabled = true,
                onCta = onUpgrade
            )
            TextButton(onClick = onTrial, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.board_f10_trial), color = colors.primary)
            }
        }
    }
}

@Composable
private fun PlanCompareCard(
    title: String,
    price: String,
    features: List<Int>,
    pro: Boolean,
    cta: String,
    ctaEnabled: Boolean,
    onCta: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radii.card))
            .border(
                width = if (pro) 2.dp else 1.dp,
                color = if (pro) colors.accentPurple else colors.surfaceMuted,
                shape = RoundedCornerShape(radii.card)
            )
            .background(if (pro) colors.accentPurple.copy(alpha = 0.06f) else colors.surface)
            .padding(PixiSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Text(price, style = MaterialTheme.typography.titleMedium, color = if (pro) colors.accentPurple else colors.textSecondary)
        features.forEach { res ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                Icon(
                    painter = painterResource(if (pro) R.drawable.pj_ic_check_circle else R.drawable.pj_ic_close),
                    contentDescription = null,
                    tint = if (pro) colors.success else colors.danger,
                    modifier = Modifier.size(18.dp)
                )
                Text(stringResource(res), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (ctaEnabled) {
            GradientCta(text = cta, onClick = onCta)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(radii.pill))
                    .background(colors.surfaceMuted),
                contentAlignment = Alignment.Center
            ) {
                Text(cta, color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun HighlightBenefits(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val benefits = listOf(
        Triple(R.drawable.pj_ic_sparkles, colors.accentPurple, R.string.board_f10_feat_styles),
        Triple(R.drawable.pj_ic_premium, colors.primary, R.string.board_f10_feat_hd),
        Triple(R.drawable.pj_ic_block, colors.danger, R.string.board_f10_feat_no_ads),
        Triple(R.drawable.pj_ic_movie, colors.accentPink, R.string.board_f10_feat_video),
        Triple(R.drawable.pj_ic_star, colors.accentYellow, R.string.board_f10_feat_templates),
        Triple(R.drawable.pj_ic_time, Color(0xFF26A69A), R.string.board_f10_feat_early),
        Triple(R.drawable.pj_ic_chat, Color(0xFF6366F1), R.string.board_f10_feat_support)
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
                Icon(painter = painterResource(R.drawable.pj_ic_premium), null, tint = colors.accentYellow)
                Spacer(modifier = Modifier.size(PixiSpacing.sm))
                Text(
                    stringResource(R.string.flow10_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            benefits.forEach { (icon, tint, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(colors.surfaceMuted)
                        .padding(PixiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(radii.md))
                            .background(tint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painter = painterResource(icon), null, tint = tint)
                    }
                    Text(stringResource(label), fontWeight = FontWeight.Medium, color = colors.textPrimary)
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.flow10_continue), onClick = onContinue)
        }
    }
}

@Composable
private fun ChoosePayment(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlow10Session.paymentMethod) }
    val methodIcons = listOf(
        R.drawable.pj_ic_payments,
        R.drawable.pj_ic_payments,
        R.drawable.pj_ic_payments,
        R.drawable.pj_ic_payments,
        R.drawable.pj_ic_payments
    )
    val methodLabels = listOf(
        R.string.board_f10_pay_play,
        R.string.board_f10_pay_upi,
        R.string.board_f10_pay_card,
        R.string.board_f10_pay_net,
        R.string.board_f10_pay_wallet
    )
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
                    stringResource(R.string.flow10_payment),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            methodLabels.forEachIndexed { i, label ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(if (i == selected) colors.primarySoft else colors.surfaceMuted)
                        .clickable {
                            selected = i
                            BoardFlow10Session.paymentMethod = i
                        }
                        .padding(PixiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Icon(painter = painterResource(methodIcons[i]), contentDescription = null, tint = colors.primary)
                    Text(
                        stringResource(label),
                        modifier = Modifier.weight(1f),
                        color = colors.textPrimary
                    )
                    RadioButton(selected = i == selected, onClick = {
                        selected = i
                        BoardFlow10Session.paymentMethod = i
                    })
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            BoardPrimaryButton(text = stringResource(R.string.flow10_continue), onClick = onContinue)
        }
    }
}

@Composable
private fun OrderSummary(onContinue: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var showPromo by remember { mutableStateOf(false) }
    var promo by remember { mutableStateOf(BoardFlow10Session.promoCode) }
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
                    stringResource(R.string.flow10_summary),
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
                    .padding(PixiSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)) {
                    Icon(painter = painterResource(R.drawable.pj_ic_premium), null, tint = colors.accentYellow, modifier = Modifier.size(40.dp))
                    Column {
                        Text(BoardFlow10Session.planLabel(), fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text(BoardFlow10Session.priceLabel(), color = colors.accentPurple)
                    }
                }
                Text("• ${stringResource(R.string.board_f10_billed_play)}", color = colors.textSecondary)
                Text("• ${stringResource(R.string.board_f10_cancel_anytime)}", color = colors.textSecondary)
                Text("• ${stringResource(R.string.board_f10_refund)}", color = colors.textSecondary)
            }
            if (showPromo) {
                OutlinedTextField(
                    value = promo,
                    onValueChange = {
                        promo = it
                        BoardFlow10Session.promoCode = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.board_f10_promo)) },
                    shape = RoundedCornerShape(radii.md)
                )
            } else {
                TextButton(onClick = { showPromo = true }) {
                    Text(stringResource(R.string.board_f10_have_promo), color = colors.primary)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.board_f10_total), fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(BoardFlow10Session.priceLabel(), fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
            GradientCta(
                text = stringResource(R.string.flow10_continue),
                onClick = {
                    context.toastBillingPlaySoon()
                    context.openPlaySubscriptions()
                    onContinue()
                }
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_lock), null, tint = colors.success, modifier = Modifier.size(16.dp))
                Text(
                    stringResource(R.string.board_f10_play_trusted),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun ProWelcome(onExplore: () -> Unit, onManage: () -> Unit) {
    val colors = BoardTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WelcomeBg)
            .padding(PixiSpacing.xl)
            .testTag(TestTags.FLOW10_HOST),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(painter = painterResource(R.drawable.pj_ic_premium),
            null,
            tint = colors.accentYellow,
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(PixiSpacing.lg))
        Text(
            stringResource(R.string.board_soft_preview_title),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(PixiSpacing.sm))
        Text(
            stringResource(R.string.board_billing_play_soon),
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(PixiSpacing.lg))
        listOf(
            R.string.board_f10_check_sub,
            R.string.board_f10_check_unlocked,
            R.string.board_f10_check_ready
        ).forEach { res ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = PixiSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success)
                Text(stringResource(res), color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(PixiSpacing.xl))
        GradientCta(text = stringResource(R.string.board_f10_start_exploring), onClick = onExplore)
        TextButton(onClick = onManage) {
            Text(stringResource(R.string.flow10_manage), color = colors.accentYellow)
        }
    }
}

@Composable
private fun ProInApp(
    onTab: (BoardShellTab) -> Unit,
    onManage: () -> Unit,
    onOpenStyle: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var filter by remember { mutableIntStateOf(BoardFlow10Session.discoverFilter) }
    val filters = listOf(
        R.string.board_f10_filter_all,
        R.string.board_f10_filter_trending,
        R.string.board_f10_filter_pro,
        R.string.board_f10_filter_anime
    )
    val items = listOf(
        BoardFlowArt.discoverFeatured to R.string.board_f10_style_3d,
        BoardFlowArt.trendPortrait to R.string.board_f10_style_cinema,
        BoardFlowArt.differentHero to R.string.board_f10_style_oil,
        BoardFlowArt.beautifulHero to R.string.board_f10_style_fantasy
    )
    BoardMainShell(selected = BoardShellTab.Discover, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.nav_discover),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(radii.pill))
                        .background(colors.accentYellow)
                        .clickable(onClick = onManage)
                        .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.xs)
                ) {
                    Text(
                        stringResource(R.string.board_f10_pro_chip),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A0B2E)
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                filters.forEachIndexed { i, res ->
                    val on = i == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (on) colors.primary else colors.surfaceMuted)
                            .clickable {
                                filter = i
                                BoardFlow10Session.discoverFilter = i
                            }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm)
                    ) {
                        Text(
                            stringResource(res),
                            color = if (on) colors.onPrimary else colors.textSecondary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                items.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        row.forEach { (res, label) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(radii.card))
                                    .clickable(onClick = onOpenStyle)
                            ) {
                                Image(
                                    painter = painterResource(res),
                                    contentDescription = stringResource(label),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(PixiSpacing.sm)
                                        .clip(RoundedCornerShape(radii.pill))
                                        .background(colors.accentYellow)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.board_f10_pro_chip),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1A0B2E)
                                    )
                                }
                                Text(
                                    stringResource(label),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(PixiSpacing.sm),
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManageSubscription(
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    fun openPlaySubs() {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/account/subscriptions")
                )
            )
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.board_f10_play_unavailable), Toast.LENGTH_SHORT).show()
        }
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
                    stringResource(R.string.board_f10_subscription),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.primarySoft)
                    .padding(PixiSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        BoardFlow10Session.planLabel(),
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(colors.success)
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.xs)
                    ) {
                        Text(
                            stringResource(R.string.board_f10_active),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Text(
                    stringResource(R.string.board_f10_next_billing),
                    color = colors.textSecondary
                )
            }
            ManageRow(R.drawable.pj_ic_payments, R.string.board_f10_manage_play, onClick = ::openPlaySubs)
            ManageRow(R.drawable.pj_ic_history, R.string.board_f10_pay_history) {
                Toast.makeText(context, context.getString(R.string.board_f10_history_stub), Toast.LENGTH_SHORT).show()
            }
            ManageRow(R.drawable.pj_ic_payments, R.string.board_f10_update_pay, onClick = ::openPlaySubs)
            ManageRow(R.drawable.pj_ic_block, R.string.flow10_cancel, onClick = onCancel)
            ManageRow(R.drawable.pj_ic_help, R.string.board_f10_need_help, onClick = onHelp)
            BoardOutlinedButton(text = stringResource(R.string.cd_back), onClick = onBack)
        }
    }
}

@Composable
private fun ManageRow(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
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
private fun GradientCta(text: String, onClick: () -> Unit) {
    val radii = BoardTheme.radii
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(radii.pill))
            .background(ProGradient)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
