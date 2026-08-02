$AdbPath = 'D:\Users\MriT.DESKTOP-UK7OADT\AppData\Local\platform-tools\adb.exe'

function Capture-State {
    param([string]$App, [string]$State)
    $OutPath = "E:\vista_native_integration\docs\evidence\2026-08-01-int-01-native-ui-parity\$App\$State.png"
    Write-Host "Capturing $App - $State to $OutPath"
    & $AdbPath shell screencap -p /sdcard/cap.png
    & $AdbPath pull /sdcard/cap.png $OutPath | Out-Null
    & $AdbPath shell rm /sdcard/cap.png
}

function Run-App-Capture {
    param([string]$AppName, [string]$PackageName)
    
    Write-Host "--- Starting sequence for $AppName ---"
    & $AdbPath shell am force-stop $PackageName
    Start-Sleep -Seconds 2
    
    # 1. Login Initial (already done, but do it to get to known state)
    & $AdbPath shell am start -n "$PackageName/.MainActivity"
    Start-Sleep -Seconds 6
    
    # 2. Login Focused
    # Tap Username
    & $AdbPath shell input tap 540 1000
    Start-Sleep -Seconds 1
    & $AdbPath shell input text "test"
    Start-Sleep -Seconds 1
    Capture-State -App $AppName -State "login_focused"
    
    # Clear and Login
    & $AdbPath shell am force-stop $PackageName
    Start-Sleep -Seconds 2
    & $AdbPath shell am start -n "$PackageName/.MainActivity"
    Start-Sleep -Seconds 6
    
    # Enter credentials
    & $AdbPath shell input tap 540 1000
    Start-Sleep -Seconds 1
    & $AdbPath shell input text "ahmad"
    Start-Sleep -Seconds 1
    
    # Tap Password
    & $AdbPath shell input tap 540 1150
    Start-Sleep -Seconds 1
    & $AdbPath shell input text "vista2026"
    Start-Sleep -Seconds 1
    
    # Tap Login Button (around Y=1350)
    & $AdbPath shell input tap 540 1350
    
    # 3. Feed Loading
    Start-Sleep -Milliseconds 500
    Capture-State -App $AppName -State "feed_initial_loading"
    
    # 4. Feed Image Post
    Start-Sleep -Seconds 4
    Capture-State -App $AppName -State "feed_image_post"
    
    # 5. Feed Long Caption
    & $AdbPath shell input swipe 540 1800 540 400 500
    Start-Sleep -Seconds 2
    Capture-State -App $AppName -State "feed_long_caption"
}

Run-App-Capture -AppName "flutter" -PackageName "ir.coffevista.vista"
Run-App-Capture -AppName "native" -PackageName "ir.coffevista.vista_native"
