# PixiJoy — World-class quality plan (post competitor Phases 1–4)

**Verdict:** Non-infra photo-spine features are largely **device-testable**. Q1 closed Create-graph PARTIAL hosts to **WORKS**. World-class = L3 polish + infra when ops ready — not more competitor clones.

## Status snapshot

| Axis | Score | Notes |
|------|-------|-------|
| Non-infra testable | **Strong** | WORKS: Beautiful, Funny, Tell, C04–C11, C12, C14 bake, Stickers, Collage, Editor, Export, library |
| Crash | **4 / 5** | Safe bitmaps + Back skips `*_processing` on Q1 hosts |
| Nav / IA | **5 / 5** | Home · Explore · Create · Community · My Stuff only (Create = Tools \| AI) |
| Bible depth | **4 / 5** | Q2 eng Ready: ResultFooter + SoftEdge + F15 wide + Lottie processing; L3 user sign-off pending |

**Intentional SOFT (infra only):** Imagen, Play Billing, OAuth, live community publish, CDN packs.  
**OUT:** Canva suite, Lightroom RAW catalog, live effects marketplace.

## Quality phases

### Q0 — Honesty + smoke gate ✅
- Marked Tell / C07–C09 / C11 / C14 honesty in [FUNCTIONALITY_MATRIX.md](FUNCTIONALITY_MATRIX.md)
- Device smoke: C04 → Edit → Export; Stickers; Collage; Funny
- Harden Back-skips-processing before engine wiring

### Q1 — Close Create-graph PARTIAL (on-device WORKS) ✅
1. C07 Relocate → `relocateToScene` (cutout + scene composite)
2. C08 People → hasFace gate + `peopleEdit` / enhance fallback
3. C09 Portraits → `faceRecastOrSwap`
4. Tell (07) → `parseCommand` + pipeline map + bitmap save
5. C11 Talking → Ken Burns GIF (`TalkingPhotoAnimator` + `GifEncoder`) WORKS
6. C14 Remix → `remixStyle` bake; publish stays SOFT
7. C06 + results → `BoardResultFooter` + `BoardSafeResultImage`

### Q2 — Bible polish (world-class bar) ✅ eng Ready for L3 sign-off
- Kits: ResultFooter 48dp + navBars; SoftEdge honesty (no soft-preview tip); Gate navBars
- Hosts: Beautiful / C04 / F15 — `BoardWideFrame` on F15 editor+MoreTools; C04 refine/result Expanded-wide; `BoardResultFooter` on F15 SaveShare; `BoardLottie(GenerationProgress)` processing; BoardType / BoardTitleText on key headers
- Eng baseline only — user L3 Approved remains in [BOARD_L3_REVIEW.md](BOARD_L3_REVIEW.md)
- Rich UX scorecard notes updated for SoftEdge/type (not claiming Approved)


### Q3 — Infra when ops ready
- Imagen, Billing Console, Credential Manager, community backend, CDN packs

## Smoke reference

See [FUNCTIONALITY_MATRIX.md](FUNCTIONALITY_MATRIX.md) device checklist and [PRODUCT_LIVE_SMOKE.md](PRODUCT_LIVE_SMOKE.md).
