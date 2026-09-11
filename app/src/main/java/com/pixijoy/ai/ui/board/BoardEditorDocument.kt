package com.pixijoy.ai.ui.board

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import com.pixijoy.ai.ui.board.components.BoardColorAdjust
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import java.util.ArrayDeque

/**
 * Snapseed-class undo/redo for Flow 15 / 26 — bitmap snapshots + adjust floats.
 * Keeps a bounded stack to limit memory.
 */
class BoardEditHistory(private val maxDepth: Int = 12) {
    data class Snapshot(
        val bitmap: Bitmap?,
        val brightness: Float,
        val contrast: Float,
        val saturation: Float,
        val warmth: Float,
        val sharpness: Float,
        val hue: Float = BoardColorAdjust.Neutral,
        val lightness: Float = BoardColorAdjust.Neutral,
        val shadows: Float = BoardColorAdjust.Neutral,
        val midtones: Float = BoardColorAdjust.Neutral,
        val highlights: Float = BoardColorAdjust.Neutral,
        val filterIndex: Int,
        val aspectIndex: Int,
        val textOverlay: String
    )

    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun clear() {
        recycleAll(undoStack)
        recycleAll(redoStack)
        undoStack.clear()
        redoStack.clear()
    }

    fun push(snapshot: Snapshot) {
        undoStack.addLast(copySnapshot(snapshot))
        while (undoStack.size > maxDepth) {
            recycleSnapshot(undoStack.removeFirst())
        }
        recycleAll(redoStack)
        redoStack.clear()
    }

    fun undo(current: Snapshot): Snapshot? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(copySnapshot(current))
        return undoStack.removeLast()
    }

    fun redo(current: Snapshot): Snapshot? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(copySnapshot(current))
        return redoStack.removeLast()
    }

    private fun copySnapshot(s: Snapshot): Snapshot = s.copy(
        bitmap = s.bitmap?.let { src ->
            if (src.isRecycled) null else src.copy(Bitmap.Config.ARGB_8888, false)
        }
    )

    private fun recycleAll(stack: ArrayDeque<Snapshot>) {
        while (stack.isNotEmpty()) recycleSnapshot(stack.removeFirst())
    }

    private fun recycleSnapshot(s: Snapshot) {
        val b = s.bitmap ?: return
        if (!b.isRecycled) runCatching { b.recycle() }
    }
}

/** Shared working document for Flow 15 / 26 editor. */
object BoardEditorDocument {
    private val history = BoardEditHistory()

    @Volatile
    var workingBitmap: Bitmap? = null
        private set

    var brightness: Float = BoardColorAdjust.Neutral
    var contrast: Float = BoardColorAdjust.Neutral
    var saturation: Float = BoardColorAdjust.Neutral
    var warmth: Float = BoardColorAdjust.Neutral
    var sharpness: Float = BoardColorAdjust.Neutral
    var hue: Float = BoardColorAdjust.Neutral
    var lightness: Float = BoardColorAdjust.Neutral
    var shadows: Float = BoardColorAdjust.Neutral
    var midtones: Float = BoardColorAdjust.Neutral
    var highlights: Float = BoardColorAdjust.Neutral
    /** Structure vignette amount 0..20 (Neutral 0 = off). Applied via [CreateExtraPipeline.applyStructure] on Apply. */
    var vignette: Float = 0f
    /** Structure grain amount 0..20 (Neutral 0 = off). */
    var grain: Float = 0f
    var filterIndex: Int = 0
    var aspectIndex: Int = 1
    var textOverlay: String = "Good Vibes Only"
    var showAfter: Boolean = true
    var revision: Int = 0
        private set

    val canUndo: Boolean get() = history.canUndo
    val canRedo: Boolean get() = history.canRedo

    fun bump() {
        revision++
    }

    fun currentSnapshot(): BoardEditHistory.Snapshot = BoardEditHistory.Snapshot(
        bitmap = workingBitmap,
        brightness = brightness,
        contrast = contrast,
        saturation = saturation,
        warmth = warmth,
        sharpness = sharpness,
        hue = hue,
        lightness = lightness,
        shadows = shadows,
        midtones = midtones,
        highlights = highlights,
        filterIndex = filterIndex,
        aspectIndex = aspectIndex,
        textOverlay = textOverlay
    )

    fun applySnapshot(s: BoardEditHistory.Snapshot) {
        val old = workingBitmap
        workingBitmap = s.bitmap?.let { if (it.isRecycled) null else it.copy(Bitmap.Config.ARGB_8888, true) }
        if (old != null && old !== workingBitmap && !old.isRecycled) runCatching { old.recycle() }
        brightness = s.brightness
        contrast = s.contrast
        saturation = s.saturation
        warmth = s.warmth
        sharpness = s.sharpness
        hue = s.hue
        lightness = s.lightness
        shadows = s.shadows
        midtones = s.midtones
        highlights = s.highlights
        filterIndex = s.filterIndex
        aspectIndex = s.aspectIndex
        textOverlay = s.textOverlay
        bump()
    }

    fun pushHistory() {
        history.push(currentSnapshot())
        bump()
    }

    fun undo() {
        val prev = history.undo(currentSnapshot()) ?: return
        applySnapshot(prev)
    }

    fun redo() {
        val next = history.redo(currentSnapshot()) ?: return
        applySnapshot(next)
    }

