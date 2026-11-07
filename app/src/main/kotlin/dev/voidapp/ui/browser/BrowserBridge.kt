package dev.voidapp.ui.browser

/**
 * What the island's tools can do to the running WebView. [BrowserScreen] installs
 * an implementation when it creates the WebView; when no browser is open the
 * hooks are no-ops (or fall back to opening the home page / a URL).
 */
interface BrowserBridge {
    val canGoBack: Boolean
    val canGoForward: Boolean
    val currentUrl: String
    val currentTitle: String

    fun back()
    fun forward()
    fun reload()
    fun setDesktopMode(enabled: Boolean)
    fun zoomBy(delta: Float)
    fun findInPage(text: String)
    fun savePage()
    fun printPage()
}
