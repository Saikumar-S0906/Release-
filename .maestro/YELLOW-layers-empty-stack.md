# YELLOW — Layers hub empty stack

**Status:** yellow / documented (R3)
**Why:** `layers_panel` only composes when `layers.isNotEmpty()` (`PhotoSessionScreen`). Opening via `create_hub_layers` + Use sample starts with an empty `decodeLayers()` list, so panel / fade / blend / visibility tags are absent until an op is pushed.

**Green path used for gate:** `create_hub_editor` → Use sample → `session_adjust` → `session_blur` (seeds a `LayeredOp`) → `session_layers` → assert panel.

**Mask path:** use `create_hub_layers` (short rail includes `session_mask`) → `mask_protect` / `mask_erase`.

**Product ask (optional):** seed a base bitmap layer on Use-sample for Layers hub so panel is immediately assertable.
