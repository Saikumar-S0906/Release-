# R5 ML packs + F1 track contract

**Status:** Pack runtime + LiteRT bind on matching contracts after SplitCompat. E4 frame-interp and F1 track stay unbound on purpose (no artifact yet).
**Pairs with:** `docs/REMAINING_MASTER_PLAN.md` Wave E / F1, `docs/FUNCTIONALITY_MATRIX.md`.
**Hard rule:** pack file + checksum ≠ neural engine. Telea / bicubic / frame-hold are never labeled neural.

## Runtime (`:ml`)

`:ml` owns install state, `PackSplits` (SplitCompat), and the Hilt singleton `PackEngineHooks`. Play feature modules (`ml_inpaint`, `ml_superres`, `ml_denoise`, `ml_interp`, `ml_track`) own model files only. `:photo` and `:movie` call the facade and do not depend on a pack module or LiteRT. Session UI observes install state and pack CTAs; it does not call `register`. Super-res and Denoise Missing shows Download. Inpaint, FrameInterp, and Track have no hosted URL, so their Missing plate shows **Install from file** and does not show Download.

- Install state: missing / installing / ready / failed / storage error. **Ready** means the file is on the device and its SHA-256 verified. **Neural** is `PackRuntime.engineReady` (Ready and a bound interpreter). Ready alone does not enable Super-res Apply, Denoise Apply, or neural inpaint Apply
- SHA-256 sidecar on sideload. Every live `PackSpec.expectedSha256` is null, so **Install from file** records the copied file's own SHA. Super-res and Denoise HTTPS Download still refuse a body that misses `downloadSha256` before Ready. Inpaint, FrameInterp, and Track have no `downloadUrl` and no SHA pin
- Sideload: **Install from file** (`GetContent`) copies into cache then checksum-installs; fallback folder `filesDir/ml_incoming/{fileName}` (also app external files)
- HTTPS fallback (user-tapped Download only). Photos are not uploaded. Pins below are the only hosted artifacts.
  - Super-res `superres.tflite` / `ml_superres`: `https://github.com/captain-pool/GSOC/releases/download/2.0.0/compressed_esrgan.tflite` SHA-256 `27ed316318cac65fe350ccdb3c1e19ac08c512f4aa2fc23b1a351ce334809c3f`. Graph is FLOAT32 NHWC `1×180×320×3` → `1×720×1280×3` (4×). `PackModelContract.superResScale` accepts 2× or 4× NHWC RGB.
  - Denoise `denoise.tflite` / `ml_denoise`: `https://raw.githubusercontent.com/raspberrypi/AI_denoise/master/networks/unet_rgb_fast.tflite` SHA-256 `53f0112f248b18ae518af64942ea4741f5b22ccdc4af5aea0ff452875f17fb84`. Graph is FLOAT32 NHWC `1×256×256×3` → same size. `PackModelContract.isDenoiseSameRgb` plus FLOAT32 at bind. A 2×/4× graph cannot register as denoise.
- Inpaint has a sideload path only (`inpaint.tflite` / `ml_inpaint`). No HTTPS URL and no invented weights. Missing chrome is **Install from file** (or copy into `filesDir/ml_incoming/`). Ready after that copy is the verified file only (`InpaintStatus.ReadyFileOnly`). Neural Apply stays off until `PackRuntime.engineReady` after an RGB+mask FLOAT32 bind. Nearby-pixel Apply stays on and runs Telea (`PhotoOp.Fill.neural == false`). Telea is never labeled neural.
- Failed Download for packs without a URL tells the user to **Install from file** (Play modules are not in this APK).
- `PixiJoyApp.attachBaseContext` calls `PackSplits.install`. `onCreate` binds install state only and registers nothing
- After SplitCompat, the studio shell calls `PackFeatureEntry.watchVerifiedModels`. `PackLiteRt` opens a file only when LiteRT is up, the pack is Ready, and the bytes match the recorded SHA-256 (and `expectedSha256` when set). It registers only if `PackModelContract` accepts the graph. A miss releases the capability (sink null). Chips read `PackRuntime.engineReady`: Ready alone is not neural
- `PackLiteRt` builds Super-res (2×/4× FLOAT32 NHWC RGB), Denoise (same-size FLOAT32 RGB), and Inpaint (FLOAT32 RGB+mask) interpreters. Interp / track never bind — frame-hold is not labeled neural
- Engines call `PackEnhance` and `TrackFacade.follow` / `FrameInterp.interpolatedFrames`
- A null try result leaves Telea / bicubic / frame-hold in place, unlabeled as neural

## Photo

| ID | Graph | Engine |
|----|-------|--------|
| E1 Fill | `I` (Telea) / `I,…;n` (neural request) | Nearby-pixel Telea unless the op asked for neural and a sideloaded inpaint TFLite matches RGB+mask and LiteRT binds. Ready file alone does not turn neural Apply on |
| E2 Super-res | `SR,2` / `SR,4` | Distilled ESRGAN via Download (pinned SHA) or a sideload that passes 2×/4× NHWC RGB + FLOAT32 |
| E3 Denoise | `N,{amount}` | Raspberry Pi UNet via Download (pinned SHA) or a sideload that passes same-size RGB + FLOAT32 |

