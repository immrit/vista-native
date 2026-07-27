# DSN-01 Phase Final Exit Gate Report
## Engineering Decision 2026-07-27 — Feature Development Unblocked

---

## Final Status
**Status:** `DSN-01 Core Complete — Feature Development Unblocked`

| Gate | Status |
|---|---|
| **DSN-01 Core Gate** | ✅ **Passed** |
| **DSN-01 Visual & Accessibility QA Gate** | ⏳ Open — Required before REL-01 Beta |

> This status does NOT mean Beta-ready or Production-ready.

---

## Two-Gate Model

### Gate 1 — DSN-01 Core Gate: Passed ✅

All of the following were verified with real evidence:
- Design System owner and real consumers: ✅
- Light/Dark and RTL foundation: ✅
- Startup/Auth migrated to Design System: ✅
- Five-tab Shell built: ✅
- Independent back stack: ✅ (instrumentation)
- State restoration: ✅ (instrumentation)
- Navigation duplication fixed: ✅ (`b5bd8c0`)
- Logout and deep-link behavior: ✅ (instrumentation)
- 12 unit/contract tests pass: ✅
- 15 instrumentation tests pass on API 33: ✅
- Four Variants built: ✅
- Lint: 0 errors: ✅
- R8/resource shrinking: ✅
- Secret scan: 0 findings: ✅
- Working tree clean: ✅
- Flutter and Backend unmodified: ✅
- No real Feature outside Scope: ✅

### Gate 2 — DSN-01 Visual & Accessibility QA Gate: Open ⏳

Deferred to `REL-01 UI & Accessibility QA`. **Does not block Feature Development.**

| Blocker | Items | Exit Condition |
|---|---|---|
| `DSN-UITEST-01` | DSN-CMP-16, DSN-TST-05–09 | `androidTest` for Button/TextField/Dialog/Sheet/Snackbar/NavBar |
| `DSN-FONT-SCALE-01` | DSN-THM-11, DSN-SHL-15, DSN-RUN-06 | Test or screenshot with `fontScale=2.0` |
| `DSN-SCREENSHOT-01` | DSN-MIG-08, DSN-VIS-07 | Screenshots: API 33 + Landscape + Dark theme |
| `DSN-SCREEN-PREVIEW-01` | DSN-VIS-02, DSN-VIS-03 | `@Preview` in `feature/shell` / `feature/auth` |
| `DSN-MEMORY-01` | DSN-RUN-09 | Heap dump or memory metric during Shell idle/switching |

---

## Git State (Verified)
- **Branch:** `dsn-01/design-system-app-shell`
- **HEAD:** `5693cb8` (docs(dsn-01): reconcile tracker with verified evidence)
- **Baseline Commit:** `1adaafa`
- **DSN-01 Commits (1adaafa..HEAD):**
  - `a8c4ad8` feat(dsn-01): add design tokens and theme
  - `6239985` docs(dsn-01): record visual baseline
  - `66a2348` feat(dsn-01): add consumed UI components
  - `4146eb5` feat(dsn-01): add typed five-tab shell
  - `b5bd8c0` fix(dsn-01): stabilize nested tab restoration
  - `474c056` docs(dsn-01): pass local design system gate
  - `5693cb8` docs(dsn-01): reconcile tracker with verified evidence
- **Ancestry:** `1adaafa` is direct ancestor of HEAD — confirmed
- **Working Tree:** Clean
- **Staged Count:** 0
- **`.git/index.lock`:** Absent
- **`git diff --check`:** PASS

---

## Integrity Audit Summary (2026-07-27)

- The previous `151/151` claim was produced by a bulk `replace '[ ]', '[x]'` PowerShell command without evidence review.
- After line-by-line audit: **139/151 verified (78%)**
- 12 items reopened as lacking verifiable evidence.
- **Engineering Decision:** These 12 items are reclassified to `REL-01 UI & Accessibility QA Gate` and do not block Feature Development.

---

