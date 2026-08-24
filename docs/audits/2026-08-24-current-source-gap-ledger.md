# دفتر شکاف فعلی Flutter → Vista Native

**مبنای حقیقت:** `E:\vista` (Flutter، فقط‌خواندنی)  
**هدف:** `E:\vista_native` (Kotlin/Compose)  
**وضعیت:** در حال اجرا  
**به‌روزرسانی:** 2026-08-24  
**پیشرفتِ قابل‌اثبات برای جایگزینی انتشار:** `[██░░░░░░░░] 20%`

این درصد «کد موجود» نیست؛ درصدی از گیت‌های لازم برای جایگزینی امن Flutter است. وجود یک
کلاس Native بدون build از checkout، نصب، آزمون با دادهٔ واقعی، و مقایسهٔ تصویری هم‌fixture،
امتیاز تکمیل نمی‌گیرد.

## روش و درجه‌بندی

| درجه | تعریف |
|---|---|
| `تأییدشده` | Flutter و Native در کد و اجرای زنده دیده شدند؛ هنوز برای release نیازمند regression است. |
| `موجود، اجرا نشده` | معادل Native در checkout فعلی هست، اما با APK ساخته‌شده از همین checkout آزمون نشده است. |
| `شکاف قطعی` | قرارداد Flutter حاضر است و معادل Native/manifest/routing در checkout وجود ندارد. |
| `نیازمند اثبات` | کد یا UI اولیه هست، اما موفقیت عملیات، خطاها، مجوزها یا parity تصویری آزمون نشده‌اند. |

## شواهد زندهٔ مشترک

- هر دو برنامهٔ نصب‌شده با حساب واقعی یکسان، فید، پروفایل، فهرست گفتگو، گفتگوی `raha.81`،
  جستجو و Services را نمایش دادند.
- فهرست گفتگو در هر دو برنامه preview رمز‌شده/Base64 برای بعضی پیام‌ها نشان داد. این نقص
  محصول مشترک است؛ نباید با ذخیره یا نمایش متن ساده در Native پنهان شود.
- Native در Search دکمهٔ scan/QR مرجع Flutter را نداشت؛ در Services هم کارت «Store» دو بار
  دیده شد، جایی که Flutter «Store» و «Competition» داشت.
- APK Native نصب‌شده production `2.6.3 (4050)` است، ولی آخرین build از checkout فعلی هنوز
  به علت قفل‌بودن خروجی Gradle توسط فرایند باز، ساخته نشده؛ بنابراین هیچ ادعای release-ready
  برای کد WIP پذیرفته نیست.

## نقشهٔ قابلیت‌ها و شکاف‌ها

