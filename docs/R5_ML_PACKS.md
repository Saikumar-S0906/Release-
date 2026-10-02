# R5 ML packs + F1 track contract

**Status:** Pack runtime + LiteRT bind on matching contracts after SplitCompat. E4 FILM HTTPS pin + LiteRT bind. F1 Track, Super-res, and Denoise ship in APK assets and bind when LiteRT is up — no Download URL for those three. Neural inpaint is trained in `tools/inpaint/` (full UNet); placement is APK assets if ≤~3 MB else PixiJoy GitHub Release — never shrink the network for size.
**Pairs with:** `docs/REMAINING_MASTER_PLAN.md` Wave E / F1–F2, `docs/FUNCTIONALITY_MATRIX.md`.
**Hard rule:** pack file + checksum ≠ neural engine. Telea / bicubic / frame-hold are never labeled neural. Small bundled TFLite (Track/SR/Denoise/Inpaint-if-small) may live in the base APK; FILM / FFmpeg / Filament stay out.

## Runtime (`:ml`)

`:ml` owns install state, `PackSplits` (SplitCompat), and the Hilt singleton `PackEngineHooks`. Play feature modules (`ml_inpaint`, `ml_superres`, `ml_denoise`, `ml_interp`, `ml_track`) own optional Play assets. `:photo` and `:movie` call the facade and do not depend on a pack module or LiteRT. Session UI observes install state and pack CTAs; it does not call `register`. Super-res, Denoise, and Track seed from APK assets on first `PackInstallManager.get` — no Download CTA. FrameInterp Missing shows Download when an HTTPS pin exists. Inpaint Missing shows Download once a Release/APK artifact is pinned; otherwise **Install from file**. Telea Fill Apply stays on always.

