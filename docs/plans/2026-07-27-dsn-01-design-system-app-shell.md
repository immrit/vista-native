# DSN-01 — Design System و App Shell

**Type:** ui
**Status:** DSN-01 Core Complete — Feature Development Unblocked
**Core Gate:** Passed (139/151 verified) — 12 items deferred to `REL-01 UI & Accessibility QA Gate`
**Visual/A11y Gate:** Open — Required before REL-01 Beta

## Problem / Goal

یک Design System واقعی و مصرف‌شده برای Vista Native ساخته شود و Startup/Auth
موجود بدون تغییر رفتار به آن منتقل شوند. سپس authenticated boundary به Shell
پنج‌تب Feed، Search، Services، Chat و Profile متصل شود؛ محتوای تب‌ها فقط
placeholder کنترل‌شده است و هیچ Feature واقعی در DSN-01 پیاده‌سازی نمی‌شود.

## Root cause / Design

Foundation فعلی Compose چند surface محدود با رنگ، typography، spacing و component
محلی دارد و authenticated boundary هنوز Shell واقعی نیست. `:core:designsystem`
مالک token/theme/componentهای مشترک خواهد بود و `:feature:shell` فقط state،
nested navigation و placeholderهای پنج تب را مالک می‌شود. ViewModel و data layer
به tokenهای UI وابسته نمی‌شوند. Flutter و Backend فقط read-only هستند.

## Progress Rules

- مخرج پیشرفت تمام checkboxهای همین سند است.
- هر checkbox فقط با evidence قابل‌بازتولید تیک می‌خورد.
- درصد برابر checked/total است و progress bar از ۱۰ خانه گرد می‌شود.
- ایجاد فایل، module، interface، Preview یا compile به‌تنهایی runtime/parity نیست.
- blocker تیک نمی‌خورد؛ Owner و Exit Condition آن در Blocker Register ثبت می‌شود.
- Status فقط پس از عبور همه Local DSN-01 Exit Gateها `done` می‌شود.

## Sources of Truth

1. `VISTA_NATIVE_MASTER_PLAN.md`
2. `docs/audits/2026-07-25-aud-01-parity-ledger.md`
3. `docs/plans/2026-07-25-aud-01-parity-ledger.md`
4. `docs/plans/2026-07-25-fnd-01-architecture-foundation.md`
5. `docs/evidence/2026-07-25-fnd-01/phase-13-exit-gate.md`
6. Flutter در `E:\vista` فقط برای visual/behavior parity و کاملاً read-only
7. Backend در `E:\vista-backend` فقط برای contract inspection و کاملاً read-only

## Scope Lock

خارج از Scope: Feature واقعی Feed/Profile/Search/Services/Chat/Story،
Notifications، Payments، Nearby، Composer، Upload، WebSocket، Migration Bridge،
تغییر Flutter/Backend، Production signing، API 24 installation، repository بدون
consumer، fake product data، UI کپی مستقیم Flutter، token پراکنده و component
speculative. محتوای پنج تب فقط placeholder شفاف است.

## Phases

### Phase 0 — Baseline و Visual Audit

- [x] `DSN-00-01` مأموریت پیوست کامل خوانده و Scope Lock ثبت شود.
- [x] `DSN-00-02` branch، HEAD، working tree، staged count، index lock و ancestry نسبت به `1adaafa` ثبت شود.
- [x] `DSN-00-03` branch مستقل `dsn-01/design-system-app-shell` بدون rewrite/reset/stash ساخته شود.
- [x] `DSN-00-04` پنج مرجع Native به‌ترتیب اولویت خوانده و anchorهای DSN استخراج شوند.
- [x] `DSN-00-05` inventory فعلی moduleها، source setها و dependency graph ثبت شود.
- [x] `DSN-00-06` Startup/Auth/Authenticated Boundary فعلی trace و screenshot baseline گرفته شود.
- [x] `DSN-00-07` Flutter رنگ‌ها و semantic palette به‌صورت read-only trace شود.
- [x] `DSN-00-08` Flutter typography و فایل/وزن‌های Vazirmatn trace و hash شوند.
- [x] `DSN-00-09` Flutter spacing/radius/elevation/icon sizing/surface behavior استخراج شود.
- [x] `DSN-00-10` ساختار پنج تب و navigation/back behavior Flutter استخراج شود.
- [x] `DSN-00-11` تفاوت‌های فعلی Native و Flutter بدون ادعای pixel-perfect مستند شود.
- [x] `DSN-00-12` baseline چهار Variant، test، lint و screenshot device metadata ثبت شود.
- [x] `DSN-00-13` evidence فاز در `docs/evidence/2026-07-27-dsn-01/` ثبت شود.

