package dev.voidapp.ui.proto

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.voidapp.ui.BrowserTab
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.components.AppMark
import dev.voidapp.ui.theme.Poppins
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

/** Both collapsed circles (search and VOID) share this exact fill. */
private const val CircleAlpha = 0.86f

/**
 * Everything under the page:
 *
 *  - the search / address field and the nav pill (back, forward, home, hamburger);
 *  - the hamburger grows the pill upward into the paged tools box (names under the
 *    icons) and raises the tabs box above the search field;
 *  - scrolling a page folds the search field and the pill into TWO separate circles;
 *    tapping the search circle grows only the search field, tapping the VOID circle
 *    grows only the pill;
 *  - swiping the pill left / right changes tab, pulling it up shrinks the current
 *    page into the tabs page.
 */
@Composable
fun ProtoControls(state: VoidAppState, modifier: Modifier = Modifier) {
    val c = protoColors()
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val motion = rememberMotion()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val maxHpx = with(density) { h.toPx() }
        val stepPx = with(density) { state.ui.swipeStep.dp.toPx() }

        val navBottom = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
        val imeBottom = with(density) { WindowInsets.ime.getBottom(density).toDp() }
        // The bar rides up with the keyboard only while ITS OWN field is being typed
        // in. Typing in the centre search of the Homepage leaves the bottom part put.
        val bottomInset = if (state.bottomFocus && imeBottom > navBottom) imeBottom else navBottom

        val pillHeight = c.barHeight
        val rowBottom = h * 0.012f + bottomInset

        // Pages leave room under their content for these controls.
        LaunchedEffect(h, navBottom, pillHeight) {
            state.pageBottomInsetDp = (h * 0.012f + pillHeight + 16.dp + navBottom).value
        }

        // ---- collapsing: two independent circles ------------------------------------
        val scrolled = state.pageScrolled && state.browserOpen
        var navForced by remember { mutableStateOf(false) }
        var searchForced by remember { mutableStateOf(false) }
        LaunchedEffect(scrolled, state.activeTabId) {
            navForced = false
            searchForced = false
            if (scrolled) state.closeMenu()
        }
        val navCollapsed = scrolled && !navForced
        val searchCollapsed = scrolled && !searchForced
        val open = state.menuOpen && !navCollapsed

        // ---- sizes, with the prototype's breakpoints ---------------------------------
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
        val closedRadius = minOf(c.barRadius, pillHeight / 2)

        val navWidthTarget = when {
            navCollapsed -> circleSize
            open -> available / 1.88f
            else -> navClosed
        }
        val navWidth by animateDpAsState(navWidthTarget, motion.soft(), label = "navWidth")
        val navHeight by animateDpAsState(
            targetValue = when {
                navCollapsed -> circleSize
                open -> openHeight
                else -> pillHeight
            },
            animationSpec = motion.soft(),
            label = "navHeight",
        )
        val navRadius by animateDpAsState(
            targetValue = when {
                navCollapsed -> circleSize / 2
                open -> minOf(23.dp, c.barRadius)
                else -> closedRadius
            },
            animationSpec = motion.soft(),
            label = "navRadius",
        )
        val openFraction by animateFloatAsState(if (open) 1f else 0f, motion.soft(), label = "open")

        val searchWidth by animateDpAsState(
            targetValue = if (searchCollapsed) circleSize else available - navWidthTarget,
            animationSpec = motion.soft(),
            label = "searchWidth",
        )
        val searchHeight by animateDpAsState(if (searchCollapsed) circleSize else pillHeight, motion.soft(), label = "searchHeight")
        val searchRadius by animateDpAsState(
            targetValue = if (searchCollapsed) circleSize / 2 else closedRadius,
            animationSpec = motion.soft(),
            label = "searchRadius",
        )
        val lift by animateDpAsState(if (searchCollapsed) 2.dp else 0.dp, motion.soft(), label = "lift")

        // ---- gestures of the pill ----------------------------------------------------------
        var swipeX by remember { mutableFloatStateOf(0f) }
        var swipeY by remember { mutableFloatStateOf(0f) }
        var searchSwipeX by remember { mutableFloatStateOf(0f) }
        fun tick(type: HapticFeedbackType) {
            if (state.ui.haptics) haptics.performHapticFeedback(type)
        }
        val pillGestures = Modifier.navGestures(
            enabled = !open && (state.ui.tabSwipe || state.ui.pullToTabs),
            allowPull = state.ui.pullToTabs,
            allowSlide = state.ui.tabSwipe,
            stepPx = stepPx,
            onPullStart = { state.beginTabDrag() },
            onPull = { total ->
                swipeY = total.coerceIn(-maxHpx, 0f)
                // The pill follows the finger and the page shrinks with it.
                state.dragTabTo(-swipeY / (maxHpx * 0.45f))
            },
            onPullEnd = { velocityY ->
                val pulled = -swipeY / (maxHpx * 0.45f)
                val openGrid = pulled > state.ui.pullThreshold || (velocityY < -1400f && swipeY < -24f)
                if (openGrid) tick(HapticFeedbackType.LongPress)
                state.endTabDrag(openGrid)
                val from = swipeY
                scope.launch {
                    animate(0f, 1f, animationSpec = motion.soft()) { t, _ ->
                        swipeY = from * (1f - t)
                    }
                    swipeY = 0f
                }
            },
            onSlide = { offset -> swipeX = (offset * 0.6f).coerceIn(-stepPx, stepPx) },
            onSlideStep = { direction ->
                if (state.switchTabBy(direction)) tick(HapticFeedbackType.TextHandleMove)
            },
            onSlideEnd = {
                val from = swipeX
                scope.launch {
                    animate(0f, 1f, animationSpec = motion.pop()) { t, _ ->
                        swipeX = from * (1f - t)
                    }
                    swipeX = 0f
                }
            },
        )

        // The search bar: swipe right = page back, swipe left = page forward (again after a back).
        val searchGestures = Modifier.navGestures(
            enabled = state.ui.historySwipe && !state.bottomFocus,
            allowPull = false,
            allowSlide = true,
            stepPx = stepPx,
            onPullStart = {},
            onPull = {},
            onPullEnd = {},
            onSlide = { offset -> searchSwipeX = (offset * 0.6f).coerceIn(-stepPx, stepPx) },
            onSlideStep = { direction ->
                if (state.historyStep(direction)) tick(HapticFeedbackType.TextHandleMove)
            },
            onSlideEnd = {
                val from = searchSwipeX
                scope.launch {
                    animate(0f, 1f, animationSpec = motion.pop()) { t, _ ->
                        searchSwipeX = from * (1f - t)
                    }
                    searchSwipeX = 0f
                }
            },
        )

        // Find in page replaces the whole bottom part while it is open.
        if (!state.findOpen) {
            // The tab you swiped to, shown for a moment above the pill.
            var lastHint by remember { mutableStateOf("") }
            LaunchedEffect(state.tabHint) { state.tabHint?.let { lastHint = it } }
            AnimatedVisibility(
                visible = state.tabHint != null,
                enter = fadeIn(tween(120)),
                exit = fadeOut(tween(300)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = w * 0.04f, bottom = rowBottom + pillHeight + 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .frosted(RoundedCornerShape(14.dp), c.tooltip.copy(alpha = 0.82f))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = lastHint,
                        color = c.tooltipText,
                        fontFamily = Poppins,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 220.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = w * 0.04f, end = w * 0.04f, bottom = rowBottom)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(
                    modifier = Modifier
                        .width(searchWidth)
                        .clipToBounds()
                        .graphicsLayer { alpha = (1f - state.tabProgress * 2f).coerceIn(0f, 1f) },
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
                    SearchPill(
                        state = state,
                        c = c,
                        collapsed = searchCollapsed,
                        radius = searchRadius,
                        onExpand = { searchForced = true },
                        modifier = Modifier
                            .padding(bottom = lift)
                            .fillMaxWidth()
                            .height(searchHeight)
                            .graphicsLayer { translationX = searchSwipeX }
                            .then(searchGestures),
                    )
                }
                Spacer(Modifier.width(gap))
                NavPill(
                    state = state,
                    c = c,
                    collapsed = navCollapsed,
                    open = open,
                    openFraction = openFraction,
                    radius = navRadius,
                    onExpand = { navForced = true },
                    modifier = Modifier
                        .padding(bottom = lift)
                        .width(navWidth)
                        .height(navHeight)
                        .graphicsLayer {
                            translationX = swipeX
                            translationY = swipeY
                        }
                        .then(pillGestures),
                )
            }
        }
    }
}

