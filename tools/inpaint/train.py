#!/usr/bin/env python3
"""Full-fledged train for neural inpaint: many epochs, val split, LR schedule."""

from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import tensorflow as tf

from config import BATCH, EPOCHS, SEED, STEPS, VAL_STEPS
from dataset import batch
from model import build_model


def masked_l1(y_true, y_pred, mask):
    """Primary loss on hole pixels; light L1 everywhere for stability."""
    per_pix = tf.reduce_mean(tf.abs(y_true - y_pred), axis=-1, keepdims=True)
    hole = tf.reduce_sum(per_pix * mask) / (tf.reduce_sum(mask) + 1e-6)
    valid = tf.reduce_sum(per_pix * (1.0 - mask)) / (tf.reduce_sum(1.0 - mask) + 1e-6)
    return hole + 0.1 * valid


class InpaintLoss(tf.keras.losses.Loss):
    def call(self, y_true, y_pred):
        # y_true is target RGB; mask is passed via model inputs — use sample_weight instead.
        return tf.reduce_mean(tf.abs(y_true - y_pred))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, default=Path(__file__).parent / "dist" / "inpaint.keras")
    parser.add_argument("--epochs", type=int, default=EPOCHS)
    parser.add_argument("--steps", type=int, default=STEPS)
    parser.add_argument("--val-steps", type=int, default=VAL_STEPS)
    parser.add_argument("--batch", type=int, default=BATCH)
    args = parser.parse_args()
    args.out.parent.mkdir(parents=True, exist_ok=True)

    tf.keras.utils.set_random_seed(SEED)
    train_rng = np.random.default_rng(SEED)
    val_rng = np.random.default_rng(SEED + 1)

    model = build_model()

    def loss_fn(y_true, y_pred):
        # Recover mask from the fact that known pixels in input match target;
        # train_gen feeds sample_weight as mask via a custom train step below.
        return tf.reduce_mean(tf.abs(y_true - y_pred))

    # Custom training: pass mask into loss via a wrapper model output of (pred, mask)
    image_in = model.input[0]
    mask_in = model.input[1]
    pred = model.output

    def train_loss(y_true, y_pred_and_mask):
        # Not used — we compile with a custom train step
        return tf.constant(0.0)

    optimizer = tf.keras.optimizers.Adam(1e-3)

    @tf.function
    def train_step(images, masks, targets):
        with tf.GradientTape() as tape:
            preds = model([images, masks], training=True)
            loss = masked_l1(targets, preds, masks)
        grads = tape.gradient(loss, model.trainable_variables)
        optimizer.apply_gradients(zip(grads, model.trainable_variables))
        return loss

    @tf.function
    def val_step(images, masks, targets):
        preds = model([images, masks], training=False)
        return masked_l1(targets, preds, masks)

    model.summary()
    best_val = float("inf")
    history = {"loss": [], "val_loss": []}
    patience = 8
    bad = 0
    ckpt_path = args.out.with_suffix(".ckpt.keras")

    for epoch in range(args.epochs):
        losses = []
        for _ in range(args.steps):
            imgs, masks, tgts = batch(train_rng, args.batch)
            loss = train_step(
                tf.constant(imgs),
                tf.constant(masks),
                tf.constant(tgts),
            )
            losses.append(float(loss.numpy()))
        train_loss_e = float(np.mean(losses))

        vlosses = []
        for _ in range(args.val_steps):
            imgs, masks, tgts = batch(val_rng, args.batch)
            vloss = val_step(
                tf.constant(imgs),
                tf.constant(masks),
                tf.constant(tgts),
            )
            vlosses.append(float(vloss.numpy()))
        val_loss_e = float(np.mean(vlosses))
        history["loss"].append(train_loss_e)
        history["val_loss"].append(val_loss_e)
        print(f"epoch {epoch + 1}/{args.epochs} loss={train_loss_e:.4f} val_loss={val_loss_e:.4f}")

        if val_loss_e < best_val - 1e-5:
            best_val = val_loss_e
            bad = 0
            model.save(ckpt_path)
            print(f"  saved best -> {ckpt_path}")
        else:
            bad += 1
            if bad >= patience:
                print("early stopping")
                break
        # LR decay
        if bad > 0 and bad % 3 == 0:
            new_lr = float(optimizer.learning_rate.numpy()) * 0.5
            if new_lr >= 1e-5:
                optimizer.learning_rate.assign(new_lr)
                print(f"  lr -> {new_lr}")

    if ckpt_path.is_file():
        model = tf.keras.models.load_model(ckpt_path, safe_mode=False)
    model.save(args.out)
    np.savez(args.out.parent / "train_history.npz", **{k: np.array(v) for k, v in history.items()})
    print(f"wrote {args.out} best_val={best_val:.4f}")


if __name__ == "__main__":
    main()
