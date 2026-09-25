package com.pixijoy.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.google.android.gms.tflite.java.TfLite
import com.pixijoy.ml.pack.PackCatalog
import com.pixijoy.ml.pack.PackId
import com.pixijoy.ml.pack.PackModelContract
import com.pixijoy.ml.pack.PackRuntime
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import org.tensorflow.lite.DataType
import org.tensorflow.lite.InterpreterApi

/**
 * Builds a real Play-services interpreter when a pack file is Ready, the SHA-256
 * matches, and [PackModelContract] accepts the graph. Does not register by itself.
 * [PackFeatureEntry] registers the engine after SplitCompat. A miss releases the
 * capability. Frame-interp and track are not bound. Missing GMS produces no engine.
 */
object PackLiteRt {
    private const val MAX_WORK_EDGE = 256
    private val lock = Any()
    private val exec = Executors.newSingleThreadExecutor()
    private var app: Context? = null
    private var tfliteReady = false
    private var sink: ((PackId, NeuralEngine?) -> Unit)? = null
    private val engines = mutableMapOf<PackId, InterpreterApi>()

    /**
     * After [PackSplits.install]. Listens for verified pack files. [sink] receives
     * an engine only when the tensor contract matches, or null when the pack is removed.
     */
    fun watch(context: Context, sink: (PackId, NeuralEngine?) -> Unit) {
        val appCtx = context.applicationContext
        val startInit = synchronized(lock) {
            this.sink = sink
            if (app != null) {
                false
            } else {
                app = appCtx
                PackRuntime.onPackReady = { id, file -> scheduleBind(id, file) }
                PackRuntime.onPackRemoved = { id -> scheduleUnbind(id) }
                true
            }
        }
        if (!startInit) {
            if (synchronized(lock) { tfliteReady }) bindInstalled()
            return
        }
        TfLite.initialize(appCtx)
            .addOnSuccessListener {
                synchronized(lock) { tfliteReady = true }
                PackRuntime.liteRtReady = true
                PackRuntime.notifyEngineChanged()
                bindInstalled()
            }
            .addOnFailureListener {
                synchronized(lock) { tfliteReady = false }
                PackRuntime.liteRtReady = false
                PackRuntime.notifyEngineChanged()
            }
    }

