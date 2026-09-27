"""PixiJoy video/subject matting LiteRT lab (Wave M3 / H6).

Contract (FLOAT32):
  In0 image  NHWC 1x256x256x3
  Out0 alpha NHWC 1x256x256x1   1 = foreground subject, 0 = background

Ship rule: only --quality trains on real data may go to Release-.
See tools/ML_QUALITY_BAR.md
"""

from __future__ import annotations

import sys
from pathlib import Path

_TOOLS = Path(__file__).resolve().parents[1]
if str(_TOOLS) not in sys.path:
    sys.path.insert(0, str(_TOOLS))

from ml_train_presets import QUALITY, SMOKE, resolve_preset  # noqa: E402

SIZE = 256
SEED = 43
MAX_APK_BUNDLE_BYTES = 3 * 1024 * 1024
ARTIFACT = "matting.tflite"
KERAS_NAME = "matting.keras"

EPOCHS = SMOKE.epochs
BATCH = 8
STEPS = SMOKE.steps
VAL_STEPS = SMOKE.val_steps
MAX_SMOKE_MAE = SMOKE.max_smoke_mae
QUALITY_MAX_SMOKE_MAE = QUALITY.max_smoke_mae


def resolved_batch(override: int | None = None) -> int:
    if override is not None:
        return override
    try:
        import tensorflow as tf

        if tf.config.list_physical_devices("GPU"):
            return 16
    except Exception:
        pass
    return BATCH


def apply_preset(quality: bool) -> None:
    global EPOCHS, STEPS, VAL_STEPS, MAX_SMOKE_MAE
    p = resolve_preset(quality)
    EPOCHS = p.epochs
    STEPS = p.steps
    VAL_STEPS = p.val_steps
    MAX_SMOKE_MAE = p.max_smoke_mae
