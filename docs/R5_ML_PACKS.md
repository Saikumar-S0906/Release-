# R5 ML packs + F1 track contract

**Status:** Pack runtime + LiteRT bind on matching contracts after SplitCompat. E4 frame-interp and F1 track stay unbound on purpose (no artifact yet).
**Pairs with:** `docs/REMAINING_MASTER_PLAN.md` Wave E / F1, `docs/FUNCTIONALITY_MATRIX.md`.
**Hard rule:** pack file + checksum ≠ neural engine. Telea / bicubic / frame-hold are never labeled neural.

## Runtime (`:ml`)

`:ml` owns install state, `PackSplits` (SplitCompat), and the Hilt singleton `PackEngineHooks`. Play feature modules (`ml_inpaint`, `ml_superres`, `ml_denoise`, `ml_interp`, `ml_track`) own model files only. `:photo` and `:movie` call the facade and do not depend on a pack module or LiteRT. Session UI observes install state and download CTAs; it does not call `register`.

- Install state: missing / installing / ready / failed / storage error
- SHA-256 sidecar on sideload; optional pinned hash in `PackCatalog`
- Sideload: **Install from file** (`GetContent`) copies into cache then checksum-installs; fallback folder `filesDir/ml_incoming/{fileName}` (also app external files)
- HTTPS fallback (user-tapped Download only). Photos are not uploaded. Pins below are the only hosted artifacts. `expectedSha256` stays null so **Install from file** records the copied file's own SHA; Download still refuses a body that misses `downloadSha256`.
  - Super-res `superres.tflite` / `ml_superres`: `https://github.com/captain-pool/GSOC/releases/download/2.0.0/compressed_esrgan.tflite` SHA-256 `27ed316318cac65fe350ccdb3c1e19ac08c512f4aa2fc23b1a351ce334809c3f`. Graph is FLOAT32 NHWC `1×180×320×3` → `1×720×1280×3` (4×). `PackModelContract.superResScale` accepts 2× or 4× NHWC RGB.
  - Denoise `denoise.tflite` / `ml_denoise`: `https://raw.githubusercontent.com/raspberrypi/AI_denoise/master/networks/unet_rgb_fast.tflite` SHA-256 `53f0112f248b18ae518af64942ea4741f5b22ccdc4af5aea0ff452875f17fb84`. Graph is FLOAT32 NHWC `1×256×256×3` → same size. `PackModelContract.isDenoiseSameRgb` plus FLOAT32 at bind. A 2×/4× graph cannot register as denoise.
- Inpaint has a sideload path only (`inpaint.tflite` / `ml_inpaint`). No HTTPS URL and no invented weights. Bind still requires RGB+mask and FLOAT32.
- Failed Download for packs without a URL tells the user to **Install from file** (Play modules are not in this APK).
- `PixiJoyApp.attachBaseContext` calls `PackSplits.install`. `onCreate` binds install state only and registers nothing
- After SplitCompat, the studio shell calls `PackFeatureEntry.watchVerifiedModels`. `PackLiteRt` opens a file only when LiteRT is up, the pack is Ready, and the bytes match the recorded SHA-256 (and `expectedSha256` when set). It registers only if `PackModelContract` accepts the graph. A miss releases the capability (sink null). Chips read `PackRuntime.engineReady`: Ready alone is not neural
- `PackLiteRt` builds Super-res (2×/4× FLOAT32 NHWC RGB), Denoise (same-size FLOAT32 RGB), and Inpaint (FLOAT32 RGB+mask) interpreters. Interp / track never bind — frame-hold is not labeled neural
- Engines call `PackEnhance` and `TrackFacade.follow` / `FrameInterp.interpolatedFrames`
- A null try result leaves Telea / bicubic / frame-hold in place, unlabeled as neural

## Photo

| ID | Graph | Engine |
|----|-------|--------|
| E1 Fill | `I` | Telea unless a sideloaded inpaint TFLite matches RGB+mask and LiteRT binds |
| E2 Super-res | `SR,2` / `SR,4` | Distilled ESRGAN via Download (pinned SHA) or a sideload that passes 2×/4× NHWC RGB + FLOAT32 |
| E3 Denoise | `N,{amount}` | Raspberry Pi UNet via Download (pinned SHA) or a sideload that passes same-size RGB + FLOAT32 |

## Movie

| ID | Contract | Engine |
|----|----------|--------|
| E4 | `FrameInterp.mlAvailable` ← pack engine | **HOLD.** `PackLiteRt` leaves `FrameInterp` as `Unit`. No `downloadUrl`, no SHA pin, no shape gate, no `interp.tflite` in the repo. Chip stays off `engineReady` until Architect lands a real artifact |
| F1 | `ClipTrack` field on `TimelineClip` | **HOLD.** `PackLiteRt` leaves `Track` as `Unit`. No `downloadUrl`, no SHA pin, no shape gate, no `track.tflite` in the repo. No motion follow |

## E5 size

- `selfie_segmenter.tflite` stays in `:ml` (~244 KB) — moving it would not change the gate
- `./gradlew :app:assembleRelease :app:reportApkSize`
- **Measured 2026-09-25:** minified `app-release-unsigned.apk` **131.11 MB**. Over the ~40 MB aspiration because of Media3 + MediaPipe JNI already in the base app, not because of pack models (none ship in the APK)
- Gate for pack ship: do not add inpaint/SR/denoise/interp/track weights to the base APK

## Maestro

- `.maestro/packs/fill-pack-status.yaml`
- `.maestro/packs/superres-denoise.yaml`
- `.maestro/movie/f1-track-roto.yaml`
- Play download: `.maestro/YELLOW-ml-packs.md`
