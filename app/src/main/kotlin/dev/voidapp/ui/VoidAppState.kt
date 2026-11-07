package dev.voidapp.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.voidapp.data.Background
import dev.voidapp.data.BoxEdit
import dev.voidapp.data.DayEvent
import dev.voidapp.data.Note
import dev.voidapp.data.OnThisDay
import dev.voidapp.data.PageEntry
import dev.voidapp.data.SearchProvider
import dev.voidapp.data.Site
import dev.voidapp.data.VoidStore
import dev.voidapp.data.urlForInput
import dev.voidapp.ui.browser.BrowserBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
    fun toggleDesktopMode()
    fun zoomBy(delta: Float)
    fun saveCurrent()
    fun copyCurrent()
    fun shareCurrent()
    fun openCurrentExternally()
    fun bookmarkCurrent()
    fun scrollToTop()
}

/** A browser tab: URL plus the title the WebView reported. */
data class BrowserTab(
    val id: Long,
    val url: String,
    val title: String = "",
)

/**
 * Single source of truth. Everything the first frame needs (sites, favicons,
 * notes, background, box edits, tool order) is read from [VoidStore]
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

    /** Installed by BrowserScreen while a WebView exists. */
    var bridge: BrowserBridge? by mutableStateOf(null)

    // ---- island -----------------------------------------------------------
    var islandOffset by mutableStateOf(store.islandOffset)
        private set
    var islandOnRight by mutableStateOf(store.islandOnRight)
        private set
    /** Set while the user swipes up on the island / opens the tab grid. */
    var tabGridOpen by mutableStateOf(false)

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

    fun results(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val matching = suggestionIndex.filter { it.contains(query, ignoreCase = true) }
        return (listOf("${provider.label}: $query") + matching).distinct().take(9)
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
        sites.add(Site(name = label, url = clean, custom = true))
        persistSites()
    }

    fun removeSite(site: Site) {
        sites.remove(site)
        persistSites()
    }

    fun moveSite(from: Int, to: Int) {
        if (from !in sites.indices || to !in sites.indices) return
        val item = sites.removeAt(from)
        sites.add(to, item)
        persistSites()
    }

    /** Fills the favicon cache so shortcuts render without a flash on launch. */
    /** Favicon stored from a previous launch, so the first frame is complete. */
    fun faviconFor(url: String): String? = faviconCache[url]

    fun cacheFavicon(url: String, bytesBase64: String) {
        if (faviconCache[url] == bytesBase64) return
        val map = store.cachedFavicons.toMutableMap()
        map[url] = bytesBase64
        store.writeFavicons(map)
        faviconCache.putAll(map)
    }

    /** In-memory mirror of the persisted favicons (survives the first frame). */
    private val faviconCache = mutableStateMapOf<String, String>().apply {
        putAll(store.cachedFavicons)
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
        addAll(store.cachedTools.ifEmpty { DEFAULT_TOOLS })
    }

    fun moveTool(from: Int, to: Int) {
        if (from !in toolOrder.indices || to !in toolOrder.indices) return
        val item = toolOrder.removeAt(from)
        toolOrder.add(to, item)
        store.writeStringList("tool_order", toolOrder.toList())
    }

    // ---- tabs -------------------------------------------------------------
    val tabs = mutableStateListOf<BrowserTab>()
    var activeTabId by mutableStateOf(0L)
    private var nextTabId = 1L

    val activeTab: BrowserTab? get() = tabs.firstOrNull { it.id == activeTabId }

    fun newTab(url: String = "") {
        val tab = BrowserTab(id = nextTabId++, url = url)
        tabs.add(tab)
        activeTabId = tab.id
        if (url.isNotBlank()) {
            address = url
            browserOpen = true
        }
    }

    fun closeTab(id: Long) {
        tabs.removeAll { it.id == id }
        if (activeTabId == id) {
            activeTabId = tabs.lastOrNull()?.id ?: 0L
            val next = tabs.lastOrNull()
            if (next != null) address = next.url else browserOpen = false
        }
        if (tabs.isEmpty()) browserOpen = false
    }

    fun updateActiveTab(url: String, title: String) {
        val index = tabs.indexOfFirst { it.id == activeTabId }
        if (index >= 0) tabs[index] = tabs[index].copy(url = url, title = title)
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
            kotlinx.coroutines.delay(1600)
            if (toast == message) toast = null
        }
    }

    fun open(url: String) {
        if (url.isBlank()) return
        if (tabs.isEmpty()) newTab(url) else {
            address = url
            updateActiveTab(url, "")
        }
        browserOpen = true
    }

    fun openInNewTab(url: String) {
        newTab(url)
        browserOpen = true
    }

    fun search(query: String) {
        if (query.isBlank()) return
        open(resolve(query))
    }

    fun goHome() {
        browserOpen = false
        tabGridOpen = false
    }

    fun showTab(id: Long) {
        if (tabs.none { it.id == id }) return
        activeTabId = id
        address = tabs.firstOrNull { it.id == id }?.url.orEmpty()
        browserOpen = true
    }

    fun closeActiveTab() {
        closeTab(activeTabId)
        if (tabs.isEmpty()) {
            browserOpen = false
            tabGridOpen = false
        }
    }

    // ---- look & feel ------------------------------------------------------
    var darkTheme by mutableStateOf(store.darkTheme)
        private set
    var desktopMode by mutableStateOf(false)
        private set

    fun updateDarkTheme(value: Boolean) {
        darkTheme = value
        store.darkTheme = value
    }

    fun toggleTheme() = updateDarkTheme(!darkTheme)

    fun updateDesktopMode(value: Boolean) {
        desktopMode = value
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

    fun recordVisit(url: String, title: String) {
        if (url.isBlank() || url.startsWith("about:")) return
        val entry = PageEntry(url, title.ifBlank { url.substringAfter("://").substringBefore("/") })
        history.removeAll { it.url == url }
        history.add(0, entry)
        store.writeEntries("history", history.toList())
        updateActiveTab(url, entry.title)
    }

    fun toggleBookmark(url: String, title: String) {
        if (url.isBlank()) return
        val existing = bookmarks.firstOrNull { it.url == url }
        if (existing != null) bookmarks.remove(existing) else bookmarks.add(0, PageEntry(url, title))
        store.writeEntries("bookmarks", bookmarks.toList())
    }

    fun isBookmarked(url: String): Boolean = bookmarks.any { it.url == url }

    fun clearBrowsingData() {
        history.clear()
        bookmarks.clear()
        store.clearBrowsingData()
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

    // ---- island actions ---------------------------------------------------
    override fun browserBack() {
        val bridge = bridge
        if (bridge != null && browserOpen) bridge.back() else goHome()
    }

    override fun browserForward() {
        bridge?.takeIf { browserOpen }?.forward()
    }

    override fun browserReload() {
        bridge?.takeIf { browserOpen }?.reload()
    }

    override fun openHistory() = showPanel(IslandPanel.Navigator, history.take(9).map { it.label })

    override fun openBookmarks() = showPanel(IslandPanel.Navigator, bookmarks.take(9).map { it.label })

    override fun openSettings() = showPanel(IslandPanel.Settings)

    override fun openNotes() = showPanel(IslandPanel.Notes)

    override fun openTabs() {
        tabGridOpen = true
    }

    override fun findInPage() {
        bridge?.findInPage()
    }

    override fun toggleDesktopMode() {
        updateDesktopMode(!desktopMode)
        showToast(if (desktopMode) "Desktop site" else "Mobile site")
    }

    override fun zoomBy(delta: Float) {
        bridge?.zoomBy(delta)
    }

    override fun saveCurrent() {
        bridge?.savePage()
        showToast("Saving…")
    }

    override fun copyCurrent() = copyLink(currentUrl)

    override fun shareCurrent() = shareLink(currentUrl)

    override fun openCurrentExternally() {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse(currentUrl))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override fun bookmarkCurrent() {
        toggleBookmark(currentUrl, bridge?.currentTitle ?: currentUrl)
        showToast(if (isBookmarked(currentUrl)) "Bookmarked" else "Removed")
    }

    override fun scrollToTop() {
        bridge?.scrollToTop()
    }

    // ---- link helpers (bar long-press menu) -------------------------------
    val currentUrl: String
        get() = bridge?.currentUrl ?: address

    fun copyLink(url: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("VOID", url))
        showToast("Copied")
    }

    fun shareLink(url: String) {
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
            "back", "forward", "reload", "home", "tabs",
            "notes", "history", "bookmark", "share", "copy",
            "external", "desktop", "zoom_in", "zoom_out", "find",
            "top", "settings", "clear", "save", "theme",
        )
    }
}

@Composable
fun rememberVoidAppState(): VoidAppState {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    return remember {
        VoidAppState(
            context = context.applicationContext,
            store = VoidStore(context.applicationContext),
            scope = scope,
        ).also { it.attachActivity(context) }
    }
}
