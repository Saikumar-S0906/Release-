# PixiJoy — Remaining master plan (B→G)

**Authority:** Client roadmap 2026-09-23 + [`TOTAL_REMAINING.txt`](TOTAL_REMAINING.txt) + [`NORTH_STAR_ROADMAP.md`](NORTH_STAR_ROADMAP.md)  
**Hard rule:** edit-graph or timeline op → engine (`:photo` / `:movie` / `:ml`) → WorkManager export. Local-first. No fake Maestro greens.  
**Branch:** land on `origin/next` after review-clear.  
**Excluded:** Section A (done/exited) — do not reopen.

**Snapshot (2026-09-27):** **Wave B EXITED** · **Wave C EXITED** · **D1–D11 MET** · Wave D EXITED. Live: **E pack runtime + Super-res/Denoise HTTPS LiteRT + inpaint sideload bind + E4 FILM bind/export mux + F1 Track bundled APK asset + LiteRT bind + TrackPathBake follow + F2 classical particle VFX overlays**. G2 slider/preview extract complete.

```
B → C → D → E → F ──▶ **CODING P0–P6** (docs/CODING_PHASES.md) ──▶ **P7 ML plugs**
                              │
         Wave H–L product shape + Wave M Colab (never blocks coding)
                              │
                              └──── G parallel (never blocks exits) ────┘

Director kit: docs/DIRECTOR_KIT.md  ·  Perfect: docs/PERFECT_BUILD.md  ·  Coding: docs/CODING_PHASES.md
```

---

## 0. Completion bar (every slice)

1. Review-clear  
2. Relevant units green (`:photo` / `:movie` / `:ml` / `:app` as needed)  
3. Honest Pixel_7a Maestro — or yellow/manual with a short doc  
4. Status docs flipped in the same landing (`HANDS_OFF`, `TOTAL_REMAINING`, op-shape notes)  
5. No probe YAMLs, assemble logs, or `_hsl_pkg` mirrors in feature commits  

---

## Wave B — Close quality gates and R3.1 UX

| ID | Work | State | Exit criteria |
|----|------|-------|---------------|
| **B1** | Selective HSL | **EXITED green** Pixel_7a | Maestro `selective-hsl-apply.yaml` green 2026-09-23 |
| **B2** | Compare (live peel + pre-bake) | **EXITED** | Maestro `compare-hold.yaml` green Pixel_7a |
| **B3** | R3.1 UX | **EXITED** | Library/Export/Recommended/Create/movie glass landed; Maestro green |
| **B4** | Color/Looks live preview | Measure-deferred | Profile Pixel_7a; downscale **only if** jank; else leave closed |
| **B5** | Docs on `origin/next` | **EXITED** with Phase 0 land | Status docs in same commit |
| **B6** | Gallery Photo Picker | Yellow/manual | Keep documented; no fake green |

**Immediate next:** interp / track engines only with real movie inference; device Maestro/TalkBack/five-user remains the R5 quality gate.

**G overlap with B:** **G4** exited with B3 (chips ≥48dp, announced selection). Studio TalkBack chrome descriptions are in; the device TalkBack pass stays yellow.

---

## Wave C — Finish core R3 photo depth

Dependency order is fixed. Each op: graph letter → `:photo` engine → session UI under glass scrim → WorkManager export parity → Maestro.

