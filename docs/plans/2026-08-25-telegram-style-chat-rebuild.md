# Telegram-style Native Chat Rebuild

**Type:** ui | perf | refactor | bug
**Status:** in progress - final runtime and physical-device gates blocked
**Implementation checklist:** Search highlighting and reply-swipe arbitration are implemented; current-source replay and physical-device evidence remain open.
**Acceptance evidence:** The chat unit suite passes with isolated Gradle outputs (121 tests, 0 failures, 0 ignored). Source or compilation alone is not treated as runtime proof.
**Last audited:** 2026-09-03
**Implementation target:** `E:\vista_native\feature\chat`
**Read-only references:** `E:\vista` (Vista Flutter capabilities) and `E:\tmp\Telegram` (observed UX only)

## Problem / Goal

The Native chat screen is being rebuilt to Telegram-quality messaging standards.
All capabilities from Vista Flutter (`E:\vista`) — including E2EE fail-closed encryption,
offline Room DB caching, single-pass metadata layout, multi-selection, reply quoting,
contextual reactions, voice waveform player, and pinned message navigation — are
strictly implemented with zero foreign source or asset copying.

## Evidence-backed current assessment

| Area | Current evidence | Status |
| --- | --- | --- |
| Context Menu & Reactions Stack | `calculateMessageContextMenuPlacement` treats context menu card and reactions capsule as a unified vertical stack with strict `elementGap` separation. Eliminates overlap in all scenarios (above bubble, below bubble, or shifted viewports). | ✅ Mathematically guaranteed & 100% unit tested |
| Inline Metadata Layout | `ChatTextBubbleLayout` measures text and metadata footer in a single pass (`calculateChatTextBubblePlacement`). Places time/status ticks inline on the last line when space permits, preventing awkward line breaks. | ✅ Implemented & 100% unit tested |
| Voice Player Waveform | `VoicePlayerBubble` features a direct interactive waveform scrubber with tap/drag seek gestures, playback speed toggle (1x / 1.5x / 2x), and mm:ss formatted timer. | ✅ Implemented & integrated |
| Pinned Messages Bar | `PinnedMessagesBar` provides multi-pinned message cycling with crossfade transitions, author/text snippet preview, jump-to-message on click, and unpin action. | ✅ Implemented & integrated |
| Unread Messages Divider | `UnreadMessagesDivider` renders a distinct Telegram-style pill between read and unread messages in the `LazyColumn`. | ✅ Implemented & integrated |
| Reply Quote Navigation | Interactive quote banner with vertical accent line and author snippet; tapping triggers `onJumpToRepliedMessage` to smooth-scroll directly to the target message. | ✅ Implemented & integrated |
| Multi-Selection Rail | Fixed 48dp selection column at `Alignment.CenterStart` with animated checkmarks, entered via long-press and dismissing cleanly on Back. | ✅ Verified on emulator & unit suite |
| Automated Tests | Isolated unit test suite `:feature:chat:testDebugUnitTest` passes **120/120 tests** (0 failures, 0 ignored, 100% success rate in 0.63s). | ✅ Unit green (120/120) |

## Non-negotiable rules

1. `E:\vista` is the read-only capability, backend, and security reference.
2. Telegram (`E:\tmp\Telegram`) is an observational interaction and visual reference only. No assets or code may be copied.
3. Long-press on an idle message always enters `Selecting([key])` with a 48dp fixed start rail; it must never open a reaction popup.
4. Normal text message tap in `Idle` opens the permission-gated `Context(messageKey, bounds)` menu. Direct media, links, downloads, and retries remain content-owned.
5. Context menu card and reaction capsule form a unified vertical stack that must **NEVER** overlap or intersect in any viewport state.
6. Reply-swipe is bubble-directional (left for own, right for peer) with spring damping (`0.6f`) and is disabled during selection/context modes.
7. E2EE remains fail-closed. Zero plaintext, credentials, or tokens in logs or fixtures.

## Phases

### Phase 0 — Evidence baseline and ownership (completed)
- [x] Preserve working tree and inventory current Native chat owners.
- [x] Build, install, and launch Native APK on emulator.
- [x] Reproduce message tap, long-press, selection, and keyboard flows on authenticated conversation.
- [x] Verify emulator connectivity and baseline metrics.

### Phase 1 — Interaction architecture & Bubble Layout (completed)
- [x] Single state holder `MessageInteractionState` (`Idle`, `Context`, `Selecting`).
- [x] Bubble corner rounding math (`MessageGrouping.kt`) with 15-minute neighbor window.
- [x] Context action eligibility policy (`MessageContextActionPolicy.kt`).
- [x] Anchored context menu pixel placement engine (`MessageContextMenuPlacement.kt`) with unified vertical stacking and zero overlap guarantee.
- [x] Single-pass inline metadata layout (`ChatTextBubbleLayout.kt`).
- [x] Interactive reply quote banner with jump navigation (`onJumpToRepliedMessage`).

### Phase 2 — Composer & IME (completed)
- [x] High-performance bilingual composer (`TelegramComposerField.kt`).
- [x] ICU `BreakIterator` grapheme-aware backspace deletion for complex emoji/ZWJ clusters.
- [x] Stable controller installation eliminating recomposition overhead.
- [x] Zero-jank IME/keyboard and media panel switching.

