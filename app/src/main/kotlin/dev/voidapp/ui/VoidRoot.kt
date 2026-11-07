package dev.voidapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.voidapp.R
import dev.voidapp.ui.browser.BrowserScreen
import dev.voidapp.ui.home.HomeScreen
import dev.voidapp.ui.island.SearchIsland

/**
 * App shell: home <-> in-app browser, the floating island on top of both, and the
 * back stack - a page first, then home, then a confirmation before the app closes.
 */
@Composable
fun VoidRoot() {
    val state = rememberVoidAppState()

    // Today's event, fetched once on start (tap on the card retries).
    LaunchedEffect(Unit) {
        if (state.event == null) state.reloadEvent()
    }

    var confirmExit by remember { mutableStateOf(false) }

    // Scroll direction (positive = finger down) drives the island's slide-away.
    var scrollDelta by remember { mutableFloatStateOf(0f) }
    val onScroll: (Float) -> Unit = { delta -> scrollDelta = delta }

    BackHandler(enabled = true) {
        if (confirmExit) {
            confirmExit = false
            return@BackHandler
        }
        when {
            state.browserOpen -> state.goHome()
            state.sheet != VoidAppState.Sheet.None -> state.hideSheet()
            else -> confirmExit = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = state.browserOpen,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "root",
        ) { browsing ->
            if (browsing) {
                BrowserScreen(
                    state = state,
                    startUrl = state.address,
                    onScrollDirection = onScroll,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                HomeScreen(
                    state = state,
                    onScrollDirection = onScroll,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // The island floats above every page.
        SearchIsland(
            state = state,
            scrollDelta = scrollDelta,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
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
