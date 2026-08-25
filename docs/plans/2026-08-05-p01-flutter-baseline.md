# P01 — قرارداد هم‌ارزی و موجودی جامع Flutter

**Type:** refactor

**Status:** P01 — Verified complete after consolidation and human-review remediation

**Progress:** `[██████████] 100%` — 44/44

## Scope و Safety

ساخت baseline کامل، قابل‌شمارش و evidence-based از تمام کد first-party پروژهٔ Flutter در `E:\vista`، بدون تغییر Flutter Source. هر فایل، journey، قرارداد، storage، state و surface بصری باید به یک مالک و فاز Native نگاشت شود. Native در `E:\vista_native` برای اصلاحات ضروری، scope-controlled و متصل به findingهای P01 قابل ویرایش است.

## Artifact design

ممیزی `AUD-01` پوشش سطح محصول و findingهای امنیت/مهاجرت را دارد، اما ledger فایل‌به‌فایل با schema موردنیاز P01 وجود نداشت. خروجی نهایی P01 عمداً به این tracker و دو ledger کمینه شده است:

- `docs/audits/2026-08-05-p01-flutter-coverage.csv`
- `docs/audits/2026-08-05-p01-parity-ledger.csv`

## Phases

### Phase 1 — Safety و Baseline

- [x] `P01-SAFE-01` `memory.md` کامل خوانده شود.
- [x] `P01-SAFE-02` branch، HEAD و status هر دو repository ثبت و با snapshot تطبیق داده شود.
- [x] `P01-SAFE-03` تمام دستورالعمل‌های repository بررسی شدند؛ `AGENTS.md` اضافی وجود ندارد، Flutter Source read-only است و Native فقط برای finding دارای evidence و owner قابل تغییر است.
- [x] `P01-SAFE-04` تغییرات نامرتبط کاربر در هر دو repository دست‌نخورده می‌مانند؛ scope مجاز شامل اسناد P01/`memory.md` و Source/Test/Build ضروری Native با change trace و evidence است.

### Phase 2 — Count و Coverage Ledger

- [x] `P01-COV-01` universe فایل‌ها و patternهای inclusion/exclusion تعریف شد؛ `lib/test/integration_test/**/*.dart`.
- [x] `P01-COV-02` raw=`490`، generated=`17`، production first-party=`459`، test=`14` و first-party total=`473` محاسبه شد.
- [x] `P01-COV-03` برای هر فایل ledger row با ID یکتای `F-0001…F-0490` ساخته شد.
- [x] `P01-COV-04` Category/Feature/Layer برای همهٔ ۴۹۰ ردیف تعیین شد.
- [x] `P01-COV-05` Key symbol با `path:line` برای همهٔ ردیف‌ها ثبت شد؛ empty=`0`.
- [x] `P01-COV-06` ستون Contract/Storage/Test linkage برای همهٔ ردیف‌ها ثبت شد؛ خانهٔ خالی وجود ندارد و موارد نامرتبط صریحاً `N/A` هستند.
- [x] `P01-COV-07` `473 Classified + 17 Generated exclusion = 490`؛ ID مفقود/تکراری=`0`.
- [x] `P01-COV-08` `Unknown — requires follow-up` در Coverage Ledger برابر `0` است.

### Phase 3 — Entry، Navigation و Journey Ledger

- [x] `P01-JRN-01` entry point/flavor/environment/release configuration trace شود.
- [x] `P01-JRN-02` startup/session/maintenance/banned/notification launch trace شود.
- [x] `P01-JRN-03` route registry، tabها، guardها، deep linkها و share intents inventory شوند.
- [x] `P01-JRN-04` تمام route-level screenها و user journeyها ID پایدار بگیرند.
- [x] `P01-JRN-05` UI → State → Repository/Service → API/Storage برای journeyها trace شود.
- [x] `P01-JRN-06` loading/empty/error/offline/permission/keyboard/private/degraded stateها ثبت شوند.
- [x] `P01-JRN-07` هر journey به فاز مقصد Native و status نگاشت شود.

### Phase 4 — Contract، Storage و Background

