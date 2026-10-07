package com.pixijoy.ml.pack

/**
 * Tensor-shape gates so a random `.tflite` cannot register as a neural pack engine.
 *
 * Pinned graphs (dtype is checked at bind, not here):
 * - Super-res `compressed_esrgan.tflite`: FLOAT32 NHWC `1×180×320×3` → `1×720×1280×3` (4×).
 * - Denoise `unet_rgb_fast.tflite`: FLOAT32 NHWC `1×256×256×3` → `1×256×256×3`.
 * - FrameInterp FILM community TFLite: FLOAT32 time `(1,1)` + two NHWC RGB
 *   `(1,H,W,3)` → at least one NHWC RGB `(1,H,W,3)` (pinned path is 512×512).
 *
 * Track (F1): FLOAT32 template `1×64×64×3` + search `1×128×128×3` → `1×2` xy.
 */
object PackModelContract {
    const val TRACK_TEMPLATE = 64
    const val TRACK_SEARCH = 128
    /** Frozen monocular-depth spatial size. 0 is near, 1 is far. */
    const val DEPTH_EDGE = 256

    /**
     * PixiJoy point-track: template + search NHWC RGB → normalized `(x,y)`.
     */
    fun isPointTrack(template: IntArray, search: IntArray, out: IntArray): Boolean {
        if (!isNhwcRgb(template) || !isNhwcRgb(search)) return false
        if (template[1] != TRACK_TEMPLATE || template[2] != TRACK_TEMPLATE) return false
        if (search[1] != TRACK_SEARCH || search[2] != TRACK_SEARCH) return false
        return out.size == 2 && out[0] == 1 && out[1] == 2
    }

    /** Resolve template/search input indices by spatial size (LiteRT may reorder names). */
    fun trackInputIndices(template: IntArray, search: IntArray): Pair<Int, Int>? {
        // Caller passes shapes at index 0 and 1; return which index is template vs search.
        val aIsTemplate = isNhwcRgb(template) &&
            template[1] == TRACK_TEMPLATE && template[2] == TRACK_TEMPLATE
        val bIsSearch = isNhwcRgb(search) &&
            search[1] == TRACK_SEARCH && search[2] == TRACK_SEARCH
        if (aIsTemplate && bIsSearch) return 0 to 1
        val aIsSearch = isNhwcRgb(template) &&
            template[1] == TRACK_SEARCH && template[2] == TRACK_SEARCH
        val bIsTemplate = isNhwcRgb(search) &&
            search[1] == TRACK_TEMPLATE && search[2] == TRACK_TEMPLATE
        if (aIsSearch && bIsTemplate) return 1 to 0
        return null
    }

    /** FILM-style: time + frame0 + frame1 → RGB mid-frame (same H×W). */
    fun isFrameInterpFilm(
        time: IntArray,
        frame0: IntArray,
        frame1: IntArray,
        rgbOutputs: List<IntArray>
    ): Boolean {
        if (time.size != 2 || time[0] != 1 || time[1] != 1) return false
        if (!isNhwcRgb(frame0) || !isNhwcRgb(frame1)) return false
        if (frame0[1] != frame1[1] || frame0[2] != frame1[2]) return false
        val h = frame0[1]
        val w = frame0[2]
        if (h < 8 || w < 8) return false
        return rgbOutputs.any { out ->
            isNhwcRgb(out) && out[1] == h && out[2] == w
        }
    }

    /**
     * NHWC RGB, batch 1, uniform 2× or 4×. Returns the scale, or null when the
     * graph is not that super-res contract (including same-size denoise).
     */
    fun superResScale(input: IntArray, output: IntArray): Int? {
        if (input.size != 4 || output.size != 4) return null
        if (input[0] != 1 || output[0] != 1) return null
        if (input[3] != 3 || output[3] != 3) return null
        val ih = input[1]
        val iw = input[2]
        val oh = output[1]
        val ow = output[2]
        if (ih <= 0 || iw <= 0 || oh <= 0 || ow <= 0) return null
        if (oh % ih != 0 || ow % iw != 0) return null
        val scaleH = oh / ih
        val scaleW = ow / iw
        if (scaleH != scaleW) return null
        if (scaleH != 2 && scaleH != 4) return null
        return scaleH
    }

    fun isInpaintRgbMask(input: IntArray, output: IntArray): Boolean {
        if (input.size != 4 || output.size != 4) return false
        return input[0] == 1 &&
            output[0] == 1 &&
            input[1] > 0 &&
            input[2] > 0 &&
            input[1] == output[1] &&
            input[2] == output[2] &&
            input[3] == 4 &&
            output[3] == 3
    }

