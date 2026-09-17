package com.pixijoy.photo

import android.content.Context
import android.graphics.Bitmap
import com.pixijoy.core.common.AppResult

interface PhotoEngine {
    fun render(sources: List<String>, ops: List<PhotoOp>, maxEdge: Int): Bitmap
    fun export(
        context: Context,
        sources: List<String>,
        ops: List<PhotoOp>,
        displayName: String,
        format: ExportFormat
    ): AppResult<String>
}

class StubPhotoEngine : PhotoEngine {
    override fun render(sources: List<String>, ops: List<PhotoOp>, maxEdge: Int): Bitmap {
        error("Stub")
    }

    override fun export(
        context: Context,
        sources: List<String>,
        ops: List<PhotoOp>,
        displayName: String,
        format: ExportFormat
    ): AppResult<String> = AppResult.Err("Stub")
}
