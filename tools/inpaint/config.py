"""PixiJoy neural inpaint LiteRT lab.

Trains a UNet on synthetic hole-filling and exports `inpaint.tflite`
matching the frozen app contract (dual-input preferred):

  In0 image  FLOAT32 NHWC 1x256x256x3
  In1 mask   FLOAT32 NHWC 1x256x256x1   (1 = hole / inpaint region)
  Out0       FLOAT32 NHWC 1x256x256x3   RGB reconstruction

Full training budget — do not shrink for convenience. Quality is not
compromised for APK size: if the exported TFLite exceeds
MAX_APK_BUNDLE_BYTES, ship via GitHub Release Download (same pattern as FILM).

License: model weights produced by this script are Apache-2.0 for PixiJoy use.
Training data is synthetic (procedural), not third-party photos.
"""

from __future__ import annotations

SIZE = 256
# Full-fledged budget — wall clock is expected to be long on CPU/GPU.
EPOCHS = 25
BATCH = 8
STEPS = 300
VAL_STEPS = 40
SEED = 42
# Masked-region L1 gate after export (0..1 RGB).
MAX_SMOKE_MASKED_MAE = 0.12
# Soft APK budget; larger models go to Release artifacts — never downsize the net.
MAX_APK_BUNDLE_BYTES = 3 * 1024 * 1024
