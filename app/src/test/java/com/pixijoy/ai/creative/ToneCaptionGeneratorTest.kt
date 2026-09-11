package com.pixijoy.ai.creative

import com.pixijoy.ai.domain.model.CaptionTone
import com.pixijoy.ai.domain.model.ImageContext
import org.junit.Assert.assertTrue
import org.junit.Test

class ToneCaptionGeneratorTest {

    private val generator = ToneCaptionGenerator()

    @Test
    fun generatesCaption_forEachTone() {
        val context = ImageContext(isSelfie = true)
        CaptionTone.entries.forEach { tone ->
            val (top, bottom) = generator.generate(context, tone)
            assertTrue("Top empty for $tone", top.isNotBlank())
            assertTrue("Bottom empty for $tone", bottom.isNotBlank())
        }
    }

    @Test
    fun petContext_usesPetSubject() {
        val (_, bottom) = generator.generate(ImageContext(isPet = true), CaptionTone.ENGLISH)
        assertTrue(bottom.contains("PET", ignoreCase = true))
    }
}
