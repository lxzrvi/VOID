package dev.voidapp.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voidapp.data.SearchProvider
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.proto.ProtoColors
import dev.voidapp.ui.proto.protoColors
import dev.voidapp.ui.theme.Poppins
import kotlin.math.roundToInt

private class Group(val page: String, val title: String, val subtitle: String, val icon: ImageVector)

private val Groups = listOf(
    Group("home", "Home page", "Boxes, wordmark, layout, search pill", Icons.Filled.Home),
    Group("appearance", "Appearance", "Theme, colours, bars, text size", Icons.Filled.Palette),
    Group("menu", "Hamburger menu", "Move, remove and add tools", Icons.Filled.Menu),
    Group("browser", "Browser", "Pages, text size, media, screen", Icons.Filled.Language),
    Group("search", "Search", "Search engine and suggestions", Icons.Filled.Search),
    Group("downloads", "Downloads", "Confirmation, folder, manager", Icons.Filled.Download),
    Group("privacy", "Privacy & security", "Blocking, cookies, clearing data", Icons.Filled.Shield),
    Group("advanced", "Advanced", "JavaScript, user agent, debugging, resets", Icons.Filled.Tune),
    Group("about", "About", "Version and credits", Icons.Filled.Info),
)

/**
 * Settings: a full screen page. It opens on a list of groups; a tap opens that
 * group's own page, back returns to the list.
 */
@Composable
fun SettingsScreen(state: VoidAppState, modifier: Modifier = Modifier) {
    val c = protoColors()
    val page = state.settingsPage
    val group = Groups.firstOrNull { it.page == page }

    BackHandler(enabled = true) {
        if (page != null) state.settingsPage = null else state.settingsOpen = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.page)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(c.soft)
                    .clickable { if (page != null) state.settingsPage = null else state.settingsOpen = false },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = c.ink, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = group?.title ?: "Settings",
                color = c.ink,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (page) {
                "home" -> HomePageSettings(state, c)
                "appearance" -> AppearanceSettings(state, c)
                "menu" -> MenuSettings(state, c)
                "browser" -> BrowserSettings(state, c)
                "search" -> SearchSettings(state, c)
                "downloads" -> DownloadSettings(state, c)
                "privacy" -> PrivacySettings(state, c)
                "advanced" -> AdvancedSettings(state, c)
                "about" -> AboutSettings(state, c)
                else -> GroupList(state, c)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun GroupList(state: VoidAppState, c: ProtoColors) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.soft)
            .border(1.dp, c.line, shape),
    ) {
        Groups.forEachIndexed { index, group ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { state.settingsPage = group.page }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(c.page),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(group.icon, null, tint = c.ink, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.title, color = c.ink, fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(group.subtitle, color = c.tabIdle, fontFamily = Poppins, fontSize = 11.sp)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.tabIdle, modifier = Modifier.size(22.dp))
            }
            if (index < Groups.lastIndex) {
                Box(Modifier.padding(start = 68.dp).fillMaxWidth().height(1.dp).background(c.line))
            }
        }
    }
}

// =============================================================================
// the smaller pages
// =============================================================================

@Composable
private fun BrowserSettings(state: VoidAppState, c: ProtoColors) {
    val ui = state.ui
    SectionTitle(c, "PAGES")
    Panel(c) {
        SwitchRow(c, "Dark web pages", state.webDark) { state.updateWebDark(it) }
        SwitchRow(c, "Open sites as desktop by default", ui.desktopDefault) { ui.desktopDefault = it }
        SwitchRow(c, "Load images", ui.loadImages) {
            ui.loadImages = it
            state.applyWebSettings()
        }
        SwitchRow(c, "Allow media to autoplay", ui.autoplay) {
            ui.autoplay = it
            state.applyWebSettings()
        }
        SliderRow(c, "Default text size", "${ui.textZoom}%", ui.textZoom.toFloat(), 50f..200f, steps = 14) {
            ui.textZoom = (it / 10f).roundToInt() * 10
            state.applyWebSettings()
        }
    }
    SectionTitle(c, "SCREEN")
    Panel(c) {
        SwitchRow(c, "Keep the screen on", ui.keepAwake) {
            ui.keepAwake = it
            state.applyKeepAwake()
        }
    }
    Note(c, "Turn images off to browse text only; reload the page to see it.")
}

