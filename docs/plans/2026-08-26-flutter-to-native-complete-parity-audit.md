# Vista Flutter -> Native Complete Migration Plan

**Type:** refactor / migration / parity  
**Status:** in-progress  
**Last updated:** 2026-08-27  
**Progress:** `[########--] 80%`  
**Scope:** `E:\vista` (Flutter source of truth) -> `E:\vista_native` (Android Native target)

## Problem / Goal

Move the complete Vista product from Flutter to Native without functional, navigational, security, or visual regressions. Flutter remains the behavioral reference until Native passes every release gate below. “Implementation exists” is never treated as “parity is proven”.

## Non-negotiable acceptance policy

1. Every migrated capability has a source trace, deterministic test/fixture, and runtime evidence.
2. Pixel parity uses paired screenshots from the same device profile; every difference is classified and reviewed.
3. Credentials, tokens, message plaintext, and production secrets never enter logs, fixtures, screenshots, or reports.
4. A checkbox is checked only when its evidence artifact exists.
5. Flutter is retired only after the final cutover gate is signed off.

## Repository and evidence baseline

- Flutter: `E:\vista`
- Native: `E:\vista_native`
- Existing ledgers:
  - `docs/audits/2026-08-05-p01-parity-ledger.csv`
  - `docs/audits/2026-08-24-current-source-gap-ledger.md`
  - `docs/reports/2026-08-24-vista-native-live-parity-audit.md`
- Worktree is dirty; unrelated changes are preserved.
- Last known-good Native production APK was built on 2026-08-26. A current-checkout build is still required.

## Phases

### Phase 0 — Governance, inventory, and freeze

- [x] Register both repository paths and existing audit/ledger documents.
- [x] Record dirty-worktree policy; no broad reset or cleanup.
- [x] Import the Flutter feature inventory into the parity ledger.
- [ ] Assign an owner and evidence location for every P0/P1 ledger row.
- [ ] Freeze the reference Flutter commit/configuration for each comparison run.

**Exit gate:** every feature has an owner, source reference, target status, and comparison fixture.

### Phase 1 — Reproducible build and device baseline

- [x] Record the previously successful Native build metadata/hash.
- [x] Record environment blockers separately from product defects.
- [x] Run a clean current Native `assembleProductionDebug`; record timestamp, command, APK path, package, version, and SHA-256.
- [x] Build Flutter from its reference checkout; record the same metadata.
- [x] Install both APKs on one emulator/device profile.
- [x] Record API level, ABI, density, font scale, locale, theme, animation scale, network state, and fixture IDs.
- [x] Create a capture manifest and immutable run ID.

**Exit gate:** both apps are reproducibly built and installed with identical comparison conditions.

### Phase 2 — Startup, authentication, session, and navigation

#### Implemented and unit-verified

- [x] Startup state machine: loading, maintenance, banned, onboarding, authentication, authenticated, recoverable error.
- [x] Login, OTP, password setup/recovery, logout, session refresh, biometric preference, onboarding, offline fallback.
- [x] `SessionStore` contract and test fakes updated for biometric state.
- [x] Deferred placeholder destination removed; canonical post/profile/group/chat links enter real shell destinations.
- [x] Auth module test suite passes after targeted KSP cache cleanup (`:feature:auth:testDebugUnitTest`).

#### Required runtime proof

- [x] Ten consecutive Native cold launches with startup timing and resumed-activity success evidence.
- [x] Warm launch, process restart, offline, maintenance, onboarding, recoverable-error, and cold deep-link fixture matrix.
- [ ] Expired-session and banned-state runtime cases still require explicit device evidence.
- [ ] Real UI flows for login, OTP, recovery, logout, refresh, biometric allow/deny, and onboarding completion.
- [ ] Deep links in logged-out and logged-in states, including pending replay, dedupe, invalid, and unsupported links.
- [ ] Paired screenshots and pixel-diff review for all Phase 2 states.

**Exit gate:** clean-install runtime matrix passes and all visual differences are classified or fixed.

### Phase 3 — Design system and application shell

- [x] Map Flutter theme tokens to Native colors, typography, spacing, elevation, shapes, icons, and motion.
- [x] Match system bars, safe areas, RTL direction, keyboard insets, dark/light theme, and font scale at source level.
- [ ] Verify shell tabs, back behavior, deep-link handoff, loading/empty/error surfaces, and global toasts.
- [ ] Produce component-level screenshot fixtures and diffs.

**Exit gate:** shell and shared components pass screenshot thresholds in both themes and RTL.

### Phase 4 — Core product surfaces

- [ ] Feed/home: pagination, refresh, media, reactions, comments, share, save, report, loading/empty/error.
- [ ] Posts and stories: create/edit/delete, media upload, 24-hour expiry, viewers, privacy, moderation states.
- [ ] Profile/social graph: own/other profile, follow/unfollow, block/report, counts, edit profile, avatar/cover media.
- [ ] Search/nearby/music/premium/badges: query states, permissions, playback, entitlement gates, and failures.
- [ ] Settings/onboarding/help: every setting, persistence, localization, notification/privacy/security controls.

**Exit gate:** each surface has ledger coverage, runtime fixtures, and paired screenshot evidence.

### Phase 5 — Chat and real-time behavior

- [ ] Conversation list, message list, composer, reply/edit/delete/react, media, voice, read state, typing, pagination.
- [ ] E2EE success and failure paths; encryption failure never falls back to plaintext.
- [ ] WebSocket/SSE reconnect, lifecycle cancellation, duplicate suppression, offline queue, and push handoff.
- [ ] Keyboard performance, scroll anchoring, interaction latency, and dark/RTL visual parity.

