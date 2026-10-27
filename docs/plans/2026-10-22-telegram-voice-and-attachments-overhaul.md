# Telegram-Style Voice Recording & Attachment Architecture Overhaul

**Type:** feature | bug | ui
**Status:** done
**Progress:** `[██████████] 100%`

## Problem / Goal
Voice recording and sending was broken and lacked Telegram's fluid UX:
1. `ChatScreens.kt` conditionally swapped the `VoiceRecorderDock` between two branches on `isVoiceHolding = true`, causing the active dock to unmount mid-gesture, disposing the `MediaRecorder`, cancelling the touch pointer session, and killing the recording.
2. `OfflineFirstChatRepository.kt` deleted the recorded voice file from disk on send and set `localUri = null`, preventing the sender from playing back their own sent voice messages.
3. Attachment sheet lacked Telegram's polished visual hierarchy and comprehensive media types (Gallery, File, Camera, Audio/Music).
Goal: Implement an exact Telegram-grade UI/UX for voice recording (hold to record, slide up to lock, slide left to cancel, real-time animated waveform, haptic feedback, locked controls) and Telegram-grade media/file attachment sheet.

## Root cause / Design
1. **Voice Recorder Lifecycle Bug**: `ChatScreens.kt:5722` had `if (isVoiceHolding) VoiceRecorderDock(...) else AnimatedContent { VoiceRecorderDock(...) }`. When `onHoldingStateChange(true)` fired, the composable unmounted, destroying internal state.
   - *Fix*: Keep a single persistent `VoiceRecorderDock` embedded into the composer layout, smoothly expanding over the input field without remounting.
2. **Ephemeral Voice Deletion**: `OfflineFirstChatRepository.kt:1858-1872` explicitly purged `/chat-voice/` files on send.
   - *Fix*: Retain `localUri` on the outgoing message attachment so `VoicePlayerBubble` plays instantly from local cache.
3. **Telegram UX Parity**:
   - Tuned drag thresholds (`-65dp` for lock, `-65dp` for cancel).
   - Dynamic real-time sound amplitude waveform.
   - Haptic feedback on start, lock, cancel, and send.
   - Telegram-style attachment sheet with vibrant gradient badges (Gallery, File, Camera, Audio/Music).

## Phases

### Phase 1 — Investigate & Architecture Design
- [x] Reproduce and locate voice recorder remount bug in `ChatScreens.kt` and `VoiceRecorderDock.kt`
- [x] Trace voice upload and local cache deletion in `OfflineFirstChatRepository.kt`
- [x] Review Telegram attachment sheet parity requirements

### Phase 2 — Implement
- [x] Fix voice cache retention in `OfflineFirstChatRepository.kt` (do not delete local voice file on send)
- [x] Overhaul `VoiceRecorderDock.kt` with Telegram-exact UI/UX (lock pill, sliding chevron, animated waveforms, locked bar)
- [x] Restructure `Composer` in `ChatScreens.kt` to prevent any remounting of `VoiceRecorderDock` during recording
- [x] Overhaul `ChatAttachmentBottomSheet.kt` with Telegram-style cards, icons, and audio picker support
- [x] Ensure smooth keyboard & composer transitions during voice recording and attachment selection

### Phase 3 — Verify
- [x] Run `:feature:chat:testDebugUnitTest` and compile cleanly
- [x] Install productionDebug APK on emulator
- [x] Manual check: Hold mic to record, verify live waveform and timer
- [x] Manual check: Slide left to cancel, verify clean deletion and haptics
- [x] Manual check: Slide up to lock, release finger, verify locked dock with delete and send buttons
- [x] Manual check: Send voice, verify it appears in chat and plays smoothly in `VoicePlayerBubble`
- [x] Manual check: Open attachment sheet, verify Telegram design and file/media selection

## Decisions & Notes
- Keep `LayoutDirection.Ltr` on the outer composer row to match Telegram's standard layout (input on start, mic/send on end), with `dragOffsetX < -65dp` for sliding toward the left.
- Audio recording format: `MediaRecorder.OutputFormat.MPEG_4` with `MediaRecorder.AudioEncoder.AAC` at 96kbps / 44.1kHz.
- Verified on `emulator-5554`: Recording, lock gesture, cancel gesture, sending, playback, and attachment sheet all work smoothly with 0 errors.
