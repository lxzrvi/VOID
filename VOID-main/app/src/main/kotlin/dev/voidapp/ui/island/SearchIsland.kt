package dev.voidapp.ui.island

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import dev.voidapp.ui.home.ProviderDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.compose.ui.unit.sp
import dev.voidapp.R
import dev.voidapp.ui.IslandPanel
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.components.AppMark
import dev.voidapp.ui.components.Favicon
import dev.voidapp.ui.components.SearchButton
import dev.voidapp.ui.icons.Lucide
import dev.voidapp.ui.theme.Poppins
import dev.voidapp.ui.theme.VoidRed
import kotlinx.coroutines.delay
import kotlin.math.abs

private val IslandHeight = 56.dp
private val IslandShape = RoundedCornerShape(IslandHeight / 2)

/**
 * The floating island.
 *
 * - Resting: the app mark in a red circle at the user's height on one side. It
 *   slides off that edge on scroll and comes back when scrolling stops, and it
 *   fades away on inactivity. Swipe up on it to reveal the tab grid; drag it
 *   vertically after a long press to move it (the bar then grows the other way).
 * - Tap: grows into the bar, which sits above the keyboard (`imePadding`).
 * - Bar: search field showing the current page address, provider favicon + arrow,
 *   a bare search glyph (no circle behind it), the arrows with the 1/9 counter
 *   once there is text, and the three dots which morph into an X.
 * - Long press on the bar: copy / share / make shortcut / new tab / close tab /
 *   expand. Scrolling any page collapses the bar back into the circle.
 */
