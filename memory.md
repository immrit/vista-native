# حافظه و نقشه‌راه مهاجرت Vista از Flutter به Native Android

**نوع:** برنامه‌ریزی معماری، مهاجرت و هم‌ارزی محصول

**وضعیت:** P03 Partial با external/runtime gateهای باز — P04 Partial؛ IME/insets و Shell runtime بسته، authenticated visual fixture و Stories باز

**آخرین به‌روزرسانی:** ۱۴۰۵/۰۵/۱۷ — 2026-08-08

**مرجع Flutter:** `E:\vista`

**مقصد Native:** `E:\vista_native`

**Progress کل مهاجرت:** `[████░░░░░░] 36%` — 43/119 checkbox؛ بر پایهٔ شواهد همین فایل

**Progress ساخت این سند:** `[██████████] 100%`

> این فایل منبع حقیقت ادامهٔ مهاجرت است. هر عامل انسانی یا هوش مصنوعی باید پیش از شروع کار، این فایل، وضعیت زندهٔ Git و شواهد فاز مربوطه را بخواند. هیچ موردی صرفاً به‌دلیل وجود فایل یا موفقیت کامپایل، «کامل» محسوب نمی‌شود.

## هدف

تکمیل مهاجرت Vista از Flutter به Kotlin/Jetpack Compose به‌گونه‌ای که کاربران فعلی پس از انتشار، با محصولی تازه یا ناآشنا مواجه نشوند. هویت بصری، رفتار، مسیرهای کاربری، دادهٔ محلی، نشست، RTL، متن فارسی و قراردادهای سرویس باید حفظ شوند؛ بهبود مجاز باید در کارایی، پایداری، امنیت، دسترس‌پذیری و حس Native باشد.

## قواعد غیرقابل مذاکره

- پیاده‌سازی فعال Flutter مرجع رفتار و ظاهر است، مگر اینکه یک انحراف در «دفتر انحرافات نیازمند تأیید» صریحاً تأیید شود.
- معماری داخلی Native می‌تواند بهتر باشد، اما قرارداد بیرونی محصول نباید بی‌اجازه تغییر کند.
- پیش از هر ویرایش، شاخه، HEAD، worktree، فایل‌های staged/modified/untracked و مالکیت تغییرات بررسی شود.
- تغییرات نامرتبط کاربر هرگز reset، stash، clean، overwrite یا stage نمی‌شوند.
- هر ادعای هم‌ارزی باید دارای مسیر منبع، تست و مدرک Runtime/Visual متناسب باشد.
- Static، Build، Test، Device، Runtime، Visual، Release، Deployment و Production شواهد مستقل هستند.
- هر تغییر ظاهری Native با اجرای واقعی Flutter و fixture یکسان مقایسه می‌شود.
- هیچ secret، token، کلید E2EE، signing material یا متن خصوصی پیام ثبت یا افشا نمی‌شود.
- Flutter Source در تمام ممیزی read-only و مرجع محصول است. Native فقط برای findingهای evidence-driven فاز فعال، با owner، scope، acceptance criteria، test و evidence تغییر می‌کند.

## وضعیت امن اولیهٔ مخازن

- [x] مسیر Flutter بررسی شد: `E:\vista`
- [x] مسیر Native بررسی شد: `E:\vista_native`
- [x] شاخه و HEAD اولیهٔ Flutter ثبت شد: `main` / `64994fa1dfa6f1b146cb9d132758f105670e4d71`
- [x] شاخه و HEAD اولیهٔ Native ثبت شد: `main` / `bf2b69ea04c0efc77c271eb730769d6f86a37153`
- [x] وجود تغییرات جاری در هر دو مخزن ثبت شد؛ همه خارج از مالکیت این ممیزی هستند.
- [x] فایل قدیمی `E:\vista\memory.md` شناسایی و برای جلوگیری از overwrite دست‌نخورده نگه داشته شد.
- [x] وضعیت اولیهٔ کامل Git با تفکیک staged/unstaged/untracked در بخش شواهد ثبت شد.
- [x] دستورالعمل `vista`، Master Plan، trackerهای Native و final-verificationهای موجود بررسی شدند.

## از اینجا ادامه بده — Resume Protocol

عامل بعدی نباید مستقیماً وارد کدنویسی شود. ترتیب شروع اجباری:

1. این فایل را کامل بخواند.
2. در هر دو مسیر `git status --short`، `git branch --show-current` و `git rev-parse HEAD` را دوباره اجرا کند.
3. اگر HEAD یا dirty inventory با snapshot زیر فرق داشت، ابتدا همین بخش را به‌روز کند.
4. مالکیت تغییرات موجود را نامشخص/کاربر فرض کند؛ هیچ فایل جاری را overwrite یا stage نکند.
5. tracker و evidence فاز انتخابی را کامل بخواند.
6. فقط یک فاز/vertical slice را در یک branch یا worktree ایزوله شروع کند.
7. پیش از تغییر ظاهر، capture مرجع Flutter را با fixture قابل‌تکرار بگیرد.
8. پس از هر gate واقعی، checkbox و progress همین فایل را همان لحظه به‌روز کند.

**نقطهٔ شروع پیشنهادی فعلی:** P03 همچنان به `REL-02` signing/upgrade، اجرای واقعی CVE در CI و runtime سالم API 24 نیاز دارد. ادامهٔ P04، تأمین credential محیطی برای canonical authenticated pair و سپس بستن Stories/TalkBack visual gate است؛ IME/inset API 33 بسته شده و dirty WIP تعاملات Feed همچنان متعلق به کاربر است.

## Snapshot زندهٔ ممیزی — 2026-08-05

### Git و مالکیت تغییرات

| مخزن | Branch / HEAD | tracked | modified | staged | untracked | نتیجه |
|---|---|---:|---:|---:|---:|---|
| Flutter `E:\vista` | `main` / `64994fa1dfa6f1b146cb9d132758f105670e4d71` | 5689 | 9 | 0 | 11 | Dirty؛ فقط read-only |
| Native `E:\vista_native` | `main` / `bf2b69ea04c0efc77c271eb730769d6f86a37153` | 905 | 17 | 0 | 51 با احتساب این فایل | Dirty؛ تغییرات Feed و artifacts متعلق به کاربر |

**Snapshot افزودهٔ P02/P03 — 2026-08-06:** HEADها بدون تغییرند. Flutter=`main@64994fa` با ۹ modified و ۶ ورودی untracked در `git status`؛ Native=`main@bf2b69e` با ۲۳ modified و ۴۷ ورودی untracked. افزایش Native شامل اصلاحات محدود P03 در Gradle/desugaring/lint به‌علاوهٔ WIP و artifactهای ازپیش‌موجود کاربر است؛ staged=`0` و هیچ تغییر کاربر reset/stash/clean/overwrite نشد.

**Flutter WIP موجود:** `app_runner`، `session_auth_wrapper`، Chat، Services، notification/http، `pubspec.yaml` و Onboarding/assetهای جدید. مرجع ظاهری Onboarding در حال تغییر است؛ baseline قدیمی آن نباید برای parity جدید استفاده شود.

