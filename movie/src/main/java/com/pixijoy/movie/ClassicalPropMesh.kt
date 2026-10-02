package com.pixijoy.movie

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Screen position of one mesh vertex. [rx]/[ry]/[rz] stay in rotated model space
 * so the raster can shade and depth-sort without a second transform.
 */
internal class ProjPt(
    val x: Float,
    val y: Float,
    val rx: Float,
    val ry: Float,
    val rz: Float
)

/** Same plate offset the cube prop uses for X/Y place. Shared with the Filament prop plate. */
fun cgPlateOffset(ox: Float, oy: Float, width: Int, height: Int): Pair<Float, Float> {
    val w = width.coerceAtLeast(2)
    val h = height.coerceAtLeast(2)
    return (ox * w * 0.45f) to (-oy * h * 0.45f)
}

/**
 * Perspective match for [ClassicalCgRenderer]'s cube. [size] already includes the
 * layer scale, so a larger scale moves the prop farther from the plate center.
 */
internal fun projectPropVertex(
    lx: Float,
    ly: Float,
    lz: Float,
    yawDeg: Float,
    pitchDeg: Float,
    cx: Float,
    cy: Float,
    size: Float
): ProjPt {
    val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
    val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()
    val cosY = cos(yawRad)
    val sinY = sin(yawRad)
    val cosP = cos(pitchRad)
    val sinP = sin(pitchRad)
    val ry = ly * cosP - lz * sinP
    val rz0 = ly * sinP + lz * cosP
    val rx = lx * cosY - rz0 * sinY
    val rz = lx * sinY + rz0 * cosY
    val depth = 1f / (2.2f + rz * 0.015f)
    val x = cx + rx * size * depth
    val y = cy + ry * size * depth - rz * size * 0.35f * depth
    return ProjPt(x, y, rx, ry, rz)
}

internal fun drawClassicalPropMesh(
    canvas: Canvas,
    width: Int,
    height: Int,
    scale: Float,
    yaw: Float,
    pitch: Float,
    mesh: GltfMesh
) {
    val cx = width * 0.5f
    val cy = height * 0.48f
    val size = (minOf(width, height) * 0.18f * scale).coerceIn(24f, minOf(width, height) * 0.4f)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.argb(70, 0, 0, 0)
    canvas.drawOval(cx - size * 0.9f, cy + size * 0.85f, cx + size * 0.9f, cy + size * 1.15f, paint)
    val tris = rasterTriangles(mesh, yaw, pitch, cx, cy, size)
    tris.sortByDescending { it.z }
    paint.style = Paint.Style.FILL
    val path = Path()
    for (tri in tris) {
        path.rewind()
        path.moveTo(tri.x0, tri.y0)
        path.lineTo(tri.x1, tri.y1)
        path.lineTo(tri.x2, tri.y2)
        path.close()
        paint.color = tri.color
        canvas.drawPath(path, paint)
    }
}

private class RasterTri(
    val z: Float,
    val x0: Float,
    val y0: Float,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val color: Int
)

private fun rasterTriangles(
    mesh: GltfMesh,
    yaw: Float,
    pitch: Float,
    cx: Float,
    cy: Float,
    size: Float
): ArrayList<RasterTri> {
    val vertCount = mesh.positions.size / 3
    val projected = arrayOfNulls<ProjPt>(vertCount)
    for (v in 0 until vertCount) {
        projected[v] = projectPropVertex(
            mesh.positions[v * 3],
            mesh.positions[v * 3 + 1],
            mesh.positions[v * 3 + 2],
            yaw,
            pitch,
            cx,
            cy,
            size
        )
    }
    val tris = ArrayList<RasterTri>(mesh.triangleCount)
    var t = 0
    while (t + 2 < mesh.indices.size) {
        val a = projected.getOrNull(mesh.indices[t])
        val b = projected.getOrNull(mesh.indices[t + 1])
        val c = projected.getOrNull(mesh.indices[t + 2])
        t += 3
        if (a == null || b == null || c == null) continue
        val e1x = b.rx - a.rx
        val e1y = b.ry - a.ry
        val e1z = b.rz - a.rz
        val e2x = c.rx - a.rx
        val e2y = c.ry - a.ry
        val e2z = c.rz - a.rz
        val nx = e1y * e2z - e1z * e2y
        val ny = e1z * e2x - e1x * e2z
        val nz = e1x * e2y - e1y * e2x
        if (nx * nx + ny * ny + nz * nz < 1e-8f) continue
        tris.add(
            RasterTri(
                z = (a.rz + b.rz + c.rz) / 3f,
                x0 = a.x,
                y0 = a.y,
                x1 = b.x,
                y1 = b.y,
                x2 = c.x,
                y2 = c.y,
                color = classicalPropColor(nx, ny, nz)
            )
        )
    }
    return tris
}

private fun classicalPropColor(nx: Float, ny: Float, nz: Float): Int {
    val len = sqrt(nx * nx + ny * ny + nz * nz)
    if (len < 1e-4f) return Color.rgb(150, 160, 175)
    val lx = 0.35f
    val ly = -0.8f
    val lz = 0.5f
    val light = sqrt(lx * lx + ly * ly + lz * lz)
    val nd = abs((nx * lx + ny * ly + nz * lz) / (len * light))
    val lit = 0.38f + 0.62f * nd
    return Color.rgb(
        (168f * lit).toInt().coerceIn(28, 235),
        (186f * lit).toInt().coerceIn(28, 240),
        (210f * lit).toInt().coerceIn(36, 245)
    )
}
