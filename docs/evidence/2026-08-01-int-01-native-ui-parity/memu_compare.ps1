Add-Type -AssemblyName System.Drawing

$Dirs = @("feed", "post-detail", "own-profile", "search", "other-profile")
$BaseDir = "E:\vista_native_integration\docs\evidence\2026-08-01-int-01-native-ui-parity"

$TotalPairs = 0
$MissingPairs = 0
$Report = @()

foreach ($Dir in $Dirs) {
    $NativeDir = "$BaseDir\native\$Dir"
    $FlutterDir = "$BaseDir\flutter\$Dir"
    $CompDir = "$BaseDir\comparison\$Dir"
    if (-not (Test-Path $CompDir)) { New-Item -ItemType Directory -Force $CompDir | Out-Null }
    
    if (Test-Path $NativeDir) {
        $Files = Get-ChildItem -Path $NativeDir -Filter "*.png"
        foreach ($File in $Files) {
            $State = $File.BaseName
            $NativeImgPath = $File.FullName
            $FlutterImgPath = "$FlutterDir\$State.png"
            
            $TotalPairs++
            if (-not (Test-Path $FlutterImgPath)) {
                $MissingPairs++
                Write-Host "Missing flutter image: $FlutterImgPath"
                continue
            }
            
            $OutComp = "$CompDir\${State}_comparison.png"
            
            $img1 = [System.Drawing.Image]::FromFile($FlutterImgPath)
            $img2 = [System.Drawing.Image]::FromFile($NativeImgPath)
            [int]$w = $img1.Width
            [int]$h = $img1.Height
            
            $compBmp = New-Object System.Drawing.Bitmap($w * 2, $h)
            $g = [System.Drawing.Graphics]::FromImage($compBmp)
            $g.DrawImage($img1, 0, 0, $w, $h)
            $g.DrawImage($img2, $w, 0, $w, $h)
            $compBmp.Save($OutComp, [System.Drawing.Imaging.ImageFormat]::Png)
            
            $g.Dispose()
            $compBmp.Dispose()
            $img1.Dispose()
            $img2.Dispose()
            
            $Report += [PSCustomObject]@{
                Dir = $Dir
                State = $State
                Matched = $true
            }
        }
    }
}

$FinalReport = @{
    TotalPairs = $TotalPairs
    MissingEvidence = $MissingPairs
    Pairs = $Report
}
$FinalReport | ConvertTo-Json | Out-File "$BaseDir\reports\final-parity-report.json" -Encoding utf8
Write-Host "Done comparing."
