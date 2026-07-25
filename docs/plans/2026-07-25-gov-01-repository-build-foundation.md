# GOV-01 — Repository و Build Foundation

**نوع:** Build Governance
**تاریخ شروع:** 2026-07-25
**وضعیت Gate:** `IN PROGRESS`
**پیشرفت کل:** `[████░░░░░░] 38%` — 15 از 40 تسک تکمیل شده

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
| اسناد و scope | `[█████░░░░░] 50%` — 3/6 | Master Plan مقصد در workspace غایب است؛ منبع بالادستی خوانده شد |
| baseline و safety | `[██████████] 100%` — 8/8 | تکمیل |
| Git baseline | `[████░░░░░░] 40%` — 2/5 | parent/nested check تکمیل؛ init/commit باقی است |
| Build governance | `[██░░░░░░░░] 20%` — 1/5 | Version Catalog موجود و نیازمند تکمیل است |
| flavor/signing/R8 | `[░░░░░░░░░░] 0%` — 0/6 | شروع نشده |
| CI و مستندات | `[░░░░░░░░░░] 0%` — 0/5 | شروع نشده |
| verification و Gate | `[██░░░░░░░░] 20%` — 1/5 | baseline build پاس؛ verification نهایی باقی است |

## 3. Task checklist

### A — اسناد، Scope و Risk Transfer

- [x] متن مأموریت پیوست کامل خوانده شد.
- [ ] `VISTA_NATIVE_MASTER_PLAN.md` کامل خوانده شود. **BLOCKED:** فایل در
  `E:\vista_native` وجود نداشت.
- [x] منبع بالادستی Master Plan در
  `C:\Users\MriT.DESKTOP-UK7OADT\Downloads\PLAN.md` کامل و read-only خوانده شد.
- [x] `docs/plans/2026-07-25-aud-01-parity-ledger.md` کامل خوانده شد.
- [x] `docs/audits/2026-07-25-aud-01-parity-ledger.md` کامل خوانده شد.
- [ ] هشت ریسک منتقل‌شده در Risk Register نهایی با owner و وضعیت `Open`
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
- [ ] `.gitignore` امن برای Android/Gradle/IDE/signing/secrets تکمیل شود.
- [ ] repository روی branch `main` ایجاد و baseline commit مستقل ثبت شود.
- [ ] branch فاز GOV-01 بدون rewrite کردن history ایجاد شود.

### D — Build Governance

- [x] Version Catalog موجود در `gradle/libs.versions.toml` شناسایی شد.
- [ ] Version Catalog کامل و تنها منبع version/dependency/plugin مصوب شود.
- [ ] included build به نام `build-logic` ایجاد شود.
- [ ] convention plugin محدود به Android application defaults پیاده شود.
- [ ] پیکربندی تکراری `app` بدون شروع معماری `FND-01` به convention منتقل شود.

### E — Flavor، Signing و R8

- [ ] flavor `beta` با application ID دقیق
  `ir.coffevista.vista_native` تعریف شود.
- [ ] flavor `production` با application ID دقیق
  `ir.coffevista.vista` تعریف شود.
- [ ] API/environment source هر flavor بدون secret و قابل override تعریف شود.
- [ ] release signing فقط از environment/Gradle secret injection دریافت شود؛
  در نبود secret، artifact صریحاً unsigned باشد.
- [ ] R8 و resource shrinking فقط برای release فعال شود و debug بدون تغییر بماند.
- [ ] mapping/seeds/usage بررسی و حفظ startup/auth بدون keep rule حدسی اثبات شود.

### F — CI، Artifact و Documentation

- [ ] CI شامل wrapper validation، assemble، unit test، lint،
  dependency/security check، secret scan، `git diff --check` و artifact metadata شود.
- [ ] screenshot smoke placeholder یا executable smoke موجود در CI ثبت شود.
- [ ] Variant Matrix کامل شود.
- [ ] build/signing/secret strategy و commandهای clean checkout مستند شوند.
- [ ] artifact name، SHA-256، size، application ID، signing state و R8 metadata
  برای خروجی‌ها ثبت شود.

### G — Verification و Exit Gate

- [x] baseline پیش از تغییر پاس شد:
  `.\gradlew.bat --no-daemon test lint assembleDebug assembleRelease "-Pkotlin.incremental=false"`
  با `BUILD SUCCESSFUL` در 59 ثانیه.
- [ ] buildهای Debug/Beta/Production از clean state پاس شوند.
- [ ] unit test، lint، dependency/security و secret scan نهایی پاس شوند.
- [ ] دو build مستقل برای variantهای خروجی با artifact identity قابل‌توضیح
  اجرا و reproducibility ارزیابی شود.
- [ ] `git diff --check`، working tree و تمام بندهای Gate ارزیابی و نتیجه
  `PASS/FAIL` ثبت شود.

## 4. Variant Matrix

تا پایان پیاده‌سازی تکمیل می‌شود.

| Variant | Application ID | Build type | Debuggable | R8 | Shrink | Signing | API source | Artifact | Coexistence |
|---|---|---|---:|---:|---:|---|---|---|---|
| `betaDebug` | `ir.coffevista.vista_native` | debug | TBD | TBD | TBD | TBD | TBD | TBD | با Production |
| `betaRelease` | `ir.coffevista.vista_native` | release | TBD | TBD | TBD | TBD | TBD | TBD | با Production |
| `productionDebug` | `ir.coffevista.vista` | debug | TBD | TBD | TBD | TBD | TBD | TBD | با Beta |
| `productionRelease` | `ir.coffevista.vista` | release | TBD | TBD | TBD | TBD | TBD | TBD | با Beta |

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

پس از پایان کار، نتایج repository baseline، commitها، فایل‌های تغییرکرده،
variant matrix، signing strategy، CI، build/test/lint، R8، blockerها،
risk transfer، نتیجه Gate و تأیید عدم اجرای `FND-01` در همین بخش ثبت می‌شوند.
