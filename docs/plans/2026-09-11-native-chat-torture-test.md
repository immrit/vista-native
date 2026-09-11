# Native chat torture test

**Type:** bug
**Status:** planning
**Progress:** `[░░░░░░░░░░] 0%`

## Problem / Goal
Exercise the native chat implementation as an aggressive real-user session, compare its behavior with `E:/vista`, and repair confirmed gaps. Coverage includes one-to-one and group chat, ordinary and secret messaging, attachments, cache/offline recovery, reply flows, notes, and story replies.

## Root cause / Design
Start from the live UI without making external changes, then trace every confirmed failure through the Compose screen, ViewModel/provider equivalent, repository, service, and backend contract. Secret-chat validation must prove that encryption failure never falls back to plaintext.

## Phases

### Phase 1 — Investigate
- [ ] Inventory native and Flutter chat capabilities and their code paths
- [ ] Inspect the running native app, conversation with `mahshid`, available groups, and test-state prerequisites
- [ ] Check backend chat endpoints/contracts if the sibling backend checkout is available

### Phase 2 — Execute and repair
- [ ] Run read-only navigation and rendering checks (chat list, messages, search, notes, story replies)
- [ ] Run confirmed live-message and attachment scenarios with `mahshid` and a test group
- [ ] Validate secret chat encryption and failure behavior without exposing message plaintext in logs
- [ ] Validate cache, process restart, and offline/reconnect behavior
- [ ] Implement minimal fixes for confirmed divergences

### Phase 3 — Verify
- [ ] Re-run every affected live scenario
- [ ] `./gradlew :feature:chat:compileDebugKotlin` succeeds
- [ ] RTL, light/dark theme, and no plaintext/token logging remain correct

## Decisions & Notes
Live sends, uploads, and messages require a confirmation immediately before the first external action. Use only disposable test content and do not delete other users' data.
