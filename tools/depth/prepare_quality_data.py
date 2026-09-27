#!/usr/bin/env python3
"""M2 depth quality data: real NYU Depth pairs via HuggingFace `datasets` (NOT tensorflow_datasets)."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

import numpy as np

_TOOLS = Path(__file__).resolve().parents[1]
if str(_TOOLS) not in sys.path:
    sys.path.insert(0, str(_TOOLS))

from colab_fetch import pip_install

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


def fetch_hf_nyu(data: Path, n: int) -> int:
    """Real NYU depth — uses `datasets` package (Arrow), not tensorflow_datasets."""
    pip_install("datasets", "pillow", "huggingface_hub")
    from datasets import load_dataset

    candidates = [
        ("sayakpaul/nyu_depth_v2", "validation"),
        ("nlphuji/nyu_depth_v2", "validation"),
        ("fcakyon/nyuv2", "validation"),
        ("huggingface/documentation-images", None),  # last resort skip
    ]
    rgb_dir, d_dir = data / "rgb", data / "depth"
    for ds_id, split in candidates:
        if split is None:
            continue
        try:
            print(f"HF datasets: {ds_id} [{split}[:{n}]]", flush=True)
            ds = load_dataset(ds_id, split=f"{split}[:{n}]", trust_remote_code=True)
        except Exception as e:
            print(f"  skip {ds_id}: {e}", flush=True)
            continue
        count = 0
        for i, ex in enumerate(ds):
            # field names vary across mirrors
            image = ex.get("image") or ex.get("rgb") or ex.get("color")
            depth = ex.get("depth_map") or ex.get("depth") or ex.get("labeled_depth")
            if image is None or depth is None:
                # try nested
                continue
            if _save_pair(rgb_dir, d_dir, f"nyu_{count:04d}", image, depth):
                count += 1
            if count % 10 == 0 and count:
                print(f"  saved {count}", flush=True)
        if count >= 8:
            return count
    return _count_pairs(data)


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
        n = fetch_hf_nyu(args.data, max(args.n, args.min))
        print(f"HF NYU pairs={n}", flush=True)
    except Exception as e:
        print(f"HF NYU failed: {e}", flush=True)

    if n < args.min:
        raise SystemExit(
            f"QUALITY depth needs ≥{args.min} REAL rgb/depth pairs (NYU), found {n}. "
            "Do not fall back to fake depth — fix network / HF access and re-run."
        )
    (args.data / "READY").write_text("nyu_hf\n", encoding="utf-8")
    print(f"QUALITY ready: {n} real NYU depth pairs", flush=True)


if __name__ == "__main__":
    main()
