package com.pixijoy.photo

import android.graphics.Bitmap
import java.util.PriorityQueue
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * On-device content-aware fill / inpaint.
 *
 * Primary path: Telea-like fast-marching fill from known surroundings (no network).
 * [LiteRtInpaintHook] runs only when the E1 inpaint pack is installed **and** a real
 * interpreter is bound. Telea is never reported as neural.
 *
 * Distinct from [applyHeal]: Heal is clone-stamp / ring-average per brush stamp;
 * Fill builds a unified mask then reconstructs the hole from the border inward.
 */
fun applyFill(src: Bitmap, stamps: List<Stamp>): Bitmap {
    if (stamps.isEmpty()) return src
    val w = src.width
    val h = src.height
    if (w <= 0 || h <= 0) return src

    val mask = buildFillMask(w, h, stamps)
    if (mask.none { it }) return src

    LiteRtInpaintHook.tryInpaint(src, mask)?.let { return it }

    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val px = IntArray(w * h)
    out.getPixels(px, 0, w, 0, 0, w, h)
    teleaFill(px, mask, w, h)
    out.setPixels(px, 0, w, 0, 0, w, h)
    return out
}

/**
 * Optional LiteRT inpaint. [isAvailable] is true only with a checksumed pack **and**
 * a bound interpreter. Returns null → classical Telea path.
 */
object LiteRtInpaintHook {
    fun isAvailable(): Boolean = PackEnhance.inpaintAvailable()

    fun tryInpaint(src: Bitmap, mask: BooleanArray): Bitmap? = PackEnhance.tryInpaint(src, mask)
}

/** Pure helper for unit tests: union stamp circles into a mask. */
internal fun buildFillMask(width: Int, height: Int, stamps: List<Stamp>): BooleanArray {
    val mask = BooleanArray(width * height)
    for (stamp in stamps) {
        val cx = (stamp.x * width).roundToInt()
        val cy = (stamp.y * height).roundToInt()
        val radius = (stamp.r * min(width, height)).roundToInt().coerceAtLeast(1)
        for (y in (cy - radius).coerceAtLeast(0)..(cy + radius).coerceAtMost(height - 1)) {
            for (x in (cx - radius).coerceAtLeast(0)..(cx + radius).coerceAtMost(width - 1)) {
                if (hypot((x - cx).toDouble(), (y - cy).toDouble()) <= radius) {
                    mask[y * width + x] = true
                }
            }
        }
    }
    return mask
}

/**
 * Telea-inspired fill: process hole pixels by distance-to-known, sampling a local
 * known neighborhood with distance + direction weights, then blend.
 */
