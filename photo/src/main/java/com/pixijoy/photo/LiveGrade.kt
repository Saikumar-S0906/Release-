package com.pixijoy.photo

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

object LiveGrade {
    fun curvesIdentity(ys: FloatArray): Boolean =
        ys.size >= 5 &&
            ys[0] == 0f && ys[1] == 0.25f && ys[2] == 0.5f && ys[3] == 0.75f && ys[4] == 1f

    fun isAdjustIdentity(exposure: Float, contrast: Float, hue: Float, saturation: Float): Boolean =
        exposure == 0f && contrast == 1f && hue == 0f && saturation == 1f

    fun vignetteMul(nx: Float, ny: Float, amount: Float): Float {
        val a = amount.coerceIn(0f, 1f)
        if (a <= 0f) return 1f
        val d = hypot(nx - 0.5f, ny - 0.5f) / 0.70710678f
        val fall = ((d - (1f - a)) / a.coerceAtLeast(0.001f)).coerceIn(0f, 1f)
        return (1f - a * fall * fall).coerceIn(0f, 1f)
    }

    fun colorMatrix(
        exposure: Float,
        contrast: Float,
        hueDeg: Float,
        saturation: Float
    ): FloatArray {
        val grade = gradeMatrix(exposure, contrast)
        val sat = saturationMatrix(saturation)
        val hue = hueMatrix(hueDeg)
        return concat(hue, concat(sat, grade))
    }

    internal fun gradeMatrix(exposureEv: Float, contrast: Float): FloatArray {
        val gain = 2.0.pow(exposureEv.toDouble()).toFloat()
        val translate = (1f - contrast) * 0.18f * 255f
        val s = contrast * gain
        return floatArrayOf(
            s, 0f, 0f, 0f, translate,
            0f, s, 0f, 0f, translate,
            0f, 0f, s, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
    }

    internal fun saturationMatrix(saturation: Float): FloatArray {
        val s = saturation.coerceAtLeast(0f)
        val r = 0.213f * (1f - s)
        val g = 0.715f * (1f - s)
        val b = 0.072f * (1f - s)
        return floatArrayOf(
            r + s, g, b, 0f, 0f,
            r, g + s, b, 0f, 0f,
            r, g, b + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    internal fun hueMatrix(degrees: Float): FloatArray {
        val rad = Math.toRadians(degrees.toDouble())
        val c = cos(rad).toFloat()
        val n = sin(rad).toFloat()
        val lr = 0.213f
        val lg = 0.715f
        val lb = 0.072f
        return floatArrayOf(
            lr + c * (1f - lr) - n * lr, lg - c * lg - n * lg, lb - c * lb + n * (1f - lb), 0f, 0f,
            lr - c * lr + n * 0.143f, lg + c * (1f - lg) + n * 0.140f, lb - c * lb - n * 0.283f, 0f, 0f,
            lr - c * lr - n * (1f - lr), lg - c * lg + n * lg, lb + c * (1f - lb) + n * lb, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** Result = a × b, so b is applied first. */
    internal fun concat(a: FloatArray, b: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (i in 0..3) {
            val a0 = a[i * 5]
            val a1 = a[i * 5 + 1]
            val a2 = a[i * 5 + 2]
            val a3 = a[i * 5 + 3]
            val a4 = a[i * 5 + 4]
            for (j in 0..3) {
                out[i * 5 + j] = a0 * b[j] + a1 * b[5 + j] + a2 * b[10 + j] + a3 * b[15 + j]
            }
            out[i * 5 + 4] = a0 * b[4] + a1 * b[9] + a2 * b[14] + a3 * b[19] + a4
        }
        return out
    }

    const val CURVES_AGSL = """
uniform shader img;
uniform float y0;
uniform float y1;
uniform float y2;
uniform float y3;
uniform float y4;

float evalCurve(float x) {
  float t = clamp(x, 0.0, 1.0);
  if (t <= 0.25) return mix(y0, y1, t / 0.25);
  if (t <= 0.50) return mix(y1, y2, (t - 0.25) / 0.25);
  if (t <= 0.75) return mix(y2, y3, (t - 0.50) / 0.25);
  return mix(y3, y4, (t - 0.75) / 0.25);
}

half4 main(float2 coord) {
  half4 c = img.eval(coord);
  return half4(evalCurve(c.r), evalCurve(c.g), evalCurve(c.b), c.a);
}
"""

    const val DETAIL_AGSL = """
uniform shader img;
uniform float2 size;
uniform float sharpen;
uniform float vignette;

half4 main(float2 coord) {
  half4 c = img.eval(coord);
  if (sharpen > 0.001) {
    float2 t = float2(1.0, 1.0) / max(size, float2(1.0, 1.0));
    half4 acc = c;
    acc += img.eval(coord + float2(t.x, 0.0));
    acc += img.eval(coord - float2(t.x, 0.0));
    acc += img.eval(coord + float2(0.0, t.y));
    acc += img.eval(coord - float2(0.0, t.y));
    acc += img.eval(coord + float2(t.x, t.y));
    acc += img.eval(coord + float2(-t.x, t.y));
    acc += img.eval(coord + float2(t.x, -t.y));
    acc += img.eval(coord + float2(-t.x, -t.y));
    half4 blur = acc / 9.0;
    c = half4(clamp(c.rgb + sharpen * (c.rgb - blur.rgb), 0.0, 1.0), c.a);
  }
  if (vignette > 0.001) {
    float2 p = coord / max(size, float2(1.0, 1.0)) - float2(0.5);
    float d = length(p) / 0.70710678;
    float a = clamp(vignette, 0.0, 1.0);
    float fall = clamp((d - (1.0 - a)) / max(a, 0.001), 0.0, 1.0);
    float m = 1.0 - a * fall * fall;
    c = half4(c.rgb * m, c.a);
  }
  return c;
}
"""
}
