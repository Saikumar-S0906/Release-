# PixiJoy — Competitor Coverage Matrix

Living map of Picsart / Remini / Photoroom / Lightroom / Snapseed / Canva features onto PixiJoy routes.  
Tags match [FUNCTIONALITY_MATRIX.md](FUNCTIONALITY_MATRIX.md): **WORKS** · **SOFT** · **OUT**.

**Product bar:** world-class **photo + AI** depth inside locked IA (Home · Explore · Create · Community · My Stuff). Create = Tools | AI. Not a six-app clone.

**CDN note:** Large sticker/template packs may use Firebase Storage / remote URLs later (`BoardCdn`). Phase 1 ships small owned `pj_bg_*` in the APK — no paid CDN required to start.

---

## Non-goals (OUT)

| Feature | Why OUT |
|---------|---------|
| Canva multi-page docs / brand kits / print pipeline | Breaks photo spine; needs design-suite IA |
| Canva full video timeline editor | Separate product mode |
| Lightroom RAW develop + cloud catalog sync | Catalog product, not Create intent |
| Picsart live effects marketplace / creator commerce | Needs live backend + store |
| Live community chat / published comments | Device social only until backend |

---

## Macro coverage

| Competitor strength | PixiJoy home | Status |
|---------------------|--------------|--------|
| Remini restore / enhance / upscale | Beautiful, C05, C10, face toggle | WORKS (on-device enhance/restore/upscale; Imagen still SOFT) |
| Photoroom BG cutout + studio fills | C04 + F17 export | WORKS (cutout + fills + custom color/photo + contact shadow) |
| Snapseed retouch toolkit | Flow 15 Editor | WORKS (undo/redo, adjust, HSL, curves, heal/selective, rotate/flip, looks) |
| Crop / heal / HSL / curves / selective | F15 | WORKS |
| Undo / redo | F15 / F26 | WORKS |
| Picsart stickers / collage / funny | Stickers, Collage, Funny, Discover | WORKS cutout/pack/erase + collage raster + Funny bake/C06; Discover CDN still SOFT |
| Lightroom looks (lite) | Beautiful / Editor presets | WORKS (`applyLook` + ColorFilter preview) |
| Lightroom structure (lite) | F15 Adjust | WORKS (`applyStructure` sharpness / vignette / grain) |
| Platform export crop | F17 | WORKS (`exportForPlatform` + gallery encode) |
| Canva text-on-photo (lite) | F15 Text | WORKS (`bakeTextOverlay`) |
| Canva templates (lite) | Discover, C12, Collage | WORKS C12 photo-first + F16 prefs + F26 bake save; Discover CDN packs still SOFT |

---

## Micro — C04 Background (Photoroom-class)

| Micro-feature | Route / UI | Engine | Status |
|---------------|------------|--------|--------|
| Auto cutout (person) | C04 options · Auto | ML Kit selfie segmentation | WORKS when photo in session |
| Studio white fill | C04 · Studio white | Cutout + `#FFFFFF` composite | WORKS |
| Studio black fill | C04 · Studio black | Cutout + `#000000` composite | WORKS |
| Transparent PNG | C04 · Transparent | Alpha matte | WORKS |
| Blur background | C04 · Blur BG | PortraitBlurProcessor | WORKS |
| Accent gradient fill | C04 · Gradient | Cutout + purple→pink fill | WORKS |
| Remove object / people | C04 · Remove object | ObjectEraser | WORKS / soft fallback |
| Solid custom color picker | C04 sheet | Composite on user color | WORKS (option 7) |
| Studio scene pack (beach/office) | C04 variations | Cutout composite over scenes | WORKS |
| Custom photo BG | C04 | Gallery / sample composite | WORKS (option 8) |
| Edge refine (feather/precision) | C04 refine sliders | Matte refine on Next | WORKS |
| Contact shadow | C04 | Soft shadow under subject | WORKS (`withContactShadow`) |
| CDN studio packs | BoardCdn | Remote | OUT until packs hosted |

---

## Micro — Remini-class (Phase 1–2)

