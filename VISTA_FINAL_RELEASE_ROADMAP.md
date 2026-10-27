# نقشه راه جامع و حافظه فنی انتشار نهایی `vista_native`
**(`VISTA_FINAL_RELEASE_ROADMAP.md` — Single Source of Truth)**

**Type:** Release Audit, Architecture & Full-Stack Parity Roadmap  
**Status:** Complete (Ready for Release)  
**Progress:** `[██████████] 100% (33 / 33 Tasks Completed)`  
**Target Repositories:**
- **Native Android Client (Primary Target):** `E:\vista_native` (Kotlin, Jetpack Compose, Hilt, Room, Retrofit/OkHttp, Media3)
- **Legacy Flutter Client (Feature Parity Reference):** `E:\vista`
- **Go Backend Service:** `E:\vista-backend`
- **Next.js Admin & Control Panel:** `E:\vista-manager`

---

## ۱. خلاصه مدیریتی و وضعیت آمادگی انتشار (Executive Readiness Overview)

بررسی خط‌به‌خط هر چهار مخزن نشان می‌دهد که `E:\vista_native` از نظر اسکلت معماری (ماژول‌بندی `:core:*` و `:feature:*`، دیتابیس Room، ساختار اولویت آفلاین و طراحی پایه) زیرساخت خوبی دارد، اما **در وضعیت فعلی دارای ۲۸ باگ بحرانی (Release Blockers / P0)**، **۲۲ عدم تطابق مستقیم با بک‌اند و پنل مدیریت** و **۳۵+ نقص تجربه کاربری (UI/UX) در مقایسه با استاندارد تلگرام، X، تردز و اینستاگرام** است که قبل از جایگزینی نسخه فلاتر (`E:\vista`) باید به صورت سیستماتیک برطرف شوند.

### مهم‌ترین موانع بحرانی انتشار (Top Release Blockers):
1. **قفل شدن صفحه‌بندی فید «دنبال‌شده‌ها» روی صفحه اول و عدم فراخوانی الگوریتم فید شخصی‌سازی‌شده (`GET /v1/feed`):** تب «دنبال‌شده‌ها» پارامتر `cursor` ارسال می‌کند در حالی که بک‌اند (`internal/posts/handlers.go:91`) منتظر `offset` است و همیشه ۲۰ پست اول تکرار می‌شود. همچنین فید الگوریتمی `GET /v1/feed` (که از پنل ادمین `vista-manager` وزن‌دهی می‌شود) اصلاً صدا زده نمی‌شود و هیچ رویداد `view` یا `dwell` برای الگوریتم ثبت نمی‌گردد.
2. **باگ‌های مخرب در سیستم چت و پیام‌رسان:**
   - تابع `peerIdentityKey()` در `OfflineFirstChatRepository.kt:181` در صورت خالی بودن `peerId` از `title` (نام نمایشی) استفاده می‌کند و مکالمات دو کاربر هم‌نام (مثلاً دو نفر با نام «علی») را یکی فرض کرده، پیام‌های یکی را به دیگری منتقل و مکالمه دوم را برای همیشه از دیتابیس Room حذف می‌کند!
   - هنگام قطع کامل اینترنت، `ChatRealtime.kt:501` وضعیت اتصال را به دروغ `CONNECTED` (نقطه سبز «متصل») تنظیم می‌کند و فالبک وب‌سوکت به `/ws` (به‌جای `/v1/chat/ws`) وصل می‌شود که هیچ رویداد چتی دریافت نمی‌کند.
   - در چت محرمانه (Secret Chat)، ویرایش پیام (`editMessage`) متن را به‌صورت **Plaintext** به سرور می‌فرستد (که با خطای `400 E2EE_REQUIRED` رد می‌شود)، فایل‌های مدیا به‌صورت خام آپلود می‌شوند (`if (false && ...)` در خط ۱۴۰۶)، محافظ اسکرین‌شات (`FLAG_SECURE`) وجود ندارد، و تایمر حذف خودکار به‌محض فعال‌سازی کل تاریخچه قبلی چت را پاک می‌کند!
   - جستجوی داخل چت (`ChatScreens.kt:2173`) به‌جای اسکرول روی پیام منطبق، کل لیست پیام‌های چت را مخفی کرده و فقط ۱ پیام را نشان می‌دهد.
3. **باگ‌های بحرانی در ارسال پست و مدیا:**
   - پست جدید پس از انتشار در فید نمایش داده نمی‌شود (عدم تطابق `CreatePostResponseDto` با خروجی روت `POST /v1/posts`، عدم درج در `FeedDao` و فیلتر شدن پست‌های خود کاربر در کوئری بک‌اند).
   - برش ویدیو (`VideoTrimmerScreen.kt:401`) به دلیل رد کردن Keyframe اولیه (`presentationTimeUs >= startUs`) فایل MP4 خروجی را خراب می‌کند و انتخاب ویدیو هم‌زمان کل فایل خام را در `cacheDir` کپی و نشت فضا ایجاد می‌کند.
   - برش موزیک فقط یک اسلایدر بی‌صداست که کل فایل صوتی خام را بدون برش آپلود می‌کند و بک‌اند هم `music_start_ms` / `music_end_ms` را نادیده می‌گیرد.
   - تمامی عکس‌های فید به دلیل `"4:5".toFloatOrNull() == null` و محدودیت `.coerceAtMost(280.dp)` در `FeedScreen.kt:1092` از بالا و پایین بریده می‌شوند، و ویدیوهای دارای کاور در پروفایل، هشتگ و پست‌های ذخیره‌شده به‌عنوان عکس ثابت رندر می‌شوند.
4. **قطع بودن ارتباط با پنل مدیریت (`E:\vista-manager`) و امنیت شبکه:**
   - پینینگ پویا (Dynamic TLS Pinning) به دلیل `signatureVerified = false` و `expiresAtEpochSeconds = null` در `BootstrapStatusClient.kt:67-70` و خطای `X509TrustManager` کاملاً غیرفعال است.
   - صفحه مسدودیت کاربر (`BannedScreen.kt`) کد مرده است و هیچ‌گاه در محیط پروداکشن باز نمی‌شود.
   - فلگ‌های قابلیت‌ها (`feature_flags` / `kill_switches`)، هشدارهای سیستم (`alerts`) و `fcm_resync_epoch` از `/api/v1/system/status` نادیده گرفته می‌شوند.
   - درخواست تیک آبی/تأیید هویت (`VerificationRequestScreen.kt`) و گزارش استوری/پروفایل کاملاً نمایشی (Fake Snackbar/Toast) هستند.

---

## ۲. ماتریس کامل تطابق قابلیت‌ها: نسخه فلاتر (`E:\vista`) در برابر نسخه نیتیو (`E:\vista_native`)

