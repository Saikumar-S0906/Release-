package com.pixijoy.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.google.android.gms.tflite.java.TfLite
import com.pixijoy.ml.pack.PackEngineHooks
import com.pixijoy.ml.pack.PackId
import com.pixijoy.ml.pack.PackModelContract
import com.pixijoy.ml.pack.PackRuntime
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import org.tensorflow.lite.InterpreterApi

/**
 * Binds Play-services LiteRT interpreters to [PackEngineHooks] when a pack file
 * matches a known tensor contract. Missing GMS or a mismatched graph leaves hooks null.
 */
object PackLiteRt {
    private const val MAX_WORK_EDGE = 256
    private val lock = Any()
    private val exec = Executors.newSingleThreadExecutor()
    private var app: Context? = null
    private var tfliteReady = false
    private val engines = mutableMapOf<PackId, InterpreterApi>()

    fun start(context: Context) {
        val appCtx = context.applicationContext
        synchronized(lock) {
            app = appCtx
            PackRuntime.onPackReady = { id, file -> scheduleBind(id, file) }
            PackRuntime.onPackRemoved = { id -> scheduleUnbind(id) }
        }
        TfLite.initialize(appCtx)
            .addOnSuccessListener {
                synchronized(lock) { tfliteReady = true }
                bindInstalled()
            }
            .addOnFailureListener {
                synchronized(lock) { tfliteReady = false }
            }
    }

    private fun scheduleUnbind(id: PackId) {
        exec.execute {
            synchronized(lock) {
                engines.remove(id)?.close()
                when (id) {
                    PackId.SuperRes -> PackEngineHooks.superRes = null
                    PackId.Inpaint -> PackEngineHooks.inpaint = null
                    PackId.Denoise -> PackEngineHooks.denoise = null
                    PackId.FrameInterp -> PackEngineHooks.frameInterpBound = false
                    PackId.Track -> PackEngineHooks.trackBound = false
                }
            }
        }
    }

    private fun bindInstalled() {
        val mgr = PackRuntime.manager() ?: return
        PackId.entries.forEach { id ->
            if (mgr.isReady(id)) scheduleBind(id, mgr.modelFile(id))
        }
    }

    private fun scheduleBind(id: PackId, file: File) {
        exec.execute { bindSync(id, file) }
    }

    private fun bindSync(id: PackId, file: File) {
        if (!file.isFile) return
        val ready = synchronized(lock) { tfliteReady }
        if (!ready) return
        when (id) {
            PackId.SuperRes -> bindSuperRes(file)
            PackId.Inpaint,
            PackId.Denoise,
            PackId.FrameInterp,
            PackId.Track -> Unit
        }
    }

    private fun bindSuperRes(file: File) {
        val interpreter = openInterpreter(file) ?: return
        val inShape = interpreter.getInputTensor(0).shape()
        if ((inShape.getOrNull(1) ?: 0) <= 0 || (inShape.getOrNull(2) ?: 0) <= 0) {
            runCatching {
                interpreter.resizeInput(0, intArrayOf(1, 50, 50, 3))
                interpreter.allocateTensors()
            }
        }
        val scale = PackModelContract.superResScale(
            interpreter.getInputTensor(0).shape(),
            interpreter.getOutputTensor(0).shape()
        )
        if (scale == null) {
            interpreter.close()
            return
        }
        synchronized(lock) {
            engines.remove(PackId.SuperRes)?.close()
            engines[PackId.SuperRes] = interpreter
            PackEngineHooks.superRes = { bitmap, requested ->
                runCatching { superRes(bitmap, requested) }.getOrNull()
            }
        }
    }

