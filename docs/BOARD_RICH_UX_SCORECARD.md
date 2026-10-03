# Board Rich UX Scorecard — Phase 0 baseline

**Date:** 2026-09-08  
**Sources of truth:** [PixiJoy UI-UX Design Bible.pdf](../PixiJoy%20UI-UX%20Design%20Bible.pdf) · rematch `flows/` · [PIXIjOY_VISUAL_IDENTITY.md](PIXIjOY_VISUAL_IDENTITY.md)  
**Plan:** Rich UX Roadmap Phase 0 exit artifact  
**Device for captures:** Pixel_6_Pro (API 35) → `docs/board_review/phase0_baseline/` · Pixel_Tablet (API 35, 2560×1600) → `docs/board_review/phase0_baseline_tablet/`  
**Tablet Expanded:** captured 2026-09-08 (21 screens).

Scoring: **P** pass · **F** fail · **P-** partial · **N** not assessed yet (needs side-by-side PNG)

---

## 1. Bible §52 Premium Feel Checklist (system-wide)

### Visual
| Check | Status | Evidence |
|-------|--------|----------|
| Correct spacing | **F** | No Bible XS–XXXL token module; hosts use ad-hoc padding |
| Correct typography | **F** | `BoardType` = title20/body**14**/caption12/cta14 — Bible needs Hero32…Body**16**; `BoardTitleText` almost unused in hosts |
| Consistent radius | **P-** | `BoardRadii` 8/12/16/20/pill — Bible wants card16 / large20 / **sheet28** |
| Correct iconography | **F** | ~**828** Material `Icons.*` hits under `ui/board/`; shell uses Material Defaults |
| Correct color hierarchy | **F** | Board light `#FAFAFC` + ActionBlue `#3B82F6` vs Identity dark `#0B0B10` + purple→pink |
| No unnecessary borders | **P-** | Kits restrained; many hosts still Material card chrome |
| No excessive shadows | **P** | No heavy shadow language observed |

### UX
| Check | Status | Evidence |
|-------|--------|----------|
| One primary action | **P-** | Kits support hierarchy; not enforced per screen |
| Clear navigation | **P** | 5-tab shell; `BoardFlowChrome` Back+Next on key create steps |
| Immediate feedback | **P-** | Press/pulse only — no MOTION_MANIFEST Lottie |
| Error recovery | **P-** | SoftEdge on create/C/media only |
| Empty state | **F** | No shared `PixiEmptyState` kit; collections often thin |
| Loading state | **F** | No shared skeleton; full-block progress common |
| Offline state | **P-** | Flow28 + SoftEdge elsewhere incomplete |

### Android (Bible §52)
| Check | Status | Evidence |
|-------|--------|----------|
| Edge-to-edge | **F** | **0** matches for `WindowInsets` / `systemBarsPadding` / `enableEdgeToEdge` under `ui/board/` |
| Insets | **F** | Same |
| Gesture navigation | **N** | Needs device pass |
| Back navigation | **P-** | Predictable in hosts; state preservation uneven |
| Font scaling | **P-** | Phase 7 DoD smoke (fontScale 1.3) |
| Accessibility | **P-** | contentDesc helpers; TalkBack still uneven |
| Dark mode | **P** | Dark default + first-class light tonal (system follow) |
| Localization / RTL | **P-** | RTL Compose smoke; B9 assets Pending |
| Adaptive layout | **P** | ListDetail creations; PreviewControls Beautiful/Funny; kits elsewhere |

### Performance
| Check | Status | Evidence |
|-------|--------|----------|
| Smooth scrolling | **N** | Manual |
| No main-thread blocking | **P-** | Soft AI demos; live AI later |
| Image optimization | **P-** | `BoardArtDecode` exists; sparse packs |
| Efficient animations | **F** | No Lottie; timing tokens absent |
| Fast startup | **P-** | Prior ANR fixes applied (see WHAT_NEXT) |

---

