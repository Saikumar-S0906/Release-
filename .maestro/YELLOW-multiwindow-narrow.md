# Wave 2 — multi-window / narrow viewport

**Gate:** `isStudioTwoPane` still requires width ≥ 800 **and** height ≥ 480. Split-screen halves on phones stay single-pane bottom glass.

**Maestro (Pixel_7a):**
```
adb -s emulator-5556 shell wm size 1080x1600
maestro --device emulator-5556 test .maestro/chrome/multiwindow-narrow.yaml
adb -s emulator-5556 shell wm size reset
```

Asserts `studio_chrome_bar` visible and `studio_session_side_tools` **not** visible.

**Manual (optional):** Android split-screen with another app — resize handle must keep Export reachable and not flip to side dock on phone.
