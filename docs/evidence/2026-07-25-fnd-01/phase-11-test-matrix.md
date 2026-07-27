# Phase 11 Evidence — Foundation Test Matrix

## Final unit and contract matrix

Command:

`gradlew :core:common:test :core:datastore:testDebugUnitTest :core:network:testDebugUnitTest :core:security:testDebugUnitTest :core:worker:testDebugUnitTest :feature:auth:testDebugUnitTest :app:testBetaDebugUnitTest -Pvista.isolatedBuildDir=true -Pvista.isolatedBuildOutput=fnd-build-output-fnd56 -Pkotlin.incremental=false`

Result:

- `BUILD SUCCESSFUL` in 1m 20s
- 16 suites / 65 tests
- 0 failure / 0 error / 0 skipped
- 193 actionable tasks: 189 executed / 4 up-to-date

Coverage mapping:

- Error/Outcome and redaction: `ErrorClassifierTest`, `ArchitectureFoundationTest`,
  `SecureLoggerTest`
- Dispatcher/clock behavior: injectable dispatcher test plus deterministic clock consumers in
  Startup, TLS and deep-link suites
- Session/startup/auth/onboarding: 27 Auth tests plus 8 encrypted-session tests
- single-flight refresh/401 behavior: `SessionRefreshCoordinatorTest`
- TLS/pinning state machine: 7 `NetworkSecurityPolicyTest` cases
- deep-link parser/pending/replay/dedup: 5 `DeepLinkContractTest` cases
- Proto serializer/default/corruption/migration: 5 DataStore unit cases
- Work retry/idempotency/constraints/cancellation: 3 policy cases plus device tests
- Auth success contract: Retrofit request path/body and successful backend envelope/JWT mapping
- Auth error contract: flat, nested and legacy error envelopes

The Auth success contract test exposed Android-only JWT parsing in the JVM suite. JWT Base64
decoding was moved to the already-used URL-safe Java decoder, and `libs.json.jvm` was added only to
the Auth test runtime. The final suite above passed without `--write-locks`.

## Room and device matrix

Command:

`gradlew :core:database:connectedDebugAndroidTest :core:datastore:connectedDebugAndroidTest :core:security:connectedDebugAndroidTest :core:worker:connectedDebugAndroidTest :app:connectedBetaDebugAndroidTest -Pvista.isolatedBuildDir=true -Pvista.isolatedBuildOutput=fnd-build-output-fnd53b -Pkotlin.incremental=false`

Result on `Medium_Phone_2(AVD) - API 33`:

- `BUILD SUCCESSFUL` in 3m 8s
- 5 Room tests: fresh schema, 1→2 migration, transaction rollback, corruption rejection and
  downgrade rejection
- 2 Proto DataStore tests
- 3 Android Keystore tests
- 2 WorkManager tests
- 11 App tests
- total 23 tests / 0 failure / 0 error / 0 skipped

Post-JWT-change re-check:

`gradlew :core:security:connectedDebugAndroidTest :app:connectedBetaDebugAndroidTest -Pvista.isolatedBuildDir=true -Pvista.isolatedBuildOutput=fnd-build-output-fnd57 -Pkotlin.incremental=false`

- `BUILD SUCCESSFUL` in 2m 7s
- 3 Keystore + 11 App tests
- 0 failed / 0 skipped

App instrumentation includes Startup→Onboarding, Startup→Auth, authenticated/offline/maintenance/
invalid-session, logout, process recreation, forced landscape, RTL alignment, dark/light,
cold/post-login deep-link replay and duplicate suppression.

## Template removal and lock verification

Both generated `ExampleUnitTest` and `ExampleInstrumentedTest` were removed. All reported executions
belong to real Foundation suites.

The first complete unit/device attempts found missing `kotlin-stdlib-common:2.0.21` runtime
configurations in the DataStore lock. Gradle generated the entries through targeted
`dependencyInsight --update-locks`; subsequent unit tasks passed without write mode. No lock entry
was edited manually.

The final all-suite rerun on the final source state is recorded in
`phase-12-runtime-verification.md`: 65 unit/contract plus 23 device tests, all green.
