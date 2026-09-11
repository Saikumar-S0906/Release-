package com.pixijoy.ai.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import com.pixijoy.ai.util.BitmapUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundStudioProcessor @Inject constructor(
    private val backgroundRemover: BackgroundRemover
) {
    suspend fun replaceBackground(bitmap: Bitmap, prompt: String): Bitmap {
        val cutout = backgroundRemover.removeBackground(bitmap)
        val width = bitmap.width
        val height = bitmap.height
        val background = createBackground(width, height, prompt)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(background, 0f, 0f, null)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(cutout, 0f, 0f, paint)
        if (cutout != bitmap) cutout.recycle()
        background.recycle()
        return output
    }

    private fun createBackground(width: Int, height: Int, prompt: String): Bitmap {
        val p = prompt.lowercase()
        val colors = when {
            p == "studio" || "office" in p || "professional" in p || "linkedin" in p ->
                intArrayOf(0xFFE8E8E8.toInt(), 0xFFB0B0B0.toInt(), 0xFF808080.toInt())
            p == "beach" || "beach" in p || "tropical" in p || "ocean" in p ->
                intArrayOf(0xFF00B4D8.toInt(), 0xFF48CAE4.toInt(), 0xFFFFE66D.toInt())
            p == "party" || "diwali" in p || "festival" in p ->
                intArrayOf(0xFFFF6B35.toInt(), 0xFFFFD700.toInt(), 0xFFFF1493.toInt())
            p == "nature" || "forest" in p || "garden" in p ->
                intArrayOf(0xFF2D6A4F.toInt(), 0xFF52B788.toInt(), 0xFF95D5B2.toInt())
            p == "city" || "dubai" in p || "luxury" in p ->
                intArrayOf(0xFF1A1A2E.toInt(), 0xFFFFD700.toInt(), 0xFFFF6B35.toInt())
            "mars" in p || "space" in p || "cyberpunk" in p ->
                intArrayOf(0xFF0D0221.toInt(), 0xFF7B2CBF.toInt(), 0xFFFF006E.toInt())
            "poster" in p || "cinematic" in p || "movie" in p ->
                intArrayOf(0xFF0F0C29.toInt(), 0xFF302B63.toInt(), 0xFF24243E.toInt())
            else ->
                intArrayOf(0xFF667EEA.toInt(), 0xFF764BA2.toInt(), 0xFFF093FB.toInt())
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), colors, null, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }
}
