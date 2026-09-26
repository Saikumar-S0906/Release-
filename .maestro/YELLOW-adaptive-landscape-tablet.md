# Adaptive Studio — landscape phone + tablet (2026-09-26)

## Gates

- Hub catalog | canvas: `isStudioTwoPane` → `width ≥ 800 && height ≥ 480` (tablet / fold-inner only)
- Session side tools: `isStudioSessionSideTools` → hub gate **or** wide phone landscape (`width ≥ 700 && width > height && height < 560`)

Phone landscape dock width uses `studioSideDockWidthDp` (~280–320 dp). Open session is **full-bleed** (no hub catalog, no bottom nav).

| Device | Size | Hub | Session |
|--------|------|-----|---------|
| Phone portrait | ~411×891 | Bottom glass | Bottom glass |
| Phone landscape | ~914×411 | Single-pane hub | Side tools (full-bleed) |
| Pixel Tablet | ~800×500 | Catalog \| canvas | Side tools (360 dp) |
| Fold outer | ~600×960 | Single-pane | Bottom glass |
| Narrow / split sim | `wm size 1080x1600` | Bottom glass | Bottom glass |

## Maestro

- `.maestro/chrome/tablet-side-tools.yaml` — Pixel_Tablet
- `.maestro/chrome/landscape-phone-session.yaml` — host `user_rotation 1` (not `wm size` on API 35); expects `studio_session_side_tools`, not hub catalog
- `.maestro/chrome/multiwindow-narrow.yaml` — `wm size 1080x1600`
- `.maestro/chrome/fontscale-studio.yaml` — `font_scale 1.3`

## Product notes

- Privacy Continue is sticky under short height so landscape can accept without clipping.
- `StudioChromeBar.imePadding()` lifts glass above the IME on both panes.
- Narrow landscape / multi-window still uses bottom glass; max ~42% of height (160–220 dp).
- Create sheet densifies under short height; Sample sits above Files/Camera.
