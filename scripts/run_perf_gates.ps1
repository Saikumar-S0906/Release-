<#
.SYNOPSIS
  Run PixiJoy performance / leak gates on an attached device.

.EXAMPLE
  .\scripts\run_perf_gates.ps1
  .\scripts\run_perf_gates.ps1 -Device emulator-5556 -SkipBenchmark
#>
param(
    [string]$Device = "emulator-5554",
    [switch]$SkipBenchmark,
    [switch]$SkipStress,
    [switch]$SkipMaestro,
    [string]$MaestroBin = $(if ($env:MAESTRO_BIN) { $env:MAESTRO_BIN } elseif (Test-Path "$env:LOCALAPPDATA\maestro\maestro\bin\maestro.bat") { "$env:LOCALAPPDATA\maestro\maestro\bin\maestro.bat" } else { "maestro" })
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$outDir = Join-Path $root "app\build\reports\perf_gates"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$log = Join-Path $outDir "perf_$stamp.txt"

function Write-Log([string]$msg) {
    $line = "$(Get-Date -Format o)  $msg"
    Write-Host $line
    Add-Content -Path $log -Value $line
}

function Capture-Meminfo([string]$label) {
    $file = Join-Path $outDir "meminfo_${label}_$stamp.txt"
    adb -s $Device shell dumpsys meminfo com.pixijoy.studio > $file
    $summary = Select-String -Path $file -Pattern "TOTAL PSS:|TOTAL RSS:|Java Heap:|Native Heap:|Graphics:" |
        ForEach-Object { $_.Line.Trim() }
    Write-Log "meminfo[$label]: $($summary -join ' | ')"
    return $file
}

Write-Log "Device=$Device"
adb -s $Device wait-for-device
adb -s $Device shell setprop debug.pixijoy.e2e 1 | Out-Null

Write-Log "Assembling debug + installing..."
& .\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --quiet
if ($LASTEXITCODE -ne 0) { throw "assembleDebug failed ($LASTEXITCODE)" }
adb -s $Device install -r "app\build\outputs\apk\debug\app-debug.apk"
adb -s $Device install -r "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"

Capture-Meminfo "before" | Out-Null

$stressCode = 0
if (-not $SkipStress) {
    Write-Log "Running StudioSessionStressTest on $Device..."
    & .\gradlew.bat :app:connectedDebugAndroidTest `
        "-Pandroid.testInstrumentationRunnerArguments.class=com.pixijoy.studio.StudioSessionStressTest" `
        "-Pandroid.injected.androidTest.connectedDeviceSerial=$Device"
    $stressCode = $LASTEXITCODE
    Write-Log "Stress exit=$stressCode"
}

Capture-Meminfo "after" | Out-Null

# LeakCanary toast / analysis lines (best-effort)
$leakLog = Join-Path $outDir "leakcanary_$stamp.txt"
adb -s $Device logcat -d -s LeakCanary:I LeakCanary:D > $leakLog
$leakHits = Select-String -Path $leakLog -Pattern "LEAK|LeakCanary|Retaining" -SimpleMatch:$false |
    Select-Object -First 40
Write-Log "LeakCanary log hits: $($leakHits.Count)"
$leakHits | ForEach-Object { Write-Log "  $($_.Line)" }

$benchCode = 0
if (-not $SkipBenchmark) {
    Write-Log "Running Macrobenchmark StartupBenchmark..."
    & .\gradlew.bat :benchmark:connectedBenchmarkAndroidTest `
        "-Pandroid.injected.androidTest.connectedDeviceSerial=$Device"
    $benchCode = $LASTEXITCODE
    Write-Log "Benchmark exit=$benchCode"
    $benchOut = Get-ChildItem -Path "benchmark\build\outputs" -Recurse -Filter "*.json" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 3
    foreach ($f in $benchOut) {
        Write-Log "Benchmark artifact: $($f.FullName)"
        Get-Content $f.FullName -TotalCount 40 | ForEach-Object { Write-Log "  $_" }
    }
}

Write-Log "DONE stress=$stressCode bench=$benchCode log=$log"
if ($stressCode -ne 0 -or $benchCode -ne 0) {
    exit 1
}
exit 0
