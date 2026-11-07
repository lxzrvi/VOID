package dev.voidapp.ui.island

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voidapp.R
import dev.voidapp.data.SearchProvider
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.icons.Lucide
import dev.voidapp.ui.theme.VoidRed

private val SheetShape = RoundedCornerShape(22.dp)
private val TileShape = RoundedCornerShape(14.dp)

// ===========================================================================
// Bar pieces
// ===========================================================================

/** Circular action button used for the search glyph inside the bar. */
@Composable
internal fun CircleAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.outline, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = colors.onSurface,
            modifier = Modifier.size(17.dp),
        )
    }
}

/**
 * `← 1/9 →` - the counter between the arrows is the piece that opens the
 * navigator sheet when tapped.
 */
@Composable
internal fun ArrowCounter(
    index: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCounter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.height(34.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BareIconButton(icon = Lucide.ArrowLeft, label = stringResource(R.string.action_previous), onClick = onPrevious)
        Box(
            modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(colors.surfaceVariant)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClickLabel = stringResource(R.string.action_next),
                    onClick = onCounter,
                )
                .padding(horizontal = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = "${if (total == 0) 0 else index + 1}/$total",
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(90)) },
                label = "counter",
            ) { value ->
                Text(
                    text = value,
                    color = colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
        BareIconButton(icon = Lucide.ArrowRight, label = stringResource(R.string.action_next), onClick = onNext)
    }
}

@Composable
internal fun BareIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
internal fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    provider: SearchProvider,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val style = TextStyle(
        color = colors.onSurface,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Start,
    )

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(VoidRed),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { inner ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_hint),
                        style = style,
                        color = colors.onSurfaceVariant,
                    )
                }
                inner()
            }
        },
    )
}

// ===========================================================================
// Tools panel: 3 rows x 5 columns, horizontally scrollable for more
// ===========================================================================

internal data class ToolAction(
    val key: String,
    val icon: ImageVector,
    val labelRes: Int,
    val core: Boolean,
    val onClick: () -> Unit,
)

