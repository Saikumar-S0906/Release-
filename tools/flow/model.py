"""Siamese-style UNet: two RGB frames → dense flow (dx, dy)."""

from __future__ import annotations

import tensorflow as tf

from config import SIZE


def _conv_block(x, filters: int, name: str):
    x = tf.keras.layers.Conv2D(filters, 3, padding="same", activation="relu", name=f"{name}_c1")(x)
    x = tf.keras.layers.Conv2D(filters, 3, padding="same", activation="relu", name=f"{name}_c2")(x)
    return x


def build_model() -> tf.keras.Model:
    a = tf.keras.Input(shape=(SIZE, SIZE, 3), name="frame_t", dtype=tf.float32)
    b = tf.keras.Input(shape=(SIZE, SIZE, 3), name="frame_t1", dtype=tf.float32)
    x = tf.keras.layers.Concatenate(name="pair")([a, b])

    e1 = _conv_block(x, 64, "e1")
    p1 = tf.keras.layers.MaxPooling2D(2, name="p1")(e1)
    e2 = _conv_block(p1, 128, "e2")
    p2 = tf.keras.layers.MaxPooling2D(2, name="p2")(e2)
    e3 = _conv_block(p2, 256, "e3")
    p3 = tf.keras.layers.MaxPooling2D(2, name="p3")(e3)
    e4 = _conv_block(p3, 384, "e4")
    p4 = tf.keras.layers.MaxPooling2D(2, name="p4")(e4)

    bot = _conv_block(p4, 384, "bottleneck")

    u4 = tf.keras.layers.UpSampling2D(2, name="u4")(bot)
    u4 = tf.keras.layers.Concatenate(name="skip4")([u4, e4])
    d4 = _conv_block(u4, 384, "d4")
    u3 = tf.keras.layers.UpSampling2D(2, name="u3")(d4)
    u3 = tf.keras.layers.Concatenate(name="skip3")([u3, e3])
    d3 = _conv_block(u3, 256, "d3")
    u2 = tf.keras.layers.UpSampling2D(2, name="u2")(d3)
    u2 = tf.keras.layers.Concatenate(name="skip2")([u2, e2])
    d2 = _conv_block(u2, 128, "d2")
    u1 = tf.keras.layers.UpSampling2D(2, name="u1")(d2)
    u1 = tf.keras.layers.Concatenate(name="skip1")([u1, e1])
    d1 = _conv_block(u1, 64, "d1")

    # tanh * 0.25 → displacement / SIZE typically within ±0.25
    # Rescaling (not Lambda) so .keras checkpoints reload on Colab/Keras 3.
    raw = tf.keras.layers.Conv2D(2, 1, activation="tanh", name="flow_raw")(d1)
    out = tf.keras.layers.Rescaling(0.25, name="flow")(raw)
    return tf.keras.Model(inputs=[a, b], outputs=out, name="pixijoy_flow")
