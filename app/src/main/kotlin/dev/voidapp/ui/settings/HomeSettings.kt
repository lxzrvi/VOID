package dev.voidapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.proto.ProtoColors
import dev.voidapp.ui.theme.Poppins
import kotlin.math.roundToInt

/** Settings > Home page: boxes, wordmark, layout, search pill, background, what is in the boxes. */
@Composable
internal fun HomePageSettings(state: VoidAppState, c: ProtoColors) {
    val ui = state.ui

    SectionTitle(c, "BOXES")
    BoxPreview(state, c)
    Panel(c) {
        SliderRow(c, "Boxes per row", state.homeCount.toString(), state.homeCount.toFloat(), 4f..12f, steps = 7) {
            state.updateHomeCount(it.roundToInt())
        }
        SliderRow(c, "Box transparency", "${(state.boxAlpha * 100).roundToInt()}%", state.boxAlpha, 0.1f..1f) {
            state.updateBoxAlpha(it)
        }
        SliderRow(c, "Corner radius", "${state.boxRadius.roundToInt()} dp", state.boxRadius, 0f..30f) {
            state.updateBoxRadius(it)
        }
        ColorRow(c, "Box colour", state.boxColor) { state.updateBoxColor(it) }
        ColorRow(c, "Letter colour", state.boxTextColor) { state.updateBoxTextColor(it) }
    }

    SectionTitle(c, "WORDMARK")
    Panel(c) {
        SwitchRow(c, "Show the wordmark", ui.showBrand) { ui.showBrand = it }
        TextRow(c, "Text", ui.brandText, "VOID") { ui.brandText = it }
        SliderRow(c, "Size", "${(ui.brandSize * 100).roundToInt()}%", ui.brandSize, 0.5f..2f) { ui.brandSize = it }
        ColorRow(c, "Colour", ui.brandColor) { ui.brandColor = it }
    }

    SectionTitle(c, "LAYOUT")
    Panel(c) {
        SliderRow(c, "Boxes position", "${ui.shortcutsTop.roundToInt()}%", ui.shortcutsTop, 4f..45f) { ui.shortcutsTop = it }
        SliderRow(c, "Wordmark position", "${ui.brandTop.roundToInt()}%", ui.brandTop, 12f..60f) { ui.brandTop = it }
        SliderRow(c, "Search position", "${ui.searchTop.roundToInt()}%", ui.searchTop, 25f..78f) { ui.searchTop = it }
    }

    SectionTitle(c, "SEARCH PILL")
    Panel(c) {
        SliderRow(c, "Height", "${ui.searchHeight.roundToInt()} dp", ui.searchHeight, 36f..72f) { ui.searchHeight = it }
        SliderRow(c, "Corner radius", "${ui.searchRadius.roundToInt()} dp", ui.searchRadius, 0f..40f) { ui.searchRadius = it }
        TextRow(c, "Placeholder text", ui.searchHint, "Search or enter address") { ui.searchHint = it }
    }

    SectionTitle(c, "BACKGROUND")
    Panel(c) {
        ColorRow(c, "Homepage colour", ui.homeBg) { ui.homeBg = it }
    }

    SectionTitle(c, "ADD TO HOME")
    AddWebsite(state, c)
    AddFromBookmarks(state, c)

    SectionTitle(c, "YOUR BOXES")
    YourBoxes(state, c)

    Spacer(Modifier.height(4.dp))
    ActionButton(c, "Reset home page") {
        ui.resetHomePage()
        state.updateHomeCount(9)
        state.updateBoxAlpha(1f)
        state.updateBoxRadius(7f)
        state.updateBoxColor(null)
        state.updateBoxTextColor(null)
    }
}

/** The Homepage row exactly as it looks with the current settings. */
@Composable
private fun BoxPreview(state: VoidAppState, c: ProtoColors) {
    val sites = state.homeSites
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (index in 0 until state.homeCount) {
            val site = sites.getOrNull(index)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(state.boxRadius.dp))
                    .background((state.boxColor ?: c.iconBg).copy(alpha = state.boxAlpha)),
                contentAlignment = Alignment.Center,
            ) {
                if (site != null) {
                    Text(
                        text = site.name.trim().take(1).uppercase().ifEmpty { "?" },
                        color = state.boxTextColor ?: c.label,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium,
                        fontSize = (200f / state.homeCount).coerceIn(12f, 24f).sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddWebsite(state: VoidAppState, c: ProtoColors) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    Panel(c) {
        Text("Add a website", color = c.ink, fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Field(c, name, "Name (its first letter shows)") { name = it }
        Spacer(Modifier.height(6.dp))
        Field(c, url, "Address, e.g. example.com") { url = it }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(c.ink)
                .clickable {
                    if (url.isNotBlank()) {
                        state.addSite(name, url)
                        state.showToast("Added to your boxes")
                        name = ""
                        url = ""
                    }
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Filled.Add, null, tint = c.page, modifier = Modifier.size(18.dp))
            Text("Add", color = c.page, fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun AddFromBookmarks(state: VoidAppState, c: ProtoColors) {
    val used = state.sites.map { it.url }.toSet()
    val available = state.bookmarks.filter { it.url !in used }
    Panel(c) {
        Text("Add from bookmarks", color = c.ink, fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Spacer(Modifier.height(2.dp))
        if (available.isEmpty()) {
            Text(
                text = if (state.bookmarks.isEmpty()) "No bookmarks yet." else "All your bookmarks are already boxes.",
                color = c.tabIdle,
                fontFamily = Poppins,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            available.take(30).forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { state.addSite(entry.label, entry.url) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.label, color = c.ink, fontFamily = Poppins, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(entry.host, color = c.tabIdle, fontFamily = Poppins, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Filled.Add, "Add", tint = c.ink, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun YourBoxes(state: VoidAppState, c: ProtoColors) {
    val sites = state.visibleSites
    Panel(c) {
        if (sites.isEmpty()) {
            Text("No boxes yet.", color = c.tabIdle, fontFamily = Poppins, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
        sites.forEachIndexed { index, site ->
            val onHome = index < state.homeCount
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(state.boxRadius.dp.coerceAtMost(17.dp)))
                        .background((state.boxColor ?: c.iconBg).copy(alpha = if (onHome) state.boxAlpha else 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = site.name.trim().take(1).uppercase().ifEmpty { "?" },
                        color = state.boxTextColor ?: c.label,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(site.name, color = c.ink, fontFamily = Poppins, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = if (onHome) "On home" else "Not on home",
                        color = c.tabIdle,
                        fontFamily = Poppins,
                        fontSize = 10.5.sp,
                    )
                }
                RowIcon(Icons.Filled.KeyboardArrowUp, c, enabled = index > 0) { state.moveSite(index, index - 1) }
                RowIcon(Icons.Filled.KeyboardArrowDown, c, enabled = index < sites.lastIndex) { state.moveSite(index, index + 1) }
                RowIcon(Icons.Filled.Delete, c, enabled = true) { state.removeSite(site) }
            }
        }
    }
}

@Composable
internal fun RowIcon(icon: ImageVector, c: ProtoColors, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (enabled) c.ink else c.line, modifier = Modifier.size(22.dp))
    }
}
