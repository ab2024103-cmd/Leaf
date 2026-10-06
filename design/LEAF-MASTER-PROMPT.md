# 🌿 LEAF — MASTER BUILD PROMPT
### Native Android · Kotlin · Jetpack Compose · Material 3 · API 23 → 36

> **How to use this file:** paste this entire document as the first message to your AI coding agent
> (Claude Code / Cursor / Codex / Gemini CLI). Then paste `LEAF-KICKOFF-PROMPT.md` to start Milestone 1.
> Keep `LEAF-SPEC.md`, `leaf-mockup-v2.html` and `design/leaf_tokens.json` in the repo — this prompt
> refers to them as the source of truth. Work through `LEAF-BUILD-CHECKLIST.md` milestone by milestone.

---

## 0 · YOUR ROLE AND MISSION

You are a senior Android engineer and product designer building **Leaf**, an offline-first document
viewer, from a finished, interactive HTML mockup. Your job is to ship the **real app**, not a sketch:
**100 % of the look, 100 % of the behaviour** described below.

- The interactive mockup `leaf-mockup-v2.html` is the **visual and behavioural contract**. Open it,
  read its CSS and JS, and match it. Where the mockup and this document disagree, this document wins;
  where this document is silent, the mockup wins.
- `LEAF-SPEC.md` is the product specification. `design/leaf_tokens.json` holds the generated Material 3
  token set. Match the tokens exactly — no "close enough" colours, radii or type sizes.
- **Never drop a feature.** The coverage matrix in §14 is the definition of "done". It is not acceptable
  to stub, hide, TODO or silently omit any row. If something is genuinely blocked, say so explicitly
  and propose the closest working alternative — do not quietly skip it.

### Definition of done (per feature)
1. It works on API 23 **and** on the latest API, on a 360 dp phone **and** a tablet.
2. It survives process death (state is persisted, not held in memory only).
3. It looks like the mockup: same colours, spacing, radii, type, motion and copy.
4. It has no `TODO`, no `FIXME`, no `println` debugging, no dead buttons.
5. It is covered by a unit test (logic) or a UI/screenshot test (visual), per §12.

---

## 1 · NON-NEGOTIABLES

| # | Rule |
|---|------|
| 1 | **Language & UI:** Kotlin only. UI in **Jetpack Compose** with **Material 3**. No XML layouts except `themes.xml` for the splash/window background. |
| 2 | **Support range:** `minSdk = 23` (Android 6.0) · `targetSdk = 36` · `compileSdk = 36`. Google Play requires API 36 for new apps and updates from 31 Aug 2026 — do not lower `targetSdk`. |
| 3 | **API 23 safety:** every API above 23 must be guarded (`Build.VERSION.SDK_INT`) or handled by an androidx compat API. The app must never crash with `NoSuchMethodError` / `NoClassDefFoundError` on API 23. |
| 4 | **No dynamic colour by default.** Material You dynamic colour would destroy brand parity. Ship the Leaf Green scheme below as the default; expose dynamic colour only as an optional "Match system colours (Android 12+)" switch. |
| 5 | **Offline-first.** No network permission in the manifest for the core product. No accounts, no analytics SDKs. |
| 6 | **Two independent theme axes:** *App theme* (light/dark/system) for chrome, and *Reading theme* (paper/sepia/night/oled/auto) for the document canvas. Never let one drive the other. |
| 7 | **Persistence is mandatory.** Library, folders, tags, collections, favorites, highlights, bookmarks, recents, reading positions, open tabs and settings all survive process death and reboot. |
| 8 | **Responsive by width class, not by device type.** Bottom navigation on compact, navigation rail on medium/expanded. Nothing may look stretched on a 900 dp tablet or cramped on a 360 dp phone. |
| 9 | **Every render is verified by running it.** After each milestone: build, install on the API 23 emulator *and* a current-API device/emulator, take screenshots, compare against the mockup, fix discrepancies. Do not report success without a green build you produced yourself. |
| 10 | **Copy is part of the design.** Use the exact strings in §11. Put them in `strings.xml` — no hard-coded text in composables. |

---

## 2 · TOOLCHAIN & DEPENDENCIES

Use current stable versions; pin them in `gradle/libs.versions.toml`.

```toml
[versions]
agp = "8.13.0"              # AGP requires JDK 17
kotlin = "2.1.20"
ksp = "2.1.20-1.0.32"
composeBom = "2025.09.00"   # any current BOM
coreKtx = "1.15.0"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
navigationCompose = "2.9.0"
room = "2.7.0"
datastore = "1.1.1"
coil = "2.7.0"              # image loading (thumbnails, page previews)
koin = "4.0.0"              # or Hilt 2.52+ — pick one, stay consistent
readium = "3.1.0"           # EPUB engine (minSdk 21, needs core library desugaring below API 26)
pdfboxAndroid = "2.0.27.0"  # text extraction for API < 35 (optional — see §7.4)
turbine = "1.2.0"
bom = "2025.09.00"
```

```kotlin
android {
    namespace = "app.leaf.reader"
    compileSdk = 36
    defaultConfig {
        applicationId = "app.leaf.reader"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables.useSupportLibrary = true      // required on API < 24 for some vectors
        resourceConfigurations += listOf("en","es","fr","de","pt","ar","yo","hi","ja")
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true         // Readium needs it below API 26; harmless elsewhere
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(17) }
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
```

**Emulator/device matrix you must test on before calling the project done:**
API 23 (phone, 360×640), API 29 (360×800), API 36 (412×915), API 36 tablet (800×1280, medium/expanded),
one foldable profile. Use `adb shell setprop debug.layout` / Compose layout inspector only for debugging.

**Manifest essentials for API 23 + edge-to-edge:**
```xml
<uses-permission android:name="android.permission.VIBRATE"/><!-- haptics -->
<application
    android:largeHeap="false"
    android:hardwareAccelerated="true"
    android:requestLegacyExternalStorage="false"
    android:enableOnBackInvokedCallback="true">   <!-- predictive back where available -->
    <activity android:name=".MainActivity"
        android:exported="true"
        android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|smallestScreenSize|uiMode|density"
        android:launchMode="singleTask">
        <intent-filter>
            <action android:name="android.intent.action.MAIN"/>
            <category android:name="android.intent.category.LAUNCHER"/>
        </intent-filter>
        <!-- document intake: open-with from other apps (no Import screen exists in this product) -->
        <intent-filter>
            <action android:name="android.intent.action.VIEW"/>
            <category android:name="android.intent.category.DEFAULT"/>
            <data android:mimeType="application/pdf"/>
            <data android:mimeType="text/plain"/>
            <data android:mimeType="application/epub+zip"/>
            <data android:mimeType="application/vnd.openxmlformats-officedocument.wordprocessingml.document"/>
        </intent-filter>
        <intent-filter>
            <action android:name="android.intent.action.SEND"/>
            <category android:name="android.intent.category.DEFAULT"/>
            <data android:mimeType="application/pdf"/>
            <data android:mimeType="text/plain"/>
            <data android:mimeType="application/epub+zip"/>
        </intent-filter>
    </activity>
</application>
```
`configChanges` is set because rotation and resizing are handled in-app (per-document orientation lock,
free-form resizing); do not let the framework recreate the reader mid-read.

> Documents enter the library through the system **file picker (`ACTION_OPEN_DOCUMENT`)** and the
> **share/open-with** intents above, then are persisted with `takePersistableUriPermission`.
> There is deliberately **no Import screen** — it was removed from scope.

---

## 3 · PROJECT STRUCTURE

Single Gradle module at first; keep the package boundaries clean so it can be split later.