- [x] `P01-CON-01` REST endpointها با method/path/source line بدون حدس استخراج شوند.
- [x] `P01-CON-02` WS/SSE/streaming، notification payload و background flowها ثبت شوند.
- [x] `P01-CON-03` pagination/retry/backoff/cache/offline/concurrency semantics ثبت شوند.
- [x] `P01-STO-01` Isar collection/schema/version/writer/reader/logout behavior ثبت شود.
- [x] `P01-STO-02` SharedPreferences و Secure Storage key registry بدون مقدار secret ثبت شود.
- [x] `P01-STO-03` file/media/download/draft/pending/session/E2EE/search history inventory شود.
- [x] `P01-STO-04` migration policy، Unknown، blocker و Native target برای هر storage ثبت شود.

### Phase 5 — Design System و Visual Reference

- [x] `P01-VIS-01` color/theme/typography/Vazirmatn sourceها ثبت شوند.
- [x] `P01-VIS-02` spacing/shape/elevation/dimension/component/icon/asset sourceها ثبت شوند.
- [x] `P01-VIS-03` RTL/bidi/Persian/Jalali/number/accessibility rules ثبت شوند.
- [x] `P01-VIS-04` animation/transition/haptic/system-bar behavior ثبت شود.
- [x] `P01-VIS-05` برای تمام route-level screen/stateها Visual Reference row ساخته شود.
- [x] `P01-VIS-06` fixture/theme/direction/keyboard/capture requirements کامل شوند.

### Phase 6 — Test/Evidence و Exit Gate

- [x] `P01-QA-01` unit/widget/integration/golden/fixture inventory و source counts ثبت شود.
- [x] `P01-QA-02` runtime/visual evidence با branch/commit و stale status طبقه‌بندی شود.
- [x] `P01-QA-03` path:lineهای کلیدی نمونه‌برداری و بازاعتبارسنجی شوند.
- [x] `P01-QA-04` تمام totals و ledgerها دوباره reconcile شوند.
- [x] `P01-QA-05` `memory.md` checkboxها، P01 progress و progress کل به‌روز شوند.
- [x] `P01-QA-06` `git diff --check` و final scope audit پاس شود.
- [x] `P01-GATE-01` P01 فقط در صورت پاس تمام Exit Gateها `Verified complete` شود.

### Phase 6A — Policy Correction و Native Gap Control

- [x] `P01-POL-01` سیاست اشتباه read-only بودن Native با حفظ تاریخچه در tracker و `memory.md` اصلاح شد.
- [x] `P01-GAP-01` برای هر ۹۲ journey، implementation/gap/blocker/owner/phase/acceptance/next action Native ثبت شد.
- [x] `P01-GAP-02` تمام gapها طبق A/B/C طبقه‌بندی شدند؛ dirty WIPهای Like/Save/Comments بدون تغییر معنایی یا ادعای completion به دسته C منتقل شدند.
- [x] `P01-NAT-001` دو خطای whitespace جداسازی‌پذیر Native بدون overwrite منطق dirty کاربر اصلاح شدند.
- [x] `P01-NAT-002` change trace و evidence متناسب برای اصلاح Native ثبت شد؛ `git diff --check` پس از اصلاح exit=0.

## Decisions & Notes

- `2026-08-05`: Flutter `main@64994fa1...` و Native `main@bf2b69ea...` با snapshot `memory.md` برابرند.
- هر دو worktree dirty و تغییرات موجود متعلق به کاربر/عامل دیگر هستند؛ Flutter Source تغییر نمی‌کند. Native فقط در محل‌های از قبل dirty پس از خواندن diff و فقط با patch جداسازی‌پذیر، یا در فایل‌های clean با finding ثبت‌شده، تغییر می‌کند.
- اسناد `AUD-01`، asset inventory، Feed/Profile ledger و Search ledger ورودی تاریخی‌اند؛ شواهد stale به‌عنوان پاس جاری استفاده نمی‌شوند.
- Coverage ledger باید فایل‌های Generated را نیز به‌صورت exclusion row ثبت کند تا `raw = classified + generated exclusions` قابل اثبات باشد؛ dependency/build/cache خارج از universe `lib/test/integration_test` با pattern و count جدا ثبت می‌شوند.
- `2026-08-05`: Coverage Ledger در `docs/audits/2026-08-05-p01-flutter-coverage.csv` با ۴۹۰ ردیف، ۴۹۰ ID یکتا و reconciliation صددرصدی پاس شد.
- `2026-08-06 Policy correction`: ممنوعیت سراسری تغییر Native در P01 تصمیم اشتباه بود و لغو شد. Flutter همچنان مرجع read-only محصول است؛ هر تغییر Native باید به finding/journey/contract/parity gap، owner، acceptance criteria، test و evidence متصل باشد. تاریخچهٔ تصمیم قبلی در log حفظ شده است.

