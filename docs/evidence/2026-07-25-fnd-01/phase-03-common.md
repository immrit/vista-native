# Phase 3 — Common Foundation Evidence

## فرمان بازتولید

```powershell
.\gradlew.bat --no-daemon `
  :core:common:test `
  :core:network:testDebugUnitTest `
  :feature:auth:testDebugUnitTest `
  --rerun-tasks `
  "-Pvista.isolatedBuildDir=true" `
  "-Pkotlin.incremental=false"

.\gradlew.bat --no-daemon testBetaDebugUnitTest assembleBetaDebug `
  "-Pvista.isolatedBuildDir=true" `
  "-Pkotlin.incremental=false"

git diff --check
```

## شواهد contract

- `Outcome<T>` تنها result wrapper و `AppError` تنها error payload است.
- testهای `ErrorClassifier` نگاشت HTTP، timeout و TLS و حفظ فقط cause type
  غیرحساس را اثبات می‌کنند.
- testهای `BackendErrorParser` fixtureهای flat، nested و legacy را پوشش می‌دهند.
- test repository، `DispatcherProvider` را با `StandardTestDispatcher`
  جایگزین می‌کند.
- testهای Startup، `EpochClock` ثابت و `NetworkMonitor` جعلی offline را مصرف
  می‌کنند و صفر maintenance/refresh call را اثبات می‌کنند.
- testهای `AppEnvironment` رد cleartext و URL دارای credential را اثبات می‌کنند.
- test logger، redaction token، phone، message، encryption payload، key و
  header حساس و خاموش‌بودن sink را اثبات می‌کند.

## نتیجه

- اجرای non-cache سه suite: ۸۴ task executed، exit code صفر.
- XML سه ماژول: ۱۲ suite، ۴۷ test، صفر failure/error/skipped.
- checkpoint کامل `testBetaDebugUnitTest assembleBetaDebug --rerun-tasks`:
  ۱۵۱ task executed، exit code صفر.
- XML تمام outputهای isolated موجود: ۱۹ suite، ۵۵ test، صفر
  failure/error/skipped.
- APK تازه‌ی Beta Debug با اندازه ۱۴٬۶۷۸٬۲۱۱ بایت تولید شد.
- `verify-module-boundaries.ps1`: پاس؛ dependency معکوس یا
  Feature-to-Feature گزارش نشد.
- `git diff --check`: exit code صفر.
