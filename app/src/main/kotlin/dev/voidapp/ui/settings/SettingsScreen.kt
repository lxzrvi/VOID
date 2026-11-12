package dev.voidapp.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
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
import dev.voidapp.ui.proto.rememberMotion
import dev.voidapp.ui.theme.Poppins
import kotlin.math.roundToInt

private class Group(val page: String, val title: String, val subtitle: String, val icon: ImageVector)

private val Groups = listOf(
    Group("home", "Home page", "Boxes, wordmark, layout, search pill", Icons.Filled.Home),
    Group("appearance", "Appearance", "Theme, colours, bars, text size", Icons.Filled.Palette),
    Group("motion", "Motion & blur", "Animation style, speed, live blur", Icons.Filled.AutoAwesome),
    Group("menu", "Hamburger menu", "Move, remove and add tools", Icons.Filled.Menu),
    Group("gestures", "Gestures", "Swipes, pull-up, haptics, back button", Icons.Filled.TouchApp),
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
            Crossfade(targetState = group?.title ?: "Settings", animationSpec = tween(180), label = "title") { title ->
                Text(
                    text = title,
                    color = c.ink,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                )
            }
        }

        val motion = rememberMotion()
        val fast = if (motion.instant) 1 else 180
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(motion.screen()) { it / 3 } + fadeIn(tween(fast))) togetherWith
                        (slideOutHorizontally(motion.screen()) { -it / 5 } + fadeOut(tween(fast / 2)))
                } else {
                    (slideInHorizontally(motion.screen()) { -it / 5 } + fadeIn(tween(fast))) togetherWith
                        (slideOutHorizontally(motion.screen()) { it / 3 } + fadeOut(tween(fast / 2)))
                }
            },
            modifier = Modifier.weight(1f),
            label = "settingsPage",
        ) { current ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (current) {
                    "home" -> HomePageSettings(state, c)
                    "appearance" -> AppearanceSettings(state, c)
                    "motion" -> MotionSettings(state, c)
                    "menu" -> MenuSettings(state, c)
                    "gestures" -> GestureSettings(state, c)
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
private fun MotionSettings(state: VoidAppState, c: ProtoColors) {
    val ui = state.ui
    Section(c, "Animation style", "How everything moves", initiallyOpen = true) {
        ChoiceRow(c, listOf("Bouncy", "Smooth", "Off"), ui.animStyle) { ui.animStyle = it }
        SliderRow(c, "Speed", "${(ui.animSpeed * 100).roundToInt()}%", ui.animSpeed, 0.6f..1.6f) { ui.animSpeed = it }
    }
    Note(c, "Bouncy overshoots and settles, Smooth glides without overshoot, Off removes animations.")

    Section(c, "Live blur", "Bars blur the page behind them", initiallyOpen = true) {
        SwitchRow(c, "Blur behind bars", ui.blurEnabled) { ui.blurEnabled = it }
        SliderRow(c, "Blur strength", "${ui.blurRadius.roundToInt()} dp", ui.blurRadius, 0f..40f, enabled = ui.blurEnabled) { ui.blurRadius = it }
        SliderRow(c, "Frost grain", "${(ui.blurNoise * 100).roundToInt()}%", ui.blurNoise, 0f..0.2f, enabled = ui.blurEnabled) { ui.blurNoise = it }
    }
    Note(c, "Live blur needs Android 12 or newer; older phones show the plain translucent bar. Make bars more see-through in Appearance > Bars.")
}

@Composable
private fun GestureSettings(state: VoidAppState, c: ProtoColors) {
    val ui = state.ui
    Section(c, "Nav pill", "Back, forward, home, menu", initiallyOpen = true) {
        SwitchRow(c, "Swipe sideways to change tab", ui.tabSwipe) { ui.tabSwipe = it }
        SwitchRow(c, "Pull up to open the tabs page", ui.pullToTabs) { ui.pullToTabs = it }
        SliderRow(c, "Pull distance to open", "${(ui.pullThreshold * 100).roundToInt()}%", ui.pullThreshold, 0.15f..0.6f, enabled = ui.pullToTabs) {
            ui.pullThreshold = it
        }
    }
    Section(c, "Search bar", "The address field", initiallyOpen = true) {
        SwitchRow(c, "Swipe sideways for page back / forward", ui.historySwipe) { ui.historySwipe = it }
    }
    Note(c, "Swipe right = back, swipe left = forward. After going back you can swipe forward again.")
    Section(c, "Feel", "Distance and feedback", initiallyOpen = true) {
        SliderRow(c, "Swipe distance per step", "${ui.swipeStep.roundToInt()} dp", ui.swipeStep, 36f..100f) { ui.swipeStep = it }
        SwitchRow(c, "Haptic feedback", ui.haptics) { ui.haptics = it }
    }
    Section(c, "Back button", "System back", initiallyOpen = true) {
        SwitchRow(c, "Back closes a tab opened from another tab", ui.backClosesOpener) { ui.backClosesOpener = it }
    }
    Note(c, "Back goes: close what is open, then page history, then the tab it came from, then the Homepage.")
    ActionButton(c, "Reset gestures") { ui.resetGestures() }
}

@Composable
private fun BrowserSettings(state: VoidAppState, c: ProtoColors) {
    val ui = state.ui
    Section(c, "Pages", initiallyOpen = true) {
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
    Section(c, "Screen", initiallyOpen = true) {
        SwitchRow(c, "Keep the screen on", ui.keepAwake) {
            ui.keepAwake = it
            state.applyKeepAwake()
        }
    }
    Note(c, "Turn images off to browse text only; reload the page to see it.")
}

@Composable
private fun SearchSettings(state: VoidAppState, c: ProtoColors) {
    Section(c, "Search engine", initiallyOpen = true) {
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
    Section(c, "Suggestions", initiallyOpen = true) {
        SwitchRow(c, "Show search suggestions", state.suggestionsEnabled) { state.updateSuggestionsEnabled(it) }
    }
}

@Composable
private fun DownloadSettings(state: VoidAppState, c: ProtoColors) {
    val ui = state.ui
    Section(c, "Before a download", initiallyOpen = true) {
        SwitchRow(c, "Ask before downloading", ui.askDownload) { ui.askDownload = it }
    }
    Note(c, "On: a box shows the file name (you can rename it) and where it will be saved.")

    Section(c, "Where files go", initiallyOpen = true) {
        TextRow(c, "Folder name", ui.downloadFolder, "VOID") {
            ui.downloadFolder = it.filter { ch -> ch.isLetterOrDigit() || ch == ' ' || ch == '_' || ch == '-' }
        }
    }
    Note(c, "Saved to ${state.downloads.displayPath(ui.downloadFolder.ifBlank { "VOID" })}")

    Section(c, "Manager", initiallyOpen = true) {
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
    Section(c, "Blocking", initiallyOpen = true) {
        SwitchRow(c, "Block ads & trackers", state.blockAds) { state.updateBlockAds(it) }
        SwitchRow(c, "Block pop-ups", ui.blockPopups) { ui.blockPopups = it }
    }
    Section(c, "Cookies", initiallyOpen = true) {
        SwitchRow(c, "Allow cookies", ui.cookies) {
            ui.cookies = it
            state.applyWebSettings()
        }
        SwitchRow(c, "Allow third-party cookies", ui.thirdPartyCookies, enabled = ui.cookies) {
            ui.thirdPartyCookies = it
            state.applyWebSettings()
        }
    }
    Section(c, "Security", initiallyOpen = true) {
        SwitchRow(c, "Safe browsing", ui.safeBrowsing) {
            ui.safeBrowsing = it
            state.applyWebSettings()
        }
        SwitchRow(c, "HTTPS only (upgrade http links)", ui.httpsOnly) { ui.httpsOnly = it }
    }
    Section(c, "Clear", initiallyOpen = true) {
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
    Section(c, "Pages", initiallyOpen = true) {
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
    Section(c, "Reset", initiallyOpen = true) {
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
