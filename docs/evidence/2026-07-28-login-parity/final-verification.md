# VISUAL-PARITY-01 Final Verification

Date: 2026-07-29  
Status: `VISUAL-PARITY-01 Complete — Login Screen Parity Passed`  
Stop gate: `Login Visual Parity Ready — Awaiting User Approval`

## Runtime identities

| App | Package/build |
| --- | --- |
| Flutter reference | `ir.coffevista.vista`, `2.6.2+4049`, debug APK `E:\vista\build\app\outputs\flutter-apk\app-debug.apk`, target SDK 34 |
| Native | `ir.coffevista.vista_native`, label `ویستا`, `1.0-beta` (`1`), `MainActivity`, Beta Debug/Release, target SDK 36 |

Emulator: `Medium_Phone_2` / `emulator-5554`, API 33, physical
1080×2400, density 420, portrait, light theme, Gboard.

Native package identity was verified from the installed package resolver and
`dumpsys package`; the launcher resolves to
`ir.coffevista.vista_native/.MainActivity`. Flutter identity and version were
verified from its read-only build and source metadata.

## Audit, assets, icons, and fonts

- 4,017 Flutter asset/font files have a per-file inventory with path, type,
  dimensions, SHA-256, consumer, migration decision, destination, and notes:
  `docs/audits/2026-07-28-flutter-visual-assets-index.csv`.
- Exact light and dark Flutter Login logo rasters were copied byte-for-byte:
  - `black-logo.png` → `vista_auth_logo_light.png`,
    SHA-256 `13A42B...2795C`;
  - `logo-white.png` → `vista_auth_logo_dark.png`,
    SHA-256 `6DF423...AB510`.
- The seven Native Vazirmatn files for weights 300–900 were already present
  and match their Flutter sources byte-for-byte.
- Flutter uses Material icons for person, lock, visibility, visibility-off,
  and the directional back control. Native uses canonical Android vectors;
  no project-specific icon was replaced by an approximation.
- No external file was downloaded, converted, recompressed, or migrated.
  The logos/fonts are first-party assets already present in the supplied
  Vista Flutter repository. No separate license metadata was found; provenance
  and hashes are retained in the audit and manifest.

References:

- `docs/audits/2026-07-28-flutter-visual-assets-inventory.md`
- `docs/evidence/2026-07-28-login-parity/asset-migration-manifest.md`

## Token reconciliation

The Login/shared-foundation consumers now use the exact Flutter palette and
semantics for light/dark background, surfaces, field fill, border, primary,
accent, content, hint, error, divider, focus, disabled and ribbon colors.
Vazirmatn family/weights, the Login typography scale, radii, borders, spacing,
field/button/OTP dimensions, elevation, status/navigation bar treatment, and
150/600ms motion durations were reconciled. Hard-coded Login values remain only
where Flutter defines a real screen-specific value.

`CurrentRoute`, `DeepLinkDebug`, fixture labels, route names, and internal IDs
are absent from product UI and from the searched main source sets.

## Real screenshots

Flutter: 9/9 PNGs in `flutter/`; Native: 9/9 PNGs in `native/`. Every image is
1080×2400 and was captured from the same real API-33 Emulator; no Preview was
used.

| # | Flutter state | Native state |
| ---: | --- | --- |
| 01 | Login initial | Login initial |
| 02 | Login focused | Login focused |
| 03 | Login filled (`09123456789`) | Login filled (`09123456789`) |
| 04 | Login validation error | Login validation error |
| 05 | Login loading/disabled | Login loading/disabled |
| 06 | OTP initial | OTP initial |
| 07 | OTP partial, timer 25 | OTP partial, timer 25 |
| 08 | OTP error | OTP error |
| 09 | OTP resend/error | OTP resend/error |

Phone and OTP content are LTR inside the RTL shell; Persian and mixed text,
cursor position, keyboard states, and the OTP back direction were visually and
instrumentally verified.

