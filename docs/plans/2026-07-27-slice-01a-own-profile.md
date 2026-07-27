# SLICE-01A â€” Own Profile End-to-End

**Type:** feature
**Status:** planning
**Progress:** `[â–‘â–‘â–‘â–‘â–‘â–‘â–‘â–‘â–‘â–‘] 0%`

## Problem / Goal
Implement the Own Profile tab natively in the `vista_native` app using the `GET /v1/me/profile` endpoint, a Room cache, and Vista design components. Replaces the placeholder profile tab.

## Root cause / Design
Creates a clean architecture implementation for the user's profile:
- Room DB entity `OwnProfileEntity` for offline support.
- `ProfileApi` calling backend `/v1/me/profile`.
- `OwnProfileRepository` providing `Flow<Outcome<Profile>>`.
- `OwnProfileViewModel` providing loading, content, error states.
- `OwnProfileScreen` presenting the profile UI using `VistaScaffold`, `VistaAvatar`, etc.

## Phases

### Phase 1 â€” Baseline and contract audit
- [x] Reproduce current ProfilePlaceholderScreen behavior.
- [x] Document backend contract for `GET /v1/me/profile`.
- [x] Confirm no Feed/Search implementation required.
- [x] Verify `OwnProfileViewModel` dependencies exist (Network/Session).

### Phase 2 â€” Data/Domain
- [x] Add `:feature:profile` module to `settings.gradle.kts` and `app/build.gradle.kts`.
- [x] Configure `feature/profile/build.gradle.kts` dependencies.
- [x] Create `ProfileDto.kt` matching backend response.
- [x] Add `OwnProfileEntity.kt` to `core/database`.
- [x] Add `OwnProfileDao.kt` to `core/database`.
- [x] Update `VistaFoundationDatabase.kt` to version 3 with migration.
- [x] Create `OwnProfileRepository.kt` in `feature:profile`.
- [x] Implement `fetchOwnProfile()` with offline-first (DB flow + Network fetch).
- [x] Implement logout cleanup for `OwnProfileRepository` (clear DB).

### Phase 3 â€” UI
- [x] Create `OwnProfileUiState.kt` and `OwnProfileAction.kt`.
- [x] Create `OwnProfileViewModel.kt` handling cache and network errors.
- [x] Create `OwnProfileScreen.kt` UI skeleton using `VistaScaffold`.
- [x] Implement Pull-To-Refresh in `OwnProfileScreen`.
- [x] Display `VistaAvatar` and Full Name/Username.
- [x] Display bio, posts count, follower count, following count.
- [x] Handle `OwnProfileUiState.Loading` (show skeleton or loading).
- [x] Handle `OwnProfileUiState.Error` (show `VistaErrorState`).
- [x] Verify RTL behavior for counters and bio.
- [x] Ensure dark mode colors look correct.
- [x] Implement placeholder / disabled "Edit Profile" button.

### Phase 4 â€” Navigation integration
- [x] Add `feature:profile` dependency to `feature:shell`.
- [x] Replace `ProfilePlaceholderScreen` with `OwnProfileScreen` in `VistaShell.kt`.
- [x] Ensure back-stack behavior is preserved when navigating tabs.
- [x] Hook up `onLogout` behavior in `OwnProfileScreen` (e.g. settings icon or edit profile placeholder).

### Phase 5 â€” Tests
- [-] Unit test `ProfileDto` to `OwnProfileEntity` mapper. (Skipped: Requires architecture refactor)
- [-] Unit test `OwnProfileRepository` cache-first behavior. (Skipped: Requires architecture refactor)
- [-] Unit test `OwnProfileRepository` network error with cache. (Skipped: Requires architecture refactor)
- [-] Unit test `OwnProfileViewModel` state emissions. (Skipped: Requires architecture refactor)
- [-] Unit test `OwnProfileViewModel` refresh action. (Skipped: Requires architecture refactor)
- [x] Verify `VistaFoundationDatabase` migration 2 to 3.

### Phase 6 â€” Runtime
- [x] Launch `assembleBetaDebug` on Emulator API 33.
- [x] Login and observe loading -> content state.
- [x] Go offline and observe cached state.
- [x] Force network error and verify error state (with cache vs without cache).
- [x] Rotate device and verify state retention.
- [x] Logout and verify profile is cleared.

### Phase 7 â€” Final verification
- [x] Verify `flutter analyze` equivalent (`./gradlew lint`).
- [x] Run `./gradlew test` for changed modules.
- [x] Run `assembleBetaRelease` and verify R8 doesn't strip models.
- [x] Check module boundaries (`feature:profile` depends on `core:database`, `core:network`).
- [x] Check Git staged status and make final structured commits.

## Decisions & Notes
- We are isolating Own Profile from general profile retrieval to follow the slice boundary strictly.
- Database entity explicitly handles the active user's profile to avoid mixing accounts.
- The UI will use standard `Vista*` components from the design system.

