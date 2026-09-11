# Parses Android test XML results and produces a failure analysis report.
param(
    [string]$ResultsDir = "",
    [string]$OutputFile = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
if (-not $ResultsDir) {
    $ResultsDir = Join-Path $root "app\build\outputs\androidTest-results\connected\debug"
}
if (-not $OutputFile) {
    $OutputFile = Join-Path $root "app\build\reports\qa-failure-analysis.txt"
}

New-Item -ItemType Directory -Force -Path (Split-Path $OutputFile) | Out-Null

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("=== PIXIJOY QA FAILURE ANALYSIS ===")
$lines.Add("Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
$lines.Add("Results dir: $ResultsDir")
$lines.Add("")

if (-not (Test-Path $ResultsDir)) {
    $lines.Add("No instrumented test results found. Run .\scripts\run_qa_tests.ps1 first.")
    $lines | Set-Content $OutputFile -Encoding UTF8
    Write-Host "No results at $ResultsDir"
    exit 1
}

$xmlFiles = Get-ChildItem -Path $ResultsDir -Filter "TEST-*.xml" -Recurse -ErrorAction SilentlyContinue
if (-not $xmlFiles) {
    $lines.Add("No TEST-*.xml files found under $ResultsDir")
    $lines | Set-Content $OutputFile -Encoding UTF8
    exit 1
}

$failures = @()
$passes = 0
$total = 0

foreach ($file in $xmlFiles) {
    [xml]$doc = Get-Content $file.FullName
    $suite = $doc.testsuite
    if (-not $suite) { continue }
    foreach ($case in $suite.testcase) {
        $total++
        if ($case.failure -or $case.error) {
            $failures += [pscustomobject]@{
                Class = $case.classname
                Method = $case.name
                Message = if ($case.failure) { $case.failure.message } else { $case.error.message }
                Type = if ($case.failure) { $case.failure.type } else { $case.error.type }
                File = $file.FullName
            }
        } else {
            $passes++
        }
    }
}

$lines.Add("Summary: $passes passed, $($failures.Count) failed, $total total")
$lines.Add("")

if ($failures.Count -eq 0) {
    $lines.Add("All instrumented tests passed.")
} else {
    $lines.Add("=== FAILURES (action items) ===")
    $i = 1
    foreach ($f in $failures) {
        $lines.Add("")
        $lines.Add("$i. $($f.Class)#$($f.Method)")
        $lines.Add("   Type: $($f.Type)")
        $msg = ($f.Message -replace "`r?`n", " ").Trim()
        if ($msg.Length -gt 500) { $msg = $msg.Substring(0, 500) + "..." }
        $lines.Add("   Message: $msg")
        $lines.Add("   Retest: .\gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=$($f.Class)")
        $i++
    }
}

$lines.Add("")
$lines.Add("=== MATRIX GAPS ===")
$matrixPath = Join-Path $root "app\src\main\java\com\pixijoy\ai\testing\QaTestMatrix.kt"
if (Test-Path $matrixPath) {
    $lines.Add("Review QaTestMatrix.kt for cases marked NOT_RUN in on-device qa_test_results/latest_run.txt")
}

$lines | Set-Content $OutputFile -Encoding UTF8
Write-Host "Failure analysis written to: $OutputFile"
Get-Content $OutputFile
