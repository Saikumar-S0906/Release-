package com.pixijoy.ai.data

import com.pixijoy.ai.R
import com.pixijoy.ai.core.network.PixiJoyBackendApi
import com.pixijoy.ai.domain.model.PhotoTool
import com.pixijoy.ai.domain.model.TrendItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrendsRepository @Inject constructor(
    private val backendApi: PixiJoyBackendApi,
    private val authRepository: AuthRepository
) {
    suspend fun trending(): List<TrendItem> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authRepository.currentToken().orEmpty()
            backendApi.fetchTrends(authToken = token).trends.map { dto ->
                TrendItem(
                    id = dto.id,
                    emoji = dto.emoji,
                    titleRes = 0,
                    descRes = 0,
                    prompt = dto.prompt,
                    tool = PhotoTool.fromId(dto.toolId) ?: PhotoTool.CREATE_FOR_ME,
                    title = dto.title,
                    description = dto.description
                )
            }
        }.getOrElse { localFallback() }
    }

    private fun localFallback(): List<TrendItem> = listOf(
        TrendItem("birthday_wa", "🎂", R.string.trend_birthday_wa, R.string.trend_birthday_wa_desc, "Create a funny birthday WhatsApp status for my daughter", PhotoTool.CREATE_FOR_ME),
        TrendItem("reel_transition", "🎬", R.string.trend_reel_transition, R.string.trend_reel_transition_desc, "Make a viral reel with transitions", PhotoTool.AI_REELS)
    )
}
