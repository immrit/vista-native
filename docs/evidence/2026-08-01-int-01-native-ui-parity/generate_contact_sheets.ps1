Add-Type -AssemblyName System.Drawing

$Dirs = @("login", "feed", "post-detail", "own-profile", "search", "other-profile")
$BaseDir = "E:\vista_native_integration\docs\evidence\2026-08-01-int-01-native-ui-parity"

foreach ($Dir in $Dirs) {
    $CompDir = "$BaseDir\comparison\$Dir"
    $Files = Get-ChildItem -Path $CompDir -Filter "*_comparison.png"
    if ($Files.Count -eq 0) { continue }
    
    $Images = @()
    $TotalHeight = 0
    $MaxWidth = 0
    
    foreach ($File in $Files) {
        $Img = [System.Drawing.Image]::FromFile($File.FullName)
        $Images += $Img
        $TotalHeight += $Img.Height
        if ($Img.Width -gt $MaxWidth) { $MaxWidth = $Img.Width }
    }
    
    $SheetBmp = New-Object System.Drawing.Bitmap($MaxWidth, $TotalHeight)
    $g = [System.Drawing.Graphics]::FromImage($SheetBmp)
    
    $CurrentY = 0
    foreach ($Img in $Images) {
        $g.DrawImage($Img, 0, $CurrentY, $Img.Width, $Img.Height)
        $CurrentY += $Img.Height
        $Img.Dispose()
    }
    
    $OutSheet = "$BaseDir\${Dir}-contact-sheet.png"
    $SheetBmp.Save($OutSheet, [System.Drawing.Imaging.ImageFormat]::Png)
    
    $g.Dispose()
    $SheetBmp.Dispose()
    Write-Host "Created contact sheet: $OutSheet"
}
