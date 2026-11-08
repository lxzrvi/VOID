package dev.voidapp.ui.proto

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Icons come from the Material Icons dependency (filled, bold) - nothing here is
 * drawn by hand. [get] maps the ids used by the bars and the tools box.
 */
object ProtoIcons {
    @Suppress("UNUSED_PARAMETER")
    fun get(id: String, stroke: Float = 1.8f): ImageVector = when (id) {
        // ---- bars ----------------------------------------------------------
        "back" -> Icons.AutoMirrored.Filled.ArrowBack
        "forward" -> Icons.AutoMirrored.Filled.ArrowForward
        "home" -> Icons.Filled.Home
        "menu" -> Icons.Filled.Menu
        "close" -> Icons.Filled.Close
        "pillsearch", "search" -> Icons.Filled.Search
        "plus" -> Icons.Filled.Add
        // ---- tools: navigation -----------------------------------------------
        "reload" -> Icons.Filled.Refresh
        "stop" -> Icons.Filled.Cancel
        "closetab" -> Icons.Filled.Close
        "tabs" -> Icons.Filled.Tab
        "duplicate" -> Icons.Filled.ContentCopy
        "reopen" -> Icons.Filled.Restore
        "closeothers" -> Icons.Filled.ClearAll
        // ---- tools: page ---------------------------------------------------------
        "find" -> Icons.Filled.FindInPage
        "desktop" -> Icons.Filled.Computer
        "translate" -> Icons.Filled.Translate
        "addbookmark" -> Icons.Filled.BookmarkAdd
        "bookmarks" -> Icons.Filled.Bookmarks
        "history" -> Icons.Filled.History
        "downloads" -> Icons.Filled.Download
        "share" -> Icons.Filled.Share
        "copylink" -> Icons.Filled.Link
        "external" -> Icons.AutoMirrored.Filled.OpenInNew
        "print" -> Icons.Filled.Print
        "top" -> Icons.Filled.VerticalAlignTop
        // ---- tools: browser ---------------------------------------------------------
        "bottom" -> Icons.Filled.VerticalAlignBottom
        "zoomin" -> Icons.Filled.ZoomIn
        "zoomout" -> Icons.Filled.ZoomOut
        "zoomreset" -> Icons.Filled.ZoomOutMap
        "fullscreen" -> Icons.Filled.Fullscreen
        "darkpages" -> Icons.Filled.Contrast
        "privacy" -> Icons.Filled.Shield
        "theme" -> Icons.Filled.DarkMode
        "notes" -> Icons.Filled.StickyNote2
        "cleardata" -> Icons.Filled.DeleteSweep
        "addshortcut" -> Icons.Filled.AddBox
        "settings" -> Icons.Filled.Settings
        else -> Icons.Filled.Search
    }
}

/** The prototype's colours, with a matching dark set (the app keeps its dark theme). */
@Immutable
class ProtoColors(
    val page: Color,
    val ink: Color,
    val line: Color,
    val soft: Color,
    val iconBg: Color,
    val label: Color,
    val centerBorder: Color,
    val centerFocus: Color,
    val navIcon: Color,
    val menuDot: Color,
    val tabIdle: Color,
    val tabActive: Color,
    val addBorder: Color,
    val addIcon: Color,
    val placeholder: Color,
    val boxFill: Color,
    val rowActive: Color,
    val dotIdle: Color,
    val dotActive: Color,
    val tooltip: Color,
    val tooltipText: Color,
    val press: Color,
    val circle: Color,
)

@Composable
fun protoColors(): ProtoColors {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (!dark) {
        ProtoColors(
            page = Color(0xFFFFFFFF), ink = Color(0xFF171717), line = Color(0xFFE8E8E8),
            soft = Color(0xFFF5F5F5), iconBg = Color(0xFFF3F3F3), label = Color(0xFF555555),
            centerBorder = Color(0xFFDDDDDD), centerFocus = Color(0xFFAAAAAA),
            navIcon = Color(0xFF555555), menuDot = Color(0xFFE9E9E9),
            tabIdle = Color(0xFF888888), tabActive = Color(0xFF222222),
            addBorder = Color(0xFFE4E4E4), addIcon = Color(0xFF777777),
            placeholder = Color(0xFF777777), boxFill = Color(0xFAFFFFFF),
            rowActive = Color(0xFFF1F1F1), dotIdle = Color(0xFFD7D7D7),
            dotActive = Color(0xFF222222), tooltip = Color(0xFF222222),
            tooltipText = Color.White, press = Color(0xFFF1F1F1),
            circle = Color(0xFF171717),
        )
    } else {
        ProtoColors(
            page = Color(0xFF0A0A0B), ink = Color(0xFFF5F5F5), line = Color(0xFF2E2E33),
            soft = Color(0xFF141416), iconBg = Color(0xFF1C1C1F), label = Color(0xFF9C9CA3),
            centerBorder = Color(0xFF2E2E33), centerFocus = Color(0xFF5A5A62),
            navIcon = Color(0xFFB4B4BB), menuDot = Color(0xFF232327),
            tabIdle = Color(0xFF8A8A92), tabActive = Color(0xFFEDEDED),
            addBorder = Color(0xFF2E2E33), addIcon = Color(0xFF9C9CA3),
            placeholder = Color(0xFF8A8A92), boxFill = Color(0xFA141416),
            rowActive = Color(0xFF1F1F23), dotIdle = Color(0xFF3A3A40),
            dotActive = Color(0xFFEDEDED), tooltip = Color(0xFFEDEDED),
            tooltipText = Color(0xFF0A0A0B), press = Color(0xFF1F1F23),
            circle = Color(0xFF2B2B30),
        )
    }
}
