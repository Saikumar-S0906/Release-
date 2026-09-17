package com.pixijoy.ml

/** Null Object until a Play Feature pack is installed. */
interface VisionFacade {
    val available: Boolean
}

object UnavailableVision : VisionFacade {
    override val available: Boolean = false
}
