# AUD-01 — ممیزی و Parity Ledger ویستا

**وضعیت فاز:** `AUD-01 Complete — Audit Closure Gate Passed`
**وضعیت اصلاحات:** `Implementation Remediation Backlog Open`
**تاریخ ممیزی:** 2026-07-25
**آخرین بازبینی Gate:** 2026-07-25 — Audit Closure از Implementation Remediation تفکیک شد
**مخزن‌های اصلی:** `E:\vista` و `E:\vista_native`
**مخزن کمکی قرارداد:** `E:\vista-backend` (فقط read-only route verification)
**ممنوعیت رعایت‌شده:** هیچ کد اجرایی، dependency، navigation، theme، backend یا migration bridge تغییر نکرد.

## 1. واژگان وضعیت شواهد

| کد | دسته |
|---|---|
| VF | Verified repository fact |
| RI | Reasonable inference |
| PD | Proposed decision |
| UC | Unknown requiring confirmation |
| MI | Missing or incomplete |
| LD | Legacy/dead/duplicate |
| GE | Generated or intentionally excluded |

`path:line`های Flutter نسبت به `E:\vista` و pathهای Native نسبت به
`E:\vista_native` هستند. شواهد Backend با پیشوند `backend:` مشخص شده‌اند.

## 2. خط مبنای Git و Repository coverage

### 2.1 Git baseline

| Repository | Branch / commit | وضعیت |
|---|---|---|
| Flutter | `main` / `64994fa1dfa6f1b146cb9d132758f105670e4d71` | VF — dirty |
| Native | ندارد | VF — هر سه فرمان `git branch --show-current`، `git rev-parse HEAD` و `git status --short` با `fatal: not a git repository` شکست خوردند. ایجاد Git متعلق به `GOV-01` است. |

تغییرات موجود Flutter در شروع Audit:

```text
 M lib/app/app_runner.dart
 M lib/features/auth/widgets/session_auth_wrapper.dart
 M lib/features/chat/providers/chat_action_controller.dart
 M lib/features/chat/screens/modern_chat_screen.dart
 M lib/features/services/providers/services_hub_provider.dart
 M lib/features/services/screens/services_screen.dart
 M lib/services/http_client_factory.dart
 M lib/services/notification_sound_service.dart
 M pubspec.yaml
?? .claude/
?? assets/images/onboarding/
?? lib/features/onboarding/screens/onboarding_screen.dart
?? tmp/
```

هیچ‌کدام reset، checkout، stash، overwrite یا پاک نشدند.

### 2.2 آمار قابل‌بازتولید

فرمان مبنا:

```powershell
rg --files --hidden `
  -g '!**/.git/**' -g '!**/.gradle/**' -g '!**/build/**' `
  -g '!**/.dart_tool/**' -g '!**/.idea/**' -g '!**/node_modules/**' `
  -g '!**/vendor/**' -g '!**/generated/**' -g '!**/tmp/**' `
  -g '!**/coverage/**' <repository>
```

| Repository | فایل‌های غیرمستثنا | Source اصلی | خطوط Source اصلی | نکته |
|---|---:|---:|---:|---|
| Flutter | 4,677 | 463 Dart First-party | 162,706 | 491 Dart و 189,749 خط با generatedها؛ 28 فایل و 27,043 خط `*.g.dart`/generated از ممیزی خط‌به‌خط خارج شد. |
| Native | 90 | 34 Kotlin | 3,481 | 28 فایل main Kotlin و 6 فایل test/androidTest؛ یک module به نام `:app`. |

بخش‌های Flutter بر اساس فایل Dart First-party:

| بخش | فایل | خط |
|---|---:|---:|
| auth | 15 | 5,388 |
| chat | 130 | 51,199 |
| emoji | 5 | 461 |
| home | 1 | 712 |
| music | 1 | 304 |
| nearby | 9 | 3,653 |
| onboarding | 2 | 1,915 |
| posts | 33 | 16,010 |
| profile | 16 | 6,044 |
| search | 2 | 1,581 |
| services | 10 | 2,916 |
| settings | 19 | 8,494 |
| share | 1 | 432 |
| stories | 45 | 16,017 |

### 2.3 Include / exclude ledger

| دسته | وضعیت و دلیل |
|---|---|
| `lib/**`, Native `src/**` | VF — First-party source، شامل UI/state/data/security/platform bridge |
| Gradle، manifest، resources، `pubspec.yaml` | VF — dependency، package، permission، asset و platform contract |
| `test/**`, `androidTest/**` | VF — baseline و coverage intent |
| localization و asset declaration | VF — `pubspec.yaml:148-170` و ARBها؛ فایل‌های bitmap/font به‌عنوان binary inventory شدند، نه line-by-line text |
| Android/iOS/desktop integration | VF — declaration و First-party bridgeها بررسی شدند؛ تمرکز migration بر Android است |
| `*.g.dart`, generated Isar code | GE — generated؛ schema sourceهای `@collection` بررسی شدند |
| `.git`, `.dart_tool`, `.gradle`, `build`, `coverage`, `.idea`, `tmp` | GE — metadata/cache/output |
| `vendor`, `node_modules` | GE — third-party dependency code |
| Backend `vendor` و `.env`/secret material | GE — vendor و secret؛ فقط First-party route registration خوانده شد |

### 2.4 نسخه ابزارها

| ابزار | نتیجه |
|---|---|
| Java | Temurin OpenJDK `17.0.19` |
| Flutter metadata | stable `3.44.2`, framework `c9a6c484…` |
| Dart metadata | `3.12.2` |
| Gradle wrapper Native | `8.13`؛ `gradle/wrapper/gradle-wrapper.properties:3` |
| AGP / Kotlin | `8.13.2` / `2.0.21`؛ `gradle/libs.versions.toml:2-3` |
| `flutter --version` | UC — به lock موجود در SDK خورد و timeout شد؛ هیچ پردازش ناشناخته‌ای terminate نشد. نسخه از `E:\flutter\bin\cache\flutter.version.json` خوانده شد. |

## 3. معماری و پنج تب اصلی

VF — `HomeScreen` پنج tab را lazy می‌سازد و بعد از اولین بازدید state آن‌ها را
در `IndexedStack` حفظ می‌کند:

| Tab | Entry | شاهد |
|---:|---|---|
| 0 | Feed / `ExploreFeedScreen` | `lib/features/home/screens/homeScreen.dart:69-80` |
| 1 | Search / `SearchPage` | `lib/features/home/screens/homeScreen.dart:81` |
| 2 | Services / `ServicesScreen` | `lib/features/home/screens/homeScreen.dart:82-83` |
| 3 | Chat / `ChatConversationsScreen` | `lib/features/home/screens/homeScreen.dart:84-85` |
| 4 | Profile / `ProfileScreen` | `lib/features/home/screens/homeScreen.dart:86-91` |

Back در tab غیر Feed ابتدا به Feed برمی‌گردد؛ در Feed، دوبار Back طی دو ثانیه
اپ را می‌بندد: `lib/features/home/screens/homeScreen.dart:386-409` و
`lib/features/home/screens/homeScreen.dart:456-470`.

## 4. Flutter Feature Parity Ledger

ستون‌های اجباری به‌صورت دو جدول هم‌کلید نگهداری شده‌اند تا جدول قابل‌خواندن
بماند. `NS/P/V` یعنی Not Started / Partial / Verified.

### 4.1 رفتار و dependency

