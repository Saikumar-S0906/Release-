package com.pixijoy.ai.ui.board.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.pixijoy.ai.ui.board.BoardFeatureFlags
import com.pixijoy.ai.ui.board.theme.BoardTheme

/**
 * B10 stub catalog — remote creative keys for Discover / Create / Stickers.
 * URLs are placeholders until design CDN packs land; Coil errors fall back to drawables.
 */
object BoardCdn {
    const val DISCOVER_FEATURED = "discover/featured"
    const val DISCOVER_TRENDING = "discover/trending"
    const val CREATE_BEAUTIFUL = "create/beautiful"
    const val CREATE_FUNNY = "create/funny"
    const val STICKER_PACK = "stickers/pack_hero"

    private val urls = mapOf(
        DISCOVER_FEATURED to "https://cdn.pixijoy.app/v1/discover/featured.jpg",
        DISCOVER_TRENDING to "https://cdn.pixijoy.app/v1/discover/trending.jpg",
        CREATE_BEAUTIFUL to "https://cdn.pixijoy.app/v1/create/beautiful.jpg",
        CREATE_FUNNY to "https://cdn.pixijoy.app/v1/create/funny.jpg",
        STICKER_PACK to "https://cdn.pixijoy.app/v1/stickers/pack_hero.jpg"
    )

    fun urlOrNull(key: String): String? =
        if (BoardFeatureFlags.cdnEnabled) urls[key] else null
}

/**
 * Coil remote image with drawable fallback (soft offline / missing CDN packs).
 */
@Composable
fun BoardRemoteImage(
    cdnKey: String,
    @DrawableRes fallbackRes: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    aspectRatio: Float = 16f / 9f,
    contentScale: ContentScale = ContentScale.Crop
) {
    val radii = BoardTheme.radii
    val url = BoardCdn.urlOrNull(cdnKey)
    val shape = RoundedCornerShape(radii.card)
    val base = modifier
        .fillMaxWidth()
        .aspectRatio(aspectRatio)
        .clip(shape)
    if (url == null) {
        Image(
            painter = painterResource(fallbackRes),
            contentDescription = contentDescription,
            modifier = if (contentDescription == null) base.boardDecorative() else base,
            contentScale = contentScale
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .build(),
            contentDescription = contentDescription,
            modifier = if (contentDescription == null) base.boardDecorative() else base,
            contentScale = contentScale,
            placeholder = painterResource(fallbackRes),
            error = painterResource(fallbackRes)
        )
    }
}
