#!/usr/bin/env python3
"""Export FLOAT32 depth.tflite — frozen 256 RGB→depth contract."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

import tensorflow as tf

_TOOLS = Path(__file__).resolve().parents[1]
if str(_TOOLS) not in sys.path:
    sys.path.insert(0, str(_TOOLS))

from config import ARTIFACT, KERAS_NAME, MAX_APK_BUNDLE_BYTES, SIZE
from ml_ckpt import load_for_export
from model import build_model


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--keras", type=Path, default=Path(__file__).parent / "dist" / KERAS_NAME)
    parser.add_argument("--out", type=Path, default=Path(__file__).parent / "dist" / ARTIFACT)
    parser.add_argument("--allow-untrained", action="store_true")
    args = parser.parse_args()
    args.out.parent.mkdir(parents=True, exist_ok=True)

    trained = load_for_export(args.keras, build_model, allow_untrained=args.allow_untrained)
    image_in = tf.keras.Input(shape=(SIZE, SIZE, 3), batch_size=1, name="in0_image")
    depth = trained(image_in, training=False)
    export_model = tf.keras.Model(inputs=image_in, outputs=depth, name="pixijoy_depth_export")

    converter = tf.lite.TFLiteConverter.from_keras_model(export_model)
    converter.optimizations = []
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS]
    tflite_model = converter.convert()
    args.out.write_bytes(tflite_model)

    interpreter = tf.lite.Interpreter(model_content=tflite_model)
    interpreter.allocate_tensors()
    shapes = [[int(x) for x in i["shape"]] for i in interpreter.get_input_details()]
    out_shapes = [[int(x) for x in o["shape"]] for o in interpreter.get_output_details()]
    print(f"tflite_inputs={shapes}")
    print(f"tflite_outputs={out_shapes}")
    print(f"wrote {args.out} ({len(tflite_model)} bytes)")
    if len(tflite_model) > MAX_APK_BUNDLE_BYTES:
        print(f"SIZE_NOTE: ship via Release- (>{MAX_APK_BUNDLE_BYTES})")
    else:
        print(f"SIZE_NOTE: within APK budget ({MAX_APK_BUNDLE_BYTES})")


if __name__ == "__main__":
    main()
