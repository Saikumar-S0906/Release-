# PixiJoy north-star feature forecast and development plan

**Spine:** Local-first pro mobile studio - more than Photoshop for stills, cinema-class for movie, no upload / no account / no watermark / no subscription.
**Hard rule:** every feature is an edit-graph or timeline op -> engine (`:photo` / `:movie` / `:ml`) -> WorkManager export. UI never owns giant bitmaps.
**Audience:** Saikumar + team. Aligns with `ARCHITECTURE.md` and `MASTER_PLAN.md`; does not replace shipping focus.

---

## 1. Positioning

| Competitor class | PixiJoy aim |
|---|---|
| Photoshop / Lightroom / Snapseed | Full stills craft on device: layers, grade, retouch, RAW, batch |
| CapCut / VN / Premiere Rush | Multi-track mobile cinema: timeline, grade, audio, titles, 4K/HDR |
| Avatar-class storytelling | User-facing cinema craft on phone. Full CGI/VFX farm stays out of base APK; advanced packs via Play Feature Delivery |

Constraint: **on-device only on the editor path.** Cloud ML stays out. Optional `:ml-*` and ffmpeg packs are opt-in, never base APK.

---

## 2. Photo feature map (Photoshop+)

### Have / shipping (R1-R2)
- Crop, rotate, flip; exposure / contrast / HSL
- Heal + clone-offset (explicit Stamp source); erase; cutout / subject
- Text, stickers, collage (2-up / 4-up); expand / fill
- Curves, blur, sharpen, vignette (as ops)
- Export JPEG / PNG / WebP; EXIF keep/strip + HEIC/AVIF
- Non-destructive `PhotoOp` graph encode/decode

### Next depth (R3)
- Real **layers + masks + blend modes**
- Selective color / HSL ranges; **LUT** import/apply
- Content-aware fill polish; on-device generative fill pack (`:ml-inpaint`)
- Sky / background replace; perspective, liquify; RAW/DNG; presets; batch; wide-gamut

### Later (R5-R6+)
- Smart-object analog; advanced typography; print/PDF; portrait retouch packs

---

## 3. Movie feature map (cinema-class mobile)

### Have / shipping (R2)
- Play, scrub, filmstrip; trim / split / concat; speed
- Ken Burns / talking still; Export HD / FHD / 4K (`MovieExportSize`); progress %
- Audio stems + EQ / dynamics; sticky Export session chrome

### Next depth (R4)
- Multi-track V/A timeline; ripple/roll; transitions; speed ramps
- Keyframed transform; titles/captions; grade + LUT + scopes
- Audio finish (ducking, VO); PiP/multicam; stabilize; chroma; frame-interp; HDR export

### Ambition packs (R6+, on-demand)
- Tracking / roto-lite; VFX packs; 3D/Filament titles; ARCore previz; optional ffmpeg

---

## 4. Staged plan

| Stage | Focus | Exit |
|---|---|---|
| **R2** | Studio v1 quality (done / exited) | review + units + honest Maestro |
| **R3** | Photo layers/masks/LUTs/RAW track | LayerStack round-trip + export |
| **R4** | Movie multi-track depth | versioned timeline + export match |
| **R5** | ML packs on-demand | base APK under 40 MB |
| **R6+** | Chroma/tracking/HDR/3D/AR packs | isolated modules |

---

## 5. Module ownership

| Concern | Module |
|---|---|
| Decode / EXIF / color / IO | `:core:media` |
| Still ops + photo session | `:photo` |
| Timeline / Media3 / movie export | `:movie` |
| On-device models | `:ml` (+ on-demand packs) |
| Adaptive chrome | `:app` + `:designsystem` |

---

*Architect - re-parked on next after tip 078dc9b. Update when a stage exits.*