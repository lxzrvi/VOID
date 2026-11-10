package dev.voidapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.proto.ProtoColors
import dev.voidapp.ui.proto.ProtoIcons
import dev.voidapp.ui.proto.ProtoToolById
import dev.voidapp.ui.proto.ProtoToolCatalog
import dev.voidapp.ui.proto.TOOLS_PER_PAGE
import dev.voidapp.ui.theme.Poppins
import kotlin.math.roundToInt

/** Settings > Appearance: theme, colours, bars, text size, status bar. */
@Composable
internal fun AppearanceSettings(state: VoidAppState, c: ProtoColors) {
    val ui = state.ui

    SectionTitle(c, "THEME")
    Panel(c) {
        ChoiceRow(c, listOf("System", "Light", "Dark"), ui.themeMode) { ui.themeMode = it }
    }

    SectionTitle(c, "COLOURS")
    Panel(c) {
        ColorRow(c, "Accent (progress line, switches on)", ui.accent) { ui.accent = it }
        ColorRow(c, "App background", ui.pageBg) { ui.pageBg = it }
        ColorRow(c, "Bar fill", ui.barFill) { ui.barFill = it }
        ColorRow(c, "Bar border", ui.barBorder) { ui.barBorder = it }
        ColorRow(c, "Bar icons", ui.barIcon) { ui.barIcon = it }
        ColorRow(c, "Text", ui.textColor) { ui.textColor = it }
    }

    SectionTitle(c, "BARS")
    Panel(c) {
        SliderRow(c, "Transparency", "${(ui.barAlpha * 100).roundToInt()}%", ui.barAlpha, 0.2f..1f) { ui.barAlpha = it }
        SliderRow(c, "Corner radius", "${ui.barRadius.roundToInt()} dp", ui.barRadius, 0f..30f) { ui.barRadius = it }
        SliderRow(c, "Height", "${ui.barHeight.roundToInt()} dp", ui.barHeight, 40f..64f) { ui.barHeight = it }
    }

    SectionTitle(c, "TEXT SIZE")
    Panel(c) {
        SliderRow(c, "Everything in the app", "${(ui.uiScale * 100).roundToInt()}%", ui.uiScale, 0.8f..1.3f) { ui.uiScale = it }
    }

    SectionTitle(c, "STATUS BAR")
    Panel(c) {
        SwitchRow(c, "Match the page's top colour", ui.matchStatusBar) { ui.matchStatusBar = it }
    }
    Note(c, "On: the status bar wears the colour of each site's top and scrolls away with the page. Off: it keeps the app's background.")

    Spacer(Modifier.height(4.dp))
    ActionButton(c, "Reset appearance") { ui.resetAppearance() }
}

/** Settings > Hamburger menu: reorder, remove and add tools. */
@Composable
internal fun MenuSettings(state: VoidAppState, c: ProtoColors) {
    val ids = state.ui.menuToolIds
    val tools = ids.mapNotNull { ProtoToolById[it] }
    val pages = (tools.size + TOOLS_PER_PAGE - 1) / TOOLS_PER_PAGE

    Note(c, "${tools.size} tools on $pages page${if (pages == 1) "" else "s"} (12 per page: 3 columns x 4 rows).")

    SectionTitle(c, "IN THE MENU")
    Panel(c) {
        if (tools.isEmpty()) Note(c, "Nothing here. Add tools below.")
        tools.forEachIndexed { index, tool ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(ProtoIcons.get(tool.icon), null, tint = c.navIcon, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(tool.name, color = c.ink, fontFamily = Poppins, fontSize = 13.sp)
                    Text("Page ${index / TOOLS_PER_PAGE + 1}", color = c.tabIdle, fontFamily = Poppins, fontSize = 10.5.sp)
                }
                RowIcon(Icons.Filled.KeyboardArrowUp, c, enabled = index > 0) {
                    state.ui.setMenuTools(ids.toMutableList().also { list ->
                        val moved = list.removeAt(index)
                        list.add(index - 1, moved)
                    })
                }
                RowIcon(Icons.Filled.KeyboardArrowDown, c, enabled = index < tools.lastIndex) {
                    state.ui.setMenuTools(ids.toMutableList().also { list ->
                        val moved = list.removeAt(index)
                        list.add(index + 1, moved)
                    })
                }
                RowIcon(Icons.Filled.Delete, c, enabled = true) {
                    state.ui.setMenuTools(ids.filter { it != tool.id })
                }
            }
        }
    }

    SectionTitle(c, "AVAILABLE")
    Panel(c) {
        val rest = ProtoToolCatalog.filter { it.id !in ids }
        if (rest.isEmpty()) Note(c, "Every tool is already in the menu.")
        rest.forEach { tool ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(ProtoIcons.get(tool.icon), null, tint = c.navIcon, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(tool.name, color = c.ink, fontFamily = Poppins, fontSize = 13.sp, modifier = Modifier.weight(1f))
                RowIcon(Icons.Filled.Add, c, enabled = true) { state.ui.setMenuTools(ids + tool.id) }
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    ActionButton(c, "Reset to the default 48 tools") { state.ui.resetMenuTools() }
}
