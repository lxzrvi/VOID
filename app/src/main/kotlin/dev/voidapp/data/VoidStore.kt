package dev.voidapp.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * SharedPreferences-backed store. Everything the home page needs (shortcut order,
 * favicons, notes, box edits, background) is read synchronously in the
 * constructor, so the page renders complete on the very first frame - no
 * "loading, then pop in".
 */
class VoidStore(context: Context) {
    private val prefs = context.getSharedPreferences("void_store", Context.MODE_PRIVATE)

    // ---- startup snapshot ------------------------------------------------
    val cachedSites: List<Site> = readSites()
    val cachedFavicons: Map<String, String> = readJsonObject(prefs.getString("favicons", "{}"))
    val cachedNotes: List<Note> = readNotes()
    val cachedBackground: Background = readBackground()
    val cachedBoxEdits: Map<String, BoxEdit> = readBoxEdits()
    val cachedTools: List<String> = readStringList("tool_order")

    // ---- island placement -------------------------------------------------
    var islandOffset: Float
        get() = prefs.getFloat("island_offset", 1f)
        set(value) = prefs.edit { putFloat("island_offset", value.coerceIn(0f, 1f)) }

    var islandOnRight: Boolean
        get() = prefs.getBoolean("island_right", true)
        set(value) = prefs.edit { putBoolean("island_right", value) }

    var darkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", true)
        set(value) = prefs.edit { putBoolean("dark_theme", value) }

    var blockAds: Boolean
        get() = prefs.getBoolean("block_ads", true)
        set(value) = prefs.edit { putBoolean("block_ads", value) }

    var webDark: Boolean
        get() = prefs.getBoolean("web_dark", false)
        set(value) = prefs.edit { putBoolean("web_dark", value) }

    var suggestions: Boolean
        get() = prefs.getBoolean("suggestions", true)
        set(value) = prefs.edit { putBoolean("suggestions", value) }

    // ---- search -----------------------------------------------------------
    var provider: SearchProvider
        get() = SearchProvider.fromName(prefs.getString("provider", null))
        set(value) = prefs.edit { putString("provider", value.name) }

