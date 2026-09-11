package com.pixijoy.ai.creative

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class FaceTextZones(
    val topTextY: Float,
    val bottomTextY: Float,
    val hasFace: Boolean
)

@Singleton
class FaceAwareTextPlacer @Inject constructor() {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .build()
    )

    suspend fun detectTextZones(bitmap: Bitmap): FaceTextZones {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val faces = detector.process(inputImage).await()
        if (faces.isEmpty()) {
            return FaceTextZones(
                topTextY = 0.12f,
                bottomTextY = 0.92f,
                hasFace = false
            )
        }
        val primaryFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            ?: return FaceTextZones(0.12f, 0.92f, false)
        val box: Rect = primaryFace.boundingBox
        val topY = (box.top.toFloat() / bitmap.height).coerceIn(0.05f, 0.4f)
        val bottomY = (box.bottom.toFloat() / bitmap.height).coerceIn(0.6f, 0.95f)
        return FaceTextZones(
            topTextY = topY * 0.5f,
            bottomTextY = bottomY + (1f - bottomY) * 0.5f,
            hasFace = true
        )
    }
}
