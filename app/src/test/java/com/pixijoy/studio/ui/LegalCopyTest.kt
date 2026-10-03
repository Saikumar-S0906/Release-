package com.pixijoy.studio.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The shipped policy and terms stay in the app and do not point at a website. */
class LegalCopyTest {
    @Test
    fun policyAndTermsAreLocalAndMatchTheApp() {
        val privacy = File("src/main/assets/legal/privacy.txt").readText()
        val terms = File("src/main/assets/legal/terms.txt").readText()
        assertFalse(privacy.contains("http://") || privacy.contains("https://"))
        assertFalse(terms.contains("http://") || terms.contains("https://"))
        assertFalse(privacy.contains("www."))
        assertFalse(terms.contains("www."))
        assertTrue(privacy.contains("does not upload"))
        assertTrue(privacy.contains("PixiJoy/2.0"))
        assertTrue(privacy.contains("GPS"))
        assertTrue(terms.contains("privacy policy"))
        val gradle = File("build.gradle.kts").readText()
        assertTrue(gradle.contains("namespace = \"com.pixijoy.studio\""))
        assertTrue(gradle.contains("applicationId = \"com.pixijoy.studio\""))
        assertFalse(gradle.contains("com.pixijoy.ai"))
        val strings = File("src/main/res").walk().filter { it.name == "strings.xml" }
        strings.forEach { file ->
            val text = file.readText()
            assertFalse(file.path, text.contains("studio_url_privacy"))
            assertFalse(file.path, text.contains("studio_url_terms"))
            assertFalse(file.path, text.contains("pixijoy.ai"))
            assertFalse(file.path, text.contains("PixiJoyAI"))
        }
    }
}
