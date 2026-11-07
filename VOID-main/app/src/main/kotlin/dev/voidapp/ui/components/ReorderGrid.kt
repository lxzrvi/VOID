package dev.voidapp.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A grid with real long-press drag-to-reorder.
 *
 * - Every cell has a fixed height; widths fill the row exactly. With
 *   [stretchLastRow] the last (shorter) row is spread over the full width, so the
 *   grid never shows an empty slot.
 * - Long press lifts the cell, it follows the finger 1:1, the others glide out of
 *   the way as soon as the finger crosses another cell, and on release the cell
 *   settles into its slot with a spring.
 * - [fixedHead] cells at the start and [fixedTail] cells at the end cannot be moved
 *   and cannot be displaced.
 */
@Composable
fun <T> ReorderGrid(
    items: List<T>,
    itemKey: (T) -> Any,
    columns: Int,
    cellHeight: Dp,
    gap: Dp,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    fixedHead: Int = 0,
    fixedTail: Int = 0,
    stretchLastRow: Boolean = true,
    onDragActive: (Boolean) -> Unit = {},
    cell: @Composable (item: T, index: Int, lifted: Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val cols = columns.coerceAtLeast(1)
    val gapPx = with(density) { gap.toPx() }
    val cellH = with(density) { cellHeight.toPx() }
    val rows = (items.size + cols - 1) / cols
    val totalHeight = with(density) {
        (rows * cellH + (rows - 1).coerceAtLeast(0) * gapPx).toDp()
    }

    val latestItems by rememberUpdatedState(items)
    val latestOnMove by rememberUpdatedState(onMove)
    val latestHead by rememberUpdatedState(fixedHead)
    val latestTail by rememberUpdatedState(fixedTail)

    var activeKey by remember { mutableStateOf<Any?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(totalHeight)) {
        val widthPx = constraints.maxWidth.toFloat()

        fun slot(index: Int, count: Int): Rect {
            val row = index / cols
            val col = index % cols
            val inRow = if (stretchLastRow) minOf(cols, count - row * cols).coerceAtLeast(1) else cols
            val w = (widthPx - (inRow - 1) * gapPx) / inRow
            val left = col * (w + gapPx)
            val top = row * (cellH + gapPx)
            return Rect(left, top, left + w, top + cellH)
        }

        items.forEachIndexed { index, item ->
            val k = itemKey(item)
            key(k) {
                val canDrag = index >= fixedHead && index < items.size - fixedTail
                val s = slot(index, items.size)
                val isActive = k == activeKey
                val animated by animateIntOffsetAsState(
                    targetValue = IntOffset(s.left.roundToInt(), s.top.roundToInt()),
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                    label = "slot",
                )
                val width by animateFloatAsState(
                    targetValue = s.width,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "width",
                )

                Box(
                    modifier = Modifier
                        .offset {
                            if (isActive) IntOffset(dragPos.x.roundToInt(), dragPos.y.roundToInt()) else animated
                        }
                        .size(width = with(density) { width.toDp() }, height = cellHeight)
                        .zIndex(if (isActive) 2f else 0f)
                        .graphicsLayer {
                            val lift = if (isActive) 1.05f else 1f
                            scaleX = lift
                            scaleY = lift
                            alpha = if (isActive) 0.96f else 1f
                        }
                        .then(
                            if (canDrag) {
                                Modifier.pointerInput(k) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            val list = latestItems
                                            val at = list.indexOfFirst { itemKey(it) == k }
                                            if (at >= 0) {
                                                dragPos = slot(at, list.size).topLeft
                                                activeKey = k
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onDragActive(true)
                                            }
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            if (activeKey != k) return@detectDragGesturesAfterLongPress
                                            dragPos += amount
                                            val list = latestItems
                                            val from = list.indexOfFirst { itemKey(it) == k }
                                            if (from < 0) return@detectDragGesturesAfterLongPress
                                            val me = slot(from, list.size)
                                            val center = Offset(
                                                dragPos.x + me.width / 2f,
                                                dragPos.y + me.height / 2f,
                                            )
                                            val lo = latestHead
                                            val hi = list.size - latestTail - 1
                                            var target = from
                                            for (i in lo..hi) {
                                                if (slot(i, list.size).contains(center)) {
                                                    target = i
                                                    break
                                                }
                                            }
                                            if (target != from) latestOnMove(from, target)
                                        },
                                        onDragEnd = {
                                            settle(k, scope, { latestItems }, itemKey, ::slot, { dragPos }, { dragPos = it }, {
                                                activeKey = null
                                                onDragActive(false)
                                            })
                                        },
                                        onDragCancel = {
                                            settle(k, scope, { latestItems }, itemKey, ::slot, { dragPos }, { dragPos = it }, {
                                                activeKey = null
                                                onDragActive(false)
                                            })
                                        },
                                    )
                                }
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    cell(item, index, isActive)
                }
            }
        }
    }
}

private fun <T> settle(
    key: Any,
    scope: kotlinx.coroutines.CoroutineScope,
    items: () -> List<T>,
    itemKey: (T) -> Any,
    slot: (Int, Int) -> Rect,
    position: () -> Offset,
    setPosition: (Offset) -> Unit,
    done: () -> Unit,
) {
    scope.launch {
        val list = items()
        val at = list.indexOfFirst { itemKey(it) == key }
        if (at < 0) {
            done()
            return@launch
        }
        val from = position()
        val to = slot(at, list.size).topLeft
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        ) { t, _ ->
            setPosition(Offset(lerp(from.x, to.x, t), lerp(from.y, to.y, t)))
        }
        done()
    }
}