```
app/src/main/java/app/leaf/reader/
├── LeafApp.kt                      // Application, DI graph, WorkManager-free
├── MainActivity.kt                 // single activity, edge-to-edge, back handling
├── core/
│   ├── model/                      // Document, Folder, Tag, SmartCollection, RecentEntry,
│   │                               // Highlight, NormalizedRect, Bookmark, Progress, ReaderTab, Settings
│   ├── data/db/                    // Room: LeafDatabase, DAOs, entities, converters
│   ├── data/prefs/                 // DataStore: settings, nav state, open tabs
│   ├── data/repo/                  // DocumentRepository, FolderRepository, TagRepository,
│   │                               // HighlightRepository, RecentRepository, SearchRepository
│   ├── format/                     // DocumentEngine: PdfEngine, DocxEngine, TxtEngine, EpubEngine
│   ├── search/                     // LibrarySearch, InFileSearch, SearchHistory
│   └── ui/                         // theme/, components/, util/ (Modifier.leafRipple, Haptics, Insets)
├── feature/
│   ├── library/  recents/  favorites/  search/  settings/
│   ├── reader/                     // ReaderScreen, ReaderViewModel, TabStrip, FindBar, Toolbar,
│   │                               // PageCanvas, HighlightLayer, ZoomState, ReflowView
│   └── sheets/                     // ActionsSheet, SortSheet, JumpSheet, ViewLayoutSheet, ShareSheet,
│                                   // FolderManager, TagManager, HighlightsSheet, BookmarksSheet, InfoSheet
└── res/
    ├── values/strings.xml, themes.xml, dimens.xml
    ├── values-night/themes.xml
    └── font/  (Noto Serif — bundled, OFL)
```

Architecture: **MVVM + repository**, `StateFlow` UI state per screen, one-shot events via `Channel`.
Compose navigation with a **single top-level graph** (5 destinations) and the reader as a full-screen
route that overlays the shell. Prefer `NavHost` for top level and a plain `AnimatedVisibility` overlay
for the reader so each open document keeps its own state (the tab list itself lives in a repository-backed
`StateFlow`, not in a composable).

---

## 4 · DESIGN TOKENS — COPY THESE EXACTLY

### 4.1 Colour roles (Material 3,TonalSpot scheme generated from seed `#2B7A5B`)

```kotlin
// core/ui/theme/Color.kt
private val LeafGreen = Color(0xFF2B7A5B)   // brand seed — also the app icon base

val LeafLight = lightColorScheme(
    primary              = Color(0xFF206A4E), onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFFA8F2CE), onPrimaryContainer   = Color(0xFF002114),
    inversePrimary       = Color(0xFF8DD5B3),
    secondary            = Color(0xFF4D6357), onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFCFE9D9), onSecondaryContainer = Color(0xFF0A1F16),
    tertiary             = Color(0xFF3D6373), onTertiary           = Color(0xFFFFFFFF),
    tertiaryContainer    = Color(0xFFC1E9FB), onTertiaryContainer  = Color(0xFF001F29),
    error                = Color(0xFFBA1A1A), onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFFFFDAD6), onErrorContainer     = Color(0xFF410002),
    background           = Color(0xFFF5FBF5), onBackground         = Color(0xFF171D1A),
    surface              = Color(0xFFF5FBF5), onSurface            = Color(0xFF171D1A),
    surfaceVariant       = Color(0xFFDBE5DD), onSurfaceVariant     = Color(0xFF404943),
    surfaceDim           = Color(0xFFD6DBD6), surfaceBright        = Color(0xFFF5FBF5),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow  = Color(0xFFEFF5EF),
    surfaceContainer       = Color(0xFFEAEFE9), surfaceContainerHigh = Color(0xFFE4EAE4),
    surfaceContainerHighest= Color(0xFFDEE4DE),
    outline              = Color(0xFF707973), outlineVariant       = Color(0xFFBFC9C2),
    inverseSurface       = Color(0xFF2C322E), inverseOnSurface     = Color(0xFFEDF2EC),
    scrim                = Color(0xFF000000)
)

val LeafDark = darkColorScheme(
    primary              = Color(0xFF8DD5B3), onPrimary            = Color(0xFF003826),
    primaryContainer     = Color(0xFF005138), onPrimaryContainer   = Color(0xFFA8F2CE),
    inversePrimary       = Color(0xFF206A4E),
    secondary            = Color(0xFFB3CCBE), onSecondary          = Color(0xFF1F352A),
    secondaryContainer   = Color(0xFF354B40), onSecondaryContainer = Color(0xFFCFE9D9),
    tertiary             = Color(0xFFA5CCDF), onTertiary           = Color(0xFF073543),
    tertiaryContainer    = Color(0xFF244C5B), onTertiaryContainer  = Color(0xFFC1E9FB),
    error                = Color(0xFFFFB4AB), onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A), onErrorContainer     = Color(0xFFFFDAD6),
    background           = Color(0xFF0F1511), onBackground         = Color(0xFFDEE4DE),
    surface              = Color(0xFF0F1511), onSurface            = Color(0xFFDEE4DE),
    surfaceVariant       = Color(0xFF404943), onSurfaceVariant     = Color(0xFFBFC9C2),
    surfaceDim           = Color(0xFF0F1511), surfaceBright        = Color(0xFF353B37),
    surfaceContainerLowest = Color(0xFF0A0F0C), surfaceContainerLow  = Color(0xFF171D1A),
    surfaceContainer       = Color(0xFF1B211D), surfaceContainerHigh = Color(0xFF252B28),
    surfaceContainerHighest= Color(0xFF303632),
    outline              = Color(0xFF8A938C), outlineVariant       = Color(0xFF404943),
    inverseSurface       = Color(0xFFDEE4DE), inverseOnSurface     = Color(0xFF2C322E),
    scrim                = Color(0xFF000000)
)
```

### 4.2 Reading-theme tokens (document canvas only)

```kotlin
enum class ReadingTheme { PAPER, SEPIA, NIGHT, OLED, AUTO }

data class ReadingPalette(
    val surface: Color, val surfaceAlt: Color, val text: Color,
    val textMuted: Color, val rule: Color, val link: Color, val isDark: Boolean
)

val Paper = ReadingPalette(Color(0xFFFFFFFF), Color(0xFFF7F5EF), Color(0xFF1B241F),
                           Color(0xFF5C6A63), Color(0xFFE7E4DB), Color(0xFF206A4E), false)
val Sepia = ReadingPalette(Color(0xFFF4ECD8), Color(0xFFEFE4C9), Color(0xFF43382A),
                           Color(0xFF6D5D47), Color(0xFFE2D5BA), Color(0xFF8A5A2B), false)
val Night = ReadingPalette(Color(0xFF12171A), Color(0xFF1A2124), Color(0xFFDDE5E0),
                           Color(0xFF94A29B), Color(0xFF252D31), Color(0xFF8DD5B3), true)
val Oled  = ReadingPalette(Color(0xFF000000), Color(0xFF0C0F10), Color(0xFFCFD6D1),
                           Color(0xFF8B9791), Color(0xFF1C2124), Color(0xFF8DD5B3), true)
// AUTO resolves to Night when the system is in dark mode, else Paper — at render time, not at save time.
```

### 4.3 Highlight colours (tone-matched per reading theme)

| Colour | Light-canvas fill | Dark-canvas fill | Active ring |
|---|---|---|---|
| Yellow | `#F2CD55` @ 55 % | `#A08019` @ 65 % | `primary` 2 dp + 24 % halo |
| Green  | `#82D199` @ 50 % | `#407E52` @ 62 % | 〃 |
| Blue   | `#82B8F0` @ 50 % | `#3A6896` @ 62 % | 〃 |
| Pink   | `#F099C0` @ 50 % | `#96486C` @ 62 % | 〃 |
| Orange | `#F5B478` @ 55 % | `#9E6834` @ 62 % | 〃 |

