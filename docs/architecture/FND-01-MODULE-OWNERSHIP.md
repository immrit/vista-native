# FND-01 Module Ownership

این سند snapshot اجرایی Phase 1 است. rule متناظر آن در
`scripts/verify-module-boundaries.ps1` روی هر checkpoint اجرا می‌شود.

## انتقال ownership از `:app`

| مسئولیت پیشین در `:app` | Owner جدید | Consumer واقعی |
|---|---|---|
| `Outcome` | `:core:common` | Auth repository/data source |
| Auth/session modelها | `:core:model` | Auth، Startup و secure storage |
| HTTP failure classification | `:core:network` | Auth remote data source |
| onboarding preference | `:core:datastore` | Startup و Onboarding |
| encrypted session persistence | `:core:security` | Startup و Auth |
| durable-work identity، retry policy و account cancellation | `:core:worker` | terminal-session cleanup و WorkManager test infrastructure |
| verified TLS policy persistence | `:core:database` | app composition و `TlsPolicyStore` |
| Auth UI/domain/data و Startup/Onboarding flow | `:feature:auth` | `:app` navigation graph |
| dispatcher test rule | `:core:testing` | Auth ViewModel tests |
| Android process، theme/resources و composition | `:app` | `VistaApplication`/`MainActivity` |

`core:database` عمداً در Phase 1 ایجاد نشده بود: Startup/Auth آن زمان هیچ schema
یا DAO مصرف‌شده‌ای نداشت. در Phase 5، نیاز واقعی نگهداری آخرین TLS policy
تأییدشده به schema، DAO و consumer واقعی تبدیل شد؛ بنابراین ماژول خالی نیست.

## Graph مصرف‌شده

```text
:app
  ├─> :feature:auth
  ├─> :core:common
  ├─> :core:model
  ├─> :core:network
  ├─> :core:database
  ├─> :core:datastore
  ├─> :core:security
  ├─> :core:worker
  └─> :core:testing (test-only)

:feature:auth
  ├─> :core:common
  ├─> :core:model
  ├─> :core:network
  ├─> :core:datastore
  ├─> :core:security
  └─> :core:worker

:core:network  ─> :core:common
:core:security ─> :core:common, :core:model
:core:common, :core:model, :core:database, :core:datastore, :core:testing,
:core:worker
  ─> no project module
```

## Dependency rules

- `:app` فقط Android entry point، navigation composition و visual adapterهای
  resource/theme موجود را مالک است.
- feature به feature دیگر وابسته نمی‌شود؛ flowهای فعلی Startup، Onboarding و
  Auth یک journey واحد در `:feature:auth` هستند.
- Domain و `:core:model`/`:core:common` به Android/AndroidX وابسته نیستند.
- contractهای Auth در `domain` و implementationهای HTTP در `data` قرار دارند.
- هر dependency جدید باید در allowlist اسکریپت مرزبندی ثبت و consumer واقعی
  آن مشخص شود؛ cycle و production module بدون source خطا هستند.
- ساخت Design System، Shell یا feature آینده در این graph ممنوع است.
