"""PixiJoy F1 point-track LiteRT lab.

Trains a Siamese/correlation net on synthetic moving subjects and exports
`track.tflite` matching the frozen app contract:

  In0 template  FLOAT32 NHWC 1x64x64x3
  In1 search    FLOAT32 NHWC 1x128x128x3
  Out0          FLOAT32 1x2   normalized (x, y) in 0..1

Full training budget (do not shrink for convenience):
  EPOCHS × STEPS × BATCH synthetic pairs, plus a held-out val set.

License: model weights produced by this script are Apache-2.0 for PixiJoy use.
Training data is synthetic (procedural), not third-party video.
"""

from __future__ import annotations

TEMPLATE = 64
SEARCH = 128
# Full-fledged budget — wall clock is expected to be long on CPU.
EPOCHS = 20
BATCH = 32
STEPS = 250
VAL_STEPS = 40
SEED = 42
# Offline smoke gate after export (normalized L1 over 10-frame path).
MAX_SMOKE_MAE = 0.18
