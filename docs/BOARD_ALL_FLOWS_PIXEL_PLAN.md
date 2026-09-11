# All-flows pixel plan (every board)

Not C04-only. This is the **complete** map of every `flows/` + `flows/additional/` board → Compose host → what “good” means → tablet/fold → remaining polish.

Catalog status is currently **Done** for structure/kits. Treat that as **L1**. True pixel lock is **L3** (your side-by-side).

## Fidelity levels

| Level | Meaning |
|-------|---------|
| **L0** | Missing host / missing screen IDs |
| **L1** | Screens + alt/edge + tips/trust + BoardTheme + adaptive kits (current catalog Done) |
| **L2** | Board-matched hierarchy, chips, imagery pack for that flow |
| **L3** | You approve phone + tablet/fold vs board PNG/JPG |

**Target:** every row → **L2**, then **L3** by your review.

## Locked rules (all flows)

1. Board PNG/JPG is source of truth (REMATCH_* beats older `05`/`06`/`07`/`13`/`14` cores).
2. Happy + **Alt + Edge** from each board must stay reachable (catalog + host `when`).
3. Tips / credits / trust where the board shows them.
4. Chrome: Home · Explore · Create · Community · My Stuff.
5. Devices: **Compact** = phone column; **Medium** = 3-col grids; **Expanded** = rail + `BoardListDetail` / `BoardPreviewControls` + hinge-safe.
6. Assets: `drawable/board_<flow>_<role>.*` (hybrid generate/crop).

---

## A. Core flows 1–30

| Flow | Board file (canonical) | Host | Entry IDs (examples) | L1 | L2 gap (polish) | Tablet/fold |
|------|------------------------|------|----------------------|----|-----------------|-------------|
| 1 | `01_Launch_and_Onboarding.png` | `BoardFlow1Host` | `b1_splash`…`b1_whats_new` | Y | Splash/welcome/features art exists; tighten dots, For Everyone collage, signup methods | Phone frame; feature grid → 2×N on Expanded |
| 2 | `02_Home_and_Discover.png` | `BoardFlow2Host` | `b2_home`…`b2_recent` | Y | Template thumbs, Discover chips, scroll sections vs board | Intent 2×2→wider; Discover two-pane |
| 3 | `03_Photo_Video_Input.png` | `BoardFlow3Host` | `b3_open`…`b3_corrupt` | Y | Chooser icons, gallery denser thumbs, trim chrome | Gallery `boardGridColumns`; edges full-width |
| 4 | `04_AI_Understanding.png` | `BoardFlow4Host` | `b4_selected`…`b4_safety` | Y | Checklist icons, suggestion chips art | Path cards multi-col |
| 5 | **`REMATCH_05_*.png`** (not old 05) | `BoardCreateRematchHost` | `b5_*` | Y | Type grid tiles, adjust toggles layout | Preview\|controls split |
| 6 | **`REMATCH_06_*.png`** | same | `b6_*` | Y | Meme style grid, share strip | Same |
| 7 | **`REMATCH_07_*.png`** | same | `b7_*` | Y | Prompt chips, speak/type sheet | Sheet max-width |
| 8 | `08_Save_Share_Community.png` | `BoardLibraryHost` | `b8_*` | Y | Share targets icons, publish privacy | Share sheet grid |
| 9 | `09_Profile_Personalization.png` | `BoardMeTrustHost` | `b9_*` | Y | Avatar/badges/albums visuals | Profile two-pane |
| 10 | `10_Pro_Subscription_Discovery.png` | same | `b10_*` | Y | Compare Free/Pro matrix art | Wide compare table |
| 11 | `11_Settings_Help.png` | same | `b11_*` | Y | Settings rows, help article | List\|detail |
| 12 | `12_End_to_End_Journey.png` | `BoardSocialRecoveryHost` | `b12_*` | Y | Milestone cards chrome | Wide checklist |
| 13 | **`REMATCH_13_*.png`** | `BoardMediaStudioHost` | `b13_*` | Y | Cutout/refine/pack/share art | Preview\|tools |
| 14 | **`REMATCH_14_*.png`** | same | `b14_*` | Y | Motion styles, timeline, poster/video | Same + media preview |
| 15 | `15_Advanced_Photo_Editor.png` | same | `b15_*` | Y | Tool rail, crop/filters chrome | Expanded workspace |
| 16 | `16_Multi_Photo_Collage.png` | same | `b16_*` | Y | 12 purposes grid, layouts | Wide purpose grid |
| 17 | `17_Export_Platform_Optimization.png` | same | `b17_*` | Y | Platform cards, staging | Platform multi-col |
| 18 | `18_My_Creations_Drafts_History.png` | `BoardLibraryHost` | `b18_*` | Y | Grid thumbs, trash tip | List\|detail |
| 19 | `19_AI_Generation_Recovery.png` | `BoardF19Host` | `b19_*` | Y | All recovery illustrations | Centered phone frame |
| 20 | `20_AI_Safety_Moderation.png` | `BoardMeTrustHost` | `b20_*` | Y | Allowed/Not Allowed boards | Two-col rules |
| 21 | `21_Privacy_Center.png` | same | `b21_*` | Y | Hub tiles | Hub grid |
| 22 | `22_Account_and_Authentication.png` | same | `b22_*` | Y | OAuth buttons, devices | Phone frame |
| 23 | `23_AI_Credits.png` | same | `b23_*` | Y | Pack cards, cost table | Wide packs |
| 24 | `24_Subscription_Management.png` | same | `b24_*` | Y | Plan matrix, cancel survey | Wide matrix |
| 25 | `25_Notification_Center.png` | same | `b25_*` | Y | Notif rows, settings | List\|detail |
| 26 | `26_Search_Templates.png` | `BoardLibraryHost` | `b26_*` | Y | Results grid, filters | Two-pane search |
| 27 | `27_Community_UGC.png` | `BoardSocialRecoveryHost` | `b27_*` | Y | Feed cards, guidelines | Feed\|post |
| 28 | `28_Offline_Network_Recovery.png` | same | `b28_*` | Y | Banner/modal/sync failed | Phone frame |
| 29 | `29_Accessibility_localization.png` | same | `b29_*` | Y | A11y toggles, RTL tip | Settings two-pane |
| 30 | `30_AppLifeCycle_Recovery.png` | same | `b30_*` | Y | Update/crash/integrity | Phone frame |

