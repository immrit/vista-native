# Flutter-to-Native 100% Parity Plan

**Type:** feature
**Status:** in progress
**Progress:** source work in progress; runtime acceptance pending
**Last Updated:** 2026-09-01
**Audit Baseline:** 46 features across 8 modules — Flutter `E:\vista` vs Native `E:\vista_native`

---

## Problem / Goal

The native Android Jetpack Compose app (`E:\vista_native`) is being brought to functional and visual parity with the Flutter reference app (`E:\vista`). A checklist item is only complete after a fresh APK is built, installed, and exercised on the emulator with a matching Flutter flow.

## Architecture Contracts

- Multi-module Gradle with typed Navigation Compose destinations in `AppNavGraph.kt`
- Hilt DI, `StateFlow` + `collectAsStateWithLifecycle()`, Room, OkHttp/Retrofit
- RTL-first layout (`LayoutDirection.Rtl`), Vazirmatn font, Persian strings matching Flutter ARB catalogs
- Backend error envelope: nested `{"error":{"code","message"}}` and flat `{"error":"code"}` with `messageFa`

---

## Parity Matrix — Full 46-Feature Audit

### Legend

| Icon | Meaning |
|:---:|:---|
| ✅ | Implemented and accepted on a fresh emulator APK |
| 🔧 | Source implementation exists; fresh build and emulator acceptance are pending |
| ⏸️ | Excluded because there is no corresponding Flutter feature |

---

### Module 1: Auth & Onboarding

| # | Feature | Flutter Reference | Native Status | Icon | Notes |
|:--:|:---|:---|:---|:---:|:---|
| 1 | Splash & API resolver | `app_initialization.dart` | `StartupResolver.kt` + `StartupScreen.kt` | ✅ | Verified |
| 2 | Maintenance & Banned screens | `MaintenanceScreen`, `BannedScreen` | `MaintenanceScreen.kt`, `BannedScreen.kt` | ✅ | Verified |
| 3 | Onboarding slides | `OnboardingScreen` (3 slides) | `OnboardingScreen.kt` | ✅ | Verified |
| 4 | Phone + SMS OTP login | `AuthWizardScreen` + `OtpDialog` | `AuthScreen.kt` (`OtpScreen`, `OtpBoxes`) | ✅ | Verified |
| 5 | Password set/login | `PasswordSetScreen`, `MandatoryPasswordScreen` | `PasswordScreen`, `PasswordSetupScreen` | ✅ | Verified |
| 6 | Biometric login screen | `BiometricLoginScreen` | `BiometricLoginScreen.kt` + route in `AppNavGraph.kt` | ✅ | Clean biometric authentication with fallback to password/OTP. |
| 7 | Profile Setup Wizard | `ProfileSetupWizardScreen` (`/profile-setup`) | `ProfileSetupWizardScreen.kt` + shared Jalali picker | ✅ | Clean Hilt ViewModel, shared Jalali date picker, avatar upload, and gating. |

---

### Module 2: Feed & Posts

| # | Feature | Flutter Reference | Native Status | Icon | Notes |
|:--:|:---|:---|:---|:---:|:---|
| 8 | Feed (For You / Following) | `ExploreFeedScreen` | `FeedScreen.kt` | ✅ | Dual tabs with smooth paging |
| 9 | Post card (double-tap like + music pill) | `post_action_buttons.dart`, `post_music_bubble.dart` | `FeedScreen.kt` | ✅ | Double-tap heart burst animation + `PostMusicBubble` with ExoPlayer preview. |
| 10 | Post options menu (report, edit, delete, appeal) | 3-dot menu + `AppealScreen` | `EditPostDialog.kt`, `ReportReasonDialog.kt`, `AppealScreen.kt` | ✅ | Full moderation flow with reason selection and submission. |
| 11 | Hashtag posts feed | `HashtagPostsScreen` (`/hashtag-posts`) | `HashtagPostsScreen.kt` | ✅ | Paginated hashtag posts with pull-to-refresh. |
| 12 | Post composer (Add Post) | `AddPost.dart` + `music_trim_sheet.dart` | `AddPostScreen.kt` + `VideoTrimmerScreen.kt` | 🔧 | انتخاب فایل صوتی، برش ۱۵/۶۰ ثانیه، آپلود امضاشده و ارسال بازهٔ برش افزوده شد. تست Gradle و امولاتور باز است. |
| 13 | Reels / fullscreen vertical video player | `reels_viewer_launcher.dart` | `ReelsViewerScreen.kt` | ✅ | Fullscreen `VerticalPager` with ExoPlayer, likes, comments, and music disk animation. |
| 14 | Post detail + comments | `PostDetailPage.dart` | `PostDetailScreen.kt` + `CommentsBottomSheet.kt` | ✅ | Verified |