/**
 * Gestures of the nav pill (pill or circle), all read in the Initial pass and
 * consumed once decided so the buttons never fire at the end of a drag:
 *  - pull up: [onPull] gets the total travel, [onPullEnd] the release velocity;
 *  - slide sideways: every [stepPx] is one tab ([onSlideStep] -1 previous / +1 next),
 *    [onSlide] the leftover offset for the pill's own movement.
 */
private fun Modifier.navGestures(
    enabled: Boolean,
    allowPull: Boolean,
    allowSlide: Boolean,
    stepPx: Float,
    onPullStart: () -> Unit,
    onPull: (Float) -> Unit,
    onPullEnd: (Float) -> Unit,
    onSlide: (Float) -> Unit,
    onSlideStep: (Int) -> Unit,
    onSlideEnd: () -> Unit,
): Modifier = if (!enabled) {
    this
} else {
    pointerInput(stepPx, allowPull, allowSlide) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val slop = viewConfiguration.touchSlop

            // 1 = pull up, 2 = slide, 3 = ignore (a tap, or downwards)
            var mode = 0
            while (mode == 0) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                if (change.isConsumed || !change.pressed) return@awaitEachGesture
                val d = change.position - down.position
                if (d.getDistance() > slop) {
                    mode = when {
                        abs(d.x) > abs(d.y) -> if (allowSlide) 2 else 3
                        d.y < 0f -> if (allowPull) 1 else 3
                        else -> 3
                    }
                }
            }

            when (mode) {
                1 -> {
                    onPullStart()
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        tracker.addPosition(change.uptimeMillis, change.position)
                        change.consume()
                        if (!change.pressed) break
                        onPull((change.position.y - down.position.y).coerceAtMost(0f))
                    }
                    onPullEnd(tracker.calculateVelocity().y)
                }
                2 -> {
                    var steps = 0
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        if (!change.pressed) break
                        val total = change.position.x - down.position.x
                        val wanted = (total / stepPx).toInt()
                        while (steps < wanted) {
                            steps++
                            onSlideStep(-1) // finger moved right: the previous tab
                        }
                        while (steps > wanted) {
                            steps--
                            onSlideStep(1) // finger moved left: the next tab
                        }
                        onSlide(total - steps * stepPx)
                    }
                    onSlideEnd()
                }
                else -> Unit
            }
        }
    }
}

