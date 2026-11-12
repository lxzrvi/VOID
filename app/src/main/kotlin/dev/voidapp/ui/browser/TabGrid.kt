package dev.voidapp.ui.browser

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voidapp.R
import dev.voidapp.ui.BrowserTab
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.components.Favicon
import dev.voidapp.ui.components.ReorderGrid
import dev.voidapp.ui.proto.rememberMotion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import dev.voidapp.ui.theme.Poppins
import dev.voidapp.ui.theme.VoidRed

private sealed interface GridEntry {
    data class Tab(val tab: BrowserTab) : GridEntry
    data object New : GridEntry
}

private fun entryKey(entry: GridEntry): Any = when (entry) {
    GridEntry.New -> "new"
    is GridEntry.Tab -> entry.tab.id
}

private fun faviconOf(url: String): String =
    "https://www.google.com/s2/favicons?sz=128&domain=" +
        url.substringAfter("://", url).substringBefore("/").removePrefix("www.")

/**
 * The tab grid. It sits UNDER the live page: while the circle is pulled up (or the
 * tab tool is used) the page itself shrinks into its own card, so the card of the
 * current page is always the real page, and every other tab's card is its last
 * snapshot. Long-press a card to rearrange; a Homepage tab is a card like any other.
 */
@Composable
fun TabGrid(
    state: VoidAppState,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val scroll = rememberScrollState()

    val entries: List<GridEntry> = buildList {
        state.tabs.forEach { add(GridEntry.Tab(it)) }
        add(GridEntry.New)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = (state.tabProgress * 2f).coerceIn(0f, 1f) }
            .background(colors.background),
    ) {
        val pad = 16.dp
        val gap = 12.dp
        val columns = 3
        val cardWidth = (maxWidth - pad * 2 - gap * (columns - 1)) / columns
        val headerHeight = 30.dp
        val previewHeight = cardWidth * 1.75f
        val cellHeight = headerHeight + 4.dp + previewHeight

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = pad + 2.dp)
                    .padding(top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.tabs),
                    color = colors.onBackground,
                    fontFamily = Poppins,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = state.tabs.size.toString(),
                    color = colors.onSurfaceVariant,
                    fontFamily = Poppins,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.5.dp, colors.outline, CircleShape)
                        .clickable(enabled = state.tabGridOpen) { state.closeTabGrid() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.browser_close),
                        tint = colors.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .padding(horizontal = pad),
            ) {
                ReorderGrid(
                    items = entries,
                    itemKey = ::entryKey,
                    columns = columns,
                    cellHeight = cellHeight,
                    gap = gap,
                    fixedTail = 1,
                    stretchLastRow = false,
                    onMove = { from, to -> state.moveTab(from, to) },
                ) { entry, _, lifted ->
                    when (entry) {
                        GridEntry.New -> NewCard(
                            state = state,
                            headerHeight = headerHeight,
                        )
                        is GridEntry.Tab -> TabCard(
                            state = state,
                            tab = entry.tab,
                            lifted = lifted,
                            headerHeight = headerHeight,
                            onClose = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                state.closeTab(entry.tab.id)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(96.dp).navigationBarsPadding())
            }
        }
    }
}

private val PreviewShape = RoundedCornerShape(14.dp)

/** The preview area; its window rect is where the live page lands. */
@Composable
private fun PreviewBox(
    state: VoidAppState,
    cardKey: Long,
    active: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(PreviewShape)
            .background(colors.surfaceVariant)
            .border(
                width = if (active) 2.dp else 1.dp,
                color = if (active) VoidRed else colors.outline,
                shape = PreviewShape,
            )
            .onGloballyPositioned { coords ->
                val position = coords.positionInWindow()
                state.cardRects[cardKey] = Rect(
                    Offset(position.x, position.y),
                    Size(coords.size.width.toFloat(), coords.size.height.toFloat()),
                )
            },
    ) {
        // The live page glides in above the active card and then hands over to it, so the
        // current tab never stays behind as a floating layer.
        content()
    }
}

@Composable
private fun CardHeader(
    title: String,
    active: Boolean,
    height: androidx.compose.ui.unit.Dp,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(5.dp))
        Text(
            text = title,
            color = if (active) VoidRed else MaterialTheme.colorScheme.onSurface,
            fontFamily = Poppins,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun TabCard(
    state: VoidAppState,
    tab: BrowserTab,
    lifted: Boolean,
    headerHeight: androidx.compose.ui.unit.Dp,
    onClose: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val active = state.activeTabId == tab.id
    val motion = rememberMotion()
    val appear by animateFloatAsState(if (state.tabGridOpen) 1f else 0.88f, motion.pop(), label = "cardIn")
    val blank = tab.url.isBlank()
    val title = if (blank) "Homepage" else tab.title.ifBlank { tab.url.substringAfter("://", tab.url).substringBefore("/") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val scale = if (active) 1f else appear
                scaleX = scale
                scaleY = scale
            }
            .clickable(enabled = state.tabGridOpen && !lifted) { state.selectTab(tab.id) },
    ) {
        CardHeader(
            title = title,
            active = active,
            height = headerHeight,
            leading = {
                if (blank) {
                    Icon(Icons.Filled.Home, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
                } else {
                    Favicon(
                        faviconUrl = faviconOf(tab.url),
                        contentDescription = title,
                        size = 16.dp,
                        circleColor = Color.Transparent,
                        fallbackSize = 9.sp,
                    )
                }
            },
            trailing = {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceVariant)
                        .clickable(enabled = state.tabGridOpen, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.tab_close),
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(12.dp),
                    )
                }
            },
        )
        Spacer(Modifier.height(4.dp))
        PreviewBox(state = state, cardKey = tab.id, active = active, modifier = Modifier.weight(1f)) {
            val thumb = state.tabThumbs[tab.id]
            if (blank) {
                Box(Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.TopCenter) {
                    Text(
                        text = "VOID",
                        color = colors.onBackground,
                        fontFamily = Poppins,
                        fontSize = 13.sp,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.padding(top = 38.dp),
                    )
                }
            } else if (thumb != null) {
                Image(
                    bitmap = thumb,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Favicon(
                        faviconUrl = faviconOf(tab.url),
                        contentDescription = title,
                        size = 38.dp,
                        circleColor = colors.surface,
                    )
                }
            }
        }
    }
}

@Composable
private fun NewCard(state: VoidAppState, headerHeight: androidx.compose.ui.unit.Dp) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = state.tabGridOpen) {
                state.startNewTab()
                state.closeTabGrid()
            },
    ) {
        Spacer(Modifier.height(headerHeight + 4.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(PreviewShape)
                .border(1.5.dp, colors.outline, PreviewShape),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.tab_new),
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.tab_new),
                    color = colors.onSurfaceVariant,
                    fontFamily = Poppins,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
