package dev.voidapp.ui.proto

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import dev.voidapp.ui.BrowserTab
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.theme.Poppins
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Everything below the page, as in the prototype:
 *
 *  - the tab row (italic tab names with an x, and a + button),
 *  - the bottom row: the home/search pill and the five-icon nav pill,
 *  - menu open: the nav pill grows upward into the paged tools box (the five icons
 *    stay in place at its bottom) and the tabs box rises above the home pill,
 *  - page scrolled: the nav pill folds into a dot, the tab row and the boxes hide.
 *
 * The literal prototype CSS pushes the grown pill off the bottom of the screen, so
 * both columns are aligned instead: the tabs box + home pill column is exactly as
 * tall as the grown pill.
 */
@Composable
fun ProtoControls(state: VoidAppState, modifier: Modifier = Modifier) {
    val c = protoColors()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight

        val navBottom = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
        val imeBottom = with(density) { WindowInsets.ime.getBottom(density).toDp() }
        val bottomInset = if (imeBottom > navBottom) imeBottom else navBottom

        // Pages leave room under their content for these controls.
        LaunchedEffect(h, navBottom) {
            state.pageBottomInsetDp = (h * 0.115f + 32.dp + 24.dp + navBottom).value
        }

        val menuOpen = state.menuOpen
        val collapsed = state.pageScrolled && state.browserOpen
        LaunchedEffect(collapsed) { if (collapsed) state.closeMenu() }
        val open = menuOpen && !collapsed

        // ---- sizes, with the prototype's breakpoints ---------------------------
        val rowWidth = w * 0.92f
        val gap = 9.dp
        val available = rowWidth - gap
        val navClosed = if (w <= 360.dp) 145.dp else 157.dp
        val openHeight = when {
            h <= 650.dp -> 215.dp
            w <= 360.dp -> 230.dp
            else -> 245.dp
        }
        val pillHeight = 51.dp

        val navWidth by animateDpAsState(
            targetValue = when {
                collapsed -> 43.dp
                open -> available / 1.88f
                else -> navClosed
            },
            animationSpec = tween(350),
            label = "navWidth",
        )
        val navHeight by animateDpAsState(
            targetValue = when {
                collapsed -> 43.dp
                open -> openHeight
                else -> pillHeight
            },
            animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)),
            label = "navHeight",
        )
        val navRadius by animateDpAsState(
            targetValue = when {
                collapsed -> 22.dp
                open -> 23.dp
                else -> 30.dp
            },
            animationSpec = tween(350),
            label = "navRadius",
        )
        val navLift by animateDpAsState(if (collapsed) 4.5.dp else 0.5.dp, tween(300), label = "navLift")
        val openFraction by animateFloatAsState(if (open) 1f else 0f, tween(300), label = "open")
        val leftWidth = available - navWidth

        // ---- a soft floor so the controls read over a page ----------------------
        if (state.browserOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(h * 0.115f + 32.dp + 36.dp + bottomInset)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, c.page.copy(alpha = 0.94f)),
                        ),
                    ),
            )
        }

        // ---- tab row --------------------------------------------------------------
        val tabRowAlpha by animateFloatAsState(
            targetValue = if (collapsed || menuOpen) 0f else 1f,
            animationSpec = tween(250),
            label = "tabRow",
        )
        if (tabRowAlpha > 0.01f) {
            TabRow(
                state = state,
                c = c,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = w * 0.05f, end = w * 0.05f, bottom = h * 0.115f + bottomInset)
                    .fillMaxWidth()
                    .height(32.dp)
                    .graphicsLayer { alpha = tabRowAlpha },
            )
        }

        // ---- bottom row -------------------------------------------------------------
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = w * 0.04f, end = w * 0.04f, bottom = h * 0.012f + bottomInset)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier = Modifier.width(leftWidth),
                verticalArrangement = Arrangement.Bottom,
            ) {
                AnimatedVisibility(
                    visible = open,
                    enter = fadeIn(tween(300)) + expandVertically(tween(400)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(300)),
                ) {
                    Column {
                        TabsBox(
                            state = state,
                            c = c,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(openHeight - pillHeight - gap),
                        )
                        Spacer(Modifier.height(gap))
                    }
                }
                HomePill(
                    state = state,
                    c = c,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(pillHeight)
                        .padding(bottom = 0.dp),
                )
            }

            NavPill(
                state = state,
                c = c,
                collapsed = collapsed,
                open = open,
                openFraction = openFraction,
                modifier = Modifier
                    .padding(bottom = navLift)
                    .width(navWidth)
                    .height(navHeight),
                radius = navRadius,
            )
        }
    }
}

// =====================================================================================
// Tab row
// =====================================================================================

