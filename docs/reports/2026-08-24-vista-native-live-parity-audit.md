# گزارش سخت‌گیرانهٔ انتقال Vista Flutter به Native

**تاریخ ممیزی:** 2026-08-24  
**وضعیت:** `Blocked — parity not established`  
**پیشرفت ممیزی:** `[███████░░░] 70%`  
**پیشرفت کل مهاجرت با معیار سخت‌گیرانه:** `[██░░░░░░░░] 15%`

## حکم اجرایی

پروژه از نظر معماری و حجم کد پیشرفت دارد، ولی «نزدیک parity» نیست. امتیاز **۱۵٪** عمداً محافظه‌کارانه است: فقط قابلیت‌هایی که با APK تازه، دستگاه/fixture یکسان و مقایسهٔ تصویری پذیرفته شوند در امتیاز اصلی حساب می‌شوند. Native در این ممیزی از startup عبور نکرد؛ بنابراین **۰٪ قابلیت Native دارای parity تازهٔ معتبر** است. ۱۵٪ فقط پیشرفت پیاده‌سازی قابل مشاهده را نشان می‌دهد، نه تجربهٔ قابل تحویل کاربر.

| بُعد | پیاده‌سازی قابل مشاهده | runtime/visual تازه | امتیاز |
|---|---:|---|---:|
| foundation، design system، auth، shell | 55% | startup متوقف است | 5% |
| feed/post/comments/profile/search | 45% | مسیر Native قابل مشاهده نشد | 5% |
| chat/E2EE/realtime/file | 45% | بدون account/conversation واقعی | 3% |
| stories/media/services/nearby | 35% | بدون مسیر runtime معتبر | 2% |
| migration/performance/release QA | 15% | build تازه شکست خورده | 0% |
| **کل parity قابل قبول** | — | — | **15%** |

Flutter (`E:\vista`, `ir.coffevista.vista`) مرجع read-only است. Native فقط با همان دستگاه، 1080×2400، API، theme، locale، fixture واقعی و state تعامل، به‌همراه side-by-side/diff کمّی، پذیرفته می‌شود. build یا unit test یا screenshot قدیمی parity نیست.

## شواهد تازهٔ دستگاه

| مورد | Flutter | Native production | نتیجه |
|---|---|---|---|
| دستگاه | `emulator-5554`، API 13، 1080×2400، proxy=`null` | همان | قابل مقایسه |
| package/activity | `ir.coffevista.vista/.MainActivity` | `ir.coffevista.vista_native.production/ir.coffevista.vista_native.MainActivity` | هویت‌ها درست و جدا هستند |
| نسخهٔ نصب‌شده | 2.6.2 / 4049، 2026-08-15 | 2.6.3 / 4050، 2026-08-22 | Native نصب‌شده جدیدتر است، اما APK checkout فعلی نیست |
| cold launch | بعد از splash به feed skeleton رسید؛ سپس خطای load پیشنهادها مشاهده شد | پس از splash و 7+ ثانیه همچنان «در حال آماده‌سازی ویستا» | **P0: startup Native مانع همهٔ تست‌هاست** |
| feature comparison | feed مرجع دیده شد ولی live data خطای شبکه داشت | feed/chat/profile قابل دسترس نشد | **Blocked؛ diff معتبر نداریم** |

تصاویر با کنترل کامپیوتر از Emulator مشاهده شد. credential/token/plaintext ذخیره یا منتشر نشده است.

## مانع ساخت آخرین APK

`E:\vista_native\gradlew.bat :app:assembleBetaDebug` پس از دریافت Gradle 8.13 در `:core:common:compileKotlin` شکست خورد: Gradle نمی‌تواند `core\\common\\build\\classes\\kotlin\\main` را حذف کند، زیرا پردازش دیگری فایل‌های کلاس را باز نگه داشته است. Android Studio مربوط به `vista_native` باز است؛ برای حفظ تغییرات dirty، آن را اجباری نبستم. پس APK تازهٔ Native ساخته/نصب نشد؛ build Flutter نیز شروع نشد تا checkout بسیار dirty بی‌دلیل تغییر نکند.

## سلامت ورک‌تری و شواهد

