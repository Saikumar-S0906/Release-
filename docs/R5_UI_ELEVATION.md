# R5 UI elevation — CapCut / Lightroom checklist

Craft pass after R4 structure. Compare on Pixel_7a (`emulator-5556`) against CapCut and Lightroom Mobile.

## Acceptance bar

| # | Bar | Status |
|---|-----|--------|
| 1 | Picture owns the frame | Keep R4 canvas weight + glass ≤280dp |
| 2 | Icon-first chrome | Back / Undo / Redo / Export are `PixiIconButton` with a11y labels |
| 3 | Quiet selection | `PixiChip` / `PixiSegment` use soft fill + hairline, not solid purple slabs |
| 4 | One accent job | Accent marks state + primary CTA; panels stay near-black layered surfaces |
| 5 | Shell feels alive | Home “Start with” strip + ghost thumbs; Library empty ghost thumbs |
| 6 | Type has roles | `PixiTypography` (Display / Title / Label / Value / Hint) via `PixiTheme` |
| 7 | Motion intentional | Group panel fade (`pixiGroupTween`), chip scale (`PixiChipSelectedScale`), reduced-motion via `pixiMotionMs` |

## What landed

- Design tokens: layered `PixiBg` / `PixiSurface` / `PixiElevated` / `PixiScrim`, quieter `PixiAccent`, `PixiAccentSoft` / `PixiAccentMuted`
- Controls: `PixiChip`, `PixiIconButton`, `PixiSlider`, `PixiApply`, `PixiSegment`
- Studio hub editorial tiles (gradient glyph plate)
- Crop L-corner handles; smaller histogram strip
- Export sheet chips on soft selection; single Export on movie top bar (GIF in sheet)
- Quieter shell nav colors

## Recapture

```powershell
.\gradlew.bat :app:assembleDebug
adb -s emulator-5556 install -r app\build\outputs\apk\debug\app-debug.apk
maestro --device emulator-5556 test --debug-output docs\ui_review\r5_after .maestro\ui_review\after-shell.yaml
maestro --device emulator-5556 test --debug-output docs\ui_review\r5_after .maestro\ui_review\after-photo.yaml
maestro --device emulator-5556 test --debug-output docs\ui_review\r5_after .maestro\ui_review\after-movie.yaml
```

Maestro CLI was not on PATH in the elevation session; run the block above when Maestro is available and drop shots under `docs/ui_review/r5_after/`.

## Functional smoke

- Maestro YELLOW: crop snap, slider value, talkback studio, export rename, create sheet tags
- Unit: `:designsystem:testDebugUnitTest`, crop / chrome-related app tests
