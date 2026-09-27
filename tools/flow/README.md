# PixiJoy flow lab (M4)

Optical flow / stabilize assist → `flow.tflite`.

**Ship rule:** only `--quality` + `data/READY`. Smoke ≠ Release-.  
Law: [`../ML_QUALITY_BAR.md`](../ML_QUALITY_BAR.md)

## Contract

| | Shape | Dtype |
|---|-------|-------|
| In0 frame_t | `1×256×256×3` | FLOAT32 |
| In1 frame_t1 | `1×256×256×3` | FLOAT32 |
| Out0 flow | `1×256×256×2` | FLOAT32 `(dx, dy) / SIZE` |

## Colab GPU

1. Open: https://colab.research.google.com/github/Saikumar-S0906/Release-/blob/main/train_flow_colab.ipynb
2. Runtime → **GPU** → Run all (Drive resume; **auto ETH DIV2K+exact flow** — no HF / no upload)
3. Pin only if verify quality passes

## Smoke

```bash
python train.py
python export_tflite.py
python verify_contract.py
```
