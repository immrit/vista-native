# FND-01 Network، TLS و External URL

## Client ownership

| Client | Qualifier | Trust/Auth | Consumer |
|---|---|---|---|
| Internal API | `@InternalApi` | OS trust + verified dynamic policy؛ Authorization فقط exact internal host | Retrofit Auth API |
| Bootstrap | `@BootstrapApi` | فقط OS trust، بدون pin و auth | system status و TLS mismatch report |
| External/media | `@ExternalMedia` | فقط OS trust، بدون internal auth interceptor | Foundation client contract و runtime DI smoke |

هر سه client timeoutهای connect/read/write/call صریح دارند. retry داخلی OkHttp
خاموش است و `BoundedRetryInterceptor` فقط GET/HEAD/OPTIONS را حداکثر یک بار
پس از transport failure غیر-TLS تکرار می‌کند. POSTهای login/OTP/refresh replay
خودکار ندارند.

stack درخواست داخلی `Retrofit 2.11 + OkHttp 4.12 + Kotlinx Serialization`
است. `OkHttpAuthRemoteDataSource` اکنون API واقعی Auth را از Retrofit مصرف
می‌کند و parsing پاسخ با `JsonObject` Kotlinx انجام می‌شود.

## Refresh و session transition

`SingleFlightSessionRefreshCoordinator` تمام callerهای هم‌زمان یک refresh
token را روی یک `Deferred` process-scoped coalesce می‌کند. پاسخ موفق پیش از
تحویل به caller در secure store ذخیره می‌شود. فقط Unauthorized، Forbidden و
AccountDisabled نشست را terminal و پاک می‌کنند؛ timeout/network/server و
malformed response نشست را نگه می‌دارند و fallback آفلاین می‌دهند.

refresh یک POST است و retry interceptor آن را replay نمی‌کند. Startup فقط در
expiry صریح coordinator را صدا می‌زند؛ هیچ Authenticator بازگشتی یا مسیر
refresh-on-refresh در graph وجود ندارد.

## TLS policy

policy مدل‌شده دارای contract version، revision، mode، current pin، next pin
و expiry است. `TlsPolicyStore` فقط policy با signature تأییدشده، version
پشتیبانی‌شده، revision جدیدتر و pin معتبر را قبول می‌کند؛ ورودی unsigned،
stale یا invalid policy قبلی را جایگزین نمی‌کند.

- `off`: OS trust کافی است.
- `monitor`: missing/expired/mismatch اجازه داده و فقط گزارش امن می‌شود.
- `enforce`: missing/expired/mismatch اتصال را fail-closed رد می‌کند.
- current و next pin هم‌زمان در rotation window معتبرند.

`PolicyTrustManager` ابتدا platform trust را اجرا می‌کند و بعد SHA-256
certificate DER را می‌سنجد. تغییر revision هنگام call بعدی client داخلی را
زیر lock بازسازی و pool/executor قبلی را dispose می‌کند. گزارش mismatch فقط
host، fingerprint دیده‌شده و reason را از bootstrap client می‌فرستد؛ token،
cookie، URL query، certificate bytes یا user data ارسال نمی‌شود.

## Backend read-only finding

بررسی read-only در `E:\vista-backend` وجود endpointهای
`/api/v1/system/status` و `/api/v1/system/tls-report` را تأیید کرد. contract
فعلی فقط `mode`، آرایه‌ی `fingerprints` و `expires_at` دارد و config از Redis
می‌آید. signature، contract version، revision و فیلدهای current/next صریح
وجود ندارند؛ Redis unavailable/invalid نیز backend را به `off` می‌برد.

بنابراین Native policy فعلی Backend را unsigned مشاهده می‌کند ولی فعال
نمی‌کند. فعال‌سازی monitor/enforce تا ارائه‌ی envelope امضاشده و versioned
در blocker `FND-TLS-01` باز است؛ Backend در این Task تغییر نکرد.

## External URL

`ExternalUrlPolicy` فقط HTTPS، host دقیق allowlisted، path prefix مجاز و URL
بدون user-info/fragment را با disposition مرورگر خارجی می‌پذیرد. subdomain
ساختگی، cleartext، credential-bearing URL و path خارج allowlist رد می‌شوند.
Authorization، Cookie، Proxy-Authorization و X-Api-Key هرگز به مقصد خارجی
forward نمی‌شوند.
