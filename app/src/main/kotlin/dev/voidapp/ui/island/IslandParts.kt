package dev.voidapp.ui.island

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.voidapp.R
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.components.ReorderGrid
import dev.voidapp.ui.icons.Lucide
import dev.voidapp.ui.theme.Poppins
import dev.voidapp.ui.theme.VoidRed
import androidx.compose.ui.res.stringResource

private val SheetShape = RoundedCornerShape(22.dp)
private val CellShape = RoundedCornerShape(14.dp)
private val Cell = 52.dp

/** One tool: a glyph, a label and what it does. */
data class ToolSpec(val key: String, val label: String, val icon: ImageVector)

@Composable
fun toolSpecs(state: VoidAppState): List<ToolSpec> = listOf(
    ToolSpec("back", stringResource(R.string.tool_back), Lucide.ArrowLeft),
    ToolSpec("forward", stringResource(R.string.tool_forward), Lucide.ArrowRight),
    ToolSpec("reload", stringResource(R.string.tool_reload), Lucide.RotateCw),
    ToolSpec("home", stringResource(R.string.tool_home), Lucide.House),
    ToolSpec("tabs", stringResource(R.string.tool_tabs), Lucide.Layers),
    ToolSpec("notes", stringResource(R.string.tool_notes), Lucide.FileText),
    ToolSpec("history", stringResource(R.string.tool_history), Lucide.History),
    ToolSpec("bookmark", stringResource(R.string.tool_bookmark), Lucide.Bookmark),
    ToolSpec("share", stringResource(R.string.tool_share), Lucide.Share2),
    ToolSpec("copy", stringResource(R.string.tool_copy), Lucide.Copy),
    ToolSpec("external", stringResource(R.string.tool_external), Lucide.ExternalLink),
    ToolSpec("desktop", stringResource(R.string.tool_desktop), Lucide.Monitor),
    ToolSpec("zoom_in", stringResource(R.string.tool_zoom_in), Lucide.ZoomIn),
    ToolSpec("zoom_out", stringResource(R.string.tool_zoom_out), Lucide.ZoomOut),
    ToolSpec("find", stringResource(R.string.tool_find), Lucide.Search),
    ToolSpec("top", stringResource(R.string.tool_top), Lucide.ArrowUp),
    ToolSpec("settings", stringResource(R.string.tool_settings), Lucide.Settings),
    ToolSpec("clear", stringResource(R.string.tool_clear), Lucide.Trash2),
    ToolSpec("save", stringResource(R.string.tool_save), Lucide.Save),
    ToolSpec("theme", stringResource(R.string.tool_theme), Lucide.Moon),
    ToolSpec("newtab", stringResource(R.string.tool_newtab), Lucide.Plus),
    ToolSpec("bookmarks", stringResource(R.string.tool_bookmarks), Lucide.Star),
    ToolSpec("downloads", stringResource(R.string.tool_downloads), Lucide.Download),
    ToolSpec("translate", stringResource(R.string.tool_translate), Lucide.Globe),
)

/** Runs a tool. Plain function: it is called from click handlers. */
private fun runTool(state: VoidAppState, key: String) {
    when (key) {
        "back" -> state.browserBack()
        "forward" -> state.browserForward()
        "reload" -> state.browserReload()
        "home" -> state.goHome()
        "tabs" -> state.openTabs()
        "notes" -> state.openNotes()
        "history" -> state.openHistory()
        "bookmark" -> state.bookmarkCurrent()
        "bookmark_page" -> state.bookmarkCurrent()
        "share" -> state.shareCurrent()
        "copy" -> state.copyCurrent()
        "external" -> state.openCurrentExternally()
        "desktop" -> state.toggleDesktopMode()
        "zoom_in" -> state.zoomBy(0.15f)
        "zoom_out" -> state.zoomBy(-0.15f)
        "find" -> state.findInPage()
        "top" -> state.scrollToTop()
        "settings" -> state.openSettings()
        "clear" -> state.clearBrowsingData()
        "save" -> state.saveCurrent()
        "theme" -> state.toggleTheme()
        "newtab" -> state.startNewTab()
        "bookmarks" -> state.openBookmarks()
        "downloads" -> state.openDownloads()
        "translate" -> state.translatePage()
        else -> Unit
    }
}

