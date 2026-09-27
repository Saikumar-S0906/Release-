"""Synthetic scenes + irregular hole masks for neural inpaint."""

from __future__ import annotations

import numpy as np

from config import SIZE


def _noise_field(rng: np.random.Generator, scale: int) -> np.ndarray:
    h = max(2, SIZE // scale)
    w = max(2, SIZE // scale)
    small = rng.random((h, w, 3), dtype=np.float32)
    ys = (np.linspace(0, h - 1, SIZE)).astype(np.float32)
    xs = (np.linspace(0, w - 1, SIZE)).astype(np.float32)
    y0 = np.floor(ys).astype(np.int32)
    x0 = np.floor(xs).astype(np.int32)
    y1 = np.minimum(y0 + 1, h - 1)
    x1 = np.minimum(x0 + 1, w - 1)
    wy = (ys - y0)[:, None]
    wx = (xs - x0)[None, :]
    # Bilinear upsample of low-freq field
    out = np.zeros((SIZE, SIZE, 3), dtype=np.float32)
    for c in range(3):
        Ia = small[y0[:, None], x0[None, :], c]
        Ib = small[y0[:, None], x1[None, :], c]
        Ic = small[y1[:, None], x0[None, :], c]
        Id = small[y1[:, None], x1[None, :], c]
        top = Ia * (1 - wx) + Ib * wx
        bot = Ic * (1 - wx) + Id * wx
        out[:, :, c] = top * (1 - wy) + bot * wy
    return out


def make_scene(rng: np.random.Generator) -> np.ndarray:
    """Procedural RGB scene in 0..1."""
    base = _noise_field(rng, scale=int(rng.integers(4, 16)))
    detail = _noise_field(rng, scale=int(rng.integers(16, 48))) * 0.35
    img = np.clip(base * 0.75 + detail, 0.0, 1.0)
    # Soft gradient overlays for structure
    yy, xx = np.mgrid[0:SIZE, 0:SIZE].astype(np.float32)
    img = img + 0.08 * np.stack(
        [
            xx / SIZE,
            yy / SIZE,
            1.0 - (xx + yy) / (2 * SIZE),
        ],
        axis=-1,
    )
    # Random blobs / rectangles as "objects"
    for _ in range(int(rng.integers(2, 7))):
        color = rng.random(3, dtype=np.float32)
        if rng.random() < 0.5:
            cy, cx = rng.integers(0, SIZE, size=2)
            ry, rx = rng.integers(8, SIZE // 3, size=2)
            y0, y1 = max(0, cy - ry), min(SIZE, cy + ry)
            x0, x1 = max(0, cx - rx), min(SIZE, cx + rx)
            img[y0:y1, x0:x1] = 0.65 * img[y0:y1, x0:x1] + 0.35 * color
        else:
            cy, cx = rng.integers(0, SIZE, size=2)
            r = int(rng.integers(10, SIZE // 4))
            mask = (yy - cy) ** 2 / (r * r + 1e-6) + (xx - cx) ** 2 / (r * r + 1e-6) <= 1.0
            img[mask] = 0.55 * img[mask] + 0.45 * color
    return np.clip(img, 0.0, 1.0).astype(np.float32)


def make_mask(rng: np.random.Generator) -> np.ndarray:
    """Hole mask HxWx1, 1 = inpaint region."""
    m = np.zeros((SIZE, SIZE), dtype=np.float32)
    n_strokes = int(rng.integers(1, 5))
    for _ in range(n_strokes):
        kind = rng.integers(0, 3)
        if kind == 0:
            cy, cx = rng.integers(0, SIZE, size=2)
            r = int(rng.integers(12, SIZE // 3))
            yy, xx = np.ogrid[:SIZE, :SIZE]
            m[((yy - cy) ** 2 + (xx - cx) ** 2) <= r * r] = 1.0
        elif kind == 1:
            y0 = int(rng.integers(0, SIZE - 8))
            x0 = int(rng.integers(0, SIZE - 8))
            h = int(rng.integers(8, SIZE // 2))
            w = int(rng.integers(8, SIZE // 2))
            m[y0 : min(SIZE, y0 + h), x0 : min(SIZE, x0 + w)] = 1.0
        else:
            # Brush stroke
            y, x = float(rng.integers(0, SIZE)), float(rng.integers(0, SIZE))
            steps = int(rng.integers(20, 80))
            rad = float(rng.uniform(4.0, 18.0))
            for _ in range(steps):
                y = np.clip(y + rng.normal(0, 6), 0, SIZE - 1)
                x = np.clip(x + rng.normal(0, 6), 0, SIZE - 1)
                yy, xx = np.ogrid[:SIZE, :SIZE]
                m[((yy - y) ** 2 + (xx - x) ** 2) <= rad * rad] = 1.0
    # Ensure non-empty and not full-frame
    if m.sum() < 64:
        m[SIZE // 4 : SIZE // 2, SIZE // 4 : SIZE // 2] = 1.0
    if m.mean() > 0.85:
        m = (m > 0.5).astype(np.float32) * 0.0
        m[SIZE // 3 : 2 * SIZE // 3, SIZE // 3 : 2 * SIZE // 3] = 1.0
    return m[..., None].astype(np.float32)


def make_sample(rng: np.random.Generator) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    """Return (masked_image, mask, target). Masked pixels are zeroed."""
    target = make_scene(rng)
    mask = make_mask(rng)
    masked = target * (1.0 - mask)
    return masked.astype(np.float32), mask.astype(np.float32), target.astype(np.float32)


def batch(rng: np.random.Generator, n: int) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    imgs, masks, tgts = [], [], []
    for _ in range(n):
        i, m, t = make_sample(rng)
        imgs.append(i)
        masks.append(m)
        tgts.append(t)
    return (
        np.stack(imgs, axis=0),
        np.stack(masks, axis=0),
        np.stack(tgts, axis=0),
    )
