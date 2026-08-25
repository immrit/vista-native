# Native Messenger Gap Audit

**Status:** in-progress  
**Fixture:** authenticated account and one shared real conversation; no credentials recorded.

| Area | Flutter reference | Native baseline | Status / evidence |
| --- | --- | --- | --- |
| Conversation list | Search, menus, archives, selection, requests, notes | Route and view model present | Runtime action sweep pending |
| Message loading | Cache + REST cursor + realtime | Room + cursor repository present; per-conversation fetch mutex added | Newly installed Production Debug APK rendered real cached `raha.81` history; REST/realtime retest blocked by emulator DNS |
| IME | Stable message viewport and composer directly above keyboard | Composer-only inset work present | Final APK capture confirms composer is directly above IME: `E:\vista\artifacts\native-final-raha-ime.png`; old baseline regression is closed |
| Composer | Text, reply/edit, emoji/GIF, attachment | Equivalent controls present | Per-action parity pending |
| Message actions | Reply, reaction, forward, edit, delete, search | Action sheet and routes present | Per-action parity pending |
| Voice | Basic recording UI | Session-aware hold/cancel/lock/send policy, real meter/audio focus, non-blocking player setup and playback seek control added | 80 unit tests, 32/32 Compose/device tests, and focused player test pass; cache cleanup accepts both recorder path forms; final send/device verification pending DNS and final APK |
| Private chat detail | `VistaChatProfileScreen`: 340dp blurred hero, centred avatar, four quick actions, seven scrollable tabs | Header is directly tappable; full-screen route uses real profile projection in memory only | Final APK runtime parity captured: `flutter-final-raha-detail.png` and `native-final-raha-detail-tabs-loaded.png`; action-specific navigation sweep remains |
| Profile/group | Details and management flows | Group routes/services present | Runtime action sweep pending |

## Acceptance evidence

- Same-device paired screenshots at 1080x2280 for list, detail, IME, actions, attachment, and voice states.
- REST and authenticated realtime validated before declaring data behavior complete.
- Temporary test content removed, and no secrets/plaintext copied into this document.

## 2026-08-21 runtime evidence

- Emulator proxy is unset. The emulator has network connectivity but currently no usable DNS resolver (`api.coffevista.ir` returns `unknown host`), blocking authenticated REST/realtime/send validation in both applications.
- Flutter and Native were captured with the same authenticated `raha.81` fixture at `1080x2280`. Final detail comparison is `E:\vista\artifacts\flutter-final-raha-detail.png` against `E:\vista\artifacts\native-final-raha-detail-tabs-loaded.png`.
- `raha.81` rendered real cached message history in both applications. Final APK IME evidence is `E:\vista\artifacts\native-final-raha-ime.png`: the native composer remains above the keyboard, matching the required viewport behavior.
- `:feature:chat:testDebugUnitTest` passed 80 tests and `:feature:chat:connectedDebugAndroidTest` passed 32/32 on `emulator-5554`. The latest full app `:app:assembleProductionDebug` now succeeds and the manifest-verified APK was installed for the final runtime captures.