---

### Module 3: Stories

| # | Feature | Flutter Reference | Native Status | Icon | Notes |
|:--:|:---|:---|:---|:---:|:---|
| 15 | Story tray above feed | `StoryTray` | `StoryTray.kt` | ✅ | Verified |
| 16 | Story player | `StoryPlayerScreen` | `StoryPlayerScreen.kt` | ✅ | Video/image story player with floating emoji quick-reactions bar and direct replies. |
| 17 | Story editor | `StoryEditorScreen` | `StoryEditorScreen.kt` | 🔧 | Duration, text, stickers, privacy settings، و دیالوگ دوستان نزدیک پیاده شده؛ پذیرش اجرا نشده است. |
| 18 | Close Friends list picker | `StoryPrivacySettingsScreen` | `StoryEditorScreen.kt` | 🔧 | خواندن فالوینگ‌ها و فهرست دوستان نزدیک، ذخیرهٔ واقعی و ارسال `allowed_user_ids` افزوده شد؛ پذیرش اجرا نشده است. |

---

### Module 4: Chat & Messaging

| # | Feature | Flutter Reference | Native Status | Icon | Notes |
|:--:|:---|:---|:---|:---:|:---|
| 19 | Conversation list | `ChatConversationsScreen` | `ConversationListRoute` in `ChatScreens.kt` | ✅ | Verified |
| 20 | Bubble physical position (BiDi) | RTL-aware bubbles | `ChatScreens.kt` | ✅ | Verified |
| 21 | Bubble corner grouping radii | Consecutive msg grouping <15min | `MessageGrouping.kt` | ✅ | Verified |
| 22 | Timestamp + delivery tick inline | Inline metadata flow | `MessageStatusMark` in `ChatScreens.kt` | ✅ | Animated delivery ticks (pending, sent, delivered, read). |
| 23 | Swipe to reply | Horizontal swipe + haptic | `SwipeToReplyLayout.kt` | ✅ | Verified |
| 24 | Message context menu | Long-press menu | `TelegramMessageContextMenu.kt` | ✅ | Full context menu with guards. |
| 25 | Voice recorder + player | Record dock + waveform player | `VoiceRecorderDock.kt` + `VoicePlayerBubble.kt` | ✅ | Verified |
| 26 | Media attachments in chat | Image, video, GIF, file, fullscreen viewer | `ChatAttachmentBottomSheet.kt` + `FullscreenMediaViewerDialog.kt` | ✅ | Verified |
| 27 | Group management | Create, edit, invite link, admin | `GroupDetailsSheet` + `NewMessageScreen.kt` | ✅ | Verified |
| 28 | E2EE encryption | Signal-based AES-GCM | `AndroidKeystoreChatContentCipher.kt` | ✅ | Verified |

---

### Module 5: Search & Discovery

| # | Feature | Flutter Reference | Native Status | Icon | Notes |
|:--:|:---|:---|:---|:---:|:---|
| 29 | Search (users, posts, history) | `searchPage.dart` | `SearchLauncherScreen` + `SearchWorkspaceScreen` | ✅ | Verified |
| 30 | Profile QR scanner | `qr_scanner_screen.dart` | `ProfileQrScannerScreen.kt` | ✅ | Verified |

