package dev.voidapp.ui.proto

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.PopupProperties
import dev.voidapp.ui.components.AppMark
import kotlinx.coroutines.delay
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
        // The bar rides up with the keyboard only while ITS OWN field is being typed
        // in. Typing in the centre search of the Homepage leaves the bottom part put.
        val bottomInset = if (state.bottomFocus && imeBottom > navBottom) imeBottom else navBottom

        val rowBottom = h * 0.012f + bottomInset
        val pillHeight = 51.dp
        val tabRowBottom = rowBottom + 52.dp + 14.dp

        // Pages leave room under their content for these controls.
        LaunchedEffect(h, navBottom) {
            state.pageBottomInsetDp = (h * 0.012f + 52.dp + 14.dp + 32.dp + 16.dp + navBottom).value
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
        val circleSize = 46.dp
        val openHeight = when {
            h <= 650.dp -> 215.dp
            w <= 360.dp -> 230.dp
            else -> 245.dp
        }

        val navWidth by animateDpAsState(
            targetValue = when {
                collapsed -> circleSize
                open -> available / 1.88f
                else -> navClosed
            },
            animationSpec = tween(350),
            label = "navWidth",
        )
        val navHeight by animateDpAsState(
            targetValue = when {
                collapsed -> circleSize
                open -> openHeight
                else -> pillHeight
            },
            animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)),
            label = "navHeight",
        )
        val navRadius by animateDpAsState(
            targetValue = when {
                collapsed -> circleSize / 2
                open -> 23.dp
                else -> 30.dp
            },
            animationSpec = tween(350),
            label = "navRadius",
        )
        val navLift by animateDpAsState(if (collapsed) 3.dp else 0.5.dp, tween(300), label = "navLift")
        val openFraction by animateFloatAsState(if (open) 1f else 0f, tween(300), label = "open")

        // The search bar shrinks away while the pill becomes the circle.
        val leftWidth by animateDpAsState(
            targetValue = if (collapsed) 0.dp else available - (if (open) available / 1.88f else navClosed),
            animationSpec = tween(350),
            label = "leftWidth",
        )
        val gapWidth by animateDpAsState(if (collapsed) 0.dp else gap, tween(350), label = "gap")

        // Find in page replaces the whole bottom part while it is open.
        if (!state.findOpen) {
            // ---- tab row: pills, a little above the bottom search and nav pills ------
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
                        .padding(start = w * 0.05f, end = w * 0.05f, bottom = tabRowBottom)
                        .fillMaxWidth()
                        .height(32.dp)
                        .graphicsLayer { alpha = tabRowAlpha },
                )
            }

            // ---- bottom row ---------------------------------------------------------
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = w * 0.04f, end = w * 0.04f, bottom = rowBottom)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom,
            ) {
                if (leftWidth > 1.dp) {
                    Column(
                        modifier = Modifier.width(leftWidth).clipToBounds(),
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
                            modifier = Modifier.fillMaxWidth().height(pillHeight),
                        )
                    }
                }
                Spacer(Modifier.width(gapWidth))
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
}

// =====================================================================================
// Tab row: every tab is a pill, e.g. "Homepage x"
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
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(state.tabs, key = { it.id }) { tab ->
                TabChip(state, c, tab, active = tab.id == state.activeTabId)
            }
        }
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(c.soft)
                .border(1.dp, c.line, CircleShape)
                .clickable { state.startNewTab() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(ProtoIcons.get("plus"), "New tab", tint = c.navIcon, modifier = Modifier.size(18.dp))
        }
    }
}

