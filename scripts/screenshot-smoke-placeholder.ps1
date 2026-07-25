[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$evidenceRoot = Join-Path $repositoryRoot "docs/audits/evidence/2026-07-25-aud-01-gate-closure"
$requiredEvidence = @(
    "native-auth-api33.png",
    "native-auth-dark-font130-api33.png",
    "native-clean-onboarding-api33.png"
)

foreach ($fileName in $requiredEvidence) {
    $path = Join-Path $evidenceRoot $fileName
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Missing screenshot smoke baseline: $path"
    }
    if ((Get-Item -LiteralPath $path).Length -eq 0) {
        throw "Empty screenshot smoke baseline: $path"
    }
}

Write-Host "Screenshot smoke placeholder passed: $($requiredEvidence.Count) baseline images are present."
Write-Host "This gate validates baseline availability; it is not fresh emulator/device evidence."
