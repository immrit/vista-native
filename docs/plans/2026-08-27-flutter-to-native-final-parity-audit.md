# Vista Flutter → Native Final Parity Audit and Cutover Plan

**Type:** refactor / migration / parity  
**Status:** in-progress  
**Last updated:** 2026-08-27  
**Progress:** `[█████░░░░░] 50%`  
**Scope:** `E:\vista` (Flutter reference) → `E:\vista_native` (Kotlin/Compose Native target)

## Problem / Goal

تعیین درصد واقعی انتقال Vista از Flutter به Native، با تفکیک «کد موجود»، «قابلیت اثبات‌شده» و «آمادهٔ جایگزینی». هدف نهایی این است که پس از بسته‌شدن تمام گیت‌های این سند، Flutter فقط به‌صورت archive/read-only نگه‌داری شود و توسعهٔ جاری فقط روی Native انجام شود.

این سند جایگزین معنایی برای درصدهای قبلی نیست؛ درصدها باید طبق شواهد همین checkout و APK نصب‌شده محاسبه شوند.

## Executive decision

### درصد واقعی در تاریخ 2026-08-27

| معیار | درصد | تفسیر |
|---|---:|---|
| پیاده‌سازی Native در سورس، بدون تضمین parity | **59%** | بخش عمدهٔ foundation، shell، feed، profile، search، services، settings، stories و chat در checkout فعلی وجود دارد؛ وجود کد به‌تنهایی تکمیل انتقال نیست. |
| قابلیت‌های دارای evidence قابل اتکا | **23%** | بخشی از unit/connected tests و چند runtime capture موجود است؛ ماتریس کامل happy/error/offline/restart و مقایسهٔ هم‌state هنوز بسته نشده است. |
| parity پیکسلی کامل و قابل انتشار | **0%** | هیچ ماتریس کامل screenshot با fixture یکسان، state یکسان، diff طبقه‌بندی‌شده و sign-off برای همهٔ P0/P1ها وجود ندارد. |
| آمادگی برای کنار گذاشتن Flutter | **0%** | inbound share، بعضی recovery branchها، media/music، migration/update identity، runtime proof و release gate هنوز باز هستند. |

**عدد رسمی برای گزارش مدیریت:** انتقال از نظر ساخت کد حدود **59%** پیش رفته، اما انتقالِ اثبات‌شده فقط **23%** است و از نظر cutover هنوز **0%** محسوب می‌شود.

این درصدها بر اساس وزن ریسک و قابلیت‌های محصول محاسبه شده‌اند، نه تعداد فایل‌ها. شمردن فایل‌های Dart و Kotlin یا تعداد routeها به‌تنهایی معیار انتقال نیست.

### مدل امتیازدهی

| حوزه | وزن | اجرای سورس | evidence فعلی |
|---|---:|---:|---:|
| foundation، startup، auth، session، navigation | 20% | 70% | 40% |
| feed، posts، comments، profile، search، services، settings، stories | 35% | 65% | 20% |
| chat، realtime، E2EE، media داخل chat | 25% | 75% | 35% |
| media platform، music، share receiver، notifications، permissions | 10% | 35% | 15% |
| migration، performance، accessibility، signing، release | 10% | 20% | 5% |
| **نتیجهٔ وزنی** | **100%** | **59%** | **23%** |

هر حوزه فقط زمانی «کامل» است که هم implementation، هم behavior، هم error/offline/restart، هم visual evidence و هم security/release contract آن بسته شده باشد.

## Evidence baseline

