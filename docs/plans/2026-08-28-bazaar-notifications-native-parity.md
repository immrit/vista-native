# Native Bazaar Payment and Notifications Parity

Status: in progress  
Last reviewed: 2026-08-28  
Reference projects: `E:\vista` (Flutter) and `E:\vista_native` (Native)

## Executive status

| Area | Native status | Evidence | Remaining blocker |
|---|---:|---|---|
| FCM service registration | Implemented | `VistaFirebaseMessagingService`, manifest service | Requires Firebase app registration for the Native package |
| FCM token sync | Implemented | `PushTokenRegistrar`, auth-state retry | Runtime test needs a valid Native Firebase config |
| Notification channels/icon | Implemented | `social_notify`, `chat_messages`, `post_upload_progress`, copied icon | Device-level visual test still required |
| Notification tap routing | Partial/implemented for core routes | `MainActivity` deep-link mapping | Comment-specific route and inbox actions need product/API confirmation |
| Notification settings | Implemented | profile notification settings API/UI | Server-side preference enforcement in the push service is not verified |
| Zibal payment | Implemented | request, external launch, verify, profile refresh | Requires end-to-end merchant test |
| Bazaar payment SDK | Not buildable yet | Flutter uses Poolakey `2.2.0` | Poolakey is not cached; JitPack/repository approval and Bazaar app setup required |
| Bazaar server verification | Existing Flutter contract identified | `POST /payment/bazaar-verify` | Native package/signing/product registration must be accepted by backend/Bazaar |

## Changes completed in Native

- Copied Flutter's notification icon to `app/src/main/res/drawable/ic_notification.png`.
- Copied Flutter Firebase configuration as `app/google-services.json.flutter-reference.json`.
- Kept the copied Firebase file reference-only because its client package is
  `ir.coffevista.vista`, while Native production is
  `ir.coffevista.vista_native.production`.
- Added Bazaar billing permission, Bazaar package/billing intent queries, default
  notification icon, and default notification channel metadata to the manifest.
- Changed FCM registration from a hardcoded URL to the configured internal API
  base URL.
- Retried token synchronization after authentication becomes signed in.
- Added notification tap mapping for chat, post, story, and profile payloads.
- Added support for Flutter payload types `chat_message`, `reaction`,
  `comment_reply`, follow/suggestion, and digest notifications.
- Switched displayed notifications to the copied monochrome notification icon.

## Bazaar implementation gate

Do not mark Bazaar parity complete until all gates pass:

1. Add Poolakey `2.2.0` using an approved, content-filtered repository.
2. Initialize Poolakey with the Flutter RSA public key.
3. Connect and launch subscription purchase using the three product IDs:
   `vista_premium_monthly`, `vista_premium_3monthly`, `vista_premium_yearly`.
4. Send only `purchase_token`, `product_id`, and the actual package name to
   `POST /payment/bazaar-verify`.
5. Verify the backend accepts the Native package IDs and the release signing
   identity.
6. Test success, cancellation, connection failure, duplicate purchase, and
   server-verification failure on a Bazaar-installed signed APK.

The dependency was intentionally not left in the build because it is absent
from the local cache and the current repository list. The project must remain
buildable offline until the repository change is explicitly approved.

## Firebase configuration gate

The copied Flutter JSON must not be renamed to `google-services.json` for
Native. Firebase Console must first register each Native application ID and
provide matching configuration:

- beta: `ir.coffevista.vista_native`
- production: `ir.coffevista.vista_native.production`

After receiving matching files, place them in the appropriate variant source
sets, enable the Google Services plugin, and run a real-device token and
background notification test.

## Verification record

- Passed:
  `:app:compileProductionDebugKotlin`
  `:app:testProductionDebugUnitTest`
- Passed before this focused change:
  `:app:assembleProductionDebug --offline`
- Expected current offline behavior:
  Native build remains valid because Poolakey was not committed without its
  approved repository and cached artifact.

## Exit criteria

Flutter may be ignored for payment/notification maintenance only after:

- Bazaar gates 1–6 pass on a signed Bazaar-distributed build.
- Firebase gates pass for beta and production.
- Pixel comparison covers notification permission, channel settings, icon,
  foreground/background/terminated states, and tap destinations.
- Server settings are verified for every notification type and user preference.
- A release smoke test confirms premium entitlement is granted only after
  server verification.
