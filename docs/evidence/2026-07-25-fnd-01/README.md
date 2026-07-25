# FND-01 Evidence Index

این پوشه، خلاصه‌ی شواهد قابل‌بازتولید فاز `FND-01` را نگهداری می‌کند. خروجی‌های
generated حجیم Gradle در `build/` و `app/build/` باقی می‌مانند و وارد Git
نمی‌شوند. screenshotها و گزارش‌های متنی مرحله‌های بعد با نام phase و سناریو
در همین مسیر ثبت خواهند شد.

## Phase 0 — Pre-refactor baseline

**Repository**

- Branch پیش از شروع اجرا: `gov-01/repository-build-foundation`
- Baseline commit: `3115b0db2f2874e2254d87cacc9da6f04e479e3c`
- Branch اجرای فاز: `fnd-01/architecture-foundation`
- Module graph: فقط `:app`؛ included build برابر `:build-logic`
- Source inventory: ۲۸ فایل Kotlin اصلی، ۵ فایل unit test و ۱ فایل
  instrumentation test
- Dependency graph مبنا:
  `:app:dependencies --configuration productionDebugRuntimeClasspath`

**Toolchain**

- Java: Temurin `17.0.19`
- Gradle: `8.13`
- Kotlin plugin: `2.0.21`
- AGP: `8.13.2`
- Android platformهای نصب‌شده: `31`, `33`, `34`, `35`, `36`
- Android Build Tools: `29.0.1`, `34.0.0`, `35.0.0`, `36.0.0`, `37.0.0`
- AVDهای مشاهده‌شده: `Medium_Phone`, `Medium_Phone_2`
- دستگاه متصل هنگام baseline: `emulator-5554`، مدل `sdk_gphone64_x86_64`

**Clean verification command**

```powershell
.\gradlew.bat --no-daemon clean test lint `
  assembleBetaDebug assembleBetaRelease `
  assembleProductionDebug assembleProductionRelease `
  "-Pkotlin.incremental=false"
```

نتیجه: exit code صفر در حدود ۲ دقیقه و ۴۰ ثانیه.

| Gate | نتیجه |
|---|---|
| Unit tests | ۲۰ XML suite، ۸۰ execution، صفر failure/error/skipped |
| Lint | task کامل با exit code صفر |
| `betaDebug` | APK ساخته شد |
| `betaRelease` | APK unsigned ساخته شد |
| `productionDebug` | APK ساخته شد |
| `productionRelease` | APK unsigned ساخته شد |
| R8 | mapping/seeds/usage/configuration هر دو release موجود و کلاس‌های بحرانی حفظ شدند |
| Secret scan | ۱۲۹ فایل tracked، صفر finding |
| Diff check | `git diff --check` پاس شد |
| Dependency governance | دو lockfile و `gradle/verification-metadata.xml` موجود |

اولین تلاش clean پس از timeout عمدی command کوتاه، به‌دلیل Gradle daemon
فعال و lock روی `app/build` شکست خورد. فقط daemon نسخه `8.13` با
`gradlew --stop` متوقف شد؛ daemon نسخه `8.14` متعلق به جریان دیگر دست‌نخورده
ماند. تکرار clean verification سپس پاس شد.

Artifact metadata generated در `artifacts/artifact-metadata.json` و reportهای
test/lint/R8 در `app/build/` قرار دارند و طبق Governance وارد Git نمی‌شوند.
