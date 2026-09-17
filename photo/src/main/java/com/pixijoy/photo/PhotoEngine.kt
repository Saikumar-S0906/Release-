package com.pixijoy.photo

import android.content.Context
import android.graphics.Bitmap
import com.pixijoy.core.common.AppResult

interface PhotoEngine {
    fun render(sourcePath: String, ops: List<PhotoOp>, maxEdge: Int): Bitmap
    fun exportJpeg(
        context: Context,
        sourcePath: String,
        ops: List<PhotoOp>,
        displayName: String
    ): AppResult<String>
}

class StubPhotoEngine : PhotoEngine {
    override fun render(sourcePath: String, ops: List<PhotoOp>, maxEdge: Int): Bitmap {
        error("Stub")
    }

    override fun exportJpeg(
        context: Context,
        sourcePath: String,
        ops: List<PhotoOp>,
        displayName: String
    ): AppResult<String> = AppResult.Err("Stub")
}