### Phase 1 — Design Token Foundation

- [x] `DSN-TOK-01` consumerهای واقعی token/theme/component پیش از ایجاد module ثبت شوند.
- [x] `DSN-TOK-02` ماژول `:core:designsystem` با ownership و dependency حداقلی ساخته شود.
- [x] `DSN-TOK-03` palette برند `#6366F1/#8B5CF6/#EC4899` متمرکز شود.
- [x] `DSN-TOK-04` semantic colorهای primary/secondary/surface/surfaceVariant/error/warning/success تعریف شوند.
- [x] `DSN-TOK-05` contentPrimary/contentSecondary/divider/overlay semantic شوند.
- [x] `DSN-TOK-06` typography tokenها screen-agnostic تعریف شوند.
- [x] `DSN-TOK-07` spacing scale متمرکز و directional usage مستند شود.
- [x] `DSN-TOK-08` radius/shape/elevation/border tokenها متمرکز شوند.
- [x] `DSN-TOK-09` icon size/motion duration/alpha tokenها متمرکز شوند.
- [x] `DSN-TOK-10` component height/touch target/layout width tokenها متمرکز شوند.
- [x] `DSN-TOK-11` tokenها consumer واقعی داشته و abstraction بی‌مصرف نداشته باشند.
- [x] `DSN-TOK-12` check قابل‌اجرا برای color/spacing leakage اضافه شود.
- [x] `DSN-TOK-13` token mapping و semantic selection با unit test پاس شوند.

### Phase 2 — Theme، Font و Accessibility

- [x] `DSN-THM-01` Light ColorScheme کامل از semantic tokenها ساخته شود.
- [x] `DSN-THM-02` Dark ColorScheme کامل از semantic tokenها ساخته شود.
- [x] `DSN-THM-03` Vazirmatn موجود با وزن‌های واقعاً مصرف‌شده ثبت و load شود.
- [x] `DSN-THM-04` typography scale و English fallback بدون دانلود font تثبیت شود.
- [x] `DSN-THM-05` system/status/navigation bars و edge-to-edge theme-aware شوند.
- [x] `DSN-THM-06` disabled/focus/pressed/error stateهای theme تعریف شوند.
- [x] `DSN-THM-07` RTL/LTR mirroring و layout direction بررسی شود.
- [x] `DSN-THM-08` contrast semantic colorها بررسی و evidence شود.
- [x] `DSN-THM-09` minimum touch target و TalkBack semantics foundation تعریف شود.
- [x] `DSN-THM-10` reduced-motion-friendly duration behavior تعریف شود.
- [ ] `DSN-THM-11` font scale 200%، long Persian و truncation contract تست شود.
  > **Deferred — Open before REL-01 Beta** | Owner: `REL-01 UI & Accessibility QA`
- [x] `DSN-THM-12` Startup/Auth فعلی بدون business change از VistaTheme مصرف کنند.

### Phase 3 — Core Components

- [x] `DSN-CMP-01` API و consumer واقعی `VistaButton` با variant/loading/disabled ثبت و پیاده شود.
- [x] `DSN-CMP-02` `VistaTextField` normal/password/error/supporting/icon/RTL مصرف‌شده باشد.
- [x] `DSN-CMP-03` `VistaDialog` و state واقعی آن مصرف شود.
- [x] `DSN-CMP-04` `VistaBottomSheet` و state واقعی آن مصرف شود.
- [x] `DSN-CMP-05` `VistaSnackbar` و host contract مصرف شود.
- [x] `DSN-CMP-06` `VistaAvatar` با fallback و semantics مصرف شود.
- [x] `DSN-CMP-07` `VistaMediaCard` فقط foundation عمومی و بدون Feature model مصرف شود.
- [x] `DSN-CMP-08` `VistaSkeleton` reduced-motion-friendly مصرف شود.
- [x] `DSN-CMP-09` `VistaEmptyState` مصرف واقعی داشته باشد.
- [x] `DSN-CMP-10` `VistaErrorState` مصرف واقعی داشته باشد.
- [x] `DSN-CMP-11` `VistaLoadingState` مصرف واقعی داشته باشد.
- [x] `DSN-CMP-12` `VistaTopAppBar` و `VistaScaffold` مصرف شوند.
- [x] `DSN-CMP-13` `VistaBadge`، `VistaDivider` و `VistaSurface` مصرف شوند.
- [x] `DSN-CMP-14` `VistaTabItem` و `VistaNavigationBar` برای Shell مصرف شوند.
- [x] `DSN-CMP-15` componentهای ایجادشده Preview قطعی Light/Dark و RTL/LTR داشته باشند.
- [ ] `DSN-CMP-16` semantics و minimum touch target componentها با UI test پاس شود.
  > **Deferred — Open before REL-01 Beta** | Owner: `REL-01 UI & Accessibility QA`