- Flutter reference: `E:\vista`
- Native target: `E:\vista_native`
- Device used in this audit: `emulator-5556`, API 33، `1080x2280`، density `440`، ABI `x86_64`
- Flutter installed artifact: version `2.6.2` / versionCode `4049`
- Native installed artifact: version `2.6.3` / versionCode `4050`
- Native APK: `app/build/outputs/apk/production/debug/app-production-debug.apk`
- Native APK SHA-256: `7569E457F6F50BF98742858ABBF6C2F2CB24568CAE6C01BC5AFAF6123E699183`
- Current captures: `artifacts/native-current-startup.png`, `artifacts/native-current-next.png`, `artifacts/flutter-current-startup.png`, `artifacts/flutter-current-after10s.png`
- Existing ledgers:
  - `docs/audits/2026-08-05-p01-parity-ledger.csv`
  - `docs/audits/2026-08-24-current-source-gap-ledger.md`
  - `docs/reports/2026-08-24-vista-native-live-parity-audit.md`
  - `docs/audits/2026-08-21-messenger-full-gap-audit.md`

## Audit findings

### What is genuinely present in the current Native checkout

- `app/src/main/java/ir/coffevista/vista_native/navigation/AppNavGraph.kt:313` wires real Native content into the authenticated shell.
- Current wiring includes feed, story player/editor, add-post/video trim, search/QR entry, own/other profile, profile posts, settings subpages, services/nearby/contacts/groups/web/game, conversation list, new message and chat detail.
- `feature/auth/src/main/java/ir/coffevista/vista_native/features/auth/AuthViewModel.kt:20` contains identifier, password, OTP and password-setup state transitions.
- Chat has substantial Room/repository/realtime/E2EE implementation and targeted tests; this is not a missing feature anymore, but it is not yet a fully proven product parity result.
- Notification manager, Firebase service and token registration exist, but runtime payload/action coverage is incomplete.

### What remains a confirmed cutover blocker

1. **No full same-state pixel matrix.** The fresh clean-install captures are not a valid pair: Native reached onboarding while Flutter remained on a splash/transition frame. The images can demonstrate that the capture protocol is not normalized; they cannot be counted as a parity pass.
2. **Flutter and Native application identities differ.** Flutter uses `ir.coffevista.vista`; Native production uses `ir.coffevista.vista_native.production`. A direct in-place upgrade, existing-data handoff and store/update replacement are not proven.
3. **Inbound Android share is not mirrored.** Flutter declares `SEND`/`SEND_MULTIPLE` for text, image, video and generic files. Native manifest currently exposes MAIN, VIEW and FCM only.
4. **Manifest capability parity is incomplete.** Flutter declares camera, coarse location, contacts, media/storage, audio settings, foreground media playback, Bazaar payment and install/update permissions. Native manifest currently declares only internet, network state, notifications and record audio.
5. **Password recovery is only a simplified branch.** Native has a forgot-password flag and OTP path, but Flutter has distinct recovery-option, recovery-code, confirmation and password-reset contracts that require end-to-end verification.
6. **Music/background audio is not a proven Native feature.** Flutter has `MusicDownloadManager`, shared audio service and media controls; Native has post/chat media pieces but no equivalent end-to-end music lifecycle evidence.
7. **Notification parity is incomplete.** Native service/channel/token code exists, but notification list behavior, all Flutter action types, killed-state routing, read state and destination coverage are not fully proven.
8. **Controlled fallback destinations still exist.** `feature/shell/src/main/java/ir/coffevista/vista_native/features/shell/VistaShell.kt:397` uses a controlled detail fallback for services detail; similar fallbacks exist for optional content paths. A fallback is acceptable during development, not as a closed product journey.
9. **Visual differences remain in core surfaces.** Existing feed/profile audit evidence records mismatched app bars, navigation island, skeletons, post card geometry, media sizing, action rows, profile header/actions/tabs and detail layout.
10. **Runtime evidence is environment-sensitive.** Previous chat evidence was constrained by emulator DNS/API availability; cached rendering is useful, but it does not prove REST pagination, authenticated realtime, send, retry or server reconciliation.
11. **Migration of local data is not complete.** Native Room/DataStore migrations exist, but Flutter Isar/session/E2EE/draft/download/pending data compatibility and rollback behavior are not proven as a signed upgrade scenario.
12. **The current worktree is dirty.** Existing user changes, untracked build/evidence folders and WIP files mean a release claim cannot be based only on “current source contains the code”; the exact reference commit and exact Native release candidate must be frozen first.

