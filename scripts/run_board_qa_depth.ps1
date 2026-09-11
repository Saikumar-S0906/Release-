# Batched board Compose QA + day-in-life + golden-key smoke.
# Avoids melting one AVD with the full BoardCatalogComposeTest matrix.
#
# Usage:
#   .\scripts\run_board_qa_depth.ps1                  # unit + all batches + day + golden
#   .\scripts\run_board_qa_depth.ps1 -Batch create    # one batch only
#   .\scripts\run_board_qa_depth.ps1 -UnitOnly
#   .\scripts\run_board_qa_depth.ps1 -SkipInstrumented

param(
    [ValidateSet("all", "core", "create", "ctools", "media", "library", "me", "social")]
    [string]$Batch = "all",
    [switch]$UnitOnly,
    [switch]$SkipInstrumented,
    [switch]$SkipRetry
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not $env:GRADLE_USER_HOME) {
    $env:GRADLE_USER_HOME = Join-Path $root ".gradle-local"
}

Write-Host "=== PixiJoy board QA depth ==="

Write-Host "`n>>> Unit: catalog batches + journeys + typography"
.\gradlew :app:testDebugUnitTest --console=plain `
    --tests "com.pixijoy.ai.ui.board.BoardCatalogBatchesUnitTest" `
    --tests "com.pixijoy.ai.ui.board.BoardFlowJourneysUnitTest" `
    --tests "com.pixijoy.ai.ui.board.BoardUiCatalogUnitTest" `
    --tests "com.pixijoy.ai.ui.board.BoardTypographyUnitTest"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($UnitOnly -or $SkipInstrumented) {
    Write-Host "`nUnit-only / skip instrumented — done."
    exit 0
}

$batchClasses = @{
    core    = "com.pixijoy.ai.board.BoardCatalogBatchCoreComposeTest"
    create  = "com.pixijoy.ai.board.BoardCatalogBatchCreateComposeTest"
    ctools  = "com.pixijoy.ai.board.BoardCatalogBatchCToolsComposeTest"
    media   = "com.pixijoy.ai.board.BoardCatalogBatchMediaComposeTest"
    library = "com.pixijoy.ai.board.BoardCatalogBatchLibraryComposeTest"
    me      = "com.pixijoy.ai.board.BoardCatalogBatchMeComposeTest"
    social  = "com.pixijoy.ai.board.BoardCatalogBatchSocialComposeTest"
}

$toRun = if ($Batch -eq "all") { @($batchClasses.Keys | Sort-Object) } else { @($Batch) }

$failed = @()
foreach ($b in $toRun) {
    $cls = $batchClasses[$b]
    Write-Host "`n>>> Catalog batch: $b ($cls)"
    .\gradlew :app:connectedDebugAndroidTest --console=plain `
        "-Pandroid.testInstrumentationRunnerArguments.class=$cls"
    if ($LASTEXITCODE -ne 0) {
        $failed += $cls
        if (-not $SkipRetry) {
            Write-Host "Retrying $cls ..."
            .\gradlew :app:connectedDebugAndroidTest --console=plain `
                "-Pandroid.testInstrumentationRunnerArguments.class=$cls"
            if ($LASTEXITCODE -eq 0) {
                $failed = @($failed | Where-Object { $_ -ne $cls })
            }
        }
    }
}

if ($Batch -eq "all") {
    Write-Host "`n>>> Day-in-life"
    $day = "com.pixijoy.ai.board.BoardDayInLifeComposeTest"
    .\gradlew :app:connectedDebugAndroidTest --console=plain `
        "-Pandroid.testInstrumentationRunnerArguments.class=$day"
    if ($LASTEXITCODE -ne 0) { $failed += $day }

    Write-Host "`n>>> Golden keys"
    $golden = "com.pixijoy.ai.board.BoardGoldenKeyComposeTest"
    .\gradlew :app:connectedDebugAndroidTest --console=plain `
        "-Pandroid.testInstrumentationRunnerArguments.class=$golden"
    if ($LASTEXITCODE -ne 0) { $failed += $golden }
}

if ($failed.Count -gt 0) {
    Write-Host "`nFAILED:"
    $failed | ForEach-Object { Write-Host " - $_" }
    exit 1
}

Write-Host "`nBoard QA depth PASSED (batch=$Batch)"
Write-Host "Optional screenshots: .\scripts\capture_board_phone.ps1 -GoldenOnly"
exit 0