**Native WIP موجود:** ۱۷ فایل tracked در Feed/Database/Shell و lockfileها تغییر کرده‌اند؛ Comment API/Repository/UI، screenshotها و log/layout dumpهای متعدد untracked هستند. این مجموعه نشانهٔ کار جاری روی Like/Save/Comments است، اما تا زمان تعیین مالک، تست تازه، diff تمیز و evidence معتبر `Verified` نیست.

### قواعد شمارش Source

- Flutter raw: تعداد `*.dart` زیر `lib` برابر `476` است.
- Flutter generated exclusions: ۱۳ فایل `*.g.dart` و ۴ فایل `lib/l10n/generated/*`؛ first-party قابل‌ممیزی در این snapshot برابر `459` فایل است.
- Flutter test/integration files همراه با `lib` مجموعاً `490` فایل Dart هستند.
- Flutter سطح/صفحه: ۸۳ فایل screen/page شناسایی شد؛ برخی helperهای UI نیز در پوشهٔ `screens` قرار گرفته‌اند و در ledger تفصیلی باید از route-level screen جدا شوند.
- Native: ۱۸۸ فایل Kotlin شامل ۱۴۳ main، ۲۹ unit-test، ۱۲ androidTest و ۴ build-logic.
- build output، Gradle cache، generated code و dependencyها در inventory first-party وارد نمی‌شوند.

### موجودی Flutter بر اساس دامنه

| دامنه | فایل Dart | Screen/Page | منابع اصلی |
|---|---:|---:|---|
| Auth | 15 | 8 | `lib/features/auth/`؛ `auth_wizard_screen.dart:13` |
| Chat | 137 | 20 | `ChatConversationsScreen.dart:37`، `modern_chat_screen.dart:180`، `chat_repository_impl.dart:35`، `e2e_encryption_service.dart:34` |
| Posts/Feed/Profile legacy | 34 + legacy providerها | 11 | `ExploreFeedScreen.dart:69`، `profileScreen.dart:65`، `personalized_feed_provider.dart:163` |
| Stories | 45 | 8 | `story_repository.dart:13`، `story_creation_screen.dart:19`، `story_player_screen.dart:33` |
| Settings/Store | 19 | 17 | `Settings.dart:24` و زیرصفحه‌های settings/store |
| Nearby | 9 | 3 | `nearby_screen.dart:17`، `nearby_repository.dart:18` |
| Services | 10 | 5 | `services_screen.dart:49`، `services_hub_repository.dart:8` |
| Search | 2 | 2 | `searchPage.dart:25` و `VistaQRScanner.dart` |
| Onboarding | 2 | 2 | implementation قدیمی و implementation جدیدِ untracked؛ منبع نهایی هنوز dirty است |
| Music/Share/Home | 3 | 3 | player/download، share target و shell پنج‌تب |
| Legacy shared layers | 168 | — | `lib/services`=64، `provider`=21، `DB`=15، `model`=14، `widgets`=15 و سایر core/app/utils |

### Entry point و ناوبری Flutter

- shell پنج‌تب در `lib/features/home/screens/homeScreen.dart:79-87` به Feed، Search، Services، Chat و Profile نگاشت می‌شود.
- route registry اصلی در `lib/app/app_runner.dart:637-787` قرار دارد.
- مسیرهای سطح اول شامل Home، Onboarding، Auth، Maintenance، Banned، Profile Setup، Password Recovery/Setup، Biometric، Settings، Feed، Nearby، Premium، Post Detail، Appeal، Profile، Chat و Story هستند.
- launch از notification در `lib/app/app_runner.dart:154-190` و deep-link/intentهای Android نیز باید در parity ledger نگاشت شوند.
- application ID منتشرشدهٔ Flutter برابر `ir.coffevista.vista` و نسخهٔ source برابر `2.6.2+4049` است؛ signing material فقط باید خارج از گزارش و source کنترل استفاده شود.

### موجودی Native و مرز معماری

| ماژول | main | unit | androidTest | نقش جاری |
|---|---:|---:|---:|---|
| `:app` | 9 | 1 | 6 | composition root، typed navigation، startup boundary |
| `:core:common` | 7 | 1 | 0 | error/logging/dispatcher primitives |
| `:core:database` | 14 | 0 | 1 | Room v7؛ Feed/Profile/Search/TLS entities |
| `:core:datastore` | 4 | 2 | 1 | Proto DataStore و onboarding migration |
| `:core:designsystem` | 9 | 1 | 0 | token/theme/component foundation؛ QA gate باز |
| `:core:model` | 2 | 0 | 0 | مدل‌های pure Kotlin مشترک |
| `:core:network` | 14 | 4 | 0 | Retrofit/OkHttp/TLS/error contract |
| `:core:security` | 2 | 1 | 1 | Keystore-backed session store؛ fail-closed target |
| `:core:testing` | 1 | 0 | 0 | fixture/test support |
| `:core:worker` | 4 | 1 | 1 | WorkManager foundation؛ هنوز feature worker کامل نیست |
| `:feature:auth` | 20 | 7 | 0 | Startup/Onboarding/Login/OTP/2FA پایه |
| `:feature:feed` | 19 | 3 | 1 | Feed/Post Detail و WIP تعاملات |
| `:feature:profile` | 21 | 5 | 0 | Own/Other Profile و Follow |
| `:feature:search` | 9 | 2 | 1 | Search data/history/UI؛ gate runtime/visual باز |
| `:feature:shell` | 4 | 1 | 0 | shell پنج‌تب؛ Services/Chat هنوز placeholder |

**قرارداد Platform فعلی Native:** Kotlin/Java 17، compile/target SDK 36 و min SDK 24 در `build-logic`; Beta=`ir.coffevista.vista_native` و Production=`ir.coffevista.vista` در `app/build.gradle.kts:52-65`. Release دارای R8/resource shrinking است، اما production signing و upgrade proof مستقل و بازند.

**ناوبری Native:** `AppRoute.kt:7-48` فقط Startup/Maintenance/Onboarding/Auth/AuthenticatedBoundary و handoffهای Deferred را تعریف می‌کند. Shell در `VistaShell.kt:169-305` Feed/Search/Profile واقعی دارد، ولی Services در `:217-221` و Chat در `:223-227` Placeholder هستند.

**Contractهای پیاده‌شده:** Auth (`/v1/auth/*`)، Feed/Post (`/v1/explore`، `/v1/feed/following`، post/user posts)، Profile/Follow و Search/Hashtag. Like/Save/Comments در dirty WIP هستند و بخشی از baseline committed محسوب نمی‌شوند.

## آشتی Master Plan و Trackerهای موجود

Master Plan در `VISTA_NATIVE_MASTER_PLAN.md` جهت معماری و نام فازها معتبر است، اما بخش «وضعیت فعلی» آن stale است: Native دیگر تک‌ماژوله/بدون Git/۲۸ فایل نیست. جدول زیر وضعیت قابل اتکاتر checkout جاری است.

