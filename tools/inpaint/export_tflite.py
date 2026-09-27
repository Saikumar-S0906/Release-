#!/usr/bin/env python3
"""Export FLOAT32 inpaint.tflite with frozen 256 dual-input contract."""

from __future__ import annotations

import argparse
from pathlib import Path

import tensorflow as tf

from config import MAX_APK_BUNDLE_BYTES, SIZE
from model import build_model


def load_trained(keras_path: Path) -> tf.keras.Model:
    if not (keras_path.is_file() or keras_path.is_dir()):
        print("no keras checkpoint; exporting untrained graph")
        return build_model()
    return tf.keras.models.load_model(keras_path, safe_mode=False)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--keras", type=Path, default=Path(__file__).parent / "dist" / "inpaint.keras")
    parser.add_argument("--out", type=Path, default=Path(__file__).parent / "dist" / "inpaint.tflite")
    args = parser.parse_args()
    args.out.parent.mkdir(parents=True, exist_ok=True)

    trained = load_trained(args.keras)
    image_in = tf.keras.Input(shape=(SIZE, SIZE, 3), batch_size=1, name="in0_image")
    mask_in = tf.keras.Input(shape=(SIZE, SIZE, 1), batch_size=1, name="in1_mask")
    rgb = trained([image_in, mask_in], training=False)
    export_model = tf.keras.Model(
        inputs=[image_in, mask_in],
        outputs=rgb,
        name="pixijoy_inpaint_export",
    )

    converter = tf.lite.TFLiteConverter.from_keras_model(export_model)
    converter.optimizations = []  # full FLOAT32 — no quality compromise via quant
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
        print(
            f"SIZE_NOTE: {len(tflite_model)} > APK budget {MAX_APK_BUNDLE_BYTES}; "
            "ship via GitHub Release Download — do not shrink the network."
        )
    else:
        print(f"SIZE_NOTE: within APK budget ({MAX_APK_BUNDLE_BYTES}); may bundle as asset.")


if __name__ == "__main__":
    main()
