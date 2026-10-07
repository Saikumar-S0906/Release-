# PixiJoy fine-tune kit v2 — Kaggle (30 GPU-h / week)

Replaces "train a 75 MB UNet from scratch" (the `ml-packs-v1` path) with **fine-tuning strong pretrained,
permissively licensed models** that already beat v1 before any training (see `docs/MODEL_LAB_REPORT.md`).
Training is offline only; at runtime every model runs on the phone. Nothing is uploaded.

| Task | Start model (license) | Fine-tune data | Kaggle budget | Ship target (app contract) |
|------|-----------------------|----------------|---------------|----------------------------|
| M2 depth | Depth Anything V2 **Small** (Apache-2.0) | NYU train GT + self-distill (same model @518 teaches @266) on COCO photos | 4 h | `depth.tflite` 1×256×256×3 → 1×256×256×1 |
| M3 matting | MODNet photographic (Apache-2.0) | P3M-10k train ⚠ research-only | 5 h | `matting.tflite` 1×256×256×3 → 1×256×256×1 |
| M1 inpaint | MI-GAN 256 Places2 (MIT) | COCO-2017 photos (CC-BY) + the app's own `make_mask` holes | 8 h | `inpaint.tflite` image+mask → RGB |
| M4 flow | RAFT-small torchvision (BSD-3) | MPI Sintel train scenes ⚠ research-use, 6 scenes held out | 6 h | export blocked (see below) |

Total ≈ 23 h → fits one Kaggle week with margin for a re-run.

## Why not keep training the v1 UNets?
Measured on public test sets (Model Lab): v1 depth AbsRel **0.222** vs Depth Anything V2 Small **0.077**;
v1 inpaint is **worse than Telea** (masked L1 0.221 vs 0.112) and fills holes with flat grey. A plain
L1 UNet from scratch on a few thousand images cannot reach the pretrained models within any Kaggle budget.

## One-time setup: the `pixijoy-ml-kit` Dataset (avoids Hugging Face problems)
Hugging Face worked in the build workspace, but Kaggle/Colab sessions have had HF failures, so the kit
lets every notebook run **without HF**:

1. Use the ready zip `lab/pixijoy-ml-kit.zip` (427 MB, SHA-256 in `lab/pixijoy-ml-kit.zip.sha256`) from the
   workspace, **or** rebuild it on any machine with `python tools/kaggle_v2/make_kit.py` (≈ 430 MB):
   `dav2s/` (Depth Anything V2 Small), `weights/` (MI-GAN 256, MODNet ckpt, RAFT-small),
   `eval/` (depth.npz, inpaint.npz, matting.npz = the exact 100-sample Model Lab test sets).
2. Kaggle → Datasets → New → upload → name **pixijoy-ml-kit** → Private.
3. In each notebook: *Add Input* → your dataset.

Fallback order inside `pixi_ft.py` for every file: kit → huggingface.co (3 retries) → hf-mirror.com →
official source (NYU labeled `.mat` from nyu.edu, MODNet/MI-GAN Google Drive, torchvision CDN).
Training photos come from the official COCO zip (HTTP range, not HF).

## Run
1. `python tools/kaggle_v2/gen_notebooks.py` → `tools/kaggle_v2/notebooks/*.ipynb`
2. Kaggle → New Notebook → File → Import → pick one → Accelerator **GPU T4** → Internet **On** → Run All.
3. One task per session. If the 12 h session limit hits, *Save Version*, add that output as an Input,
   re-run: it resumes from `out/<task>.ckpt.pt`.
4. Ship only if `out/<task>_report.json` → `"improved": true`. The export cells build the same
   CPU converter env used for the Model Lab (`export/setup_conv_env.sh`) and write
   `out/<task>_ft_dyn8.tflite` (int8 weights, FLOAT32 in/out = unchanged Kotlin contract).
5. Upload the `.tflite` to Release- `ml-packs-v2`, update `PackCatalog` URL + SHA-256.

`python pixi_ft.py <task> --smoke` runs a 2-minute CPU code-path check (verified in the workspace).

## Licenses (read before shipping a fine-tune)
- Commercial-safe: DA-V2 **Small** (Base/Large are CC-BY-NC — never use them as teacher), MODNet,
  MI-GAN, RAFT, COCO images (CC-BY, attribution), NYU Depth V2 (research dataset; widely used for
  fine-tunes, check your counsel).
- Research-only data: **P3M-10k**, **MPI Sintel**, Places365. A model fine-tuned on these is for internal
  evaluation; ship the pretrained conversion unless you have rights.

## Flow export status (RAFT; NeuFlow v2 is converted — see docs/MODEL_LAB_REPORT.md)
`ai-edge-torch` fails on RAFT (`tfl.div` legalize, torchvision `raft.py:497`), and the app has no flow
`PackId` yet. Phase 2: replace the integer coordinate math in RAFT's correlation lookup, or port
NeuFlow v2 (EPE 1.14 px on Sintel, best measured).
