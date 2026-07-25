[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$trackedFiles = @(git -C $repositoryRoot ls-files)
if ($LASTEXITCODE -ne 0) {
    throw "Unable to enumerate tracked files."
}

$sensitiveNamePattern =
    '(^|/)(\.env($|\.)|[^/]+\.(jks|keystore|p12|pfx|pem|key)$|' +
    'keystore\.properties$|signing\.properties$|secrets?\.properties$|' +
    'credentials?[^/]*$)'
$nameHits = @($trackedFiles | Where-Object { $_ -match $sensitiveNamePattern })

$rules = [ordered]@{
    "private-key" = '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----'
    "aws-access-key" = 'AKIA[0-9A-Z]{16}'
    "github-token" = 'gh[pousr]_[A-Za-z0-9]{30,}'
    "google-api-key" = 'AIza[0-9A-Za-z_-]{35}'
    "signing-password" = '(?i)^\s*(storePassword|keyPassword)\s*=\s*(?!\$\{|providers\.|signingValues)[^#\s]{4,}\s*$'
    "client-secret" = '(?i)(client_secret|api_key|apikey)\s*[=:]\s*["''][A-Za-z0-9_\-]{16,}'
}

$contentHits = New-Object System.Collections.Generic.List[string]
foreach ($relativePath in $trackedFiles) {
    $absolutePath = Join-Path $repositoryRoot $relativePath
    if (-not (Test-Path -LiteralPath $absolutePath -PathType Leaf)) {
        continue
    }
    $item = Get-Item -LiteralPath $absolutePath
    if ($item.Length -gt 2MB) {
        continue
    }
    try {
        $lineNumber = 0
        foreach ($line in Get-Content -LiteralPath $absolutePath -Encoding utf8) {
            $lineNumber++
            foreach ($rule in $rules.GetEnumerator()) {
                if ($line -match $rule.Value) {
                    $contentHits.Add("${relativePath}:${lineNumber} [$($rule.Key)]")
                }
            }
        }
    } catch {
        # Binary or undecodable files are covered by filename policy.
    }
}

if ($nameHits.Count -gt 0 -or $contentHits.Count -gt 0) {
    Write-Host "Secret scan failed. Only paths and rule names are printed." -ForegroundColor Red
    $nameHits | ForEach-Object { Write-Host "$_ [sensitive-filename]" }
    $contentHits | ForEach-Object { Write-Host $_ }
    exit 1
}

Write-Host "Secret scan passed: $($trackedFiles.Count) tracked files, 0 findings."
