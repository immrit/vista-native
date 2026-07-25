# GOV-01 — Build، Variant، Signing و Artifact Contract

## Variant matrix

| Variant | Application ID | Namespace | Type | Debuggable | R8 | Shrink | Signing source | API/environment source | Coexistence |
|---|---|---|---|---:|---:|---:|---|---|---|
| `betaDebug` | `ir.coffevista.vista_native` | `ir.coffevista.vista_native` | debug | بله | خیر | خیر | Android debug keystore | `vista.api.beta` / `VISTA_BETA_API_BASE_URL` / production API fallback | کنار Production نصب می‌شود |
| `betaRelease` | `ir.coffevista.vista_native` | `ir.coffevista.vista_native` | release | خیر | بله | بله | فقط injected release secret؛ در نبود آن unsigned | همان beta source | کنار Production نصب می‌شود |
| `productionDebug` | `ir.coffevista.vista` | `ir.coffevista.vista_native` | debug | بله | خیر | خیر | Android debug keystore | `vista.api.production` / `VISTA_PRODUCTION_API_BASE_URL` / production API fallback | کنار Beta؛ روی Flutter امضاشده upgrade نمی‌شود |
| `productionRelease` | `ir.coffevista.vista` | `ir.coffevista.vista_native` | release | خیر | بله | بله | فقط injected production signing؛ در نبود آن unsigned | همان production source | کنار Beta؛ جایگزین Flutter فقط با signing اصلی |

`productionRelease` بدون signing اصلی صرفاً یک artifact unsigned برای build
validation است و release-ready، production-installable یا upgrade-compatible
اعلام نمی‌شود.

## Signing injection

هر چهار مقدار باید با هم از Gradle secret property یا environment تزریق شوند:

| Gradle property | Environment |
|---|---|
| `vista.signing.storeFile` | `VISTA_SIGNING_STORE_FILE` |
| `vista.signing.storePassword` | `VISTA_SIGNING_STORE_PASSWORD` |
| `vista.signing.keyAlias` | `VISTA_SIGNING_KEY_ALIAS` |
| `vista.signing.keyPassword` | `VISTA_SIGNING_KEY_PASSWORD` |

پیکربندی ناقص عمداً build را متوقف می‌کند. هیچ keystore، password یا alias
واقعی در repository نگهداری نمی‌شود. مسیر Production signing تولید یا حدس زده
نشده است.

## Build commands

```powershell
.\gradlew.bat --no-daemon clean `
  test lint `
  assembleBetaDebug assembleBetaRelease `
  assembleProductionDebug assembleProductionRelease `
  "-Pkotlin.incremental=false"

.\scripts\verify-r8.ps1
.\scripts\secret-scan.ps1
.\scripts\report-artifacts.ps1
git diff --check
```

برای dependency vulnerability scan:

```powershell
$env:NVD_API_KEY = "[INJECT_FROM_SECRET_STORE]"
.\gradlew.bat --no-daemon dependencyCheckAggregate
```

گزارش OWASP در `build/reports/dependency-check/` و metadata artifact در
`artifacts/artifact-metadata.json` تولید می‌شود؛ هر دو generated و خارج از Git
هستند. Analyzer مربوط به Sonatype OSS Index عمداً غیرفعال است تا dependency
metadata پروژه به سرویس ثالث ارسال نشود؛ اسکن از CVE data عمومی NVD که به‌صورت
محلی دریافت می‌شود استفاده می‌کند. CI پیش از اجرای scan، وجود secret
`NVD_API_KEY` را الزام می‌کند تا job وارد retry طولانی و rate-limit نشود.

## Artifact identity

Artifact identity با موارد زیر تعریف می‌شود:

- flavor و build type؛
- application ID ثابت flavor؛
- version code/name؛
- SHA-256 و اندازه‌ی APK؛
- signed/unsigned state؛
- وضعیت minification و resource shrinking؛
- mapping/seeds/usage/configuration مربوط به release.

نام و مسیر واقعی APK را Gradle تولید می‌کند و اسکریپت metadata همان خروجی واقعی
را ثبت می‌کند؛ نامی که توسط API داخلی AGP تغییر داده شود استفاده نشده است.

## Reproducibility boundary

- Gradle Wrapper، Version Catalog، dependency locks و verification metadata
  باید commit شوند.
- line endingها با `.gitattributes` ثابت‌اند.
- build به `local.properties`، IDE state یا فایل secret محلی وابسته نیست؛ در CI
  از `ANDROID_HOME` استفاده می‌شود.
- build بدون signing secret، release APK unsigned می‌سازد. مقایسه‌ی SHA-256
  فقط بین artifactهایی با signing state یکسان معتبر است.
- timestamp، ابزار signing و نسخه‌ی JDK/SDK بخشی از build environment metadata
  هستند؛ اگر byte-for-byte equality حاصل نشود، اختلاف باید ثبت شود و
  «reproducible» به معنای ساخت قابل‌تکرار از clean checkout گزارش می‌شود.

## Verification snapshot — 2026-07-25

| Artifact | SHA-256 فعلی | Bytes | Signing |
|---|---|---:|---|
| `app-beta-debug.apk` | `2d86fda12bafff1c79d2a4b6ff9e97e4cdeca5faa48b04ccb34f0d3a4b558257` | 14,392,165 | debug signed |
| `app-beta-release-unsigned.apk` | `864324ac4a9869640eaf85d6f2ac954098d24ba4727de58be51dbf7428e15ce5` | 4,661,070 | unsigned |
| `app-production-debug.apk` | `80c14d882fccf8b26579e6a9e4247238a742724b74ccab4e02ea3488c1b3134b` | 14,392,161 | debug signed |
| `app-production-release-unsigned.apk` | `52a5119ad955ae046bbcdc9b5552b272ca6733e8db6a319687d1dfc3a484a59b` | 4,661,066 | unsigned |

- clean build اول: `BUILD SUCCESSFUL` در ۳m۱۷s؛ clean build دوم:
  `BUILD SUCCESSFUL` در ۲m۰۲s.
- دو release unsigned در دو build مستقل byte-for-byte SHA برابر داشتند.
- debugهای signed در هر دو build تولید شدند، اما SHA آن‌ها یکسان نبود؛ بنابراین
  برای debug ادعای byte-for-byte reproducibility نمی‌شود.
- unit test نهایی: ۸۰ execution، صفر failure/error/skipped.
- lint نهایی: صفر error و ۲۳ warning.
- R8: mapping/seeds/usage/configuration برای هر دو release موجود؛ پنج کلاس
  بحرانی startup/auth در mapping باقی ماندند.
- secret scan: ۱۲۸ فایل tracked، صفر finding.
- OWASP task graph با `dependencyCheckAggregate --dry-run` معتبر بود، اما scan
  واقعی به‌دلیل نبود `NVD_API_KEY` و مجوز شبکه Pass نشده است.

## Scope boundary

این تغییر فقط Build Governance است. هیچ Feature، Startup/Auth behavior،
Navigation، Room، DataStore، WorkManager، Hilt، Backend یا Migration Bridge
تغییر نکرده است.
