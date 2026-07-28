# Vista Native Visual Review

تاریخ بررسی: 2026-07-28

وضعیت این سند فقط مربوط به Visual Review نسخه فعلی است. هیچ branch یا پیاده‌سازی از
`SLICE-01D` در این اجرا آغاز نشده است.

## Git Baseline

- Branch: `slice-01c/other-profile-follow`
- HEAD پیش از commit شواهد: `b19c6d4ecde8516f3fa19ee0be7ac520556ecd04`
- `git status --short`: پاک
- Staged: `0`
- Untracked: `0`
- `git diff --check`: پاس
- `.git/index.lock`: وجود نداشت
- branch جدید ساخته نشد.

## Package و Device

- Package نصب‌شده: `ir.coffevista.vista_native`
- Version: `1.0-beta` (`versionCode=1`)
- Application label: `ویستا`
- Launcher activity: `ir.coffevista.vista_native/.MainActivity`
- Production package مجزا در build: `ir.coffevista.vista`
- AVD: `Medium_Phone_2` (`sdk_gphone64_x86_64`)
- Android API: `33`
- Resolution: `1080x2400`
- Density: `420 dpi`
- Orientation: Portrait، rotation `0`
- Theme: Light

## Build و Runtime

Build نهایی پس از حذف کامل کنترل‌ها و test harness موقت capture اجرا شد:

```powershell
.\gradlew.bat :app:assembleBetaDebug --no-daemon --stacktrace `
  "-Pvista.isolatedBuildDir=true" `
  "-Pvista.isolatedBuildOutput=visual-review-final-build"
```

نتیجه: پاس. APK نهایی:

`E:\vista_native\.gradle\visual-review-final-build\app\outputs\apk\beta\debug\app-beta-debug.apk`

نصب و شناسایی package:

```powershell
adb -s emulator-5554 install -r app-beta-debug.apk
adb -s emulator-5554 shell cmd package resolve-activity --brief ir.coffevista.vista_native
```

نتیجه: `Success` و activity برابر
`ir.coffevista.vista_native/.MainActivity`.

Cold launch از Launcher:

```powershell
adb -s emulator-5554 shell am force-stop ir.coffevista.vista_native
adb -s emulator-5554 logcat -c
adb -s emulator-5554 shell monkey -p ir.coffevista.vista_native `
  -c android.intent.category.LAUNCHER 1
```

نتیجه: process فعال شد، focus روی `MainActivity` قرار گرفت و مسیر onboarding نمایش داده شد.

Cold launch با session معتبر debug:

```powershell
adb -s emulator-5554 shell am force-stop ir.coffevista.vista_native
adb -s emulator-5554 logcat -c
adb -s emulator-5554 shell am start -W `
  -n ir.coffevista.vista_native/.MainActivity `
  --es vista.foundation.fixture valid-session
