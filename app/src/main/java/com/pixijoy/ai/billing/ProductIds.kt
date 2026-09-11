package com.pixijoy.ai.billing

import androidx.annotation.StringRes
import com.pixijoy.ai.R

object ProductIds {
    const val PRO_MONTHLY = "pixijoy_pro_monthly"
    const val PRO_YEARLY = "pixijoy_pro_yearly"
    const val PRO_LIFETIME = "pixijoy_pro_lifetime"

    val subscriptions = listOf(PRO_MONTHLY, PRO_YEARLY)
    val inAppProducts = listOf(PRO_LIFETIME)
    val all = subscriptions + inAppProducts
}

enum class ProPlan(
    val productId: String,
    @StringRes val titleRes: Int,
    @StringRes val fallbackPriceRes: Int
) {
    YEARLY(ProductIds.PRO_YEARLY, R.string.plan_yearly, R.string.plan_yearly_fallback),
    MONTHLY(ProductIds.PRO_MONTHLY, R.string.plan_monthly, R.string.plan_monthly_fallback),
    LIFETIME(ProductIds.PRO_LIFETIME, R.string.plan_lifetime, R.string.plan_lifetime_fallback)
}
