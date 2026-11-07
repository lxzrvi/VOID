package dev.voidapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** One "on this day" event from Wikipedia's public feed. */
data class DayEvent(
    val year: Int,
    val text: String,
    val sourceUrl: String?,
)

/**
 * Wikipedia "On this day" - free, no key, no library. The last successful result
 * is persisted so the home page shows something even offline.
 */
object OnThisDay {

    suspend fun fetch(): List<DayEvent> = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val url = "https://api.wikimedia.org/feed/v1/wikipedia/en/onthisday/selected/$month/$day"

        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "VOID-Android/0.3 (https://github.com/lxzrvi/VOID)")
            }
            try {
                parse(connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(emptyList())
    }

    private fun parse(body: String): List<DayEvent> {
        val root = JSONObject(body)
        val events = root.optJSONArray("selected") ?: root.optJSONArray("events") ?: return emptyList()
        val out = ArrayList<DayEvent>(events.length())
        for (i in 0 until events.length()) {
            val obj = events.optJSONObject(i) ?: continue
            val text = obj.optString("text").takeIf { it.isNotBlank() } ?: continue
            val source = obj.optJSONArray("pages")?.optJSONObject(0)
                ?.optJSONObject("content_urls")?.optJSONObject("desktop")?.optString("page")
            out += DayEvent(obj.optInt("year"), text, source)
        }
        return out
    }

    fun todayLabel(): String = SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Calendar.getInstance().time)

    fun timeLabel(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)
}
