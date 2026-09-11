package com.pixijoy.ai.core.network

/**
 * Throws when a real backend URL is configured but the call should not use mock.
 * Replaced by [MockPixiJoyBackendApi] when no URL is set.
 */
class NoOpPixiJoyBackendApi : PixiJoyBackendApi {
    override suspend fun proxyGeminiRequest(authToken: String, request: ProxyAiRequest): ProxyAiResponse =
        throw UnsupportedOperationException("Use mock backend or configure PIXIJOY_API_BASE_URL")

    override suspend fun validatePurchase(authToken: String, request: PurchaseValidationRequest): PurchaseValidationResponse =
        PurchaseValidationResponse(valid = true, isPro = true, message = "Local validation")

    override suspend fun fetchTrends(authToken: String): TrendsApiResponse =
        throw UnsupportedOperationException("Backend not configured")

    override suspend fun fetchTemplates(authToken: String, category: String?): TemplatesApiResponse =
        throw UnsupportedOperationException("Backend not configured")

    override suspend fun fetchCommunityFeed(authToken: String): CommunityFeedResponse =
        throw UnsupportedOperationException("Backend not configured")

    override suspend fun guestAuth(request: GuestAuthRequest): AuthResponse =
        throw UnsupportedOperationException("Backend not configured")
}
