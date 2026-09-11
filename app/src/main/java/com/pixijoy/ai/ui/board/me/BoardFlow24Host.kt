package com.pixijoy.ai.ui.board.me

import android.widget.Toast
import androidx.annotation.DrawableRes
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Style
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
import androidx.compose.ui.graphics.Color
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
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.openPlaySubscriptions
import com.pixijoy.ai.ui.board.toastBillingPlaySoon
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing

/** Flow 24 session — plan / cancel choices. Soft mock only. */
internal object BoardFlow24Session {
    /** 0 = Free, 1 = Pro, 2 = Ultra */
    var planIndex: Int = 1
    var yearly: Boolean = false
    var isActive: Boolean = false

    fun planNameRes(): Int = when (planIndex) {
        2 -> R.string.board_f24_plan_ultra
        1 -> R.string.board_f24_plan_pro
        else -> R.string.board_f24_plan_free
    }

    fun priceLabel(): String = when {
        planIndex == 0 -> "₹0"
        yearly && planIndex == 2 -> "₹4,999 / year"
        yearly && planIndex == 1 -> "₹999 / year"
        planIndex == 2 -> "₹999 / month"
        else -> "₹199 / month"
    }

    fun payAmount(): String = when {
        planIndex == 0 -> "₹0"
        yearly && planIndex == 2 -> "₹4,999"
        yearly && planIndex == 1 -> "₹999"
        planIndex == 2 -> "₹999"
        else -> "₹199"
    }
}

/**
 * Flow 24 — Subscription Management.
 * Billing is mocked (Google Play UI only) — matches board soft limit.
 */
@Composable
fun BoardFlow24Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val pipeline = LocalBoardPipeline.current
    when (screenId) {
        "b24_hub", "b24_plans" -> UpgradePromo(
            onViewPlans = { onNavigate("b24_choose") },
            onBack = onBack
        )
        "b24_choose" -> ChoosePlan(
            onContinue = { onNavigate("b24_matrix") },
            onBack = { onNavigate("b24_plans") }
        )
        "b24_matrix" -> FeatureMatrix(
            onSelect = { onNavigate("b24_pay") },
            onBack = { onNavigate("b24_choose") }
        )
        "b24_pay" -> Payment(
            onPay = { /* toast inside Payment — no fake Pro activation */ },
            onBack = { onNavigate("b24_matrix") }
        )
        "b24_welcome" -> WelcomePro(
            onCreate = pipeline.openMediaInput,
            onManage = { onNavigate("b24_manage") },
            onBack = onBack
        )
        "b24_manage" -> ManageSub(
            onChangePlan = { onNavigate("b24_choose") },
            onPayment = { onNavigate("b24_pay") },
            onBilling = { onNavigate("b24_billing") },
            onCancel = { onNavigate("b24_cancel") },
            onBack = { onNavigate("b24_welcome") }
        )
        "b24_billing" -> BillingHistory(
            onDone = { onNavigate("b24_manage") },
            onBack = { onNavigate("b24_manage") }
        )
        "b24_cancel" -> CancelSurvey(
            onCancel = {
                BoardFlow24Session.isActive = false
                BoardFlow24Session.planIndex = 0
                onNavigate("b24_plans")
            },
            onKeep = { onNavigate("b24_manage") },
            onBack = { onNavigate("b24_manage") }
        )
        else -> UpgradePromo(
            onViewPlans = { onNavigate("b24_choose") },
            onBack = onBack
        )
    }
}

