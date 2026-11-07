package dev.voidapp.data

/** A shortcut shown in the home grid. [url] is what gets opened. */
data class Site(
    val name: String,
    val url: String,
    val custom: Boolean = false,
) {
    val host: String
        get() = url.substringAfter("://", url).substringBefore("/").removePrefix("www.")

    /** Favicons come from Google's public favicon service, no API key needed. */
    val faviconUrl: String
        get() = "https://www.google.com/s2/favicons?sz=128&domain=$host"

    companion object {
        /** The 14 defaults the home grid starts with. */
        val Defaults: List<Site> = listOf(
            Site("Google", "https://www.google.com"),
            Site("YouTube", "https://www.youtube.com"),
            Site("GitHub", "https://github.com"),
            Site("Reddit", "https://www.reddit.com"),
            Site("X", "https://x.com"),
            Site("Instagram", "https://www.instagram.com"),
            Site("Wikipedia", "https://www.wikipedia.org"),
            Site("Gmail", "https://mail.google.com"),
            Site("Drive", "https://drive.google.com"),
            Site("Notion", "https://www.notion.so"),
            Site("Stack Overflow", "https://stackoverflow.com"),
            Site("ChatGPT", "https://chatgpt.com"),
            Site("Netflix", "https://www.netflix.com"),
            Site("Amazon", "https://www.amazon.in"),
        )
    }
}

/** One search engine the bar can use. */
enum class SearchProvider(val label: String, val queryUrl: String) {
    Google("Google", "https://www.google.com/search?q="),
    DuckDuckGo("DuckDuckGo", "https://duckduckgo.com/?q="),
    Bing("Bing", "https://www.bing.com/search?q="),
    Brave("Brave", "https://search.brave.com/search?q="),
    Wikipedia("Wikipedia", "https://en.wikipedia.org/w/index.php?search=");

    fun urlFor(query: String): String = queryUrl + android.net.Uri.encode(query)

    companion object {
        fun fromName(name: String?): SearchProvider =
            entries.firstOrNull { it.name == name } ?: Google
    }
}

/** Turns whatever the user typed into something loadable. */
fun urlForInput(input: String, provider: SearchProvider): String {
    val text = input.trim()
    if (text.isEmpty()) return provider.urlFor("")
    val looksLikeUrl = text.startsWith("http://") || text.startsWith("https://")
    val looksLikeHost = !text.contains(' ') && text.contains('.') &&
        text.substringAfterLast('.').length in 2..24
    return when {
        looksLikeUrl -> text
        looksLikeHost -> "https://$text"
        else -> provider.urlFor(text)
    }
}

/** History / bookmark entry. */
data class PageEntry(
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val host: String
        get() = url.substringAfter("://", url).substringBefore("/").removePrefix("www.")
    val faviconUrl: String
        get() = "https://www.google.com/s2/favicons?sz=64&domain=$host"
}
