package com.pixijoy.ai.creative

import com.pixijoy.ai.domain.model.AgentIntent
import com.pixijoy.ai.domain.model.CaptionTone
import com.pixijoy.ai.domain.model.MusicMood
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentIntentParser @Inject constructor() {

    fun parse(prompt: String): AgentIntent {
        val text = prompt.lowercase()
        val occasion = when {
            text.contains("birthday") || text.contains("bday") || text.contains("పుట్టినరోజు") -> "birthday"
            text.contains("wedding") || text.contains("marriage") -> "wedding"
            text.contains("festival") || text.contains("diwali") || text.contains("sankranti") -> "festival"
            text.contains("anniversary") -> "anniversary"
            else -> "general"
        }
        val recipient = when {
            text.contains("daughter") || text.contains("girl") || text.contains("baby girl") -> "daughter"
            text.contains("son") || text.contains("boy") -> "son"
            text.contains("mom") || text.contains("mother") || text.contains("amma") -> "mom"
            text.contains("dad") || text.contains("father") || text.contains("anna") -> "dad"
            text.contains("friend") -> "friend"
            text.contains("wife") -> "wife"
            text.contains("husband") -> "husband"
            else -> null
        }
        val tone = when {
            text.contains("funny") || text.contains("meme") -> "funny"
            text.contains("emotional") || text.contains("love") -> "emotional"
            text.contains("cute") -> "cute"
            else -> "celebration"
        }
        val language = when {
            text.contains("telugu") || text.contains("తెలుగు") -> CaptionTone.TELUGU
            text.contains("hindi") || text.contains("हिंदी") -> CaptionTone.HINDI
            else -> CaptionTone.TELUGU
        }
        val themePrompt = when (occasion) {
            "birthday" -> "pink birthday balloons confetti celebration"
            "wedding" -> "elegant floral wedding gold"
            "festival" -> "festive lights diwali colors"
            "anniversary" -> "romantic hearts rose petals"
            else -> "vibrant social media gradient"
        }
        val musicMood = when (occasion) {
            "birthday" -> MusicMood.BIRTHDAY
            "wedding", "anniversary" -> MusicMood.ROMANTIC
            "festival" -> MusicMood.EPIC
            else -> MusicMood.CHILL
        }
        return AgentIntent(
            occasion = occasion,
            recipient = recipient,
            platform = if (text.contains("whatsapp") || text.contains("wa")) "whatsapp" else "generic",
            tone = tone,
            language = language,
            themePrompt = themePrompt,
            musicMood = musicMood
        )
    }
}

@Singleton
class AgentCaptionGenerator @Inject constructor() {

    fun generate(intent: AgentIntent): com.pixijoy.ai.domain.model.AgentCaption {
        return when (intent.occasion to intent.recipient) {
            "birthday" to "daughter" -> com.pixijoy.ai.domain.model.AgentCaption(
                primary = "అమ్మాయి పుట్టినరోజు శుభాకాంక్షలు! 🎂",
                secondary = "Happy Birthday my princess!",
                spoken = "Happy birthday my darling daughter! Wishing you endless joy."
            )
            "birthday" to "son" -> com.pixijoy.ai.domain.model.AgentCaption(
                primary = "పుట్టినరోజు శుభాకాంక్షలు చిన్నోడా! 🎉",
                secondary = "Happy Birthday champ!",
                spoken = "Happy birthday my son! Keep shining bright."
            )
            "birthday" to "mom" -> com.pixijoy.ai.domain.model.AgentCaption(
                primary = "అమ్మ పుట్టినరోజు శుభాకాంక్షలు! 💐",
                secondary = "Happy Birthday Mom!",
                spoken = "Happy birthday mom! Thank you for everything."
            )
            "birthday" to null -> com.pixijoy.ai.domain.model.AgentCaption(
                primary = "పుట్టినరోజు శుభాకాంక్షలు! 🥳",
                secondary = "Happy Birthday!",
                spoken = "Happy birthday! Wishing you a wonderful day."
            )
            "festival" to null -> com.pixijoy.ai.domain.model.AgentCaption(
                primary = "శుభాకాంక్షలు! 🪔",
                secondary = "Happy Festival!",
                spoken = "Wishing you and your family a joyful festival."
            )
            else -> when (intent.language) {
                CaptionTone.TELUGU -> com.pixijoy.ai.domain.model.AgentCaption(
                    primary = "ఈ క్షణం ప్రత్యేకం ✨",
                    secondary = "Made with PixiJoy AI",
                    spoken = "This moment is special. Created with PixiJoy AI."
                )
                CaptionTone.HINDI -> com.pixijoy.ai.domain.model.AgentCaption(
                    primary = "यह पल खास है ✨",
                    secondary = "Made with PixiJoy AI",
                    spoken = "This moment is special. Created with PixiJoy AI."
                )
                else -> com.pixijoy.ai.domain.model.AgentCaption(
                    primary = "THIS MOMENT",
                    secondary = "Made with PixiJoy AI",
                    spoken = "This moment is special. Created with PixiJoy AI."
                )
            }
        }
    }
}
