# 🌿 Building Leaf with a GitHub-capable coding agent

This is the operator's manual for the package: how to drive an AI agent that can **write code, push to
GitHub, run CI and publish releases** so that it produces the Leaf app that the mockup defines.

Read §1 once, then live in §3 (the per-milestone loop).

---

## 0 · The pipeline in one picture

```
   you                     the agent                         GitHub                      you
┌──────────┐   prompt   ┌──────────────┐  push/PR  ┌────────────────────┐  artifact  ┌──────────┐
│ design   │ ─────────▶ │ writes code  │ ────────▶ │ Actions: build +   │ ─────────▶ │ install  │
│ package  │            │ per milestone│           │ lint + tests       │  leaf.apk  │ & compare│
│ (this)   │ ◀───────── │ reports back │ ◀──────── │ Release on tag v*  │ ◀───────── │ to qa/   │
└──────────┘  report    └──────────────┘           └────────────────────┘            └──────────┘
```

The agent never has to *see* the app: CI hands you an installable APK to check, and the reference
screenshots in `qa/mockup-ref/` are the pixel spec.

---

## 1 · Put the package in the repository first

Create **one repository** for the app (suggested name `leaf-android`), then put the design package in it —
the agent reads it as its contract. Recommended layout in the repo:

| Path in the repo | What goes there | Why |
|---|---|---|
| `design/LEAF-MASTER-PROMPT.md` | master build prompt | the full specification the agent implements |
| `design/LEAF-SPEC.md` | product + technical spec | data model, acceptance criteria |
| `design/leaf-mockup-v2.html` | the interactive mockup | source of truth for pixels, copy and behaviour |
| `design/qa/mockup-ref/*.png` | 26 reference screenshots | what the agent diffs its screenshots against |
| `design/LEAF-BUILD-CHECKLIST.md` | gates, QA grid, device matrix, spot-checks | your review sheet |
| `.github/workflows/android.yml` | `ci/android.yml` from this package | builds APK artifacts + publishes releases |
| `README.md` | the agent maintains it: coverage matrix + build badge | your progress dashboard |

Everything else (`app/`, `gradle/`, `build.gradle.kts`, tests…) the agent creates.

> `.gitignore` must exclude `local.properties`, `*.jks`, `*.keystore`, `keystore.properties`, `.gradle/`,
> `build/` and `.idea/`. Never let the keystore or its passwords reach the repo — CI injects them (§4).

---

## 2 · Paste this once — the bootstrap session

Use this as the **first message** in a fresh agent session that has repo access. It gets you a repo that
already builds, tests, and ships an APK artifact before a single feature exists.

```text
You are the build agent for "Leaf", a native Android document reader (Kotlin · Jetpack Compose ·
Material 3 · minSdk 23 · targetSdk 36). Repo: <OWNER>/leaf-android (create it if it does not exist —
private). You may push branches, open PRs, run GitHub Actions and create releases.

INPUTS (read these first, in this order):
  design/LEAF-MASTER-PROMPT.md   — the full specification. It is the contract.
  design/LEAF-SPEC.md            — product + data-model spec.
  design/leaf-mockup-v2.html     — the interactive mockup: exact colours, sizes, copy and behaviour.
  design/qa/mockup-ref/*.png     — reference screenshots (360/412/700 dp, light/dark, reader themes).
  design/LEAF-BUILD-CHECKLIST.md — the gates you will be checked against.

TASK — MILESTONE 1 (Foundation) ONLY:
  1. Create the project exactly as §2–§4 of the master prompt specify: Gradle version catalog pinned to the
     listed versions, applicationId app.leaf.reader, minSdk 23 / targetSdk 36 / compileSdk 36, JDK 17,
     R8 off for debug, core library desugaring ON.
  2. Implement §4 in full: the M3 colour roles, reading palettes, highlight colours, type scale
     (serif for reading text, sans for UI), shapes, spacing, motion, and component metrics. Copy the hex
     values verbatim — do not "improve" them.
  3. Navigation shell: the 5 destinations (Library · Recents · Favorites · Search · Settings) with
     NavigationBar below 600 dp and NavigationRail at/above 600 dp, in the exact responsive contract of §5.
  4. Room + DataStore layer per §3/§8 with the seed data (10 documents incl. the .xlsx and .pptx,
     folders, 7 tags, smart collections) and the settings defaults of the mockup.
  5. Copy ci/android.yml into .github/workflows/android.yml and make it pass as-is.
  6. Add the README coverage matrix skeleton (§14) so it is ready to tick.

DEFINITION OF DONE for this session:
  • `./gradlew lint testDebugUnitTest assembleDebug` passes locally and in CI.
  • CI artifacts contain an installable debug APK.
  • Screenshots rendered at 360 / 412 / 700 dp for the 5 empty screens (Paparazzi or Roborazzi, wired in
    this milestone) and committed under app/src/test/snapshots/.
  • README: what you built, the exact commands, what deviates from the mockup and why, what is next.

WORKING RULES (they apply to every session):
  • Follow the master prompt exactly. Never invent features, copy or colours.
  • NEVER stub a feature to make a build pass — if something is ambiguous, ask me and wait.
  • Do not build any of the rejected scope: no Import screen, no export-highlights-as-list, no
    share-a-link, no Duplicate.
  • One branch per milestone (`m1-foundation`, `m2-library`, …), PR into main, CI green before you ask me
    to review. Conventional commit messages.
  • Open a draft PR as soon as the branch exists so I can watch the CI runs.
  • Report back with: branch + PR link, CI run link, the APK artifact link, screenshots, deviations, next step.

Start by replying with a one-page build plan and the M1 file list for my approval — do not write code
before I approve the plan.
```