| ID | Slice | Depends on | Deliverables | Notes |
|----|-------|------------|--------------|-------|
| **C1** | LUT import | B3 stable | `.cube` + `.3dl`; project `luts/`; `lut:` keys; Looks Import; export; units; picker yellow/manual | **MET 2026-09-23** |
| **C2** | RAW/DNG | C1 | Open/metadata/preview sidecar; edit on preview; develop track later | **MET path** (develop later) |
| **C3** | Content-aware Fill polish | Layers/masks | Better mask UX, quality, undo; **edit-graph contract stable for E1** | **MET** undo stroke + clear; Telea; LiteRt stub |
| **C4** | Sky / background replace | C3 masks + Subject | Op + ML/heuristic sky matte; preview/export | **MET 2026-09-23** (heuristic; Maestro green) |
| **C5** | Perspective + liquify | Stable warp pipeline | Two ops; live preview budget; export | **MET** graph D/M; WarpOps; session Warp |
| **C6** | Presets + batch export | C1–C5 ops serializable | Preset encode/decode; multi-project WorkManager batch | **MET** PhotoPreset + BatchPhotoExportWorker |
| **C7** | Wide-gamut / color mgmt | Decode/export path | Display P3 awareness; export color intent | **MET** best-effort ColorIntent on export |
| **C8** | Portrait retouch | — | **Lightweight** tools may late-R3; **model-heavy** → R5 pack | **Light Soften MET** (O); heavy → R5 |

**Intentional overlap:** C3 = polished fill UX + graph contract; **E1** = optional generative inpaint engine that plugs into C3.

---

## Wave D — R4 movie foundation, then cinema

**Rule:** do not build D2–D11 effects on a throwaway single-track model. Finish **D1** (data + preview composition + persistence + export) first.

| ID | Slice | Depends on | Deliverables |
|----|-------|------------|--------------|
| **D1** | Multi-track V/A | B3 | Promote `MultiTrackTimeline` to source of truth; preview composition; persistence; export match | **MET 2026-09-23** composition E2E + track mute UI |
| **D2** | Ripple/roll; transitions; speed ramps | D1 | Edit ops on tracks; Maestro trim/transition | **MET 2026-09-23** rollCut + Soft fades + expandRamp; Maestro `d2-transition-speed` green Pixel_7a |
| **D3** | Keyframed transform / motion | D1 | Keyframe model + preview + export | **MET 2026-09-23** ClipMotion + MatrixTransformation; Maestro `d3-keyframed-motion` green Pixel_7a |
| **D4** | Titles + captions | D3 | Text layers on timeline | **MET 2026-09-23** TimelineTitle + OverlayEffect; Maestro `d4-titles-captions` green Pixel_7a |
| **D5** | Grade + LUT + scopes | D1, C1 | Timeline grade; scopes UI | **MET 2026-09-23** TimelineGrade + SingleColorLut + scopes meters; Maestro `d5-grade-scopes` green Pixel_7a |
| **D6** | Audio finish | D1 | Ducking polish, VO UX beyond current stems | **MET 2026-09-23** releaseMs + Soft/Med/Hard presets; Maestro `d6-audio-finish` green Pixel_7a |
| **D7** | PiP / multicam | D1, D3 | Multi-video layout | **MET 2026-09-23** VideoLayout + VideoCompositorSettings; Maestro `d7-pip-layout` green Pixel_7a |
| **D8** | Stabilize + reframe | D1 | Op + export | **MET 2026-09-23** ClipFrame + Crop/zoom; Maestro `d8-stabilize-reframe` green Pixel_7a |
| **D9** | Chroma key | D1, masks | Keyer op | **MET 2026-09-23** ClipChroma + ChromaKeyEffect; Maestro `d9-chroma-key` green Pixel_7a |
| **D10** | Frame-interp slow-mo UX | D1, speed model | Timeline/export **contract** for slow-mo | **MET 2026-09-23** ClipSlowMo + FrameInterp; Maestro `d10-slowmo-interp` green Pixel_7a |
| **D11** | HDR export | D1, C7 | Beyond HD/FHD/4K SDR | **MET 2026-09-23** MovieHdrMode + Composition.setHdrMode; Maestro `d11-hdr-export` green Pixel_7a |

**Intentional overlap:** D10 = movie slow-mo UX + timeline/export contract; **E4** = optional ML interpolation pack behind D10.

---

## Wave E — R5 ML packs (productization, not extras)

**Before E1:** ship shared **ML-pack foundation** (blocking prerequisite for all E*):

