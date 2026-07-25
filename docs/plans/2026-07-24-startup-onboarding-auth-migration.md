# مهاجرت Native مسیر شروع، آن‌بوردینگ و ورود

**Type:** feature
**Status:** done
**Progress:** `[██████████] 100%`

## Problem / Goal
برش اول مهاجرت Vista باید فقط مسیر شروع برنامه، بازیابی نشست، آن‌بوردینگ نسخه‌دار و ورود با رمز یا OTP را از Flutter به Android Native منتقل کند. تصاویر و نشان‌های همین مسیر باید عیناً از نسخه Flutter استفاده شوند و پس از ورود، یک صفحهٔ خالی فقط نام کاربر و پیام خوش‌آمدگویی را نشان دهد. خروجی باید قابل build و test باشد و هیچ قابلیت Home، Chat، Profile یا بخش آینده‌ای را پیشاپیش پیاده‌سازی نکند.

## Root cause / Design
پروژه Native در شروع این کار هیچ سورس production یا entry point نداشت و فقط یک تنظیمات اولیه Gradle داشت. رفتار مرجع از `E:\vista\lib\features\auth\widgets\session_auth_wrapper.dart:42`، `E:\vista\lib\features\onboarding\screens\onboarding_screen.dart:18` و `E:\vista\lib\features\auth\screens\auth_wizard_screen.dart:92` استخراج شد. طراحی انتخابی شامل Compose UI، state/action تغییرناپذیر، ViewModel، repository contract، HTTP data source، preference آن‌بوردینگ و secure session store است؛ تزریق وابستگی با یک container کوچک application-level انجام می‌شود تا framework یا ماژول آینده‌نگر اضافه نشود.

## Phases

### Phase A — Read-only discovery
- [x] ثبت baseline: `E:\vista_native` فاقد `.git` و سورس production است
- [x] ثبت baseline Flutter: شاخه `main` با تغییرات حل‌نشده کاربر؛ فقط read-only
- [x] ردیابی startup → session restore → onboarding/auth در Flutter
- [x] ردیابی قراردادهای `/v1/auth` و `/api/v1/system/status` در backend
- [x] ثبت ابهام‌ها، مرز scope و پایه‌های مشترک لازم در اسناد migration

### Phase B — Minimal architecture foundation
- [x] اصلاح حداقلی dependencyهای غیرقابل resolve و فعال‌سازی Compose
- [x] افزودن model/result/error taxonomy و پیام‌های فارسی
- [x] افزودن onboarding preference و secure session storage
- [x] افزودن HTTP data source، repository contract و container کوچک
- [x] unit test پایه‌های تصمیم‌گیری و build فاز

### Phase C — Splash and startup routing
- [x] پیاده‌سازی startup state و ViewModel
- [x] پیاده‌سازی splash، maintenance و retry
- [x] پیاده‌سازی session restore با تمایز خطای terminal و transient
- [x] تست ماتریس deterministic routing و build فاز

### Phase D — Onboarding
- [x] پیاده‌سازی سه اسلاید RTL، ردکردن، بعدی و قبلی
- [x] persistence نسخه `1.0.0` و رفتار relaunch
- [x] تست state/relaunch/back و build فاز

### Phase E — Login
- [x] پیاده‌سازی identifier lookup و validation
- [x] پیاده‌سازی password login و نمایش/پنهان‌سازی رمز
- [x] پیاده‌سازی OTP پنج‌رقمی، resend timer و 2FA handoff
- [x] persistence اتمیک نشست، خطاهای فارسی و success navigation
- [x] تست ViewModel/repository و build فاز

### Phase F — Parity and hardening
- [x] انتقال لوگوهای splash/auth و سه تصویر onboarding از Flutter به منابع Native
- [x] جایگزینی placeholderهای ترسیمی با تصاویر و آیکون‌های متناظر Flutter
- [x] نگهداری امن username/full_name در نشست و نمایش نام واقعی پس از login/restore
- [x] ساده‌سازی مقصد authenticated به صفحهٔ خالی خوش‌آمدگویی
- [x] اجرای unit tests، lint و assembleDebug
- [x] بررسی RTL، back navigation و stateهای loading/error/retry
- [x] به‌روزرسانی اسناد parity و معماری با نتیجه واقعی
- [x] ثبت محدودیت runtime و پیشنهاد برش بعدی بدون پیاده‌سازی آن

