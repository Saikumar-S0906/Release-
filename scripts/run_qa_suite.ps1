# Run PixiJoy QA by suite name (unit + optional instrumented class filter).
param(
    [ValidateSet("all", "unit", "ui", "e2e", "regression", "negative", "edge", "stability", "monkey", "board")]
    [string]$Suite = "all",
    [switch]$SkipRetry
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    $env:Path = "$env:Path;$(Split-Path $adb)"
}

# Map suite -> fully-qualified instrumented test class(es)
$suiteClasses = @{
    ui         = @(
        "com.pixijoy.studio.PixiJoyNavigationSmokeTest",
        "com.pixijoy.studio.PixiJoyScreenReachabilityTest",
        "com.pixijoy.studio.qa.MemeStickerFlowTest"
    )
    e2e        = @(
        "com.pixijoy.studio.qa.ColdStartE2ETest",
        "com.pixijoy.studio.qa.MemeStickerFlowTest",
        "com.pixijoy.studio.PixiJoyNavigationSmokeTest"
    )
    regression = @(
        "com.pixijoy.studio.qa.RegressionSuiteTest",
        "com.pixijoy.studio.LocaleResourcesTest",
        "com.pixijoy.studio.QaReportManagerTest",
        "com.pixijoy.studio.qa.CreditEdgeCaseTest"
    )
    negative   = @("com.pixijoy.studio.qa.NegativeCaseTest")
    edge       = @(
        "com.pixijoy.studio.qa.EdgeCaseTest",
        "com.pixijoy.studio.qa.CreditEdgeCaseTest",
        "com.pixijoy.studio.LocaleResourcesTest"
    )
    stability  = @(
        "com.pixijoy.studio.qa.MonkeyStabilityTest",
        "com.pixijoy.studio.qa.UiAutomatorLifecycleTest"
    )
    monkey     = @("com.pixijoy.studio.qa.MonkeyStabilityTest")
}

Write-Host "=== PixiJoy QA Suite: $Suite ==="

if ($Suite -eq "unit") {
    & "$PSScriptRoot\run_qa_tests.ps1" -UnitOnly
    exit $LASTEXITCODE
}

if ($Suite -eq "board") {
    & "$PSScriptRoot\run_board_qa_depth.ps1" -SkipRetry:$SkipRetry
    exit $LASTEXITCODE
}

if ($Suite -eq "all") {
    & "$PSScriptRoot\run_qa_tests.ps1" -SkipRetry:$SkipRetry
    exit $LASTEXITCODE
}

# Suite-specific: always run unit first, then filtered instrumented classes
Write-Host "Running unit tests..."
.\gradlew testDebugUnitTest --console=plain
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$classes = $suiteClasses[$Suite]
if (-not $classes) {
    Write-Error "No classes mapped for suite $Suite"
    exit 1
}

# Android instrumentation runner accepts one class at a time for reliability
$failed = @()
foreach ($cls in $classes) {
    Write-Host "`n>>> Instrumented: $cls"
    .\gradlew connectedDebugAndroidTest --console=plain "-Pandroid.testInstrumentationRunnerArguments.class=$cls"
    if ($LASTEXITCODE -ne 0) {
        $failed += $cls
        if (-not $SkipRetry) {
            Write-Host "Retrying $cls ..."
            .\gradlew connectedDebugAndroidTest --console=plain "-Pandroid.testInstrumentationRunnerArguments.class=$cls"
            if ($LASTEXITCODE -eq 0) {
                $failed = $failed | Where-Object { $_ -ne $cls }
            }
        }
    }
}

& "$PSScriptRoot\analyze_qa_failures.ps1"

if ($failed.Count -gt 0) {
    Write-Host "`nFAILED classes:"
    $failed | ForEach-Object { Write-Host " - $_" }
    exit 1
}

Write-Host "`nSuite $Suite PASSED"
exit 0