Find-in-file matches use the Yellow swatch at 60 % with an amber outline; the *current* match is solid
`#FF8A3C` with white text. Never reuse highlight colours for search marks.

### 4.4 Type scale

| Role | Size / line | Weight | Family |
|---|---|---|---|
| Screen title (app bar) | 22 sp / 28 | 600 | UI sans (Roboto/system) |
| Section label ("8 DOCUMENTS") | 13 sp / 16 | 700 · tracking 0.6 · UPPERCASE | UI |
| List item title | 14.5 sp / 19 | 600 | UI |
| List item meta | 11 sp / 15 | 400 | UI |
| Tag chip label | 10 sp / 13 | 700 | UI |
| Body (sheets, settings) | 14 sp / 20 | 400–600 | UI |
| Supporting text | 11.5 sp / 16 | 400 | UI |
| **Reading body** | 17 sp / 1.75 (≈29.75 sp) at 100 % zoom | 400 | **Noto Serif** (bundled) |
| Reading body (reflow) | 18 sp / 1.85 | 400 | Noto Serif |
| Reading heading inside page | 1.25× body, 600 | | Noto Serif |
| Reading sans option | same sizes | 400 | UI sans, line-height 1.7 |
| Page label ("PAGE 4 OF 7") | 10 sp / 13 | 800 · tracking 1.4 · UPPERCASE | UI |

Fonts: bundle **Noto Serif Regular + SemiBold** (OFL, add the licence to `res/raw`). Offer Serif/Sans in
Settings. Reading text must scale with `zoom` (70 %–250 %) *and* with the system font scale.

### 4.5 Shape, spacing, elevation, motion

```kotlin
object LeafShape {                     object LeafSpacing {
  val xs = 4.dp;   val s = 8.dp           val screenH = 16.dp      // screen side padding
  val m  = 12.dp;  val l = 16.dp          val listGap = 6.dp       // gap between cards
  val xl = 20.dp;  val xxl = 28.dp        val cardPad = 12.dp
  val full = 999.dp                       val control = 40.dp      // icon button
}                                        // touch target never below 48.dp
```
- Cards / list rows: **16 dp** radius. Document tiles: **16 dp**. Sheets: **28 dp** top corners.
  Chips: 8 dp (filter chips) / full (pill buttons, chips with icons). Buttons: full. Dialog: 28 dp.
  Search bar: full (height 52 dp, `surfaceContainerHigh`). Nav-bar active pill: 62×30 dp, full.
- Elevation: level 0 flat for list content, **1** for raised cards, **2** for the "Jump back in" hero,
  **3** for sheets/dialogs/FAB. On API 23–27 prefer `Modifier.shadow` + tonal surface over real elevation
  to avoid grey smudges — never use `Modifier.blur` (API 31+).
- Motion (respect "Animations" setting and `Settings.Global.ANIMATOR_DURATION_SCALE`):
  screen switch **250 ms** shared-axis (fade + 14 dp slide), sheet **250 ms** emphasized
  `cubic-bezier(0.05,0.7,0.1,1)`, page turn **250 ms**, ripples **450 ms**, snackbar **250 ms**.
  Long-press delay **470 ms** with a 0.97 press scale; haptic 18 ms on long-press, 8–10 ms on highlight/bookmark.

### 4.6 Component metrics (match the mockup 1:1)

| Component | Spec |
|---|---|
| Icon button | 40 dp, full radius, transparent; hover/active = `onSurface` 8 % / 12 % overlay |
| Document row | 16 dp radius card, `surfaceContainerLow`, 11 dp vertical padding, 12 dp icon gap, icon 40×48 dp |
| File icon | tonal container per type: PDF `#FFDAD6`/`#93000A` · DOCX `#C1E9FB`/`#0F4A5C` · **XLSX `#CDE7C4`/`#2C4A22`** · **PPTX `#FFD9C2`/`#7A3A15`** · TXT `#E0E6E1`/`#3C4640` · EPUB `#A8F2CE`/`#005138` (dark theme: containers at 30 % tone, labels at 90 %); 9 sp/800 label (`PDF`, `DOCX`, `XLSX`, `PPTX`, `TXT`, `EPUB`); dog-ear corner 11 dp |
| Progress bar | 4 dp track `surfaceContainerHighest`, fill `primary`, full radius |
| Hero card | 28 dp radius, gradient `primary → primary 72 % + tertiary`, 18 dp padding, white circular Resume button |
| Chips | height 32 dp (`mini` 28 dp), 8 dp radius, 1 dp `outlineVariant`; active = `secondaryContainer` |
| Segmented control | full radius, 1 dp `outline`, active segment `secondaryContainer`, 7×14 dp padding |
| Switch | M3 switch, 52×32 dp, 24 dp thumb when on |
| Tab (reader) | 34 dp tall, top corners 8 dp, active = `surface` + outline, name max 104 dp ellipsised, × 19 dp |
| Snackbar | inverse surface, 4 dp radius, 16 dp side margins, sits **above the navigation bar** (bar height + 14 dp), 2 200 ms (4 200 ms with Undo) |
| Sheet | bottom, 28 dp top corners, grabber 32×4 dp at 40 % opacity, max 82 % height, scrim `#000` 42 % |
| Dialog | 28 dp radius, max width 340 dp, left-aligned title 17 sp/600, actions right-aligned |

---

## 5 · RESPONSIVE LAYOUT CONTRACT

Use `WindowSizeClass` (Material 3 window size classes) — never `Configuration.screenWidthDp` branches
scattered in composables.

| Width class | Threshold | Navigation | Content |
|---|---|---|---|
| Compact | < 600 dp | `NavigationBar`, 5 items, icon + 11.5 sp label, active pill 62×30 dp | 1 column, 16 dp side padding |
| Medium | 600–839 dp | `NavigationRail` (84 dp) with icon + label, active pill | 2-column card grid (`LazyVerticalGrid`, min 150 dp tiles) |
| Expanded | ≥ 840 dp | `NavigationRail` | 2–3 columns; reader text column max 720 dp, centred |

Additional layout rules:
- **Insets:** apply `WindowInsets.safeDrawing`; the reader's toolbar must sit above the gesture/navigation
  bar; the status bar stays visible in the shell but the reader is immersive-capable (hide system bars in
  Full screen mode, restore on exit).
- **The Library has three list states** — flat list · file-type overview · one open file-type group (§6.1).
  The header toggle switches the overview on and off; an open group is left with back, Esc or its crumb arrow.
- **No in-reader tab strip.** Open documents live in an **Open documents** sheet reached from the tabs
  button (app bar of the shell **and** of the reader), which carries a live count badge. The reader itself
  is one document at a time — the sheet is the only tab UI, so the reader keeps its full height.
- **Landscape phone:** the reader goes to a wide measure (max 720 dp) and the toolbar shows the widest
  variant of the priority ladder below.
- **Reader toolbar priority ladder** (drop the lowest-priority group first when the row cannot fit; never
  let the row scroll horizontally):

  | Frame width | Highlight (label + swatch + caret) | undo/redo/find/view | zoom −/%/+ | highlights list | full screen + share |
  |---|---|---|---|---|---|
  | 360 dp | swatch + caret only (icon trigger, 56 dp) | shown | **hidden** (View & layout sheet + gestures only) | hidden | shown, pinned right |
  | 412 dp | swatch + caret only | shown | −/+ shown, `%` label hidden | hidden | shown, pinned right |
  | 480 dp | swatch + caret only | shown | −/%/+ shown | hidden | shown, pinned right |
  | ≥ 600 dp | full "Highlight" label | shown | −/%/+ shown | shown | shown, pinned right |

  The right-hand group (full screen, share) is always visible; hide the others instead. The toolbar never
  scrolls, so no control is ever half-off-screen.
