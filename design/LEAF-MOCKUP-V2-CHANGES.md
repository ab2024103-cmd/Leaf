# 🌿 Mockup v1 → v2 — what changed

**v1** = your original `leaf mock.html` (interactive prototype, earthy-green custom UI).
**v2** = `leaf-mockup-v2.html` (same product, Material 3 modernised, expanded to the full updated spec).

---

## 1 · Removed exactly as you asked

| Removed | What it means in v2 |
|---|---|
| **Export highlights as a list** | Gone. The Highlush sheet only offers jump / delete / clear all. |
| **Share a link to the file (cloud-backed)** | Gone. Share sheet is now: document · page as image · selected text · copy file path · copy page text. |
| **Entire Import section** (local storage, cloud services, recent downloads, batch import) | Gone — there is no Import screen anywhere. Documents enter through the system file picker and the share/open-with intents (documented in the build prompt §2). |
| **Duplicate** (from file actions) | Gone from the actions sheet and the reader menu. |

## 2 · Added to close every gap against your spec

| Gap in v1 | Added in v2 |
|---|---|
| Folders could only be created, never managed | **Folder manager**: rename · recolour (10 swatches) · delete (re-parents documents) · **nested subfolders** |
| No smart collections | **Collections** rail with 7 auto rules (All PDFs, Added this week/month, Still reading, Not filed, Has highlights…) + "add collection" |
| Single tag filter only | **Multi-tag AND filtering**, plus a **tag manager** (rename cascades to documents, recolour, delete) |
| Favorites used the global sort | Favorites has its **own sort**: Name · Date added · Last opened |
| Sort had no direction | Sort sheet with **field + Ascending/Descending** segmented control ("Name · A–Z") |
| Only PDF/DOCX/TXT were modelled | **Excel (`.xlsx`) and PowerPoint (`.pptx`)** documents are first-class: type icons (`.xlsx` green, `.pptx` amber), their own filter chips, a **Spreadsheets** smart collection, type-aware sorting and grouped headings |
| Search had scopes but no combined filters, no history | **Filter panel** (type × folder × tag × added-within, with an active count), **search history** (10, removable, re-runnable) |
| Recents could only be cleared entirely | **Per-entry removal** (with Undo), **Clear today**, plus a nav badge for the last 24 h |
| No bookmarks | **Bookmarks**: page flag on every page, toolbar button, sheet with jump/delete, footer count |
| Zoom was buttons only | **Pinch zoom, double-tap zoom (1.0 ⇄ 1.6), ctrl-wheel, drag-to-pan**, live zoom % readout |
| Orientation wasn't per document | **Per-document orientation lock** (auto / portrait / landscape) in View & layout |
| Tab limit was hard-coded at 6 | **Configurable tab limit (1–10)** in Settings, surfaced in the Open documents sheet ("2 of 6 tabs open") and in the pick-a-document banner |
| One theme for the whole app | **Two axes**: App theme (light/dark/system) *and* reading theme (Paper/Sepia/Night/OLED/Auto) |
| No rename action | **Rename** everywhere (extension preserved) |
| Clear recents / search history were partial | Data section: clear recents · clear search history · clear reading positions · **clear all highlights** · reset library — each confirmed |
| Toast only, no undo | **Snackbar with Undo** for deletes and highlight removals, positioned above the navigation bar |

## 3 · Visual modernisation (Material 3)

- Palette regenerated as a real M3 **TonalSpot scheme** from seed `#2B7A5B` (full tonal ramps, light + dark
  role sets, container/on-container pairs). Values are in `design/leaf_tokens.json` and hard-coded into
  the build prompt, so what you see is what gets built.
- List rows, chips, segmented controls, sheets, dialogs, switches, FAB and snackbars follow M3 metrics
  (28 dp sheet corners, 4 dp snackbar, 16 dp cards, state layers, tonal file-type icons).
- **Responsive for real:** bottom navigation below 600 dp, **navigation rail** at ≥ 600 dp, 2-column card
  grid on medium/expanded, page measure capped at 720 dp — verified at 360 · 412 · 480 · 700 dp and landscape.
- Reader keeps its own chrome: find bar, footer progress, the Highlight trigger with its expanding colour
  menu and undo/redo, the highlight context bar, and full-screen mode with a dimmed footer. Tabs are surfaced
  by the app-bar badge + Open documents sheet instead of an in-reader strip.
- Motion: shared-axis screen transitions, ripple on every tappable surface, 250 ms emphasized sheets,
  page-turn animation in horizontal mode, haptics on long-press and highlight.

## 3b · Round-2 revision (after your second review pass)

Three changes you asked for, plus two defects the re-test exposed:

