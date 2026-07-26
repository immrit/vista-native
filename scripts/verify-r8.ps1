[CmdletBinding()]
param(
    [string[]]$Variants = @("betaRelease", "productionRelease")
)

$ErrorActionPreference = "Stop"
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$criticalClasses = @(
    "ir.coffevista.vista_native.MainActivity",
    "ir.coffevista.vista_native.VistaApplication",
    "ir.coffevista.vista_native.features.startup.StartupResolver",
    "ir.coffevista.vista_native.features.auth.AuthViewModel",
    "ir.coffevista.vista_native.features.auth.data.OkHttpAuthRemoteDataSource"
)

foreach ($variant in $Variants) {
    $mappingRoot = Join-Path $repositoryRoot "app/build/outputs/mapping/$variant"
    foreach ($requiredFile in @("mapping.txt", "seeds.txt", "usage.txt", "configuration.txt")) {
        $path = Join-Path $mappingRoot $requiredFile
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            throw "Missing R8 artifact: $path"
        }
    }

    $mappingPath = Join-Path $mappingRoot "mapping.txt"
    foreach ($className in $criticalClasses) {
        $retained = Select-String -LiteralPath $mappingPath -Pattern "^$([Regex]::Escape($className)) -> "
        if (-not $retained) {
            throw "Critical startup/auth class is absent from R8 mapping in ${variant}: $className"
        }
    }

    Write-Host "R8 verified for ${variant}: artifacts exist and critical classes remain in mapping."
}
