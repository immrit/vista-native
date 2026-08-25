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
- Flutter در فهرست گفتگو برای بعضی پیام‌ها preview رمز‌شده/Base64 نشان داد. Native نیز ابتدا همان
  نقص را نشان داد، اما APK beta تازه روی همان cache حساب، previewهای `mahshid` و `ahmadesmaili` را
  به `پیام جدید` تبدیل کرد و متن عادی `raha.81` را حفظ کرد. این یک hardening Native تأییدشده است؛
  اصلاح متناظر Flutter/backend هنوز باز است و نباید با ذخیره یا نمایش متن ساده پنهان شود.
- در اجرای زندهٔ 2026-08-24 با emulator-5554، Flutter در Search آیکن QR را در سمت چپ فیلد
  دارد؛ Native production نصب‌شده همان صفحه را بدون آن نشان داد. wiring آن اکنون در APK beta ساخته‌شده
  از checkout حاضر است، اما launch/scan با build تازه هنوز evidenceِ runtime نیست. در Services کارت
  تکراری «Store» Native با normalizer همسان Flutter رفع شد؛ APK تازه اکنون جفت «Store» و «Competition» را نشان می‌دهد.
- APK beta `2.6.3-beta (4050)` از checkout فعلی با output ایزوله ساخته، نصب و اجرا شد؛
  `:app:verifyNativeApplicationIds` نیز در build عبور کرد. این فقط baseline اجرایی است و به معنی
  release-ready یا جایگزینی Flutter نیست.

## نقشهٔ قابلیت‌ها و شکاف‌ها

