# VISUAL-FUNCTIONAL-PARITY-02 — Feed + Post Detail + Profiles

Date: 2026-07-29  
Worktree: `E:\vista_native_feed_profile`  
Branch: `visual-parity-02/feed-profile`  
Base commit: `f663484e84d6e1ca47260e4aceab8424f2488a83`

Progress: 22/84 (26.2%)

Evidence rule: an item is checked only after its stated static, build, test, device, runtime, screenshot, or comparison evidence exists. Build success is not visual/runtime parity.

## 1. Git / Worktree baseline

- [x] Record original branch, HEAD, staged, unstaged, and untracked state (terminal baseline: original `visual-parity-01/flutter-assets-login`, HEAD `f663484`, staged empty, dirty unstaged/untracked).
- [x] Record the latest 15 commits and all local branches (terminal baseline captured before worktree creation).
- [x] Record all worktrees and confirm `.git/index.lock` is absent (terminal baseline captured; lock absent).
- [x] Create `E:\vista_native_feed_profile` on `visual-parity-02/feed-profile`.
- [x] Verify the new worktree is clean and at the exact base commit.
- [x] Record Login branch dependency: unpublished Login work remains dirty and is not imported.

## 2. Flutter screen inventory

- [x] Trace Home/Shell to Feed route and tab ownership (parity ledger confirmed call chains).
- [x] Trace For You and Following feed widgets and providers (parity ledger).
- [x] Trace feed card, image carousel, video, actions, skeleton, empty, and error widgets (parity ledger).
- [x] Trace Post Detail route, provider, media, counts, comments preview, and back behavior (parity ledger).
- [x] Trace Own Profile route, header, actions, tabs, post list, and refresh behavior (parity ledger).
- [x] Trace Other Profile route, follow states, privacy behavior, and post visibility (parity ledger).
- [x] Trace theme, fonts, localization, icons, animations, and bidi helpers used by scope (source inventory + asset manifest).

## 3. Flutter behavior / data audit

- [x] Record Feed initial/refresh/append/dedup/end/error behavior and pagination contracts (parity ledger).
- [x] Record cache/offline/session dependencies and identify Flutter versus Native differences (parity ledger).
- [x] Record Post Detail cache/network/recreation behavior and route arguments (parity ledger).
- [x] Record Own Profile profile/posts refresh and cache behavior (parity ledger).
- [x] Record Other Profile follow/unfollow/requested/private/rollback behavior (parity ledger).
- [x] Verify backend endpoints, DTO envelopes, nullability, errors, and paging semantics (backend handlers/services traced).
- [x] Complete the parity ledger with source-to-action evidence for every scoped state.

## 4. Asset migration

- [x] Inventory all Feed/Profile raster, SVG, and icon-font consumers from Flutter source (asset manifest).
- [x] Hash every source asset and check for existing canonical Native copies (asset manifest + byte-identical logo check).
- [ ] Migrate only missing canonical assets without downloading or approximating.
- [ ] Record destination, consumer, tint, dimensions, hash, and conversion method.
- [ ] Verify release resources contain no duplicate Login-branch asset copy.
- [ ] Complete `asset-migration-manifest.md`.

## 5. Feed visual parity

- [ ] Match Flutter app bar/logo/action/tab structure without duplicate Shell app bar.
- [ ] Match Feed background, dividers, 8dp top and bottom-nav clearance.
- [ ] Match author row, 22dp avatar radius, username, badge, timestamp, and follow display.
- [ ] Match caption typography, six-line expansion, bidi, hashtags, and mentions display.
- [ ] Match image/card radius, max height, carousel counter/dots, placeholders, and errors.
- [ ] Match video poster/indicator behavior without stretching or incorrect crop.
- [ ] Match read-only like/comment/save/share/overflow display states and visible counts.
- [ ] Match skeleton, refreshing, empty, offline, initial error, append loading, and append error.

## 6. Feed functional parity

- [ ] Align For You endpoint and 15-item offset paging with Flutter/backend.
- [ ] Preserve cache-first Room flow, account isolation, and atomic first-page replacement.
- [ ] Preserve deduplication and concurrent append protection.
- [ ] Match 480px-equivalent pagination threshold and explicit append retry behavior.
- [ ] Preserve content on refresh/append failure and expose the correct stale/offline state.
- [ ] Preserve scroll state across detail/profile navigation, tabs, and recreation.
- [ ] Keep Like/Save mutation deferred unless an existing healthy Native contract is present.

