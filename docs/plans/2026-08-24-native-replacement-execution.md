# Vista Native replacement execution

**Type:** feature  
**Status:** in-progress  
**Progress:** `[██░░░░░░░░] 20%`

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

### Phase 2 — Unblock executable Native baseline (P0)
- [ ] Build the latest Native APK after the Gradle file-lock owner is resolved; prove package/version/activity.
- [x] Verify `DeferredFeature` is not the active deep-link delivery path; shell now opens post/profile/group/chat routes directly. Retain only as dead-code cleanup candidate, not a P0 feature gap.
- [ ] Run authenticated cold/warm/restart and session-expiry paths against Flutter.

### Phase 3 — Core social and media parity (P0)
- [ ] Feed/post detail/actions, comments/replies/composer, profile/follow, search, stories and create-post media.
- [ ] Verify a shared authenticated fixture, Persian RTL, light/dark, IME, paging, offline and error states.

### Phase 4 — Chat and security parity (P0)
- [ ] Conversation/detail, realtime, cursor paging, E2EE fail-closed behavior, encrypted persistence, file/voice/GIF/group flows.
- [ ] Prove no token/plaintext persistence or logs and compare the full composer/emoji surface.

### Phase 5 — Remaining product surfaces (P1)
- [ ] Services/contacts/game/WebView/nearby, settings/privacy/sessions/storage, notifications/deep links, music/premium/share.
- [x] Search QR affordance را به scanner موجود Native وصل کن؛ build/install و scan واقعی همچنان gate باز است.

### Phase 6 — Upgrade and release replacement gate (P0)
- [ ] Signed Flutter-to-Native migration rehearsal and rollback for session, E2EE keys, drafts, pending work and downloads.
- [ ] API/device/theme/font-scale/accessibility/performance/release verification with visual diff artifacts.

## Decisions & Notes

- No reset, stash, clean, broad staging, or overwrite of existing dirty work.
- Flutter package is `ir.coffevista.vista`; Native must remain an explicitly Native-suffixed package.
- A compile/test success is local evidence only. Completion requires fresh authenticated runtime and visual comparison.
- P0 work is selected first. While the build gate is blocked by an external file lock, only a narrow, independently-routed, source-proven P1 gap may be changed; it remains unaccepted until that build gate is cleared.
