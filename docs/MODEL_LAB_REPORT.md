# PixiJoy Model Lab report — are the ML packs good, and can they be made better?

Date: 2026-10-07 · Build workspace: x86 CPU only (no GPU). All numbers below are **measured**, not quoted.
Everything is reproducible from `tools/model_lab/` (bench + convert scripts) and `tools/kaggle_v2/` (fine-tune kit).
Runtime promise unchanged: every model runs on the phone, nothing is uploaded.

## TL;DR

| Pack | Shipped today (`ml-packs-v1` / APK) | Measured | Drop-in replacement (same Kotlin contract) | Measured | Size |
|------|-------------------------------------|----------|---------------------------------------------|----------|------|
| **M2 Depth** | PixiJoy UNet `depth.tflite` | AbsRel **0.222**, δ1 0.60 | Depth Anything V2 Small, int8 | AbsRel **0.077**, δ1 0.94 | 75 → **26 MB** |
| **M1 Inpaint** | PixiJoy UNet `inpaint.tflite` | masked L1 **0.221**, LPIPS 0.132 — *worse than Telea* | MI-GAN 256 Places2, int8 | masked L1 **0.113**, LPIPS **0.076** | 75 → **7.1 MB** |
| **M3 Matting** | MediaPipe `selfie_segmenter.tflite` | MAD **0.0345**, edge MAD 0.133 | MODNet photographic, int8 | MAD **0.0149**, edge MAD **0.074** | 0.24 → 6.8 MB |
| **M4 Flow** | none (classical crop-stabilize) | OpenCV DIS: EPE 5.62 px | RAFT-small / NeuFlow v2 | EPE 2.72 / **1.14** px | TFLite export pending |

**Answer to "can you train these models well?"** Not by continuing the current recipe. The v1 UNets were
trained from scratch with plain L1 on small data; that recipe cannot reach the pretrained open models on
any Kaggle budget. The better path, already done here for three packs: **convert strong pretrained
models into the frozen app contracts** (no Kotlin change except URL + SHA-256), then optionally
**fine-tune** them on Kaggle (`tools/kaggle_v2`, ≈ 23 GPU-h total, fits 30 h/week).

## Protocol (fixed, seeded, 100 samples per task)

| Task | Test set | Metric |
|------|----------|--------|
| Depth | NYU Depth V2 **official test split** (654 → 100 evenly spread), Eigen crop | Per-image scale+shift alignment (relative depth), AbsRel ↓, δ1 ↑ |
| Inpaint | Places365 val, 256², holes from the app's own `tools/inpaint/dataset.make_mask` (mean hole 16.9 %) | masked L1 ↓ (= ship-gate "masked MAE"), PSNR, SSIM, LPIPS-Alex ↓ |
| Matting | P3M-500-P (real alpha GT), 512² | MAD ↓ (ship gate 0.04), edge-band MAD ↓ (hair/edges) |
| Flow | MPI Sintel clean train GT, 104 pairs | EPE px ↓, native 1024×436 and on the 256 contract grid |

`cpu_ms` = desktop x86, 4 threads, LiteRT/ORT. It is a relative speed signal, **not** a phone number —
measure on device (Pixel/Galaxy) before quoting latency.

## Depth (NYU test, 100)

| Model | AbsRel ↓ | δ1 ↑ | Size | CPU ms |
|-------|---------|------|------|--------|
| PixiJoy UNet v1 (Release- `depth.tflite`) | 0.222–0.239 | 0.60–0.63 | 74.7 MB | 520–1600 |
| DA-V2 Small @266, fp32 TFLite (contract) | 0.076 | 0.939 | 94.7 MB | 280–420 |
| **DA-V2 Small @266, int8 TFLite (contract)** | **0.077** | **0.938** | **26.2 MB** | **63–116** |
| DA-V2 Small @518 (reference) | 0.064 | 0.951 | — | 900 |

v1 ships depth that is ~3× less accurate. Samples: `docs/model_lab/samples/depth_*.jpg` (RGB, GT, v1, DA-V2).
Artifact: `depth_dav2s_dyn8.tflite` SHA-256 `251c44443c96b66790cc0f54aaf1e746de9d36af4b2e0ed01e504ee0d82e87ad`.
Contract check: FLOAT32 1×256×256×3 → 1×256×256×1, 0 near / 1 far (verified: floor nearer than ceiling on 10/10).

## Inpaint (Places365 val, 100)

