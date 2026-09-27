#!/usr/bin/env python3
"""Verify flow.tflite — smoke or --quality (real triplets)."""

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
from real_data import batch_flow, flow_triplets, require_ready


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
    if len(inputs) != 2 or len(outputs) < 1:
        raise SystemExit(f"expected 2 in / 1 out, got {len(inputs)}/{len(outputs)}")

    shapes = [[int(x) for x in i["shape"]] for i in inputs]
    out_shape = [int(x) for x in outputs[0]["shape"]]
    print(f"in0={shapes[0]} in1={shapes[1]} out={out_shape}")
    print(f"sha256={sha} bytes={len(data)}")

    for i in inputs:
        if i["dtype"] != np.float32:
            raise SystemExit("dtype must be float32")
    if outputs[0]["dtype"] != np.float32:
        raise SystemExit("dtype must be float32")
    if shapes[0] != [1, SIZE, SIZE, 3] or shapes[1] != [1, SIZE, SIZE, 3]:
        raise SystemExit(f"input contract failed: {shapes}")
    if out_shape != [1, SIZE, SIZE, 2]:
        raise SystemExit(f"output contract failed: {out_shape}")

    rng = np.random.default_rng(0)
    errs = []
    if args.quality:
        require_ready(args.data)
        trips = flow_triplets(args.data)
        if len(trips) < 8:
            raise SystemExit(f"quality needs ≥8 triplets, found {len(trips)}")
        for _ in range(80):
            fa, fb, flow = batch_flow(rng, 1, trips, SIZE)
            interpreter.set_tensor(inputs[0]["index"], fa)
            interpreter.set_tensor(inputs[1]["index"], fb)
            interpreter.invoke()
            pred = interpreter.get_tensor(outputs[0]["index"])[0]
            errs.append(float(np.abs(pred - flow[0]).mean()))
        label = "quality_flow_mae"
    else:
        for _ in range(60):
            fa, fb, flow = make_sample(rng)
            interpreter.set_tensor(inputs[0]["index"], fa[None, ...])
            interpreter.set_tensor(inputs[1]["index"], fb[None, ...])
            interpreter.invoke()
            pred = interpreter.get_tensor(outputs[0]["index"])[0]
            errs.append(float(np.abs(pred - flow).mean()))
        label = "smoke_flow_mae"
    mae = float(np.mean(errs))
    print(f"{label}={mae:.4f} gate={args.max_mae}")
    if mae > args.max_mae:
        raise SystemExit(f"flow MAE {mae:.4f} exceeds {args.max_mae} — do NOT pin")
    print("OK")


if __name__ == "__main__":
    main()
