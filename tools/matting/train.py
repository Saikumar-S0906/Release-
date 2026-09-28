#!/usr/bin/env python3
"""Train subject matting UNet.

--smoke (default): synthetic — NOT for Release-
--quality: real data (--data READY) — only ship path
Auto-resumes from *.ckpt.keras + train_state.json unless --no-resume.
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
from model import build_model
from real_data import batch_matte, matte_pairs, require_ready
from ml_ckpt import (
    ckpt_path_for,
    load_keras_or_weights,
    maybe_auto_resume,
    save_train_state,
)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, default=Path(__file__).parent / "dist" / KERAS_NAME)
    parser.add_argument("--quality", action="store_true")
    parser.add_argument("--data", type=Path, default=Path(__file__).parent / "data")
    parser.add_argument("--epochs", type=int, default=None)
    parser.add_argument("--steps", type=int, default=None)
    parser.add_argument("--val-steps", type=int, default=None)
    parser.add_argument("--batch", type=int, default=None)
    parser.add_argument("--resume", action="store_true", help="Force resume (also auto when ckpt exists)")
    parser.add_argument("--no-resume", action="store_true", help="Ignore existing ckpt and train from scratch")
    args = parser.parse_args()
    quality = bool(args.quality)
    apply_preset(quality)
    pairs = []
    if quality:
        require_ready(args.data)
        pairs = matte_pairs(args.data)
        if len(pairs) < 8:
            raise SystemExit(f"QUALITY needs ≥8 rgb/alpha pairs; found {len(pairs)}")
        print(f"QUALITY mode: {len(pairs)} matte pairs", flush=True)
    else:
        print("SMOKE mode — do NOT pin to Release-", flush=True)

    epochs = args.epochs if args.epochs is not None else cfg.EPOCHS
    steps = args.steps if args.steps is not None else cfg.STEPS
    val_steps = args.val_steps if args.val_steps is not None else cfg.VAL_STEPS
    batch_n = resolved_batch(args.batch)
    args.out.parent.mkdir(parents=True, exist_ok=True)

    print(f"GPUs={tf.config.list_physical_devices('GPU')} batch={batch_n}", flush=True)
    tf.keras.utils.set_random_seed(SEED)
    train_rng = np.random.default_rng(SEED)
    val_rng = np.random.default_rng(SEED + 1)

    model = build_model()
    model.compile(
        optimizer=tf.keras.optimizers.Adam(1e-3 if not quality else 5e-4),
        loss="binary_crossentropy",
    )
    model.summary()

    ckpt_path = ckpt_path_for(args.out)
    _, start_epoch, best_val, bad = maybe_auto_resume(
        model,
        args.out,
        resume=True if args.resume else None,
        no_resume=args.no_resume,
    )
    patience = 12 if quality else 8
    history = {"loss": [], "val_loss": []}

    def next_batch(rng: np.random.Generator):
        if quality:
            return batch_matte(rng, batch_n, pairs, SIZE)
        return synth_batch(rng, batch_n)

    if start_epoch >= epochs:
        print(f"already finished epochs ({start_epoch}/{epochs})", flush=True)
    for epoch in range(start_epoch, epochs):
        losses = []
        for step in range(steps):
            imgs, alphas = next_batch(train_rng)
            loss = model.train_on_batch(imgs, alphas)
            losses.append(float(loss))
            if step == 0 or (step + 1) % 25 == 0:
                print(f"  step {step + 1}/{steps} loss={losses[-1]:.4f}", flush=True)
        train_loss_e = float(np.mean(losses))
        vlosses = [float(model.test_on_batch(*next_batch(val_rng))) for _ in range(val_steps)]
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
                lr = float(tf.keras.backend.get_value(model.optimizer.learning_rate)) * 0.5
                if lr >= 1e-5:
                    model.optimizer.learning_rate.assign(lr)
                    print(f"  lr -> {lr}", flush=True)

    if ckpt_path.is_file():
        model = load_keras_or_weights(
            ckpt_path,
            build_model,
            compile_fn=lambda m: m.compile(
                optimizer=tf.keras.optimizers.Adam(1e-3), loss="binary_crossentropy"
            ),
        )
    model.save(args.out)
    np.savez(args.out.parent / "train_history.npz", **{k: np.array(v) for k, v in history.items()})
    print(f"wrote {args.out} best_val={best_val:.4f} mode={'QUALITY' if quality else 'SMOKE'}", flush=True)


if __name__ == "__main__":
    main()
