# Flutter-to-Native Settings Parity

**Type:** ui
**Status:** in-progress
**Progress:** `[██████░░░░] 60%`

## Problem / Goal

Reproduce the authenticated Flutter Settings flow and every reachable settings subpage in the Native Android app, using the same live account and evidence-backed screenshot comparison.

## Root cause / Design

Flutter at `E:\vista\lib\features\settings\screens\Settings.dart` is the visual and behavioral source of truth. Native has an untracked Settings implementation at `E:\vista_native\feature\profile\src\main\java\ir\coffevista\vista_native\features\profile\settings`; its ownership and parity must be audited before any modification. Evidence will use separately installed Flutter (`ir.coffevista.vista`) and Native package identities on one reserved emulator, with no credentials placed in files or logs.

## Phases

### Phase 1 — Investigate
- [x] Inspect worktree ownership, package identities, Flutter settings entry points, and Native routes.
- [x] Reserve a running emulator; confirm Flutter and Native resolved package/activity identities and clear any emulator-wide proxy.
- [x] Capture authenticated Flutter main settings, every reachable subpage, scrolled states, choice sheets, and empty/error states that are reachable with live data.
- [x] Produce a Flutter-to-Native route, state, and visual contract ledger with file:line references.

### Phase 2 — Implement
- [x] Audit the existing Native Settings WIP against the Flutter contract; modify only files demonstrably needed for parity.
- [x] Remove fixture identity values from the main Settings profile card and edit-profile form.
- [x] Align the account route with Flutter and replace fixture posts/sessions with honest empty states; initialize available privacy values from the own-profile contract.
- [ ] Connect all settings operations to the actual Native data/network layer; do not substitute fixtures. (Edit Profile profile fields and save operation completed; other settings remain.)
- [ ] Implement missing routes, dialogs/sheets, persistence, RTL/accessibility behavior, and navigation.

### Phase 3 — Verify
- [ ] Build and run the affected Native variant with its non-Flutter package identity.
- [ ] Capture Native screenshots for the exact Flutter states using the same account and emulator.
- [ ] Generate paired side-by-side, overlay/diff, and quantitative metrics; fix acceptance-blocking mismatches.
- [ ] Verify RTL, dark/light theme, back navigation, and real-data state transitions.

## Decisions & Notes

- Both repositories are substantially dirty with parallel work. No reset, stash, clean, broad staging, or overwrite is permitted.
- 2026-08-20: `adb devices -l` returned no connected emulator. Flutter runtime capture must occur before Native implementation or comparison; this is the current external blocker.
- Existing untracked capture scripts and settings code are treated as user/parallel work until ownership and current runtime behavior are confirmed.
- 2026-08-21: the main Settings card now renders the fetched profile username rather than a hard-coded email. Edit Profile no longer pre-fills another user's identity. Details of UI-only behavior are in `docs/reports/2026-08-21-settings-ui-only.md`.
- 2026-08-21: `:app:assembleBetaDebug` and focused `:feature:profile:compileDebugKotlin` both reached existing build-directory permission/lock failures. No build outputs were deleted or overwritten to work around this.
- 2026-08-21: native `حساب کاربری` had navigated to the public-profile route, unlike Flutter's edit-profile route. It now uses the edit-profile callback. Saved Posts and Active Sessions had fixture content; fixtures were removed rather than presented as real account data.
- 2026-08-21: reserved `emulator-5554` has Flutter package `ir.coffevista.vista`, Native production package `ir.coffevista.vista_native.production`, and no stale HTTP proxy (`:0`). Captured 28 Flutter Settings artifacts under `artifacts/screenshots/flutter_settings`, including all routes driven by `scripts/capture_flutter_settings.js`.
- 2026-08-21: building the current Native APK requires externally approved network access for Gradle. Two approval-review attempts failed before Gradle ran; do not substitute an old APK as evidence for the current source.
- 2026-08-21: Edit Profile now uses the Flutter-confirmed `POST /v1/me/profile/update` contract for identity, contact, bio, website, demographic, and visibility fields. Those fields are persisted in the own-profile Room cache via a reversible 8-to-9 migration. Avatar selection/upload remains intentionally unconnected because no compatible Native media-upload contract exists yet.
