# 🌿 LEAF — KICKOFF PROMPT (paste this to start the build)

> Paste this **after** `LEAF-MASTER-PROMPT.md`. It scopes the first session and sets the working rhythm.
> Reuse it at the start of every new session by changing the milestone number.
>
> **If your agent can push to GitHub and run CI** (GitHub Actions, Codex/Claude Code with repo access,
> Devin, …) read `LEAF-AGENT-WORKFLOW.md` instead — it contains a drop-in bootstrap prompt, the
> per-milestone loop prompt, the release/keystore setup and `ci/android.yml` for building APK artifacts
> and publishing releases.

---

Read these files in the repository before writing any code:

1. `LEAF-MASTER-PROMPT.md` — the full build contract (design tokens, screens, behaviours, engine, tests).
2. `LEAF-SPEC.md` — the product specification and data model.
3. `leaf-mockup-v2.html` — the interactive mockup that defines the exact look and feel. Open its CSS
   (`:root` tokens, component rules) and its `<script>` (interaction rules) and treat them as the
   visual contract.
4. `design/leaf_tokens.json` — the generated Material 3 token set.
5. `LEAF-BUILD-CHECKLIST.md` — milestones and the QA matrix.
6. The `qa/` folder — reference screenshots of every mockup screen at 360, 412 and 700 dp.

**Your task now: build Milestone 1 — Foundation.**

Deliverables for this session:
1. A fresh Android project: Kotlin 2.1+, Compose + Material 3, `minSdk 23`, `targetSdk 36`, `compileSdk 36`,
   JDK 17, version catalog (`gradle/libs.versions.toml`), core library desugaring enabled.
2. `core/ui/theme/` — `Color.kt`, `Type.kt`, `Shape.kt`, `Motion.kt`, `Theme.kt` implementing the token tables
   in §4 of the master prompt **exactly** (both `LeafLight` and `LeafDark`), plus the `ReadingPalette` model
   with Paper/Sepia/Night/OLED and the highlight colour table.
3. Typeface: bundle Noto Serif (Regular + SemiBold) with its OFL licence in `res/raw`, exposed as
   `LeafTypography.reading`.
4. Navigation shell: single activity, edge-to-edge, `WindowInsets` correct on API 23, a `NavigationBar`
   (5 destinations: Library, Recents, Favorites, Search, Settings) on compact widths and a `NavigationRail`
   on ≥ 600 dp, driven by the Material 3 window size classes.
5. The five destination screens as structural scaffolds: app bar with the correct title/subtitle/actions,
   scroll container, and empty states using the exact copy from §11. No dead buttons — every action either
   works or opens a "not built yet" placeholder that is removed in a later milestone.
6. Room database + DataStore with the schema from `LEAF-SPEC.md` §4, plus a `SeedData` provider that inserts
   the eight sample documents, five folders (one nested), seven tags and five smart collections from the
   mockup so the app looks alive from the first run.
7. `LeafApp` + DI (Koin or Hilt) wiring repositories to ViewModels, `StateFlow` UI state, and a one-shot
   event channel for snackbars.
8. CI: copy `ci/android.yml` from this package to `.github/workflows/android.yml` and confirm the first run
   is green — lint, unit tests and a debug APK artifact must all appear on the run page.
9. Verification: install and run on an **API 23 emulator (360×640)** and an **API 36 emulator (412×915)**,
   plus one tablet profile in landscape. Capture screenshots of all five screens in light and dark, save them
   to `qa/impl/`, and compare against the mockup. Fix every deviation in colour, spacing, radius, type and
   elevation before reporting back.

**Rules for how you work:**
- Do not start any other milestone in this session.
- Do not stub features silently; if something in M1 is blocked, say so and propose the alternative.
- Every string goes into `strings.xml`, even at M1.
- Keep the coverage matrix (§14 of the master prompt) in `README.md` and mark M1 rows honestly.
- Report back with: what you built, the exact commands you ran, the screenshots you captured, the deviations
  you found and fixed, and what you propose for Milestone 2.
