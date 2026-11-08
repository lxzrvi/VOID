package dev.voidapp.ui.proto

import dev.voidapp.ui.VoidAppState

/** One entry of the paged tools box. [icon] is a [ProtoIcons] id. */
class ProtoTool(val name: String, val icon: String, val action: String)

/** Three tool pages: 3 columns x 4 rows each. */
val ProtoToolPages: List<List<ProtoTool>> = listOf(
    // ---- page 1: moving around and tabs ---------------------------------------
    listOf(
        ProtoTool("Back", "back", "back"),
        ProtoTool("Forward", "forward", "forward"),
        ProtoTool("Reload", "reload", "reload"),
        ProtoTool("Stop", "stop", "stop"),
        ProtoTool("Home", "home", "home"),
        ProtoTool("Search", "search", "search"),
        ProtoTool("New tab", "plus", "newtab"),
        ProtoTool("Close tab", "closetab", "closetab"),
        ProtoTool("Tabs overview", "tabs", "tabs"),
        ProtoTool("Duplicate tab", "duplicate", "duplicate"),
        ProtoTool("Reopen tab", "reopen", "reopen"),
        ProtoTool("Close others", "closeothers", "closeothers"),
    ),
    // ---- page 2: this page -------------------------------------------------------
    listOf(
        ProtoTool("Find page", "find", "find"),
        ProtoTool("Desktop site", "desktop", "desktop"),
        ProtoTool("Translate", "translate", "translate"),
        ProtoTool("Add bookmark", "addbookmark", "addbookmark"),
        ProtoTool("Bookmarks", "bookmarks", "bookmarks"),
        ProtoTool("History", "history", "history"),
        ProtoTool("Downloads", "downloads", "downloads"),
        ProtoTool("Share", "share", "share"),
        ProtoTool("Copy link", "copylink", "copylink"),
        ProtoTool("Open in app", "external", "external"),
        ProtoTool("Print / PDF", "print", "print"),
        ProtoTool("Scroll top", "top", "top"),
    ),
    // ---- page 3: view, privacy and the app ----------------------------------------
    listOf(
        ProtoTool("Scroll bottom", "bottom", "bottom"),
        ProtoTool("Zoom in", "zoomin", "zoomin"),
        ProtoTool("Zoom out", "zoomout", "zoomout"),
        ProtoTool("Reset zoom", "zoomreset", "zoomreset"),
        ProtoTool("Fullscreen", "fullscreen", "fullscreen"),
        ProtoTool("Dark pages", "darkpages", "darkpages"),
        ProtoTool("Block ads", "privacy", "privacy"),
        ProtoTool("Dark theme", "theme", "theme"),
        ProtoTool("Notes", "notes", "notes"),
        ProtoTool("Clear data", "cleardata", "cleardata"),
        ProtoTool("Add to home", "addshortcut", "addshortcut"),
        ProtoTool("Settings", "settings", "settings"),
    ),
)

/** What every tool does, against the real WebView of the current tab. */
fun runProtoTool(state: VoidAppState, action: String) {
    fun needsPage(block: () -> Unit) {
        if (state.browserOpen) block() else state.showToast("Open a page first")
    }
    when (action) {
        "back" -> state.browserBack()
        "forward" -> state.browserForward()
        "reload" -> state.browserReload()
        "stop" -> state.stopLoading()
        "home" -> {
            state.goHome()
            state.closeMenu()
        }
        "search" -> state.requestSearchFocus()
        "newtab" -> state.startNewTab()
        "closetab" -> state.closeActiveTab()
        "tabs" -> {
            state.closeMenu()
            state.openTabGrid()
        }
        "duplicate" -> needsPage { state.duplicateTab(state.activeTabId) }
        "reopen" -> state.reopenClosedTab()
        "closeothers" -> state.closeOtherTabs(state.activeTabId)
        "find" -> {
            state.closeMenu()
            state.findInPage()
        }
        "desktop" -> state.toggleDesktopMode()
        "translate" -> state.translatePage()
        "addbookmark" -> needsPage { state.bookmarkCurrent() }
        "bookmarks" -> {
            state.closeMenu()
            state.openBookmarks()
        }
        "history" -> {
            state.closeMenu()
            state.openHistory()
        }
        "downloads" -> state.openDownloads()
        "share" -> needsPage { state.shareCurrent() }
        "copylink" -> needsPage { state.copyCurrent() }
        "external" -> needsPage { state.openCurrentExternally() }
        "print" -> state.saveCurrent()
        "top" -> needsPage { state.scrollToTop() }
        "bottom" -> needsPage { state.scrollToBottom() }
        "zoomin" -> needsPage { state.zoomBy(0.1f) }
        "zoomout" -> needsPage { state.zoomBy(-0.1f) }
        "zoomreset" -> needsPage { state.resetZoom() }
        "fullscreen" -> state.toggleFullscreen()
        "darkpages" -> {
            state.updateWebDark(!state.webDark)
            state.showToast(if (state.webDark) "Dark pages on" else "Dark pages off")
        }
        "privacy" -> {
            state.updateBlockAds(!state.blockAds)
            state.showToast(if (state.blockAds) "Ads & trackers blocked" else "Blocking off")
        }
        "theme" -> state.toggleTheme()
        "notes" -> {
            state.closeMenu()
            state.openNotes()
        }
        "cleardata" -> state.clearBrowsingData()
        "addshortcut" -> needsPage { state.makeShortcut(state.currentUrl) }
        "settings" -> {
            state.closeMenu()
            state.openSettings()
        }
    }
}

/** What a tab is called in the tab row and the tabs box. */
fun protoTabTitle(url: String, title: String): String = when {
    url.isBlank() -> "Homepage"
    title.isNotBlank() -> title
    else -> url.substringAfter("://", url).substringBefore("/").removePrefix("www.")
}