    /** Two-input inpaint: RGB image + 1-channel mask, RGB out, same spatial size. */
    fun isInpaintRgbAndMask(image: IntArray, mask: IntArray, output: IntArray): Boolean {
        if (!isNhwcRgb(image) || !isNhwcRgb(output)) return false
        if (image[1] != output[1] || image[2] != output[2]) return false
        val (mh, mw) = maskSpatial(mask) ?: return false
        return mh == image[1] && mw == image[2]
    }

    /**
     * Image index to mask index. LiteRT may put the mask first (the on-disk
     * graph does). Null when the two tensors are not RGB plus a same-size mask.
     */
    fun inpaintInputIndices(first: IntArray, second: IntArray): Pair<Int, Int>? {
        if (isRgbPlusMask(first, second)) return 0 to 1
        if (isRgbPlusMask(second, first)) return 1 to 0
        return null
    }

    /**
     * Monocular relative depth: NHWC RGB `1×256×256×3` → `1×256×256×1`
     * FLOAT32, 0 near → 1 far. Bind checks dtype. This is not a quality score.
     */
    fun isMonocularDepth(input: IntArray, output: IntArray): Boolean {
        if (!isNhwcRgb(input)) return false
        if (input[1] != DEPTH_EDGE || input[2] != DEPTH_EDGE) return false
        return output.size == 4 &&
            output[0] == 1 &&
            output[1] == DEPTH_EDGE &&
            output[2] == DEPTH_EDGE &&
            output[3] == 1
    }

    /**
     * Subject matte (MODNet): NHWC RGB `1×256×256×3` → alpha `1×256×256×1` FLOAT32, 1 = subject.
     * Same shapes as depth; the pack id decides which engine binds.
     */
    fun isSubjectMatte(input: IntArray, output: IntArray): Boolean = isMonocularDepth(input, output)

    /** Dense flow: two NHWC RGB `1×256×256×3` frames → `1×256×256×2` FLOAT32 `(dx, dy) / 256`. */
    fun isDenseFlow(frame0: IntArray, frame1: IntArray, output: IntArray): Boolean {
        if (!isNhwcRgb(frame0) || !isNhwcRgb(frame1)) return false
        if (frame0[1] != DEPTH_EDGE || frame0[2] != DEPTH_EDGE) return false
        if (frame1[1] != DEPTH_EDGE || frame1[2] != DEPTH_EDGE) return false
        return output.size == 4 && output[0] == 1 && output[1] == DEPTH_EDGE &&
            output[2] == DEPTH_EDGE && output[3] == 2
    }

    private fun isRgbPlusMask(image: IntArray, mask: IntArray): Boolean {
        if (!isNhwcRgb(image)) return false
        val (mh, mw) = maskSpatial(mask) ?: return false
        return mh == image[1] && mw == image[2]
    }

    /**
     * Same-size RGB restore, NHWC or NCHW. Rejects 2×/4× super-res graphs so an
     * ESRGAN file cannot register as denoise. Bind also requires FLOAT32.
     */
    fun isDenoiseSameRgb(input: IntArray, output: IntArray): Boolean {
        if (superResScale(input, output) != null) return false
        if (isNhwcRgb(input) && isNhwcRgb(output)) {
            return input[1] == output[1] && input[2] == output[2]
        }
        if (isNchwRgb(input) && isNchwRgb(output)) {
            return input[2] == output[2] && input[3] == output[3]
        }
        return false
    }

    fun isNhwcRgb(shape: IntArray): Boolean =
        shape.size == 4 && shape[0] == 1 && shape[3] == 3 && shape[1] > 0 && shape[2] > 0

    fun isNchwRgb(shape: IntArray): Boolean =
        shape.size == 4 && shape[0] == 1 && shape[1] == 3 && shape[2] > 0 && shape[3] > 0

    fun maskSpatial(shape: IntArray): Pair<Int, Int>? = when (shape.size) {
        3 -> if (shape[0] == 1 && shape[1] > 0 && shape[2] > 0) shape[1] to shape[2] else null
        4 -> if (shape[0] == 1 && shape[1] > 0 && shape[2] > 0 && shape[3] == 1) {
            shape[1] to shape[2]
        } else {
            null
        }
        else -> null
    }

    fun tileSize(shape: IntArray, fallback: Int = 50): Pair<Int, Int> {
        val h = if (shape.size >= 3 && shape[1] > 0) shape[1] else fallback
        val w = if (shape.size >= 3 && shape[2] > 0) shape[2] else fallback
        return h to w
    }
}