**Your review of that session:** open the PR, download the APK artifact, install it, and compare the five
screens with `design/qa/mockup-ref/final-01…05`. If the tokens are right, the app already *looks* like Leaf.

---

## 3 · The per-milestone loop (M2 → M8)

One **session per milestone** keeps the agent focused and gives you a clean review point. Paste this at the
start of each session, changing `{{MILESTONE}}`:

```text
Continue the Leaf build. Repo: <OWNER>/leaf-android.
Read design/LEAF-MASTER-PROMPT.md §6–§15 as needed and the current state of the repo (including README's
coverage matrix and the last merged PR).

THIS SESSION — MILESTONE {{N}}: {{NAME}}
  Scope and deliverable: master prompt §13, row {{N}}.
  Screens/sheets you may touch: {{e.g. §6.1 Library + §6.7 sheets it opens}}.
  Verification for this milestone: {{copy the "Verification" cell from §13}}.

DELIVER:
  • A branch `m{{N}}-{{slug}}` and a draft PR titled "M{{N}} · {{NAME}}".
  • Code + unit tests for the logic this milestone introduces (see §12 for the test list).
  • Screenshot tests for every screen or sheet you changed, at 360 dp and 412 dp, light + dark, and for the
    reader also Paper / Sepia / Night / OLED — committed under app/src/test/snapshots/.
  • README: tick off the §14 coverage-matrix rows that are now genuinely done, and paste the CI status.
  • A short report: what shipped · exact commands run · what deviates from the mockup and why ·
    what remains · anything you need decided.

RULES: no new features beyond the milestone · no stubs · no rejected scope · CI must be green ·
ask before guessing. When the PR is ready for review, tag it and tell me.
```

**Milestone names** (use the master prompt §13 verbs):
`M2 Library & organisation` · `M3 Reader core` · `M4 Search` · `M5 Annotation & tabs` ·
`M6 Formats & sharing` · `M7 Settings, polish, i18n` · `M8 Hardening`.

### After each milestone — your 6-point review

1. **CI is green** on the PR (lint + tests + APK built).
2. **APK installs** on a real device or the API 23 emulator, and on API 36 if you have it.
3. **Screenshots** (from the `rendered-screens-*` artifact, once the screenshot job is on) match
   `design/qa/mockup-ref/` — misalignment means tokens or metrics drifted.
4. **Coverage matrix** in the README has only rows that actually work ticked.
5. **Spot-checks** from `design/LEAF-BUILD-CHECKLIST.md` §E for the areas this milestone touched
   (Resume · Organise · Search · Annotation · Tabs · Highlight menu).
6. **No rejected scope** — quick grep of the PR for Import / export list / share link / Duplicate.

If a review fails, reply in the same PR: *"CI is green but X deviates — here is the screenshot. Fix X before
merging, keep everything else."* Small, in-PR corrections are far cheaper than a new milestone session.

