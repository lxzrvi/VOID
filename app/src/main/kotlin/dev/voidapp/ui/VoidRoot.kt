package dev.voidapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import dev.voidapp.R
import dev.voidapp.ui.browser.BrowserScreen
import dev.voidapp.ui.browser.TabGrid
import dev.voidapp.ui.home.HomeScreen
import dev.voidapp.ui.island.SearchIsland
import dev.voidapp.ui.theme.VoidTheme

/**
 * App shell: home <-> in-app browser, the floating island on top of both, the tab
 * grid that scales up from the island, and the back stack - a page first, then
 * home, then a confirmation before the app closes.
 */
@Composable
fun VoidRoot() {
    val state = rememberVoidAppState()

    // Today's event, fetched once on start (tap on the card retries).
    LaunchedEffect(Unit) {
        if (state.event == null) state.reloadEvent()
    }

    // Shortcut icons: the stored ones are already on screen; this quietly fills
    // in anything a previous launch never managed to fetch.
    LaunchedEffect(Unit) {
        dev.voidapp.ui.components.preloadFavicons(
            state.sites.associate { it.faviconUrl to it.faviconUrl },
        )
    }

    var confirmExit by remember { mutableStateOf(false) }

    // Scroll direction drives the island's slide-away.
    var scrollDelta by remember { mutableFloatStateOf(0f) }
    val onScroll: (Float) -> Unit = { delta -> scrollDelta = delta }

    BackHandler(enabled = true) {
        if (confirmExit) {
            confirmExit = false
            return@BackHandler
        }
        when {
            state.tabGridOpen -> state.tabGridOpen = false
            state.browserOpen -> state.goHome()
            else -> confirmExit = true
        }
    }

    // Status bar icons follow the theme, so the page colour never sits under a
    // black strip with invisible icons.
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !state.darkTheme
            isAppearanceLightNavigationBars = !state.darkTheme
        }
    }

    VoidTheme(darkTheme = state.darkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            AnimatedContent(
                targetState = state.browserOpen,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "root",
            ) { browsing ->
                if (browsing) {
                    BrowserScreen(
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                        onScrollDirection = onScroll,
                    )
                } else {
                    HomeScreen(
                        state = state,
                        onScrollDirection = onScroll,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // The tab grid scales up out of the island and back down into it.
            AnimatedVisibility(
                visible = state.tabGridOpen,
                enter = fadeIn(tween(180)) +
                    scaleIn(initialScale = 0.82f, animationSpec = tween(320)),
                exit = fadeOut(tween(160)) +
                    scaleOut(targetScale = 0.82f, animationSpec = tween(220)),
            ) {
                TabGrid(state = state, modifier = Modifier.fillMaxSize())
            }

            // Toasts: a small phantom pill near the bottom.
            androidx.compose.animation.AnimatedVisibility(
                visible = state.toast != null,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(160)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 84.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = state.toast.orEmpty(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                    )
                }
            }

            // The island floats above every page, clear of the status bar.
            if (!state.tabGridOpen) {
                SearchIsland(
                    state = state,
                    scrollDelta = scrollDelta,
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
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