Duplicates on disk (`*_copy.png`) are **not** separate flows — ignore for implementation.

---

## B. EXTRA boards

| Board | Host | IDs | L2 gap |
|-------|------|-----|--------|
| `EXTRA_Make_it_Different_AI_Art.png` / **REMATCH_EXTRA_Different** | `BoardCreateRematchHost` | `b_diff_*`, `b_different` | Style-12 grid art, strength/aspect |
| `EXTRA_Sharing_and_Collaboration.png` | `BoardLibraryHost` | `b_collab_*` | People/perms/album/activity art |

---

## C. Additional C-series (all tools — not only C04)

Shared host: [`BoardCToolsHost`](app/src/main/java/com/pixijoy/ai/ui/board/ctools/BoardCToolsHost.kt) + [`BoardCTool`](app/src/main/java/com/pixijoy/ai/ui/board/ctools/BoardCTool.kt).

| Tool | Board file | Prefix | Happy+edges (shared) | L2 asset pack needed |
|------|------------|--------|----------------------|----------------------|
| C00 | `C00_Empty_Home_First_Creation.jpg` | `b_c00_*` | Dedicated host | Hero + funny dog **have** |
| C04 | `C04_Remove_Background_Object.jpg` | `b_c04*` | intro→share + refine + edges | subject/cutout/beach **have** |
| C05 | `C05_Restore_Old_Photo.jpg` | `b_c05*` | same wizard + BA | restore before/after |
| C06 | `C06_Face_Swap_Recast.jpg` | `b_c06*` | + consent / denied | face pair + consent art |
| C07 | `C07_Change_Scene_Relocate.jpg` | `b_c07*` | scene options | scene thumbs × N |
| C08 | `C08_Add_or_Remove_People.jpg` | `b_c08*` | + consent | people add/remove art |
| C09 | `C09_AI_Portrait_Headshot.jpg` | `b_c09*` | portrait styles | headshot variants |
| C10 | `C10_AI_Expand_Fill_Upscale.jpg` | `b_c10*` | expand/fill | outpaint BA |
| C11 | `C11_Talking_Photo.jpg` | `b_c11*` | motion + preview | talking poster/video |
| C12 | `C12_Greeting_Card_Festival.jpg` | `b_c12*` | festivals | card templates |
| C14 | `C14_Remix_from_Community.jpg` | `b_c14*` | remix | community remix thumbs |

