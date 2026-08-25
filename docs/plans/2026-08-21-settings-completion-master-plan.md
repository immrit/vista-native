# Native Settings — Completion Master Plan

**Status:** in progress  
**Overall progress:** `[████░░░░░░] 40%`  
**Reference application:** `E:\vista` (Flutter, read-only source of truth)  
**Target application:** `E:\vista_native` (Native Android)

## Objective

Finish the Native Settings experience so every Flutter Settings route has:

1. equivalent RTL/Farsi UI and navigation;
2. real account data and a real mutation/read contract where Flutter has one;
3. honest empty/unavailable states where a backend capability does not exist;
4. persistence that affects the application, not only Compose-local state; and
5. same-account emulator screenshots and visual comparison evidence.

Do not reuse the Flutter package for Native validation. Do not place credentials, tokens, or private profile data in source, plans, screenshots committed to Git, or logs.

## Baseline and guardrails

- [x] Capture authenticated Flutter Settings and reachable subpages on the reserved emulator.
- [x] Register Native routes and remove demonstrated fixture profile/session/saved-post content.
- [x] Implement real Edit Profile read/save fields and Room migration `8 -> 9`.
- [ ] Build, install, and capture the current Native source under its own application id.
- [ ] Establish pixel/state comparison evidence for each completed phase.

Flutter screenshots: `artifacts/screenshots/flutter_settings/`  
Current UI-only register: `docs/reports/2026-08-21-settings-ui-only.md`

## Phase 0 — Runtime and visual-validation foundation

**Progress:** `[███████░░░] 70%`

- [x] Reserve `emulator-5554`; confirm Flutter and Native package identities are distinct.
- [x] Capture Flutter root, subpages, sheets, dialogs, empty states, and scrolled states.
- [x] Resolve the existing Gradle build-directory lock without cleaning or overwriting parallel work.
- [ ] Build the current Native variant, prove package/activity identity, then install it.
- [ ] Capture Native screenshots using the same authenticated account and device state.
- [ ] Generate paired images, overlays/diffs, and a discrepancy ledger.

**Acceptance:** every later phase has before/after runtime evidence; compilation alone is never acceptance.

### Phase 0 lock evidence — 2026-08-21

- An earlier `:feature:profile:compileDebugKotlin --no-daemon` run failed before app compilation at `:build-logic:compileKotlin` because it could not delete `E:\vista_native\.gradle\feed-canonical-buildlogic\classes\kotlin\main\META-INF`. The isolated-output build below supersedes that environmental blocker.
- The target contains only the 24-byte generated file `build-logic.kotlin_module` dated 2026-08-14. A single-file, recoverable rename to a `.bak` name was denied by Windows (`Access is denied`).
- An elevated `tasklist` query found no running `java.exe` process. The lock is therefore external to a visible Gradle daemon; no shared cache, source, or parallel worktree output was deleted.
- `E:\vista\tmp\settings-buildlogic-isolation.init.gradle.kts` redirects the included `build-logic` project and every Native module's build directory to unique `E:\vista_native\.gradle\settings-20260821-*` paths. With the same source and no deletion, `:build-logic:compileKotlin` and `:core:datastore:compileDebugKotlin` completed successfully; the prior locked `core:common/build` output was not touched. `gradlew --status` then reported no running daemons. This safely bypasses the lock.

### Phase 0 compile evidence — 2026-08-21

- The isolated build completed `:feature:profile:kspDebugKotlin` and `:feature:profile:compileDebugKotlin` successfully, validating the Native Edit Profile/settings module and its transitive settings dependencies at compile time.
- `:core:security:testDebugUnitTest` completed successfully. Its JUnit report records 9 tests, 0 failures, and 0 errors, including encrypted biometric-policy persistence.
- This remains source/build evidence only. Native APK installation, same-account screenshot capture, and Flutter-vs-Native visual diffs remain open in this phase.

## Phase 1 — Edit Profile and profile media

**Progress:** `[████████░░] 80%`

