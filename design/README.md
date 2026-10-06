# 🌿 Leaf — Document Viewer

A native Android document reader (Kotlin · Compose · Material 3), **Android 6.0 (API 23) → latest**,
offline-first, with resume-reading, folders/collections/tags, full-text search, multi-colour highlights
and multi-document (tabbed) reading. Supported formats: **PDF · DOCX · XLSX · PPTX · TXT · EPUB**.

## What's in this package

| File | What it is | Use it to |
|---|---|---|
| **`leaf-mockup-v2.html`** | Interactive Material 3 mockup — every screen, sheet, gesture and theme | Review the design; it is the **visual contract** for the build |
| **`LEAF-MASTER-PROMPT.md`** | The master build prompt (tokens, screens, engine, behaviours, tests, milestones) | Paste into your AI coding agent to build the app |
| **`LEAF-KICKOFF-PROMPT.md`** | Short session starter, scoped to Milestone 1 | Kick off the build (reuse per session, change the milestone) |
| **`LEAF-BUILD-CHECKLIST.md`** | Milestone gates + QA grid + device matrix + parity spot-checks | Track and verify progress |
| **`LEAF-SPEC.md`** | Product & technical spec: data model, IA, acceptance criteria | Reference the requirements |
| **`LEAF-MOCKUP-V2-CHANGES.md`** | v1 → v2 change log | See exactly what was removed/added |
| **`LEAF-AGENT-WORKFLOW.md`** | How to drive a GitHub-capable coding agent: bootstrap prompt, per-milestone loop, releases, keystore secrets, review gates | Run the build end to end |
| **`ci/android.yml`** | GitHub Actions workflow (lint + tests + debug APK artifact, signed release on tag `v*`) | Copy into the app repo as `.github/workflows/android.yml` |
| `design/leaf_tokens.json` · `design/gen_palette.py` | Material 3 tokens generated from seed `#2B7A5B` | Regenerate/adjust the palette |
| `qa/suite.py` | 74-check regression suite for the mockup (`python3 qa/suite.py` after `qa/setup-playwright.sh`) |
| `qa/capture.py` | Regenerates every reference screenshot from the current mockup |
| `qa/mockup-ref/` | 26 reference screenshots (360 / 412 / 700 dp, light + dark, reader themes, Open documents sheet, pick-a-document, file-type overview + an open group, highlight colour menu) | Compare the built app against the design |

## Try the mockup

Open `leaf-mockup-v2.html` in a browser (or preview it here). Design-review controls at the top switch the
frame between **360 × 760**, **412 × 892**, **480 × 1000** and **700 × 900 (tablet)**, rotate it, flip the app
theme, and reset the demo data.

- Long-press a document → actions sheet
- Star, filter by folder / collection / tag, sort by field + direction. The **group-by-type toggle** turns the
  library into one card per file type (`PDF documents`, `Word · DOCX`, `Excel · XLSX`, `PowerPoint · PPTX`,
  `Plain text · TXT`, …) with counts and sizes; tap a card to drill into that type's file list, where the sort
  chip is available again — back or Esc returns to the groups
- Search scopes incl. **Inside files** + combined filters + history
- Reader: bookmarks, find-in-file, zoom (pinch / double-tap / buttons), reflow, full screen,
  per-document orientation lock, reading themes (Paper · Sepia · Night · OLED · Auto)
- **Tabs** live in the app bar: the tabs button (with a live count badge) opens the **Open documents**
  sheet — switch, close any row (**including the only open one**), close all, or "Open a new document…" which
  starts **pick-a-document mode**
  (banner + ＋affordances). Leaving the reader **keeps documents open**; back again from the Library exits pick mode.
- Highlight with 5 colours through the expanding **Highlight** menu (swatch + labelled palette + hint),
  recolour, remove, copy, undo/redo, highlights summary
- Shortcuts: `Ctrl+F` find · `Ctrl+Z` / `Ctrl+Shift+Z` undo/redo · `Ctrl+B` bookmark ·
  `Ctrl+G` jump to page · `Ctrl+P` view & layout · `Ctrl+T` open another document in a new tab (works from
  any screen) · `Esc` closes the highlight menu → dialog → sheet → full screen → reader
- The reader toolbar adapts to the frame: at 360 dp the zoom group is hidden, at 412 dp the `%` label is
  hidden, below 600 dp the highlights-list button is hidden — full screen and share always stay visible

## Build order

M1 Foundation → M2 Library & organisation → M3 Reader core → M4 Search → M5 Annotation & tabs →
M6 Formats & sharing → M7 Settings, polish, i18n → M8 Hardening.

Every feature row in `LEAF-MASTER-PROMPT.md` §14 must end green. Nothing gets stubbed.

**Running the build with an AI agent:** follow `LEAF-AGENT-WORKFLOW.md`. Short version — put this package in
the repo under `design/`, paste the bootstrap prompt once (it scaffolds the project and makes CI build an
installable APK), then run one session per milestone with the loop prompt. Tag `v0.3.0` after M3 for the first
build worth installing, `v1.0.0` after M8.
