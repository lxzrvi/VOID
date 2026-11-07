package dev.voidapp.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Favicons without an image-loading dependency: fetch the bytes off the main
 * thread, decode, and keep a small in-memory cache. Failures fall back to a
 * plain circle, which keeps the grid stable when offline.
 */
private object FaviconCache {
    private val cache = LruCache<String, ImageBitmap>(64)
    private val failed = HashSet<String>()

    fun get(key: String): ImageBitmap? = cache.get(key)

    fun has(key: String): Boolean = cache.get(key) != null

    fun failed(key: String): Boolean = synchronized(failed) { key in failed }

    fun markFailed(key: String) = synchronized(failed) { failed += key }

    fun put(key: String, value: ImageBitmap) {
        cache.put(key, value)
    }
}

private suspend fun loadFavicon(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("User-Agent", "VOID-Android")
        }
        try {
            val bytes = connection.inputStream.use { it.readBytes() }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}

/** Renders [faviconUrl] inside a white circle of [size]. */
@Composable
fun Favicon(
    faviconUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    circleColor: Color = Color.White,
) {
    var bitmap by remember(faviconUrl) { mutableStateOf(FaviconCache.get(faviconUrl)) }

    LaunchedEffect(faviconUrl) {
        if (bitmap == null && !FaviconCache.failed(faviconUrl)) {
            val loaded = loadFavicon(faviconUrl)
            if (loaded != null) {
                FaviconCache.put(faviconUrl, loaded)
                bitmap = loaded
            } else {
                FaviconCache.markFailed(faviconUrl)
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(circleColor),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}