- **Foldable:** use `WindowLayoutInfo` to keep content out of the hinge; when the posture is `FLAT` in
  table-top mode, put the page above the fold and the toolbar below it.
- **Free-form / desktop windows:** the shell must re-flow live (no reload), because the app declares
  `configChanges` and handles resize through Compose.
- **Per-document orientation:** store `auto | portrait | landscape` on the document; apply with
  `Activity.requestedOrientation` (API 23-safe) when the lock setting is on, otherwise follow the sensor.

---

## 6 · SCREEN SPECIFICATIONS

Every screen below lists the exact structure, states and behaviours.

### 6.1 Library (default destination)

**App bar** — leaf mark (26 dp `primary`) + "Leaf" 22 sp/600 + subtitle:
`All 10 documents` · or `Folder · Work` · or `Collection · All PDFs`. Actions, left to right:
**Open documents** (tabs glyph with a `primary`-filled count badge — hidden when the count is 0),
**Folders & collections** (folder icon), **Tags** (tag icon). App bar gains a `surfaceContainer` tint + top
hairline once the list scrolls past 4 dp (scroll-linked elevation).

**Open documents entry point** — the badge button opens the **Open documents** sheet (§6.7) from anywhere
in the shell; it is the single home of the multi-tab feature (there is no tab strip in the reader).

**Filter rails (3 rows, horizontally scrolling, 16 dp gutters, right-edge fade mask):**
1. Folder chips with live counts + `+ Folder`
2. Smart collections + `+ Collection`
3. Tag chips (colour dot + name) + `+ Tag` — multi-select, AND semantics

**Section header row:** "N DOCUMENTS" (13 sp/700/uppercase, plus `· 5 types` while the grouped view is on) ·
filter icon · **group-by-file-type toggle** (36 dp icon button, toggled state while the grouped view is
active) · sort chip showing the current sort ("Name · A–Z"). Tapping the sort chip opens the **Sort sheet**
with an Ascending/Descending segmented control and a list of fields with ✓ on the active field:
Name · Date added · Last opened · File size. **File type is not a sort field** — it is its own view (below).
The sort chip is **hidden while the file-type overview is showing** (there is nothing to order there) and
returns as soon as a group is open.

**File-type groups view** (the header toggle; see §8.13 for the migration rule) — two states in one screen:

1. **Overview** — one tappable card per file type present in the current filter context, in the fixed order
   **PDF → Word (DOCX) → Excel (XLSX) → PowerPoint (PPTX) → Plain text (TXT) → EPUB books**, as a single
   column on compact and a 2-column grid on medium/expanded:
   `[file icon 40×48]` · title = the type name (`PDF documents`, `Word · DOCX`, `Excel · XLSX`,
   `PowerPoint · PPTX`, `Plain text · TXT`, `EPUB books`) · second line = `5 documents · 24.10 MB ·
   2 highlights · 2 bookmarks` (highlight/bookmark parts appear only when non-zero) · trailing chevron.
   Card = `surfaceContainerLow`, 1 dp `outlineVariant`, 16 dp radius, 14 dp padding, ripple. Tapping a card
   enters its group.
2. **Group** — a filled **crumb bar** above the section header: back arrow (36 dp) · that type's 30×36 icon ·
   the type name (15 sp/750) · second line `5 documents · Name · A–Z` (count **and the live sort**). Below it
   the section header shows `N DOCUMENTS` with the sort chip available, and the list holds only that type's
   rows (§4.6 metrics) with every row behaviour intact (open, long-press actions, star, tags, progress).
   Sorting inside a group offers the full field list, persists to the whole library, and **stays inside the
   group**. Back arrow, system back or Esc returns to the overview; switching the group toggle off returns to
   the flat list. Filters compose in all three states — folders, collections, tags, advanced filters and
   search all apply to the overview counts and to the grouped list. Snackbars:
   `Grouped by file type · tap a group to see its files` / `Showing one list of every document` /
   `PDF documents · 5 documents · sorted by Name · A–Z` on entering a group.

**Document row** (see §4.6 metrics):
```
[file icon 40×48]  Name (1 line, ellipsised)
                   n pages · 2.40 MB · 1 bookmark · 3 d ago
                   [tag chips]
                   [progress 57% · p4]            [★ star button]
```
- Tap → open the document in the current tab. **Long-press (470 ms)** → actions sheet.
- **Pick-a-document mode** (started by the tabs button's "Open a new document…", by the `＋` in the actions
  sheet or by ⌘/Ctrl+T on desktop-class devices): a `primaryContainer` banner appears above the filter rails —
  bold "Choose a document" + "Opens in a new tab · 2 of 6 in use" + **Cancel**; every row gets a 2 dp dashed
  `outline` and a 40 dp `primaryContainer` `＋` circle (the star is suppressed), the FAB hides, and the list
  scrolls to the top. Tapping a row opens it **in a new tab** and leaves pick mode. Cancel, back or choosing
  a document exits the mode.
- Star toggles instantly with a "Added to favorites"/"Removed from favorites" snackbar.
- Progress row appears only when `0 < start% < 100` (i.e. started but not finished).
- **Empty state:** book icon in a `secondaryContainer` circle, "Nothing here yet", copy: "Change the folder,
  collection or tag filter — or clear your filters to see the whole library."

**FAB** — extended FAB, `primaryContainer`, label "New folder" (or a round `+` FAB in compact landscape).
The list must scroll clear of it (92 dp bottom padding).

**Folders & collections sheet** — rows for each folder (indented when nested) with document/subfolder counts
and inline **rename / recolour / delete** buttons; then "New folder" and "New subfolder" rows.
Delete confirms: "N document(s) will move to 'Not filed'. They are not deleted."

**Tag manager sheet** — every tag with usage count and inline rename / recolour / delete; rename cascades to
all documents; deleting confirms and never touches documents.

### 6.2 Recents

- Subtitle: "N documents opened recently".
- **Hero card ("JUMP BACK IN")** for the most recent document: title, `Page 2 of 5 · 45 min ago · 1 bookmark`,
  a white progress bar, then **Resume reading** (filled white pill with a play glyph) and **Actions**
  (ghost pill, opens the actions sheet).
- **Continue · last 5**: a 2-up (3-up on medium/expanded) grid of "cover" tiles — file-type chip, percentage
  top-right, name (2 lines), "page n of m", progress track. Tap → resume.
- **Today** group (each row shows a working ✕ that removes just that entry, with Undo) and **Earlier** group.
- Header actions: `Clear today` inside the Today group; trash icon in the app bar → confirmation
  ("Clear all recents? … Your documents, highlights and reading positions are kept.").
- Navigation item shows a badge with the count of entries from the last 24 h.
- Empty state: clock icon, "No recents yet", "Documents you open appear here automatically — tap one to jump
  straight back to the exact page you left."

### 6.3 Favorites

- Own sort control as a segmented row under the app bar: **Name · Date added · Last opened**
  (persisted independently of the library sort).
- Same row component as the library. Empty state: star icon, "No favorites yet", "Tap the star on any
  document, or long-press a document and choose 'Add to favorites'."

### 6.4 Search

- M3 search bar (52 dp, full radius): magnifier, hint "Search name, tag, folder or words inside files…",
  clear button appears with text. `ImeAction.Search` commits the query to history.
- Segmented scope row: **Everything · Name · Tag · Folder · Inside files** + a **Filters** chip that opens the
  inline filter panel (toggles the panel; the count of active filters shows as "(n)").
