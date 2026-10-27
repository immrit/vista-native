# Profile Action Icons, Keyboard Dismissal, Feed & Search Overhaul

**Type:** bug | ui | perf | refactor
**Status:** done
**Progress:** `[██████████] 100%`

## Problem / Goal
1. **Profile Dark Theme Action Icons**: In dark theme, post action icons (`vista_post_comment`, `vista_post_send`) in profile screens appear pure black and invisible against the dark background.
2. **Keyboard Dismissal Across Screens**: When closing chat or navigating to any other screen (including conversations tab switch or back-navigation), the software keyboard remains on screen instead of closing immediately.
3. **Feed Screen & Algorithm Overhaul**: Feed recommendation algorithm needs full integration with backend (`POST /v1/feed/event`) for `like`, `save`, `open`, `comment`, `share`, and `view`, ensuring "برای شما" (AI-ranked `/v1/explore`) and "دنبال شده‌ها" (`/v1/feed/following`) run flawlessly with pull-to-refresh and offline caching.
4. **Search Screen Guillotine Audit & Overhaul**: Rigorously audit and overhaul Search screen (`SearchScreens.kt`, `SearchViewModel.kt`, `DefaultSearchRepository.kt`), adding proper workspace back button, wiring QR scanner click, fixing empty state and pull-to-refresh for trending hashtags, and ensuring flawless user/hashtag/post search results.

## Root cause / Design
- **Profile Icons**: In `ProfileParityComponents.kt:799` and `813`, `Image(painter = painterResource(DesignSystemR.drawable.vista_post_comment))` and `vista_post_send` lacked `colorFilter`. The drawables are black PNGs, making them invisible on dark surfaces. `ProfileEmptyView:1321` and `PostDetailScreen.kt:342, 412` had the same missing `colorFilter`. Fix: add `colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)` or `onSurfaceVariant`.
- **Keyboard Dismissal**:
  - `TelegramComposerField.kt:170`: `onDispose` did not hide the soft keyboard or clear `editTextRef` focus.
  - `ChatScreens.kt:1824`: `ChatScreenContent` lacked disposal keyboard cleanup, and `onBack` was called directly without hiding keyboard first.
  - `VistaShell.kt`: `navController` lacked a global destination change listener that unconditionally clears focus and hides keyboard on any navigation transition.
- **Feed Event Tracking**: `FeedApi.kt` has `trackFeedEvent(FeedEventRequestDto)` for `POST /v1/feed/event`, but it was never invoked in `OfflineFirstFeedRepository.kt` or `FeedViewModel.kt`. Wiring `trackFeedEvent` allows the backend recommendation ranker (`user_affinities` and `session_events` in `E:\vista-backend\internal\posts\repository.go:770`) to dynamically personalize the feed based on user interactions.
- **Search Screen**:
  - `SearchField` in `SearchWorkspaceContent` lacked a back button to exit workspace back to launcher.
  - `SmallIconButton` for QR scanner at `SearchScreens.kt:353` had an empty lambda `{ /* open scanner */ }`.
  - `LauncherContent` had no empty state when trending list was empty and lacked pull-to-refresh.
  - `SearchWorkspaceScreen` lacked QR scanner and back navigation callbacks.

## Phases

### Phase 1 — Investigate & Plan
- [x] Trace profile post action icons and locate missing `colorFilter` in `ProfileParityComponents.kt` and `PostDetailScreen.kt`
- [x] Trace keyboard lifecycle in `TelegramComposerField.kt`, `ChatScreens.kt`, and `VistaShell.kt`
- [x] Review backend feed endpoints (`/v1/feed`, `/v1/feed/following`, `/v1/feed/event`, `/v1/explore`) in `E:\vista-backend`
- [x] Audit Search screen flow in `SearchScreens.kt`, `SearchViewModel.kt`, and `DefaultSearchRepository.kt`

### Phase 2 — Profile & Keyboard Implementations
- [x] Fix action icons tint in `ProfileParityComponents.kt` (`vista_post_comment`, `vista_post_send`, and `ProfileEmptyView`)
- [x] Fix action icons tint in `PostDetailScreen.kt` (`vista_post_comment`)
- [x] Fix keyboard dismissal in `TelegramComposerField.kt` `onDispose`
- [x] Fix keyboard dismissal on back and screen exit in `ChatScreens.kt`
- [x] Add global keyboard dismissal and focus clear on navigation in `VistaShell.kt`

### Phase 3 — Feed & Search Implementations
- [x] Integrate feed event tracking (`POST /v1/feed/event`) in `OfflineFirstFeedRepository.kt` and `FeedViewModel.kt`
- [x] Overhaul `SearchScreens.kt`: add back button to `SearchField` in workspace mode, wire QR scanner button, add empty state and pull-to-refresh in `LauncherContent`
- [x] Update `AppNavGraph.kt` and `ShellFeatureContent.kt` if needed for search back and scanner navigation

### Phase 4 — Verify & Build Release
- [x] Run unit tests: `:feature:profile:testDebugUnitTest`, `:feature:feed:testDebugUnitTest`, `:feature:search:testDebugUnitTest`, `:feature:chat:testDebugUnitTest`
- [x] Build production release APK: `./gradlew.bat assembleProductionRelease`
- [x] Deploy and verify on emulator `emulator-5554`: check profile action icons in dark mode, test keyboard closing when leaving chat, test search flow and feed tabs
- [x] Copy final release APK to `E:\vista_release` and update `SHA256SUMS.txt`

## Decisions & Notes
- Preserving strict read-only status for `E:\vista` (Flutter repo).
- Using `MaterialTheme.colorScheme.onBackground` for action icons so they automatically adapt to light mode (black) and dark mode (white/light grey).
- Making feed event tracking asynchronous and resilient (`runCatching`) so offline or transient network drops never block the UI or fail post interactions.