## Why the existing 80% is misleading

- The `80%` in `docs/plans/2026-08-26-flutter-to-native-complete-parity-audit.md:6` is explicitly plan/checklist execution progress, not migration completion.
- Its own acceptance policy says implementation, unit tests and old screenshots do not establish parity.
- Several “immediate queue” items are already completed in the current checkout, while the phase checkboxes remain open; this makes the percentage stale in both directions.
- The older ledger contains 92 journey rows, 70 marked `Missing — transferred`, and 294 visual rows marked capture-pending. Those values are a historical inventory, not a current verdict, but they demonstrate that a source inventory cannot be converted directly into an 80% migration claim.
- The current source has advanced beyond portions of the historical ledger, especially stories, settings, profile, services and chat wiring. The ledger must be re-baselined instead of selectively checking old rows.
- Conversely, the current source contains fallbacks, unproven runtime behavior and release-contract gaps that a source-only percentage hides.

## Non-negotiable acceptance policy

1. Every Flutter user journey has one Native destination, one source trace and one evidence record.
2. Every P0/P1 journey passes happy, validation/error, offline, retry, process-restart and back-stack cases where applicable.
3. Pixel comparison uses the same device profile, locale, font scale, theme, animation scale, fixture, account state and exact semantic state.
4. A screenshot pair is invalid if one app is on splash/loading and the other is on onboarding/content.
5. Every difference is classified as product mismatch, renderer residual, environment artifact or intentional Native improvement.
6. No token, credential, E2EE plaintext, recovery secret or production URL ticket is written to logs, fixtures or reports.
7. No checkbox is checked from source inspection alone.
8. Flutter remains read-only reference until the final cutover gate is signed.

## Phases

### Phase 0 — Freeze provenance and rebuild the baseline

- [x] Record Flutter reference commit, Native reference commit and dirty-worktree ownership.
- [ ] Build both APKs from the recorded commits; record package, activity, version, versionCode, signing certificate and SHA-256. Native production debug assembled successfully; Flutter artifact remains prebuilt because `E:\vista` is read-only in this workspace.
- [ ] Decide the production identity strategy: same package/signature for upgrade, or an explicit migration/install strategy.
- [x] Normalize emulator/device profile: API 33, x86_64, 1080x2280, density 440 was recorded; semantic state/locale/network lock still needs a clean run manifest.
- [ ] Create a run manifest containing only fixture IDs and non-sensitive metadata.

**Exit gate:** exact source and artifact provenance is reproducible.

### Phase 1 — Product inventory and route closure

- [ ] Rebuild the Flutter journey inventory from current `E:\vista`, not only the August 5 CSV.
- [ ] Map every Flutter screen, modal, bottom sheet, notification action, deep link and Android intent to a Native owner.
- [ ] Replace or explicitly remove every `ControlledDetailScreen`, placeholder, deferred route and “implementation stage” copy from release paths.
- [x] Validate source-level contracts for self-profile routing, post/profile/group/chat deep links, invalid links, logged-out pending replay and logged-in dedupe; runtime connected proof is still pending.
- [ ] Add an owner, priority, source trace, Native trace and evidence path to every row.

**Exit gate:** zero unowned or hidden product journeys; zero release-path placeholders.

### Phase 2 — Core functional parity

- [x] Startup/auth source and fixture coverage exists for clean/warm/offline/maintenance/onboarding, OTP, 2FA, password setup and recreation; connected runtime execution remains pending.
- [ ] Password recovery: recovery options, send code, verify code, invalid/expired code, set new password, retry and secure cleanup. Native now contains the four Flutter-compatible remote contracts, but the UI/state flow is not yet wired to the recovery token lifecycle.
- [ ] Feed/posts: For You, Following, refresh, paging, append retry, cache fallback, post detail, image/video, caption entities, reactions, save, share, report and moderation states.
- [ ] Comments: root/reply tree, three-plus-level thread fixture, composer/IME, edit/delete/report, optimistic failure rollback, paging and dark/RTL states.
- [ ] Profile/social graph: own/other profile, private account, follow/requested/following, followers/following, edit profile, avatar, QR, block/report and post tabs.
- [ ] Stories/media: tray, player, create/edit, stickers, reply/reaction/report, viewers, upload/cancel/retry and expiry.
- [ ] Search/services/settings: query/debounce/history/QR decode, contacts, nearby, groups, WebView host policy, game SSO failure, all settings toggles, legal/about/help and saved posts.

