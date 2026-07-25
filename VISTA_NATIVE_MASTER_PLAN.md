# نقشه‌راه مهندسی بازنویسی Native ویستا

## ۱. هدف و خط مبنا

خروجی اجرایی این طرح باید در `E:\vista_native\VISTA_NATIVE_MASTER_PLAN.md` نگهداری شود و مرجع اصلی مهاجرت باشد.

### هدف نهایی

بازنویسی کامل ویستا برای Android با Kotlin و Jetpack Compose، به‌صورتی که:

- هویت بصری، رفتارها، RTL و مسیرهای اصلی نسخه Flutter حفظ شوند.
- کاربر نسخه جدید را ادامه همان ویستا بداند، نه محصولی متفاوت.
- سرعت راه‌اندازی، اسکرول، مصرف حافظه، پایداری و رفتار آفلاین بهتر شود.
- مهاجرت بدون خروج اجباری کاربر، از دست رفتن کلیدهای رمزنگاری، Draftها یا عملیات Pending انجام شود.
- نسخه Flutter و Native تا زمان جایگزینی کامل، هم‌زمان با Backend سازگار بمانند.

### وضعیت تأییدشده فعلی

- Flutter شامل حدود ۴۶۳ فایل Dart و ۱۴۸ هزار خط کد است.
- بزرگ‌ترین دامنه‌ها Chat، Posts، Stories، Profile، Settings و Auth هستند.
- اپ اصلی پنج بخش Feed، Search، Services، Chat و Profile دارد.
- Flutter ترکیبی از Riverpod، StatefulWidget، Isar، SharedPreferences، Dio و چند معماری قدیمی و جدید است؛ بنابراین ترجمه خط‌به‌خط ممنوع است و فقط رفتار و قراردادها باید منتقل شوند.
- Native فعلی یک ماژول `:app` با حدود ۲۸ فایل Kotlin است و فقط Startup، Onboarding و Auth را تا مرز placeholder پیاده کرده است.
- تست واحد و lint پروژه Native در وضعیت فعلی با موفقیت اجرا شده‌اند.
- Native هنوز Git معتبر ندارد و قبل از توسعه گسترده باید repository و baseline commit آن ایجاد شود.
- تم فعلی Native با تم واقعی Flutter اختلاف دارد و باید به خانواده Indigo/Violet/Pink و فونت Vazirmatn برگردد.
- `E:\vista` دارای تغییرات محلی است؛ ممیزی و مهاجرت نباید این تغییرات را بازنویسی یا پاک کند.

## ۲. معماری هدف