- Install state: missing / installing / ready / failed / storage error. **Ready** means the file is on the device and its SHA-256 verified. **Neural** is `PackRuntime.engineReady` (Ready and a bound interpreter). Ready alone does not enable Super-res Apply, Denoise Apply, neural inpaint Apply, or Track follow
- SHA-256 sidecar on sideload. Track / Super-res / Denoise use `expectedSha256` = bundled asset pin. FrameInterp HTTPS Download refuses a body that misses `downloadSha256` before Ready. Inpaint has no `downloadUrl` until a trained artifact is published (APK or Release)
- Sideload: **Install from file** (`GetContent`) copies into cache then checksum-installs; fallback folder `filesDir/ml_incoming/{fileName}` (also app external files)
- HTTPS fallback (user-tapped Download only) for packs without `assetPath`. Photos are not uploaded. Pins below are the only hosted artifacts.
  - Super-res `superres.tflite` / `ml_superres`: **ships in** `ml/src/main/assets/superres.tflite` (SHA-256 `27ed316318cac65fe350ccdb3c1e19ac08c512f4aa2fc23b1a351ce334809c3f`). Provenance ESRGAN pin retained as constant only. Graph is FLOAT32 NHWC `1×180×320×3` → `1×720×1280×3` (4×).
  - Denoise `denoise.tflite` / `ml_denoise`: **ships in** `ml/src/main/assets/denoise.tflite` (SHA-256 `53f0112f248b18ae518af64942ea4741f5b22ccdc4af5aea0ff452875f17fb84`). Graph is FLOAT32 NHWC `1×256×256×3` → same size.
  - FrameInterp `interp.tflite` / `ml_interp`: FILM (~67.29 MB) from public [Release-](https://github.com/Saikumar-S0906/Release-) `ml-packs-v1/interp.tflite` SHA-256 `3a201916658277ed6e7626d93a6ee117a47b04bc1986579600a90feabfe05eb4`. Graph FLOAT32 FILM 512 path.
  - Track `track.tflite` / `ml_track`: PixiJoy-trained Siamese (~1.99 MB) **ships in** `ml/src/main/assets/track.tflite`.
  - Inpaint `inpaint.tflite` / `ml_inpaint`: PixiJoy-trained UNet from `tools/inpaint/` (full capacity, no size-driven shrink). Telea Fill always on. Neural Apply after Ready + LiteRT RGB+mask bind. Placement: APK assets if ≤~3 MB else GitHub Release Download.
- Failed Download for packs without a URL tells the user to **Install from file** (Play modules are not in this APK).
- `PixiJoyApp.attachBaseContext` calls `PackSplits.install`. `onCreate` binds install state only and registers nothing
- After SplitCompat, the studio shell calls `PackFeatureEntry.watchVerifiedModels`. `PackLiteRt` opens a file only when LiteRT is up, the pack is Ready, and the bytes match the recorded SHA-256 (and `expectedSha256` when set). It registers only if `PackModelContract` accepts the graph. A miss releases the capability (sink null). Chips read `PackRuntime.engineReady`: Ready alone is not neural
- `PackLiteRt` builds Super-res (2×/4× FLOAT32 NHWC RGB), Denoise (same-size FLOAT32 RGB), Inpaint (FLOAT32 RGB+mask), FrameInterp (FILM FLOAT32 time+2×RGB → mid RGB), and Track (template+search → xy).
- Engines call `PackEnhance` and `TrackFacade.follow` / `FrameInterp.interpolatedFrames`
- A null try result leaves Telea / bicubic / frame-hold in place, unlabeled as neural. Neural slow-mo export falls back to Media3 rate/hold the same way.

## Photo

| ID | Graph | Engine |
|----|-------|--------|
| E1 Fill | `I` (Telea) / `I,…;n` (neural request) | Nearby-pixel Telea unless the op asked for neural and a sideloaded inpaint TFLite matches RGB+mask and LiteRT binds. Ready file alone does not turn neural Apply on |
| E2 Super-res | `SR,2` / `SR,4` | Distilled ESRGAN via Download (pinned SHA) or a sideload that passes 2×/4× NHWC RGB + FLOAT32 |
| E3 Denoise | `N,{amount}` | Raspberry Pi UNet via Download (pinned SHA) or a sideload that passes same-size RGB + FLOAT32 |

## Movie

| ID | Contract | Engine |
|----|----------|--------|
| E4 | `FrameInterp.mlAvailable` ← pack engine | HTTPS FILM pin + LiteRT bind MET when Ready + FLOAT32 film contract (512×512). Export Neural frames → `NeuralSlowMoMux` mid-frame synth + audio remux; preview stays rate/hold; heuristic fallback if synth fails. Maestro download: `.maestro/packs/e4-frame-interp-download.yaml` |
| F1 | `ClipTrack` + `TrackPathBake` | Bundled `track.tflite` in APK assets + LiteRT bind when Ready + FLOAT32 point-track contract. Enable Track unlocks on `engineReady`; bake samples the clip source window (trim/speed) and maps pan/zoom into CG/VFX attach so overlays do not drift. Seed-only when unbound. Maestro: `.maestro/movie/f1-track-roto.yaml` |

## E5 size

- `selfie_segmenter.tflite` stays in `:ml` (~244 KB) for MediaPipe `ImageSegmenter` only (`VisionFacade`). It is not a `PackId`, not in `PackCatalog`, and `PackLiteRt` does not bind it. Moving it would not change the pack gate
- `./gradlew :app:assembleRelease :app:reportApkSize` prints the minified unsigned APK size and each `.so`
- **Baseline, remeasured on tip `03fb2ea` (2026-09-25):** minified `app-release-unsigned.apk` **54.80 MB** (57,457,669 bytes). The earlier **131.11 MB** note does not match this artifact. Nothing in that APK is a pack weight
  - MediaPipe `libmediapipe_tasks_vision_jni.so`, stored uncompressed: x86 **20.30** + arm64-v8a **12.43** + armeabi-v7a **7.61** = **40.34 MB**
  - dex **10.29 MB**. `material-icons-extended` is a ~35 MB Maven AAR, but R8 left **0** `androidx.compose.material.icons` classes in this dex. Studio chrome already uses `pj_ic_*` vectors
  - Play Services LiteRT client `libtensorflowlite_jni_gms_client.so` **2.11 MB** across x86, x86_64, arm64-v8a, and armeabi-v7a. SuperRes/Denoise/FrameInterp/Track bind needs this client. It is not stripped
  - `assets/selfie_segmenter.tflite` **0.24 MB**, vision only
  - Media3 is Java/Kotlin in the dex. This graph ships no Media3 JNI
  - No inpaint/SR/denoise/interp weights in the base APK (those packs stay on-demand). F1 `track.tflite` (~1.99 MB) ships in `:ml` assets
- **After:** release `ndk.abiFilters` = `arm64-v8a` + `armeabi-v7a` (debug stays multi-ABI). Same task: **33.08 MB** (34,683,268 bytes), **−21.72 MB**. Phone MediaPipe stays (12.43 + 7.61). LiteRT client stays on those ABIs (0.55 + 0.35). Dex unchanged (7.87 + 2.42). The unused extended-icons dependency is removed so a later keep rule cannot pull the 35 MB AAR back in; that removal did not change the measured dex. x86 and x86_64 `.so` files are absent. Pack weights stay out of the base APK
- This release APK is under the ~40 MB aspiration. A fat APK that also stores the emulator ABIs is the 54.80 MB baseline. What remains in base is phone MediaPipe JNI (~20.0 MB) plus dex (~10.3 MB)
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
8. FrameInterp: Missing shows **Download** (HTTPS FILM pin). After Ready + LiteRT bind, neural Slow-mo unlocks. Track: bundled in APK assets — no Download. After LiteRT bind (`isPointTrack`), Enable Track unlocks and `TrackPathBake` follows. Inpaint stays sideload-only.

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
- `.maestro/packs/e4-frame-interp-download.yaml`
- `.maestro/movie/f1-track-roto.yaml`
- Play download: `.maestro/YELLOW-ml-packs.md`
