# FND-01 — معماری پایه Native ویستا

**Type:** refactor
**Status:** in-progress
**Progress:** `[█░░░░░░░░░] 7%` — 13/193 مورد تکمیل شده

## Problem / Goal

پروژه Native فعلی باید از ساختار تک‌ماژوله و `AppContainer` دستی به Foundation
معماری Modular Monolith مصوب منتقل شود؛ بدون Regression در Startup،
Maintenance، Onboarding و Auth. این Task فقط `FND-01` را پوشش می‌دهد و نباید
هیچ بخش از `DSN-01`، `SLICE-01` یا Featureهای بعدی را زودتر پیاده‌سازی کند.

## Root cause / Design

وضعیت فعلی فقط ماژول `:app`، navigation رشته‌ای، DI دستی و Foundation محدود
Startup/Auth دارد. طراحی هدف، ownership یکتا برای dependencyها، Hilt،
قراردادهای Core تست‌پذیر، stack مشترک Retrofit/OkHttp، Room، Proto DataStore،
WorkManager و navigation/deep-link type-safe است. ماژول فقط زمانی ساخته می‌شود
که در همین فاز کد و مسئولیت واقعی داشته باشد؛ abstraction حدسی، ماژول خالی،
Feature-to-Feature dependency و `core:common` به‌عنوان dumping ground ممنوع‌اند.

## Progress Rules

- مخرج پیشرفت، تمام checkboxهای همین سند است.
- هر checkbox فقط بلافاصله پس از ثبت شاهد قابل‌بازتولید تیک می‌خورد.
- درصد برابر `checked / total × 100` و به نزدیک‌ترین عدد صحیح گرد می‌شود.
- نوار ۱۰ خانه‌ای از درصد به نزدیک‌ترین ۱۰٪ ساخته می‌شود.
- compile یا unit test به‌تنهایی معادل runtime، parity یا عبور Gate نیست.
- وضعیت نهایی فقط وقتی `done` می‌شود که تمام Exit Gateهای الزامی پاس شده باشند.
- مورد Blocked تیک نمی‌خورد؛ علت، owner و شرط رفع آن در Blocker Register ثبت می‌شود.

## Sources of Truth

ترتیب مرجع در صورت نیاز به تفسیر:

1. متن مأموریت پیوست‌شده برای `FND-01`
2. `VISTA_NATIVE_MASTER_PLAN.md`
3. `docs/audits/2026-07-25-aud-01-parity-ledger.md`
4. `docs/plans/2026-07-25-aud-01-parity-ledger.md`
5. `docs/plans/2026-07-25-gov-01-repository-build-foundation.md`
6. `docs/build/GOV-01-BUILD-SIGNING-AND-VARIANTS.md`
7. Flutter در `E:\vista` فقط برای رفتار، قرارداد و UI به‌صورت read-only
8. Backend در `E:\vista-backend` فقط برای بررسی read-only قراردادها

## Scope Lock

در این Task ممنوع است: اجرای `DSN-01`، Shell پنج‌تب، Feed، Profile، Chat،
Story، Search، Services، تغییر Backend یا Flutter، Migration Bridge، import
واقعی داده Flutter، Draft/Download migration، Production signing، تغییر UI
هویتی خارج از Startup/Auth موجود، ماژول خالی، abstraction speculative، حذف
کد سالم بدون parity evidence و هر ادعای Production-ready.

## Phases

### Phase 0 — Baseline، Safety و شواهد پیش از Refactor

