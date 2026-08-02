Add-Type -AssemblyName System.Drawing

function Take-Screenshot {
    param (
        [string]$AdbPath,
        [string]$AppPackage,
        [string]$AppActivity,
        [string]$OutputPath,
        [string]$DeviceCapPath = "/sdcard/cap.png"
    )

    Write-Host "Force stopping $AppPackage..."
    & $AdbPath shell am force-stop $AppPackage
    Start-Sleep -Seconds 2

    Write-Host "Starting $AppPackage/$AppActivity..."
    & $AdbPath shell am start -W -n "$AppPackage/$AppActivity"

    Write-Host "Waiting 5 seconds for app to load..."
    Start-Sleep -Seconds 5

    Write-Host "Capturing screen to $DeviceCapPath..."
    & $AdbPath shell screencap -p $DeviceCapPath

    Write-Host "Pulling screen capture to $OutputPath..."
    & $AdbPath pull $DeviceCapPath $OutputPath

    Write-Host "Cleaning up $DeviceCapPath..."
    & $AdbPath shell rm $DeviceCapPath

    # Verify dimensions and image data
    $img = [System.Drawing.Image]::FromFile($OutputPath)
    $width = $img.Width
    $height = $img.Height
    $img.Dispose()

    if ($width -ne 1080 -or $height -ne 2400) {
        Write-Warning "Dimensions mismatch: ${width}x${height} instead of 1080x2400"
    } else {
        Write-Host "Screenshot captured successfully: ${width}x${height}"
    }
}
