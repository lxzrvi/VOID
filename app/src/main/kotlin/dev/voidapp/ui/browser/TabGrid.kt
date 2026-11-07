package dev.voidapp.ui.browser

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voidapp.R
import dev.voidapp.ui.BrowserTab
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.icons.Lucide
import dev.voidapp.ui.theme.Poppins
import dev.voidapp.ui.theme.VoidRed
import androidx.compose.ui.res.stringResource

private val CardShape = RoundedCornerShape(16.dp)

/**
 * The tab grid. It opens with the same backdrop as the home page; the page the
 * user came from shrinks from full screen into a phone-shaped card and lands in
 * its slot in a three-column grid.
 */
@Composable
fun TabGrid(
    state: VoidAppState,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var closing by remember { mutableStateOf(false) }
    val enter = remember { Animatable(0f) }
    var order by remember { mutableStateOf(state.tabs.map { it.id }) }

    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)) }

    BackHandler(enabled = true) {
        state.tabGridOpen = false
    }

    // Swipe down anywhere closes the grid, mirroring the swipe up that opened it.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, amount -> if (amount > 9f) state.tabGridOpen = false },
                )
            },
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(56.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.tabs),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = Poppins,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { state.tabGridOpen = false },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Lucide.X,
                        contentDescription = stringResource(R.string.browser_close),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            val currentId = state.activeTabId
            val tabs = order.mapNotNull { id -> state.tabs.firstOrNull { it.id == id } }

            // Three columns, filled row by row.
            tabs.chunked(3).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEachIndexed { col, tab ->
                        val slot = rowIndex * 3 + col
                        TabCard(
                            tab = tab,
                            active = tab.id == currentId,
                            enterProgress = ((enter.value - slot * 0.05f) / 0.6f).coerceIn(0f, 1f),
                            onClick = {
                                state.showTab(tab.id)
                                state.tabGridOpen = false
                            },
                            onClose = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                state.closeTab(tab.id)
                                order = order - tab.id
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Pad only the visual row, never the stored data.
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(12.dp))
            }

            // New tab card.
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f / 3f)
                    .padding(end = 8.dp)
                    .aspectRatio(0.62f)
                    .clip(CardShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CardShape)
                    .clickable {
                        state.newTab("")
                        state.tabGridOpen = false
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Lucide.Plus,
                        contentDescription = stringResource(R.string.tab_new),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.tab_new),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = Poppins,
                        fontSize = 10.sp,
                    )
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun TabCard(
    tab: BrowserTab,
    active: Boolean,
    enterProgress: Float,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = 0.72f + 0.28f * enterProgress
    Box(
        modifier = modifier
            .aspectRatio(0.62f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = enterProgress
            }
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (active) 2.dp else 1.dp,
                color = if (active) VoidRed else MaterialTheme.colorScheme.outline,
                shape = CardShape,
            )
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tab.title.ifBlank { tab.url.ifBlank { stringResource(R.string.tab_new) } },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = Poppins,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Lucide.X,
                        contentDescription = stringResource(R.string.tab_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            // A stand-in preview of the page: the URL, faintly, on its own card.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Lucide.Globe,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = tab.url.removePrefix("https://").removePrefix("http://"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = Poppins,
                fontSize = 8.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
