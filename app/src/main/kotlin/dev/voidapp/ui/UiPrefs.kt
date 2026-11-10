package dev.voidapp.ui

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.voidapp.ui.proto.DefaultMenuTools
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Every customisable look-and-behaviour value of the app, as Compose state that is
 * written to SharedPreferences the moment it changes. Nothing here needs a restart.
 */
@Stable
class UiPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("void_ui", Context.MODE_PRIVATE)

    private class StatePref<T>(initial: T, private val save: (T) -> Unit) : ReadWriteProperty<Any?, T> {
        private val holder = mutableStateOf(initial)
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = holder.value
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            holder.value = value
            save(value)
        }
    }

    private fun bool(key: String, default: Boolean): ReadWriteProperty<Any?, Boolean> =
        StatePref(prefs.getBoolean(key, default)) { prefs.edit().putBoolean(key, it).apply() }

    private fun int(key: String, default: Int): ReadWriteProperty<Any?, Int> =
        StatePref(prefs.getInt(key, default)) { prefs.edit().putInt(key, it).apply() }

    private fun float(key: String, default: Float): ReadWriteProperty<Any?, Float> =
        StatePref(prefs.getFloat(key, default)) { prefs.edit().putFloat(key, it).apply() }

    private fun string(key: String, default: String): ReadWriteProperty<Any?, String> =
        StatePref(prefs.getString(key, default) ?: default) { prefs.edit().putString(key, it).apply() }

    /** null = follow the theme. */
    private fun color(key: String): ReadWriteProperty<Any?, Color?> =
        StatePref(prefs.getInt(key, 0).takeIf { it != 0 }?.let { Color(it) }) {
            prefs.edit().putInt(key, it?.toArgb() ?: 0).apply()
        }

    // ---- appearance -----------------------------------------------------------
    /** 0 = follow the system, 1 = light, 2 = dark. */
    var themeMode by int("theme_mode", 0)
    var accent by color("accent")
    var pageBg by color("page_bg")
    var barFill by color("bar_fill")
    var barBorder by color("bar_border")
    var barIcon by color("bar_icon")
    var textColor by color("text_color")
    var barAlpha by float("bar_alpha", 1f)
    var barRadius by float("bar_radius", 30f)
    var barHeight by float("bar_height", 51f)
    var uiScale by float("ui_scale", 1f)
    var matchStatusBar by bool("match_status", true)

    // ---- home page ------------------------------------------------------------
    var showBrand by bool("show_brand", true)
    var brandText by string("brand_text", "VOID")
    var brandSize by float("brand_size", 1f)
    var brandColor by color("brand_color")
    var homeBg by color("home_bg")
    var shortcutsTop by float("shortcuts_top", 17f)
    var brandTop by float("brand_top", 31f)
    var searchTop by float("search_top", 50f)
    var searchHeight by float("search_height", 47f)
    var searchRadius by float("search_radius", 40f)
    var searchHint by string("search_hint", "Search or enter address")

    // ---- the hamburger's tools ---------------------------------------------------
    private var menuToolsRaw by string("menu_tools", DefaultMenuTools.joinToString(","))

    val menuToolIds: List<String>
        get() = menuToolsRaw.split(",").filter { it.isNotBlank() }

    fun setMenuTools(ids: List<String>) {
        menuToolsRaw = ids.joinToString(",")
    }

    fun resetMenuTools() = setMenuTools(DefaultMenuTools)

    // ---- browser -------------------------------------------------------------------
    var jsEnabled by bool("js", true)
    var cookies by bool("cookies", true)
    var thirdPartyCookies by bool("third_cookies", true)
    var blockPopups by bool("block_popups", false)
    var textZoom by int("text_zoom", 100)
    var desktopDefault by bool("desktop_default", false)
    var autoplay by bool("autoplay", true)
    var loadImages by bool("images", true)
    var keepAwake by bool("keep_awake", false)
    var safeBrowsing by bool("safe_browsing", true)
    var httpsOnly by bool("https_only", false)
    var customUa by string("custom_ua", "")
    var debugging by bool("debugging", false)

    // ---- downloads -------------------------------------------------------------------
    var askDownload by bool("ask_download", true)
    var downloadFolder by string("download_folder", "VOID")

    // ---- resets --------------------------------------------------------------------
    fun resetAppearance() {
        themeMode = 0
        accent = null
        pageBg = null
        barFill = null
        barBorder = null
        barIcon = null
        textColor = null
        barAlpha = 1f
        barRadius = 30f
        barHeight = 51f
        uiScale = 1f
        matchStatusBar = true
    }

    fun resetHomePage() {
        showBrand = true
        brandText = "VOID"
        brandSize = 1f
        brandColor = null
        homeBg = null
        shortcutsTop = 17f
        brandTop = 31f
        searchTop = 50f
        searchHeight = 47f
        searchRadius = 40f
        searchHint = "Search or enter address"
    }

    fun resetBrowser() {
        jsEnabled = true
        cookies = true
        thirdPartyCookies = true
        blockPopups = false
        textZoom = 100
        desktopDefault = false
        autoplay = true
        loadImages = true
        keepAwake = false
        safeBrowsing = true
        httpsOnly = false
        customUa = ""
        debugging = false
    }
}
