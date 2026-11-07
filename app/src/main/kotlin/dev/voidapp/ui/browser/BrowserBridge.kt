package dev.voidapp.ui.browser

/**
 * What the island's tools can do to the running WebView. BrowserScreen installs an
 * implementation while a page is open; the island never touches WebView directly.
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
    fun zoomBy(delta: Float)
    fun findInPage()
    fun savePage()
}