internal fun teleaFill(px: IntArray, mask: BooleanArray, w: Int, h: Int) {
    val known = BooleanArray(w * h) { !mask[it] }
    val dist = FloatArray(w * h) { if (known[it]) 0f else Float.POSITIVE_INFINITY }

    data class Node(val i: Int, val d: Float)
    val pq = PriorityQueue<Node>(compareBy { it.d })
    val inQueue = BooleanArray(w * h)

    fun trySeed(i: Int) {
        if (known[i] || inQueue[i]) return
        val x = i % w
        val y = i / w
        var best = Float.POSITIVE_INFINITY
        for (dy in -1..1) for (dx in -1..1) {
            if (dx == 0 && dy == 0) continue
            val nx = x + dx
            val ny = y + dy
            if (nx !in 0 until w || ny !in 0 until h) continue
            val ni = ny * w + nx
            if (known[ni]) {
                val d = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (d < best) best = d
            }
        }
        if (best.isFinite()) {
            dist[i] = best
            pq.add(Node(i, best))
            inQueue[i] = true
        }
    }

    for (i in mask.indices) if (mask[i]) trySeed(i)

    val sampleR = 4
    while (pq.isNotEmpty()) {
        val node = pq.poll() ?: break
        val i = node.i
        if (known[i]) continue
        if (node.d > dist[i] + 1e-3f) continue

        val x = i % w
        val y = i / w
        var wr = 0.0
        var wg = 0.0
        var wb = 0.0
        var wa = 0.0
        var wt = 0.0

        var gx = 0.0
        var gy = 0.0
        var gn = 0
        for (dy in -1..1) for (dx in -1..1) {
            if (dx == 0 && dy == 0) continue
            val nx = x + dx
            val ny = y + dy
            if (nx !in 0 until w || ny !in 0 until h) continue
            val ni = ny * w + nx
            if (!known[ni]) continue
            val c = px[ni]
            val lum = ((c shr 16) and 0xff) * 0.299 + ((c shr 8) and 0xff) * 0.587 + (c and 0xff) * 0.114
            gx += dx * lum
            gy += dy * lum
            gn++
        }
        if (gn > 0) {
            gx /= gn
            gy /= gn
        }
        val gLen = sqrt(gx * gx + gy * gy).coerceAtLeast(1e-3)

        for (sy in (y - sampleR).coerceAtLeast(0)..(y + sampleR).coerceAtMost(h - 1)) {
            for (sx in (x - sampleR).coerceAtLeast(0)..(x + sampleR).coerceAtMost(w - 1)) {
                val si = sy * w + sx
                if (!known[si]) continue
                val dx = (sx - x).toDouble()
                val dy = (sy - y).toDouble()
                val d = hypot(dx, dy).coerceAtLeast(1e-3)
                if (d > sampleR + 0.5) continue
                val dirDot = ((dx / d) * (gx / gLen) + (dy / d) * (gy / gLen)).coerceIn(-1.0, 1.0)
                val weight = (1.0 / d) * (1.0 + dirDot).coerceAtLeast(0.05)
                val c = px[si]
                wa += weight * ((c ushr 24) and 0xff)
                wr += weight * ((c shr 16) and 0xff)
                wg += weight * ((c shr 8) and 0xff)
                wb += weight * (c and 0xff)
                wt += weight
            }
        }

        if (wt > 1e-6) {
            val a = (wa / wt).roundToInt().coerceIn(0, 255)
            val r = (wr / wt).roundToInt().coerceIn(0, 255)
            val g = (wg / wt).roundToInt().coerceIn(0, 255)
            val b = (wb / wt).roundToInt().coerceIn(0, 255)
            val edgeSoft = (1f - (dist[i] / (sampleR + 2f)).coerceIn(0f, 1f)) * 0.15f
            px[i] = mixArgbFill(px[i], (a shl 24) or (r shl 16) or (g shl 8) or b, 1f - edgeSoft)
        }

        known[i] = true
        dist[i] = node.d

        val nbrs = intArrayOf(i - 1, i + 1, i - w, i + w)
        for (ni in nbrs) {
            if (ni < 0 || ni >= known.size) continue
            if (known[ni]) continue
            val nx = ni % w
            val ny = ni / w
            if (kotlin.math.abs(nx - x) + kotlin.math.abs(ny - y) != 1) continue
            val nd = node.d + 1f
            if (nd < dist[ni]) {
                dist[ni] = nd
                pq.add(Node(ni, nd))
                inQueue[ni] = true
            }
        }
    }

    for (i in mask.indices) {
        if (!mask[i] || known[i]) continue
        val x = i % w
        val y = i / w
        var wr = 0; var wg = 0; var wb = 0; var wa = 0; var n = 0
        for (sy in (y - 3).coerceAtLeast(0)..(y + 3).coerceAtMost(h - 1)) {
            for (sx in (x - 3).coerceAtLeast(0)..(x + 3).coerceAtMost(w - 1)) {
                val si = sy * w + sx
                if (!known[si] && mask[si]) continue
                val c = px[si]
                wa += (c ushr 24) and 0xff
                wr += (c shr 16) and 0xff
                wg += (c shr 8) and 0xff
                wb += c and 0xff
                n++
            }
        }
        if (n > 0) {
            px[i] = (wa / n shl 24) or (wr / n shl 16) or (wg / n shl 8) or (wb / n)
            known[i] = true
        }
    }
}

private fun mixArgbFill(orig: Int, fill: Int, t: Float): Int {
    val tt = t.coerceIn(0f, 1f)
    fun mix(a: Int, b: Int) = (a + (b - a) * tt).roundToInt().coerceIn(0, 255)
    val a = mix((orig ushr 24) and 0xff, (fill ushr 24) and 0xff)
    val r = mix((orig shr 16) and 0xff, (fill shr 16) and 0xff)
    val g = mix((orig shr 8) and 0xff, (fill shr 8) and 0xff)
    val b = mix(orig and 0xff, fill and 0xff)
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
