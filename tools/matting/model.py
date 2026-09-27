"""UNet: RGB → subject alpha matte."""

from __future__ import annotations

import tensorflow as tf

from config import SIZE


def _conv_block(x, filters: int, name: str):
    x = tf.keras.layers.Conv2D(filters, 3, padding="same", activation="relu", name=f"{name}_c1")(x)
    x = tf.keras.layers.Conv2D(filters, 3, padding="same", activation="relu", name=f"{name}_c2")(x)
    return x


def build_model() -> tf.keras.Model:
    image_in = tf.keras.Input(shape=(SIZE, SIZE, 3), name="image", dtype=tf.float32)
    x = image_in

    e1 = _conv_block(x, 64, "e1")
    p1 = tf.keras.layers.MaxPooling2D(2, name="p1")(e1)
    e2 = _conv_block(p1, 128, "e2")
    p2 = tf.keras.layers.MaxPooling2D(2, name="p2")(e2)
    e3 = _conv_block(p2, 256, "e3")
    p3 = tf.keras.layers.MaxPooling2D(2, name="p3")(e3)
    e4 = _conv_block(p3, 512, "e4")
    p4 = tf.keras.layers.MaxPooling2D(2, name="p4")(e4)

    b = _conv_block(p4, 512, "bottleneck")

    u4 = tf.keras.layers.UpSampling2D(2, name="u4")(b)
    u4 = tf.keras.layers.Concatenate(name="skip4")([u4, e4])
    d4 = _conv_block(u4, 512, "d4")
    u3 = tf.keras.layers.UpSampling2D(2, name="u3")(d4)
    u3 = tf.keras.layers.Concatenate(name="skip3")([u3, e3])
    d3 = _conv_block(u3, 256, "d3")
    u2 = tf.keras.layers.UpSampling2D(2, name="u2")(d3)
    u2 = tf.keras.layers.Concatenate(name="skip2")([u2, e2])
    d2 = _conv_block(u2, 128, "d2")
    u1 = tf.keras.layers.UpSampling2D(2, name="u1")(d2)
    u1 = tf.keras.layers.Concatenate(name="skip1")([u1, e1])
    d1 = _conv_block(u1, 64, "d1")

    out = tf.keras.layers.Conv2D(1, 1, activation="sigmoid", name="alpha")(d1)
    return tf.keras.Model(inputs=image_in, outputs=out, name="pixijoy_matting")
