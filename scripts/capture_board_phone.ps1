# Capture Scope-A board entry screenshots on a connected phone/emulator.
# Usage:
#   .\scripts\capture_board_phone.ps1
#   .\scripts\capture_board_phone.ps1 -GoldenOnly   # rematch / day-in-life keys only

param(
    [switch]$GoldenOnly
)

$ErrorActionPreference = "Continue"
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$adb = Join-Path $sdk "platform-tools\adb.exe"
$root = Split-Path -Parent $PSScriptRoot
$outDir = if ($GoldenOnly) {
    Join-Path $root "docs\board_review\golden"
} else {
    Join-Path $root "docs\board_review\phone"
}
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$idsFull = @(
    "b1_splash", "b1_welcome", "b1_ready",
    "b2_home", "b2_home_scroll", "b2_discover", "b2_discover_cat",
    "b2_template", "b2_template_create", "b2_template_result",
    "b2_search", "b2_trending", "b2_collections",
    "b_c00_empty",
    "b3_chooser", "b4_result",
    "b_create_hub",
    "b5_choose", "b6_choose", "b7_sheet", "b_diff_choose",
    "b8_ready", "b9_open", "b10_discover", "b11_home",
    "b12_intro",
    "b13_photo", "b14_input", "b15_open", "b16_select", "b17_open",
    "b18_browse", "b19_start", "b26_open",
    "b20_input", "b21_hub", "b22_welcome", "b23_dash", "b24_plans", "b25_center",
    "b27_feed", "b28_banner", "b29_home", "b30_home",
    "b_c04", "b_c05", "b_c06", "b_c07", "b_c08", "b_c09", "b_c10", "b_c11", "b_c12", "b_c14",
    "b_collab_select"
)

# Align with BoardCatalogBatches.goldenKeyScreenIds (+ result for rematch review).
$idsGolden = @(
    "b2_home",
    "b_create_hub",
    "b5_choose",
    "b6_choose",
    "b7_sheet",
    "b_diff_choose",
    "b5_result",
    "b8_ready",
    "b8_share",
    "b18_browse",
    "b13_photo",
    "b14_input",
    "b_c04",
    "b_c07"
)

$ids = if ($GoldenOnly) { $idsGolden } else { $idsFull }

function Capture-Screen([string]$dest) {
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $adb
    $psi.Arguments = "exec-out screencap -p"
    $psi.RedirectStandardOutput = $true
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $p = [System.Diagnostics.Process]::Start($psi)
    $ms = New-Object System.IO.MemoryStream
    $p.StandardOutput.BaseStream.CopyTo($ms)
    $p.WaitForExit()
    [System.IO.File]::WriteAllBytes($dest, $ms.ToArray())
}

function Get-UiXml {
    $null = & $adb shell uiautomator dump /sdcard/u.xml 2>$null
    return (& $adb shell cat /sdcard/u.xml 2>$null)
}

Write-Host ("Capturing {0} screens -> {1}" -f $ids.Count, $outDir)
& $adb wait-for-device | Out-Null

foreach ($id in $ids) {
    Write-Host ("  {0}" -f $id)
    & $adb shell am force-stop com.pixijoy.ai 2>$null | Out-Null
    Start-Sleep -Milliseconds 600
    & $adb shell am start -W -n com.pixijoy.ai/.MainActivity --es board_screen $id 2>$null | Out-Null
    # Settle compose + seed (DataStore prepareBoardReviewCapture needs ≥10–12s cold)
    Start-Sleep -Seconds 12
    # Dismiss consent/privacy if present
    for ($i = 0; $i -lt 4; $i++) {
        $xml = Get-UiXml
        if ($xml -match 'I Agree') {
            & $adb shell input tap 1075 1855 2>$null | Out-Null
            Start-Sleep -Seconds 1
        } elseif ($xml -match 'Privacy' -and $xml -match 'Accept') {
            & $adb shell input tap 720 2100 2>$null | Out-Null
            Start-Sleep -Seconds 1
        } else {
            $i = 99
        }
    }
    Start-Sleep -Seconds 2
    $dest = Join-Path $outDir ("{0}.png" -f $id)
    Capture-Screen $dest
    Write-Host ("    bytes={0}" -f (Get-Item $dest).Length)
}

Write-Host ("Done. Screenshots in {0}" -f $outDir)
