# Maestro / test_automator — Selective HSL stubs

Tags to wire (mirror Curves / Looks):

| Tag | Role |
|-----|------|
| `session_selective_hsl` | Rail chip to open Selective HSL tool |
| `hsl_range_reds` … `hsl_range_magentas` | Range chips (`hsl_range_${enum.lowercase()}`) |
| `session_selective_hue` | Hue slider |
| `session_selective_sat` | Saturation slider |
| `session_selective_light` | Light slider |
| `session_apply_selective_hsl` | Apply (bakes `PhotoOp.SelectiveHsl`) |
| `session_selective_hsl_hint` | Hint text |

Suggested flow (YELLOW until device harness picks them up):
1. Open photo editor session
2. Tap `session_selective_hsl`
3. Tap `hsl_range_greens`, drag sliders, tap `session_apply_selective_hsl`
4. Assert layer / undo stack advanced (no Home/Create/Compare collision)

Export bar stays outside glass scrim (`session_tool_scrim`) — do not assert export inside tool panel.
