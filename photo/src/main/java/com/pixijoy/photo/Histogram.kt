package com.pixijoy.photo

data class HistogramClip(
    val shadowPct: Float,
    val highlightPct: Float,
    val peak: Int
)

fun histogramClip(
    bins: IntArray,
    shadowEnd: Int = 5,
    highlightStart: Int = 251
): HistogramClip {
    if (bins.isEmpty()) return HistogramClip(0f, 0f, 0)
    val total = bins.sum().coerceAtLeast(1).toFloat()
    val lo = shadowEnd.coerceIn(1, bins.size)
    val hi = highlightStart.coerceIn(0, bins.size)
    val shadows = bins.copyOfRange(0, lo).sum()
    val highlights = if (hi >= bins.size) 0 else bins.copyOfRange(hi, bins.size).sum()
    val peak = bins.indices.maxByOrNull { bins[it] } ?: 0
    return HistogramClip(shadows / total, highlights / total, peak)
}
