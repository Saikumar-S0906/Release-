package com.pixijoy.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter.ImageSegmenterOptions

interface VisionFacade {
    val available: Boolean
    fun subjectMask(bitmap: Bitmap): Bitmap?
}

object UnavailableVision : VisionFacade {
    override val available: Boolean = false
    override fun subjectMask(bitmap: Bitmap): Bitmap? = null
}

class MediaPipeVision(context: Context) : VisionFacade {
    private val segmenter: ImageSegmenter? = runCatching {
        val options = ImageSegmenterOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("selfie_segmenter.tflite")
                    .build()
            )
            .setRunningMode(RunningMode.IMAGE)
            .setOutputConfidenceMasks(true)
            .setOutputCategoryMask(false)
            .build()
        ImageSegmenter.createFromOptions(context.applicationContext, options)
    }.getOrNull()

    override val available: Boolean get() = segmenter != null

    override fun subjectMask(bitmap: Bitmap): Bitmap? {
        val engine = segmenter ?: return null
        return runCatching {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = engine.segment(mpImage)
            val confidence = result.confidenceMasks().get().firstOrNull() ?: return@runCatching null
            val w = confidence.width
            val h = confidence.height
            val buf = ByteBufferExtractor.extract(confidence)
            buf.rewind()
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val px = IntArray(w * h)
            val floats = buf.asFloatBuffer()
            for (i in px.indices) {
                val conf = floats.get().coerceIn(0f, 1f)
                val a = (conf * 255f).toInt().coerceIn(0, 255)
                px[i] = Color.argb(a, 255, 255, 255)
            }
            out.setPixels(px, 0, w, 0, 0, w, h)
            if (w != bitmap.width || h != bitmap.height) {
                val scaled = Bitmap.createScaledBitmap(out, bitmap.width, bitmap.height, true)
                if (scaled !== out) out.recycle()
                scaled
            } else {
                out
            }
        }.getOrNull()
    }
}
