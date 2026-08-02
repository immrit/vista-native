param(
    [string]$Serial = "emulator-5554",
    [string]$Package = "ir.coffevista.vista_native",
    [string]$Activity = "ir.coffevista.vista_native.MainActivity",
    [Parameter(Mandatory = $true)]
    [string]$ApkPath,
    [Parameter(Mandatory = $true)]
    [string]$MetadataPath,
    [string]$CapturePath,
    [string]$Fixture,
    [string]$ExpectedUiText,
    [switch]$InstallClean,
    [switch]$Launch
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$defaultSdk = "D:\Users\MriT.DESKTOP-UK7OADT\AppData\Local"
$sdkRoot = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} else {
    $defaultSdk
}
$adb = Join-Path $sdkRoot "platform-tools\adb.exe"
if (-not (Test-Path -LiteralPath $adb)) {
    throw "ADB was not found at $adb"
}

$resolvedApk = (Resolve-Path -LiteralPath $ApkPath).Path
$resolvedMetadata = [System.IO.Path]::GetFullPath($MetadataPath)
$metadataDirectory = Split-Path -Parent $resolvedMetadata
if (-not (Test-Path -LiteralPath $metadataDirectory)) {
    New-Item -ItemType Directory -Path $metadataDirectory -Force | Out-Null
}

function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
    $previousErrorPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $output = & $adb -s $Serial @Arguments 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorPreference
    }
    if ($exitCode -ne 0) {
        throw "ADB failed: $($Arguments -join ' ')`n$($output -join "`n")"
    }
    return @($output)
}

function Read-Setting {
    param([string]$Namespace, [string]$Name)
    return ((Invoke-Adb shell settings get $Namespace $Name) -join "").Trim()
}

function Wait-ForUiText {
    param([string]$Text, [int]$TimeoutSeconds = 15)
    if ([string]::IsNullOrWhiteSpace($Text)) { return }
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        try {
            Invoke-Adb shell uiautomator dump /sdcard/int01-window.xml | Out-Null
            $xml = (Invoke-Adb shell cat /sdcard/int01-window.xml) -join "`n"
            if ($xml.Contains($Text)) { return }
        } catch {
            # Accessibility can briefly expose no root during activity startup.
            # Keep polling until the same bounded semantic deadline expires.
        }
        Start-Sleep -Milliseconds 250
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "Timed out waiting for UI text '$Text'."
}

function Wait-ForResumedActivity {
    param([int]$TimeoutSeconds = 10)
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        $match = (Invoke-Adb shell dumpsys activity activities) |
            Select-String -Pattern "mResumedActivity|topResumedActivity|ResumedActivity" |
            Select-Object -First 1
        if ($null -ne $match) { return $match.Line.Trim() }
        Start-Sleep -Milliseconds 250
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "Timed out waiting for a resumed activity."
}

function Assert-Png {
    param([string]$Path)
    Add-Type -AssemblyName System.Drawing
    $bitmap = [System.Drawing.Bitmap]::FromFile($Path)
    try {
        if ($bitmap.Width -ne 1080 -or $bitmap.Height -ne 2400) {
            throw "Unexpected screenshot dimensions $($bitmap.Width)x$($bitmap.Height)."
        }
        $sum = 0L
        $samples = 0L
        for ($y = 0; $y -lt $bitmap.Height; $y += 80) {
            for ($x = 0; $x -lt $bitmap.Width; $x += 80) {
                $pixel = $bitmap.GetPixel($x, $y)
                $sum += $pixel.R + $pixel.G + $pixel.B
                $samples += 3
            }
        }
        $meanChannel = $sum / [double]$samples
        if ($meanChannel -lt 5.0) {
            throw "Screenshot is blank or effectively black (mean channel $meanChannel)."
        }
        return [ordered]@{
            width = $bitmap.Width
            height = $bitmap.Height
            sampledMeanChannel = [Math]::Round($meanChannel, 3)
        }
    } finally {
        $bitmap.Dispose()
    }
}

$online = @(
    & $adb devices | Select-String -Pattern "\tdevice$" | ForEach-Object {
        ($_.Line -split "\s+")[0]
    }
)
if ($online.Count -ne 1 -or $online[0] -ne $Serial) {
    throw "Expected exactly one canonical device '$Serial'; online: $($online -join ', ')."
}

$api = ((Invoke-Adb shell getprop ro.build.version.sdk) -join "").Trim()
$model = ((Invoke-Adb shell getprop ro.boot.qemu.avd_name) -join "").Trim()
$physicalSize = ((Invoke-Adb shell wm size) -join " ").Trim()
if ($api -ne "33") { throw "Expected API 33; found API $api." }
if ($physicalSize -notmatch "1080x2400") {
    throw "Expected 1080x2400 device; wm size returned '$physicalSize'."
}

$previous = [ordered]@{
    accelerometerRotation = Read-Setting system accelerometer_rotation
    userRotation = Read-Setting system user_rotation
    fontScale = Read-Setting system font_scale
    nightMode = ((Invoke-Adb shell cmd uimode night) -join " ").Trim()
}

