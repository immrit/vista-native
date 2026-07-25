# ممیزی جامع و دفتر تطابق ویستا — AUD-01

**Type:** refactor
**Status:** done
**Progress:** `[██████████] 100%` — 55/55 مورد تکمیل شده؛
`Audit Closure Gate` پاس شده و `Implementation Remediation Backlog` باز است.

## Problem / Goal

هدف این فاز، ممیزی مستند و قابل‌بازتولید تمام sourceهای First-party در
`E:\vista` و `E:\vista_native` و ساخت مرجع قطعی Parity برای مهاجرت Android
است. این فاز فقط Audit و استخراج شواهد است؛ هیچ قابلیت، معماری، dependency،
screen، repository، database، network stack یا migration bridge پیاده‌سازی
نمی‌شود.

## Root cause / Design

Master Plan فعلی چند آمار و تصمیم معماری سطح‌بالا دارد، اما برای عبور از Gate
فاز `AUD-01` به ledgerهای کامل با شواهد repository-relative از نوع
`path:line` نیاز است. Gate ممیزی، کشف و اثبات finding، طبقه‌بندی ریسک،
تعیین فاز مالک و تعریف Acceptance/Exit Gate را می‌سنجد؛ رفع executable defect
در فاز مالک سنجیده می‌شود. ممیزی هر قابلیت Flutter باید زنجیره واقعی
`UI → State → Domain → Repository/Service → Storage/Network/Platform → Backend Contract → Result`
را دنبال کند و صرف وجود نام فایل یا کلاس را مدرک کامل بودن نداند.

## Phases

### Phase 1 — آماده‌سازی و خط مبنا

- [x] متن مأموریت پیوست کامل و خط‌به‌خط خوانده شد.
- [x] `C:\Users\MriT.DESKTOP-UK7OADT\Downloads\PLAN.md` کامل خوانده شد.
- [x] Branch، commit و Git status مخزن Native بررسی شد: مخزن Git معتبر نیست.
- [x] Branch، commit و Git status مخزن Flutter ثبت شد؛ تغییرات محلی موجود و دست‌نخورده می‌مانند.
- [x] نسخه ابزارها و فرمان‌های قابل‌بازتولید ممیزی ثبت شود.
- [x] قواعد include/exclude و فهرست Generated/cache/build/vendorهای مستثنا ثبت شود.
- [x] آمار فایل‌ها و خطوط First-party هر دو Repository با فرمان قابل‌بازتولید استخراج شود.
- [x] ساختار پوشه‌ها، تنظیمات، asset declarationها، localizationها، testها، scriptها و Android integrationها پوشش داده شود.

### Phase 2 — Repository coverage و Feature parity

- [x] Repository coverage ledger برای Flutter و Native کامل شود.
- [x] پنج تب اصلی Flutter و تمام مسیرهای قابل‌دسترسی آن‌ها شناسایی شوند.
- [x] همه Featureهای کشف‌شده Stable Feature ID دریافت کنند.
- [x] برای هر Feature زنجیره کامل لایه‌ها و نتیجه قابل‌مشاهده کاربر ثبت شود.
- [x] Business rule، state owner و dependencyهای API/realtime هر Feature ثبت شود.
- [x] Storage/cache، permission/platform، offline/retry و security هر Feature ثبت شود.
- [x] وضعیت Native و طبقه‌بندی Retain/Refactor/Replace/Merge/Remove هر Feature ثبت شود.
- [x] مقصد Native، فاز مهاجرت، dependency، blocker، test و acceptance criteria هر Feature ثبت شود.

### Phase 3 — UI، Navigation و Entry Pointها

- [x] inventory تمام Screenها، routeها، dialogها، sheetها و overlayها کامل شود.
- [x] برای هر UI entry حالت‌های loading/empty/error/offline/permission-denied ثبت شود.
- [x] RTL، accessibility و screenshot parity requirement هر UI entry ثبت شود.
- [x] Navigation و back-stack matrix، شامل entry/exit و back behavior، کامل شود.
- [x] Deep-link matrix با dispatcher، guard، مقصد و failure behavior کامل شود.
- [x] Notification-action matrix با payload، action، مقصد و guard کامل شود.