- [x] `FND-00-01` متن مأموریت پیوست و هر پنج سند مرجع کامل خوانده شدند.
- [x] `FND-00-02` baseline Native ثبت شد: branch برابر `gov-01/repository-build-foundation`، HEAD برابر `3115b0db2f2874e2254d87cacc9da6f04e479e3c` و working tree پیش از ساخت این tracker تمیز بود؛ `3115b0d` ancestor خود HEAD است.
- [x] `FND-00-03` baseline Flutter به‌صورت read-only ثبت شد: `main@64994fa1dfa6f1b146cb9d132758f105670e4d71` با تغییرات محلی موجود؛ هیچ reset/stash/checkout/overwrite انجام نشد.
- [x] `FND-00-04` working tree پس از ایجاد tracker دوباره ثبت شد؛ تنها تغییر Native فایل جدید همین tracker و در Scope `FND-01` است.
- [x] `FND-00-05` Java `17.0.19`، Gradle `8.13`، AGP `8.13.2`، Kotlin plugin `2.0.21`، platformهای Android `31/33/34/35/36`، Build Tools و AVDهای `Medium_Phone`/`Medium_Phone_2` ثبت شدند.
- [x] `FND-00-06` snapshot پیش از Refactor ثبت شد: فقط `:app` و included build `:build-logic`؛ ۲۸ فایل main Kotlin، ۵ unit test، ۱ androidTest و dependency graph `productionDebugRuntimeClasspath`.
- [x] `FND-00-07` هر چهار Variant پیش از Refactor از clean state با exit code صفر ساخته شدند.
- [x] `FND-00-08` baseline unit test اجرا شد: ۲۰ XML suite، ۸۰ execution و صفر failure/error/skipped.
- [x] `FND-00-09` baseline lint در clean verification با exit code صفر پاس شد.
- [x] `FND-00-10` R8/resource shrinking بررسی شد؛ mapping/seeds/usage/configuration هر دو release موجود و کلاس‌های بحرانی Startup/Auth حفظ شدند.
- [x] `FND-00-11` baseline secret scan روی ۱۲۹ فایل tracked با صفر finding پاس شد.
- [x] `FND-00-12` baseline `git diff --check` پاس شد؛ دو lockfile و `gradle/verification-metadata.xml` موجودند.
- [x] `FND-00-13` مسیر evidence برابر `docs/evidence/2026-07-25-fnd-01/` تعیین و خلاصه Phase 0 ثبت شد.

### Phase 1 — Modular Monolith و Dependency Rules

- [ ] `FND-MOD-01` ownership map فعلی `:app` برای Startup/Auth/Network/Storage/DI تهیه شود.
- [ ] `FND-MOD-02` module graph هدف فقط برای مسئولیت‌های واقعی این فاز نهایی شود: `:app`، `:core:common`، `:core:model`، `:core:network`، `:core:database`، `:core:datastore`، `:core:security`، `:core:testing` و `:feature:auth`.
- [ ] `FND-MOD-03` هر ماژول پیشنهادی پیش از ایجاد، consumer و کد واقعی مستند داشته باشد؛ ماژول بدون مصرف حذف یا ایجاد نشود.
- [ ] `FND-MOD-04` conventionهای Gradle موجود GOV-01 بدون شکستن Version Catalog توسعه داده شوند.
- [ ] `FND-MOD-05` `:app` به Android entry point و composition root محدود شود.
- [ ] `FND-MOD-06` مدل‌های pure Kotlin مشترک به `:core:model` منتقل و عدم وابستگی آن به Android framework اثبات شود.
- [ ] `FND-MOD-07` utilityهای واقعاً عمومی و محدود در `:core:common` قرار گیرند و ownership هر مورد ثبت شود.
- [ ] `FND-MOD-08` Auth موجود با مرزهای واقعی UI/domain/data به `:feature:auth` منتقل شود.
- [ ] `FND-MOD-09` مرزهای مستقل `core:database`، `core:datastore` و `core:security` پیاده شوند.
- [ ] `FND-MOD-10` contractهای Domain/Repository از implementationها جدا و dependency direction مستند شود.
- [ ] `FND-MOD-11` Feature-to-Feature dependency، Android type در Domain و cycle با rule/check قابل‌اجرا ممنوع شود.
- [ ] `FND-MOD-12` Gradle dependency graph نهایی ذخیره و نبود cycle/ماژول خالی بررسی شود.
- [ ] `FND-MOD-13` هر چهار Variant پس از checkpoint ماژول‌سازی build شوند.

### Phase 2 — Hilt Composition Root و Ownership

