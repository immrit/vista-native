param(
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath $RepositoryRoot).Path
$settingsPath = Join-Path $root "settings.gradle.kts"
$settings = Get-Content -Raw -Encoding utf8 -LiteralPath $settingsPath

$modules = [ordered]@{
    ":core:common"   = "core\common"
    ":core:model"    = "core\model"
    ":core:network"  = "core\network"
    ":core:database" = "core\database"
    ":core:datastore" = "core\datastore"
    ":core:security" = "core\security"
    ":core:testing"  = "core\testing"
    ":core:worker"   = "core\worker"
    ":feature:auth"  = "feature\auth"
}

$allowedDependencies = @{
    ":core:common"    = @()
    ":core:model"     = @()
    ":core:network"   = @(":core:common")
    ":core:database"  = @()
    ":core:datastore" = @(":core:common")
    ":core:security"  = @(":core:common", ":core:model")
    ":core:testing"   = @()
    ":core:worker"    = @()
    ":feature:auth"   = @(
        ":core:common",
        ":core:datastore",
        ":core:model",
        ":core:network",
        ":core:security",
        ":core:testing",
        ":core:worker"
    )
}

$errors = [System.Collections.Generic.List[string]]::new()
$graph = @{}

foreach ($entry in $modules.GetEnumerator()) {
    $moduleName = $entry.Key
    $modulePath = Join-Path $root $entry.Value
    if ($settings -notmatch [regex]::Escape("include(`"$moduleName`")")) {
        $errors.Add("$moduleName is missing from settings.gradle.kts")
        continue
    }

    $sourceRoot = Join-Path $modulePath "src\main"
    $sources = @(
        Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Filter *.kt -ErrorAction SilentlyContinue
    )
    if ($sources.Count -eq 0) {
        $errors.Add("$moduleName has no production Kotlin consumer/source")
    }

    $buildFile = Join-Path $modulePath "build.gradle.kts"
    $buildText = Get-Content -Raw -Encoding utf8 -LiteralPath $buildFile
    $dependencies = @(
        [regex]::Matches($buildText, 'project\("(?<module>:[^"]+)"\)') |
            ForEach-Object { $_.Groups["module"].Value } |
            Sort-Object -Unique
    )
    $graph[$moduleName] = $dependencies

    foreach ($dependency in $dependencies) {
        if ($dependency -like ":feature:*") {
            $errors.Add("$moduleName must not depend on feature module $dependency")
        }
        if ($dependency -notin $allowedDependencies[$moduleName]) {
            $errors.Add("$moduleName has unapproved dependency $dependency")
        }
    }
}

$pureKotlinRoots = @(
    (Join-Path $root "core\common\src"),
    (Join-Path $root "core\model\src"),
    (Join-Path $root "feature\auth\src\main\java\ir\coffevista\vista_native\features\auth\domain")
)
foreach ($sourceRoot in $pureKotlinRoots) {
    $androidImports = @(
        Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Filter *.kt -ErrorAction SilentlyContinue |
            Select-String -Pattern '^\s*import\s+(android|androidx)\.'
    )
    if ($androidImports.Count -gt 0) {
        foreach ($match in $androidImports) {
            $errors.Add("Android type crossed a pure/domain boundary: $($match.Path):$($match.LineNumber)")
        }
    }
}

$visiting = [System.Collections.Generic.HashSet[string]]::new()
$visited = [System.Collections.Generic.HashSet[string]]::new()
function Visit-Module {
    param([string]$Module)
    if ($visiting.Contains($Module)) {
        $errors.Add("Dependency cycle detected at $Module")
        return
    }
    if ($visited.Contains($Module)) {
        return
    }
    $null = $visiting.Add($Module)
    foreach ($dependency in @($graph[$Module])) {
        if ($graph.ContainsKey($dependency)) {
            Visit-Module -Module $dependency
        }
    }
    $null = $visiting.Remove($Module)
    $null = $visited.Add($Module)
}

foreach ($module in $graph.Keys) {
    Visit-Module -Module $module
}

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output "Module boundary verification passed."
foreach ($module in $modules.Keys) {
    $dependencies = @($graph[$module])
    $rendered = if ($dependencies.Count -eq 0) { "(none)" } else { $dependencies -join ", " }
    Write-Output "$module -> $rendered"
}
