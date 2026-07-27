# FND-01 WorkManager Foundation

## Integration

نسخه‌های Foundation:

- `androidx.work:work-runtime-ktx:2.11.2`
- `androidx.hilt:hilt-work:1.3.0`
- `androidx.hilt:hilt-compiler:1.3.0`

`VistaApplication`، `Configuration.Provider` را پیاده می‌کند و
`HiltWorkerFactory` را به WorkManager می‌دهد. default
`WorkManagerInitializer` از Manifest حذف شده، ولی سایر AndroidX Startup
initializerها حفظ شده‌اند.

این wiring مطابق راهنمای رسمی
[Hilt + WorkManager](https://developer.android.com/training/dependency-injection/hilt-jetpack)
است. WorkManager 2.11.2 نسخه stable ثبت‌شده در
[AndroidX Work release notes](https://developer.android.com/jetpack/androidx/releases/work)
است.

## Ownership

`:core:worker` owner قرارداد durable-work و cancellation account-scoped است.
consumer واقعی آن terminal-session cleanup در `SessionRefreshCoordinator` است:
قبل از پاک‌کردن session، account id خوانده می‌شود و تمام workهای tagشده برای
همان حساب cancel می‌شوند.

هیچ production Worker برای Upload، Chat، Sync یا Feature آینده ساخته نشده است.
`FoundationProbeWorker` فقط در source set تست دستگاه وجود دارد.

## Identity و idempotency

`WorkIdentity` از این مؤلفه‌ها unique name می‌سازد:

```text
vista-work-v1:<operation>:<account-hash>:<operation-uuid>
```

- account id خام در name/tag نشت نمی‌کند؛ SHA-256 کوتاه‌شده استفاده می‌شود.
- operation نام محدود و validate‌شده دارد.
- operation UUID همان idempotency key است.
- enqueue contract برای عملیات durable برابر `ExistingWorkPolicy.KEEP` است؛
  duplicate با identity یکسان worker دوم ایجاد نمی‌کند.

## Retry

- حداکثر attempt: `5`
- base delay: `30s`
- exponential growth: `2^attempt`
- deterministic jitter: بازه `-20%..+20%` از idempotency key + attempt
- upper bound: `5h`
- پس از attempt نهایی: `Exhausted`، بدون retry بی‌نهایت

Feature worker آینده باید این تصمیم را مصرف و terminal failure را durable ثبت
کند؛ FND-01 عمداً payload/worker مربوط به Feature نمی‌سازد.

## Constraints

default durable network operation فقط این نیازها را اعلام می‌کند:

- network = `CONNECTED`
- storage not low = true
- charging = false
- device idle = false

Feature owner در آینده فقط با نیاز واقعی می‌تواند این requirements را تغییر دهد.

## Test infrastructure

`work-testing`، `WorkManagerTestInitHelper` و `TestDriver` روی instrumentation
فعال‌اند. تست integration، `KEEP` dedup و account-tag cancellation را روی
WorkManager واقعیِ test driver اثبات می‌کند.
