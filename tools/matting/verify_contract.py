#!/usr/bin/env python3
"""Verify matting.tflite — smoke or --quality (real pairs)."""

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

from config import ARTIFACT, MAX_APK_BUNDLE_BYTES, MAX_SMOKE_MAE, QUALITY_MAX_SMOKE_MAE, SIZE
from dataset import make_sample
from real_data import batch_matte, matte_pairs, require_ready


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", type=Path, default=Path(__file__).parent / "dist" / ARTIFACT)
    parser.add_argument("--quality", action="store_true")
    parser.add_argument("--data", type=Path, default=Path(__file__).parent / "data")
    parser.add_argument("--max-mae", type=float, default=None)
    args = parser.parse_args()
    if args.max_mae is None:
        args.max_mae = QUALITY_MAX_SMOKE_MAE if args.quality else MAX_SMOKE_MAE
    if not args.model.is_file():
        raise SystemExit(f"missing model: {args.model}")

    data = args.model.read_bytes()
    sha = hashlib.sha256(data).hexdigest()
    interpreter = tf.lite.Interpreter(model_content=data)
    interpreter.allocate_tensors()
    inputs = interpreter.get_input_details()
    outputs = interpreter.get_output_details()
    if len(inputs) != 1 or len(outputs) < 1:
        raise SystemExit(f"expected 1 in / 1 out, got {len(inputs)}/{len(outputs)}")

    in_shape = [int(x) for x in inputs[0]["shape"]]
    out_shape = [int(x) for x in outputs[0]["shape"]]
    print(f"in={in_shape} {inputs[0]['dtype']}")
    print(f"out={out_shape} {outputs[0]['dtype']}")
    print(f"sha256={sha} bytes={len(data)}")

    if inputs[0]["dtype"] != np.float32 or outputs[0]["dtype"] != np.float32:
        raise SystemExit("dtype must be float32")
    if in_shape != [1, SIZE, SIZE, 3] or out_shape not in ([1, SIZE, SIZE, 1], [1, SIZE, SIZE]):
        raise SystemExit(f"shape contract failed: {in_shape} -> {out_shape}")

    rng = np.random.default_rng(0)
    errs = []
    if args.quality:
        require_ready(args.data)
        pairs = matte_pairs(args.data)
        if len(pairs) < 8:
            raise SystemExit(f"quality needs ≥8 pairs, found {len(pairs)}")
        for _ in range(100):
            img, alpha = batch_matte(rng, 1, pairs, SIZE)
            interpreter.set_tensor(inputs[0]["index"], img)
            interpreter.invoke()
            pred = interpreter.get_tensor(outputs[0]["index"])[0]
            if pred.ndim == 2:
                pred = pred[..., None]
            errs.append(float(np.abs(pred - alpha[0]).mean()))
        label = "quality_matte_mae"
    else:
        for _ in range(80):
            img, alpha = make_sample(rng)
            interpreter.set_tensor(inputs[0]["index"], img[None, ...])
            interpreter.invoke()
            pred = interpreter.get_tensor(outputs[0]["index"])[0]
            if pred.ndim == 2:
                pred = pred[..., None]
            errs.append(float(np.abs(pred - alpha).mean()))
        label = "smoke_matte_mae"
    mae = float(np.mean(errs))
    print(f"{label}={mae:.4f} gate={args.max_mae}")
    if mae > args.max_mae:
        raise SystemExit(f"matte MAE {mae:.4f} exceeds {args.max_mae} — do NOT pin")
    print("OK")


if __name__ == "__main__":
    main()
