package com.pixijoy.ai.ui.board.me

import androidx.annotation.DrawableRes

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.openPlaySubscriptions
import com.pixijoy.ai.ui.board.toastBillingPlaySoon
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardEmptyState
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardTitleText
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing

/** Flow 23 session — balance and selected pack across purchase steps. Soft mock only. */
internal object BoardFlow23Session {
    var balance: Int = 120
    var selectedCredits: Int = 500
    var selectedPriceLabel: String = "₹399"
}

private data class CreditPack(
    val credits: Int,
    val price: String,
    @StringRes val badge: Int? = null
)

private data class HistoryEntry(@StringRes val title: Int, val delta: Int)

/**
 * Flow 23 — AI Credits.
 * Purchase is mocked (Google Play consent UI only) — matches board soft limit.
 */
@Composable
fun BoardFlow23Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val pipeline = LocalBoardPipeline.current
    when (screenId) {
        "b23_hub" -> ProfileEntry(
            onCredits = { onNavigate("b23_dash") },
            onPro = { onNavigate("b24_plans") },
            onSettings = { onNavigate("b11_hub") },
            onCreations = { onNavigate("b18_browse") },
            onBack = onBack
        )
        "b23_dash" -> Dashboard(
            onBuy = { onNavigate("b23_packs") },
            onHow = { onNavigate("b23_costs") },
            onHistory = { onNavigate("b23_history") },
            onBack = onBack
        )
        "b23_costs" -> CostTable(
            onBuy = { onNavigate("b23_packs") },
            onBack = { onNavigate("b23_dash") }
        )
        "b23_packs" -> PackStore(
            onContinue = { onNavigate("b23_pay") },
            onBack = { onNavigate("b23_dash") }
        )
        "b23_pay" -> PaymentMethod(
            onPay = { /* toast inside PaymentMethod — no fake credit grant */ },
            onBack = { onNavigate("b23_packs") }
        )
        "b23_added" -> CreditsAdded(
            onCreate = { onNavigate("b23_use") },
            onReceipt = { onNavigate("b23_history") },
            onClose = { onNavigate("b23_dash") }
        )
        "b23_use" -> UseCredits(
            onCreate = pipeline.openMediaInput,
            onBack = { onNavigate("b23_dash") }
        )
        "b23_history" -> UsageHistory(
            onDone = { onNavigate("b23_dash") },
            onBack = { onNavigate("b23_dash") }
        )
        else -> Dashboard(
            onBuy = { onNavigate("b23_packs") },
            onHow = { onNavigate("b23_costs") },
            onHistory = { onNavigate("b23_history") },
            onBack = onBack
        )
    }
}

