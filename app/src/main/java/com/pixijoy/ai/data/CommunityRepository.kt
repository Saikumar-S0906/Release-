package com.pixijoy.ai.data

import com.pixijoy.ai.core.network.PixiJoyBackendApi
import com.pixijoy.ai.domain.model.PhotoTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class CommunityPost(
    val id: String,
    val author: String,
    val title: String,
    val likes: Int,
    val tool: PhotoTool
)

@Singleton
class CommunityRepository @Inject constructor(
    private val backendApi: PixiJoyBackendApi,
    private val authRepository: AuthRepository
) {
    suspend fun feed(): List<CommunityPost> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authRepository.currentToken().orEmpty()
            backendApi.fetchCommunityFeed(authToken = token).posts.map { dto ->
                CommunityPost(
                    id = dto.id,
                    author = dto.author,
                    title = dto.title,
                    likes = dto.likes,
                    tool = PhotoTool.fromId(dto.toolId) ?: PhotoTool.CREATE_FOR_ME
                )
            }
        }.getOrElse { emptyList() }
    }
}
