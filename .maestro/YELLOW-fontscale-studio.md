# Wave 2 — fontScale 1.3 studio smoke

**Prep (Pixel_7a `emulator-5556`):**
```
adb -s emulator-5556 shell settings put system font_scale 1.3
adb -s emulator-5556 shell am force-stop com.pixijoy.ai
```

**Maestro:** `.maestro/chrome/fontscale-studio.yaml`

**Restore:**
```
adb -s emulator-5556 shell settings put system font_scale 1.0
```

**Code:** `studioGlassMaxDp(height, fontScale)` grows glass under large type (cap ~55% height).

**Contrast:** Glass uses white `PixiText` on ~40% black frosted scrim — readable at 1.3/1.5.