@Composable
private fun ProfileEntry(
    onCredits: () -> Unit,
    onPro: () -> Unit,
    onSettings: () -> Unit,
    onCreations: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val rows = listOf(
        Triple(R.drawable.pj_ic_gallery, R.string.board_f23_menu_creations, onCreations),
        Triple(R.drawable.pj_ic_wallet, R.string.board_f23_menu_credits, onCredits),
        Triple(R.drawable.pj_ic_premium, R.string.board_f23_menu_pro, onPro),
        Triple(R.drawable.pj_ic_settings, R.string.board_f23_menu_settings, onSettings),
        Triple(R.drawable.pj_ic_help, R.string.board_f23_menu_help, onBack)
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW23_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f23_profile),
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(BoardFlowArt.creditsHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.surfaceMuted)
                    .padding(PixiSpacing.lg)
            ) {
                Image(
                    painter = painterResource(BoardFlowArt.profileAvatar),
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column {
                    Text(BoardFlow22Session.displayName, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("@${BoardFlow22Session.username}", color = colors.textSecondary)
                }
            }
            rows.forEach { (icon, label, action) ->
                val isCredits = label == R.string.board_f23_menu_credits
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(if (isCredits) colors.primary.copy(0.08f) else colors.surfaceMuted)
                        .clickable(onClick = action)
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
                    if (isCredits) {
                        Text(
                            BoardFlow23Session.balance.toString(),
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                    Icon(painter = painterResource(R.drawable.pj_ic_chevron_right), null, tint = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun Dashboard(
    onBuy: () -> Unit,
    onHow: () -> Unit,
    onHistory: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    data class Link(@DrawableRes val icon: Int, @StringRes val label: Int, val action: () -> Unit)
    val links = listOf(
        Link(R.drawable.pj_ic_info, R.string.flow23_how, onHow),
        Link(R.drawable.pj_ic_history, R.string.flow23_history, onHistory),
        Link(R.drawable.pj_ic_gift, R.string.flow23_earn) {
            Toast.makeText(context, context.getString(R.string.board_f23_earn_soon), Toast.LENGTH_SHORT).show()
        },
        Link(R.drawable.pj_ic_help, R.string.board_f23_faq) {
            Toast.makeText(context, context.getString(R.string.board_f23_faq_soon), Toast.LENGTH_SHORT).show()
        }
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW23_HOST),
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
                Text(
                    stringResource(R.string.flow23_title),
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(BoardFlowArt.creditsHero),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.flow23_balance, BoardFlow23Session.balance),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary
            )
            Text(
                stringResource(R.string.board_f23_dash_body),
                textAlign = TextAlign.Center,
                color = colors.textSecondary
            )
            BoardPrimaryButton(text = stringResource(R.string.flow23_buy), onClick = onBuy)
            links.forEach { link ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .clickable(onClick = link.action)
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painter = painterResource(link.icon), null, tint = colors.primary)
                    Text(
                        stringResource(link.label),
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = PixiSpacing.md)
                    )
                    Icon(painter = painterResource(R.drawable.pj_ic_chevron_right), null, tint = colors.textSecondary)
                }
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun CostTable(onBuy: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val costs = listOf(
        Triple(R.drawable.pj_ic_magic, R.string.flow23_cost_edit, 5),
        Triple(R.drawable.pj_ic_landscape, R.string.flow23_cost_gen, 10),
        Triple(R.drawable.pj_ic_video, R.string.flow23_cost_video, 20),
        Triple(R.drawable.pj_ic_emoji, R.string.flow23_cost_sticker, 5),
        Triple(R.drawable.pj_ic_sparkles, R.string.flow23_cost_remove, 5)
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
                    stringResource(R.string.flow23_how_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            costs.forEach { (icon, label, cost) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
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
                    Text(
                        stringResource(R.string.board_f23_credits_n, cost),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC9A227)
                    )
                }
            }
            Text(
                stringResource(R.string.flow23_cost_note),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            BoardPrimaryButton(text = stringResource(R.string.flow23_buy), onClick = onBuy)
        }
    }
}

@Composable
private fun PackStore(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var tabOneTime by remember { mutableStateOf(true) }
    var selected by remember { mutableIntStateOf(BoardFlow23Session.selectedCredits) }
    val packs = listOf(
        CreditPack(100, "₹99"),
        CreditPack(500, "₹399", badge = R.string.flow26_popular),
        CreditPack(1_200, "₹799", badge = R.string.board_f23_off_20),
        CreditPack(3_000, "₹1,799", badge = R.string.board_f23_off_40),
        CreditPack(10_000, "₹4,999", badge = R.string.flow23_best_value)
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
                    stringResource(R.string.flow23_buy),
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
                listOf(true to R.string.flow23_one_time, false to R.string.flow23_best_value).forEach { (one, label) ->
                    val active = tabOneTime == one
                    Text(
                        stringResource(label),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (active) colors.primary else Color.Transparent)
                            .clickable { tabOneTime = one }
                            .padding(vertical = PixiSpacing.sm),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) colors.onPrimary else colors.textSecondary
                    )
                }
            }
            packs.forEach { pack ->
                val active = selected == pack.credits
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
                            selected = pack.credits
                            BoardFlow23Session.selectedCredits = pack.credits
                            BoardFlow23Session.selectedPriceLabel = pack.price
                        }
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.board_f23_pack_credits, pack.credits),
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        pack.badge?.let {
                            Text(
                                stringResource(it),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primary
                            )
                        }
                    }
                    Text(pack.price, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.flow23_continue_pay), onClick = onContinue)
        }
    }
}