    // ---- shortcuts --------------------------------------------------------
    fun readSites(): List<Site> = runCatching {
        val array = JSONArray(prefs.getString("sites", "[]"))
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Site(
                name = obj.optString("name"),
                url = obj.optString("url"),
                custom = obj.optBoolean("custom", false),
                hidden = obj.optBoolean("hidden", false),
            )
        }
    }.getOrDefault(emptyList())

    fun writeSites(sites: List<Site>) = prefs.edit {
        val array = JSONArray()
        sites.forEach {
            array.put(
                JSONObject()
                    .put("name", it.name)
                    .put("url", it.url)
                    .put("custom", it.custom)
                    .put("hidden", it.hidden),
            )
        }
        putString("sites", array.toString())
    }

    fun writeFavicons(map: Map<String, String>) = prefs.edit {
        putString("favicons", JSONObject(map as Map<*, *>).toString())
    }

    // ---- notes ------------------------------------------------------------
    private fun readNotes(): List<Note> = runCatching {
        val array = JSONArray(prefs.getString("notes", "[]"))
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Note(obj.optLong("id"), obj.optString("text"), obj.optLong("ts"))
        }
    }.getOrDefault(emptyList())

    fun writeNotes(notes: List<Note>) = prefs.edit {
        val array = JSONArray()
        notes.forEach {
            array.put(JSONObject().put("id", it.id).put("text", it.text).put("ts", it.createdAt))
        }
        putString("notes", array.toString())
    }

    // ---- box edits --------------------------------------------------------
    private fun readBoxEdits(): Map<String, BoxEdit> = runCatching {
        val obj = JSONObject(prefs.getString("box_edits", "{}"))
        obj.keys().asSequence().associateWith { key ->
            val item = obj.getJSONObject(key)
            BoxEdit(
                x = item.optDouble("x", 0.0).toFloat(),
                y = item.optDouble("y", 0.0).toFloat(),
                scale = item.optDouble("scale", 1.0).toFloat(),
                radiusDp = item.optDouble("radius", 24.0).toFloat(),
                alpha = item.optDouble("alpha", 1.0).toFloat(),
                colorArgb = if (item.has("color")) item.optLong("color") else null,
            )
        }
    }.getOrDefault(emptyMap())

    fun writeBoxEdits(edits: Map<String, BoxEdit>) = prefs.edit {
        val obj = JSONObject()
        edits.forEach { (key, value) ->
            obj.put(
                key,
                JSONObject()
                    .put("x", value.x)
                    .put("y", value.y)
                    .put("scale", value.scale)
                    .put("radius", value.radiusDp)
                    .put("alpha", value.alpha)
                    .apply { value.colorArgb?.let { put("color", it) } },
            )
        }
        putString("box_edits", obj.toString())
    }

    // ---- background -------------------------------------------------------
    private fun readBackground(): Background = runCatching {
        val obj = JSONObject(prefs.getString("background", "{}"))
        Background(
            kind = runCatching { Background.Kind.valueOf(obj.optString("kind", "Brand")) }
                .getOrDefault(Background.Kind.Brand),
            imageUri = obj.optString("image").takeIf { it.isNotBlank() },
            blur = obj.optDouble("blur", 0.0).toFloat(),
            dim = obj.optDouble("dim", 0.0).toFloat(),
            brightness = obj.optDouble("brightness", 0.0).toFloat(),
            angle = obj.optDouble("angle", 135.0).toFloat(),
            startColor = obj.optLong("start", 0xFFC10520),
            endColor = obj.optLong("end", 0xFF0A0A0B),
        )
    }.getOrDefault(Background())

    fun writeBackground(background: Background) = prefs.edit {
        putString(
            "background",
            JSONObject()
                .put("kind", background.kind.name)
                .put("image", background.imageUri.orEmpty())
                .put("blur", background.blur)
                .put("dim", background.dim)
                .put("brightness", background.brightness)
                .put("angle", background.angle)
                .put("start", background.startColor)
                .put("end", background.endColor)
                .toString(),
        )
    }

    // ---- tools order ------------------------------------------------------
    fun readStringList(key: String): List<String> = runCatching {
        val array = JSONArray(prefs.getString(key, "[]"))
        (0 until array.length()).map { array.getString(it) }
    }.getOrDefault(emptyList())

    fun writeStringList(key: String, values: List<String>) = prefs.edit {
        putString(key, JSONArray(values).toString())
    }

    // ---- open tabs (restored on the next launch) --------------------------
    fun readTabs(): List<SavedTab> = runCatching {
        val array = JSONArray(prefs.getString("tabs", "[]"))
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            SavedTab(obj.optString("url"), obj.optString("title"), obj.optBoolean("desktop", false))
        }
    }.getOrDefault(emptyList())

    var activeTabIndex: Int
        get() = prefs.getInt("tab_active", 0)
        set(value) = prefs.edit { putInt("tab_active", value) }

    fun writeTabs(tabs: List<SavedTab>) = prefs.edit {
        val array = JSONArray()
        tabs.forEach {
            array.put(
                JSONObject().put("url", it.url).put("title", it.title).put("desktop", it.desktop),
            )
        }
        putString("tabs", array.toString())
    }

    // ---- bookmarks / history ---------------------------------------------
    fun readEntries(key: String): List<PageEntry> = runCatching {
        val array = JSONArray(prefs.getString(key, "[]"))
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            PageEntry(obj.optString("url"), obj.optString("title"), obj.optLong("ts"))
        }
    }.getOrDefault(emptyList())

    fun writeEntries(key: String, entries: List<PageEntry>) = prefs.edit {
        val array = JSONArray()
        entries.forEach {
            array.put(JSONObject().put("url", it.url).put("title", it.title).put("ts", it.timestamp))
        }
        putString(key, array.toString())
    }

    fun clearBrowsingData() = prefs.edit {
        putString("history", "[]")
        putString("bookmarks", "[]")
    }

    private fun readJsonObject(raw: String?): Map<String, String> = runCatching {
        val obj = JSONObject(raw ?: "{}")
        obj.keys().asSequence().associateWith { obj.getString(it) }
    }.getOrDefault(emptyMap())
}
