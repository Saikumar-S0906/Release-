package com.pixijoy.movie

import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Shared preview/export decision for a PropGltf plate.
 *
 * Filament draws the prop only when the on-demand renderer is bound and a
 * project file is present. Cube, ground, sky, and dust stay on the classical
 * raster. A Filament miss uses that same raster.
 */
enum class CgPropDrawPath {
    Classical,
    Filament
}

fun cgPropDrawPath(
    preset: CgPreset,
    filamentPropBound: Boolean,
    hasPropFile: Boolean
): CgPropDrawPath {
    val filamentProp = preset == CgPreset.PropGltf && filamentPropBound && hasPropFile
    return if (filamentProp) CgPropDrawPath.Filament else CgPropDrawPath.Classical
}

/**
 * One plate request for both rasters. [propFile] is a file under the project props directory.
 * Filament reads that file; the classical raster parses it into [GltfMesh].
 */
data class CgPlateRequest(
    val preset: CgPreset,
    val width: Int,
    val height: Int,
    val scale: Float,
    val yaw: Float,
    val pitch: Float = 0f,
    val ox: Float = 0f,
    val oy: Float = 0f,
    val progress: Float = 0f,
    val propFile: File? = null
)

/** Marker for the on-demand Filament prop renderer. Classical raster does not implement this. */
interface FilamentPropCgRenderer : CgSceneRenderer

/**
 * Runs the Filament plate when [path] says so. Any failure returns [classical]
 * so a missing split or a bad frame does not drop the prop.
 */
internal fun <T> cgRenderPropOrClassical(
    path: CgPropDrawPath,
    filament: () -> T,
    classical: () -> T
): T {
    if (path != CgPropDrawPath.Filament) return classical()
    return try {
        filament()
    } catch (_: RuntimeException) {
        classical()
    } catch (_: Error) {
        classical()
    }
}

/** `{project}/props/{name}` when [propFile] is a safe glTF name and the file exists. */
fun cgPropFile(projectDir: File?, propFile: String): File? {
    if (projectDir == null || !GltfProp.isSafePropFileName(propFile)) return null
    val file = File(File(projectDir, CG_PROP_DIR), propFile)
    return file.takeIf { it.isFile }
}

/**
 * Sibling of [propFile] for a glTF buffer or texture URI.
 * Rejects absolute paths and `..` so a prop cannot read outside its props directory.
 */
fun cgPropResourceFile(propFile: File, uri: String): File? {
    if (!isSafePropResourceUri(uri)) return null
    val parentPath = propFile.parentFile?.let { runCatching { it.canonicalFile }.getOrNull() }
    val childPath = parentPath?.let { runCatching { File(it, uri).canonicalFile }.getOrNull() }
    val prefix = parentPath?.let { it.path + File.separator }
    val inside = childPath != null && prefix != null &&
        (childPath.path == parentPath.path || childPath.path.startsWith(prefix))
    return childPath?.takeIf { inside && it.isFile }
}

internal fun isSafePropResourceUri(uri: String): Boolean {
    if (uri.isBlank() || uri.length > 180) return false
    if (uri.contains('\\') || uri.contains(':') || uri.startsWith("/")) return false
    if (uri.contains("..")) return false
    return true
}

/**
 * Column-major root transform for a Filament prop.
 *
 * The mesh is centered, fit to a unit box, then scaled. Yaw is about Y and
 * pitch about X. Environment lighting lives on the Filament scene (IBL), not
 * in this matrix.
 */
fun cgFilamentPropTransform(
    yawDeg: Float,
    pitchDeg: Float,
    userScale: Float,
    center: FloatArray,
    halfExtent: FloatArray
): FloatArray {
    val hx = abs(halfExtent.getOrElse(0) { 0.5f })
    val hy = abs(halfExtent.getOrElse(1) { 0.5f })
    val hz = abs(halfExtent.getOrElse(2) { 0.5f })
    val extent = max(hx, max(hy, hz)) * 2f
    val fit = if (extent < 1e-4f) 1f else 1f / extent
    val s = fit * userScale.coerceIn(0.35f, 2.5f)
    val yaw = Math.toRadians(yawDeg.toDouble())
    val pitch = Math.toRadians(pitchDeg.toDouble())
    val cy = cos(yaw).toFloat()
    val sy = sin(yaw).toFloat()
    val cp = cos(pitch).toFloat()
    val sp = sin(pitch).toFloat()
    val c0x = cy * s
    val c0y = 0f
    val c0z = -sy * s
    val c1x = -sy * sp * s
    val c1y = cp * s
    val c1z = -cy * sp * s
    val c2x = sy * cp * s
    val c2y = sp * s
    val c2z = cy * cp * s
    val cx = center.getOrElse(0) { 0f }
    val cy0 = center.getOrElse(1) { 0f }
    val cz = center.getOrElse(2) { 0f }
    val tx = -(c0x * cx + c1x * cy0 + c2x * cz)
    val ty = -(c0y * cx + c1y * cy0 + c2y * cz)
    val tz = -(c0z * cx + c1z * cy0 + c2z * cz)
    return floatArrayOf(
        c0x, c0y, c0z, 0f,
        c1x, c1y, c1z, 0f,
        c2x, c2y, c2z, 0f,
        tx, ty, tz, 1f
    )
}

/**
 * Camera distance that frames a unit-box prop at 45° vertical fov.
 * User scale is in the mesh transform, so preview and export share this distance.
 */
fun cgFilamentCameraDistance(): Double {
    val half = 0.5
    val fov = Math.toRadians(45.0)
    return (half / kotlin.math.tan(fov / 2.0)) * 1.35
}
