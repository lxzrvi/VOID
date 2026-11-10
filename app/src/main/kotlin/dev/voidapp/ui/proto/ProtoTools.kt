package dev.voidapp.ui.proto

import dev.voidapp.ui.VoidAppState

/** One tool of the hamburger box. [icon] is a [ProtoIcons] id. */
class ProtoTool(val id: String, val name: String, val icon: String)

/** Tools shown per page of the hamburger box: 3 columns x 4 rows. */
const val TOOLS_PER_PAGE = 12

/**
 * Every tool the hamburger can hold (48). The order here is the default order:
 * four pages of twelve - moving around, tabs & library, view, app. Settings lets the
 * user reorder, remove and add any of them.
 */
val ProtoToolCatalog: List<ProtoTool> = listOf(
    // ---- page 1: moving around this page ---------------------------------------------
    ProtoTool("back", "Back", "back"),
    ProtoTool("forward", "Forward", "forward"),
    ProtoTool("reload", "Reload", "reload"),
    ProtoTool("stop", "Stop", "stop"),
    ProtoTool("home", "Home", "home"),
    ProtoTool("search", "Search", "search"),
    ProtoTool("pastego", "Paste & go", "pastego"),
    ProtoTool("top", "To top", "top"),
    ProtoTool("bottom", "To bottom", "bottom"),
    ProtoTool("find", "Find", "find"),
    ProtoTool("share", "Share", "share"),
    ProtoTool("copylink", "Copy link", "copylink"),
    // ---- page 2: tabs and library ----------------------------------------------------------
    ProtoTool("newtab", "New tab", "plus"),
    ProtoTool("closetab", "Close tab", "closetab"),
    ProtoTool("tabs", "Tabs", "tabs"),
    ProtoTool("duplicate", "Duplicate", "duplicate"),
    ProtoTool("reopen", "Reopen", "reopen"),
    ProtoTool("closeothers", "Others", "closeothers"),
    ProtoTool("bookmarks", "Bookmarks", "bookmarks"),
    ProtoTool("addbookmark", "Bookmark", "addbookmark"),
    ProtoTool("history", "History", "history"),
    ProtoTool("downloads", "Downloads", "downloads"),
    ProtoTool("external", "Open in app", "external"),
    ProtoTool("addshortcut", "Add to home", "addshortcut"),
    // ---- page 3: how the page looks ----------------------------------------------------------
    ProtoTool("zoomin", "Zoom in", "zoomin"),
    ProtoTool("zoomout", "Zoom out", "zoomout"),
    ProtoTool("zoomreset", "Reset zoom", "zoomreset"),
    ProtoTool("fullscreen", "Fullscreen", "fullscreen"),
    ProtoTool("desktop", "Desktop", "desktop"),
    ProtoTool("reader", "Reader", "reader"),
    ProtoTool("darkpages", "Dark pages", "darkpages"),
    ProtoTool("textonly", "Text only", "textonly"),
    ProtoTool("keepawake", "Keep awake", "keepawake"),
    ProtoTool("translate", "Translate", "translate"),
    ProtoTool("print", "Print", "print"),
    ProtoTool("screenshot", "Screenshot", "screenshot"),
    // ---- page 4: the app and its data ----------------------------------------------------------
    ProtoTool("privacy", "Block ads", "privacy"),
    ProtoTool("theme", "Theme", "theme"),
    ProtoTool("notes", "Notes", "notes"),
    ProtoTool("saveoffline", "Save page", "saveoffline"),
    ProtoTool("clearcookies", "Cookies", "clearcookies"),
    ProtoTool("clearhistory", "Clr history", "clearhistory"),
    ProtoTool("cleardata", "Clear all", "cleardata"),
    ProtoTool("engine", "Engine", "engine"),
    ProtoTool("edithome", "Edit home", "edithome"),
    ProtoTool("appearance", "Look", "appearance"),
    ProtoTool("settings", "Settings", "settings"),
    ProtoTool("exit", "Exit", "exit"),
)

val ProtoToolById: Map<String, ProtoTool> = ProtoToolCatalog.associateBy { it.id }

