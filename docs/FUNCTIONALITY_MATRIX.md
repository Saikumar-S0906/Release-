# PixiJoy — Functionality Matrix

Living inventory after **Full Functionality Wiring** Waves 0–4 (2026-09).  
Tags match the wiring plan: what the control *does*, not how polished the UI looks.

| Tag | Meaning |
|-----|---------|
| **WORKS** | Real side effect (nav, MediaStore, DataStore, Intent, Gemini path when keyed) |
| **SOFT** | Blocked only by external infra (Imagen/cloud key, Play Billing Console, OAuth/Credential Manager, live community backend, remote CDN packs). On-device features must be **WORKS**, not preview toasts. |
| **BROKEN** | Dead click, missing back, or silent decoy — should be empty after Waves 0–4 |

**APK:** `app/build/outputs/apk/debug/app-debug.apk`  
## Related

- Competitor / micro-feature map: [COMPETITOR_COVERAGE_MATRIX.md](COMPETITOR_COVERAGE_MATRIX.md)

---

## Wave 0 — Navigation

| Surface | Tag | Notes |
|---------|-----|-------|
| Tab BackHandler + in-tab stack (`MainScaffold`) | **WORKS** | Pop embedded board history; Home root does not trap exit; other tabs → Home at root |
| `LocalBoardCanPop` / My Stuff browse Back | **WORKS** | Hide Back when stack empty |
| `BoardFlowChrome` / Image modules Back+Close | **WORKS** | Gesture and header agree |
| Create hub Back when not embedded | **WORKS** | |
| Onboarding stack Back | **WORKS** | Does not skip to Home |
| Beautiful ResultCompare / Funny result Back+Close | **WORKS** | |

**Smoke:** Drill Home → create → result → system Back repeatedly; each step pops; leave Create tab from root lands Home.

---

## Wave 1 — Gemini + photo

| Surface | Tag | Notes |
|---------|-----|-------|
| Gallery / photo pick → `BoardMediaSession` | **WORKS** | Real URI or sample drawable |
| Beautiful / Funny generate → `AiOrchestrator.analyzePhoto` | **WORKS** | Needs `GEMINI_API_KEY` + `BOARD_LIVE_GENERATE=true`; consent + credits |
| Free-tier Flash Beautiful result | **WORKS** / **SOFT** | User photo + analysis/tips (not Imagen new image) |
| Funny captions on user photo | **WORKS** | |
| Soft fallback on failure / missing key | **SOFT** | Demo art + local copy; flow completes |
| Dummy key rejection | **WORKS** | `dummy*` keys treated as unset |

**Smoke:** See [PRODUCT_LIVE_SMOKE.md](PRODUCT_LIVE_SMOKE.md) C1.

---

## Wave 2 — Create controls

| Surface | Tag | Notes |
|---------|-----|-------|
| Beautiful brightness / contrast / sat / warmth | **WORKS** | ColorMatrix live preview; bake into gallery save |
| Lighting / color / noise toggles | **WORKS** | Affect preview matrix |
| Face enhance toggle | **WORKS** | Portrait youthful pass via `CreateExtraPipeline.enhanceFaceLook` when on + photo in session |
| Stickers cutout / pack / erase | **WORKS** | `cutoutTransparent` + `generateStickerPack` + `healAt` erase; WhatsApp pack export; SoftEdge overlays stay |
| Stickers Erase/Restore | **WORKS** | Tap heal erase on cutout; brush maps to radiusFrac |
| Collage raster save | **WORKS** | `BoardCollageRasterizer.render` (2–9) → `saveBitmapToGallery` |
| Funny caption bake + Face Swap nav | **WORKS** | `bakeMemeCaptions` on save/share; Face Swap style → `b_c06_photo` |
| C06 face meme composite | **WORKS** | `faceMemeComposite` local remix; ResultFooter + safe image; Back skips processing; SoftEdge stay |
| C07 Relocate | **WORKS** | `relocateToScene` + scene thumbs; ResultFooter; Back → adjust |
| C08 People add/remove | **WORKS** | `peopleEdit` + hasFace → enhanceFaceLook fallback; ResultFooter; Back → consent |
| C09 Portraits | **WORKS** | `faceRecastOrSwap(styleIndex)`; Variations More → adjust; ResultFooter |
| C11 Talking photo | **WORKS** | Ken Burns GIF via `TalkingPhotoAnimator` + `GifEncoder`; poster preview; GIF MediaStore save |
| C14 Remix bake | **WORKS** | `remixStyle` from toggles; save/share WORKS; live publish stays infra SOFT |
| Tell PixiJoy (07) | **WORKS** | `AiOrchestrator.parseCommand` → pipeline map; bitmap save; Back skips generating |
| Edit Further non-adjust tools | **WORKS** | Filters/effects look bake via `applyLook`; text + emoji overlays |
| Different strength + Save | **WORKS** | Grade preview; MediaStore PNG |
| Move customize extras | **WORKS** | Filter look + caption bake to still frame; gallery save |
| Funny sticker / resize tools | **WORKS** | Emoji overlay + captionScale; bake respects scale |
| C04 BG chips: auto / white / black / transparent / blur / gradient / erase | **WORKS** | ML Kit selfie cutout via `CreateExtraPipeline.removeBgOptions` when photo in `BoardMediaSession`; soft art if none |
| C04 Save result bitmap | **WORKS** | `BoardDrawableIo.saveBitmapToGallery` → Pictures/PixiJoyAI |
| C04 edge refine sliders / scene variations / custom color / photo BG / contact shadow | **WORKS** | Options 7–8 + `withContactShadow`; refine toggle |
| C05 restore (colorize / repair / face / full) | **WORKS** | `restoreChain` when photo in session; gallery save |
| C04 / C05 / C10 Phase 1 hardening | **WORKS** | Back never re-enters processing; safe bitmap preview; ResultFooter spine; soft-fallback caption; Beautiful→Restore entry |