| ID | Feature و Flutter entry | رفتار/Business rule | State و supporting files | API / realtime | Storage / platform / offline / security | Native |
|---|---|---|---|---|---|---|
| AUT-01 | Startup, maintenance, ban — `lib/features/auth/widgets/session_auth_wrapper.dart:45`, `lib/screens/maintenance_screen.dart:5`, `lib/screens/banned_screen.dart:3` | session را resolve می‌کند؛ maintenance/ban/password/profile gate قبل از Home | Riverpod + service | system status, auth/me, refresh | secure token + offline fallback؛ auth-sensitive | P |
| AUT-02 | Onboarding — `lib/features/onboarding/screens/Onboarding.dart:8`, `lib/features/onboarding/screens/onboarding_screen.dart:388` | تکمیل onboarding قبل از auth | Stateful + `lib/services/onboarding_service.dart:1` | ندارد | SharedPreferences؛ local asset | P |
| AUT-03 | Login/register/OTP/2FA — `lib/features/auth/screens/auth_wizard_screen.dart:13` | lookup تعیین می‌کند password یا OTP؛ passwordless user باید password تنظیم کند | `auth_controller.dart:210`, `auth_repository.dart:375` | `/v1/auth/*` | secure session؛ retry دستی؛ TLS-sensitive | P |
| AUT-04 | Password recovery/change/biometric — `reset_password_screen.dart:7`, `biometric_login_screen.dart:7` | recovery چندمرحله‌ای، 2FA setup/change، local biometric unlock | mixed Stateful/Riverpod | recovery, 2fa, change-password | LocalAuthentication + secure token | MI |
| AUT-05 | Active sessions/logout — `ActiveSessionsScreen.dart:14` | register/validate/touch/list/terminate/terminate-others | `session_manager_service_v2.dart:351-970` | `/v1/sessions/*` | secure session ID/token؛ periodic touch | MI |
| SHE-01 | پنج‌تب Home | lazy build + state retention؛ composer جدا از tab | local index + Riverpod unread | چند provider | in-memory stack؛ custom back | NS |
| PST-01 | Feed/explore/following | cache-first/pagination، inline load-more retry | `posts_provider.dart:1`, `go_posts_repository.dart:65` | `/feed`, `/feed/following`, `/explore`, `/feed/event` | Isar PostEntity؛ network state | NS |
| PST-02 | Composer/upload | image/video/audio، validation، progress، cancel | `AddPost.dart:29`, `post_upload_provider.dart:1` | posts, upload presign/object | temp files؛ draft پایدار یافت نشد؛ retry محدود | NS |
| PST-03 | Post detail/comments | detail، comment/reply/edit/delete/report/appeal | `PostDetailPage.dart:31`, `comment_repository.dart:9` | posts/comments/appeal | cache محدود؛ retry دستی | NS |
| PST-04 | Like/save/follow | optimistic interaction و follow request | `post_action_buttons.dart:1`, `profile_repository.dart:1` | like/save/follow/unfollow | Isar merge؛ auth | NS |
| PST-05 | Reels/media | full-screen reel، image carousel، video autoplay/preload | `ReelsScreen.dart:13`, `reels_viewer_launcher.dart:9` | feed/media URL | media cache/player lifecycle | NS |
| PST-06 | Hashtag | trending/search/tag feed | `hashtag_posts_screen.dart:24` | hashtags/search/trending/posts/hashtag | pagination | NS |
| PST-07 | Saved posts | grid/reels/music saved content | `saved_posts_screen.dart:35` | `/me/saved` | local state + server | NS |
| STR-01 | Story list/player | active/user stories، preload، view، reply/reaction/report | `story_player_screen.dart:33`, `story_repository.dart:20` | stories + chat reply | DefaultCacheManager/audio/video lifecycle | NS |
| STR-02 | Story create/editor | camera/gallery/editor/stickers/upload | `story_creation_screen.dart:19`, `story_editor_screen.dart:25` | stories/upload | camera/location/photos؛ temp media | NS |
| STR-03 | Story privacy/highlights | close friends، reply policy، viewers، polls/questions/highlights | `story_privacy_settings_screen.dart:8`, `story_viewers_sheet.dart:20` | story settings/highlights/interactions | auth/privacy-sensitive | NS |
| NOT-01 | Notifications | list/read/delete/read-all و route به target | `notificationScreen.dart:13`, `notification_provider.dart:1` | notifications + FCM | local notification pending actions | NS |
| SRC-01 | Search/QR | user/content search و QR user/chat/group | `searchPage.dart:25`, `VistaQRScanner.dart:69-198` | profile/search/group | camera؛ recent search Isar | NS |
| NRB-01 | Nearby discover | location/preferences/discover/random/report | `nearby_screen.dart:17`, `nearby_repository.dart:20` | `/v1/nearby/*` | coarse location، denial states | NS |
| NRB-02 | Nearby likes/matches | likes received، match، unmatch، open chat | `nearby_likes_screen.dart:14` | likes/matches/match chat | auth/location | NS |
| SRV-01 | Services hub | dynamic section/banner با local fallback | `services_screen.dart:49`, `services_hub_repository.dart:14` | services-hub | cached provider state؛ safe fallback | NS |
| SRV-02 | Contacts/groups/game/web | contact discovery، top groups، SSO web game، restricted web | `contacts_screen.dart:11`, `game_launch_screen.dart:17`, `in_app_web_screen.dart:6` | contacts/top-groups/game-sso | READ_CONTACTS، WebView allowlist/host restriction | NS |
| PRF-01 | Profile view/edit/setup | profile، media tabs، refresh، edit، completion gate | `profileScreen.dart:65`, `profile_controller.dart:1` | profiles/me/profile | Isar ProfileEntity + image picker | NS |
| PRF-02 | Notes/follow graph | profile note، followers/following | `note_input_sheet.dart:8`, `FollowersScreen.dart:8` | note/batch/followers/following | cache + auth | NS |
| CHT-01 | Inbox/archive/new | cache-first conversations، unread، archive، new/group | `ChatConversationsScreen.dart:37`, `chat_repository_impl.dart:157-208` | chat REST + WS | Isar ConversationEntity؛ reconnect | NS |
| CHT-02 | Conversation/send | text/reply/media optimistic send، pagination | `modern_chat_screen.dart:180`, `chat_messages_provider.dart:1` | messages + WS | Isar MessageEntity؛ pending resend | NS |
| CHT-03 | Message actions | edit/delete/forward/pin/reaction/read | `message_actions_service.dart:57-240` | message subresources | tombstone + optimistic state | NS |
| CHT-04 | Media/voice/document | picker/editor/upload/download/cache/playback | `chat_attachment_sheet.dart:58`, `document_preview_screen.dart:26` | upload + message | mic/files/photos؛ temp/persistent files | NS |
| CHT-05 | Groups | create/edit/detail/members/admin/invite/join/leave | `group_service.dart:100`, `group_details_screen.dart:13` | chat/groups | group state + invite deep link | NS |
| CHT-06 | E2EE/at-rest | X25519، HKDF، AES-GCM، legacy envelope، fail-closed decrypt | `e2e_encryption_service.dart:10-35`, `local_content_cipher.dart:28-42` | public key via profile + encrypted message | secure keys + encrypted Isar fields | NS |
| CHT-07 | Realtime/presence/typing/read | process singleton WS، dedup، backoff+jitter، targeted streams | `sse_manager.dart:144-209`, `typing_service.dart:100-138` | `/v1/chat/ws` | no durable cursor found؛ reconnect sync | NS |
| CHT-08 | Search/info/profile/pinned | message search، message info، peer/group profile، pinned | screen classes at `ChatMessageSearchScreen.dart:29`, `message_info_screen.dart:25` | chat search/pinned/profile | server + local cache | NS |
| CHT-09 | Offline/retry/tombstone | failed-message resend، delete tombstone، orphan cleanup | `retry_queue_service.dart:78-190`, `message_tombstone_service.dart:1` | chat/upload delete | Isar RetryQueue/DeletionTask؛ no OS scheduler | NS |
| SHR-01 | Share in/out | Android share receiver، target picker، post/story/chat share | `share_target_screen.dart:27`, `share_receiver_service.dart:40-45` | upload/chat/post | MethodChannel + SEND/SEND_MULTIPLE | NS |
| SET-01 | Settings/theme/storage/notification/privacy | local/server settings و cache cleanup | `Settings.dart:1`, `settings_cache_service.dart:199-240` | me/settings/privacy/notification-settings | SharedPreferences + Isar | NS |
| SET-02 | Block/report/verify/legal/support | block list، report، verification، FAQ/legal/contact | `privacy_security_page.dart:15`, `ContactUs.dart:13` | block/verify/contact/support chat | sensitive account actions | NS |
| PAY-01 | Premium/Bazaar/Zibal | plan list، Bazaar purchase، server verify، Zibal callback verify | `pricing_page.dart:16`, `payment_service.dart:55-276` | payment endpoints | Bazaar MethodChannel + browser callback | NS |
| MUS-01 | Music/playback/download | upload/play، local download state | `MusicDownloadManager.dart:32-303`, `MusicService.dart:1` | music + media URL | broad storage permission؛ manifest پایدار ندارد | NS |
| SYS-01 | Update/system/TLS | status poll، feature gate، APK update، pin off/monitor/enforce | `system_status_service.dart:233-237`, `http_client_factory.dart:1`, `tls_pinning_store.dart:9-107` | system status/check-update/tls-report | secure config/cache؛ updater MethodChannel | MI |

### 4.2 Migration/classification/test ledger

| ID group | Retain/Refactor/Replace/Merge/Remove | Target Native / phase | Required tests و acceptance | Dependency / blocker / status |
|---|---|---|---|
| AUT-01..05 | Refactor موجود Native؛ Replace فقط placeholder | `feature:auth`, FND-01 | startup matrix، refresh، offline، ban، password gate، session list | Native session register/validate/touch ندارد؛ Partial |
| SHE-01 | Replace Flutter shell behavior | `feature:shell`, DSN-01 | پنج back-stack مستقل، restore/process death، RTL | type-safe nav/design system؛ NS |
| PST-01..07 | Replace behavior؛ Merge upload policy | feed/composer، SLICE-01/MEDIA-01 | paging/cache/optimistic/offline/media/screenshot | Room/outbox/upload؛ NS |
| STR-01..03 | Replace behavior؛ Merge media pipeline | stories، STORY-01 | player lifecycle، privacy، upload retry، interactions | media/outbox/design system؛ NS |
| NOT-01/SRC-01/NRB-01..02 | Replace behavior | DISC-01 | payload routes، QR/deeplink، permission denial، offline | FCM/navigation/location؛ NS |
| SRV-01..02 | Replace behavior؛ Retain server fallback semantics | services، DISC-01 | dynamic/local fallback، contact denial، WebView allowlist | contacts/SSO/web security؛ NS |
| PRF-01..02 | Replace behavior | profile، SLICE-01 | refresh/cache/follow/note/profile gate | auth/Room/upload؛ NS |
| CHT-01..09 | Replace UI/data؛ Retain crypto wire formats و retry semantics | chat، CHAT-01/02 | offline send، reconnect، duplicate/out-of-order، E2EE vectors، all media/group/actions | largest blocker: Room/WS/FCM/migration keys؛ NS |
| SHR-01 | Refactor Android bridge into Native intents | composer/chat، MEDIA-01 | SEND/SEND_MULTIPLE/process recreation | FileProvider/URI permissions؛ NS |
| SET-01..02 | Replace behavior؛ Merge preferences migration | settings، DISC-01/MIG-01 | privacy/session/block/legal/settings migration | DataStore/Room/auth؛ NS |
| PAY-01 | Replace integration؛ Retain server verification rule | payments، DISC-01 | Bazaar/Zibal/restore/deeplink | signing/package/store credentials؛ NS |
| MUS-01 | Replace broad storage path | music، DISC-01 | SAF/MediaStore/download manifest/restart | download migration source missing؛ NS |
| SYS-01 | Replace client stack؛ Retain signed/pinning policy | core network/update، FND-01/DISC-01 | flat+nested error، pin rotation، update/install | cleartext mismatch و signed config؛ MI |

تمام featureها برای رسیدن به `Verified` به runtime واقعی، screenshot parity و
acceptance test نیاز دارند؛ compile/unit test به‌تنهایی کافی نیست.

## 5. Screen / dialog / sheet / overlay inventory

### 5.1 آمار

| مورد | تعداد | روش |
|---|---:|---|
| Public `Screen`/`Page` class | 66 | declaration scan |
| Dialog/Sheet/Overlay class declaration | 44 | declaration scan؛ شامل private nestedها |
| `showDialog` call site | 36 | static call-site scan |
| `showModalBottomSheet` call site | 42 | static call-site scan |
| `OverlayEntry`/insert call site | 5 | static call-site scan |

کد state: `R`=Riverpod/Consumer، `S`=Stateful، `-`=Stateless.
کد حالت: `L/E/X/O/P`=Loading/Empty/Error/Offline/Permission؛ این flagها RI و
بر اساس شاخه‌های موجود در همان فایل‌اند و به‌معنی runtime verification نیستند.

### 5.2 تمام Screen/Pageهای کشف‌شده

