# Phase 7 — Secure Session Evidence

## Trace

Native callsiteها:

- `StartupResolver`: read، terminal clear و offline fallback
- `AuthViewModel`: login/OTP/password save و password transition
- `SessionRefreshCoordinator`: single-flight refresh persist یا terminal clear
- `SessionAccessTokenProvider`: internal request token read

Flutter read-only:

- `E:\vista\lib\features\auth\providers\auth_controller.dart`
- `E:\vista\lib\services\session_manager_service_v2.dart`
- `E:\vista\lib\security\secure_kv_store.dart`

هیچ فایل Flutter یا Backend تغییر نکرد.

## Unit evidence

```text
.\gradlew.bat --no-daemon :core:security:testDebugUnitTest --rerun-tasks
  -Pvista.isolatedBuildOutput=fnd-build-output-fnd26
  -Pvista.buildLogicOutput=build-logic-output-fnd26
  -Pkotlin.incremental=false

BUILD SUCCESSFUL in 54s
30 actionable tasks: 30 executed
8 tests / 0 failures / 0 errors / 0 skipped
```

سناریوها: ciphertext round-trip و نبود plaintext، malformed payload، missing
key، invalidated key، account-binding tamper، expired-but-decryptable session،
password transition و logout account-only.

گزارش‌های `fnd25` همچنین 22 تست Startup/Auth را بدون failure/error/skip ثبت
کردند. transitionهای fresh، expired+refresh، terminal unauthorized/revoked،
transient failure، offline fallback و malformed secure payload به‌صورت
deterministic پوشش دارند. command aggregate به سقف 124 ثانیه میزبان رسید و
به‌عنوان pass command گزارش نمی‌شود؛ XML resultها شاهد مستقل test execution هستند.

## Android Keystore instrumentation

```text
.\gradlew.bat --no-daemon :core:security:connectedDebugAndroidTest
  --rerun-tasks --write-locks --write-verification-metadata sha256
  -Pvista.isolatedBuildOutput=fnd-build-output-fnd27
  -Pvista.buildLogicOutput=build-logic-output-fnd27
  -Pkotlin.incremental=false

Starting 3 tests on Medium_Phone_2(AVD) - 13
Finished 3 tests on Medium_Phone_2(AVD) - 13
BUILD SUCCESSFUL in 1m 40s
63 actionable tasks: 63 executed
```

تست‌های واقعی Keystore شامل round-trip بدون plaintext، حذف alias/missing key و
corrupt ciphertext fail-closed بودند.

## App/Hilt compile checkpoint

```text
.\gradlew.bat --no-daemon :app:compileBetaDebugKotlin
  -Pvista.isolatedBuildOutput=fnd-build-output-fnd29
  -Pvista.buildLogicOutput=build-logic-output-fnd29
  -Pkotlin.incremental=false

BUILD SUCCESSFUL in 1m 16s
111 actionable tasks: 111 executed
```

یک اجرای assemble جداگانه در `fnd28` پس از timeout میزبان APK
`app-beta-debug.apk` با اندازه 15,665,239 bytes تولید کرد؛ چون exit code همان
process ثبت نشد، فقط artifact evidence است و build pass این فاز از compile
checkpoint صریح بالا گرفته شده است.

## نتیجه

Native secure storage اکنون versioned، user-bound و fail-closed است؛ logout
مرز account/device را حفظ می‌کند و target contract بدون اجرای import Flutter
آماده است. `FND-SEC-ISAR-01` همچنان Open / Import Blocked باقی ماند.
