package com.pixijoy.ai.creative

import android.content.Context
import android.graphics.Bitmap
import com.pixijoy.ai.domain.model.MemeTextStyle
import com.pixijoy.ai.domain.model.SocialFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ViralPack(
    val meme: Bitmap,
    val storyCard: Bitmap,
    val reactionCard: Bitmap,
    val profilePic: Bitmap
)

@Singleton
class ViralPackExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memeComposer: MemeComposer,
    private val profilePicComposer: ProfilePicComposer
) {
    fun createPack(
        original: Bitmap,
        enhanced: Bitmap,
        memeStyle: MemeTextStyle,
        storyHeadline: String,
        storySubline: String,
        reactionCaption: String
    ): ViralPack {
        val meme = memeComposer.compose(
            base = enhanced,
            style = memeStyle,
            format = SocialFormat.INSTAGRAM_POST
        )
        val story = memeComposer.createStoryCard(context, enhanced, storyHeadline, storySubline)
        val reaction = memeComposer.createReactionCard(context, original, enhanced, reactionCaption)
        val profile = profilePicComposer.create(enhanced)
        return ViralPack(meme = meme, storyCard = story, reactionCard = reaction, profilePic = profile)
    }
}
