package dev.voidapp.ui.browser

import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient

/**
 * What the island's tools can do to the WebView that is on screen. BrowserScreen
 * installs an implementation while a page is open; the island never touches a
 * WebView directly.
 */
interface BrowserBridge {
    val canGoBack: Boolean
    val canGoForward: Boolean
    val currentUrl: String
    val currentTitle: String

    fun back()
    fun forward()
    fun reload()
    fun scrollToTop()
    fun setDesktopMode(enabled: Boolean)
    fun applyWebDark(enabled: Boolean)

    /** Text zoom, in steps of [delta] (0.15 = 15 %). */
    fun zoomBy(delta: Float)

    fun find(query: String)
    fun findNext(forward: Boolean)
    fun clearFind()

    /** Opens the system print sheet, which also offers "Save as PDF". */
    fun printPage()

    /** A small picture of the page as it is right now, for the tab card. */
    fun snapshot(width: Int): Bitmap?
}

/** Things a WebView needs from the Activity: pickers, permissions, fullscreen. */
interface BrowserHost {
    fun chooseFile(
        callback: ValueCallback<Array<Uri>>,
        params: WebChromeClient.FileChooserParams,
    ): Boolean

    fun requestPermissions(permissions: Array<String>, onResult: (granted: Boolean) -> Unit)
}
