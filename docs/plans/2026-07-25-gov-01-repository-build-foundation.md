# GOV-01 — Repository و Build Foundation

**نوع:** Build Governance
**تاریخ شروع:** 2026-07-25
**وضعیت Gate:** `GOV-01 Complete — Local Governance Gate Passed`
**پیشرفت کل:** `[██████████] 100%` — 40 از 40 تسک تکمیل شده
**Production Signing Gate:** `Open`
**Dependency CVE Execution Gate:** `Open`
**Implementation Remediation Backlog:** `Open`

> قاعده‌ی پیشرفت: فقط پس از ثبت شاهد قابل‌بازتولید، checkbox همان تسک و
> Progress Bar به‌روزرسانی می‌شود. موفقیت compile به‌تنهایی معادل عبور Gate
> یا runtime verification نیست.

## 1. مرز فاز

این سند tracker اجرایی فاز `GOV-01` است. اجرای `FND-01`، modularization کامل،
Room، Proto DataStore، WorkManager، Hilt، بازنویسی Navigation، اصلاح Deep Link،
اصلاح Isar/WebView Flutter، Migration Bridge، Feature جدید، Backend و رفتار
Startup/Auth خارج از محدوده‌اند.

## 2. Progress dashboard

| بخش | پیشرفت | وضعیت |
|---|---:|---|
| اسناد و scope | `[██████████] 100%` — 6/6 | Master Plan اصلی ثبت و با Source تطبیق داده شد |
| baseline و safety | `[██████████] 100%` — 8/8 | تکمیل |
| Git baseline | `[██████████] 100%` — 5/5 | baseline مستقل و branch فاز ثبت شد |
| Build governance | `[██████████] 100%` — 5/5 | تکمیل |
| flavor/signing/R8 | `[██████████] 100%` — 6/6 | تکمیل |
| CI و مستندات | `[██████████] 100%` — 5/5 | config و task graph محلی معتبر؛ workflow هنوز روی GitHub اجرا نشده |
| verification و Gate | `[██████████] 100%` — 5/5 | Local Gate پاس؛ CVE execution به CI activation منتقل شد |

## 3. Task checklist

### A — اسناد، Scope و Risk Transfer

- [x] متن مأموریت پیوست کامل خوانده شد.
- [x] `VISTA_NATIVE_MASTER_PLAN.md` در مسیر اصلی Repository ثبت و کامل خوانده
  شد؛ Source و Target هر دو `22156` بایت و دارای SHA-256 برابر
  `9511b4cda42fb93e50fd6d04c520a4b958b8a6fe2ffb4db37f3da7640d7fec4d`
  هستند.
- [x] منبع بالادستی Master Plan در
  `C:\Users\MriT.DESKTOP-UK7OADT\Downloads\PLAN.md` کامل و read-only خوانده شد.
- [x] `docs/plans/2026-07-25-aud-01-parity-ledger.md` کامل خوانده شد.
- [x] `docs/audits/2026-07-25-aud-01-parity-ledger.md` کامل خوانده شد.
- [x] هشت ریسک منتقل‌شده در Risk Register نهایی با owner و وضعیت `Open`
  حفظ شوند.

### B — Baseline و Safety