| Domain | Screen/Page و شاهد | State / state flags |
|---|---|---|
| Startup | `BannedScreen` — `lib/screens/banned_screen.dart:3`; `MaintenanceScreen` — `lib/screens/maintenance_screen.dart:5`; `InvalidRouteScreen` — `lib/widgets/invalid_route_screen.dart:5` | `-/none`, `S/LX`, `-/X` |
| Auth | `AuthWizardScreen:13`, `BiometricLoginScreen:7`, `MandatoryPasswordScreen:9`, `ResetPasswordScreen:7`, `PasswordRecoveryConfirmScreen:9`, `PasswordResetCodeScreen:10`, `PasswordResetSmsScreen:11`, `PasswordSetScreen:8` under `lib/features/auth/screens/` | R/S؛ L/X؛ custom back در wizard/mandatory |
| Shell | `HomeScreen` — `lib/features/home/screens/homeScreen.dart:54` | R/LX؛ custom back |
| Feed/Post | `ExploreFeedScreen:69`, `AddPublicPostScreen:29`, `PostDetailsPage:31`, `AppealScreen:12`, `HashtagPostsScreen:24`, `SavedPostsScreen:35`, `VistaStoryTemplateScreen:8` under `lib/features/posts/screens/`; `ReelsScreen` — `lib/widgets/ReelsScreen.dart:13`; `YourVideoTrimmerPage` — `lib/widgets/YourVideoTrimmerPage.dart:7` | R/S؛ feed دارای LEXO؛ composer/detail دارای LEX؛ trimmer custom back |
| Social/Profile | `ProfileScreen` — `lib/features/posts/screens/profileScreen.dart:65`; `FollowersScreen:8`, `FollowingScreen:8`; `AccountDetailsScreen:15`, `ProfileSetupWizardScreen:13` under profile | R/S؛ LEX؛ setup custom back |
| Story | `StoryCreationScreen:19`, `StoryEditorScreen:25`, `StoryPlayerScreen:33`, `StoryPrivacySettingsScreen:8` under `lib/features/stories/presentation/screens/` | R؛ LEXP؛ editor custom back |
| Search | `SearchPage` — `lib/features/search/screens/searchPage.dart:25` | R/LEX |
| Nearby | `NearbyScreen:17`, `NearbyLikesScreen:14`, `NearbyMatchesScreen:11` | R/-؛ LEXP |
| Services | `ServicesScreen:49`, `ContactsScreen:11`, `TopGroupsScreen:8`, `GameLaunchScreen:17`, `InAppWebScreen:6` | R/S؛ LEXP؛ WebView custom back |
| Notification | `NotificationsPage` — `lib/features/posts/screens/notificationScreen.dart:13` | R/LE |
| Chat list | `ChatConversationsScreen:37`, `ArchivedConversationsScreen:14`, `NewMessageScreen:18` | R/LEX |
| Chat detail | `ModernChatScreen:180`, `ChatMessageSearchScreen:29`, `MessageInfoScreen:25`, `DocumentPreviewScreen:26` | R/S/-؛ LEX |
| Chat profile/group | `VistaChatProfileScreen:79`, `ChatPartnerInfoScreen:11`, `ModernGroupProfileScreen:23`, `GroupCreateScreen:16`, `GroupDetailsScreen:13`, `GroupEditScreen:10` | R؛ LEX |
| Settings | `Settings` خود class suffix ندارد ولی route screen است — `lib/features/settings/screens/Settings.dart:18`; `PrivacySecurityPage:15`, `DataStorageSettingsPage:13`, `NotificationSettingsPage:9`, `ThemeSettingsPage:12`, `ActiveSessionsScreen:14`, `BlockedUsersPage:12`, `VerificationRequestPage:13`, `AboutSettingsPage:13` | mixed؛ Active/Blocked دارای LEX |
| Legal/support | `ContactUsScreen:13`, `FAQScreen:3`, `PrivacyPolicyScreen:3`, `TermsAndConditionsScreen:3` | S/- |
| Premium | `PricingPage` — `lib/features/settings/screens/vistaStore/pricing_page.dart:16`; `VerificationBadgeStore` class suffix ندارد — `store.dart:10` | R/S؛ LEX |

### 5.3 Modalها و overlayها

تمام 44 declaration در این گروه‌ها قرار گرفتند:

- Chat: `ChatAttachmentSheet`, `DocumentUploadSheet`, `ForwardMessageSheet`,
  `DeleteMessageDialog`, `EditMessageDialog`, `BlockReportBottomSheet`,
  `ReactionsDetailSheet`, add-member/note-quick-reply sheets و crop overlay؛
  شواهد classها در `lib/features/chat/widgets/*` و
  `lib/features/chat/screens/group_details_screen.dart:669`.
- Story: media/sticker/location/link/mention/poll/question/countdown/music/privacy/
  album sheets و `StoryViewersSheet`؛
  `lib/features/stories/presentation/widgets/story_sticker_sheet.dart:21`.
- Post/profile: music trim، upload/like overlay، content picker، note input/viewer؛
  `lib/features/posts/widgets/upload_progress_overlay.dart:13` و
  `lib/features/profile/widgets/note_input_sheet.dart:8`.
- Shared/legacy: دو `CommentsBottomSheet` declaration (LD duplicate)،
  `ShareBottomSheet`, `ShareTargetSheet`, `UserSelectionBottomSheet`,
  `ReportDialog`, `ReportProfileDialog`, `VistaDialog`؛
  `lib/utils/comments_bottom_sheet.dart:12` و `lib/utils/widgets.dart:619`.
- Nearby/settings: permission request/settings، preferences و session details
  sheets؛ `lib/features/nearby/widgets/location_permission_dialog.dart:84-244`.

برای همه مقصدهای Native، screenshot parity الزامی است. A11y در بسیاری از
Screenها فقط RI از وجود `Semantics`/`Tooltip` است و روی دستگاه تأیید نشده.

## 6. Navigation و back-stack matrix

### 6.1 Named routeها

23 route key در `lib/app/app_runner.dart:639-767`:

```text
/home, /onboarding, /auth, /maintenance, /banned, /profile-setup,
/reset-password, /reset-password-confirm, /reset-password-set,
/mandatory-password, /biometric-login, /editeProfile, /settings, /feed,
/nearby, /verification-store, /premium, /post-detail, /appeal, /profile,
/chat, /story/create, /story/view
```

`/post-detail`، `/appeal`، `/profile` و `/chat` argument guard دارند و در
نبود argument به Invalid state می‌روند:
`lib/app/app_runner.dart:671-760`.

57 مقصد distinct از `MaterialPageRoute` scan نیز یافت شد؛ مقصدهای اصلی شامل
تمام Settings subpageها، follower/following، QR، group flows، media editor،
reels، story editor/player، chat profile/search/info و post/profile detail
هستند. custom routeها `ProfileRoute`, `PostDetailRoute` و `ChatScreenRoute`
هستند: `lib/features/posts/navigation/content_routes.dart:8-64` و
`lib/features/chat/navigation/chat_screen_route.dart:13`.

| Context | Back behavior |
|---|---|
| Home tab | custom: tab → Feed؛ Feed double-back → exit |
| Auth wizard | custom: step back؛ root محدود |
| Mandatory password/profile setup | custom gate؛ نباید bypass شود |
| WebView | custom: WebView history، سپس Navigator |
| Story editor/trimmer | custom discard/back handling |
| سایر Material routeها | RI — default pop مگر nested modal باز باشد |
| Native فعلی | startup routes با `popUpTo(... inclusive=true)`؛ `AppNavGraph.kt:54-79`; فقط 5 route |

## 7. Deep-link matrix

| Link | مقصد فعلی | وضعیت |
|---|---|---|
| `vista://group/{code}` | join/preview group | VF — `deep_link_service.dart:77-80` |
| `vista://post/{id}` | post detail | VF — `:81-84` |
| `vista://profile/{id}` | profile | VF — `:85-89` |
| `vista://chat/{conversation}` | `/chat-detail` | MI — route ثبت‌شده `/chat` است؛ `:90-96` در برابر `app_runner.dart:721` |
| `vista://appeal/{post}?type=` | appeal | VF — `:97-104` |
| `vista://notifications` | Home | MI — tab Notifications مستقلی انتخاب نمی‌شود؛ `:105-108` |
| `vista://nearby[/likes|matches]` | nearby یا likes tab | VF — `:109-120` |
| HTTPS `/payment/callback` | Zibal server verify | VF — `:135-145` |
| HTTPS `/post/{id}` | post | VF — `:146-149` |
| HTTPS `/profile/{username}` | profile | VF — `:150-153` |
| HTTPS `/group/{code}` | group | VF — `:154-157` |
| HTTPS `/feed` | feed | VF — `:158-160` |
| `vista://user/{id}` / `vista://chat` QR | QR-only dispatcher | LD/MI — naming با central dispatcher یکسان نیست؛ `VistaQRScanner.dart:69-129` |

Android Flutter فقط hostهای `auth/post/profile/group` را برای scheme `vista`
اعلام کرده است (`android/app/src/main/AndroidManifest.xml:89-99`)؛ درنتیجه
appeal/notifications/nearby برای cold OS dispatch صریحاً در manifest host ندارند.
Native فعلی فقط launcher intent دارد و هیچ deep link ندارد:
`app/src/main/AndroidManifest.xml:19-27`.

## 8. Notification-action matrix

| canonical type/action | destination | evidence |
|---|---|---|
| message/reaction | chat | `notification_action_resolver.dart:41-45` |
| like | post | `:46-50` |
| comment | post comments | `:51-56` |
| comment_reply | focused reply | `:57-63` |
| follow/follow_request/accepted | profile | `:64-70` |
| mention | post، deep link یا notifications fallback | `:71-87` |
| suggest_follow | profile suggestion | `:88-92` |
| suggest_post | post/deep link | `:93-106` |
| daily_suggestion_digest | digest | `:107-110` |
| unknown | deep link یا notifications | `:111-121` |
| local action `reply` | background/direct chat reply | `PushNotificationService.dart:259-264`, `:921-932` |
| local action `mark_read` | conversation read | `PushNotificationService.dart:264-266`, `:932` |

Native فعلی FCM، notification channel، receiver/action یا destination ندارد
(MI؛ manifest فقط Internet + launcher).

## 9. API contract inventory

Static extraction از callsiteهای literal، **176 امضای یکتای method/path**
یافت. base URLهای repository-specific (`/v1/auth`, `/v1/chat`, `/v1/nearby`
و غیره) هنگام runtime به pathها افزوده می‌شوند؛ بنابراین جدول زیر group
inventory است و command extraction مرجع بازتولید raw signatureهاست.

| Domain/path head | تعداد | شاهدهای representative |
|---|---:|---|
| chat | 39 | `chat_repository_impl.dart:232-1530`, `group_service.dart:121-406` |
| me | 25 | `profile_repository.dart:373-485`, `settings_cache_service.dart:199-434` |
| stories | 17 | `story_repository.dart:30-360` |
| posts | 10 | `go_posts_repository.dart:110-462` |
| profiles | 8 | `profile_repository.dart:142-449` |
| comments | 7 | `comment_repository.dart:89-335` |
| sessions (`/v1`) | 7 | `session_manager_service_v2.dart:351-893` |
| highlights | 5 | `story_repository.dart:382-460` |
| notifications | 5 | `notification_provider.dart:305-485` |
| payment | 4 | `payment_service.dart:55-250` |
| feed | 3 | `go_posts_repository.dart:88-101`, `:449` |
| matches | 3 | `nearby_repository.dart:140-163` |
| music | 3 | `MusicService.dart:52-81` |
| services-hub | 3 | `services_hub_repository.dart:28-61` |
| auth root actions | 18 | `auth_repository.dart:429-838` |
| nearby remaining | 12 | `nearby_repository.dart:61-194` |
| upload | 2 | `backend_upload_service.dart:191-237` |
| presence | 2 | `user_presence_service.dart:164-195` |
| system/TLS | 3 dynamic | `system_status_service.dart:233-237`, `tls_pinning_store.dart:107` |
| FCM | 1 dynamic | `PushNotificationService.dart:1395-1425` |