### Phase 4 — Contract، Data و Platform

- [x] API contract inventory شامل method/path/request/response/auth/error/consumer کامل شود.
- [x] Realtime/WebSocket event inventory شامل producer/consumer/order/retry/idempotency کامل شود.
- [x] Local-storage و cache inventory شامل schema/owner/lifecycle/sensitivity کامل شود.
- [x] Background task، retry، upload، sync و pending-operation inventory کامل شود.
- [x] Permission و platform-integration inventory، manifest و runtime behavior کامل شود.
- [x] Security-sensitive flow inventory برای auth/session/TLS/E2EE/payment/WebView/logging کامل شود.
- [x] Migration-source inventory برای Session، E2EE، Draft، Pending Operation، Download و Preferences کامل شود.

### Phase 5 — Native classification و Baseline

- [x] تمام First-party کد Native به Retain/Refactor/Replace/Merge/Remove طبقه‌بندی شود.
- [x] برای هر طبقه‌بندی Native دلیل و شاهد دقیق `path:line` ثبت شود.
- [x] baseline تشخیصی Emulator/debug و محدودیت آن ثبت و baseline الزامی ARM profile/release به `PERF-01` منتقل شود.
- [x] Native baseline status شامل build/test/lint/runtime و محدودیت شواهد ثبت شود.
- [x] testها یا buildهای غیرمخرب لازم اجرا و نتیجه بدون اصلاح کد ثبت شود.

### Phase 6 — Unknowns، Verification و Gate

- [x] تمام findingهای مهم در دسته شواهد تعیین‌شده طبقه‌بندی شوند.
- [x] Unknownها، assumptionها، blockerها و تصمیم‌های نیازمند تأیید کاربر ثبت شوند.
- [x] همه ادعاهای مهم به شاهد repository-relative از نوع `path:line` متصل شوند.
- [x] آمار Feature/Screen/Route/Endpoint/Integration نهایی محاسبه و cross-check شود.
- [x] Gate کامل `AUD-01` بندبه‌بند ارزیابی و نتیجه Pass/Fail ثبت شود.
- [x] گزارش اولیه هشت‌بندی تهیه و توقف پیش از `GOV-01` و `FND-01` تأیید شود.

### Phase 7 — تکمیل Audit Closure برای AUD-01

- [x] پرامپت ادامه‌ی `AUD-01` و دو سند جاری دوباره کامل خوانده و محدوده‌ی audit-only تثبیت شد.
- [x] Flutter runtime traversal با state/guard/back-stack واقعی ثبت یا blocker دقیق آن اثبات شود.
- [x] Flutter screenshot baseline در stateهای لازم جمع‌آوری یا blocker دقیق آن اثبات شود.
- [x] Flutter performance baseline با عدد runtime جمع‌آوری یا blocker دقیق آن اثبات شود.
- [x] Native runtime/device baseline جمع‌آوری یا blocker دقیق آن اثبات شود.
- [x] Deep-link route completeness به‌صورت static و runtime بسته شود.
- [x] Cold-start deep-link coverage برای manifest/dispatcher/guard بسته شود.
- [x] Draft migration source با source-to-target matrix و نتیجه‌ی definitive بسته شود.
- [x] Download manifest migration source و تمایز user-owned/cache بسته شود.
- [x] Preferences migration mapping با owner، key و target بسته شود.
- [x] Dynamic/external URL inventory با owner، policy و migration disposition بسته شود.
- [x] Native instrumentation/runtime evidence جمع‌آوری یا blocker دقیق آن اثبات شود.
- [x] ریسک fallback رمزنگاری Isar و testهای قبل از مهاجرت تعیین تکلیف شود.
- [x] `Audit Closure Gate` با evidence/severity/owner/remediation/acceptance/exit برای تمام findingها پاس شود.
- [x] Ledger، گزارش نهایی یازده‌بندی و شرایط ورود کنترل‌شده به `GOV-01`/`FND-01` به‌روزرسانی شود.