- Filter panel rows: Type (Any/PDF/DOCX/**XLSX**/**PPTX**/TXT/EPUB) · Folder (Any + each folder) · Tag (Any + each tag) ·
  Added (Any time / This week / This month / Last 3 months) + Reset / Apply.
- With an empty query: **Recent searches** chips (tap to re-run, ✕ each, Clear all) and **Search by** quick
  chips (All PDFs · Added this week · Folder · Work · Tag · important · Word · chapter), then the empty state.
- With a query: a summary banner — "N documents · M files contain “q” (K hits)" — then
  **Documents** results, then **Inside files** results with a serif snippet where the query is emphasised and
  `n matches · first on page p · last opened`. Tapping an inside-file result opens the document at that page
  **with the find bar pre-filled and the first hit highlighted**.
- No results: "Nothing matched “q”. Try another word, or loosen the filters — you have N active."
- History sheet (clock icon in the app bar): last 10 queries, tap to run, ✕ to remove, "Clear search history".

### 6.5 Settings

Grouped `surfaceContainerLow` cards with `primary` uppercase group titles and hairline dividers:

- **Appearance** — App theme (Light/Dark/System, sheet) · Default reading theme (5 swatch tiles:
  Paper, Sepia, Night, OLED, Auto) · Animations (switch) · Haptic feedback (switch) · Language (sheet)
- **Reading** — Default scroll direction · Text reflow by default · Default zoom (sheet with a slider,
  shows %) · Reading typeface (Serif/Sans) · Remember last page · Page numbers on pages
- **Device** — Auto-rotate · Lock orientation per document · Maximum open tabs (− n + stepper, 1–10)
- **Data & storage** — Clear recents (shows count) · Clear search history (count) · Clear reading positions ·
  Clear all highlights (total count) · Reset demo library — each with a confirmation dialog
- **About** — Spec coverage (the §14 matrix rendered as a list — proof nothing was dropped) · version ·
  "Seed #2B7A5B · Android 6.0 (API 23) → latest · phone, foldable & tablet layouts" · live counts
- Footer: "Leaf · Material 3 · v1.0.0".

Every row is a single tap target; toggles flip in place with a snackbar confirmation for irreversible ones.

### 6.6 Reader (full-screen overlay)

**Tab model — no strip inside the reader.** Documents stay open in an ordered list; exactly one is active.
- The reader app bar shows back arrow · centred title (file name) + subtitle `7 pages · DOCX · vertical`
  (+ `· reflow`, `· bookmarked`) · **Open documents** button **with count badge** · bookmark button (fills
  `primary` when the current page is bookmarked) · overflow (⋮).
- **Back arrow and the back gesture leave the reader but do not close the document** — it stays open with its
  position saved. The first time one or more documents are left open, show the snackbar
  `N documents kept open · Tap Tabs in Library to come back`.
- Switching documents restores that document's page, zoom, scroll direction, reading theme and bookmark
  state; a snackbar confirms `Switched to “Name” · page 4 of 7`.
- Closing the active document activates the next tab, or the previous one when it was last; closing the last
  one closes the reader, returns to the Library and (with the sheet open) shows its empty state.
  **There is always a way to close the last tab** — every row in the Open documents sheet carries an × and
  Close all tabs is offered from one document upwards.

**App bar** — see the tab model above.

**Find bar** (collapsible under the app bar) — magnifier, input "Find in document…", counter "3 of 17"
(or "No matches"), previous/next, close. Every match is marked; the current match is amber and scrolled to
centre. Closing clears the marks.

**Viewport** — the page canvas in the current reading theme. Vertical mode: continuous page list with 12 dp
gaps, each page a `readSurface` card (16 dp radius, elevation 1, 20 dp horizontal padding, max 720 dp wide
centred). Horizontal mode: page-per-screen with snap, swipe, and a subtle 250 ms page-turn animation.
Reflow mode: no cards, a single column on `readSurfaceAlt`, hairline rules between pages, 18 sp serif.

**Footer** — `Page 4 of 7 ⌄` (opens Jump sheet) · progress track · `57 %` · bookmark button with count.

**Toolbar** — one row, never horizontally scrolled, with the priority ladder of §5:
**Highlight** trigger (pen glyph + live colour swatch; the caret and the "Highlight" label appear at
≥ 600 dp) · undo · redo · find · view & layout · zoom − / `100 %` / zoom + · highlights list (≥ 600 dp) ·
right-anchored full screen + share.

**Highlight colour menu** (expands from the trigger, replacing the old five always-visible dots) — a
`surfaceContainerHigh` popover above the toolbar, 12 dp radius, 236 dp minimum width, elevation 3:
header "HIGHLIGHT COLOUR" + close ×, then one row of five labelled options
(Yellow · Green · Blue · Pink · Orange) where each option is a 24 dp dot + 12 sp label and the active one
sits on a `secondaryContainer` pill; then a hint line:
- with a selection: "Tap a colour to highlight the **selected text**."
- without: "Select text in the page first, then pick a colour. Your choice becomes the default for the next
  selection."

Picking a colour sets the default for the next selection (and recolours immediately if a highlight is
selected), closes the menu and updates the trigger swatch. The menu closes on outside tap, Esc/back, on any
sheet opening, on entering full screen and on leaving the reader. The trigger shows
`state_expanded`/`state_collapsed` and `aria-expanded` equivalents.

**Highlight context bar** — appears instead of the toolbar when a highlight is tapped: label "Highlight",
five swatches with the current colour ringed, **Remove** (red), Copy (icon), Done. Tapping elsewhere dismisses it.

### 6.7 Sheets, dialogs and snackbars

| Sheet | Contents |
|---|---|
| **Sort** | ascending/descending segmented + 5 fields with ✓ |
| **Actions** (long-press) | Open · Open in new tab · Rename · Favorite toggle (✓) · Manage tags · Move to folder · Share · File info · **Delete document** (danger) |
| **View & layout** | Reading theme chips · scroll direction · text reflow switch · typeface · page numbers switch · zoom slider + Reset · orientation lock cycle (auto → portrait → landscape, stored per document) · full screen · reset zoom |
| **Jump to page** | big page number, range slider, −/+ stepper, quick jumps (First/25 %/50 %/75 %/Last), filled "Go to page n" |
| **Highlights** | colour chip + page + quoted text + timestamp per entry; tap to jump & select; per-entry delete; Clear all highlights (danger) |
| **Bookmarks** | saved pages with label and age; jump · delete; "Bookmark current page" |
| **Share** | Share document · Share page as image · Share selected text · Copy file path · Copy page text |
| **File info** | type · size · pages · added · last opened · reading position · bookmarks · highlights · folder · tags |
| **Open documents** (tabs) | "N of limit tabs open · switching keeps each document's page, zoom and theme"; one row per open document — type icon, name, `Page 3 of 5 · 2 h ago`, the active row tinted `primary` at 10 % with an **ACTIVE** marker and a filled dot. **Every row has an × close button, including the active one** (closing the active row activates the next tab, or the previous when it was last). Tapping any row — active included — opens that document in the reader. Then "Open a new document…" (enters pick-a-document mode, shows "Tab limit reached (n) — close a tab first" when at the limit) and **Close all tabs** (danger, shown whenever **one or more** documents are open, labelled **Close it** for a single tab; confirms `Close all tabs?` / "Your reading positions are saved, so every document resumes where you stopped. Highlights, bookmarks and favorites are untouched."). With nothing open the sheet shows the empty state "Nothing open yet — pick a document and it will stay here until you close it." and hides both rows. |
| **Move to folder** | nested folder list with counts, ✓ on current, "Remove from folder" |

