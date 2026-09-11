package com.pixijoy.ai.creative

import android.graphics.Bitmap
import com.pixijoy.ai.core.ai.CloudAiService
import com.pixijoy.ai.core.network.VariationStyleJson
import com.pixijoy.ai.domain.model.PhotoFilter
import javax.inject.Inject
import javax.inject.Singleton

data class ImageVariation(val label: String, val bitmap: Bitmap)

@Singleton
class ImageVariationGenerator @Inject constructor(
    private val photoEditProcessor: PhotoEditProcessor,
    private val cloudAiService: CloudAiService
) {
    suspend fun generate(bitmap: Bitmap, count: Int = 4): List<ImageVariation> {
        val styles = runCatching { cloudAiService.suggestVariationStyles(bitmap) }
            .getOrElse { defaultStyles() }
            .take(count)
        return styles.map { style ->
            val varied = when (style.style.lowercase()) {
                "warm" -> photoEditProcessor.applyFilter(bitmap, PhotoFilter.WARM)
                "cool" -> photoEditProcessor.applyFilter(bitmap, PhotoFilter.COOL)
                "vintage" -> photoEditProcessor.applyFilter(bitmap, PhotoFilter.VINTAGE)
                "vivid" -> photoEditProcessor.applyFilter(bitmap, PhotoFilter.VIVID)
                "cinematic" -> photoEditProcessor.applyFilter(bitmap, PhotoFilter.CINEMATIC)
                "bw" -> photoEditProcessor.applyFilter(bitmap, PhotoFilter.BW)
                "glow" -> photoEditProcessor.applyFilter(bitmap, PhotoFilter.AI_GLOW)
                else -> photoEditProcessor.applyFilter(bitmap, PhotoFilter.PORTRAIT)
            }
            ImageVariation(style.label, varied)
        }
    }

    private fun defaultStyles() = listOf(
        VariationStyleJson("Warm cinematic", "warm"),
        VariationStyleJson("Cool night", "cool"),
        VariationStyleJson("Vintage film", "vintage"),
        VariationStyleJson("Vivid pop", "vivid")
    )
}
