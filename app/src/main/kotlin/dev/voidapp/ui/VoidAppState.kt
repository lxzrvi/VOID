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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import dev.voidapp.data.Background
import dev.voidapp.data.BoxEdit
import dev.voidapp.data.DayEvent
import dev.voidapp.data.DownloadCenter
import dev.voidapp.data.DownloadRequest
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
import dev.voidapp.ui.browser.applyWebPrefs
import dev.voidapp.ui.proto.protoTabTitle
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

    /** Colour at the very top of the page (the status bar band wears it). */
    var topColor by mutableStateOf<Color?>(null)
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

    /** Every customisable look/behaviour value (persisted as it changes). */
    val ui = UiPrefs(context)

    /** The built-in download manager. */
    val downloads = DownloadCenter(context, scope) { showToast(it) }

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

    // ---- home boxes: how many, and how they look --------------------------------
    var homeCount by mutableIntStateOf(store.homeCount.coerceIn(4, 12))
        private set
    var boxColor by mutableStateOf(store.boxColor.takeIf { it != 0 }?.let { Color(it) })
        private set
    var boxTextColor by mutableStateOf(store.boxTextColor.takeIf { it != 0 }?.let { Color(it) })
        private set
    var boxAlpha by mutableFloatStateOf(store.boxAlpha.coerceIn(0f, 1f))
        private set
    var boxRadius by mutableFloatStateOf(store.boxRadius.coerceIn(0f, 30f))
        private set

    fun updateHomeCount(value: Int) {
        homeCount = value.coerceIn(4, 12)
        store.homeCount = homeCount
    }

    fun updateBoxColor(value: Color?) {
        boxColor = value
        store.boxColor = value?.toArgb() ?: 0
    }

    fun updateBoxTextColor(value: Color?) {
        boxTextColor = value
        store.boxTextColor = value?.toArgb() ?: 0
    }

    fun updateBoxAlpha(value: Float) {
        boxAlpha = value.coerceIn(0f, 1f)
        store.boxAlpha = boxAlpha
    }

    fun updateBoxRadius(value: Float) {
        boxRadius = value.coerceIn(0f, 30f)
        store.boxRadius = boxRadius
    }

    // ---- shortcuts (order + hidden state persist) --------------------------
    val sites = mutableStateListOf<Site>().apply {
        val saved = store.cachedSites
        addAll(if (saved.isEmpty()) Site.Bundled else saved)
    }

    val visibleSites: List<Site> get() = sites.filterNot { it.hidden }

    /** The Homepage shows one row only: the first [homeCount] boxes. */
    val homeSites: List<Site> get() = visibleSites.take(homeCount)

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
        // Launch on a Homepage tab: reuse one if it exists, otherwise add one.
        val home = tabs.firstOrNull { it.url.isBlank() }
            ?: BrowserTab(nextTabId++, "", "Homepage").also { tabs.add(it) }
        activeTabId = home.id
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

    fun applyInsetToPages(topCssPx: Float, bottomCssPx: Float) {
        webViews.values.forEach {
            it.evaluateJavascript(
                "window.__voidSetInset&&window.__voidSetInset($topCssPx,$bottomCssPx)",
                null,
            )
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

    /** A blank [url] is a Homepage tab. */
    private fun newTab(url: String) {
        if (browserOpen) captureActiveThumb()
        val tab = BrowserTab(
            id = nextTabId++,
            url = url,
            title = if (url.isBlank()) "Homepage" else "",
            desktop = ui.desktopDefault,
        )
        tabs.add(tab)
        activeTabId = tab.id
        address = url
        findOpen = false
        refreshScrolled()
        persistTabs()
    }

    fun switchTab(id: Long) {
        if (id == activeTabId || tabs.none { it.id == id }) return
        if (browserOpen) captureActiveThumb()
        activeTabId = id
        address = tabs.firstOrNull { it.id == id }?.url.orEmpty()
        findOpen = false
        refreshScrolled()
        persistTabs()
    }

    /** There is always at least one tab; closing the last one leaves a fresh Homepage. */
    private val closedTabs = ArrayList<BrowserTab>()

    fun closeTab(id: Long) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        tabs[index].takeIf { it.url.isNotBlank() }?.let {
            closedTabs.add(it)
            if (closedTabs.size > 10) closedTabs.removeAt(0)
        }
        tabs.removeAt(index)
        destroyWebView(id)
        runtimes.remove(id)
        tabThumbs.remove(id)
        cardRects.remove(id)
        if (tabs.isEmpty()) {
            val fresh = BrowserTab(nextTabId++, "", "Homepage")
            tabs.add(fresh)
            activeTabId = fresh.id
            address = ""
        } else if (activeTabId == id) {
            val next = tabs[maxOf(0, index - 1)]
            activeTabId = next.id
            address = next.url
        }
        findOpen = false
        refreshScrolled()
        persistTabs()
    }

    fun duplicateTab(id: Long) {
        val tab = tabs.firstOrNull { it.id == id } ?: return
        newTab(tab.url)
    }

    fun reopenClosedTab() {
        val last = closedTabs.removeLastOrNull()
        if (last == null) {
            showToast("No closed tabs")
        } else {
            newTab(last.url)
        }
    }

    fun closeOtherTabs(id: Long) {
        tabs.filter { it.id != id }.forEach {
            destroyWebView(it.id)
            runtimes.remove(it.id)
            tabThumbs.remove(it.id)
            cardRects.remove(it.id)
        }
        tabs.removeAll { it.id != id }
        if (tabs.none { it.id == id }) return
        activeTabId = id
        address = tabs.first().url
        findOpen = false
        refreshScrolled()
        persistTabs()
    }

    fun moveTab(from: Int, to: Int) {
        if (from !in tabs.indices || to !in tabs.indices || from == to) return
        val item = tabs.removeAt(from)
        tabs.add(to, item)
        persistTabs()
    }

    fun closeActiveTab() = closeTab(activeTabId)

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
        get() = cardRects[activeTabId]

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
        switchTab(id)
        closeTabGrid()
    }

    // ---- navigation -------------------------------------------------------
    /** True when the active tab shows a page; false on a Homepage tab. */
    val browserOpen: Boolean get() = activeTab?.url?.isNotBlank() == true

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

    /** Loads [url] in the current tab (a Homepage tab turns into that page). */
    fun open(url: String) {
        if (url.isBlank()) return
        val index = tabs.indexOfFirst { it.id == activeTabId }
        if (index < 0) {
            newTab(url)
            return
        }
        val wasHome = tabs[index].url.isBlank()
        val target = if (ui.httpsOnly && url.startsWith("http://")) "https://" + url.removePrefix("http://") else url
        tabs[index] = tabs[index].copy(
            url = target,
            title = "",
            desktop = tabs[index].desktop || (wasHome && ui.desktopDefault),
        )
        address = target
        webViews[activeTabId]?.loadUrl(target)
        refreshScrolled()
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

    /** The Home button: the current tab becomes a Homepage again. */
    fun goHome() {
        val index = tabs.indexOfFirst { it.id == activeTabId }
        if (index < 0) return
        destroyWebView(activeTabId)
        tabThumbs.remove(activeTabId)
        tabs[index] = tabs[index].copy(url = "", title = "Homepage")
        address = ""
        findOpen = false
        pageScrolled = false
        persistTabs()
    }

    override fun startNewTab() = newTab("")

    // ---- bottom bar: menu, scroll collapse, fullscreen ---------------------
    var menuOpen by mutableStateOf(false)

    fun closeMenu() {
        menuOpen = false
    }

    /** True once the page is scrolled: the nav pill folds into a dot. */
    var pageScrolled by mutableStateOf(false)
        private set

    /** Scroll offset of the current page in px; the status bar band scrolls away with it. */
    var pageScrollY by mutableIntStateOf(0)
        private set

    fun reportScrollY(y: Int) {
        pageScrollY = y
        pageScrolled = y > 20 * context.resources.displayMetrics.density
    }

    fun refreshScrolled() {
        val view = if (browserOpen) webViews[activeTabId] else null
        reportScrollY(view?.scrollY ?: 0)
    }

    /** Tapping the dot unfolds the pill again, even in the middle of a page. */
    fun expandPill() {
        pageScrolled = false
    }

    /** Asks the search field that matches the current page to take focus. */
    var focusRequest by mutableIntStateOf(0)
        private set

    fun requestSearchFocus() {
        menuOpen = false
        focusRequest++
    }

    /** True while the bottom search field has focus: only then the bar rides up with the keyboard. */
    var bottomFocus by mutableStateOf(false)

    /** Settings is a full screen page; [settingsPage] null shows the list of groups. */
    var settingsOpen by mutableStateOf(false)
    var settingsPage by mutableStateOf<String?>(null)

    /** Height the bottom controls cover, so pages can leave room for them. */
    var pageBottomInsetDp by mutableFloatStateOf(120f)

    var fullscreen by mutableStateOf(false)
        private set

    fun toggleFullscreen() {
        val window = activity?.window ?: return
        fullscreen = !fullscreen
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        if (fullscreen) {
            controller.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    // ---- look & feel ------------------------------------------------------
    /** Set by the app shell from the system setting (used when the theme mode is "system"). */
    var systemDark by mutableStateOf(false)

    val darkTheme: Boolean
        get() = when (ui.themeMode) {
            1 -> false
            2 -> true
            else -> systemDark
        }

    val desktopMode: Boolean get() = activeTab?.desktop ?: false

    var blockAds by mutableStateOf(store.blockAds)
        private set
    var webDark by mutableStateOf(store.webDark)
        private set

    fun updateDarkTheme(value: Boolean) {
        ui.themeMode = if (value) 2 else 1
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

    // ---- downloads ------------------------------------------------------------
    var downloadsOpen by mutableStateOf(false)

    /** A download waiting for the user's confirmation (name, path). */
    var pendingDownload by mutableStateOf<DownloadRequest?>(null)

    fun requestDownload(url: String, userAgent: String?, disposition: String?, mime: String?, length: Long) {
        if (!url.startsWith("http")) {
            showToast("This download type is not supported")
            return
        }
        val name = android.webkit.URLUtil.guessFileName(url, disposition, mime)
        val request = DownloadRequest(
            url = url,
            userAgent = userAgent,
            mime = mime,
            length = length,
            referer = currentUrl.ifBlank { null },
            fileName = name,
        )
        if (ui.askDownload) pendingDownload = request else startDownload(request, name)
    }

    fun startDownload(request: DownloadRequest, name: String) {
        pendingDownload = null
        downloads.enqueue(request.copy(fileName = name.ifBlank { request.fileName }), ui.downloadFolder)
        showToast("Downloading")
    }

    // ---- tab swipe on the nav pill ------------------------------------------------
    var tabHint by mutableStateOf<String?>(null)
        private set
    private var hintJob: Job? = null

    /** Moves to the previous (-1) / next (+1) tab. Returns false at either end. */
    fun switchTabBy(delta: Int): Boolean {
        val index = tabs.indexOfFirst { it.id == activeTabId }
        if (index < 0) return false
        val next = (index + delta).coerceIn(0, tabs.lastIndex)
        if (next == index) return false
        switchTab(tabs[next].id)
        val tab = tabs[next]
        tabHint = "${next + 1}/${tabs.size}  ${protoTabTitle(tab.url, tab.title)}"
        hintJob?.cancel()
        hintJob = scope.launch {
            delay(1100)
            tabHint = null
        }
        return true
    }

    // ---- more tools -------------------------------------------------------------------
    fun pasteAndGo() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        val text = if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0).coerceToText(context).toString().trim()
        } else {
            ""
        }
        if (text.isBlank()) showToast("Clipboard is empty") else search(text)
    }

    fun switchEngine() {
        val all = SearchProvider.entries
        val next = all[(all.indexOf(provider) + 1) % all.size]
        selectProvider(next)
        showToast("Search: ${next.label}")
    }

    fun toggleReader() {
        val b = bridge
        if (!browserOpen || b == null) {
            showToast("Open a page first")
            return
        }
        b.evaluate(READER_JS) { result ->
            showToast(if (result.contains("on")) "Reader mode on" else "Reader mode off")
        }
    }

    fun toggleTextOnly() {
        ui.loadImages = !ui.loadImages
        applyWebSettings()
        showToast(if (ui.loadImages) "Images on" else "Text only (reload the page)")
    }

    fun toggleKeepAwake() {
        ui.keepAwake = !ui.keepAwake
        applyKeepAwake()
        showToast(if (ui.keepAwake) "Screen stays on" else "Screen may sleep")
    }

    fun applyKeepAwake() {
        val window = activity?.window ?: return
        if (ui.keepAwake) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /** Pushes the browser settings to every open page. */
    fun applyWebSettings() {
        WebView.setWebContentsDebuggingEnabled(ui.debugging)
        webViews.forEach { (id, view) ->
            runCatching {
                applyWebPrefs(view, this, tabs.firstOrNull { it.id == id }?.desktop == true)
            }
        }
    }

    fun clearCookies() {
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        showToast("Cookies cleared")
    }

    fun clearCache() {
        webViews.values.forEach { it.clearCache(true) }
        showToast("Cache cleared")
    }

    fun saveOffline() {
        val b = bridge
        if (!browserOpen || b == null) {
            showToast("Open a page first")
            return
        }
        val name = b.currentTitle.ifBlank { "page" }.take(40).replace(Regex("[^A-Za-z0-9 _-]"), "_") + ".mht"
        val temp = java.io.File(context.cacheDir, "page-${System.currentTimeMillis()}.mht")
        b.saveArchive(temp.absolutePath) { ok ->
            if (!ok) {
                showToast("Could not save the page")
            } else {
                scope.launch {
                    val uri = withContext(Dispatchers.IO) {
                        runCatching {
                            downloads.publish(temp, name, "application/x-mimearchive", ui.downloadFolder)
                        }.getOrNull()
                    }
                    temp.delete()
                    showToast(
                        if (uri != null) "Saved to ${downloads.displayPath(ui.downloadFolder)}/$name"
                        else "Could not save the page",
                    )
                }
            }
        }
    }

    fun screenshot() {
        val bitmap = bridge?.takeIf { browserOpen }?.snapshot(1080)
        if (bitmap == null) {
            showToast("Open a page first")
            return
        }
        scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { savePicture(bitmap) }.isSuccess }
            showToast(if (ok) "Screenshot saved to Pictures/VOID" else "Could not save the screenshot")
        }
    }

    private fun savePicture(bitmap: android.graphics.Bitmap) {
        val name = "VOID-${System.currentTimeMillis()}.png"
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, name)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VOID")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw java.io.IOException("insert failed")
            resolver.openOutputStream(uri)?.use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        } else {
            val dir = java.io.File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "VOID")
            dir.mkdirs()
            java.io.File(dir, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    // ---- island actions ---------------------------------------------------
    override fun browserBack() {
        if (!browserOpen) return
        val b = bridge
        if (b != null && b.canGoBack) b.back() else goHome()
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

    override fun openSettings() {
        settingsPage = null
        settingsOpen = true
        menuOpen = false
    }

    /** Opens Settings straight on one of its pages (e.g. "home", "appearance", "menu"). */
    fun openSettingsPage(page: String) {
        settingsPage = page
        settingsOpen = true
        menuOpen = false
    }

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

    fun scrollToBottom() {
        bridge?.scrollToBottom()
    }

    fun stopLoading() {
        bridge?.stop()
    }

    fun resetZoom() {
        bridge?.resetZoom()
        showToast("100%")
    }

    override fun translatePage() {
        if (!browserOpen || currentUrl.isBlank()) {
            showToast("Open a page first")
            return
        }
        open("https://translate.google.com/translate?sl=auto&tl=en&u=" + Uri.encode(currentUrl))
    }

    override fun openDownloads() {
        downloadsOpen = true
        menuOpen = false
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
        /** Toggles a simple reader view on the current page; answers "on" or "off". */
        const val READER_JS = "(function(){var id='__void_reader';var s=document.getElementById(id);" +
            "if(s){s.remove();return 'off';}s=document.createElement('style');s.id=id;" +
            "s.textContent='header,nav,aside,footer,[role=banner],[role=navigation],[class*=sidebar]," +
            "[class*=cookie],[class*=advert],[id*=ads],iframe{display:none!important}" +
            "body{max-width:720px!important;margin:0 auto!important;padding:16px!important;" +
            "font-size:19px!important;line-height:1.7!important;background:#fbf9f4!important;" +
            "color:#222!important}img{max-width:100%!important;height:auto!important}';" +
            "document.documentElement.appendChild(s);return 'on';})()"

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