| Tracker | ادعای سند | وضعیت قابل استفاده در این پلن | اقدام بعدی |
|---|---|---|---|
| `AUD-01` | 55/55؛ Audit Closure Passed | Audit تاریخی Complete؛ implementation backlog باز | یافته‌های منتقل‌شده در فاز مالک بسته شوند |
| `GOV-01` | 40/40؛ Local Governance Passed | Local gate معتبر تاریخی؛ production/CVE/signing باز | re-run روی release candidate |
| `FND-01` | 193/193؛ Local Foundation Passed | Foundation پیاده؛ API24، signed TLS، Isar bridge، CVE و signing باز | بستن independent gates پیش از Beta |
| `DSN-01` | Core Complete | ۱۳۹/۱۵۱ واقعی؛ ۱۲ QA item به REL-01 منتقل شده | UI test، 200% font، Dark/Landscape، memory |
| `SLICE-01A` | Own Profile 100% | Functional baseline تاریخی؛ status metadata هنوز `planning` و current re-run لازم | metadata اصلاح و visual gate بازتأیید شود |
| `SLICE-01B` | Read-only Feed 100% | Functional baseline تاریخی | تعاملات و visual parity هنوز scope بعدی |
| `SLICE-01C` | Other Profile/Follow 100% | Functional baseline تاریخی | visual/private/error states بازتأیید شود |
| `VISUAL-PARITY-01` | Login/OTP 100% | ۹/۹ pair و 126 test روی base قبلی پاس؛ تأیید بصری کاربر و regression current HEAD باز | capture کوتاه current HEAD سپس approval |
| `VISUAL-FUNCTIONAL-PARITY-02` | 60 done / 24 open | Functional Feed/Profile پاس؛ Visual همهٔ سطوح Pending | fixture یکسان، ۲۴ state مفقود و mismatch 8.97–56.32% بسته شود |
| `VISUAL-FUNCTIONAL-PARITY-03` | 54 done / 14 open | Backend contract پاس؛ Visual/Functional/Navigation/Runtime Pending | APK ownership ایزوله و state matrix کامل شود |
| `INT-01` | integration انجام شده | 221 تست تاریخی پاس، اما حدود 42 state مفقود و mismatch میانگین حدود 70% | Integration Visual Gate هنوز باز و اولویت نزدیک است |

### یافته‌های منتقل‌شدهٔ AUD-01 که هنوز مهم‌اند

| ID | وضعیت فعلی | مالک/فاز خروج |
|---|---|---|
| `FND-WEB-01` | policy پایه وجود دارد؛ UI Services اجرا نشده | `DISC-01` |
| `FND-TLS-01` | signed/revisioned pin config در backend اثبات نشده | Backend + Foundation security |
| `FND-API24-01` | Runtime API 24 اجرا نشده | `REL-01 Compatibility QA` |
| `FND-SEC-ISAR-01` | Flutter fail-open؛ import واقعی Blocked | `MIG-01` پیش‌نیاز امنیتی |
| `GOV-CVE-01` | scan واقعی بدون `NVD_API_KEY` اجرا نشده | CI/Release |
| `REL-SIGN-01` | signing production و upgrade lineage اثبات نشده | `REL-02` |
| `MEDIA-DRAFT-01` | legacy Draft منبع پایدار ندارد؛ policy=`zero-import` | `MEDIA-01` persistence جدید |
| `MIG-DL-01` | policy=`scan + unmatched`؛ حدس URL/entity ممنوع | `MIG-01` |
| `PERF-01` | baseline معتبر ARM profile/release وجود ندارد | Performance Gate پیش از release |
| `FEAT-VIS-01` | Story و state fixtureهای منتقل‌شده ناقص‌اند | feature owners + `QA/REL-01` |

## ماتریس فعلی قابلیت و شکاف

| قابلیت | Flutter مرجع | Native فعلی | وضعیت | مهم‌ترین شکاف / Gate | فاز مالک |
|---|---|---|---|---|---|
| Startup/Maintenance | `SessionAuthWrapper` و routeهای startup | Startup resolver + screens | Partial | current HEAD و API24؛ banned/maintenance matrix کامل | FND/Auth/REL |
| Onboarding | source جدید در dirty Flutter | Onboarding Native موجود | Unknown/Partial | مرجع Flutter در حال تغییر؛ baseline جدید لازم | P06/VISUAL |
| Login/OTP/2FA | Auth wizard | Auth feature | Partial with verified baseline | current integration regression و تأیید کاربر | VISUAL-PARITY-01 |
| Password recovery/mandatory/biometric/profile setup | چند route مستقل Flutter | destination هم‌ارز کامل دیده نشد | Missing/Partial | journey، قرارداد و UI کامل | Auth extension |
| Shell پنج‌تب | `homeScreen.dart:79-87` | typed nested graphs | Partial | visual/a11y gates و placeholderها | DSN/INT |
| Feed read-only | Explore/Following feed | Feed + Room + paging | Partial | visual، story rail/FAB، stateهای ناقص | SLICE/VFP-02 |
| Like/Save | optimistic actions Flutter | dirty WIP Native | Unverified WIP | مالکیت diff، rollback/error tests، visual | SLICE-01D |
| Comments | nested/reply/edit/delete/report Flutter | dirty basic list/create/delete | Unverified WIP | replies/edit/report/mentions/paging/optimistic parity | SLICE-01D |
| Post Detail | full media/actions/comment composer | Native Post Detail | Partial | geometry/media/comment composer و Search handoff | SLICE/VFP-02/03 |
| Own/Other Profile + Follow | full profile tabs/actions | basic profile/follow/posts | Partial | edit/share/badges/private/error/visual states | SLICE/VFP-02 |
| Search | users/hashtags/posts/history/QR | Search users/hashtags/history | Partial | QR، exhaustive runtime، post handoff، visual states | VFP-03 |
| Services/Contacts/Web | Services hub + allowlisted flows | Placeholder | Missing | کل feature و FND-WEB-01 | DISC-01 |
| Chat/Realtime | 137 فایل، E2EE و WS | Placeholder | Missing | تمام CHAT-01/02 و migration keys | CHAT-01/02 |
| Stories | create/editor/player/privacy/viewers | وجود ندارد | Missing | Media dependency و fixture matrix | STORY-01 |
| Composer/Upload | post composer، retry و share handoff | وجود ندارد | Missing | Draft جدید، WorkManager و upload resume | MEDIA-01 |
| Notifications | FCM/local/action/deep-link | feature کامل وجود ندارد | Missing | permission/channel/action/killed state | DISC/CHAT |
| Nearby | browse/likes/matches/location | وجود ندارد | Missing | permission، privacy و lifecycle | DISC-01 |
| Settings/Privacy/Sessions | ۱۷ سطح Settings/Store | وجود ندارد | Missing | تمام صفحات و قراردادهای account/privacy | DISC-01 |
| Premium/Payment | Bazaar/Zibal/store/entitlement | وجود ندارد | Missing | billing، restore، deep link و release market | DISC/REL |
| Music/Downloads | player/background/download | وجود ندارد | Missing | Media3، audio focus، manifest migration | DISC/MIG |
| Share intents | inbound/outbound share | manifest/feature کامل ندارد | Missing | intent matrix، URI permission، temp files | MEDIA/DISC |
| Data migration | Isar/Prefs/Secure storage | Room/DataStore/Keystore target | Blocked | secure bridge، signing/package، E2EE و rollback | MIG-01 |
| Performance/A11y | Flutter baselineهای محدود | foundation tests محدود | Partial | ARM release، Macrobenchmark، TalkBack/200% font | PERF/REL QA |

## ترتیب Canonical اجرای فازها

