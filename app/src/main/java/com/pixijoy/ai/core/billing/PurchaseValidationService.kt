package com.pixijoy.ai.core.billing

import com.pixijoy.ai.core.network.PurchaseValidationRequest
import com.pixijoy.ai.core.network.PixiJoyBackendApi
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PurchaseValidationService @Inject constructor(
    private val backendApi: PixiJoyBackendApi,
    @Named("backendConfigured") private val backendConfigured: Boolean
) {
    suspend fun validatePurchase(
        authToken: String,
        purchaseToken: String,
        productId: String,
        packageName: String
    ): Boolean {
        if (!backendConfigured) return true
        return runCatching {
            val response = backendApi.validatePurchase(
                authToken = authToken,
                request = PurchaseValidationRequest(
                    purchaseToken = purchaseToken,
                    productId = productId,
                    packageName = packageName
                )
            )
            response.valid && response.isPro
        }.getOrDefault(false)
    }
}