## Claim labels

- **Verified fact:** مستقیماً از checkout جاری و anchor معتبر استخراج شده است.
- **Evidence-backed inference:** نتیجهٔ محدود و قابل‌ردیابی از markerهای source است؛ قرارداد runtime فرض نشده است.
- **Unknown / requires confirmation:** مالک، فاز مقصد و اقدام بعدی صریح دارد.

## File coverage summary

| Metric | Current result | Evidence |
|---|---:|---|
| Dart universe | 490 | `lib/test/integration_test/**/*.dart` |
| Production raw | 476 | current filesystem count |
| Generated exclusions | 17 | 13 `*.g.dart` + 4 `lib/l10n/generated/*` |
| Production first-party | 459 | 476 − 17 |
| Test files | 14 | 12 unit-style + 2 widget-style؛ integration-dir=0 |
| First-party total | 473 | 459 + 14 |
| Coverage rows | 490 | 473 Classified + 17 Generated exclusion |
| Missing / duplicate / coverage Unknown | 0 / 0 / 0 | CSV validation |

Dependency، build و cache output از ابتدا بیرون universe هستند: `.dart_tool/**`، `build/**`، `android/.gradle/**`، `ios/Pods/**`، `windows/flutter/ephemeral/**` و package cache؛ count آن‌ها در universe تعریف‌شده صفر است. **Verified fact.**

## Entry/navigation summary

- تنها entry point محصول در universe، `lib/main.dart:8` است و اجرای app به `lib/app/app_runner.dart:105` می‌رسد. scriptهای root/scratch خارج از universe محصول هستند. **Verified fact.**
- startup binding/Firebase/background messaging/session/date locale در `lib/app/app_initialization.dart:35`، `:91`، `:98` و `:104` ثبت شده؛ cache/Isar/retry initialization در `:140-148` است. **Verified fact.**
- route اولیه `/` و `SessionAuthWrapper` در `lib/app/app_runner.dart:637-639`؛ maintenance و banned در `:644-645`؛ route replacement نگهداری در `:378-384`. **Verified fact.**
- cold-launch محلی و FCM در `lib/app/app_runner.dart:154-185`؛ deep-link setup/replay در `:506-554`. **Verified fact.**
- environment از `API_BASE_URL`، `BACKEND_URL`، `WS_BASE_URL` و release defineها در `lib/utils/env_config.dart:9-16` می‌آید؛ flavor compile-time در `lib/core/app_config.dart:25`. productFlavor Gradle مستقلی مشاهده نشد. **Evidence-backed inference.**
- Android application ID برابر `ir.coffevista.vista`، target SDK=34 و release signing selection در `android/app/build.gradle:32-56`؛ نسخهٔ Flutter `2.6.2+4049` در `pubspec.yaml:5`. **Verified fact.**

### Navigation and surface map

- named route registry: 23 declaration در `lib/app/app_runner.dart:639-767`. **Verified fact.**
- Android app/web/custom-scheme links در `android/app/src/main/AndroidManifest.xml:79-99` و inbound `SEND/SEND_MULTIPLE` در `:106-152`. **Verified fact.**
- dispatch مقصدهای post/profile/group/feed/payment/nearby در `lib/services/deep_link_service.dart:65-160`؛ notification destinations در `lib/services/notification_navigation_service.dart:99-155` و deep-link fallback در `:380-471`. **Verified fact.**
- shell/back contract با `PopScope(canPop: false)` در `lib/features/home/screens/homeScreen.dart:456-458`. **Verified fact.**
- 88 فایل به‌عنوان UI Screen در Coverage طبقه‌بندی شده‌اند؛ 72 declaration سطح route از helper/componentها جدا شده و همراه 20 journey ترکیبی/بدون screen، مجموع 92 Journey ID یکتا ساخته است. **Verified fact.**
- modal inventory جاری: `showDialog`=36 در 30 فایل، `showModalBottomSheet`=42 در 27 فایل، `OverlayEntry`=3 در 3 فایل، `PopScope`=8 در 8 فایل. declarationهای generic برای جلوگیری از double count به total مستقل افزوده نشده‌اند. **Verified fact.**

## Feature and journey summary

