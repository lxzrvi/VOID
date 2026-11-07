package dev.voidapp.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
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
 * Favicons without an image-loader dependency, with a persistent cache:
 *
 * 1. a favicon already stored in SharedPreferences (base64) is shown on the very
 *    first frame, so shortcuts never pop in late;
 * 2. otherwise it is fetched once, then handed back through [onFetched] so the
 *    caller can persist it for the next launch (offline-safe after that).
 */
private object FaviconMemory {
    private val cache = LruCache<String, ImageBitmap>(96)
    private val failed = HashSet<String>()

    fun get(key: String): ImageBitmap? = cache.get(key)
    fun failed(key: String): Boolean = synchronized(failed) { key in failed }
    fun markFailed(key: String) = synchronized(failed) { failed += key }
    fun put(key: String, value: ImageBitmap) = cache.put(key, value)
}

private suspend fun load(url: String): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("User-Agent", "VOID-Android")
        }
        try {
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}

private fun ByteArray.toImageBitmap(): ImageBitmap? =
    BitmapFactory.decodeByteArray(this, 0, size)?.asImageBitmap()

/** Warms the in-memory cache for every shortcut, once, on app start. */
suspend fun preloadFavicons(items: Map<String, String>) {
    items.forEach { (url, faviconUrl) ->
        if (FaviconMemory.get(faviconUrl) != null || FaviconMemory.failed(faviconUrl)) return@forEach
        withContext(Dispatchers.IO) {
            val bytes = load(faviconUrl) ?: run {
                FaviconMemory.markFailed(faviconUrl)
                null
            } ?: return@withContext
            val image = bytes.toImageBitmap() ?: return@withContext
            FaviconMemory.put(faviconUrl, image)
        }
    }
}

@Composable
fun Favicon(
    faviconUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    circleColor: Color = Color.White,
    cachedBase64: String? = null,
    onFetched: (String) -> Unit = {},
) {
    // 1st frame: whatever the store already has.
    val cachedBitmap = remember(cachedBase64) {
        cachedBase64?.let {
            runCatching { Base64.decode(it, Base64.NO_WRAP).toImageBitmap() }.getOrNull()
        }
    }
    var bitmap by remember(faviconUrl, cachedBase64) {
        mutableStateOf(FaviconMemory.get(faviconUrl) ?: cachedBitmap)
    }

    // The background preloader may have filled the memory cache since the last
    // composition; pick it up without a network round trip.
    LaunchedEffect(faviconUrl, cachedBase64) {
        val warm = FaviconMemory.get(faviconUrl)
        if (warm != null && bitmap == null) bitmap = warm
    }

    LaunchedEffect(faviconUrl) {
        if (bitmap == null && !FaviconMemory.failed(faviconUrl)) {
            val bytes = load(faviconUrl)
            val image = bytes?.toImageBitmap()
            if (image != null) {
                FaviconMemory.put(faviconUrl, image)
                bitmap = image
                onFetched(Base64.encodeToString(bytes, Base64.NO_WRAP))
            } else {
                FaviconMemory.markFailed(faviconUrl)
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
                modifier = Modifier.size(size * 0.62f),
            )
        }
    }
}
