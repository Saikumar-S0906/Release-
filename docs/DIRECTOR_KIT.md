# PixiJoy — Director kit (Hollywood-class mobile edits)

**Status:** PLANNED north-star product shape (after Wave F / alongside Wave H)  
**Pairs with:** `docs/R7_CG_EFFECTS.md`, `docs/REMAINING_MASTER_PLAN.md` Waves H–L, `docs/NORTH_STAR_ROADMAP.md`  
**Promise:** A director can **cut, look-dev, sound-sketch, VFX-previz, and review** a scene on phone — local-first, no watermark, no upload — then **hand off cleanly** to DI / VFX / Avid when the farm is needed.  
**Build checklist (Colab ML + engines + UX):** [`docs/PERFECT_BUILD.md`](PERFECT_BUILD.md).  
**Coding now (do not wait on Colab):** [`docs/CODING_PHASES.md`](CODING_PHASES.md) — P0 UX → P1 CG → P2 Assembly → P3 Color → P4 Sound → P5 CG polish → P6 Delivery → P7 ML plugs.

**Honesty bar:** PixiJoy is not a replacement for a Resolve DI suite, Nuke farm, or theatrical mix stage. It is the **director’s always-on cutting room**: trustworthy assemblies, cinema-grade mobile CG, honest scopes, and deliverables that survive conform.

---

## 1. Who this is for

| Persona | What they need on the plane / set / hotel |
|---------|-------------------------------------------|
| Feature / series director | Shape performance, pace, coverage; lock a cut before the room |
| DP / look lead | Grade sketches, LUT, scopes, stills continuity |
| VFX supervisor (light) | Previz inserts, track, plates, set extension — not final beauty |
| Editor (first pass) | Multicam, markers, versions, audio sketch, export for review |

**Not for:** generative undress / NSFW invent; cloud “magic edit”; CapCut template scrolling as craft.

---

## 2. Director pillars (must all feel professional)

```
        ┌─────────────┐
        │  ASSEMBLY   │  bins · markers · versions · multicam · ripple
        └──────┬──────┘
               │
   ┌───────────┼───────────┬──────────────┬─────────────┐
   ▼           ▼           ▼              ▼             ▼
 COLOR      SOUND        VFX/CG      CONTINUITY     DELIVERY
 grade      stems        Filament    plates         review
 scopes     duck/VO      track/matte inpaint        handoff
 LUT        loudness     depth/set   stills look    metadata
```

Every pillar stays: **timeline/op → engine → WorkManager export**. UI never owns giant bitmaps.

---

## 3. Competitive wedge (director chooses PixiJoy)

| CapCut / VN / Canva Video | Premiere Rush / LumaFusion | PixiJoy Director kit |
|---------------------------|----------------------------|----------------------|
| Template + sticker first | Solid cut, thin CG | **Director timeline + Filament CG lane** |
| Flat overlay VFX | Limited 3D | Depth/matte-aware inserts, glTF, IBL |
| Cloud tricks | Account friction | **On-device only** on editor path |
| Social export | Consumer delivery | Review notes + **conform-friendly** handoff |
| No scopes culture | Basic color | Grade + LUT + **honest scopes** |

---

## 4. Wave map (after F foundations)

| Wave | Stage | Focus | Detail |
|------|-------|-------|--------|
| **H** | **R7** | On-device CG / cinema VFX | [`R7_CG_EFFECTS.md`](R7_CG_EFFECTS.md) H1–H10 |
| **I** | **R8** | Assembly / editorial finish | Markers, bins, versions, snap edit, proxy scrub |
| **J** | **R8** | Colorist kit | Secondary grade, shot match, LUT pipeline polish, scopes truth |
| **K** | **R8** | Sound finish | Stem buses, ADR lane, sync tools, loudness target |
| **L** | **R9** | Delivery & review | Review comments, handoff XML/EDL-lite, deliverable presets |

Suggested order: finish **F leftovers → H1–H3 early CG → I1–I3 assembly** in parallel with H depth; then **J → K → H4–H8 → L**. Color and sound make CG look “film,” not game.  
**Execute as coding phases:** [`CODING_PHASES.md`](CODING_PHASES.md) (P0–P6 code-first; P7 ML bind when TFLite ready).

---

## 5. Wave I — Assembly (director editorial)

| ID | Slice | Deliverable | Exit |
|----|-------|-------------|------|
| **I1** | Markers + notes | Timeline markers (beat / soft / hard / VFX / music); note text; jump-to | Maestro add/jump; encode round-trip |
| **I2** | Bins + selects | Project bins (A-cam / B-cam / selects / VFX plates); drag clip to bin | Persist with project |
| **I3** | Versions / assemblies | Named cut versions (`v1_assembly`, `director_alt`); duplicate timeline; compare flip | Two versions open; export tagged |
| **I4** | Snap edit polish | Magnetic edges, J/L cut UX, ripple defaults directors expect | Unit + Maestro trim |
| **I5** | Proxy / performance scrub | Optional half-res proxy for long 4K timelines; full-res on export | Mid-range 30 min scrub budget |
| **I6** | Multicam polish | Angle switching beyond D7 PiP; sync by clap/waveform heuristic | Sync yellow/manual OK if honest |

---

