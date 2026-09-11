# PixiJoy Visual Identity (locked)

Source: `pixijoy_asset_starter_pack.zip` + `PixiJoy.png` wireflow board.  
Coding agents must follow `design/AI_AGENT_ASSET_RULES.md`.

## Brand

- **Promise:** One photo. Endless possibilities.
- **Tone:** Clear for ages 7+, premium for adults — not baby pastel, not tool-dump clutter.
- **Logo:** `design/pixijoy_logo.svg` (final 1024 Play master still required).

## Color tokens

| Token | Hex | Use |
|-------|-----|-----|
| Background | `#0B0B10` | App canvas |
| Surface | `#15151C` | Cards / bars |
| Surface 2 | `#20202A` | Elevated sheets |
| Text | `#FFFFFF` | Primary copy |
| Text secondary | `#B7B7C7` | Hints |
| Border | `#30303A` | Dividers |
| Accent start | `#6C4DFF` | Gradient / AI |
| Accent end | `#FF5AA5` | Gradient / CTA |
| Success | `#26C281` | Recoverable OK |
| Error | `#FF5B68` | Failures |
| Warning | `#F4B740` | Soft warnings |

XML: `res/values/colors.xml` (`pixijoy_*`). Compose: `ui/theme/Color.kt`.

## Typography

- **Board face:** Platform sans in Compose (`FontFamily.SansSerif`) — crash-safe on all devices/emulators. Downloadable Noto XML remains for canvas (`CanvasFontHelper`) with try/catch fallback only.
- **Hierarchy (`BoardType` — Design Bible §7):** hero 32 Bold · largeHeadline 28 Bold · headline 24 SemiBold · title 20 SemiBold · body **16** · secondary 14 · caption 12 Medium · CTA 14 SemiBold.
- Prefer `BoardTitleText` / `BoardBodyText` / `BoardCaptionText` / kit chrome over ad-hoc `fontSize`.
- Primary CTAs SemiBold; AI generate CTAs may use purple→pink gradient (`aiAccent`) — not wallpaper gradients.
- Tokens: `PixiSpacing` / `PixiRadii` / `PixiMotionMs` + `BoardColors.dark()` default (light via `BoardColors.light()`).

## Icons

- Prefer `pj_ic_*` vector drawables in `res/drawable/`.
- Never invent a core icon when `pj_ic_*` exists.
- Never use emoji alone for nav or critical actions.

## AI sparkle language

- Use `pj_ic_sparkles` + accent gradient **only** on AI paths (Talk to PixiJoy, analyzing, generation).
- Subtle / premium — not cartoon spam.

## Layout / touch

- Min touch target 48dp (`pj_touch_target`).
- Radii: 12 / 18 / 24 (`pj_radius_*`).
- Spacing scale: 4–32 (`pj_spacing_*`).

## Motion

See `motion/MOTION_MANIFEST.json`. Every animation needs a reduced-motion alternative.

## IA (kid-clear)

Five tabs (chrome labels): **Home · Explore · Create · Community · My Stuff**.  
Create tab is **Tools** (default) | **AI** — not a sixth tab. Beautiful / Funny / Different / Tell live on **AI**. Editor, Restore, Remove BG, Stickers, Move live on **Tools**.  
Older `PixiJoy.png` 4-tab chrome is obsolete; follow [pixijoy-cursor-nav/docs/NAVIGATION.md](../pixijoy-cursor-nav/docs/NAVIGATION.md).  
Photo input: **Take a Photo | Choose from Gallery | Import from Files**.  
Every result: **Save · Share · Edit · Export · Remake · Publish**.
