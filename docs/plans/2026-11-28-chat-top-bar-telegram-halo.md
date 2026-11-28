# Telegram-Style Top App Bar Halo for Chat & Chat Details

**Type:** ui
**Status:** done
**Progress:** `[██████████] 100%`

## Problem / Goal
Both the chat conversation screen (`MessageDetailScreen`) and chat/group details screens (`VistaNotchMetaballHeader`) have floating glass header buttons with an empty/bare area behind the status bar and top app bar, lacking the smooth Telegram-style frosted top halo that starts dense from the top edge (`y = 0`), wraps the app bar pills, and feathers smoothly down into the scrolling content below.

## Root cause / Design
1. `VistaGlass.kt` (`vistaGlassHalo` when `fromTop = true`) drops alpha too aggressively (`0.56` at 30%, `0.26` at 50%, `0.02` at 84%), and callers (`ChatScreens.kt:3075` and `VistaNotchMetaballHeader.kt:148`) pass low `maxAlpha` (`0.36f–0.46f`) and constrain the halo with `.matchParentSize()`, cutting it off right at the bottom edge of the pills with zero downward feather skirt.
2. `VistaNotchMetaballHeader.kt:148` also gates the halo behind `collapseProgress > 0.15f`, leaving the top of Chat Details and Group Details completely bare before scrolling, and uses `isSystemInDarkTheme()` instead of `MaterialTheme.colorScheme.background.luminance() < 0.5f`.
3. **Fix:**
   - Calibrate `vistaGlassHalo(fromTop = true)` in `VistaGlass.kt` with a Telegram-grade S-curve: dense across the status bar and floating pill region (`1.00 -> 0.94 -> 0.82` up to ~48%), then smooth feathering (`0.56 -> 0.28 -> 0.10 -> 0.02 -> 0.00`) down into the content below, with matched fallback gradient alpha for non-blur devices.
   - In `ChatScreens.kt` (`MessageDetailScreen`), measure `headerHeightDp` on the interactive header `Column` (keeping `effectiveTopPadding` unchanged) and extend the `vistaGlassHalo` underlay `30.dp` below `headerHeightDp` with higher calibrated opacity (`0.68f` light / `0.72f` dark) and `20.dp` blur radius.
   - In `VistaNotchMetaballHeader.kt`, always render the top halo (`statusBarTop + 90.dp`, covering status bar + top pills + `~32.dp` feather skirt below the collapsed bar) behind the avatar/title/pills, and resolve `isDark` from `MaterialTheme.colorScheme.background.luminance() < 0.5f`.

## Phases

### Phase 1 — Investigate
- [x] Inspect `VistaGlass.kt` (`vistaGlassHalo`), `ChatScreens.kt` (`MessageDetailScreen`), `VistaNotchMetaballHeader.kt`, `GroupDetailsScreen.kt`
- [x] Verify emulator `emulator-5556` availability and debug chat fixtures

### Phase 2 — Implement
- [x] Update `vistaGlassHalo(fromTop = true)` gradient curve and fallback opacity in `VistaGlass.kt`
- [x] Update `MessageDetailScreen` top halo overhang & sizing in `ChatScreens.kt`
- [x] Update `VistaNotchMetaballHeader.kt` to always render the Telegram top halo with downward feathering and in-app theme detection

### Phase 3 — Verify
- [x] Compile & run unit tests (`:core:designsystem:testDebugUnitTest`, `:feature:chat:testDebugUnitTest`)
- [x] Install `:app:installBetaDirectDebug` on `emulator-5554`, open Chat & Chat Details screens, and capture screenshots to verify visually in light/dark + RTL
