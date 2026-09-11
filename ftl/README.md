# Firebase Test Lab assets for PixiJoy

- `device-matrix.yaml` — PR / nightly device × locale ideas
- Scripts: `scripts/run_ftl_robo.ps1`, `scripts/run_ftl_instrumentation.ps1`

## Robo strategy

1. Build debug APK
2. Run Robo with timeout 5–10 minutes
3. Review crash clusters + screenshots in Firebase console
4. Convert recurring failures into Compose/UI Automator regressions

Optional: record a Robo script in Firebase console that:
accepts privacy → skips onboarding → opens Create hub, then let Robo explore freely.