@Composable
private fun SearchSettings(state: VoidAppState, c: ProtoColors) {
    SectionTitle(c, "SEARCH ENGINE")
    Panel(c) {
        SearchProvider.entries.forEach { provider ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { state.selectProvider(provider) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = provider.label,
                    color = c.ink,
                    fontFamily = Poppins,
                    fontSize = 13.sp,
                    fontWeight = if (provider == state.provider) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                )
                if (provider == state.provider) {
                    Icon(Icons.Filled.Check, null, tint = c.ink, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
    SectionTitle(c, "SUGGESTIONS")
    Panel(c) {
        SwitchRow(c, "Show search suggestions", state.suggestionsEnabled) { state.updateSuggestionsEnabled(it) }
    }
}

@Composable
private fun DownloadSettings(state: VoidAppState, c: ProtoColors) {
    val ui = state.ui
    SectionTitle(c, "BEFORE A DOWNLOAD")
    Panel(c) {
        SwitchRow(c, "Ask before downloading", ui.askDownload) { ui.askDownload = it }
    }
    Note(c, "On: a box shows the file name (you can rename it) and where it will be saved.")

    SectionTitle(c, "WHERE FILES GO")
    Panel(c) {
        TextRow(c, "Folder name", ui.downloadFolder, "VOID") {
            ui.downloadFolder = it.filter { ch -> ch.isLetterOrDigit() || ch == ' ' || ch == '_' || ch == '-' }
        }
    }
    Note(c, "Saved to ${state.downloads.displayPath(ui.downloadFolder.ifBlank { "VOID" })}")

    SectionTitle(c, "MANAGER")
    Panel(c) {
        ValueRow(c, "Open the downloads page", "${state.downloads.items.size}") {
            state.settingsOpen = false
            state.downloadsOpen = true
        }
        ValueRow(c, "Pause all") { state.downloads.pauseAll() }
        ValueRow(c, "Resume all") { state.downloads.resumeAll() }
        ValueRow(c, "Clear finished downloads") { state.downloads.clearFinished() }
    }
}

@Composable
private fun PrivacySettings(state: VoidAppState, c: ProtoColors) {
    val ui = state.ui
    SectionTitle(c, "BLOCKING")
    Panel(c) {
        SwitchRow(c, "Block ads & trackers", state.blockAds) { state.updateBlockAds(it) }
        SwitchRow(c, "Block pop-ups", ui.blockPopups) { ui.blockPopups = it }
    }
    SectionTitle(c, "COOKIES")
    Panel(c) {
        SwitchRow(c, "Allow cookies", ui.cookies) {
            ui.cookies = it
            state.applyWebSettings()
        }
        SwitchRow(c, "Allow third-party cookies", ui.thirdPartyCookies, enabled = ui.cookies) {
            ui.thirdPartyCookies = it
            state.applyWebSettings()
        }
    }
    SectionTitle(c, "SECURITY")
    Panel(c) {
        SwitchRow(c, "Safe browsing", ui.safeBrowsing) {
            ui.safeBrowsing = it
            state.applyWebSettings()
        }
        SwitchRow(c, "HTTPS only (upgrade http links)", ui.httpsOnly) { ui.httpsOnly = it }
    }
    SectionTitle(c, "CLEAR")
    Panel(c) {
        ValueRow(c, "Clear cookies") { state.clearCookies() }
        ValueRow(c, "Clear cache") { state.clearCache() }
        ValueRow(c, "Clear history") {
            state.clearHistoryOnly()
            state.showToast("History cleared")
        }
        ValueRow(c, "Clear everything (history, bookmarks, cookies, cache)") { state.clearBrowsingData() }
    }
}

@Composable
private fun AdvancedSettings(state: VoidAppState, c: ProtoColors) {
    val ui = state.ui
    SectionTitle(c, "PAGES")
    Panel(c) {
        SwitchRow(c, "JavaScript", ui.jsEnabled) {
            ui.jsEnabled = it
            state.applyWebSettings()
        }
        TextRow(c, "User agent (empty = default)", ui.customUa, "Mozilla/5.0 ...") {
            ui.customUa = it
            state.applyWebSettings()
        }
        SwitchRow(c, "Remote debugging (chrome://inspect)", ui.debugging) {
            ui.debugging = it
            state.applyWebSettings()
        }
    }
    SectionTitle(c, "RESET")
    Panel(c) {
        ValueRow(c, "Reset browser settings") {
            ui.resetBrowser()
            state.applyWebSettings()
            state.applyKeepAwake()
            state.showToast("Browser settings reset")
        }
        ValueRow(c, "Reset appearance") {
            ui.resetAppearance()
            state.showToast("Appearance reset")
        }
        ValueRow(c, "Reset the hamburger menu") {
            ui.resetMenuTools()
            state.showToast("Menu reset")
        }
        ValueRow(c, "Reset the home page") {
            ui.resetHomePage()
            state.showToast("Home page reset")
        }
    }
}

@Composable
private fun AboutSettings(state: VoidAppState, c: ProtoColors) {
    val context = LocalContext.current
    val version = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()
    Panel(c) {
        Text("VOID", color = c.ink, fontFamily = Poppins, fontWeight = FontWeight.Light, fontSize = 34.sp)
        Text("Version $version", color = c.tabIdle, fontFamily = Poppins, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "A small, fast browser. ${state.tabs.size} tab${if (state.tabs.size == 1) "" else "s"} open.",
            color = c.ink,
            fontFamily = Poppins,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(6.dp))
    }
}
