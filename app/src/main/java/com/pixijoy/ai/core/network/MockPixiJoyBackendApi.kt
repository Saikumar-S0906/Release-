package com.pixijoy.ai.core.network

import com.google.gson.Gson
import com.pixijoy.ai.core.ai.LocalAiSimulator
import javax.inject.Inject

/**
 * In-app mock backend when PIXIJOY_API_BASE_URL is not set.
 * Simulates trends, templates, community feed, and AI proxy.
 * Point PIXIJOY_API_BASE_URL to a real server to replace this.
 */
class MockPixiJoyBackendApi @Inject constructor(
    private val localAi: LocalAiSimulator,
    private val gson: Gson
) : PixiJoyBackendApi {

    override suspend fun proxyGeminiRequest(authToken: String, request: ProxyAiRequest): ProxyAiResponse {
        val text = localAi.respondToPrompt(request.prompt)
        return ProxyAiResponse(text = text, model = "mock-gemini-flash")
    }

    override suspend fun validatePurchase(
        authToken: String,
        request: PurchaseValidationRequest
    ): PurchaseValidationResponse = PurchaseValidationResponse(
        valid = true,
        isPro = true,
        message = "Mock validation — replace with real backend"
    )

    override suspend fun fetchTrends(authToken: String): TrendsApiResponse = TrendsApiResponse(
        trends = listOf(
            TrendDto("birthday_wa", "🎂", "Birthday WhatsApp Status", "Funny family birthday status", "Create a funny birthday WhatsApp status for my daughter", "create_for_me"),
            TrendDto("reel_beat", "🎬", "Beat-Sync Reel", "Photo reel with music", "Make a viral reel with transitions", "ai_reels"),
            TrendDto("anime_avatar", "🧑‍🎨", "Anime Avatar", "Selfie to anime", "Turn my selfie into anime avatar", "avatar"),
            TrendDto("diwali_story", "🪔", "Diwali Story", "Festival greeting", "Create Diwali WhatsApp story in Telugu", "creative_agent"),
            TrendDto("office_meme", "💼", "Office Meme", "Work humor", "Make office humor meme", "meme_maker"),
            TrendDto("talking_bday", "🗣️", "Talking Birthday", "Voice + animation", "Birthday talking photo for my mom", "talking_photo")
        )
    )

    override suspend fun fetchTemplates(authToken: String, category: String?): TemplatesApiResponse =
        TemplatesApiResponse(
            templates = listOf(
                TemplateDto("tpl_bday_1", "birthday", "Pink Balloons", "🎂"),
                TemplateDto("tpl_diwali_1", "festival", "Diwali Lights", "🪔"),
                TemplateDto("tpl_wedding_1", "wedding", "Elegant Gold", "💍"),
                TemplateDto("tpl_meme_1", "meme", "Classic Top/Bottom", "😂"),
                TemplateDto("tpl_love_1", "love", "Hearts Gradient", "❤️"),
                TemplateDto("tpl_office_1", "office", "Monday Mood", "💼")
            ).filter { category.isNullOrBlank() || it.category == category }
        )

    override suspend fun fetchCommunityFeed(authToken: String): CommunityFeedResponse = CommunityFeedResponse(
        posts = listOf(
            CommunityPostDto("p1", "Priya", "Birthday reel for my daughter 🎂", 1240, "creative_agent"),
            CommunityPostDto("p2", "Rahul", "Office meme of the week 😂", 892, "meme_maker"),
            CommunityPostDto("p3", "Ananya", "Diwali sticker pack 🪔", 2103, "sticker_studio"),
            CommunityPostDto("p4", "Vikram", "Anime avatar trend 🧑‍🎨", 1567, "avatar")
        )
    )

    override suspend fun guestAuth(request: GuestAuthRequest): AuthResponse = AuthResponse(
        token = "mock_guest_${request.deviceId.take(8)}",
        userId = "guest_${request.deviceId}"
    )
}
