package com.pixijoy.ai.ui.session

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.pixijoy.ai.R
import com.pixijoy.ai.export.PhotoExportWorker
import com.pixijoy.photo.BundledLooks
import com.pixijoy.photo.ImportedLuts
import com.pixijoy.photo.LayeredOp
import com.pixijoy.photo.LiveGrade
import com.pixijoy.photo.PhotoOp
import com.pixijoy.photo.PhotoPreviewPolicy
import com.pixijoy.photo.applyCurves
import com.pixijoy.photo.applyLut
import com.pixijoy.photo.applySelectiveHsl
import com.pixijoy.photo.hasVisibleEffect
import com.pixijoy.photo.isIdentity
import com.pixijoy.photo.lumaHistogram
import com.pixijoy.photo.withBackgroundSeed
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Slider commit and preview baking for a photo session.
 * [PhotoSessionScreen] observes [PhotoSessionControls] and calls this; it does not bake.
 */
internal class PhotoSessionPreview(
    private val controls: PhotoSessionControls
) {
    val comparing: Boolean
        get() = controls.compareControlHeld || controls.compareCanvasHeld || controls.compareAccessible

    fun hasLiveDelta(mode: SessionMode): Boolean {
        val c = controls
        return (mode == SessionMode.Adjust &&
            (!LiveGrade.isAdjustIdentity(c.exposure, c.contrast, c.hue, c.saturation) ||
                c.sharpen > 0.01f || c.vignette > 0.01f || c.blurRadius > 0.5f)) ||
            (mode == SessionMode.Curves && !LiveGrade.curvesIdentity(c.curveYs)) ||
            (mode == SessionMode.Looks && (
                (!BundledLooks.isIdentity(c.lookKey) && !ImportedLuts.isImported(c.lookKey)) ||
                    c.lutLivePreview != null
                )) ||
            (mode == SessionMode.SelectiveHsl && c.selectiveLivePreview != null) ||
            (mode == SessionMode.Curves && c.curvesLivePreview != null)
    }

    fun canCompare(mode: SessionMode, layers: List<LayeredOp>): Boolean {
        val snap = controls.compareSnapshot
        val hasBakedSnapshot = snap != null && !snap.isRecycled
        val hasBakedDelta = layers.any { it.hasVisibleEffect() }
        return hasLiveDelta(mode) || (hasBakedDelta && hasBakedSnapshot)
    }

    fun releaseCompareHolds(allowed: Boolean) {
        if (allowed) return
        controls.compareControlHeld = false
        controls.compareCanvasHeld = false
        controls.compareAccessible = false
    }

    fun setExposure(value: Float) { controls.exposure = value }
    fun setContrast(value: Float) { controls.contrast = value }
    fun setHue(value: Float) { controls.hue = value }
    fun setSaturation(value: Float) { controls.saturation = value }
    fun setSharpen(value: Float) { controls.sharpen = value }
    fun setVignette(value: Float) { controls.vignette = value }
    fun setBlur(value: Float) { controls.blurRadius = value }
    fun setCurveYs(value: FloatArray) { controls.curveYs = value }
    fun setLookKey(value: String) { controls.lookKey = value }
    fun setSelectiveRange(value: com.pixijoy.photo.SelectiveHslRange) { controls.selectiveRange = value }
    fun setSelectiveHue(value: Float) { controls.selectiveHue = value }
    fun setSelectiveSat(value: Float) { controls.selectiveSat = value }
    fun setSelectiveLight(value: Float) { controls.selectiveLight = value }

    fun applyGrade(push: (PhotoOp) -> Unit) {
        val c = controls
        if (!PhotoOp.Grade(c.exposure, c.contrast).isIdentity()) {
            push(PhotoOp.Grade(c.exposure, c.contrast))
        }
        if (!PhotoOp.Hsl(c.hue, c.saturation).isIdentity()) {
            push(PhotoOp.Hsl(c.hue, c.saturation))
        }
        c.exposure = 0f
        c.contrast = 1f
        c.hue = 0f
        c.saturation = 1f
        if (c.sharpen > 0.01f) push(PhotoOp.Sharpen(c.sharpen))
        if (c.vignette > 0.01f) push(PhotoOp.Vignette(c.vignette))
        if (c.blurRadius > 0.5f) push(PhotoOp.Blur(c.blurRadius.toInt().coerceIn(1, 8)))
        c.sharpen = 0f
        c.vignette = 0f
        c.blurRadius = 0f
    }

    fun applyCurves(ys: FloatArray, push: (PhotoOp) -> Unit) {
        if (!PhotoOp.Curves(ys).isIdentity()) push(PhotoOp.Curves(ys.copyOf()))
        controls.curveYs = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
    }

    fun applySelective(push: (PhotoOp) -> Unit) {
        val c = controls
        val op = PhotoOp.SelectiveHsl(c.selectiveRange, c.selectiveHue, c.selectiveSat, c.selectiveLight)
        if (!op.isIdentity()) {
            push(op)
            c.selectiveHue = 0f
            c.selectiveSat = 1f
            c.selectiveLight = 0f
        }
    }

    fun applyLook(key: String, push: (PhotoOp) -> Unit) {
        val imported = ImportedLuts.isImported(key)
        if (imported || !BundledLooks.isIdentity(key)) {
            push(PhotoOp.Lut(key))
            controls.lookKey = BundledLooks.ORIGINAL
            controls.importedLookLabel = null
        }
    }

    fun acceptImportedLut(key: String, label: String, status: String) {
        controls.lookKey = key
        controls.importedLookLabel = label
        controls.status = status
        controls.statusIsError = false
    }

    fun reportError(message: String) {
        controls.status = message
        controls.statusIsError = true
    }

    fun onExport(phase: PhotoExportPhase, uri: String?, message: String) {
        val c = controls
        when (phase) {
            PhotoExportPhase.Running -> {
                c.exporting = true
                if (c.status == null) {
                    c.status = message
                    c.statusIsError = false
                }
            }
            PhotoExportPhase.Succeeded -> if (c.exporting) {
                c.exporting = false
                c.exportSuccessUri = uri
                c.showExport = true
                c.status = message
                c.statusIsError = false
            }
            PhotoExportPhase.Failed -> if (c.exporting) {
                c.exporting = false
                c.status = message
                c.statusIsError = true
            }
        }
    }

    suspend fun refreshLut(mode: SessionMode, projectDir: File, renderFailed: String) {
        val src = controls.preview
        val lookKey = controls.lookKey
        if (mode != SessionMode.Looks || src == null || src.isRecycled || !ImportedLuts.isImported(lookKey)) {
            recycleLive(controls.lutLivePreview)
            controls.lutLivePreview = null
            return
        }
        val baked = try {
            withContext(Dispatchers.Default) { applyLut(src, lookKey, projectDir) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            recycleLive(controls.lutLivePreview)
            controls.lutLivePreview = null
            reportError(renderFailed)
            return
        }
        val previous = controls.lutLivePreview
        if (previous != null && !previous.isRecycled && previous !== baked && previous !== src) {
            previous.recycle()
        }
        controls.lutLivePreview = if (baked !== src) baked else null
    }

    suspend fun refreshSelective(mode: SessionMode, renderFailed: String) {
        val src = controls.preview
        val c = controls
        if (mode != SessionMode.SelectiveHsl || src == null || src.isRecycled) {
            recycleLive(c.selectiveLivePreview)
            c.selectiveLivePreview = null
            return
        }
        val op = PhotoOp.SelectiveHsl(c.selectiveRange, c.selectiveHue, c.selectiveSat, c.selectiveLight)
        if (op.isIdentity()) {
            recycleLive(c.selectiveLivePreview)
            c.selectiveLivePreview = null
            return
        }
        val baked = try {
            applySelectiveHsl(src, c.selectiveRange, c.selectiveHue, c.selectiveSat, c.selectiveLight)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            recycleLive(c.selectiveLivePreview)
            c.selectiveLivePreview = null
            reportError(renderFailed)
            return
        }
        val previous = c.selectiveLivePreview
        if (previous != null && !previous.isRecycled && previous !== baked) previous.recycle()
        c.selectiveLivePreview = if (baked !== src) baked else null
    }

    suspend fun refreshCurves(mode: SessionMode, renderFailed: String) {
        val src = controls.preview
        val useCpuCurves = Build.VERSION.SDK_INT < 33
        if (mode != SessionMode.Curves || !useCpuCurves || src == null || src.isRecycled ||
            LiveGrade.curvesIdentity(controls.curveYs)
        ) {
            recycleLive(controls.curvesLivePreview)
            controls.curvesLivePreview = null
            return
        }
        val baked = try {
            withContext(Dispatchers.Default) { applyCurves(src, controls.curveYs) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            recycleLive(controls.curvesLivePreview)
            controls.curvesLivePreview = null
            reportError(renderFailed)
            return
        }
        val previous = controls.curvesLivePreview
        if (previous != null && !previous.isRecycled && previous !== baked) previous.recycle()
        controls.curvesLivePreview = if (baked !== src) baked else null
    }

    suspend fun render(
        sources: List<String>,
        layers: List<LayeredOp>,
        maxEdge: Int,
        renderLayers: suspend (List<String>, List<LayeredOp>, Int) -> Bitmap,
        renderFailed: String
    ) {
        controls.busy = true
        var beforeBake: Bitmap? = null
        var snapshotCommitted = false
        try {
            val hasBakedDelta = layers.any { it.hasVisibleEffect() }
            val current = controls.preview
            beforeBake = when {
                !hasBakedDelta -> null
                current != null -> current.copy(current.config ?: Bitmap.Config.ARGB_8888, false)
                else -> {
                    val lastEffect = layers.indexOfLast { it.hasVisibleEffect() }
                    renderLayers(sources, layers.take(lastEffect), maxEdge)
                }
            }
            val next = renderLayers(sources, layers, maxEdge)
            if (beforeBake != null) {
                recycleLive(controls.compareSnapshot)
                controls.compareSnapshot = beforeBake
                snapshotCommitted = true
            } else if (!hasBakedDelta) {
                recycleLive(controls.compareSnapshot)
                controls.compareSnapshot = null
            }
            val previous = controls.preview
            controls.preview = next
            if (previous !== next) recycleLive(previous)
            controls.histogram = lumaHistogram(next)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            reportError(renderFailed)
        } finally {
            if (!snapshotCommitted) {
                val bake = beforeBake
                if (bake != null && !bake.isRecycled && bake !== controls.preview) bake.recycle()
            }
            controls.busy = false
        }
    }

    private fun recycleLive(bitmap: Bitmap?) {
        if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
    }
}

internal enum class PhotoExportPhase { Running, Succeeded, Failed }

@Composable
internal fun PhotoPreviewEffects(
    viewModel: PhotoSessionViewModel,
    projectId: String,
    sources: List<String>,
    layers: List<LayeredOp>,
    projectDir: File,
    renderLayers: suspend (List<String>, List<LayeredOp>, Int) -> Bitmap
) {
    val context = LocalContext.current
    val controls = viewModel.controls
    val mode = viewModel.toolMode
    val renderFailed = context.getString(R.string.studio_photo_render_failed)
    val lookKey = controls.lookKey
    val preview = controls.preview
    val selectiveRange = controls.selectiveRange
    val selectiveHue = controls.selectiveHue
    val selectiveSat = controls.selectiveSat
    val selectiveLight = controls.selectiveLight
    val curveYs = controls.curveYs
    LaunchedEffect(mode, lookKey, preview, projectDir) {
        viewModel.preview.refreshLut(mode, projectDir, renderFailed)
    }
    LaunchedEffect(mode, projectId) {
        // Hub opens are already seeded. This covers a later switch into Layers.
        viewModel.seedLayers(projectId)
    }
    LaunchedEffect(mode, selectiveRange, selectiveHue, selectiveSat, selectiveLight, preview) {
        viewModel.preview.refreshSelective(mode, renderFailed)
    }
    LaunchedEffect(mode, curveYs, preview) {
        viewModel.preview.refreshCurves(mode, renderFailed)
    }
    val inspectDetail = mode == SessionMode.Heal || mode == SessionMode.Mask || mode == SessionMode.Fill
    val previewMaxEdge = PhotoPreviewPolicy.previewMaxEdge(controls.viewportState.zoom, inspectDetail)
    LaunchedEffect(sources, layers, previewMaxEdge) {
        viewModel.preview.render(
            sources = sources,
            layers = layers,
            maxEdge = previewMaxEdge,
            renderLayers = { src, ops, edge ->
                withContext(Dispatchers.Default) { renderLayers(src, ops, edge) }
            },
            renderFailed = renderFailed
        )
    }
    DisposableEffect(Unit) {
        onDispose { controls.releaseBitmaps() }
    }
    val workManager = remember { WorkManager.getInstance(context) }
    val exportInfos by workManager
        .getWorkInfosForUniqueWorkFlow(PhotoExportWorker.uniqueName(projectId))
        .collectAsState(initial = emptyList())
    LaunchedEffect(exportInfos) {
        val info = exportInfos.firstOrNull() ?: return@LaunchedEffect
        when (info.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED ->
                viewModel.preview.onExport(
                    PhotoExportPhase.Running,
                    uri = null,
                    message = context.getString(R.string.studio_export_background)
                )
            WorkInfo.State.SUCCEEDED ->
                viewModel.preview.onExport(
                    PhotoExportPhase.Succeeded,
                    uri = info.outputData.getString(PhotoExportWorker.KEY_URI),
                    message = context.getString(R.string.studio_export_ok)
                )
            WorkInfo.State.FAILED ->
                viewModel.preview.onExport(
                    PhotoExportPhase.Failed,
                    uri = null,
                    message = (info.outputData.getString(PhotoExportWorker.KEY_ERROR)
                        ?: context.getString(R.string.studio_export_failed)) +
                        "\n" + context.getString(R.string.studio_export_failed_help)
                )
            else -> Unit
        }
    }
    val canCompare = viewModel.preview.canCompare(mode, layers)
    LaunchedEffect(canCompare) {
        viewModel.preview.releaseCompareHolds(canCompare)
    }
}