---

## 4 · Releases, versioning and secrets

**Versioning:** tag `vMAJOR.MINOR.PATCH`. Tag the moments that matter:
`v0.1.0` after M1 (foundation) · `v0.3.0` after M3 (first real reader — the first build worth keeping on
your phone) · `v0.5.0` after M5 · `v0.7.0` after M7 · `v1.0.0` after M8.

Push a tag and CI does the rest:

```bash
git tag -a v0.3.0 -m "M3 · Reader core" && git push origin v0.3.0
```

or tell the agent: *"cut release v0.3.0 — tag main at the current commit, push the tag, and paste me the
release URL."*

The release job publishes a GitHub Release with **`leaf-vX.Y.Z.apk`**, **`leaf-vX.Y.Z.aab`** and the **R8
`mapping.txt`** attached, plus auto-generated release notes. Until you add signing secrets it publishes an
*unsigned* APK (marked as such in the notes) so the pipeline never breaks.

**Signing secrets** — add these once, under *Settings → Secrets and variables → Actions*:

| Secret | Content |
|---|---|
| `KEYSTORE_BASE64` | base64 of your release keystore: `base64 -w0 leaf-release.jks` |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias (`keytool -genkeypair … -alias leaf`) |
| `KEY_PASSWORD` | key password |

Generate the keystore once and keep it **out** of the repo (put it in a password manager — losing it means
losing your Play update path):

```bash
keytool -genkeypair -v -keystore leaf-release.jks -alias leaf \
        -keyalg RSA -keysize 4096 -validity 10000 \
        -dname "CN=Leaf, OU=Mobile, O=Your Name, L=Ibadan, C=NG"
base64 -w0 leaf-release.jks          # paste this output into the KEYSTORE_BASE64 secret
```

**Later, Play Store:** the `.aab` is already produced — add a `publish` job using
`r0adkll/upload-google-play@v1` with a service-account JSON secret when you are ready to go to internal
testing. Ask me and I will write that job.

---

## 5 · Notes and known CI pitfalls

| Symptom | Cause / fix |
|---|---|
| `SDK location not found` | commit the Gradle wrapper and make sure `local.properties` is **not** required — CI uses `ANDROID_HOME` from the runner. |
| `compileSdk 36 requires build-tools…` | add `android-actions/setup-android@v3` + `sdkmanager "platforms;android-36" "build-tools;36.0.0"` before building. |
| `Unsupported class file major version` | wrong JDK — the workflow pins Java 17; keep `kotlin { jvmToolchain(17) }` in the build too. |
| Gradle daemon killed / OOM | the workflow sets `-Xmx4g --no-daemon`; if the agent adds heavy KSP tasks, raise it. |
| Readium / java.time crash below API 26 | core library desugaring must be **on** (master prompt §2). Never remove it to "fix" a compile error. |
| KSP version mismatch | KSP must match the Kotlin version exactly (`2.1.20-1.0.32`). |
| APK never attaches to a release | the file glob is `out/*` — check the job log's "Collect outputs" step lists the APK. |
| Screenshot diffs are noisy | render on the CI JVM with a fixed font scale/density, and compare only layout-critical regions (master prompt §12). |
| Agent keeps re-planning | it is re-reading the whole master prompt every session; point it at the specific §s in the loop prompt. |

---

## 6 · Guardrails worth repeating in every session

Paste these whenever the agent drifts (they are the rules that matter most, in the agent's own language):

- **“The mockup is the spec.”** `design/leaf-mockup-v2.html` defines colours, dimensions and every behaviour;
  `design/qa/mockup-ref/*.png` is the pixel reference. Copy values verbatim; never "modernise" them.
- **“No stubs, no placeholders.”** A feature is either finished or it is not ticked in the coverage matrix.
  A build that compiles with `TODO()` in the middle is not a deliverable.
- **“Rejected scope stays rejected.”** No Import screen, no export-highlights-as-list, no share-a-link,
  no Duplicate — in code, copy, comments and docs.
- **“Ask, don't guess.”** Ambiguity is a question, not an invention.
- **“Every milestone ends with evidence.”** Branch + PR + green CI + APK artifact + screenshots + report.
