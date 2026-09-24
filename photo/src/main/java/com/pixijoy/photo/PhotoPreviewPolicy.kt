package com.pixijoy.photo

/**
 * Preview stays at a fit resolution for everyday editing. Heal/mask inspection
 * can request a higher spot render once the viewport is zoomed in.
 */
object PhotoPreviewPolicy {
    const val FIT_MAX_EDGE = 1600
    const val SPOT_MAX_EDGE = 4096
    const val EXPORT_MAX_EDGE = 8192
    const val SPOT_ZOOM_THRESHOLD = 2f

    fun previewMaxEdge(zoom: Float, inspectDetail: Boolean): Int =
        if (inspectDetail && zoom >= SPOT_ZOOM_THRESHOLD) SPOT_MAX_EDGE else FIT_MAX_EDGE

    fun usesSpotRender(zoom: Float, inspectDetail: Boolean): Boolean =
        previewMaxEdge(zoom, inspectDetail) == SPOT_MAX_EDGE
}