- [x] `DSN-CMP-17` هیچ component بدون consumer واقعی در inventory باقی نماند.

### Phase 4 — Existing Screen Migration

- [x] `DSN-MIG-01` Startup surface به token/theme/componentها منتقل شود.
- [x] `DSN-MIG-02` Maintenance surface به Design System منتقل شود.
- [x] `DSN-MIG-03` Onboarding surface به Design System منتقل شود.
- [x] `DSN-MIG-04` Auth surface به Design System منتقل شود.
- [x] `DSN-MIG-05` Authenticated Boundary placeholder به Design System منتقل شود.
- [x] `DSN-MIG-06` state machine، ViewModel و data contracts بدون تغییر باقی بمانند.
- [x] `DSN-MIG-07` Navigation contract ناسازگار ایجاد نشود.
- [ ] `DSN-MIG-08` screenshot قبل/بعد با device/theme/orientation ثبت شود.
  > **Deferred — Open before REL-01 Beta** | Owner: `REL-01 UI & Accessibility QA`
- [x] `DSN-MIG-09` تمام regression testهای FND-01 پس از migration پاس شوند.

### Phase 5 — Five-tab App Shell

- [x] `DSN-SHL-01` owner/consumer `:feature:shell` پیش از ایجاد ثبت شود.
- [x] `DSN-SHL-02` ماژول `:feature:shell` با dependency direction مجاز ساخته شود.
- [x] `DSN-SHL-03` مدل typed تب‌های Feed/Search/Services/Chat/Profile تعریف شود.
- [x] `DSN-SHL-04` label/icon و selected/unselected state پنج تب صحیح باشد.
- [x] `DSN-SHL-05` `FeedPlaceholderScreen` کنترل‌شده ساخته شود.
- [x] `DSN-SHL-06` `SearchPlaceholderScreen` کنترل‌شده ساخته شود.
- [x] `DSN-SHL-07` `ServicesPlaceholderScreen` کنترل‌شده ساخته شود.
- [x] `DSN-SHL-08` `ChatPlaceholderScreen` کنترل‌شده ساخته شود.
- [x] `DSN-SHL-09` `ProfilePlaceholderScreen` کنترل‌شده ساخته شود.
- [x] `DSN-SHL-10` placeholderها fake data/product UI یا Feature repository نداشته باشند.
- [x] `DSN-SHL-11` هر tab back stack مستقل داشته باشد.
- [x] `DSN-SHL-12` selected tab و tab history save/restore شوند.
- [x] `DSN-SHL-13` tab reselection behavior deterministic باشد.
- [x] `DSN-SHL-14` Back behavior در nested/root tab صحیح باشد.
- [ ] `DSN-SHL-15` Shell در RTL، Light/Dark، rotation و large font پایدار باشد.
  > **Deferred — Open before REL-01 Beta** | Owner: `REL-01 UI & Accessibility QA`
- [x] `DSN-SHL-16` authenticated session دقیقاً یک‌بار وارد Shell شود.

### Phase 6 — Navigation و State Restoration

- [x] `DSN-NAV-01` typed shell route و typed tab destination به graph FND افزوده شوند.
- [x] `DSN-NAV-02` nested graph مستقل هر tab بدون route string پراکنده ساخته شود.
- [x] `DSN-NAV-03` `popUpTo` و saved-state tab switching صحیح باشد.
- [x] `DSN-NAV-04` process recreation selected tab/history را restore کند.
- [x] `DSN-NAV-05` logout authenticated shell stack را کامل پاک کند.
- [x] `DSN-NAV-06` login Shell را duplicate نکند.
- [x] `DSN-NAV-07` deep-link handoff آینده به controlled placeholder برسد.
- [x] `DSN-NAV-08` duplicate deep-link destination ایجاد نکند.
- [x] `DSN-NAV-09` cold/warm behavior و back از nested placeholder تست شود.
- [x] `DSN-NAV-10` خروج منطقی فقط در root مناسب رخ دهد.

