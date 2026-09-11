package com.pixijoy.ai.data

import com.pixijoy.ai.core.network.PixiJoyBackendApi
import com.pixijoy.ai.core.network.TemplateDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ContentTemplate(
    val id: String,
    val category: String,
    val name: String,
    val emoji: String,
    val previewUrl: String? = null
)

@Singleton
class TemplateRepository @Inject constructor(
    private val backendApi: PixiJoyBackendApi,
    private val authRepository: AuthRepository
) {
    suspend fun fetch(category: String? = null): List<ContentTemplate> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authRepository.currentToken().orEmpty()
            backendApi.fetchTemplates(authToken = token, category = category).templates.map { it.toModel() }
        }.getOrElse { defaultTemplates() }
    }

    private fun TemplateDto.toModel() = ContentTemplate(id, category, name, emoji, previewUrl)

    private fun defaultTemplates() = listOf(
        ContentTemplate("tpl_bday", "birthday", "Birthday Balloons", "🎂"),
        ContentTemplate("tpl_diwali", "festival", "Diwali Lights", "🪔"),
        ContentTemplate("tpl_meme", "meme", "Classic Meme", "😂")
    )
}
