# Phase 3 — Color/Looks live-preview measurement (2026-09-23)

## Decision

**No downscaled live-preview optimization landed in this pass.**

Plan rule: profile Color and Looks dragging on Pixel 7a with representative medium/large images; only then add downscaled drag preview + full bake on Apply.

## What we checked

- Emulator attached this session was Pixel_6_Pro (`emulator-5554`), not Pixel_7a — not the acceptance device for jank claims.
- Cold-start Maestro flakes blocked reliable editor profiling automation in the same window.
- Existing live path already uses ColorMatrix / Looks matrices on the display bitmap (cheap vs full bake); Selective HSL still allocates a live bitmap — that path is Apply-gated for identity.

## Exit for this slice

Mark B4 as **measure-deferred / no speculative change**. Re-open only when Pixel_7a Systrace/FrameTimeline shows sustained jank while dragging Color or Looks on ≥12 MP sources.