## 2. L3 matrix family scores (Phase 0)

Scores are **engineering baseline** (code + assets), not your final L3 Approved. Use with phone PNGs in `phase0_baseline/`.

Legend for Visual / UX / Density / SoftEdge / TypeDebt: **1** weak … **5** strong.

| Family | Entry ID | Visual | UX | Density | SoftEdge | TypeDebt* | Bible §52 Visual+UX | L2 gap (pixel plan) | Phase 6+ wave |
|--------|----------|-------:|--:|--------:|---------:|----------:|---------------------|---------------------|---------------|
| 1 Onboarding | `b1_splash` | 3 | 3 | 3 | 1 | med | **F** brand/type | dots, collage, signup | AF1 |
| 2 Home/Discover | `b2_home` | 3 | 3 | **4** | 1 | **high** | **F** | thumbs, chips, sections | AF1 |
| C00 Empty | `b_c00_empty` | 3 | 3 | 3 | 1 | high | **F** | verify vs board | AF1 |
| 3 Input | `b3_open` | 2 | 3 | 2 | 1 | med | **F** | chooser, gallery density | AF2 |
| 4 Understanding | `b4_selected` | 2 | 3 | 2 | 1† | med | **F** | checklist, chips | AF2 |
| 5/6/7/Diff | `b5_choose` | 3 | 3 | 2 | **5** | high | **F** | style grids, sheets | AF3 |
| C04–C14 | `b_c04` | 3 | 3 | 3‡ | **5** | high | **F** | C05–C14 packs | AF4 |
| 13–17 Media | `b13_hub` | 3 | 3 | 2 | **5** | high | **F** | cutout/motion/tool/collage/export | AF5 |
| 19 Generation | `b19_start` | 2 | 3 | 1 | 2† | med | **F** | all recovery art | AF2 |
| 8 Save/Share | `b8_ready` | 2 | 3 | 1 | 1 | med | **F** | share icons, privacy | AF6 |
| 18/26/collab | `b18_browse` | 2 | 3 | 2 | 1 | **high** | **F** | thumbs, search, collab art | AF6 |
| 9 Profile | `b9_open` | 2 | 3 | 2 | 1 | med | **F** | avatar/badges/albums | AF7 |
| 10/20–25 Me | `b10_discover` | 2 | 3 | 2 | 1 | **high** | **F** | Pro/safety/privacy/auth/credits/sub/notif | AF7 |
| 12 Journey | `b12_intro` | 3 | 3 | 3 | 1 | med | **P** milestone thumbs | milestone chrome | AF8 ✓ |
| 27 Community | `b27_feed` | 3 | 3 | 3 | 1 | med | **P** feed+guidelines | feed, guidelines | AF8 ✓ |
| 28–30 Recovery | `b28_banner` | 3 | 3 | 3 | 1 | med | **P** banner/lifecycle | banner/a11y/lifecycle | AF8 ✓ |

\*TypeDebt = relative `MaterialTheme.typography` usage in family hosts.  
†Custom edges, not `BoardSoftEdgeGate`.  
‡C07 rich (8 files); most C-tools = 1 hero.

**Worst-first within waves (frozen for Phase 6):**  
AF8 social → AF7 me → AF6 library → AF2 F19/input → AF3 create grids → AF5 media → AF4 C-tools → AF1 home/onboard (strongest assets today).

---

## 3. Asset sparsity audit (`board_*`)

**Total packed drawables: 60** (54 PNG + 6 B3/fail vectors as of Phase 4)

| Prefix | Count | Notes |
|--------|------:|-------|
| b2 | 8 | Strongest Discover pack |
| c07 | 8 | Scene pack |
| b3_ai / image_fail | 6 | Phase 4 vector stubs |
| c04 | 3 | subject/cutout/beach |
| ba | 2 | before/after shared |
| c00 / c05 / onboard | 2 each | |
| Most other flows | **1** | b5,b6,b9–b11,b13–b16,b18,b20,b21,b23,b25,b27,b28, c06,c08–c12,c14, diff |
| **Missing photo packs** | — | b1,b4,b7,b8,b12,b17,b19 photos,b22,b24,b26,b29,b30,collab |

