# Vista Native replacement execution

**Type:** feature  
**Status:** in-progress  
**Progress:** `[█████░░░░░] 50%`

## Problem / Goal

Make `E:\vista_native` a production replacement for Flutter (`E:\vista`), with Flutter retained as the read-only behavioral and visual source of truth. Every Native capability must be mapped, implemented when missing, and proven against the same authenticated runtime state before the Flutter app can be retired.

## Root cause / Design

The Native checkout has substantial in-progress, unowned dirty work and several existing plans, but no accepted current-state capability ledger that separates a source-level candidate from live parity. Work therefore proceeds by dependency order: executable baseline and dead routes; shared contracts/security; core social/media; chat; remaining feature surfaces; migration and release gates. Each phase owns a narrow feature set and closes only with live Flutter↔Native evidence.

## Phases

### Phase 1 — Establish the canonical current-state ledger
- [x] Preserve dirty-worktree ownership and create a separate execution plan.
- [x] Extract Flutter routes/features/endpoints and Native route/module equivalents into a complete capability ledger: `docs/audits/2026-08-24-current-source-gap-ledger.md`.
- [x] Classify every capability as parity-proven, implementation candidate, partial, absent, inactive, or blocked.
- [x] Reconcile pre-existing plans/audits without treating them as current proof; deep-link and biometric claims were re-checked against checkout source.
- [x] Revalidate the high-risk platform contracts against both source trees: Flutter has recovery endpoints, inbound `SEND`/`SEND_MULTIPLE`, and background audio; Native source still has no recovery or inbound-share contract. Search QR is now wired in Native but remains runtime-unverified.

### Phase 2 — Unblock executable Native baseline (P0)
- [x] Build the latest Native APK and prove package/version/activity. **Evidence 2026-08-24:** `:app:assembleBetaDebug` passed; `E:\vista_native\_phase2_build_output\app\outputs\apk\beta\debug\app-beta-debug.apk`, `ir.coffevista.vista_native`, `2.6.3-beta (4050)`, `ir.coffevista.vista_native/.MainActivity`; installed on `emulator-5554`. The isolated build output avoids pre-existing locked Gradle artefacts without deleting WIP.
- [x] Verify `DeferredFeature` is not the active deep-link delivery path; shell now opens post/profile/group/chat routes directly. Retain only as dead-code cleanup candidate, not a P0 feature gap.
- [ ] Run authenticated cold/warm/restart and session-expiry paths against Flutter. **Evidence 2026-08-24:** after reinstalling the latest beta APK, the emulator rendered an authenticated Native Feed and post detail without a crash; this is only a warm-session smoke test. The beta application ID remains distinct from Flutter and the installed Native production package, so a signed package/update-path migration policy plus same-fixture Flutter comparison are still required before this can pass.

### Phase 3 — Core social and media parity (P0)
- [ ] Feed/post detail/actions, comments/replies/composer, profile/follow, search, stories and create-post media. **Evidence 2026-08-24:** latest beta rendered authenticated Feed, profile, and post-detail/comment composer in Persian RTL. A live Flutter/Native cold-start comparison found Native rendering Gregorian dates; `feedRelativeTime` now uses Jalali dates and Persian digits. `PersianFeedTimeTest`, full APK build, install and the same Feed screenshot verified the change (`۱۴۰۵/۰۵/۲۶`). For the shared live post by `mahshid`, Flutter and a freshly built/installed Native beta showed the same root+reply thread. The Native rail now uses Flutter’s invariant geometry (`right: 35`, reply-agnostic `top: 52`) rather than shrinking/offsetting at reply rows. Native now also mirrors Flutter’s explicit reply reload: it requests up to 1000 comments for the post, rebuilds only the target subtree, displays an in-flight state, and never emits raw transport text into the composer. Inline editing now calls `updateComment(commentId, content)` in both Native surfaces, so it cannot accidentally create a new root/reply; an enforced rerun caught a missing-parent regression and the current source preserves the prior parent/post relationship. A no-cache enforced rerun at 20:00 passed all 12 `CommentsViewModelTest` cases, including partial-to-full nested subtree, raw-error and inline-reply update; production compilation and a fresh 19:57 APK build/install also passed. A 19:41 local Computer capture from the freshly reinstalled APK showed hide→show on the live `mahshid` thread. A three-or-more-reply live fixture, IME/optimistic failure and dark-mode comparison are still open. The same post's empty-comment detail state now has matched data but not matched visual structure: Flutter uses a rounded post card and different spacing/action layout. Search QR route and scanner screen were launch-verified on the freshly installed APK; actual camera/gallery decode remains open.
- [ ] Verify a shared authenticated fixture, Persian RTL, light/dark, IME, paging, offline and error states. **Test gate 2026-08-24:** `:feature:feed:testDebugUnitTest :feature:search:testDebugUnitTest` passed after `StoryRepository` gained an injectable test dispatcher while production Hilt keeps `Dispatchers.IO`. Same-fixture Flutter comparison, dark/IME/paging/offline/error runtime states remain open.

