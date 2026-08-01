# VISUAL-FUNCTIONAL-PARITY-03 — Search End-to-End Migration

Status: In Progress
Progress: `54 / 68` (`79.4%`)
Worktree: `E:\vista_native_search`
Branch: `visual-functional-parity-03/search`
Base: `a8bd9675ffa6ae434e826fc9c6ccd9e0a8fa1b4c`

Evidence rule: a checked item has current source, build, test, device, runtime, or visual evidence. A successful compile does not close runtime or parity gates.

## 1. Git / Worktree baseline

- [x] Confirm the exact base object exists and is a commit.
- [x] Record original branch, HEAD, status, staged files, and untracked files.
- [x] Record the last 15 commits, worktrees, and local branches.
- [x] Confirm original `git diff --check` is clean.
- [x] Confirm `.git/index.lock` is absent before worktree creation.
- [x] Create and re-verify the isolated Search worktree and branch.

## 2. Flutter Search audit

- [x] Trace the Search launcher and full workspace screen.
- [x] Trace query input, 300 ms debounce, submit, and stale-response behavior.
- [x] Trace user, hashtag, mixed-result, loading, empty, error, and retry rendering.
- [x] Trace local recent-search persistence, ordering, deduplication, cap, delete, clear, and logout wipe.
- [x] Trace user and post navigation plus tab-lifetime behavior.
- [ ] Run the current Flutter source on API 33 and verify source-audited behavior.
- [ ] Close every Flutter ledger gap with Native evidence or an explicit deferral.

## 3. Backend contract audit

- [x] Extract the profile search and exact-username contracts.
- [x] Extract hashtag post search, hashtag suggestions, and trending contracts.
- [x] Extract effective limits, offset/cursor behavior, optional authentication, and empty envelopes.
- [x] Extract nullable user/post/media fields and response envelopes.
- [x] Extract validation and server error envelope behavior.

## 4. Asset / icon migration

- [x] Prove Search body icons are Flutter Material icons rather than custom bitmap/SVG assets.
- [x] Inventory the custom bottom-navigation Search SVG with hash and viewBox.
- [x] Confirm there is no Search-specific empty-state illustration in Flutter.
- [x] Migrate the bottom-navigation Search vector and verify tint/size consumers.

## 5. Domain / data architecture

- [x] Add `feature:search` with isolated Gradle boundaries.
- [x] Add DTO, domain, persisted-history, and UI models without leaking DTOs into Compose.
- [x] Implement cancellable Retrofit APIs matching Backend paths and envelopes.
- [x] Implement repository orchestration for exact username, profiles, hashtag posts, suggestions, and trending.
- [x] Implement account-isolated history persistence with a maximum of 20 items.
- [x] Prove Debug fixtures cannot be resolved from Release sources.

## 6. Search UI

- [ ] Match the launcher field, trending zero-state, spacing, background, and system bars.
- [ ] Match the workspace field, focus state, icons, keyboard action, and clear behavior.
- [x] Match the three Flutter tabs and 200 ms content transition behavior.
- [x] Match user rows, 46 dp avatars, highlight styles, verified badge behavior, and chevron direction.
- [x] Match the 3-column square post grid, 2 dp spacing, and 8 dp corner radius.
- [ ] Match loading, empty, error banner, retry, suggestion panel, and bottom clearances.

## 7. Search behavior

- [x] Normalize trim, leading `@`, leading `#`, hashtag whitespace, and case exactly at the proper layer.
- [x] Debounce typing by 300 ms while allowing immediate IME submit.
- [x] Cancel prior jobs, enforce latest-query-wins, and ignore stale responses.
- [x] Suppress duplicate normalized queries without suppressing retry.
- [x] Reset results, pagination, suggestions, and errors when the query changes or clears.
- [x] Restore query and selected tab across rotation/process recreation without fake results.

## 8. History

- [x] Add selected users as `@username` and selected tags as `#tag`.
- [x] Move duplicate selections to the front and preserve descending recency.
- [x] Delete one item and clear all with the same confirmation/state behavior as Flutter.
- [x] Isolate history by account and clear the account history on logout.

## 9. Pagination / offline

- [x] Implement profile `limit=20` / `offset` append with 240 dp prefetch threshold behavior.
- [x] Deduplicate appended users and retain content on append failure.
- [x] Preserve Flutter's no-post-load-more behavior while parsing Backend `has_more`/`next_cursor`.
- [x] Match Flutter offline/error differences for direct hashtag versus supplementary tag lookup.

## 10. Navigation

- [x] User result routes to own or other profile without duplicating Profile UI.
- [ ] Post result routes to the existing Post Detail UI.
- [ ] Back and tab switching restore Search query, results, selected tab, and scroll ownership.
- [x] Keep Shell integration minimal and isolated in a conflict-friendly commit.

## 11. Tests

- [x] Pass normalization, mapper, nullable-field, empty/error-envelope, and contract tests.
- [x] Pass debounce, cancellation, latest-query, duplicate suppression, retry, and recreation ViewModel tests.
- [x] Pass user pagination, deduplication, append-error, and end-state tests.
- [x] Pass history add/dedup/delete/clear/cap/account-isolation tests.
- [ ] Pass Search Compose/instrumentation and navigation restoration tests with zero skipped tests.

## 12. Runtime

- [ ] Clean-install Flutter and capture the applicable real API 33 state inventory.
- [ ] Clean-install Native Beta Debug and capture the matching real API 33 states.
- [ ] Complete real-account Search smoke without storing or reporting credentials.
- [ ] Verify rotation, recreation, logout, and logcat with no Search crash or ANR.

## 13. Visual comparison

- [x] Generate side-by-side, alpha overlay, thresholded diff, metrics, boxes, and notes for every valid pair.
- [ ] Run at least two capture/compare/fix cycles for product mismatches.
- [x] Generate the final `Flutter | Native | Diff` contact sheet.

## 14. Verification

- [x] Pass Search tests, Auth/Shell regression, lint, Beta Debug, Beta Release, and R8 verification.
- [x] Record exact total/passed/failed/errors/skipped counts from generated reports.
- [x] Pass module boundary, fixture leakage, secret scan, credential scan, and `git diff --check`.
- [ ] Create scoped commits, verify staged zero, record final gates, and stop for user visual approval.

## Exit gates

- Search Visual Parity: Pending
- Search Functional Parity: Pending
- Search Backend Contract: Passed
- Search Navigation: Pending
- Search Runtime API 33: Pending

Current stop condition: `In Progress — visual state coverage, Search-only Post Detail handoff, and exhaustive runtime/navigation evidence remain open`
