# Run Firebase Test Lab Robo exploration (automatic UI crawl + crash capture).
# Prerequisites: gcloud CLI, Firebase project, billing enabled.
param(
    [string]$ProjectId = "",
    [string]$Apk = "",
    [ValidateSet("pr", "nightly")]
    [string]$Matrix = "pr",
    [int]$TimeoutSec = 300
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not $Apk) {
    Write-Host "Building debug APK..."
    .\gradlew assembleDebug --console=plain | Out-Null
    $Apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
}

if (-not (Test-Path $Apk)) { throw "APK not found: $Apk" }
if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
    throw "gcloud CLI not found. Install Google Cloud SDK and run: gcloud auth login"
}

$devices = switch ($Matrix) {
    "pr" {
        @(
            "model=MediumPhone.arm,version=34,locale=en_US,orientation=portrait"
        )
    }
    "nightly" {
        @(
            "model=MediumPhone.arm,version=34,locale=en_US,orientation=portrait",
            "model=MediumPhone.arm,version=34,locale=hi_IN,orientation=portrait",
            "model=MediumPhone.arm,version=34,locale=ar,orientation=portrait",
            "model=MediumTablet.arm,version=34,locale=en_US,orientation=landscape"
        )
    }
}

$args = @(
    "firebase", "test", "android", "run",
    "--type", "robo",
    "--app", $Apk,
    "--timeout", "${TimeoutSec}s",
    "--results-dir", "pixijoy-robo-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
)
if ($ProjectId) { $args += @("--project", $ProjectId) }
foreach ($d in $devices) { $args += @("--device", $d) }

Write-Host "Launching Robo on Test Lab ($Matrix)..."
& gcloud @args
exit $LASTEXITCODE
