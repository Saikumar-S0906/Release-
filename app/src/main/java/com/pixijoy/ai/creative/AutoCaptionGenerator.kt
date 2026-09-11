package com.pixijoy.ai.creative

import com.pixijoy.ai.domain.model.DailyChallenge
import com.pixijoy.ai.domain.model.MemeTemplate
import com.pixijoy.ai.domain.model.MemeTemplates
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class CaptionSuggestion(
    val topText: String,
    val bottomText: String,
    val emoji: String
)

@Singleton
class AutoCaptionGenerator @Inject constructor() {

    private val captionPool = listOf(
        CaptionSuggestion("WHEN YOU SEE", "THE FINAL RESULT", "😱"),
        CaptionSuggestion("POV:", "YOU FOUND THE BEST EDITING APP", "👀"),
        CaptionSuggestion("NOBODY:", "ME DOWNLOADING PIXIJOY", "📲"),
        CaptionSuggestion("BEFORE AI", "AFTER AI", "✨"),
        CaptionSuggestion("MY FRIENDS:", "HOW DID YOU EDIT THIS?!", "🤯"),
        CaptionSuggestion("EXPECTATION", "REALITY (BUT BETTER)", "💯"),
        CaptionSuggestion("THAT ONE FRIEND", "WHO EDITS IN 1 TAP", "😎"),
        CaptionSuggestion("EXCUSE ME WHAT", "HOW IS THIS FREE", "🔥"),
        CaptionSuggestion("ME EXPLAINING", "I DIDN'T USE PHOTOSHOP", "🗿"),
        CaptionSuggestion("PLOT TWIST:", "THE APP DID EVERYTHING", "🎬"),
        CaptionSuggestion("जब तुम्हें पता चले", "AI सब कुछ कर देता है", "🇮🇳"),
        CaptionSuggestion("EXAM TOMORROW", "STILL MAKING MEMES", "📚"),
        CaptionSuggestion("CRICKET MATCH ON", "MEME MAKING MODE ON", "🏏"),
        CaptionSuggestion("MONDAY MOOD", "AFTER PIXIJOY EDIT", "☕")
    )

    fun suggestCaptions(count: Int = 5): List<CaptionSuggestion> = captionPool.shuffled().take(count)

    fun suggestForTemplate(template: MemeTemplate): CaptionSuggestion =
        CaptionSuggestion(template.topText, template.bottomText, template.emoji)

    fun randomCaption(): CaptionSuggestion = captionPool.random()
}

@Singleton
class DailyChallengeProvider @Inject constructor() {

    private val challenges = listOf(
        DailyChallenge(
            title = "Nobody Meme Monday",
            description = "Create a Nobody: / Me: meme and share it",
            template = MemeTemplates.trending.first { it.id == "nobody" },
            hashtag = "#NobodyMeme"
        ),
        DailyChallenge(
            title = "POV Challenge",
            description = "Make a POV meme with your best photo",
            template = MemeTemplates.trending.first { it.id == "pov" },
            hashtag = "#POVChallenge"
        ),
        DailyChallenge(
            title = "Before vs After",
            description = "Show your AI glow-up transformation",
            template = MemeTemplates.trending.first { it.id == "reaction" },
            hashtag = "#GlowUp"
        ),
        DailyChallenge(
            title = "Galaxy Brain",
            description = "Compare old way vs PixiJoy way",
            template = MemeTemplates.trending.first { it.id == "brain" },
            hashtag = "#GalaxyBrain"
        ),
        DailyChallenge(
            title = "Viral Story Card",
            description = "Create a 9:16 story and share to Instagram",
            template = MemeTemplates.india.first(),
            hashtag = "#PixiJoyStory"
        ),
        DailyChallenge(
            title = "Exam Season Mood",
            description = "Relatable exam meme for your friends",
            template = MemeTemplates.india.first { it.id == "exam" },
            hashtag = "#ExamMemes"
        ),
        DailyChallenge(
            title = "Friday Flex",
            description = "Flex your best AI-edited photo",
            template = MemeTemplates.trending.first { it.id == "success" },
            hashtag = "#FridayFlex"
        )
    )

    fun todayChallenge(): DailyChallenge {
        val dayIndex = LocalDate.now(ZoneId.systemDefault()).dayOfYear
        return challenges[dayIndex % challenges.size]
    }
}