| Coverage owner | First-party files | Included journeys/capabilities |
|---|---:|---|
| Chat | 147 | inbox، group، send/retry، E2EE، realtime، presence، typing، read receipt، attachment |
| Shared | 52 | reusable widgets، media players، dialogs/sheets، formatting، platform helpers |
| Stories | 49 | feed/player، composer/editor، privacy، highlight، upload |
| Posts | 48 | feed، post detail، comments، composer، upload، engagement |
| Services | 39 | services hub، weather/market/top users/groups و service detail |
| Settings | 33 | privacy، sessions، security، updater/store، premium/payment |
| Profile | 27 | public/private profile، edit، follow/request، notes |
| Auth | 19 | onboarding gate، OTP/2FA، password recovery/setup، biometric، session |
| Notifications | 10 | FCM/local payload، inbox، tap/cold launch |
| Nearby | 10 | discovery، permission، map/location state |
| Cross-cutting | 8 | app config، localization، navigation، initialization |
| Music | 7 | player، download manager، mini-player |
| Search | 5 | query/debounce، history، QR |
| Emoji | 5 | picker/rendering |
| Storage | 4 | Isar manager/entities and cache boundary |
| Startup | 3 | entry، runner، initialization |
| Premium | 2 | entitlement/payment surfaces |
| Onboarding | 2 | onboarding version/screen |
| Share | 2 | Android inbound share and target UI |
| Home | 1 | five-tab shell/back behavior |

مجموع بالا 473 فایل Classified است. Media، Composer، Upload/Download، Store/Updater و Realtime به‌عنوان capabilityهای چنددامنه‌ای در Journey/Contract/Visual ledger مستقل ID دارند و به یک پوشهٔ مصنوعی جدید تقلیل داده نشده‌اند. **Verified fact.**

## State-management trace

- Coverage، 39 فایل State، 12 Repository، 91 Service، 21 Storage و 27 Model/Domain را جدا می‌کند. **Verified fact.**
- Parity Ledger برای هر 92 flow ستون‌های UI، State، Repository/Service، API/Storage، visible states، test و Native phase را بدون خانهٔ خالی نگه می‌دارد. **Verified fact.**
- برای هر 39 فایل State، lifecycle/input/output/loading/error/retry/cancellation/optimistic/pagination/cache/concurrency/process recovery/downstream در `Record Type=State` ثبت شده است. **Evidence-backed inference.**
- `Source-level state-file inventory complete; journey-level behavioral confirmation remains assigned to implementation/runtime phases.`

## API/realtime/background summary

- Parity Ledger دارای 187 رکورد `Contract` یکتا و بدون خانهٔ خالی است: 171 REST literal، 11 dynamic HTTP و 5 realtime/background/inbound contract. **Verified fact.**
- URLهای پویا مانند presigned upload و media attachment عمداً به endpoint حدسی تبدیل نشده‌اند؛ expression و owner واقعی ثبت شده است. **Verified fact.**
- SSE در `lib/features/chat/services/sse_manager.dart:92`، notification payload در `lib/services/PushNotificationService.dart:906`، cold launch در `lib/app/app_runner.dart:154` و persistent retry در `lib/services/retry_queue_service.dart:54-78`. **Verified fact.**
- `Source-level API and storage call-site inventory complete; runtime contract behavior requires phase-specific contract and integration tests.`

## Storage and migration summary

- هشت Isar collection در `lib/DB/isar_database_manager.dart:80-87`؛ encryption/open retry در `:117-171` و key material در `:254-272`. **Verified fact.**
- logout boundary، Isar/cache wipe، preferences clear، secure delete و file-cache clear در `lib/services/secure_logout_service.dart:30-50`. **Verified fact.**
- `Record Type=Storage` برای 76 مورد، type، schema/version، writer/reader evidence، sensitivity، lifecycle نوع‌محور، logout، update/uninstall، migration policy و owner/action را ثبت می‌کند. typeها به `Isar`، `SharedPreferences`، `SecureStorage`، `InternalFile`، `ExternalFile`، `LegacySQLite` و `MemoryOnly` محدودند؛ هیچ مقدار secret ثبت نشده است. **Verified fact.**
- draft حافظه‌ای صریحاً `Memory/navigation-scoped; no persistence across process death is proven.` ثبت شده است.

## Design system and visual summary