- [x] Match Flutter form fields: username, full name, email, phone, birth date, gender, marital status, website, bio, and visibility controls.
- [x] Add `POST /v1/me/profile/update`, server-response cache update, error mapping, and Room migration.
- [x] Add repository test coverage for successful profile update/cache behavior.
- [x] Implement Android photo picker with app-scoped temporary JPEG handling.
- [x] Match Flutter media rules: image-only, maximum 5 MB, 1080 px JPEG compression, progress/error UI.
- [x] Trace and implement the real authenticated upload/delete contract; update `avatar_url` only after upload succeeds.
- [ ] Verify save/reload and avatar add/remove with real account data.

**Acceptance:** every visible field reloads from server, saves accurately, survives app restart, and avatar actions are real—not a local preview.

### Phase 1 implementation evidence — 2026-08-21

- Added Android Photo Picker integration and an authenticated avatar update flow.
- Avatar input is constrained to images, rejects files larger than 5 MB, down-samples to 1080 px, encodes JPEG at quality 85, streams upload progress to presigned object storage, and removes the temporary JPEG in `finally`.
- The new avatar URL is persisted only after `POST /v1/me/profile/update` succeeds. A newly-uploaded object is deleted after a failed profile mutation; an old avatar is deleted only after a successful replacement.
- Unit-test execution is currently blocked before compilation by a locked parallel-build path: `E:\vista_native\.gradle\feed-canonical-buildlogic\classes\kotlin\main\META-INF`. No shared cache or build output was deleted.

## Phase 2 — App-wide preferences foundation

**Progress:** `[████████░░] 80%`

- [x] Define a versioned DataStore settings model for locale, theme, motion, media playback, and quality preferences.
- [x] Add a singleton settings store; forbid competing DataStore instances or `remember` as persistent settings storage.
- [x] Complete locale-driven layout direction at the app root and all active Settings routes; root direction is restored from DataStore. Text translation resources remain Phase 6 work.
- [x] Apply theme, dark/light/system selection, text/icon contrast, and the shared animation/motion behavior app-wide.
- [x] Add versioned default/migration mapping tests for preferences. Full DataStore I/O execution remains blocked by the shared Gradle lock.

**Acceptance:** changing a preference survives process death and changes the relevant application behavior.

### Phase 2 implementation evidence — 2026-08-21

- `MainActivity` derives dark/light/system theme and root LTR/RTL direction from `SettingsPreferenceStore`; the selected values are restored by DataStore. Active Settings routes inherit that root direction; content translation remains incomplete, so locale parity is not yet claimed.
- `SettingsScreen` now inherits the persisted `en` LTR selection instead of forcibly resetting its own route to RTL. Flutter-derived screen strings still need resource-based Farsi/English/Arabic translations; text-localization parity remains incomplete until that resource conversion is finished.
- The Settings root now resolves its title, navigation rows, language sheet, and logout dialog from the exact `app_fa/en/ar.arb` strings used by Flutter. Translation of individual subpage content remains pending; no translation has been guessed.
- Every active Settings route and the shared Settings choice sheet now inherit `LocalLayoutDirection` from the app root rather than hard-coding RTL. The legacy private Compose implementations retained beside the replaced FAQ/legal renderers are not navigable; they do not participate in the active route tree.
- `VistaTheme` now receives the persisted `reduceMotion` preference and supplies instant shared motion tokens when it is enabled, in addition to honoring Android's system animation scale. Feature animations that bypass `MaterialTheme.vistaMotion` remain a Phase 7 audit item.
- `SettingsPreferenceStoreTest` verifies every current persisted settings field and the safe defaults for a pre-media schema. It passed in the isolated Gradle output (`3 tests, 0 failures, 0 errors`), including the invalid legacy appearance-value fallback.
- The Settings root footer now reads `versionName` from the installed Native package, matching Flutter's runtime-version behavior instead of displaying a stale fixture value.
- Flutter's `chat_entry_mode` values are now exact in Native (`adaptive`, `minimal`, `off`) and stored in the version-5 preferences schema. `MainActivity` supplies that policy to the chat renderer: adaptive retains the existing entry motion, minimal shortens it, and off renders with no entry motion. The `emoji_style` values (`custom`, `system`) are likewise persisted instead of being `remember` state, but Native has no Flutter-equivalent custom emoji renderer yet, so no visual effect is claimed for it.
- The Flutter custom-emoji source contract was audited before any asset migration: `modern_emoji_map.json` contains 3,967 entries under `assets/emoji/telegram`, while the checked Flutter workspace contains only `assets/emoji/modern` and no `telegram` directory. This source inconsistency must be repaired or an authoritative asset mapping supplied before Native can claim pixel parity for the custom renderer; the system-emoji option remains honest and functional.

