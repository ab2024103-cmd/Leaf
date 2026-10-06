# 🌿 Leaf — Document Viewer
## Product & Technical Specification (v2.0)

**Platform:** Native Android (Kotlin + Jetpack Compose, Material 3)
**Support range:** Android 6.0 Marshmallow (API 23) → latest (API 36)
**Build target:** `minSdk 23` · `targetSdk 36` · `compileSdk 36`
**Visual source of truth:** `leaf-mockup-v2.html` (this repo) + `design/leaf_tokens.json`
**Status:** Ready to build · 24 feature groups · 0 dropped items

---

## 1. Product summary

Leaf is an offline-first document reader for Android. It opens PDF, DOCX, XLSX, PPTX, TXT and EPUB files
and treats *resuming* and *remembering* as the core value: every document remembers its page,
zoom, reading theme and orientation, and every piece of organisation — folders, collections,
tags, favorites, highlights, bookmarks — belongs to the document, not to a cloud account.

**Design language:** Material 3 (tonal surfaces, state layers, shared-axis motion) seeded from
Leaf Green `#2B7A5B`, warmed by the reading canvas themes (Paper, Sepia, Night, OLED).

**Two independent theme axes — this is a defining product decision:**
| Axis | Controls | Options |
|---|---|---|
| **App theme** | Chrome: app bars, nav, lists, sheets, dialogs | Light · Dark · System |
| **Reading theme** | The document canvas only | Paper · Sepia · Night · OLED · Auto (system) |

---

## 2. Feature specification

### 2.1 Library & organisation

**Recents**
- Every document open is logged automatically (`docId`, timestamp). Opening the same document again
  moves it to the top instead of duplicating.
- Home for "Jump back in": hero card for the most recent document with page position, relative time,
  progress bar and a **Resume reading** action, plus a *Continue · last 5* cover-tile grid.
- List is grouped into **Today** and **Earlier**.
- Individual removal: an ✕ button on each row removes that entry from history (files untouched),
  with an Undo snackbar.
- Bulk: **Clear today** in the header and **Clear all recents** via the app-bar action (confirmation dialog).
- Recents count badge on the navigation item (fresh = opened within the last 24 h).
- Recents never deletes or mutates documents.

**Favorites**
- Star/unstar from the list row, the long-press actions sheet, or the reader menu. Star state is instant
  and haptic-confirmed.
- Dedicated destination with its **own sort control**: Name · Date added · Last opened.
- `favAt` timestamp is recorded so "date added" means date *starred*.

**Folders & collections**
- Custom folders with name + colour (10-swatch palette) + optional parent (one nesting level minimum,
  unlimited depth supported by the model).
- Create, rename, recolour, delete. Deleting a folder re-parents its documents to the parent folder or
  "Not filed" — documents are never deleted with a folder.
- Folder manager screen lists folders with document counts, subfolder counts, inline rename/recolour/delete.
- **Smart collections** (auto-updating by rule): All PDFs · All documents · Added this week ·
  Added this month · Still reading · Not filed · Has highlights. Users may add more; duplicates are rejected.
- Folder chips in the library show live counts; nested folders are indented in the move picker.

**Tags / labels**
- Create, rename, recolour (10-swatch palette), delete. Renaming a tag cascades to every document.
- Multiple tags per document; library filtering uses **AND** semantics when several tags are selected.
- Tag manager destination lists usage counts; tapping a tag filters the library.
- Deleting a tag removes it from all documents (documents untouched).

### 2.2 Reading experience

**Page Stay (resume reading)**
- Persists per document: page index, intra-page scroll offset, zoom, scroll direction, reflow state,
  reading theme override and orientation preference.
- Auto-resume on reopen when *Remember last page* is on.
- Progress is surfaced three ways: footer "Page n of m", percentage read, and a progress bar that is
  also shown on list rows ("57% · p4").