## 6. Wave J — Color (look the director trusts)

| ID | Slice | Deliverable | Exit |
|----|-------|-------------|------|
| **J1** | Secondary / power windows | Shape + HSL qualify on timeline grade (beyond global D5) | Preview ≈ export |
| **J2** | Shot match | Copy grade; still-reference match assist (histogram / mean) | Side-by-side Maestro |
| **J3** | LUT pipeline | Show → look → technical LUT order documented; import stays C1 | Grade continuity with H3 IBL |
| **J4** | Scopes truth | Waveform / vectorscope / histogram tied to **display-referred** preview; HDR path labeled | Doc + Maestro scopes visible |
| **J5** | Still lookbook bridge | Send movie frame → photo project with grade ops; back to LUT | Round-trip unit |

---

## 7. Wave K — Sound (temp mix directors accept)

| ID | Slice | Deliverable | Exit |
|----|-------|-------------|------|
| **K1** | Stem buses | Dialogue / music / SFX buses + group mute/solo | Mix pane + export |
| **K2** | ADR / VO lane | Dedicated VO record into timeline; slate name | Mic permission + encode |
| **K3** | Sync tools | Slip audio; waveform align; optional clap detect | Manual OK if documented |
| **K4** | Loudness | Target LUFS meter (e.g. −14 / −16 / −24 selectable); soft limiter | Export respects target best-effort |
| **K5** | Spot FX to picture | Drop SFX on marker; optional F1 track attach for whoosh | One hero demo |

---

## 8. Wave H — CG (recap; full table in R7 doc)

H1 compositor → H2 glTF → H3 lights/IBL → H4 GPU particles → H5 depth → H6 matting → H7 camera → H8 set extension (H9 audio-reactive opt, H10 size gate).

**Director use cases:** hero prop insert, set extension, particle atmosphere, sky/plate under subject — all **tracked**, not stickers.

---

## 9. Wave L — Delivery & review

| ID | Slice | Deliverable | Exit |
|----|-------|-------------|------|
| **L1** | Review package | Burn-in TC optional; watermark **off by default**; comments file next to export | Local JSON notes |
| **L2** | Handoff | EDL-lite or FCPXML-lite export of cuts + markers (best-effort; document gaps) | Open in at least one desktop NLE smoke |
| **L3** | Deliverable presets | Social / Review ProRes-proxy-like (via F5 when needed) / Master H.264/H.265 HDR | Preset chips |
| **L4** | Frame + metadata | Export embeds project title, version name, LUT used | ffprobe/doc check |

---

## 10. ML that serves directors (Colab — Wave M)

**Detail:** [`PERFECT_BUILD.md`](PERFECT_BUILD.md) §1 · [`REMAINING_MASTER_PLAN.md`](REMAINING_MASTER_PLAN.md) Wave M.  
**Do not wait for inpaint** to start Depth / Matting / Flow — separate Colab runtimes; packs bind independently.

| ID | Pack | Director job | Start |
|----|------|----------------|-------|
| M1 | Inpaint (E1) | Continuity / remove boom / logo | Keep running |
| M2 | Depth | Soft Z for CG inserts (H5) | **Now (parallel)** |
| M3 | Video matting | Clean plates / set extension (H6) | **Now (parallel)** |
| M4 | Optical flow / stabilize | Cleaner attach + reframe | Parallel or after M2/M3 if one GPU |
| — | Track (F1) | Attach CG / titles / FX | Shipped |
| — | FILM (E4) | Performance slow-mo | Shipped |
| M5–M7 | Roto / portrait / sky (opt) | Quality upgrades | Only if gaps |

No “Avatar in one TFLite.” Fidelity = **edit craft + Filament + assets + these assists**.

---

## 11. Hard rules

1. **Engines out of base APK** when heavy (Filament CG, FFmpeg, AR, large ML).  
2. **Timeline is source of truth** — versions, markers, CG lanes, grade, stems.  
3. **Preview ≈ export** for director trust; if not, UI must say so.  
4. **No fake Hollywood claims** — label previz vs beauty; scopes honesty.  
5. **Local-first** — review notes and handoff files live next to the project.  
6. **Release-** for large assets (HDRI, glTF, FILM, inpaint when large).

---

## 12. Non-goals

- Path-traced offline CGI / full USD / Nuke replacement  
- Theatrical Atmos / Dolby Vision authoring suite  
- Cloud collab / account login as required path  
- Generative body/clothing invent for exploitation  
- Shipping farm-sized assets in the **base** APK  

---

## 13. Exit for “director kit v1”

1. **Hero reel:** multicam assembly + markers + secondary grade + stem duck + tracked glTF insert + depth soft-comp — preview ≈ export  
2. **Handoff:** versioned cut + markers + EDL-lite opens elsewhere (documented limits)  
3. **Size:** CG / heavy ML stay on-demand; base APK within noise of E5 gate  
4. **Matrix:** Waves H–L rows LIVE/PACK with honest yellows  
5. **Device:** mid-range Pixel smoke + Maestro yellows documented  

When this drifts, update this file and `REMAINING_MASTER_PLAN.md` / `NORTH_STAR_ROADMAP.md` / `TOTAL_REMAINING.txt` / `R7_CG_EFFECTS.md` in the same change.
