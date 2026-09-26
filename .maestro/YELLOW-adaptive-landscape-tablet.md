# Adaptive Studio — landscape phone + tablet (2026-09-26)

## Gate

`isStudioTwoPane(widthDp, heightDp)` → `width ≥ 800 && height ≥ 480`.

| Device | Size | Layout |
|--------|------|--------|
| Phone portrait | ~411×891 | Bottom glass (`studio_chrome_bar`) |
| Phone landscape | ~914×411 | Bottom glass (shorter max via `studioGlassMaxDp`) — **not** side dock |
| Pixel Tablet | ~800×500 | Side tools (`studio_session_side_tools`) |
| Fold outer | ~600×960 | Single-pane |

## Maestro

- `.maestro/chrome/tablet-side-tools.yaml` — **green** on Pixel_Tablet (`emulator-5554`)
- `.maestro/chrome/landscape-phone-session.yaml` — host sets `adb shell wm size 2400x1080` first (Maestro 2.10 has no `setOrientation`), then asserts single-pane chrome + Light slider

## Product notes

- `StudioChromeBar.imePadding()` lifts glass above the IME on both panes.
- Short landscape glass max is ~42% of height (160–220 dp) so the canvas stays visible.