**No C13 board** — stickers live under Flow 13 / REMATCH_13.

`FLOW_06` / `FLOW_13` / `FLOW_14` in additional/ are superseded by REMATCH — do not dual-implement.

---

## D. Delivery waves (all flows)

Execute **per board file**, not per one C-tool:

| Wave | Flows | Work |
|------|-------|------|
| **AF0** | All | Audit sheet: L1 vs L2 checklist per flow (this doc) |
| **AF1** | 1, 2, C00 | L2 polish (onboarding/home already strongest) |
| **AF2** | 3, 4, 19 | Input + understanding + generation recovery imagery |
| **AF3** | 5, 6, 7, Diff | REMATCH create L2 style grids + results |
| **AF4** | **C04→C14 each** | Per-tool option art + consent/BA/motion (C04 first *as template*, then C05…C14) |
| **AF5** | 13–17 | Stickers/Move/Editor/Collage/Export L2 |
| **AF6** | 8, EXTRA Collab, 18, 26 | Save/share/creations/search L2 |
| **AF7** | 9–11, 20–25 | Profile/Pro/Settings/Safety/Privacy/Auth/Credits/Sub/Notifs L2 |
| **AF8** | 12, 27–30 | E2E/Community/Offline/A11y/Lifecycle L2 |
| **AF9** | All | Tablet/fold QA matrix (phone, 7", unfold) + mark L3 after your approve |

Est. **~25–40 sessions** for full L2 across all boards; L3 gated on your review.

---

## E. Per-flow session recipe (repeat for every board)

1. Open board PNG/JPG side-by-side with catalog entry.  
2. List panels: Happy / Alt / Edge / Tip.  
3. Confirm every panel has a `screenId` + host branch (gap → add).  
4. Generate/crop `board_<id>_*` assets; wire heroes/grids/BA.  
5. Match hierarchy, CTAs, chips, tips, trust.  
6. Run Compact + Expanded preview.  
7. You approve → L3 note in catalog / review log.

---

## F. Tablet / fold (applies to every flow)

| Size | Requirement |
|------|-------------|
| Compact | Match phone frame column |
| Medium | 3-column grids; wider max content |
| Expanded | Nav rail; `BoardListDetail` for browse/detail; `BoardPreviewControls` for edit/adjust; hinge-safe CTAs |

Hosts already have kits in [`BoardAdaptive.kt`](app/src/main/java/com/pixijoy/ai/ui/board/components/BoardAdaptive.kt) — AF waves must **use** them on every main screen, not only C04.

---

## G. Honest current state

- **Inventory:** all board files above are mapped to hosts/IDs (no L0).  
- **Catalog Done = UI Done / soft AI** — Compose + kits; simulated generate/OAuth/sync. See [BOARD_RELEASE_HYGIENE.md](BOARD_RELEASE_HYGIENE.md).  
- **L1:** catalog Done across batches.  
- **L2 (AF1–AF8):** shipped across hosts — per-tool `BoardToolArt` (C04–C14) with **optionImages**, C07 scene pack, trust hub heroes.  
- **L3:** **your** phone+tablet approve in [`BOARD_L3_REVIEW.md`](BOARD_L3_REVIEW.md). Remaining work is feedback-driven pixel fixes only.  
- **Canonical rematch boards:** `REMATCH_05/06/07/13/14` + `REMATCH_EXTRA_Different`. Older cores were removed.  
- **Production flip:** Board UI is the live path — MainScaffold tabs, onboarding (`b1_*`), and legacy `Ui*` routes resolve to board hosts via `BoardRouteMap`.

**AF wave status**

| Wave | Status |
|------|--------|
| AF0 Audit sheet | Done (this doc) |
| AF1 Flow 1–2 + C00 | Done (L2 assets + Discover thumbs) |
| AF2 Flow 3–4 + F19 | Done |
| AF3 REMATCH 5/6/7/Diff | Done |
| AF4 C04→C14 per-tool art | Done |
| AF5 Media 13–17 | Done |
| AF6 Library / collab / search | Done |
| AF7 Me / Pro / trust | Done |
| AF8 Social / recovery | Done |
| AF9 Adaptive + docs | Done (kits wired; L3 = your review) |
| AF10 Production flip | Done (tabs + onboarding + pipeline; old scaffold screens unused) |
