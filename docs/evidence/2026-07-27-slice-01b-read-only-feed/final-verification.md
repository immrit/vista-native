# SLICE-01B Final Verification

**Date:** 2026-07-28
**Branch:** `slice-01b/read-only-feed`
**Baseline HEAD:** `0f3149b717b66336844ffd746c1325b909517000`
**Exit status:** `SLICE-01B Complete — Read-only Feed Vertical Slice Passed`

## Scope

این evidence فقط Read-only Feed موجود را پوشش می‌دهد. Like، Comment، Save، Share،
Profile navigation و هر Feature جدید خارج از Scope ماندند. چون Backend زنده با session
قابل استفاده در دسترس نبود، runtime با fixture محدود به `debug/androidTest` اجرا شد.
بررسی DEX نشان داد fixture در betaDebug حاضر و در betaRelease غایب است.

## Baseline Inventory

- Branch: `slice-01b/read-only-feed`
- HEAD: `0f3149b717b66336844ffd746c1325b909517000`
- Staged: 0
- Working tree: 6 فایل tracked تغییرکرده و 20 فایل Slice untracked
- `git diff --check`: pass
- Artifactهای موجود: فقط مسیرهای ignored شامل `build/`، `.gradle/` و
  `.kotlin/errors/*.log`
- فایل‌های `task.md`، `walkthrough.md`، APK یا AAB خارج از build directory یافت نشدند.

## Backend Contract

قرارداد با `E:\vista-backend\internal\posts\handlers.go` و `service.go` تطبیق داده شد:

- `GET /v1/feed`
- Queryهای `limit` و `offset`؛ مقدار معمول صفحه 15 و سقف Backend برابر 30
- Success envelope مستقیم: `posts`، `has_more` و `next_cursor` اختیاری
- Empty envelope: `posts=[]`، `has_more=false` و cursor حذف‌شده
- Timestamp: JSON زمان Go با قالب RFC3339Nano
- فیلدهای nullable پست: content، URLهای منفرد media/music، aspect ratio، music title،
  diagnostics/moderation و follow status
- فیلدهای nullable author: username، avatar URL و verification type
- Error envelopeهای معتبر Backend: `code/message` یا `error`؛ هیچ‌کدام success decode
  نمی‌شوند.
- `image_urls` و `tags` توسط Backend به آرایه خالی normalize می‌شوند.

## Repository and Room

- Cache-first با Flowهای Room
- Refresh صفحه اول با replace تراکنشی
- Append با offset ذخیره‌شده و پیشروی بر اساس تعداد raw response
- جلوگیری از append هم‌زمان با `Mutex.tryLock`
- توقف پایدار در پایان Feed
- Dedup بر اساس post ID در response و cache
- خطای refresh/append بدون تخریب cache
- جداسازی کامل با `account_id`
- پاک‌سازی posts و pagination metadata هنگام logout
- نگهداری `next_offset`، `has_more`، `next_cursor` و زمان refresh برای recreation
- Migration واقعی `3→4` شامل `feed_post` و `feed_page_state`

## Post Detail

Post Detail با ID route و SavedStateHandle ساخته شد، ابتدا از Room می‌خواند و author،
caption، media/thumbnail و counts را نمایش می‌دهد. Back، tab return و Activity recreation
روی API 33 بررسی شدند. هیچ action تعاملی یا Profile navigation اضافه نشد.

## Test Results

| Suite | Total | Passed | Failed | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| Feed mapper/contract | 6 | 6 | 0 | 0 | 0 |
| OfflineFirstFeedRepository | 10 | 10 | 0 | 0 | 0 |
| FeedViewModel | 8 | 8 | 0 | 0 | 0 |
| Auth/Shell/Profile unit regressions | 37 | 37 | 0 | 0 | 0 |
| Room DAO + migration API 33 | 13 | 13 | 0 | 0 | 0 |
| Feed instrumentation API 33 | 6 | 6 | 0 | 0 | 0 |
| App runtime + Shell/Auth/Profile regression API 33 | 18 | 18 | 0 | 0 | 0 |
| **Total** | **98** | **98** | **0** | **0** | **0** |

Final XML evidence:

- Unit: `.gradle/slice01b-unit-evidence/**/test-results/testDebugUnitTest/TEST-*.xml`
- Room/Feed device:
  `.gradle/slice01b-device-evidence/**/outputs/androidTest-results/connected/debug/TEST-*.xml`
- App runtime:
  `.gradle/slice01b-app-api33-evidence2/app/outputs/androidTest-results/connected/debug/flavors/beta/TEST-Medium_Phone_2(AVD) - 13-_app-beta.xml`

هیچ تستی حذف یا skipped نشده است.

## API 33 Runtime

Device: `Medium_Phone_2`, Android API 33, serial `emulator-5556`.

اجرای `:app:connectedBetaDebugAndroidTest`، betaDebug و APK تست را نصب و Activity واقعی
را اجرا کرد. سه سناریوی اختصاصی Feed این موارد را اثبات کردند:

- valid session و نمایش محتوای اولیه
- scroll و افزایش cache از 15 به 22 پست
- pull-to-refresh با افزایش واقعی شمارنده درخواست صفحه اول
- Feed → Post Detail → Back و نمایش media/counts
- switch tab → return
- Activity recreation
- logout → Auth
- offline با cache و recreation
- error بدون cache

مجموعه کامل App برابر 18/18 پاس شد. پس از پاک‌کردن logcat پیش از run،
جست‌وجوی `FATAL EXCEPTION`، `ANR in`، `am_crash` و `am_anr` برابر صفر بود.

## Build, Lint, R8 and Security

- `assembleBetaDebug`: pass روی source نهایی
- `assembleBetaRelease`: pass؛ APK unsigned برابر 5,380,689 بایت
- R8 betaRelease: pass
  - `mapping.txt`: 37,954,667 بایت
  - `usage.txt`: 4,130,364 بایت
  - `seeds.txt`: 145,043 بایت
  - `configuration.txt`: 39,287 بایت
  - پنج کلاس حیاتی Startup/Auth در mapping حاضرند.
- Lint:
  - App betaDebug: pass، 0 error، 9 warning مربوط به نسخه dependency
  - Core database: pass، 0 error
  - Feature feed: pass، 0 error
  - Feature shell: pass، 0 error، 2 hint
- Secret scan: 308 فایل tracked/untracked، 0 finding
- DEX fixture boundary: debug matches > 0، release matches = 0
- `git diff --check`: pass

## Commits

- `112bcf3` — `feat(feed): complete cached pagination and post detail`
- `9d3d850` — `test(feed): verify pagination cache and navigation`
- `docs(feed): record slice-01b verification`
