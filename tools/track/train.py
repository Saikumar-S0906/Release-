#!/usr/bin/env python3
"""Full-fledged train for F1 point-track: many epochs, val split, LR schedule."""

from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import tensorflow as tf

from config import BATCH, EPOCHS, SEED, STEPS, VAL_STEPS
from dataset import batch
from model import build_model


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, default=Path(__file__).parent / "dist" / "track.keras")
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
    model.compile(
        optimizer=tf.keras.optimizers.Adam(1e-3),
        loss="mse",
        metrics=["mae"],
    )
    model.summary()

    def train_gen():
        while True:
            t, s, y = batch(train_rng, args.batch)
            yield (t, s), y

    def val_gen():
        while True:
            t, s, y = batch(val_rng, args.batch)
            yield (t, s), y

    callbacks = [
        tf.keras.callbacks.ReduceLROnPlateau(
            monitor="val_loss",
            factor=0.5,
            patience=3,
            min_lr=1e-5,
            verbose=1,
        ),
        tf.keras.callbacks.EarlyStopping(
            monitor="val_loss",
            patience=8,
            restore_best_weights=True,
            verbose=1,
        ),
        tf.keras.callbacks.ModelCheckpoint(
            filepath=str(args.out.with_suffix(".ckpt.keras")),
            monitor="val_loss",
            save_best_only=True,
            verbose=1,
        ),
    ]

    history = model.fit(
        train_gen(),
        steps_per_epoch=args.steps,
        epochs=args.epochs,
        validation_data=val_gen(),
        validation_steps=args.val_steps,
        callbacks=callbacks,
        verbose=1,
    )
    model.save(args.out)
    hist_path = args.out.parent / "train_history.npz"
    np.savez(
        hist_path,
        loss=np.array(history.history.get("loss", [])),
        mae=np.array(history.history.get("mae", [])),
        val_loss=np.array(history.history.get("val_loss", [])),
        val_mae=np.array(history.history.get("val_mae", [])),
    )
    print(f"wrote {args.out}")
    print(f"wrote {hist_path}")
    print(
        f"budget: epochs={args.epochs} steps={args.steps} "
        f"val_steps={args.val_steps} batch={args.batch} "
        f"pairs~={args.epochs * args.steps * args.batch}"
    )


if __name__ == "__main__":
    main()
