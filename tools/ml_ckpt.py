"""Shared checkpoint load helpers for Wave M labs (Colab / Keras 3 safe)."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Callable

import tensorflow as tf


def load_keras_or_weights(
    path: Path,
    build_fn: Callable[[], tf.keras.Model],
    *,
    compile_fn: Callable[[tf.keras.Model], None] | None = None,
) -> tf.keras.Model:
    """Load full .keras model, or rebuild + load_weights (with by_name fallback)."""
    try:
        return tf.keras.models.load_model(path, safe_mode=False)
    except Exception as e:
        print(f"load_model {path.name} failed: {e}", flush=True)
        model = build_fn()
        if compile_fn is not None:
            compile_fn(model)
        try:
            model.load_weights(path)
            print(f"loaded weights from {path}", flush=True)
            return model
        except Exception as e2:
            print(f"load_weights strict failed ({e2}); by_name skip_mismatch", flush=True)
            model.load_weights(path, skip_mismatch=True, by_name=True)
            return model


def resume_into(
    model: tf.keras.Model,
    ckpt_path: Path,
) -> bool:
    """Load checkpoint weights into an already-built model. Returns True if loaded."""
    if not ckpt_path.is_file():
        print(f"resume skipped — no file {ckpt_path}", flush=True)
        return False
    print(f"loading weights from {ckpt_path} …", flush=True)
    try:
        model.load_weights(ckpt_path)
        print(f"resumed weights from {ckpt_path}", flush=True)
        return True
    except Exception as e:
        print(f"resume load_weights failed ({e}); by_name skip_mismatch", flush=True)
        try:
            model.load_weights(ckpt_path, skip_mismatch=True, by_name=True)
            print(f"resumed (partial) from {ckpt_path}", flush=True)
            return True
        except Exception as e2:
            print(f"resume failed: {e2}", flush=True)
            return False


def ckpt_path_for(out: Path) -> Path:
    return out.with_suffix(".ckpt.keras")


def state_path_for(out: Path) -> Path:
    return out.parent / "train_state.json"


def load_train_state(out: Path) -> dict:
    p = state_path_for(out)
    if not p.is_file():
        return {}
    try:
        return json.loads(p.read_text(encoding="utf-8"))
    except Exception as e:
        print(f"train_state read failed: {e}", flush=True)
        return {}


def save_train_state(out: Path, **fields) -> None:
    out.parent.mkdir(parents=True, exist_ok=True)
    p = state_path_for(out)
    cur = load_train_state(out)
    cur.update(fields)
    p.write_text(json.dumps(cur, indent=2), encoding="utf-8")


def maybe_auto_resume(
    model: tf.keras.Model,
    out: Path,
    *,
    resume: bool | None,
    no_resume: bool,
) -> tuple[bool, int, float, int]:
    """
    Auto-resume when a checkpoint exists unless --no-resume.
    Returns (did_resume, start_epoch, best_val, bad_streak).
    """
    ckpt = ckpt_path_for(out)
    if no_resume:
        print("resume disabled (--no-resume)", flush=True)
        return False, 0, float("inf"), 0
    want = resume if resume is not None else ckpt.is_file()
    if not want:
        return False, 0, float("inf"), 0
    ok = resume_into(model, ckpt)
    if not ok:
        return False, 0, float("inf"), 0
    st = load_train_state(out)
    start_epoch = int(st.get("next_epoch", 0))
    best_val = float(st.get("best_val", float("inf")))
    bad = int(st.get("bad", 0))
    print(
        f"auto-resume: next_epoch={start_epoch} best_val={best_val:.4f} bad={bad} ckpt={ckpt}",
        flush=True,
    )
    return True, start_epoch, best_val, bad


def load_for_export(
    keras_path: Path,
    build_fn: Callable[[], tf.keras.Model],
    *,
    allow_untrained: bool,
) -> tf.keras.Model:
    candidates = [keras_path, keras_path.with_suffix(".ckpt.keras")]
    seen: set[Path] = set()
    for path in candidates:
        if path in seen:
            continue
        seen.add(path)
        if not (path.is_file() or path.is_dir()):
            continue
        return load_keras_or_weights(path, build_fn)
    if allow_untrained:
        print("no keras checkpoint; exporting untrained graph")
        return build_fn()
    raise SystemExit(
        "FATAL: no loadable trained checkpoint — refusing untrained export. "
        "Re-run train.py, then export (do not pin random weights)."
    )
