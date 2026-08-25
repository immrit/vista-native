# جایگزینی Feed اصلی و تکمیل parity با داده واقعی

**Type:** feature
**Status:** in_progress
**Progress:** `[██████░░░░] 60%`

## Problem / Goal
Feed تکمیل‌شده در worktree `feed-parity-completion` باید به checkout اصلی Native منتقل شود، بدون اینکه WIP نامرتبط Chat/Profile/Search از بین برود یا دو نسخهٔ متناقض Feed باقی بماند. سپس Flutter و Native با حساب واقعی و یک پست/دیدگاه مشترک روی API 33 اجرا، تصویربرداری و ریزبه‌ریز هم‌تراز شوند.

## Root cause / Design
`main` در `E:\vista_native` تغییرات هم‌زمان و overlap مستقیم در Feed/Shell/Auth/Database دارد؛ بنابراین حذف worktree یا overwrite کور مجاز نیست. ادغام به‌صورت allowlist و semantic reconciliation انجام می‌شود: نسخه worktree منبع قطعی رفتار Feed است، قراردادهای جدید Auth/Database در main حفظ می‌شوند، و فقط پس از build/runtime/visual gate دوگانگی حذف می‌شود.

## Phases

### Phase 1 — Investigate
- [x] ثبت HEAD/status و مالکیت overlap بین main و worktree
- [x] مقایسه semantic تمام فایل‌های Feed/Comment/Shell/DAO و تعیین keep/replace/reconcile
- [x] ثبت وضعیت device، package، locale/theme/font-scale و نشست‌های واقعی
- [x] ورود امن Flutter و Native بدون ذخیره credential و انتخاب fixture مشترک

### Phase 2 — Implement
- [x] انتقال کامل Comment/API/Repository/ViewModel/UI از worktree به main
- [x] reconciliation Feed/PostDetail با Auth/Database/Shell جدید main
- [x] حذف worktree، شاخهٔ موقت و رفع دوگانگی؛ ادامهٔ کار فقط در `E:\vista_native`
- [ ] تکمیل Comments Flutter parity: geometry/header/list/thread/actions/composer/IME/error
- [ ] تکمیل menu/like/save/share/count sync و Post Detail با داده واقعی
- [x] به‌روزرسانی unit و instrumentation متناسب با قرارداد canonical

### Phase 3 — Verify
- [x] unit/contract tests Feed و Comments = 0 failure
- [ ] instrumentation API 33 = 0 failure؛ اجرای دستی ADB جای instrumentation محسوب نمی‌شود
- [x] `assembleBetaDebug` و ۳۲/۳۲ unit test Feed/Comments در ۲۰۲۶-۰۸-۱۳ موفق
- [ ] lint و static gates روی وضعیت نهایی
- [ ] runtime واقعی Flutter و Native برای Feed/Detail/Comments/menu/actions
- [x] Flutter-before و Native-before با fixture مشترک `raha.81` و PNG سالم capture شد
- [ ] Native-after نهایی، side-by-side، overlay/diff و contact sheet بدون mismatch تأییدنشده
- [ ] font scale 100/130/150/200، RTL و light/dark بررسی شود؛ استفاده از `sp` به‌تنهایی شاهد نیست
- [ ] API 24 smoke یا Blocked با شاهد تازه؛ targetSdk جای smoke API 24 نیست
- [ ] `git diff --check`، secret/fixture leakage و ownership audit پاک
- [x] tracker و `memory.md` با package lock و نقطه ادامهٔ واقعی به‌روز شدند

