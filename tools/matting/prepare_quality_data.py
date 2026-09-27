#!/usr/bin/env python3
"""M3 matting quality data: Oxford-IIIT Pet (real RGB + real trimap alpha). No TFDS."""

from __future__ import annotations

import argparse
import shutil
import sys
import tarfile
from pathlib import Path

import numpy as np

_TOOLS = Path(__file__).resolve().parents[1]
if str(_TOOLS) not in sys.path:
    sys.path.insert(0, str(_TOOLS))

from colab_fetch import download, pip_install

IMAGES_URL = "https://www.robots.ox.ac.uk/~vgg/data/pets/data/images.tar.gz"
ANNS_URL = "https://www.robots.ox.ac.uk/~vgg/data/pets/data/annotations.tar.gz"
# HuggingFace mirror of the same VGG tarballs (if OX is blocked)
IMAGES_HF = "https://huggingface.co/datasets/timm/oxford-iiit-pet/resolve/main/images.tar.gz"
ANNS_HF = "https://huggingface.co/datasets/timm/oxford-iiit-pet/resolve/main/annotations.tar.gz"
SIZE = 256


def _count_pairs(data: Path) -> int:
    from real_data import matte_pairs

    return len(matte_pairs(data))


def main() -> None:
    pip_install("pillow")
    from PIL import Image

    parser = argparse.ArgumentParser()
    parser.add_argument("--data", type=Path, default=Path(__file__).parent / "data")
    parser.add_argument("--min", type=int, default=32)
    parser.add_argument("--n", type=int, default=100)
    args = parser.parse_args()
    args.data.mkdir(parents=True, exist_ok=True)
    if _count_pairs(args.data) >= args.min and (args.data / "READY").is_file():
        print(f"already ready: {_count_pairs(args.data)}", flush=True)
        return

    tmp = args.data / "_dl"
    tmp.mkdir(parents=True, exist_ok=True)
    img_tar, ann_tar = tmp / "images.tar.gz", tmp / "annotations.tar.gz"
    err = None
    for iu, au in ((IMAGES_URL, ANNS_URL), (IMAGES_HF, ANNS_HF)):
        try:
            download(iu, img_tar, label="pet images")
            download(au, ann_tar, label="pet annotations")
            err = None
            break
        except Exception as e:
            err = e
            print(f"mirror fail: {e}", flush=True)
    if err is not None and not img_tar.is_file():
        raise SystemExit(f"Oxford Pet download failed: {err}")

    with tarfile.open(img_tar, "r:gz") as t:
        t.extractall(tmp)
    with tarfile.open(ann_tar, "r:gz") as t:
        t.extractall(tmp)

    images_root = tmp / "images"
    trimaps_root = tmp / "annotations" / "trimaps"
    if not images_root.is_dir():
        # some mirrors nest differently
        for p in tmp.rglob("images"):
            if p.is_dir():
                images_root = p
                break
    if not trimaps_root.is_dir():
        for p in tmp.rglob("trimaps"):
            if p.is_dir():
                trimaps_root = p
                break

    rgb_out, a_out = args.data / "rgb", args.data / "alpha"
    rgb_out.mkdir(parents=True, exist_ok=True)
    a_out.mkdir(parents=True, exist_ok=True)

    stems = sorted(
        {
            p.stem
            for p in images_root.iterdir()
            if p.suffix.lower() in {".jpg", ".jpeg", ".png"} and not p.name.startswith(".")
        }
    )
    count = 0
    for stem in stems:
        if count >= args.n:
            break
        trip = trimaps_root / f"{stem}.png"
        if not trip.is_file() or stem.startswith("._"):
            continue
        img_path = next(
            (images_root / f"{stem}{ext}" for ext in (".jpg", ".jpeg", ".png") if (images_root / f"{stem}{ext}").is_file()),
            None,
        )
        if img_path is None:
            continue
        try:
            rgb = Image.open(img_path).convert("RGB").resize((SIZE, SIZE))
            tri = np.asarray(Image.open(trip).convert("L").resize((SIZE, SIZE), Image.NEAREST), dtype=np.float32)
        except Exception:
            continue
        if tri.max() > 1.5:
            alpha = ((tri == 1).astype(np.float32) + 0.5 * (tri == 3).astype(np.float32)).clip(0, 1)
        else:
            alpha = (tri / 255.0).clip(0, 1)
        if float(alpha.mean()) < 0.05 or float(alpha.mean()) > 0.95:
            continue
        name = f"pet_{count:04d}"
        rgb.save(rgb_out / f"{name}.png")
        Image.fromarray((alpha * 255).astype(np.uint8), mode="L").save(a_out / f"{name}.png")
        count += 1
        if count % 20 == 0:
            print(f"  saved {count}", flush=True)

    shutil.rmtree(tmp, ignore_errors=True)
    n = _count_pairs(args.data)
    if n < args.min:
        raise SystemExit(f"need ≥{args.min} real matte pairs, found {n}")
    (args.data / "READY").write_text("oxford_pet\n", encoding="utf-8")
    print(f"QUALITY ready: {n} real Pet RGB+trimap pairs", flush=True)


if __name__ == "__main__":
    main()
