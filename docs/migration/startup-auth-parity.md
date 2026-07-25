# ماتریس برابری مسیر Startup / Onboarding / Auth

تاریخ کشف: ۲۰۲۶-۰۷-۲۴

## Baseline

- Native: `E:\vista_native` فاقد `.git` و فاقد سورس production بود.
- Flutter: `E:\vista` روی شاخه `main` بود و پیش از این کار تغییرات حل‌نشده داشت؛ همه بررسی‌ها read-only انجام شد.
- Backend: `E:\vista-backend` فقط برای تأیید قرارداد HTTP خوانده شد.

## رفتار مرجع تأییدشده

| مسیر | مرجع Flutter / Backend | رفتار لازم در Native | وضعیت |
|---|---|---|---|
| Splash | `session_auth_wrapper.dart:203-224` | نمایش لوگوی اصلی Flutter و loading تا پایان تصمیم startup | پیاده‌سازی و build |
| Maintenance | `session_auth_wrapper.dart:42-64` و `GET /api/v1/system/status` | maintenance=true مقصد اختصاصی؛ شکست status غیرمسدودکننده | کشف‌شده |
| access معتبر | `session_auth_wrapper.dart:66-70` | ادامه به authenticated boundary بدون refresh | پیاده‌سازی و تست |
| access منقضی + refresh | `session_auth_wrapper.dart:71-89` | refresh و ذخیره tokenهای چرخیده و نام کاربر | پیاده‌سازی و تست |
| refresh با 401/403 | `auth_repository.dart:486-510` | پاک‌سازی نشست و رفتن به onboarding/auth | پیاده‌سازی و تست |
| refresh با network/5xx | `session_auth_wrapper.dart:84` | حفظ نشست و fallback آفلاین؛ logout ممنوع | پیاده‌سازی و تست |
| بدون نشست | `session_auth_wrapper.dart:190-200` | onboarding نسخه‌دار، سپس auth | پیاده‌سازی و تست |
| onboarding | `onboarding_screen.dart:18-52,127-153` | سه تصویر اصلی Flutter، فارسی RTL، skip/next/back و تکمیل پایدار | پیاده‌سازی و build |
| نسخه onboarding | `onboarding_service.dart` | کلید تکمیل + نسخه `1.0.0`؛ mismatch یعنی نمایش دوباره | کشف‌شده |
| identifier | `auth_wizard_screen.dart:92-149` | شماره/ایمیل/نام کاربری، lookup و انتخاب password/OTP | پیاده‌سازی و تست |
| phone normalization | backend `internal/auth/normalize.go` | ارقام فارسی/عربی و قالب ایران به `09xxxxxxxxx` | کشف‌شده |
| password login | `POST /v1/auth/login` | identifier + password، validation و خطای فارسی | پیاده‌سازی و تست |
| OTP | `send-otp`, `verify-otp` | کد ۵ رقمی، شمارش معکوس ۶۰ ثانیه و resend | پیاده‌سازی و تست |
| OTP + password account | backend `VerifyOTP` | دریافت `is_2fa_required/two_factor_token` و درخواست رمز | پیاده‌سازی و تست |
| login success | `AuthResponse` | ذخیره رمزگذاری‌شده access/refresh/user/expiry/displayName و انتقال یک‌باره به صفحه خوش‌آمد | پیاده‌سازی و تست |
| profile/password gates | `session_auth_wrapper.dart:231-238` | فقط اعلام نیاز در authenticated boundary؛ پیاده‌سازی feature خارج scope | مرز آگاهانه |
| Home | prompt strict boundary | هیچ Home feature یا data layer ساخته نشود | خارج scope |

## قرارداد خطا

Native باید هر سه envelope موجود را بفهمد:

1. سرویس auth: `{"code":"...", "message":"پیام فارسی"}`
2. handler: `{"error":{"code":"error","message":"..."}}`
3. feature gate: `{"error":"feature_disabled", ...}`

اولویت نمایش با پیام فارسی معتبر backend است. fallbackهای محلی برای validation، 401، 403، 409، 429، 5xx، timeout و offline تعریف می‌شوند. هیچ token، password یا OTP در log نوشته نمی‌شود.

## موارد عمداً منتقل‌نشده

- Home، Profile setup، Mandatory-password implementation، password recovery، chat، push، session registration و device management.
- معماری چندماژوله، Hilt، Room، framework عمومی use-case و packageهای placeholder.
- TLS pinning پویا؛ HTTPS اجباری است و pinning باید در یک برش امنیتی مستقل با rollout قابل بازیابی انجام شود.

## نتیجهٔ اعتبارسنجی

- ۲۰ unit test با ۰ failure.
- Android lint: ۰ error؛ ۲۳ warning مربوط به نسخه dependency، resource اولیهٔ unused و پیشنهاد KTX.
- `assembleDebug`: موفق؛ APK روی دستگاه `SM_N975F` نصب و صفحه ورود از طریق UI hierarchy تأیید شد.
- ورود زنده نیازمند حساب/OTP واقعی است و بدون credential آزمایشی اجرا نشد؛ هیچ دادهٔ ورود در log یا artifact ذخیره نشده است.