Backend route registration برای domainهای اصلی تأیید شد:

- auth/session/upload/system:
  `backend:internal/httpapi/router.go:151-200`
- chat REST/WS:
  `backend:internal/chat/handlers.go:93-103`
- post:
  `backend:internal/posts/handlers.go:29-41`
- story:
  `backend:internal/stories/handlers.go:28-34`
- profile/settings:
  `backend:internal/profile/handlers.go:36-59`
- nearby:
  `backend:internal/nearby/routes.go:11-20`
- services:
  `backend:internal/serviceshub/routes.go:11-15`
- payment:
  `backend:internal/payment/handlers.go:36-40`

MI — این extraction literal pathهای ساخته‌شده کاملاً dynamic، presigned S3 PUT،
external weather/geocoder/GIF/media URL و WebView URL را در عدد 176 ادغام
نمی‌کند؛ آن‌ها به‌عنوان platform/external integration نگهداری شده‌اند.

## 10. Realtime / WebSocket inventory

| Event/transport | producer/consumer و رفتار |
|---|---|
| `/v1/chat/ws` | VF — Authorization header، ping 20s، backoff 1..30s با ±20% jitter؛ `sse_manager.dart:144-209`; backend `chat/handlers.go:102-103` |
| dedup/type fanout | VF — parsed envelope dedup و per-type controller؛ `sse_manager.dart:178-189` |
| `new_message` | conversation/message sync؛ `chat_repository_impl.dart:181-184`, `:391-413` |
| `message_updated` | message sync؛ `:391-413` |
| `message_deleted` | immediate Isar delete/tombstone؛ `:369-388` |
| `conversation_updated` | inbox sync؛ `:181-184` |
| `conversation_cleared` | local clear + sync؛ `:183`, `:414-419` |
| `read_receipt` | ignore self echo، update local read cursor؛ `:397-410`, `modern_read_receipt_service.dart:167-203` |
| `typing` | targeted stream + timeout removal؛ `typing_service.dart:112-133` |
| `reaction_updated` | targeted reaction cache publish؛ `message_reactions_service.dart:143-150` |
| presence | REST heartbeat/provider periodic path؛ full WS event contract UC |

MI — durable sequence/cursor و explicit gap token در client یافت نشد؛ reconnect
با REST re-sync جبران می‌شود، اما out-of-order contract کامل برای Native هنوز
تصمیم/تست ندارد.

## 11. Local storage و cache inventory

| Store | داده / lifecycle / sensitivity | evidence |
|---|---|---|
| Isar | Message, Conversation, RecentSearch, AppSettings, DeletionTask, Profile, Post, RetryQueue | `lib/DB/isar_database_manager.dart:79-87` |
| Isar encryption | key در secure store؛ fallback open بدون key در failure path وجود دارد | `isar_database_manager.dart:222-272` |
| Message/Conversation legacy files | `messages_cache.sqlite`, `conversations.sqlite` | `database_file_utils.dart:11-25` |
| Flutter secure storage | access/refresh/user/expiry/password/session IDs | `auth_controller.dart:23-28`, `:152-164` |
| E2EE keys | global legacy `e2ee_*` و per-user `e2e_*` | `security/e2ee_service.dart:12-24`, `e2e_encryption_service.dart:74-98` |
| Local content key | `local_content_aes_key_v1` | `local_content_cipher.dart:42-66` |
| SharedPreferences | 27 owner با فراخوانی مستقیم `getInstance`؛ 31 reference در broad scan؛ onboarding، settings، media، prompt، FCM، session/location و stateهای per-conversation | static inventory + exact key-family mapping در بخش 15 |
| File/cache dirs | app documents/cache/temp + public Download/Vista dirs | `file_manager_service.dart:12-90` |
| Native session | AES-GCM payload in SharedPreferences، key in AndroidKeyStore | `SessionStore.kt:34-136` |
| Native onboarding | versioned SharedPreferences | `OnboardingStore.kt:12-43` |

## 12. Background task و retry inventory

| کار | persistence/trigger | وضعیت |
|---|---|---|
| FCM background handler | top-level entrypoint + queued actions | VF — `PushNotificationService.dart:28-38`, `:172-266` |
| Chat failed-message resend | reconnect-aware repository path | VF/RI |
| RetryQueue | Isar persistent؛ pending/failed processing on online/in-process trigger | VF — `retry_queue_service.dart:78-135` |
| RetryQueue chat operations | intentionally return false؛ repository owns them | VF — `retry_queue_service.dart:147-162` |
| Orphan media cleanup | RetryQueue delete object/by URL | VF — `:143-145`, `:170-190` |
| Deletion task/tombstone | Isar persistent | VF |
| system/session/presence polls | in-process `Timer.periodic` | VF — `app_runner.dart:346-362`, `presence_provider.dart:111` |
| image processing | `compute` isolate | VF — `telegram_image_editor.dart:122-265` |
| WorkManager / OS durable scheduler | وجود ندارد | MI |

## 13. Permission و platform integration inventory

| Integration | Flutter | Native |
|---|---|---|
| Internet/network | manifest `:5-6` | Internet only؛ Native manifest `:5` |
| Notifications/FCM | POST_NOTIFICATIONS + Firebase Messaging/local actions | MI |
| Location | COARSE + Geolocator denial/settings | MI |
| Contacts | READ_CONTACTS + permission_handler/FlutterContacts | MI |
| Camera/photos/video | CAMERA + picker/camera + media permissions | MI |
| Microphone/audio | RECORD_AUDIO + voice services | MI |
| Storage/download | MANAGE/READ/WRITE/READ_MEDIA؛ broad/legacy | MI؛ PD: SAF/MediaStore |
| Share receiver | SEND/SEND_MULTIPLE + MethodChannel | MI |
| Bazaar payment | permission/queries + native channel | MI |
| Update installer | REQUEST_INSTALL_PACKAGES + MethodChannel | MI |
| Audio foreground service | WAKE_LOCK/FOREGROUND_SERVICE/media service/receiver | MI |
| WebView | host/path restriction exists | MI |

VF security mismatch: Flutter manifest production declaration
`android:usesCleartextTraffic="true"` در
`android/app/src/main/AndroidManifest.xml:51-58` است، درحالی‌که Native آن را
false کرده: `app/src/main/AndroidManifest.xml:7-17`. تصمیم نهایی Network
Security Config در FND-01 نیازمند تأیید و test است؛ تغییر در AUD-01 ممنوع بود.

## 14. Security-sensitive flow inventory

| Flow | وضعیت |
|---|---|
| Session/token | secure storage، refresh interceptor و session manager موجود؛ Native فقط auth refresh دارد و session registration ندارد |
| TLS pinning | Flutter admin-managed store/report؛ Native plain OkHttp بدون pin/status policy |
| E2EE | Flutter X25519 + AES-GCM/legacy envelopes؛ Native ندارد |
| Local message encryption | Flutter LC1 AES-GCM؛ Native chat DB ندارد |
| Password/OTP/2FA | هر دو partial parity؛ Native error parser flat/nested را پوشش می‌دهد (`OkHttpAuthRemoteDataSource.kt:130-146`) |
| Biometric | Flutter دارد؛ Native ندارد |
| Payment | server verification دارد؛ Native ندارد |
| WebView | Flutter restrict host/path؛ Native ندارد |
| Logging | UC — static sensitive-key scan انجام شد، اما runtime log/redaction test نشده |
| Backup/exported | Native backup false/cleartext false؛ Flutter FileProvider non-exported، ولی AudioService/receiver exported هستند (`AndroidManifest.xml:168-195`) |

## 15. Migration-source inventory

### 15.1 Source-to-target matrix قطعی

| Source | Source فعلی و شاهد | نتیجه‌ی ممیزی | Target Native / import policy | تست پذیرش قبل از مهاجرت |
|---|---|---|---|---|
| Session | secure keys شامل access/refresh/user/expiry/password؛ `auth_controller.dart:23-28`, `:152-164`. session ID/token قدیمی یک‌بار از SharedPreferences به secure storage منتقل می‌شود؛ `session_manager_service_v2.dart:700-738`. | VF — source وجود دارد؛ bridge بین packageهای Flutter و Native وجود ندارد. | `EncryptedSessionStore` + Android Keystore؛ `SessionStore.kt:34-147`. importer باید زیر package نهایی و با schema version اجرا، session را با Backend validate و فقط پس از commit موفق source را پاک کند. | valid، expired+refresh، revoked، missing refresh، malformed ciphertext، rollback و idempotent re-run |
| E2EE | legacy global `e2ee_private_key/public_key`، per-user `e2e_priv/pub_{user}`، peer keyهای `e2e_peer_pub_{conversation}` و `local_content_aes_key_v1`؛ `security/e2ee_service.dart:12-24` و `e2e_encryption_service.dart:74-98`. | VF/MI — sourceها کشف شدند؛ bridge، envelope version و cross-client test vector وجود ندارد. | private/local-content keys در Keystore-wrapped storage؛ public/peer keys و envelope metadata در Room با user/conversation scope. هیچ key بدون verify حذف نشود. | decrypt fixtureهای legacy/current، wrong-user isolation، key rotation، reinstall/restore و mixed-version conversation |
| Draft | Post composer فقط controller/fileهای حافظه است؛ `AddPost.dart:45-107` و هنگام dispose از بین می‌رود؛ `AddPost.dart:766-771`. Upload retry نیز فقط `StateNotifier<List<UploadTask>>` و `_params` حافظه است؛ `post_upload_provider.dart:19-95`, `:273-310`. chat «draft» فقط handoff متن share و pre-edit restore همان screen است؛ `modern_chat_screen.dart:144`, `:505-511`, `:628-639` و `new_message_screen.dart:138-152`. | **Definitive MI — persistent Draft source وجود ندارد.** فایل انتخاب‌شده یا upload task، draft قابل‌مهاجرت محسوب نمی‌شود. | policy قفل‌شده `zero-import` است. Native در `MEDIA-01` قابلیت پایدار جدیدی مانند `Room DraftEntity(ownerId, destinationId, text, attachments, updatedAt, schemaVersion)` می‌سازد؛ برای نسخه موجود Flutter هیچ bridge/importی تعریف نمی‌شود. | zero imported rows، process-death composer، attachment permission expiry و isolation کاربر/مقصد |
| Pending operation | Isar `RetryQueueEntity`، `DeletionTaskEntity` و pending message ownerهای جدا دارند؛ `retry_queue_entity.dart:12-33`, `deletion_task_entity.dart:5-23`, `message_entity.dart`. Post upload task persistent نیست. | Partial — data واقعی برای chat/delete وجود دارد؛ payload version و owner contract یکپارچه نیست. | Room durable operation queue + WorkManager؛ import فقط operationهای version شناخته‌شده و idempotency key معتبر، quarantine برای payload ناشناخته. | duplicate delivery، crash mid-import، network loss، stale media URI، max-attempt و rollback |
| Download | Music manager وضعیت را فقط در `Map<String, DownloadInfo>` نگه می‌دارد؛ `MusicDownloadManager.dart:32-49`, `:189-196`. فایل Android مستقیماً در `/storage/emulated/0/Download` یا external dir نوشته می‌شود؛ `:95-135`. `FileManagerService` پوشه‌های `Download/Vista/{files,images,audio_files,temp}` را می‌سازد؛ `file_manager_service.dart:12-97`. | **Definitive MI — music manifest پایدار وجود ندارد.** فایل ممکن است بماند ولی ارتباط URL/status/path پس از process death از دست می‌رود. | user-owned: scan محدود MediaStore/SAF و ثبت Room `DownloadManifest` بدون جابه‌جایی اجباری. app-cache/temp/DefaultCacheManager: migrate نشود و قابل‌بازسازی باشد. | duplicate filename، partial file، content hash، revoked permission، uninstall/reinstall، delete semantics و cache eviction |
| Preferences | 27 owner مستقیم SharedPreferences، Isar `AppSettingsEntity` و cacheهای JSON/server؛ key-familyها در 15.2. | VF — source کامل group شد؛ schema واحد فعلی وجود ندارد. | Proto DataStore برای scalar app prefs؛ Room برای per-entity/cache/queue؛ Keystore برای credential/key؛ server همچنان source-of-truth تنظیمات حساب. | default parity، unknown key، version upgrade/downgrade، per-user isolation، logout retention و idempotent import |