**Implication:** Design still owes photo densification; eng wired unique grids from existing PNG + B3 vectors into gates/F19/create/C-tools.

Full list: see repo `drawable/board_*` (enumerated 2026-09-08; vectors added Phase 4).

---

## 4. Debt sheets

### 4.1 SoftEdgeGate

**HAS:** Flow5/6/7/Diff, C04–C14 (+ CToolsHost), Flow13–17, Flow4/F19 (offline/credits/safety), library 8/18/26/collab, me 9–11 (+ route helper), social 12/27 (+ route helper via `BoardTrySoftEdgeGate`).

**MISSING (custom / non-SoftEdge):** Flow1, 2, C00 permission/marketing edges; Flow28 custom offline; F19 busy/timeout/failed (still RecoveryState).

### 4.2 Typography debt

- **`MaterialTheme.typography` hits under `ui/board/`: ~553**
- **`BoardTitleText` / `BoardBodyText` / `BoardCaptionText`:** used almost only inside `BoardKits` / `BoardGate` — **not** adopted by flow hosts
- Worst files (hits): Flow27 (22), Flow2 (20), Flow28 (19), Flow24 (19), Flow17/12/C00/30/18/26/10 (16), Flow5/25/16/23 (15)

### 4.3 Icon debt

- **`Icons.*` hits under `ui/board/`: ~828**
- Heavy: Flow15 (33), Flow18 (33), Flow11 (32), Flow10 (31), Flow23 (34), Flow17 (28), FlowC10 (27), Flow9 (27), Shell (8 Material nav)

### 4.4 Edge-to-edge / insets

- `MainScaffold` + `BoardMainShell`: `WindowInsets.safeDrawing` on Scaffold / NavigationBar / expanded rail.
- Search (`b26_search`): `imePadding()` on field column.

### 4.5 Sample large-font / inset smoke (code review only)

| Screen | Note |
|--------|------|
| `BoardMainShell` | `WindowInsets.safeDrawing` applied (Phase 3) |
| Create hub | Uses Material type; light surfaces |
| SoftEdge gate | Has BoardType text helpers; Material outline icons for kinds |

---

## 5. Token gap sheet (Bible × Identity × Board)

| Token | Bible | Identity / Color.kt | BoardColors today | Gap |
|-------|-------|---------------------|-------------------|-----|
| Canvas | Neutral bg | `#0B0B10` dark | `#FAFAFC` light | **Major** |
| Surface | Surface | `#15151C` | `#FFFFFF` | **Major** |
| Surface 2 | Variant | `#20202A` | `#F0F0F5` | **Major** |
| Text primary | — | `#FFFFFF` dark | `#11111A` | **Major** |
| Text secondary | — | `#B7B7C7` | `#646477` | Medium |
| Border | Divider | `#30303A` | missing dedicated | Medium |
| Accent / CTA | Brand+Accent | Purple `#6848FF`→Pink `#F229A5` gradient | Flat blue `#3B82F6` | **Major** |
| Board accentPurple | — | `#6848FF` | `#7C3AED` | Drift |
| Board accentPink | — | `#F229A5` | `#F472B6` | Drift |
| Warning / Info | Semantic | Warning `#F4B740` | warning missing on Board | Medium |
| Spacing | XS4…XXXL64 | pj_spacing_* | ad-hoc | **Major** |
| Radius sheet | **28** | pj_radius | max card20 | Medium |
| Type body | **16sp** | — | **14sp** | **Major** |
| Type hero/headline | 32/28/24 | — | missing | **Major** |
| Light + dark | Both first-class | Color.kt has both | Board light-only scheme | **Major** |
| Motion tokens | fast/normal/slow | MOTION_MANIFEST | pulse only | **Major** |