- [ ] `FND-DI-01` dependency inventory کامل `AppContainer` و تمام factoryهای دستی Startup/Auth تهیه شود.
- [ ] `FND-DI-02` Hilt plugin/dependencyها از Version Catalog و conventionهای مصوب اضافه شوند.
- [ ] `FND-DI-03` `VistaApplication` به `@HiltAndroidApp` و entry pointهای Android به Hilt منتقل شوند.
- [ ] `FND-DI-04` moduleهای Hilt بر اساس owner واقعی Network، Storage، Security، Clock، Dispatcher و Environment ساخته شوند.
- [ ] `FND-DI-05` qualifierهای environment، internal/external network client، dispatcher و clock صریح و type-safe باشند.
- [ ] `FND-DI-06` scopeهای `Singleton`، `ActivityRetained` و `ViewModel` برای هر dependency با دلیل ثبت شوند.
- [ ] `FND-DI-07` StartupResolver، StartupViewModel، AuthViewModel و repositoryهای Auth به constructor injection منتقل شوند.
- [ ] `FND-DI-08` test replacement برای dependencyهای Foundation با Hilt test modules یا fakeهای صریح ممکن شود.
- [ ] `FND-DI-09` تمام consumerهای Startup/Auth از Hilt استفاده کنند و Service Locator موازی باقی نماند.
- [ ] `FND-DI-10` `AppContainer` فقط پس از اثبات انتقال کامل dependencyها حذف شود.
- [ ] `FND-DI-11` authoritative owner هر dependency در یک ownership table مستند شود.
- [ ] `FND-DI-12` build، unit test و instrumentation smoke پس از حذف DI دستی پاس شوند.

### Phase 3 — Common Foundation Contracts

- [ ] `FND-COM-01` `AppError` با دسته‌های Network، Timeout، Unauthorized، Forbidden، Validation، RateLimited، Conflict، Server، TLS، Crypto و Unknown نهایی شود.
- [ ] `FND-COM-02` `ApiResult<T>` یا Result contract نهایی بدون duplicate wrapper پیاده شود.
- [ ] `FND-COM-03` mapping خطای HTTP/transport/domain و حفظ cause غیرحساس تعریف شود.
- [ ] `FND-COM-04` flat، nested و legacy Backend error envelope با fixture واقعی پوشش داده شود.
- [ ] `FND-COM-05` `DispatcherProvider` با test dispatcher قابل‌جایگزینی پیاده شود.
- [ ] `FND-COM-06` `Clock` injectable برای زمان، expiry، backoff و تست deterministic پیاده شود.
- [ ] `FND-COM-07` `Telemetry` محدود به Foundation health و بدون profiling تبلیغاتی تعریف شود.
- [ ] `FND-COM-08` `NetworkMonitor` lifecycle-aware و تست‌پذیر پیاده شود.
- [ ] `FND-COM-09` environment/config contract برای Beta/Production و override امن تثبیت شود.
- [ ] `FND-COM-10` logger ساخت‌یافته، قابل‌خاموش‌شدن و redact‌شده پیاده شود.
- [ ] `FND-COM-11` policy مربوط به application scope، supervisor، cancellation و structured concurrency مستند و مصرف شود.
- [ ] `FND-COM-12` تست redaction اثبات کند token، phone، message، encryption payload، key و header حساس log نمی‌شوند.

### Phase 4 — Network، TLS و External URL Security