### 15.2 Preferences owner/key → target mapping

| Family | Source owner / keyهای فعلی | مالکیت و حساسیت | Target و disposition |
|---|---|---|---|
| Server settings cache | `cached_user_settings`, `cached_app_settings`, `cached_privacy_settings`, `cached_notification_settings`, `settings_cache_last_update`؛ `settings_cache_service.dart:20-24`, `:51-110` | server-owned mirror؛ JSON و timestamp | Room cache با user/type/version/updatedAt؛ refresh از `/me/settings`، نه تبدیل blob کور |
| Isar app settings | `selectedColor`, `isDark`, auto-download، font size، battery/cache/preload و language؛ `app_settings_entity.dart:5-26` | local scalar | Proto DataStore؛ map explicit با defaultهای فعلی |
| Appearance/performance | `animations_enabled`, `battery_saver_mode`, `chat_blur_background_enabled`؛ `animation_controller_service.dart:73-105`, `settings_providers.dart:375-420` | local scalar | Proto DataStore |
| Media/data | `data_upload_quality`, `image_quality`, `video_quality`, `data_saver`, `auto_quality`, `volume`, `video_auto_play`, `video_data_saver`, `video_position_{videoId}`, `voice_cache_info`؛ `data_storage_settings_page.dart:35-91`, `VideoPlayerConfig.dart:6-90`, `voice_cache_service.dart:16-73` | scalar + per-media/cache manifest | scalarها Proto؛ position/cache metadata در Room؛ cache bytes migrate نشود |
| Onboarding/prompts/UI | `onboarding_completed`, `onboarding_version`, `loc_perm_prompt.never_ask`, `loc_perm_prompt.last_prompt_ms`, `user_selection_info_banner_visible`, `last_phone_verification_skipped_time`, `vista_recent_emojis_v1` | local UX state | Proto DataStore؛ versioned و با retention صریح در logout |
| Notification/FCM | `pending_notification_actions_v1`, `fcm_sync_epoch_ack`, `fcm_last_sync_ok_ms`, `fcm_last_synced_token`, `chat_notification_read_at:{conversation}`, `chat_notification_latest_at:{conversation}`, `in_app_chat_sounds`؛ `PushNotificationService.dart:52-64`, `notification_sound_service.dart:67` | queue/per-conversation + scalar | pending actions/watermark در Room؛ sync metadata/sound در Proto؛ token بعد از import با server re-register شود |
| Session/location/device | `session_manager_v2.session_id/token`، scoped location/cache/sync keys و `vista_device_id`؛ `session_manager_service_v2.dart:72-90`, `:1128-1207`, `device_id_service.dart:5-16` | session حساس؛ location/device pseudonymous | credential در Keystore؛ location snapshot در Room یا encrypted Proto با user scope/TTL؛ device ID با privacy policy و re-registration |
| E2EE/secret chat | `e2e_peer_pub_{conversation}` و per-conversation secret-auto-delete keyها؛ `modern_chat_screen.dart:689-1020`, `:1478-1517` | security/conversation-scoped | Room composite-key tables؛ private material فقط Keystore؛ import باید conversation/user را verify کند |
| Logout semantics | `prefs.clear()` و secure/cache wipe؛ `secure_logout_service.dart:13-57` | retention boundary | Native wipe matrix باید account data، device-wide prefs و user-owned downloads را از هم جدا کند؛ blanket delete مجاز نیست |

### 15.3 Transaction، order، rollback و missing dependency

| Data | Encryption / user binding / version / lifetime | Import order و transaction boundary | failure / rollback / verification | Missing dependency |
|---|---|---|---|---|
| Session | secure storage؛ user از token/user key؛ version صریح ندارد؛ تا logout/revoke | 1) read بدون delete، 2) decode+validate schema، 3) Backend validate/refresh، 4) commit encrypted target، 5) mark imported، 6) source delete در release بعد | هر خطا source را نگه دارد و Native به Auth fallback کند؛ verify user ID/expiry/session endpoint؛ re-run idempotent | package/signature strategy و bridge زیر sandbox قدیمی |
| E2EE | private/local keys حساس؛ per-user/per-conversation؛ چند naming/version legacy؛ عمر حساب/conversation | بعد از Session identity؛ copy به staging، decrypt known fixture/message، commit key+metadata اتمیک، سپس marker؛ source تا verification باقی | wrong user/key/envelope → quarantine و no delete؛ rollback به source؛ verify known-answer decrypt و round-trip | canonical envelope spec، key provenance و cross-client vectors |
| Draft | plaintext memory/file references؛ user/conversation binding ضمنی؛ version/lifetime process؛ persistence ندارد | import transaction ندارد؛ importer باید نبود source را با نتیجه‌ی موفق صفر رکورد ثبت کند. persistence جدید Native در `MEDIA-01` مستقل از migration ساخته می‌شود. | no-source → zero-import موفق؛ verify imported count=`0` و نبود synthetic draft | ندارد؛ policy `zero-import` قفل شده است |
| Pending | Isar rows؛ بعضی payloadها ممکن است حساس باشند؛ owner conversation/user؛ version payload صریح نیست؛ تا success/max retry | بعد از Session/E2EE؛ freeze producer، copy known types با idempotency key در یک Room transaction، enqueue WorkManager پس از commit | unknown type/version quarantine؛ crash-safe marker؛ rollback target rows بدون حذف Isar؛ verify count/status/idempotency | payload version، global owner contract، producer freeze |
| Download | فایل bytes غالباً plaintext؛ user binding نامشخص؛ music mapping in-memory؛ lifetime user-owned یا cache | user-owned scan/manifest commit بعد از Session؛ cache skip. bridge جدید باید هنگام download، URL/entity/content hash را اتمیک کنار file completion ثبت کند | orphan/partial/duplicate به review state؛ raw path move نشود؛ verify URI open + size/hash؛ rollback فقط manifest target، نه user file | legacy trustworthy manifest، MediaStore/SAF URI grants |
| Preferences | اغلب plaintext scalar/JSON؛ بعضی per-user؛ version پراکنده؛ تا logout/app lifetime | بعد Session؛ map allowlisted keyها به staged Proto/Room، validate defaults، atomic swap؛ server-owned settings سپس refresh | unknown key ignore+count؛ parse failure default بدون حذف source؛ verify old/new semantic snapshot؛ rollback target version | canonical schema/default/retention table |

### 15.4 Draft و Download definitive consequences

- با process death در Post composer، متن، image/gallery selection، video/music
  reference، trim start/end، background mode و mention-resolution context از بین
  می‌رود؛ `AddPost.dart:45-107`, `:656-713`. Upload task/progress/retry params
  نیز با process می‌میرند؛ `post_upload_provider.dart:19-95`, `:273-310`.
- chat unsent controller text، reply/edit context و `_preEditDraft` process-local
  است؛ تنها share handoff از route args می‌آید. پس bridge فعلی برای هیچ Draft
  قابل‌اعتماد ممکن نیست.
- policy مهاجرت Draft برای نسخه موجود به‌طور قطعی `zero-import` است. Native باید
  در `MEDIA-01` Draft پایدار جدید بسازد، اما تولید bridge یا حدس‌زدن draft برای
  داده‌ای که source پایدار ندارد ممنوع است.
- Music file در public Download user-owned تلقی می‌شود؛ `Download/Vista/temp`
  و Default/voice/story caches قابل‌بازسازی‌اند. basename فایل music تنها
  mapping قابل‌مشاهده است و URL/entity قابل‌اعتماد از آن بازیابی نمی‌شود.
- bridge آینده باید manifest را هنگام rename از `.partial` به فایل نهایی commit
  کند. برای legacy فقط filesystem/MediaStore scan ممکن است و رکورد باید
  `unmatched` بماند؛ حدس URL از filename ممنوع.
- raw filesystem path بین Flutter و Native contract پایدار نیست؛ target باید
  `content://` MediaStore/SAF URI یا app-private relative ID ذخیره کند و
  permission/ownership را verify کند.

## 16. Performance baseline

### 16.1 محیط runtime مشترک

| مورد | شاهد |
|---|---|
| AVD | `Medium_Phone_2`، Android 13 / API 33، `x86_64`، `1080×2400` |
| ایزوله‌سازی | emulator با `-read-only -no-snapshot-save` اجرا شد؛ تغییر app data در snapshot ذخیره نشد. |
| Flutter artifact | APK اولیه فقط `arm64-v8a` بود و روی AVD با `MissingLibraryException: libflutter.so` crash کرد. build `android-x64 --no-pub` پس از 184s در wrapper timeout شد، اما artifact جدید ایجاد شد و `tar -tf` وجود `lib/x86_64/libflutter.so` را تأیید کرد؛ نصب موفق بود. |
| Build mode | هر دو APK `debug`؛ این اعداد baseline تشخیصی‌اند، نه release SLA. |
| Source integrity | پس از build/runtime، Git status Flutter همان baseline dirty قبلی ماند؛ هیچ source تازه‌ای تغییر نکرد. Native Git repository ندارد. |

### 16.2 Flutter traversal و screenshot baseline

VF runtime:

- onboarding slide، location prompt و Auth در state بدون session ثبت شد؛
- session موجود AVD بعد از startup settle شد و هر پنج tab واقعی Feed، Search،
  Services، Chat inbox و Profile پیمایش شدند؛
- Back از Profile به Feed برگشت، مطابق
  `homeScreen.dart:386-409`;
- screenshotهای Light برای پنج tab، Auth، onboarding و prompt؛ Dark برای Home
  و Auth؛ و Dark + font scale 130% برای Services ثبت شد؛
- screenshot Dark Home نشان داد label دکمه‌های Follow روی pill آبی دیده
  نمی‌شود؛ این یک visual finding است، نه اصلاح‌شده در AUD-01.

Evidence directory:
`docs/audits/evidence/2026-07-25-aud-01-gate-closure/`.

نمونه‌های کلیدی:

- `flutter-home-feed-api33-1080x2400.png`
- `flutter-tab-search-api33.png`
- `flutter-tab-services-api33.png`
- `flutter-tab-chat-api33.png`
- `flutter-tab-profile-api33.png`
- `flutter-chat-detail-api33.png`
- `flutter-settings-api33.png`
- `flutter-warm-deeplink-post-logged-out-api33.png`
- `flutter-back-profile-to-feed-api33.png`
- `flutter-home-dark-api33.png`
- `flutter-services-dark-font130-api33.png`

### 16.3 Flutter runtime numbers

| سناریو | sample | median | p95 / max sample | محدودیت |
|---|---:|---:|---:|---|
| logged-out cold `am start -W` | 5 | 4,581ms | 4,992ms | Auth/splash debug |
| logged-out warm resume | 5 | 191ms | 226ms | debug/x86_64 |
| authenticated cold Activity total | 5 | 5,079ms | 8,634ms | first run coldest |
| authenticated meaningful-semantics checkpoint | 5 | 11,910ms | 17,312ms | upper-bound؛ `uiautomator` polling overhead و network در آن هست |
| authenticated warm resume | 5 | 330ms | 494ms | debug/x86_64 |
| Chat Inbox semantics checkpoint | 5 | 2,192ms | 2,279ms | upper-bound؛ شامل `uiautomator dump` |
| Chat Detail semantics checkpoint | 5 | 3,405ms | 3,883ms | upper-bound؛ group موجود، شامل `uiautomator dump` |

پس از Feed traversal، `dumpsys meminfo` برابر `TOTAL PSS=686,450KB` و
`TOTAL RSS=896,192KB` بود. در logged-out checkpoint مقدارها به‌ترتیب
`580,624KB` و `792,236KB` بودند. این اعداد emulator/debug و برای trend هستند.

MI — در Feed و Chat list پس از reset و swipe، `dumpsys gfxinfo` برای Flutter
Impeller مقدار `Total frames rendered: 0` برگرداند؛ بنابراین frame
p50/p95/p99 و jank از این ابزار قابل‌اعتبار نبود. DevTools/Perfetto در
profile/release روی ARM و حداقل یک دستگاه ضعیف API 24/26 هنوز لازم است.
Story/editor، media send و network request count نیز baseline عددی ندارند.

### 16.4 Native runtime/device و instrumentation

| سناریو | نتیجه |
|---|---|
| authenticated cold start | 5 run؛ median `2,129ms`، p95/max sample `2,193ms` |
| warm resume | 5 run؛ median WaitTime `25ms`، p95/max `32ms` |
| memory | `TOTAL PSS=74,745KB`، `TOTAL RSS=157,588KB` |
| small gfx sample | 71 frame؛ jank `16.90%`، p50 `18ms`، p90 `32ms`، p95 `34ms`، p99 `1700ms`؛ launch/transient sample، نه benchmark |
| process restart | `force-stop` + cold start، encrypted session snapshot به authenticated placeholder بازگشت |
| offline | Wi-Fi/data خاموش، active network none؛ cold start `2,261ms` و session `ahmad` با offline fallback حفظ شد؛ network سپس restore شد |
| rotation | portrait `1080×2400` به landscape `2400×1080`؛ authenticated state حفظ شد و تنظیم rotation restore شد |
| clean state | پس از reinstall/clear موقت روی AVD read-only، onboarding و Auth واقعی ثبت شد؛ Back از Auth app را بست |
| theme/font | Auth در Dark + font scale 130% ثبت و system settings restore شد |

فرمان baseline unit/lint قبلی همچنان معتبر است:

```powershell
.\gradlew.bat test lint --no-daemon
```

VF: `BUILD SUCCESSFUL` در 55s؛ 40 unit-test execution با صفر
failure/error/skipped و lint با صفر error و 23 warning.

فرمان device:

```powershell
$env:ANDROID_SERIAL='emulator-5556'
.\gradlew.bat connectedDebugAndroidTest --no-daemon
```

VF: `BUILD SUCCESSFUL` در 1m35s؛ 71 task؛ یک test روی
`Medium_Phone_2(AVD) - 13`، `tests=1`, `failures=0`, `errors=0`, `skipped=0`.
اما test فقط package context را assert می‌کند:
`app/src/androidTest/java/ir/coffevista/vista_native/ExampleInstrumentedTest.kt:16-23`.
پس instrumentation **اجرا شده** ولی feature instrumentation coverage هنوز MI است.

Blockerهای دقیق Native:

- invalid session به fixture با refresh token منقضی/لغوشده یا injectable fake
  backend نیاز دارد؛ دست‌کاری ciphertext AES-GCM evidence کنترل‌شده محسوب نمی‌شود.
- maintenance فقط از `maintenanceMode()` server response می‌آید؛
  `StartupResolver.kt:16-20`. هیچ local toggle/test fixture وجود ندارد و تغییر
  backend خارج از AUD-01 است.
- macrobenchmark/baseline profile وجود ندارد.

## 17. طبقه‌بندی تمام کد Native

| فایل/گروه | Class | دلیل و شاهد |
|---|---|---|
| `MainActivity.kt` | Retain | Single Activity، edge-to-edge، RTL؛ `:17-30` |
| `VistaApplication.kt` | Refactor | container دستی؛ هدف بعدی DI، ولی در AUD تغییر ممنوع؛ `:6-9` |
| `navigation/AppNavGraph.kt` | Refactor | 5 route string-based و popUpTo درست؛ shell/type-safe/deeplink ندارد؛ `:25-40`, `:54-150` |
| `features/startup/StartupModels.kt` | Retain | explicit destination/state؛ `:3-21` |
| `StartupResolver.kt` | Refactor | refresh/offline logic خوب؛ maintenance network failure semantics نیازمند contract test؛ `:16-63` |
| `StartupViewModel.kt` | Retain | immutable StateFlow + retry؛ `:11-29` |
| `StartupScreen.kt`, `MaintenanceScreen.kt` | Refactor | functional states؛ visual parity/runtime absent |
| `AuthenticatedBoundaryScreen.kt` | Replace | فقط welcome placeholder؛ `:17-42` |
| `core/auth/AuthModels.kt` | Retain | focused transport models |
| `AuthRemoteDataSource.kt`, `AuthRepository.kt` | Retain | interface boundaries |
| `DefaultAuthRepository.kt` | Retain | outcome/error boundary |
| `OkHttpAuthRemoteDataSource.kt` | Refactor | auth/status + flat/nested error؛ pinning/refresh/session registration absent؛ `:23-112`, `:130-146` |
| `AuthenticationStateOwner.kt` | Retain | process auth state boundary |
| `core/common/Outcome.kt`, `core/network/*` | Retain | typed error/result foundation |
| `core/storage/SessionStore.kt` | Refactor | AES-GCM/Keystore good؛ migration/import/session metadata absent؛ `:34-147` |
| `core/storage/OnboardingStore.kt` | Retain | versioned completion |
| `core/validation/AuthValidation.kt` | Retain | Persian digit/phone/password rules؛ tests exist |
| `core/di/AppContainer.kt` | Refactor | single client/manual graph؛ `:16-35` |
| `features/auth/AuthViewModel.kt` | Refactor | flow/tests موجود؛ recovery/biometric/session registration absent |
| `features/auth/AuthScreen.kt` | Refactor | functional RTL auth، screenshot/a11y parity absent |
| `features/onboarding/OnboardingViewModel.kt` | Retain | small state owner/tested |
| `features/onboarding/OnboardingScreen.kt` | Refactor | functional local assets؛ parity/runtime unverified |
| `ui/components/VistaBrand.kt` | Retain | reusable local brand assets |
| `ui/theme/VistaTheme.kt` | Refactor | centralized theme؛ full Flutter token/component parity absent |
| `VistaApplication.kt`/manual factories | Merge | در FND-01 باید در target DI merge شوند؛ اکنون دست‌نخورده |
| 5 substantive unit test files | Retain | startup/auth/onboarding/validation/error tests |
| `ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt` | Remove later | template tests؛ حذف در این فاز ممنوع |

Resource classification: Vazirmatn و Vista image assets Retain؛ launcher resources
Refactor after identity decision؛ template purple/teal colors Remove later
(lint unused)؛ manifest/build files Refactor در GOV/FND، نه AUD.

## 18. Dynamic / external URL inventory closure

literal host scan روی Dartهای First-party، comment/example را نیز می‌بیند؛
بنابراین هر مورد با owner و runtime disposition تفکیک شد:

| Family | Host/source | Owner و policy فعلی | Native disposition |
|---|---|---|---|
| First-party API/WS | `api.coffevista.ir` / `wss://api.coffevista.ir`؛ `env_config.dart:18-64` | compile-time override + production fallback؛ Dio factory روی API، OS trust + admin pin policy؛ `http_client_factory.dart:25-95` | یک typed environment config؛ HTTPS/WSS اجباری؛ pin/status policy جداگانه تست شود |
| First-party web/deep/share | `cafevista.ir`, `vista.me` و legacy `coffevista.ir`/`www.*`؛ `deep_link_service.dart:127-167`, `smart_share_service.dart:18-90` | share/deep link/payment callback/game web | canonical host allowlist؛ alias redirect contract؛ App Link verification برای هر host |
| Dynamic Services Hub | banner/section URL از Backend مستقیم به `InAppWebScreen` می‌رود؛ `services_screen.dart:132-168` | URLهای `http` و `https` پذیرفته می‌شوند؛ WebView JavaScript unrestricted است و وقتی `restrictHost=null` تمام navigationها مجازند؛ `in_app_web_screen.dart:55-117` | **High-risk policy gap:** فقط HTTPS؛ allowlist per service؛ arbitrary URL در external browser؛ JS حداقلی؛ no auth token/cookie برای untrusted host |
| Game SSO | `$webUrl/game/sso?ticket=...`؛ `game_launch_screen.dart:63-92` | cookie clear + exact host و `/game` prefix restriction | retain concept؛ one-time short-lived ticket، exact host/path، redirect/expiry/replay tests |
| Weather/location | `api.open-meteo.com`, `nominatim.openstreetmap.org`, `ipwho.is`؛ `weather_service.dart:7-15`, `geocoder_service.dart:38-43`, `:139` | direct third-party HTTPS؛ location/privacy data | typed adapters، timeout/rate limit، user consent، no auth header، privacy disclosure؛ OS trust |
| GIF | `g.tenor.com/v1` و key literal؛ `gif_service.dart:11-45` | direct HTTP client؛ API key در source | vendor contract/key ownership تعیین شود؛ key از managed config/backend proxy؛ safe-search/privacy tests |
| Payment/distribution | `gateway.zibal.ir`, callback `cafevista.ir`, `zarinp.al`, `cafebazaar.ir`, `bazaar://`؛ `payment_service.dart:114-132`, `BazaarService.dart:12-93` | external app/browser؛ premium فقط پس از server verify | explicit provider allowlist، verified callback، scheme fallback، no client-only entitlement |
| User/content links | Story link، bio/rich text، Maps و arbitrary `http(s)`؛ `story_player_screen.dart:1360-1421`, `rich_text_parser.dart:131-135` | external browser؛ user-controlled | normalize HTTPS، confirmation برای unknown host، block dangerous schemes، no WebView credential sharing |
| Media/presigned/object | dynamic media URL، presigned PUT/GET، S3-derived avatar URL؛ `backend_upload_service.dart`, `avatar_asset_utils.dart:35-44` | multiple direct `http` clients؛ API pin policy لزوماً اعمال نمی‌شود | typed `MediaUri` + expiry/mime/size/host policy؛ presigned host allowlist؛ cache/download owner صریح |
| Non-runtime literals | `example.com` hint، `github.com` comment و `api.vista.app` build-doc example | runtime endpoint نیستند | از production allowlist حذف شوند |

نتیجه: dynamic/external inventory از حالت «عدد 176 آن‌ها را نمی‌بیند» خارج شد
و disposition دارد؛ ولی WebView allowlist و external-client policy نیازمند تغییر
کد در `FND-01`/`DISC-01` است و در AUD-01 اصلاح نشد. وضعیت Audit این finding
`Closed/Transferred` و وضعیت Implementation آن `Open` است.

## 19. Deep-link route و cold-start closure

### 19.1 سه‌لایه‌ی static

| Link family | Android manifest | Dispatcher | Flutter route/final destination | defect / owner |
|---|---|---|---|---|
| `vista://auth/...` | resolve؛ host در manifest `:94` | branch ندارد؛ unsupported | ندارد | manifest/dispatcher mismatch؛ `FND-01` |
| `vista://post/{id}` | resolve | `deep_link_service.dart:81-84` | `/post-detail` registered؛ `app_runner.dart:671-680` | warm کار می‌کند؛ cold queue ندارد |
| `vista://profile/{id}` | resolve | `:85-89` | `/profile` registered؛ `app_runner.dart:696-717` | missing arg → no navigation؛ guard policy صریح نیست |
| `vista://group/{code}` | resolve | `:77-80` | async invite → `/chat` | account/network dependent؛ cold queue ندارد |
| `vista://chat/{id}` | **manifest ندارد** | `/chat-detail`؛ `:90-96` | route واقعی `/chat` است؛ `app_runner.dart:721+` | دو defect مستقل؛ `FND-01` |
| `vista://appeal/{post}` | manifest ندارد | `/appeal`؛ `:97-104` | registered؛ `app_runner.dart:681-695` | cold هرگز resolve نمی‌شود |
| `vista://notifications` | manifest ندارد | `/home`؛ `:105-108` | registered | target notification screen/action مشخص نیست |
| `vista://nearby[/likes|matches]` | manifest ندارد | `/nearby` یا direct screen؛ `:109-120` | registered | cold هرگز resolve نمی‌شود |
| `https://{VistaHost}/post|profile|group|feed|payment` | manifest فقط `cafevista.ir` و `vista.me`؛ `AndroidManifest.xml:79-88` | dispatcher پنج alias را می‌پذیرد؛ `deep_link_service.dart:127-164` | routeهای متناظر | `www.*`/`coffevista.ir` parser-only؛ host/path declaration و App Link verify ناقص |
| `https://vista.me/chat/...` | Android resolve می‌کند | path `chat` unsupported | مقصد ندارد | manifest بیش از dispatcher؛ `FND-01` |

### 19.2 Runtime matrix روی API 33

`cmd package query-activities`:

- resolve: `vista://auth`, `post`, `profile`, `group`;
- no activity: `vista://chat`, `appeal`, `notifications`, `nearby`;
- `https://cafevista.ir/post/...` و `https://vista.me/chat/...` به
  `MainActivity` resolve شدند.

نتیجه‌ی stateها:

- **Cold + root Auth:** `vista://post/audit-fixture` Activity را در `5,138ms`
  باز کرد، ولی UI روی Login ماند و مقصد pending نشد.
- **Warm + root Auth:** همان URI با `WaitTime=6ms` به instance موجود تحویل شد
  و Post Detail/error state را باز کرد؛ screenshot
  `flutter-warm-deeplink-post-logged-out-api33.png`.
- **Cold chat:** Android با `unable to resolve Intent` رد کرد.
- dispatcher برای cold link فقط null-aware `navigatorKey.currentState?.push...`
  دارد و queue/replay ندارد؛ `deep_link_service.dart:45-50`, `:171-175`.
- `SessionMiddleware` auth را validate نمی‌کند؛ فقط Back route اول را به Home
  می‌برد؛ `middleware/session_middleware.dart:21-48`. public/private بودن هر
  destination باید contract صریح داشته باشد.
- missing/invalid argument در dispatcher عمدتاً log/no-op است؛ route
  registration در بعضی مقصدها `InvalidRouteScreen` برمی‌گرداند. failure UX
  یکنواخت نیست.

**Implementation state: FAIL؛ Audit disposition: Closed/Transferred.** رفع
manifest، `/chat-detail`، pending-intent queue، post-login replay و guard
contract تغییر اجرایی است و مالک آن `FND-01` است.

Acceptance suite لازم: هر URI در warm/cold × logged-out/logged-in ×
valid/missing ID × supported/unsupported host، assertion مقصد، replay بعد Login،
Back و single-delivery.

## 20. Isar encryption fallback security assessment

### 20.1 مسیرهای فعال‌شدن

1. `_getEncryptionKey` هر exception از secure storage، base64 decode، random
   generation یا write را swallow می‌کند و `null` می‌دهد؛
   `isar_database_manager.dart:252-276`.
2. اگر DB از قبل وجود دارد و flag برابر `true` نیست، عمداً بدون key باز می‌شود
   تا DB legacy plaintext خراب نشود؛ `:261-266`.
3. `_openIsarOnce` فقط در `NoSuchMethodError` یا `ArgumentError`، argument
   رمزنگاری را حذف و دوباره `Isar.open` می‌کند؛ `:235-247`.
4. `IsarError` ناشی از wrong/missing key fallback plaintext ندارد و rethrow
   می‌شود؛ `:226-234`.
5. retry path فقط lock/resource errorهای مشخص را retry می‌کند و در پایان lock
   file را best-effort پاک می‌کند؛ `:150-203`.

### 20.2 اثر امنیتی/داده

- DB رمزگذاری‌شده‌ی موجود با wrong/missing key از این کد «موفق» بدون key باز
  نمی‌شود؛ انتظار، failure availability است و DB در این مسیر delete نمی‌شود.
- اما در install جدید، اگر secure key read/write fail کند، `null` می‌تواند
  باعث ساخت DB plaintext بدون signal شود.
- در API/signature mismatch نیز حذف `encryptionKey` می‌تواند DB جدید/legacy را
  plaintext باز کند، درحالی‌که flag/key secure ممکن است تصور «encrypted» بدهد.
- DB legacy plaintext عمداً plaintext باقی می‌ماند و migration اتمیک برای
  re-encryption ندارد.
- داده‌های در معرض شامل Message/Conversation، Profile/Post cache، RecentSearch،
  AppSettings، RetryQueue و DeletionTask است؛ schemaها در `:205-215`.
- data loss مستقیم در fallback کد دیده نشد، اما wrong-key outage، fork شدن
  state به DB تازه در رفتار library/نام‌گذاری آینده و plaintext write ریسک‌های
  migration هستند.

**طبقه‌بندی:** High-risk Security Implementation Blocker + Migration
Precondition؛ `Closed/Transferred` در Audit و `Open` در Implementation.
exposure قطعی تولید اثبات نشده، اما fail-open plaintext path در source موجود
است. اصلاح متعلق به `FND-01`/Security Foundation و پیش‌شرط هر bridge یا import
داده واقعی است.

حداقل طراحی اصلاح: fail closed؛ metadata versioned و مستقل از key؛ plaintext
legacy detection صریح؛ export→encrypted-import اتمیک با backup/rollback؛ هرگز
swallow کردن secure-storage failure؛ health telemetry بدون secret.

Regression tests اجباری:

- new install + key success و اثبات عدم open بدون key؛
- new install + secure-store read/write failure → no DB created؛
- existing plaintext upgrade اتمیک و rollback؛
- existing encrypted + missing/wrong key → preserve DB، no plaintext fallback؛
- `NoSuchMethodError`/`ArgumentError` injection → fail closed؛
- crash در میانه copy/swap، key rotation، logout، backup/restore و stale-lock retry.

## 21. Implementation Remediation Backlog — انتقال کنترل‌شده

در این جدول، `Closed/Transferred` فقط یعنی finding در Audit با مالک و معیار
خروج بسته شده است؛ به معنی اصلاح رفتار اجرایی نیست.

