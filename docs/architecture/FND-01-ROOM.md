# FND-01 Room Foundation

## نیاز واقعی

TLS policy تأییدشده باید در restart process حفظ شود. در غیر این صورت policy
`enforce` قبلی تا bootstrap بعدی از دست می‌رود و اتصال می‌تواند ناخواسته با
حالت `off` شروع شود. Room فقط برای همین داده‌ی Foundation ایجاد شده است.

تنها entity دیتابیس `verified_tls_policy` است. هیچ entity مربوط به Feed،
Chat، Story، Profile، cache محتوایی یا داده‌ی Flutter/Isar وجود ندارد.

## Ownership

| جزء | Owner | Consumer |
|---|---|---|
| `VistaFoundationDatabase` | `:core:database` | composition root |
| `VerifiedTlsPolicyDao` | `:core:database` | `FoundationModule` |
| mapping Entity/Domain | `:app` composition | `TlsPolicyStore` |
| policy state/evaluation | `:core:network` | internal TLS client |

UI و feature به DAO دسترسی مستقیم ندارند. app فقط در composition root رکورد
Room را به contract شبکه map می‌کند. read اولیه روی `DispatcherProvider.io`
انجام می‌شود. update policy تا وقتی transaction پاک‌کردن/نوشتن موفق نشده،
در حافظه accepted نمی‌شود؛ در failure آخرین policy معتبر حفظ می‌شود.

## Schema و migration

database در version 2 است. migration واقعی `1 -> 2` مدل تک-pin قدیمی را به
contract فعلی versioned با `current_pin_sha256` و `next_pin_sha256` تبدیل
می‌کند. schema export با KSP روی مسیر version-controlled
`core/database/schemas` فعال است.

fallback destructive و downgrade destructive در builder وجود ندارد. تست
instrumentation موارد fresh create، migration، rollback transaction،
corruption و downgrade را پوشش می‌دهد.

## Encryption و sensitive data

certificate fingerprint، mode، revision و expiry credential یا user data
نیستند؛ بنابراین دیتابیس Foundation در این فاز رمزگذاری جداگانه ندارد.
session token، refresh token، private key، message، encryption payload و
account data اجازه ورود به Room را ندارند و در `core:security`/Keystore باقی
می‌مانند.

هیچ Isar replacement، Flutter Migration Bridge، import یا خواندن فایل Flutter
اجرا نمی‌شود. finding fail-open قبلی با owner Migration/Security باز می‌ماند.
