# بازطراحی، رفع باگ‌ها و ارتقای سرعت صفحه گفتگوها و پروفایل به استاندارد تلگرام

**Type:** feature / bugfix / performance / UI polish  
**Status:** completed  
**Progress:** `[██████████] 100%`

## ۱. صورت مسئله و هدف (Problem / Goal)
1. **صفحه گفتگوها (`ConversationsScreen`):**
   - رفع نمایش مکرر نوار نارنجی «نمایش گفتگوهای ذخیره‌شده / تلاش مجدد» با لودینگ بی‌پایان، در حالی که اتصال برقرار است (چراغ سبز آنلاین است).
   - حل مشکل واکشی با تأخیر پیام‌های جدید و ارتقای سرعت به‌روزرسانی مکالمات به سطح بی‌درنگ و استاندارد تلگرام (Telegram-level speed).
2. **صفحه پروفایل (`OwnProfileScreen`, `OtherUserProfileScreen`, `ProfileParityComponents`):**
   - رفع خطای کرش/نمایش خطای خام جیسون (`Unexpected JSON token at offset 3198: Expected quotation mark '"', but had 'EOF' instead at path: $.posts[4].id`).
   - تنظیم اندازه صفحه واکشی پست‌های پروفایل (`profileLimit`) منطبق بر فلاتر (`15` به جای `30`) برای جلوگیری از ناقص ماندن پاسخ سرور.
   - افزودن دکمه «تلاش مجدد» در حالت خطای بارگذاری پست‌های پروفایل.
   - زیباسازی و پولیش گرافیکی، تب‌ها، آمار، نشان‌ها و تم تاریک/روشن نشان کاربری (`MemberOrderBadge`).

## ۲. ریشه‌یابی و معماری پیاده‌سازی (Root Cause & Design)
- **نوار نارنجی تلاش مجدد در صفحه گفتگوها:**
  - در `OfflineFirstChatRepository.kt` ماکزیمم صفحات رفرش روی ۲۰ صفحه تنظیم شده بود و `syncFromNetwork` از `reset = false` استفاده می‌کرد که باعث می‌شد پیام‌های قدیمی پیج شوند و ریکوئست‌ها تایم‌اوت بخورند. با تنظیم `MAX_CONVERSATION_PAGES_PER_REFRESH = 2` و `reset = true` مشکل تایم‌اوت رفرش کاملاً برطرف شد.
  - کامپوننت `ConnectionBanner` به صورت هاردکد دارای یک `CircularProgressIndicator` بود که حتی بدون در حال رفرش بودن، مدام می‌چرخید. پارامتر `isRefreshing` به آن اضافه شد و تنها در زمان فعال بودن رفرش می‌چرخد.
  - بنر نارنجی به عدم اتصال وب‌سوکت مشروط شد (`state.isOffline && state.connectionState != RealtimeConnectionState.CONNECTED`).
- **تأخیر در واکشی پیام‌های جدید:**
  - متد `syncFromNetwork()` همیشه صفحه اول را که حامل جدیدترین گفتگوهاست واکشی می‌کند.
  - در حالت فال‌بک پولینگ (`ChatRealtime`)، فاصله زمانی به ۳ ثانیه در فورگراند کاهش یافت.
- **خطای جیسون پست‌های پروفایل:**
  - درخواست با `limit = 30` توسط سرور Truncate می‌شد که با کاهش به `limit = 15` مشکل برطرف شد.
  - خطاهای سرور با پیام‌های تمیز و فارسی استاندارد جایگزین شدند.
  - دکمه شکیل «تلاش مجدد» به `ProfilePostsError` و `ProfileEmptyView` افزوده شد.
  - کامپوننت نشان عضویت (`MemberOrderBadge`) به تم گرادیان غنی دارک/لایت (طلایی برای بنیان‌گذار، آبی برای اولین هزار نفر، بنفش برای پیشگام) ارتقا یافت.

## ۳. فازها و اقدامات (Phases)

### فاز ۱ — اصلاح موتور همگام‌سازی و صفحه گفتگوها (Conversations & Realtime Sync)
- [x] اصلاح `OfflineFirstChatRepository`: همگام‌سازی همیشه صفحه اول را تازه کند (`cursor = null`) و `loadMore` مختص صفحه‌بندی باشد.
- [x] بهینه‌سازی بازه زمانی فال‌بک پولینگ در `ChatRealtime` (از ۸ ثانیه به ۳ ثانیه در فورگراند).
- [x] اصلاح `ConversationsViewModel`: مدیریت بدون گیر افتادن پرچم‌های خطای رفرش و همگام‌سازی تمیز.
- [x] مشروط‌سازی نمایش `ConnectionBanner` به عدم اتصال (`connectionState != CONNECTED`).
- [x] اصلاح اسپینر `ConnectionBanner` جهت نمایش فقط هنگام رفرش فعال و نمایش دکمه تلاش مجدد در زمان ایستا.

### فاز ۲ — اصلاح و ارتقای صفحه پروفایل (Profile Screen Overhaul)
- [x] اصلاح `profileLimit` از ۳۰ به ۱۵ در `OfflineFirstFeedRepository.kt` برای جلوگیری از ترانکیت پاسخ سرور.
- [x] اصلاح مدیریت خطا در `ProfilePostsViewModel` و ارائه پیام‌های فارسی تمیز و عدم پاک شدن پست‌های کش‌شده.
- [x] افزودن دکمه «تلاش مجدد» به `ProfilePostsError` و متصل کردن آن به بازخوانی پست‌ها در `ProfileParityList`.
- [x] بهینه‌سازی و پولیش بصری هدر، بیو، تب‌ها، کشیدن به پایین برای بازخوانی (Pull-to-refresh) و گرادیان تاریک/روشن `MemberOrderBadge`.

### فاز ۳ — تست، بیلد و اعتبارسنجی زنده (Build & Verification)
- [x] اجرای موفق ۱۰۰٪ تست‌های یونیت: `:feature:chat:testDebugUnitTest`, `:feature:feed:testDebugUnitTest`, `:feature:profile:testDebugUnitTest`.
- [x] کامپایل و بیلد نسخه رسمی Release با امضای رسمی (`app-production-release.apk`).
- [x] تولید و قرارگیری فایل در مسیر `E:\vista_release\vista-native-v2.6.3-b4050-production-release.apk` با هش SHA256: `845d514685d7a966fb587ec1d3156c261cb404f18e9d1358f599cea39649bc76`.
- [x] نصب و اعتبارسنجی زنده روی شبیه‌ساز `emulator-5554` با موفقیت کامل و بدون کوچک‌ترین خطا یا کرش.
