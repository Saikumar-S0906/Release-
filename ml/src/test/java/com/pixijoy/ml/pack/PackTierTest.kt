package com.pixijoy.ml.pack

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackTierTest {
    @After
    fun reset() = PackTier.overrideForTest(false)

    @Test
    fun flagship_needs_ram_64bit_and_android12() {
        assertTrue(PackTier.isFlagship(8_000_000_000L, is64Bit = true, sdk = 33, lowRam = false))
        assertFalse(PackTier.isFlagship(6_000_000_000L, is64Bit = true, sdk = 33, lowRam = false))
        assertFalse(PackTier.isFlagship(12_000_000_000L, is64Bit = false, sdk = 33, lowRam = false))
        assertFalse(PackTier.isFlagship(12_000_000_000L, is64Bit = true, sdk = 30, lowRam = false))
        assertFalse(PackTier.isFlagship(12_000_000_000L, is64Bit = true, sdk = 34, lowRam = true))
    }

    @Test
    fun hq_tier_swaps_only_depth_inpaint_matting() {
        PackTier.overrideForTest(true)
        listOf(PackId.Depth, PackId.Inpaint, PackId.Matting).forEach { id ->
            val s = PackCatalog.spec(id)
            assertEquals(PackCatalog.V2_HQ, s.version)
            assertTrue(s.downloadUrl!!.contains("/ml-packs-v2-hq/"))
            assertEquals(64, s.downloadSha256!!.length)
            assertEquals(PackCatalog.standardSpec(id).playModule, s.playModule)
        }
        assertEquals(PackCatalog.standardSpec(PackId.Flow), PackCatalog.spec(PackId.Flow))
        PackTier.overrideForTest(false)
        assertEquals(PackCatalog.V2, PackCatalog.spec(PackId.Depth).version)
    }

    @Test
    fun hq_edges_accepted_by_depth_and_matte_contracts() {
        val hqIn = intArrayOf(1, 512, 512, 3)
        val hqOut = intArrayOf(1, 512, 512, 1)
        assertTrue(PackModelContract.isMonocularDepth(hqIn, hqOut))
        assertTrue(PackModelContract.isSubjectMatte(hqIn, hqOut))
        assertFalse(PackModelContract.isMonocularDepth(intArrayOf(1, 384, 384, 3), intArrayOf(1, 384, 384, 1)))
        assertFalse(PackModelContract.isMonocularDepth(hqIn, intArrayOf(1, 256, 256, 1)))
        assertTrue(
            PackModelContract.isDenseFlow(
                intArrayOf(1, 256, 256, 3), intArrayOf(1, 256, 256, 3), intArrayOf(1, 256, 256, 2)
            )
        )
    }
}