| Micro | Route | Status |
|-------|-------|--------|
| Face enhance | Beautiful toggle | WORKS (`enhanceFaceLook`) |
| Restore / colorize | C05 | WORKS (`restoreChain`) |
| Upscale 2× / 4× | C10 | WORKS (`expandOrUpscale`) |
| Expand / fill | C10 | WORKS (canvas expand / square pad) |
| Imagen generative Beautiful | Beautiful | SOFT (Flash analyze WORKS) |

---

## Micro — Snapseed-class Editor (Phase 2 + Phase 4)

| Micro | Route | Status |
|-------|-------|--------|
| Brightness / contrast / sat / warmth | Beautiful Edit + F15 | WORKS |
| Crop rotate / flip | F15 | WORKS |
| Crop aspect ratios | F15 | WORKS (1:1 · 4:5 · 9:16 · 16:9 · free) |
| Heal / HSL / curves / selective | F15 | WORKS |
| Structure (sharpen / vignette / grain) | F15 Adjust | WORKS (`applyStructure`) |
| Text overlay bake | F15 Text | WORKS (`bakeTextOverlay`) |
| Undo / redo | F15 / F26 | WORKS |
| Platform export | F17 | WORKS (`exportForPlatform`) |

---

## Micro — Canva-lite / Discover (Phase 4)

| Micro | Route | Status |
|-------|-------|--------|
| Greeting photo-first | C12 | WORKS (`BoardMediaSession` photo + FestivalTemplates) |
| Template customize save | F26 | WORKS (bake working bitmap → gallery) |
| Discover Use → customize | F2 → F26 | WORKS |
| Remote CDN packs | Discover | SOFT |

---

## Micro — Picsart-class Stickers / Collage / Funny (Phase 3)

| Micro | Route | Status |
|-------|-------|--------|
| Sticker cutout | F13 | WORKS (`cutoutTransparent`) |
| Sticker erase / heal | F13 Edit | WORKS (`healAt`) |
| Sticker pack + WhatsApp | F13 pack / share | WORKS (`generateStickerPack` + exporter) |
| Collage raster save | F16 | WORKS (`BoardCollageRasterizer` → gallery) |
| Funny caption bake | F6 result / share | WORKS (`bakeMemeCaptions`) |
| Face Swap style → C06 | F6 → `b_c06_photo` | WORKS |
| C06 local face remix | C06 processing / result | WORKS (`faceMemeComposite`; ResultFooter; not generative ID swap) |
| C07 relocate to scene | C07 processing / result | WORKS (`relocateToScene`) |
| C08 people edit | C08 processing / result | WORKS (`peopleEdit` + hasFace enhance fallback) |
| C09 portrait / face recast | C09 processing / result | WORKS (`faceRecastOrSwap`) |
| C11 talking Ken Burns GIF | C11 processing / save | WORKS (`TalkingPhotoAnimator` + `GifEncoder`) |
| C14 remix bake | C14 processing / result | WORKS (`remixStyle`); live publish SOFT |
| Tell NL command → edit | Flow 7 understanding / generating | WORKS (`parseCommand` → pipeline) |

---

## Libs

| Need | Approach | Buy? |
|------|----------|------|
| Segmentation | ML Kit selfie (shipped) | No |
| Better subject seg | MediaPipe later | Free SDK |
| Blur / GPU looks | Existing processors + RenderEffect later | No |
| Upscale | TFLite / cloud later | Model license TBD |
| Generative | Gemini Imagen when keyed | API usage |
| Motion | Lottie Compose (optional) | No |
| Large packs | Firebase Storage free tier → CDN at scale | Not required day one |

---

## Assets (owned)

| Asset | Location | Status |
|-------|----------|--------|
| `pj_bg_solid_white` | drawable | Phase 1 |
| `pj_bg_solid_black` | drawable | Phase 1 |
| `pj_bg_grad_accent` | drawable | Phase 1 |
| Studio photo scenes | board_c04_* / CDN later | Existing board art |
| Sticker / template CDN | B4–B5 batches | Pending content |

---

## Maintenance

After shipping a micro-feature, update this file and [FUNCTIONALITY_MATRIX.md](FUNCTIONALITY_MATRIX.md) in the same change.