- tokenهای spacing/radius/elevation در `lib/core/theme/app_theme.dart:9-60`، color palette در `:68-140`، typography/Vazirmatn و light/dark ThemeData در `:145-318`. **Verified fact.**
- وزن‌های Vazirmatn در `pubspec.yaml:170-185` و motion durationها در `lib/utils/vista_motion.dart:10-12`. **Verified fact.**
- RTL/directional helper در `lib/utils/widgets.dart:597-606` و bidi detection چت در `lib/features/chat/utils/chat_text_direction.dart:2-16`؛ Jalali در `lib/utils/birth_date_picker.dart:40-101`؛ عدد فارسی/قیمت در `lib/features/settings/screens/vistaStore/pricing_page.dart:211`. **Verified fact.**
- system bars در `lib/core/theme/app_theme.dart:300` و `lib/app/app_initialization.dart:75-85`؛ haptic نمونه در `lib/services/audio_recording_service.dart:119-268`؛ accessibility/font-scale در `lib/features/services/screens/services_screen.dart:469` و `:607-689`. **Verified fact.**
- Parity Ledger شامل 294 رکورد `Visual` برای 72 screen سطح route است؛ هر رکورد Fixture ID یکتا و deterministic، Light/Dark، RTL/LTR+bidi و keyboard condition دارد. Captureها در P01 تولید نشده و Pending implementation phase هستند. **Verified fact.**
- قاعدهٔ capture مشترک: Flutter reference و Native candidate با device/fixture/theme/direction/keyboard یکسان ثبت شوند؛ contact sheet، overlay و pixel-diff/SSIM متناسب در فاز مقصد تولید و mismatchها ثبت شوند.

## Test and existing evidence

- 14 فایل test فعلی: 12 unit-style و 2 widget-style؛ integration directory=0 و golden matcher=0. وجود فایل به معنی پاس فعلی نیست و در این فاز تست اجرا نشده است. **Verified fact.**
- evidenceهای قبلی AUD/FND/DSN/Auth/Login/Search/Integration با branch/commit خود در اسناد موجود حفظ شده‌اند؛ برای checkout جاری فقط provenance تاریخی‌اند و runtime/visual pass تازه محسوب نمی‌شوند. **Verified fact.**

## Controlled unknowns and blockers

| ID | Finding | Label | Owner / target | Next action | P01 impact |
|---|---|---|---|---|---|
| `P01-U01` | persisted first-party draft writer پیدا نشد؛ draft چت فعلی memory/navigation-based است | Evidence-backed inference | `CHAT-02 / MIG-01` | پیش از implementation writers را دوباره scan؛ در صورت نبود، zero-import | Non-blocking؛ سیاست محافظه‌کارانه مشخص است |
| `P01-U02` | app-level schema version صریح برای Isar مشاهده نشد؛ generated CollectionSchema وجود دارد | Unknown / requires confirmation | `MIG-01` | fingerprint/schema decoder و fixture upgrade تهیه شود؛ تا آن زمان fail-open/no-import | Known migration blocker؛ مالک و اقدام دارد |

Unknown بدون owner، فاز مقصد یا اقدام بعدی: **0**.

## Native gap summary

- `Record Type=NativeGap`: ۹۲ ردیف / ۹۲ ID یکتا / ۹۲ Journey ID یکتا / empty=0 / invalid Flutter anchor=0.
- هر ۹۲ رکورد `Journey` به یک Native trace متناظر در همان Parity Ledger نگاشت شده است.
- وضعیت current implementation: ۷۰ Missing/Transferred، ۲۰ Partial و ۲ Unverified dirty WIP.
- ۹۰ gap در دسته C canonical transfer و ۲ gap Like/Save/Comments در دسته C dirty-work conflict هستند. هیچ gap به‌اشتباه «موکول شود» بدون blocker/owner/phase/acceptance/next action نمانده است.
- در این iteration هیچ vertical slice دسته B شروع نشد: checkout اصلی Native دارای WIP گستردهٔ Feed است و تمام gapهای feature-level باقی‌مانده یا dependency/security/visual gate دارند یا برای اصلاح کوچک P01 بیش از حد بزرگ‌اند. **Evidence-backed decision.**

## Findings and remediation

| Finding ID | Finding | Category | Owner / phase | Acceptance | Status |
|---|---|---|---|---|---|
| `P01-F01` | persisted Flutter draft writer پیدا نشد | C | `CHAT-02 / MIG-01` | re-scan writerها؛ در صورت نبود zero-import | Transferred |
| `P01-F02` | Isar app-level schema version صریح نیست | C | `MIG-01` | schema fingerprint/decoder fixture؛ fail-open ممنوع | Blocked/Transferred |
| `P01-F03` | Native global diff gate روی دو whitespace defect شکست می‌خورد | A | `P01 QA` | حذف فقط whitespace معیوب و `git diff --check=0` | Resolved by `P01-NAT-001` |