@Composable
private fun TabRow(state: VoidAppState, c: ProtoColors, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LazyRow(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(state.tabs, key = { it.id }) { tab ->
                TabChip(state, c, tab, active = tab.id == state.activeTabId)
            }
        }
        Box(
            modifier = Modifier
                .size(25.dp)
                .clip(CircleShape)
                .background(c.page)
                .border(1.dp, c.addBorder, CircleShape)
                .clickable { state.startNewTab() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(ProtoIcons.get("plus"), "New tab", tint = c.addIcon, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun TabChip(state: VoidAppState, c: ProtoColors, tab: BrowserTab, active: Boolean) {
    val color = if (active) c.tabActive else c.tabIdle
    Row(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) { state.switchTab(tab.id) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = protoTabTitle(tab.url, tab.title),
            color = color,
            fontFamily = Poppins,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Light,
            fontSize = 11.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 140.dp),
        )
        Box(
            modifier = Modifier
                .size(14.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { state.closeTab(tab.id) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                ProtoIcons.get("close", 2f),
                "Close tab",
                tint = color.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

// =====================================================================================
// Home / search pill
// =====================================================================================

@Composable
private fun HomePill(state: VoidAppState, c: ProtoColors, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var text by remember { mutableStateOf("") }
    val shape = RoundedCornerShape(30.dp)
    val tab = state.activeTab
    val placeholder = if (tab == null) "Homepage" else protoTabTitle(tab.url, tab.title)

    // "Search" in the tools box focuses this field while a page is open.
    var lastRequest by remember { mutableIntStateOf(state.focusRequest) }
    LaunchedEffect(state.focusRequest) {
        if (state.focusRequest != lastRequest) {
            lastRequest = state.focusRequest
            if (state.browserOpen) runCatching { focusRequester.requestFocus() }
        }
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(c.soft)
            .border(1.dp, c.line, shape)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            ProtoIcons.get("pillsearch"),
            contentDescription = null,
            tint = c.navIcon,
            modifier = Modifier.size(21.dp),
        )
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = TextStyle(color = c.ink, fontSize = 13.sp, fontFamily = Poppins),
            cursorBrush = SolidColor(c.ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                val query = text
                text = ""
                focusManager.clearFocus()
                state.search(query)
            }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = c.placeholder,
                            fontSize = 13.sp,
                            fontFamily = Poppins,
                            fontStyle = FontStyle.Italic,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

// =====================================================================================
// Nav pill: five icons, grows upward into the tools box
// =====================================================================================

@Composable
private fun NavPill(
    state: VoidAppState,
    c: ProtoColors,
    collapsed: Boolean,
    open: Boolean,
    openFraction: Float,
    radius: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(c.soft)
            .border(1.dp, c.line, shape),
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (openFraction > 0.02f && !collapsed) {
            ToolsContent(
                state = state,
                c = c,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { alpha = openFraction },
            )
        }

        // The five icons never move: at the bottom of the pill, open or closed.
        val rowHeight = (49f + (42f - 49f) * openFraction).dp
        val buttonHeight = (40f + (37f - 40f) * openFraction).dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (collapsed) 41.dp else rowHeight)
                .padding(
                    horizontal = if (collapsed) 0.dp else 8.dp,
                    vertical = 0.dp,
                )
                .padding(bottom = if (collapsed) 0.dp else (5f * openFraction).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.SpaceAround,
        ) {
            if (!collapsed) {
                NavButton("tabs", buttonHeight, c) { state.menuOpen = !state.menuOpen }
                NavButton("home", buttonHeight, c) {
                    state.goHome()
                    state.closeMenu()
                }
                NavButton("back", buttonHeight, c) { state.browserBack() }
                NavButton("forward", buttonHeight, c) { state.browserForward() }
            }
            MenuButton(
                c = c,
                open = open,
                collapsed = collapsed,
                onClick = {
                    if (collapsed) {
                        state.expandPill()
                        state.menuOpen = true
                    } else {
                        state.menuOpen = !state.menuOpen
                    }
                },
            )
        }
    }
}

@Composable
private fun NavButton(icon: String, height: Dp, c: ProtoColors, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(width = 25.dp, height = height)
            .clip(CircleShape)
            .background(if (pressed) c.menuDot else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(ProtoIcons.get(icon), icon, tint = c.navIcon, modifier = Modifier.size(20.dp))
    }
}

/** The grey dot / dash. Open: it turns into a bigger x and rotates a quarter turn. */
@Composable
private fun MenuButton(c: ProtoColors, open: Boolean, collapsed: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(if (open) 90f else 0f, tween(300), label = "rotate")
    val iconSize by animateDpAsState(if (open) 21.dp else 15.dp, tween(250), label = "menuIcon")
    val fill by animateFloatAsState(if (open && !collapsed) 0f else 1f, tween(250), label = "fill")
    Box(
        modifier = Modifier
            .size(if (collapsed) 41.dp else 25.dp)
            .clip(CircleShape)
            .background(c.menuDot.copy(alpha = fill))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ProtoIcons.get(if (open) "close" else "menu"),
            contentDescription = "Browser menu",
            tint = c.navIcon,
            modifier = Modifier.size(iconSize).rotate(rotation),
        )
    }
}

// =====================================================================================
// Tabs box (left column when the menu is open)
// =====================================================================================

@Composable
private fun TabsBox(state: VoidAppState, c: ProtoColors, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(17.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(c.boxFill)
            .border(1.dp, c.line, shape)
            .padding(9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(25.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("TABS", color = c.tabIdle, fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 10.sp)
            Text(
                text = state.tabs.size.toString() + if (state.tabs.size == 1) " TAB" else " TABS",
                color = c.tabIdle.copy(alpha = 0.8f),
                fontFamily = Poppins,
                fontSize = 9.sp,
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp),
        ) {
            items(state.tabs, key = { it.id }) { tab ->
                val active = tab.id == state.activeTabId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) c.rowActive else Color.Transparent)
                        .clickable { state.switchTab(tab.id) }
                        .padding(horizontal = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = protoTabTitle(tab.url, tab.title),
                        color = if (active) c.tabActive else c.tabIdle,
                        fontFamily = Poppins,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { state.closeTab(tab.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("\u00D7", color = c.tabIdle, fontSize = 15.sp)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        Row(
            modifier = Modifier.fillMaxWidth().height(32.dp).padding(start = 4.dp, top = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(25.dp)
                    .clip(CircleShape)
                    .border(1.dp, c.addBorder, CircleShape)
                    .clickable { state.startNewTab() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(ProtoIcons.get("plus"), "New tab", tint = c.navIcon, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// =====================================================================================
// Tools box: title, page dots, and two swipeable 3 x 4 pages
// =====================================================================================

@Composable
private fun ToolsContent(state: VoidAppState, c: ProtoColors, modifier: Modifier = Modifier) {
    val pager = rememberPagerState(pageCount = { ProtoToolPages.size })
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.padding(start = 8.dp, end = 8.dp, top = 9.dp, bottom = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "TOOLS",
                color = c.tabIdle,
                fontFamily = Poppins,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 0.4.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                ProtoToolPages.indices.forEach { index ->
                    val active = pager.currentPage == index
                    val dotWidth by animateDpAsState(if (active) 16.dp else 6.dp, tween(250), label = "dot")
                    Box(
                        modifier = Modifier
                            .width(dotWidth)
                            .height(6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) c.dotActive else c.dotIdle)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { scope.launch { pager.animateScrollToPage(index) } },
                    )
                }
            }
        }
        HorizontalPager(
            state = pager,
            pageSpacing = 5.dp,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            ToolGrid(state, c, ProtoToolPages[page])
        }
    }
}

@Composable
private fun ToolGrid(state: VoidAppState, c: ProtoColors, tools: List<ProtoTool>) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (row in 0 until 4) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                for (col in 0 until 3) {
                    val tool = tools.getOrNull(row * 3 + col)
                    if (tool != null) {
                        ToolCell(state, c, tool, Modifier.weight(1f).fillMaxHeight())
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Tooltip above the cell, like the prototype's dark label. */
private class AboveCell(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.center.x - popupContentSize.width / 2)
            .coerceIn(0, max(0, windowSize.width - popupContentSize.width))
        val y = max(0, anchorBounds.top - popupContentSize.height - gapPx)
        return IntOffset(x, y)
    }
}

@Composable
private fun ToolCell(state: VoidAppState, c: ProtoColors, tool: ProtoTool, modifier: Modifier = Modifier) {
    var pressed by remember { mutableStateOf(false) }
    var showName by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (pressed) c.press else Color.Transparent)
            .pointerInput(tool.action) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                        showName = false
                    },
                    onLongPress = { showName = true },
                    onTap = { runProtoTool(state, tool.action) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Icon(ProtoIcons.get(tool.icon, 1.7f), tool.name, tint = c.navIcon, modifier = Modifier.size(20.dp))
            if (showName) {
                Text(
                    text = tool.name,
                    color = c.label,
                    fontFamily = Poppins,
                    fontSize = 8.sp,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        if (showName) {
            Popup(popupPositionProvider = AboveCell(with(density) { 5.dp.roundToPx() })) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(c.tooltip)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = tool.name,
                        color = c.tooltipText,
                        fontFamily = Poppins,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
