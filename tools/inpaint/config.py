"""PixiJoy neural inpaint LiteRT lab (Wave M1 / E1).

Contract (FLOAT32):
  In0 image  NHWC 1x256x256x3
  In1 mask   NHWC 1x256x256x1   (1 = hole / inpaint region)
  Out0       NHWC 1x256x256x3   RGB reconstruction

Ship rule: only --quality trains on real photos may go to Release-.
Smoke = contract only. See tools/ML_QUALITY_BAR.md

Size: if TFLite > MAX_APK_BUNDLE_BYTES → GitHub Release Download (never shrink the net).
"""

from __future__ import annotations

import sys
from pathlib import Path

_TOOLS = Path(__file__).resolve().parents[1]
if str(_TOOLS) not in sys.path:
    sys.path.insert(0, str(_TOOLS))

from ml_train_presets import QUALITY, SMOKE, resolve_preset  # noqa: E402

SIZE = 256
SEED = 42
MAX_APK_BUNDLE_BYTES = 3 * 1024 * 1024
ARTIFACT = "inpaint.tflite"
KERAS_NAME = "inpaint.keras"

# Defaults = smoke (contract). Override via train.py --quality
EPOCHS = SMOKE.epochs
BATCH = 8
STEPS = SMOKE.steps
VAL_STEPS = SMOKE.val_steps
# Smoke gate (synthetic holes). Quality gate is stricter (real photos).
MAX_SMOKE_MASKED_MAE = 0.12
QUALITY_MAX_MASKED_MAE = 0.08


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
    global EPOCHS, STEPS, VAL_STEPS
    p = resolve_preset(quality)
    EPOCHS = p.epochs
    STEPS = p.steps
    VAL_STEPS = p.val_steps
