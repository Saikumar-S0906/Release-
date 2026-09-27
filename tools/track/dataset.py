"""Synthetic template/search pairs with labeled normalized centers."""

from __future__ import annotations

import numpy as np

from config import SEARCH, TEMPLATE


def _blob(canvas: np.ndarray, cx: float, cy: float, radius: float, color: np.ndarray) -> None:
    h, w = canvas.shape[:2]
    yy, xx = np.ogrid[:h, :w]
    mask = (xx - cx) ** 2 + (yy - cy) ** 2 <= radius**2
    canvas[mask] = color


def _rect(canvas: np.ndarray, cx: float, cy: float, rw: float, rh: float, color: np.ndarray) -> None:
    h, w = canvas.shape[:2]
    x0 = int(max(0, cx - rw / 2))
    x1 = int(min(w, cx + rw / 2))
    y0 = int(max(0, cy - rh / 2))
    y1 = int(min(h, cy + rh / 2))
    if x1 > x0 and y1 > y0:
        canvas[y0:y1, x0:x1] = color


def _crop(frame: np.ndarray, cx: float, cy: float, size: int) -> np.ndarray:
    h, w = frame.shape[:2]
    half = size // 2
    x0 = int(round(cx)) - half
    y0 = int(round(cy)) - half
    out = np.zeros((size, size, 3), dtype=np.float32)
    src_y0 = max(0, y0)
    src_x0 = max(0, x0)
    src_y1 = min(h, y0 + size)
    src_x1 = min(w, x0 + size)
    dst_y0 = src_y0 - y0
    dst_x0 = src_x0 - x0
    dst_y1 = dst_y0 + (src_y1 - src_y0)
    dst_x1 = dst_x0 + (src_x1 - src_x0)
    if src_y1 > src_y0 and src_x1 > src_x0:
        out[dst_y0:dst_y1, dst_x0:dst_x1] = frame[src_y0:src_y1, src_x0:src_x1]
    return out


def _subject(
    canvas: np.ndarray,
    cx: float,
    cy: float,
    rng: np.random.Generator,
    color: np.ndarray,
) -> None:
    if rng.random() < 0.55:
        _blob(canvas, cx, cy, float(rng.uniform(8.0, 20.0)), color)
    else:
        _rect(
            canvas,
            cx,
            cy,
            float(rng.uniform(14.0, 36.0)),
            float(rng.uniform(14.0, 36.0)),
            color,
        )


def make_pair(rng: np.random.Generator, frame_size: int = 256) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    """Return template (64), search (128), label (x,y) in 0..1 for current center."""
    # Varied backgrounds: noise, gradients, clutter blobs.
    frame = rng.uniform(0.02, 0.35, size=(frame_size, frame_size, 3)).astype(np.float32)
    if rng.random() < 0.4:
        gx = np.linspace(0, 1, frame_size, dtype=np.float32)
        gy = np.linspace(0, 1, frame_size, dtype=np.float32)[:, None]
        frame = frame * 0.5 + (gx * 0.15 + gy * 0.15)[..., None]
    # Distractors
    for _ in range(int(rng.integers(0, 4))):
        dc = rng.uniform(0.2, 0.6, size=(3,)).astype(np.float32)
        _subject(
            frame,
            float(rng.uniform(0.1, 0.9)) * (frame_size - 1),
            float(rng.uniform(0.1, 0.9)) * (frame_size - 1),
            rng,
            dc,
        )

    color = rng.uniform(0.55, 1.0, size=(3,)).astype(np.float32)
    margin = 28.0
    x0 = float(rng.uniform(margin, frame_size - margin))
    y0 = float(rng.uniform(margin, frame_size - margin))
    # Motion up to ~search half-width so the subject stays findable in the search crop.
    dx = float(rng.uniform(-28.0, 28.0))
    dy = float(rng.uniform(-28.0, 28.0))
    x1 = float(np.clip(x0 + dx, margin, frame_size - margin))
    y1 = float(np.clip(y0 + dy, margin, frame_size - margin))

    prev = frame.copy()
    curr = frame.copy()
    _subject(prev, x0, y0, rng, color)
    # Slight color/size jitter on current frame.
    color2 = np.clip(color + rng.uniform(-0.08, 0.08, size=3).astype(np.float32), 0.4, 1.0)
    _subject(curr, x1, y1, rng, color2)
    if rng.random() < 0.25:
        # Mild occlusion bar on current frame.
        bar_y = int(np.clip(y1 + rng.uniform(-10, 10), 0, frame_size - 6))
        curr[bar_y : bar_y + 5, :] *= 0.35

    template = _crop(prev, x0, y0, TEMPLATE)
    search = _crop(curr, x0, y0, SEARCH)
    label = np.array([x1 / (frame_size - 1), y1 / (frame_size - 1)], dtype=np.float32)
    return template, search, label


def batch(rng: np.random.Generator, n: int) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    templates = np.zeros((n, TEMPLATE, TEMPLATE, 3), dtype=np.float32)
    searches = np.zeros((n, SEARCH, SEARCH, 3), dtype=np.float32)
    labels = np.zeros((n, 2), dtype=np.float32)
    for i in range(n):
        t, s, y = make_pair(rng)
        templates[i] = t
        searches[i] = s
        labels[i] = y
    return templates, searches, labels
