# Add Post & Media Trimming Crash Investigation & Hardening

**Type:** bug | perf | refactor
**Status:** in-progress
**Progress:** `[██░░░░░░░░] 20%`

## Problem / Goal
Deep scrutiny of the Add Post flow, Music Trimmer, Video Trimmer, and Media Selection components to uncover and fix any edge cases, unhandled exceptions, lifecycle leaks, and memory issues that could cause runtime crashes during user interactions.

## Root cause / Design
Detailed inspection revealed 6 distinct crash vectors:
1. **ExoPlayer Lifecycle Race (`IllegalStateException: Player is released`)**: In `VideoTrimmerScreen`, `MusicTrimSheet`, and `VideoPreviewCard`, coroutines (`LaunchedEffect` loops with delays) and UI interactions (`clickable`) called `player.currentPosition`, `player.seekTo`, and `player.isPlaying` after the player had been released in `DisposableEffect.onDispose`.
2. **PlayerView Teardown & Recomposition Leak**: `AndroidView` holding `PlayerView` only assigned the player in `factory`. When `videoUri` changed or was disposed, `PlayerView` retained a released player reference without detaching (`playerView.player = null` on release).
3. **Out-Of-Memory (OOM) on High-Resolution Video Thumbnails**: `VideoTrimEngine.loadVideoThumbnails` decoded up to 40 full-resolution (4K/1080p) frames into Java heap with `retriever.getFrameAtTime()` and only caught `Exception` (which DOES NOT catch `OutOfMemoryError`). On API 27+, `retriever.getScaledFrameAtTime` must be used, and all `Throwable` errors must be caught.
4. **MediaMuxer Non-Monotonic PTS & Missing Advance**: In `VideoTrimEngine.trimVideo`, variable frame rate or B-frame videos could write non-monotonic PTS causing native `MediaMuxer` crash (`timestampUs < previousTimestampUs`). Also `extractor.advance()` was skipped when reaching `timeUs > endUs`, causing potential infinite loops or native stalls.
5. **URLDecoder Crash on Special Characters**: In `VistaShell.kt`, `URLDecoder.decode(rawUri, "UTF-8")` was called without `try-catch` on navigation arguments that could contain bare `%` or special characters from Persian media file names.
6. **ActivityNotFoundException on System Pickers / Camera**: `launchCamera` and visual media pickers lacked `try-catch` / `runCatching`, which crashes if device has no camera app or photo picker provider fails.

## Phases

### Phase 1 — Investigate
- [x] Identify critical risk areas across the 8 affected files
- [x] Inspect `VideoTrimEngine.kt` for extraction/muxing/OOM bugs
- [x] Inspect `VideoTrimmerScreen.kt` for ExoPlayer/Compose lifecycle crashes
- [x] Inspect `MusicTrimSheet.kt` for MediaPlayer crash vectors
- [x] Inspect `AddPostViewModel.kt` and `AddPostScreen.kt` for navigation & state crashes

### Phase 2 — Implement Fixes
- [ ] Hardened error handling & graceful fallbacks in `VideoTrimEngine`
- [ ] Safe MediaPlayer lifecycle & state machine in `MusicTrimSheet`
- [ ] Safe ExoPlayer cleanup & lifecycle observer in `VideoTrimmerScreen`
- [ ] Safe ExoPlayer lifecycle & PlayerView detachment in `VideoPreviewCard` (`AddPostComponents.kt`)
- [ ] Safe navigation URL decoding in `VistaShell.kt`
- [ ] Robust content Uri, camera exception handling, and music trim sheet fallback in `AddPostScreen` / `AddPostViewModel`

### Phase 3 — Verify
- [ ] Unit tests pass
- [ ] Gradle compile & build verification (`:app:compileProductionDebugKotlin`)
- [ ] APK packaging verification


## Decisions & Notes
- Initial audit plan created.
