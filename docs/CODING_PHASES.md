# PixiJoy — Coding phases (do not wait on Colab)

**Rule:** Code everything that is **engine + UX + timeline**. ML packs plug in later via `PackId` + `engineReady`. Classical / Filament / editorial work never blocks on TFLite.

**Pairs with:** [`DIRECTOR_KIT.md`](DIRECTOR_KIT.md) · [`PERFECT_BUILD.md`](PERFECT_BUILD.md) · [`R7_CG_EFFECTS.md`](R7_CG_EFFECTS.md) · [`REMAINING_MASTER_PLAN.md`](REMAINING_MASTER_PLAN.md)  
**Colab (parallel, your queue):** [`tools/COLAB_ML_RUNBOOK.md`](../tools/COLAB_ML_RUNBOOK.md)

```
YOU (Colab, anytime)          WE (Kotlin coding now)
─────────────────────         ──────────────────────
M1 inpaint keep running  ──▶  Phase 0 wiring stubs
M2→M3→M4 one-by-one      ──▶  Phases 1–6 full product
                              Phase 7 bind when .tflite lands
```

---

## What coding solves without ML

| Need | Code path (no Colab) | ML later (optional upgrade) |
|------|----------------------|-----------------------------|
| CG / 3D / lights / props | Filament `:feature_cg`, glTF, IBL, particles | — |
| Assembly / markers / versions | Timeline encode + UI | — |
| Color secondary / scopes / LUT order | Grade ops + UI | — |
| Stems / ADR / loudness / slip | Audio graph + UI | — |
| Review / EDL / presets | Export + files | — |
| Tool find ≤2 taps / status chips | Designsystem + session | — |
| Stabilize (basic) | Motion / crop heuristics (improve D8) | M4 flow |
| Soft Z / plates | **Flat insert + chroma/manual mask first** | M2 depth, M3 matte |
| Continuity remove | Telea (already) | M1 inpaint |
| Track attach | F1 point track (shipped) | M5 roto opt |

**Honesty:** Depth-aware and temporal matting look best with ML — but H1–H4, I, J, K, L, U ship **fully** without waiting.

---

## Phase overview

| Phase | Name | ML wait? | Exit | Status |
|-------|------|----------|------|--------|
| **P0** | UX spine + honesty chips | No | Tools ≤2 taps; Live/Pack/Export-only | **LANDED** — search + CG/Deliver + capability chip |
| **P1** | CG foundation H1–H3 | No | Filament lane + glTF + IBL; preview≈export | **PARTIAL** — classical plates LIVE; glTF place/scale LIVE; on-demand `:feature_cg` PropGltf with directional key + procedural IBL when the split binds (classical raster if it does not). Release- HDRI still next |
| **P2** | Assembly I1–I4 | No | Markers, bins, versions, snap | **LANDED** — markers/bins/versions/J-L/snap |
| **P3** | Color J1–J4 | No | Secondary, shot match, scopes truth | **LANDED** — secondary + match + pipeline/scopes labels |
| **P4** | Sound K1–K4 | No | Stems, ADR, sync, LUFS | **LANDED** — buses + ADR + slip + LUFS gate |
| **P5** | CG polish H4,H7,H8 + classical H5/H6 fallbacks | No* | GPU particles, camera, set ext; flat Z/matte until ML | **PARTIAL** — Z/matte/cam keys + dust drift/motion-blur crumbs + F1 track-attach + **H10 size/perf gate MET**; Filament GPU later |
| **P6** | Delivery L1–L4 | No | Review package + EDL-lite + presets | **LANDED** — presets · TC · notes · EDL/FCPXML · meta sidecar |
| **P7** | ML bind plugs | **Yes — only this phase** | Pin M1–M4 when files exist; swap fallbacks | Planned |

\*P5 ships classical fallbacks; P7 swaps in neural when ready.

Suggested coding calendar: **P0 → P1 → P2 → P3 → P4 → P5 → P6**, with **P7** whenever each `.tflite` lands (can interleave).

---

## Phase 0 — UX spine (U1–U3, U7)

**Goal:** Directors find every tool without hunting; never overclaim neural.

| ID | Work | Module |
|----|------|--------|
| P0.1 | Tool categories: Edit / Color / Sound / VFX / CG / Deliver (+ search) | `:app` session |
| P0.2 | Status chip: Live · Needs pack · Export-only | `:designsystem` |
| P0.3 | Same Apply / Undo / Compare affordances photo+movie | `:app` |
| P0.4 | Pack empty states already honest — audit CG/VFX copy | `:app` / strings |

**Exit:** Maestro “open Grade / Track / Titles3d in ≤2 taps”; no “neural” without `engineReady`.

---

## Phase 1 — CG foundation (H1–H3) — coding only

