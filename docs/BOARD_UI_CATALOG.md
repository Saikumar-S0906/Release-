# Board UI catalog (parallel path → production path)

Pixel-faithful Compose UI under `ui/board/`. Board hosts are the live tab/onboarding path.

**Status honesty:** catalog `Done` displays as **UI Done / soft AI**. That means screens are wired with simulated generate / OAuth / sync — **not** L3 pixel lock and **not** live generative backends. Full rules: [BOARD_RELEASE_HYGIENE.md](BOARD_RELEASE_HYGIENE.md).

## How to open

Settings / Flow Index → **Open Board UI catalog** → pick a screen.

| Tab | Screen ID |
|-----|-----------|
| Home | `b2_home` |
| Explore | `b2_discover` |
| Create | `b_create_hub` (Tools \| AI) |
| Community | `b27_feed` |
| My Stuff | `b18_browse` |

## Design system

- `BoardTheme` — **dark** brand canvas (`BoardColors.dark()`), purple primary, AI gradient CTAs, **crash-safe SansSerif** (`BoardFontFamily` / `BoardType` Bible scale)
- Component map (Bible §57): [BOARD_COMPONENT_MAP.md](BOARD_COMPONENT_MAP.md)
- Rich UX baseline: [BOARD_RICH_UX_SCORECARD.md](BOARD_RICH_UX_SCORECARD.md)
- Kits: trust, tips, credit, generate block, BA, VAR, media preview, intent tiles
- Adaptive: `BoardAdaptiveFrame`, `BoardWideFrame`, `BoardListDetail`, `BoardPreviewControls`, `boardGridColumns`
- Soft edges: `BoardSoftEdgeGate` (offline / low credits / safety)

C-tool intros use bare prefixes (`b_c04`, `b_c07`, …) — not `*_intro`.

## Pixel / fidelity status

| Batch | Catalog | Meaning |
|-------|---------|---------|
| Flow 1–2, C00 | UI Done / soft AI | L2 assets; L3 = your approve |
| Flow 3–4 | UI Done / soft AI | same |
| REMATCH 5/6/7/Diff + F19 | UI Done / soft AI | Canonical boards = `REMATCH_*` |
| C04–C14 tools | UI Done / soft AI | Soft AI wizards |
| Media 13–17 | UI Done / soft AI | Stickers/Move use REMATCH_13/14 |
| Library 8/18/26/collab | UI Done / soft AI | same |
| Me/Trust 9–11/20–25 | UI Done / soft AI | same |
| Social/Recovery 12/27–30 | UI Done / soft AI | same |

See [BOARD_L3_REVIEW.md](BOARD_L3_REVIEW.md) for phone + tablet approve checklists (L3).

See [BOARD_ALL_FLOWS_PIXEL_PLAN.md](BOARD_ALL_FLOWS_PIXEL_PLAN.md) for every flow board → L1/L2/L3.

Rich UX baseline (Design Bible §52 + debt/token gaps + Phase 0 captures): [BOARD_RICH_UX_SCORECARD.md](BOARD_RICH_UX_SCORECARD.md).

Component map (Bible §57): [BOARD_COMPONENT_MAP.md](BOARD_COMPONENT_MAP.md).

## Assets

`drawable/board_*` packs: onboarding, C00, C04–C14 per-tool (`BoardToolArt`), rematch/media/library/social (`BoardFlowArt`). Rematch Beautiful/Funny/Tell/Stickers/Move/Different against `flows/additional/REMATCH_*` only.