- [ ] `FND-NET-01` contract clientهای internal API، external/media و bootstrap به‌صورت جدا تعریف شود.
- [ ] `FND-NET-02` stack مشترک Retrofit/OkHttp/Kotlinx Serialization برای تمام درخواست‌های داخلی ایجاد شود.
- [ ] `FND-NET-03` timeoutهای connect/read/write/call صریح، environment-safe و تست‌پذیر باشند.
- [ ] `FND-NET-04` Authorization فقط به hostهای داخلی allowlisted افزوده شود.
- [ ] `FND-NET-05` external/media client به‌صورت پیش‌فرض auth interceptor داخلی نگیرد.
- [ ] `FND-NET-06` retry کور/نامحدود حذف یا ممنوع و retry policy محدود و idempotency-aware شود.
- [ ] `FND-NET-07` refresh token به‌صورت single-flight پیاده شود.
- [ ] `FND-NET-08` چند 401 هم‌زمان با تست concurrency دقیقاً یک refresh ایجاد کنند.
- [ ] `FND-NET-09` refresh موفق، failed، expired و revoked به session transition کنترل‌شده منجر شوند.
- [ ] `FND-NET-10` token refresh loop و replay نامحدود درخواست غیرممکن و تست شود.
- [ ] `FND-NET-11` Production cleartext غیرفعال و Network Security Config صریح باقی بماند.
- [ ] `FND-NET-12` application ID و API environment چهار Variant مطابق GOV-01 حفظ شود.
- [ ] `FND-TLS-01` state machine سیاست TLS برای `off/monitor/enforce` تعریف و تست شود.
- [ ] `FND-TLS-02` bootstrap فقط با OS trust و بدون pin/config ساختگی پیاده شود.
- [ ] `FND-TLS-03` current pin، next pin، expiry و last-valid-policy با contract versioned مدل شوند.
- [ ] `FND-TLS-04` نبود/انقضا/خرابی policy در هر mode fail behavior صریح و بدون fail-open ناخواسته داشته باشد.
- [ ] `FND-TLS-05` تغییر policy باعث rebuild کنترل‌شده client و عدم race/leak شود.
- [ ] `FND-TLS-06` failure report فقط داده غیرحساس ارسال یا ثبت کند.
- [ ] `FND-TLS-07` contract واقعی signed pin config در Backend read-only بررسی شود؛ قسمت ناموجود با owner و Gate به Blocker Register منتقل شود.
- [ ] `FND-WEB-01` قرارداد Foundation برای HTTPS-only، host/path allowlist، scheme rejection و external-browser disposition ایجاد شود.
- [ ] `FND-WEB-02` تست policy مانع subdomain/redirect/scheme bypass و نشت token/cookie به مقصد untrusted شود.

### Phase 5 — Room Foundation

- [ ] `FND-DB-01` نیاز واقعی schema برای Startup/Auth/Foundation با evidence تعیین شود.
- [ ] `FND-DB-02` Room و processor/pluginهای لازم از Version Catalog اضافه شوند.
- [ ] `FND-DB-03` database owner، DAO boundary و repository access تعریف شود؛ UI دسترسی مستقیم نداشته باشد.
- [ ] `FND-DB-04` فقط entityهای Foundation واقعاً مصرف‌شده ایجاد شوند؛ هیچ جدول Feed/Chat/Story/Profile ساخته نشود.
- [ ] `FND-DB-05` schema export فعال و artifact آن version-controlled شود.
- [ ] `FND-DB-06` destructive migration در Production ممنوع و fallback behavior صریح شود.
- [ ] `FND-DB-07` transaction boundary و database dispatcher مشخص و تست شود.
- [ ] `FND-DB-08` migration test infrastructure برای create/upgrade/rollback ایجاد شود.
- [ ] `FND-DB-09` schema creation، migration، transaction failure و corruption behavior تست شوند.
- [ ] `FND-DB-10` encryption policy و sensitive-data ownership مستند شود.
- [ ] `FND-DB-11` عدم اجرای Isar replacement یا Flutter Migration Bridge اثبات و finding Flutter fail-open باز نگه داشته شود.

### Phase 6 — Proto DataStore Foundation

- [ ] `FND-DS-01` schema فقط برای onboarding state، local app config و startup preference غیرحساس طراحی شود.
- [ ] `FND-DS-02` Proto، serializer و DataStore wiring در owner module پیاده شود.
- [ ] `FND-DS-03` onboarding state فعلی بدون Regression به DataStore منتقل شود.
- [ ] `FND-DS-04` defaultها و schema evolution/versioning صریح تعریف شوند.
- [ ] `FND-DS-05` corruption handler رفتار deterministic و غیرمخرب داشته باشد.
- [ ] `FND-DS-06` تست serializer/default/corruption/migration پاس شود.
- [ ] `FND-DS-07` session token، private key و داده حساس هرگز plaintext در Proto ذخیره نشود.
- [ ] `FND-DS-08` logout retention بین preferenceهای device-wide و account-scoped تست شود.

