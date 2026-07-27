# Phase 10 Evidence — Startup, Onboarding and Auth Regression Shield

## Read-only parity trace

Flutter startup/auth was retraced from `E:\vista` without modification. The comparison and retained
transition contract are recorded in `docs/architecture/FND-01-STARTUP-AUTH-FIXTURES.md`.

## Implementation evidence

- release-safe extension point: `feature/auth/.../startup/StartupFixture.kt`
- debug-only implementation: `app/src/debug/.../debug/DebugStartupFixture.kt`
- production consumer: `StartupResolver`
- fixture configuration before UI/startup: `MainActivity`
- release graph: empty fixture set through `@Multibinds`
- scenarios cover maintenance enabled/disabled, valid, refresh-success, refresh-expired, revoked,
  malformed and offline session/no-session

`maintenance-disabled` resolves directly to the signed-out Auth boundary; it cannot accidentally
call the live maintenance endpoint.

## Unit checkpoint

Command:

`gradlew :feature:auth:testDebugUnitTest -Pvista.isolatedBuildDir=true -Pvista.isolatedBuildOutput=fnd-build-output-fnd48 -Pkotlin.incremental=false`

Result:

- `BUILD SUCCESSFUL`
- 5 suites / 27 tests
- 0 failure / 0 error / 0 skipped
- Startup valid/expired/revoked/malformed/offline/missing-refresh and fixture short-circuit covered
- Auth lookup/password/OTP/2FA/password-required covered
- onboarding completion/version behavior covered

## Release separation checkpoint

Command:

`gradlew :app:compileBetaDebugKotlin :app:compileProductionReleaseKotlin -Pvista.isolatedBuildDir=true -Pvista.isolatedBuildOutput=fnd-build-output-fnd49 -Pkotlin.incremental=false`

Result:

- `BUILD SUCCESSFUL`
- 241 tasks
- debug compilation artifacts contain the debug fixture implementation
- productionRelease compilation artifacts contain zero `DebugStartupFixture` entries

## Instrumentation checkpoint

Final command:

`gradlew :app:connectedBetaDebugAndroidTest -Pvista.isolatedBuildDir=true -Pvista.isolatedBuildOutput=fnd-build-output-fnd51b -Pkotlin.incremental=false`

Device/result:

- `Medium_Phone_2(AVD) - API 33`
- 10 tests / 0 failed / 0 skipped
- `BUILD SUCCESSFUL` in 2m 1s
- 223 actionable tasks: 219 executed / 4 up-to-date
- maintenance enabled and disabled, malformed session, offline-valid recreation and logout covered
- Hilt, WorkManager and navigation/deep-link instrumentation remained green

## Independent runtime evidence

- maintenance-enabled cold start displayed `در حال بروزرسانی ویستا هستیم`
- malformed cold start displayed `ورود به ویستا`
- offline-valid-session displayed `کاربر تست Foundation` and `به ویستا خوش آمدید`
- dark mode preserved the authenticated boundary; emulator was reset to light afterward
- forced rotation reported `mCurrentRotation=ROTATION_90`
- UI hierarchy reported `rotation="1"` and bounds `[0,0][2400,1080]` while both authenticated texts
  remained visible
- RTL auth hierarchy had right-biased title/field bounds; `MainActivity` supplies
  `LocalLayoutDirection.Rtl`

## Scope and blockers

`FND-AUTH-01` and `FND-QA-01` are resolved by deterministic debug fixtures and the real ten-test
device suite. Flutter and Backend remained read-only. The authenticated screen is still the existing
placeholder; no DSN-01 shell or later feature was created.
