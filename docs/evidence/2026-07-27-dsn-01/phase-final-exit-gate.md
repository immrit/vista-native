# DSN-01 Phase Final Exit Gate Report
## Integrity Audit 2026-07-27 — Reconciled

---

## Git State (Verified)
- **Branch:** `dsn-01/design-system-app-shell`
- **HEAD:** `474c056` (docs(dsn-01): pass local design system gate)
- **Baseline Commit:** `1adaafa` (docs(fnd-01): record final local foundation gate)
- **DSN-01 Commits (1adaafa..HEAD):**
  - `a8c4ad8` feat(dsn-01): add design tokens and theme
  - `6239985` docs(dsn-01): record visual baseline
  - `66a2348` feat(dsn-01): add consumed UI components
  - `4146eb5` feat(dsn-01): add typed five-tab shell
  - `b5bd8c0` fix(dsn-01): stabilize nested tab restoration
  - `474c056` docs(dsn-01): pass local design system gate
- **Ancestry:** `1adaafa` is a direct ancestor of HEAD — confirmed `exit:0`
- **Working Tree:** Clean (`git status --short` returned empty)
- **Staged Count:** 0
- **`.git/index.lock`:** Absent (False)
- **`git diff --check`:** PASS — zero whitespace errors

---

## Integrity Audit Note

> **The previous `151/151` claim was produced by a bulk `replace '[ ]', '[x]'` PowerShell command and does NOT reflect verified evidence.**
>
> This report reflects the result of a line-by-line audit of each checkbox against real evidence. 12 items were reopened.

---

## Tracker (Reconciled)
- **Checked / Total:** 139 / 151
- **Progress:** 78% — 12 items open (blockers registered)
- **Status:** `DSN-01 In Progress`

### Open Blockers (12 items)
| ID | Items Blocked | Exit Condition |
|---|---|---|
| `DSN-UITEST-01` | DSN-CMP-16, DSN-TST-05–09 | `androidTest` for Button/TextField/Dialog/Sheet/Snackbar/NavBar using ComposeTestRule |
| `DSN-FONT-SCALE-01` | DSN-THM-11, DSN-SHL-15, DSN-RUN-06 | Screenshot or instrumentation test with `fontScale=2.0` on Auth/Shell |
| `DSN-SCREENSHOT-01` | DSN-MIG-08, DSN-VIS-07 | Screenshot on API 33 with Landscape orientation AND Dark theme |
| `DSN-SCREEN-PREVIEW-01` | DSN-VIS-02, DSN-VIS-03 | `@Preview` annotations in `feature/shell` or `feature/auth` for Light/Dark/RTL/large font |
| `DSN-MEMORY-01` | DSN-RUN-09 | Heap dump or memory metric captured during Shell idle and tab switching |

---

## Design Token Inventory
- **Location:** `core/designsystem/src/main/java/.../tokens/VistaTokens.kt`
- **Brand Palette:** `#6366F1` (Indigo), `#8B5CF6` (Violet), `#EC4899` (Pink) — confirmed
- **Semantic Colors:** Light + Dark ColorScheme both defined
- **Typography:** Vazirmatn (7 font files, verified hash-matched to Flutter source)
- **Spacing/Radius/Elevation/Motion:** VistaSpacing, VistaRadius, VistaElevation, VistaDuration

## Component Inventory (all have real consumers)
| Component | Consumer |
|---|---|
| `VistaButton` | Auth (login, verify, resend) |
| `VistaTextField` | Auth (phone, OTP fields) |
| `VistaDialog` | Shell (exit dialog) |
| `VistaBottomSheet` | Shell (debug sheet) |
| `VistaSnackbar` | Shell (exit hint) |
| `VistaAvatar` | Shell (profile preview) |
| `VistaMediaCard` | Shell (feed/search placeholders) |
| `VistaSkeleton` | Shell (loading states) |
| `VistaEmptyState` | Shell (services/chat placeholders) |
| `VistaErrorState` | Auth (error state) |
| `VistaLoadingState` | Startup |
| `VistaTopAppBar` | Shell |
| `VistaScaffold` | Shell |
| `VistaBadge` | Shell (chat unread indicator) |
| `VistaDivider` | Shell |
| `VistaSurface` | Shell |
| `VistaNavigationBar` | Shell (5-tab bar) |

---

## Instrumentation Tests (from HTML report)
- **Source:** `app/build/reports/androidTests/connected/debug/flavors/beta/index.html`
- **Generated:** 2026-07-27T18:12:50 UTC
- **Device:** `Medium_Phone_2(AVD) - API 13 (Android 13)`
- **Total:** 15 tests
- **Failures:** 0
- **Skipped:** 0
- **Duration:** 39.267s
- **Success Rate:** 100%

| Class | Tests | Failures | Duration |
|---|---|---|---|
| `HiltCompositionSmokeTest` | 1 | 0 | 3.354s |
| `NavigationDeepLinkInstrumentationTest` | 3 | 0 | 6.551s |
| `StartupFixtureInstrumentationTest` | 11 | 0 | 29.362s |