**Smoke:** Beautiful → Edit Further → sliders → Apply → Save → check `Pictures/PixiJoyAI`. Different strength → Save. Create → Remove BG → photo → White/Black → processing → Save.

---


## Q2 — Bible polish (Beautiful / C04 / F15)

| Surface | Tag | Notes |
|---------|-----|-------|
| ResultFooter 48dp + navBars | **WORKS** | `BoardResultFooter` / Tertiary buttons `heightIn(min=48)`; `navigationBarsPadding` |
| SoftEdge honesty | **WORKS** | Soft-preview tip removed; `BoardGateScreen` has `navigationBarsPadding`; SoftEdge stays real |
| F15 wide frame | **WORKS** | `BoardWideFrame` on Workspace / Crop / Adjust / Filters / Text / MoreTools (Expanded ListDetail uncapped) |
| F15 SaveShare ResultFooter | **WORKS** | Save/share/edit/export/remake/publish spine; gallery save bitmap kept; publish → `b8_publish` |
| Lottie processing | **WORKS** | `BoardLottie(GenerationProgress)` on Beautiful processing, C04 processing, F15 heal busy |
| C04 Expanded refine/result | **WORKS** | `BoardWideFrame` when Expanded size class (phone Adaptive preserved) |

## Wave 3 — Library I/O

| Surface | Tag | Notes |
|---------|-----|-------|
| Creations download | **WORKS** | `BoardDrawableIo` → MediaStore |
| Favorite / rename / album / save copy | **WORKS** | `BoardLibraryStore` + DataStore |
| Delete → Trash → Restore / Delete forever | **WORKS** | List mutation persists |
| Browse favorites / media filters | **WORKS** | Applied on grid |
| F26 template download | **WORKS** | Gallery write |
| F15 Undo / Redo | **WORKS** | `BoardEditHistory` on workspace; rotate/flip/adjust/heal push snapshots |
| F15 Adjust live grade | **WORKS** | `BoardColorAdjust` ColorFilter preview + history on Apply |
| F15 Filters / looks | **WORKS** | Live `lookColorFilter` + `applyLook` bake on Apply |
| F15 HSL / Curves | **WORKS** | Hue·lightness + shadows·midtones·highlights panels in Adjust |
| F15 Heal / Selective | **WORKS** | Tap heal inpaint + dodge/burn via `ObjectEraser` |
| F15 Crop rotate / flip | **WORKS** | Matrix rotate90 / flip H·V on working bitmap |
| F15 Save working bitmap | **WORKS** | Gallery write of editor document when present |
| F16 collage template prefs | **WORKS** | SharedPreferences layoutIndex + selectedIndices |
| F26 Undo / Redo | **WORKS** | Same `BoardEditorDocument` history on customize |
| F26 customize tools | **WORKS** | Text / sticker / filter cycle / adjust / rotate |

**Smoke:** My Stuff → More → Favorite / Download / Rename / Delete → Trash restore.

---

## Wave 4 — Me / social honesty