| # | ماژول / قابلیت | مسیر مرجع در فلاتر (`E:\vista`) | مسیر در نیتیو (`E:\vista_native`) | وضعیت تطابق | کسری‌ها، باگ‌ها و قابلیت‌های فراموش‌شده |
|---|---|---|---|---|---|
| 1 | **راه‌اندازی، گیت‌های سیستم و وضعیت سرور** | `lib/app/app_runner.dart:288-485`, `lib/services/system_status_service.dart`, `lib/screens/maintenance_screen.dart`, `banned_screen.dart` | `app/.../navigation/StartupResolver.kt`, `core/network/.../BootstrapStatusClient.kt`, `feature/auth/.../startup/SplashScreen.kt`, `MaintenanceScreen.kt`, `BannedScreen.kt` | **ناقص / خراب** | • `BannedScreen.kt` در پروداکشن غیرقابل دسترس است (`AppNavGraph.kt:224`).<br>• TLS Pinning به دلیل `signatureVerified = false` همیشه خاموش است.<br>• `feature_flags`, `kill_switches`, `alerts`, `fcm_resync_epoch` نادیده گرفته می‌شوند.<br>• حالت تعمیرات فقط در شروع سرد چک می‌شود و تایمر بازیابی خودکار ۱۰ ثانیه‌ای یا هندلینگ `503` در حین کار ندارد. |
| 2 | **آنبوردینگ و معرفی برنامه** | `lib/features/onboarding/screens/onboarding_screen.dart`, `AppInfoSlideShow.dart` | `feature/auth/.../onboarding/OnboardingScreen.kt` | **کامل** | اسلایدر ۴ صفحه‌ای با قابلیت رد کردن و مشاهده مجدد از بخش «درباره ما». |
| 3 | **احراز هویت (OTP، رمز، 2FA، بازیابی)** | `lib/features/auth/screens/auth_wizard_screen.dart`, `recovery_screen.dart`, `lib/services/auth_api_service.dart` | `feature/auth/.../AuthWizardScreen.kt`, `AuthWizardViewModel.kt`, `OkHttpAuthRemoteDataSource.kt`, `SessionRefreshCoordinator.kt` | **ناقص / دارای باگ** | • **باگ Race Condition در رفرش توکن** (`SessionRefreshCoordinator.kt:45-90`) که نشست معتبر جدید را پاک می‌کند.<br>• **فراموش‌شده:** حذف حساب کاربری با OTP (`POST /v1/auth/account-deletion/request-otp`, `confirm`)، غیرفعال‌سازی تایید دو مرحله‌ای (`POST /v1/auth/2fa/remove`)، تایید ایمیل (`send-email-verification`, `verify-email`). |
| 4 | **امنیت، بیومتریک و نشست‌های فعال** | `lib/services/biometric_service.dart`, `lib/services/session_manager_service_v2.dart:1-920`, `active_sessions_screen.dart` | `core/security/.../BiometricAuthenticator.kt`, `MainActivity.kt:215`, `feature/profile/.../privacy/ActiveSessionsApi.kt`, `ActiveSessionsScreen.kt` | **ناقص / خراب** | • ۴ اندپوینت نشست پیاده‌سازی نشده: `POST /v1/sessions/register`، `validate`، `touch`، `terminate-others`.<br>• `GET v1/sessions/active` پارامتر `current_session_id` را نمی‌فرستد.<br>• رویداد وب‌سوکت `"session_terminated"` در `ChatRealtime.kt` نادیده گرفته می‌شود.<br>• هدر `X-Device-ID` در درخواست‌های REST در هر اجرا تصادفی تولید می‌شود (`InternalAuthorizationInterceptor.kt:37`). |
| 5 | **فید خانه، اکسپلور، الگوریتم و ارسال پست** | `lib/features/posts/screens/home_screen.dart`, `explore_screen.dart`, `add_post_screen.dart`, `appeal_screen.dart` | `feature/feed/.../ui/FeedScreen.kt`, `PostDetailScreen.kt`, `create/AddPostScreen.kt`, `appeal/AppealScreen.kt`, `data/FeedApi.kt`, `OfflineFirstFeedRepository.kt` | **خراب / ناقص** | • اندپوینت فید الگوریتمی `GET /v1/feed` هرگز صدا زده نمی‌شود.<br>• صفحه‌بندی فید دنبال‌شده‌ها (`GET /v1/feed/following`) به دلیل ارسال `cursor` به‌جای `offset` همیشه صفحه ۱ را برمی‌گرداند.<br>• عدم ارسال رویدادهای `view` و `dwell` به `POST /v1/feed/events`.<br>• عدم نمایش پست جدید پس از انتشار، نبود پیش‌نویس (Draft) و نبود آپلود پس‌زمینه (`WorkManager`). |
| 6 | **نظرات و پاسخ‌های تودرتو (Threads/Comments)** | `lib/features/posts/widgets/comments_bottom_sheet.dart`, `comment_input.dart` | `feature/feed/.../ui/CommentsBottomSheet.kt`, `CommentsViewModel.kt`, `data/CommentApi.kt`, `CommentRepository.kt` | **دارای باگ منطقی** | • پاسخ به یک پاسخ (سطح ۲ به بعد) در دیتابیس ثبت می‌شود اما در `CommentsBottomSheet.kt:329-388` هرگز رندر نمی‌شود (غیب شدن کامنت!).<br>• پاسخ‌های دارای والد در صفحه بعدی مخفی می‌مانند.<br>• منشن در کامنت فقط کاربران کامنت‌گذار فعلی را پیشنهاد می‌دهد.<br>• تعداد کامنت در `FeedDao` پس از ثبت/حذف کامنت آپدیت نمی‌شود. |
| 7 | **اعلان‌ها (Notifications)** | `lib/features/posts/screens/notificationScreen.dart`, `lib/provider/notification_provider.dart` | `feature/feed/.../ui/NotificationsScreen.kt`, `NotificationsViewModel.kt`, `data/NotificationApi.kt`, `NotificationModels.kt` | **دارای باگ** | • تداخل `markAllAsRead()` در لحظه باز شدن صفحه با `refresh()` که نشانگرهای خوانده‌نشده را فوراً پاک می‌کند.<br>• عدم نگاشت `post_admin_edit` و `post_admin_removed` برای باز کردن صفحه اعتراض (`AppealScreen`).<br>• نمایش متن انگلیسی `"mentioned you in a post"` به‌جای فارسی. |
| 8 | **موزیک و مینی‌پلیر** | `lib/features/music/screens/music_list_screen.dart`, `mini_player_widget.dart` | `feature/feed/.../ui/MusicMiniPlayerBar.kt`, `create/AddPostScreen.kt`, `data/FeedApi.kt:95-115` | **دارای باگ** | • عدم برش واقعی فایل صوتی قبل از آپلود (`AddPostViewModel.kt:393`).<br>• نمایش `"audio:100004523"` به‌عنوان نام آهنگ در URIهای SAF.<br>• ایجاد و بافر هم‌زمان چند `ExoPlayer` در `PostMusicBubble` هنگام اسکرول فید. |
| 9 | **استوری ۲۴ ساعته و هایلایت‌ها** | `lib/features/stories/presentation/screens/story_player_screen.dart`, `story_creation_screen.dart`, `story_privacy_settings_screen.dart`, `story_repository.dart` | `feature/feed/.../stories/data/StoryApi.kt`, `StoryRepository.kt`, `ui/player/StoryPlayerScreen.kt`, `ui/editor/StoryEditorScreen.kt` | **ناقص / استاب** | • **هایلایت استوری (`/v1/highlights/*`) ۱۰۰٪ فراموش شده.**<br>• **تنظیمات حریم خصوصی استوری (`/v1/me/story-settings`) ۱۰۰٪ فراموش شده.**<br>• گزارش استوری (`StoryPlayerScreen.kt:839`) فیک است و به سرور ارسال نمی‌شود.<br>• رای دادن در نظرسنجی استوری UI را آپدیت نمی‌کند.<br>• استیکرهای سوال (`question`)، شمارش معکوس، آب‌وهوا، تاریخ، گیف، هشتگ و نقاشی آزاد (Drawing) وجود ندارند. |
| 10 | **چت، گروه‌ها، ویس/مدیا، وب‌سوکت و E2EE** | `lib/features/chat/screens/modern_chat_screen.dart`, `conversation_list_screen.dart`, `group_info_screen.dart`, `e2e_Crypto_service.dart`, `group_e2e_service.dart` | `feature/chat/.../data/remote/ChatApi.kt`, `ChatRealtime.kt`, `OfflineFirstChatRepository.kt`, `ChatE2EEService.kt`, `ChatScreens.kt`, `GroupDetailsScreen.kt` | **ناقص / دارای باگ‌های بحرانی** | • **رمزنگاری سرتاسری گروه (`VE2E_GROUP_V1:` و `/v1/chat/conversations/{id}/keys`) وجود ندارد.**<br>• **آپدیت وضعیت آنلاین (`POST /v1/presence/update`) ارسال نمی‌شود.**<br>• ادغام مخرب مکالمات هم‌نام (`peerIdentityKey`)، نشت متن خام در ویرایش پیام محرمانه، آپلود خام مدیا در چت محرمانه، نبود `FLAG_SECURE`، باگ تایمر حذف خودکار، باگ جستجوی چت، موج صوتی فیک و پخش هم‌زمان چند ویس. |
| 11 | **پروفایل (شخصی، عمومی، ویرایش، ذخیره‌شده‌ها، کارت ویستا)** | `lib/features/profile/screens/profileScreen.dart`, `editeProfile.dart`, `saved_posts_screen.dart`, `qr_scanner_page.dart`, `account_details_screen.dart` | `feature/profile/.../ui/OwnProfileScreen.kt`, `OtherUserProfileScreen.kt`, `SavedPostsScreen.kt`, `VistaIdCardDialog.kt`, `QrScannerScreen.kt`, `EditProfileScreen.kt` | **ناقص / دارای باگ** | • در `SavedPostDto` فیلد نویسنده بدون `@SerialName("profiles")` تعریف شده و نام و آواتار همه پست‌های ذخیره‌شده خالی است!<br>• آیکون ۳ نقطه در پروفایل سایر کاربران (`OtherUserProfileScreen.kt:270`) غیرقابل کلیک است (عدم امکان بلاک/ریپورت).<br>• دکمه `+` در `OwnProfileScreen.kt:231` فقط Toast نشان می‌دهد.<br>• عدم چرخش EXIF در آپلود آواتار (`ProfileAvatarUploader.kt:81`). |
| 12 | **جستجو، هشتگ‌ها و اکسپلور** | `lib/features/search/screens/searchPage.dart` | `feature/search/.../ui/SearchScreens.kt`, `SearchViewModel.kt`, `HashtagPostsScreen.kt`, `UserSuggestionsScreen.kt` | **ناقص** | • `HashtagPostCard` (`HashtagPostsScreen.kt:200`) یک کارت ۳۰ خطی ناقص بدون آواتار، ویدیو، کاروسل و دکمه‌های لایک/کامنت/ذخیره است.<br>• صفحه‌بندی پست‌های هشتگ در `SearchViewModel.kt:285` روی `offset = 0` قفل است.<br>• گرید بصری اکسپلور (مانند اینستاگرام) در تب جستجو وجود ندارد. |
| 13 | **تنظیمات، حریم خصوصی و پشتیبانی** | `lib/features/settings/screens/Settings.dart`, `privacy_security_page.dart`, `notification_settings_screen.dart`, `ContactUs.dart` | `feature/profile/.../settings/SettingsScreen.kt`, `PrivacySecurityScreen.kt`, `NotificationSettingsScreen.kt`, `ThemeSettingsScreen.kt`, `ContactUsScreen.kt` | **ناقص** | • صفحه تنظیمات حریم خصوصی استوری، صفحه حذف حساب کاربری و گزینه حذف 2FA وجود ندارند. |
| 14 | **تأیید هویت (تیک آبی/طلایی) و فروشگاه نشان‌ها** | `lib/features/settings/screens/subpages/VerificationRequestPage.dart`, `vistaStore/store.dart` | `feature/profile/.../settings/verification/VerificationRequestScreen.kt`, `VerificationBadgeIcon.kt` | **استاب / فراموش‌شده** | • **`VerificationRequestScreen.kt:178-231` کاملاً استاب است** و هیچ فایلی آپلود نمی‌کند و `POST /v1/me/verify` را صدا نمی‌زند.<br>• فروشگاه نشان‌ها (`vistaStore/store.dart`) منتقل نشده است. |
| 15 | **اشتراک ویژه، قیمت‌گذاری و پرداخت (زیبال و بازار)** | `lib/features/pricing/screens/pricing_screen.dart`, `payment_service.dart`, `BazaarService.dart` | `feature/profile/.../settings/pricing/PricingPlansApi.kt`, `PricingScreen.kt`, `PricingViewModel.kt` | **ناقص / خراب** | • **پرداخت درون‌برنامه‌ای کافه‌بازار (`POST /v1/payment/bazaar-verify`) و بسته‌های سکه (`GET /v1/payment/packages`) وجود ندارند.**<br>• دیپ‌لینک بازگشت از درگاه زیبال (`https://cafevista.ir/payment/callback`) در `DeepLinkCoordinator.kt:68` به‌عنوان `UNSUPPORTED` رد می‌شود! |
| 16 | **افراد نزدیک (Nearby) و هاب سرویس‌ها / بازی‌ها** | `lib/features/nearby/presentation/nearby_screen.dart`, `lib/features/services/screens/services_screen.dart` | `feature/services/.../nearby/NearbyScreen.kt`, `NearbyViewModel.kt`, `ServicesScreen.kt`, `GameLaunchScreen.kt`, `TopGroupsScreen.kt` | **دارای باگ / ناقص** | • کلیک روی چت در `NearbyScreen` (`VistaShell.kt:497`) و کلیک روی گروه در `TopGroupsScreen` (`VistaShell.kt:515`) به مسیر اشتباه ناوبری می‌کنند و چت را باز نمی‌کنند!<br>• اندپوینت `POST /v1/nearby/undo` و تبدیل مختصات به نام شهر/استان (`city_name`, `province_name`) وجود ندارند. |
| 17 | **اشتراک‌گذاری (درون‌برنامه‌ای و سیستم‌عامل)** | `lib/features/share/screens/share_handler_screen.dart`, `post_share_bottom_sheet.dart` | `app/.../share/IncomingShareCache.kt`, `ShareTargetBottomSheet.kt`, `PostShareBottomSheet.kt`, `PostDirectShareSheet.kt` | **کامل** | دریافت اشتراک از اندروید (`ACTION_SEND`) و اشتراک پست به دایرکت، استوری و سایر برنامه‌ها کار می‌کند. |
| 18 | **ایموجی اختصاصی و پیکر** | `lib/features/emoji/widgets/custom_emoji_text.dart`, `emoji_picker_widget.dart` | `core/designsystem/.../component/VistaEmojiText.kt`, `VistaEmojiCatalog.kt`, `VistaEmojiPickerSheet.kt` | **کامل** | رندر SVG ایموجی‌ها و شیت انتخاب ایموجی پیاده‌سازی شده است. |
| 19 | **پوش نوتیفیکیشن، دیپ‌لینک و آپدیتر خودکار** | `lib/services/push_notification_service.dart`, `deep_link_service.dart`, `update_service.dart` | `app/.../notifications/PushTokenRegistrar.kt`, `VistaFirebaseMessagingService.kt`, `DeepLinkCoordinator.kt`, `update/VistaUpdateManager.kt` | **خراب / سیم‌کشی نشده** | • **`VistaUpdateManager.kt` کد مرده است** و هیچ‌جا تزریق یا فراخوانی نشده است.<br>• `PushTokenRegistrar.kt:61` توکن FCM را به `userId` مقید نمی‌کند (باگ تعویض اکانت) و در خروج حذف نمی‌کند.<br>• دیپ‌لینک‌های `/u/{username}`، `/join/{code}` در دامنه `coffevista.ir` و فیلد `deeplink` در پوش‌های ادمین کار نمی‌کنند. |

