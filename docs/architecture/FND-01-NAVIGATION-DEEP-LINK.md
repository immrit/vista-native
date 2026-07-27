# FND-01 Navigation and Deep-link Contract

## Scope

این قرارداد فقط مسیرهای موجود Foundation را پوشش می‌دهد. مقصدهای Post، Profile، Group و Chat
در این فاز Feature واقعی ندارند و فقط به یک مقصد کنترل‌شده‌ی deferred می‌روند.

## Native route inventory

| Typed route | Owner | Back-stack rule |
|---|---|---|
| `AppRoute.Startup` | `feature:auth` UI در composition root | start destination؛ پس از resolve به‌صورت inclusive حذف می‌شود |
| `AppRoute.Maintenance` | `feature:auth` | Retry آن را inclusive با Startup جایگزین می‌کند |
| `AppRoute.Onboarding` | `feature:auth` | completion آن را inclusive با Authentication جایگزین می‌کند |
| `AppRoute.Authentication` | `feature:auth` | login موفق آن را inclusive با AuthenticatedBoundary جایگزین می‌کند |
| `AppRoute.AuthenticatedBoundary` | `:app` | بدون `SignedIn` به Startup برمی‌گردد |
| `AppRoute.DeferredFeature(kind, reference)` | `:app` | فقط failure-safe handoff برای Feature آینده |
| `AppRoute.DeepLinkFailure(reason)` | `:app` | مقصد کنترل‌شده برای لینک نامعتبر/پشتیبانی‌نشده |

تمام routeها `@Serializable` هستند و `composable<T>`، `navigate(T)` و `toRoute<T>()` مصرف‌کننده‌ی
واقعی آن‌ها هستند. هیچ string route در graph باقی نمانده است.

## Deep-link registry

Manifest و parser فقط contractهای زیر را می‌پذیرند:

- `vista://post/{reference}`
- `vista://profile/{reference}`
- `vista://group/{reference}`
- `vista://chat/{reference}`

`reference` دقیقاً یک segment با الگوی `[A-Za-z0-9_-]{1,128}` است. query، fragment، user-info،
port، scheme دیگر و segment اضافی رد می‌شوند. قرارداد قدیمی `/chat-detail` عمداً
`UNSUPPORTED` است؛ canonical contract برابر `vista://chat/{reference}` است.

## Delivery state machine

1. intent پیش از resolve شدن session به `PendingSession` یا `PendingFailure` می‌رود.
2. Startup ابتدا route پایه را تثبیت می‌کند و سپس `onSessionResolved` را اعلام می‌کند.
3. مقصد معتبر logged-out در `PendingAuthentication` باقی می‌ماند.
4. login موفق آن را با همان delivery id به `Ready` تبدیل می‌کند.
5. navigation effect پس از `consume(id)` به `Idle` می‌رود.
6. delivery فعال تکراری و delivery مصرف‌شده در پنجره‌ی دوثانیه‌ای deduplicate می‌شوند.

Startup فقط وقتی transition خود را اعمال می‌کند که همان Startup back-stack entry هنوز current
باشد؛ بنابراین نتیجه‌ی دیررس Startup نمی‌تواند مقصد deep link را overwrite کند.

## Failure and security rules

- Feature پیاده‌نشده هرگز fake UI یا no-op ندارد و به `DeferredDestinationScreen` می‌رود.
- ورودی نامعتبر یا پشتیبانی‌نشده به `DeepLinkFailureScreen` می‌رود.
- کل URI در state/log نگه‌داری نمی‌شود؛ delivery key شکست از SHA-256 محدودشده ساخته می‌شود.
- authenticated boundary بدون `AuthenticationState.SignedIn` قابل عبور نیست.
- این فاز هیچ Post/Profile/Group/Chat feature و هیچ DSN-01 shell ایجاد نمی‌کند.
