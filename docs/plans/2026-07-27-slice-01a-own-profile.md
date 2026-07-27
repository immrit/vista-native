# SLICE-01A — Own Profile End-to-End

**Type:** feature
**Status:** planning
**Progress:** `[░░░░░░░░░░] 0%`

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

### Phase 1 — Baseline and contract audit
- [ ] Reproduce current ProfilePlaceholderScreen behavior.
- [ ] Document backend contract for `GET /v1/me/profile`.
- [ ] Confirm no Feed/Search implementation required.
- [ ] Verify `OwnProfileViewModel` dependencies exist (Network/Session).

### Phase 2 — Data/Domain
- [ ] Add `:feature:profile` module to `settings.gradle.kts` and `app/build.gradle.kts`.
- [ ] Configure `feature/profile/build.gradle.kts` dependencies.
- [ ] Create `ProfileDto.kt` matching backend response.
- [ ] Add `OwnProfileEntity.kt` to `core/database`.
- [ ] Add `OwnProfileDao.kt` to `core/database`.
- [ ] Update `VistaFoundationDatabase.kt` to version 3 with migration.
- [ ] Create `OwnProfileRepository.kt` in `feature:profile`.
- [ ] Implement `fetchOwnProfile()` with offline-first (DB flow + Network fetch).
- [ ] Implement logout cleanup for `OwnProfileRepository` (clear DB).

### Phase 3 — UI
- [ ] Create `OwnProfileUiState.kt` and `OwnProfileAction.kt`.
- [ ] Create `OwnProfileViewModel.kt` handling cache and network errors.
- [ ] Create `OwnProfileScreen.kt` UI skeleton using `VistaScaffold`.
- [ ] Implement Pull-To-Refresh in `OwnProfileScreen`.
- [ ] Display `VistaAvatar` and Full Name/Username.
- [ ] Display bio, posts count, follower count, following count.
- [ ] Handle `OwnProfileUiState.Loading` (show skeleton or loading).
- [ ] Handle `OwnProfileUiState.Error` (show `VistaErrorState`).
- [ ] Verify RTL behavior for counters and bio.
- [ ] Ensure dark mode colors look correct.
- [ ] Implement placeholder / disabled "Edit Profile" button.

### Phase 4 — Navigation integration
- [ ] Add `feature:profile` dependency to `feature:shell`.
- [ ] Replace `ProfilePlaceholderScreen` with `OwnProfileScreen` in `VistaShell.kt`.
- [ ] Ensure back-stack behavior is preserved when navigating tabs.
- [ ] Hook up `onLogout` behavior in `OwnProfileScreen` (e.g. settings icon or edit profile placeholder).

### Phase 5 — Tests
- [ ] Unit test `ProfileDto` to `OwnProfileEntity` mapper.
- [ ] Unit test `OwnProfileRepository` cache-first behavior.
- [ ] Unit test `OwnProfileRepository` network error with cache.
- [ ] Unit test `OwnProfileViewModel` state emissions.
- [ ] Unit test `OwnProfileViewModel` refresh action.
- [ ] Verify `VistaFoundationDatabase` migration 2 to 3.

### Phase 6 — Runtime
- [ ] Launch `assembleBetaDebug` on Emulator API 33.
- [ ] Login and observe loading -> content state.
- [ ] Go offline and observe cached state.
- [ ] Force network error and verify error state (with cache vs without cache).
- [ ] Rotate device and verify state retention.
- [ ] Logout and verify profile is cleared.

### Phase 7 — Final verification
- [ ] Verify `flutter analyze` equivalent (`./gradlew lint`).
- [ ] Run `./gradlew test` for changed modules.
- [ ] Run `assembleBetaRelease` and verify R8 doesn't strip models.
- [ ] Check module boundaries (`feature:profile` depends on `core:database`, `core:network`).
- [ ] Check Git staged status and make final structured commits.

## Decisions & Notes
- We are isolating Own Profile from general profile retrieval to follow the slice boundary strictly.
- Database entity explicitly handles the active user's profile to avoid mixing accounts.
- The UI will use standard `Vista*` components from the design system.
