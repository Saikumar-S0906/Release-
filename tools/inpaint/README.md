# PixiJoy neural inpaint lab (M1 / E1)

Full UNet → `inpaint.tflite`. Hardened: no Lambda, safe ckpt load, refuses untrained export.

**Ship rule:** only `python train.py --quality` after `data/READY` + ≥32 real photos. Smoke ≠ Release-.  
Law: [`../ML_QUALITY_BAR.md`](../ML_QUALITY_BAR.md)

## Contract

| | Shape | Dtype |
|---|-------|-------|
| In0 image | `1×256×256×3` | FLOAT32 |
| In1 mask | `1×256×256×1` | FLOAT32 (1 = hole) |
| Out0 RGB | `1×256×256×3` | FLOAT32 |

## Colab GPU (quality — product)

1. Open: https://colab.research.google.com/github/Saikumar-S0906/Release-/blob/main/train_colab.ipynb
2. Runtime → **GPU** → **Run all**
3. Notebook mounts Drive (auto-resume) and runs `prepare_quality_data.py` (DIV2K — **no photo upload**)
4. Pin `inpaint.tflite` only if `verify_contract.py --quality` passes

**Do not upload personal photos** unless you want to. Public DIV2K is enough for the quality bar.

## Smoke (contract only — never pin)

```bash
python train.py --smoke
python export_tflite.py
python verify_contract.py
```
