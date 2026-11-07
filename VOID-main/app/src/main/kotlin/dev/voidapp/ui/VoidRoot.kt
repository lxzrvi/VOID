package dev.voidapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.view.WindowCompat
import dev.voidapp.R
import dev.voidapp.data.SearchProvider
import dev.voidapp.ui.browser.BrowserScreen
import dev.voidapp.ui.browser.TabGrid
import dev.voidapp.ui.components.FaviconStore
import dev.voidapp.ui.home.HomeScreen
import dev.voidapp.ui.island.SearchIsland
import dev.voidapp.ui.library.LibraryScreen
import dev.voidapp.ui.theme.VoidTheme

/** Clips the shrinking page to the card it is landing in (rounded, top-anchored). */
private class PageClip(private val height: Float, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(
            RoundRect(0f, 0f, size.width, height.coerceIn(1f, size.height), CornerRadius(radius, radius)),
        )
}

/**
 * App shell: home <-> in-app browser, the floating island on top of both, the tab
 * grid underneath the page, and the back stack - find bar / fullscreen video /
 * library / tab grid first, then page history, then home, then a confirmation
 * before the app closes.
 *
 * Tab switcher: the page layer shrinks (scale + translate + corner clip) from full
 * screen into the card slot the grid reports for that page, driven by
 * [VoidAppState.tabProgress] - finger driven while the circle is pulled up.
 */
@Composable
fun VoidRoot() {
    val state = rememberVoidAppState()

    LaunchedEffect(Unit) {
        if (state.event == null) state.reloadEvent()
    }

    // Icons that were never fetched before (first launch / new shortcut): one
    // quiet fetch each. Anything already stored is not touched again.
    LaunchedEffect(Unit) {
        FaviconStore.fetchMissing(
            state.sites.map { it.faviconUrl } + SearchProvider.entries.map { it.faviconUrl },
        )
    }

    var confirmExit by remember { mutableStateOf(false) }
    val gridVisible by remember { derivedStateOf { state.tabProgress > 0.0005f || state.tabGridOpen } }

    BackHandler(enabled = true) {
        when {
            confirmExit -> confirmExit = false
            state.libraryOpen -> state.libraryOpen = false
            state.tabGridOpen -> state.closeTabGrid()
            state.findOpen -> state.findOpen = false
            state.browserOpen -> state.browserBack()
            else -> confirmExit = true
        }
    }

    // Status bar icons follow whatever is behind them: the page's own colour in
    // the browser, the brand red on Home, the theme elsewhere.
    val view = LocalView.current
    val pageColor = state.activeRuntime?.pageColor
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        val themeLight = !state.darkTheme
        controller.isAppearanceLightStatusBars = when {
            gridVisible || state.libraryOpen -> themeLight
            state.browserOpen -> pageColor?.let { it.luminance() > 0.5f } ?: themeLight
            else -> false
        }
        controller.isAppearanceLightNavigationBars = themeLight
    }

    VoidTheme(darkTheme = state.darkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            // The tab grid lives UNDER the page.
            if (gridVisible) {
                TabGrid(state = state, modifier = Modifier.fillMaxSize())
            }

            // The page layer: full screen, or shrunk into its card.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = state.tabProgress.coerceIn(0f, 1.04f)
                        if (p > 0.0005f) {
                            val rect = state.activeCardRect ?: Rect(
                                size.width * 0.34f,
                                size.height * 0.22f,
                                size.width * 0.66f,
                                size.height * 0.22f + size.width * 0.32f * 1.75f,
                            )
                            val s = lerp(1f, rect.width / size.width, p)
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = s
                            scaleY = s
                            translationX = lerp(0f, rect.left, p)
                            translationY = lerp(0f, rect.top, p)
                            clip = true
                            shape = PageClip(
                                height = lerp(size.height, rect.height, p) / s,
                                radius = lerp(0f, 14.dp.toPx(), p) / s,
                            )
                        }
                    },
            ) {
                AnimatedContent(
                    targetState = state.browserOpen,
                    transitionSpec = {
                        if (state.tabProgress > 0.01f) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                        }
                    },
                    label = "root",
                ) { browsing ->
                    if (browsing) {
                        BrowserScreen(
                            state = state,
                            modifier = Modifier.fillMaxSize(),
                            onScrollDirection = state::reportScroll,
                        )
                    } else {
                        HomeScreen(
                            state = state,
                            onScrollDirection = state::reportScroll,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                // In the grid the page is just a card: a tap returns to it.
                if (state.tabGridOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { state.closeTabGrid() })
                            },
                    )
                }
            }

            if (state.libraryOpen) {
                LibraryScreen(state = state, modifier = Modifier.fillMaxSize())
            }

            // Toasts: a small pill near the bottom.
            AnimatedVisibility(
                visible = state.toast != null,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(160)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = state.toast.orEmpty(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                    )
                }
            }

            // The island floats above every page. It stays while the tab grid is
            // being pulled open (the circle follows the finger), then goes away.
            AnimatedVisibility(
                visible = !state.tabGridOpen && !state.libraryOpen && state.customView == null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(120)),
                modifier = Modifier.fillMaxSize(),
            ) {
                SearchIsland(
                    state = state,
                    scrollDelta = state.scrollDelta,
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }

        JsDialogHost(state)
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(stringResource(R.string.exit_title)) },
            text = { Text(stringResource(R.string.exit_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    state.requestAppExit()
                }) { Text(stringResource(R.string.exit_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) {
                    Text(stringResource(R.string.exit_cancel))
                }
            },
        )
    }
}

/** A page's alert / confirm / prompt. Always answers the page, so it never hangs. */
@Composable
private fun JsDialogHost(state: VoidAppState) {
    val request = state.jsDialog ?: return
    var text by remember(request) { mutableStateOf(request.defaultValue.orEmpty()) }
    AlertDialog(
        onDismissRequest = {
            state.jsDialog = null
            request.onResult(false, null)
        },
        text = {
            Column {
                Text(request.message)
                if (request.kind == JsDialogRequest.Kind.Prompt) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                state.jsDialog = null
                request.onResult(true, text)
            }) { Text("OK") }
        },
        dismissButton = if (request.kind == JsDialogRequest.Kind.Alert) {
            null
        } else {
            {
                TextButton(onClick = {
                    state.jsDialog = null
                    request.onResult(false, null)
                }) { Text(stringResource(R.string.add_site_cancel)) }
            }
        },
    )
}