- هر دو repository به‌شدت dirty هستند؛ Native شامل حذف/تغییر گسترده، artifactهای build و فایل‌های untracked فراوان است.
- `E:\vista_native\VISTA_NATIVE_MASTER_PLAN.md` حذف‌شده است؛ بدون تعیین مالکیت نباید بازگردانده/بازنویسی شود.
- evidenceهای تاریخی، خصوصاً موارد حذف‌شده، اثبات وضعیت فعلی نیستند.
- تنها تغییر این ممیزی همین گزارش است؛ هیچ فایل موجود، data دستگاه یا APK حذف/بازنویسی نشد.

## ماتریس قابلیت‌ها

`P`=کد قابل مشاهده اما unverified؛ `A`=غایب/غیرفعال؛ `B`=blocked by startup؛ `V`=parity تازه. در این ممیزی هیچ `V` نداریم.

| حوزهٔ Flutter | وضعیت Native | یافته | اولویت |
|---|---|---|---|
| startup/onboarding/login/OTP | B | loading دائمی Native؛ login تازه دیده نشد | P0 |
| shell و tabها | B | Compose shell/route وجود دارد، لمس‌پذیر نیست | P0 |
| feed/post/comments/composer | P/B | `feature:feed`، bottom sheet و ViewModel موجود؛ live/IME/reply اثبات نشده | P0 |
| stories/player/editor | P/B | کد stories زیر `feature:feed` وجود دارد؛ camera/upload/parity اثبات نشده | P0 |
| add post/media/video | P/B | `AddPostScreen`/uploader هستند؛ crop/trim/upload/player واقعی باز است | P0 |
| profile/follow/QR/notes | P/B | UI/repository دیده می‌شود؛ stateهای privacy/offline/live باز است | P1 |
| search/hashtags/history | P/B | module هست؛ Persian/English/keyboard/debounce باز است | P1 |
| chat/realtime/E2EE/files | P/B | Room/cipher/websocket/uploader دیده می‌شود؛ real conversation و fail-closed proof باز است | P0 |
| group/archive/message search/info/emoji/GIF/voice | P/B | کدهایی هست؛ feature parity و keyboard/performance باز است | P1 |
| services/contacts/WebView/game/nearby | P/B | `feature:services` هست؛ permission/privacy/SSO واقعی باز است | P1 |
| settings/theme/sessions/storage/push | P/B | بخش‌هایی حاضر است؛ persistence/background/deep-link باز است | P1 |
| music/premium/badges/store/share | A/P | Flutter featureهای جدا دارد؛ Native top-level feature معادل ندارد | P2 |
| signed upgrade migration | A | session/E2EE/draft/pending/download/rollback واقعی اثبات نشده | P0 |
| performance/release/accessibility | A | benchmark ARM/release، TalkBack، font-scale و ANR evidence تازه نداریم | P1 |

## قابلیت‌های غیرفعال/ناقصِ قطعی

1. `AppRoute.DeferredFeature` برای `POST`، `PROFILE`، `GROUP` و `CHAT` واقعاً به `DeferredDestinationScreen.kt` می‌رود و متن «این مقصد هنوز در نسخه Native آماده نشده است» نمایش می‌دهد. deep linkهای این مسیرها product failure هستند.
2. startup Native در build نصب‌شده P0 failure است؛ تا رفع آن parity هیچ route داخلی معتبر نیست.
3. pipeline ساخت/نصب checkout فعلی با file lock بسته است.
4. Flutter feed در محیط فعلی نیز health gate دارد؛ proxy/DNS/TLS/API و fixture واقعی باید پیش از live test تثبیت شوند.
5. Flutter 13 feature top-level دارد (`auth, chat, emoji, home, music, nearby, onboarding, posts, profile, search, services, settings, share, stories`) و Native 7 (`auth, chat, feed, profile, search, services, shell`). ادغام ماژول‌ها اشکال نیست، اما معادل اجرایی همهٔ زیرقابلیت‌ها باید تک‌به‌تک ثابت شود.

## نقشهٔ ادامه، به ترتیب وابستگی

### Phase 0 — محیط و تکرارپذیری (P0)
- [ ] مالک Android Studio/process نگه‌دارندهٔ lock مشخص و کار unsaved آن حفظ/بسته شود.
- [ ] `assembleBetaDebug` و `assembleProductionDebug` موفق شوند؛ package/version/activity خروجی ثبت شود.
- [ ] Flutter debug APK از `E:\vista` و Native تازه روی همان Emulator نصب شوند.
- [ ] fixture/account تست مجاز و قابل پاکسازی فراهم شود؛ credential/token/plaintext هرگز در evidence/repo/log نرود.
- [ ] proxy/DNS/TLS/API و failure feed Flutter ثابت یا جداگانه ثبت شود.