/** One tab: a pill with its name and an x. Long press opens its option box right there. */
@Composable
private fun TabChip(state: VoidAppState, c: ProtoColors, tab: BrowserTab, active: Boolean) {
    val density = LocalDensity.current
    var menu by remember { mutableStateOf(false) }
    val color = if (active) c.tabActive else c.tabIdle
    val shape = RoundedCornerShape(16.dp)

    Box {
        Row(
            modifier = Modifier
                .height(30.dp)
                .clip(shape)
                .background(if (active) c.page else c.soft)
                .border(1.dp, if (active) c.centerFocus else c.line, shape)
                .pointerInput(tab.id) {
                    detectTapGestures(
                        onTap = { state.switchTab(tab.id) },
                        onLongPress = { menu = true },
                    )
                }
                .padding(start = 12.dp, end = 6.dp),
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
                modifier = Modifier.widthIn(max = 130.dp),
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { state.closeTab(tab.id) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    ProtoIcons.get("close"),
                    "Close tab",
                    tint = color.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        if (menu) {
            Popup(
                popupPositionProvider = AbovePopup(with(density) { 8.dp.roundToPx() }, centered = false),
                onDismissRequest = { menu = false },
                properties = PopupProperties(focusable = true),
            ) {
                TabOptions(state, c, tab) { menu = false }
            }
        }
    }
}

/** The option box of a tab. */
@Composable
private fun TabOptions(state: VoidAppState, c: ProtoColors, tab: BrowserTab, dismiss: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val isPage = tab.url.isNotBlank()
    Column(
        modifier = Modifier
            .width(176.dp)
            .clip(shape)
            .background(c.boxFill)
            .border(1.dp, c.line, shape)
            .padding(5.dp),
    ) {
        if (isPage) {
            OptionRow(c, "reload", "Reload") {
                state.switchTab(tab.id)
                state.browserReload()
                dismiss()
            }
            OptionRow(c, "duplicate", "Duplicate") {
                state.duplicateTab(tab.id)
                dismiss()
            }
            OptionRow(c, "copylink", "Copy link") {
                state.copyLink(tab.url)
                dismiss()
            }
            OptionRow(c, "share", "Share") {
                state.shareLink(tab.url)
                dismiss()
            }
        } else {
            OptionRow(c, "plus", "New tab") {
                state.startNewTab()
                dismiss()
            }
        }
        OptionRow(c, "closeothers", "Close others") {
            state.closeOtherTabs(tab.id)
            dismiss()
        }
        OptionRow(c, "closetab", "Close tab") {
            state.closeTab(tab.id)
            dismiss()
        }
    }
}

@Composable
private fun OptionRow(c: ProtoColors, icon: String, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(ProtoIcons.get(icon), null, tint = c.navIcon, modifier = Modifier.size(18.dp))
        Text(label, color = c.ink, fontFamily = Poppins, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// =====================================================================================
// Home / search pill
// =====================================================================================

@Composable
private fun HomePill(state: VoidAppState, c: ProtoColors, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val shape = RoundedCornerShape(30.dp)
    val url = state.activeTab?.url.orEmpty()

    var focused by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(TextFieldValue(url)) }

    // The field shows the address of the current tab until it is being edited.
    LaunchedEffect(url, state.activeTabId, focused) {
        if (!focused) value = TextFieldValue(url)
    }
    // Focus selects the whole address, so one paste or keystroke replaces it.
    LaunchedEffect(focused) {
        state.bottomFocus = focused
        if (focused) {
            delay(60)
            value = value.copy(selection = TextRange(0, value.text.length))
        }
    }
    DisposableEffect(Unit) { onDispose { state.bottomFocus = false } }

    // "Search" in the tools box focuses this field.
    var lastRequest by remember { mutableIntStateOf(state.focusRequest) }
    LaunchedEffect(state.focusRequest) {
        if (state.focusRequest != lastRequest) {
            lastRequest = state.focusRequest
            runCatching { focusRequester.requestFocus() }
        }
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(c.soft)
            .border(1.dp, c.line, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            ProtoIcons.get("pillsearch"),
            contentDescription = null,
            tint = c.navIcon,
            modifier = Modifier.size(22.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            textStyle = TextStyle(color = c.ink, fontSize = 13.sp, fontFamily = Poppins),
            cursorBrush = SolidColor(c.ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                // A URL opens as it is, anything else is searched.
                val query = value.text
                focusManager.clearFocus()
                state.search(query)
            }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.text.isEmpty()) {
                        Text(
                            text = "Search or enter address",
                            color = c.placeholder,
                            fontSize = 13.sp,
                            fontFamily = Poppins,
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
    val fill by animateColorAsState(if (collapsed) c.circle else c.soft, tween(300), label = "navFill")
    val borderColor by animateColorAsState(if (collapsed) c.circle else c.line, tween(300), label = "navBorder")

    Column(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, borderColor, shape),
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (collapsed) {
            // Search bar and pill folded into one circle: the VOID mark, untouched.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { state.expandPill() },
                contentAlignment = Alignment.Center,
            ) {
                AppMark(modifier = Modifier.size(26.dp), color = Color.White)
            }
            return@Column
        }

        if (openFraction > 0.02f) {
            ToolsContent(
                state = state,
                c = c,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { alpha = openFraction },
            )
        }

        // The four icons never move: at the bottom of the pill, open or closed.
        val rowHeight = (49f + (42f - 49f) * openFraction).dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .padding(horizontal = 8.dp)
                .padding(bottom = (5f * openFraction).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            NavButton("back", c) { state.browserBack() }
            NavButton("forward", c) { state.browserForward() }
            NavButton("home", c) {
                state.goHome()
                state.closeMenu()
            }
            MenuButton(c = c, open = open, onClick = { state.menuOpen = !state.menuOpen })
        }
    }
}

@Composable
private fun NavButton(icon: String, c: ProtoColors, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (pressed) c.menuDot else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(ProtoIcons.get(icon), icon, tint = c.navIcon, modifier = Modifier.size(23.dp))
    }
}

/** The hamburger circle. Open: it turns into an x and rotates a quarter turn. */
@Composable
private fun MenuButton(c: ProtoColors, open: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(if (open) 90f else 0f, tween(300), label = "rotate")
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(c.menuDot)
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
            modifier = Modifier.size(21.dp).rotate(rotation),
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

/** A popup right above its anchor: centred (tooltips) or starting at the anchor's edge (option boxes). */
private class AbovePopup(private val gapPx: Int, private val centered: Boolean = true) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val wanted = if (centered) anchorBounds.center.x - popupContentSize.width / 2 else anchorBounds.left
        val x = wanted.coerceIn(0, max(0, windowSize.width - popupContentSize.width))
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
            Popup(popupPositionProvider = AbovePopup(with(density) { 5.dp.roundToPx() })) {
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
