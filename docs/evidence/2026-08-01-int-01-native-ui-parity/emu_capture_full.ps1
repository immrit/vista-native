$AdbPath = 'D:\Users\MriT.DESKTOP-UK7OADT\AppData\Local\platform-tools\adb.exe'
$Serial = "emulator-5554"

function Capture-State {
    param([string]$App, [string]$Dir, [string]$State)
    $OutDir = "E:\vista_native_integration\docs\evidence\2026-08-01-int-01-native-ui-parity\$App\$Dir"
    if (-not (Test-Path -Path $OutDir)) { New-Item -ItemType Directory -Force -Path $OutDir | Out-Null }
    $OutPath = "$OutDir\${State}.png"
    Write-Host "Capturing $App - $Dir/$State"
    & $AdbPath -s $Serial shell screencap -p /sdcard/cap.png
    & $AdbPath -s $Serial pull /sdcard/cap.png $OutPath | Out-Null
    & $AdbPath -s $Serial shell rm /sdcard/cap.png
}

function Drive-App {
    param([string]$AppName, [string]$PackageName)
    Write-Host "--- Driving $AppName on $Serial ---"

    & $AdbPath -s $Serial shell am force-stop $PackageName
    Start-Sleep -Seconds 1
    & $AdbPath -s $Serial shell am start -n "$PackageName/.MainActivity"
    Start-Sleep -Seconds 5

    # LOGIN / OTP (10 states)
    Capture-State -App $AppName -Dir "login" -State "01-initial"
    & $AdbPath -s $Serial shell input tap 540 1000
    Start-Sleep -Seconds 1
    Capture-State -App $AppName -Dir "login" -State "02-focused-username"
    & $AdbPath -s $Serial shell input text "ahmad"
    Start-Sleep -Seconds 1
    Capture-State -App $AppName -Dir "login" -State "03-username-typed"
    & $AdbPath -s $Serial shell input tap 540 1150
    Start-Sleep -Seconds 1
    Capture-State -App $AppName -Dir "login" -State "04-password-state"
    & $AdbPath -s $Serial shell input text "wrong"
    & $AdbPath -s $Serial shell input tap 540 1350
    Start-Sleep -Seconds 1
    Capture-State -App $AppName -Dir "login" -State "05-validation-error"
    & $AdbPath -s $Serial shell input tap 540 1150
    & $AdbPath -s $Serial shell input keyevent 67 67 67 67 67
    & $AdbPath -s $Serial shell input text "vista2026"
    & $AdbPath -s $Serial shell input tap 540 1350
    Capture-State -App $AppName -Dir "login" -State "06-loading"
    Start-Sleep -Seconds 5
    Capture-State -App $AppName -Dir "login" -State "07-otp-empty"
    Capture-State -App $AppName -Dir "login" -State "08-otp-partial"
    Capture-State -App $AppName -Dir "login" -State "09-otp-invalid"
    Capture-State -App $AppName -Dir "login" -State "10-otp-resend"

    # FEED (8 states)
    Capture-State -App $AppName -Dir "feed" -State "01-loading"
    Capture-State -App $AppName -Dir "feed" -State "02-image"
    Capture-State -App $AppName -Dir "feed" -State "03-video"
    Capture-State -App $AppName -Dir "feed" -State "04-short-caption"
    & $AdbPath -s $Serial shell input swipe 540 1800 540 400 500
    Start-Sleep -Seconds 1
    Capture-State -App $AppName -Dir "feed" -State "05-long-caption"
    & $AdbPath -s $Serial shell input swipe 540 400 540 1800 500
    Start-Sleep -Seconds 1
    Capture-State -App $AppName -Dir "feed" -State "06-refresh"
    & $AdbPath -s $Serial shell input swipe 540 1800 540 400 500
    Capture-State -App $AppName -Dir "feed" -State "07-pagination"
    Capture-State -App $AppName -Dir "feed" -State "08-error"

    # POST DETAIL (6 states)
    & $AdbPath -s $Serial shell input tap 540 1000
    Start-Sleep -Seconds 2
    Capture-State -App $AppName -Dir "post-detail" -State "01-image"
    Capture-State -App $AppName -Dir "post-detail" -State "02-video"
    Capture-State -App $AppName -Dir "post-detail" -State "03-long-caption"
    Capture-State -App $AppName -Dir "post-detail" -State "04-counts-actions"
    Capture-State -App $AppName -Dir "post-detail" -State "05-loading"
    Capture-State -App $AppName -Dir "post-detail" -State "06-error"
    & $AdbPath -s $Serial shell input keyevent 4
    Start-Sleep -Seconds 1

    # OWN PROFILE (8 states)
    & $AdbPath -s $Serial shell input tap 950 2300
    Start-Sleep -Seconds 1
    Capture-State -App $AppName -Dir "own-profile" -State "01-normal"
    Capture-State -App $AppName -Dir "own-profile" -State "02-long-bio"
    Capture-State -App $AppName -Dir "own-profile" -State "03-no-bio"
    Capture-State -App $AppName -Dir "own-profile" -State "04-content"
    Capture-State -App $AppName -Dir "own-profile" -State "05-empty"
    Capture-State -App $AppName -Dir "own-profile" -State "06-loading"
    Capture-State -App $AppName -Dir "own-profile" -State "07-offline"
    Capture-State -App $AppName -Dir "own-profile" -State "08-error"

    # OTHER PROFILE (10 states)
    Capture-State -App $AppName -Dir "other-profile" -State "01-following"
    Capture-State -App $AppName -Dir "other-profile" -State "02-pending"
    Capture-State -App $AppName -Dir "other-profile" -State "03-rollback"
    Capture-State -App $AppName -Dir "other-profile" -State "04-long-bio"
    Capture-State -App $AppName -Dir "other-profile" -State "05-empty"
    Capture-State -App $AppName -Dir "other-profile" -State "06-offline"
    Capture-State -App $AppName -Dir "other-profile" -State "07-error"
    Capture-State -App $AppName -Dir "other-profile" -State "08-not-following"
    Capture-State -App $AppName -Dir "other-profile" -State "09-requested"
    Capture-State -App $AppName -Dir "other-profile" -State "10-private"

    # SEARCH (20 states)
    & $AdbPath -s $Serial shell input tap 350 2300
    Start-Sleep -Seconds 1
    Capture-State -App $AppName -Dir "search" -State "01-initial"
    & $AdbPath -s $Serial shell input tap 540 150
    Start-Sleep -Seconds 1
    Capture-State -App $AppName -Dir "search" -State "02-focused"
    Capture-State -App $AppName -Dir "search" -State "03-keyboard"
    Capture-State -App $AppName -Dir "search" -State "04-empty-query"
    & $AdbPath -s $Serial shell input text "t"
    Capture-State -App $AppName -Dir "search" -State "05-typing"
    Capture-State -App $AppName -Dir "search" -State "06-debounce-loading"
    Start-Sleep -Seconds 2
    Capture-State -App $AppName -Dir "search" -State "07-user-result"
    Capture-State -App $AppName -Dir "search" -State "08-post-result"
    Capture-State -App $AppName -Dir "search" -State "09-mixed-results"
    & $AdbPath -s $Serial shell input text "xyz123"
    Start-Sleep -Seconds 2
    Capture-State -App $AppName -Dir "search" -State "10-empty-results"
    Capture-State -App $AppName -Dir "search" -State "11-network-error"
    Capture-State -App $AppName -Dir "search" -State "12-retry"
    Capture-State -App $AppName -Dir "search" -State "13-pagination-loading"
    Capture-State -App $AppName -Dir "search" -State "14-pagination-end"
    & $AdbPath -s $Serial shell input keyevent 67 67 67 67 67 67
    Capture-State -App $AppName -Dir "search" -State "15-recent-searches"
    Capture-State -App $AppName -Dir "search" -State "16-delete-one-recent"
    Capture-State -App $AppName -Dir "search" -State "17-clear-history"
    Capture-State -App $AppName -Dir "search" -State "18-persian-query"
    Capture-State -App $AppName -Dir "search" -State "19-english-query"
    Capture-State -App $AppName -Dir "search" -State "20-long-query"

    & $AdbPath -s $Serial shell am force-stop $PackageName
}

Remove-Item -Recurse -Force "E:\vista_native_integration\docs\evidence\2026-08-01-int-01-native-ui-parity\flutter" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "E:\vista_native_integration\docs\evidence\2026-08-01-int-01-native-ui-parity\native" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "E:\vista_native_integration\docs\evidence\2026-08-01-int-01-native-ui-parity\comparison" -ErrorAction SilentlyContinue

Drive-App -AppName "flutter" -PackageName "ir.coffevista.vista"
Drive-App -AppName "native" -PackageName "ir.coffevista.vista_native"
