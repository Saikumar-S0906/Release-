package com.pixijoy.ai.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.pixijoy.ai.data.CreationsRepository
import com.pixijoy.ai.data.SavedCreationType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val creationsRepository: CreationsRepository
) {
    suspend fun saveToGallery(
        bitmap: Bitmap,
        addWatermark: Boolean,
        type: SavedCreationType = SavedCreationType.PHOTO,
        title: String = "Photo"
    ): Uri? = withContext(Dispatchers.IO) {
        val outputBitmap = if (addWatermark) addWatermark(bitmap) else bitmap
        val fileName = "PixiJoy_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PixiJoyAI")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext null
        try {
            val wrote = resolver.openOutputStream(uri)?.use { stream ->
                outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            } == true
            if (!wrote) {
                runCatching { resolver.delete(uri, null, null) }
                return@withContext null
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            creationsRepository.record(uri, type, title)
            uri
        } catch (_: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            null
        } finally {
            if (outputBitmap != bitmap) outputBitmap.recycle()
        }
    }

    suspend fun saveBatch(bitmaps: List<Bitmap>, addWatermark: Boolean): Int = withContext(Dispatchers.IO) {
        var saved = 0
        bitmaps.forEachIndexed { index, bitmap ->
            val outputBitmap = if (addWatermark) addWatermark(bitmap) else bitmap
            val fileName = "PixiJoy_Pack_${System.currentTimeMillis()}_$index.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PixiJoyAI/ViralPack")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                try {
                    val wrote = context.contentResolver.openOutputStream(uri)?.use { stream ->
                        outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    } == true
                    if (wrote) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            values.clear()
                            values.put(MediaStore.Images.Media.IS_PENDING, 0)
                            context.contentResolver.update(uri, values, null, null)
                        }
                        saved++
                    } else {
                        runCatching { context.contentResolver.delete(uri, null, null) }
                    }
                } catch (_: Exception) {
                    runCatching { context.contentResolver.delete(uri, null, null) }
                }
            }
            if (outputBitmap != bitmap) outputBitmap.recycle()
        }
        saved
    }

    suspend fun saveGifToGallery(gifBytes: ByteArray): Uri? = withContext(Dispatchers.IO) {
        val fileName = "PixiJoy_${System.currentTimeMillis()}.gif"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/gif")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PixiJoyAI")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        try {
            resolver.openOutputStream(uri)?.use { stream -> stream.write(gifBytes) }
                ?: run {
                    runCatching { resolver.delete(uri, null, null) }
                    return@withContext null
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (_: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            null
        }
    }

    suspend fun createGifShareUri(gifBytes: ByteArray): Uri? = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(cacheDir, "share_${System.currentTimeMillis()}.gif")
        FileOutputStream(file).use { stream -> stream.write(gifBytes) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    suspend fun saveVideoToGallery(videoFile: File): Uri? = withContext(Dispatchers.IO) {
        val fileName = "PixiJoy_Reel_${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/PixiJoyAI")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        try {
            resolver.openOutputStream(uri)?.use { output ->
                videoFile.inputStream().use { input -> input.copyTo(output) }
            } ?: run {
                runCatching { resolver.delete(uri, null, null) }
                return@withContext null
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (_: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            null
        }
    }

    suspend fun createVideoShareUri(videoFile: File): Uri? = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(cacheDir, "share_${System.currentTimeMillis()}.mp4")
        videoFile.copyTo(file, overwrite = true)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    suspend fun createAudioShareUri(audioFile: File, mimeType: String = "audio/wav"): Uri? = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "share").apply { mkdirs() }
        val ext = if (mimeType.contains("wav")) "wav" else "aac"
        val file = File(cacheDir, "share_${System.currentTimeMillis()}.$ext")
        audioFile.copyTo(file, overwrite = true)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    suspend fun createShareUri(bitmap: Bitmap, addWatermark: Boolean): Uri? = withContext(Dispatchers.IO) {
        val outputBitmap = if (addWatermark) addWatermark(bitmap) else bitmap
        val cacheDir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(cacheDir, "share_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { stream ->
            outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        if (outputBitmap != bitmap) outputBitmap.recycle()
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun addWatermark(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(source, 0f, 0f, null)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 180
            textSize = (source.width * 0.04f).coerceAtLeast(24f)
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }
        val text = "PixiJoy AI"
        val padding = paint.textSize
        canvas.drawText(text, padding, source.height - padding, paint)
        return output
    }
}
