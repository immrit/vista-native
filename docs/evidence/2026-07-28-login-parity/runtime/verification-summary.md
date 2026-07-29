# API 33 Runtime Verification Summary

Date: 2026-07-29

## Full instrumentation

Command:

```powershell
adb shell am instrument -w -r ir.coffevista.vista_native.test/androidx.test.runner.AndroidJUnitRunner
```

Result excerpt:

```text
INSTRUMENTATION_STATUS: numtests=23
Time: 98.753
OK (23 tests)
INSTRUMENTATION_CODE: -1
```

All 23 tests passed; failed 0, errors 0, skipped 0.

## Clean install and routes

- `adb uninstall ir.coffevista.vista_native.test` → `Success`
- `adb uninstall ir.coffevista.vista_native` → `Success`
- final Beta Debug `adb install` → `Success`
- launcher `monkey` start → Native `MainActivity`
- `offline-no-session` cold start → Login title present
- `offline-valid-session` cold start → Shell/Feed navigation present

Installed metadata:

```text
package=ir.coffevista.vista_native
activity=ir.coffevista.vista_native/.MainActivity
versionName=1.0-beta
versionCode=1
targetSdk=36
```

Emulator:

```text
Medium_Phone_2 / emulator-5554
API=33
physical=1080x2400
density=420
rotation=0
theme=light
```

## Large Persian text, keyboard, and process recreation

Commands used `settings put system font_scale 1.3`, fixture launch,
`uiautomator dump`, real-field tap, `dumpsys input_method`, force-stop, and a
second cold fixture launch. Font scale was restored to `1.0`.

Result:

```text
font_scale=1.3
LARGE_FONT_PID=15693 TITLE_PRESENT=True
IME_SHOWN=True BUTTON_PRESENT=True
PROCESS_BEFORE=15693
AFTER_FORCE_STOP=
AFTER_RESTART=15856 TITLE_PRESENT=True
font_scale_restored=1.0
```

This verifies that large Persian Login content stays visible, the real Gboard
does not hide the primary action, and a new process relaunches to the same
usable Login route.

## Logcat

Search patterns:

```text
FATAL EXCEPTION
ANR in
SQLiteException
Room.*Exception
Hilt.*Exception
NoSuchMethodError
VerifyError
SerializationException
```

Result: no app crash/ANR match. `AndroidRuntime` entries were only the normal
`monkey` and `uiautomator` command processes, each exiting normally.

## Secret and leakage checks

```text
scripts/secret-scan.ps1
Secret scan passed: 407 tracked/untracked files, 0 findings.
```

Main-source search for `AuthVisualParity`, `vista-login-parity`, screenshot
names, `CurrentRoute`, and `DeepLinkDebug` returned zero matches.
