package dev.voidapp.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tiny SharedPreferences store for settings, custom shortcuts, bookmarks and
 * history. JSON in a string keeps it dependency-free and easy to migrate.
 */
class VoidStore(context: Context) {
    private val prefs = context.getSharedPreferences("void_store", Context.MODE_PRIVATE)

    // ---- island placement -------------------------------------------------
    /** Vertical resting position of the circle, 0f (top) .. 1f (bottom). */
    var islandOffset: Float
        get() = prefs.getFloat("island_offset", 1f)
        set(value) = prefs.edit { putFloat("island_offset", value.coerceIn(0f, 1f)) }

    /** Which side the circle snaps to; the bar grows the other way. */
    var islandOnRight: Boolean
        get() = prefs.getBoolean("island_right", true)
        set(value) = prefs.edit { putBoolean("island_right", value) }

    // ---- search -----------------------------------------------------------
    var provider: SearchProvider
        get() = SearchProvider.fromName(prefs.getString("provider", null))
        set(value) = prefs.edit { putString("provider", value.name) }

    // ---- shortcuts --------------------------------------------------------
    /** Custom shortcuts the user added, appended after the 14 defaults. */
    var customSites: List<Site>
        get() = runCatching {
            val array = JSONArray(prefs.getString("custom_sites", "[]"))
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Site(
                    name = obj.optString("name"),
                    url = obj.optString("url"),
                    custom = true,
                )
            }
        }.getOrDefault(emptyList())
        set(value) = prefs.edit {
            val array = JSONArray()
            value.forEach {
                array.put(JSONObject().put("name", it.name).put("url", it.url))
            }
            putString("custom_sites", array.toString())
        }

    // ---- bookmarks --------------------------------------------------------
    var bookmarks: List<PageEntry>
        get() = readEntries("bookmarks")
        set(value) = writeEntries("bookmarks", value)

    fun toggleBookmark(entry: PageEntry) {
        val current = bookmarks
        bookmarks = if (current.any { it.url == entry.url }) {
            current.filterNot { it.url == entry.url }
        } else {
            listOf(entry) + current
        }
    }

    // ---- history ----------------------------------------------------------
    var history: List<PageEntry>
        get() = readEntries("history")
        set(value) = writeEntries("history", value.take(MAX_HISTORY))

    fun addHistory(entry: PageEntry) {
        if (entry.url.startsWith("about:")) return
        history = (listOf(entry) + history.filterNot { it.url == entry.url }).take(MAX_HISTORY)
    }

    // ---- data -------------------------------------------------------------
    fun clearBrowsingData() = prefs.edit {
        putString("history", "[]")
        putString("bookmarks", "[]")
    }

    // ---- helpers ----------------------------------------------------------
    private fun readEntries(key: String): List<PageEntry> = runCatching {
        val array = JSONArray(prefs.getString(key, "[]"))
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            PageEntry(
                url = obj.optString("url"),
                title = obj.optString("title"),
                timestamp = obj.optLong("ts"),
            )
        }
    }.getOrDefault(emptyList())

    private fun writeEntries(key: String, entries: List<PageEntry>) = prefs.edit {
        val array = JSONArray()
        entries.forEach {
            array.put(
                JSONObject()
                    .put("url", it.url)
                    .put("title", it.title)
                    .put("ts", it.timestamp),
            )
        }
        putString(key, array.toString())
    }

    private companion object {
        const val MAX_HISTORY = 200
    }
}
