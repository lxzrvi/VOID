package dev.voidapp.data

import androidx.compose.runtime.Immutable

/** A box on the home page. Position/size is in pixels of the layout box, relative 0..1. */
@Immutable
data class BoxEdit(
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val radiusDp: Float = 24f,
    val alpha: Float = 1f,
    val colorArgb: Long? = null,
)

/** A shortcut in the grid. [bundled] shortcuts ship with the app and can be hidden. */
@Immutable
data class Site(
    val name: String,
    val url: String,
    val custom: Boolean = false,
    val hidden: Boolean = false,
) {
    val host: String
        get() = url.substringAfter("://", url).substringBefore("/").removePrefix("www.")

    val faviconUrl: String
        get() = "https://www.google.com/s2/favicons?sz=128&domain=$host"

    val key: String get() = url

    companion object {
        /** The 14 bundled shortcuts, in the order they appear on first launch. */
        val Bundled: List<Site> = listOf(
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
enum class SearchProvider(val label: String, val queryUrl: String, val host: String) {
    Google("Google", "https://www.google.com/search?q=", "google.com"),
    DuckDuckGo("DuckDuckGo", "https://duckduckgo.com/?q=", "duckduckgo.com"),
    Bing("Bing", "https://www.bing.com/search?q=", "bing.com"),
    Brave("Brave", "https://search.brave.com/search?q=", "brave.com"),
    Wikipedia("Wikipedia", "https://en.wikipedia.org/w/index.php?search=", "wikipedia.org");

    fun urlFor(query: String): String = queryUrl + android.net.Uri.encode(query)

    /** Favicon of the provider itself, shown in the bar. */
    val faviconUrl: String
        get() = "https://www.google.com/s2/favicons?sz=64&domain=$host"

    companion object {
        fun fromName(name: String?): SearchProvider = entries.firstOrNull { it.name == name } ?: Google
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
@Immutable
data class PageEntry(
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val host: String
        get() = url.substringAfter("://", url).substringBefore("/").removePrefix("www.")
    val faviconUrl: String
        get() = "https://www.google.com/s2/favicons?sz=64&domain=$host"
    val label: String
        get() = title.ifBlank { url.substringAfter("://").substringBefore("/") }
}

/** A quick note from the notes button. */
@Immutable
data class Note(
    val id: Long,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Custom background of the home page. */
@Immutable
data class Background(
    val kind: Kind = Kind.Brand,
    val imageUri: String? = null,
    val blur: Float = 0f,
    val dim: Float = 0f,
    val brightness: Float = 0f,
    val angle: Float = 135f,
    val startColor: Long = 0xFFC10520,
    val endColor: Long = 0xFF0A0A0B,
) {
    enum class Kind { Brand, Gradient, Image }
}

/** A tab as it is stored between launches. */
@Immutable
data class SavedTab(
    val url: String,
    val title: String,
    val desktop: Boolean = false,
)
