# VOID

Ek blank page. Bas ek circle, bottom-right corner me. Tap karo — woh smoothly
poori width tak grow hota hai aur ek search island ban jaata hai.

Built with Jetpack Compose, AGP 9 (built-in Kotlin) and
[Lucide icons](https://github.com/thelacspace/lucide-compose).

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
the browser / file manager. Latest: **0.1.1** (`dev.voidapp`, minSdk 26, ~1.3 MB).

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
* UI glyphs (arrows, three dots, close) are Lucide icons. Their paths are
  inlined in `ui/components/Lucide.kt` (copied 1:1 from the
  [lucide-compose](https://github.com/thelacspace/lucide-compose) sources) and
  drawn with a 2.5f stroke instead of Lucide's default 2f — that is the "bold"
  look in the pill.
  Why not the Maven artifact: `io.github.thelacspace:lucide-compose:1.16.0` on
  Maven Central only ships the generator's test icons (Triangle,
  TriangleAlert, TriangleDashed, Trophy) — the actual icon set is missing, so
  `LucideIcons.All.ArrowLeft` does not exist at compile time. Swap the `by lazy`
  bodies in `Lucide.kt` for the library's composable properties once a full
  release is published; call sites don't change.

## Project layout

```
app/                          single application module (package dev.voidapp)
  src/main/kotlin/dev/voidapp/
    MainActivity.kt           edge-to-edge host
    ui/VoidScreen.kt          the one screen: blank surface + island
    ui/components/VoidIsland.kt   the circle → island animation, search field, pill
    ui/components/Lucide.kt       inlined Lucide glyphs (arrows, ellipsis, x)
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
| Lucide glyphs | inlined from lucide-compose 1.16.0 |

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