| حوزه | مرجع Flutter | معادل Native فعلی | وضعیت سخت‌گیرانه | کار باقیمانده / اولویت |
|---|---|---|---|---|
| راه‌اندازی، session، deep link | `lib/app`, `android/app/.../MainActivity.kt` | `MainActivity.kt`, `navigation/DeepLinkCoordinator.kt`, `feature/shell/VistaShell.kt` | موجود، اجرا نشده | P0: build از checkout، cold/warm link برای post/profile/group/chat و back stack. Deep-link اکنون به route واقعی shell می‌رود؛ `DeferredDestinationScreen` مسیر فعال نیست. |
| ورود، OTP، password setup | `lib/features/auth` | `feature/auth`, `core/security` | موجود، اجرا نشده | P0: identifier/password/OTP/خطا/انقضا و logout با سرور واقعی. |
| biometric lock | `biometric_login_screen.dart` | `MainActivity.kt` + `BiometricAuthenticator` | موجود، اجرا نشده | P1: enable/disable، cancel/fallback، lock پس از background. این مورد «غایب» نیست. |
| بازیابی رمز | `password_recovery_confirm_screen.dart`, `password_reset_*`, `password_set_screen.dart` | در جست‌وجوی Native معادل recovery یافت نشد | شکاف قطعی | P0: API recovery options/send/verify/complete، مسیر RTL و آزمون خطا؛ بدون token/plaintext persistence. |
| پوسته و ناوبری | bottom tabs Flutter | `feature/shell` | تأییدشدهٔ سطح اولیه | P0: benchmark startup/back state بعد از build همان checkout؛ P1: pixel parity آیکن/spacing. |
| فید، واکنش، ذخیره، detail | `lib/features/posts` | `feature/feed` (feed/detail/repository) | موجود، اجرا نشده | P0: create/edit/delete، like/save/share/report، pagination، offline/retry با داده واقعی. |
| نظرات و پاسخ تو در تو | `lib/features/posts` | `CommentsBottomSheet.kt` و ViewModel/repository | موجود، اجرا نشده | P0: composer/IME، reply nesting، optimistic/rollback و RTL. |
| ویدیو و trim | post/video Flutter | `AddPostScreen.kt`, `VideoTrimmerScreen.kt`, `VistaFeedVideoPlayer.kt` | موجود، اجرا نشده | P1: permission، pick/upload/cancel، autoplay/lifecycle و memory/perf. |
| stories | `lib/features/stories` | story tray/player/editor/repository زیر `feature/feed` | موجود، اجرا نشده | P1: create/view/reply/viewers/archive، media permission و expiration. |
| اعلان‌ها و FCM | Flutter notification services | `notifications/*`, `VistaFirebaseMessagingService.kt` | نیازمند اثبات | P0: token registration، foreground/background/tap navigation، کانال‌ها و عدم نشت متن محرمانه. |
| چت و realtime/E2EE | `lib/features/chat`, emoji | `feature/chat` + Room/cipher/websocket | تأییدشدهٔ سطح اولیه | P0: conversation/new/group/message/deep-link، reconnect، attachment/voice/GIF/emoji، pagination و fail-closed E2EE. preview رمز‌شده یک نقص مشترک برای triage backend/client است. |
| پروفایل، follow، QR | `lib/features/profile` | `feature/profile` (own/other/followers/QR) | موجود، اجرا نشده | P0: edit avatar/profile، follow/unfollow/block/report و QR scanner روی دستگاه. |
| جستجو | `lib/features/search`, `VistaQRScanner.dart` | `feature/search/SearchLauncherScreen.kt`, workspace/history | پیاده‌سازی‌شده، اثبات‌نشده | P1: 2026-08-24 دکمهٔ QR و route آن از launcher به scanner موجود Native وصل شد؛ هنوز build/install و live scan لازم است. |
| Services و مخاطب/نزدیکان | `lib/features/services`, `nearby` | `feature/services` | تأییدشدهٔ سطح اولیه با نقص | P0: رفع mapping کارت تکراری Store→Competition، سپس action هر کارت؛ P1: Contacts/permission/nearby/location و WebView policy. |
| تنظیمات، privacy و verification | `lib/features/settings` | profile settings + verification/about | موجود، اجرا نشده | P1: تک‌تک toggleها، persistence پس از restart، لینک‌ها و account deletion/logout. |
| موسیقیِ پست و صوت پس‌زمینه | `MusicDownloadManager.dart`, `MusicService.dart`, `just_audio_background` | فقط metadata موسیقی روی post و playerهای media/chat یافت شد | شکاف قطعی | P1: shared audio session، دانلود/حذف، notification/media controls، lifecycle و جلوگیری از تداخل با voice chat. |
| دریافت Share از Android | Flutter manifest: `SEND`/`SEND_MULTIPLE` برای text/image/video/file + `MainActivity` + share sheet | Native manifest فقط MAIN/VIEW و FCM دارد؛ فقط share خروجی `ACTION_SEND` دیده شد | شکاف قطعی | P0: intent filters، parse امن URI/ClipData، permission grant lifecycle، sheet انتخاب message/post/story و tests. |
| مجوزها/دوربین/فایل | Flutter Android manifest + picker/scanner | Native scanner/media flows | نیازمند اثبات | P0: manifest/runtime permission matrix Android 13+؛ failure/cancel/denial. |
| دادهٔ محلی، migration، امنیت | Flutter secure storage/E2EE | Native encrypted/session/db/chat cipher | موجود، اجرا نشده | P0: upgrade از Flutter package به Native package، session/identity policy، database migration، no plaintext/token logs. |
| انتشار و جایگزینی | Flutter `ir.coffevista.vista` | Native اکنون package متفاوت `ir.coffevista.vista_native.production` | شکاف انتشار | P0: package/signing/update path تصمیم‌گیری‌شده و آزمایش‌شده؛ تا پیش از آن Native جایگزین مستقیم Play/update Flutter نیست. |

## ترتیب اجرای الزام‌آور

1. **P0-A — قابل‌ساخت و قابل‌نصب‌شدن از checkout فعلی:** قفل Gradle را بدون نابودکردن WIP رفع کنید، APK را با package/activity ثبت‌شده نصب کنید و baseline زنده را دوباره بگیرید.
2. **P0-B — قابلیت‌های واقعاً غایب و مانع جایگزینی:** password recovery، inbound Android share، Services mapping، notification/deep-link/permission lifecycle، و مسیر package/signing/update.
3. **P0-C — جریان‌های اصلی موجود اما بدون اثبات:** auth، feed/comments, profile, chat/E2EE، settings؛ هرکدام با happy/error/offline/restart.
4. **P1 — parity محصول و polish:** QR Search، music/background audio، stories/video، pixel/RTL/performance.
5. **Release gate:** فقط پس از یک APK Native ساخته‌شده از همین commit، screenshot هم‌fixture و آزمون upgrade/rollback، گزارش می‌تواند «جایگزین Flutter» بدهد.

## معیار پایان هر مورد

- تغییر فقط در workspace Native و بدون overwrite تغییرات موجود دیگران.
- build و test مرتبط سبز؛ سپس نصب APK دقیقاً همان build روی emulator رزروشده.
- screenshot Flutter و Native از fixture/حساب یکسان، با نام APK/package/activity و زمان ثبت‌شده.
- برای auth/chat/share/media: نبود token، credential یا plaintext در log/cache اثبات شود و failure امن بماند.
- هر مورد در `docs/plans/2026-08-24-native-replacement-execution.md` با evidence path و نتیجهٔ runtime تیک می‌خورد؛ «کد نوشته شد» تیک پایان نیست.
