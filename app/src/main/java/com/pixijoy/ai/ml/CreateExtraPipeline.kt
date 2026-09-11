package com.pixijoy.ai.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.pixijoy.ai.creative.PhotoEditProcessor
import com.pixijoy.ai.domain.model.PhotoFilter
import com.pixijoy.ai.util.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot
import kotlin.math.max
import com.pixijoy.ai.ui.board.components.BoardColorAdjust

/**
 * Injectable C-tool pipelines (ML Kit + existing processors).
 * All public methods are safe to call from [Dispatchers.Default].
 */
@Singleton
class CreateExtraPipeline @Inject constructor(
    private val imageEnhancer: ImageEnhancer,
    private val portraitStudio: PortraitStudioProcessor,
    private val backgroundRemover: BackgroundRemover,
    private val portraitBlur: PortraitBlurProcessor,
    private val objectEraser: ObjectEraser,
    private val stickerPackGenerator: com.pixijoy.ai.creative.StickerPackGenerator,
    private val photoEditProcessor: PhotoEditProcessor
) {
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.12f)
            .build()
    )

    /** Returns true if at least one face is detected (for FaceSwap / People gate). */
    suspend fun hasFace(bitmap: Bitmap): Boolean = withContext(Dispatchers.Default) {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return@withContext false
        runCatching {
            faceDetector.process(InputImage.fromBitmap(bitmap, 0)).await().isNotEmpty()
        }.getOrDefault(false)
    }

    /** Heal / clone-style inpaint at normalized image coords (0..1). */
    suspend fun healAt(
        bitmap: Bitmap,
        nx: Float,
        ny: Float,
        radiusFrac: Float = 0.07f
    ): Bitmap = withContext(Dispatchers.Default) {
        if (!isUsable(bitmap)) return@withContext bitmap
        val x = nx.coerceIn(0f, 1f) * bitmap.width
        val y = ny.coerceIn(0f, 1f) * bitmap.height
        val radius = (radiusFrac.coerceIn(0.02f, 0.25f) * minOf(bitmap.width, bitmap.height))
        runCatching { objectEraser.eraseAtPoint(bitmap, x, y, radius) }.getOrDefault(bitmap)
    }

    /** Selective dodge/burn at normalized coords; [amount] -1..1. */
    suspend fun selectiveAt(
        bitmap: Bitmap,
        nx: Float,
        ny: Float,
        radiusFrac: Float = 0.08f,
        amount: Float = 0.35f
    ): Bitmap = withContext(Dispatchers.Default) {
        if (!isUsable(bitmap)) return@withContext bitmap
        val x = nx.coerceIn(0f, 1f) * bitmap.width
        val y = ny.coerceIn(0f, 1f) * bitmap.height
        val radius = (radiusFrac.coerceIn(0.02f, 0.3f) * minOf(bitmap.width, bitmap.height))
        runCatching {
            objectEraser.selectiveAtPoint(bitmap, x, y, radius, amount.coerceIn(-1f, 1f))
        }.getOrDefault(bitmap)
    }

    /** Emotion sticker pack from a source photo (transparent cutout + badges). */
    suspend fun generateStickerPack(bitmap: Bitmap): com.pixijoy.ai.creative.StickerPackResult =
        withContext(Dispatchers.Default) {
            if (!isUsable(bitmap)) {
                return@withContext com.pixijoy.ai.creative.StickerPackResult(emptyList(), emptyList())
            }
            runCatching { stickerPackGenerator.generatePack(bitmap) }
                .getOrElse { com.pixijoy.ai.creative.StickerPackResult(emptyList(), emptyList()) }
        }

    /**
     * Honest on-device "face swap" for C06: crop detected face (or center square) onto [backdrop].
     * Not generative identity swap — labeled as local remix in UI.
     */
    suspend fun faceMemeComposite(user: Bitmap, backdrop: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        if (!isUsable(user) || !isUsable(backdrop)) return@withContext backdrop
        runCatching {
            val faces = faceDetector.process(InputImage.fromBitmap(user, 0)).await()
            val box = faces.firstOrNull()?.boundingBox
            val pad = if (box != null) (box.width() * 0.15f).toInt() else 0
            val face = if (box != null) {
                val l = (box.left - pad).coerceAtLeast(0)
                val t = (box.top - pad).coerceAtLeast(0)
                val r = (box.right + pad).coerceAtMost(user.width)
                val b = (box.bottom + pad).coerceAtMost(user.height)
                Bitmap.createBitmap(user, l, t, (r - l).coerceAtLeast(1), (b - t).coerceAtLeast(1))
            } else {
                val side = minOf(user.width, user.height)
                val l = (user.width - side) / 2
                val t = (user.height - side) / 2
                Bitmap.createBitmap(user, l, t, side, side)
            }
            val outW = backdrop.width.coerceAtMost(MAX_OUT_SIDE)
            val outH = backdrop.height.coerceAtMost(MAX_OUT_SIDE)
            val bg = if (backdrop.width == outW && backdrop.height == outH) {
                BitmapUtils.copy(backdrop)
            } else {
                Bitmap.createScaledBitmap(backdrop, outW, outH, true)
            }
            val canvas = Canvas(bg)
            val diameter = (minOf(outW, outH) * 0.42f).toInt().coerceAtLeast(64)
            val scaled = Bitmap.createScaledBitmap(face, diameter, diameter, true)
            val circle = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
            val c = Canvas(circle)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            c.drawCircle(diameter / 2f, diameter / 2f, diameter / 2f, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            c.drawBitmap(scaled, 0f, 0f, paint)
            val cx = (outW - diameter) / 2f
            val cy = outH * 0.28f
            canvas.drawBitmap(circle, cx, cy, null)
            recycleTemp(face, scaled, circle)
            bg
        }.getOrElse { BitmapUtils.copy(backdrop) }
    }

    /**
     * Apply a look preset (0..7) via [PhotoEditProcessor].
     * 0 NONE · 1 VIVID · 2 WARM · 3 COOL · 4 BW · 5 VINTAGE · 6 CINEMATIC · 7 AI_GLOW (dream).
     */
    fun applyLook(bitmap: Bitmap, filterIndex: Int, category: Int = 0): Bitmap {
        if (!isUsable(bitmap)) return bitmap
        val filter = when (filterIndex.coerceIn(0, 7)) {
            1 -> PhotoFilter.VIVID
            2 -> PhotoFilter.WARM
            3 -> PhotoFilter.COOL
            4 -> PhotoFilter.BW
            5 -> PhotoFilter.VINTAGE
            6 -> PhotoFilter.CINEMATIC
            7 -> PhotoFilter.AI_GLOW
            else -> PhotoFilter.NONE
        }
        val filtered = runCatching { photoEditProcessor.applyFilter(bitmap, filter) }.getOrDefault(bitmap)
        if (category <= 0) return filtered
        return applyColorMatrix(filtered, BoardColorAdjust.lookAndroidMatrix(0, category))
    }

    fun applyEraLook(bitmap: Bitmap, era: Int): Bitmap {
        if (!isUsable(bitmap)) return bitmap
        return applyColorMatrix(bitmap, BoardColorAdjust.eraAndroidMatrix(era))
    }

    fun applyColorMatrix(bitmap: Bitmap, matrix: android.graphics.ColorMatrix): Bitmap {
        if (!isUsable(bitmap)) return bitmap
        val output = BitmapUtils.copy(bitmap)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    fun cropNormalized(
        bitmap: Bitmap,
        left: Float,
        top: Float,
        width: Float,
        height: Float
    ): Bitmap {
        if (!isUsable(bitmap)) return bitmap
        val l = (left.coerceIn(0f, 1f) * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val t = (top.coerceIn(0f, 1f) * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val w = (width.coerceIn(0.05f, 1f) * bitmap.width).toInt().coerceIn(1, bitmap.width - l)
        val h = (height.coerceIn(0.05f, 1f) * bitmap.height).toInt().coerceIn(1, bitmap.height - t)
        return runCatching {
            photoEditProcessor.cropFree(bitmap, android.graphics.Rect(l, t, l + w, t + h))
        }.getOrDefault(bitmap)
    }

    /** Soft black contact ellipse under subject, then cutout on top. */
    fun withContactShadow(cutout: Bitmap): Bitmap {
        if (!isUsable(cutout)) return cutout
        val output = Bitmap.createBitmap(cutout.width, cutout.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val cx = cutout.width / 2f
        val cy = cutout.height * 0.88f
        val rx = cutout.width * 0.32f
        val ry = cutout.height * 0.07f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 5 downTo 1) {
            val t = i / 5f
            paint.color = Color.argb((28 * t).toInt().coerceIn(8, 36), 0, 0, 0)
            val expand = 1f + (5 - i) * 0.08f
            canvas.drawOval(
                RectF(cx - rx * expand, cy - ry * expand, cx + rx * expand, cy + ry * expand),
                paint
            )
        }
        canvas.drawBitmap(cutout, 0f, 0f, null)
        return output
    }

    /** Bake top/bottom meme captions onto a photo. [scale] multiplies default text size. */
    fun bakeMemeCaptions(
        bitmap: Bitmap,
        top: String,
        bottom: String,
        colorArgb: Int = Color.WHITE,
        scale: Float = 1f
    ): Bitmap {
        if (!isUsable(bitmap)) return bitmap
        val out = BitmapUtils.copy(bitmap)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
            setShadowLayer(6f, 2f, 2f, Color.BLACK)
        }
        val w = out.width.toFloat()
        val sizeScale = scale.coerceIn(0.5f, 2.5f)
        paint.textSize = (out.height * 0.07f * sizeScale).coerceIn(22f, 96f)
        if (top.isNotBlank()) {
            canvas.drawText(top.uppercase(), w / 2f, paint.textSize + 12f, paint)
        }
        if (bottom.isNotBlank()) {
            canvas.drawText(bottom.uppercase(), w / 2f, out.height - 20f, paint)
        }
        return out
    }

    /**
     * Richer text-on-photo for Flow 15 / Canva-lite.
     * [align] 0=left · 1=center · 2=right; [fontStyle] 0=default · 1=sans · 2=serif · 3=mono.
     */
    fun bakeTextOverlay(
        bitmap: Bitmap,
        text: String,
        colorArgb: Int = Color.WHITE,
        align: Int = 1,
        fontStyle: Int = 0,
        scale: Float = 1f,
        verticalBias: Float = 0.5f
    ): Bitmap {
        if (!isUsable(bitmap) || text.isBlank()) return bitmap
        val out = BitmapUtils.copy(bitmap)
        val canvas = Canvas(out)
        val paintAlign = when (align) {
            0 -> Paint.Align.LEFT
            2 -> Paint.Align.RIGHT
            else -> Paint.Align.CENTER
        }
        val family = when (fontStyle) {
            1 -> android.graphics.Typeface.SANS_SERIF
            2 -> android.graphics.Typeface.SERIF
            3 -> android.graphics.Typeface.MONOSPACE
            else -> android.graphics.Typeface.DEFAULT
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            textAlign = paintAlign
            typeface = android.graphics.Typeface.create(family, android.graphics.Typeface.BOLD)
            setShadowLayer(8f, 2f, 2f, Color.BLACK)
            textSize = (out.height * 0.06f * scale.coerceIn(0.5f, 2.5f)).coerceIn(20f, 96f)
        }
        val x = when (align) {
            0 -> out.width * 0.06f
            2 -> out.width * 0.94f
            else -> out.width / 2f
        }
        val y = (out.height * verticalBias.coerceIn(0.12f, 0.9f)).coerceAtLeast(paint.textSize + 8f)
        canvas.drawText(text, x, y, paint)
        return out
    }

    /**
     * Lightroom/Snapseed-lite structure: sharpen + optional vignette + grain.
     * Sliders use Neutral=10 (BoardColorAdjust); pass absolute amounts 0..1 for vignette/grain.
     */
    fun applyStructure(
        bitmap: Bitmap,
        sharpness: Float = BoardColorAdjust.Neutral,
        vignette: Float = 0f,
        grain: Float = 0f
    ): Bitmap {
        if (!isUsable(bitmap)) return bitmap
        var out = bitmap
        val sharpAmt = ((sharpness - BoardColorAdjust.Neutral) / BoardColorAdjust.Neutral).coerceIn(-1f, 1f)
        if (kotlin.math.abs(sharpAmt) > 0.05f) {
            out = runCatching { imageEnhancer.sharpen(out, 0.35f + sharpAmt * 0.45f) }
                .getOrElse { out }
        }
        val vig = vignette.coerceIn(0f, 1f)
        if (vig > 0.02f) {
            val next = BitmapUtils.copy(out)
            val canvas = Canvas(next)
            val cx = next.width / 2f
            val cy = next.height / 2f
            val radius = hypot(cx.toDouble(), cy.toDouble()).toFloat()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.RadialGradient(
                    cx, cy, radius,
                    intArrayOf(Color.TRANSPARENT, Color.argb((140 * vig).toInt(), 0, 0, 0)),
                    floatArrayOf(0.45f, 1f),
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, next.width.toFloat(), next.height.toFloat(), paint)
            if (out !== bitmap && out !== next) recycleTemp(out)
            out = next
        }
        val g = grain.coerceIn(0f, 1f)
        if (g > 0.02f) {
            val next = BitmapUtils.copy(out)
            val rnd = java.util.Random(42)
            val step = 3
            val strength = (28 * g).toInt()
            for (y in 0 until next.height step step) {
                for (x in 0 until next.width step step) {
                    val n = rnd.nextInt(strength * 2 + 1) - strength
                    val c = next.getPixel(x, y)
                    val nr = (Color.red(c) + n).coerceIn(0, 255)
                    val ng = (Color.green(c) + n).coerceIn(0, 255)
                    val nb = (Color.blue(c) + n).coerceIn(0, 255)
                    next.setPixel(x, y, Color.argb(Color.alpha(c), nr, ng, nb))
                }
            }
            if (out !== bitmap && out !== next) recycleTemp(out)
            out = next
        }
        return out
    }

    /** Crop to platform aspect then downscale for export (F17). */
    fun exportForPlatform(
        bitmap: Bitmap,
        aspectW: Float,
        aspectH: Float,
        maxSide: Int = 1920
    ): Bitmap {
        if (!isUsable(bitmap)) return bitmap
        val cropped = runCatching {
            photoEditProcessor.crop(bitmap, aspectW, aspectH)
        }.getOrDefault(bitmap)
        val capped = maxSide.coerceIn(512, MAX_OUT_SIDE)
        return runCatching { photoEditProcessor.resize(cropped, capped) }.getOrElse { cropped }
    }

    /**
     * Letterbox onto a print page (white canvas) at exact pixels — 4×6 / A4 @ 300dpi.
     * Does not use [MAX_OUT_SIDE] so print dimensions stay honest.
     */
    fun exportPrintPage(bitmap: Bitmap, pageW: Int, pageH: Int): Bitmap {
        if (!isUsable(bitmap)) return bitmap
        val w = pageW.coerceIn(400, 3600)
        val h = pageH.coerceIn(400, 3600)
        return runCatching { letterboxOnWhite(bitmap, w, h) }.getOrElse {
            letterboxOnWhite(bitmap, (w / 2).coerceAtLeast(400), (h / 2).coerceAtLeast(400))
        }
    }

    private fun letterboxOnWhite(bitmap: Bitmap, pageW: Int, pageH: Int): Bitmap {
        val out = Bitmap.createBitmap(pageW, pageH, Bitmap.Config.ARGB_8888)
        out.eraseColor(Color.WHITE)
        val scale = minOf(pageW / bitmap.width.toFloat(), pageH / bitmap.height.toFloat())
        val dw = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val dh = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, dw, dh, true)
        val left = (pageW - dw) / 2f
        val top = (pageH - dh) / 2f
        Canvas(out).drawBitmap(scaled, left, top, null)
        if (scaled !== bitmap && !scaled.isRecycled) runCatching { scaled.recycle() }
        return out
    }

    /** Face enhance for Beautiful toggle — enhance + youthful portrait pass. */
    suspend fun enhanceFaceLook(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        if (!isUsable(bitmap)) return@withContext bitmap
        runCatching {
            val enhanced = imageEnhancer.enhance(bitmap)
            portraitStudio.apply(enhanced, PortraitEffect.YOUTHFUL, 0.75f).also { out ->
                recycleTemp(enhanced, bitmap, out)
            }
        }.getOrElse { runCatching { imageEnhancer.enhance(bitmap) }.getOrDefault(bitmap) }
    }

    suspend fun restoreChain(bitmap: Bitmap, optionIndex: Int): Bitmap = withContext(Dispatchers.Default) {
        if (!isUsable(bitmap)) return@withContext bitmap
        runCatching {
            val lifted = CreateExtraAlgorithms.applyRestoreColorMatrix(bitmap)
            val out = when (optionIndex) {
                1 -> imageEnhancer.enhance(lifted)
                2 -> {
                    val enhanced = imageEnhancer.enhance(lifted)
                    portraitStudio.apply(enhanced, PortraitEffect.YOUTHFUL, 0.85f).also { result ->
                        recycleTemp(enhanced, bitmap, lifted, result)
                    }
                }
                3 -> {
                    val enhanced = imageEnhancer.enhance(lifted)
                    upscale(enhanced, 2).also { result ->
                        recycleTemp(enhanced, bitmap, lifted, result)
                    }
                }
                else -> {
                    val enhanced = imageEnhancer.enhance(lifted)
                    portraitStudio.apply(enhanced, PortraitEffect.YOUTHFUL, 0.55f).also { result ->
                        recycleTemp(enhanced, bitmap, lifted, result)
                    }
                }
            }
            recycleTemp(lifted, bitmap, out)
            out
        }.getOrElse { runCatching { imageEnhancer.enhance(bitmap) }.getOrDefault(bitmap) }
    }

    suspend fun expandOrUpscale(bitmap: Bitmap, optionIndex: Int): Bitmap = withContext(Dispatchers.Default) {
        if (!isUsable(bitmap)) return@withContext bitmap
        runCatching {
            when (optionIndex) {
                2 -> upscale(bitmap, 2)
                3 -> upscale(bitmap, 4)
                4 -> CreateExtraAlgorithms.squarePad(bitmap)
                5 -> {
                    val scaled = upscale(bitmap, 2)
                    imageEnhancer.enhance(scaled).also { result ->
                        recycleTemp(scaled, bitmap, result)
                    }
                }
                else -> {
                    val ratio = CreateExtraAlgorithms.expandPaddingRatio(optionIndex)
                    val expanded = CreateExtraAlgorithms.expandCanvas(bitmap, ratio)
                    val capped = BitmapUtils.downscaleCopy(expanded, MAX_OUT_SIDE)
                    recycleTemp(expanded, bitmap, capped)
                    imageEnhancer.enhance(capped).also { result ->
                        recycleTemp(capped, bitmap, result)
                    }
                }
            }
        }.getOrElse { runCatching { imageEnhancer.enhance(bitmap) }.getOrDefault(bitmap) }
    }

    suspend fun faceRecastOrSwap(bitmap: Bitmap, optionIndex: Int): Bitmap = withContext(Dispatchers.Default) {
        if (!isUsable(bitmap)) return@withContext bitmap
        runCatching {
            val faces = runCatching {
                faceDetector.process(InputImage.fromBitmap(bitmap, 0)).await()
            }.getOrDefault(emptyList()).sortedByDescending {
                it.boundingBox.width() * it.boundingBox.height()
            }
            when {
                faces.size >= 2 -> swapTwoFaces(bitmap, faces[0].boundingBox, faces[1].boundingBox)
                faces.size == 1 -> {
                    val effect = when (optionIndex % 5) {
                        0 -> PortraitEffect.GLOW
                        1 -> PortraitEffect.YOUTHFUL
                        2 -> PortraitEffect.RELIGHT
                        3 -> PortraitEffect.TEETH
                        else -> PortraitEffect.HAIR
                    }
                    portraitStudio.apply(bitmap, effect, 0.9f)
                }
                else -> portraitStudio.apply(imageEnhancer.enhance(bitmap), PortraitEffect.GLOW, 0.7f)
            }
        }.getOrElse { runCatching { imageEnhancer.enhance(bitmap) }.getOrDefault(bitmap) }
    }

    /**
     * C04 background options (must stay aligned with [com.pixijoy.ai.ui.board.ctools.BoardFlowC04Session]):
     * 0 Auto cutout (transparent) · 1 Studio white · 2 Studio black · 3 Transparent ·
     * 4 Blur BG · 5 Accent gradient · 6 Remove object ·
     * 7 Custom solid ([customColor] or gray) · 8 Custom photo ([bgBitmap] via [compositeOnScene]).
     */
    suspend fun removeBgOptions(
        bitmap: Bitmap,
        optionIndex: Int,
        params: MatteRefineParams = MatteRefineParams(),
        customColor: Int? = null,
        bgBitmap: Bitmap? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        if (!isUsable(bitmap)) return@withContext bitmap
        runCatching {
            when (optionIndex) {
                4 -> portraitBlur.applyBlur(bitmap)
                6 -> objectEraser.removePeople(bitmap)
                1 -> {
                    val cut = backgroundRemover.removeBackground(bitmap, params)
                    compositeOnColor(cut, Color.WHITE).also { out -> recycleTemp(cut, bitmap, out) }
                }
                2 -> {
                    val cut = backgroundRemover.removeBackground(bitmap, params)
                    compositeOnColor(cut, Color.BLACK).also { out -> recycleTemp(cut, bitmap, out) }
                }
                5 -> {
                    val cut = backgroundRemover.removeBackground(bitmap, params)
                    compositeOnAccentGradient(cut).also { out -> recycleTemp(cut, bitmap, out) }
                }
                7, 8 -> {
                    val cut = backgroundRemover.removeBackground(bitmap, params)
                    fillCutout(cut, optionIndex, customColor, bgBitmap).also { out ->
                        recycleTemp(cut, bitmap, out)
                    }
                }
                // 0 auto, 3 transparent — alpha cutout
                else -> backgroundRemover.removeBackground(bitmap, params)
            }
        }.getOrElse {
            runCatching { imageEnhancer.enhance(bitmap) }.getOrDefault(bitmap)
        }
    }

    /**
     * Returns transparent cutout + display bitmap for the chosen option.
     * Always stores a reusable cutout when segmentation succeeds (for refine / scenes).
     */
    suspend fun removeBgWithCutout(
        bitmap: Bitmap,
        optionIndex: Int,
        params: MatteRefineParams = MatteRefineParams(),
        customColor: Int? = null,
        bgBitmap: Bitmap? = null
    ): Pair<Bitmap?, Bitmap> = withContext(Dispatchers.Default) {
        if (!isUsable(bitmap)) return@withContext null to bitmap
        runCatching {
            when (optionIndex) {
                4 -> null to portraitBlur.applyBlur(bitmap)
                6 -> null to objectEraser.removePeople(bitmap)
                else -> {
                    val cut = backgroundRemover.removeBackground(bitmap, params)
                    val display = fillCutout(cut, optionIndex, customColor, bgBitmap)
                    cut to display
                }
            }
        }.getOrElse {
            val soft = runCatching { imageEnhancer.enhance(bitmap) }.getOrDefault(bitmap)
            null to soft
        }
    }

    /** Transparent cutout only — used so variations can re-composite fills. */
    suspend fun cutoutTransparent(
        bitmap: Bitmap,
        params: MatteRefineParams = MatteRefineParams()
    ): Bitmap = withContext(Dispatchers.Default) {
        if (!isUsable(bitmap)) return@withContext bitmap
        runCatching { backgroundRemover.removeBackground(bitmap, params) }
            .getOrElse { runCatching { imageEnhancer.enhance(bitmap) }.getOrDefault(bitmap) }
    }

    fun refineCutout(cutout: Bitmap, params: MatteRefineParams): Bitmap {
        if (!isUsable(cutout)) return cutout
        return runCatching { backgroundRemover.refineCutoutAlpha(cutout, params) }
            .getOrDefault(cutout)
    }

    fun fillCutout(
        cutout: Bitmap,
        optionIndex: Int,
        customColor: Int? = null,
        bgBitmap: Bitmap? = null
    ): Bitmap {
        if (!isUsable(cutout)) return cutout
        return when (optionIndex) {
            1 -> compositeOnColor(cutout, Color.WHITE)
            2 -> compositeOnColor(cutout, Color.BLACK)
            5 -> compositeOnAccentGradient(cutout)
            7 -> compositeOnColor(cutout, customColor ?: Color.rgb(96, 125, 139))
            8 -> if (bgBitmap != null && isUsable(bgBitmap)) {
                compositeOnScene(cutout, bgBitmap)
            } else {
                compositeOnAccentGradient(cutout)
            }
            else -> cutout
        }
    }

    /** Composite transparent cutout over a scene / studio background bitmap. */
    fun compositeOnScene(cutout: Bitmap, background: Bitmap): Bitmap {
        if (!isUsable(cutout)) return cutout
        val w = cutout.width
        val h = cutout.height
        val bg = if (background.width == w && background.height == h) {
            background
        } else {
            Bitmap.createScaledBitmap(background, w, h, true)
        }
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(bg, 0f, 0f, null)
        canvas.drawBitmap(cutout, 0f, 0f, null)
        if (bg !== background && !bg.isRecycled) runCatching { bg.recycle() }
        return output
    }

    /**
     * C04 variation index → display:
     * 0 original option fill · 1 white · 2 beach · 3 office · 4 bokeh · 5 solid gray.
     */
    fun applyC04Variation(
        cutout: Bitmap,
        variationIndex: Int,
        optionIndex: Int,
        sceneBitmap: Bitmap?
    ): Bitmap {
        if (!isUsable(cutout)) return cutout
        return when (variationIndex) {
            1 -> compositeOnColor(cutout, Color.WHITE)
            2, 3, 4 -> if (sceneBitmap != null && isUsable(sceneBitmap)) {
                compositeOnScene(cutout, sceneBitmap)
            } else {
                fillCutout(cutout, optionIndex)
            }
            5 -> compositeOnColor(cutout, Color.rgb(236, 236, 236))
            else -> fillCutout(cutout, optionIndex)
        }
    }

    suspend fun remixStyle(bitmap: Bitmap, optionIndex: Int, keepFace: Boolean): Bitmap =
        withContext(Dispatchers.Default) {
            if (!isUsable(bitmap)) return@withContext bitmap
            runCatching {
                val base = imageEnhancer.enhance(bitmap)
                when (optionIndex) {
                    1 -> CreateExtraAlgorithms.boostSaturation(base, 1.25f).also { out ->
                        recycleTemp(base, bitmap, out)
                    }
                    3 -> if (keepFace) {
                        portraitStudio.apply(base, PortraitEffect.RELIGHT, 0.4f).also { out ->
                            recycleTemp(base, bitmap, out)
                        }
                    } else {
                        base
                    }
                    else -> base
                }
            }.getOrElse { runCatching { imageEnhancer.enhance(bitmap) }.getOrDefault(bitmap) }
        }

    /** C07 relocate: cutout subject onto a scene bitmap. */
    suspend fun relocateToScene(user: Bitmap, scene: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        if (!isUsable(user)) return@withContext user
        runCatching {
            val cut = cutoutTransparent(user)
            compositeOnScene(cut, scene).also { out -> recycleTemp(cut, user, out) }
        }.getOrElse { runCatching { imageEnhancer.enhance(user) }.getOrDefault(user) }
    }

    /** C08 remove people when faces present; otherwise enhance. */
    suspend fun peopleEdit(bitmap: Bitmap, removeMode: Boolean): Bitmap = withContext(Dispatchers.Default) {
        if (!isUsable(bitmap)) return@withContext bitmap
        if (removeMode) {
            runCatching { objectEraser.removePeople(bitmap) }
                .getOrElse { runCatching { imageEnhancer.enhance(bitmap) }.getOrDefault(bitmap) }
        } else {
            // Add-person lite: enhance + youthful portrait so the path is real (not generative insert).
            enhanceFaceLook(bitmap)
        }
    }

    private fun upscale(bitmap: Bitmap, factor: Int): Bitmap {
        val tw = CreateExtraAlgorithms.cappedScaledSide(bitmap.width, factor, MAX_OUT_SIDE)
        val th = CreateExtraAlgorithms.cappedScaledSide(bitmap.height, factor, MAX_OUT_SIDE)
        if (tw == bitmap.width && th == bitmap.height) return bitmap
        return Bitmap.createScaledBitmap(bitmap, tw, th, true)
    }

    private fun swapTwoFaces(bitmap: Bitmap, a: Rect, b: Rect): Bitmap {
        val out = BitmapUtils.copySafe(bitmap)
        val cropA = cropClamped(bitmap, a)
        val cropB = cropClamped(bitmap, b)
        try {
            val canvas = Canvas(out)
            drawFeathered(canvas, cropB, a)
            drawFeathered(canvas, cropA, b)
        } finally {
            recycleTemp(cropA, bitmap, out)
            recycleTemp(cropB, bitmap, out)
        }
        return out
    }

    private fun cropClamped(bitmap: Bitmap, box: Rect): Bitmap {
        if (!isUsable(bitmap)) return bitmap
        val l = box.left.coerceIn(0, bitmap.width - 1)
        val t = box.top.coerceIn(0, bitmap.height - 1)
        val r = box.right.coerceIn(l + 1, bitmap.width)
        val btm = box.bottom.coerceIn(t + 1, bitmap.height)
        return Bitmap.createBitmap(bitmap, l, t, max(1, r - l), max(1, btm - t))
    }

    private fun drawFeathered(canvas: Canvas, face: Bitmap, dest: Rect) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val destF = RectF(dest)
        val layer = canvas.saveLayer(destF, paint)
        canvas.drawOval(destF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(face, null, destF, paint)
        paint.xfermode = null
        canvas.restoreToCount(layer)
    }

    private fun compositeOnColor(cutout: Bitmap, color: Int): Bitmap {
        val output = Bitmap.createBitmap(cutout.width, cutout.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(color)
        canvas.drawBitmap(cutout, 0f, 0f, null)
        return output
    }

    /** PixiJoy AI accent gradient fill (#6C4DFF → #FF5AA5). */
    private fun compositeOnAccentGradient(cutout: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(cutout.width, cutout.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                cutout.width.toFloat(),
                cutout.height.toFloat(),
                intArrayOf(0xFF6C4DFF.toInt(), 0xFFFF5AA5.toInt()),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, cutout.width.toFloat(), cutout.height.toFloat(), paint)
        canvas.drawBitmap(cutout, 0f, 0f, null)
        return output
    }

    private fun isUsable(bitmap: Bitmap): Boolean =
        !bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0

    /** Recycle a temp bitmap only when it is not still needed as input or output. */
    private fun recycleTemp(temp: Bitmap?, vararg keep: Bitmap?) {
        if (temp == null || temp.isRecycled) return
        if (keep.any { it === temp }) return
        runCatching { temp.recycle() }
    }

    companion object {
        /** Cap output side to avoid OOM (matches ImageProcessor load ceiling). */
        const val MAX_OUT_SIDE = 2048
    }
}