| # | Change | What it looks like now |
|---|---|---|
| 1 | **Highlight colours: one expanding menu instead of five always-visible dots** | The reader toolbar has a single **Highlight** trigger — pen glyph + a live colour swatch + caret (the swatch is always the colour that will be applied). Tapping it expands a `surfaceContainerHigh` popover: header "HIGHLIGHT COLOUR" + ×, five labelled options (Yellow · Green · Blue · Pink · Orange) with the active one on a pill, then a hint line that changes when text is selected. It closes on outside tap, Esc/back, on any sheet, on full screen and on leaving the reader. The old five-dot row is gone from the toolbar and in full screen. |
| 2 | **Tabs moved out of the reader and into the Library** | There is no browser-like tab strip inside the reader any more — the page area is taller and there is no `＋`/counter strip. Instead: • the Library app bar (and the reader app bar) carries a **tabs button with a live count badge**, hidden at 0; • it opens the **Open documents** sheet (active row tinted + ACTIVE marker, per-tab page/age, × to close a single tab, "Open a new document…", "Close all tabs" with confirmation); • back/leave **keeps documents open** — "2 documents kept open · Tap Tabs in Library to come back"; • closing a tab offers **Undo**; • **pick-a-document mode** adds the `primaryContainer` "Choose a document" banner with Cancel, dashed rows and ＋ affordances, and hides the New-folder FAB. |
| 3 | **Wider format support (Excel + PowerPoint)** | Two new seed documents (`Q3 Budget — 2026.xlsx`, `Q3 Kickoff Deck.pptx`), XLSX/PPTX type icons and filter chips, a "Spreadsheets" smart collection and a third sort field — later replaced by the dedicated **file-type groups view** (see row 6). |
| 4 | **Fix — toolbar overflow** | The new Highlight trigger made the reader toolbar wider than a 360/412 dp frame, pushing full screen and share off-screen. The toolbar now follows an explicit priority ladder: hidden zoom group at 360 dp, hidden `%` label at 412 dp, hidden highlights-list button below 600 dp, and the full-screen/share group is pinned right. Measured overflow is **0 px at 360 / 412 / 480 / 700 dp** and the row never scrolls. |
| 5 | **Fix — hidden snackbar under sheets** | A visible snackbar used to sit on top of an opening sheet (it hid the "Open a new document…" row). Opening any sheet now dismisses the snackbar first. |
| 6 | **"Sort by file type" replaced by a file-type groups view** | Sorting by file type used to dump a grouped flat list under inline headings. It is now a **drill-in view**: the section header gained a **group-by-type toggle** and File type left the Sort sheet (fields are now Name, Date added, Last opened, File size). Grouped, the library shows one **card per file type** in the fixed order PDF → DOCX → XLSX → PPTX → TXT → EPUB, each with icon, document count, total MB, highlight/bookmark counts and a chevron — header reads `10 DOCUMENTS · 5 TYPES`. Tapping a card opens that type's **own file list** under a crumb bar (`‹ [PDF] PDF documents ‹ 5 documents · Name · A–Z`) where the **sort chip is available again**; sorting re-orders the group, persists to the whole library and stays inside the group. Back, Esc or the crumb arrow returns to the overview; the toggle returns to the flat list. Folder/collection/tag/advanced filters apply to both the overview counts and the group list. |
| 7 | **Fix — the last open document could not be closed** | In the Open documents sheet only the *inactive* rows had an ×, and "Close all tabs" appeared only with 2+ tabs — so with one document open there was no way to close it. Every row now has an × (the active one keeps its ACTIVE dot as well) and **Close all tabs** is offered whenever anything is open, confirming with `Close all tabs?` and the "reading positions are saved" body (the button reads **Close it** for a single tab). Tapping the **active** row now opens that document in the reader — it previously did nothing unless the reader was already open — and closing the last tab shows the sheet's empty state plus the snackbar "Tab closed — pick a document any time". |

## 4 · Verified behaviour (the mockup was driven by an automated test sweep)

45+ interactions were exercised with zero console errors, including: create/rename/recolour/delete folder and
subfolder · add smart collection · tag rename cascade · sort field + direction · multi-tag filter ·
remove & undo a recent entry · clear recents · search history run/delete/clear · open at page from a search hit ·
find-in-file next/prev · jump-to-page slider · bookmark add/jump/delete · open documents sheet (switch / close one / close all / undo) · pick-a-document mode · back keeps documents open · tab badge · highlight colour menu (open, pick, swatch, Esc, outside-tap) · toolbar fits at 360/412/480/700 dp · file-type groups (overview → drill in → sort inside a group → back) + XLSX/PPTX rows and details ·
highlight add/recolour/copy/remove · undo/redo · highlights sheet jump + delete · rename document ·
share sheet actions · file info · delete document + undo · every settings toggle ·
clear positions / clear highlights / reset library / clear recents confirmations.

Bugs found during those sweeps and fixed: creating a folder from the folder manager now returns to the
manager instead of closing the flow · the reading-position writer (`saveProgress`) was referenced but never
defined, so scroll positions were not persisted · the toolbar overflow and snackbar-under-sheet defects
listed above, plus the unclosable-last-tab dead end and a `[hidden]`/`display:flex` clash that leaked the group
crumb bar into the overview state. `qa/suite.py` now runs **74 checks across 10 groups** (it is the regression
gate — run it after any edit) and reports 74/74 with **0 console errors** and **0 px toolbar overflow** at every
frame size.

## 5 · Files in this package

| File | Purpose |
|---|---|
| `leaf-mockup-v2.html` | The interactive mockup — the visual contract for the build |
| `LEAF-MASTER-PROMPT.md` | The master build prompt (paste into your AI builder) |
| `LEAF-KICKOFF-PROMPT.md` | Short per-session starter prompt (M1 scoped) |
| `LEAF-BUILD-CHECKLIST.md` | Milestone gates, screen × size × theme QA grid, device matrix |
| `LEAF-SPEC.md` | Product & technical specification (data model, acceptance criteria) |
| `design/leaf_tokens.json` | Generated M3 tokens (roles, reading palettes, highlights, type, shape, motion) |
| `design/gen_palette.py` | The script that generates the tokens from the seed colour |
| `qa/suite.py` · `qa/capture.py` · `qa/setup-playwright.sh` | The regression suite (56 checks), the screenshot regenerator and the one-shot browser setup | Re-verify and re-shoot the mockup after every edit |
| `qa/mockup-ref/*.png` | 26 reference screenshots: every v2 screen at 360 / 412 / 700 dp, light + dark, the reader themes, and the round-2 surfaces (Open documents sheet light + dark, pick-a-document, highlight colour menu light + night, landscape reader, **file-type overview at 412 dp and 700 dp tablet, one open file-type group**) |