- Pack availability + download/install state machine  
- Model version + checksum  
- Storage error handling  
- Graceful “feature unavailable until downloaded” UI  
- Play Feature Delivery / on-demand module wiring  
- Processing remains **local after install**  

Then:

| ID | Pack | Prerequisite | Delivery |
|----|------|--------------|----------|
| **E5** | Base APK ≤ ~40 MB | With pack foundation | `:ml` pack runtime + `reportApkSize`; `selfie_segmenter` (~244 KB) stays in base for MediaPipe only (not a pack engine). Remeasured tip `03fb2ea` minified unsigned APK **54.80 MB** (131.11 MB does not match). Release arm64+armeabi-v7a filter measured **33.08 MB** (−21.72). Phone MediaPipe JNI and LiteRT client stay. Extended icons were already gone from dex. Pack models stay out. E4/F1 weights stay off-APK |
| **E1** | Inpaint / generative fill | C3 + pack foundation | Missing CTA is Install from file (no HTTPS pin, no Download). Ready file ≠ neural. Neural Apply uses `engineReady` after RGB+mask FLOAT32 sideload bind. Nearby-pixel Telea stays on and is not neural |
| **E2** | Super-resolution | Pack runtime | Graph `SR`; HTTPS ESRGAN SHA pin → Ready → FLOAT32 2×/4× NHWC → LiteRT. Apply uses `engineReady` (Ready alone is not neural) |
| **E3** | Denoise | Same runtime | Graph `N`; HTTPS UNet SHA pin → Ready → FLOAT32 same-size RGB → LiteRT. Apply uses `engineReady` |
| **E4** | Frame interpolation | D10 + pack runtime | `FrameInterp.mlAvailable` ← pack engine; D10 CTA. **MET** — HTTPS FILM pin + LiteRT bind + export `NeuralSlowMoMux`. Preview stays rate/hold. |

---

## Wave F — R6+ opt-in (after primitives)

| ID | Feature | Depends on |
|----|---------|------------|
| **F1** | Tracking / roto-lite | D1, D3. Bundled `track.tflite` in APK assets + LiteRT bind + `TrackPathBake` follow when `engineReady` |
| **F2** | VFX / particles | **MET** — classical `TimelineVfx` composition overlays + optional F1 track follow. Not neural. |
| **F3** | 3D / Filament titles | **MET** — `TimelineTitle3d` + extruded overlay in base; Filament in on-demand `:feature_titles3d` |
| **F4** | ARCore previz | **MET** — `ArPrevizGate` + `:feature_ar` plane previz Activity (device-gated) |
| **F5** | Optional FFmpeg export | **MET** — Media3 default; `:feature_ffmpeg` + `MovieExportBackends` for GIF/WebM gaps |
| **F6** | Smart objects, advanced type, print/PDF | **MET** — `SmartObject` / `AdvancedType` / `PdfExport` in `:photo` |

Never land F* as isolated effects without listed primitives.

---

## Wave H — R7 on-device CG (after F foundations)

**Plan detail:** [`docs/R7_CG_EFFECTS.md`](R7_CG_EFFECTS.md) · **Director kit:** [`docs/DIRECTOR_KIT.md`](DIRECTOR_KIT.md).  
Real Filament CG + depth/matte inserts vs CapCut sticker VFX. Not a Hollywood farm; director-grade mobile CG previz.

| ID | Feature | Notes |
|----|---------|--------|
| **H1** | CG compositor lane | **PARTIAL** — PropGltf offscreen Filament in on-demand `:feature_cg` (directional + procedural IBL). Other plates stay classical. Preview and export share `CgOverlays`. |
| **H2** | glTF prop library | Release- asset packs; place / scale / track-attach |
| **H3** | Lights + IBL | **PARTIAL** — procedural IBL on PropGltf; point + Release- HDRI + grade continuity open |
| **H4** | GPU particles / trails | Replaces cheap sparkle for CG shots |
| **H5** | Depth-aware insert | Monocular depth ML + soft Z-comp |
| **H6** | Video matting plates | Temporal subject matte for CG underlays |
| **H7** | CG camera + motion blur | Keyframed dolly/orbit on CG layer |
| **H8** | Set extension / sky dome | HDRI dome + shadow catcher |
| **H9** | Audio-reactive CG (opt) | Local amplitude → lights/particles |
| **H10** | Perf + size gate | **MET** — on-demand `:feature_cg`; `reportH10SizeGate` + mid-range budgets in [`H10_PERF_SIZE_GATE.md`](H10_PERF_SIZE_GATE.md) |

