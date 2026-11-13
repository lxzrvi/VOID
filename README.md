# VOID

Ek blank page. Bas ek circle, bottom-right corner me. Tap karo — woh smoothly
poori width tak grow hota hai aur ek search island ban jaata hai.

Built with Jetpack Compose, AGP 9 (built-in Kotlin) and
[Material icons](https://github.com/thelacspace/lucide-compose).

---

## The interaction

| State | What you see |
| --- | --- |
| Rest | Blank page + one 56dp red circle in the bottom-right with the VOID inner mark |
| Tap the circle | The circle grows smoothly (spring) into a full-width island; colour cross-fades red → surface, shadow fades in |
| Grown (left) | Search field, flexible width — it fills whatever space the pill leaves |
| Grown (right) | Pill: `←` `1/9` `→` divider `⋯`. Arrows move the counter through the result set, `⋯` (inner icon slot) switches to `✕`, and `✕` closes the island |
| Close | Tap the blank page, press `✕`, or use the system back gesture |

Everything animates from a single `Animatable` progress value (0 = circle,
1 = island), so the width, colour, radius, shadow and content cross-fade are
always in sync. The grown content is laid out at its final width and revealed by
the widening clip, so the pill stays pinned to the right edge and nothing
re-flows mid-animation.

## Install

**[Download the latest APK](https://github.com/lxzrvi/VOID/releases/latest)** →
`VOID-<version>.apk`, open it on the device and allow "install unknown apps" for
the browser / file manager. Latest: **0.1.4** (`dev.voidapp`, minSdk 26, ~1.3 MB).

Every push to `main` publishes a new release automatically, so there is nothing
to build locally.

## Icons

* `icon/void_icon.svg` — full mark (red tile + white V). Used for the launcher
  icon (`adaptive-icon` background = brand red, foreground = the mark).
* `icon/void_inner_icon.svg` — mark only, white. Copied into
  `app/src/main/res/drawable/void_inner_icon.xml` and tinted, used inside the
  resting circle.
* The `c2pa` metadata blob and the oversized `width`/`height` were stripped from
  both files, so they are ~1.9 KB each instead of ~9.7 KB.
* Android cannot draw raw SVG. `tools/make_icons.py` converts both SVGs into real
  VectorDrawables (`<vector android:viewportWidth=…>`) in `res/drawable`, and
  `tools/check_icons.py` (run in CI before every Gradle build) fails the build if
  a drawable is a raw `<svg>`, has no viewport, or draws nothing. Shipping raw SVG
  in 0.1.0 is what crashed on launch with
  `<VectorGraphic> tag requires viewportWidth > 0`.
* UI glyphs (arrows, three dots, close) are Material icons. Their paths are
  inlined in `ui/components/Material.kt` (copied 1:1 from the
  [lucide-compose](https://github.com/thelacspace/lucide-compose) sources) and
  drawn with a 2.5f stroke instead of Material's default 2f — that is the "bold"
  look in the pill.
  Why not the Maven artifact: `io.github.thelacspace:lucide-compose:1.16.0` on
  Maven Central only ships the generator's test icons (Triangle,
  TriangleAlert, TriangleDashed, Trophy) — the actual icon set is missing, so
  `MaterialIcons.All.ArrowLeft` does not exist at compile time. Swap the `by lazy`
  bodies in `Material.kt` for the library's composable properties once a full
  release is published; call sites don't change.

## Project layout

```
app/                          single application module (package dev.voidapp)
  src/main/kotlin/dev/voidapp/
    MainActivity.kt           edge-to-edge host
    ui/VoidScreen.kt          the one screen: blank surface + island
    ui/components/VoidIsland.kt   the circle → island animation, search field, pill
    ui/components/Material.kt       inlined Material glyphs (arrows, ellipsis, x)
    ui/theme/Theme.kt         brand red + light/dark schemes (follows the system)
  src/main/res/
    drawable/void_icon.xml            launcher art (full mark) - generated
    drawable/void_inner_icon.xml      inner mark - generated
    drawable/ic_launcher_foreground.xml  adaptive foreground - generated
    mipmap-anydpi-v26/ic_launcher*.xml
build-logic/                  convention plugins (void.android.application / .compose)
gradle/libs.versions.toml     every version, including the app version
icon/                         original SVG sources
```

## Versions

| Tool | Version |
| --- | --- |
| Gradle | 9.7.1 |
| AGP | 9.5.1 (`com.android.application`, built-in Kotlin) |
| Kotlin | 2.3.21 |
| Compose BOM | 2026.09.00 |
| compileSdk / targetSdk | 37 |
| minSdk | 26 |
| Material glyphs | inlined from lucide-compose 1.16.0 |

AGP 9 compiles Kotlin itself and pins that compiler to the KGP version it ships
with. The root `build.gradle.kts` puts the project's KGP on the build classpath
(without applying `kotlin-android` anywhere) so built-in Kotlin — and therefore
the Compose compiler plugin — runs on Kotlin 2.3.21.

## Releasing

`voidVersionName` / `voidVersionCode` in `gradle/libs.versions.toml` are the
single source of truth; the workflow reads them, tags `v<name>` and attaches the
APK to a GitHub Release.

The APK is signed with the debug keystore so it installs out of the box. To sign
with your own key instead, add the keystore as repository secrets and extend the
`release { }` block in `VoidAndroidApplicationPlugin` — that is the only place
that needs to change.

## Local development (optional)

```bash
./gradlew :app:installDebug     # build + install on a connected device
./gradlew :app:assembleRelease  # release APK
```

JDK 17 and Android SDK platform 37 are required.


---

## 0.4.0 - browser, tabs, icons

* **Status bar:** pages and Home are edge to edge. Content starts *below* the
  status bar and scrolls *under* it; a scrim tinted with the page's own colour
  softens the edge and the icons flip light/dark to match. No black bar, no
  navigation-bar scrim.
* **Circle:** long-press then drag moves it freely (x and y, 1:1 with the finger)
  and it springs to the nearest edge. Pull it up and it follows the finger while
  the current page shrinks into its card; release past the threshold (or flick)
  to open the tab grid, otherwise it springs back.
* **Tabs:** one WebView per tab (no reload on switch), Home is the first card,
  live card for the current page, snapshots for the rest, long-press to
  rearrange, tabs are restored on the next launch.
* **Icons:** shortcut and search-provider icons are cached on disk and decoded
  before the first frame; an icon that is already stored is never fetched again.
* **Boxes:** shortcuts and tools use a reorder grid whose last row stretches, so
  there are no empty cells; long-press drag rearranges them.
* **Browser:** find in page, downloads, file upload, camera / mic / location
  prompts, fullscreen video, JS dialogs, pop-ups open as tabs, external app
  links, print / save as PDF, translate, ad & tracker blocking, dark web pages,
  live search suggestions, full history & bookmarks screen, proper back
  navigation, per-tab desktop mode.


---

## 0.5.0 - the prototype UI

The main page and all behaviour follow the HTML prototype:

* **Homepage:** white page, nine shortcuts at 17 % height, the light `VOID`
  wordmark at 31 % (tap it for the squeeze animation), the search pill at 50 %.
  Focusing the search pill dims the shortcuts and wordmark.
* **Tabs:** every tab is either a Homepage or a page. New tab = Homepage tab, the
  Home button turns the current tab back into a Homepage, closing the last tab
  leaves a fresh one. Tabs are listed as italic names with an x and a + button.
* **Bottom bar:** home/search pill plus the five-icon nav pill. The menu button
  grows the pill upward into the paged 3 x 4 tools box (long press on a tool
  shows its name); the tabs box rises above the home pill.
* **Scrolling a page** folds the nav pill into a dot and hides the tab row;
  tapping the dot unfolds it again.
* "Tabs overview" opens the card grid (page shrinks into its card).


---

## 0.6.0 - voidupdate1

* **Home:** one row of boxes (9 by default), each showing only the app's first
  capital letter - no icons or images, no add button. Boxes per row, box colour,
  letter colour, transparency and corner radius live in Settings.
* **Tabs:** every tab is a pill (`Homepage x`); long press opens the tab's option
  box next to it (reload, duplicate, copy link, share, close others, close).
* **Bottom bar:** the search field shows the current URL, fully selected while
  editing; it takes URLs and searches. It only rides up with the keyboard when it
  is the field being typed in. The nav pill has four icons (back, forward, home,
  hamburger); the hamburger opens 36 tools on 3 pages of 4 rows. Find in page
  also opens at the bottom.
* **Scrolling a page** folds the search bar and the nav pill into one circle with
  the VOID mark (`void_inner_icon`); tap it to unfold. No fade over the bottom.
* **Settings** is a full screen page: home boxes (add a website, add a bookmark,
  reorder, remove), browser switches, theme, data.
* **Icons** come from Material Icons (filled); nothing is hand-drawn any more.


---

## 0.7.0 - voidupdate2

* **No tab row.** Tabs live in the hamburger's tabs box (long press a tab for its option
  box), on the pill (swipe it left / right to change tab, the pill moves with you), and in
  the tabs page (pull the pill up: the current page shrinks into its card).
* **Status bar** wears the colour at the top of the site and scrolls away with the page, so
  the site is shown full screen. The load line is at the bottom of the screen.
* **Scrolling** folds the search bar and the nav pill into two separate circles; the search
  circle grows only the search bar, the VOID circle grows only the pill.
* **Hamburger:** the name sits under every icon; 48 tools on 4 pages; Settings > Hamburger
  menu moves, removes and adds them.
* **Downloads** are built in: a confirmation box (rename, shows the folder), a downloads
  page with pause / resume / cancel / retry / open / delete and pause all / resume all.
* **Settings** is a list of groups, each with its own page: Home page, Appearance,
  Hamburger menu, Browser, Search, Downloads, Privacy & security, Advanced, About.
* Appearance and Home page customise colours, bars, text size, wordmark, positions and
  the search pill.


---

## 0.8.0 - voidupdate4

* **Motion:** every transition is a spring (Settings > Motion & blur: Bouncy / Smooth / Off,
  speed). Overlays slide up, settings pages slide sideways, the pill, circles and boxes
  grow with a wobble, home boxes pop in one by one.
* **Live blur:** bars, circles and the tabs box blur the page behind them with Haze
  (Settings > Motion & blur). Needs Android 12+; older phones get the translucent bar.
* **Circles:** the search circle and the VOID circle now share one background.
* **Status bar:** by default every page starts *below* the status bar, so headers (YouTube,
  GitHub...) never slide under it. "Scrolls away" is still available in Appearance.
* **Back:** closes what is open, then page history, then returns to the tab it was opened
  from (that tab closes), then the Homepage; on a Homepage opened from another tab it
  returns there.
* **Swipes:** nav pill = tab left/right (or pull up for the tabs page); search bar = page
  back (swipe right) / forward (swipe left), forward again after a back. All tunable in
  Settings > Gestures.
* **Tabs page:** the live page glides into its card and hands over to it - no leftover layer.
* **Downloads:** long press a download for open / open with / share / rename / copy link /
  properties / download again / remove / delete; tap a finished file to open it. After
  confirming a download the file's format icon flies into the bottom circle, the circle's
  border becomes the combined progress ring, and on completion the icon morphs into a tick
  and back.
* **Settings:** every page is made of sections that fold open with a spring; new pages for
  Motion & blur and Gestures.


---

## 0.8.1 - voidupdate5

* **Crash fix:** a bouncy spring overshoots below zero when it settles, and two paddings in
  the bottom bar (the lift of the circles, the gap under the nav icons) were fed straight
  from such springs - "Padding must be non-negative". Paddings are now clamped; sizes may
  still wobble.
* **Homepage gradient:** the VOID-red glow is back at the top of the Homepage. Settings >
  Home page > Background: on/off, presets, direction (down / up / diagonal / glow), reach,
  strength, gradient colour, fade-to colour and page colour.
