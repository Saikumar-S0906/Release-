package com.pixijoy.ai.ui.board

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * Displays a session result bitmap without sharing a live native buffer that may be recycled.
 * Falls back to [fallbackRes] when null/recycled.
 */
@Composable
fun BoardSafeResultImage(
    bitmap: Bitmap?,
    @DrawableRes fallbackRes: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    colorFilter: ColorFilter? = null
) {
    val snapshot: ImageBitmap? = remember(bitmap) {
        if (bitmap == null || bitmap.isRecycled) {
            null
        } else {
            runCatching {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)?.asImageBitmap()
            }.getOrNull()
        }
    }
    DisposableEffect(snapshot) {
        onDispose {
            // ImageBitmap from copy — nothing to recycle on Android Compose ImageBitmap
        }
    }
    if (snapshot != null) {
        Image(
            bitmap = snapshot,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            colorFilter = colorFilter
        )
    } else {
        Image(
            painter = painterResource(fallbackRes),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            colorFilter = colorFilter
        )
    }
}
