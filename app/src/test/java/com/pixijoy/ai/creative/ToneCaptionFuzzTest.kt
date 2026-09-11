package com.pixijoy.ai.creative

import com.pixijoy.ai.domain.model.CaptionTone
import com.pixijoy.ai.domain.model.ImageContext
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Property / fuzz-style inputs — caption generator must never crash or return blanks.
 */
@RunWith(Parameterized::class)
class ToneCaptionFuzzTest(
    private val context: ImageContext,
    private val tone: CaptionTone
) {
    private val generator = ToneCaptionGenerator()

    @Test
    fun generate_neverCrashes_andReturnsNonBlank() {
        val (top, bottom) = generator.generate(context, tone)
        assertNotNull(top)
        assertNotNull(bottom)
        assertTrue("blank top for $tone / $context", top.isNotBlank())
        assertTrue("blank bottom for $tone / $context", bottom.isNotBlank())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun data(): Collection<Array<Any>> {
            val contexts = listOf(
                ImageContext(),
                ImageContext(isSelfie = true),
                ImageContext(isPet = true),
                ImageContext(isSelfie = true, isPet = true),
                ImageContext(isGroup = true, faceCount = 4),
                ImageContext(isOutdoor = true),
                ImageContext(isFood = true),
                ImageContext(isBirthday = true),
                ImageContext(labels = listOf("dog", "park", "sunset"), isOutdoor = true, isPet = true),
                ImageContext(labels = List(50) { "label$it" }, faceCount = 8, isGroup = true)
            )
            return contexts.flatMap { ctx ->
                CaptionTone.entries.map { tone -> arrayOf<Any>(ctx, tone) }
            }
        }
    }
}