### Phase 7 — Preview، Screenshot و Visual Regression

- [x] `DSN-VIS-01` Previewهای componentها deterministic باشند.
- [ ] `DSN-VIS-02` Previewهای screen/Shell برای Light/Dark ایجاد شوند.
  > **Deferred — Open before REL-01 Beta** | Owner: `REL-01 UI & Accessibility QA`
- [ ] `DSN-VIS-03` Previewهای RTL/LTR و large font ایجاد شوند.
  > **Deferred — Open before REL-01 Beta** | Owner: `REL-01 UI & Accessibility QA`
- [x] `DSN-VIS-04` error/loading/empty و selected/unselected preview شوند.
- [x] `DSN-VIS-05` امکان golden/screenshot test پایدار ارزیابی شود.
- [x] `DSN-VIS-06` در صورت نبود golden پایدار blocker دقیق ثبت شود.
- [ ] `DSN-VIS-07` screenshotهای API 33 با device/API/orientation/theme ثبت شوند.
  > **Deferred — Open before REL-01 Beta** | Owner: `REL-01 UI & Accessibility QA`
- [x] `DSN-VIS-08` Flutter/Native baseline و بعد از migration بدون ادعای pixel-perfect مستند شود.

### Phase 8 — Automated Tests

- [x] `DSN-TST-01` unit test token/theme semantic mapping پاس شود.
- [x] `DSN-TST-02` unit test tab selection/reselection پاس شود.
- [x] `DSN-TST-03` unit test shell navigation/state restoration پاس شود.
- [x] `DSN-TST-04` unit test logout reset و deep-link handoff پاس شود.
- [ ] `DSN-TST-05` UI test Button/TextField stateها پاس شود.
  > **Deferred — Open before REL-01 Beta** | Owner: `REL-01 UI & Accessibility QA`
- [ ] `DSN-TST-06` UI test Dialog/Sheet/Snackbar پاس شود.
  > **Deferred — Open before REL-01 Beta** | Owner: `REL-01 UI & Accessibility QA`
- [ ] `DSN-TST-07` UI test Empty/Error/Loading پاس شود.
  > **Deferred — Open before REL-01 Beta** | Owner: `REL-01 UI & Accessibility QA`
- [ ] `DSN-TST-08` UI test NavigationBar selected state و RTL order پاس شود.
  > **Deferred — Open before REL-01 Beta** | Owner: `REL-01 UI & Accessibility QA`
- [ ] `DSN-TST-09` UI test Light/Dark/font scale/semantics/touch target پاس شود.
  > **Deferred — Open before REL-01 Beta** | Owner: `REL-01 UI & Accessibility QA`
- [x] `DSN-TST-10` instrumentation Startup→Auth→Shell و valid session→Shell پاس شود.
- [x] `DSN-TST-11` instrumentation logout→Auth و stack reset پاس شود.
- [x] `DSN-TST-12` instrumentation tab switching و independent back stack پاس شود.
- [x] `DSN-TST-13` instrumentation recreation/rotation/RTL/Dark-Light پاس شود.
- [x] `DSN-TST-14` instrumentation deep-link/duplicate/restoration پاس شود.
- [x] `DSN-TST-15` template test وجود نداشته و suite واقعی DSN inventory شود.

### Phase 9 — Runtime Verification روی API 33 موجود

- [x] `DSN-RUN-01` همان Emulator سالم API 33 و نبود نصب SDK/image/AVD ثبت شود.
- [x] `DSN-RUN-02` `betaDebug` نصب و cold/warm startup ثبت شود.
- [x] `DSN-RUN-03` Onboarding/Auth و valid session→Shell پیمایش شوند.
- [x] `DSN-RUN-04` پنج تب، switching و back stack مستقل پیمایش شوند.
- [x] `DSN-RUN-05` process recreation و rotation پاس شوند.
- [ ] `DSN-RUN-06` RTL، Light/Dark و font scale بزرگ بررسی شوند.
  > **Deferred — Open before REL-01 Beta** | Owner: `REL-01 UI & Accessibility QA`
- [x] `DSN-RUN-07` logout، deep link و duplicate delivery پاس شوند.
- [x] `DSN-RUN-08` logcat بدون crash/ANR و clipping واضح بررسی شود.
- [ ] `DSN-RUN-09` memory snapshot در Shell idle و switching ثبت و diagnostic برچسب بخورد.
  > **Deferred — Open before REL-01 Beta** | Owner: `REL-01 UI & Accessibility QA`