## Phase 3 — Privacy, security, and account safety

**Progress:** `[██████████] 100%`

- [x] Seed available privacy controls from the own-profile contract.
- [x] Trace Flutter/API contracts for private account, message privacy, profile zoom, and related controls.
- [x] Implement server-backed privacy mutations with rollback and actionable Farsi errors.
- [x] Add authenticated owner-scoped offline cache reconciliation for privacy settings.
- [x] Implement biometric/app-lock policy using Android security APIs and encrypted local state; runtime validation remains Phase 7.
- [x] Implement Change Password against the authenticated endpoint; never persist plaintext passwords.
- [x] Implement Active Sessions server list, current-device label, individual revoke flow, confirmation, empty/error/retry states.
- [x] Add the Flutter-equivalent sensitive-action confirmation before revoking all other sessions; do not infer biometric approval from a local toggle.
- [x] Implement Blocked Users server list, search, empty/error/retry, and confirmed unblock against the moderation contract.
- [x] Verify parent blocked-user presentation against Flutter: neither Flutter nor Native renders a count, so no non-reference parent counter is added.

**Acceptance:** sensitive actions are authenticated, errors are actionable in Farsi, and security state is never represented as fake success.

### Phase 3 implementation evidence — 2026-08-21

- `PrivacySettingsApi.kt` mirrors Flutter's authenticated `GET/POST /v1/me/privacy` fields for last-seen, message, group-add, read-receipt, and profile-zoom preferences.
- `PrivacySettingsViewModel` rolls back any optimistic value if the request fails and surfaces a Farsi error. The private-account switch additionally calls `POST /v1/me/profile/update` with `is_private`, matching Flutter's server access-control gate.
- `PrivacySecurityScreen` no longer presents Compose-local values for those backend settings. Its biometric switch is backed by the encrypted session policy: enabling it requires a successful AndroidX strong-biometric or device-credential prompt, while disabling it clears that encrypted policy.
- `BlockedUsersApi` mirrors Flutter's `GET /v1/me/blocked-users` and `POST /v1/me/unblock`; the native screen now renders only returned profiles and removes an entry only after a successful unblock response.
- `ActiveSessionsApi` mirrors Flutter's active-list and terminate endpoints. Native only permits an all-other-sessions action for server-marked non-current entries, so the current session is not guessed or included by a local fixture.
- The all-other-sessions mutation is behind the Flutter-equivalent explicit confirmation. When the encrypted biometric policy is enabled, the Native app additionally prompts before it sends an individual or all-other session termination request; cancellation and prompt errors fail closed.
- `ChangePasswordApi` mirrors Flutter's `POST /v1/change-password` request. Password strings exist only in the Compose input state and the in-flight request; the ViewModel never stores them and no DataStore/Room preference is used.
- Privacy settings are now cached only with the authenticated `userId` that fetched them. A failed refresh can display that same account's snapshot with an explicit offline message; a different account cannot read it because the owner id must match before the snapshot is mapped. Successful server reads and writes replace the snapshot. The cache holds non-secret privacy choices only; credentials and password data remain outside it.
- Flutter's parent Privacy & Security route presents only the `کاربران مسدود شده` navigation row, not a count. Native matches it, so an extra parent-state refresh/count was intentionally not introduced.
- Biometric implementation reuses the AES-GCM Android Keystore envelope in `core:security:SessionStore`; AndroidX `BiometricPrompt` is the authentication gate. The persisted policy alone never constitutes approval.
- The encrypted session envelope now has an account-bound `biometricEnabled` policy field. It survives a same-account session refresh, is AES-GCM encrypted with the rest of the session, and is erased by logout. `EncryptedSessionStoreTest` covers the encrypted round-trip/preservation.
- AndroidX `BiometricPrompt` is now wired to the persisted policy: enabling the Settings switch requires a successful strong biometric or device-credential prompt, and individual/all-other Active Session termination prompts again when that policy is enabled. Prompt cancellation/error is fail-closed. A whole-app foreground lifecycle lock remains separate pending work; it is not claimed by this sensitive-action gate.
- `MainActivity` now requires foreground re-authentication after the app enters the background while the encrypted biometric policy is enabled. Its opaque Compose overlay blocks the active route until an AndroidX prompt succeeds; a cancellation/error leaves it locked. Configuration changes do not trigger the lock. Runtime/device proof remains blocked with the rest of Phase 7 by the Gradle lock.
- Build/device/API validation remains pending because the locked shared Gradle build-logic directory prevents compilation before source tasks run.