// =====================================================================================
// Search / address pill
// =====================================================================================

@Composable
private fun SearchPill(
    state: VoidAppState,
    c: ProtoColors,
    collapsed: Boolean,
    radius: Dp,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val shape = RoundedCornerShape(radius)
    val url = state.activeTab?.url.orEmpty()

    var focused by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(TextFieldValue(url)) }
    var wantFocus by remember { mutableStateOf(false) }

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

    // Tapping the search circle grows only this field, then types into it.
    LaunchedEffect(collapsed, wantFocus) {
        if (!collapsed && wantFocus) {
            wantFocus = false
            delay(120)
            runCatching { focusRequester.requestFocus() }
        }
    }

    // "Search" in the tools box focuses this field.
    var lastRequest by remember { mutableIntStateOf(state.focusRequest) }
    LaunchedEffect(state.focusRequest) {
        if (state.focusRequest != lastRequest) {
            lastRequest = state.focusRequest
            if (collapsed) {
                wantFocus = true
                onExpand()
            } else {
                runCatching { focusRequester.requestFocus() }
            }
        }
    }

    val motion = rememberMotion()
    val tint by animateColorAsState(
        targetValue = if (collapsed) c.circle.copy(alpha = CircleAlpha) else c.soft.copy(alpha = c.barAlpha),
        animationSpec = motion.soft(),
        label = "searchTint",
    )
    Box(
        modifier = modifier
            .frosted(shape, tint)
            .border(1.dp, if (collapsed) Color.Transparent else c.line, shape),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (collapsed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        wantFocus = true
                        onExpand()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(ProtoIcons.get("pillsearch"), "Search", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(ProtoIcons.get("pillsearch"), null, tint = c.navIcon, modifier = Modifier.size(22.dp))
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
                                    text = state.ui.searchHint,
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
    }
}

// =====================================================================================
// Nav pill: four icons, grows upward into the tools box, or folds into the VOID circle
// =====================================================================================

@Composable
private fun NavPill(
    state: VoidAppState,
    c: ProtoColors,
    collapsed: Boolean,
    open: Boolean,
    openFraction: Float,
    radius: Dp,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(radius)
    val motion = rememberMotion()
    val fill by animateColorAsState(
        if (collapsed) c.circle.copy(alpha = CircleAlpha) else c.soft.copy(alpha = c.barAlpha),
        motion.soft(),
        label = "navFill",
    )
    val borderColor by animateColorAsState(
        if (collapsed) Color.Transparent else c.line,
        motion.soft(),
        label = "navBorder",
    )

    Column(
        modifier = modifier
            .frosted(shape, fill)
            .border(1.dp, borderColor, shape),
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (collapsed) {
            // Folded into a circle: the VOID mark, untouched. Tap to grow the pill.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onExpand() },
                contentAlignment = Alignment.Center,
            ) {
                DownloadRingCircle(state, 46.dp, Color.White) {
                    AppMark(modifier = Modifier.size(26.dp), color = Color.White)
                }
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
        val closedRow = (c.barHeight.value - 2f)
        val rowHeight = (closedRow + (42f - closedRow) * openFraction).dp
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
            MenuButton(state = state, c = c, open = open, onClick = { state.menuOpen = !state.menuOpen })
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
private fun MenuButton(state: VoidAppState, c: ProtoColors, open: Boolean, onClick: () -> Unit) {
    val motion = rememberMotion()
    val rotation by animateFloatAsState(if (open) 90f else 0f, motion.pop(), label = "rotate")
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
        DownloadRingCircle(state, 34.dp, c.accent) {
            Icon(
                imageVector = ProtoIcons.get(if (open) "close" else "menu"),
                contentDescription = "Browser menu",
                tint = c.navIcon,
                modifier = Modifier.size(21.dp).rotate(rotation),
            )
        }
    }
}

/**
 * A circle whose border becomes the download progress ring. While files download the
 * ring fills with their combined progress (or spins when a size is unknown); when one
 * finishes the icon morphs into a tick and, after a moment, back into [content]. A
 * file icon that lands here (see the flying icon) makes the circle pulse.
 */
@Composable
private fun DownloadRingCircle(
    state: VoidAppState,
    diameter: Dp,
    ringColor: Color,
    content: @Composable () -> Unit,
) {
    val motion = rememberMotion()
    val center = state.downloads

    var showTick by remember { mutableStateOf(false) }
    var lastTick by remember { mutableIntStateOf(center.tick) }
    LaunchedEffect(center.tick) {
        if (center.tick != lastTick) {
            lastTick = center.tick
            showTick = true
            delay(1600)
            showTick = false
        }
    }

    val running = center.activeCount > 0
    val progress by animateFloatAsState(if (showTick) 1f else center.overall, motion.soft(), label = "ring")
    val ringAlpha by animateFloatAsState(if (running || showTick) 1f else 0f, motion.pop(), label = "ringAlpha")
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "angle",
    )
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(state.downloadPulse) {
        if (state.downloadPulse > 0) {
            pulse.snapTo(1.28f)
            pulse.animateTo(1f, motion.pop())
        }
    }

    Box(
        modifier = Modifier
            .size(diameter)
            .graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            }
            .onGloballyPositioned { coords ->
                val p = coords.positionInWindow()
                state.downloadTarget = Offset(p.x + coords.size.width / 2f, p.y + coords.size.height / 2f)
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (ringAlpha > 0.01f) {
                val stroke = 2.5.dp.toPx()
                val box = Size(this.size.width - stroke, this.size.height - stroke)
                val spinning = center.indeterminate && !showTick
                drawCircle(
                    color = ringColor.copy(alpha = 0.22f * ringAlpha),
                    radius = box.width / 2f,
                    style = Stroke(width = stroke),
                )
                drawArc(
                    color = ringColor.copy(alpha = ringAlpha),
                    startAngle = if (spinning) angle - 90f else -90f,
                    sweepAngle = if (spinning) 100f else 360f * progress,
                    useCenter = false,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = box,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        AnimatedContent(
            targetState = showTick,
            transitionSpec = {
                (scaleIn(motion.pop(), initialScale = 0.3f) + fadeIn(tween(160))) togetherWith
                    (scaleOut(motion.pop(), targetScale = 0.3f) + fadeOut(tween(120)))
            },
            label = "ringIcon",
        ) { done ->
            if (done) {
                Icon(Icons.Filled.Check, "Done", tint = ringColor, modifier = Modifier.size(diameter * 0.55f))
            } else {
                content()
            }
        }
    }
}

// =====================================================================================
// Tabs box (above the search field while the menu is open)
// =====================================================================================

@Composable
private fun TabsBox(state: VoidAppState, c: ProtoColors, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(17.dp)
    Column(
        modifier = modifier
            .frosted(shape, c.boxFill.copy(alpha = 0.8f))
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
                TabRowItem(state, c, tab, active = tab.id == state.activeTabId)
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
                Icon(ProtoIcons.get("plus"), "New tab", tint = c.navIcon, modifier = Modifier.size(15.dp))
            }
        }
    }
}

/** One tab in the box. Tap = switch, x = close, long press = its option box, right there. */
@Composable
private fun TabRowItem(state: VoidAppState, c: ProtoColors, tab: BrowserTab, active: Boolean) {
    val density = LocalDensity.current
    var menu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (active) c.rowActive else Color.Transparent)
                .pointerInput(tab.id) {
                    detectTapGestures(
                        onTap = { state.switchTab(tab.id) },
                        onLongPress = { menu = true },
                    )
                }
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
                Icon(ProtoIcons.get("close"), "Close tab", tint = c.tabIdle, modifier = Modifier.size(14.dp))
            }
        }

        if (menu) {
            Popup(
                popupPositionProvider = AbovePopup(with(density) { 6.dp.roundToPx() }, centered = false),
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
// Tools box: pages of 3 x 4 tools, the name under every icon
// =====================================================================================

@Composable
private fun ToolsContent(state: VoidAppState, c: ProtoColors, modifier: Modifier = Modifier) {
    val tools = state.ui.menuToolIds.mapNotNull { ProtoToolById[it] }
    val pages = tools.chunked(TOOLS_PER_PAGE)
    val pageCount = max(1, pages.size)
    val latestCount by rememberUpdatedState(pageCount)
    val pager = rememberPagerState(pageCount = { latestCount })
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
            if (pageCount > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    for (index in 0 until pageCount) {
                        val active = pager.currentPage == index
                        val dotWidth by animateDpAsState(if (active) 16.dp else 6.dp, rememberMotion().pop(), label = "dot")
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
        }
        if (tools.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "No tools. Add some in Settings > Hamburger menu.",
                    color = c.tabIdle,
                    fontFamily = Poppins,
                    fontSize = 10.sp,
                )
            }
        } else {
            HorizontalPager(
                state = pager,
                pageSpacing = 5.dp,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { page ->
                ToolGrid(state, c, pages.getOrElse(page) { emptyList() })
            }
        }
    }
}

@Composable
private fun ToolGrid(state: VoidAppState, c: ProtoColors, tools: List<ProtoTool>) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in 0 until 4) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
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

@Composable
private fun ToolCell(state: VoidAppState, c: ProtoColors, tool: ProtoTool, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val on = protoToolOn(state, tool.id)
    val tint = if (on) c.accent else c.navIcon

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (pressed) c.press else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null) { runProtoTool(state, tool.id) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(ProtoIcons.get(tool.icon, 1.7f), tool.name, tint = tint, modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(2.dp))
        // The name sits right under its icon.
        Text(
            text = tool.name,
            color = if (on) c.accent else c.label,
            fontFamily = Poppins,
            fontSize = 8.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A popup right above its anchor, starting at the anchor's edge (option boxes). */
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