---

### Module 6: Profile & Social Graph

| # | Feature | Flutter Reference | Native Status | Icon | Notes |
|:--:|:---|:---|:---|:---:|:---|
| 31 | Own/Other profile screen | `ProfileScreen.dart` | `OwnProfileScreen.kt` + `OtherUserProfileScreen.kt` | ✅ | Verified |
| 32 | Followers / Following list | `FollowersScreen`, `FollowingScreen` | `FollowersFollowingScreen.kt` | ✅ | Verified |
| 33 | Edit Profile | `editeProfile.dart` | `EditProfileScreen.kt` | 🔧 | Jalali picker باید روی APK تازه پذیرفته شود. مودال OTP شماره موبایل خارج از مرجع است؛ Flutter صریحاً تغییر شماره را غیرفعال می‌کند. |

---

### Module 7: Services Hub

| # | Feature | Flutter Reference | Native Status | Icon | Notes |
|:--:|:---|:---|:---|:---:|:---|
| 34 | Services dashboard | `ServicesScreen.dart` | `ServicesScreen.kt` | ✅ | Verified |
| 35 | Nearby radar | `NearbyScreen`, `NearbyLikesScreen` | `NearbyScreen.kt` + `NearbyLikesScreen.kt` | ✅ | Verified |
| 36 | Contacts sync | `contacts_screen.dart` | `ContactsScreen.kt` | ✅ | Verified |
| 37 | Top groups | `top_groups_screen.dart` | `TopGroupsScreen.kt` | ✅ | Verified |
| 38 | HTML5 games | `game_launch_screen.dart` + `InAppWebScreen` | `GameLaunchScreen.kt` + `InAppWebScreen.kt` | ✅ | Verified |

---

### Module 8: Settings & Account

| # | Feature | Flutter Reference | Native Status | Icon | Notes |
|:--:|:---|:---|:---|:---:|:---|
| 39 | Premium pricing | `PricingPage.dart` | `PricingScreen.kt` | ✅ | Verified |
| 40 | Blue tick verification request | — | — | ⏸️ | خارج از دامنه: در نسخهٔ فلاتر وجود ندارد و بنا به دستور مالک محصول پیاده‌سازی نمی‌شود. |
| 41 | Settings root | `Settings.dart` | `SettingsScreen.kt` | ✅ | Verified |
| 42 | Active sessions + Blocked users | `ActiveSessionsScreen`, `BlockedUsersScreen` | `ActiveSessionsScreen.kt`, `BlockedUsersScreen.kt` | ✅ | Verified |
| 43 | Theme / Appearance | `ThemeSettingsScreen` | `ThemeSettingsScreen.kt` | ✅ | Verified |
| 44 | Data & Storage | `DataStorageSettingsScreen` | `DataStorageSettingsScreen.kt` + `AppCacheManager.kt` | ✅ | Verified |
| 45 | Change password | `ChangePasswordScreen` | `ChangePasswordScreen.kt` | ✅ | Verified |
| 46 | About, Terms, FAQ, Contact | `AboutSettings`, `Terms`, `FAQ`, `ContactUs` | `AboutSettingsScreen.kt`, `FAQScreen.kt`, `ContactUsScreen.kt` | ✅ | Verified |

---

## Summary Statistics

| Category | Count | Details |
|:---|:---:|:---|
| ✅ Fully done | 0 | تا نصب APK تازه و اجرای سناریوهای پذیرش، هیچ موردی «قبول‌شده» شمرده نمی‌شود. |
| 🔧 Needs verify | 45 | مستلزم کامپایل، نصب، و آزمون تعاملی روی امولاتور متصل است. |
| ⏸️ Excluded | 1 | درخواست تیک آبی در مرجع فلاتر وجود ندارد. |
| ❌ Not implemented | تعیین‌نشده | ممیزی دوبارهٔ موردبه‌مورد در حال انجام است. |

**Current parity estimate: تأییدنشده تا زمان اجرای پذیرش روی APK تازه**