- Search-result and highlight/bookmark jumps always persist the new position.
- Optional **bookmarks**: multiple per document, from the page flag, the toolbar bookmark button, or
  `Ctrl/⌘+B`; managed in a Bookmarks sheet (jump, delete, add current page).

**Viewing modes**
| Mode | Behaviour |
|---|---|
| Paper / Sepia / Night / OLED | Reading-canvas themes; text, rules and links are re-toned, highlight colours are tone-matched so they stay legible |
| Auto | Follows system dark mode at the moment of render |
| Vertical scrolling | Continuous vertical page list |
| Horizontal scrolling | Page-per-screen with snap, swipe like a book |
| Zoom & pan | Pinch, double-tap (1.0 ⇄ 1.6), ctrl-wheel and −/+ controls, range 70 %–250 %; drag-to-pan while zoomed |
| Text reflow | Reflows extracted text to the screen width, single column, no horizontal scroll |
| Full screen | Hides tabs, app bar, toolbar and highlight bar; keeps a dimmed footer that restores on touch |
| Rotate | Auto-rotate toggle, manual cycle, and a **per-document orientation lock** (auto / portrait / landscape) |

Zoom % is always visible in the toolbar; page numbers can be hidden per user preference.

### 2.3 Search & navigation

**Advanced search**
- Scopes: Everything · File name · Tag · Folder · **Inside files** (full-text).
- Combined filters (AND): type, folder, tag, added-within (7 / 30 / 90 days).
- Result summary: "N documents · M files contain “q” (K hits)".
- Inside-file results show a match snippet with the query emphasised, match count and first page.
- Tapping a result opens the document at that page with the find bar pre-filled.
- **Search history**: last 10 distinct queries, individually removable, clearable, re-runnable; Enter commits a query.
- Empty/blank state offers quick searches (All PDFs, Added this week, Folder · Work, Tag · important, Word · chapter).

**Sorting**
- Field: name · date added · last opened · file size.
- Direction chosen explicitly (Ascending / Descending), displayed as "Name · A–Z".
- Favorites has its own independent sort.
- The chosen field and direction also apply inside an open file-type group (see Groups below).

**File-type groups**
- A group-by-type toggle turns the library into an overview of file-type cards (icon, document count, total
  size, highlight and bookmark counts) in the fixed order PDF → DOCX → XLSX → PPTX → TXT → EPUB.
- Tapping a card drills into that type's own file list, with a crumb bar showing the type, its document count
  and the active sort; the full sort control is available there and stays inside the group.
- Back, Esc or the crumb arrow returns to the overview; switching the toggle off returns to the flat list.
- Folder, collection, tag, advanced-filter and search filters apply to both the overview counts and the group.

### 2.4 Annotation & interaction

**Highlighting**
- 5 colours: Yellow, Green, Blue, Pink, Orange.
- Select text → tap a colour (or the colour pre-selected) to highlight.
- Tap an existing highlight → context bar with recolour swatches, **Remove**, **Copy** (text to clipboard), Done.
- **Undo / Redo** for add, remove, recolour, page-clear and full-clear — 50-step stack, with snackbars
  offering Undo for destructive actions.
- **Highlights summary panel** per document: colour chip, page number, quoted text, timestamp; tap to jump,
  delete inline, or clear all.
- Highlights are stored with page + normalized geometry (PDF and the paged Office formats XLSX/PPTX) or text offsets (reflow/TXT/DOCX/EPUB) + selected text
  so they survive zoom and layout changes.

**Multi-tab viewing** (the tab UI lives in the shell, not in the reader)
- The library and reader app bars carry an **Open documents** button with a live count badge (hidden at zero).
- Tapping it opens the **Open documents** sheet: one row per open document with type icon, name,
  `Page 3 of 5 · 2 h ago`, an ACTIVE marker on the current one and an × on the rest; then
  "Open a new document…" and "Close all tabs" (with confirmation).
- Every row carries an × including the active one, so the last open document can always be closed; tapping a row
  (active or not) opens it in the reader, and "Close all tabs" is offered whenever one or more are open.
