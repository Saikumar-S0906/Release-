#!/usr/bin/env python3
"""M2 depth quality data: official NYU Depth V2 labeled.mat (no Hugging Face / TFDS)."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

import numpy as np

_TOOLS = Path(__file__).resolve().parents[1]
if str(_TOOLS) not in sys.path:
    sys.path.insert(0, str(_TOOLS))

from colab_fetch import NYU_DEPTH_LABELED_URLS, download_first, pip_install

SIZE = 256


def _count_pairs(data: Path) -> int:
    from real_data import depth_pairs

    return len(depth_pairs(data))


def _save_pair(rgb_dir: Path, d_dir: Path, stem: str, rgb, depth) -> bool:
    from PIL import Image

    rgb_dir.mkdir(parents=True, exist_ok=True)
    d_dir.mkdir(parents=True, exist_ok=True)
    if hasattr(rgb, "convert"):
        img = rgb.convert("RGB").resize((SIZE, SIZE))
    else:
        arr = np.asarray(rgb)
        if arr.dtype != np.uint8:
            arr = (np.clip(arr, 0, 1) * 255).astype(np.uint8) if float(np.max(arr)) <= 1.5 else arr.astype(np.uint8)
        img = Image.fromarray(arr).resize((SIZE, SIZE))
    d = np.asarray(depth, dtype=np.float32)
    if d.ndim == 3:
        d = d[..., 0]
    finite = np.isfinite(d)
    if int(finite.sum()) < 32:
        return False
    lo, hi = float(np.percentile(d[finite], 2)), float(np.percentile(d[finite], 98))
    if hi - lo < 1e-6:
        return False
    d01 = np.clip((d - lo) / (hi - lo), 0, 1)
    d_img = Image.fromarray((d01 * 255).astype(np.uint8), mode="L").resize((SIZE, SIZE))
    img.save(rgb_dir / f"{stem}.png")
    d_img.save(d_dir / f"{stem}.png")
    return True


def fetch_nyu_official(data: Path, n: int) -> int:
    """Download official NYU labeled .mat and extract RGB/depth pairs via h5py."""
    pip_install("h5py", "pillow")
    import h5py
    from PIL import Image

    tmp = data / "_dl"
    tmp.mkdir(parents=True, exist_ok=True)
    mat_path = tmp / "nyu_depth_v2_labeled.mat"
    if not mat_path.is_file() or mat_path.stat().st_size < 1_000_000_000:
        download_first(NYU_DEPTH_LABELED_URLS, mat_path)
    else:
        print(f"reuse cached {mat_path} ({mat_path.stat().st_size} bytes)", flush=True)

    rgb_dir, d_dir = data / "rgb", data / "depth"
    count = 0
    with h5py.File(mat_path, "r") as f:
        images = f["images"]
        depths = f["depths"]
        total = int(images.shape[0])
        # Uniform stride so we sample across scenes, not only the first N frames.
        step = max(1, total // max(n, 1))
        indices = list(range(0, total, step))[:n]
        print(f"NYU official: extracting {len(indices)}/{total} pairs → {SIZE}px", flush=True)
        for i in indices:
            # Stored as (3, 640, 480) / (640, 480) — transpose to HWC / HW.
            img = np.asarray(images[i]).transpose(2, 1, 0)
            if img.dtype != np.uint8:
                img = np.clip(img, 0, 255).astype(np.uint8)
            depth = np.asarray(depths[i], dtype=np.float32).T
            if _save_pair(rgb_dir, d_dir, f"nyu_{count:04d}", Image.fromarray(img), depth):
                count += 1
            if count % 10 == 0 and count:
                print(f"  saved {count}", flush=True)
    return count


def main() -> None:
    if str(Path(__file__).resolve().parent) not in sys.path:
        sys.path.insert(0, str(Path(__file__).resolve().parent))
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", type=Path, default=Path(__file__).parent / "data")
    parser.add_argument("--min", type=int, default=32)
    parser.add_argument("--n", type=int, default=80)
    args = parser.parse_args()
    args.data.mkdir(parents=True, exist_ok=True)
    if _count_pairs(args.data) >= args.min and (args.data / "READY").is_file():
        print(f"already ready: {_count_pairs(args.data)}", flush=True)
        return

    n = 0
    try:
        n = fetch_nyu_official(args.data, max(args.n, args.min))
        print(f"NYU official pairs={n}", flush=True)
    except Exception as e:
        print(f"NYU official failed: {e}", flush=True)

    if n < args.min:
        raise SystemExit(
            f"QUALITY depth needs ≥{args.min} REAL rgb/depth pairs (NYU), found {n}. "
            "Do not fall back to fake depth — fix network / NYU host access and re-run."
        )
    (args.data / "READY").write_text("nyu_official\n", encoding="utf-8")
    print(f"QUALITY ready: {n} real NYU depth pairs (official .mat, no HF)", flush=True)


if __name__ == "__main__":
    main()
