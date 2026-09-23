package com.pixijoy.movie

import android.content.Context
import android.opengl.GLES20
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect

/**
 * Media3 [GlEffect] that punches [ClipChroma] key colors toward transparent black.
 * Same path for CompositionPlayer preview and Transformer export.
 */
class ChromaKeyEffect(
    private val keyR: Float,
    private val keyG: Float,
    private val keyB: Float,
    private val similarity: Float,
    private val softness: Float
) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): BaseGlShaderProgram =
        ChromaKeyShaderProgram(useHdr, keyR, keyG, keyB, similarity, softness)

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = false

    companion object {
        fun from(chroma: ClipChroma): ChromaKeyEffect? {
            if (chroma.isIdentity) return null
            val n = chroma.normalized()
            val rgb = n.keyRgb() ?: return null
            return ChromaKeyEffect(rgb[0], rgb[1], rgb[2], n.similarity, n.softness)
        }
    }
}

private class ChromaKeyShaderProgram(
    useHdr: Boolean,
    keyR: Float,
    keyG: Float,
    keyB: Float,
    similarity: Float,
    softness: Float
) : BaseGlShaderProgram(/* useHighPrecisionColorComponents= */ useHdr, /* texturePoolCapacity= */ 1) {
    private val glProgram: GlProgram = try {
        GlProgram(VERTEX, FRAGMENT)
    } catch (e: GlUtil.GlException) {
        throw VideoFrameProcessingException(e)
    }

    init {
        glProgram.setBufferAttribute(
            "aFramePosition",
            GlUtil.getNormalizedCoordinateBounds(),
            GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE
        )
        val identity = GlUtil.create4x4IdentityMatrix()
        glProgram.setFloatsUniform("uTransformationMatrix", identity)
        glProgram.setFloatsUniform("uTexTransformationMatrix", identity)
        glProgram.setFloatsUniform("uKeyColor", floatArrayOf(keyR, keyG, keyB))
        glProgram.setFloatUniform("uSimilarity", similarity)
        glProgram.setFloatUniform("uSoftness", softness.coerceAtLeast(0.001f))
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size = Size(inputWidth, inputHeight)

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            glProgram.use()
            glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, /* texUnitIndex= */ 0)
            glProgram.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /* first= */ 0, /* count= */ 4)
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    override fun release() {
        super.release()
        try {
            glProgram.delete()
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }
    }

    companion object {
        // Matches Media3 vertex_shader_transformation_es2.glsl (inline — not app assets).
        private val VERTEX = """
#version 100
attribute vec4 aFramePosition;
uniform mat4 uTransformationMatrix;
uniform mat4 uTexTransformationMatrix;
varying vec2 vTexSamplingCoord;
void main() {
  gl_Position = uTransformationMatrix * aFramePosition;
  vec4 texturePosition = vec4(aFramePosition.x * 0.5 + 0.5,
      aFramePosition.y * 0.5 + 0.5, 0.0, 1.0);
  vTexSamplingCoord = (uTexTransformationMatrix * texturePosition).xy;
}
""".trimIndent()

        private val FRAGMENT = """
#version 100
precision mediump float;
uniform sampler2D uTexSampler;
uniform vec3 uKeyColor;
uniform float uSimilarity;
uniform float uSoftness;
varying vec2 vTexSamplingCoord;
void main() {
  vec4 src = texture2D(uTexSampler, vTexSamplingCoord);
  float d = distance(src.rgb, uKeyColor);
  float edge0 = max(uSimilarity - uSoftness, 0.0);
  float edge1 = min(uSimilarity + uSoftness, 1.0);
  float keep = smoothstep(edge0, edge1, d);
  gl_FragColor = vec4(src.rgb * keep, src.a * keep);
}
""".trimIndent()
    }
}
