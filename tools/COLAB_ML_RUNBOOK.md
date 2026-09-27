# Wave M — Colab runbook (quality, no compromise)

**Quality law:** [`ML_QUALITY_BAR.md`](ML_QUALITY_BAR.md)  
Synthetic `--smoke` = contract only. **`--quality` + real `data/READY` = only path to Release-.**

**Order:** finish **M1** → **M2 Depth** → **M3 Matting** → **M4 Flow** → **M7 Sky** (photo gap).  
Parallel GPUs: M2 ∥ M3 after M1.

Host artifacts on [Release- ml-packs-v1](https://github.com/Saikumar-S0906/Release-/releases/tag/ml-packs-v1).

## Prep (once)

```powershell
.\tools\pack_ml_labs.ps1
```

## Per pack (product)

| Step | Lab | Artifact | Quality data |
|------|-----|----------|--------------|
| M1 | `tools/inpaint/` | `inpaint.tflite` | `data/rgb` (≥32) + `READY` (+ optional `masks`) |
| M2 | `tools/depth/` | `depth.tflite` | `data/rgb` + `data/depth` + `READY` |
| M3 | `tools/matting/` | `matting.tflite` | `data/rgb` + `data/alpha` + `READY` |
| M4 | `tools/flow/` | `flow.tflite` | `data/frame0` + `frame1` + `flow_npy` + `READY` |

1. Stage real datasets per lab `data/README.md` → create `READY`  
2. Colab GPU → run notebook with **quality** cell (or `python train.py --quality`)  
3. `python export_tflite.py` → `python verify_contract.py`  
4. Pass **quality gates** in `ML_QUALITY_BAR.md` (not just smoke MAE)  
5. Upload TFLite + record SHA-256 → app pin later (P7)

## Contracts (frozen — student must match)

- **Depth:** `1×256×256×3` → `1×256×256×1`  
- **Matting:** `1×256×256×3` → `1×256×256×1`  
- **Flow:** two `1×256×256×3` → `1×256×256×2`

Teacher distill is encouraged; student export must still pass `verify_contract.py`.

## Forbidden

- Pinning smoke weights as product  
- Labeling Telea / HSV sky / classical Z as neural  
- Loosening quality gates to force a green  

App LiteRT bind = coding **P7** when each quality file lands.