$captureInfo = $null
try {
    Invoke-Adb shell settings put system accelerometer_rotation 0 | Out-Null
    Invoke-Adb shell settings put system user_rotation 0 | Out-Null
    Invoke-Adb shell settings put system font_scale 1.0 | Out-Null
    Invoke-Adb shell cmd uimode night no | Out-Null

    if ($InstallClean) {
        $installedBefore = ((Invoke-Adb shell pm list packages $Package) -join "") -match $Package
        if ($installedBefore) {
            Invoke-Adb uninstall $Package | Out-Null
        }
        Invoke-Adb install $resolvedApk | Out-Null
    }

    $packageLine = ((Invoke-Adb shell pm list packages $Package) -join "").Trim()
    if ($packageLine -ne "package:$Package") {
        throw "Expected installed package '$Package'; got '$packageLine'."
    }

    if ($Launch) {
        $launchArguments = @("shell", "am", "start", "-W", "-n", "$Package/$Activity")
        if (-not [string]::IsNullOrWhiteSpace($Fixture)) {
            $launchArguments += @("--es", "vista.foundation.fixture", $Fixture)
        }
        Invoke-Adb @launchArguments | Out-Null
    }

    Wait-ForUiText -Text $ExpectedUiText
    $resumed = Wait-ForResumedActivity
    $shortActivity = if ($Activity.StartsWith($Package)) {
        $Activity.Substring($Package.Length)
    } else {
        $Activity
    }
    $fullComponent = "$Package/$Activity"
    $shortComponent = "$Package/$shortActivity"
    if (
        $resumed -notmatch [Regex]::Escape($fullComponent) -and
        $resumed -notmatch [Regex]::Escape($shortComponent)
    ) {
        throw "Unexpected resumed activity: $resumed"
    }

    if (-not [string]::IsNullOrWhiteSpace($CapturePath)) {
        $resolvedCapture = [System.IO.Path]::GetFullPath($CapturePath)
        $captureDirectory = Split-Path -Parent $resolvedCapture
        if (-not (Test-Path -LiteralPath $captureDirectory)) {
            New-Item -ItemType Directory -Path $captureDirectory -Force | Out-Null
        }
        $remoteCapture = "/sdcard/int01-capture.png"
        $captureArguments = @("shell", "screencap", "-p", $remoteCapture)
        Invoke-Adb @captureArguments | Out-Null
        $pullArguments = @("pull", $remoteCapture, $resolvedCapture)
        Invoke-Adb @pullArguments | Out-Null
        $cleanupArguments = @("shell", "rm", $remoteCapture)
        Invoke-Adb @cleanupArguments | Out-Null
        $captureInfo = Assert-Png -Path $resolvedCapture
        $captureInfo.path = $resolvedCapture
        $captureInfo.sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $resolvedCapture).Hash
    }

    $packageDump = (Invoke-Adb shell dumpsys package $Package) -join "`n"
    $versionName = [Regex]::Match($packageDump, "versionName=([^\r\n]+)").Groups[1].Value.Trim()
    $versionCode = [Regex]::Match($packageDump, "versionCode=(\d+)").Groups[1].Value
    $lastUpdateTime = [Regex]::Match($packageDump, "lastUpdateTime=([^\r\n]+)").Groups[1].Value.Trim()
    $installedSource = ((Invoke-Adb shell pm path $Package) -join "").Trim()

    $metadata = [ordered]@{
        capturedAtUtc = [DateTime]::UtcNow.ToString("o")
        serial = $Serial
        avd = $model
        api = [int]$api
        physicalSize = $physicalSize
        orientation = "portrait"
        theme = "light"
        fontScale = 1.0
        package = $Package
        activity = $Activity
        resumedActivity = $resumed
        installedSource = $installedSource
        installedVersionCode = $versionCode
        installedVersionName = $versionName
        installedLastUpdateTime = $lastUpdateTime
        apkPath = $resolvedApk
        apkSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $resolvedApk).Hash
        fixture = $Fixture
        expectedUiText = $ExpectedUiText
        capture = $captureInfo
    }
    $metadata | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $resolvedMetadata -Encoding utf8
    $metadata | ConvertTo-Json -Depth 8
} finally {
    $restoreAccelerometerRotation = if ([string]::IsNullOrWhiteSpace($previous.accelerometerRotation) -or $previous.accelerometerRotation -eq "null") { "1" } else { $previous.accelerometerRotation }
    Invoke-Adb shell settings put system accelerometer_rotation $restoreAccelerometerRotation | Out-Null
    $restoreUserRotation = if ([string]::IsNullOrWhiteSpace($previous.userRotation) -or $previous.userRotation -eq "null") { "0" } else { $previous.userRotation }
    Invoke-Adb shell settings put system user_rotation $restoreUserRotation | Out-Null
    $restoreFontScale = if ([string]::IsNullOrWhiteSpace($previous.fontScale) -or $previous.fontScale -eq "null") { "1.0" } else { $previous.fontScale }
    Invoke-Adb shell settings put system font_scale $restoreFontScale | Out-Null
    $nightTarget = if ($previous.nightMode -match "yes") { "yes" } elseif (
        $previous.nightMode -match "auto"
    ) { "auto" } else { "no" }
    Invoke-Adb shell cmd uimode night $nightTarget | Out-Null
}
