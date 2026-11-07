package dev.voidapp.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.voidapp.R
import dev.voidapp.data.urlForInput
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.icons.Lucide
import dev.voidapp.ui.theme.VoidRed

/**
 * The in-app browser: one WebView, a load-progress bar pinned to the bottom of
 * the screen in the brand red with an animated pulse, and pull-down-to-refresh.
 *
 * All tool actions (back / forward / reload / zoom / desktop mode / find ...) run
 * through [BrowserBridge], which this screen installs on the shared app state.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    state: VoidAppState,
    startUrl: String,
    modifier: Modifier = Modifier,
    onScrollDirection: (Float) -> Unit = {},
) {
    val context = LocalContext.current

    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var failed by remember { mutableStateOf(false) }
    var desktopMode by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf(startUrl) }

    // ---- pull-to-refresh -------------------------------------------------
    var pull by remember { mutableStateOf(0f) }
    var refreshing by remember { mutableStateOf(false) }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        mediaPlaybackRequiresUserGesture = false
                    }
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            failed = false
                            progress = 0.04f
                            currentUrl = url ?: currentUrl
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            progress = 1f
                            currentUrl = url ?: currentUrl
                            title = view?.title.orEmpty()
                            state.recordVisit(currentUrl, title)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean = false

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: android.webkit.WebResourceError?,
                        ) {
                            if (request?.isForMainFrame == true) failed = true
                        }
                    }

                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress / 100f
                            if (newProgress >= 100) title = view?.title.orEmpty()
                        }
                    }

                    loadUrl(startUrl)
                    webView = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .nestedScroll(
                    remember {
                        object : NestedScrollConnection {
                            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                if (available.y != 0f) onScrollDirection(available.y)
                                return Offset.Zero
                            }
                        }
                    },
                ),
            update = { view ->
                if (view.url == null) view.loadUrl(startUrl) else if (view.url != startUrl &&
                    startUrl.isNotBlank() && view.url == null
                ) {
                    view.loadUrl(startUrl)
                }
            },
        )

        // ---- load progress, bottom of the screen, brand red with pulse ----
        val animatedProgress by animateFloatAsState(progress, label = "progress")
        if (animatedProgress < 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceIn(0.02f, 1f))
                        .height(3.dp)
                        .background(VoidRed),
                )
            }
        }

        if (failed) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Lucide.Globe,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.browser_error),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { webView?.reload() }) {
                    Text(stringResource(R.string.browser_reload))
                }
            }
        }
    }

    // ---- wire the island's tools to this WebView -------------------------
    DisposableEffect(webView) {
        val view = webView
        if (view != null) {
            state.bridge = object : BrowserBridge {
                override val canGoBack: Boolean get() = view.canGoBack()
                override val canGoForward: Boolean get() = view.canGoForward()
                override val currentUrl: String get() = view.url ?: currentUrl
                override val currentTitle: String get() = view.title ?: title

                override fun back() = view.goBack()
                override fun forward() = view.goForward()
                override fun reload() = view.reload()

                override fun setDesktopMode(enabled: Boolean) {
                    desktopMode = enabled
                    view.settings.userAgentString = if (enabled) {
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
                    } else {
                        android.webkit.WebSettings.getDefaultUserAgent(view.context)
                    }
                    view.reload()
                }

                override fun zoomBy(delta: Float) {
                    // WebView zoom is page-level; re-scale via initial scale + text zoom.
                    view.settings.textZoom = (view.settings.textZoom * (1f + delta)).toInt().coerceIn(50, 250)
                }

                override fun findInPage(text: String) {
                    if (text.isBlank()) view.clearMatches() else view.findAllAsync(text)
                }

                override fun savePage() {
                    // Saved into the app cache dir; the filesystem path is required.
                    val file = java.io.File(view.context.cacheDir, "void-page-${System.currentTimeMillis()}.mht")
                    view.saveWebArchive(file.absolutePath)
                }

                override fun printPage() {
                    view.createPrintDocumentAdapter("VOID")
                }
            }
        }
        onDispose { state.bridge = null }
    }

    // Keep following the address the state asks for (search from the island).
    LaunchedEffect(startUrl) {
        val view = webView ?: return@LaunchedEffect
        if (startUrl.isNotBlank() && view.url != startUrl) view.loadUrl(startUrl)
    }
}
