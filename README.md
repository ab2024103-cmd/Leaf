# 🌿 Leaf — offline document reader for Android

Native Android · Kotlin · Jetpack Compose · Material 3 · **Android 6.0 (API 23) → latest (API 36)**.

Leaf opens PDF · DOCX · XLSX · PPTX · TXT · EPUB and treats *resuming* as the core value: every
document remembers its page, zoom, reading theme and orientation, and every piece of organisation —
folders, collections, tags, favorites, highlights, bookmarks — belongs to the document, not to a
cloud account.

The design contract lives in [`design/`](design/): `LEAF-MASTER-PROMPT.md` (the specification),
`LEAF-SPEC.md` (product + data model), `leaf-mockup-v2.html` (the interactive mockup) and
`qa/mockup-ref/` (the reference screenshots).

---

## Status · Milestones 1–2 — Foundation + Library & organisation

M1 established the Android foundation; M2 is now complete and green in CI. The default Library is a
native Compose screen with live filters, grouping, sorting, organisation sheets and document actions.
The milestones remain scoped: the rows that can genuinely work are live, while M3+ entry points are
left out rather than displayed as dead buttons.

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

**Not in this build** — the reader (M3), advanced Search and its filter panel (M4), tabs and
pick-a-document mode (M5), sharing (M6), full Settings controls (M7) and the remaining M8 scope.
Recents, Favorites, Search and Settings still show their M1 scaffolds until their milestones.

## Build & verify

```bash
./gradlew lint testDebugUnitTest assembleDebug   # what CI runs
./gradlew recordRoborazziDebug                   # regenerate the screenshot baselines
./gradlew verifyRoborazziDebug                   # check the baselines (committed in app/src/test/snapshots)
python3 tools/gen_seed_content.py                # regenerate SeedContent.kt from the mockup
python3 tools/gen_launcher_icon.py               # regenerate the legacy launcher PNGs
```

The session's draft PR is pinned to `arena/ceade348-leaf`; its CI uploads `leaf-debug-<sha>` (an
installable debug APK) and `rendered-screens-<sha>` (the screenshots).

## Screenshots

Roborazzi renders the five destinations at 360 / 412 / 700 dp in light and dark and commits the
baselines to `app/src/test/snapshots/`. The six Library baselines show the seeded rows, three filter
rails, star/progress states, group and sort controls; the other four destinations remain the M1
scaffolds. The Library screenshot harness waits for the Room flow and uses a fixed clock so record
and verify see the same content.

| Destination | 360 dp | 412 dp | 700 dp |
|---|---|---|---|
| Library | `library_360_light` · `library_360_dark` | `library_412_*` | `library_700_*` |
| Recents | `recents_360_*` | `recents_412_*` | `recents_700_*` |
| Favorites | `favorites_360_*` | `favorites_412_*` | `favorites_700_*` |
| Search | `search_360_*` | `search_412_*` | `search_700_*` |
| Settings | `settings_360_*` | `settings_412_*` | `settings_700_*` |

## Feature coverage matrix (§14)

The definition of done. A row is ticked only when the feature works end to end and has been verified
by running it.

