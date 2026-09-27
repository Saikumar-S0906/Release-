"""Synthetic subject-on-background mattes."""

from __future__ import annotations

import numpy as np

from config import SIZE


def _noise_bg(rng: np.random.Generator) -> np.ndarray:
    h = max(2, SIZE // int(rng.integers(4, 20)))
    w = max(2, SIZE // int(rng.integers(4, 20)))
    small = rng.random((h, w, 3), dtype=np.float32)
    ys = np.linspace(0, h - 1, SIZE)
    xs = np.linspace(0, w - 1, SIZE)
    y0 = np.floor(ys).astype(np.int32)
    x0 = np.floor(xs).astype(np.int32)
    y1 = np.minimum(y0 + 1, h - 1)
    x1 = np.minimum(x0 + 1, w - 1)
    out = np.zeros((SIZE, SIZE, 3), dtype=np.float32)
    for c in range(3):
        Ia = small[y0[:, None], x0[None, :], c]
        Ib = small[y0[:, None], x1[None, :], c]
        Ic = small[y1[:, None], x0[None, :], c]
        Id = small[y1[:, None], x1[None, :], c]
        wy = (ys - y0)[:, None]
        wx = (xs - x0)[None, :]
        top = Ia * (1 - wx) + Ib * wx
        bot = Ic * (1 - wx) + Id * wx
        out[:, :, c] = top * (1 - wy) + bot * wy
    return np.clip(out, 0, 1)


def make_sample(rng: np.random.Generator) -> tuple[np.ndarray, np.ndarray]:
    """Return (rgb, alpha HxWx1). Soft-edged subject blob(s)."""
    bg = _noise_bg(rng)
    alpha = np.zeros((SIZE, SIZE), dtype=np.float32)
    yy, xx = np.mgrid[0:SIZE, 0:SIZE].astype(np.float32)
    fg_color = rng.random(3, dtype=np.float32)

    for _ in range(int(rng.integers(1, 3))):
        cy, cx = float(rng.integers(SIZE // 5, 4 * SIZE // 5)), float(rng.integers(SIZE // 5, 4 * SIZE // 5))
        ry = float(rng.uniform(SIZE * 0.12, SIZE * 0.35))
        rx = float(rng.uniform(SIZE * 0.12, SIZE * 0.35))
        # Soft ellipse
        d = ((yy - cy) / (ry + 1e-6)) ** 2 + ((xx - cx) / (rx + 1e-6)) ** 2
        soft = np.clip(1.0 - (d - 0.7) / 0.35, 0.0, 1.0)
        soft = np.where(d <= 0.7, 1.0, soft)
        soft = np.where(d >= 1.05, 0.0, soft)
        alpha = np.maximum(alpha, soft.astype(np.float32))

    if alpha.mean() < 0.02:
        alpha[SIZE // 3 : 2 * SIZE // 3, SIZE // 3 : 2 * SIZE // 3] = 1.0

    a3 = alpha[..., None]
    img = bg * (1.0 - a3) + fg_color * a3
    img = np.clip(img + rng.normal(0, 0.015, size=img.shape).astype(np.float32), 0, 1)
    return img.astype(np.float32), a3.astype(np.float32)


def batch(rng: np.random.Generator, n: int) -> tuple[np.ndarray, np.ndarray]:
    imgs, alphas = [], []
    for _ in range(n):
        i, a = make_sample(rng)
        imgs.append(i)
        alphas.append(a)
    return np.stack(imgs, axis=0), np.stack(alphas, axis=0)
