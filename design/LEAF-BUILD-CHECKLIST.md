# 🌿 LEAF — BUILD CHECKLIST & QA MATRIX

Use this alongside `LEAF-MASTER-PROMPT.md`. Tick items only when they were **verified by running the app**.
Every behaviour below was implemented and tested in `leaf-mockup-v2.html` — the app must behave identically.

---

## A · Milestone gates

### M1 · Foundation
- [ ] Project builds on JDK 17 with `minSdk 23` / `targetSdk 36` / `compileSdk 36`
- [ ] Core library desugaring enabled; no API > 23 call unguarded
- [ ] Material 3 theme matches the token table (light **and** dark) — verified against the mockup hexes
- [ ] Reading palettes Paper/Sepia/Night/OLED implemented and swappable
- [ ] Noto Serif bundled and used for reading surfaces
- [ ] Bottom navigation (compact) and navigation rail (≥ 600 dp) with 5 destinations
- [ ] Edge-to-edge insets correct on API 23 and API 36 (status bar icon contrast, nav bar clearance)
- [ ] Room + DataStore schema created and migrating cleanly; seed data populates the library
- [ ] App survives process death with the seed data intact

### M2 · Library & organisation
- [ ] Document rows: file icon, name, pages/size/bookmark/age meta, tag chips, progress row, star
- [ ] Folder chips with live counts; smart-collection chips with counts; tag chips with multi-select AND filter
- [ ] Sort sheet: 5 fields × ascending/descending; chip label shows "Name · A–Z"
- [ ] Folder manager: create, rename, recolour, delete, nest, subfolder; deletes re-parent documents
- [ ] Tag manager: create, rename (cascades), recolour, delete
- [ ] Actions sheet (long-press 470 ms + haptic): open, open in new tab, rename, favorite, tags, move, share, info, delete
- [ ] Rename preserves the extension when the user omits it
- [ ] Delete → confirm → snackbar with Undo that restores highlights and bookmarks
- [ ] Empty state copy exact

### M3 · Reader core
- [ ] PDF renders through `PdfRenderer` with progressive tile loading; no crash on a damaged file
- [ ] Zoom 70 %–250 %: pinch, double-tap toggle, −/+, ctrl-wheel; drag-to-pan while zoomed
- [ ] Vertical continuous scroll and horizontal snap scrolling with page-turn animation
- [ ] Tab strip, app bar (back/title/subtitle/bookmark/overflow), footer (page · bar · % · bookmarks), toolbar
- [ ] Jump-to-page sheet: slider, stepper, First/25/50/75/Last, "Go to page n"
- [ ] Page Stay: page + offset + zoom + scroll direction + theme persist through close and process death
- [ ] Full screen hides all chrome; footer dims and restores on touch; Esc/tap exits

### M4 · Search
- [ ] Scopes: Everything / Name / Tag / Folder / Inside files
- [ ] Combined filters: type, folder, tag, added-within (+ active count badge, Reset, Apply)
- [ ] Summary banner with document, file and hit counts
- [ ] Inside-file results: serif snippet with the query emphasised, match count, first page
- [ ] Tapping a result opens the document at that page with the find bar pre-filled
- [ ] In-file find: mark all, "n of m", prev/next scrolling to centre, close clears marks
- [ ] Search history: last 10, de-duplicated, per-item delete, clear all, re-runnable, Enter commits

### M5 · Annotation & tabs
- [ ] 5 highlight colours; select → tap colour; tap highlight → context bar (recolour / remove / copy / done)
- [ ] Undo/redo for add, remove, recolour, page-clear and full-clear with labelled snackbars
- [ ] Highlights survive zoom, scroll-direction change, theme change and app restart
- [ ] Highlights summary sheet: quote, page, colour chip, timestamp, jump, delete, clear all
- [ ] Bookmarks: page flag, toolbar button, shortcut, sheet with jump/delete, count in footer
- [ ] Multi-tab: per-tab page/zoom/scroll/find state, close one (returns to previous), close all, configurable limit 1–10

### M6 · Formats & sharing
- [ ] TXT, DOCX, XLSX and PPTX (OOXML zip + XmlPullParser) and EPUB (Readium) open, paginate and remember position
- [ ] Text reflow available on every format and keeps page anchoring
- [ ] Extracted text cached per document so in-file search is instant on reopen
- [ ] Share: document, page as image, selection, copy path, copy page text
- [ ] Share targets open; page image is a real PNG of the current page

### M7 · Settings, polish, i18n
- [ ] Appearance / Reading / Device / Data / About groups with every row from the spec
- [ ] Tab limit stepper 1–10; affects open tabs immediately
- [ ] All confirmations from §11 present, all snackbars match the copy deck
- [ ] Animations + haptics honour their switches (and system animation scale)
- [ ] 9 languages in `strings.xml`; plurals used everywhere; RTL verified in Arabic
- [ ] TalkBack pass on all five destinations and the reader; ≥ 48 dp targets
- [ ] Screenshot diff vs mockup ≤ 2 % on layout-critical regions

