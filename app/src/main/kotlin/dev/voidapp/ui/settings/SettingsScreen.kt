package dev.voidapp.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voidapp.data.SearchProvider
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.proto.ProtoColors
import dev.voidapp.ui.proto.protoColors
import dev.voidapp.ui.theme.Poppins
import kotlin.math.roundToInt

private val Palette: List<Color?> = listOf(
    null,
    Color(0xFFF3F3F3), Color(0xFFFFFFFF), Color(0xFF171717),
    Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835), Color(0xFF43A047),
    Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFEC407A), Color(0xFF6D4C41),
)

/**
 * Settings: a full screen page. Home boxes (how many per row, look, what is in
 * them), the browser's switches, appearance and data.
 */
@Composable
fun SettingsScreen(state: VoidAppState, modifier: Modifier = Modifier) {
    val c = protoColors()
    var showProviders by remember { mutableStateOf(false) }

    BackHandler(enabled = true) { state.settingsOpen = false }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.page)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // ---- top bar -------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(c.soft)
                    .clickable { state.settingsOpen = false },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = c.ink, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("Settings", color = c.ink, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ======================= HOME BOXES =======================
            SectionTitle(c, "HOME BOXES")
            BoxPreview(state, c)
            Panel(c) {
                SliderRow(
                    c = c,
                    label = "Boxes per row",
                    shown = state.homeCount.toString(),
                    value = state.homeCount.toFloat(),
                    range = 4f..12f,
                    steps = 7,
                ) { state.updateHomeCount(it.roundToInt()) }
                SliderRow(
                    c = c,
                    label = "Box transparency",
                    shown = "${(state.boxAlpha * 100).roundToInt()}%",
                    value = state.boxAlpha,
                    range = 0.1f..1f,
                    steps = 0,
                ) { state.updateBoxAlpha(it) }
                SliderRow(
                    c = c,
                    label = "Corner radius",
                    shown = "${state.boxRadius.roundToInt()} dp",
                    value = state.boxRadius,
                    range = 0f..30f,
                    steps = 0,
                ) { state.updateBoxRadius(it) }
                ColorRow(c, "Box colour", state.boxColor) { state.updateBoxColor(it) }
                ColorRow(c, "Letter colour", state.boxTextColor) { state.updateBoxTextColor(it) }
            }

            SectionTitle(c, "ADD TO HOME")
            AddWebsite(state, c)
            AddFromBookmarks(state, c)

            SectionTitle(c, "YOUR BOXES")
            YourBoxes(state, c)

            // ======================= BROWSER =======================
            SectionTitle(c, "BROWSER")
            Panel(c) {
                ValueRow(c, "Search engine", state.provider.label) { showProviders = true }
                SwitchRow(c, "Dark web pages", state.webDark) { state.updateWebDark(it) }
                SwitchRow(c, "Block ads & trackers", state.blockAds) { state.updateBlockAds(it) }
                SwitchRow(c, "Search suggestions", state.suggestionsEnabled) { state.updateSuggestionsEnabled(it) }
                SwitchRow(c, "Desktop site (this tab)", state.desktopMode, enabled = state.browserOpen) {
                    state.updateDesktopMode(it)
                }
            }

            SectionTitle(c, "APPEARANCE")
            Panel(c) {
                SwitchRow(c, "Dark theme", state.darkTheme) { state.updateDarkTheme(it) }
            }

            SectionTitle(c, "DATA")
            Panel(c) {
                ValueRow(c, "History", "") {
                    state.openHistory()
                }
                ValueRow(c, "Bookmarks", "") {
                    state.openBookmarks()
                }
                ValueRow(c, "Clear browsing data", "") { state.clearBrowsingData() }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showProviders) {
        AlertDialog(
            onDismissRequest = { showProviders = false },
            title = { Text("Search with", fontFamily = Poppins) },
            text = {
                Column {
                    SearchProvider.entries.forEach { provider ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    state.selectProvider(provider)
                                    showProviders = false
                                }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = provider.label,
                                fontFamily = Poppins,
                                fontSize = 14.sp,
                                fontWeight = if (provider == state.provider) FontWeight.SemiBold else FontWeight.Normal,
                            )
                            if (provider == state.provider) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Filled.Check, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showProviders = false }) { Text("Close", fontFamily = Poppins) } },
        )
    }
}

// =============================================================================
// building blocks
// =============================================================================

@Composable
private fun SectionTitle(c: ProtoColors, text: String) {
    Text(
        text = text,
        color = c.tabIdle,
        fontFamily = Poppins,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.3.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}

@Composable
private fun Panel(c: ProtoColors, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.soft)
            .border(1.dp, c.line, shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        content()
    }
}

/** The Homepage row exactly as it looks with the current settings. */
@Composable
private fun BoxPreview(state: VoidAppState, c: ProtoColors) {
    val sites = state.homeSites
    val gap = 6.dp
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
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
private fun SliderRow(
    c: ProtoColors,
    label: String,
    shown: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = c.ink, fontFamily = Poppins, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(shown, color = c.tabIdle, fontFamily = Poppins, fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = c.ink,
                activeTrackColor = c.ink,
                inactiveTrackColor = c.line,
                activeTickColor = c.page,
                inactiveTickColor = c.tabIdle,
            ),
        )
    }
}

@Composable
private fun ColorRow(c: ProtoColors, label: String, selected: Color?, onPick: (Color?) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, color = c.ink, fontFamily = Poppins, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Palette.forEach { color ->
                val isSelected = color == selected
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color ?: c.page)
                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) c.ink else c.line, CircleShape)
                        .clickable { onPick(color) },
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        color == null -> Text("A", color = c.ink, fontFamily = Poppins, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        color != null && isSelected -> Icon(
                            Icons.Filled.Check,
                            null,
                            tint = if (color.red * 0.3f + color.green * 0.6f + color.blue * 0.1f > 0.55f) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    c: ProtoColors,
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = if (enabled) c.ink else c.tabIdle,
            fontFamily = Poppins,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.ink,
                checkedThumbColor = c.page,
                checkedBorderColor = c.ink,
                uncheckedTrackColor = c.line,
                uncheckedThumbColor = c.page,
                uncheckedBorderColor = c.line,
            ),
        )
    }
}

@Composable
private fun ValueRow(c: ProtoColors, label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = c.ink, fontFamily = Poppins, fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, color = c.tabIdle, fontFamily = Poppins, fontSize = 12.sp)
    }
}

// =============================================================================
// adding and arranging boxes
// =============================================================================

@Composable
private fun AddWebsite(state: VoidAppState, c: ProtoColors) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    Panel(c) {
        Text("Add a website", color = c.ink, fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Field(c, name, "Name (the first letter shows)") { name = it }
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
private fun Field(c: ProtoColors, value: String, hint: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.page)
            .border(1.dp, c.line, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(hint, color = c.placeholder, fontFamily = Poppins, fontSize = 13.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = c.ink, fontFamily = Poppins, fontSize = 13.sp),
            cursorBrush = SolidColor(c.ink),
            modifier = Modifier.fillMaxWidth(),
        )
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
private fun RowIcon(icon: ImageVector, c: ProtoColors, enabled: Boolean, onClick: () -> Unit) {
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