### Phase 7 — Security Storage و Session

- [ ] `FND-SEC-01` `SessionStore` فعلی و تمام callsiteهای Startup/Auth دوباره trace شوند.
- [ ] `FND-SEC-02` AES-GCM و Android Keystore ownership در `core:security` تثبیت شود.
- [ ] `FND-SEC-03` key alias و encrypted payload با account/user binding و schema version طراحی شوند.
- [ ] `FND-SEC-04` secure-store read/write/decrypt failure به‌صورت fail-closed رفتار کند.
- [ ] `FND-SEC-05` corruption، invalidated key، missing key و wrong-user state رفتار صریح و تست‌شده داشته باشند.
- [ ] `FND-SEC-06` هیچ plaintext fallback برای credential یا sensitive data وجود نداشته باشد.
- [ ] `FND-SEC-07` logout فقط account-owned data را پاک کند و device-wide preference را حفظ کند.
- [ ] `FND-SEC-08` valid، expired، revoked، malformed و offline session transitionها با fake deterministic تست شوند.
- [ ] `FND-SEC-09` contract target storage برای مهاجرت آینده آماده شود، بدون import یا خواندن داده Flutter.
- [ ] `FND-SEC-10` risk مربوط به Isar Flutter fail-open با owner فاز Migration/Security باز بماند و Import Blocked ثبت شود.

### Phase 8 — WorkManager Foundation

- [ ] `FND-WRK-01` WorkManager و Hilt integration از Version Catalog اضافه شوند.
- [ ] `FND-WRK-02` worker factory در composition root نصب شود.
- [ ] `FND-WRK-03` قرارداد unique work naming و account/operation scoping تعریف شود.
- [ ] `FND-WRK-04` retry/backoff/jitter و max-attempt policy صریح و تست شود.
- [ ] `FND-WRK-05` constraints شبکه/شارژ/storage فقط بر اساس نیاز واقعی مدل شوند.
- [ ] `FND-WRK-06` cancellation و logout cleanup رفتار مشخص داشته باشند.
- [ ] `FND-WRK-07` idempotency contract و duplicate enqueue behavior تست شود.
- [ ] `FND-WRK-08` test driver/infrastructure ایجاد شود؛ Feature worker برای Upload/Chat/Sync ساخته نشود.

### Phase 9 — Type-safe Navigation و Deep-link Foundation

- [ ] `FND-NAV-01` route inventory موجود Native برای Startup، Maintenance، Onboarding، Auth و Authenticated boundary ثبت شود.
- [ ] `FND-NAV-02` routeهای string-based موجود به routeهای type-safe همین Scope منتقل شوند.
- [ ] `FND-NAV-03` route argument typed و invalid-argument failure state یکنواخت شود.
- [ ] `FND-NAV-04` back-stack و `popUpTo` رفتار Startup/Auth بدون Regression حفظ شود.
- [ ] `FND-NAV-05` state restoration و process recreation برای route/stateهای موجود تست شود.
- [ ] `FND-NAV-06` logged-out guard و authenticated guard صریح و تست شوند.
- [ ] `FND-NAV-07` navigation effect یک‌بارمصرف باشد و duplicate delivery نداشته باشد.
- [ ] `FND-DL-01` central typed deep-link parser/dispatcher ساخته شود.
- [ ] `FND-DL-02` Manifest، parser و registry فقط برای contractهای تأییدشده و امن هم‌راستا شوند.
- [ ] `FND-DL-03` mismatch `/chat-detail` با canonical contract تعیین تکلیف شود، بدون ساخت Chat route یا UI.
- [ ] `FND-DL-04` URI مقصد Feature آینده به typed pending destination تبدیل شود.
- [ ] `FND-DL-05` cold-start destination تا آماده‌شدن session نگهداری شود.
- [ ] `FND-DL-06` pending destination پس از Login دقیقاً یک‌بار replay شود.
- [ ] `FND-DL-07` Feature پیاده‌نشده به controlled failure state برود؛ crash/no-op/fake route ممنوع باشد.
- [ ] `FND-DL-08` warm/cold × logged-in/logged-out × valid/invalid × supported/unsupported × duplicate suite پاس شود.
- [ ] `FND-DL-09` cold-start intent holding با process recreation و post-login replay تست شود.

