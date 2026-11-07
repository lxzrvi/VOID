package dev.voidapp.ui

import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import dev.voidapp.data.Background
import dev.voidapp.data.BoxEdit
import dev.voidapp.data.DayEvent
import dev.voidapp.data.Note
import dev.voidapp.data.OnThisDay
import dev.voidapp.data.PageEntry
import dev.voidapp.data.SavedTab
import dev.voidapp.data.SearchProvider
import dev.voidapp.data.Site
import dev.voidapp.data.VoidStore
import dev.voidapp.data.urlForInput
import dev.voidapp.ui.browser.BrowserBridge
import dev.voidapp.ui.browser.BrowserHost
import dev.voidapp.ui.components.FaviconStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/** Which panel is open above the island bar. */
enum class IslandPanel { None, Tools, Navigator, Settings, Notes, TabGrid }

/** What the island's tools do. BrowserScreen implements the page part. */
interface IslandActions {
    fun browserBack()
    fun browserForward()
    fun browserReload()
    fun findInPage()
    fun openHistory()
    fun openBookmarks()
    fun openSettings()
    fun openNotes()
    fun openTabs()
    fun startNewTab()
    fun toggleDesktopMode()
    fun zoomBy(delta: Float)
    fun saveCurrent()
    fun copyCurrent()
    fun shareCurrent()
    fun openCurrentExternally()
    fun bookmarkCurrent()
    fun scrollToTop()
    fun translatePage()
    fun openDownloads()
}

/** A browser tab: URL plus the title the WebView reported. */
data class BrowserTab(
    val id: Long,
    val url: String,
    val title: String = "",
    val desktop: Boolean = false,
)

/** Live, UI-facing state of one tab's WebView. */
@Stable
class TabRuntime {
    var progress by mutableFloatStateOf(1f)
    var error by mutableStateOf<String?>(null)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var pageColor by mutableStateOf<Color?>(null)
    var findCount by mutableIntStateOf(0)
    var findIndex by mutableIntStateOf(0)

    /** Bumped when the WebView had to be rebuilt (renderer crash). */
    var generation by mutableIntStateOf(0)
}

/** A JavaScript alert / confirm / prompt waiting for the user. */
class JsDialogRequest(
    val kind: Kind,
    val message: String,
    val defaultValue: String?,
    val onResult: (confirmed: Boolean, text: String?) -> Unit,
) {
    enum class Kind { Alert, Confirm, Prompt }
}

/** Key of the Home card in [VoidAppState.cardRects]. */
const val HOME_CARD_KEY = -1L

/**
 * Single source of truth. Everything the first frame needs (sites, notes,
 * background, box edits, tool order, open tabs) is read from [VoidStore]
 * synchronously when this object is created.
 */