## Movie

| ID | Contract | Engine |
|----|----------|--------|
| E4 | `FrameInterp.mlAvailable` ← pack engine | **HOLD.** `PackLiteRt` leaves `FrameInterp` as `Unit`. No `downloadUrl`, no SHA pin, no shape gate, no `interp.tflite` in the repo. Chip stays off `engineReady` until Architect lands a real artifact |
| F1 | `ClipTrack` field on `TimelineClip` | **HOLD.** `PackLiteRt` leaves `Track` as `Unit`. No `downloadUrl`, no SHA pin, no shape gate, no `track.tflite` in the repo. No motion follow |

## E5 size

- `selfie_segmenter.tflite` stays in `:ml` (~244 KB) for MediaPipe `ImageSegmenter` only (`VisionFacade`). It is not a `PackId`, not in `PackCatalog`, and `PackLiteRt` does not bind it. Moving it would not change the pack gate
- `./gradlew :app:assembleRelease :app:reportApkSize`
- **Measured 2026-09-25:** minified `app-release-unsigned.apk` **131.11 MB**. Over the ~40 MB aspiration because of Media3 + MediaPipe JNI already in the base app, not because of pack models (none ship in the APK)
- Gate for pack ship: do not add inpaint/SR/denoise/interp/track weights to the base APK

## Device smoke — Super-res / Denoise

Steps for when a desktop or KVM device is available. Do not record a pass in this file. No Maestro or device result is claimed.

1. Install a debug build on a device or emulator that can initialize Play services LiteRT. If LiteRT init fails, stop after step 4.
2. Photo → Retouch → Super-res. Before Download, the status is not “Neural ready” and Apply (`session_apply_super_res`) is off.
3. Tap Download. When the status says downloaded and verified (Ready), read `filesDir/ml_packs/superres/1/superres.tflite.sha256` and confirm it equals `PackCatalog.SUPERRES_DOWNLOAD_SHA256` (`27ed316318cac65fe350ccdb3c1e19ac08c512f4aa2fc23b1a351ce334809c3f`). Apply must still be off while the copy says the neural engine is not bound.
4. If Play services LiteRT is unavailable, the status stays Ready without neural and Apply stays off. Stop.
5. When LiteRT is up and the FLOAT32 2×/4× NHWC contract accepts the graph, the status changes to “Neural ready” and Apply turns on. That is `PackRuntime.engineReady(SuperRes)`.
6. Apply 2× once. Export uses the same op. If the interpreter is released, the status returns to Ready-without-neural and Apply turns off. The photo is left unchanged.
7. Repeat steps 2–6 for Denoise (`session_apply_denoise`). The sidecar is `filesDir/ml_packs/denoise/1/denoise.tflite.sha256` and must equal `PackCatalog.DENOISE_DOWNLOAD_SHA256` (`53f0112f248b18ae518af64942ea4741f5b22ccdc4af5aea0ff452875f17fb84`). The bind is same-size FLOAT32 RGB. A 2×/4× graph must not turn Denoise Apply on.
8. Do not use this list for FrameInterp, Track, or an Inpaint HTTPS pin. Those packs have no hosted URL or SHA. Inpaint stays sideload-only. E4 and F1 stay HOLD (`PackLiteRt` is `Unit`).

## Device smoke — Inpaint (sideload only)

Steps for when a desktop or KVM device is available. Do not record a pass in this file. No Maestro or device result is claimed. There is no Inpaint HTTPS URL and no SHA pin.

1. Photo → Retouch → Fill. The status is the sideload missing copy (“not hosted for download”). The primary control is Install from file (`session_fill_pack_download_from_file`). Download (`session_fill_pack_download`) is absent.
2. Nearby-pixel Apply (`session_apply_fill`) is on. Neural Apply (`session_apply_fill_neural`) is off. The nearby-pixel result is Telea and is not neural.
3. Copy `inpaint.tflite` into `filesDir/ml_incoming/` or use Install from file. When the status says installed and verified (Ready), the file is checksum-recorded only. Neural Apply stays off until the status says the sideloaded model is bound.
4. If Play services LiteRT is unavailable, or the graph is not FLOAT32 RGB+mask, the status stays Ready without neural and Neural Apply stays off. Nearby-pixel Apply stays on.
5. Neural Apply turns on only when `PackRuntime.engineReady(Inpaint)` after `PackLiteRt` binds that contract. Do not treat a Ready file as neural.

## Maestro

- `.maestro/packs/fill-pack-status.yaml`
- `.maestro/packs/superres-denoise.yaml`
- `.maestro/movie/f1-track-roto.yaml`
- Play download: `.maestro/YELLOW-ml-packs.md`
