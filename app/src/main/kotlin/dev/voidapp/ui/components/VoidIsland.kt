package dev.voidapp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalFocusManager
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

/** Diameter of the resting circle, and the height of the grown island. */
private val IslandHeight = 56.dp
private val IslandShape = RoundedCornerShape(IslandHeight / 2)
private val PillShape = RoundedCornerShape(20.dp)
private val CounterShape = RoundedCornerShape(8.dp)

/** The trailing button shows three dots, then a close cross. */
private enum class IslandGlyph { Dots, Close }

/**
 * The whole app: a blank page with one floating circle in the bottom-right
 * corner. Tapping the circle grows it smoothly to the full width; the grown
 * island holds a search field on the left (flexible width) and a pill pinned to
 * the right with back / next arrows, a "current/total" counter and the glyph
 * button where the inner icon turns into three dots.
 */
@Composable
fun VoidSearchIsland(
    results: List<String>,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var index by rememberSaveable { mutableStateOf(0) }
    var glyph by rememberSaveable { mutableStateOf(IslandGlyph.Dots) }

    val filtered = remember(results, query) {
        if (query.isBlank()) results else results.filter { it.contains(query, ignoreCase = true) }
    }
    val total = filtered.size

    // Keep the selection inside the current result set.
    LaunchedEffect(total) {
        if (index > total - 1) index = (total - 1).coerceAtLeast(0)
    }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // 0f = resting circle, 1f = full-width island. A single Animatable drives
    // width, colour, shadow and the content cross-fade, so everything moves
    // together in one smooth spring.
    val fraction = remember { Animatable(if (expanded) 1f else 0f) }
    LaunchedEffect(expanded) {
        fraction.animateTo(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = spring(
                dampingRatio = 0.9f,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    LaunchedEffect(expanded) {
        glyph = IslandGlyph.Dots
        if (expanded) focusRequester.requestFocus() else focusManager.clearFocus()
    }

    BackHandler(enabled = expanded) { expanded = false }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        val progress = fraction.value
        val collapsed = progress < 0.5f
        // Captured here: maxWidth belongs to the BoxWithConstraints scope, and the
        // island content below runs inside Box/Row scopes that would shadow it.
        val fullWidth = maxWidth
        val islandWidth = lerpDp(IslandHeight, fullWidth, progress)

        // Tapping the blank page collapses the island again.
        if (!collapsed) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = stringResource(R.string.action_close_search),
                    ) { expanded = false },
            )
        }

        Box(
            modifier = Modifier
                .width(islandWidth)
                .height(IslandHeight)
                .shadow(elevation = lerpDp(0.dp, 6.dp, progress), shape = IslandShape, clip = false)
                .clip(IslandShape)
                .background(lerpColor(VoidRed, MaterialTheme.colorScheme.surfaceVariant, progress))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f * progress),
                    shape = IslandShape,
                )
                .then(
                    if (collapsed) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.action_open_search),
                        ) { expanded = true }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.CenterEnd,
        ) {
            // The grown content is laid out at the final width and revealed by
            // the widening clip, so the pill stays pinned to the right edge and
            // nothing re-flows while the island grows.
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
                        .fillMaxHeight()
                        .focusRequester(focusRequester),
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
                        if (glyph == IslandGlyph.Dots) {
                            glyph = IslandGlyph.Close
                        } else {
                            expanded = false
                        }
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
                    InnerMark(modifier = Modifier.size(24.dp))
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
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.onSurface,
                modifier = Modifier.size(18.dp),
            )
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