## Automated comparison

Command:

```powershell
C:\Users\MriT.DESKTOP-UK7OADT\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe tools\login_visual_compare.py
```

Outputs: 9 side-by-side images, 9 alpha overlays, 9 thresholded heatmaps with
bounding boxes, `comparison/metrics.csv`, and
`login-parity-contact-sheet.png` with `Flutter | Native | Diff` rows.

| State | Mismatch over RGB threshold 16 | Disposition |
| --- | ---: | --- |
| 01 initial | 0.9908% | pass |
| 02 focused | 1.7232% | pass |
| 03 filled | 1.6791% | pass |
| 04 validation | 2.1374% | pass |
| 05 loading | 1.6453% | pass |
| 06 OTP initial | 0.9708% | pass |
| 07 OTP partial | 0.9893% | pass |
| 08 OTP error | 1.0707% | pass |
| 09 OTP resend | 0.9218% | pass |

The percentages include system-clock changes, platform antialiasing, cursor
blink, snackbar blur and indeterminate-spinner phase. Direct inspection and an
independent second review found no acceptance-blocking structural, asset,
primary-color, state, RTL/LTR, clipping, placeholder, or debug-UI mismatch.

## Iterative mismatch closure

| State | Element | Flutter | Native before | Native after | Result |
| --- | --- | --- | --- | --- | --- |
| Login all | logo | exact black/white Vista asset, 100dp | placeholder-like branding | byte-identical source asset, 100dp | pass |
| Login all | vertical layout | viewport-centered Flutter structure | top/spacing drift | calibrated to measured runtime | pass |
| Login all | field/button | 53dp/51dp visible controls, 12dp radius | 56dp/52dp | 53dp/51dp, exact radii/borders | pass |
| Focused/filled | LTR input | cursor/phone LTR | RTL/capture drift | matching LTR and fixture value | pass |
| Validation | error state | canonical copy/snackbar | copy/alignment drift | exact copy/alignment/colors | pass |
| Loading | disabled state/footer | canonical disabled fill and footer baseline | wrong fill/baseline drift | exact fill and final 2.5dp baseline correction | pass |
| OTP all | content/cells | centered, 56×60dp, 8dp gaps | top-biased/stretched | exact fixed geometry | pass |
| OTP all | back | Material arrow at RTL right | text glyph/wrong side | canonical vector/right side | pass |
| OTP states | timer/error/resend | real canonical states | stale/incorrect capture | explicit matching states | pass |

Final main-container geometry differs by at most 0–5 pixels, below 2dp on the
420-density reference device. Remaining text-ink width differences (about
8–9px for two long strings), spinner phase and snackbar blur are renderer-level
residuals; style, centerline and control geometry match.

## Tests

Gradle unit-report roots:

- `.gradle/visual-parity-final-20260729-m/core/designsystem/test-results/testDebugUnitTest/`
- `.gradle/visual-parity-final-20260729-m/feature/auth/test-results/testDebugUnitTest/`
- `.gradle/visual-parity-final-20260729-m/feature/shell/test-results/testDebugUnitTest/`
- `.gradle/visual-parity-final-20260729-m/feature/feed/test-results/testDebugUnitTest/`
- `.gradle/visual-parity-final-20260729-m/feature/profile/test-results/testDebugUnitTest/`

Instrumentation command:

```powershell
adb shell am instrument -w -r ir.coffevista.vista_native.test/androidx.test.runner.AndroidJUnitRunner
```

| Suite | Total | Passed | Failed | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Design System unit | 9 | 9 | 0 | 0 | 0 |
| Auth unit | 32 | 32 | 0 | 0 | 0 |
| Shell unit | 3 | 3 | 0 | 0 | 0 |
| Feed unit | 24 | 24 | 0 | 0 | 0 |
| Profile unit | 35 | 35 | 0 | 0 | 0 |
| Instrumentation | 23 | 23 | 0 | 0 | 0 |
| **Total** | **126** | **126** | **0** | **0** | **0** |