معماری بر پایه راهنمای رسمی Android، لایه‌بندی Data/Domain/UI، جریان داده یک‌طرفه و ViewModelهای مستقل از Compose خواهد بود. [راهنمای معماری Android](https://developer.android.com/topic/architecture)

### ساختار پروژه

یک Modular Monolith با granularity متوسط استفاده شود تا برای یک توسعه‌دهنده قابل مدیریت بماند. از ماژول‌سازی هر صفحه یا هر Use Case خودداری شود؛ راهنمای رسمی Android نیز نسبت به ماژول‌های بیش‌ازحد ریز یا درشت هشدار می‌دهد. [راهنمای Modularization](https://developer.android.com/topic/modularization)

```text
:app
:core:common
:core:model
:core:network
:core:database
:core:datastore
:core:security
:core:designsystem
:core:testing

:feature:auth
:feature:shell
:feature:profile
:feature:feed
:feature:composer
:feature:stories
:feature:chat
:feature:notifications
:feature:search
:feature:nearby
:feature:services
:feature:settings
:feature:payments
```

Chat فقط زمانی به زیرماژول‌های `chat:data` و `chat:crypto` شکسته شود که وابستگی‌ها یا زمان Build آن را توجیه کنند.

### پشته فنی قطعی

- Kotlin، Java 17، Jetpack Compose و Single Activity
- Navigation Compose با routeهای type-safe
- Hilt برای Dependency Injection
- Coroutines و Flow
- Retrofit، OkHttp و Kotlinx Serialization
- Room به‌عنوان منبع اصلی داده‌های ساختاریافته
- Proto DataStore برای تنظیمات و وضعیت‌های کوچک؛ DataStore برای این نوع داده‌ها جایگزین مناسب SharedPreferences است. [راهنمای DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- WorkManager برای Outbox، Sync، Retry و Uploadهای قابل ادامه؛ WorkManager برای کارهای تضمین‌شده و پایدار پس‌زمینه طراحی شده است. [مرجع WorkManager](https://developer.android.com/reference/androidx/work/WorkManager.html)
- Android Keystore برای Session، کلیدهای E2EE و کلید رمزگذاری محلی. [راهنمای Android Keystore](https://developer.android.com/privacy-and-security/keystore)
- Coil برای تصاویر، Media3 برای صوت و ویدئو، CameraX برای دوربین
- Macrobenchmark، Baseline Profiles و JankStats برای کارایی

### الگوی هر Feature

هر Feature شامل این مرزها باشد:

```text
Compose Screen
    ↓ UiAction
ViewModel + immutable UiState
    ↓
Repository interface / focused Use Case
    ↓
Room / Network / Realtime / WorkManager
```

- ViewModel فقط `UiState` پایدار، `UiAction` و effectهای یک‌بارمصرف منتشر کند.
- UI مستقیماً به Retrofit، Room، WebSocket یا WorkManager دسترسی نداشته باشد.
- Use Case فقط برای عملیات چندمرحله‌ای یا منطق تجاری مشترک ساخته شود؛ برای هر کلیک یک Use Case ایجاد نشود.
- Repository ابتدا Flow دیتابیس را منتشر کند و پاسخ شبکه را در Room ادغام کند.
- Paging، refresh، optimistic update و retry از یک سیاست یکسان استفاده کنند.

### قراردادهای عمومی

حداقل interfaceهای زیر در Core تعریف شوند:

- `SessionRepository`
- `ProfileRepository`
- `FeedRepository`
- `PostComposerRepository`
- `StoryRepository`
- `ChatRepository`
- `RealtimeGateway`
- `UploadCoordinator`
- `NotificationRepository`
- `MigrationCoordinator`
- `FeatureFlagRepository`
- `Telemetry`
- `NetworkMonitor`
- `DispatcherProvider`

مدل‌های مشترک:

- `ApiResult<T>`
- `AppError`
- `PageKey`
- `SyncState`
- `PendingOperation`
- `UploadJob`
- `MigrationManifest`
- routeهای type-safe
- مدل واحد Media، UserSummary، PostSummary و MessageEnvelope

خطاها حداقل به Network، Timeout، Unauthorized، Forbidden، Validation، RateLimited، Conflict، Server، TLS، Crypto و Unknown تفکیک شوند.

## ۳. طراحی، امنیت و داده

### هویت بصری

- توکن‌های رنگ Flutter مبنا باشند: `#6366F1`، `#8B5CF6` و `#EC4899`.
- Vazirmatn به‌صورت local و با وزن‌های واقعی بسته‌بندی شود.
- RTL حالت پیش‌فرض UI فارسی باشد.
- فاصله‌ها، radius، elevation، motion، typography و component stateها در `:core:designsystem` متمرکز شوند.
- Dark/Light، font scale تا ۲۰۰٪، TalkBack، reduced motion و contrast بررسی شوند.
- بهبودها فقط شامل انیمیشن روان‌تر، spacing منظم، skeleton مناسب و feedback بهتر باشند؛ ساختار آشنا و جای کنترل‌ها تغییر اساسی نکند.
- برای هر صفحه اصلی یک screenshot baseline از Flutter و یک golden از Native نگهداری شود.

### شبکه و TLS

- تمام درخواست‌ها فقط از یک OkHttp stack مشترک عبور کنند.
- refresh token به‌صورت single-flight اجرا شود تا چند درخواست هم‌زمان چند refresh نسازند.
- decoder خطا پاسخ‌های flat، nested و legacy Backend را پوشش دهد.
- cleartext در production غیرفعال باشد و سیاست آن در Network Security Config ثبت شود. [راهنمای Network Security Configuration](https://developer.android.com/privacy-and-security/security-config)
- Pinning موجود ویستا با حالت‌های `off/monitor/enforce` حفظ شود.
- bootstrap وضعیت سیستم فقط با OS trust انجام شود؛ config امضاشده شامل current pin، next pin، mode و expiry باشد.
- در نبود شبکه، آخرین policy معتبر استفاده شود.
- تغییر pin باعث بازسازی کنترل‌شده OkHttpClient شود.
- token، شماره تماس، محتوای پیام و headerهای حساس هرگز log نشوند.

### Offline-first و Sync

Room منبع اصلی حقیقت UI باشد؛ این الگو با راهنمای رسمی Offline-first Android هم‌راستاست. [راهنمای Offline-first](https://developer.android.com/topic/architecture/data-layer/offline-first)

Outbox مشترک این عملیات را پوشش دهد:

- ارسال و ویرایش پیام
- reaction و read receipt
- ایجاد یا ویرایش پست
- Draft و Upload
- حذف محتوا
- follow/save/like
- عملیات Story

هر عملیات دارای UUID idempotency، user ID، payload version، attempt count، next retry و وضعیت Sync باشد. Retry با exponential backoff و jitter انجام شود. Conflictها به‌صورت domain-specific حل شوند و هیچ عملیات pending بی‌صدا حذف نشود.

### Realtime و Push

- یک `RealtimeGateway` process-scoped اتصال `/v1/chat/ws` را مدیریت کند.
- reconnect به lifecycle، وضعیت شبکه، token و foreground/background وابسته باشد.
- cursor یا sequence آخر ذخیره شود و gap با REST sync جبران گردد.
- WebSocket فقط Room را به‌روزرسانی کند؛ UI مستقیماً event خام دریافت نکند.
- FCM مکمل Realtime باشد و event آن با همان idempotency در Room merge شود.
- disconnect، duplicate delivery، out-of-order event، token rotation و حذف پیام پوشش داده شوند.

### رمزنگاری و مهاجرت داده

قالب‌های E2EE فعلی، envelopeهای legacy/v1/v2، کلید X25519 و AES-GCM محلی بدون تغییر ناسازگار حفظ شوند. هیچ رمزنگاری جدید بدون test vector و بررسی مستقل معرفی نشود.

برای جایگزینی production:

1. یک نسخه نهایی Flutter تحت package و signing فعلی، Migration Bridge را منتشر کند.
2. Bridge یک bundle نسخه‌بندی‌شده و AES-GCM-encrypted در `noBackupFilesDir` بسازد.
3. bundle شامل session، شناسه کاربر، تنظیمات، کلیدهای E2EE، کلید رمزگذاری محلی، Draftها، pending uploadها و manifest دانلودهای کاربر باشد.
4. cacheهای قابل بازسازی منتقل نشوند.
5. Native production با همان application ID و signing key، bundle را transactionally وارد Room، DataStore و Keystore کند.
6. قبل از commit، schema، checksum، تعلق bundle به کاربر و امکان decrypt یک نمونه تأیید شود.
7. شکست مهاجرت باعث logout یا حذف bundle نشود؛ retry ایمن ارائه گردد.
8. bundle فقط پس از تأیید کامل import حذف شود.
9. نسخه Beta با package جدا اجازه خواندن داده production را نداشته باشد.

ساختار منطقی `MigrationManifest`:

```text
schemaVersion
sourceAppVersion
createdAt
userBinding
session
preferences
cryptoMaterial
drafts
pendingOperations
downloadManifest
itemCounts
integrityHash
```

## ۴. فازهای پیاده‌سازی

### AUD-01 — ممیزی و Parity Ledger

- تمام Screenها، routeها، modalها، deep linkها، notification actionها و حالت‌های empty/loading/error/offline Flutter فهرست شوند.
- برای هر Feature مسیر Screen → Provider → Repository → Service → Backend/Isar ثبت شود.
- endpoint، permission، cache، background task، analytics و security dependency هر قابلیت استخراج شود.
- قابلیت‌ها در ماتریس `Not Started / Partial / Parity / Verified / Deferred` قرار گیرند.
- screenshot و performance baseline از Flutter ثبت شود.
- خروجی این فاز مرجع Scope همه فازهای بعدی باشد.

دروازه خروج: هیچ مسیر کاربری یا dependency ناشناخته در پنج تب اصلی باقی نماند.

### GOV-01 — Repository و Build Foundation

- برای `E:\vista_native` یک Git repository معتبر و baseline commit مستقل ایجاد شود.
- Version Catalog و convention pluginهای Gradle افزوده شوند.
- flavorها:

```text
beta       → ir.coffevista.vista_native
production → ir.coffevista.vista
```

- signing production از source control خارج و فقط از secret store/CI دریافت شود.
- CI شامل assemble، unit test، lint، dependency check و screenshot smoke باشد.
- R8 و resource shrinking برای release فعال شوند.

دروازه خروج: Debug/Beta/Production reproducible build داشته باشند و secret وارد repository نشده باشد.

### FND-01 — معماری پایه

- پروژه تک‌ماژوله به Modular Monolith هدف منتقل شود.
- Hilt، navigation type-safe، error model، dispatchers، clock و logging اضافه شوند.
- Retrofit/OkHttp، Room، Proto DataStore و WorkManager پایه راه‌اندازی شوند.
- AppContainer دستی حذف تدریجی شود.
- Startup/Auth فعلی بدون regression روی معماری جدید منتقل شود.

دروازه خروج: Startup، Maintenance، Onboarding و Auth با تست‌های فعلی و جدید کار کنند.

### DSN-01 — Design System و App Shell

- تم Native به توکن‌های واقعی Vista تغییر کند.
- Componentهای Button، TextField، Dialog، Sheet، Snackbar، Avatar، MediaCard، Skeleton و Empty/Error State ساخته شوند.
- Shell پنج‌تب، state restoration و back-stack مستقل هر تب پیاده شود.
- Deep link dispatcher مرکزی افزوده شود.

دروازه خروج: Shell در Light/Dark، RTL و process recreation پایدار باشد.

### SLICE-01 — Profile + Feed

اولین vertical slice بعد از foundation:

- Profile خود کاربر، پروفایل دیگران، follow state، counts و refresh
- Feed pagination، pull-to-refresh، media cards و optimistic like/save
- جزئیات پست، comments، share و report
- Room cache و offline read
- telemetry کارایی و screenshot parity

دروازه خروج: کاربر از login تا Feed، Post Detail و Profile یک جریان واقعی end-to-end داشته باشد.

### MEDIA-01 — Composer و Upload

- انتخاب فایل با Photo Picker/SAF، نه storage permission گسترده
- CameraX و Media3
- Draft پایدار، resumable upload، progress، cancel و retry
- validation مشترک با Backend
- WorkManager/foreground worker برای عملیات طولانی
- جلوگیری از بارگذاری کامل فایل بزرگ در RAM

دروازه خروج: قطع شبکه، restart process و کمبود حافظه باعث از دست رفتن Draft یا Upload نشود.

### STORY-01 — Stories

- create، editor، privacy، viewers و player
- preload محدود و cache budget
- upload/retry مشترک با Media
- هماهنگی lifecycle پخش و background

### CHAT-01 — Inbox و Realtime

- Conversation list، pagination، unread counts، archive و search
- WebSocket manager، REST gap sync و FCM merge
- presence و typing با rate limit
- retry queue و delivery/read state

### CHAT-02 — Message و E2EE

- متن، reply، edit، delete، reaction و forward
- سازگاری کامل envelopeهای رمزنگاری موجود
- message-at-rest encryption
- voice، image، video، document و download
- group create/edit/details/member management
- message info، pinned/replied state و deleted-message behavior

دروازه خروج Chat: اتصال مجدد، ارسال آفلاین، duplicate push، key migration، token rotation و history decrypt روی دستگاه واقعی موفق باشند.

### DISC-01 — سایر قابلیت‌ها

به ترتیب dependency:

1. Notifications و Search
2. Nearby، Likes و Matches
3. Services Hub، contacts و WebViewهای مجاز
4. Settings، privacy، active sessions و blocked users
5. Premium، پرداخت بازار/Zibal و restore entitlement
6. Music bridge، update flow، legal/about/support

WebView فقط allowlist، Safe Browsing، محدودیت JavaScript و کنترل navigation داشته باشد.

### MIG-01 — Flutter Bridge

- contract bundle در Dart و Kotlin با test vector مشترک پیاده شود.
- مهاجرت Session، DataStore، E2EE، Draft، Pending و Download تست شود.
- Flutter bridge release ابتدا روی cohort داخلی نصب شود.
- نرخ bundle creation/import و علت خطا فقط به‌شکل غیرحساس گزارش شود.

### REL-01 — Beta

- Beta با package جدا در Bazaar و APK مستقیم منتشر شود.
- Backend فقط تغییرات additive یا versioned دریافت کند.
- کاربران beta داده production را به‌صورت خودکار مصرف نکنند.
- هر قابلیت با remote feature flag قابل خاموش‌شدن باشد.

### REL-02 — Production Replacement

- production Native با application ID و signing اصلی ساخته شود.
- rollout به‌ترتیب ۵٪، ۲۰٪، ۵۰٪ و ۱۰۰٪ انجام شود.
- ارتقا فقط در صورت عبور از crash، ANR، migration و auth gate ادامه یابد.
- rollback با حفظ compatibility Backend و bridge release ممکن باشد.
- Flutter فقط پس از دو نسخه Native پایدار و بسته‌شدن Parity Ledger بازنشسته شود.

## ۵. تست و معیار پذیرش

### ماتریس تست

- Emulator: API 24، 29، 34 و 36
- حداقل یک دستگاه ضعیف API 24/26
- یک دستگاه میان‌رده رایج
- یک دستگاه Samsung با محدودیت background
- یک دستگاه high-refresh
- Light/Dark، RTL، font scale، TalkBack و rotation/process death

### سطوح تست

- Unit: ViewModel، reducer، mapper، validation و retry policy
- Contract: endpointها با MockWebServer و fixtureهای واقعی
- Database: migration، paging، conflict و rollback
- Crypto: test vector مشترک Flutter/Kotlin و legacy envelopes
- WorkManager: retry، duplicate work، cancellation و reboot
- UI: navigation، state restoration، screenshot parity
- Integration: Auth، Feed، Upload، Chat، Push و Migration
- Security: secret scan، permission audit، exported component، deep link، backup، TLS و WebView
- Runtime: memory، battery، network loss، low storage و process death

### بودجه کارایی

- p95 cold start حداکثر ۲ ثانیه روی میان‌رده و ۳ ثانیه روی دستگاه ضعیف
- حداقل ۳۰٪ بهبود نسبت به baseline Flutter در p95 startup و frame time
- frozen frame در مسیرهای بحرانی صفر
- jank در Feed و Chat کمتر از ۳٪ روی دستگاه مرجع
- عدم رشد بدون سقف cache، bitmap memory یا database
- عدم ANR در upload، migration، sync و crypto
- Baseline Profile برای Startup، Feed، Profile و Chat تولید و در release نصب شود؛ مستندات Android تأثیر آن را بر startup و jank توضیح می‌دهد. [Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/overview)

### Release Gate

انتشار production فقط وقتی مجاز است که:

- Parity Ledger قابلیت‌های ضروری ۱۰۰٪ `Verified` باشد.
- هیچ crash یا ANR بحرانی باز نباشد.
- Migration روی upgrade واقعی از نسخه Flutter تأیید شده باشد.
- session و E2EE بدون logout یا از دست رفتن history منتقل شوند.
- نرخ موفقیت Auth و Migration حداقل ۹۹٫۵٪ باشد.
- crash-free users حداقل ۹۹٫۸٪ باشد.
- smoke test بازار، APK مستقیم، پرداخت، deep link، push و update موفق باشد.

## ۶. حریم خصوصی، پایش و پیش‌فرض‌های قفل‌شده

- telemetry فقط crash، ANR، performance، network class، feature health و migration result را ثبت کند.
- متن پیام، شماره تلفن، token، media content، کلید، query خصوصی و payload رمزنگاری ثبت نشود.
- شناسه telemetry تصادفی، قابل reset و غیرتبلیغاتی باشد.
- Android تنها پلتفرم این برنامه است و حداقل نسخه API 24 باقی می‌ماند.
- اولویت کیفیت و سازگاری است؛ deadline مصنوعی نباید باعث حذف تست یا migration شود.
- Backend در دوران هم‌زیستی فقط با تغییرات backward-compatible توسعه می‌یابد.
- UI هویت فعلی Vista را حفظ می‌کند و فقط پالایش کنترل‌شده می‌شود.
- session، تنظیمات، Draftها، Pendingها، دانلودهای کاربر و کلیدهای رمزنگاری منتقل می‌شوند؛ cacheهای بازسازی‌پذیر منتقل نمی‌شوند.
- انتشار ابتدا Beta با package جدا و سپس جایگزینی برنامه اصلی با package/signing قبلی خواهد بود.
- کانال‌های انتشار Cafe Bazaar و APK مستقیم هستند.
- هیچ ادعای تکمیل Feature صرفاً با compile یا unit test پذیرفته نیست؛ وضعیت نهایی فقط پس از runtime test روی دستگاه به `Verified` تغییر می‌کند.
