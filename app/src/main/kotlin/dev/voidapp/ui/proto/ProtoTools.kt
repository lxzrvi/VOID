package dev.voidapp.ui.proto

import dev.voidapp.ui.VoidAppState

/** One entry of the paged tools box. [icon] is a [ProtoIcons] id. */
class ProtoTool(val name: String, val icon: String, val action: String)

/** The prototype's two tool pages: 3 columns x 4 rows each. */
val ProtoToolPages: List<List<ProtoTool>> = listOf(
    listOf(
        ProtoTool("Back", "back", "back"),
        ProtoTool("Forward", "forward", "forward"),
        ProtoTool("Reload", "reload", "reload"),
        ProtoTool("Home", "home", "home"),
        ProtoTool("Search", "search", "search"),
        ProtoTool("New tab", "plus", "newtab"),
        ProtoTool("Close tab", "closetab", "closetab"),
        ProtoTool("Tabs overview", "tabs", "tabs"),
        ProtoTool("History", "history", "history"),
        ProtoTool("Bookmarks", "bookmarks", "bookmarks"),
        ProtoTool("Downloads", "downloads", "downloads"),
        ProtoTool("Share", "share", "share"),
    ),
    listOf(
        ProtoTool("Find page", "find", "find"),
        ProtoTool("Desktop site", "desktop", "desktop"),
        ProtoTool("Translate", "translate", "translate"),
        ProtoTool("Settings", "settings", "settings"),
        ProtoTool("Privacy", "privacy", "privacy"),
        ProtoTool("Notes", "reader", "notes"),
        ProtoTool("Print", "print", "print"),
        ProtoTool("Add bookmark", "addbookmark", "addbookmark"),
        ProtoTool("Clear data", "cleardata", "cleardata"),
        ProtoTool("Fullscreen", "fullscreen", "fullscreen"),
        ProtoTool("Zoom in", "zoomin", "zoomin"),
        ProtoTool("Zoom out", "zoomout", "zoomout"),
    ),
)

/** What every tool does, against the real WebView of the current tab. */
fun runProtoTool(state: VoidAppState, action: String) {
    when (action) {
        "back" -> state.browserBack()
        "forward" -> state.browserForward()
        "reload" -> state.browserReload()
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
        "history" -> {
            state.closeMenu()
            state.openHistory()
        }
        "bookmarks" -> {
            state.closeMenu()
            state.openBookmarks()
        }
        "downloads" -> state.openDownloads()
        "share" -> if (state.browserOpen) state.shareCurrent() else state.showToast("Open a page first")
        "find" -> {
            state.closeMenu()
            state.findInPage()
        }
        "desktop" -> state.toggleDesktopMode()
        "translate" -> state.translatePage()
        "settings" -> {
            state.closeMenu()
            state.openSettings()
        }
        "privacy" -> {
            state.updateBlockAds(!state.blockAds)
            state.showToast(if (state.blockAds) "Ads & trackers blocked" else "Blocking off")
        }
        "notes" -> {
            state.closeMenu()
            state.openNotes()
        }
        "print" -> state.saveCurrent()
        "addbookmark" -> state.bookmarkCurrent()
        "cleardata" -> state.clearBrowsingData()
        "fullscreen" -> state.toggleFullscreen()
        "zoomin" -> state.zoomBy(0.1f)
        "zoomout" -> state.zoomBy(-0.1f)
    }
}

/** What a tab is called in the tab row and the tabs box. */
fun protoTabTitle(url: String, title: String): String = when {
    url.isBlank() -> "Homepage"
    title.isNotBlank() -> title
    else -> url.substringAfter("://", url).substringBefore("/").removePrefix("www.")
}