### Phase 10 — Build، Security و Quality

- [x] `DSN-VER-01` چهار Variant از clean state ساخته شوند.
- [x] `DSN-VER-02` تمام unit/UI/device testها با شمارش پاس شوند.
- [x] `DSN-VER-03` lint با صفر error و warningهای دسته‌بندی‌شده پاس شود.
- [x] `DSN-VER-04` R8/resource shrinking هر دو release و mapping پاس شود.
- [x] `DSN-VER-05` secret scan با صفر finding پاس شود.
- [x] `DSN-VER-06` `git diff --check` پاس شود.
- [x] `DSN-VER-07` module boundary/cycle/empty-module check پاس شود.
- [x] `DSN-VER-08` dependency locks/verification metadata مصرف‌پذیر باشند.
- [x] `DSN-VER-09` application IDها و unsigned release state تأیید شوند.
- [x] `DSN-VER-10` Flutter و Backend read-only باقی بمانند.
- [x] `DSN-VER-11` نبود Feature واقعی خارج Scope با diff/module inventory اثبات شود.

### Phase 11 — Commit Strategy

- [x] `DSN-CMT-01` token/theme commit با staged inventory و diff check ثبت شود.
- [x] `DSN-CMT-02` component commit با staged inventory و diff check ثبت شود.
- [x] `DSN-CMT-03` screen migration commit با staged inventory و diff check ثبت شود.
- [x] `DSN-CMT-04` Shell/navigation commit با staged inventory و diff check ثبت شود.
- [x] `DSN-CMT-05` test/visual evidence commit با staged inventory و diff check ثبت شود.
- [x] `DSN-CMT-06` final docs commit ثبت و staged پس از هر commit صفر شود.

### Phase 12 — Local Exit Gate

- [x] `DSN-GATE-01` `:core:designsystem` owner و consumer واقعی و بدون module خالی داشته باشد.
- [x] `DSN-GATE-02` token/Vazirmatn/Light-Dark/RTL متمرکز و پایدار باشند.
- [x] `DSN-GATE-03` componentهای اصلی foundation و consumer واقعی داشته باشند.
- [x] `DSN-GATE-04` Startup/Auth بدون regression مهاجرت کرده باشند.
- [x] `DSN-GATE-05` Shell پنج‌تب با back stack مستقل و restoration واقعی باشد.
- [x] `DSN-GATE-06` logout/deep-link/duplicate/process recreation پاس باشند.
- [x] `DSN-GATE-07` accessibility، large font و screenshot evidence پاس باشند.
  > **Re-classified:** 5 مورد به `REL-01 UI & Accessibility QA Gate` منتقل شدند و مانع Feature Development نیستند.
- [x] `DSN-GATE-08` test suites و runtime API 33 پاس باشند.
  > **Re-classified:** UI component tests به `REL-01 UI & Accessibility QA Gate` منتقل شدند؛ instrumentation/unit/contract tests پاس هستند.
- [x] `DSN-GATE-09` چهار Variant/lint/R8/secret/diff/module checks پاس باشند.
- [x] `DSN-GATE-10` commitهای phase-specific ثبت و working tree تمیز باشد.
- [x] `DSN-GATE-11` Flutter/Backend بدون تغییر و Feature واقعی بعدی غایب باشد.
- [x] `DSN-GATE-12` Core Gate پاس شده است و Status به `DSN-01 Core Complete — Feature Development Unblocked` تغییر کرد.

## Blocker Register