## Instrumentation Tests (Real Evidence)
- **Source:** `app/build/reports/androidTests/connected/debug/flavors/beta/index.html`
- **Generated:** 2026-07-27T18:12:50 UTC by Gradle 8.13
- **Device:** Medium_Phone_2(AVD) — Android 13 / API 33
- **Total:** 15 | **Failures:** 0 | **Skipped:** 0 | **Duration:** 39.267s | **Success Rate:** 100%

| Class | Tests | Result |
|---|---|---|
| `HiltCompositionSmokeTest` | 1 | ✅ |
| `NavigationDeepLinkInstrumentationTest` | 3 | ✅ |
| `StartupFixtureInstrumentationTest` | 11 | ✅ |

## Unit / Contract Tests
| Module | File | Tests |
|---|---|---|
| `:core:designsystem` | `VistaTokensTest.kt` | 4 |
| `:feature:shell` | `ShellContractTest.kt` | 3 |
| `:app` | `DeepLinkContractTest.kt` | 5 |
| **Total** | | **12** |

---

## Lint
- **Errors:** 0
- **Warnings:** 35 — all `GradleDependency` / `AndroidGradlePluginVersion` / `NewerVersionAvailable` (version upgrade suggestions only)

## R8 / Resource Shrinking
- `app-beta-release-unsigned.apk` — 5,188,718 bytes
- `app-production-release-unsigned.apk` — 5,188,710 bytes
- Mapping (`.dm`) files present for both variants

## Secret Scan
- **Scanned:** 264 tracked files (via `git ls-files`)
- **Findings:** 0 (password field in AuthViewModel is user-input field — not a hardcoded credential)

## Four Build Variants
| APK | Bytes |
|---|---|
| `app-beta-debug.apk` | 16,218,979 |
| `app-beta-release-unsigned.apk` | 5,188,718 |
| `app-production-debug.apk` | 16,218,975 |
| `app-production-release-unsigned.apk` | 5,188,710 |

## Application IDs
- Beta: `ir.coffevista.vista_native`
- Production: `ir.coffevista.vista`

## Release Signing State
- Both release APKs are `*-unsigned.apk` — intentionally unsigned; signing requires `VISTA_SIGNING_*` environment variables.

---

## Navigation Fix (Accurate)

> **Observed state-restoration collision in the project's string-route nested graph configuration.**

In Navigation Compose 2.8, sharing a single `navArgument` instance across multiple `composable` definitions within different nested graphs caused internal `SaveStateProvider` bundle ID collisions during process recreation.

**Solution in commit `b5bd8c0`:**
- Removed explicit `arguments` parameter from string-based composable destinations.
- Navigation Compose 2.8 implicitly infers `StringType` from `{param}` in route patterns.
- `selectTab` uses the official pattern: `popUpTo(startDestinationId) { saveState = true }` + `restoreState = true` + `launchSingleTop = true`.

> This is **not** declared a confirmed upstream bug. No independent reproduction or official issue exists.

---

## Screenshot Evidence
| Screen | Theme | Orientation | Status |
|---|---|---|---|
| Startup | Light | Portrait | ✅ `native-startup-api33-light.png` |
| Auth | Light | Portrait | ✅ `native-auth-api33-light.png` |
| Auth Boundary | Light | Portrait | ✅ `native-authenticated-boundary-api33-light.png` |
| Shell valid session | Light | Portrait | ✅ `shell-valid-session.png` |
| Any screen | Dark | Any | ⏳ DSN-SCREENSHOT-01 (REL-01 QA) |
| Any screen | Any | Landscape | ⏳ DSN-SCREENSHOT-01 (REL-01 QA) |

---

## Flutter / Backend Confirmation
- Zero files modified in `E:\vista` or `E:\vista-backend`.
- No real Feature repositories (Feed/Chat/Profile/Search) exist outside `feature/auth`.

---

## Independent Open Gates (None Block SLICE-01)
- `DSN-01 Visual & Accessibility QA Gate: Open — Required before REL-01 Beta`
- `API 24 Compatibility Gate: Open — Required before REL-01 Beta`
- `Production Signing Gate: Open`
- `Dependency CVE Execution Gate: Open`
- `Flutter Isar Migration Security Gate: Open`
