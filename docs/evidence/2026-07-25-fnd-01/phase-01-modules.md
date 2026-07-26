# Phase 1 — Modular Monolith Evidence

## نتیجه

- sourceهای Startup/Onboarding/Auth از `:app` به `:feature:auth` منتقل شدند.
- model، common result، network failure، onboarding persistence و secure
  session persistence owner مستقل و مصرف‌شده دارند.
- دو convention plugin برای libraryهای Android و pure Kotlin اضافه شد و
  Version Catalog مرجع dependencyها باقی ماند.
- UI منتقل‌شده resource و theme را از `:app` از طریق visual slot دریافت
  می‌کند؛ Design System جدیدی پیاده نشده است.
- `core:database` تا Phase 5 و وجود schema/DAO/consumer واقعی defer شد.

## شواهد قابل‌بازتولید

```powershell
.\scripts\verify-module-boundaries.ps1
.\gradlew.bat --no-daemon test assembleBetaDebug "-Pkotlin.incremental=false"
.\gradlew.bat --no-daemon assembleBetaDebug assembleBetaRelease `
  assembleProductionDebug assembleProductionRelease "-Pkotlin.incremental=false"
.\scripts\verify-r8.ps1
git diff --check
```

Checkpoint میانی `test assembleBetaDebug` با ۲۹۸ task و exit code صفر پاس شد.
checksum artifactهای AndroidX/Kotlin که به‌واسطه انتقال Compose به library
resolve شدند با `--write-verification-metadata sha256` ثبت شدند؛ dependency
lock هر ماژول نیز جداگانه version-controlled است.

Checkpoint نهایی:

- `betaDebug` و `betaRelease`: exit code صفر؛ ۳۲۱ task.
- `productionDebug` و `productionRelease`: exit code صفر؛ ۳۲۱ task.
- unit test XML: ۱۴ suite، ۴۲ execution، صفر failure/error/skipped.
- R8: mapping/seeds/usage/configuration هر دو Release موجود و class boundary
  سه owner بحرانی Startup/Auth با keep rule محدود حفظ شد؛ optimization و
  obfuscation همچنان فعال است.
- module boundary script و `git diff --check`: exit code صفر.