@Composable
internal fun ToolsPanel(
    state: VoidAppState,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    val coreTools = listOf(
        ToolAction("back", Lucide.ChevronLeft, R.string.browser_back, true) { state.browserBack() },
        ToolAction("forward", Lucide.ChevronRight, R.string.browser_forward, true) { state.browserForward() },
        ToolAction("reload", Lucide.RotateCw, R.string.browser_reload, true) { state.browserReload() },
        ToolAction("home", Lucide.House, R.string.browser_home, true) { state.goHome() },
        ToolAction("tabs", Lucide.Files, R.string.browser_reload, true) { state.browserReload() },
        ToolAction("history", Lucide.History, R.string.browser_history, false) { state.showHistoryInIsland() },
        ToolAction("bookmark", Lucide.Bookmark, R.string.browser_bookmark_add, false) { state.bookmarkCurrent() },
        ToolAction("share", Lucide.Share2, R.string.browser_share, false) { state.shareCurrent() },
        ToolAction("copy", Lucide.Copy, R.string.browser_copy, false) { state.copyCurrent() },
        ToolAction("external", Lucide.ExternalLink, R.string.browser_open_external, false) { state.openCurrentExternally() },
        ToolAction("desktop", Lucide.Monitor, R.string.browser_desktop, false) { state.toggleDesktopMode() },
        ToolAction("find", Lucide.Type, R.string.browser_find, false) { state.findInPage() },
        ToolAction("zoom_in", Lucide.Plus, R.string.browser_zoom_in, false) { state.zoomBy(0.15f) },
        ToolAction("zoom_out", Lucide.Maximize2, R.string.browser_zoom_out, false) { state.zoomBy(-0.15f) },
        ToolAction("settings", Lucide.Settings, R.string.browser_settings, false) { state.openSettings() },
        ToolAction("clear", Lucide.Trash2, R.string.browser_clear_data, false) { state.clearBrowsingData() },
        ToolAction("downloads", Lucide.Download, R.string.browser_save, false) { state.saveCurrent() },
        ToolAction("globe", Lucide.Globe, R.string.browser_open_external, false) { state.openCurrentExternally() },
        ToolAction("clock", Lucide.Clock, R.string.browser_reload, false) { state.browserReload() },
        ToolAction("star", Lucide.Star, R.string.browser_bookmarks, false) { state.showBookmarksInIsland() },
        ToolAction("zoom_reset", Lucide.ArrowUpRight, R.string.browser_zoom_in, false) { state.zoomBy(0f) },
        ToolAction("exit", Lucide.X, R.string.browser_close, false) { state.goHome() },
    )

    // 3 rows x 5 columns per page: chunk into pages so a page never breaks mid-row.
    val columns = 5
    val rows = 3
    val pageSize = columns * rows
    val pages = coreTools.chunked(pageSize)

    Column(
        modifier = modifier
            .clip(SheetShape)
            .background(colors.surfaceContainerHigh.copy(alpha = 0.97f))
            .border(1.dp, colors.outline, SheetShape)
            .shadow(10.dp, SheetShape)
            .padding(vertical = 12.dp),
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(pages) { page ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    page.chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { tool -> ToolButton(tool) }
                            // keep the last row aligned to 5 slots
                            repeat(columns - row.size) { Spacer(Modifier.width(52.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolButton(tool: ToolAction) {
    val colors = MaterialTheme.colorScheme
    val label = stringResource(tool.labelRes)
    Column(
        modifier = Modifier
            .width(52.dp)
            .clip(TileShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                role = Role.Button,
                onClickLabel = label,
                onClick = tool.onClick,
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = tool.icon,
            contentDescription = null,
            tint = colors.onSurface,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = colors.onSurfaceVariant,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

// ===========================================================================
// Navigator: grows up to ~half the screen with 1..9
// ===========================================================================

@Composable
internal fun SettingsSheet(
    state: VoidAppState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(SheetShape)
            .background(colors.surfaceContainerHigh.copy(alpha = 0.98f))
            .border(1.dp, colors.outline, SheetShape)
            .shadow(12.dp, SheetShape)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.browser_settings),
                color = colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            BareIconButton(icon = Lucide.X, label = stringResource(R.string.action_clear), onClick = onDismiss)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Search with ${state.provider.label}",
            color = colors.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SearchProvider.entries.take(3).forEach { provider ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(TileShape)
                        .background(if (provider == state.provider) VoidRed.copy(alpha = 0.14f) else colors.surface)
                        .border(1.dp, if (provider == state.provider) VoidRed else colors.outline, TileShape)
                        .clickable { state.selectProvider(provider) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = provider.label,
                        color = if (provider == state.provider) VoidRed else colors.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SearchProvider.entries.drop(3).forEach { provider ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(TileShape)
                        .background(if (provider == state.provider) VoidRed.copy(alpha = 0.14f) else colors.surface)
                        .border(1.dp, if (provider == state.provider) VoidRed else colors.outline, TileShape)
                        .clickable { state.selectProvider(provider) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = provider.label,
                        color = if (provider == state.provider) VoidRed else colors.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(TileShape)
                .background(colors.surface)
                .border(1.dp, colors.outline, TileShape)
                .clickable {
                    state.clearBrowsingData()
                    onDismiss()
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.browser_clear_data),
                color = VoidRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
internal fun NavigatorSheet(
    results: List<String>,
    selected: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val cells = (results.size.coerceAtLeast(1)).coerceAtMost(9)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(SheetShape)
            .background(colors.surfaceContainerHigh.copy(alpha = 0.98f))
            .border(1.dp, colors.outline, SheetShape)
            .shadow(12.dp, SheetShape)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.navigator_title),
                color = colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            BareIconButton(icon = Lucide.X, label = stringResource(R.string.action_clear), onClick = onDismiss)
        }
        Spacer(Modifier.height(8.dp))
        // 1..9 in a 3x3 grid; each cell previews the matching result.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (0 until cells).chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { i ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(TileShape)
                                .background(if (i == selected) VoidRed.copy(alpha = 0.14f) else colors.surface)
                                .border(
                                    1.dp,
                                    if (i == selected) VoidRed else colors.outline,
                                    TileShape,
                                )
                                .clickable { onPick(i) }
                                .padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = (i + 1).toString(),
                                color = if (i == selected) VoidRed else colors.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = results.getOrNull(i) ?: "—",
                                color = colors.onSurfaceVariant,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Vertical guide shown while the island is being dragged. */
@Composable
internal fun DragHint(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(0.dp)
            .alpha(0f),
    )
}
