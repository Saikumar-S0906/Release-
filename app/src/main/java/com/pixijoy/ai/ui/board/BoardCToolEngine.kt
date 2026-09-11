package com.pixijoy.ai.ui.board

import android.content.Context
import android.graphics.Bitmap
import com.pixijoy.ai.di.BoardAiEntryPoint
import com.pixijoy.ai.ml.CreateExtraPipeline
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shared C-tool / Beautiful pixel pipeline runner (C04/C05/C10 pattern).
 */
object BoardCToolEngine {
    fun pipeline(context: Context): CreateExtraPipeline? = runCatching {
        EntryPointAccessors
            .fromApplication(context.applicationContext, BoardAiEntryPoint::class.java)
            .createExtraPipeline()
    }.getOrNull()

    suspend fun decodeSource(context: Context): Bitmap? =
        withContext(Dispatchers.IO) {
            BoardMediaSession.decodeBitmap(context.applicationContext)
        }

    suspend fun run(
        context: Context,
        block: suspend (CreateExtraPipeline, Bitmap) -> Bitmap
    ): Bitmap? = withContext(Dispatchers.Default) {
        val pipe = pipeline(context) ?: return@withContext null
        val source = BoardMediaSession.decodeBitmap(context.applicationContext) ?: return@withContext null
        runCatching { block(pipe, source) }.getOrNull()
    }
}
