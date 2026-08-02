$AdbPath = 'D:\Users\MriT.DESKTOP-UK7OADT\AppData\Local\platform-tools\adb.exe'
$Serial = "127.0.0.1:21503"

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
    
    Write-Host "--- Driving $AppName ---"
    & $AdbPath -s $Serial shell am force-stop $PackageName
    Start-Sleep -Seconds 1
    & $AdbPath -s $Serial shell am start -n "$PackageName/.MainActivity"
    Start-Sleep -Seconds 5
    
    # Login
    & $AdbPath -s $Serial shell input tap 540 1000
    Start-Sleep -Milliseconds 500
    & $AdbPath -s $Serial shell input text "ahmad"
    & $AdbPath -s $Serial shell input tap 540 1150
    Start-Sleep -Milliseconds 500
    & $AdbPath -s $Serial shell input text "vista2026"
    & $AdbPath -s $Serial shell input tap 540 1350
    Start-Sleep -Seconds 5
    
    # Feed
    Capture-State -App $AppName -Dir "feed" -State "01-initial-loading"
    Start-Sleep -Seconds 3
    Capture-State -App $AppName -Dir "feed" -State "02-image-post"
    & $AdbPath -s $Serial shell input swipe 540 1800 540 400 500
    Start-Sleep -Seconds 2
    Capture-State -App $AppName -Dir "feed" -State "03-long-caption"
    
    # Post Detail
    & $AdbPath -s $Serial shell input tap 540 1000
    Start-Sleep -Seconds 2
    Capture-State -App $AppName -Dir "post-detail" -State "01-image"
    & $AdbPath -s $Serial shell input swipe 540 1800 540 400 500
    Start-Sleep -Seconds 1
    Capture-State -App $AppName -Dir "post-detail" -State "02-long-caption"
    & $AdbPath -s $Serial shell input keyevent 4
    Start-Sleep -Seconds 1
    
    # Own Profile
    & $AdbPath -s $Serial shell input tap 950 2300
    Start-Sleep -Seconds 2
    Capture-State -App $AppName -Dir "own-profile" -State "01-normal"
    & $AdbPath -s $Serial shell input swipe 540 1800 540 400 500
    Start-Sleep -Seconds 1
    Capture-State -App $AppName -Dir "own-profile" -State "02-posts-content"
    
    # Search
    & $AdbPath -s $Serial shell input tap 350 2300
    Start-Sleep -Seconds 2
    Capture-State -App $AppName -Dir "search" -State "01-initial"
    & $AdbPath -s $Serial shell input tap 540 150
    Start-Sleep -Seconds 1
    Capture-State -App $AppName -Dir "search" -State "02-focused"
    & $AdbPath -s $Serial shell input text "test"
    Start-Sleep -Seconds 2
    Capture-State -App $AppName -Dir "search" -State "03-user-result"
    
    # Other Profile
    & $AdbPath -s $Serial shell input tap 540 350
    Start-Sleep -Seconds 2
    Capture-State -App $AppName -Dir "other-profile" -State "01-not-following"
    
    & $AdbPath -s $Serial shell am force-stop $PackageName
}

Drive-App -AppName "flutter" -PackageName "ir.coffevista.vista"
Drive-App -AppName "native" -PackageName "ir.coffevista.vista_native"