## Native Change Trace — P01-NAT-001

```text
Change ID: P01-NAT-001
P01 Finding ID: P01-F03
Flutter reference: N/A — repository hygiene infrastructure; no Flutter behavior or contract changed
Native previous state: git diff --check failed on FeedDao.kt:98 blank EOF and PostDetailScreen.kt:56 trailing whitespace
Native change: removed exactly the extra EOF blank line and trailing spaces; no Kotlin token or behavior changed
Reason: P01-QA-06 and the corrected Exit Gate require a clean global diff check
Files changed: core/database/.../FeedDao.kt; feature/feed/.../PostDetailScreen.kt
Tests added/updated: none; non-semantic whitespace-only correction
Commands executed: git diff for both dirty files; git diff --check before/after
Runtime evidence: N/A — no executable behavior changed
Visual evidence: N/A — no UI token/layout/content changed
Remaining gaps: user-owned Feed engagement WIP remains unverified and transferred to SLICE-01D
Target canonical phase: P01 QA
Status: Verified complete — git diff --check exit=0
```

## Exit Gate evidence

هر finding یکی از وضعیت‌های زیر را می‌گیرد:

- **A — Immediate Native correction:** کوچک/متوسط، کم‌ریسک، بدون تصمیم محصولی و قابل‌تست در P01.
- **B — Isolated vertical slice:** scope بزرگ‌تر ولی وابستگی روشن؛ فقط در branch/worktree ایزوله با tracker و evidence.
- **C — Canonical phase transfer:** تداخل dirty work، dependency/امنیت/backend/release decision یا scope بزرگ؛ الزاماً دارای blocker، owner، target phase، acceptance criteria و next action.

P01 فقط زمانی `Verified complete` است که inventoryهای Flutter reconcile، تمام journey/contract/storage/visual referenceها ثبت، Unknownها دارای owner/action، تغییرات Native دارای finding/trace/test/evidence، gapهای باز دارای انتقال کامل، progress صحیح، `git diff --check` سبز و scope audit مؤید عدم تغییر Flutter Source و عدم overwrite کار کاربر باشد.

**وضعیت پیش از iteration اصلاحی: Partial.** خروجی‌های inventory پاس بودند، اما `git diff --check` سراسری Native روی دو فایل dirty شکست می‌خورد:

- `core/database/src/main/java/ir/coffevista/vista_native/core/database/feed/FeedDao.kt:98` — blank line at EOF.
- `feature/feed/src/main/java/ir/coffevista/vista_native/features/feed/ui/PostDetailScreen.kt:56` — trailing whitespace.

تصمیم قبلی این خطاها را صرفاً به‌دلیل read-only فرض‌کردن Native اصلاح نکرد. در iteration جاری آن تصمیم منسوخ است؛ هر دو diff ابتدا بررسی و فقط در صورت جداسازی‌پذیری به‌عنوان `P01-NAT-001` اصلاح می‌شوند. تا ثبت test/evidence، `P01-QA-06` و `P01-GATE-01` باز می‌مانند.

## Cleanup manifest

| File | Action | Destination | Reason |
|---|---|---|---|
| `memory.md` | Kept | همان مسیر | مرجع progress و continuation |
| `docs/plans/2026-08-05-p01-flutter-baseline.md` | Kept | همان مسیر | منبع حقیقت summary و Exit Gate |
| `docs/audits/2026-08-05-p01-flutter-coverage.csv` | Kept | همان مسیر | reconciliation فایل‌به‌فایل 490 ردیفی |
| شش ledger قدیمی journey/state/API-storage/storage/visual/native-gap | Merged / Deleted | `docs/audits/2026-08-05-p01-parity-ledger.csv` | حفظ تمام record typeها با schema واحد و حذف هم‌پوشانی |

## Current result

**نتیجه جاری — 2026-08-06: P01 — Verified complete after consolidation and human-review remediation.** Coverage=`490`، Parity=`782`، Journey=`92`، State=`39`، Contract=`187`، Storage=`76`، Visual=`294`، NativeGap=`92` و Unknown=`2`؛ blank/duplicate/invalid-anchor/mojibake=`0`. `P01-NAT-001` تاریخی بدون تغییر semantic بسته مانده است؛ در iteration تجمیع هیچ Flutter یا Native Source تغییر نکرد، P02 آغاز نشد و `git diff --check` نهایی پاس شد.
