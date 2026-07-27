# Phase 5 — Room Evidence

## Scope

- یک database واقعی، یک entity و یک DAO برای آخرین verified TLS policy.
- persistence به `TlsPolicyStore` مصرف‌شده وصل است.
- migration `1 -> 2` از single pin به current/next pin.
- هیچ جدول Feature و هیچ bridge/import از Flutter/Isar وجود ندارد.

## Checkpoint

فرمان‌های اصلی:

```powershell
.\gradlew.bat --no-daemon `
  :core:database:connectedDebugAndroidTest --write-locks `
  "-Pvista.isolatedBuildDir=true" `
  "-Pvista.isolatedBuildOutput=fnd-build-output-fnd07" `
  "-Pvista.buildLogicOutput=build-logic-output-fnd05"

.\gradlew.bat --no-daemon testBetaDebugUnitTest assembleBetaDebug `
  --rerun-tasks --write-locks `
  "-Pvista.isolatedBuildDir=true" `
  "-Pvista.isolatedBuildOutput=fnd-build-output-fnd08" `
  "-Pvista.buildLogicOutput=build-logic-output-fnd05" `
  "-Pkotlin.incremental=false"

.\gradlew.bat --no-daemon connectedBetaDebugAndroidTest --write-locks `
  "-Pvista.isolatedBuildDir=true" `
  "-Pvista.isolatedBuildOutput=fnd-build-output-fnd09" `
  "-Pvista.buildLogicOutput=build-logic-output-fnd09"

.\gradlew.bat --no-daemon test --write-locks `
  "-Pvista.isolatedBuildDir=true" `
  "-Pvista.isolatedBuildOutput=fnd-build-output-fnd11" `
  "-Pvista.buildLogicOutput=build-logic-output-fnd11"
```

## Results

- Room instrumentation: ۵/۵ روی `Medium_Phone_2`، API 33؛ fresh create،
  migration 1→2، transaction rollback، schema corruption و downgrade denial.
- app Hilt/runtime smoke پس از اتصال Room: ۲/۲ روی همان emulator.
- full root `test`: ۳۷۲ task executed؛ ۲۵ suite، ۸۰ test، صفر
  failure/error/skipped.
- Beta Debug build non-incremental: ۱۷۴ task executed، exit code صفر.
- schema export:
  - `1.json`: `F3E96F2B02793BD2E4930555D9E0D85EB13056C8F3E5C7AB30760FD218B93258`
  - `2.json`: `33E33ABAC9A89F977C124F7260B2D3859C33A9F61D29E53125921A10FC9E08FF`
- module boundary و `git diff --check`: پاس.
- inventory production: فقط یک `@Entity` با نام `verified_tls_policy`؛ صفر
  Isar/import/Feature table.
