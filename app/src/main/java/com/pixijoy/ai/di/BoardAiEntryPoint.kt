package com.pixijoy.ai.di

import com.pixijoy.ai.core.ai.AiOrchestrator
import com.pixijoy.ai.core.ai.CloudAiService
import com.pixijoy.ai.ml.CreateExtraPipeline
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BoardAiEntryPoint {
    fun cloudAiService(): CloudAiService
    fun aiOrchestrator(): AiOrchestrator
    fun createExtraPipeline(): CreateExtraPipeline
    fun whatsAppStickerExporter(): com.pixijoy.ai.sticker.WhatsAppStickerExporter
    fun talkingPhotoAnimator(): com.pixijoy.ai.creative.TalkingPhotoAnimator
    fun gifEncoder(): com.pixijoy.ai.creative.GifEncoder
    fun reelExporter(): com.pixijoy.ai.creative.ReelExporter
    fun videoToGifExtractor(): com.pixijoy.ai.creative.VideoToGifExtractor
    fun advancedEditEngine(): com.pixijoy.ai.creative.AdvancedEditEngine
    fun creativeAgentOrchestrator(): com.pixijoy.ai.core.agent.CreativeAgentOrchestrator
    fun imageVariationGenerator(): com.pixijoy.ai.creative.ImageVariationGenerator
    fun avatarStyleProcessor(): com.pixijoy.ai.creative.AvatarStyleProcessor
}
