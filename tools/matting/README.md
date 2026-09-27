# PixiJoy matting lab (M3 / H6)

Subject alpha matte → `matting.tflite`.

**Ship rule:** only `--quality` + `data/READY`. Smoke ≠ Release-.  
Law: [`../ML_QUALITY_BAR.md`](../ML_QUALITY_BAR.md)

## Contract

| | Shape | Dtype |
|---|-------|-------|
| In0 image | `1×256×256×3` | FLOAT32 |
| Out0 alpha | `1×256×256×1` | FLOAT32 (1 = foreground) |

## Colab GPU

1. Open: https://colab.research.google.com/github/Saikumar-S0906/Release-/blob/main/train_matting_colab.ipynb
2. Runtime → **GPU** → Run all (Drive resume; **auto Pet/seg data** — no upload)
3. Pin only if verify quality passes

## Smoke

```bash
python train.py
python export_tflite.py
python verify_contract.py
```
