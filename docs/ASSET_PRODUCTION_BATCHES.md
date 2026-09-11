# PixiJoy — parallel asset production batches

Starter pack is foundation only. Track production outside the APK for large creative packs.

| Batch | Deliverable | Owner | Status |
|-------|-------------|-------|--------|
| B1 | Final logo master (SVG + 1024 adaptive layers + mono) | Design | Pending |
| B2 | Full custom UI icon family (nav, edit, status) refining `pj_ic_*` | Design | In progress (starter vectors) |
| B3 | AI state illustrations (analyzing, success, fail, safety, empty) | Design + Eng | **Eng vector stubs shipped** (`board_b3_ai_*`); design photo pack still Pending |
| B4 | 30–50 starter templates (CDN, not APK) | Content | Eng stub (`BoardCdn` + Coil); design packs Pending |
| B5 | 10–20 sticker packs (CDN) | Content | Eng stub key `stickers/pack_hero`; packs Pending |
| B6 | 20+ GIF/reaction packs (CDN) | Content | Pending |
| B7 | 15–20 Lottie assets matching `motion/MOTION_MANIFEST.json` | Motion | **Eng stubs shipped** (`BoardLottie`); drop JSON into `res/raw/board_motion_*` when ready |
| B8 | Play Store screenshots + feature graphic | Marketing | Pending (post-L3) |
| B9 | A11y / localization / RTL asset pass | Design | Eng smoke (font-scale/RTL); design assets Pending |
| B10 | CDN + cache pipeline for remote creative | Eng | **Stub landed** (`BoardRemoteImage` + drawable fallback) |

Rules: see `design/PIXIjOY_ASSET_SPEC.md` and `design/AI_AGENT_ASSET_RULES.md`.  
Do not ship unlicensed meme/photo/sticker/font assets.