@Composable
private fun UpgradePromo(onViewPlans: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val features = listOf(
        R.drawable.pj_ic_block to R.string.board_f24_feat_no_ads,
        R.drawable.pj_ic_sparkles to R.string.board_f24_feat_unlimited,
        R.drawable.pj_ic_sparkles to R.string.board_f24_feat_styles,
        R.drawable.pj_ic_premium to R.string.board_f24_feat_hd,
        R.drawable.pj_ic_premium to R.string.board_f24_feat_early
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW24_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f24_upgrade_title),
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            Box {
                Image(
                    painter = painterResource(BoardFlowArt.proHero),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(radii.card)),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(PixiSpacing.lg)
                ) {
                    Text(
                        stringResource(R.string.board_f24_hero_line),
                        style = BoardType.title,
                        color = Color.White
                    )
                    Text(stringResource(R.string.board_f24_hero_sub), color = Color.White.copy(0.9f))
                }
            }
            features.forEach { (icon, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painter = painterResource(icon), null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    }
                    Text(stringResource(label), fontWeight = FontWeight.Medium, color = colors.textPrimary)
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.flow10_see_plans), onClick = onViewPlans)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun ChoosePlan(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var yearly by remember { mutableStateOf(BoardFlow24Session.yearly) }
    var selected by remember { mutableIntStateOf(BoardFlow24Session.planIndex.coerceIn(0, 2)) }
    val plans = listOf(
        Triple(R.string.board_f24_plan_free, if (yearly) "₹0 / year" else "₹0 / month", R.string.board_f24_free_blurb),
        Triple(R.string.board_f24_plan_pro, if (yearly) "₹999 / year" else "₹199 / month", R.string.board_f24_pro_blurb),
        Triple(R.string.board_f24_plan_ultra, if (yearly) "₹4,999 / year" else "₹999 / month", R.string.board_f24_ultra_blurb)
    )
    val selectedFeatures = listOf(
        R.string.board_f24_feat_no_ads,
        R.string.board_f24_feat_unlimited,
        R.string.board_f24_feat_styles,
        R.string.board_f24_feat_hd
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
                    stringResource(R.string.flow24_choose),
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
                Text(
                    stringResource(R.string.board_f24_monthly),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(radii.pill))
                        .background(if (!yearly) colors.primary else Color.Transparent)
                        .clickable {
                            yearly = false
                            BoardFlow24Session.yearly = false
                        }
                        .padding(vertical = PixiSpacing.sm),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    color = if (!yearly) colors.onPrimary else colors.textSecondary
                )
                Text(
                    stringResource(R.string.board_f24_yearly),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(radii.pill))
                        .background(if (yearly) colors.primary else Color.Transparent)
                        .clickable {
                            yearly = true
                            BoardFlow24Session.yearly = true
                        }
                        .padding(vertical = PixiSpacing.sm),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    color = if (yearly) colors.onPrimary else colors.textSecondary
                )
            }
            if (!yearly) {
                Text(
                    stringResource(R.string.board_f24_save_50),
                    color = colors.primary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.End)
                )
            }
            plans.forEachIndexed { index, (name, price, blurb) ->
                val active = selected == index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .border(
                            width = if (active) 2.dp else 1.dp,
                            color = if (active) colors.primary else colors.surfaceMuted,
                            shape = RoundedCornerShape(radii.card)
                        )
                        .background(if (active) colors.primary.copy(0.06f) else colors.surfaceMuted)
                        .clickable {
                            selected = index
                            BoardFlow24Session.planIndex = index
                        }
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = active, onClick = {
                        selected = index
                        BoardFlow24Session.planIndex = index
                    })
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(name), fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            if (index == 1) {
                                Text(
                                    stringResource(R.string.flow26_popular),
                                    modifier = Modifier.padding(start = PixiSpacing.sm),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.primary
                                )
                            }
                        }
                        Text(stringResource(blurb), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(price, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
            }
            if (selected > 0) {
                selectedFeatures.forEach { res ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                    ) {
                        Icon(painter = painterResource(R.drawable.pj_ic_check), null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        Text(stringResource(res), color = colors.textPrimary)
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.flow10_continue), onClick = onContinue)
        }
    }
}

@Composable
private fun FeatureMatrix(onSelect: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    data class RowSpec(@StringRes val feature: Int, val free: String, val pro: String, val ultra: String)
    val rows = listOf(
        RowSpec(R.string.board_f24_row_gens, "10/day", "Unlimited", "Unlimited"),
        RowSpec(R.string.board_f24_row_templates, "—", "✓", "✓"),
        RowSpec(R.string.board_f24_row_hd, "—", "✓", "✓"),
        RowSpec(R.string.board_f24_feat_no_ads, "—", "✓", "✓"),
        RowSpec(R.string.board_f24_row_video, "—", "✓", "✓"),
        RowSpec(R.string.board_f24_row_advanced, "—", "—", "✓"),
        RowSpec(R.string.board_f24_row_support, "—", "✓", "Priority"),
        RowSpec(R.string.board_f24_feat_early, "—", "✓", "✓"),
        RowSpec(R.string.board_f24_row_cloud, "1GB", "100GB", "1TB")
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
                    stringResource(R.string.flow24_matrix),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("", modifier = Modifier.weight(1.4f))
                listOf(R.string.board_f24_plan_free, R.string.board_f24_plan_pro, R.string.board_f24_plan_ultra).forEach {
                    Text(
                        stringResource(it),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textPrimary
                    )
                }
            }
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.md))
                        .background(colors.surfaceMuted)
                        .padding(PixiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(row.feature),
                        modifier = Modifier.weight(1.4f),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textPrimary
                    )
                    listOf(row.free, row.pro, row.ultra).forEach {
                        Text(
                            it,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f24_select_pro), onClick = {
                BoardFlow24Session.planIndex = 1
                onSelect()
            })
        }
    }
}

@Composable
private fun Payment(onPay: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val methods = listOf(
        R.drawable.pj_ic_bag to R.string.board_f23_pay_play,
        R.drawable.pj_ic_payments to R.string.board_f23_pay_card,
        R.drawable.pj_ic_payments to R.string.board_f23_pay_upi,
        R.drawable.pj_ic_wallet to R.string.board_f23_pay_net,
        R.drawable.pj_ic_star to R.string.board_f23_pay_wallets
    )
    var selected by remember { mutableIntStateOf(methods.first().second) }
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
                    stringResource(R.string.flow10_payment),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            methods.forEach { (icon, label) ->
                val active = selected == label
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .border(
                            width = if (active) 2.dp else 0.dp,
                            color = if (active) colors.primary else Color.Transparent,
                            shape = RoundedCornerShape(radii.card)
                        )
                        .background(colors.surfaceMuted)
                        .clickable { selected = label }
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painter = painterResource(icon), null, tint = colors.primary)
                    Text(
                        stringResource(label),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = PixiSpacing.md)
                    )
                    if (active) Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.primary)
                }
            }
            Text(
                stringResource(R.string.board_f23_pay_secure),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.board_f24_total, BoardFlow24Session.priceLabel()),
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_f23_pay_amount, BoardFlow24Session.payAmount()),
                onClick = {
                    if (selected == R.string.board_f23_pay_play) {
                        context.openPlaySubscriptions()
                    }
                    context.toastBillingPlaySoon()
                    onPay()
                }
            )
        }
    }
}

