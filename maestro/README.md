# PixiJoy Maestro E2E (new IA)

Black-box flows for `com.pixijoy.ai` against **Home · Explore · Create · Community · My Stuff**.  
Create tab is **Tools** (default) | **AI**.

## Suites

| Folder | What it proves |
|--------|----------------|
| `positive/` | Cold start, tabs, Tools functions (crop/adjust/filters/text/heal, restore, expand, cutout+erase, stickers, move, talking, collage), AI Beautiful/Funny/Different/Tell |
| `negative/` | Privacy gate, Tools/AI without photo, Tools hides AI tiles, Back from every Tools tile |
| `edge/` | Rapid tabs, double Home, Tools→AI back stack, double-back from a tool |
| `monkey/` | Chaos nav + Tools/AI toggle |
| `helpers/` | Onboard skip, ANR Wait, consent, re-enter app, Create hub |

## Run

```powershell
.\scripts\run_maestro.ps1 -Device emulator-5556
.\scripts\run_maestro.ps1 -Device emulator-5556 -Suite positive
.\scripts\run_maestro.ps1 -Flow maestro\positive\03_create_tools.yaml
```

Reports: `maestro/reports/<timestamp>/`

## Hooks

- Compose `testTag` maps to Maestro `id:` (`testTagsAsResourceId`)
- Create hub: `create_hub_segment_tools` / `create_hub_segment_ai`
- Tools tiles: `create_hub_editor` `create_hub_restore` `create_hub_expand` `create_hub_remove_bg` `create_hub_sticker` `create_hub_move` `create_hub_talking` `create_hub_collage`
- On-tool sample (no gallery): `tool_use_sample` · landing continue: `tool_continue`
- Editor workspace: `flow15_workspace` `flow15_crop` `flow15_adjust` `flow15_filters` `flow15_text` `flow15_more` `flow15_heal` `flow15_apply`
- Cutout erase: `flow_c04_erase` `flow_c04_apply`
- AI primary: `create_hub_beautiful`
- Empty Home studio CTA: `c00_open_studio`

## Flake notes

- System ANR sheets: helpers tap **Wait**; `run_maestro.ps1` sets `debug.pixijoy.e2e=1`
- Home-root Back finishes the Activity — edge/monkey use `ensure_in_app`
- Gallery pickers are avoided; tools use `tool_use_sample` (in-app sample bitmap)
- These flows do **not** call live Gemini; processing uses on-device / sample fallback