@Composable
fun SearchIsland(
    state: VoidAppState,
    scrollDelta: Float = 0f,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }   // free move after a long press
    var swiping by remember { mutableStateOf(false) }    // pulled up towards the tab grid
    var moveX by remember { mutableFloatStateOf(0f) }
    var moveY by remember { mutableFloatStateOf(0f) }
    var swipeY by remember { mutableFloatStateOf(0f) }
    var barMenu by remember { mutableStateOf(false) }
    var showProviders by remember { mutableStateOf(false) }

    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val panel = state.islandPanel
    val frozen = expanded || panel != IslandPanel.None || dragging || swiping ||
        state.backgroundPickerAt != null

    // ---- slide away on scroll, fade on inactivity -------------------------
    val visibility = remember { Animatable(1f) }
    LaunchedEffect(scrollDelta) {
        if (!frozen && abs(scrollDelta) > 0.5f) {
            visibility.animateTo(0f, tween(220))
        }
    }
    LaunchedEffect(scrollDelta, frozen) {
        if (!frozen) {
            delay(1200)
            visibility.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    // Inactivity: hidden after 6 s without interaction.
    var idleTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(idleTick, frozen) {
        if (!frozen) {
            delay(6000)
            visibility.animateTo(0f, tween(320))
        }
    }
    val bump = { idleTick++ }

    // Scrolling a page collapses the bar back into the circle.
    LaunchedEffect(scrollDelta) {
        if (expanded && abs(scrollDelta) > 4f) expanded = false
    }

    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "progress",
    )

    BackHandler(enabled = expanded || barMenu) {
        if (barMenu) barMenu = false else expanded = false
    }

    LaunchedEffect(expanded) {
        if (!expanded) {
            state.hidePanel()
            focusManager.clearFocus()
        }
    }

    val onRight = state.islandOnRight
    val restingFrac = state.islandOffset.coerceIn(0f, 1f)
    val hideDistancePx = with(density) { 260.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .imePadding()          // the bar rises with the keyboard
            .fillMaxSize(),
    ) {
        val maxWpx = constraints.maxWidth.toFloat()
        val maxHpx = constraints.maxHeight.toFloat()
        val islandPx = with(density) { IslandHeight.toPx() }
        // The circle's top edge travels over (height - circle); that is the range
        // the resting fraction maps onto, so a finger and the circle move 1:1.
        val freeH = (maxHpx - islandPx).coerceAtLeast(1f)
        val islandWidth = lerpDp(IslandHeight, maxWidth, progress)

        val finishFreeDrag: () -> Unit = {
            if (dragging) {
                val restLeft = if (onRight) maxWpx - islandPx else 0f
                val restTop = restingFrac * freeH
                val leftNow = restLeft + moveX
                val topNow = restTop + moveY
                // Snap to whichever edge the circle is closer to.
                val newOnRight = leftNow + islandPx / 2f > maxWpx / 2f
                val newFrac = (topNow / freeH).coerceIn(0.02f, 0.98f)
                val newLeft = if (newOnRight) maxWpx - islandPx else 0f
                val newTop = newFrac * freeH
                state.placeIsland(newFrac, newOnRight)
                val startX = leftNow - newLeft
                val startY = topNow - newTop
                moveX = startX
                moveY = startY
                dragging = false
                scope.launch {
                    animate(0f, 1f, animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)) { t, _ ->
                        moveX = startX * (1f - t)
                        moveY = startY * (1f - t)
                    }
                    moveX = 0f
                    moveY = 0f
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    // The bar's bottom edge stays where the circle was; panels grow upward.
                    .layout { measurable, c ->
                        val placeable = measurable.measure(c.copy(minWidth = 0, minHeight = 0))
                        val barTop = (restingFrac * freeH).roundToInt()
                        val y = (barTop + islandPx.roundToInt() - placeable.height).coerceAtLeast(0)
                        layout(c.maxWidth, c.maxHeight) { placeable.place(0, y) }
                    }
                    .graphicsLayer {
                        val hide = (if (onRight) 1f else -1f) * (1f - visibility.value) * hideDistancePx
                        translationX = hide + moveX
                        translationY = moveY + swipeY
                        alpha = if (expanded) 1f else visibility.value
                    },
            ) {
                IslandBody(
                    state = state,
                    expanded = expanded,
                    progress = progress,
                    islandWidth = islandWidth,
                    onRight = onRight,
                    query = query,
                    onQueryChange = { text ->
                        query = text
                        index = 0
                        state.updateSuggestions(text)
                        bump()
                    },
                    index = index,
                    resultCount = state.resultCount(query),
                    panel = panel,
                    onOpenPanel = { state.showPanel(it) },
                    onClosePanel = { state.hidePanel() },
                    sheetItems = state.islandList.ifEmpty { state.results(query) },
                    onSelectedIndex = { i ->
                        if (state.islandList.isNotEmpty()) {
                            val pick = state.islandList.getOrNull(i).orEmpty()
                            val entry = state.history.firstOrNull { it.label == pick }
                                ?: state.bookmarks.firstOrNull { it.label == pick }
                            if (entry != null) state.open(entry.url) else state.search(pick)
                        } else {
                            state.openResult(query, i)
                        }
                        state.hidePanel()
                        query = ""
                        expanded = false
                    },
                    onTapCircle = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        bump()
                        expanded = true
                    },
                    onSearch = {
                        bump()
                        if (query.isBlank()) state.open(state.provider.urlFor("")) else state.search(query)
                        query = ""
                        focusManager.clearFocus()
                        expanded = false
                    },
                    onProviderClick = { showProviders = true },
                    onPrevious = {
                        val total = state.resultCount(query)
                        if (total > 0) index = (index - 1 + total) % total
                    },
                    onNext = {
                        val total = state.resultCount(query)
                        if (total > 0) index = (index + 1) % total
                    },
                    onLongPressBar = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        barMenu = true
                    },
                    onLongPressStart = {
                        if (!expanded) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            moveX = 0f
                            moveY = 0f
                            dragging = true
                        }
                    },
                    onFreeDrag = { dx, dy ->
                        if (dragging) {
                            val restLeft = if (onRight) maxWpx - islandPx else 0f
                            val restTop = restingFrac * freeH
                            moveX = (moveX + dx).coerceIn(-restLeft, maxWpx - islandPx - restLeft)
                            moveY = (moveY + dy).coerceIn(-restTop, freeH - restTop)
                        }
                    },
                    onFreeEnd = { finishFreeDrag() },
                    onSwipeStart = {
                        if (!expanded) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            swiping = true
                            swipeY = 0f
                            state.beginTabDrag()
                        }
                    },
                    onSwipeDrag = { total ->
                        if (swiping) {
                            swipeY = total.coerceIn(-maxHpx, 0f)
                            // The circle follows the finger; the page shrinks with it.
                            state.dragTabTo(-swipeY / (maxHpx * 0.45f))
                        }
                    },
                    onSwipeEnd = { velocityY ->
                        if (swiping) {
                            val pulled = -swipeY / (maxHpx * 0.45f)
                            val open = pulled > 0.3f || (velocityY < -1400f && swipeY < -24f)
                            if (open) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            state.endTabDrag(open)
                            val from = swipeY
                            scope.launch {
                                animate(0f, 1f, animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)) { t, _ ->
                                    swipeY = from * (1f - t)
                                }
                                swipeY = 0f
                                swiping = false
                            }
                        }
                    },
                )
            }
        }
    }

    if (showProviders) {
        ProviderDialog(
            current = state.provider,
            onDismiss = { showProviders = false },
            onPick = {
                state.selectProvider(it)
                showProviders = false
            },
        )
    }

    if (barMenu) {
        BarMenuDialog(
            state = state,
            query = query,
            onDismiss = { barMenu = false },
            onExpand = {
                barMenu = false
                expanded = true
            },
        )
    }
}