---

## ۳. ممیزی یکپارچگی با بک‌اند (`E:\vista-backend`) و پنل مدیریت (`E:\vista-manager`)

جدول زیر تمامی نقاطی را نشان می‌دهد که کلاینت نیتیو با قراردادهای بک‌اند Go (`E:\vista-backend`) و قابلیت‌های مدیریتی پنل Next.js (`E:\vista-manager`) ناهماهنگ است:

| # | اندپوینت بک‌اند (`E:\vista-backend`) | بخش مربوطه در پنل ادمین (`E:\vista-manager`) | وضعیت در `E:\vista_native` و فایل مربوطه | تأثیر در پروداکشن |
|---|---|---|---|---|
| 1 | `GET /api/v1/system/status` (`internal/system/handler.go:69-88`) — `tls_pinning` | `src/app/dashboard/tls-pinning/page.tsx` (`PUT /api/v1/admin/system/tls-pinning`) | **خراب:** `BootstrapStatusClient.kt:51-71` مقدار `signatureVerified = false` و `expiresAtEpochSeconds = null` می‌دهد و `PolicyTrustManager` در `TlsPolicy.kt:138` فاقد `X509TrustManagerExtensions` است. | تنظیمات پینینگ گواهی SSL از پنل مدیریت یا اصلاً اعمال نمی‌شود یا در صورت فعال‌سازی باعث خطای شبکه در اندروید می‌شود. |
| 2 | `POST /api/v1/system/tls-pin-report` (`internal/system/handler.go:33-67`) | `src/app/dashboard/tls-pinning/page.tsx` (`GET .../tls-pinning/reports`) | **خراب:** `TlsFailureReporterImpl.kt:35-43` هدر `X-Device-ID` و فیلد `app_version` را ارسال نمی‌کند. | جدول گزارش خطاهای گواهی در پنل ادمین هیچ گزارشی از نسخه نیتیو دریافت نمی‌کند. |
| 3 | `GET /api/v1/system/status` — `feature_flags`, `features`, `kill_switches`, `alerts` | `src/app/dashboard/settings/page.tsx` (`PUT /api/v1/admin/system/features/update`) | **نادیده گرفته‌شده:** `BootstrapStatusClient.kt:20-23` این فیلدها را پارس نمی‌کند و `ErrorClassifier.kt:18` کد `feature_disabled` را به «ورود موقتاً غیرفعال شده است» ترجمه می‌کند. | خاموش کردن قابلیت‌ها (مثلاً چت، آپلود یا پرداخت) از پنل ادمین در اپ نیتیو اعمال نمی‌شود و پیام خطای اشتباه به کاربر می‌دهد. |
| 4 | `GET /api/v1/system/status` — `fcm_resync_epoch` | `src/app/dashboard/notifications/page.tsx` (`POST .../notifications/fcm-resync`) | **نادیده گرفته‌شده:** `BootstrapStatusClient.kt` و `PushTokenRegistrar.kt` فیلد `fcm_resync_epoch` را چک نمی‌کنند. | دکمه همگام‌سازی مجدد توکن‌های FCM در پنل ادمین روی کاربران نیتیو بی‌اثر است. |
| 5 | HTTP 403 `{"error":{"code":"error","message":"banned"}}` (`internal/httpapi/router.go:453-465`) | `src/app/dashboard/security/page.tsx` و `users/page.tsx` | **خراب:** `BackendErrorParser.kt:19-27` کد `"error"` را به‌جای `"banned"` برمی‌گرداند و هیچ ناوبری به `AppRoute.Banned` (`AppNavGraph.kt:289`) انجام نمی‌شود. | مسدود کردن کاربر یا دستگاه از پنل مدیریت، اپ نیتیو را به صفحه مسدودیت هدایت نمی‌کند. |
| 6 | HTTP 503 `maintenance_mode` (`internal/httpapi/router.go:467-490`) | `src/app/dashboard/settings/page.tsx` (`POST .../system/maintenance`) | **ناقص:** فقط در شروع سرد چک می‌شود؛ اینترسپتور 503 در حین اجرا و پولینگ خودکار در `MaintenanceScreen.kt` وجود ندارد. | فعال کردن حالت تعمیرات، کاربران آنلاین فعلی را به صفحه تعمیرات منتقل نمی‌کند. |
| 7 | `GET /v1/feed` (`internal/posts/handlers.go:30, 69-88`) | `src/app/dashboard/monitoring/page.tsx` (`/api/v1/admin/monitoring/feed-ranker/profiles`) | **فراموش‌شده:** `FeedApi.kt` فقط `v1/explore` و `v1/feed/following` را دارد و `GET /v1/feed` را صدا نمی‌زند. | تغییر ضرایب الگوریتم فید در پنل ادمین (`FeedRankerProfile`) هیچ تأثیری روی فید کاربران نیتیو ندارد! |
| 8 | `GET /v1/feed/following` (`internal/posts/handlers.go:91`) | — | **خراب:** `FeedApi.kt:14-18` کوئری `@Query("cursor")` می‌فرستد ولی بک‌اند `r.URL.Query().Get("offset")` می‌خواند. | صفحه‌بندی تب «دنبال‌شده‌ها» همیشه صفحه اول را تکرار می‌کند. |
| 9 | `POST /v1/feed/events` (`internal/posts/handlers.go:33`) | `src/app/dashboard/monitoring/page.tsx` | **ناقص:** رویدادهای `view` و `dwell` اصلاً در `FeedScreen.kt` تولید نمی‌شوند و ارسال رویدادها بدون Batching است (`OfflineFirstFeedRepository.kt:280`). | جدول `post_impressions` پر نمی‌شود و پست‌های تکراری در اکسپلور حذف نمی‌گردند. |
| 10 | `POST /v1/me/verify` (`internal/profile/handlers.go:59`) | `src/app/dashboard/verification/page.tsx` (`/api/v1/admin/verifications`) | **استاب:** `VerificationRequestScreen.kt:178-231` هیچ درخواستی به سرور نمی‌فرستد. | کاربران نیتیو امکان ثبت درخواست تیک تأیید برای بررسی در پنل ادمین را ندارند. |
| 11 | `POST /v1/stories/{storyId}/report` (`internal/stories/handlers.go:118`) | `src/app/dashboard/reports/page.tsx` (`/api/v1/admin/reports`) | **فیک:** `StoryPlayerScreen.kt:839-845` بدون فراخوانی API پیام موفقیت نشان می‌دهد. | گزارش تخلف استوری‌ها به پنل مدیریت نمی‌رسد. |
| 12 | `POST /v1/sessions/register`, `validate`, `touch`, `terminate-others` (`router.go:177-184`) + WS `"session_terminated"` | `src/app/dashboard/users/[id]/page.tsx` (`GET .../users/{id}/sessions`) | **فراموش‌شده:** در `ActiveSessionsApi.kt` و `ChatRealtime.kt:125` وجود ندارد. | پنل ادمین اطلاعات دستگاه نشست‌های نیتیو را نمی‌بیند و بستن نشست از راه دور، وب‌سوکت را قطع نمی‌کند. |
| 13 | `POST /v1/auth/account-deletion/request-otp` و `confirm` (`router.go:170-173`) | `src/app/dashboard/account-deletions/page.tsx` | **فراموش‌شده:** در `:feature:auth` و `:feature:profile` وجود ندارد. | امکان ثبت درخواست حذف حساب ۳۰ روزه در نسخه نیتیو وجود ندارد. |
| 14 | `POST /v1/payment/bazaar-verify` و `GET /v1/payment/packages` (`internal/payment/routes.go:24-26`) | `src/app/dashboard/pricing/page.tsx` (`coin_packages`) | **فراموش‌شده:** در `PricingPlansApi.kt` وجود ندارد. | خرید سکه و پرداخت کافه‌بازار در نسخه نیتیو غیرفعال است. |
| 15 | `POST /v1/presence/update` (`internal/presence/handlers.go:26`) | `src/app/dashboard/users/page.tsx` (`GET .../admin/users/online`) | **فراموش‌شده:** `ChatApi.kt:17` فقط `GET` دارد و هیچ هارت‌بیت حضوری ارسال نمی‌کند. | آمار کاربران آنلاین در پنل ادمین و وضعیت آنلاین کاربران نیتیو به‌روز نمی‌شود. |
| 16 | پوش‌های ادمین (`POST /api/v1/admin/broadcasts`, `test-push` با فیلد `deeplink`) و اعلان‌های مدریشن (`post_admin_edit`, `post_admin_removed`) | `src/app/dashboard/broadcasts/page.tsx`, `notifications/page.tsx`, `posts/page.tsx` | **خراب:** `MainActivity.kt:308-321` فیلد `data["deeplink"]` را نادیده می‌گیرد و `NotificationModels.kt:173` انواع مدریشن ادمین را نمی‌شناسد. | کلیک روی پوش‌های ادمین و اعلان‌های ویرایش/حذف پست توسط ادمین به صفحه مقصد هدایت نمی‌شود. |

