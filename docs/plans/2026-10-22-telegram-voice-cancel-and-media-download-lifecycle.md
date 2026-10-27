# Telegram Voice Cancel & Media Download Lifecycle Architecture

**Type:** feature | bug | ui
**Status:** complete
**Progress:** `[██████████] 100%`

## Problem / Goal
1. **Voice Slide-to-Cancel Bug**:
   - Swiping to cancel during voice recording (`HOLDING`) did NOT cancel; it sent the voice message to the recipient.
   - Root cause: `val isCanceling = dragOffsetX < -cancelThresholdPx` was evaluated once at Composable body level and captured by value (`false`) inside `pointerInput { awaitEachGesture { ... } }`. Inside the gesture loop, `if (isCanceling)` was perpetually `false`.
2. **Voice Recording Visual Polish**:
   - Upgraded to Telegram-grade visuals:
   - Floating lock pill with smooth vertical translation, glowing lock icon, and circular progress.
   - Monospace bold recording timer (`00:00`), glowing red recording pulse, live 22-bar waveform, and animated slide chevron (`‹ ‹ برای لغو به چپ بکشید`).
   - Real-time haptic feedback on entering cancel threshold.
3. **Media Download Lifecycle & UI/UX**:
   - Previously, incoming photos loaded automatically over remote URL while an ugly external `TextButton("دانلود")` sat beneath images and documents.
   - Implemented Telegram's exact engineered download lifecycle and UI/UX:
     - **Photos/Videos**: Dim preview with dark scrim overlay + centered translucent circular download button (`↓` + file size badge). When downloading: smooth circular progress spinner + `✕` cancel button. When downloaded: full clear image without overlay.
     - **Voice Messages**: The download button is built directly into the circular action button of `VoicePlayerBubble` (down arrow `↓` -> spinner with `✕` -> Play `▶` button). No awkward external text button!
     - **Documents/Files**: Integrated circular button (down arrow `↓` + file size -> spinner + `✕` -> document icon). No awkward external text button!
     - Completely removed the detached `if (needsDownload) { TextButton("دانلود") }` block.

## Phases

### Phase 1 — Investigate & Design
- [x] Identify closure capture bug in `VoiceRecorderDock.kt` where `isCanceling` was frozen at `false`
- [x] Trace download button and media loading in `ChatScreens.kt:AttachmentBubble` and `VoicePlayerBubble.kt`
- [x] Design Telegram-exact circular download overlays and state transitions

### Phase 2 — Implement
- [x] Fix gesture cancel evaluation in `VoiceRecorderDock.kt` (dynamic calculation inside pointer loop + immediate cancel threshold trigger)
- [x] Upgrade `VoiceRecorderDock.kt` visuals (Telegram glassmorphic bar, pulsing red dot, animated cancel chevron, glowing floating lock pill)
- [x] Upgrade `VoicePlayerBubble.kt` to integrate download state into its circular action button (download `↓` -> progress spinner `✕` -> Play `▶`)
- [x] Overhaul `AttachmentBubble` in `ChatScreens.kt`:
  - Center-overlay Telegram download button for photos/videos with file size badge
  - Streamlined document row with integrated download icon/progress
  - Remove all raw external `TextButton("دانلود")` artifacts
- [x] Upgrade `ChatAttachmentBottomSheet.kt` with 6 Telegram gradient action tiles
- [x] Support HTTP and HTTPS in `ChatMediaDownloader.kt` for local and dev testing

### Phase 3 — Verify
- [x] Run `:feature:chat:testDebugUnitTest` to ensure 100% clean test pass
- [x] Install productionDebug APK on `emulator-5554`
- [x] Live test: Swipe left to cancel voice recording, verify it cancels and does NOT send
- [x] Live test: View media/files/voice download states, verify Telegram-exact UX
- [x] Confirm clean chat screen with zero stray buttons or error overlays

## Decisions & Notes
- For un-downloaded photos: Render a dimmed preview with `drawWithContent` dark scrim (`alpha = 0.45f`) and centered 54dp dark translucent circle (`Icons.Default.ArrowDownward` + size badge). Tapping downloads or cancels; tapping once downloaded opens full-screen viewer.
- `VoicePlayerBubble`: Integrates `downloadTask`, `isDownloaded`, `onStartDownload`, `onCancelDownload`. Circular button dynamically switches between download arrow, progress spinner, and play/pause.
- `AttachmentBubble`: Replaced awkward detached text download button with native media-embedded controls matching Telegram.
