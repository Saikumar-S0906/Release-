package com.pixijoy.ml.pack

/**
 * Tensor-shape gates so a random `.tflite` cannot register as a neural pack engine.
 */
object PackModelContract {
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

    fun tileSize(shape: IntArray, fallback: Int = 50): Pair<Int, Int> {
        val h = if (shape.size >= 3 && shape[1] > 0) shape[1] else fallback
        val w = if (shape.size >= 3 && shape[2] > 0) shape[2] else fallback
        return h to w
    }
}
