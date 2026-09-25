# R5 ML packs + F1 track contract

**Status:** Pack runtime landed on `next`. Neural engines stay unbound in the base APK.
**Pairs with:** `docs/REMAINING_MASTER_PLAN.md` Wave E / F1, `docs/FUNCTIONALITY_MATRIX.md`.
**Hard rule:** pack file + checksum ≠ neural engine. Telea / bicubic / frame-hold are never labeled neural.

## Runtime (`:ml:pack`)

- Install state: missing / installing / ready / failed / storage error
- SHA-256 sidecar on sideload; optional pinned hash in `PackCatalog`
- Play Feature Delivery module names: `ml_inpaint`, `ml_superres`, `ml_denoise`, `ml_interp`, `ml_track`
- Sideload: **Install from file** (`GetContent`) copies into cache then checksum-installs; fallback folder `filesDir/ml_incoming/{fileName}` (also app external files)
- `SplitCompat.install` in `PixiJoyApp.attachBaseContext`
- `PackEngineHooks` stay null until a real interpreter registers

## Photo

| ID | Graph | Engine |
|----|-------|--------|
| E1 Fill | `I` | Telea now; `LiteRtInpaintHook` only if inpaint pack **and** hook bound |
| E2 Super-res | `SR,2` / `SR,4` | Identity until super-res hook; Apply gated |
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
