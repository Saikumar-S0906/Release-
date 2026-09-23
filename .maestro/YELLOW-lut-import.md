# YELLOW / manual — LUT file pick (C1)

`session_lut_import` opens the system document picker for `.cube` / `.3dl`.
Maestro cannot reliably drive the system picker on all emulators.

Manual:

1. Create → Try a sample → Looks
2. Tap Import LUT → choose a small `.cube` (size ≤ 33)
3. Confirm live preview, Apply, Export
4. Re-open project — graph key `lut:<file>` still applies

Automated: `.maestro/looks/lut-import-chip.yaml` asserts the Import control is reachable.