- "Open a new document…" starts **pick-a-document mode**: a banner over the library, dashed rows and a ＋
  affordance on every document; choosing one opens it in a new tab and ends the mode (Cancel/back also ends it).
- Switching restores that document's page, zoom, scroll direction, reading theme and find state.
- **Leaving the reader never closes a document** — back returns to the shell with the tab intact
  (first time: "N documents kept open · Tap Tabs in Library to come back"); closing a tab offers Undo.
- **Tab limit is configurable** (1–10, default 6) in Settings; adding beyond it is refused with
  "Tab limit reached (n) — close a tab or raise the limit in Settings".

**Share**
- Share the whole document through the system share sheet.
- Share the current page as an image (PNG).
- Share the current text selection.
- Copy file path, or copy the current page's text to the clipboard.
- *(Out of scope by decision: cloud links, export highlights as a list.)*

### 2.5 File management

- **Rename** (extension is preserved when omitted), **Delete** (confirmation dialog, Undo snackbar),
  **Move to folder** (including "Remove from folder"), **Add/remove tags**, **Add/remove favorites**.
- **File info**: type, size, page count, date added, last opened, reading position, bookmark count,
  highlight count, folder, tags.
- Long-press any row for the full actions sheet; the reader overflow offers the same actions in context.
- *(Out of scope by decision: duplicate.)*

### 2.6 Settings

**Appearance** — App theme (Light/Dark/System) · default reading theme · Animations · Haptic feedback · Language
**Reading** — default scroll direction · text reflow default · default zoom · reading typeface (Serif/Sans) ·
remember last page · page numbers on pages
**Device** — auto-rotate · lock orientation per document · maximum open tabs
**Data & storage** — clear recents · clear search history · clear reading positions · clear all highlights ·
reset demo library
**About** — spec coverage list · version · counts

**Explicitly out of scope** (per product decision): the entire Import section (local storage, cloud services,
recent downloads, batch import), Export highlights as a list, Share a link to the file, Duplicate.

---

## 3. Information architecture

```
Bottom navigation (compact, < 600 dp)      Navigation rail (medium/expanded, ≥ 600 dp)
├── Library        ← default                ├── Library
├── Recents  (badge)                        ├── Recents
├── Favorites                               ├── Favorites
├── Search                                  ├── Search
└── Settings                                └── Settings

Reader (full screen, above the shell)
├── App bar (back · title/subtitle · Open documents + count badge · bookmark · overflow)
├── Find bar (collapsible)
├── Document viewport (vertical | horizontal, zoomable, reflowable)
├── Footer (page label · progress · % · bookmark count)
└── Toolbar (Highlight trigger + colour menu · undo · redo · find · view & layout · zoom −/%/+ · highlights · full screen · share)
    ├── Highlight colour menu (expands from the trigger: 5 labelled colours + hint) — closes on outside tap/back/sheet
    └── Highlight context bar (recolour · remove · copy · done) — replaces the toolbar when a highlight is selected
```

Overlays: bottom sheets (actions, pickers, sort, jump-to-page, view & layout, share, manager screens),
confirm dialogs, snackbars (place them above the navigation bar).

---

## 4. Data model

```kotlin
Document(
  id, name, type(PDF|DOCX|XLSX|PPTX|TXT|EPUB), sizeBytes, uri,
  dateAdded, lastOpened, fav, favAt, folderId?, orientation(auto|portrait|landscape),
  progress: Progress(page, scrollFraction, zoom, scrollDir, updatedAt),
  tags: List<String>,                    // normalised tag names
  bookmarks: List<Bookmark(id, page, label, createdAt)>,
  highlights: List<Highlight>            // see below
)

Highlight(
  id, page, color(YELLOW|GREEN|BLUE|PINK|ORANGE),
  text: String,                          // selected text (for the summary panel + copy)
  bounds: List<NormalizedRect>,          // PDF: 0..1 page space, one per line
  textRange: IntRange?,                  // reflow / TXT / DOCX / XLSX / PPTX / EPUB: character offsets
  cfiRange: String?,                     // EPUB
  createdAt
)

Folder(id, name, colorHex, iconKey, parentId?, isSystem)
Tag(name, colorHex)
SmartCollection(id, name, rule(kind, …)) // type | ageDays | inProgress | unfiled | hasHighlights | tag
RecentEntry(docId, openedAt)             // capped at 60, unique per docId
Settings( see §2.6 ) + searchHistory: List<String>   // capped at 10
ReaderTab(docId, page, zoom, scrollDir, findQuery)
```