---

## 6. Component gap sheet (Bible §57 vs Board)

| Bible component | Board today | Gap |
|-----------------|-------------|-----|
| PixiButton | `BoardPrimary/Secondary/OutlinedButton` | No tertiary/destructive kit; no full state matrix |
| PixiIconButton | ad-hoc IconButton | No shared kit |
| PixiTextField | ad-hoc | Missing kit |
| PixiCard | Material Card ad-hoc | Missing kit |
| PixiChip | ad-hoc / FilterChip | Missing kit |
| PixiTopBar | `BoardScreenHeader` | Partial |
| PixiBottomBar / Nav | `BoardMainShell` | Material icons |
| PixiBottomSheet | Material sheets ad-hoc | Missing shared Pixi sheet (r28) |
| PixiDialog | ad-hoc | Missing |
| PixiSnackbar | rare | Missing |
| PixiLoading | `BoardProgressBlock` / Generate | Partial |
| **PixiSkeleton** | **none** | **Missing** |
| **PixiEmptyState** | **none** | **Missing** |
| **PixiErrorState** | SoftEdge / custom | Partial |
| PixiImage | `BoardHeroImage` / `BoardArtImage` | Partial |
| PixiAvatar | Flow9 ad-hoc | Missing kit |
| PixiSection | — | Missing |
| PixiTab | shell tabs | Partial |
| PixiProgress | `BoardProgressTrack` / SoftPulse | Partial |
| SoftEdge gate | `BoardSoftEdgeGate` | Exists; not app-wide |

---

## 7. Screenshot baseline SOP

```powershell
$env:GRADLE_USER_HOME = "...\MyApplication\.gradle-local"
# Phone API 35 — settle ≥12–14s after --es board_screen (DataStore seed + nav)
# First Phase 0 attempt at 5s captured splash/loading only (purple dot).
adb -s <serial> shell am start -W -n com.pixijoy.studio/.MainActivity --es board_screen <id>
# Output: docs/board_review/phase0_baseline/
```

**Known issue:** gate `LaunchedEffect` may race `board_screen` navigation and land on main Home shell for some IDs. Treat captures as visual chrome baseline; fix deep-link race in Phase 1/2 if needed. Compare against rematch PNGs manually.

Phase 0 capture set (L3 entries):  
`b1_splash`, `b2_home`, `b_c00_empty`, `b3_chooser`, `b4_result`, `b5_choose`, `b_c04`, `b13_photo`, `b19_start`, `b8_ready`, `b18_browse`, `b26_open`, `b9_open`, `b10_discover`, `b12_intro`, `b27_feed`, `b28_banner`, `b_create_hub`, `b6_choose`, `b7_sheet`, `b_diff_choose`

Tablet: repeat on `Pixel_Tablet` → `docs/board_review/phase0_baseline_tablet/`.

---

## 8. Phase 0 exit checklist

- [x] Bible linked + §52 scored system-wide  
- [x] L3 family score table + wave freeze  
- [x] `board_*` sparsity audit (54 files)  
- [x] SoftEdge / typography (~553) / Icons (~828) debt  
- [x] Token gap sheet  
- [x] Component gap sheet (§57)  
- [x] Edge-to-edge finding (0 insets in board)  
- [x] Phone PNG baseline captured under `phase0_baseline/` (**21 unique** screens, Pixel_6_Pro API 35, 14s settle)  
- [x] Tablet Expanded baseline under `phase0_baseline_tablet/` (**21** screens, Pixel_Tablet API 35, 2560×1600)

**Phase 0 complete (phone + tablet).**  

**Phase 1 (2026-09-08) landed:** `PixiSpacing`/`PixiRadii`/`PixiMotionMs` Bible-aligned · `BoardColors.dark()` default + `light()` stub · `BoardType` Bible scale (body 16) · dark `BoardThemeProvider` · `PixiJoyTheme` default Dark · AI gradient CTA · Tertiary/Destructive · Skeleton/Empty/Error kits · [BOARD_COMPONENT_MAP.md](BOARD_COMPONENT_MAP.md).

