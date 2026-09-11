package com.pixijoy.ai.ui.board.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.FileProvider
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.board.BoardMemory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Decode board drawables with inSampleSize so 1–2MB PNGs aren't always full-res in memory. */
fun decodeBoardDrawable(
    context: Context,
    @DrawableRes resId: Int,
    maxSidePx: Int
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(context.resources, resId, bounds)
    val w = bounds.outWidth
    val h = bounds.outHeight
    if (w <= 0 || h <= 0) return null
    var sample = 1
    while (w / sample > maxSidePx || h / sample > maxSidePx) {
        sample *= 2
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
    return BitmapFactory.decodeResource(context.resources, resId, opts)
}

/**
 * Size-bounded board art. Prefer this over raw [painterResource] for large board_*.png assets.
 * Vectors fall back to [painterResource]; decode failure shows [BoardFlowArt.imageFail] (Bible §29).
 * @param maxSidePx decode budget (thumbs ~384, heroes ~1080, export ~1600)
 */
@Composable
fun BoardArtImage(
    @DrawableRes resId: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    maxSidePx: Int = 1080
) {
    val context = LocalContext.current
    val bitmap = remember(resId, maxSidePx) {
        if (resId == 0) null else decodeBoardDrawable(context, resId, maxSidePx)
    }
    when {
        bitmap != null -> Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
        resId == 0 -> Image(
            painter = painterResource(BoardFlowArt.imageFail),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
        else -> {
            // Vectors / XML drawables: BitmapFactory bounds are empty — use painter.
            Image(
                painter = painterResource(resId),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
    }
}

/** Shared gallery save / share cache for board soft-AI demos (always call from IO). */
object BoardDrawableIo {
    private val shareScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun saveToGallery(
        context: Context,
        @DrawableRes resId: Int,
        filePrefix: String = "PixiJoy",
        colorMatrix: android.graphics.ColorMatrix? = null
    ): Boolean {
        val decoded = decodeBoardDrawable(context, resId, maxSidePx = 1600) ?: return false
        val bitmap = bakeColorMatrix(decoded, colorMatrix)
        return writePngToGallery(context, bitmap, filePrefix)
    }

    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        filePrefix: String = "PixiJoy",
        /** 0=JPG · 1=PNG · 2=WEBP */
        formatIndex: Int = 1,
        quality: Int = 92
    ): Boolean = writeBitmapToGallery(context, bitmap, filePrefix, formatIndex, quality)

    /** Write raw bytes (e.g. GIF) into Pictures/PixiJoyAI via MediaStore Images. */
    fun saveBytesToGallery(
        context: Context,
        bytes: ByteArray,
        filePrefix: String = "PixiJoy",
        mimeType: String = "image/gif",
        extension: String = "gif"
    ): Boolean {
        if (bytes.isEmpty()) return false
        val ext = extension.trimStart('.').ifBlank { "bin" }
        val fileName = "${filePrefix}_${System.currentTimeMillis()}.$ext"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/PixiJoyAI"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { out -> out.write(bytes) } ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            BoardMemory.rememberCreationAsync(context, uri, filePrefix)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Copy an MP4 into Movies/PixiJoyAI via MediaStore Video. */
    fun saveVideoToGallery(
        context: Context,
        file: File,
        filePrefix: String = "PixiJoy_Reel"
    ): Boolean {
        if (!file.exists() || file.length() <= 0L) return false
        val fileName = "${filePrefix}_${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_MOVIES + "/PixiJoyAI"
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input -> input.copyTo(out) }
            } ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            BoardMemory.rememberCreationAsync(context, uri, filePrefix)
            true
        } catch (_: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            false
        }
    }

    private fun bakeColorMatrix(
        source: Bitmap,
        colorMatrix: android.graphics.ColorMatrix?
    ): Bitmap {
        if (colorMatrix == null) return source
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
        }
        android.graphics.Canvas(out).drawBitmap(source, 0f, 0f, paint)
        return out
    }

    private fun writePngToGallery(
        context: Context,
        bitmap: Bitmap,
        filePrefix: String
    ): Boolean = writeBitmapToGallery(context, bitmap, filePrefix, formatIndex = 1, quality = 92)

    private fun writeBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        filePrefix: String,
        formatIndex: Int,
        quality: Int
    ): Boolean {
        val q = quality.coerceIn(1, 100)
        val (ext, mime, compress) = when (formatIndex.coerceIn(0, 2)) {
            0 -> Triple("jpg", "image/jpeg", Bitmap.CompressFormat.JPEG)
            2 -> Triple(
                "webp",
                "image/webp",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            )
            else -> Triple("png", "image/png", Bitmap.CompressFormat.PNG)
        }
        val fileName = "${filePrefix}_${System.currentTimeMillis()}.$ext"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/PixiJoyAI"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(compress, if (compress == Bitmap.CompressFormat.PNG) 100 else q, out)
            } ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            BoardMemory.rememberCreationAsync(context, uri, filePrefix)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun cacheShareUri(
        context: Context,
        @DrawableRes resId: Int,
        filePrefix: String = "pixijoy_share"
    ): Uri? {
        val bitmap = decodeBoardDrawable(context, resId, maxSidePx = 1600) ?: return null
        return try {
            val cache = File(context.cacheDir, "share").also { it.mkdirs() }
            val file = File(cache, "${filePrefix}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 92, out)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            null
        }
    }

    fun cacheShareUri(
        context: Context,
        bitmap: Bitmap,
        filePrefix: String = "pixijoy_share"
    ): Uri? {
        if (bitmap.isRecycled) return null
        return try {
            val cache = File(context.cacheDir, "share").also { it.mkdirs() }
            val file = File(cache, "${filePrefix}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 92, out)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Off-main-thread share; safe to call from click handlers. */
    fun shareAsync(
        context: Context,
        @DrawableRes resId: Int,
        @StringRes textRes: Int,
        filePrefix: String = "pixijoy_share",
        targetPackage: String? = null,
        onComplete: (() -> Unit)? = null
    ) {
        shareAsync(
            context = context,
            resId = resId,
            text = context.getString(textRes),
            filePrefix = filePrefix,
            targetPackage = targetPackage,
            onComplete = onComplete
        )
    }

    fun shareAsync(
        context: Context,
        @DrawableRes resId: Int,
        text: String,
        filePrefix: String = "pixijoy_share",
        targetPackage: String? = null,
        onComplete: (() -> Unit)? = null
    ) {
        shareBitmapAsync(
            context = context,
            bitmap = null,
            fallbackRes = resId,
            text = text,
            filePrefix = filePrefix,
            targetPackage = targetPackage,
            onComplete = onComplete
        )
    }

    fun shareBitmapAsync(
        context: Context,
        bitmap: Bitmap?,
        @DrawableRes fallbackRes: Int,
        text: String = "",
        filePrefix: String = "pixijoy_share",
        targetPackage: String? = null,
        onComplete: (() -> Unit)? = null
    ) {
        val app = context.applicationContext
        val fail = app.getString(R.string.board_f5_save_failed)
        val chooserTitle = app.getString(R.string.share)
        val live = bitmap?.takeIf { !it.isRecycled }
        shareScope.launch {
            val uri = withContext(Dispatchers.IO) {
                if (live != null) cacheShareUri(app, live, filePrefix)
                else cacheShareUri(app, fallbackRes, filePrefix)
            }
            withContext(Dispatchers.Main.immediate) {
                launchShare(app, uri, text, targetPackage, fail, chooserTitle)
                onComplete?.invoke()
            }
        }
    }

    fun shareToWhatsApp(
        context: Context,
        bitmap: Bitmap?,
        @DrawableRes fallbackRes: Int,
        filePrefix: String = "pixijoy_share"
    ) {
        shareBitmapAsync(
            context = context,
            bitmap = bitmap,
            fallbackRes = fallbackRes,
            filePrefix = filePrefix,
            targetPackage = PKG_WHATSAPP
        )
    }

    private fun launchShare(
        app: Context,
        uri: Uri?,
        text: String,
        targetPackage: String?,
        fail: String,
        chooserTitle: String
    ) {
        if (uri == null) {
            Toast.makeText(app, fail, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (text.isNotBlank()) putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pkg = resolveSharePackage(app, targetPackage)
        if (pkg != null) {
            intent.setPackage(pkg)
            runCatching {
                app.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                app.startActivity(intent)
            }.onFailure {
                runCatching {
                    app.startActivity(
                        Intent.createChooser(intent.setPackage(null), chooserTitle)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }.onFailure {
                    Toast.makeText(app, fail, Toast.LENGTH_SHORT).show()
                }
            }
            return
        }
        runCatching {
            app.startActivity(
                Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            Toast.makeText(app, fail, Toast.LENGTH_SHORT).show()
        }
    }

    private fun resolveSharePackage(context: Context, requested: String?): String? {
        if (requested.isNullOrBlank()) return null
        if (isPackageInstalled(context, requested)) return requested
        if (requested == PKG_WHATSAPP && isPackageInstalled(context, PKG_WHATSAPP_BIZ)) {
            return PKG_WHATSAPP_BIZ
        }
        return null
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(packageName, 0) }.isSuccess

    private const val PKG_WHATSAPP = "com.whatsapp"
    private const val PKG_WHATSAPP_BIZ = "com.whatsapp.w4b"
}