Instrumentation breakdown: Auth visual parity 2, Feed runtime 3, Hilt smoke 1,
Navigation/deep-link 3, Other Profile runtime 3, Startup/Auth/Shell 11.

## Build, lint, R8, leakage, and repository checks

Final Gradle command:

```powershell
.\gradlew.bat `
  :core:designsystem:testDebugUnitTest :feature:auth:testDebugUnitTest `
  :feature:shell:testDebugUnitTest :feature:feed:testDebugUnitTest `
  :feature:profile:testDebugUnitTest `
  :app:assembleBetaDebug :app:assembleBetaRelease `
  :core:designsystem:lintDebug :feature:auth:lintDebug `
  :feature:shell:lintDebug :app:lintBetaDebug `
  "-Pvista.isolatedBuildDir=true" `
  "-Pvista.isolatedBuildOutput=visual-parity-final-20260729-m" `
  "-Pvista.buildLogicOutput=visual-parity-build-logic-final-20260729-d" `
  "-Pkotlin.incremental=false" --no-daemon --console=plain
```

Result: `BUILD SUCCESSFUL` (957 tasks).

- Beta Debug APK: 16,811,741 bytes.
- Beta Release unsigned APK: 5,414,577 bytes.
- R8 mapping:
  `.gradle/visual-parity-final-20260729-m/app/outputs/mapping/betaRelease/mapping.txt`,
  38,554,317 bytes.
- Lint reports: Design System 0 issues; Auth 0 issues; Shell 2 non-error hints
  (`AutoboxingStateCreation`); app 36 existing warnings; all four have 0 errors.
- Secret scan: 408 tracked repository files in the final clean snapshot,
  0 findings.
- Visual capture control and screenshot naming exist only under `androidTest`;
  searched `main` source sets contain 0 parity-fixture hits.
- Release compiled and R8-minified without a visual fixture binding.
- No build logic, flavor, dependency, Feed, Profile, Follow, Backend, or Flutter
  source was changed.
- `git diff --check`: pass.

Persisted runtime command/result summary:
`runtime/verification-summary.md`.

## Runtime API 33 and regressions

- Clean install: test and Native packages uninstalled, Beta Debug installed
  successfully.
- Launcher cold-started the installed Native package; fixtureless startup and
  `offline-no-session` both reached the real centered Login.
- `offline-valid-session` reached Shell/Feed on the installed APK.
- Login/OTP initial, focus, fill, validation, loading, disabled, timer, error,
  resend, keyboard, RTL, rotation, Activity recreation and relaunch passed.
- With Android font scale set to 1.3, the large Persian title and primary
  action remained visible; real Gboard reported `mInputShown=true`. After
  force-stop, the process PID changed from 15693 to 15856 and the same Login
  route remained usable. Font scale was restored to 1.0.
- Full 23-test instrumentation regression passed Startup, Auth, Hilt, Shell,
  Feed, Feed Detail, Own Profile, Other Profile, Follow rollback, tab
  restoration, deep links, process recreation and logout.
- Final logcat contains no app `FATAL EXCEPTION`, ANR, `SQLiteException`,
  `NoSuchMethodError`, `VerifyError`, or `SerializationException`. The only
  `AndroidRuntime` lines were normal `monkey`/`uiautomator` command processes
  exiting with code 0.

## Known residual differences

No acceptance-blocking difference remains. Residual pixels are limited to
Flutter Skia versus Compose font shaping/antialiasing, system clock/status-icon
capture time, snackbar shadow blur, cursor blink and spinner/gradient animation
phase. States 02/03 also include an external Gboard suggestion-strip difference
(Native clipboard chip versus Flutter toolbar). These do not alter asset
identity, exact source token colors, main
geometry, state completeness, RTL/LTR behavior, clipping or usability.