| Surface | Tag | Notes |
|---------|-----|-------|
| Privacy Improve AI / Analytics (F21) | **WORKS** | `ConsentRepository` (same as Settings) |
| Notifications master (F11 / F25 push) | **WORKS** | Consent DataStore |
| Category notif toggles | **WORKS** | Consent DataStore category keys |
| Community like / follow / bookmark | **WORKS** | Device DataStore via `BoardSocialStore` + toast |
| Chat / comment / demo share | **SOFT** | Local toast; not published (live community backend) |
| Credits / Pro Pay | **SOFT** | No fake grant; toast + Play Manage Intent |
| Manage in Play (F10 / F24) | **WORKS** | `play.google.com/store/account/subscriptions` |
| OAuth / Google signup | **SOFT** | Preview walkthrough copy + toast |
| Real Play Billing purchase | **SOFT** / pending | Needs Console products + license tester — see C2 |

**Smoke:** Toggle AI consent in Privacy → confirm Settings matches. Pay credits → no balance bump. Like a post → restart → still liked.

---

## Phase 4 — Lite Lightroom / Canva (on-device WORKS)

| Surface | Tag | Notes |
|---------|-----|-------|
| F17 platform export | **WORKS** | `exportForPlatform` crop+resize; gallery via `saveBitmapToGallery` (JPG/PNG/WEBP + quality); Ready shows `exportedBitmap` |
| F15 text bake | **WORKS** | `bakeTextOverlay` on Apply (font / color / align); preview uses working bitmap |
| F15 crop aspect | **WORKS** | Free · 1:1 · 4:5 · 9:16 · 16:9 via `exportForPlatform` / crop |
| F15 structure | **WORKS** | Sharpness + vignette + grain → `applyStructure` on Adjust Apply |
| F5 Edit Further bake | **WORKS** | Look + ColorMatrix + text/emoji baked into `resultBitmap`; gallery prefers result bitmap |
| F26 template save | **WORKS** | Continue bakes grade+text; download saves `BoardEditorDocument.workingBitmap` |
| C12 photo-first greeting | **WORKS** | Photo layer from `BoardMediaSession.decodeBitmap`; FestivalTemplates local greetings |
| Discover template Use | **WORKS** | `b2_template` Use → `b26_customize` (real bake/save path) |
| Discover CDN packs | **SOFT** | Remote pack download / CDN still infra |

**Smoke:** Editor → Adjust vignette/grain → Apply → Export platform → Done preview matches export. Greeting card with session photo. Discover Use lands customize → download.

---

## Still out of scope / remaining SOFT

| Area | Tag | Why |
|------|-----|-----|
| Imagen / true generative Beautiful image | **SOFT** | Free Flash path is analysis only |
| Live community backend | **SOFT** | Local social store only; chat/comment not published |
| Credential Manager OAuth | **SOFT** | Preview until wired |
| Real Play Billing purchase | **SOFT** | Needs Console products + license tester |
| Collab invite send | **SOFT** | Message/perms selectable; demo share honesty |
| Discover CDN packs / Discover remote templates | **SOFT** | Ops / remote CDN — on-device templates WORKS |
| FTL Robo | — | Ops, not board wiring |
| Legacy `ui/flow` hosts | — | Redirected / not expanded; rematch boards win |

**BROKEN:** None known on primary chrome after Waves 0–5 + Phase 4 + Q1. Remaining gaps are infra **SOFT** only — see [WORLD_CLASS_QUALITY_PLAN.md](WORLD_CLASS_QUALITY_PLAN.md).

### Post-wave honesty pass (same day)
- Dead meme/video FilterChips → selectable
- Collab message/perms → editable + selectable + demo labels
- Pro/Credits welcome screens → billing honesty copy (no fake grant)
- C-tool “Generate more” → cycles variation + Preview toast
- Unit test compile fix: `BoardPhase7UnitTest` JVM-safe contracts

---

## Device smoke checklist (Waves 0–1 first)

Install: `app/build/outputs/apk/debug/app-debug.apk`

1. **Cold start** → privacy → onboarding (Back does not skip) → Home.
2. **All 5 tabs** — Home · Explore · Create · Community · My Stuff; Back at tab root behaves as Wave 0.
3. **Create → Beautiful** — real photo → generate → Gemini or soft fallback; Back/Close on result.
4. **Create → Funny** — photo → captions; Back/Close.
5. **Beautiful Edit** — sliders change preview; Save writes graded image (Wave 2).
6. **My Stuff** — download / favorite / trash (Wave 3).
7. **Community** — like/follow toast “Saved on this device” (Wave 4).
8. **Me → Credits/Pro Pay** — honest Play toast, no fake Success (Wave 4).
9. **Settings** — AI/analytics match Privacy Center; Send bug report if anything crashes.

Full product smoke (billing products, ads, URLs): [PRODUCT_LIVE_SMOKE.md](PRODUCT_LIVE_SMOKE.md).

---

## Maintenance

- After any board host change that claims a side effect, update the matching row here.
- Prefer fixing **BROKEN** → **WORKS** or honest **SOFT**; never leave silent success toasts.
