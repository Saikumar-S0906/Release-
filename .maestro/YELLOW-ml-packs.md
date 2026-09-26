# YELLOW / manual — Play Feature Delivery pack install

Maestro cannot drive Play Store module download or a system file picker sideload. Those stay manual.

Manual:

1. Photo Fill / Super-res / Denoise, or Movie Slow-mo / Track
2. Super-res, Denoise, and FrameInterp Missing show **Download** on `empty_packs`. Fill (Inpaint) and Track have no hosted weight, so Missing shows **Install from file**, not Download.
3. On a Play-installed build, confirm a Play module only for packs that ship one. This APK does not ship inpaint, interp, or track weights.
4. On a sideload APK or emulator, tap **Download** for Super-res, Denoise, or Slow-mo (FrameInterp FILM ~67 MB). Checksum failure stays Failed, not Ready. Inpaint and Track have no HTTPS pin: **Install from file** or copy into `filesDir/ml_incoming/`. Do not invent a Track URL.
5. Super-res / Denoise: status “Downloaded and verified (Ready)” still leaves Apply off until “Neural ready”. FrameInterp: after Download, LiteRT may bind FILM (FLOAT32 512) → neural Slow-mo chip unlocks (`engineReady`). Export with Neural frames synthesizes mid-frames; preview stays rate/hold. Fill neural Apply waits on `engineReady(Inpaint)`. Track stays locked. Device measure: `scripts/measure_pack_memory.ps1`.

Automated (Pixel_7a `emulator-5556`, 2026-09-25): **green** —
- `empty-packs`, `fill-pack-status`, `superres-denoise`, `d10-slowmo-interp`, `f1-track-roto`
- `.maestro/packs/e4-frame-interp-download.yaml` — FILM Download (~67 MB) → `movie_slowmo_pack_status` + `movie_slowmo_ml`

Flows use `studio_scroll_more` / `studio_hscroll_more` under 280dp glass. H-scroll fade is tappable (scrolls chips).