/**
 * Gestures of the resting circle.
 *
 * - tap: left alone, the click handler below takes it;
 * - long press, then drag: free movement in x and y ([onFreeDrag]);
 * - drag upwards: pulls up the tab grid ([onSwipeDrag] gets the total travel,
 *   [onSwipeEnd] the release velocity).
 *
 * Everything runs in the Initial pass and consumes the moves, so the circle's
 * own tap handler never fires at the end of a drag.
 */
private fun Modifier.circleGestures(
    onLongPressStart: () -> Unit,
    onFreeDrag: (Float, Float) -> Unit,
    onFreeEnd: () -> Unit,
    onSwipeStart: () -> Unit,
    onSwipeDrag: (Float) -> Unit,
    onSwipeEnd: (Float) -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val slop = viewConfiguration.touchSlop

        // 3 = ignore (tap / sideways), 2 = pull up, null = held still -> long press
        val decision: Int? = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var out = 0
            while (out == 0) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                out = when {
                    change == null || change.isConsumed || !change.pressed -> 3
                    else -> {
                        val d = change.position - down.position
                        if (d.getDistance() > slop) {
                            if (d.y < 0f && abs(d.y) > abs(d.x)) 2 else 3
                        } else {
                            0
                        }
                    }
                }
            }
            out
        }

        when (decision) {
            null -> {
                onLongPressStart()
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val delta = change.positionChange()
                    change.consume()
                    if (!change.pressed) break
                    onFreeDrag(delta.x, delta.y)
                }
                onFreeEnd()
            }
            2 -> {
                onSwipeStart()
                val tracker = VelocityTracker()
                tracker.addPosition(down.uptimeMillis, down.position)
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    tracker.addPosition(change.uptimeMillis, change.position)
                    change.consume()
                    if (!change.pressed) break
                    onSwipeDrag((change.position.y - down.position.y).coerceAtMost(0f))
                }
                onSwipeEnd(tracker.calculateVelocity().y)
            }
            else -> Unit
        }
    }
}

