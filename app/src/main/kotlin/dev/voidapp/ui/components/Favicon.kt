package dev.voidapp.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voidapp.ui.theme.Poppins
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Favicons that never "refresh" on app open.
 *
 * Lookup order, all of it synchronous for what is already known:
 *  1. in-memory LruCache (decoded bitmaps),
 *  2. a PNG on disk in filesDir/favicons (survives restarts, never evicted by the OS),
 *  3. only when neither has the icon: one network fetch, written to disk + memory.
 *
 * An icon that is already stored is NEVER fetched again, so the shortcuts and the
 * search providers are fully drawn on the very first frame, with no pop-in.
 * [warm] is called before the first frame to decode every known icon up front.
 */
object FaviconStore {
    private var dir: File? = null
    private val memory = LruCache<String, ImageBitmap>(200)
    private val failedAt = HashMap<String, Long>()
    private const val RETRY_AFTER_MS = 60_000L

    fun init(context: Context) {
        if (dir == null) {
            dir = File(context.applicationContext.filesDir, "favicons").apply { mkdirs() }
        }
    }

    private fun fileFor(url: String): File? {
        val base = dir ?: return null
        val digest = MessageDigest.getInstance("SHA-1").digest(url.toByteArray())
        val name = digest.take(10).joinToString("") { "%02x".format(it) }
        return File(base, "$name.png")
    }

    /** Memory, then disk. Never touches the network. */
    fun peek(url: String): ImageBitmap? {
        memory.get(url)?.let { return it }
        val file = fileFor(url) ?: return null
        if (!file.exists()) return null
        val decoded = runCatching { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }.getOrNull()
        if (decoded != null) memory.put(url, decoded)
        return decoded
    }

    /** Decodes every known icon before the first frame. */
    fun warm(urls: Collection<String>) {
        urls.forEach { peek(it) }
    }

    private fun recentlyFailed(url: String): Boolean = synchronized(failedAt) {
        val at = failedAt[url] ?: return false
        System.currentTimeMillis() - at < RETRY_AFTER_MS
    }

    /** Network fetch, only for icons that are not stored yet. */
    suspend fun fetch(url: String): ImageBitmap? {
        peek(url)?.let { return it }
        if (recentlyFailed(url)) return null
        val bytes = withContext(Dispatchers.IO) {
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
        val image = bytes?.let {
            runCatching { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }.getOrNull()
        }
        if (bytes == null || image == null) {
            synchronized(failedAt) { failedAt[url] = System.currentTimeMillis() }
            return null
        }
        memory.put(url, image)
        withContext(Dispatchers.IO) {
            runCatching { fileFor(url)?.writeBytes(bytes) }
        }
        return image
    }

    /** Fills in whatever is still missing (first launch / newly added sites). */
    suspend fun fetchMissing(urls: Collection<String>) {
        urls.forEach { url ->
            if (peek(url) == null) fetch(url)
        }
    }
}

/**
 * A round icon. The bitmap is looked up synchronously in the first composition,
 * so an already known icon is drawn on the first frame. While an unknown icon is
 * loading, the first letter of [fallbackLabel] sits in the circle instead of an
 * empty disc.
 */
@Composable
fun Favicon(
    faviconUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    circleColor: Color = Color.White,
    fallbackLabel: String? = null,
    fallbackSize: TextUnit = 14.sp,
) {
    val context = LocalContext.current
    remember { FaviconStore.init(context) }

    var bitmap by remember(faviconUrl) { mutableStateOf(FaviconStore.peek(faviconUrl)) }
    LaunchedEffect(faviconUrl) {
        if (bitmap == null) bitmap = FaviconStore.fetch(faviconUrl)
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
        } else {
            val letter = (fallbackLabel ?: contentDescription).orEmpty().trim().take(1).uppercase()
            if (letter.isNotEmpty()) {
                Text(
                    text = letter,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = fallbackSize,
                )
            }
        }
    }
}
