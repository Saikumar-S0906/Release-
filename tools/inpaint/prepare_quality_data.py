#!/usr/bin/env python3
"""M1 inpaint quality data: real DIV2K photos (no TFDS, no upload)."""

from __future__ import annotations

import argparse
import shutil
import sys
from pathlib import Path

_TOOLS = Path(__file__).resolve().parents[1]
if str(_TOOLS) not in sys.path:
    sys.path.insert(0, str(_TOOLS))

from colab_fetch import DIV2K_URLS, download_first, extract_images_from_zip, pip_install


def _count(rgb: Path) -> int:
    exts = {".png", ".jpg", ".jpeg", ".webp", ".bmp"}
    return sum(1 for p in rgb.iterdir() if p.suffix.lower() in exts) if rgb.is_dir() else 0


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", type=Path, default=Path(__file__).parent / "data")
    parser.add_argument("--min", type=int, default=32)
    args = parser.parse_args()
    pip_install("pillow")
    rgb = args.data / "rgb"
    rgb.mkdir(parents=True, exist_ok=True)
    if _count(rgb) >= args.min and (args.data / "READY").is_file():
        print(f"already ready: {_count(rgb)}", flush=True)
        return

    tmp = args.data / "_dl"
    tmp.mkdir(parents=True, exist_ok=True)
    zpath = tmp / "div2k.zip"
    download_first(DIV2K_URLS, zpath)
    paths = extract_images_from_zip(zpath, rgb)
    shutil.rmtree(tmp, ignore_errors=True)
    n = _count(rgb)
    if n < args.min:
        raise SystemExit(f"need ≥{args.min} real photos, got {n} (extracted {len(paths)})")
    (args.data / "READY").write_text("div2k\n", encoding="utf-8")
    print(f"QUALITY ready: {n} real DIV2K photos", flush=True)


if __name__ == "__main__":
    main()