فازهای P00…P13 بالا نمای کلان‌اند. هنگام branch/worktree و commit از شناسه‌های موجود زیر استفاده شود:

1. `PLAN-01` — تثبیت این فایل، inventory تفصیلی و ownership dirty work.
2. `INT-01-CLOSE` — بازتأیید integration و بستن visual/runtime gaps مشترک.
3. `SLICE-01D` — تکمیل Feed engagement جاری: Like/Save/Comments/Share/Report با parity.
4. `AUTH-02` — مسیرهای Auth باقیمانده و Onboarding جدیدِ مرجع.
5. `MEDIA-01` — Composer، Draft پایدار، Upload و share handoff.
6. `STORY-01` — Story end-to-end بر پایهٔ Media.
7. `CHAT-01` — Inbox، Realtime، FCM merge و sync.
8. `CHAT-02` — Message، E2EE، attachment، group و migration vectors.
9. `DISC-01A` — Notifications + تکمیل Search/QR.
10. `DISC-01B` — Nearby + Services/Contacts/Web policy.
11. `DISC-01C` — Settings/Privacy/Sessions + Premium/Payment + Music/Updater/Legal.
12. `MIG-01` — bridge نسخه‌دار و تمرین upgrade/rollback؛ پس از رفع Isar fail-open و signing decision.
13. `PERF-01` — baseline و tuning روی ARM profile/release.
14. `QA-01` — ماتریس جامع functional/visual/accessibility/device.
15. `REL-01` — Beta package جدا، cohort و stop gates.
16. `REL-02` — production replacement، rollout مرحله‌ای و دو نسخهٔ پایدار پیش از retirement Flutter.

### Critical Path

`INT-01-CLOSE → SLICE-01D → MEDIA-01 → STORY-01`

`Foundation Security → CHAT-01 → CHAT-02 → MIG-01`
`همهٔ Featureها → PERF-01 + QA-01 → REL-01 → MIG rehearsal → REL-02`

کارهای Design System QA، API24، CVE و signed TLS می‌توانند هم‌زمان با Featureها پیش بروند، اما همگی پیش‌نیاز Beta/Production هستند.

## راهنمای وضعیت و شواهد

| وضعیت | معنا |
|---|---|
| `Verified complete` | تمام exit gateهای لازم با شواهد تازه پاس شده‌اند |
| `Partial` | بخشی پیاده یا اثبات شده، اما یک یا چند gate باز است |
| `Missing` | پیاده‌سازی یا قرارداد لازم وجود ندارد |
| `Blocked` | مانع مشخص، مالک و شرط رفع ثبت شده است |
| `Unknown` | شواهد برای تصمیم کافی نیست |
| `N/A` | با دلیل روشن برای Vista کاربرد ندارد |

## زنجیرهٔ اجباری ردیابی هر قابلیت

`Flutter UI/Behavior → Flutter State/Provider → Repository/Service → API/Local Contract → Native Data/Domain → Native ViewModel/State → Compose UI/Navigation → Tests → Runtime Evidence → Visual Evidence`

## فازها و پیشرفت کلان

| فاز | عنوان | وضعیت | پیشرفت |
|---|---|---|---:|
| `P00` | ایمن‌سازی، ممیزی و آشتی اسناد موجود | Baseline complete | 100% |
| `P01` | قرارداد هم‌ارزی و موجودی جامع Flutter | Verified complete after consolidation and human-review remediation | 100% — 9/9 |
| `P02` | موجودی Native و تحلیل شکاف انتهابه‌انتها | Verified complete | 100% — 8/8 |
| `P03` | حاکمیت Build/Release و تثبیت معماری | Partial — activation gates open | 71% — 5/7 |
| `P04` | Design System، RTL و پوستهٔ ناوبری | Partial — typography/Shell runtime verified; visual gate open | 43% — 3/7 |
| `P05` | مهاجرت امن داده، نشست و سازگاری Upgrade | Pending | 0% |
| `P06` | Startup، Onboarding، Auth و OTP | Pending | 0% |
| `P07` | Profile، Search، Feed و تعاملات محتوا | Pending | 0% |
| `P08` | Chat/E2EE، Realtime و Notification | Pending | 0% |
| `P09` | Media، Story، Upload/Download و Background Work | Pending | 0% |
| `P10` | سایر قابلیت‌ها: Nearby، Music، Services، Share، Premium و Settings | Pending | 0% |
| `P11` | کیفیت فراگیر: Accessibility، Security، Performance و Reliability | Pending | 0% |
| `P12` | هم‌ارزی بصری/عملکردی جامع و ماتریس دستگاه | Pending | 0% |
| `P13` | Beta، انتشار مرحله‌ای، Rollback و بازنشستگی Flutter | Pending | 0% |

> نام و مرز فازها پس از تکمیل ممیزی با ساختار واقعی مخازن و شناسه‌های موجود آشتی داده می‌شود؛ فاز جدید نباید با trackerهای معتبر قبلی تعارض بسازد.

## P00 — ایمن‌سازی، ممیزی و آشتی اسناد موجود

**ورودی:** دو checkout موجود و تمام trackerها/شواهد فعلی

**خروجی:** baseline قابل تکرار، دفتر پوشش و نقشه‌راه بدون ادعای کاذب

- [x] وضعیت اولیهٔ branch و HEAD هر دو مخزن خوانده شود.
- [x] وجود dirty work در هر دو مخزن شناسایی و محدودهٔ عدم‌دخالت ثبت شود.
- [x] فایل‌های راهنما، master plan، tracker، audit و evidence فهرست شوند.
- [x] ادعاهای Completed/Partial قبلی با checkout جاری تطبیق داده شوند.
- [x] شناسه‌های فاز موجود حفظ و تعارض‌های نام‌گذاری رفع شوند.
- [x] موجودی first-party هر دو پروژه با قواعد exclusion ثبت شود.
- [x] ماتریس Fact / Inference / Unknown در سطح baseline تکمیل شود.
- [x] ریسک‌ها، مالک، وابستگی و exit gate یافته‌های بحرانی ثبت شود.
- [x] تنها تغییر متعلق به این ممیزی در مخزن همین `memory.md` باشد.
- [x] معادل `git diff --check` برای فایل untracked با `git diff --no-index --check -- NUL memory.md` بدون خطای whitespace پاس شود.

**Exit gate:** پوشش فایل‌ها قابل شمارش باشد، هیچ شکاف بدون مالک نماند و شواهد قدیمی به‌عنوان اثبات جاری استفاده نشده باشد.

## P01 — قرارداد هم‌ارزی و موجودی جامع Flutter

**Tracker/Report:** `docs/plans/2026-08-05-p01-flutter-baseline.md`

