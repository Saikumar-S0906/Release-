#!/usr/bin/env python3
"""Train neural inpaint UNet.

--smoke (default): synthetic scenes — NOT for Release-
--quality: real photos under --data (requires data/READY) — only ship path
Auto-resumes from *.ckpt.keras + train_state.json when present (unless --no-resume).
See tools/ML_QUALITY_BAR.md
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

import numpy as np
import tensorflow as tf

_TOOLS = Path(__file__).resolve().parents[1]
if str(_TOOLS) not in sys.path:
    sys.path.insert(0, str(_TOOLS))

import config as cfg
from config import KERAS_NAME, SEED, SIZE, apply_preset, resolved_batch
from dataset import batch as synth_batch
from dataset import make_mask
from ml_ckpt import (
    ckpt_path_for,
    load_keras_or_weights,
    maybe_auto_resume,
    save_train_state,
)
from model import build_model
from real_data import batch_inpaint, inpaint_images, require_ready


def masked_l1(y_true, y_pred, mask):
    """Primary loss on hole pixels; light L1 everywhere for stability."""
    per_pix = tf.reduce_mean(tf.abs(y_true - y_pred), axis=-1, keepdims=True)
    hole = tf.reduce_sum(per_pix * mask) / (tf.reduce_sum(mask) + 1e-6)
    valid = tf.reduce_sum(per_pix * (1.0 - mask)) / (tf.reduce_sum(1.0 - mask) + 1e-6)
    return hole + 0.1 * valid


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, default=Path(__file__).parent / "dist" / KERAS_NAME)
    parser.add_argument("--quality", action="store_true", help="Product train; requires --data READY")
    parser.add_argument("--smoke", action="store_true", help="Synthetic contract train (default)")
    parser.add_argument("--data", type=Path, default=Path(__file__).parent / "data")
    parser.add_argument("--epochs", type=int, default=None)
    parser.add_argument("--steps", type=int, default=None)
    parser.add_argument("--val-steps", type=int, default=None)
    parser.add_argument("--batch", type=int, default=None)
    parser.add_argument(
        "--resume",
        action="store_true",
        help="Force resume (also auto when ckpt exists)",
    )
    parser.add_argument(
        "--no-resume",
        action="store_true",
        help="Ignore existing ckpt and train from scratch",
    )
    args = parser.parse_args()
    quality = bool(args.quality)
    apply_preset(quality)

    images: list[Path] = []
    if quality:
        require_ready(args.data)
        images = inpaint_images(args.data)
        if len(images) < 32:
            raise SystemExit(
                f"QUALITY needs ≥32 real photos in {args.data}/rgb; found {len(images)}. "
                "See data/README.md — Places2/DIV2K/phone stills. No cheap synthetic ship."
            )
        print(f"QUALITY mode: {len(images)} real photos from {args.data}/rgb", flush=True)
    else:
        print("SMOKE mode: synthetic only — do NOT pin to Release-", flush=True)

    epochs = args.epochs if args.epochs is not None else cfg.EPOCHS
    steps = args.steps if args.steps is not None else cfg.STEPS
    val_steps = args.val_steps if args.val_steps is not None else cfg.VAL_STEPS
    batch_n = resolved_batch(args.batch)
    args.out.parent.mkdir(parents=True, exist_ok=True)

    print(f"GPUs={tf.config.list_physical_devices('GPU')} batch={batch_n} epochs={epochs}", flush=True)
    print(f"out={args.out}", flush=True)
    tf.keras.utils.set_random_seed(SEED)
    train_rng = np.random.default_rng(SEED)
    val_rng = np.random.default_rng(SEED + 1)

    print("building model…", flush=True)
    model = build_model()
    print(f"model built params={model.count_params()}", flush=True)
    lr0 = 5e-4 if quality else 1e-3
    optimizer = tf.keras.optimizers.Adam(lr0)
    ckpt_path = ckpt_path_for(args.out)
    print(f"ckpt path={ckpt_path} exists={ckpt_path.is_file()}", flush=True)
    if ckpt_path.is_file():
        print(f"ckpt size_mb={ckpt_path.stat().st_size / (1024 * 1024):.1f} (Drive loads can take many minutes on CPU)", flush=True)

    print("resume/load weights…", flush=True)
    _, start_epoch, best_val, bad = maybe_auto_resume(
        model,
        args.out,
        resume=True if args.resume else None,
        no_resume=args.no_resume,
    )
    print(f"ready to train start_epoch={start_epoch}/{epochs} best_val={best_val}", flush=True)

    @tf.function
    def train_step(images_t, masks, targets):
        with tf.GradientTape() as tape:
            preds = model([images_t, masks], training=True)
            loss = masked_l1(targets, preds, masks)
        grads = tape.gradient(loss, model.trainable_variables)
        optimizer.apply_gradients(zip(grads, model.trainable_variables))
        return loss

    @tf.function
    def val_step(images_t, masks, targets):
        preds = model([images_t, masks], training=False)
        return masked_l1(targets, preds, masks)

    def next_batch(rng: np.random.Generator):
        if quality:
            return batch_inpaint(rng, batch_n, images, args.data, SIZE, make_mask_fn=make_mask)
        return synth_batch(rng, batch_n)

    model.summary()
    history = {"loss": [], "val_loss": []}
    patience = 12 if quality else 8

    if start_epoch >= epochs:
        print(f"already finished epochs ({start_epoch}/{epochs}); skipping train loop", flush=True)
    for epoch in range(start_epoch, epochs):
        losses = []
        print(f"epoch {epoch + 1}/{epochs} starting ({steps} steps)…", flush=True)
        for step in range(steps):
            if step == 0:
                print("  first step: building TF graph (CPU can take 5–20+ min with no other logs)…", flush=True)
            imgs, masks, tgts = next_batch(train_rng)
            loss = train_step(tf.constant(imgs), tf.constant(masks), tf.constant(tgts))
            losses.append(float(loss.numpy()))
            if step == 0 or (step + 1) % 25 == 0:
                print(f"  step {step + 1}/{steps} loss={losses[-1]:.4f}", flush=True)
        train_loss_e = float(np.mean(losses))

        vlosses = []
        for _ in range(val_steps):
            imgs, masks, tgts = next_batch(val_rng)
            vloss = val_step(tf.constant(imgs), tf.constant(masks), tf.constant(tgts))
            vlosses.append(float(vloss.numpy()))
        val_loss_e = float(np.mean(vlosses))
        history["loss"].append(train_loss_e)
        history["val_loss"].append(val_loss_e)
        print(f"epoch {epoch + 1}/{epochs} loss={train_loss_e:.4f} val_loss={val_loss_e:.4f}", flush=True)

        if val_loss_e < best_val - 1e-5:
            best_val = val_loss_e
            bad = 0
            model.save(ckpt_path)
            save_train_state(
                args.out,
                next_epoch=epoch + 1,
                best_val=best_val,
                bad=bad,
                mode="QUALITY" if quality else "SMOKE",
            )
            print(f"  saved best -> {ckpt_path}", flush=True)
        else:
            bad += 1
            save_train_state(
                args.out,
                next_epoch=epoch + 1,
                best_val=best_val,
                bad=bad,
                mode="QUALITY" if quality else "SMOKE",
            )
            if bad >= patience:
                print("early stopping", flush=True)
                break
            if bad % 3 == 0:
                new_lr = float(optimizer.learning_rate.numpy()) * 0.5
                if new_lr >= 1e-5:
                    optimizer.learning_rate.assign(new_lr)
                    print(f"  lr -> {new_lr}", flush=True)

    if ckpt_path.is_file():
        model = load_keras_or_weights(ckpt_path, build_model)
    model.save(args.out)
    np.savez(args.out.parent / "train_history.npz", **{k: np.array(v) for k, v in history.items()})
    mode = "QUALITY" if quality else "SMOKE"
    print(f"wrote {args.out} best_val={best_val:.4f} mode={mode}", flush=True)
    if quality:
        print("OK to export + verify --quality + pin to Release- after gates.", flush=True)
    else:
        print("SMOKE only — re-run with --quality before Release- pin.", flush=True)


if __name__ == "__main__":
    main()
