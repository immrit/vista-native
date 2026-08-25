# Native Messenger Real-Runtime Parity

**Type:** feature | ui | perf
**Status:** blocked-on-emulator-network
**Progress:** `[█████░░░░░] 50%`

## Problem / Goal

Bring the Native messenger to evidence-backed functional and visual parity with the Flutter reference using the same authenticated account and real conversation data. Investigate the reported `raha` message-loading and IME jank defects, then close the Native-only gaps without weakening encrypted-content or session handling.

## Root cause / Design

Flutter is read-only behavior and visual truth. Native work follows the full chain: Compose route -> ViewModel -> offline-first repository -> Room/remote API/realtime, with IME insets confined to the composer rather than the message viewport. Parity is accepted only after paired, same-fixture runtime captures; build/test success is necessary but not sufficient.

## Phases

### Phase 1 — Investigate
- [x] Preserve existing dirty work and inventory prior Native chat plans and artifacts.
- [ ] Capture authenticated Flutter reference for conversation list, `raha` detail, keyboard, and each reachable chat action. (List/detail/IME captured; full action sweep remains.)
- [x] Capture the same Native fixture and trace message loading, window insets, recomposition, pagination, and realtime paths.
- [x] Produce a Flutter-to-Native capability-gap matrix with source anchors and runtime evidence.
- [x] Inspect the public upstream animation/layout approaches for transferable non-copied principles.

### Phase 2 — Implement
- [ ] Fix the responsible data/pagination/realtime layer for the `raha` loading defect.
- [x] Fix IME transition work at the narrowest Native layout/state layer.
- [x] Implement prioritized missing interactions and fluent Compose transitions, including hold/lock/cancel voice recording behavior.
- [ ] Update the capability-gap matrix as each Native gap is closed.

### Phase 3 — Verify
- [ ] Run targeted Native tests and assemble the app.
- [ ] Reinstall verified Native package and repeat real authenticated flows.
- [ ] Capture paired Flutter/Native screenshots for list, chat, and keyboard; record differences or quantitative comparison.
- [ ] Verify RTL, dark theme, privacy boundaries, and no credential/token/plaintext persistence or logging.

### Phase 4 — Completion Gate (no return-work backlog)
- [ ] Emulator DNS, HTTPS, REST, and authenticated WebSocket are proven against `api.coffevista.ir` before interpreting any chat failure as an app defect.
- [ ] Run the complete touch-action sweep in Flutter and Native: list search/menu, note tray, new conversation, row swipe/long-press actions, detail header/menu/search, message tap/long-press/swipe/reaction/reply/edit/forward/delete/pin, attachments, emoji/GIF, scroll-to-latest, and group/user sheets.
- [ ] Execute safe real-data checks: load first and older message pages; verify refresh/reconnect/deduplication; send no permanent test data unless explicitly authorized, and clean up any authorized temporary test data.
- [ ] Exercise voice permission, hold, cancel, lock, send, playback, speed, failure, and rotation/background interruption paths; retain no voice plaintext outside normal encrypted attachment handling.
- [ ] Build `:feature:chat` tests and app APK; prove final APK package/activity before install.
- [ ] Reinstall the final Native APK, repeat the authenticated `raha.81` fixture, and capture Flutter/Native list, detail, IME-open, action menu, attachment, and voice states.
- [ ] Record visual differences quantitatively and resolve every material mismatch, or explicitly document a Flutter-side/environment-only limitation.
- [ ] Mark this plan `done` only when every item is checked and the evidence paths are present.

## Decisions & Notes

- Credentials supplied by the user are used only for the live sign-in workflow and are not written to source, plans, logs, or screenshots.
- Existing plan `2026-08-19-native-chat-keyboard-lag.md` reports a prior source/build fix; it still requires fresh device validation with the requested conversation.
- No external product names are introduced into Native code identifiers, comments, resources, or files.
- Live server validation is currently blocked by the emulator's DNS failure (`UnknownHostException`); both clients rendered cached real data, so no data-layer change was made without a reproducible failure.
- Targeted Gradle verification is pending because the sandbox could not download the required Gradle distribution; escalation review then disconnected before approval.
- Emulator recovery evidence: gateway and direct IP traffic work, while name resolution fails; Wi-Fi reconnect and Private DNS did not resolve it. The next safe action is a same-AVD restart with explicit DNS servers.
