# 🌿 Leaf — offline document reader for Android

Native Android · Kotlin · Jetpack Compose · Material 3 · **Android 6.0 (API 23) → latest (API 36)**.

Leaf is being built to read PDF · DOCX · XLSX · PPTX · TXT · EPUB and treats *resuming* as the core
value: every document remembers its page, zoom, reading theme and orientation, and every piece of
organisation — folders, collections, tags, favorites, highlights and bookmarks — belongs to the
document, not to a cloud account.

The design contract lives in [`design/`](design/): `LEAF-MASTER-PROMPT.md` (the specification),
`LEAF-SPEC.md` (product + data model), `leaf-mockup-v2.html` (the interactive mockup) and
`qa/mockup-ref/` (the reference screenshots).

---

## Status · Milestones 1–3 — Foundation + Library + Reader core

M1 established the Android foundation and M2 completed the Library & organisation flows. M3 adds a
real PDF reading path: demo PDFs, external PDF VIEW/SEND intake, the API-23-safe renderer and a live
reader overlay. The milestone boundary remains explicit: unsupported formats and later-milestone
controls are omitted rather than presented as dead buttons.

**In this build**

- **Project** — version catalog pinned to the versions in `LEAF-MASTER-PROMPT.md` §2,
  `applicationId app.leaf.reader`, `minSdk 23` / `targetSdk 36` / `compileSdk 36`, JDK 17,
  core library desugaring on, R8 off for debug, multidex on, the manifest from §2 (haptics
  permission only, no network permission, SAF open-with intake).
- **Tokens (§4, verbatim)** — the M3 TonalSpot light and dark role sets, the Paper/Sepia/Night/OLED
  reading palettes, the tone-matched highlight colours and find marks, the role-based type scale
  (bundled Noto Serif for reading text, system sans for UI), and the shape / spacing / motion /
  component-metric tables.
- **Navigation shell (§5)** — one activity, edge-to-edge, `WindowSizeClass`-driven:
  `NavigationBar` below 600 dp, `NavigationRail` at or above it. Five destinations with per-screen
  saved state; Library is the default.
- **Data layer (§3, §8)** — Room (documents, tags, folders, collections, recents, bookmarks,
  highlights, cached page text, reading positions) and DataStore settings with the mockup's
  defaults, including the §8.13 migration that turns a legacy *File type* sort field into the
  grouped view.
- **Demo library** — the mockup's seed, generated from `design/leaf-mockup-v2.html`: 10 documents
  (including the `.xlsx` and the `.pptx`), 5 folders, 7 tags, 6 smart collections, recents,
  bookmarks, highlights and the text of all 40 pages.
- **Library rows (§4.6)** — name and file icon, pages / size / bookmark / last-opened metadata, tag
  chips, reading progress, star toggle, and the 470 ms long-press actions sheet.
- **Filter rails and sort** — folders with subtree counts, smart collections, multi-select AND tags,
  a four-field sort sheet, and a visible file-type filter glyph.
- **File-type groups (§6.1, §8.13)** — PDF → DOCX → XLSX → PPTX → TXT → EPUB overview, responsive
  one-/two-column cards, a transient group crumb, sorting inside a group, persisted grouping setting,
  empty-group fallback and the three required snackbars.
- **Folder and tag managers** — nested folders with document/subfolder counts; create/rename/recolour/
  confirmed delete; tag create/rename with cascade/recolour/confirmed delete. Neither delete path ever
  deletes documents.
- **Smart collections** — the complete seven-rule composer (all, type, age, in progress, unfiled,
  highlights, tag) with type/age/tag follow-up sheets.
- **Document actions** — rename preserving the extension, favorite, manage tags, move, file info,
  confirmed delete and Undo that restores attached data.
- **M3 PDF reader** — Library PDF row taps and the PDF-only Open action open a real paged reader;
  vertical continuous scroll, horizontal page snapping, pinch/double-tap zoom, drag-pan, progress and
  jump-to-page, per-document reading themes, full-screen mode, and persisted Page Stay state.
- **PDF engine** — `PdfRenderer` serialized per open document, progressive preview + 512 px tiles with
  one-tile overdraw, a memory-bounded/recycling LRU and trim-memory cleanup. API 35+ platform text
  extraction and API 23–34 PDFBox fallback cache extracted page text in Room.
- **Demo and external PDFs** — five generated demo PDFs from the existing seed content; external
  `ACTION_VIEW` / `ACTION_SEND` PDF intake retains a durable URI grant or copies the file privately.
  There is no Import screen or in-app file picker.

**Not in this build** — advanced Search and its filter panel (M4), annotations and tabs (M5),
non-PDF engines, reflow and sharing (M6), full Settings controls (M7) and the remaining M8 scope.
Recents, Favorites, Search and Settings still show their M1 scaffolds until their milestones.