---

## ۴. کالبدشکافی عمیق بخش‌ها و ایرادات فنی (Deep-Dive Section Audits)

### بخش A: سیستم چت و پیام‌رسان (`:feature:chat` — استاندارد مرجع: تلگرام)

#### A.1. باگ‌های لایه Realtime (WebSocket / SSE)، همگام‌سازی و دیتابیس
1. **نمایش دروغین وضعیت «متصل» (نقطه سبز) در زمان قطع اینترنت (`ChatRealtime.kt:465-467, 501-518`):**
   - وقتی اتصال WebSocket و SSE هر دو شکست می‌خورند، بلوک `catch` در خط ۵۰۱ متغیر `isFallbackPolling = true` و `mutableState.value = RealtimeConnectionState.CONNECTED` را ست می‌کند! در نتیجه حتی در حالت پرواز (Airplane Mode)، هدر چت وضعیت سبز «متصل» را نشان می‌دهد و بنر «در انتظار شبکه / در حال اتصال...» (`RealtimeBanner` در `ChatScreens.kt:1989`) هرگز ظاهر نمی‌شود.
2. **اتصال فالبک وب‌سوکت به اندپوینت اشتباه `/ws` و بلعیدن خطای 401 (`ChatRealtime.kt:238-252, 307, 326`):**
   - در بک‌اند، هاب چت روی `/v1/chat/ws` (`internal/chat/handlers.go:103`) است و `/ws` (`router.go:148`) یک وب‌سوکت عمومی مجزاست که هیچ پیام چتی منتشر نمی‌کند. کلاینت در صورت خطا روی `/v1/chat/ws` به `/ws` وصل می‌شود و دیگر هیچ پیام، تایپینگ یا تیک خوانده‌شدنی دریافت نمی‌کند. همچنین در صورت انقضای توکن (401)، کد وضعیت HTTP در استثنا حفظ نمی‌شود و در SSE توکن در کوئری URL (`?token=...`) نشت می‌کند.
3. **تبدیل مقدار `null` در JSON به رشته متنی `"null"` (`ChatRealtime.kt:180`):**
   - تابع `JsonObject.string(key)` از `get(key)?.toString()?.trim('"')` استفاده می‌کند. در `kotlinx.serialization`، خروجی `JsonNull.toString()` برابر `"null"` است؛ بنابراین فیلدهای تهی به رشته `"null"` تبدیل شده و مسیریابی رویدادها را مختل می‌کنند.
4. **پاک شدن اطلاعات مکالمه در رویداد `CONVERSATION_UPDATED` (`ChatRealtime.kt:75-79`, `OfflineFirstChatRepository.kt:2070-2075`):**
   - بک‌اند هنگام تایید درخواست پیام فقط `{"id": "...", "status": "accepted"}` می‌فرستد. چون تمام فیلدهای `ConversationDto` مقدار پیش‌فرض دارند، این JSON ناقص به یک `ConversationDto` با `name = null, peerId = null, lastMessageText = null` تبدیل و در Room ذخیره می‌شود و اطلاعات مکالمه را پاک می‌کند!
5. **ادغام و حذف مخرب مکالمات کاربران هم‌نام (`OfflineFirstChatRepository.kt:181-184, 454-486`, `ChatModels.kt:30-38`):**
   - در `peerIdentityKey()`، کلید یکتا به صورت `val identity = cleanPeer ?: cleanTitle ?: id` محاسبه می‌شود. اگر دو مکالمه خصوصی `peerId == null` داشته باشند و نام نمایشی هر دو کاربر یکسان باشد (مثلاً هر دو «محمد» یا «کاربر»)، `purgeDuplicateConversations` یکی را حذف کرده و تمام پیام‌هایش را به مکالمه دیگر منتقل می‌کند!
6. **رمزگشایی و رمزنگاری مجدد کل تاریخچه پیام‌ها در هر تیک خوانده‌شدن (`OfflineFirstChatRepository.kt:2080-2090`):**
   - با دریافت هر رویداد `RealtimeEvent.Read`، کل پیام‌های مکالمه از Room خوانده شده، تک‌تک با Android Keystore رمزگشایی و مجدداً رمزنگاری و ذخیره می‌شوند (به‌جای یک کوئری ساده `UPDATE chat_message SET status = 'READ' WHERE ...`).
7. **بلعیده شدن پیام‌های خطای فارسی بک‌اند در چت (`OfflineFirstChatRepository.kt:2513-2522`, `BackendErrorParser.kt:16-27`):**
   - متد `remoteCall` در مواجهه با `HttpException` بدنه خطا (`errorBody`) را نادیده می‌گیرد و پیام‌های دقیق بک‌اند (مثل محدودیت حریم خصوصی، تکمیل ظرفیت گروه، لینک دعوت نامعتبر) را با عبارت کلی «ارتباط با سرویس گفتگو ناموفق بود» جایگزین می‌کند.

#### A.2. آسیب‌پذیری‌ها و باگ‌های چت محرمانه (Secret Chat & E2EE)
8. **ارسال متن خام (Plaintext) هنگام ویرایش پیام در چت محرمانه (`OfflineFirstChatRepository.kt:1956-1972`, `2048-2061`):**
   - متد `editMessage` بدون بررسی `ConversationType.SECRET` و بدون فراخوانی `e2eeService.encryptText`، متن جدید را به‌صورت خام به `api.editMessage` می‌فرستد (که بک‌اند در `chat/service.go:1075` با خطای `400 E2EE_REQUIRED` رد می‌کند). در مسیر دریافت هم `RealtimeEvent.MessageUpdated` متن رمزنگاری‌شده را بدون رمزگشایی در Room می‌نویسد!
9. **آپلود خام فایل‌ها و مدیا در چت محرمانه (`OfflineFirstChatRepository.kt:1406, 1486, 1820-1854`):**
   - گارد امنیتی در خطوط ۱۴۰۶ و ۱۴۸۶ با `if (false && conversation?.type == ConversationType.SECRET.name)` غیرفعال شده و فایل‌های عکس/ویدیو/ویس در چت محرمانه بدون رمزنگاری روی S3 آپلود می‌شوند.
10. **نبود `FLAG_SECURE` در چت محرمانه (`ChatScreens.kt:1636-1680`):**
    - برخلاف تلگرام و نسخه فلاتر (`SecretChatPrivacyService`)، در صفحه چت محرمانه پرچم `WindowManager.LayoutParams.FLAG_SECURE` تنظیم نمی‌شود و امکان اسکرین‌شات و ضبط صفحه باز است.
11. **باگ مخرب تایمر حذف خودکار در چت محرمانه (`MessagesViewModel.kt:830-871`):**
    - اولاً زمان فعال‌سازی تایمر (`enabledAt`) ذخیره نمی‌شود؛ بنابراین فعال کردن تایمر ۵ ثانیه‌ای روی یک چت قدیمی، بلافاصله تمام پیام‌های قدیمی را پاک می‌کند! ثانیاً شمارش معکوس از `createdAtEpochMillis` شروع می‌شود نه از زمان خوانده شدن (`MessageStatus.READ`)، لذا اگر مخاطب آفلاین باشد پیام قبل از دیده شدن از سرور پاک می‌شود!
12. **عدم هشدار تغییر کلید امنیتی مخاطب (TOFU) و عدم آپدیت پیش‌نمایش لیست چت (`OfflineFirstChatRepository.kt:883-907, 928-952`):**
    - تغییر کلید عمومی مخاطب بدون هشدار امنیتی بازنویسی می‌شود و پس از `redecryptSecretMessages` پیش‌نمایش لیست مکالمات روی «پیام رمزنگاری شده» باقی می‌ماند.
13. **نمایش چت‌های محرمانه در لیست فوروارد (`ChatScreens.kt:2995`):**
    - انتخاب چت محرمانه در `ForwardMessageSheet` با خطای `400 E2EE_FORWARD_UNSUPPORTED` از سمت بک‌اند مواجه می‌شود.
14. **نبود پشتیبانی از رمزنگاری گروهی (`VE2E_GROUP_V1:` و `GET /v1/chat/conversations/{id}/keys`):**
    - پیام‌های رمزنگاری‌شده گروهی نسخه فلاتر در نسخه نیتیو قابل رمزگشایی نیستند.

#### A.3. باگ‌های مدیا، ویس، پیوست‌ها و UI/UX تلگرامی
15. **مسدود بودن ارسال هرگونه فایل غیر PDF در `ChatUploadPolicy.kt:85-106`:**
    - `ChatUploadPolicy` برای `AttachmentKind.DOCUMENT` هدر فایل را با `%PDF-` چک می‌کند و ارسال فایل‌های ZIP، DOCX، XLSX، TXT، APK و غیره را با خطای «فقط فایل PDF معتبر قابل ارسال است» مسدود می‌کند!
16. **رد شدن ویدیوهای معتبر اندروید (HEVC/MP4) و آپلود در پوشه اشتباه (`ChatUploadPolicy.kt:192`, `ChatMediaUploader.kt:142`):**
    - برندهای استاندارد `iso4`, `iso5`, `iso6`, `3gp5`, `hevc`, `hvc1` در لیست سفید نیستند و ویدیوها به‌جای `videos/` در `files/` آپلود می‌شوند.
17. **کد مرده بودن `FullscreenMediaViewerDialog.kt` (پلیر حرفه‌ای Media3) و استفاده از `VideoView` قدیمی و عکس بدون زوم (`ChatScreens.kt:3025, 5417-5475`):**
    - کامپوننت کامل `FullscreenMediaViewerDialog.kt` هرگز فراخوانی نشده و به‌جای آن `AttachmentViewer` بدون قابلیت Pinch-to-Zoom، بدون نوار جلو/عقب بردن ویدیو و بدون ورق زدن (`HorizontalPager`) نمایش داده می‌شود.
18. **موج صوتی (Waveform) جعلی، پخش هم‌زمان چند ویس، و حذف شدن ویس‌های کوتاه ۱ ثانیه‌ای (`VoicePlayerBubble.kt:91, 351`, `VoiceRecorderDock.kt:215, 509`):**
    - موج صوتی همه ویس‌ها با فرمول ثابت `((i * 11 + 3) % 7 + 2) / 8.5f` کشیده می‌شود و آمپلی‌تود واقعی ضبط‌شده دور ریخته می‌شود.
    - هر حباب ویس یک `MediaPlayer` مجزا می‌سازد و زدن پلی روی دو ویس، هر دو را هم‌زمان پخش می‌کند و پخش خودکار ویس بعدی (Sequential Autoplay تلگرام) وجود ندارد.
    - شمارنده زمان ضبط بعد از `prepare/start` با تأخیر شروع می‌شود و ویس‌های ۱ تا ۱.۳ ثانیه‌ای به دلیل `durationSeconds < 1` بی‌صدا پاک می‌شوند!