@Composable
private fun WelcomePro(onCreate: () -> Unit, onManage: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val checks = listOf(
        R.string.board_f24_feat_no_ads,
        R.string.board_f24_feat_unlimited,
        R.string.board_f24_feat_styles,
        R.string.board_f24_feat_hd,
        R.string.board_f24_feat_early
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
                    .background(colors.accentYellow.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_star), null, tint = colors.accentYellow, modifier = Modifier.size(64.dp))
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Text(
                stringResource(R.string.board_soft_preview_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.board_billing_play_soon),
                textAlign = TextAlign.Center,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = PixiSpacing.md)
            )
            checks.forEach { res ->
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
            BoardPrimaryButton(text = stringResource(R.string.flow23_start), onClick = onCreate)
            TextButton(onClick = onManage) {
                Text(stringResource(R.string.flow24_manage), color = colors.primary)
            }
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.cd_back), color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun ManageSub(
    onChangePlan: () -> Unit,
    onPayment: () -> Unit,
    onBilling: () -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    data class ManageRow(@DrawableRes val icon: Int, @StringRes val label: Int, val action: () -> Unit)
    val rows = listOf(
        ManageRow(R.drawable.pj_ic_premium, R.string.flow24_change_plan, onChangePlan),
        ManageRow(R.drawable.pj_ic_payments, R.string.board_f24_update_payment, onPayment),
        ManageRow(R.drawable.pj_ic_history, R.string.flow24_history, onBilling),
        ManageRow(R.drawable.pj_ic_bag, R.string.board_f10_manage_play) {
            context.openPlaySubscriptions()
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
                    stringResource(R.string.flow10_manage),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.primary.copy(0.08f))
                    .padding(PixiSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.xs)
            ) {
                Text(
                    stringResource(BoardFlow24Session.planNameRes()),
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(BoardFlow24Session.priceLabel(), color = colors.textSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.flow24_active),
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(colors.successSoft)
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.xs),
                        color = colors.success,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.board_f24_next_renewal),
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .clickable(onClick = row.action)
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painter = painterResource(row.icon), null, tint = colors.primary)
                    Text(
                        stringResource(row.label),
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = PixiSpacing.md)
                    )
                    Icon(painter = painterResource(R.drawable.pj_ic_chevron_right), null, tint = colors.textSecondary)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .clickable(onClick = onCancel)
                    .padding(PixiSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_block), null, tint = colors.danger)
                Text(stringResource(R.string.flow24_cancel), color = colors.danger, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun BillingHistory(onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val invoices = listOf(
        "Sep 15, 2026",
        "Aug 15, 2026",
        "Jul 15, 2026",
        "Jun 15, 2026",
        "May 15, 2026"
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
                    stringResource(R.string.flow24_history),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            invoices.forEach { date ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(date, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text("₹199", color = colors.textSecondary)
                    }
                    Text(
                        stringResource(R.string.board_f24_paid),
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(colors.successSoft)
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.xs),
                        color = colors.success,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            BoardOutlinedButton(
                text = stringResource(R.string.board_f24_download_receipts),
                onClick = {
                    Toast.makeText(context, context.getString(R.string.board_f24_receipts_soon), Toast.LENGTH_SHORT).show()
                }
            )
            BoardPrimaryButton(text = stringResource(R.string.board_f21_got_it), onClick = onDone)
        }
    }
}

@Composable
private fun CancelSurvey(onCancel: () -> Unit, onKeep: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val reasons = listOf(
        R.string.board_f24_reason_usage,
        R.string.board_f24_reason_price,
        R.string.board_f24_reason_tech,
        R.string.board_f24_reason_features,
        R.string.board_f24_reason_other
    )
    var selected by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }
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
                    stringResource(R.string.flow24_cancel_survey),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(BoardFlowArt.profileAvatar),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .align(Alignment.CenterHorizontally),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.board_f24_cancel_prompt),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            reasons.forEachIndexed { i, res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .clickable { selected = i }
                        .padding(PixiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected == i, onClick = { selected = i })
                    Text(stringResource(res), color = colors.textPrimary)
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.board_f24_cancel_note)) },
                shape = RoundedCornerShape(radii.md),
                minLines = 2
            )
            BoardPrimaryButton(text = stringResource(R.string.flow24_cancel), onClick = onCancel)
            TextButton(onClick = onKeep, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.board_f24_keep), color = colors.primary)
            }
        }
    }
}
