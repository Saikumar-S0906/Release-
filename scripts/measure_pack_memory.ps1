<#
.SYNOPSIS
  Download the E4 frame-interp pack via Maestro, then report on-disk size and
  process PSS (memory). Neural engine stays unbound — this measures the Ready file.

.EXAMPLE
  .\scripts\measure_pack_memory.ps1 -Device emulator-5556
#>
param(
    [string]$Device = "emulator-5556",
    [string]$MaestroBin = $(if ($env:MAESTRO_BIN) { $env:MAESTRO_BIN } else {
        if (Test-Path "C:\Users\andro\tools\maestro\bin\maestro.bat") {
            "C:\Users\andro\tools\maestro\bin\maestro.bat"
        } else { "maestro" }
    }),
    [string]$Apk = "app\build\outputs\apk\debug\app-debug.apk",
    [string]$Package = "com.pixijoy.studio",
    [switch]$SkipInstall,
    [switch]$SkipMaestro
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Get-AppPssKb {
    param([string]$Serial, [string]$Pkg)
    $dump = adb -s $Serial shell dumpsys meminfo $Pkg 2>$null
    if (-not $dump) { return $null }
    $line = $dump | Select-String -Pattern "TOTAL\s+PSS:\s*(\d+)" | Select-Object -First 1
    if ($line) { return [int]$line.Matches[0].Groups[1].Value }
    $alt = $dump | Select-String -Pattern "TOTAL:\s+(\d+)" | Select-Object -First 1
    if ($alt) { return [int]$alt.Matches[0].Groups[1].Value }
    return $null
}

function Get-PackFileBytes {
    param([string]$Serial, [string]$Pkg, [string]$RelPath)
    $path = "/data/data/$Pkg/files/$RelPath"
    $raw = adb -s $Serial shell run-as $Pkg ls -l $path 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $raw) {
        $raw = adb -s $Serial shell "run-as $Pkg ls -l files/$RelPath" 2>$null
    }
    if (-not $raw) { return $null }
    $parts = ($raw -split "\s+") | Where-Object { $_ }
    # Android ls -l: permissions links owner group size ...
    foreach ($p in $parts) {
        if ($p -match '^\d+$' -and [long]$p -gt 1000) { return [long]$p }
    }
    return $null
}

Write-Host "Device: $Device"
adb -s $Device wait-for-device
adb -s $Device shell setprop debug.pixijoy.e2e 1 | Out-Null

if (-not $SkipInstall) {
    if (-not (Test-Path $Apk)) {
        Write-Host "Assembling debug APK..."
        .\gradlew.bat :app:assembleDebug --quiet
    }
    Write-Host "Installing $Apk ..."
    adb -s $Device install -r $Apk
}

$before = Get-AppPssKb -Serial $Device -Pkg $Package
Write-Host ("PSS before pack download: {0} KB ({1:N1} MB)" -f $(if ($null -eq $before) { '?' } else { $before }), $(if ($null -eq $before) { 0 } else { $before / 1024.0 }))

if (-not $SkipMaestro) {
    $flow = Join-Path $root ".maestro\packs\e4-frame-interp-download.yaml"
    Write-Host "Maestro: $flow (FILM ~67 MB — can take several minutes)"
    & $MaestroBin --device $Device test $flow
    if ($LASTEXITCODE -ne 0) {
        throw "Maestro E4 download flow failed (exit $LASTEXITCODE)."
    }
}

Start-Sleep -Seconds 2
$after = Get-AppPssKb -Serial $Device -Pkg $Package
$bytes = Get-PackFileBytes -Serial $Device -Pkg $Package -RelPath "ml_packs/frameinterp/1/interp.tflite"

Write-Host ""
Write-Host "=== E4 pack memory / disk report ==="
Write-Host ("PSS after:  {0} KB ({1:N1} MB)" -f $(if ($null -eq $after) { '?' } else { $after }), $(if ($null -eq $after) { 0 } else { $after / 1024.0 }))
if ($null -ne $before -and $null -ne $after) {
    $delta = $after - $before
    Write-Host ("PSS delta:  {0:+#;-#;0} KB ({1:N1} MB)" -f $delta, ($delta / 1024.0))
}
if ($null -ne $bytes) {
    Write-Host ("On-disk interp.tflite: {0:N0} bytes ({1:N2} MB)" -f $bytes, ($bytes / 1MB))
} else {
    Write-Host "On-disk interp.tflite: not found via run-as (check PackStore path / debug build)."
}
Write-Host "Note: PackLiteRt does not bind FrameInterp yet — Ready file != neural / engineReady."
Write-Host "F1 Track still has no HTTPS pin (no public LiteRT tracker TFLite)."
#>
