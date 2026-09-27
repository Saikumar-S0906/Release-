#!/usr/bin/env python3
"""Export FLOAT32 track.tflite with frozen 64/128 → 1x2 contract."""

from __future__ import annotations

import argparse
from pathlib import Path

import tensorflow as tf

from config import SEARCH, TEMPLATE
from model import Correlate, build_model


def correlate(args):
    """Legacy Lambda used in the full-train save; needed to deserialize .keras."""
    proj, feat = args
    tiled = tf.tile(
        tf.reshape(proj, (-1, 1, 1, tf.shape(proj)[-1])),
        [1, tf.shape(feat)[1], tf.shape(feat)[2], 1],
    )
    return feat * tiled


def load_trained(keras_path: Path) -> tf.keras.Model:
    if not (keras_path.is_file() or keras_path.is_dir()):
        print("no keras checkpoint; exporting untrained graph")
        return build_model()
    return tf.keras.models.load_model(
        keras_path,
        custom_objects={
            "correlate": correlate,
            "Correlate": Correlate,
        },
        safe_mode=False,
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--keras", type=Path, default=Path(__file__).parent / "dist" / "track.keras")
    parser.add_argument("--out", type=Path, default=Path(__file__).parent / "dist" / "track.tflite")
    args = parser.parse_args()
    args.out.parent.mkdir(parents=True, exist_ok=True)

    trained = load_trained(args.keras)
    # Wrap with fixed batch-1 signatures. LiteRT may list inputs alphabetically
    # (search before template); Android bind resolves by spatial size.
    template_in = tf.keras.Input(shape=(TEMPLATE, TEMPLATE, 3), batch_size=1, name="in0_template")
    search_in = tf.keras.Input(shape=(SEARCH, SEARCH, 3), batch_size=1, name="in1_search")
    xy = trained([template_in, search_in], training=False)
    export_model = tf.keras.Model(
        inputs=[template_in, search_in],
        outputs=tf.keras.layers.Reshape((2,), name="xy")(xy),
        name="pixijoy_track_export",
    )

    converter = tf.lite.TFLiteConverter.from_keras_model(export_model)
    converter.optimizations = []
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS]
    tflite_model = converter.convert()
    args.out.write_bytes(tflite_model)

    interpreter = tf.lite.Interpreter(model_content=tflite_model)
    interpreter.allocate_tensors()
    shapes = [[int(x) for x in i["shape"]] for i in interpreter.get_input_details()]
    print(f"tflite_inputs={shapes}")
    has_64 = any(s == [1, TEMPLATE, TEMPLATE, 3] for s in shapes)
    has_128 = any(s == [1, SEARCH, SEARCH, 3] for s in shapes)
    if not (has_64 and has_128):
        raise SystemExit(f"missing 64/128 inputs: {shapes}")
    print(f"wrote {args.out} ({len(tflite_model)} bytes)")


if __name__ == "__main__":
    main()
