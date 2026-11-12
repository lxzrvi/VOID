package dev.voidapp.ui.browser

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import dev.voidapp.R
import dev.voidapp.ui.VoidAppState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import dev.voidapp.ui.theme.Poppins
import dev.voidapp.ui.theme.VoidRed

/**
 * The in-app browser.
 *
 * - One WebView per tab, kept alive in [VoidAppState]; switching tabs never reloads.
 * - The WebView is edge to edge. The page content starts BELOW the status bar (an
 *   inset injected into the page), and while scrolling the content passes under
 *   the transparent bar, softened by a scrim tinted with the page's own colour.
 *   The status-bar icons switch light/dark to match that colour.
 * - Find bar, file picker, permissions, fullscreen video, downloads and pop-ups
 *   are handled here and in [createTabWebView].
 */
@Composable
fun BrowserScreen(
    state: VoidAppState,
    modifier: Modifier = Modifier,
    onScrollDirection: (Float) -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val tab = state.activeTab
    if (tab == null) {
        Box(modifier.background(MaterialTheme.colorScheme.background))
        return
    }
    val tabId = tab.id
    val runtime = state.runtimeFor(tabId)

    // The status bar height, as CSS pixels for the page.
    val insetPx = WindowInsets.statusBars.getTop(density)
    state.statusInsetPx = insetPx
    val insetDp = with(density) { insetPx.toDp() }
    // Mode 0: the page sits BELOW the status bar, so no header of any site can slide
    // under it. Mode 1: the page starts under the bar and the band scrolls away.
    val reserved = state.ui.statusMode == 0
    val topCss = if (reserved) 0f else insetPx / density.density
    state.pageTopInsetDp = topCss
    LaunchedEffect(topCss, state.pageBottomInsetDp) {
        state.applyInsetToPages(topCss, state.pageBottomInsetDp)
    }

    val webView = remember(tabId, runtime.generation) {
        state.webViewFor(tabId) { createTabWebView(context, state, tabId) }
    }

    // ---- activity bound helpers: file chooser + permission prompts ---------
    var pendingFiles by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var pendingPermissions by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = pendingFiles
        pendingFiles = null
        callback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val callback = pendingPermissions
        pendingPermissions = null
        callback?.invoke(results.isNotEmpty() && results.values.all { it })
    }

    DisposableEffect(Unit) {
        state.host = object : BrowserHost {
            override fun chooseFile(
                callback: ValueCallback<Array<Uri>>,
                params: WebChromeClient.FileChooserParams,
            ): Boolean {
                pendingFiles?.onReceiveValue(null)
                pendingFiles = callback
                return try {
                    fileLauncher.launch(params.createIntent())
                    true
                } catch (e: Exception) {
                    pendingFiles = null
                    callback.onReceiveValue(null)
                    true
                }
            }

            override fun requestPermissions(permissions: Array<String>, onResult: (Boolean) -> Unit) {
                pendingPermissions?.invoke(false)
                pendingPermissions = onResult
                permissionLauncher.launch(permissions)
            }
        }
        onDispose { state.host = null }
    }

    DisposableEffect(webView) {
        webView.onResume()
        state.reportScrollY(webView.scrollY)
        onDispose { webView.onPause() }
    }

    val surface = MaterialTheme.colorScheme.background
    val pageColor = runtime.pageColor ?: surface

    Box(modifier = modifier.background(surface)) {
        // The page, edge to edge.
        AndroidView(
            factory = { ctx -> FrameLayout(ctx) },
            update = { frame ->
                if (frame.childCount != 1 || frame.getChildAt(0) !== webView) {
                    frame.removeAllViews()
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    frame.addView(
                        webView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                }
            },
            onRelease = { frame -> frame.removeAllViews() },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (reserved) insetDp else 0.dp),
        )

        // Status bar band: wears the colour of the page's top and scrolls away WITH the
        // page, so once you scroll the site is shown full screen.
        val bandColor = if (state.ui.matchStatusBar) {
            runtime.topColor ?: runtime.pageColor ?: surface
        } else {
            surface
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(insetDp)
                .graphicsLayer {
                    translationY = if (reserved) 0f else -minOf(state.pageScrollY.toFloat(), insetPx.toFloat())
                }
                .background(bandColor),
        )

        // Load progress: a line at the very bottom of the screen.
        val animatedProgress by animateFloatAsState(runtime.progress, label = "progress")
        if (animatedProgress < 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(animatedProgress.coerceIn(0.02f, 1f))
                    .height(3.dp)
                    .background(state.ui.accent ?: VoidRed),
            )
        }

        if (state.findOpen) {
            FindBar(
                state = state,
                // Find in page lives at the bottom, riding up with the keyboard.
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }

        val error = runtime.error
        if (error != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.browser_error),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Poppins,
                    fontSize = 11.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = {
                        runtime.error = null
                        webView.reload()
                    }) { Text(stringResource(R.string.browser_reload)) }
                    TextButton(onClick = { state.openCurrentExternally() }) {
                        Text(stringResource(R.string.browser_open_external))
                    }
                }
            }
        }
    }

    // ---- fullscreen video -----------------------------------------------------
    val custom = state.customView
    if (custom != null) {
        BackHandler { state.exitCustomView() }
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx -> FrameLayout(ctx) },
                update = { frame ->
                    if (frame.getChildAt(0) !== custom) {
                        frame.removeAllViews()
                        (custom.parent as? ViewGroup)?.removeView(custom)
                        frame.addView(custom, FrameLayout.LayoutParams(-1, -1))
                    }
                },
                onRelease = { frame -> frame.removeAllViews() },
                modifier = Modifier.fillMaxSize(),
            )
        }
        val view = LocalView.current
        DisposableEffect(custom) {
            val window = (view.context as? Activity)?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
            onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
        }
    }

    // ---- wire the island's tools to this WebView ------------------------------
    DisposableEffect(webView) {
        state.bridge = object : BrowserBridge {
            override val canGoBack: Boolean get() = webView.canGoBack()
            override val canGoForward: Boolean get() = webView.canGoForward()
            override val currentUrl: String get() = webView.url ?: state.address
            override val currentTitle: String get() = webView.title ?: ""

            override fun back() = webView.goBack()
            override fun forward() = webView.goForward()
            override fun reload() = webView.reload()
            override fun scrollToTop() = webView.scrollTo(0, 0)
            override fun scrollToBottom() {
                webView.pageDown(true)
            }
            override fun stop() = webView.stopLoading()
            override fun evaluate(js: String, onResult: (String) -> Unit) {
                webView.evaluateJavascript(js) { onResult(it.orEmpty()) }
            }
            override fun saveArchive(path: String, done: (Boolean) -> Unit) {
                webView.saveWebArchive(path, false) { done(it != null) }
            }
            override fun resetZoom() {
                webView.settings.textZoom = 100
            }

            override fun setDesktopMode(enabled: Boolean) {
                applyWebPrefs(webView, state, enabled)
                webView.reload()
            }

            override fun applyWebDark(enabled: Boolean) {
                state.forEachWebView { view ->
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                        WebSettingsCompat.setAlgorithmicDarkeningAllowed(view.settings, enabled)
                    }
                }
            }

            override fun zoomBy(delta: Float) {
                val next = (webView.settings.textZoom + (delta * 100).toInt()).coerceIn(50, 300)
                webView.settings.textZoom = next
                state.showToast("$next%")
            }

            override fun find(query: String) {
                if (query.isEmpty()) {
                    webView.clearMatches()
                    runtime.findCount = 0
                    runtime.findIndex = 0
                } else {
                    webView.findAllAsync(query)
                }
            }

            override fun findNext(forward: Boolean) = webView.findNext(forward)

            override fun clearFind() = webView.clearMatches()

            override fun printPage() {
                val manager = webView.context.getSystemService(android.content.Context.PRINT_SERVICE) as PrintManager
                val name = (webView.title ?: "VOID page").ifBlank { "VOID page" }
                manager.print(name, webView.createPrintDocumentAdapter(name), PrintAttributes.Builder().build())
            }

            override fun snapshot(width: Int): android.graphics.Bitmap? = runCatching {
                if (webView.width <= 0 || webView.height <= 0) return@runCatching null
                val scale = width.toFloat() / webView.width
                val bitmap = android.graphics.Bitmap.createBitmap(
                    width,
                    (webView.height * scale).toInt().coerceAtLeast(1),
                    android.graphics.Bitmap.Config.ARGB_8888,
                )
                val canvas = android.graphics.Canvas(bitmap)
                canvas.scale(scale, scale)
                webView.draw(canvas)
                bitmap
            }.getOrNull()
        }
        onDispose { state.bridge = null }
    }

    // Closing the find bar also clears the highlights.
    LaunchedEffect(state.findOpen) {
        if (!state.findOpen) webView.clearMatches()
    }
}