---

## Wave I — R8 assembly (director editorial)

**Plan detail:** [`docs/DIRECTOR_KIT.md`](DIRECTOR_KIT.md) §5. Parallel-safe with early H once D1–D7 stable.

| ID | Feature | Notes |
|----|---------|--------|
| **I1** | Markers + notes | Beat / VFX / music markers; jump-to |
| **I2** | Bins + selects | A/B/selects/VFX plate bins |
| **I3** | Versions / assemblies | Named cuts; compare flip; tagged export |
| **I4** | Snap edit polish | Magnetic edges, J/L, ripple defaults |
| **I5** | Proxy scrub | Half-res scrub; full-res export |
| **I6** | Multicam polish | Angle switch + clap/waveform sync (honest yellow OK) |

---

## Wave J — R8 colorist kit

**Plan detail:** [`docs/DIRECTOR_KIT.md`](DIRECTOR_KIT.md) §6. Builds on D5 + C1.

| ID | Feature | Notes |
|----|---------|--------|
| **J1** | Secondary / power windows | Shape + HSL qualify on timeline |
| **J2** | Shot match | Copy grade + still-reference assist |
| **J3** | LUT pipeline order | Show → look → technical; continuity with H3 |
| **J4** | Scopes truth | Display-referred; HDR path labeled |
| **J5** | Still lookbook bridge | Frame → photo grade → LUT back |

---

## Wave K — R8 sound finish

**Plan detail:** [`docs/DIRECTOR_KIT.md`](DIRECTOR_KIT.md) §7. Builds on D6.

| ID | Feature | Notes |
|----|---------|--------|
| **K1** | Stem buses | Dialogue / music / SFX mute/solo |
| **K2** | ADR / VO lane | Record into timeline |
| **K3** | Sync tools | Slip + waveform; clap detect optional |
| **K4** | Loudness | LUFS target + soft limiter best-effort |
| **K5** | Spot FX to picture | Marker drop; optional track attach |

---

## Wave L — R9 delivery & review

**Plan detail:** [`docs/DIRECTOR_KIT.md`](DIRECTOR_KIT.md) §9.

| ID | Feature | Notes |
|----|---------|--------|
| **L1** | Review package | Optional TC burn-in; local comment JSON; watermark off by default |
| **L2** | Handoff | EDL-lite / FCPXML-lite + markers; document gaps |
| **L3** | Deliverable presets | Social / review proxy / master HDR |
| **L4** | Export metadata | Title, version, LUT embedded |

---

## Wave M — Colab ML packs (parallel; do not serialize on inpaint)

