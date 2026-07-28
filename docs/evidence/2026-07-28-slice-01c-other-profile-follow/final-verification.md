# SLICE-01C Final Verification

**Date:** 2026-07-28
**Branch:** `slice-01c/other-profile-follow`
**Baseline:** `d6cde1bf50fcbf3f31f64c9aeeea23b73230d3fe`
**Status:** `SLICE-01C Complete — Other User Profile + Follow Vertical Slice Passed`

## Runtime Recovery

The API 33 device initially had only the older Flutter/production package
`ir.coffevista.vista` version `2.6.2`; Native beta
`ir.coffevista.vista_native` was absent. No Native startup, Hilt, Room,
serialization, session, fixture, or navigation crash was reproduced after installing a
fresh APK. This was a deployment/package mismatch, so no artificial runtime source fix
or `fix(runtime)` commit was created.

Independent evidence:
`docs/evidence/2026-07-28-runtime-recovery/final-verification.md`.

Gate:
`Runtime Recovery Passed — Feature Development Unblocked`.

## Contract

The read-only Backend and Flutter audit is recorded in `contract-audit.md`.

- Public profile: optional-auth `GET /v1/profiles/{user_id}`; top-level profile object.
- Follow: authenticated `POST /v1/me/follow`,
  `{"target_user_id":"<UUID>"}`; canonical `following` or `requested`.
- Unfollow: authenticated `POST /v1/me/unfollow`, same payload; canonical
  `unfollowed`.
- Backend-supported relationship states persisted by Native: `none`, `following`,
  `requested`; self redirects to Own Profile and blocked targets are unavailable.

## Implementation Inventory

- Room v5: `PublicProfileEntity`, `PublicProfileDao`, exported schema `5.json`, and
  registered `MIGRATION_4_5`.
- Cache key: composite `viewer_account_id + profile_user_id`.
- Layers: separate Retrofit DTOs, domain model, Room entity, repository contract,
  repository implementation, UI state, ViewModel, and Compose screen.
- Repository: cache-first observation, explicit refresh, cached stale fallback, viewer
  isolation, logout clear, one in-flight mutation, optimistic state/count update,
  success reconciliation, failure rollback, and canonical refresh.
- State machine: `NotFollowing → pending follow → Following|Requested`, and
  `Following|Requested → pending unfollow → NotFollowing`; failure restores the complete
  prior entity and emits a snackbar effect. `Unavailable` sends no mutation.
- Navigation: Feed author avatar/name/username opens
  `other_profile/{userId}`; self redirects to Own Profile; Back restores Feed.
- Debug fixture: public-profile/follow fixture exists only in `app/src/debug`.

## Automated Tests

Commands used `"-Pkotlin.incremental=false"` and isolated output roots.

| Report set | Total | Passed | Failed | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| Auth unit | 28 | 28 | 0 | 0 | 0 |
| Feed unit | 24 | 24 | 0 | 0 | 0 |
| Profile unit/mapper/repository/ViewModel | 35 | 35 | 0 | 0 | 0 |
| Shell unit | 3 | 3 | 0 | 0 | 0 |
| Room DAO/migration device tests | 17 | 17 | 0 | 0 | 0 |
| Feed module instrumentation | 6 | 6 | 0 | 0 | 0 |
| App Auth/Shell/Profile/Feed instrumentation | 21 | 21 | 0 | 0 | 0 |
| **Unique total** | **134** | **134** | **0** | **0** | **0** |

Report roots:

- `.gradle/slice01c-final-unit-build/**/test-results/testDebugUnitTest/TEST-*.xml`
- `.gradle/slice01c-db-final-build/core/database/outputs/androidTest-results/connected/debug/TEST-*.xml`
- `.gradle/slice01c-feed-device-final2-build/feature/feed/outputs/androidTest-results/connected/debug/TEST-*.xml`
- `.gradle/slice01c-app-final-build/app/outputs/androidTest-results/connected/debug/flavors/beta/TEST-*.xml`

The 21-test app suite includes Other Profile navigation, Follow, Unfollow, controlled
failure rollback, refresh, offline cache, rotation/recreation, Back, tab switching,
self redirect, and logout, plus Auth/Shell/Own Profile/Feed regressions.