**Confirm dialog** is used for every destructive action: delete document, delete folder, delete tag,
clear recents, clear search history, clear reading positions, clear all highlights, reset library.

**Snackbar** rules: 2 200 ms normally, 4 200 ms when an action is offered; positioned above the navigation
bar (or 14 dp above the bottom in rail layouts) and above the reader footer when the reader is open;
Undo restores deletes and highlight removals.

---

## 7 · DOCUMENT ENGINE (the hard part — do this properly)

Define `interface DocumentEngine` with `open(uri): DocumentHandle`, `pageCount`, `renderPage(index, tile, scale): Bitmap`,
`pageText(index): List<TextRun>`, `findInPage(index, query): List<MatchRect>`, `close()`.
Implement it per format and select by MIME/first bytes.

### 7.1 Renderer baseline (API 23-safe)
- Use `android.graphics.pdf.PdfRenderer` — available since API 21, so it is the correct, dependency-free
  way to rasterise PDFs on Android 6. Never bundle a native PDF renderer just for display.
- Serialise access: a single render thread (or a `Mutex`) per open document. `PdfRenderer` is **not** thread-safe.
- **Tiled rendering** for zoom: at scale `s`, compute the visible page rect from the scroll offset, render
  only the tiles that are on screen (tile ≈ 512×512 px at device density), with one tile of overdraw.
  Cap the bitmap cache at `Runtime.maxMemory() / 8` and use an `LruCache<String, Bitmap>` keyed by
  `docId/page/scale/tileX/tileY`. Recycle evicted bitmaps (`if (!bitmap.isRecycled) bitmap.recycle()`).
- Progressive render order per page: (1) low-res full-page bitmap (scale ≤ 0.5) placed instantly,
  (2) tiles at the target scale as the user settles, (3) the text/highlight layer.
- Page limit sanity: if `pageCount > 2000`, warn once and render only visible pages.
- Never hold more than 3 full-resolution page bitmaps at once; on `onTrimMemory` clear the cache.
- Guard every render call in `try/catch` — damaged PDFs must show a "This page could not be rendered"
  inline state, not crash the app.

### 7.2 Text, search and reflow
- **API ≥ 35:** use the platform text APIs — `PdfRenderer.Page.getTextContents()` for text runs and
  `PdfRenderer.Page.searchText(query)` for match bounds (both added in Android 15 / API 35), and
  `selectContent()` for precise selection. This gives you real geometry for highlights.
- **API 23–34:** extract text with a pure-JVM parser (**PDFBox-Android** preferred; it supports API 19+)
  in a background coroutine with a progress indicator for large files, then cache the extracted text
  (and word boxes where available) in Room so it happens once per document, not once per open.
- **Reflow mode** uses the same extracted text: join runs into paragraphs, drop ligature/hyphenation
  artefacts, then render as a single Compose text column (no horizontal scroll), honouring the serif/sans
  setting, zoom and system font scale. Keep an anchor map `paragraphIndex → pageIndex` so the footer page
  number and "jump to page" still work in reflow mode, and so highlights created in reflow can be mapped
  back to page geometry when possible.
- **In-file search** always operates on the extracted text index (fast, works at every API level). Show
  `n of m`, mark all hits, and auto-select the nearest hit when the document opens from a search result.
  Debounce input by 250 ms and run on `Dispatchers.Default`.

### 7.3 Format support
| Format | Render | Text | Reflow | Notes |
|---|---|---|---|---|
| **PDF** | `PdfRenderer` + tiles | §7.2 | ✅ from extracted text | highlights stored as normalised rects |
| **TXT** | Compose text, paginated by measured line height | source | ✅ native | page count computed once and cached |
| **DOCX** | Read `word/document.xml` from the OOXML zip with a streaming `XmlPullParser` (no heavy dependency); render paragraphs, headings and simple lists | source | ✅ native | images: render as placeholders with their alt text; tables flattened to rows |
| **XLSX** | first sheet rendered as a grid: read `xl/worksheets/*.xml` + `xl/sharedStrings.xml` with the same streaming `XmlPullParser`, paginate by rows (~30 rows/page) and columns, 2 dp `outlineVariant` grid lines, tabular figures | cell text | ✅ native | one page per sheet tab; sheet name in the page label; no formulas/formatting beyond bold headers |
| **PPTX** | one page per slide: `ppt/slides/slide*.xml` + `ppt/slideLayouts` placeholders, slide rendered as the familiar 16:9 card with a title placeholder and bullet lines | placeholder text | ✅ native | slide number badge `3 / 12` on the page card; ignores shapes, media and animations |
| **EPUB** | **Readium Kotlin toolkit 3.1.0** (`readium-navigator`, `readium-shared`, `readium-streamer`) | Readium | ✅ native | needs `coreLibraryDesugaring` below API 26; highlights use CFI/text ranges; wire Readium's own settings for font size/theme so Leaf's reading themes apply to EPUB too |

For all formats expose the same `PageCanvas` contract so the toolbar, find, highlights, bookmarks, tabs and
Page Stay behave identically regardless of file type.

### 7.4 Highlight storage contract
- PDF: `bounds: List<NormalizedRect>` in 0..1 page space (independent of zoom/screen), plus `text`.
  Merge adjacent line rects into one highlight entry; store them in reading order.
- Reflow/TXT/DOCX/XLSX/PPTX/EPUB: `textRange` (character offsets in the extracted text) and/or `cfiRange`.
- Rendering: one `Canvas` overlay above the page bitmap draws all rects for that page with the colour at the
  theme's alpha; hit-testing uses the same rects (with 4 dp inflation for finger accuracy).
- Every mutation (add, remove, recolour, page-clear, full-clear) pushes an op on a 50-deep undo stack;
  the stack is per session (not persisted) and exposes `undo()/redo()` plus a label for the snackbar verb.
- Selection UI on API < 35: a custom text-selection layer for PDFs is expensive — ship the pragmatic
  version: long-press selects the nearest text run (word → sentence fallback), shows drag handles for the
  start/end runs, then the colour palette applies the highlight. Document this trade-off in the README.

---

## 8 · BEHAVIOUR CONTRACTS

1. **Open a document** → make it the active tab (reuse the current tab), update `lastOpened`, prepend to
   recents (unique), restore position/zoom/scroll direction per *Remember last page*, apply the reading theme.
   **Open in a new tab** (pick mode, actions sheet, tab-sheet "Open a new document…", Ctrl/⌘+T) adds a tab
   instead of reusing one and is refused with
   `Tab limit reached (6) — close a tab or raise the limit in Settings` when the limit is reached.
2. **Close a tab** → persist its position, activate the next tab (previous when it was last) and offer
   **Undo** in the snackbar; the Undo restores the tab without reopening the reader if it was closed.
   Closing the last tab closes the reader and restores the shell; the snackbar reads
   `Tab closed — pick a document any time`. **Leaving the reader never closes a tab.**
3. **Kill and relaunch** → the app must restore: last tab set, active tab, positions, scroll directions,
   zoom, reading theme, app theme, filters, sort fields, search history, and (optionally) the last screen.
4. **Navigation state:** switching between the 5 destinations preserves each screen's scroll position and
   filter state for the session.
5. **Filters compose:** folder × smart collection × tags × type × advanced filters all apply together, and
   the counts shown on chips always reflect the *current* filter context.
6. **Rename** keeps the extension: typing "Annual report" on a PDF yields `Annual report.pdf`.
7. **Delete flow:** confirm dialog → remove from library, tabs and recents → snackbar "Document deleted"
   with **Undo** that restores the document *with* its highlights and bookmarks.
