# Phase 12 Evidence — Runtime and Final Verification

## Runtime targets

Installed AVD/system-image inventory:

- `Medium_Phone_2`: API 33, `google_apis_playstore`, x86_64
- `Medium_Phone`: API 37 preview image, not used as a substitute for minSdk
- installed system images: API 33 and API 37 only
- API 24 and any image between API 24 and API 32 are absent

`API 24 Compatibility Gate: Open — Required before REL-01 Beta release`

- Owner: `REL-01 Compatibility QA`
- Exit: install or access an API 24 device/emulator; run Startup, Auth,
  SessionStore/Keystore, Room, DataStore and WorkManager; observe no crash or API
  incompatibility.
- This waiver is not API 24 compatibility evidence.
- API 33 is only the current Foundation Runtime Gate and is not reported as a
  substitute for API 24.
- No API 24 system image, SDK package, or AVD was installed or created during
  closure.

The final Beta Debug APK installed successfully on API 33:

- artifact:
  `.gradle/fnd-build-output-api33-gate/app/outputs/apk/beta/debug/app-beta-debug.apk`
- Gradle `:app:installBetaDebug`: `Installed on 1 device`
- installed package: `ir.coffevista.vista_native`

Closure confirmation used the existing healthy `Medium_Phone_2` AVD:

- API: 33
- ABI: x86_64
- boot completed: `1`
- only connected device: `emulator-5554`

## Startup and journey runtime

Three force-stopped cold runs:

| Run | Launch state | Total time |
|---|---|---:|
| 1 | COLD | 4,783 ms |
| 2 | COLD | 5,338 ms |
| 3 | COLD | 5,910 ms |

Three same-process foreground launches returned with zero launch time, followed by three background
resumes with wait times 517/173/172 ms. These numbers are diagnostic only and are not a performance
claim.

`uiautomator` hierarchy checks on API 33:

- `first-run` → onboarding title present
- `maintenance-disabled` → Auth title present
- `maintenance-enabled` → Maintenance title present
- `valid-session` → authenticated placeholder present
- `offline-valid-session` → offline authenticated user present
- `offline-no-session` → Auth title present
- `malformed` → Auth title present
- `refresh-expired` → Auth title present

Closure rerun recorded eight force-stopped cold journeys with expected hierarchy
and zero crash lines:

| Fixture | Result | Total time |
|---|---|---:|
| `first-run` | Onboarding | 4,680 ms |
| `maintenance-disabled` | Auth | 5,456 ms |
| `maintenance-enabled` | Maintenance | 5,724 ms |
| `valid-session` | Authenticated boundary | 5,363 ms |
| `offline-valid-session` | Authenticated boundary | 5,015 ms |
| `offline-no-session` | Auth | 5,995 ms |
| `malformed` | Auth | 5,126 ms |
| `refresh-expired` | Auth | 5,328 ms |

The same-process warm delivery returned in 9 ms with the expected Auth
hierarchy.

Memory at the Auth checkpoint:

- Total PSS: 101,038 KB
- Total RSS: 195,540 KB
- Total Swap PSS: 301 KB
- 1 Activity / 7 Views / 0 WebViews

## Recreation, rotation and deep link

- force-stop removed PID `30720`; restart created PID `30844` and restored the offline-authenticated
  fixture boundary
- final forced landscape hierarchy reported `rotation="1"` with Auth visible; emulator was reset to
  `ROTATION_0` and light mode
- Activity recreation and state preservation also pass in instrumentation
- cold `vista://post/post_42` opened the controlled deferred destination
- a duplicate warm intent followed by one Back returned directly to the authenticated boundary,
  proving no duplicate destination was stacked
- logged-out post-login replay passes as a real API 33 instrumentation journey exactly once

Closure confirmation additionally recorded:

- process restart: PID `5295` stopped, no PID remained, PID `5356` started, and
  the offline authenticated boundary restored
- cold deep link: controlled deferred destination displayed
- duplicate warm deep link: one Back returned to the authenticated boundary
  with no duplicate destination remaining
- landscape hierarchy: `rotation="1"` with Auth visible; rotation was reset
- fresh logcat scan: zero `FATAL EXCEPTION`, `NoSuchMethodError`, or
  `VerifyError` lines

## Final test matrix

Command:

`gradlew [all Foundation unit tasks] [all Foundation connectedAndroidTest tasks] -Pvista.isolatedBuildDir=true -Pvista.isolatedBuildOutput=fnd-build-output-fnd63 -Pkotlin.incremental=false`

Result:

- `BUILD SUCCESSFUL` in 2m 48s
- 405 actionable tasks: 401 executed / 4 up-to-date
- unit/contract: 16 suites / 65 tests / 0 failure / 0 error / 0 skipped
- device: 5 suites / 23 tests / 0 failure / 0 error / 0 skipped
- device split: Room 5, DataStore 2, Keystore 3, WorkManager 2, App 11

A fresh closure-only API 33 device run produced the same 5 suites / 23 tests /
0 failure / 0 error / 0 skipped under
`.gradle/fnd-build-output-api33-device-final`.

## Four-variant and R8 checkpoint

Final command:

`gradlew clean :app:assembleBetaDebug :app:assembleBetaRelease :app:assembleProductionDebug :app:assembleProductionRelease -Pvista.isolatedBuildDir=true -Pvista.isolatedBuildOutput=fnd-build-output-fnd62 -Pkotlin.incremental=false`

Result:

- `BUILD SUCCESSFUL` in 3m 50s
- 577 actionable tasks: 523 executed / 54 up-to-date
- all four APKs exist
- Beta IDs: `ir.coffevista.vista_native`
- Production IDs: `ir.coffevista.vista`
- both release filenames end in `-unsigned.apk`
- both release APKs return `DOES NOT VERIFY` from `apksigner`
- both release mapping directories contain non-empty mapping/resources/seeds/usage/configuration
- both mappings contain MainActivity, VistaApplication, StartupResolver and DeepLinkCoordinator

The matrix exposed incomplete lock configurations for DataStore runtime and App typed-navigation
serialization. Each missing configuration was generated through Gradle `dependencyInsight`,
`--update-locks` or configuration-specific `--write-locks`; no lock entry was manually edited.

## Lint, secrets and repository checks

- final full `lint`: `BUILD SUCCESSFUL`, 0 errors and 38 warnings
- 35 warnings are dependency-upgrade availability; upgrading the stack is outside FND-01
- 1 warning is the delegate-first custom trust manager; platform certificate validation runs before
  the additional pin decision
- 2 warnings recommend optional SharedPreferences KTX syntax
- secret scan was expanded to include tracked and untracked non-ignored files:
  238 files / 0 findings in the final documentation-only state
- module-boundary verification passed with no cycle or forbidden feature dependency
- `git diff --check` passed; only Git's CRLF normalization notice for verification metadata remains
- successful dependency-verified builds prove lock/verification metadata are consumable

Flutter remains at `64994fa1dfa6f1b146cb9d132758f105670e4d71` with the pre-existing dirty
worktree. Backend is currently `a0536966fb5fbeb5c5d36e8cf099e99619bd897c` with its pre-existing
`internal/serviceshub/repository.go` modification. Both repositories were outside the writable roots
for this task and were only read.
