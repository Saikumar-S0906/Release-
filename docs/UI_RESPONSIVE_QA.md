# PixiJoy UI-7 Responsive / Visual QA

Manual matrix after UI scaffold parity. Not pixel-perfect branding — check hierarchy, CTAs, chrome, and adaptive behavior.

## Breakpoints

| Class | Width | Expected chrome |
|-------|-------|-----------------|
| Compact | &lt;600dp | Bottom nav + center Create FAB |
| Medium | 600–839dp | Bottom nav; wider content; ~3-col grids |
| Expanded | ≥840dp | **Navigation rail** + Create FAB; hinged padding; two-pane demos |

## Checklist

1. Phone portrait — Home / Create / Community / Profile match board section order.
2. Large phone / fold closed — style & template grids use 2–3 columns.
3. Tablet / fold open — rail visible; Responsive QA two-pane shows list|detail.
4. Font scale 1.3 (Settings → Display) — primary CTAs remain fully tappable (min 54dp).
5. Light + Dark system theme smoke on Flow 1, 2, 8, 27.
6. Force RTL — nav labels / back affordances still usable.
7. Flow Index opens every scaffolded flow host.

## In-app tools

- **Settings → Responsive QA** — live width class, font scale, RTL, theme + demos.
- **Settings → UI Flow Index** — all flow hosts.
- Resizable emulator / tablet AVD / foldable AVD for Expanded class.

## After UI-7

Functionality wire-up (AI jobs, Room, billing, sync) is a **separate** milestone — do not block on pixel art assets.

## D01–D04 device chrome (Phase D)

From `additional_flow/PixiJoy_02a` device boards — validate against the checklist above:

| Board | Focus |
|-------|--------|
| **D01 Phones** | Compact portrait/landscape; bottom nav + Create FAB; Flow 2 hero CTA |
| **D02 Tablets** | Expanded rail; two-pane where used; Create hub Tools wrap |
| **D03 Foldables** | Hinge-safe padding; closed≈phone / open≈tablet |
| **D04 Responsive Home** | Width-class transitions; Tools chips scroll on compact |

Manual pass: Settings → Responsive QA + Home / Create / Community on phone, tablet, and fold AVD.
