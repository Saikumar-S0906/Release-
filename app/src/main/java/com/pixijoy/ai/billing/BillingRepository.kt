package com.pixijoy.ai.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.pixijoy.ai.R
import com.pixijoy.ai.core.billing.PurchaseValidationService
import com.pixijoy.ai.data.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class BillingUiState(
    val isConnected: Boolean = false,
    val isLoading: Boolean = true,
    val isPro: Boolean = false,
    val products: Map<String, ProductDetails> = emptyMap(),
    val formattedPrices: Map<String, String> = emptyMap(),
    val messageRes: Int? = null,
    val messageArg: String? = null,
    val purchaseInProgress: Boolean = false
)

@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository,
    private val purchaseValidationService: PurchaseValidationService
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            com.android.billingclient.api.PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun startConnection() {
        if (billingClient.isReady) {
            queryProducts()
            restorePurchases()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.update { it.copy(isConnected = true) }
                    queryProducts()
                    restorePurchases()
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            messageRes = R.string.billing_unavailable,
                            messageArg = result.debugMessage
                        )
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.update { it.copy(isConnected = false) }
            }
        })
    }

    private fun queryProducts() {
        val subscriptionParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                ProductIds.subscriptions.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            ).build()

        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                ProductIds.inAppProducts.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            ).build()

        billingClient.queryProductDetailsAsync(subscriptionParams) { _, subsList ->
            billingClient.queryProductDetailsAsync(inAppParams) { _, inAppList ->
                val all = (subsList.orEmpty() + inAppList.orEmpty())
                val productMap = all.associateBy { it.productId }
                val prices = productMap.mapValues { (_, details) -> formatPrice(details) }
                _state.update {
                    it.copy(
                        products = productMap,
                        formattedPrices = prices,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun launchPurchase(activity: Activity, productId: String) {
        val details = _state.value.products[productId]
        if (details == null) {
            _state.update { it.copy(messageRes = R.string.billing_product_not_configured) }
            return
        }
        _state.update { it.copy(purchaseInProgress = true) }

        val productDetailsParams = if (details.productType == BillingClient.ProductType.SUBS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken.orEmpty())
                .build()
        } else {
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.update {
                it.copy(
                    purchaseInProgress = false,
                    messageRes = R.string.billing_payment_failed,
                    messageArg = result.debugMessage
                )
            }
        }
    }

    fun restorePurchases() {
        if (!billingClient.isReady) return

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { _, subsPurchases ->
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            ) { _, inAppPurchases ->
                val all = subsPurchases + inAppPurchases
                handlePurchases(all)
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        _state.update { it.copy(purchaseInProgress = false) }
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { handlePurchases(it) }
                _state.update { it.copy(messageRes = R.string.billing_welcome_pro) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.update { it.copy(messageRes = R.string.billing_cancelled) }
            }
            else -> {
                _state.update {
                    it.copy(
                        messageRes = R.string.billing_failed,
                        messageArg = result.debugMessage
                    )
                }
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val activePro = purchases.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.any { it in ProductIds.all }
        }
        purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach { purchase ->
                acknowledge(purchase)
                scope.launch {
                    val serverValidated = purchaseValidationService.validatePurchase(
                        authToken = "",
                        purchaseToken = purchase.purchaseToken,
                        productId = purchase.products.firstOrNull().orEmpty(),
                        packageName = context.packageName
                    )
                    if (serverValidated) {
                        preferencesRepository.setProStatus(true)
                        _state.update { it.copy(isPro = true) }
                    }
                }
            }

        scope.launch {
            preferencesRepository.setProStatus(activePro)
            _state.update { it.copy(isPro = activePro) }
        }
    }

    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { /* no-op */ }
    }

    private fun formatPrice(details: ProductDetails): String {
        val playPrice = when (details.productType) {
            BillingClient.ProductType.SUBS -> {
                details.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.firstOrNull()
                    ?.formattedPrice
            }
            else -> details.oneTimePurchaseOfferDetails?.formattedPrice
        }
        if (!playPrice.isNullOrBlank()) return playPrice
        val plan = ProPlan.entries.find { it.productId == details.productId }
        return plan?.let { context.getString(it.fallbackPriceRes) }.orEmpty()
    }

    fun clearMessage() {
        _state.update { it.copy(messageRes = null, messageArg = null) }
    }

    fun priceFor(productId: String): String {
        return _state.value.formattedPrices[productId]
            ?: ProPlan.entries.find { it.productId == productId }
                ?.let { context.getString(it.fallbackPriceRes) }
            ?: ""
    }
}