## Decisions & Notes
- `E:\vista_native` هنگام شروع Git repository نبود؛ بنابراین branch/status و diff مبتنی بر Git برای این مخزن ممکن نیست. فهرست اولیه فقط شامل فایل‌های Gradle، manifest/resources و دو Example test بود.
- build پایه پیش از تغییر production شکست خورد: `androidx.core:core-ktx:1.19.0` resolve نشد. اصلاح نسخه فقط برای build این برش مجاز است و upgrade گسترده انجام نمی‌شود.
- Google Maven در محیط اجرا برای artifactهای موجود AndroidX پاسخ 404 داد؛ mirror محدود به گروه‌های Android/Google و پس از repository رسمی به‌عنوان fallback build اضافه شد.
- `E:\vista` روی `main` تغییرات کاربر در auth/chat/services/onboarding و assets داشت. هیچ‌کدام ویرایش نخواهد شد.
- مقصد پس از احراز هویت یک `AuthenticatedBoundary` محدود است؛ Home/Profile در این برش پیاده‌سازی نمی‌شود.
- بازیابی نشست از رفتار Flutter پیروی می‌کند: فقط 401/403 refresh نشست را terminal می‌کند؛ خطای شبکه/5xx نباید token را پاک کند.
- profile setup و mandatory password در Flutter پس از auth قرار دارند، اما implementation کامل آن‌ها خارج scope است. native فقط نتیجه `profile_completed/password_required` را در مرز authenticated گزارش می‌کند.
- TLS پویا در Flutter از system status تغذیه می‌شود. این برش از HTTPS و trust store سیستم استفاده می‌کند؛ pinning پویا تا وقتی contract و rollout مستقل آن انتخاب نشده، به‌عنوان زیرساخت speculative اضافه نمی‌شود.
- Gate فاز B: `gradlew --no-daemon clean testDebugUnitTest assembleDebug -Pkotlin.incremental=false` با `BUILD SUCCESSFUL` پاس شد.
- Gate فاز C: تست‌های first-run، returning signed-out، access تازه، refresh موفق، refresh terminal، offline fallback و maintenance پاس شدند؛ `assembleDebug` موفق بود.
- ADB در مسیرهای شناخته‌شدهٔ Android SDK پیدا نشد؛ runtime/device verification در این فاز در دسترس نبود.
- Gate فاز D: تست‌های next/previous/skip/completion persistence پاس شدند و APK با آن‌بوردینگ واقعی build شد.
- Gate فاز E: تست lookup→password، password success/error، new-user OTP→set-password و restored mandatory-password پاس شد؛ `assembleDebug` موفق بود.
- قرارداد backend در `internal/auth/service.go` فیلدهای `username` و `full_name` را در تمام پاسخ‌های login/OTP/2FA/refresh برمی‌گرداند؛ Native قبلاً آن‌ها را parse یا persist نمی‌کرد.
- منابع بصری این برش در Flutter به `vistalogo.png`، `vistalogo-new.png`، لوگوی روشن/تیره auth و سه تصویر `viu_connect/create/private.png` محدود می‌شوند.
- SHA-256 هر هفت تصویر Native با فایل منبع Flutter برابر است. خانواده Vazirmatn وزن‌های 300 تا 900 نیز از Flutter منتقل و روی Typography سراسری اعمال شد.
- Gate نهایی: `testDebugUnitTest` شامل ۲۰ تست با ۰ شکست، `lintDebug` با ۰ error و `assembleDebug` همگی `BUILD SUCCESSFUL` شدند.
- APK روی دستگاه `SM_N975F` نصب و `MainActivity` اجرا شد. hierarchy واقعی Compose صفحه ورود، لوگو، فیلد شناسه و دکمه را تأیید کرد؛ capture تصویری این emulator سیاه بود اما Activity foreground، hierarchy و logcat نبود crash را تأیید کردند.
- ورود زنده با حساب واقعی یا OTP انجام نشد، چون هیچ credential/شمارهٔ آزمایشی در scope ارائه نشده بود؛ مسیرهای password/OTP/2FA/new-user در unit test پوشش دارند و قراردادشان با backend تطبیق داده شد.
