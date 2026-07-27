# FND-01 Proto DataStore

## Ownership و محدوده schema

`:core:datastore` تنها owner پیاده‌سازی preferenceهای device-wide در Foundation
است. schema فعلی عمداً به سه field غیرحساس محدود است:

| Field | نقش | Scope |
|---|---|---|
| `schema_version` | evolution صریح schema | device-wide |
| `onboarding_completed` | پایان journey onboarding | device-wide |
| `onboarding_version` | invalidation کنترل‌شده هنگام تغییر journey | device-wide |

token، session، credential، private key و داده account-scoped در Proto مجاز
نیستند. این داده‌ها فقط در `:core:security` نگهداری می‌شوند.

## Wiring

`OnboardingStoreFactory` در owner module، `DataStore<AppPreferences>` را با
serializer، corruption handler، migrationها، application scope و IO dispatcher
می‌سازد. `:app` فقط lifecycle singleton را در Hilt composition root تعیین
می‌کند؛ هیچ generated protobuf type به graph Hilt نشت نمی‌کند.

`ProtoOnboardingStore` consumer واقعی این DataStore است. Startup و Onboarding
همان contract قبلی `OnboardingStore` را به‌صورت suspend مصرف می‌کنند؛ module
یا abstraction بدون consumer ایجاد نشده است.

## Migration، default و corruption

- schema فعلی version `1` و onboarding journey version برابر `1.0.0` است.
- default deterministic برابر version 1، `completed=false` و version خالی است.
- SharedPreferences قدیمی `vista_onboarding` دقیقاً یک بار migrate و keyهای
  منتقل‌شده پاک می‌شوند.
- completion متعلق به onboarding version قدیمی پس از schema migration معتبر
  تلقی نمی‌شود.
- protobuf نامعتبر با `CorruptionException` fail می‌شود و
  `ReplaceFileCorruptionHandler` فقط preference غیرحساس را به default امن برمی‌گرداند.

## Retention

logout و terminal refresh فقط `SessionStore` account-owned را پاک می‌کنند.
`OnboardingStore` device-wide مستقل می‌ماند؛ تست Startup این مرز را بعد از
پاک‌شدن session بررسی می‌کند.

## Scope boundary

هیچ token، کلید خصوصی یا session به DataStore منتقل نشده، هیچ table یا import
برای Flutter/Isar ساخته نشده و هیچ Feature مربوط به `DSN-01` پیاده‌سازی نشده است.
