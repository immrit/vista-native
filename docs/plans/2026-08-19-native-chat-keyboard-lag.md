# Native Chat Keyboard Lag — Telegram-Level Smoothness

**Type:** perf  
**Status:** done  
**Progress:** `[██████████] 100%`

## Problem / Goal

Native chat (`feature/chat`) keyboard open/close + scroll heavily lagged. Target was Telegram/Telegram X level smoothness.

## Root Causes Identified & Fixed

### 1. `imePadding()` on Scaffold moved to `bottomBar` Column
- **Problem:** `Scaffold(modifier = Modifier.imePadding())` caused the entire screen including `LazyColumn` to re-layout on every single keyboard animation frame (~60 frames).
- **Fix:** Moved `imePadding()` to only the `bottomBar` `Column`. `LazyColumn` now sits in stable window coordinates, completely immune to keyboard transition frames (same architecture as Flutter reference `KeyboardStableMediaQuery`).
- **File:** `feature/chat/src/main/java/ir/coffevista/vista_native/features/chat/presentation/ChatScreens.kt:1652, 1726`

### 2. Hoisted Composer blank/notBlank checks to `derivedStateOf`
- **Problem:** Every keystroke updated `composer` state, causing `AnimatedVisibility` (attach button) and `AnimatedContent` (mic vs send) to re-evaluate their animation transition specs on every character.
- **Fix:** Introduced `isComposerBlank by remember { derivedStateOf { composer.isBlank() } }` and `isComposerNotBlank`. Passed as stable booleans into `Composer`.
- **File:** `feature/chat/src/main/java/ir/coffevista/vista_native/features/chat/presentation/ChatScreens.kt:1371, 3262`

### 3. Added `contentType` to `LazyColumn` items
- **Problem:** Compose wasn't recycling item layouts efficiently during fast fling scrolling.
- **Fix:** Added `contentType = { _, message -> message.attachment?.kind?.name ?: "text" }` so text bubbles, image bubbles, voice bubbles reuse their own layout slots.
- **File:** `feature/chat/src/main/java/ir/coffevista/vista_native/features/chat/presentation/ChatScreens.kt:1772`

## Verification

- `./gradlew :feature:chat:testDebugUnitTest` — PASSED (100% unit tests green)
- `./gradlew :feature:chat:assembleDebug` — PASSED (clean module build)
- `./gradlew :app:assembleDebug` — PASSED (full app APK built)