## Build & verify

```bash
./gradlew lint testDebugUnitTest assembleDebug   # build, lint and JVM tests
./gradlew connectedDebugAndroidTest              # real PDFs on an API 23 / API 36 emulator
./gradlew recordRoborazziDebug                   # render screen snapshots
./gradlew verifyRoborazziDebug                   # verify recorded screenshot baselines
python3 tools/gen_seed_content.py                # regenerate SeedContent.kt from the mockup
python3 tools/gen_launcher_icon.py               # regenerate the legacy launcher PNGs
python3 tools/generate_demo_pdfs.py               # regenerate demo PDF assets
```

The session's draft PR is pinned to `arena/ceade348-leaf`; its CI uploads `leaf-debug-<sha>` (an
installable debug APK) and `rendered-screens-<sha>` (the screenshots).

## Screenshots

Roborazzi renders the five destinations at 360 / 412 / 700 dp in light and dark. M3 adds the reader
at 360 / 412 / 800 dp in Paper / Sepia / Night / OLED using a deterministic PDF-engine fixture; the
separate API 23 / 36 instrumented test exercises the real bundled PDF and `PdfRenderer`. The Library
screenshot harness waits for the Room flow and uses a fixed clock so record and verify see the same
content.

| Destination | 360 dp | 412 dp | 700 dp |
|---|---|---|---|
| Library | `library_360_light` · `library_360_dark` | `library_412_*` | `library_700_*` |
| Recents | `recents_360_*` | `recents_412_*` | `recents_700_*` |
| Favorites | `favorites_360_*` | `favorites_412_*` | `favorites_700_*` |
| Search | `search_360_*` | `search_412_*` | `search_700_*` |
| Settings | `settings_360_*` | `settings_412_*` | `settings_700_*` |
| Reader | `reader_{paper,sepia,night,oled}_360` | `reader_{paper,sepia,night,oled}_412` | `reader_{paper,sepia,night,oled}_800` |

## Feature coverage matrix (§14)

The definition of done. A row is ticked only when the feature works end to end and has been verified
by running it.

| # | Feature group | Source | Status |
|---|---|---|---|
| 1 | Recents — auto tracking, hero, continue grid, today/earlier, per-entry remove, clear today, clear all, badge | Mockup §6.2 | ☐ |
| 2 | Favorites — star toggle, own sort (name/added/opened) | §6.3 | 🟨 partial — the Library star toggle is live; Favorites screen and its independent sort are later |
| 3 | Page Stay — position, offset, zoom, scroll dir, theme, orientation; progress everywhere | §2.2 | 🟨 partial — PDF position/offset/zoom/direction/theme restore through Room and Library progress updates; the existing orientation field has no M3 control |
| 4 | Folders — create, rename, recolour, delete, nesting, counts | §6.1 | ✅ M2 |
| 5 | Smart collections — 7 rules, addable | §6.1 | ✅ M2 — all seven rules and their composer |
| 6 | Tags — create, rename (cascade), recolour, delete, AND filtering, manager | §6.1 | ✅ M2 |
| 7 | Advanced search — 5 scopes, combined filters, summary, snippets, open-at-page | §6.4 | ☐ |
| 8 | Search history — chips, per-item delete, clear, re-run | §6.4 | ☐ |
| 9 | Sorting — 4 fields × direction, independent favorites sort, applies to the open file-type group | §6.1 | 🟨 partial — Library sort and open groups are live; Favorites sort is later |
| 10 | Highlighting — 5 colours, selection, recolour, remove, copy | §7.4 | ☐ |
| 11 | Highlight undo/redo — 50-step, labels, snackbars | §7.4 | ☐ |
| 12 | Highlights summary panel — quote, page, jump, delete, clear all | §6.7 | ☐ |
| 13 | Bookmarks — page flag, toolbar, shortcut, sheet, count | §6.6 | ☐ |
| 14 | Multi-tab — Open documents sheet + badge, pick-a-document mode, per-tab state, close any row incl. the active/last one, close all from n≥1, undo, configurable limit, back keeps tabs | §6.6 | ☐ |
| 15 | Share — document, page image, selection, copy path, copy page text | §6.7 | ☐ |
| 16 | File actions — rename, delete (+undo), move, tags, favorite, info | §6.7 | 🟨 partial — M2 actions and PDF Open work; non-PDF Open is M6, new tab M5, Share M6 |
| 17 | Reading themes — Paper, Sepia, Night, OLED, Auto | §4.2 | 🟨 partial — all five are live and saved in PDF Page Stay; global default control remains M7 |
| 18 | Scroll modes — vertical, horizontal with snap+page-turn | §6.6 | 🟨 implemented — continuous vertical list and snapping horizontal pager; reader screenshot matrix and API 23/36 PDF tests pass in M3 CI |
| 19 | Zoom & pan — pinch, double-tap, buttons, ctrl-wheel, drag-pan, 70–250 % | §6.6 | 🟨 implemented — pinch, double-tap, toolbar/sheet zoom, ctrl-wheel and drag-pan; M3 CI passes |
| 20 | Text reflow — all formats, anchored to pages | §7.2 | ☐ |
| 21 | Full screen — hides all chrome, dim footer, Esc/tap exit | §6.6 | 🟨 implemented — system bars/app bar/toolbar hide; dim footer, Escape/back and tap exit; M3 CI passes |
| 22 | Rotate — auto toggle, per-document lock (auto/portrait/landscape) | §5 | ☐ |
| 23 | Settings — appearance, reading, device, data, about | §6.5 | ☐ |
| 24 | Responsive + platform — bar/rail, 2/3-col, foldable, toolbar priority ladder, API 23 → 36, i18n, a11y | §5, §9, §10 | 🟨 partial — bar/rail, 2-col grid metrics and API 23 → 36 support are in; 2-col *lists*, the toolbar ladder, i18n and the a11y pass land with their screens |
| 25 | Highlight colour menu — expanding labelled palette, live swatch, selection-aware hint, dismiss rules | §6.6 | ☐ |
| 26 | Excel `.xlsx` — sheet-per-page grid rendering, text, find, highlights, reflow | §7.3 | ☐ |
| 27 | PowerPoint `.pptx` — slide-per-page card rendering, text, find, highlights, reflow | §7.3 | ☐ |

