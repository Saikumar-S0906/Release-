package com.pixijoy.ai.creative

import android.content.Context
import android.graphics.Bitmap
import com.pixijoy.ai.di.BoardAiEntryPoint
import com.pixijoy.ai.domain.model.AvatarStyle
import com.pixijoy.ai.domain.model.FestivalTemplates
import com.pixijoy.ai.ui.board.BoardMediaSession
import dagger.hilt.android.EntryPointAccessors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CompeteLook(
    val id: String,
    val label: String,
    val bitmap: Bitmap
)

data class CompetePackResult(
    val original: Bitmap,
    val beauty: Bitmap,
    val cutout: Bitmap,
    val looks: List<CompeteLook>,
    val stickers: List<Bitmap>,
    val festivalName: String,
    val festivalDate: String,
    val gifBytes: ByteArray?
)

object CompetePack {
    fun festivalToday(): Pair<String, String> {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val tpl = when {
            month == 12 && day >= 10 -> FestivalTemplates.all.first { it.id == "christmas" }
            month == 3 -> FestivalTemplates.all.first { it.id == "holi" }
            month == 4 -> FestivalTemplates.all.first { it.id == "ugadi" }
            month == 1 && day <= 20 -> FestivalTemplates.all.first { it.id == "sankranti" }
            month == 10 || month == 11 -> FestivalTemplates.all.first { it.id == "diwali" }
            month == 9 -> FestivalTemplates.all.first { it.id == "onam" }
            else -> FestivalTemplates.all.first { it.id == "birthday" }
        }
        val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date())
        return tpl.name to "$date · ${tpl.topText}"
    }

    suspend fun generate(context: Context): CompetePackResult? {
        val app = context.applicationContext
        val entry = runCatching {
            EntryPointAccessors.fromApplication(app, BoardAiEntryPoint::class.java)
        }.getOrNull() ?: return null
        val pipe = entry.createExtraPipeline()
        val src = BoardMediaSession.decodeBitmap(app, maxSide = 1280) ?: return null
        val (festName, festLine) = festivalToday()
        val beauty = runCatching { pipe.enhanceFaceLook(src) }.getOrDefault(src)
        val cutout = runCatching { pipe.cutoutTransparent(src) }.getOrDefault(src)
        val studio = runCatching { pipe.applyLook(cutout, 2) }.getOrDefault(cutout)
        val sticker = runCatching { pipe.generateStickerPack(src) }.getOrNull()
        val stickers = sticker?.stickers.orEmpty().take(8)
        val story = runCatching { pipe.exportForPlatform(beauty, 9f, 16f, 1080) }.getOrDefault(beauty)
        val square = runCatching { pipe.exportForPlatform(beauty, 1f, 1f, 1080) }.getOrDefault(beauty)
        val meme = runCatching {
            pipe.bakeMemeCaptions(src, festLine, festName.uppercase(Locale.getDefault()))
        }.getOrDefault(src)
        val avatar = runCatching {
            entry.avatarStyleProcessor().apply(src, AvatarStyle.CARTOON)
        }.getOrDefault(src)
        val print = runCatching { pipe.exportPrintPage(beauty, 1200, 1800) }.getOrDefault(beauty)
        val festival = runCatching {
            pipe.bakeMemeCaptions(beauty, festName.uppercase(Locale.getDefault()), festLine)
        }.getOrDefault(beauty)
        val frames = runCatching {
            entry.talkingPhotoAnimator().animate(beauty, frames = 8, startScale = 1.04f, endScale = 1.16f)
        }.getOrDefault(emptyList())
        val gif = runCatching {
            if (frames.isNotEmpty()) entry.gifEncoder().encode(frames, frameDelayMs = 120) else null
        }.getOrNull()
        return CompetePackResult(
            original = src,
            beauty = beauty,
            cutout = studio,
            looks = listOf(
                CompeteLook("sticker", "Sticker", stickers.firstOrNull() ?: cutout),
                CompeteLook("story", "Story", story),
                CompeteLook("square", "Post", square),
                CompeteLook("meme", "Meme", meme),
                CompeteLook("avatar", "Avatar", avatar),
                CompeteLook("print", "Print", print),
                CompeteLook("festival", festName, festival)
            ),
            stickers = stickers,
            festivalName = festName,
            festivalDate = festLine,
            gifBytes = gif
        )
    }
}
