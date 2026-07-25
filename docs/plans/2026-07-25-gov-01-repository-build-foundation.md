# GOV-01 — Repository و Build Foundation

**نوع:** Build Governance
**تاریخ شروع:** 2026-07-25
**وضعیت Gate:** `BLOCKED — NOT COMPLETE`
**پیشرفت کل:** `[██████████] 95%` — 38 از 40 تسک تکمیل شده

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
| اسناد و scope | `[████████░░] 83%` — 5/6 | Master Plan مقصد در workspace غایب است؛ منبع بالادستی خوانده شد |
| baseline و safety | `[██████████] 100%` — 8/8 | تکمیل |
| Git baseline | `[██████████] 100%` — 5/5 | baseline مستقل و branch فاز ثبت شد |
| Build governance | `[██████████] 100%` — 5/5 | تکمیل |
| flavor/signing/R8 | `[██████████] 100%` — 6/6 | تکمیل |
| CI و مستندات | `[██████████] 100%` — 5/5 | config و task graph محلی معتبر؛ workflow هنوز روی GitHub اجرا نشده |
| verification و Gate | `[████████░░] 80%` — 4/5 | CVE scan واقعی به NVD API key/مجوز شبکه نیاز دارد |

## 3. Task checklist

### A — اسناد، Scope و Risk Transfer

- [x] متن مأموریت پیوست کامل خوانده شد.
- [ ] `VISTA_NATIVE_MASTER_PLAN.md` کامل خوانده شود. **BLOCKED:** فایل در
  `E:\vista_native` وجود نداشت.
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
- [ ] unit test، lint، dependency/security و secret scan نهایی پاس شوند.
  Unit/lint/secret پاس‌اند؛ OWASP CVE scan به‌علت نبود `NVD_API_KEY` و رد
  network authorization پاس نشده است.
- [x] دو build مستقل برای variantهای خروجی با artifact identity قابل‌توضیح
  اجرا و reproducibility ارزیابی شد: دو release unsigned byte-for-byte یکسان
  بودند؛ debug signed قابل‌ساخت بود ولی SHA آن میان دو build یکسان نبود.
- [x] `git diff --check`، working tree و تمام بندهای Gate ارزیابی و نتیجه
  `FAIL/BLOCKED` ثبت شد.

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
- غیبت `VISTA_NATIVE_MASTER_PLAN.md` در Gate نهایی به‌عنوان documentation
  blocker دوباره ارزیابی می‌شود؛ منبع `Downloads\PLAN.md` جایگزین silently
  فرض نشده است.

## 7. Final Report

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
10. **Blockerها:** فایل `VISTA_NATIVE_MASTER_PLAN.md` در workspace وجود
    نداشت؛ فقط منبع بالادستی کامل خوانده شد. CVE scan شبکه‌ای بدون تأیید ارسال
    metadata رد شد و اجرای ظاهراً offline نیز NVD را صدا زد و پس از ۱۷ retry
    کنترل‌شده متوقف شد. CI برای جلوگیری از hang، `NVD_API_KEY` را اجباری می‌کند.
    production signing واقعی نیز در دسترس نیست و تولید/حدس زده نشد.
11. **Risk transfer:** هر هشت ریسک بخش 5 همچنان `Open/Transferred` هستند؛
    هیچ‌کدام حل‌شده علامت نخورد.
12. **نتیجه Gate:** `GOV-01 BLOCKED — NOT COMPLETE`. شرط build، variant،
    repository، R8، test، lint، secret، CI config و docs پاس است؛ شرط
    dependency vulnerability scan پاس نیست. طبق مأموریت، فاز Complete اعلام
    نمی‌شود.
13. **مرز فاز:** `FND-01`، Feature، Backend، Navigation، Room، DataStore،
    WorkManager، Hilt، Migration Bridge و تغییر رفتار Auth/Startup اجرا نشد.
