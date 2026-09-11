package com.pixijoy.ai.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.pixijoy.ai.util.BitmapUtils
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Passport-style ID photo: white background, 35:45 head-and-shoulders crop. */
@Singleton
class IdPhotoProcessor @Inject constructor(
    private val backgroundRemover: BackgroundRemover
) {
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    suspend fun create(bitmap: Bitmap): Bitmap {
        val cutout = runCatching { backgroundRemover.removeBackground(bitmap) }
            .getOrElse { BitmapUtils.copy(bitmap) }
        val outW = 350
        val outH = 450
        val output = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)

        val faceBox = detectFaceBox(bitmap)
        val subject = scaleSubject(cutout, outW, outH, faceBox)
        canvas.drawBitmap(subject, 0f, 0f, null)
        if (subject != cutout) subject.recycle()
        if (cutout != bitmap) cutout.recycle()
        return output
    }

    private suspend fun detectFaceBox(bitmap: Bitmap): Rect? {
        val faces = faceDetector.process(InputImage.fromBitmap(bitmap, 0)).await()
        return faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }?.boundingBox
    }

    private fun scaleSubject(source: Bitmap, outW: Int, outH: Int, faceBox: Rect?): Bitmap {
        val targetHeadHeight = (outH * 0.55f).toInt()
        val faceHeight = faceBox?.height()?.coerceAtLeast(1) ?: (source.height / 3)
        val scale = targetHeadHeight.toFloat() / faceHeight
        val scaledW = (source.width * scale).toInt().coerceAtLeast(1)
        val scaledH = (source.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(source, scaledW, scaledH, true)

        val faceCenterX = faceBox?.let { (it.left + it.right) / 2f * scale } ?: scaledW / 2f
        val faceCenterY = faceBox?.let { (it.top + it.bottom) / 2f * scale } ?: scaledH * 0.35f
        val left = (outW / 2f - faceCenterX).toInt().coerceIn(outW - scaledW, 0)
        val top = (outH * 0.22f - faceCenterY).toInt().coerceIn(outH - scaledH, 0)

        val result = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(scaled, left.toFloat(), top.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG))
        if (scaled != source) scaled.recycle()
        return result
    }
}
