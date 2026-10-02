# PixiJoy — Perfect build inventory

**Goal:** Director kit that works as labeled, tools are effortless to reach, preview ≈ export.  
**Spine:** [`DIRECTOR_KIT.md`](DIRECTOR_KIT.md) · CG [`R7_CG_EFFECTS.md`](R7_CG_EFFECTS.md) · waves in [`REMAINING_MASTER_PLAN.md`](REMAINING_MASTER_PLAN.md)  
**Coding phases (start now, no Colab wait):** [`CODING_PHASES.md`](CODING_PHASES.md)  
**Rule:** Colab trains / exports TFLite → pin on [Release-](https://github.com/Saikumar-S0906/Release-) (or APK if ≤~3 MB) → LiteRT bind → timeline/photo op. No cloud inference on the editor path. **Do not idle the app team on Colab** — Phases P0–P6 are pure coding.

---

## 0. Already strong (do not rebuild)

| Area | Status |
|------|--------|
| Photo R3 depth (layers, LUT, sky, warp, presets, P3) | MET |
| Movie R4 (multi-track, grade, scopes, audio duck, PiP, chroma, HDR) | MET |
| Pack runtime + SR / Denoise / Track / FILM / classical VFX / 3D titles / AR / FFmpeg / smart+PDF | Largely MET |
| UX chrome G1–G9 | Mostly MET; TalkBack / crop-snap device yellow |

**Perfect means polish + remaining pillars — not starting over.**

---

## 1. Colab ML (you train → Release- or APK)

**Parallel rule:** Do **not** wait for inpaint to finish before starting Depth / Video matting / Flow. Use separate Colab runtimes (or queue after M1). App binds each pack independently via `engineReady`.

| # | Pack | Wave | Why directors need it | Colab job | Ship |
|---|------|------|----------------------|-----------|------|
| **M1** | **Inpaint** (E1) | E | Boom/logo remove, continuity plates under CG | Finish full UNet RGB+mask → TFLite FLOAT32 | Release- if large; pin SHA; Download CTA |
| **M2** | **Depth** (H5) | H | Soft Z: CG behind/in front of subject | **Quality** monocular depth (real NYU/KITTI+) → TFLite | Release- `depth.tflite` — **no smoke pin** |
| **M3** | **Video matting** (H6) | H | Clean subject plate for set extension / sky | **Quality** matte (VideoMatte/AIM hair edges) → TFLite | Release- `matting.tflite` — **no smoke pin** |
| **M4** | **Optical flow / stabilize** | H / D8 | Cleaner track attach, reframe beyond crop-zoom | **Quality** flow (Sintel+) → TFLite | Release- `flow.tflite` — **no smoke pin** |
| **M5** | **Roto-lite upgrade** (optional) | F1 | Better than point-track for plates | Mask-track / box-track distill | Only if F1 quality gaps show up |
| **M6** | **Portrait retouch heavy** (optional C8) | C/E | Soft skin / face polish pack | Small face net | Release-; keep light Soften as is |
| **M7** | **Sky matte upgrade** | C4 | Close photo sky gap vs HSV heuristic | Sky segmenter (quality) | Release- — **planned with M2–M4, not “cheap later”** |

**Already have (Colab not required unless quality-upgrade):** Track, Super-res, Denoise, FILM interp.

**Quality law:** [`tools/ML_QUALITY_BAR.md`](../tools/ML_QUALITY_BAR.md) — smoke ≠ ship. Teacher distill OK; gates hard.

### Colab priority if GPU quota is tight

1. **M1 Inpaint** — quality finish  
2. **M2 Depth** — quality (2nd notebook)  
3. **M3 Video matting** — quality  
4. **M4 Flow/stab** — quality  
5. **M7 Sky** — after M3 or parallel if GPU free  

### Colab checklist per model

`--quality` train on real `data/READY` → export TFLite FLOAT32 → contract test → **quality gates** → SHA-256 → Release- → app `PackId` pin + `engineReady` → never label Telea/heuristic as neural.

**Lab folders:** `tools/inpaint/` (M1) · `tools/depth/` (M2) · `tools/matting/` (M3) · `tools/flow/` (M4) — each with train/export/verify + `train_colab.ipynb`.  
**Runbook:** [`tools/COLAB_ML_RUNBOOK.md`](../tools/COLAB_ML_RUNBOOK.md) · pack zips: `.\tools\pack_ml_labs.ps1`  
**Do not Colab:** “Avatar one model,” undress/NSFW, path-traced CGI. CG fidelity = Filament + assets + depth/matte assists.

---

## 2. Engine / product build (Kotlin — not Colab)

### A. Close ML wiring (near-term)
- E1 pin + Download after M1 upload; preview/export neural path
- Device Maestro: Track follow, FILM export, SR/Denoise Apply only when `engineReady`
- Honest UI: Ready file ≠ neural until bind

### B. Wave H — CG (R7)
| ID | Build |
|----|--------|
| H1 | Filament → timeline CG lane; preview = export |
| H2 | glTF import/place/scale; Release- prop pack |
| H3 | Lights + IBL/HDRI; match timeline grade |
| H4 | GPU particles (not sticker sparkle) |
| H5 | Depth pack bind + soft Z-comp |
| H6 | Matting pack + plates |
| H7 | CG camera keyframes + motion blur |
| H8 | Set extension / sky dome + shadow catcher |
| H10 | **MET** — `:feature_cg` on-demand size + mid-range fps/MiB budgets ([H10_PERF_SIZE_GATE.md](H10_PERF_SIZE_GATE.md)) |

### C. Wave I — Assembly
Markers, bins, versions, snap/J-L, proxy scrub, multicam polish

### D. Wave J — Color
Secondary/power windows, shot match, LUT order, scopes truth, still↔movie lookbook

### E. Wave K — Sound
Stem buses, ADR/VO record, slip/sync, LUFS, spot FX — **K1–K4 coded** (spot FX K5 optional)

### F. Wave L — Delivery
Review notes, EDL/FCPXML-lite, deliverable presets, export metadata — **L1–L4 coded** (sidecar meta; gaps documented in UI)

### G. Assets on Release- (not ML)
- 5–10 CC0/licensed **glTF** props  
- 1–2 **HDRI** environments  
- Optional SFX pack for K5  

---

## 3. UX perfection (makes “flawless”)

| # | Work | Why |
|---|------|-----|
| **U1** | Tool find ≤2 taps — search or clear categories (Edit / Color / Sound / VFX / CG / Deliver) | Directors don’t hunt |
| **U2** | Status chip: Live / Needs pack / Export-only | Trust |
| **U3** | Same Apply / Undo / Compare muscle memory photo+movie | Muscle memory |
| **U4** | Device TalkBack pass (close G4 yellow) | Access + polish |
| **U5** | Mid-range perf: scrub, grade, CG preview 30 fps budget | Enjoyable |
| **U6** | Five-user / Maestro gate before calling a wave “done” | No fake greens |
| **U7** | Empty/error copy: what to download, never overclaim neural | Honesty |

---

## 4. Ship order (“perfect” path)

**Coding (primary — see [`CODING_PHASES.md`](CODING_PHASES.md)):**  
`P0 UX → P1 CG H1–H3 → P2 Assembly → P3 Color → P4 Sound → P5 CG polish + classical H5/H6 → P6 Delivery → P7 ML plugs`

```
Colab (your queue — never blocks P0–P6):
  M1 Inpaint     ── keep / finish ──▶ P7 E1 pin
  M2 Depth       ── after M1      ──▶ P7 H5 soft Z
  M3 Matting     ── after M2      ──▶ P7 H6 plates
  M4 Flow/stab   ── after M3      ──▶ P7 D8/attach
```

**Hero reel exit (director kit v1):** multicam assembly + markers + secondary grade + stems + tracked glTF + (classical or neural) depth soft-comp + review export — **preview ≈ export**, tools findable in ≤2 taps.

---

## 5. Out of scope (keeps the app honest)

- Theatrical Atmos / full Dolby Vision authoring  
- Nuke / USD / path-traced farm on phone  
- Cloud collab / login-required edit  
- Generative undress / NSFW body invent  
- Stuffing Filament + HDRI + FILM into **base** APK  

---

When this list drifts, update this file with `DIRECTOR_KIT.md` / `TOTAL_REMAINING.txt` in the same change.
