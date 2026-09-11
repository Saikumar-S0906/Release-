# Board UI — L3 review matrix

**L3** = you approve **phone + tablet/fold** side-by-side vs the board PNG/JPG.

Statuses: `Ready` · `Approved` · `Blocked`

Catalog `Done` is only **UI Done / soft AI** until you mark rows Approved here. See [BOARD_RELEASE_HYGIENE.md](BOARD_RELEASE_HYGIENE.md).

Open via **Board UI catalog** (`b*` IDs). Use **REMATCH_*** boards for flows 5/6/7/13/14/Different.

**Phase 7 DoD eng (2026-09-08):** Expanded list-detail (creations), preview-controls (Beautiful/Funny adjust), system light/dark board theme, CDN Coil stub + drawable fallback, live-generate flag with soft fallback, font-scale + RTL Compose smoke. **Your** §52/§53 Approved ticks still required below.

**Wave B eng (2026-09-08):** All matrix rows remain **Eng Ready**. No Blocked flow IDs from you yet — eng will pixel-fix only IDs you mark Blocked. Fill **Your status** = `Approved` / `Blocked` after phone + tablet vs rematch.

---

## Ready for your sign-off

| Area (Bible §52–53) | Entry | Eng Ready | Your status | Notes |
|---------------------|-------|-----------|-------------|-------|
| Create | `b5_choose` | Ready | | PreviewControls + SoftGenerate; rematch PNG |
| Media / F19 | `b13_hub` / `b19_start` | Ready | | CDN sticker stub; recovery art |
| Library | `b18_browse` | Ready | | Expanded `BoardListDetail` |
| Home / Input | `b2_home` / `b3_open` | Ready | | Adaptive + Discover CDN stub |
| Me | `b9_open` / `b10_discover` | Ready | | Heroes from Phase 6 |
| Social / C-tools | `b27_feed` / `b_c04` | Ready | | SoftEdge + option art |

### Per-flow (optional detail)

| Flow | Entry | Notes |
|------|-------|-------|
| **1** Onboarding | `b1_splash` | Dots, feature grid, OAuth signup |
| **2** Home/Discover | `b2_home` | Photo hero + template thumbs · CDN featured stub |
| **C00** Empty home | `b_c00_empty` | Hero scrim + taste colors |
| **3** Input | `b3_open` | Chooser art |
| **4** Understanding | `b4_selected` | Edges with heroes |
| **5/6/7/Diff** Rematch | `b5_choose` | Per-intent heroes · PreviewControls |
| **C04–C14** Tools | `b_c04` | Per-tool art + image options |
| **13–17** Media | `b13_hub` | Hub heroes · sticker CDN stub |
| **19** Generation | `b19_start` | Recovery art |
| **8** Save/Share | `b8_ready` | Ready + share grid |
| **18/26/collab** | `b18_browse` / `b26_open` | List-detail Expanded |
| **9** Profile | `b9_open` | Avatar, stats, themes, albums |
| **10/20–25** Me/trust | `b10_discover` | Gate heroes on hubs |
| **12** Journey | `b12_intro` | Milestone chrome |
| **27** Community | `b27_feed` | Feed + guidelines |
| **28–30** Recovery | `b28_banner` | Offline/a11y/lifecycle heroes |

**Approve all (or mark exceptions):** _______________ date _______________

**Blocked-row pixel fix loop:** reply with flow IDs that fail phone+tablet vs rematch — agent fixes those next.

---

## How to review (fast)

1. Phone: Board catalog → walk happy path for a flow.  
2. Tablet/fold: same IDs with Expanded width (or device).  
3. Tick vs board PNG in `flows/` / `flows/additional/`.  
4. Reply with flow IDs that fail — agent fixes those next.

See [BOARD_ALL_FLOWS_PIXEL_PLAN.md](BOARD_ALL_FLOWS_PIXEL_PLAN.md).