val DefaultMenuTools: List<String> = ProtoToolCatalog.map { it.id }

/** True for tools that are switches and are currently on (drawn in the accent colour). */
fun protoToolOn(state: VoidAppState, id: String): Boolean = when (id) {
    "desktop" -> state.desktopMode
    "darkpages" -> state.webDark
    "privacy" -> state.blockAds
    "textonly" -> !state.ui.loadImages
    "keepawake" -> state.ui.keepAwake
    "fullscreen" -> state.fullscreen
    "theme" -> state.darkTheme
    else -> false
}

/** What every tool does, against the real WebView of the current tab. */
fun runProtoTool(state: VoidAppState, id: String) {
    fun needsPage(block: () -> Unit) {
        if (state.browserOpen) block() else state.showToast("Open a page first")
    }
    when (id) {
        "back" -> state.browserBack()
        "forward" -> state.browserForward()
        "reload" -> state.browserReload()
        "stop" -> state.stopLoading()
        "home" -> {
            state.goHome()
            state.closeMenu()
        }
        "search" -> state.requestSearchFocus()
        "pastego" -> state.pasteAndGo()
        "top" -> needsPage { state.scrollToTop() }
        "bottom" -> needsPage { state.scrollToBottom() }
        "find" -> {
            state.closeMenu()
            state.findInPage()
        }
        "share" -> needsPage { state.shareCurrent() }
        "copylink" -> needsPage { state.copyCurrent() }

        "newtab" -> state.startNewTab()
        "closetab" -> state.closeActiveTab()
        "tabs" -> {
            state.closeMenu()
            state.openTabGrid()
        }
        "duplicate" -> needsPage { state.duplicateTab(state.activeTabId) }
        "reopen" -> state.reopenClosedTab()
        "closeothers" -> state.closeOtherTabs(state.activeTabId)
        "bookmarks" -> {
            state.closeMenu()
            state.openBookmarks()
        }
        "addbookmark" -> needsPage { state.bookmarkCurrent() }
        "history" -> {
            state.closeMenu()
            state.openHistory()
        }
        "downloads" -> state.openDownloads()
        "external" -> needsPage { state.openCurrentExternally() }
        "addshortcut" -> needsPage { state.makeShortcut(state.currentUrl) }

        "zoomin" -> needsPage { state.zoomBy(0.1f) }
        "zoomout" -> needsPage { state.zoomBy(-0.1f) }
        "zoomreset" -> needsPage { state.resetZoom() }
        "fullscreen" -> state.toggleFullscreen()
        "desktop" -> state.toggleDesktopMode()
        "reader" -> state.toggleReader()
        "darkpages" -> {
            state.updateWebDark(!state.webDark)
            state.showToast(if (state.webDark) "Dark pages on" else "Dark pages off")
        }
        "textonly" -> state.toggleTextOnly()
        "keepawake" -> state.toggleKeepAwake()
        "translate" -> state.translatePage()
        "print" -> state.saveCurrent()
        "screenshot" -> state.screenshot()

        "privacy" -> {
            state.updateBlockAds(!state.blockAds)
            state.showToast(if (state.blockAds) "Ads & trackers blocked" else "Blocking off")
        }
        "theme" -> state.toggleTheme()
        "notes" -> {
            state.closeMenu()
            state.openNotes()
        }
        "saveoffline" -> state.saveOffline()
        "clearcookies" -> state.clearCookies()
        "clearhistory" -> {
            state.clearHistoryOnly()
            state.showToast("History cleared")
        }
        "cleardata" -> state.clearBrowsingData()
        "engine" -> state.switchEngine()
        "edithome" -> state.openSettingsPage("home")
        "appearance" -> state.openSettingsPage("appearance")
        "settings" -> {
            state.closeMenu()
            state.openSettings()
        }
        "exit" -> state.requestAppExit()
    }
}

/** What a tab is called in the tabs box and in hints. */
fun protoTabTitle(url: String, title: String): String = when {
    url.isBlank() -> "Homepage"
    title.isNotBlank() -> title
    else -> url.substringAfter("://", url).substringBefore("/").removePrefix("www.")
}