### M8 · Hardening
- [ ] API 23 crash sweep (open, annotate, search, share, rotate, background/foreground ×20)
- [ ] Memory flat while swiping 50 PDF pages; `onTrimMemory` clears caches
- [ ] R8 rules for Room/Readium/PDFBox; release build installs and works
- [ ] Cold start < 800 ms on API 23 emulator; 60 fps library scroll
- [ ] AAB signed, mapping file archived, coverage matrix 24/24 green
- [ ] No `TODO`, no dead buttons, no placeholder strings, no debug logging

---

## B · Screen × size × theme QA sweep

Run this grid and initial each cell after verifying visually (screenshot compared with `qa/`).

| Screen | 360 dp | 412 dp | 600 dp | 800 dp | Landscape | Light | Dark | Sepia/Night/OLED |
|---|---|---|---|---|---|---|---|---|
| Library (flat list) | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | n/a |
| Library — file-type overview | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | n/a |
| Library — open group + its sort | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | n/a |
| Recents | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | n/a |
| Favorites | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | n/a |
| Search + filters | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | n/a |
| Settings | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | n/a |
| Reader (PDF) | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Reader (TXT) | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Reader (DOCX) | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Reader (XLSX) | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Reader (PPTX) | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Reader (EPUB) | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Open documents sheet + pick mode | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | n/a |
| Highlight colour menu (open/close/dismiss) | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Every sheet/dialog | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | n/a |

---

## C · Device matrix

| Device / API | Purpose | Result |
|---|---|---|
| Emulator API 23, 360×640, mdpi/hdpi | The floor: Android 6.0 must work | ☐ |
| Emulator API 29, 360×800 | Mid-range, scoped storage transition | ☐ |
| Emulator API 36, 412×915 | Current flagship behaviour, dynamic type | ☐ |
| Emulator API 36 tablet, 800×1280 | Medium/expanded layout, rail, 2-col | ☐ |
| Foldable profile (e.g. 7" folded / 8" unfolded) | Posture + hinge handling | ☐ |
| Physical device (any), landscape | Rotation, per-document lock, gestures | ☐ |

---

## D · Rejected-scope guard

Confirm these **do not** exist anywhere in the build:

- [ ] No Import screen (no local-storage browser, no cloud service pickers, no "recent downloads", no batch import)
- [ ] No "Export highlights as a list"
- [ ] No "Share a link to the file"
- [ ] No "Duplicate" action

---

## E · Behaviour parity spot-checks (fast regression pass)

Run these five flows end-to-end before every release — they exercise most of the app:

1. **Resume flow** — Open a PDF, scroll to page 4, zoom to 160 %, switch to Sepia, switch to horizontal scroll,
   close the app from recents, relaunch → the document reopens at page 4, 160 %, Sepia, horizontal.
2. **Organise flow** — Create folder "Invoices" (purple), move a document into it, rename the document without
   an extension, tag it `important`, star it → the library chip counts, favorites list, tag filter and file-info
   sheet all update; deleting the folder returns the document to "Not filed" without deleting it.
3. **Search flow** — Search "chapter" → summary counts, document results and inside-file snippets appear;
   tap a snippet → the reader opens at that page with the find bar showing "1 of 6"; press next four times →
   counter advances and the page scrolls to each match.
4. **Annotation flow** — Highlight a sentence green, tap it → context bar, recolour to pink, copy it,
   then Undo (back to green), Redo (pink), then open the Highlights sheet, jump to the entry, delete it →
   the count drops and the snackbar offers Undo.
5. **Tabs flow** — Open two documents with the tabs button → "Open a new document…" (the pick-a-document
   banner appears, rows gain the ＋ affordance), then: press back from the reader (the document stays open,
   the badge still shows 2, the "kept open" snackbar appears), reopen the Open documents sheet and switch to
   the other document (its page, zoom and reading theme are restored), close the active one (focus moves to
   the next tab, snackbar offers **Undo** → the tab returns — and with a **single** document open the × on the
   active row and "Close all tabs" both work, returning to the library with the badge hidden), lower the tab
   limit below the open count in
   Settings (the limit is clamped, no tab is killed), and finally Close all tabs → the reader closes and the
   badge disappears.
6. **Highlight-menu flow** — In the reader tap Highlight → the labelled colour menu expands with the live
   swatch; pick Orange; drag a selection over a sentence → an orange highlight appears with an Undo snackbar;
   tap the highlight → the context bar shows Orange ringed; press back → only the menu/context bar closes,
   the reader stays; press back again → the reader closes but the tab count is unchanged.
