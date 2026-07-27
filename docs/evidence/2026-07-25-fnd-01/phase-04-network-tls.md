# Phase 4 — Network/TLS Evidence

## Automated evidence

```powershell
.\gradlew.bat --no-daemon `
  :core:network:testDebugUnitTest `
  :feature:auth:testDebugUnitTest `
  --rerun-tasks `
  "-Pvista.isolatedBuildDir=true" `
  "-Pkotlin.incremental=false"

.\gradlew.bat --no-daemon testBetaDebugUnitTest assembleBetaDebug `
  --rerun-tasks --write-locks `
  "-Pvista.isolatedBuildDir=true" `
  "-Pkotlin.incremental=false"

.\gradlew.bat --no-daemon connectedBetaDebugAndroidTest --write-locks `
  "-Pvista.isolatedBuildDir=true"

.\scripts\verify-module-boundaries.ps1
git diff --check
```

- targeted Network/Auth: ۸۲ task executed؛ ۱۳ suite و ۵۴ test، صفر
  failure/error/skipped.
- checkpoint کامل app: exit code صفر؛ ۲۱ suite و ۶۴ test، صفر
  failure/error/skipped؛ APK Beta Debug با اندازه ۱۴٬۸۷۵٬۳۱۲ بایت.
- instrumentation پس از اصلاح نهایی boundary: ۲/۲ تست سبز روی
  `Medium_Phone_2`، Android 13 / API 33.
- module boundary: پاس؛ `core:network -> core:common` و
  `core:security -> core:common, core:model`.
- `git diff --check`: پاس.

## Covered security cases

- bounded idempotent retry و منع retry برای POST/TLS
- exact-host Authorization و منع subdomain bypass
- single-flight burst با ۱۲ caller، دقیقاً یک refresh و یک persist
- transition موفق، terminal و transient
- TLS off/monitor/enforce، missing، expired، mismatch و current/next rotation
- retain آخرین policy معتبر در ورودی unsigned/stale
- rebuild دقیقاً یک‌باره‌ی client روی revision جدید
- HTTPS/host/path allowlist و حذف headerهای credential برای external URL
- cleartext=false در Manifest و Network Security Config

## Variant matrix closure

پس از تأیید تازه، matrix در output مستقل `fnd-build-output-fnd10` اجرا شد:

```powershell
.\gradlew.bat --no-daemon `
  assembleBetaDebug assembleBetaRelease `
  assembleProductionDebug assembleProductionRelease `
  --write-locks `
  "-Pvista.isolatedBuildDir=true" `
  "-Pvista.isolatedBuildOutput=fnd-build-output-fnd10" `
  "-Pvista.buildLogicOutput=build-logic-output-fnd10"
```

- ۵۰۲ task executed، exit code صفر.
- Beta Debug: ۱۵٬۰۳۹٬۸۹۰ بایت.
- Beta Release unsigned: ۴٬۷۷۹٬۹۴۹ بایت.
- Production Debug: ۱۵٬۰۳۹٬۸۸۶ بایت.
- Production Release unsigned: ۴٬۷۷۹٬۹۴۹ بایت.
- هر دو Release از R8، resource shrink و lintVital عبور کردند.
- unsigned بودن Release مطابق blocker مستقل `REL-SIGN-01` است و به‌عنوان
  production-signing evidence محسوب نمی‌شود.