/** Rounded container used by every island sheet. */
@Composable
private fun Sheet(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(SheetShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, SheetShape)
            .padding(12.dp),
    ) {
        content()
    }
}

// ===========================================================================
// Tools: grid that fits its contents, long press to reorder
// ===========================================================================

@Composable
fun ToolsPanel(state: VoidAppState, modifier: Modifier = Modifier) {
    val specs = toolSpecs(state)
    val ordered = state.toolOrder.mapNotNull { key -> specs.firstOrNull { it.key == key } }

    // Pick the column count that leaves no short row (24 tools -> 6 x 4); if
    // nothing divides evenly the last row is stretched across the full width.
    val columns = listOf(6, 5, 4).firstOrNull { ordered.size % it == 0 } ?: 5

    Sheet(modifier = modifier) {
        Column {
            ToolSectionLabel(stringResource(R.string.tool_section_common))
            Spacer(Modifier.height(8.dp))
            ReorderGrid(
                items = ordered,
                itemKey = { it.key },
                columns = columns,
                cellHeight = 56.dp,
                gap = 8.dp,
                onMove = { from, to -> state.moveTool(from, to) },
            ) { tool, _, lifted ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CellShape)
                        .background(
                            if (lifted) VoidRed.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                        )
                        .clickable(enabled = !lifted) {
                            runTool(state, tool.key)
                            if (tool.key != "tabs" && tool.key != "settings" && tool.key != "notes") {
                                state.hidePanel()
                            }
                        },
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.label,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = tool.label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = Poppins,
                        fontSize = 8.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.tools_reorder_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = Poppins,
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun ToolSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = Poppins,
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
    )
}

// ===========================================================================
// Navigator: results / history / bookmarks list
// ===========================================================================

@Composable
fun NavigatorSheet(
    results: List<String>,
    selected: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Sheet {
        Column(modifier = Modifier.fillMaxWidth()) {
            ToolSectionLabel(stringResource(R.string.navigator))
            Spacer(Modifier.height(8.dp))
            if (results.isEmpty()) {
                Text(
                    stringResource(R.string.nothing_here),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Poppins,
                    fontSize = 11.sp,
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                    itemsIndexed(results) { i, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (i == selected) VoidRed.copy(alpha = 0.12f) else Color.Transparent,
                                )
                                .clickable { onPick(i) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${i + 1}",
                                color = if (i == selected) VoidRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = Poppins,
                                fontSize = 10.sp,
                                modifier = Modifier.width(18.dp),
                            )
                            Text(
                                text = item,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = Poppins,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ===========================================================================
// Settings
// ===========================================================================

@Composable
fun SettingsSheet(state: VoidAppState, onDismiss: () -> Unit) {
    Sheet {
        Column(modifier = Modifier.width(250.dp)) {
            ToolSectionLabel(stringResource(R.string.settings))
            Spacer(Modifier.height(6.dp))
            SettingRow(
                label = stringResource(R.string.setting_dark),
                checked = state.darkTheme,
                onChange = { state.updateDarkTheme(it) },
            )
            SettingRow(
                label = stringResource(R.string.setting_desktop),
                checked = state.desktopMode,
                onChange = { state.updateDesktopMode(it) },
            )
            SettingRow(
                label = stringResource(R.string.setting_web_dark),
                checked = state.webDark,
                onChange = { state.updateWebDark(it) },
            )
            SettingRow(
                label = stringResource(R.string.setting_adblock),
                checked = state.blockAds,
                onChange = { state.updateBlockAds(it) },
            )
            SettingRow(
                label = stringResource(R.string.setting_suggestions),
                checked = state.suggestionsEnabled,
                onChange = { state.updateSuggestionsEnabled(it) },
            )
            SettingRow(
                label = stringResource(R.string.setting_island_right),
                checked = state.islandOnRight,
                onChange = { state.placeIsland(state.islandOffset, it) },
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniButton(stringResource(R.string.clear_browsing)) { state.clearBrowsingData() }
                MiniButton(stringResource(R.string.tool_clear_notes)) { state.clearNotes() }
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 38.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontFamily = Poppins, fontSize = 11.5.sp)
        Spacer(Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun MiniButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontFamily = Poppins, fontSize = 10.sp)
    }
}

// ===========================================================================
// Notes (the same list as the home Notes button)
// ===========================================================================

@Composable
fun NotesPanel(state: VoidAppState, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    Sheet {
        Column(modifier = Modifier.width(250.dp)) {
            ToolSectionLabel(stringResource(R.string.notes))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 11.dp),
                ) {
                    if (draft.isEmpty()) {
                        Text(
                            stringResource(R.string.notes_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = Poppins,
                            fontSize = 11.sp,
                        )
                    }
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = Poppins,
                            fontSize = 11.5.sp,
                        ),
                        cursorBrush = SolidColor(VoidRed),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(VoidRed)
                        .clickable {
                            state.addNote(draft)
                            draft = ""
                            focus.clearFocus()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Lucide.Plus, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            if (state.notes.isEmpty()) {
                Text(
                    stringResource(R.string.notes_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Poppins,
                    fontSize = 10.sp,
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(state.notes, key = { it.id }) { note ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 9.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                note.text,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = Poppins,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                Lucide.X,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { state.removeNote(note) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ===========================================================================
// Bar long-press menu
// ===========================================================================

@Composable
fun BarMenuDialog(
    state: VoidAppState,
    query: String,
    onDismiss: () -> Unit,
    onExpand: () -> Unit,
) {
    val url = if (state.browserOpen) state.currentUrl else state.resolve(query)
    val items = listOf(
        Triple(stringResource(R.string.menu_copy), Lucide.Copy) { state.copyLink(url) },
        Triple(stringResource(R.string.menu_share), Lucide.Share2) { state.shareLink(url) },
        Triple(stringResource(R.string.menu_shortcut), Lucide.Plus) { state.makeShortcut(url) },
        Triple(stringResource(R.string.menu_new_tab), Lucide.Layers) { state.openInNewTab(url) },
        Triple(stringResource(R.string.menu_close_tab), Lucide.X) { state.closeActiveTab() },
        Triple(stringResource(R.string.menu_refresh), Lucide.RotateCw) { state.browserReload() },
        Triple(stringResource(R.string.menu_bookmark), Lucide.Bookmark) { state.toggleBookmark(url, url) },
        Triple(stringResource(R.string.menu_external), Lucide.ExternalLink) { state.openCurrentExternally() },
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SheetShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, SheetShape)
                .padding(10.dp),
        ) {
            Column {
                Text(
                    text = url,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Poppins,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                )
                items.forEach { (label, icon, action) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                action()
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            icon,
                            null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            label,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = Poppins,
                            fontSize = 12.5.sp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The arrow / counter box: previous, the 1/9 position, next. It wears the same
 * border as the bar itself, which is what the three-dot box copies too.
 */
@Composable
fun ArrowCounter(
    index: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCounter: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(shape)
            .border(1.5.dp, colors.outline, shape)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Lucide.ArrowUp,
            contentDescription = stringResource(R.string.action_previous),
            tint = colors.onSurface,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .clickable(onClick = onPrevious)
                .padding(5.dp),
        )
        Text(
            text = "${(index + 1).coerceAtMost(total.coerceAtLeast(1))}/${total.coerceAtLeast(1)}",
            color = colors.onSurfaceVariant,
            fontFamily = Poppins,
            fontSize = 10.sp,
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onCounter)
                .padding(horizontal = 3.dp, vertical = 4.dp),
        )
        Icon(
            Lucide.ChevronDown,
            contentDescription = stringResource(R.string.action_next),
            tint = colors.onSurface,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .clickable(onClick = onNext)
                .padding(5.dp),
        )
    }
}
