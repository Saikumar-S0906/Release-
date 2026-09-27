#!/usr/bin/env python3
"""Generate synthetic QA clips (PNG sequences) for F1 track verification.

Writes under testdata/track/:
  move_lr/   — subject moves left → right
  approach/  — subject grows (toward camera)
  occlude/   — subject moves with a crossing bar

Encode to MP4 yourself if needed, e.g.:
  ffmpeg -y -framerate 15 -i move_lr/frame_%03d.png -c:v libx264 -pix_fmt yuv420p move_lr.mp4
"""

from __future__ import annotations

from pathlib import Path

import numpy as np

try:
    from PIL import Image
except ImportError as e:  # pragma: no cover
    raise SystemExit("pip install pillow") from e

ROOT = Path(__file__).resolve().parents[2] / "testdata" / "track"
SIZE = 256
FRAMES = 24


def _save(dir_name: str, frames: list[np.ndarray]) -> None:
    out = ROOT / dir_name
    out.mkdir(parents=True, exist_ok=True)
    for i, frame in enumerate(frames):
        rgb = (np.clip(frame, 0, 1) * 255).astype(np.uint8)
        Image.fromarray(rgb, mode="RGB").save(out / f"frame_{i:03d}.png")
    (out / "README.txt").write_text(
        f"{FRAMES} frames @ {SIZE}px. Subject centers listed in centers.csv\n",
        encoding="utf-8",
    )


def _paint(cx: float, cy: float, radius: float) -> np.ndarray:
    frame = np.full((SIZE, SIZE, 3), 0.12, dtype=np.float32)
    yy, xx = np.ogrid[:SIZE, :SIZE]
    mask = (xx - cx) ** 2 + (yy - cy) ** 2 <= radius**2
    frame[mask] = (0.9, 0.25, 0.2)
    return frame


def main() -> None:
    ROOT.mkdir(parents=True, exist_ok=True)

    # L→R
    frames = []
    centers = []
    for i in range(FRAMES):
        t = i / (FRAMES - 1)
        cx = 40 + t * (SIZE - 80)
        cy = SIZE / 2
        frames.append(_paint(cx, cy, 14))
        centers.append((cx / (SIZE - 1), cy / (SIZE - 1)))
    _save("move_lr", frames)
    (ROOT / "move_lr" / "centers.csv").write_text(
        "frame,x,y\n" + "\n".join(f"{i},{x:.4f},{y:.4f}" for i, (x, y) in enumerate(centers)),
        encoding="utf-8",
    )

    # Approach (grow)
    frames = []
    centers = []
    for i in range(FRAMES):
        t = i / (FRAMES - 1)
        r = 8 + t * 28
        cx = cy = SIZE / 2
        frames.append(_paint(cx, cy, r))
        centers.append((0.5, 0.5))
    _save("approach", frames)
    (ROOT / "approach" / "centers.csv").write_text(
        "frame,x,y\n" + "\n".join(f"{i},{x:.4f},{y:.4f}" for i, (x, y) in enumerate(centers)),
        encoding="utf-8",
    )

    # Partial occlusion
    frames = []
    centers = []
    for i in range(FRAMES):
        t = i / (FRAMES - 1)
        cx = 50 + t * (SIZE - 100)
        cy = SIZE / 2
        frame = _paint(cx, cy, 14)
        bar_x = int(SIZE * 0.45)
        frame[:, bar_x : bar_x + 18] *= 0.2
        frames.append(frame)
        centers.append((cx / (SIZE - 1), cy / (SIZE - 1)))
    _save("occlude", frames)
    (ROOT / "occlude" / "centers.csv").write_text(
        "frame,x,y\n" + "\n".join(f"{i},{x:.4f},{y:.4f}" for i, (x, y) in enumerate(centers)),
        encoding="utf-8",
    )
    print(f"wrote QA sequences under {ROOT}")


if __name__ == "__main__":
    main()