## Decisions & Notes

- منبع اصلی نقشه‌راه فعلی:
  `C:\Users\MriT.DESKTOP-UK7OADT\Downloads\PLAN.md`
- سند خروجی اصلی مهاجرت طبق Master Plan:
  `E:\vista_native\VISTA_NATIVE_MASTER_PLAN.md`
- خط مبنای Native در شروع Audit: دستورهای `git branch --show-current`،
  `git rev-parse HEAD` و `git status --short` همگی با
  `fatal: not a git repository` پایان یافتند. طبق ممنوعیت فاز، repository یا
  baseline commit ایجاد نمی‌شود.
- خط مبنای Flutter در شروع Audit: branch برابر `main` و commit برابر
  `64994fa1dfa6f1b146cb9d132758f105670e4d71` بود. فایل‌های تغییرکرده یا
  untracked موجود متعلق به کاربر هستند و در این فاز overwrite، reset، checkout
  یا stash نمی‌شوند.
- اجرای `GOV-01`، `FND-01` و تمام فازهای بعدی در این Task خارج از محدوده بود.
  پس از Audit Closure، ورود به `GOV-01` مجاز است؛ ورود به `FND-01` فقط وقتی
  مجاز است که تمام Foundation blockerهای منتقل‌شده در Scope و Acceptance Gate
  آن وارد شوند.
- تغییر progress فقط بلافاصله پس از تکمیل و ثبت شواهد هر checkbox انجام می‌شود.
- خروجی اصلی Audit:
  `docs/audits/2026-07-25-aud-01-parity-ledger.md`
- Gate اولیه به‌علت یکی‌گرفتن Audit Closure با Implementation Remediation وارد
  dependency cycle شده بود: AUD اجازه‌ی تغییر executable source نداشت، اما
  Pass شدن آن به همان تغییرها وابسته شده بود. تعریف اصلاح‌شده فقط کامل بودن
  evidence، root-cause scope، severity، owner، remediation، acceptance test و
  exit gate را در `AUD-01` می‌سنجد.
- تمام findingهای باز مالک و معیار خروج دارند؛ بنابراین
  `AUD-01 Complete — Audit Closure Gate Passed`. رفع واقعی آن‌ها همچنان انجام
  نشده و وضعیت مستقل `Implementation Remediation Backlog Open` است.
- AVD مرجع Gate closure: `Medium_Phone_2`، Android API 33، رزولوشن
  `1080×2400`، معماری `x86_64`، اجراشده با `-read-only` و
  `-no-snapshot-save`. APKهای debug هر دو app روی همین AVD نصب شدند.
- Flutter runtime پنج tab، onboarding/auth/location prompt، back از Profile
  به Feed، cold/warm deep link و Light/Dark/font-scale را پوشش داد. اعداد
  startup و memory جمع شد؛ `dumpsys gfxinfo` برای Flutter/Impeller صفر frame
  برگرداند، بنابراین profile/release DevTools trace هنوز blocker است.
- Native runtime onboarding/auth، authenticated placeholder موجود در snapshot،
  process restart، offline، rotation و Light/Dark/font-scale را پوشش داد.
  `connectedDebugAndroidTest` یک test template را با صفر failure پاس کرد؛
  maintenance و invalid-session به fixture کنترل‌شده نیاز دارند.
- Decisionهای قفل‌شده در گزارش Audit ثبت شده‌اند: Draft=`zero-import`؛
  Download legacy=`scan + unmatched` و بدون URL guessing؛ baseline فعلی صرفاً
  diagnostic؛ Isar قبل از import واقعی باید fail-closed شود؛ Deep Link پیش از
  Navigation Foundation؛ و screenshot/fixtureهای باقی‌مانده پیش از Verified
  شدن Feature مالک.