19. **مخفی شدن کل تاریخچه چت هنگام جستجو (`ChatScreens.kt:2173-2177`):**
    - در زمان جستجو، `displayedMessages` فقط به یک لیست ۱ عضوی تبدیل می‌شود به‌جای اینکه روی لیست اصلی اسکرول و هایلایت شود.
20. **استفاده نشدن از `UnreadMessagesDivider.kt` و `PinnedMessagesBar.kt` (`ChatScreens.kt:2002, 4637`):**
    - جداکننده «پیام‌های خوانده‌نشده» و نوار چرخشی چند پیام پین‌شده (`PinnedMessagesBar`) کد مرده هستند و در صورت پین کردن پست اشتراکی، رشته JSON خام نمایش داده می‌شود.
21. **جهت و تراز اشتباه ژست Swipe-to-Reply در حالت RTL (`SwipeToReplyLayout.kt:40-48, 120-128`):**
    - جهت سوایپ برای پیام‌های ورودی و خروجی متفاوت است (برخلاف تلگرام که همه به چپ سوایپ می‌شوند) و به دلیل قرارگیری بیرون از `LayoutDirection.Ltr`، آیکون ریپلای در سمت اشتباه صفحه ظاهر می‌شود.
22. **بریده شدن حاشیه ری‌اکشن‌ها در منوی کانتکست (`TelegramMessageContextMenu.kt:166-195`, `ChatScreens.kt:3816`) و عدم امکان کپی/ویرایش کپشن مدیا (`MessageContextActionPolicy.kt:67, 76`):**
    - `bubbleBounds` کل ستون شامل حباب و ردیف ری‌اکشن‌ها را می‌گیرد و با `BlendMode.Clear` یک حفره مستطیلی زشت دور ری‌اکشن‌ها ایجاد می‌کند. همچنین کپی و ویرایش متن پیام‌های دارای عکس/ویدیو/فایل مسدود شده است.
23. **نبود Swipe-to-Reply، منوی کانتکست و ری‌اکشن روی آلبوم‌های چندتایی (`MediaAlbumBubble` در `ChatScreens.kt:2288-2307`).**
24. **نمایش JSON خام هنگام ریپلای روی پست اشتراک‌گذاری‌شده (`OfflineFirstChatRepository.kt:1340-1345`, `MessagesViewModel.kt:378-401`).**
25. **باقی ماندن عملیات‌های معلق (`PendingOperationEntity`) پس از حذف پیام در حال ارسال (`OfflineFirstChatRepository.kt:1974-1986`)** که باعث ارسال مجدد پیام حذف‌شده در پس‌زمینه می‌شود!
26. **دکمه‌های بی‌عمل یا اشتباه در `PartnerDetailsScreen`، `GroupDetailsScreen` و `ConversationListScreen` (`ChatScreens.kt:518, 6611, 6671, 6811, 6861-7029`):**
    - گزینه «گفتگوی محرمانه جدید» چت عادی باز می‌کند؛ دکمه ۳ نقطه در پروفایل مخاطب چت فقط Toast «به‌زودی» نشان می‌دهد و دکمه بلاک وجود ندارد؛ تب‌های رسانه/فایل/صدا/لینک در جزئیات چت و گروه غیرقابل کلیک هستند.

---

### بخش B: نمایش فید، الگوریتم و عملکرد (`:feature:feed` — استاندارد مرجع: X / Threads / Instagram)

1. **فراخوانی نشدن فید الگوریتمی `GET /v1/feed` (`FeedApi.kt:8-18`, `OfflineFirstFeedRepository.kt:54-60`):**
   - تب اکسپلور/پیشنهادی فقط `GET v1/explore` را صدا می‌زند و اندپوینت فید رتبه‌بندی‌شده شخصی (`GET /v1/feed`) اصلاً در `FeedApi` تعریف نشده است.
2. **باگ قفل شدن صفحه‌بندی فید «دنبال‌شده‌ها» روی صفحه اول (`FeedApi.kt:14-18` vs `internal/posts/handlers.go:91`):**
   - ارسال `@Query("cursor")` به‌جای `@Query("offset")` باعث می‌شود اسکرول بی‌نهایت در تب دنبال‌شده‌ها هرگز صفحه ۲ به بعد را بارگذاری نکند.
3. **عدم ثبت رویدادهای `view` و `dwell` و ارسال تک‌تک رویدادها بدون Batching (`FeedScreen.kt:507-567`, `OfflineFirstFeedRepository.kt:275-287`):**
   - هیچ ردیابی دیده‌شدن پست در Viewport انجام نمی‌شود؛ در نتیجه جدول `post_impressions` در بک‌اند خالی مانده و الگوریتم اکسپلور پست‌های تکراری نشان می‌دهد.
4. **اشتراک یک `LazyListState` واحد بین دو تب فید (`FeedScreen.kt:190, 412`):**
   - جابجایی بین تب «دنبال‌شده‌ها» و «پیشنهادی» موقعیت اسکرول تب قبلی را خراب یا ریست می‌کند.
5. **بریده شدن تمام عکس‌های مربعی و عمودی فید به ارتفاع `280.dp` و شکست پارس نسبت تصویر (`FeedScreen.kt:1054, 1091-1093`):**
   - `"4:5".toFloatOrNull()` مقدار `null` برمی‌گرداند و `.coerceAtMost(280.dp)` تمام عکس‌های `1:1` و `4:5` را از بالا و پایین کراپ می‌کند.
6. **ساخت هم‌زمان چندین نمونه `ExoPlayer` بدون هماهنگ‌کننده و بدون کش دیسک (`VistaFeedVideoPlayer.kt:77-96`, `FeedScreen.kt:1224-1249`):**
   - تمام ویدیوهای داخل صفحه هم‌زمان پخش می‌شوند، تمام پست‌های موزیک در زمان اسکرول شروع به دانلود صوت می‌کنند، و هیچ `SimpleCache` دیسکی برای جلوگیری از دانلود مجدد ویدیوها وجود ندارد.
7. **کار نکردن دابل‌تپ لایک روی ویدیوها و غیرقابل پخش مجدد بودن ویدیو در `PostDetailScreen` (`FeedScreen.kt:1069`, `VistaFeedVideoPlayer.kt:162`, `PostDetailScreen.kt:312`):**
   - `.clickable` داخل `VistaFeedVideoPlayer` رویدادهای لمسی را می‌بلعد و در `PostDetailScreen` به دلیل پاس دادن `onPostClick = {}` (غیر تهی)، کلیک روی ویدیو فقط `exoPlayer.pause()` را صدا می‌زند و هرگز دوباره پخش نمی‌شود!
8. **نمایش ویدیوهای دارای کاور به‌عنوان عکس ثابت در پروفایل، هشتگ و ذخیره‌شده‌ها (`ProfileParityComponents.kt:763`, `HashtagPostsScreen.kt:223`, `SavedPostsScreen.kt:261`).**

---

### بخش C: ارسال پست و پایپ‌لاین مدیا (`AddPostScreen`, `AddPostViewModel`, `PostMediaUploader`, `VideoTrimmerScreen`)

1. **عدم نمایش پست جدید پس از انتشار (`FeedDto.kt:226-231`, `AddPostViewModel.kt:470-480`, `internal/posts/repository.go:691`):**
   - `CreatePostResponseDto` منتظر فیلد تودرتوی `post` است در حالی که بک‌اند آبجکت پست را در ریشه JSON برمی‌گرداند؛ پست جدید در `FeedDao` درج نمی‌شود؛ و کوئری `GetFollowingFeed` در بک‌اند پست‌های خود کاربر (`OR p.user_id = $1`) را شامل نمی‌شود.
2. **خراب شدن فایل MP4 خروجی در برش سریع ویدیو (`VideoTrimmerScreen.kt:367-408`):**
   - شرط `if (bufferInfo.presentationTimeUs >= startUs)` کی‌فریم (I-frame) ابتدایی حاصل از `SEEK_TO_PREVIOUS_SYNC` را دور می‌ریزد و باعث کرش `MediaMuxer.stop()` یا تصویر سبز/سیاه در ابتدای ویدیو می‌شود.
3. **کپی هم‌زمان کل ویدیوی خام در `cacheDir` و نشت حافظه پنهان (`AddPostScreen.kt:174-181`, `AddPostViewModel.kt:235-282`).**
4. **نبود فشرده‌سازی/ترنسکد ویدیو (`PostMediaUploader.kt:108-186`)** که باعث رد شدن ویدیوهای دوربین (بالای ۱۵ مگابایت) می‌شود.
5. **آپلود کل فایل صوتی بدون برش، نبود پیش‌نمایش صوتی در `MusicTrimBottomSheet` (`AddPostScreen.kt:1018`) و نمایش نام `"audio:100004523"` (`AddPostViewModel.kt:341`).**
6. **نصف شدن رزولوشن عکس‌ها به دلیل `inSampleSize` توانی از ۲ و نبود چرخش EXIF در آواتار پروفایل (`PostMediaUploader.kt:243`, `ProfileAvatarUploader.kt:81`).**
7. **پاک شدن عکس‌های قبلی هنگام زدن دکمه `+` برای افزودن عکس جدید در کامپوزر (`AddPostViewModel.kt:200`).**
8. **عدم ارسال موقعیت مکانی (`selectedLocation`) به بک‌اند (`AddPostViewModel.kt:454`, `FeedDto.kt:209`).**
9. **نبود قفل پریمیوم روی گزینه `hideLikeCount` (`AddPostScreen.kt:883`)** که باعث خطای `403` بعد از اتمام آپلود سنگین مدیا می‌شود، و نادیده گرفته شدن `commentsDisabled` در بک‌اند.
10. **نبود آپلود پس‌زمینه (`WorkManager`)، نبود پاک‌سازی فایل‌های آپلودشده روی S3 در صورت لغو (`POST /v1/uploads/delete`)، نبود ذخیره پیش‌نویس (Draft)، و نبود هایلایت/اتوکامپلیت زنده `@mention` و `#hashtag` در کپشن پست.**

---

