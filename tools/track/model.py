"""Siamese encoder + correlation head for point track."""

from __future__ import annotations

import tensorflow as tf
from keras.saving import register_keras_serializable

from config import SEARCH, TEMPLATE


@register_keras_serializable(package="pixijoy")
class Correlate(tf.keras.layers.Layer):
    """Broadcast-multiply template embedding over search feature map."""

    def call(self, inputs):
        proj, feat = inputs
        tiled = tf.tile(
            tf.reshape(proj, (-1, 1, 1, tf.shape(proj)[-1])),
            [1, tf.shape(feat)[1], tf.shape(feat)[2], 1],
        )
        return feat * tiled

    def get_config(self):
        return super().get_config()


def build_model() -> tf.keras.Model:
    template_in = tf.keras.Input(shape=(TEMPLATE, TEMPLATE, 3), name="template", dtype=tf.float32)
    search_in = tf.keras.Input(shape=(SEARCH, SEARCH, 3), name="search", dtype=tf.float32)

    def backbone(name: str) -> tf.keras.Model:
        inp = tf.keras.Input(shape=(None, None, 3), name=f"{name}_in")
        x = tf.keras.layers.Conv2D(32, 3, strides=2, padding="same", activation="relu")(inp)
        x = tf.keras.layers.Conv2D(64, 3, strides=2, padding="same", activation="relu")(x)
        x = tf.keras.layers.Conv2D(128, 3, strides=2, padding="same", activation="relu")(x)
        x = tf.keras.layers.Conv2D(128, 3, padding="same", activation="relu")(x)
        return tf.keras.Model(inp, x, name=name)

    enc = backbone("enc")
    t_feat = enc(template_in)
    s_feat = enc(search_in)

    t_vec = tf.keras.layers.GlobalAveragePooling2D()(t_feat)
    s_vec = tf.keras.layers.GlobalAveragePooling2D()(s_feat)
    t_proj = tf.keras.layers.Dense(128, activation="relu")(t_vec)

    corr = Correlate(name="corr")([t_proj, s_feat])
    corr = tf.keras.layers.Conv2D(64, 3, padding="same", activation="relu")(corr)
    heat = tf.keras.layers.Conv2D(1, 1, activation="sigmoid")(corr)
    heat_flat = tf.keras.layers.Flatten()(heat)

    merged = tf.keras.layers.Concatenate()([t_vec, s_vec, heat_flat])
    x = tf.keras.layers.Dense(256, activation="relu")(merged)
    x = tf.keras.layers.Dropout(0.1)(x)
    x = tf.keras.layers.Dense(128, activation="relu")(x)
    out = tf.keras.layers.Dense(2, activation="sigmoid", name="xy")(x)
    return tf.keras.Model(inputs=[template_in, search_in], outputs=out, name="pixijoy_track")
