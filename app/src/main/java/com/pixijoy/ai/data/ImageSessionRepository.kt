package com.pixijoy.ai.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory edit session plus a lightweight persisted source URI so process death
 * can reload the last photo into tools that call [restoreSourceUri].
 */
@Singleton
class ImageSessionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var sourceUri: Uri? = null
    var workingBitmap: Bitmap? = null
    var pendingMemeTop: String? = null
    var pendingMemeBottom: String? = null
    var pendingBackgroundPrompt: String? = null
    var sceneDescription: String? = null
    var photoEditExposure: Float? = null
    var photoEditContrast: Float? = null
    var pendingAgentPrompt: String? = null
    var pendingNlCommand: String? = null
    var pendingRegenerate: Boolean = false
    var pendingTrendPrompt: String? = null
    var pendingTemplateCategory: String? = null
    var resultBitmap: Bitmap? = null
    var resultType: SavedCreationType? = null
    var resultTitle: String = ""
    var resultCaptionTop: String? = null
    var resultCaptionBottom: String? = null
    var resultStickerCount: Int = 0

    init {
        prefs.getString(KEY_SOURCE_URI, null)?.let { sourceUri = Uri.parse(it) }
    }

    fun setPhotoEditPreset(exposure: Float, contrast: Float) {
        photoEditExposure = exposure
        photoEditContrast = contrast
    }

    fun setSession(uri: Uri, bitmap: Bitmap) {
        clearBitmapsOnly()
        sourceUri = uri
        workingBitmap = bitmap
        persistSourceUri(uri)
    }

    fun setAiHints(top: String?, bottom: String?, background: String? = null, scene: String? = null) {
        pendingMemeTop = top
        pendingMemeBottom = bottom
        pendingBackgroundPrompt = background
        sceneDescription = scene
    }

    fun setResult(
        bitmap: Bitmap,
        type: SavedCreationType,
        title: String,
        top: String? = null,
        bottom: String? = null,
        stickerCount: Int = 0
    ) {
        resultBitmap?.let { old ->
            if (old != bitmap) old.recycle()
        }
        resultBitmap = bitmap
        resultType = type
        resultTitle = title
        resultCaptionTop = top
        resultCaptionBottom = bottom
        resultStickerCount = stickerCount
    }

    fun clearResult() {
        resultBitmap?.recycle()
        resultBitmap = null
        resultType = null
        resultTitle = ""
        resultCaptionTop = null
        resultCaptionBottom = null
        resultStickerCount = 0
    }

    fun updateBitmap(bitmap: Bitmap) {
        workingBitmap?.let { old ->
            if (old != bitmap) old.recycle()
        }
        workingBitmap = bitmap
    }

    /** URI to reload after process death when [workingBitmap] is gone. */
    fun restoreSourceUri(): Uri? = sourceUri ?: prefs.getString(KEY_SOURCE_URI, null)?.let { Uri.parse(it) }

    fun clear() {
        clearBitmapsOnly()
        sourceUri = null
        pendingMemeTop = null
        pendingMemeBottom = null
        pendingBackgroundPrompt = null
        sceneDescription = null
        photoEditExposure = null
        photoEditContrast = null
        pendingAgentPrompt = null
        pendingNlCommand = null
        pendingRegenerate = false
        pendingTrendPrompt = null
        pendingTemplateCategory = null
        prefs.edit().remove(KEY_SOURCE_URI).apply()
        clearResult()
    }

    private fun clearBitmapsOnly() {
        workingBitmap?.recycle()
        workingBitmap = null
        clearResult()
    }

    private fun persistSourceUri(uri: Uri) {
        prefs.edit().putString(KEY_SOURCE_URI, uri.toString()).apply()
    }

    private companion object {
        const val PREFS = "pixijoy_image_session"
        const val KEY_SOURCE_URI = "source_uri"
    }
}