| ID | Work | Notes |
|----|------|--------|
| P1.1 | `TimelineCgLayer` encode/decode on `MultiTrackTimeline` | Versioned lane |
| P1.2 | `:feature_cg` Filament offscreen → overlay | SplitInstall. PropGltf, directional + procedural IBL (no HDRI asset yet). Classical raster if the split is missing |
| P1.3 | Shared preview + export bake path | preview ≈ export |
| P1.4 | glTF load/place/scale/yaw; 5–10 CC0 props on Release- | H2 |
| P1.5 | Directional + procedural IBL (HDRI later) | H3 PARTIAL; Release- HDRI + point lights + grade continuity still open |

**Exit:** Hero still: video plate + lit glTF prop, export matches preview. **No depth ML required.**

---

## Phase 2 — Assembly (I1–I4)

| ID | Work |
|----|------|
| P2.1 | Markers + notes (beat / VFX / music); jump-to |
| P2.2 | Bins + selects persist with project |
| P2.3 | Named timeline versions; duplicate; compare flip |
| P2.4 | Magnetic snap + J/L cut UX + ripple defaults |

**Exit:** Unit round-trip + Maestro add marker / switch version. Optional later: I5 proxy, I6 multicam polish.

---

## Phase 3 — Colorist kit (J1–J4)

| ID | Work |
|----|------|
| P3.1 | Secondary / power windows (shape + HSL qualify) |
| P3.2 | Copy grade + shot-match assist (histogram/mean — **code**, not ML) |
| P3.3 | Documented LUT order: show → look → technical |
| P3.4 | Scopes truth labels (display-referred; HDR path named) |

**Exit:** Preview ≈ export on secondary; Maestro scopes visible. J5 lookbook bridge can follow.

---

## Phase 4 — Sound finish (K1–K4)

| ID | Work |
|----|------|
| P4.1 | Stem buses: Dialogue / Music / SFX mute/solo |
| P4.2 | ADR / VO record lane into timeline |
| P4.3 | Slip + waveform align (clap detect = heuristic code OK) |
| P4.4 | LUFS target meter + soft limiter best-effort |

**Exit:** Mix pane + export respects stems/LUFS gate. K5 spot FX optional with F1 track.

---

## Phase 5 — CG polish + classical fallbacks (H4, H7, H8, H5/H6 code)

| ID | Work | ML? |
|----|------|-----|
| P5.1 | GPU particles / trails (Filament/compute) | No |
| P5.2 | CG camera keyframes + motion blur on CG layer; F1 classical track-attach along shot | No |
| P5.3 | Set extension / sky dome + shadow catcher | No |
| P5.4 | **H5 classical:** manual Z-order / track-plane insert (CG behind/over subject by layer) | ML later |
| P5.5 | **H6 classical:** chroma key / brush matte / MediaPipe selfie matte for plates | ML later |
| P5.6 | H10 size/perf gate mid-range (**MET** — `reportH10SizeGate` + [H10_PERF_SIZE_GATE.md](H10_PERF_SIZE_GATE.md)) | No |

**Exit:** Hero reel without neural depth/matte — still looks like CG, not stickers. Matrix labels classical vs pack honestly.

---

## Phase 6 — Delivery (L1–L4)

| ID | Work |
|----|------|
| P6.1 | Review package: optional TC; local comment JSON; watermark **off** by default |
| P6.2 | EDL-lite / FCPXML-lite + markers (document gaps) |
| P6.3 | Deliverable presets: Social / Review / Master HDR |
| P6.4 | Export metadata: title, version, LUT |

**Exit:** Handoff file opens in one desktop NLE smoke (or documented limit).

---

## Phase 7 — ML plugs (only when `.tflite` ready)

Do **not** block Phases 0–6. Each plug is a thin bind:

| When file lands | Code work |
|-----------------|-----------|
| `inpaint.tflite` | E1 HTTPS/SHA pin + Download CTA (Telea stays fallback) |
| `depth.tflite` | PackId.Depth + LiteRT + soft Z replace P5.4 |
| `matting.tflite` | PackId.Matting + LiteRT + plates replace P5.5 |
| `flow.tflite` | PackId.Flow + LiteRT + stabilize/attach assist |

Contract already frozen in `tools/depth|matting|flow`. App: `PackModelContract` + `PackLiteRt` + CTA + Maestro.

---

## Parallelism cheat sheet

```
Week coding focus          Your Colab
─────────────────          ──────────
P0 UX                      M1 finish / upload
P1 CG H1–H3                M2 depth train
P2 Assembly                M3 matting train
P3 Color                   M4 flow train
P4 Sound                   —
P5 CG polish + fallbacks   —
P6 Delivery                —
P7 bind as each file lands interleaved anytime
```

---

## Non-goals for coding phases

- Waiting idle for Colab before starting P0–P6  
- Claiming neural depth/matte before P7 bind  
- Shipping Filament/HDRI/glTF in **base** APK  
- Path-traced farm / Atmos suite on phone  

---

## Phase exit = shippable increment

Each phase: review-clear · units · honest Maestro/yellow · matrix + TOTAL_REMAINING flipped in same land.

When this drifts, update with `DIRECTOR_KIT.md` / `REMAINING_MASTER_PLAN.md` / `TOTAL_REMAINING.txt` in the same change.
