# Myket Review Rejections Fix (Cafe Bazaar References & Auth 5xx Resilience)

**Type:** bug
**Status:** done
**Progress:** `[██████████] 100%`

## Problem / Goal
Myket rejected the app submission with two issues:
1. "اشاره به کافه بازار را از توضیحات نسخه فارسی برنامه حذف نمایید" — Remove Cafe Bazaar references from the Persian app descriptions/UI in the Myket flavor (and remind user to verify the Myket developer panel Persian description as well).
2. "در زمان بررسی این برنامه توسط کارشناس تیم مایکت، پس از درج شماره موبایل، عملکرد صحیح وجود نداشته و پیام خطای: 'سرور موقتا در دسترس نیست' نمایش داده شده و امکان دسترسی به محتوای این قسمت وجود ندارد." — After entering a mobile number on the login screen, the app showed `"سرور موقتاً در دسترس نیست. لطفاً کمی بعد دوباره تلاش کنید"` (from `ErrorClassifier.kt:43`, triggered on HTTP `500..599` without a Persian backend error message).

## Root cause / Design
1. **Cafe Bazaar references in Myket build:**
   - `StoreChannel` & `StoreBillingConfig` live in `feature/profile/.../pricing/StoreBillingConfig.kt`, so `:app` and `:feature:services` could not inject/branch on `StoreChannel`.
   - `app/src/main/java/ir/coffevista/vista_native/update/VistaUpdateManager.kt` (`openStoreForUpdate`) hardcodes `bazaar://details?id=...` and `https://cafebazaar.ir/app/...`.
   - `app/src/main/java/ir/coffevista/vista/MainActivity.kt` lines 427, 433, 621 hardcode "کافه‌بازار" / "بازار" in the mandatory update dialog and update toast.
   - `feature/profile/.../settings/about/AboutSettingsScreen.kt` lines 215–262 hardcode "کافه‌بازار" / "بازار" / `bazaar://` / `cafebazaar.ir` in the rating dialog, update fallback, and snackbar messages.
   - `feature/services/.../ui/nearby/` (`NearbyScreen.kt`, `NearbyShopSheet.kt`, `NearbyViewModel.kt`, `NearbyApi.kt`, `NearbyRepository.kt`) hardcodes "کافه‌بازار" strings and only uses Poolakey (Cafe Bazaar billing) for coin purchases instead of branching on `StoreChannel` (supporting `MyketBillingClient` + `/v1/payment/myket-verify` in Myket builds).
   - **Design:** Move `StoreChannel` & `StoreBillingConfig` into `:core:common` (keeping compatibility typealiases in `feature/profile`), move `MyketBillingClient` into `:core:designsystem` using direct `IBinder` transactions (avoiding `poolakey` dependency in `:core:designsystem` and keeping all `gradle.lockfile`s untouched; keeping compatibility typealiases in `feature/profile`), inject `StoreBillingConfig` into `VistaUpdateManager` and `NearbyViewModel`, pass `StoreChannel` to `AboutSettingsScreen` from `AppNavGraph`, and localize all store-facing strings/URIs/billing per `StoreChannel`.

2. **Auth 5xx `"سرور موقتاً در دسترس نیست"` after entering mobile number:**
   - Live verification against `https://api.coffevista.ir/v1/auth/lookup` (`200 OK`) and `https://api.coffevista.ir/v1/auth/send-otp` (`202 Accepted`) shows the production API endpoints are currently healthy, and `BackendErrorParser` + `ErrorClassifier` properly surface Persian backend messages when `SMS_DELIVERY_FAILED` (`502`) occurs.
   - However, `ErrorClassifier.kt:43` produces `"سرور موقتاً در دسترس نیست. لطفاً کمی بعد دوباره تلاش کنید"` whenever an endpoint returns `500..599` without a Persian message (e.g., ArvanCloud `502`/`503`/`504` during deploys or origin timeouts).
   - `BoundedRetryInterceptor` (`RetryPolicy.kt`) only retries `GET`/`HEAD`/`OPTIONS` on `IOException`, so the read-only `POST v1/auth/lookup` call has zero retry resilience against transient `502`/`503`/`504` or `IOException` blips.
   - Also keep `AuthApi` explicitly in `app/proguard-rules.pro` and ensure `DefaultAuthRepository` / `OkHttpAuthRemoteDataSource` retries idempotent `lookupIdentifier` once on transient `502`/`503`/`504`/`IOException`.

## Phases

### Phase 1 — Investigate
- [x] Locate all Cafe Bazaar strings, URIs, and billing references across `app/`, `core/`, `feature/`
- [x] Trace mobile-number auth flow (`AuthScreen` -> `AuthViewModel` -> `DefaultAuthRepository` -> `OkHttpAuthRemoteDataSource` -> `ErrorClassifier` -> `vista-backend` handlers)
- [x] Verify live `api.coffevista.ir` `/v1/auth/lookup`, `/v1/auth/send-otp`, `/api/v1/system/status`, `/api/v1/system/check-update`

### Phase 2 — Implement
- [x] Move `StoreChannel` & `StoreBillingConfig` to `:core:common` and `MyketBillingClient` (pure `IBinder`) to `:core:designsystem` with compatibility aliases in `feature/profile`
- [x] Update `VistaUpdateManager.kt` and `MainActivity.kt` to use `StoreChannel` for update dialogs, toasts, and store intents (Myket / Bazaar / Direct)
- [x] Update `AboutSettingsScreen.kt` and `AppNavGraph.kt` to use `StoreChannel` for rating/update text and store URLs
- [x] Update `NearbyApi.kt`, `NearbyRepository.kt`, `NearbyViewModel.kt`, `NearbyShopSheet.kt`, and `NearbyScreen.kt` to support `StoreChannel.Myket` (`myket-verify`, `MyketBillingClient`, Myket UI strings) and `Direct` without Cafe Bazaar text
- [x] Add safe bounded retry (1 retry on transient `502`/`503`/`504`/`IOException`) for idempotent `lookupIdentifier` in `OkHttpAuthRemoteDataSource.kt` + keep `AuthApi` in `app/proguard-rules.pro`

### Phase 3 — Verify
- [x] Run unit tests across `:core:common`, `:core:designsystem`, `:core:network`, `:feature:auth`, `:feature:profile`, `:feature:services` and `:app:compileProductionMyketReleaseKotlin` (`BUILD SUCCESSFUL`)
- [x] Verify zero Cafe Bazaar user-facing strings remain in Myket code paths (`git grep "کافه"` shows only `StoreChannel.Bazaar` branches)
- [x] Confirm auth lookup & store-channel tests pass

## Decisions & Notes
- `vista_native` is native Kotlin/Compose (multi-module Gradle), not Flutter; adapted verification to Gradle unit tests + Kotlin compilation instead of `flutter analyze`.
- Also remind the user to check the Myket Developer Panel's Persian app description (`توضیحات نسخه فارسی برنامه`) because Myket's point #1 often refers to the store listing text in addition to in-app Persian texts.
- Leave unrelated pre-existing working-tree edits untouched (`InternalNetworkModule.kt`, `PolicyAwareInternalCallFactory.kt`, `PricingScreen.kt`, etc.).