/** Find in page: field, "2/7" counter, previous / next and close. */
@Composable
private fun FindBar(state: VoidAppState, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val runtime = state.runtimeFor(state.activeTabId)
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(20.dp)

    LaunchedEffect(Unit) { focus.requestFocus() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(colors.surface)
            .border(1.5.dp, colors.outline, shape)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (text.isEmpty()) {
                Text(
                    text = stringResource(R.string.browser_find_hint),
                    color = colors.onSurfaceVariant,
                    fontFamily = Poppins,
                    fontSize = 13.sp,
                )
            }
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    state.bridge?.find(it)
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = colors.onSurface,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                ),
                cursorBrush = SolidColor(VoidRed),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    state.bridge?.findNext(true)
                    focusManager.clearFocus()
                }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        if (text.isNotEmpty()) {
            val total = runtime.findCount
            Text(
                text = if (total == 0) "0/0" else "${runtime.findIndex + 1}/$total",
                color = colors.onSurfaceVariant,
                fontFamily = Poppins,
                fontSize = 11.sp,
            )
            Spacer(Modifier.width(2.dp))
        }
        FindIcon(Icons.Filled.KeyboardArrowUp) { state.bridge?.findNext(false) }
        FindIcon(Icons.Filled.KeyboardArrowDown) { state.bridge?.findNext(true) }
        FindIcon(Icons.Filled.Close) { state.findOpen = false }
    }
}

@Composable
private fun FindIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
    }
}
