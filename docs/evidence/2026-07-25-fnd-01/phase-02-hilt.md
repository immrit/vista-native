# Phase 2 — Hilt Composition Evidence

## فرمان‌های بازتولید

```powershell
rg -n "AppContainer|ViewModelProvider\.Factory" app/src/main feature/auth/src/main
.\gradlew.bat --no-daemon test assembleBetaDebug `
  "-Pvista.isolatedBuildDir=true" "-Pkotlin.incremental=false"
.\gradlew.bat --no-daemon connectedBetaDebugAndroidTest `
  "-Pvista.isolatedBuildDir=true"
.\scripts\verify-module-boundaries.ps1
git diff --check
```

Hilt `2.52` و AndroidX Hilt Navigation `1.2.0` از Version Catalog resolve
می‌شوند. processor از KSP `2.0.21-1.0.28` هم‌نسخه با Kotlin plugin استفاده
می‌کند و dependency locks/checksum metadata به‌روزرسانی شده‌اند.

`HiltCompositionSmokeTest` Activity واقعی، `VistaApplication` واقعی و graph
production را launch کرد و تزریق `AuthenticationStateOwner` را اثبات کرد.

## نتیجه

- `test assembleBetaDebug`: ۳۵۸ task، exit code صفر.
- XML: ۱۵ suite، ۴۴ execution، صفر failure/error/skipped.
- `connectedBetaDebugAndroidTest`: دو تست، صفر failure/error روی
  `Medium_Phone_2`، API 33.
- جست‌وجوی `AppContainer` و `ViewModelProvider.Factory` در production source:
  صفر hit.
- `verify-module-boundaries.ps1` و `git diff --check`: exit code صفر.

Hilt 2.52 در `AggregateDepsTask` با JavaPoet قدیمی AGP خطای شناخته‌شده
`ClassName.canonicalName()` داشت. Hilt plugin و JavaPoet 1.13 در included
`build-logic` هم‌راستا شدند و task اختیاری aggregating غیرفعال شد؛ processor
aggregation پشتیبانی‌شده graph را برای تمام Variantهای unit test و app
کامپایل کرد و runtime instrumentation نیز injection را اثبات کرد.

فایل‌های generated مسیرهای قدیمی `*/build` توسط handle خارجی Windows قفل
شدند. برای حفظ شواهد، override اختیاری `vista.isolatedBuildDir=true` output
تازه را زیر `.gradle/fnd-build-output` ساخت. این workaround در Git نادیده
گرفته می‌شود و blocker clean path تا Phase 12 باز می‌ماند.