**گیت:** دو APK تازه، cold launch پایدار، fixture مشترک.

### Phase 1 — startup و dead routeها (P0)
- [ ] علت loading دائمی را با trace startup و logcat فیلترشده در auth/session/network/database رفع کن.
- [ ] `DeferredFeature(POST/PROFILE/GROUP/CHAT)` را به مقصد واقعی وصل کن یا تا زمان تکمیل غیرقابل‌دسترسی و fail-closed کن.
- [ ] cold/warm/restart، offline، expired session، maintenance و onboarding را جفتی مقایسه کن.

**گیت:** 10 cold start بدون hang/ANR و screenshot pair startup/login/onboarding.

### Phase 2 — هستهٔ اجتماعی و media (P0)
- [ ] feed/paging/error/cache، post detail/actions را با post واقعی image/video و caption فارسی بلند مقایسه کن.
- [ ] comments/reply/edit/delete/optimistic/retry و composer+IME را با thread واقعی ثابت کن.
- [ ] add post، picker/crop/trim/compression/upload/cancel/retry/player را end-to-end بررسی کن.
- [ ] stories tray/player/editor/reply/reaction/viewers/upload را به parity برسان.

**گیت:** state matrix، diff کمّی، RTL/font-scale/light-dark، بدون fixture leakage.

### Phase 3 — chat و امنیت (P0/P1)
- [ ] conversation/detail، cursor pagination، reconnect/dedup، read/typing/presence را با گفت‌وگوی واقعی مقایسه کن.
- [ ] E2EE fail-closed، encrypted Room، file/voice/GIF/reaction/reply/forward/edit/delete و cleanup را اثبات کن.
- [ ] groups/archive/message-search/info/emoji recents و keyboard performance را ببند.

**گیت:** بدون plaintext/token روی disk/log؛ proof realtime/file/offline/retry/restart و visual sheets.

### Phase 4 — profile/search/services/settings/notifications (P1)
- [ ] profile/followers/notes/QR/edit/setup و privacy/follow stateها را مقایسه کن.
- [ ] search Persian/English/hashtag/history/keyboard/debounce را ثابت کن.
- [ ] services/contacts/top-groups/game/WebView/nearby را با permission و privacy واقعی اجرا کن.
- [ ] settings/theme/language/sessions/storage و FCM/background/deep link را end-to-end تست کن.

### Phase 5 — باقی‌مانده و upgrade migration (P1/P2)
- [ ] music/premium/badges/store/share receiver/accessibility semantic parity را پوشش بده.
- [ ] signed Flutter→Native upgrade برای secure session/E2EE keys/drafts/pending/download با rollback تست شود.
- [ ] Draft=`zero-import` و Download=`scan + unmatched` حفظ شود؛ URL/entity هرگز حدس زده نشود.

### Phase 6 — release gate (P1)
- [ ] API 24/33/35+، light/dark، RTL، font 1.0/1.3، rotation، process death، offline/slow network و accessibility اجرا شود.
- [ ] signed release/R8/secret scan/lint/tests و benchmark startup/jank/memory/network ثبت شود.
- [ ] برای هر feature Flutter/Native pair + overlay + metric + package/activity/version/timestamp تولید شود.

**گیت نهایی:** 100% matrix، صفر dead route و P0/P1 باز، و Native در benchmark توافق‌شده سریع‌تر یا حداقل هم‌سطح Flutter.

## چک‌لیست ممیزی این نوبت

### Phase 0 — provenance
- [x] dirty-worktree پیش از هر mutation ثبت شد.
- [x] emulator/API/viewport/proxy/package/activity شناسایی شد.
- [x] build Native تلاش و blocker تکرارپذیر ثبت شد.

### Phase 1 — inventory
- [x] feature inventory Flutter/Native و تعداد فایل‌ها ثبت شد.
- [x] routeهای Deferred غیرفعال کشف شدند.
- [x] evidence تاریخی از evidence فعلی تفکیک شد.

