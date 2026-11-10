package dev.voidapp.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

enum class DownloadStatus { Running, Paused, Completed, Failed, Canceled }

/** What a page asked to download, before the user confirmed it. */
data class DownloadRequest(
    val url: String,
    val userAgent: String?,
    val mime: String?,
    val length: Long,
    val referer: String?,
    val fileName: String,
)

@Stable
class DownloadItem(
    val id: Long,
    val url: String,
    fileName: String,
    val mime: String?,
    val referer: String?,
    val userAgent: String?,
    val folder: String,
    val addedAt: Long = System.currentTimeMillis(),
) {
    var fileName by mutableStateOf(fileName)
    var status by mutableStateOf(DownloadStatus.Paused)
    var total by mutableLongStateOf(-1L)
    var done by mutableLongStateOf(0L)
    var speed by mutableLongStateOf(0L)
    var error by mutableStateOf<String?>(null)

    /** content:// or file:// of the finished file. */
    var resultUri by mutableStateOf<String?>(null)

    val progress: Float get() = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
}

/**
 * The built-in download manager. Transfers run in the app (HttpURLConnection) into
 * a part file, so they can be paused and resumed with a Range request; a finished
 * file is moved into Download/<folder> (MediaStore on Android 10+).
 */
class DownloadCenter(
    private val context: Context,
    private val scope: CoroutineScope,
    private val notify: (String) -> Unit,
) {
    val items = mutableStateListOf<DownloadItem>()
    private val jobs = HashMap<Long, Job>()
    private val prefs = context.getSharedPreferences("void_downloads", Context.MODE_PRIVATE)
    private val partsDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "parts").apply { mkdirs() }

    init {
        load()
    }

    val activeCount: Int get() = items.count { it.status == DownloadStatus.Running }

    fun displayPath(folder: String): String = "${Environment.DIRECTORY_DOWNLOADS}/$folder"

    // ---- control ------------------------------------------------------------------
    fun enqueue(request: DownloadRequest, folder: String): DownloadItem {
        val item = DownloadItem(
            id = System.currentTimeMillis(),
            url = request.url,
            fileName = request.fileName,
            mime = request.mime,
            referer = request.referer,
            userAgent = request.userAgent,
            folder = folder.ifBlank { "VOID" },
        )
        item.total = request.length
        items.add(0, item)
        start(item)
        return item
    }

    fun start(item: DownloadItem) {
        if (jobs[item.id]?.isActive == true) return
        item.status = DownloadStatus.Running
        item.error = null
        save()
        jobs[item.id] = scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                try {
                    transfer(item)
                } catch (e: Exception) {
                    item.error = e.message ?: "Download failed"
                    Outcome.Failed
                }
            }
            when (outcome) {
                Outcome.Done -> {
                    item.status = DownloadStatus.Completed
                    notify("Downloaded ${item.fileName}")
                }
                Outcome.Failed -> item.status = DownloadStatus.Failed
                Outcome.Stopped -> Unit // pause / cancel already set the status
            }
            item.speed = 0
            save()
        }
    }

    fun pause(item: DownloadItem) {
        if (item.status != DownloadStatus.Running) return
        item.status = DownloadStatus.Paused
        item.speed = 0
        jobs[item.id]?.cancel()
        save()
    }

    fun resume(item: DownloadItem) {
        if (item.status == DownloadStatus.Paused || item.status == DownloadStatus.Failed) start(item)
    }

    fun retry(item: DownloadItem) {
        partFile(item).delete()
        item.done = 0
        start(item)
    }

    fun cancel(item: DownloadItem) {
        item.status = DownloadStatus.Canceled
        item.speed = 0
        jobs[item.id]?.cancel()
        partFile(item).delete()
        save()
    }

    fun remove(item: DownloadItem, deleteFile: Boolean) {
        jobs[item.id]?.cancel()
        partFile(item).delete()
        if (deleteFile) {
            runCatching {
                val uri = item.resultUri?.let { Uri.parse(it) }
                if (uri != null && uri.scheme == "content") {
                    context.contentResolver.delete(uri, null, null)
                } else if (uri != null && uri.scheme == "file") {
                    File(uri.path.orEmpty()).delete()
                }
            }
        }
        items.remove(item)
        save()
    }

    fun pauseAll() = items.filter { it.status == DownloadStatus.Running }.forEach { pause(it) }

    fun resumeAll() = items.filter { it.status == DownloadStatus.Paused || it.status == DownloadStatus.Failed }
        .forEach { start(it) }

    fun clearFinished() {
        items.removeAll { it.status == DownloadStatus.Completed || it.status == DownloadStatus.Canceled }
        save()
    }

    fun open(item: DownloadItem) {
        val uri = item.resultUri?.let { Uri.parse(it) } ?: return
        val type = item.mime ?: mimeOf(item.fileName)
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, type)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
        }.onFailure { notify("No app can open this file") }
    }

    // ---- transfer ---------------------------------------------------------------------
    private enum class Outcome { Done, Failed, Stopped }

    private fun partFile(item: DownloadItem) = File(partsDir, "${item.id}.part")

    private suspend fun transfer(item: DownloadItem): Outcome {
        val ctx = currentCoroutineContext()
        val part = partFile(item)
        var existing = if (part.exists()) part.length() else 0L

        val connection = (URL(item.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            instanceFollowRedirects = true
            item.userAgent?.let { setRequestProperty("User-Agent", it) }
            item.referer?.let { setRequestProperty("Referer", it) }
            CookieManager.getInstance().getCookie(item.url)?.let { setRequestProperty("Cookie", it) }
            if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
        }
        try {
            val code = connection.responseCode
            val append: Boolean
            when (code) {
                206 -> {
                    append = true
                    val range = connection.getHeaderField("Content-Range")
                    item.total = range?.substringAfter('/')?.toLongOrNull() ?: item.total
                }
                200 -> {
                    append = false
                    existing = 0
                    item.total = connection.contentLengthLong
                }
                else -> throw IOException("Server answered $code")
            }
            item.done = existing

            connection.inputStream.use { input ->
                FileOutputStream(part, append).use { out ->
                    val buffer = ByteArray(32 * 1024)
                    var lastTick = System.currentTimeMillis()
                    var lastBytes = item.done
                    while (true) {
                        if (!ctx.isActive) return Outcome.Stopped
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        item.done += read
                        val now = System.currentTimeMillis()
                        if (now - lastTick >= 400) {
                            item.speed = ((item.done - lastBytes) * 1000 / (now - lastTick))
                            lastTick = now
                            lastBytes = item.done
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        if (!ctx.isActive) return Outcome.Stopped
        item.resultUri = publish(part, item.fileName, item.mime, item.folder)
        part.delete()
        return Outcome.Done
    }

    /** Moves a finished file into Download/<folder>. Returns its uri. */
    fun publish(file: File, name: String, mime: String?, folder: String): String {
        val type = mime ?: mimeOf(name)
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, type)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$folder")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Could not create the file")
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                ?: throw IOException("Could not write the file")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri.toString()
        }
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), folder)
        val target = runCatching {
            dir.mkdirs()
            uniqueFile(dir, name).also { file.copyTo(it, overwrite = false) }
        }.getOrElse {
            // No storage permission on old Android: keep it in the app's own folder.
            val own = File(context.getExternalFilesDir(null) ?: context.filesDir, "Download").apply { mkdirs() }
            uniqueFile(own, name).also { file.copyTo(it, overwrite = false) }
        }
        return Uri.fromFile(target).toString()
    }

    private fun uniqueFile(dir: File, name: String): File {
        var candidate = File(dir, name)
        var index = 1
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
        while (candidate.exists()) {
            candidate = File(dir, "$base ($index)$ext")
            index++
        }
        return candidate
    }

    private fun mimeOf(name: String): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase())
            ?: "application/octet-stream"

    // ---- persistence ------------------------------------------------------------------
    private fun save() {
        val array = JSONArray()
        items.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("url", it.url)
                    .put("name", it.fileName)
                    .put("mime", it.mime ?: "")
                    .put("referer", it.referer ?: "")
                    .put("ua", it.userAgent ?: "")
                    .put("folder", it.folder)
                    .put("status", it.status.name)
                    .put("total", it.total)
                    .put("done", it.done)
                    .put("uri", it.resultUri ?: "")
                    .put("added", it.addedAt),
            )
        }
        prefs.edit().putString("items", array.toString()).apply()
    }

    private fun load() {
        runCatching {
            val array = JSONArray(prefs.getString("items", "[]"))
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val item = DownloadItem(
                    id = o.getLong("id"),
                    url = o.getString("url"),
                    fileName = o.getString("name"),
                    mime = o.optString("mime").ifEmpty { null },
                    referer = o.optString("referer").ifEmpty { null },
                    userAgent = o.optString("ua").ifEmpty { null },
                    folder = o.optString("folder", "VOID"),
                    addedAt = o.optLong("added", 0L),
                )
                item.total = o.optLong("total", -1L)
                item.resultUri = o.optString("uri").ifEmpty { null }
                val saved = runCatching { DownloadStatus.valueOf(o.getString("status")) }.getOrDefault(DownloadStatus.Paused)
                // Whatever was running when the app closed is now paused, with its bytes kept.
                item.status = if (saved == DownloadStatus.Running) DownloadStatus.Paused else saved
                item.done = if (item.status == DownloadStatus.Completed) item.total.coerceAtLeast(0) else {
                    partFile(item).takeIf { it.exists() }?.length() ?: 0L
                }
                items.add(item)
            }
        }
    }
}
