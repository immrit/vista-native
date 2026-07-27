param(
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath $RepositoryRoot).Path
$sourceRoots = @(
    (Join-Path $root "app\src\main\java"),
    (Join-Path $root "feature")
)
$errors = [System.Collections.Generic.List[string]]::new()

foreach ($sourceRoot in $sourceRoots) {
    $files = @(
        Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Filter *.kt -ErrorAction SilentlyContinue
    )
    foreach ($file in $files) {
        $rawColors = @(Select-String -LiteralPath $file.FullName -Pattern 'Color\(0x[0-9A-Fa-f]+\)')
        foreach ($match in $rawColors) {
            $errors.Add("Raw color outside :core:designsystem: $($file.FullName):$($match.LineNumber)")
        }
        $duplicateTokenObjects = @(
            Select-String -LiteralPath $file.FullName -Pattern 'object\s+\w*(Colors|Spacing|Radii|Elevation|Dimensions)\b'
        )
        foreach ($match in $duplicateTokenObjects) {
            $errors.Add("Token owner outside :core:designsystem: $($file.FullName):$($match.LineNumber)")
        }
    }
}

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output "Design token leakage verification passed."