### Phase 2 — runtime torture test
- [x] cold launch Flutter و Native با Emulator مشاهده شد.
- [x] Native startup hang و Flutter feed network failure ثبت شد.
- [ ] feature-level screenshot pair/diff — **Blocked by startup + build lock + fixture**.

### Phase 3 — roadmap
- [x] اولویت، گیت و ترتیب وابستگی آماده شد.
- [ ] بعد از رفع Phase 0/1، این گزارش با metrics واقعی به‌روزرسانی شود.

## نتیجه

Native production نصب‌شده package صحیح و version بالاتر از Flutter دارد، اما در startup متوقف می‌شود؛ بنابراین از دید کاربر هم‌ارز Flutter نیست و تست «هیچ تفاوتی احساس نشود» هنوز شروع معتبر نشده است. شروع درست: **رفع startup hang و file lock، build/install تازه، سپس مقایسهٔ جفتی با fixture واقعی**؛ نه اضافه کردن featureهای بیشتر.

## افزونهٔ اعتبارسنجی زنده — 2026-08-24 11:42 تا 11:49 (جایگزین بخشی از نتیجهٔ اولیه)

اتصال Emulator بعداً توسط کاربر اصلاح شد. در این بازه هر دو APK نصب‌شده با حساب `ahmad` و دادهٔ واقعی بازآزمایی شدند. این شواهد فقط به APKهای نصب‌شده مربوط‌اند، نه build تازهٔ checkout dirty.

| مسیر | Flutter | Native | نتیجه |
|---|---|---|---|
| startup + feed | موفق؛ feed واقعی لود شد | موفق؛ feed واقعی لود شد | هر دو قابل استفاده؛ تفاوت typography/icon/spacing هنوز نیازمند diff کمّی است |
| own profile | نام، آواتار، شمارنده‌ها، badge، edit/share و postها حاضر | همان داده و hierarchy اصلی حاضر | close but not pixel parity؛ Native در iconography و فاصله‌ها متفاوت است |
| chat list | گفتگوها و previewها حاضر | همان گفتگوها و previewها حاضر | قابلیت پایه برابر؛ هر دو برای بعضی گفتگوها preview رمز‌شده/Base64 نشان می‌دهند؛ این defect محصول است، نه تفاوت اختصاصی Native |
| chat detail (`raha.81`) | bubbleها، wallpaper، composer و messageهای واقعی حاضر | همان گفتگو و messageها حاضر | functionality نزدیک؛ Native app bar/جهت back، date divider، composer icons و geometry تفاوت دارند |
| search initial | input، QR/scan affordance، trends حاضر | input و trends حاضر | **Native فاقد affordance اسکن/QR سمت چپ input است** |
| services hub | 4 quick action و کارت‌های «فروشگاه»/«مسابقات» حاضر | quick actionها حاضر، ولی در «سرویس‌های بیشتر» دو کارت «فروشگاه» تکراری دیده شد | **P1 parity defect: محتوای/route کارت‌ها نادرست یا ناقص است** |
| other profile (`raha.81`) | در این افزونه هنوز جفت Flutter ثبت نشده | Native به‌صورت live باز شد و feed/profile data واقعی دارد | Pending side-by-side |

### یافته‌های زندهٔ جدید

1. مشکل شبکه را نمی‌توان صرفاً ping دانست: قبل از اصلاح کاربر، DNS/ICMP فعال بود اما `SSLHandshakeException` برای Native دیده شد. پس تمام نتایج قبل از 11:42 که startup را blocked اعلام کرده بودند برای APK نصب‌شده **باطل/منسوخ** هستند.
2. برای routeهای اصلی مشاهده‌شده، Native session و API واقعی کار می‌کنند؛ با این حال این امر build فعلی Native یا parity کامل را ثابت نمی‌کند.
3. نمایش preview رمز‌شده در هر دو برنامه، نشت UI/UX بالقوه‌ای است. قبل از هر تغییر باید بررسی شود که پیام‌های legacy، E2EE key/session یا mapper مسئول‌اند؛ نباید با fallback plaintext اصلاح شود.
4. مقایسهٔ کامل هنوز باز است: post detail/comments، stories/create post، search query/keyboard/results، profile subpages، settings، contacts/game/nearby، group chat و notification/deep-link باید با همان fixture تکمیل شوند.