| # | Feature group | Source | Status |
|---|---|---|---|
| 1 | Recents — auto tracking, hero, continue grid, today/earlier, per-entry remove, clear today, clear all, badge | Mockup §6.2 | ☐ |
| 2 | Favorites — star toggle, own sort (name/added/opened) | §6.3 | 🟨 partial — the Library star toggle is live; Favorites screen and its independent sort are later |
| 3 | Page Stay — position, offset, zoom, scroll dir, theme, orientation; progress everywhere | §2.2 | ☐ |
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
| 16 | File actions — rename, delete (+undo), move, tags, favorite, info | §6.7 | 🟨 partial — these M2 actions are live; Open/new-tab/Share wait for their reader/tab/sharing milestones |
| 17 | Reading themes — Paper, Sepia, Night, OLED, Auto | §4.2 | ☐ |
| 18 | Scroll modes — vertical, horizontal with snap+page-turn | §6.6 | ☐ |
| 19 | Zoom & pan — pinch, double-tap, buttons, ctrl-wheel, drag-pan, 70–250 % | §6.6 | ☐ |
| 20 | Text reflow — all formats, anchored to pages | §7.2 | ☐ |
| 21 | Full screen — hides all chrome, dim footer, Esc/tap exit | §6.6 | ☐ |
| 22 | Rotate — auto toggle, per-document lock (auto/portrait/landscape) | §5 | ☐ |
| 23 | Settings — appearance, reading, device, data, about | §6.5 | ☐ |
| 24 | Responsive + platform — bar/rail, 2/3-col, foldable, toolbar priority ladder, API 23 → 36, i18n, a11y | §5, §9, §10 | 🟨 partial — bar/rail, 2-col grid metrics and API 23 → 36 support are in; 2-col *lists*, the toolbar ladder, i18n and the a11y pass land with their screens |
| 25 | Highlight colour menu — expanding labelled palette, live swatch, selection-aware hint, dismiss rules | §6.6 | ☐ |
| 26 | Excel `.xlsx` — sheet-per-page grid rendering, text, find, highlights, reflow | §7.3 | ☐ |
| 27 | PowerPoint `.pptx` — slide-per-page card rendering, text, find, highlights, reflow | §7.3 | ☐ |

## Verification

The final M2 branch run is green: [build, lint, all unit tests, APK assembly and screenshot parity](https://github.com/ab2024103-cmd/Leaf/actions/runs/37651118098).

| Job | Result |
|---|---|
| Build, lint & unit tests | ✅ |
| Assemble debug APK | ✅ |
| Screenshot parity — record + verify | ✅ |
| Release APK + AAB | skipped — only runs on `v*` tags |

- **Installable debug APK:** [download](https://github.com/ab2024103-cmd/Leaf/actions/runs/37651118098/artifacts/11496925848)
- **Rendered screenshots:** [download](https://github.com/ab2024103-cmd/Leaf/actions/runs/37651118098/artifacts/11496970301)
- **Committed Library baselines:** `app/src/test/snapshots/library_{360,412,700}_{light,dark}.png`

The 89 tests include sorting in both directions, filter composition/count context, all seven smart
rules, folder subtree/group counts, the empty-group fallback, rename-extension behavior, delete+Undo,
folder/tag delete safety, and the 470 ms row long-press/tap flow; the 30 Roborazzi captures cover five
destinations × three widths × light/dark.

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
9. **Demo documents use a `leaf-demo://` URI.** They are content, not files on disk, so their text
   lives in the `extracted_text` cache. M2 does not add an Import screen or a fake SAF result; real
   document intake is deferred to the milestone that implements the reader/open flow.
10. **No `design/leaf_tokens.json` in this repo.** The token set was re-derived from the mockup's CSS
    and compared against §4; the two agree everywhere except deviation 4.
11. **Row tap stays inert until M3.** This was explicitly approved for M2. A long-press still opens the
    actions sheet; the row has no dead click target.
12. **Future actions are omitted rather than stubbed.** The M2 actions sheet omits Open, Open in new
    tab and Share until M3/M5/M6 can perform them. Pick-a-document mode is deferred to M5 because its
    entry points are part of the tabs flow.
13. **The §6.1 filter glyph is decorative in M2.** The advanced filter panel is part of the later
    Search scope; no inert tappable filter button is exposed.
14. **Sort direction follows the labels.** “Ascending” shows A–Z / Newest / Recent / Largest, matching
    the mockup's visible sort copy. The mockup's comparator reverses the three numeric/time fields
    against those labels; Leaf keeps the labels and makes the order agree with what the user sees.
15. **Milestone branch naming is session-pinned.** The Arena session is fixed to
    `arena/ceade348-leaf`; the existing PR carries M1 + M2 rather than creating `m2-library`.

## What's next — M3 · Reader

Build the real document-open path and reader while keeping the M2 Library rows, actions, filters,
counts, file-type groups and persisted sort settings intact.
