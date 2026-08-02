Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Security

function Get-SHA256([string]$Path) {
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    $stream = [System.IO.File]::OpenRead($Path)
    $hash = $sha256.ComputeHash($stream)
    $stream.Close()
    $sha256.Dispose()
    return [BitConverter]::ToString($hash).Replace("-", "").ToLowerInvariant()
}

function Compare-Images {
    param (
        [string]$Img1Path,
        [string]$Img2Path,
        [string]$OutDiffPath,
        [string]$OutCompositePath
    )

    $img1 = [System.Drawing.Image]::FromFile($Img1Path)
    $img2 = [System.Drawing.Image]::FromFile($Img2Path)

    $w = $img1.Width
    $h = $img1.Height

    if ($w -ne $img2.Width -or $h -ne $img2.Height) {
        Write-Warning "Dimension mismatch!"
        $img1.Dispose(); $img2.Dispose()
        return
    }

    $bmp1 = New-Object System.Drawing.Bitmap($img1)
    $bmp2 = New-Object System.Drawing.Bitmap($img2)
    $diffBmp = New-Object System.Drawing.Bitmap($w, $h)

    $totalAbsDiff = 0
    $mismatchPixels = 0
    $totalPixels = $w * $h

    $mean1 = 0

    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            $p1 = $bmp1.GetPixel($x, $y)
            $p2 = $bmp2.GetPixel($x, $y)

            $mean1 += ($p1.R + $p1.G + $p1.B) / 3

            $dr = [Math]::Abs($p1.R - $p2.R)
            $dg = [Math]::Abs($p1.G - $p2.G)
            $db = [Math]::Abs($p1.B - $p2.B)

            $totalAbsDiff += $dr + $dg + $db

            if ($dr -gt 10 -or $dg -gt 10 -or $db -gt 10) {
                $mismatchPixels++
                $diffBmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 255, 0, 0)) # Red for mismatch
            } else {
                $diffBmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $p1.R, $p1.G, $p1.B)) # Base pixel for context
            }
        }
    }

    $mean1 = $mean1 / $totalPixels
    if ($mean1 -lt 5) {
        Write-Warning "Image 1 is nearly blank (mean=$mean1)"
    }

    $mismatchPct = [Math]::Round(($mismatchPixels / $totalPixels) * 100, 2)
    $mae = [Math]::Round($totalAbsDiff / ($totalPixels * 3), 2)

    $diffBmp.Save($OutDiffPath, [System.Drawing.Imaging.ImageFormat]::Png)

    # Create composite (Img1 | Img2 | Diff)
    $compW = $w * 3
    $compBmp = New-Object System.Drawing.Bitmap($compW, $h)
    $g = [System.Drawing.Graphics]::FromImage($compBmp)

    $g.DrawImage($img1, 0, 0, $w, $h)
    $g.DrawImage($img2, $w, 0, $w, $h)
    $g.DrawImage($diffBmp, ($w * 2), 0, $w, $h)

    $compBmp.Save($OutCompositePath, [System.Drawing.Imaging.ImageFormat]::Png)

    $g.Dispose()
    $compBmp.Dispose()
    $diffBmp.Dispose()
    $bmp1.Dispose()
    $bmp2.Dispose()
    $img1.Dispose()
    $img2.Dispose()

    $hash1 = Get-SHA256 $Img1Path
    $hash2 = Get-SHA256 $Img2Path

    $report = @{
        mismatch_pct = $mismatchPct
        mae = $mae
        flutter_sha256 = $hash1
        native_sha256 = $hash2
        captured_at_utc = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    }

    return $report
}
