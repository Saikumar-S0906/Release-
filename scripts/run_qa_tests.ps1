# Runs unit + instrumented tests, analyzes failures, retries failed suites once.
param(
    [switch]$UnitOnly,
    [switch]$SkipInstall,
    [switch]$SkipRetry,
    [string]$TestClass = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$summary = Join-Path $root "app\build\reports\qa-summary.txt"
New-Item -ItemType Directory -Force -Path (Split-Path $summary) | Out-Null

function Write-Section($title) {
    Add-Content $summary "`n=== $title ==="
}

"" | Set-Content $summary
Add-Content $summary "PixiJoy QA Test Run - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"

Write-Host "Running unit tests..."
.\gradlew testDebugUnitTest --console=plain 2>&1 | Tee-Object -Variable unitOut
$unitOk = $LASTEXITCODE -eq 0
Write-Section "UNIT TESTS"
Add-Content $summary $(if ($unitOk) { "PASSED" } else { "FAILED" })
Add-Content $summary "Report: app\build\reports\tests\testDebugUnitTest\index.html"

$instOk = $true
if (-not $UnitOnly) {
    if (-not $SkipInstall) {
        Write-Host "Building debug APK + test APK..."
        .\gradlew assembleDebugAndroidTest assembleDebug --console=plain | Out-Null
    }

    if (Test-Path $adb) {
        $devices = & $adb devices | Select-String "device$" | Where-Object { $_ -notmatch "List of devices" }
        if (-not $devices) {
            Write-Warning "No device/emulator connected. Skipping instrumented tests."
            Write-Section "INSTRUMENTED TESTS"
            Add-Content $summary "SKIPPED - no device connected"
            $instOk = $false
        } else {
            $gradleArgs = @("connectedDebugAndroidTest", "--console=plain")
            if ($TestClass) {
                $gradleArgs += "-Pandroid.testInstrumentationRunnerArguments.class=$TestClass"
            }
            Write-Host "Running instrumented tests on device..."
            & .\gradlew @gradleArgs 2>&1 | Tee-Object -Variable instOut
            $instOk = $LASTEXITCODE -eq 0
            Write-Section "INSTRUMENTED TESTS"
            Add-Content $summary $(if ($instOk) { "PASSED" } else { "FAILED" })
            Add-Content $summary "Report: app\build\reports\androidTests\connected\index.html"

            if (-not $instOk -and -not $SkipRetry) {
                Write-Host "Analyzing failures..."
                & "$PSScriptRoot\analyze_qa_failures.ps1"
                Write-Host "Retrying instrumented tests once..."
                & .\gradlew connectedDebugAndroidTest --console=plain 2>&1 | Tee-Object -Variable retryOut
                $retryOk = $LASTEXITCODE -eq 0
                Write-Section "INSTRUMENTED RETRY"
                Add-Content $summary $(if ($retryOk) { "PASSED on retry" } else { "FAILED on retry" })
                $instOk = $retryOk
                if (-not $retryOk) {
                    & "$PSScriptRoot\analyze_qa_failures.ps1"
                }
            } elseif ($instOk) {
                & "$PSScriptRoot\analyze_qa_failures.ps1" | Out-Null
            }
        }
    } else {
        Write-Warning "adb not found. Skipping instrumented tests."
        $instOk = $false
    }
}

Write-Section "OVERALL"
$overall = $unitOk -and $instOk
Add-Content $summary $(if ($overall) { "PASSED" } else { "FAILED" })

Write-Host ""
Write-Host "QA summary: $summary"
if (Test-Path $summary) { Get-Content $summary }

if (-not $overall) { exit 1 }
