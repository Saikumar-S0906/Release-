# Additional Flow Catalog (C-series)

Companion journeys aligned with core flows **1–30** chrome. C-flows are **Create Tools** (+ C00 home empty-state, C14 Community remix). Core boards win on nav / tokens / IA.

Board assets live in [`flows/additional/`](../flows/additional/).

---

## Create hub IA

Create tab (`b_create_hub`) is **Tools** (default) | **AI**. C-flows are Tools (+ C00 empty home, C14 Community remix).

| Segment | Items | Route / host |
|---------|--------|----------------|
| **Tools** | Editor, Restore, Expand, Remove BG, Stickers, People, Scene, Move, Talking, Slideshow, Video→GIF, Collage, Greeting, Portrait, Remix, Export | `BoardRouteMap` media / createExtra |
| **AI** | Beautiful, Funny, Different, Tell PixiJoy | `board_ui/` via `BoardRouteMap.createIntent` |

Locked shell: Home · Explore · Create · Community · My Stuff.

---

## C-flow registry

| ID | Title | Board file | Entry points | Reuses | Phase E algorithm (target) | UI status |
|----|-------|------------|--------------|--------|----------------------------|-----------|
| **C00** | Empty Home & First Creation | `C00_Empty_Home_First_Creation.jpg` | Home zero-state, sample photo CTA | Flow 2, Discover, Auth guest/sign-up | N/A (UX) | Wired |
| **C04** | Remove Background & Object | `C04_Remove_Background_Object.jpg` | Create Tools, Beautiful advanced, Editor More | Flow 5, 15, 19, share | On-device segmentation + cloud SAM/inpaint | Wired |
| **C05** | Restore Old Photo | `C05_Restore_Old_Photo.jpg` | Create Tools (above fold) | Flow 19, before/after, share | Restore → colorize → face enhance → upscale | Wired |
| **C06** | Face Swap & Recast | `C06_Face_Swap_Recast.jpg` | Create Tools, Funny path | Flow 6, Safety consent, 19 | Face detect → consent → swap | Wired |
| **C07** | Change Scene / Relocate | `C07_Change_Scene_Relocate.jpg` | Create Tools, Tell PixiJoy suggestions | Flow 7, 19 | Cutout + scene / generative relocate | Wired |
| **C08** | Add or Remove People | `C08_Add_or_Remove_People.jpg` | Create Tools, Tell suggestions | Flow 7, mask UI, Safety | Mask → inpaint add/remove | Wired |
| **C09** | AI Portrait & Headshot | `C09_AI_Portrait_Headshot.jpg` | Create Tools | Legacy `portrait_studio` → redirect | Identity-preserving style packs | Wired |
| **C10** | AI Expand, Fill & Upscale | `C10_AI_Expand_Fill_Upscale.jpg` | Editor More, Create Tools | Flow 15, Pro HD gate | Outpaint + 2x/4x upscale | Wired |
| **C11** | Talking Photo | `C11_Talking_Photo.jpg` | Create Tools | Legacy `talking_photo` → redirect | Face mesh / lip-sync; MP4+GIF | Wired |
| **C12** | Greeting Card & Festival | `C12_Greeting_Card_Festival.jpg` | Create Tools, Explore seasons | Collage purposes, share WhatsApp | Template + text + festival packs | Wired |
| **C14** | Remix from Community | `C14_Remix_from_Community.jpg` | Community Remix button | Flow 27, 8 publish, attribution | Style ref + credit metadata | Wired |

**Functional wiring (current):** Board C-tool hosts + `CreateExtraPipeline` / `CreateExtraToolMapper`
(gallery/camera/sample → credits → on-device algos). Legacy `CreateExtraViewModel` / `UiCreateExtraHost` removed in Phase 2 cleanup.

| Tool | Algorithm |
|------|-----------|
| C04 Remove BG | ML Kit selfie seg; options → blur BG / erase / color composite |
| C05 Restore | ColorMatrix lift + enhance + youthful / upscale options |
| C06 Face | ML Kit faces → swap 2 faces (feather blend) or recast effect |
| C07 Scene | Cutout + prompt gradient composite |
| C08 People | Face detect + border inpaint remove |
| C09 Portrait | PortraitStudioProcessor effects + keepFace intensity |
| C10 Expand | Reflective/edge outpaint + 2×/4× upsample |
| C11 Talking | Animator frames → `ReelExporter` MP4 on save |
| C12 Greeting | Story card templates |
| C14 Remix | Enhance + style tilt; attribution string in state |

**Aliases (not separate routes):** C01≈Beautiful, C02≈Funny, C03≈Different, C13≈Stickers (core hosts).

**Deferred:** Kids mode (no named board).

---

## Route contract (Phase B+)

```
board_ui/{screenId}   // via BoardRouteMap.createExtra(toolKey)
```

| tool key | C-flow | board id (typical) |
|----------|--------|--------------------|
| `empty_home` | C00 | `b_c00_empty` |
| `remove_bg` | C04 | `b_c04` |
| `restore` | C05 | `b_c05` |
| `face_swap` | C06 | `b_c06` |
| `scene` | C07 | `b_c07` |
| `people` | C08 | `b_c08` |
| `portrait` | C09 |
| `expand` | C10 |
| `talking` | C11 |
| `greeting` | C12 |
| `remix` | C14 |

---

## Competitive UX DoD (every C host)

- One-tap entry from Home/Create Tools
- Sample photo for first-time users
- Option grid (never single dead-end)
- Strength / keep-pose / keep-face where board shows them
- Flow 19 progress + cancel + edges
- Before/after or ≥4 variations
- Credit cost chip on generate
- Result: Remix · Save · Share · Export · Make Sticker (as relevant)
- Consent gate for C06 / C08
- Share: Gallery, WhatsApp, Reels/Stories, Community

---

## Rematched core boards (Phase C fidelity)

**Canonical = `REMATCH_*` only.** Older `FLOW_*` pack copies were removed.

| File | Maps to |
|------|---------|
| `REMATCH_05_Make_it_Beautiful.png` | Flow 5 |
| `REMATCH_06_Make_it_Funny.png` | Flow 6 |
| `REMATCH_07_Tell_PixiJoy.png` | Flow 7 |
| `REMATCH_13_Sticker.png` | Flow 13 |
| `REMATCH_14_Move.png` | Flow 14 |
| `REMATCH_EXTRA_Different.png` | EXTRA Different |

---

## Device boards (Phase D)

From `PixiJoy_02a`: D01 Phones · D02 Tablets · D03 Foldables · D04 Responsive Home — validate against [UI_RESPONSIVE_QA.md](UI_RESPONSIVE_QA.md).

---

## Phase E — AI / billing wire-up (deferred)

UI hosts are **UI Done / soft AI**. Real pipelines land in a later milestone:

| Tool | Target pipeline |
|------|-----------------|
| C04 | On-device ML Kit/MediaPipe cutout; cloud SAM/inpaint for objects |
| C05 | Restore → colorize → face enhance → optional upscale |
| C06 / C08 | Face detect → consent gate → swap/inpaint; safety log |
| C07 | Cutout + scene composite / generative relocate |
| C09 | Identity-preserving style packs + 1:1 crop |
| C10 | Outpaint + Real-ESRGAN-class 2x/4x; Pro HD gate |
| C11 | Face mesh + lip-sync; MP4/GIF; duration presets |
| C12 | Template engine + festival packs + WhatsApp share |
| C14 | Style ref embedding + attribution metadata on publish |

Until then, AI generate paths use Flow 19 progress and credit chips. Local Tools (cutout, restore, editor, stickers, export) stay on-device without AI credit chips.
