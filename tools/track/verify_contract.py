#!/usr/bin/env python3
"""Verify track.tflite matches the Kotlin PackModelContract.isPointTrack gate."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path

import numpy as np
import tensorflow as tf

from config import MAX_SMOKE_MAE, SEARCH, TEMPLATE
from dataset import make_pair


def as_list(shape) -> list[int]:
    return [int(x) for x in shape]


def track_input_indices(shapes: list[list[int]]) -> tuple[int, int] | None:
    if len(shapes) < 2:
        return None
    for i, j in ((0, 1), (1, 0)):
        a, b = shapes[i], shapes[j]
        if (
            len(a) == 4
            and len(b) == 4
            and a[0] == 1
            and b[0] == 1
            and a[3] == 3
            and b[3] == 3
            and a[1] == TEMPLATE
            and a[2] == TEMPLATE
            and b[1] == SEARCH
            and b[2] == SEARCH
        ):
            return i, j
    return None


def is_point_track(template: list[int], search: list[int], out: list[int]) -> bool:
    return (
        template == [1, TEMPLATE, TEMPLATE, 3]
        and search == [1, SEARCH, SEARCH, 3]
        and out == [1, 2]
    )


def pair_mae(interpreter: tf.lite.Interpreter, tmpl_i: int, search_i: int, n: int = 200) -> float:
    """Same distribution as training — primary smoke gate."""
    rng = np.random.default_rng(0)
    ins = interpreter.get_input_details()
    outs = interpreter.get_output_details()
    errs: list[float] = []
    for _ in range(n):
        t, s, y = make_pair(rng)
        interpreter.set_tensor(ins[tmpl_i]["index"], t[None, ...])
        interpreter.set_tensor(ins[search_i]["index"], s[None, ...])
        interpreter.invoke()
        pred = interpreter.get_tensor(outs[0]["index"]).reshape(2)
        errs.append(float(np.abs(pred - y).sum()))
    return float(np.mean(errs))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", type=Path, default=Path(__file__).parent / "dist" / "track.tflite")
    parser.add_argument("--max-mae", type=float, default=None)
    args = parser.parse_args()
    if args.max_mae is None:
        args.max_mae = MAX_SMOKE_MAE
    if not args.model.is_file():
        raise SystemExit(f"missing model: {args.model}")

    data = args.model.read_bytes()
    sha = hashlib.sha256(data).hexdigest()
    interpreter = tf.lite.Interpreter(model_content=data)
    interpreter.allocate_tensors()
    inputs = interpreter.get_input_details()
    outputs = interpreter.get_output_details()
    if len(inputs) < 2 or len(outputs) < 1:
        raise SystemExit("expected 2 inputs and 1 output")

    shapes = [as_list(i["shape"]) for i in inputs]
    indices = track_input_indices(shapes)
    if indices is None:
        raise SystemExit(f"shape contract failed (no 64/128 pair): {shapes}")
    tmpl_i, search_i = indices
    t_shape = shapes[tmpl_i]
    s_shape = shapes[search_i]
    o_shape = as_list(outputs[0]["shape"])
    print(f"template_idx={tmpl_i} shape={t_shape} {inputs[tmpl_i]['dtype']}")
    print(f"search_idx={search_i} shape={s_shape} {inputs[search_i]['dtype']}")
    print(f"out={o_shape} {outputs[0]['dtype']}")
    print(f"sha256={sha}")
    print(f"bytes={len(data)}")

    if inputs[tmpl_i]["dtype"] != np.float32 or inputs[search_i]["dtype"] != np.float32:
        raise SystemExit("dtype must be float32")
    if outputs[0]["dtype"] != np.float32:
        raise SystemExit("dtype must be float32")
    if not is_point_track(t_shape, s_shape, o_shape):
        raise SystemExit(f"shape contract failed: {t_shape} {s_shape} {o_shape}")

    mae = pair_mae(interpreter, tmpl_i, search_i)
    print(f"pair_mae={mae:.4f}")
    if mae > args.max_mae:
        raise SystemExit(f"pair MAE {mae:.4f} exceeds {args.max_mae}")
    print("OK")


if __name__ == "__main__":
    main()
