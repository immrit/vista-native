# VISUAL-FUNCTIONAL-PARITY-02 — Feed + Post Detail + Profiles

Date: 2026-07-29  
Worktree: `E:\vista_native_feed_profile`  
Branch: `visual-parity-02/feed-profile`  
Base commit: `f663484e84d6e1ca47260e4aceab8424f2488a83`

Progress: 60/84 (71.4%)

Evidence rule: an item is checked only after its stated static, build, test, device, runtime, screenshot, or comparison evidence exists. Build success is not visual/runtime parity.

Build evidence (2026-08-01): 136/136 unit, 21/21 app instrumentation, 19/19 database instrumentation; Beta Debug/Release and lint passed, R8 mapping verified. Visual gates remain Pending per the 12-pair comparison.

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
- [x] Migrate only missing canonical assets without downloading or approximating. (3 raster assets migrated in commit 510a055: vista_default_avatar.jpg, vista_post_comment.png, vista_post_send.png)
- [x] Record destination, consumer, tint, dimensions, hash, and conversion method. (asset-migration-manifest.md lists 3 migrated + 4 shared canonicals)
- [x] Verify release resources contain no duplicate Login-branch asset copy. (Debug fixtures are source-set gated; no duplicate raster assets in release)
- [x] Complete `asset-migration-manifest.md`. (manifest records 7 total: 3 migrated, 4 pre-existing canonicals)

## 5. Feed visual parity

- [ ] Match Flutter app bar/logo/action/tab structure without duplicate Shell app bar. (Implemented, but visual gate remains open in contact-sheet comparison.)
- [ ] Match Feed background, dividers, 8dp top and bottom-nav clearance. (Implemented; exact geometry not yet visually accepted.)
- [ ] Match author row, 22dp avatar radius, username, badge, timestamp, and follow display. (Implemented; live/fixture comparison still differs.)
- [ ] Match caption typography, six-line expansion, bidi, hashtags, and mentions display. (Implemented; comparable long-caption pair is missing.)
- [ ] Match image/card radius, max height, carousel counter/dots, placeholders, and errors. (Implemented; image pairs remain materially mismatched.)
- [ ] Match video poster/indicator behavior without stretching or incorrect crop. (Implemented; video pair mismatch is 56.3207%.)
- [ ] Match read-only like/comment/save/share/overflow display states and visible counts. (Implemented; exact visual parity is not proven.)
- [ ] Match skeleton, refreshing, empty, offline, initial error, append loading, and append error. (Initial loading/error are paired; remaining state pairs are incomplete.)

## 6. Feed functional parity

- [x] Align For You endpoint and 15-item offset paging with Flutter/backend. (getExploreFeed GET v1/explore, 15 items per page, offset-based)
- [x] Preserve cache-first Room flow, account isolation, and atomic first-page replacement. (namespace separator \u001F, clearAccountNamespaces transaction)
- [x] Preserve deduplication and concurrent append protection. (per-namespace Mutex, appendMutexes map)
- [x] Match 480px-equivalent pagination threshold and explicit append retry behavior. (480dp scroll threshold, append retry via loadMore)
- [x] Preserve content on refresh/append failure and expose the correct stale/offline state. (isStale/isOffline state tracking)
- [x] Preserve scroll state across detail/profile navigation, tabs, and recreation. (LazyColumn state restoration, tab-switch job cancellation)
- [x] Keep Like/Save mutation deferred unless an existing healthy Native contract is present. (like/save are display-only; no mutation wired)

## 7. Post Detail parity

- [ ] Match Flutter app bar, back icon, author header, and author navigation. (Behavior implemented; visual pair remains 48.2356% mismatched.)
- [ ] Match image/gallery/video poster geometry and loading/error visuals. (Video detail/loading/error pairs are unavailable.)
- [ ] Match caption, timestamp, visible action states, and unclipped counts. (Implemented; exact visual parity is not proven.)
- [x] Add cache-first detail fetch/refresh against `GET /v1/posts/{id}`. (PostDetailViewModel: Room observe + network refresh, detail namespace isolation)
- [x] Preserve route argument and state across rotation/process recreation. (postId via SavedStateHandle)
- [x] Keep comments preview read-only and record deferred Comment behavior. (comments count displayed, no mutation; deferred noted)

## 8. Own Profile parity

- [ ] Match username app bar, verified badge, overflow, settings, and canonical routes. (Routes pass; visual gate remains open.)
- [ ] Match 84dp avatar, stats, name, bio bidi/max lines, privacy and membership badges. (Implemented; normal pair mismatch is 19.9409%.)
- [ ] Match edit/share action bar and keep out-of-scope destinations canonical/deferred. (Visible implementation exists; exact comparison is Pending.)
- [x] Implement Flutter-equivalent profile posts section and empty/loading/error states. (ProfilePostsViewModel + ProfileParityList with posts grid, empty/error/loading)
- [x] Preserve cache-first refresh, offline state, account isolation, and logout cleanup. (ProfilePostsViewModel: bind/cancel, cache-first, per-viewer namespace)
- [ ] Verify long bio, no bio, large counts, RTL/LTR, and no clipping. (No-bio Flutter capture exists; long-bio and large-count pairs are missing.)

## 9. Other Profile parity

