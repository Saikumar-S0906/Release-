<#
.SYNOPSIS
  Run PixiJoy Maestro E2E suites and write reports.

.EXAMPLE
  .\scripts\run_maestro.ps1 -Device emulator-5554
  .\scripts\run_maestro.ps1 -Suite positive
  .\scripts\run_maestro.ps1 -Flow maestro\positive\01_cold_start_privacy_to_home.yaml
#>
param(
    [string]$Device = "emulator-5556",
    [ValidateSet("all", "positive", "negative", "edge", "monkey")]
    [string]$Suite = "all",
    [string]$Flow = "",
    [string]$MaestroBin = "C:\Users\andro\tools\maestro\bin\maestro.bat",
    [string]$Apk = "app\build\outputs\apk\debug\app-debug.apk",
    [switch]$SkipInstall
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Test-Path $MaestroBin)) {
    throw "Maestro not found at $MaestroBin. Install CLI 2.x first."
}

$env:MAESTRO_CLI_NO_ANALYTICS = "true"
$env:MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED = "true"
$env:ANDROID_SERIAL = $Device

$reports = Join-Path $root "maestro\reports"
New-Item -ItemType Directory -Force -Path $reports | Out-Null
$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$outDir = Join-Path $reports $stamp
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

Write-Host "Device: $Device"
adb -s $Device wait-for-device
adb -s $Device devices

# Skip AdMob/Billing under Maestro to prevent cold-start ANR
adb -s $Device shell setprop debug.pixijoy.e2e 1
Write-Host "setprop debug.pixijoy.e2e=1 (E2E light mode)"

if (-not $SkipInstall) {
    if (-not (Test-Path $Apk)) {
        Write-Host "APK missing — assembling debug..."
        .\gradlew.bat :app:assembleDebug --quiet
    }
    Write-Host "Installing $Apk ..."
    adb -s $Device install -r $Apk
}

$target = if ($Flow) {
    Join-Path $root $Flow
} elseif ($Suite -eq "all") {
    @(
        (Join-Path $root "maestro\positive"),
        (Join-Path $root "maestro\negative"),
        (Join-Path $root "maestro\edge"),
        (Join-Path $root "maestro\monkey")
    )
} else {
    Join-Path $root "maestro\$Suite"
}

$junit = Join-Path $outDir "junit.xml"
$html = Join-Path $outDir "report.html"

Write-Host "Running Maestro → $outDir"
& $MaestroBin --device $Device test `
    --format junit `
    --output $junit `
    --debug-output $outDir `
    $target

$code = $LASTEXITCODE
Write-Host "Maestro exit: $code"
Write-Host "JUnit: $junit"
Write-Host "Debug: $outDir"
exit $code