**Plan detail:** [`docs/PERFECT_BUILD.md`](PERFECT_BUILD.md) §1 · runbook [`tools/COLAB_ML_RUNBOOK.md`](../tools/COLAB_ML_RUNBOOK.md). Train on Colab → TFLite → [Release-](https://github.com/Saikumar-S0906/Release-) → LiteRT bind. Labs ready: `tools/inpaint/`, `tools/depth/`, `tools/matting/`, `tools/flow/` (+ `.\tools\pack_ml_labs.ps1`).

| ID | Pack | Unlocks | Notes |
|----|------|---------|--------|
| **M1** | Inpaint | E1 neural Apply | Keep Colab running; pin SHA + Download when `inpaint.tflite` lands |
| **M2** | Depth (monocular) | H5 soft Z-comp | **Start now** — independent of M1 |
| **M3** | Video matting | H6 plates / set extension | **Start now** — independent of M1 |
| **M4** | Optical flow / stabilize | D8 beyond crop-zoom; cleaner H2/H4 attach | Parallel, or after M2/M3 if one GPU |
| **M5** | Roto upgrade (opt) | F1 quality | Only if point-track gaps hurt |
| **M6** | Portrait heavy (opt) | C8 pack | Keep light Soften |
| **M7** | Sky matte (opt) | C4 upgrade | If heuristic fails on film |

**Already shipped (no Colab unless upgrade):** Track, Super-res, Denoise, FILM.

**Quota tip:** 2nd Colab for Depth while Inpaint trains; 3rd for Matting if available. App code for H1–H3 can proceed without any of M1–M4.

---

## Wave G — Platform / polish (parallel, non-blocking)

| ID | Work | When |
|----|------|------|
| **G4** | Accessibility | **With B3** (must-exit). Studio TalkBack names: short verbs, "Looks, selected", "Crop edge, {side}", export View/Share/Done, "Downloaded" vs "Ready to apply". Landed 2026-09-25. Device TalkBack stays yellow (`.maestro/YELLOW-talkback-studio.md`) |
| **G1** | Tablet/foldable two-pane | **MET** catalog\|session + session preview\|tools 2026-09-25 |
| **G2** | Photo/Movie ViewModels | **Slider/preview extract complete** 2026-09-25 — Hilt session VMs own graph, tool chrome, and slider/preview baking. Screens observe that state. Movie players stay on the screen. Tool chrome soft-resets on entry, rotation, and tool switch; the edit graph stays. |
| **G3** | Hilt in StudioApp | **MET** 2026-09-25 — one `StudioGraphModule`; shell navigates and observes injected engines; composition preview and export workers no longer construct engines |
| **G5** | Haptics | **MET** crop Apply + movie split 2026-09-25; crop-handle edge snap + haptic 2026-09-25 (unit-tested, device drag yellow) |
| **G6** | Skeleton Library thumbs | **MET** 2026-09-25 — cover sheen while Coil decodes; still surface if the file fails. |
| **G7** | Empty-state illustrations | **MET** 2026-09-25 — one glass plate (`PixiEmptyState`): `empty_library` “No projects yet” + Create; hosted `empty_packs` “Download to unlock” + Download. Sideload-only packs (Inpaint) show Install from file on that plate. Neural copy only when `engineReady` (Ready alone is the file). |
| **G8** | Autosave + rename-after-create/export | **MET** 2026-09-25 — session chip follows graph writes; library Saved age; filename or dated title; tap title or Name this project after export |
| **G9** | BuildConfig cleanup | **MET** 2026-09-25 |

---

## Suggested delivery calendar (quality > clock)

| Phase | Focus | Parallel G |
|-------|-------|------------|
| **P0** | **DONE** — Wave B exit + C1–C3 foundations + B1 green | G4 with B3 |
| **P1** | **DONE** — C4 sky + Wave C foundations | G1 start; G8 rename |
| **P2** | **DONE** — C5–C8 Wave C exit | G2/G3 gradual |
| **P3** | **DONE** — D1 composition E2E | G5–G7 |
| **P4** | D2–D6 cinema core | — |
| **P5** | D7–D11 | — |
| **P6** | ML-pack foundation + E5 size gate → E1 → E2/E3 → E4 | — |
| **P7** | F1→F6 as opt-in modules | — |
| **P8** | Wave H / R7 CG (H1→H8) after F foundations | CODING_PHASES P1 + P5 |
| **P9** | Waves I–K / R8 assembly + color + sound | CODING_PHASES P2–P4 |
| **P10** | Wave L / R9 delivery & review handoff | CODING_PHASES P6 |
| **P-ML** | Wave M Colab + CODING_PHASES **P7** bind | never blocks P0–P6 |
| **P-UX** | CODING_PHASES **P0** tool find + status chips | first coding slice |

---

## Module ownership (unchanged)

| Concern | Module |
|---------|--------|
| Decode / EXIF / color / IO | `:core:media` |
| Still ops + photo session | `:photo` |
| Timeline / Media3 / movie export | `:movie` |
| On-device models + packs | `:ml` (+ Release- hosts for large TFLite) |
| Filament / CG / AR / FFmpeg | on-demand `:feature_*` modules (not base APK) |
| Adaptive chrome | `:app` + `:designsystem` |

---

## Explicit non-goals / anti-patterns

- Do not reopen A (pending-URI, movie `runBlocking`, exited R2 slices)  
- Do not speculative-optimize Color/Looks without Pixel_7a evidence (B4)  
- Do not ship generative fill before C3 contract (E1 after C3)  
- Do not build cinema effects on single-track forever (D2+ after D1 E2E)  
- Do not put FFmpeg / Filament / ARCore / FILM (~67 MB) in the base APK (on-demand modules or Release Download). Small TFLite packs (Track / Super-res / Denoise / Inpaint-if-≤~3 MB) may ship as assets.  
- Do not block feature exits on full ViewModel rewrite (G2/G3 parallel)

---

## Local hygiene — 2026-10-03 (not an exit)

Code and unit tests only. No device, no Maestro green, no `result: PASS`, and the project stays **not green**. `targetSdk` stays 35. `TOTAL_REMAINING.txt` and `HANDS_OFF.md` were not edited. Telea and manual Z stay the fallbacks until a real `engineReady` result.

| Item | What landed | What it is not |
|------|-------------|----------------|
| Application id | `com.pixijoy.studio` (`versionName` `2.0.0-studio`) | Not an update of `com.pixijoy.ai`. Play treats it as a new app. Install is side-by-side. Project data does not move. |
| Legal | Privacy policy and terms are in the app. No website is opened or stored. | Not a law-firm opinion. Play Console still requires a public privacy-policy URL at submit time. Play has not approved a listing. |
| Media permissions | `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` removed. Gallery uses the system picker. Export inserts into MediaStore. Camera, microphone, notifications, internet, and the export foreground service stay. | Not a claim that Play Data safety is filled in. |
| Backup | `allowBackup=false`. API 31+ cloud backup and device transfer exclude every domain (root, file, database, sharedpref, external). Pre-31 `backup_rules.xml` matches. | Not a phone-to-phone project move. Projects were already excluded; models, the database sidecars, and DataStore prefs are now excluded too. |
| File provider | Cache and external cache only. | Nothing in the app requests a share URI today. Project folders stay private. |
| Import | A failed new import deletes the folder and any partial project row. An empty incoming file is an error. LUT and audio replace keep the previous file if the new copy fails. Prop names never reuse an existing file. | Not a multi-gigabyte size cap. LUT and prop stay at 8 MB. |
| Photo export | A color-intent copy and the original bitmap are both recycled. | Not a color-management certification. |
| Same tree, local test | Live bed/voice volume, GIF resolution chip, 4-up collage drawn as 2×2, fold/tablet/phone chrome. | Not a visual device pass. |

Debug APK for a local install is `app/build/outputs/apk/debug/app-debug.apk` after `:app:assembleDebug`. It is debug-signed and multi-ABI. It is not a Play artifact and not a quality gate.

---

## Next concrete slice (start here)

1. Interp / track engines only after Architect publishes a real artifact plus catalog `downloadUrl` / SHA and a `PackModelContract` gate. Until then leave the `Unit` no-op. `PackFeatureEntry.registerEngines` after SplitCompat, Ready + SHA-256, and `PackModelContract`. Do not register from `Application.onCreate`. Do not register Telea / bicubic / frame-hold
2. R5 device gate (Maestro + TalkBack + five-user) on a mid-range phone

When this plan drifts, update this file and `TOTAL_REMAINING.txt` in the same commit as the exit. This hygiene note is not an exit, so `TOTAL_REMAINING.txt` stays untouched.
