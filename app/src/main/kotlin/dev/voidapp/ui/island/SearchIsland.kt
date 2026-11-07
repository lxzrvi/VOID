package dev.voidapp.ui.island

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import dev.voidapp.R
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.components.AppMark
import dev.voidapp.ui.icons.Lucide
import dev.voidapp.ui.theme.VoidRed
import kotlin.math.abs

private val IslandHeight = 56.dp
private val IslandShape = RoundedCornerShape(IslandHeight / 2)


/**
 * The floating island.
 *
 * - Resting: the app mark in a red circle, snapped to one side at the user's
 *   chosen height. Scrolling the page slides it off that edge; when scrolling
 *   stops it comes back. Long-press + drag moves it vertically (it snaps to the
 *   nearest horizontal side, and the bar then grows the opposite way).
 * - Tap: the circle grows into the full-width bar - search field, a circular
 *   search button (the app icon), and once there is text the arrows with the
 *   1/9 counter plus a bare three-dot glyph in the bar's own colour.
 * - Three dots: a 3x5 tools grid (horizontally scrollable) opens above the bar.
 * - Counter: a sheet with 1..9 opens above the bar.
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
    val panel = state.islandPanel
    var dragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(state.islandOffset) }

    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current

    val frozen = expanded || panel != IslandPanel.None || dragging

    // ---- slide-away on scroll --------------------------------------------
    var hidden by remember { mutableStateOf(false) }
    LaunchedEffect(scrollDelta, frozen) {
        if (!frozen && abs(scrollDelta) > 0.5f) hidden = true
    }
    LaunchedEffect(hidden, frozen) {
        if (hidden && !frozen) {
            kotlinx.coroutines.delay(700)
            hidden = false
        }
    }
    val slide by animateFloatAsState(
        targetValue = if (hidden && !frozen) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
        label = "slide",
    )

    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "progress",
    )

    BackHandler(enabled = expanded) { expanded = false }

    LaunchedEffect(expanded) {
        if (!expanded) {
            state.islandPanel = IslandPanel.None
            state.clearIslandList()
            focusManager.clearFocus()
        }
    }

    val onRight = state.islandOnRight
    val restingOffset = if (dragging) dragOffset else state.islandOffset
    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(modifier = modifier) {
        val fullWidth = maxWidth
        val islandWidth = lerpDp(IslandHeight, fullWidth, progress)
        val slidePx = with(density) { (islandWidth + 32.dp).toPx() }
        val travelPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val direction = if (onRight) 1f else -1f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = direction * slide * slidePx },
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
                },
                index = index,
                resultCount = state.resultCount(query),
                panel = panel,
                onOpenPanel = { state.islandPanel = it },
                sheetItems = state.islandList.ifEmpty { state.results(query) },
                onSelectedIndex = { i ->
                    if (state.islandList.isNotEmpty()) {
                        val pick = state.islandList.getOrNull(i)
                        val entry = state.history.firstOrNull { (it.title.ifBlank { it.url }) == pick }
                            ?: state.bookmarks.firstOrNull { (it.title.ifBlank { it.url }) == pick }
                        if (entry != null) state.open(entry.url) else state.search(pick.orEmpty())
                        state.clearIslandList()
                    } else {
                        state.openResult(query, i)
                    }
                    state.islandPanel = IslandPanel.None
                },
                onTapCircle = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    expanded = true
                },
                onSearch = {
                    if (query.isBlank()) state.open(state.provider.urlFor("")) else state.search(query)
                },
                onPrevious = {
                    if (resultCount(query) > 0) index = (index - 1 + resultCount(query)) % resultCount(query)
                },
                onNext = {
                    if (resultCount(query) > 0) index = (index + 1) % resultCount(query)
                },
                onClear = {
                    query = ""
                    index = 0
                },
                onLongPressStart = {
                    if (!expanded) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        dragging = true
                        dragOffset = state.islandOffset
                    }
                },
                onDrag = { dy ->
                    if (dragging) {
                        dragOffset = (dragOffset + dy / travelPx).coerceIn(0.02f, 0.98f)
                    }
                },
                onDragEnd = {
                    if (dragging) {
                        dragging = false
                        // Side is unchanged (the island always hugs its own edge);
                        // only the vertical resting point is remembered.
                        state.placeIsland(dragOffset, state.islandOnRight)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
            )
            Spacer(Modifier.weight((1f - restingOffset).coerceIn(0.001f, 0.999f)))
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
    sheetItems: List<String>,
    onSelectedIndex: (Int) -> Unit,
    onTapCircle: () -> Unit,
    onSearch: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClear: () -> Unit,
    onLongPressStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val alignment = if (onRight) Alignment.End else Alignment.Start

    Column(horizontalAlignment = alignment, modifier = Modifier.fillMaxWidth()) {
        // ---- panels above the bar ---------------------------------------
        AnimatedVisibility(
            visible = expanded && panel != IslandPanel.None,
            enter = fadeIn(tween(160)) + expandVertically(tween(240)),
            exit = fadeOut(tween(110)) + shrinkVertically(tween(180)),
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
                        onDismiss = { onOpenPanel(IslandPanel.None) },
                    )
                    IslandPanel.Settings -> SettingsSheet(
                        state = state,
                        onDismiss = { onOpenPanel(IslandPanel.None) },
                    )
                    IslandPanel.None -> Unit
                }
            }
        }

        // ---- the bar ----------------------------------------------------
        Row(
            modifier = Modifier
                .width(islandWidth)
                .height(IslandHeight)
                .shadow(lerpDp(0.dp, 10.dp, progress), IslandShape, clip = false)
                .clip(IslandShape)
                .background(lerpColor(VoidRed, colors.surfaceContainerHigh, progress))
                .border(
                    width = lerpDp(0.dp, 1.5.dp, progress),
                    color = lerpColor(Color.Transparent, VoidRed, progress),
                    shape = IslandShape,
                )
                .then(
                    if (!expanded) {
                        Modifier.pointerInput(Unit) {
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
                    } else {
                        Modifier
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
                            start = if (onRight) 20.dp else 8.dp,
                            end = if (onRight) 6.dp else 20.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (onRight) Arrangement.End else Arrangement.Start,
                ) {
                    if (onRight) {
                        SearchField(
                            query = query,
                            onQueryChange = onQueryChange,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                        Spacer(Modifier.width(8.dp))
                    }

                    CircleAction(
                        icon = Lucide.Search,
                        label = stringResource(R.string.search_hint),
                        onClick = onSearch,
                    )

                    if (query.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
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

                    Spacer(Modifier.width(2.dp))
                    // Bare three dots - no chip background, just the bar colour.
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                role = Role.Button,
                                onClickLabel = stringResource(R.string.browser_tools),
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onOpenPanel(if (panel == IslandPanel.Tools) IslandPanel.None else IslandPanel.Tools)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Lucide.Ellipsis,
                            contentDescription = stringResource(R.string.browser_tools),
                            tint = colors.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    if (!onRight) {
                        Spacer(Modifier.width(8.dp))
                        SearchField(
                            query = query,
                            onQueryChange = onQueryChange,
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