| ID / finding | Evidence قابل‌بازتولید | Severity / risk | Owner phase | Remediation scope | Acceptance test | Exit gate | Status |
|---|---|---|---|---|---|---|---|
| `FND-DL-01` — parser/manifest/route/cold replay | جدول 19.1؛ runtime 19.2؛ `deep_link_service.dart:45-50,77-175`، `AndroidManifest.xml:79-97`، `app_runner.dart:671-721` | High؛ route loss/incorrect destination | `FND-01` | canonical URI contract، manifest parity، route registry، pending queue، post-login replay و guard | تمام URIها در warm/cold × logged-in/out × valid/missing args × host supported/unsupported؛ مقصد، single delivery، Back و replay assert شود | Navigation Foundation فقط با سبز شدن suite و صفر unresolved supported URI پذیرفته شود | `Closed/Transferred — Implementation Open` |
| `FND-SEC-01` — Isar fail-open | بخش 20؛ `isar_database_manager.dart:150-276` و schemaهای `:205-215` | **High Security**؛ plaintext creation/migration integrity | `FND-01` / Security Foundation | fail closed، metadata versioned، legacy detection و re-encryption اتمیک با rollback | تمام fault-injectionهای بخش 20: secure-store failure، wrong/missing key، API mismatch، crash mid-swap، rotation و restore | پیش از هر Migration Bridge/import واقعی، همه fault tests پاس و plaintext fallback غیرممکن باشد | `Closed/Transferred — Implementation Open; Import Blocked` |
| `FND-WEB-01` — WebView allowlist | بخش 18؛ `services_screen.dart:132-168` و `in_app_web_screen.dart:55-117` | High؛ untrusted navigation/JS boundary | `FND-01` + `DISC-01` | HTTPS-only، allowlist host/path per service، external-browser disposition، JS/cookie/token isolation | allowlisted navigation pass؛ subdomain/redirect/scheme bypass و token/cookie leakage tests fail-safe؛ unknown URL در external browser | Security Foundation و Services discovery تا سبز شدن policy suite `Verified` نشوند | `Closed/Transferred — Implementation Open` |
| `FND-AUTH-01` — maintenance/invalid session fixtures | runtime بخش 16.4 و branchهای `StartupResolver.kt:16-20` | High؛ startup/auth recovery اثبات‌نشده | `FND-01` | fake backend یا injectable repository برای maintenance، 401، revoked/expired refresh و transient failure | deterministic instrumentation برای هر state؛ مقصد، پیام، retry، wipe/preserve session و recovery assert شود | Startup/Auth Foundation فقط با پاس تمام state matrix پذیرفته شود | `Closed/Transferred — Implementation Open` |
| `FND-QA-01` — feature instrumentation و Macrobenchmark | `connectedDebugAndroidTest` واقعی 1/1 پاس، اما فقط `ExampleInstrumentedTest.kt:16-23` | Medium؛ regression/performance assurance ناکافی | Foundation feature owners + `PERF-01` | Compose navigation/auth/startup tests، deterministic fixtures، Macrobenchmark و baseline-profile harness | instrumentation روی API هدف برای startup/auth/navigation و Macrobenchmark startup/scroll؛ صفر failure و artifact قابل‌بازتولید | هر Foundation feature با suite اختصاصی و Performance Gate با Macrobenchmarkهای مصوب `Verified` شود | `Closed/Transferred — Implementation Open` |
| `PERF-01` — ARM profile/release baseline | بخش 16.3؛ x86 debug و `gfxinfo=0 frame` برای Flutter/Impeller؛ startup/memory diagnostic موجود | Medium/Release؛ ادعای performance بدون baseline معتبر | `PERF-01` | ARM device ضعیف، profile/release، DevTools/Perfetto و سناریوهای Feed/Chat/Story | حداقل ۵ run؛ startup، memory و frame p50/p95/p99/jank با build/device metadata | پیش از هر parity-performance claim و Release Gate baseline ثبت و thresholdهای `PERF-01` پاس شود؛ مانع `GOV-01` نیست | `Closed/Transferred — Implementation Open` |
| `MEDIA-DRAFT-01` — persistence جدید Draft | بخش 15.1/15.4؛ `AddPost.dart:45-107,766-771` و `post_upload_provider.dart:19-95,273-310` | Medium؛ UX/data loss آینده، بدون legacy import risk | `MEDIA-01` | Native persistent draft با user/destination scope؛ **legacy policy=`zero-import`** | migration count=`0`؛ process-death restore، attachment permission expiry، isolation و delete tests | MEDIA Draft فقط پس از پاس persistence tests `Verified` شود؛ هیچ legacy importer لازم نیست | `Closed/Transferred — Implementation Open` |
| `MIG-DL-01` — legacy Download discovery/manifest | بخش 15.1/15.4؛ `MusicDownloadManager.dart:32-49,95-135,189-196` و `file_manager_service.dart:12-97` | High Migration Integrity؛ false association/data ownership | `MIG-01` | legacy MediaStore/filesystem scan؛ target manifest؛ future completion-time manifest bridge؛ بدون URL/entity guessing | duplicate/partial/hash/permission/uninstall/delete cases؛ فایل نامطمئن `unmatched` بماند؛ bytes جابه‌جا/حذف نشود | MIG Download فقط با scan idempotent، URI-open verification و صفر inferred URL/entity پذیرفته شود | `Closed/Transferred — Implementation Open` |
| `FEAT-VIS-01` — Story و fixture screenshot states | بخش 16.2؛ ۲۴ artifact موجود و نبود active Story/seeded states | Medium Evidence؛ visual parity ناقص | Story/Chat/permission feature phases | seeded deterministic fixtures و screenshot matrix Light/Dark/font/empty/error/permission | golden/manual parity برای Story و stateهای منتقل‌شده روی device هدف | همان Feature تا تکمیل screenshot matrix نباید `Verified` شود | `Closed/Transferred — Implementation Open` |
| `GOV-PKG-01` — cross-package import/signing | بخش 15.3؛ sandbox بین `ir.coffevista.vista` و package Native، bridge موجود نیست | High Governance/Migration dependency | `GOV-01` | package/signing/release channel و دسترسی bridge تعیین شود | نصب/upgrade روی artifact امضاشده؛ source read، rollback و downgrade policy اثبات شود | قبل از طراحی importerهای Session/E2EE/Pending تصمیم مصوب و قابل‌تست ثبت شود | `Owned Governance Dependency` |

## 22. Gate model و Decision Log

### 22.1 Audit Closure Gate

| معیار | نتیجه | شاهد |
|---|---|---|
| finding با شاهد قابل‌بازتولید کشف شده | **Pass** | ledgerهای 2 تا 20، فرمان‌ها، runtime matrix و evidence artifactها |
| Root cause یا محدوده Root cause ثبت شده | **Pass** | مسیرهای `path:line` و scope هر ردیف جدول 21 |
| Severity و risk مشخص شده | **Pass** | ستون Severity/Risk جدول 21 و ledgerهای feature/security |
| فاز مالک اصلاح مشخص شده | **Pass** | ستون Owner Phase؛ هیچ finding بحرانی بدون owner نیست |
| Acceptance Test و Exit Gate تعریف شده | **Pass** | دو ستون مستقل جدول 21 |
| Unknown بحرانی بدون مالک باقی نمانده | **Pass** | موارد اجرا‌نشده به backlog مالک‌دار منتقل شده‌اند؛ MIهای Draft/Download با policy قطعی بسته شده‌اند |
| ممنوعیت تغییر executable source رعایت شده | **Pass** | فقط docs/evidence/build/test outputs؛ هیچ remediation در AUD اجرا نشد |

**نتیجه:** `AUD-01 Complete — Audit Closure Gate Passed`

### 22.2 Implementation Remediation Gate

این Gate هنوز پاس نشده و در `AUD-01` قرار نیست پاس شود. اصلاح executable،
تست‌های remediation و اثبات Exit Gate هر ردیف در فاز مالک انجام می‌شود.

**وضعیت:** `Implementation Remediation Backlog Open`

### 22.3 Decision Log — Locked

1. **Draft migration:** Legacy Flutter persistent Draft source ندارد؛ policy
   برابر `zero-import` است. Native در `MEDIA-01` Draft پایدار جدید می‌سازد،
   اما از نسخه موجود داده‌ای import نمی‌شود.
2. **Legacy Download:** بدون manifest فقط MediaStore/filesystem scan مجاز است.
   حدس URL یا entity از filename ممنوع و رکورد نامطمئن باید `unmatched` بماند.
3. **Performance evidence:** Emulator/debug فعلی فقط diagnostic است. ARM
   profile/release پیش از performance claim و Release Gate اجباری است، ولی
   مانع `GOV-01` نیست.
4. **Isar:** fail-open یک High-risk implementation blocker است. ورود به هر
   Migration Bridge یا import داده واقعی پیش از fail-closed remediation و
   fault-injection suite ممنوع است.
5. **Deep Link:** defectها Foundation blocker هستند و پیش از پذیرش Navigation
   Foundation باید Exit Gate ردیف `FND-DL-01` را پاس کنند.
6. **Screenshot fixtures:** Story و stateهای fixtureمحور به فاز Feature مالک
   منتقل شدند و پیش از `Verified` شدن همان Feature اجباری‌اند.

### 22.4 مجوز عبور فازی

- ورود به `GOV-01` مجاز است.
- ورود به `FND-01` فقط پس از واردکردن `FND-DL-01`، `FND-SEC-01`,
  `FND-WEB-01`, `FND-AUTH-01` و `FND-QA-01` در Scope و Acceptance Gate آن
  مجاز است.
- این مجوز به معنی رفع findingها یا Pass شدن Implementation Remediation Gate
  نیست.

## 23. خروجی نهایی یازده‌بندی

1. **موارد باز قبلی:** ۱۲ محور اجباری بررسی و چرخه وابستگی میان Audit و
   executable remediation شناسایی شد.
2. **موارد بسته‌شده:** همه findingها evidence، root-cause scope، severity،
   owner phase، remediation scope، acceptance test و exit gate دارند.
3. **Runtime evidence:** Flutter و Native روی AVD API 33 read-only اجرا شدند؛
   stateهای Auth/authenticated/offline/rotation/back و cold/warm پوشش یافت.
4. **Screenshot evidence:** ۲۴ artifact زمان‌دار زیر
   `docs/audits/evidence/2026-07-25-aud-01-gate-closure/` ثبت شد؛ Story و چند
   fixture state به Featureهای مالک منتقل و پیش از `Verified` شدن آن‌ها اجباری شد.
5. **Performance evidence:** Flutter startup/warm/meaningful checkpoint/memory
   و Native startup/warm/memory/gfx sample ثبت شد؛ ARM profile/release به
   `PERF-01` منتقل شد و مانع `GOV-01` نیست.
6. **Migration findings:** Draft policy برابر `zero-import` و Download legacy
   برابر `scan + unmatched` بدون URL/entity guessing قفل شد؛ سایر mappingها کامل‌اند.
7. **Deep-link findings:** manifest، dispatcher، route و cold replay ناسازگارند؛
   `/chat-detail` ثبت نشده و چند host resolve نمی‌شوند؛ finding به `FND-01`
   منتقل شد و پیش از Navigation Foundation باید بسته شود.
8. **Security findings:** Isar مسیر fail-open plaintext دارد؛ dynamic WebView
   بدون restrictHost و با unrestricted JS نیز blocker منتقل‌شده است؛ import
   واقعی پیش از اصلاح Isar ممنوع شد.
9. **Blockerهای باقی‌مانده:** همگی `Transferred implementation blocker` یا
   governance dependency هستند و در جدول 21 مالک و Exit Gate دارند.
10. **نتیجه نهایی Gate:** `AUD-01 Complete — Audit Closure Gate Passed`؛
    هم‌زمان `Implementation Remediation Backlog Open`.
11. **تأیید مرز فاز:** هیچ فاز اجرایی در این Task شروع نشد. ورود به `GOV-01`
    مجاز است؛ `FND-01` فقط با واردکردن تمام Foundation blockerها در Scope و
    Acceptance Gate آن مجاز است.
