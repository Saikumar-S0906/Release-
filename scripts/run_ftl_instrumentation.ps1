# Run instrumentation suite on Firebase Test Lab device matrix.
param(
    [string]$ProjectId = "",
    [ValidateSet("pr", "nightly")]
    [string]$Matrix = "pr",
    [string]$TestClass = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Write-Host "Building app + androidTest APKs..."
.\gradlew assembleDebug assembleDebugAndroidTest --console=plain | Out-Null

$app = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
$test = Join-Path $root "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
if (-not (Test-Path $app) -or -not (Test-Path $test)) { throw "APKs missing" }
if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
    throw "gcloud CLI not found"
}

$devices = switch ($Matrix) {
    "pr" {
        @("model=MediumPhone.arm,version=34,locale=en_US,orientation=portrait")
    }
    "nightly" {
        @(
            "model=MediumPhone.arm,version=34,locale=en_US,orientation=portrait",
            "model=MediumPhone.arm,version=33,locale=en_US,orientation=portrait",
            "model=MediumTablet.arm,version=34,locale=en_US,orientation=portrait"
        )
    }
}

$args = @(
    "firebase", "test", "android", "run",
    "--type", "instrumentation",
    "--app", $app,
    "--test", $test,
    "--timeout", "15m",
    "--results-dir", "pixijoy-instr-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
)
if ($ProjectId) { $args += @("--project", $ProjectId) }
foreach ($d in $devices) { $args += @("--device", $d) }
if ($TestClass) {
    $args += @("--test-targets", "class $TestClass")
}

Write-Host "Launching instrumentation on Test Lab ($Matrix)..."
& gcloud @args
exit $LASTEXITCODE
