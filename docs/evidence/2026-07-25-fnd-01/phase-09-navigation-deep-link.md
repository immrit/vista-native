# Phase 9 Evidence — Type-safe Navigation and Deep-link

## Implementation evidence

- inventory و قرارداد: `docs/architecture/FND-01-NAVIGATION-DEEP-LINK.md`
- typed routeها: `app/.../navigation/AppRoute.kt`
- parser، pending-session، replay و dedup: `app/.../navigation/DeepLinkCoordinator.kt`
- typed graph و back-stack guard: `app/.../navigation/AppNavGraph.kt`
- cold/warm intent entry: `MainActivity.onCreate/onNewIntent`
- Manifest registry: فقط hostهای `post/profile/group/chat`
- controlled deferred/failure UI: `DeferredDestinationScreen.kt`

## Unit checkpoint

Command:

`gradlew :app:testBetaDebugUnitTest -Pvista.isolatedBuildDir=true -Pvista.isolatedBuildOutput=fnd-build-output-fnd45b -Pkotlin.incremental=false`

Result:

- `BUILD SUCCESSFUL`
- 2 suites / 6 tests
- 0 failure / 0 error / 0 skipped
- 5 contract tests: canonical matrix، invalid/unsupported، cold pending-session،
  logged-out post-login replay، warm dedup window و controlled failure

## Device checkpoint

Command:

`gradlew :app:connectedBetaDebugAndroidTest -Pvista.isolatedBuildDir=true -Pvista.isolatedBuildOutput=fnd-build-output-fnd44b -Pkotlin.incremental=false`

Device/result:

- `Medium_Phone_2(AVD) - API 33`
- 5 tests / 0 failed / 0 skipped
- Hilt composition smoke و WorkManager foundation tests همچنان سبز
- navigation tests: failure route restoration، warm duplicate single-effect،
  cold pending destination recreation و post-login replay دقیقاً یک‌باره
- `BUILD SUCCESSFUL`

## Defect found and closed

اجرای اولیه یک race واقعی را نشان داد: failure deep link می‌توانست قبل از پایان Startup نمایش
داده شود و سپس transition دیررس Startup آن را overwrite کند. state machine به pending-session
تغییر کرد و Startup transition با current back-stack entry guard شد. اجرای نهایی unit و device
پس از اصلاح سبز است.

## Scope boundary

Flutter و Backend فقط read-only ماندند. هیچ Feature مقصد و هیچ DSN-01 shell ساخته نشد.