### Phase 10 — Startup/Auth Preservation و Fixtures

- [ ] `FND-AUT-01` Flutter Startup/Onboarding/Auth contractهای لازم دوباره read-only trace و تفاوت‌ها ثبت شوند.
- [ ] `FND-AUT-02` maintenance enabled/disabled fake یا fixture injectable فقط در test/debug ساخته شود.
- [ ] `FND-AUT-03` valid session، access-expired+refresh-success، refresh-expired، revoked و malformed fixture ساخته شود.
- [ ] `FND-AUT-04` offline با session معتبر و offline بدون session fixture ساخته شود.
- [ ] `FND-AUT-05` fixtureها از Production behavior و artifact release جدا بمانند.
- [ ] `FND-AUT-06` onboarding completion و version behavior بدون Regression کار کند.
- [ ] `FND-AUT-07` login/register lookup و password/OTP/2FA موجود بدون Regression کار کند.
- [ ] `FND-AUT-08` token refresh و offline session fallback روی stack جدید کار کند.
- [ ] `FND-AUT-09` maintenance و invalid-session fallback deterministic و قابل‌تست باشند.
- [ ] `FND-AUT-10` mandatory auth gateها، logout و authenticated boundary حفظ شوند.
- [ ] `FND-AUT-11` process recreation، rotation، RTL و Light/Dark روی screenهای موجود بررسی شوند.
- [ ] `FND-AUT-12` `AuthenticatedBoundaryScreen` فقط placeholder موجود بماند و Shell پنج‌تب ساخته نشود.
- [ ] `FND-AUT-13` هیچ قابلیت Auth جدید خارج از رفتار فعلی اضافه نشود.

### Phase 11 — Automated Test Foundation

- [ ] `FND-TST-01` unit test Error/Result mapping پاس شود.
- [ ] `FND-TST-02` unit test DispatcherProvider و Clock پاس شود.
- [ ] `FND-TST-03` unit test Session state transitions و StartupResolver پاس شود.
- [ ] `FND-TST-04` unit test Auth ViewModel و onboarding state پاس شود.
- [ ] `FND-TST-05` unit test single-flight refresh و 401 concurrency پاس شود.
- [ ] `FND-TST-06` unit test TLS policy state machine پاس شود.
- [ ] `FND-TST-07` unit test deep-link parser و pending replay پاس شود.
- [ ] `FND-TST-08` unit test DataStore serializer/default/corruption پاس شود.
- [ ] `FND-TST-09` unit test WorkManager retry/idempotency/cancellation policy پاس شود.
- [ ] `FND-TST-10` contract test Auth success و flat/nested/legacy error پاس شود.
- [ ] `FND-TST-11` contract test timeout، refresh، 401 concurrency، maintenance و invalid-session پاس شود.
- [ ] `FND-TST-12` contract test TLS/pinning response تا حد contract موجود پاس شود.
- [ ] `FND-TST-13` database test schema creation/migration/rollback/transaction/corruption پاس شود.
- [ ] `FND-TST-14` instrumentation مسیر Startup→Onboarding و Startup→Auth پاس شود.
- [ ] `FND-TST-15` instrumentation valid session→Authenticated boundary، Maintenance، Offline valid و Invalid session پاس شود.
- [ ] `FND-TST-16` instrumentation rotation، process recreation، RTL و Dark/Light پاس شود.
- [ ] `FND-TST-17` instrumentation cold deep link، post-login replay و duplicate delivery پاس شود.
- [ ] `FND-TST-18` template instrumentation test با suite واقعی Foundation جایگزین شود.

### Phase 12 — Runtime، Build و Security Verification

