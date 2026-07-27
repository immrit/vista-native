# Phase 6 — Proto DataStore Evidence

## شاهدهای موفق

### Unit و contract

```text
.\gradlew.bat --no-daemon
  :core:datastore:testDebugUnitTest
  :feature:auth:testDebugUnitTest
  --rerun-tasks --write-locks --write-verification-metadata sha256
  -Pvista.isolatedBuildOutput=fnd-build-output-fnd13
  -Pvista.buildLogicOutput=build-logic-output-fnd13
  -Pkotlin.incremental=false

BUILD SUCCESSFUL
85 actionable tasks: 85 executed
7 suites / 27 tests / 0 failures / 0 errors / 0 skipped
```

این مجموعه serializer round-trip، default، malformed proto، schema migration،
onboarding state و retention onboarding پس از terminal session clear را پوشش می‌دهد.

### Instrumentation روی API 33

```text
.\gradlew.bat --no-daemon
  :core:datastore:connectedDebugAndroidTest
  -Pvista.isolatedBuildOutput=fnd-build-output-fnd19
  -Pvista.buildLogicOutput=build-logic-output-fnd19
  -Pkotlin.incremental=false

Starting 2 tests on Medium_Phone_2(AVD) - 13
Finished 2 tests on Medium_Phone_2(AVD) - 13
BUILD SUCCESSFUL in 1m 12s
66 actionable tasks: 66 executed
```

هر دو تست زیر پاس شدند:

- `legacySharedPreferencesMigratesExactlyOnceToCurrentProto`
- `corruptNonSensitivePreferencesRecoverToDeterministicDefault`

### App composition/build

```text
.\gradlew.bat --no-daemon assembleBetaDebug
  --update-locks org.jetbrains.kotlin:kotlin-stdlib-common
  -Pvista.isolatedBuildOutput=fnd-build-output-fnd23
  -Pvista.buildLogicOutput=build-logic-output-fnd23
  -Pkotlin.incremental=false

BUILD SUCCESSFUL in 1m 42s
166 actionable tasks: 166 executed
APK: app-beta-debug.apk
Size: 15,665,239 bytes
```

این build، provider واقعی `OnboardingStore` در Hilt composition root و consumerهای
Startup/Onboarding را compile و package کرده است.

### Sensitive-field scan

```text
rg -n -i "token|private.?key|credential|session"
  core/datastore/src/main/proto/app_preferences.proto

no-sensitive-field-names
```

## رخدادهای تشخیصی

- اجرای اولیه به خطای KSP `error.NonExistentClass` برای generated protobuf type
  خورد. construction در owner module باقی ماند و Hilt singleton lifecycle به
  composition root منتقل شد؛ اجرای بعدی سبز شد.
- instrumentation اولیه نشان داد `kotlin-stdlib-common:2.0.21` در runtime
  lock ثبت نشده بود. classpathهای debug/release و unit/instrumentation به‌صورت
  هدفمند resolve و lock شدند.
- اجرای aggregate `test` با output `fnd24` پس از 124 ثانیه توسط سقف میزبان
  timeout شد؛ 20 suite و 64 تست ثبت‌شده تا آن لحظه همگی سبز بودند، اما این
  اجرا به‌عنوان pass کامل aggregate شمرده نمی‌شود.
- output استفاده‌شده‌ی `fnd15` در اجرای تکراری به Windows handle-lock خورد؛
  اجرای موفق و مستقل instrumentation روی output تازه `fnd19` ثبت شد.

## نتیجه Phase 6

Proto schema محدود و غیرحساس، serializer، versioning، migration، corruption
recovery، onboarding consumer و logout retention با شاهد unit، device و App
build اثبات شدند. هیچ Flutter/Backend change یا `DSN-01` feature ایجاد نشده است.
