# FND-01 Secure Session Storage

## Owner و contract

`:core:security` تنها owner credentialهای Native است. `SessionStore` توسط
Startup، Auth، refresh coordinator و internal authorization consumer استفاده
می‌شود. preferenceهای device-wide مانند onboarding در `:core:datastore` مستقل
هستند و logout آن‌ها را پاک نمی‌کند.

Flutter فقط read-only trace شد:

- `TokenStorage` در `auth_controller.dart` از `FlutterSecureStorage` استفاده می‌کند.
- `SessionManagerServiceV2` tokenها را از همان owner می‌خواند.
- هیچ Flutter storage، Isar database یا legacy secret در FND-01 خوانده یا import
  نمی‌شود؛ target storage Native مستقل و fail-closed است.

## Cryptographic envelope

| جزء | قرارداد |
|---|---|
| الگوریتم | AES/GCM/NoPadding، tag برابر 128 bit |
| key owner | Android Keystore |
| alias | `vista_native_session_aes_v2` |
| schema | `1` داخل ciphertext |
| envelope | `v1.accountBinding.iv.ciphertext` |
| account binding | SHA-256 از user id، بدون ذخیره user id به‌صورت plaintext |
| AAD | envelope version + schema version + account binding |

tokenها، user id، expiry، profile/password state و display name همگی داخل
ciphertext هستند. تغییر account binding، IV یا ciphertext توسط GCM/AAD رد می‌شود.

## Fail-closed behavior

- malformed envelope، JSON/schema نامعتبر، wrong-account binding، missing key،
  invalidated/unreadable key یا GCM failure هرگز session برنمی‌گرداند.
- failure موجب پاک‌شدن encrypted payload و reset کنترل‌شده alias می‌شود تا login
  بعدی بتواند key سالم بسازد.
- write/clear ناموفق exception می‌دهد؛ هیچ SharedPreferences/plaintext fallback
  وجود ندارد.
- session منقضی اما سالم decrypt می‌شود تا Startup تصمیم refresh را بگیرد.
- terminal/revoked auth فقط account-owned session را پاک می‌کند؛ failure شبکه
  session را برای offline fallback حفظ می‌کند.

## Migration boundary

این envelope، contract target برای migration آینده است، اما FND-01 هیچ key،
token یا رکوردی از Flutter/Isar نمی‌خواند. finding `FND-SEC-ISAR-01` تا رفع
fail-open در owner Migration/Security باز و Import Blocked می‌ماند.
