package dev.voidapp.ui.island

import androidx.activity.compose.BackHandler
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
    var dragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(state.islandOffset) }
    var barMenu by remember { mutableStateOf(false) }
    var swipeUp by remember { mutableFloatStateOf(0f) }

    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current

    val panel = state.islandPanel
    val frozen = expanded || panel != IslandPanel.None || dragging || state.backgroundPickerAt != null

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
    val restingOffset = if (dragging) dragOffset else state.islandOffset
    val colors = MaterialTheme.colorScheme
    val onRightOffset = with(density) { ((if (onRight) 1f else -1f) * (1f - visibility.value) * 260.dp.toPx()) }

    BoxWithConstraints(
        modifier = modifier
            .imePadding()          // the bar rises with the keyboard
            .fillMaxSize(),
    ) {
        val fullWidth = maxWidth
        val islandWidth = lerpDp(IslandHeight, fullWidth, progress)
        val travelPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = onRightOffset
                    alpha = if (expanded) 1f else visibility.value
                },
            horizontalAlignment = if (onRight) Alignment.End else Alignment.Start,
        ) {
            Spacer(Modifier.weight(restingOffset.coerceIn(0.001f, 0.999f)))
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
                    } else if (state.browserOpen && state.activeTab != null) {
                        state.openInNewTab(state.resolve(query))
                    } else {
                        state.openResult(query, i)
                    }
                    state.hidePanel()
                },
                onTapCircle = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    bump()
                    expanded = true
                },
                onSearch = {
                    bump()
                    if (query.isBlank()) state.open(state.provider.urlFor("")) else state.search(query)
                },
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
                        dragging = true
                        dragOffset = state.islandOffset
                    }
                },
                onDrag = { dy ->
                    if (dragging) dragOffset = (dragOffset + dy / travelPx).coerceIn(0.02f, 0.98f)
                },
                onDragEnd = {
                    if (dragging) {
                        dragging = false
                        state.placeIsland(dragOffset, state.islandOnRight)
                    }
                },
                onSwipeUp = { amount ->
                    if (!expanded) {
                        swipeUp += amount
                        if (swipeUp < -60f) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            state.tabGridOpen = true
                            swipeUp = 0f
                        }
                    }
                },
                onSwipeEnd = { swipeUp = 0f },
            )
            Spacer(Modifier.weight((1f - restingOffset).coerceIn(0.001f, 0.999f)))
        }
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
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLongPressBar: () -> Unit,
    onLongPressStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onSwipeUp: (Float) -> Unit,
    onSwipeEnd: () -> Unit,
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
                        Modifier
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { onLongPressStart() },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        onDrag(amount.y)
                                    },
                                    onDragEnd = { onDragEnd() },
                                    onDragCancel = { onDragEnd() },
                                )
                            }
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, amount -> onSwipeUp(amount) },
                                    onDragEnd = { onSwipeEnd() },
                                    onDragCancel = { onSwipeEnd() },
                                )
                            }
                    } else {
                        Modifier.pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(onDragStart = { onLongPressBar() })
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
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
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
                onClick = { focusManager.clearFocus() },
            )
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Favicon(
            faviconUrl = state.provider.faviconUrl,
            contentDescription = state.provider.label,
            size = 20.dp,
            circleColor = Color.Transparent,
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
            onSearch = { focusManager.clearFocus() },
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
