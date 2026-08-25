# Native Settings — UI-only Completion Register

**Status:** implementation in progress; not a runtime or visual-parity sign-off.

## Verified source and implemented route surface

The Flutter source of truth is `E:\vista\lib\features\settings\screens\Settings.dart` plus its subpages. The Native route surface is connected through `feature/shell/.../ShellRoutes.kt` and `app/.../AppNavGraph.kt`, and all of the following screens have Compose UI routes:

- Settings root, profile card, premium entry, language sheet, logout dialog.
- Edit profile, pricing, privacy/security, blocked users, active sessions.
- Notifications, appearance, data/storage, saved posts, change password, verification request.
- Terms, about, about slideshow, contact us, privacy policy, and FAQ.

The root profile card now uses the server-backed `OwnProfileEntity.username`; it no longer renders a fixed email. The `حساب کاربری` row now follows Flutter and opens Edit Profile. Privacy initial values consume the existing own-profile cache (`isPrivate`, `messagePrivacy`, and `allowProfileZoom`). Edit Profile now reads and saves its actual identity, contact, website, bio, demographic, and profile-detail visibility values through `POST /v1/me/profile/update`, and caches them in Room.

## UI-only / next-phase integration work

| Area | Current Native behavior | Required integration phase |
|---|---|---|
| Language | Choice sheet changes `remember` state only. | Persist and apply locale through the app-wide locale/data-store layer. |
| Edit profile | Real profile read/save and cache migration are implemented. The avatar control is still UI-only. | Native image picker, 5 MB validation/compression, authenticated media upload/delete, then `avatar_url` mutation. |
| Premium | Plans and purchase feedback are local UI. | Billing/catalog API and entitlement refresh. |
| Privacy/security | Switches and choice sheets are local Compose state. | Privacy settings repository, biometric policy, and backend mutation contracts. |
| Notifications | Switch UI has no real notification-preference persistence. | Notification settings repository and FCM/system-notification synchronization. |
| Appearance | Theme and motion selections are screen-local. | App-wide theme/performance preferences in DataStore. |
| Data/storage | Autoplay, quality, cache size, and clear-cache result are presentational. | Media preferences and actual cache accounting/clearing. |
| Saved posts | Uses an honest empty state; no fixture posts are shown. | Saved-post repository, pagination, and live post navigation. |
| Change password | Validates locally then simulates completion. | Authenticated password-change endpoint and error mapping. |
| Verification | Category/form/upload state is local. | Verification submission, document upload, and request-status API. |
| Active sessions | Shows only the current-device shell; no fixture device/session data is shown. | Session list/revoke endpoints plus sensitive-action guard. |
| Blocked users | Empty-state UI and local search only. | Moderation repository, unblock mutation, and count refresh. |
| About/contact/update | Informational UI is present; rating/update actions are not wired to platform services. | Bazaar/in-app support/update integrations. |

## Evidence limitations

- Flutter-first live screenshots are now captured on `emulator-5554` under `artifacts/screenshots/flutter_settings`. Native screenshots and same-fixture visual metrics remain pending until the current source can be built and installed.
- Earlier build attempts encountered locked/permission-denied files under `E:\vista_native\app\build` and `feature\profile\build`. The workspace was deliberately not cleaned because it contains concurrent work. The current build attempt is additionally awaiting explicit external approval for Gradle network access.
