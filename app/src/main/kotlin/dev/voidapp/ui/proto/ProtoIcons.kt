package dev.voidapp.ui.proto

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The exact SVG glyphs of the VOID prototype (24x24, round caps and joins, stroke
 * only), parsed from the same path data. [get] takes the stroke width because the
 * prototype uses 1.7 in the tools box, 1.8 in the nav pill and 2 on the tab "x".
 */
object ProtoIcons {
    private val cache = HashMap<String, ImageVector>()

    fun get(id: String, stroke: Float = 1.8f): ImageVector =
        cache.getOrPut("$id@$stroke") { build(id, stroke) }

    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${cx - r} $cy a$r $r 0 1 0 ${2 * r} 0 a$r $r 0 1 0 ${-2 * r} 0"

    private fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float): String =
        "M${x + rx} $y h${w - 2 * rx} a$rx $rx 0 0 1 $rx $rx v${h - 2 * rx} " +
            "a$rx $rx 0 0 1 ${-rx} $rx h${-(w - 2 * rx)} a$rx $rx 0 0 1 ${-rx} ${-rx} " +
            "v${-(h - 2 * rx)} a$rx $rx 0 0 1 $rx ${-rx} z"

    private const val BOOKMARK = "M6 3h12v18l-6-4-6 4z"
    private const val TABS_LINES = "M8 8h8M8 12h8"

    private fun pathsFor(id: String): List<String> = when (id) {
        // ---- nav pill / bars ---------------------------------------------
        "tabs" -> listOf(rect(4f, 4f, 16f, 16f, 2f), TABS_LINES)
        "home" -> listOf("m3 10 9-7 9 7v10H3z")
        "back" -> listOf("m15 18-6-6 6-6")
        "forward" -> listOf("m9 18 6-6-6-6")
        "menu" -> listOf("M5 12h14")
        "close" -> listOf("m6 6 12 12M18 6 6 18")
        "pillsearch" -> listOf(circle(10.8f, 10.8f, 7.4f), "m16.3 16.3 5 5")
        "plus" -> listOf("M12 5v14M5 12h14")
        // ---- tools, page 1 -----------------------------------------------
        "reload" -> listOf("M20 7v5h-5M20 12a8 8 0 1 1-2.3-5.7")
        "search" -> listOf(circle(10.5f, 10.5f, 7f), "m16 16 5 5")
        "closetab" -> listOf("M5 5h14v14H5zM8 8l8 8m0-8-8 8")
        "history" -> listOf(circle(12f, 12f, 9f), "M12 7v5l3 2")
        "bookmarks" -> listOf(BOOKMARK)
        "downloads" -> listOf("M12 3v12m-5-5 5 5 5-5M4 19h16")
        "share" -> listOf(
            circle(18f, 5f, 3f), circle(6f, 12f, 3f), circle(18f, 19f, 3f),
            "m9 11 6-5M9 13l6 5",
        )
        // ---- tools, page 2 -----------------------------------------------
        "find" -> listOf(circle(10f, 10f, 6f), "m15 15 5 5M8 10h4")
        "desktop" -> listOf(rect(3f, 4f, 18f, 13f, 2f), "M8 21h8m-4-4v4")
        "translate" -> listOf("M3 5h12M9 3v2m-4 4c2 3 4 5 8 7m-5-7c-1 3-3 6-6 8m12-5 5 9m-8 0 5-9")
        "settings" -> listOf(
            circle(12f, 12f, 3f),
            "m19 13 2 2-2 4-3-1-2 2h-4l-1-3-3-1-1-4 2-2V7l4-2 2 2h3l2 3z",
        )
        "privacy" -> listOf("M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11z", "m9 12 2 2 4-4")
        "reader" -> listOf("M4 5h16M4 10h16M4 15h10M4 20h10")
        "print" -> listOf("M6 9V3h12v6M6 18H4V9h16v9h-2M6 14h12v7H6z")
        "addbookmark" -> listOf(BOOKMARK)
        "cleardata" -> listOf("M4 7h16M10 11v6m4-6v6M6 7l1 14h10l1-14M9 7V4h6v3")
        "fullscreen" -> listOf("M8 3H3v5m13-5h5v5M3 16v5h5m13-5v5h-5")
        "zoomin" -> listOf(circle(10f, 10f, 7f), "M10 6v8m-4-4h8m2 2 5 5")
        "zoomout" -> listOf(circle(10f, 10f, 7f), "M6 10h8m2 2 5 5")
        else -> emptyList()
    }

    private fun build(id: String, stroke: Float): ImageVector {
        val builder = ImageVector.Builder(
            name = id,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        pathsFor(id).forEach { d ->
            builder.addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = stroke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
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
        )
    }
}
