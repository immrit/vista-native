# Native Chat Performance and Interaction Hardening

**Type:** perf | bug | ui  
**Status:** in-progress  
**Progress:** `[█████░░░░░] 50%`

## Problem / Goal

On a real phone, opening a conversation for the first time makes the IME open with severe jank while messages load. IME transitions and general chat scrolling remain slow. Message context menus and message selection are unreliable. Bring the full chat interaction path to a Telegram-like responsiveness standard without weakening E2EE or overwriting parallel work.

## Root cause / Design

The existing keyboard-lag plan reports static changes, but device feedback proves that it did not close the runtime gate. The audit will trace navigation/initial loading, state collection, Room/realtime work, `LazyColumn` composition, IME insets, gesture handling, context overlay geometry, and selection state. Telegram source is a behavior/performance reference only; no Telegram implementation code is copied.

## Phases

### Phase 1 — Investigate

- [x] Preserve and scope the heavily dirty Native worktree; locate the untracked chat implementation and prior keyboard plan
- [x] Trace first-open flow: navigation -> ViewModel -> repository -> cache/network/realtime -> Compose
- [x] Inspect composition/IME/scroll/gesture hot paths and compare their architecture with local Telegram reference
- [ ] Reproduce and measure on an exclusively identified physical device; record package/activity, fixture, and frame/jank evidence

### Phase 2 — Implement

- [x] Remove first-open main-thread/composition work from the IME path while preserving correct message anchoring
- [x] Make context menu anchoring, dismissal, available actions, and RTL geometry stable across scroll/insets
- [x] Implement deterministic multi-select, back handling, batch actions, and visual selected-state feedback
- [ ] Fix other audit findings only at their owning layer; add focused regression tests where the module already has test coverage

### Phase 3 — Verify

- [ ] Run focused chat unit tests and module compilation
- [ ] Install a proven Native-suffixed APK on the reserved physical device and exercise cold first-open, IME, scroll, context, and multi-select
- [ ] Verify RTL, Persian text, dark/light theme, privacy/E2EE fail-closed behavior, and no plaintext/token logging
- [ ] Update this plan with concrete evidence, remaining issues, and next actions

## Decisions & Notes

- Existing `2026-08-19-native-chat-keyboard-lag.md` says done, but the current physical-device report reopens its runtime claim. Build/test output alone is not accepted as performance proof.
- `feature/chat` is untracked alongside broad parallel changes. This task must not reset, stash, clean, or broadly stage the worktree.
- The first-open path subscribed to encrypted Room rows on the `viewModelScope` main collector (`OfflineFirstChatRepository.kt`), so mapping 50 messages could run 50 Keystore decryptions on the UI thread. Stream mapping now runs on `Dispatchers.Default`; remote calls run on `Dispatchers.IO`.
- `MessageDetailScreen` read animated IME insets in its top-level composition and held bubble coordinates as Compose state. Insets now affect only a leaf layout measurement and coordinates are held outside snapshot state. The full-list tap detector was removed because it competed with message long-press/scroll gestures; context actions are long-press only, as in the local Telegram reference.
- 2026-08-24: the normal focused build was blocked by an external lock on `core/common/build/classes/kotlin/main`; a safe retry excluding that task was blocked by the environment approval timeout. Only an emulator (`emulator-5554`) is currently connected, not the reported physical phone. No runtime or performance gate has been claimed.
- 2026-08-24 live UI check: the preinstalled Native-suffixed production package launches on `emulator-5554`, but it has no authenticated session and stops at the login screen. The login keyboard rendered, while chat keyboard/context-menu/multi-select could not be exercised without a user-authenticated chat fixture. No login automation was attempted.
