package com.pixijoy.ai.ui.flow.extra

import com.pixijoy.ai.domain.model.PhotoTool
import com.pixijoy.ai.ml.PortraitEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateExtraToolMapperTest {

    @Test
    fun consentRequired_forFaceAndPeopleOnly() {
        assertTrue(CreateExtraToolMapper.needsConsent(CreateExtraTool.FaceSwap))
        assertTrue(CreateExtraToolMapper.needsConsent(CreateExtraTool.People))
        CreateExtraTool.entries
            .filter { it != CreateExtraTool.FaceSwap && it != CreateExtraTool.People }
            .forEach { tool ->
                assertFalse("${tool.name} should not need consent", CreateExtraToolMapper.needsConsent(tool))
            }
    }

    @Test
    fun networkRequired_forSceneAndRemixOnly() {
        assertTrue(CreateExtraToolMapper.requiresNetwork(CreateExtraTool.Scene))
        assertTrue(CreateExtraToolMapper.requiresNetwork(CreateExtraTool.Remix))
        CreateExtraTool.entries
            .filter { it != CreateExtraTool.Scene && it != CreateExtraTool.Remix }
            .forEach { tool ->
                assertFalse("${tool.name} should be offline-capable", CreateExtraToolMapper.requiresNetwork(tool))
            }
    }

    @Test
    fun creditCosts_allToolsPositive() {
        CreateExtraTool.entries.forEach { tool ->
            assertTrue("${tool.name} cost", CreateExtraToolMapper.creditCost(tool) > 0)
        }
    }

    @Test
    fun creditCosts_matchExpectedSchedule() {
        assertEquals(1, CreateExtraToolMapper.creditCost(CreateExtraTool.RemoveBg))
        assertEquals(2, CreateExtraToolMapper.creditCost(CreateExtraTool.Restore))
        assertEquals(3, CreateExtraToolMapper.creditCost(CreateExtraTool.FaceSwap))
        assertEquals(2, CreateExtraToolMapper.creditCost(CreateExtraTool.Scene))
        assertEquals(3, CreateExtraToolMapper.creditCost(CreateExtraTool.People))
        assertEquals(3, CreateExtraToolMapper.creditCost(CreateExtraTool.Portrait))
        assertEquals(1, CreateExtraToolMapper.creditCost(CreateExtraTool.Expand))
        assertEquals(3, CreateExtraToolMapper.creditCost(CreateExtraTool.Talking))
        assertEquals(1, CreateExtraToolMapper.creditCost(CreateExtraTool.Greeting))
        assertEquals(2, CreateExtraToolMapper.creditCost(CreateExtraTool.Remix))
        assertEquals(1, CreateExtraToolMapper.creditCost(CreateExtraTool.EmptyHome))
    }

    @Test
    fun photoToolHint_fullMatrix() {
        assertEquals(PhotoTool.BACKGROUND_REMOVE, CreateExtraToolMapper.photoToolHint(CreateExtraTool.RemoveBg))
        assertEquals(PhotoTool.ENHANCE, CreateExtraToolMapper.photoToolHint(CreateExtraTool.Restore))
        assertEquals(PhotoTool.ENHANCE, CreateExtraToolMapper.photoToolHint(CreateExtraTool.Expand))
        assertEquals(PhotoTool.ENHANCE, CreateExtraToolMapper.photoToolHint(CreateExtraTool.Remix))
        assertEquals(PhotoTool.ENHANCE, CreateExtraToolMapper.photoToolHint(CreateExtraTool.EmptyHome))
        assertEquals(PhotoTool.BACKGROUND_REPLACE, CreateExtraToolMapper.photoToolHint(CreateExtraTool.Scene))
        assertNull(CreateExtraToolMapper.photoToolHint(CreateExtraTool.FaceSwap))
        assertNull(CreateExtraToolMapper.photoToolHint(CreateExtraTool.People))
        assertNull(CreateExtraToolMapper.photoToolHint(CreateExtraTool.Portrait))
        assertNull(CreateExtraToolMapper.photoToolHint(CreateExtraTool.Talking))
        assertNull(CreateExtraToolMapper.photoToolHint(CreateExtraTool.Greeting))
    }

    @Test
    fun scenePrompt_coversAllOptionsAndBounds() {
        val expected = listOf(
            "sunny beach",
            "city night lights",
            "mountain landscape",
            "cozy cafe",
            "professional studio",
            "outer space nebula",
            "festival lights",
            "custom scenic backdrop"
        )
        expected.forEachIndexed { index, prompt ->
            assertEquals(prompt, CreateExtraToolMapper.scenePrompt(index))
        }
        assertEquals("professional studio", CreateExtraToolMapper.scenePrompt(-1))
        assertEquals("professional studio", CreateExtraToolMapper.scenePrompt(8))
        assertEquals("professional studio", CreateExtraToolMapper.scenePrompt(99))
    }

    @Test
    fun portraitEffect_indicesZeroThroughFour() {
        assertEquals(PortraitEffect.GLOW, CreateExtraToolMapper.portraitEffect(0))
        assertEquals(PortraitEffect.RELIGHT, CreateExtraToolMapper.portraitEffect(1))
        assertEquals(PortraitEffect.YOUTHFUL, CreateExtraToolMapper.portraitEffect(2))
        assertEquals(PortraitEffect.TEETH, CreateExtraToolMapper.portraitEffect(3))
        assertEquals(PortraitEffect.HAIR, CreateExtraToolMapper.portraitEffect(4))
        // wraps by PortraitEffect.entries.size (5)
        assertEquals(PortraitEffect.GLOW, CreateExtraToolMapper.portraitEffect(5))
        assertEquals(PortraitEffect.RELIGHT, CreateExtraToolMapper.portraitEffect(6))
    }

    @Test
    fun createHubTools_sizeAndExclusions() {
        assertEquals(9, CreateExtraTool.createHubTools.size)
        assertFalse(CreateExtraTool.createHubTools.contains(CreateExtraTool.EmptyHome))
        assertFalse(CreateExtraTool.createHubTools.contains(CreateExtraTool.Remix))
        assertEquals(
            listOf(
                CreateExtraTool.RemoveBg,
                CreateExtraTool.Restore,
                CreateExtraTool.FaceSwap,
                CreateExtraTool.Scene,
                CreateExtraTool.People,
                CreateExtraTool.Portrait,
                CreateExtraTool.Expand,
                CreateExtraTool.Talking,
                CreateExtraTool.Greeting
            ),
            CreateExtraTool.createHubTools
        )
    }

    @Test
    fun routeKeys_matchCatalog() {
        assertEquals("empty_home", CreateExtraTool.EmptyHome.routeKey)
        assertEquals("remove_bg", CreateExtraTool.RemoveBg.routeKey)
        assertEquals("restore", CreateExtraTool.Restore.routeKey)
        assertEquals("face_swap", CreateExtraTool.FaceSwap.routeKey)
        assertEquals("scene", CreateExtraTool.Scene.routeKey)
        assertEquals("people", CreateExtraTool.People.routeKey)
        assertEquals("portrait", CreateExtraTool.Portrait.routeKey)
        assertEquals("expand", CreateExtraTool.Expand.routeKey)
        assertEquals("talking", CreateExtraTool.Talking.routeKey)
        assertEquals("greeting", CreateExtraTool.Greeting.routeKey)
        assertEquals("remix", CreateExtraTool.Remix.routeKey)
    }

    @Test
    fun fromRoute_knownAndFallback() {
        assertEquals(CreateExtraTool.Expand, CreateExtraTool.fromRoute("expand"))
        assertEquals(CreateExtraTool.RemoveBg, CreateExtraTool.fromRoute("unknown_tool"))
    }

    @Test
    fun expandPadFractions_coversOptions() {
        assertEquals(0.06f, CreateExtraToolMapper.expandPadFractions(1), 0.0001f)
        assertEquals(0.18f, CreateExtraToolMapper.expandPadFractions(4), 0.0001f)
        assertEquals(0.18f, CreateExtraToolMapper.expandPadFractions(5), 0.0001f)
        assertEquals(0.12f, CreateExtraToolMapper.expandPadFractions(0), 0.0001f)
        assertEquals(0.12f, CreateExtraToolMapper.expandPadFractions(99), 0.0001f)
    }
}