Persistence: **Room** for documents, folders, tags, collections, recents, bookmarks, highlights;
**DataStore (Preferences)** for settings, navigation state and reader tabs; document bytes stay where the user
granted them (SAF / app storage) — Leaf never copies a file it does not have to.

---

## 5. Responsive requirements

| Width class | Navigation | Lists | Reader |
|---|---|---|---|
| Compact `< 600 dp` (phones, 360–599) | Bottom navigation bar, 5 items, 62×30 dp pill, icon + 11.5 sp label | Single column, 12 dp gutters | Chrome stacked; toolbar horizontally scrollable; zoom % centred |
| Medium `600–839 dp` (foldables, tablets portrait) | Navigation rail, 84 dp, icon above label | Two-column cards | Pages centred, max 720 dp measure |
| Expanded `≥ 840 dp` (tablets landscape, desktop) | Navigation rail | Two/three-column | Pages centred, max 720 dp, toolbar centred |

Rules: layouts use `WindowSizeClass` + `WindowInsets`; no hard-coded phone widths; every screen must look
*organised*, not stretched, at 360 dp and at 900+ dp; support foldable hinge/posture (keep content out of the
hinge) and free-form resizing; free rotation with per-document lock override.

---

## 6. Non-functional requirements

- **Offline-first:** no network permission is required for the core product.
- **Performance:** cold start < 800 ms on a mid-range API 23 device; 60 fps list scrolling; PDF pages render
  progressively (thumbnail → tiles → text layer) with an LRU bitmap cache sized to the device heap class.
- **Memory:** never hold more than 3 full-resolution page bitmaps; recycle; cap cache by `maxMemory / 8`.
- **Accessibility:** every icon has a content description; touch targets ≥ 48 dp; lists and toolbars support
  TalkBack traversal order; text scales with system font size (text reflow absorbs the change); no colour-only
  meaning (tags/highlights carry text labels).
- **Internationalisation:** all strings in `strings.xml`; no concatenated sentences; RTL mirroring works
  (Arabic); 9 UI languages at launch: English, Español, Français, Deutsch, Português, العربية, Yoruba, हिन्दी, 日本語.
- **Data safety:** destructive actions always confirm (delete document/folder/tag, clear recents/positions/
  highlights/reset) and offer Undo where the data still exists.

---

## 7. Acceptance criteria (product level)

1. Every row in §2 exists and is reachable within 2 taps/sheets from a top-level destination.
2. Opening a document, closing it and reopening it returns to the exact page, offset, zoom and theme.
3. Killing the app (process death) and reopening preserves library, organisation, highlights, bookmarks,
   recents, settings and open tabs.
4. In-file search finds every occurrence, highlights them all, reports "n of m", and jumping lands on the match.
5. Highlights survive zoom changes, theme changes, scroll-direction changes and app restarts.
6. Tab switching preserves per-tab position; closing a tab returns to the previously active tab.
7. Deleting a folder or tag never deletes documents; deleting a document is undoable from the snackbar.
8. All five colours are distinguishable in each of the four reading themes.
9. The app installs and runs on API 23 without crashes and without rendering artefacts across 360 dp → tablet.
10. No feature from §2 is stubbed: no "TODO", no dead buttons, no placeholder text in the shipped build.