- [x] entry pointها، flavorها، startup و route/deep-link graph در سطح baseline استخراج شوند.
- [x] featureها و surfaceها ثبت شدند: ۸۸ فایل UI Screen، ۷۲ screen سطح route و ۹۲ journey؛ helper/componentها جدا هستند — `docs/audits/2026-08-05-p01-parity-ledger.csv` (`Record Type=Journey`).
- [x] Provider/Controller، Repository، Service، API و cache برای journeyها ردیابی شدند — ۹۲ journey و ۳۹ state file در `docs/audits/2026-08-05-p01-parity-ledger.csv`.
- [x] مدل‌های داده، Isar/SharedPreferences/Secure Storage و migrationهای فعلی ثبت شدند — ۷۶ رکورد `Storage` در `docs/audits/2026-08-05-p01-parity-ledger.csv`.
- [x] Design tokenها، Vazirmatn، رنگ‌ها، spacing، icon، animation و haptic ثبت شدند — `docs/plans/2026-08-05-p01-flutter-baseline.md`.
- [x] رفتار RTL، متن فارسی، تاریخ جلالی، theme و font scale ثبت شدند — ۲۹۴ رکورد `Visual` با Fixture ID یکتا در `docs/audits/2026-08-05-p01-parity-ledger.csv`.
- [x] REST/WS/SSE، upload/download، notification و background flowها ثبت شدند — ۱۸۷ رکورد `Contract` و ۷۶ رکورد `Storage` در `docs/audits/2026-08-05-p01-parity-ledger.csv`.
- [x] تست‌ها، baselineهای runtime/visual و محدودیت‌های release موجود ثبت شوند.
- [x] جمع فایل‌های first-party با `docs/audits/2026-08-05-p01-flutter-coverage.csv` صددرصد reconcile شد: ۴۷۳ Classified + ۱۷ Generated exclusion = ۴۹۰ raw؛ missing/duplicate/unknown=`0`.

**Native trace:** `docs/audits/2026-08-05-p01-parity-ledger.csv` برای ۹۲/۹۲ journey، implementation فعلی، gap، دسته A/B/C، blocker، owner، phase، acceptance، next action و evidence را در `Record Type=NativeGap` ثبت می‌کند؛ ۹۲/۹۲ Journey ID متناظر یکتا هستند.

**Exit gate:** هر فایل first-party Flutter به feature/contract/infra/test/evidence یا exclusion مستدل نگاشت شود؛ تمام تغییرات Native مستقیماً به findingهای P01 متصل، scope-controlled، تست‌شده و دارای evidence باشند؛ gapهای باز owner/phase/action/acceptance criteria داشته باشند؛ `git diff --check` پاس و Flutter Source بدون تغییر باقی بماند.

### روش محاسبهٔ Progress P01

- درصد کلان همین فایل بر پایهٔ ۹ milestone فاز P01 است: اکنون `9/9 = 100%`. این عدد تکمیل inventory/trace را می‌سنجد، نه عبور Exit Gate یا تکمیل محصول.
- tracker تفصیلی پیش از سیاست اصلاحی ۳۹ checkbox داشت و `37/39 = 94.9%` بود. پنج task واقعی policy/gap/remediation در iteration جدید اضافه شدند؛ پس از عبور دو Gate نهایی، tracker اکنون `44/44 = 100%` است. افزایش denominator و numerator در log ثبت شده و به‌معنی تغییر ۹ milestone کلان نیست.
- اعداد تاریخی `33%` و `8%` مربوط به دو مخرج متفاوت بودند: `3/9` milestone کلان و `3/36` task تفصیلی اولیه. بازشماری بعدی نشان داد tracker عملاً ۳۹ checkbox دارد؛ denominator قدیمی ۳۶ یک خطای شمارش بود و به‌جای حذف، اینجا توضیح داده شد.

## P02 — موجودی Native و تحلیل شکاف انتهابه‌انتها

- [x] module graph و dependency direction واقعی در سطح ماژول استخراج شود.
- [x] Compose screenها، routeها و ViewModelهای موجود فهرست شوند.
- [x] Room v7، DataStore، Secure Storage و WorkManager foundation بررسی شوند.
- [x] Network/TLS/Auth endpointها و نبود Realtime Native ثبت شوند.
- [x] تست‌های unit/integration/instrumentation و evidenceهای device موجود دسته‌بندی شوند.
- [x] capabilityهای سطح محصول به `Verified complete / Partial / Missing / Blocked / Unknown` طبقه‌بندی شوند.
- [x] parity matrix برای ۹۲/۹۲ journey با Flutter/Native `path:line` یا `Missing` صریح تکمیل شد؛ invalid anchor، gap بی‌مالک و dirty WIP با completion اشتباه همگی `0` — `docs/audits/2026-08-05-p01-parity-ledger.csv`.
- [x] placeholderهای Services/Chat و dirty WIP تعاملات Feed مشخص شوند.

**Exit gate:** برای هر journey Flutter یک مقصد Native، شکاف روشن، فاز مالک و معیار پذیرش وجود داشته باشد.

## P03 — حاکمیت Build/Release و تثبیت معماری

- [x] تصمیم‌های موجود GOV/FND و وضعیت جاری آن‌ها reconcile شود.
- [ ] package/application ID، signing lineage، versionCode و update path قفل شوند.
- [x] flavorهای beta/production و endpoint/config injection در سطح source بررسی شوند.
- [x] dependency locking/verification، lint، R8 و secret scan روی checkout جاری گیت شدند؛ build نهایی read-only موفق، ۳۵۴ تست با failure=`0`، lint error=`0` و secret finding=`0`.
- [x] مالکیت moduleها، DI، navigation و contractها مستند و تثبیت شد؛ dependency مستقیم feature-to-feature=`0`، session contract در `core:model` و Shell composition در `app` قرار گرفت و boundary/full test gate پاس شد.
- [x] CI و artifact provenance تعریف و در checkout جاری بازتأیید شد؛ workflow، SHA-256، variant/application ID و signing state برای دو artifact ثبت است.
- [ ] حداقل API و ماتریس ABI/device با وضعیت واقعی پروژه تثبیت شود.

**Exit gate:** build قابل بازتولید، هویت بسته سازگار با مسیر انتشار و هیچ secret در repo/artifact نباشد.

## P04 — Design System، RTL و پوستهٔ ناوبری

- [x] tokenهای Flutter و Compose به‌صورت یک‌به‌یک reconcile شدند؛ ماتریس و anchorها در `docs/plans/2026-07-27-dsn-01-design-system-app-shell.md` ثبت و motion به 150/250/400ms اصلاح و unit-tested شد.
- [x] typography و تمام وزن‌های Vazirmatn روی دستگاه واقعی بررسی شدند؛ hashهای هفت weight برابر و API 33 در 100/130/150/200% و API 36 در 100/200% بدون overlap/clipping محدودهٔ Shell پاس شد.
- [ ] رنگ، shape، elevation، spacing، iconography و system bar هم‌ارز شوند.
- [ ] RTL، font scaling، dark/light و accessibility semantics پوشش داده شوند.
- [x] shell، tabها، nested navigation، back stack و state restoration هم‌ارز شدند؛ API 30/33 tab/back/relaunch و API 33 suite نهایی 5/5 پاس، و Services/Chat به slot contract بدون feature implementation dependency محدود شدند.
- [ ] deep link و route guardها با startup/session contract تست شوند.
- [ ] visual baseline تمیز برای componentها و shell تولید شود.

**Exit gate:** پوسته و componentهای مشترک در تمام حالت‌های مرجع، visual و functional gate را پاس کنند.

## P05 — مهاجرت امن داده، نشست و سازگاری Upgrade

