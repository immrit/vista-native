[CmdletBinding()]
param(
    [string]$Output = "artifacts/artifact-metadata.json"
)

$ErrorActionPreference = "Stop"
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$outputPath = Join-Path $repositoryRoot $Output
$outputDirectory = Split-Path -Parent $outputPath
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

$apks = @(Get-ChildItem -Path (Join-Path $repositoryRoot "app/build/outputs/apk") -Recurse -File -Filter "*.apk")
if ($apks.Count -eq 0) {
    throw "No APK artifacts found."
}

$metadata = @($apks | Sort-Object FullName | ForEach-Object {
    $relative = $_.FullName.Substring($repositoryRoot.Length + 1).Replace("\", "/")
    $segments = $relative -split "/"
    $flavor = $segments[-3]
    $buildType = $segments[-2]
    $applicationId = if ($flavor -eq "production") {
        "ir.coffevista.vista"
    } else {
        "ir.coffevista.vista_native"
    }
    $signed = $buildType -eq "debug" -or $env:VISTA_SIGNING_STORE_FILE

    [pscustomobject][ordered]@{
        path = $relative
        sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        bytes = $_.Length
        flavor = $flavor
        buildType = $buildType
        applicationId = $applicationId
        signing = if ($signed) { "signed" } else { "unsigned-no-production-secret" }
        minified = $buildType -eq "release"
        resourceShrinking = $buildType -eq "release"
    }
})

$metadata | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $outputPath -Encoding utf8
Write-Host "Artifact metadata written to $outputPath"
$metadata | Format-Table path, sha256, bytes, applicationId, signing -AutoSize
