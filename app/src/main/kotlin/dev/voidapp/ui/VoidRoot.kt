package dev.voidapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.view.WindowCompat
import dev.voidapp.R
import dev.voidapp.ui.browser.BrowserScreen
import dev.voidapp.ui.browser.TabGrid
import dev.voidapp.ui.notes.NotesSheet
import dev.voidapp.ui.settings.SettingsScreen
import dev.voidapp.ui.downloads.DownloadConfirmDialog
import dev.voidapp.ui.downloads.DownloadsScreen
import dev.voidapp.ui.downloads.FlyingFileOverlay
import dev.voidapp.ui.library.LibraryScreen
import dev.voidapp.ui.proto.LocalHaze
import dev.voidapp.ui.proto.LocalUiPrefs
import dev.voidapp.ui.proto.Motion
import dev.voidapp.ui.proto.ProtoControls
import dev.voidapp.ui.proto.ProtoHome
import dev.voidapp.ui.theme.VoidTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/** Clips the shrinking page to the card it is landing in (rounded, top-anchored). */
private class PageClip(private val height: Float, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(
            RoundRect(0f, 0f, size.width, height.coerceIn(1f, size.height), CornerRadius(radius, radius)),
        )
}

/**
 * App shell, laid out like the VOID prototype: the page (Homepage or website) fills
 * the screen and the controls float over it - tab row, home/search pill and the
 * nav pill that opens the tabs box and the tools box. "Tabs overview" shrinks the
 * page into its card in the tab grid, which lives underneath.
 *
 * Back: find bar / fullscreen video / library / sheets / tab grid / menu first,
 * then page history, then a confirmation before the app closes.
 */
@Composable
fun VoidRoot() {
    val state = rememberVoidAppState()
    state.systemDark = isSystemInDarkTheme()
    val haze = remember { HazeState() }
    val motion = remember(state.ui.animStyle, state.ui.animSpeed) { Motion(state.ui.animStyle, state.ui.animSpeed) }
    LaunchedEffect(Unit) {
        state.applyKeepAwake()
        state.applyWebSettings()
    }

    var confirmExit by remember { mutableStateOf(false) }
    val gridVisible by remember { derivedStateOf { state.tabProgress > 0.0005f || state.tabGridOpen } }

    BackHandler(enabled = true) {
        when {
            confirmExit -> confirmExit = false
            state.downloadsOpen -> state.downloadsOpen = false
            state.libraryOpen -> state.libraryOpen = false
            state.settingsOpen -> state.settingsOpen = false
            state.islandPanel != IslandPanel.None -> state.hidePanel()
            state.tabGridOpen -> state.closeTabGrid()
            state.findOpen -> state.findOpen = false
            state.menuOpen -> state.closeMenu()
            state.browserOpen -> state.browserBack()
            !state.backFromHomepage() -> confirmExit = true
        }
    }

    // Status bar icons follow what is behind them: the page's own colour in the
    // browser, the theme everywhere else.
    val view = LocalView.current
    val pageColor = state.activeRuntime?.pageColor
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        val themeLight = !state.darkTheme
        controller.isAppearanceLightStatusBars = when {
            gridVisible || state.libraryOpen || state.settingsOpen || state.downloadsOpen -> themeLight
            state.browserOpen && state.ui.matchStatusBar -> pageColor?.let { it.luminance() > 0.5f } ?: themeLight
            else -> {
                // Over the Homepage glow: white icons on a dark colour, dark icons on a light one.
                val ui = state.ui
                if (ui.homeGradient && ui.gradHeight >= 6f) {
                    (ui.gradStart ?: dev.voidapp.ui.proto.BrandRed).copy(alpha = 1f).luminance() > 0.5f
                } else {
                    themeLight
                }
            }
        }
        controller.isAppearanceLightNavigationBars = themeLight
    }

    val baseDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalUiPrefs provides state.ui,
        LocalHaze provides haze,
        LocalDensity provides Density(baseDensity.density, baseDensity.fontScale * state.ui.uiScale),
    ) {
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
                        // Once the grid is open the live page hands over to its card.
                        alpha = if (state.tabGridOpen) {
                            1f - ((p - 0.9f) / 0.1f).coerceIn(0f, 1f)
                        } else {
                            1f
                        }
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
                    }
                    .hazeSource(state = haze),
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
                    label = "page",
                ) { browsing ->
                    if (browsing) {
                        BrowserScreen(state = state, modifier = Modifier.fillMaxSize())
                    } else {
                        ProtoHome(state = state, modifier = Modifier.fillMaxSize())
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

            ScreenOverlay(visible = state.settingsOpen, motion = motion) {
                SettingsScreen(state = state, modifier = Modifier.fillMaxSize())
            }
            ScreenOverlay(visible = state.libraryOpen, motion = motion) {
                LibraryScreen(state = state, modifier = Modifier.fillMaxSize())
            }
            ScreenOverlay(visible = state.downloadsOpen, motion = motion) {
                DownloadsScreen(state = state, modifier = Modifier.fillMaxSize())
            }

            // The prototype's controls float over the page.
            AnimatedVisibility(
                visible = !state.tabGridOpen && !state.libraryOpen && !state.settingsOpen &&
                    !state.downloadsOpen && state.customView == null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(120)),
                modifier = Modifier.fillMaxSize(),
            ) {
                ProtoControls(state = state, modifier = Modifier.fillMaxSize())
            }

            SheetHost(state)

            // The file icon of a confirmed download, flying into the bottom circle.
            FlyingFileOverlay(state)

            // Toasts: a small pill above the controls.
            AnimatedVisibility(
                visible = state.toast != null,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(160)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 150.dp),
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
        }

        JsDialogHost(state)
        DownloadConfirmDialog(state)
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
}

/** A full screen that slides up with a spring and fades, and slides back down. */
@Composable
private fun ScreenOverlay(visible: Boolean, motion: Motion, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(motion.screen()) { it / 6 } + fadeIn(tween(if (motion.instant) 1 else 200)),
        exit = slideOutVertically(motion.screen()) { it / 6 } + fadeOut(tween(if (motion.instant) 1 else 160)),
        modifier = Modifier.fillMaxSize(),
    ) {
        content()
    }
}

/** Notes open as a card above the controls; a tap outside closes it. */
@Composable
private fun SheetHost(state: VoidAppState) {
    if (state.islandPanel != IslandPanel.Notes) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f))
            .pointerInput(Unit) { detectTapGestures(onTap = { state.hidePanel() }) },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 80.dp)
                .pointerInput(Unit) { detectTapGestures(onTap = { }) },
        ) {
            NotesSheet(state = state)
        }
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