    private fun scheduleUnbind(id: PackId) {
        exec.execute {
            val notify = synchronized(lock) {
                engines.remove(id)?.close()
                sink
            }
            notify?.invoke(id, null)
            PackRuntime.notifyEngineChanged()
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
        try {
            if (!synchronized(lock) { tfliteReady }) return
            // E4 / F1: no artifact, pin, or contract. Leave the Unit no-op.
            if (id == PackId.FrameInterp || id == PackId.Track) return
            if (!file.isFile || !verifiedModel(id, file)) {
                releaseEngine(id)
                return
            }
            when (id) {
                PackId.SuperRes -> bindSuperRes(file)
                PackId.Inpaint -> bindInpaint(file)
                PackId.Denoise -> bindDenoise(file)
                PackId.FrameInterp,
                PackId.Track -> Unit
            }
        } finally {
            PackRuntime.notifyEngineChanged()
        }
    }

    /** Ready + recorded SHA-256 (+ catalog expected pin). Download pins are applied before Ready. */
    private fun verifiedModel(id: PackId, file: File): Boolean {
        val manager = PackRuntime.manager() ?: return false
        return modelFileVerified(
            manager.state(id),
            file,
            PackCatalog.spec(id).expectedSha256
        )
    }

    /** Contract or checksum miss: drop any previous interpreter and tell the registrar to release. */
    private fun releaseEngine(id: PackId, opened: InterpreterApi? = null) {
        val notify = synchronized(lock) {
            val previous = engines.remove(id)
            if (previous != null && previous !== opened) {
                runCatching { previous.close() }
            }
            opened?.let { runCatching { it.close() } }
            sink
        }
        notify?.invoke(id, null)
    }

    private fun floatIo(interpreter: InterpreterApi): Boolean =
        interpreter.getInputTensor(0).dataType() == DataType.FLOAT32 &&
            interpreter.getOutputTensor(0).dataType() == DataType.FLOAT32

    private fun bindSuperRes(file: File) {
        val interpreter = openInterpreter(file) ?: run {
            releaseEngine(PackId.SuperRes)
            return
        }
        val accepted = runCatching {
            val inShape = interpreter.getInputTensor(0).shape()
            if ((inShape.getOrNull(1) ?: 0) <= 0 || (inShape.getOrNull(2) ?: 0) <= 0) {
                interpreter.resizeInput(0, intArrayOf(1, 50, 50, 3))
                interpreter.allocateTensors()
            }
            floatIo(interpreter) &&
                PackModelContract.superResScale(
                    interpreter.getInputTensor(0).shape(),
                    interpreter.getOutputTensor(0).shape()
                ) != null
        }.getOrDefault(false)
        if (!accepted) {
            releaseEngine(PackId.SuperRes, interpreter)
            return
        }
        val engine = NeuralEngine.SuperRes { bitmap, requested ->
            runCatching { superRes(bitmap, requested) }.getOrNull()
        }
        val notify = synchronized(lock) {
            engines.remove(PackId.SuperRes)?.close()
            engines[PackId.SuperRes] = interpreter
            sink
        }
        notify?.invoke(PackId.SuperRes, engine)
    }

    private fun bindInpaint(file: File) {
        val interpreter = openInterpreter(file) ?: run {
            releaseEngine(PackId.Inpaint)
            return
        }
        val accepted = runCatching {
            prepareInpaintShapes(interpreter)
            floatIo(interpreter) && inpaintContractMatches(interpreter)
        }.getOrDefault(false)
        if (!accepted) {
            releaseEngine(PackId.Inpaint, interpreter)
            return
        }
        val engine = NeuralEngine.Inpaint { bitmap, mask ->
            runCatching { inpaint(bitmap, mask) }.getOrNull()
        }
        val notify = synchronized(lock) {
            engines.remove(PackId.Inpaint)?.close()
            engines[PackId.Inpaint] = interpreter
            sink
        }
        notify?.invoke(PackId.Inpaint, engine)
    }

    private fun bindDenoise(file: File) {
        val interpreter = openInterpreter(file) ?: run {
            releaseEngine(PackId.Denoise)
            return
        }
        val accepted = runCatching {
            prepareDenoiseShape(interpreter)
            floatIo(interpreter) &&
                PackModelContract.isDenoiseSameRgb(
                    interpreter.getInputTensor(0).shape(),
                    interpreter.getOutputTensor(0).shape()
                )
        }.getOrDefault(false)
        if (!accepted) {
            releaseEngine(PackId.Denoise, interpreter)
            return
        }
        val engine = NeuralEngine.Denoise { bitmap, amount ->
            runCatching { denoise(bitmap, amount) }.getOrNull()
        }
        val notify = synchronized(lock) {
            engines.remove(PackId.Denoise)?.close()
            engines[PackId.Denoise] = interpreter
            sink
        }
        notify?.invoke(PackId.Denoise, engine)
    }

    private fun inpaintContractMatches(interpreter: InterpreterApi): Boolean {
        val out = interpreter.getOutputTensor(0).shape()
        return when (interpreter.inputTensorCount) {
            1 -> PackModelContract.isInpaintRgbMask(interpreter.getInputTensor(0).shape(), out)
            2 -> PackModelContract.isInpaintRgbAndMask(
                interpreter.getInputTensor(0).shape(),
                interpreter.getInputTensor(1).shape(),
                out
            )
            else -> false
        }
    }

    private fun prepareInpaintShapes(interpreter: InterpreterApi) {
        when (interpreter.inputTensorCount) {
            1 -> {
                val shape = interpreter.getInputTensor(0).shape()
                if ((shape.getOrNull(1) ?: 0) <= 0 || (shape.getOrNull(2) ?: 0) <= 0) {
                    interpreter.resizeInput(0, intArrayOf(1, MAX_WORK_EDGE, MAX_WORK_EDGE, 4))
                    interpreter.allocateTensors()
                }
            }
            2 -> {
                val image = interpreter.getInputTensor(0).shape()
                if ((image.getOrNull(1) ?: 0) <= 0 || (image.getOrNull(2) ?: 0) <= 0) {
                    interpreter.resizeInput(0, intArrayOf(1, MAX_WORK_EDGE, MAX_WORK_EDGE, 3))
                    val mask = interpreter.getInputTensor(1).shape()
                    if (mask.size == 3) {
                        interpreter.resizeInput(1, intArrayOf(1, MAX_WORK_EDGE, MAX_WORK_EDGE))
                    } else {
                        interpreter.resizeInput(1, intArrayOf(1, MAX_WORK_EDGE, MAX_WORK_EDGE, 1))
                    }
                    interpreter.allocateTensors()
                }
            }
        }
    }

    private fun prepareDenoiseShape(interpreter: InterpreterApi) {
        val shape = interpreter.getInputTensor(0).shape()
        if (PackModelContract.isNchwRgb(shape)) return
        if ((shape.getOrNull(1) ?: 0) <= 0 || (shape.getOrNull(2) ?: 0) <= 0) {
            interpreter.resizeInput(0, intArrayOf(1, MAX_WORK_EDGE, MAX_WORK_EDGE, 3))
            interpreter.allocateTensors()
        }
    }

    private fun inpaint(src: Bitmap, mask: BooleanArray): Bitmap? {
        if (mask.size != src.width * src.height) return null
        val interpreter = synchronized(lock) { engines[PackId.Inpaint] } ?: return null
        val outShape = interpreter.getOutputTensor(0).shape()
        val modelH = outShape[1]
        val modelW = outShape[2]
        if (modelH <= 0 || modelW <= 0) return null
        val work = capLongEdge(src, MAX_WORK_EDGE)
        val workMask = if (work === src) {
            mask
        } else {
            scaleMask(mask, src.width, src.height, work.width, work.height)
        }
        val sized = if (work.width != modelW || work.height != modelH) {
            Bitmap.createScaledBitmap(work, modelW, modelH, true)
        } else {
            work
        }
        val sizedMask = if (sized.width != work.width || sized.height != work.height) {
            scaleMask(workMask, work.width, work.height, sized.width, sized.height)
        } else {
            workMask
        }
        try {
            val generated = when (interpreter.inputTensorCount) {
                1 -> inferInpaintFour(interpreter, sized, sizedMask, modelH, modelW)
                2 -> inferInpaintDual(interpreter, sized, sizedMask, modelH, modelW)
                else -> return null
            }
            val full = if (generated.width == src.width && generated.height == src.height) {
                generated
            } else {
                Bitmap.createScaledBitmap(generated, src.width, src.height, true).also {
                    if (it !== generated) generated.recycle()
                }
            }
            val stamped = stampMasked(src, full, mask)
            if (full !== stamped && !full.isRecycled) full.recycle()
            return stamped
        } finally {
            if (sized !== work && !sized.isRecycled) sized.recycle()
            if (work !== src && !work.isRecycled) work.recycle()
        }
    }

    private fun inferInpaintFour(
        interpreter: InterpreterApi,
        tile: Bitmap,
        mask: BooleanArray,
        h: Int,
        w: Int
    ): Bitmap {
        val input = Array(1) { Array(h) { Array(w) { FloatArray(4) } } }
        fillRgb(tile, input[0])
        var i = 0
        for (row in 0 until h) {
            for (col in 0 until w) {
                input[0][row][col][3] = if (mask.getOrElse(i) { false }) 1f else 0f
                i++
            }
        }
        val output = Array(1) { Array(h) { Array(w) { FloatArray(3) } } }
        interpreter.run(input, output)
        return bitmapFromRgb(output[0], h, w)
    }

    private fun inferInpaintDual(
        interpreter: InterpreterApi,
        tile: Bitmap,
        mask: BooleanArray,
        h: Int,
        w: Int
    ): Bitmap {
        val rgb = Array(1) { Array(h) { Array(w) { FloatArray(3) } } }
        fillRgb(tile, rgb[0])
        val maskShape = interpreter.getInputTensor(1).shape()
        val maskInput: Any = if (maskShape.size == 3) {
            Array(1) { Array(h) { FloatArray(w) } }.also { plane ->
                var i = 0
                for (row in 0 until h) {
                    for (col in 0 until w) {
                        plane[0][row][col] = if (mask.getOrElse(i) { false }) 1f else 0f
                        i++
                    }
                }
            }
        } else {
            Array(1) { Array(h) { Array(w) { FloatArray(1) } } }.also { vol ->
                var i = 0
                for (row in 0 until h) {
                    for (col in 0 until w) {
                        vol[0][row][col][0] = if (mask.getOrElse(i) { false }) 1f else 0f
                        i++
                    }
                }
            }
        }
        val output = Array(1) { Array(h) { Array(w) { FloatArray(3) } } }
        val outputs = HashMap<Int, Any>()
        outputs[0] = output
        interpreter.runForMultipleInputsOutputs(arrayOf(rgb, maskInput), outputs)
        return bitmapFromRgb(output[0], h, w)
    }

    private fun denoise(src: Bitmap, amount: Float): Bitmap? {
        val interpreter = synchronized(lock) { engines[PackId.Denoise] } ?: return null
        val inShape = interpreter.getInputTensor(0).shape()
        val nchw = PackModelContract.isNchwRgb(inShape)
        val modelH = if (nchw) inShape[2] else inShape[1]
        val modelW = if (nchw) inShape[3] else inShape[2]
        if (modelH <= 0 || modelW <= 0) return null
        val cap = max(modelH, modelW)
        val work = capLongEdge(src, cap)
        val sized = if (work.width != modelW || work.height != modelH) {
            Bitmap.createScaledBitmap(work, modelW, modelH, true)
        } else {
            work
        }
        try {
            val restored = if (nchw) {
                inferRgbNchw(interpreter, sized, modelH, modelW)
            } else {
                inferRgb(interpreter, sized, modelH, modelW, modelH, modelW)
            }
            val full = if (restored.width == src.width && restored.height == src.height) {
                restored
            } else {
                Bitmap.createScaledBitmap(restored, src.width, src.height, true).also {
                    if (it !== restored) restored.recycle()
                }
            }
            val blended = blend(src, full, amount.coerceIn(0f, 1f))
            if (full !== blended && !full.isRecycled) full.recycle()
            return blended
        } finally {
            if (sized !== work && !sized.isRecycled) sized.recycle()
            if (work !== src && !work.isRecycled) work.recycle()
        }
    }

    private fun inferRgbNchw(interpreter: InterpreterApi, tile: Bitmap, h: Int, w: Int): Bitmap {
        val input = Array(1) { Array(3) { Array(h) { FloatArray(w) } } }
        val px = IntArray(w * h)
        tile.getPixels(px, 0, w, 0, 0, w, h)
        var i = 0
        for (row in 0 until h) {
            for (col in 0 until w) {
                val c = px[i++]
                input[0][0][row][col] = ((c shr 16) and 0xff) / 255f
                input[0][1][row][col] = ((c shr 8) and 0xff) / 255f
                input[0][2][row][col] = (c and 0xff) / 255f
            }
        }
        val output = Array(1) { Array(3) { Array(h) { FloatArray(w) } } }
        interpreter.run(input, output)
        val outPx = IntArray(w * h)
        i = 0
        for (row in 0 until h) {
            for (col in 0 until w) {
                val r = (output[0][0][row][col].coerceIn(0f, 1f) * 255f).toInt()
                val g = (output[0][1][row][col].coerceIn(0f, 1f) * 255f).toInt()
                val b = (output[0][2][row][col].coerceIn(0f, 1f) * 255f).toInt()
                outPx[i++] = (0xff shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(outPx, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun fillRgb(tile: Bitmap, dest: Array<Array<FloatArray>>) {
        val h = dest.size
        val w = dest[0].size
        val px = IntArray(w * h)
        tile.getPixels(px, 0, w, 0, 0, w, h)
        var i = 0
        for (row in 0 until h) {
            for (col in 0 until w) {
                val c = px[i++]
                dest[row][col][0] = ((c shr 16) and 0xff) / 255f
                dest[row][col][1] = ((c shr 8) and 0xff) / 255f
                dest[row][col][2] = (c and 0xff) / 255f
            }
        }
    }

    private fun bitmapFromRgb(rgb: Array<Array<FloatArray>>, h: Int, w: Int): Bitmap {
        val outPx = IntArray(w * h)
        var i = 0
        for (row in 0 until h) {
            for (col in 0 until w) {
                val r = (rgb[row][col][0].coerceIn(0f, 1f) * 255f).toInt()
                val g = (rgb[row][col][1].coerceIn(0f, 1f) * 255f).toInt()
                val b = (rgb[row][col][2].coerceIn(0f, 1f) * 255f).toInt()
                outPx[i++] = (0xff shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(outPx, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun scaleMask(
        mask: BooleanArray,
        srcW: Int,
        srcH: Int,
        dstW: Int,
        dstH: Int
    ): BooleanArray {
        val out = BooleanArray(dstW * dstH)
        if (srcW <= 0 || srcH <= 0) return out
        for (y in 0 until dstH) {
            val sy = (y * srcH / dstH).coerceIn(0, srcH - 1)
            for (x in 0 until dstW) {
                val sx = (x * srcW / dstW).coerceIn(0, srcW - 1)
                out[y * dstW + x] = mask[sy * srcW + sx]
            }
        }
        return out
    }

    private fun stampMasked(src: Bitmap, generated: Bitmap, mask: BooleanArray): Bitmap {
        val sw = src.width
        val sh = src.height
        val gw = generated.width
        val gh = generated.height
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val srcPx = IntArray(sw * sh)
        val genPx = IntArray(gw * gh)
        src.getPixels(srcPx, 0, sw, 0, 0, sw, sh)
        generated.getPixels(genPx, 0, gw, 0, 0, gw, gh)
        for (y in 0 until sh) {
            for (x in 0 until sw) {
                val i = y * sw + x
                if (!mask.getOrElse(i) { false }) continue
                val gx = (x * gw / sw).coerceIn(0, gw - 1)
                val gy = (y * gh / sh).coerceIn(0, gh - 1)
                srcPx[i] = genPx[gy * gw + gx]
            }
        }
        out.setPixels(srcPx, 0, sw, 0, 0, sw, sh)
        return out
    }

    private fun blend(src: Bitmap, restored: Bitmap, amount: Float): Bitmap {
        if (amount >= 0.995f) return restored
        if (amount <= 0.005f) return src
        val w = src.width
        val h = src.height
        val a = amount
        val ia = 1f - a
        val srcPx = IntArray(w * h)
        val dstPx = IntArray(w * h)
        src.getPixels(srcPx, 0, w, 0, 0, w, h)
        restored.getPixels(dstPx, 0, w, 0, 0, w, h)
        for (i in srcPx.indices) {
            val s = srcPx[i]
            val d = dstPx[i]
            val r = (((s shr 16) and 0xff) * ia + ((d shr 16) and 0xff) * a).toInt()
            val g = (((s shr 8) and 0xff) * ia + ((d shr 8) and 0xff) * a).toInt()
            val b = ((s and 0xff) * ia + (d and 0xff) * a).toInt()
            dstPx[i] = (0xff shl 24) or (r shl 16) or (g shl 8) or b
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(dstPx, 0, w, 0, 0, w, h)
        return out
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