| حوزه | مرجع Flutter | معادل Native فعلی | وضعیت سخت‌گیرانه | کار باقیمانده / اولویت |
|---|---|---|---|---|
| راه‌اندازی، session، deep link | `lib/app`, `android/app/.../MainActivity.kt` | `MainActivity.kt`, `navigation/DeepLinkCoordinator.kt`, `feature/shell/VistaShell.kt` | موجود، اجرا نشده | P0: build از checkout، cold/warm link برای post/profile/group/chat و back stack. Deep-link اکنون به route واقعی shell می‌رود؛ `DeferredDestinationScreen` مسیر فعال نیست. |
| ورود، OTP، password setup | `lib/features/auth` | `feature/auth`, `core/security` | موجود، اجرا نشده | P0: identifier/password/OTP/خطا/انقضا و logout با سرور واقعی. |
| biometric lock | `biometric_login_screen.dart` | `MainActivity.kt` + `BiometricAuthenticator` | موجود، اجرا نشده | P1: enable/disable، cancel/fallback، lock پس از background. این مورد «غایب» نیست. |
| بازیابی رمز | `password_recovery_confirm_screen.dart`, `password_reset_*`, `password_set_screen.dart` | در جست‌وجوی Native معادل recovery یافت نشد | شکاف قطعی | P0: API recovery options/send/verify/complete، مسیر RTL و آزمون خطا؛ بدون token/plaintext persistence. |
| پوسته و ناوبری | bottom tabs Flutter | `feature/shell` | تأییدشدهٔ سطح اولیه | P0: benchmark startup/back state بعد از build همان checkout؛ P1: pixel parity آیکن/spacing. |
| فید، واکنش، ذخیره، detail | `lib/features/posts` | `feature/feed` (feed/detail/repository) | partial، اجراشده | Cold-start و Post detail هر دو روی دادهٔ واقعی دیده شدند. تاریخ Feed Native اکنون با Flutter شمسی/فارسی است. اما screenshot detail هنوز شکاف بصری P0 دارد: Flutter کارت post قاب‌دار با spacing/action layout متفاوت دارد و Native آن را تخت رندر می‌کند. سپس create/edit/delete، like/save/share/report، pagination و offline/retry باید با داده واقعی آزمون شوند. |
| نظرات و پاسخ تو در تو | `lib/features/posts` | `CommentsBottomSheet.kt` و ViewModel/repository | partial، اجراشده | P0: روی پست مشترک `mahshid`، Flutter و APK تازهٔ Native هر دو thread یک کامنت و یک پاسخ را رندر کردند و با Computer hide→show دوباره بررسی شد. rail نیتیو اکنون دقیقاً هندسهٔ ثابت Flutter را دارد: محور 35dp از لبهٔ راست RTL و شروع خط پایین 52dp برای ریشه و پاسخ؛ پیش از این پاسخ کوچک‌تر محور/start را به 36/44dp جابه‌جا می‌کرد و در thread چندپاسخی می‌شکست. Native اکنون مانند Flutter هنگام نمایش دوبارهٔ پاسخ‌ها، up to 1000 comment را reload و فقط subtree هدف را merge می‌کند؛ loading state و پیام فارسی fail-safe هم دارد. ویرایش inline هم در Bottom Sheet و Post Detail مستقیماً `updateComment(id, content)` است و parent/post رابطه را حتی با پاسخ PATCH ناقص حفظ می‌کند. اجرای اجباری regression را پیدا کرد و source اصلاح شد؛ rerun بدون cache در ساعت 20:00 هر 12 تست `CommentsViewModelTest` را سبز کرد، شامل subtree/error/inline. compile و APK build/install تازهٔ 19:57 هم سبز است. هنوز fixture زندهٔ سه‌پاسخ، composer/IME، optimistic rollback، dark mode و paging باید تأیید شوند. |
| شواهد تکمیلی کامنت، 20:52 | fixture دیباگ ایزوله | `DebugCommentApiFixture.kt` | اجراشده، غیر-production | `CommentsViewModelTest` بدون cache سبز شد، APK 20:49 ساخته/نصب شد و Computer یک thread پنج‌گرهی (ریشه، سه پاسخ مستقیم، یک پاسخ تو در تو) را تا انتها باز کرد؛ ریل برای همهٔ ردیف‌ها روی محور ثابت Flutter ماند. ورود 0.8-scale/fade هر CommentItem، pull-to-refresh و safe inset composer نیز به قرارداد Flutter افزوده شد. fixture فقط GET پست مصنوعی را پاسخ می‌دهد و دادهٔ کاربر را تغییر نمی‌دهد. این مدرک ساختاری است؛ گیت دادهٔ زندهٔ سه‌پاسخ همچنان باز می‌ماند. |
| شواهد زندهٔ empty reply، 21:01 | post واقعی `raha.81` با ۵ کامنت | Flutter و `PostDetailScreen.kt` Native | اجراشده | Computer همان پست را در هر دو برنامه باز کرد. Flutter برای هر root بدون پاسخ، متن ایتالیک `هنوز پاسخی وجود ندارد` نشان می‌داد؛ APK تازهٔ Native اکنون همان state و جایگاه فهرست را دارد. هیچ داده‌ای نوشته/تغییر نشد. |
| اصلاح نهایی محور rail، 21:10 | fixture پنج‌گرهی | `CommentsBottomSheet.kt` | اجراشده | علت فاصله‌داشتن rail از مرکز آواتار، اجرای `drawBehind` بعد از padding شانزده‌dp Row بود. draw اکنون قبل از padding است و Computer روی APK 21:08 همهٔ ردیف‌ها را بازبینی کرد: خط از مرکز آواتار ریشه/پاسخ می‌گذرد و بین ردیف‌ها قطع نمی‌شود. |
| ناوبری نویسندهٔ کامنت، 21:19 | post واقعی `raha.81` | `PostDetailScreen.kt` Native | اجراشده | APK تازه پس از `BUILD SUCCESSFUL in 53s` نصب شد. Computer پست واقعی را باز کرد و لمس نام `re.mhd` به پروفایل همان حساب (`reza`) رفت. TODO پیشینِ callback نویسندهٔ کامنت حذف شده است؛ گیت‌های دادهٔ زندهٔ چندپاسخ، dark، IME و rollback هنوز بازند. |
| ویدیو و trim | post/video Flutter | `AddPostScreen.kt`, `VideoTrimmerScreen.kt`, `VistaFeedVideoPlayer.kt` | موجود، اجرا نشده | P1: permission، pick/upload/cancel، autoplay/lifecycle و memory/perf. |
| stories | `lib/features/stories` | story tray/player/editor/repository زیر `feature/feed` | موجود، اجرا نشده | P1: create/view/reply/viewers/archive، media permission و expiration. |
| اعلان‌ها و FCM | Flutter notification services | `notifications/*`, `VistaFirebaseMessagingService.kt` | نیازمند اثبات | P0: token registration، foreground/background/tap navigation، کانال‌ها و عدم نشت متن محرمانه. |
| چت و realtime/E2EE | `lib/features/chat`, emoji | `feature/chat` + Room/cipher/websocket | تأییدشدهٔ سطح اولیه | P0: conversation/new/group/message/deep-link، reconnect، attachment/voice/GIF/emoji، pagination و fail-closed E2EE. APK beta تازه، Base64 cache inbox را fail-closed به `پیام جدید` تبدیل کرد و متن عادی را حفظ کرد؛ Flutter/backend counterpart، و همهٔ جریان‌های P0، هنوز بازند. |
| پروفایل، follow، QR | `lib/features/profile` | `feature/profile` (own/other/followers/QR) | موجود، اجرا نشده | P0: edit avatar/profile، follow/unfollow/block/report و QR scanner روی دستگاه. |
| جستجو | `lib/features/search`, `VistaQRScanner.dart` | `feature/search/SearchLauncherScreen.kt`, workspace/history | launch-verified، decode اثبات‌نشده | P1: 2026-08-24 دکمهٔ QR و route آن از launcher به scanner موجود Native وصل شد، در APK beta build/install شد و scanner RTL launch گردید؛ camera/gallery permission و decode واقعی همچنان لازم است. |
| Services و مخاطب/نزدیکان | `lib/features/services`, `nearby` | `feature/services` | partial، fail-closed | کارت managed تکراری با dedup route/title مطابق Flutter رفع و روی APK تازه تأیید شد. Game/Competition SSO به `ERR_SSL_UNRECOGNIZED_NAME_ALERT` زیرساختی می‌رسد؛ Native خطا را cancel می‌کند و URL/ticket را نمایش نمی‌دهد. P0: repair server certificate/SNI سپس action هر کارت؛ P1: Contacts/permission/nearby/location و WebView policy. |
| تنظیمات، privacy و verification | `lib/features/settings` | profile settings + verification/about | موجود، اجرا نشده | P1: تک‌تک toggleها، persistence پس از restart، لینک‌ها و account deletion/logout. |
| موسیقیِ پست و صوت پس‌زمینه | `MusicDownloadManager.dart`, `MusicService.dart`, `just_audio_background` | فقط metadata موسیقی روی post و playerهای media/chat یافت شد | شکاف قطعی | P1: shared audio session، دانلود/حذف، notification/media controls، lifecycle و جلوگیری از تداخل با voice chat. |
| دریافت Share از Android | Flutter manifest: `SEND`/`SEND_MULTIPLE` برای text/image/video/file + `MainActivity` + share sheet | Native manifest فقط MAIN/VIEW و FCM دارد؛ فقط share خروجی `ACTION_SEND` دیده شد | شکاف قطعی | P0: intent filters، parse امن URI/ClipData، permission grant lifecycle، sheet انتخاب message/post/story و tests. |
| مجوزها/دوربین/فایل | Flutter Android manifest + picker/scanner | Native scanner/media flows | نیازمند اثبات | P0: manifest/runtime permission matrix Android 13+؛ failure/cancel/denial. |
| دادهٔ محلی، migration، امنیت | Flutter secure storage/E2EE | Native encrypted/session/db/chat cipher | موجود، اجرا نشده | P0: upgrade از Flutter package به Native package، session/identity policy، database migration، no plaintext/token logs. |
| انتشار و جایگزینی | Flutter `ir.coffevista.vista` | Native اکنون package متفاوت `ir.coffevista.vista_native.production` | شکاف انتشار | P0: package/signing/update path تصمیم‌گیری‌شده و آزمایش‌شده؛ تا پیش از آن Native جایگزین مستقیم Play/update Flutter نیست. |

## ترتیب اجرای الزام‌آور

1. **P0-A — قابل‌ساخت و قابل‌نصب‌شدن از checkout فعلی:** انجام شد برای beta ایزوله: APK با package/activity Native ثبت‌شده نصب و baseline زنده گرفته شد. این گیت باید پیش از هر release candidate دوباره تکرار شود.
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
