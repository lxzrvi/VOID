package dev.voidapp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.compose.ui.unit.sp
import dev.voidapp.R
import dev.voidapp.ui.theme.VoidRed

/** Diameter of the resting circle, and the height of the grown bar. */
private val IslandHeight = 56.dp

/** Margin of the circle from the bottom-right corner of the screen. */
private val EdgeMargin = 16.dp
private val IslandShape = RoundedCornerShape(IslandHeight / 2)
private val PillShape = RoundedCornerShape(20.dp)
private val CounterShape = RoundedCornerShape(8.dp)

/** The trailing button shows three dots, then a close cross. */
private enum class IslandGlyph { Dots, Close }

/**
 * The whole app.
 *
 * Resting: a blank page with one red circle pinned to the **bottom-right**
 * corner, showing the VOID inner mark.
 *
 * Tapping the circle grows it smoothly to the full width. The grown bar holds a
 * search field on the left (flexible width) and a pill on the right with
 * back / next arrows, a "current/total" counter and the glyph button where the
 * inner icon turns into three dots.
 */
@Composable
fun VoidSearchIsland(
    results: List<String>,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var glyph by rememberSaveable { mutableStateOf(IslandGlyph.Dots) }

    // 0f = resting circle, 1f = full-width bar. One value drives the width, the
    // colour, the shadow and the content cross-fade, so they always move together.
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "islandProgress",
    )

    val filtered = remember(results, query) {
        if (query.isBlank()) results else results.filter { it.contains(query, ignoreCase = true) }
    }
    val total = filtered.size

    LaunchedEffect(total) {
        if (index > total - 1) index = (total - 1).coerceAtLeast(0)
    }

    val focusManager = LocalFocusManager.current
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(expanded) {
        glyph = IslandGlyph.Dots
        // Opening never steals focus: the bar just grows. The keyboard appears
        // when the search field itself is tapped.
        if (!expanded) focusManager.clearFocus()
    }

    BackHandler(enabled = expanded) { expanded = false }

    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = EdgeMargin, vertical = EdgeMargin),
    ) {
        // maxWidth belongs to this scope; the island's inner Box/Row lambdas
        // would otherwise shadow it.
        val fullWidth = maxWidth
        val islandWidth = lerpDp(IslandHeight, fullWidth, progress)
        val collapsed = progress < 0.5f

        // Tapping the blank page collapses the bar again.
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = stringResource(R.string.action_close_search),
                    ) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        expanded = false
                    },
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)          // bottom-right corner
                .width(islandWidth)
                .height(IslandHeight)
                .shadow(
                    elevation = lerpDp(0.dp, 8.dp, progress),
                    shape = IslandShape,
                    clip = false,
                )
                .clip(IslandShape)
                .background(
                    lerpColor(VoidRed, colors.surfaceContainerHigh, progress),
                )
                .border(
                    width = 1.dp,
                    color = lerpColor(Color.Transparent, colors.outline, progress),
                    shape = IslandShape,
                )
                .then(
                    if (collapsed) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White),
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.action_open_search),
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            expanded = true
                        }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.CenterEnd,
        ) {
            // Grown content is laid out at the final width and revealed by the
            // widening clip, so the pill stays pinned to the right edge and
            // nothing re-flows while the bar grows.
            Row(
                modifier = Modifier
                    .requiredWidth(fullWidth)
                    .fillMaxHeight()
                    .padding(start = 22.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                Spacer(Modifier.width(8.dp))
                IslandPill(
                    counter = stringResource(
                        R.string.result_counter,
                        if (total == 0) 0 else index + 1,
                        total,
                    ),
                    glyph = glyph,
                    onPrevious = { if (total > 0) index = (index - 1 + total) % total },
                    onNext = { if (total > 0) index = (index + 1) % total },
                    onGlyphClick = {
                        if (glyph == IslandGlyph.Dots) glyph = IslandGlyph.Close else expanded = false
                    },
                )
            }

            // Resting circle content: the VOID inner mark, centred in the circle.
            AnimatedVisibility(
                visible = collapsed,
                modifier = Modifier.align(Alignment.CenterEnd),
                enter = fadeIn(tween(140)) + scaleIn(initialScale = 0.6f),
                exit = fadeOut(tween(110)) + scaleOut(targetScale = 0.6f),
            ) {
                Box(
                    modifier = Modifier
                        .width(IslandHeight)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    InnerMark(modifier = Modifier.size(21.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val textStyle = TextStyle(
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
        textStyle = textStyle,
        cursorBrush = SolidColor(VoidRed),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_hint),
                        style = textStyle,
                        color = colors.onSurfaceVariant,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun IslandPill(
    counter: String,
    glyph: IslandGlyph,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGlyphClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(PillShape)
            .background(colors.surface)
            .border(1.dp, colors.outline, PillShape)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillIconButton(
            icon = Lucide.ArrowLeft,
            label = stringResource(R.string.action_previous),
            onClick = onPrevious,
        )
        CounterBadge(text = counter)
        PillIconButton(
            icon = Lucide.ArrowRight,
            label = stringResource(R.string.action_next),
            onClick = onNext,
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .width(1.dp)
                .height(18.dp)
                .background(colors.outline),
        )
        GlyphButton(glyph = glyph, onClick = onGlyphClick)
    }
}

@Composable
private fun PillIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(32.dp)
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
private fun CounterBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .height(26.dp)
            .clip(CounterShape)
            .background(colors.surfaceVariant)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(90)) },
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
}

/** Trailing circle of the pill: inner icon, then three dots, then a close cross. */
@Composable
private fun GlyphButton(
    glyph: IslandGlyph,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val label = when (glyph) {
        IslandGlyph.Dots -> stringResource(R.string.action_more)
        IslandGlyph.Close -> stringResource(R.string.action_close_search)
    }

    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = glyph,
            transitionSpec = {
                (fadeIn(tween(180)) + scaleIn(initialScale = 0.5f, animationSpec = tween(220))) togetherWith
                    (fadeOut(tween(110)) + scaleOut(targetScale = 0.5f, animationSpec = tween(110)))
            },
            label = "glyph",
        ) { state ->
            val icon = when (state) {
                IslandGlyph.Dots -> Lucide.Ellipsis
                IslandGlyph.Close -> Lucide.X
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = colors.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * The app mark (icon/void_inner_icon.svg), tinted white so it reads on the red
 * circle exactly like the launcher icon.
 */
@Composable
fun InnerMark(modifier: Modifier = Modifier) {
    val painter = rememberVectorPainter(ImageVector.vectorResource(R.drawable.void_inner_icon))
    Image(
        painter = painter,
        contentDescription = null,
        colorFilter = ColorFilter.tint(Color.White),
        modifier = modifier,
    )
}