### بخش D: تجربه شبکه اجتماعی (پروفایل، کامنت‌ها، ریلز، استوری، ذخیره‌شده‌ها، اعلان‌ها — استاندارد X / Threads / Instagram)

1. **غیب شدن پاسخ‌های سطح ۲ به بعد در کامنت‌ها (`CommentsBottomSheet.kt:329-388`) و مخفی ماندن پاسخ‌های دارای والد در صفحات بعد.**
2. **ارسال اشتباه نوتیفیکیشن `"comment_reply"` به صاحب پست به‌جای نویسنده کامنت والد در بک‌اند (`internal/comments/service.go:123-135`).**
3. **خالی بودن نام و آواتار تمام پست‌های ذخیره‌شده در `SavedPostsScreen` به دلیل نبود `@SerialName("profiles")` در `SavedPostDto` (`SavedPostsApi.kt:40`) و نمایش تاریخ خام ISO.**
4. **غیرفعال بودن دکمه ۳ نقطه در `OtherUserProfileScreen.kt:270`، فیک بودن دکمه `+` و ریپورت در `OwnProfileScreen.kt:231, 258`، و بی‌عمل بودن دکمه‌های کامنت/اشتراک/منو در `ProfilePostCard` (`ProfileParityComponents.kt:738, 799`).**
5. **خراب بودن دکمه اشتراک‌گذاری، خروج از ریلز هنگام زدن دکمه کامنت، و باز نشدن ریلزهای پروفایل در `ReelsViewerScreen.kt:128-137` و `VistaShell.kt:720-724`.**
6. **استاب بودن `HashtagPostCard` (`HashtagPostsScreen.kt:200`)، نبود صفحه‌بندی هشتگ در `SearchViewModel.kt:285` و خالی بودن کال‌بک‌های `searchRoot` در `AppNavGraph.kt:549`.**
7. **استوری‌ها:** عدم آپدیت UI پس از رای در نظرسنجی (`StoryPlayerViewModel.kt:48`)، قطع نشدن موزیک استوری هنگام نگه‌داشتن انگشت (`StoryPlayerScreen.kt:471`)، صفحه سیاه پیش‌نمایش ویدیو در `StoryEditorScreen.kt:318`، غیرقابل جابجایی/حذف بودن استیکرها، فیک بودن ریپورت استوری، و نبود کامل **هایلایت‌ها (`/v1/highlights/*`)** و **تنظیمات استوری (`/v1/me/story-settings`)**.
8. **تقویم میلادی به‌جای شمسی (جلالی):** در `PersianFeedTime.kt:38`، `ProfileParityComponents.kt:1149` و `NotificationsScreen.kt:764` تاریخ‌ها فقط با ارقام فارسی ولی به میلادی (`۲۰۲۶/۱۰/۲۳`) نمایش داده می‌شوند نه شمسی (`۱۴۰۵/۰۸/۰۱`).

---

## ۵. نقشه راه اجرایی فازبندی‌شده (Phased Execution Roadmap)

> [!IMPORTANT]
> این چک‌لیست مرجع اجرایی گام‌به‌گام است. در زمان اجرای هر فاز، پس از تکمیل و تست هر مورد، چک‌باکس آن (`- [x]`) و نوار پیشرفت بالای همین فایل باید بلافاصله آپدیت شود.

### فاز ۱ (P0): پایداری هسته، امنیت شبکه، نشست‌ها و یکپارچگی با پنل مدیریت (`:core:*`, `:feature:auth`, `app`)
- [x] **[P0-1][Network & Error Parsing]** اصلاح `BackendErrorParser.kt:13-33` و `ErrorClassifier.kt:12-78` برای پشتیبانی کامل از هر دو فرمت خطای بک‌اند (`{"error":{"code","message"}}` و `{"error":"..."}`)، تشخیص پیام‌های فارسی در فیلد `flatError`، تشخیص کد `"banned"` در `message`، ترجمه دقیق `feature_disabled` بر اساس فیلد `feature`، و تبدیل خودکار `retrofit2.HttpException` به `RemoteFailure`.
- [x] **[P0-2][Dynamic TLS Pinning]** اصلاح `BootstrapStatusClient.kt:51-71` (پارس کردن `expires_at` به `epochSecond` و تطبیق نسخه قرارداد با بک‌اند)، اصلاح `PolicyTrustManager` در `TlsPolicy.kt:138-163` با استفاده از `X509TrustManagerExtensions(delegate)` برای پشتیبانی از `checkServerTrusted(chain, authType, host)` در حضور `network_security_config.xml`، حذف `runBlocking` از `FoundationModule.kt:98-115` با کش `AtomicReference`، و افزودن هدر `X-Device-ID` و فیلد `app_version` به `TlsFailureReporterImpl.kt:35-43`.
- [x] **[P0-3][System Control Plane & Gates]** ارتقای `BootstrapStatusClient.kt` و `StartupResolver.kt` برای پارس کردن کامل `/api/v1/system/status` شامل `feature_flags`, `features`, `kill_switches`, `alerts`, `fcm_resync_epoch`؛ افزودن پولینگ دوره‌ای (۶۰ ثانیه‌ای و هنگام Resume)؛ سیم‌کشی ناوبری سراسری به `AppRoute.Banned` (`BannedScreen.kt`) در صورت دریافت HTTP 403 `banned`؛ و افزودن هندلینگ HTTP 503 `maintenance_mode` در حین کار به همراه تایمر بازیابی خودکار ۱۰ ثانیه‌ای در `MaintenanceScreen.kt`.
- [x] **[P0-4][Auth Token Refresh Race Condition]** محافظت از `SingleFlightSessionRefreshCoordinator.refresh` (`SessionRefreshCoordinator.kt:45-90`) با `Mutex`؛ بررسی اینکه آیا توکن در `sessionStore` قبلاً توسط درخواست دیگری چرخانده (Rotate) شده است یا خیر قبل از ارسال درخواست شبکه؛ پاک کردن `sessionStore` در خطای 401 فقط در صورتی که توکن فعلی همان توکن ارسال‌شده باشد؛ و فراخوانی `authenticationStateOwner.refreshSession()` در `ChatSessionRefreshModule.kt:14-22`.
- [x] **[P0-5][Unified Device ID & Active Sessions]** یکسان‌سازی `X-Device-ID` پایدار بین `InternalAuthorizationInterceptor.kt:16-39`، `DeviceIdProvider.kt` و `ChatDataModule.kt:159-165`؛ تکمیل اندپوینت‌های نشست در `ActiveSessionsApi.kt` (`POST /v1/sessions/register`, `validate`, `touch`, `terminate-others` و ارسال `@Query("current_session_id")` در `GET /v1/sessions/active`)؛ و هندل کردن رویداد وب‌سوکت `"session_terminated"` برای خروج فوری.
- [x] **[P0-6][Push Token, Deep Links & Self-Updater]** مقید کردن کش توکن FCM به `userId` در `PushTokenRegistrar.kt:61-87`، پشتیبانی از `fcm_resync_epoch` و حذف توکن در زمان خروج؛ افزودن دامنه‌های `coffevista.ir` و `www.coffevista.ir` به `AndroidManifest.xml:101` و `DeepLinkCoordinator.kt:103`؛ پشتیبانی از مسیرهای `/u/{username}`، `/join/{code}` و `/payment/callback` در `DeepLinkCoordinator.kt:68-90`؛ پشتیبانی از `data["deeplink"]` و انواع نوتیفیکیشن مدریشن در `MainActivity.kt:308-321`؛ و تزریق و فراخوانی `VistaUpdateManager.kt` در `MainActivity.kt` و `AboutSettingsScreen.kt:195`.
- [x] **[P0-7][Shell Navigation & Unread Badge]** اتصال شمارنده پیام‌های خوانده‌نشده (`chatBadgeCount`) در `AppNavGraph.kt` و `ShellFeatureContent.kt:151`؛ اصلاح ناوبری دیپ‌لینک گروه (`ShellDeferredKind.GROUP`)، کلیک روی گروه‌های برتر (`ServicesTopGroups`) و دکمه چت در `NearbyScreen` (`VistaShell.kt:196-206, 497-543`) برای باز کردن صحیح مکالمه در تب چت.

---

### فاز ۲ (P0/P1): رفع باگ‌های بحرانی و ارتقای تلگرامی سیستم چت (`:feature:chat`)
- [x] **[P0-8][Chat Data Corruption Fix]** حذف کامل fallback به `cleanTitle` در `OfflineFirstChatRepository.kt:181-184` و `ChatModels.kt:30-38` (`peerIdentityKey`) تا هرگز مکالمات دو کاربر هم‌نام با هم ادغام و حذف نشوند!
- [x] **[P0-9][Chat Realtime & WebSocket Fix]** حذف فالبک اشتباه `/ws` از `OkHttpChatWebSocketFactory.connect` (`ChatRealtime.kt:238-252`) و استفاده از هدر `Authorization: Bearer` در فالبک SSE (`/v1/chat/stream`)؛ حفظ کد وضعیت 401 در استثنای وب‌سوکت برای اجرای رفرش توکن؛ اصلاح `JsonObject.string(key)` در `ChatRealtime.kt:180` برای عدم تبدیل `JsonNull` به `"null"`؛ و جلوگیری از بازنویسی اطلاعات مکالمه توسط پیلود ناقص `CONVERSATION_UPDATED` (`ChatRealtime.kt:75-79`).
- [x] **[P0-10][Chat Connection State & Presence]** تزریق `NetworkMonitor` به `ChatRealtimeCoordinator` (`ChatRealtime.kt:465, 501-518`) تا در زمان قطع اینترنت یا خطای اتصال وضعیت واقعی `DISCONNECTED` / `RECONNECTING` را منتشر کند (حذف دروغ `CONNECTED` در بلوک `catch`) و به‌محض وصل شدن شبکه فالبک خواب ۴۵ ثانیه‌ای را بشکند؛ و افزودن ارسال دوره‌ای `POST /v1/presence/update` و تایپینگ از طریق وب‌سوکت.
- [x] **[P0-11][Secret Chat & E2EE Security Fixes]**
  - رمزنگاری متن ویرایش‌شده در `OfflineFirstChatRepository.editMessage` (`خط ۱۹۵۶`) با `e2eeService.encryptText` و رمزگشایی `RealtimeEvent.MessageUpdated` (`خط ۲۰۴۸`) در چت‌های محرمانه.
  - رمزنگاری بایت‌های فایل پیوست (AES-256-GCM) قبل از آپلود در چت محرمانه (یا فعال‌سازی گارد خط ۱۴۰۶ و ۱۴۸۶ تا زمان تکمیل رمزنگاری فایل).
  - اعمال `WindowManager.LayoutParams.FLAG_SECURE` در `MessageDetailScreen` (`ChatScreens.kt:1656`) هنگام فعال بودن `isSecretChat`.
  - اصلاح تایمر حذف خودکار در `MessagesViewModel.kt:830-871`: ذخیره زمان فعال‌سازی (`enabledAt`) برای عدم حذف تاریخچه قبلی، و شروع شمارش معکوس فقط پس از خوانده شدن پیام (`MessageStatus.READ` / اولین نمایش).
  - افزودن هشدار امنیتی تغییر کلید مخاطب (TOFU) و فراخوانی `refreshConversationPreview` پس از `redecryptSecretMessages` (`OfflineFirstChatRepository.kt:892, 950`).
  - فیلتر کردن چت‌های محرمانه در `ForwardMessageSheet` (`ChatScreens.kt:2995`) یا بازرمزنگاری محلی هنگام فوروارد.
