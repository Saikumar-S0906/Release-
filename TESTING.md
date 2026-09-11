# PixiJoy Testing Guide

See also the full multi-layer blueprint: [`docs/QA_STACK.md`](docs/QA_STACK.md)

## Quick commands

```powershell
# Local suites
.\scripts\run_qa_suite.ps1 -Suite unit
.\scripts\run_qa_suite.ps1 -Suite e2e
.\scripts\run_qa_suite.ps1 -Suite board
.\scripts\run_qa_suite.ps1 -Suite all

# Board QA depth (batched catalog — avoids full-catalog AVD melt)
.\scripts\run_board_qa_depth.ps1 -UnitOnly
.\scripts\run_board_qa_depth.ps1 -Batch create
.\scripts\run_board_qa_depth.ps1

# Golden rematch screenshots (device connected)
.\scripts\capture_board_phone.ps1 -GoldenOnly

# Maestro E2E (new 5-tab Tools | AI design)
.\scripts\run_maestro.ps1 -Device emulator-5556
.\scripts\run_maestro.ps1 -Suite positive


# Static quality
.\gradlew detekt
.\gradlew :app:lintDebug

# Cloud exploration (needs gcloud + Firebase)
.\scripts\run_ftl_robo.ps1 -Matrix pr
.\scripts\run_ftl_instrumentation.ps1 -Matrix pr

# Startup performance (physical device preferred)
.\gradlew :benchmark:connectedBenchmarkAndroidTest
```

## Board QA depth

| Piece | What |
|-------|------|
| Batches | `core` · `create` · `ctools` · `media` · `library` · `me` · `social` (`BoardCatalogBatches`) |
| Day-in-life | Journey `DAY`: Home → Create (Tools) → optional AI Beautiful → Share → My Stuff |
| Golden keys | Rematch/create entries; Compose smoke + `capture_board_phone.ps1 -GoldenOnly` |
| Full catalog | `BoardCatalogComposeTest` — nightly / FTL only |

See [QA_STACK.md](docs/QA_STACK.md) · [BOARD_RELEASE_HYGIENE.md](docs/BOARD_RELEASE_HYGIENE.md).

## On-device bug report (no root)

**Me → Settings → Send bug report**

## Reports

| Report | Path |
|--------|------|
| Unit | `app/build/reports/tests/testDebugUnitTest/` |
| Instrumented | `app/build/reports/androidTests/` |
| Detekt | `app/build/reports/detekt/` |
| Lint | `app/build/reports/lint-results.html` |
| Suite results | `app/build/reports/qa-suite-results.txt` |
| Maestro E2E | `maestro/reports/` |
