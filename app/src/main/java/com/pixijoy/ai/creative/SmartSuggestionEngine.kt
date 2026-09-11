package com.pixijoy.ai.creative

import com.pixijoy.ai.core.ai.NlKeywordMatcher
import com.pixijoy.ai.domain.model.CaptionTone
import com.pixijoy.ai.domain.model.CreationType
import com.pixijoy.ai.domain.model.ImageContext
import com.pixijoy.ai.domain.model.NlAction
import com.pixijoy.ai.domain.model.NlEditResult
import com.pixijoy.ai.domain.model.SmartSuggestion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartSuggestionEngine @Inject constructor() {

    fun suggest(context: ImageContext): List<SmartSuggestion> {
        val suggestions = mutableListOf<SmartSuggestion>()

        when {
            context.isBirthday -> {
                suggestions += SmartSuggestion(CreationType.MEME, "Birthday detected — make a celebration meme", 1)
                suggestions += SmartSuggestion(CreationType.STORY, "Perfect for birthday WhatsApp status", 2)
                suggestions += SmartSuggestion(CreationType.STICKER, "Create birthday sticker pack", 3)
                suggestions += SmartSuggestion(CreationType.VIRAL_PACK, "Share the celebration everywhere", 4)
            }
            context.isSelfie -> {
                suggestions += SmartSuggestion(CreationType.ENHANCE, "Selfie detected — enhance your look", 1)
                suggestions += SmartSuggestion(CreationType.PROFILE, "Make a pro profile picture", 2)
                suggestions += SmartSuggestion(CreationType.STICKER, "Turn selfie into sticker pack", 3)
                suggestions += SmartSuggestion(CreationType.REMOVE_BG, "Remove background for clean cutout", 4)
            }
            context.isGroup -> {
                suggestions += SmartSuggestion(CreationType.MEME, "Group photo — perfect for memes", 1)
                suggestions += SmartSuggestion(CreationType.VIRAL_PACK, "Share with the whole group", 2)
                suggestions += SmartSuggestion(CreationType.STORY, "Post to Instagram Story", 3)
            }
            context.isFood -> {
                suggestions += SmartSuggestion(CreationType.ENHANCE, "Make food look delicious", 1)
                suggestions += SmartSuggestion(CreationType.STORY, "Share on Instagram Story", 2)
                suggestions += SmartSuggestion(CreationType.MEME, "Food meme time", 3)
            }
            context.isPet -> {
                suggestions += SmartSuggestion(CreationType.MEME, "Pet meme potential detected", 1)
                suggestions += SmartSuggestion(CreationType.STICKER, "Pet sticker pack", 2)
                suggestions += SmartSuggestion(CreationType.GIF, "Animated pet GIF", 3)
            }
            context.isOutdoor -> {
                suggestions += SmartSuggestion(CreationType.ENHANCE, "Enhance travel photo", 1)
                suggestions += SmartSuggestion(CreationType.REMOVE_BG, "Replace with dream destination", 2)
                suggestions += SmartSuggestion(CreationType.STORY, "Travel story card", 3)
            }
            else -> {
                suggestions += SmartSuggestion(CreationType.ENHANCE, "One-tap AI enhance", 1)
                suggestions += SmartSuggestion(CreationType.MEME, "Create a viral meme", 2)
                suggestions += SmartSuggestion(CreationType.VIRAL_PACK, "Export all formats at once", 3)
            }
        }

        suggestions += SmartSuggestion(CreationType.EDIT, "Crop, rotate & apply filters", 10)
        suggestions += SmartSuggestion(CreationType.QUOTE, "Motivational quote card", 11)
        suggestions += SmartSuggestion(CreationType.WA_STICKER, "WhatsApp sticker", 12)

        return suggestions.sortedBy { it.priority }.distinctBy { it.creationType }
    }
}

@Singleton
class NaturalLanguageEditor @Inject constructor() {

    fun parse(command: String): NlEditResult {
        val action = NlKeywordMatcher.matchAction(command)
        return when (action) {
            NlAction.REMOVE_BACKGROUND -> NlEditResult(NlAction.REMOVE_BACKGROUND, "Removing background...")
            NlAction.MAKE_MEME -> NlEditResult(NlAction.MAKE_MEME, "Creating meme...")
            NlAction.MAKE_STICKER -> NlEditResult(NlAction.MAKE_STICKER, "Building sticker pack...")
            NlAction.ENHANCE -> NlEditResult(NlAction.ENHANCE, "Enhancing photo...")
            NlAction.PORTRAIT_BLUR -> NlEditResult(NlAction.PORTRAIT_BLUR, "Applying portrait blur...")
            NlAction.OBJECT_REMOVE -> NlEditResult(NlAction.OBJECT_REMOVE, "Erasing selected area...")
            NlAction.CREATE_REEL -> NlEditResult(NlAction.CREATE_REEL, "Creating reel story...")
            NlAction.BRIGHTEN -> NlEditResult(NlAction.BRIGHTEN, "Brightening...")
            NlAction.DARKEN -> NlEditResult(NlAction.DARKEN, "Applying cinematic look...")
            NlAction.PORTRAIT_STUDIO -> NlEditResult(NlAction.PORTRAIT_STUDIO, "Opening portrait studio...")
            NlAction.GENERATE_VARIATIONS -> NlEditResult(NlAction.GENERATE_VARIATIONS, "Generating AI variations...")
            NlAction.ADVANCED_EDIT -> NlEditResult(NlAction.ADVANCED_EDIT, "Opening advanced editor...")
            null -> NlEditResult(NlAction.UNKNOWN, "Try: remove background, make meme, enhance photo")
            else -> NlEditResult(action, "Processing...")
        }
    }
}

@Singleton
class ToneCaptionGenerator @Inject constructor() {

    fun generate(context: ImageContext, tone: CaptionTone): Pair<String, String> {
        val subject = when {
            context.isBirthday -> "THE BIRTHDAY"
            context.isFood -> "THE FOOD"
            context.isPet -> "MY PET"
            context.isSelfie -> "MY FACE"
            context.isGroup -> "THE SQUAD"
            context.isOutdoor -> "THE VIEW"
            else -> "THIS PHOTO"
        }

        return when (tone) {
            CaptionTone.HINDI -> "JAB $subject" to "TAB MAIN PIXIJOY USE KARTA HU"
            CaptionTone.TELUGU -> "IDI $subject" to "PIXIJOY THO EDIT CHEYALI"
            CaptionTone.GENZ -> "NO BECAUSE WHY IS" to "$subject ACTUALLY FIRE THO"
            CaptionTone.SAVAGE -> "THEY SAID EDIT IT" to "I SAID WATCH THIS"
            CaptionTone.OFFICE -> "BOSS: FIX THIS" to "ME: *OPENS PIXIJOY*"
            CaptionTone.DAD_JOKE -> "I TOLD MY WIFE" to "SHE SAID I'M GLOWING"
            CaptionTone.ENGLISH -> "WHEN YOU SEE" to "$subject HIT DIFFERENT"
        }
    }
}
