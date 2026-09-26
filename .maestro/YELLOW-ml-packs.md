# YELLOW / manual — Play Feature Delivery pack install only

Maestro cannot drive Play Store module download or a system file picker sideload. Those stay manual on a Play-installed build.

Manual Play path:

1. Photo Fill / Super-res / Denoise, or Movie Slow-mo / Track
2. On a Play-installed build, confirm a Play module only for packs that ship one.

Automated (Pixel_7a `emulator-5556`, 2026-09-25/26): **green** —
- `empty-packs`, `fill-pack-status`, `superres-denoise`, `d10-slowmo-interp`, `f1-track-roto`
- `.maestro/packs/e4-frame-interp-download.yaml` — FILM Download (~67 MB) → pack status + ML chip

Flows use `studio_scroll_more` / `studio_hscroll_more` under 280dp glass.
