# FND-01 Hilt Ownership

## حذف graph دستی

پیش از Phase 2، `AppContainer` مالک ساخت مستقیم `OkHttpClient`،
`OnboardingStore`، `SessionStore`، `AuthRemoteDataSource`،
`AuthRepository` و `AuthenticationStateOwner` بود. سه Factory دستی نیز
`StartupViewModel`، `OnboardingViewModel` و `AuthViewModel` را می‌ساختند.

پس از انتقال، `VistaApplication` با `@HiltAndroidApp` و `MainActivity` با
`@AndroidEntryPoint` تنها process/activity entry pointها هستند. هر سه
ViewModel با `@HiltViewModel` و `hiltViewModel()` resolve می‌شوند و هیچ
Service Locator یا `ViewModelProvider.Factory` در production source باقی
نمانده است.

## Authoritative owner و scope

| Dependency/contract | Binding owner | Scope | دلیل |
|---|---|---|---|
| internal `OkHttpClient` | `:core:network` | `Singleton` | connection pool مشترک در process |
| API base URL | `:app` environment composition | unscoped immutable value | مقدار BuildConfig وابسته به Variant |
| `OnboardingStore` | implementation/factory در `:core:datastore`، singleton binding در `:app` composition | `Singleton` | DataStore owner مستقل از Hilt می‌ماند و process lifecycle را composition root تعیین می‌کند |
| `SessionStore` | `:core:security` | `Singleton` | یک Keystore/session owner |
| `AuthRemoteDataSource` | `:feature:auth` data | `Singleton` | client state مشترک و stateless API wrapper |
| `AuthRepository` | `:feature:auth` data | `Singleton` | تنها implementation journey فعلی |
| `AuthenticationStateOwner` | `:feature:auth` | `Singleton` | session routing state در سطح process |
| `NetworkMonitor` | `:core:network` | `Singleton` | callback شبکه مشترک، lifecycle-aware و قابل‌جایگزینی با fake |
| `DispatcherProvider` | contract در `:core:common`، binding در `:app` | `Singleton` | dispatcherهای process و test replacement یکتا |
| application `CoroutineScope` | `:app` composition، با `@ApplicationScope` | `Singleton` | `SupervisorJob + Default` برای جریان‌های process-scoped |
| `FoundationTelemetry` | contract در `:core:common`، binding در `:app` | `Singleton` | فقط health signalهای Foundation؛ فعلاً No-op |
| `SecureLogger` | contract در `:core:common`، sink در `:app` | `Singleton` | logger ساخت‌یافته و redact‌شده؛ خاموش در release |
| `EpochClock` | contract در `:core:common`، binding در `:app` | `Singleton` | زمان واحد و fake صریح در test |
| Startup/Auth/Onboarding ViewModel | `:feature:auth` | `ViewModel` | lifecycle و cancellation owner صفحه |

`ActivityRetained` binding مستقلی در graph حاضر وجود ندارد چون dependency
مصرف‌شده‌ای با عمر بین Activity و ViewModel لازم نیست. ایجاد scope بدون
consumer ممنوع است.

## Qualifierها

- `@InternalApi` مانع تزریق تصادفی client داخلی بدون هویت می‌شود.
- `@InternalEnvironment` config داخلی هر Variant را از external config جدا می‌کند.
- `@ApplicationScope` scope طول‌عمر process را از scopeهای ViewModel جدا می‌کند.
- external/media client qualifier تا زمان consumer واقعی در Phase Network
  ایجاد نمی‌شود؛ dispatcherها با contract تایپ‌شده‌ی `DispatcherProvider`
  جایگزین می‌شوند و qualifier اضافی بدون consumer ساخته نشده است.

## Test replacement

Unit testهای Startup/Auth قراردادها را با fakeهای constructor-injected و
`EpochClock` deterministic جایگزین می‌کنند. instrumentation smoke نیز
`MainActivity` واقعی را با graph production Hilt launch می‌کند؛ بنابراین حذف
DI دستی فقط compile-level نیست.
