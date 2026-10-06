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

## Status · Milestone 1 — Foundation

M1 builds the ground the rest of the app stands on. It is intentionally the smallest milestone that
is *complete* rather than the largest that compiles.

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

**Not in this build** — document rows, filter rails, the sort sheet, the folder and tag managers,
search and the reader. They arrive in M2–M8 with the features that make them work, so nothing on
screen is a stub, a placeholder or a dead button.

## Build & verify

```bash
./gradlew lint testDebugUnitTest assembleDebug   # what CI runs
./gradlew recordRoborazziDebug                   # regenerate the screenshot baselines
./gradlew verifyRoborazziDebug                   # check the baselines (committed in app/src/test/snapshots)
python3 tools/gen_seed_content.py                # regenerate SeedContent.kt from the mockup
python3 tools/gen_launcher_icon.py               # regenerate the legacy launcher PNGs
```

Every push opens a draft PR whose CI run uploads `leaf-debug-<sha>` (an installable debug APK) and
`rendered-screens-<sha>` (the screenshots).

## Screenshots

Roborazzi renders the five destinations at 360 / 412 / 700 dp in light and dark and commits the
baselines to `app/src/test/snapshots/`. The screenshots are of the **empty** states: M1 ships the
scaffolds, and the seeded library is proven by unit tests rather than by rows that M2 has not built
yet.

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
| 14 | Multi-tab — Open documents sheet + badge, pick-a-document mode, per-tab state, close any row incl. the active/last one, close all from n≥1, undo, configurable limit, back keeps tabs | §6.6 | ☐ |
| 15 | Share — document, page image, selection, copy path, copy page text | §6.7 | ☐ |
| 16 | File actions — rename, delete (+undo), move, tags, favorite, info | §6.7 | ☐ |
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

## Deviations from the mockup and the spec

Every deviation is a decision, not an accident.

1. **Screens are empty scaffolds.** The mockup shows a full library; M1 shows each destination's app
   bar and its exact empty-state copy. The seed — 10 documents, 5 folders, 7 tags, 6 collections,
   40 pages of text, highlights and bookmarks — is in the database and verified by
   `SeedDataTest`, but rows are M2. This is the milestone scope that was agreed before any code was
   written: nothing on screen is a stub.
2. **`ci/android.yml` could not pass as-is.** Its release job used
   `if: ${{ secrets.KEYSTORE_BASE64 != '' }}`; GitHub rejects the `secrets` context inside an `if:`
   expression, which invalidated the *entire* workflow file — every run of it produced zero jobs, on
   `main` as well as on branches. The secrets are now surfaced as env vars and the signed/unsigned
   decision is taken in the shell. Behaviour is unchanged: signed when the keystore secrets exist,
   unsigned with a warning in the release notes when they do not.
3. **XLSX / PPTX dark file-icon tones.** §4.6 gives the light values as hexes but states the dark
   rule only in words ("containers at 30 % tone, labels at 90 %"). The dark values are taken from
   the mockup's own CSS for those two types; the other four types match both sources.
4. **Dynamic colour.** §1.4 requires an optional "Match system colours (Android 12+)" switch, which
   the mockup does not have. It exists in settings as `matchSystemColors`, default **off**, and is
   wired to `LeafTheme`; the Settings row that flips it lands in M7.
5. **Demo documents use a `leaf-demo://` URI.** They are content, not files on disk, so their text
   lives in the `extracted_text` cache. Real documents will arrive through the file picker and the
   open-with intents in M2/M6.
6. **No `design/leaf_tokens.json` in this repo.** The token set was re-derived from the mockup's CSS
   and compared against §4; the two agree everywhere except deviation 3.

## What's next — M2 · Library & organisation

Document rows (file icon, meta line, tag chips, progress row, star), the three filter rails with live
counts, the sort sheet, the folder and tag managers, the long-press actions sheet, and
rename / move / delete with Undo — with unit tests for the sort comparators and filter composition.
