"""Synthetic frame pairs with known global + local translation flow."""

from __future__ import annotations

import numpy as np

from config import SIZE


def _scene(rng: np.random.Generator) -> np.ndarray:
    img = rng.random((SIZE, SIZE, 3), dtype=np.float32) * 0.35 + 0.2
    yy, xx = np.mgrid[0:SIZE, 0:SIZE].astype(np.float32)
    for _ in range(int(rng.integers(3, 8))):
        color = rng.random(3, dtype=np.float32)
        cy, cx = rng.integers(0, SIZE, size=2)
        r = int(rng.integers(10, SIZE // 4))
        mask = (yy - cy) ** 2 + (xx - cx) ** 2 <= r * r
        img[mask] = 0.4 * img[mask] + 0.6 * color
    return np.clip(img, 0, 1).astype(np.float32)


def _warp(img: np.ndarray, dx: float, dy: float) -> np.ndarray:
    """Translate img by (dx, dy) pixels with edge clamp (nearest)."""
    yy, xx = np.mgrid[0:SIZE, 0:SIZE]
    src_y = np.clip(np.rint(yy - dy).astype(np.int32), 0, SIZE - 1)
    src_x = np.clip(np.rint(xx - dx).astype(np.int32), 0, SIZE - 1)
    return img[src_y, src_x]


def make_sample(rng: np.random.Generator) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    """Return (frame_t, frame_t1, flow HxWx2) with flow = displacement/SIZE."""
    base = _scene(rng)
    # Global camera shake + small local bump
    dx = float(rng.uniform(-12.0, 12.0))
    dy = float(rng.uniform(-12.0, 12.0))
    frame_t = base
    frame_t1 = _warp(base, dx, dy)
    # Optional second blob motion on top of global (approximate with same flow field)
    flow = np.zeros((SIZE, SIZE, 2), dtype=np.float32)
    flow[..., 0] = dx / SIZE
    flow[..., 1] = dy / SIZE
    # Add mild spatially varying component
    if rng.random() < 0.4:
        bump = float(rng.uniform(-4.0, 4.0))
        cy, cx = int(rng.integers(0, SIZE)), int(rng.integers(0, SIZE))
        r = int(rng.integers(20, SIZE // 3))
        yy, xx = np.ogrid[:SIZE, :SIZE]
        m = (yy - cy) ** 2 + (xx - cx) ** 2 <= r * r
        flow[m, 0] += bump / SIZE
        # Rebuild t1 approximately: re-warp with mean flow for simplicity
        mean_dx = float(flow[..., 0].mean() * SIZE)
        mean_dy = float(flow[..., 1].mean() * SIZE)
        frame_t1 = _warp(base, mean_dx, mean_dy)
        flow[..., 0] = mean_dx / SIZE
        flow[..., 1] = mean_dy / SIZE

    noise = rng.normal(0, 0.01, size=frame_t1.shape).astype(np.float32)
    frame_t1 = np.clip(frame_t1 + noise, 0, 1)
    return frame_t.astype(np.float32), frame_t1.astype(np.float32), flow.astype(np.float32)


def batch(rng: np.random.Generator, n: int) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    a, b, f = [], [], []
    for _ in range(n):
        fa, fb, fl = make_sample(rng)
        a.append(fa)
        b.append(fb)
        f.append(fl)
    return np.stack(a), np.stack(b), np.stack(f)
