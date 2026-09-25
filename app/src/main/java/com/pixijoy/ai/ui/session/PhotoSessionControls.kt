package com.pixijoy.ai.ui.session

import android.graphics.Bitmap
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pixijoy.photo.BundledLooks
import com.pixijoy.photo.ColorIntent
import com.pixijoy.photo.ExportFormat
import com.pixijoy.photo.LiveGrade
import com.pixijoy.photo.MaskFill
import com.pixijoy.photo.MaskStroke
import com.pixijoy.photo.PhotoOp
import com.pixijoy.photo.PhotoPreset
import com.pixijoy.photo.SelectiveHslRange
import com.pixijoy.photo.SkyReplacePresets
import com.pixijoy.photo.SkyReplaceTarget
import com.pixijoy.photo.Stamp

/**
 * Slider, export, and preview bitmap state for one photo session.
 * Owned by [PhotoSessionViewModel] so [PhotoSessionScreen] does not keep it in composition.
 * [reset] matches the old `remember` defaults on each screen entry, including rotation.
 */
internal class PhotoSessionControls(toolKey: String) {
    var preview by mutableStateOf<Bitmap?>(null)
    var histogram by mutableStateOf<IntArray?>(null)
    var crop by mutableStateOf(PhotoOp.Crop(0.08f, 0.08f, 0.92f, 0.92f))
    var exposure by mutableStateOf(0f)
    var contrast by mutableStateOf(1f)
    var hue by mutableStateOf(0f)
    var saturation by mutableStateOf(1f)
    var selectiveRange by mutableStateOf(SelectiveHslRange.Reds)
    var selectiveHue by mutableStateOf(0f)
    var selectiveSat by mutableStateOf(1f)
    var selectiveLight by mutableStateOf(0f)
    var selectiveLivePreview by mutableStateOf<Bitmap?>(null)
    var curvesLivePreview by mutableStateOf<Bitmap?>(null)
    var lastMaskSnapshot by mutableStateOf<Pair<List<MaskStroke>, MaskFill>?>(null)
    var curveYs by mutableStateOf(floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f))
    var lookKey by mutableStateOf(BundledLooks.ORIGINAL)
    var importedLookLabel by mutableStateOf<String?>(null)
    var lutLivePreview by mutableStateOf<Bitmap?>(null)
    val curvesShader =
        if (Build.VERSION.SDK_INT >= 33) RuntimeShader(LiveGrade.CURVES_AGSL) else null
    val detailShader =
        if (Build.VERSION.SDK_INT >= 33) RuntimeShader(LiveGrade.DETAIL_AGSL) else null
    var sharpen by mutableStateOf(0f)
    var vignette by mutableStateOf(0f)
    var blurRadius by mutableStateOf(0f)
    var expand by mutableStateOf(0.12f)
    var brush by mutableStateOf(0.05f)
    var maskReveal by mutableStateOf(false)
    var pendingStamps by mutableStateOf(listOf<Stamp>())
    var presets by mutableStateOf(listOf<PhotoPreset>())
    var lastStamp by mutableStateOf<Stamp?>(null)
    var healSource by mutableStateOf<Pair<Float, Float>?>(null)
    var pickingHealSource by mutableStateOf(false)
    var collageCols by mutableStateOf(2)
    var text by mutableStateOf("PixiJoy")
    var placeX by mutableStateOf(0.5f)
    var placeY by mutableStateOf(0.82f)
    var format by mutableStateOf(defaultFormat(toolKey))
    var quality by mutableStateOf(92)
    var keepExif by mutableStateOf(false)
    var colorIntent by mutableStateOf(ColorIntent.Srgb)
    var showExport by mutableStateOf(false)
    var exportSuccessUri by mutableStateOf<String?>(null)
    var status by mutableStateOf<String?>(null)
    var statusIsError by mutableStateOf(false)
    var exporting by mutableStateOf(false)
    var busy by mutableStateOf(false)
    var compareSnapshot by mutableStateOf<Bitmap?>(null)
    var compareControlHeld by mutableStateOf(false)
    var compareCanvasHeld by mutableStateOf(false)
    var compareAccessible by mutableStateOf(false)
    var viewportState by mutableStateOf(PhotoViewportState())
    var viewportImageSize by mutableStateOf<Pair<Int, Int>?>(null)
    var lightFocus by mutableIntStateOf(0)
    var colorFocus by mutableIntStateOf(0)
    var curveFocus by mutableIntStateOf(2)
    var skyTarget by mutableStateOf(SkyReplaceTarget.Sky)
    var skyColor by mutableStateOf(SkyReplacePresets.BlueSky)
    private var strokeDraftMode: SessionMode? = null

    /**
     * Drops drafts that would paint or bake as the wrong tool.
     * Strokes stay parked when leaving Heal for Light so the same tool can resume;
     * they clear when another stroke tool (Fill / Erase) would apply them.
     */
    fun abandonModeDrafts(previous: SessionMode, next: SessionMode) {
        if (previous == next) return
        val strokes = setOf(SessionMode.Heal, SessionMode.Fill, SessionMode.Erase)
        val spacing = strokes + SessionMode.Mask
        if (previous in strokes) {
            strokeDraftMode = if (pendingStamps.isEmpty()) null else previous
        }
        if (next in strokes && strokeDraftMode != null && strokeDraftMode != next) {
            pendingStamps = emptyList()
            lastStamp = null
            strokeDraftMode = null
        }
        if (previous in spacing && previous != next) lastStamp = null
        if (next != SessionMode.Heal) pickingHealSource = false
        if (next == SessionMode.Erase && previous != SessionMode.Erase) healSource = null
    }

    fun reset(toolKey: String) {
        releaseBitmaps()
        histogram = null
        crop = PhotoOp.Crop(0.08f, 0.08f, 0.92f, 0.92f)
        exposure = 0f
        contrast = 1f
        hue = 0f
        saturation = 1f
        selectiveRange = SelectiveHslRange.Reds
        selectiveHue = 0f
        selectiveSat = 1f
        selectiveLight = 0f
        lastMaskSnapshot = null
        curveYs = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        lookKey = BundledLooks.ORIGINAL
        importedLookLabel = null
        sharpen = 0f
        vignette = 0f
        blurRadius = 0f
        expand = 0.12f
        brush = 0.05f
        maskReveal = false
        pendingStamps = emptyList()
        presets = emptyList()
        lastStamp = null
        healSource = null
        pickingHealSource = false
        collageCols = 2
        text = "PixiJoy"
        placeX = 0.5f
        placeY = 0.82f
        format = defaultFormat(toolKey)
        quality = 92
        keepExif = false
        colorIntent = ColorIntent.Srgb
        showExport = false
        exportSuccessUri = null
        status = null
        statusIsError = false
        exporting = false
        busy = false
        compareControlHeld = false
        compareCanvasHeld = false
        compareAccessible = false
        viewportState = PhotoViewportState()
        viewportImageSize = null
        lightFocus = 0
        colorFocus = 0
        curveFocus = 2
        skyTarget = SkyReplaceTarget.Sky
        skyColor = SkyReplacePresets.BlueSky
        strokeDraftMode = null
    }

    fun releaseBitmaps() {
        recycle(preview)
        recycle(compareSnapshot)
        recycle(lutLivePreview)
        recycle(selectiveLivePreview)
        recycle(curvesLivePreview)
        preview = null
        compareSnapshot = null
        lutLivePreview = null
        selectiveLivePreview = null
        curvesLivePreview = null
    }

    private fun recycle(bitmap: Bitmap?) {
        if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
    }

    private companion object {
        fun defaultFormat(toolKey: String): ExportFormat =
            if (toolKey == "cutout" || toolKey == "stickers") ExportFormat.PNG else ExportFormat.JPEG
    }
}
