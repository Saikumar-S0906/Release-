# PixiJoy depth lab (M2 / H5)

Monocular relative depth → `depth.tflite`.

**Ship rule:** only `python train.py --quality` after staging `data/READY`. Smoke ≠ Release-.  
Law: [`../ML_QUALITY_BAR.md`](../ML_QUALITY_BAR.md)

## Contract

| | Shape | Dtype |
|---|-------|-------|
| In0 image | `1×256×256×3` | FLOAT32 |
| Out0 depth | `1×256×256×1` | FLOAT32 0..1 (near→far) |

## Colab GPU

1. Open: https://colab.research.google.com/github/Saikumar-S0906/Release-/blob/main/train_depth_colab.ipynb
2. Runtime → **GPU** → Run all (Drive resume; **auto NYU data** — no upload)
3. Pin only if verify `--quality` OK

## Smoke (contract only)

```bash
python train.py
python export_tflite.py
python verify_contract.py
```
