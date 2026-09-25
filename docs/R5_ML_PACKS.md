# R5 ML packs + F1 track contract

**Status:** Pack runtime landed on `next`. Neural engines stay unbound in the base APK.
**Pairs with:** `docs/REMAINING_MASTER_PLAN.md` Wave E / F1, `docs/FUNCTIONALITY_MATRIX.md`.
**Hard rule:** pack file + checksum ≠ neural engine. Telea / bicubic / frame-hold are never labeled neural.

## Runtime (`:ml`)

`:ml` owns install state, `PackSplits` (SplitCompat), and the Hilt singleton `PackEngineHooks`. Play feature modules (`ml_inpaint`, `ml_superres`, `ml_denoise`, `ml_interp`, `ml_track`) own model files only. `:photo` and `:movie` call the facade and do not depend on a pack module or LiteRT. Session UI observes install state and download CTAs; it does not call `register`.

- Install state: missing / installing / ready / failed / storage error
- SHA-256 sidecar on sideload; optional pinned hash in `PackCatalog`
- Sideload: **Install from file** (`GetContent`) copies into cache then checksum-installs; fallback folder `filesDir/ml_incoming/{fileName}` (also app external files)
- HTTPS fallback (user-tapped Download only): Super-res fetches distilled ESRGAN (`PackCatalog.SUPERRES_DOWNLOAD_URL`) with a pinned SHA-256. Photos are not uploaded.
- `PixiJoyApp.attachBaseContext` calls `PackSplits.install`. `onCreate` binds install state only and registers nothing
- After SplitCompat, the studio shell calls `PackFeatureEntry.watchVerifiedModels`. That registers an interpreter only when the pack is Ready, the SHA-256 matches, and `PackModelContract` accepts the graph
- `PackLiteRt` builds that Super-res interpreter only for a verified 2×/4× RGB file. Inpaint / denoise / interp / track stay unbound
- Engines call `PackEnhance` and `TrackFacade.follow` / `FrameInterp.interpolatedFrames`
- A null try result leaves Telea / bicubic / frame-hold in place, unlabeled as neural

## Photo

| ID | Graph | Engine |
|----|-------|--------|
| E1 Fill | `I` | Telea now; `LiteRtInpaintHook` only if inpaint pack **and** hook bound |
| E2 Super-res | `SR,2` / `SR,4` | Distilled ESRGAN via Download or sideload; LiteRT bind on 2×/4× RGB contract |
| E3 Denoise | `N,{amount}` | Identity until denoise hook; Apply gated; same op on preview + WorkManager export |

## Movie

| ID | Contract | Engine |
|----|----------|--------|
| E4 | `FrameInterp.mlAvailable` ← pack engine | Neural frames chip locked until bound |
| F1 | `ClipTrack` field on `TimelineClip` | No motion follow in base; Track chip + pack CTA |

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