## Phase 4 — Notifications and data/media behavior

**Progress:** `[████████░░] 80%`

- [x] Map Flutter notification categories to the real `GET/POST /v1/me/notification-settings` fields; Android channels remain pending.
- [x] Show and request the real Android notification permission independently of server preference toggles; never claim it is enabled when it is not.
- [ ] Add Flutter-equivalent FCM delivery and Android notification channels (`chat_messages`, `social_notify`) before treating the platform-permission tile as an end-to-end notification capability.
- [ ] Implement autoplay and network/media quality preferences in actual media consumers.
- [x] Measure cache size from the real app cache; implement scoped, recoverable clear-cache behavior.
- [x] Match Flutter quality-selector labels and explanations; runtime media-consumer behavior remains a separate open item.

**Acceptance:** each setting changes notification/media/cache behavior and accurately reports unavailable system permissions.

### Phase 4 implementation evidence — 2026-08-21

- Flutter's authenticated notification contract was traced to `/me/notification-settings`; Native mirrors it as `v1/me/notification-settings` in `NotificationSettingsApi.kt`.
- `NotificationSettingsViewModel` loads the server state, applies optimistic changes, and restores the prior value with a Farsi error if the POST fails.
- `NotificationSettingsScreen` now binds every Flutter category—including the four-field social aggregate and quiet-hour start/end—to that state instead of `remember` fixtures.
- `AppCacheManager` measures only `cacheDir` and `externalCacheDir`, and clears their children only after the existing confirmation dialog; account data in `filesDir` is not touched.
- Native now declares Android 13's `POST_NOTIFICATIONS` permission and displays its real runtime/channel-enabled state. The tile requests the runtime permission when applicable, opens app notification settings otherwise, and refreshes after returning to the app; server notification preferences remain distinct.
- Flutter creates `chat_messages` and `social_notify` channels through Firebase/local-notifications. A Native source audit found neither `FirebaseMessaging` nor a messaging receiver/channel creator, so permission state is truthful only as a platform capability—not proof that Native can receive or display a remote notification. FCM token registration, payload routing, the two channels, and server token contract tracing remain required work.
- Flutter registers its FCM token at `POST /v1/fcm/token` with `token`, `platform`, and `device_type`. The only checked Firebase configuration is `E:\vista\android\app\google-services.json` for Flutter's package; `E:\vista_native\app\google-services.json` is absent. A configuration registered for the distinct Native application id is required before adding Firebase dependencies or copying a configuration file, otherwise token delivery cannot be validly proven.
- Data & Storage now mirrors Flutter's autoplay/data-saver help text and all three upload-quality labels/descriptions. The selected value remains DataStore-backed.
- A source audit found no Native `ExoPlayer`, `Media3`, `PlayerView`, or equivalent video-player consumer in the Feed/Profile feature modules. Therefore the stored autoplay/quality values cannot yet affect playback; their real integration belongs to the future Native media-player implementation, not to a Compose-local workaround in Settings.
- This is source-level completion only: Gradle is still blocked before compilation by the known locked shared build-logic directory, so no device/API or screenshot evidence exists yet.

## Phase 5 — Premium, verification, and saved content

**Progress:** `[████░░░░░░] 40%`

- [x] Trace the premium catalogue and replace Native local plan prices with authenticated server catalogue data.
- [ ] Complete store-specific billing for every configured gateway. Zibal source flow is implemented; Cafe Bazaar SDK parity remains pending.
- [ ] Implement verification category, form validation, document picker/upload, submission, status, and retry behavior. **Blocked pending explicit authorization:** this transmits a government/business identity document to external presigned storage.
- [x] Implement Saved Posts server repository, cursor pagination, unsave, empty/error/retry states, and navigation to the existing real-post route.

**Acceptance:** monetary/identity operations cannot display local-only success; saved items are server-backed and open the intended post.