**Exit gate:** every P0/P1 flow passes functional runtime fixtures on a healthy network and offline fixture set.

### Phase 3 — Chat, realtime and security closure

- [ ] Conversation list, archive, new message, group create/edit/details, partner info, message info and message search.
- [ ] Message loading from Room/cache, REST cursor paging, realtime merge, reconnect, dedupe, presence, typing, read state and lifecycle cancellation.
- [ ] Composer: text, emoji, GIF, reply, edit, delete, reactions, forward, attachments, document preview, image viewer and voice.
- [ ] E2EE: encryption success, malformed/corrupt payload, missing key, decrypt failure, retry and fail-closed behavior.
- [ ] Prove no plaintext/token leakage in logcat, Room snapshots, crash reports, screenshots and temporary files.
- [ ] Verify keyboard geometry, scroll anchoring, long-press, swipe-to-reply, multi-selection and interaction latency.

**Exit gate:** deterministic two-user chat suite passes with live REST/realtime and zero plaintext leakage.

### Phase 4 — Android contract parity

- [ ] Mirror required `SEND` and `SEND_MULTIPLE` intent filters and secure URI/ClipData handling.
- [ ] Close camera, media, audio, location, contacts, notification and storage permission matrices for API 24, 33 and current target API.
- [ ] Mirror FCM token refresh, foreground/background/killed delivery, channels, notification actions, read state and deep-link destinations.
- [ ] Implement or deliberately remove music download, shared playback, media notification and voice-chat audio-session conflicts.
- [ ] Validate WebView, external URL, FileProvider, media playback and update/install security policies.

**Exit gate:** manifest and runtime capability diff has no unexplained release-relevant gap.

### Phase 5 — Pixel parity certification

- [ ] Freeze reference fixtures for startup, auth, onboarding, shell, feed, post detail, comments, profile, search, services, settings, stories and chat.
- [ ] Capture Flutter and Native in the same semantic state, not merely after the same delay.
- [ ] Compare full screen and region crops: system bars, app bar, content, media, action row, bottom navigation, keyboard and dialogs.
- [ ] Record dimensions, mismatch percentage, MAE, mismatch bounding boxes and classification notes for every pair.
- [ ] Fix structural differences first: geometry, spacing, typography, direction, icon assets, safe areas and state copy.
- [ ] Re-run light/dark, RTL/LTR where supported, font scale 1.0/1.3, keyboard open, rotation and accessibility-size fixtures.

**Suggested acceptance thresholds:** no unclassified structural mismatch; full-screen mismatch ≤1% above threshold 16; critical component regions ≤0.5%; MAE and thresholds recorded per fixture. Thresholds may be tightened, never loosened, after the first clean baseline.

**Exit gate:** every P0/P1 screenshot pair is accepted or has a documented, approved intentional difference.

### Phase 6 — Data migration, performance and release

- [ ] Test signed Flutter → Native upgrade on the same package/signature strategy.
- [ ] Verify session, account identity, E2EE keys, Room/DataStore state, drafts, pending uploads, downloads and notification state.
- [ ] Verify rollback, interrupted upgrade, app kill during migration and corrupted/unknown legacy data.
- [ ] Run release build with R8/obfuscation, secret scan, lint, unit tests, connected tests and crash/ANR smoke.
- [ ] Measure startup, frame time/jank, memory, battery, network retries and APK size against Flutter reference budgets.
- [ ] Test API 24/33/current target, low bandwidth, offline recovery, TalkBack, large font and rotation.

**Exit gate:** release candidate is signed, upgrade-safe, observable and no slower than agreed budgets.