- **Comment-thread visual gate, 20:52:** fresh source compiled, the no-cache `CommentsViewModelTest` rerun passed, and a 20:49 APK was installed. Computer opened the debug-only five-node thread (root, three direct replies, one nested reply): the rail stayed at Flutter's fixed root axis through every row; the composer also respects both IME and navigation-bar safe insets. The native list now retains Flutter's pull-to-refresh behavior and per-comment 0.8-scale/fade entrance. This fixture only intercepts GET comments for its synthetic post, so it is structural visual evidence, not a substitute for the still-open three-or-more-reply live-data gate.
- **Live empty-reply gate, 21:01:** Computer opened the same real `raha.81` post with five comments in Flutter and freshly installed Native. Flutter's explicit italic `هنوز پاسخی وجود ندارد` below each root without a reply is now present in Native with the same list-level placement. The remaining visual proof is a *live* root with three-or-more replies; the deterministic five-node fixture already covers that rail geometry.
- **Rail-coordinate correction, 21:10:** the earlier Compose rail was drawn *after* the Row's 16dp padding, so it was not at Flutter's outer-Stack `right: 35` / `top: 52` coordinates. It now draws before padding. Computer on the freshly installed 21:08 APK opened every node of the five-node thread: each segment crosses the centre of the root and reply avatars and joins continuously to the next row.
- **Comment-author navigation, 21:19:** a fresh `:app:assembleBetaDebug` completed successfully in 53s, was installed, and Computer opened the real `raha.81` post. Tapping the `re.mhd` comment author navigated to that account's `reza` profile. This closes the prior `PostDetailScreen` comment-author no-op; the multi-reply live-data, dark-mode, IME and rollback gates remain open.

### Phase 4 — Chat and security parity (P0)
- [ ] Conversation/detail, realtime, cursor paging, E2EE fail-closed behavior, encrypted persistence, file/voice/GIF/group flows.
- [ ] Prove no token/plaintext persistence or logs and compare the full composer/emoji surface.
- [x] Inbox opaque-preview hardening: latest beta was installed over the authenticated cache on `emulator-5554`. The previously visible Base64 previews for `mahshid` and `ahmadesmaili` now render as `پیام جدید`, while ordinary preview text remains visible. `ChatModelsTest` and `:app:assembleBetaDebug` passed. This is a Native security hardening; Flutter still needs its own product-level correction.

### Phase 5 — Remaining product surfaces (P1)
- [ ] Services/contacts/game/WebView/nearby, settings/privacy/sessions/storage, notifications/deep links, music/premium/share.
- [x] Search QR affordance را به scanner موجود Native وصل کن؛ build/install و scan واقعی همچنان gate باز است.
- [x] Services managed-card parity: Native now normalizes active sections by route/title exactly as Flutter does. `:feature:services:testDebugUnitTest` and a fresh beta APK passed; emulator screenshot confirms duplicate Store is removed and the Store/Competition pair matches Flutter.
- [x] Services WebView failure hardening: native cancels SSL errors and replaces the platform error page with a Persian fail-closed message. Fresh-emulator reproduction of the game SSO TLS failure confirms the transient ticket URL is not rendered. **Open infrastructure blocker:** `coffevista.ir/game/sso` returns an SNI/TLS failure on this emulator; this must be repaired server-side before the Game/Competition flow can pass.

### Phase 6 — Upgrade and release replacement gate (P0)
- [ ] Signed Flutter-to-Native migration rehearsal and rollback for session, E2EE keys, drafts, pending work and downloads.
- [ ] API/device/theme/font-scale/accessibility/performance/release verification with visual diff artifacts.

## Decisions & Notes

- No reset, stash, clean, broad staging, or overwrite of existing dirty work.
- Flutter package is `ir.coffevista.vista`; Native must remain an explicitly Native-suffixed package.
- A compile/test success is local evidence only. Completion requires fresh authenticated runtime and visual comparison.
- P0 work is selected first. While the build gate is blocked by an external file lock, only a narrow, independently-routed, source-proven P1 gap may be changed; it remains unaccepted until that build gate is cleared.
