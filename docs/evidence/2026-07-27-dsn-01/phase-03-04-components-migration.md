# DSN-01 Phase 3–4 — Component and Migration Checkpoint

Date: 2026-07-27

## Implemented and consumed

- `VistaButton` is consumed by Auth, Onboarding and Maintenance with primary,
  outline, text, loading and disabled behavior.
- `VistaTextField` is consumed by identifier, password and OTP states.
- `VistaErrorState` and `VistaLoadingState` are consumed by Startup.
- `VistaSurface` is consumed by Auth and Onboarding.
- The Startup resolver, Auth ViewModel/state machine, onboarding persistence and
  navigation contracts were not changed.

Dialog, sheet, snackbar, avatar, media card, skeleton, empty state, scaffold,
top app bar, badge, divider and navigation components are present for the Shell
implementation but remain unticked until their production consumers and tests
are connected.

## Verification

```text
.\gradlew.bat :feature:auth:testDebugUnitTest
  :app:testBetaDebugUnitTest :app:assembleBetaDebug
  "-Pkotlin.incremental=false"
  "-Pvista.buildLogicOutput=build-logic-output-dsn01e"
  "-Pvista.isolatedBuildDir=true"
  "-Pvista.isolatedBuildOutput=dsn-build-output-05"
  --no-daemon --console=plain
```

Result: `BUILD SUCCESSFUL in 1m 17s`, 227 actionable tasks.

JUnit XML inventory under `.gradle/dsn-build-output-05`:

- result files: 7
- tests: 33
- failures: 0
- errors: 0

`git diff --check` passed. Runtime and post-migration screenshots are deferred
to the API 33 phases and are not inferred from this compile/test checkpoint.
