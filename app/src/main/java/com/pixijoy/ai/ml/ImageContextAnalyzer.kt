package com.pixijoy.ai.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.pixijoy.ai.domain.model.ImageContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageContextAnalyzer @Inject constructor() {

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.6f)
            .build()
    )

    suspend fun analyze(bitmap: Bitmap): ImageContext {
        val input = InputImage.fromBitmap(bitmap, 0)
        val faces = faceDetector.process(input).await()
        val labels = labeler.process(input).await().map { it.text.lowercase() }

        val faceCount = faces.size
        val labelText = labels.joinToString(" ")

        return ImageContext(
            labels = labels,
            faceCount = faceCount,
            isSelfie = faceCount == 1 && bitmap.width < bitmap.height * 1.2f,
            isGroup = faceCount >= 2,
            isOutdoor = labels.any { it in OUTDOOR_KEYWORDS },
            isFood = labels.any { it in FOOD_KEYWORDS },
            isBirthday = labels.any { it in BIRTHDAY_KEYWORDS },
            isPet = labels.any { it in PET_KEYWORDS }
        )
    }

    companion object {
        private val OUTDOOR_KEYWORDS = setOf("sky", "mountain", "beach", "tree", "nature", "outdoor", "travel")
        private val FOOD_KEYWORDS = setOf("food", "meal", "dish", "pizza", "coffee", "drink", "dessert")
        private val BIRTHDAY_KEYWORDS = setOf("cake", "birthday", "party", "celebration", "balloon")
        private val PET_KEYWORDS = setOf("dog", "cat", "pet", "animal", "puppy")
    }
}
