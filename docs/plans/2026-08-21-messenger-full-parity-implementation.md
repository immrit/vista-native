# Native Messenger Full Parity Implementation

**Type:** feature | ui | perf
**Status:** in-progress
**Progress:** `[████████░░] 80%`

## Problem / Goal

Complete the Native messenger against the Flutter reference using authenticated real data, while upgrading voice recording to a gesture-first, fluid interaction model. Preserve existing unrelated work and never store credentials, tokens, or message plaintext in diagnostics.

## Root cause / Design

The chat feature is offline-first: Compose routes -> view models -> `OfflineFirstChatRepository` -> Room/REST/realtime. IME must affect only the composer. Voice capture needs one lifecycle-aware state machine so gesture callbacks cannot race asynchronous recorder startup.

## Phases

### Phase 1 — Investigate
- [x] Inventory existing chat work, Flutter reference, and dirty-tree ownership.
- [x] Recheck emulator DNS/proxy/session and capture the available real fixture in both apps.
- [x] Record the Flutter-to-Native gap matrix and source/runtime evidence.

### Phase 2 — Implement
- [x] Correct the proven refresh/older-page cursor race at repository boundaries.
- [x] Keep the existing composer-only IME architecture; final-device validation remains required.
- [x] Replace voice capture interaction with a session-aware gesture/lifecycle state machine and live waveform.
- [x] Delete successful recorder-cache uploads regardless of whether the recorder supplied an absolute path or file URI.
- [x] Move voice playback preparation off the UI thread and apply audio focus while playing.
- [x] Replace the private-chat header detail bottom sheet with a full-screen Flutter-aligned detail route.
- [ ] Close audited action/UI gaps without introducing external-product identifiers.

### Phase 3 — Verify
- [x] Run focused unit tests and `:feature:chat` build gates.
- [x] Run the full `:feature:chat` device suite (32/32) after the recorder/player updates.
- [x] Rebuild, manifest-verify, and reinstall the APK after the latest recorder/player/detail changes.
- [ ] Validate real REST/realtime, temporary send/file/voice workflows, and cleanup.
- [ ] Capture paired RTL/dark screenshots and document quantitative differences.

## Decisions & Notes

- Flutter is read-only visual and behavioral truth except the user-directed enhanced voice interaction.
- All real test messages, attachments, and recordings are temporary and must be deleted after verification.
- Existing uncommitted work is preserved; this plan only owns newly created messenger files and targeted chat changes.
- Both Flutter and Native have previously rendered the same authenticated `raha.81` cached fixture. The current emulator has no configured HTTP proxy but its DNS resolver is unavailable; this is an environment blocker for live REST/realtime/send verification, not evidence of a client pagination/UI regression.
- A previously built Production Debug APK (versionCode 4050) was package-verified, installed, and opened on the emulator. `raha.81` rendered real cached history and its composer remained visible in an earlier capture at `E:\vista\artifacts\native-raha-ime-build4050.png`; it predates the final detail/IME verification gate.
- The emulator restarted during validation and subsequently had no DNS resolver (`ping api.coffevista.ir` returned unknown host). Realtime's displayed reconnect state is therefore environment evidence, not an unproven client URL defect. REST/realtime send validation remains pending DNS recovery.
- `:feature:chat:testDebugUnitTest` passed with 80 tests after adding recorder cache-path coverage. The full Compose/instrumentation suite then passed 32/32 on `emulator-5554`; it covers group details, message actions, IME/composer fixtures, voice playback and the player seek control.
- Voice playback no longer calls `MediaPlayer.prepare()` on the UI thread. It prepares asynchronously, publishes an accessible loading state, requests/abandons audio focus on play/pause/completion, and retains seek/speed behavior. The focused playback instrumentation test passes on `emulator-5554`.
- Private-chat headers are now directly actionable. The resulting full-screen detail view has a 280dp profile hero, RTL identity/status section, the reference three-action row (share/mute/block), and four media tabs with a three-column media grid/empty states. Its dedicated instrumentation test passed on `emulator-5554`.
- The detail view now obtains peer username/avatar/bio through the existing authenticated profile endpoint only after the header is opened, keeps that projection only in `MessagesUiState`, and falls back to the cached conversation identity if the request fails. The complete `:feature:chat:connectedDebugAndroidTest` suite passed 32/32 after this wiring; its direct detail test also asserts the lazy-load callback.
- The current full-app APK rebuild progressed past the prior Pricing/Profile imports but is now blocked by a separate untracked file: `feature/profile/src/main/java/ir/coffevista/vista_native/features/profile/settings/SettingsScreen.kt` has Kotlin parser errors beginning at line 128. It is outside messenger scope and has not been modified. A final APK containing the latest chat work therefore remains pending its owner resolving that independent source.
- On 2026-08-21, both installed applications were captured on `emulator-5554` at `1080x2280` using the same authenticated `raha.81` conversation: `E:\vista\artifacts\flutter-conversations-before-parity.png`, `flutter-raha-before-parity.png`, `flutter-raha-ime-before-parity.png`, `native-conversations-before-parity.png`, `native-raha-before-parity.png`, and `native-raha-ime-before-parity.png`. The old installed Native APK keeps its conversation list close to Flutter's hierarchy but differs in header spacing/typography and has a serious IME regression: with the keyboard open, the composer is no longer visible while Flutter keeps it directly above the IME. This APK predates the current chat changes, so the observation is a final-APK reproduction gate rather than proof against the pending source.
- The final Production Debug APK was rebuilt successfully after isolated mechanical fixes in unrelated profile/app source, manifest-verified as `ir.coffevista.vista_native.production` with launch activity `ir.coffevista.vista_native.MainActivity`, and installed on `emulator-5554`. The native runtime now opens the partner detail from the `raha.81` chat header. It matches the actual Flutter `VistaChatProfileScreen` composition: 340dp blurred avatar hero, centred circular avatar, directional back/menu controls, four action cards, and seven scrollable tabs. Same-fixture comparison screenshots: `E:\vista\artifacts\flutter-final-raha-detail.png` and `E:\vista\artifacts\native-final-raha-detail-tabs-loaded.png`.
- The final native IME screenshot `E:\vista\artifacts\native-final-raha-ime.png` proves the composer stays immediately above the keyboard. This closes the prior old-APK composer disappearance reproduction; only the broader per-action/realtime acceptance sweep remains.
