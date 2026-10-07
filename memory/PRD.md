# PixiJoy — On-device ML upgrade (Model Lab + Kaggle fine-tune kit)

## Original problem
"I think you got my project pixi joy from next branch having code, you need to understand that first and then.. it run with local ml, no uploads to ai. so i just want to know whether you can train that models well, those are there in release_ too.."
User choices: Kaggle GPU (30 h/week) for training; avoid hard dependency on Hugging Face (had issues before); Perplexity request was accidental, so it was dropped.

## Context
- The repo is a native Android Kotlin app (Compose/Hilt/LiteRT), not Expo. The Kotlin code was not changed.
- Models from Release- `ml-packs-v1`: depth.tflite and inpaint.tflite (PixiJoy UNets, about 75 MB each). Matting today is the MediaPipe selfie segmenter. Flow has no pack.
- The build workspace has no GPU, and `/app` is the only persistent disk (resets wipe /tmp and /opt).

## Done (2026-10-07)
- Benchmarked v1 against candidate models on public test sets: NYU test, Places365 val, P3M-500-P, Sintel. Report: `docs/MODEL_LAB_REPORT.md`, JSON and samples in `docs/model_lab/`.
- Converted the winning models into the frozen app contracts (int8 weights, FLOAT32 in/out): depth DA-V2-S 26 MB (AbsRel 0.222 → 0.077), inpaint MI-GAN 7.1 MB (masked L1 0.221 → 0.113), matting MODNet 6.8 MB (MAD 0.0345 → 0.0149). Files are in `lab/out/` (gitignored).
- Found bugs: the v1 inpaint model fills holes with flat grey (worse than Telea), and training blanks the hole while `PackLiteRt` feeds the original pixels.
- Kaggle kit `tools/kaggle_v2/`: `pixi_ft.py` (depth/matting/inpaint/flow fine-tune, budgeted and resumable), export scripts, 4 generated notebooks, README. Download order: kit → Hugging Face → hf-mirror → official sources. All 4 tasks pass the CPU smoke test, and the 3 export paths were verified (contract_ok).
- Bench and convert scripts are copied to `tools/model_lab/`.

## Backlog
- P0: Upload the 3 int8 TFLite files to Release- `ml-packs-v2`, then update `PackCatalog` URLs, SHA-256 and VERSION=2. Run the device smoke test per R5_ML_PACKS.
- P0: The user runs the Kaggle notebooks (depth 4 h, matting 5 h, inpaint 8 h, flow 6 h).
- P1: Add `PackId.Matting` plus a bind. Fix RAFT TFLite export or port NeuFlow v2, then add `PackId.Flow`.
- P1: Build the `pixijoy-ml-kit` zip for Kaggle (lab/kit, ~520 MB).
- P2: Measure on-phone latency. Video matting/depth with temporal smoothing.
