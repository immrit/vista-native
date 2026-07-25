# معماری حداقلی برش Startup / Onboarding / Auth

## اصل طراحی

برای توسعه‌پذیری مرز تعریف می‌شود، اما فقط requirement اثبات‌شدهٔ مسیر فعلی implementation دارد. ساختار یک Android app module باقی می‌ماند.

## لایه‌ها و مالکیت

```text
Compose screens
  -> immutable UiState + UiAction
  -> StartupViewModel / AuthViewModel
  -> StartupRepository / AuthRepository contracts
  -> Default repositories
  -> AuthRemoteDataSource + OnboardingStore + SecureSessionStore
```

- UI فقط action ارسال و state مشاهده می‌کند؛ به OkHttp، SharedPreferences یا encrypted storage دسترسی مستقیم ندارد.
- `StartupViewModel` تنها مالک تصمیم مقصد startup است.
- `AuthViewModel` state machine شناسه، رمز، OTP و 2FA را نگه می‌دارد.
- `AuthRepository` قرارداد lookup/login/OTP/refresh است.
- `SessionStore` ذخیره و پاک‌سازی امن tokenها را مالک است.
- `OnboardingStore` تکمیل نسخه `1.0.0` را مالک است.
- `AppContainer` فقط همین وابستگی‌های concrete را می‌سازد؛ DI framework اضافه نمی‌شود.

## مدل startup

```text
Loading
  -> Maintenance
  -> Onboarding
  -> Authentication
  -> Authenticated(profileCompleted, passwordRequired, offline)
  -> RecoverableError(retry)
```

قواعد:

- status endpoint best-effort است؛ عدم دسترسی آن startup را متوقف نمی‌کند.
- access token با `exp` و حاشیه ۳۰ ثانیه سنجیده می‌شود.
- refresh موفق، نشست را جایگزین می‌کند.
- refresh 401/403، storage را پاک می‌کند.
- refresh timeout/network/5xx storage را حفظ می‌کند. اگر user id محلی موجود باشد، authenticated offline انتخاب می‌شود؛ وگرنه خطای قابل retry نمایش داده می‌شود.

## navigation

graph فقط routeهای زیر را دارد:

- `startup`
- `onboarding`
- `auth`
- `authenticated-boundary`
- `maintenance`

تعویض مقصد startup با پاک‌کردن route قبلی انجام می‌شود تا Back به splash یا onboarding کامل‌شده برنگردد. Back در onboarding ابتدا به اسلاید قبل می‌رود؛ در auth ابتدا step قبلی را برمی‌گرداند.

## تغییرات shared و علت

| فایل/ناحیه | علت مجاز |
|---|---|
| version catalog و Gradle app | نسخه اولیه resolve نمی‌شد؛ Compose و dependencyهای لازم همین برش باید فعال شوند |
| settings repositories | Google Maven در محیط اجرا برای artifact موجود 404 برمی‌گرداند؛ mirror محدود Android/Google بعد از منبع رسمی fallback می‌شود |
| AndroidManifest و Application | entry point، network permission، backup exclusion و dependency container |
| theme/resources | edge-to-edge، RTL و نام Vista برای صفحه‌های همین برش |
| `core/*` | result/error، HTTP، persistence امن و token expiry مورد استفاده مستقیم startup/auth |
| `navigation/*` | graph محدود همین برش |
| `ui/theme/*` و `ui/components/*` | token/componentهایی که splash/onboarding/auth واقعاً مصرف می‌کنند |

هیچ package خالی یا abstraction بدون مصرف ایجاد نمی‌شود.

## امنیت

- فقط `https://api.coffevista.ir` به‌عنوان base URL release استفاده می‌شود.
- cleartext traffic غیرفعال است.
- access/refresh/user/expiry/displayName در یک payload اتمیک AES/GCM با کلید non-exportable در Android Keystore ذخیره می‌شوند.
- backup برای داده‌های نشست غیرفعال می‌شود.
- request logging حاوی payload احراز هویت اضافه نمی‌شود.
- خطای parse یا 5xx refresh به‌عنوان revocation تفسیر نمی‌شود.
- نام خوش‌آمدگویی از `full_name`، سپس `username` و در نهایت شماره موبایل پاسخ backend انتخاب می‌شود و در refresh نیز به‌روز می‌ماند.

## آزمون‌پذیری

- repositoryها و storeها interface هستند و ViewModel با fake قابل تست است.
- تصمیم startup به‌صورت unit test برای access معتبر، refresh موفق، terminal refresh، offline fallback، first run و onboarding-complete پوشش داده می‌شود.
- Auth state machine برای lookup، validation، password success/error، OTP و 2FA پوشش داده می‌شود.
