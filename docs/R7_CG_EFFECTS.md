# R7 — On-device CG / cinema VFX (director kit)

**Status:** PLANNED (next major wave after Wave F / R6+ opt-in foundations)  
**Pairs with:** `docs/DIRECTOR_KIT.md` (full director product), `docs/REMAINING_MASTER_PLAN.md` Wave H, `docs/NORTH_STAR_ROADMAP.md`, `docs/FUNCTIONALITY_MATRIX.md`  
**Positioning:** CG pillar of the **director kit** — compete with CapCut/VN/Premiere Rush on timeline craft, go **beyond** on **real CG compositing** on phone. Assembly/color/sound/delivery live in DIRECTOR_KIT Waves I–L.

**Honesty bar:** Avatar / Hollywood finals still need offline pipelines. R7 wins **director-grade mobile CG previz**: Filament scenes, tracked inserts, depth-aware comps, GPU particles, glTF props — quality that does not look like sticker overlays.

---

## Hard rules

1. **CG engines stay out of the base APK** — `:feature_titles3d` / new `:feature_cg` (Filament, glTF, IBL). SplitInstall / Play Feature Delivery.
2. **Timeline is source of truth** — every CG shot is a versioned lane/op (`TimelineCg*` encode), preview + export share one compositor path.
3. **No cheap graphics** — no emoji particles as “CG”; no unlit billboards labeled 3D; materials, lights, and depth matter.
4. **ML assists CG; ML is not CG** — depth / matting / track feed the compositor; Filament owns the render.
5. **Release- host** for large assets (HDRIs, glTF packs) via `Saikumar-S0906/Release-` — same pattern as FILM.

---

## Prerequisites (must be green before R7 exit)

| ID | Prerequisite | Why |
|----|--------------|-----|
| F1 | Point track LIVE | Attach CG / emitters to subject |
| F2 | Particle overlays LIVE | Classical seed; R7 upgrades to GPU/sim |
| F3 | Filament titles module | Engine bootstrap + SplitInstall pattern |
| F4 | AR previz gate | Device/scene placement lessons |
| E1 | Neural inpaint (Release when large) | Continuity plates under CG |
| D1–D5 | Multi-track + grade | Comp stack under titles/VFX |

---

## Wave H — R7 CG slices

| ID | Slice | Deliverable | Exit |
|----|-------|-------------|------|
| **H1** | CG compositor lane | `TimelineCgLayer` (world, camera, lights) composed under/over Media3 video via Filament offscreen → `BitmapOverlay` / Texture; export uses same bake | Unit round-trip + Maestro “add CG plate” |
| **H2** | glTF prop library | Ship 5–10 licensed/CC0 props (vehicles, set dressing) on Release-; import + place + scale + yaw; track-attach optional | Preview + export match; pack download CTA |
| **H3** | Lights + IBL | **PARTIAL** — directional + procedural SH/cubemap IBL on PropGltf in `:feature_cg`. Point lights + Release- HDRI + grade continuity still open | Side-by-side grade continuity |
| **H4** | GPU particles / trails | Filament or compute particles (ember, sparks, dust, magic) driven by F1 track or null; replaces “sticker sparkle” for CG shots | 30 fps mid-range device budget doc |
| **H5** | Depth-aware insert | Monocular depth ML pack → soft Z-composite of CG behind/in front of subject | Contract + LiteRT bind; fallback flat insert |
| **H6** | Video matting for CG plates | Temporal subject matte pack → clean plate under CG / sky / set extension | Better than selfie segmenter for movie |
| **H7** | Camera + motion blur | Virtual camera keyframes (dolly/orbit) + optional motion blur on CG layer only | Keyframe encode + export |
| **H8** | Set extension / sky dome | Spherical/HDRI backdrop + ground plane shadow catcher (Filament) | One hero demo shot Maestro |
| **H9** | Audio-reactive / beat CG (optional) | Amplitude → light intensity / particle rate (local audio analysis, no cloud) | Opt-in effect preset |
| **H10** | Performance & size gate | **MET** — `:feature_cg` on-demand; [`H10_PERF_SIZE_GATE.md`](H10_PERF_SIZE_GATE.md) mid-range MiB/frame budgets + `reportH10SizeGate` base scan; Pixel_7a checklist (soft Maestro YELLOW parked) | Feature module not in base APK measure |

Suggested order: **H1 → H2 → H3 → H4 → H7 → H8** in code first (CODING_PHASES P1+P5); **H5/H6 classical fallbacks** until M2/M3 TFLite; then neural soft-Z/plates (P7). H9 parallel, H10 continuous.

**PropGltf + IBL slice:** `:feature_cg` is on-demand and renders a user glTF with a directional key and procedural IBL (SH radiance + 1×1 reflections cubemap) through `CgHost` when the split loads. Filament JNI stays in on-demand `:feature_titles3d` (AGP will not package that AAR in a second split) and is not in the base APK. `:feature_cg` uses compileOnly Filament APIs and does not embed JNI or an HDRI asset. Cube, ground, sky, and dust stay classical, and PropGltf falls back to that raster when the split is missing or a frame fails.

---

## ML packs R7 may need (Wave M — Colab; Release- if large)

| Pack | Wave M | Role in CG | Colab |
|------|--------|------------|-------|
| Inpaint (E1) | M1 | Continuity under plates | Keep running — do not block others |
| Depth (monocular) | M2 | H5 Z-composite | **Start now** (parallel with M1) |
| Video matting | M3 | H6 plates | **Start now** (parallel with M1) |
| Optical flow / stabilize | M4 | Cleaner attach for H2/H4; D8 upgrade | Parallel, or after M2/M3 if one GPU |

Labs: `tools/inpaint/` · planned `tools/depth/`, `tools/matting/`, `tools/flow/`.  
Do **not** invent “one Avatar TFLite.” CG fidelity comes from **Filament + assets + compositor**, assisted by these packs.

---

## Competitive wedge (vs CapCut / VN / Canva Video)

| Them | PixiJoy R7 |
|------|------------|
| 2D stickers / canned 3D titles | Real Filament materials + lights + glTF |
| Flat overlay VFX | Depth/matte-aware inserts |
| Cloud Magic | On-device only |
| Template-first | Director timeline + CG lane |

---

## Non-goals (R7)

- Full path-traced offline CGI / USD film pipelines  
- Generative “undress” / NSFW body invent  
- Shipping Filament / HDRI / glTF libraries in the **base** APK  
- Claiming bit-identical Avatar finals on phone  

---

## Exit for Wave H

1. One hero project: tracked subject + glTF prop + IBL + GPU particles + depth soft-comp, preview ≈ export  
2. `:feature_cg` (or extended `:feature_titles3d`) installable on demand; base APK size unchanged within noise  
3. Matrix rows for H1–H8 marked LIVE/PACK with honest gates  
4. Mid-range device smoke + Maestro yellows documented  

When this plan drifts, update this file and `docs/REMAINING_MASTER_PLAN.md` / `docs/TOTAL_REMAINING.txt` in the same change.