    fun setWorkingBitmap(bitmap: Bitmap?, push: Boolean = false) {
        if (push) pushHistory()
        val old = workingBitmap
        workingBitmap = bitmap
        if (old != null && old !== bitmap && !old.isRecycled) runCatching { old.recycle() }
        bump()
    }

    fun ensureLoaded(context: Context, @androidx.annotation.DrawableRes fallbackRes: Int = BoardFlowArt.editorHero) {
        val current = workingBitmap
        if (current != null && current.isRecycled) {
            workingBitmap = null
        } else if (current != null) {
            return
        }
        val decoded = BoardMediaSession.decodeBitmap(context, maxSide = 1600)
            ?: android.graphics.BitmapFactory.decodeResource(context.resources, fallbackRes)
        if (decoded != null && !decoded.isRecycled) {
            val software = if (decoded.config == Bitmap.Config.ARGB_8888) {
                decoded
            } else {
                decoded.copy(Bitmap.Config.ARGB_8888, false)?.also {
                    if (it !== decoded && !decoded.isRecycled) runCatching { decoded.recycle() }
                } ?: decoded
            }
            workingBitmap = software
            history.clear()
            bump()
        }
    }

    fun loadFromDrawable(context: Context, @androidx.annotation.DrawableRes resId: Int, clearHistory: Boolean = true) {
        val decoded = android.graphics.BitmapFactory.decodeResource(context.resources, resId) ?: return
        val old = workingBitmap
        workingBitmap = decoded
        if (old != null && old !== decoded && !old.isRecycled) runCatching { old.recycle() }
        if (clearHistory) history.clear()
        bump()
    }

    fun resetAdjust(push: Boolean = true) {
        if (push) pushHistory()
        brightness = BoardColorAdjust.Neutral
        contrast = BoardColorAdjust.Neutral
        saturation = BoardColorAdjust.Neutral
        warmth = BoardColorAdjust.Neutral
        sharpness = BoardColorAdjust.Neutral
        hue = BoardColorAdjust.Neutral
        lightness = BoardColorAdjust.Neutral
        shadows = BoardColorAdjust.Neutral
        midtones = BoardColorAdjust.Neutral
        highlights = BoardColorAdjust.Neutral
        vignette = 0f
        grain = 0f
        filterIndex = 0
        bump()
    }

    fun rotate90(push: Boolean = true) {
        val src = workingBitmap ?: return
        if (push) pushHistory()
        val m = Matrix().apply { postRotate(90f) }
        val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        setWorkingBitmap(out, push = false)
    }

    fun flip(horizontal: Boolean, push: Boolean = true) {
        val src = workingBitmap ?: return
        if (push) pushHistory()
        val m = Matrix().apply {
            if (horizontal) postScale(-1f, 1f, src.width / 2f, src.height / 2f)
            else postScale(1f, -1f, src.width / 2f, src.height / 2f)
        }
        val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        setWorkingBitmap(out, push = false)
    }

    fun previewColorFilter() = BoardColorAdjust.colorFilter(
        brightness = brightness,
        contrast = contrast,
        saturation = saturation,
        warmth = warmth,
        hue = hue,
        lightness = lightness,
        shadows = shadows,
        midtones = midtones,
        highlights = highlights
    )

    fun bakeMatrix() = BoardColorAdjust.androidMatrix(
        brightness = brightness,
        contrast = contrast,
        saturation = saturation,
        warmth = warmth,
        hue = hue,
        lightness = lightness,
        shadows = shadows,
        midtones = midtones,
        highlights = highlights
    )

    /**
     * Bake live color sliders into [workingBitmap].
     * Uses [com.pixijoy.ai.creative.AdvancedEditEngine] for HSL + curves when provided.
     */
    fun bakePreviewGrade(
        engine: com.pixijoy.ai.creative.AdvancedEditEngine? = null
    ): Boolean {
        val src = workingBitmap ?: return false
        if (src.isRecycled) return false
        var graded: Bitmap = src
        if (engine != null) {
            val n = BoardColorAdjust.Neutral
            graded = engine.applyHsl(
                graded,
                com.pixijoy.ai.creative.HslAdjustments(
                    hue = ((hue - n) / n).coerceIn(-1f, 1f),
                    saturation = (saturation / n).coerceIn(0f, 2f),
                    lightness = ((lightness - n) / n).coerceIn(-1f, 1f)
                )
            )
            graded = engine.applyCurves(
                graded,
                com.pixijoy.ai.creative.CurveAdjustments(
                    shadows = ((shadows - n) / n).coerceIn(-1f, 1f),
                    midtones = ((midtones - n) / n).coerceIn(-1f, 1f),
                    highlights = ((highlights - n) / n).coerceIn(-1f, 1f)
                )
            )
        }
        val out = Bitmap.createBitmap(graded.width, graded.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(bakeMatrix())
        }
        Canvas(out).drawBitmap(graded, 0f, 0f, paint)
        if (graded !== src && !graded.isRecycled) runCatching { graded.recycle() }
        setWorkingBitmap(out, push = true)
        brightness = BoardColorAdjust.Neutral
        contrast = BoardColorAdjust.Neutral
        saturation = BoardColorAdjust.Neutral
        warmth = BoardColorAdjust.Neutral
        hue = BoardColorAdjust.Neutral
        lightness = BoardColorAdjust.Neutral
        shadows = BoardColorAdjust.Neutral
        midtones = BoardColorAdjust.Neutral
        highlights = BoardColorAdjust.Neutral
        bump()
        return true
    }
}