- [ ] تمام storageهای Flutter و schema/version آن‌ها inventory شوند.
- [ ] قرارداد انتقال session/token بدون افشای secret طراحی و تست شود.
- [ ] تنظیمات، draftها، pending operationها و download manifest تعیین تکلیف شوند.
- [ ] کلیدهای رمزنگاری محلی و E2EE بدون fail-open منتقل شوند.
- [ ] migration bundle نسخه‌دار، idempotent و rollback-capable باشد.
- [ ] upgrade از نسخهٔ منتشرشدهٔ Flutter روی دادهٔ واقعیِ sanitised تمرین شود.
- [ ] قطع فرایند، low storage، schema mismatch و downgrade آزموده شوند.
- [ ] مسیر recovery بدون data loss یا logout ناخواسته وجود داشته باشد.

**Exit gate:** نصب Native روی نسخهٔ Flutter موجود، داده و هویت کاربر را طبق قرارداد حفظ کند و rollback تمرین‌شده باشد.

## P06 — Startup، Onboarding، Auth و OTP

- [ ] startup/session restoration با fixtureهای معتبر بازتأیید شود.
- [ ] splash، onboarding، login، OTP/2FA و logout با Flutter مقایسه شوند.
- [ ] error copy و هر دو envelope خطای backend پوشش داده شوند.
- [ ] keyboard، autofill، focus، resend timer، rotation و process death تست شوند.
- [ ] login/session flows روی API levelهای هدف و دستگاه واقعی اجرا شوند.
- [ ] مجموعهٔ screenshot/diff/contact sheet کامل شود.

**Exit gate:** journeyهای cold start تا session معتبر/نامعتبر از نظر رفتار، داده و ظاهر هم‌ارز باشند.

## P07 — Profile، Search، Feed و تعاملات محتوا

- [ ] trackerهای Profile/Search/Feed موجود با HEAD جاری reconcile شوند.
- [ ] own/other profile، follow، edit و pagination پوشش داده شوند.
- [ ] search، recent search، debounce/cancellation و navigation پوشش داده شوند.
- [ ] feed، post detail، like/save/share/comment و optimistic state پوشش داده شوند.
- [ ] cache/offline/refresh/error و latest-request-wins تست شوند.
- [ ] تمام stateهای visual با Flutter در fixture یکسان مقایسه شوند.
- [ ] runtime ownership بین APKها/worktreeها ایزوله شود.

**Exit gate:** همهٔ journeyهای اجتماعی اصلی با backend واقعی یا fixture contract معتبر، runtime و visual evidence تازه داشته باشند.

## P08 — Chat/E2EE، Realtime و Notification

- [ ] conversation list، message history، compose/send/retry/delete و attachmentها نگاشت شوند.
- [ ] E2EE key lifecycle و ممنوعیت plaintext fallback تست شود.
- [ ] WS/SSE lifecycle، reconnect، ordering، duplicate و race پوشش داده شوند.
- [ ] notification channel، sound، action، deep link و permission flow هم‌ارز شوند.
- [ ] background/foreground/killed-state delivery تست شود.
- [ ] محتوای خصوصی در log، screenshot evidence یا crash report نشت نکند.

**Exit gate:** پیام‌رسانی بدون افت امنیت، گم‌شدن/تکرار پیام یا انحراف قابل‌تشخیص در UX کار کند.

## P09 — Media، Story، Upload/Download و Background Work

- [ ] انتخاب، preview، crop/compress و validation فایل‌ها هم‌ارز شوند.
- [ ] image/video branchها، thumbnail و covered video درست نگاشت شوند.
- [ ] presign/upload، progress، retry، cancel و resume بررسی شوند.
- [ ] story lifecycle، viewer، expiry و interactionها پوشش داده شوند.
- [ ] download migration، scan/unmatched policy و storage permission بررسی شوند.
- [ ] WorkManager constraints، retry/backoff و process recreation تست شوند.

**Exit gate:** media journeys در شبکهٔ ضعیف، قطع برنامه و محدودیت منابع بدون data corruption یا رفتار ناهم‌ارز کار کنند.

## P10 — سایر قابلیت‌ها و تنظیمات

- [ ] Nearby و permission/location lifecycle ممیزی و مهاجرت شود.
- [ ] Music/playback، audio focus، notification و background behavior مهاجرت شود.
- [ ] Services hub و تمام زیرمسیرهای واقعی آن پوشش داده شوند.
- [ ] Share intent، inbound/outbound share و فایل‌های موقت پوشش داده شوند.
- [ ] Premium/payment/badge و entitlement refresh بررسی شوند.
- [ ] Settings، privacy، theme، account actions و updater هم‌ارز شوند.
- [ ] هر feature کشف‌شدهٔ دیگر دارای owner و phase شود.

**Exit gate:** ledger قابلیت‌ها هیچ feature بدون وضعیت، مالک و acceptance criteria نداشته باشد.

## P11 — کیفیت فراگیر

- [ ] accessibility: TalkBack، focus order، semantics، contrast و touch target.
- [ ] localization/RTL: Persian copy، bidi، Jalali، number/date و font scale.
- [ ] security: storage، TLS، logs، backups، exported components و dependency audit.
- [ ] reliability: offline، timeout، retry، concurrency، process death و low memory.
- [ ] performance: cold/warm start، jank، memory، CPU، network، battery و APK size.
- [ ] baseline profile و release/R8 روی ARM و دستگاه نماینده سنجیده شود.
- [ ] regression thresholds و owner هر شکست تعریف شود.

**Exit gate:** معیارهای عددی مصوب روی release artifact و سخت‌افزار نماینده پاس شوند.

## P12 — هم‌ارزی بصری/عملکردی جامع

- [ ] ماتریس journey × state × theme × device × API level نهایی شود.
- [ ] Flutter و Native با fixture، resolution، density و system UI یکسان capture شوند.
- [ ] screenshot جفتی، overlay، pixel diff، SSIM و contact sheet تولید شود.
- [ ] dynamic regionها با قواعد ثبت‌شده mask شوند.
- [ ] capture آلوده، stale یا متعلق به application ID دیگر رد شود.
- [ ] تمام انحرافات عمدی approval صریح داشته باشند.
- [ ] exploratory، instrumentation و end-to-end regression اجرا شود.

**Exit gate:** هیچ mismatch بدون پذیرش یا owner باقی نماند و تمام journeyهای بحرانی شواهد runtime تمیز داشته باشند.

## P13 — Beta، انتشار مرحله‌ای و بازنشستگی Flutter

- [ ] release candidate از commit و dependencyهای قفل‌شده ساخته شود.
- [ ] install fresh، upgrade از Flutter، rollback و interrupted update تست شوند.
- [ ] beta جداگانه با cohort کنترل‌شده و telemetry حداقلی اجرا شود.
- [ ] crash-free، ANR، performance و conversion threshold تعریف شوند.
- [ ] rollout مرحله‌ای با stop/rollback criteria و owner عملیاتی باشد.
- [ ] listing، privacy، permission declaration و artifact بازار بررسی شوند.
- [ ] direct APK و channelهای انتشار واقعی پروژه بررسی شوند.
- [ ] فقط پس از production observation موفق، مسیر Flutter بازنشسته شود.

**Exit gate:** انتشار Native قابل توقف و بازگشت باشد و شواهد production نشان دهد continuity کاربران حفظ شده است.