## Verification

M2 is verified in [CI run 37662707845](https://github.com/ab2024103-cmd/Leaf/actions/runs/37662707845). M3 is green in [CI run 37934942214](https://github.com/ab2024103-cmd/Leaf/actions/runs/37934942214) for commit `e3a261a`.

| Check | M2 last verified | M3 latest CI |
|---|---|---|
| Build, lint & JVM tests | ✅ | ✅ CI run 37934942214 |
| Debug APK | ✅ | ✅ [download](https://github.com/ab2024103-cmd/Leaf/actions/runs/37934942214/artifacts/11618397054) |
| Screenshot parity | ✅ | ✅ 12 reader captures across Paper/Sepia/Night/OLED at 360/412/800 dp; [artifact](https://github.com/ab2024103-cmd/Leaf/actions/runs/37934942214/artifacts/11617991899) |
| Real PDF on API 23 / 36 | not part of M2 | ✅ both real-renderer instrumentation jobs passed in CI run 37934942214 |
| Release APK + AAB | skipped — only runs on `v*` tags | skipped — only runs on `v*` tags |

The PDF emulator matrix uses an API 23 x86 image and API 36 x86_64, with bounded boot/test timeouts and failure diagnostics. The API 23 tests also verified that PDFBox text extraction runs on the minimum supported API.

M2's 89 tests covered sorting in both directions, filter composition/count context, all seven smart
rules, folder subtree/group counts, the empty-group fallback, rename-extension behavior, delete+Undo,
folder/tag delete safety, and the 470 ms row long-press/tap flow. M3 adds PDF tile-grid/cache lifetime,
Page Stay repository and fresh-ViewModel restoration after closing and reopening the on-disk Room DB,
damaged-PDF error-state coverage, real-renderer instrumentation (including a 50-tile API 23/36 sweep),
and reader-theme screenshots. This is a process-recreation simulation, not an OS-level
forced-process-kill test.

## Deviations from the mockup and the spec

Every deviation is a decision, not an accident.

1. **Non-Library destinations remain M1 scaffolds.** The M2 Library is now populated and functional;
   Recents, Favorites, Search and Settings remain on their honest M1 empty-state surfaces until their
   own milestones.
2. **`ci/android.yml` could not pass as-is.** Its release job used
   `if: ${{ secrets.KEYSTORE_BASE64 != '' }}`; GitHub rejects the `secrets` context inside an `if:`
   expression, which invalidated the *entire* workflow file — every run of it produced zero jobs, on
   `main` as well as on branches. The secrets are now surfaced as env vars and the signed/unsigned
   decision is taken in the shell. Behaviour is unchanged: signed when the keystore secrets exist,
   unsigned with a warning in the release notes when they do not.
3. **The working branch was added to the push trigger.** This repository never fires
   `pull_request` events — no check appeared on the PR even after it left draft — so no run, and no
   APK artifact, could be produced for a feature branch. `on.push.branches` is now
   `[ main, 'arena/**' ]`.
4. **XLSX / PPTX dark file-icon tones.** §4.6 gives the light values as hexes but states the dark
   rule only in words ("containers at 30 % tone, labels at 90 %"). The dark values are taken from
   the mockup's own CSS for those two types; the other four types match both sources.
5. **Screenshots are captured through the view hierarchy, not the Compose one.** Roborazzi's
   Compose bridge (`onRoot().captureRoboImage()`) renders **blank** with this toolchain — a plain
   full-screen red box came out uniformly `#FAFAFA` at SDK 30, 33 and 35 — while its native view
   path renders correctly. Each screenshot test therefore launches a `ComponentActivity`, composes
   the whole shell with an explicit theme and window size class, and captures the decor view. The
   images are real renders of the real shell: light surface `#F5FBF5`, dark `#0F1511`, the
   navigation bar and rail on `surfaceContainer`, and the active pill on `secondaryContainer`.
6. **Roborazzi is pinned to 1.60.0 and Robolectric to 4.14.1.** Roborazzi 1.61.0+ is compiled with
   Kotlin 2.3.21, whose metadata (2.3.0) the pinned Kotlin 2.1.20 compiler cannot read, and
   Roborazzi depends on Robolectric as `compileOnly`, so it is aligned with the version Roborazzi
   1.60.0 was built against (4.14.1).
7. **`WindowInsets.safeDrawing` needed its own import.** The inset values are extension properties
   on `WindowInsets.Companion` in the Android source set, so they are not in scope from the class
   import alone.
8. **Dynamic colour.** §1.4 requires an optional "Match system colours (Android 12+)" switch, which
   the mockup does not have. It exists in settings as `matchSystemColors`, default **off**, and is
   wired to `LeafTheme`; the Settings row that flips it lands in M7.
9. **Demo documents keep their stable `leaf-demo://` URI.** M3 generates a real PDF asset for each
   of the five seeded PDF rows and materializes it into app cache for `PdfRenderer`; the other seeded
   formats continue to use their existing text cache until M6. No Import screen or fake SAF result is
   added.
10. **No `design/leaf_tokens.json` in this repo.** The token set was re-derived from the mockup's CSS
    and compared against §4; the two agree everywhere except deviation 4.
11. **Only seeded PDFs open in M3.** PDF row taps and the PDF-only Open action launch the reader;
    non-PDF demo rows remain available for Library organization but have no fake reader target. The
    M2 long-press actions sheet remains intact.
12. **Later actions are omitted rather than stubbed.** Open in new tab and Share are deferred to
    M5/M6; incoming external PDF VIEW/SEND intents work in M3, while the in-app document picker stays
    deferred to M5. Pick-a-document mode remains part of the M5 tabs flow.
13. **The §6.1 filter glyph is decorative in M2.** The advanced filter panel is part of the later
    Search scope; no inert tappable filter button is exposed.
14. **Sort direction follows the labels.** “Ascending” shows A–Z / Newest / Recent / Largest, matching
    the mockup's visible sort copy. The mockup's comparator reverses the three numeric/time fields
    against those labels; Leaf keeps the labels and makes the order agree with what the user sees.
15. **Milestone branch naming is session-pinned.** The Arena session is fixed to
    `arena/ceade348-leaf`; the existing draft PR carries M1–M3 rather than creating a separate
    milestone branch.
16. **Local Gradle verification is unavailable.** The editing environment has no Java runtime, so
    Gradle could not start locally. CI run 37934942214 passed build/lint/JVM tests, screenshot parity,
    and real-PDF instrumentation on API 23 and API 36; the debug APK and rendered screenshot artifacts
    are linked in the verification table. Release APK + AAB remain tag-only and were correctly skipped.
17. **Page Stay test boundary.** The M3 test closes and reopens the on-disk Room database and creates a
    fresh ReaderViewModel; an OS-level forced process-kill / relaunch test remains a release QA check.
18. **Frame-rate QA boundary.** API 23 / 36 instrumentation sweeps 50 real-PDF tile requests and logs
    mean render time plus cache bytes; it does not prove 60 fps. Frame profiling on a physical/API-23
    device remains a manual release gate.
19. **PDFBox-Android is pinned to 2.0.24.0 for API 23 text extraction.** CI reproduced a
    `StackOverflowError` in `BufferedRandomAccessFile` on API 23 with 2.0.27.0. Upstream documents
    the same regression on API 19–23, introduced in 2.0.25.0, while 2.0.24.0 and earlier do not
    exhibit it ([upstream issue #442](https://github.com/TomRoush/PdfBox-Android/issues/442)). The
    pinned release preserves the required PDFBox fallback and passes the real-PDF matrix on API 23
    and API 36.

## What's next — M4 · Search

Add the advanced search UI against cached PDF text without moving annotation, tab, reflow, sharing or
broader settings work forward from their approved milestones.