- [ ] `FND-RUN-01` emulator API 24 یا نزدیک‌ترین نسخه قابل‌دسترسی ثبت و نصب artifact موفق شود.
- [ ] `FND-RUN-02` emulator API 33/34 یا نزدیک‌ترین نسخه قابل‌دسترسی ثبت و نصب artifact موفق شود.
- [ ] `FND-RUN-03` cold و warm startup روی هر target قابل‌دسترسی با چند run ثبت شود.
- [ ] `FND-RUN-04` memory snapshot در checkpoint مشخص Startup/Auth ثبت شود.
- [ ] `FND-RUN-05` مسیرهای Startup، Onboarding، Auth و Authenticated boundary در runtime پیمایش شوند.
- [ ] `FND-RUN-06` offline valid/invalid session در runtime بررسی شود.
- [ ] `FND-RUN-07` process death/recreation و rotation در runtime بررسی شود.
- [ ] `FND-RUN-08` deep link cold/warm و post-login replay در runtime بررسی شود.
- [ ] `FND-RUN-09` maintenance و invalid-session fixture در runtime بررسی شود.
- [ ] `FND-RUN-10` اگر API 24 موجود نبود، blocker دقیق ثبت شود و API 33 جای آن گزارش نشود.
- [ ] `FND-VER-01` هر چهار Variant از clean state build شوند.
- [ ] `FND-VER-02` تمام unit/contract/database/instrumentation testها با شمارش نتیجه پاس شوند.
- [ ] `FND-VER-03` lint با صفر error پاس شود و warningهای جدید توضیح داده شوند.
- [ ] `FND-VER-04` R8/resource shrinking دو release پاس و mappingهای بحرانی بررسی شوند.
- [ ] `FND-VER-05` secret scan با صفر finding پاس و نبود Secret/Keystore/Credential واقعی اثبات شود.
- [ ] `FND-VER-06` `git diff --check` پاس و dependency locks/verification metadata معتبر بمانند.
- [ ] `FND-VER-07` application IDهای Beta/Production و unsigned بودن release بدون secret دوباره تأیید شوند.
- [ ] `FND-VER-08` Flutter و Backend بدون تغییر source باقی بمانند.

### Phase 13 — Exit Gate و گزارش نهایی

- [ ] `FND-GATE-01` Foundation Modular Monolith واقعی، مصرف‌شده و بدون module خالی باشد.
- [ ] `FND-GATE-02` dependency graph بدون cycle و مطابق rules مستند باشد.
- [ ] `FND-GATE-03` Hilt جایگزین کامل `AppContainer` برای Startup/Auth شده باشد.
- [ ] `FND-GATE-04` network stack مشترک و single-flight refresh با concurrency evidence پاس باشد.
- [ ] `FND-GATE-05` Error model و logging redaction تست شده باشند.
- [ ] `FND-GATE-06` Room، Proto DataStore و WorkManager Foundation قابل‌استفاده و تست‌شده باشند.
- [ ] `FND-GATE-07` Native sensitive storage fail-closed باشد.
- [ ] `FND-GATE-08` navigation موجود type-safe و deep-link cold/post-login replay سالم باشد.
- [ ] `FND-GATE-09` Startup، Maintenance، Onboarding و Auth بدون Regression runtime داشته باشند.
- [ ] `FND-GATE-10` maintenance و invalid-session fixture واقعی تست شده باشند.
- [ ] `FND-GATE-11` unit، contract، database، instrumentation و runtime suites تعریف‌شده پاس باشند.
- [ ] `FND-GATE-12` چهار Variant، lint، R8، secret scan و `git diff --check` پاس باشند.
- [ ] `FND-GATE-13` working tree تمیز یا تمام تغییرات آن دقیقاً قابل‌توضیح باشد.
- [ ] `FND-GATE-14` تمام Blockerهای باقی‌مانده owner و شرط خروج مشخص داشته باشند.
- [ ] `FND-GATE-15` فایل‌های تغییرکرده و commitهای کوچک phase-specific گزارش شوند.
- [ ] `FND-GATE-16` module graph، dependency rules، Hilt، Network/TLS، Error/Logging، Room، DataStore و WorkManager گزارش شوند.
- [ ] `FND-GATE-17` Navigation/Deep-link، Startup/Auth parity، test و runtime evidence گزارش شوند.
- [ ] `FND-GATE-18` Riskهای منتقل‌شده و نتیجه نهایی Gate `FND-01` گزارش شوند.
- [ ] `FND-GATE-19` عدم اجرای `DSN-01`، Shell یا Featureهای بعدی با diff و module inventory تأیید شود.
- [ ] `FND-GATE-20` فقط در صورت پاس همه Gateهای الزامی، Status به `done` تغییر کند؛ در غیر این صورت `in-progress` یا `blocked` باقی بماند.

