package com.pixijoy.ai.ui.board

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.DrawableRes
import com.pixijoy.ai.di.BoardAiEntryPoint
import com.pixijoy.ai.domain.model.CaptionTone
import com.pixijoy.ai.ui.board.components.BoardColorAdjust
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Board Beautiful / Funny generate.
 *
 * Always starts from the user photo in [BoardMediaSession] and returns a
 * processed bitmap. Gemini (when configured) supplies captions / scene copy;
 * pixels are enhanced on-device so the result is the user's photo, not stock art.
 */
object BoardSoftGenerate {
    private const val TAG = "BoardSoftGenerate"

    enum class Intent { Beautiful, Funny }

    data class Result(
        @DrawableRes val artRes: Int,
        val usedLiveAttempt: Boolean,
        val softFallback: Boolean,
        val isCloudPowered: Boolean,
        val sceneDescription: String? = null,
        val topText: String? = null,
        val bottomText: String? = null,
        val userMessage: String? = null,
        val bitmap: Bitmap? = null
    )

    suspend fun run(context: Context, intent: Intent): Result = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val fallbackArt = when (intent) {
            Intent.Beautiful -> BoardFlowArt.baAfter
            Intent.Funny -> BoardFlowArt.funnyHero
        }
        val source = BoardMediaSession.decodeBitmap(app, maxSide = 1600)
            ?: BitmapFactory.decodeResource(
                app.resources,
                when (intent) {
                    Intent.Beautiful -> BoardFlowArt.beautifulHero
                    Intent.Funny -> BoardFlowArt.funnyHero
                }
            )

        if (source == null) {
            return@withContext Result(
                artRes = fallbackArt,
                usedLiveAttempt = false,
                softFallback = true,
                isCloudPowered = false,
                userMessage = "No photo available"
            )
        }

        val pipe = runCatching {
            EntryPointAccessors.fromApplication(app, BoardAiEntryPoint::class.java)
                .createExtraPipeline()
        }.getOrNull()

        var top: String? = null
        var bottom: String? = null
        var scene: String? = null
        var cloud = false
        var msg: String? = null
        var liveAttempt = false

        if (BoardFeatureFlags.liveGenerateConfigured) {
            liveAttempt = true
            runCatching {
                val orch = EntryPointAccessors
                    .fromApplication(app, BoardAiEntryPoint::class.java)
                    .aiOrchestrator()
                val analysis = orch.analyzePhoto(source)
                scene = analysis.sceneDescription
                    ?: analysis.suggestedCaption.first.takeIf { it.isNotBlank() }
                top = analysis.suggestedCaption.first.takeIf { it.isNotBlank() }
                bottom = analysis.suggestedCaption.second.takeIf { it.isNotBlank() }
                cloud = analysis.isCloudPowered
                msg = analysis.userMessage
                if (intent == Intent.Funny && top.isNullOrBlank() && bottom.isNullOrBlank()) {
                    val caption = orch.generateCaption(
                        source,
                        analysis.context,
                        CaptionTone.SAVAGE,
                        analysis.sceneDescription ?: "funny photo meme"
                    )
                    top = caption.topText.takeIf { it.isNotBlank() }
                    bottom = caption.bottomText.takeIf { it.isNotBlank() }
                    cloud = caption.isCloudPowered
                    msg = caption.userMessage ?: msg
                }
            }.onFailure { t ->
                Log.w(TAG, "Live generate failed; continuing with on-device look", t)
                msg = t.message
            }
        }

        val processed = try {
            when (intent) {
                Intent.Beautiful -> {
                    var out = if (pipe != null) pipe.enhanceFaceLook(source) else source
                    out = pipe?.applyColorMatrix(
                        out,
                        BoardColorAdjust.androidMatrix(
                            boostLighting = true,
                            boostColor = true
                        )
                    ) ?: out
                    out
                }
                Intent.Funny -> {
                    val capTop = top ?: "WAIT FOR IT"
                    val capBottom = bottom ?: "THIS IS MY PHOTO NOW"
                    pipe?.bakeMemeCaptions(source, capTop, capBottom) ?: source
                }
            }
        } catch (_: Throwable) {
            source
        }

        if (processed !== source && !source.isRecycled) {
            runCatching { source.recycle() }
        }

        Result(
            artRes = fallbackArt,
            usedLiveAttempt = liveAttempt,
            softFallback = !cloud,
            isCloudPowered = cloud,
            sceneDescription = scene,
            topText = top,
            bottomText = bottom,
            userMessage = msg,
            bitmap = processed
        )
    }
}