### Phase 7 — Final cutover and Flutter retirement

- [ ] P0/P1 ledger contains zero `Missing`, `Partial`, `Unverified`, `Unknown`, placeholder or capture-pending release rows.
- [ ] All release gates are linked to current artifacts from the exact candidate commit.
- [ ] Product, QA, security and engineering owners sign the final parity report.
- [ ] Publish Native release candidate and verify install/update/rollback path.
- [ ] Freeze `E:\vista` as read-only archive with commit/hash and retention policy.
- [ ] Remove Flutter from active CI/development only after production rollback window is accepted.

**Final cutover rule:** until every item above is checked, Flutter must not be ignored.

## Current priority queue

1. Freeze exact commits and package/signing decision.
2. Add Native inbound share and complete manifest/permission parity.
3. Finish recovery flow as a true Flutter contract, not only the simplified OTP branch.
4. Remove release-path fallbacks and close deep-link/notification destinations.
5. Restore a healthy authenticated network fixture and run live functional matrices.
6. Build the screenshot manifest and recapture all states in semantic lockstep.
7. Complete migration/upgrade/rollback and release certification.

## Decisions & Notes

- Existing dirty changes were preserved; this audit does not reset, clean, commit or overwrite user work.
- Current Native wiring is materially ahead of the 2026-08-24 historical report, so that report must not be used as the sole current status.
- Existing screenshots are valuable evidence artifacts, but old APKs, cached accounts and different semantic states cannot close current parity gates.
- “Implementation exists” is counted in the 59% source score only; it is intentionally excluded from the 23% evidence score unless runtime behavior is demonstrated.
- The Native architecture may be better than Flutter internally, but that does not waive required user-visible parity or release-contract compatibility.
- Do not delete or archive `E:\vista` before the final signed cutover and rollback decision.

## Phase 0–2 execution log — 2026-08-27

- Flutter reference commit: `64994fa1dfa6f1b146cb9d132758f105670e4d71`; worktree dirty.
- Native reference commit: `82be29b0fa125d3228325097d58c144fe8201a3c`; worktree dirty and user changes preserved.
- Native `:app:assembleProductionDebug --offline` passed after source changes.
- Native production compile passed; `:feature:auth:testDebugUnitTest` reached 33 tests but 3 pre-existing encoding-corrupted assertions failed.
- `:app:testProductionDebugUnitTest` hit an incremental KSP internal compiler error; this is not counted as a parity pass.
- Native manifest now declares HTTPS app links for `cafevista.ir` and `vista.me`.
- Native deep-link parser now accepts Flutter-compatible HTTPS `/post/<id>`, `/profile/<id>` and `/group/<code>` links, with contract tests added.
- Native auth remote/domain contracts now include Flutter recovery endpoints: `recovery-options`, `recovery-send`, `recovery-verify`, `recovery-complete`.
- Phase 0–2 are not release-complete: package/signing migration, clean semantic screenshot matrix, release-path fallbacks, inbound share, full recovery UI and connected runtime evidence remain open.
- 
## Addendum — verified implementation checkpoint

Date: 2026-08-27

- Verified `:feature:shell:compileDebugKotlin` successfully.
- Verified `:feature:auth:testDebugUnitTest` successfully.
- Verified `:app:assembleProductionDebug --offline` successfully.
- Built APK: `app/build/outputs/apk/production/debug/app-production-debug.apk`.
- Built APK SHA-256: `D5FF3B89C953FE492E7B394DABC1F74D57FCCF90CAF15E920006D93C41A23228`.
- Recovery now covers option lookup, code send, code verification, token capture, password completion, and recovery-code resend.
- Shell callback contracts are non-null and active chat/profile/settings/services routes no longer use the controlled-detail fallback.
- Cutover is still blocked: no pixel-diff sign-off, no complete runtime matrix, no verified inbound-share matrix, no final package/signing migration proof, and the dynamic services-detail route still lacks a real destination policy.
- The source implementation score must not be treated as release readiness; Flutter remains the reference until every cutover gate is evidenced.