@Composable
private fun IslandBody(
    state: VoidAppState,
    expanded: Boolean,
    progress: Float,
    islandWidth: androidx.compose.ui.unit.Dp,
    onRight: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    index: Int,
    resultCount: Int,
    panel: IslandPanel,
    onOpenPanel: (IslandPanel) -> Unit,
    onClosePanel: () -> Unit,
    sheetItems: List<String>,
    onSelectedIndex: (Int) -> Unit,
    onTapCircle: () -> Unit,
    onSearch: () -> Unit,
    onProviderClick: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLongPressBar: () -> Unit,
    onLongPressStart: () -> Unit,
    onFreeDrag: (Float, Float) -> Unit,
    onFreeEnd: () -> Unit,
    onSwipeStart: () -> Unit,
    onSwipeDrag: (Float) -> Unit,
    onSwipeEnd: (Float) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val alignment = if (onRight) Alignment.End else Alignment.Start

    Column(horizontalAlignment = alignment, modifier = Modifier.fillMaxWidth()) {
        // ---- panels above the bar (smooth slide, no jumpy expand) ---------
        AnimatedVisibility(
            visible = expanded && panel != IslandPanel.None,
            enter = fadeIn(tween(180)) + expandVertically(tween(300), expandFrom = Alignment.Bottom),
            exit = fadeOut(tween(140)) + shrinkVertically(tween(220), shrinkTowards = Alignment.Bottom),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                contentAlignment = if (onRight) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                when (panel) {
                    IslandPanel.Tools -> ToolsPanel(state = state, modifier = Modifier.fillMaxWidth())
                    IslandPanel.Navigator -> NavigatorSheet(
                        results = sheetItems,
                        selected = index,
                        onPick = onSelectedIndex,
                        onDismiss = onClosePanel,
                    )
                    IslandPanel.Settings -> SettingsSheet(state = state, onDismiss = onClosePanel)
                    IslandPanel.Notes -> NotesPanel(state = state, onDismiss = onClosePanel)
                    IslandPanel.TabGrid -> Unit
                    IslandPanel.None -> Unit
                }
            }
        }

        // ---- the bar -----------------------------------------------------
        Row(
            modifier = Modifier
                .width(islandWidth)
                .height(IslandHeight)
                .shadow(lerpDp(0.dp, 10.dp, progress), IslandShape, clip = false)
                .clip(IslandShape)
                // "Phantom" container: near-black in dark, white in light.
                .background(lerpColor(VoidRed, colors.surface, progress))
                .border(
                    width = lerpDp(0.dp, 1.5.dp, progress),
                    color = lerpColor(Color.Transparent, VoidRed, progress),
                    shape = IslandShape,
                )
                .then(
                    if (!expanded) {
                        Modifier.circleGestures(
                            onLongPressStart = onLongPressStart,
                            onFreeDrag = onFreeDrag,
                            onFreeEnd = onFreeEnd,
                            onSwipeStart = onSwipeStart,
                            onSwipeDrag = onSwipeDrag,
                            onSwipeEnd = onSwipeEnd,
                        )
                    } else {
                        Modifier.pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onLongPressBar() },
                                onDrag = { _, _ -> },
                            )
                        }
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (onRight) Arrangement.End else Arrangement.Start,
        ) {
            if (!expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White),
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.action_open_search),
                            onClick = onTapCircle,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AppMark(modifier = Modifier.size(22.dp))
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(((progress - 0.3f) / 0.7f).coerceIn(0f, 1f))
                        .padding(
                            start = if (onRight) 16.dp else 6.dp,
                            end = if (onRight) 6.dp else 16.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (onRight) Arrangement.End else Arrangement.Start,
                ) {
                    if (onRight) {
                        BarSearchField(
                            query = query,
                            onQueryChange = onQueryChange,
                            state = state,
                            onProviderClick = onProviderClick,
                            onSubmit = onSearch,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                        Spacer(Modifier.width(6.dp))
                    }

                    // Bare search glyph with the void mark inside - no circle.
                    SearchButton(
                        onClick = onSearch,
                        contentDescription = stringResource(R.string.search_hint),
                        size = 24.dp,
                        tint = colors.onSurface,
                    )

                    if (query.isNotBlank()) {
                        Spacer(Modifier.width(4.dp))
                        ArrowCounter(
                            index = index,
                            total = resultCount,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onCounter = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenPanel(IslandPanel.Navigator)
                            },
                        )
                    }

                    // Three dots <-> X, morphing with a scale/fade.
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, colors.outline, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                role = Role.Button,
                                onClickLabel = stringResource(R.string.browser_tools),
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onOpenPanel(
                                        if (panel == IslandPanel.Tools) IslandPanel.None else IslandPanel.Tools,
                                    )
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(
                            targetState = panel == IslandPanel.Tools,
                            transitionSpec = {
                                (fadeIn(tween(200)) + scaleIn(initialScale = 0.6f, animationSpec = tween(240))) togetherWith
                                    (fadeOut(tween(140)) + scaleOut(targetScale = 0.6f, animationSpec = tween(140)))
                            },
                            label = "morph",
                        ) { open ->
                            Icon(
                                imageVector = if (open) Lucide.X else Lucide.Ellipsis,
                                contentDescription = stringResource(R.string.browser_tools),
                                tint = colors.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    if (!onRight) {
                        Spacer(Modifier.width(6.dp))
                        BarSearchField(
                            query = query,
                            onQueryChange = onQueryChange,
                            state = state,
                            onProviderClick = onProviderClick,
                            onSubmit = onSearch,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

/** Search field that shows the current page address while browsing. */
@Composable
private fun BarSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    state: VoidAppState,
    onProviderClick: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val pageUrl = if (state.browserOpen) state.currentUrl else ""
    val hint = when {
        pageUrl.isNotBlank() -> pageUrl
        else -> stringResource(R.string.search_hint)
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
    // Provider: its own icon plus a down arrow, no coloured chip behind it.
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onProviderClick,
            )
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Favicon(
            faviconUrl = state.provider.faviconUrl,
            contentDescription = state.provider.label,
            size = 20.dp,
            circleColor = Color.Transparent,
            fallbackSize = 10.sp,
        )
        Icon(
            imageVector = Lucide.ChevronDown,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(13.dp),
        )
    }
    Spacer(Modifier.width(6.dp))

    androidx.compose.foundation.text.BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = colors.onSurface,
            fontSize = 15.sp,
            fontFamily = Poppins,
            fontWeight = FontWeight.Medium,
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(VoidRed),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Search,
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onSearch = { onSubmit() },
        ),
        decorationBox = { inner ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = hint,
                        color = colors.onSurfaceVariant,
                        fontSize = 14.sp,
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
