$AdbPath = 'D:\Users\MriT.DESKTOP-UK7OADT\AppData\Local\platform-tools\adb.exe'

function Capture-State {
    param([string]$App, [string]$Dir, [string]$State)
    $OutDir = "E:\vista_native_integration\docs\evidence\2026-08-01-int-01-native-ui-parity\$App\$Dir"
    if (-not (Test-Path -Path $OutDir)) { New-Item -ItemType Directory -Force -Path $OutDir | Out-Null }
    
    $OutPath = "$OutDir\${App}-${State}.png"
    Write-Host "Capturing $App - $Dir/$State"
    & $AdbPath shell screencap -p /sdcard/cap.png
    & $AdbPath pull /sdcard/cap.png $OutPath | Out-Null
    & $AdbPath shell rm /sdcard/cap.png
}

function Do-Login {
    param([string]$PackageName)
    & $AdbPath shell am force-stop $PackageName
    Start-Sleep -Seconds 1
    & $AdbPath shell am start -n "$PackageName/.MainActivity"
    Start-Sleep -Seconds 5
    
    # Check if already logged in (look for bottom nav)
    # If not, enter credentials:
    # We blindly click Username (540, 1000) and Password (540, 1150) and Login (540, 1350)
    & $AdbPath shell input tap 540 1000
    Start-Sleep -Milliseconds 500
    & $AdbPath shell input text "ahmad"
    & $AdbPath shell input tap 540 1150
    Start-Sleep -Milliseconds 500
    & $AdbPath shell input text "vista2026"
    & $AdbPath shell input tap 540 1350
    Start-Sleep -Seconds 5
}

function Drive-App {
    param([string]$AppName, [string]$PackageName)
    
    # 1. Login and get to Feed
    Do-Login -PackageName $PackageName
    
    # Feed states
    Capture-State -App $AppName -Dir "feed" -State "01-initial-loading"
    Start-Sleep -Seconds 4
    Capture-State -App $AppName -Dir "feed" -State "02-image-post"
    & $AdbPath shell input swipe 540 1800 540 400 500
    Start-Sleep -Seconds 2
    Capture-State -App $AppName -Dir "feed" -State "03-long-caption"
    
    # Post Detail
    & $AdbPath shell input tap 540 1000  # Tap middle of post
    Start-Sleep -Seconds 2
    Capture-State -App $AppName -Dir "post-detail" -State "01-image"
    & $AdbPath shell input swipe 540 1800 540 400 500
    Start-Sleep -Seconds 1
    Capture-State -App $AppName -Dir "post-detail" -State "02-long-caption"
    
    # Back to Feed
    & $AdbPath shell input keyevent 4
    Start-Sleep -Seconds 1
    
    # Own Profile
    # Tap Profile Tab (Bottom right, e.g. X=950, Y=2300)
    & $AdbPath shell input tap 950 2300
    Start-Sleep -Seconds 2
    Capture-State -App $AppName -Dir "own-profile" -State "01-normal"
    & $AdbPath shell input swipe 540 1800 540 400 500
    Start-Sleep -Seconds 1
    Capture-State -App $AppName -Dir "own-profile" -State "02-posts-content"
    
    # Search
    # Tap Search Tab (X=350, Y=2300)
    & $AdbPath shell input tap 350 2300
    Start-Sleep -Seconds 2
    Capture-State -App $AppName -Dir "search" -State "01-initial"
    
    # Tap Search Field (X=540, Y=150)
    & $AdbPath shell input tap 540 150
    Start-Sleep -Seconds 1
    Capture-State -App $AppName -Dir "search" -State "02-focused"
    
    & $AdbPath shell input text "test"
    Start-Sleep -Seconds 2
    Capture-State -App $AppName -Dir "search" -State "03-user-result"
    
    # Other Profile
    # Tap first user result (X=540, Y=350)
    & $AdbPath shell input tap 540 350
    Start-Sleep -Seconds 2
    Capture-State -App $AppName -Dir "other-profile" -State "01-not-following"
    
    # Back out
    & $AdbPath shell input keyevent 4
    & $AdbPath shell input keyevent 4
}

Drive-App -AppName "native" -PackageName "ir.coffevista.vista_native"
Drive-App -AppName "flutter" -PackageName "ir.coffevista.vista"
