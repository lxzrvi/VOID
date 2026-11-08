package dev.voidapp.ui.proto

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import dev.voidapp.data.Site
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.components.Favicon
import dev.voidapp.ui.components.ReorderGrid
import dev.voidapp.ui.home.AddSiteDialog
import dev.voidapp.ui.theme.Poppins
import kotlinx.coroutines.launch

private sealed interface Shortcut {
    data class Entry(val site: Site) : Shortcut
    data object Add : Shortcut
}

/** Scale / letter-spacing / opacity of the VOID wordmark tap, from the prototype's keyframes. */
private fun brandFrame(t: Float): FloatArray {
    fun seg(a: Float, b: Float, x: Float) = lerp(a, b, FastOutSlowInEasing.transform(x.coerceIn(0f, 1f)))
    return when {
        t < 0.35f -> {
            val x = t / 0.35f
            floatArrayOf(seg(1f, 0.86f, x), seg(-2f, 5f, x), seg(1f, 0.45f, x))
        }
        t < 0.7f -> {
            val x = (t - 0.35f) / 0.35f
            floatArrayOf(seg(0.86f, 1.1f, x), seg(5f, 1f, x), seg(0.45f, 1f, x))
        }
        else -> {
            val x = (t - 0.7f) / 0.3f
            floatArrayOf(seg(1.1f, 1f, x), seg(1f, -2f, x), 1f)
        }
    }
}

/**
 * The Homepage, laid out exactly like the prototype: nine shortcuts at 17 % of the
 * height, the light VOID wordmark at 31 %, the search pill at 50 %. Focusing the
 * search pill dims the shortcuts and the wordmark.
 */
@Composable
fun ProtoHome(state: VoidAppState, modifier: Modifier = Modifier) {
    val c = protoColors()
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var searchFocus by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }

    // "Search" in the tools box asks this field for focus (only on a Homepage tab).
    var lastRequest by remember { mutableIntStateOf(state.focusRequest) }
    LaunchedEffect(state.focusRequest) {
        if (state.focusRequest != lastRequest) {
            lastRequest = state.focusRequest
            if (!state.browserOpen) runCatching { focusRequester.requestFocus() }
        }
    }

    val brand = remember { Animatable(0f) }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(c.page)) {
        val w = maxWidth
        val h = maxHeight
        val dim by animateFloatAsState(if (searchFocus) 0.3f else 1f, tween(300), label = "dim")

        // ---- shortcuts ------------------------------------------------------
        val gap = (w * 0.022f).coerceIn(7.dp, 13.dp)
        val iconSize = (w * 0.075f).coerceIn(27.dp, 34.dp)
        val labelSize = with(density) { (w * 0.024f).coerceIn(8.dp, 10.dp).toSp() }
        val sites = state.visibleSites
        val items: List<Shortcut> = sites.map { Shortcut.Entry(it) } + Shortcut.Add

        Box(
            modifier = Modifier
                .offset(y = h * 0.17f)
                .padding(horizontal = w * 0.05f)
                .fillMaxWidth()
                .graphicsLayer { alpha = dim },
        ) {
            ReorderGrid(
                items = items,
                itemKey = { item ->
                    when (item) {
                        is Shortcut.Entry -> item.site.key
                        Shortcut.Add -> "__add__"
                    }
                },
                columns = 9,
                cellHeight = iconSize + 7.dp + 15.dp,
                gap = gap,
                fixedTail = 1,
                stretchLastRow = false,
                onMove = { from, to -> state.moveSite(from, to) },
            ) { item, _, lifted ->
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(if (pressed) 0.86f else 1f, tween(200), label = "press")
                val label = when (item) {
                    is Shortcut.Entry -> item.site.name.take(3).uppercase()
                    Shortcut.Add -> "ADD"
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            enabled = !lifted,
                        ) {
                            when (item) {
                                is Shortcut.Entry -> state.open(item.site.url)
                                Shortcut.Add -> showAdd = true
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        when (item) {
                            is Shortcut.Entry -> Favicon(
                                faviconUrl = item.site.faviconUrl,
                                contentDescription = item.site.name,
                                size = iconSize,
                                circleColor = c.iconBg,
                                shape = RoundedCornerShape(7.dp),
                                fallbackSize = 16.sp,
                                fallbackColor = c.label,
                            )
                            Shortcut.Add -> Box(
                                modifier = Modifier
                                    .size(iconSize)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(c.iconBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("+", color = c.label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Box(Modifier.height(7.dp))
                    Text(
                        text = label,
                        color = c.label,
                        fontFamily = Poppins,
                        fontSize = labelSize,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // ---- brand ----------------------------------------------------------
        val frame = brandFrame(brand.value)
        val brandSize = with(density) { (w * 0.09f).coerceIn(36.dp, 46.dp).toSp() }
        Text(
            text = "VOID",
            color = c.ink,
            fontFamily = Poppins,
            fontWeight = FontWeight.Light,
            fontSize = brandSize,
            letterSpacing = frame[1].sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .offset(y = h * 0.31f)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = dim * frame[2]
                    scaleX = frame[0]
                    scaleY = frame[0]
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    scope.launch {
                        brand.snapTo(0f)
                        brand.animateTo(1f, tween(650, easing = LinearEasing))
                    }
                },
        )

        // While the search pill has focus, a tap anywhere else lets go of it.
        if (searchFocus) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
            )
        }

        // ---- centre search --------------------------------------------------
        val pillShape = RoundedCornerShape(40.dp)
        Box(
            modifier = Modifier
                .offset(y = h * 0.5f)
                .padding(horizontal = w * 0.05f)
                .fillMaxWidth()
                .height(47.dp)
                .clip(pillShape)
                .background(c.page)
                .border(1.dp, if (searchFocus) c.centerFocus else c.centerBorder, pillShape)
                .padding(horizontal = 17.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(color = c.ink, fontSize = 14.sp, fontFamily = Poppins),
                cursorBrush = SolidColor(c.ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    val query = text
                    text = ""
                    focusManager.clearFocus()
                    state.search(query)
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { searchFocus = it.isFocused },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) {
                            Text(
                                text = "Search or enter address",
                                color = c.placeholder,
                                fontSize = 14.sp,
                                fontFamily = Poppins,
                                fontStyle = FontStyle.Normal,
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }

    if (showAdd) {
        AddSiteDialog(
            onDismiss = { showAdd = false },
            onAdd = { name, url ->
                state.addSite(name, url)
                showAdd = false
            },
        )
    }
}