    private fun openInterpreter(file: File): InterpreterApi? {
        val options = InterpreterApi.Options()
            .setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)
            .setNumThreads(4)
        return runCatching { InterpreterApi.create(file, options) }.getOrNull()
    }

    private fun superRes(src: Bitmap, requestedScale: Int): Bitmap? {
        val interpreter = synchronized(lock) { engines[PackId.SuperRes] } ?: return null
        val inShape = interpreter.getInputTensor(0).shape()
        val outShape = interpreter.getOutputTensor(0).shape()
        val modelScale = PackModelContract.superResScale(inShape, outShape) ?: return null
        val (tileH, tileW) = PackModelContract.tileSize(inShape)
        val outTileH = outShape[1]
        val outTileW = outShape[2]
        val want = if (requestedScale >= 4) 4 else 2
        val work = capLongEdge(src, MAX_WORK_EDGE)
        try {
            val assembled = Bitmap.createBitmap(
                (work.width * modelScale).coerceAtLeast(1),
                (work.height * modelScale).coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(assembled)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            var y = 0
            while (y < work.height) {
                var x = 0
                while (x < work.width) {
                    val tw = min(tileW, work.width - x)
                    val th = min(tileH, work.height - y)
                    val tile = Bitmap.createBitmap(work, x, y, tw, th)
                    val padded = if (tw != tileW || th != tileH) {
                        Bitmap.createScaledBitmap(tile, tileW, tileH, true).also { scaled ->
                            if (scaled !== tile) tile.recycle()
                        }
                    } else {
                        tile
                    }
                    val hr = inferRgb(interpreter, padded, tileH, tileW, outTileH, outTileW)
                    if (!padded.isRecycled) padded.recycle()
                    canvas.drawBitmap(
                        hr,
                        null,
                        Rect(
                            x * modelScale,
                            y * modelScale,
                            (x + tw) * modelScale,
                            (y + th) * modelScale
                        ),
                        paint
                    )
                    hr.recycle()
                    x += tileW
                }
                y += tileH
            }
            val targetW = (src.width * want).coerceAtLeast(1)
            val targetH = (src.height * want).coerceAtLeast(1)
            if (assembled.width == targetW && assembled.height == targetH) return assembled
            val scaled = Bitmap.createScaledBitmap(assembled, targetW, targetH, true)
            if (scaled !== assembled) assembled.recycle()
            return scaled
        } finally {
            if (work !== src && !work.isRecycled) work.recycle()
        }
    }

    private fun inferRgb(
        interpreter: InterpreterApi,
        tile: Bitmap,
        inH: Int,
        inW: Int,
        outH: Int,
        outW: Int
    ): Bitmap {
        val sized = if (tile.width != inW || tile.height != inH) {
            Bitmap.createScaledBitmap(tile, inW, inH, true)
        } else {
            tile
        }
        val input = Array(1) { Array(inH) { Array(inW) { FloatArray(3) } } }
        val px = IntArray(inW * inH)
        sized.getPixels(px, 0, inW, 0, 0, inW, inH)
        if (sized !== tile && !sized.isRecycled) sized.recycle()
        var i = 0
        for (row in 0 until inH) {
            for (col in 0 until inW) {
                val c = px[i++]
                input[0][row][col][0] = ((c shr 16) and 0xff) / 255f
                input[0][row][col][1] = ((c shr 8) and 0xff) / 255f
                input[0][row][col][2] = (c and 0xff) / 255f
            }
        }
        val output = Array(1) { Array(outH) { Array(outW) { FloatArray(3) } } }
        interpreter.run(input, output)
        val outPx = IntArray(outW * outH)
        i = 0
        for (row in 0 until outH) {
            for (col in 0 until outW) {
                val r = (output[0][row][col][0].coerceIn(0f, 1f) * 255f).toInt()
                val g = (output[0][row][col][1].coerceIn(0f, 1f) * 255f).toInt()
                val b = (output[0][row][col][2].coerceIn(0f, 1f) * 255f).toInt()
                outPx[i++] = (0xff shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        bmp.setPixels(outPx, 0, outW, 0, 0, outW, outH)
        return bmp
    }

    private fun capLongEdge(src: Bitmap, maxEdge: Int): Bitmap {
        val longEdge = max(src.width, src.height)
        if (longEdge <= maxEdge) return src
        val scale = maxEdge / longEdge.toFloat()
        val w = (src.width * scale).toInt().coerceAtLeast(1)
        val h = (src.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }
}
