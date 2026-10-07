# ml-packs-v2 rollout (Model Lab → app)

The Kotlin side already points at these files (`PackCatalog`, spec version "2"). Downloads stay off
until the four files are uploaded with **exactly** these names and hashes.

| File (upload name) | Model | Size | SHA-256 (pinned in `PackId.kt`) |
|--------------------|-------|------|---------------------------------|
| `depth.tflite` | Depth Anything V2 Small, int8 weights | 26 MB | `251c44443c96b66790cc0f54aaf1e746de9d36af4b2e0ed01e504ee0d82e87ad` |
| `inpaint.tflite` | MI-GAN 256 Places2, int8 weights | 7.1 MB | `3d9d6b30a1fe5bed0eb231c9877d5668380490229e8695a142df4872f65e1809` |
| `matting.tflite` | MODNet photographic, int8 weights | 6.8 MB | `9902196ea431f4e7611d370561030e969fee45e309915dfa890b3a887ad60ec4` |
| `flow.tflite` | NeuFlow v2 things, fp32 | 36 MB | `7fdffa3e5077201f351ca86975ef119ead2d7c1a39755114e8fbc9ead501a57a` |

Workspace copy: `lab/out/ml-packs-v2/` (+ `SHA256SUMS`). Rebuild from scratch: `tools/model_lab/convert/`
(`setup_conv_env.sh`, then `rebuild_all.sh`, then `convert_neuflow.py`).

## Steps
1. GitHub → `Saikumar-S0906/Release-` → Releases → New release, tag **`ml-packs-v2`** → attach the 4 files.
2. `./gradlew :ml:testDebugUnitTest :app:testDebugUnitTest` (catalog pins and counts were updated:
   8 packs, HTTPS set = FrameInterp, Inpaint, Depth, Matting, Flow).
3. Device smoke per `docs/R5_ML_PACKS.md`:
   - Photo → Cutout → **Matting** status row: Download → Ready → "Neural ready". Cutout / background
     swap now use MODNet (`TieredVision.subjectMask`). MediaPipe stays the preview tier (`previewMask`)
     and the fallback.
   - Retouch → Fill → neural Apply uses MI-GAN. CG panel → depth Download → soft Z uses DA-V2.
   - Flow: `PackEngineHooks.tryFlow(frame0, frame1)` returns 256×256 (dx, dy) in grid pixels. The
     stabilize / motion-trail consumers are the next step; classical crop-zoom stays until wired.
4. Devices with a v1 file re-download automatically, because the v2 spec uses a new version folder.

## What changed in code
- `ml/.../pack/PackId.kt`: `Matting` and `Flow` ids, v2 URLs + SHA-256, spec version "2".
- `ml/.../pack/PackModelContract.kt`: `isSubjectMatte`, `isDenseFlow`.
- `ml/.../PackEngineHooks.kt`: `NeuralEngine.Matting` / `Flow`, `tryMatte`, `tryFlow`.
- `ml/.../PackLiteRt.kt`: `bindMatting` / `subjectMatte`, `bindFlow` / `denseFlow`.
- `ml/.../VisionFacade.kt`: `previewMask`, `neuralMatte`, `TieredVision`. Wired in `StudioGraphModule`.
- `app/.../PackChrome.kt` + `PhotoSessionScreen.kt`: Matting status/Download row in Cutout. Strings added to all 23 strings.xml locales.
- Not compiled in the build workspace (no JDK/Android SDK there). Run step 2 before merging.

## Flagship HQ tier (`ml-packs-v2-hq`, automatic)
`PackTier.detect()` runs in `PixiJoyApp.onCreate`. Phones with ≥ 8 GB RAM, a 64-bit ABI and no low-RAM
flag resolve `PackCatalog.spec()` to the HQ files below. Same `PackId`, same UI, separate store
folder (version `2hq`). Depth output is box-pooled to 256² so SoftZ/CG consumers are unchanged.

| Upload name | Model | Size | Measured (same protocol) | SHA-256 |
|-------------|-------|------|--------------------------|---------|
| `depth_hq.tflite` | DA-V2 Small, 512² in, 518 internal | 26 MB | AbsRel **0.0646** (std 0.077), δ1 0.951 | `6addaa7826620c017a589ead0aff3bbdaea3a0a7aa8400d8619dfeb86f094960` |
| `matting_hq.tflite` | MODNet @512 | 7.8 MB | MAD **0.0138** (std 0.0149), edge 0.069 (std 0.074) | `e715a2c951445b705d1dc071742a906720cf2fb55be5819479cf68b0edbf0a2d` |
| `inpaint_hq.tflite` | MI-GAN 512 Places2 | 10.1 MB | L1 0.114 / LPIPS 0.076 *scored at 256* (gain is detail at 512 output; not measurable at 256) | `5769125dc24b60a7160f6854fb2adfea4780b66052fa1fa71e523ec238fb1c50` |

Workspace copy: `lab/out/ml-packs-v2-hq/` (+ `SHA256SUMS`). Rebuild: `tools/model_lab/convert/build_hq.sh`.
Upload them to Release- tag **`ml-packs-v2-hq`**.

## AI stabilize (Flow pack)
`movie/FlowStabilize.kt`: when the Flow engine is bound and a clip has Stabilize on, export samples
8 fps (max 160 pairs). NeuFlow v2 runs on each pair, and the per-axis median gives global shake. The
camera path is smoothed with a Gaussian (σ grows with strength), the per-frame correction becomes a
Media3 `MatrixTransformation`, and zoom is only as large as the measured correction (≤ 1.2×). Without
the pack it falls back to the classical constant crop-zoom. Baked synchronously and LRU-cached, like
`TrackPathBake`. Motion trails are not wired yet.

## Live cutout preview
`BitmapPhotoEngine.renderLayers(..., draft)` uses `VisionFacade.previewMask` (MediaPipe). The photo
preview renders a draft on every edit, then after `MATTE_SETTLE_MS` (350 ms) with no new edit it
re-renders with the MODNet matte (only when the Matting engine is bound and a layer uses a subject
matte). Export always uses the neural matte.
