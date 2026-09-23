# D1 multi-track composition E2E

**Date:** 2026-09-23  
**Device:** Pixel_7a

## Met
- Preview + export build Media3 `Composition` from `MultiTrackTimeline` (not flatten-only decode loss).
- `MovieExportWorker` decodes MT graphs (preserves trim).
- Session holds MT; track mute chips (`movie_tracks` / `movie_track_*`).
- Units: composed clips, withPrimaryFlat, sequence plan.

## Maestro
- `.maestro/movie/multitrack-lanes.yaml`

## Explicitly later (D2+)
Ripple/roll, transitions, keyframes, titles, PiP layout, HDR, etc.