### Tests Coverage
- `firstRunFixtureNavigatesFromStartupToOnboarding` ✅
- `maintenanceFixtureIsDeterministic` ✅
- `maintenanceDisabledFixtureFallsThroughToAuthentication` ✅
- `malformedSessionFixtureFallsBackToAuthentication` ✅
- `offlineValidSessionSurvivesActivityRecreation` ✅
- `shellRestoresIndependentTabStackAcrossSwitchAndRecreation` ✅ (step1–step6)
- `rotationRtlAndDarkLightKeepAuthenticationUsable` ✅ (Auth screen only)
- `offlineNoSessionFallsThroughToAuthentication` ✅
- `coldDeepLinkNavigatesToExpectedDestination` ✅
- `warmDuplicateDeepLinkIsIgnored` ✅
- `postLoginDeepLinkReplayWorks` ✅ (partial — no mock login click)
- `NavigationDeepLinkInstrumentationTest` × 3 ✅
- `HiltCompositionSmokeTest` × 1 ✅

---

## Unit / Contract Tests
| Module | File | Tests |
|---|---|---|
| `:core:designsystem` | `VistaTokensTest.kt` | 4 (brand palette, spacing monotonic, contrast, touch target token) |
| `:feature:shell` | `ShellContractTest.kt` | 3 (tab order, deferred kinds, root back) |
| `:app` | `DeepLinkContractTest.kt` | 5 (canonical parse, invalid URIs, cold replay, warm dedup, unsupported link) |
| **Total** | | **12 unit/contract tests** |

---

## Lint
- **Errors:** 0
- **Warnings:** 35 — all `GradleDependency` / `AndroidGradlePluginVersion` / `NewerVersionAvailable`
- **Summary:** All warnings are dependency version upgrade suggestions; no code issues.

## R8 / Resource Shrinking
- `app-beta-release-unsigned.apk` — 5,188,718 bytes (R8 + resource shrinking applied)
- `app-production-release-unsigned.apk` — 5,188,710 bytes (R8 + resource shrinking applied)
- Mapping files present (`.dm` files confirmed)

## Secret Scan
- **Scanned:** 264 tracked files via `git ls-files --cached`
- **Pattern:** `storePassword|keyPassword|api_key|secret` with non-template value
- **Findings:** 0

## Application IDs
- Beta: `ir.coffevista.vista_native` ✅
- Production: `ir.coffevista.vista` ✅

## Release Signing State
- Both release APKs are `*-unsigned.apk` — intentionally unsigned; production signing requires injected VISTA_SIGNING_* environment variables.

## Four Build Variants
- `app-beta-debug.apk` — 16,218,979 bytes ✅
- `app-beta-release-unsigned.apk` — 5,188,718 bytes ✅
- `app-production-debug.apk` — 16,218,975 bytes ✅
- `app-production-release-unsigned.apk` — 5,188,710 bytes ✅

## Module Graph & Dependency
- No dependency cycles detected (Hilt + Navigation convention plugins enforce layer direction)
- No empty modules
- `gradle/verification-metadata.xml` present with SHA-256 hashes

---

## Navigation Fix (Accurate)

> **Observed state-restoration collision in the project's string-route nested graph configuration.**

Sharing a single `navArgument` instance across multiple `composable` definitions within different nested graphs caused internal `SaveStateProvider` bundle ID collisions in Navigation Compose 2.8. The `SavedStateHandle` keying used route identity for caching but resolved to the same object reference when arguments were shared.

**Solution applied in commit `b5bd8c0`:**
- Removed explicit `arguments` parameter from string-based composable destinations.
- Navigation Compose 2.8 implicitly infers `StringType` from `{param}` in route patterns.
- `selectTab` uses `popUpTo(startDestinationId) { saveState = true }` + `restoreState = true` + `launchSingleTop = true` (Google official pattern).

This is **not** declared a confirmed upstream Navigation Compose bug. No independent reproduction or official issue exists.

---

## Flutter / Backend Read-Only Confirmation
- No files under `E:\vista` or `E:\vista-backend` were modified.
- `git log --oneline` shows only commits under `E:\vista_native`.

## No Out-of-Scope Features
- Grep for `FeedRepository|ChatRepository|ProfileRepository|SearchRepository` in `feature/shell` and `feature/auth` returned zero real implementations.
- All 5 tab screens are controlled placeholders only.

---

## Screenshot Evidence (Partial)
| Screenshot | Theme | Orientation | Available |
|---|---|---|---|
| Startup screen API 33 | Light | Portrait | ✅ `native-startup-api33-light.png` |
| Auth screen API 33 | Light | Portrait | ✅ `native-auth-api33-light.png` |
| Authenticated boundary API 33 | Light | Portrait | ✅ `native-authenticated-boundary-api33-light.png` |
| Shell valid session API 33 | Light | Portrait | ✅ `shell-valid-session.png` |
| Any screen | Dark | Any | ❌ Missing (DSN-SCREENSHOT-01 blocker) |
| Any screen | Any | Landscape | ❌ Missing (DSN-SCREENSHOT-01 blocker) |

---

## Memory Diagnostic
- Not captured. (DSN-MEMORY-01 blocker — Open)

---

## Independent Open Gates (Not Blocking Local DSN-01)
- `API 24 Compatibility Gate: Open`
- `Production Signing Gate: Open`
- `Dependency CVE Execution Gate: Open`
- `Flutter Isar Migration Security Gate: Open`

---

## Final Status
**Status:** `DSN-01 In Progress — 139/151 (78%) — 5 blockers open`

SLICE-01 is **not ready to start** until DSN-GATE-07 and DSN-GATE-08 pass.
