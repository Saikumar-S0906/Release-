# YELLOW — Layers hub stack (soft-emu not claimed green)

**Status:** product seed landed. Soft-emulator Maestro stays yellow — this change does not mark a device run green.

**Seed:** `create_hub_layers` + Try a sample used to open with `decodeLayers()` empty, so `layers_panel` (only composed when `layers.isNotEmpty()`) waited on a later op. A Layers-hub photo session now starts with one real Background layer: identity `PhotoOp.Grade` composited on the decoded source bitmap. The stack is non-empty before the first frame, so fade / blend / visibility can compose without the editor → adjust → blur detour. Switching into Layers later still uses the same seed if the stack is empty. Deleting that layer does not immediately re-insert it.

**Assert path (device, not run here):** `.maestro/layers/layers-visibility.yaml` and `layers-blend.yaml` — `create_hub_layers` → Try a sample → `layers_panel`.

**Mask path:** unchanged. `create_hub_layers` (short rail includes `session_mask`) → `mask_protect` / `mask_erase`.
