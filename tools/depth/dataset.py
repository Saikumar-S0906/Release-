"""Synthetic layered scenes with ground-truth relative depth."""

from __future__ import annotations

import numpy as np

from config import SIZE


def make_sample(rng: np.random.Generator) -> tuple[np.ndarray, np.ndarray]:
    """Return (rgb HxWx3, depth HxWx1) in 0..1. Far=background, near=foreground."""
    yy, xx = np.mgrid[0:SIZE, 0:SIZE].astype(np.float32)
    # Far backdrop
    bg = rng.random(3, dtype=np.float32) * 0.4 + 0.3
    img = np.zeros((SIZE, SIZE, 3), dtype=np.float32)
    img[:] = bg
    depth = np.ones((SIZE, SIZE), dtype=np.float32)  # far = 1

    n_layers = int(rng.integers(3, 8))
    for i in range(n_layers):
        # Closer layers get smaller depth values
        z = 0.85 - (i + 1) * (0.7 / n_layers) + float(rng.uniform(-0.05, 0.05))
        z = float(np.clip(z, 0.05, 0.95))
        color = rng.random(3, dtype=np.float32)
        cy, cx = int(rng.integers(0, SIZE)), int(rng.integers(0, SIZE))
        if rng.random() < 0.5:
            ry, rx = int(rng.integers(12, SIZE // 2)), int(rng.integers(12, SIZE // 2))
            y0, y1 = max(0, cy - ry), min(SIZE, cy + ry)
            x0, x1 = max(0, cx - rx), min(SIZE, cx + rx)
            patch = img[y0:y1, x0:x1]
            img[y0:y1, x0:x1] = 0.35 * patch + 0.65 * color
            depth[y0:y1, x0:x1] = z
        else:
            r = float(rng.integers(16, SIZE // 3))
            mask = (yy - cy) ** 2 + (xx - cx) ** 2 <= r * r
            img[mask] = 0.35 * img[mask] + 0.65 * color
            depth[mask] = z

    # Soft vignette noise so net cannot cheat on flat color alone
    noise = rng.normal(0, 0.02, size=img.shape).astype(np.float32)
    img = np.clip(img + noise, 0.0, 1.0)
    return img.astype(np.float32), depth[..., None].astype(np.float32)


def batch(rng: np.random.Generator, n: int) -> tuple[np.ndarray, np.ndarray]:
    imgs, depths = [], []
    for _ in range(n):
        i, d = make_sample(rng)
        imgs.append(i)
        depths.append(d)
    return np.stack(imgs, axis=0), np.stack(depths, axis=0)
