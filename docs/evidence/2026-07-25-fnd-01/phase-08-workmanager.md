# Phase 8 — WorkManager Evidence

## Unit policy

```text
.\gradlew.bat --no-daemon :core:worker:testDebugUnitTest --rerun-tasks
  -Pvista.isolatedBuildOutput=fnd-build-output-fnd31
  -Pvista.buildLogicOutput=build-logic-output-fnd31
  -Pkotlin.incremental=false

BUILD SUCCESSFUL in 1m 8s
26 actionable tasks: 26 executed
3 tests / 0 failures / 0 errors / 0 skipped
```

پوشش: unique identity و account hashing، deterministic exponential
backoff+jitter/max-attempt و constraints حداقلی.

## Test-driver instrumentation

اجرای اولیه `fnd32` در compile تست، سه API misuse را آشکار کرد (`enqueue`
لیست می‌خواهد و `WorkInfo` nullable است). production source تغییر نکرد؛ تست
اصلاح و در output تازه اجرا شد:

```text
.\gradlew.bat --no-daemon :core:worker:connectedDebugAndroidTest --rerun-tasks
  -Pvista.isolatedBuildOutput=fnd-build-output-fnd33
  -Pvista.buildLogicOutput=build-logic-output-fnd33
  -Pkotlin.incremental=false

Starting 2 tests on Medium_Phone_2(AVD) - 13
Finished 2 tests on Medium_Phone_2(AVD) - 13
BUILD SUCCESSFUL in 1m 14s
59 actionable tasks: 59 executed
```

پوشش:

- `ExistingWorkPolicy.KEEP` دو enqueue یکسان را به یک WorkInfo محدود کرد.
- `TestDriver` قابل‌دریافت بود.
- cancellation با account tag فقط work همان حساب را CANCELLED کرد و work حساب
  دیگر ENQUEUED ماند.

## Auth cleanup consumer

گزارش XML اجرای `fnd30`، 25 تست (3 Worker + 22 Auth) را با صفر
failure/error/skip ثبت کرد. `explicitUnauthorizedClearsWhileTransientFailureKeepsSession`
علاوه بر session clear، cancellation دقیق `account-42` را assert می‌کند.
خود command پس از تولید reportها برای نوشتن lock/metadata از سقف 124 ثانیه
میزبان عبور کرد، پس pass command از اجرای مستقل `fnd31` و device run `fnd33`
گرفته شده است.

## App/Hilt/Manifest

```text
.\gradlew.bat --no-daemon
  :app:compileBetaDebugKotlin :app:processBetaDebugMainManifest
  -Pvista.isolatedBuildOutput=fnd-build-output-fnd34
  -Pvista.buildLogicOutput=build-logic-output-fnd34
  -Pkotlin.incremental=false

exit code 0
```

Merged Manifest شامل `InitializationProvider` برای Emoji/Lifecycle/Profile است
اما هیچ `androidx.work.WorkManagerInitializer` ندارد. Hilt graph با
`HiltWorkerFactory` و `AccountWorkController` compile شد.

## Boundaries

```text
Module boundary verification passed.
:core:worker -> (none)
:feature:auth -> ... :core:worker
no-production-feature-workers
```

هیچ Upload/Chat/Sync worker یا Feature مربوط به فاز بعدی ایجاد نشد.
