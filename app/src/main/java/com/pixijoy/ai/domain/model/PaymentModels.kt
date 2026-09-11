package com.pixijoy.ai.domain.model

import androidx.annotation.StringRes
import com.pixijoy.ai.R

enum class PaymentMethod(
    @StringRes val labelRes: Int,
    val emoji: String,
    @StringRes val descriptionRes: Int
) {
    GOOGLE_PAY(R.string.payment_google_pay, "G", R.string.payment_google_pay_desc),
    PHONEPE(R.string.payment_phonepe, "Pe", R.string.payment_phonepe_desc),
    UPI(R.string.payment_upi, "₹", R.string.payment_upi_desc),
    CREDIT_CARD(R.string.payment_credit_card, "💳", R.string.payment_credit_card_desc),
    DEBIT_CARD(R.string.payment_debit_card, "🏦", R.string.payment_debit_card_desc),
    PAYPAL(R.string.payment_paypal, "P", R.string.payment_paypal_desc)
}

data class ProPlanUi(
    @StringRes val planRes: Int,
    val productId: String,
    val price: String,
    @StringRes val subtitleRes: Int,
    @StringRes val badgeRes: Int? = null,
    val isSubscription: Boolean
)
