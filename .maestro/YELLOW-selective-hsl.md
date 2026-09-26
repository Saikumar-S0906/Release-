# Selective HSL — green (Pixel_7a `emulator-5556`, 2026-09-25)

Re-checked after glass h-scroll / More chrome. Flow: `.maestro/hsl/selective-hsl-apply.yaml` — **EXIT=0**.

| Tag | Role |
|-----|------|
| `session_selective_hsl` | Rail chip to open Selective HSL tool |
| `hsl_range_reds` … `hsl_range_magentas` | Range chips (`hsl_range_${enum.lowercase()}`) |
| `session_selective_hue` | Hue slider |
| `session_selective_sat` | Saturation slider |
| `session_selective_light` | Light slider |
| `session_apply_selective_hsl` | Apply (bakes `PhotoOp.SelectiveHsl`) |
| `session_selective_hsl_hint` | Hint text |

Band row is `StudioHScrollRow` — Magentas may need `studio_hscroll_more`. Export bar stays outside glass scrim (`session_tool_scrim`). See also `.maestro/GREEN-selective-hsl-exit.md` (B1 exit).