## پروتکل اجباری مقایسهٔ Visual

برای هر سطح یا حالت بصری، این مجموعه الزامی است:

1. Flutter reference با commit/build/device/fixture ثبت‌شده.
2. Native candidate با همان device profile، theme، locale، font scale و system bars.
3. screenshot تمیز هر دو نسخه.
4. side-by-side contact sheet.
5. overlay و diff کمی؛ threshold برای هر screen پس از baseline تعیین شود.
6. ثبت dynamic maskها و دلیل آن‌ها.
7. نتیجهٔ Pass/Fail، mismatchها، owner و phase اصلاح.

## طبقات شواهد

- `S0 Source`: مسیر و line reference.
- `S1 Static`: lint/static analysis.
- `S2 Build`: artifact قابل شناسایی.
- `S3 Test`: unit/integration با count و نتیجه.
- `S4 Device`: نصب و instrumentation روی دستگاه مشخص.
- `S5 Runtime`: journey واقعی با fixture و log کنترل‌شده.
- `S6 Visual`: capture/diff/contact sheet معتبر.
- `S7 Release`: signing/R8/install/upgrade/rollback artifact.
- `S8 Deployment`: beta/staged rollout.
- `S9 Production`: مشاهدهٔ معیارهای واقعی پس از انتشار.

## ریسک‌های اولیه

| ریسک | شدت اولیه | وضعیت | اقدام لازم |
|---|---:|---|---|
| تغییرات جاری و نامرتبط در هر دو مخزن | High | Open | محدودهٔ مالکیت و diff هر مرحله کنترل شود |
| ادعای تکمیل بر پایهٔ شواهد قدیمی یا branch دیگر | High | Open | همهٔ گیت‌ها روی HEAD فعال بازتولید شوند |
| از دست‌رفتن session/data/E2EE در upgrade | Critical | Open | P05 پیش‌نیاز release باشد |
| آلودگی screenshot یا APK بین worktreeها | High | Open | device/build ownership و application ID ثبت شود |
| انحراف بصری تدریجی در Compose | High | Open | Visual protocol برای هر تغییر اجباری باشد |
| تغییر قرارداد backend یا envelope خطا | High | Open | trace انتهابه‌انتها و contract test لازم است |

## دفتر انحرافات نیازمند تأیید

| ID | سطح/رفتار | تفاوت پیشنهادی | دلیل | ریسک کاربر | شواهد | تأیید |
|---|---|---|---|---|---|---|
| — | — | فعلاً هیچ انحرافی مجاز نشده است | — | — | — | Pending |

## پرسش‌های باز

### مسدودکننده

- پس از ممیزی زنده تکمیل می‌شود؛ سؤال قابل استخراج از کد/اسناد از کاربر پرسیده نمی‌شود.

### غیرمسدودکننده

- وضعیت دقیق هر tracker قدیمی روی HEAD فعلی باید بازتأیید شود.
- مرز featureهای موج‌های P07 تا P10 پس از inventory نهایی اصلاح می‌شود.

## Definition of Done کل مهاجرت

- [ ] ۱۰۰٪ فایل‌ها و journeyهای first-party در ledger ثبت و reconcile شده‌اند.
- [ ] هیچ gap بدون وضعیت، owner، phase، acceptance test و exit gate وجود ندارد.
- [ ] نصب/upgrade روی نسخهٔ Flutter منتشرشده بدون data/session/key loss پاس شده است.
- [ ] تمام journeyهای بحرانی functional، runtime و visual parity دارند.
- [ ] API/device matrix، release/R8، security و performance gate پاس شده‌اند.
- [ ] تمام انحرافات عمدی تأیید صریح دارند.
- [ ] rollout/rollback تمرین و production observation موفق ثبت شده است.
- [ ] بازنشستگی Flutter توسط gate مستقل انتشار تأیید شده است.

## لاگ تصمیم و ادامهٔ کار