**Phase 2 (2026-09-08) landed:** `pj_ic_*` nav (shell + MainScaffold) · `BoardAiSparkleIcon` on Talk/credits/home AI tools · tip/credit dark contrast · Create hub `BoardType` · processing block on `processingSurface` · soft nav elevation.

**Phase 3 (2026-09-08) landed:** `BoardTrySoftEdgeGate` on library/me/social hosts · SoftEdge catalog edges for 8/18/26/collab/9–11/12/27 · Flow4/F19 offline·credits·safety on SoftEdge language · `BoardEmptyState`/`BoardSkeletonCard` on creations + search · `WindowInsets.safeDrawing` on MainScaffold + BoardMainShell · IME padding on search · demo captions on `BoardGenerateBlock` · Flow3 permission why-tips.

**Phase 4 (2026-09-08) landed:** B3 vector stubs (`board_b3_ai_*` + fail placeholder) · denser unique `BoardFlowArt` rematch/library packs · Diff 12-style uniqueness · Flow5 style cells · C-tool option dedupe · F19 progress/recovery heroes · empty-state hero · Bible §29 fail path on `BoardArtImage`.

**Phase 5 (2026-09-08) landed:** `PixiMotionMs` Bible bands · `BoardLottie` + `BoardMotionKey` (manifest) Compose stubs · reduced-motion + selective haptics · BA peek + button alternative · favorite micro-pop · bound on F19/F5/F8/F26 · unit tests for tokens/keys.

**Phase 6 (2026-09-08) landed:** L2 rematch polish waves worst-first AF8→AF1 · journey milestone thumbs · denser feed/guidelines/offline/lifecycle heroes · me/library/media/create/C-tool/home `BoardType` + reused `BoardFlowArt` density (no new photo packs).

**Phase 7 (2026-09-08) landed:** System light/dark `BoardThemeProvider` · completed light tonal splash/processing · Expanded `BoardListDetail` (creations) · `BoardPreviewControls` (Beautiful/Funny) · `BoardCdn`/`BoardRemoteImage` Coil stub · `BOARD_LIVE_GENERATE` + `BoardSoftGenerate` soft fallback · demo captions gated · font-scale + RTL DoD smoke · L3 matrix Ready for your §52/§53 approve.

**Post–Phase 7 IA pass (2026-09-08):** 5-tab shell Home·Explore·Create·Community·My Stuff · first-viewport hero density (`BoardTabViewport`) · `BoardFlowChrome` Back+Next · CreateForMe/Meme → board redirect · deleted unused Home/Explore/Flow2 screens.

**Bible chrome unify (2026-09-08):** Settings / Privacy / Terms / offline·slow banners / Flow Index rethemed off light `BrandPink` onto Identity dark `MaterialTheme` · `BoardScreenHost` tracks shell luminance (no system-light flip vs dark shell).


**Q2 Bible polish (2026-09-09) eng baseline:** Beautiful / C04 / F15 — SoftEdge honesty + `BoardType`/`BoardTitleText` headers; F15 `BoardWideFrame`; GenerationProgress Lottie on processing; ResultFooter on F15 SaveShare. **Do not claim user L3 Approved** — eng Ready only.
Next: **Your L3 Approved ticks** in [BOARD_L3_REVIEW.md](BOARD_L3_REVIEW.md) — continue host `BoardType` + `pj_ic_*` adoption · enable live via `BOARD_LIVE_GENERATE` + Gemini · real CDN/Lottie packs.

---

## 9. Recommended next step

**L3 sign-off** — phone + tablet vs rematch PNGs; mark Approved/Blocked in [BOARD_L3_REVIEW.md](BOARD_L3_REVIEW.md). Walk the 5 tabs on Compact: primary CTAs visible without scroll; Back/Next on create wizards.
