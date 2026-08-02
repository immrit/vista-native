. E:\vista_native_integration\docs\evidence\2026-08-01-int-01-native-ui-parity\compare_script.ps1

$res = Compare-Images -Img1Path 'E:\vista_native_integration\docs\evidence\2026-08-01-int-01-native-ui-parity\flutter\login_initial.png' -Img2Path 'E:\vista_native_integration\docs\evidence\2026-08-01-int-01-native-ui-parity\native\login_initial.png' -OutDiffPath 'E:\vista_native_integration\docs\evidence\2026-08-01-int-01-native-ui-parity\comparison\login_initial_diff.png' -OutCompositePath 'E:\vista_native_integration\docs\evidence\2026-08-01-int-01-native-ui-parity\comparison\login_initial_comparison.png'

$jsonObj = @{
    state = 'login_initial'
    mismatch_pct = $res.mismatch_pct
    mae = $res.mae
    flutter_sha256 = $res.flutter_sha256
    native_sha256 = $res.native_sha256
    captured_at_utc = $res.captured_at_utc
} 

$json = $jsonObj | ConvertTo-Json
Set-Content -Path 'E:\vista_native_integration\docs\evidence\2026-08-01-int-01-native-ui-parity\reports\login_initial_metrics.json' -Value $json
Write-Host $json
