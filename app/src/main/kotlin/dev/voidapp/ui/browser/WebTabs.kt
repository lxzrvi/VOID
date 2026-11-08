package dev.voidapp.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import dev.voidapp.data.AdBlock
import dev.voidapp.ui.JsDialogRequest
import dev.voidapp.ui.VoidAppState
import java.io.ByteArrayInputStream

private const val DESKTOP_UA =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"

/**
 * Document-start script. It keeps page content below the status bar (padding on
 * <html>, plus a nudge for fixed headers glued to the top edge), while scrolled
 * content still passes UNDER the transparent status bar.
 */
private fun insetScript(topPx: Float, bottomPx: Float): String = """
(function(){
  if (window.__voidInsetInstalled) { window.__voidSetInset($topPx, $bottomPx); return; }
  window.__voidInsetInstalled = true;
  var top = $topPx;
  var bottom = $bottomPx;
  function css(){
    var s = document.getElementById('__void_inset');
    if (!s) {
      s = document.createElement('style'); s.id = '__void_inset';
      (document.head || document.documentElement).appendChild(s);
    }
    s.textContent = 'html{padding-top:' + top + 'px !important;padding-bottom:' + bottom + 'px !important;scroll-padding-top:' + top + 'px !important;}';
  }
  function fix(){
    try {
      var all = (document.body || document.documentElement).querySelectorAll('*');
      var n = Math.min(all.length, 600);
      for (var i = 0; i < n; i++) {
        var e = all[i];
        if (e.__voidShifted) continue;
        var cs = getComputedStyle(e);
        if (cs.position === 'fixed' && parseFloat(cs.top) === 0 && e.offsetWidth > 0) {
          e.style.setProperty('top', top + 'px', 'important');
          e.__voidShifted = true;
        }
      }
    } catch (x) {}
  }
  window.__voidSetInset = function(t, b){ top = t; if (b !== undefined) bottom = b; css(); fix(); };
  css();
  document.addEventListener('DOMContentLoaded', function(){ css(); fix(); setTimeout(fix, 900); setTimeout(fix, 2600); });
})();
"""

private const val COLOR_SCRIPT = """
(function(){
  var m = document.querySelector('meta[name=theme-color]');
  var c = m && m.content;
  if (!c) {
    var b = document.body && getComputedStyle(document.body).backgroundColor;
    var h = getComputedStyle(document.documentElement).backgroundColor;
    c = (b && b !== 'rgba(0, 0, 0, 0)' && b !== 'transparent') ? b : h;
  }
  return c || '';
})()
"""

/** Parses "rgb(…)", "rgba(…)", "#rgb" and "#rrggbb"; anything else is null. */
fun parseCssColor(raw: String?): Color? {
    val text = raw?.trim()?.removeSurrounding("\"")?.lowercase().orEmpty()
    if (text.isEmpty()) return null
    runCatching {
        if (text.startsWith("#")) {
            val hex = text.removePrefix("#")
            val full = if (hex.length == 3) hex.map { "$it$it" }.joinToString("") else hex
            if (full.length >= 6) {
                val v = full.substring(0, 6).toLong(16)
                return Color(
                    red = ((v shr 16) and 0xFF) / 255f,
                    green = ((v shr 8) and 0xFF) / 255f,
                    blue = (v and 0xFF) / 255f,
                )
            }
        }
        if (text.startsWith("rgb")) {
            val parts = text.substringAfter("(").substringBefore(")")
                .split(",", " ", "/").map { it.trim() }.filter { it.isNotEmpty() }
            val r = parts[0].toFloat()
            val g = parts[1].toFloat()
            val b = parts[2].toFloat()
            val a = parts.getOrNull(3)?.toFloatOrNull() ?: 1f
            if (a < 0.05f) return null
            return Color(red = r / 255f, green = g / 255f, blue = b / 255f)
        }
    }
    return null
}

