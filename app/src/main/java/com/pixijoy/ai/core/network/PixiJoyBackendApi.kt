package com.pixijoy.ai.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface PixiJoyBackendApi {

    @POST("v1/ai/generate")
    suspend fun proxyGeminiRequest(
        @Header("Authorization") authToken: String,
        @Body request: ProxyAiRequest
    ): ProxyAiResponse

    @POST("v1/billing/validate")
    suspend fun validatePurchase(
        @Header("Authorization") authToken: String,
        @Body request: PurchaseValidationRequest
    ): PurchaseValidationResponse

    @GET("v1/trends")
    suspend fun fetchTrends(
        @Header("Authorization") authToken: String = ""
    ): TrendsApiResponse

    @GET("v1/templates")
    suspend fun fetchTemplates(
        @Header("Authorization") authToken: String = "",
        @Query("category") category: String? = null
    ): TemplatesApiResponse

    @GET("v1/community/feed")
    suspend fun fetchCommunityFeed(
        @Header("Authorization") authToken: String = ""
    ): CommunityFeedResponse

    @POST("v1/auth/guest")
    suspend fun guestAuth(
        @Body request: GuestAuthRequest
    ): AuthResponse
}

data class ProxyAiRequest(
    val prompt: String,
    val imageBase64: String? = null,
    val mimeType: String = "image/jpeg",
    val locale: String = "en"
)

data class ProxyAiResponse(
    val text: String,
    val model: String? = null
)

data class PurchaseValidationRequest(
    @SerializedName("purchase_token") val purchaseToken: String,
    @SerializedName("product_id") val productId: String,
    @SerializedName("package_name") val packageName: String
)

data class PurchaseValidationResponse(
    val valid: Boolean,
    @SerializedName("is_pro") val isPro: Boolean = false,
    val message: String? = null
)

data class TrendDto(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val prompt: String,
    @SerializedName("tool_id") val toolId: String
)

data class TrendsApiResponse(val trends: List<TrendDto>)

data class TemplateDto(
    val id: String,
    val category: String,
    val name: String,
    val emoji: String,
    @SerializedName("preview_url") val previewUrl: String? = null
)

data class TemplatesApiResponse(val templates: List<TemplateDto>)

data class CommunityPostDto(
    val id: String,
    val author: String,
    val title: String,
    val likes: Int,
    @SerializedName("tool_id") val toolId: String
)

data class CommunityFeedResponse(val posts: List<CommunityPostDto>)

data class GuestAuthRequest(@SerializedName("device_id") val deviceId: String)

data class AuthResponse(
    val token: String,
    @SerializedName("user_id") val userId: String
)
