# DSN-01 Phase 1–2 — Tokens, Theme and Font Evidence

Date: 2026-07-27
Branch: `dsn-01/design-system-app-shell`

## Ownership and consumers

- `:core:designsystem` owns brand and semantic colors, spacing, radii, elevation,
  borders, icon size, motion, alpha, component sizes, layout width, typography,
  Light/Dark schemes, system-bar appearance and Vazirmatn resources.
- `:app` consumes `VistaTheme`, brand colors and the new module.
- `:feature:auth` consumes the module and remains under the app-level theme.
- No ViewModel, data source or domain model depends on UI tokens.
- Seven existing Vazirmatn files were moved from app resources to the unique
  owner. No font, SDK, image or dependency was downloaded.

## Static checks

```text
powershell -File scripts\verify-design-token-leakage.ps1
Design token leakage verification passed.

powershell -File scripts\verify-module-boundaries.ps1
Module boundary verification passed.

git diff --check
exit 0
```

The leakage check rejects raw hexadecimal Compose colors and duplicate color or
spacing token owners outside `:core:designsystem`.

## Unit evidence

Task:

```text
:core:designsystem:testDebugUnitTest
```

Result file:

`E:\vista_native\.gradle\dsn-build-output-02\core\designsystem\test-results\testDebugUnitTest\TEST-ir.coffevista.vista_native.core.designsystem.tokens.VistaTokensTest.xml`

Result: 4 tests, 0 skipped, 0 failures, 0 errors. Tests cover approved brand
mapping, monotonic spacing, semantic contrast and the 48dp minimum touch target.

## Build evidence

Final command used a dedicated output so active IDE/Gradle processes were not
terminated or disturbed:

```text
.\gradlew.bat :app:assembleBetaDebug
  "-Pkotlin.incremental=false"
  "-Pvista.buildLogicOutput=build-logic-output-dsn01c"
  "-Pvista.isolatedBuildDir=true"
  "-Pvista.isolatedBuildOutput=dsn-build-output-03"
  --no-daemon --console=plain
```

Result: `BUILD SUCCESSFUL in 1m 27s`, 209 actionable tasks executed.

## Diagnosed attempts

1. The default build-logic output was held by an existing Java/IDE process.
   No process was terminated and no output was deleted.
2. The first isolated attempt selected older transitive tooling coordinates not
   present in verification metadata.
3. Versions were aligned to already-verified project coordinates. A transitive
   debug `ui-tooling` dependency was then removed from the library and kept at
   the app boundary. No verification checksum was weakened or blindly added.

## Still open

- RTL/LTR component behavior, TalkBack semantics, font scale 200% and long
  Persian truncation require component and runtime evidence in later phases.
- Component-only tokens remain unticked until those components consume them.