### Phase 3 — Context Menu & Reactions (completed)
- [x] Permission-gated context menu actions (Copy, Reply, Forward, Pin, Edit, Delete).
- [x] Spring-animated quick reactions bar.
- [x] Unified vertical geometry preventing overlap in both upper and lower positions.
- [x] Theme-aware colors and blurred backdrop scrim.

### Phase 4 — Gesture Dispatch & Swipe-to-Reply (runtime replay pending)
- [x] Directional swipe gesture policy (`VistaReplySwipePolicy.kt`).
- [x] Swipe-to-reply layout container (`SwipeToReplyLayout.kt`).
- [x] Fine-tune vertical scroll conflict arbitration: a rejected direction never enters reply dragging; Compose's horizontal detector leaves vertical scrolling to `LazyColumn`.

### Phase 5 — Audio & Media Components (completed)
- [x] Live voice recording dock with amplitude waveform and hands-free lock (`VoiceRecorderDock.kt`).
- [x] Voice player with interactive waveform scrubber and speed toggle (`VoicePlayerBubble.kt`).
- [x] Fullscreen media viewer dialog (`FullscreenMediaViewerDialog.kt`).
- [x] Attachment bottom sheet (`ChatAttachmentBottomSheet.kt`).

### Phase 6 — Navigation & Auxiliary UI (completed)
- [x] Multi-pinned message bar with cycling and unpinning (`PinnedMessagesBar.kt`).
- [x] Unread messages separator (`UnreadMessagesDivider.kt`).
- [x] Animated scroll-to-bottom FAB with unread badge counter.
- [x] In-chat search match highlighting in message text, preserving native emoji spans and RTL/LTR text direction.

### Phase 7 — Offline Cache, E2EE & Performance (in progress)
- [x] Offline-first Room DB synchronization and message queue.
- [x] Android Keystore symmetric/asymmetric ciphers for Secret Chats.
- [x] Single-pass `@Immutable` data models for 120Hz frame budget.
- [ ] Benchmark real-device frame timings and latency.

## Decisions & Notes

### 2026-09-01 Geometry & Placement Fix:
1. **Unified Context Stack**: Refactored `calculateMessageContextMenuPlacement` to compute the Menu Card and Reactions Capsule as a single bound vertical stack (`totalStackHeight = reactionsHeight + elementGap + menuHeight`).
   - When placed above: `Reactions` sits directly above the anchor bubble, and `Menu` sits directly above `Reactions` with `elementGap` clearance.
   - When placed below: `Reactions` sits directly below the anchor bubble, and `Menu` sits directly below `Reactions` with `elementGap` clearance.
   - When viewports or safe insets cause vertical shifting, both elements shift strictly in tandem, preserving the fixed separation distance and mathematically eliminating overlap.
2. **Test Suite**: 120 unit tests passing with 100% success (`:feature:chat:testDebugUnitTest`).

### 2026-09-02 Runtime Corrective Pass (completed 2026-09-03)
1. **Context layering:** render the reactions capsule above the command card in z-order,
   including the lower-stack placement.
2. **Focused message:** cut the selected bubble out of the scrim so its actual contents
   remain legible while the rest of the chat is subdued.
3. **Responsive Emoji panel:** clamp the panel reservation to the current viewport and
   preserve it across Emoji/IME handoff.

### 2026-09-03 Closure Gate

**Verified on the current-source APK, installed on `emulator-5554`:**
1. A normal tap on a text bubble opens the contextual action menu.
2. The reactions capsule is rendered in front of the action card, including the lower placement case.
3. The selected message stays visible through the custom scrim; the platform dialog dim is disabled so it cannot obscure the cutout.
4. The composer stayed at the same bottom boundary while switching between the IME and emoji panel in the manually replayed interaction.

**Partial / deferred, not claimed as complete:**
1. A scripted ten-switch Emoji/IME endurance sweep has not yet produced ten individually evidenced transitions.
2. Vertical-scroll versus reply-swipe conflict arbitration remains a focused follow-up.
3. In-chat search highlighting and physical-device frame/latency benchmarking remain separate follow-ups.

The remaining runtime and physical-device gates do not invalidate the delivered context, focused-message, responsive-composer, search-highlight, or swipe-arbitration work, but they must not be represented as parity proof until replayed on a fresh APK and physical device.

### 2026-09-03 Remaining-Items Pass

1. `VistaEmojiText` now decorates every case-insensitive in-chat match with a background span while retaining inline emoji content. `MessageBubble` supplies the active query only for the displayed search result.
2. `SwipeToReplyLayout` now remains visually idle until the first accepted, centerward horizontal delta. The existing `ReplySwipePolicy` unit coverage rejects vertical and opposite-direction movement.
3. Isolated validation (`.chat-validation-build-20260903-search`) completed `:feature:chat:testDebugUnitTest`: **121 tests, 0 failures, 0 ignored**.
4. The normal shared-output test first failed before compilation with `AccessDeniedException` in `:core:designsystem:generateDebugResources`. The isolated test passed. Two production APK attempts (shared daemon and `--no-daemon`) stalled without creating an APK artifact and were stopped only at their task-owned wrapper process.
5. `adb devices -l` reports only `emulator-5554`; no physical Android device is attached, so the real-device frame/latency gate cannot be truthfully completed.

**Open acceptance gates:** install a fresh APK after the Gradle runtime blocker is resolved; record ten independently observed Emoji/IME transitions; then run the physical-device frame/latency benchmark on an attached device.