## Runtime API 33

Device: `Medium_Phone_2`, serial `emulator-5556`, Android API 33.

Upgrade migration:

1. Installed the SLICE-01B APK from
   `.gradle/slice01b-build-lint-r8-evidence4/app/outputs/apk/beta/debug/app-beta-debug.apk`.
2. Cleared app data, cold-launched `valid-session`, and confirmed
   `vista_foundation.db` was created by v4.
3. Installed the final v5 APK with `adb install -r`.
4. Cold-launched the same session; Feed and fixture author were visible.
5. The database remained intact and grew from the migrated schema; logcat had zero
   migration-verification, SQLite, crash, or ANR matches.

Final clean-install smoke:

- Uninstalled the Native target package and installed the final betaDebug APK.
- Launcher start without fixture reached Onboarding/Auth.
- Direct cold start returned `Status: ok`, `LaunchState: COLD`.
- Warm start delivered the intent to the running `MainActivity`.
- Valid session showed Feed and the expected author; process remained alive.
- Manual Feed author tap showed Other Profile title, user identity, and Follow action;
  system Back returned to Feed.
- The 21-test device suite verified mutation success/rollback, offline cache,
  recreation, tab restoration, self redirect, and logout.
- Final logcat scan for `FATAL EXCEPTION`, app `ANR`, `SQLiteException`, Room migration
  verification, `IllegalStateException`, `IllegalArgumentException`, Hilt,
  `NoSuchMethodError`, `VerifyError`, and `SerializationException`: **0 matches**.

## Build, Lint, R8, and Static Gates

Final command:

`gradle.bat :app:assembleBetaDebug :app:assembleBetaRelease :app:lintBetaDebug :core:database:lintDebug :core:designsystem:lintDebug :feature:profile:lintDebug :feature:feed:lintDebug :feature:shell:lintDebug -Pvista.isolatedBuildDir=true -Pvista.isolatedBuildOutput=.gradle/slice01c-final-build-escalated "-Pkotlin.incremental=false" --console=plain --stacktrace`

Result: `BUILD SUCCESSFUL in 6m 46s`, 929 actionable tasks.

- `assembleBetaDebug`: pass; APK size 16,794,708 bytes.
- `assembleBetaRelease`: pass; unsigned APK size 5,414,001 bytes.
- R8: `minifyBetaReleaseWithR8` pass.
- Mapping artifacts: `mapping.txt` 38,449,254 bytes, `seeds.txt` 162,765 bytes,
  `usage.txt` 4,154,542 bytes, and `configuration.txt` 39,311 bytes.
- Changed-module lint: zero errors. Profile, Feed, Database, and Design System have zero
  issues; Shell has two `AutoboxingStateCreation` hints. App aggregate has 36 existing
  dependency/tool-version warnings and zero errors.
- Fixture leakage: `apkanalyzer dex packages` found debug fixture symbols in Debug and
  **0** `DebugPublicProfileApiFixture`/`DebugFeedApiFixture` symbols in Release.
- Secret scan: 328 repository source/evidence files scanned; **0 findings**.
- Module boundary: Profile has zero imports from Feed/Shell; changed core modules have
  zero feature imports.
- `git diff --check`: pass with no whitespace errors.

Build reports and artifacts:

- `.gradle/.gradle/slice01c-final-build-escalated/app/reports/lint-results-betaDebug.xml`
- `.gradle/.gradle/slice01c-final-build-escalated/{core,feature}/**/reports/lint-results-debug.xml`
- `.gradle/.gradle/slice01c-final-build-escalated/app/outputs/mapping/betaRelease/`
- `.gradle/slice01c-final-build-escalated.stdout.log`

## Scope Lock

Followers list, Following list, Chat, Block/Unblock, Story, Like, Comment, Save, Share,
Composer, Upload, Search, Notifications, and a fake post grid were not implemented.
Backend and Flutter remained read-only.

## Git Closure

Permitted commits:

- `7048291 feat(profile): add other user profile and follow state`
- `2968ea5 test(profile): verify follow rollback cache and navigation`
- `docs(profile): record slice-01c verification`

The documentation commit contains this file, so its own hash and the final clean-tree
result are reported in the final handoff after the commit is created.