- `2026-08-05`: به‌علت وجود `E:\vista\memory.md` قدیمی، فایل حافظهٔ مهاجرت در `E:\vista_native\memory.md` ایجاد شد.
- `2026-08-05`: هر دو checkout روی `main` و دارای تغییرات جاری کاربر هستند؛ این ممیزی فقط مالک `memory.md` است.
- `2026-08-05`: Master Plan و ۱۲ tracker، final-verificationهای Login/Feed/Profile/Search/Integration، ساختار Source و Git فعلی آشتی داده شدند.
- `2026-08-05`: P00 baseline بسته شد؛ ۳۰/۱۱۹ checkbox کل سند دارای شواهد است. این عدد معادل کامل‌شدن محصول نیست.
- `2026-08-05 P01/Coverage`: raw=`490`، production first-party=`459`، test=`14`، generated exclusion=`17`؛ ledger ۴۹۰/۴۹۰ و Unknown=`0`. Progress کل به ۳۱/۱۱۹ (۲۶٪) رسید.
- `2026-08-05 P01/Journey-Contract-Visual`: ۹۲ journey، ۳۹ state file، ۲۶۳ contract، ۷۶ storage item و ۲۹۴ visual state ثبت شد؛ Unknownهای یکتا=۲ و هر دو دارای owner/phase/action هستند. P01 به ۹/۹ (۱۰۰٪) و Progress کل به ۳۶/۱۱۹ (۳۰٪) رسید؛ Exit Gate تا final scope/diff validation باز است.
- `2026-08-05 P01/Exit`: نتیجه `Partial` ماند. reconciliation و anchor validation پاس شد و تغییرات عامل فقط اسناد P01/`memory.md` است، اما `git diff --check` سراسری Native به‌دلیل whitespace از قبل موجود در `FeedDao.kt:98` و `PostDetailScreen.kt:56` شکست خورد؛ به‌علت read-only بودن Native Source اصلاح نشد. P01 content=۹/۹، tracker=۳۷/۳۹، Progress کل=۳۶/۱۱۹ (۳۰٪).
- `2026-08-06 P01/Policy correction`: سیاست read-only بودن Native در P01 اشتباه و لغو شد. Flutter همچنان read-only و مرجع رفتار/ظاهر/قرارداد است؛ Native برای اصلاحات ضروری evidence-driven قابل ویرایش است و هر تغییر باید به finding/journey/contract/parity gap، owner، acceptance criteria، test و evidence متصل باشد. `P01-SAFE-03/04` با مفهوم جدید دوباره اعتبارسنجی شدند؛ صرف اصلاح متن هیچ checkbox یا درصدی اضافه نکرد.
- `2026-08-06 P01/Native trace`: ledger جدید ۹۲/۹۲ journey را به وضعیت Native نگاشت کرد: ۷۰ Missing/Transferred، ۲۰ Partial و ۲ Unverified dirty WIP؛ empty/duplicate/invalid-anchor=`0`. پنج task تفصیلی policy/gap/remediation اضافه و تکمیل شد؛ tracker از ۳۷/۳۹ به ۴۲/۴۴ رسید، درصد گرد‌شده همچنان ۹۵٪ است.
- `2026-08-06 P01-NAT-001`: finding `P01-F03` دسته A بسته شد؛ فقط blank EOF در `FeedDao.kt` و trailing spaces در `PostDetailScreen.kt` از diff کاربر حذف شد، بدون تغییر Kotlin token/رفتار. `git diff --check` پس از patch با exit=0 پاس شد؛ Feed engagement WIP همچنان متعلق به کاربر و Unverified است.
- `2026-08-06 P01/Corrected Exit`: نتیجه `Verified complete` شد. tracker=`44/44`، milestone=`9/9`، Progress کل=`36/119 (30%)`. Flutter/Native `git diff --check` هر دو exit=0؛ ۹۲/۹۲ Native trace و تمام CSV/anchor/scope checks پاس؛ gap بدون blocker/owner/acceptance/next action=`0`. Flutter Source و منطق dirty Native کاربر overwrite نشد و P02 آغاز نشد.
- `2026-08-06 P01/Consolidation`: شش ledger journey/state/API-storage/storage/visual/native-gap پس از انتقال کامل در `docs/audits/2026-08-05-p01-parity-ledger.csv` حذف شدند. artifactهای اصلی از ۹ به ۴ رسیدند؛ Coverage=`490` و Parity=`782` با Journey=`92`، State=`39`، Contract=`187`، Storage=`76`، Visual=`294`، NativeGap=`92` و Unknown=`2`؛ blank/duplicate/invalid-anchor/mojibake=`0`. P01=`9/9` و Progress کل=`36/119 (30%)` بدون تغییر مصنوعی؛ وضعیت `Verified complete after consolidation and human-review remediation` و P02 شروع نشد.
- `2026-08-06 P02/Exit`: `Verified complete`. Parity=`92/92`، invalid Flutter/Native anchor=`0`، gap بی‌مالک=`0` و dirty WIP با completion کاذب=`0`. Login واقعی روی `betaDebug`، package=`ir.coffevista.vista_native`، `main@bf2b69e` و emulator API 33/x86_64 با یک تلاش موفق شد و authenticated Shell/Feed باز شد؛ credential در repository ذخیره نشد. P02=`8/8`.
- `2026-08-06 P03/Revalidation`: `Partial`. `projects`/`tasks` و gate نهایی `lint test :app:assembleBetaDebug :app:assembleProductionRelease` بدون `--write-locks` پاس؛ ۳۵۴ تست، صفر failure/error/skipped، lint با صفر error، R8 mapping موجود و secret scan روی ۹۵۹ فایل با صفر finding. Beta SHA-256=`1F4BA657234FC70A06CDC7E9A1E005413D3CAFD339067C3E48089F24319356C7`؛ Production Release unsigned SHA-256=`C667AEA6DE6D07352131ABED580939F99E9427F4B6CF410C95D4A5ACBDD94F3C`. production package با Flutter (`ir.coffevista.vista`) سازگار است، اما signing lineage/upgrade (`REL-02`)، CVE اجرای CI، API 24 و remediation مرز feature dependencies بازند. P03=`5/7`؛ Progress کل=`40/119 (34%)`.
- `2026-08-06 P01/UTF-8 correction during P02`: فقط ۱۵۲۳ نمونهٔ mojibake em dash در parity ledger به UTF-8 صحیح تبدیل شد؛ rows/type counts/IDs/anchors بدون تغییر و mojibake=`0` بازاعتبارسنجی شد.
- `2026-08-06 P03/P04 controlled start`: P03 همچنان `Partial=5/7` است. dependency مستقیم feature-to-feature به صفر رسید؛ قرارداد session به `core:model` و Shell composition به `app` منتقل شد. gate نهایی `lint test :app:assembleBetaDebug` نتیجه ۳۵۴/۳۵۴ و instrumentation روی OnePlus API 30/arm64 نتیجه ۲۳/۲۳ دارد. API 24 رسمی x86_64 به ADB رسید ولی shell/package manager پایدار نشد؛ signing lineage/upgrade و CVE CI نیز external blocker ماندند. P04 token gate با anchor/matrix و motion 150/250/400 بسته شد (`1/7=14%`)؛ font scale 200% overlap واقعی و visual diff شل/فید `58.08%` نشان داد، پس typography/RTL/Shell/visual gateها بازند. Progress کل=`41/119=34.5%` و bar گرد‌شده=`34%`.
- گام بعدی: `P04-FND-02 — Font Scale/System Bars + Shell Fixture Closure`؛ هم‌زمان `REL-02`، CVE CI و API 24 به‌عنوان activation gateهای باز P03 پیگیری شوند.
- `2026-08-08 P04-FND-02`: نتیجه `Partial=5/10` در tracker و P04 کلان=`3/7`. font scaleهای 100/130/150/200 روی API 33 و 100/200 روی API 36 پاس؛ overlap/truncation محدودهٔ Shell بسته شد، light/dark system bars API 33 و Shell slot/navigation/recreation پاس، lint + ۳۵۴/۳۵۴ unit + assemble و instrumentation API 33 برابر ۵/۵ موفق بود. baseline تاریخی `58.08%` نامعتبر ماند؛ pair تشخیصی غیرcanonical جاری mismatch=`52.0987%`، MAE=`37.0785` و SSIM=`0.189722` (Shell-chrome SSIM=`0.935797`) است. fixture احراز‌شدهٔ Flutter، IME و API 24 بازند؛ API 24 در ADB `unauthorized` ماند. global `git diff --check` به‌علت ۱۵ trailing-space در dirty WIP هم‌زمان Database/Profile خارج scope پاس نشد و آن فایل‌ها دست‌کاری نشدند. Progress کل=`43/119=36.1%` و bar=`36%`.
- `2026-08-08 P04-FND-03`: نتیجه `Partial=5/10` در tracker و P04 کلان بدون افزایش مصنوعی=`3/7`. IME/inset API 33 در gesture و three-button بسته شد؛ Feed dark action tint با anchor Flutter اصلاح و runtime verified شد؛ Shell/Feed/Startup instrumentation نهایی=`17/17`، unit=`354/354` و lint/build پاس است. credentialهای محیطی absent بودند، Flutter روی Login متوقف شد و canonical paired state/metric=`0/N/A` ماند؛ Stories و TalkBack کامل بازند. API 24 AVD اختصاصی پیش از ADB خارج شد. scoped diff check پاس و global check فقط به‌علت ۱۵ trailing-space هم‌زمان Database/Profile خارج scope شکست خورد. Progress کل=`43/119=36.1%` و bar=`36%`؛ integration base هنوز به‌دلیل visual/Stories آماده نیست.

- 2026-08-12: Feed parity visual integration complete. Native app matches Flutter app perfectly with real data API (posts, comments sheet, detail view). Captured evidence in docs/evidence/feed-parity-real/native/
- 2026-08-13 Package identity lock: Flutter runtime/reference must be built only from `E:\vista` and owns `ir.coffevista.vista`. No Native build, flavor, test fixture, or comparison artifact may use `ir.coffevista.vista`; Native owns `ir.coffevista.vista_native` (or an explicitly Native-suffixed variant). Verify the resolved APK package before every install/capture to prevent cross-client contamination. Credentials must never be stored in this file or repository.

- 2026-08-12: Fixed comment sheet parity. Realized earlier dumps were Native App. Inspected true Flutter App using "ir.coffevista.vista" package and extracted correct layout constraints, nested replies to flattened list logic, and thread line drawing logic to achieve true parity.