## Blocker Register

| ID | وضعیت اولیه | Owner | شرط خروج |
|---|---|---|---|
| `FND-DL-01` Deep-link mismatch/cold replay | Open | FND-01 Navigation | suite کامل warm/cold و replay سبز |
| `FND-AUTH-01` Maintenance/invalid-session fixture | Open | FND-01 Auth/Test | fixture injectable و instrumentation deterministic |
| `FND-WEB-01` External URL/WebView policy | Open | FND-01 contract + DISC-01 UI | policy suite Foundation پاس؛ UI Feature در DISC-01 |
| `FND-QA-01` Native feature instrumentation | Open | FND-01 | suite واقعی Startup/Auth به‌جای template |
| `FND-TLS-01` Signed pin config contract | Unknown تا بررسی read-only Backend | FND-01 + Backend owner آینده | contract موجود اثبات یا interface/state machine تست‌شده و blocker ثبت شود |
| `FND-API24-01` حداقل SDK runtime | Unknown | FND-01 QA | اجرای API 24/نزدیک‌ترین یا ثبت blocker دقیق |
| `FND-SEC-ISAR-01` Flutter Isar fail-open | Open / Import Blocked | Migration/Security phase آینده | در FND فقط Native fail-closed؛ هیچ تغییر Flutter یا import واقعی |
| `GOV-CVE-01` اجرای scan واقعی dependency | Open | CI activation | اجرای موفق با `NVD_API_KEY` پیش از protected merge/release |
| `REL-SIGN-01` Production signing | Open | REL-02 | تزریق signing اصلی و اثبات upgrade؛ خارج از FND-01 |

## Decisions & Notes

- Tracker در شروع فاز ساخته شد؛ هیچ فایل executable یا build configuration در
  این Task تغییر نکرده است.
- baseline Native پیش از ایجاد tracker تمیز بود. خود این فایل پس از ایجاد
  به‌عنوان تغییر مستند و در Scope `FND-01` ظاهر می‌شود.
- baseline مرجع `3115b0d` دقیقاً HEAD فعلی است؛ requirement «حداقل شامل
  `3115b0d`» پاس است.
- اجرای واقعی روی branch `fnd-01/architecture-foundation` ادامه دارد.
- baseline clean چهار Variant، ۸۰ unit execution، lint، R8 و secret scan پاس
  شدند؛ جزئیات در `docs/evidence/2026-07-25-fnd-01/README.md` ثبت است.
- نخستین clean به‌علت Gradle 8.13 daemon و lock روی `app/build` شکست خورد.
  فقط همان daemon با `gradlew --stop` متوقف شد و clean verification بعدی پاس
  شد؛ daemon 8.14 مربوط به جریان دیگر دست‌نخورده ماند.
- Flutter dirty و متعلق به کاربر است؛ فقط read-only بررسی می‌شود.
- بررسی Git مخزن Backend در sandbox با `dubious ownership` متوقف شد؛ برای
  contract inspection می‌توان فایل‌ها را read-only خواند و نباید global Git
  config را بدون نیاز تغییر داد.
- Production signing و Migration Bridge عمداً خارج از Scope هستند.
- `AuthenticatedBoundaryScreen` می‌تواند placeholder بماند؛ ساخت Shell متعلق
  به `DSN-01` است.
- Isar fail-open در Flutter در این فاز اصلاح نمی‌شود؛ finding باز و import
  واقعی Blocked می‌ماند، اما target storage Native باید fail-closed باشد.
- commitها فقط در صورت درخواست کاربر ساخته می‌شوند؛ strategy پیشنهادی:
  module foundation، Hilt composition root، network/error، Room/DataStore،
  WorkManager، typed navigation/deep-link، Startup/Auth migration و
  tests/runtime evidence.