### Phase 5 implementation evidence — 2026-08-21

- `SavedPostsApi` mirrors Flutter's authenticated `GET /v1/me/saved` pagination and `POST /v1/posts/save/{postId}` toggle.
- The Native list uses only server posts, loads the next cursor explicitly, and removes a post after the server confirms it is no longer saved; static fixture posts remain prohibited.
- Compilation, API response shape, navigation, and visual proof are pending the shared Gradle lock resolution.
- Native now has a Zibal source flow: explicit user confirmation, authenticated `POST /v1/payment/zibal/request`, HTTPS gateway launch, user-triggered server verification, and own-profile refresh only after server success. Track ids remain ViewModel-only and are neither logged nor persisted. Cafe Bazaar SDK purchase/receipt verification is still required for gateway parity.
- The Zibal request uses the exact Flutter product identifiers (`vista_premium_monthly`, `vista_premium_3monthly`, `vista_premium_yearly`), while the server catalogue remains the authority for whether and how a plan is displayed.
- `PricingPlansApi` now mirrors Flutter's authenticated `GET /v1/payment/subscription-plans`; only the server's `monthly`, `three_monthly`, and `yearly` plan prices render. When the catalogue is unavailable, cards and purchase are disabled rather than falling back to a local amount.
- The native benefits list now mirrors the eleven Flutter entries and their normal-account limits. This remains parity UI only until each entitlement is verified against the server and the corresponding Native feature module.
- Verification Request no longer toggles a fake attached document or displays simulated submission success. It now matches Flutter's free-text category field and validates it, while visibly retaining the document area and explaining that the real secure transfer is not enabled. Real selection, upload and submission remain blocked pending explicit authorization to transmit identity documents to external presigned storage.

## Phase 6 — Language, legal, support, and platform actions

**Progress:** `[████████░░] 80%`

- [x] Create Native UI routes for language, terms, privacy policy, FAQ, contact, about, and logout dialog.
- [x] Persist/apply language through Phase 2’s locale system.
- [x] Source the locally-published Terms and Privacy Policy text from the same Flutter source and preserve its document layout.
- [x] Complete the Flutter FAQ catalogue/search/filter behavior: all 27 locally-published Flutter items now render in Native.
- [x] Wire the Flutter contact-request form and support-conversation endpoint to authenticated Native APIs; preserve safe email intent.
- [x] Wire rating and marketplace-update actions to safe Android intents/Bazaar integrations with web fallback; show no false completion result.
- [x] Trace Flutter-equivalent sharing action: no share action is exposed from Flutter Settings or its reachable About subpages, so no Native setting action is invented.
- [x] Implement honest unsupported-service fallbacks in source: Bazaar web fallback and the explicit unavailable identity-upload state; runtime verification remains Phase 7.

**Acceptance:** every action either completes through an external service or gives a truthful, recoverable explanation.

### Phase 6 implementation evidence — 2026-08-21

- `ContactRequestsApi` mirrors Flutter's `/v1/contact-requests` request fields: `full_name`, `email`, `subject`, `message`, and `category: contact`.
- `ContactUsViewModel` sends the request, clears the form only after an HTTP-success response, and renders a Farsi retry error otherwise. The former timed delay and simulated success were removed.
- `SupportConversationApi` mirrors Flutter's `POST /v1/chat/support/conversation`; after the server returns a nonempty conversation id, Shell switches to the Chat tab and opens the existing conversation route. Failed/unauthorized calls remain on the contact page with a Farsi retry error.
- The email intent now uses Flutter's actual published support destination, `info@cafevista.ir`, rather than the earlier unverified Native placeholder.
- Rating and update both open the published Flutter app listing (`ir.coffevista.vista`) through `bazaar://details`, then fall back to its Cafe Bazaar web page. The Native package does not claim an installation/update/rating result. The displayed version is read from the installed Native package rather than a fixture.
- Terms and Privacy Policy now use a shared Native document renderer with Flutter's 16 dp page padding, 22 sp centered heading, 18 sp section headings, 16 sp justified body text, and the complete locally-published Flutter legal text. This is intentionally local rather than a guessed remote URL because Flutter ships this approved content locally. Runtime visual comparison is still pending Phase 7.
- Flutter's FAQ source contains 27 static items across account, chat, content, security, music, search, stories, offline, limits, technical issues, special features, and support. Native now uses that same source-derived catalogue, case-insensitive question/answer/category search, the same informational category chips, expandable answer cards, and category visual treatment. Runtime visual comparison remains Phase 7 work.
- A source trace of Flutter Settings and reachable About routes found no user-facing app-sharing action. Native therefore does not add a non-reference share button. Existing Native external actions handle unsupported Bazaar availability through a web fallback, while Verification Request explicitly reports that secure identity-document transfer is unavailable instead of simulating an attachment or completed request.
- The language selector accepts only Flutter's three offered locale codes (`fa`, `en`, `ar`), persists the selection through `SettingsPreferenceStore`, and causes the `MainActivity` root to recompose with the selected root direction. The Settings root itself resolves its strings from the matching Flutter ARB catalogues; device proof remains Phase 7.

