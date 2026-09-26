# Studio TalkBack — a11y labels green on Maestro; full TalkBack still manual

**Maestro (Pixel_7a `emulator-5556`, 2026-09-26):** `.maestro/chrome/talkback-studio.yaml` — **green**. Asserts spoken crop-edge strings and rail labels in the accessibility tree without enabling system TalkBack.

A desktop pass with TalkBack **on** is still useful for focus order / live regions, but the tag + contentDescription surface is covered.

Semantics covered in code (unit-tested where the phrase is pure):

- Session rail (`studio_group_*`): short label only. Selected speaks "Looks, selected". No control-type role is set on the rail.
- Tool chips (`StudioChip`): same pattern, state only inside the name.
- Crop overlay `session_crop_overlay` has no frame description. Four edge nodes speak "Crop edge, left", "Crop edge, right", "Crop edge, top", and "Crop edge, bottom".
- Export success reuses the visible labels View, Share, and Done (`export_view` / `export_share` / `export_done`). Rename reuses "Name this project".
- Pack status: a verified hosted file speaks "Downloaded". `engineReady` speaks "Ready to apply". Those names replace the long visible sentence so TalkBack does not say "neural" or "AI" before the engine is bound. Apply on super-res, denoise, and neural fill speaks "Apply" until `engineReady`, then "Ready to apply". Nearby-pixel fill, Telea, bicubic, and frame-hold stay unlabeled as neural. E4 / F1, unbound, speak "Coming with pack" instead of "Downloaded".
- Save chip `session_save_status` speaks the visible Saved / Saving… / Not saved label, with a polite live region.
- Undo, redo, Back, Export, compare, Brush, Place, trim handles, movie play/scrub.

## Optional manual (TalkBack on)

1. Open a photo and focus the tool rail. Hear "Crop" (or the visible verb). The selected tool says "Looks, selected", not a control type.
2. Open Crop. Hear the four edge names. There is no second crop-frame or corner-handle announcement.
3. Export a photo and open the success sheet. View, Share, and Done match the visible labels.
4. Open Fill or Super-res. A verified file without a bound engine says "Downloaded". After `engineReady`, the status and Apply say "Ready to apply".
5. Library overflow says "Project actions". Empty library still says "No projects yet".
