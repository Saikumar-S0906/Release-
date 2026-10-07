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