## 7. Post Detail parity

- [ ] Match Flutter app bar, back icon, author header, and author navigation.
- [ ] Match image/gallery/video poster geometry and loading/error visuals.
- [ ] Match caption, timestamp, visible action states, and unclipped counts.
- [ ] Add cache-first detail fetch/refresh against `GET /v1/posts/{id}`.
- [ ] Preserve route argument and state across rotation/process recreation.
- [ ] Keep comments preview read-only and record deferred Comment behavior.

## 8. Own Profile parity

- [ ] Match username app bar, verified badge, overflow, settings, and canonical routes.
- [ ] Match 84dp avatar, stats, name, bio bidi/max lines, privacy and membership badges.
- [ ] Match edit/share action bar and keep out-of-scope destinations canonical/deferred.
- [ ] Implement Flutter-equivalent profile posts section and empty/loading/error states.
- [ ] Preserve cache-first refresh, offline state, account isolation, and logout cleanup.
- [ ] Verify long bio, no bio, large counts, RTL/LTR, and no clipping.

## 9. Other Profile parity

- [ ] Reuse the current optimistic/canonical/rollback follow repository flow.
- [ ] Match not-following/following/requested/pending/error button visuals.
- [ ] Match private-account post visibility and user-not-found/error states.
- [ ] Implement cached profile posts section with Flutter-equivalent visuals.
- [ ] Preserve duplicate-mutation prevention, account isolation, and canonical refresh.
- [ ] Preserve self-profile redirect and author navigation behavior.

## 10. Navigation / integration

- [ ] Remove scoped Shell debug UI and duplicate app bars without redesigning other tabs.
- [ ] Feed author routes to Own or Other Profile as appropriate.
- [ ] Feed post routes to Post Detail and detail author routes like Flutter.
- [ ] Verify back restores Feed scroll position and avoids duplicate destinations.
- [ ] Verify independent tab stacks, tab restoration, deep links, recreation, and logout reset.

## 11. Tests

- [ ] Add/update Feed mapper, nullability, cache, refresh, append, dedup, end, concurrency, account, logout tests.
- [ ] Add/update Post Detail cache, missing, refresh, recreation, and route tests.
- [ ] Add/update Own Profile cache, refresh, offline, error, account, logout tests.
- [ ] Add/update Other Profile follow/unfollow/requested/rollback/duplicate/self/account tests.
- [ ] Add/update UI/instrumentation coverage for states, navigation, rotation, recreation, and logout.
- [ ] Extract exact total/passed/failed/errors/skipped counts from reports.

## 12. Runtime / screenshots

- [ ] Confirm Flutter and Native package/build identities and API 33 device geometry.
- [ ] Capture all reachable Flutter reference states without modifying Flutter source.
- [ ] Document legitimately unreachable Flutter states and the valid evidence used.
- [ ] Capture matching Native runtime states on the same emulator configuration.
- [ ] Generate side-by-side, alpha overlay, pixel diff, mismatch percentage, and notes.
- [ ] Run at least two screenshot-correction iterations for material mismatches.
- [ ] Generate Feed, Post Detail, Own Profile, and Other Profile contact sheets.

## 13. Final verification

- [ ] Run related unit, database/migration, instrumentation, Auth, and Shell regression tests.
- [ ] Run `assembleBetaDebug`, `assembleBetaRelease`, lint, and Beta Release R8 verification.
- [ ] Verify mapping artifacts, fixture leakage zero, secret scan, dependencies, and `git diff --check`.
- [ ] Record runtime API 33 cold/warm/feed/profile/navigation/rotation/logout/logcat evidence.
- [ ] Complete final verification with exact gates, commits, dependencies, and remaining mismatches.
- [ ] Confirm staged inventory is targeted, working tree is understood, and no temporary files are committed.
- [ ] Stop at `Feed/Profile Parity Ready — Awaiting User Visual Approval`; do not start `SLICE-01D`.

## Exit gates

- Feed Visual Parity: Pending
- Feed Functional Parity: Pending
- Post Detail Parity: Pending
- Own Profile Visual Parity: Pending
- Own Profile Functional Parity: Pending
- Other Profile Visual Parity: Pending
- Other Profile Functional Parity: Pending

Current status: `In Progress — Flutter reference capture waiting for the shared Emulator to become idle`
