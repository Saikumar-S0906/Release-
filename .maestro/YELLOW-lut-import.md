# LUT import — chip green; system picker still manual (C1)

`session_lut_import` opens the system document picker for `.cube` / `.3dl`.
Maestro cannot reliably choose a file in the system picker on all emulators.

**Maestro (Pixel_7a `emulator-5556`, 2026-09-26):** `.maestro/looks/lut-import-chip.yaml` — **green**. Looks → Import LUT control reachable + Apply Look + Export.

Manual (full apply path):

1. Create → Try a sample → Looks
2. Tap Import LUT → choose a small `.cube` (size ≤ 33)
3. Confirm live preview, Apply, Export
4. Re-open project — graph key `lut:<file>` still applies