- [x] نبود parent Git repository در `E:\` اثبات شد.
- [x] پوشه‌ی خالی `.git` از Git repository معتبر تفکیک شد؛
  `git rev-parse` با exit code `128` پایان یافت.
- [x] وضعیت Flutter ثبت شد:
  `main@64994fa1dfa6f1b146cb9d132758f105670e4d71` با dirty changes موجود.
- [x] تمام تغییرات Flutter بدون reset/stash/checkout/overwrite حفظ شدند.
- [x] inventory اولیه Native ثبت شد: `2414` فایل، `126231293` بایت.
- [x] snapshot فایل‌های قابل‌نسخه‌گذاری ثبت شد: `114` فایل،
  `16534291` بایت، fingerprint فهرست SHA-256 برابر
  `3ce6f7d3e438e0c7549b05b40560990c9619256d6b73bf528b07d678b8096a22`.
- [x] sensitive-filename scan پاس شد: `0` مورد `.env`، keystore، PEM،
  signing/credential/secret file.
- [x] content secret scan پاس شد: `0` private key، AWS/GitHub/Google key،
  signing password یا client secret. ابزار `gitleaks` محلی نصب نبود.

فرمان بازتولید fingerprint:

```powershell
$root = (Get-Location).Path
$files = Get-ChildItem -Recurse -Force -File | Where-Object {
  $rel = $_.FullName.Substring($root.Length + 1)
  $rel -notmatch '(^|[\\/])(\.git|\.gradle|\.idea|\.kotlin|build)([\\/]|$)' -and
  $_.Name -ne 'local.properties'
}
$entries = @($files | Sort-Object FullName | ForEach-Object {
  $rel = $_.FullName.Substring($root.Length + 1).Replace('\', '/')
  $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
  "$hash  $rel"
})
```

### C — Git Baseline

- [x] nested repository ناخواسته وجود ندارد.
- [x] `.gitignore` فعلی و فایل‌های خروجی/محلی شناسایی شدند.
- [x] `.gitignore` امن برای Android/Gradle/IDE/signing/secrets تکمیل شد.
- [x] repository روی branch `main` ایجاد و baseline commit مستقل
  `b327d50` ثبت شد.
- [x] branch فاز `gov-01/repository-build-foundation` بدون rewrite کردن
  history ایجاد شد.

### D — Build Governance

- [x] Version Catalog موجود در `gradle/libs.versions.toml` شناسایی شد.
- [x] Version Catalog کامل و تنها منبع version/dependency/plugin مصوب شد.
- [x] included build به نام `build-logic` ایجاد شد.
- [x] convention plugin محدود به Android application defaults پیاده شد.
- [x] پیکربندی تکراری `app` بدون شروع معماری `FND-01` به convention منتقل شد.

### E — Flavor، Signing و R8

- [x] flavor `beta` با application ID دقیق
  `ir.coffevista.vista_native` تعریف شود.
- [x] flavor `production` با application ID دقیق
  `ir.coffevista.vista` تعریف شود.
- [x] API/environment source هر flavor بدون secret و قابل override تعریف شد.
- [x] release signing فقط از environment/Gradle secret injection دریافت می‌شود؛
  در نبود secret، artifact صریحاً unsigned باشد.
- [x] R8 و resource shrinking فقط برای release فعال شد و debug بدون تغییر ماند.
- [x] mapping/seeds/usage/configuration بررسی و حضور پنج کلاس بحرانی startup/auth
  در mapping، بدون keep rule حدسی، اثبات شد.

### F — CI، Artifact و Documentation

- [x] CI شامل wrapper validation، assemble، unit test، lint،
  dependency/security check، secret scan، `git diff --check` و artifact metadata شود.
- [x] screenshot smoke placeholder صریح و executable در CI ثبت شد؛ این gate
  fresh device evidence نیست.
- [x] Variant Matrix کامل شد.
- [x] build/signing/secret strategy و commandهای clean checkout مستند شدند.
- [x] artifact name، SHA-256، size، application ID، signing state و R8 metadata
  برای خروجی‌ها ثبت شود.

### G — Verification و Exit Gate

- [x] baseline پیش از تغییر پاس شد:
  `.\gradlew.bat --no-daemon test lint assembleDebug assembleRelease "-Pkotlin.incremental=false"`
  با `BUILD SUCCESSFUL` در 59 ثانیه.
- [x] buildهای `betaDebug`، `betaRelease`، `productionDebug` و
  `productionRelease` از clean state پاس شدند.
- [x] unit test، lint، dependency/security configuration و secret scan نهایی
  پاس شدند. اجرای CVE scan واقعی طبق Gate جدید مانع Local Governance نیست و
  با وضعیت `Open — CI activation required before protected merge/release`
  منتقل شد.
- [x] دو build مستقل برای variantهای خروجی با artifact identity قابل‌توضیح
  اجرا و reproducibility ارزیابی شد: دو release unsigned byte-for-byte یکسان
  بودند؛ debug signed قابل‌ساخت بود ولی SHA آن میان دو build یکسان نبود.
- [x] `git diff --check`، working tree و تمام بندهای Local Gate ارزیابی و
  نتیجه `PASS` ثبت شد.

## 4. Variant Matrix

تا پایان پیاده‌سازی تکمیل می‌شود.

| Variant | Application ID | Build type | Debuggable | R8 | Shrink | Signing | API source | Artifact | Coexistence |
|---|---|---|---:|---:|---:|---|---|---|---|
| `betaDebug` | `ir.coffevista.vista_native` | debug | بله | خیر | خیر | debug keystore | Gradle/env/fallback | `app-beta-debug.apk` | با Production |
| `betaRelease` | `ir.coffevista.vista_native` | release | خیر | بله | بله | injected یا unsigned | Gradle/env/fallback | `app-beta-release-unsigned.apk` | با Production |
| `productionDebug` | `ir.coffevista.vista` | debug | بله | خیر | خیر | debug keystore | Gradle/env/fallback | `app-production-debug.apk` | با Beta؛ نه upgrade Flutter امضاشده |
| `productionRelease` | `ir.coffevista.vista` | release | خیر | بله | بله | injected یا unsigned | Gradle/env/fallback | `app-production-release-unsigned.apk` | با Beta؛ جایگزینی Flutter فقط با signing اصلی |

## 5. Risk Register انتقال‌یافته

هیچ‌کدام از موارد زیر در GOV-01 حل‌شده تلقی نمی‌شوند:

| Risk | Owner | وضعیت GOV-01 |
|---|---|---|
| Deep-link mismatch و cold replay | `FND-01` | Open / Transferred |
| Isar fail-open encryption path | `FND-01` Security | Open / Import Blocked |
| Dynamic WebView unrestricted policy | `FND-01` + `DISC-01` | Open / Transferred |
| نبود persistent Draft source | `MEDIA-01` | Open؛ policy=`zero-import` |
| نبود Download manifest | `MIG-01` | Open؛ policy=`scan + unmatched` |
| نبود Native feature instrumentation | Feature owners + `PERF-01` | Open |
| نبود ARM profile/release frame baseline | `PERF-01` | Open؛ مانع GOV-01 نیست |
| نبود maintenance/invalid-session fixture | `FND-01` | Open |

## 6. Decision Log

- Flutter فقط read-only بررسی می‌شود.
- package تولیدی Production باید `ir.coffevista.vista` باشد، ولی بدون signing
  واقعی production، artifact آن release-ready یا production-installable نامیده نمی‌شود.
- Beta با `ir.coffevista.vista_native` از Production جدا می‌ماند و به داده
  production دسترسی خودکار ندارد.
- در GOV-01 هیچ keep rule speculative برای Featureهای پیاده‌نشده اضافه نمی‌شود.
- `VISTA_NATIVE_MASTER_PLAN.md` از Source اعلام‌شده به‌صورت byte-for-byte کپی،
  با SHA-256 تطبیق و به مرجع اصلی tracked Repository تبدیل شد.
- نبود signing واقعی Production مانع Local Governance نیست و با وضعیت
  `Open — Required before production replacement` به `REL-02` منتقل می‌شود.
- نبود `NVD_API_KEY` محلی مانع Local Governance نیست؛ CI وجود Secret را اجباری
  و نبود آن را با Fail صریح متوقف می‌کند. اجرای موفق scan پیش از protected
  merge/release الزامی است.

## 7. Gate Re-evaluation

### Local Governance Gate

| معیار | نتیجه | شاهد |
|---|---|---|
| Git repository و baseline commit معتبر | **Pass** | `main@b327d50` و branch فاز با commit `fbbb7d0` |
| Working tree تمیز یا قابل‌توضیح | **Pass** | پیش از این Task تمیز؛ تغییر جاری فقط Master Plan و اسناد Gate است و در commit جدید ثبت می‌شود |
| Master Plan موجود و tracked | **Pass** | `VISTA_NATIVE_MASTER_PLAN.md`، تطبیق byte/SHA با Source |
| Variantها ساخته شوند | **Pass** | clean build موفق چهار variant |
| Unit test و lint | **Pass** | ۸۰ execution با صفر failure/error/skipped؛ lint با صفر error |
| R8 و resource shrinking | **Pass** | هر دو release، mapping/seeds/usage/configuration و critical-class check |
| Secret scan | **Pass** | صفر finding روی snapshot tracked |
| CI configuration معتبر | **Pass** | سه job، wrapper/clean/build/test/lint/R8/secret/artifact/CVE و task graph معتبر |
| Release signing فقط از Secret injection | **Pass** | قرارداد چهار secret؛ release بدون secret صریحاً unsigned |
| عدم اجرای Feature یا FND-01 | **Pass** | صفر تغییر در `app/src/**` |

**نتیجه:** `GOV-01 Complete — Local Governance Gate Passed`

### Activation Gates

| Gate | وضعیت | شرط خروج |
|---|---|---|
| Production Signing Gate | `Open — Required before production replacement` | تزریق signing اصلی و اثبات upgrade فقط در `REL-02` |
| Dependency CVE Execution Gate | `Open — CI activation required before protected merge/release` | اجرای موفق `dependencyCheckAggregate` با `NVD_API_KEY` در CI |
| Implementation Remediation Backlog | `Open` | رفع هر finding در فاز مالک؛ هیچ finding در GOV-01 حل‌شده فرض نمی‌شود |

عبور Local Governance به معنی Production-ready، release-ready یا
upgrade-compatible بودن artifact فعلی نیست.

## 8. Final Report

1. **Repository baseline:** repository جدید روی `main` ایجاد شد؛ ۱۱۶ فایل
   source/config/audit در baseline ثبت شدند. build/cache/local/IDE/signing
   material وارد Git نشد.
2. **Commit baseline:** `b327d50 — chore(gov-01): establish native baseline`.
   ادامه روی `gov-01/repository-build-foundation` انجام شد.
3. **فایل‌های Governance:** `build-logic/**`، تنظیمات Gradle و Version Catalog،
   dependency lock/verification metadata، CI، چهار اسکریپت verification،
   `.gitignore`/`.gitattributes` و سند build افزوده یا اصلاح شدند. executable
   source مربوط به Feature/Startup/Auth تغییر نکرد.
4. **Variant matrix:** جدول بخش 4 و
   `docs/build/GOV-01-BUILD-SIGNING-AND-VARIANTS.md` مرجع کامل‌اند. package
   واقعی APKها با `aapt2 dump badging` نیز تأیید شد.
5. **Signing/secrets:** debug از debug keystore؛ release فقط با تزریق چهار
   secret کامل. هر دو release فعلی unsigned هستند و production-installable یا
   release-ready اعلام نمی‌شوند.
6. **CI:** سه job برای repository safety، build/test/lint/R8/artifact و
   dependency security تعریف شد. wrapper validation، clean checkout،
   `git diff --check`، secret scan و screenshot placeholder نیز Gate هستند.
   workflow هنوز روی GitHub runner اجرا نشده است.
7. **Build/test/lint:** clean build چهار variant موفق؛ ۸۰ اجرای unit-test با
   صفر failure/error/skipped؛ lint با صفر error و ۲۳ warning؛ secret scan روی
   ۱۲۸ فایل tracked با صفر finding.
8. **R8/shrinking:** هر دو release با minify و resource shrinking ساخته شدند؛
   mapping/seeds/usage/configuration موجودند. `MainActivity`,
   `VistaApplication`, `StartupResolver`, `AuthViewModel` و
   `OkHttpAuthRemoteDataSource` در mapping باقی ماندند. keep rule حدسی افزوده
   نشد. کد فعلی JSON را دستی parse می‌کند؛ ریسک reflection/serialization
   Featureهای آینده همچنان باید در فاز مالک ارزیابی شود.
9. **Reproducibility:** build تمیز اول ۳m۱۷s و build تمیز دوم ۲m۰۲s موفق شد.
   SHA دو release unsigned در هر دو اجرا دقیقاً یکسان بود:
   Beta=`864324ac4a9869640eaf85d6f2ac954098d24ba4727de58be51dbf7428e15ce5`،
   Production=`52a5119ad955ae046bbcdc9b5552b272ca6733e8db6a319687d1dfc3a484a59b`.
   debug signed قابل‌بازتولید عملی بود ولی byte-for-byte یکسان نشد.
10. **Master Plan:** فایل اصلی از Source اعلام‌شده بدون تغییر محتوا کپی و با
    طول `22156` بایت و SHA-256 برابر Source ثبت شد.
11. **Activation blockerها:** Production signing با وضعیت
    `Open — Required before production replacement` و CVE execution با وضعیت
    `Open — CI activation required before protected merge/release` حفظ شدند.
    هیچ‌کدام مانع Local Governance Gate نیستند.
12. **Risk transfer:** هر هشت ریسک بخش 5 همچنان `Open/Transferred` هستند؛
    هیچ‌کدام حل‌شده علامت نخورد.
13. **نتیجه Gate:** `GOV-01 Complete — Local Governance Gate Passed`.
    Production Signing Gate، Dependency CVE Execution Gate و Implementation
    Remediation Backlog جداگانه باز می‌مانند.
14. **مرز فاز:** `FND-01`، Feature، Backend، Navigation، Room، DataStore،
    WorkManager، Hilt، Migration Bridge و تغییر رفتار Auth/Startup اجرا نشد.

## 9. P03 Revalidation — 2026-08-06

**وضعیت:** `Partial — activation gates open`؛ Local Build/Release Governance روی `main@bf2b69e` بازتأیید شد، اما production replacement هنوز مجاز نیست.

| معیار | نتیجه | شاهد جاری |
|---|---|---|
| Package/config | Pass | `app/build.gradle.kts:28-45,66-76`؛ namespace=`ir.coffevista.vista_native`، beta=`ir.coffevista.vista_native`، production=`ir.coffevista.vista`، baseline منتشرشده Flutter=`2.6.2+4049` و Native default=`2.6.3+4050`؛ override فقط از Gradle/env و `versionCode > 4049` الزام شده است |
| SDK | Partial / runtime blocked | `build-logic/src/main/kotlin/VistaAndroidApplicationPlugin.kt:16-20`؛ compile/target=`36` و min=`24`. AVD رسمی API 24/x86_64 به ADB رسید ولی shell/package manager در سه launch تمیز غیرپاسخگو شد؛ fallback x86 download نشد. دستگاه متصل OnePlus API 30/arm64 است، پس API 24 هنوز Pass نیست |
| Signing/update | Blocked | `app/build.gradle.kts:18-32,48-60,90-96`؛ debug signed و production بدون چهار secret صریحاً unsigned است. SHA-256 گواهی APK منتشرشده Flutter=`45ED8D0152484EBA6805A575A1E67778F0AD8E5B5F970DFE8BD07EDC141684AE`؛ material اصلی در دسترس نیست و مالک `REL-02` باید lineage و upgrade را روی دستگاه clone اثبات کند |
| Reproducible gate | Pass | `.\gradlew.bat --no-daemon lint test :app:assembleBetaDebug :app:assembleProductionRelease "-Pvista.isolatedBuildDir=true" "-Pvista.isolatedBuildOutput=p03-20260806c" "-Pkotlin.incremental=false"` بدون `--write-locks`: ۱۲۶۱ task، `BUILD SUCCESSFUL` |
| Tests/lint/R8 | Pass | ۵۹ XML suite، ۳۵۴ test، failure/error/skipped=`0`; lint: ۱۲ report، error=`0`، warning=`47`، hint=`2`; `app/outputs/mapping/productionRelease/mapping.txt` موجود |
| Artifact provenance | Pass local | betaDebug: `17488221` bytes، SHA-256=`1F4BA657234FC70A06CDC7E9A1E005413D3CAFD339067C3E48089F24319356C7`; productionRelease unsigned: `5640807` bytes، SHA-256=`C667AEA6DE6D07352131ABED580939F99E9427F4B6CF410C95D4A5ACBDD94F3C`; هر دو به HEAD/variant بالا متصل‌اند |
| CI | Defined / execution unverified | `.github/workflows/android-ci.yml:13-90` wrapper، secret، diff، build/test/lint/R8/artifact و CVE را تعریف می‌کند؛ اجرای runner جاری اثبات نشده و CVE نیازمند `NVD_API_KEY` است |
| Secrets | Pass local | real-account credential فقط در محیط فرایند runtime استفاده شد؛ scan رسمی روی ۹۵۹ tracked/untracked file و literal scan نتیجهٔ صفر داشت |
| Architecture boundary | Pass | dependency مستقیم Gradle از `feature/feed`، `feature/profile`، `feature/search` و `feature/shell` به feature دیگر صفر شد؛ قرارداد session در `core/model/.../session/AuthenticationState.kt` و composition UI در `app/.../AppNavGraph.kt` مالکیت صریح دارد. `scripts/verify-module-boundaries.ps1` و test/compile affected modules پاس شدند |

Revalidation جاری پس از boundary/P04 foundation: `lint test :app:assembleBetaDebug` بدون lock rewrite در output ایزوله پاس شد؛ ۵۹ suite و ۳۵۴ test با failure/error/skipped=`0`. instrumentation `:app:connectedBetaDebugAndroidTest` روی OnePlus N100 API 30/arm64 پس از رفع compatibility خود تست، `23/23` پاس شد. APK betaDebug جاری 17,457,742 bytes و SHA-256=`FEA0753FD6420E7DD1C9BBEEF62DC15065F14B40F685C9EC72DD9F9CB40CC572` است.

`SearchScreens.kt` audit: تغییر `AnimatedContent` فقط target-state `(phase, selectedTab)` را به `SearchBody` منتقل می‌کند؛ طبقه‌بندی=`Evidence-driven functional fix`، نه تغییر UI نامرتبط. WIP حفظ شد و با boundary/full gate بالا compile/test شد.

Runtime Login smoke: APK `betaDebug` روی emulator API 33/x86_64 نصب شد؛ یک تلاش با حساب واقعی به session معتبر و authenticated Shell/Feed رسید، OTP/2FA فعال نشد و credential/token/response حساس ذخیره یا log نشد. این شاهد فقط Login boundary است و تکمیل کل Auth یا سایر journeyها را ثابت نمی‌کند.

روش acceptance آینده `REL-02`: چهار signing secret فقط در محیط امن تزریق شوند؛ `productionRelease` با `versionCode` بزرگ‌تر از نسخه نصب‌شده ساخته شود؛ fingerprint خروجی با baseline بالا برابر باشد؛ `adb install -r` روی clone دارای Flutter و داده واقعی انجام و session/storage migration تأیید شود. rollback فقط با همان certificate و `versionCode` بالاتر مجاز است و هر schema migration باید backward-safe یا restore-tested باشد.

Activation gateهای باز: production signing/certificate lineage و upgrade (`REL-02`)، اجرای موفق CVE در CI و runtime API 24. این external blockerها بدون material/runner واقعی Pass نمی‌شوند؛ شروع محدود P04 به foundation پایدار مجاز است، اما P03 تا بسته‌شدن gateهای رسمی `Partial` می‌ماند.
