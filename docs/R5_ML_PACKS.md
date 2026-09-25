# R5 ML packs + F1 track contract

**Status:** Pack runtime landed on `next`. Neural engines stay unbound in the base APK.
**Pairs with:** `docs/REMAINING_MASTER_PLAN.md` Wave E / F1, `docs/FUNCTIONALITY_MATRIX.md`.
**Hard rule:** pack file + checksum ≠ neural engine. Telea / bicubic / frame-hold are never labeled neural.

## Runtime (`:ml:pack`)

- Install state: missing / installing / ready / failed / storage error
- SHA-256 sidecar on sideload; optional pinned hash in `PackCatalog`
- Play Feature Delivery module names: `ml_inpaint`, `ml_superres`, `ml_denoise`, `ml_interp`, `ml_track`
- Sideload: **Install from file** (`GetContent`) copies into cache then checksum-installs; fallback folder `filesDir/ml_incoming/{fileName}` (also app external files)
- HTTPS fallback (user-tapped Download only): Super-res fetches distilled ESRGAN (`PackCatalog.SUPERRES_DOWNLOAD_URL`) with a pinned SHA-256. Photos are not uploaded.
- `SplitCompat.install` in `PixiJoyApp.attachBaseContext`
- `PackLiteRt` binds Play-services `InterpreterApi` only when tensor shapes match `PackModelContract` (2×/4× RGB). Inpaint / denoise / interp / track stay unbound without matching models.

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