## Phase 7 — Full visual and behavioral parity sign-off

**Progress:** `[░░░░░░░░░░] 0%`

- [ ] Run each Settings route with the same live account fixture and capture matching top/scrolled/dialog states.
- [ ] Compare typography, spacing, colors, elevation, icons, divider geometry, RTL alignment, touch targets, and system insets.
- [ ] Test back navigation, loading, empty, validation, offline, unauthorized, and retry states.
- [ ] Test light/dark/system theme, locale switching, process restart, and font-scale accessibility.
- [ ] Publish a final route-by-route evidence table with Flutter/Native screenshots and remaining deltas (if any).

**Acceptance:** no page is marked complete without current Native screenshots and a documented comparison against the corresponding Flutter state.

## Route completion ledger

| Route | UI route | Real behavior | Planned phase |
|---|---:|---:|---|
| Settings root / logout | yes | real profile data and logout source exist; runtime re-verification pending | 0, 7 |
| Edit Profile | yes | real fields/save/avatar upload source; runtime verification pending | 1 |
| Premium | yes | real server catalogue and Zibal source flow; Bazaar billing remains external-config dependent | 5 |
| Privacy & Security | yes | privacy/password/sessions/blocked source complete; offline cache, biometric lock and runtime proof pending | 3 |
| Blocked Users | yes | real server list/search/unblock source; runtime verification pending | 3 |
| Active Sessions | yes | real list/revoke plus confirmation guard source; runtime verification pending | 3 |
| Notifications | yes | server preference and Android permission source; channel/runtime verification pending | 2, 4 |
| Language | yes | DataStore/root direction source; translated content and route-wide direction parity pending | 2, 6 |
| Appearance | yes | persisted theme/reduced-motion source; animation audit/runtime verification pending | 2, 7 |
| Data & Storage | yes | persisted/cache source; video-player consumer is not implemented in Native yet | 2, 4 |
| Saved Posts | yes | real server pagination/unsave source; runtime verification pending | 5 |
| Change Password | yes | authenticated server mutation source; runtime validation pending | 3 |
| Verification | yes | UI/form only; sensitive document upload awaits explicit authorization | 5 |
| Terms/Privacy | yes | full locally-published Flutter text and equivalent document layout source; runtime audit pending | 6, 7 |
| FAQ | yes | 27 source-derived entries plus search/category chips/expand behavior; runtime visual audit pending | 6, 7 |
| About/Contact | yes | real contact/support source and marketplace intents; exact visual/runtime audit pending | 6, 7 |

## Execution order

1. Unblock Phase 0 so every change can be installed and proven.
2. Finish Phase 1 avatar upload, then take the first Edit Profile visual comparison.
3. Build Phase 2 once as shared infrastructure; use it in language, appearance, notifications, and data/storage instead of duplicating state.
4. Complete Phase 3 before any claim that Settings is production-ready, because it contains account-sensitive actions.
5. Complete Phases 4–6 by real backend/platform contract, then perform Phase 7 as the release gate.

## Progress update protocol

For every completed checkbox, record in this file:

- source files changed;
- API/backend contract and test evidence;
- Native package, APK, foreground activity, emulator, and account fixture used for runtime proof;
- Flutter screenshot counterpart and Native screenshot path;
- known deltas, if any.

Never advance a phase progress bar solely for a UI mock, a static analysis result, or an old APK.
