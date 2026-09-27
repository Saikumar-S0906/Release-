# F1 point-track LiteRT lab

PixiJoy-owned `track.tflite` for movie Track pack (`PackId.Track`).

## Contract

| Tensor | Shape / dtype |
|--------|----------------|
| template | FLOAT32 NHWC `1×64×64×3` |
| search | FLOAT32 NHWC `1×128×128×3` |
| xy | FLOAT32 `1×2` normalized `(x,y)` in `0..1` |

## Full training

Default budget (`config.py`): **20 epochs × 250 steps × batch 32** ≈ **160k** synthetic pairs, plus val each epoch, ReduceLROnPlateau + EarlyStopping (restore best). Wall clock on CPU is long by design — do not shrink for convenience.

```bash
cd tools/track
pip install tensorflow
python train.py
python export_tflite.py
python verify_contract.py
```

Artifact: `dist/track.tflite`. Training data is **synthetic** (procedural blobs/rects, clutter, mild occlusion). Weights are for PixiJoy pack hosting (Apache-2.0).

## Hosting

`track.tflite` ships in the base APK as `ml/src/main/assets/track.tflite` (copied from `dist/` after export). No HTTPS Download pin. `PackInstallManager` seeds the checksumed pack store from that asset on first use.
