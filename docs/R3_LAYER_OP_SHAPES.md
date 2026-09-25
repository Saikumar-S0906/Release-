# R3 prep - Photo layer / mask op shapes

**Status:** Architecture for R3 Layers (in flight on `next`).
**Pairs with:** UX self-explain (Layers hub, Fade, Protect/Erase, Blend); `docs/NORTH_STAR_ROADMAP.md`.
**Hard rule:** edit-graph op -> `:photo` engine -> WorkManager export. UI never owns giant bitmaps.

---

## 1. Goal

Move from a flat `List<PhotoOp>` toward a **layer stack**: named layers, visibility, opacity, blend, per-layer masks - without breaking existing encode/decode.

---

## 2. Data model

```
Project (PHOTO)
  LayerStack
    Layer { id, name, visible, opacity 0..1, blend: Normal|Multiply|Screen|Overlay, kind }
      kind = BitmapRef | Adjustment | Text | Shape | Fill
      mask: Mask?
  EditGraph: List<PhotoOp>  // existing ops; new ops may target layerId
```

### Engine-facing ops

| Op | Meaning |
|---|---|
| `PhotoOp.AddLayer` | insert layer |
| `PhotoOp.ReorderLayers` | z-order |
| `PhotoOp.SetLayerProps` | visible / opacity / blend / name |
| `PhotoOp.PaintMask` | stamp list on mask (explicit Stamp source nullable) |
| `PhotoOp.ClearMask` | |
| Existing Heal/Erase/Grade/... | optional `layerId` (default = active) |

UX labels: Fade = opacity; Protect / Erase area = `PaintMask`; Darken/Lighten/Contrast map to Multiply/Screen/Overlay (plus Normal).

### Assert tags (agreed)

`create_hub_layers`, `layers_panel`, `layer_row_{id}`, `layer_visibility`, `layer_fade`, `mask_protect`, `mask_erase`, `blend_normal` .. `blend_overlay`.

---

## 3. Engine algorithm

1. Rasterize bottom -> top: blend + opacity * mask.
2. Adjustment layers apply to composite below.
3. Preview: downscaled / GPU texture; full-res only on export worker.
4. Blend v1: Normal, Multiply, Screen, Overlay.

---

## 4. First-slice exit

- Round-trip encode/decode of 2+ layers + one mask
- Unit: blend Normal/Multiply; mask paint round-trip
- Maestro: open Layers -> toggle visibility -> export still green
- Base APK size unchanged (no new ML)

---

## 5. Out of scope (first slice)

Smart objects, vector paths, group folders, generative fill, RAW (parallel track).

---

*Architect - re-parked on next. Update when UX tags lock or first slice exits.*

---

## First-slice exit: MET

**Tip:** `29b0753` / `2292dfd` (BOM soft-fix).
**Met:** default Background Grade seed; Fade / visibility / Protect-Erase / Blend; encode/decode + Maestro visibility/blend/mask green on Pixel_7a.
**Date noted:** 2026-09-18 ~12:01 IST.

## Curves -> LUT handoff (next grade)

- **Curves (current):** keep `PhotoOp.Curves` as the only tone-curve op. LiveGrade preview -> Apply bakes `PhotoOp.Curves` onto the active layer / graph -> export via existing photo path. No second curve model.
- **Bundled LUT (next):** add `PhotoOp.Lut(assetKey)` applied after curves in composite order. Ship 3-5 bundled LUTs; **no file-picker import** in this burst.
- **Order:** Background -> layer raster -> Curves -> Lut -> export. UI: Curves first; LUT later with plain names (Film / Cool / Warm).
- **Do not:** invent a parallel grade graph, merge LUT into Curves points, or block on RAW.

---

## Looks / LUT exit: MET (+ C1 import wire-up 2026-09-23)

**Tip:** `3aa262e` (bundled); C1 import in working tree.
**Met:** `PhotoOp.Lut(assetKey)` / graph `U`; bundled Original/Clean/Warm/Cool/Film/Night (ColorMatrix); chip live preview; Apply bakes; Maestro looks-apply green on Pixel_7a.
**C1 import:** `.cube` / `.3dl` parse (`CubeLutParser`); project `luts/` copy; graph keys `lut:<file>`; engine `lutDirectory`; Looks **Import LUT**; export parity; units green; system picker **yellow/manual** (`.maestro/YELLOW-lut-import.md`). Cap size 33 for imported 3D apply (nearest).
**Explicitly later:** tetrahedral sampling; LUT size > 33 at full export res.

## Selective HSL exit: MET (green Pixel_7a)

**Tip:** Phase 0 land `b4169a1`.
**Met:** `PhotoOp.SelectiveHsl` graph `A`; Maestro **green** on Pixel_7a. See `.maestro/GREEN-selective-hsl-exit.md`.

## Sky / background replace (C4): MET

**Tip:** land with this commit.
**Met:** `PhotoOp.SkyReplace(target, color)` graph `H`; HSV + top-bias sky matte; background via subject mask or corner-key; solid presets; session rail under glass; Maestro `sky-replace-apply.yaml` **green** Pixel_7a.
**Explicitly later:** image fill / LUT sky packs; MediaPipe sky segmenter as optional ML pack.

## Wave C depth (C3/C5–C8): MET

**Met:**
- **C3** Fill undo stroke + clear; Telea; E1 `LiteRtInpaintHook` consults `:ml` `PackEngineHooks` (engine unbound).
- **C5** `PhotoOp.Perspective` (`D`) + `Liquify` (`M`); `WarpOps`; session Warp rail.
- **C6** `PhotoPreset` local files; Library **Export all photos** via `BatchPhotoExportWorker`.
- **C7** `ColorIntent` sRGB / Display P3 on export path (best-effort device support).
- **C8** light `PhotoOp.Soften` (`O`); model-heavy → R5 Super-res/Denoise graphs `SR`/`N` (Apply gated).
**Units:** `WarpOpsTest` + graph round-trip + `PackEnhanceOpTest`. Maestro: `.maestro/warp/`, `.maestro/soften/`, `.maestro/packs/`.

## R5 pack runtime: MET (engines unbound)

See `docs/R5_ML_PACKS.md`. F1 `ClipTrack` contract landed.

## Next

Bind LiteRT interpreters. Do not ship Telea/bicubic as neural.

See `TOTAL_REMAINING.txt` and `REMAINING_MASTER_PLAN.md`.