8. **Move to folder** updates counts everywhere (library chips, folder manager, file info).
9. **Long-press** anywhere on a row opens actions; the row shows a pressed state and a 18 ms haptic.
10. **Back button order:** dialog → sheet → filters panel → find bar → full screen → close reader → previous
    destination → exit. Implement with `BackHandler` per layer and never leave a dead back press.
11. **Snackbars replace, never stack.** One at a time, newest wins.
12. **Every toggle** persists immediately (DataStore) and re-renders dependent surfaces.
13. **File-type grouping** is a persisted setting (`groupByType`, default off) that always opens on the
    **overview**, never inside a group; the open group itself is transient session state. Migrate any build
    that persisted `sortField = FileType` to `groupByType = true, sortField = Name`. When filters empty the
    open group's type, return to the overview rather than showing an empty group.

---

## 9 · PLATFORM CONSTRAINTS FOR API 23 (Android 6.0)

- **Colours on surfaces:** no `RenderEffect`, no `Modifier.blur`, no dynamic colour. Use tonal surfaces
  (`surfaceContainerLow/High`) for depth and `Modifier.shadow` with small elevations.
- **Insets:** use `WindowInsetsCompat`; on API 23 set the status-bar icons dark with
  `WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = true`. Do not use
  `Window.setDecorFitsSystemWindows` directly — go through androidx.
