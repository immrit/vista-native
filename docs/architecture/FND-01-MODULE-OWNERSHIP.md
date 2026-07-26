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
| Auth UI/domain/data و Startup/Onboarding flow | `:feature:auth` | `:app` navigation graph |
| dispatcher test rule | `:core:testing` | Auth ViewModel tests |
| Android process، theme/resources و composition | `:app` | `VistaApplication`/`MainActivity` |

`core:database` عمداً در Phase 1 ایجاد نشده است: Startup/Auth فعلی هیچ schema
یا DAO مصرف‌شده‌ای ندارد. این ماژول فقط در Phase 5 و هم‌زمان با schema و
consumer واقعی Room ایجاد می‌شود؛ بنابراین graph حاضر ماژول خالی ندارد.

## Graph مصرف‌شده

```text
:app
  ├─> :feature:auth
  ├─> :core:common
  ├─> :core:model
  ├─> :core:network
  ├─> :core:datastore
  ├─> :core:security
  └─> :core:testing (test-only)

:feature:auth
  ├─> :core:common
  ├─> :core:model
  ├─> :core:network
  ├─> :core:datastore
  └─> :core:security

:core:network  ─> :core:common
:core:security ─> :core:model
:core:common, :core:model, :core:datastore, :core:testing ─> no project module
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