## Decisions & Notes
- قفل جدید پذیرش (2026-08-13): هیچ ورود یا آزمون runtime با حساب/session/post فیک مجاز نیست. اجرای instrumentation مبتنی بر `valid-session` متوقف شد و Pass پذیرش محسوب نمی‌شود. همهٔ شواهد بعدی فقط با نشست واقعی و یک پست دارای بیش از ۲ کامنت و حداقل یک reply واقعی ثبت می‌شوند.
- worktree منبع قطعی Feed است، اما فایل مشترک main فقط با reconciliation تغییر می‌کند.
- credential صرفاً برای ورود تعاملی استفاده می‌شود و در فایل، screenshot، shell history یا log نوشته نمی‌شود.
- تا زمانی که visual/runtime واقعی روی کد ادغام‌شده ثبت نشود، worktree حذف نمی‌شود و Done اعلام نمی‌گردد.
- RTK طبق `AGENTS.md` بررسی شد اما روی سیستم نصب/قابل‌اجرا نیست؛ فرمان‌ها با ابزارهای استاندارد ادامه می‌یابد.
- Flutter روی `emulator-5554`، API 33، رزولوشن 1080x2280 و font scale 1.0 با نشست واقعی capture شد؛ fixture مرجع پست `sssssss` با متن «آهااااای کسی صدای منو میشنوههههه» و 7 دیدگاه است.
- مرجع واقعی Flutter در لمس کامنت کارت، Post Detail را باز کرد و بعد از انتظار نیز فقط summary «7 دیدگاه» نشان داد؛ این رفتار runtime به‌عنوان شاهد ثبت شد و با قرارداد inline/pinned Native تطبیق داده می‌شود.
- compile canonical ماژول Feed با Gradle 8.13 آفلاین و خروجی ایزوله موفق شد: `BUILD SUCCESSFUL in 1m 1s`.
- تست واحد Feed/Comments و compile سطح app موفق شد: `BUILD SUCCESSFUL in 1m 58s`؛ assemble canonical نیز `BUILD SUCCESSFUL in 1m 21s`.
- APK canonical روی API 33 نصب و Feed با داده واقعی اجرا شد؛ بعداً DNS emulator قطع شد و runtime visual ادامه‌دار است، نه Pass نهایی.
- مقایسه سورس Flutter ثابت کرد Feed از bottom sheet با ارتفاع 85%، گوشه 20dp، عنوان «نظرات» و آیکن استفاده می‌کند و Detail فهرست inline + composer pinned دارد؛ Native بر همین مبنا اصلاح و مجدداً compile شد.
- تصاویر قدیمی `flutter_actual_feed.png`، `06-true-flutter-comment-sheet.png` و `28-comment-sheet-parity-flutter.png` به‌علت signature خراب PNG شاهد محسوب نمی‌شوند؛ capture تازه فقط با `adb shell screencap` و `adb pull` انجام می‌شود.
- مرجع تازه Flutter از APK ساخته‌شده در `E:\vista` با package=`ir.coffevista.vista` و SHA-256=`222CEA20F83844E29C664396FDB85EEA752A3783D9CA728163AFA97655972CA3` ثبت شد.
- تصاویر معتبر تازه: `E:\vista\tmp\flutter-feed-fresh.png`، `E:\vista\tmp\flutter-comment-sheet-fresh.png`، `E:\vista\tmp\native-feed-fresh.png` و `E:\vista\tmp\native-comment-sheet-after.png`؛ fixture مشترک پست `raha.81` و دیدگاه `re.mhd` است.
- mismatch واقعی Comments در Native-before: partial sheet، دو drag handle، composer خارج viewport، full name به‌جای username و زمان mock. در Native-after اول skip partial، یک handle، composer pinned، username و زمان واقعی اصلاح شد؛ surface/height/avatar composer هنوز در چرخهٔ build/capture بعدی است.
- package production Native از شناسهٔ رزروشدهٔ Flutter جدا و به `ir.coffevista.vista_native.production` تغییر کرد؛ beta همچنان `ir.coffevista.vista_native` است.
- نقطهٔ ادامه: build refinement دوم Comments، نصب APK beta، capture همان fixture، سپس اصلاح Feed chrome/card و بررسی Detail/menu/IME؛ حذف worktree فقط در انتهای این gates.
- `2026-08-13 reconciliation`: تمام سورس‌های Feed/Comment/Shell/DAO worktree با checkout اصلی مقایسه شدند؛ هیچ فایل سورس منحصربه‌فردی در worktree نمانده بود و ۱۰ فایل متفاوت در main نسخهٔ توسعه‌یافته‌تر داشتند. سپس worktree `feed-parity-completion`، پوشهٔ orphan و شاخهٔ `codex/feed-parity-completion` حذف شدند. شواهد قدیمی fixture به معیار پذیرش واقعی منتقل نشدند.
- نقطهٔ ادامهٔ فعلی: compile اصلاحات Comments در checkout اصلی، نصب beta با package امن، capture همان پست واقعی ۷ کامنتی دارای reply، سپس چرخهٔ IME/create/reply/delete و visual diff.
