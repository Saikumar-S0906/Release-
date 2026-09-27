#!/usr/bin/env python3
"""Verify inpaint.tflite matches PackModelContract.isInpaintRgbAndMask.

--smoke (default): synthetic masked MAE gate
--quality: real-photo masked MAE gate (stricter) — required before Release-
"""

from __future__ import annotations

import argparse
import hashlib
import sys
from pathlib import Path

import numpy as np
import tensorflow as tf

_TOOLS = Path(__file__).resolve().parents[1]
if str(_TOOLS) not in sys.path:
    sys.path.insert(0, str(_TOOLS))

from config import (
    MAX_APK_BUNDLE_BYTES,
    MAX_SMOKE_MASKED_MAE,
    QUALITY_MAX_MASKED_MAE,
    SIZE,
)
from dataset import make_mask, make_sample
from real_data import batch_inpaint, inpaint_images, require_ready


def as_list(shape) -> list[int]:
    return [int(x) for x in shape]


def is_inpaint_rgb_and_mask(image: list[int], mask: list[int], output: list[int]) -> bool:
    if image != [1, SIZE, SIZE, 3] or output != [1, SIZE, SIZE, 3]:
        return False
    if mask == [1, SIZE, SIZE, 1] or mask == [1, SIZE, SIZE]:
        return True
    return False


def _eval_batch(
    interpreter: tf.lite.Interpreter,
    img_i: int,
    mask_i: int,
    masked: np.ndarray,
    mask: np.ndarray,
    target: np.ndarray,
) -> float | None:
    ins = interpreter.get_input_details()
    outs = interpreter.get_output_details()
    interpreter.set_tensor(ins[img_i]["index"], masked[None, ...])
    m = mask
    if len(ins[mask_i]["shape"]) == 3:
        m = mask[..., 0]
    interpreter.set_tensor(ins[mask_i]["index"], m[None, ...])
    interpreter.invoke()
    pred = interpreter.get_tensor(outs[0]["index"])[0]
    hole = mask[..., 0] > 0.5
    if hole.sum() < 1:
        return None
    return float(np.abs(pred[hole] - target[hole]).mean())


def masked_mae_smoke(interpreter: tf.lite.Interpreter, img_i: int, mask_i: int, n: int = 80) -> float:
    rng = np.random.default_rng(0)
    errs: list[float] = []
    for _ in range(n):
        masked, mask, target = make_sample(rng)
        e = _eval_batch(interpreter, img_i, mask_i, masked, mask, target)
        if e is not None:
            errs.append(e)
    return float(np.mean(errs)) if errs else 999.0


def masked_mae_quality(
    interpreter: tf.lite.Interpreter,
    img_i: int,
    mask_i: int,
    data_dir: Path,
    n: int = 100,
) -> float:
    require_ready(data_dir)
    images = inpaint_images(data_dir)
    if len(images) < 8:
        raise SystemExit(f"quality verify needs ≥8 real photos in {data_dir}/rgb")
    rng = np.random.default_rng(1)
    errs: list[float] = []
    for _ in range(n):
        masked_b, mask_b, tgt_b = batch_inpaint(
            rng, 1, images, data_dir, SIZE, make_mask_fn=make_mask
        )
        e = _eval_batch(interpreter, img_i, mask_i, masked_b[0], mask_b[0], tgt_b[0])
        if e is not None:
            errs.append(e)
    return float(np.mean(errs)) if errs else 999.0


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", type=Path, default=Path(__file__).parent / "dist" / "inpaint.tflite")
    parser.add_argument("--quality", action="store_true", help="Real-photo gate (Release- only)")
    parser.add_argument("--data", type=Path, default=Path(__file__).parent / "data")
    parser.add_argument("--max-mae", type=float, default=None)
    args = parser.parse_args()
    if args.max_mae is None:
        args.max_mae = QUALITY_MAX_MASKED_MAE if args.quality else MAX_SMOKE_MASKED_MAE
    if not args.model.is_file():
        raise SystemExit(f"missing model: {args.model}")

    data = args.model.read_bytes()
    sha = hashlib.sha256(data).hexdigest()
    interpreter = tf.lite.Interpreter(model_content=data)
    interpreter.allocate_tensors()
    inputs = interpreter.get_input_details()
    outputs = interpreter.get_output_details()
    if len(inputs) != 2 or len(outputs) < 1:
        raise SystemExit(f"expected 2 inputs and 1 output, got {len(inputs)}/{len(outputs)}")

    shapes = [as_list(i["shape"]) for i in inputs]
    img_i, mask_i = (0, 1) if shapes[0][-1] == 3 else (1, 0)
    o_shape = as_list(outputs[0]["shape"])
    print(f"image_idx={img_i} shape={shapes[img_i]} {inputs[img_i]['dtype']}")
    print(f"mask_idx={mask_i} shape={shapes[mask_i]} {inputs[mask_i]['dtype']}")
    print(f"out={o_shape} {outputs[0]['dtype']}")
    print(f"sha256={sha}")
    print(f"bytes={len(data)}")
    if len(data) > MAX_APK_BUNDLE_BYTES:
        print(f"placement=github_release (over APK budget {MAX_APK_BUNDLE_BYTES})")
    else:
        print("placement=apk_assets")

    if inputs[img_i]["dtype"] != np.float32 or inputs[mask_i]["dtype"] != np.float32:
        raise SystemExit("dtype must be float32")
    if outputs[0]["dtype"] != np.float32:
        raise SystemExit("dtype must be float32")
    if not is_inpaint_rgb_and_mask(shapes[img_i], shapes[mask_i], o_shape):
        raise SystemExit(f"shape contract failed: {shapes} -> {o_shape}")

    if args.quality:
        mae = masked_mae_quality(interpreter, img_i, mask_i, args.data)
        print(f"quality_masked_mae={mae:.4f} gate={args.max_mae}")
    else:
        mae = masked_mae_smoke(interpreter, img_i, mask_i)
        print(f"smoke_masked_mae={mae:.4f} gate={args.max_mae}")
    if mae > args.max_mae:
        raise SystemExit(f"masked MAE {mae:.4f} exceeds {args.max_mae} — do NOT pin to Release-")
    print("OK")


if __name__ == "__main__":
    main()