- [x] **[P1-12][Group E2EE Parity]** پیاده‌سازی کامل رمزنگاری گروهی (`VE2E_GROUP_V1:` با HKDF `vista-e2e-group-v1` و اندپوینت `GET /v1/chat/conversations/{id}/keys`) در `ChatE2EEService.kt` و `ChatApi.kt` مطابق `E:\vista\lib\features\chat\services\group_e2e_service.dart`.
- [x] **[P1-13][Read Receipt Performance & Pending Delete Cleanup]** جایگزینی حلقه رمزگشایی/رمزنگاری کل پیام‌های چت در `RealtimeEvent.Read` (`OfflineFirstChatRepository.kt:2080-2090`) با یک کوئری مستقیم SQL `UPDATE` در `ChatDao`؛ و لغو آپلود فعال + حذف `TransferRecordEntity` و `PendingOperationEntity` هنگام حذف پیام در `OfflineFirstChatRepository.deleteMessage` (`خط ۱۹۷۴`).
- [x] **[P1-14][Chat Error Envelopes]** اصلاح `OfflineFirstChatRepository.remoteCall` (`خط ۲۵۱۳`) برای پارس کردن بدنه خطای بک‌اند توسط `BackendErrorParser` و نمایش پیام‌های خطای دقیق فارسی سرور به کاربر.
- [x] **[P1-15][Chat Upload Policy & Video Formats]** اصلاح `ChatUploadPolicy.kt:85-106, 192` برای اجازه ارسال تمام فرمت‌های استاندارد فایل/سند (ZIP, DOCX, XLSX, TXT, APK و غیره) در `AttachmentKind.DOCUMENT`، افزودن برندهای مدرن MP4/HEVC/3GP/WebM به لیست ویدیوهای مجاز، و تغییر پوشه آپلود ویدیو به `"videos"` در `ChatMediaUploader.kt:142`.
- [x] **[P1-16][Voice Recorder & Player Telegram UX]**
  - اصلاح محاسبه زمان ضبط در `VoiceRecorderDock.kt:215, 509` با `SystemClock.elapsedRealtime()` تا ویس‌های کوتاه ۱ ثانیه‌ای حذف نشوند.
  - انتقال آرایه آمپلی‌تود واقعی ضبط‌شده (`waveformAmplitudes`) به متادیتای پیام صوتی و رندر موج واقعی در `VoicePlayerBubble.kt:351` به‌جای فرمول ثابت.
  - ایجاد `VoicePlaybackCoordinator` برای جلوگیری از پخش هم‌زمان چند ویس، پشتیبانی از پخش خودکار ویس بعدی (Sequential Autoplay)، و تغییر سرعت پخش (`1x / 1.5x / 2x`).
- [x] **[P1-17][Fullscreen Media Viewer & Pinch-to-Zoom]** جایگزینی `AttachmentViewer` قدیمی در `ChatScreens.kt:3025` با `FullscreenMediaViewerDialog.kt` (مجهز به Media3 ExoPlayer)، افزودن قابلیت Pinch-to-Zoom و Double-Tap Zoom روی تصاویر، و افزودن `HorizontalPager` برای ورق زدن رسانه‌های آلبوم و کل چت.
- [x] **[P1-18][In-Chat Search, Unread Divider & Multi-Pin Bar]**
  - اصلاح جستجوی داخل چت در `ChatScreens.kt:2173-2177` تا کل تاریخچه پیام‌ها حفظ شود، عبارت جستجو هایلایت گردد و لیست با دکمه‌های بالا/پایین بین نتایج اسکرول کند.
  - ثبت `firstUnreadMessageId` قبل از `markRead` در `MessagesViewModel` و رندر کردن `UnreadMessagesDivider.kt` در لیست پیام‌ها.
  - جایگزینی `PinnedMessageBanner` با `PinnedMessagesBar.kt` در `ChatScreens.kt:2002` و فرمت کردن پیش‌نمایش پست‌های اشتراکی پین‌شده و ریپلای‌شده (`SharedPostParser`) به‌جای نمایش JSON خام (`OfflineFirstChatRepository.kt:1342`, `MessagesViewModel.kt:381`, `PinnedMessagesBar.kt:72`).
- [x] **[P1-19][Telegram Gestures, Context Menu & Partner/Group Details]**
  - اصلاح جهت سوایپ و تراز آیکون در `SwipeToReplyLayout.kt:40-48, 120-128` با استفاده از `AbsoluteAlignment` (سوایپ یکپارچه به سمت چپ برای همه پیام‌ها مانند تلگرام).
  - اتصال `bubbleBounds` فقط به `Surface` حباب پیام در `ChatScreens.kt:3842` (برای رفع بریدگی دور ری‌اکشن‌ها در `TelegramMessageContextMenu.kt:170`) و فعال‌سازی کپی/ویرایش کپشن پیام‌های رسانه‌دار در `MessageContextActionPolicy.kt:67, 76`.
  - افزودن `SwipeToReplyLayout`، منوی کانتکست و `ReactionRow` به `MediaAlbumBubble` (`ChatScreens.kt:2288-2307`).
  - سیم‌کشی گزینه «گفتگوی محرمانه جدید» (`ChatScreens.kt:518`)، منوی بلاک/ریپورت/پاک‌سازی تاریخچه در `PartnerDetailsScreen` (`ChatScreens.kt:6611, 6671, 6811`) و کلیک‌پذیر کردن تب‌های رسانه/فایل/صدا/لینک در جزئیات چت و گروه (`GroupDetailsScreen.kt:850-980`).

---

### فاز ۳ (P0/P1): رفع باگ‌های ارسال پست، فید، الگوریتم و مدیا (`:feature:feed`, `E:\vista-backend`)
- [x] **[P0-20][Following Feed Pagination & Personalized Feed Algorithm]**
  - تغییر `@Query("cursor")` به `@Query("offset")` در `FeedApi.kt:14-18` (`getFollowingFeed`) تا صفحه‌بندی تب «دنبال‌شده‌ها» از صفحه ۱ فراتر برود.
  - افزودن اندپوینت فید الگوریتمی `GET /v1/feed` به `FeedApi.kt` و اتصال آن در `OfflineFirstFeedRepository.kt`.
  - پیاده‌سازی ردیابی دیده‌شدن پست‌ها در Viewport (`"view"` پس از ۸۰۰ میلی‌ثانیه با ۶۰٪ دید و `"dwell"` با زمان توقف) در `FeedScreen.kt` و ارسال دسته‌ای (Batched هر ۲ ثانیه) به `POST /v1/feed/events` در `OfflineFirstFeedRepository.kt:275-287`.
  - جداسازی `LazyListState` برای تب‌های `Following` و `Explore` در `FeedScreen.kt:190`.
- [x] **[P0-21][Immediate Post Visibility After Creation]**
  - اصلاح نوع خروجی `FeedApi.createPost` از `CreatePostResponseDto` به `PostDto` (`FeedDto.kt:226-231`).
  - درج فوری پست جدید در `FeedDao` (در ابتدای فید `following` و `explore`) داخل `AddPostViewModel.submitPost()` (`خط ۴۷۰`).
  - اصلاح کوئری `GetFollowingFeed` در بک‌اند (`E:\vista-backend\internal\posts\repository.go:691`) برای شامل شدن پست‌های خود کاربر (`WHERE f.follower_id = $1 OR p.user_id = $1`).
- [x] **[P0-22][Video Trimmer Keyframe Fix, Compression & Cache Cleanup]**
  - اصلاح `VideoTrimmerScreen.trimVideoFast` (`خط ۳۶۷-۴۰۸`): خارج کردن `seekTo` از حلقه ترک‌ها، نوشتن نمونه‌ها از اولین Sync Keyframe (`basePtsUs`) و نرمال‌سازی `presentationTimeUs` (یا استفاده از Media3 `Transformer`) تا فایل MP4 خروجی خراب نشود.
  - حذف فراخوانی هم‌زمان `viewModel.onVideoSelected(uri)` هنگام باز کردن صفحه برش ویدیو در `AddPostScreen.kt:174-181` و پاک کردن فایل‌های موقت ویدیو از `context.cacheDir` در `onVideoTrimmed`, `removeVideo`, `onCleared`.
  - افزودن فشرده‌سازی ویدیو (Media3 `Transformer` به H.264 720p/1080p) و بررسی حجم فایل بلافاصله پس از برش.
- [x] **[P0-23][Audio Trimming, Preview & Metadata Extraction]**
  - پیاده‌سازی برش واقعی فایل صوتی بین `musicStartMs` و `musicEndMs` در `PostMediaUploader.kt:188-241` قبل از بررسی حجم و آپلود به S3 (و افزودن فیلدهای `music_start_ms` / `music_end_ms` در بک‌اند `internal/posts/repository.go`).
  - افزودن پلیر پیش‌نمایش صدا (`ExoPlayer`) و تاگل حالت موزیک پس‌زمینه در `MusicTrimBottomSheet` (`AddPostScreen.kt:1018-1061`).
  - استخراج نام واقعی آهنگ و خواننده از تگ‌های ID3 (`MediaMetadataRetriever`) و `OpenableColumns.DISPLAY_NAME` در `AddPostViewModel.kt:341` به‌جای نمایش `"audio:100004523"`.
