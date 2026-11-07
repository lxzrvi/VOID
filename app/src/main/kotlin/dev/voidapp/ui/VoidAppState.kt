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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.voidapp.data.DayEvent
import dev.voidapp.data.OnThisDay
import dev.voidapp.data.PageEntry
import dev.voidapp.data.SearchProvider
import dev.voidapp.data.Site
import dev.voidapp.data.VoidStore
import dev.voidapp.data.urlForInput
import dev.voidapp.ui.browser.BrowserBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Which panel is open above the island bar. */
enum class IslandPanel { None, Tools, Navigator, Settings }

/** What the island's panel buttons should do; [BrowserScreen] implements it. */
interface IslandActions {
    fun browserBack()
    fun browserForward()
    fun browserReload()
    fun browserIslandFocus()
    fun openHistory()
    fun openBookmarks()
    fun openSettings()
    fun toggleDesktopMode()
    fun zoomBy(delta: Float)
    fun findInPage()
    fun saveCurrent()
    fun copyCurrent()
    fun shareCurrent()
    fun openCurrentExternally()
    fun bookmarkCurrent()
}

/**
 * All app state in one remembered object: island placement, search provider,
 * shortcuts, the current page, history/bookmarks and today's event.
 */
@Stable
class VoidAppState(
    private val context: Context,
    private val store: VoidStore,
    private val scope: CoroutineScope,
) : IslandActions {

    /** Set from the composition so we can finish the Activity on exit. */
    private var activity: Activity? = null

    fun attachActivity(value: Context) {
        activity = value as? Activity
    }

    /** Installed by BrowserScreen while a WebView exists. */
    var bridge: BrowserBridge? by mutableStateOf(null)

    // ---- island placement -------------------------------------------------
    var islandOffset by mutableStateOf(store.islandOffset)
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

    // ---- search results (used by the island's counter/navigator) ----------
    private val index = listOf(
        "Google", "YouTube", "GitHub", "Reddit", "X", "Instagram", "Wikipedia",
        "Gmail", "Drive", "Notion", "Stack Overflow", "ChatGPT", "Netflix", "Amazon",
    )

    /** Suggestions for [query]: matching shortcuts first, then the provider. */
    fun results(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val matching = index.filter { it.contains(query, ignoreCase = true) }
        val withProvider = listOf("${provider.label}: $query") + matching
        return withProvider.distinct().take(9)
    }

    fun resultCount(query: String): Int = results(query).size

    /** Show history / bookmarks as a result list inside the island sheet. */
    fun showHistoryInIsland() {
        islandList = history.map { it.title.ifBlank { it.url } }.take(9)
    }

    fun showBookmarksInIsland() {
        islandList = bookmarks.map { it.title.ifBlank { it.url } }.take(9)
    }

    /** Overrides the suggestion list (history, bookmarks, settings). */
    var islandList: List<String> by mutableStateOf(emptyList())
        private set

    /** Panel shown above the island bar. */
    var islandPanel by mutableStateOf(IslandPanel.None)

    fun clearIslandList() {
        islandList = emptyList()
    }

    /** Opens the nth result of the current query (1-based in the UI). */
    fun openResult(query: String, position: Int) {
        val pick = results(query).getOrNull(position) ?: return
        if (pick.startsWith("${provider.label}: ")) {
            search(pick.removePrefix("${provider.label}: "))
            return
        }
        val site = sites.firstOrNull { it.name == pick }
        if (site != null) open(site.url) else search(pick)
    }

    // ---- shortcuts --------------------------------------------------------
    val sites = mutableStateListOf<Site>().apply {
        addAll(Site.Defaults)
        addAll(store.customSites)
    }

    fun addSite(name: String, url: String) {
        val clean = url.trim().let { if (it.startsWith("http")) it else "https://$it" }
        val label = name.trim().ifBlank { clean.substringAfter("://").substringBefore("/") }
        sites.add(Site(name = label, url = clean, custom = true))
        store.customSites = sites.filter { it.custom }
    }

    fun removeSite(site: Site) {
        sites.remove(site)
        store.customSites = sites.filter { it.custom }
    }

    // ---- navigation -------------------------------------------------------
    var browserOpen by mutableStateOf(false)
        private set
    var address by mutableStateOf("")
        private set

    /** Transient message shown in the bar area (e.g. "Copied"). */
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
        address = url
        browserOpen = true
    }

    fun search(query: String) {
        if (query.isBlank()) return
        open(resolve(query))
    }

    fun goHome() {
        browserOpen = false
    }

    fun closeBrowser() = goHome()

    fun requestAppExit() {
        activity?.finish()
    }

    // ---- history / bookmarks ---------------------------------------------
    val history = mutableStateListOf<PageEntry>().apply { addAll(store.history) }
    val bookmarks = mutableStateListOf<PageEntry>().apply { addAll(store.bookmarks) }

    /** Which list the island sheet should show, if any. */
    var sheet by mutableStateOf(Sheet.None)
        private set

    enum class Sheet { None, History, Bookmarks, Settings }

    fun showSheet(value: Sheet) {
        sheet = value
    }

    fun hideSheet() {
        sheet = Sheet.None
    }

    fun recordVisit(url: String, title: String) {
        if (url.isBlank() || url.startsWith("about:")) return
        val entry = PageEntry(
            url = url,
            title = title.ifBlank { url.substringAfter("://").substringBefore("/") },
        )
        history.removeAll { it.url == url }
        history.add(0, entry)
        store.history = history.toList()
    }

    fun toggleBookmark(url: String, title: String) {
        if (url.isBlank()) return
        val existing = bookmarks.firstOrNull { it.url == url }
        if (existing != null) bookmarks.remove(existing) else bookmarks.add(0, PageEntry(url, title))
        store.bookmarks = bookmarks.toList()
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
            val fetched = OnThisDay.fetch()
            event = fetched.firstOrNull()
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

    override fun browserIslandFocus() {
        // "Tabs" is a single-tab browser for now: reload current page.
        bridge?.takeIf { browserOpen }?.reload()
    }

    override fun openHistory() = showSheet(Sheet.History)

    override fun openBookmarks() = showSheet(Sheet.Bookmarks)

    override fun openSettings() = showSheet(Sheet.Settings)

    override fun toggleDesktopMode() {
        bridge?.setDesktopMode(true)
        showToast("Desktop site")
    }

    override fun zoomBy(delta: Float) {
        bridge?.zoomBy(delta)
    }

    override fun findInPage() {
        // The bar's own search field doubles as find-in-page while browsing.
        showToast("Type to find")
    }

    override fun saveCurrent() {
        bridge?.savePage()
        showToast("Saving…")
    }

    override fun copyCurrent() {
        val url = bridge?.currentUrl ?: address
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("VOID", url))
        showToast("Copied")
    }

    override fun shareCurrent() {
        val url = bridge?.currentUrl ?: address
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun openCurrentExternally() {
        val url = bridge?.currentUrl ?: address
        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun bookmarkCurrent() {
        val url = bridge?.currentUrl ?: address
        val title = bridge?.currentTitle ?: url
        toggleBookmark(url, title)
        showToast(if (isBookmarked(url)) "Bookmarked" else "Removed")
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
        ).also { state ->
            // Remember the Activity separately: the store only needs the app context.
            state.attachActivity(context)
        }
    }
}