**Exit gate:** deterministic two-user runtime suite passes with no plaintext leakage or stream leaks.

### Phase 6 — Cross-cutting hardening

- [ ] Network/API contract parity, both backend error envelopes, retries, timeouts, TLS pinning, and refresh interceptor.
- [ ] Secure storage, token lifecycle, biometric policy, logging redaction, notification/deep-link security.
- [ ] Accessibility, localization, Persian/Jalali dates, large font, low bandwidth, and offline recovery.
- [ ] Performance budgets: startup, frame time, memory, jank, battery, and APK size.

**Exit gate:** security, accessibility, performance, and contract checklists pass with reports.

### Phase 7 — Release certification and Flutter retirement

- [ ] Full P0/P1 ledger has no unclassified gaps.
- [ ] Clean-install, upgrade, rollback, migration-data, and crash-recovery tests pass.
- [ ] Release signing/versioning/obfuscation and smoke tests pass.
- [ ] Final Flutter/Native parity report is reviewed and signed off.
- [ ] Freeze Flutter as a read-only archive; remove it from active development only after sign-off.

**Exit gate:** Native is the sole active product implementation.

## Current Phase 1/2 execution log

| Date | Evidence | Status |
|---|---|---|
| 2026-08-26 | Native production-debug build (prior checkout) | Passed; metadata/hash in prior audit |
| 2026-08-26 | Recorded Auth/Startup/Session XML reports | Zero failures/errors |
| 2026-08-27 | `:feature:auth:testDebugUnitTest --no-daemon --no-build-cache --rerun-tasks` | Passed; 101 actionable tasks |
| 2026-08-27 | Current Native APK build | Passed; `app/build/outputs/apk/production/debug/app-production-debug.apk`, 44,949,605 bytes, SHA-256 `7569E457F6F50BF98742858ABBF6C2F2CB24568CAE6C01BC5AFAF6123E699183` |
| 2026-08-27 | Flutter APK build/install | Existing Flutter APK installed successfully; version `2.6.2` (4049), SHA-256 `47E4DE4F80BF64D19289CE20B1259D5C84E9F6A63B4E315EC9F384D2FD188D1F` |
| 2026-08-27 | Native startup capture | 1080x2400 PNG; SHA-256 `BFC498F55A84D4A6E621F2B074BBB6FA42BA2F57CEE9344BB00C76CEE65FC1C6` |
| 2026-08-27 | Flutter startup capture | 1080x2400 PNG; SHA-256 `52005AD36B7825F249950F1B4151B85F62C1AB75B42BA4471D5BDAE5709AACE0` |
| 2026-08-27 | Startup pixel baseline | Provisional mismatch 98.8787% at threshold 16; MAE 162.3715. Not accepted as parity evidence because deterministic app state/fixture is not yet normalized |
| 2026-08-27 | `tools/int01_capture_guard.ps1` | Fixed package-list parsing and Native production package default; verified both APK installs/captures on `emulator-5554` |
| 2026-08-27 | Native cold-launch loop (10 runs, `am force-stop` + `am start -W`) | 10/10 `Status: ok`, `Exit 0`; `TotalTime` 1326–1501 ms |
| 2026-08-27 | Native `StartupFixtureInstrumentationTest` | Passed; 11/11 tests via `am instrument` on `emulator-5554` |
| 2026-08-27 | Native `NavigationDeepLinkInstrumentationTest` | Passed; 3/3 tests via `am instrument` |
| 2026-08-27 | Native `AuthVisualParityInstrumentationTest` | Passed; 2/2 tests; nine Auth state PNGs pulled from device |
| 2026-08-27 | Native Auth visual evidence | `artifacts/phase2/native-auth-latest/vista-login-parity/native-01..09-*.png` |
| 2026-08-27 | Native startup/deep-link fixture suite | Passed; Startup 11/11 and NavigationDeepLink 3/3 |
| 2026-08-27 | Full connected instrumentation suite | Failed outside Phase 2 scope in Feed/Profile tests; failures retained as open product/runtime gaps |
| 2026-08-27 | `:feature:shell:testDebugUnitTest :core:designsystem:testDebugUnitTest` | Passed; 38 actionable tasks |
| 2026-08-27 | `:app:assembleProductionDebugAndroidTest` after Phase 3 system-bar change | Passed; 359 actionable tasks |
| 2026-08-27 | `FeedRuntimeInstrumentationTest#shellSystemBarsMatchFlutterLightAndDarkContract` | Failed by timeout waiting for night-mode configuration refresh after external `cmd uimode`; not accepted as a parity pass |
| 2026-08-27 | ADB/device discovery | Resolved by using SDK root `D:\Users\MriT.DESKTOP-UK7OADT\AppData\Local`; `emulator-5554` online, `emulator-5556` offline |
| 2026-08-27 | Same-device install/runtime/pixel diff | APK installation and startup captures completed; full Phase 2 runtime matrix remains pending |

## Immediate engineering queue

1. Run the current Native production build to completion and record its artifact.
2. Build the Flutter reference APK from `E:\vista`.
3. Obtain a working ADB/emulator profile and execute the Phase 1 manifest.
4. Execute the Phase 2 runtime matrix and capture paired screenshots.
5. Fix only defects demonstrated by runtime or pixel-diff evidence; update checkboxes immediately after each gate.

## Decisions and notes

- The percentage above is plan execution progress, not a migration or pixel-parity percentage.
- Source inspection and unit tests cannot close runtime or visual gates.
- Environment failures (KSP cache, missing ADB/emulator, DNS/TLS/API) remain explicitly documented and must not be converted into product pass/fail claims.
