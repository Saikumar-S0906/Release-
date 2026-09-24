package com.pixijoy.photo

/**
 * Selective HSL stays on the measured CPU path. A GPU port is only justified
 * after drag jank exceeds the editor budget; do not flip this without numbers.
 */
object SelectiveHslGpu {
    const val requested: Boolean = false
}