| Model | masked L1 ↓ | PSNR ↑ | LPIPS ↓ | Size | CPU ms |
|-------|-------------|--------|---------|------|--------|
| Telea (always-on fallback) | 0.112 | 25.5 | 0.133 | 0 | 6 |
| PixiJoy UNet v1 | **0.221** | 21.3 | 0.132 | 75 MB | 510–1030 |
| **MI-GAN 256, int8 TFLite (contract)** | **0.113** | — | **0.076** | **7.1 MB** | **137** |
| MI-GAN 256, fp32 TFLite | 0.111 | — | 0.075 | 23.6 MB | 163 |
| Big-LaMa 512 (reference, too big for phones) | 0.087 | 27.5 | 0.063 | 198 MB | 3500 |

**v1 is broken, not just weak:** it paints holes flat grey (`samples/inpaint_*.jpg`, 3rd tile). It is no
better with the hole zeroed (masked L1 0.243), so the cause is training collapse, not the feed. The app's
`inpaintHoleIsDeadFill` guard probably rejects many v1 results already.

**Real train/infer mismatch found:** `tools/real_data.batch_inpaint` feeds `target * (1 - mask)` (hole
blanked), while `PackLiteRt.inferInpaintDual` feeds original pixels. The MI-GAN export blanks the hole
inside the graph, so it is correct with today's Kotlin code. Any future UNet must match the app feed.

Artifact: `inpaint_migan256_dyn8.tflite` SHA-256 `3d9d6b30a1fe5bed0eb231c9877d5668380490229e8695a142df4872f65e1809`.
Inputs `image` 1×256×256×3 + `mask` 1×256×256×1 (1 = hole), output RGB 0..1 with known pixels kept. It passes
`PackModelContract.inpaintInputIndices` in either input order.

## Matting (P3M-500-P, 100)

| Model | MAD ↓ | edge MAD ↓ | Size | CPU ms |
|-------|-------|-----------|------|--------|
| MediaPipe selfie segmenter (today) | 0.0345 | 0.133 | 0.24 MB | 2 |
| **MODNet, int8 TFLite (contract)** | **0.0149** | **0.074** | **6.8 MB** | **22** |
| MODNet @512 (reference) | 0.0144 | 0.066 | 24.7 MB | 459 |

MediaPipe passes the 0.04 MAD gate on whole images but fails at hair/edges, which is the M3 bar.
Artifact: `matting_modnet_dyn8.tflite` SHA-256 `9902196ea431f4e7611d370561030e969fee45e309915dfa890b3a887ad60ec4`
(contract of `tools/matting/config.py`). The app has no matting `PackId` yet (one Kotlin enum + bind).

## Flow (Sintel clean, 104 pairs)

| Model | EPE native ↓ | EPE @256 ↓ | Size | CPU ms |
|-------|-------------|-----------|------|--------|
| OpenCV DIS medium (classical) | 5.62 | 1.97 | 0 | 15 |
| OpenCV Farneback | 9.50 | 3.49 | 0 | 84 |
| RAFT-small (Chairs+Things) | 2.72 | 0.96 | 3.8 MB | 1500 |
| RAFT-small fed 256² contract frames | 3.45 | **1.34** (gate ≤ 1.5 ✔) | 3.8 MB | 1270 |
| **NeuFlow v2 "things"** (no Sintel in training) | **1.14** | **0.46** | 40 MB | 1300 |

(NeuFlow "mixed" was trained on Sintel, so it was excluded as test leakage.) TFLite export: RAFT fails
in `ai-edge-torch` (`tfl.div` legalize, torchvision `raft.py:497`). This is the Phase 2 item, together with
a flow `PackId`.

## What the Kotlin side needs (minimal)
1. Upload the three `*_dyn8.tflite` files to Release- tag **`ml-packs-v2`**.
2. `PackCatalog`: `INPAINT_DOWNLOAD_URL/SHA256`, `DEPTH_DOWNLOAD_URL/SHA256` → v2 values, and `VERSION = "2"`
   so devices re-download. Shapes and dtypes are unchanged, so `PackLiteRt` binds without edits.
3. Optional: add `PackId.Matting` (+ bind using the depth-style 1-in/1-out path) to replace the selfie oval.
4. Device smoke per `docs/R5_ML_PACKS.md` (Neural Apply only after `engineReady`). No device run was claimed here.

## Licenses
DA-V2 **Small**: Apache-2.0 (Base/Large are CC-BY-NC, never use them). MI-GAN code + weights: MIT.
MODNet: Apache-2.0. RAFT (torchvision): BSD-3. NeuFlow v2: Apache-2.0. Test sets were used for evaluation only.
For fine-tunes, P3M-10k, Sintel and Places365 are research-only (see `tools/kaggle_v2/README.md`).
