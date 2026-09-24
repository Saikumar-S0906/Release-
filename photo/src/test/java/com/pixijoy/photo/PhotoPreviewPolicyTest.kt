package com.pixijoy.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoPreviewPolicyTest {
    @Test
    fun fitPreview_staysAt1600UntilInspectionZoom() {
        assertEquals(1600, PhotoPreviewPolicy.previewMaxEdge(1f, inspectDetail = false))
        assertEquals(1600, PhotoPreviewPolicy.previewMaxEdge(8f, inspectDetail = false))
        assertEquals(1600, PhotoPreviewPolicy.previewMaxEdge(1.9f, inspectDetail = true))
        assertFalse(PhotoPreviewPolicy.usesSpotRender(1f, inspectDetail = true))
    }

    @Test
    fun zoomedHealOrMask_requestsSpotRender() {
        assertEquals(4096, PhotoPreviewPolicy.previewMaxEdge(2f, inspectDetail = true))
        assertTrue(PhotoPreviewPolicy.usesSpotRender(3f, inspectDetail = true))
        assertEquals(8192, PhotoPreviewPolicy.EXPORT_MAX_EDGE)
    }
}
