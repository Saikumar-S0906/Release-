package com.pixijoy.ai.ui.board

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.pixijoy.ai.ui.board.components.BoardFlowArt

/**
 * Shared user photo for board create paths (Beautiful / Funny / Flow3).
 * Survives screen navigations within a create session.
 */
object BoardMediaSession {
    private const val TAG = "BoardMediaSession"

    @Volatile
    var photoUri: Uri? = null
        private set

    /** Short clip for Video→GIF (Create Tools). Independent of [photoUri]. */
    @Volatile
    var videoUri: Uri? = null
        private set

    @DrawableRes
    @Volatile
    var sampleRes: Int? = null
        private set

    /** Bumped on every set/clear so Compose can observe picks. */
    var revision by mutableIntStateOf(0)
        private set

    fun hasPhoto(): Boolean = photoUri != null || sampleRes != null

    fun setPhotoUri(uri: Uri) {
        photoUri = uri
        sampleRes = null
        revision++
    }

    fun setVideoUri(uri: Uri) {
        videoUri = uri
        revision++
    }

    fun setSampleRes(@DrawableRes resId: Int) {
        sampleRes = resId
        photoUri = null
        revision++
    }

    fun clear() {
        photoUri = null
        sampleRes = null
        videoUri = null
        revision++
    }

    fun decodeBitmap(context: Context, maxSide: Int = 1024): Bitmap? {
        photoUri?.let { uri ->
            return runCatching {
                decodeUri(context, uri, maxSide)
            }.onFailure { Log.w(TAG, "decode Uri failed", it) }.getOrNull()
        }
        sampleRes?.let { res ->
            return runCatching {
                BitmapFactory.decodeResource(context.resources, res)?.let { downscale(it, maxSide) }
            }.getOrNull()
        }
        return null
    }

    /** JPEG/PNG/HEIF plus camera RAW (DNG) via [ImageDecoder] on API 28+. */
    private fun decodeUri(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= 28) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > maxSide) {
                    val sample = (longest / maxSide).coerceAtLeast(1)
                    decoder.setTargetSampleSize(sample)
                }
            }
            val software = toSoftwareArgb(decoded) ?: return null
            return downscale(software, maxSide)
        }
        context.contentResolver.openInputStream(uri)?.use { input ->
            val raw = BitmapFactory.decodeStream(input) ?: return null
            return downscale(raw, maxSide)
        }
        return null
    }

    private fun toSoftwareArgb(src: Bitmap): Bitmap? {
        if (src.isRecycled) return null
        if (src.config == Bitmap.Config.ARGB_8888) return src
        val copy = src.copy(Bitmap.Config.ARGB_8888, false)
        if (copy != null && copy !== src && !src.isRecycled) {
            runCatching { src.recycle() }
        }
        return copy
    }

    private fun downscale(src: Bitmap, maxSide: Int): Bitmap {
        val w = src.width
        val h = src.height
        val longest = maxOf(w, h)
        if (longest <= maxSide) return src
        val scale = maxSide.toFloat() / longest
        val out = Bitmap.createScaledBitmap(
            src,
            (w * scale).toInt().coerceAtLeast(1),
            (h * scale).toInt().coerceAtLeast(1),
            true
        )
        if (out !== src) src.recycle()
        return out
    }
}

/** Renders [BoardMediaSession] photo, else [fallbackRes]. */
@Composable
fun BoardSessionPhoto(
    modifier: Modifier = Modifier,
    @DrawableRes fallbackRes: Int = BoardFlowArt.beautifulHero,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    colorFilter: androidx.compose.ui.graphics.ColorFilter? = null
) {
    val context = LocalContext.current
    val revision = BoardMediaSession.revision
    val uri = BoardMediaSession.photoUri
    val sample = BoardMediaSession.sampleRes
    @Suppress("UNUSED_VARIABLE")
    val observed = revision
    when {
        uri != null -> AsyncImage(
            model = ImageRequest.Builder(context).data(uri).build(),
            contentDescription = contentDescription,
            modifier = modifier.fillMaxSize(),
            contentScale = contentScale,
            colorFilter = colorFilter
        )
        sample != null -> Image(
            painter = painterResource(sample),
            contentDescription = contentDescription,
            modifier = modifier.fillMaxSize(),
            contentScale = contentScale,
            colorFilter = colorFilter
        )
        else -> Image(
            painter = painterResource(fallbackRes),
            contentDescription = contentDescription,
            modifier = modifier.fillMaxSize(),
            contentScale = contentScale,
            colorFilter = colorFilter
        )
    }
}