@Composable
private fun PaymentMethod(onPay: () -> Unit, onBack: () -> Unit) {
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
                    stringResource(R.string.board_f23_payment_method),
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
                    if (active) {
                        Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.primary)
                    }
                }
            }
            Text(
                stringResource(R.string.board_f23_pay_secure),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_f23_pay_amount, BoardFlow23Session.selectedPriceLabel),
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
private fun CreditsAdded(onCreate: () -> Unit, onReceipt: () -> Unit, onClose: () -> Unit) {
    val colors = BoardTheme.colors
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.board_soft_preview_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
                IconButton(onClick = onClose) {
                    Icon(painter = painterResource(R.drawable.pj_ic_close), contentDescription = stringResource(R.string.cd_back))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(BoardFlowArt.creditsHero),
                contentDescription = null,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(BoardTheme.radii.card)),
                contentScale = ContentScale.Crop
            )
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
                modifier = Modifier.padding(vertical = PixiSpacing.sm)
            )
            Text(
                stringResource(R.string.board_soft_preview_tip),
                textAlign = TextAlign.Center,
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(PixiSpacing.xl))
            BoardPrimaryButton(text = stringResource(R.string.flow23_start), onClick = onCreate)
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardOutlinedButton(text = stringResource(R.string.board_f23_view_receipt), onClick = onReceipt)
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun UseCredits(onCreate: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val tools = listOf(
        Triple(R.drawable.pj_ic_magic, R.string.board_f23_tool_enhance, 5),
        Triple(R.drawable.pj_ic_transform, R.string.board_f23_tool_transform, 10),
        Triple(R.drawable.pj_ic_play, R.string.board_f23_tool_animate, 20),
        Triple(R.drawable.pj_ic_emoji, R.string.flow23_cost_sticker, 5)
    )
    var selected by remember { mutableIntStateOf(0) }
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
                    stringResource(R.string.flow23_use_hero),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(BoardFlowArt.homeHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            tools.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    row.forEach { (icon, label, cost) ->
                        val globalIdx = tools.indexOfFirst { it.second == label }
                        val active = selected == globalIdx
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(radii.card))
                                .border(
                                    width = if (active) 2.dp else 0.dp,
                                    color = if (active) colors.primary else Color.Transparent,
                                    shape = RoundedCornerShape(radii.card)
                                )
                                .background(colors.surfaceMuted)
                                .clickable { selected = globalIdx }
                                .padding(PixiSpacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                        ) {
                            Icon(painter = painterResource(icon), null, tint = colors.primary, modifier = Modifier.size(28.dp))
                            Text(
                                stringResource(label),
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                stringResource(R.string.board_f23_credits_n, cost),
                                color = Color(0xFFC9A227),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.flow23_create_ai), onClick = onCreate)
        }
    }
}

@Composable
private fun UsageHistory(onDone: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        R.string.board_f23_tab_all,
        R.string.board_f23_tab_used,
        R.string.board_f23_tab_purchased,
        R.string.board_f23_tab_earned
    )
    val today = listOf(
        HistoryEntry(R.string.board_f23_hist_enhance, -5),
        HistoryEntry(R.string.board_f23_hist_generate, -10),
        HistoryEntry(R.string.board_f23_hist_purchased, 500)
    )
    val yesterday = listOf(
        HistoryEntry(R.string.flow23_cost_sticker, -5),
        HistoryEntry(R.string.board_f23_hist_video, -20),
        HistoryEntry(R.string.board_f23_hist_bonus, 10)
    )
    fun filter(list: List<HistoryEntry>) = when (tab) {
        1 -> list.filter { it.delta < 0 }
        2 -> list.filter { it.title == R.string.board_f23_hist_purchased }
        3 -> list.filter { it.delta > 0 && it.title != R.string.board_f23_hist_purchased }
        else -> list
    }
    val todayFiltered = filter(today)
    val yesterdayFiltered = filter(yesterday)
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
                    Icon(
                        painter = painterResource(R.drawable.pj_ic_back),
                        contentDescription = stringResource(R.string.cd_back),
                        tint = colors.textPrimary
                    )
                }
                BoardTitleText(text = stringResource(R.string.board_m23_history))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.xs)
            ) {
                tabs.forEachIndexed { i, label ->
                    val active = tab == i
                    Text(
                        stringResource(label),
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (active) colors.primary else colors.surfaceMuted)
                            .clickable { tab = i }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                        color = if (active) colors.onPrimary else colors.textSecondary,
                        style = BoardType.caption,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (todayFiltered.isEmpty() && yesterdayFiltered.isEmpty()) {
                BoardEmptyState(
                    title = stringResource(R.string.board_empty_title),
                    body = stringResource(R.string.board_empty_body),
                    ctaLabel = stringResource(R.string.board_f21_got_it),
                    onCta = onDone
                )
            } else {
                HistorySection(stringResource(R.string.board_f23_today), todayFiltered)
                HistorySection(stringResource(R.string.board_f23_yesterday), yesterdayFiltered)
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f21_got_it), onClick = onDone)
        }
    }
}

@Composable
private fun HistorySection(title: String, entries: List<HistoryEntry>) {
    if (entries.isEmpty()) return
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Text(title, fontWeight = FontWeight.Bold, color = colors.textSecondary)
    entries.forEach { entry ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(radii.card))
                .background(colors.surfaceMuted)
                .padding(PixiSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(entry.title),
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (entry.delta > 0) "+${entry.delta}" else entry.delta.toString(),
                fontWeight = FontWeight.Bold,
                color = if (entry.delta > 0) colors.success else colors.danger
            )
        }
    }
}