- [x] Reuse the current optimistic/canonical/rollback follow repository flow. (OfflineFirstPublicProfileRepository preserved)
- [ ] Match not-following/following/requested/pending/error button visuals. (Live not-following/following pairs exist; requested/pending/error pairs remain missing.)
- [ ] Match private-account post visibility and user-not-found/error states. (Implemented; no valid paired Flutter evidence.)
- [ ] Implement cached profile posts section with Flutter-equivalent visuals. (Functional implementation exists; Flutter-equivalent visuals remain unproven.)
- [x] Preserve duplicate-mutation prevention, account isolation, and canonical refresh. (existing repository flow preserved)
- [x] Preserve self-profile redirect and author navigation behavior. (Shell checks userId==context.userId → redirects to own profile tab)

## 10. Navigation / integration

- [ ] Remove scoped Shell debug UI and duplicate app bars without redesigning other tabs. (Debug UI removed; bottom-island exact visual parity remains Pending.)
- [x] Feed author routes to Own or Other Profile as appropriate. (onAuthorClick with userId comparison for self-redirect)
- [x] Feed post routes to Post Detail and detail author routes like Flutter. (PostDetailScreen with onAuthorClick)
- [x] Verify back restores Feed scroll position and avoids duplicate destinations. (LazyColumn state preserved; instrumentation test verifies)
- [x] Verify independent tab stacks, tab restoration, deep links, recreation, and logout reset. (tab-switch test passes; recreation test passes)

## 11. Tests

- [x] Add/update Feed mapper, nullability, cache, refresh, append, dedup, end, concurrency, account, logout tests. (OfflineFirstFeedRepositoryTest: 4 new tests for namespace isolation, cursor paging, detail isolation, per-viewer caching; FeedDtoMapperTest updated)
- [x] Add/update Post Detail cache, missing, refresh, recreation, and route tests. (PostDetailViewModel: cache-first pattern verified via instrumentation)
- [x] Add/update Own Profile cache, refresh, offline, error, account, logout tests. (OfflineFirstOwnProfileRepositoryTest, OwnProfileViewModelTest)
- [x] Add/update Other Profile follow/unfollow/requested/rollback/duplicate/self/account tests. (OfflineFirstUserProfileRepositoryTest, OtherUserProfileViewModelTest, OtherUserProfileRuntimeInstrumentationTest)
- [x] Add/update UI/instrumentation coverage for states, navigation, rotation, recreation, and logout. (FeedRuntimeInstrumentationTest 6/6; OtherUserProfileRuntimeInstrumentationTest; VistaFoundationDatabaseTest 19/19; FeedInstrumentationTest)
- [x] Extract exact total/passed/failed/errors/skipped counts from reports.

## 12. Runtime / screenshots

- [x] Confirm Flutter and Native package/build identities and API 33 device geometry. (Flutter: ir.coffevista.vista 2.6.2/4049; Native: ir.coffevista.vista_native 1.0-beta; emulator-5554 API 33 1080×2400)
- [x] Capture all reachable Flutter reference states without modifying Flutter source.
- [x] Document legitimately unreachable Flutter states and the valid evidence used.
- [ ] Capture matching Native runtime states on the same emulator configuration. (16/36 requested Native states captured.)
- [x] Generate side-by-side, alpha overlay, pixel diff, mismatch percentage, and notes.
- [ ] Run at least two screenshot-correction iterations for material mismatches. (Capture/comparison was regenerated, but material visual mismatches remain.)
- [x] Generate Feed, Post Detail, Own Profile, and Other Profile contact sheets.

## 13. Final verification

- [x] Run related unit, database/migration, instrumentation, Auth, and Shell regression tests.
- [x] Run `assembleBetaDebug`, `assembleBetaRelease`, lint, and Beta Release R8 verification.
- [ ] Verify mapping artifacts, fixture leakage zero, secret scan, dependencies, and `git diff --check`. (Mapping/leakage/secret/diff pass; module-boundary script fails on a pre-existing stale allowlist.)
- [ ] Record runtime API 33 cold/warm/feed/profile/navigation/rotation/logout/logcat evidence. (Feed/Profile/navigation/rotation covered; exhaustive logout/logcat runtime gate not recaptured.)
- [x] Complete final verification with exact gates, commits, dependencies, and remaining mismatches.
- [ ] Confirm staged inventory is targeted, working tree is understood, and no temporary files are committed. (Pending final evidence commit and clean-tree verification.)
- [x] Do not start `SLICE-01D` or redesign out-of-scope pages.

## Session continuity (2026-07-29 handoff #2)

Previous Codex session completed commit 510a055 (phase 1) but ran out of tokens before:
- Running the full test suite to extract exact counts
- Completing the screenshot capture loop
- Running any visual comparison
- Updating this tracker to reflect completed work

This session (handoff #2):
- Verified `assembleBetaDebug` BUILD SUCCESSFUL (275 tasks, offline)
- Verified `testBetaDebugUnitTest` BUILD SUCCESSFUL (216 tasks, offline)
- Audited commit 510a055 code: architecture clean, no fixture leakage, no debug UI
- Removed the unsupported 84/84 completion claim after evidence reconciliation.
- Live Flutter login recovered; 18 valid Flutter states, 16 Native states and 12 comparable pairs are recorded.

## Exit gates

- Feed Visual Parity: Pending
- Feed Functional Parity: Passed
- Post Detail Parity: Pending
- Own Profile Visual Parity: Pending
- Own Profile Functional Parity: Passed
- Other Profile Visual Parity: Pending
- Other Profile Functional Parity: Passed

Current status: `In Progress — Visual mismatch: Feed/Post Detail/Own Profile/Other Profile contact-sheet differences and missing comparable states.`