```

نتیجه:

- `Status: ok`
- `LaunchState: COLD`
- `TotalTime: 3263 ms`
- process فعال و focus روی `MainActivity`
- UI hierarchy شامل `فید`، `خانه` و `CurrentRoute: feed_root`

Logcat پس از launch:

- Process logcat lines: `47`
- تطبیق با
  `FATAL EXCEPTION|ANR in|AndroidRuntime|SQLiteException|IllegalStateException|IllegalArgumentException|NoSuchMethodError|VerifyError|SerializationException`:
  `0`
- Crash buffer lines: `0`

تلاش اولیه build روی outputهای قبلی با خطای قفل فایل generated output متوقف شد؛
build نهایی با output تازه و ایزوله پاس شد. این مشکل تغییری در source برنامه ایجاد نکرد.

## Fixture و Release Isolation

تصاویر با داده contract-backed موجود و سناریوهای `valid-session` و
`offline-valid-session` گرفته شدند. برای stateهای سریع refresh و follow/unfollow یک delay
دوثانیه‌ای محدود به Debug و یک instrumentation harness موقت استفاده شد. پس از capture،
هر دو به‌طور کامل حذف شدند و build/runtime نهایی از source بدون آن‌ها پاس شد.

- `git grep` برای `visual-review-valid-session` و `VisualReviewScreenshot`: صفر
- `apkanalyzer dex packages` روی آخرین Beta Release برای fixture/debug capture classes:
  صفر تطبیق
- Release leakage: صفر

## Screenshot Inventory

مسیر تصاویر اصلی:

`docs/evidence/2026-07-28-visual-review/screenshots/`

همه تصاویر PNG، با ابعاد کامل `1080x2400`، Light theme، Portrait و از
`Medium_Phone_2 / API 33` هستند.

| Filename | Screen / State | Fixture scenario | Theme | Orientation | Device / API | نتیجه | مشکل بصری مشاهده‌شده |
|---|---|---|---|---|---|---|---|
| `01-startup.png` | Startup | `valid-session` با delay موقت Debug برای capture | Light | Portrait | Medium_Phone_2 / 33 | پاس؛ status/navigation bar کامل و متن فارسی واضح | لوگوی startup داخل یک مستطیل خاکستری بزرگ، placeholder-like و کم‌پرداخت دیده می‌شود. |
| `02-feed-first-page.png` | Feed، صفحه اول | `valid-session` | Light | Portrait | Medium_Phone_2 / 33 | پاس؛ داده و media واقعی fixture دیده می‌شود | دو عنوان `خانه` و `فید` فضای عمودی زیادی می‌گیرند؛ debug route در UI دیده می‌شود؛ username از نظر bidi بصری `fixture2@` دیده می‌شود. |
| `03-feed-scrolled.png` | Feed، پس از scroll و append | `valid-session` | Light | Portrait | Medium_Phone_2 / 33 | پاس؛ چند پست و حفظ Shell قابل‌مشاهده است | debug route فضای پایین content را اشغال کرده و usernameها bidi معکوس دارند. |
| `04-feed-refreshing.png` | Pull-to-refresh فعال | `valid-session` با delay موقت Debug | Light | Portrait | Medium_Phone_2 / 33 | پاس؛ refresh indicator فعال و از حالت عادی قابل‌تشخیص است | ساختار دو app-bar و debug route تراکم عمودی را کاهش می‌دهند. |
| `05-feed-offline-cache.png` | Feed آفلاین با cache | `offline-valid-session` پس از seed cache | Light | Portrait | Medium_Phone_2 / 33 | پاس؛ نشانگر `نمایش نسخه ذخیره‌شده` واضح است | media بخش عمده viewport را می‌گیرد و debug route در پایین UI باقی است. |
| `06-post-detail-image.png` | Post Detail تصویر | `valid-session` | Light | Portrait | Medium_Phone_2 / 33 | پاس؛ author، caption و تصویر دیده می‌شوند | counts در انتهای viewport clip شده‌اند؛ media تقریباً کل viewport را پر کرده و debug route/bottom navigation فضای محتوا را می‌گیرند. |
| `07-post-detail-video-thumbnail.png` | Post Detail ویدیو/thumbnail | `valid-session` | Light | Portrait | Medium_Phone_2 / 33 | پاس؛ thumbnail و badge ویدیو قابل‌تشخیص‌اند | counts زیر media دیده نمی‌شوند؛ hierarchy با app-bar Shell و detail متراکم/دوگانه است. |
| `08-own-profile.png` | Own Profile آنلاین | `valid-session` | Light | Portrait | Medium_Phone_2 / 33 | پاس؛ avatar، bio، badges، counts و logout دیده می‌شوند | username بصری `fixture@` دیده می‌شود و debug route در محصول نمایان است. |
| `09-own-profile-offline.png` | Own Profile آفلاین | `offline-valid-session` پس از seed cache | Light | Portrait | Medium_Phone_2 / 33 | پاس؛ cache بدون crash نمایش داده شد | با تصویر آنلاین تقریباً غیرقابل‌تشخیص است و stale/offline indicator ندارد؛ username bidi معکوس است. |
| `10-other-profile-not-following.png` | Other Profile، Not Following | `valid-session` | Light | Portrait | Medium_Phone_2 / 33 | پاس؛ دکمه Follow و counts واضح‌اند | بخش `پست‌های عمومی` صریحاً placeholder پیاده‌سازی‌نشده است؛ debug route و username bidi معکوس دیده می‌شوند. |
| `11-other-profile-pending-follow.png` | Other Profile، Pending Follow | `valid-session` با delay موقت Debug | Light | Portrait | Medium_Phone_2 / 33 | پاس؛ دکمه disabled و count خوش‌بینانه `43` state را متمایز می‌کند | placeholder بخش پست‌ها و debug route همچنان غالب‌اند. |
| `12-other-profile-following.png` | Other Profile، Following | `valid-session` | Light | Portrait | Medium_Phone_2 / 33 | پاس؛ label `دنبال می‌کنید` و count `43` روشن است | username bidi معکوس و بخش عمومی placeholder-like است. |
| `13-other-profile-pending-unfollow.png` | Other Profile، Pending Unfollow | `valid-session` با delay موقت Debug | Light | Portrait | Medium_Phone_2 / 33 | پاس؛ state disabled و count برگشته به `42` قابل‌تشخیص است | placeholder بخش پست‌ها و debug route دیده می‌شوند. |
| `14-other-profile-follow-rollback-error.png` | Follow rollback error | `valid-session` با failure کنترل‌شده Debug | Light | Portrait | Medium_Phone_2 / 33 | پاس؛ rollback به Not Following و snackbar خطا واضح است | snackbar نزدیک ناحیه debug route قرار گرفته و hierarchy پایین صفحه شلوغ است. |
| `15-feed-after-back-from-profile.png` | بازگشت Other Profile به Feed | `valid-session` | Light | Portrait | Medium_Phone_2 / 33 | پاس؛ Feed و موقعیت قبلی بازیابی شده‌اند | همان دو عنوان بالا، username bidi و debug route قابل‌مشاهده‌اند. |
| `16-profile-tab-return.png` | بازگشت به Profile tab | `valid-session` | Light | Portrait | Medium_Phone_2 / 33 | پاس؛ Own Profile پس از tab switch نمایش داده شد | username bidi معکوس و debug route باقی است. |

## کنترل کیفیت تصاویر

- تعداد تصاویر اصلی: `16`
- ابعاد همه تصاویر: `1080x2400`
- status bar و navigation bar در همه تصاویر حفظ شده‌اند.
- keyboard، پنجره Emulator، صفحه سیاه یا loading دائمی دیده نشد.
- متن فارسی در همه گروه‌های اصلی قابل‌مشاهده و خواناست.
- media و avatar fallback شکسته نیستند.
- stateهای Not Following، Pending Follow، Following، Pending Unfollow و rollback
  از یکدیگر قابل‌تشخیص‌اند.
- Contact sheet: `2440x5350`

## Codex Visual Findings

1. متن‌های `CurrentRoute` و `DeepLinkDebug` در همه صفحات Shell داخل UI قابل‌مشاهده‌اند؛
   این اطلاعات تشخیصی ارتفاع زیادی مصرف می‌کنند و ظاهر build را غیرمحصولی می‌کنند.
2. usernameهای LTR در بافت RTL روی تصویر با `@` در انتها دیده می‌شوند
   (`fixture2@` به‌جای `@fixture2`)؛ bidi isolation بصری کافی نیست.
3. title سطح Shell (`خانه` یا `نمایه`) همراه با title داخلی screen فضای بالا را دوبرابر
   و hierarchy را ضعیف کرده است.
4. در Post Detail، media بیش از حد viewport را می‌گیرد و counts در تصویر یا clip شده‌اند
   یا پشت ناحیه پایین صفحه خارج از دید قرار گرفته‌اند.
5. Own Profile آفلاین indicator بصری stale/offline ندارد و از حالت آنلاین قابل‌تشخیص نیست.
6. asset لوگوی Startup داخل مستطیل خاکستری بزرگ، placeholder-like و ناهماهنگ با سطح
   پرداخت سایر صفحات دیده می‌شود.
7. بخش `پست‌های عمومی` در Other Profile متن صریح «پیاده‌سازی نمی‌شود» دارد و به‌عنوان
   placeholder قابل‌مشاهده است.

این موارد در مرحله Visual Review فقط ثبت شدند و هیچ redesign یا Feature جدیدی اعمال نشد.