- [x] **[P0-24][Feed Media Aspect Ratio, Covered Videos & Video Playback Coordinator]**
  - نوشتن تابع پارس نسبت تصویر (`"4:5" -> 0.8f`, `"16:9" -> 1.777f`, `"1:1" -> 1f`) و حذف محدودیت ارتفاع `.coerceAtMost(280.dp)` در `FeedScreen.kt:1054, 1092` و `AppNavGraph.kt:908` تا عکس‌های مربعی و عمودی کراپ نشوند.
  - اصلاح `ProfilePostCard` (`ProfileParityComponents.kt:763`)، `HashtagPostCard` (`HashtagPostsScreen.kt:223`) و `SavedPostsScreen.kt:261` تا ابتدا `!post.videoUrl.isNullOrBlank()` را بررسی کنند و ویدیوهای دارای کاور را به‌عنوان ویدیو (نه عکس ثابت) رندر نمایند.
  - ایجاد `FeedVideoPlaybackCoordinator` به همراه کش دیسک ۲۵۰ مگابایتی (`SimpleCache` + `CacheDataSource.Factory`) تا در هر لحظه فقط ۱ ویدیوی متمرکز در فید پخش شود، و جلوگیری از ساخت `ExoPlayer` در `PostMusicBubble` (`FeedScreen.kt:1224`) تا قبل از فشردن دکمه پخش.
  - اصلاح تداخل `.clickable` در `VistaFeedVideoPlayer.kt:162` با `detectTapGestures` (برای کار کردن دابل‌تپ لایک روی ویدیوها) و پاس دادن `onVideoClick = null` در `PostDetailScreen.kt:312` تا ویدیو در صفحه جزئیات پست قابل توقف و پخش مجدد باشد.
- [x] **[P1-25][Image Scaling, EXIF Orientation & Composer Fixes]**
  - اصلاح `PostMediaUploader.compressImage` (`خط ۲۴۳`) و `ProfileAvatarUploader.compressAvatar` (`خط ۸۱`) برای اعمال دقیق `Bitmap.createScaledBitmap` و پشتیبانی کامل از تمام حالات چرخش و آینه‌ای `ExifInterface`.
  - تغییر `AddPostViewModel.onImagesSelected` (`خط ۲۰۰`) برای الحاق عکس‌های جدید (`selectedImages + uris`) به‌جای پاک کردن عکس‌های قبلی.
  - افزودن فیلد `location` به `CreatePostRequestDto`، `PostDto` و بک‌اند (`internal/posts`).
  - قفل کردن سوئیچ‌های `hideLikeCount` و `hideCommentCount` برای کاربران غیرپریمیوم در `AddPostScreen.kt:883-917`، اصلاح متن ارتقا به «تا ۱۰۰۰ کاراکتر» (`خط ۹۴۸`)، افزودن پیش‌نمایش ویدیو با `ExoPlayer` در کامپوزر، و هایلایت/اتوکامپلیت زنده `#hashtag` و `@mention` در مکان‌نمای متن (`AddPostScreen.kt:1066`).
- [x] **[P1-26][Background Post Upload, S3 Cleanup & Drafts]**
  - انتقال آپلود پست به سرویس/ورکر پس‌زمینه (`WorkManager`) با نمایش بنر پیشرفت و دکمه تلاش مجدد در بالای `FeedScreen` (مانند اینستاگرام و نسخه فلاتر).
  - فراخوانی `POST /v1/uploads/delete` برای پاک کردن فایل‌های orphan روی S3 در صورت لغو یا خطای ساخت پست.
  - افزودن قابلیت ذخیره و بازیابی پیش‌نویس (Draft) در دیالوگ خروج `AddPostScreen.kt:296`.

---

### فاز ۴ (P1/P2): تکمیل تجربه شبکه اجتماعی، کامنت‌ها، ریلز، استوری، پروفایل و قابلیت‌های فراموش‌شده فلاتر
- [x] **[P0-27][Threaded Comments & Reply Notifications]**
  - اصلاح ساختار درختی/گروه‌بندی پاسخ‌ها در `CommentsBottomSheet.kt:329-388` تا پاسخ به پاسخ‌ها (سطح ۲ به بعد) و پاسخ‌های دارای والد در صفحات بعد به‌درستی با خط اتصال عمودی (سبک X/Threads) نمایش داده شوند.
  - اصلاح باگ بک‌اند در `E:\vista-backend\internal\comments\service.go:123-135` تا نوتیفیکیشن `"comment_reply"` برای نویسنده کامنت والد ارسال شود نه صاحب پست!
  - همگام‌سازی شمارنده کامنت‌ها در `FeedDao` (`adjustCommentCount`) پس از ثبت یا حذف کامنت در `CommentsViewModel.kt`.
  - اتصال جستجوی منشن کامنت‌ها به `SearchRepository.searchUsers` (`CommentsViewModel.kt:80`) و کلیک‌پذیر کردن `@username` در `ClickableCommentText` (`CommentsBottomSheet.kt:839`).
- [x] **[P0-28][Saved Posts, Profile & Reels Fixes]**
  - افزودن `@SerialName("profiles")` به `SavedPostDto.author` در `SavedPostsApi.kt:40`، فرمت زمان نسبی فارسی و سیم‌کشی دکمه‌های لایک/کامنت/اشتراک/ذخیره در `SavedPostsScreen.kt`.
  - سیم‌کشی دکمه `+` در `OwnProfileScreen.kt:231` به `AddPostRoute` (`AppNavGraph.kt:575`)، حذف گزینه ریپورت از پروفایل خود کاربر، کلیک‌پذیر کردن دکمه ۳ نقطه در `OtherUserProfileScreen.kt:270-277` (برای بلاک، گزارش تخلف و اشتراک پروفایل)، و تکمیل `ProfilePostCard` (`ProfileParityComponents.kt:738-820`) با منوی حذف پست، کاروسل چندعکسی، پخش ویدیو و موزیک.
  - در `ReelsViewerScreen.kt` و `VistaShell.kt:720-724`: باز کردن `CommentsBottomSheet` و `PostShareBottomSheet` به‌صورت شیت مودال روی خود ریلز، بارگذاری پست اولیه از `getPostById` در صورت نبودن در کش اکسپلور (برای پخش ریلزهای پروفایل)، افزودن صفحه‌بندی خودکار در انتهای لیست ریلزها، و رعایت `hideLikeCount` / `hideCommentCount`.
  - جایگزینی کارت ناقص `HashtagPostCard` در `HashtagPostsScreen.kt:200` با کامپوننت کامل `FeedPostCard`، افزودن صفحه‌بندی هشتگ در `SearchViewModel.kt:285`، و سیم‌کشی کال‌بک‌های `searchRoot` در `AppNavGraph.kt:549`.
- [x] **[P1-29][Stories Player, Editor, Highlights & Privacy Parity]**
  - آپدیت خوش‌بینانه (Optimistic) وضعیت نظرسنجی در `StoryPlayerViewModel.kt:48-55` پس از ثبت رای.
  - توقف پخش موزیک استوری هنگام نگه‌داشتن انگشت (`isPaused`) در `StoryPlayerScreen.kt:471` و بی‌صدا کردن ترک ویدیو در صورت وجود موزیک.
  - اتصال دیالوگ گزارش استوری (`StoryPlayerScreen.kt:839`) به اندپوینت واقعی `POST /v1/stories/{storyId}/report`.
  - رندر پیش‌نمایش ویدیو با `ExoPlayer` در `StoryEditorScreen.kt:318` و افزودن قابلیت جابجایی (Drag/Scale) و حذف استیکرها.
  - پیاده‌سازی کامل **هایلایت‌های استوری** (`/v1/highlights/*`) در ریپازیتوری، پروفایل کاربری و پلیر استوری.
  - پیاده‌سازی **تنظیمات حریم خصوصی استوری** (`GET/PATCH /v1/me/story-settings` و `GET /v1/stories/{id}/reply-access`) و استیکر جعبه سوال (`question` answer & list).
- [x] **[P1-30][Notifications & Moderation Flows]**
  - رفع تداخل `markAllAsRead()` و `refresh()` در `NotificationsScreen.kt:119-121` تا نشانگرهای خوانده‌نشده در بدو ورود به صفحه از بین نروند.
  - نگاشت `"post_admin_edit"` و `"post_admin_removed"` در `NotificationModels.kt:173-185` برای باز کردن صفحه اعتراض (`AppealScreen`)، و ترجمه متن انگلیسی منشن‌ها به فارسی (`خط ۲۰۵`).
- [x] **[P2-31][Missing Flutter Features Migration (Verification, Payments, Account Deletion, Nearby)]**
  - تکمیل صفحه درخواست تیک تأیید هویت (`VerificationRequestScreen.kt:178-231`): انتخاب تصویر مدرک، آپلود امن و فراخوانی `POST /v1/me/verify`.
  - افزودن پرداخت درون‌برنامه‌ای کافه‌بازار (`POST /v1/payment/bazaar-verify`)، بسته‌های سکه (`GET /v1/payment/packages`) و فروشگاه نشان‌ها (`BadgeStore`) در `:feature:profile`.
  - افزودن فلو حذف حساب کاربری با OTP (`POST /v1/auth/account-deletion/request-otp`, `confirm`) و گزینه غیرفعال‌سازی 2FA (`POST /v1/auth/2fa/remove`) در تنظیمات امنیت.
  - افزودن `POST /v1/nearby/undo` و تبدیل مختصات به نام شهر/استان (`Geocoder`) در `NearbyViewModel.kt:74-80`.

---

### فاز ۵ (P2/P3): پولیش نهایی UI/UX، تقویم شمسی و انیمیشن‌های سطح اول
- [x] **[P2-32][Shamsi / Jalali Calendar Everywhere]** افزودن مبدل دقیق تاریخ میلادی به هجری شمسی (جلالی) در `PersianFeedTime.kt` و استفاده یکپارچه از آن در فید، کامنت‌ها، پروفایل (`ProfileParityComponents.kt:1149`)، استوری‌ها (`StoryPlayerScreen.kt:891`)، پست‌های ذخیره‌شده (`SavedPostsViewModel.kt:85`) و اعلان‌ها (`NotificationsScreen.kt:764`) به همراه ارقام فارسی.
- [x] **[P3-33][Micro-Interactions, Haptics, Pull-to-Refresh & Fullscreen Image Zoom]**
  - افزودن انیمیشن فنری (`Spring` scale bounce) و بازخورد لمسی (`HapticFeedback`) به دکمه‌های لایک، بوکمارک و ری‌اکشن‌ها در سراسر فید و ریلز.
  - افزودن `PullToRefreshBox` به `PostDetailScreen`، `HashtagPostsScreen` و `SavedPostsScreen`.
  - افزودن نمایشگر تمام‌صفحه عکس با قابلیت Pinch-to-Zoom و ورق زدن در `FeedScreen` و `PostDetailScreen`.