- **Vector drawables:** every vector must be compatible (no `<gradient>` in vectors below API 24 — rasterise
  or approximate gradients with layered paths, as the mockup's gradients are decorative only).
- **Multidex:** native multidex is available from API 21, so `multiDexEnabled = true` in `defaultConfig` is
  enough — do **not** add `MultiDexApplication` / `MultiDex.install()`.
- **Storage:** no scoped-storage APIs; access files only through SAF URIs with persistable permissions, or
  `content://` URIs your app created. Never assume `Environment.getExternalStorageDirectory()` paths work.
- **Notifications/background:** none required. Do all indexing in `Dispatchers.IO` coroutines scoped to the
  app process and re-run on next launch if interrupted.
- **Fonts:** bundle the serif; `res/font` XML families with `android:fontVariationSettings` are not reliable
  below API 26 — use plain `Typeface.createFromAsset`/`FontFamily` with explicit weights.
- **Emoji/icons:** use vector paths, never emoji, for UI affordances (API 23 emoji coverage is patchy).
- **Ripple:** Material 3 `indication` works, but keep the custom press states from §4.5 as the primary
  feedback because `pointerInput` long-press + ripple can mis-order on older devices.

---

## 10 · ACCESSIBILITY & INTERNATIONALISATION

- Content descriptions for every icon button, descriptive (not "button"): "Bookmark this page",
  "Clear all recents", "Highlight colour yellow".
- `semantics { heading() }` on section labels; `stateDescription` on progress bars ("57% read, page 4 of 7").
- Touch targets ≥ 48 dp even when the visual is 38–40 dp (use `Modifier.minimumInteractiveComponentSize()`).
- Respect system font scale to 200 %: text reflow absorbs it; lists must not clip.
- No colour-only meaning: tags and highlights carry labels in the summary sheets; file types carry text
  (PDF/DOCX/TXT/XLSX/PPTX/EPUB) inside the icon.
- RTL: mirror chips rails, sheets and the reader chrome for Arabic; the writing direction of reading content
  follows the document's own language. Test with the API 36 emulator in Arabic (`ar-SA`).
- All strings in `values/strings.xml` + translations for the 9 launch languages; use `<plurals>` for
  "1 document"/"8 documents", "1 highlight"/"5 highlights"; never concatenate sentences in code.

---

## 11 · COPY DECK (use these strings verbatim)

| Context | String |
|---|---|
| Library hero | `Jump back in` · `Resume reading` · `Actions` |
| Continue grid | `Continue · last 5` · `See all` |
| Groups | `Today` · `Earlier` · `Clear today` |
| Search summary | `{n} documents · {m} files contain “{q}” ({k} hits)` · `{n} matches` |
| Open documents | `Open documents` · `{n} of {limit} tabs open · switching keeps each document's page, zoom and theme` · `Open a new document…` · `Goes to your library so you can choose` · `Close all tabs` · `{n} documents will be closed` · `Tab restored` · `All tabs closed` |
| Tab limit | `Tab limit reached ({limit}) — close a tab or raise the limit in Settings` · `Tab limit reached ({limit}) — close a tab first` |
| Kept open | `{n} document(s) kept open · Tap Tabs in Library to come back` |
| Pick a document | `Choose a document` · `Opens in a new tab · {n} of {limit} in use` · `Choose a document to open in a new tab` · `Cancel` |
| Close all confirm | `Close all tabs?` · `Your reading positions are saved, so every document resumes where you stopped. Highlights, bookmarks and favorites are untouched.` · `Close all` |
| Highlight menu | `Highlight colour` · colour names `Yellow/Green/Blue/Pink/Orange` · `Tap a colour to highlight the selected text.` · `Select text in the page first, then pick a colour. Your choice becomes the default for the next selection.` · `Selected {colour} · select text to highlight it` |
| File-type groups | `Group by file type` · `Grouped by file type · tap a group to see its files` · `Showing one list of every document` · card titles `PDF documents`, `Word · DOCX`, `Excel · XLSX`, `PowerPoint · PPTX`, `Plain text · TXT`, `EPUB books` · card sub-line `5 documents · 24.10 MB · 2 highlights · 2 bookmarks` · crumb `PDF documents` / `5 documents · Name · A–Z` · entry snackbar `PDF documents · 5 documents · sorted by Name · A–Z` |
| Search scopes | `Everything` `Name` `Tag` `Folder` `Inside files` `Filters` |
| Find bar | `Find in document…` · `{i} of {n}` · `No matches` |
| Empty states | `Nothing here yet` · `No recents yet` · `No favorites yet` · `No results` · `No bookmarks yet` · `No highlights in this document yet` |
| Snackbars | `Added to favorites` · `Removed from favorites` · `Document deleted` · `Document restored` · `Recents cleared` · `Search history cleared` · `Reading positions cleared` · `All highlights cleared` · `Folder created` · `Subfolder created` · `Folder renamed` · `Folder deleted` · `Tag created` · `Tag renamed` · `Tag deleted` · `Highlighted · yellow` · `Colour changed to orange` · `Highlight removed` · `Page 4 bookmarked` · `Bookmark removed from page 4` · `Jumped to page 12` · `Zoom 130%` · `Text reflow on` · `Vertical scrolling` · `Horizontal — swipe between pages` · `Reading theme: Sepia` · `App theme: dark` · `Tab limit reached (6)` · `Opened in new tab` · `Closed “Q3 Product Roadmap”` · `Orientation locked: landscape` · `Library reset to the sample data` |
| Confirmations | `Delete this document?` · `Delete “Work”?` · `Delete tag “important”?` · `Clear all recents?` · `Clear search history?` · `Clear reading positions?` · `Clear every highlight?` · `Reset demo library?` |
| Undo affordance | button label `Undo` (4 200 ms snackbar) |

Sentence case for UI labels, Title Case never; use the typographic apostrophe (’) and the ellipsis character (…).

---

## 12 · TESTING & VERIFICATION

**Unit tests (JVM):** sort comparators (all 5 fields × 2 directions) · filter composition (folder × tag ×
smart × type × advanced) · smart-collection rules · `timeAgo` boundaries · search history de-duplication and
cap · recents cap and uniqueness · highlight undo/redo state machine · rename extension preservation ·
page-percentage maths · JSON/DataStore migrations.

**Instrumented tests:** open → close → reopen restores page/zoom/theme · tab switch restores per-tab state ·
find-in-file counts and jumps · highlight add/recolour/remove survives config change · delete + Undo restores
highlights · process death (use `ActivityScenario.recreate()` and a real `kill` test) restores library state ·
folder/tag delete never removes documents.

**Screenshot tests:** Roborazzi (or Paparazzi for pure-compose screens) for the 5 destinations and the reader
in Paper/Sepia/Night/OLED at 360 dp, 412 dp and 800 dp. Diff against the mockup screenshots in `/qa` —
tolerance: ≤ 2 % pixel difference on layout-critical regions (app bar, nav bar, list row, toolbar).

**Performance gates:** cold start < 800 ms on the API 23 emulator · no dropped frames while scrolling a
200-item library · memory stays flat while swiping 50 PDF pages (no leak in the bitmap cache) ·
APK/AAB ≤ 25 MB with the serif bundled.

**Manual QA sweep before release** (run it, don't assume): the 24-row acceptance matrix in §14 executed on
API 23 and API 36, portrait and landscape, phone and tablet, light + dark, Paper + Night reading themes.

---

## 13 · MILESTONES (build in this order, verify each before moving on)

| # | Milestone | Deliverable | Verification |
|---|---|---|---|
| **M1** | Foundation | Project, Gradle, theme + tokens, navigation shell (5 destinations), responsive bar/rail, sample data seed | Screenshots at 360/412/800 dp for all 5 empty screens; app bar/insets correct on API 23 |
| **M2** | Library & organisation | Rows, folder/smart/tag chips, sort sheet, folder manager, tag manager, actions sheet, rename/move/delete+undo | Unit tests for sort/filter; long-press flow; counts stay correct |
| **M3** | Reader core | PDF engine, tiles, zoom/pan, vertical/horizontal, reader chrome, jump-to-page, Page Stay | Reopen restores exact position after process death; 60 fps on API 23 |
| **M4** | Search | Library search with 5 scopes, combined filters, in-file search, history, result→open-at-page | Find counts correct; snippet emphasis; filters compose |
| **M5** | Annotation & tabs | Highlights (5 colours, selection, undo/redo, summary), bookmarks, multi-tab with limit | Highlight survives zoom/theme/restart; tab state preserved |
| **M6** | Formats & sharing | TXT, DOCX, **XLSX**, **PPTX**, EPUB engines; reflow; share sheet; clipboard | Same features on every format; share targets work |
| **M7** | Settings, polish, i18n | All settings, all confirmations, snackbars, animations, haptics, 9 languages, accessibility pass | Screenshot diff vs mockup ≤ 2 %; TalkBack traversal clean |
| **M8** | Hardening | API 23 crash sweep, memory profiling, ProGuard/R8 rules, release build, coverage matrix filled in | Zero stubs; §14 fully green |

After each milestone, post a short report: what shipped, the exact commands you ran, the screenshots you took,
what deviates from the mockup and why, and what remains.

---

## 14 · FEATURE COVERAGE MATRIX (fill this in as you build — it is the definition of done)

| # | Feature group | Source | Status |
|---|---|---|---|
| 1 | Recents — auto tracking, hero, continue grid, today/earlier, per-entry remove, clear today, clear all, badge | Mockup §6.2 | ☐ |
| 2 | Favorites — star toggle, own sort (name/added/opened) | §6.3 | ☐ |
| 3 | Page Stay — position, offset, zoom, scroll dir, theme, orientation; progress everywhere | §2.2 | ☐ |
| 4 | Folders — create, rename, recolour, delete, nesting, counts | §6.1 | ☐ |
| 5 | Smart collections — 7 rules, addable | §6.1 | ☐ |
| 6 | Tags — create, rename (cascade), recolour, delete, AND filtering, manager | §6.1 | ☐ |
| 7 | Advanced search — 5 scopes, combined filters, summary, snippets, open-at-page | §6.4 | ☐ |
| 8 | Search history — chips, per-item delete, clear, re-run | §6.4 | ☐ |
| 9 | Sorting — 4 fields × direction, independent favorites sort, applies to the open file-type group | §6.1 | ☐ |
| 10 | Highlighting — 5 colours, selection, recolour, remove, copy | §7.4 | ☐ |
| 11 | Highlight undo/redo — 50-step, labels, snackbars | §7.4 | ☐ |
| 12 | Highlights summary panel — quote, page, jump, delete, clear all | §6.7 | ☐ |
| 13 | Bookmarks — page flag, toolbar, shortcut, sheet, count | §6.6 | ☐ |
| 14 | Multi-tab — Open documents sheet + badge, pick-a-document mode, per-tab state, **close any row incl. the active/last one**, close all from n≥1, undo, configurable limit, back keeps tabs | §6.6 | ☐ |
| 15 | Share — document, page image, selection, copy path, copy page text | §6.7 | ☐ |
| 16 | File actions — rename, delete (+undo), move, tags, favorite, info | §6.7 | ☐ |
| 17 | Reading themes — Paper, Sepia, Night, OLED, Auto | §4.2 | ☐ |
| 18 | Scroll modes — vertical, horizontal with snap+page-turn | §6.6 | ☐ |
| 19 | Zoom & pan — pinch, double-tap, buttons, ctrl-wheel, drag-pan, 70–250 % | §6.6 | ☐ |
| 20 | Text reflow — all formats, anchored to pages | §7.2 | ☐ |
| 21 | Full screen — hides all chrome, dim footer, Esc/tap exit | §6.6 | ☐ |
| 22 | Rotate — auto toggle, per-document lock (auto/portrait/landscape) | §5 | ☐ |
| 23 | Settings — appearance, reading, device, data, about | §6.5 | ☐ |
| 24 | Responsive + platform — bar/rail, 2/3-col, foldable, **toolbar priority ladder**, API 23 → 36, i18n, a11y | §5, §9, §10 | ☐ |
| 25 | Highlight colour menu — expanding labelled palette, live swatch, selection-aware hint, dismiss rules | §6.6 | ☐ |
| 26 | Excel `.xlsx` — sheet-per-page grid rendering, text, find, highlights, reflow | §7.3 | ☐ |
| 27 | PowerPoint `.pptx` — slide-per-page card rendering, text, find, highlights, reflow | §7.3 | ☐ |

**Explicitly out of scope — do not build:** Import screen (local storage, cloud services, recent downloads,
batch import), Export highlights as a list, Share a link to the file, Duplicate.

---

## 15 · HOW TO WORK

1. Start by reading `leaf-mockup-v2.html` end to end (CSS tokens, layout, interaction JS) and
   `LEAF-SPEC.md`. Then write a one-page build plan and the milestone list back to me for approval.
2. Build **M1 → M8** in order. Do not start a milestone before the previous one builds and runs.
   Work on one branch per milestone and open a draft PR immediately, so CI (`ci/android.yml` →
   `.github/workflows/android.yml`) builds and uploads a debug APK artifact for every push. Releases are cut
   by tagging `v*`; the release job must stay green from the first commit. See `LEAF-AGENT-WORKFLOW.md`.
3. After each milestone: `./gradlew assembleDebug lint testDebugUnitTest`, install on the API 23 emulator
   and the API 36 emulator, screenshot every screen you touched, and compare with the mockup. Report deviations.
4. Prefer androidx/Compose APIs that work on API 23. If you must guard an API, add a comment naming the
   feature it enables and the fallback used.
5. Keep the coverage matrix (§14) updated in `README.md` — it is the artefact I will check.
6. Never delete or stub a feature to make a build pass. Ask instead.
7. When you finish everything: produce a release-signed AAB, the R8 mapping file, the filled coverage matrix,
   the screenshot set (all screen × size × theme combinations), and a CHANGELOG.