@Stable
class VoidAppState(
    private val context: Context,
    private val store: VoidStore,
    private val scope: CoroutineScope,
) : IslandActions {

    private var activity: Activity? = null

    fun attachActivity(value: Context) {
        activity = value as? Activity
    }

    /** Installed by BrowserScreen while a WebView is on screen. */
    var bridge: BrowserBridge? by mutableStateOf(null)

    /** Activity-bound things a WebView needs (file chooser, permissions...). */
    var host: BrowserHost? = null

    /** Height of the status bar in px, kept for the injected page inset. */
    var statusInsetPx: Int = 0

    /** Last scroll movement of the page / home; the island slides away on it. */
    var scrollDelta by mutableFloatStateOf(0f)
        private set

    fun reportScroll(delta: Float) {
        scrollDelta = delta
    }

    // ---- island -----------------------------------------------------------
    var islandOffset by mutableFloatStateOf(store.islandOffset)
        private set
    var islandOnRight by mutableStateOf(store.islandOnRight)
        private set

    fun placeIsland(offset: Float, onRight: Boolean) {
        islandOffset = offset.coerceIn(0f, 1f)
        islandOnRight = onRight
        store.islandOffset = islandOffset
        store.islandOnRight = onRight
    }

    // ---- search -----------------------------------------------------------
    var provider by mutableStateOf(store.provider)
        private set

    fun selectProvider(value: SearchProvider) {
        provider = value
        store.provider = value
    }

    fun resolve(input: String): String = urlForInput(input, provider)

    private val suggestionIndex = listOf(
        "Google", "YouTube", "GitHub", "Reddit", "X", "Instagram", "Wikipedia",
        "Gmail", "Drive", "Notion", "Stack Overflow", "ChatGPT", "Netflix", "Amazon",
    )

    var suggestionsEnabled by mutableStateOf(store.suggestions)
        private set
    private val remoteSuggestions = mutableStateListOf<String>()
    private var suggestJob: Job? = null

    fun updateSuggestionsEnabled(value: Boolean) {
        suggestionsEnabled = value
        store.suggestions = value
        if (!value) remoteSuggestions.clear()
    }

    /** Live suggestions from the network, debounced; local matches show instantly. */
    fun updateSuggestions(query: String) {
        suggestJob?.cancel()
        val text = query.trim()
        if (text.isBlank() || !suggestionsEnabled || text.startsWith("http")) {
            remoteSuggestions.clear()
            return
        }
        suggestJob = scope.launch {
            delay(180)
            val list = withContext(Dispatchers.IO) { fetchSuggestions(text) }
            remoteSuggestions.clear()
            remoteSuggestions.addAll(list)
        }
    }

    private fun fetchSuggestions(query: String): List<String> = runCatching {
        val url = URL("https://suggestqueries.google.com/complete/search?client=firefox&q=" + Uri.encode(query))
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
        }
        try {
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(text).getJSONArray(1)
            (0 until array.length()).map { array.getString(it) }
        } finally {
            connection.disconnect()
        }
    }.getOrDefault(emptyList())

    fun results(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val matching = suggestionIndex.filter { it.contains(query, ignoreCase = true) }
        return (listOf("${provider.label}: $query") + matching + remoteSuggestions).distinct().take(9)
    }

    fun resultCount(query: String): Int = results(query).size

    fun openResult(query: String, position: Int) {
        val pick = results(query).getOrNull(position) ?: return
        if (pick.startsWith("${provider.label}: ")) {
            search(pick.removePrefix("${provider.label}: "))
            return
        }
        val site = sites.firstOrNull { it.name == pick }
        if (site != null) open(site.url) else search(pick)
    }

    // ---- shortcuts (order + hidden state persist) --------------------------
    val sites = mutableStateListOf<Site>().apply {
        val saved = store.cachedSites
        addAll(if (saved.isEmpty()) Site.Bundled else saved)
    }

    val visibleSites: List<Site> get() = sites.filterNot { it.hidden }

    fun persistSites() = store.writeSites(sites.toList())

    fun addSite(name: String, url: String) {
        val clean = url.trim().let { if (it.startsWith("http")) it else "https://$it" }
        val label = name.trim().ifBlank { clean.substringAfter("://").substringBefore("/") }
        val site = Site(name = label, url = clean, custom = true)
        sites.add(site)
        persistSites()
        scope.launch { FaviconStore.fetch(site.faviconUrl) }
    }

    fun removeSite(site: Site) {
        sites.remove(site)
        persistSites()
    }

    /** Moves a visible shortcut; indices are positions in [visibleSites]. */
    fun moveSite(from: Int, to: Int) {
        val visible = visibleSites
        if (from !in visible.indices || to !in visible.indices || from == to) return
        val fromSite = visible[from]
        val toSite = visible[to]
        val a = sites.indexOf(fromSite)
        val b = sites.indexOf(toSite)
        if (a < 0 || b < 0) return
        val item = sites.removeAt(a)
        sites.add(b, item)
        persistSites()
    }

    // ---- notes ------------------------------------------------------------
    val notes = mutableStateListOf<Note>().apply { addAll(store.cachedNotes) }

    fun addNote(text: String) {
        if (text.isBlank()) return
        notes.add(0, Note(id = System.currentTimeMillis(), text = text.trim()))
        store.writeNotes(notes.toList())
    }

    fun removeNote(note: Note) {
        notes.remove(note)
        store.writeNotes(notes.toList())
    }

    // ---- appearance -------------------------------------------------------
    var background by mutableStateOf(store.cachedBackground)
        private set
    val boxEdits = mutableStateMapOf<String, BoxEdit>().apply { putAll(store.cachedBoxEdits) }

    fun updateBackground(value: Background) {
        background = value
        store.writeBackground(value)
    }

    fun boxEdit(key: String): BoxEdit = boxEdits[key] ?: BoxEdit()

    fun setBoxEdit(key: String, value: BoxEdit) {
        boxEdits[key] = value
        store.writeBoxEdits(boxEdits.toMap())
    }

    /** Which box the long-press editor is open for (null = closed). */
    var editingBox by mutableStateOf<String?>(null)

    /** Where a background-only long press happened (screen fraction). */
    var backgroundPickerAt by mutableStateOf<Pair<Float, Float>?>(null)

    // ---- tools order ------------------------------------------------------
    val toolOrder = mutableStateListOf<String>().apply {
        val saved = store.cachedTools.filter { it in DEFAULT_TOOLS }.distinct()
        // Tools added in newer versions are appended so they never go missing.
        addAll(saved + DEFAULT_TOOLS.filterNot { it in saved })
    }

    fun moveTool(from: Int, to: Int) {
        if (from !in toolOrder.indices || to !in toolOrder.indices || from == to) return
        val item = toolOrder.removeAt(from)
        toolOrder.add(to, item)
        store.writeStringList("tool_order", toolOrder.toList())
    }

    // ---- tabs -------------------------------------------------------------
    val tabs = mutableStateListOf<BrowserTab>()
    var activeTabId by mutableStateOf(0L)
    private var nextTabId = 1L

    init {
        val saved = store.readTabs()
        saved.forEach { tabs.add(BrowserTab(nextTabId++, it.url, it.title, it.desktop)) }
        activeTabId = tabs.getOrNull(store.activeTabIndex)?.id ?: tabs.lastOrNull()?.id ?: 0L
    }

    val activeTab: BrowserTab? get() = tabs.firstOrNull { it.id == activeTabId }

    private fun persistTabs() {
        store.writeTabs(tabs.map { SavedTab(it.url, it.title, it.desktop) })
        store.activeTabIndex = tabs.indexOfFirst { it.id == activeTabId }.coerceAtLeast(0)
    }

    // One WebView per tab: switching tabs keeps scroll position, history and forms.
    private val webViews = HashMap<Long, WebView>()
    private val runtimes = HashMap<Long, TabRuntime>()

    fun runtimeFor(id: Long): TabRuntime = runtimes.getOrPut(id) { TabRuntime() }

    val activeRuntime: TabRuntime? get() = activeTab?.let { runtimeFor(it.id) }

    fun webViewFor(id: Long, create: () -> WebView): WebView = webViews.getOrPut(id, create)

    fun destroyWebView(id: Long) {
        val view = webViews.remove(id) ?: return
        runCatching {
            view.stopLoading()
            (view.parent as? ViewGroup)?.removeView(view)
            view.destroy()
        }
    }

    fun recreateWebView(id: Long) {
        destroyWebView(id)
        runtimeFor(id).generation++
    }

    fun forEachWebView(block: (WebView) -> Unit) {
        webViews.values.forEach { runCatching { block(it) } }
    }

    fun applyInsetToPages(cssPx: Float) {
        webViews.values.forEach {
            it.evaluateJavascript("window.__voidSetInset&&window.__voidSetInset($cssPx)", null)
        }
    }

    private fun updateTab(id: Long, url: String?, title: String?) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        val old = tabs[index]
        val next = old.copy(url = url ?: old.url, title = title ?: old.title)
        if (next != old) {
            tabs[index] = next
            persistTabs()
        }
    }

    fun onTabNavigated(id: Long, url: String, title: String?) {
        updateTab(id, url, title)
        if (id == activeTabId) address = url
    }

    fun onTabTitle(id: Long, title: String) = updateTab(id, null, title)

    private fun newTab(url: String) {
        if (browserOpen) captureActiveThumb()
        val tab = BrowserTab(id = nextTabId++, url = url, desktop = false)
        tabs.add(tab)
        activeTabId = tab.id
        address = url
        browserOpen = true
        persistTabs()
    }

    fun closeTab(id: Long) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        tabs.removeAt(index)
        destroyWebView(id)
        runtimes.remove(id)
        tabThumbs.remove(id)
        cardRects.remove(id)
        if (activeTabId == id) {
            val next = tabs.getOrNull(index) ?: tabs.lastOrNull()
            if (next != null) {
                activeTabId = next.id
                address = next.url
            } else {
                activeTabId = 0L
                browserOpen = false
            }
        }
        if (tabs.isEmpty()) browserOpen = false
        persistTabs()
    }

    fun moveTab(from: Int, to: Int) {
        if (from !in tabs.indices || to !in tabs.indices || from == to) return
        val item = tabs.removeAt(from)
        tabs.add(to, item)
        persistTabs()
    }

    fun closeActiveTab() {
        closeTab(activeTabId)
        if (tabs.isEmpty()) closeTabGrid()
    }

    // ---- tab grid: the page shrinks into its card -------------------------
    /** 0 = a page fills the screen, 1 = the grid is fully open. */
    var tabProgress by mutableFloatStateOf(0f)
        private set
    var tabGridOpen by mutableStateOf(false)
        private set
    private var tabJob: Job? = null

    val tabThumbs = mutableStateMapOf<Long, ImageBitmap>()

    /** Window rect of every card's preview area (the live page lands on the active one). */
    val cardRects = mutableStateMapOf<Long, Rect>()

    val activeCardRect: Rect?
        get() = cardRects[if (browserOpen && activeTab != null) activeTabId else HOME_CARD_KEY]

    fun captureActiveThumb() {
        if (!browserOpen) return
        val id = activeTabId
        val bitmap = bridge?.snapshot(420) ?: return
        tabThumbs[id] = bitmap.asImageBitmap()
    }

    /** Finger started pulling the circle up. */
    fun beginTabDrag() {
        tabJob?.cancel()
        captureActiveThumb()
        hidePanel()
        if (tabProgress < 0.001f) tabProgress = 0.001f
    }

    fun dragTabTo(progress: Float) {
        tabProgress = progress.coerceIn(0.001f, 1f)
    }

    fun endTabDrag(open: Boolean) {
        if (open) openTabGrid() else closeTabGrid()
    }

    fun openTabGrid() {
        if (!tabGridOpen && tabProgress < 0.01f) captureActiveThumb()
        hidePanel()
        tabGridOpen = true
        animateTabTo(1f)
    }

    fun closeTabGrid() {
        tabGridOpen = false
        animateTabTo(0f)
    }

    private fun animateTabTo(target: Float) {
        tabJob?.cancel()
        tabJob = scope.launch {
            animate(
                initialValue = tabProgress,
                targetValue = target,
                animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow),
            ) { value, _ -> tabProgress = value }
            tabProgress = target
        }
    }

    fun selectTab(id: Long) {
        if (tabs.none { it.id == id }) return
        if (browserOpen && id != activeTabId) captureActiveThumb()
        activeTabId = id
        address = tabs.firstOrNull { it.id == id }?.url.orEmpty()
        browserOpen = true
        persistTabs()
        closeTabGrid()
    }

    fun selectHome() {
        browserOpen = false
        closeTabGrid()
    }

    // ---- navigation -------------------------------------------------------
    var browserOpen by mutableStateOf(false)
        private set
    var address by mutableStateOf("")

    var toast by mutableStateOf<String?>(null)
        private set

    fun showToast(message: String) {
        toast = message
        scope.launch {
            delay(1600)
            if (toast == message) toast = null
        }
    }

    /** From Home a page always opens in a NEW tab; inside a page it loads in that tab. */
    fun open(url: String) {
        if (url.isBlank()) return
        if (!browserOpen || activeTab == null) {
            newTab(url)
            return
        }
        val index = tabs.indexOfFirst { it.id == activeTabId }
        if (index >= 0) tabs[index] = tabs[index].copy(url = url)
        address = url
        webViews[activeTabId]?.loadUrl(url)
        persistTabs()
    }

    fun openInNewTab(url: String) {
        if (url.isBlank()) return
        newTab(url)
    }

    fun search(query: String) {
        if (query.isBlank()) return
        open(resolve(query))
    }

    fun goHome() {
        if (browserOpen) captureActiveThumb()
        browserOpen = false
        closeTabGrid()
        findOpen = false
    }

    override fun startNewTab() = goHome()

    // ---- look & feel ------------------------------------------------------
    var darkTheme by mutableStateOf(store.darkTheme)
        private set

    val desktopMode: Boolean get() = activeTab?.desktop ?: false

    var blockAds by mutableStateOf(store.blockAds)
        private set
    var webDark by mutableStateOf(store.webDark)
        private set

    fun updateDarkTheme(value: Boolean) {
        darkTheme = value
        store.darkTheme = value
    }

    fun toggleTheme() = updateDarkTheme(!darkTheme)

    fun updateBlockAds(value: Boolean) {
        blockAds = value
        store.blockAds = value
    }

    fun updateWebDark(value: Boolean) {
        webDark = value
        store.webDark = value
        bridge?.applyWebDark(value)
    }

    fun updateDesktopMode(value: Boolean) {
        val index = tabs.indexOfFirst { it.id == activeTabId }
        if (index < 0) return
        tabs[index] = tabs[index].copy(desktop = value)
        persistTabs()
        bridge?.setDesktopMode(value)
    }

    fun clearNotes() {
        notes.clear()
        store.writeNotes(emptyList())
    }

    fun requestAppExit() {
        activity?.finish()
    }

    // ---- history / bookmarks ---------------------------------------------
    val history = mutableStateListOf<PageEntry>().apply { addAll(store.readEntries("history")) }
    val bookmarks = mutableStateListOf<PageEntry>().apply { addAll(store.readEntries("bookmarks")) }

    var libraryOpen by mutableStateOf(false)
    var libraryTab by mutableIntStateOf(0)

    fun recordVisit(tabId: Long, url: String, title: String) {
        if (url.isBlank() || url.startsWith("about:")) return
        val entry = PageEntry(url, title.ifBlank { url.substringAfter("://").substringBefore("/") })
        history.removeAll { it.url == url }
        history.add(0, entry)
        while (history.size > 500) history.removeAt(history.lastIndex)
        store.writeEntries("history", history.toList())
        onTabNavigated(tabId, url, entry.title)
    }

    fun toggleBookmark(url: String, title: String) {
        if (url.isBlank()) return
        val existing = bookmarks.firstOrNull { it.url == url }
        if (existing != null) bookmarks.remove(existing) else bookmarks.add(0, PageEntry(url, title))
        store.writeEntries("bookmarks", bookmarks.toList())
    }

    fun isBookmarked(url: String): Boolean = bookmarks.any { it.url == url }

    fun removeHistory(entry: PageEntry) {
        history.remove(entry)
        store.writeEntries("history", history.toList())
    }

    fun removeBookmark(entry: PageEntry) {
        bookmarks.remove(entry)
        store.writeEntries("bookmarks", bookmarks.toList())
    }

    fun clearHistoryOnly() {
        history.clear()
        store.writeEntries("history", emptyList())
    }

    fun clearBrowsingData() {
        history.clear()
        bookmarks.clear()
        store.clearBrowsingData()
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.WebStorage.getInstance().deleteAllData()
        webViews.values.forEach { it.clearCache(true) }
        showToast("Cleared")
    }

    // ---- today's event ----------------------------------------------------
    var event by mutableStateOf<DayEvent?>(null)
    var eventLoading by mutableStateOf(false)

    fun reloadEvent() {
        if (eventLoading) return
        eventLoading = true
        scope.launch {
            event = OnThisDay.fetch().firstOrNull() ?: event
            eventLoading = false
        }
    }

    // ---- page level UI: find bar, dialogs, fullscreen video ----------------
    var findOpen by mutableStateOf(false)
    var jsDialog by mutableStateOf<JsDialogRequest?>(null)
    var customView by mutableStateOf<View?>(null)
    var customViewCallback: WebChromeClient.CustomViewCallback? = null

    fun exitCustomView() {
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
    }

    fun startDownload(url: String, userAgent: String?, disposition: String?, mime: String?) {
        if (!url.startsWith("http")) {
            showToast("This download type is not supported")
            return
        }
        runCatching {
            val name = android.webkit.URLUtil.guessFileName(url, disposition, mime)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                mime?.let { setMimeType(it) }
                android.webkit.CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
                userAgent?.let { addRequestHeader("User-Agent", it) }
                setTitle(name)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, name)
            }
            (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            showToast("Downloading $name")
        }.onFailure { showToast("Download failed") }
    }

    // ---- island actions ---------------------------------------------------
    override fun browserBack() {
        val b = bridge
        if (browserOpen && b != null && b.canGoBack) b.back() else goHome()
    }

    override fun browserForward() {
        bridge?.takeIf { browserOpen }?.forward()
    }

    override fun browserReload() {
        bridge?.takeIf { browserOpen }?.reload()
    }

    override fun openHistory() {
        libraryTab = 0
        libraryOpen = true
        hidePanel()
    }

    override fun openBookmarks() {
        libraryTab = 1
        libraryOpen = true
        hidePanel()
    }

    override fun openSettings() = showPanel(IslandPanel.Settings)

    override fun openNotes() = showPanel(IslandPanel.Notes)

    override fun openTabs() = openTabGrid()

    override fun findInPage() {
        if (browserOpen) {
            findOpen = true
            hidePanel()
        } else {
            showToast("Open a page first")
        }
    }

    override fun toggleDesktopMode() {
        if (!browserOpen) {
            showToast("Open a page first")
            return
        }
        updateDesktopMode(!desktopMode)
        showToast(if (desktopMode) "Desktop site" else "Mobile site")
    }

    override fun zoomBy(delta: Float) {
        bridge?.zoomBy(delta)
    }

    override fun saveCurrent() {
        if (browserOpen) {
            bridge?.printPage()
        } else {
            showToast("Open a page first")
        }
    }

    override fun copyCurrent() = copyLink(currentUrl)

    override fun shareCurrent() = shareLink(currentUrl)

    override fun openCurrentExternally() {
        if (currentUrl.isBlank()) return
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    override fun bookmarkCurrent() {
        if (currentUrl.isBlank()) return
        toggleBookmark(currentUrl, bridge?.currentTitle ?: currentUrl)
        showToast(if (isBookmarked(currentUrl)) "Bookmarked" else "Removed")
    }

    override fun scrollToTop() {
        bridge?.scrollToTop()
    }

    override fun translatePage() {
        if (!browserOpen || currentUrl.isBlank()) {
            showToast("Open a page first")
            return
        }
        open("https://translate.google.com/translate?sl=auto&tl=en&u=" + Uri.encode(currentUrl))
    }

    override fun openDownloads() {
        runCatching {
            context.startActivity(
                Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    // ---- link helpers (bar long-press menu) -------------------------------
    val currentUrl: String
        get() = if (browserOpen) (bridge?.currentUrl ?: address) else ""

    fun copyLink(url: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("VOID", url))
        showToast("Copied")
    }

    fun shareLink(url: String) {
        if (url.isBlank()) return
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                },
                null,
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Adds whatever page is open to the home shortcuts. */
    fun makeShortcut(url: String) {
        if (url.isBlank()) return
        val host = url.substringAfter("://").substringBefore("/").removePrefix("www.")
        addSite(host.substringBefore('.').replaceFirstChar { it.uppercase() }, url)
        showToast("Shortcut added")
    }

    // ---- panels -----------------------------------------------------------
    var islandPanel by mutableStateOf(IslandPanel.None)
    var islandList by mutableStateOf<List<String>>(emptyList())
        private set

    fun showPanel(panel: IslandPanel, items: List<String> = emptyList()) {
        islandList = items
        islandPanel = panel
    }

    fun hidePanel() {
        islandPanel = IslandPanel.None
        islandList = emptyList()
    }

    companion object {
        /** Tools in their default order: the usual suspects first. */
        val DEFAULT_TOOLS = listOf(
            "back", "forward", "reload", "home", "tabs", "newtab",
            "bookmark", "bookmarks", "history", "downloads", "share", "copy",
            "find", "desktop", "translate", "zoom_in", "zoom_out", "top",
            "external", "save", "notes", "clear", "theme", "settings",
        )
    }
}

@Composable
fun rememberVoidAppState(): VoidAppState {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    return remember {
        val app = context.applicationContext
        FaviconStore.init(app)
        VoidAppState(
            context = app,
            store = VoidStore(app),
            scope = scope,
        ).also { state ->
            state.attachActivity(context)
            // Decode every known icon BEFORE the first frame: shortcuts and the
            // search providers are drawn complete, nothing pops in.
            FaviconStore.warm(
                state.sites.map { it.faviconUrl } +
                    dev.voidapp.data.SearchProvider.entries.map { it.faviconUrl },
            )
        }
    }
}