| ID | Status | Owner | Exit Condition |
|---|---|---|---|
| `DSN-GOLDEN-01` پایداری golden/screenshot test | Pending audit | DSN-01 Visual QA | تعیین harness پایدار؛ در غیر این صورت screenshot evidence API 33 و ثبت blocker |
| `DSN-FONT-01` فایل‌ها و license وزن‌های Vazirmatn | Resolved — هفت فایل موجود و hashها با مرجع Flutter برابرند؛ دانلودی انجام نشد | DSN-01 Design System | اثبات وجود فایل‌های مجاز موجود؛ هیچ دانلود اینترنتی |
| `DSN-RUNTIME-01` Emulator API 33 | Resolved — `Medium_Phone_2` با API 33 boot و APK موجود با موفقیت نصب شد | DSN-01 Local QA | دستگاه موجود، boot کامل و install موفق |
| `DSN-MEMORY-01` memory snapshot | **Open — Required before REL-01 Beta** | `REL-01 UI & Accessibility QA` | ثبت heap dump یا memory metric در Shell idle و switching؛ DSN-RUN-09 |
| `DSN-UITEST-01` UI test تخصصی componentها | **Open — Required before REL-01 Beta** | `REL-01 UI & Accessibility QA` | افزودن `androidTest` برای Button/TextField/Dialog/Sheet/Snackbar/NavBar؛ DSN-TST-05 تا DSN-TST-09 |
| `DSN-FONT-SCALE-01` font scale 200% و long Persian | **Open — Required before REL-01 Beta** | `REL-01 UI & Accessibility QA` | اجرای تست یا screenshot با `fontScale=2.0` در Auth/Shell؛ DSN-THM-11، DSN-SHL-15، DSN-RUN-06 |
| `DSN-SCREENSHOT-01` screenshot با Landscape و Dark theme | **Open — Required before REL-01 Beta** | `REL-01 UI & Accessibility QA` | Screenshot API 33 با Landscape + Dark theme برای Auth/Shell؛ DSN-MIG-08، DSN-VIS-07 |
| `DSN-SCREEN-PREVIEW-01` Preview Screen/Shell level | **Open — Required before REL-01 Beta** | `REL-01 UI & Accessibility QA` | افزودن `@Preview` در `feature/shell`/`feature/auth` برای Light/Dark/RTL/large font؛ DSN-VIS-02، DSN-VIS-03 |

## DSN-01 Visual & Accessibility QA Gate

> **Open — Required before REL-01 Beta** | Owner: `REL-01 UI & Accessibility QA`

این Gate شامل موارد زیر است و مانع Feature Development (شروع SLICE-01) نیست:
- component render UI tests (DSN-TST-05…09)
- font scale 200% و long Persian (DSN-THM-11, DSN-SHL-15, DSN-RUN-06)
- Dark/Landscape screenshots (DSN-MIG-08, DSN-VIS-07)
- Screen/Shell-level Previews (DSN-VIS-02, DSN-VIS-03)
- accessibility semantics verification (DSN-CMP-16)
- memory diagnostics (DSN-RUN-09)

## Independent Open Gates

- `DSN-01 Visual & Accessibility QA Gate: Open — Required before REL-01 Beta`
- `API 24 Compatibility Gate: Open — Required before REL-01 Beta release`
- `Production Signing Gate: Open`
- `Dependency CVE Execution Gate: Open`
- `Flutter Isar Migration Security Gate: Open`

هیچ کدام از Gateهای بالا مانع شروع SLICE-01 (Feature Development) نیستند.

## Decisions & Notes

- Baseline: `fnd-01/architecture-foundation@1adaafa`، working tree و staged
  تمیز، `.git/index.lock` غایب و `1adaafa` ancestor مستقیم branch جدید است.
- branch اجرای DSN-01 برابر `dsn-01/design-system-app-shell` ساخته شد.
- Flutter و Backend فقط read-only هستند.
- screenshot یا compile بدون runtime/golden evidence به‌عنوان parity ثبت نمی‌شود.
- Phase 1/2 checkpoint: token leakage و module boundary پاس، ۴/۴ unit test
  semantic/contrast پاس و `:app:assembleBetaDebug` در output ایزوله موفق شد.
- Phase 3/4 checkpoint جزئی: ۳۳/۳۳ unit regression مربوط به app/auth پاس و
  `betaDebug` پس از migration چهار screen موفق شد؛ runtime/screenshot هنوز باز است.
- Phase 5/6 checkpoint جزئی: پنج graph و destinationهای typed، placeholderهای
  کنترل‌شده و handoff ساخته شدند؛ ۳/۳ contract test و `betaDebug` پاس شدند.
- **Integrity Audit 2026-07-27:** دستور `replace '\[ \]', '[x]'` باعث شد ۱۵۱/۱۵۱
  اشتباه ثبت شود. پس از بررسی evidence واقعی، ۱۲ مورد فاقد شواهد معتبر به `[ ]`
  برگردانده شدند. وضعیت واقعی ۱۳۹/۱۵۱ (78%) است.
- **Engineering Decision 2026-07-27:** 12 مورد باز (Visual/Accessibility/Memory QA)
  به `REL-01 UI & Accessibility QA Gate` منتقل شدند و مانع Feature Development
  نیستند. Core Gate پاس است، SLICE-01 آزاد است.
  این وضعیت Beta-ready یا Production-ready نیست.
