# FND-01 Common Foundation Contracts

## Result و Error

`Outcome<T>` تنها wrapper نتیجه در Foundation است. خطا فقط با `AppError`
منتقل می‌شود و category، پیام فارسی امن، کد backend، زمان retry و نام type
علت را نگه می‌دارد. متن exception و stack trace وارد contract، telemetry یا
log field نمی‌شود.

`ErrorClassifier` خطاهای HTTP، timeout، TLS، network، JSON و unknown را به
categoryهای پایدار نگاشت می‌کند. `BackendErrorParser` سه envelope فعلی
backend را می‌پذیرد:

- flat: `code/message/retry_after_seconds`
- nested: `error.code/error.message`
- legacy: اولین عضو `errors[]`

## Dispatcher، Clock و Coroutine Scope

`DispatcherProvider` owner یکتای `Default`، `IO` و `Main` است. repository
احراز هویت عملیات blocking/network را روی `io` اجرا می‌کند و unit test آن
provider را با `StandardTestDispatcher` جایگزین می‌کند.

`EpochClock` برای expiry نشست در `StartupResolver` مصرف می‌شود و تست‌ها زمان
ثابت تزریق می‌کنند. application scope در composition root از
`SupervisorJob + dispatchers.default` ساخته می‌شود. این scope فقط برای
جریان‌های process-scoped مانند `NetworkMonitor.state` مجاز است؛ کار صفحه باید
در `viewModelScope` بماند. child failure نباید siblingها را لغو کند و
callback شبکه هنگام نبود subscriber بعد از پنج ثانیه unregister می‌شود.

## Environment و Network State

`AppEnvironment` فقط دو نام `BETA` و `PRODUCTION` دارد. base URL داخلی باید
HTTPS و بدون credential باشد؛ مقدار نهایی از BuildConfig همان Variant گرفته
می‌شود. override ناامن در زمان ساخت contract fail-fast می‌شود.

`NetworkMonitor` یک `StateFlow<NetworkState>` و snapshot هم‌زمان ارائه می‌دهد.
پیاده‌سازی Android فقط شبکه‌ی دارای `INTERNET` و `VALIDATED` را online می‌داند.
Startup در حالت offline درخواست maintenance/refresh نمی‌فرستد و از session
محلی با علامت offline استفاده می‌کند. fake صریح این رفتار را deterministic
تست می‌کند.

## Logging و Telemetry

`SecureLogger` event و field ساخت‌یافته دارد، در release خاموش است و قبل از
رسیدن داده به sink، key/valueهای حساس را redact می‌کند. token، Authorization،
cookie، phone، message، payload رمزنگاری، key، secret، password و credential
مجاز به log شدن نیستند.

`FoundationTelemetry` فقط signalهای سلامت Foundation مانند نتیجه‌ی request
احراز هویت و تصمیم startup را ثبت می‌کند. identifier، payload، متن پیام،
داده‌ی تبلیغاتی، profiling کاربر و شناسه‌ی شخصی در contract آن وجود ندارد.
binding فعلی `NoOpFoundationTelemetry` است تا collector بدون policy تصویب‌شده
ساخته نشود.
