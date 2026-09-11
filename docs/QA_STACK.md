# PixiJoy 99.9% Automated QA Blueprint

Goal: **99.9% automated QA execution** (not a claim of 99.9% defect detection).

```text
                    PIXIJOY APP
                          │
             ┌────────────┴────────────┐
             │                         │
       DETERMINISTIC               EXPLORATORY
         TESTING                     TESTING
             │                         │
     ┌───────┼────────┐                │
     │       │        │                │
   Unit    UI/E2E   Integration      ROBO (FTL)
     │       │        │                │
     └───────┼────────┘                │
             │                         │
             └──────────┬──────────────┘
                        ↓
              Firebase Test Lab matrix
                        ↓
         Visual / Perf / A11y / Security (phased)
                        ↓
                   AI triage + retest
```

## Layer map (PixiJoy)

| Layer | Status | How to run |
|-------|--------|------------|
| **Lint** | Ready | `./gradlew :app:lintDebug` |
| **Detekt** | Ready | `./gradlew detekt` |
| **Unit + fuzz** | Ready | `./gradlew testDebugUnitTest` |
| **Compose UI / E2E** | Ready | `.\scripts\run_qa_suite.ps1 -Suite e2e` |
| **Maestro E2E** | Ready | `.\scripts\run_maestro.ps1` — 5-tab Tools \| AI (`maestro/`) |
| **Board QA depth** | Ready | `.\scripts\run_board_qa_depth.ps1` (batched catalog + day-in-life + golden keys) |
| **Board golden shots** | Ready | `.\scripts\capture_board_phone.ps1 -GoldenOnly` → `docs/board_review/golden/` |
| **UI Automator** | Ready | class `UiAutomatorLifecycleTest` |
| **Monkey / stability** | Ready (lighter load) | `-Suite monkey` |
| **FTL Robo** | Scripted | `.\scripts\run_ftl_robo.ps1` (needs gcloud + Firebase) |
| **FTL instrumentation** | Scripted | `.\scripts\run_ftl_instrumentation.ps1` |
| **Macrobenchmark** | Module `:benchmark` | `./gradlew :benchmark:connectedBenchmarkAndroidTest` |
| **Screenshot / visual** | Board golden pack ready; Paparazzi/Roborazzi still optional | `capture_board_phone.ps1 -GoldenOnly` vs `flows/additional/REMATCH_*` |
| **A11y** | Planned | Compose semantics + Espresso a11y checks |
| **Security** | Planned | Dependabot + OWASP dependency-check |
| **Play pre-launch** | Free with Play Console | Internal testing track |

## Deterministic vs exploratory

- **Your tests** assert business rules (credits, cold start, meme screen).
- **Robo** explores unknown paths and finds crashes you didn't script.
- Use **both**.

Recommended Robo seed flow:

```text
Launch → (accept privacy if shown) → Home → Create hub → let Robo crawl
```

## CI cadence

| When | What |
|------|------|
| **PR** | Detekt + Lint + unit + fuzz |
| **Nightly** | Local emulator suites + FTL Robo (en/hi/ar + tablet) |
| **Release** | Full FTL matrix + Play pre-launch + Macrobenchmark |

GitHub Actions: `.github/workflows/qa.yml`

## Folder layout

```text
app/src/test/                 # Unit + fuzz
app/src/androidTest/          # Compose + UI Automator + E2E
benchmark/                    # Macrobenchmark startup
ftl/                          # Device matrix + Robo notes
scripts/run_qa_suite.ps1      # Local suites (-Suite board → depth)
scripts/run_board_qa_depth.ps1 # Batched board catalog + DAY + golden keys
scripts/capture_board_phone.ps1 # Phone PNGs (-GoldenOnly)
scripts/run_ftl_robo.ps1      # Cloud Robo
scripts/run_ftl_instrumentation.ps1
scripts/analyze_qa_failures.ps1
config/detekt/detekt.yml
```

## Initial scenario buckets

Already mapped in `QaTestMatrix.kt`. Expand toward:

1. Cold start / privacy / onboarding
2. 5-tab navigation
3. Create hub tools (meme, sticker, photo)
4. Credits / Pro gates
5. Settings / bug report / consent
6. Locale × RTL
7. Lifecycle: home/back/rotation (UI Automator started)
8. Network offline / slow banner
9. Permission deny paths
10. Generation failure / empty photo

## Reality check

Automate **execution** of repetitive QA. Usability and creative taste still need product judgment.

## Next phases

1. Wire Firebase project + enable FTL Robo in nightly CI
2. Paparazzi screenshots for Home/Create/Settings × en/hi/ar × light/dark
3. Baseline Profiles + Macrobenchmark thresholds in CI
4. Accessibility Compose checks on primary CTAs
5. Dependabot + Play pre-launch on every internal track upload