/** Builds the WebView of one tab, wired to [state]. */
@SuppressLint("SetJavaScriptEnabled")
fun createTabWebView(context: Context, state: VoidAppState, tabId: Long): WebView {
    val runtime = state.runtimeFor(tabId)
    val density = context.resources.displayMetrics.density
    val startTab = state.tabs.firstOrNull { it.id == tabId }

    fun insetCss(): Float = state.statusInsetPx / density

    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        overScrollMode = View.OVER_SCROLL_NEVER

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setGeolocationEnabled(true)
            safeBrowsingEnabled = true
            if (startTab?.desktop == true) userAgentString = DESKTOP_UA
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, state.webDark)
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(
                this,
                insetScript(insetCss(), state.pageBottomInsetDp),
                setOf("*"),
            )
        }

        // Colour of the page, for the status-bar scrim and icon contrast.
        fun sampleColor(view: WebView) {
            view.evaluateJavascript(COLOR_SCRIPT) { value ->
                runtime.pageColor = parseCssColor(value)
            }
        }
        val colorRunnable = Runnable { sampleColor(this) }

        setOnScrollChangeListener { v, _, scrollY, _, oldScrollY ->
            if (tabId == state.activeTabId) {
                state.reportScroll((oldScrollY - scrollY).toFloat())
                state.reportScrollY(scrollY)
            }
            v.removeCallbacks(colorRunnable)
            v.postDelayed(colorRunnable, 260)
        }

        setDownloadListener { url, userAgent, disposition, mime, _ ->
            state.startDownload(url, userAgent, disposition, mime)
        }

        setFindListener { activeOrdinal, numberOfMatches, _ ->
            runtime.findIndex = activeOrdinal
            runtime.findCount = numberOfMatches
        }

        webViewClient = object : WebViewClient() {
            private fun refreshFlags(view: WebView) {
                runtime.canGoBack = view.canGoBack()
                runtime.canGoForward = view.canGoForward()
            }

            private fun injectInset(view: WebView) {
                // Fallback for WebViews without document-start scripts.
                view.evaluateJavascript(insetScript(insetCss(), state.pageBottomInsetDp), null)
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                runtime.error = null
                runtime.progress = 0.04f
                if (url != null) state.onTabNavigated(tabId, url, null)
                if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) injectInset(view)
            }

            override fun onPageCommitVisible(view: WebView, url: String?) {
                view.evaluateJavascript(
                    "window.__voidSetInset&&window.__voidSetInset(${insetCss()},${state.pageBottomInsetDp})",
                    null,
                )
                sampleColor(view)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                runtime.progress = 1f
                refreshFlags(view)
                val finalUrl = url ?: view.url ?: ""
                state.recordVisit(tabId, finalUrl, view.title.orEmpty())
                if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) injectInset(view)
                sampleColor(view)
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                refreshFlags(view)
                if (url != null) state.onTabNavigated(tabId, url, null)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                return when (uri.scheme?.lowercase()) {
                    "http", "https", "about", "data", "blob", "javascript", "file" -> false
                    else -> {
                        openExternally(view, uri)
                        true
                    }
                }
            }

            private fun openExternally(view: WebView, uri: Uri) {
                val ctx = view.context
                runCatching {
                    if (uri.scheme == "intent") {
                        val intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME).apply {
                            addCategory(Intent.CATEGORY_BROWSABLE)
                            component = null
                            selector = null
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        val fallback = intent.getStringExtra("browser_fallback_url")
                        if (intent.resolveActivity(ctx.packageManager) != null) {
                            ctx.startActivity(intent)
                        } else if (!fallback.isNullOrBlank()) {
                            view.loadUrl(fallback)
                        }
                    } else {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (state.blockAds && !request.isForMainFrame && AdBlock.isBlocked(request.url.host)) {
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                }
                return null
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    runtime.error = error.description?.toString().orEmpty().ifBlank { "Page could not be loaded" }
                    runtime.progress = 1f
                }
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel()
                runtime.error = "This connection is not private (certificate problem)"
                runtime.progress = 1f
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                // Keep the app alive; rebuild this tab's WebView and reload it.
                state.recreateWebView(tabId)
                return true
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                runtime.progress = newProgress / 100f
            }

            override fun onReceivedTitle(view: WebView, title: String?) {
                if (!title.isNullOrBlank()) state.onTabTitle(tabId, title)
            }

            // ---- fullscreen video -----------------------------------------
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                state.customViewCallback?.onCustomViewHidden()
                state.customViewCallback = callback
                state.customView = view
            }

            override fun onHideCustomView() {
                state.exitCustomView()
            }

            // ---- <input type=file> ----------------------------------------
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams,
            ): Boolean {
                val host = state.host
                if (host == null) {
                    filePathCallback.onReceiveValue(null)
                    return true
                }
                return host.chooseFile(filePathCallback, fileChooserParams)
            }

            // ---- camera / microphone --------------------------------------
            override fun onPermissionRequest(request: PermissionRequest) {
                val needed = request.resources.mapNotNull {
                    when (it) {
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> android.Manifest.permission.CAMERA
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> android.Manifest.permission.RECORD_AUDIO
                        else -> null
                    }
                }
                if (needed.isEmpty()) {
                    request.deny()
                    return
                }
                val missing = needed.filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }
                if (missing.isEmpty()) {
                    request.grant(request.resources)
                    return
                }
                val host = state.host
                if (host == null) {
                    request.deny()
                    return
                }
                host.requestPermissions(missing.toTypedArray()) { granted ->
                    if (granted) request.grant(request.resources) else request.deny()
                }
            }

            // ---- location -------------------------------------------------
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback,
            ) {
                val fine = android.Manifest.permission.ACCESS_FINE_LOCATION
                if (ContextCompat.checkSelfPermission(context, fine) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false)
                    return
                }
                val host = state.host
                if (host == null) {
                    callback.invoke(origin, false, false)
                    return
                }
                host.requestPermissions(
                    arrayOf(fine, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                ) { granted -> callback.invoke(origin, granted, false) }
            }

            // ---- alert / confirm / prompt ---------------------------------
            override fun onJsAlert(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
                state.jsDialog = JsDialogRequest(JsDialogRequest.Kind.Alert, message.orEmpty(), null) { _, _ ->
                    result.confirm()
                }
                return true
            }

            override fun onJsConfirm(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
                state.jsDialog = JsDialogRequest(JsDialogRequest.Kind.Confirm, message.orEmpty(), null) { ok, _ ->
                    if (ok) result.confirm() else result.cancel()
                }
                return true
            }

            override fun onJsPrompt(
                view: WebView,
                url: String?,
                message: String?,
                defaultValue: String?,
                result: JsPromptResult,
            ): Boolean {
                state.jsDialog = JsDialogRequest(JsDialogRequest.Kind.Prompt, message.orEmpty(), defaultValue) { ok, text ->
                    if (ok) result.confirm(text.orEmpty()) else result.cancel()
                }
                return true
            }

            // ---- target=_blank / window.open -> a new tab -----------------
            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message,
            ): Boolean {
                val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
                val catcher = WebView(view.context)
                catcher.webViewClient = object : WebViewClient() {
                    private var handled = false

                    private fun take(url: String?) {
                        if (handled || url.isNullOrBlank() || url == "about:blank") return
                        handled = true
                        state.openInNewTab(url)
                        catcher.post { catcher.destroy() }
                    }

                    override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean {
                        take(request.url.toString())
                        return true
                    }

                    override fun onPageStarted(v: WebView, url: String?, favicon: Bitmap?) {
                        take(url)
                    }
                }
                transport.webView = catcher
                resultMsg.sendToTarget()
                return true
            }
        }

        startTab?.url?.takeIf { it.isNotBlank() }?.let { loadUrl(it) }
    }
}
